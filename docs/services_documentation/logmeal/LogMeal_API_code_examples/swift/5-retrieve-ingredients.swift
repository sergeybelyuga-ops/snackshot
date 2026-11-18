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
let API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>"                  // APIUser token
let IMAGE_ID = "<imageId-from-previous-step>"                // From segmentation
let url = URL(string: "\(BASE_URL)/nutrition/recipe/ingredients")!
var request = URLRequest(url: url)
request.httpMethod = "POST"
request.setValue("Bearer \(API_USER_TOKEN)", forHTTPHeaderField: "Authorization")
request.setValue("application/json", forHTTPHeaderField: "Content-Type")

// === Step 2: Request ingredients & quantities ===
let payload: [String: Any] = ["imageId": IMAGE_ID]
request.httpBody = try? JSONSerialization.data(withJSONObject: payload)

let semaphore = DispatchSemaphore(value: 0)
var responseData: Data?
var requestError: Error?

let task = URLSession.shared.dataTask(with: request) { data, response, error in
    responseData = data
    requestError = error
    semaphore.signal()
}
task.resume()
semaphore.wait()

if let error = requestError {
    print("Error: \(error)")
    exit(1)
}

guard let data = responseData, 
      let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
    print("Error: Failed to parse JSON response")
    exit(1)
}

// === Step 3: Parse & print summary (matches provided structure) ===
print("imageId: \(json["imageId"] ?? IMAGE_ID)")
if let servingSize = json["serving_size"] {
    print("meal_serving_size: \(servingSize) g")
}

// Prefer per-item recipes if present
let items = json["recipe_per_item"] as? [[String: Any]] ?? []

if !items.isEmpty {
    for it in items {
        let pos = it["food_item_position"] ?? ""
        let dname = it["name"] ?? ""
        let ss = it["serving_size"] ?? ""
        let unit = it["unit"] ?? ""
        print("[#\(pos)] dish=\(dname) serving_size=\(ss) \(unit)")

        // Ingredients for this dish
        let recipe = it["recipe"] as? [[String: Any]] ?? []
        for ing in recipe {
            let iid = ing["id"] ?? ""
            let name = ing["name"] ?? ""
            let weight = ing["weight"] ?? ""
            let unit_i = ing["unit"] as? String ?? "g"
            let meas = ing["measure"] as? [String: Any] ?? [:]
            let metric = meas["metric"] as? [String: Any] ?? [:]
            let preferred = meas["preferred"] as? [String: Any] ?? [:]

            // Metric (canonical) amount
            let m_qty = metric["quantity"] ?? weight
            let m_short = metric["short"] as? String ?? unit_i

            // Preferred (human-friendly) measure if available
            let p_qty = preferred["quantity"]
            let p_name = preferred["name"] as? String

            var line = "  - \(name) (id \(iid)) -> \(m_qty) \(m_short)"
            if let pq = p_qty, let pn = p_name {
                line += " (~\(pq) \(pn))"
            }
            print(line)
        }
    }
} else {
    // Fallback 1: show flat global recipe
    let ingrList = json["recipe"] as? [[String: Any]] ?? []
    if !ingrList.isEmpty {
        print("Global recipe:")
        for ing in ingrList {
            let iid = ing["id"] ?? ""
            let name = ing["name"] ?? ""
            let weight = ing["weight"] ?? ""
            let unit_i = ing["unit"] as? String ?? "g"
            let meas = ing["measure"] as? [String: Any] ?? [:]
            let metric = meas["metric"] as? [String: Any] ?? [:]
            let preferred = meas["preferred"] as? [String: Any] ?? [:]

            let m_qty = metric["quantity"] ?? weight
            let m_short = metric["short"] as? String ?? unit_i

            let p_qty = preferred["quantity"]
            let p_name = preferred["name"] as? String

            var line = "  - \(name) (id \(iid)) -> \(m_qty) \(m_short)"
            if let pq = p_qty, let pn = p_name {
                line += " (~\(pq) \(pn))"
            }
            print(line)
        }
    } else {
        // Fallback 2: generic fields (unlikely with this endpoint)
        let excludedFields: Set<String> = ["imageId", "recipe_per_item", "recipe", "dish_id", "foodName", "is_combo", "serving_size", "source"]
        for (name, value) in json {
            if !excludedFields.contains(name) {
                if let jsonData = try? JSONSerialization.data(withJSONObject: value, options: []),
                   let jsonString = String(data: jsonData, encoding: .utf8) {
                    print("\(name): \(jsonString)")
                }
            }
        }
    }
}