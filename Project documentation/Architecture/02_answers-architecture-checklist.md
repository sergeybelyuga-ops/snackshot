# Health Connect Documentation — Answers to Must-Answer Architecture Questions

This document cross-checks every question from [01_must-answer-architecture-checklist.md](./01_must-answer-architecture-checklist.md) against the fetched Health Connect documentation (`HC_*.md` files in this folder) and project-level architecture decisions, recording what is already answered, partially answered, or still unknown.

Legend: ✅ Answered · 🟡 Partially answered · ❌ Not answered (not covered by HC docs — requires a separate product/architecture decision)

## Summary table

| # | Chapter | Question | Status | Source |
| --- | --- | --- | --- | --- |
| 1.1 | 1. Platform and app structure | Target platform: Android only or cross-platform? | ✅ | Product decision (see below) |
| 1.2 | 1. Platform and app structure | Client technology stack? | ✅ | [04_stack-comparative-analysis.md](./04_stack-comparative-analysis.md) |
| 1.3 | 1. Platform and app structure | Main app modules/layers? | ✅ | [04_stack-comparative-analysis.md](./04_stack-comparative-analysis.md) |
| 1.4 | 1. Platform and app structure | Navigation model between screens? | ✅ | Product decision; hybrid navigation |
| 1.5 | 1. Platform and app structure | Local app state/lifecycle model? | ✅ | Product decision; restore full unfinished meal draft after process death |
| 2.1 | 2. Health Connect and Google account dependency | Is a Google account mandatory for Health Connect access? | ✅ | Product decision (see below); [HC_availability.md](./HC_availability.md), [HC_get_started.md](./HC_get_started.md) |
| 2.2 | 2. Health Connect and Google account dependency | When is Google account login required? | ✅ | Product decision (see below); [how-do-i-use-health-connect-with-the-google-health-app.md](./how-do-i-use-health-connect-with-the-google-health-app.md) §Connect the Google Health app to Health Connect |
| 2.3 | 2. Health Connect and Google account dependency | What happens if user has no Google account / declines access? | ✅ | Product decision (see below); [HC_get_started.md](./HC_get_started.md) §Step 4 |
| 2.4 | 2. Health Connect and Google account dependency | Can users enable/disable/re-enable HC integration later? | ✅ | [HC_architecture.md](./HC_architecture.md) §Permissions management, [HC_get_started.md](./HC_get_started.md) §Onboard users, [how-do-i-use-health-connect-with-the-google-health-app.md](./how-do-i-use-health-connect-with-the-google-health-app.md) §Manage Health Connect data permissions |
| 2.5 | 2. Health Connect and Google account dependency | Fallback behavior when HC unavailable/denied/unsupported? | ✅ | [HC_get_started.md](./HC_get_started.md) §Step 3, [HC_availability.md](./HC_availability.md) |
| 3.1 | 3. Data ownership and local-first architecture | What data stays only on device? | ✅ | Architecture diagrams + project charter; local logs, corrections, settings, and processed images remain local |
| 3.2 | 3. Data ownership and local-first architecture | What data is written to Health Connect? | ✅ | Product decision + Health Connect sync flow; meal nutrition records only |
| 3.3 | 3. Data ownership and local-first architecture | What data is intentionally not stored externally? | ✅ | Product decision; no app-owned backend stores user health data; a minimal LogMeal Access Broker stores only anonymous AI-access status and usage counters ([11](./11_logmeal-access-token-management.md)); Google Health synchronization is optional |
| 3.4 | 3. Data ownership and local-first architecture | Source of truth: local, HC, or both? | ✅ | Product decision (see below); [HC_sync_data.md](./HC_sync_data.md) §Synchronize data (intro) |
| 3.5 | 3. Data ownership and local-first architecture | How is local-first storage organized/persisted? | ✅ | Product decision (see below); [HC_data_types.md](./HC_data_types.md), [HC_write_data.md](./HC_write_data.md) §Records with units of measurement |
| 3.6 | 3. Data ownership and local-first architecture | Consistency handling when record exists in both stores? | ✅ | [HC_sync_data.md](./HC_sync_data.md) §Feed data to Health Connect, §Store Health Connect IDs |
| 4.1 | 4. Data model and sync logic | Exact fields stored per meal entry? | ✅ | Product decision; core meal fields |
| 4.2 | 4. Data model and sync logic | Which fields map directly to HC nutrition records? | ✅ | [HC_write_data.md](./HC_write_data.md), [HC_aggregate_data.md](./HC_aggregate_data.md) §Supported aggregate data types |
| 4.3 | 4. Data model and sync logic | How are calories/protein/carbs/fat represented? | ✅ | Product decision (see below); [common-data/food-energy.md](./common-data/food-energy.md); [HC_write_data.md](./HC_write_data.md) §Records with units of measurement |
| 4.4 | 4. Data model and sync logic | How are timestamps/photo refs/user edits tracked? | ✅ | Product decision; current-state metadata without edit history |
| 4.5 | 4. Data model and sync logic | Sync strategy for create/update/delete? | ✅ | [HC_sync_data.md](./HC_sync_data.md) (full document) |
| 4.6 | 4. Data model and sync logic | Is sync automatic, manual, or both? | ✅ | Product decision + process flow; [HC_sync_data.md](./HC_sync_data.md) §Import timings |
| 4.7 | 4. Data model and sync logic | How often are goals refreshed from HC? | ✅ | Product decision + process flow; refresh on app activation and manual sync |
| 4.8 | 4. Data model and sync logic | Conflict resolution strategy when app and HC data differ? | ✅ | [HC_write_data.md](./HC_write_data.md) §Value check in Client Record Version |
| 5.x | 5. AI food recognition and portion estimation | AI food recognition and portion estimation questions | ✅ | Product decision; 80% threshold, single-photo grams correction, test-only 95% Zero-Edit target, and explicit recognition consent |
| 6.1–6.4 | 6. Nutrition calculation logic | Nutrition calculation source/composition/manual edits | ✅ | Product decision; provider-first values with local recalculation after edits |
| 6.5 | 6. Nutrition calculation logic | Historical aggregation and weekly averages? | ✅ | [HC_aggregate_data.md](./HC_aggregate_data.md) §Buckets (Period) |
| 6.6 | 6. Nutrition calculation logic | Daily totals displayed/updated in real time? | ✅ | Product decision; immediate local update plus background Health Connect refresh |
| 7.1 | 7. User flows and UX dependencies | First-time onboarding flow? | ✅ | Product decision; minimal onboarding with contextual permissions |
| 7.2 | 7. User flows and UX dependencies | Camera workflow and permission model? | ✅ | Product decision; contextual camera/gallery permission requests |
| 7.3 | 7. User flows and UX dependencies | AI chat input flow (text/audio/image/gallery)? | ✅ | Product decision; text and image inputs |
| 7.4 | 7. User flows and UX dependencies | Manual text search / quick manual entry flow? | ✅ | Product decision; search first with quick-add fallback |
| 7.5 | 7. User flows and UX dependencies | Handling invalid input, failed recognition, cancellation? | ✅ | Product decision; retry once, then preserve draft and offer fallback |
| 7.6 | 7. User flows and UX dependencies | Edit or delete a previously saved log entry? | ✅ | Product decision + [HC_write_data.md](./HC_write_data.md) §Update data, [HC_sync_data.md](./HC_sync_data.md) §Delete data from Health Connect |
| 8.1 | 8. Privacy, security, and compliance | Does the app store data on external servers? | ✅ | Product decision; no health data on external app servers (local-only mode or optional Google Health synchronization); the LogMeal Access Broker keeps anonymous AI-access records only |
| 8.2 | 8. Privacy, security, and compliance | Where is personal nutrition data stored/protected? | ✅ | Product decision; Android app-private encrypted storage |
| 8.3 | 8. Privacy, security, and compliance | Consent text shown before HC authorization? | ✅ | Product decision + [HC_get_started.md](./HC_get_started.md) §Show your app's privacy policy dialog |
| 8.4 | 8. Privacy, security, and compliance | User data retention model? | ✅ | Product decision; immediate raw-photo deletion, 365-day processed-photo retention, indefinite nutrition-log retention until user deletion |
| 8.5 | 8. Privacy, security, and compliance | PII/health data compliance review needed? | ✅ | Product decision; privacy and health-data compliance review is required for local storage and external transfers |
| 8.6 | 8. Privacy, security, and compliance | Privacy guarantees for local-only mode? | ✅ | Product decision; app-private encrypted storage, scoped access, deletion controls, and no app-owned backend for health data |
| 9.1 | 9. Performance, reliability, and scalability | Target recognition latency? | ✅ | Product decision; p98 under 3 seconds |
| 9.2 | 9. Performance, reliability, and scalability | Acceptable success rate for AI recognition/zero-edit? | ✅ | Product decision; 90% usable result or actionable fallback |
| 9.3 | 9. Performance, reliability, and scalability | Behavior if AI service is slow/unavailable? | ✅ | Product decision; queue for later processing |
| 9.4 | 9. Performance, reliability, and scalability | Retry strategy after failed requests? | ✅ | Product decision; one retry, then background exponential retry |
| 9.5 | 9. Performance, reliability, and scalability | Behavior with larger histories/dashboards/entries? | ✅ | Product decision; show more after seven days and cached aggregates |
| 9.6 | 9. Performance, reliability, and scalability | Reliability requirements for sync failures/retries? | ✅ | [HC_sync_data.md](./HC_sync_data.md) §Best practices for syncing data |
| 10.x | 10. Metrics and validation | Metrics and validation questions | ✅ | Product decision; current MVP targets approved, instrumentation is an implementation task |

