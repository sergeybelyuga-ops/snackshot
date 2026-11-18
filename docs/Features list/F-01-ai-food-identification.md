# F-01 | AI Food Identification

## Overview
The core feature of the app: the user captures or selects a food photo, the AI identifies what it is, estimates how much was eaten, calculates calories and macros, and saves the result to the nutrition log.

## User story
As an application user, I want to start using the main food logging feature so I can quickly capture what I eat and save it to my nutrition log with minimal effort.

## User flow
1. The user opens the app and starts the main food logging flow.
2. The app opens the camera and requests camera permission if it is the first time the feature is used.
3. On the camera screen, the user can:
   - take a photo,
   - turn the flashlight on or off,
   - close the camera and return to the previous screen,
   - open the phone gallery to use an existing image.
   - see how many free AI pictures are left (F-32).
   Before the first AI action the app silently obtains AI access from the LogMeal Access Broker, with no registration (F-30). If the free trial has ended, the paywall is shown; if the pilot is full, the waitlist is shown. In both cases manual logging is offered instead.
4. After the user takes a photo, the system sends the image to the AI to identify the object type:
   - meal,
   - packed product,
   - raw vegetable or fruit.
5. The AI then performs the next classification step:
   - If it is a meal, the system identifies which products are in the meal and what the meal is called.
   - If it is a packed product, the system identifies the brand, product name, and size.
   - If it is a raw fruit or vegetable, the system identifies the product name.
6. The system estimates the weight using a combination of:
   - visual estimate based on the size of the plate, glass, or mug,
   - fullness of the container,
   - user history to correct assumptions using previous dishes of the same type,
   - global reference data for typical average weights of foods such as eggs, apples, bananas, and similar items.
7. The app calculates calories and macros (carbohydrates, protein, and fat) and presents the results to the user.
8. The user can quickly correct the estimated weight or calories if needed.
9. Every meal photo, recognized item name, timestamp, weight, and calculated calories/macros are saved to the user's log without requiring extra confirmation.
10. The user can delete a logged value from the history if needed.

## Acceptance criteria
- Camera permission is requested only when needed and is explained clearly to the user.
- The user can access the photo-taking flow without friction.
- AI classification starts automatically after photo capture.
- The app supports meal, packaged product, and raw produce recognition.
- Nutritional data is generated immediately after recognition and portion estimation.
- Users can correct recognized values before or after saving.
- Saved logs include a timestamp, image, recognized name, weight, and macro totals.
- User deletion of log entries is available and works reliably.
- AI capture works without any registration or sign-in; each successful recognition counts as one of the 90 free pictures.

## MVP status
Necessary for MVP: Yes

## Related feature IDs
- F-02 Volume / Portion Estimation
- F-03 Fast Fix of Recognized Values
- F-19 Connect to Health Connect (Mandatory)
- F-20 Google Health Connect Sync
- F-23 Calories & Macros Log Feed
- F-30 Account-free App Access
- F-32 Free AI Trial & Paywall
