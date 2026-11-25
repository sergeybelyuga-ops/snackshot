# LogMeal fixtures

Recorded LogMeal responses shared by every fake: Android `MockWebServer`, broker MSW, and `tools/fake-logmeal` ([12](../../docs/Architecture/12_dev-and-test-environment.md) §3).

| | |
| --- | --- |
| Recorded | 25 November 2026, WBS 1.1 spike ([13](../../docs/Architecture/13_spike-1.1-results.md)) |
| API | `https://api.logmeal.com/v2`, default model version |
| APIUser | Test APIUser `75682` (`snackshot-test-2026-11-25-b44777`), language `eng`, country `RS` |
| Device | Pixel 9 Pro, Android 17 (API 37), gallery photos resized to ≤ 1000 px, JPEG < 1 MB |

## Files

| File | Photo (`../images/`) | Real weight | LogMeal says | Notes |
| --- | --- | --- | --- | --- |
| `segmentation-complete/ok-single-dish-apple.json` | `apple.jpg` | 116 g | apple 73 % | |
| `segmentation-complete/ok-single-dish-tangerine.json` | `tangerine.jpg` | 104 g | tangerine 64 % | |
| `segmentation-complete/ok-single-dish-banana.json` | `banana.jpg` | 196 g | banana 74 % | |
| `segmentation-complete/ok-single-dish-bread.json` | `bread.jpg` | 24 g | whole grain bread 60 % | lowest confidence so far |
| `segmentation-complete/one-tomato-three-items.json` | `tomato.jpg` | 140 g | 3 items: tomato 70 %, tangerine 52 %, raf tomato 49 % | **one** tomato over-segmented into three items, use for multi-item UI and for "remove item" |
| `nutritional-info/ok-{apple,tangerine,banana,bread}.json` | same | | `serving_size` 182 / 100 / 118 / 60 g | |
| `nutritional-info/one-tomato-three-items.json` | `tomato.jpg` | | `serving_size` 346 g, 3 × `nutritional_info_per_item` | |
| `nutritional-info/apple-after-confirm-116g.json` | `apple.jpg` | | 116 g, 60 kcal | after `quantity/confirm-ok.json` |
| `quantity/confirm-ok.json` | | | `{"result":"OK"}` | request: `{"imageId":…,"quantity":{"1":116}}` |
| `errors/400-1614-quantity-not-object.json` | | | HTTP 400, code 1614 | request with integer `quantity` after segmentation |
| `errors/401-empty-body.txt` | | | HTTP 401, **empty body** | invalid token; error parsing must not assume JSON |
| `info-limitations.json` | | | plan limits | free endpoint |

The files are kept as LogMeal returns them, apart from scrubbing. Matching `imageId`s: apple 1000001, tomato 1000002, tangerine 1000003, banana 1000004, bread 1000005.

### Response headers (not in the JSON files)

`segmentation/complete` and `info/limitations` return `ratelimit-limit: 20`, `ratelimit-remaining`, `ratelimit-reset` (seconds, rolling 24 h), `x-account-credits-used`, `x-account-credits-remaining`. `nutritionalInfo` and `confirm/quantity` return none of them. Fakes should send these headers on segmentation.

### Still to record

`segmentation-complete/ok-mixed-plate.json` (a real plate with several foods), `errors/403-116.json` (disabled APIUser), `errors/429.json`, `errors/500.json`. Record them with the test APIUser when needed (they cost at most one credit each), scrub, and add rows above.

## Scrubbing rules

Run `node tools/scrub-logmeal-fixture.ts <in.json> <out.json> --image-id <fake>`:

- `imageId` → a fixed fake value from 1000001 upwards.
- Keys `token`, `api_user_id`, `user_id`, `userId`, `username` → removed (none appear in these endpoints today).
- Dish, food-family and food-type ids are LogMeal **catalogue** ids, the same for every account, and are kept.

Images: re-encoded by the app, then all `APP1`–`APP15` segments (EXIF, XMP, ICC) and the trailing Ultra HDR gain-map JPEG are removed, so the files hold no device or location metadata.

## Refreshing

Manual only, with the test APIUser token, never in CI. Re-record when LogMeal changes its API or a new case is needed, and update the "Recorded" row.
