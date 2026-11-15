const axios = require('axios');

/**
 * ####################################################################################
 * # CODE FOR LOCAL TESTING
 * const BASE_URL = "http://localhost:8080/v2";
 * const API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";
 * ####################################################################################
 */

// === Step 1: Configure inputs & intended corrections ===
const BASE_URL = "https://api.logmeal.com/v2";
const API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";   // APIUser token (Bearer)
const IMAGE_ID = 1981521;  // obtained from a previous food intake insertion

/* Example mapping from food intake insertion results you want to confirm/correct:
# IMPORTANT: /image/confirm/dish OVERWRITES the image; any region NOT SENT here gets REMOVED.
# - Region #1 was recognized as "donuts" (id 110) and that's correct.
# - Region #2 should be corrected to "mushroom cream" (id 1474).
# - Add an extra dish not in any segment: free text "grandma soup".
*/

const confirmed_classes = [110, 1474, "grandma soup"];  // ids or free-text
const food_item_positions = [1, 2, "extra_dish_1"];     // int for segments, string for extra
const sources = ["logmeal", "logmeal", "other"];        // one entry per confirmed class

const url = `${BASE_URL}/image/confirm/dish`;
const headers = {
    "Authorization": `Bearer ${API_USER_TOKEN}`,
    "Content-Type": "application/json"
};

async function confirmDishes() {
    try {
        // === Step 2: Confirm/correct dishes (per segment or extra) ===
        const payload = {
            "imageId": IMAGE_ID,
            "confirmedClass": confirmed_classes,
            "food_item_position": food_item_positions,
            "source": sources
        };

        const response = await axios.post(url, payload, { 
            headers: headers,
            timeout: 30000 
        });

        console.log("Dish confirmation -> OK");
        console.log(JSON.stringify(response.data, null, 2));

    } catch (error) {
        console.error("Error:", error.response ? error.response.data : error.message);
    }
}

confirmDishes();