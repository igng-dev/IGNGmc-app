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
import kotlinx.serialization.json.put
import net.igng.mcstatus.core.auth.AuthRepository
import net.igng.mcstatus.core.model.SiteEndpoints
import net.igng.mcstatus.core.model.V1Id
import net.igng.mcstatus.core.network.ApiClient
import net.igng.mcstatus.core.network.CredentialEnvelopeSeal
import net.igng.mcstatus.core.network.SealedCredential
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

/**
 * All API traffic in `ExploreRepository` hits the unified v1 gateway (`${endpoints.api}/api/v1`).
 */
class ExploreRepository(
    private val endpoints: SiteEndpoints,
    private val api: ApiClient,
    private val cache: LocalJsonCache,
    private val json: Json = api.json,
) {
    suspend fun fetchHomeFamilies(): List<ServerFamilyDto> {
        val raw = api.get("${endpoints.api}/api/v1/mc/servers")
        cache.write("home_families_v1", raw)
        return json.decodeFromString<HomeServersResponse>(raw).families
    }

    suspend fun readCachedFamilies(): List<ServerFamilyDto> =
        cache.read("home_families_v1")?.let { raw ->
            runCatching { json.decodeFromString<HomeServersResponse>(raw).families }.getOrNull()
        }.orEmpty()

    suspend fun fetchMotd(serverId: Int): MotdResponse {
        val sid = V1Id.format("srv", serverId)
        return json.decodeFromString(api.get("${endpoints.api}/api/v1/mc/servers/$sid/motd"))
    }

    suspend fun search(query: String): List<SearchResultItem> {
        val trimmed = query.trim()
        if (trimmed.length < 2) return emptyList()
        val url = "${endpoints.api}/api/v1/mc/search".toHttpUrl().newBuilder()
            .addQueryParameter("q", trimmed)
            .build().toString()
        return json.decodeFromString<GlobalSearchResponse>(api.get(url)).results
    }

    suspend fun fetchPlayerProfile(igngId: Int): PublicPlayerProfileResponse =
        json.decodeFromString(api.get("${endpoints.api}/api/v1/users/${V1Id.format("usr", igngId)}/mc-profile"))

    suspend fun fetchPlayerProfileStats(igngId: Int): VanillaStatsResponse =
        json.decodeFromString(api.get("${endpoints.api}/api/v1/users/${V1Id.format("usr", igngId)}/mc-stats"))

    suspend fun fetchPlayerProfileMedals(igngId: Int): MedalProfileDto =
        json.decodeFromString(api.get("${endpoints.api}/api/v1/users/${V1Id.format("usr", igngId)}/mc-medals"))

    suspend fun fetchMcCharacter(username: String): McCharacterProfileResponse =
        json.decodeFromString(api.get("${endpoints.api}/api/v1/mc/characters/${Uri.encode(username)}"))

    suspend fun fetchPublicLands(): List<HallLandDto> =
        json.decodeFromString<HallLandsResponse>(api.get("${endpoints.api}/api/v1/mc/hall/lands")).data

    suspend fun fetchPublicFakePlayers(): List<HallFakePlayerDto> =
        json.decodeFromString<HallFakePlayersResponse>(api.get("${endpoints.api}/api/v1/mc/hall/fake-players")).data

    suspend fun fetchPublicTickets(query: String = "", includeClosed: Boolean = true, page: Int = 1): TicketListResponse {
        val url = "${endpoints.api}/api/v1/mc/hall/tickets".toHttpUrl().newBuilder()
            .addQueryParameter("page", page.coerceAtLeast(1).toString())
            .addQueryParameter("page_size", "24")
            .addQueryParameter("include_closed", includeClosed.toString())
            .apply { if (query.isNotBlank()) addQueryParameter("q", query.trim()) }
            .build().toString()
        return json.decodeFromString(api.get(url))
    }

    suspend fun fetchPublicTicketDetail(id: Int): TicketDetailDto =
        json.decodeFromString(api.get("${endpoints.api}/api/v1/mc/hall/tickets/${V1Id.format("tk", id)}"))

    suspend fun fetchPublicPerfectRoster(): PublicPerfectRosterResponse =
        json.decodeFromString(api.get("${endpoints.api}/api/v1/mc/perfect"))
}

