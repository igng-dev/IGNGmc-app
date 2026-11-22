package net.igng.mcstatus.core.network

import java.security.KeyPairGenerator
import java.security.spec.MGF1ParameterSpec
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
    @Test
    fun sealsAndDecryptsWithSha256OaepAndAesGcmAad() {
        val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val spki = Base64.getEncoder().encodeToString(keyPair.public.encoded)
        val challenge = CryptoChallenge(
            nonce = "nonce-unit-test-1234",
            publicKey = spki,
            serverTime = 1763800000000L,
        )

        val sealed = CredentialEnvelopeSeal.seal(
            challenge = challenge,
            fields = mapOf("oldField" to "alpha-sample", "newField" to "beta-sample"),
        )

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
        assertEquals("alpha-sample", obj["oldField"]?.jsonPrimitive?.content)
        assertEquals("beta-sample", obj["newField"]?.jsonPrimitive?.content)
    }
}
