> Source: https://docs.logmeal.com/docs/guides-features-multi-language-support

# Multi-language Responses & Localized Food Names

## Plan & Token Requirements

This feature is available in the Analyse, Monitor, Recommend, and Custom plans.

It is accessible to APICompany, APIUser, and APIUserManager users.

- **APIUser:** Has a language attached to the user profile; most endpoints use this language by default for user-facing information.
- **APICompany and APIUserManager:** Can set or override language on behalf of assigned users.

## What It Does

LogMeal’s multi-language support returns localized names for:

- Detected dishes and food items
- Ingredients, including reference ingredients such as salt, sugar, and oil
- Food groups and food types
- Occasions such as breakfast and lunch
- Nutritional indicators and custom nutritional indicators
- Measurement units at dish and ingredient level
- Custom occasions and custom recipes through translation endpoints

Choose a language once per user or per request and supported endpoints respond in that language.

Supported languages and ISO 639-2/T codes include:

| Code | Language |
|---|---|
| `spa` | Spanish |
| `eng` | English |
| `fre` | French |
| `ita` | Italian |
| `nld` | Dutch |
| `ger` | German |
| `gre` | Greek |
| `tur` | Turkish |
| `heb` | Hebrew |
| `cat` | Catalan |

Retrieve the current list programmatically with [GET /info/languages](https://docs.logmeal.com/reference/get_v2-info-languages#/).

## When to Use It / Outcomes

Use multi-language responses when:

- The app UI is not in English and needs localized dish names.
- The app operates in multiple countries and wants labels based on each user’s profile.
- The app should avoid maintaining a translation table for thousands of dishes, ingredients, and nutrients.

Expected outcomes include JSON responses where food names, dish names, ingredient names, food groups, occasions, nutritional indicators, and related values are returned in the requested language. Internal IDs remain stable and language-independent, so they can be safely stored and queried in the application database.

No additional monthly credits, daily request limits, or per-second request limits are consumed beyond the underlying endpoint calls.

## Feature-Specific Details

### User-level default language (recommended)

Store a default language and country in each APIUser profile.

- Set or update the language through [POST /profile/modifyUserProfileInfo](https://docs.logmeal.com/reference/post_v2-profile-modifyuserprofileinfo#/).
- Use a language such as `spa` and optionally a country such as `ES`.
- Once set, most endpoints use that language by default.

### Per-request language override

Where supported, override the language for one request with the `language` query parameter:

```http
POST /image/segmentation/complete?language=spa
```

The value must be one of the codes returned by `/info/languages`. If omitted, the endpoint uses the APIUser profile language or English.

Unsupported language codes may result in `400 Bad Request`; validate user input against the current language list.

## Related Endpoints

- [GET /info/languages](https://docs.logmeal.com/reference/get_v2-info-languages#/) returns available languages.
- [POST /profile/modifyUserProfileInfo](https://docs.logmeal.com/reference/post_v2-profile-modifyuserprofileinfo#/) sets APIUser language and country.
- [POST /image/segmentation/complete](https://docs.logmeal.com/reference/post_v2-image-segmentation-complete#/) supports localized recognition output.
- [POST /nutrition/recipe/ingredients](https://docs.logmeal.com/reference/post_v2-nutrition-recipe-ingredients#/) returns localized ingredients.
- [POST /nutrition/recipe/nutritionalInfo](https://docs.logmeal.com/reference/post_v2-nutrition-recipe-nutritionalinfo#/) returns localized nutritional indicators.
- [GET /dataset/dishes](https://docs.logmeal.com/reference/get_v2-dataset-dishes#/) returns localized dish names.
- [GET /dataset/ingredients](https://docs.logmeal.com/reference/get_v2-dataset-ingredients#/) returns localized ingredient names.
- [GET /dataset/referenceIngredients](https://docs.logmeal.com/reference/get_v2-dataset-referenceingredients#/) returns localized modulator ingredients.
- [GET /dataset/foodGroups](https://docs.logmeal.com/reference/get_v2-dataset-foodgroups#/) returns localized food groups.
- [GET /info/nutrients](https://docs.logmeal.com/reference/get_v2-info-nutrients#/) returns localized nutrient names.
- [GET /dataset/weightMeasures/dishes](https://docs.logmeal.com/reference/get_v2-dataset-weightmeasures-dishes#/) returns localized dish measures and conversion rates.
- [GET /dataset/weightMeasures/ingredients](https://docs.logmeal.com/reference/get_v2-dataset-weightmeasures-ingredients#/) returns localized ingredient measures and conversion rates.

## Typical Workflow

1. Create an APIUser and obtain its token.
2. Let the end user choose language and country in the app settings.
3. Update the APIUser profile with language and country, for example `language: "spa"` and `country: "ES"`.
4. Call recognition endpoints without specifying `language`; responses use the APIUser’s configured language.
5. Optionally override language per request with `?language=eng` or another valid code.
6. Store dish IDs, ingredient IDs, and indicator codes in the local database, displaying localized names in the UI.

## Related Use Cases

- Personalized food diaries where users see intakes in their own language.
- Multi-country apps with localized dish and ingredient names.
- Clinical platforms where APIUserManagers review users in different countries and languages.
- White-labeled apps that change branding and language without retraining models.
