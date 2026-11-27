package app.snackshot.ui.review

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.snackshot.domain.meal.MealDraft
import app.snackshot.domain.meal.MealId
import app.snackshot.domain.meal.MealSource
import app.snackshot.domain.meal.MealType
import app.snackshot.domain.nutrition.NutrientField
import app.snackshot.domain.nutrition.NutritionValues
import app.snackshot.ui.R
import app.snackshot.ui.home.formatInt
import app.snackshot.ui.home.mealTypeLabel
import app.snackshot.ui.meal.MealPhoto
import app.snackshot.ui.theme.GoogleSansFontFamily
import app.snackshot.ui.theme.Size
import app.snackshot.ui.theme.SnackshotTheme
import app.snackshot.ui.theme.Spacing
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

@Composable
fun MealReviewRoute(
    viewModel: MealReviewViewModel,
    onSaved: (MealId) -> Unit,
    onClosed: () -> Unit,
    onRetake: () -> Unit,
    onQuickAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { state.saved?.let(onSaved) }
    LaunchedEffect(state.closed) { if (state.closed) onClosed() }
    MealReviewScreen(
        state = state,
        zone = viewModel.zoneId,
        actions = ReviewActions(
            onGrams = viewModel::onGrams,
            onStep = viewModel::stepGrams,
            onMealType = viewModel::onMealType,
            onMissingValue = viewModel::onMissingValue,
            onApplyEdit = viewModel::applyEdit,
            onReset = viewModel::reset,
            onSave = viewModel::save,
            onDiscard = viewModel::discard,
            onRetake = {
                viewModel.discard()
                onRetake()
            },
            onQuickAdd = onQuickAdd,
            onBack = onClosed,
        ),
        modifier = modifier,
    )
}

class ReviewActions(
    val onGrams: (String) -> Unit = {},
    val onStep: (up: Boolean) -> Unit = {},
    val onMealType: (MealType) -> Unit = {},
    val onMissingValue: (NutrientField, String) -> Unit = { _, _ -> },
    val onApplyEdit: (NutritionEdit) -> Boolean = { true },
    val onReset: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onDiscard: () -> Unit = {},
    val onRetake: () -> Unit = {},
    val onQuickAdd: () -> Unit = {},
    val onBack: () -> Unit = {},
)

