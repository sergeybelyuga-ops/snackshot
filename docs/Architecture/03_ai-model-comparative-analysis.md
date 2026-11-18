# AI Model Comparative Analysis for the MVP

**Decision:** Select the most suitable AI provider for Snackshot's MVP food-logging flow.  
**Analysis date:** 10 September 2026  
**Recommendation:** Use LogMeal as the primary MVP provider, subject to a short technical spike and representative-meal validation. Keep Passio as the fallback provider candidate. Do not make Gemini or Bite the primary nutrition engine without additional validation and architecture work.

## 1. Decision question

Which available AI provider best supports Snackshot's MVP within the one-month, approximately 28-hour delivery window while meeting the core requirements for photo-based food identification, portion estimation, calorie and macro output, quick correction, and local-first Health Connect logging?

## 2. Frame of reference

Snackshot's differentiator is private, low-friction nutrition logging. The project charter defines a backend-less, local-first client: nutrition history remains on the device, Health Connect is an optional synchronization target, and an external AI service receives image payloads and returns data for local storage. The MVP target is friends and family, with free use until external API limits are reached and a one-month delivery window with approximately 28 hours available. Sources: [project charter](../01_project_charter.md), [architecture checklist](01_must-answer-architecture-checklist.md), [architecture answers](02_answers-architecture-checklist.md).

The core AI flow must identify a meal, packaged product, or raw fruit/vegetable; estimate weight or portion; calculate calories and macros; present the result for fast correction; and save a timestamped log. The MVP also needs a manual text-search fallback, a macro dashboard, local data storage, and optional Health Connect synchronization. Sources: [F-01 AI food identification](../Features%20list/F-01-ai-food-identification.md), [F-02 portion estimation](../Features%20list/F-02-volume-portion-estimation.md), [F-03 fast correction](../Features%20list/F-03-fast-fix-of-recognized-values.md), [F-10 manual text search](../Features%20list/F-10-manual-text-search.md), [F-16 macro dashboard](../Features%20list/F-16-macronutrient-dashboard.md), [F-19 Health Connect](../Features%20list/F-19-connect-health-connect.md), [F-20 Health Connect sync](../Features%20list/F-20-google-health-connect-sync.md), and [F-23 log feed](../Features%20list/F-23-calories-macros-log-feed.md).

## 3. Grounds for comparison

The providers are compared against the same dimensions:

- **MVP functional fit:** food recognition, ingredients or meal parsing, nutrition values, and portion support.
- **Integration effort:** API/SDK availability, mobile-platform support, response structure, and suitability for a stateless client.
- **Nutrition-data completeness:** whether the provider supplies a food database and macro data rather than only visual labels.
- **MVP cost and limits:** trial/free access, recurring price, quotas, rate limits, and cost predictability.
- **Privacy and architecture fit:** compatibility with local-first storage and the ability to avoid making the provider the system of record.
- **Operational risk:** evidence gaps, vendor dependencies, unclear limits, and unresolved implementation questions.

### Thesis

> If a provider covers recognition, nutrition, and portion estimation with a usable API within the MVP trial budget, and can return data without becoming the system of record, choose it over a general-purpose vision model that would require the project to build or validate the nutrition layer itself.

## 4. Evidence base and limitations

The comparison uses all provider-specific research currently stored in `Architecture/ai-models/`, plus the project charter, feature specifications, architecture checklist, Health Connect decisions, and food-energy rules.

The repository does not yet contain provider latency benchmarks, verified mapping from the provider response to the application contract, image-retention policies, confidence-score behavior, exact nutrition-database provenance, or a representative test set. Scores below are therefore decision judgments based on documented capabilities, not measured model accuracy. The first implementation task must validate these unknowns.

There is also a product-scope conflict: the project charter lists AI chat as out of scope for the MVP, while [F-05 AI Chat](../Features%20list/F-05-ai-chat.md) marks it as necessary. [F-06 voice logging](../Features%20list/F-06-ai-chat-voice-logging.md) is explicitly not MVP. This analysis treats photo logging and correction as the binding MVP scope; AI chat should remain a decision to resolve before implementation.

## 5. Provider evidence

### LogMeal

**Documented strengths:**

