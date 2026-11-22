package net.igng.mcstatus.core.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import net.igng.mcstatus.core.auth.AuthRepository
import net.igng.mcstatus.core.model.SiteEndpoints
import net.igng.mcstatus.core.network.ApiClient
import net.igng.mcstatus.core.network.CredentialEnvelopeSeal
import okhttp3.HttpUrl.Companion.toHttpUrl

/** Atomic JSON file cache for offline Wiki pages, server catalog snapshots, and ticket drafts. */
class LocalJsonCache(context: Context, folderName: String) {
    private val dir = File(context.applicationContext.filesDir, folderName).apply { mkdirs() }

    suspend fun write(key: String, raw: String) = withContext(Dispatchers.IO) {
        val safe = key.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val tmp = File(dir, "$safe.tmp")
        val target = File(dir, "$safe.json")
        tmp.writeText(raw, Charsets.UTF_8)
        if (!tmp.renameTo(target)) {
            target.writeText(raw, Charsets.UTF_8)
            tmp.delete()
        }
    }

    suspend fun read(key: String): String? = withContext(Dispatchers.IO) {
        val safe = key.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val file = File(dir, "$safe.json")
        if (file.exists()) runCatching { file.readText(Charsets.UTF_8) }.getOrNull() else null
    }

    suspend fun delete(key: String) = withContext(Dispatchers.IO) {
        val safe = key.replace(Regex("[^A-Za-z0-9._-]"), "_")
        File(dir, "$safe.json").delete()
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        dir.listFiles()?.forEach { it.delete() }
    }

    suspend fun sizeBytes(): Long = withContext(Dispatchers.IO) {
        dir.listFiles()?.sumOf { it.length() } ?: 0L
    }
}

class ExploreRepository(
    private val endpoints: SiteEndpoints,
    private val api: ApiClient,
    private val cache: LocalJsonCache,
    private val json: Json = api.json,
) {
    suspend fun fetchHomeFamilies(): List<ServerFamilyDto> {
        val raw = api.get("${endpoints.mc}/api/home/servers")
        cache.write("home_families", raw)
        return json.decodeFromString<HomeServersResponse>(raw).families
    }

    suspend fun readCachedFamilies(): List<ServerFamilyDto> =
        cache.read("home_families")?.let { raw ->
            runCatching { json.decodeFromString<HomeServersResponse>(raw).families }.getOrNull()
        }.orEmpty()

    suspend fun fetchMotd(serverId: Int): MotdResponse {
        val url = "${endpoints.mc}/api/motd".toHttpUrl().newBuilder()
            .addQueryParameter("serverId", serverId.toString())
            .build().toString()
        return json.decodeFromString(api.get(url))
    }

    suspend fun search(query: String): List<SearchResultItem> {
        val trimmed = query.trim()
        if (trimmed.length < 2) return emptyList()
        val url = "${endpoints.mc}/api/search".toHttpUrl().newBuilder()
            .addQueryParameter("q", trimmed)
            .build().toString()
        return json.decodeFromString<GlobalSearchResponse>(api.get(url)).results
    }

    suspend fun fetchPlayerProfile(igngId: Int): PublicPlayerProfileResponse =
        json.decodeFromString(api.get("${endpoints.mc}/api/profile/$igngId"))

    suspend fun fetchPlayerProfileStats(igngId: Int): VanillaStatsResponse =
        json.decodeFromString(api.get("${endpoints.mc}/api/profile/$igngId/stats"))

    suspend fun fetchPlayerProfileMedals(igngId: Int): MedalProfileDto =
        json.decodeFromString(api.get("${endpoints.mc}/api/profile/$igngId/medals"))

    suspend fun fetchMcCharacter(username: String): McCharacterProfileResponse =
        json.decodeFromString(api.get("${endpoints.mc}/api/profile/mc/${Uri.encode(username)}"))

    suspend fun fetchPublicLands(): List<HallLandDto> =
        json.decodeFromString<HallLandsResponse>(api.get("${endpoints.mc}/api/hall/lands")).lands

    suspend fun fetchPublicFakePlayers(): List<HallFakePlayerDto> =
        json.decodeFromString<HallFakePlayersResponse>(api.get("${endpoints.mc}/api/hall/fake-players")).fakePlayers

    suspend fun fetchPublicTickets(query: String = "", includeClosed: Boolean = true, page: Int = 1): TicketListResponse {
        val url = "${endpoints.mc}/api/hall/tickets".toHttpUrl().newBuilder()
            .addQueryParameter("page", page.coerceAtLeast(1).toString())
            .addQueryParameter("limit", "24")
            .addQueryParameter("all", if (includeClosed) "1" else "0")
            .apply { if (query.isNotBlank()) addQueryParameter("q", query.trim()) }
            .build().toString()
        return json.decodeFromString(api.get(url))
    }

    suspend fun fetchPublicTicketDetail(id: Int): TicketDetailDto =
        json.decodeFromString<TicketDetailResponse>(api.get("${endpoints.mc}/api/hall/tickets/$id")).ticket

    suspend fun fetchPublicPerfectRoster(): PublicPerfectRosterResponse =
        json.decodeFromString(api.get("${endpoints.mc}/api/perfect"))
}

