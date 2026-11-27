package app.snackshot.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.snackshot.domain.access.AccessCheck
import app.snackshot.domain.access.AccessError
import app.snackshot.domain.access.AiAccessRepository
import app.snackshot.domain.access.AiAccessStatus
import app.snackshot.domain.meal.MealEntry
import app.snackshot.domain.meal.MealId
import app.snackshot.domain.meal.MealRepository
import app.snackshot.domain.meal.MealSource
import app.snackshot.domain.meal.MealType
import app.snackshot.domain.nutrition.GoalsRepository
import app.snackshot.domain.nutrition.NutritionGoals
import app.snackshot.domain.nutrition.NutritionTotals
import app.snackshot.domain.nutrition.NutritionValues
import app.snackshot.domain.nutrition.total
import app.snackshot.ui.state.UiError
import app.snackshot.ui.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One row of Recent meals. [name] is blank for a Quick add saved without a name. */
data class HomeMeal(
    val id: MealId,
    val name: String,
    val source: MealSource,
    val mealType: MealType,
    val time: LocalTime,
    val energyKcal: Double,
    val values: NutritionValues,
)

/** S-02 Home content: today's totals against the optional goals, and today's and yesterday's meals. */
data class HomeContent(
    val day: LocalDate,
    val totals: NutritionTotals,
    val goals: NutritionGoals,
    val today: List<HomeMeal>,
    val yesterday: List<HomeMeal>,
) {
    val isEmpty: Boolean get() = today.isEmpty() && yesterday.isEmpty()
}

/** A one-off message for the snackbar. */
sealed interface HomeMessage {
    data class MealSaved(val id: MealId) : HomeMessage

    data object TrialEnded : HomeMessage

    data object Waitlisted : HomeMessage

    /** The kill switch is off. */
    data object AiPaused : HomeMessage

    /** Paused, suspended or cancelled by the broker. */
    data object AiUnavailable : HomeMessage

    data object Offline : HomeMessage

    data object Busy : HomeMessage

    data object ServiceError : HomeMessage
}

/**
 * S-02 Home (doc 10 §4.4, WBS 5.1). Reads only local data, so it works offline; AI access is
 * asked only when the user picks Take a photo or Gallery.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val meals: MealRepository,
    private val goals: GoalsRepository,
    private val access: AiAccessRepository,
    private val clock: Clock,
    private val zone: Provider<ZoneId>,
) : ViewModel() {
    private val attempts = MutableStateFlow(0)

    /** The local date, re-checked every minute so Home moves to a new day at midnight. */
    private val day = flow {
        while (true) {
            emit(LocalDate.now(clock.withZone(zone.get())))
            delay(DAY_CHECK_MS)
        }
    }.distinctUntilChanged()

    val state: StateFlow<UiState<HomeContent>> = attempts
        .flatMapLatest { day }
        .flatMapLatest { today ->
            val z = zone.get()
            val from = today.minusDays(1).atStartOfDay(z).toInstant()
            val until = today.plusDays(1).atStartOfDay(z).toInstant()
            combine(meals.observeMeals(from, until), goals.goals) { entries, goals ->
                val (todays, yesterdays) = entries.partition { it.eatenAt.atZone(z).toLocalDate() == today }
                UiState.Success(
                    HomeContent(
                        day = today,
                        totals = todays.map { it.values }.total(),
                        goals = goals,
                        today = todays.map { it.toHomeMeal(z) },
                        yesterday = yesterdays.map { it.toHomeMeal(z) },
                    ),
                ) as UiState<HomeContent>
            }
        }
        .catch { emit(UiState.Error(UiError(operation = "home"))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading())

    private val _message = MutableStateFlow<HomeMessage?>(null)
    val message: StateFlow<HomeMessage?> = _message.asStateFlow()

    private val _checkingAccess = MutableStateFlow(false)

    /** An AI action is waiting for the broker; the entry action shows progress. */
    val checkingAccess: StateFlow<Boolean> = _checkingAccess.asStateFlow()

    private val _capture = MutableStateFlow<MealSource?>(null)

    /** AI access is available: open the camera (S-03) or the photo picker (S-04), then [captureOpened]. */
    val capture: StateFlow<MealSource?> = _capture.asStateFlow()

    private val _highlighted = MutableStateFlow<MealId?>(null)

    /** The meal just saved, highlighted in Recent meals. */
    val highlighted: StateFlow<MealId?> = _highlighted.asStateFlow()

    /** Free pictures left, once AI access exists (shown with WBS 9.1). */
    val picturesLeft: StateFlow<Int?> = access.access
        .map { it?.takeIf { a -> a.status == AiAccessStatus.TRIAL }?.pictures?.left }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    init {
        // At app start: a stale AI status is refreshed in the background (doc 11 §8, cached ~1 h).
        viewModelScope.launch { runCatching { access.refreshIfStale() } }
    }

    fun retry() {
        attempts.value++
    }

    /** Take a photo or Gallery: the first AI action provisions this install (doc 11 §9). */
    fun onAiAction(source: MealSource) {
        require(source.isAiRecognition) { "Quick add needs no AI access" }
        if (_checkingAccess.value) return
        _checkingAccess.value = true
        viewModelScope.launch {
            try {
                val check = access.ensureAccess()
                if (check is AccessCheck.Available) _capture.value = source else _message.value = messageFor(check)
            } finally {
                _checkingAccess.value = false
            }
        }
    }

    fun captureOpened() {
        _capture.value = null
    }

    fun onMealSaved(id: MealId) {
        _highlighted.value = id
        _message.value = HomeMessage.MealSaved(id)
    }

    fun messageShown() {
        _message.value = null
    }

    private fun messageFor(check: AccessCheck): HomeMessage? = when (check) {
        is AccessCheck.Available -> null
        is AccessCheck.NotAvailable -> when {
            !check.access.aiEnabled -> HomeMessage.AiPaused
            check.access.status == AiAccessStatus.TRIAL_ENDED -> HomeMessage.TrialEnded
            check.access.status == AiAccessStatus.WAITLISTED -> HomeMessage.Waitlisted
            else -> HomeMessage.AiUnavailable
        }
        is AccessCheck.Failed -> when (check.error) {
            AccessError.OFFLINE -> HomeMessage.Offline
            AccessError.AI_PAUSED -> HomeMessage.AiPaused
            AccessError.BUSY -> HomeMessage.Busy
            AccessError.SERVICE -> HomeMessage.ServiceError
        }
    }

    private fun MealEntry.toHomeMeal(zone: ZoneId) = HomeMeal(
        id = id,
        name = name,
        source = source,
        mealType = mealType,
        time = eatenAt.atZone(zone).toLocalTime(),
        energyKcal = energyKcal,
        values = values,
    )

    private companion object {
        const val DAY_CHECK_MS = 60_000L
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
