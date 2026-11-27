package app.snackshot.ui.goals

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.snackshot.domain.nutrition.CalorieRange
import app.snackshot.domain.nutrition.GoalsRepository
import app.snackshot.domain.nutrition.NutritionGoals
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One goal of Settings → Nutrition preferences (F-16). */
enum class GoalKind { CALORIES, CARBS, FAT, PROTEIN }

/** S-16 Nutrition preferences: optional daily goals, stored only on this device (WBS 5.3). */
@HiltViewModel
class NutritionPreferencesViewModel @Inject constructor(private val repository: GoalsRepository) : ViewModel() {
    /** Null while loading. */
    val goals: StateFlow<NutritionGoals?> = repository.goals.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun clearAll() {
        viewModelScope.launch { repository.setGoals(NutritionGoals.NONE) }
    }
}

/**
 * The goal being edited with the steppers. Calories are a range (minimum and maximum, the target is
 * the middle); a macro goal is one number of grams.
 */
data class GoalDraft(val minKcal: Int, val maxKcal: Int, val grams: Int)

data class GoalDetailState(
    val kind: GoalKind,
    val draft: GoalDraft,
    /** The goal is set now, so it can be cleared. */
    val isSet: Boolean,
    /** For the macro explanation: the calorie target, when set. */
    val calorieTargetKcal: Int?,
) {
    val canDecreaseMin get() = draft.minKcal - CALORIE_STEP >= NutritionGoals.CALORIES_LIMIT.first
    val canIncreaseMin get() = draft.minKcal + CALORIE_STEP < draft.maxKcal
    val canDecreaseMax get() = draft.maxKcal - CALORIE_STEP > draft.minKcal
    val canIncreaseMax get() = draft.maxKcal + CALORIE_STEP <= NutritionGoals.CALORIES_LIMIT.last
    val canDecreaseGrams get() = draft.grams - 1 >= gramsLimit(kind).first
    val canIncreaseGrams get() = draft.grams + 1 <= gramsLimit(kind).last

    companion object {
        const val CALORIE_STEP = 10
    }
}

internal fun gramsLimit(kind: GoalKind): IntRange = when (kind) {
    GoalKind.CARBS -> NutritionGoals.CARBS_LIMIT
    GoalKind.FAT -> NutritionGoals.FAT_LIMIT
    GoalKind.PROTEIN -> NutritionGoals.PROTEIN_LIMIT
    GoalKind.CALORIES -> 0..0
}

/** One goal's detail screen with steppers (prototype `s16_settings.html`, goal detail views). */
@HiltViewModel
class GoalDetailViewModel @Inject constructor(private val handle: SavedStateHandle, private val repository: GoalsRepository) : ViewModel() {
    /** From the navigation route `GoalRoute(goal)`. */
    val kind: GoalKind = GoalKind.valueOf(checkNotNull(handle.get<String>(ARG_GOAL)) { "GoalRoute needs a goal" })

    private val _state = MutableStateFlow<GoalDetailState?>(null)

    /** Null while the current goals load. */
    val state: StateFlow<GoalDetailState?> = _state.asStateFlow()

    private val _done = MutableStateFlow(false)

    /** Set after Set goal or Clear goal; the screen goes back. */
    val done: StateFlow<Boolean> = _done.asStateFlow()

    init {
        viewModelScope.launch {
            val goals = repository.goals.first()
            val saved = handle.get<IntArray>(KEY_DRAFT)?.let { GoalDraft(it[0], it[1], it[2]) }
            _state.value =
                GoalDetailState(kind, saved ?: initialDraft(goals), isSet = isSet(goals), calorieTargetKcal = goals.calories?.targetKcal)
        }
    }

    fun decreaseMin() = edit { it.copy(minKcal = it.minKcal - GoalDetailState.CALORIE_STEP) }

    fun increaseMin() = edit { it.copy(minKcal = it.minKcal + GoalDetailState.CALORIE_STEP) }

    fun decreaseMax() = edit { it.copy(maxKcal = it.maxKcal - GoalDetailState.CALORIE_STEP) }

    fun increaseMax() = edit { it.copy(maxKcal = it.maxKcal + GoalDetailState.CALORIE_STEP) }

    fun decreaseGrams() = edit { it.copy(grams = it.grams - 1) }

    fun increaseGrams() = edit { it.copy(grams = it.grams + 1) }

    fun setGoal() = update { goals ->
        val d = checkNotNull(_state.value).draft
        when (kind) {
            GoalKind.CALORIES -> goals.copy(calories = CalorieRange(d.minKcal, d.maxKcal))
            GoalKind.CARBS -> goals.copy(carbsGrams = d.grams)
            GoalKind.FAT -> goals.copy(fatGrams = d.grams)
            GoalKind.PROTEIN -> goals.copy(proteinGrams = d.grams)
        }
    }

    fun clearGoal() = update { goals ->
        when (kind) {
            GoalKind.CALORIES -> goals.copy(calories = null)
            GoalKind.CARBS -> goals.copy(carbsGrams = null)
            GoalKind.FAT -> goals.copy(fatGrams = null)
            GoalKind.PROTEIN -> goals.copy(proteinGrams = null)
        }
    }

    /** Steppers never leave the limits (the buttons are disabled at the ends). */
    private fun edit(change: (GoalDraft) -> GoalDraft) {
        val s = _state.value ?: return
        val next = change(s.draft)
        val valid = when (kind) {
            GoalKind.CALORIES ->
                next.minKcal in NutritionGoals.CALORIES_LIMIT &&
                    next.maxKcal in NutritionGoals.CALORIES_LIMIT &&
                    next.minKcal < next.maxKcal
            else -> next.grams in gramsLimit(kind)
        }
        if (!valid) return
        _state.value = s.copy(draft = next)
        handle[KEY_DRAFT] = intArrayOf(next.minKcal, next.maxKcal, next.grams)
    }

    private fun update(change: (NutritionGoals) -> NutritionGoals) {
        if (_state.value == null || _done.value) return
        viewModelScope.launch {
            repository.setGoals(change(repository.goals.first()))
            _done.value = true
        }
    }

    private fun isSet(goals: NutritionGoals) = when (kind) {
        GoalKind.CALORIES -> goals.calories != null
        GoalKind.CARBS -> goals.carbsGrams != null
        GoalKind.FAT -> goals.fatGrams != null
        GoalKind.PROTEIN -> goals.proteinGrams != null
    }

    /** The current goal, or the prototype's starting values for a goal not set yet. */
    private fun initialDraft(goals: NutritionGoals): GoalDraft {
        val calories = goals.calories ?: DEFAULT_CALORIES
        val grams = when (kind) {
            GoalKind.CARBS -> goals.carbsGrams ?: DEFAULT_CARBS
            GoalKind.FAT -> goals.fatGrams ?: DEFAULT_FAT
            GoalKind.PROTEIN -> goals.proteinGrams ?: DEFAULT_PROTEIN
            GoalKind.CALORIES -> 0
        }
        return GoalDraft(calories.minKcal, calories.maxKcal, grams)
    }

    companion object {
        /** The property name of `GoalRoute`; type-safe navigation stores route arguments under it. */
        const val ARG_GOAL = "goal"
        private const val KEY_DRAFT = "draft"

        // Starting values from the S-16 prototype: 2,300 kcal ±10 %, carbs 220, fat 70, protein 150 g.
        val DEFAULT_CALORIES = CalorieRange(2070, 2530)
        const val DEFAULT_CARBS = 220
        const val DEFAULT_FAT = 70
        const val DEFAULT_PROTEIN = 150
    }
}
