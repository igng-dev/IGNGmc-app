package net.igng.mcstatus.core.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import net.igng.mcstatus.core.model.DailyExpStatus
import net.igng.mcstatus.core.model.McAdminRole
import net.igng.mcstatus.core.model.SavedAccount
import net.igng.mcstatus.core.model.SiteEndpoints
import net.igng.mcstatus.core.network.ApiClient
import net.igng.mcstatus.core.network.CryptoChallenge

@Serializable
data class OAuthTokenUser(
    val id: Int,
    val username: String,
    val nickname: String? = null,
    val email: String? = null,
    val avatar_url: String? = null,
)

@Serializable
data class OAuthTokenResponse(
    @SerialName("session_token") val sessionToken: String? = null,
    @SerialName("session_id") val sessionId: String? = null,
    @SerialName("session_expires_at") val sessionExpiresAt: String? = null,
    val user: OAuthTokenUser? = null,
)

@Serializable
data class MobileMeResponse(
    val success: Boolean = false,
    val sessionId: String? = null,
    val deviceName: String? = null,
    val createdAt: String? = null,
    val lastUsedAt: String? = null,
    val expiresAt: String? = null,
    val user: OAuthTokenUser,
)

@Serializable
private data class DailyLoginWire(
    val ok: Boolean = false,
    val loggedIn: Boolean = false,
    val awarded: Boolean = false,
    val exp: Int? = null,
    val level: Int? = null,
    val multiplier: Double? = null,
)

class OAuthPendingStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("igng_mc_oauth_pending", Context.MODE_PRIVATE)

    fun save(pair: PkcePair) {
        prefs.edit()
            .putString(KEY_VERIFIER, pair.verifier)
            .putString(KEY_STATE, pair.state)
            .putLong(KEY_CREATED_AT, System.currentTimeMillis())
            .apply()
    }

    fun consume(expectedState: String): String? {
        val savedState = prefs.getString(KEY_STATE, null)
        val verifier = prefs.getString(KEY_VERIFIER, null)
        val createdAt = prefs.getLong(KEY_CREATED_AT, 0L)
        clear()
        if (savedState.isNullOrBlank() || verifier.isNullOrBlank()) return null
        if (savedState != expectedState) return null
        if (System.currentTimeMillis() - createdAt > 15 * 60 * 1000L) return null
        return verifier
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_VERIFIER = "verifier"
        const val KEY_STATE = "state"
        const val KEY_CREATED_AT = "created_at"
    }
}

