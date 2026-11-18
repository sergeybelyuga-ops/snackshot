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
const API_USER_TOKEN: string = "<YOUR_APIUSER_TOKEN>";                  // APIUser token
const IMAGE_ID: string = "<imageId-from-previous-step>";                // From segmentation
const url: string = `${BASE_URL}/nutrition/recipe/ingredients`;         // default latest model
const headers = { "Authorization": `Bearer ${API_USER_TOKEN}` };          // Bearer auth

async function runRecipe(): Promise<void> {
    try {
        // === Step 2: Request ingredients & quantities ===
        const payload = { "imageId": IMAGE_ID };
        const response = await axios.post(url, payload, {
            headers: headers,
            timeout: 30000
        });

        const data: any = response.data;

        // === Step 3: Parse & print summary ===
        console.log(`imageId: ${data.imageId || IMAGE_ID}`);
        if (data.serving_size) {
            console.log(`meal_serving_size: ${data.serving_size} g`);
        }

        // Prefer per-item recipes if present
        const items: any[] = data.recipe_per_item || [];

        if (items.length > 0) {
            items.forEach((it: any) => {
                const pos = it.food_item_position;
                const dname = it.name;
                const ss = it.serving_size;
                const unit = it.unit;
                console.log(`[#${pos}] dish=${dname} serving_size=${ss} ${unit}`);

                // Ingredients for this dish
                (it.recipe || []).forEach((ing: any) => {
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
            const ingr_list: any[] = data.recipe || [];
            if (ingr_list.length > 0) {
                console.log("Global recipe:");
                ingr_list.forEach((ing: any) => {
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
                // Fallback 2: generic fields
                const skipFields = ['imageId', 'recipe_per_item', 'recipe', 'dish_id', 'foodName', 'is_combo', 'serving_size', 'source'];
                for (const [key, value] of Object.entries(data)) {
                    if (!skipFields.includes(key)) {
                        console.log(`${key}: ${JSON.stringify(value)}`);
                    }
                }
            }
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