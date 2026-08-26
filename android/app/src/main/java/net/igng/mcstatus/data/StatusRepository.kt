package net.igng.mcstatus.data

import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

class StatusRepository(
    private val baseUrl: String,
    private val client: OkHttpClient = OkHttpClient(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    },
) {
    suspend fun fetchServers(): List<ServerSummary> =
        getJson("$baseUrl/api/status/list")

    suspend fun fetchNodes(): List<NodeSummary> =
        getJson("$baseUrl/api/status/nodes")

    suspend fun fetchCurrentStatus(serverIds: List<Int> = emptyList()): CurrentStatusResponse {
        val url = "$baseUrl/api/status/current".toHttpUrl().newBuilder().apply {
            if (serverIds.isNotEmpty()) {
                addQueryParameter("serverIds", serverIds.joinToString(","))
            }
        }.build()
        return getJson(url.toString())
    }

    suspend fun fetchTimeline(
        serverIds: List<Int>,
        preset: RangePreset,
    ): TimelineResponse {
        if (serverIds.isEmpty()) {
            return TimelineResponse()
        }

        val (start, end) = buildRange(preset)
        val url = "$baseUrl/api/status/timeline".toHttpUrl().newBuilder()
            .addQueryParameter("serverIds", serverIds.joinToString(","))
            .addQueryParameter("start", start)
            .addQueryParameter("end", end)
            .build()

        return getJson(url.toString())
    }

    suspend fun fetchServerDetail(
        serverId: Int,
        preset: RangePreset,
    ): ServerDetailResponse {
        val (start, end) = buildRange(preset)
        val url = "$baseUrl/api/status/server/$serverId".toHttpUrl().newBuilder()
            .addQueryParameter("start", start)
            .addQueryParameter("end", end)
            .build()

        return getJson(url.toString())
    }

    suspend fun fetchTraffic(
        serverId: Int,
        preset: RangePreset,
    ): TrafficResponse {
        val (start, end) = buildRange(preset)
        val url = "$baseUrl/api/status/traffic".toHttpUrl().newBuilder()
            .addQueryParameter("serverId", serverId.toString())
            .addQueryParameter("start", start)
            .addQueryParameter("end", end)
            .build()
        return getJson(url.toString())
    }

    suspend fun fetchFakePlayers(): List<FakePlayer> =
        getJson("$baseUrl/api/status/fake-players")

    suspend fun fetchMcAuthMe(token: String): McAuthMeResponse =
        getJson("$baseUrl/api/auth/me", token = token)

    suspend fun fetchAdminTraffic(
        token: String,
        preset: TrafficAdminRange,
        serverId: Int? = null,
        source: String = "backend",
        ip: String? = null,
        limit: Int = 30,
    ): TrafficAdminResponse {
        val (start, end) = buildTrafficRange(preset)
        val url = "$baseUrl/api/status/traffic/admin".toHttpUrl().newBuilder()
            .addQueryParameter("start", start)
            .addQueryParameter("end", end)
            .addQueryParameter("source", source)
            .addQueryParameter("limit", limit.coerceIn(1, 50).toString())
            .apply {
                if (serverId != null) addQueryParameter("serverId", serverId.toString())
                if (!ip.isNullOrBlank()) addQueryParameter("ip", ip.trim())
            }
            .build()
        return getJson(url.toString(), token = token)
    }

    private suspend inline fun <reified T> getJson(
        url: String,
        token: String? = null,
    ): T = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .get()
            .apply {
                if (!token.isNullOrBlank()) header("Authorization", "Bearer $token")
            }
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Request failed: ${response.code} $url")
            }

            val body = response.body?.string()
                ?: throw IOException("Empty response body: $url")
            json.decodeFromString(body)
        }
    }

    private fun buildRange(preset: RangePreset): Pair<String, String> {
        val end = Instant.now()
        val start = end.minus(preset.hours, ChronoUnit.HOURS)
        return start.toString() to end.toString()
    }

    private fun buildTrafficRange(preset: TrafficAdminRange): Pair<String, String> {
        val end = Instant.now()
        val localZone = ZoneId.systemDefault()
        val start = when (preset) {
            TrafficAdminRange.HOUR_1 -> end.minus(1, ChronoUnit.HOURS)
            TrafficAdminRange.TODAY -> LocalDate.now(localZone).atStartOfDay(localZone).toInstant()
            TrafficAdminRange.DAY_3 -> end.minus(3, ChronoUnit.DAYS)
            TrafficAdminRange.DAY_7 -> end.minus(7, ChronoUnit.DAYS)
            TrafficAdminRange.MONTH -> end.minus(30, ChronoUnit.DAYS)
        }
        return start.toString() to end.toString()
    }
}

enum class TrafficAdminRange(val id: String, val label: String) {
    HOUR_1("1h", "最近 1 小时"),
    TODAY("today", "今天"),
    DAY_3("3d", "近 3 天"),
    DAY_7("7d", "近 7 天"),
    MONTH("month", "本月"),
}
