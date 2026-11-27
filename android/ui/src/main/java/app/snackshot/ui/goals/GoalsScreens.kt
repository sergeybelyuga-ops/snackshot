package app.snackshot.ui.goals

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.snackshot.domain.nutrition.Atwater
import app.snackshot.domain.nutrition.NutritionGoals
import app.snackshot.ui.R
import app.snackshot.ui.home.formatInt
import app.snackshot.ui.settings.AccentBar
import app.snackshot.ui.settings.Chevron
import app.snackshot.ui.settings.Footnote
import app.snackshot.ui.settings.IntroText
import app.snackshot.ui.settings.ListGroup
import app.snackshot.ui.settings.ListRow
import app.snackshot.ui.settings.RowIcon
import app.snackshot.ui.settings.RowValue
import app.snackshot.ui.settings.SectionLabel
import app.snackshot.ui.settings.SettingsPage
import app.snackshot.ui.settings.emphasize
import app.snackshot.ui.state.LoadingState
import app.snackshot.ui.theme.GoogleSansFontFamily
import app.snackshot.ui.theme.Size
import app.snackshot.ui.theme.SnackshotTheme
import app.snackshot.ui.theme.Spacing
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------------------------------------------------------------- Nutrition preferences

@Composable
fun NutritionPreferencesRoute(
    viewModel: NutritionPreferencesViewModel,
    onGoal: (GoalKind) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    NutritionPreferencesScreen(goals, onGoal, onClearAll = viewModel::clearAll, onBack = onBack, modifier = modifier)
}

