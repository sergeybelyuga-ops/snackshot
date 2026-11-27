package app.snackshot.ui.quickadd

import androidx.lifecycle.SavedStateHandle
import app.snackshot.domain.meal.MealSource
import app.snackshot.domain.meal.MealType
import app.snackshot.domain.nutrition.NutritionValues
import app.snackshot.ui.FakeMealRepository
import app.snackshot.ui.FakePendingMeals
import app.snackshot.ui.MutableClock
import app.snackshot.ui.TEST_NOW
import app.snackshot.ui.TEST_ZONE
import app.snackshot.ui.meal.MealSaver
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuickAddViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = MutableClock()
    private val meals = FakeMealRepository()
    private val handle = SavedStateHandle()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(h: SavedStateHandle = handle) = QuickAddViewModel(h, MealSaver(meals, clock), clock, FakePendingMeals()) {
        TEST_ZONE
    }

    @Test
    fun `starts now, with the meal type suggested from the time`() {
        val state = viewModel().state.value
        assertEquals(TEST_NOW, state.form.eatenAt)
        assertEquals("13:00 in Belgrade is lunch", MealType.LUNCH, state.form.mealType)
        assertTrue(state.errors.isEmpty())
        assertFalse(state.nothingEntered)
    }

    @Test
    fun `saves calories and macros, with a comma as decimal separator`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onName("  Protein bar ")
        vm.onEnergy("210")
        vm.onProtein("20,5")
        vm.onCarbs("22")
        vm.onFat("7")
        vm.save()
        advanceUntilIdle()

        val saved = meals.meals.value.single()
        assertEquals(vm.saved.value, saved.id)
        assertEquals(MealSource.QUICK_ADD, saved.source)
        assertEquals("Protein bar", saved.name)
        assertEquals(NutritionValues(210.0, 20.5, 22.0, 7.0), saved.values)
        assertNull(saved.grams)
        assertEquals(TEST_NOW, saved.eatenAt)
        assertEquals(TEST_ZONE.rules.getOffset(TEST_NOW), saved.zoneOffset)
    }

    @Test
    fun `nothing entered blocks the save and says why`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onName("Snack")
        vm.save()
        advanceUntilIdle()
        assertTrue(vm.state.value.nothingEntered)
        assertTrue(meals.meals.value.isEmpty())

        vm.onFat("5")
        assertFalse("the message goes as soon as a value is entered", vm.state.value.nothingEntered)
    }

    @Test
    fun `invalid numbers are marked at once and block the save`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onEnergy("12a")
        vm.onProtein("-3")
        assertEquals(FieldError.NOT_A_NUMBER, vm.state.value.errors[QuickAddField.ENERGY])
        assertEquals(FieldError.NEGATIVE, vm.state.value.errors[QuickAddField.PROTEIN])
        vm.save()
        advanceUntilIdle()
        assertTrue(meals.meals.value.isEmpty())
        assertNull(vm.saved.value)
    }

    @Test
    fun `limits from the domain are shown after Save`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onEnergy("20000")
        vm.onGrams("0")
        vm.onName("x".repeat(81))
        assertTrue("not before the first Save", vm.state.value.errors.isEmpty())
        vm.save()
        advanceUntilIdle()
        val errors = vm.state.value.errors
        assertEquals(FieldError.TOO_HIGH, errors[QuickAddField.ENERGY])
        assertEquals(FieldError.PORTION_RANGE, errors[QuickAddField.GRAMS])
        assertEquals(FieldError.TOO_LONG, errors[QuickAddField.NAME])
        assertTrue(meals.meals.value.isEmpty())
    }

    @Test
    fun `blank calories are saved from the macros, and can be filled in`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onProtein("10")
        vm.onCarbs("20")
        vm.onFat("5")
        assertEquals(165, vm.state.value.energyFromMacros)
        assertTrue(vm.state.value.canCalculateEnergy)

        vm.calculateEnergy()
        assertEquals("165", vm.state.value.form.energy)
        assertNull(vm.state.value.energyFromMacros)

        vm.onEnergy("")
        vm.save()
        advanceUntilIdle()
        assertEquals(165.0, meals.meals.value.single().energyKcal, 0.0)
    }

    @Test
    fun `the time can move back but not into the future, and the meal type follows it`() {
        val vm = viewModel()
        vm.onTime(LocalTime.of(8, 15))
        assertEquals(LocalTime.of(8, 15), vm.state.value.eatenAt.toLocalTime())
        assertEquals(MealType.BREAKFAST, vm.state.value.form.mealType)

        vm.onDate(LocalDate.parse("2026-11-26"))
        assertEquals(LocalDate.parse("2026-11-26"), vm.state.value.eatenAt.toLocalDate())

        vm.onDate(LocalDate.parse("2026-11-27"))
        vm.onTime(LocalTime.of(20, 0))
        assertEquals("later today becomes now", TEST_NOW, vm.state.value.form.eatenAt)
    }

    @Test
    fun `a chosen meal type stays when the time changes`() {
        val vm = viewModel()
        vm.onMealType(MealType.SNACK)
        vm.onTime(LocalTime.of(8, 0))
        assertEquals(MealType.SNACK, vm.state.value.form.mealType)
    }

    @Test
    fun `the form survives process death`() {
        val vm = viewModel()
        vm.onName("Soup")
        vm.onEnergy("12,")
        vm.onMealType(MealType.DINNER)
        vm.onTime(LocalTime.of(9, 30))

        val restored = viewModel(handle).state.value.form
        assertEquals("Soup", restored.name)
        assertEquals("half-typed numbers are kept as typed", "12,", restored.energy)
        assertEquals(MealType.DINNER, restored.mealType)
        assertEquals(LocalTime.of(9, 30), restored.eatenAt.atZone(TEST_ZONE).toLocalTime())
    }

    @Test
    fun `a second tap on Save saves once`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onEnergy("100")
        vm.save()
        vm.save()
        advanceUntilIdle()
        vm.save()
        advanceUntilIdle()
        assertNotNull(vm.saved.value)
        assertEquals(1, meals.meals.value.size)
    }
}
