package app.snackshot.datalocal.db

import android.content.Context
import androidx.room.Room
import app.snackshot.domain.meal.MealRepository
import app.snackshot.domain.meal.SyncStateRepository
import app.snackshot.domain.metrics.MetricsStore
import app.snackshot.domain.nutrition.GoalsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {
    @Binds
    abstract fun mealRepository(impl: RoomMealRepository): MealRepository

    @Binds
    abstract fun syncStateRepository(impl: RoomSyncStateRepository): SyncStateRepository

    @Binds
    abstract fun goalsRepository(impl: RoomGoalsRepository): GoalsRepository

    @Binds
    abstract fun metricsStore(impl: RoomMetricsStore): MetricsStore

    companion object {
        /**
         * `databases/snackshot.db` in app-private storage, excluded from backup
         * (data_extraction_rules.xml). No destructive fallback: a missing migration must fail
         * loudly in testing rather than wipe a user's meal log.
         */
        @Provides
        @Singleton
        fun database(@ApplicationContext context: Context): SnackshotDatabase =
            Room.databaseBuilder(context, SnackshotDatabase::class.java, SnackshotDatabase.FILE_NAME)
                .addMigrations(*SnackshotDatabase.MIGRATIONS)
                .build()

        @Provides
        @Singleton
        fun clock(): Clock = Clock.systemUTC()
    }
}
