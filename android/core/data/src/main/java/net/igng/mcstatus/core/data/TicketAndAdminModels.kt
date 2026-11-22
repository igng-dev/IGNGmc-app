package net.igng.mcstatus.core.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.igng.mcstatus.core.model.V1Id

private fun ticketTypeLabel(raw: String): String = when (raw.trim().lowercase()) {
    "report" -> "举报"
    "appeal" -> "申诉"
    "feedback" -> "反馈"
    else -> raw.ifBlank { "工单" }
}

private fun ticketRelationLabel(raw: String?): String = when (raw?.trim()?.lowercase()) {
    "created" -> "我发起的"
    "participated" -> "我参与的"
    "viewed" -> "我查看的"
    else -> "工单"
}

// ==================== Tickets (GET/POST/PATCH /api/v1/tickets*, /ticket-options, /mc-players, /media) ====================

@Serializable
data class TicketTargetDto(
    @SerialName("mc_username") val mcUsernameRaw: String? = null,
    @SerialName("mc_uuid") val mcUuid: String? = null,
) {
    val mcUsername: String get() = mcUsernameRaw.orEmpty()
}

@Serializable
data class TicketActorDto(
    val id: String = "",
    @SerialName("display_name") val displayName: String? = null,
)

@Serializable
data class TicketAccessRule(
    @SerialName("group_code") val groupCode: String = "mc.admin",
    @SerialName("target_group_enabled") val targetGroupEnabled: Boolean = true,
)

@Serializable
data class TicketAccessPolicy(
    val view: TicketAccessRule = TicketAccessRule("mc.admin", true),
    val participate: TicketAccessRule = TicketAccessRule("mc.admin", true),
    val manage: TicketAccessRule = TicketAccessRule("mc.admin", false),
)

@Serializable
data class TicketSummaryDto(
    @SerialName("id") val v1Id: String = "",
    val title: String = "",
    @SerialName("ticket_type") val type: String = "",
    @SerialName("ticket_status") val status: String = "",
    val relation: String? = null,
    @SerialName("server_scope") val serverScope: String = "all",
    @SerialName("server_id") val serverIdRaw: String? = null,
    @SerialName("server_name") val serverName: String? = null,
    val targets: List<TicketTargetDto> = emptyList(),
    @SerialName("last_message_at") val lastMessageAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("can_view") val canView: Boolean = true,
    @SerialName("can_reply") val canReply: Boolean = false,
    @SerialName("can_manage") val canManage: Boolean = false,
    val etag: String = "",
) {
    val id: Int get() = V1Id.parseNumeric(v1Id) ?: 0
    val statusLabel: String get() = if (status.equals("closed", true)) "已结单" else "处理中"
    val typeLabel: String get() = ticketTypeLabel(type)
    val creatorName: String get() = targets.firstOrNull()?.mcUsername?.ifBlank { null } ?: ticketRelationLabel(relation)
}

@Serializable
data class TicketListResponse(
    val data: List<TicketSummaryDto> = emptyList(),
    val pagination: PaginationDto = PaginationDto(),
) {
    val resolvedTickets: List<TicketSummaryDto> get() = data
    val total: Int get() = pagination.total
}

@Serializable
data class TicketAttachmentDto(
    val id: String = "",
    val url: String? = null,
    @SerialName("size_bytes") val sizeBytes: Long = 0L,
    @SerialName("content_type") val contentType: String? = null,
    @SerialName("audit_status") val auditStatus: String? = null,
    @SerialName("appeal_status") val appealStatus: String? = null,
) {
    val resolvedMediaId: String get() = id
}

@Serializable
data class TicketMessageDto(
    val id: String = "",
    @SerialName("author_type") val authorType: String = "user",
    val author: TicketActorDto? = null,
    val kind: String = "reply",
    val content: String = "",
    val attachments: List<TicketAttachmentDto> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null,
) {
    val authorName: String get() = author?.displayName?.ifBlank { null } ?: "用户"
    val isInitial: Boolean get() = kind.equals("initial", ignoreCase = true)
    val isAdmin: Boolean get() = authorType.equals("admin", ignoreCase = true)
}