- REST API for image recognition, ingredients, nutrition, intake history, and quantity workflows.
- The image segmentation endpoint detects multiple foods, recognizes them, and can log an intake.
- Nutrition endpoints return energy, carbohydrates, protein, fats, and more than 35 nutritional indicators.
- Quantity Detection API estimates quantities using a depth-based image sequence.
- Quantity can be confirmed in grams or Small/Medium/Big portions and nutrition is recalculated.
- The documented user-confirmation flow matches Snackshot's fast correction requirement.
- The trial provides 30 days of access; the pricing page separately states a maximum of 200 queries and 5 registered users.
- Standard plans allow 20 image recognitions per day per API user and one image per second per API user.
- The visible pricing examples are 144 EUR/month, 316 EUR/month, and 488 EUR/month, excluding VAT.

**Risks and gaps:**

- The two local documents describe the trial differently: the plans document says unlimited access to endpoints during the trial, while the pricing document says 200 queries and 5 users. Use the stricter 200-query limit until LogMeal confirms the current terms.
- Portion estimation may require a depth-based sequence rather than the single-photo flow described in the main MVP journey.
- No latency, retention, confidence-threshold, or full response-schema evidence is stored in the project.
- The provider has endpoints for history and goals, but Snackshot must not use provider history as its source of truth under the local-first architecture.

Sources: [LogMeal API overview](ai-models/logmeal-welcome-api.md), [LogMeal plans and limits](ai-models/logmeal-plans-limits.md), and [LogMeal pricing](ai-models/logmeal-pricing.md).

### Passio Nutrition-AI

**Documented strengths:**

- Nutrition-focused platform with image detection for ingredients and complete meals.
- Supports REST API and SDKs for Android, iOS, Flutter, React Native, and web projects.
- Includes voice recognition, barcode detection, nutrition-label OCR, packaging detection, semantic search, and a database of more than 3.5 million foods.
- Photo logging supports single foods and complex meals, with user confirmation or editing before logging.
- Natural-language food parsing can split a spoken sentence into loggable items.

**Risks and gaps:**

- The repository does not explicitly document visual gram/portion estimation or a depth-based quantity API, which is a core MVP requirement.
- Growth costs $99/month for 1M tokens; the documentation estimates 20,000-30,000 tokens for one photo, so the included volume is roughly 33-50 analyzed photos before other usage. This is an estimate, not a guaranteed request quota.
- The provider's token model is less directly comparable with LogMeal's image quotas.
- The platform is feature-rich, but the MVP should avoid implementing barcode, voice, OCR, and post-MVP workflows prematurely.

Sources: [Passio platform](ai-models/passio-platform.md) and [Passio pricing](ai-models/passio-pricing.md).

### Gemini API

**Documented strengths:**

- General multimodal API that can analyze images and produce text or structured JSON.
- Structured outputs can constrain responses for automated processing.
- Function calling could connect model output to a nutrition lookup service.
- Free access is available for developers and small projects; paid pricing is documented for production use.
- Gemini 2.5 Flash-Lite is listed at $0.10 per 1M text/image/video input tokens and $0.40 per 1M output tokens; Gemini 2.5 Flash is listed at $0.30 input and $2.50 output for text/image/video.
- The API is easy to prototype and can support future chat or voice features.

**Risks and gaps:**

- The repository provides no food-specific database, validated nutrition source, portion-estimation method, or accuracy evidence for this use case.
- The application would need an additional nutrition lookup and deterministic calculation layer to avoid asking a general model to invent calories and macros.
- The free plan states that content may be used to improve Google's products. That is a material privacy concern for food images and should not be treated as equivalent to local-only storage.
- A mobile client must not expose a long-lived provider API key; the backend-less architecture needs an explicit key-protection decision.
- The model list and prices are time-sensitive and require a final pre-implementation verification.

Sources: [Gemini API overview](ai-models/gemini-api-overview.md) and [Gemini pricing](ai-models/gemini-api-pricing.md).

### Bite AI

**Documented strengths:**

- Food Knowledge Graph with food information, nutrition facts, ingredients, translations, categories, and identifiers.
- Food-recognition endpoint returns references to items in the knowledge graph.
- Food Logging SDK manages users and meal history and can personalize recognition.
- REST API and client-library documentation are available.

**Risks and gaps:**

- The repository does not document portion or quantity estimation, macro calculation behavior for recognized portions, mobile SDK platform coverage, pricing, quotas, trial access, latency, or retention.
- The food logging SDK stores or manages user meal history, which needs careful reconciliation with Snackshot's local-first and stateless-client constraints.
- These gaps make it impossible to establish MVP cost or delivery feasibility from the current project evidence.

Source: [Bite AI introduction](ai-models/bite-ai-introduction.md).

## 6. Point-by-point comparison

Scores are from 1 (poor or unknown) to 5 (strong documented fit). They are interpretations of the repository evidence, not provider benchmarks.

