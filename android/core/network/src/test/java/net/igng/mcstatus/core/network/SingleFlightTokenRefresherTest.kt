package net.igng.mcstatus.core.network

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
                "tok-v2"
            },
        )

        val results = (1..8).map {
            async { refresher.refreshIfNeeded("tok-v1") }
        }.awaitAll()

        assertEquals(1, refreshCount.get())
        assertEquals(List(8) { "tok-v2" }, results)
    }

    @Test
    fun returnsNullWhenRefreshFailsOrNoRefreshToken() = runTest {
        val refresher = SingleFlightTokenRefresher(
            currentTokenProvider = { "tok-stale" },
            refreshBlock = { null },
        )

        assertNull(refresher.refreshIfNeeded("tok-stale"))
    }
}
