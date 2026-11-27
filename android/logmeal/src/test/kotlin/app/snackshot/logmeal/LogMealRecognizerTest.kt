package app.snackshot.logmeal

import app.snackshot.domain.access.LogMealCredentialProvider
import app.snackshot.domain.meal.RecognitionFailure
import app.snackshot.domain.meal.RecognitionOutcome
import app.snackshot.domain.meal.RecognitionRef
import java.time.Duration
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LogMealRecognizerTest {
    private val server = MockWebServer()
    private val credentials = FakeCredentials()
    private lateinit var recognizer: LogMealRecognizer
    private val jpeg = byteArrayOf(-1, -40, -1, -32, 1, 2, 3)

    @BeforeEach
    fun setUp() {
        server.start()
        recognizer = LogMealRecognizer(LogMealApi(server.url("/").toString(), OkHttpClient()), credentials)
    }

    @AfterEach
    fun tearDown() {
        server.close()
    }

    private fun fixture(path: String): String = checkNotNull(javaClass.classLoader.getResource("logmeal/$path")) { path }.readText()

    private fun ok(path: String, rateHeaders: Boolean = false) = MockResponse.Builder()
        .code(200)
        .body(fixture(path))
        .addHeader("Content-Type", "application/json")
        .apply {
            if (rateHeaders) {
                addHeader("ratelimit-limit", "20")
                addHeader("ratelimit-remaining", "19")
                addHeader("ratelimit-reset", "86400")
            }
        }
        .build()

    private fun status(code: Int, body: String = "", vararg headers: Pair<String, String>) = MockResponse.Builder()
        .code(code)
        .body(body)
        .apply { headers.forEach { (k, v) -> addHeader(k, v) } }
        .build()

    private fun enqueueDish(dish: String, nutrition: String = "ok-$dish", segmentation: String = "ok-single-dish-$dish") {
        server.enqueue(ok("segmentation-complete/$segmentation.json", rateHeaders = true))
        server.enqueue(ok("nutritional-info/$nutrition.json"))
    }

    private suspend fun success(): RecognitionOutcome.Success =
        assertInstanceOf(RecognitionOutcome.Success::class.java, recognizer.recognize(jpeg))

    private suspend fun failure(): RecognitionOutcome.Failure =
        assertInstanceOf(RecognitionOutcome.Failure::class.java, recognizer.recognize(jpeg))

    @Test
    fun `a single dish maps to one recognition with LogMeal's portion and values`() = runTest {
        enqueueDish("apple")

        val r = success().recognition

        assertEquals("apple", r.foodName)
        assertEquals(0.7251, r.confidence, 0.0001)
        assertEquals(182.0, r.grams)
        assertEquals(94.64, r.values.energyKcal!!, 0.01)
        assertEquals(0.473, r.values.proteinGrams!!, 0.001)
        assertEquals(25.134, r.values.carbsGrams!!, 0.001)
        assertEquals(0.309, r.values.fatGrams!!, 0.001)
        assertEquals(RecognitionRef(1000001, 1), r.ref)
        assertEquals(0, r.otherItems)
        assertEquals(19, r.dailyRemaining)

        val segmentation = server.takeRequest()
        assertEquals("/v2/image/segmentation/complete", segmentation.url.encodedPath)
        assertEquals("Bearer token-1", segmentation.headers["Authorization"])
        assertTrue(segmentation.headers["Content-Type"]!!.startsWith("multipart/form-data"))
        assertTrue(segmentation.body!!.utf8().contains("name=\"image\"; filename=\"meal.jpg\""))
        val nutrition = server.takeRequest()
        assertEquals("/v2/nutrition/recipe/nutritionalInfo", nutrition.url.encodedPath)
        assertEquals("{\"imageId\":1000001}", nutrition.body!!.utf8())
    }

    @Test
    fun `an over-segmented photo keeps only the most likely item and its own portion`() = runTest {
        enqueueDish("tomato", nutrition = "one-tomato-three-items", segmentation = "one-tomato-three-items")

        val r = success().recognition

        assertEquals("tomato", r.foodName)
        assertEquals(RecognitionRef(1000002, 1), r.ref)
        assertEquals(123.0, r.grams)
        assertEquals(22.14, r.values.energyKcal!!, 0.01)
        assertEquals(2, r.otherItems)
    }

    @Test
    fun `no token means no request`() = runTest {
        credentials.token = null

        assertEquals(RecognitionFailure.NO_ACCESS, failure().reason)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `a rejected token is refreshed through the broker once and the call repeated`() = runTest {
        server.enqueue(status(401))
        enqueueDish("banana")
        credentials.refreshed = "token-2"

        assertEquals("banana", success().recognition.foodName)
        assertEquals(1, credentials.refreshCalls)
        assertEquals("Bearer token-1", server.takeRequest().headers["Authorization"])
        assertEquals("Bearer token-2", server.takeRequest().headers["Authorization"])
        assertEquals("Bearer token-2", server.takeRequest().headers["Authorization"])
    }

    @Test
    fun `a second rejection gives up without asking the broker again`() = runTest {
        server.enqueue(status(401))
        server.enqueue(status(403, """{"code":116,"message":"The APIUser is disabled."}"""))
        credentials.refreshed = "token-2"

        assertEquals(RecognitionFailure.NO_ACCESS, failure().reason)
        assertEquals(1, credentials.refreshCalls)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `a disabled APIUser with no new token from the broker has no access`() = runTest {
        server.enqueue(status(403, """{"code":116,"message":"The APIUser is disabled."}"""))
        credentials.refreshed = null

        assertEquals(RecognitionFailure.NO_ACCESS, failure().reason)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `the daily limit says when it resets`() = runTest {
        server.enqueue(
            status(
                429,
                """{"code":429,"message":"Too many requests."}""",
                "ratelimit-remaining" to "0",
                "ratelimit-reset" to "3600",
            ),
        )

        val f = failure()

        assertEquals(RecognitionFailure.DAILY_LIMIT, f.reason)
        assertEquals(Duration.ofHours(1), f.retryAfter)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `a server error is retried once`() = runTest {
        server.enqueue(status(500, """{"code":500}"""))
        enqueueDish("bread")

        assertEquals("whole grain bread", success().recognition.foodName)
        assertEquals(3, server.requestCount)
    }

    @Test
    fun `two server errors in a row fail`() = runTest {
        server.enqueue(status(502))
        server.enqueue(status(500))

        assertEquals(RecognitionFailure.SERVICE, failure().reason)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `an unreachable LogMeal is offline after one retry`() = runTest {
        val url = server.url("/").toString()
        server.close()
        val offline = LogMealRecognizer(LogMealApi(url, OkHttpClient()), credentials)

        val f = assertInstanceOf(RecognitionOutcome.Failure::class.java, offline.recognize(jpeg))

        assertEquals(RecognitionFailure.OFFLINE, f.reason)
    }

    @Test
    fun `a refused image is invalid`() = runTest {
        server.enqueue(status(400, """{"code":1200,"message":"Invalid image"}"""))

        assertEquals(RecognitionFailure.INVALID_IMAGE, failure().reason)
    }

    @Test
    fun `no food in the photo`() = runTest {
        server.enqueue(status(200, """{"imageId":1000009,"segmentation_results":[]}"""))

        assertEquals(RecognitionFailure.NO_FOOD, failure().reason)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `an unreadable answer is a service failure`() = runTest {
        server.enqueue(status(200, "<html>Bad gateway</html>"))

        assertEquals(RecognitionFailure.SERVICE, failure().reason)
    }

    @Test
    fun `when nutrition fails the food is still recognized and the values are left to the user`() = runTest {
        server.enqueue(ok("segmentation-complete/ok-single-dish-tangerine.json", rateHeaders = true))
        server.enqueue(status(500))
        server.enqueue(status(500))

        val r = success().recognition

        assertEquals("tangerine", r.foodName)
        assertNull(r.grams)
        assertTrue(r.values.isEmpty)
    }

    @Test
    fun `a corrected portion is confirmed in the per-item object form`() = runTest {
        server.enqueue(ok("quantity/confirm-ok.json"))

        assertTrue(recognizer.confirmQuantity(RecognitionRef(1000001, 1), 116.0))

        val request = server.takeRequest()
        assertEquals("/v2/nutrition/confirm/quantity", request.url.encodedPath)
        assertEquals("""{"imageId":1000001,"quantity":{"1":116}}""", request.body!!.utf8())
    }

    @Test
    fun `a failed confirmation is reported, not thrown`() = runTest {
        server.enqueue(status(400, fixture("errors/400-1614-quantity-not-object.json")))

        assertFalse(recognizer.confirmQuantity(RecognitionRef(1000001, 1), 116.5))
        assertEquals("""{"imageId":1000001,"quantity":{"1":116.5}}""", server.takeRequest().body!!.utf8())
    }

    private class FakeCredentials : LogMealCredentialProvider {
        var token: String? = "token-1"
        var refreshed: String? = null
        var refreshCalls = 0

        override suspend fun token(): String? = token

        override suspend fun refreshAfterRejection(): String? {
            refreshCalls++
            return refreshed
        }
    }
}
