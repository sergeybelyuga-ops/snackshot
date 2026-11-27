package app.snackshot.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import app.snackshot.ui.FakeAccess
import app.snackshot.ui.FakeGoals
import app.snackshot.ui.FakeMealRepository
import app.snackshot.ui.MutableClock
import app.snackshot.ui.TEST_ZONE
import app.snackshot.ui.home.HomeViewModel
import app.snackshot.ui.meal.MealSaver
import app.snackshot.ui.quickadd.QuickAddViewModel
import app.snackshot.ui.theme.SnackshotTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Global navigation for I-0: S-01 → S-02, tabs, S-01A (doc 10 §2.2, WBS §7.5 I-0). */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class NavigationTest {
    @get:Rule val compose = createComposeRule()

    private var completed = 0
    private lateinit var nav: NavHostController

    private fun launch(welcomeCompleted: Boolean) {
        compose.setContent {
            nav = rememberNavController()
            SnackshotTheme {
                SnackshotNavHost(
                    welcomeCompleted = welcomeCompleted,
                    onWelcomeCompleted = { completed++ },
                    environmentName = "local",
                    navController = nav,
                    viewModels = fakeViewModels,
                )
            }
        }
    }

    private val meals = FakeMealRepository()
    private val clock = MutableClock()

    private val fakeViewModels = object : ScreenViewModels {
        @Composable
        override fun home(): HomeViewModel = viewModel { HomeViewModel(meals, FakeGoals(), FakeAccess(), clock) { TEST_ZONE } }

        @Composable
        override fun quickAdd(): QuickAddViewModel = viewModel {
            QuickAddViewModel(createSavedStateHandle(), MealSaver(meals, clock), clock) { TEST_ZONE }
        }
    }

    private fun tab(label: String) = compose.onNode(hasText(label) and hasClickAction())

    @Test
    fun `first run shows Welcome without bottom navigation`() {
        launch(welcomeCompleted = false)
        compose.onNodeWithText("Welcome to Snackshot").assertIsDisplayed()
        compose.onNodeWithText("Dashboard").assertDoesNotExist()
    }

    @Test
    fun `Continue opens Home and removes Welcome from the back stack`() {
        launch(welcomeCompleted = false)
        compose.onNodeWithText("Continue").performClick()
        compose.onNodeWithText("Recent meals").assertIsDisplayed()
        tab("Home").assertIsSelected()
        assertEquals(1, completed)
        assertNull("Back from Home must leave the app, not return to Welcome", nav.previousBackStackEntry)
    }

    @Test
    fun `Privacy details from Welcome and back`() {
        launch(welcomeCompleted = false)
        compose.onNodeWithText("Privacy details").performClick()
        compose.onNodeWithText("Your data stays on your phone").assertIsDisplayed()
        compose.onNodeWithText("Product metrics").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Welcome to Snackshot").assertIsDisplayed()
        assertEquals(0, completed)
    }

    @Test
    fun `returning user starts on Home and can switch tabs`() {
        launch(welcomeCompleted = true)
        compose.onNodeWithText("Recent meals").assertIsDisplayed()
        listOf("Dashboard", "History", "Settings").forEach { label ->
            tab(label).performClick()
            tab(label).assertIsSelected()
        }
        compose.onNodeWithText("Build: local").assertIsDisplayed()
        tab("Home").performClick()
        compose.onNodeWithText("Recent meals").assertIsDisplayed()
    }

    @Test
    fun `Settings opens Privacy details without the bottom bar`() {
        launch(welcomeCompleted = true)
        tab("Settings").performClick()
        compose.onNodeWithText("Privacy details").performClick()
        compose.onNodeWithText("Your data stays on your phone").assertIsDisplayed()
        compose.onNodeWithText("Dashboard").assertDoesNotExist()
        compose.onNodeWithText("Got it").performScrollTo().performClick()
        tab("Settings").assertIsSelected()
    }

    @Test
    fun `Manual add opens Quick add, and a saved meal shows on Home`() {
        launch(welcomeCompleted = true)
        compose.onNodeWithContentDescription(
            "Take a photo",
            substring = true,
        ).performCustomAccessibilityActionWithLabel("Switch to Manual add")
        compose.onNodeWithContentDescription("Manual add", substring = true).performClick()
        compose.onNodeWithText("Log calories and macros you already know. No photo or search needed.").assertIsDisplayed()
        compose.onNodeWithText("Dashboard").assertDoesNotExist()

        compose.onNodeWithText("Name").performTextInput("Protein bar")
        compose.onNodeWithText("Calories").performTextInput("210")
        compose.onNodeWithText("Save").performClick()

        compose.onNodeWithText("Recent meals").assertIsDisplayed()
        compose.onNodeWithContentDescription("Protein bar, Lunch at", substring = true).assertIsDisplayed()
        assertEquals(1, meals.meals.value.size)
        assertNull("Back from Home leaves the app", nav.previousBackStackEntry)
    }

    @Test
    fun `Back from Quick add returns Home without saving`() {
        launch(welcomeCompleted = true)
        compose.onNodeWithContentDescription(
            "Take a photo",
            substring = true,
        ).performCustomAccessibilityActionWithLabel("Switch to Manual add")
        compose.onNodeWithContentDescription("Manual add", substring = true).performClick()
        compose.onNodeWithText("Calories").performTextInput("210")
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Recent meals").assertIsDisplayed()
        assertEquals(0, meals.meals.value.size)
    }
}
