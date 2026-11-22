package net.igng.mcstatus.core.model

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
    val token: String,
    val expiresAt: String? = null,
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
    val level: Int? = null,
    val multiplier: Double? = null,
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
