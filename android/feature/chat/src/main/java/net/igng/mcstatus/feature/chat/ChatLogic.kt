package net.igng.mcstatus.feature.chat

import androidx.compose.ui.graphics.Color
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import net.igng.mcstatus.core.data.ChatAttachmentDto
import net.igng.mcstatus.core.data.ChatGroupSourceDto
import net.igng.mcstatus.core.data.ChatLastMessageDto
import net.igng.mcstatus.core.data.ChatMessageDto
import net.igng.mcstatus.core.data.ChatPersonaDto
import net.igng.mcstatus.core.data.ChatRelationshipDto
import net.igng.mcstatus.core.data.ChatSenderBadgeDto
import net.igng.mcstatus.core.data.ChatSenderProfileDto
import net.igng.mcstatus.core.data.ChatServerSourceDto
import net.igng.mcstatus.core.data.ChatSourcesResponse
import net.igng.mcstatus.core.data.ChatStructurePartDto
import net.igng.mcstatus.core.model.McTextParser
import net.igng.mcstatus.core.model.SavedAccount

// ==================== Platform & Session Taxonomy (Aligned with chat.igng.net) ====================

enum class ChatSessionKind(val label: String, val compactLabel: String) {
    QQ_GROUP("QQ 群聊", "QQ 群"),
    QQ_PRIVATE("QQ 私聊", "QQ 私聊"),
    WECHAT_GROUP("微信群聊", "微信群"),
    WECHAT_PRIVATE("微信私聊", "微信私聊"),
    MC_SERVER("MC 服务器", "服务器"),
}

enum class ChatSessionTab(val label: String) {
    ALL("全部"),
    QQ("QQ"),
    WECHAT("微信"),
    MC("MC"),
    PRIVATE("私聊"),
}

data class ChatSessionItem(
    val key: String,
    val apiSource: String,       // "qq" or "server"
    val targetId: String,        // group_id or numeric server id string
    val serverNumericId: Int?,   // non-null for MC_SERVER
    val platformKey: String,     // "qq" | "wechat" | "mc"
    val kind: ChatSessionKind,
    val isPrivate: Boolean,
    val name: String,
    val avatarUrl: String?,
    val isAdminGroup: Boolean,
    val isTechGroup: Boolean,
    val messageCount: Int,
    val lastMessageAt: String?,
    val lastTimestampMs: Long,
    val previewSender: String?,
    val previewContent: String?,
    val previewHasAtAll: Boolean,
    val persona: ChatPersonaDto?,
) {
    val displayNumber: String?
        get() = when (kind) {
            ChatSessionKind.QQ_GROUP -> targetId.takeIf { it.all(Char::isDigit) }
            ChatSessionKind.QQ_PRIVATE -> targetId.removePrefix("-").takeIf { it.all(Char::isDigit) }
            else -> null
        }

    val formattedPreview: String
        get() {
            val rawContent = previewContent?.trim().orEmpty()
            if (rawContent.isBlank()) {
                return when (kind) {
                    ChatSessionKind.MC_SERVER -> "Minecraft 服务器聊天"
                    else -> "${kind.label}消息流"
                }
            }
            val cleaned = if (kind == ChatSessionKind.MC_SERVER) {
                McTextParser.parse(rawContent).joinToString("") { it.text }
            } else {
                rawContent
            }.replace(Regex("\\s+"), " ").trim()
            val sender = previewSender?.trim().orEmpty()
            return when {
                sender.isBlank() -> cleaned
                isPrivate && sender == name -> cleaned
                else -> "$sender: $cleaned"
            }
        }
}

object ChatPlatformRules {
    private const val WECHAT_SYNTHETIC_MIN = 700_000_000_000_001L
    private const val WECHAT_SYNTHETIC_MAX = 799_999_999_999_999L
    const val GROUP_GAP_MS: Long = 5L * 60L * 1000L
    const val MAX_GROUP_SIZE: Int = 30
    const val IDENTITY_TONE_COUNT: Int = 8

    fun isPrivateConversationId(value: String?): Boolean {
        val text = value?.trim().orEmpty()
        return Regex("^-[1-9]\\d*$").matches(text)
    }

    fun isWechatSyntheticId(value: String?): Boolean {
        val clean = value?.trim().orEmpty().removePrefix("-")
        if (clean.length != 15 || !clean.all(Char::isDigit)) return false
        val num = clean.toLongOrNull() ?: return false
        return num in WECHAT_SYNTHETIC_MIN..WECHAT_SYNTHETIC_MAX
    }

    fun resolvePlatform(platform: String?, conversationId: String?, source: String): String {
        if (source == "server") return "mc"
        val explicit = platform?.trim()?.lowercase().orEmpty()
        if (explicit == "wechat" || explicit == "qq") return explicit
        return if (isWechatSyntheticId(conversationId)) "wechat" else "qq"
    }

    fun isNumericQqId(value: String?): Boolean {
        val text = value?.trim().orEmpty()
        return text.length >= 5 && text.all(Char::isDigit) && !isWechatSyntheticId(text)
    }

    /** Audit C-4: only allow absolute http(s) URLs. */
    fun safeExternalUrl(raw: String?): String? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty() || trimmed.length > 2048) return null
        val lower = trimmed.lowercase()
        return if (lower.startsWith("https://") || lower.startsWith("http://")) trimmed else null
    }

    fun parseLocateMessageId(raw: String, source: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val stripped = trimmed.removePrefix("#")
        if (Regex("^qq-\\d+$", RegexOption.IGNORE_CASE).matches(stripped)) {
            return stripped.lowercase()
        }
        if (trimmed.startsWith("#") && stripped.all(Char::isDigit) && stripped.isNotEmpty()) {
            return if (source == "qq") "qq-$stripped" else stripped
        }
        return null
    }

    /** Deterministic FNV-1a hash matching `chat.igng.net/components/chat/utils.js`. */
    fun fnv1aHash(value: String): Long {
        var hash = 0x811c9dc5L
        for (ch in value) {
            hash = hash xor ch.code.toLong()
            hash = (hash * 0x01000193L) and 0xFFFF_FFFFL
        }
        return hash
    }

    fun identityToneIndex(key: String): Int = (fnv1aHash(key) % IDENTITY_TONE_COUNT).toInt()
}

