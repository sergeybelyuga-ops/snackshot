####################################################################################
# CODE FOR LOCAL TESTING, DO NOT COPY THIS INTO THE README.COM RECIPE

BASE_URL = "http://localhost:8080/v2"
API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>"  # APIUser token
IMAGE_ID = "1981523"  # Obtained from previous /image/segmentation/complete

####################################################################################

# CODE TO BE COPIED TO README.COM

import requests

# === Step 1: Configure inputs & endpoint ===
BASE_URL = "https://api.logmeal.com/v2"
API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>"                   # APIUser token
IMAGE_ID = "<imageId-from-previous-step>"                 # From segmentation
url = f"{BASE_URL}/nutrition/confirm/quantity"           # confirm food quantities
headers = {"Authorization": f"Bearer {API_USER_TOKEN}"}   # Bearer auth

# === Step 2: Build edits & POST ===
# Example: set/adjust per-item serving sizes (by region)
# - Use food_item_positions returned by the previous recipe endpoint
# - Values are quantities in grams (or ml for liquids)
edits = {
    1: 50.0,   # steamed broccoli
    2: 180.2,   # schnitzel
    3: 30.0,    # mayonnaise
    4: 120.0    # baked potatoes
}

payload = {"imageId": IMAGE_ID, "quantity": edits}
resp = requests.post(url, headers=headers, json=payload)

# Raise for HTTP errors (e.g., 401 bad token, 429 rate limit)
resp.raise_for_status()
