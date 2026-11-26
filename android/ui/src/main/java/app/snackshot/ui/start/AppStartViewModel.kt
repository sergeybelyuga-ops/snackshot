package app.snackshot.ui.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.snackshot.domain.onboarding.OnboardingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Where the app starts: S-01 Welcome on first run, S-02 Home afterwards. */
sealed interface StartState {
    data object Loading : StartState

    data class Ready(val welcomeCompleted: Boolean) : StartState

    /** Local storage could not be read (prototype S-01 error state). */
    data object StorageError : StartState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AppStartViewModel @Inject constructor(private val onboarding: OnboardingRepository) : ViewModel() {
    private val attempts = MutableStateFlow(0)

    /** Decided once per attempt: completing Welcome later must not move the start destination. */
    val state: StateFlow<StartState> = attempts
        .flatMapLatest {
            flow<StartState> { emit(StartState.Ready(onboarding.welcomeCompleted.first())) }
                .onStart { emit(StartState.Loading) }
                .catch { emit(StartState.StorageError) }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, StartState.Loading)

    fun retry() {
        attempts.value++
    }

    fun completeWelcome() {
        viewModelScope.launch { onboarding.completeWelcome() }
    }
}
