package app.snackshot.ui.review

import app.snackshot.domain.meal.MealDraft
import app.snackshot.domain.meal.MealEdit
import app.snackshot.domain.meal.MealSource
import app.snackshot.domain.meal.MealType
import app.snackshot.domain.meal.NutritionEstimate
import app.snackshot.domain.meal.PendingMeal
import app.snackshot.domain.nutrition.NutrientField
import app.snackshot.domain.nutrition.NutritionValues
import app.snackshot.ui.FakeImages
import app.snackshot.ui.FakeMealRepository
import app.snackshot.ui.FakePendingMeals
import app.snackshot.ui.MutableClock
import app.snackshot.ui.TEST_NOW
import app.snackshot.ui.TEST_ZONE
import app.snackshot.ui.meal.MealSaver
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class MealReviewViewModelTest {
    @get:Rule val tmp = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()
    private val clock = MutableClock()
    private val meals = FakeMealRepository()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    private fun draft(values: NutritionValues = NutritionValues(400.0, 30.0, 40.0, 10.0), confidence: Double = 0.86) =
        MealDraft.fromEstimate(
            source = MealSource.PHOTO,
            estimate = NutritionEstimate("Grilled chicken salad", 200.0, values, confidence),
            eatenAt = TEST_NOW,
            zoneOffset = TEST_ZONE.rules.getOffset(TEST_NOW),
            mealType = MealType.LUNCH,
            imageRef = "meal-images/x.jpg",
        )

    private fun TestScope.viewModel(pending: FakePendingMeals): MealReviewViewModel {
        val vm = MealReviewViewModel(pending, FakeImages(tmp.root), MealSaver(meals, clock), clock) { TEST_ZONE }
        advanceUntilIdle()
        return vm
    }

    @Test
    fun `the portion rescales the values and every change is kept in the draft`() = runTest(dispatcher) {
        val pending = FakePendingMeals(PendingMeal.Reviewing(draft()))
        val vm = viewModel(pending)

        vm.onGrams("300")
        advanceUntilIdle()

        assertEquals(600.0, vm.state.value.draft!!.values.energyKcal!!, 0.001)
        assertEquals(300.0, (pending.state.value as PendingMeal.Reviewing).draft.grams)
        assertTrue(vm.state.value.canReset)

        vm.stepGrams(up = false)
        assertEquals(290.0, vm.state.value.draft!!.grams)
        assertEquals("290", vm.state.value.gramsText)
    }

    @Test
    fun `an invalid portion blocks Save without losing the typed text`() = runTest(dispatcher) {
        val vm = viewModel(FakePendingMeals(PendingMeal.Reviewing(draft())))

        vm.onGrams("0")

        assertTrue(vm.state.value.gramsInvalid)
        assertFalse(vm.state.value.canSave)
        assertEquals("0", vm.state.value.gramsText)
        assertEquals(200.0, vm.state.value.draft!!.grams)
    }

    @Test
    fun `the edit sheet keeps only values that differ from what it showed`() = runTest(dispatcher) {
        val vm = viewModel(FakePendingMeals(PendingMeal.Reviewing(draft())))

        val ok = vm.applyEdit(
            NutritionEdit(
                name = "Chicken salad",
                mealType = MealType.DINNER,
                time = LocalTime.of(12, 30),
                grams = "100",
                // 100 g shows 200 kcal, 20 g carbs, 5 g fat, 15 g protein; only protein is changed.
                energy = "200",
                carbs = "20",
                fat = "5",
                protein = "25",
            ),
        )

        assertTrue(ok)
        val d = vm.state.value.draft!!
        assertEquals("Chicken salad", d.name)
        assertEquals(MealType.DINNER, d.mealType)
        assertEquals(100.0, d.grams)
        assertEquals(NutritionValues(proteinGrams = 25.0), d.overrides)
        assertEquals(MealEdit.NAME_CHANGED, d.edit)
        assertEquals(LocalTime.of(12, 30), d.eatenAt.atZone(TEST_ZONE).toLocalTime())
    }

    @Test
    fun `an unreadable number in the sheet changes nothing`() = runTest(dispatcher) {
        val vm = viewModel(FakePendingMeals(PendingMeal.Reviewing(draft())))

        val ok = vm.applyEdit(NutritionEdit("x", MealType.LUNCH, LocalTime.NOON, "200", "abc", "", "", ""))

        assertFalse(ok)
        assertEquals("Grilled chicken salad", vm.state.value.draft!!.name)
    }

    @Test
    fun `a partial result saves once the missing values are typed`() = runTest(dispatcher) {
        val vm = viewModel(FakePendingMeals(PendingMeal.Reviewing(draft(NutritionValues(400.0, null, 40.0, null)))))
        assertEquals(setOf(NutrientField.PROTEIN, NutrientField.FAT), vm.state.value.missingFields)
        assertFalse(vm.state.value.canSave)

        vm.onMissingValue(NutrientField.PROTEIN, "30")
        vm.onMissingValue(NutrientField.FAT, "10")

        assertTrue(vm.state.value.canSave)
    }

    @Test
    fun `saving stores the meal and completes the draft`() = runTest(dispatcher) {
        val pending = FakePendingMeals(PendingMeal.Reviewing(draft()))
        val vm = viewModel(pending)

        vm.save()
        vm.state.first { it.saved != null }

        assertEquals(1, pending.completed)
        val saved = meals.observeMeals(TEST_NOW.minusSeconds(60), TEST_NOW.plusSeconds(60)).first().single()
        assertEquals("Grilled chicken salad", saved.name)
        assertEquals(MealEdit.NONE, saved.edit)
        assertEquals("meal-images/x.jpg", saved.imageRef)
    }

    @Test
    fun `low confidence is flagged below 80 percent`() = runTest(dispatcher) {
        val vm = viewModel(FakePendingMeals(PendingMeal.Reviewing(draft(confidence = 0.73))))

        assertTrue(vm.state.value.lowConfidence)
        assertEquals(73, vm.state.value.confidencePercent)
    }

    @Test
    fun `discard deletes the draft and closes`() = runTest(dispatcher) {
        val pending = FakePendingMeals(PendingMeal.Reviewing(draft()))
        val vm = viewModel(pending)

        vm.discard()
        advanceUntilIdle()

        assertEquals(1, pending.discarded)
        assertTrue(vm.state.value.closed)
        assertNull(pending.state.value)
    }

    @Test
    fun `without a draft the review closes`() = runTest(dispatcher) {
        val vm = viewModel(FakePendingMeals())

        assertTrue(vm.state.value.closed)
    }
}
