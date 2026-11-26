package app.snackshot.domain.onboarding

import kotlinx.coroutines.flow.Flow

/** Whether the user has passed S-01 Welcome on this device. */
interface OnboardingRepository {
    /** Emits the stored value; errors if local storage cannot be read. */
    val welcomeCompleted: Flow<Boolean>

    suspend fun completeWelcome()
}
