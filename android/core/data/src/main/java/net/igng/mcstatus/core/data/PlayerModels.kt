package net.igng.mcstatus.core.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// ==================== 5. Player Center (dash.igng.net/mc) ====================

@Serializable
data class BoundMcAccountsResponse(
    val success: Boolean = false,
    val accounts: List<BoundMcAccountDto> = emptyList(),
)

@Serializable
data class BoundMcAccountDto(
    val id: Int = 0,
    val username: String = "",
    val mc_username: String = "",
    val uuid: String? = null,
    val isPublic: Boolean = false,
    val is_public: JsonElement? = null,
    val createdAt: String? = null,
    val created_at: String? = null,
) {
    val resolvedName: String get() = username.ifBlank { mc_username }
}

@Serializable
data class McCharacterDetailsResponse(
    val success: Boolean = false,
    val authme: McAuthmeDto? = null,
    val activePenalty: McActivePenaltyDto? = null,
    val history: List<McPunishmentDto> = emptyList(),
    val totalHistory: Int = 0,
    val page: Int = 1,
    val totalPages: Int = 0,
)

@Serializable
data class McAuthmeDto(
    val lastlogin: Long? = null,
    val ip: String? = null,
)

@Serializable
data class McActivePenaltyDto(
    val ban: McPunishmentDto? = null,
    val mute: McPunishmentDto? = null,
)

@Serializable
data class McPunishmentDto(
    val id: Long = 0,
    val type: String? = null,
    val player_name: String? = null,
    val reason: String? = null,
    val banned_by_name: String? = null,
    val admin_name: String? = null,
    val time: Long? = null,
    val until: Long? = null,
    val started_at: String? = null,
    val ended_at: String? = null,
    val active: JsonElement? = null,
)

@Serializable
data class McActionResult(
    val success: Boolean = false,
    val message: String? = null,
    val isPublic: Boolean? = null,
    val description: String? = null,
)

@Serializable
data class PlayerLandsResponse(
    val success: Boolean = false,
    val records: List<PlayerLandDto> = emptyList(),
    val page: Int = 1,
    val total: Int = 0,
    val totalPages: Int = 1,
)

@Serializable
data class PlayerLandDto(
    val id: Int = 0,
    val server_id: Int = 0,
    val server_name: String? = null,
    val plugin_type: String? = null,
    val owner_name: String? = null,
    val land_name: String? = null,
    val world: String? = null,
    val x: Double? = null,
    val y: Double? = null,
    val z: Double? = null,
    val size: Size3Dto? = null,
    val guest_flags: Map<String, JsonElement> = emptyMap(),
    val environment_flags: Map<String, JsonElement> = emptyMap(),
    val members: List<LandMemberDto> = emptyList(),
    val viewer_role: String? = null,
    val can_edit: Boolean = false,
    val is_public: Boolean = false,
    val description: String = "",
    val last_updated: String? = null,
)

@Serializable
data class LandMemberDto(
    val uuid: String? = null,
    val name: String? = null,
    val permissions: Map<String, JsonElement> = emptyMap(),
)

@Serializable
data class PlayerTeleportsResponse(
    val success: Boolean = false,
    val records: List<PlayerTeleportDto> = emptyList(),
)

@Serializable
data class PlayerTeleportDto(
    val id: Int = 0,
    val server_id: Int = 0,
    val server_name: String? = null,
    val point_name: String? = null,
    val owner_name: String? = null,
    val world: String? = null,
    val x: Double? = null,
    val y: Double? = null,
    val z: Double? = null,
    val last_updated: String? = null,
)

@Serializable
data class PlayerFakePlayersResponse(
    val success: Boolean = false,
    val records: List<PlayerFakePlayerDto> = emptyList(),
)

@Serializable
data class PlayerFakePlayerDto(
    val id: Int = 0,
    val fake_name: String? = null,
    val server_id: Int = 0,
    val server_name: String? = null,
    val creator_name: String? = null,
    val health: Double? = null,
    val hunger: Double? = null,
    val world: String? = null,
    val x: Double? = null,
    val y: Double? = null,
    val z: Double? = null,
    val last_update: String? = null,
)

@Serializable
data class PlayerLoginsResponse(
    val success: Boolean = false,
    val records: List<PlayerLoginRecordDto> = emptyList(),
)

@Serializable
data class PlayerLoginRecordDto(
    val id: Int = 0,
    val player_name: String? = null,
    val server_id: Int = 0,
    val server_name: String? = null,
    val is_success: JsonElement? = null,
    val world: String? = null,
    val x: Double? = null,
    val y: Double? = null,
    val z: Double? = null,
    val ip_address: String? = null,
    val recorded_at: String? = null,
)

