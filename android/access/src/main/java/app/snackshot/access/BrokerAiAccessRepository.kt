package app.snackshot.access

import app.snackshot.access.broker.BrokerApi
import app.snackshot.access.broker.BrokerAuth
import app.snackshot.access.broker.BrokerHttpException
import app.snackshot.access.broker.ClientViewDto
import app.snackshot.access.broker.PicturesDto
import app.snackshot.domain.access.AccessCheck
import app.snackshot.domain.access.AccessError
import app.snackshot.domain.access.AiAccess
import app.snackshot.domain.access.AiAccessCache
import app.snackshot.domain.access.AiAccessRepository
import app.snackshot.domain.access.AiAccessStatus
import app.snackshot.domain.access.CachedAccess
import app.snackshot.domain.access.LogMealCredentialProvider
import app.snackshot.domain.access.LogMealTokenStore
import app.snackshot.domain.access.PictureAllowance
import app.snackshot.domain.access.RecognitionLocaleSource
import java.io.IOException
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * AI access through the broker (doc 11 §9, WBS 4.1).
 *
 * - The first AI action signs in anonymously and calls `provision` with the hashed device ID and the
 *   recognition locale. A reinstall on the same phone gets its old client and picture count back.
 * - Later actions answer from the cached status while it is fresh (about an hour) and call `status`
 *   when it isn't; the broker sends the token only when its version changed.
 * - One broker call at a time per install, so a double tap provisions once.
 * - Tokens are never logged; failures map to [AccessError] and leave the cache as it was.
 */