class AuthRepository(
    private val endpoints: SiteEndpoints,
    private val apiClient: ApiClient,
    private val json: Json = apiClient.json,
) {
    fun buildAuthorizeUri(pkce: PkcePair, forceLogin: Boolean = false): Uri {
        val builder = Uri.parse("${endpoints.sso}/oauth/authorize").buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("state", pkce.state)
            .appendQueryParameter("code_challenge", pkce.challenge)
            .appendQueryParameter("code_challenge_method", "S256")
        if (forceLogin) {
            builder.appendQueryParameter("prompt", "login")
        }
        return builder.build()
    }

    suspend fun exchangeAuthorizationCode(code: String, codeVerifier: String): SavedAccount {
        val body = buildJsonObject {
            put("grant_type", "authorization_code")
            put("client_id", CLIENT_ID)
            put("code", code)
            put("redirect_uri", REDIRECT_URI)
            put("code_verifier", codeVerifier)
        }
        val response: OAuthTokenResponse = json.decodeFromString(
            apiClient.post("${endpoints.sso}/api/oauth/token", body.toString(), suppressAuthNotify = true),
        )
        val token = response.sessionToken?.takeIf { it.isNotBlank() }
            ?: error("授权交换未返回有效会话令牌")
        val user = response.user ?: error("授权交换未返回用户信息")
        val enriched = runCatching { fetchMe(token) }.getOrNull()
        val adminRole = runCatching { detectAdminRole(token) }.getOrNull()
        return SavedAccount(
            userId = enriched?.user?.id ?: user.id,
            username = enriched?.user?.username ?: user.username,
            nickname = enriched?.user?.nickname ?: user.nickname,
            email = enriched?.user?.email ?: user.email,
            avatarUrl = enriched?.user?.avatar_url ?: user.avatar_url,
            token = token,
            expiresAt = enriched?.expiresAt ?: response.sessionExpiresAt,
            deviceName = enriched?.deviceName ?: "IGNGmc Android",
            adminRoleWire = adminRole?.name,
        )
    }

    suspend fun fetchMe(token: String): MobileMeResponse =
        json.decodeFromString(
            apiClient.get(
                url = "${endpoints.sso}/api/mobile/auth/me",
                tokenOverride = token,
                suppressAuthNotify = true,
            ),
        )

    suspend fun logout(token: String) {
        apiClient.post(
            url = "${endpoints.sso}/api/mobile/auth/logout",
            body = "{}",
            tokenOverride = token,
            suppressAuthNotify = true,
        )
    }

    suspend fun fetchDashCryptoChallenge(token: String? = null): CryptoChallenge =
        json.decodeFromString(
            apiClient.get(
                url = "${endpoints.dash}/api/auth/crypto",
                tokenOverride = token,
                suppressAuthNotify = true,
            ),
        )

    suspend fun settleDailyLogin(token: String): DailyExpStatus? = runCatching {
        val wire: DailyLoginWire = json.decodeFromString(
            apiClient.post(
                url = "${endpoints.dash}/api/daily-login",
                body = "{}",
                tokenOverride = token,
                suppressAuthNotify = true,
            ),
        )
        DailyExpStatus(
            loggedIn = wire.loggedIn,
            awarded = wire.awarded,
            exp = wire.exp,
            level = wire.level,
            multiplier = wire.multiplier,
        )
    }.getOrNull()

    suspend fun detectAdminRole(token: String): McAdminRole? {
        // Lightweight non-mutating check: admin/punishments/records returns { role: "ADMIN"|"TECHADMIN"|"SUPERADMIN" }
        // when authorized and 403 otherwise.
        val roleFromAdmin = runCatching {
            val raw = apiClient.get(
                url = "${endpoints.dash}/api/mc/admin/punishments/records?page=1&pageSize=1",
                tokenOverride = token,
                suppressAuthNotify = true,
            )
            val roleStr = json.parseToJsonElement(raw).jsonObject["role"]?.jsonPrimitive?.content
            McAdminRole.fromWire(roleStr)
        }.getOrNull()
        if (roleFromAdmin != null) return roleFromAdmin

        val hasTicketsAdmin = runCatching {
            apiClient.get(
                url = "${endpoints.dash}/api/mc/admin/tickets/overview",
                tokenOverride = token,
                suppressAuthNotify = true,
            )
            true
        }.getOrDefault(false)
        return if (hasTicketsAdmin) McAdminRole.ADMIN else null
    }

    companion object {
        const val CLIENT_ID = "igng_mc_android"
        const val REDIRECT_URI = "igngmc://auth/callback"
        const val CALLBACK_SCHEME = "igngmc"
        const val CALLBACK_HOST = "auth"
        const val CALLBACK_PATH = "/callback"
    }
}

object CustomTabsLauncher {
    fun launchOAuth(context: Context, uri: Uri, isDark: Boolean) {
        val toolbarColor = if (isDark) 0xFF0B0F16.toInt() else 0xFFF4F6FA.toInt()
        val params = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(toolbarColor)
            .build()
        val intent = CustomTabsIntent.Builder()
            .setDefaultColorSchemeParams(params)
            .setShowTitle(true)
            .build()
        runCatching {
            intent.launchUrl(context, uri)
        }.onFailure {
            val fallback = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(fallback)
        }
    }

    fun openExternalUrl(context: Context, url: String, isDark: Boolean) {
        launchOAuth(context, Uri.parse(url), isDark)
    }
}
