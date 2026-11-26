package app.snackshot.datalocal.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Query("SELECT * FROM meals WHERE eaten_at >= :from AND eaten_at < :until ORDER BY eaten_at DESC, created_at DESC")
    fun observeRange(from: Long, until: Long): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals WHERE id = :id")
    fun observe(id: String): Flow<MealEntity?>

    @Query("SELECT * FROM meals WHERE id = :id")
    suspend fun get(id: String): MealEntity?

    @Upsert
    suspend fun upsert(meal: MealEntity)

    @Query("DELETE FROM meals WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface SyncStateDao {
    @Query("SELECT * FROM sync_state WHERE meal_id = :mealId")
    suspend fun get(mealId: String): SyncStateEntity?

    @Query("SELECT * FROM sync_state WHERE status != 'SYNCED' ORDER BY changed_at, meal_id")
    suspend fun pending(): List<SyncStateEntity>

    @Upsert
    suspend fun upsert(state: SyncStateEntity)

    @Query("DELETE FROM sync_state WHERE meal_id = :mealId")
    suspend fun delete(mealId: String)
}

@Dao
interface NutritionGoalsDao {
    @Query("SELECT * FROM nutrition_goals WHERE id = ${NutritionGoalsEntity.ID}")
    fun observe(): Flow<NutritionGoalsEntity?>

    @Upsert
    suspend fun upsert(goals: NutritionGoalsEntity)
}

@Dao
interface MetricsDao {
    // minSdk 28 ships SQLite 3.22, which has no INSERT … ON CONFLICT DO UPDATE: update, then insert.
    @Query("UPDATE metric_counters SET value = value + :by WHERE day = :day AND counter = :counter")
    suspend fun addToCounter(day: String, counter: String, by: Long): Int

    @Insert
    suspend fun insertCounter(counter: MetricCounterEntity)

    @Transaction
    suspend fun increment(day: String, counter: String, by: Long) {
        if (addToCounter(day, counter, by) == 0) insertCounter(MetricCounterEntity(day, counter, by))
    }

    @Insert
    suspend fun insertSample(sample: MetricSampleEntity)

    @Query("SELECT * FROM metric_counters WHERE day = :day")
    suspend fun counters(day: String): List<MetricCounterEntity>

    @Query("SELECT * FROM metric_samples WHERE day = :day ORDER BY id")
    suspend fun samples(day: String): List<MetricSampleEntity>

    // ISO dates sort as text, so `<` compares days.
    @Query("SELECT day FROM metric_counters WHERE day < :today UNION SELECT day FROM metric_samples WHERE day < :today ORDER BY day")
    suspend fun daysBefore(today: String): List<String>

    @Query("DELETE FROM metric_counters WHERE day = :day")
    suspend fun deleteCounters(day: String)

    @Query("DELETE FROM metric_samples WHERE day = :day")
    suspend fun deleteSamples(day: String)

    @Transaction
    suspend fun deleteDay(day: String) {
        deleteCounters(day)
        deleteSamples(day)
    }

    @Query("DELETE FROM metric_counters")
    suspend fun deleteAllCounters()

    @Query("DELETE FROM metric_samples")
    suspend fun deleteAllSamples()

    @Transaction
    suspend fun deleteAll() {
        deleteAllCounters()
        deleteAllSamples()
    }
}
