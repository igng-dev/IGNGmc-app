package net.igng.mcstatus.core.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import net.igng.mcstatus.core.model.V1Id

// ==================== Server Catalog & MOTD (GET /api/v1/mc/servers, /motd) ====================

@Serializable
data class FlatServerDto(
    @SerialName("server_id") val serverId: String = "",
    @SerialName("numeric_id") val numericId: Int = 0,
    @SerialName("server_name") val serverName: String = "",
    val slug: String = "",
    @SerialName("family_slug") val familySlug: String = "",
    val address: String? = null,
    val version: String? = null,
    @SerialName("is_enabled") val isEnabled: Boolean = true,
)

@Serializable
data class HomeServersResponse(
    val families: List<ServerFamilyDto> = emptyList(),
    val servers: List<FlatServerDto> = emptyList(),
)

@Serializable
data class ServerFamilyDto(
    val slug: String = "",
    val name: String = "",
    val label: String = "",
    @SerialName("theme_key") val themeKey: String? = null,
    val tagline: String = "",
    val description: String = "",
    val kind: String = "",
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("home_featured") val homeFeatured: Boolean = false,
    @SerialName("primary_season") val primarySeason: ServerSeasonDto? = null,
    val seasons: List<ServerSeasonDto> = emptyList(),
    @SerialName("alive_seasons") val aliveSeasons: List<ServerSeasonDto> = emptyList(),
    @SerialName("historical_seasons") val historicalSeasons: List<ServerSeasonDto> = emptyList(),
    @SerialName("updated_at") val updatedAt: String? = null,
) {
    val allKnownSeasons: List<ServerSeasonDto>
        get() {
            val map = LinkedHashMap<String, ServerSeasonDto>()
            primarySeason?.let { map[it.slug] = it }
            aliveSeasons.forEach { map.putIfAbsent(it.slug, it) }
            seasons.forEach { map.putIfAbsent(it.slug, it) }
            historicalSeasons.forEach { map.putIfAbsent(it.slug, it) }
            return map.values.toList()
        }
}

@Serializable
data class ServerSeasonDto(
    val slug: String = "",
    @SerialName("family_slug") val familySlug: String = "",
    @SerialName("family_name") val familyName: String = "",
    val label: String = "",
    val ordinal: Int? = null,
    val state: String = "",
    val kind: String = "",
    @SerialName("is_primary") val primary: Boolean = false,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("ended_at") val endedAt: String? = null,
    @SerialName("archive_id") val archiveId: String? = null,
    @SerialName("map_relay_key") val mapRelayKey: String? = null,
    @SerialName("map_mode") val mapMode: String = "none",
    @SerialName("has_map") val hasMap: Boolean = false,
    val modpack: String? = null,
    @SerialName("server_id") val serverIdRaw: String? = null,
    @SerialName("numeric_server_id") val numericServerId: Int? = null,
    val address: String? = null,
    val version: String? = null,
    val core: String? = null,
    val platform: String? = null,
    val seed: String? = null,
    @SerialName("status_text") val statusText: String? = null,
    val notice: String? = null,
    val badge: String? = null,
    val eyebrow: String? = null,
    val description: String = "",
    @SerialName("card_description") val cardDescription: String = "",
    @SerialName("background_key") val backgroundKey: String? = null,
    val highlights: List<String> = emptyList(),
    val rules: List<String> = emptyList(),
    val plugins: List<String> = emptyList(),
    val worlds: List<String> = emptyList(),
    val command: String? = null,
    @SerialName("cpu_model") val cpuModel: String? = null,
    @SerialName("cpu_spec") val cpuSpec: String? = null,
    @SerialName("memory_spec") val memorySpec: String? = null,
    @SerialName("disk_spec") val diskSpec: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
) {
    val serverId: Int?
        get() = numericServerId ?: V1Id.parseNumeric(serverIdRaw)
}

@Serializable
data class MotdPlayersDto(
    val online: Int = 0,
    val max: Int = 0,
)

@Serializable
data class MotdResponse(
    @SerialName("server_id") val serverId: String = "",
    @SerialName("numeric_server_id") val numericServerId: Int? = null,
    val online: Boolean = false,
    val motd: String = "",
    val players: MotdPlayersDto? = null,
    val version: String = "",
    @SerialName("ping_ms") val pingMs: Double? = null,
    val error: String? = null,
)

