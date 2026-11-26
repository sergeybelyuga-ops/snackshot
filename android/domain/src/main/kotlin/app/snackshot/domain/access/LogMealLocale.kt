package app.snackshot.domain.access

import java.util.Locale

/**
 * Picks the language and country LogMeal recognizes dishes for (WBS 4.2, doc 04 §4).
 *
 * - **Language:** the first of the user's languages that LogMeal supports (spa, cat, eng, ita, nld,
 *   fre, ger, tur, gre, heb), otherwise English.
 * - **Country:** the region of the user's locales, otherwise the SIM or network country, otherwise
 *   the United States.
 * - A language or country chosen in Settings wins over the device.
 */
object LogMealLocale {
    const val FALLBACK_LANGUAGE = "eng"
    const val FALLBACK_COUNTRY = "US"

    /** ISO 639-1 (and the old Hebrew code `iw` some devices report) to LogMeal codes. */
    private val LANGUAGES = mapOf(
        "es" to "spa",
        "ca" to "cat",
        "en" to "eng",
        "it" to "ita",
        "nl" to "nld",
        "fr" to "fre",
        "de" to "ger",
        "tr" to "tur",
        "el" to "gre",
        "he" to "heb",
        "iw" to "heb",
    )

    /** The codes LogMeal accepts. */
    val supportedLanguages: Set<String> = LANGUAGES.values.toSet()

    private val COUNTRY = Regex("[A-Z]{2}")

    /**
     * @param deviceLocales the user's locales in preference order.
     * @param telephonyCountry the SIM or network country (ISO 3166-1, any case), if known.
     * @param chosenLanguage the Settings choice: a LogMeal code, or null to follow the device.
     * @param chosenCountry the Settings choice, or null to follow the device.
     */
    fun resolve(
        deviceLocales: List<Locale>,
        telephonyCountry: String? = null,
        chosenLanguage: String? = null,
        chosenCountry: String? = null,
    ): RecognitionLocale {
        val language = chosenLanguage?.let(::logMealLanguage)
            ?: deviceLocales.firstNotNullOfOrNull { logMealLanguage(it.language) }
            ?: FALLBACK_LANGUAGE
        val country = sequenceOf(chosenCountry)
            .plus(deviceLocales.map { it.country })
            .plus(telephonyCountry?.uppercase(Locale.ROOT))
            .firstOrNull { it != null && it.matches(COUNTRY) }
            ?: FALLBACK_COUNTRY
        return RecognitionLocale(language, country)
    }

    /** A LogMeal code for an ISO 639 code or a LogMeal code, or null when LogMeal doesn't support it. */
    fun logMealLanguage(code: String): String? {
        val lower = code.lowercase(Locale.ROOT)
        return if (lower in supportedLanguages) lower else LANGUAGES[lower]
    }
}
