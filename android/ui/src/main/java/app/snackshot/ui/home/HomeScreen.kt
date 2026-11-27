package app.snackshot.ui.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.snackshot.domain.meal.MealId
import app.snackshot.domain.meal.MealSource
import app.snackshot.domain.meal.MealType
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
import kotlin.math.roundToInt

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
