package app.snackshot.access.firebase

import android.content.Context
import app.snackshot.domain.EnvironmentConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuth

/**
 * The Firebase app of this flavor, from the values in `app/src/<flavor>/google-services.json`
 * (public client identifiers, not secrets). Set up in code instead of the google-services Gradle
 * plugin, so the local flavor can point at the emulator project `demo-snackshot` without a file.
 */
data class FirebaseConfig(val projectId: String, val applicationId: String, val apiKey: String, val gcmSenderId: String)

object FirebaseSetup {
    /** Port of the Auth emulator, forwarded from the device by `tools/adb-reverse.ps1`. */
    const val AUTH_EMULATOR_PORT = 9099

    /**
     * Call once from `Application.onCreate`, before any Firebase use.
     *
     * @param appCheck the flavor's attestation (Play Integrity in prod, debug in dev, none locally).
     */
    fun initialize(context: Context, config: FirebaseConfig, environment: EnvironmentConfig, appCheck: AppCheckProviderFactory?) {
        if (FirebaseApp.getApps(context).isEmpty()) {
            val options = FirebaseOptions.Builder()
                .setProjectId(config.projectId)
                .setApplicationId(config.applicationId)
                .setApiKey(config.apiKey)
                .setGcmSenderId(config.gcmSenderId)
                .build()
            FirebaseApp.initializeApp(context, options)
        }
        if (environment.useFirebaseEmulators) {
            FirebaseAuth.getInstance().useEmulator("localhost", AUTH_EMULATOR_PORT)
        }
        if (appCheck != null) {
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(appCheck)
        }
    }
}
