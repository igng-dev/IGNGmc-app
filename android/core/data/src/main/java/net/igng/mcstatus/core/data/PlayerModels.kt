package net.igng.mcstatus.core.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import net.igng.mcstatus.core.model.V1Id

@Serializable
data class PaginationDto(
    val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 20,
    val total: Int = 0,
    @SerialName("total_pages") val totalPages: Int = 1,
)

// ==================== Bound Minecraft Accounts (GET/PATCH/DELETE /api/v1/mc/accounts) ====================

@Serializable
data class BoundMcAccountsResponse(
    val data: List<BoundMcAccountDto> = emptyList(),
)

@Serializable
data class BoundMcAccountDto(
    @SerialName("account_id") val accountId: String = "",
    val username: String = "",
    val uuid: String? = null,
    @SerialName("uuid_provenance") val uuidProvenance: String? = null,
    val verified: Boolean = false,
    @SerialName("is_public") val isPublic: Boolean = false,
    @SerialName("bound_at") val boundAt: String? = null,
    @SerialName("last_login_at") val lastLoginAt: String? = null,
    @SerialName("is_banned") val isBanned: Boolean? = null,
    @SerialName("is_muted") val isMuted: Boolean? = null,
    @SerialName("login_history_available") val loginHistoryAvailable: Boolean = false,
    @SerialName("penalty_history_available") val penaltyHistoryAvailable: Boolean = false,
    @SerialName("login_history_since") val loginHistorySince: String? = null,
    @SerialName("penalty_history_since") val penaltyHistorySince: String? = null,
) {
    val id: String get() = accountId.ifBlank { username }
    val resolvedName: String get() = username
    val createdAt: String? get() = boundAt
}

@Serializable
data class BoundMcAccountDetailDto(
    @SerialName("account_id") val accountId: String = "",
    val username: String = "",
    val uuid: String? = null,
    @SerialName("uuid_provenance") val uuidProvenance: String? = null,
    val verified: Boolean = false,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("is_public") val isPublic: Boolean = false,
    @SerialName("bound_at") val boundAt: String? = null,
    @SerialName("last_login_at") val lastLoginAt: String? = null,
    @SerialName("login_history_since") val loginHistorySince: String? = null,
    @SerialName("penalty_history_since") val penaltyHistorySince: String? = null,
)

@Serializable
data class PenaltyRecordDto(
    val id: Int = 0,
    val type: String? = null,
    val reason: String? = null,
    @SerialName("admin_name") val adminName: String? = null,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("is_active") val isActive: Boolean = false,
    @SerialName("server_scope") val serverScope: String? = null,
    @SerialName("predates_registration") val predatesRegistration: Boolean = false,
)

@Serializable
data class ActivePenaltyDto(
    val ban: PenaltyRecordDto? = null,
    val mute: PenaltyRecordDto? = null,
)

@Serializable
data class PenaltyPageDto(
    val data: List<PenaltyRecordDto> = emptyList(),
    val pagination: PaginationDto = PaginationDto(),
)

@Serializable
data class McCharacterDetailsResponse(
    val account: BoundMcAccountDetailDto? = null,
    @SerialName("login_history_available") val loginHistoryAvailable: Boolean = false,
    @SerialName("penalty_history_available") val penaltyHistoryAvailable: Boolean = false,
    @SerialName("active_penalty") val activePenalty: ActivePenaltyDto? = null,
    val penalties: PenaltyPageDto = PenaltyPageDto(),
) {
    val history: List<PenaltyRecordDto> get() = penalties.data
}

@Serializable
data class McLoginRecordDto(
    @SerialName("record_id") val recordId: String = "",
    val username: String = "",
    val uuid: String? = null,
    @SerialName("server_id") val serverIdRaw: String? = null,
    @SerialName("server_name") val serverName: String? = null,
    @SerialName("is_success") val isSuccess: Boolean = true,
    @SerialName("ip_address") val ipAddress: String? = null,
    val world: String? = null,
    val x: Double? = null,
    val y: Double? = null,
    val z: Double? = null,
    @SerialName("recorded_at") val recordedAt: String? = null,
) {
    val serverId: Int get() = serverIdRaw?.let { V1Id.parseNumeric(it) } ?: 0
}

