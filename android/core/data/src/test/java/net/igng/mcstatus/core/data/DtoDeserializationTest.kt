package net.igng.mcstatus.core.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DtoDeserializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Test
    fun decodesHomeServersAndAllKnownSeasons() {
        val raw = """
            {
              "families": [
                {
                  "slug": "clear",
                  "name": "明净界",
                  "themeKey": "clear",
                  "tagline": "原版纯净生电生存",
                  "primarySeason": {
                    "slug": "clear-s4",
                    "label": "四周目",
                    "state": "live",
                    "primary": true,
                    "address": "clear.mc.igng.net",
                    "version": "1.21.4",
                    "hasMap": true
                  },
                  "aliveSeasons": [
                    {
                      "slug": "clear-s4",
                      "label": "四周目",
                      "state": "live",
                      "primary": true
                    }
                  ],
                  "historicalSeasons": [
                    {
                      "slug": "clear-s3",
                      "label": "三周目",
                      "state": "retired",
                      "mapMode": "snapshot"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val parsed = json.decodeFromString<HomeServersResponse>(raw)
        assertEquals(1, parsed.families.size)
        val family = parsed.families.first()
        assertEquals("明净界", family.name)
        assertEquals(2, family.allKnownSeasons.size)
        assertEquals("clear-s4", family.primarySeason?.slug)
        assertEquals("clear.mc.igng.net", family.primarySeason?.address)
    }

    @Test
    fun decodesPublicStatusAndHistorySeries() {
        val statusRaw = """
            {
              "generatedAt": "2026-11-22T09:00:00.000Z",
              "collector": {
                "id": 1,
                "label": "los-vps",
                "ageSeconds": 12.5,
                "freshness": "fresh"
              },
              "servers": [
                {
                  "id": 7,
                  "slug": "clear-s4",
                  "name": "明净界 · 四周目",
                  "familySlug": "clear",
                  "themeKey": "clear",
                  "online": true,
                  "status": "online",
                  "onlinePlayers": 14,
                  "pingMs": 38.4,
                  "tps": 19.98,
                  "mspt": 14.2,
                  "unknownFutureField": "ignored"
                }
              ]
            }
        """.trimIndent()

        val status = json.decodeFromString<PublicStatusResponse>(statusRaw)
        assertEquals("fresh", status.collector?.freshness)
        assertEquals(1, status.servers.size)
        assertEquals(14, status.servers.first().onlinePlayers)
        assertEquals(19.98, status.servers.first().tps ?: 0.0, 0.001)

        val historyRaw = """
            {
              "bucketMinutes": 16,
              "data": {
                "7": {
                  "latencies": [
                    {"avg_latency_ms": 41.2, "packet_loss_pct": 0.0, "timestamp_utc": "2026-11-22T08:00:00Z", "status": "online"}
                  ],
                  "performance": [
                    {"avg_tps": 20.0, "avg_mspt": 12.1, "online_players": 10, "recorded_at": "2026-11-22T08:00:00Z"}
                  ],
                  "statusGrid": {
                    "segments": 2,
                    "buckets": [
                      {"start": "2026-11-22T08:00:00Z", "status": "online", "coverageLabel": "100%"},
                      {"start": "2026-11-22T08:16:00Z", "status": "degraded", "coverageLabel": "92%"}
                    ]
                  }
                }
              }
            }
        """.trimIndent()

        val history = json.decodeFromString<PublicStatusHistoryResponse>(historyRaw)
        val series = history.data["7"]
        assertNotNull(series)
        assertEquals(1, series?.latencies?.size)
        assertEquals(2, series?.statusGrid?.buckets?.size)
    }

    @Test
    fun decodesTicketDetailCapabilitiesAndAttachments() {
        val raw = """
            {
              "ticket": {
                "id": 42,
                "title": "领地权限协助排查",
                "type": "SUPPORT",
                "typeLabel": "技术协助",
                "status": "OPEN",
                "statusLabel": "处理中",
                "creatorName": "Steve",
                "capabilities": {
                  "canReply": true,
                  "canEdit": true,
                  "canClose": true,
                  "canReopen": false
                },
                "messages": [
                  {
                    "id": 101,
                    "authorName": "Steve",
                    "kind": "INITIAL",
                    "content": "箱子容器无法开启",
                    "attachments": [
                      {"mediaId": "m-99", "previewUrl": "/api/mc/media/messages/101/m-99"}
                    ]
                  }
                ]
              }
            }
        """.trimIndent()

        val detail = json.decodeFromString<TicketDetailResponse>(raw).ticket
        assertEquals(42, detail.id)
        assertTrue(detail.effectiveCanReply)
        assertTrue(detail.effectiveCanClose)
        assertFalse(detail.effectiveCanReopen)
        assertEquals("m-99", detail.messages.first().attachments.first().resolvedMediaId)
    }

    @Test
    fun decodesVaultVanillaStatsMedalsChatAndAdminPayloads() {
        val vaultRaw = """
            {
              "success": true,
              "available": true,
              "total": 1,
              "groups": [
                {
                  "material": "minecraft:netherite_pickaxe",
                  "chinese_name": "下界合金镐",
                  "custom_name": "§b星辉镐",
                  "total_amount": 2,
                  "entry_count": 2,
                  "enchantments": {"efficiency": 5, "mending": 1},
                  "lore": ["§7四周目纪念"]
                }
              ]
            }
        """.trimIndent()
        val vault = json.decodeFromString<VaultItemsResponse>(vaultRaw)
        assertEquals("§b星辉镐", vault.groups.first().title)
        assertEquals(5, vault.groups.first().enchantments["efficiency"])

        val statsRaw = """
            {
              "success": true,
              "summary": {"playTimeSeconds": 7200, "blocksMined": 4096, "mobKills": 128},
              "topMined": [{"key": "minecraft:stone", "label": "石头", "value": 2048}]
            }
        """.trimIndent()
        val stats = json.decodeFromString<VanillaStatsResponse>(statsRaw)
        assertEquals(7200L, stats.summary.playTimeSeconds)
        assertEquals("石头", stats.topMined.first().label)

        val medalRaw = """
            {
              "success": true,
              "activeMedals": [{"id": 1, "medalId": 9, "name": "开拓者", "account": "Steve"}],
              "customMedals": [{"account": "Steve", "customName": "红石学徒", "canEdit": true}]
            }
        """.trimIndent()
        val medals = json.decodeFromString<MedalProfileDto>(medalRaw)
        assertEquals(1, medals.allActiveMedals.size)
        assertEquals("Steve", medals.customMedals.first().ownerName)

        val chatRaw = """
            {
              "messages": [
                {
                  "id": "qq-1",
                  "source": "qq",
                  "player_name": "Alex",
                  "content": "截图来了",
                  "has_forward": true,
                  "media": [{"index": 0, "url": "/api/media/qq-1/0"}]
                }
              ],
              "hasMore": true
            }
        """.trimIndent()
        val chat = json.decodeFromString<ChatLogsResponse>(chatRaw)
        assertTrue(chat.hasMore)
        assertEquals(1, chat.messages.first().allAttachments.size)

        val historianRaw = """
            {
              "servers": [{"server_id": 7, "server_name": "明净界"}],
              "items": [{"id": "rep-1", "server_id": 7, "period_kind": "weekly", "period_start": "2026-11-16", "status": "succeeded"}]
            }
        """.trimIndent()
        val historian = json.decodeFromString<HistorianListResponse>(historianRaw)
        assertEquals("明净界", historian.servers.first().server_name)
        assertEquals("rep-1", historian.items.first().id)

        val trafficRaw = """
            {
              "bucketSeconds": 1800,
              "totals": {"tx": 1048576, "rx": 2097152, "total": 3145728},
              "topIps": [{"ip": "198.51.100.10", "tx": 1000, "rx": 2000, "total": 3000}]
            }
        """.trimIndent()
        val traffic = json.decodeFromString<TrafficAdminResponse>(trafficRaw)
        assertEquals(3145728L, traffic.totals.total)
        assertEquals("198.51.100.10", traffic.topIps.first().ip)
    }
}
