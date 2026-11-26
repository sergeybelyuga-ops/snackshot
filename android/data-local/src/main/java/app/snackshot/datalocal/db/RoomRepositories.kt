package app.snackshot.datalocal.db

import androidx.room.withTransaction
import app.snackshot.domain.meal.MealEntry
import app.snackshot.domain.meal.MealId
import app.snackshot.domain.meal.MealRepository
import app.snackshot.domain.meal.SyncState
import app.snackshot.domain.meal.SyncStateRepository
import app.snackshot.domain.meal.SyncStatus
import app.snackshot.domain.metrics.DailyMetrics
import app.snackshot.domain.metrics.MetricCounter
import app.snackshot.domain.metrics.MetricSample
import app.snackshot.domain.metrics.MetricsStore
import app.snackshot.domain.nutrition.GoalsRepository
import app.snackshot.domain.nutrition.NutritionGoals
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class RoomMealRepository @Inject constructor(private val db: SnackshotDatabase, private val clock: Clock) : MealRepository {
    private val meals = db.meals()
    private val sync = db.syncState()

    override fun observeMeals(from: Instant, until: Instant): Flow<List<MealEntry>> =
        meals.observeRange(from.toEpochMilli(), until.toEpochMilli()).map { list -> list.map { it.toDomain() } }

    override fun observeMeal(id: MealId): Flow<MealEntry?> = meals.observe(id.value).map { it?.toDomain() }

    override suspend fun getMeal(id: MealId): MealEntry? = meals.get(id.value)?.toDomain()

    override suspend fun save(entry: MealEntry) = db.withTransaction {
        meals.upsert(entry.toEntity())
        val state = sync.get(entry.id.value)?.toDomain()?.edited() ?: SyncState.newMeal(entry.id)
        sync.upsert(state.toEntity(clock.instant()))
    }

    override suspend fun delete(id: MealId) = db.withTransaction {
        meals.delete(id.value)
        val state = sync.get(id.value)?.toDomain() ?: return@withTransaction
        val next = state.deleted()
        if (next == null) sync.delete(id.value) else sync.upsert(next.toEntity(clock.instant()))
    }
}

class RoomSyncStateRepository @Inject constructor(private val db: SnackshotDatabase, private val clock: Clock) : SyncStateRepository {
    private val sync = db.syncState()

    override suspend fun get(mealId: MealId): SyncState? = sync.get(mealId.value)?.toDomain()

    override suspend fun pending(): List<SyncState> = sync.pending().map { it.toDomain() }

    override suspend fun markUploaded(mealId: MealId, uploadedVersion: Long, healthConnectRecordId: String) = db.withTransaction {
        val state = sync.get(mealId.value)?.toDomain() ?: return@withTransaction
        val next = state.uploaded(uploadedVersion, healthConnectRecordId)
        if (next != state) sync.upsert(next.toEntity(clock.instant()))
    }

    override suspend fun markRemoteDeleted(mealId: MealId) = db.withTransaction {
        val state = sync.get(mealId.value)?.toDomain() ?: return@withTransaction
        if (state.status == SyncStatus.PENDING_DELETE) sync.delete(mealId.value)
    }
}

class RoomGoalsRepository @Inject constructor(db: SnackshotDatabase) : GoalsRepository {
    private val dao = db.goals()

    override val goals: Flow<NutritionGoals> = dao.observe().map { it.toDomain() }.distinctUntilChanged()

    override suspend fun setGoals(goals: NutritionGoals) = dao.upsert(goals.toEntity())
}

class RoomMetricsStore @Inject constructor(db: SnackshotDatabase) : MetricsStore {
    private val dao = db.metrics()

    override suspend fun increment(day: LocalDate, counter: MetricCounter, by: Long) {
        require(by > 0) { "Counters only grow: $by" }
        dao.increment(day.toString(), counter.name, by)
    }

    override suspend fun addSample(day: LocalDate, sample: MetricSample, value: Long) {
        require(value >= 0) { "Samples are durations: $value" }
        dao.insertSample(MetricSampleEntity(day = day.toString(), sample = sample.name, value = value))
    }

    override suspend fun daysBefore(today: LocalDate): List<LocalDate> = dao.daysBefore(today.toString()).map(LocalDate::parse)

    override suspend fun dailyMetrics(day: LocalDate): DailyMetrics {
        val key = day.toString()
        // Names this build no longer knows (after a downgrade) are skipped, not fatal.
        val counters = dao.counters(key).mapNotNull { row ->
            MetricCounter.entries.firstOrNull { it.name == row.counter }?.let { it to row.value }
        }.toMap()
        val samples = dao.samples(key).mapNotNull { row ->
            MetricSample.entries.firstOrNull { it.name == row.sample }?.let { it to row.value }
        }.groupBy({ it.first }, { it.second })
        return DailyMetrics(day, counters, samples)
    }

    override suspend fun deleteDay(day: LocalDate) = dao.deleteDay(day.toString())

    override suspend fun deleteAll() = dao.deleteAll()
}
