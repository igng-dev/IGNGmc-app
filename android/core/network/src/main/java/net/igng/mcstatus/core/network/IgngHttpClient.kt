package net.igng.mcstatus.core.network

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.igng.mcstatus.core.model.SiteEndpoints
import okhttp3.ConnectionPool
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

data class SessionExpiredEvent(
    val url: String,
    val message: String,
)

class SessionExpiredNotifier {
    private val _events = MutableSharedFlow<SessionExpiredEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<SessionExpiredEvent> = _events.asSharedFlow()

    fun notify(url: String, message: String) {
        _events.tryEmit(SessionExpiredEvent(url, message))
    }
}

object IgngHttpClientFactory {
    const val USER_AGENT = "IGNGmc-Android/2.0.0"

    fun create(
        endpoints: SiteEndpoints,
        tokenProvider: () -> String?,
    ): OkHttpClient {
        val trustedHosts = endpoints.trustedAuthHosts()
        return OkHttpClient.Builder()
            .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val original = chain.request()
                val host = original.url.host.lowercase()
                val builder = original.newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", original.header("Accept") ?: "application/json")
                if (original.header("Authorization").isNullOrBlank() && host in trustedHosts) {
                    val token = tokenProvider()?.takeIf { it.isNotBlank() }
                    if (token != null) {
                        builder.header("Authorization", "Bearer $token")
                    }
                }
                chain.proceed(builder.build())
            }
            .build()
    }
}

class ApiClient(
    val okHttpClient: OkHttpClient,
    val json: Json = DefaultJson,
    private val sessionExpiredNotifier: SessionExpiredNotifier? = null,
) {
    suspend fun get(
        url: String,
        tokenOverride: String? = null,
        suppressAuthNotify: Boolean = false,
    ): String = requestWithRetry(
        method = "GET",
        url = url,
        body = null,
        tokenOverride = tokenOverride,
        suppressAuthNotify = suppressAuthNotify,
    )

    suspend fun post(
        url: String,
        body: String = "{}",
        tokenOverride: String? = null,
        suppressAuthNotify: Boolean = false,
    ): String = executeOnce("POST", url, body, tokenOverride, suppressAuthNotify)

    suspend fun patch(
        url: String,
        body: String,
        tokenOverride: String? = null,
        suppressAuthNotify: Boolean = false,
    ): String = executeOnce("PATCH", url, body, tokenOverride, suppressAuthNotify)

    suspend fun delete(
        url: String,
        tokenOverride: String? = null,
        suppressAuthNotify: Boolean = false,
    ): String = executeOnce("DELETE", url, null, tokenOverride, suppressAuthNotify)

    suspend fun uploadImage(
        url: String,
        fileName: String,
        mimeType: String,
        bytes: ByteArray,
        tokenOverride: String? = null,
    ): String = withContext(Dispatchers.IO) {
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                fileName,
                bytes.toRequestBody(mimeType.toMediaType()),
            )
            .build()
        val request = Request.Builder()
            .url(url)
            .apply {
                if (!tokenOverride.isNullOrBlank()) {
                    header("Authorization", "Bearer $tokenOverride")
                }
            }
            .post(multipart)
            .build()
        executeCall(request, suppressAuthNotify = false)
    }

    private suspend fun requestWithRetry(
        method: String,
        url: String,
        body: String?,
        tokenOverride: String?,
        suppressAuthNotify: Boolean,
    ): String {
        var attempt = 0
        var lastError: Throwable? = null
        while (attempt < 2) {
            try {
                return executeOnce(method, url, body, tokenOverride, suppressAuthNotify)
            } catch (error: ApiException) {
                if (error.isServerTransient && attempt == 0) {
                    lastError = error
                    val waitMs = ((error.retryAfterSeconds ?: 1).coerceIn(1, 4)) * 1000L
                    delay(waitMs)
                    attempt++
                    continue
                }
                throw error
            } catch (error: IOException) {
                if (attempt == 0) {
                    lastError = error
                    delay(600L)
                    attempt++
                    continue
                }
                throw error
            }
        }
        throw lastError ?: IOException("网络请求失败")
    }

    private suspend fun executeOnce(
        method: String,
        url: String,
        body: String?,
        tokenOverride: String?,
        suppressAuthNotify: Boolean,
    ): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .apply {
                if (!tokenOverride.isNullOrBlank()) {
                    header("Authorization", "Bearer $tokenOverride")
                }
            }
            .method(method, body?.toRequestBody(JSON_MEDIA_TYPE))
            .build()
        executeCall(request, suppressAuthNotify)
    }

    private fun executeCall(request: Request, suppressAuthNotify: Boolean): String {
        okHttpClient.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val parsed = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull()
                val nestedError = parsed?.get("error")
                val message = when {
                    nestedError is kotlinx.serialization.json.JsonObject ->
                        nestedError["message"]?.jsonPrimitive?.contentOrNull
                    else -> nestedError?.jsonPrimitive?.contentOrNull
                } ?: "请求失败（${response.code}）"
                val code = when {
                    nestedError is kotlinx.serialization.json.JsonObject ->
                        nestedError["code"]?.jsonPrimitive?.contentOrNull
                    else -> parsed?.get("code")?.jsonPrimitive?.contentOrNull
                }
                val retryAfter = response.header("Retry-After")?.toIntOrNull()
                if (response.code == 401 && !suppressAuthNotify && !request.header("Authorization").isNullOrBlank()) {
                    sessionExpiredNotifier?.notify(request.url.toString(), message)
                }
                throw ApiException(
                    statusCode = response.code,
                    errorCode = code,
                    message = message,
                    retryAfterSeconds = retryAfter,
                )
            }
            return text
        }
    }

    companion object {
        val DefaultJson: Json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            explicitNulls = false
        }
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
