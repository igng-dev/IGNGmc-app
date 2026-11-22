package net.igng.mcstatus.core.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import net.igng.mcstatus.core.model.SavedAccount

/**
 * Hardware-backed Android Keystore AES-256-GCM encrypted multi-account session vault.
 * Preserves compatibility with v1.0.10 / v2.0.0 key alias and SharedPreferences file so upgrading
 * users transition cleanly to OAuth 2.1 rotating access_token + refresh_token storage.
 */
class SecureSessionStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    fun getOrCreateDeviceId(): String {
        val existing = preferences.getString(KEY_DEVICE_ID, null)?.trim()
        if (!existing.isNullOrBlank() && DEVICE_ID_REGEX.matches(existing)) {
            return existing
        }
        val generated = "dev_" + UUID.randomUUID().toString().replace("-", "").lowercase().take(24)
        preferences.edit().putString(KEY_DEVICE_ID, generated).apply()
        return generated
    }

    fun save(account: SavedAccount) {
        val key = accountKey(account.userId)
        val ids = accountIds().toMutableSet().apply { add(account.userId.toString()) }
        val editor = preferences.edit()
            .putStringSet(KEY_ACCOUNT_IDS, ids)
            .putString(KEY_ACTIVE_USER_ID, account.userId.toString())
            .putString("${key}_token", encrypt(account.token))
            .putString("${key}_username", account.username)
            .putString("${key}_nickname", account.nickname.orEmpty())
            .putString("${key}_email", account.email.orEmpty())
            .putString("${key}_avatar", account.avatarUrl.orEmpty())
            .putString("${key}_expires", account.expiresAt.orEmpty())
            .putString("${key}_refresh_expires", account.refreshExpiresAt.orEmpty())
            .putString("${key}_scope", account.scope.orEmpty())
            .putString("${key}_session_id", account.sessionId.orEmpty())
            .putString("${key}_device", account.deviceName.orEmpty())
            .putString("${key}_role", account.adminRoleWire.orEmpty())
        val refresh = account.refreshToken?.takeIf { it.isNotBlank() }
        if (refresh != null) {
            editor.putString("${key}_refresh", encrypt(refresh))
        } else {
            editor.remove("${key}_refresh")
        }
        editor.apply()
    }

    fun activeAccount(): SavedAccount? = activeUserId()?.let(::account)

    fun accounts(): List<SavedAccount> =
        accountIds().mapNotNull { it.toIntOrNull() }.sorted().mapNotNull(::account)

    fun switchTo(userId: Int): SavedAccount? = account(userId)?.also {
        preferences.edit().putString(KEY_ACTIVE_USER_ID, userId.toString()).apply()
    }

    fun removeActive(): SavedAccount? {
        activeUserId()?.let(::remove)
        val remaining = accounts().firstOrNull()
        if (remaining != null) {
            preferences.edit().putString(KEY_ACTIVE_USER_ID, remaining.userId.toString()).apply()
        }
        return remaining
    }

    fun remove(userId: Int) {
        val key = accountKey(userId)
        val ids = accountIds().toMutableSet().apply { remove(userId.toString()) }
        val editor = preferences.edit()
            .remove("${key}_token")
            .remove("${key}_refresh")
            .remove("${key}_refresh_expires")
            .remove("${key}_scope")
            .remove("${key}_session_id")
            .remove("${key}_username")
            .remove("${key}_nickname")
            .remove("${key}_email")
            .remove("${key}_avatar")
            .remove("${key}_expires")
            .remove("${key}_device")
            .remove("${key}_role")
            .putStringSet(KEY_ACCOUNT_IDS, ids)
        if (activeUserId() == userId) {
            editor.remove(KEY_ACTIVE_USER_ID)
        }
        editor.apply()
    }

    private fun account(userId: Int): SavedAccount? {
        val key = accountKey(userId)
        val encrypted = preferences.getString("${key}_token", null) ?: return null
        val token = runCatching { decrypt(encrypted) }.getOrNull()?.takeIf { it.isNotBlank() } ?: return null
        val encryptedRefresh = preferences.getString("${key}_refresh", null)
        val refreshToken = encryptedRefresh?.let { enc ->
            runCatching { decrypt(enc) }.getOrNull()?.takeIf { it.isNotBlank() }
        }
        return SavedAccount(
            userId = userId,
            username = preferences.getString("${key}_username", "").orEmpty(),
            nickname = preferences.getString("${key}_nickname", "")?.ifBlank { null },
            email = preferences.getString("${key}_email", "")?.ifBlank { null },
            avatarUrl = preferences.getString("${key}_avatar", "")?.ifBlank { null },
            token = token,
            expiresAt = preferences.getString("${key}_expires", "")?.ifBlank { null },
            refreshToken = refreshToken,
            refreshExpiresAt = preferences.getString("${key}_refresh_expires", "")?.ifBlank { null },
            scope = preferences.getString("${key}_scope", "")?.ifBlank { null },
            sessionId = preferences.getString("${key}_session_id", "")?.ifBlank { null },
            deviceName = preferences.getString("${key}_device", "")?.ifBlank { null },
            adminRoleWire = preferences.getString("${key}_role", "")?.ifBlank { null },
        )
    }

    private fun accountIds(): Set<String> = preferences.getStringSet(KEY_ACCOUNT_IDS, emptySet()).orEmpty()
    private fun activeUserId(): Int? = preferences.getString(KEY_ACTIVE_USER_ID, null)?.toIntOrNull()
    private fun accountKey(userId: Int) = "account_$userId"

    private fun secretKey(): SecretKey {
        val existing = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existing != null) return existing.secretKey
        return KeyGenerator.getInstance("AES", "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
        }.generateKey()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        return base64(cipher.iv) + "." + base64(cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8)))
    }

    private fun decrypt(value: String): String {
        val parts = value.split(".")
        require(parts.size == 2)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, decode(parts[0])))
        return String(cipher.doFinal(decode(parts[1])), StandardCharsets.UTF_8)
    }

    private fun base64(value: ByteArray) = Base64.encodeToString(value, Base64.NO_WRAP)
    private fun decode(value: String) = Base64.decode(value, Base64.NO_WRAP)

    private companion object {
        const val PREFS_NAME = "igng_mc_auth"
        const val KEY_ALIAS = "igng_mc_session_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_ACCOUNT_IDS = "account_ids"
        const val KEY_ACTIVE_USER_ID = "active_user_id"
        const val KEY_DEVICE_ID = "v1_device_id"
        val DEVICE_ID_REGEX = Regex("^[a-z0-9_-]{8,64}$")
    }
}
