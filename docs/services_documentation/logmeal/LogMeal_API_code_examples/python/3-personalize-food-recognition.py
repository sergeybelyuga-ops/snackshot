
####################################################################################
# CODE FOR LOCAL TESTING, DO NOT COPY THIS INTO THE README.COM RECIPE

BASE_URL = "http://localhost:8080/v2"
API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>"  # APIUser token

####################################################################################

# CODE TO BE COPIED TO README.COM

import requests
import json

# -----------------------------
# Step 1 - Prepare configuration & inputs
# -----------------------------
BASE_URL = "https://api.logmeal.com/v2"
API_USER_TOKEN = "<REPLACE_WITH_APIUSER_TOKEN>"  # APIUser token

headers = {
    "Authorization": f"Bearer {API_USER_TOKEN}",
    "Content-Type": "application/json"
}

# Define preferences (ISO country code + ISO language code)
preferences = {
    "country": "ES",   # Spain
    "language": "spa"   # Spanish
}

# -----------------------------
# Step 2 - Send profile personalization request
# -----------------------------
resp = requests.post(f"{BASE_URL}/profile/modifyUserProfileInfo", headers=headers, json=preferences)

# -----------------------------
# Step 3 - Confirm personalization response
# -----------------------------
if resp.status_code != 200:
    raise SystemExit(f"Failed to personalize profile ({resp.status_code}): {resp.text}")

profile_info = resp.json()
print("Profile updated:", json.dumps(profile_info, indent=2))

# -----------------------------
# Step 4 - Use personalization in subsequent recognition requests
# -----------------------------
# After this call, all recognition endpoints (e.g., /image/segmentation/complete)
# will return dish, ingredients and nutrient labels in the chosen language and
# prioritized for the selected country.

