package app.snackshot.di

import app.snackshot.domain.EnvironmentConfig
import app.snackshot.domain.access.LogMealCredentialProvider
import app.snackshot.domain.meal.FoodRecognizer
import app.snackshot.logmeal.LogMealApi
import app.snackshot.logmeal.LogMealRecognizer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Duration
import javax.inject.Singleton
import okhttp3.OkHttpClient

/** The LogMeal adapter (WBS 6.3). It lives in a pure JVM module without Hilt, so it is wired here. */
@Module
@InstallIn(SingletonComponent::class)
object RecognitionModule {
    @Provides
    @Singleton
    fun foodRecognizer(environment: EnvironmentConfig, credentials: LogMealCredentialProvider): FoodRecognizer = LogMealRecognizer(
        LogMealApi(
            environment.logmealBaseUrl,
            OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                // segmentation/complete took up to 6.4 s in the spike; allow for slow mobile data.
                .readTimeout(Duration.ofSeconds(20))
                .writeTimeout(Duration.ofSeconds(20))
                .callTimeout(Duration.ofSeconds(30))
                .build(),
        ),
        credentials,
    )
}