// ==================== Public Status & History (GET /api/v1/mc/status, /history) ====================

@Serializable
data class PublicStatusResponse(
    @SerialName("generated_at") val generatedAt: String? = null,
    val collector: CollectorStatusDto? = null,
    val servers: List<PublicServerStatusDto> = emptyList(),
)

@Serializable
data class CollectorStatusDto(
    val id: Int? = null,
    val name: String? = null,
    val label: String? = null,
    @SerialName("last_seen_at") val lastSeenAt: String? = null,
    @SerialName("age_seconds") val ageSeconds: Double? = null,
    val freshness: String? = null,
    val version: String? = null,
)

@Serializable
data class StatusComponentDto(
    val status: String = "unknown",
    @SerialName("last_checked_at") val lastCheckedAt: String? = null,
    val source: String? = null,
)

@Serializable
data class PublicServerStatusDto(
    @SerialName("id") val v1Id: String = "",
    @SerialName("server_id") val serverIdRaw: String = "",
    @SerialName("numeric_id") val numericId: Int = 0,
    val slug: String = "",
    val name: String = "",
    @SerialName("family_slug") val familySlug: String? = null,
    @SerialName("family_name") val familyName: String? = null,
    @SerialName("family_label") val familyLabel: String? = null,
    @SerialName("family_kind") val familyKind: String? = null,
    @SerialName("theme_key") val themeKey: String? = null,
    @SerialName("season_label") val seasonLabel: String? = null,
    @SerialName("season_state") val seasonState: String? = null,
    @SerialName("season_ordinal") val seasonOrdinal: Int? = null,
    @SerialName("is_primary") val isPrimary: Boolean = false,
    val description: String = "",
    val address: String? = null,
    val version: String? = null,
    val core: String? = null,
    val platform: String? = null,
    val status: String = "unknown",
    val online: Boolean = false,
    @SerialName("online_players") val onlinePlayers: Int? = null,
    @SerialName("max_players") val maxPlayers: Int? = null,
    val tps: Double? = null,
    val mspt: Double? = null,
    @SerialName("cpu_usage") val cpuUsage: Double? = null,
    @SerialName("memory_usage_mb") val memoryUsageMb: Double? = null,
    @SerialName("ping_ms") val pingMs: Double? = null,
    @SerialName("packet_loss_pct") val packetLossPct: Double? = null,
    @SerialName("ping_status") val pingStatus: String? = null,
    @SerialName("ping_freshness") val pingFreshness: String? = null,
    @SerialName("last_checked_at") val lastCheckedAt: String? = null,
    val components: Map<String, StatusComponentDto> = emptyMap(),
) {
    val id: Int
        get() = if (numericId > 0) numericId else (V1Id.parseNumeric(v1Id.ifBlank { serverIdRaw }) ?: 0)

    val resolvedServerId: String
        get() = serverIdRaw.ifBlank { v1Id.ifBlank { V1Id.format("srv", id) } }
}

@Serializable
data class StatusHistoryRangeDto(
    @SerialName("start_at") val startAt: String? = null,
    @SerialName("end_at") val endAt: String? = null,
)

@Serializable
data class PublicStatusHistoryResponse(
    @SerialName("generated_at") val generatedAt: String? = null,
    val range: StatusHistoryRangeDto? = null,
    @SerialName("bucket_minutes") val bucketMinutes: Int = 0,
    val collector: CollectorStatusDto? = null,
    val servers: Map<String, ServerHistorySeriesDto> = emptyMap(),
) {
    fun seriesFor(serverId: Int): ServerHistorySeriesDto? =
        servers[V1Id.format("srv", serverId)] ?: servers[serverId.toString()]
}

@Serializable
data class ServerHistorySeriesDto(
    @SerialName("server_id") val serverId: String = "",
    @SerialName("numeric_server_id") val numericServerId: Int? = null,
    val latencies: List<HistoryLatencyPoint> = emptyList(),
    val performance: List<HistoryPerformancePoint> = emptyList(),
    @SerialName("status_grid") val statusGrid: List<StatusGridBucketDto> = emptyList(),
)

