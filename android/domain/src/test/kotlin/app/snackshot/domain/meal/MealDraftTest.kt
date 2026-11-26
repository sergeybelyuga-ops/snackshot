package app.snackshot.domain.meal

import app.snackshot.domain.nutrition.NutrientField
import app.snackshot.domain.nutrition.NutritionValues
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import kotlin.math.roundToInt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MealDraftTest {
    private val eatenAt = Instant.parse("2026-11-26T11:02:00Z")
    private val saveTime = Instant.parse("2026-11-26T11:05:00Z")
    private val zone = ZoneOffset.ofHours(1)
    private val id = MealId("meal-1")

    private val salad = NutritionEstimate(
        foodName = "Grilled chicken salad",
        grams = 350.0,
        values = NutritionValues(energyKcal = 450.0, proteinGrams = 38.0, carbsGrams = 14.0, fatGrams = 26.0),
        confidence = 0.86,
    )

    private fun review(estimate: NutritionEstimate = salad) =
        MealDraft.fromEstimate(MealSource.PHOTO, estimate, eatenAt, zone, MealType.LUNCH, imageRef = "images/meal-1.jpg")

    private fun quickAdd() = MealDraft.quickAdd(eatenAt, zone, MealType.SNACK)

    @Test
    fun `untouched AI result saves as recognized with zero edits`() {
        val entry = review().toEntry(saveTime) { id }
        assertEquals(id, entry.id)
        assertEquals("Grilled chicken salad", entry.name)
        assertEquals(350.0, entry.grams)
        assertEquals(salad.values, entry.values)
        assertEquals(0.86, entry.confidence)
        assertEquals(MealEdit.NONE, entry.edit)
        assertEquals("images/meal-1.jpg", entry.imageRef)
        assertEquals(saveTime, entry.createdAt)
    }

    @Test
    fun `grams edit scales every value proportionally`() {
        val draft = review().withGrams(175.0)
        assertEquals(NutritionValues(225.0, 19.0, 7.0, 13.0), draft.values)
        assertEquals(MealEdit.GRAMS_ONLY, draft.edit)
    }

    @Test
    fun `local scaling matches LogMeal's own recalculation from the spike`() {
        // Spike 1.1: LogMeal recalculated apple 182 g / 95 kcal to 116 g / 60 kcal.
        val apple = NutritionEstimate("apple", 182.0, NutritionValues(95.0, 0.5, 25.0, 0.3), confidence = 0.73)
        val kcal = review(apple).withGrams(116.0).values.energyKcal!!
        assertTrue(kcal.roundToInt() in 60..61, "got $kcal")
    }

    @Test
    fun `values the user typed stay as entered when grams change`() {
        val partial = salad.copy(values = NutritionValues(energyKcal = 450.0, carbsGrams = 14.0))
        val draft = review(partial).withOverrides(NutritionValues(proteinGrams = 30.0, fatGrams = 20.0)).withGrams(700.0)
        assertEquals(NutritionValues(energyKcal = 900.0, proteinGrams = 30.0, carbsGrams = 28.0, fatGrams = 20.0), draft.values)
        assertEquals(MealEdit.VALUES_CHANGED, draft.edit)
    }

    @Test
    fun `partial recognition saves only after missing values are supplied`() {
        val partial = review(salad.copy(values = NutritionValues(energyKcal = 450.0, carbsGrams = 14.0)))
        assertEquals(listOf(DraftProblem.MissingValues(setOf(NutrientField.PROTEIN, NutrientField.FAT))), partial.problems())
        assertThrows<IllegalStateException> { partial.toEntry(saveTime) }
        assertTrue(partial.withOverrides(NutritionValues(proteinGrams = 38.0, fatGrams = 26.0)).canSave)
    }

    @Test
    fun `a name change is classified as such whatever else changed`() {
        val draft = review().withName("Chicken Caesar salad").withGrams(300.0)
        assertEquals(MealEdit.NAME_CHANGED, draft.edit)
        assertEquals(MealEdit.NONE, review().withName("  Grilled chicken salad ").edit)
    }

    @Test
    fun `meal type is not a recognition edit`() {
        assertEquals(MealEdit.NONE, review().withMealType(MealType.DINNER).edit)
    }

    @Test
    fun `reset returns to the estimate`() {
        val draft = review().withName("Other").withGrams(10.0).withOverrides(NutritionValues(fatGrams = 1.0)).reset()
        assertEquals(review(), draft)
        assertEquals(MealEdit.NONE, draft.edit)
    }

    @Test
    fun `AI portion must be within 1 to 5000 g`() {
        assertEquals(listOf(DraftProblem.PortionOutOfRange), review().withGrams(0.5).problems())
        assertEquals(listOf(DraftProblem.PortionOutOfRange), review().withGrams(null).problems())
        assertTrue(review().withGrams(5000.0).canSave)
        assertFalse(review().withGrams(5001.0).canSave)
    }

    @Test
    fun `AI result needs a name`() {
        assertEquals(listOf(DraftProblem.NameMissing), review().withName("  ").problems())
        assertEquals(listOf(DraftProblem.NameTooLong), review().withName("x".repeat(81)).problems())
    }

    @Test
    fun `values over the limit are reported`() {
        val draft = review().withOverrides(NutritionValues(energyKcal = 10_001.0, fatGrams = 1_001.0))
        assertEquals(listOf(DraftProblem.ValuesOverLimit(setOf(NutrientField.ENERGY, NutrientField.FAT))), draft.problems())
    }

    @Test
    fun `quick add needs at least one value but no name or grams`() {
        assertEquals(listOf(DraftProblem.NoNutritionValues), quickAdd().problems())
        val entry = quickAdd().withOverrides(NutritionValues(energyKcal = 230.0)).toEntry(saveTime) { id }
        assertEquals(MealSource.QUICK_ADD, entry.source)
        assertEquals("", entry.name)
        assertNull(entry.grams)
        assertNull(entry.confidence)
        assertNull(entry.edit)
        assertEquals(230.0, entry.energyKcal)
    }

    @Test
    fun `quick add without calories saves the energy of the macros`() {
        val entry = quickAdd().withOverrides(NutritionValues(proteinGrams = 20.0, carbsGrams = 24.0, fatGrams = 8.0)).toEntry(saveTime)
        assertEquals(248.0, entry.energyKcal)
        assertNull(quickAdd().withOverrides(NutritionValues(proteinGrams = 20.0)).edit)
    }

    @Test
    fun `quick add keeps entered calories even when macros disagree`() {
        val entry = quickAdd().withOverrides(NutritionValues(energyKcal = 100.0, fatGrams = 20.0)).toEntry(saveTime)
        assertEquals(100.0, entry.energyKcal)
    }

    @Test
    fun `quick add serving size is optional but validated`() {
        val draft = quickAdd().withOverrides(NutritionValues(energyKcal = 230.0))
        assertEquals(listOf(DraftProblem.PortionOutOfRange), draft.withGrams(0.0).problems())
        assertTrue(draft.withGrams(60.0).canSave)
        // A new Quick add has no estimate, so a serving size does not change the entered values.
        assertEquals(230.0, draft.withGrams(120.0).values.energyKcal)
    }

    @Test
    fun `quick add keeps the photo of a failed recognition`() {
        val draft = MealDraft.quickAdd(eatenAt, zone, MealType.LUNCH, imageRef = "images/failed.jpg")
        assertEquals("images/failed.jpg", draft.withOverrides(NutritionValues(energyKcal = 300.0)).toEntry(saveTime).imageRef)
    }

    @Test
    fun `editing a saved meal rescales it and keeps its id and first-save classification`() {
        val saved = review().withGrams(175.0).toEntry(saveTime) { id }
        val later = Instant.parse("2026-11-26T19:00:00Z")
        val edited = MealDraft.fromEntry(saved).withGrams(350.0).withName("Caesar salad").toEntry(later)
        assertEquals(id, edited.id)
        assertEquals(salad.values, edited.values)
        assertEquals("Caesar salad", edited.name)
        assertEquals(MealEdit.GRAMS_ONLY, edited.edit)
        assertEquals(saveTime, edited.createdAt)
        assertEquals(later, edited.updatedAt)
    }

    @Test
    fun `editing a quick add entry without a serving keeps its values`() {
        val saved = quickAdd().withOverrides(NutritionValues(energyKcal = 230.0)).toEntry(saveTime) { id }
        val draft = MealDraft.fromEntry(saved)
        assertEquals(saved.values, draft.values)
        assertEquals(saved, draft.toEntry(saveTime))
    }

    @Test
    fun `quick add entry with a serving rescales when edited`() {
        val saved = quickAdd().withOverrides(NutritionValues(energyKcal = 230.0, proteinGrams = 20.0)).withGrams(60.0).toEntry(saveTime)
        assertEquals(NutritionValues(energyKcal = 460.0, proteinGrams = 40.0), MealDraft.fromEntry(saved).withGrams(120.0).values)
    }

    @Test
    fun `confidence threshold is 80 percent`() {
        assertTrue(salad.copy(confidence = 0.8).isHighConfidence)
        assertFalse(salad.copy(confidence = 0.79).isHighConfidence)
        assertThrows<IllegalArgumentException> { salad.copy(confidence = 1.2) }
    }

    @Test
    fun `meal type is suggested from the time of day`() {
        assertEquals(MealType.BREAKFAST, MealType.suggestFor(LocalTime.of(4, 0)))
        assertEquals(MealType.LUNCH, MealType.suggestFor(LocalTime.of(11, 17)))
        assertEquals(MealType.SNACK, MealType.suggestFor(LocalTime.of(16, 30)))
        assertEquals(MealType.DINNER, MealType.suggestFor(LocalTime.of(21, 59)))
        assertEquals(MealType.SNACK, MealType.suggestFor(LocalTime.of(23, 0)))
    }

    @Test
    fun `quick add cannot be built as an AI result`() {
        assertThrows<IllegalArgumentException> { MealDraft.fromEstimate(MealSource.QUICK_ADD, salad, eatenAt, zone, MealType.LUNCH, null) }
    }
}