// ==================== Platform & Identity Color Tokens (Aligned with globals.css + IgngTheme) ====================

data class IdentityTonePalette(
    val avatarBg: Color,
    val avatarFg: Color,
    val nameText: Color,
)

data class ChatSemanticColors(
    val qq: Color,
    val qqSoft: Color,
    val wechat: Color,
    val wechatSoft: Color,
    val privateAccent: Color,
    val privateSoft: Color,
    val server: Color,
    val serverSoft: Color,
    val bot: Color,
    val botSoft: Color,
    val botBubbleBg: Color,
    val botBubbleBorder: Color,
    val selfBubbleBg: Color,
    val selfBubbleBorder: Color,
    val otherBubbleBg: Color,
    val otherBubbleBorder: Color,
    val serviceBubbleBg: Color,
    val personaBg: Color,
    val personaFg: Color,
    val relationshipBg: Color,
    val relationshipFg: Color,
    val proBg: Color,
    val proFg: Color,
    val plusBg: Color,
    val plusFg: Color,
    val redPacketTop: Color,
    val redPacketBottom: Color,
    val transferAccent: Color,
    val tones: List<IdentityTonePalette>,
)

fun chatSemanticColors(isDark: Boolean): ChatSemanticColors = if (isDark) {
    ChatSemanticColors(
        qq = Color(0xFF6AAEFF),
        qqSoft = Color(0xFF172A42),
        wechat = Color(0xFF5FD35F),
        wechatSoft = Color(0xFF163016),
        privateAccent = Color(0xFFB08CFF),
        privateSoft = Color(0xFF2A2140),
        server = Color(0xFF6CC77A),
        serverSoft = Color(0xFF18301D),
        bot = Color(0xFF4FD1C5),
        botSoft = Color(0xFF123331),
        botBubbleBg = Color(0xFF102B2A),
        botBubbleBorder = Color(0x474FD1C5),
        selfBubbleBg = Color(0xFF162C4E),
        selfBubbleBorder = Color(0x4D58A6FF),
        otherBubbleBg = Color(0xFF141A23),
        otherBubbleBorder = Color(0x2EFFFFFF),
        serviceBubbleBg = Color(0xFF181E28),
        personaBg = Color(0x248B5CF6),
        personaFg = Color(0xFFC4B5FD),
        relationshipBg = Color(0x24EC4899),
        relationshipFg = Color(0xFFF9A8D4),
        proBg = Color(0x2EA855F7),
        proFg = Color(0xFFD8B4FE),
        plusBg = Color(0x293B82F6),
        plusFg = Color(0xFF93C5FD),
        redPacketTop = Color(0xFF9A3412),
        redPacketBottom = Color(0xFF7C2D12),
        transferAccent = Color(0xFFD97706),
        tones = listOf(
            IdentityTonePalette(Color(0xFF1E3A5F), Color(0xFFBFDBFE), Color(0xFF93C5FD)),
            IdentityTonePalette(Color(0xFF33265E), Color(0xFFDDD6FE), Color(0xFFC4B5FD)),
            IdentityTonePalette(Color(0xFF4A1D36), Color(0xFFFBCFE8), Color(0xFFF9A8D4)),
            IdentityTonePalette(Color(0xFF4A2716), Color(0xFFFED7AA), Color(0xFFFDBA74)),
            IdentityTonePalette(Color(0xFF45320F), Color(0xFFFDE68A), Color(0xFFFCD34D)),
            IdentityTonePalette(Color(0xFF173A26), Color(0xFFBBF7D0), Color(0xFF86EFAC)),
            IdentityTonePalette(Color(0xFF123D3A), Color(0xFF99F6E4), Color(0xFF5EEAD4)),
            IdentityTonePalette(Color(0xFF272B5E), Color(0xFFC7D2FE), Color(0xFFA5B4FC)),
        ),
    )
} else {
    ChatSemanticColors(
        qq = Color(0xFF2F7DE1),
        qqSoft = Color(0xFFE3EEFC),
        wechat = Color(0xFF1AAD19),
        wechatSoft = Color(0xFFE3F7E3),
        privateAccent = Color(0xFF7C4DDB),
        privateSoft = Color(0xFFEFE8FC),
        server = Color(0xFF2F8A3E),
        serverSoft = Color(0xFFE2F3E4),
        bot = Color(0xFF0F8F84),
        botSoft = Color(0xFFDCF4F1),
        botBubbleBg = Color(0xFFECF9F7),
        botBubbleBorder = Color(0x400F8F84),
        selfBubbleBg = Color(0xFFE0EDFF),
        selfBubbleBorder = Color(0x3D2563EB),
        otherBubbleBg = Color(0xFFFFFFFF),
        otherBubbleBorder = Color(0x1C0F172A),
        serviceBubbleBg = Color(0xFFF2F5FA),
        personaBg = Color(0x1F8B5CF6),
        personaFg = Color(0xFF7C3AED),
        relationshipBg = Color(0x1FEC4899),
        relationshipFg = Color(0xFFDB2777),
        proBg = Color(0x24A855F7),
        proFg = Color(0xFF9333EA),
        plusBg = Color(0x1F3B82F6),
        plusFg = Color(0xFF2563EB),
        redPacketTop = Color(0xFFEA580C),
        redPacketBottom = Color(0xFFC2410C),
        transferAccent = Color(0xFFD97706),
        tones = listOf(
            IdentityTonePalette(Color(0xFFDBEAFE), Color(0xFF1E40AF), Color(0xFF1D4ED8)),
            IdentityTonePalette(Color(0xFFEDE9FE), Color(0xFF5B21B6), Color(0xFF6D28D9)),
            IdentityTonePalette(Color(0xFFFCE7F3), Color(0xFF9D174D), Color(0xFFBE185D)),
            IdentityTonePalette(Color(0xFFFFEDD5), Color(0xFF9A3412), Color(0xFFC2410C)),
            IdentityTonePalette(Color(0xFFFEF3C7), Color(0xFF92400E), Color(0xFFA16207)),
            IdentityTonePalette(Color(0xFFDCFCE7), Color(0xFF166534), Color(0xFF15803D)),
            IdentityTonePalette(Color(0xFFCCFBF1), Color(0xFF115E59), Color(0xFF0F766E)),
            IdentityTonePalette(Color(0xFFE0E7FF), Color(0xFF3730A3), Color(0xFF4338CA)),
        ),
    )
}

