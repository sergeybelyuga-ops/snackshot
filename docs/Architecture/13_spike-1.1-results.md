# WBS 1.1 Spike Results: LogMeal + Health Connect

**Snackshot: AI-Powered Nutrition & Calorie Tracker**

**Purpose:** Evidence for the decision gates in [04 Stack comparative analysis](04_stack-comparative-analysis.md) §10 and the I-0 Definition of Done in the [WBS](../04_wbs_and_mvp_implementation_plan.md) §7.5.
**Date:** 25 November 2026
**Status:** Measured. Decisions on the open findings (§6) are pending.

Spike code: branch `spike/1.1`, folder `spike/logmeal-hc/` (throwaway, never merged). Fixtures: [test-fixtures/logmeal](../../test-fixtures/logmeal/README.md).

---

## 1. Setup

| | |
| --- | --- |
| Device | Pixel 9 Pro, Android 17 (API 37), home network |
| App | Views, OkHttp 5.5.1, Health Connect client 1.1.0, ARCore 1.57.0 |
| LogMeal | Test APIUser `75682`, language `eng`, country `RS`; default model version |
| Flow | Photo Picker image → resize to ≤ 1000 px, JPEG < 1 MB → `POST /v2/image/segmentation/complete` → `POST /v2/nutrition/recipe/nutritionalInfo` → `POST /v2/nutrition/confirm/quantity` → one `NutritionRecord` |
| Sample | 5 different foods weighed on a kitchen scale (6 runs; the apple was sent twice) |

All five photos came from the gallery. The system camera path in the spike was not used, so CameraX capture is still to be measured in I-4.

## 2. Latency

Measured in the app, from "photo chosen" to "nutrition shown". Upload size was 113–161 KB.

| Step | Min | Median | Max |
| --- | --- | --- | --- |
| Resize + JPEG encode | 77 ms | 102 ms | 112 ms |
| `segmentation/complete` | 2 922 ms | 3 593 ms | 6 398 ms |
| `nutritionalInfo` | 233 ms | 246 ms | 2 987 ms |
| **Total photo → nutrition** | **3 668 ms** | **4 313 ms** | **6 766 ms** |

**Result: the p98 < 3 s target ([02](02_answers-architecture-checklist.md) 9.1) is not met.** None of the six runs is under 3 s, and `segmentation/complete` alone takes about 3.6 s. This is LogMeal server time, because the app's own work is about 0.1 s. Six runs are too few for a real p98, but the gap is too large to close by tuning the client.

## 3. Grams and recognition quality

| Food | Real | LogMeal top result | LogMeal grams | Error |
| --- | --- | --- | --- | --- |
| Tomato | 140 g | tomato 70 % **+ 2 extra items** (tangerine 52 %, raf tomato 49 %) | 346 g (3 items) | +147 % |
| Tangerine | 104 g | tangerine 64 % | 100 g | −4 % |
| Apple | 116 g | apple 73 % | 182 g | +57 % |
| Banana | 196 g | banana 74 % | 118 g | −40 % |
| Bread slice | 24 g | whole grain bread 60 % | 60 g | +150 % |

- **Recognition:** the right food was the top result for 5 of 5. Confidence was only 60–74 %. One single tomato was split into three items.
- **Grams:** 1 of 5 within ±20 %, mean absolute error about 80 %. Without depth, `serving_size` behaves like a **standard portion** for the food, not a measurement of this portion. This matches LogMeal's own field description ("can also correspond to the standard quantity of food corresponding to one portion").
- **Correction works:** `confirm/quantity` with the **per-item object** `{"imageId": …, "quantity": {"1": 116}}` was accepted on the current plan, and nutrition was recalculated (apple 182 g / 95 kcal → 116 g / 60 kcal). The **integer** form is rejected for segmented images (HTTP 400, code 1614).

## 4. Credits and plan limits

