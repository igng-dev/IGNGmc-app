package net.igng.mcstatus.core.model

import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

object IgngFormatters {
    private val localZone: ZoneId get() = ZoneId.systemDefault()
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.CHINA)
    private val shortDateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm", Locale.CHINA)
    private val timeOnlyFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)
    private val numberFormat = NumberFormat.getIntegerInstance(Locale.US)

    fun formatNumber(value: Long?): String =
        if (value == null) "—" else numberFormat.format(value)

    fun formatNumber(value: Int?): String = formatNumber(value?.toLong())

    fun formatDecimal(value: Double?, digits: Int = 1, suffix: String = ""): String {
        if (value == null || !value.isFinite()) return "—"
        return String.format(Locale.US, "%.${digits}f%s", value, suffix)
    }

    fun formatIsoDateTime(iso: String?, short: Boolean = false): String {
        if (iso.isNullOrBlank()) return "—"
        return runCatching {
            val instant = Instant.parse(iso)
            val formatter = if (short) shortDateTimeFormatter else dateTimeFormatter
            formatter.withZone(localZone).format(instant)
        }.getOrElse { iso.take(16).replace('T', ' ') }
    }

    fun formatEpochMillis(millis: Long?): String {
        if (millis == null || millis <= 0L) return "—"
        return runCatching {
            dateTimeFormatter.withZone(localZone).format(Instant.ofEpochMilli(millis))
        }.getOrDefault("—")
    }

    fun formatTimeOnly(iso: String?): String {
        if (iso.isNullOrBlank()) return "—"
        return runCatching {
            timeOnlyFormatter.withZone(localZone).format(Instant.parse(iso))
        }.getOrElse { iso }
    }

    fun formatRelativeTime(iso: String?, now: Instant = Instant.now()): String {
        if (iso.isNullOrBlank()) return "—"
        val instant = runCatching { Instant.parse(iso) }.getOrNull() ?: return formatIsoDateTime(iso, short = true)
        val seconds = Duration.between(instant, now).seconds
        return when {
            seconds < 45 -> "刚刚"
            seconds < 3600 -> "${seconds / 60} 分钟前"
            seconds < 86400 -> "${seconds / 3600} 小时前"
            seconds < 86400 * 7 -> "${seconds / 86400} 天前"
            else -> shortDateTimeFormatter.withZone(localZone).format(instant)
        }
    }

    fun formatPlaytimeSeconds(seconds: Long?): String {
        if (seconds == null || seconds < 0L) return "—"
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return when {
            hours >= 24 -> String.format(Locale.US, "%.1f 小时", seconds / 3600.0)
            hours > 0 -> "${hours} 小时 ${minutes} 分钟"
            minutes > 0 -> "${minutes} 分钟"
            else -> "${seconds} 秒"
        }
    }

    fun formatDistanceCm(cm: Long?): String {
        if (cm == null || cm < 0L) return "—"
        val meters = cm / 100.0
        return when {
            meters >= 1000.0 -> String.format(Locale.US, "%.2f km", meters / 1000.0)
            else -> String.format(Locale.US, "%.0f m", meters)
        }
    }

    fun formatBytes(bytes: Long?): String {
        if (bytes == null || bytes < 0L) return "—"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var index = 0
        while (value >= 1024.0 && index < units.lastIndex) {
            value /= 1024.0
            index++
        }
        return if (index == 0) "${bytes} B" else String.format(Locale.US, "%.2f %s", value, units[index])
    }

    fun formatCoordinate(x: Double?, y: Double?, z: Double?): String {
        if (x == null || z == null) return "—"
        return if (y != null) {
            "${x.toInt()}, ${y.toInt()}, ${z.toInt()}"
        } else {
            "${x.toInt()}, ${z.toInt()}"
        }
    }

    fun seasonStateLabel(state: String?): String = when (state?.lowercase()) {
        "live" -> "进行中"
        "persistent" -> "长期常驻"
        "hibernating" -> "休眠存档"
        "retired" -> "已关停"
        "draft" -> "未发布"
        else -> "运行中"
    }

    fun worldLabel(world: String?): String = when (world?.lowercase()) {
        "world", "overworld", "minecraft:overworld" -> "主世界"
        "world_nether", "nether", "minecraft:the_nether" -> "下界"
        "world_the_end", "the_end", "minecraft:the_end" -> "末地"
        null, "" -> "未知世界"
        else -> world
    }
}

fun JsonElement?.asBooleanFlag(): Boolean {
    val primitive = this as? JsonPrimitive ?: return false
    primitive.booleanOrNull?.let { return it }
    primitive.intOrNull?.let { return it != 0 }
    primitive.doubleOrNull?.let { return it != 0.0 }
    return when (primitive.contentOrNull?.trim()?.lowercase()) {
        "1", "true", "yes", "on", "allow" -> true
        else -> false
    }
}

data class McTextSpan(
    val text: String,
    val colorHex: Long? = null,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
)

object McTextParser {
    private val colorMap = mapOf(
        '0' to 0xFF000000L,
        '1' to 0xFF0000AAL,
        '2' to 0xFF00AA00L,
        '3' to 0xFF00AAAAL,
        '4' to 0xFFAA0000L,
        '5' to 0xFFAA00AAL,
        '6' to 0xFFFFAA00L,
        '7' to 0xFFAAAAAAL,
        '8' to 0xFF555555L,
        '9' to 0xFF5555FFL,
        'a' to 0xFF55FF55L,
        'b' to 0xFF55FFFFL,
        'c' to 0xFFFF5555L,
        'd' to 0xFFFF55FFL,
        'e' to 0xFFFFFF55L,
        'f' to 0xFFFFFFFFL,
    )

