package net.igng.mcstatus.core.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PkceGeneratorTest {
    @Test
    fun generatesRfc7636CompliantS256Pair() {
        val pair = PkceGenerator.generate()
        assertTrue(pair.verifier.length in 43..128)
        assertTrue(pair.challenge.length in 43..128)
        assertEquals(pair.challenge, PkceGenerator.computeS256Challenge(pair.verifier))
        assertTrue(Regex("^[A-Za-z0-9_-]+$").matches(pair.verifier))
        assertTrue(Regex("^[A-Za-z0-9_-]+$").matches(pair.challenge))
    }

    @Test
    fun matchesRfc7636TestVector() {
        val verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
        val expected = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"
        assertEquals(expected, PkceGenerator.computeS256Challenge(verifier))
    }
}
