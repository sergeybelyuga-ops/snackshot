package app.snackshot.ui.review

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.snackshot.domain.meal.MealDraft
import app.snackshot.domain.meal.MealType
import app.snackshot.domain.nutrition.NutrientField
import app.snackshot.ui.R
import app.snackshot.ui.home.mealTypeLabel
import app.snackshot.ui.quickadd.parseAmount
import app.snackshot.ui.theme.Size
import app.snackshot.ui.theme.SnackshotTheme
import app.snackshot.ui.theme.Spacing
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

/**
 * S-07 Edit nutrition (prototype `s07_edit_nutrition.html`): name, meal, time, weight and values in
 * a sheet over S-15. Changing the weight rescales the values shown until the user types one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNutritionSheet(draft: MealDraft, zone: ZoneId, onApply: (NutritionEdit) -> Boolean, onReset: () -> Unit, onDismiss: () -> Unit) {
    val values = draft.values
    var name by rememberSaveable { mutableStateOf(draft.name) }
    var mealType by rememberSaveable { mutableStateOf(draft.mealType) }
    var time by rememberSaveable { mutableStateOf(draft.eatenAt.atZone(zone).toLocalTime().withSecond(0).withNano(0)) }
    var grams by rememberSaveable { mutableStateOf(draft.grams?.let { formatGrams(it) }.orEmpty()) }
    var energy by rememberSaveable { mutableStateOf(values.rounded(NutrientField.ENERGY)) }
    var carbs by rememberSaveable { mutableStateOf(values.rounded(NutrientField.CARBS)) }
    var fat by rememberSaveable { mutableStateOf(values.rounded(NutrientField.FAT)) }
    var protein by rememberSaveable { mutableStateOf(values.rounded(NutrientField.PROTEIN)) }
    var typed by rememberSaveable { mutableStateOf(setOf<NutrientField>()) }
    var invalid by rememberSaveable { mutableStateOf(false) }
    var pickTime by rememberSaveable { mutableStateOf(false) }

    /** A new weight rescales the fields the user hasn't typed in this sheet. */
    fun onWeight(text: String) {
        grams = text
        val g = parseAmount(text).value?.takeIf { it in MealDraft.MIN_GRAMS..MealDraft.MAX_GRAMS } ?: return
        val scaled = draft.withGrams(g).values
        if (NutrientField.ENERGY !in typed) energy = scaled.rounded(NutrientField.ENERGY)
        if (NutrientField.CARBS !in typed) carbs = scaled.rounded(NutrientField.CARBS)
        if (NutrientField.FAT !in typed) fat = scaled.rounded(NutrientField.FAT)
        if (NutrientField.PROTEIN !in typed) protein = scaled.rounded(NutrientField.PROTEIN)
    }

    fun step(up: Boolean) {
        val g = parseAmount(grams).value ?: draft.grams ?: return
        val next = (if (up) g + MealReviewViewModel.GRAMS_STEP else g - MealReviewViewModel.GRAMS_STEP).coerceIn(
            MealDraft.MIN_GRAMS,
            MealDraft.MAX_GRAMS,
        )
        onWeight(formatGrams(next))
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().imePadding().navigationBarsPadding()) {
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.section),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.edit_title),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f).semantics { heading() },
                    )
                    TextButton(onClick = {
                        onReset()
                        onDismiss()
                    }) {
                        Icon(painterResource(R.drawable.ic_undo), contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.edit_reset), modifier = Modifier.padding(start = Spacing.small))
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.edit_food_name)) },
                    supportingText = if (name == draft.originalName) aiEstimate() else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                    MealTypeField(mealType, onPick = { mealType = it }, modifier = Modifier.weight(1f))
                    PickerField(
                        label = R.string.edit_time,
                        value = time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)),
                        icon = R.drawable.ic_schedule,
                        onClick = { pickTime = true },
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                    FilledTonalIconButton(onClick = { step(false) }, modifier = Modifier.size(56.dp)) {
                        Icon(
                            painterResource(R.drawable.ic_remove),
                            contentDescription = stringResource(R.string.review_grams_less, MealReviewViewModel.GRAMS_STEP.toInt()),
                        )
                    }
                    NumberField(
                        grams,
                        ::onWeight,
                        R.string.edit_weight,
                        R.string.unit_g,
                        Modifier.weight(1f),
                        isError =
                        invalid && !validGrams(grams),
                    )
                    FilledTonalIconButton(onClick = { step(true) }, modifier = Modifier.size(56.dp)) {
                        Icon(
                            painterResource(R.drawable.ic_add),
                            contentDescription = stringResource(R.string.review_grams_more, MealReviewViewModel.GRAMS_STEP.toInt()),
                        )
                    }
                }
                Text(
                    stringResource(R.string.edit_nutrition),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = Spacing.small),
                )
                NumberField(
                    energy,
                    {
                        energy = it
                        typed = typed + NutrientField.ENERGY
                    },
                    R.string.quick_add_calories,
                    R.string.unit_kcal,
                    Modifier.fillMaxWidth(),
                    isError = invalid && parseAmount(energy).error != null,
                )
                val colors = SnackshotTheme.colors
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                    MacroField(carbs, {
                        carbs = it
                        typed = typed + NutrientField.CARBS
                    }, R.string.macro_carbs, colors.macroCarbs, Modifier.weight(1f))
                    MacroField(fat, {
                        fat = it
                        typed = typed + NutrientField.FAT
                    }, R.string.macro_fat, colors.macroFat, Modifier.weight(1f))
                    MacroField(protein, {
                        protein = it
                        typed = typed + NutrientField.PROTEIN
                    }, R.string.macro_protein, colors.macroProtein, Modifier.weight(1f))
                }
                if (invalid) {
                    Text(
                        stringResource(R.string.edit_invalid),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            HorizontalDivider(Modifier.padding(top = Spacing.medium), color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Spacing.section, vertical = Spacing.medium),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small, Alignment.End),
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = Size.minTouchTarget)) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(
                    onClick = {
                        invalid = !onApply(NutritionEdit(name, mealType, time, grams, energy, carbs, fat, protein))
                    },
                    modifier = Modifier.heightIn(min = Size.minTouchTarget),
                ) { Text(stringResource(R.string.edit_apply)) }
            }
        }
    }

    if (pickTime) {
        val pickerState = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute)
        AlertDialog(
            onDismissRequest = { pickTime = false },
            confirmButton = {
                TextButton(onClick = {
                    time = LocalTime.of(pickerState.hour, pickerState.minute)
                    pickTime = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text(stringResource(R.string.action_cancel)) } },
            text = { TimePicker(pickerState) },
        )
    }
}

