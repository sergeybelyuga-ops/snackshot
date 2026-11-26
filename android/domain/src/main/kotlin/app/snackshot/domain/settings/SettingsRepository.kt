package app.snackshot.domain.settings

import kotlinx.coroutines.flow.Flow

enum class ThemePreference { SYSTEM, LIGHT, DARK }

/**
 * Local preferences from S-16 Settings.
 *
 * @property recognitionLanguage LogMeal language code (for example `eng`); null follows the device.
 * @property recognitionCountry ISO 3166-1 alpha-2 country (for example `RS`); null follows the device.
 */
data class AppSettings(
    val productMetricsEnabled: Boolean = true,
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val recognitionLanguage: String? = null,
    val recognitionCountry: String? = null,
)

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setProductMetricsEnabled(enabled: Boolean)

    suspend fun setTheme(theme: ThemePreference)

    suspend fun setRecognitionLocale(language: String?, country: String?)
}
