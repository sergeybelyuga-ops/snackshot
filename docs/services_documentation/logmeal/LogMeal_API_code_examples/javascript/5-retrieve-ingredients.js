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
const API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";                  // APIUser token
const IMAGE_ID = "<imageId-from-previous-step>";                // From segmentation
const url = `${BASE_URL}/nutrition/recipe/ingredients`;        // default latest model
const headers = { "Authorization": `Bearer ${API_USER_TOKEN}` };  // Bearer auth

async function retrieveIngredients() {
    try {
        // === Step 2: Request ingredients & quantities ===
        const payload = { "imageId": IMAGE_ID };
        const resp = await axios.post(url, payload, { headers, timeout: 30000 });
        const data = resp.data;

        // === Step 3: Parse & print summary (matches provided structure) ===
        console.log(`imageId: ${data.imageId || IMAGE_ID}`);
        if (data.serving_size) {
            console.log(`meal_serving_size: ${data.serving_size} g`);
        }

        // Prefer per-item recipes if present
        const items = data.recipe_per_item || [];

        if (items.length > 0) {
            items.forEach(it => {
                const pos = it.food_item_position;
                const dname = it.name;
                const ss = it.serving_size;
                const unit = it.unit;
                console.log(`[#${pos}] dish=${dname} serving_size=${ss} ${unit}`);

                // Ingredients for this dish
                (it.recipe || []).forEach(ing => {
                    const iid = ing.id;
                    const name = ing.name;
                    const weight = ing.weight;
                    const unit_i = ing.unit || 'g';
                    const meas = ing.measure || {};
                    const metric = meas.metric || {};
                    const preferred = meas.preferred || {};

                    // Metric (canonical) amount
                    const m_qty = metric.quantity !== undefined ? metric.quantity : weight;
                    const m_short = metric.short || unit_i;

                    // Preferred (human-friendly) measure if available
                    const p_qty = preferred.quantity;
                    const p_name = preferred.name;

                    let line = `  - ${name} (id ${iid}) -> ${m_qty} ${m_short}`;
                    if (p_qty !== undefined && p_qty !== null && p_name) {
                        line += ` (~${p_qty} ${p_name})`;
                    }
                    console.log(line);
                });
            });
        } else {
            // Fallback 1: show flat global recipe
            const ingr_list = data.recipe || [];
            if (ingr_list.length > 0) {
                console.log("Global recipe:");
                ingr_list.forEach(ing => {
                    const iid = ing.id;
                    const name = ing.name;
                    const weight = ing.weight;
                    const unit_i = ing.unit || 'g';
                    const meas = ing.measure || {};
                    const metric = meas.metric || {};
                    const preferred = meas.preferred || {};

                    const m_qty = metric.quantity !== undefined ? metric.quantity : weight;
                    const m_short = metric.short || unit_i;

                    const p_qty = preferred.quantity;
                    const p_name = preferred.name;

                    let line = `  - ${name} (id ${iid}) -> ${m_qty} ${m_short}`;
                    if (p_qty !== undefined && p_qty !== null && p_name) {
                        line += ` (~${p_qty} ${p_name})`;
                    }
                    console.log(line);
                });
            } else {
                // Fallback 2: generic fields (unlikely with this endpoint)
                const excludedFields = ['imageId', 'recipe_per_item', 'recipe', 'dish_id', 'foodName', 'is_combo', 'serving_size', 'source'];
                for (const [name, value] of Object.entries(data)) {
                    if (!excludedFields.includes(name)) {
                        console.log(`${name}: ${JSON.stringify(value)}`);
                    }
                }
            }
        }
    } catch (error) {
        console.error("Error:", error.response ? error.response.data : error.message);
    }
}

retrieveIngredients();