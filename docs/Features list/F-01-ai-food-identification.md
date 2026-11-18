# F-01 | AI Food Identification

## Overview
The core feature of the app: the user captures or selects a food photo, the AI identifies what it is, estimates how much was eaten, calculates calories and macros, and the user reviews and saves the result to the nutrition log.

## User story
As an application user, I want to start using the main food logging feature so I can quickly capture what I eat and save it to my nutrition log with minimal effort.

## User flow
The flow follows [10_architecture_ux_spec.md](../Architecture/10_architecture_ux_spec.md) §2.3 ("Camera to AI recognition and correction").

1. The user opens the app on S-02 Home and chooses **Take a photo** (or **Gallery**, F-09) in the meal-entry carousel.
2. Before the first AI action, the app silently obtains AI access from the LogMeal Access Broker, with no registration (F-30). If the free trial has ended, S-19 Paywall is shown; if the pilot is full, S-20 Waitlist is shown. In both cases **Quick add** (F-11) is offered instead.
3. The app opens the camera (S-03) and requests camera permission if it is the first time. On the camera screen the user can:
   - take a photo,
   - turn the flashlight on or off,
   - close the camera and return to Home,
   - open the phone gallery to use an existing image,
   - see how many free AI pictures are left (F-32).
4. After the photo is taken, the app preprocesses the image in the background and shows recognition progress (S-05).
5. The AI identifies the food. In the MVP, one photo is recognized as **one dish, drink, or raw fruit or vegetable**. Several items on one plate (F-04) and packaged products (F-08) are post-MVP.
6. The system estimates the weight (F-02) using:
   - the LogMeal quantity estimate,
   - user history to correct assumptions using previous dishes of the same type,
   - global reference data for typical average weights of foods such as eggs, apples, bananas, and similar items.
7. The app shows the result on S-15 Meal review: name, confidence, grams, calories, and macros (carbohydrates, protein, and fat).
8. The user can correct the name, weight, or values (F-03, S-07) and then taps **Save**. The meal is saved to the local log with its processed photo and timestamp.
9. If recognition fails, the app retries once and then offers **Quick add** with the draft (photo and time) kept.
10. The user can later edit or delete the logged meal from the history (F-23).

## Acceptance criteria
- Camera permission is requested only when needed and is explained clearly to the user.
- The user can reach the photo-taking flow from Home in one tap.
- AI recognition starts automatically after photo capture or gallery selection.
- The app recognizes one dish, drink, or raw fruit or vegetable per photo.
- Nutritional data is generated immediately after recognition and portion estimation.
- The user reviews the result and can correct it before saving, and can edit it after saving.
- Saved logs include a timestamp, processed image, recognized name, weight, and macro totals.
- User deletion of log entries is available and works reliably.
- AI capture works without any registration or sign-in; each successful recognition counts as one of the 90 free pictures.
- When AI is unavailable or recognition fails, Quick add is offered.

## MVP status
Necessary for MVP: Yes (one item per photo; multi-item and packaged-product recognition are post-MVP)

## Related feature IDs
- F-02 Volume / Portion Estimation
- F-03 Fast Fix of Recognized Values
- F-04 Mixed Plate Breakdown (post-MVP)
- F-08 AI Product Identification (post-MVP)
- F-09 Phone Gallery Usage
- F-11 Quick Add Calories
- F-19 Connect to Google Health Connect
- F-20 Google Health Connect Sync
- F-23 Calories & Macros Log Feed
- F-30 Account-free App Access
- F-32 Free AI Trial & Paywall
