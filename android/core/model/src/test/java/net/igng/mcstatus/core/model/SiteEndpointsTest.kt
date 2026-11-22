package net.igng.mcstatus.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SiteEndpointsTest {
    @Test
    fun restrictsBearerAuthToApiGatewayAndSsoOnly() {
        val endpoints = SiteEndpoints()
        val trusted = endpoints.trustedAuthHosts()
        assertTrue("www.igng.net" in trusted)
        assertTrue("sso.igng.net" in trusted)
        assertFalse("mc.igng.net" in trusted)
        assertFalse("dash.igng.net" in trusted)
        assertFalse("chat.igng.net" in trusted)
    }

    @Test
    fun buildsV1MediaUrlsAndParsesTypedIds() {
        val endpoints = SiteEndpoints()
        assertEquals(
            "https://www.igng.net/api/v1/media/med_abc/content",
            endpoints.mediaContentUrl("med_abc"),
        )
        assertEquals(
            "https://www.igng.net/api/v1/chat/media/qq-1/0?variant=thumb",
            endpoints.resolveApiMediaUrl("/api/v1/chat/media/qq-1/0?variant=thumb"),
        )
        assertEquals("srv_3", V1Id.format("srv", 3))
        assertEquals(3, V1Id.parseNumeric("srv_3"))
        assertEquals(2048, V1Id.parseNumeric("tk_2048"))
        assertEquals(42, V1Id.parseNumeric("42"))
        assertNull(V1Id.parseNumeric(""))
    }
}
