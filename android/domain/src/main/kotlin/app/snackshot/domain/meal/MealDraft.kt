package app.snackshot.domain.meal

import app.snackshot.domain.nutrition.Atwater
import app.snackshot.domain.nutrition.NutrientField
import app.snackshot.domain.nutrition.NutritionValues
import java.time.Instant
import java.time.ZoneOffset

/** What the provider returned for one photo (F-01): one dish, drink, or raw fruit or vegetable. */
data class NutritionEstimate(val foodName: String, val grams: Double, val values: NutritionValues, val confidence: Double) {
    init {
        require(grams.isFinite() && grams > 0.0) { "Estimated grams must be positive: $grams" }
        require(confidence in 0.0..1.0) { "Confidence must be within 0..1: $confidence" }
    }

    /** At or above the 80 % threshold (doc 02 §5.x); below it the review is emphasized. */
    val isHighConfidence: Boolean get() = confidence >= HIGH_CONFIDENCE

    companion object {
        const val HIGH_CONFIDENCE = 0.8
    }
}

/** The portion that values scale from: the provider estimate, or the saved meal being edited. */
data class Portion(val grams: Double, val values: NutritionValues) {
    init {
        require(grams.isFinite() && grams > 0.0) { "Portion grams must be positive: $grams" }
    }
}

/** Why a draft cannot be saved yet. */
sealed interface DraftProblem {
    data object NameMissing : DraftProblem

    data object NameTooLong : DraftProblem

    /** The portion is missing (AI results) or outside [MealDraft.MIN_GRAMS]..[MealDraft.MAX_GRAMS]. */
    data object PortionOutOfRange : DraftProblem

    /** Quick add with no nutrition value at all (F-11). */
    data object NoNutritionValues : DraftProblem

    /** An AI result still lacks these values; the user must supply them (doc 10 §4.7). */
    data class MissingValues(val fields: Set<NutrientField>) : DraftProblem

    data class ValuesOverLimit(val fields: Set<NutrientField>) : DraftProblem
}

/**
 * A meal being reviewed (S-15, S-07) or entered (S-09) before it is saved.
 *
 * Values come from [basis] scaled proportionally to [grams] (doc 02 §6.1–6.4; never Atwater),
 * except fields in [overrides], which the user entered and which stay as entered when the
 * grams change (doc 10 §4.7).
 */
