package net.igng.mcstatus.core.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// ==================== 1. Explore / Servers / Search / Profile / Hall / Perfect ====================

@Serializable
data class HomeServersResponse(
    val families: List<ServerFamilyDto> = emptyList(),
)

@Serializable
data class ServerFamilyDto(
    val slug: String = "",
    val name: String = "",
    val label: String = "",
    val themeKey: String = "clear",
    val backgroundKey: String = "clear",
    val tagline: String = "",
    val description: String = "",
    val kind: String = "seasonal",
    val sortOrder: Int = 0,
    val primarySeason: ServerSeasonDto? = null,
    val aliveSeasons: List<ServerSeasonDto> = emptyList(),
    val seasons: List<ServerSeasonDto> = emptyList(),
    val historicalSeasons: List<ServerSeasonDto> = emptyList(),
) {
    val allKnownSeasons: List<ServerSeasonDto>
        get() = (aliveSeasons + historicalSeasons + seasons + listOfNotNull(primarySeason))
            .distinctBy { it.slug }
}

@Serializable
data class ServerSeasonDto(
    val slug: String = "",
    val familySlug: String = "",
    val familyName: String = "",
    val label: String = "",
    val ordinal: Int? = null,
    val state: String = "live",
    val kind: String = "mainline",
    val primary: Boolean = false,
    val startedAt: String? = null,
    val endedAt: String? = null,
    val mapRelayKey: String? = null,
    val mapMode: String = "none",
    val modpack: String? = null,
    val serverId: Int? = null,
    val address: String? = null,
    val version: String? = null,
    val core: String? = null,
    val platform: String? = null,
    val seed: String? = null,
    val status: String? = null,
    val notice: String? = null,
    val badge: String? = null,
    val description: String = "",
    val cardDescription: String = "",
    val backgroundKey: String = "clear",
    val highlights: List<String> = emptyList(),
    val rules: List<String> = emptyList(),
    val plugins: List<String> = emptyList(),
    val worlds: List<String> = emptyList(),
    val command: String? = null,
    val cpuModel: String? = null,
    val cpuSpec: String? = null,
    val memorySpec: String? = null,
    val diskSpec: String? = null,
    val hasMap: Boolean = false,
)

@Serializable
data class MotdResponse(
    val online: Boolean? = null,
    val motd: String? = null,
    val ping: Double? = null,
    val onlinePlayers: Int? = null,
    val maxPlayers: Int? = null,
    val version: String? = null,
)

@Serializable
data class GlobalSearchResponse(
    val found: Boolean = false,
    val results: List<SearchResultItem> = emptyList(),
)

@Serializable
data class SearchResultItem(
    val kind: String = "igng",
    val mcUsername: String = "",
    val igngId: Int? = null,
    val igngName: String? = null,
    val hasBoundMc: Boolean = false,
    val href: String = "",
    val summary: SearchStatsSummary? = null,
)

@Serializable
data class SearchStatsSummary(
    val playTimeSeconds: Long = 0,
    val blocksMined: Long = 0,
    val mobKills: Long = 0,
)

@Serializable
data class PublicPlayerProfileResponse(
    val user: PublicProfileUser? = null,
    val accounts: List<String> = emptyList(),
    val mcAccounts: List<String> = emptyList(),
    val lands: List<PublicProfileLand> = emptyList(),
    val fakePlayers: List<PublicProfileFakePlayer> = emptyList(),
) {
    val boundCharacters: List<String> get() = (accounts + mcAccounts).distinct()
}

@Serializable
data class PublicProfileUser(
    val id: Int = 0,
    val username: String = "",
    val nickname: String? = null,
    val avatar_url: String? = null,
) {
    val displayName: String get() = nickname?.takeIf { it.isNotBlank() } ?: username
}

@Serializable
data class PublicProfileLand(
    val land_name: String = "",
    val owner_name: String = "",
    val description: String? = null,
)

@Serializable
data class PublicProfileFakePlayer(
    val fake_name: String = "",
    val server_name: String? = null,
    val creator_name: String? = null,
    val world: String? = null,
    val x: Double? = null,
    val y: Double? = null,
    val z: Double? = null,
    val health: Double? = null,
    val hunger: Double? = null,
)

@Serializable
data class McCharacterProfileResponse(
    val username: String = "",
    val registeredAt: String? = null,
    val lastLoginAt: String? = null,
    val binding: McCharacterBinding? = null,
    val medals: MedalProfileDto? = null,
    val stats: VanillaStatsResponse? = null,
)