@Serializable
data class HistoryLatencyPoint(
    @SerialName("timestamp_at") val timestampAt: String = "",
    val avg_latency_ms: Double? = null,
    val packet_loss_pct: Double? = null,
    @SerialName("sample_count") val sampleCount: Int? = null,
) {
    val timestamp_utc: String get() = timestampAt
}

@Serializable
data class HistoryPerformancePoint(
    val recorded_at: String = "",
    val avg_tps: Double? = null,
    val avg_mspt: Double? = null,
    val cpu_usage: Double? = null,
    val memory_usage_mb: Double? = null,
    val online_players: Int? = null,
    @SerialName("metrics_status") val metricsStatus: String? = null,
    @SerialName("resource_status") val resourceStatus: String? = null,
)

@Serializable
data class StatusGridBucketDto(
    @SerialName("bucket_at") val bucketAt: String = "",
    val status: String = "unknown",
    @SerialName("avg_latency_ms") val avgLatencyMs: Double? = null,
    @SerialName("packet_loss_pct") val packetLossPct: Double? = null,
) {
    val start: String get() = bucketAt
    val coverageLabel: String
        get() = buildString {
            append(status)
            avgLatencyMs?.let { append(" · ${it.toInt()}ms") }
            packetLossPct?.takeIf { it > 0.0 }?.let { append(" · 丢包 ${"%.1f".format(it)}%") }
        }
}

enum class StatusRangePreset(val label: String, val hours: Long) {
    H1("1小时", 1L),
    H6("6小时", 6L),
    H24("24小时", 24L),
    D7("7天", 168L),
}

// ==================== Global Search & Public Player / Character Passports ====================

@Serializable
data class GlobalSearchResponse(
    val found: Boolean = false,
    val results: List<SearchResultItem> = emptyList(),
)

@Serializable
data class SearchResultItem(
    val kind: String = "",
    @SerialName("mc_username") val mcUsernameRaw: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("numeric_user_id") val numericUserId: Int? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("has_bound_mc") val hasBoundMc: Boolean = false,
    val summary: SearchSummaryDto? = null,
) {
    val mcUsername: String get() = mcUsernameRaw.orEmpty()
    val igngId: Int? get() = numericUserId ?: V1Id.parseNumeric(userId)
    val igngName: String? get() = displayName
}

@Serializable
data class SearchSummaryDto(
    @SerialName("play_time_seconds") val playTimeSeconds: Long = 0L,
    @SerialName("blocks_mined") val blocksMined: Long = 0L,
    @SerialName("mob_kills") val mobKills: Long = 0L,
    @SerialName("medal_count") val medalCount: Int = 0,
)

@Serializable
data class PublicUserSummaryDto(
    val id: String = "",
    @SerialName("numeric_id") val numericId: Int = 0,
    val username: String = "",
    val nickname: String? = null,
    val avatar_url: String? = null,
) {
    val displayName: String get() = nickname?.takeIf { it.isNotBlank() } ?: username
}

@Serializable
data class PublicBoundAccountDto(
    val username: String = "",
    @SerialName("account_id") val accountId: String? = null,
    val uuid: String? = null,
    @SerialName("is_public") val isPublic: Boolean? = null,
    @SerialName("bound_at") val boundAt: String? = null,
)

@Serializable
data class PublicProfileLandDto(
    @SerialName("land_id") val landId: String = "",
    @SerialName("server_id") val serverId: String? = null,
    val land_name: String = "",
    val owner_name: String = "",
    val world: String? = null,
    val description: String? = null,
)

@Serializable
data class PublicProfileFakePlayerDto(
    @SerialName("fake_name") val fakeName: String = "",
    @SerialName("creator_name") val creatorName: String = "",
    @SerialName("server_id") val serverId: String? = null,
    @SerialName("server_name") val serverName: String? = null,
    val world: String? = null,
    val x: Double? = null,
    val y: Double? = null,
    val z: Double? = null,
    val health: Double? = null,
    @SerialName("last_updated_at") val lastUpdatedAt: String? = null,
)

