import Foundation

#if canImport(FoundationNetworking)
import FoundationNetworking
#endif

import requests
import json

// ####################################################################################
// # CODE FOR LOCAL TESTING, DO NOT COPY THIS INTO THE README.COM RECIPE
// val BASE_URL = "http://localhost:8080/v2"
// val API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>"
// let IMAGE_ID = 2072707
// ####################################################################################

// CODE TO BE COPIED TO README.COM

// === Step 1: Configure inputs & intended corrections ===
let baseUrl = "https://api.logmeal.com/v2"
let apiUserToken = "<YOUR_APIUSER_TOKEN>"   // APIUser token (Bearer)
let imageId: Any = 1981521                  // obtained from a previous food intake insertion

// Example mapping from segmentation results you want to confirm/correct:
// IMPORTANT: /image/confirm/dish OVERWRITES the image; any region NOT SENT here gets REMOVED.
// - Region #1 was recognized as "donuts" (id 110) and that's correct.
// - Region #2 should be corrected to "mushroom cream" (id 1474).
// - Add an extra dish not in any segment: free text "grandma soup".
let confirmedClasses: [Any] = [110, 1474, "grandma soup"]  // ids or free-text (when source='other')
let foodItemPositions: [Any] = [1, 2, "extra_dish_1"]      // int for segments, string for extra dish
let sources = ["logmeal", "logmeal", "other"]              // one entry per confirmed class

let headers = ["Authorization": "Bearer \(apiUserToken)",
               "Content-Type": "application/json"]

// === Step 2: Confirm/correct dishes (per segment or extra) ===
let url = URL(string: "\(baseUrl)/image/confirm/dish")!
var request = URLRequest(url: url)
request.httpMethod = "POST"
request.timeoutInterval = 30
for (key, value) in headers {
    request.setValue(value, forHTTPHeaderField: key)
}

let payload: [String: Any] = [
    "imageId": imageId,
    "confirmedClass": confirmedClasses,
    "food_item_position": foodItemPositions,
    "source": sources
]

guard let httpBody = try? JSONSerialization.data(withJSONObject: payload) else {
    print("Error: Could not encode payload")
    exit(1)
}
request.httpBody = httpBody

let semaphore = DispatchSemaphore(value: 0)
let task = URLSession.shared.dataTask(with: request) { data, response, error in
    defer { semaphore.signal() }
    
    if let error = error {
        print("HTTP Request Error: \(error)")
        return
    }
    
    guard let data = data,
          let json = try? JSONSerialization.jsonObject(with: data),
          let prettyData = try? JSONSerialization.data(withJSONObject: json, options: .prettyPrinted),
          let prettyString = String(data: prettyData, encoding: .utf8) else {
        return
    }

    print("Dish confirmation -> OK")
    print(prettyString)
}

task.resume()
semaphore.wait()