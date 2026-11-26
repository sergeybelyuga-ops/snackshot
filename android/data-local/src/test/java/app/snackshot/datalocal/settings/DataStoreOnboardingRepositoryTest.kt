package app.snackshot.datalocal.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreOnboardingRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun repository(file: File, scope: CoroutineScope) =
        DataStoreOnboardingRepository(PreferenceDataStoreFactory.create(scope = scope) { file })

    @Test
    fun `welcome is not completed on a fresh install`() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        assertFalse(repository(tmp.root.resolve("a.preferences_pb"), scope).welcomeCompleted.first())
        scope.cancel()
    }

    @Test
    fun `completing welcome survives a restart`() = runTest {
        val file = tmp.root.resolve("b.preferences_pb")
        val first = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        repository(file, first).completeWelcome()
        first.cancel() // the "app" stops; a new DataStore reads the same file

        val second = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        assertTrue(repository(file, second).welcomeCompleted.first())
        second.cancel()
    }
}
