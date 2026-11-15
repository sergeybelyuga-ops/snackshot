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
const url = `${BASE_URL}/nutrition/confirm/ingredients`;        // confirm edits
const headers = { "Authorization": `Bearer ${API_USER_TOKEN}` };   // Bearer auth

async function editIngredients() {
    try {
        // === Step 2: Build edits & POST ===
        // Example: adjust quantities and swap ingredients per detected item
        // - Use food_item_positions returned by the previous recipe endpoint
        // - 'ingredientAmount' is in grams (or ml for liquids)
        const edits = {
            1: [  // food_item_position
                { "ingredientId": 1478, "ingredientAmount": 85.0 },  // chicken (boneless) -> 85 g
                { "ingredientId": 61,   "ingredientAmount": 12.0 },  // bread crumbles       -> 12 g
                { "ingredientId": 562,  "ingredientAmount": 12.0 }   // oil             -> 12 ml
            ],
            2: [  // food_item_position
                { "ingredientId": 204,  "ingredientAmount": 220.0 }, // potato -> 220 g
                { "ingredientId": 562,  "ingredientAmount": 8.0 },   // oil -> 8 ml
                { "ingredientId": 45,   "ingredientAmount": 1.2 }    // salt    -> 1.2 g
            ]
        };

        const payload = { "imageId": IMAGE_ID, "ingredients_per_item": edits };
        const resp = await axios.post(url, payload, { headers, timeout: 30000 });

        // Raise for HTTP errors (Axios does this by default if response is not 2xx)
        console.log("Ingredients edited -> OK");
        console.log("Status:", resp.status);

    } catch (error) {
        console.error("Error:", error.response ? error.response.data : error.message);
    }
}

editIngredients();