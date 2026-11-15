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
const url: string = `${BASE_URL}/nutrition/confirm/ingredients`;         // confirm edits
const headers = { "Authorization": `Bearer ${API_USER_TOKEN}` };          // Bearer auth

// === Step 2: Build edits & POST ===
/**
 * Example: adjust quantities and swap ingredients per detected item
 * - Use food_item_positions returned by the previous recipe endpoint
 * - 'ingredientAmount' is in grams (or ml for liquids)
 */
const edits: { [key: number]: any[] } = {
    1: [  // food_item_position
        { "ingredientId": 1478, "ingredientAmount": 85.0 },  // chicken (boneless) -> 85 g
        { "ingredientId": 61,   "ingredientAmount": 12.0 },  // bread crumbles     -> 12 g
        { "ingredientId": 562,  "ingredientAmount": 12.0 }   // oil                -> 12 ml
    ],
    2: [  // food_item_position
        { "ingredientId": 204,  "ingredientAmount": 220.0 }, // potato -> 220 g
        { "ingredientId": 562,  "ingredientAmount": 8.0 },   // oil    -> 8 ml
        { "ingredientId": 45,   "ingredientAmount": 1.2 }    // salt   -> 1.2 g
    ]
};

async function runRecipe(): Promise<void> {
    try {
        const payload = { 
            "imageId": IMAGE_ID, 
            "ingredients_per_item": edits 
        };

        const response = await axios.post(url, payload, {
            headers: headers,
            timeout: 30000
        });

        // Raise for HTTP errors (already handled by catch block in axios)
        console.log("Ingredients updated successfully");
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