package app.snackshot.domain.access

import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LogMealLocaleTest {
    private fun locales(vararg tags: String) = tags.map(Locale::forLanguageTag)

    @Test
    fun `every LogMeal language is mapped from its ISO code`() {
        val expected = mapOf(
            "es-ES" to "spa", "ca-ES" to "cat", "en-GB" to "eng", "it-IT" to "ita", "nl-NL" to "nld",
            "fr-FR" to "fre", "de-DE" to "ger", "tr-TR" to "tur", "el-GR" to "gre", "he-IL" to "heb",
        )
        expected.forEach { (tag, code) -> assertEquals(code, LogMealLocale.resolve(locales(tag)).language, tag) }
        assertEquals(10, LogMealLocale.supportedLanguages.size)
    }

    @Test
    fun `the old Hebrew code some devices report still maps`() {
        assertEquals("heb", LogMealLocale.logMealLanguage("iw"))
    }

    @Test
    fun `an unsupported language falls back to the next preferred one, then to English`() {
        assertEquals(RecognitionLocale("ger", "RS"), LogMealLocale.resolve(locales("sr-RS", "de-DE")))
        assertEquals(RecognitionLocale("eng", "RS"), LogMealLocale.resolve(locales("sr-Latn-RS")))
        assertEquals(RecognitionLocale("eng", "JP"), LogMealLocale.resolve(locales("ja-JP")))
    }

    @Test
    fun `the country comes from the locale, then telephony, then the fallback`() {
        assertEquals("GB", LogMealLocale.resolve(locales("en-GB"), telephonyCountry = "rs").country)
        assertEquals("RS", LogMealLocale.resolve(locales("en"), telephonyCountry = "rs").country)
        assertEquals("ES", LogMealLocale.resolve(locales("en", "es-ES")).country)
        assertEquals("US", LogMealLocale.resolve(locales("en"), telephonyCountry = "").country)
        assertEquals(RecognitionLocale("eng", "US"), LogMealLocale.resolve(emptyList()))
    }

    @Test
    fun `numeric regions such as Latin America are not ISO countries`() {
        assertEquals("MX", LogMealLocale.resolve(locales("es-419"), telephonyCountry = "MX").country)
    }

    @Test
    fun `a Settings choice wins over the device`() {
        val device = locales("en-US")
        assertEquals(RecognitionLocale("spa", "ES"), LogMealLocale.resolve(device, chosenLanguage = "spa", chosenCountry = "ES"))
        assertEquals(RecognitionLocale("ita", "US"), LogMealLocale.resolve(device, chosenLanguage = "it"))
        assertEquals("eng", LogMealLocale.resolve(device, chosenLanguage = "xx").language, "ignored when unsupported")
    }

    @Test
    fun `unknown codes map to nothing`() {
        assertNull(LogMealLocale.logMealLanguage("sr"))
    }

    @Test
    fun `the broker's formats are enforced`() {
        assertThrows<IllegalArgumentException> { RecognitionLocale("en", "US") }
        assertThrows<IllegalArgumentException> { RecognitionLocale("eng", "us") }
    }
}
