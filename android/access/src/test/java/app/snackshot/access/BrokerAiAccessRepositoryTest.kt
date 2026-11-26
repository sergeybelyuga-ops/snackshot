package app.snackshot.access

import app.snackshot.access.broker.BrokerApi
import app.snackshot.access.broker.BrokerAuth
import app.snackshot.domain.access.AccessCheck
import app.snackshot.domain.access.AccessError
import app.snackshot.domain.access.AiAccessCache
import app.snackshot.domain.access.AiAccessStatus
import app.snackshot.domain.access.CachedAccess
import app.snackshot.domain.access.LogMealTokenStore
import app.snackshot.domain.access.PictureAllowance
import app.snackshot.domain.access.RecognitionLocale
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BrokerAiAccessRepositoryTest {
    private val server = MockWebServer()
    private val session = FakeSession()
    private val tokens = FakeTokenStore()
    private val cache = FakeCache()
    private val clock = MutableClock(Instant.parse("2026-11-27T10:00:00Z"))
    private lateinit var repository: BrokerAiAccessRepository

    private val deviceId = DeviceId.hash("android-id-1")

    @Before
    fun setUp() {
        server.start()
        repository = BrokerAiAccessRepository(
            api = BrokerApi(server.url("/api").toString(), OkHttpClient()),
            session = session,
            tokens = tokens,
            cache = cache,
            deviceId = { deviceId },
            locale = { RecognitionLocale("eng", "RS") },
            clock = clock,
        )
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun json(code: Int, body: String) =
        MockResponse.Builder().code(code).body(body).addHeader("Content-Type", "application/json").build()

    private fun view(
        status: String = "trial",
        token: String? = "token-1",
        tokenVersion: Int = 1,
        used: Int = 0,
        aiEnabled: Boolean = true,
    ) = json(
        200,
        """{"status":"$status","reason":"provisioned","tokenVersion":$tokenVersion,"pictures":{"used":$used,"limit":90},"isTest":true,"aiEnabled":$aiEnabled${if (token != null) ""","token":"$token"""" else ""}}""",
    )

    private fun error(code: Int, error: String) = json(code, """{"error":"$error","message":"x"}""")

    private fun take(): RecordedRequest = server.takeRequest()

    private val RecordedRequest.path: String get() = url.encodedPath + (url.encodedQuery?.let { "?$it" } ?: "")

    @Test
    fun `first AI action provisions with the hashed device id and locale, stores the token, then answers from the cache`() = runTest {
        server.enqueue(view())

        val first = repository.ensureAccess()

        assertTrue(first is AccessCheck.Available)
        first as AccessCheck.Available
        assertEquals(AiAccessStatus.TRIAL, first.access.status)
        assertEquals(PictureAllowance(0, 90), first.access.pictures)
        val request = take()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/logmeal/provision", request.path)
        assertEquals("Bearer id-1", request.headers["Authorization"])
        assertEquals("app-check-1", request.headers["X-Firebase-AppCheck"])
        assertEquals("""{"deviceId":"$deviceId","language":"eng","country":"RS"}""", request.body?.utf8())
        assertEquals("token-1", tokens.value)
        assertEquals(1, cache.value?.tokenVersion)

        clock.advance(Duration.ofMinutes(59))
        assertTrue(repository.ensureAccess() is AccessCheck.Available)
        assertEquals("no second call while the status is fresh", 1, server.requestCount)
        assertEquals("token-1", repository.token())
    }

    @Test
    fun `the device id is a lowercase SHA-256 hex digest the broker accepts`() {
        assertTrue(deviceId.matches(Regex("[0-9a-f]{64}")))
        assertEquals(deviceId, DeviceId.hash("android-id-1"))
        assertTrue(deviceId != DeviceId.hash("android-id-2"))
    }

    @Test
    fun `a double tap provisions once`() = runTest {
        server.enqueue(view())
        val results = (1..3).map { async { repository.ensureAccess() } }.awaitAll()
        assertTrue(results.all { it is AccessCheck.Available })
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `a stale status is refreshed with the known token version, and a new token replaces the old one`() = runTest {
        server.enqueue(view())
        repository.ensureAccess()
        take()

        clock.advance(Duration.ofHours(2))
        server.enqueue(view(token = "token-2", tokenVersion = 2, used = 5))
        val refreshed = repository.ensureAccess() as AccessCheck.Available

        val request = take()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/logmeal/status?knownTokenVersion=1", request.path)
        assertEquals("token-2", tokens.value)
        assertEquals(2, cache.value?.tokenVersion)
        assertEquals(85, refreshed.access.pictures.left)
    }

    @Test
    fun `without a usable token copy the status call asks for the token whatever its version`() = runTest {
        server.enqueue(view())
        repository.ensureAccess()
        take()
        tokens.value = null // e.g. the Keystore key was reset

        server.enqueue(view())
        assertTrue(repository.ensureAccess() is AccessCheck.Available)
        assertEquals("/api/v1/logmeal/status", take().path)
        assertEquals("token-1", tokens.value)
    }

    @Test
    fun `waitlisted and trial ended clients are told why, without a token`() = runTest {
        server.enqueue(view(status = "waitlisted", token = null, tokenVersion = 0))
        val waitlisted = repository.ensureAccess()
        assertTrue(waitlisted is AccessCheck.NotAvailable)
        assertEquals(AiAccessStatus.WAITLISTED, (waitlisted as AccessCheck.NotAvailable).access.status)
        assertNull(tokens.value)
        assertNull(repository.token())
        assertEquals("a fresh waitlisted status needs no call", 1, server.requestCount)

        clock.advance(Duration.ofHours(2))
        server.enqueue(view(status = "trial_ended", token = null, used = 90))
        assertEquals(AiAccessStatus.TRIAL_ENDED, (repository.ensureAccess() as AccessCheck.NotAvailable).access.status)
    }

    @Test
    fun `the kill switch turns recognition off without touching the status`() = runTest {
        server.enqueue(view(aiEnabled = false))
        val result = repository.ensureAccess() as AccessCheck.NotAvailable
        assertEquals(AiAccessStatus.TRIAL, result.access.status)
        assertTrue(!result.access.canRecognize)
    }

    @Test
    fun `an expired ID token is refreshed once`() = runTest {
        server.enqueue(error(401, "unauthenticated"))
        server.enqueue(view())

        assertTrue(repository.ensureAccess() is AccessCheck.Available)
        assertEquals("Bearer id-1", take().headers["Authorization"])
        assertEquals("Bearer id-2", take().headers["Authorization"])
        assertEquals(listOf(false, true), session.refreshes)
    }

    @Test
    fun `a busy client is retried once`() = runTest {
        server.enqueue(error(503, "client_busy"))
        server.enqueue(view())
        assertTrue(repository.ensureAccess() is AccessCheck.Available)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `failures keep the last known state and say what happened`() = runTest {
        server.enqueue(error(503, "ai_disabled"))
        assertEquals(AccessCheck.Failed(AccessError.AI_PAUSED, null), repository.ensureAccess())

        server.enqueue(view())
        repository.ensureAccess()
        clock.advance(Duration.ofHours(2))
        server.enqueue(error(500, "internal"))
        val failed = repository.ensureAccess() as AccessCheck.Failed
        assertEquals(AccessError.SERVICE, failed.error)
        assertEquals(AiAccessStatus.TRIAL, failed.cached?.status)
        assertEquals("token-1", tokens.value)

        session.offline = true
        assertEquals(AccessError.OFFLINE, (repository.ensureAccess() as AccessCheck.Failed).error)
    }

    @Test
    fun `a broker that no longer knows this sign-in provisions again`() = runTest {
        server.enqueue(view())
        repository.ensureAccess()
        clock.advance(Duration.ofHours(2))
        server.enqueue(error(404, "not_provisioned"))
        server.enqueue(view(token = "token-3", tokenVersion = 1))

        assertTrue(repository.ensureAccess() is AccessCheck.Available)
        take()
        assertEquals("/api/v1/logmeal/status?knownTokenVersion=1", take().path)
        assertEquals("/api/v1/logmeal/provision", take().path)
        assertEquals("token-3", tokens.value)
    }

    @Test
    fun `refreshIfStale does nothing before the first AI action`() = runTest {
        assertNull(repository.refreshIfStale())
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `a picture report updates the free pictures left`() = runTest {
        server.enqueue(view())
        repository.ensureAccess()
        take()
        server.enqueue(json(200, """{"status":"trial","pictures":{"used":1,"limit":90}}"""))

        val result = repository.reportPicture("request-0001") as AccessCheck.Available

        val request = take()
        assertEquals("/api/v1/logmeal/picture", request.path)
        assertEquals("""{"requestId":"request-0001"}""", request.body?.utf8())
        assertEquals(89, result.access.pictures.left)
        assertEquals(89, cache.value?.access?.pictures?.left)
    }

    @Test
    fun `the 90th picture ends the trial`() = runTest {
        server.enqueue(view(used = 89))
        repository.ensureAccess()
        server.enqueue(json(200, """{"status":"trial_ended","pictures":{"used":90,"limit":90}}"""))
        assertEquals(AiAccessStatus.TRIAL_ENDED, (repository.reportPicture("request-0090") as AccessCheck.NotAvailable).access.status)
    }

    @Test
    fun `after LogMeal rejects the token, a new token from the broker is returned once`() = runTest {
        server.enqueue(view())
        repository.ensureAccess()
        take()

        server.enqueue(view(token = "token-2", tokenVersion = 2))
        assertEquals("token-2", repository.refreshAfterRejection())
        assertEquals("/api/v1/logmeal/status?knownTokenVersion=1", take().path)

        server.enqueue(view(status = "paused", token = null, tokenVersion = 2))
        assertNull("no new token: the status explains why", repository.refreshAfterRejection())
    }

    @Test
    fun `profile changes go to the broker only after provisioning`() = runTest {
        assertTrue(repository.updateProfile("spa", "ES"))
        assertEquals(0, server.requestCount)

        server.enqueue(view())
        repository.ensureAccess()
        take()
        server.enqueue(json(200, """{"language":"spa","country":"ES"}"""))
        assertTrue(repository.updateProfile("spa", "ES"))
        assertEquals("""{"language":"spa","country":"ES"}""", take().body?.utf8())
    }

    @Test
    fun `delete all data removes the broker record and the device copy`() = runTest {
        server.enqueue(view())
        repository.ensureAccess()
        take()
        server.enqueue(MockResponse.Builder().code(204).build())

        assertTrue(repository.deleteAccount())

        val request = take()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/account", request.path)
        assertNull(tokens.value)
        assertNull(cache.value)
    }

    @Test
    fun `delete all data keeps the device copy when the broker can't be reached`() = runTest {
        server.enqueue(view())
        repository.ensureAccess()
        session.offline = true
        assertTrue(!repository.deleteAccount())
        assertEquals("token-1", tokens.value)
    }

    @Test
    fun `tokens never appear in toString`() {
        val text = BrokerAuth("secret-id-token", "secret-app-check").toString()
        assertTrue(text, !text.contains("secret"))
    }
}

private class FakeSession : BrokerSession {
    var offline = false
    private var version = 0
    val refreshes = mutableListOf<Boolean>()
    override var isSignedIn = false

    override suspend fun auth(forceRefresh: Boolean): BrokerAuth {
        if (offline) throw IOException("offline")
        refreshes += forceRefresh
        isSignedIn = true
        if (forceRefresh || version == 0) version++
        return BrokerAuth("id-$version", "app-check-1")
    }
}

private class FakeTokenStore : LogMealTokenStore {
    var value: String? = null

    override suspend fun read() = value

    override suspend fun write(token: String) {
        value = token
    }

    override suspend fun clear() {
        value = null
    }
}

private class FakeCache : AiAccessCache {
    private val state = MutableStateFlow<CachedAccess?>(null)
    val value: CachedAccess? get() = state.value
    override val cached: StateFlow<CachedAccess?> = state

    override suspend fun read() = state.value

    override suspend fun write(value: CachedAccess) {
        state.value = value
    }

    override suspend fun clear() {
        state.value = null
    }
}

private class MutableClock(private var now: Instant) : Clock() {
    fun advance(by: Duration) {
        now += by
    }

    override fun instant(): Instant = now

    override fun getZone() = ZoneOffset.UTC

    override fun withZone(zone: java.time.ZoneId?) = this
}