// ==================== OneWayVault ====================

@Serializable
data class VaultOverviewResponse(
    val success: Boolean = false,
    val available: Boolean = false,
    val identities: Int = 0,
    val totals: VaultTotalsDto = VaultTotalsDto(),
    val vaults: List<VaultBucketDto> = emptyList(),
)

@Serializable
data class VaultTotalsDto(
    val vault_count: Int = 0,
    val entry_count: Int = 0,
    val item_amount: Long = 0,
    val material_count: Int = 0,
)

@Serializable
data class VaultBucketDto(
    val vault_id: String = "",
    val entry_count: Int = 0,
    val item_amount: Long = 0,
    val material_count: Int = 0,
    val source_server_count: Int = 0,
    val last_deposit_at: String? = null,
)

@Serializable
data class VaultItemsResponse(
    val success: Boolean = false,
    val available: Boolean = false,
    val total: Int = 0,
    val page: Int = 1,
    val totalPages: Int = 1,
    val groups: List<VaultItemGroupDto> = emptyList(),
)

@Serializable
data class VaultItemGroupDto(
    val material: String = "",
    val display_name: String? = null,
    val chinese_name: String? = null,
    val total_amount: Long = 0,
    val entry_count: Int = 0,
    val custom_name: String? = null,
    val enchantments: Map<String, Int> = emptyMap(),
    val lore: List<String> = emptyList(),
    val entries: List<VaultItemEntryDto> = emptyList(),
) {
    val title: String get() = custom_name?.takeIf { it.isNotBlank() }
        ?: chinese_name?.takeIf { it.isNotBlank() }
        ?: display_name?.takeIf { it.isNotBlank() }
        ?: material
}

@Serializable
data class VaultItemEntryDto(
    val entry_id: Long = 0,
    val vault_id: String = "",
    val amount: Int = 0,
    val source_server: String? = null,
    val source_season: String? = null,
    val created_at: String? = null,
)

@Serializable
data class VaultHistoryResponse(
    val success: Boolean = false,
    val available: Boolean = false,
    val page: Int = 1,
    val total: Int = 0,
    val totalPages: Int = 1,
    val records: List<VaultOperationDto> = emptyList(),
)

@Serializable
data class VaultOperationDto(
    val operation_id: String = "",
    val vault_id: String = "",
    val kind: String = "",
    val state: String = "",
    val source_server: String? = null,
    val source_season: String? = null,
    val material: String? = null,
    val chinese_name: String? = null,
    val amount: Int? = null,
    val item_count: Int? = null,
    val created_at: String? = null,
)

@Serializable
data class VaultTraceResponse(
    val success: Boolean = false,
    val operation_id: String = "",
    val events: List<VaultTraceEventDto> = emptyList(),
)

@Serializable
data class VaultTraceEventDto(
    val event_type: String? = null,
    val state: String? = null,
    val server_id: String? = null,
    val detail: String? = null,
    val created_at: String? = null,
)

// ==================== Vanilla Stats ====================

@Serializable
data class VanillaStatsResponse(
    val success: Boolean = false,
    val available: Boolean = true,
    val accounts: List<String> = emptyList(),
    val summary: VanillaStatsSummaryDto = VanillaStatsSummaryDto(),
    val servers: List<VanillaServerStatsOption> = emptyList(),
    val movement: List<VanillaStatEntryDto> = emptyList(),
    val categories: Map<String, List<VanillaStatEntryDto>> = emptyMap(),
    val topMined: List<VanillaStatEntryDto> = emptyList(),
    val topCrafted: List<VanillaStatEntryDto> = emptyList(),
    val topUsed: List<VanillaStatEntryDto> = emptyList(),
    val topKilled: List<VanillaStatEntryDto> = emptyList(),
    val topKilledBy: List<VanillaStatEntryDto> = emptyList(),
)

@Serializable
data class VanillaStatsSummaryDto(
    val playTimeSeconds: Long = 0,
    val blocksMined: Long = 0,
    val itemsCrafted: Long = 0,
    val itemsUsed: Long = 0,
    val mobKills: Long = 0,
    val deaths: Long = 0,
    val distanceCm: Long = 0,
    val jumpCount: Long = 0,
    val serverCount: Int = 0,
)

@Serializable
data class VanillaServerStatsOption(
    val serverId: Int = 0,
    val serverName: String = "",
    val playTimeSeconds: Long = 0,
)

