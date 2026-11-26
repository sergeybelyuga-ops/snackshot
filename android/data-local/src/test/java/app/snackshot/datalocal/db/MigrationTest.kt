package app.snackshot.datalocal.db

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Migration strategy check (module README): every released schema version is exported, and the
 * oldest one migrates to the current version with the registered migrations and validates
 * against the exported current schema.
 */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SnackshotDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun `every schema version is exported`() {
        val dir = File("schemas/${SnackshotDatabase::class.java.name}")
        (1..SnackshotDatabase.VERSION).forEach { version ->
            assertTrue("Missing exported schema $version.json: commit the schemas directory", dir.resolve("$version.json").isFile)
        }
    }

    @Test
    fun `oldest schema migrates to the current one`() {
        helper.createDatabase(dbPath, 1).close()
        helper.runMigrationsAndValidate(dbPath, SnackshotDatabase.VERSION, true, *SnackshotDatabase.MIGRATIONS).close()
    }

    // An absolute path: with a bare name Room 2.8's helper resolves the file twice under
    // Robolectric and rejects the mismatch ("driver is configured to open a database named …").
    private val dbPath = ApplicationProvider.getApplicationContext<Context>().getDatabasePath("migration-test.db").absolutePath
}
