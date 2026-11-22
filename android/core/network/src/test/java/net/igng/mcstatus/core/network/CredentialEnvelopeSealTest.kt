package net.igng.mcstatus.core.network

import java.security.KeyPairGenerator
import java.security.spec.MGF1ParameterSpec
import java.time.Instant
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class CredentialEnvelopeSealTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun sealsAndDecryptsV1IsoServerTimeAndSnakeCaseFields() {
        val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val spki = Base64.getEncoder().encodeToString(keyPair.public.encoded)
        val raw = """
            {
              "nonce": "nonce-v1-iso-test-9999",
              "alg": "RSA-OAEP-256+A256GCM",
              "public_key": "$spki",
              "server_time": "2026-11-22T12:00:00Z",
              "ttl_ms": 60000
            }
        """.trimIndent()
        val challenge = json.decodeFromString<CryptoChallenge>(raw)

        val sealed = CredentialEnvelopeSeal.seal(
            challenge = challenge,
            fields = mapOf("old_password" to "alpha-sample", "new_password" to "beta-sample"),
        )

        assertEquals(Instant.parse("2026-11-22T12:00:00Z").toEpochMilli(), sealed.ts)

        val rsa = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        rsa.init(
            Cipher.DECRYPT_MODE,
            keyPair.private,
            OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT),
        )
        val rawAes = rsa.doFinal(Base64.getDecoder().decode(sealed.ek))
        val aes = Cipher.getInstance("AES/GCM/NoPadding")
        aes.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(rawAes, "AES"),
            GCMParameterSpec(128, Base64.getDecoder().decode(sealed.iv)),
        )
        aes.updateAAD("${sealed.nonce}|${sealed.ts}".toByteArray(Charsets.UTF_8))
        val plaintext = String(aes.doFinal(Base64.getDecoder().decode(sealed.ct)), Charsets.UTF_8)
        val obj = Json.parseToJsonElement(plaintext).jsonObject
        assertEquals("alpha-sample", obj["old_password"]?.jsonPrimitive?.content)
        assertEquals("beta-sample", obj["new_password"]?.jsonPrimitive?.content)
    }
}
