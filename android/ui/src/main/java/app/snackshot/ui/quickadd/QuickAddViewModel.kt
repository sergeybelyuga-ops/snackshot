package app.snackshot.ui.quickadd

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.snackshot.domain.meal.DraftProblem
import app.snackshot.domain.meal.MealDraft
import app.snackshot.domain.meal.MealId
import app.snackshot.domain.meal.MealType
import app.snackshot.domain.meal.PendingMeal
import app.snackshot.domain.meal.PendingMealStore
import app.snackshot.domain.nutrition.Atwater
import app.snackshot.domain.nutrition.NutrientField
import app.snackshot.domain.nutrition.NutritionValues
import app.snackshot.ui.meal.MealSaver
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Provider
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class QuickAddField { NAME, ENERGY, PROTEIN, CARBS, FAT, GRAMS }

enum class FieldError {
    NOT_A_NUMBER,
    NEGATIVE,
    TOO_HIGH,
    TOO_LONG,

    /** Serving size outside 1–5,000 g. */
    PORTION_RANGE,
}

/**
 * The S-09 form as typed. Numbers stay text until saved, so a half-typed "12," is never lost or
 * reformatted under the user's fingers.
 */
data class QuickAddForm(
    val name: String = "",
    val energy: String = "",
    val protein: String = "",
    val carbs: String = "",
    val fat: String = "",
    val grams: String = "",
    val mealType: MealType,
    /** The user picked the meal type; otherwise it follows the time. */
    val mealTypeChosen: Boolean = false,
    val eatenAt: Instant,
)

data class QuickAddState(
    val form: QuickAddForm,
    val zone: ZoneId,
    val errors: Map<QuickAddField, FieldError>,
    /** Save was tapped with no nutrition value at all (prototype "Blank save"). */
    val nothingEntered: Boolean,
    val saving: Boolean,
) {
    val eatenAt: ZonedDateTime get() = form.eatenAt.atZone(zone)

    /** "Calculate kcal from macros" is offered once a macro is known. */
    val canCalculateEnergy: Boolean get() = parsedMacros().let { it.proteinGrams != null || it.carbsGrams != null || it.fatGrams != null }

    /** Calories left blank are saved from the macros (S-09 copy); shown as a hint. */
    val energyFromMacros: Int?
        get() = if (form.energy.isBlank()) Atwater.energyFromMacros(parsedMacros())?.roundToInt() else null

    private fun parsedMacros() = NutritionValues(
        proteinGrams = parseAmount(form.protein).value,
        carbsGrams = parseAmount(form.carbs).value,
        fatGrams = parseAmount(form.fat).value,
    )
}

/**
 * S-09 Quick add (doc 10 §4.8, WBS 5.2): calories and macros the user already knows. Local only,
 * so it works offline and whatever the AI access status. Survives process death.
 */