    fun stripFormatting(raw: String): String =
        parse(raw).joinToString("") { it.text }

    fun parse(raw: String): List<McTextSpan> {
        if (raw.isEmpty()) return emptyList()
        val result = mutableListOf<McTextSpan>()
        val buffer = StringBuilder()
        var color: Long? = null
        var bold = false
        var italic = false
        var underline = false
        var strike = false

        fun flush() {
            if (buffer.isNotEmpty()) {
                result += McTextSpan(
                    text = buffer.toString(),
                    colorHex = color,
                    bold = bold,
                    italic = italic,
                    underline = underline,
                    strikethrough = strike,
                )
                buffer.setLength(0)
            }
        }

        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            if (c == '§' && i + 1 < raw.length) {
                val code = raw[i + 1].lowercaseChar()
                if (code in colorMap) {
                    flush()
                    color = colorMap[code]
                    bold = false
                    italic = false
                    underline = false
                    strike = false
                    i += 2
                    continue
                }
                when (code) {
                    'l' -> { flush(); bold = true; i += 2; continue }
                    'o' -> { flush(); italic = true; i += 2; continue }
                    'n' -> { flush(); underline = true; i += 2; continue }
                    'm' -> { flush(); strike = true; i += 2; continue }
                    'r' -> {
                        flush()
                        color = null
                        bold = false
                        italic = false
                        underline = false
                        strike = false
                        i += 2
                        continue
                    }
                    'k' -> { i += 2; continue }
                }
            }
            buffer.append(c)
            i++
        }
        flush()
        return result
    }
}

sealed interface MedalValidationResult {
    data class Valid(val normalized: String) : MedalValidationResult
    data class Invalid(val message: String) : MedalValidationResult
}

object CustomMedalValidator {
    const val MIN_LENGTH = 1
    const val MAX_LENGTH = 16

    private val legacyFormatRegex = Regex("[§&][0-9a-fk-orxA-FK-ORX]|<[/#]?[a-zA-Z0-9_:-]+>")

    fun validate(raw: String): MedalValidationResult {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return MedalValidationResult.Invalid("自定义勋章名称不能为空。")
        val cpCount = trimmed.codePointCount(0, trimmed.length)
        if (cpCount < MIN_LENGTH) return MedalValidationResult.Invalid("自定义勋章名称至少需要 $MIN_LENGTH 个字符。")
        if (cpCount > MAX_LENGTH) return MedalValidationResult.Invalid("自定义勋章名称最多只能输入 $MAX_LENGTH 个字符。")
        for (ch in trimmed) {
            val code = ch.code
            if (code < 0x20 || code == 0x7F || code in 0x200B..0x200F || code in 0x202A..0x202E || code in 0x2060..0x206F) {
                return MedalValidationResult.Invalid("自定义勋章名称不能包含换行、控制字符或不可见字符。")
            }
        }
        if (legacyFormatRegex.containsMatchIn(trimmed)) {
            return MedalValidationResult.Invalid("自定义勋章名称不能包含颜色代码或格式标签。")
        }
        return MedalValidationResult.Valid(trimmed)
    }
}

data class SocialChannel(
    val key: String,
    val label: String,
    val badge: String,
    val summary: String,
    val copyValue: String? = null,
    val url: String? = null,
    val highlights: List<String> = emptyList(),
)

object IgngSocialChannels {
    val qqGroups: List<SocialChannel> = listOf(
        SocialChannel(
            key = "mc-qq-1",
            label = "IGNGmc QQ 1 群",
            badge = "主群",
            summary = "日常闲聊、维护通知、版本更新公告与进服求助都会优先同步到这里。",
            copyValue = "885647215",
            highlights = listOf("维护公告", "联机寻人", "问题求助"),
        ),
        SocialChannel(
            key = "mc-qq-2",
            label = "IGNGmc QQ 2 群",
            badge = "互通玩家群",
            summary = "与 1 群同享运营公告和群内互动机器人，两群任选其一加入即可。",
            copyValue = "975806691",
            highlights = listOf("公告同步", "Bot 互动", "轻松交流"),
        ),
    )

    val externalChannels: List<SocialChannel> = listOf(
        SocialChannel(
            key = "mc-discord",
            label = "IGNGmc Discord 频道",
            badge = "仅限 IGNGmc",
            summary = "专为 IGNGmc 玩家保留的 Discord 服务器，适合语音开黑、跨区联机与海外玩家交流。",
            url = "https://discord.gg/UQFETw6DsB",
            highlights = listOf("语音开黑", "海外友好"),
        ),
        SocialChannel(
            key = "bilibili-official",
            label = "B站 · 官方账号",
            badge = "官方视频",
            summary = "关注服务器演示视频、周目纪实、更新预告与活动集锦。",
            url = "https://space.bilibili.com/3546681266145440/",
            highlights = listOf("周目纪实", "更新预告"),
        ),
        SocialChannel(
            key = "bbs",
            label = "IGNG 官方 BBS 论坛",
            badge = "社区论坛",
            summary = "发长帖分享建筑作品、生存攻略、红石电路与建议反馈。",
            url = "https://www.igngbbs.net",
            highlights = listOf("建筑作品", "长文攻略"),
        ),
    )
}
