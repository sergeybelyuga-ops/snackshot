package app.snackshot.datalocal.images

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.snackshot.datalocal.TEST_JPEG
import app.snackshot.datalocal.db.RoomMealRepository
import app.snackshot.datalocal.db.SnackshotDatabase
import app.snackshot.datalocal.testMeal
import app.snackshot.datalocal.testReviewDraft
import app.snackshot.domain.meal.MealId
import app.snackshot.domain.meal.PendingMeal
import app.snackshot.domain.meal.PendingMealStore
import app.snackshot.domain.meal.RetentionReport
import java.io.File
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ImageRetentionCleanerTest {
    @get:Rule val tmp = TemporaryFolder()

    private val now = Instant.parse("2026-11-26T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SnackshotDatabase::class.java).build()
    private val meals = RoomMealRepository(db, clock)
    private val dirs by lazy { ImageDirectories(File(tmp.root, "meal-images"), File(tmp.root, "captures")) }
    private val images by lazy { FileMealImageStore(dirs) }

    private val pendingFlow = MutableStateFlow<PendingMeal?>(null)
    private val pending = object : PendingMealStore {
        override val pending = pendingFlow

        override suspend fun save(meal: PendingMeal) {
            pendingFlow.value = meal
        }

        override suspend fun complete() {
            pendingFlow.value = null
        }

        override suspend fun discard() {
            pendingFlow.value = null
        }
    }

    private val cleaner by lazy { ImageRetentionCleaner(dirs, db, pending, clock) }

    @After fun close() = db.close()

    private suspend fun photo(age: Duration): String = images.saveProcessed(TEST_JPEG).also { ref ->
        images.fileFor(ref)!!.setLastModified(now.minus(age).toEpochMilli())
    }

    @Test
    fun `photos older than 365 days are deleted and the meal keeps its values`() = runTest {
        val old = photo(Duration.ofDays(366))
        val recent = photo(Duration.ofDays(364))
        meals.save(testMeal("old", old))
        meals.save(testMeal("recent", recent))
        val versionBefore = db.syncState().get("old")!!.clientRecordVersion

        assertEquals(RetentionReport(expiredDeleted = 1, orphansDeleted = 0, capturesDeleted = 0, mealRefsCleared = 1), cleaner.run())

        assertNull(images.fileFor(old))
        val oldMeal = meals.getMeal(MealId("old"))!!
        assertNull(oldMeal.imageRef)
        assertEquals(450.0, oldMeal.energyKcal, 0.0)
        assertEquals("No sync change for a photo", versionBefore, db.syncState().get("old")!!.clientRecordVersion)
        assertEquals(recent, meals.getMeal(MealId("recent"))!!.imageRef)
        assertNotNull(images.fileFor(recent))
    }

    @Test
    fun `unreferenced photos go after a day, the pending draft's photo stays`() = runTest {
        val orphan = photo(Duration.ofHours(25))
        val fresh = photo(Duration.ofHours(2))
        val draftPhoto = photo(Duration.ofDays(3))
        pending.save(PendingMeal.Reviewing(testReviewDraft(draftPhoto)))

        assertEquals(1, cleaner.run().orphansDeleted)
        assertNull(images.fileFor(orphan))
        assertNotNull(images.fileFor(fresh)) // may belong to a flow that is just saving
        assertNotNull(images.fileFor(draftPhoto))
    }

    @Test
    fun `leftover raw captures are deleted after a day`() = runTest {
        val old = images.newCaptureFile().apply { setLastModified(now.minus(Duration.ofDays(2)).toEpochMilli()) }
        val current = images.newCaptureFile().apply { setLastModified(now.minus(Duration.ofMinutes(1)).toEpochMilli()) }
        assertEquals(1, cleaner.run().capturesDeleted)
        assertFalse(old.exists())
        assertTrue(current.exists())
    }

    @Test
    fun `meals pointing at a missing photo are cleared`() = runTest {
        meals.save(testMeal("gone", "meal-images/00000000-0000-0000-0000-00000000dead.jpg"))
        assertEquals(1, cleaner.run().mealRefsCleared)
        assertNull(meals.getMeal(MealId("gone"))!!.imageRef)
    }

    @Test
    fun `interrupted writes are removed`() = runTest {
        dirs.processed.mkdirs()
        val partial = File(dirs.processed, "00000000-0000-0000-0000-000000000001.jpg.tmp").apply {
            writeBytes(TEST_JPEG)
            setLastModified(now.minus(Duration.ofDays(2)).toEpochMilli())
        }
        cleaner.run()
        assertFalse(partial.exists())
    }

    @Test
    fun `nothing to do on a fresh install`() = runTest {
        assertEquals(RetentionReport(0, 0, 0, 0), cleaner.run())
    }
}
