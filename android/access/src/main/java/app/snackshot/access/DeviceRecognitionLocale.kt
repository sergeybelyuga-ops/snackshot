package app.snackshot.access

import android.content.Context
import android.os.LocaleList
import android.telephony.TelephonyManager
import app.snackshot.domain.access.LogMealLocale
import app.snackshot.domain.access.RecognitionLocale
import app.snackshot.domain.access.RecognitionLocaleSource
import app.snackshot.domain.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * The recognition locale for provisioning (WBS 4.2): the Settings choice, else the device's
 * languages and region, else the SIM or network country. Telephony needs no permission here.
 */
class DeviceRecognitionLocale @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
) : RecognitionLocaleSource {
    override suspend fun current(): RecognitionLocale {
        val chosen = settings.settings.first()
        val list = LocaleList.getDefault()
        return LogMealLocale.resolve(
            deviceLocales = (0 until list.size()).map { list[it] },
            telephonyCountry = telephonyCountry(),
            chosenLanguage = chosen.recognitionLanguage,
            chosenCountry = chosen.recognitionCountry,
        )
    }

    private fun telephonyCountry(): String? {
        val telephony = context.getSystemService(TelephonyManager::class.java) ?: return null
        return telephony.simCountryIso?.takeIf { it.isNotBlank() } ?: telephony.networkCountryIso?.takeIf { it.isNotBlank() }
    }
}
