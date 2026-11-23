package net.igng.mcstatus.core.data

import android.content.Context
import android.os.Build
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import coil3.ImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import java.util.Collections
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import net.igng.mcstatus.core.auth.AuthRepository
import net.igng.mcstatus.core.auth.OAuthPendingStore
import net.igng.mcstatus.core.auth.SecureSessionStore
import net.igng.mcstatus.core.model.AppPreferences
import net.igng.mcstatus.core.model.BootstrapResponse
import net.igng.mcstatus.core.model.DailyExpStatus
import net.igng.mcstatus.core.model.MotionIntensity
import net.igng.mcstatus.core.model.SavedAccount
import net.igng.mcstatus.core.model.SessionAuthStatus
import net.igng.mcstatus.core.model.SiteEndpoints
import net.igng.mcstatus.core.model.ThemeMode
import net.igng.mcstatus.core.network.ApiClient
import net.igng.mcstatus.core.network.ApiException
import net.igng.mcstatus.core.network.IgngHttpClientFactory
import net.igng.mcstatus.core.network.SessionExpiredNotifier
import net.igng.mcstatus.core.network.SingleFlightTokenRefresher
import net.igng.mcstatus.core.network.TokenRefreshResult

private val Context.igngPrefsDataStore by preferencesDataStore(name = "igng_mc_v2_prefs")

class SettingsRepository(private val context: Context) {
    val preferences: Flow<AppPreferences> = context.igngPrefsDataStore.data.map { prefs ->
        AppPreferences(
            themeMode = ThemeMode.entries.firstOrNull { it.id == prefs[KEY_THEME] } ?: ThemeMode.SYSTEM,
            motionIntensity = MotionIntensity.entries.firstOrNull { it.id == prefs[KEY_MOTION] } ?: MotionIntensity.FULL,
            hapticsEnabled = prefs[KEY_HAPTICS] ?: true,
        )
    }

    val recentWikiIds: Flow<List<Int>> = context.igngPrefsDataStore.data.map { prefs ->
        prefs[KEY_RECENT_WIKI].orEmpty().split(",").mapNotNull { it.toIntOrNull() }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.igngPrefsDataStore.edit { it[KEY_THEME] = mode.id }
    }

    suspend fun setMotionIntensity(intensity: MotionIntensity) {
        context.igngPrefsDataStore.edit { it[KEY_MOTION] = intensity.id }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.igngPrefsDataStore.edit { it[KEY_HAPTICS] = enabled }
    }

    suspend fun recordRecentWiki(pageId: Int) {
        context.igngPrefsDataStore.edit { prefs ->
            val current = prefs[KEY_RECENT_WIKI].orEmpty().split(",").mapNotNull { it.toIntOrNull() }
            val next = (listOf(pageId) + current.filterNot { it == pageId }).take(12)
            prefs[KEY_RECENT_WIKI] = next.joinToString(",")
        }
    }

    private companion object {
        val KEY_THEME = stringPreferencesKey("theme_mode")
        val KEY_MOTION = stringPreferencesKey("motion_intensity")
        val KEY_HAPTICS = booleanPreferencesKey("haptics_enabled")
        val KEY_RECENT_WIKI = stringPreferencesKey("recent_wiki_ids")
        val KEY_SESSION_EPOCH = intPreferencesKey("session_epoch")
    }
}

/**
 * Central application dependency container (zero annotation-processor overhead).
 */
