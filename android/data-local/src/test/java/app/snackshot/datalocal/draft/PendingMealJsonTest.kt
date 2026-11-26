package app.snackshot.datalocal.draft

import app.snackshot.datalocal.testMeal
import app.snackshot.datalocal.testReviewDraft
import app.snackshot.domain.meal.MealDraft
import app.snackshot.domain.meal.MealSource
import app.snackshot.domain.meal.MealType
import app.snackshot.domain.meal.PendingMeal
import app.snackshot.domain.nutrition.NutritionValues
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PendingMealJsonTest {
    private fun roundTrip(meal: PendingMeal) = assertEquals(meal, PendingMealJson.decode(PendingMealJson.encode(meal)))

    @Test
    fun `photo waiting for recognition round-trips`() = roundTrip(
        PendingMeal.AwaitingRecognition(
            source = MealSource.PHOTO,
            imageRef = "meal-images/00000000-0000-0000-0000-000000000001.jpg",
            capturedAt = Instant.parse("2026-11-26T11:02:00Z"),
            zoneOffset = ZoneOffset.ofHours(1),
            failedAttempts = 1,
        ),
    )

    @Test
    fun `review with every correction round-trips`() {
        val draft = testReviewDraft("meal-images/00000000-0000-0000-0000-000000000002.jpg")
            .withName("Caesar salad")
            .withGrams(275.5)
            .withMealType(MealType.DINNER)
            .withOverrides(NutritionValues(proteinGrams = 31.0))
        roundTrip(PendingMeal.Reviewing(draft))
        // The edit classification and recalculation come back with it.
        val restored = (PendingMealJson.decode(PendingMealJson.encode(PendingMeal.Reviewing(draft))) as PendingMeal.Reviewing).draft
        assertEquals(draft.edit, restored.edit)
        assertEquals(draft.values, restored.values)
    }

    @Test
    fun `quick add and editing a saved meal round-trip`() {
        roundTrip(
            PendingMeal.Reviewing(
                MealDraft.quickAdd(Instant.parse("2026-11-26T16:00:00Z"), ZoneOffset.UTC, MealType.SNACK)
                    .withOverrides(NutritionValues(energyKcal = 230.0)),
            ),
        )
        roundTrip(
            PendingMeal.Reviewing(
                MealDraft.fromEntry(testMeal("saved", "meal-images/00000000-0000-0000-0000-000000000003.jpg")).withGrams(100.0),
            ),
        )
    }

    @Test
    fun `unreadable or other-schema drafts are dropped`() {
        assertNull(PendingMealJson.decode("not json"))
        assertNull(PendingMealJson.decode("""{"schema":99,"meal":{"kind":"reviewing"}}"""))
        assertNull(PendingMealJson.decode("""{"schema":1,"meal":{"kind":"something_new"}}"""))
        // Valid JSON that breaks a domain rule (negative kcal) is dropped too, not a crash.
        val broken = PendingMealJson.encode(
            PendingMeal.Reviewing(testReviewDraft(null)),
        ).replace("\"energyKcal\":450.0", "\"energyKcal\":-1.0")
        assertNull(PendingMealJson.decode(broken))
    }
}
