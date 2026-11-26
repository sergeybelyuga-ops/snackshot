package app.snackshot.domain.access

import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/** The broker's client status (doc 11 §5.1). Only AI recognition depends on it. */
enum class AiAccessStatus {
    TRIAL,
    TRIAL_ENDED,
    ACTIVE,
    PAUSED,
    SUSPENDED,
    CANCELLED,
    WAITLISTED,
    ;

    /** The client's APIUser may recognize photos in this status. */
    val allowsRecognition: Boolean get() = this == TRIAL || this == ACTIVE
}

/** Free pictures of the trial (doc 11 §6.1a). */
data class PictureAllowance(val used: Int, val limit: Int) {
    init {
        require(used >= 0 && limit >= 0) { "Picture counts must not be negative: $used of $limit" }
    }

    val left: Int get() = (limit - used).coerceAtLeast(0)
}

/**
 * The last AI access state the broker reported for this install.
 *
 * @property reason the broker's status reason, for diagnostics only (never shown as is).
 * @property isTest a test client (dev, local, or the owner's devices), not counted in the pilot.
 * @property aiEnabled the global kill switch (doc 11 §6.3); when off, nobody recognizes photos.
 * @property checkedAt when the broker said so; the status is refreshed after [STATUS_TTL].
 */
data class AiAccess(
    val status: AiAccessStatus,
    val reason: String?,
    val pictures: PictureAllowance,
    val isTest: Boolean,
    val aiEnabled: Boolean,
    val checkedAt: Instant,
) {
    val canRecognize: Boolean get() = aiEnabled && status.allowsRecognition

    fun isFresh(now: Instant): Boolean = !now.isBefore(checkedAt) && Duration.between(checkedAt, now) < STATUS_TTL

    companion object {
        /** `GET /status` is cached for about an hour (doc 11 §8). */
        val STATUS_TTL: Duration = Duration.ofHours(1)
    }
}

/** Why the broker couldn't be asked. Quick add and the rest of the app keep working. */
enum class AccessError {
    /** No connection, or the broker didn't answer in time. */
    OFFLINE,

    /** The kill switch is off: no new AI access for anybody right now. */
    AI_PAUSED,

    /** The broker is finishing another change to this client; try again shortly. */
    BUSY,

    /** Anything else: the broker refused the call or failed. */
    SERVICE,
}

/** The answer to "may this install recognize a photo now?". */
sealed interface AccessCheck {
    /** Recognition may start; the LogMeal token is stored. */
    data class Available(val access: AiAccess) : AccessCheck

    /** The broker answered, but this client can't recognize now (trial ended, waitlisted, paused, AI off). */
    data class NotAvailable(val access: AiAccess) : AccessCheck

    /** The broker couldn't be asked. [cached] is the last known state, if any. */
    data class Failed(val error: AccessError, val cached: AiAccess?) : AccessCheck
}

/**
 * AI access for this install, through the LogMeal Access Broker (doc 11 §9). Sign-in is silent and
 * anonymous; nothing happens until the first AI action.
 */
interface AiAccessRepository {
    /** The last known state; null until the first AI action provisions this install. */
    val access: Flow<AiAccess?>

    /**
     * Before every AI action: provisions on the first one, otherwise answers from the cache while it
     * is fresh and asks the broker when it isn't.
     */
    suspend fun ensureAccess(): AccessCheck

    /** At app start: refreshes a stale status. Does nothing (returns null) before the first AI action. */
    suspend fun refreshIfStale(): AccessCheck?

    /** Reports one successful recognition; idempotent by [requestId] (8–64 letters, digits, `-`, `_`). */
    suspend fun reportPicture(requestId: String): AccessCheck

    /** Sends a changed recognition language or country. Before the first AI action there is nothing to send. */
    suspend fun updateProfile(language: String, country: String): Boolean

    /** "Delete all data": deletes the broker record and the device's token copy. False if the broker couldn't be reached. */
    suspend fun deleteAccount(): Boolean
}

/**
 * The seam between the LogMeal adapter and the broker (doc 11 §9). The adapter never sees how the
 * token is obtained.
 */
interface LogMealCredentialProvider {
    /** This client's LogMeal token, or null when AI recognition isn't available now. */
    suspend fun token(): String?

    /**
     * After LogMeal rejected the token (401 or 403/116): asks the broker once. Returns a new token to
     * retry with, or null when there is none (the status explains why).
     */
    suspend fun refreshAfterRejection(): String?
}

/** The last broker answer, kept on the device between app starts. */
data class CachedAccess(val access: AiAccess, val tokenVersion: Int)

/** Local storage for [CachedAccess]. Not secret: the token itself is in [LogMealTokenStore]. */
interface AiAccessCache {
    val cached: Flow<CachedAccess?>

    suspend fun read(): CachedAccess?

    suspend fun write(value: CachedAccess)

    suspend fun clear()
}

/** The language and country LogMeal recognizes dishes for (doc 11 §8, WBS 4.2). */
data class RecognitionLocale(val language: String, val country: String) {
    init {
        require(language.matches(Regex("[a-z]{3}"))) { "LogMeal language codes are three lowercase letters: $language" }
        require(country.matches(Regex("[A-Z]{2}"))) { "Country must be ISO 3166-1 alpha-2: $country" }
    }
}

/** The locale to provision with. */
fun interface RecognitionLocaleSource {
    suspend fun current(): RecognitionLocale
}
