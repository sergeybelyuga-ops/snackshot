package app.snackshot.datalocal.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.snackshot.datalocal.testDataStore
import app.snackshot.domain.access.AiAccess
import app.snackshot.domain.access.AiAccessStatus
import app.snackshot.domain.access.CachedAccess
import app.snackshot.domain.access.PictureAllowance
import java.io.File
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreAiAccessCacheTest {
    @get:Rule val tmp = TemporaryFolder()

    private val sample = CachedAccess(
        AiAccess(
            AiAccessStatus.TRIAL,
            "provisioned",
            PictureAllowance(3, 90),
            isTest = true,
            aiEnabled = true,
            checkedAt = Instant.parse("2026-11-27T10:00:00Z"),
        ),
        tokenVersion = 2,
    )

    private suspend fun TestScope.withCache(file: File, block: suspend (DataStoreAiAccessCache, DataStore<Preferences>) -> Unit) {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        try {
            val store = testDataStore(file, scope)
            block(DataStoreAiAccessCache(store), store)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `empty until written, then survives a restart, then clears`() = runTest {
        val file = tmp.root.resolve("a.preferences_pb")
        withCache(file) { cache, _ ->
            assertNull(cache.read())
            cache.write(sample)
            assertEquals(sample, cache.cached.first())
        }
        withCache(file) { cache, _ ->
            assertEquals(sample, cache.read())
            cache.clear()
            assertNull(cache.read())
        }
    }

    @Test
    fun `unreadable or unknown data counts as no cache`() = runTest {
        withCache(tmp.root.resolve("b.preferences_pb")) { cache, store ->
            val key = stringPreferencesKey("ai_access_v1")
            store.edit { it[key] = "not json" }
            assertNull(cache.read())
            store.edit {
                it[key] =
                    """{"status":"SOMETHING_NEW","picturesUsed":1,"pictureLimit":90,"isTest":false,"aiEnabled":true,"checkedAtMillis":0,"tokenVersion":1}"""
            }
            assertNull(cache.read())
        }
    }
}
