package app.snackshot.ui.state

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import app.snackshot.ui.R
import app.snackshot.ui.theme.Size
import app.snackshot.ui.theme.SnackshotTheme
import app.snackshot.ui.theme.Spacing

/**
 * Renders a [UiState] by the shared contract (doc 10 §4.1).
 *
 * - Loading without data: centered progress. Loading with data: the data plus a thin progress bar.
 * - Error without data: [ErrorState]. Error with data: the data plus an [ErrorBanner].
 * - Empty: [empty], which must offer one clear next action.
 */
@Composable
fun <T> StateContent(
    state: UiState<T>,
    onRetry: () -> Unit,
    empty: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (state) {
            is UiState.Success -> content(state.data)
            UiState.Empty -> empty()
            is UiState.Loading -> {
                val previous = state.previous
                if (previous == null) {
                    LoadingState()
                } else {
                    content(previous)
                    LoadingBar(Modifier.align(Alignment.TopCenter))
                }
            }
            is UiState.Error -> {
                val previous = state.previous
                if (previous == null) {
                    ErrorState(state.error, onRetry = onRetry)
                } else {
                    Column {
                        ErrorBanner(state.error, onRetry = onRetry)
                        content(previous)
                    }
                }
            }
        }
    }
}

/** Full-screen loading when there is nothing to show yet. */
@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    val label = stringResource(R.string.state_loading)
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = label })
    }
}

/** Thin progress over content that stays visible. */
@Composable
fun LoadingBar(modifier: Modifier = Modifier) {
    val label = stringResource(R.string.state_loading)
    LinearProgressIndicator(modifier = modifier.fillMaxWidth().semantics { contentDescription = label })
}

/**
 * Empty state with exactly one primary next action.
 */
@Composable
fun EmptyState(title: String, message: String, actionLabel: String, onAction: () -> Unit, modifier: Modifier = Modifier) {
    CenteredMessage(modifier) {
        Icon(
            painter = painterResource(R.drawable.ic_state_empty),
            contentDescription = null, // decorative; the title says it
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Size.minTouchTarget),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onAction, modifier = Modifier.heightIn(min = Size.minTouchTarget)) { Text(actionLabel) }
    }
}

/**
 * Full-screen error: names the failed operation, offers Retry and optionally a local fallback
 * (e.g. "Use Quick add").
 */
@Composable
fun ErrorState(
    error: UiError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    fallbackLabel: String? = null,
    onFallback: (() -> Unit)? = null,
) {
    CenteredMessage(modifier.semantics { liveRegion = LiveRegionMode.Assertive }) {
        Icon(
            painter = painterResource(R.drawable.ic_state_error),
            contentDescription = stringResource(R.string.state_error),
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(Size.minTouchTarget),
        )
        Text(
            text = error.operation,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        error.detail?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (error.retryable) {
            Button(onClick = onRetry, modifier = Modifier.heightIn(min = Size.minTouchTarget)) {
                Text(stringResource(R.string.state_retry))
            }
        }
        if (fallbackLabel != null && onFallback != null) {
            OutlinedButton(onClick = onFallback, modifier = Modifier.heightIn(min = Size.minTouchTarget)) { Text(fallbackLabel) }
        }
    }
}

/** Inline error above content that is still shown. */
@Composable
fun ErrorBanner(error: UiError, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.content)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            modifier = Modifier.padding(start = Spacing.content, end = Spacing.small),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_state_error),
                contentDescription = stringResource(R.string.state_error),
                tint = MaterialTheme.colorScheme.error,
            )
            Text(
                text = error.operation,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f).padding(vertical = Spacing.medium),
            )
            if (error.retryable) {
                TextButton(onClick = onRetry, modifier = Modifier.heightIn(min = Size.minTouchTarget)) {
                    Text(stringResource(R.string.state_retry))
                }
            }
        }
    }
}

/** Snackbar host in Snackshot style. Messages are announced by TalkBack (Material behavior). */
@Composable
fun SnackshotSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        Snackbar(snackbarData = data, shape = MaterialTheme.shapes.medium)
    }
}

@Composable
private fun CenteredMessage(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(Spacing.section),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}

@Preview(showBackground = true)
@Composable
private fun EmptyStatePreview() {
    SnackshotTheme {
        EmptyState("No meals yet", "Take a photo or use Quick add to log your first meal.", "Quick add", onAction = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorStatePreview() {
    SnackshotTheme(darkTheme = true) {
        Surface {
            ErrorState(
                UiError("Couldn't recognize the photo", "Check your connection and try again."),
                onRetry = {},
                fallbackLabel = "Use Quick add",
                onFallback = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorBannerPreview() {
    SnackshotTheme { ErrorBanner(UiError("Couldn't sync with Health Connect"), onRetry = {}) }
}
