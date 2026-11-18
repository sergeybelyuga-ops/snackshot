<?php

/**
 * ####################################################################################
 * # CODE FOR LOCAL TESTING, DO NOT COPY THIS INTO THE README.COM RECIPE
 * * $BASE_URL = "http://localhost:8080/v2";
 * $API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";
 * ####################################################################################
 */

// -----------------------------
// Step 1 - Prepare configuration & inputs
// -----------------------------
$BASE_URL = "https://api.logmeal.com/v2";
$API_USER_TOKEN = "<REPLACE_WITH_APIUSER_TOKEN>";  // APIUser token

$headers = [
    "Authorization: Bearer " . $API_USER_TOKEN,
    "Content-Type: application/json"
];

// Define preferences (ISO country code + ISO language code)
$preferences = [
    "country" => "ES",   // Spain
    "language" => "spa"   // Spanish
];

$url = $BASE_URL . "/profile/modifyUserProfileInfo";

// -----------------------------
// Step 2 - Send profile personalization request
// -----------------------------
$ch = curl_init();

curl_setopt_array($ch, [
    CURLOPT_URL => $url,
    CURLOPT_POST => true,
    CURLOPT_POSTFIELDS => json_encode($preferences),
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_HTTPHEADER => $headers,
    CURLOPT_TIMEOUT => 30,
    CURLOPT_SSL_VERIFYPEER => false, // For local testing environments
]);

$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);

if (curl_errno($ch)) {
    die("cURL Error: " . curl_error($ch) . "\n");
}

curl_close($ch);

// -----------------------------
// Step 3 - Confirm personalization response
// -----------------------------
if ($httpCode !== 200) {
    die("Failed to personalize profile ({$httpCode}): {$response}\n");
}

$profile_info = json_decode($response, true);
echo "Profile updated: " . json_encode($profile_info, JSON_PRETTY_PRINT) . "\n";

// -----------------------------
// Step 4 - Use personalization in subsequent recognition requests
// -----------------------------
// After this call, all recognition endpoints (e.g., /image/segmentation/complete)
// will return dish, ingredients and nutrient labels in the chosen language and
// prioritized for the selected country.