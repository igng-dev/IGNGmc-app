package net.igng.mcstatus.core.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.igng.mcstatus.core.model.BootstrapResponse
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
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("token_type") val tokenType: String? = null,
    @SerialName("expires_in") val expiresIn: Long? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("refresh_expires_at") val refreshExpiresAt: String? = null,
    val scope: String? = null,
    @SerialName("session_id") val sessionId: String? = null,
    val user: OAuthTokenUser? = null,
)

@Serializable
private data class V1DailyLoginWire(
    val awarded: Boolean = false,
    val exp: Int? = null,
    @SerialName("base_exp") val baseExp: Int? = null,
    val multiplier: Double? = null,
    val level: Int? = null,
    val period: String? = null,
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
            .appendQueryParameter("scope", DEFAULT_SCOPES)
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
        val accessToken = response.accessToken?.takeIf { it.isNotBlank() }
            ?: error("授权交换未返回有效访问令牌")
        val refreshToken = response.refreshToken?.takeIf { it.isNotBlank() }
        val user = response.user ?: error("授权交换未返回用户信息")
        val expiresAt = response.expiresIn?.takeIf { it > 0 }?.let { Instant.now().plusSeconds(it).toString() }

        val bootstrap = runCatching { fetchBootstrap(accessToken) }.getOrNull()
        val viewerUser = bootstrap?.viewer?.user
        val adminRole = McAdminRole.fromWire(bootstrap?.viewer?.adminRole)

        return SavedAccount(
            userId = viewerUser?.resolvedNumericId?.takeIf { it > 0 } ?: user.id,
            username = viewerUser?.username?.takeIf { it.isNotBlank() } ?: user.username,
            nickname = viewerUser?.nickname ?: user.nickname,
            email = user.email,
            avatarUrl = viewerUser?.avatarUrl ?: user.avatar_url,
            token = accessToken,
            expiresAt = expiresAt,
            refreshToken = refreshToken,
            refreshExpiresAt = response.refreshExpiresAt,
            scope = response.scope ?: DEFAULT_SCOPES,
            sessionId = response.sessionId,
            deviceName = "IGNGmc Android",
            adminRoleWire = adminRole?.name,
        )
    }

    /**
     * Rotates the single-use OAuth 2.1 `refresh_token` at `sso.igng.net/api/oauth/token`.
     * Returns null when no refresh token is stored (legacy upgrade) or when the server rejects it.
     */
    suspend fun refreshSession(account: SavedAccount): SavedAccount? {
        val currentRefresh = account.refreshToken?.takeIf { it.isNotBlank() } ?: return null
        val body = buildJsonObject {
            put("grant_type", "refresh_token")
            put("client_id", CLIENT_ID)
            put("refresh_token", currentRefresh)
        }
        val response: OAuthTokenResponse = json.decodeFromString(
            apiClient.post("${endpoints.sso}/api/oauth/token", body.toString(), suppressAuthNotify = true),
        )
        val newAccess = response.accessToken?.takeIf { it.isNotBlank() } ?: return null
        val newRefresh = response.refreshToken?.takeIf { it.isNotBlank() } ?: return null
        val expiresAt = response.expiresIn?.takeIf { it > 0 }?.let { Instant.now().plusSeconds(it).toString() }
        return account.copy(
            token = newAccess,
            expiresAt = expiresAt ?: account.expiresAt,
            refreshToken = newRefresh,
            refreshExpiresAt = response.refreshExpiresAt ?: account.refreshExpiresAt,
            scope = response.scope ?: account.scope,
            sessionId = response.sessionId ?: account.sessionId,
            username = response.user?.username?.takeIf { it.isNotBlank() } ?: account.username,
            nickname = response.user?.nickname ?: account.nickname,
            email = response.user?.email ?: account.email,
        )
    }

    suspend fun fetchBootstrap(token: String? = null): BootstrapResponse =
        json.decodeFromString(
            apiClient.get(
                url = "${endpoints.api}/api/v1/mc/bootstrap",
                tokenOverride = token,
                suppressAuthNotify = !token.isNullOrBlank(),
            ),
        )

    suspend fun logout(token: String) {
        apiClient.post(
            url = "${endpoints.api}/api/v1/auth/logout",
            body = "{}",
            tokenOverride = token,
            suppressAuthNotify = true,
        )
    }

    suspend fun fetchCryptoChallenge(token: String? = null): CryptoChallenge =
        json.decodeFromString(
            apiClient.get(
                url = "${endpoints.api}/api/v1/auth/crypto",
                tokenOverride = token,
                suppressAuthNotify = true,
            ),
        )

    suspend fun settleDailyLogin(token: String? = null): DailyExpStatus? = runCatching {
        val wire: V1DailyLoginWire = json.decodeFromString(
            apiClient.post(
                url = "${endpoints.api}/api/v1/users/me/daily-login",
                body = "{}",
                tokenOverride = token,
                suppressAuthNotify = true,
            ),
        )
        DailyExpStatus(
            loggedIn = true,
            awarded = wire.awarded,
            exp = wire.exp,
            baseExp = wire.baseExp,
            level = wire.level,
            multiplier = wire.multiplier,
            period = wire.period,
        )
    }.getOrNull()

    companion object {
        const val CLIENT_ID = "igng_mc_android"
        const val REDIRECT_URI = "igngmc://auth/callback"
        const val CALLBACK_SCHEME = "igngmc"
        const val CALLBACK_HOST = "auth"
        const val CALLBACK_PATH = "/callback"
        const val DEFAULT_SCOPES =
            "profile:read profile:write devices:read devices:write " +
                "mc:accounts:read mc:accounts:write mc:logins:read mc:permissions:read " +
                "mc:lands:read mc:lands:write mc:vault:read mc:teleports:read " +
                "mc:fake-players:read mc:medals:read mc:medals:write " +
                "mc:tickets:read mc:tickets:write mc:media:write " +
                "mc:status:read mc:search:read mc:chat:read mc:admin:read mc:admin:write"
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
