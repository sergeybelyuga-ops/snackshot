const axios = require('axios');

/**
 * ####################################################################################
 * # CODE FOR LOCAL TESTING, DO NOT COPY THIS INTO THE README.COM RECIPE
 * * BASE_URL = "http://localhost:8080/v2"
 * API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>"
 * ####################################################################################
 */

// === Step 1: Configure inputs ===
const BASE_URL: string = "https://api.logmeal.com/v2";
const API_USER_TOKEN: string = "<YOUR_APIUSER_TOKEN>";   // APIUser token (Bearer)
const IMAGE_ID: number = 1981521;                        // obtained from a previous segmentation/complete
const USER_QUERY: string = "miso soup";                  // what the user typed
const TARGET_REGION: number = 2;                         // region to correct (food_item_position)
const LANGUAGE: string = "eng";                          // language of the dishes search

const headers = {
    "Authorization": `Bearer ${API_USER_TOKEN}`,
    "Content-Type": "application/json"
};

// (Example) Current mapping you plan to CONFIRM for the WHOLE image.
// IMPORTANT: /image/confirm/dish OVERWRITES the image; any region NOT SENT here gets REMOVED.
let confirmed_classes: (number | string | null)[] = [110, null]; // e.g., region #1 is donuts (id=110)
const food_item_positions: (number | string)[] = [1, 2];         // parallel to confirmed_classes
const sources: string[] = ["logmeal", "logmeal"];                // 'logmeal' for catalog ids

/**
 * Helper function to flatten the dishes catalog response.
 */
function flattenDishes(payload: any): any[] {
    const flat: any[] = [];
    if (payload && typeof payload === 'object' && !Array.isArray(payload)) {
        for (const key in payload) {
            const items = payload[key];
            if (Array.isArray(items)) {
                items.forEach(it => {
                    if (it && it.name && it.id !== undefined) {
                        flat.push({ id: it.id, name: it.name });
                    }
                });
            }
        }
    } else if (Array.isArray(payload)) {
        payload.forEach(it => {
            if (it && it.name && it.id !== undefined) {
                flat.push({ id: it.id, name: it.name });
            }
        });
    }
    return flat;
}

async function runRecipe(): Promise<void> {
    try {
        // === Step 2: Search the dish catalog (GET /dataset/dishes) ===
        const ds_url: string = `${BASE_URL}/dataset/dishes`;
        const resp_ds = await axios.get(ds_url, {
            headers: { "Authorization": `Bearer ${API_USER_TOKEN}` },
            params: { 'language': LANGUAGE },
            timeout: 30000
        });

        const catalog = resp_ds.data;
        const all_dishes = flattenDishes(catalog);
        const q = USER_QUERY.toLowerCase().trim();
        
        // Simple case-insensitive substring match on "name"
        const hits = all_dishes.filter(d => d.name.toLowerCase().includes(q));

        // Map the user query to a dish id if found; otherwise fall back to free-text
        if (hits.length > 0) {
            const picked = hits[0];
            const dish_id = picked.id;
            // Put the found id into the arrays at TARGET_REGION’s position
            const idx = food_item_positions.indexOf(TARGET_REGION);
            if (idx !== -1) {
                confirmed_classes[idx] = dish_id;
            }
        }

        // === Step 3: Confirm/correct dishes ===
        const dish_url: string = `${BASE_URL}/image/confirm/dish`;
        const dish_payload = {
            "imageId": IMAGE_ID,
            "confirmedClass": confirmed_classes,
            "food_item_position": food_item_positions,
            "source": sources
        };

        const dish_resp = await axios.post(dish_url, dish_payload, {
            headers: headers,
            timeout: 30000
        });

        console.log("Dish confirmation -> OK");
        console.log(JSON.stringify(dish_resp.data, null, 2));

    } catch (error: any) {
        if (error.response) {
            console.error(`HTTP Error ${error.response.status}: ${JSON.stringify(error.response.data)}`);
        } else {
            console.error("Error:", error.message);
        }
    }
}

runRecipe();