@Serializable
data class McCharacterBinding(
    val igngId: Int? = null,
    val isPublic: Boolean = false,
    val user: PublicProfileUser? = null,
)

@Serializable
data class HallLandsResponse(
    val success: Boolean = false,
    val lands: List<HallLandDto> = emptyList(),
)

@Serializable
data class HallLandDto(
    val id: Long = 0,
    val serverId: Int? = null,
    val serverName: String = "",
    val name: String = "",
    val ownerName: String = "",
    val world: String? = null,
    val center: Point3Dto = Point3Dto(),
    val size: Size3Dto = Size3Dto(),
    val playerPermissions: Map<String, JsonElement> = emptyMap(),
    val environmentPermissions: Map<String, JsonElement> = emptyMap(),
    val description: String = "",
)

@Serializable
data class Point3Dto(
    val x: Double? = null,
    val y: Double? = null,
    val z: Double? = null,
)

@Serializable
data class Size3Dto(
    val width: Double? = null,
    val height: Double? = null,
    val length: Double? = null,
)

@Serializable
data class HallFakePlayersResponse(
    val success: Boolean = false,
    val fakePlayers: List<HallFakePlayerDto> = emptyList(),
)

@Serializable
data class HallFakePlayerDto(
    val name: String = "",
    val ownerName: String = "未知",
    val serverName: String = "未命名服务器",
    val onlineSeconds: Long = 0,
)

@Serializable
data class PublicPerfectRosterResponse(
    val success: Boolean = false,
    val total: Int = 0,
    val principles: List<String> = emptyList(),
    val criteria: List<PerfectGuideItem> = emptyList(),
    val benefits: List<PerfectBenefitDto> = emptyList(),
    val members: List<PublicPerfectMemberDto> = emptyList(),
)

@Serializable
data class PerfectGuideItem(
    val title: String = "",
    val description: String = "",
)

@Serializable
data class PerfectBenefitDto(
    val key: String = "",
    val title: String = "",
    val summary: String = "",
    val description: String = "",
    val servers: List<String> = emptyList(),
)

@Serializable
data class PublicPerfectMemberDto(
    val memberNo: Int? = null,
    val memberNoLabel: String? = null,
    val username: String = "",
    val igngId: Int? = null,
    val qualificationLabel: String? = null,
    val since: String? = null,
)

// ==================== 2. Public Status & History ====================

@Serializable
data class PublicStatusResponse(
    val generatedAt: String? = null,
    val collector: PublicCollectorDto? = null,
    val servers: List<PublicServerStatusDto> = emptyList(),
)

@Serializable
data class PublicCollectorDto(
    val id: Int? = null,
    val name: String? = null,
    val label: String? = null,
    val lastSeenAt: String? = null,
    val ageSeconds: Double? = null,
    val freshness: String? = null,
)

@Serializable
data class PublicServerStatusDto(
    val id: Int = 0,
    val slug: String = "",
    val name: String = "",
    val familySlug: String? = null,
    val familyName: String? = null,
    val familyLabel: String? = null,
    val themeKey: String? = null,
    val seasonLabel: String? = null,
    val seasonState: String = "live",
    val isPrimary: Boolean = false,
    val description: String = "",
    val address: String? = null,
    val version: String? = null,
    val core: String? = null,
    val status: String = "unknown",
    val online: Boolean = false,
    val onlinePlayers: Int? = null,
    val pingMs: Double? = null,
    val tps: Double? = null,
    val mspt: Double? = null,
    val cpuUsage: Double? = null,
    val memoryUsageMb: Double? = null,
    val memoryLimitMb: Double? = null,
    val packetLossPct: Double? = null,
    val observedAt: String? = null,
)

@Serializable
data class PublicStatusHistoryResponse(
    val generatedAt: String? = null,
    val bucketMinutes: Int? = null,
    val data: Map<String, ServerHistorySeriesDto> = emptyMap(),
)

@Serializable
data class ServerHistorySeriesDto(
    val latencies: List<LatencySampleDto> = emptyList(),
    val performance: List<PerfSampleDto> = emptyList(),
    val statusGrid: StatusGridDto? = null,
)

@Serializable
data class LatencySampleDto(
    val avg_latency_ms: Double? = null,
    val packet_loss_pct: Double? = null,
    val timestamp_utc: String = "",
    val status: String? = null,
)

