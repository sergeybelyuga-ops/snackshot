package app.snackshot.ui.home

import app.snackshot.domain.access.AccessCheck
import app.snackshot.domain.access.AccessError
import app.snackshot.domain.access.AiAccessStatus
import app.snackshot.domain.meal.MealId
import app.snackshot.domain.meal.MealSource
import app.snackshot.domain.nutrition.CalorieRange
import app.snackshot.domain.nutrition.NutritionGoals
import app.snackshot.domain.nutrition.NutritionTotals
import app.snackshot.ui.FakeAccess
import app.snackshot.ui.FakeGoals
import app.snackshot.ui.FakeMealRepository
import app.snackshot.ui.MutableClock
import app.snackshot.ui.TEST_ZONE
import app.snackshot.ui.quickAddMeal
import app.snackshot.ui.state.UiState
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = MutableClock()
    private val meals = FakeMealRepository()
    private val goals = FakeGoals()
    private val access = FakeAccess()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(): HomeViewModel {
        val vm = HomeViewModel(meals, goals, access, clock) { TEST_ZONE }
        backgroundScope.launch { vm.state.collect() }
        backgroundScope.launch { vm.picturesLeft.collect() }
        runCurrent()
        return vm
    }

    private fun HomeViewModel.content() = (state.value as UiState.Success).data

    private fun at(local: String): Instant = LocalDateTime.parse(local).atZone(TEST_ZONE).toInstant()

    @Test
    fun `today's totals count only today's meals in the local time zone`() = runTest(dispatcher) {
        meals.meals.value = listOf(
            quickAddMeal("breakfast", at("2026-11-27T08:00"), 300.0, protein = 20.0, carbs = 40.0, fat = 5.0),
            quickAddMeal("lunch", at("2026-11-27T12:30"), 450.0, protein = 38.0, carbs = 14.0, fat = 26.0),
            // Late last night, local time: yesterday, not today.
            quickAddMeal("late", at("2026-11-26T23:30"), 200.0),
            quickAddMeal("old", at("2026-11-25T12:00"), 999.0),
        )

        val content = viewModel().content()

        assertEquals(LocalDate.parse("2026-11-27"), content.day)
        assertEquals(NutritionTotals(750.0, 58.0, 54.0, 31.0), content.totals)
        assertEquals(listOf("lunch", "breakfast"), content.today.map { it.id.value })
        assertEquals(listOf("late"), content.yesterday.map { it.id.value })
        assertEquals("12:30", content.today.first().time.toString())
    }

    @Test
    fun `a new meal shows up immediately`() = runTest(dispatcher) {
        val vm = viewModel()
        assertTrue(vm.content().isEmpty)

        meals.save(quickAddMeal("new", at("2026-11-27T13:00"), 120.0))
        runCurrent()

        assertEquals(120.0, vm.content().totals.energyKcal, 0.0)
        vm.onMealSaved(MealId("new"))
        assertEquals(MealId("new"), vm.highlighted.value)
        assertEquals(HomeMessage.MealSaved(MealId("new")), vm.message.value)
        vm.messageShown()
        assertNull(vm.message.value)
    }

    @Test
    fun `goals are shown when set`() = runTest(dispatcher) {
        val vm = viewModel()
        assertEquals(NutritionGoals.NONE, vm.content().goals)
        val set = NutritionGoals(calories = CalorieRange(2070, 2530), proteinGrams = 150)
        goals.setGoals(set)
        runCurrent()
        assertEquals(set, vm.content().goals)
    }

    @Test
    fun `Home moves to the new day at midnight`() = runTest(dispatcher) {
        meals.meals.value = listOf(quickAddMeal("today", at("2026-11-27T12:00"), 500.0))
        val vm = viewModel()
        assertEquals(1, vm.content().today.size)

        clock.now = at("2026-11-28T00:00:30")
        advanceTimeBy(61_000)
        runCurrent()

        assertEquals(LocalDate.parse("2026-11-28"), vm.content().day)
        assertTrue(vm.content().today.isEmpty())
        assertEquals(listOf("today"), vm.content().yesterday.map { it.id.value })
    }

    @Test
    fun `a stale AI status is refreshed at start`() = runTest(dispatcher) {
        viewModel()
        assertEquals(1, access.refreshCalls)
        assertEquals(0, access.ensureCalls)
    }

    @Test
    fun `Take a photo asks for AI access once, even when tapped twice`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        access.gate = gate
        access.next = AccessCheck.Available(FakeAccess.access(used = 3))
        val vm = viewModel()

        vm.onAiAction(MealSource.PHOTO)
        vm.onAiAction(MealSource.PHOTO)
        runCurrent()
        assertTrue(vm.checkingAccess.value)

        gate.complete(Unit)
        runCurrent()

        assertEquals(1, access.ensureCalls)
        assertFalse(vm.checkingAccess.value)
        // Access is available: the camera opens instead of a message.
        assertEquals(MealSource.PHOTO, vm.capture.value)
        assertEquals(null, vm.message.value)
        vm.captureOpened()
        assertEquals(null, vm.capture.value)
    }

    @Test
    fun `every AI access answer has a message`() = runTest(dispatcher) {
        val vm = viewModel()
        val cases = listOf(
            AccessCheck.NotAvailable(FakeAccess.access(AiAccessStatus.TRIAL_ENDED, used = 90)) to HomeMessage.TrialEnded,
            AccessCheck.NotAvailable(FakeAccess.access(AiAccessStatus.WAITLISTED)) to HomeMessage.Waitlisted,
            AccessCheck.NotAvailable(FakeAccess.access(aiEnabled = false)) to HomeMessage.AiPaused,
            AccessCheck.NotAvailable(FakeAccess.access(AiAccessStatus.SUSPENDED)) to HomeMessage.AiUnavailable,
            AccessCheck.Failed(AccessError.OFFLINE, null) to HomeMessage.Offline,
            AccessCheck.Failed(AccessError.AI_PAUSED, null) to HomeMessage.AiPaused,
            AccessCheck.Failed(AccessError.BUSY, null) to HomeMessage.Busy,
            AccessCheck.Failed(AccessError.SERVICE, null) to HomeMessage.ServiceError,
        )
        cases.forEach { (check, expected) ->
            access.next = check
            vm.onAiAction(MealSource.GALLERY)
            runCurrent()
            assertEquals(check.toString(), expected, vm.message.value)
            vm.messageShown()
        }
    }

    @Test
    fun `free pictures left are known once a trial exists`() = runTest(dispatcher) {
        val vm = viewModel()
        assertNull(vm.picturesLeft.value)
        access.state.value = FakeAccess.access(used = 10)
        runCurrent()
        assertEquals(80, vm.picturesLeft.value)
        access.state.value = FakeAccess.access(AiAccessStatus.WAITLISTED)
        runCurrent()
        assertNull(vm.picturesLeft.value)
    }
}
