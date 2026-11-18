import Foundation

#if canImport(FoundationNetworking)
import FoundationNetworking
#endif

// ####################################################################################
// # CODE FOR LOCAL TESTING, DO NOT COPY THIS INTO THE README.COM RECIPE
// let BASE_URL = "http://localhost:8080/v2"
// let API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>"
// ####################################################################################

// CODE TO BE COPIED TO README.COM

// -----------------------------
// Step 1 - Prepare configuration & inputs
// -----------------------------
let baseUrl = "https://api.logmeal.com/v2"  // Base URL
let apiUserToken = "<YOUR_APIUSER_TOKEN>"  // APIUser token

// Define preferences (ISO country code + ISO language code)
let preferences: [String: String] = [
    "country": "ES",   // Spain
    "language": "spa"   // Spanish
]

let url = URL(string: "\(baseUrl)/profile/modifyUserProfileInfo")!
var request = URLRequest(url: url)
request.httpMethod = "POST"
request.timeoutInterval = 30
request.setValue("Bearer \(apiUserToken)", forHTTPHeaderField: "Authorization")
request.setValue("application/json", forHTTPHeaderField: "Content-Type")

// -----------------------------
// Step 2 - Send profile personalization request
// -----------------------------
guard let httpBody = try? JSONSerialization.data(withJSONObject: preferences) else {
    print("Error: Could not encode preferences")
    exit(1)
}
request.httpBody = httpBody

let semaphore = DispatchSemaphore(value: 0)
let task = URLSession.shared.dataTask(with: request) { data, response, error in
    defer { semaphore.signal() }
    
    if let error = error {
        print("Error: \(error)")
        return
    }
    
    guard let httpResponse = response as? HTTPURLResponse, let data = data else { return }

    // -----------------------------
    // Step 3 - Confirm personalization response
    // -----------------------------
    if httpResponse.statusCode != 200 {
        let errorText = String(data: data, encoding: .utf8) ?? ""
        print("Failed to personalize profile (\(httpResponse.statusCode)): \(errorText)")
        exit(1)
    }

    if let json = try? JSONSerialization.jsonObject(with: data),
       let prettyData = try? JSONSerialization.data(withJSONObject: json, options: .prettyPrinted),
       let prettyString = String(data: prettyData, encoding: .utf8) {
        
        print("Profile updated: \(prettyString)")
    }
}

task.resume()
semaphore.wait()

// -----------------------------
// Step 4 - Use personalization in subsequent recognition requests
// -----------------------------
// After this call, all recognition endpoints (e.g., /image/segmentation/complete)
// will return dish, ingredients and nutrient labels in the chosen language and
// prioritized for the selected country.