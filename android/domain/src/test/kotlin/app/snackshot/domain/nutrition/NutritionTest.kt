package app.snackshot.domain.nutrition

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class NutritionTest {
    private val salad = NutritionValues(energyKcal = 450.0, proteinGrams = 38.0, carbsGrams = 14.0, fatGrams = 26.0)

    @Test
    fun `values must be non-negative and finite`() {
        assertThrows<IllegalArgumentException> { NutritionValues(energyKcal = -1.0) }
        assertThrows<IllegalArgumentException> { NutritionValues(fatGrams = Double.NaN) }
        assertThrows<IllegalArgumentException> { NutritionValues(proteinGrams = Double.POSITIVE_INFINITY) }
        NutritionValues(energyKcal = 0.0)
    }

    @Test
    fun `scaling keeps unknown values unknown`() {
        val partial = NutritionValues(energyKcal = 200.0, carbsGrams = 10.0)
        assertEquals(NutritionValues(energyKcal = 100.0, carbsGrams = 5.0), partial.scaled(0.5))
        assertEquals(setOf(NutrientField.PROTEIN, NutrientField.FAT), partial.missingFields())
        assertFalse(partial.isComplete)
        assertTrue(salad.isComplete)
    }

    @Test
    fun `orElse fills only unknown fields`() {
        val typed = NutritionValues(proteinGrams = 40.0)
        assertEquals(salad.copy(proteinGrams = 40.0), typed.orElse(salad))
    }

    @Test
    fun `totals treat unknown values as zero`() {
        val totals = listOf(salad, NutritionValues(energyKcal = 230.0, proteinGrams = 20.0)).total()
        assertEquals(NutritionTotals(680.0, 58.0, 14.0, 26.0), totals)
        assertEquals(NutritionTotals.ZERO, emptyList<NutritionValues>().total())
    }

    @Test
    fun `Atwater uses 4 4 9 and 7 kcal per gram`() {
        assertEquals(4.0 * 20 + 4.0 * 24 + 9.0 * 8, Atwater.energyKcal(proteinGrams = 20.0, carbsGrams = 24.0, fatGrams = 8.0))
        assertEquals(7.0 * 14, Atwater.energyKcal(0.0, 0.0, 0.0, alcoholGrams = 14.0))
        assertThrows<IllegalArgumentException> { Atwater.energyKcal(-1.0, 0.0, 0.0) }
    }

    @Test
    fun `energy from macros needs at least one macro`() {
        assertNull(Atwater.energyFromMacros(NutritionValues(energyKcal = 100.0)))
        assertEquals(36.0, Atwater.energyFromMacros(NutritionValues(fatGrams = 4.0)))
    }

    @Test
    fun `Atwater comparison against the provider energy`() {
        val comparison = AtwaterComparison.of(salad)!!
        assertEquals(4.0 * 38 + 4.0 * 14 + 9.0 * 26, comparison.atwaterKcal)
        assertEquals(450.0 - 442.0, comparison.differenceKcal, 1e-9)
        assertEquals(8.0 / 450.0 * 100.0, comparison.differencePercent!!, 1e-9)
    }

    @Test
    fun `no Atwater comparison for an incomplete result or zero energy percent`() {
        assertNull(AtwaterComparison.of(NutritionValues(energyKcal = 100.0, proteinGrams = 5.0)))
        assertNull(AtwaterComparison.of(NutritionValues(0.0, 0.0, 0.0, 0.0))!!.differencePercent)
    }

    @Test
    fun `goals are optional one by one and validated`() {
        assertTrue(NutritionGoals.NONE.isEmpty)
        val goals = NutritionGoals(proteinGrams = 150)
        assertFalse(goals.isEmpty)
        assertNull(goals.calories)
        assertThrows<IllegalArgumentException> { NutritionGoals(fatGrams = 401) }
        assertThrows<IllegalArgumentException> { NutritionGoals(carbsGrams = -1) }
    }

    @Test
    fun `calorie range has min below max and a middle target`() {
        val range = CalorieRange(2070, 2530)
        assertEquals(2300, range.targetKcal)
        assertThrows<IllegalArgumentException> { CalorieRange(2500, 2500) }
        assertThrows<IllegalArgumentException> { CalorieRange(700, 2000) }
        assertThrows<IllegalArgumentException> { CalorieRange(2000, 6001) }
    }

    @Test
    fun `macro goal band is plus or minus 10 percent`() {
        assertEquals(198..242, NutritionGoals.macroBand(220))
        assertEquals(0..0, NutritionGoals.macroBand(0))
    }
}
