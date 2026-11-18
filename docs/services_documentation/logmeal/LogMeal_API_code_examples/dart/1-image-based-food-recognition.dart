import 'dart:convert';
import 'dart:io';
import 'package:http/http.dart' as http;

/**
 * ####################################################################################
 * # CODE FOR LOCAL TESTING, DO NOT COPY THIS INTO THE README.COM RECIPE
 * * const String BASE_URL = "http://localhost:8080/v2";
 * const String API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";
 * const String IMAGE_PATH = "path/to/multiple_items_meal.jpg>";
 * ####################################################################################
 */

// CODE TO BE COPIED TO README.COM

void main() async {
  // === Step 1: Configure inputs & endpoint ===
  const String BASE_URL = "https://api.logmeal.com/v2";
  const String API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";         // APIUser token
  const String IMAGE_PATH = "path/to/multiple_items_meal.jpg>"; // Local JPEG path
  const String url = "$BASE_URL/image/segmentation/complete";
  
  final Map<String, String> headers = {
    "Authorization": "Bearer $API_USER_TOKEN"
  };

  // === Step 2: POST image & get JSON ===
  final File imageFile = File(IMAGE_PATH);
  if (!await imageFile.exists()) {
    print("Error: File not found at $IMAGE_PATH");
    return;
  }

  final request = http.MultipartRequest('POST', Uri.parse(url));
  request.headers.addAll(headers);

  // multipart field name must be 'image'
  request.files.add(await http.MultipartFile.fromPath(
    'image', 
    imageFile.path
  ));

  final streamedResponse = await request.send().timeout(const Duration(seconds: 30));
  final response = await http.Response.fromStream(streamedResponse);

  // Raise for HTTP errors (e.g., 401 bad token, 429 rate limit)
  if (response.statusCode != 200) {
    print("Request failed with status: ${response.statusCode}.");
    print(response.body);
    return;
  }

  final Map<String, dynamic> data = jsonDecode(response.body);

  // === Step 3: Parse & print summary ===
  print("imageId: ${data['imageId']}");
  if (data.containsKey('occasion')) {
    print("occasion: ${data['occasion']}");
  }
  
  final ft = data['foodType'];
  if (ft != null) {
    print("foodType: ${ft['name']} (id ${ft['id']})");
  }

  // Iterate each segmented food item / region
  final List segmentationResults = data['segmentation_results'] ?? [];
  for (var item in segmentationResults) {
    final pos = item['food_item_position'];
    final bb = item['contained_bbox'] ?? {};
    final center = item['center'] ?? {};
    final grams = item['serving_size']; // may be absent depending on plan
    final List recs = item['recognition_results'] ?? [];
    final Map? top = recs.isNotEmpty ? recs[0] : null;

    String line = "[#$pos] bbox={'x': ${bb['x']}, 'y': ${bb['y']}, 'w': ${bb['w']}, 'h': ${bb['h']}} center={'x': ${center['x']}, 'y': ${center['y']}}";
    if (grams != null) {
      line += " grams=$grams";
    }
    print(line);

    if (top != null) {
      var prob = top['prob'];
      if (prob is num) {
        prob = prob.toStringAsFixed(3);
      }
      print("  top dish: ${top['name']} (id ${top['id']}, prob $prob)");

      // extra: print top-5 candidates for this region
      List<String> cands = [];
      for (var r in recs.take(5)) {
        var p = r['prob'];
        if (p is num) p = p.toStringAsFixed(3);
        cands.add("${r['name']} (id ${r['id']}, prob $p)");
      }
      print("  top-5 candidates: ${cands.join('; ')}");
    }
  }
}