package app.snackshot.datalocal.settings

import app.snackshot.datalocal.testDataStore
import app.snackshot.domain.settings.AppSettings
import app.snackshot.domain.settings.ThemePreference
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreSettingsRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    private suspend fun TestScope.withRepository(file: File, block: suspend (DataStoreSettingsRepository) -> Unit) {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        try {
            block(DataStoreSettingsRepository(testDataStore(file, scope)))
        } finally {
            scope.cancel()
        }
    }

    private suspend fun assertRejected(block: suspend () -> Unit) {
        val error = runCatching { block() }.exceptionOrNull()
        assertTrue("Expected IllegalArgumentException, got $error", error is IllegalArgumentException)
    }

    @Test
    fun `defaults are metrics on, system theme and device locale`() = runTest {
        withRepository(tmp.root.resolve("a.preferences_pb")) { assertEquals(AppSettings(), it.settings.first()) }
    }

    @Test
    fun `changes survive a restart`() = runTest {
        val file = tmp.root.resolve("b.preferences_pb")
        withRepository(file) {
            it.setProductMetricsEnabled(false)
            it.setTheme(ThemePreference.DARK)
            it.setRecognitionLocale("eng", "RS")
        }
        withRepository(file) {
            assertEquals(AppSettings(false, ThemePreference.DARK, "eng", "RS"), it.settings.first())
            it.setRecognitionLocale(null, null)
            assertEquals(AppSettings(false, ThemePreference.DARK, null, null), it.settings.first())
        }
    }

    @Test
    fun `malformed locale codes are rejected`() = runTest {
        withRepository(tmp.root.resolve("c.preferences_pb")) { repo ->
            assertRejected { repo.setRecognitionLocale("English", null) }
            assertRejected { repo.setRecognitionLocale(null, "rs") }
            assertEquals(AppSettings(), repo.settings.first())
        }
    }
}
