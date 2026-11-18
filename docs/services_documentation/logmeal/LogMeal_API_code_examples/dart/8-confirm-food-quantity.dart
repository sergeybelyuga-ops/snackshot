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
  const String url = "$BASE_URL/nutrition/confirm/quantity";           // confirm food quantities
  
  final Map<String, String> headers = {
    "Authorization": "Bearer $API_USER_TOKEN",
    "Content-Type": "application/json"
  };

  // === Step 2: Build edits & POST ===
  // Example: set/adjust per-item serving sizes (by region)
  // - Use food_item_positions returned by the previous recipe endpoint
  // - Values are quantities in grams (or ml for liquids)
  // IMPORTANT: Dart's jsonEncode requires Map keys to be Strings for JSON objects.
  final Map<String, double> edits = {
    "1": 50.0,   // steamed broccoli
    "2": 180.2,  // schnitzel
    "3": 30.0,   // mayonnaise
    "4": 120.0   // baked potatoes
  };

  final Map<String, dynamic> payload = {
    "imageId": IMAGE_ID, 
    "quantity": edits
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

  print("Quantities confirmed successfully");
}