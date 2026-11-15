<?php

/**
 * ####################################################################################
 * # CODE FOR LOCAL TESTING, DO NOT COPY THIS INTO THE README.COM RECIPE
 * * $BASE_URL = "http://localhost:8080/v2";
 * $API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";
 * $IMAGE_ID = "2072700";
 * ####################################################################################
 */

// === Step 1: Configure inputs & endpoint ===
$BASE_URL = "https://api.logmeal.com/v2";
$API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";                   // APIUser token
$IMAGE_ID = "<imageId-from-previous-step>";                 // From segmentation
$url = $BASE_URL . "/nutrition/confirm/quantity";           // confirm food quantities
$headers = [
    "Authorization: Bearer " . $API_USER_TOKEN,
    "Content-Type: application/json"
];

// === Step 2: Build edits & POST ===
// Example: set/adjust per-item serving sizes (by region)
// - Use food_item_positions returned by the previous recipe endpoint
// - Values are quantities in grams (or ml for liquids)
$edits = [
    1 => 50.0,   // steamed broccoli
    2 => 180.2,  // schnitzel
    3 => 30.0,   // mayonnaise
    4 => 120.0   // baked potatoes
];

$payload = [
    "imageId" => $IMAGE_ID, 
    "quantity" => $edits
];

$ch = curl_init();

curl_setopt_array($ch, [
    CURLOPT_URL => $url,
    CURLOPT_POST => true,
    CURLOPT_POSTFIELDS => json_encode($payload),
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_HTTPHEADER => $headers,
    CURLOPT_TIMEOUT => 30,
    CURLOPT_SSL_VERIFYPEER => false,
]);

$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);

if (curl_errno($ch)) {
    die("cURL Error: " . curl_error($ch) . "\n");
}

curl_close($ch);

// Raise for HTTP errors (e.g., 401 bad token, 429 rate limit)
if ($httpCode !== 200) {
    die("HTTP Error {$httpCode}: {$response}\n");
}

echo "Quantities updated successfully\n";