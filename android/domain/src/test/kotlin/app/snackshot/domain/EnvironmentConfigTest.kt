package app.snackshot.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class EnvironmentConfigTest {
    private fun config(broker: String, emulators: Boolean, appCheck: AppCheckProvider = AppCheckProvider.DEBUG, atwater: Boolean = false) =
        EnvironmentConfig(
            name = "test",
            brokerBaseUrl = broker,
            logmealBaseUrl = if (emulators) "http://localhost:8090" else "https://api.logmeal.com",
            useFirebaseEmulators = emulators,
            appCheckProvider = appCheck,
            showAtwaterComparison = atwater,
        )

    @Test
    fun `local flavor may use cleartext localhost`() {
        config("http://localhost:5001/demo-snackshot/europe-west3/api", emulators = true, appCheck = AppCheckProvider.NONE)
    }

    @Test
    fun `cloud flavors must use https`() {
        assertThrows<IllegalArgumentException> { config("http://example.com/api", emulators = false) }
    }

    @Test
    fun `cloud flavors must use App Check`() {
        assertThrows<IllegalArgumentException> {
            config("https://example.com/api", emulators = false, appCheck = AppCheckProvider.NONE)
        }
    }

    @Test
    fun `Atwater comparison is not allowed in the Play Integrity build`() {
        config("https://example.com/api", emulators = false, atwater = true)
        assertThrows<IllegalArgumentException> {
            config("https://example.com/api", emulators = false, appCheck = AppCheckProvider.PLAY_INTEGRITY, atwater = true)
        }
    }

    @Test
    fun `maps BuildConfig provider names`() {
        assertEquals(AppCheckProvider.PLAY_INTEGRITY, AppCheckProvider.fromBuildConfig("playIntegrity"))
        assertThrows<IllegalArgumentException> { AppCheckProvider.fromBuildConfig("other") }
    }
}
