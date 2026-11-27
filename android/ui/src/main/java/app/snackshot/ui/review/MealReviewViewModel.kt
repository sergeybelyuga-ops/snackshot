package app.snackshot.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.snackshot.domain.meal.MealDraft
import app.snackshot.domain.meal.MealId
import app.snackshot.domain.meal.MealImageStore
import app.snackshot.domain.meal.MealType
import app.snackshot.domain.meal.NutritionEstimate
import app.snackshot.domain.meal.PendingMeal
import app.snackshot.domain.meal.PendingMealStore
import app.snackshot.domain.nutrition.NutrientField
import app.snackshot.domain.nutrition.NutritionValues
import app.snackshot.ui.meal.MealSaver
import app.snackshot.ui.quickadd.parseAmount
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.time.Clock
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Provider
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReviewState(
    /** Null while the pending draft is read. */
    val draft: MealDraft? = null,
    val image: File? = null,
    /** The portion as typed; the draft follows it whenever it is a valid number. */
    val gramsText: String = "",
    val gramsInvalid: Boolean = false,
    /** Values typed for the fields the recognition didn't return (partial result). */
    val missingText: Map<NutrientField, String> = emptyMap(),
    val saving: Boolean = false,
    val saved: MealId? = null,
    /** Discarded, or there is no draft to review: S-15 closes. */
    val closed: Boolean = false,
) {
    val confidencePercent: Int? get() = draft?.confidence?.let { (it * 100).roundToInt() }

    /** Below the 80 % threshold: the review is emphasized (doc 10 §4.7). */
    val lowConfidence: Boolean get() = (draft?.confidence ?: 1.0) < NutritionEstimate.HIGH_CONFIDENCE

    /** The fields LogMeal didn't return; the user types them inline to save. */
    val missingFields: Set<NutrientField> get() = draft?.basis?.values?.missingFields().orEmpty()

    val canSave: Boolean get() = draft != null && !gramsInvalid && !saving && draft.canSave

    /** The portion differs from the estimate: "Reset to estimate" is offered. */
    val canReset: Boolean get() = draft != null &&
        (draft.grams != draft.basis?.grams || !draft.overrides.isEmpty || draft.name != draft.originalName)
}

/** What the S-07 edit sheet applies; every number is as typed. */
data class NutritionEdit(
    val name: String,
    val mealType: MealType,
    val time: LocalTime,
    val grams: String,
    val energy: String,
    val carbs: String,
    val fat: String,
    val protein: String,
)

/**
 * S-15 Meal review and the S-07 edit sheet (doc 10 §4.7) for the pending photo or gallery result.
 * Every change is saved to the pending draft at once, so it survives process death; values follow
 * the grams by scaling (doc 02 §6.1–6.4) unless the user typed them.
 */