/** Settings → Nutrition preferences (S-16, prototype `s16_settings.html`, `view-nutrition`). */
@Composable
fun NutritionPreferencesScreen(
    goals: NutritionGoals?,
    onGoal: (GoalKind) -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    SettingsPage(stringResource(R.string.goals_title), onBack, modifier) {
        if (goals == null) {
            LoadingState()
            return@SettingsPage
        }
        IntroText(stringResource(R.string.goals_intro))
        SectionLabel(stringResource(R.string.goals_daily))
        val colors = SnackshotTheme.colors
        ListGroup(count = 4) { i, shape ->
            when (i) {
                0 -> {
                    val calories = goals.calories
                    GoalRow(
                        title = stringResource(R.string.goals_calories),
                        value = calories?.let { formatInt(it.targetKcal) },
                        unit = stringResource(R.string.unit_kcal),
                        support = calories?.let { stringResource(R.string.goals_range_kcal, formatInt(it.minKcal), formatInt(it.maxKcal)) },
                        shape = shape,
                        leading = { RowIcon(R.drawable.ic_local_fire_department) },
                        onClick = { onGoal(GoalKind.CALORIES) },
                    )
                }
                1 -> MacroGoalRow(R.string.macro_carbs, goals.carbsGrams, colors.macroCarbs, shape) { onGoal(GoalKind.CARBS) }
                2 -> MacroGoalRow(R.string.macro_fat, goals.fatGrams, colors.macroFat, shape) { onGoal(GoalKind.FAT) }
                else -> MacroGoalRow(R.string.macro_protein, goals.proteinGrams, colors.macroProtein, shape) { onGoal(GoalKind.PROTEIN) }
            }
        }
        Footnote(R.drawable.ic_info, stringResource(R.string.goals_band_note))
        if (!goals.isEmpty) {
            TextButton(
                onClick = { confirmClear = true },
                modifier = Modifier.heightIn(min = Size.minTouchTarget),
            ) { Text(stringResource(R.string.goals_clear_all)) }
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.goals_clear_all_title)) },
            text = { Text(stringResource(R.string.goals_clear_all_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onClearAll()
                }) { Text(stringResource(R.string.goals_clear), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun MacroGoalRow(@StringRes name: Int, grams: Int?, color: Color, shape: Shape, onClick: () -> Unit) {
    GoalRow(
        title = stringResource(name),
        value = grams?.toString(),
        unit = stringResource(R.string.unit_g),
        support = grams?.let {
            val band = NutritionGoals.macroBand(it)
            stringResource(R.string.goals_range_g, band.first, band.last)
        },
        shape = shape,
        leading = { AccentBar(color) },
        onClick = onClick,
    )
}

@Composable
private fun GoalRow(
    title: String,
    value: String?,
    unit: String,
    support: String?,
    shape: Shape,
    leading: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    ListRow(
        title = title,
        support = support ?: stringResource(R.string.goals_not_set),
        shape = shape,
        onClick = onClick,
        leading = leading,
        trailing = {
            if (value != null) RowValue(value, unit)
            Chevron()
        },
    )
}

// ---------------------------------------------------------------- Goal detail

@Composable
fun GoalDetailRoute(viewModel: GoalDetailViewModel, onDone: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val done by viewModel.done.collectAsStateWithLifecycle()
    LaunchedEffect(done) { if (done) onDone() }
    GoalDetailScreen(
        kind = viewModel.kind,
        state = state,
        actions = GoalDetailActions(
            decreaseMin = viewModel::decreaseMin,
            increaseMin = viewModel::increaseMin,
            decreaseMax = viewModel::decreaseMax,
            increaseMax = viewModel::increaseMax,
            decreaseGrams = viewModel::decreaseGrams,
            increaseGrams = viewModel::increaseGrams,
            setGoal = viewModel::setGoal,
            clearGoal = viewModel::clearGoal,
        ),
        onBack = onBack,
        modifier = modifier,
    )
}

class GoalDetailActions(
    val decreaseMin: () -> Unit = {},
    val increaseMin: () -> Unit = {},
    val decreaseMax: () -> Unit = {},
    val increaseMax: () -> Unit = {},
    val decreaseGrams: () -> Unit = {},
    val increaseGrams: () -> Unit = {},
    val setGoal: () -> Unit = {},
    val clearGoal: () -> Unit = {},
)

/** One goal with steppers (prototype `s16_settings.html`, `goal-detail-view`). */
@Composable
fun GoalDetailScreen(
    kind: GoalKind,
    state: GoalDetailState?,
    actions: GoalDetailActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsPage(
        title = stringResource(goalName(kind)),
        onBack = onBack,
        modifier = modifier,
        bottomBar = state?.let {
            {
                Column(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Spacing.content, vertical = Spacing.small),
                    verticalArrangement = Arrangement.spacedBy(Spacing.grid),
                ) {
                    Button(onClick = actions.setGoal, modifier = Modifier.fillMaxWidth().heightIn(min = Size.minTouchTarget)) {
                        Text(stringResource(R.string.goals_set))
                    }
                    if (it.isSet) {
                        TextButton(onClick = actions.clearGoal, modifier = Modifier.fillMaxWidth().heightIn(min = Size.minTouchTarget)) {
                            Text(stringResource(R.string.goals_clear))
                        }
                    }
                }
            }
        },
    ) {
        if (state == null) {
            LoadingState()
            return@SettingsPage
        }
        val d = state.draft
        if (kind == GoalKind.CALORIES) {
            GoalMain {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                ) {
                    ValueStepper(
                        caption = stringResource(R.string.goals_minimum),
                        value = formatInt(d.minKcal),
                        valueSize = 44.sp,
                        unit = stringResource(R.string.goals_kcal_per_day),
                        decrease = stringResource(R.string.goals_decrease_min),
                        increase = stringResource(R.string.goals_increase_min),
                        canDecrease = state.canDecreaseMin,
                        canIncrease = state.canIncreaseMin,
                        onDecrease = actions.decreaseMin,
                        onIncrease = actions.increaseMin,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "–",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Normal),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 30.dp),
                    )
                    ValueStepper(
                        caption = stringResource(R.string.goals_maximum),
                        value = formatInt(d.maxKcal),
                        valueSize = 44.sp,
                        unit = stringResource(R.string.goals_kcal_per_day),
                        decrease = stringResource(R.string.goals_decrease_max),
                        increase = stringResource(R.string.goals_increase_max),
                        canDecrease = state.canDecreaseMax,
                        canIncrease = state.canIncreaseMax,
                        onDecrease = actions.decreaseMax,
                        onIncrease = actions.increaseMax,
                        modifier = Modifier.weight(1f),
                    )
                }
                val target = formatInt(((d.minKcal + d.maxKcal) / 2.0).roundToInt())
                TargetChip(stringResource(R.string.goals_target, target), stringResource(R.string.goals_value_kcal, target))
            }
            AboutCard(stringResource(R.string.goals_about_calories))
        } else {
            val name = stringResource(goalName(kind))
            GoalMain {
                Box(Modifier.width(56.dp).height(8.dp).clip(RoundedCornerShape(4.dp)).background(macroColor(kind)))
                ValueStepper(
                    caption = null,
                    value = d.grams.toString(),
                    valueSize = 57.sp,
                    unit = stringResource(R.string.goals_g_per_day),
                    decrease = stringResource(R.string.goals_decrease_grams, name.lowercase()),
                    increase = stringResource(R.string.goals_increase_grams, name.lowercase()),
                    canDecrease = state.canDecreaseGrams,
                    canIncrease = state.canIncreaseGrams,
                    onDecrease = actions.decreaseGrams,
                    onIncrease = actions.increaseGrams,
                    modifier = Modifier.fillMaxWidth(),
                )
                val band = NutritionGoals.macroBand(d.grams)
                TargetChip(
                    stringResource(R.string.goals_shown_as, band.first, band.last),
                    stringResource(R.string.goals_band_g, band.first, band.last),
                )
            }
            val kcal = (d.grams * kcalPerGram(kind)).roundToInt()
            val target = state.calorieTargetKcal
            AboutCard(
                if (target != null) {
                    stringResource(
                        R.string.goals_about_macro_share,
                        d.grams,
                        name.lowercase(),
                        formatInt(kcal),
                        (kcal * 100.0 / target).roundToInt(),
                        formatInt(target),
                    )
                } else {
                    stringResource(R.string.goals_about_macro, d.grams, name.lowercase(), formatInt(kcal))
                },
                note = stringResource(macroNote(kind)),
            )
        }
    }
}

