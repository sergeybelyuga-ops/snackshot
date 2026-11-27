package app.snackshot.ui.quickadd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.snackshot.domain.meal.MealId
import app.snackshot.domain.meal.MealType
import app.snackshot.ui.R
import app.snackshot.ui.home.formatInt
import app.snackshot.ui.home.mealTypeLabel
import app.snackshot.ui.theme.Size
import app.snackshot.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun QuickAddRoute(viewModel: QuickAddViewModel, onSaved: (MealId) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()
    LaunchedEffect(saved) { saved?.let(onSaved) }
    QuickAddScreen(
        state = state,
        actions = QuickAddActions(
            onName = viewModel::onName,
            onEnergy = viewModel::onEnergy,
            onProtein = viewModel::onProtein,
            onCarbs = viewModel::onCarbs,
            onFat = viewModel::onFat,
            onGrams = viewModel::onGrams,
            onMealType = viewModel::onMealType,
            onDate = viewModel::onDate,
            onTime = viewModel::onTime,
            onCalculateEnergy = viewModel::calculateEnergy,
            onSave = viewModel::save,
            onBack = onBack,
        ),
        modifier = modifier,
    )
}

class QuickAddActions(
    val onName: (String) -> Unit = {},
    val onEnergy: (String) -> Unit = {},
    val onProtein: (String) -> Unit = {},
    val onCarbs: (String) -> Unit = {},
    val onFat: (String) -> Unit = {},
    val onGrams: (String) -> Unit = {},
    val onMealType: (MealType) -> Unit = {},
    val onDate: (LocalDate) -> Unit = {},
    val onTime: (LocalTime) -> Unit = {},
    val onCalculateEnergy: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onBack: () -> Unit = {},
)