@Serializable
data class TicketDetailDto(
    @SerialName("id") val v1Id: String = "",
    val title: String = "",
    @SerialName("ticket_type") val type: String = "",
    @SerialName("ticket_status") val status: String = "",
    val relation: String? = null,
    @SerialName("server_scope") val serverScope: String = "all",
    @SerialName("server_id") val serverIdRaw: String? = null,
    @SerialName("server_name") val serverName: String? = null,
    val targets: List<TicketTargetDto> = emptyList(),
    @SerialName("access_policy") val accessPolicy: TicketAccessPolicy = TicketAccessPolicy(),
    @SerialName("can_view") val effectiveCanView: Boolean = true,
    @SerialName("can_reply") val effectiveCanReply: Boolean = false,
    @SerialName("can_edit") val effectiveCanEdit: Boolean = false,
    @SerialName("can_manage") val effectiveCanManage: Boolean = false,
    @SerialName("can_close") val effectiveCanClose: Boolean = false,
    @SerialName("can_reopen") val effectiveCanReopen: Boolean = false,
    val creator: TicketActorDto? = null,
    @SerialName("closed_by") val closedBy: TicketActorDto? = null,
    @SerialName("closed_at") val closedAt: String? = null,
    val messages: List<TicketMessageDto> = emptyList(),
    @SerialName("truncated_messages") val truncatedMessages: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val etag: String = "",
) {
    val id: Int get() = V1Id.parseNumeric(v1Id) ?: 0
    val statusLabel: String get() = if (status.equals("closed", true)) "已结单" else "处理中"
    val typeLabel: String get() = ticketTypeLabel(type)
    val creatorName: String get() = creator?.displayName?.ifBlank { null } ?: "玩家"
}

@Serializable
data class TicketServerOptionDto(
    @SerialName("id") val v1Id: String = "",
    val name: String = "",
    @SerialName("sort_order") val sortOrder: Int = 0,
) {
    val id: Int get() = V1Id.parseNumeric(v1Id) ?: 0
}

@Serializable
data class TicketAccessGroupDto(
    val code: String = "",
    val name: String = "",
)

@Serializable
data class TicketLimitsDto(
    @SerialName("title_max_length") val titleMaxLength: Int = 120,
    @SerialName("content_max_length") val contentMaxLength: Int = 10000,
    @SerialName("targets_max_count") val targetsMaxCount: Int = 20,
    @SerialName("player_query_max_length") val playerQueryMaxLength: Int = 32,
    @SerialName("attachment_max_bytes") val attachmentMaxBytes: Long = 10_485_760L,
    @SerialName("attachment_max_count") val attachmentMaxCount: Int = 6,
)

data class TicketTypeOption(
    val value: String,
    val label: String,
)

@Serializable
data class TicketOptionsResponse(
    val servers: List<TicketServerOptionDto> = emptyList(),
    @SerialName("ticket_types") val ticketTypes: List<String> = emptyList(),
    @SerialName("server_scopes") val serverScopes: List<String> = emptyList(),
    @SerialName("access_groups") val accessGroups: List<TicketAccessGroupDto> = emptyList(),
    @SerialName("access_actions") val accessActions: List<String> = emptyList(),
    val limits: TicketLimitsDto = TicketLimitsDto(),
) {
    val types: List<TicketTypeOption>
        get() = ticketTypes.map { TicketTypeOption(value = it, label = ticketTypeLabel(it)) }
}

@Serializable
data class TicketPlayerOption(
    @SerialName("mc_username") val username: String = "",
    @SerialName("mc_uuid") val uuid: String? = null,
)

@Serializable
data class TicketPlayersResponse(
    val data: List<TicketPlayerOption> = emptyList(),
    val pagination: PaginationDto = PaginationDto(),
)

@Serializable
data class UploadedMediaDto(
    val id: String = "",
    val url: String? = null,
    @SerialName("size_bytes") val sizeBytes: Long = 0L,
    @SerialName("content_type") val contentType: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
)