| Dimension | Weight | LogMeal | Passio | Gemini | Bite |
|---|---:|---:|---:|---:|---:|
| MVP functional fit | 30% | 5 | 4 | 2 | 3 |
| Integration effort and mobile suitability | 20% | 4 | 4 | 4 | 2 |
| Nutrition data and portion support | 20% | 5 | 4 | 2 | 2 |
| MVP cost and limits | 15% | 4 | 2 | 5 | 1 |
| Local-first architecture fit | 10% | 4 | 4 | 2 | 2 |
| Evidence completeness and operational risk | 5% | 4 | 3 | 3 | 1 |
| **Weighted score** | **100%** | **4.45** | **3.65** | **2.75** | **2.15** |

### Interpretation

- **LogMeal** leads because it is the only documented option with a direct combination of food recognition, ingredients, nutrition extraction, confirmation, and quantity detection. Its principal risk is implementation and quota validation, not missing core capability.
- **Passio** is the strongest alternative. Its SDK/API breadth and food database reduce surrounding product work, but the documented price is significant for a free friends-and-family MVP and the repository does not prove portion estimation.
- **Gemini** is cheap and quick to prototype, but the project would be taking responsibility for nutrition correctness, food lookup, portion logic, API-key protection, and privacy configuration. That is too much risk for a 28-hour MVP.
- **Bite** has promising domain primitives, but too many decision-critical commercial and technical facts are absent from the project.

## 7. Key patterns and differences

1. **Domain specialization matters more than raw multimodal capability.** The MVP is not merely classifying an image. It needs a usable nutrition result, portion handling, correction, and a path to deterministic local storage.
2. **The nutrition layer is the deciding boundary.** A provider that recognizes food but does not supply trustworthy nutrition data shifts the hardest problem into the application.
3. **Portion estimation is the largest unresolved technical risk.** LogMeal documents a quantity workflow but may require multiple depth images. Passio and Bite do not document an equivalent method in the repository. Gemini can estimate visually, but there is no project evidence that the result is reliable.
4. **The local-first requirement does not mean no external processing.** It means the app must not use the provider as its historical datastore. Images and request data still leave the device, so retention, training use, transport security, and deletion policies must be confirmed for the selected provider.
5. **Free access and production suitability are different.** Gemini's free tier is attractive for experiments, but its content-use terms and missing nutrition layer weaken it as the production MVP choice. LogMeal's trial is attractive for validation, but the stricter documented query limit must drive the test plan.

## 8. Tradeoffs, risks, and variables

### Controllable variables

- Image preprocessing, compression, and camera guidance.
- The MVP uses one photo with user-editable grams; a separate depth quantity-capture flow is deferred.
- Prompt/schema design and validation of returned fields.
- An 80% confidence threshold and fallback to manual text search or quick add below the threshold.
- Whether a result is saved automatically or only after user correction/review.
- Test-set composition, request throttling, caching of non-personal reference data, and usage caps.
- Test-only local calculation of calories from returned macros using the project's Atwater rules: protein and carbohydrates at approximately 4 kcal/g, fat at approximately 9 kcal/g, with alcohol and other components handled explicitly when present. The test version displays this value alongside LogMeal's energy response for comparison; LogMeal's energy remains the application value. Source: [food-energy.md](common-data/food-energy.md).

### Uncontrollable or external variables

- Provider pricing, quotas, model updates, service availability, and API deprecation.
- Recognition performance for unusual foods, mixed meals, poor lighting, occlusion, and culturally specific dishes.
- Camera hardware and whether reliable depth data is available on the target Android devices.
- Health Connect permissions, device availability, and synchronization interruptions.
- User behavior, especially willingness to correct estimates and the test-only 95% Zero-Edit target.

### Critical unknowns to resolve before committing

- Does LogMeal return usable grams from the single-photo MVP flow, and can users correct grams in one lightweight interaction?
- How does the complete LogMeal response map to the application contract for food name, grams, confidence, energy, protein, carbohydrate, and fat?
- Are LogMeal trial limits currently 200 total queries and 5 users, or is the plans document accurate about unlimited trial endpoint access?
- What are the providers' image retention, training-use, deletion, and data-processing terms?
- Can the selected API be called without exposing credentials in a mobile-only client?
- What is the p50/p95 recognition latency on representative images?
- How large are the differences between LogMeal's energy response and the test-only Atwater comparison across representative images?
- Does the 80% confidence threshold produce an acceptable balance between direct review and manual fallback?

## 9. Recommendation and rationale

### Recommended MVP choice: LogMeal, with a bounded technical spike

