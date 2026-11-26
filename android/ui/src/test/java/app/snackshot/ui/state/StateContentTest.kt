package app.snackshot.ui.state

import androidx.compose.material3.Text
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.snackshot.ui.theme.SnackshotTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The shared state contract (doc 10 §4.1) as rendered by [StateContent]. */
@RunWith(RobolectricTestRunner::class)
class StateContentTest {
    @get:Rule val compose = createComposeRule()

    private var retries = 0

    private fun render(state: UiState<String>) {
        compose.setContent {
            SnackshotTheme {
                StateContent(
                    state = state,
                    onRetry = { retries++ },
                    empty = { EmptyState("No meals yet", "Log your first meal.", "Quick add", onAction = {}) },
                ) { data -> Text("data: $data") }
            }
        }
    }

    @Test
    fun `success shows the data`() {
        render(UiState.Success("lunch"))
        compose.onNodeWithText("data: lunch").assertIsDisplayed()
    }

    @Test
    fun `loading without data shows progress only`() {
        render(UiState.Loading())
        compose.onNodeWithContentDescription("Loading").assertIsDisplayed()
        compose.onNodeWithText("data:", substring = true).assertDoesNotExist()
    }

    @Test
    fun `loading keeps already available data visible`() {
        render(UiState.Loading(previous = "lunch"))
        compose.onNodeWithText("data: lunch").assertIsDisplayed()
        compose.onNodeWithContentDescription("Loading").assertIsDisplayed()
    }

    @Test
    fun `error names the operation and retries`() {
        render(UiState.Error(UiError("Couldn't load your meals")))
        compose.onNodeWithText("Couldn't load your meals").assertIsDisplayed()
        compose.onNodeWithText("Retry").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun `error with data shows a banner above the data`() {
        render(UiState.Error(UiError("Couldn't sync"), previous = "lunch"))
        compose.onNodeWithText("data: lunch").assertIsDisplayed()
        compose.onNodeWithText("Couldn't sync").assertIsDisplayed()
    }

    @Test
    fun `non-retryable error has no retry button`() {
        render(UiState.Error(UiError("Pilot is full", retryable = false)))
        compose.onNodeWithText("Retry").assertDoesNotExist()
    }

    @Test
    fun `empty offers one next action`() {
        render(UiState.Empty)
        compose.onNodeWithText("No meals yet").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        compose.onNodeWithText("Quick add").assertIsDisplayed()
    }

    @Test
    fun `dataOrNull exposes what can be shown`() {
        assertEquals("a", UiState.Loading("a").dataOrNull)
        assertNull(UiState.Empty.dataOrNull)
        assertEquals("b", UiState.Error(UiError("x"), "b").dataOrNull)
    }
}
