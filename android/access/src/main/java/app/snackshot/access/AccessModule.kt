package app.snackshot.access

import app.snackshot.access.broker.BrokerApi
import app.snackshot.access.firebase.FirebaseBrokerSession
import app.snackshot.domain.EnvironmentConfig
import app.snackshot.domain.access.AiAccessRepository
import app.snackshot.domain.access.LogMealCredentialProvider
import app.snackshot.domain.access.RecognitionLocale
import app.snackshot.domain.access.RecognitionLocaleSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Duration
import java.util.Locale
import javax.inject.Singleton
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
abstract class AccessModule {
    @Binds
    abstract fun aiAccessRepository(impl: BrokerAiAccessRepository): AiAccessRepository

    @Binds
    abstract fun credentialProvider(impl: BrokerAiAccessRepository): LogMealCredentialProvider

    @Binds
    abstract fun brokerSession(impl: FirebaseBrokerSession): BrokerSession

    @Binds
    abstract fun deviceIdSource(impl: DeviceId): DeviceIdSource

    companion object {
        @Provides
        @Singleton
        fun brokerApi(environment: EnvironmentConfig): BrokerApi = BrokerApi(
            environment.brokerBaseUrl,
            OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                // A cold start of the broker function can take several seconds.
                .readTimeout(Duration.ofSeconds(30))
                .callTimeout(Duration.ofSeconds(45))
                .build(),
        )

        /** English and the device region until WBS 4.2 maps the device locale. */
        @Provides
        fun recognitionLocale(): RecognitionLocaleSource = RecognitionLocaleSource {
            val region = Locale.getDefault().country
            RecognitionLocale("eng", if (region.matches(Regex("[A-Z]{2}"))) region else "US")
        }
    }
}