// ==================== Unified Session List Builder ====================

fun buildUnifiedSessions(sources: ChatSourcesResponse): List<ChatSessionItem> {
    val result = mutableListOf<ChatSessionItem>()
    val seenImIds = HashSet<String>()

    fun addImGroup(group: ChatGroupSourceDto, forcePrivate: Boolean = false) {
        if (group.id.isBlank() || !seenImIds.add(group.id)) return
        val isPriv = forcePrivate || group.isPrivate || ChatPlatformRules.isPrivateConversationId(group.id)
        val platform = ChatPlatformRules.resolvePlatform(group.platform, group.id, "qq")
        val kind = when {
            platform == "wechat" && isPriv -> ChatSessionKind.WECHAT_PRIVATE
            platform == "wechat" -> ChatSessionKind.WECHAT_GROUP
            isPriv -> ChatSessionKind.QQ_PRIVATE
            else -> ChatSessionKind.QQ_GROUP
        }
        val lastAt = group.lastMessage?.createdAt ?: group.lastMessageAt
        val rawPreview = group.lastMessage?.content?.takeIf { it.isNotBlank() } ?: group.lastPreview
        val preview = rawPreview?.let(::humanizeMediaPlaceholders)
        val normalizedGroupAvatar = normalizeChatAvatarUrl(group.avatarUrl)
        val defaultQqAvatar = when {
            normalizedGroupAvatar != null -> null
            kind == ChatSessionKind.QQ_PRIVATE && sources.authenticated -> {
                val rawQq = group.id.removePrefix("-")
                if (ChatPlatformRules.isNumericQqId(rawQq)) "https://q1.qlogo.cn/g?b=qq&nk=$rawQq&s=100" else null
            }
            kind == ChatSessionKind.QQ_GROUP && ChatPlatformRules.isNumericQqId(group.id) -> {
                "https://p.qlogo.cn/gh/${group.id}/${group.id}/100"
            }
            else -> null
        }
        result += ChatSessionItem(
            key = "qq:${group.id}",
            apiSource = "qq",
            targetId = group.id,
            serverNumericId = null,
            platformKey = platform,
            kind = kind,
            isPrivate = isPriv,
            name = group.name.ifBlank { "${kind.compactLabel} ${group.id.takeLast(4)}" },
            avatarUrl = normalizedGroupAvatar ?: defaultQqAvatar,
            isAdminGroup = group.isAdminGroup,
            isTechGroup = group.isTechGroup,
            messageCount = group.messageCount,
            lastMessageAt = lastAt,
            lastTimestampMs = parseIsoEpochMillis(lastAt),
            previewSender = group.lastMessage?.sender?.takeIf { it.isNotBlank() },
            previewContent = preview,
            previewHasAtAll = preview?.let {
                it.contains("@全体成员") || it.contains("@所有人") || it.contains("@全体")
            } == true,
            persona = group.persona,
        )
    }

    sources.qqGroups.forEach { addImGroup(it, forcePrivate = false) }
    sources.privateChats.forEach { addImGroup(it, forcePrivate = true) }

    sources.servers.forEach { srv: ChatServerSourceDto ->
        val lastAt = srv.lastMessage?.createdAt ?: srv.lastMessageAt
        val preview = srv.lastMessage?.content?.takeIf { it.isNotBlank() } ?: srv.lastPreview
        result += ChatSessionItem(
            key = "server:${srv.id}",
            apiSource = "server",
            targetId = srv.id.toString(),
            serverNumericId = srv.id,
            platformKey = "mc",
            kind = ChatSessionKind.MC_SERVER,
            isPrivate = false,
            name = srv.name.ifBlank { "MC 服务器 #${srv.id}" },
            avatarUrl = null,
            isAdminGroup = false,
            isTechGroup = false,
            messageCount = 0,
            lastMessageAt = lastAt,
            lastTimestampMs = parseIsoEpochMillis(lastAt),
            previewSender = srv.lastMessage?.sender?.takeIf { it.isNotBlank() },
            previewContent = preview,
            previewHasAtAll = false,
            persona = null,
        )
    }

    return result.sortedWith(
        compareByDescending<ChatSessionItem> { it.lastTimestampMs }
            .thenBy { it.kind.ordinal }
            .thenBy { it.name.lowercase() },
    )
}

