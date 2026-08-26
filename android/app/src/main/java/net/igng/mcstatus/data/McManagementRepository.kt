package net.igng.mcstatus.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import okhttp3.HttpUrl.Companion.toHttpUrl

enum class McDataKind(val id: String, val label: String) {
    LANDS("lands", "我的领地"),
    TELEPORTS("teleports", "传送点"),
    LOGINS("logins", "登录记录"),
    FAKE_PLAYERS("fake-players", "假人列表"),
}

@Serializable
data class McAccountsResponse(
    val success: Boolean = false,
    val accounts: List<McAccount> = emptyList(),
)

@Serializable
data class McAccount(
    val id: Int = 0,
    val mc_username: String = "",
    val is_public: JsonElement? = null,
    val created_at: String? = null,
    val is_banned: JsonElement? = null,
    val is_muted: JsonElement? = null,
)

@Serializable
data class McServersResponse(
    val success: Boolean = false,
    val servers: List<McServer> = emptyList(),
)

@Serializable
data class McServer(
    val server_id: Int = 0,
    val server_name: String = "",
    val land_providers: List<String> = emptyList(),
)

@Serializable
data class McLandsResponse(
    val success: Boolean = false,
    val records: List<McLandRecord> = emptyList(),
)

@Serializable
data class McLandRecord(
    val id: Int = 0,
    val server_id: Int = 0,
    val server_name: String? = null,
    val plugin_type: String? = null,
    val owner_uuid: String? = null,
    val owner_name: String? = null,
    val land_name: String? = null,
    val world: String? = null,
    val x: Double? = null,
    val y: Double? = null,
    val z: Double? = null,
    val size: McLandSize? = null,
    val corners: List<McCoordinate> = emptyList(),
    val guest_flags: Map<String, JsonElement> = emptyMap(),
    val environment_flags: Map<String, JsonElement> = emptyMap(),
    val members: List<McLandMember> = emptyList(),
    val viewer_member: McLandMember? = null,
    val viewer_role: String? = null,
    val can_edit: Boolean = false,
    val is_public: Boolean = false,
    val description: String = "",
    val last_updated: String? = null,
)

@Serializable
data class McLandSize(
    val width: Double? = null,
    val height: Double? = null,
    val length: Double? = null,
)

@Serializable
data class McCoordinate(
    val x: Double? = null,
    val y: Double? = null,
    val z: Double? = null,
)

@Serializable
data class McLandMember(
    val uuid: String? = null,
    val name: String? = null,
    val permissions: Map<String, JsonElement> = emptyMap(),
)

@Serializable
data class McTeleportsResponse(
    val success: Boolean = false,
    val records: List<McTeleportRecord> = emptyList(),
)

