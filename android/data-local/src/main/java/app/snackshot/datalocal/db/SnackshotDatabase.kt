package app.snackshot.datalocal.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

/**
 * The local database in app-private storage, encrypted at rest by Android file-based
 * encryption (decision 8). Schema changes follow the migration strategy in the module README.
 */
@Database(
    entities = [
        MealEntity::class,
        SyncStateEntity::class,
        NutritionGoalsEntity::class,
        MetricCounterEntity::class,
        MetricSampleEntity::class,
    ],
    version = SnackshotDatabase.VERSION,
    exportSchema = true,
)
abstract class SnackshotDatabase : RoomDatabase() {
    abstract fun meals(): MealDao

    abstract fun syncState(): SyncStateDao

    abstract fun goals(): NutritionGoalsDao

    abstract fun metrics(): MetricsDao

    companion object {
        const val VERSION = 1
        const val FILE_NAME = "snackshot.db"

        /** Every hand-written migration, in order. Room's AutoMigrations are declared on @Database. */
        val MIGRATIONS: Array<Migration> = emptyArray()
    }
}
