package app.snackshot.ui.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GeometrySize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.snackshot.domain.meal.MealId
import app.snackshot.domain.meal.MealSource
import app.snackshot.domain.meal.MealType
import app.snackshot.domain.nutrition.NutritionGoals
import app.snackshot.domain.nutrition.NutritionTotals
import app.snackshot.ui.R
import app.snackshot.ui.state.ErrorState
import app.snackshot.ui.state.SnackshotSnackbarHost
import app.snackshot.ui.state.UiError
import app.snackshot.ui.state.UiState
import app.snackshot.ui.theme.Size
import app.snackshot.ui.theme.SnackshotTheme
import app.snackshot.ui.theme.Spacing
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.abs
import kotlin.math.roundToInt

/** The three ways to log a meal, in carousel order (doc 10 §4.4). */
enum class EntryAction(@StringRes val label: Int, @DrawableRes val icon: Int) {
    TAKE_PHOTO(R.string.home_entry_photo, R.drawable.ic_photo_camera),
    GALLERY(R.string.home_entry_gallery, R.drawable.ic_photo_library),
    MANUAL_ADD(R.string.home_entry_manual, R.drawable.ic_edit_note),
}

/** S-02 Home: today's totals, Recent meals and the meal-entry carousel (doc 10 §4.4, WBS 5.1). */
@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    savedMealId: MealId?,
    onSavedMealConsumed: () -> Unit,
    onManualAdd: () -> Unit,
    onOpenDashboard: () -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val checking by viewModel.checkingAccess.collectAsStateWithLifecycle()
    val highlighted by viewModel.highlighted.collectAsStateWithLifecycle()

    LaunchedEffect(savedMealId) {
        if (savedMealId != null) {
            viewModel.onMealSaved(savedMealId)
            onSavedMealConsumed()
        }
    }

    HomeScreen(
        state = state,
        highlighted = highlighted,
        message = message,
        checkingAccess = checking,
        onMessageShown = viewModel::messageShown,
        onRetry = viewModel::retry,
        onEntry = { action ->
            when (action) {
                EntryAction.TAKE_PHOTO -> viewModel.onAiAction(MealSource.PHOTO)
                EntryAction.GALLERY -> viewModel.onAiAction(MealSource.GALLERY)
                EntryAction.MANUAL_ADD -> onManualAdd()
            }
        },
        onManualAdd = onManualAdd,
        onOpenDashboard = onOpenDashboard,
        onOpenHistory = onOpenHistory,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: UiState<HomeContent>,
    highlighted: MealId?,
    message: HomeMessage?,
    checkingAccess: Boolean,
    onMessageShown: () -> Unit,
    onRetry: () -> Unit,
    onEntry: (EntryAction) -> Unit,
    onManualAdd: () -> Unit,
    onOpenDashboard: () -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbar = remember { SnackbarHostState() }
    val text = message?.let { messageText(it) }
    val quickAdd = stringResource(R.string.quick_add_title)
    LaunchedEffect(message) {
        if (message == null || text == null) return@LaunchedEffect
        val offerQuickAdd = message !is HomeMessage.MealSaved && message !is HomeMessage.AiReady
        val result = snackbar.showSnackbar(
            message = text,
            actionLabel = if (offerQuickAdd) quickAdd else null,
            duration = if (offerQuickAdd) SnackbarDuration.Long else SnackbarDuration.Short,
        )
        onMessageShown()
        if (result == SnackbarResult.ActionPerformed) onManualAdd()
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.brand_mark)) },
                actions = { SyncStatus(Modifier.padding(end = Spacing.content)) },
            )
        },
        snackbarHost = { SnackshotSnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (state) {
                is UiState.Error -> ErrorState(
                    error = UiError(stringResource(R.string.home_error), stringResource(R.string.home_error_detail)),
                    onRetry = onRetry,
                    fallbackLabel = quickAdd,
                    onFallback = onManualAdd,
                )
                else -> {
                    val content = (state as? UiState.Success)?.data
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = Spacing.content),
                        verticalArrangement = Arrangement.spacedBy(Spacing.section),
                    ) {
                        if (content == null) {
                            SummarySkeleton()
                        } else {
                            DailySummary(content.totals, content.goals, onClick = onOpenDashboard)
                        }
                        RecentMeals(content, highlighted, onOpenHistory)
                        // Keeps the last row clear of the entry dock.
                        Spacer(Modifier.height(DOCK_HEIGHT + Spacing.content))
                    }
                    EntryDock(
                        enabled = content != null,
                        busy = checkingAccess,
                        onAction = onEntry,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(Spacing.content),
                    )
                }
            }
        }
    }
}