@Serializable
data class McLoginRecordsFilterDto(
    val username: String? = null,
    @SerialName("server_id") val serverId: String? = null,
)

@Serializable
data class McLoginRecordsResponse(
    val data: List<McLoginRecordDto> = emptyList(),
    val pagination: PaginationDto = PaginationDto(),
    val filters: McLoginRecordsFilterDto = McLoginRecordsFilterDto(),
    @SerialName("history_since") val historySince: String? = null,
    @SerialName("unverified_accounts") val unverifiedAccounts: List<String> = emptyList(),
)

@Serializable
data class McActionResult(
    @SerialName("account_id") val accountId: String? = null,
    @SerialName("land_id") val landId: String? = null,
    val username: String? = null,
    @SerialName("is_public") val isPublic: Boolean? = null,
    val description: String? = null,
    @SerialName("password_updated") val passwordUpdated: Boolean? = null,
    @SerialName("custom_name") val customName: String? = null,
    @SerialName("is_customized") val isCustomized: Boolean? = null,
    val message: String? = null,
)

// ==================== Lands / Teleports / Fake Players ====================

@Serializable
data class LandMemberDto(
    val uuid: String? = null,
    val name: String? = null,
    @SerialName("group_name") val groupName: String? = null,
)

@Serializable
data class PlayerLandDto(
    @SerialName("land_id") val landId: String = "",
    @SerialName("server_id") val serverIdRaw: String = "",
    val server_name: String? = null,
    val land_name: String? = null,
    val owner_name: String? = null,
    val owner_uuid: String? = null,
    val plugin_type: String? = null,
    val world: String? = null,
    val x: Double? = null,
    val y: Double? = null,
    val z: Double? = null,
    val viewer_role: String? = null,
    val can_edit: Boolean = false,
    val is_public: Boolean = false,
    val description: String = "",
    val members: List<LandMemberDto> = emptyList(),
    @SerialName("updated_at") val updatedAt: String? = null,
) {
    val id: Int get() = V1Id.parseNumeric(landId) ?: 0
    val server_id: Int get() = V1Id.parseNumeric(serverIdRaw) ?: 0
}

@Serializable
data class PlayerLandsResponse(
    val data: List<PlayerLandDto> = emptyList(),
    val pagination: PaginationDto = PaginationDto(),
) {
    val records: List<PlayerLandDto> get() = data
    val total: Int get() = pagination.total
}

@Serializable
data class PlayerTeleportDto(
    @SerialName("server_id") val serverIdRaw: String = "",
    val server_name: String? = null,
    @SerialName("username") val owner_name: String? = null,
    @SerialName("player_uuid") val playerUuid: String? = null,
    @SerialName("name") val point_name: String? = null,
    val world: String? = null,
    @SerialName("world_key") val worldKey: String? = null,
    val x: Double? = null,
    val y: Double? = null,
    val z: Double? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
) {
    val server_id: Int get() = V1Id.parseNumeric(serverIdRaw) ?: 0
    val id: String get() = "$serverIdRaw:${owner_name.orEmpty()}:${point_name.orEmpty()}"
}

@Serializable
data class PlayerTeleportsResponse(
    val data: List<PlayerTeleportDto> = emptyList(),
)

@Serializable
data class PlayerFakePlayerDto(
    val creator_name: String? = null,
    val fake_name: String? = null,
    @SerialName("server_id") val serverIdRaw: String? = null,
    val server_name: String? = null,
    val world: String? = null,
    val x: Double? = null,
    val y: Double? = null,
    val z: Double? = null,
    val health: Double? = null,
    @SerialName("food_level") val hunger: Double? = null,
    @SerialName("last_updated_at") val lastUpdatedAt: String? = null,
) {
    val server_id: Int get() = V1Id.parseNumeric(serverIdRaw) ?: 0
    val id: String get() = "${serverIdRaw.orEmpty()}:${fake_name.orEmpty()}"
}

@Serializable
data class PlayerFakePlayersResponse(
    val data: List<PlayerFakePlayerDto> = emptyList(),
)

// ==================== OneWayVault (GET /api/v1/mc/vaults*) ====================

@Serializable
data class VaultTotalsDto(
    val vault_count: Int = 0,
    val entry_count: Int = 0,
    val item_amount: Long = 0L,
    val material_count: Int = 0,
)

