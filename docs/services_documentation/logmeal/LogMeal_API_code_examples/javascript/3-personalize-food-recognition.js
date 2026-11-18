const axios = require('axios');

/**
 * ####################################################################################
 * # CODE FOR LOCAL TESTING
 * const BASE_URL = "http://localhost:8080/v2";
 * const API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";
 * ####################################################################################
 */

// -----------------------------
// Step 1 - Prepare configuration & inputs
// -----------------------------
const BASE_URL = "https://api.logmeal.com/v2";
const API_USER_TOKEN = "<REPLACE_WITH_APIUSER_TOKEN>";  // APIUser token

const headers = {
    "Authorization": `Bearer ${API_USER_TOKEN}`,
    "Content-Type": "application/json"
};

// Define preferences (ISO country code + ISO language code)
const preferences = {
    "country": "ES",   // Spain
    "language": "spa"   // Spanish
};

async function run() {
    try {
        // -----------------------------
        // Step 2 - Send profile personalization request
        // -----------------------------
        const url = `${BASE_URL}/profile/modifyUserProfileInfo`;
        const resp = await axios.post(url, preferences, { headers, timeout: 30000 });

        // -----------------------------
        // Step 3 - Confirm personalization response
        // -----------------------------
        console.log("Profile updated:", JSON.stringify(resp.data, null, 2));

    } catch (err) {
        const status = err.response ? err.response.status : 'Error';
        const text = err.response ? JSON.stringify(err.response.data) : err.message;
        console.error(`Failed to personalize profile (${status}): ${text}`);
        process.exit(1);
    }
}

run();

// -----------------------------
// Step 4 - Use personalization in subsequent recognition requests
// -----------------------------
// After this call, all recognition endpoints (e.g., /image/segmentation/complete)
// will return dish, ingredients and nutrient labels in the chosen language and
// prioritized for the selected country.