Use LogMeal as the primary provider for the photo recognition pipeline because it has the best documented match to the core MVP: recognition, ingredients, nutrition extraction, confirmation, and quantity detection. It also offers a REST API that can be called as a stateless processing dependency while Snackshot keeps its own local database as the source of truth.

The recommendation is conditional. Do not build the complete UI around LogMeal until the single-photo grams response, response contract, limits, privacy terms, and mobile credential strategy pass the technical spike. The MVP uses an explicit user-entered portion correction; a depth-based quantity flow is deferred.

### Fallback choice: Passio

Choose Passio if LogMeal's quantity flow or commercial terms fail validation. Passio has the strongest documented SDK/API and nutrition ecosystem after LogMeal, including Android and cross-platform integration, a large food database, photo confirmation, semantic search, and packaging support. The $99/month Growth plan and token consumption make it less suitable for an unfunded MVP, but it may reduce implementation risk enough to justify the cost if the technical spike demonstrates better end-to-end performance.

### Not recommended as primary MVP provider

- **Gemini:** Use only as a controlled prototype or later conversational layer unless Snackshot adds a validated nutrition database, deterministic portion/nutrition logic, secure key handling, and a paid privacy configuration.
- **Bite:** Reconsider after obtaining current pricing, quotas, platform support, portion behavior, retention terms, and a complete API contract.

## 10. Next steps and decision triggers

### Technical spike

Implement only the provider adapter and a thin local test harness before committing to the full camera and Health Connect flow.

1. Create a self-made dataset of at least 30 meal photos with known ground-truth food names and approximate weights in grams. Include single foods, mixed meals, packaged products, raw produce, drinks, and different plate or container sizes.
2. Capture the same or comparable meals under different real-world conditions: daylight, indoor light, low light, shadows, different camera angles and distances, cluttered backgrounds, partially occluded food, and varied presentation. Record the capture condition for every image.
3. Send the minimum supported single-photo request to LogMeal and record latency, HTTP status, quota consumption, response fields, confidence values, grams or portion values, energy, protein, carbohydrate, fat, and whether manual gram correction is needed.
4. Compare the response with the ground-truth label and weight. Apply the 80% confidence threshold, mark identification as usable or requiring fallback, and mark the grams result as correct, incorrect but correctable in one lightweight interaction, or unusable.
5. In the test version, calculate energy with the local Atwater formula and display it alongside LogMeal's energy response. Record differences for analysis; use LogMeal's energy as the application value.
6. Test a manual correction path for food name and grams. Recalculate calories and macros locally and verify that the corrected record maps to the Health Connect `NutritionRecord` fields.
7. Require explicit user consent before recognition and verify that no provider history or external user profile is required. Review privacy, retention, training-use, deletion, and key-protection terms with the provider before release.
8. Repeat a smaller subset with Passio if any LogMeal gate fails.

### MVP acceptance gates

Proceed with LogMeal if all of the following are true:

- At least 80% of the self-made test images return a usable food identification or an actionable fallback across the selected capture conditions.
- At least 70% return a grams result that can be corrected in one lightweight interaction.
- In development and test modes, target 95% of recognized data requiring no manual edit and record all manual edits locally for model evaluation.
- p95 photo-to-result latency is acceptable for the intended flow; the exact target must be set before testing.
- The provider can remain a stateless processing dependency and does not require Snackshot to store user history externally.
- The trial/quota supports the planned friends-and-family test set.
- Provider terms permit the intended handling of food images and health-related data.

These thresholds are proposed validation gates, not existing project metrics. They should be approved or adjusted before the spike.

### Decision triggers

- **Switch to Passio** if LogMeal cannot provide a workable quantity flow, acceptable privacy terms, or sufficient trial quota, and Passio's SDK can produce a faster reliable correction flow.
- **Use Gemini only for a prototype** if the goal is to test the conversational UI rather than validate nutrition accuracy.
- **Defer the provider decision** if neither domain provider can satisfy portion and privacy requirements; ship manual text search and quick calorie entry as the validation slice instead of presenting untrustworthy AI nutrition results.

## 11. Final decision statement

Based on the evidence currently in the project, LogMeal is the most fit provider for Snackshot's photo-based MVP because it minimizes the amount of nutrition-specific logic the team must invent within a 28-hour delivery window. The decision remains provisional until the single-photo grams response, trial limits, privacy terms, credential strategy, provider-field mapping to the application contract, and representative-image performance are verified. The application should preserve a provider adapter boundary so Passio can replace LogMeal without changing local storage, correction logic, macro calculation, or Health Connect synchronization.