/** S-15 Meal review (prototype `s15_meal_review.html`, doc 10 §4.7) for a new photo or gallery result. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealReviewScreen(state: ReviewState, zone: ZoneId, actions: ReviewActions, modifier: Modifier = Modifier) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val draft = state.draft

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = { editing = true }, enabled = draft != null) {
                        Icon(painterResource(R.drawable.ic_edit), contentDescription = stringResource(R.string.review_edit_values))
                    }
                },
            )
        },
        bottomBar = {
            BottomActions(state, onSave = actions.onSave, onRetake = actions.onRetake, onQuickAdd = actions.onQuickAdd, onDiscard = {
                confirmDiscard = true
            })
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.content, vertical = Spacing.small),
            verticalArrangement = Arrangement.spacedBy(Spacing.content),
        ) {
            MealPhoto(
                file = state.image,
                contentDescription = stringResource(R.string.review_photo),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(184.dp),
            )
            if (draft != null) {
                TitleBlock(draft, state, zone)
                Hint(state, onQuickAdd = actions.onQuickAdd)
                Values(draft, state, actions, onEdit = { editing = true })
                MacroSplit(draft, state, actions.onMissingValue)
            }
        }
    }

    if (editing && draft != null) {
        EditNutritionSheet(
            draft = draft,
            zone = zone,
            onApply = { edit -> actions.onApplyEdit(edit).also { ok -> if (ok) editing = false } },
            onReset = actions.onReset,
            onDismiss = { editing = false },
        )
    }
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.review_discard_title)) },
            text = { Text(stringResource(R.string.review_discard_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    actions.onDiscard()
                }) { Text(stringResource(R.string.review_discard), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun TitleBlock(draft: MealDraft, state: ReviewState, zone: ZoneId) {
    var info by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        Text(
            draft.name.ifBlank { draft.originalName },
            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = GoogleSansFontFamily),
            modifier = Modifier.semantics { heading() },
        )
        Text(metaLine(draft, zone), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val percent = state.confidencePercent ?: return@Column
        val low = state.lowConfidence
        Surface(
            onClick = { info = !info },
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, if (low) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline),
            color = if (low) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f) else Color.Transparent,
        ) {
            Row(
                Modifier.heightIn(min = 32.dp).padding(horizontal = Spacing.small),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                Icon(
                    painterResource(if (low) R.drawable.ic_error else R.drawable.ic_auto_awesome),
                    contentDescription = null,
                    tint = if (low) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    stringResource(if (low) R.string.review_confidence_low else R.string.review_confidence, percent),
                    style = MaterialTheme.typography.labelLarge,
                )
                Icon(painterResource(R.drawable.ic_expand_more), contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
        if (info) {
            Text(
                stringResource(R.string.review_confidence_info),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun metaLine(draft: MealDraft, zone: ZoneId): String {
    val at = draft.eatenAt.atZone(zone)
    val today = LocalDate.now(zone)
    val day = when (at.toLocalDate()) {
        today -> stringResource(R.string.review_today)
        today.minusDays(1) -> stringResource(R.string.review_yesterday)
        else -> at.toLocalDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }
    val time = at.toLocalTime().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    val from = stringResource(if (draft.source == MealSource.GALLERY) R.string.review_from_gallery else R.string.review_from_photo)
    return stringResource(R.string.review_meta, day, time, from)
}

/** Low confidence or a partial result: a note above the values (doc 10 §4.7). */
@Composable
private fun Hint(state: ReviewState, onQuickAdd: () -> Unit) {
    val partial = state.missingFields.isNotEmpty()
    if (!partial && !state.lowConfidence) return
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (partial) scheme.errorContainer.copy(alpha = 0.55f) else scheme.surfaceContainer,
    ) {
        Column(Modifier.fillMaxWidth().padding(Spacing.content), verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.medium), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(if (partial) R.drawable.ic_error else R.drawable.ic_fact_check),
                    contentDescription = null,
                    tint = if (partial) scheme.error else scheme.primary,
                )
                Text(
                    stringResource(if (partial) R.string.review_partial_title else R.string.review_low_title),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Text(
                if (partial) {
                    stringResource(R.string.review_partial_text, missingNames(state.missingFields))
                } else {
                    stringResource(R.string.review_low_text)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
            if (!partial) {
                TextButton(onClick = onQuickAdd, modifier = Modifier.heightIn(min = Size.minTouchTarget)) {
                    Icon(painterResource(R.drawable.ic_bolt), contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.recognition_quick_add), modifier = Modifier.padding(start = Spacing.small))
                }
            }
        }
    }
}

@Composable
private fun missingNames(fields: Set<NutrientField>): String = NutrientField.entries.filter { it in fields }.map { field ->
    stringResource(
        when (field) {
            NutrientField.ENERGY -> R.string.review_missing_calories
            NutrientField.CARBS -> R.string.review_missing_carbs
            NutrientField.FAT -> R.string.review_missing_fat
            NutrientField.PROTEIN -> R.string.review_missing_protein
        },
    )
}.joinToString(", ")

