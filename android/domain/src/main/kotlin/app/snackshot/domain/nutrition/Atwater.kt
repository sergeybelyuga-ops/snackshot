package app.snackshot.domain.nutrition

/**
 * General Atwater factors (doc 02 §4.3, common-data/food-energy.md). Used by Quick add's
 * "Calculate kcal from macros" and by the test-build comparison. Never used to recalculate a
 * provider result after a grams edit: that scales the provider values (doc 02 §6.1–6.4).
 */
object Atwater {
    const val PROTEIN_KCAL_PER_GRAM = 4.0
    const val CARBS_KCAL_PER_GRAM = 4.0
    const val FAT_KCAL_PER_GRAM = 9.0
    const val ALCOHOL_KCAL_PER_GRAM = 7.0

    fun energyKcal(proteinGrams: Double, carbsGrams: Double, fatGrams: Double, alcoholGrams: Double = 0.0): Double {
        listOf(proteinGrams, carbsGrams, fatGrams, alcoholGrams).forEach {
            require(it.isFinite() && it >= 0.0) { "Grams must be finite and non-negative: $it" }
        }
        return proteinGrams * PROTEIN_KCAL_PER_GRAM + carbsGrams * CARBS_KCAL_PER_GRAM + fatGrams * FAT_KCAL_PER_GRAM +
            alcoholGrams * ALCOHOL_KCAL_PER_GRAM
    }

    /** Energy from the known macros, or null when no macro is known. Unknown macros count as zero. */
    fun energyFromMacros(values: NutritionValues): Double? {
        if (values.proteinGrams == null && values.carbsGrams == null && values.fatGrams == null) return null
        return energyKcal(values.proteinGrams ?: 0.0, values.carbsGrams ?: 0.0, values.fatGrams ?: 0.0)
    }
}

/**
 * Test builds only (local and dev, `EnvironmentConfig.showAtwaterComparison`): the provider's
 * energy next to the Atwater energy of the provider's own macros, recorded for evaluation
 * (doc 03 §5). The provider's energy stays the application value.
 */
data class AtwaterComparison(val providerKcal: Double, val atwaterKcal: Double) {
    val differenceKcal: Double get() = providerKcal - atwaterKcal

    /** Difference relative to the provider value, or null when the provider value is 0. */
    val differencePercent: Double? get() = if (providerKcal == 0.0) null else differenceKcal / providerKcal * 100.0

    companion object {
        /** Null when the provider result is incomplete: there is nothing to compare. */
        fun of(provider: NutritionValues, alcoholGrams: Double = 0.0): AtwaterComparison? {
            val energy = provider.energyKcal ?: return null
            val protein = provider.proteinGrams ?: return null
            val carbs = provider.carbsGrams ?: return null
            val fat = provider.fatGrams ?: return null
            return AtwaterComparison(providerKcal = energy, atwaterKcal = Atwater.energyKcal(protein, carbs, fat, alcoholGrams))
        }
    }
}
