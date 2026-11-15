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
const url = `${BASE_URL}/nutrition/recipe/nutritionalInfo`;    // default latest model
const headers = { "Authorization": `Bearer ${API_USER_TOKEN}` };  // Bearer auth

async function retrieveNutrition() {
    try {
        // === Step 2: Request nutritional info ===
        const payload = { "imageId": IMAGE_ID };
        const resp = await axios.post(url, payload, { headers, timeout: 30000 });
        const data = resp.data;

        // === Step 3: Parse & print summary (matches provided structure) ===
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
                console.log(`calories_total: ${cal.toFixed(2)} kcal`);
            }

            // Daily Intake Reference (meal) summary
            const dirf = total.dailyIntakeReference || {};
            if (Object.keys(dirf).length > 0) {
                console.log("Daily Intake Reference (meal):");
                ['ENERC_KCAL','PROCNT','FAT','CHOCDF','FIBTG','SUGAR','FASAT','NA'].forEach(k => {
                    const v = dirf[k];
                    if (v && typeof v === 'object') {
                        const label = v.label || k;
                        const level = v.level;
                        let pct = v.percent;
                        if (pct !== undefined && pct !== null) {
                            pct = parseFloat(pct).toFixed(1);
                        }
                        console.log(`  ${label}: ${pct}% (${level})`);
                    }
                });
            }

            // Key nutrients (totals)
            const tn = total.totalNutrients || {};
            if (Object.keys(tn).length > 0) {
                const _fmt = (n) => {
                    if (!n || typeof n !== 'object') return null;
                    const q = n.quantity; const u = n.unit; const lab = n.label;
                    if (q === undefined || u === undefined) return null;
                    return `${lab}: ${parseFloat(q).toFixed(2)} ${u}`;
                };
                const key_codes = ['PROCNT','FAT','CHOCDF','FIBTG','SUGAR'];
                const nutrientLines = key_codes.map(c => _fmt(tn[c])).filter(l => l !== null);
                console.log("Nutrients (meal): " + nutrientLines.join("; "));
            }
        }

        // Per-item nutritional info
        let name_map = {};
        if (Array.isArray(data.ids) && Array.isArray(data.foodName)) {
            data.ids.forEach((id, index) => {
                name_map[id] = data.foodName[index];
            });
        }

        const items = data.nutritional_info_per_item || [];
        if (items.length > 0) {
            items.forEach(it => {
                const pos = it.food_item_position;
                const iid = it.id;
                const dname = name_map[iid];
                const ss = it.serving_size;
                console.log(`[#${pos}] dish=${dname} serving_size=${ss} g`);

                // Item Nutri-Score (if present)
                if (it.nutri_score && typeof it.nutri_score === 'object') {
                    const ns = it.nutri_score;
                    console.log(`  nutri_score: ${ns.nutri_score_category} (${ns.nutri_score_standardized})`);
                }

                const ni = it.nutritional_info || {};
                const cal_i = ni.calories;
                if (cal_i !== undefined && cal_i !== null) {
                    console.log(`  calories: ${cal_i.toFixed(2)} kcal`);
                }

                // Item nutrients and DIR
                const tn_i = ni.totalNutrients || {};
                const dir_i = ni.dailyIntakeReference || {};
                
                if (Object.keys(tn_i).length > 0) {
                    const _fmt_i = (n) => {
                        if (!n || typeof n !== 'object') return null;
                        const q = n.quantity; const u = n.unit; const lab = n.label;
                        if (q === undefined || u === undefined) return null;
                        return `${lab}: ${parseFloat(q).toFixed(2)} ${u}`;
                    };
                    const key_codes = ['PROCNT','FAT','CHOCDF','FIBTG','SUGAR'];
                    const itemNutrientLines = key_codes.map(c => _fmt_i(tn_i[c])).filter(l => l !== null);
                    console.log("  nutrients: " + itemNutrientLines.join("; "));
                }

                if (Object.keys(dir_i).length > 0) {
                    let out = [];
                    ['ENERC_KCAL','PROCNT','FAT','CHOCDF','FIBTG','SUGAR','FASAT','NA'].forEach(k => {
                        const v = dir_i[k];
                        if (v && typeof v === 'object' && v.percent !== undefined && v.percent !== null) {
                            const pct = parseFloat(v.percent).toFixed(1);
                            out.push(`${v.label || k}: ${pct}%`);
                        }
                    });
                    if (out.length > 0) {
                        console.log("  daily intake: " + out.join("; "));
                    }
                }
            });
        }

    } catch (error) {
        console.error("Error:", error.response ? error.response.data : error.message);
    }
}

retrieveNutrition();