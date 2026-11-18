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
  const String API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";                   // APIUser token
  const int IMAGE_ID = 1981521;                                         // From segmentation
  const String url = "$BASE_URL/nutrition/confirm/ingredients";         // confirm edits
  
  final Map<String, String> headers = {
    "Authorization": "Bearer $API_USER_TOKEN",
    "Content-Type": "application/json"
  };

  // === Step 2: Build edits & POST ===
  // IMPORTANT: Dart's jsonEncode requires Map keys to be Strings for JSON objects.
  // We use "1" and "2" instead of 1 and 2.
  final Map<String, List<Map<String, dynamic>>> edits = {
    "1": [  // food_item_position as String
        {"ingredientId": 1478, "ingredientAmount": 85.0},
        {"ingredientId": 61,   "ingredientAmount": 12.0},
        {"ingredientId": 562,  "ingredientAmount": 12.0}
    ],
    "2": [  // food_item_position as String
        {"ingredientId": 204,  "ingredientAmount": 220.0},
        {"ingredientId": 562,  "ingredientAmount": 8.0},
        {"ingredientId": 45,   "ingredientAmount": 1.2}
    ]
  };

  final Map<String, dynamic> payload = {
    "imageId": IMAGE_ID, 
    "ingredients_per_item": edits
  };

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

  print("Ingredients updated successfully");
}