@Serializable
data class CreateTicketPayload(
    val title: String,
    val content: String,
    @SerialName("ticket_type") val ticketType: String,
    @SerialName("server_scope") val serverScope: String = "all",
    @SerialName("server_id") val serverId: String? = null,
    val targets: List<String> = emptyList(),
    @SerialName("attachment_ids") val attachmentIds: List<String> = emptyList(),
    @SerialName("access_policy") val accessPolicy: TicketAccessPolicy? = null,
)

@Serializable
data class UpdateTicketPayload(
    val title: String? = null,
    @SerialName("ticket_type") val ticketType: String? = null,
    val targets: List<String>? = null,
    @SerialName("access_policy") val accessPolicy: TicketAccessPolicy? = null,
)

@Serializable
data class TicketDraft(
    val title: String = "",
    val content: String = "",
    val type: String = "",
    val savedAtMs: Long = 0L,
)

// ==================== Admin Pocket Console (GET/POST /api/v1/mc/admin/*) ====================

@Serializable
data class AdminTicketOverviewByServerDto(
    @SerialName("server_id") val serverId: String? = null,
    @SerialName("server_name") val serverName: String = "",
    @SerialName("open_count") val openCount: Int = 0,
    @SerialName("total_count") val totalCount: Int = 0,
)

@Serializable
data class AdminTicketOverviewResponse(
    val role: String? = null,
    @SerialName("open_count") val openCount: Int = 0,
    @SerialName("closed_count") val closedCount: Int = 0,
    @SerialName("total_count") val totalCount: Int = 0,
    @SerialName("unassigned_count") val unassignedCount: Int = 0,
    @SerialName("by_type") val byType: Map<String, Int> = emptyMap(),
    @SerialName("by_server") val byServer: List<AdminTicketOverviewByServerDto> = emptyList(),
)

@Serializable
data class AdminPlayerResultDto(
    val kind: String = "",
    @SerialName("mc_username") val mcUsername: String? = null,
    @SerialName("mc_uuid") val mcUuid: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("numeric_user_id") val igngId: Int? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("is_public") val isPublic: Boolean? = null,
    @SerialName("has_bound_mc") val hasBoundMc: Boolean = false,
    @SerialName("last_login_at") val lastLoginAt: String? = null,
    @SerialName("medal_count") val medalCount: Int = 0,
    @SerialName("play_time_seconds") val playTimeSeconds: Long = 0L,
    @SerialName("play_time_formatted") val playTimeFormatted: String = "",
    @SerialName("blocks_mined") val blocksMined: Long = 0L,
    @SerialName("mob_kills") val mobKills: Long = 0L,
)

@Serializable
data class AdminPlayerSearchResponse(
    val role: String? = null,
    val total: Int = 0,
    val results: List<AdminPlayerResultDto> = emptyList(),
)

@Serializable
data class AdminPunishmentDto(
    val id: Int = 0,
    val type: String? = null,
    val player_name: String? = null,
    val player_uuid: String? = null,
    val admin_name: String? = null,
    val reason: String? = null,
    @SerialName("is_active") val isActive: Boolean = false,
    @SerialName("is_permanent") val isPermanent: Boolean = false,
    @SerialName("server_origin") val serverOrigin: String? = null,
    val started_at: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
)

@Serializable
data class AdminPunishmentsResponse(
    val data: List<AdminPunishmentDto> = emptyList(),
    val pagination: PaginationDto = PaginationDto(),
) {
    val records: List<AdminPunishmentDto> get() = data
    val total: Int get() = pagination.total
}

@Serializable
data class HistorianServerRefDto(
    @SerialName("server_id") val serverId: String = "",
    @SerialName("server_name") val serverName: String = "",
)

@Serializable
data class HistorianItemDto(
    @SerialName("report_id") val id: String = "",
    @SerialName("server_id") val serverId: String = "",
    @SerialName("period_type") val periodKind: String = "",
    @SerialName("period_start") val periodStart: String = "",
    val title: String? = null,
    val status: String? = null,
    @SerialName("latest_run_id") val latestRunId: String? = null,
    val error: String? = null,
    @SerialName("latest_created_at") val latestCreatedAt: String? = null,
)