@Composable
private fun messageText(message: HomeMessage): String = when (message) {
    is HomeMessage.MealSaved -> stringResource(R.string.home_meal_saved)
    is HomeMessage.AiReady -> {
        val ready = pluralStringResource(R.plurals.home_ai_ready, message.picturesLeft, message.picturesLeft)
        stringResource(if (message.source == MealSource.PHOTO) R.string.home_ai_camera_soon else R.string.home_ai_gallery_soon, ready)
    }
    HomeMessage.TrialEnded -> stringResource(R.string.home_ai_trial_ended)
    HomeMessage.Waitlisted -> stringResource(R.string.home_ai_waitlisted)
    HomeMessage.AiPaused -> stringResource(R.string.home_ai_paused)
    HomeMessage.AiUnavailable -> stringResource(R.string.home_ai_unavailable)
    HomeMessage.Offline -> stringResource(R.string.home_ai_offline)
    HomeMessage.Busy -> stringResource(R.string.home_ai_busy)
    HomeMessage.ServiceError -> stringResource(R.string.home_ai_error)
}

/** Health Connect sync arrives with WBS 8; until then every meal is stored on this device only. */
@Composable
private fun SyncStatus(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = Spacing.small, vertical = Spacing.grid),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.grid),
    ) {
        Icon(painterResource(R.drawable.ic_smartphone), contentDescription = null, modifier = Modifier.size(18.dp))
        Text(stringResource(R.string.home_sync_local), style = MaterialTheme.typography.labelLarge)
    }
}

// ---------------------------------------------------------------- Daily summary