@Serializable
data class VaultBucketDto(
    val vault_id: String = "",
    val kind: String = "storage",
    val state: String = "active",
    val entry_count: Int = 0,
    val item_amount: Long = 0L,
    val material_count: Int = 0,
    val source_server_count: Int = 0,
    val source_season_count: Int = 0,
    val last_deposit_at: String? = null,
)

@Serializable
data class VaultOverviewResponse(
    val available: Boolean = false,
    @SerialName("schema_version") val schemaVersion: String? = null,
    val vaults: List<VaultBucketDto> = emptyList(),
    val totals: VaultTotalsDto = VaultTotalsDto(),
)

@Serializable
data class VaultMetaDto(
    val available: Boolean = true,
    val truncated: Boolean = false,
    val scanned: Int = 0,
)

@Serializable
data class VaultItemGroupDto(
    @SerialName("group_key") val groupKey: String? = null,
    val material: String = "",
    @SerialName("material_name") val materialName: String = "",
    @SerialName("display_name") val displayName: String = "",
    @SerialName("has_custom_name") val hasCustomName: Boolean = false,
    val total_amount: Long = 0L,
    val entry_count: Int = 0,
    val enchantments: JsonElement? = null,
    @SerialName("enchantment_labels") val enchantmentLabels: List<String> = emptyList(),
    @SerialName("max_damage") val maxDamage: Int? = null,
    @SerialName("remaining_durability") val remainingDurability: Int? = null,
    @SerialName("earliest_at") val earliestAt: String? = null,
    @SerialName("latest_at") val latestAt: String? = null,
) {
    val title: String
        get() = displayName.ifBlank { materialName }.ifBlank { material }
}

@Serializable
data class VaultItemsResponse(
    val data: List<VaultItemGroupDto> = emptyList(),
    val pagination: PaginationDto = PaginationDto(),
    val meta: VaultMetaDto = VaultMetaDto(),
) {
    val groups: List<VaultItemGroupDto> get() = data
    val available: Boolean get() = meta.available
}

@Serializable
data class VaultOperationDto(
    val operation_id: String = "",
    val vault_id: String = "",
    val kind: String = "",
    val state: String = "",
    @SerialName("server_id") val serverId: String? = null,
    val source_server: String? = null,
    val source_season: String? = null,
    val material: String? = null,
    val amount: Long? = null,
    @SerialName("item_count") val itemCount: Int? = null,
    val error_code: String? = null,
    val error_detail: String? = null,
    val created_at: String? = null,
    val completed_at: String? = null,
)

@Serializable
data class VaultHistoryResponse(
    val data: List<VaultOperationDto> = emptyList(),
    val pagination: PaginationDto = PaginationDto(),
    val meta: VaultMetaDto = VaultMetaDto(),
) {
    val records: List<VaultOperationDto> get() = data
}

@Serializable
data class VaultTraceEventDto(
    @SerialName("audit_id") val auditId: String? = null,
    val event_type: String? = null,
    @SerialName("from_state") val fromState: String? = null,
    @SerialName("to_state") val toState: String? = null,
    val detail: String? = null,
    val created_at: String? = null,
) {
    val state: String? get() = toState ?: fromState
}

@Serializable
data class VaultTraceResponse(
    val available: Boolean = false,
    val operation_id: String = "",
    val events: List<VaultTraceEventDto> = emptyList(),
)

// ==================== Vanilla Stats / Medals / Personal Perfect / Permissions ====================

@Serializable
data class VanillaStatsOverviewDto(
    @SerialName("play_time_seconds") val playTimeSeconds: Long = 0L,
    @SerialName("play_time_formatted") val playTimeFormatted: String = "",
    @SerialName("blocks_mined") val blocksMined: Long = 0L,
    @SerialName("items_crafted") val itemsCrafted: Long = 0L,
    @SerialName("mob_kills") val mobKills: Long = 0L,
    @SerialName("player_kills") val playerKills: Long = 0L,
    val deaths: Long = 0L,
    @SerialName("distance_cm") val distanceCm: Long = 0L,
    @SerialName("distance_formatted") val distanceFormatted: String = "",
    @SerialName("server_count") val serverCount: Int = 0,
    @SerialName("last_synced_at") val lastSyncedAt: String? = null,
)

