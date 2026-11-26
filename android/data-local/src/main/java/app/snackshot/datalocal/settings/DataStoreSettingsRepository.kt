package app.snackshot.datalocal.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.snackshot.domain.settings.AppSettings
import app.snackshot.domain.settings.SettingsRepository
import app.snackshot.domain.settings.ThemePreference
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** S-16 preferences in the app-private settings DataStore (decision 8). */
class DataStoreSettingsRepository @Inject constructor(@SettingsStore private val store: DataStore<Preferences>) : SettingsRepository {
    override val settings: Flow<AppSettings> = store.data.map { prefs ->
        AppSettings(
            productMetricsEnabled = prefs[PRODUCT_METRICS] ?: true,
            // An unknown value (written by a newer build) falls back to the default.
            theme = prefs[THEME]?.let { name -> ThemePreference.entries.firstOrNull { it.name == name } } ?: ThemePreference.SYSTEM,
            recognitionLanguage = prefs[LANGUAGE],
            recognitionCountry = prefs[COUNTRY],
        )
    }.distinctUntilChanged()

    override suspend fun setProductMetricsEnabled(enabled: Boolean) {
        store.edit { it[PRODUCT_METRICS] = enabled }
    }

    override suspend fun setTheme(theme: ThemePreference) {
        store.edit { it[THEME] = theme.name }
    }

    override suspend fun setRecognitionLocale(language: String?, country: String?) {
        require(language == null || language.matches(Regex("[a-z]{2,3}"))) { "Language must be a lowercase code: $language" }
        require(country == null || country.matches(Regex("[A-Z]{2}"))) { "Country must be ISO 3166-1 alpha-2: $country" }
        store.edit { prefs ->
            if (language == null) prefs.remove(LANGUAGE) else prefs[LANGUAGE] = language
            if (country == null) prefs.remove(COUNTRY) else prefs[COUNTRY] = country
        }
    }

    private companion object {
        val PRODUCT_METRICS = booleanPreferencesKey("product_metrics_enabled")
        val THEME = stringPreferencesKey("theme")
        val LANGUAGE = stringPreferencesKey("recognition_language")
        val COUNTRY = stringPreferencesKey("recognition_country")
    }
}
