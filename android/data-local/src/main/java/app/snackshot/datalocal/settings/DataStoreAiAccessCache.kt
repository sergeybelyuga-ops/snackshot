package app.snackshot.datalocal.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.snackshot.domain.access.AiAccess
import app.snackshot.domain.access.AiAccessCache
import app.snackshot.domain.access.AiAccessStatus
import app.snackshot.domain.access.CachedAccess
import app.snackshot.domain.access.PictureAllowance
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * The last broker answer (status, pictures, token version) as JSON in the settings DataStore.
 * Not secret; the token itself is in the secrets store. Excluded from backup like everything else.
 */
class DataStoreAiAccessCache @Inject constructor(@SettingsStore private val store: DataStore<Preferences>) : AiAccessCache {
    override val cached: Flow<CachedAccess?> = store.data.map { decode(it[KEY]) }.distinctUntilChanged()

    override suspend fun read(): CachedAccess? = cached.first()

    override suspend fun write(value: CachedAccess) {
        val json = with(value.access) {
            CachedAccessJson(
                status = status.name,
                reason = reason,
                picturesUsed = pictures.used,
                pictureLimit = pictures.limit,
                isTest = isTest,
                aiEnabled = aiEnabled,
                checkedAtMillis = checkedAt.toEpochMilli(),
                tokenVersion = value.tokenVersion,
            )
        }
        store.edit { it[KEY] = JSON.encodeToString(CachedAccessJson.serializer(), json) }
    }

    override suspend fun clear() {
        store.edit { it.remove(KEY) }
    }

    /** Unreadable or unknown data (a newer build) counts as no cache: the next AI action asks the broker. */
    private fun decode(raw: String?): CachedAccess? {
        if (raw == null) return null
        return try {
            val json = JSON.decodeFromString(CachedAccessJson.serializer(), raw)
            val status = AiAccessStatus.entries.firstOrNull { it.name == json.status } ?: return null
            CachedAccess(
                access = AiAccess(
                    status = status,
                    reason = json.reason,
                    pictures = PictureAllowance(json.picturesUsed, json.pictureLimit),
                    isTest = json.isTest,
                    aiEnabled = json.aiEnabled,
                    checkedAt = Instant.ofEpochMilli(json.checkedAtMillis),
                ),
                tokenVersion = json.tokenVersion,
            )
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    @Serializable
    private data class CachedAccessJson(
        val status: String,
        val reason: String? = null,
        val picturesUsed: Int,
        val pictureLimit: Int,
        val isTest: Boolean,
        val aiEnabled: Boolean,
        val checkedAtMillis: Long,
        val tokenVersion: Int,
    )

    private companion object {
        val KEY = stringPreferencesKey("ai_access_v1")
        val JSON = Json { ignoreUnknownKeys = true }
    }
}