@Serializable
data class PerfSampleDto(
    val avg_tps: Double? = null,
    val avg_mspt: Double? = null,
    val cpu_usage: Double? = null,
    val memory_usage_mb: Double? = null,
    val online_players: Int? = null,
    val recorded_at: String = "",
)

@Serializable
data class StatusGridDto(
    val segments: Int = 0,
    val buckets: List<StatusGridBucketDto> = emptyList(),
)

@Serializable
data class StatusGridBucketDto(
    val start: String = "",
    val end: String? = null,
    val status: String = "no_data",
    val coverageLabel: String = "",
)

enum class StatusRangePreset(val id: String, val label: String, val hours: Long) {
    H1("1h", "1小时", 1),
    H24("24h", "24小时", 24),
    D3("3d", "3天", 72),
    D7("7d", "7天", 168),
    D30("30d", "30天", 720),
}

// ==================== 3. ChatLogs (chat.igng.net) ====================

@Serializable
data class ChatSourcesResponse(
    val servers: List<ChatServerChannel> = emptyList(),
    val qqGroups: List<ChatQqGroupChannel> = emptyList(),
)

@Serializable
data class ChatServerChannel(
    val id: Int = 0,
    val name: String = "",
    val address: String? = null,
)

@Serializable
data class ChatQqGroupChannel(
    val id: String = "",
    val name: String = "",
    val count: Int? = null,
    val platform: String? = null,
)

@Serializable
data class ChatLogsResponse(
    val messages: List<ChatMessageDto> = emptyList(),
    val hasMore: Boolean = false,
    val focusedId: String? = null,
)

@Serializable
data class ChatAttachmentDto(
    val index: Int = 0,
    val type: String = "image",
    val url: String? = null,
    val previewUrl: String? = null,
    val mimeType: String? = null,
    val fileName: String? = null,
)

@Serializable
data class ChatMessageDto(
    val id: String = "",
    val source: String = "mc",
    val server_id: Int? = null,
    val group_id: String? = null,
    val player_name: String = "",
    val sender_id: String? = null,
    val content: String = "",
    val sent_at: String = "",
    val is_at_all: Boolean = false,
    val has_forward: Boolean = false,
    val forward_count: Int? = null,
    val attachments: List<ChatAttachmentDto> = emptyList(),
    val media: List<ChatAttachmentDto> = emptyList(),
) {
    val allAttachments: List<ChatAttachmentDto> get() = (attachments + media).distinctBy { "${it.index}:${it.url}:${it.previewUrl}" }
}

@Serializable
data class ChatForwardResponse(
    val messageId: String? = null,
    val title: String? = null,
    val nodes: List<ChatForwardNodeDto> = emptyList(),
    val items: List<ChatForwardNodeDto> = emptyList(),
    val messages: List<ChatForwardNodeDto> = emptyList(),
) {
    val resolvedNodes: List<ChatForwardNodeDto>
        get() = when {
            nodes.isNotEmpty() -> nodes
            items.isNotEmpty() -> items
            else -> messages
        }
}

@Serializable
data class ChatForwardNodeDto(
    val sender_name: String? = null,
    val senderName: String? = null,
    val nickname: String? = null,
    val sender_id: String? = null,
    val content: String = "",
    val time: String? = null,
    val sent_at: String? = null,
) {
    val author: String get() = sender_name ?: senderName ?: nickname ?: sender_id ?: "未知成员"
}

// ==================== 4. Wiki (docs.igng.net) ====================

@Serializable
data class WikiCatalogResponse(
    val knowledgeBase: WikiKnowledgeBase? = null,
    val groups: List<WikiGroupDto> = emptyList(),
    val pages: List<WikiPageSummary> = emptyList(),
    val updatedAt: String? = null,
)

@Serializable
data class WikiKnowledgeBase(
    val slug: String = "IGNGmc",
    val title: String = "IGNGmc Wiki",
    val description: String = "",
)

@Serializable
data class WikiGroupDto(
    val id: Int = 0,
    val title: String = "",
    val slug: String = "",
    val sortOrder: Int = 0,
    val pages: List<WikiPageSummary> = emptyList(),
)

@Serializable
data class WikiPageSummary(
    val id: Int = 0,
    val title: String = "",
    val slug: String = "",
    val groupId: Int? = null,
    val groupTitle: String? = null,
    val summary: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class WikiPageResponse(
    val page: WikiPageDetail,
)

@Serializable
data class WikiPageDetail(
    val id: Int = 0,
    val title: String = "",
    val slug: String = "",
    val content: String = "",
    val updatedAt: String? = null,
    val groupTitle: String? = null,
)