data class MealDraft(
    val source: MealSource,
    val eatenAt: Instant,
    val zoneOffset: ZoneOffset,
    val mealType: MealType,
    val name: String,
    val grams: Double?,
    val basis: Portion?,
    val overrides: NutritionValues = NutritionValues.EMPTY,
    val imageRef: String? = null,
    val confidence: Double? = null,
    /** The name the review started from: the recognized name, or the saved name when editing. */
    val originalName: String = name,
    /** The saved meal this draft edits, or null for a new meal. */
    val editing: MealEntry? = null,
) {
    init {
        require(basis != null || source == MealSource.QUICK_ADD) { "An AI draft needs the estimate as its basis" }
        require(editing == null || editing.source == source) { "Editing keeps the meal source" }
    }

    /** Current values for the whole portion, recalculated instantly on every change (F-03). */
    val values: NutritionValues
        get() {
            val scaled = basis?.let { b ->
                val g = grams
                if (g != null && g.isFinite() && g > 0.0) b.values.scaled(g / b.grams) else b.values
            } ?: NutritionValues.EMPTY
            return overrides.orElse(scaled)
        }

    /** The Zero-Edit classification this draft would be saved with; null for Quick add. */
    val edit: MealEdit?
        get() {
            if (!source.isAiRecognition) return null
            val nameChanged = name.trim() != originalName.trim()
            val gramsChanged = grams != basis?.grams
            return when {
                nameChanged -> MealEdit.NAME_CHANGED
                !overrides.isEmpty -> MealEdit.VALUES_CHANGED
                gramsChanged -> MealEdit.GRAMS_ONLY
                else -> MealEdit.NONE
            }
        }

    fun withName(name: String) = copy(name = name)

    fun withGrams(grams: Double?) = copy(grams = grams)

    fun withMealType(mealType: MealType) = copy(mealType = mealType)

    /** Sets the values the user typed. Null fields fall back to the scaled basis. */
    fun withOverrides(overrides: NutritionValues) = copy(overrides = overrides)

    /** S-07 Reset: back to the recognized name, grams, and values. */
    fun reset() = copy(name = originalName, grams = basis?.grams, overrides = NutritionValues.EMPTY)

    fun problems(): List<DraftProblem> = buildList {
        val trimmed = name.trim()
        if (source.isAiRecognition && trimmed.isEmpty()) add(DraftProblem.NameMissing)
        if (trimmed.length > MAX_NAME_LENGTH) add(DraftProblem.NameTooLong)

        val g = grams
        val portionValid = g != null && g.isFinite() && g in MIN_GRAMS..MAX_GRAMS
        if (!portionValid && (source.isAiRecognition || g != null)) add(DraftProblem.PortionOutOfRange)

        val current = values
        if (source.isAiRecognition) {
            current.missingFields().takeIf { it.isNotEmpty() }?.let { add(DraftProblem.MissingValues(it)) }
        } else if (current.isEmpty) {
            add(DraftProblem.NoNutritionValues)
        }
        overLimit(current).takeIf { it.isNotEmpty() }?.let { add(DraftProblem.ValuesOverLimit(it)) }
    }

    val canSave: Boolean get() = problems().isEmpty()

    /**
     * The meal to save: a new entry, or [editing] updated. Quick add without calories saves the
     * energy of the entered macros, as S-09 tells the user. An edit keeps the first-save
     * Zero-Edit classification, because the metric counts first saves only.
     */
    fun toEntry(now: Instant, newId: () -> MealId = MealId::random): MealEntry {
        val problems = problems()
        check(problems.isEmpty()) { "Draft cannot be saved: $problems" }
        val current = values.let { v -> if (v.energyKcal == null) v.copy(energyKcal = Atwater.energyFromMacros(v)) else v }
        return MealEntry(
            id = editing?.id ?: newId(),
            source = source,
            name = name.trim(),
            mealType = mealType,
            eatenAt = eatenAt,
            zoneOffset = zoneOffset,
            grams = grams,
            values = current,
            imageRef = imageRef,
            confidence = confidence,
            edit = editing?.edit ?: edit,
            createdAt = editing?.createdAt ?: now,
            updatedAt = now,
        )
    }

    companion object {
        const val MIN_GRAMS = 1.0
        const val MAX_GRAMS = 5000.0
        const val MAX_ENERGY_KCAL = 10_000.0
        const val MAX_NUTRIENT_GRAMS = 1000.0
        const val MAX_NAME_LENGTH = 80

        /** S-15 review of a fresh AI result from the camera or the gallery. */
        fun fromEstimate(
            source: MealSource,
            estimate: NutritionEstimate,
            eatenAt: Instant,
            zoneOffset: ZoneOffset,
            mealType: MealType,
            imageRef: String?,
        ): MealDraft {
            require(source.isAiRecognition) { "Estimates come from photo or gallery recognition" }
            return MealDraft(
                source = source,
                eatenAt = eatenAt,
                zoneOffset = zoneOffset,
                mealType = mealType,
                name = estimate.foodName,
                grams = estimate.grams,
                basis = Portion(estimate.grams, estimate.values),
                imageRef = imageRef,
                confidence = estimate.confidence,
            )
        }

        /** S-09 Quick add. [imageRef] keeps the photo of a failed recognition (doc 10 §4.8). */
        fun quickAdd(eatenAt: Instant, zoneOffset: ZoneOffset, mealType: MealType, imageRef: String? = null) = MealDraft(
            source = MealSource.QUICK_ADD,
            eatenAt = eatenAt,
            zoneOffset = zoneOffset,
            mealType = mealType,
            name = "",
            grams = null,
            basis = null,
            imageRef = imageRef,
        )

        /**
         * Editing a saved meal from history (F-23). The saved values become the basis, so a grams
         * change rescales them. A Quick add entry without a serving size has no basis: its values
         * are the user's own and do not scale.
         */
        fun fromEntry(entry: MealEntry): MealDraft {
            val grams = entry.grams
            val scalable = grams != null && grams > 0.0
            return MealDraft(
                source = entry.source,
                eatenAt = entry.eatenAt,
                zoneOffset = entry.zoneOffset,
                mealType = entry.mealType,
                name = entry.name,
                grams = grams,
                basis = if (scalable) Portion(grams!!, entry.values) else null,
                overrides = if (scalable) NutritionValues.EMPTY else entry.values,
                imageRef = entry.imageRef,
                confidence = entry.confidence,
                editing = entry,
            )
        }

        private fun overLimit(values: NutritionValues): Set<NutrientField> = buildSet {
            if ((values.energyKcal ?: 0.0) > MAX_ENERGY_KCAL) add(NutrientField.ENERGY)
            if ((values.proteinGrams ?: 0.0) > MAX_NUTRIENT_GRAMS) add(NutrientField.PROTEIN)
            if ((values.carbsGrams ?: 0.0) > MAX_NUTRIENT_GRAMS) add(NutrientField.CARBS)
            if ((values.fatGrams ?: 0.0) > MAX_NUTRIENT_GRAMS) add(NutrientField.FAT)
        }
    }
}
