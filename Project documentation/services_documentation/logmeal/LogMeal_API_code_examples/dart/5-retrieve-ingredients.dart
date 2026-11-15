import 'dart:convert';
import 'dart:io';
import 'package:http/http.dart' as http;

/**
 * ####################################################################################
 * # CODE FOR LOCAL TESTING, DO NOT COPY THIS INTO THE README.COM RECIPE
 * * const String BASE_URL = "http://localhost:8080/v2";
 * const String API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";
 * const int IMAGE_ID = 2072702;
 * ####################################################################################
 */

// CODE TO BE COPIED TO README.COM

void main() async {
  // === Step 1: Configure inputs & endpoint ===
  const String BASE_URL = "https://api.logmeal.com/v2";
  const String API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";                  // APIUser token
  const int IMAGE_ID = 1981521;                                         // From segmentation
  const String url = "$BASE_URL/nutrition/recipe/ingredients";         // default latest model
  
  final Map<String, String> headers = {
    "Authorization": "Bearer $API_USER_TOKEN",
    "Content-Type": "application/json"
  };

  // === Step 2: Request ingredients & quantities ===
  final Map<String, dynamic> payload = {"imageId": IMAGE_ID};
  
  final response = await http.post(
    Uri.parse(url),
    headers: headers,
    body: jsonEncode(payload),
  ).timeout(const Duration(seconds: 30));

  // Raise for HTTP errors (e.g., 401 bad token, 429 rate limit)
  if (response.statusCode != 200) {
    print("Request failed with status: ${response.statusCode}.");
    print(response.body);
    exit(1);
  }

  final Map<String, dynamic> data = jsonDecode(response.body);

  // === Step 3: Parse & print summary (matches provided structure) ===
  print("imageId: ${data['imageId'] ?? IMAGE_ID}");
  if (data.containsKey('serving_size')) {
    print("meal_serving_size: ${data['serving_size']} g");
  }

  // Prefer per-item recipes if present
  final List items = data['recipe_per_item'] ?? [];

  if (items.isNotEmpty) {
    for (var it in items) {
      final pos = it['food_item_position'];
      final dname = it['name'];
      final ss = it['serving_size'];
      final unit = it['unit'];
      print("[#$pos] dish=$dname serving_size=$ss $unit");

      // Ingredients for this dish
      final List recipe = it['recipe'] ?? [];
      for (var ing in recipe) {
        final iid = ing['id'];
        final name = ing['name'];
        final weight = ing['weight'];
        final unitI = ing['unit'] ?? 'g';
        final meas = ing['measure'] ?? {};
        final metric = meas['metric'] ?? {};
        final preferred = meas['preferred'] ?? {};

        // Metric (canonical) amount
        final mQty = metric['quantity'] ?? weight;
        final mShort = metric['short'] ?? unitI;

        // Preferred (human-friendly) measure if available
        final pQty = preferred['quantity'];
        final pName = preferred['name'];

        String line = "  - $name (id $iid) -> $mQty $mShort";
        if (pQty != null && pName != null) {
          line += " (~$pQty $pName)";
        }
        print(line);
      }
    }
  } else {
    // Fallback 1: show flat global recipe
    final List ingrList = data['recipe'] ?? [];
    if (ingrList.isNotEmpty) {
      print("Global recipe:");
      for (var ing in ingrList) {
        final iid = ing['id'];
        final name = ing['name'];
        final weight = ing['weight'];
        final unitI = ing['unit'] ?? 'g';
        final meas = ing['measure'] ?? {};
        final metric = meas['metric'] ?? {};
        final preferred = meas['preferred'] ?? {};

        final mQty = metric['quantity'] ?? weight;
        final mShort = metric['short'] ?? unitI;

        final pQty = preferred['quantity'];
        final pName = preferred['name'];

        String line = "  - $name (id $iid) -> $mQty $mShort";
        if (pQty != null && pName != null) {
          line += " (~$pQty $pName)";
        }
        print(line);
      }
    } else {
      // Fallback 2: generic fields (unlikely with this endpoint)
      final List<String> skipKeys = ['imageId', 'recipe_per_item', 'recipe', 'dish_id', 'foodName', 'is_combo', 'serving_size', 'source'];
      data.forEach((key, value) {
        if (!skipKeys.contains(key)) {
          print("$key: ${jsonEncode(value)}");
        }
      });
    }
  }
}