fun filterSessions(
    sessions: List<ChatSessionItem>,
    tab: ChatSessionTab,
    query: String,
): List<ChatSessionItem> {
    val q = query.trim().lowercase()
    return sessions.filter { item ->
        val tabOk = when (tab) {
            ChatSessionTab.ALL -> true
            ChatSessionTab.QQ -> item.kind == ChatSessionKind.QQ_GROUP || item.kind == ChatSessionKind.QQ_PRIVATE
            ChatSessionTab.WECHAT -> item.kind == ChatSessionKind.WECHAT_GROUP || item.kind == ChatSessionKind.WECHAT_PRIVATE
            ChatSessionTab.MC -> item.kind == ChatSessionKind.MC_SERVER
            ChatSessionTab.PRIVATE -> item.isPrivate
        }
        if (!tabOk) return@filter false
        if (q.isEmpty()) return@filter true
        item.name.lowercase().contains(q) ||
            (item.displayNumber?.contains(q) == true) ||
            (item.previewContent?.lowercase()?.contains(q) == true) ||
            (item.previewSender?.lowercase()?.contains(q) == true)
    }
}

// ==================== Sender Resolution & Privacy-Safe Avatars ====================

private val HTTP_QLOGO_REGEX = Regex("^http://([a-z0-9-]+\\.qlogo\\.cn/)", RegexOption.IGNORE_CASE)
private val MC_HEADS_REGEX = Regex("^https?://mc-heads\\.net/avatar/([^/?#]+)(?:/(\\d+))?$", RegexOption.IGNORE_CASE)

fun normalizeChatAvatarUrl(raw: String?): String? {
    val trimmed = raw?.trim().orEmpty()
    if (trimmed.isEmpty()) return null
    HTTP_QLOGO_REGEX.find(trimmed)?.let {
        return "https://" + trimmed.substring(7)
    }
    MC_HEADS_REGEX.matchEntire(trimmed)?.let { match ->
        val name = match.groupValues[1]
        val size = match.groupValues[2].ifBlank { "64" }
        return "/api/v1/mc/avatars/$name?size=$size"
    }
    return trimmed
}

data class ResolvedChatSender(
    val groupKey: String,
    val displayName: String,
    val secondaryName: String?,
    val igngUsername: String?,
    val avatarUrl: String?,
    val fallbackAvatarUrls: List<String> = emptyList(),
    val initials: String,
    val toneIndex: Int,
    val isBot: Boolean,
    val isAccountSelf: Boolean,
    val isService: Boolean,
    val sourceLabel: String?,
    val isMine: Boolean,
    val isMc: Boolean,
    val boundIgngId: Int?,
    val mcUsername: String?,
    val badges: List<ChatSenderBadgeDto>,
    val relationship: ChatRelationshipDto?,
    val filterSenderId: String?,
)

fun senderGroupKeyOf(message: ChatMessageDto): String {
    if (message.source == "qq") {
        val platform = ChatPlatformRules.resolvePlatform(message.platform, message.groupId, "qq")
        return if (message.isSelf || message.isBot) {
            "$platform:self:${message.logicalSpeaker ?: if (message.isBot) "agent" else "unknown"}:${message.messageSource ?: "onebot_event"}"
        } else {
            "$platform:${message.senderId ?: message.senderName}"
        }
    }
    return "player:${message.senderName.trim().lowercase()}"
}

fun resolveChatSender(
    message: ChatMessageDto,
    profiles: Map<String, ChatSenderProfileDto>,
    activeAccount: SavedAccount?,
    boundMcUsernames: Set<String> = emptySet(),
): ResolvedChatSender {
    val isIm = message.source == "qq"
    val platform = ChatPlatformRules.resolvePlatform(message.platform, message.groupId, message.source)
    val isWechat = isIm && platform == "wechat"
    val isQq = isIm && !isWechat
    val isAccountSelf = isIm && (message.isSelf || message.isBot)
    val logicalSpeaker = message.logicalSpeaker ?: if (message.isBot) "agent" else if (isAccountSelf) "unknown" else "person"
    val isBot = isAccountSelf && logicalSpeaker == "agent"
    val isService = isAccountSelf && logicalSpeaker == "service"

    val rawId = if (isIm) message.senderId.orEmpty() else message.senderName
    val profileKey = if (isIm) "$platform:$rawId" else "player:${rawId.trim().lowercase()}"
    val profile = profiles[profileKey]
    val boundIgngId = if (!isAccountSelf && profile?.bound == true) profile.igngId else null

    val snapshotName = if (isIm && !isBot) message.senderName.trim() else ""
    val profileNickname = profile?.igngNickname
    val displayName: String
    var secondaryName: String? = null
    when {
        isBot -> displayName = "云萤"
        isAccountSelf -> displayName = if (isService) "云萤程序" else "云萤账号"
        !isIm -> displayName = rawId.ifBlank { "玩家" }
        boundIgngId != null && !profileNickname.isNullOrBlank() -> {
            displayName = profileNickname
            if (snapshotName.isNotBlank() && snapshotName != displayName && !snapshotName.startsWith("QQ ")) {
                secondaryName = snapshotName
            }
        }
        snapshotName.isNotBlank() -> displayName = snapshotName
        isWechat -> displayName = "微信用户"
        else -> displayName = if (rawId.isNotBlank()) "QQ $rawId" else "QQ 用户"
    }

    // Audit C-7: never query qlogo with masked QQ IDs (containing '*'), and MC always uses skin heads
    // routed through the unified v1 gateway proxy first with multi-mirror fallback.
    val profileAvatar = normalizeChatAvatarUrl(profile?.avatarUrl)
    val msgAvatar = normalizeChatAvatarUrl(message.senderAvatar)?.takeUnless { it.contains('*') }
    val avatarCandidates: List<String> = when {
        isAccountSelf -> emptyList()
        !isIm -> rawId.takeIf { it.isNotBlank() }?.let { name ->
            val encoded = java.net.URLEncoder.encode(name, "UTF-8").replace("+", "%20")
            buildList {
                if (profileAvatar != null && profileAvatar.startsWith("/api/v1/mc/avatars/")) {
                    add(profileAvatar)
                }
                add("/api/v1/mc/avatars/$encoded?size=64")
                add("https://minotar.net/helm/$encoded/64.png")
                add("https://mc-heads.net/avatar/$encoded/64")
                add("https://vzge.me/face/64/$encoded")
            }.distinct()
        }.orEmpty()
        else -> buildList {
            profileAvatar?.let { add(it) }
            msgAvatar?.let { add(it) }
            if (isQq && ChatPlatformRules.isNumericQqId(rawId)) {
                add("https://q1.qlogo.cn/g?b=qq&nk=$rawId&s=100")
            }
        }.distinct()
    }
    val avatarUrl: String? = avatarCandidates.firstOrNull()
    val fallbackAvatarUrls: List<String> = avatarCandidates.drop(1)

    val isMine = when {
        isAccountSelf -> false
        activeAccount == null -> false
        boundIgngId != null && boundIgngId == activeAccount.userId -> true
        !isIm && rawId.isNotBlank() && boundMcUsernames.any { it.equals(rawId, ignoreCase = true) } -> true
        else -> false
    }

    val digits = rawId.filter(Char::isDigit)
    val initials = when {
        isBot -> ""
        isAccountSelf -> if (isService) "程" else "账"
        !isIm -> displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        isWechat || boundIgngId != null || (snapshotName.isNotBlank() && !snapshotName.startsWith("QQ ")) ->
            displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "微"
        else -> digits.takeLast(2).ifBlank { "QQ" }
    }

    val badges = buildList {
        profile?.badges?.let { addAll(it) }
        if (!isIm && profile?.badges.isNullOrEmpty() && !profile?.lpGroups.isNullOrEmpty()) {
            profile!!.lpGroups.filter { it.isNotBlank() && it != "default" }.forEach { grp ->
                add(ChatSenderBadgeDto(key = "lp-$grp", label = grp, tone = "lp"))
            }
        }
    }

    val filterSenderId = when {
        isAccountSelf -> null
        !isIm -> rawId.takeIf { it.isNotBlank() }
        !rawId.contains('*') && rawId.isNotBlank() -> rawId
        else -> null
    }

    return ResolvedChatSender(
        groupKey = senderGroupKeyOf(message),
        displayName = displayName,
        secondaryName = secondaryName,
        igngUsername = if (boundIgngId != null) profile?.igngUsername?.takeIf { it.isNotBlank() && it != displayName } else null,
        avatarUrl = avatarUrl,
        fallbackAvatarUrls = fallbackAvatarUrls,
        initials = initials,
        toneIndex = ChatPlatformRules.identityToneIndex(profileKey),
        isBot = isBot,
        isAccountSelf = isAccountSelf,
        isService = isService,
        sourceLabel = if (isAccountSelf) message.messageSourceLabel ?: "来源未确认" else null,
        isMine = isMine,
        isMc = !isIm,
        boundIgngId = boundIgngId,
        mcUsername = if (!isIm) rawId.takeIf { it.isNotBlank() } else null,
        badges = badges,
        relationship = profile?.relationship,
        filterSenderId = filterSenderId,
    )
}

