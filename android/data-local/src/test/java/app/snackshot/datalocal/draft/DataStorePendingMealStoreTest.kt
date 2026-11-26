package app.snackshot.datalocal.draft

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.snackshot.datalocal.TEST_JPEG
import app.snackshot.datalocal.db.RoomMealRepository
import app.snackshot.datalocal.db.SnackshotDatabase
import app.snackshot.datalocal.images.FileMealImageStore
import app.snackshot.datalocal.images.ImageDirectories
import app.snackshot.datalocal.testDataStore
import app.snackshot.datalocal.testMeal
import app.snackshot.datalocal.testReviewDraft
import app.snackshot.domain.meal.MealDraft
import app.snackshot.domain.meal.MealSource
import app.snackshot.domain.meal.PendingMeal
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DataStorePendingMealStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SnackshotDatabase::class.java).build()
    private val images by lazy { FileMealImageStore(ImageDirectories(File(tmp.root, "meal-images"), File(tmp.root, "captures"))) }
    private val file by lazy { tmp.root.resolve("pending_meal.preferences_pb") }

    @After fun close() = db.close()

    /** One app process: a DataStore on the draft file for the duration of [block]. */
    private suspend fun TestScope.process(block: suspend (DataStorePendingMealStore) -> Unit) {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        try {
            block(DataStorePendingMealStore(testDataStore(file, scope), images, db))
        } finally {
            scope.cancel()
        }
    }

    private fun awaiting(ref: String) =
        PendingMeal.AwaitingRecognition(MealSource.PHOTO, ref, Instant.parse("2026-11-26T11:02:00Z"), ZoneOffset.ofHours(1))

    @Test
    fun `the draft survives process death`() = runTest {
        val ref = images.saveProcessed(TEST_JPEG)
        val draft = PendingMeal.Reviewing(testReviewDraft(ref).withGrams(200.0))
        process { it.save(draft) }
        process { assertEquals(draft, it.pending.first()) }
    }

    @Test
    fun `nothing is pending on a fresh install`() = runTest {
        process { assertNull(it.pending.first()) }
    }

    @Test
    fun `recognition result replaces the waiting photo and keeps the image`() = runTest {
        val ref = images.saveProcessed(TEST_JPEG)
        process {
            it.save(awaiting(ref))
            it.save(PendingMeal.Reviewing(testReviewDraft(ref)))
        }
        assertNotNull(images.fileFor(ref))
    }

    @Test
    fun `a retake deletes the previous photo`() = runTest {
        val first = images.saveProcessed(TEST_JPEG)
        val second = images.saveProcessed(TEST_JPEG)
        process {
            it.save(awaiting(first))
            it.save(awaiting(second))
        }
        assertNull(images.fileFor(first))
        assertNotNull(images.fileFor(second))
    }

    @Test
    fun `discard deletes the draft and its photo`() = runTest {
        val ref = images.saveProcessed(TEST_JPEG)
        process {
            it.save(awaiting(ref))
            it.discard()
            assertNull(it.pending.first())
        }
        assertNull(images.fileFor(ref))
    }

    @Test
    fun `complete keeps the photo for the saved meal`() = runTest {
        val ref = images.saveProcessed(TEST_JPEG)
        process {
            it.save(PendingMeal.Reviewing(testReviewDraft(ref)))
            it.complete()
            assertNull(it.pending.first())
        }
        assertNotNull(images.fileFor(ref))
    }

    @Test
    fun `discarding an edit of a saved meal keeps that meal's photo`() = runTest {
        val ref = images.saveProcessed(TEST_JPEG)
        val saved = testMeal("saved", ref)
        RoomMealRepository(db, Clock.systemUTC()).save(saved)
        process {
            it.save(PendingMeal.Reviewing(MealDraft.fromEntry(saved).withGrams(100.0)))
            it.discard()
        }
        assertNotNull(images.fileFor(ref))
    }
}