class StatusRepository(
    private val endpoints: SiteEndpoints,
    private val api: ApiClient,
    private val json: Json = api.json,
) {
    suspend fun fetchPublicStatus(): PublicStatusResponse =
        json.decodeFromString(api.get("${endpoints.api}/api/v1/mc/status"))

    suspend fun fetchPublicHistory(
        preset: StatusRangePreset,
        serverId: Int? = null,
    ): PublicStatusHistoryResponse {
        val end = Instant.now()
        val start = end.minus(preset.hours, ChronoUnit.HOURS)
        val url = "${endpoints.api}/api/v1/mc/status/history".toHttpUrl().newBuilder()
            .addQueryParameter("start", start.toString())
            .addQueryParameter("end", end.toString())
            .apply { if (serverId != null) addQueryParameter("server_id", V1Id.format("srv", serverId)) }
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
        json.decodeFromString(api.get("${endpoints.api}/api/v1/chat/sources"))

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
        val v1Source = if (source == "qq") "qq" else "server"
        val url = "${endpoints.api}/api/v1/chat/messages".toHttpUrl().newBuilder()
            .addQueryParameter("source", v1Source)
            .addQueryParameter("limit", limit.coerceIn(1, 200).toString())
            .apply {
                if (v1Source == "qq" && !groupId.isNullOrBlank()) addQueryParameter("group_id", groupId)
                if (v1Source == "server" && serverId != null) addQueryParameter("server_id", V1Id.format("srv", serverId))
                if (query.isNotBlank()) addQueryParameter("q", query.trim())
                if (senderId.isNotBlank()) addQueryParameter("sender_id", senderId.trim())
                if (v1Source == "qq" && atAll) addQueryParameter("at_all", "true")
                if (v1Source == "qq" && kind.isNotBlank()) addQueryParameter("kind", kind)
                if (!beforeId.isNullOrBlank()) addQueryParameter("before_id", beforeId)
            }
            .build().toString()
        return json.decodeFromString(api.get(url))
    }

    suspend fun fetchForwardDetail(messageId: String): ChatForwardResponse =
        json.decodeFromString(api.get("${endpoints.api}/api/v1/chat/messages/${Uri.encode(messageId)}/forward"))
}

