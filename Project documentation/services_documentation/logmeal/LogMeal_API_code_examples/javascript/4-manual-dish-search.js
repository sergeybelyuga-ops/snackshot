const axios = require('axios');

/**
 * ####################################################################################
 * # CODE FOR LOCAL TESTING
 * const BASE_URL = "http://localhost:8080/v2";
 * const API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";
 * ####################################################################################
 */

// === Step 1: Configure inputs ===
const BASE_URL = "https://api.logmeal.com/v2";
const API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";   // APIUser token (Bearer)
const IMAGE_ID = 1981521;                        // obtained from a previous segmentation/complete
const USER_QUERY = "miso soup";                  // what the user typed
const TARGET_REGION = 2;                         // region to correct (food_item_position)
const LANGUAGE = "eng";                          // language of the dishes search

const headers = {
    "Authorization": `Bearer ${API_USER_TOKEN}`,
    "Content-Type": "application/json"
};

// (Example) Current mapping you plan to CONFIRM for the WHOLE image.
// IMPORTANT: /image/confirm/dish OVERWRITES the image; any region NOT SENT here gets REMOVED.
// Fill this from your UI / prior step. We'll correct TARGET_REGION with the search result below.
let confirmed_classes = [110, null];           // e.g., region #1 is donuts (id=110), region #2 to be filled
const food_item_positions = [1, 2];            // parallel to confirmed_classes
const sources = ["logmeal", "logmeal"];        // 'logmeal' for catalog ids, 'other' for free-text

function flatten_dishes(payload) {
    let flat = [];
    if (payload && typeof payload === 'object' && !Array.isArray(payload)) {
        for (const group in payload) {
            const items = payload[group];
            if (Array.isArray(items)) {
                items.forEach(it => {
                    const name = (it || {}).name;
                    const did = (it || {}).id;
                    if (name && did !== undefined && did !== null) {
                        flat.push({ id: did, name: name });
                    }
                });
            }
        }
    } else if (Array.isArray(payload)) {
        payload.forEach(it => {
            const name = (it || {}).name;
            const did = (it || {}).id;
            if (name && did !== undefined && did !== null) {
                flat.push({ id: did, name: name });
            }
        });
    }
    return flat;
}

async function manualDishSearch() {
    try {
        // === Step 2: Search the dish catalog (GET /dataset/dishes) ===
        // The endpoint returns several top-level groups (e.g., "food", "drinks", "combo", "ingredients", ...).
        // We'll flatten them and do a simple case-insensitive substring match on "name".
        const ds_url = `${BASE_URL}/dataset/dishes`;
        const resp_ds = await axios.get(ds_url, { 
            headers: { "Authorization": `Bearer ${API_USER_TOKEN}` },
            params: { 'language': LANGUAGE }
        });
        
        const catalog = resp_ds.data;
        const all_dishes = flatten_dishes(catalog);
        const q = USER_QUERY.toLowerCase().trim();
        const hits = all_dishes.filter(d => d.name.toLowerCase().includes(q));

        // Map the user query to a dish id if found; otherwise fall back to free-text
        if (hits.length > 0) {
            const picked = hits[0];
            const dish_id = picked.id || picked.dishId || picked.class_id;
            // Put the found id into the arrays at TARGET_REGION’s position
            const idx = food_item_positions.indexOf(TARGET_REGION);
            if (idx !== -1) {
                confirmed_classes[idx] = dish_id;
            }
        }

        // === Step 3: Confirm/correct dishes ===
        const dish_url = `${BASE_URL}/image/confirm/dish`;
        const dish_payload = {
            "imageId": IMAGE_ID,
            "confirmedClass": confirmed_classes,
            "food_item_position": food_item_positions,
            "source": sources
        };
        const dish_resp = await axios.post(dish_url, dish_payload, { headers, timeout: 30000 });
        
        console.log("Dish confirmation -> OK");
        console.log(JSON.stringify(dish_resp.data, null, 2));

    } catch (error) {
        console.error("Error:", error.response ? error.response.data : error.message);
    }
}

manualDishSearch();