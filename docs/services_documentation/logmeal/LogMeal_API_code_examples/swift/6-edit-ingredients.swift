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
let url = URL(string: "\(BASE_URL)/nutrition/confirm/ingredients")!
var request = URLRequest(url: url)
request.httpMethod = "POST"
request.setValue("Bearer \(API_USER_TOKEN)", forHTTPHeaderField: "Authorization")
request.setValue("application/json", forHTTPHeaderField: "Content-Type")

// === Step 2: Build edits & POST ===
// Example: adjust quantities and swap ingredients per detected item
// - Use food_item_positions returned by the previous recipe endpoint
// - 'ingredientAmount' is in grams (or ml for liquids)
let edits: [String: [[String: Any]]] = [
    "1": [  // food_item_position
        ["ingredientId": 1478, "ingredientAmount": 85.0],  // chicken (boneless) -> 85 g
        ["ingredientId": 61,   "ingredientAmount": 12.0],  // bread crumbles     -> 12 g
        ["ingredientId": 562,  "ingredientAmount": 12.0]   // oil                -> 12 ml
    ],
    "2": [  // food_item_position
        ["ingredientId": 204,  "ingredientAmount": 220.0], // potato -> 220 g
        ["ingredientId": 562,  "ingredientAmount": 8.0],   // oil    -> 8 ml
        ["ingredientId": 45,   "ingredientAmount": 1.2]    // salt   -> 1.2 g
    ]
]

let payload: [String: Any] = [
    "imageId": IMAGE_ID,
    "ingredients_per_item": edits
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
            print("Ingredients edited -> OK")
            print("Status: \(httpResponse.statusCode)")
        } else {
            print("Failed to edit ingredients. Status: \(httpResponse.statusCode)")
            if let data = data, let responseString = String(data: data, encoding: .utf8) {
                print("Response: \(responseString)")
            }
        }
    }
}

task.resume()
semaphore.wait()