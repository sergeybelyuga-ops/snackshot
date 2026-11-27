package app.snackshot.datalocal.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import app.snackshot.datalocal.secure.AesGcmCipher
import app.snackshot.datalocal.secure.DataStoreLogMealTokenStore
import app.snackshot.datalocal.secure.KeystoreKeys
import app.snackshot.domain.access.AiAccessCache
import app.snackshot.domain.access.LogMealTokenStore
import app.snackshot.domain.onboarding.OnboardingRepository
import app.snackshot.domain.settings.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Qualifier
import javax.inject.Singleton

/** The `settings` DataStore: preferences and onboarding flags. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SettingsStore

/** The `secrets` DataStore: encrypted blobs only. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SecretsStore

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {
    @Binds
    abstract fun onboardingRepository(impl: DataStoreOnboardingRepository): OnboardingRepository

    @Binds
    abstract fun settingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds
    abstract fun logMealTokenStore(impl: DataStoreLogMealTokenStore): LogMealTokenStore

    @Binds
    abstract fun aiAccessCache(impl: DataStoreAiAccessCache): AiAccessCache

    companion object {
        // Both files live in app-private storage and are excluded from backup (data_extraction_rules.xml).

        @Provides
        @Singleton
        @SettingsStore
        fun settingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }

        @Provides
        @Singleton
        @SecretsStore
        fun secretsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("secrets") }

        @Provides
        @Singleton
        @Named(DataStoreLogMealTokenStore.CIPHER)
        fun tokenCipher(): AesGcmCipher = AesGcmCipher { KeystoreKeys.getOrCreate(DataStoreLogMealTokenStore.KEY_ALIAS) }
    }
}
