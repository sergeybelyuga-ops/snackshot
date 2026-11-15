import Foundation

#if canImport(FoundationNetworking)
import FoundationNetworking
#endif

// ####################################################################################
// # CODE FOR LOCAL TESTING, DO NOT COPY THIS INTO THE README.COM RECIPE
// val BASE_URL = "http://localhost:8080/v2"
// val API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>"
// val IMAGE_PATH = "path/to/multiple_items_meal.jpg>"
// ####################################################################################

// CODE TO BE COPIED TO README.COM

// === Step 1: Configure inputs & endpoint ===
let baseUrl = "https://api.logmeal.com/v2"
let apiUserToken = "<YOUR_APIUSER_TOKEN>"                 // APIUser token
let imagePath = "path/to/multiple_items_meal.jpg"         // Local JPEG path

let url = URL(string: "\(baseUrl)/image/segmentation/complete")!
var request = URLRequest(url: url)
request.httpMethod = "POST"
request.timeoutInterval = 30
request.setValue("Bearer \(apiUserToken)", forHTTPHeaderField: "Authorization")

// === Step 2: POST image & get JSON ===
let boundary = "Boundary-\(UUID().uuidString)"
request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")

let fileURL = URL(fileURLWithPath: imagePath)
guard let imageData = try? Data(contentsOf: fileURL) else {
    print("Error: Could not read image at \(imagePath)")
    exit(1)
}

var body = Data()
body.append("--\(boundary)\r\n".data(using: .utf8)!)
body.append("Content-Disposition: form-data; name=\"image\"; filename=\"image.jpg\"\r\n".data(using: .utf8)!)
body.append("Content-Type: image/jpeg\r\n\r\n".data(using: .utf8)!)
body.append(imageData)
body.append("\r\n--\(boundary)--\r\n".data(using: .utf8)!)
request.httpBody = body

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
    print("Request error: \(error)")
    exit(1)
}

guard let data = responseData, 
      let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
    print("Error: Failed to parse JSON response")
    exit(1)
}

// === Step 3: Parse & print summary ===
print("imageId: \(json["imageId"] ?? "nil")")
if let occasion = json["occasion"] as? String {
    print("occasion: \(occasion)")
}
if let foodType = json["foodType"] as? [String: Any] {
    print("foodType: \(foodType["name"] ?? "") (id \(foodType["id"] ?? ""))")
}

// Iterate each segmented food item / region
let results = json["segmentation_results"] as? [[String: Any]] ?? []
for item in results {
    let pos = item["food_item_position"] ?? 0
    let bb = item["contained_bbox"] as? [String: Any] ?? [:]
    let center = item["center"] as? [String: Any] ?? [:]
    let grams = item["serving_size"]
    
    var line = "[#\(pos)] bbox={'x': \(bb["x"] ?? 0), 'y': \(bb["y"] ?? 0), 'w': \(bb["w"] ?? 0), 'h': \(bb["h"] ?? 0)} "
    line += "center={'x': \(center["x"] ?? 0), 'y': \(center["y"] ?? 0)}"
    if let g = grams { line += " grams=\(g)" }
    print(line)

    let recs = item["recognition_results"] as? [[String: Any]] ?? []
    if let top = recs.first {
        let prob = (top["prob"] as? Double ?? 0.0)
        print("  top dish: \(top["name"] ?? "") (id \(top["id"] ?? ""), prob \(String(format: "%.3f", prob)))")
        
        // extra: print top-5 candidates for this region
        let cands = recs.prefix(5).map { r -> String in
            let p = (r["prob"] as? Double ?? 0.0)
            return "\(r["name"] ?? "") (id \(r["id"] ?? ""), prob \(String(format: "%.3f", p)))"
        }
        print("  top-5 candidates: \(cands.joined(separator: "; "))")
    }
}