@Serializable
data class PublicPlayerProfileResponse(
    val user: PublicUserSummaryDto? = null,
    val accounts: List<PublicBoundAccountDto> = emptyList(),
    val lands: List<PublicProfileLandDto> = emptyList(),
    @SerialName("fake_players") val fakePlayers: List<PublicProfileFakePlayerDto> = emptyList(),
) {
    val boundCharacters: List<String>
        get() = accounts.map { it.username }.filter { it.isNotBlank() }
}

@Serializable
data class McCharacterIdentityDto(
    val username: String = "",
    @SerialName("name_key") val nameKey: String = "",
    @SerialName("in_authme") val inAuthme: Boolean = false,
    @SerialName("last_login_at") val lastLoginAt: String? = null,
    @SerialName("registered_at") val registeredAt: String? = null,
)

@Serializable
data class McCharacterBindingDto(
    @SerialName("user_id") val userId: String? = null,
    @SerialName("numeric_user_id") val numericUserId: Int? = null,
    val username: String = "",
    val nickname: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
) {
    val isPublic: Boolean get() = true
    val igngId: Int? get() = numericUserId ?: V1Id.parseNumeric(userId)
}

@Serializable
data class McCharacterProfileResponse(
    val character: McCharacterIdentityDto = McCharacterIdentityDto(),
    @SerialName("public_binding") val publicBinding: McCharacterBindingDto? = null,
    @SerialName("medal_profile") val medalProfile: MedalProfileDto? = null,
    val stats: VanillaStatsResponse? = null,
) {
    val username: String get() = character.username
    val lastLoginAt: String? get() = character.lastLoginAt
    val binding: McCharacterBindingDto? get() = publicBinding
}

// ==================== Community Hall & Perfect Roster ====================

@Serializable
data class CoordinateDto(
    val x: Double? = null,
    val y: Double? = null,
    val z: Double? = null,
)

