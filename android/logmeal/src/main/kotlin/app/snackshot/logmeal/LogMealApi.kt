package app.snackshot.logmeal

import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

/** One LogMeal answer: the raw body of a 2xx, or the status with LogMeal's numeric error code, if any. */
internal sealed interface ApiResult {
    val headers: Headers

    data class Ok(val body: String, override val headers: Headers) : ApiResult

    data class Error(val status: Int, val code: Int?, override val headers: Headers) : ApiResult {
        /** LogMeal rejected the token: 401 (empty body) or 403 with code 116 (disabled APIUser). */
        val isTokenRejected: Boolean get() = status == 401 || (status == 403 && code == CODE_USER_DISABLED)
    }

    companion object {
        const val CODE_USER_DISABLED = 116
    }
}

/**
 * The LogMeal v2 calls Snackshot makes (spike: docs/Architecture/13_spike-1.1-results.md §7).
 * No logging interceptor: every request carries the APIUser token in `Authorization` (doc 11 §4).
 * Network failures surface as [IOException]; everything else as an [ApiResult].
 */
class LogMealApi(baseUrl: String, private val client: OkHttpClient) {
    private val base: HttpUrl = baseUrl.trimEnd('/').toHttpUrl()

    /** `segmentation/complete` with the processed photo. Costs one credit; carries the rate-limit headers. */
    internal suspend fun segment(token: String, jpeg: ByteArray): ApiResult {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("image", "meal.jpg", jpeg.toRequestBody(JPEG))
            .build()
        return post(token, LogMealEndpoints.SEGMENTATION_COMPLETE, body)
    }

    /** `nutritionalInfo` for a segmented image: totals and `nutritional_info_per_item`. Free. */
    internal suspend fun nutrition(token: String, imageId: Long): ApiResult =
        post(token, LogMealEndpoints.NUTRITIONAL_INFO, buildJsonObject { put("imageId", imageId) }.toString().toRequestBody(JSON))

    /** `confirm/quantity` in the per-item object form; the integer form is rejected for segmented images (400/1614). */
    internal suspend fun confirmQuantity(token: String, imageId: Long, itemPosition: Int, grams: Double): ApiResult {
        val body = buildJsonObject {
            put("imageId", imageId)
            // Whole grams go as an integer, as in the request the spike recorded: {"1": 116}.
            putJsonObject("quantity") {
                if (grams % 1.0 == 0.0) put(itemPosition.toString(), grams.toLong()) else put(itemPosition.toString(), grams)
            }
        }
        return post(token, LogMealEndpoints.CONFIRM_QUANTITY, body.toString().toRequestBody(JSON))
    }

    private suspend fun post(token: String, path: String, body: RequestBody): ApiResult = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(base.newBuilder().addEncodedPathSegments(path.removePrefix("/")).build())
            .header("Authorization", "Bearer $token")
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            // LogMeal's 401 has an empty body: never assume JSON on errors.
            val text = response.body.string()
            if (response.isSuccessful) {
                ApiResult.Ok(text, response.headers)
            } else {
                ApiResult.Error(response.code, errorCode(text), response.headers)
            }
        }
    }

    private fun errorCode(text: String): Int? = runCatching { json.decodeFromString(ErrorDto.serializer(), text).code }.getOrNull()

    internal companion object {
        val json = Json { ignoreUnknownKeys = true }
        private val JPEG = "image/jpeg".toMediaType()
        private val JSON = "application/json".toMediaType()
    }
}