@Composable
private fun DailySummary(totals: NutritionTotals, goals: NutritionGoals, onClick: () -> Unit) {
    val kcal = totals.energyKcal.roundToInt()
    val target = goals.calories?.targetKcal
    val macros = listOf(
        MacroLine(R.string.macro_carbs, totals.carbsGrams, goals.carbsGrams, SnackshotTheme.colors.macroCarbs),
        MacroLine(R.string.macro_fat, totals.fatGrams, goals.fatGrams, SnackshotTheme.colors.macroFat),
        MacroLine(R.string.macro_protein, totals.proteinGrams, goals.proteinGrams, SnackshotTheme.colors.macroProtein),
    )
    val description = summaryDescription(kcal, target, macros)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClickLabel = stringResource(R.string.home_open_dashboard), onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
            }
            .padding(vertical = Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.content)) {
            CalorieRing(kcal, target)
            val sum = totals.carbsGrams + totals.fatGrams + totals.proteinGrams
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                macros.forEach { MacroRow(it, sum) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(caption(kcal, target), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private data class MacroLine(@StringRes val name: Int, val grams: Double, val goal: Int?, val color: Color)

@Composable
private fun CalorieRing(kcal: Int, target: Int?) {
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    val value = MaterialTheme.colorScheme.primary
    Box(Modifier.size(RING), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val inset = stroke / 2
            val arcSize = GeometrySize(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, useCenter = false, topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke))
            if (target != null && kcal > 0) {
                val sweep = 360f * (kcal.toFloat() / target).coerceIn(0f, 1f)
                // Starts at 6 o'clock and runs clockwise, as in the prototype.
                drawArc(
                    value,
                    90f,
                    sweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.home_today),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(formatInt(kcal), style = MaterialTheme.typography.headlineMedium)
            Text(
                if (target != null) stringResource(R.string.home_of_kcal, formatInt(target)) else stringResource(R.string.unit_kcal),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MacroRow(line: MacroLine, sumGrams: Double) {
    val grams = line.grams.roundToInt()
    val name = stringResource(line.name)
    val (label, fraction, over) = if (line.goal != null && line.goal > 0) {
        val pct = (line.grams / line.goal * 100).roundToInt()
        val over = line.grams > line.goal
        Triple(
            stringResource(if (over) R.string.home_macro_goal_over else R.string.home_macro_goal, name, pct, grams, line.goal),
            (
                line.grams /
                    line.goal
                ).toFloat(),
            over,
        )
    } else {
        val share = if (sumGrams > 0) (line.grams / sumGrams * 100).roundToInt() else 0
        Triple(
            stringResource(R.string.home_macro_share, name, share, grams),
            if (sumGrams >
                0
            ) {
                (line.grams / sumGrams).toFloat()
            } else {
                0f
            },
            false,
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.grid)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(SnackshotTheme.colors.macroTrack),
        ) {
            if (grams > 0) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction.coerceIn(0.08f, 1f))
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(line.color),
                )
            }
        }
    }
}

@Composable
private fun caption(kcal: Int, target: Int?) = buildAnnotatedString {
    if (target == null) {
        append(stringResource(R.string.home_no_goal))
        return@buildAnnotatedString
    }
    val left = target - kcal
    val amount = stringResource(R.string.home_kcal_amount, formatInt(abs(left)))
    val template = stringResource(if (left >= 0) R.string.home_kcal_left else R.string.home_kcal_over, amount)
    val start = template.indexOf(amount)
    append(template)
    if (start >= 0) addStyle(SpanStyle(fontWeight = FontWeight.SemiBold), start, start + amount.length)
}

@Composable
private fun summaryDescription(kcal: Int, target: Int?, macros: List<MacroLine>): String {
    val parts = macros.map { m ->
        val name = stringResource(m.name)
        if (m.goal != null) {
            stringResource(R.string.home_a11y_macro_goal, name, m.grams.roundToInt(), m.goal)
        } else {
            stringResource(R.string.home_a11y_macro, name, m.grams.roundToInt())
        }
    }.joinToString(", ")
    return if (target != null) {
        stringResource(
            R.string.home_a11y_summary_goal,
            formatInt(kcal),
            formatInt(target),
            formatInt((target - kcal).coerceAtLeast(0)),
            parts,
        )
    } else {
        stringResource(R.string.home_a11y_summary, formatInt(kcal), parts)
    }
}

@Composable
private fun SummarySkeleton() {
    val label = stringResource(R.string.state_loading)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.medium)
            .semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.content),
    ) {
        Box(Modifier.size(RING).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
            repeat(3) {
                Box(
                    Modifier.fillMaxWidth(
                        0.7f,
                    ).height(14.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh),
                )
                Box(
                    Modifier.fillMaxWidth().height(
                        14.dp,
                    ).clip(RoundedCornerShape(7.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh),
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Recent meals

@Composable
private fun RecentMeals(content: HomeContent?, highlighted: MealId?, onOpenHistory: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.home_recent_meals),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            TextButton(onClick = onOpenHistory, modifier = Modifier.heightIn(min = Size.minTouchTarget)) {
                Text(stringResource(R.string.home_see_all))
                Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null)
            }
        }
        when {
            content == null -> repeat(2) { MealRowSkeleton() }
            content.isEmpty -> EmptyMeals()
            else -> {
                if (content.today.isNotEmpty()) {
                    DayLabel(stringResource(R.string.home_today))
                    content.today.forEach { MealRow(it, it.id == highlighted) }
                }
                if (content.yesterday.isNotEmpty()) {
                    DayLabel(stringResource(R.string.home_yesterday))
                    content.yesterday.forEach { MealRow(it, it.id == highlighted) }
                }
            }
        }
    }
}

@Composable
private fun DayLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Spacing.small).semantics { heading() },
    )
}