**Coverage:** 45 answered, 0 partially answered, and 0 unanswered. The AI provider decision is now recorded as LogMeal for the MVP, subject to technical and compliance validation in [03_ai-model-comparative-analysis.md](./03_ai-model-comparative-analysis.md). The stack decision is recorded in [04_stack-comparative-analysis.md](./04_stack-comparative-analysis.md).

---

## Detailed answers

### Product decision: Android-only MVP platform

The MVP target platform is **Android**. Android is the primary platform for the initial release because the MVP depends on Google Health Connect integration and the LogMeal Android Depth SDK is the relevant path for future quantity estimation.

After the MVP, the application is planned to be adapted for **iOS**. Web and desktop versions are not planned and are outside the product scope.

This decision answers question 1.1. The client technology stack, major application layers, navigation model, and draft-lifecycle behavior are defined below.

**1.4 Navigation model between screens — ✅**

The MVP uses hybrid navigation: primary navigation for the main application areas, with nested or modal flows for meal capture, recognition review, editing, settings, and permissions. Exact route names remain a UI design detail.

### Product decision: native Android Kotlin MVP stack

The MVP will use a **native Android Kotlin stack**:

- Kotlin as the application language.
- Jetpack Compose for the Android UI, with native Android components where required.
- CameraX or the selected Android camera API for lifecycle-aware camera capture.
- Android Health Connect SDK in a dedicated integration boundary.
- Local application persistence behind repository interfaces; Health Connect remains an optional synchronization target.
- LogMeal REST API behind a provider adapter, with the LogMeal Android Depth SDK added only if device and plan validation succeeds.