@Serializable
data class McTeleportRecord(
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
data class McLoginsResponse(
    val success: Boolean = false,
    val records: List<McLoginRecord> = emptyList(),
)

@Serializable
data class McLoginRecord(
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

@Serializable
data class McFakePlayersResponse(
    val success: Boolean = false,
    val records: List<McFakePlayerRecord> = emptyList(),
)

@Serializable
data class McFakePlayerRecord(
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
data class McPermissionsResponse(
    val success: Boolean = false,
    val source: String? = null,
    val accounts: List<McPermissionAccount> = emptyList(),
    val summary: McPermissionSummary = McPermissionSummary(),
)

@Serializable
data class McPermissionAccount(
    val id: Int = 0,
    val username: String = "",
    val uuid: String? = null,
    val isPublic: Boolean = false,
    val createdAt: String? = null,
    val identities: List<McPermissionIdentity> = emptyList(),
)

@Serializable
data class McPermissionIdentity(
    val serverId: Int? = null,
    val username: String? = null,
    val uuid: String? = null,
    val found: Boolean = false,
    val groups: List<McPermissionGroup> = emptyList(),
    val independentPermissions: List<McPermissionNode> = emptyList(),
    val summary: McIdentityPermissionSummary = McIdentityPermissionSummary(),
)

@Serializable
data class McPermissionGroup(
    val name: String = "",
    val displayName: String? = null,
    val weight: Int? = null,
    val assignment: String? = null,
    val inheritedFrom: String? = null,
    val contexts: List<String> = emptyList(),
    val expiresAt: String? = null,
    val permissions: List<McPermissionNode> = emptyList(),
)

@Serializable
data class McPermissionNode(
    val id: Int = 0,
    val nodeKey: String = "",
    val value: Boolean = false,
    val effect: String? = null,
    val contexts: String? = null,
    val contextLabel: String? = null,
    val expiresAt: String? = null,
)

@Serializable
data class McPermissionSummary(
    val accountCount: Int = 0,
    val identityCount: Int = 0,
    val groupCount: Int = 0,
    val directGroupCount: Int = 0,
    val inheritedGroupCount: Int = 0,
    val groupPermissionCount: Int = 0,
    val independentPermissionCount: Int = 0,
    val permissionNodeCount: Int = 0,
)

@Serializable
data class McIdentityPermissionSummary(
    val groupCount: Int = 0,
    val directGroupCount: Int = 0,
    val inheritedGroupCount: Int = 0,
    val groupPermissionCount: Int = 0,
    val independentPermissionCount: Int = 0,
    val permissionNodeCount: Int = 0,
)

@Serializable
data class McAccountDetailsResponse(
    val success: Boolean = false,
    val authme: McAuthmeInfo? = null,
    val activePenalty: McActivePenalty? = null,
    val history: List<McPunishmentRecord> = emptyList(),
    val totalHistory: Int = 0,
    val page: Int = 1,
    val totalPages: Int = 0,
)

@Serializable
data class McAuthmeInfo(
    val lastlogin: Long? = null,
    val ip: String? = null,
)

@Serializable
data class McActivePenalty(
    val ban: McPunishmentRecord? = null,
    val mute: McPunishmentRecord? = null,
)

@Serializable
data class McPunishmentRecord(
    val type: String? = null,
    val reason: String? = null,
    val banned_by_name: String? = null,
    val time: Long? = null,
    val until: Long? = null,
    val active: JsonElement? = null,
)

@Serializable
data class McActionResponse(
    val success: Boolean = false,
    val message: String? = null,
    val isPublic: Boolean? = null,
    val description: String? = null,
)

sealed class McRecords {
    data class Lands(val records: List<McLandRecord>) : McRecords()
    data class Teleports(val records: List<McTeleportRecord>) : McRecords()
    data class Logins(val records: List<McLoginRecord>) : McRecords()
    data class FakePlayers(val records: List<McFakePlayerRecord>) : McRecords()
}

class McManagementRepository(
    private val baseUrl: String,
    private val client: ApiClient = ApiClient(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    },
) {
    private val root = baseUrl.trimEnd('/')

    suspend fun fetchAccounts(token: String): List<McAccount> =
        decode<McAccountsResponse>(client.get("$root/api/mc/accounts", token)).accounts

    suspend fun fetchServers(token: String): List<McServer> =
        decode<McServersResponse>(client.get("$root/api/mc/servers", token)).servers

    suspend fun fetchRecords(
        token: String,
        kind: McDataKind,
        username: String = "all",
        serverId: String = "all",
    ): McRecords {
        val url = "$root/api/mc/${kind.id}".toHttpUrl().newBuilder()
            .addQueryParameter("server_id", serverId.ifBlank { "all" })
            .apply {
                // The site deliberately does not apply the account filter to lands:
                // a member must still see a land owned by another bound account.
                if (kind != McDataKind.LANDS && username != "all" && username.isNotBlank()) {
                    addQueryParameter("username", username)
                }
            }
            .build()
            .toString()

        return when (kind) {
            McDataKind.LANDS -> McRecords.Lands(decode<McLandsResponse>(client.get(url, token)).records)
            McDataKind.TELEPORTS -> McRecords.Teleports(decode<McTeleportsResponse>(client.get(url, token)).records)
            McDataKind.LOGINS -> McRecords.Logins(decode<McLoginsResponse>(client.get(url, token)).records)
            McDataKind.FAKE_PLAYERS -> McRecords.FakePlayers(decode<McFakePlayersResponse>(client.get(url, token)).records)
        }
    }

    suspend fun fetchPermissions(token: String): McPermissionsResponse =
        decode(client.get("$root/api/mc/permissions", token))

    suspend fun fetchAccountDetails(token: String, username: String, page: Int = 1): McAccountDetailsResponse {
        val url = "$root/api/mc/details".toHttpUrl().newBuilder()
            .addQueryParameter("username", username)
            .addQueryParameter("page", page.coerceAtLeast(1).toString())
            .build()
            .toString()
        return decode(client.get(url, token))
    }

    suspend fun manageAccount(
        token: String,
        username: String,
        action: String,
        newPassword: String? = null,
        isPublic: Boolean? = null,
    ): McActionResponse {
        val body = buildJsonObject {
            put("mcUsername", username)
            put("action", action)
            newPassword?.let { put("newPassword", it) }
            isPublic?.let { put("isPublic", it) }
        }
        return decode(client.post("$root/api/mc/manage", body.toString(), token))
    }

    suspend fun manageLand(
        token: String,
        landId: Int,
        action: String,
        isPublic: Boolean? = null,
        description: String? = null,
    ): McActionResponse {
        val body = buildJsonObject {
            put("landId", landId)
            put("action", action)
            isPublic?.let { put("isPublic", it) }
            description?.let { put("description", it) }
        }
        return decode(client.post("$root/api/mc/lands/manage", body.toString(), token))
    }

    private inline fun <reified T> decode(payload: String): T = json.decodeFromString(payload)
}

fun JsonElement?.asBoolean(): Boolean {
    val primitive = this as? JsonPrimitive ?: return false
    primitive.booleanOrNull?.let { return it }
    return when (primitive.contentOrNull?.lowercase()) {
        "1", "true", "yes", "on" -> true
        else -> false
    }
}
