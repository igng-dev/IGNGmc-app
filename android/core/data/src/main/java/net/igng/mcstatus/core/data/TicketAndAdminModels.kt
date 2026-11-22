package net.igng.mcstatus.core.data

import kotlinx.serialization.Serializable

// ==================== 6. Tickets (dash.igng.net/api/mc/tickets & mc.igng.net/api/hall/tickets) ====================

@Serializable
data class TicketOptionsResponse(
    val servers: List<TicketServerOption> = emptyList(),
    val types: List<TicketTypeOption> = emptyList(),
    val accessGroups: List<TicketAccessGroupOption> = emptyList(),
    val defaultAccessPolicy: TicketAccessPolicy = TicketAccessPolicy(),
)

@Serializable
data class TicketServerOption(
    val id: Int = 0,
    val name: String = "",
)

@Serializable
data class TicketTypeOption(
    val value: String = "",
    val label: String = "",
)

@Serializable
data class TicketAccessGroupOption(
    val value: String = "",
    val label: String = "",
    val description: String = "",
    val level: Int = 0,
)

@Serializable
data class TicketAccessEntry(
    val groupCode: String = "mc.admin",
    val groupLabel: String = "",
    val includeTargetGroup: Boolean = false,
    val label: String = "",
)

@Serializable
data class TicketAccessPolicy(
    val view: TicketAccessEntry = TicketAccessEntry(),
    val participate: TicketAccessEntry = TicketAccessEntry(),
    val manage: TicketAccessEntry = TicketAccessEntry(),
)

@Serializable
data class TicketPlayersResponse(
    val players: List<TicketPlayerOption> = emptyList(),
)

@Serializable
data class TicketPlayerOption(
    val username: String = "",
    val authmeUsername: String = "",
)

@Serializable
data class TicketListResponse(
    val tickets: List<TicketSummaryDto> = emptyList(),
    val items: List<TicketSummaryDto> = emptyList(),
    val page: Int = 1,
    val total: Int = 0,
    val totalPages: Int = 1,
) {
    val resolvedTickets: List<TicketSummaryDto> get() = if (tickets.isNotEmpty()) tickets else items
}

@Serializable
data class TicketTargetDto(
    val id: Int = 0,
    val mcUsername: String = "",
    val isMine: Boolean = false,
)

@Serializable
data class TicketSummaryDto(
    val id: Int = 0,
    val title: String = "",
    val type: String = "",
    val typeLabel: String = "",
    val serverScope: String = "ALL",
    val serverId: Int? = null,
    val serverName: String? = null,
    val status: String = "OPEN",
    val statusLabel: String = "",
    val creatorName: String = "",
    val creatorId: Int = 0,
    val replyCount: Int = 0,
    val messageCount: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = "",
    val lastRepliedAt: String? = null,
    val targets: List<TicketTargetDto> = emptyList(),
)

@Serializable
data class TicketDetailResponse(
    val ticket: TicketDetailDto,
)

@Serializable
data class TicketAttachmentDto(
    val id: String = "",
    val mediaId: String = "",
    val url: String? = null,
    val previewUrl: String? = null,
    val publicUrl: String? = null,
    val mimeType: String? = null,
    val auditStatus: String? = null,
    val audit_status: String? = null,
) {
    val resolvedMediaId: String get() = mediaId.ifBlank { id }
    val resolvedUrl: String? get() = previewUrl ?: url ?: publicUrl
}

@Serializable
data class TicketAuthorLabelDto(
    val key: String = "",
    val label: String = "",
    val tone: String = "muted",
)

@Serializable
data class TicketMessageDto(
    val id: Int = 0,
    val authorId: Int = 0,
    val authorName: String = "",
    val authorAvatarUrl: String? = null,
    val authorType: String? = null,
    val authorLabels: List<TicketAuthorLabelDto> = emptyList(),
    val kind: String = "REPLY",
    val content: String = "",
    val createdAt: String = "",
    val attachments: List<TicketAttachmentDto> = emptyList(),
)

@Serializable
data class TicketCapabilitiesDto(
    val canReply: Boolean = false,
    val canEdit: Boolean = false,
    val canClose: Boolean = false,
    val canReopen: Boolean = false,
)

@Serializable
data class TicketDetailDto(
    val id: Int = 0,
    val title: String = "",
    val type: String = "",
    val typeLabel: String = "",
    val serverScope: String = "ALL",
    val serverId: Int? = null,
    val serverName: String? = null,
    val status: String = "OPEN",
    val statusLabel: String = "",
    val creatorId: Int = 0,
    val creatorName: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
    val targets: List<TicketTargetDto> = emptyList(),
    val accessPolicy: TicketAccessPolicy = TicketAccessPolicy(),
    val capabilities: TicketCapabilitiesDto = TicketCapabilitiesDto(),
    val canReply: Boolean = false,
    val canEdit: Boolean = false,
    val canClose: Boolean = false,
    val canReopen: Boolean = false,
    val messages: List<TicketMessageDto> = emptyList(),
) {
    val effectiveCanReply: Boolean get() = capabilities.canReply || canReply
    val effectiveCanEdit: Boolean get() = capabilities.canEdit || canEdit
    val effectiveCanClose: Boolean get() = capabilities.canClose || canClose
    val effectiveCanReopen: Boolean get() = capabilities.canReopen || canReopen
}

