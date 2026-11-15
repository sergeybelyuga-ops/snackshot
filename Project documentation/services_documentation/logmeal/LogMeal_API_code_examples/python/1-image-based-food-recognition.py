
####################################################################################
# CODE FOR LOCAL TESTING, DO NOT COPY THIS INTO THE README.COM RECIPE

BASE_URL = "http://localhost:8080/v2"
API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>"  # APIUser token
IMAGE_PATH = "path/to/multiple_items_meal.jpg>"

####################################################################################

# CODE TO BE COPIED TO README.COM

import requests
import json

# === Step 1: Configure inputs & endpoint ===
BASE_URL = "https://api.logmeal.com/v2"
API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>"                     # APIUser token
IMAGE_PATH = "<path/to/multiple_items_meal.jpg>"          # Local JPEG path
url = f"{BASE_URL}/image/segmentation/complete"
headers = {"Authorization": f"Bearer {API_USER_TOKEN}"}  # Bearer auth

# === Step 2: POST image & get JSON ===
with open(IMAGE_PATH, "rb") as f:
    files = {"image": f}  # multipart field name must be 'image'
    resp = requests.post(url, headers=headers, files=files, timeout=30)

# Raise for HTTP errors (e.g., 401 bad token, 429 rate limit)
resp.raise_for_status()
data = resp.json()

# === Step 3: Parse & print summary ===
print(f"imageId: {data.get('imageId')}")
if 'occasion' in data:
    print(f"occasion: {data.get('occasion')}")
ft = data.get('foodType')
if ft:
    print(f"foodType: {ft.get('name')} (id {ft.get('id')})")

# Iterate each segmented food item / region
for item in data.get('segmentation_results', []):
    pos = item.get('food_item_position')
    bb = item.get('contained_bbox', {})
    center = item.get('center', {})
    grams = item.get('serving_size')  # may be absent depending on plan
    recs = item.get('recognition_results') or []
    top = recs[0] if recs else None

    line = f"[#{pos}] bbox={{'x': {bb.get('x')}, 'y': {bb.get('y')}, 'w': {bb.get('w')}, 'h': {bb.get('h')}}} center={{'x': {center.get('x')}, 'y': {center.get('y')}}}"
    if grams is not None:
        line += f" grams={grams}"
    print(line)

    if top:
        prob = top.get('prob')
        if isinstance(prob, (int, float)):
            prob = round(prob, 3)
        print(f"  top dish: {top.get('name')} (id {top.get('id')}, prob {prob})")

        # extra: print top-5 candidates for this region
        cands = []
        for r in recs[:5]:
            p = r.get('prob')
            if isinstance(p, (int, float)): p = round(p, 3)
            cands.append(f"{r.get('name')} (id {r.get('id')}, prob {p})")
        print("  top-5 candidates: " + "; ".join(cands))

