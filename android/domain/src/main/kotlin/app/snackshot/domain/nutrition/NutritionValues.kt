package app.snackshot.domain.nutrition

/**
 * Calories and macros for one whole serving, in Health Connect units: energy in kilocalories,
 * nutrients in grams (doc 02 §4.3). A null field is unknown (for example, partial recognition
 * or a Quick add field left blank), which is different from zero.
 */
data class NutritionValues(
    val energyKcal: Double? = null,
    val proteinGrams: Double? = null,
    val carbsGrams: Double? = null,
    val fatGrams: Double? = null,
) {
    init {
        listOf(energyKcal, proteinGrams, carbsGrams, fatGrams).forEach { value ->
            require(value == null || (value.isFinite() && value >= 0.0)) { "Nutrition values must be finite and non-negative: $this" }
        }
    }

    /** All four values are known. AI results must be complete before they can be saved. */
    val isComplete: Boolean get() = energyKcal != null && proteinGrams != null && carbsGrams != null && fatGrams != null

    /** No value is known. Quick add needs at least one (F-11). */
    val isEmpty: Boolean get() = energyKcal == null && proteinGrams == null && carbsGrams == null && fatGrams == null

    /** Every known value multiplied by [factor]; unknown values stay unknown. */
    fun scaled(factor: Double): NutritionValues {
        require(factor.isFinite() && factor >= 0.0) { "Scale factor must be finite and non-negative: $factor" }
        return NutritionValues(energyKcal?.times(factor), proteinGrams?.times(factor), carbsGrams?.times(factor), fatGrams?.times(factor))
    }

    /** Field by field: this value when known, otherwise the value from [fallback]. */
    fun orElse(fallback: NutritionValues) = NutritionValues(
        energyKcal = energyKcal ?: fallback.energyKcal,
        proteinGrams = proteinGrams ?: fallback.proteinGrams,
        carbsGrams = carbsGrams ?: fallback.carbsGrams,
        fatGrams = fatGrams ?: fallback.fatGrams,
    )

    /** The fields that are unknown. */
    fun missingFields(): Set<NutrientField> = buildSet {
        if (energyKcal == null) add(NutrientField.ENERGY)
        if (proteinGrams == null) add(NutrientField.PROTEIN)
        if (carbsGrams == null) add(NutrientField.CARBS)
        if (fatGrams == null) add(NutrientField.FAT)
    }

    companion object {
        val EMPTY = NutritionValues()
    }
}

enum class NutrientField { ENERGY, PROTEIN, CARBS, FAT }

/** Totals of several servings (Home, Dashboard). An unknown value counts as zero. */
data class NutritionTotals(val energyKcal: Double, val proteinGrams: Double, val carbsGrams: Double, val fatGrams: Double) {
    operator fun plus(values: NutritionValues) = NutritionTotals(
        energyKcal = energyKcal + (values.energyKcal ?: 0.0),
        proteinGrams = proteinGrams + (values.proteinGrams ?: 0.0),
        carbsGrams = carbsGrams + (values.carbsGrams ?: 0.0),
        fatGrams = fatGrams + (values.fatGrams ?: 0.0),
    )

    companion object {
        val ZERO = NutritionTotals(0.0, 0.0, 0.0, 0.0)
    }
}

fun Iterable<NutritionValues>.total(): NutritionTotals = fold(NutritionTotals.ZERO) { sum, values -> sum + values }
