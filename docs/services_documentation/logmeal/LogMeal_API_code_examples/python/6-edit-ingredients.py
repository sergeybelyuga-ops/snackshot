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
url = f"{BASE_URL}/nutrition/confirm/ingredients"        # confirm edits
headers = {"Authorization": f"Bearer {API_USER_TOKEN}"}   # Bearer auth

# === Step 2: Build edits & POST ===
# Example: adjust quantities and swap ingredients per detected item
# - Use food_item_positions returned by the previous recipe endpoint
# - 'ingredientAmount' is in grams (or ml for liquids)
edits = {
    1: [  # food_item_position
        {"ingredientId": 1478, "ingredientAmount": 85.0},  # chicken (boneless) -> 85 g
        {"ingredientId": 61,   "ingredientAmount": 12.0},  # bread crumbles       -> 12 g
        {"ingredientId": 562,  "ingredientAmount": 12.0}   # oil             -> 12 ml
    ],
    2: [  # food_item_position
        {"ingredientId": 204,  "ingredientAmount": 220.0}, # potato -> 220 g
        {"ingredientId": 562,  "ingredientAmount": 8.0},   # oil -> 8 ml
        {"ingredientId": 45,   "ingredientAmount": 1.2}    # salt    -> 1.2 g
    ]
}


payload = {"imageId": IMAGE_ID, "ingredients_per_item": edits}
resp = requests.post(url, headers=headers, json=payload)

# Raise for HTTP errors (e.g., 401 bad token, 429 rate limit)
resp.raise_for_status()
