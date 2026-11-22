package net.igng.mcstatus.core.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.igng.mcstatus.core.auth.OAuthTokenResponse
import net.igng.mcstatus.core.model.BootstrapResponse
import net.igng.mcstatus.core.network.ApiClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract drift guard: deserializes EVERY golden JSON fixture exported directly from
 * `IGNG站点/contracts/fixtures/igngmc-v1/` using the production Kotlin DTOs.
 */
class DtoDeserializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    private fun fixture(name: String): String =
        requireNotNull(javaClass.classLoader?.getResourceAsStream("fixtures/v1/$name")) {
            "Missing golden fixture: fixtures/v1/$name"
        }.bufferedReader(Charsets.UTF_8).use { it.readText() }

    @Test
    fun decodesBootstrapAndOAuthTokenFixtures() {
        val boot = json.decodeFromString<BootstrapResponse>(fixture("bootstrap.json"))
        assertEquals("1.14", boot.apiVersion)
        assertEquals("https://www.igng.net", boot.urls.apiBaseUrl)
        assertTrue(boot.features.loginHistory.available)
        assertTrue(boot.features.penaltyHistory.available)
        assertTrue(boot.viewer.authenticated)
        assertEquals(1024, boot.viewer.user?.resolvedNumericId)
        assertEquals("SUPERADMIN", boot.viewer.adminRole)
        assertEquals(1, boot.viewer.boundMcAccounts.size)
        assertTrue(boot.viewer.boundMcAccounts.first().verified)
        assertEquals("ingame_verify", boot.viewer.boundMcAccounts.first().uuidProvenance)

        val token = json.decodeFromString<OAuthTokenResponse>(fixture("oauth_token.json"))
        assertEquals("Bearer", token.tokenType)
        assertEquals(3600L, token.expiresIn)
        assertFalse(token.accessToken.isNullOrBlank())
        assertFalse(token.refreshToken.isNullOrBlank())
        assertEquals(1024, token.user?.id)

        val dailyObj = json.parseToJsonElement(fixture("daily_login.json")).jsonObject
        assertEquals("true", dailyObj["awarded"]?.jsonPrimitive?.content)
        assertEquals("1250", dailyObj["exp"]?.jsonPrimitive?.content)
        assertEquals("13", dailyObj["level"]?.jsonPrimitive?.content)

        val errObj = json.parseToJsonElement(fixture("error_envelope.json")).jsonObject
        val nested = errObj["error"]?.jsonObject
        assertEquals("ticket.not_found", nested?.get("code")?.jsonPrimitive?.content)
        assertFalse(nested?.get("message")?.jsonPrimitive?.content.isNullOrBlank())
    }

    @Test
    fun decodesServersMotdStatusAndHistoryFixtures() {
        val catalog = json.decodeFromString<HomeServersResponse>(fixture("servers.json"))
        assertEquals(1, catalog.families.size)
        val family = catalog.families.first()
        assertEquals("survival", family.slug)
        assertEquals(3, family.primarySeason?.serverId)
        assertTrue(family.allKnownSeasons.isNotEmpty())
        assertEquals(1, catalog.servers.size)

        val motd = json.decodeFromString<MotdResponse>(fixture("server_motd.json"))
        assertEquals("srv_3", motd.serverId)
        assertTrue(motd.online)
        assertEquals(18, motd.players?.online)

        val status = json.decodeFromString<PublicStatusResponse>(fixture("status_public.json"))
        assertEquals("fresh", status.collector?.freshness)
        assertEquals(1, status.servers.size)
        val srv = status.servers.first()
        assertEquals(3, srv.id)
        assertEquals("srv_3", srv.resolvedServerId)
        assertEquals(18, srv.onlinePlayers)
        assertEquals(19.95, srv.tps ?: 0.0, 0.001)

        val history = json.decodeFromString<PublicStatusHistoryResponse>(fixture("status_history.json"))
        assertEquals(15, history.bucketMinutes)
        val series = history.seriesFor(3)
        assertNotNull(series)
        assertEquals(1, series?.latencies?.size)
        assertEquals(1, series?.performance?.size)
        assertEquals(1, series?.statusGrid?.size)
    }

    @Test
    fun decodesTicketsFixtures() {
        val list = json.decodeFromString<TicketListResponse>(fixture("tickets_list.json"))
        assertEquals(1, list.total)
        assertEquals(2048, list.resolvedTickets.first().id)
        assertEquals("appeal", list.resolvedTickets.first().type)
        assertEquals("申诉", list.resolvedTickets.first().typeLabel)
        assertEquals("open", list.resolvedTickets.first().status)

        val detail = json.decodeFromString<TicketDetailDto>(fixture("ticket_detail.json"))
        assertEquals(2048, detail.id)
        assertEquals("\"tk-2048-95ce5f92\"", detail.etag)
        assertTrue(detail.effectiveCanReply)
        assertTrue(detail.effectiveCanEdit)
        assertTrue(detail.effectiveCanClose)
        assertFalse(detail.effectiveCanReopen)
        assertEquals("Alice", detail.creatorName)
        assertEquals(1, detail.messages.size)
        assertEquals("med_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", detail.messages.first().attachments.first().resolvedMediaId)
    }

    @Test
    fun decodesPlayerAssetsStatsMedalsPermissionsAndPerfectFixtures() {
        val accounts = json.decodeFromString<BoundMcAccountsResponse>(fixture("mc_accounts.json"))
        assertEquals(1, accounts.data.size)
        val firstAccount = accounts.data.first()
        assertEquals("Alice_CN", firstAccount.resolvedName)
        assertTrue(firstAccount.isPublic)
        assertTrue(firstAccount.verified)
        assertEquals("ingame_verify", firstAccount.uuidProvenance)
        assertTrue(firstAccount.loginHistoryAvailable)
        assertTrue(firstAccount.penaltyHistoryAvailable)

        val accountDetail = json.decodeFromString<McCharacterDetailsResponse>(fixture("mc_account_detail.json"))
        assertTrue(accountDetail.loginHistoryAvailable)
        assertTrue(accountDetail.penaltyHistoryAvailable)
        assertTrue(accountDetail.account?.verified == true)
        assertNull(accountDetail.activePenalty?.ban)
        assertEquals(19, accountDetail.activePenalty?.mute?.id)
        assertEquals(1, accountDetail.history.size)
        assertEquals("*", accountDetail.history.first().serverScope)
        assertFalse(accountDetail.history.first().predatesRegistration)

        val loginRecords = json.decodeFromString<McLoginRecordsResponse>(fixture("mc_login_records.json"))
        assertEquals(1, loginRecords.data.size)
        assertEquals("lgn_881204", loginRecords.data.first().recordId)
        assertEquals(9, loginRecords.data.first().serverId)
        assertTrue(loginRecords.data.first().isSuccess)
        assertEquals("2026-11-22T00:00:00Z", loginRecords.historySince)
        assertTrue(loginRecords.unverifiedAccounts.isEmpty())

        val lands = json.decodeFromString<PlayerLandsResponse>(fixture("mc_lands.json"))
        assertEquals(1, lands.total)
        val land = lands.records.first()
        assertEquals(4091, land.id)
        assertEquals(3, land.server_id)
        assertTrue(land.can_edit)
        assertTrue(land.is_public)

        val stats = json.decodeFromString<VanillaStatsResponse>(fixture("stats.json"))
        assertTrue(stats.available)
        assertEquals(36000L, stats.summary.playTimeSeconds)
        assertEquals(12345L, stats.summary.blocksMined)
        assertEquals(3, stats.servers.first().serverId)
        assertEquals("石头", stats.topMined.first().label)

        val medals = json.decodeFromString<MedalProfileDto>(fixture("medals.json"))
        assertEquals(1, medals.allActiveMedals.size)
        assertEquals("开服元老", medals.allActiveMedals.first().name)
        assertEquals("Alice_CN", medals.customMedals.first().ownerName)
        assertEquals("星辰旅者", medals.customMedals.first().customName)

        val perms = json.decodeFromString<PlayerPermissionsResponse>(fixture("mc_permissions.json"))
        assertEquals("luck_perms", perms.source)
        assertEquals(1, perms.accounts.size)
        assertEquals("essentials.home", perms.accounts.first().groups.first().permissions.first().nodeKey)
        assertTrue(perms.accounts.first().independentPermissions.first().value)

        val perfect = json.decodeFromString<PublicPerfectRosterResponse>(fixture("perfect_public.json"))
        assertEquals(5, perfect.total)
        assertEquals(1, perfect.members.size)
        assertEquals(1024, perfect.members.first().igngId)
    }

    @Test
    fun decodesChatAndWikiFixtures() {
        val sources = json.decodeFromString<ChatSourcesResponse>(fixture("chat_sources.json"))
        assertEquals(3, sources.servers.first().id)
        assertEquals("Alice_CN", sources.servers.first().lastMessage?.sender)
        assertEquals("主城东门修好了", sources.servers.first().lastPreview)
        assertEquals(2, sources.qqGroups.size)
        assertEquals("123456789", sources.qqGroups.first().id)
        assertEquals(420, sources.qqGroups.first().messageCount)
        assertEquals("wechat", sources.qqGroups[1].platform)
        assertEquals("https://img.igng.net/avatars/wx-group.webp", sources.qqGroups[1].avatarUrl)
        assertEquals("云萤", sources.qqGroups[1].persona?.label)
        assertEquals(1, sources.privateChats.size)
        assertEquals("-10001", sources.privateChats.first().id)
        assertTrue(sources.privateChats.first().isPrivate)

        val messages = json.decodeFromString<ChatLogsResponse>(fixture("chat_messages.json"))
        assertTrue(messages.hasMore)
        assertEquals("yunying", messages.persona?.id)
        assertEquals(1024, messages.senderProfiles["qq:10001"]?.igngId)
        assertEquals("云萤 Pro", messages.senderProfiles["qq:10001"]?.badges?.firstOrNull()?.label)
        val msg = messages.messages.first()
        assertEquals("qq-90001", msg.id)
        assertEquals("Alice", msg.player_name)
        assertTrue(msg.is_at_all)
        assertTrue(msg.has_forward)
        assertEquals("Bob", msg.replyTo?.senderName)
        assertEquals(2, msg.messageStructure.size)
        assertEquals("/api/v1/chat/media/qq-90001/0", msg.allAttachments.first().url)
        assertEquals("/api/v1/chat/media/qq-90001/0?variant=thumb", msg.allAttachments.first().thumbUrl)
        assertEquals("末影龙坐标 0, 64, 0", msg.allAttachments.first().ocrText)

        val forward = json.decodeFromString<ChatForwardResponse>(fixture("chat_forward.json"))
        assertEquals("qq-90001", forward.messageId)
        assertEquals(2, forward.resolvedNodes.size)
        assertEquals(2, forward.structure.first().children.size)
        assertEquals("Alice", forward.structure.first().children.first().nickname)

        val wikiCatalog = json.decodeFromString<WikiCatalogResponse>(fixture("wiki_catalog.json"))
        assertEquals("IGNGmc", wikiCatalog.knowledgeBase?.slug)
        assertEquals(2, wikiCatalog.pages.size)
        assertEquals(10, wikiCatalog.pages.first().id)

        val wikiPage = json.decodeFromString<WikiPageResponse>(fixture("wiki_page.json"))
        assertEquals(11, wikiPage.page.id)
        assertEquals("新手入门", wikiPage.page.title)
        assertTrue(wikiPage.page.content.contains("新手入门"))
    }

    @Test
    fun decodesVaultHistorianNodesAndTrafficV1Payloads() {
        val vaultRaw = """
            {
              "data": [
                {
                  "group_key": "fp-1",
                  "material": "minecraft:netherite_pickaxe",
                  "material_name": "下界合金镐",
                  "display_name": "§b星辉镐",
                  "has_custom_name": true,
                  "total_amount": 2,
                  "entry_count": 2,
                  "enchantments": [{"id": "efficiency", "level": 5}],
                  "enchantment_labels": ["效率 V", "经验修补"]
                }
              ],
              "pagination": {"page": 1, "page_size": 30, "total": 1, "total_pages": 1},
              "meta": {"available": true, "truncated": false, "scanned": 2}
            }
        """.trimIndent()
        val vault = json.decodeFromString<VaultItemsResponse>(vaultRaw)
        assertTrue(vault.available)
        assertEquals("§b星辉镐", vault.groups.first().title)
        assertEquals(2, vault.groups.first().enchantmentLabels.size)

        val historianRaw = """
            {
              "role": "superadmin",
              "servers": [{"server_id": "srv_3", "server_name": "生存服"}],
              "items": [{"report_id": "rep-1", "server_id": "srv_3", "period_type": "weekly", "period_start": "2026-11-16", "status": "succeeded"}]
            }
        """.trimIndent()
        val historian = json.decodeFromString<HistorianListResponse>(historianRaw)
        assertEquals("生存服", historian.servers.first().serverName)
        assertEquals("rep-1", historian.items.first().id)

        val nodesRaw = """
            {
              "role": "superadmin",
              "generated_at": "2026-11-22T12:00:00Z",
              "servers": [
                {
                  "server_id": "srv_3",
                  "numeric_server_id": 3,
                  "server_name": "生存服",
                  "status": {"overall": "healthy", "liveness": "online", "performance": "healthy", "network": "healthy"},
                  "performance": {"tps": 19.9, "online_players": 12}
                }
              ]
            }
        """.trimIndent()
        val nodes = json.decodeFromString<AdminCurrentNodesResponse>(nodesRaw)
        assertEquals(1, nodes.servers.size)
        assertEquals(19.9, nodes.servers.first().performance?.tps ?: 0.0, 0.001)

        val trafficRaw = """
            {
              "role": "superadmin",
              "bucket_seconds": 1800,
              "source": "backend",
              "source_label": "后端流量",
              "totals": {"tx_bytes": 1048576, "rx_bytes": 2097152, "total_bytes": 3145728},
              "top_ips": [{"ip": "198.51.100.10", "tx_bytes": 1000, "rx_bytes": 2000, "total_bytes": 3000}]
            }
        """.trimIndent()
        val traffic = json.decodeFromString<TrafficAdminResponse>(trafficRaw)
        assertEquals(3145728L, traffic.totals.total)
        assertEquals("198.51.100.10", traffic.topIps.first().ip)
    }

    @Test
    fun resolvesArchivedAndRetiredMapModesCorrectly() {
        val archived = ServerSeasonDto(slug = "s2", state = "retired", mapMode = "archived", hasMap = true)
        assertTrue(archived.hasViewableMap)
        assertTrue(archived.isArchivedMap)
        assertFalse(archived.isSnapshotMap)

        val snapshot = ServerSeasonDto(slug = "s1", state = "retired", mapMode = "snapshot", hasMap = true)
        assertTrue(snapshot.hasViewableMap)
        assertFalse(snapshot.isArchivedMap)
        assertTrue(snapshot.isSnapshotMap)

        val retiredWithoutMap = ServerSeasonDto(slug = "s0", state = "retired", mapMode = "none", hasMap = false)
        assertFalse(retiredWithoutMap.hasViewableMap)
        assertFalse(retiredWithoutMap.isArchivedMap)
        assertFalse(retiredWithoutMap.isSnapshotMap)
    }

    @Test
    fun serializesTicketPatchAndAccessPolicyWithRequiredKeys() {
        val partialRaw = ApiClient.DefaultJson.encodeToString(
            UpdateTicketPayload.serializer(),
            UpdateTicketPayload(title = "修改后的标题"),
        )
        val partialObj = ApiClient.DefaultJson.parseToJsonElement(partialRaw).jsonObject
        assertEquals("修改后的标题", partialObj["title"]?.jsonPrimitive?.content)
        assertNull(partialObj["ticket_type"])
        assertNull(partialObj["targets"])
        assertNull(partialObj["access_policy"])

        val policyRaw = ApiClient.DefaultJson.encodeToString(
            UpdateTicketPayload.serializer(),
            UpdateTicketPayload(accessPolicy = TicketAccessPolicy()),
        )
        val policyObj = ApiClient.DefaultJson.parseToJsonElement(policyRaw).jsonObject["access_policy"]?.jsonObject
        assertNotNull(policyObj)
        for (action in listOf("view", "participate", "manage")) {
            val rule = policyObj?.get(action)?.jsonObject
            assertNotNull("Missing action $action", rule)
            assertEquals("mc.admin", rule?.get("group_code")?.jsonPrimitive?.content)
            assertNotNull("Missing target_group_enabled for $action", rule?.get("target_group_enabled"))
        }
        assertEquals("false", policyObj?.get("manage")?.jsonObject?.get("target_group_enabled")?.jsonPrimitive?.content)
    }
}
