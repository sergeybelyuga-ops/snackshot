package app.snackshot.domain.nutrition

import kotlin.math.roundToInt

/** A daily calorie goal range in kcal, shown as the goal band on the dashboard. */
data class CalorieRange(val minKcal: Int, val maxKcal: Int) {
    init {
        require(minKcal in NutritionGoals.CALORIES_LIMIT && maxKcal in NutritionGoals.CALORIES_LIMIT) { "Calorie goal out of range: $this" }
        require(minKcal < maxKcal) { "Calorie goal minimum must be below the maximum: $this" }
    }

    /** The target shown in Settings: the middle of the range. */
    val targetKcal: Int get() = ((minKcal + maxKcal) / 2.0).roundToInt()
}

/**
 * Optional daily goals from Settings → Nutrition preferences (F-16). Each goal is set and cleared
 * on its own; null means not set. Stored only on the device and never synced to Health Connect.
 */
data class NutritionGoals(
    val calories: CalorieRange? = null,
    val proteinGrams: Int? = null,
    val carbsGrams: Int? = null,
    val fatGrams: Int? = null,
) {
    init {
        require(proteinGrams == null || proteinGrams in PROTEIN_LIMIT) { "Protein goal out of range: $proteinGrams" }
        require(carbsGrams == null || carbsGrams in CARBS_LIMIT) { "Carbs goal out of range: $carbsGrams" }
        require(fatGrams == null || fatGrams in FAT_LIMIT) { "Fat goal out of range: $fatGrams" }
    }

    val isEmpty: Boolean get() = calories == null && proteinGrams == null && carbsGrams == null && fatGrams == null

    companion object {
        val NONE = NutritionGoals()

        // Stepper limits from the S-16 prototype.
        val CALORIES_LIMIT = 800..6000
        val PROTEIN_LIMIT = 0..500
        val CARBS_LIMIT = 0..900
        val FAT_LIMIT = 0..400

        /** A macro goal is shown on the dashboard as a band of ±10 % around it (S-16 prototype). */
        fun macroBand(goalGrams: Int): IntRange = (goalGrams * 0.9).roundToInt()..(goalGrams * 1.1).roundToInt()
    }
}
