package app.snackshot.datalocal.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import app.snackshot.domain.onboarding.OnboardingRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Onboarding flags in the app-private settings DataStore (decision 8). */
class DataStoreOnboardingRepository @Inject constructor(@SettingsStore private val settings: DataStore<Preferences>) :
    OnboardingRepository {
    override val welcomeCompleted: Flow<Boolean> =
        settings.data.map { it[WELCOME_COMPLETED] ?: false }.distinctUntilChanged()

    override suspend fun completeWelcome() {
        settings.edit { it[WELCOME_COMPLETED] = true }
    }

    private companion object {
        val WELCOME_COMPLETED = booleanPreferencesKey("welcome_completed")
    }
}
