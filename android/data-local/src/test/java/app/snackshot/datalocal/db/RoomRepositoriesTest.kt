package app.snackshot.datalocal.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.snackshot.domain.meal.MealEdit
import app.snackshot.domain.meal.MealEntry
import app.snackshot.domain.meal.MealId
import app.snackshot.domain.meal.MealSource
import app.snackshot.domain.meal.MealType
import app.snackshot.domain.meal.SyncStatus
import app.snackshot.domain.metrics.MetricCounter
import app.snackshot.domain.metrics.MetricSample
import app.snackshot.domain.nutrition.CalorieRange
import app.snackshot.domain.nutrition.NutritionGoals
import app.snackshot.domain.nutrition.NutritionValues
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomRepositoriesTest {
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SnackshotDatabase::class.java).build()
    private val clock = Clock.fixed(Instant.parse("2026-11-26T12:00:00Z"), ZoneOffset.UTC)
    private val meals = RoomMealRepository(db, clock)
    private val sync = RoomSyncStateRepository(db, clock)
    private val goals = RoomGoalsRepository(db)
    private val metrics = RoomMetricsStore(db)

    @After fun close() = db.close()

    private fun meal(id: String, eatenAt: String, kcal: Double = 450.0) = MealEntry(
        id = MealId(id),
        source = MealSource.PHOTO,
        name = "Grilled chicken salad",
        mealType = MealType.LUNCH,
        eatenAt = Instant.parse(eatenAt),
        zoneOffset = ZoneOffset.ofHours(1),
        grams = 350.0,
        values = NutritionValues(kcal, 38.0, 14.0, 26.0),
        imageRef = "images/$id.jpg",
        confidence = 0.86,
        edit = MealEdit.GRAMS_ONLY,
        createdAt = Instant.parse("2026-11-26T11:05:00Z"),
        updatedAt = Instant.parse("2026-11-26T11:05:00Z"),
    )

    private val quickAdd = MealEntry(
        id = MealId("quick"),
        source = MealSource.QUICK_ADD,
        name = "",
        mealType = MealType.SNACK,
        eatenAt = Instant.parse("2026-11-26T16:00:00Z"),
        zoneOffset = ZoneOffset.UTC,
        grams = null,
        values = NutritionValues(energyKcal = 230.0),
        imageRef = null,
        confidence = null,
        edit = null,
        createdAt = Instant.parse("2026-11-26T16:00:00Z"),
        updatedAt = Instant.parse("2026-11-26T16:00:00Z"),
    )

    @Test
    fun `meals round-trip including unknown values`() = runTest {
        meals.save(meal("a", "2026-11-26T11:02:00Z"))
        meals.save(quickAdd)
        assertEquals(meal("a", "2026-11-26T11:02:00Z"), meals.getMeal(MealId("a")))
        assertEquals(quickAdd, meals.getMeal(MealId("quick")))
        assertNull(meals.getMeal(MealId("missing")))
    }

    @Test
    fun `range query is half-open and newest first`() = runTest {
        meals.save(meal("morning", "2026-11-26T08:00:00Z"))
        meals.save(meal("noon", "2026-11-26T12:00:00Z"))
        meals.save(meal("tomorrow", "2026-11-27T00:00:00Z"))
        val day = meals.observeMeals(Instant.parse("2026-11-26T00:00:00Z"), Instant.parse("2026-11-27T00:00:00Z")).first()
        assertEquals(listOf("noon", "morning"), day.map { it.id.value })
    }

    @Test
    fun `observers see saves and deletes`() = runTest {
        val flow = meals.observeMeal(MealId("a"))
        assertNull(flow.first())
        meals.save(meal("a", "2026-11-26T11:02:00Z"))
        assertEquals(450.0, flow.first()!!.energyKcal, 0.0)
        meals.delete(MealId("a"))
        assertNull(flow.first())
    }

    @Test
    fun `save queues an upload and every edit raises the version`() = runTest {
        meals.save(meal("a", "2026-11-26T11:02:00Z"))
        assertEquals(1L, sync.get(MealId("a"))!!.clientRecordVersion)
        sync.markUploaded(MealId("a"), uploadedVersion = 1, healthConnectRecordId = "hc-a")
        assertEquals(SyncStatus.SYNCED, sync.get(MealId("a"))!!.status)
        assertTrue(sync.pending().isEmpty())

        meals.save(meal("a", "2026-11-26T11:02:00Z", kcal = 300.0))
        val state = sync.get(MealId("a"))!!
        assertEquals(2L, state.clientRecordVersion)
        assertEquals(SyncStatus.PENDING_UPSERT, state.status)
        assertEquals("hc-a", state.healthConnectRecordId)
        assertEquals(listOf(state), sync.pending())
    }

    @Test
    fun `deleting a meal that never reached Health Connect leaves nothing to sync`() = runTest {
        meals.save(meal("a", "2026-11-26T11:02:00Z"))
        meals.delete(MealId("a"))
        assertNull(sync.get(MealId("a")))
    }

    @Test
    fun `deleting a synced meal queues a remote delete until it is done`() = runTest {
        meals.save(meal("a", "2026-11-26T11:02:00Z"))
        sync.markUploaded(MealId("a"), 1, "hc-a")
        meals.delete(MealId("a"))
        assertEquals(SyncStatus.PENDING_DELETE, sync.pending().single().status)
        sync.markRemoteDeleted(MealId("a"))
        assertNull(sync.get(MealId("a")))
    }

    @Test
    fun `undo after delete brings the meal back with a newer version`() = runTest {
        val saved = meal("a", "2026-11-26T11:02:00Z")
        meals.save(saved)
        sync.markUploaded(MealId("a"), 1, "hc-a")
        meals.delete(MealId("a"))
        meals.save(saved)
        val state = sync.get(MealId("a"))!!
        assertEquals(SyncStatus.PENDING_UPSERT, state.status)
        assertEquals(2L, state.clientRecordVersion)
        // A remote delete that finishes late must not drop the restored meal's metadata.
        sync.markRemoteDeleted(MealId("a"))
        assertEquals(state, sync.get(MealId("a")))
    }

    @Test
    fun `an upload of an older version keeps the newer edit pending`() = runTest {
        meals.save(meal("a", "2026-11-26T11:02:00Z"))
        meals.save(meal("a", "2026-11-26T11:02:00Z", kcal = 300.0))
        sync.markUploaded(MealId("a"), uploadedVersion = 1, healthConnectRecordId = "hc-a")
        assertEquals(SyncStatus.PENDING_UPSERT, sync.get(MealId("a"))!!.status)
    }

    @Test
    fun `goals start empty and can be set partly and cleared`() = runTest {
        assertEquals(NutritionGoals.NONE, goals.goals.first())
        val set = NutritionGoals(calories = CalorieRange(2070, 2530), proteinGrams = 150)
        goals.setGoals(set)
        assertEquals(set, goals.goals.first())
        goals.setGoals(NutritionGoals.NONE)
        assertEquals(NutritionGoals.NONE, goals.goals.first())
    }

    @Test
    fun `metric counters add up per day and samples keep every value`() = runTest {
        val day = LocalDate.parse("2026-11-25")
        metrics.increment(day, MetricCounter.MEALS_LOGGED)
        metrics.increment(day, MetricCounter.MEALS_LOGGED, by = 2)
        metrics.increment(day, MetricCounter.QUICK_ADDS)
        metrics.addSample(day, MetricSample.RECOGNITION_LATENCY_MS, 1800)
        metrics.addSample(day, MetricSample.RECOGNITION_LATENCY_MS, 2900)

        val daily = metrics.dailyMetrics(day)
        assertEquals(mapOf(MetricCounter.MEALS_LOGGED to 3L, MetricCounter.QUICK_ADDS to 1L), daily.counters)
        assertEquals(mapOf(MetricSample.RECOGNITION_LATENCY_MS to listOf(1800L, 2900L)), daily.samples)
    }

    @Test
    fun `only finished days are ready to report and reported days are removed`() = runTest {
        val yesterday = LocalDate.parse("2026-11-25")
        val today = LocalDate.parse("2026-11-26")
        metrics.addSample(LocalDate.parse("2026-11-24"), MetricSample.TIME_TO_LOG_SEC, 14)
        metrics.increment(yesterday, MetricCounter.MEALS_LOGGED)
        metrics.increment(today, MetricCounter.MEALS_LOGGED)
        assertEquals(listOf(LocalDate.parse("2026-11-24"), yesterday), metrics.daysBefore(today))

        metrics.deleteDay(yesterday)
        assertTrue(metrics.dailyMetrics(yesterday).counters.isEmpty())
        metrics.deleteAll()
        assertTrue(metrics.daysBefore(LocalDate.parse("2026-12-31")).isEmpty())
    }
}