The application will not introduce a cross-platform UI framework, web target, or desktop target in the MVP. Platform-neutral domain models, nutrition calculations, LogMeal contracts, validation, provider interfaces, repository interfaces, synchronization abstractions, and error types should remain modular so they can be extracted into Kotlin Multiplatform shared modules when iOS development begins. Android Health Connect, future iOS HealthKit, camera implementations, permissions, and platform UI remain platform-specific.

This decision answers question 1.2. It also answers question 1.3 at the major-layer level: presentation, domain, local data, Health Connect, camera, and LogMeal integration. The detailed package/module structure remains an implementation task.

**1.5 Local app state/lifecycle model — ✅**

The app separates presentation, domain, local data, and integrations. When a meal flow is unfinished, the app persists the full draft, including the processed image, recognition result, corrections, and current review state, so it can be restored after backgrounding or process death. Completed records continue to be persisted in the local database.

### Product decision: local-first storage with optional Google Health connection

The app does **not** require a Google account or Health Connect for its core function: health/nutrition data is stored locally on-device from the first launch. Google Health integration is an entirely optional, user-initiated add-on:

1. The app works standalone, storing data locally, with no Google account and no registration needed. This also applies to AI recognition: AI access is obtained silently with an anonymous identity tied to the device (see [11_logmeal-access-token-management.md](./11_logmeal-access-token-management.md)).
2. If the user wants their data available in Google Health, they must install the Google Health app themselves and register/sign in with a Google Account inside that app (not inside ours).
3. In the Google Health app's settings, the user adds our app to their list of connected apps ("Partner apps").
4. Once connected, our app can write data to Google Health (via Health Connect) and share it with other apps connected through Health Connect.
5. The Android Health Connect SDK is the integration layer that simplifies this connection between our app and Google Health/Health Connect — our app talks to the SDK, not directly to Google Health.

The C4 context, container, and component diagrams confirm the same architectural model:

- The user story is: open the app without registration, capture meals, review AI results, save logs locally, and optionally view or synchronize health data in Google Health.
- The application is the system boundary and keeps local history, settings, corrections, and nutrition records as the primary source of truth.
- LogMeal is a consumer-facing recognition service, not a database.
- Google Health is a health-data presentation and sync target, not the primary app datastore.

This decision directly answers the following checklist questions:

### 2. Health Connect and Google account dependency

**2.1 Is a Google account mandatory for Health Connect access? — ✅**
No. A Google account is only required if/when the user chooses to connect the app to Google Health; it is never required for the app's core local-first functionality.

**2.2 When is Google account login required? — ✅**
Only at the point the user opts in to connect with Google Health — not during onboarding and not before the first local save. The login itself happens inside the Google Health app (via its own sign-in/registration), not inside our app.

**2.3 What happens if the user has no Google account or declines access? — ✅**
The app continues to work fully with local-only storage. Without a Google account and without installing/connecting Google Health, the Health Connect integration simply remains inactive; no feature required for core logging is blocked.

### 3. Data ownership and local-first architecture

**3.1 What data stays only on device? — ✅**
The architecture diagrams and the product charter define local-only data as the app's meal logs, user-generated corrections, timestamps, local history, settings, sync metadata, and any photo reference or local image metadata. This data remains on the device and is not persisted on an external app server.