/** Sorts badges by priority for compact mobile header (max 2 inline). */
fun prioritizedInlineBadges(badges: List<ChatSenderBadgeDto>, limit: Int = 2): List<ChatSenderBadgeDto> {
    if (badges.size <= limit) return badges
    fun rank(badge: ChatSenderBadgeDto): Int = when (badge.key) {
        "superadmin" -> 0
        "techadmin" -> 1
        "mcadmin" -> 2
        "pro" -> 3
        "plus" -> 4
        else -> if (badge.tone == "lp") 5 else 6
    }
    return badges.sortedBy(::rank).take(limit)
}

// ==================== Content, Attachment & Placeholder Extraction ====================

private val IMAGE_ATTACHMENT_KINDS = setOf("image", "mface", "face", "sticker", "emoji", "photo", "gif", "webp")
private val PLAYABLE_AUDIO_KINDS = setOf("audio", "record", "voice")
private val PLAYABLE_VIDEO_KINDS = setOf("video", "shortvideo")
private val IMAGE_PLACEHOLDER_REGEX = Regex(
    "[\\[【](?:(?:图片|动画表情|动图|表情|贴纸)(?:(?:\\s*[:：]|\\s+md5\\s*=)[^\\]】\\n]*)?|(?:image|img|mface|face|sticker|emoji|photo)(?:\\s*[:：=][^\\]】\\n]*)?)[\\]】]",
    RegexOption.IGNORE_CASE,
)
private val VIDEO_PLACEHOLDER_REGEX = Regex(
    "[\\[【](?:视频|小视频|video|shortvideo)(?:\\s*[:：=][^\\]】\\n]*)?[\\]】]",
    RegexOption.IGNORE_CASE,
)
private val AUDIO_PLACEHOLDER_REGEX = Regex(
    "[\\[【](?:语音|音频|audio|record|voice)(?:\\s*[:：=][^\\]】\\n]*)?[\\]】]",
    RegexOption.IGNORE_CASE,
)
private val FILE_PLACEHOLDER_REGEX = Regex("\\[(?:文件|file)(?:[:：]\\s*([^\\]\\n]*))?\\]", RegexOption.IGNORE_CASE)
private val STRUCTURE_PREFIX_CHATLOG = Regex("^\\[聊天记录\\]\\s*")
private val STRUCTURE_PREFIX_FORWARD = Regex("^\\[转发消息\\]\\s*")
private val STRUCTURE_PREFIX_CARD = Regex("^\\[(?:卡片|小程序)(?::[^\\n]*)?\\][^\\n]*")
private val STRUCTURE_PREFIX_LINK = Regex("^\\[链接\\]\\s*")
private val HASH_FILENAME_REGEX = Regex(
    "^(?:-?\\d+_[0-9a-fA-F]{16,}(?:_[0-9a-fA-F]{8,})?|[0-9a-fA-F]{24,}|[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}|(?:face|mface|img|image|pic|photo|video|audio|voice|record)_[-0-9a-fA-F]+)(?:\\.[A-Za-z0-9]{1,6})?$",
    RegexOption.IGNORE_CASE,
)