class StatusRepository(
    private val endpoints: SiteEndpoints,
    private val api: ApiClient,
    private val json: Json = api.json,
) {
    suspend fun fetchPublicStatus(): PublicStatusResponse =
        json.decodeFromString(api.get("${endpoints.mc}/api/status/public"))

    suspend fun fetchPublicHistory(
        preset: StatusRangePreset,
        serverId: Int? = null,
    ): PublicStatusHistoryResponse {
        val end = Instant.now()
        val start = end.minus(preset.hours, ChronoUnit.HOURS)
        val url = "${endpoints.mc}/api/status/public/history".toHttpUrl().newBuilder()
            .addQueryParameter("start", start.toString())
            .addQueryParameter("end", end.toString())
            .apply { if (serverId != null) addQueryParameter("serverId", serverId.toString()) }
            .build().toString()
        return json.decodeFromString(api.get(url))
    }
}

class ChatRepository(
    private val endpoints: SiteEndpoints,
    private val api: ApiClient,
    private val json: Json = api.json,
) {
    suspend fun fetchSources(): ChatSourcesResponse =
        json.decodeFromString(api.get("${endpoints.chat}/api/chatlogs/sources"))

    suspend fun fetchMessages(
        source: String,
        serverId: Int? = null,
        groupId: String? = null,
        query: String = "",
        senderId: String = "",
        atAll: Boolean = false,
        kind: String = "",
        beforeId: String? = null,
        limit: Int = 80,
    ): ChatLogsResponse {
        val url = "${endpoints.chat}/api/chatlogs".toHttpUrl().newBuilder()
            .addQueryParameter("source", source)
            .addQueryParameter("limit", limit.coerceIn(1, 200).toString())
            .apply {
                if (source == "qq" && !groupId.isNullOrBlank()) addQueryParameter("groupId", groupId)
                if (source != "qq" && serverId != null) addQueryParameter("serverId", serverId.toString())
                if (query.isNotBlank()) addQueryParameter("q", query.trim())
                if (senderId.isNotBlank()) addQueryParameter("senderId", senderId.trim())
                if (source == "qq" && atAll) addQueryParameter("atAll", "1")
                if (kind.isNotBlank()) addQueryParameter("kind", kind)
                if (!beforeId.isNullOrBlank()) addQueryParameter("beforeId", beforeId)
            }
            .build().toString()
        return json.decodeFromString(api.get(url))
    }

    suspend fun fetchForwardDetail(messageId: String): ChatForwardResponse {
        val url = "${endpoints.chat}/api/chatlogs/forward".toHttpUrl().newBuilder()
            .addQueryParameter("messageId", messageId)
            .build().toString()
        return json.decodeFromString(api.get(url))
    }
}