| Call | Credits | Rate-limit headers |
| --- | --- | --- |
| `segmentation/complete` | **1** | yes |
| `nutritionalInfo` | 0 | no |
| `confirm/quantity` | 0 | no |
| `info/limitations` | 0 | yes |

One picture with any number of corrections costs **1 credit**. Total spike cost: 6 credits.

`GET /v2/info/limitations` for the test APIUser reports: **"200 queries per month among all your users"**, **20 calls per day per user**, 1 call per second. `x-account-credits-remaining` was 194 after the spike. **This is the standard trial quota**, not the special conditions assumed in [11](11_logmeal-access-token-management.md) §0 decision 15 and §6 ("5 clients × 90 pictures = 450 recognitions … covered by the LogMeal special conditions").

## 5. Health Connect and ARCore

| Check | Result |
| --- | --- |
| Health Connect on Android 17 | Built in, `SDK_AVAILABLE` |
| Permissions | `WRITE_NUTRITION` + `READ_NUTRITION` granted through the standard dialog |
| `NutritionRecord` insert | **OK in 25 ms**, read back with the same name and kcal |
| ARCore | `SUPPORTED_INSTALLED`; Depth API `AUTOMATIC` and `RAW_DEPTH_ONLY` both **supported** |

Creating an ARCore `Session` needs the `CAMERA` runtime permission. When the app declares `CAMERA`, `ACTION_IMAGE_CAPTURE` also needs it granted.

## 6. Findings that need a decision

| # | Finding | Proposal |
| --- | --- | --- |
| F1 | Latency is about 4.3 s median and 6.8 s max, against a target of p98 < 3 s. | Change the target to what the app controls: **app overhead < 0.5 s and photo → result p90 < 6 s on Wi-Fi**, with the recoverable pending state from [10](10_architecture_ux_spec.md) ("show a recoverable pending state beyond that threshold") shown from the first second. Re-measure with CameraX and mobile data in I-4, and ask LogMeal whether a faster model or region is available. |
| F2 | Grams without depth are standard portions (mean error about 80 %). | Make the quantity step on S-15 part of **every** recognition: show the grams pre-selected and editable, with SMALL / MEDIUM / BIG presets, and send the per-item object form. Keep depth quantity (ARCore is supported on the pilot phone) as a post-MVP experiment. |
| F3 | The account reports the **trial quota** (200 queries/month across all users) while the pilot needs about 450. | **Ask LogMeal to apply the special conditions** to the account, including the monthly query quota, before I-4. Until then, the credit alarm ([11](11_logmeal-access-token-management.md) §6.3) must be set against 200. |

## 7. Notes for production code

- Send a plain baseline JPEG. On Android 17, `Bitmap.compress` keeps the Ultra HDR gain map, EXIF, XMP and ICC from the source (about 2 KB extra, with no location data seen). Drop the gain map before encoding (`bitmap.gainmap = null`).
- LogMeal's 401 has an **empty body**. Error handling must not assume a JSON body.
- `nutritionalInfo` returns `foodName` as a list and `nutritional_info_per_item` keyed by `food_item_position`. Use the position as the item key for `confirm/quantity`.
- The rate-limit and credit headers come only with `segmentation/complete`. Read the remaining daily quota from that response.

## 8. Decision gates ([04](04_stack-comparative-analysis.md) §10)

| Gate | Status |
| --- | --- |
| Camera → LogMeal → correction on a physical device | ✅ Passed with gallery photos. CameraX capture is still to be measured in I-4. |
| Health Connect permissions and `NutritionRecord` writes work | ✅ Passed |
| LogMeal tokens handled without an unacceptable long-lived secret | ✅ Resolved by [11](11_logmeal-access-token-management.md); test APIUser created through Secret Manager |
| Regular-recognition fallback without depth | ✅ Passed: regular recognition + per-item `confirm/quantity` |
| Stack compatible with the MVP budget | ✅ No change to the native Android stack |

The stack decision stands. The open findings F1–F3 change targets, UX and the LogMeal account, not the stack.
