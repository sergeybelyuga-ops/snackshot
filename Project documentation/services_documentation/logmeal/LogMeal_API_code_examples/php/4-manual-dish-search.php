<?php

/**
 * ####################################################################################
 * # CODE FOR LOCAL TESTING, DO NOT COPY THIS INTO THE README.COM RECIPE
 * * $BASE_URL = "http://localhost:8080/v2";
 * $API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";
 * $IMAGE_ID = 2072700;
 * ####################################################################################
 */

// === Step 1: Configure inputs ===
$BASE_URL = "https://api.logmeal.com/v2";
$API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";                     // APIUser token (Bearer)
$IMAGE_ID = "<imageId-from-previous-step>";                // obtained from a previous segmentation/complete
$USER_QUERY = "miso soup";                                    // what the user typed
$TARGET_REGION = 2;                                           // region to correct (food_item_position)
$LANGUAGE = "eng";                                            // language of the dishes search

$headers = [
    "Authorization: Bearer " . $API_USER_TOKEN,
    "Content-Type: application/json"
];

// (Example) Current mapping you plan to CONFIRM for the WHOLE image.
// IMPORTANT: /image/confirm/dish OVERWRITES the image; any region NOT SENT here gets REMOVED.
$confirmed_classes = [110, null];           // e.g., region #1 is donuts (id=110), region #2 to be filled
$food_item_positions = [1, 2];              // parallel to confirmed_classes
$sources = ["logmeal", "logmeal"];          // 'logmeal' for catalog ids, 'other' for free-text

// === Step 2: Search the dish catalog (GET /dataset/dishes) ===
// The endpoint returns several top-level groups (e.g., "food", "drinks", "combo", ...).
// We'll flatten them and do a simple case-insensitive substring match on "name".
$ch = curl_init();
$ds_url = $BASE_URL . "/dataset/dishes?language=" . $LANGUAGE;

curl_setopt_array($ch, [
    CURLOPT_URL => $ds_url,
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_HTTPHEADER => ["Authorization: Bearer " . $API_USER_TOKEN],
    CURLOPT_TIMEOUT => 30,
    CURLOPT_SSL_VERIFYPEER => false,
]);

$ds_response = curl_exec($ch);
if (curl_errno($ch)) { die("cURL Error: " . curl_error($ch)); }
$catalog = json_decode($ds_response, true);

function flatten_dishes($payload) {
    $flat = [];
    if (is_array($payload)) {
        foreach ($payload as $group => $items) {
            if (is_array($items)) {
                foreach ($items as $it) {
                    $name = $it['name'] ?? null;
                    $did = $it['id'] ?? null;
                    if ($name && $did !== null) {
                        $flat[] = ["id" => $did, "name" => $name];
                    }
                }
            }
        }
    }
    return $flat;
}

$all_dishes = flatten_dishes($catalog);
$q = strtolower(trim($USER_QUERY));
$hits = [];

foreach ($all_dishes as $d) {
    if (strpos(strtolower($d['name']), $q) !== false) {
        $hits[] = $d;
    }
}

// Map the user query to a dish id if found; otherwise fall back to free-text
if (!empty($hits)) {
    $picked = $hits[0];
    $dish_id = $picked['id'];
    
    // Find the index of the target region to update the confirmed_classes array
    $idx = array_search($TARGET_REGION, $food_item_positions);
    if ($idx !== false) {
        $confirmed_classes[$idx] = $dish_id;
    }
}

// === Step 3: Confirm/correct dishes ===
$dish_url = $BASE_URL . "/image/confirm/dish";
$dish_payload = [
    "imageId" => (int)$IMAGE_ID,
    "confirmedClass" => $confirmed_classes,
    "food_item_position" => $food_item_positions,
    "source" => $sources
];

curl_setopt($ch, CURLOPT_URL, $dish_url);
curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($dish_payload));
curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);

$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);

if (curl_errno($ch)) { die("cURL Error: " . curl_error($ch)); }
curl_close($ch);

if ($httpCode !== 200) {
    die("HTTP Error {$httpCode}: {$response}\n");
}

$dish_data = json_decode($response, true);
echo "Dish confirmation -> OK\n";
echo json_encode($dish_data, JSON_PRETTY_PRINT) . "\n";