package app.snackshot.datalocal

import app.snackshot.domain.meal.MealDraft
import app.snackshot.domain.meal.MealEdit
import app.snackshot.domain.meal.MealEntry
import app.snackshot.domain.meal.MealId
import app.snackshot.domain.meal.MealSource
import app.snackshot.domain.meal.MealType
import app.snackshot.domain.meal.NutritionEstimate
import app.snackshot.domain.nutrition.NutritionValues
import java.time.Instant
import java.time.ZoneOffset

/** A minimal valid JPEG header plus filler; the stores only check the SOI marker. */
internal val TEST_JPEG = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()) + ByteArray(64) { it.toByte() }

internal fun testMeal(id: String, imageRef: String? = null) = MealEntry(
    id = MealId(id),
    source = MealSource.PHOTO,
    name = "Grilled chicken salad",
    mealType = MealType.LUNCH,
    eatenAt = Instant.parse("2026-11-26T11:02:00Z"),
    zoneOffset = ZoneOffset.ofHours(1),
    grams = 350.0,
    values = NutritionValues(450.0, 38.0, 14.0, 26.0),
    imageRef = imageRef,
    confidence = 0.86,
    edit = MealEdit.NONE,
    createdAt = Instant.parse("2026-11-26T11:05:00Z"),
    updatedAt = Instant.parse("2026-11-26T11:05:00Z"),
)

internal fun testReviewDraft(imageRef: String?) = MealDraft.fromEstimate(
    source = MealSource.GALLERY,
    estimate = NutritionEstimate("Grilled chicken salad", 350.0, NutritionValues(450.0, null, 14.0, 26.0), confidence = 0.72),
    eatenAt = Instant.parse("2026-11-26T11:02:00Z"),
    zoneOffset = ZoneOffset.ofHours(1),
    mealType = MealType.LUNCH,
    imageRef = imageRef,
)
