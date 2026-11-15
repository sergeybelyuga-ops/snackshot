import Foundation

#if canImport(FoundationNetworking)
import FoundationNetworking
#endif

// ####################################################################################
// # CODE FOR LOCAL TESTING, DO NOT COPY THIS INTO THE README.COM RECIPE
// let BASE_URL = "http://localhost:8080/v2"
// let API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>"  // APIUser token
// let IMAGE_ID = "1981523"  // Obtained from previous steps
// ####################################################################################

// CODE TO BE COPIED TO README.COM

// === Step 1: Configure inputs & endpoint ===
let BASE_URL = "https://api.logmeal.com/v2"
let API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>"                  // APIUser token
let IMAGE_ID = "<imageId-from-previous-step>"                // From segmentation
let url = URL(string: "\(BASE_URL)/nutrition/recipe/nutritionalInfo")!
var request = URLRequest(url: url)
request.httpMethod = "POST"
request.setValue("Bearer \(API_USER_TOKEN)", forHTTPHeaderField: "Authorization")
request.setValue("application/json", forHTTPHeaderField: "Content-Type")

// === Step 2: Request nutritional info ===
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

// Optional: overall Nutri-Score (if present)
if let ns = json["image_nutri_score"] as? [String: Any] {
    let cat = ns["nutri_score_category"] ?? ""
    let std = ns["nutri_score_standardized"] ?? ""
    print("nutri_score: \(cat) (\(std))")
}

// Meal-level totals
let total = json["nutritional_info"] as? [String: Any] ?? [:]
if !total.isEmpty {
    if let cal = total["calories"] as? Double {
        print("calories_total: \(String(format: "%.2f", cal)) kcal")
    }

    // Daily Intake Reference (DIR) summary
    let dirf = total["dailyIntakeReference"] as? [String: Any] ?? [:]
    if !dirf.isEmpty {
        print("Daily Intake Reference (meal):")
        let keys = ["ENERC_KCAL","PROCNT","FAT","CHOCDF","FIBTG","SUGAR","FASAT","NA"]
        for k in keys {
            if let v = dirf[k] as? [String: Any] {
                let label = v["label"] as? String ?? k
                let level = v["level"] ?? ""
                let pct = v["percent"] as? Double ?? 0.0
                print("  \(label): \(String(format: "%.1f", pct))% (\(level))")
            }
        }
    }

    // Key nutrients (totals)
    let tn = total["totalNutrients"] as? [String: Any] ?? [:]
    func _fmt(_ n: Any?) -> String? {
        guard let dict = n as? [String: Any],
              let q = dict["quantity"] as? Double,
              let u = dict["unit"] as? String,
              let lab = dict["label"] as? String else { return nil }
        return "\(lab): \(String(format: "%.2f", q)) \(u)"
    }
    
    let keyCodes = ["PROCNT","FAT","CHOCDF","FIBTG","SUGAR"]
    let formattedNutrients = keyCodes.compactMap { _fmt(tn[$0]) }
    if !formattedNutrients.isEmpty {
        print("Nutrients (meal): " + formattedNutrients.joined(separator: "; "))
    }
}

// Per-item nutritional info
var nameMap: [Int: String] = [:]
if let ids = json["ids"] as? [Int], let names = json["foodName"] as? [String] {
    for (index, id) in ids.enumerated() {
        nameMap[id] = names[index]
    }
}

let items = json["nutritional_info_per_item"] as? [[String: Any]] ?? []
for it in items {
    let pos = it["food_item_position"] ?? ""
    let iid = it["id"] as? Int ?? 0
    let dname = nameMap[iid] ?? "Unknown"
    let ss = it["serving_size"] ?? 0
    print("[#\(pos)] dish=\(dname) serving_size=\(ss) g")

    // Item Nutri-Score (if present)
    if let ns = it["nutri_score"] as? [String: Any] {
        let cat = ns["nutri_score_category"] ?? ""
        let std = ns["nutri_score_standardized"] ?? ""
        print("  nutri_score: \(cat) (\(std))")
    }

    let ni = it["nutritional_info"] as? [String: Any] ?? [:]
    if let calI = ni["calories"] as? Double {
        print("  calories: \(String(format: "%.2f", calI)) kcal")
    }

    // Item nutrients and DIR
    let tnI = ni["totalNutrients"] as? [String: Any] ?? [:]
    let dirI = ni["dailyIntakeReference"] as? [String: Any] ?? [:]
    
    let itemNutrients = ["PROCNT","FAT","CHOCDF","FIBTG","SUGAR"].compactMap { _fmt(tnI[$0]) }
    if !itemNutrients.isEmpty {
        print("  nutrients: " + itemNutrients.joined(separator: "; "))
    }

    let dirKeys = ["ENERC_KCAL","PROCNT","FAT","CHOCDF","FIBTG","SUGAR","FASAT","NA"]
    var dirOut: [String] = []
    for k in dirKeys {
        if let v = dirI[k] as? [String: Any], let pct = v["percent"] as? Double {
            let label = v["label"] as? String ?? k
            dirOut.append("\(label): \(String(format: "%.1f", pct))%")
        }
    }
    if !dirOut.isEmpty {
        print("  daily intake: " + dirOut.joined(separator: "; "))
    }
}