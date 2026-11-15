const axios = require('axios');

/**
 * ####################################################################################
 * # CODE FOR LOCAL TESTING, DO NOT COPY THIS INTO THE README.COM RECIPE
 * * BASE_URL = "http://localhost:8080/v2"
 * API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>"
 * ####################################################################################
 */

// === Step 1: Prepare configuration & inputs ===
const BASE_URL: string = "https://api.logmeal.com/v2";
const API_USER_TOKEN: string = "<REPLACE_WITH_APIUSER_TOKEN>";  // APIUser token

const headers = {
    "Authorization": `Bearer ${API_USER_TOKEN}`,
    "Content-Type": "application/json"
};

// Define preferences (ISO country code + ISO language code)
const preferences = {
    "country": "ES",    // Spain
    "language": "spa"   // Spanish
};

async function runRecipe(): Promise<void> {
    try {
        // === Step 2: Send profile personalization request ===
        const url: string = `${BASE_URL}/profile/modifyUserProfileInfo`;
        
        const response = await axios.post(url, preferences, {
            headers: headers,
            timeout: 30000
        });

        // === Step 3: Confirm personalization response ===
        const profile_info: any = response.data;
        console.log("Profile updated:", JSON.stringify(profile_info, null, 2));

        /**
         * === Step 4: Use personalization in subsequent recognition requests ===
         * After this call, all recognition endpoints (e.g., /image/segmentation/complete)
         * will return dish, ingredients and nutrient labels in the chosen language and
         * prioritized for the selected country.
         */

    } catch (error: any) {
        if (error.response) {
            console.error(`Failed to personalize profile (${error.response.status}): ${JSON.stringify(error.response.data)}`);
        } else {
            console.error("Error:", error.message);
        }
    }
}

runRecipe();