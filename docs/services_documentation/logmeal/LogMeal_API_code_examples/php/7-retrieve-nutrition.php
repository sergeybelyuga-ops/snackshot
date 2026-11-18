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
$url = $BASE_URL . "/nutrition/recipe/nutritionalInfo";    // default latest model
$headers = ["Authorization: Bearer " . $API_USER_TOKEN];   // Bearer auth

// === Step 2: Request nutritional info ===
$payload = ["imageId" => $IMAGE_ID];
$ch = curl_init();

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

if ($httpCode !== 200) {
    die("HTTP Error {$httpCode}: {$response}\n");
}

$data = json_decode($response, true);

// === Step 3: Parse & print summary (matches provided structure) ===
echo "imageId: " . ($data['imageId'] ?? $IMAGE_ID) . "\n";
if (isset($data['serving_size'])) {
    echo "meal_serving_size: " . $data['serving_size'] . " g\n";
}

// Optional: overall Nutri-Score (if present)
if (isset($data['image_nutri_score']) && is_array($data['image_nutri_score'])) {
    $ns = $data['image_nutri_score'];
    echo "nutri_score: " . ($ns['nutri_score_category'] ?? 'N/A') . " (" . ($ns['nutri_score_standardized'] ?? 'N/A') . ")\n";
}

// Meal-level totals
$total = $data['nutritional_info'] ?? [];
if (!empty($total)) {
    $cal = $total['calories'] ?? null;
    if ($cal !== null) {
        echo "calories_total: " . round($cal, 2) . " kcal\n";
    }

    // Daily Intake Reference (DIR) summary
    $dirf = $total['dailyIntakeReference'] ?? [];
    if (!empty($dirf)) {
        echo "Daily Intake Reference (meal):\n";
        $codes = ['ENERC_KCAL','PROCNT','FAT','CHOCDF','FIBTG','SUGAR','FASAT','NA'];
        foreach ($codes as $k) {
            if (isset($dirf[$k]) && is_array($dirf[$k])) {
                $v = $dirf[$k];
                $label = $v['label'] ?? $k;
                $level = $v['level'] ?? 'N/A';
                $pct = isset($v['percent']) ? round((float)$v['percent'], 1) : 'N/A';
                echo "  {$label}: {$pct}% ({$level})\n";
            }
        }
    }

    // Key nutrients (totals) helper function
    $fmt = function($n) {
        if (!is_array($n)) return null;
        $q = $n['quantity'] ?? null;
        $u = $n['unit'] ?? null;
        $lab = $n['label'] ?? null;
        if ($q === null || $u === null) return null;
        return "{$lab}: " . round((float)$q, 2) . " {$u}";
    };

    $tn = $total['totalNutrients'] ?? [];
    if (!empty($tn)) {
        $key_codes = ['PROCNT','FAT','CHOCDF','FIBTG','SUGAR'];
        $formatted = [];
        foreach ($key_codes as $c) {
            $res = $fmt($tn[$c] ?? null);
            if ($res) $formatted[] = $res;
        }
        echo "Nutrients (meal): " . implode("; ", $formatted) . "\n";
    }
}

// Per-item nutritional info
$name_map = [];
if (isset($data['ids'], $data['foodName']) && is_array($data['ids']) && is_array($data['foodName'])) {
    $name_map = array_combine($data['ids'], $data['foodName']);
}

$items = $data['nutritional_info_per_item'] ?? [];
if (!empty($items)) {
    foreach ($items as $it) {
        $pos = $it['food_item_position'] ?? 'N/A';
        $iid = $it['id'] ?? null;
        $dname = $name_map[$iid] ?? 'Unknown';
        $ss = $it['serving_size'] ?? 0;
        echo "[#{$pos}] dish={$dname} serving_size={$ss} g\n";

        // Item Nutri-Score
        if (isset($it['nutri_score']) && is_array($it['nutri_score'])) {
            $ns = $it['nutri_score'];
            echo "  nutri_score: " . ($ns['nutri_score_category'] ?? 'N/A') . " (" . ($ns['nutri_score_standardized'] ?? 'N/A') . ")\n";
        }

        $ni = $it['nutritional_info'] ?? [];
        $cal_i = $ni['calories'] ?? null;
        if ($cal_i !== null) {
            echo "  calories: " . round($cal_i, 2) . " kcal\n";
        }

        // Item nutrients and DIR
        $tn_i = $ni['totalNutrients'] ?? [];
        $dir_i = $ni['dailyIntakeReference'] ?? [];
        
        if (!empty($tn_i)) {
            $formatted_i = [];
            foreach (['PROCNT','FAT','CHOCDF','FIBTG','SUGAR'] as $c) {
                $res = $fmt($tn_i[$c] ?? null);
                if ($res) $formatted_i[] = $res;
            }
            echo "  nutrients: " . implode("; ", $formatted_i) . "\n";
        }

        if (!empty($dir_i)) {
            $out = [];
            foreach (['ENERC_KCAL','PROCNT','FAT','CHOCDF','FIBTG','SUGAR','FASAT','NA'] as $k) {
                if (isset($dir_i[$k]) && is_array($dir_i[$k]) && isset($dir_i[$k]['percent'])) {
                    $v = $dir_i[$k];
                    $pct = round((float)$v['percent'], 1);
                    $out[] = ($v['label'] ?? $k) . ": {$pct}%";
                }
            }
            if (!empty($out)) {
                echo "  daily intake: " . implode("; ", $out) . "\n";
            }
        }
    }
}