private fun validGrams(text: String) = parseAmount(text).value?.let { it in MealDraft.MIN_GRAMS..MealDraft.MAX_GRAMS } == true

private fun formatGrams(g: Double) = if (g % 1.0 == 0.0) g.toInt().toString() else ((g * 10).roundToInt() / 10.0).toString()

@Composable
private fun aiEstimate(): @Composable () -> Unit = {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.grid)) {
        Icon(painterResource(R.drawable.ic_auto_awesome), contentDescription = null, modifier = Modifier.size(14.dp))
        Text(stringResource(R.string.edit_ai_estimate))
    }
}

@Composable
private fun NumberField(
    value: String,
    onChange: (String) -> Unit,
    @StringRes label: Int,
    @StringRes unit: Int,
    modifier: Modifier,
    isError: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        suffix = { Text(stringResource(unit)) },
        singleLine = true,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

@Composable
private fun MacroField(value: String, onChange: (String) -> Unit, @StringRes label: Int, color: Color, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        prefix = { Box(Modifier.padding(end = Spacing.small)) { MacroDot(color) } },
        suffix = { Text(stringResource(R.string.unit_g)) },
        singleLine = true,
        isError = parseAmount(value).error != null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

@Composable
private fun MealTypeField(type: MealType, onPick: (MealType) -> Unit, modifier: Modifier) {
    var open by rememberSaveable { mutableStateOf(false) }
    Box(modifier) {
        PickerField(R.string.review_meal, stringResource(mealTypeLabel(type)), R.drawable.ic_arrow_drop_down, onClick = {
            open = true
        }, modifier = Modifier.fillMaxWidth())
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

/** A read-only field that opens a menu or a picker. */
@Composable
private fun PickerField(@StringRes label: Int, value: String, icon: Int, onClick: () -> Unit, modifier: Modifier) {
    Box(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(label)) },
            trailingIcon = { Icon(painterResource(icon), contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        // The field itself would take focus and open the keyboard; a transparent button on top opens the picker.
        val description = "${stringResource(label)}: $value"
        TextButton(onClick = onClick, modifier = Modifier.matchParentSize().semantics { contentDescription = description }) {}
    }
}