@Serializable
data class HallLandDto(
    @SerialName("land_id") val id: String = "",
    @SerialName("server_id") val serverId: String = "",
    @SerialName("server_name") val serverName: String = "",
    @SerialName("land_name") val name: String = "",
    @SerialName("owner_name") val ownerName: String = "",
    val world: String = "",
    val center: CoordinateDto = CoordinateDto(),
    @SerialName("guest_flags") val playerPermissions: Map<String, JsonElement> = emptyMap(),
    @SerialName("environment_flags") val environmentFlags: Map<String, JsonElement> = emptyMap(),
    val description: String = "",
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class HallLandsResponse(
    val data: List<HallLandDto> = emptyList(),
)

@Serializable
data class HallFakePlayerDto(
    @SerialName("fake_name") val name: String = "",
    @SerialName("creator_name") val ownerName: String = "",
    @SerialName("server_id") val serverId: String? = null,
    @SerialName("server_name") val serverNameRaw: String? = null,
    val world: String? = null,
    val x: Double? = null,
    val y: Double? = null,
    val z: Double? = null,
    val health: Double? = null,
    @SerialName("food_level") val foodLevel: Double? = null,
    @SerialName("online_seconds") val onlineSeconds: Long = 0L,
    @SerialName("last_updated_at") val lastUpdatedAt: String? = null,
) {
    val serverName: String get() = serverNameRaw ?: serverId ?: "服务器"
}

@Serializable
data class HallFakePlayersResponse(
    val data: List<HallFakePlayerDto> = emptyList(),
)

@Serializable
data class PerfectBenefitServerDto(
    @SerialName("server_id") val serverId: String? = null,
    @SerialName("server_name") val serverName: String? = null,
)

@Serializable
data class PerfectBenefitDto(
    val key: String = "",
    val title: String = "",
    val description: String = "",
    val used: Int? = null,
    val limit: Int? = null,
    val servers: List<PerfectBenefitServerDto> = emptyList(),
) {
    val summary: String get() = description
}

@Serializable
data class PublicPerfectMemberDto(
    val username: String = "",
    @SerialName("user_id") val userId: String? = null,
    @SerialName("numeric_user_id") val numericUserId: Int? = null,
    @SerialName("joined_at") val since: String? = null,
) {
    val igngId: Int? get() = numericUserId ?: V1Id.parseNumeric(userId)
    val memberNoLabel: String? get() = null
    val qualificationLabel: String? get() = null
}

@Serializable
data class PublicPerfectRosterResponse(
    val available: Boolean = true,
    val group: String = "perfect",
    @SerialName("wiki_url") val wikiUrl: String? = null,
    @SerialName("total_count") val total: Int = 0,
    @SerialName("public_count") val publicCount: Int = 0,
    @SerialName("hidden_count") val hiddenCount: Int = 0,
    val benefits: List<PerfectBenefitDto> = emptyList(),
    val rules: List<String> = emptyList(),
    val criteria: List<String> = emptyList(),
    @SerialName("apply_steps") val applySteps: List<String> = emptyList(),
    val principles: List<String> = emptyList(),
    @SerialName("public_members") val members: List<PublicPerfectMemberDto> = emptyList(),
)

// ==================== Chat Archive (GET /api/v1/chat/*) ====================

@Serializable
data class ChatServerSourceDto(
    @SerialName("server_id") val serverId: String = "",
    @SerialName("numeric_server_id") val numericServerId: Int = 0,
    val name: String = "",
) {
    val id: Int get() = if (numericServerId > 0) numericServerId else (V1Id.parseNumeric(serverId) ?: 0)
}

@Serializable
data class ChatGroupSourceDto(
    val id: String = "",
    val name: String = "",
    val platform: String = "qq",
    @SerialName("is_private") val isPrivate: Boolean = false,
    @SerialName("message_count") val messageCount: Int = 0,
    @SerialName("last_message_at") val lastMessageAt: String? = null,
    @SerialName("last_preview") val lastPreview: String? = null,
)

@Serializable
data class ChatSourcesResponse(
    val servers: List<ChatServerSourceDto> = emptyList(),
    @SerialName("qq_groups") val qqGroups: List<ChatGroupSourceDto> = emptyList(),
    @SerialName("private_chats") val privateChats: List<ChatGroupSourceDto> = emptyList(),
    val authenticated: Boolean = false,
    @SerialName("can_view_recalled") val canViewRecalled: Boolean = false,
)

@Serializable
data class ChatReplyPreviewDto(
    val id: String = "",
    @SerialName("sender_name") val senderName: String = "",
    val preview: String = "",
)

@Serializable
data class ChatAttachmentDto(
    val index: Int = 0,
    val kind: String = "file",
    val name: String? = null,
    val url: String? = null,
    @SerialName("thumb_url") val thumbUrl: String? = null,
    @SerialName("mime_type") val mimeType: String? = null,
    @SerialName("size_bytes") val sizeBytes: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    @SerialName("external_url") val externalUrl: String? = null,
) {
    val previewUrl: String? get() = thumbUrl ?: url
}

@Serializable
data class ChatMessageDto(
    val id: String = "",
    val source: String = "",
    val platform: String = "",
    @SerialName("server_id") val serverId: String? = null,
    @SerialName("group_id") val groupId: String? = null,
    @SerialName("sender_id") val senderId: String? = null,
    @SerialName("sender_name") val senderName: String = "",
    @SerialName("sender_avatar") val senderAvatar: String? = null,
    @SerialName("is_bot") val isBot: Boolean = false,
    @SerialName("is_recalled") val isRecalled: Boolean = false,
    @SerialName("persona_label") val personaLabel: String? = null,
    val content: String = "",
    @SerialName("reply_to") val replyTo: ChatReplyPreviewDto? = null,
    val attachments: List<ChatAttachmentDto> = emptyList(),
    @SerialName("forward_summary") val forwardSummary: String? = null,
    @SerialName("has_forward") val has_forward: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
) {
    val player_name: String get() = senderName
    val sent_at: String? get() = createdAt
    val is_at_all: Boolean get() = content.contains("@全体成员") || content.contains("@all")
    val allAttachments: List<ChatAttachmentDto> get() = attachments
}

@Serializable
data class ChatLogsResponse(
    val messages: List<ChatMessageDto> = emptyList(),
    @SerialName("has_more") val hasMore: Boolean = false,
    @SerialName("focused_id") val focusedId: String? = null,
)

@Serializable
data class ChatForwardNodeDto(
    @SerialName("sender_id") val senderId: String? = null,
    @SerialName("sender_name") val author: String = "",
    val content: String = "",
    @SerialName("created_at") val sent_at: String? = null,
) {
    val time: String? get() = sent_at
}

@Serializable
data class ChatForwardResponse(
    @SerialName("message_id") val messageId: String = "",
    val title: String? = null,
    val summary: String? = null,
    val nodes: List<ChatForwardNodeDto> = emptyList(),
) {
    val resolvedNodes: List<ChatForwardNodeDto> get() = nodes
}

// ==================== Wiki (GET /api/v1/wiki/catalog, /pages/{page_id}) ====================

@Serializable
data class WikiPageSummary(
    val id: Int = 0,
    val slug: String = "",
    val title: String = "",
    val format: String = "markdown",
    @SerialName("current_version") val currentVersion: Int = 1,
    @SerialName("updated_at") val updatedAt: String? = null,
    val path: String = "",
    val summary: String? = null,
    val groupTitle: String? = null,
)

@Serializable
data class WikiKnowledgeBaseDto(
    val id: Int = 0,
    val slug: String = "",
    val name: String = "",
    val description: String? = null,
    @SerialName("root_page") val rootPage: WikiPageSummary? = null,
)

@Serializable
data class WikiNavNodeDto(
    val id: Int = 0,
    @SerialName("parent_id") val parentId: Int? = null,
    @SerialName("node_type") val nodeType: String = "page",
    val slug: String = "",
    val title: String = "",
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("updated_at") val updatedAt: String? = null,
    val path: String = "",
    val page: WikiPageSummary? = null,
    val children: List<WikiNavNodeDto> = emptyList(),
)

data class WikiCatalogGroup(
    val id: String,
    val title: String,
    val pages: List<WikiPageSummary>,
)

@Serializable
data class WikiCatalogResponse(
    @SerialName("schema_version") val schemaVersion: Int = 1,
    @SerialName("generated_at") val generatedAt: String? = null,
    @SerialName("knowledge_base") val knowledgeBase: WikiKnowledgeBaseDto? = null,
    val nodes: List<WikiNavNodeDto> = emptyList(),
) {
    private fun collectPages(node: WikiNavNodeDto, groupTitle: String?): List<WikiPageSummary> {
        val list = mutableListOf<WikiPageSummary>()
        node.page?.let { p ->
            list += p.copy(
                title = p.title.ifBlank { node.title },
                groupTitle = groupTitle,
            )
        }
        node.children.forEach { child ->
            list += collectPages(child, groupTitle ?: node.title)
        }
        return list
    }

    val groups: List<WikiCatalogGroup>
        get() {
            val out = mutableListOf<WikiCatalogGroup>()
            val topLoose = mutableListOf<WikiPageSummary>()
            knowledgeBase?.rootPage?.let { root ->
                topLoose += root.copy(groupTitle = knowledgeBase.name.ifBlank { "首页" })
            }
            nodes.forEach { node ->
                if (node.children.isNotEmpty() || node.nodeType.equals("folder", true)) {
                    val items = collectPages(node, node.title)
                    if (items.isNotEmpty()) {
                        out += WikiCatalogGroup(id = "node_${node.id}", title = node.title, pages = items)
                    }
                } else {
                    node.page?.let { p ->
                        topLoose += p.copy(title = p.title.ifBlank { node.title }, groupTitle = "基础指南")
                    }
                }
            }
            if (topLoose.isNotEmpty()) {
                out.add(0, WikiCatalogGroup(id = "root_docs", title = knowledgeBase?.name?.ifBlank { "文档导航" } ?: "文档导航", pages = topLoose))
            }
            return out
        }

    val pages: List<WikiPageSummary>
        get() = groups.flatMap { it.pages }.distinctBy { it.id }
}

@Serializable
data class WikiPageDetail(
    val id: Int = 0,
    val slug: String = "",
    val title: String = "",
    val content: String = "",
    val format: String = "markdown",
    @SerialName("current_version") val currentVersion: Int = 1,
    val path: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class WikiPageResponse(
    @SerialName("schema_version") val schemaVersion: Int = 1,
    @SerialName("knowledge_base") val knowledgeBase: WikiKnowledgeBaseDto? = null,
    val page: WikiPageDetail = WikiPageDetail(),
)