class IgngAppContainer(
    val appContext: Context,
    val endpoints: SiteEndpoints,
    val clientVersion: String = IgngHttpClientFactory.DEFAULT_CLIENT_VERSION,
) {
    val sessionStore = SecureSessionStore(appContext)
    val oauthPendingStore = OAuthPendingStore(appContext)
    val sessionExpiredNotifier = SessionExpiredNotifier()
    val deviceId: String = sessionStore.getOrCreateDeviceId()

    private val _activeAccount = MutableStateFlow(sessionStore.activeAccount())
    val activeAccount: StateFlow<SavedAccount?> = _activeAccount.asStateFlow()

    private val _accounts = MutableStateFlow(sessionStore.accounts())
    val accounts: StateFlow<List<SavedAccount>> = _accounts.asStateFlow()

    private val _dailyExp = MutableStateFlow<DailyExpStatus?>(null)
    val dailyExp: StateFlow<DailyExpStatus?> = _dailyExp.asStateFlow()

    private val _bootstrap = MutableStateFlow<BootstrapResponse?>(null)
    val bootstrap: StateFlow<BootstrapResponse?> = _bootstrap.asStateFlow()

    private val _sessionEpoch = MutableStateFlow(0)
    val sessionEpoch: StateFlow<Int> = _sessionEpoch.asStateFlow()

    // Tokens proven invalid (e.g. pre-v2.1.0 legacy session tokens or revoked refresh families).
    // Suppressing them from subsequent requests prevents 401 poisoning of public optionalAuth tabs.
    private val invalidatedTokens: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())

    private val _sessionAuthStatus = MutableStateFlow(computeSessionAuthStatus(_activeAccount.value))
    val sessionAuthStatus: StateFlow<SessionAuthStatus> = _sessionAuthStatus.asStateFlow()

    private fun computeSessionAuthStatus(account: SavedAccount?): SessionAuthStatus = when {
        account == null -> SessionAuthStatus.SIGNED_OUT
        account.refreshToken.isNullOrBlank() -> SessionAuthStatus.LEGACY_UPGRADE_REQUIRED
        invalidatedTokens.contains(account.token) -> SessionAuthStatus.EXPIRED
        else -> SessionAuthStatus.ACTIVE
    }

    private fun syncSessionAuthStatus() {
        _sessionAuthStatus.value = computeSessionAuthStatus(_activeAccount.value)
    }

    fun activeUsableToken(): String? {
        val account = _activeAccount.value ?: return null
        if (account.refreshToken.isNullOrBlank()) return null
        val token = account.token.takeIf { it.isNotBlank() } ?: return null
        return token.takeUnless { invalidatedTokens.contains(it) }
    }

    private fun isActiveSessionInvalidated(): Boolean =
        computeSessionAuthStatus(_activeAccount.value).requiresReauth

    val okHttpClient = IgngHttpClientFactory.create(
        endpoints = endpoints,
        tokenProvider = { activeUsableToken() },
        deviceIdProvider = { deviceId },
        clientVersion = clientVersion,
    )

    // Dedicated client without refresh loop for OAuth token exchange / refresh calls.
    private val authApiClient = ApiClient(
        okHttpClient = okHttpClient,
        sessionExpiredNotifier = null,
        tokenRefresher = null,
    )

    val authRepository = AuthRepository(endpoints, authApiClient)

    private val tokenRefresher = SingleFlightTokenRefresher(
        currentTokenProvider = { activeUsableToken() },
        isTokenInvalidated = { invalidatedTokens.contains(it) },
        refreshBlock = { failedAccessToken ->
            val current = _activeAccount.value ?: return@SingleFlightTokenRefresher TokenRefreshResult.Invalidated
            if (current.token != failedAccessToken && !invalidatedTokens.contains(current.token) && !current.refreshToken.isNullOrBlank()) {
                syncSessionAuthStatus()
                return@SingleFlightTokenRefresher TokenRefreshResult.Refreshed(current.token)
            }
            if (current.refreshToken.isNullOrBlank()) {
                invalidatedTokens.add(failedAccessToken)
                _dailyExp.value = null
                syncSessionAuthStatus()
                return@SingleFlightTokenRefresher TokenRefreshResult.Invalidated
            }
            try {
                val rotated = authRepository.refreshSession(current)
                if (rotated == null) {
                    invalidatedTokens.add(failedAccessToken)
                    _dailyExp.value = null
                    syncSessionAuthStatus()
                    return@SingleFlightTokenRefresher TokenRefreshResult.Invalidated
                }
                invalidatedTokens.remove(rotated.token)
                sessionStore.save(rotated)
                _activeAccount.value = sessionStore.activeAccount()
                _accounts.value = sessionStore.accounts()
                syncSessionAuthStatus()
                TokenRefreshResult.Refreshed(rotated.token)
            } catch (error: ApiException) {
                if (error.statusCode in listOf(400, 401, 403)) {
                    invalidatedTokens.add(failedAccessToken)
                    _dailyExp.value = null
                    syncSessionAuthStatus()
                    TokenRefreshResult.Invalidated
                } else {
                    TokenRefreshResult.TransientFailure
                }
            } catch (_: Exception) {
                TokenRefreshResult.TransientFailure
            }
        },
    )

    val apiClient = ApiClient(
        okHttpClient = okHttpClient,
        sessionExpiredNotifier = sessionExpiredNotifier,
        tokenRefresher = tokenRefresher,
        hasInvalidatedSession = { isActiveSessionInvalidated() },
    )

    val imageLoader: ImageLoader = ImageLoader.Builder(appContext)
        .components {
            add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient }))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                add(AnimatedImageDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }
        .build()

    val exploreCache = LocalJsonCache(appContext, "igng_explore_cache")
    val wikiCache = LocalJsonCache(appContext, "igng_wiki_cache")
    val draftCache = LocalJsonCache(appContext, "igng_draft_cache")

    val settingsRepository = SettingsRepository(appContext)
    val exploreRepository = ExploreRepository(endpoints, apiClient, exploreCache)
    val statusRepository = StatusRepository(endpoints, apiClient)
    val chatRepository = ChatRepository(endpoints, apiClient)
    val wikiRepository = WikiRepository(endpoints, apiClient, wikiCache)
    val playerRepository = PlayerRepository(endpoints, apiClient, authRepository)
    val ticketRepository = TicketRepository(endpoints, apiClient, draftCache)
    val adminRepository = AdminRepository(endpoints, apiClient)

    suspend fun fetchBootstrapForCurrentSession(): BootstrapResponse =
        apiClient.json.decodeFromString(
            apiClient.get(
                url = "${endpoints.api}/api/v1/mc/bootstrap",
                allowAnonymousFallback = true,
            ),
        )

    suspend fun settleDailyLoginForCurrentSession(): DailyExpStatus? {
        val token = activeUsableToken() ?: return null
        return authRepository.settleDailyLogin(token)
    }

    fun saveAndActivateAccount(account: SavedAccount, resetDailyExp: Boolean = true) {
        invalidatedTokens.remove(account.token)
        sessionStore.save(account)
        _activeAccount.value = sessionStore.activeAccount()
        _accounts.value = sessionStore.accounts()
        syncSessionAuthStatus()
        if (resetDailyExp) {
            _dailyExp.value = null
            _sessionEpoch.value += 1
        }
    }

    fun switchAccount(userId: Int) {
        sessionStore.switchTo(userId)
        _activeAccount.value = sessionStore.activeAccount()
        _accounts.value = sessionStore.accounts()
        syncSessionAuthStatus()
        _dailyExp.value = null
        _sessionEpoch.value += 1
    }

    fun removeActiveAccount() {
        _activeAccount.value?.token?.let { invalidatedTokens.remove(it) }
        sessionStore.removeActive()
        _activeAccount.value = sessionStore.activeAccount()
        _accounts.value = sessionStore.accounts()
        syncSessionAuthStatus()
        _dailyExp.value = null
        _sessionEpoch.value += 1
    }

    fun updateDailyExp(status: DailyExpStatus?) {
        _dailyExp.value = status
    }

    fun updateBootstrap(response: BootstrapResponse?) {
        _bootstrap.value = response
    }

    suspend fun clearAllCaches() {
        exploreCache.clearAll()
        wikiCache.clearAll()
    }

    suspend fun cacheSizeBytes(): Long =
        exploreCache.sizeBytes() + wikiCache.sizeBytes() + draftCache.sizeBytes()
}