fun isImageAttachment(att: ChatAttachmentDto): Boolean =
    att.kind.lowercase() in IMAGE_ATTACHMENT_KINDS ||
        att.mimeType?.startsWith("image/", ignoreCase = true) == true

fun isAudioAttachment(att: ChatAttachmentDto): Boolean =
    att.kind.lowercase() in PLAYABLE_AUDIO_KINDS ||
        att.mimeType?.startsWith("audio/", ignoreCase = true) == true

fun isVideoAttachment(att: ChatAttachmentDto): Boolean =
    att.kind.lowercase() in PLAYABLE_VIDEO_KINDS ||
        att.mimeType?.startsWith("video/", ignoreCase = true) == true

fun isFileAttachment(att: ChatAttachmentDto): Boolean =
    att.kind.lowercase() == "file" &&
        !isImageAttachment(att) &&
        !isAudioAttachment(att) &&
        !isVideoAttachment(att)

/**
 * Replaces raw bot/protocol English resource tags (e.g. `[图片 md5=...]`, `[image]`, `[mface]`,
 * `[video]`, `[record]`) with clean Chinese labels when media attachments are absent.
 */
fun humanizeMediaPlaceholders(text: String): String {
    if (text.isBlank()) return text
    var result = IMAGE_PLACEHOLDER_REGEX.replace(text) { m ->
        val raw = m.value.lowercase()
        when {
            raw.contains("mface") || raw.contains("sticker") || raw.contains("贴纸") -> "[贴纸]"
            raw.contains("face") || raw.contains("emoji") || raw.contains("表情") -> "[表情]"
            raw.contains("动图") -> "[动图]"
            else -> "[图片]"
        }
    }
    result = VIDEO_PLACEHOLDER_REGEX.replace(result, "[视频]")
    result = AUDIO_PLACEHOLDER_REGEX.replace(result, "[语音]")
    return result
}

/**
 * Filters machine-generated hash filenames (e.g. `-2105710147_C56BF959...webp`, `face_277.webp`)
 * into friendly Chinese media titles while preserving meaningful human filenames.
 */
fun friendlyMediaDisplayName(rawName: String?, fallback: String): String {
    val clean = rawName?.trim().orEmpty()
    if (clean.isEmpty()) return fallback
    if (HASH_FILENAME_REGEX.matches(clean)) return fallback
    return clean
}

fun isForwardPart(part: ChatStructurePartDto): Boolean = part.type == "forward" || part.type == "node"
fun isCardPart(part: ChatStructurePartDto): Boolean = part.type == "card"

data class ChatFileEntry(
    val name: String,
    val url: String?,
    val sizeBytes: Long?,
    val mimeType: String?,
    val index: Int,
)

data class ParsedMessageDisplay(
    val text: String,
    val images: List<ChatAttachmentDto>,
    val audios: List<ChatAttachmentDto>,
    val videos: List<ChatAttachmentDto>,
    val files: List<ChatFileEntry>,
    val cards: List<ChatStructurePartDto>,
    val forwards: List<ChatStructurePartDto>,
    val missingImages: Int,
    val isPatNotice: Boolean,
    val patNoticeText: String?,
) {
    val hasVisualBubbleContent: Boolean
        get() = text.isNotBlank() ||
            audios.isNotEmpty() ||
            videos.isNotEmpty() ||
            files.isNotEmpty() ||
            cards.isNotEmpty() ||
            forwards.isNotEmpty() ||
            missingImages > 0

    val isPureImages: Boolean
        get() = images.isNotEmpty() && !hasVisualBubbleContent
}

