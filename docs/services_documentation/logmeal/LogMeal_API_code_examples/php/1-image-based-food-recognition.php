<?php

/**
 * ####################################################################################
 * # CODE FOR LOCAL TESTING, DO NOT COPY THIS INTO THE README.COM RECIPE
 * * $BASE_URL = "http://localhost:8080/v2";
 * $API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";
 * $IMAGE_PATH = "<path/to/multiple_items_meal.jpg>";
 * ####################################################################################
 */

// === Step 1: Configure inputs & endpoint ===
$BASE_URL = "https://api.logmeal.com/v2";
$API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";                     // APIUser token
$IMAGE_PATH = "<path/to/multiple_items_meal.jpg>";            // Local JPEG path
$url = $BASE_URL . "/image/segmentation/complete";

// === Step 2: POST image & get JSON ===
$ch = curl_init();

$real_path = realpath($IMAGE_PATH);
if (!$real_path) {
    die("Error: Image not found at path: " . $IMAGE_PATH . "\n");
}

// multipart field name must be 'image'
$cfile = new CURLFile($real_path, 'image/jpeg', 'image.jpg');

$payload = [
    'image' => $cfile
];

curl_setopt_array($ch, [
    CURLOPT_URL => $url,
    CURLOPT_POST => true,
    CURLOPT_POSTFIELDS => $payload,
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_HTTPHEADER => [
        "Authorization: Bearer " . $API_USER_TOKEN
    ],
    CURLOPT_TIMEOUT => 30,
    CURLOPT_SSL_VERIFYPEER => false, // For local testing environments
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

$data = json_decode($response, true);

// === Step 3: Parse & print summary ===
echo "imageId: " . ($data['imageId'] ?? 'N/A') . "\n";

if (isset($data['occasion'])) {
    echo "occasion: " . $data['occasion'] . "\n";
}

$ft = $data['foodType'] ?? null;
if ($ft) {
    echo "foodType: " . $ft['name'] . " (id " . $ft['id'] . ")\n";
}

// Iterate each segmented food item / region
$results = $data['segmentation_results'] ?? [];
foreach ($results as $item) {
    $pos = $item['food_item_position'];
    $bb = $item['contained_bbox'] ?? [];
    $center = $item['center'] ?? [];
    $grams = $item['serving_size'] ?? null; // may be absent depending on plan
    $recs = $item['recognition_results'] ?? [];
    $top = $recs[0] ?? null;

    $line = "[#{$pos}] bbox={'x': {$bb['x']}, 'y': {$bb['y']}, 'w': {$bb['w']}, 'h': {$bb['h']}} center={'x': {$center['x']}, 'y': {$center['y']}}";
    if ($grams !== null) {
        $line .= " grams=" . $grams;
    }
    echo $line . "\n";

    if ($top) {
        $prob = $top['prob'] ?? null;
        if (is_numeric($prob)) {
            $prob = round($prob, 3);
        }
        echo "  top dish: " . $top['name'] . " (id " . $top['id'] . ", prob " . $prob . ")\n";

        // extra: print top-5 candidates for this region
        $cands = [];
        $top5 = array_slice($recs, 0, 5);
        foreach ($top5 as $r) {
            $p = $r['prob'] ?? null;
            if (is_numeric($p)) {
                $p = round($p, 3);
            }
            $cands[] = $r['name'] . " (id " . $r['id'] . ", prob " . $p . ")";
        }
        echo "  top-5 candidates: " . implode("; ", $cands) . "\n";
    }
}