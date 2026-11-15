####################################################################################
# CODE FOR LOCAL TESTING, DO NOT COPY THIS INTO THE README.COM RECIPE

BASE_URL = "http://localhost:8080/v2"
API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>"  # APIUser token
IMAGE_ID = "<imageId-from-previous-step>"  # Obtained from previous /image/segmentation/complete

####################################################################################

# CODE TO BE COPIED TO README.COM

import requests
import json

# === Step 1: Configure inputs & endpoint ===
BASE_URL = "https://api.logmeal.com/v2"
API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>"                  # APIUser token
IMAGE_ID = "<imageId-from-previous-step>"                # From segmentation
url = f"{BASE_URL}/nutrition/recipe/nutritionalInfo"   # default latest model
headers = {"Authorization": f"Bearer {API_USER_TOKEN}"}  # Bearer auth

# === Step 2: Request nutritional info ===
payload = {"imageId": IMAGE_ID}
resp = requests.post(url, headers=headers, json=payload)

# Raise for HTTP errors (e.g., 401 bad token, 429 rate limit)
resp.raise_for_status()
data = resp.json()

# === Step 3: Parse & print summary (matches provided structure) ===
print(f"imageId: {data.get('imageId') or IMAGE_ID}")
if 'serving_size' in data:
    print(f"meal_serving_size: {data.get('serving_size')} g")

# Optional: overall Nutri-Score (if present)
if isinstance(data.get('image_nutri_score'), dict):
    ns = data['image_nutri_score']
    print(f"nutri_score: {ns.get('nutri_score_category')} ({ns.get('nutri_score_standardized')})")

# Meal-level totals
total = data.get('nutritional_info') or {}
if total:
    cal = total.get('calories')
    if cal is not None:
        print(f"calories_total: {round(cal, 2)} kcal")

    # Daily Intake Reference (DIR) summary
    dirf = total.get('dailyIntakeReference') or {}
    if dirf:
        print("Daily Intake Reference (meal):")
        for k in ['ENERC_KCAL','PROCNT','FAT','CHOCDF','FIBTG','SUGAR','FASAT','NA']:
            v = dirf.get(k)
            if isinstance(v, dict):
                label = v.get('label') or k
                level = v.get('level')
                pct = v.get('percent')
                if pct is not None:
                    pct = round(float(pct), 1)
                print(f"  {label}: {pct}% ({level})")

    # Key nutrients (totals)
    tn = total.get('totalNutrients') or {}
    if tn:
        def _fmt(n):
            if not isinstance(n, dict): return None
            q = n.get('quantity'); u = n.get('unit'); lab = n.get('label')
            if q is None or u is None: return None
            return f"{lab}: {round(float(q), 2)} {u}"
        key_codes = ['PROCNT','FAT','CHOCDF','FIBTG','SUGAR']
        line = "Nutrients (meal): " + "; ".join([_fmt(tn.get(c)) for c in key_codes if _fmt(tn.get(c))])
        print(line)

# Per-item nutritional info
name_map = {}
if isinstance(data.get('ids'), list) and isinstance(data.get('foodName'), list):
    name_map = dict(zip(data.get('ids'), data.get('foodName')))

items = data.get('nutritional_info_per_item') or []
if items:
    for it in items:
        pos = it.get('food_item_position')
        iid = it.get('id')
        dname = name_map.get(iid)
        ss = it.get('serving_size')
        print(f"[#{pos}] dish={dname} serving_size={ss} g")

        # Item Nutri-Score (if present)
        if isinstance(it.get('nutri_score'), dict):
            ns = it['nutri_score']
            print(f"  nutri_score: {ns.get('nutri_score_category')} ({ns.get('nutri_score_standardized')})")

        ni = it.get('nutritional_info') or {}
        cal_i = ni.get('calories')
        if cal_i is not None:
            print(f"  calories: {round(cal_i, 2)} kcal")

        # Item nutrients and DIR
        tn_i = ni.get('totalNutrients') or {}
        dir_i = ni.get('dailyIntakeReference') or {}
        if tn_i:
            def _fmt_i(n):
                if not isinstance(n, dict): return None
                q = n.get('quantity'); u = n.get('unit'); lab = n.get('label')
                if q is None or u is None: return None
                return f"{lab}: {round(float(q), 2)} {u}"
            key_codes = ['PROCNT','FAT','CHOCDF','FIBTG','SUGAR']
            print("  nutrients: " + "; ".join([_fmt_i(tn_i.get(c)) for c in key_codes if _fmt_i(tn_i.get(c))]))
        if dir_i:
            out = []
            for k in ['ENERC_KCAL','PROCNT','FAT','CHOCDF','FIBTG','SUGAR','FASAT','NA']:
                v = dir_i.get(k)
                if isinstance(v, dict) and v.get('percent') is not None:
                    pct = round(float(v['percent']), 1)
                    out.append(f"{v.get('label')}: {pct}%")
            if out:
                print("  daily intake: " + "; ".join(out))