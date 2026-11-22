package net.igng.mcstatus.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class ThemeMode(val id: String, val label: String) {
    SYSTEM("system", "跟随系统"),
    LIGHT("light", "浅色画布"),
    DARK("dark", "深色画布"),
}

enum class MotionIntensity(val id: String, val label: String) {
    FULL("full", "完整动效"),
    REDUCED("reduced", "精简动效"),
}

enum class ServerThemeKey(
    val key: String,
    val displayName: String,
    val badge: String,
    val accentHex: Long,
) {
    CLEAR("clear", "纯净服", "官方纯净", 0xFF10B981),
    LITE_SERVER("lite-server", "轻量玩法服", "轻松养老", 0xFFF59E0B),
    MODS("mods", "MOD周目服", "轮换周目", 0xFF8B5CF6),
    BAN("ban", "作弊服", "开放外挂", 0xFFEF4444),
    MCSERVERS("mcservers", "档案馆", "历史存档", 0xFF06B6D4),
    ;

    companion object {
        fun resolve(raw: String?): ServerThemeKey {
            val normalized = raw?.trim()?.lowercase().orEmpty()
            return entries.firstOrNull { it.key == normalized } ?: when {
                normalized.contains("mod") -> MODS
                normalized.contains("ban") || normalized.contains("cheat") -> BAN
                normalized.contains("lite") -> LITE_SERVER
                normalized.contains("archive") || normalized.contains("mcserver") -> MCSERVERS
                else -> CLEAR
            }
        }
    }
}

enum class McAdminRole(val level: Int, val label: String) {
    ADMIN(1, "管理员"),
    TECHADMIN(2, "技术管理员"),
    SUPERADMIN(3, "超级管理员"),
    ;

    fun isElevated(): Boolean = level >= TECHADMIN.level

    companion object {
        fun fromWire(value: String?): McAdminRole? = when (value?.trim()?.uppercase()) {
            "ADMIN" -> ADMIN
            "TECHADMIN" -> TECHADMIN
            "SUPERADMIN" -> SUPERADMIN
            else -> null
        }
    }
}

@Serializable
data class SavedAccount(
    val userId: Int,
    val username: String,
    val nickname: String? = null,
    val email: String? = null,
    val avatarUrl: String? = null,
    /** OAuth 2.1 short-lived access_token (1h). */
    val token: String,
    val expiresAt: String? = null,
    /** OAuth 2.1 single-use rotating refresh_token (30d). Null for legacy upgraded sessions. */
    val refreshToken: String? = null,
    val refreshExpiresAt: String? = null,
    val scope: String? = null,
    val sessionId: String? = null,
    val deviceName: String? = null,
    val adminRoleWire: String? = null,
) {
    val displayName: String
        get() = nickname?.takeIf { it.isNotBlank() } ?: username

    val adminRole: McAdminRole?
        get() = McAdminRole.fromWire(adminRoleWire)
}

@Serializable
data class DailyExpStatus(
    val loggedIn: Boolean = false,
    val awarded: Boolean = false,
    val exp: Int? = null,
    val baseExp: Int? = null,
    val level: Int? = null,
    val multiplier: Double? = null,
    val period: String? = null,
)

@Serializable
data class BootstrapUrlsDto(
    @SerialName("api_base_url") val apiBaseUrl: String = "https://www.igng.net",
    @SerialName("sso_base_url") val ssoBaseUrl: String = "https://sso.igng.net",
    @SerialName("mc_web_url") val mcWebUrl: String = "https://mc.igng.net",
    @SerialName("dash_web_url") val dashWebUrl: String = "https://dash.igng.net",
    @SerialName("wiki_web_url") val wikiWebUrl: String = "https://wiki.igng.net",
    @SerialName("map_web_url") val mapWebUrl: String = "https://mc.igng.net/map",
)

@Serializable
data class FeatureAvailabilityDto(
    val available: Boolean = false,
    val reason: String? = null,
)

@Serializable
data class BootstrapFeaturesDto(
    @SerialName("login_history") val loginHistory: FeatureAvailabilityDto = FeatureAvailabilityDto(),
    @SerialName("penalty_history") val penaltyHistory: FeatureAvailabilityDto = FeatureAvailabilityDto(),
    @SerialName("mc_account_bind_web_only") val mcAccountBindWebOnly: Boolean = true,
    @SerialName("wiki_default_kb") val wikiDefaultKb: String = "IGNGmc",
)

@Serializable
data class BootstrapUserDto(
    val id: String = "",
    @SerialName("numeric_id") val numericId: Int = 0,
    val username: String = "",
    val nickname: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
) {
    val resolvedNumericId: Int
        get() = if (numericId > 0) numericId else (V1Id.parseNumeric(id) ?: 0)
}

@Serializable
data class BootstrapBoundAccountDto(
    @SerialName("account_id") val accountId: String = "",
    val username: String = "",
    val uuid: String? = null,
    @SerialName("uuid_provenance") val uuidProvenance: String? = null,
    val verified: Boolean = false,
    @SerialName("is_public") val isPublic: Boolean = false,
)

@Serializable
data class BootstrapViewerDto(
    val authenticated: Boolean = false,
    val user: BootstrapUserDto? = null,
    @SerialName("admin_role") val adminRole: String? = null,
    @SerialName("is_mc_admin") val isMcAdmin: Boolean = false,
    @SerialName("is_elevated_admin") val isElevatedAdmin: Boolean = false,
    @SerialName("bound_mc_accounts") val boundMcAccounts: List<BootstrapBoundAccountDto> = emptyList(),
)

@Serializable
data class BootstrapResponse(
    @SerialName("api_version") val apiVersion: String = "1.14",
    @SerialName("contract_version") val contractVersion: String = "1.14.0",
    @SerialName("server_time") val serverTime: String? = null,
    val urls: BootstrapUrlsDto = BootstrapUrlsDto(),
    val features: BootstrapFeaturesDto = BootstrapFeaturesDto(),
    val viewer: BootstrapViewerDto = BootstrapViewerDto(),
)

data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val motionIntensity: MotionIntensity = MotionIntensity.FULL,
    val hapticsEnabled: Boolean = true,
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Content<T>(val data: T, val refreshing: Boolean = false) : UiState<T>
    data class Error(val message: String, val code: String? = null) : UiState<Nothing>
}
