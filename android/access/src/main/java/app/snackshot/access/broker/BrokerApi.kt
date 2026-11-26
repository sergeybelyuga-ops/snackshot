package app.snackshot.access.broker

import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

/** What every broker call carries: the anonymous ID token and, outside the emulator, App Check. */
data class BrokerAuth(val idToken: String, val appCheckToken: String?) {
    // Never print tokens (doc 11 §4).
    override fun toString() = "BrokerAuth(idToken=…, appCheckToken=${if (appCheckToken == null) "none" else "…"})"
}

/** A broker answer outside 2xx, with its stable `error` code (`not_provisioned`, `client_busy`, …). */
class BrokerHttpException(val status: Int, val code: String?) : Exception("Broker answered $status ${code ?: ""}".trim())

/** The broker's client view (broker/functions/src/clients/provisioning.ts `viewOf`). */
@Serializable
data class ClientViewDto(
    val status: String,
    val reason: String? = null,
    val tokenVersion: Int = 0,
    val pictures: PicturesDto,
    val isTest: Boolean = false,
    val aiEnabled: Boolean = true,
    val token: String? = null,
) {
    override fun toString() =
        "ClientViewDto(status=$status, tokenVersion=$tokenVersion, pictures=$pictures, token=${if (token == null) "none" else "…"})"
}

@Serializable
data class PicturesDto(val used: Int, val limit: Int)

@Serializable
data class PictureReportDto(val status: String, val pictures: PicturesDto)

@Serializable
private data class ProvisionBody(val deviceId: String, val language: String, val country: String)

@Serializable
private data class ProfileBody(val language: String, val country: String)

@Serializable
private data class PictureBody(val requestId: String)

@Serializable
private data class ErrorDto(@SerialName("error") val code: String? = null)

/**
 * The broker's app endpoints (doc 11 §8). No logging interceptor: requests carry ID tokens and
 * responses carry the LogMeal token.
 */
class BrokerApi(baseUrl: String, private val client: OkHttpClient) {
    private val base: HttpUrl = baseUrl.trimEnd('/').toHttpUrl()

    suspend fun provision(auth: BrokerAuth, deviceId: String, language: String, country: String): ClientViewDto = call(
        auth,
        url(PROVISION),
        post(ProvisionBody.serializer(), ProvisionBody(deviceId, language, country)),
        ClientViewDto.serializer(),
    )!!

    suspend fun status(auth: BrokerAuth, knownTokenVersion: Int?): ClientViewDto {
        val url = url(STATUS).newBuilder().apply {
            if (knownTokenVersion != null) addQueryParameter("knownTokenVersion", knownTokenVersion.toString())
        }.build()
        return call(auth, url, Verb.Get, ClientViewDto.serializer())!!
    }

    suspend fun profile(auth: BrokerAuth, language: String, country: String) {
        call<Unit>(auth, url(PROFILE), post(ProfileBody.serializer(), ProfileBody(language, country)), null)
    }

    suspend fun picture(auth: BrokerAuth, requestId: String): PictureReportDto =
        call(auth, url(PICTURE), post(PictureBody.serializer(), PictureBody(requestId)), PictureReportDto.serializer())!!

    suspend fun deleteAccount(auth: BrokerAuth) {
        call<Unit>(auth, url(ACCOUNT), Verb.Delete, null)
    }

    private sealed interface Verb {
        data object Get : Verb

        data object Delete : Verb

        class Post(val body: RequestBody) : Verb
    }

    private fun url(path: String): HttpUrl = base.newBuilder().addPathSegments(path).build()

    private fun <T> post(serializer: KSerializer<T>, body: T) = Verb.Post(JSON.encodeToString(serializer, body).toRequestBody(JSON_TYPE))

    /** Decodes the answer with [response], or returns null when the endpoint answers without a body worth reading. */
    private suspend fun <T : Any> call(auth: BrokerAuth, url: HttpUrl, verb: Verb, response: KSerializer<T>?): T? =
        withContext(Dispatchers.IO) {
            val request = Request.Builder().url(url).apply {
                header("Authorization", "Bearer ${auth.idToken}")
                auth.appCheckToken?.let { header("X-Firebase-AppCheck", it) }
                when (verb) {
                    Verb.Get -> get()
                    Verb.Delete -> delete()
                    is Verb.Post -> post(verb.body)
                }
            }.build()
            client.newCall(request).execute().use { res ->
                val text = res.body.string()
                if (!res.isSuccessful) throw BrokerHttpException(res.code, errorCode(text))
                if (response == null) return@withContext null
                try {
                    JSON.decodeFromString(response, text)
                } catch (e: SerializationException) {
                    throw IOException("Unexpected broker response", e)
                } catch (e: IllegalArgumentException) {
                    throw IOException("Unexpected broker response", e)
                }
            }
        }

    private fun errorCode(text: String): String? = try {
        JSON.decodeFromString(ErrorDto.serializer(), text).code
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    private companion object {
        const val PROVISION = "v1/logmeal/provision"
        const val STATUS = "v1/logmeal/status"
        const val PROFILE = "v1/logmeal/profile"
        const val PICTURE = "v1/logmeal/picture"
        const val ACCOUNT = "v1/account"
        val JSON = Json { ignoreUnknownKeys = true }
        val JSON_TYPE = "application/json".toMediaType()
    }
}
