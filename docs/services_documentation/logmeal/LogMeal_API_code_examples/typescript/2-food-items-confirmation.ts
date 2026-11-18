const axios = require('axios');

/**
 * ####################################################################################
 * # CODE FOR LOCAL TESTING, DO NOT COPY THIS INTO THE README.COM RECIPE
 * * BASE_URL = "http://localhost:8080/v2"
 * API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>"
 * ####################################################################################
 */

// === Step 1: Configure inputs & intended corrections ===
const BASE_URL: string = "https://api.logmeal.com/v2";
const API_USER_TOKEN: string = "<YOUR_APIUSER_TOKEN>";   // APIUser token (Bearer)
const IMAGE_ID: number = 1981521;                        // obtained from a previous food intake insertion

/**
 * Example mapping from food intake insertion results you want to confirm/correct:
 * IMPORTANT: /image/confirm/dish OVERWRITES the image; any region NOT SENT here gets REMOVED.
 * - Region #1 was recognized as "donuts" (id 110) and that's correct.
 * - Region #2 should be corrected to "mushroom cream" (id 1474).
 * - Add an extra dish not in any segment: free text "grandma soup".
 */
const confirmed_classes: (number | string)[] = [110, 1474, "grandma soup"]; // ids or free-text
const food_item_positions: (number | string)[] = [1, 2, "extra_dish_1"];    // int for segments, string for extra
const sources: string[] = ["logmeal", "logmeal", "other"];                  // one entry per confirmed class

const url: string = `${BASE_URL}/image/confirm/dish`;
const headers = { 
    "Authorization": `Bearer ${API_USER_TOKEN}`,
    "Content-Type": "application/json" 
};

async function runRecipe(): Promise<void> {
    try {
        // === Step 2: Confirm/correct dishes (per segment or extra) ===
        const dish_payload = {
            "imageId": IMAGE_ID,
            "confirmedClass": confirmed_classes,
            "food_item_position": food_item_positions,
            "source": sources
        };

        const response = await axios.post(url, dish_payload, {
            headers: headers,
            timeout: 30000
        });

        const dish_data: any = response.data;
        console.log("Dish confirmation -> OK");
        console.log(JSON.stringify(dish_data, null, 2));

    } catch (error: any) {
        if (error.response) {
            console.error(`HTTP Error ${error.response.status}: ${JSON.stringify(error.response.data)}`);
        } else {
            console.error("Error:", error.message);
        }
    }
}

runRecipe();