For image retention, the implementation follows a clear product rule:

- When the user takes a photo inside the app, the app processes the file, saves a processed/improved copy for the meal record, and deletes the original captured file.
- When the user chooses a photo from the gallery, the app processes the selected file, saves a processed/improved copy for the meal record, and keeps the original gallery file untouched in the user's device gallery.
- The app's retained image is the processed copy; the original image is not stored in the app for camera captures, while gallery-sourced originals remain outside the app's control.

The processed copy is saved permanently in the app's internal storage. The lifecycle of the original source file is defined by source type: captured originals are deleted after processing, while gallery originals remain in the user's gallery outside the app's control.

**3.2 What data is written to Health Connect? — ✅**
When the user has enabled Google Health and granted permissions, the app writes meal nutrition records to Health Connect using the `NutritionRecord` model. The written scope is limited to meal nutrition values and the meal timestamp, including energy, protein, carbohydrates, and fat. Goals, raw AI provider history, corrections history, and non-health app metadata are not written to Health Connect.

**3.3 What data is intentionally not stored externally? — ✅**
The app has no Snackshot backend for user data and does not store meal logs, nutrition history, photos, or other health data on an app-owned external server. The only server component is the **LogMeal Access Broker**, which stores an anonymous client ID, a hashed device ID, the user's own LogMeal user ID and encrypted token, the AI picture counter, and trial/subscription status. It never receives photos or nutrition data. See [11_logmeal-access-token-management.md](./11_logmeal-access-token-management.md). In local-only mode, meal history, manual corrections, settings, and processed images remain on the device; photo or text input is sent to LogMeal only for recognition processing and LogMeal is not used as the user's database. In Google Health-connected mode, the user explicitly opts in to synchronize the selected nutrition data with the Google Health account they choose. The local application database remains the source of truth.

**3.4 Source of truth — ✅**
The source of truth is the local application database. Health Connect/Google Health is an optional, user-opted-in synchronization target, consistent with HC's own recommendation that integrating apps keep their own datastore as the source of truth. Source: [HC_sync_data.md](./HC_sync_data.md) (intro).

**3.5 How is local-first storage organized/persisted? — ✅**
To simplify connection and synchronization with Google Health, the local database mirrors Health Connect's own structure: local tables use the same data types, field names/units, and dimensions (e.g. energy in kilocalories, nutrient mass fields in grams) as the corresponding Health Connect record (e.g. `NutritionRecord`). This keeps mapping a local record to a Health Connect record a direct field-for-field translation instead of a lossy conversion. Source: [HC_data_types.md](./HC_data_types.md); [HC_write_data.md](./HC_write_data.md) §Records with units of measurement.

### 2. Health Connect and Google account dependency (Health Connect documentation detail)

**2.1 Is a Google account mandatory for Health Connect access? — 🟡 Partial (HC docs only)**
HC docs don't state a Google-account requirement explicitly. They only state device-level requirements: Android 9+ (API 28) with Google Play services installed; on Android 14+ HC is bundled in the OS, on Android 13 and below it's a Play Store app. Source: [HC_availability.md](./HC_availability.md).

As a reference example, the Google Health app (a Health-Connect-connected app) *does* require an account: *"You must sign in to the Google Health app using an adult Google Account."* This is a per-app decision made by Google Health, not a Health Connect platform requirement — our app must decide this independently. Source: [how-do-i-use-health-connect-with-the-google-health-app.md](./how-do-i-use-health-connect-with-the-google-health-app.md) §Connect the Google Health app to Health Connect.

**2.3 What happens if the user has no Google account / declines access? — 🟡 Partial**
Not stated for "no Google account". For declined permissions, the docs only say the app must handle it: *"Because users can grant or revoke permissions at any time, your app needs to check for permissions every time before using them and handle scenarios where permission is lost."* Source: [HC_get_started.md](./HC_get_started.md) §Step 4 (Request permissions from the user).

The Google Health app shows a concrete example of a granular consent flow that could inform our own design: users are asked per data category to "Allow all," "Allow select items," or "Don't allow" (separately for medical records and for fitness/wellness data), plus a further prompt for background/historical access. Source: [how-do-i-use-health-connect-with-the-google-health-app.md](./how-do-i-use-health-connect-with-the-google-health-app.md) §Connect the Google Health app to Health Connect.

**2.4 Can users enable/disable/re-enable HC integration later? — ✅**
Yes. Health Connect provides a permissions management UI where users control per-app access at any time, and an app's onboarding activity "may be launched more than once, for example if the user later revokes permissions ... and then reconnects it." Source: [HC_architecture.md](./HC_architecture.md) §Permissions management; [HC_get_started.md](./HC_get_started.md) §Onboard users.