@Serializable
data class VanillaStatsServerDto(
    @SerialName("server_id") val serverIdRaw: String = "",
    @SerialName("numeric_server_id") val numericServerId: Int = 0,
    @SerialName("server_name") val serverName: String = "",
    @SerialName("play_time_seconds") val playTimeSeconds: Long = 0L,
    @SerialName("play_time_formatted") val playTimeFormatted: String = "",
    @SerialName("blocks_mined") val blocksMined: Long = 0L,
    @SerialName("mob_kills") val mobKills: Long = 0L,
    val deaths: Long = 0L,
    @SerialName("last_synced_at") val lastSyncedAt: String? = null,
) {
    val serverId: Int get() = if (numericServerId > 0) numericServerId else (V1Id.parseNumeric(serverIdRaw) ?: 0)
}

@Serializable
data class VanillaStatEntryDto(
    val category: String = "",
    val key: String = "",
    val label: String = "",
    val value: Long = 0L,
    val formatted: String? = null,
)

@Serializable
data class VanillaStatsCategoryDto(
    val id: String = "",
    val label: String = "",
    val total: Long = 0L,
    val items: List<VanillaStatEntryDto> = emptyList(),
)

@Serializable
data class VanillaStatsResponse(
    val available: Boolean = true,
    val accounts: List<String> = emptyList(),
    val overview: VanillaStatsOverviewDto = VanillaStatsOverviewDto(),
    @SerialName("server_breakdown") val servers: List<VanillaStatsServerDto> = emptyList(),
    val highlights: List<VanillaStatEntryDto> = emptyList(),
    val categories: List<VanillaStatsCategoryDto> = emptyList(),
) {
    val summary: VanillaStatsOverviewDto get() = overview
    private fun categoryItems(catId: String): List<VanillaStatEntryDto> =
        categories.firstOrNull { it.id.equals(catId, ignoreCase = true) }?.items.orEmpty()

    val movement: List<VanillaStatEntryDto> get() = categoryItems("movement")
    val topMined: List<VanillaStatEntryDto> get() = categoryItems("mined")
    val topCrafted: List<VanillaStatEntryDto> get() = categoryItems("crafted")
    val topKilled: List<VanillaStatEntryDto> get() = categoryItems("killed")
}

@Serializable
data class MedalEntryDto(
    @SerialName("assignment_id") val id: String = "",
    @SerialName("medal_id") val medalId: String = "",
    val name: String = "",
    val description: String? = null,
    @SerialName("icon_material") val iconMaterial: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 0,
    val scope: String = "global",
    @SerialName("server_id") val serverId: String? = null,
    @SerialName("server_name") val serverName: String? = null,
    @SerialName("username") val account: String? = null,
    @SerialName("granted_at") val grantedAt: String? = null,
    val issuer: String? = null,
    @SerialName("reason") val publicReason: String? = null,
) {
    val displayName: String? get() = name.ifBlank { null }
}

@Serializable
data class CustomMedalSlotDto(
    @SerialName("username") val ownerName: String = "",
    @SerialName("name_key") val nameKey: String = "",
    @SerialName("name") val customName: String? = null,
    @SerialName("is_customized") val isCustomized: Boolean = false,
    @SerialName("updated_at") val updatedAt: String? = null,
) {
    val canEdit: Boolean get() = ownerName.isNotBlank()
}

@Serializable
data class MedalSummaryDto(
    @SerialName("account_count") val accountCount: Int = 0,
    @SerialName("medal_count") val medalCount: Int = 0,
    @SerialName("global_count") val globalCount: Int = 0,
    @SerialName("server_medal_count") val serverMedalCount: Int = 0,
    @SerialName("server_count") val serverCount: Int = 0,
    @SerialName("total_playtime_seconds") val totalPlaytimeSeconds: Long = 0L,
)

@Serializable
data class MedalProfileDto(
    val available: Boolean = true,
    @SerialName("schema_version") val schemaVersion: String = "003",
    val accounts: List<String> = emptyList(),
    val medals: List<MedalEntryDto> = emptyList(),
    @SerialName("custom_medals") val customMedals: List<CustomMedalSlotDto> = emptyList(),
    val summary: MedalSummaryDto = MedalSummaryDto(),
) {
    val allActiveMedals: List<MedalEntryDto> get() = medals
}