fun extractMessageDisplay(message: ChatMessageDto): ParsedMessageDisplay {
    val structure = message.messageStructure
    val patCard = structure.firstOrNull { isCardPart(it) && it.cardType == "pat" }
    val cards = structure.filter { isCardPart(it) && it.cardType != "quote" && it.cardType != "pat" }
    val forwards = structure.filter(::isForwardPart)
    val hasSpecialParts = cards.isNotEmpty() || forwards.isNotEmpty() || structure.any { isCardPart(it) && it.cardType == "quote" }

    val baseContent = if (hasSpecialParts) {
        message.content
            .replace(STRUCTURE_PREFIX_CHATLOG, "")
            .replace(STRUCTURE_PREFIX_FORWARD, "")
            .replace(STRUCTURE_PREFIX_CARD, "")
            .replace(STRUCTURE_PREFIX_LINK, "")
            .trim()
    } else {
        message.content
    }

    // Extract files
    val availableFiles = message.attachments
        .filter(::isFileAttachment)
        .map { att ->
            ChatFileEntry(
                name = att.name?.trim().orEmpty(),
                url = att.url?.takeIf { it.isNotBlank() },
                sizeBytes = att.sizeBytes,
                mimeType = att.mimeType,
                index = att.index,
            )
        }
        .toMutableList()
    val unmatchedNames = mutableListOf<String>()
    var textWithoutFiles = FILE_PLACEHOLDER_REGEX.replace(baseContent) { match ->
        unmatchedNames += match.groupValues.getOrElse(1) { "" }.trim()
        ""
    }
    if (unmatchedNames.isNotEmpty()) {
        val consumed = HashSet<Int>()
        for (name in unmatchedNames) {
            val idx = availableFiles.indexOfFirst { entry ->
                !consumed.contains(entry.index) && !entry.url.isNullOrBlank() && entry.name == name
            }
            if (idx >= 0) {
                consumed += availableFiles[idx].index
            } else {
                availableFiles += ChatFileEntry(
                    name = name.ifBlank { "未命名文件" },
                    url = null,
                    sizeBytes = null,
                    mimeType = null,
                    index = -1,
                )
            }
        }
        textWithoutFiles = textWithoutFiles
            .replace(Regex("[ \\t]{2,}"), " ")
            .replace(Regex("[ \\t]+\\n"), "\n")
            .trim()
    }

    val images = message.attachments.filter(::isImageAttachment)
    val audios = message.attachments.filter(::isAudioAttachment)
    val videos = message.attachments.filter(::isVideoAttachment)

    var cleanedMediaText = textWithoutFiles
    if (videos.isNotEmpty()) {
        cleanedMediaText = VIDEO_PLACEHOLDER_REGEX.replace(cleanedMediaText, "")
    }
    if (audios.isNotEmpty()) {
        cleanedMediaText = AUDIO_PLACEHOLDER_REGEX.replace(cleanedMediaText, "")
    }
    cleanedMediaText = cleanedMediaText
        .replace(Regex("[ \\t]{2,}"), " ")
        .replace(Regex("[ \\t]+\\n"), "\n")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()

    var imagePlaceholderCount = 0
    val strippedImagesText = IMAGE_PLACEHOLDER_REGEX.replace(cleanedMediaText) {
        imagePlaceholderCount += 1
        ""
    }.replace(Regex("[ \\t]{2,}"), " ")
        .replace(Regex("[ \\t]+\\n"), "\n")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()

    val isWechat = ChatPlatformRules.resolvePlatform(message.platform, message.groupId, message.source) == "wechat"
    val rawMissing = (imagePlaceholderCount - images.size).coerceAtLeast(0)
    // Match chat.igng.net: keep readable `[图片]` placeholder text for WeChat when media isn't extracted yet,
    // while normalizing raw English/MD5 tokens so internal syntax never leaks into bubbles.
    val finalText: String
    val missingImages: Int
    if (forwards.isNotEmpty()) {
        finalText = ""
        missingImages = 0
    } else if (isWechat && rawMissing > 0 && images.isEmpty()) {
        finalText = humanizeMediaPlaceholders(cleanedMediaText)
        missingImages = 0
    } else {
        finalText = humanizeMediaPlaceholders(
            if (imagePlaceholderCount > 0) strippedImagesText else cleanedMediaText,
        )
        missingImages = rawMissing
    }

    val patText = patCard?.let {
        listOf(it.title, it.description, it.summary).firstOrNull { s -> s.isNotBlank() }
            ?: finalText.takeIf { s -> s.isNotBlank() }
    }
    val isPatOnly = patCard != null && images.isEmpty() && audios.isEmpty() && videos.isEmpty() &&
        availableFiles.isEmpty() && cards.isEmpty() && forwards.isEmpty()

    return ParsedMessageDisplay(
        text = finalText,
        images = images,
        audios = audios,
        videos = videos,
        files = availableFiles,
        cards = cards,
        forwards = forwards,
        missingImages = missingImages,
        isPatNotice = isPatOnly,
        patNoticeText = patText,
    )
}

// ==================== Timeline Grouping & Merging ====================

sealed interface ChatTimelineItem {
    val key: String

    data class DayHeader(override val key: String, val label: String) : ChatTimelineItem
    data class TimeDivider(override val key: String, val label: String) : ChatTimelineItem
    data class SystemNotice(
        override val key: String,
        val message: ChatMessageDto,
        val text: String,
    ) : ChatTimelineItem

    data class MessageGroup(
        override val key: String,
        val sender: ResolvedChatSender,
        val messages: List<ChatMessageDto>,
    ) : ChatTimelineItem
}

