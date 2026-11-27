package app.snackshot.ui.settings

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.snackshot.domain.nutrition.NutritionGoals
import app.snackshot.ui.R
import app.snackshot.ui.home.formatInt
import app.snackshot.ui.theme.Spacing

/**
 * S-16 Settings hub (prototype `s16_settings.html`, `view-settings`). Only the sections that work
 * today are shown; AI recognition, Health Connect, image retention and theme arrive with their
 * increments (WBS 11.1).
 */
@Composable
fun SettingsScreen(
    goals: NutritionGoals?,
    environmentName: String,
    onPrivacyDetails: () -> Unit,
    onNutritionPreferences: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsPage(
        title = stringResource(R.string.nav_settings),
        onBack = null,
        modifier = modifier,
        // A tab: the NavigationBar below handles the bottom inset.
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    ) {
        ListGroup(count = 2) { i, shape ->
            when (i) {
                0 -> ListRow(
                    title = stringResource(R.string.goals_title),
                    support = goalsSummary(goals),
                    shape = shape,
                    onClick = onNutritionPreferences,
                    leading = { RowIcon(R.drawable.ic_nutrition) },
                    trailing = { Chevron() },
                )
                else -> ListRow(
                    title = stringResource(R.string.welcome_privacy_details),
                    support = stringResource(R.string.settings_privacy_support),
                    shape = shape,
                    onClick = onPrivacyDetails,
                    leading = { RowIcon(R.drawable.ic_shield) },
                    trailing = { Chevron() },
                )
            }
        }
        Footnote(R.drawable.ic_smartphone, stringResource(R.string.settings_storage_note))
        Text(
            stringResource(R.string.settings_build, environmentName),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.grid),
        )
    }
}

/** "2,300 kcal · C 220 · F 70 · P 150 g", with only the goals that are set. */
@Composable
private fun goalsSummary(goals: NutritionGoals?): String? {
    if (goals == null) return null
    if (goals.isEmpty) return stringResource(R.string.goals_settings_support)
    val parts = buildList {
        goals.calories?.let { add(stringResource(R.string.goals_value_kcal, formatInt(it.targetKcal))) }
        val macros = listOfNotNull(
            goals.carbsGrams?.let { stringResource(R.string.settings_goal_carbs, it) },
            goals.fatGrams?.let { stringResource(R.string.settings_goal_fat, it) },
            goals.proteinGrams?.let { stringResource(R.string.settings_goal_protein, it) },
        )
        if (macros.isNotEmpty()) add(macros.joinToString(" · ") + " " + stringResource(R.string.unit_g))
    }
    return parts.joinToString(" · ")
}
