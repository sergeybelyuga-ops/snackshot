package app.snackshot.ui.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import app.snackshot.ui.R
import app.snackshot.ui.theme.Spacing

// Tab screens. Dashboard and History are placeholders until their increments (I-5, I-2);
// Home is home/HomeScreen.kt and Settings settings/SettingsScreen.kt.

@Composable
fun DashboardScreen(modifier: Modifier = Modifier) = TabScreen(stringResource(R.string.nav_dashboard), modifier) {
    PlaceholderText(stringResource(R.string.dashboard_placeholder))
}

@Composable
fun HistoryScreen(modifier: Modifier = Modifier) = TabScreen(stringResource(R.string.nav_history), modifier) {
    PlaceholderText(stringResource(R.string.history_placeholder))
}

/** A tab screen above the NavigationBar, which handles the bottom inset itself. */
@Composable
private fun TabScreen(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.content),
            verticalArrangement = Arrangement.spacedBy(Spacing.content),
        ) {
            Text(title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
            content()
        }
    }
}

@Composable
private fun PlaceholderText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