The Google Health app example shows the concrete user-facing controls: per-permission toggles ("Fitness and wellness," "Medical records," "Additional access") and a "Remove access for this app" action that revokes permissions and deletes data. Source: [how-do-i-use-health-connect-with-the-google-health-app.md](./how-do-i-use-health-connect-with-the-google-health-app.md) §Manage Health Connect data permissions.

**2.5 Fallback behavior when HC unavailable, denied, or unsupported? — ✅**
Check `HealthConnectClient.getSdkStatus(context)`; if `SDK_UNAVAILABLE`, show a message like "Health Connect is not available on this device..." and skip client creation. Availability also depends on Android version (9+ required, 14+ has it built in). Source: [HC_get_started.md](./HC_get_started.md) §Step 3 (Check feature availability); [HC_availability.md](./HC_availability.md).

### 3. Data ownership and local-first architecture (Health Connect documentation detail)

**3.4 Source of truth — ✅**
The Snackshot application is the source of truth, specifically its local application database. Health Connect is an optional synchronized secondary store and is not authoritative for the app's meal logs, corrections, settings, or nutrition history. This matches the Health Connect recommendation that *"Most apps that integrate with Health Connect have their own datastore that serves as the source of truth."* Source: [HC_sync_data.md](./HC_sync_data.md) (intro).

If Health Connect contains nutrition records that are missing from local storage, the app imports them into the local database. This bootstrap import runs on the first connected use and after the user deletes all local app data. The app requests permission, reads the available Health Connect nutrition records, upserts them locally, stores the associated Health Connect identifiers and sync metadata, and then resumes normal change-token synchronization. Imported data becomes part of the local source of truth after the import completes.

**3.6 Consistency handling when the same record exists in both stores — ✅**
Use `clientRecordId`/`clientRecordVersion` metadata on each record; `insertRecords` upserts (overwrites) matching Health Connect records when the client version is higher, otherwise writes as new. Store the returned Health Connect record `id` locally to reconcile future deletions. Source: [HC_sync_data.md](./HC_sync_data.md) §Feed data to Health Connect, §Store Health Connect IDs.

### 4. Data model and sync logic

**4.1 Exact fields per meal entry — ✅**
Each local meal entry stores the core fields: food name, portion in grams, energy, protein, carbohydrates, fat, timestamp, processed-image reference, and current user-edited values. The local model may retain sync identifiers needed to map the meal to Health Connect, but it does not require the full Health Connect micronutrient set for the MVP.

**4.2 Fields mapping to Health Connect nutrition records — ✅**
Full nutrient list is documented via the aggregate metrics table (e.g. `ENERGY_TOTAL`, `PROTEIN_TOTAL`-equivalents via nutrients, `DIETARY_FIBER_TOTAL`, `CALCIUM_TOTAL`, etc.) and the `NutritionRecord` example fields. Source: [HC_aggregate_data.md](./HC_aggregate_data.md) §Supported aggregate data types by record (NutritionRecord row); [HC_write_data.md](./HC_write_data.md).

**4.3 How calories/protein/carbs/fat are represented — ✅**
Energy fields use the `Energy` unit (e.g. `105.0.kilocalories`); nutrient mass fields (carbs, fat, protein, fiber, sugar, etc.) use the `Mass` unit (e.g. `27.0.grams`), matching Health Connect's `NutritionRecord` representation. Source: [HC_write_data.md](./HC_write_data.md) §Records with units of measurement.

To calculate a food entry's total energy (calories) from its macros, the app uses the Atwater energy-conversion factors per gram, as described in [common-data/food-energy.md](./common-data/food-energy.md):

- Protein: ~17 kJ/g (4 kcal/g)
- Carbohydrate: ~17 kJ/g (4 kcal/g)
- Fat: ~37 kJ/g (9 kcal/g)
- Alcohol (ethanol): ~29 kJ/g (7 kcal/g)
- Dietary fiber: excluded, or counted at a reduced ~8 kJ/g (2 kcal/g) depending on the regulation followed
- Polyols (sugar alcohols): ~10 kJ/g (2.4 kcal/g)
- Organic acids: ~13 kJ/g (3.1 kcal/g)

Formula: `energy_kcal = protein_g * 4 + carbs_g * 4 + fat_g * 9 + alcohol_g * 7`. This total is stored in the `energy` (`Energy`) field, while each macro is stored in its own `Mass` field (`protein`, `totalCarbohydrate`, `totalFat`), so the derived energy value and its source macros are both persisted and kept consistent.

**4.4 Timestamps, photo references, user edits — ✅**
The local database stores the meal timestamp, processed-image reference, current nutrition values, and the latest user corrections. It does not retain a full edit history. Health Connect synchronization metadata such as record identifiers and client versions is stored separately from the user-facing meal values. Source: [HC_write_data.md](./HC_write_data.md) §Update data (Metadata).

