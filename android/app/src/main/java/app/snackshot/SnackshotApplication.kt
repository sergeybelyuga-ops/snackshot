package app.snackshot

import android.app.Application
import android.util.Log
import app.snackshot.datalocal.images.ImageRetentionCleaner
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class SnackshotApplication : Application() {
    @Inject lateinit var imageRetention: ImageRetentionCleaner

    /** Work that outlives any screen; never cancelled while the process lives. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
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
