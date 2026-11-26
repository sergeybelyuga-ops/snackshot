package app.snackshot.domain.meal

import app.snackshot.domain.nutrition.NutritionValues
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.UUID

@JvmInline
value class MealId(val value: String) {
    init {
        require(value.isNotBlank()) { "Meal id must not be blank" }
    }

    companion object {
        fun random() = MealId(UUID.randomUUID().toString())
    }
}

/** How the meal was logged (F-01, F-09, F-11). */
enum class MealSource {
    PHOTO,
    GALLERY,
    QUICK_ADD,
    ;

    val isAiRecognition: Boolean get() = this != QUICK_ADD
}

/** Matches the Health Connect meal types used by `NutritionRecord`. */
enum class MealType {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK,
    ;

    companion object {
        /** Suggested from the local time of the meal (S-09 prototype). The user can change it. */
        fun suggestFor(time: LocalTime): MealType = when (time.hour) {
            in 4..10 -> BREAKFAST
            in 11..15 -> LUNCH
            in 18..21 -> DINNER
            else -> SNACK
        }
    }
}

/**
 * What the user changed in an AI result before the first save. Feeds the Zero-Edit product
 * metric (WBS §6.2): only this classification is kept, never an edit history (doc 02 §4.4).
 */
enum class MealEdit {
    /** Saved as recognized. */
    NONE,

    /** Only the portion changed; values were rescaled from the estimate. */
    GRAMS_ONLY,

    /** The food name changed (whatever else changed too). */
    NAME_CHANGED,

    /** Nutrition values were entered or overridden, but the name stayed. */
    VALUES_CHANGED,
}

/**
 * A saved meal in the local log, the source of truth (doc 08). Health Connect sync metadata is
 * kept apart in [SyncState] (doc 02 §4.4).
 *
 * @property grams the portion; null only for a Quick add entry without a serving size.
 * @property values the whole portion; [NutritionValues.energyKcal] is always known.
 * @property confidence the provider confidence (0–1) for AI results; null for Quick add.
 * @property edit the first-save edit classification for AI results; null for Quick add.
 */
data class MealEntry(
    val id: MealId,
    val source: MealSource,
    val name: String,
    val mealType: MealType,
    val eatenAt: Instant,
    val zoneOffset: ZoneOffset,
    val grams: Double?,
    val values: NutritionValues,
    val imageRef: String?,
    val confidence: Double?,
    val edit: MealEdit?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(values.energyKcal != null) { "A saved meal always has energy" }
        require(grams != null || source == MealSource.QUICK_ADD) { "An AI result always has a portion" }
        require((confidence == null) == (source == MealSource.QUICK_ADD)) { "Confidence is set for AI results only" }
        require((edit == null) == (source == MealSource.QUICK_ADD)) { "Edit classification is set for AI results only" }
        require(!updatedAt.isBefore(createdAt)) { "updatedAt must not be before createdAt" }
    }

    val energyKcal: Double get() = values.energyKcal!!
}
