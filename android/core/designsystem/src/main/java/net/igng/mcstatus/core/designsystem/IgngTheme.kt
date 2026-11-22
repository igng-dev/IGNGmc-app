package net.igng.mcstatus.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.igng.mcstatus.core.model.MotionIntensity
import net.igng.mcstatus.core.model.ServerThemeKey
import net.igng.mcstatus.core.model.ThemeMode

@Immutable
data class IgngColors(
    val isDark: Boolean,
    val canvas: Color,
    val tile: Color,
    val tileSoft: Color,
    val tilePressed: Color,
    val line: Color,
    val lineStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val accentHover: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val accentGlow: Color,
    val auraBlue: Color,
    val auraViolet: Color,
    val success: Color,
    val successBg: Color,
    val warning: Color,
    val warningBg: Color,
    val danger: Color,
    val dangerBg: Color,
    val glassBar: Color,
    val scrim: Color,
)

val IgngLightColors = IgngColors(
    isDark = false,
    canvas = Color(0xFFF4F6FA),
    tile = Color(0xFFFFFFFF),
    tileSoft = Color(0xFFF7F9FC),
    tilePressed = Color(0xFFF0F4FA),
    line = Color(0x170F172A),
    lineStrong = Color(0x2E0F172A),
    textPrimary = Color(0xFF1F2328),
    textSecondary = Color(0xFF57606A),
    textMuted = Color(0xFF8C959F),
    accent = Color(0xFF2563EB),
    accentHover = Color(0xFF1D4ED8),
    onAccent = Color(0xFFFFFFFF),
    accentSoft = Color(0x172563EB),
    accentGlow = Color(0x382563EB),
    auraBlue = Color(0x102563EB),
    auraViolet = Color(0x0D8B5CF6),
    success = Color(0xFF116329),
    successBg = Color(0x14116329),
    warning = Color(0xFF9A6700),
    warningBg = Color(0x1A9A6700),
    danger = Color(0xFFB42318),
    dangerBg = Color(0x14B42318),
    glassBar = Color(0xEBFFFFFF),
    scrim = Color(0x660B0F16),
)

val IgngDarkColors = IgngColors(
    isDark = true,
    canvas = Color(0xFF0B0F16),
    tile = Color(0xFF141A23),
    tileSoft = Color(0xFF19202B),
    tilePressed = Color(0xFF1E2735),
    line = Color(0x17FFFFFF),
    lineStrong = Color(0x30FFFFFF),
    textPrimary = Color(0xFFF0F6FC),
    textSecondary = Color(0xFF8B949E),
    textMuted = Color(0xFF6E7681),
    accent = Color(0xFF58A6FF),
    accentHover = Color(0xFF79B8FF),
    onAccent = Color(0xFF0D1117),
    accentSoft = Color(0x2158A6FF),
    accentGlow = Color(0x4758A6FF),
    auraBlue = Color(0x1458A6FF),
    auraViolet = Color(0x128B5CF6),
    success = Color(0xFF56D364),
    successBg = Color(0x1C56D364),
    warning = Color(0xFFE3B341),
    warningBg = Color(0x21E3B341),
    danger = Color(0xFFFF7B72),
    dangerBg = Color(0x1CFF7B72),
    glassBar = Color(0xE8141A23),
    scrim = Color(0x99000000),
)

@Immutable
data class ServerFamilyVisual(
    val key: ServerThemeKey,
    val accent: Color,
    val secondaryGlow: Color,
    val softTint: Color,
    val borderGlow: Color,
)

fun serverFamilyVisual(key: ServerThemeKey, isDark: Boolean): ServerFamilyVisual {
    val alphaSoft = if (isDark) 0.18f else 0.11f
    val alphaBorder = if (isDark) 0.36f else 0.28f
    return when (key) {
        ServerThemeKey.CLEAR -> ServerFamilyVisual(
            key = key,
            accent = Color(0xFF10B981),
            secondaryGlow = Color(0xFFF59E0B),
            softTint = Color(0xFF10B981).copy(alpha = alphaSoft),
            borderGlow = Color(0xFF10B981).copy(alpha = alphaBorder),
        )
        ServerThemeKey.LITE_SERVER -> ServerFamilyVisual(
            key = key,
            accent = Color(0xFFF59E0B),
            secondaryGlow = Color(0xFFD97706),
            softTint = Color(0xFFF59E0B).copy(alpha = alphaSoft),
            borderGlow = Color(0xFFF59E0B).copy(alpha = alphaBorder),
        )
        ServerThemeKey.MODS -> ServerFamilyVisual(
            key = key,
            accent = Color(0xFF8B5CF6),
            secondaryGlow = Color(0xFF6366F1),
            softTint = Color(0xFF8B5CF6).copy(alpha = alphaSoft),
            borderGlow = Color(0xFF8B5CF6).copy(alpha = alphaBorder),
        )
        ServerThemeKey.BAN -> ServerFamilyVisual(
            key = key,
            accent = Color(0xFFEF4444),
            secondaryGlow = Color(0xFFB91C1C),
            softTint = Color(0xFFEF4444).copy(alpha = alphaSoft),
            borderGlow = Color(0xFFEF4444).copy(alpha = alphaBorder),
        )
        ServerThemeKey.MCSERVERS -> ServerFamilyVisual(
            key = key,
            accent = Color(0xFF06B6D4),
            secondaryGlow = Color(0xFF0E7490),
            softTint = Color(0xFF06B6D4).copy(alpha = alphaSoft),
            borderGlow = Color(0xFF06B6D4).copy(alpha = alphaBorder),
        )
    }
}

