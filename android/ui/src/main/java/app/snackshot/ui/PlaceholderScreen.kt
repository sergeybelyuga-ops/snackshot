package app.snackshot.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.snackshot.ui.state.EmptyState
import app.snackshot.ui.theme.SnackshotTheme
import app.snackshot.ui.theme.Spacing

/**
 * Temporary start screen until S-01 / S-02 arrive in WBS 2.2.
 * Shows a sample of the design system so the theme can be checked on a device.
 */
@Composable
fun PlaceholderScreen(environmentName: String, modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.content),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            Text("Today", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
            Text("Environment: $environmentName", style = MaterialTheme.typography.labelSmall)
            Text("1 850 kcal", style = MaterialTheme.typography.displayLarge)
            Text("Your intake is within your goal.", style = MaterialTheme.typography.bodyLarge)
            StatusBadge()
            Text("Calories", style = MaterialTheme.typography.titleLarge)
            Text(
                "Supporting copy in body medium, with the on-surface-variant color.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
                EmptyState(
                    title = "No meals yet",
                    message = "Take a photo or use Quick add to log your first meal.",
                    actionLabel = "Quick add",
                    onAction = {},
                    modifier = Modifier.fillMaxWidth().height(320.dp),
                )
            }
        }
    }
}

@Composable
private fun StatusBadge() {
    val status = SnackshotTheme.colors.statusInRange
    Surface(color = status.container, contentColor = status.content, shape = MaterialTheme.shapes.small) {
        Text(
            "In range",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = Spacing.small, vertical = Spacing.grid).wrapContentWidth(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceholderScreenPreview() {
    SnackshotTheme { PlaceholderScreen(environmentName = "local") }
}

@Preview(showBackground = true)
@Composable
private fun PlaceholderScreenDarkPreview() {
    SnackshotTheme(darkTheme = true) { PlaceholderScreen(environmentName = "local") }
}
