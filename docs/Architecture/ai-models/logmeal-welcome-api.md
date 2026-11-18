> Source: https://docs.logmeal.com/docs/guides-getting-started-welcome-logmeal-api

# Welcome to LogMeal API

The LogMeal API is a RESTful service that recognizes foods from images, returns ingredients and nutrition, and lets your app log intakes, track goals, and power personalized recommendations.

Don't have an account yet? [Sign up for free](https://logmeal.com/signup/form?next=/signup/plan).

## Main Shortcuts

- **Quick Start (first 10 min)** — Create an API key, make your first request (cURL + one language), and understand the response.
- **API Documentation** — Browse endpoints, schemas, and request/response samples in the [API Explorer](https://docs.logmeal.com/reference/getting-started#/).
- **Features Overview** — Recognition, ingredients, nutrition, intakes and history, goals, quantity, barcode, favorites, and occasions.
- **Ready-to-use Code in +35 languages** — Copy-paste code snippets and SDK helpers to accelerate integration.

## Essential Concepts

- **Plans & Limits** — Explore subscription plans and understand monthly credits, daily request limits, and per-second request limits.
- **User Types & Access Tokens** — Roles and tokens for account admins, end-users, and nutrition-related managers.
- **Images Pre-processing** — Image guidelines that improve recognition quality, including size, format, and lighting.

## Implement a Use Case

- **Image-based Food Recognition with User Confirmation** — Capture a meal photo, get top dish predictions, and let the user confirm or refine results before saving. Confirmations feed food-recognition personalization capabilities.
- **Create a Custom Dish from Scratch with a Manual Intake** — Create a one-off intake from ingredient IDs and quantities without selecting an existing LogMeal dish.
- **Ingredients List Retrieval** — Retrieve standardized recipe ingredients and quantities for confirmed dishes from a food intake.
- **Nutritional Information Retrieval** — Extract per-dish macro and micronutrients from a confirmed intake using standardized ingredients.
- **Food Quantity Detection** — Capture a depth-based image sequence around food items to estimate quantities using the Quantity Detection API.
- **Food Quantity Confirmation** — Confirm how much was eaten in grams or as a Small/Medium/Big portion and automatically recalculate ingredients and nutrition.
- **Units of Measurement & User Preferred Measurements** — Adapt measures to user preferences (metric/imperial, cooking measures, portions) while keeping grams as the source of truth.

## Popular API Endpoints

- `POST /v2/image/segmentation/complete` — detect multiple food items on an image, recognize them, and log a user intake.
- `POST /v2/nutrition/recipe/ingredients` — detect the list of ingredients and their quantities for an intake.
- `POST /v2/nutrition/recipe/nutritionalInfo` — detect nutritional information for an intake, including energy, carbohydrates, protein, fats, and 35+ nutritional indicators.
- `GET /v2/history/getIntakesList` — display a food diary and retrieve the list of intakes performed by a user during a period.

More information is available in the [API Documentation](https://docs.logmeal.com/reference/getting-started#/).