@Serializable
data class PersonalPerfectHolderDto(
    val username: String = "",
    val uuid: String? = null,
    @SerialName("is_holder") val isHolder: Boolean = false,
    @SerialName("in_luckperms") val inLuckPerms: Boolean = false,
    @SerialName("member_no_label") val memberNoLabel: String? = null,
    @SerialName("qualification_label") val qualificationLabel: String? = null,
    @SerialName("first_added_at") val firstAddedAt: String? = null,
    val contexts: List<String> = emptyList(),
)

@Serializable
data class PersonalPerfectResponse(
    val available: Boolean = true,
    @SerialName("has_perfect") val hasPerfect: Boolean = false,
    val group: String = "perfect",
    @SerialName("wiki_url") val wikiUrl: String? = null,
    val holders: List<PersonalPerfectHolderDto> = emptyList(),
    val benefits: List<PerfectBenefitDto> = emptyList(),
    val rules: List<PerfectRuleDto> = emptyList(),
    val criteria: List<PerfectCriterionDto> = emptyList(),
    @SerialName("apply_steps") val applySteps: List<String> = emptyList(),
    val principles: List<String> = emptyList(),
)

@Serializable
data class PermissionNodeDto(
    @SerialName("node_key") val nodeKey: String = "",
    val effect: String = "allow",
    val contexts: List<String> = emptyList(),
    @SerialName("expires_at") val expiresAt: String? = null,
) {
    val value: Boolean get() = !effect.equals("deny", ignoreCase = true)
}

@Serializable
data class PermissionGroupDto(
    val name: String = "",
    @SerialName("display_name") val displayName: String? = null,
    val prefix: String? = null,
    val weight: Int = 0,
    val assignment: String = "direct",
    @SerialName("inherited_from") val inheritedFrom: List<String> = emptyList(),
    val contexts: List<String> = emptyList(),
    val permissions: List<PermissionNodeDto> = emptyList(),
)

@Serializable
data class PlayerPermissionAccountDto(
    val username: String = "",
    val uuid: String? = null,
    @SerialName("server_id") val serverId: String? = null,
    @SerialName("server_name") val serverName: String? = null,
    val found: Boolean = false,
    @SerialName("primary_group") val primaryGroup: String? = null,
    val prefix: String? = null,
    val suffix: String? = null,
    @SerialName("snapshot_published_at") val snapshotPublishedAt: String? = null,
    val groups: List<PermissionGroupDto> = emptyList(),
    @SerialName("independent_permissions") val independentPermissions: List<PermissionNodeDto> = emptyList(),
) {
    val key: String get() = "${username}:${serverId ?: "global"}"
}

@Serializable
data class PermissionSummaryDto(
    @SerialName("account_count") val accountCount: Int = 0,
    @SerialName("identity_count") val identityCount: Int = 0,
    @SerialName("group_count") val groupCount: Int = 0,
    @SerialName("direct_group_count") val directGroupCount: Int = 0,
    @SerialName("inherited_group_count") val inheritedGroupCount: Int = 0,
    @SerialName("group_permission_count") val groupPermissionCount: Int = 0,
    @SerialName("independent_permission_count") val independentPermissionCount: Int = 0,
    @SerialName("permission_node_count") val permissionNodeCount: Int = 0,
)

@Serializable
data class PlayerPermissionsResponse(
    val source: String = "luck_perms",
    val accounts: List<PlayerPermissionAccountDto> = emptyList(),
    val summary: PermissionSummaryDto = PermissionSummaryDto(),
)

@Serializable
data class ExpiringPermissionDto(
    val id: Int = 0,
    val kind: String = "permission",
    val label: String = "",
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("context_label") val contextLabel: String = "global",
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("remaining_seconds") val remainingSeconds: Long = 0L,
)

@Serializable
data class PermissionHistoryEntryDto(
    val id: Int = 0,
    val type: String = "u",
    val action: String = "",
    @SerialName("recorded_at") val recordedAt: String? = null,
)

@Serializable
data class PermissionInsightAccountDto(
    val username: String = "",
    val uuid: String? = null,
    val expiring: List<ExpiringPermissionDto> = emptyList(),
    val history: List<PermissionHistoryEntryDto> = emptyList(),
)

@Serializable
data class PermissionInsightsResponse(
    val accounts: List<PermissionInsightAccountDto> = emptyList(),
)
