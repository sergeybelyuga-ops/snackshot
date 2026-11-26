package app.snackshot.ui.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.snackshot.ui.R
import app.snackshot.ui.state.ErrorState
import app.snackshot.ui.state.UiError
import app.snackshot.ui.theme.Size
import app.snackshot.ui.theme.SnackshotTheme
import app.snackshot.ui.theme.Spacing

/** S-01 Welcome (doc 10 §4.2). No account, no sign-in: Continue goes straight to Home. */
@Composable
fun WelcomeScreen(onContinue: () -> Unit, onPrivacyDetails: () -> Unit, modifier: Modifier = Modifier) {
    WelcomeFrame(modifier) {
        Text(
            text = stringResource(R.string.welcome_title),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.welcome_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.heightIn(min = Spacing.small))
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().heightIn(min = Size.minTouchTarget)) {
            Text(stringResource(R.string.welcome_continue))
        }
        TextButton(onClick = onPrivacyDetails, modifier = Modifier.heightIn(min = Size.minTouchTarget)) {
            Text(stringResource(R.string.welcome_privacy_details))
        }
    }
}

/** S-01 loading: restoring local state before the start destination is known. */
@Composable
fun StartLoadingScreen(modifier: Modifier = Modifier) {
    WelcomeFrame(modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
        CircularProgressIndicator()
        Text(
            stringResource(R.string.welcome_loading_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.welcome_loading_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** S-01 error: local storage could not be opened. */
@Composable
fun StartErrorScreen(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier) { padding ->
        ErrorState(
            error = UiError(
                operation = stringResource(R.string.welcome_error_title),
                detail = stringResource(R.string.welcome_error_body),
            ),
            onRetry = onRetry,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
private fun WelcomeFrame(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Scaffold(modifier = modifier) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState()) // very large text wraps and scrolls, never clips
                    .padding(horizontal = Spacing.section, vertical = Spacing.content),
                verticalArrangement = Arrangement.spacedBy(Spacing.content),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BrandMark()
                content()
            }
        }
    }
}

@Composable
internal fun BrandMark(modifier: Modifier = Modifier) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = modifier.size(96.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_state_empty),
                contentDescription = stringResource(R.string.brand_mark),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WelcomePreview() {
    SnackshotTheme { WelcomeScreen(onContinue = {}, onPrivacyDetails = {}) }
}

@Preview(showBackground = true)
@Composable
private fun StartLoadingPreview() {
    SnackshotTheme(darkTheme = true) { StartLoadingScreen() }
}
