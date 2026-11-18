####################################################################################
# CODE FOR LOCAL TESTING, DO NOT COPY THIS INTO THE README.COM RECIPE

BASE_URL = "http://localhost:8080/v2"
API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>"  # APIUser token
IMAGE_ID = "1981523"  # Obtained from previous /image/segmentation/complete

####################################################################################

# CODE TO BE COPIED TO README.COM

import requests
import json

# === Step 1: Configure inputs & endpoint ===
BASE_URL = "https://api.logmeal.com/v2"
API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>"                  # APIUser token
IMAGE_ID = "<imageId-from-previous-step>"                # From segmentation
url = f"{BASE_URL}/nutrition/recipe/ingredients"        # default latest model
headers = {"Authorization": f"Bearer {API_USER_TOKEN}"}  # Bearer auth

# === Step 2: Request ingredients & quantities ===
payload = {"imageId": IMAGE_ID}
resp = requests.post(url, headers=headers, json=payload)

# Raise for HTTP errors (e.g., 401 bad token, 429 rate limit)
resp.raise_for_status()
data = resp.json()

# === Step 3: Parse & print summary (matches provided structure) ===
print(f"imageId: {data.get('imageId') or IMAGE_ID}")
if 'serving_size' in data:
    print(f"meal_serving_size: {data.get('serving_size')} g")

# Prefer per-item recipes if present
items = data.get('recipe_per_item') or []

if items:
    for it in items:
        pos = it.get('food_item_position')
        dname = it.get('name')
        ss = it.get('serving_size')
        unit = it.get('unit')
        print(f"[#{pos}] dish={dname} serving_size={ss} {unit}")

        # Ingredients for this dish
        for ing in it.get('recipe', []):
            iid = ing.get('id')
            name = ing.get('name')
            weight = ing.get('weight')
            unit_i = ing.get('unit') or 'g'
            meas = ing.get('measure') or {}
            metric = meas.get('metric') or {}
            preferred = meas.get('preferred') or {}

            # Metric (canonical) amount
            m_qty = metric.get('quantity') if metric else weight
            m_short = metric.get('short') or unit_i

            # Preferred (human-friendly) measure if available
            p_qty = preferred.get('quantity')
            p_name = preferred.get('name')

            line = f"  - {name} (id {iid}) -> {m_qty} {m_short}"
            if p_qty is not None and p_name:
                line += f" (~{p_qty} {p_name})"
            print(line)
else:
    # Fallback 1: show flat global recipe
    ingr_list = data.get('recipe') or []
    if ingr_list:
        print("Global recipe:")
        for ing in ingr_list:
            iid = ing.get('id')
            name = ing.get('name')
            weight = ing.get('weight')
            unit_i = ing.get('unit') or 'g'
            meas = ing.get('measure') or {}
            metric = meas.get('metric') or {}
            preferred = meas.get('preferred') or {}

            m_qty = metric.get('quantity') if metric else weight
            m_short = metric.get('short') or unit_i

            p_qty = preferred.get('quantity')
            p_name = preferred.get('name')

            line = f"  - {name} (id {iid}) -> {m_qty} {m_short}"
            if p_qty is not None and p_name:
                line += f" (~{p_qty} {p_name})"
            print(line)
    else:
        # Fallback 2: generic fields (unlikely with this endpoint)
        for name, value in data.items():
            if name not in ('imageId', 'recipe_per_item', 'recipe', 'dish_id', 'foodName', 'is_combo', 'serving_size', 'source'):
                print(f"{name}: {json.dumps(value, ensure_ascii=False)}")
