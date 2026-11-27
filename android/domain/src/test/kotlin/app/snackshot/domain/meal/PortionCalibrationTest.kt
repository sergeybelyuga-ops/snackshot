package app.snackshot.domain.meal

import app.snackshot.domain.nutrition.NutritionValues
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PortionCalibrationTest {
    private fun recognition(name: String = "apple", grams: Double? = 250.0, confidence: Double = 0.9) =
        Recognition(name, confidence, grams, NutritionValues(130.0, 0.6, 34.5, 0.4), RecognitionRef(1, 1))

    private fun calibrator(history: Double? = null) = HistoryPortionCalibrator(lastGrams = { history })

    private fun portion(c: PortionCalibrator, r: Recognition) = runBlocking { c.portionFor(r) }

    @Test
    fun `the user's last grams for the food come first`() {
        assertEquals(CalibratedPortion(160.0, PortionSource.LAST_LOGGED), portion(calibrator(history = 160.0), recognition()))
    }

    @Test
    fun `without history a confident LogMeal portion is kept`() {
        assertEquals(CalibratedPortion(250.0, PortionSource.PROVIDER), portion(calibrator(), recognition()))
    }

    @Test
    fun `an uncertain LogMeal portion gives way to the reference weight`() {
        assertEquals(CalibratedPortion(182.0, PortionSource.REFERENCE), portion(calibrator(), recognition(confidence = 0.73)))
    }

    @Test
    fun `missing LogMeal grams use the reference weight`() {
        assertEquals(CalibratedPortion(118.0, PortionSource.REFERENCE), portion(calibrator(), recognition("Banana", grams = null)))
    }

    @Test
    fun `an uncertain portion of an unlisted food is still LogMeal's`() {
        val r = recognition("chicken curry", grams = 320.0, confidence = 0.5)
        assertEquals(CalibratedPortion(320.0, PortionSource.PROVIDER), portion(calibrator(), r))
    }

    @Test
    fun `with nothing known the portion is 100 g`() {
        val r = recognition("chicken curry", grams = null)
        assertEquals(CalibratedPortion(100.0, PortionSource.DEFAULT), portion(calibrator(), r))
    }

    @Test
    fun `out-of-range grams are skipped`() {
        assertEquals(PortionSource.PROVIDER, portion(calibrator(history = 0.2), recognition()).source)
        assertEquals(PortionSource.REFERENCE, portion(calibrator(), recognition(grams = 9_000.0)).source)
    }

    @Test
    fun `a failed history lookup falls through`() {
        val failing = HistoryPortionCalibrator(lastGrams = { error("database closed") })
        assertEquals(PortionSource.PROVIDER, portion(failing, recognition()).source)
    }

    @Test
    fun `reference names match whole names only, ignoring case and spacing`() {
        assertEquals(50.0, ReferencePortions.gramsFor("  Hard-boiled   egg "))
        assertNull(ReferencePortions.gramsFor("apples"))
        assertNull(ReferencePortions.gramsFor("apple pie"))
        assertNull(ReferencePortions.gramsFor(""))
    }

    @Test
    fun `the draft's values follow the calibrated portion`() {
        val draft = MealDraft.fromRecognition(
            source = MealSource.PHOTO,
            recognition = recognition(),
            portion = CalibratedPortion(125.0, PortionSource.LAST_LOGGED),
            eatenAt = java.time.Instant.parse("2026-11-26T11:02:00Z"),
            zoneOffset = java.time.ZoneOffset.ofHours(1),
            mealType = MealType.SNACK,
            imageRef = "meal-images/a.jpg",
        )
        assertEquals("Apple", draft.name)
        assertEquals(125.0, draft.grams)
        assertEquals(65.0, draft.values.energyKcal!!, 0.001)
    }
}