@Serializable
data class VanillaStatEntryDto(
    val key: String = "",
    val label: String = "",
    val value: Long = 0,
    val formatted: String? = null,
)

// ==================== Medals & Perfect & Permissions ====================

@Serializable
data class MedalProfileDto(
    val success: Boolean = false,
    val available: Boolean = true,
    val accounts: List<String> = emptyList(),
    val activeMedals: List<PlayerMedalGrantDto> = emptyList(),
    val medals: List<PlayerMedalGrantDto> = emptyList(),
    val customMedals: List<CustomMedalSlotDto> = emptyList(),
) {
    val allActiveMedals: List<PlayerMedalGrantDto> get() = (activeMedals + medals).distinctBy { "${it.id}:${it.medalId}:${it.account}" }
}

@Serializable
data class PlayerMedalGrantDto(
    val id: Int = 0,
    val medalId: Int = 0,
    val name: String = "",
    val displayName: String? = null,
    val description: String? = null,
    val rarity: String? = null,
    val account: String? = null,
    val playerName: String? = null,
    val grantedAt: String? = null,
    val publicIssuer: String? = null,
    val publicReason: String? = null,
)

@Serializable
data class CustomMedalSlotDto(
    val account: String = "",
    val playerName: String? = null,
    val customName: String? = null,
    val enabled: Boolean = true,
    val canEdit: Boolean = true,
    val updatedAt: String? = null,
) {
    val ownerName: String get() = account.ifBlank { playerName.orEmpty() }
}

@Serializable
data class PersonalPerfectResponse(
    val success: Boolean = false,
    val available: Boolean = true,
    val summary: PersonalPerfectSummaryDto = PersonalPerfectSummaryDto(),
    val accounts: List<PersonalPerfectAccountDto> = emptyList(),
    val benefits: List<PerfectBenefitDto> = emptyList(),
)

@Serializable
data class PersonalPerfectSummaryDto(
    val accountCount: Int = 0,
    val perfectCount: Int = 0,
)

@Serializable
data class PersonalPerfectAccountDto(
    val username: String = "",
    val isPublic: Boolean = false,
    val isPerfect: Boolean = false,
    val inLuckPerms: Boolean = false,
    val memberNoLabel: String? = null,
    val qualificationLabel: String? = null,
    val grantedAt: String? = null,
    val landUsage: List<PerfectUsageRowDto> = emptyList(),
    val fakeUsage: List<PerfectFakeUsageDto> = emptyList(),
)

@Serializable
data class PerfectUsageRowDto(
    val serverId: Int = 0,
    val serverName: String = "",
    val count: Int = 0,
    val limit: Int? = null,
)

@Serializable
data class PerfectFakeUsageDto(
    val serverId: Int = 0,
    val serverName: String = "",
    val total: Int = 0,
    val active: Int = 0,
)

@Serializable
data class PlayerPermissionsResponse(
    val success: Boolean = false,
    val accounts: List<PermissionAccountDto> = emptyList(),
    val summary: PermissionSummaryDto = PermissionSummaryDto(),
)

@Serializable
data class PermissionSummaryDto(
    val accountCount: Int = 0,
    val groupCount: Int = 0,
    val independentPermissionCount: Int = 0,
    val permissionNodeCount: Int = 0,
)

@Serializable
data class PermissionAccountDto(
    val id: Int = 0,
    val username: String = "",
    val identities: List<PermissionIdentityDto> = emptyList(),
)

@Serializable
data class PermissionIdentityDto(
    val serverId: Int? = null,
    val username: String? = null,
    val found: Boolean = false,
    val groups: List<PermissionGroupDto> = emptyList(),
    val independentPermissions: List<PermissionNodeDto> = emptyList(),
)

@Serializable
data class PermissionGroupDto(
    val name: String = "",
    val displayName: String? = null,
    val weight: Int? = null,
    val assignment: String? = null,
    val inheritedFrom: String? = null,
    val contexts: List<String> = emptyList(),
    val expiresAt: String? = null,
    val permissions: List<PermissionNodeDto> = emptyList(),
)

@Serializable
data class PermissionNodeDto(
    val id: Int = 0,
    val nodeKey: String = "",
    val value: Boolean = false,
    val contextLabel: String? = null,
    val expiresAt: String? = null,
)

@Serializable
data class PermissionInsightsResponse(
    val expiring: List<PermissionNodeDto> = emptyList(),
    val recentChanges: List<PermissionInsightEventDto> = emptyList(),
)

@Serializable
data class PermissionInsightEventDto(
    val action: String = "",
    val target: String = "",
    val permission: String = "",
    val timestamp: String? = null,
)
