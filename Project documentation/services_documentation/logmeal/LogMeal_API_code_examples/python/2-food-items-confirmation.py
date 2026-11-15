
####################################################################################
# CODE FOR LOCAL TESTING, DO NOT COPY THIS INTO THE README.COM RECIPE

BASE_URL = "http://localhost:8080/v2"
API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>"  # APIUser token

####################################################################################

# CODE TO BE COPIED TO README.COM

import requests
import json

# === Step 1: Configure inputs & intended corrections ===
BASE_URL = "https://api.logmeal.com/v2"
API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>"   # APIUser token (Bearer)
IMAGE_ID = 1981521  # obtained from a previous food intake insertion

# Example mapping from food intake insertion results you want to confirm/correct:
# IMPORTANT: /image/confirm/dish OVERWRITES the image; any region NOT SENT here gets REMOVED.
# - Region #1 was recognized as "donuts" (id 110) and that's correct.
# - Region #2 should be corrected to "mushroom cream" (id 1474).
# - Add an extra dish not in any segment: free text "grandma soup".
confirmed_classes = [110, 1474, "grandma soup"]  # ids or free-text (when source='other')
food_item_positions = [1, 2, "extra_dish_1"]     # int for segments, string for extra dish
sources = ["logmeal", "logmeal", "other"]        # one entry per confirmed class

headers = {"Authorization": f"Bearer {API_USER_TOKEN}",
           "Content-Type": "application/json"}

# === Step 2: Confirm/correct dishes (per segment or extra) ===
dish_url = f"{BASE_URL}/image/confirm/dish"
dish_payload = {
    "imageId": IMAGE_ID,
    "confirmedClass": confirmed_classes,
    "food_item_position": food_item_positions,
    "source": sources
}
dish_resp = requests.post(dish_url, headers=headers, json=dish_payload, timeout=30)
dish_resp.raise_for_status()
dish_data = dish_resp.json()
print("Dish confirmation -> OK")
print(json.dumps(dish_data, indent=2))