@HiltViewModel
class MealReviewViewModel @Inject constructor(
    private val pending: PendingMealStore,
    private val images: MealImageStore,
    private val saver: MealSaver,
    private val clock: Clock,
    private val zone: Provider<ZoneId>,
) : ViewModel() {
    private val _state = MutableStateFlow(ReviewState())
    val state: StateFlow<ReviewState> = _state.asStateFlow()

    val zoneId: ZoneId get() = zone.get()

    init {
        viewModelScope.launch {
            val draft = (pending.pending.first() as? PendingMeal.Reviewing)?.draft
            if (draft == null) {
                _state.update { it.copy(closed = true) }
                return@launch
            }
            _state.update {
                it.copy(draft = draft, image = draft.imageRef?.let(images::fileFor), gramsText = gramsText(draft.grams))
            }
        }
    }

    fun onGrams(text: String) {
        val grams = parseAmount(text).value
        val valid = grams != null && grams in MealDraft.MIN_GRAMS..MealDraft.MAX_GRAMS
        _state.update { it.copy(gramsText = text, gramsInvalid = !valid) }
        if (valid) change { it.withGrams(grams) }
    }

    /** −/+ beside the portion, in [GRAMS_STEP] g steps within 1–5,000 g. */
    fun stepGrams(up: Boolean) {
        val current = _state.value.draft?.grams ?: return
        val next = (if (up) current + GRAMS_STEP else current - GRAMS_STEP).coerceIn(MealDraft.MIN_GRAMS, MealDraft.MAX_GRAMS)
        _state.update { it.copy(gramsText = gramsText(next), gramsInvalid = false) }
        change { it.withGrams(next) }
    }

    fun onMealType(type: MealType) = change { it.withMealType(type) }

    /** A value LogMeal didn't return, typed inline on S-15. */
    fun onMissingValue(field: NutrientField, text: String) {
        _state.update { it.copy(missingText = it.missingText + (field to text)) }
        val value = parseAmount(text).value
        change { d -> d.withOverrides(d.overrides.with(field, value)) }
    }

    /**
     * S-07 Apply. Returns false and changes nothing when a number can't be read. A value is kept as
     * the user's own only when it differs from what the sheet showed; otherwise it keeps scaling.
     */
    fun applyEdit(edit: NutritionEdit): Boolean {
        val draft = _state.value.draft ?: return false
        val grams = parseAmount(edit.grams)
        val numbers = listOf(edit.energy, edit.carbs, edit.fat, edit.protein).map(::parseAmount)
        val gramsValue = grams.value
        if (grams.error != null || gramsValue == null || gramsValue !in MealDraft.MIN_GRAMS..MealDraft.MAX_GRAMS) return false
        if (numbers.any { it.error != null }) return false

        val z = zone.get()
        val date = draft.eatenAt.atZone(z).toLocalDate()
        val requested = date.atTime(edit.time).atZone(z).toInstant()
        val eatenAt = minOf(requested, clock.instant())

        val rescaled = draft.withGrams(gramsValue)
        val shown = rescaled.values
        val typed = NutritionValues(numbers[0].value, numbers[3].value, numbers[1].value, numbers[2].value)
        var overrides = draft.overrides
        NutrientField.entries.forEach { field ->
            val value = typed.of(field)
            if (value == null) {
                overrides = overrides.with(field, null)
            } else if (shown.of(field)?.let { display(it) } != display(value)) {
                overrides = overrides.with(field, value)
            }
        }
        val next = rescaled
            .withName(edit.name)
            .withMealType(edit.mealType)
            .withOverrides(overrides)
            .copy(eatenAt = eatenAt, zoneOffset = z.rules.getOffset(eatenAt))
        _state.update { it.copy(gramsText = gramsText(gramsValue), gramsInvalid = false, missingText = emptyMap()) }
        change { next }
        return true
    }

    /** Back to the recognized name, grams and values. */
    fun reset() {
        _state.update { it.copy(missingText = emptyMap()) }
        change { it.reset() }
        _state.update { s -> s.copy(gramsText = gramsText(s.draft?.grams), gramsInvalid = false) }
    }

    fun save() {
        val s = _state.value
        val draft = s.draft ?: return
        if (!s.canSave || s.saved != null) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                val entry = saver.save(draft)
                pending.complete()
                _state.update { it.copy(saved = entry.id) }
            } finally {
                _state.update { it.copy(saving = false) }
            }
        }
    }

    /** Discard (after the dialog) and Retake: the draft and its photo are deleted. */
    fun discard() {
        viewModelScope.launch {
            pending.discard()
            _state.update { it.copy(closed = true) }
        }
    }

    private fun change(transform: (MealDraft) -> MealDraft) {
        val draft = _state.value.draft ?: return
        val next = transform(draft)
        _state.update { it.copy(draft = next) }
        viewModelScope.launch { pending.save(PendingMeal.Reviewing(next)) }
    }

    private fun gramsText(grams: Double?) = grams?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }.orEmpty()

    companion object {
        const val GRAMS_STEP = 10.0

        /** Values are shown rounded to whole units; the sheet compares them the same way. */
        fun display(value: Double): Int = value.roundToInt()
    }
}

internal fun NutritionValues.of(field: NutrientField): Double? = when (field) {
    NutrientField.ENERGY -> energyKcal
    NutrientField.PROTEIN -> proteinGrams
    NutrientField.CARBS -> carbsGrams
    NutrientField.FAT -> fatGrams
}

internal fun NutritionValues.with(field: NutrientField, value: Double?): NutritionValues = when (field) {
    NutrientField.ENERGY -> copy(energyKcal = value)
    NutrientField.PROTEIN -> copy(proteinGrams = value)
    NutrientField.CARBS -> copy(carbsGrams = value)
    NutrientField.FAT -> copy(fatGrams = value)
}
