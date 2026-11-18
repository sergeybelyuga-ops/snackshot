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
$API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";                  // APIUser token
$IMAGE_ID = "<imageId-from-previous-step>";                // From segmentation
$url = $BASE_URL . "/nutrition/recipe/ingredients";        // default latest model
$headers = ["Authorization: Bearer " . $API_USER_TOKEN];   // Bearer auth

// === Step 2: Request ingredients & quantities ===
$ch = curl_init();

$payload = ["imageId" => $IMAGE_ID];

curl_setopt_array($ch, [
    CURLOPT_URL => $url,
    CURLOPT_POST => true,
    CURLOPT_POSTFIELDS => json_encode($payload),
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_HTTPHEADER => array_merge($headers, ["Content-Type: application/json"]),
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

// === Step 3: Parse & print summary (matches provided structure) ===
echo "imageId: " . ($data['imageId'] ?? $IMAGE_ID) . "\n";
if (isset($data['serving_size'])) {
    echo "meal_serving_size: " . $data['serving_size'] . " g\n";
}

// Prefer per-item recipes if present
$items = $data['recipe_per_item'] ?? [];

if (!empty($items)) {
    foreach ($items as $it) {
        $pos = $it['food_item_position'] ?? 'N/A';
        $dname = $it['name'] ?? 'Unknown';
        $ss = $it['serving_size'] ?? 0;
        $unit = $it['unit'] ?? 'g';
        echo "[#{$pos}] dish={$dname} serving_size={$ss} {$unit}\n";

        // Ingredients for this dish
        $recipe = $it['recipe'] ?? [];
        foreach ($recipe as $ing) {
            $iid = $ing['id'] ?? 'N/A';
            $name = $ing['name'] ?? 'Unknown';
            $weight = $ing['weight'] ?? 0;
            $unit_i = $ing['unit'] ?? 'g';
            $meas = $ing['measure'] ?? [];
            $metric = $meas['metric'] ?? [];
            $preferred = $meas['preferred'] ?? [];

            // Metric (canonical) amount
            $m_qty = isset($metric['quantity']) ? $metric['quantity'] : $weight;
            $m_short = $metric['short'] ?? $unit_i;

            // Preferred (human-friendly) measure if available
            $p_qty = $preferred['quantity'] ?? null;
            $p_name = $preferred['name'] ?? null;

            $line = "  - {$name} (id {$iid}) -> {$m_qty} {$m_short}";
            if ($p_qty !== null && $p_name) {
                $line .= " (~{$p_qty} {$p_name})";
            }
            echo $line . "\n";
        }
    }
} else {
    // Fallback 1: show flat global recipe
    $ingr_list = $data['recipe'] ?? [];
    if (!empty($ingr_list)) {
        echo "Global recipe:\n";
        foreach ($ingr_list as $ing) {
            $iid = $ing['id'] ?? 'N/A';
            $name = $ing['name'] ?? 'Unknown';
            $weight = $ing['weight'] ?? 0;
            $unit_i = $ing['unit'] ?? 'g';
            $meas = $ing['measure'] ?? [];
            $metric = $meas['metric'] ?? [];
            $preferred = $meas['preferred'] ?? [];

            $m_qty = isset($metric['quantity']) ? $metric['quantity'] : $weight;
            $m_short = $metric['short'] ?? $unit_i;

            $p_qty = $preferred['quantity'] ?? null;
            $p_name = $preferred['name'] ?? null;

            $line = "  - {$name} (id {$iid}) -> {$m_qty} {$m_short}";
            if ($p_qty !== null && $p_name) {
                $line .= " (~{$p_qty} {$p_name})";
            }
            echo $line . "\n";
        }
    } else {
        // Fallback 2: generic fields (unlikely with this endpoint)
        $exclude = ['imageId', 'recipe_per_item', 'recipe', 'dish_id', 'foodName', 'is_combo', 'serving_size', 'source'];
        foreach ($data as $name => $value) {
            if (!in_array($name, $exclude)) {
                echo "{$name}: " . json_encode($value, JSON_UNESCAPED_UNICODE) . "\n";
            }
        }
    }
}