package app.snackshot.ui.privacy

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import app.snackshot.ui.R
import app.snackshot.ui.theme.Size
import app.snackshot.ui.theme.SnackshotTheme
import app.snackshot.ui.theme.Spacing
import app.snackshot.ui.welcome.BrandMark

private data class PrivacyItem(@DrawableRes val icon: Int, @StringRes val title: Int, @StringRes val body: Int)

private data class PrivacySection(
    @DrawableRes val icon: Int,
    @StringRes val heading: Int,
    @StringRes val body: Int,
    val items: List<PrivacyItem>,
)

private val sections = listOf(
    PrivacySection(
        R.drawable.ic_smartphone,
        R.string.privacy_usage_heading,
        R.string.privacy_usage_body,
        listOf(
            PrivacyItem(R.drawable.ic_cloud_off, R.string.privacy_not_stored_title, R.string.privacy_not_stored_body),
            PrivacyItem(R.drawable.ic_key, R.string.privacy_access_title, R.string.privacy_access_body),
            PrivacyItem(R.drawable.ic_tune, R.string.privacy_goals_title, R.string.privacy_goals_body),
        ),
    ),
    PrivacySection(
        R.drawable.ic_photo_camera,
        R.string.privacy_ai_heading,
        R.string.privacy_ai_body,
        listOf(
            PrivacyItem(R.drawable.ic_check_circle, R.string.privacy_sent_title, R.string.privacy_sent_body),
            PrivacyItem(R.drawable.ic_block, R.string.privacy_never_title, R.string.privacy_never_body),
            PrivacyItem(R.drawable.ic_nav_history, R.string.privacy_kept_title, R.string.privacy_kept_body),
        ),
    ),
    PrivacySection(
        R.drawable.ic_nav_bar_chart,
        R.string.privacy_metrics_heading,
        R.string.privacy_metrics_body,
        listOf(
            PrivacyItem(R.drawable.ic_check_circle, R.string.privacy_metrics_contains_title, R.string.privacy_metrics_contains_body),
            PrivacyItem(R.drawable.ic_tune, R.string.privacy_metrics_off_title, R.string.privacy_metrics_off_body),
        ),
    ),
    PrivacySection(
        R.drawable.ic_favorite,
        R.string.privacy_hc_heading,
        R.string.privacy_hc_body,
        listOf(
            PrivacyItem(R.drawable.ic_account_circle, R.string.privacy_hc_account_title, R.string.privacy_hc_account_body),
            PrivacyItem(R.drawable.ic_sync, R.string.privacy_hc_connected_title, R.string.privacy_hc_connected_body),
            PrivacyItem(R.drawable.ic_tune, R.string.privacy_hc_choice_title, R.string.privacy_hc_choice_body),
        ),
    ),
)

/** S-01A Privacy details (doc 10 §4.3): bundled long read, reachable from Welcome and Settings. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyDetailsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.privacy_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.content),
            verticalArrangement = Arrangement.spacedBy(Spacing.section),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.medium), horizontalAlignment = Alignment.CenterHorizontally) {
                BrandMark()
                Text(
                    stringResource(R.string.privacy_headline),
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.semantics { heading() },
                )
                Text(stringResource(R.string.privacy_intro), style = MaterialTheme.typography.bodyLarge)
            }
            sections.forEach { Section(it) }
            Text(
                stringResource(R.string.privacy_footer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth().heightIn(min = Size.minTouchTarget)) {
                Text(stringResource(R.string.privacy_done))
            }
        }
    }
}

@Composable
private fun Section(section: PrivacySection) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(Spacing.content), verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.medium), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(section.icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    stringResource(section.heading),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
            }
            Text(stringResource(section.body), style = MaterialTheme.typography.bodyLarge)
            section.items.forEach { item ->
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                    Icon(painterResource(item.icon), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(item.title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(item.body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 1800)
@Composable
private fun PrivacyDetailsPreview() {
    SnackshotTheme { PrivacyDetailsScreen(onBack = {}) }
}
