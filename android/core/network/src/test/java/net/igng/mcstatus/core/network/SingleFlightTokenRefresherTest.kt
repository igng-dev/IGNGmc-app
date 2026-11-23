package net.igng.mcstatus.core.network

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.igng.mcstatus.core.model.SiteEndpoints
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SingleFlightTokenRefresherTest {

    @Test
    fun concurrent401sOnlyTriggerOneRefreshCall() = runTest {
        val storedToken = AtomicReference("tok-v1")
        val refreshCount = AtomicInteger(0)
        val refresher = SingleFlightTokenRefresher(
            currentTokenProvider = { storedToken.get() },
            refreshBlock = { failed ->
                refreshCount.incrementAndGet()
                assertEquals("tok-v1", failed)
                delay(50)
                storedToken.set("tok-v2")
                TokenRefreshResult.Refreshed("tok-v2")
            },
        )

        val results = (1..8).map {
            async { refresher.refreshIfNeeded("tok-v1") }
        }.awaitAll()

        assertEquals(1, refreshCount.get())
        assertEquals(List(8) { TokenRefreshResult.Refreshed("tok-v2") }, results)
    }

    @Test
    fun returnsInvalidatedWhenNoRefreshTokenAndSkipsRepeatedCalls() = runTest {
        val invalidated = mutableSetOf<String>()
        val refreshCount = AtomicInteger(0)
        val refresher = SingleFlightTokenRefresher(
            currentTokenProvider = { "tok-stale".takeUnless { invalidated.contains(it) } },
            isTokenInvalidated = { invalidated.contains(it) },
            refreshBlock = { failed ->
                refreshCount.incrementAndGet()
                invalidated.add(failed)
                TokenRefreshResult.Invalidated
            },
        )

        val results = (1..4).map {
            async { refresher.refreshIfNeeded("tok-stale") }
        }.awaitAll()

        assertEquals(1, refreshCount.get())
        assertEquals(List(4) { TokenRefreshResult.Invalidated }, results)
    }

    @Test
    fun publicGetFallsBackToAnonymousWhenLegacyTokenInvalidated() = runTest {
        val invalidated = mutableSetOf<String>()
        val seenAuthHeaders = mutableListOf<String?>()
        val notifier = SessionExpiredNotifier()
        val endpoints = SiteEndpoints()
        val mockOkHttp = IgngHttpClientFactory.create(
            endpoints = endpoints,
            tokenProvider = { "legacy-tok".takeUnless { invalidated.contains(it) } },
        ).newBuilder()
            .addInterceptor { chain ->
                val req = chain.request()
                val auth = req.header("Authorization")
                seenAuthHeaders.add(auth)
                assertNull(req.header(IgngHttpClientFactory.SKIP_AUTH_HEADER))
                val (code, body) = if (auth != null) {
                    401 to """{"error":{"code":"auth.token_invalid","message":"token invalid"}}"""
                } else {
                    200 to """{"families":[]}"""
                }
                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_1_1)
                    .code(code)
                    .message(if (code == 200) "OK" else "Unauthorized")
                    .body(body.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val refresher = SingleFlightTokenRefresher(
            currentTokenProvider = { "legacy-tok".takeUnless { invalidated.contains(it) } },
            isTokenInvalidated = { invalidated.contains(it) },
            refreshBlock = { failed ->
                invalidated.add(failed)
                TokenRefreshResult.Invalidated
            },
        )
        val api = ApiClient(
            okHttpClient = mockOkHttp,
            sessionExpiredNotifier = notifier,
            tokenRefresher = refresher,
            hasInvalidatedSession = { invalidated.contains("legacy-tok") },
        )

        val firstBody = api.get("${endpoints.api}/api/v1/mc/servers", allowAnonymousFallback = true)
        assertEquals("""{"families":[]}""", firstBody)
        assertEquals(listOf("Bearer legacy-tok", null), seenAuthHeaders)
        assertTrue(invalidated.contains("legacy-tok"))

        // Subsequent public GETs must not re-send the invalidated token at all.
        val secondBody = api.get("${endpoints.api}/api/v1/mc/status", allowAnonymousFallback = true)
        assertEquals("""{"families":[]}""", secondBody)
        assertEquals(listOf("Bearer legacy-tok", null, null), seenAuthHeaders)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun business401DoesNotTriggerTokenRefreshOrSessionExpired() = runTest {
        val refreshCount = AtomicInteger(0)
        val notifier = SessionExpiredNotifier()
        val emitted = mutableListOf<SessionExpiredEvent>()
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            notifier.events.collect { emitted.add(it) }
        }
        val endpoints = SiteEndpoints()
        val mockOkHttp = IgngHttpClientFactory.create(
            endpoints = endpoints,
            tokenProvider = { "valid-access-tok" },
        ).newBuilder()
            .addInterceptor { chain ->
                val req = chain.request()
                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_1_1)
                    .code(401)
                    .message("Unauthorized")
                    .body(
                        """{"error":{"code":"mc.password_invalid","message":"当前游戏密码错误"}}"""
                            .toResponseBody("application/json".toMediaType()),
                    )
                    .build()
            }
            .build()

        val refresher = SingleFlightTokenRefresher(
            currentTokenProvider = { "valid-access-tok" },
            refreshBlock = {
                refreshCount.incrementAndGet()
                TokenRefreshResult.Invalidated
            },
        )
        val api = ApiClient(
            okHttpClient = mockOkHttp,
            sessionExpiredNotifier = notifier,
            tokenRefresher = refresher,
        )

        var caught: ApiException? = null
        try {
            api.patch("${endpoints.api}/api/v1/mc/accounts/Steve", """{"credential":{}}""")
        } catch (err: ApiException) {
            caught = err
        } finally {
            collectJob.cancel()
        }

        assertEquals(401, caught?.statusCode)
        assertEquals("mc.password_invalid", caught?.errorCode)
        assertEquals(false, caught?.isSessionAuthFailure)
        assertEquals(0, refreshCount.get())
        assertTrue(emitted.isEmpty())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun auth401TriggersRefreshAndNotifiesWhenInvalidated() = runTest {
        val refreshCount = AtomicInteger(0)
        val notifier = SessionExpiredNotifier()
        val emitted = mutableListOf<SessionExpiredEvent>()
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            notifier.events.collect { emitted.add(it) }
        }
        val endpoints = SiteEndpoints()
        val mockOkHttp = IgngHttpClientFactory.create(
            endpoints = endpoints,
            tokenProvider = { "expired-access-tok" },
        ).newBuilder()
            .addInterceptor { chain ->
                val req = chain.request()
                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_1_1)
                    .code(401)
                    .message("Unauthorized")
                    .body(
                        """{"error":{"code":"auth.token_expired","message":"访问令牌已过期"}}"""
                            .toResponseBody("application/json".toMediaType()),
                    )
                    .build()
            }
            .build()

        val refresher = SingleFlightTokenRefresher(
            currentTokenProvider = { "expired-access-tok" },
            refreshBlock = {
                refreshCount.incrementAndGet()
                TokenRefreshResult.Invalidated
            },
        )
        val api = ApiClient(
            okHttpClient = mockOkHttp,
            sessionExpiredNotifier = notifier,
            tokenRefresher = refresher,
        )

        var caught: ApiException? = null
        try {
            api.get("${endpoints.api}/api/v1/mc/accounts")
        } catch (err: ApiException) {
            caught = err
        } finally {
            collectJob.cancel()
        }

        assertEquals(401, caught?.statusCode)
        assertEquals("auth.token_expired", caught?.errorCode)
        assertTrue(caught?.isSessionAuthFailure == true)
        assertEquals(1, refreshCount.get())
        assertEquals(1, emitted.size)
    }

    @Test
    fun auth401IsRecognizedAsSessionAuthFailure() {
        val expired = ApiException(401, "auth.token_expired", "访问令牌已过期")
        val invalid = ApiException(401, "auth.token_invalid", "访问令牌无效")
        val revoked = ApiException(401, "auth.session_revoked", "会话已被吊销")
        val unknown = ApiException(401, null, "Unauthorized")
        val wrongMcPassword = ApiException(401, "mc.password_invalid", "密码错误")
        val wrongLoginCred = ApiException(401, "auth.credential_invalid", "账号或密码错误")

        assertTrue(expired.isSessionAuthFailure)
        assertTrue(invalid.isSessionAuthFailure)
        assertTrue(revoked.isSessionAuthFailure)
        assertTrue(unknown.isSessionAuthFailure)
        assertEquals(false, wrongMcPassword.isSessionAuthFailure)
        assertEquals(false, wrongLoginCred.isSessionAuthFailure)
    }
}
