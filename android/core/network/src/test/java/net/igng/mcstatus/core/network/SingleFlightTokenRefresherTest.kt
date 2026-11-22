package net.igng.mcstatus.core.network

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
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
}
