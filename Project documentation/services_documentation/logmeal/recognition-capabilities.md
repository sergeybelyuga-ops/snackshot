> Source: https://docs.logmeal.com/docs/guides-essential-concepts-recognition-capabilities

# Food Recognition Capabilities

The LogMeal API provides AI-powered food recognition at multiple semantic levels. You can analyze a food image and extract information from food presence through specific dishes, ingredients, and nutrition.

## What We Recognize

1. **Food type detection**: Detects whether an image contains prepared food, drinks, fresh fruits and vegetables, sauces, or no food.
2. **Food group detection**: Identifies broad categories such as meat, fish, vegetables, eggs, bread, legumes, desserts, and dairy.
3. **Dish recognition**: Recognizes more than 1300 dishes from international cuisines. The supported dish list is available through [GET /dataset/dishes](https://docs.logmeal.com/reference/get_v2-dataset-dishes#/).
4. **Ingredients detection**: Extracts ingredients present in dishes, including estimated quantities.
5. **Nutritional information**: Provides calories, macros, and micronutrients based on detected dishes and ingredients.

## Advanced Capabilities

Higher-tier plans provide additional AI features:

- **Quantity estimation** (Recommend or higher): Estimates food volume from RGB or depth images and supports serving-size detection.
- **Food waste detection** (Recommend or higher): Compares pre-meal and post-meal photos to detect leftovers.
- **Variety Score and Nutri-Scores** (Recommend or higher): Computes diet diversity and standardized nutrition quality scores.
- **Recipe and dish recommendations** (Recommend or higher): Suggests alternatives based on user preferences, restrictions, and history.

## Plans & Access

Food recognition is available in all plans, but capability detail varies:

| Capability | Available plans |
|---|---|
| Food type detection | Analyse and above |
| Food group detection | Analyse and above |
| Dish recognition | Analyse and above |
| Ingredients information | Analyse and above |
| Nutritional information | Analyse and above |
| Quantity estimation | Recommend and above |
| Food waste detection | Recommend and above |
| Variety Score and Nutri-Scores | Recommend and above |
| Recommendations | Recommend and above |

## Datasets

- [GET /dataset/dishes](https://docs.logmeal.com/reference/get_v2-dataset-dishes#/) returns supported dishes.
- [GET /dataset/ingredients](https://docs.logmeal.com/reference/get_v2-dataset-ingredients#/) returns supported ingredients.
- [GET /info/nutrients](https://docs.logmeal.com/reference/get_v2-info-nutrients#/) returns supported nutritional indicators.

## Related Endpoints

- [POST /v2/image/segmentation/complete](https://docs.logmeal.com/reference/post_v2-image-segmentation-complete#/) detects multiple food items, recognizes them, and logs a user intake.
- [POST /v2/nutrition/recipe/ingredients](https://docs.logmeal.com/reference/post_v2-nutrition-recipe-ingredients#/) detects ingredients and their quantities.
- [POST /v2/nutrition/recipe/nutritionalInfo](https://docs.logmeal.com/reference/post_v2-nutrition-recipe-nutritionalinfo#/) detects energy, carbohydrates, protein, fats, and more than 35 nutritional indicators.
- [GET /v2/history/getIntakesList](https://docs.logmeal.com/reference/get_v2-history-getintakeslist#/) retrieves intakes for a period.

## Next Steps

- [Plans & Limits](../logmeal-plans-limits.md)
- [Image Pre-processing](./image-pre-processing.md)
