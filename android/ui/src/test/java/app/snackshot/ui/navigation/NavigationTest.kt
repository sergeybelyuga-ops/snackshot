package app.snackshot.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import app.snackshot.ui.theme.SnackshotTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Global navigation for I-0: S-01 → S-02, tabs, S-01A (doc 10 §2.2, WBS §7.5 I-0). */
@RunWith(RobolectricTestRunner::class)
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
                )
            }
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
        compose.onNodeWithText("Today").assertIsDisplayed()
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
        compose.onNodeWithText("Today").assertIsDisplayed()
        listOf("Dashboard", "History", "Settings").forEach { label ->
            tab(label).performClick()
            tab(label).assertIsSelected()
        }
        compose.onNodeWithText("Build: local").assertIsDisplayed()
        tab("Home").performClick()
        compose.onNodeWithText("Today").assertIsDisplayed()
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
}
