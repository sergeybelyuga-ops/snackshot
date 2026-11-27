package app.snackshot.ui.goals

import androidx.lifecycle.SavedStateHandle
import app.snackshot.domain.nutrition.CalorieRange
import app.snackshot.domain.nutrition.NutritionGoals
import app.snackshot.ui.FakeGoals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GoalDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val goals = FakeGoals()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(
        kind: GoalKind,
        handle: SavedStateHandle = SavedStateHandle(mapOf("goal" to kind.name)),
    ): GoalDetailViewModel {
        val vm = GoalDetailViewModel(handle, goals)
        advanceUntilIdle()
        return vm
    }

    private val GoalDetailViewModel.draft get() = state.value!!.draft

    @Test
    fun `an unset calorie goal starts at the prototype's 2,070 to 2,530 kcal and can be set`() = runTest(dispatcher) {
        val vm = viewModel(GoalKind.CALORIES)
        assertFalse(vm.state.value!!.isSet)
        assertEquals(2070, vm.draft.minKcal)
        assertEquals(2530, vm.draft.maxKcal)

        vm.increaseMin()
        vm.decreaseMax()
        vm.setGoal()
        advanceUntilIdle()

        assertEquals(CalorieRange(2080, 2520), goals.state.value.calories)
        assertTrue(vm.done.value)
    }

    @Test
    fun `a macro goal changes by a gram and keeps the other goals`() = runTest(dispatcher) {
        goals.state.value = NutritionGoals(calories = CalorieRange(1800, 2200), fatGrams = 60)
        val vm = viewModel(GoalKind.PROTEIN)
        assertEquals(150, vm.draft.grams)
        assertEquals(2000, vm.state.value!!.calorieTargetKcal)

        repeat(5) { vm.decreaseGrams() }
        vm.setGoal()
        advanceUntilIdle()

        assertEquals(NutritionGoals(calories = CalorieRange(1800, 2200), proteinGrams = 145, fatGrams = 60), goals.state.value)
    }

    @Test
    fun `an existing goal is the starting point and can be cleared alone`() = runTest(dispatcher) {
        goals.state.value = NutritionGoals(carbsGrams = 250, fatGrams = 60)
        val vm = viewModel(GoalKind.CARBS)
        assertTrue(vm.state.value!!.isSet)
        assertEquals(250, vm.draft.grams)

        vm.clearGoal()
        advanceUntilIdle()

        assertEquals(NutritionGoals(fatGrams = 60), goals.state.value)
    }

    @Test
    fun `the minimum stays below the maximum and both stay within the limits`() = runTest(dispatcher) {
        goals.state.value = NutritionGoals(calories = CalorieRange(800, 820))
        val vm = viewModel(GoalKind.CALORIES)
        val s = vm.state.value!!
        assertFalse(s.canDecreaseMin)
        assertTrue(s.canIncreaseMin)
        vm.decreaseMin()
        assertEquals(800, vm.draft.minKcal)

        vm.increaseMin()
        assertEquals(810, vm.draft.minKcal)
        assertFalse(vm.state.value!!.canIncreaseMin)
        assertFalse(vm.state.value!!.canDecreaseMax)
        vm.increaseMin()
        assertEquals("never reaches the maximum", 810, vm.draft.minKcal)
    }

    @Test
    fun `macro steppers stop at the limits`() = runTest(dispatcher) {
        goals.state.value = NutritionGoals(fatGrams = 400)
        val vm = viewModel(GoalKind.FAT)
        assertFalse(vm.state.value!!.canIncreaseGrams)
        vm.increaseGrams()
        assertEquals(400, vm.draft.grams)
    }

    @Test
    fun `the draft survives process death`() = runTest(dispatcher) {
        val handle = SavedStateHandle(mapOf("goal" to "CARBS"))
        val vm = viewModel(GoalKind.CARBS, handle)
        repeat(3) { vm.increaseGrams() }
        assertEquals(223, viewModel(GoalKind.CARBS, handle).draft.grams)
    }
}
