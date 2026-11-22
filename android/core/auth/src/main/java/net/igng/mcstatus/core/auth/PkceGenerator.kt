package net.igng.mcstatus.core.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

data class PkcePair(
    val verifier: String,
    val challenge: String,
    val state: String,
)

object PkceGenerator {
    private val random = SecureRandom()
    private val encoder = Base64.getUrlEncoder().withoutPadding()

    fun generate(): PkcePair {
        val verifierBytes = ByteArray(48).also { random.nextBytes(it) }
        val stateBytes = ByteArray(24).also { random.nextBytes(it) }
        val verifier = encoder.encodeToString(verifierBytes)
        val challenge = computeS256Challenge(verifier)
        val state = encoder.encodeToString(stateBytes)
        return PkcePair(verifier = verifier, challenge = challenge, state = state)
    }

    fun computeS256Challenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(verifier.toByteArray(Charsets.US_ASCII))
        return encoder.encodeToString(digest)
    }
}
