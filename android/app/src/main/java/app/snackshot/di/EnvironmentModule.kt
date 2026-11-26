package app.snackshot.di

import app.snackshot.BuildConfig
import app.snackshot.domain.AppCheckProvider
import app.snackshot.domain.EnvironmentConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** The only place that reads flavor-specific BuildConfig fields (doc 12 §2.1). */
@Module
@InstallIn(SingletonComponent::class)
object EnvironmentModule {
    @Provides
    @Singleton
    fun environmentConfig(): EnvironmentConfig = EnvironmentConfig(
        name = BuildConfig.FLAVOR,
        brokerBaseUrl = BuildConfig.BROKER_BASE_URL,
        logmealBaseUrl = BuildConfig.LOGMEAL_BASE_URL,
        useFirebaseEmulators = BuildConfig.USE_FIREBASE_EMULATORS,
        appCheckProvider = AppCheckProvider.fromBuildConfig(BuildConfig.APP_CHECK_PROVIDER),
        showAtwaterComparison = BuildConfig.SHOW_ATWATER_COMPARISON,
    )
}
