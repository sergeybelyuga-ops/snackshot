package app.snackshot.ui.state

/**
 * Shared state contract (docs/Architecture/10_architecture_ux_spec.md §4.1):
 * every screen is Loading, Empty, Error, or Success.
 *
 * Loading and Error carry the data the screen already has, so a refresh or a
 * failed retry never hides available local data.
 */
sealed interface UiState<out T> {
    data class Loading<T>(val previous: T? = null) : UiState<T>

    data object Empty : UiState<Nothing>

    data class Error<T>(val error: UiError, val previous: T? = null) : UiState<T>

    data class Success<T>(val data: T) : UiState<T>
}

/**
 * A failure as the user sees it.
 *
 * @param operation what failed, e.g. "Couldn't load your meals" (always shown).
 * @param detail optional explanation or next step.
 * @param retryable whether Retry is offered.
 */
data class UiError(val operation: String, val detail: String? = null, val retryable: Boolean = true)

/** The data a state can show right now, if any. */
val <T> UiState<T>.dataOrNull: T?
    get() = when (this) {
        is UiState.Success -> data
        is UiState.Loading -> previous
        is UiState.Error -> previous
        UiState.Empty -> null
    }
