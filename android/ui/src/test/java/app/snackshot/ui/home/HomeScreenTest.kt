package app.snackshot.ui.home

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import app.snackshot.domain.meal.MealId
import app.snackshot.domain.meal.MealSource
import app.snackshot.domain.meal.MealType
import app.snackshot.domain.nutrition.CalorieRange
import app.snackshot.domain.nutrition.NutritionGoals
import app.snackshot.domain.nutrition.NutritionTotals
import app.snackshot.domain.nutrition.NutritionValues
import app.snackshot.ui.state.UiError
import app.snackshot.ui.state.UiState
import app.snackshot.ui.theme.SnackshotTheme
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class HomeScreenTest {
    @get:Rule val compose = createComposeRule()

    private val entries = mutableListOf<EntryAction>()
    private var opened = ""

    private val lunch = HomeMeal(
        id = MealId("lunch"),
        name = "Grilled chicken salad",
        source = MealSource.PHOTO,
        mealType = MealType.LUNCH,
        time = LocalTime.of(12, 30),
        energyKcal = 450.0,
        values = NutritionValues(450.0, 38.0, 14.0, 26.0),
    )
    private val unnamed = lunch.copy(id = MealId("quick"), name = "", source = MealSource.QUICK_ADD, energyKcal = 300.0)

    private fun content(
        goals: NutritionGoals = NutritionGoals.NONE,
        today: List<HomeMeal> = emptyList(),
        yesterday: List<HomeMeal> = emptyList(),
    ) = HomeContent(LocalDate.parse("2026-11-27"), NutritionTotals(750.0, 58.0, 54.0, 31.0), goals, today, yesterday)

    private fun show(state: UiState<HomeContent>, busy: Boolean = false, highlighted: MealId? = null) {
        compose.setContent {
            SnackshotTheme {
                HomeScreen(
                    state = state,
                    highlighted = highlighted,
                    message = null,
                    checkingAccess = busy,
                    onMessageShown = {},
                    onRetry = {},
                    onEntry = { entries += it },
                    onManualAdd = { opened = "quick add" },
                    onOpenDashboard = { opened = "dashboard" },
                    onOpenHistory = { opened = "history" },
                )
            }
        }
    }

    private fun center(label: String) = compose.onNodeWithContentDescription("$label. Hold and slide up or down to change the action")

    @Test
    fun `without goals the summary shows totals and says no goal is set`() {
        show(UiState.Success(content()))
        compose.onNodeWithContentDescription("Today: 750 kcal, no daily goal", substring = true).assertIsDisplayed()
        compose.onNodeWithText("No meals yet").assertIsDisplayed()
    }

    @Test
    fun `with a calorie goal the summary shows what is left of its target`() {
        show(UiState.Success(content(NutritionGoals(calories = CalorieRange(2070, 2530), proteinGrams = 150))))
        compose.onNodeWithContentDescription("Today: 750 of 2,300 kcal, 1,550 left", substring = true).assertIsDisplayed()
        compose.onNodeWithContentDescription("Protein 58 of 150 grams", substring = true).assertIsDisplayed()
    }

    @Test
    fun `the summary opens the dashboard and See all opens history`() {
        show(UiState.Success(content()))
        compose.onNodeWithContentDescription("Today:", substring = true).performClick()
        assertEquals("dashboard", opened)
        compose.onNodeWithText("See all").performScrollTo().performClick()
        assertEquals("history", opened)
    }

    // Times are matched up to the minutes: the JDK puts a narrow no-break space before "PM".
    @Test
    fun `recent meals are grouped by day, and a nameless Quick add is called Quick add`() {
        show(UiState.Success(content(today = listOf(lunch), yesterday = listOf(unnamed))), highlighted = MealId("lunch"))
        compose.onNodeWithContentDescription(
            "Grilled chicken salad, Lunch at 12:30",
            substring = true,
        ).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Yesterday").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Quick add, Lunch at 12:30", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `the carousel starts on Take a photo and loops through Gallery and Manual add`() {
        show(UiState.Success(content()))
        center("Take a photo").performClick()
        // Sliding points at the action below (next) or above (previous); lifting the finger picks it.
        center("Take a photo").performTouchInput { swipeDown() }
        center("Gallery").performClick()
        center("Gallery").performTouchInput { swipeDown() }
        center("Manual add").performClick()
        center("Manual add").performTouchInput { swipeDown() }
        center("Take a photo").performTouchInput { swipeUp() }
        center("Manual add").assertIsDisplayed()
        assertEquals(listOf(EntryAction.TAKE_PHOTO, EntryAction.GALLERY, EntryAction.MANUAL_ADD), entries)
    }

    @Test
    fun `holding and sliding changes the action without starting it`() {
        show(UiState.Success(content()))
        center("Take a photo").performTouchInput {
            down(center)
            moveBy(Offset(0f, 20.dp.toPx()))
            moveBy(Offset(0f, 20.dp.toPx()))
            moveBy(Offset(0f, 20.dp.toPx()))
            up()
        }
        center("Gallery").assertIsDisplayed()
        center("Gallery").performTouchInput {
            down(center)
            // A slide shorter than half a row stays on the action it started from.
            moveBy(Offset(0f, -24.dp.toPx()))
            up()
        }
        center("Gallery").assertIsDisplayed()
        assertEquals(emptyList<EntryAction>(), entries)
    }

    @Test
    fun `TalkBack users switch actions with custom actions`() {
        show(UiState.Success(content()))
        center("Take a photo").performCustomAccessibilityActionWithLabel("Switch to Gallery")
        center("Gallery").performCustomAccessibilityActionWithLabel("Switch to Take a photo")
        center("Take a photo").performCustomAccessibilityActionWithLabel("Switch to Manual add")
        center("Manual add").assertIsDisplayed()
    }

    @Test
    fun `the entry action waits while AI access is checked`() {
        show(UiState.Success(content()), busy = true)
        center("Take a photo").assertIsNotEnabled()
    }

    @Test
    fun `an error offers Retry and Quick add`() {
        val state: UiState<HomeContent> = UiState.Error(UiError("x"))
        compose.setContent {
            SnackshotTheme {
                HomeScreen(state, null, null, false, {}, {}, { entries += it }, { opened = "quick add" }, {}, {})
            }
        }
        compose.onNodeWithText("Couldn't load your meals").assertIsDisplayed()
        compose.onNodeWithText("Quick add").performClick()
        assertEquals("quick add", opened)
    }
}
