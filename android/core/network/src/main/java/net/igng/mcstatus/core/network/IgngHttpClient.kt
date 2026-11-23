package net.igng.mcstatus.core.network

import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.igng.mcstatus.core.model.SiteEndpoints
import okhttp3.Authenticator
import okhttp3.ConnectionPool
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route

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

sealed interface TokenRefreshResult {
    data class Refreshed(val accessToken: String) : TokenRefreshResult
    data object Invalidated : TokenRefreshResult
    data object TransientFailure : TokenRefreshResult
}

/**
 * Coordinates single-flight OAuth 2.1 refresh-token rotation across concurrent 401 responses.
 * Because `refresh_token` is single-use and replay revokes the entire token family, only the
 * first coroutine observing a stale `access_token` calls `/api/oauth/token`; queued coroutines
 * immediately reuse the rotated token saved by the winner.
 */
class SingleFlightTokenRefresher(
    private val currentTokenProvider: () -> String?,
    private val isTokenInvalidated: (String) -> Boolean = { false },
    private val refreshBlock: suspend (failedAccessToken: String) -> TokenRefreshResult,
) {
    private val mutex = Mutex()

    suspend fun refreshIfNeeded(failedAccessToken: String): TokenRefreshResult = mutex.withLock {
        val latest = currentTokenProvider()?.takeIf { it.isNotBlank() }
        if (latest != null && latest != failedAccessToken) {
            return TokenRefreshResult.Refreshed(latest)
        }
        if (isTokenInvalidated(failedAccessToken)) {
            return TokenRefreshResult.Invalidated
        }
        refreshBlock(failedAccessToken)
    }
}

object IgngHttpClientFactory {
    const val CLIENT_ID = "igng_mc_android"
    const val DEFAULT_CLIENT_VERSION = "2.3.1"
    internal const val SKIP_AUTH_HEADER = "X-Igng-Internal-Skip-Auth"

    fun create(
        endpoints: SiteEndpoints,
        tokenProvider: () -> String?,
        deviceIdProvider: () -> String = { "dev_igngmc_android" },
        clientVersion: String = DEFAULT_CLIENT_VERSION,
    ): OkHttpClient {
        val trustedHosts = endpoints.trustedAuthHosts()
        val apiHost = endpoints.apiHost()
        val userAgent = "IGNGmc-Android/$clientVersion"
        return OkHttpClient.Builder()
            .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val original = chain.request()
                val host = original.url.host.lowercase()
                val skipAuth = !original.header(SKIP_AUTH_HEADER).isNullOrBlank()
                val builder = original.newBuilder()
                    .removeHeader(SKIP_AUTH_HEADER)
                    .header("User-Agent", userAgent)
                    .header("Accept", original.header("Accept") ?: "application/json, image/*, */*;q=0.8")
                if (host == apiHost) {
                    builder
                        .header("X-Client-Id", CLIENT_ID)
                        .header("X-Client-Version", clientVersion)
                        .header("X-Device-Id", deviceIdProvider())
                }
                if (!skipAuth && original.header("Authorization").isNullOrBlank() && host in trustedHosts) {
                    val token = tokenProvider()?.takeIf { it.isNotBlank() }
                    if (token != null) {
                        builder.header("Authorization", "Bearer $token")
                    }
                }
                chain.proceed(builder.build())
            }
            .build()
    }

    /**
     * Builds a derived OkHttpClient for binary media / Coil image fetching that handles 401
     * responses on the unified v1 gateway by coordinating single-flight OAuth token refresh
     * and falling back to anonymous access for public optional-auth media endpoints when the
     * session has been invalidated.
     */
    fun createMediaClient(
        baseClient: OkHttpClient,
        endpoints: SiteEndpoints,
        tokenRefresher: SingleFlightTokenRefresher,
        sessionExpiredNotifier: SessionExpiredNotifier? = null,
    ): OkHttpClient {
        val trustedHosts = endpoints.trustedAuthHosts()
        return baseClient.newBuilder()
            .authenticator(
                Authenticator { _: Route?, response: Response ->
                    if (priorResponseCount(response) >= 2) return@Authenticator null
                    val req = response.request
                    val host = req.url.host.lowercase()
                    if (host !in trustedHosts) return@Authenticator null
                    val sentAuth = req.header("Authorization")
                    val sentBearer = sentAuth
                        ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
                        ?.substring(7)
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?: return@Authenticator null

                    val outcome = runCatching {
                        runBlocking { tokenRefresher.refreshIfNeeded(sentBearer) }
                    }.getOrElse { TokenRefreshResult.TransientFailure }

                    when (outcome) {
                        is TokenRefreshResult.Refreshed -> req.newBuilder()
                            .header("Authorization", "Bearer ${outcome.accessToken}")
                            .build()
                        TokenRefreshResult.Invalidated -> {
                            sessionExpiredNotifier?.notify(req.url.toString(), "登录状态已失效，请重新登录")
                            req.newBuilder()
                                .removeHeader("Authorization")
                                .header(SKIP_AUTH_HEADER, "1")
                                .build()
                        }
                        TokenRefreshResult.TransientFailure -> null
                    }
                },
            )
            .build()
    }

    private fun priorResponseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count += 1
            prior = prior.priorResponse
        }
        return count
    }
}

