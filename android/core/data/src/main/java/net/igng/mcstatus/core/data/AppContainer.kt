package net.igng.mcstatus.core.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
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
import net.igng.mcstatus.core.model.SiteEndpoints
import net.igng.mcstatus.core.model.ThemeMode
import net.igng.mcstatus.core.network.ApiClient
import net.igng.mcstatus.core.network.IgngHttpClientFactory
import net.igng.mcstatus.core.network.SessionExpiredNotifier
import net.igng.mcstatus.core.network.SingleFlightTokenRefresher

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

    val okHttpClient = IgngHttpClientFactory.create(
        endpoints = endpoints,
        tokenProvider = { _activeAccount.value?.token },
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
        currentTokenProvider = { _activeAccount.value?.token },
        refreshBlock = { failedAccessToken ->
            val current = _activeAccount.value ?: return@SingleFlightTokenRefresher null
            if (current.token != failedAccessToken) return@SingleFlightTokenRefresher current.token
            val rotated = runCatching { authRepository.refreshSession(current) }.getOrNull()
                ?: return@SingleFlightTokenRefresher null
            sessionStore.save(rotated)
            _activeAccount.value = sessionStore.activeAccount()
            _accounts.value = sessionStore.accounts()
            rotated.token
        },
    )

    val apiClient = ApiClient(
        okHttpClient = okHttpClient,
        sessionExpiredNotifier = sessionExpiredNotifier,
        tokenRefresher = tokenRefresher,
    )

    val imageLoader: ImageLoader = ImageLoader.Builder(appContext)
        .components {
            add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient }))
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

    fun saveAndActivateAccount(account: SavedAccount, resetDailyExp: Boolean = true) {
        sessionStore.save(account)
        _activeAccount.value = sessionStore.activeAccount()
        _accounts.value = sessionStore.accounts()
        if (resetDailyExp) _dailyExp.value = null
    }

    fun switchAccount(userId: Int) {
        sessionStore.switchTo(userId)
        _activeAccount.value = sessionStore.activeAccount()
        _accounts.value = sessionStore.accounts()
        _dailyExp.value = null
    }

    fun removeActiveAccount() {
        sessionStore.removeActive()
        _activeAccount.value = sessionStore.activeAccount()
        _accounts.value = sessionStore.accounts()
        _dailyExp.value = null
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
