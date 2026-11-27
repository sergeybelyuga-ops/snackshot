package app.snackshot.datalocal.draft

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import app.snackshot.datalocal.images.AndroidPhotoPreprocessor
import app.snackshot.datalocal.images.FileMealImageStore
import app.snackshot.datalocal.images.ImageDirectories
import app.snackshot.domain.meal.MealImageStore
import app.snackshot.domain.meal.PendingMealStore
import app.snackshot.domain.meal.PhotoPreprocessor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MealFlowModule {
    @Binds
    abstract fun mealImageStore(impl: FileMealImageStore): MealImageStore

    @Binds
    abstract fun pendingMealStore(impl: DataStorePendingMealStore): PendingMealStore

    @Binds
    abstract fun photoPreprocessor(impl: AndroidPhotoPreprocessor): PhotoPreprocessor

    companion object {
        @Provides
        @Singleton
        fun imageDirectories(@ApplicationContext context: Context) = ImageDirectories(
            processed = File(context.filesDir, FileMealImageStore.PROCESSED_DIR),
            captures = File(context.cacheDir, "captures"),
        )

        /** App-private and excluded from backup (data_extraction_rules.xml), like the other stores. */
        @Provides
        @Singleton
        @PendingMealDataStore
        fun pendingMealDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("pending_meal") }
    }
}
