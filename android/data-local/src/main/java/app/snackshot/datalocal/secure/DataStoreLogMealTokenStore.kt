package app.snackshot.datalocal.secure

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.snackshot.datalocal.settings.SecretsStore
import app.snackshot.domain.access.LogMealTokenStore
import java.security.GeneralSecurityException
import java.util.Base64
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * The LogMeal APIUser token as an AES-GCM blob in its own DataStore file (decision 8). Only the
 * ciphertext touches disk; the token is never logged.
 */
class DataStoreLogMealTokenStore @Inject constructor(
    @SecretsStore private val store: DataStore<Preferences>,
    @Named(CIPHER) private val cipher: AesGcmCipher,
) : LogMealTokenStore {
    override suspend fun read(): String? {
        val encoded = store.data.first()[TOKEN] ?: return null
        val plaintext = withContext(Dispatchers.Default) {
            try {
                cipher.decrypt(Base64.getDecoder().decode(encoded), AAD)
            } catch (_: GeneralSecurityException) {
                null // the key is gone or changed (restore, Keystore reset): provision again
            } catch (_: IllegalArgumentException) {
                null // not valid Base64
            }
        }
        if (plaintext == null) {
            clear()
            return null
        }
        return plaintext.toString(Charsets.UTF_8)
    }

    override suspend fun write(token: String) {
        require(token.isNotBlank()) { "Token must not be blank" }
        val blob = withContext(Dispatchers.Default) { cipher.encrypt(token.toByteArray(Charsets.UTF_8), AAD) }
        store.edit { it[TOKEN] = Base64.getEncoder().encodeToString(blob) }
    }

    override suspend fun clear() {
        store.edit { it.remove(TOKEN) }
    }

    companion object {
        const val CIPHER = "logmeal-token-cipher"
        const val KEY_ALIAS = "snackshot.logmeal_token.v1"
        private val TOKEN = stringPreferencesKey("logmeal_token_v1")
        private val AAD = "snackshot:logmeal_token:v1".toByteArray(Charsets.UTF_8)
    }
}