@Composable
private fun Values(draft: MealDraft, state: ReviewState, actions: ReviewActions, onEdit: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        ValueRow(R.string.review_portion) {
            GramsStepper(state, actions)
        }
        if (state.gramsInvalid) {
            Text(
                stringResource(R.string.review_grams_error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (state.canReset) {
            TextButton(onClick = actions.onReset, modifier = Modifier.heightIn(min = Size.minTouchTarget)) {
                Icon(painterResource(R.drawable.ic_undo), contentDescription = null, modifier = Modifier.size(18.dp))
                Text(
                    stringResource(R.string.review_reset, formatInt((draft.basis?.grams ?: 0.0).roundToInt())),
                    modifier = Modifier.padding(start = Spacing.small),
                )
            }
        }
        ValueRow(R.string.review_calories) {
            val kcal = draft.values.energyKcal
            Text(
                if (kcal != null) bigValue(formatInt(kcal.roundToInt()), stringResource(R.string.unit_kcal)) else bigValue("–", ""),
                maxLines = 1,
            )
        }
        ValueRow(R.string.review_meal) { MealTypeMenu(draft.mealType, actions.onMealType) }
        OutlinedButton(onClick = onEdit, modifier = Modifier.heightIn(min = Size.minTouchTarget)) {
            Icon(painterResource(R.drawable.ic_tune), contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.review_edit_values), modifier = Modifier.padding(start = Spacing.small))
        }
    }
}

@Composable
private fun ValueRow(@StringRes label: Int, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(label),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        content()
    }
}

@Composable
private fun bigValue(value: String, unit: String) = buildAnnotatedString {
    withStyle(SpanStyle(fontFamily = GoogleSansFontFamily, fontSize = 28.sp)) { append(value) }
    if (unit.isNotEmpty()) withStyle(SpanStyle(fontSize = 16.sp)) { append(" $unit") }
}

@Composable
private fun GramsStepper(state: ReviewState, actions: ReviewActions) {
    val grams = state.draft?.grams ?: 0.0
    val portion = stringResource(R.string.review_portion_a11y)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
        FilledTonalIconButton(
            onClick = { actions.onStep(false) },
            enabled = grams > MealDraft.MIN_GRAMS,
            modifier = Modifier.size(Size.minTouchTarget),
        ) {
            Icon(
                painterResource(R.drawable.ic_remove),
                contentDescription = stringResource(R.string.review_grams_less, MealReviewViewModel.GRAMS_STEP.toInt()),
            )
        }
        OutlinedTextField(
            value = state.gramsText,
            onValueChange = actions.onGrams,
            singleLine = true,
            isError = state.gramsInvalid,
            suffix = { Text(stringResource(R.string.unit_g)) },
            textStyle = TextStyle(fontFamily = GoogleSansFontFamily, fontSize = 24.sp, textAlign = TextAlign.End),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(124.dp).semantics { contentDescription = portion },
        )
        FilledTonalIconButton(
            onClick = { actions.onStep(true) },
            enabled = grams < MealDraft.MAX_GRAMS,
            modifier = Modifier.size(Size.minTouchTarget),
        ) {
            Icon(
                painterResource(R.drawable.ic_add),
                contentDescription = stringResource(R.string.review_grams_more, MealReviewViewModel.GRAMS_STEP.toInt()),
            )
        }
    }
}

@Composable
private fun MealTypeMenu(type: MealType, onPick: (MealType) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    val label = stringResource(mealTypeLabel(type))
    val choose = stringResource(R.string.review_meal_choose, label)
    Box {
        Row(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { open = true }
                .heightIn(min = Size.minTouchTarget)
                .padding(start = Spacing.small)
                .semantics(mergeDescendants = true) {
                    contentDescription = choose
                    role = Role.DropdownList
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = TextStyle(fontFamily = GoogleSansFontFamily, fontSize = 28.sp), color = MaterialTheme.colorScheme.onSurface)
            Icon(
                painterResource(R.drawable.ic_arrow_drop_down),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            MealType.entries.forEach { t ->
                DropdownMenuItem(text = { Text(stringResource(mealTypeLabel(t))) }, onClick = {
                    open = false
                    onPick(t)
                })
            }
        }
    }
}

/** Each macro's share of the meal's calories (4/4/9 kcal per gram), with a track in its color. */
@Composable
private fun MacroSplit(draft: MealDraft, state: ReviewState, onMissing: (NutrientField, String) -> Unit) {
    val colors = SnackshotTheme.colors
    val v = draft.values
    val macros = listOf(
        Macro(NutrientField.CARBS, R.string.macro_carbs, v.carbsGrams, 4.0, colors.macroCarbs),
        Macro(NutrientField.FAT, R.string.macro_fat, v.fatGrams, 9.0, colors.macroFat),
        Macro(NutrientField.PROTEIN, R.string.macro_protein, v.proteinGrams, 4.0, colors.macroProtein),
    )
    val complete = macros.all { it.grams != null }
    val kcalSum = macros.sumOf { (it.grams ?: 0.0) * it.kcalPerGram }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.medium), modifier = Modifier.padding(top = Spacing.small)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            stringResource(R.string.review_macro_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics {
                heading()
            },
        )
        Text(
            stringResource(if (complete) R.string.review_macro_text else R.string.review_macro_text_missing),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        macros.forEach { m ->
            val missing = m.field in state.missingFields
            val share = if (complete && kcalSum > 0) ((m.grams ?: 0.0) * m.kcalPerGram / kcalSum * 100).roundToInt() else null
            MacroRow(m, share, missing, state.missingText[m.field].orEmpty(), onMissing)
        }
    }
}