@Serializable
data class HistorianListResponse(
    val role: String? = null,
    val servers: List<HistorianServerRefDto> = emptyList(),
    val items: List<HistorianItemDto> = emptyList(),
    @SerialName("next_cursor") val nextCursor: String? = null,
)

@Serializable
data class HistorianObservationDto(
    val title: String? = null,
    val category: String? = null,
    val detail: String? = null,
    val summary: String? = null,
)

@Serializable
data class HistorianRunDto(
    @SerialName("run_id") val runId: String = "",
    val status: String? = null,
    val phase: String? = null,
    val markdown: String? = null,
    val summary: String? = null,
    val observations: List<HistorianObservationDto> = emptyList(),
    val error: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("ended_at") val endedAt: String? = null,
)

@Serializable
data class HistorianReportMetaDto(
    @SerialName("report_id") val id: String = "",
    @SerialName("server_id") val serverId: String = "",
    @SerialName("period_type") val periodKind: String = "",
    @SerialName("period_start") val periodStart: String = "",
    val title: String? = null,
    @SerialName("current_run_id") val currentRunId: String? = null,
)

@Serializable
data class HistorianDetailResponse(
    val report: HistorianReportMetaDto? = null,
    val run: HistorianRunDto? = null,
)

@Serializable
data class AdminNodeStatusBlockDto(
    val overall: String = "unknown",
    val liveness: String = "unknown",
    val performance: String = "unknown",
    val network: String = "unknown",
)

@Serializable
data class AdminNodePerformanceDto(
    val tps: Double? = null,
    val mspt: Double? = null,
    @SerialName("cpu_usage") val cpuUsage: Double? = null,
    @SerialName("memory_usage_mb") val memoryUsageMb: Double? = null,
    @SerialName("online_players") val onlinePlayers: Int? = null,
    @SerialName("recorded_at") val recordedAt: String? = null,
)

@Serializable
data class AdminNodeLatencyDto(
    @SerialName("avg_latency_ms") val avgLatencyMs: Double? = null,
    @SerialName("packet_loss_pct") val packetLossPct: Double? = null,
    val status: String? = null,
    val freshness: String? = null,
    @SerialName("timestamp_at") val timestampAt: String? = null,
)

@Serializable
data class AdminNodeServerDto(
    @SerialName("server_id") val serverId: String = "",
    @SerialName("numeric_server_id") val numericServerId: Int = 0,
    @SerialName("server_name") val serverName: String = "",
    val address: String? = null,
    val status: AdminNodeStatusBlockDto = AdminNodeStatusBlockDto(),
    val performance: AdminNodePerformanceDto? = null,
    val latency: AdminNodeLatencyDto? = null,
)

@Serializable
data class AdminCurrentNodesResponse(
    val role: String? = null,
    @SerialName("generated_at") val generatedAt: String? = null,
    val collector: CollectorStatusDto? = null,
    val servers: List<AdminNodeServerDto> = emptyList(),
)

@Serializable
data class TrafficTotalsDto(
    @SerialName("tx_bytes") val tx: Long = 0L,
    @SerialName("rx_bytes") val rx: Long = 0L,
    @SerialName("total_bytes") val total: Long = 0L,
)

@Serializable
data class TrafficIpDto(
    val ip: String = "",
    @SerialName("tx_bytes") val tx: Long = 0L,
    @SerialName("rx_bytes") val rx: Long = 0L,
    @SerialName("total_bytes") val total: Long = 0L,
)

@Serializable
data class TrafficAdminResponse(
    val role: String? = null,
    @SerialName("bucket_seconds") val bucketSeconds: Int = 0,
    @SerialName("start_at") val startAt: String? = null,
    @SerialName("end_at") val endAt: String? = null,
    val source: String = "backend",
    @SerialName("source_label") val sourceLabel: String? = null,
    val totals: TrafficTotalsDto = TrafficTotalsDto(),
    @SerialName("top_ips") val topIps: List<TrafficIpDto> = emptyList(),
)