**4.5 Sync strategy for create/update/delete — ✅**
Full workflow documented: on first connected use or after local data reset, bootstrap missing nutrition records from Health Connect via `readRecords`; feed local changes to HC via `insertRecords` (upsert); pull subsequent changes via `getChangesToken`/`getChanges` (handles `UpsertionChange`/`DeletionChange`); and delete via `deleteRecords` (by id/clientRecordId or time range). Source: [HC_sync_data.md](./HC_sync_data.md) (full document).

**4.6 Automatic or manual sync — ✅**
Synchronization uses both automatic and manual triggers. The app starts synchronization each time it becomes active, and the user can trigger synchronization explicitly from the app. While synchronization is running, the UI shows a visible syncing state and progress; when it finishes, the UI reports completion or failure. Source: product decision and [09_process-flow-diagrams.md](./09_process-flow-diagrams.md); Health Connect recommends checking for new data each time the app becomes active in the foreground. Source: [HC_sync_data.md](./HC_sync_data.md) §Import timings.

**4.8 Conflict resolution when app and HC data differ — ✅**
Comparison is based on `clientRecordVersion`: upsert applies only if the incoming version is higher than the stored one; otherwise the existing HC record is left unchanged. Source: [HC_write_data.md](./HC_write_data.md) §Value check in Client Record Version.

### Product decision: LogMeal as the MVP AI provider

The comparative analysis selects **LogMeal** as the primary AI provider for the MVP. This is a provisional implementation decision, not a claim that provider accuracy has already been validated.

LogMeal is the best documented fit because its API combines food-image recognition, ingredient extraction, nutritional information, user confirmation, and quantity detection. Its REST API will be treated as a stateless processing dependency. Snackshot's local application database remains the source of truth; LogMeal history, goals, or user-management features must not replace local storage. Source: [03_ai-model-comparative-analysis.md](./03_ai-model-comparative-analysis.md), [logmeal-welcome-api.md](./ai-models/logmeal-welcome-api.md), [HC_sync_data.md](./HC_sync_data.md).

The implementation must keep a provider adapter boundary so Passio can replace LogMeal without changing local meal storage, correction logic, macro calculation, or Health Connect synchronization. Passio is the fallback candidate if LogMeal's quantity flow, privacy terms, quota, or response contract fails validation. Gemini and Bite are not selected as the primary MVP nutrition engine because the project evidence does not establish a complete nutrition and portion-estimation solution for either provider.

**5.x AI food recognition and portion estimation — ✅**

- **Provider:** LogMeal is selected for MVP image recognition, ingredients, nutrition, and quantity detection.
- **Input/output contract:** The application response contract requires food name, portion in grams, confidence, energy, protein, carbohydrate, and fat. The technical spike must verify how LogMeal's response fields map to this contract and how unavailable or low-confidence values are represented.
- **Portion estimation:** The MVP uses a single photo. When grams are returned, the user can edit the grams before saving; a depth-based multi-image quantity flow is deferred.
- **Confidence and fallback:** The app uses an 80% confidence threshold. Results at or above the threshold are shown for user correction before saving; results below it use manual search or quick add.
- **Validation gates:** Test at least 30 self-made representative meal images captured in different real-world conditions; target at least 80% usable identification or actionable fallback and at least 70% portion results correctable in one lightweight interaction.
- **Zero-Edit measurement:** The target is 95% of recognized data requiring no manual edit, measured only in development and test modes. All manual edits are recorded locally and used for development statistics; this instrumentation is not part of the production user data model.
- **Recognition consent:** LogMeal recognition requires explicit user consent and clear disclosure that the selected photo or text is transferred for processing. Provider privacy and retention terms must still pass legal/compliance review.

**6.1–6.4 Nutrition calculation source, composition, and manual edits — ✅**

LogMeal supplies the initial nutrition information for recognized foods and confirmed quantities. Snackshot stores calories, protein, carbohydrates, and fat locally using the Health Connect units: `Energy` for kilocalories and `Mass` for nutrient grams. User corrections are applied locally, recalculated where needed, and immediately reflected in the displayed totals before the record is saved or synchronized. LogMeal remains the initial provider authority; local recalculation becomes authoritative for the corrected local record.

LogMeal's energy value is the initial application nutrition result. The Atwater calculation is available only in the test version of the application and is displayed alongside the LogMeal response for comparison and validation; it is not used as the production calorie value. The test build uses the formula `energy_kcal = protein_g * 4 + carbs_g * 4 + fat_g * 9 + alcohol_g * 7` and records differences for evaluation. Source: [food-energy.md](./common-data/food-energy.md), [03_ai-model-comparative-analysis.md](./03_ai-model-comparative-analysis.md).

For composite meals, the MVP uses a hybrid approach: start with a combined meal estimate and allow a component-based breakdown when the user needs greater accuracy.

### 6. Nutrition calculation logic