private data class Macro(val field: NutrientField, @StringRes val name: Int, val grams: Double?, val kcalPerGram: Double, val color: Color)

@Composable
private fun MacroRow(m: Macro, share: Int?, missing: Boolean, typed: String, onMissing: (NutrientField, String) -> Unit) {
    val name = stringResource(m.name)
    val grams = m.grams?.roundToInt()
    val g = stringResource(R.string.unit_g)
    val described = when {
        grams == null -> stringResource(R.string.review_macro_missing_a11y, name)
        share == null -> stringResource(R.string.review_macro_a11y, name, grams)
        else -> stringResource(R.string.review_macro_share_a11y, name, grams, share)
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                when {
                    missing && grams == null -> "$name · ${stringResource(R.string.review_missing)}"
                    share != null -> "$name · $share% · $grams $g"
                    else -> "$name · ${grams ?: "–"} $g"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = if (missing && grams == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).clearAndSetSemantics { contentDescription = described },
            )
            if (missing) {
                OutlinedTextField(
                    value = typed,
                    onValueChange = { onMissing(m.field, it) },
                    singleLine = true,
                    placeholder = { Text("–") },
                    suffix = { Text(g) },
                    label = { Text(name) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.width(120.dp),
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(SnackshotTheme.colors.macroTrack)) {
            if (share != null && share > 0) {
                Box(Modifier.fillMaxWidth(share / 100f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(m.color))
            }
        }
    }
}

@Composable
private fun BottomActions(state: ReviewState, onSave: () -> Unit, onRetake: () -> Unit, onQuickAdd: () -> Unit, onDiscard: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(
                Modifier.padding(horizontal = Spacing.content, vertical = Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                Button(
                    onClick = onSave,
                    enabled = state.canSave,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) {
                    Icon(painterResource(R.drawable.ic_check), contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(stringResource(R.string.action_save), modifier = Modifier.padding(start = Spacing.small))
                }
                if (state.draft != null && !state.draft.canSave) {
                    Text(
                        stringResource(R.string.review_save_blocked),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    AltAction(R.drawable.ic_photo_camera, R.string.review_retake, onRetake)
                    AltAction(R.drawable.ic_bolt, R.string.recognition_quick_add, onQuickAdd)
                    AltAction(R.drawable.ic_close, R.string.review_discard, onDiscard)
                }
            }
        }
    }
}

@Composable
private fun AltAction(@DrawableRes icon: Int, @StringRes label: Int, onClick: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick, role = Role.Button)
            .heightIn(min = Size.minTouchTarget)
            .padding(horizontal = Spacing.medium, vertical = Spacing.grid),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(label), style = MaterialTheme.typography.labelMedium)
    }
}

/** A dot in the macro's color, for the S-07 macro fields. */
@Composable
internal fun MacroDot(color: Color) {
    Box(Modifier.size(8.dp).clip(CircleShape).background(color))
}

internal fun NutritionValues.rounded(field: NutrientField): String = of(field)?.roundToInt()?.toString().orEmpty()
