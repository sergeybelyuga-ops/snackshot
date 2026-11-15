import Foundation

#if canImport(FoundationNetworking)
import FoundationNetworking
#endif

// ####################################################################################
// # CODE FOR LOCAL TESTING, DO NOT COPY THIS INTO THE README.COM RECIPE
// let BASE_URL = "http://localhost:8080/v2"
// let API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>"
// let IMAGE_ID = 1981521
// ####################################################################################

// CODE TO BE COPIED TO README.COM

// === Step 1: Configure inputs ===
let baseUrl = "https://api.logmeal.com/v2"
let apiUserToken = "<YOUR_APIUSER_TOKEN>"   // APIUser token (Bearer)
let imageId = 1981521                       // obtained from a previous segmentation/complete
let userQuery = "miso soup"                 // what the user typed
let targetRegion = 2                        // region to correct (food_item_position)
let language = "eng"                        // language of the dishes search

let headers = ["Authorization": "Bearer \(apiUserToken)",
               "Content-Type": "application/json"]

// (Example) Current mapping you plan to CONFIRM for the WHOLE image.
// IMPORTANT: /image/confirm/dish OVERWRITES the image; any region NOT SENT here gets REMOVED.
// Fill this from your UI / prior step. We'll correct targetRegion with the search result below.
var confirmedClasses: [Any] = [110, NSNull()] // e.g., region #1 is donuts (id=110), region #2 to be filled
let foodItemPositions: [Any] = [1, 2]         // parallel to confirmedClasses
let sources = ["logmeal", "logmeal"]          // 'logmeal' for catalog ids, 'other' for free-text

// Helper function to flatten the catalog
func flattenDishes(payload: Any) -> [[String: Any]] {
    var flat: [[String: Any]] = []
    if let dict = payload as? [String: Any] {
        for (_, items) in dict {
            if let list = items as? [[String: Any]] {
                for it in list {
                    if let name = it["name"] as? String, let did = it["id"] {
                        flat.append(["id": did, "name": name])
                    }
                }
            }
        }
    } else if let list = payload as? [[String: Any]] {
        for it in list {
            if let name = it["name"] as? String, let did = it["id"] {
                flat.append(["id": did, "name": name])
            }
        }
    }
    return flat
}

// === Step 2: Search the dish catalog (GET /dataset/dishes) ===
// The endpoint returns several top-level groups (e.g., "food", "drinks", "combo", "ingredients", ...).
// We'll flatten them and do a simple case-insensitive substring match on "name".
var components = URLComponents(string: "\(baseUrl)/dataset/dishes")!
components.queryItems = [URLQueryItem(name: "language", value: language)]

var dsRequest = URLRequest(url: components.url!)
dsRequest.allHTTPHeaderFields = ["Authorization": "Bearer \(apiUserToken)"]

let semaphore = DispatchSemaphore(value: 0)
let task = URLSession.shared.dataTask(with: dsRequest) { data, response, error in
    defer { semaphore.signal() }
    
    guard let data = data, 
          let catalog = try? JSONSerialization.jsonObject(with: data) else { return }
    
    let allDishes = flattenDishes(payload: catalog)
    let q = userQuery.lowercased().trimmingCharacters(in: .whitespaces)
    let hits = allDishes.filter { ($0["name"] as? String ?? "").lowercased().contains(q) }

    // Map the user query to a dish id if found; otherwise fall back to free-text
    if let picked = hits.first {
        let dishId = picked["id"] ?? 0
        // Put the found id into the arrays at targetRegion's position
        if let idx = foodItemPositions.firstIndex(where: { "\($0)" == "\(targetRegion)" }) {
            confirmedClasses[idx] = dishId
        }
    }

    // === Step 3: Confirm/correct dishes ===
    let confirmUrl = URL(string: "\(baseUrl)/image/confirm/dish")!
    var confirmReq = URLRequest(url: confirmUrl)
    confirmReq.httpMethod = "POST"
    confirmReq.allHTTPHeaderFields = headers
    
    let payload: [String: Any] = [
        "imageId": imageId,
        "confirmedClass": confirmedClasses,
        "food_item_position": foodItemPositions,
        "source": sources
    ]
    
    confirmReq.httpBody = try? JSONSerialization.data(withJSONObject: payload)

    let confirmTask = URLSession.shared.dataTask(with: confirmReq) { cData, cResp, cErr in
        if let cData = cData,
           let json = try? JSONSerialization.jsonObject(with: cData),
           let pretty = try? JSONSerialization.data(withJSONObject: json, options: .prettyPrinted),
           let str = String(data: pretty, encoding: .utf8) {
            print("Dish confirmation -> OK")
            print(str)
        }
    }
    confirmTask.resume()
}

task.resume()
semaphore.wait()