@Composable
private fun EmptyMeals() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium),
    ) {
        Column(Modifier.padding(Spacing.content), verticalArrangement = Arrangement.spacedBy(Spacing.grid)) {
            Text(stringResource(R.string.home_empty_title), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.home_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MealRow(meal: HomeMeal, highlighted: Boolean) {
    val name = meal.name.ifBlank { stringResource(R.string.quick_add_default_name) }
    val type = stringResource(mealTypeLabel(meal.mealType))
    val time = meal.time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    val kcal = meal.energyKcal.roundToInt()
    val carbs = meal.values.carbsGrams?.roundToInt()
    val fat = meal.values.fatGrams?.roundToInt()
    val protein = meal.values.proteinGrams?.roundToInt()
    val description = stringResource(R.string.home_a11y_meal, name, type, time, kcal, protein ?: 0, carbs ?: 0, fat ?: 0)
    val container = if (highlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
    val outline = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(container)
            .border(1.dp, outline, MaterialTheme.shapes.medium)
            .clearAndSetSemantics { contentDescription = description }
            .padding(horizontal = Spacing.medium, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(sourceIcon(meal.source)), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(R.string.home_meal_meta, type, time),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MacroDots(carbs, fat, protein)
        }
        Text(stringResource(R.string.home_meal_kcal, formatInt(kcal)), style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun MacroDots(carbs: Int?, fat: Int?, protein: Int?) {
    val colors = SnackshotTheme.colors
    val dash = stringResource(R.string.value_unknown)
    Text(
        buildAnnotatedString {
            listOf(
                Triple(R.string.macro_carbs, carbs, colors.macroCarbs),
                Triple(R.string.macro_fat, fat, colors.macroFat),
                Triple(R.string.macro_protein, protein, colors.macroProtein),
            ).forEachIndexed { i, (name, grams, color) ->
                if (i > 0) append("   ")
                withStyle(SpanStyle(color = color)) { append("● ") }
                append(stringResource(name))
                append(" ")
                append(grams?.toString() ?: dash)
            }
            append(" ")
            append(stringResource(R.string.unit_g))
        },
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun MealRowSkeleton() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    )
}

// ---------------------------------------------------------------- Meal-entry carousel

/**
 * The meal-entry carousel (doc 10 §4.4): a vertical, icon-only dock in the thumb zone. Only the
 * centered action is enabled; the previous one is shown above it and the next one below, smaller and
 * not interactive. Swipe up or down to rotate; TalkBack users get "previous" and "next" actions.
 */
@Composable
fun EntryDock(enabled: Boolean, busy: Boolean, onAction: (EntryAction) -> Unit, modifier: Modifier = Modifier) {
    val actions = EntryAction.entries
    var index by rememberSaveable { mutableIntStateOf(0) }
    val current = actions[index]
    val previous = actions[(index - 1 + actions.size) % actions.size]
    val next = actions[(index + 1) % actions.size]
    val threshold = with(LocalDensity.current) { 24.dp.toPx() }
    var drag by remember { mutableFloatStateOf(0f) }

    val label = stringResource(current.label)
    val toPrevious = stringResource(R.string.home_entry_switch_to, stringResource(previous.label))
    val toNext = stringResource(R.string.home_entry_switch_to, stringResource(next.label))
    val hint = stringResource(R.string.home_entry_hint)

    Column(
        modifier = modifier
            .width(DOCK_WIDTH)
            .height(DOCK_HEIGHT)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { drag = 0f },
                    onVerticalDrag = { change, amount ->
                        change.consume()
                        drag += amount
                        if (abs(drag) > threshold) {
                            // Dragging up brings the next action to the center.
                            index = if (drag < 0) (index + 1) % actions.size else (index - 1 + actions.size) % actions.size
                            drag = 0f
                        }
                    },
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        EntryPreview(previous)
        Surface(
            onClick = { onAction(current) },
            enabled = enabled && !busy,
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shadowElevation = 6.dp,
            modifier = Modifier
                .size(60.dp)
                .semantics {
                    contentDescription = "$label. $hint"
                    customActions = listOf(
                        CustomAccessibilityAction(toPrevious) {
                            index = (index - 1 + actions.size) % actions.size
                            true
                        },
                        CustomAccessibilityAction(toNext) {
                            index = (index + 1) % actions.size
                            true
                        },
                    )
                },
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(28.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 3.dp)
                } else {
                    Icon(painterResource(current.icon), contentDescription = null, modifier = Modifier.size(28.dp))
                }
            }
        }
        EntryPreview(next)
    }
}

@Composable
private fun EntryPreview(action: EntryAction) {
    Box(
        Modifier
            .size(40.dp)
            .alpha(0.6f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(action.icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
    }
}

// ---------------------------------------------------------------- Shared helpers

@StringRes
fun mealTypeLabel(type: MealType): Int = when (type) {
    MealType.BREAKFAST -> R.string.meal_type_breakfast
    MealType.LUNCH -> R.string.meal_type_lunch
    MealType.DINNER -> R.string.meal_type_dinner
    MealType.SNACK -> R.string.meal_type_snack
}

@DrawableRes
private fun sourceIcon(source: MealSource): Int = when (source) {
    MealSource.PHOTO -> R.drawable.ic_photo_camera
    MealSource.GALLERY -> R.drawable.ic_photo_library
    MealSource.QUICK_ADD -> R.drawable.ic_edit_note
}

internal fun formatInt(value: Int): String = NumberFormat.getIntegerInstance().format(value)

private val RING = 136.dp
private val DOCK_WIDTH = 64.dp
private val DOCK_HEIGHT = 168.dp
