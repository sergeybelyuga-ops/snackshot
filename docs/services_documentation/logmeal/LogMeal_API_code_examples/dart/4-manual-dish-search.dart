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
  // === Step 1: Configure inputs ===
  const String BASE_URL = "https://api.logmeal.com/v2";
  const String API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";   // APIUser token (Bearer)
  const int IMAGE_ID = 1981521;                        // obtained from a previous segmentation/complete
  const String USER_QUERY = "miso soup";                  // what the user typed
  const int TARGET_REGION = 2;                         // region to correct (food_item_position)
  const String LANGUAGE = "eng";                       // language of the dishes search

  final Map<String, String> headers = {
    "Authorization": "Bearer $API_USER_TOKEN",
    "Content-Type": "application/json"
  };

  // (Example) Current mapping you plan to CONFIRM for the WHOLE image.
  // IMPORTANT: /image/confirm/dish OVERWRITES the image; any region NOT SENT here gets REMOVED.
  final List<dynamic> confirmedClasses = [110, null];   // e.g., region #1 is donuts (id=110), region #2 to be filled
  final List<dynamic> foodItemPositions = [1, 2];       // parallel to confirmedClasses
  final List<String> sources = ["logmeal", "logmeal"];  // 'logmeal' for catalog ids, 'other' for free-text

  // === Step 2: Search the dish catalog (GET /dataset/dishes) ===
  final Uri dsUri = Uri.parse("$BASE_URL/dataset/dishes").replace(queryParameters: {'language': LANGUAGE});
  final respDs = await http.get(dsUri, headers: {"Authorization": "Bearer $API_USER_TOKEN"});

  if (respDs.statusCode != 200) {
    print("Failed to fetch catalog: ${respDs.statusCode}");
    exit(1);
  }

  final dynamic catalog = jsonDecode(respDs.body);

  // Helper function to flatten the catalog
  List<Map<String, dynamic>> flattenDishes(dynamic payload) {
    List<Map<String, dynamic>> flat = [];
    if (payload is Map) {
      payload.forEach((key, items) {
        if (items is List) {
          for (var it in items) {
            if (it != null && it['name'] != null && it['id'] != null) {
              flat.add({"id": it['id'], "name": it['name']});
            }
          }
        }
      });
    } else if (payload is List) {
      for (var it in payload) {
        if (it != null && it['name'] != null && it['id'] != null) {
          flat.add({"id": it['id'], "name": it['name']});
        }
      }
    }
    return flat;
  }

  final List<Map<String, dynamic>> allDishes = flattenDishes(catalog);
  final String q = USER_QUERY.toLowerCase().trim();
  final List<Map<String, dynamic>> hits = allDishes.where((d) => 
    d["name"].toString().toLowerCase().contains(q)
  ).toList();

  // Map the user query to a dish id if found; otherwise fall back to free-text
  if (hits.isNotEmpty) {
    final picked = hits.first;
    final dynamic dishId = picked["id"];
    // Put the found id into the arrays at TARGET_REGION’s position
    int idx = foodItemPositions.indexOf(TARGET_REGION);
    if (idx != -1) {
      confirmedClasses[idx] = dishId;
    }
  }

  // === Step 3: Confirm/correct dishes ===
  const String dishUrl = "$BASE_URL/image/confirm/dish";
  final Map<String, dynamic> dishPayload = {
    "imageId": IMAGE_ID,
    "confirmedClass": confirmedClasses,
    "food_item_position": foodItemPositions,
    "source": sources
  };

  final dishResp = await http.post(
    Uri.parse(dishUrl),
    headers: headers,
    body: jsonEncode(dishPayload),
  ).timeout(const Duration(seconds: 30));

  if (dishResp.statusCode != 200) {
    print("Dish confirmation failed (${dishResp.statusCode}): ${dishResp.body}");
    exit(1);
  }

  final Map<String, dynamic> dishData = jsonDecode(dishResp.body);
  print("Dish confirmation -> OK");
  
  const JsonEncoder encoder = JsonEncoder.withIndent('  ');
  print(encoder.convert(dishData));
}