class WikiRepository(
    private val endpoints: SiteEndpoints,
    private val api: ApiClient,
    private val cache: LocalJsonCache,
    private val json: Json = api.json,
) {
    suspend fun fetchCatalog(kb: String = "IGNGmc"): WikiCatalogResponse {
        val url = "${endpoints.api}/api/v1/wiki/catalog".toHttpUrl().newBuilder()
            .addQueryParameter("kb", kb)
            .build().toString()
        val raw = api.get(url)
        cache.write("wiki_catalog_v1_$kb", raw)
        return json.decodeFromString(raw)
    }

    suspend fun readCachedCatalog(kb: String = "IGNGmc"): WikiCatalogResponse? =
        cache.read("wiki_catalog_v1_$kb")?.let { raw ->
            runCatching { json.decodeFromString<WikiCatalogResponse>(raw) }.getOrNull()
        }

    suspend fun fetchPage(pageId: Int, kb: String = "IGNGmc"): WikiPageDetail {
        val url = "${endpoints.api}/api/v1/wiki/pages/$pageId".toHttpUrl().newBuilder()
            .addQueryParameter("kb", kb)
            .build().toString()
        val raw = api.get(url)
        cache.write("wiki_page_v1_${kb}_$pageId", raw)
        return json.decodeFromString<WikiPageResponse>(raw).page
    }

    suspend fun readCachedPage(pageId: Int, kb: String = "IGNGmc"): WikiPageDetail? =
        cache.read("wiki_page_v1_${kb}_$pageId")?.let { raw ->
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
        json.decodeFromString<BoundMcAccountsResponse>(api.get("${endpoints.api}/api/v1/mc/accounts")).data

    suspend fun fetchAccountDetails(username: String): McCharacterDetailsResponse =
        json.decodeFromString(api.get("${endpoints.api}/api/v1/mc/accounts/${Uri.encode(username)}"))

    suspend fun toggleAccountPublic(mcUsername: String, isPublic: Boolean): McActionResult {
        val body = buildJsonObject { put("is_public", isPublic) }
        return json.decodeFromString(
            api.patch("${endpoints.api}/api/v1/mc/accounts/${Uri.encode(mcUsername)}", body.toString()),
        )
    }

    suspend fun unbindAccount(mcUsername: String) {
        api.delete("${endpoints.api}/api/v1/mc/accounts/${Uri.encode(mcUsername)}")
    }

    /**
     * Step-up encrypted game password reset via `PATCH /api/v1/mc/accounts/{username}`.
     * Uses `old_password` + `new_password` sealed with RSA-OAEP-256 + AES-256-GCM from `/api/v1/auth/crypto`.
     */
    suspend fun resetGamePassword(
        mcUsername: String,
        currentCredential: String,
        nextCredential: String,
    ): McActionResult {
        val challenge = authRepository.fetchCryptoChallenge()
        val sealed = CredentialEnvelopeSeal.seal(
            challenge = challenge,
            fields = mapOf(
                "old_password" to currentCredential,
                "new_password" to nextCredential,
            ),
        )
        val body = buildJsonObject {
            put("credential", json.encodeToJsonElement(SealedCredential.serializer(), sealed))
        }
        return json.decodeFromString(
            api.patch("${endpoints.api}/api/v1/mc/accounts/${Uri.encode(mcUsername)}", body.toString()),
        )
    }

    suspend fun fetchLands(serverId: String? = null, keyword: String = "", page: Int = 1): PlayerLandsResponse {
        val url = "${endpoints.api}/api/v1/mc/lands".toHttpUrl().newBuilder()
            .addQueryParameter("page", page.coerceAtLeast(1).toString())
            .addQueryParameter("page_size", "30")
            .apply {
                if (!serverId.isNullOrBlank() && serverId != "all") {
                    val normalized = if (serverId.startsWith("srv_")) serverId else serverId.toIntOrNull()?.let { V1Id.format("srv", it) }
                    if (normalized != null) addQueryParameter("server_id", normalized)
                }
                if (keyword.isNotBlank()) addQueryParameter("q", keyword.trim())
            }
            .build().toString()
        return json.decodeFromString(api.get(url))
    }

    suspend fun updateLand(landId: Int, isPublic: Boolean? = null, description: String? = null): McActionResult {
        val body = buildJsonObject {
            isPublic?.let { put("is_public", it) }
            description?.let { put("description", it) }
        }
        return json.decodeFromString(
            api.patch("${endpoints.api}/api/v1/mc/lands/${V1Id.format("land", landId)}", body.toString()),
        )
    }

    suspend fun fetchTeleports(): List<PlayerTeleportDto> =
        json.decodeFromString<PlayerTeleportsResponse>(api.get("${endpoints.api}/api/v1/mc/teleport-homes")).data

    suspend fun fetchFakePlayers(): List<PlayerFakePlayerDto> =
        json.decodeFromString<PlayerFakePlayersResponse>(api.get("${endpoints.api}/api/v1/mc/fake-players")).data

    suspend fun fetchVaultOverview(): VaultOverviewResponse =
        json.decodeFromString(api.get("${endpoints.api}/api/v1/mc/vaults"))

    suspend fun fetchVaultItems(vaultId: String, query: String = "", page: Int = 1): VaultItemsResponse {
        val url = "${endpoints.api}/api/v1/mc/vaults/${Uri.encode(vaultId)}/items".toHttpUrl().newBuilder()
            .addQueryParameter("page", page.coerceAtLeast(1).toString())
            .addQueryParameter("page_size", "30")
            .apply { if (query.isNotBlank()) addQueryParameter("search", query.trim()) }
            .build().toString()
        return json.decodeFromString(api.get(url))
    }

    suspend fun fetchVaultHistory(vaultId: String, page: Int = 1): VaultHistoryResponse {
        val url = "${endpoints.api}/api/v1/mc/vaults/${Uri.encode(vaultId)}/operations".toHttpUrl().newBuilder()
            .addQueryParameter("page", page.coerceAtLeast(1).toString())
            .addQueryParameter("page_size", "20")
            .build().toString()
        return json.decodeFromString(api.get(url))
    }

    suspend fun fetchVaultTrace(operationId: String): VaultTraceResponse =
        json.decodeFromString(api.get("${endpoints.api}/api/v1/mc/vault-operations/${Uri.encode(operationId)}/events"))

    suspend fun fetchVanillaStats(serverId: Int? = null, character: String? = null): VanillaStatsResponse {
        val url = "${endpoints.api}/api/v1/mc/stats".toHttpUrl().newBuilder()
            .apply {
                if (serverId != null) addQueryParameter("server_id", V1Id.format("srv", serverId))
                if (!character.isNullOrBlank()) addQueryParameter("character", character)
            }
            .build().toString()
        return json.decodeFromString(api.get(url))
    }

    suspend fun fetchMedals(): MedalProfileDto =
        json.decodeFromString(api.get("${endpoints.api}/api/v1/mc/medals"))

    suspend fun renameCustomMedal(mcUsername: String, customName: String): McActionResult {
        val body = buildJsonObject {
            put("username", mcUsername)
            put("custom_name", customName)
        }
        return json.decodeFromString(api.patch("${endpoints.api}/api/v1/mc/medals/custom-name", body.toString()))
    }

    suspend fun fetchPersonalPerfect(): PersonalPerfectResponse =
        json.decodeFromString(api.get("${endpoints.api}/api/v1/mc/perfect/me"))

    suspend fun fetchPermissions(): PlayerPermissionsResponse =
        json.decodeFromString(api.get("${endpoints.api}/api/v1/mc/permissions"))

    suspend fun fetchPermissionInsights(): PermissionInsightsResponse =
        json.decodeFromString(api.get("${endpoints.api}/api/v1/mc/permissions/insights"))
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
        val v1Relation = when (relation.lowercase()) {
            "mine", "created" -> "created"
            "participated" -> "participated"
            "viewed" -> "viewed"
            else -> null
        }
        val url = "${endpoints.api}/api/v1/tickets".toHttpUrl().newBuilder()
            .addQueryParameter("page", page.coerceAtLeast(1).toString())
            .addQueryParameter("page_size", "25")
            .apply {
                if (v1Relation != null) addQueryParameter("relation", v1Relation)
                if (!includeClosed) addQueryParameter("ticket_status", "open")
                if (query.isNotBlank()) addQueryParameter("q", query.trim())
            }
            .build().toString()
        return json.decodeFromString(api.get(url))
    }

    suspend fun fetchOptions(): TicketOptionsResponse =
        json.decodeFromString(api.get("${endpoints.api}/api/v1/ticket-options"))

    suspend fun searchPlayers(query: String): List<TicketPlayerOption> {
        if (query.trim().length < 2) return emptyList()
        val url = "${endpoints.api}/api/v1/mc-players".toHttpUrl().newBuilder()
            .addQueryParameter("q", query.trim())
            .build().toString()
        return json.decodeFromString<TicketPlayersResponse>(api.get(url)).data
    }

    suspend fun fetchDetail(id: Int): TicketDetailDto =
        json.decodeFromString(api.get("${endpoints.api}/api/v1/tickets/${V1Id.format("tk", id)}"))

    suspend fun createTicket(payload: CreateTicketPayload): Int {
        val raw = api.post(
            "${endpoints.api}/api/v1/tickets",
            json.encodeToString(CreateTicketPayload.serializer(), payload),
        )
        val detail = json.decodeFromString<TicketDetailDto>(raw)
        clearDraft()
        return detail.id
    }

    suspend fun updateTicket(id: Int, etag: String, payload: UpdateTicketPayload): TicketDetailDto =
        json.decodeFromString(
            api.patch(
                url = "${endpoints.api}/api/v1/tickets/${V1Id.format("tk", id)}",
                body = json.encodeToString(UpdateTicketPayload.serializer(), payload),
                ifMatch = etag,
            ),
        )

    suspend fun reply(id: Int, content: String, attachmentMediaIds: List<String> = emptyList()) {
        val body = buildJsonObject {
            put("content", content)
            if (attachmentMediaIds.isNotEmpty()) {
                put(
                    "attachment_ids",
                    buildJsonArray { attachmentMediaIds.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } },
                )
            }
        }
        api.post("${endpoints.api}/api/v1/tickets/${V1Id.format("tk", id)}/messages", body.toString())
    }

    suspend fun close(id: Int): TicketDetailDto =
        json.decodeFromString(api.post("${endpoints.api}/api/v1/tickets/${V1Id.format("tk", id)}/close", "{}"))

    suspend fun reopen(id: Int): TicketDetailDto =
        json.decodeFromString(api.post("${endpoints.api}/api/v1/tickets/${V1Id.format("tk", id)}/reopen", "{}"))

    suspend fun uploadAttachment(fileName: String, mimeType: String, bytes: ByteArray): UploadedMediaDto {
        val raw = api.uploadImage(
            url = "${endpoints.api}/api/v1/media",
            fileName = fileName,
            mimeType = mimeType,
            bytes = bytes,
            extraFields = mapOf("purpose" to "ticket_attachment"),
        )
        return json.decodeFromString<UploadedMediaDto>(raw)
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
        json.decodeFromString(api.get("${endpoints.api}/api/v1/mc/admin/tickets/overview"))

    suspend fun fetchAdminTickets(
        includeClosed: Boolean = false,
        query: String = "",
        page: Int = 1,
    ): TicketListResponse {
        val url = "${endpoints.api}/api/v1/mc/admin/tickets".toHttpUrl().newBuilder()
            .addQueryParameter("page", page.coerceAtLeast(1).toString())
            .addQueryParameter("page_size", "25")
            .addQueryParameter("include_closed", includeClosed.toString())
            .apply { if (query.isNotBlank()) addQueryParameter("q", query.trim()) }
            .build().toString()
        return json.decodeFromString(api.get(url))
    }

    suspend fun fetchAdminTicketDetail(id: Int): TicketDetailDto =
        json.decodeFromString(api.get("${endpoints.api}/api/v1/mc/admin/tickets/${V1Id.format("tk", id)}"))

    suspend fun replyAdminTicket(id: Int, content: String, attachmentMediaIds: List<String> = emptyList()) {
        val body = buildJsonObject {
            put("content", content)
            if (attachmentMediaIds.isNotEmpty()) {
                put(
                    "attachment_ids",
                    buildJsonArray { attachmentMediaIds.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } },
                )
            }
        }
        api.post("${endpoints.api}/api/v1/mc/admin/tickets/${V1Id.format("tk", id)}/messages", body.toString())
    }

    suspend fun closeAdminTicket(id: Int): TicketDetailDto =
        json.decodeFromString(api.post("${endpoints.api}/api/v1/mc/admin/tickets/${V1Id.format("tk", id)}/close", "{}"))

    suspend fun reopenAdminTicket(id: Int): TicketDetailDto =
        json.decodeFromString(api.post("${endpoints.api}/api/v1/mc/admin/tickets/${V1Id.format("tk", id)}/reopen", "{}"))

    suspend fun searchAdminPlayers(query: String): List<AdminPlayerResultDto> {
        val url = "${endpoints.api}/api/v1/mc/admin/players/search".toHttpUrl().newBuilder()
            .addQueryParameter("q", query.trim())
            .build().toString()
        return json.decodeFromString<AdminPlayerSearchResponse>(api.get(url)).results
    }

    suspend fun fetchRecentPunishments(search: String = "", page: Int = 1): AdminPunishmentsResponse {
        val url = "${endpoints.api}/api/v1/mc/admin/punishments".toHttpUrl().newBuilder()
            .addQueryParameter("page", page.coerceAtLeast(1).toString())
            .addQueryParameter("page_size", "25")
            .apply { if (search.isNotBlank()) addQueryParameter("q", search.trim()) }
            .build().toString()
        return json.decodeFromString(api.get(url))
    }

    suspend fun fetchHistorianReports(): HistorianListResponse =
        json.decodeFromString(api.get("${endpoints.api}/api/v1/mc/admin/historian"))

    suspend fun fetchHistorianDetail(id: String): HistorianDetailResponse =
        json.decodeFromString(api.get("${endpoints.api}/api/v1/mc/admin/historian/${Uri.encode(id)}"))

    suspend fun fetchAdminCurrentNodes(): AdminCurrentNodesResponse =
        json.decodeFromString(api.get("${endpoints.api}/api/v1/mc/admin/status/nodes"))

    suspend fun fetchAdminTraffic(rangeHours: Long = 24, ip: String = ""): TrafficAdminResponse {
        val end = Instant.now()
        val start = if (rangeHours == 0L) {
            LocalDate.now(ZoneId.systemDefault()).atStartOfDay(ZoneId.systemDefault()).toInstant()
        } else {
            end.minus(rangeHours, ChronoUnit.HOURS)
        }
        val url = "${endpoints.api}/api/v1/mc/admin/status/traffic".toHttpUrl().newBuilder()
            .addQueryParameter("start", start.toString())
            .addQueryParameter("end", end.toString())
            .addQueryParameter("source", "backend")
            .addQueryParameter("limit", "25")
            .apply { if (ip.isNotBlank()) addQueryParameter("ip", ip.trim()) }
            .build().toString()
        return json.decodeFromString(api.get(url))
    }
}
