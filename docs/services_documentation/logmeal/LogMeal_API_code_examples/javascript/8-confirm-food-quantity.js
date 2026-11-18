const axios = require('axios');

/**
 * ####################################################################################
 * # CODE FOR LOCAL TESTING
 * const BASE_URL = "http://localhost:8080/v2";
 * const API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";
 * const IMAGE_ID = "1981523";
 * ####################################################################################
 */

// === Step 1: Configure inputs & endpoint ===
const BASE_URL = "https://api.logmeal.com/v2";
const API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";                   // APIUser token
const IMAGE_ID = "<imageId-from-previous-step>";                 // From segmentation
const url = `${BASE_URL}/nutrition/confirm/quantity`;           // confirm food quantities
const headers = { "Authorization": `Bearer ${API_USER_TOKEN}` };   // Bearer auth

async function confirmFoodQuantity() {
    try {
        // === Step 2: Build edits & POST ===
        // Example: set/adjust per-item serving sizes (by region)
        // - Use food_item_positions returned by the previous recipe endpoint
        // - Values are quantities in grams (or ml for liquids)
        const edits = {
            1: 50.0,    // steamed broccoli
            2: 180.2,   // schnitzel
            3: 30.0,    // mayonnaise
            4: 120.0    // baked potatoes
        };

        const payload = { "imageId": IMAGE_ID, "quantity": edits };
        const resp = await axios.post(url, payload, { headers, timeout: 30000 });

        // Raise for HTTP errors (Axios handles this automatically)
        console.log("Quantities confirmed -> OK");
        console.log("Status:", resp.status);

    } catch (error) {
        console.error("Error:", error.response ? error.response.data : error.message);
    }
}

confirmFoodQuantity();