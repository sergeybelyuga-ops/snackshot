# data-local

Local persistence (WBS 2.4, decision 8). Everything is in app-private internal storage, encrypted at rest by Android file-based encryption, and excluded from backup and device transfer (`app/src/main/res/xml/data_extraction_rules.xml`, `backup_rules.xml`).

| Store | File | Holds | Domain interface |
| --- | --- | --- | --- |
| Room `SnackshotDatabase` | `databases/snackshot.db` | meals, Health Connect sync state, nutrition goals, product metric counters and samples | `MealRepository`, `SyncStateRepository`, `GoalsRepository`, `MetricsStore` |
| DataStore `settings` | `files/datastore/settings.preferences_pb` | onboarding flag, product metrics on/off, theme, recognition language and country | `OnboardingRepository`, `SettingsRepository` |
| DataStore `secrets` | `files/datastore/secrets.preferences_pb` | the LogMeal APIUser token as an AES-256-GCM blob; the key is in Android Keystore | `LogMealTokenStore` |
| DataStore `pending_meal` | `files/datastore/pending_meal.preferences_pb` | the unfinished meal flow as one JSON document (WBS 2.5) | `PendingMealStore` |
| Files | `files/meal-images/<uuid>.jpg` | processed meal photos; a meal stores the reference `meal-images/<uuid>.jpg` | `MealImageStore` |
| Files | `cache/captures/<uuid>.jpg` | raw camera captures, only until preprocessing succeeds | `MealImageStore` |

## Rules

- **Meal values and sync metadata are separate tables** (doc 02 §4.4). `sync_state` has no foreign key, so a pending remote delete outlives its meal. `RoomMealRepository.save` and `delete` update both tables in one transaction.
- **Times are epoch milliseconds (UTC), and each meal keeps its zone offset.** Enums are stored by name as TEXT and mapped explicitly in `Mappers.kt`.
- **The token is unreadable after a restore or a Keystore reset** (key gone or changed). In that case the store drops it and returns null, and the app provisions again through the broker (doc 11 §4).

## Meal draft and photos (WBS 2.5, doc 02 §1.5, §8.4)

- **The unfinished flow** (a photo waiting for recognition, or the review with every correction) is written to `pending_meal` on every change. It survives backgrounding, process death and a force stop, and there is at most one. `PendingMealJson.SCHEMA` versions the JSON. A draft that can't be read is dropped, never a saved meal.
- **The draft owns its photo until the meal is saved.**
  - `complete()` keeps the photo for the meal.
  - `discard()` and retakes delete it, unless a saved meal uses it. Discarding an edit of a saved meal keeps that meal's photo.
- **Raw captures are deleted by the caller** as soon as preprocessing succeeds (WBS 6.2). Gallery originals are never copied or touched.
- **Retention (`ImageRetentionCleaner`, at every app start):**
  - processed photos older than 365 days are deleted, and their meals keep every value without a photo (the reference is cleared without a sync change, because photos are not in Health Connect);
  - unreferenced photos (including those of deleted meals once undo is no longer possible) are deleted after a day, and so are leftover captures and interrupted writes;
  - doc 02 allows "compressed or deleted"; Snackshot deletes.
- **Image references are validated** (`meal-images/<uuid>.jpg` only), so a stored reference can't point outside the image directory.

## Migration strategy

1. **The schema is exported** to `schemas/app.snackshot.datalocal.db.SnackshotDatabase/<version>.json` on every build. The files are committed, and **a released version's file never changes.**
2. **A schema change bumps `SnackshotDatabase.VERSION`.**
   - For additive changes (new table, new nullable column, or a column with a default), add an `AutoMigration(from, to)` to `@Database`.
   - For anything else (rename, type change, data move), write a `Migration` and append it to `SnackshotDatabase.MIGRATIONS`.
3. **There is no destructive fallback** (`fallbackToDestructiveMigration` is never used). The meal log is the user's only copy unless Health Connect is connected, so a missing migration must crash in testing instead of wiping data.
4. **`MigrationTest` checks** that every version up to the current one is exported, and that the oldest schema migrates to the current one and matches the exported schema. With each new version, add a test that inserts rows at the old version and reads them back after the migration.
5. **Downgrades are not supported.** Builds only move forward.

## Tests

`./gradlew :data-local:testDebugUnitTest` runs on the JVM.
- **Room:** uses Robolectric with `sqliteMode=LEGACY`, which avoids the native runtime DLL that Smart App Control blocks on dev PCs.
- **DataStore:** tests use Okio storage (`TestDataStore.kt`), because DataStore's File storage cannot replace an existing file on Windows.
- **Android Keystore:** it does not exist on the JVM, so `KeystoreKeys` is exercised on the device. `AesGcmCipher` and the token store are tested with a software key.