@Serializable
data class CreateTicketPayload(
    val title: String,
    val type: String,
    val serverScope: String,
    val serverId: Int? = null,
    val accessPolicy: TicketAccessPolicy,
    val targetNames: List<String> = emptyList(),
    val content: String,
    val attachments: List<String> = emptyList(),
)

@Serializable
data class UpdateTicketPayload(
    val title: String,
    val type: String,
    val targetNames: List<String> = emptyList(),
    val accessPolicy: TicketAccessPolicy? = null,
)

@Serializable
data class UploadedMediaResponse(
    val success: Boolean = false,
    val media: UploadedMediaDto? = null,
)

@Serializable
data class UploadedMediaDto(
    val id: String = "",
    val previewUrl: String? = null,
    val publicUrl: String? = null,
    val mimeType: String? = null,
)

@Serializable
data class TicketDraft(
    val title: String = "",
    val content: String = "",
    val type: String = "",
    val updatedAtMs: Long = 0L,
)

// ==================== 7. Admin Pocket Console ====================

@Serializable
data class AdminTicketOverviewResponse(
    val total: Int = 0,
    val openCount: Int = 0,
    val closedCount: Int = 0,
    val pendingCount: Int = 0,
)

@Serializable
data class AdminPlayerSearchResponse(
    val results: List<AdminPlayerResultDto> = emptyList(),
    val players: List<AdminPlayerResultDto> = emptyList(),
) {
    val allResults: List<AdminPlayerResultDto> get() = if (results.isNotEmpty()) results else players
}

@Serializable
data class AdminPlayerResultDto(
    val igngId: Int? = null,
    val username: String? = null,
    val nickname: String? = null,
    val mcUsername: String? = null,
    val boundAccounts: List<String> = emptyList(),
    val isBanned: Boolean = false,
    val isMuted: Boolean = false,
    val playtimeLabel: String? = null,
)

@Serializable
data class AdminPunishmentsResponse(
    val role: String? = null,
    val total: Int = 0,
    val page: Int = 1,
    val totalPages: Int = 1,
    val records: List<McPunishmentDto> = emptyList(),
)

@Serializable
data class HistorianListResponse(
    val servers: List<HistorianServerDto> = emptyList(),
    val items: List<HistorianReportSummaryDto> = emptyList(),
    val next_cursor: String? = null,
)

@Serializable
data class HistorianServerDto(
    val server_id: Int = 0,
    val server_name: String = "",
)

@Serializable
data class HistorianReportSummaryDto(
    val id: String = "",
    val server_id: Int = 0,
    val period_kind: String = "weekly",
    val period_start: String = "",
    val title: String? = null,
    val summary: String? = null,
    val status: String? = null,
    val latest_created_at: String? = null,
)

@Serializable
data class HistorianDetailResponse(
    val report: HistorianReportSummaryDto? = null,
    val run: HistorianRunDto? = null,
)

@Serializable
data class HistorianRunDto(
    val id: String = "",
    val status: String? = null,
    val markdown: String? = null,
    val summary: String? = null,
    val report_markdown: String? = null,
    val created_at: String? = null,
    val observations: List<HistorianObservationDto> = emptyList(),
)

@Serializable
data class HistorianObservationDto(
    val title: String? = null,
    val severity: String? = null,
    val category: String? = null,
    val summary: String? = null,
    val detail: String? = null,
)

@Serializable
data class TrafficAdminResponse(
    val bucketSeconds: Long = 1800,
    val source: String? = null,
    val sourceLabel: String? = null,
    val totals: TrafficTotalsDto = TrafficTotalsDto(),
    val topIps: List<IpTrafficDto> = emptyList(),
)

@Serializable
data class TrafficTotalsDto(
    val tx: Long = 0,
    val rx: Long = 0,
    val total: Long = 0,
)

@Serializable
data class IpTrafficDto(
    val ip: String = "",
    val tx: Long = 0,
    val rx: Long = 0,
    val total: Long = 0,
)

@Serializable
data class AdminCurrentNodesResponse(
    val generatedAt: String? = null,
    val nodes: List<AdminNodeDto> = emptyList(),
    val data: Map<String, AdminServerCurrentDto> = emptyMap(),
)

@Serializable
data class AdminNodeDto(
    val node_id: Int = 0,
    val node_name: String = "",
    val freshness: String? = null,
    val last_seen_at: String? = null,
)

@Serializable
data class AdminServerCurrentDto(
    val server: AdminServerInfoDto? = null,
    val performance: PerfSampleDto? = null,
    val status: AdminServerStatusFlagsDto? = null,
)

@Serializable
data class AdminServerInfoDto(
    val server_id: Int = 0,
    val server_name: String = "",
    val address: String? = null,
)

@Serializable
data class AdminServerStatusFlagsDto(
    val overall: String = "unknown",
    val liveness: String = "unknown",
    val performance: String = "no_data",
    val network: String = "no_data",
)
