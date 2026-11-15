import 'dart:convert';
import 'dart:io';
import 'package:http/http.dart' as http;

/**
 * ####################################################################################
 * # CODE FOR LOCAL TESTING, DO NOT COPY THIS INTO THE README.COM RECIPE
 * * const String BASE_URL = "http://localhost:8080/v2";
 * const String API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";
 * ####################################################################################
 */

// CODE TO BE COPIED TO README.COM

void main() async {
  // -----------------------------
  // Step 1 - Prepare configuration & inputs
  // -----------------------------
  const String BASE_URL = "https://api.logmeal.com/v2";
  const String API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";  // APIUser token

  final Map<String, String> headers = {
    "Authorization": "Bearer $API_USER_TOKEN",
    "Content-Type": "application/json"
  };

  // Define preferences (ISO country code + ISO language code)
  final Map<String, String> preferences = {
    "country": "ES",   // Spain
    "language": "spa"   // Spanish
  };

  // -----------------------------
  // Step 2 - Send profile personalization request
  // -----------------------------
  const String url = "$BASE_URL/profile/modifyUserProfileInfo";
  
  final response = await http.post(
    Uri.parse(url),
    headers: headers,
    body: jsonEncode(preferences),
  ).timeout(const Duration(seconds: 30));

  // -----------------------------
  // Step 3 - Confirm personalization response
  // -----------------------------
  if (response.statusCode != 200) {
    print("Failed to personalize profile (${response.statusCode}): ${response.body}");
    exit(1);
  }

  final Map<String, dynamic> profileInfo = jsonDecode(response.body);
  
  // Pretty print JSON output
  const JsonEncoder encoder = JsonEncoder.withIndent('  ');
  print("Profile updated: ${encoder.convert(profileInfo)}");

  // -----------------------------
  // Step 4 - Use personalization in subsequent recognition requests
  // -----------------------------
  // After this call, all recognition endpoints (e.g., /image/segmentation/complete)
  // will return dish, ingredients and nutrient labels in the chosen language and
  // prioritized for the selected country.
}