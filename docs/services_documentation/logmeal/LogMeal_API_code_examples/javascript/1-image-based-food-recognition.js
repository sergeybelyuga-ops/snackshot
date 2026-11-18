const axios = require('axios');
const FormData = require('form-data');
const fs = require('fs');

/**
 * ####################################################################################
 * # CODE FOR LOCAL TESTING
 * * const BASE_URL = "http://localhost:8080/v2";
 * const API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";
 * const IMAGE_PATH = "<path/to/multiple_items_meal.jpg>";
 * ####################################################################################
 */

// === Step 1: Configure inputs & endpoint ===
const BASE_URL = "https://api.logmeal.com/v2";
const API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";          // APIUser token
const IMAGE_PATH = "<path/to/multiple_items_meal.jpg>"; // Local JPEG path
const url = `${BASE_URL}/image/segmentation/complete`;

async function recognizeFood() {
    try {
        // === Step 2: POST image & get JSON ===
        const form = new FormData();
        form.append('image', fs.createReadStream(IMAGE_PATH)); // field name must be 'image'

        const response = await axios.post(url, form, {
            headers: {
                ...form.getHeaders(),
                'Authorization': `Bearer ${API_USER_TOKEN}`
            },
            timeout: 30000
        });

        const data = response.data;

        // === Step 3: Parse & print summary ===
        console.log(`imageId: ${data.imageId}`);
        if (data.occasion) {
            console.log(`occasion: ${data.occasion}`);
        }
        if (data.foodType) {
            console.log(`foodType: ${data.foodType.name} (id ${data.foodType.id})`);
        }

        // Iterate each segmented food item / region
        (data.segmentation_results || []).forEach(item => {
            const pos = item.food_item_position;
            const bb = item.contained_bbox || {};
            const center = item.center || {};
            const grams = item.serving_size;
            const recs = item.recognition_results || [];
            const top = recs[0];

            let line = `[#${pos}] bbox={'x': ${bb.x}, 'y': ${bb.y}, 'w': ${bb.w}, 'h': ${bb.h}} center={'x': ${center.x}, 'y': ${center.y}}`;
            if (grams !== undefined && grams !== null) {
                line += ` grams=${grams}`;
            }
            console.log(line);

            if (top) {
                const prob = typeof top.prob === 'number' ? top.prob.toFixed(3) : top.prob;
                console.log(`  top dish: ${top.name} (id ${top.id}, prob ${prob})`);

                // extra: print top-5 candidates
                const cands = recs.slice(0, 5).map(r => {
                    const p = typeof r.prob === 'number' ? r.prob.toFixed(3) : r.prob;
                    return `${r.name} (id ${r.id}, prob ${p})`;
                });
                console.log("  top-5 candidates: " + cands.join("; "));
            }
        });

    } catch (error) {
        console.error("Error:", error.response ? error.response.data : error.message);
    }
}

recognizeFood();