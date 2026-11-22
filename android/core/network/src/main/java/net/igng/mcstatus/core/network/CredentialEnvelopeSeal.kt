package net.igng.mcstatus.core.network

import java.security.KeyFactory
import java.security.SecureRandom
import java.security.spec.MGF1ParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class CryptoChallenge(
    val nonce: String,
    val alg: String = "RSA-OAEP-256+A256GCM",
    val publicKey: String = "",
    val public_key: String = "",
    val serverTime: Long? = null,
    val ttlMs: Long? = null,
) {
    val resolvedPublicKey: String
        get() = publicKey.ifBlank { public_key }
}

@Serializable
data class SealedCredential(
    val nonce: String,
    val ts: Long,
    val ek: String,
    val iv: String,
    val ct: String,
)

/**
 * Native Kotlin implementation of `@igng/credential-envelope/client.js`.
 *
 * Uses AES-256-GCM (AAD = "$nonce|$ts", 128-bit tag) to encrypt the JSON map
 * of sensitive fields and wraps the ephemeral AES key using RSA-OAEP with
 * SHA-256 for both digest and MGF1 to match Web Crypto / Node crypto.
 */
object CredentialEnvelopeSeal {
    private val secureRandom = SecureRandom()

    fun seal(
        challenge: CryptoChallenge,
        fields: Map<String, String>,
        fallbackNowMs: Long = System.currentTimeMillis(),
    ): SealedCredential {
        val spkiBase64 = challenge.resolvedPublicKey
        require(challenge.nonce.isNotBlank() && spkiBase64.isNotBlank()) {
            "加密挑战参数无效，请重试。"
        }

        val ts = challenge.serverTime?.takeIf { it > 0L } ?: fallbackNowMs
        val payloadJson = Json.encodeToString(
            JsonObject.serializer(),
            JsonObject(fields.mapValues { (_, v) -> JsonPrimitive(v) }),
        )

        val aesKey = KeyGenerator.getInstance("AES").apply { init(256, secureRandom) }.generateKey()
        val iv = ByteArray(12).also { secureRandom.nextBytes(it) }
        val aad = "${challenge.nonce}|$ts".toByteArray(Charsets.UTF_8)

        val aesCipher = Cipher.getInstance("AES/GCM/NoPadding")
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, GCMParameterSpec(128, iv))
        aesCipher.updateAAD(aad)
        val ciphertext = aesCipher.doFinal(payloadJson.toByteArray(Charsets.UTF_8))

        val spkiBytes = Base64.getDecoder().decode(spkiBase64)
        val rsaPublicKey = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(spkiBytes))
        val oaepSpec = OAEPParameterSpec(
            "SHA-256",
            "MGF1",
            MGF1ParameterSpec.SHA256,
            PSource.PSpecified.DEFAULT,
        )
        val rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        rsaCipher.init(Cipher.ENCRYPT_MODE, rsaPublicKey, oaepSpec)
        val wrappedKey = rsaCipher.doFinal(aesKey.encoded)

        val encoder = Base64.getEncoder()
        return SealedCredential(
            nonce = challenge.nonce,
            ts = ts,
            ek = encoder.encodeToString(wrappedKey),
            iv = encoder.encodeToString(iv),
            ct = encoder.encodeToString(ciphertext),
        )
    }
}