**6.5 Historical aggregation and weekly averages — ✅**
Use `aggregateGroupByPeriod` (date-based buckets like week/month) or `aggregateGroupByDuration` (fixed-length buckets like minute/hour) against `AggregateRequest` metrics. Source: [HC_aggregate_data.md](./HC_aggregate_data.md) §Buckets.

**6.6 Daily totals displayed/updated in real time — ✅**
Daily totals update immediately from the local application database after a meal is saved or edited. Health Connect synchronization runs in the background and reconciles the local view later; the UI does not wait for Health Connect before showing the local result. Source: product decision and [HC_aggregate_data.md](./HC_aggregate_data.md) §Basic aggregation.

### 7. User flows and UX dependencies

**7.1 First-time onboarding flow — ✅**
The MVP uses minimal onboarding and lets the user begin meal logging without Health Connect. Optional permissions are introduced contextually when the user selects the related feature. Health Connect-specific education and consent are shown when the user chooses to connect it. Source: product decision and [HC_get_started.md](./HC_get_started.md) §Onboard users.

**7.2 Camera workflow and permission model — ✅**
The app requests camera permission when the user starts an in-app photo capture and requests gallery access when the user selects an image. Camera and gallery flows both pass through image preprocessing before recognition; the user is not blocked by either permission when using other logging methods.

**7.3 AI chat input flow — ✅**
The MVP supports typed text and camera/gallery images as AI inputs. Voice input is deferred. Text requests use the same local review, correction, and save flow as image recognition.

**7.6 Edit or delete a previously saved log entry — ✅**
Users edit or delete the local meal entry first. The app then synchronizes the change to Health Connect by updating or upserting the corresponding record, or deleting it when the local entry is deleted. Source: [HC_write_data.md](./HC_write_data.md) §Update data; [HC_sync_data.md](./HC_sync_data.md) §Delete data from Health Connect.

### 8. Privacy, security, and compliance

**8.1 Does the app store data on external servers? — ✅**
The application has no Snackshot backend for health data and supports two storage modes:

1. **Local-only mode:** all application data is stored on the device. The app does not persist data on an app-owned external server. Photos or text may be transferred to LogMeal for recognition, but LogMeal is a processing dependency rather than the application's data store.
2. **Google Health-connected mode:** application data remains in local storage and the user-selected nutrition data is synchronized with the user's Google Health account through Health Connect. The local application database remains the primary source of truth; the Google Health account is an optional external synchronized copy.

The user chooses whether to enable Google Health synchronization. The app must clearly communicate when recognition data is sent to LogMeal and when nutrition data is synchronized to Google Health.

In both modes, the **LogMeal Access Broker** (Firebase) holds a small AI-access record per anonymous client: client ID, hashed device ID, LogMeal user ID, encrypted per-user LogMeal token, picture counter (90 free pictures), and status (trial, trial ended, waitlisted, paused, cancelled). No name, e-mail, Google account, photo, or nutrition value is stored. This record must be disclosed in the privacy policy. See [11_logmeal-access-token-management.md](./11_logmeal-access-token-management.md).

**8.2 Where personal nutrition data is stored/protected — ✅**
The app stores personal nutrition data in Android app-private internal storage protected by Android access controls and database/file encryption. In connected mode, the synchronized nutrition copy is protected by the Google Health account and Health Connect permission controls. Source: [HC_architecture.md](./HC_architecture.md) §Data management, §Permissions management.

As a reference point, Google Health explicitly syncs data *out of* Health Connect into the user's Google account: *"Data from other apps connected to Health Connect synced to the Google Health app is stored in your account and may be used to calculate your Google Health metrics."* This supports the product decision to offer optional account synchronization while retaining the local application database as the source of truth. Source: [how-do-i-use-health-connect-with-the-google-health-app.md](./how-do-i-use-health-connect-with-the-google-health-app.md) §Understand Health Connect.

**8.3 Consent text shown before HC authorization? — ✅**
Before requesting Health Connect access, the app shows a privacy rationale explaining the data use and requests granular permissions rather than silently requesting all access. The rationale is aligned with the app's privacy policy and Health Connect's permission screen. Source: [HC_get_started.md](./HC_get_started.md) §Show your app's privacy policy dialog.

Google Health's own consent flow is a usable UX reference: it separates consent into medical records vs. fitness/wellness data, each with "Allow all / Allow select items / Don't allow" options, followed by a distinct prompt for historical and background data access. Source: [how-do-i-use-health-connect-with-the-google-health-app.md](./how-do-i-use-health-connect-with-the-google-health-app.md) §Connect the Google Health app to Health Connect.

**8.4 User data retention model — ✅**
The local-only retention policy is:

