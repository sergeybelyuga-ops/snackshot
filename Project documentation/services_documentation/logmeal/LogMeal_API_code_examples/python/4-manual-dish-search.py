
####################################################################################
# CODE FOR LOCAL TESTING, DO NOT COPY THIS INTO THE README.COM RECIPE

BASE_URL = "http://localhost:8080/v2"
API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>"  # APIUser token

####################################################################################

# CODE TO BE COPIED TO README.COM

import requests
import json

# === Step 1: Configure inputs ===
BASE_URL = "https://api.logmeal.com/v2"
API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>"   # APIUser token (Bearer)
IMAGE_ID = 1981521                        # obtained from a previous segmentation/complete
USER_QUERY = "miso soup"                  # what the user typed
TARGET_REGION = 2                         # region to correct (food_item_position)
LANGUAGE = "eng"                            # language of the dishes search

headers = {"Authorization": f"Bearer {API_USER_TOKEN}",
           "Content-Type": "application/json"}

# (Example) Current mapping you plan to CONFIRM for the WHOLE image.
# IMPORTANT: /image/confirm/dish OVERWRITES the image; any region NOT SENT here gets REMOVED.
# Fill this from your UI / prior step. We'll correct TARGET_REGION with the search result below.
confirmed_classes = [110, None]           # e.g., region #1 is donuts (id=110), region #2 to be filled
food_item_positions = [1, 2]              # parallel to confirmed_classes
sources = ["logmeal", "logmeal"]          # 'logmeal' for catalog ids, 'other' for free-text

# === Step 2: Search the dish catalog (GET /dataset/dishes) ===
# The endpoint returns several top-level groups (e.g., "food", "drinks", "combo", "ingredients", ...).
# We'll flatten them and do a simple case-insensitive substring match on "name".
ds_url = f"{BASE_URL}/dataset/dishes"
resp_ds = requests.get(ds_url, headers={"Authorization": f"Bearer {API_USER_TOKEN}"}, params={'language': LANGUAGE})
resp_ds.raise_for_status()
catalog = resp_ds.json()

def flatten_dishes(payload):
    flat = []
    if isinstance(payload, dict):
        for _, items in payload.items():
            if isinstance(items, list):
                for it in items:
                    name = (it or {}).get("name")
                    did = (it or {}).get("id")
                    if name and did is not None:
                        flat.append({"id": did, "name": name})
    elif isinstance(payload, list):
        for it in payload:
            name = (it or {}).get("name")
            did = (it or {}).get("id")
            if name and did is not None:
                flat.append({"id": did, "name": name})
    return flat

all_dishes = flatten_dishes(catalog)
q = USER_QUERY.lower().strip()
hits = [d for d in all_dishes if q in d["name"].lower()]

# Map the user query to a dish id if found; otherwise fall back to free-text
if hits:
    picked = hits[0]
    dish_id = picked.get("id") or picked.get("dishId") or picked.get("class_id")
    # Put the found id into the arrays at TARGET_REGION’s position
    idx = food_item_positions.index(TARGET_REGION)
    confirmed_classes[idx] = dish_id

# === Step 3: Confirm/correct dishes ===
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