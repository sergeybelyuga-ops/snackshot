const axios = require('axios');

/**
 * ####################################################################################
 * # CODE FOR LOCAL TESTING, DO NOT COPY THIS INTO THE README.COM RECIPE
 * * BASE_URL = "http://localhost:8080/v2"
 * API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>"
 * IMAGE_ID = "1981523"
 * ####################################################################################
 */

// === Step 1: Configure inputs & endpoint ===
const BASE_URL: string = "https://api.logmeal.com/v2";
const API_USER_TOKEN: string = "<YOUR_APIUSER_TOKEN>";                   // APIUser token
const IMAGE_ID: string = "<imageId-from-previous-step>";                 // From segmentation
const url: string = `${BASE_URL}/nutrition/confirm/quantity`;           // confirm food quantities
const headers = { "Authorization": `Bearer ${API_USER_TOKEN}` };          // Bearer auth

// === Step 2: Build edits & POST ===
/**
 * Example: set/adjust per-item serving sizes (by region)
 * - Use food_item_positions returned by the previous recipe endpoint
 * - Values are quantities in grams (or ml for liquids)
 */
const edits: { [key: number]: number } = {
    1: 50.0,    // steamed broccoli
    2: 180.2,   // schnitzel
    3: 30.0,    // mayonnaise
    4: 120.0    // baked potatoes
};

async function runRecipe(): Promise<void> {
    try {
        const payload = { 
            "imageId": IMAGE_ID, 
            "quantity": edits 
        };

        const response = await axios.post(url, payload, {
            headers: headers,
            timeout: 30000
        });

        // The API returns {"result": "OK"} on success
        console.log("Quantities updated successfully");
        if (response.data) {
            console.log(JSON.stringify(response.data, null, 2));
        }

    } catch (error: any) {
        if (error.response) {
            console.error(`HTTP Error ${error.response.status}: ${JSON.stringify(error.response.data)}`);
        } else {
            console.error("Error:", error.message);
        }
    }
}

runRecipe();