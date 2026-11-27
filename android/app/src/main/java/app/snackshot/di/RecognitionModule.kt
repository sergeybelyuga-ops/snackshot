package app.snackshot.di

import app.snackshot.domain.EnvironmentConfig
import app.snackshot.domain.access.LogMealCredentialProvider
import app.snackshot.domain.meal.FoodRecognizer
import app.snackshot.domain.meal.HistoryPortionCalibrator
import app.snackshot.domain.meal.MealRepository
import app.snackshot.domain.meal.PortionCalibrator
import app.snackshot.logmeal.LogMealApi
import app.snackshot.logmeal.LogMealRecognizer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Duration
import javax.inject.Singleton
import okhttp3.OkHttpClient

/** The LogMeal adapter (WBS 6.3) and portion calibration (WBS 6.5) live in pure JVM modules without Hilt, so they are wired here. */
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

    /** WBS 6.5: the user's last grams for the food, then LogMeal's, then reference weights. */
    @Provides
    fun portionCalibrator(meals: MealRepository): PortionCalibrator = HistoryPortionCalibrator(meals)
}
