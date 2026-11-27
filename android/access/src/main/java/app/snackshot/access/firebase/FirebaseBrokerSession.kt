package app.snackshot.access.firebase

import android.util.Log
import app.snackshot.access.BrokerSession
import app.snackshot.access.BrokerSessionException
import app.snackshot.access.broker.BrokerAuth
import app.snackshot.domain.AppCheckProvider
import app.snackshot.domain.EnvironmentConfig
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuth
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

/**
 * Silent anonymous Firebase sign-in plus App Check (doc 11 §5.3). The anonymous user lives in app
 * data, so it is lost on uninstall; the broker re-attaches the new user by the device ID.
 */
@Singleton
class FirebaseBrokerSession @Inject constructor(private val environment: EnvironmentConfig) : BrokerSession {
    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    override val isSignedIn: Boolean get() = auth.currentUser != null

    override suspend fun auth(forceRefresh: Boolean): BrokerAuth = firebaseCall {
        val user =
            auth.currentUser ?: auth.signInAnonymously().await().user ?: throw BrokerSessionException("Anonymous sign-in returned no user")
        val idToken = user.getIdToken(forceRefresh).await().token ?: throw BrokerSessionException("No ID token")
        BrokerAuth(idToken, appCheckToken())
    }

    /**
     * Null on the emulator. When App Check fails, the call goes out without it and the broker
     * refuses it (401); in dev builds the usual cause is a debug token not yet registered.
     */
    private suspend fun appCheckToken(): String? {
        if (environment.appCheckProvider == AppCheckProvider.NONE) return null
        return try {
            FirebaseAppCheck.getInstance().getAppCheckToken(false).await().token
        } catch (e: FirebaseException) {
            Log.w(TAG, "App Check token unavailable: ${e.javaClass.simpleName}")
            null
        }
    }

    private suspend fun <T> firebaseCall(block: suspend () -> T): T = try {
        block()
    } catch (e: FirebaseNetworkException) {
        throw IOException("No connection for Firebase sign-in", e)
    } catch (e: FirebaseException) {
        throw BrokerSessionException("Firebase sign-in failed: ${e.javaClass.simpleName}", e)
    }

    private companion object {
        const val TAG = "SnackshotAccess"
    }
}