class WikiRepository(
    private val endpoints: SiteEndpoints,
    private val api: ApiClient,
    private val cache: LocalJsonCache,
    private val json: Json = api.json,
) {
    suspend fun fetchCatalog(): WikiCatalogResponse {
        val raw = api.get("${endpoints.wiki}/api/wiki/catalog?kb=IGNGmc")
        cache.write("wiki_catalog", raw)
        return json.decodeFromString(raw)
    }

    suspend fun readCachedCatalog(): WikiCatalogResponse? =
        cache.read("wiki_catalog")?.let { raw ->
            runCatching { json.decodeFromString<WikiCatalogResponse>(raw) }.getOrNull()
        }

    suspend fun fetchPage(pageId: Int): WikiPageDetail {
        val raw = api.get("${endpoints.wiki}/api/wiki/page?kb=IGNGmc&pageId=$pageId")
        cache.write("wiki_page_$pageId", raw)
        return json.decodeFromString<WikiPageResponse>(raw).page
    }

    suspend fun readCachedPage(pageId: Int): WikiPageDetail? =
        cache.read("wiki_page_$pageId")?.let { raw ->
            runCatching { json.decodeFromString<WikiPageResponse>(raw).page }.getOrNull()
        }
}

class PlayerRepository(
    private val endpoints: SiteEndpoints,
    private val api: ApiClient,
    private val authRepository: AuthRepository,
    private val json: Json = api.json,
) {
    suspend fun fetchBoundAccounts(): List<BoundMcAccountDto> =
        json.decodeFromString<BoundMcAccountsResponse>(api.get("${endpoints.dash}/api/mc/accounts")).accounts

    suspend fun fetchAccountDetails(username: String, page: Int = 1): McCharacterDetailsResponse {
        val url = "${endpoints.dash}/api/mc/details".toHttpUrl().newBuilder()
            .addQueryParameter("username", username)
            .addQueryParameter("page", page.coerceAtLeast(1).toString())
            .build().toString()
        return json.decodeFromString(api.get(url))
    }

    suspend fun toggleAccountPublic(mcUsername: String, isPublic: Boolean): McActionResult {
        val body = buildJsonObject {
            put("action", "toggle-public")
            put("mcUsername", mcUsername)
            put("isPublic", isPublic)
        }
        return json.decodeFromString(api.post("${endpoints.dash}/api/mc/manage", body.toString()))
    }

    suspend fun unbindAccount(mcUsername: String): McActionResult {
        val body = buildJsonObject {
            put("action", "unbind")
            put("mcUsername", mcUsername)
        }
        return json.decodeFromString(api.post("${endpoints.dash}/api/mc/manage", body.toString()))
    }

    /**
     * Step-up encrypted game password reset matching `apps/dash/app/api/mc/manage/route.js`.
     */
    suspend fun resetGamePassword(
        mcUsername: String,
        currentCredential: String,
        nextCredential: String,
    ): McActionResult {
        val challenge = authRepository.fetchDashCryptoChallenge()
        val sealed = CredentialEnvelopeSeal.seal(
            challenge = challenge,
            fields = mapOf(
                "oldPassword" to currentCredential,
                "newPassword" to nextCredential,
            ),
        )
        val body = buildJsonObject {
            put("action", "reset-password")
            put("mcUsername", mcUsername)
            put("credential", json.encodeToJsonElement(net.igng.mcstatus.core.network.SealedCredential.serializer(), sealed))
        }
        return json.decodeFromString(api.post("${endpoints.dash}/api/mc/manage", body.toString()))
    }

    suspend fun fetchLands(serverId: String = "all", keyword: String = "", page: Int = 1): PlayerLandsResponse {
        val url = "${endpoints.dash}/api/mc/lands".toHttpUrl().newBuilder()
            .addQueryParameter("view", "page")
            .addQueryParameter("page", page.coerceAtLeast(1).toString())
            .addQueryParameter("limit", "30")
            .apply {
                if (serverId != "all" && serverId.isNotBlank()) addQueryParameter("server_id", serverId)
                if (keyword.isNotBlank()) addQueryParameter("q", keyword.trim())
            }
            .build().toString()
        return json.decodeFromString(api.get(url))
    }

    suspend fun updateLand(landId: Int, isPublic: Boolean? = null, description: String? = null): McActionResult {
        val action = if (isPublic != null) "toggle-public" else "update-description"
        val body = buildJsonObject {
            put("landId", landId)
            put("action", action)
            isPublic?.let { put("isPublic", it) }
            description?.let { put("description", it) }
        }
        return json.decodeFromString(api.post("${endpoints.dash}/api/mc/lands/manage", body.toString()))
    }

    suspend fun fetchTeleports(): List<PlayerTeleportDto> =
        json.decodeFromString<PlayerTeleportsResponse>(api.get("${endpoints.dash}/api/mc/teleports")).records

    suspend fun fetchFakePlayers(): List<PlayerFakePlayerDto> =
        json.decodeFromString<PlayerFakePlayersResponse>(api.get("${endpoints.dash}/api/mc/fake-players")).records

    suspend fun fetchLogins(): List<PlayerLoginRecordDto> =
        json.decodeFromString<PlayerLoginsResponse>(api.get("${endpoints.dash}/api/mc/logins")).records

    suspend fun fetchVaultOverview(): VaultOverviewResponse =
        json.decodeFromString(api.get("${endpoints.dash}/api/mc/vault/overview"))

    suspend fun fetchVaultItems(vaultId: String, query: String = "", page: Int = 1): VaultItemsResponse {
        val url = "${endpoints.dash}/api/mc/vault/items".toHttpUrl().newBuilder()
            .addQueryParameter("vault_id", vaultId)
            .addQueryParameter("page", page.coerceAtLeast(1).toString())
            .addQueryParameter("pageSize", "30")
            .apply { if (query.isNotBlank()) addQueryParameter("q", query.trim()) }
            .build().toString()
        return json.decodeFromString(api.get(url))
    }

    suspend fun fetchVaultHistory(vaultId: String = "", page: Int = 1): VaultHistoryResponse {
        val url = "${endpoints.dash}/api/mc/vault/history".toHttpUrl().newBuilder()
            .addQueryParameter("page", page.coerceAtLeast(1).toString())
            .addQueryParameter("pageSize", "20")
            .apply { if (vaultId.isNotBlank()) addQueryParameter("vault_id", vaultId) }
            .build().toString()
        return json.decodeFromString(api.get(url))
    }

    suspend fun fetchVaultTrace(operationId: String): VaultTraceResponse {
        val url = "${endpoints.dash}/api/mc/vault/trace".toHttpUrl().newBuilder()
            .addQueryParameter("operation_id", operationId)
            .build().toString()
        return json.decodeFromString(api.get(url))
    }

    suspend fun fetchVanillaStats(serverId: Int? = null, character: String? = null): VanillaStatsResponse {
        val url = "${endpoints.dash}/api/mc/stats".toHttpUrl().newBuilder()
            .apply {
                if (serverId != null) addQueryParameter("serverId", serverId.toString())
                if (!character.isNullOrBlank()) addQueryParameter("character", character)
            }
            .build().toString()
        return json.decodeFromString(api.get(url))
    }

    suspend fun fetchMedals(): MedalProfileDto =
        json.decodeFromString(api.get("${endpoints.dash}/api/mc/medals"))

    suspend fun renameCustomMedal(mcUsername: String, customName: String): McActionResult {
        val body = buildJsonObject {
            put("mcUsername", mcUsername)
            put("customName", customName)
        }
        return json.decodeFromString(api.patch("${endpoints.dash}/api/mc/medals/custom", body.toString()))
    }

    suspend fun fetchPersonalPerfect(): PersonalPerfectResponse =
        json.decodeFromString(api.get("${endpoints.dash}/api/mc/perfect"))

    suspend fun fetchPermissions(): PlayerPermissionsResponse =
        json.decodeFromString(api.get("${endpoints.dash}/api/mc/permissions"))

    suspend fun fetchPermissionInsights(): PermissionInsightsResponse =
        json.decodeFromString(api.get("${endpoints.dash}/api/mc/permissions/insights"))
}

