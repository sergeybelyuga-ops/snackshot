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
const url: string = `${BASE_URL}/nutrition/recipe/nutritionalInfo`;     // default latest model
const headers = { "Authorization": `Bearer ${API_USER_TOKEN}` };          // Bearer auth

// Shared formatting logic for nutrients
const key_codes = ['PROCNT','FAT','CHOCDF','FIBTG','SUGAR'];
const fmt = (n: any) => {
    if (!n || typeof n !== 'object') return null;
    const q = n.quantity; 
    const u = n.unit; 
    const lab = n.label;
    if (q === undefined || q === null || u === undefined) return null;
    return `${lab}: ${Number(q).toFixed(2)} ${u}`;
};

async function runRecipe(): Promise<void> {
    try {
        // === Step 2: Request nutritional info ===
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

        // Optional: overall Nutri-Score (if present)
        if (data.image_nutri_score && typeof data.image_nutri_score === 'object') {
            const ns = data.image_nutri_score;
            console.log(`nutri_score: ${ns.nutri_score_category} (${ns.nutri_score_standardized})`);
        }

        // Meal-level totals
        const total = data.nutritional_info || {};
        if (Object.keys(total).length > 0) {
            const cal = total.calories;
            if (cal !== undefined && cal !== null) {
                console.log(`calories_total: ${Number(cal).toFixed(2)} kcal`);
            }

            // Daily Intake Reference (DIR) summary
            const dirf = total.dailyIntakeReference || {};
            if (Object.keys(dirf).length > 0) {
                console.log("Daily Intake Reference (meal):");
                const dir_keys = ['ENERC_KCAL','PROCNT','FAT','CHOCDF','FIBTG','SUGAR','FASAT','NA'];
                dir_keys.forEach(k => {
                    const v = dirf[k];
                    if (v && typeof v === 'object') {
                        const label = v.label || k;
                        const level = v.level;
                        const pct = v.percent !== undefined ? Number(v.percent).toFixed(1) : null;
                        if (pct !== null) {
                            console.log(`  ${label}: ${pct}% (${level})`);
                        }
                    }
                });
            }

            // Key nutrients (totals)
            const tn = total.totalNutrients || {};
            const nutrientsLine = key_codes
                .map(c => fmt(tn[c]))
                .filter(res => res !== null)
                .join("; ");
            
            if (nutrientsLine) {
                console.log("Nutrients (meal): " + nutrientsLine);
            }
        }

        // Per-item nutritional info
        const name_map: { [key: number]: string } = {};
        if (Array.isArray(data.ids) && Array.isArray(data.foodName)) {
            data.ids.forEach((id: number, index: number) => {
                name_map[id] = data.foodName[index];
            });
        }

        const items: any[] = data.nutritional_info_per_item || [];
        if (items.length > 0) {
            items.forEach((it: any) => {
                const pos = it.food_item_position;
                const iid = it.id;
                const dname = name_map[iid] || "Unknown";
                const ss = it.serving_size;
                console.log(`[#${pos}] dish=${dname} serving_size=${ss} g`);

                if (it.nutri_score && typeof it.nutri_score === 'object') {
                    const ns = it.nutri_score;
                    console.log(`  nutri_score: ${ns.nutri_score_category} (${ns.nutri_score_standardized})`);
                }

                const ni = it.nutritional_info || {};
                const cal_i = ni.calories;
                if (cal_i !== undefined && cal_i !== null) {
                    console.log(`  calories: ${Number(cal_i).toFixed(2)} kcal`);
                }

                const tn_i = ni.totalNutrients || {};
                const dir_i = ni.dailyIntakeReference || {};

                if (Object.keys(tn_i).length > 0) {
                    const itemNutrients = key_codes
                        .map(c => fmt(tn_i[c]))
                        .filter(res => res !== null)
                        .join("; ");
                    if (itemNutrients) {
                        console.log("  nutrients: " + itemNutrients);
                    }
                }

                if (Object.keys(dir_i).length > 0) {
                    const dir_keys = ['ENERC_KCAL','PROCNT','FAT','CHOCDF','FIBTG','SUGAR','FASAT','NA'];
                    const out = dir_keys
                        .map(k => {
                            const v = dir_i[k];
                            return (v && v.percent !== undefined) ? `${v.label}: ${Number(v.percent).toFixed(1)}%` : null;
                        })
                        .filter(res => res !== null);
                    if (out.length > 0) {
                        console.log("  daily intake: " + out.join("; "));
                    }
                }
            });
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