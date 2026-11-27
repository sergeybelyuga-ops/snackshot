package app.snackshot.ui.goals

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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.snackshot.domain.nutrition.Atwater
import app.snackshot.domain.nutrition.NutritionGoals
import app.snackshot.ui.R
import app.snackshot.ui.home.formatInt
import app.snackshot.ui.state.LoadingState
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

/** Settings → Nutrition preferences (S-16, prototype `s16_nutrition_preferences`). */
@Composable
fun NutritionPreferencesScreen(
    goals: NutritionGoals?,
    onGoal: (GoalKind) -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    SubScreen(stringResource(R.string.goals_title), onBack, modifier) {
        if (goals == null) {
            LoadingState()
            return@SubScreen
        }
        Text(
            stringResource(R.string.goals_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(stringResource(R.string.goals_daily), style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
        Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
            Column {
                val calories = goals.calories
                GoalRow(
                    title = stringResource(R.string.goals_calories),
                    value = calories?.let { stringResource(R.string.goals_value_kcal, formatInt(it.targetKcal)) },
                    support = calories?.let { stringResource(R.string.goals_range_kcal, formatInt(it.minKcal), formatInt(it.maxKcal)) },
                    accent = null,
                    onClick = { onGoal(GoalKind.CALORIES) },
                )
                MacroGoalRow(R.string.macro_carbs, goals.carbsGrams, SnackshotTheme.colors.macroCarbs) { onGoal(GoalKind.CARBS) }
                MacroGoalRow(R.string.macro_fat, goals.fatGrams, SnackshotTheme.colors.macroFat) { onGoal(GoalKind.FAT) }
                MacroGoalRow(R.string.macro_protein, goals.proteinGrams, SnackshotTheme.colors.macroProtein) { onGoal(GoalKind.PROTEIN) }
            }
        }
        Text(
            stringResource(R.string.goals_band_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!goals.isEmpty) {
            TextButton(onClick = { confirmClear = true }, modifier = Modifier.heightIn(min = Size.minTouchTarget)) {
                Text(stringResource(R.string.goals_clear_all))
            }
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
                }) { Text(stringResource(R.string.goals_clear)) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun MacroGoalRow(@StringRes name: Int, grams: Int?, color: Color, onClick: () -> Unit) {
    GoalRow(
        title = stringResource(name),
        value = grams?.let { stringResource(R.string.goals_value_g, it) },
        support = grams?.let {
            val band = NutritionGoals.macroBand(it)
            stringResource(R.string.goals_range_g, band.first, band.last)
        },
        accent = color,
        onClick = onClick,
    )
}

@Composable
private fun GoalRow(title: String, value: String?, support: String?, accent: Color?, onClick: () -> Unit) {
    val notSet = stringResource(R.string.goals_not_set)
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(support ?: notSet) },
        leadingContent = {
            if (accent == null) {
                Icon(painterResource(R.drawable.ic_local_fire_department), contentDescription = null)
            } else {
                Box(Modifier.width(4.dp).height(32.dp).clip(RoundedCornerShape(2.dp)).background(accent))
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value != null) Text(value, style = MaterialTheme.typography.titleMedium)
                Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null)
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick).heightIn(min = Size.minTouchTarget),
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

@Composable
fun GoalDetailScreen(
    kind: GoalKind,
    state: GoalDetailState?,
    actions: GoalDetailActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SubScreen(
        title = stringResource(goalName(kind)),
        onBack = onBack,
        modifier = modifier,
        bottomBar = state?.let {
            {
                Column(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(Spacing.content),
                    verticalArrangement = Arrangement.spacedBy(Spacing.small),
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
            return@SubScreen
        }
        val d = state.draft
        if (kind == GoalKind.CALORIES) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Top) {
                ValueStepper(
                    caption = stringResource(R.string.goals_minimum),
                    value = formatInt(d.minKcal),
                    unit = stringResource(R.string.goals_kcal_per_day),
                    decrease = stringResource(R.string.goals_decrease_min),
                    increase = stringResource(R.string.goals_increase_min),
                    canDecrease = state.canDecreaseMin,
                    canIncrease = state.canIncreaseMin,
                    onDecrease = actions.decreaseMin,
                    onIncrease = actions.increaseMin,
                )
                ValueStepper(
                    caption = stringResource(R.string.goals_maximum),
                    value = formatInt(d.maxKcal),
                    unit = stringResource(R.string.goals_kcal_per_day),
                    decrease = stringResource(R.string.goals_decrease_max),
                    increase = stringResource(R.string.goals_increase_max),
                    canDecrease = state.canDecreaseMax,
                    canIncrease = state.canIncreaseMax,
                    onDecrease = actions.decreaseMax,
                    onIncrease = actions.increaseMax,
                )
            }
            val target = ((d.minKcal + d.maxKcal) / 2.0).roundToInt()
            Text(
                stringResource(R.string.goals_target, formatInt(target)),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            About(stringResource(R.string.goals_about_calories))
        } else {
            val name = stringResource(goalName(kind))
            ValueStepper(
                caption = null,
                value = d.grams.toString(),
                unit = stringResource(R.string.goals_g_per_day),
                decrease = stringResource(R.string.goals_decrease_grams, name),
                increase = stringResource(R.string.goals_increase_grams, name),
                canDecrease = state.canDecreaseGrams,
                canIncrease = state.canIncreaseGrams,
                onDecrease = actions.decreaseGrams,
                onIncrease = actions.increaseGrams,
                modifier = Modifier.fillMaxWidth(),
            )
            val band = NutritionGoals.macroBand(d.grams)
            Text(
                stringResource(R.string.goals_shown_as, band.first, band.last),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            val kcal = (d.grams * kcalPerGram(kind)).roundToInt()
            val target = state.calorieTargetKcal
            About(
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
            )
        }
    }
}

@Composable
private fun About(text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small), modifier = Modifier.padding(top = Spacing.content)) {
        Text(stringResource(R.string.goals_about), style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
        Text(text, style = MaterialTheme.typography.bodyMedium)
        Text(
            stringResource(R.string.goals_advice),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ValueStepper(
    caption: String?,
    value: String,
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
        caption?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Text(
            value,
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = listOfNotNull(caption, "$value $unit").joinToString(" ")
            },
        )
        Text(unit, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.medium)) {
            StepButton(R.drawable.ic_remove, decrease, canDecrease, onDecrease)
            StepButton(R.drawable.ic_add, increase, canIncrease, onIncrease)
        }
    }
}

/** A stepper button that repeats while held. */
@Composable
private fun StepButton(icon: Int, description: String, enabled: Boolean, onStep: () -> Unit) {
    val step by rememberUpdatedState(onStep)
    val active by rememberUpdatedState(enabled)
    val scope = rememberCoroutineScope()
    FilledTonalIconButton(
        onClick = onStep,
        enabled = enabled,
        modifier = Modifier
            .size(Size.minTouchTarget)
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
            },
    ) { Icon(painterResource(icon), contentDescription = description) }
}

// ---------------------------------------------------------------- Shared

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubScreen(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
        bottomBar = { bottomBar?.invoke() },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.content),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) { content() }
    }
}

@StringRes
private fun goalName(kind: GoalKind): Int = when (kind) {
    GoalKind.CALORIES -> R.string.goals_calories
    GoalKind.CARBS -> R.string.macro_carbs
    GoalKind.FAT -> R.string.macro_fat
    GoalKind.PROTEIN -> R.string.macro_protein
}

private fun kcalPerGram(kind: GoalKind): Double = when (kind) {
    GoalKind.CARBS -> Atwater.CARBS_KCAL_PER_GRAM
    GoalKind.FAT -> Atwater.FAT_KCAL_PER_GRAM
    GoalKind.PROTEIN -> Atwater.PROTEIN_KCAL_PER_GRAM
    GoalKind.CALORIES -> 0.0
}

private const val REPEAT_START_MS = 400L
private const val REPEAT_EVERY_MS = 60L