/** The centered value block: 36 dp above, 28 dp below. */
@Composable
private fun GoalMain(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 36.dp - Spacing.content, bottom = 28.dp - Spacing.content),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) { content() }
}

@Composable
private fun TargetChip(text: String, emphasized: String) {
    Text(
        emphasize(text, emphasized, MaterialTheme.colorScheme.onSurface),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .padding(top = Spacing.small)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}

@Composable
private fun AboutCard(text: String, note: String? = null) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(R.string.goals_about),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics {
                    heading()
                },
            )
            val body = MaterialTheme.typography.bodyMedium
            val variant = MaterialTheme.colorScheme.onSurfaceVariant
            Text(listOfNotNull(text, note).joinToString(" "), style = body, color = variant)
            Text(stringResource(R.string.goals_advice), style = body, color = variant)
        }
    }
}

@Composable
private fun ValueStepper(
    caption: String?,
    value: String,
    valueSize: TextUnit,
    unit: String,
    decrease: String,
    increase: String,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        caption?.let {
            Text(
                it.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            value,
            style = MaterialTheme.typography.displayLarge.copy(
                fontFamily = GoogleSansFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = valueSize,
                lineHeight = valueSize * 1.15f,
            ),
            maxLines = 1,
            modifier = Modifier.semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = listOfNotNull(caption, "$value $unit").joinToString(" ")
            },
        )
        Text(unit, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        // Segmented −/+ pair: 56 dp tall, 2 dp apart, rounded on the outside.
        Row(Modifier.fillMaxWidth().height(56.dp).padding(top = 0.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            StepButton(
                R.drawable.ic_remove,
                decrease,
                canDecrease,
                onDecrease,
                RoundedCornerShape(16.dp, 4.dp, 4.dp, 16.dp),
                Modifier.weight(1f),
            )
            StepButton(
                R.drawable.ic_add,
                increase,
                canIncrease,
                onIncrease,
                RoundedCornerShape(4.dp, 16.dp, 16.dp, 4.dp),
                Modifier.weight(1f),
            )
        }
    }
}

/** One half of the stepper; repeats while held. */
@Composable
private fun StepButton(
    @DrawableRes icon: Int,
    description: String,
    enabled: Boolean,
    onStep: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    val step by rememberUpdatedState(onStep)
    val active by rememberUpdatedState(enabled)
    val scope = rememberCoroutineScope()
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    val repeat = scope.launch {
                        delay(REPEAT_START_MS)
                        while (active) {
                            step()
                            delay(REPEAT_EVERY_MS)
                        }
                    }
                    waitForUpOrCancellation()
                    repeat.cancel()
                }
            }
            .clickable(enabled = enabled, role = Role.Button, onClick = onStep)
            .semantics { contentDescription = description }
            .alpha(if (enabled) 1f else 0.4f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
    }
}

// ---------------------------------------------------------------- Shared

@StringRes
private fun goalName(kind: GoalKind): Int = when (kind) {
    GoalKind.CALORIES -> R.string.goals_calories
    GoalKind.CARBS -> R.string.macro_carbs
    GoalKind.FAT -> R.string.macro_fat
    GoalKind.PROTEIN -> R.string.macro_protein
}

@StringRes
private fun macroNote(kind: GoalKind): Int = when (kind) {
    GoalKind.CARBS -> R.string.goals_note_carbs
    GoalKind.FAT -> R.string.goals_note_fat
    else -> R.string.goals_note_protein
}

@Composable
private fun macroColor(kind: GoalKind): Color = when (kind) {
    GoalKind.CARBS -> SnackshotTheme.colors.macroCarbs
    GoalKind.FAT -> SnackshotTheme.colors.macroFat
    else -> SnackshotTheme.colors.macroProtein
}

private fun kcalPerGram(kind: GoalKind): Double = when (kind) {
    GoalKind.CARBS -> Atwater.CARBS_KCAL_PER_GRAM
    GoalKind.FAT -> Atwater.FAT_KCAL_PER_GRAM
    GoalKind.PROTEIN -> Atwater.PROTEIN_KCAL_PER_GRAM
    GoalKind.CALORIES -> 0.0
}

private const val REPEAT_START_MS = 400L
private const val REPEAT_EVERY_MS = 60L