/** S-09 Quick add (prototype `s09_quick_add.html`, doc 10 §4.8). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddScreen(state: QuickAddState, actions: QuickAddActions, modifier: Modifier = Modifier) {
    var picker by rememberSaveable { mutableStateOf(Picker.NONE) }
    val form = state.form

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.quick_add_title)) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
        bottomBar = {
            // Outside the scrolling form, so large text never hides Save (doc 10 §4.8).
            Surface(tonalElevation = 2.dp, modifier = Modifier.imePadding()) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(Spacing.content),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                ) {
                    OutlinedButton(onClick = actions.onBack, modifier = Modifier.weight(1f).heightIn(min = Size.minTouchTarget)) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    Button(
                        onClick = actions.onSave,
                        enabled = !state.saving,
                        modifier = Modifier.weight(1f).heightIn(min = Size.minTouchTarget),
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.content),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            Text(
                stringResource(R.string.quick_add_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SectionTitle(R.string.quick_add_section_meal)
            OutlinedTextField(
                value = form.name,
                onValueChange = actions.onName,
                label = { Text(stringResource(R.string.quick_add_name)) },
                supportingText = {
                    Text(
                        errorText(QuickAddField.NAME, state.errors[QuickAddField.NAME]) ?: stringResource(R.string.quick_add_name_hint),
                    )
                },
                isError = state.errors[QuickAddField.NAME] != null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(R.string.quick_add_meal_type), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                MealType.entries.forEach { type ->
                    FilterChip(
                        selected = form.mealType == type,
                        onClick = { actions.onMealType(type) },
                        label = { Text(stringResource(mealTypeLabel(type))) },
                        modifier = Modifier.heightIn(min = Size.minTouchTarget),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                OutlinedButton(onClick = { picker = Picker.DATE }, modifier = Modifier.heightIn(min = Size.minTouchTarget)) {
                    Text(dateLabel(state.eatenAt.toLocalDate(), LocalDate.now(state.zone)))
                }
                OutlinedButton(onClick = { picker = Picker.TIME }, modifier = Modifier.heightIn(min = Size.minTouchTarget)) {
                    Icon(
                        painterResource(R.drawable.ic_schedule),
                        contentDescription = null,
                        modifier = Modifier.padding(end = Spacing.small),
                    )
                    Text(state.eatenAt.toLocalTime().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)))
                }
            }

            HorizontalDivider(Modifier.padding(vertical = Spacing.small))
            SectionTitle(R.string.quick_add_section_nutrition)
            if (state.nothingEntered) {
                Text(
                    stringResource(R.string.quick_add_nothing_entered),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                )
            }
            AmountField(
                state,
                QuickAddField.ENERGY,
                form.energy,
                actions.onEnergy,
                R.string.quick_add_calories,
                R.string.unit_kcal,
                hint = state.energyFromMacros?.let {
                    stringResource(R.string.quick_add_energy_from_macros, formatInt(it))
                },
            )
            if (state.canCalculateEnergy) {
                TextButton(onClick = actions.onCalculateEnergy, modifier = Modifier.heightIn(min = Size.minTouchTarget)) {
                    Text(stringResource(R.string.quick_add_calculate))
                }
            }
            AmountField(state, QuickAddField.CARBS, form.carbs, actions.onCarbs, R.string.macro_carbs, R.string.unit_g)
            AmountField(state, QuickAddField.FAT, form.fat, actions.onFat, R.string.macro_fat, R.string.unit_g)
            AmountField(state, QuickAddField.PROTEIN, form.protein, actions.onProtein, R.string.macro_protein, R.string.unit_g)

            HorizontalDivider(Modifier.padding(vertical = Spacing.small))
            SectionTitle(R.string.quick_add_section_details)
            AmountField(
                state,
                QuickAddField.GRAMS,
                form.grams,
                actions.onGrams,
                R.string.quick_add_serving,
                R.string.unit_g,
                hint = stringResource(R.string.quick_add_optional),
                last = true,
                onDone = actions.onSave,
            )
        }
    }

    when (picker) {
        Picker.DATE -> DateDialog(state, onPick = actions.onDate, onDismiss = { picker = Picker.NONE })
        Picker.TIME -> TimeDialog(state, onPick = actions.onTime, onDismiss = { picker = Picker.NONE })
        Picker.NONE -> Unit
    }
}

private enum class Picker { NONE, DATE, TIME }

@Composable
private fun SectionTitle(text: Int) {
    Text(stringResource(text), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
}

@Composable
private fun AmountField(
    state: QuickAddState,
    field: QuickAddField,
    value: String,
    onChange: (String) -> Unit,
    label: Int,
    unit: Int,
    hint: String? = null,
    last: Boolean = false,
    onDone: () -> Unit = {},
) {
    val error = state.errors[field]
    val errorMessage = errorText(field, error)
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        suffix = { Text(stringResource(unit)) },
        supportingText = (errorMessage ?: hint)?.let { { Text(it) } },
        isError = error != null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = if (last) ImeAction.Done else ImeAction.Next),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { if (errorMessage != null) error(errorMessage) },
    )
}

@Composable
private fun errorText(field: QuickAddField, error: FieldError?): String? = when (error) {
    null -> null
    FieldError.NOT_A_NUMBER -> stringResource(R.string.quick_add_error_number)
    FieldError.NEGATIVE -> stringResource(R.string.quick_add_error_negative)
    FieldError.TOO_HIGH -> stringResource(
        if (field ==
            QuickAddField.ENERGY
        ) {
            R.string.quick_add_error_energy_high
        } else {
            R.string.quick_add_error_grams_high
        },
    )
    FieldError.TOO_LONG -> stringResource(R.string.quick_add_error_name_long)
    FieldError.PORTION_RANGE -> stringResource(R.string.quick_add_error_portion)
}

@Composable
private fun dateLabel(date: LocalDate, today: LocalDate): String {
    val formatted = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    return when (date) {
        today -> stringResource(R.string.quick_add_today, formatted)
        today.minusDays(1) -> stringResource(R.string.quick_add_yesterday, formatted)
        else -> formatted
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDialog(state: QuickAddState, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val today = LocalDate.now(state.zone)
    // The picker works in UTC milliseconds at midnight.
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = state.eatenAt.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) =
                !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isAfter(today)

            override fun isSelectableYear(year: Int) = year <= today.year
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                onDismiss()
            }) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    ) { DatePicker(pickerState) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(state: QuickAddState, onPick: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val time = state.eatenAt.toLocalTime()
    val pickerState = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onPick(LocalTime.of(pickerState.hour, pickerState.minute))
                onDismiss()
            }) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
        text = { TimePicker(pickerState) },
    )
}
