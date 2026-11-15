import 'dart:convert';
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
  // === Step 1: Configure inputs & intended corrections ===
  const String BASE_URL = "https://api.logmeal.com/v2";
  const String API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";   // APIUser token (Bearer)
  const int IMAGE_ID = 1981521;  // obtained from a previous food intake insertion

  // Example mapping from food intake insertion results you want to confirm/correct:
  // IMPORTANT: /image/confirm/dish OVERWRITES the image; any region NOT SENT here gets REMOVED.
  // - Region #1 was recognized as "donuts" (id 110) and that's correct.
  // - Region #2 should be corrected to "mushroom cream" (id 1474).
  // - Add an extra dish not in any segment: free text "grandma soup".
  final List<dynamic> confirmedClasses = [110, 1474, "grandma soup"]; // ids or free-text
  final List<dynamic> foodItemPositions = [1, 2, "extra_dish_1"];    // int or string
  final List<String> sources = ["logmeal", "logmeal", "other"];      // one entry per class

  final Map<String, String> headers = {
    "Authorization": "Bearer $API_USER_TOKEN",
    "Content-Type": "application/json"
  };

  // === Step 2: Confirm/correct dishes (per segment or extra) ===
  const String dishUrl = "$BASE_URL/image/confirm/dish";
  
  final Map<String, dynamic> dishPayload = {
    "imageId": IMAGE_ID,
    "confirmedClass": confirmedClasses,
    "food_item_position": foodItemPositions,
    "source": sources
  };

  final response = await http.post(
    Uri.parse(dishUrl),
    headers: headers,
    body: jsonEncode(dishPayload),
  ).timeout(const Duration(seconds: 30));

  // Raise for HTTP errors (e.g., 401 bad token, 429 rate limit)
  if (response.statusCode != 200) {
    print("Request failed with status: ${response.statusCode}.");
    print(response.body);
    return;
  }

  final Map<String, dynamic> dishData = jsonDecode(response.body);
  print("Dish confirmation -> OK");
  
  // Pretty print JSON output
  const JsonEncoder encoder = JsonEncoder.withIndent('  ');
  print(encoder.convert(dishData));
}