- Raw high-resolution photos captured inside the app are deleted immediately after preprocessing succeeds. Only the processed, optimized copy is retained by the app.
- Processed photo copies are cached locally for 365 days. After 365 days they are compressed or deleted by the retention cleanup process.
- Long-term nutritional logs remain in local storage indefinitely until the user explicitly deletes them or closes their app account.
- The user can delete selected data for a chosen period or permanently delete all application data through the app's deletion controls. Android's application-storage clear-data function is an additional device-level deletion mechanism.
- In Google Health-connected mode, the retention and deletion of the synchronized copy are governed by Google Health and its account controls; the app must request or perform the corresponding Health Connect deletion where its permissions and API support it.

The app uses WorkManager for periodic cleanup of expired processed images, temporary files, and stale offline sync queues. Retention cleanup must be scoped to the app's internal storage and must not delete the user's original gallery files.

**8.5 PII/health data compliance review needed — ✅**
A privacy and health-data compliance review is required. Local storage does not remove compliance obligations because the app processes personal nutrition and potentially health-related data, and it transfers photos or text to LogMeal for recognition when those features are used. The review must cover the privacy policy, consent and disclosure text, data minimization, deletion behavior, provider processing and retention terms, Health Connect permissions, account synchronization, and applicable GDPR, CCPA/CPRA, Google Play, and other jurisdictional requirements. No claim of automatic exemption or certification is made by using local storage.

**8.6 Privacy guarantees for local-only mode — ✅**
In local-only mode, application data is isolated in Android app-private internal storage and is not exposed through a Snackshot backend; the access broker never receives health data. Sensitive preferences and small key-value secrets use Android Keystore-backed encrypted preferences; photos and nutrition databases use app-private encrypted file/database storage rather than `EncryptedSharedPreferences`. Data is scoped to the application, protected by Android access controls, deleted according to the retention policy, and removable through the app's deletion controls or Android's clear-storage action. Recognition requests remain an explicit exception: the selected photo or text is transferred to LogMeal for processing and is subject to the provider's terms. The per-user LogMeal token is stored encrypted with an Android Keystore key and excluded from Android backup.

### 9. Performance, reliability, and scalability

**9.1 Target recognition latency — ✅**
The MVP target is p98 photo-to-result latency under 3 seconds under the defined test conditions.

**9.2 Acceptable recognition success rate — ✅**
The MVP target is 90% of test images producing a usable result or an actionable fallback.

**9.3 AI service slow or unavailable — ✅**
If the AI provider is slow or unavailable, the app queues the image for later processing and keeps the user informed. The user can continue using local logging and manual entry while queued processing is pending.

**9.4 Retry strategy after failed requests — ✅**
The app performs one immediate retry. If it fails, it retries in the background with an exponential delay. Until the request succeeds, the UI informs the user that the data has not been saved yet and offers the available manual fallback.

**9.5 Large histories and dashboards — ✅**
Meal logs older than seven days are loaded with a "Show more" interaction. Daily and weekly dashboard statistics are cached locally so dashboards do not need to load the complete history for every refresh.

HC documents standard exceptions and recommended handling: `IllegalStateException` (validate input before request), `IOException` (backup input, apply retry/backoff), `RemoteException` (regular re-sync, retry/backoff), `SecurityException` (verify declared permissions). The product retry policy above applies to both Health Connect and AI requests. Source: [HC_read_data.md](./HC_read_data.md) §Handle exceptions; [HC_write_data.md](./HC_write_data.md) §Handle exceptions.

**9.6 Reliability requirements for sync failures and retries — ✅**
Best practices documented: Changes tokens expire after 30 days unused, so combine strategies (dedupe on read, read-since-last-timestamp, delete-then-read last 30 days, or read last 30 days without dedupe); use separate Changes tokens per data type; handle foreground-read interruptions by resuming next app open; optionally use background-read permission. Source: [HC_sync_data.md](./HC_sync_data.md) §Best practices for syncing data.

### 10. Metrics and validation

The approved MVP metric set includes recognition quality, photo-to-result latency, correction rate, Zero-Edit rate, time-to-log, Health Connect sync success, and user retention or logging frequency. The approved operational targets include p98 photo-to-result latency under 3 seconds and 90% usable result or actionable fallback. Metric instrumentation is an implementation task.

---

## Remaining open decisions

The following questions remain open after combining the Health Connect research with the AI-provider comparative analysis:

- **Section 5** — The product behavior is defined; provider privacy/retention terms and technical performance still require validation.
- **Section 8** (partly) — External AI image processing, provider retention/training use, and final compliance evidence remain to be verified. Retention, compliance-review scope, local-only protections, and the two storage modes are now defined.
- **Section 9** — Measured provider latency, recognition results, and outage behavior still require technical-spike evidence; the operational retry and history policies are defined.
- **Section 10** — Metric instrumentation remains an implementation task; the metric set and current operational targets are approved.

These should be resolved during the LogMeal technical spike or marked explicitly as deferred before implementation begins.