class TicketRepository(
    private val endpoints: SiteEndpoints,
    private val api: ApiClient,
    private val draftCache: LocalJsonCache,
    private val json: Json = api.json,
) {
    suspend fun fetchList(
        relation: String = "all",
        includeClosed: Boolean = false,
        query: String = "",
        page: Int = 1,
    ): TicketListResponse {
        val url = "${endpoints.dash}/api/mc/tickets".toHttpUrl().newBuilder()
            .addQueryParameter("relation", relation)
            .addQueryParameter("page", page.coerceAtLeast(1).toString())
            .addQueryParameter("limit", "25")
            .apply {
                if (includeClosed) addQueryParameter("all", "1")
                if (query.isNotBlank()) addQueryParameter("q", query.trim())
            }
            .build().toString()
        return json.decodeFromString(api.get(url))
    }

    suspend fun fetchOptions(): TicketOptionsResponse =
        json.decodeFromString(api.get("${endpoints.dash}/api/mc/tickets/options"))

    suspend fun searchPlayers(query: String): List<TicketPlayerOption> {
        if (query.trim().length < 2) return emptyList()
        val url = "${endpoints.dash}/api/mc/tickets/players".toHttpUrl().newBuilder()
            .addQueryParameter("q", query.trim())
            .build().toString()
        return json.decodeFromString<TicketPlayersResponse>(api.get(url)).players
    }

    suspend fun fetchDetail(id: Int): TicketDetailDto =
        json.decodeFromString<TicketDetailResponse>(api.get("${endpoints.dash}/api/mc/tickets/$id")).ticket

    suspend fun createTicket(payload: CreateTicketPayload): Int {
        val raw = api.post(
            "${endpoints.dash}/api/mc/tickets",
            json.encodeToString(CreateTicketPayload.serializer(), payload),
        )
        val obj = json.parseToJsonElement(raw).jsonObject
        val id = obj["id"]?.jsonPrimitive?.intOrNull
            ?: obj["ticket"]?.jsonObject?.get("id")?.jsonPrimitive?.intOrNull
            ?: 0
        clearDraft()
        return id
    }

    suspend fun updateTicket(id: Int, payload: UpdateTicketPayload): TicketDetailDto =
        json.decodeFromString<TicketDetailResponse>(
            api.patch(
                "${endpoints.dash}/api/mc/tickets/$id",
                json.encodeToString(UpdateTicketPayload.serializer(), payload),
            ),
        ).ticket

    suspend fun reply(id: Int, content: String, attachmentMediaIds: List<String> = emptyList()) {
        val body = buildJsonObject {
            put("content", content)
            if (attachmentMediaIds.isNotEmpty()) {
                put("attachments", buildJsonArray { attachmentMediaIds.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } })
            }
        }
        api.post("${endpoints.dash}/api/mc/tickets/$id/replies", body.toString())
    }

    suspend fun close(id: Int) {
        api.post("${endpoints.dash}/api/mc/tickets/$id/close", "{}")
    }

    suspend fun reopen(id: Int) {
        api.post("${endpoints.dash}/api/mc/tickets/$id/reopen", "{}")
    }

    suspend fun uploadAttachment(fileName: String, mimeType: String, bytes: ByteArray): UploadedMediaDto {
        val raw = api.uploadImage("${endpoints.dash}/api/mc/media/upload", fileName, mimeType, bytes)
        return json.decodeFromString<UploadedMediaResponse>(raw).media
            ?: error("上传成功但未返回媒体编号")
    }

    suspend fun saveDraft(draft: TicketDraft) {
        draftCache.write("ticket_new_draft", json.encodeToString(TicketDraft.serializer(), draft))
    }

    suspend fun readDraft(): TicketDraft? =
        draftCache.read("ticket_new_draft")?.let { raw ->
            runCatching { json.decodeFromString<TicketDraft>(raw) }.getOrNull()
        }

    suspend fun clearDraft() {
        draftCache.delete("ticket_new_draft")
    }
}

