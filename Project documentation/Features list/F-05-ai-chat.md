# F-05 | AI Chat

## Overview
The initial screen of the application is an AI chat interface. The user sees the previous communication history, including taken pictures, sent text, and sent audio messages (transcribed into text), as well as the calculated calories and macros for saved entries.

## User story
As an application user, I want to start with the AI chat screen so I can log meals using text, voice, or a photo in one natural flow, while keeping my history visible and easy to review.

## Main chat capabilities
The chat screen offers the following actions:
- send an audio message,
- send a text message,
- take a picture with the camera,
- choose an image from the gallery,
- navigate to other parts of the app.

In the MVP, chat supports only food logging and cannot answer general questions or perform other actions.

## Information parsing and processing flow
Every user input is parsed and analyzed in the following order:
1. Define the type of sent information:
   - meal,
   - product,
   - raw vegetable or fruit,
   - drink,
   - list of meals,
   - list of products,
   - list of drinks.
2. Estimate the size or weight of the dish using:
   - user logging history,
   - app logic for portion estimation,
   - historical patterns for similar meals or products.
3. Calculate and display the result to the user:
   - item name,
   - weight,
   - calories,
   - macros.
4. If the user sends a list, show several entries and allow the user to review each one individually.

## User interaction and editing
- The user can easily fix the weight and macros of the dish.
- The user can delete a logged item.
- All accepted changes are automatically saved to the user log.

## Acceptance criteria
- The AI chat is the first screen seen by the user after app launch.
- The chat history shows prior text, audio, and image interactions.
- Audio messages are transcribed into text before processing.
- The user can send text, audio, or images as meal input.
- The app identifies the input type before calculating nutrition.
- The app estimates portion size using previous user patterns and internal logic.
- The app can show multiple items when a list of meals or products is entered.
- The user can edit or delete the generated log entries.
- All modifications are saved automatically to the user's nutrition history.

## MVP status
Necessary for MVP: Yes
