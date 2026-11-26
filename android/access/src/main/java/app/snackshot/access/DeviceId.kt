package app.snackshot.access

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/** The hashed device identifier; an interface so the repository can be tested without Android. */
fun interface DeviceIdSource {
    fun deviceId(): String
}

/**
 * The device identifier the broker uses to re-attach a reinstall to its client (doc 11 §5.3):
 * `SHA-256(ANDROID_ID + salt)` as lowercase hex. The raw ANDROID_ID never leaves the device.
 * ANDROID_ID is stable for the same signing key, Android user and device, so a reinstall keeps its
 * free-picture count.
 */
@Singleton
class DeviceId @Inject constructor(@ApplicationContext private val context: Context) : DeviceIdSource {
    private val value: String by lazy { hash(androidId()) }

    override fun deviceId(): String = value

    @SuppressLint("HardwareIds") // the documented purpose: recognizing this app on this device, hashed
    private fun androidId(): String = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        ?: error("ANDROID_ID is unavailable")

    companion object {
        /** Changing the salt would turn every installed device into a new client. */
        private const val SALT = "snackshot-device-id-v1"

        fun hash(androidId: String): String {
            require(androidId.isNotBlank()) { "ANDROID_ID must not be blank" }
            val digest = MessageDigest.getInstance("SHA-256").digest((androidId + SALT).toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