@Immutable
data class IgngRadii(
    val sm: RoundedCornerShape = RoundedCornerShape(10.dp),
    val md: RoundedCornerShape = RoundedCornerShape(14.dp),
    val lg: RoundedCornerShape = RoundedCornerShape(18.dp),
    val hero: RoundedCornerShape = RoundedCornerShape(22.dp),
    val pill: RoundedCornerShape = RoundedCornerShape(999.dp),
)

@Immutable
data class IgngTextStyles(
    val heroTitle: TextStyle = TextStyle(
        fontSize = 26.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.5).sp,
    ),
    val pageTitle: TextStyle = TextStyle(
        fontSize = 21.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.3).sp,
    ),
    val sectionTitle: TextStyle = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.1).sp,
    ),
    val cardTitle: TextStyle = TextStyle(
        fontSize = 15.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.Bold,
    ),
    val body: TextStyle = TextStyle(
        fontSize = 14.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.Normal,
    ),
    val bodyMedium: TextStyle = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
    ),
    val caption: TextStyle = TextStyle(
        fontSize = 12.sp,
        lineHeight = 17.sp,
        fontWeight = FontWeight.Normal,
    ),
    val badge: TextStyle = TextStyle(
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    val metricLarge: TextStyle = TextStyle(
        fontSize = 24.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFeatureSettings = "tnum",
        letterSpacing = (-0.4).sp,
    ),
    val metricMedium: TextStyle = TextStyle(
        fontSize = 17.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Bold,
        fontFeatureSettings = "tnum",
    ),
    val mono: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 12.5.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Medium,
        fontFeatureSettings = "tnum",
    ),
)

val LocalIgngColors = staticCompositionLocalOf { IgngLightColors }
val LocalIgngRadii = staticCompositionLocalOf { IgngRadii() }
val LocalIgngTypography = staticCompositionLocalOf { IgngTextStyles() }
val LocalMotionReduced = staticCompositionLocalOf { false }
val LocalHapticsEnabled = staticCompositionLocalOf { true }

object IgngTheme {
    val colors: IgngColors
        @Composable
        @ReadOnlyComposable
        get() = LocalIgngColors.current

    val radii: IgngRadii
        @Composable
        @ReadOnlyComposable
        get() = LocalIgngRadii.current

    val type: IgngTextStyles
        @Composable
        @ReadOnlyComposable
        get() = LocalIgngTypography.current

    val motionReduced: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalMotionReduced.current
}

@Composable
fun IgngTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    motionIntensity: MotionIntensity = MotionIntensity.FULL,
    hapticsEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (dark) IgngDarkColors else IgngLightColors
    val systemReduced = rememberSystemReducedMotion()
    val reduced = motionIntensity == MotionIntensity.REDUCED || systemReduced

    val m3Scheme = if (dark) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            primaryContainer = colors.accentSoft,
            onPrimaryContainer = colors.accent,
            secondary = colors.textSecondary,
            background = colors.canvas,
            onBackground = colors.textPrimary,
            surface = colors.tile,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.tileSoft,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.line,
            outlineVariant = colors.lineStrong,
            error = colors.danger,
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            primaryContainer = colors.accentSoft,
            onPrimaryContainer = colors.accent,
            secondary = colors.textSecondary,
            background = colors.canvas,
            onBackground = colors.textPrimary,
            surface = colors.tile,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.tileSoft,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.line,
            outlineVariant = colors.lineStrong,
            error = colors.danger,
        )
    }

    val radii = IgngRadii()
    val type = IgngTextStyles()

    CompositionLocalProvider(
        LocalIgngColors provides colors,
        LocalIgngRadii provides radii,
        LocalIgngTypography provides type,
        LocalMotionReduced provides reduced,
        LocalHapticsEnabled provides hapticsEnabled,
    ) {
        MaterialTheme(
            colorScheme = m3Scheme,
            shapes = Shapes(
                small = radii.sm,
                medium = radii.md,
                large = radii.lg,
            ),
            typography = Typography(
                headlineMedium = type.heroTitle,
                titleLarge = type.pageTitle,
                titleMedium = type.sectionTitle,
                titleSmall = type.cardTitle,
                bodyMedium = type.body,
                bodySmall = type.caption,
                labelMedium = type.badge,
            ),
            content = content,
        )
    }
}
