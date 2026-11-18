import Foundation

#if canImport(FoundationNetworking)
import FoundationNetworking
#endif

// ####################################################################################
// # CODE FOR LOCAL TESTING, DO NOT COPY THIS INTO THE README.COM RECIPE
// let BASE_URL = "http://localhost:8080/v2"
// let API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>"  // APIUser token
// let IMAGE_ID = "1981523"  // Obtained from previous /image/segmentation/complete
// ####################################################################################

// CODE TO BE COPIED TO README.COM

// === Step 1: Configure inputs & endpoint ===
let BASE_URL = "https://api.logmeal.com/v2"
let API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>"                   // APIUser token
let IMAGE_ID = "<imageId-from-previous-step>"                 // From segmentation
let url = URL(string: "\(BASE_URL)/nutrition/confirm/quantity")!
var request = URLRequest(url: url)
request.httpMethod = "POST"
request.setValue("Bearer \(API_USER_TOKEN)", forHTTPHeaderField: "Authorization")
request.setValue("application/json", forHTTPHeaderField: "Content-Type")

// === Step 2: Build edits & POST ===
// Example: set/adjust per-item serving sizes (by region)
// - Use food_item_positions returned by the previous recipe endpoint
// - Values are quantities in grams (or ml for liquids)
let edits: [String: Double] = [
    "1": 50.0,   // steamed broccoli
    "2": 180.2,  // schnitzel
    "3": 30.0,   // mayonnaise
    "4": 120.0   // baked potatoes
]

let payload: [String: Any] = [
    "imageId": IMAGE_ID,
    "quantity": edits
]

do {
    request.httpBody = try JSONSerialization.data(withJSONObject: payload)
} catch {
    print("Error encoding payload: \(error)")
    exit(1)
}

let semaphore = DispatchSemaphore(value: 0)
let task = URLSession.shared.dataTask(with: request) { data, response, error in
    defer { semaphore.signal() }
    
    if let error = error {
        print("HTTP Request Error: \(error)")
        return
    }
    
    if let httpResponse = response as? HTTPURLResponse {
        if (200...299).contains(httpResponse.statusCode) {
            print("Food quantities confirmed -> OK")
            print("Status: \(httpResponse.statusCode)")
        } else {
            print("Failed to confirm quantities. Status: \(httpResponse.statusCode)")
            if let data = data, let responseString = String(data: data, encoding: .utf8) {
                print("Response: \(responseString)")
            }
        }
    }
}

task.resume()
semaphore.wait()