@HiltViewModel
class QuickAddViewModel @Inject constructor(
    private val handle: SavedStateHandle,
    private val saver: MealSaver,
    private val clock: Clock,
    private val pending: PendingMealStore,
    private val zone: Provider<ZoneId>,
) : ViewModel() {
    /** Opened from a failed or unwanted recognition: the meal keeps that photo and time (doc 10 §4.8). */
    private val fromDraft: Boolean = handle.get<Boolean>(ARG_FROM_DRAFT) == true

    /** The draft's processed photo, once it is read. */
    private var imageRef: String? = handle[KEY_IMAGE]

    private val attempted = MutableStateFlow(handle.get<Boolean>(KEY_ATTEMPTED) ?: false)
    private val form = MutableStateFlow(restore())
    private val _state = MutableStateFlow(stateOf(form.value, saving = false))
    val state: StateFlow<QuickAddState> = _state.asStateFlow()

    private val _saved = MutableStateFlow<MealId?>(null)

    /** Set once the meal is saved; the screen then returns to Home. */
    val saved: StateFlow<MealId?> = _saved.asStateFlow()

    init {
        if (fromDraft && handle.get<Boolean>(KEY_DRAFT_READ) != true) {
            viewModelScope.launch {
                val (at, ref) = when (val meal = pending.pending.first()) {
                    is PendingMeal.AwaitingRecognition -> meal.capturedAt to meal.imageRef
                    is PendingMeal.Reviewing -> meal.draft.eatenAt to meal.draft.imageRef
                    null -> return@launch
                }
                imageRef = ref
                handle[KEY_IMAGE] = ref
                handle[KEY_DRAFT_READ] = true
                edit { f -> moveTo(f, at) }
            }
        }
    }

    fun onName(value: String) = edit { it.copy(name = value) }

    fun onEnergy(value: String) = edit { it.copy(energy = value) }

    fun onProtein(value: String) = edit { it.copy(protein = value) }

    fun onCarbs(value: String) = edit { it.copy(carbs = value) }

    fun onFat(value: String) = edit { it.copy(fat = value) }

    fun onGrams(value: String) = edit { it.copy(grams = value) }

    fun onMealType(type: MealType) = edit { it.copy(mealType = type, mealTypeChosen = true) }

    fun onDate(date: LocalDate) = edit { f ->
        val z = zone.get()
        moveTo(f, date.atTime(f.eatenAt.atZone(z).toLocalTime()).atZone(z).toInstant())
    }

    fun onTime(time: LocalTime) = edit { f ->
        val z = zone.get()
        moveTo(f, f.eatenAt.atZone(z).toLocalDate().atTime(time).atZone(z).toInstant())
    }

    /** Fills calories with the Atwater energy of the entered macros (4/4/9 kcal per gram). */
    fun calculateEnergy() {
        val kcal = _state.value.let { s ->
            Atwater.energyFromMacros(
                NutritionValues(
                    proteinGrams = parseAmount(s.form.protein).value,
                    carbsGrams = parseAmount(s.form.carbs).value,
                    fatGrams = parseAmount(s.form.fat).value,
                ),
            )
        } ?: return
        edit { it.copy(energy = kcal.roundToInt().toString()) }
    }

    fun save() {
        if (_state.value.saving || _saved.value != null) return
        attempted.value = true
        handle[KEY_ATTEMPTED] = true
        val current = form.value
        val draft = draftOf(current)
        _state.value = stateOf(current, saving = draft != null)
        if (draft == null) return
        viewModelScope.launch {
            try {
                val id = saver.save(draft).id
                // The photo now belongs to the saved meal; the pending draft is done.
                if (fromDraft) pending.complete()
                _saved.value = id
            } finally {
                _state.update { it.copy(saving = false) }
            }
        }
    }

    private fun edit(change: (QuickAddForm) -> QuickAddForm) {
        val next = change(form.value)
        form.value = next
        persist(next)
        _state.value = stateOf(next, saving = _state.value.saving)
    }

    /** No meals in the future: a later time today becomes now. The meal type follows unless chosen. */
    private fun moveTo(f: QuickAddForm, requested: Instant): QuickAddForm {
        val now = clock.instant()
        val at = if (requested.isAfter(now)) now else requested
        val type = if (f.mealTypeChosen) f.mealType else MealType.suggestFor(at.atZone(zone.get()).toLocalTime())
        return f.copy(eatenAt = at, mealType = type)
    }

    /** A saveable draft, or null when a field is invalid or the domain rejects the values. */
    private fun draftOf(f: QuickAddForm): MealDraft? = baseDraft(f)?.takeIf { it.canSave }

    /** The draft for the typed values, or null while a number field can't be read. */
    private fun baseDraft(f: QuickAddForm): MealDraft? {
        if (numberErrors(f).isNotEmpty()) return null
        val z = zone.get()
        return MealDraft.quickAdd(f.eatenAt, z.rules.getOffset(f.eatenAt), f.mealType, imageRef = imageRef)
            .withName(f.name)
            .withGrams(parseAmount(f.grams).value)
            .withOverrides(
                NutritionValues(
                    energyKcal = parseAmount(f.energy).value,
                    proteinGrams = parseAmount(f.protein).value,
                    carbsGrams = parseAmount(f.carbs).value,
                    fatGrams = parseAmount(f.fat).value,
                ),
            )
    }

    private fun stateOf(f: QuickAddForm, saving: Boolean): QuickAddState {
        val errors = numberErrors(f).toMutableMap()
        var nothing = false
        if (attempted.value) {
            baseDraft(f)?.problems()?.forEach { problem ->
                when (problem) {
                    DraftProblem.NameTooLong -> errors[QuickAddField.NAME] = FieldError.TOO_LONG
                    DraftProblem.PortionOutOfRange -> errors[QuickAddField.GRAMS] = FieldError.PORTION_RANGE
                    DraftProblem.NoNutritionValues -> nothing = true
                    is DraftProblem.ValuesOverLimit -> problem.fields.forEach { errors[fieldOf(it)] = FieldError.TOO_HIGH }
                    // AI results only.
                    DraftProblem.NameMissing, is DraftProblem.MissingValues -> Unit
                }
            }
        }
        return QuickAddState(f, zone.get(), errors, nothingEntered = nothing, saving = saving)
    }

    private fun numberErrors(f: QuickAddForm): Map<QuickAddField, FieldError> = buildMap {
        listOf(
            QuickAddField.ENERGY to f.energy,
            QuickAddField.PROTEIN to f.protein,
            QuickAddField.CARBS to f.carbs,
            QuickAddField.FAT to f.fat,
            QuickAddField.GRAMS to f.grams,
        ).forEach { (field, text) -> parseAmount(text).error?.let { put(field, it) } }
    }

    private fun restore(): QuickAddForm {
        val now = clock.instant()
        val at = handle.get<Long>(KEY_AT)?.let(Instant::ofEpochMilli) ?: now
        return QuickAddForm(
            name = handle[KEY_NAME] ?: "",
            energy = handle[KEY_ENERGY] ?: "",
            protein = handle[KEY_PROTEIN] ?: "",
            carbs = handle[KEY_CARBS] ?: "",
            fat = handle[KEY_FAT] ?: "",
            grams = handle[KEY_GRAMS] ?: "",
            mealType = handle.get<String>(KEY_TYPE)?.let { name -> MealType.entries.firstOrNull { it.name == name } }
                ?: MealType.suggestFor(at.atZone(zone.get()).toLocalTime()),
            mealTypeChosen = handle[KEY_TYPE_CHOSEN] ?: false,
            eatenAt = at,
        )
    }

    private fun persist(f: QuickAddForm) {
        handle[KEY_NAME] = f.name
        handle[KEY_ENERGY] = f.energy
        handle[KEY_PROTEIN] = f.protein
        handle[KEY_CARBS] = f.carbs
        handle[KEY_FAT] = f.fat
        handle[KEY_GRAMS] = f.grams
        handle[KEY_TYPE] = f.mealType.name
        handle[KEY_TYPE_CHOSEN] = f.mealTypeChosen
        handle[KEY_AT] = f.eatenAt.toEpochMilli()
    }

    private companion object {
        const val KEY_NAME = "name"
        const val KEY_ENERGY = "energy"
        const val KEY_PROTEIN = "protein"
        const val KEY_CARBS = "carbs"
        const val KEY_FAT = "fat"
        const val KEY_GRAMS = "grams"
        const val KEY_TYPE = "meal_type"
        const val KEY_TYPE_CHOSEN = "meal_type_chosen"
        const val KEY_AT = "eaten_at"
        const val KEY_ATTEMPTED = "save_attempted"
        const val KEY_IMAGE = "draft_image"
        const val KEY_DRAFT_READ = "draft_read"
        const val ARG_FROM_DRAFT = "fromDraft"

        fun fieldOf(field: NutrientField) = when (field) {
            NutrientField.ENERGY -> QuickAddField.ENERGY
            NutrientField.PROTEIN -> QuickAddField.PROTEIN
            NutrientField.CARBS -> QuickAddField.CARBS
            NutrientField.FAT -> QuickAddField.FAT
        }
    }
}

/** A typed amount: blank is unknown, which differs from zero (doc 02 §4.3). */
internal data class ParsedAmount(val value: Double?, val error: FieldError?)

/** Accepts a comma or a dot as the decimal separator. */
internal fun parseAmount(text: String): ParsedAmount {
    val t = text.trim().replace(',', '.')
    if (t.isEmpty()) return ParsedAmount(null, null)
    val number = t.toDoubleOrNull()?.takeIf { it.isFinite() } ?: return ParsedAmount(null, FieldError.NOT_A_NUMBER)
    if (number < 0) return ParsedAmount(null, FieldError.NEGATIVE)
    return ParsedAmount(number, null)
}