class ApiClient(
    val okHttpClient: OkHttpClient,
    val json: Json = DefaultJson,
    private val sessionExpiredNotifier: SessionExpiredNotifier? = null,
    private val tokenRefresher: SingleFlightTokenRefresher? = null,
    private val hasInvalidatedSession: () -> Boolean = { false },
) {
    suspend fun get(
        url: String,
        tokenOverride: String? = null,
        suppressAuthNotify: Boolean = false,
        allowAnonymousFallback: Boolean = false,
    ): String = requestWithRetry(
        method = "GET",
        url = url,
        body = null,
        ifMatch = null,
        tokenOverride = tokenOverride,
        suppressAuthNotify = suppressAuthNotify,
        allowAnonymousFallback = allowAnonymousFallback,
    )

    suspend fun post(
        url: String,
        body: String = "{}",
        tokenOverride: String? = null,
        suppressAuthNotify: Boolean = false,
        idempotencyKey: String? = null,
    ): String = executeWithAuthRefresh(
        buildRequest = { override, skipAuth ->
            buildJsonRequest(
                method = "POST",
                url = url,
                body = body,
                ifMatch = null,
                idempotencyKey = idempotencyKey ?: autoIdempotencyKey("POST", url),
                tokenOverride = override,
                skipAuth = skipAuth,
            )
        },
        tokenOverride = tokenOverride,
        suppressAuthNotify = suppressAuthNotify,
    )

    suspend fun patch(
        url: String,
        body: String,
        ifMatch: String? = null,
        tokenOverride: String? = null,
        suppressAuthNotify: Boolean = false,
    ): String = executeWithAuthRefresh(
        buildRequest = { override, skipAuth ->
            buildJsonRequest(
                method = "PATCH",
                url = url,
                body = body,
                ifMatch = ifMatch,
                idempotencyKey = null,
                tokenOverride = override,
                skipAuth = skipAuth,
            )
        },
        tokenOverride = tokenOverride,
        suppressAuthNotify = suppressAuthNotify,
    )

    suspend fun delete(
        url: String,
        tokenOverride: String? = null,
        suppressAuthNotify: Boolean = false,
    ): String = executeWithAuthRefresh(
        buildRequest = { override, skipAuth ->
            buildJsonRequest(
                method = "DELETE",
                url = url,
                body = null,
                ifMatch = null,
                idempotencyKey = null,
                tokenOverride = override,
                skipAuth = skipAuth,
            )
        },
        tokenOverride = tokenOverride,
        suppressAuthNotify = suppressAuthNotify,
    )

    /**
     * Downloads a binary resource (chat audio/video/file) into [targetFile] with the same
     * single-flight OAuth 401 refresh and optional anonymous fallback as JSON API requests.
     */
    suspend fun downloadToFile(
        url: String,
        targetFile: File,
        allowAnonymousFallback: Boolean = true,
    ): File {
        fun buildReq(override: String?, skipAuth: Boolean): Request = Request.Builder()
            .url(url)
            .header("Accept", "*/*")
            .apply {
                if (skipAuth) {
                    header(IgngHttpClientFactory.SKIP_AUTH_HEADER, "1")
                } else if (!override.isNullOrBlank()) {
                    header("Authorization", "Bearer $override")
                }
            }
            .get()
            .build()

        suspend fun attempt(override: String?, skipAuth: Boolean): CallOutcome = withContext(Dispatchers.IO) {
            okHttpClient.newCall(buildReq(override, skipAuth)).execute().use { response ->
                if (!response.isSuccessful) {
                    val sentAuth = response.request.header("Authorization")
                    val bearer = sentAuth?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
                        ?.substring(7)?.trim()?.takeIf { it.isNotBlank() }
                    return@withContext CallOutcome.Failure(
                        url = response.request.url.toString(),
                        sentBearerToken = bearer,
                        exception = ApiException(
                            statusCode = response.code,
                            errorCode = null,
                            message = "媒体读取失败（HTTP ${response.code}）",
                            retryAfterSeconds = response.header("Retry-After")?.toIntOrNull(),
                        ),
                    )
                }
                val body = response.body ?: throw IllegalStateException("媒体响应为空")
                val tmp = File(targetFile.parentFile, "${targetFile.name}.tmp")
                body.byteStream().use { input ->
                    tmp.outputStream().use { output -> input.copyTo(output) }
                }
                if (!tmp.renameTo(targetFile)) {
                    tmp.copyTo(targetFile, overwrite = true)
                    tmp.delete()
                }
                CallOutcome.Success("")
            }
        }

        val first = attempt(null, false)
        if (first is CallOutcome.Success) return targetFile

        val err = first as CallOutcome.Failure
        val isSessionAuthError = err.exception.isSessionAuthFailure
        val refresher = tokenRefresher
        val sentBearer = err.sentBearerToken
        var sessionInvalidated = false
        if (isSessionAuthError && !sentBearer.isNullOrBlank() && refresher != null) {
            val outcome = runCatching {
                refresher.refreshIfNeeded(sentBearer)
            }.getOrElse { TokenRefreshResult.TransientFailure }
            when (outcome) {
                is TokenRefreshResult.Refreshed -> {
                    val retry = attempt(outcome.accessToken, false)
                    if (retry is CallOutcome.Success) return targetFile
                    val retryErr = retry as CallOutcome.Failure
                    if (retryErr.exception.isSessionAuthFailure) {
                        sessionExpiredNotifier?.notify(retryErr.url, retryErr.exception.message)
                    }
                    throw retryErr.exception
                }
                TokenRefreshResult.Invalidated -> {
                    sessionInvalidated = true
                }
                TokenRefreshResult.TransientFailure -> throw err.exception
            }
        }

        if (isSessionAuthError && (!sentBearer.isNullOrBlank() || hasInvalidatedSession())) {
            sessionExpiredNotifier?.notify(err.url, err.exception.message)
        }

        if (allowAnonymousFallback &&
            isSessionAuthError &&
            !sentBearer.isNullOrBlank() &&
            (sessionInvalidated || hasInvalidatedSession())
        ) {
            val anon = attempt(null, true)
            if (anon is CallOutcome.Success) return targetFile
            throw (anon as CallOutcome.Failure).exception
        }
        throw err.exception
    }

    suspend fun uploadImage(
        url: String,
        fileName: String,
        mimeType: String,
        bytes: ByteArray,
        extraFields: Map<String, String> = emptyMap(),
        tokenOverride: String? = null,
        idempotencyKey: String? = null,
    ): String {
        val key = idempotencyKey ?: autoIdempotencyKey("POST", url)
        return executeWithAuthRefresh(
            buildRequest = { override, skipAuth ->
                val multipart = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .apply {
                        extraFields.forEach { (name, value) -> addFormDataPart(name, value) }
                    }
                    .addFormDataPart(
                        "file",
                        fileName,
                        bytes.toRequestBody(mimeType.toMediaType()),
                    )
                    .build()
                Request.Builder()
                    .url(url)
                    .apply {
                        if (!key.isNullOrBlank()) header("Idempotency-Key", key)
                        if (skipAuth) {
                            header(IgngHttpClientFactory.SKIP_AUTH_HEADER, "1")
                        } else if (!override.isNullOrBlank()) {
                            header("Authorization", "Bearer $override")
                        }
                    }
                    .post(multipart)
                    .build()
            },
            tokenOverride = tokenOverride,
            suppressAuthNotify = false,
        )
    }

    private suspend fun requestWithRetry(
        method: String,
        url: String,
        body: String?,
        ifMatch: String?,
        tokenOverride: String?,
        suppressAuthNotify: Boolean,
        allowAnonymousFallback: Boolean = false,
    ): String {
        var attempt = 0
        var lastError: Throwable? = null
        while (attempt < 2) {
            try {
                return executeWithAuthRefresh(
                    buildRequest = { override, skipAuth ->
                        buildJsonRequest(method, url, body, ifMatch, null, override, skipAuth)
                    },
                    tokenOverride = tokenOverride,
                    suppressAuthNotify = suppressAuthNotify,
                    allowAnonymousFallback = allowAnonymousFallback,
                )
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

    private suspend fun executeWithAuthRefresh(
        buildRequest: (tokenOverride: String?, skipAuth: Boolean) -> Request,
        tokenOverride: String?,
        suppressAuthNotify: Boolean,
        allowAnonymousFallback: Boolean = false,
    ): String {
        val first = withContext(Dispatchers.IO) { executeCall(buildRequest(tokenOverride, false)) }
        if (first is CallOutcome.Success) return first.body

        val err = first as CallOutcome.Failure
        val isSessionAuthError = err.exception.isSessionAuthFailure
        val canRefresh = isSessionAuthError &&
            !suppressAuthNotify &&
            tokenOverride.isNullOrBlank() &&
            !err.sentBearerToken.isNullOrBlank() &&
            tokenRefresher != null

        var sessionInvalidated = false
        if (canRefresh) {
            val outcome = runCatching {
                tokenRefresher!!.refreshIfNeeded(err.sentBearerToken!!)
            }.getOrElse { TokenRefreshResult.TransientFailure }
            when (outcome) {
                is TokenRefreshResult.Refreshed -> {
                    val retry = withContext(Dispatchers.IO) { executeCall(buildRequest(outcome.accessToken, false)) }
                    if (retry is CallOutcome.Success) return retry.body
                    val retryErr = retry as CallOutcome.Failure
                    if (retryErr.exception.isSessionAuthFailure) {
                        sessionExpiredNotifier?.notify(retryErr.url, retryErr.exception.message)
                    }
                    throw retryErr.exception
                }
                TokenRefreshResult.Invalidated -> {
                    sessionInvalidated = true
                }
                TokenRefreshResult.TransientFailure -> {
                    throw err.exception
                }
            }
        }

        if (isSessionAuthError && !suppressAuthNotify &&
            (!err.sentBearerToken.isNullOrBlank() || hasInvalidatedSession())
        ) {
            sessionExpiredNotifier?.notify(err.url, err.exception.message)
        }

        if (allowAnonymousFallback &&
            isSessionAuthError &&
            !err.sentBearerToken.isNullOrBlank() &&
            (sessionInvalidated || hasInvalidatedSession())
        ) {
            val anonymousRetry = withContext(Dispatchers.IO) { executeCall(buildRequest(null, true)) }
            if (anonymousRetry is CallOutcome.Success) return anonymousRetry.body
            throw (anonymousRetry as CallOutcome.Failure).exception
        }

        throw err.exception
    }

    private fun buildJsonRequest(
        method: String,
        url: String,
        body: String?,
        ifMatch: String?,
        idempotencyKey: String?,
        tokenOverride: String?,
        skipAuth: Boolean = false,
    ): Request = Request.Builder()
        .url(url)
        .apply {
            if (!ifMatch.isNullOrBlank()) header("If-Match", ifMatch)
            if (!idempotencyKey.isNullOrBlank()) header("Idempotency-Key", idempotencyKey)
            if (skipAuth) {
                header(IgngHttpClientFactory.SKIP_AUTH_HEADER, "1")
            } else if (!tokenOverride.isNullOrBlank()) {
                header("Authorization", "Bearer $tokenOverride")
            }
        }
        .method(method, body?.toRequestBody(JSON_MEDIA_TYPE))
        .build()

    private fun autoIdempotencyKey(method: String, url: String): String? =
        if (method == "POST" && url.contains("/api/v1/")) {
            UUID.randomUUID().toString().replace("-", "")
        } else {
            null
        }

    private sealed interface CallOutcome {
        data class Success(val body: String) : CallOutcome
        data class Failure(
            val url: String,
            val sentBearerToken: String?,
            val exception: ApiException,
        ) : CallOutcome
    }

    private fun executeCall(request: Request): CallOutcome {
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
                val sentAuth = response.request.header("Authorization")
                val bearer = sentAuth?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
                    ?.substring(7)?.trim()?.takeIf { it.isNotBlank() }
                return CallOutcome.Failure(
                    url = response.request.url.toString(),
                    sentBearerToken = bearer,
                    exception = ApiException(
                        statusCode = response.code,
                        errorCode = code,
                        message = message,
                        retryAfterSeconds = retryAfter,
                    ),
                )
            }
            return CallOutcome.Success(text)
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