class AdminRepository(
    private val endpoints: SiteEndpoints,
    private val api: ApiClient,
    private val json: Json = api.json,
) {
    suspend fun fetchTicketOverview(): AdminTicketOverviewResponse =
        json.decodeFromString(api.get("${endpoints.dash}/api/mc/admin/tickets/overview"))

    suspend fun fetchAdminTickets(
        includeClosed: Boolean = false,
        query: String = "",
        page: Int = 1,
    ): TicketListResponse {
        val url = "${endpoints.dash}/api/mc/admin/tickets".toHttpUrl().newBuilder()
            .addQueryParameter("page", page.coerceAtLeast(1).toString())
            .addQueryParameter("pageSize", "25")
            .apply {
                if (includeClosed) addQueryParameter("all", "1")
                if (query.isNotBlank()) addQueryParameter("q", query.trim())
            }
            .build().toString()
        return json.decodeFromString(api.get(url))
    }

    suspend fun fetchAdminTicketDetail(id: Int): TicketDetailDto =
        json.decodeFromString<TicketDetailResponse>(api.get("${endpoints.dash}/api/mc/admin/tickets/$id")).ticket

    suspend fun replyAdminTicket(id: Int, content: String) {
        val body = buildJsonObject { put("content", content) }
        api.post("${endpoints.dash}/api/mc/admin/tickets/$id/replies", body.toString())
    }

    suspend fun closeAdminTicket(id: Int) {
        api.post("${endpoints.dash}/api/mc/admin/tickets/$id/close", "{}")
    }

    suspend fun reopenAdminTicket(id: Int) {
        api.post("${endpoints.dash}/api/mc/admin/tickets/$id/reopen", "{}")
    }

    suspend fun searchAdminPlayers(query: String): List<AdminPlayerResultDto> {
        val url = "${endpoints.dash}/api/mc/admin/players/search".toHttpUrl().newBuilder()
            .addQueryParameter("q", query.trim())
            .build().toString()
        return json.decodeFromString<AdminPlayerSearchResponse>(api.get(url)).allResults
    }

    suspend fun fetchRecentPunishments(search: String = "", page: Int = 1): AdminPunishmentsResponse {
        val url = "${endpoints.dash}/api/mc/admin/punishments/records".toHttpUrl().newBuilder()
            .addQueryParameter("page", page.coerceAtLeast(1).toString())
            .addQueryParameter("pageSize", "25")
            .apply { if (search.isNotBlank()) addQueryParameter("search", search.trim()) }
            .build().toString()
        return json.decodeFromString(api.get(url))
    }

    suspend fun fetchHistorianReports(): HistorianListResponse =
        json.decodeFromString(api.get("${endpoints.dash}/api/mc/admin/historian"))

    suspend fun fetchHistorianDetail(id: String): HistorianDetailResponse =
        json.decodeFromString(api.get("${endpoints.dash}/api/mc/admin/historian/${Uri.encode(id)}"))

    suspend fun fetchAdminCurrentNodes(): AdminCurrentNodesResponse =
        json.decodeFromString(api.get("${endpoints.mc}/api/status/current"))

    suspend fun fetchAdminTraffic(rangeHours: Long = 24, ip: String = ""): TrafficAdminResponse {
        val end = Instant.now()
        val start = if (rangeHours == 0L) {
            LocalDate.now(ZoneId.systemDefault()).atStartOfDay(ZoneId.systemDefault()).toInstant()
        } else {
            end.minus(rangeHours, ChronoUnit.HOURS)
        }
        val url = "${endpoints.dash}/api/mc/status/traffic/admin".toHttpUrl().newBuilder()
            .addQueryParameter("start", start.toString())
            .addQueryParameter("end", end.toString())
            .addQueryParameter("source", "backend")
            .addQueryParameter("limit", "25")
            .apply { if (ip.isNotBlank()) addQueryParameter("ip", ip.trim()) }
            .build().toString()
        return json.decodeFromString(api.get(url))
    }
}
