package app.snackshot.domain.access

/**
 * This client's own LogMeal APIUser token (doc 11 §4). Encrypted at rest with an Android
 * Keystore key and never backed up: after a restore the app provisions again (doc 11 §4).
 */
interface LogMealTokenStore {
    /** The stored token, or null when none is stored or it can no longer be decrypted. */
    suspend fun read(): String?

    suspend fun write(token: String)

    suspend fun clear()
}