fun buildChatTimeline(
    messages: List<ChatMessageDto>,
    profiles: Map<String, ChatSenderProfileDto>,
    activeAccount: SavedAccount?,
    boundMcUsernames: Set<String> = emptySet(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): List<ChatTimelineItem> {
    if (messages.isEmpty()) return emptyList()
    val items = mutableListOf<ChatTimelineItem>()
    var lastDayKey: String? = null
    var lastTimestampMs: Long = 0L
    var currentSender: ResolvedChatSender? = null
    var currentBucket: MutableList<ChatMessageDto>? = null

    fun flushGroup() {
        val sender = currentSender
        val bucket = currentBucket
        if (sender != null && !bucket.isNullOrEmpty()) {
            items += ChatTimelineItem.MessageGroup(
                key = "group:${bucket.first().id}",
                sender = sender,
                messages = bucket.toList(),
            )
        }
        currentSender = null
        currentBucket = null
    }

    for (msg in messages) {
        val dayKey = formatDayKey(msg.createdAt, zoneId)
        val ts = parseIsoEpochMillis(msg.createdAt)
        if (dayKey != lastDayKey) {
            flushGroup()
            items += ChatTimelineItem.DayHeader(
                key = "day:$dayKey:${msg.id}",
                label = formatDayHeaderLabel(msg.createdAt, zoneId),
            )
            lastDayKey = dayKey
            lastTimestampMs = ts
        } else if (ts > 0L && lastTimestampMs > 0L && (ts - lastTimestampMs) >= ChatPlatformRules.GROUP_GAP_MS) {
            flushGroup()
            items += ChatTimelineItem.TimeDivider(
                key = "time:${msg.id}",
                label = formatMessageClock(msg.createdAt, zoneId),
            )
        }

        val parsed = extractMessageDisplay(msg)
        if (parsed.isPatNotice && !parsed.patNoticeText.isNullOrBlank()) {
            flushGroup()
            items += ChatTimelineItem.SystemNotice(
                key = "sys:${msg.id}",
                message = msg,
                text = parsed.patNoticeText,
            )
            lastTimestampMs = ts
            continue
        }

        val sender = resolveChatSender(msg, profiles, activeAccount, boundMcUsernames)
        val canJoin = currentSender != null &&
            currentBucket != null &&
            currentSender!!.groupKey == sender.groupKey &&
            currentSender!!.isMine == sender.isMine &&
            currentBucket!!.size < ChatPlatformRules.MAX_GROUP_SIZE &&
            (ts == 0L || lastTimestampMs == 0L || (ts - lastTimestampMs) < ChatPlatformRules.GROUP_GAP_MS)

        if (canJoin) {
            currentBucket!!.add(msg)
        } else {
            flushGroup()
            currentSender = sender
            currentBucket = mutableListOf(msg)
        }
        if (ts > 0L) lastTimestampMs = ts
    }
    flushGroup()
    return items
}

fun mergeChatMessages(
    current: List<ChatMessageDto>,
    incoming: List<ChatMessageDto>,
    pruneWindow: Boolean = false,
): List<ChatMessageDto> {
    val comparator = Comparator<ChatMessageDto> { a, b ->
        val ta = parseIsoEpochMillis(a.createdAt)
        val tb = parseIsoEpochMillis(b.createdAt)
        if (ta != tb && ta > 0L && tb > 0L) return@Comparator ta.compareTo(tb)
        compareMessageIds(a.id, b.id)
    }
    val base = if (pruneWindow && incoming.isNotEmpty()) {
        val sortedIncoming = incoming.sortedWith(comparator)
        val windowStart = sortedIncoming.first()
        val incomingIds = incoming.mapTo(HashSet()) { it.id }
        current.filter { msg -> incomingIds.contains(msg.id) || comparator.compare(msg, windowStart) < 0 }
    } else {
        current
    }
    val byId = LinkedHashMap<String, ChatMessageDto>(base.size + incoming.size)
    base.forEach { byId[it.id] = it }
    incoming.forEach { byId[it.id] = it }
    return byId.values.sortedWith(comparator)
}

private fun compareMessageIds(a: String, b: String): Int {
    val numA = a.removePrefix("qq-").toLongOrNull()
    val numB = b.removePrefix("qq-").toLongOrNull()
    return if (numA != null && numB != null) numA.compareTo(numB) else a.compareTo(b)
}

// ==================== Date & Time Formatters ====================

private val CLOCK_FMT = DateTimeFormatter.ofPattern("HH:mm")
private val FULL_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
private val DAY_KEY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd")

fun parseIsoEpochMillis(iso: String?): Long {
    if (iso.isNullOrBlank()) return 0L
    return runCatching { Instant.parse(iso).toEpochMilli() }
        .recoverCatching {
            val normalized = iso.trim().replace(' ', 'T').let { if (it.endsWith("Z")) it else "${it}Z" }
            Instant.parse(normalized).toEpochMilli()
        }
        .getOrDefault(0L)
}

private fun toZonedDateTime(iso: String?, zoneId: ZoneId): LocalDateTime? {
    val ms = parseIsoEpochMillis(iso)
    if (ms <= 0L) return null
    return Instant.ofEpochMilli(ms).atZone(zoneId).toLocalDateTime()
}

fun formatSessionTime(iso: String?, zoneId: ZoneId = ZoneId.systemDefault(), nowDate: LocalDate = LocalDate.now(zoneId)): String {
    val dt = toZonedDateTime(iso, zoneId) ?: return ""
    val date = dt.toLocalDate()
    return when {
        date == nowDate -> dt.format(CLOCK_FMT)
        date == nowDate.minusDays(1) -> "昨天"
        date.year == nowDate.year -> "${date.monthValue}/${date.dayOfMonth}"
        else -> "${date.year}/${date.monthValue}"
    }
}

fun formatMessageClock(iso: String?, zoneId: ZoneId = ZoneId.systemDefault()): String =
    toZonedDateTime(iso, zoneId)?.format(CLOCK_FMT) ?: "--:--"

fun formatFullDateTime(iso: String?, zoneId: ZoneId = ZoneId.systemDefault()): String =
    toZonedDateTime(iso, zoneId)?.format(FULL_FMT) ?: "时间未知"

fun formatDayKey(iso: String?, zoneId: ZoneId = ZoneId.systemDefault()): String =
    toZonedDateTime(iso, zoneId)?.toLocalDate()?.format(DAY_KEY_FMT) ?: "unknown"

fun formatDayHeaderLabel(iso: String?, zoneId: ZoneId = ZoneId.systemDefault(), today: LocalDate = LocalDate.now(zoneId)): String {
    val date = toZonedDateTime(iso, zoneId)?.toLocalDate() ?: return "时间未知"
    val weekday = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINA)
    val base = if (date.year == today.year) {
        "${date.monthValue}月${date.dayOfMonth}日 $weekday"
    } else {
        "${date.year}年${date.monthValue}月${date.dayOfMonth}日 $weekday"
    }
    return when (date) {
        today -> "今天 · $base"
        today.minusDays(1) -> "昨天 · $base"
        else -> base
    }
}

fun dayKeyToUtcRange(dayKey: String, zoneId: ZoneId = ZoneId.systemDefault()): Pair<String, String>? {
    val date = runCatching { LocalDate.parse(dayKey, DAY_KEY_FMT) }.getOrNull() ?: return null
    val start = date.atStartOfDay(zoneId).toInstant().toString()
    val end = date.plusDays(1).atStartOfDay(zoneId).toInstant().minusMillis(1).toString()
    return start to end
}

fun formatShortDayLabel(dayKey: String, today: LocalDate = LocalDate.now()): String {
    val date = runCatching { LocalDate.parse(dayKey, DAY_KEY_FMT) }.getOrNull() ?: return dayKey
    return if (date.year == today.year) {
        "${date.monthValue}月${date.dayOfMonth}日"
    } else {
        "${date.year}年${date.monthValue}月${date.dayOfMonth}日"
    }
}

fun formatFileSize(bytes: Long?): String {
    if (bytes == null || bytes <= 0L) return ""
    return when {
        bytes < 1024L -> "$bytes B"
        bytes < 1024L * 1024L -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
        bytes < 1024L * 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}