@Singleton
class BrokerAiAccessRepository @Inject constructor(
    private val api: BrokerApi,
    private val session: BrokerSession,
    private val tokens: LogMealTokenStore,
    private val cache: AiAccessCache,
    private val deviceId: DeviceIdSource,
    private val locale: RecognitionLocaleSource,
    private val clock: Clock,
) : AiAccessRepository,
    LogMealCredentialProvider {
    private val mutex = Mutex()

    override val access: Flow<AiAccess?> = cache.cached.map { it?.access }

    override suspend fun ensureAccess(): AccessCheck = mutex.withLock { check(provisionIfMissing = true, force = false) }

    override suspend fun refreshIfStale(): AccessCheck? = mutex.withLock {
        if (cache.read() == null) null else check(provisionIfMissing = false, force = false)
    }

    override suspend fun reportPicture(requestId: String): AccessCheck = mutex.withLock {
        val cached = cache.read() ?: return@withLock check(provisionIfMissing = true, force = false)
        try {
            val report = brokerCall { api.picture(it, requestId) }
            val updated = cached.copy(
                access = cached.access.copy(
                    status = statusOf(report.status),
                    pictures = report.pictures.toDomain(),
                    checkedAt = clock.instant(),
                ),
            )
            cache.write(updated)
            verdict(updated.access)
        } catch (e: Exception) {
            failure(e, cached.access) ?: throw e
        }
    }

    override suspend fun updateProfile(language: String, country: String): Boolean = mutex.withLock {
        if (cache.read() == null) return@withLock true // provisioning sends the current locale
        try {
            brokerCall { api.profile(it, language, country) }
            true
        } catch (e: Exception) {
            if (failure(e, null) == null) throw e
            false
        }
    }

    override suspend fun deleteAccount(): Boolean = mutex.withLock {
        if (session.isSignedIn) {
            try {
                brokerCall { api.deleteAccount(it) }
            } catch (e: BrokerHttpException) {
                if (e.code != NOT_PROVISIONED) return@withLock false
            } catch (e: Exception) {
                if (failure(e, null) == null) throw e
                return@withLock false
            }
        }
        tokens.clear()
        cache.clear()
        true
    }

    override suspend fun token(): String? = when (ensureAccess()) {
        is AccessCheck.Available -> tokens.read()
        else -> null
    }

    override suspend fun refreshAfterRejection(): String? = mutex.withLock {
        val rejected = tokens.read()
        val result = check(provisionIfMissing = true, force = true)
        val current = tokens.read()
        if (result is AccessCheck.Available && current != null && current != rejected) current else null
    }

    /** Must hold [mutex]. */
    private suspend fun check(provisionIfMissing: Boolean, force: Boolean): AccessCheck {
        val cached = cache.read()
        val token = tokens.read()
        val now = clock.instant()
        if (cached != null && !force && cached.access.isFresh(now) && (token != null || !cached.access.canRecognize)) {
            return verdict(cached.access)
        }
        if (cached == null && !provisionIfMissing) return AccessCheck.Failed(AccessError.SERVICE, null)
        return try {
            val view = retryWhenBusy {
                if (cached == null) {
                    provision()
                } else {
                    try {
                        // Without a usable token copy, ask for the token whatever its version.
                        brokerCall { api.status(it, knownTokenVersion = if (token != null) cached.tokenVersion else null) }
                    } catch (e: BrokerHttpException) {
                        // The broker no longer knows this sign-in (record deleted or emulator reset).
                        if (e.code != NOT_PROVISIONED) throw e
                        provision()
                    }
                }
            }
            apply(view)
        } catch (e: Exception) {
            failure(e, cached?.access) ?: throw e
        }
    }

    private suspend fun provision(): ClientViewDto {
        val (language, country) = locale.current()
        return brokerCall { api.provision(it, deviceId.deviceId(), language, country) }
    }

    private suspend fun apply(view: ClientViewDto): AccessCheck {
        val access = AiAccess(
            status = statusOf(view.status),
            reason = view.reason,
            pictures = view.pictures.toDomain(),
            isTest = view.isTest,
            aiEnabled = view.aiEnabled,
            checkedAt = clock.instant(),
        )
        view.token?.let { tokens.write(it) }
        if (access.status == AiAccessStatus.CANCELLED) tokens.clear()
        cache.write(CachedAccess(access, view.tokenVersion))
        if (access.canRecognize && tokens.read() == null) return AccessCheck.Failed(AccessError.SERVICE, access)
        return verdict(access)
    }

    private fun verdict(access: AiAccess): AccessCheck =
        if (access.canRecognize) AccessCheck.Available(access) else AccessCheck.NotAvailable(access)

    /** One retry with a forced ID token refresh when the broker rejects the token (expired, revoked). */
    private suspend fun <T> brokerCall(block: suspend (BrokerAuth) -> T): T {
        try {
            return block(session.auth(forceRefresh = false))
        } catch (e: BrokerHttpException) {
            if (e.status != 401) throw e
        }
        return block(session.auth(forceRefresh = true))
    }

    /** `client_busy`: another change to this client is finishing; one retry after a short pause. */
    private suspend fun <T> retryWhenBusy(block: suspend () -> T): T {
        try {
            return block()
        } catch (e: BrokerHttpException) {
            if (e.code != CLIENT_BUSY) throw e
        }
        delay(BUSY_RETRY_MS)
        return block()
    }

    /** The user-facing failure for [e], or null for a programming error that must surface. */
    private fun failure(e: Exception, cached: AiAccess?): AccessCheck.Failed? {
        val error = when (e) {
            is IOException -> AccessError.OFFLINE
            is BrokerHttpException -> when (e.code) {
                AI_DISABLED -> AccessError.AI_PAUSED
                CLIENT_BUSY -> AccessError.BUSY
                else -> AccessError.SERVICE
            }
            is BrokerSessionException -> AccessError.SERVICE
            else -> return null
        }
        return AccessCheck.Failed(error, cached)
    }

    private companion object {
        const val NOT_PROVISIONED = "not_provisioned"
        const val CLIENT_BUSY = "client_busy"
        const val AI_DISABLED = "ai_disabled"
        const val BUSY_RETRY_MS = 1_500L

        /** An unknown status from a newer broker is treated as paused: AI off, everything else on. */
        fun statusOf(value: String): AiAccessStatus = AiAccessStatus.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
            ?: AiAccessStatus.PAUSED

        fun PicturesDto.toDomain() = PictureAllowance(used.coerceAtLeast(0), limit.coerceAtLeast(0))
    }
}

/** Sign-in or App Check failed for a reason other than the network. */
class BrokerSessionException(message: String, cause: Throwable? = null) : Exception(message, cause)
