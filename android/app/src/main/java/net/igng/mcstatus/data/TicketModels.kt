package net.igng.mcstatus.data

import kotlinx.serialization.Serializable

@Serializable
data class TicketServerOption(
    val id: Int,
    val name: String,
)

@Serializable
data class TicketTypeOption(
    val value: String,
    val label: String,
)

@Serializable
data class TicketAccessGroupOption(
    val value: String,
    val label: String,
    val description: String = "",
    val level: Int = 0,
)

@Serializable
data class TicketAccessActionOption(
    val value: String,
    val key: String,
    val label: String,
    val description: String = "",
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
data class TicketOptions(
    val servers: List<TicketServerOption> = emptyList(),
    val types: List<TicketTypeOption> = emptyList(),
    val accessGroups: List<TicketAccessGroupOption> = emptyList(),
    val accessActions: List<TicketAccessActionOption> = emptyList(),
    val defaultAccessPolicy: TicketAccessPolicy = TicketAccessPolicy(),
)

@Serializable
data class TicketPlayerOption(
    val username: String,
    val authmeUsername: String,
)

@Serializable
data class TicketPlayersResponse(
    val players: List<TicketPlayerOption> = emptyList(),
)

@Serializable
data class TicketTarget(
    val id: Int = 0,
    val mcUsername: String = "",
    val authmeUsername: String = "",
    val mcUuid: String? = null,
    val isMine: Boolean = false,
)

@Serializable
data class TicketAuthorLabel(
    val key: String = "",
    val label: String = "",
    val tone: String = "muted",
)

@Serializable
data class TicketMessagePreview(
    val id: Int = 0,
    val authorId: Int = 0,
    val authorName: String = "",
    val kind: String = "REPLY",
    val content: String = "",
    val createdAt: String = "",
)

@Serializable
data class CustomerTicketSummary(
    val id: Int = 0,
    val title: String = "",
    val type: String = "",
    val typeLabel: String = "",
    val serverScope: String = "ALL",
    val serverId: Int? = null,
    val serverName: String? = null,
    val status: String = "OPEN",
    val statusLabel: String = "",
    val relation: String = "",
    val relationLabel: String = "",
    val accessPolicy: TicketAccessPolicy = TicketAccessPolicy(),
    val canView: Boolean = false,
    val canParticipate: Boolean = false,
    val canManage: Boolean = false,
    val canReply: Boolean = false,
    val canEdit: Boolean = false,
    val canClose: Boolean = false,
    val canManagePolicy: Boolean = false,
    val conversationState: String? = null,
    val conversationStateLabel: String? = null,
    val latestMessage: TicketMessagePreview? = null,
    val targets: List<TicketTarget> = emptyList(),
    val targetCount: Int = 0,
    val creatorName: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
    val closedAt: String? = null,
)

@Serializable
data class TicketListResponse(
    val tickets: List<CustomerTicketSummary> = emptyList(),
    val page: Int = 1,
    val limit: Int = 20,
    val total: Int = 0,
    val totalPages: Int = 1,
    val bindingLookupUnavailable: Boolean = false,
)

@Serializable
data class TicketMessage(
    val id: Int = 0,
    val authorId: Int = 0,
    val authorName: String = "",
    val authorRole: String = "",
    val authorLabels: List<TicketAuthorLabel> = emptyList(),
    val isMine: Boolean = false,
    val isAdmin: Boolean = false,
    val isCreator: Boolean = false,
    val isTarget: Boolean = false,
    val targetAccounts: List<String> = emptyList(),
    val kind: String = "REPLY",
    val content: String = "",
    val createdAt: String = "",
)

@Serializable
data class CustomerTicketDetail(
    val id: Int = 0,
    val title: String = "",
    val type: String = "",
    val typeLabel: String = "",
    val serverScope: String = "ALL",
    val serverId: Int? = null,
    val serverName: String? = null,
    val status: String = "OPEN",
    val statusLabel: String = "",
    val creatorUserId: Int = 0,
    val creatorName: String = "",
    val isCreator: Boolean = false,
    val relation: String = "",
    val relationLabel: String = "",
    val accessPolicy: TicketAccessPolicy = TicketAccessPolicy(),
    val canView: Boolean = false,
    val canParticipate: Boolean = false,
    val canManage: Boolean = false,
    val canReply: Boolean = false,
    val canEdit: Boolean = false,
    val canClose: Boolean = false,
    val canManagePolicy: Boolean = false,
    val conversationState: String? = null,
    val conversationStateLabel: String? = null,
    val targets: List<TicketTarget> = emptyList(),
    val targetCount: Int = 0,
    val messages: List<TicketMessage> = emptyList(),
    val createdAt: String = "",
    val updatedAt: String = "",
    val closedAt: String? = null,
    val bindingLookupUnavailable: Boolean = false,
)

@Serializable
data class CustomerTicketDetailResponse(
    val ticket: CustomerTicketDetail,
)

@Serializable
data class CreateTicketResponse(
    val ok: Boolean = false,
    val id: Int = 0,
)

@Serializable
data class TicketUpdateResult(
    val id: Int = 0,
    val title: String = "",
    val type: String = "",
    val targetNames: List<String> = emptyList(),
    val accessPolicy: TicketAccessPolicy = TicketAccessPolicy(),
)

@Serializable
data class TicketUpdateResponse(
    val ok: Boolean = false,
    val ticket: TicketUpdateResult,
)

@Serializable
data class TicketReplyResult(
    val id: Int = 0,
)

@Serializable
data class TicketReplyResponse(
    val ok: Boolean = false,
    val reply: TicketReplyResult,
)

@Serializable
data class TicketCloseResponse(
    val ok: Boolean = false,
    val id: Int = 0,
    val status: String = "CLOSED",
    val closedAt: String? = null,
)
