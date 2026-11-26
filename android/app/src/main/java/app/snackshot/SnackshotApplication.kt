package app.snackshot

import android.app.Application
import android.util.Log
import app.snackshot.access.firebase.FirebaseConfig
import app.snackshot.access.firebase.FirebaseSetup
import app.snackshot.datalocal.images.ImageRetentionCleaner
import app.snackshot.domain.EnvironmentConfig
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class SnackshotApplication : Application() {
    @Inject lateinit var imageRetention: ImageRetentionCleaner

    @Inject lateinit var environment: EnvironmentConfig

    /** Work that outlives any screen; never cancelled while the process lives. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Before anything touches Firebase. Nothing signs in until the first AI action (doc 11 §9).
        FirebaseSetup.initialize(
            context = this,
            config = FirebaseConfig(
                projectId = BuildConfig.FIREBASE_PROJECT_ID,
                applicationId = BuildConfig.FIREBASE_APP_ID,
                apiKey = BuildConfig.FIREBASE_API_KEY,
                gcmSenderId = BuildConfig.FIREBASE_SENDER_ID,
            ),
            environment = environment,
            appCheck = FlavorFirebase.appCheckFactory(),
        )
        // Photo retention (doc 02 §8.4) at every start: a directory listing, cheap enough not to schedule.
        appScope.launch {
            runCatching { imageRetention.run() }
                .onSuccess { Log.i(TAG, "Image retention: $it") }
                .onFailure { Log.w(TAG, "Image retention failed", it) }
        }
    }

    private companion object {
        const val TAG = "Snackshot"
    }
}
