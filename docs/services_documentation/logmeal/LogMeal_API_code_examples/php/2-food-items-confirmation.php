<?php

/**
 * ####################################################################################
 * # CODE FOR LOCAL TESTING, DO NOT COPY THIS INTO THE README.COM RECIPE
 * * $BASE_URL = "http://localhost:8080/v2";
 * $API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";
 * $IMAGE_ID = 1981521;
 * ####################################################################################
 */

// === Step 1: Configure inputs & intended corrections ===
$BASE_URL = "https://api.logmeal.com/v2";
$API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";                     // APIUser token (Bearer)
$IMAGE_ID = "<imageId-from-previous-step>";                // From segmentation

// Example mapping from food intake insertion results you want to confirm/correct:
// IMPORTANT: /image/confirm/dish OVERWRITES the image; any region NOT SENT here gets REMOVED.
$confirmed_classes = [110, 1474, "grandma soup"];  // ids or free-text (when source='other')
$food_item_positions = [1, 2, "extra_dish_1"];     // int for segments, string for extra dish
$sources = ["logmeal", "logmeal", "other"];        // one entry per confirmed class

$url = $BASE_URL . "/image/confirm/dish";

// === Step 2: Confirm/correct dishes (per segment or extra) ===
$ch = curl_init();

$payload = [
    "imageId" => (int)$IMAGE_ID,
    "confirmedClass" => $confirmed_classes,
    "food_item_position" => $food_item_positions,
    "source" => $sources
];

curl_setopt_array($ch, [
    CURLOPT_URL => $url,
    CURLOPT_POST => true,
    CURLOPT_POSTFIELDS => json_encode($payload),
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_HTTPHEADER => [
        "Authorization: Bearer " . $API_USER_TOKEN,
        "Content-Type: application/json"
    ],
    CURLOPT_TIMEOUT => 30,
    CURLOPT_SSL_VERIFYPEER => false,
]);

$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);

if (curl_errno($ch)) {
    die("cURL Error: " . curl_error($ch) . "\n");
}

curl_close($ch);

// Raise for HTTP errors
if ($httpCode !== 200) {
    die("HTTP Error {$httpCode}: {$response}\n");
}

$data = json_decode($response, true);

echo "Dish confirmation -> OK\n";
echo json_encode($data, JSON_PRETTY_PRINT) . "\n";