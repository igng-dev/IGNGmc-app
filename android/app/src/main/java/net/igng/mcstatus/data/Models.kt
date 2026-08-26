package net.igng.mcstatus.data

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ServerSummary(
    val server_id: Int,
    val server_name: String,
    val address: String? = null,
)

@Serializable
data class NodeSummary(
    val node_id: Int,
    val node_name: String,
    val node_ip: String? = null,
    // MySQL tinyint is emitted as 0/1 by the site; keep this tolerant of
    // either that representation or a future JSON boolean.
    val enabled: JsonElement? = null,
    val last_seen_at: String? = null,
    val collector_version: String? = null,
    val age_seconds: Double? = null,
    val freshness: String? = null,
)

@Serializable
data class LatencyRecord(
    val server_id: Int? = null,
    val node_id: Int,
    val node_name: String? = null,
    val avg_latency_ms: Double? = null,
    val max_latency_ms: Double? = null,
    val min_latency_ms: Double? = null,
    val packet_loss_pct: Double? = null,
    val timestamp_utc: String,
    val attempt_count: Int? = null,
    val success_count: Int? = null,
    val timeout_count: Int? = null,
    val error_count: Int? = null,
    val probe_status: String? = null,
    val probe_protocol: String? = null,
    val p50_latency_ms: Double? = null,
    val p95_latency_ms: Double? = null,
    val jitter_ms: Double? = null,
    val probe_started_at_utc: String? = null,
    val probe_finished_at_utc: String? = null,
    val collector_id: String? = null,
    val collector_version: String? = null,
    val age_seconds: Double? = null,
    val freshness: String? = null,
    val status: String? = null,
)

@Serializable
data class PerformanceSample(
    val server_id: Int? = null,
    val avg_tps: Double? = null,
    val avg_mspt: Double? = null,
    val cpu_usage: Double? = null,
    val memory_usage_mb: Double? = null,
    val online_players: Int? = null,
    val recorded_at: String,
    val sample_count: Int? = null,
    val expected_sample_count: Int? = null,
    val metrics_status: String? = null,
    val window_start_epoch: Long? = null,
    val window_end_epoch: Long? = null,
    val window_start: String? = null,
    val window_end: String? = null,
    val container_cpu_usage_percent: Double? = null,
    val cpu_capacity_percent: Double? = null,
    val container_memory_usage_mb: Double? = null,
    val container_memory_limit_mb: Double? = null,
    val resource_status: String? = null,
    val resource_source: String? = null,
)

@Serializable
data class StatusGridReference(
    val nodeId: Int,
    val avg: Double,
    val loss: Double,
)

@Serializable
data class StatusGridNodeStat(
    val nodeId: Int,
    val avg: Double,
    val loss: Double,
    val maxLatency: Double = 0.0,
    val count: Int = 0,
)

@Serializable
data class StatusGridBucket(
    val start: String,
    val end: String? = null,
    val status: String,
    val nodeStats: List<StatusGridNodeStat> = emptyList(),
    val coverageLabel: String = "0/0",
    val referenceNode: StatusGridReference? = null,
)

@Serializable
data class StatusGridPayload(
    val segments: Int = 0,
    val buckets: List<StatusGridBucket> = emptyList(),
)

@Serializable
data class TimelineServerPayload(
    val latencies: List<LatencyRecord> = emptyList(),
    val perf: List<PerformanceSample> = emptyList(),
    val statusGrid: StatusGridPayload? = null,
)

@Serializable
data class TimelineResponse(
    val data: Map<String, TimelineServerPayload> = emptyMap(),
    val nodeNames: Map<String, String> = emptyMap(),
    val generatedAt: String? = null,
    val range: TimeRange? = null,
    val bucketMinutes: Int? = null,
    val nodes: List<NodeSummary> = emptyList(),
)

@Serializable
data class TimeRange(
    val start: String? = null,
    val end: String? = null,
)

@Serializable
data class StatusPolicy(
    val expectedPerformanceSeconds: Int = 60,
    val expectedLatencySeconds: Int = 60,
    val lookbackSeconds: Int = 900,
)

@Serializable
data class CurrentStatusSummary(
    val overall: String = "unknown",
    val liveness: String = "unknown",
    val performance: String = "no_data",
    val network: String = "no_data",
)

@Serializable
data class CurrentNetworkSummary(
    val status: String = "no_data",
    val healthy_nodes: Int = 0,
    val total_nodes: Int = 0,
    val reference_node_id: Int? = null,
    val reference_node_name: String? = null,
    val reference_latency_ms: Double? = null,
    val reference_packet_loss_pct: Double? = null,
    val reference_status: String? = null,
    val rows: List<LatencyRecord> = emptyList(),
)

@Serializable
data class CurrentServerData(
    val server: ServerSummary? = null,
    val performance: PerformanceSample? = null,
    val perf: List<PerformanceSample> = emptyList(),
    val latencies: List<LatencyRecord> = emptyList(),
    val network: CurrentNetworkSummary = CurrentNetworkSummary(),
    val status: CurrentStatusSummary = CurrentStatusSummary(),
)

@Serializable
data class CurrentStatusResponse(
    val generatedAt: String? = null,
    val policy: StatusPolicy = StatusPolicy(),
    val nodes: List<NodeSummary> = emptyList(),
    val data: Map<String, CurrentServerData> = emptyMap(),
)

@Serializable
data class ServerDetailResponse(
    val server: ServerSummary,
    val latencies: List<LatencyRecord> = emptyList(),
    val perfLogs: List<PerformanceSample> = emptyList(),
    val generatedAt: String? = null,
    val range: TimeRange? = null,
    val bucketMinutes: Int? = null,
    val current: CurrentServerData? = null,
    val policy: StatusPolicy? = null,
    val statusGrid: StatusGridPayload? = null,
)

enum class RangePreset(val id: String, val label: String, val hours: Long) {
    HOUR_1("1h", "1 小时", 1),
    DAY_1("24h", "24 小时", 24),
    DAY_3("3d", "3 天", 72),
    DAY_7("7d", "7 天", 168),
    DAY_30("30d", "30 天", 720),
}

data class OverviewMetrics(
    val serverCount: Int,
    val onlineCount: Int,
    val totalPlayers: Int,
    val avgTps: Double,
)

data class ServerCardState(
    val server: ServerSummary,
    val latestPerf: PerformanceSample?,
    val bestLatency: LatencyRecord?,
    val latencies: List<LatencyRecord> = emptyList(),
    val perf: List<PerformanceSample> = emptyList(),
    val statusGrid: StatusGridPayload? = null,
    val current: CurrentServerData? = null,
    val isOnline: Boolean,
)

data class OverviewServerSnapshot(
    val server: ServerSummary,
    val latestPerf: PerformanceSample?,
    val bestLatency: LatencyRecord?,
    val latencies: List<LatencyRecord> = emptyList(),
    val perf: List<PerformanceSample> = emptyList(),
    val statusGrid: StatusGridPayload? = null,
)

@Serializable
data class TrafficPoint(
    val bucketEpoch: Long = 0,
    val timestamp: String? = null,
    val tx: Long = 0,
    val rx: Long = 0,
    val total: Long = 0,
)

@Serializable
data class TrafficTotals(
    val tx: Long = 0,
    val rx: Long = 0,
    val total: Long = 0,
)

@Serializable
data class TrafficResponse(
    val bucketSeconds: Long = 1800,
    val start: String? = null,
    val end: String? = null,
    val data: Map<String, List<TrafficPoint>> = emptyMap(),
    val totals: Map<String, TrafficTotals> = emptyMap(),
)

@Serializable
data class FakePlayer(
    val fake_name: String,
    val server_name: String,
    val world: String? = null,
)

@Serializable
data class IpTrafficTotal(
    val ip: String,
    val tx: Long = 0,
    val rx: Long = 0,
    val total: Long = 0,
)

@Serializable
data class SearchedIpTraffic(
    val ip: String,
    val tx: Long = 0,
    val rx: Long = 0,
    val total: Long = 0,
    val points: List<TrafficPoint> = emptyList(),
)

@Serializable
data class TrafficAdminInfo(
    val userId: Int? = null,
    val username: String? = null,
    val role: String? = null,
)

@Serializable
data class TrafficAdminResponse(
    val bucketSeconds: Long = 1800,
    val start: String? = null,
    val end: String? = null,
    val source: String? = null,
    val sourceLabel: String? = null,
    val serverIds: List<Int>? = null,
    val totals: TrafficTotals = TrafficTotals(),
    val topIps: List<IpTrafficTotal> = emptyList(),
    val series: Map<String, List<TrafficPoint>> = emptyMap(),
    val searched: SearchedIpTraffic? = null,
    val admin: TrafficAdminInfo? = null,
)

@Serializable
data class McAuthMeResponse(
    val user: LoginUser? = null,
    val adminRole: String? = null,
)

enum class DetailSection(val id: String) {
    OVERVIEW("overview"),
    TPS("tps"),
    MSPT("mspt"),
    MEMORY("memory"),
    CPU("cpu"),
    PLAYERS("players"),
    LATENCY("latency"),
    LATENCY_MAX("latency-max"),
    LATENCY_MIN("latency-min"),
    PACKET_LOSS("packet-loss");

    companion object {
        fun fromId(id: String?): DetailSection =
            entries.firstOrNull { it.id == id } ?: OVERVIEW
    }
}

enum class ThemeAccent(val id: String, val label: String) {
    TEAL("teal", "青绿"),
    BLUE("blue", "蔚蓝"),
    ORANGE("orange", "橙色"),
    ROSE("rose", "玫红"),
}

data class AppSettings(
    val vibrationEnabled: Boolean = true,
    val useSystemAccent: Boolean = true,
    val accent: ThemeAccent = ThemeAccent.TEAL,
    val sessionToken: String? = null,
    val accountName: String? = null,
    val accountUsername: String? = null,
    val accounts: List<SavedAccount> = emptyList(),
)

data class SavedAccount(
    val userId: Int,
    val username: String,
    val nickname: String? = null,
    val token: String,
) {
    val displayName: String get() = nickname?.takeIf { it.isNotBlank() } ?: username
}

@Serializable data class MathCaptcha(val a: Int, val b: Int, val token: String)
@Serializable data class LoginUser(val id: Int, val username: String, val nickname: String? = null)
@Serializable data class LoginResponse(val success: Boolean, val sessionToken: String, val expiresAt: String? = null, val user: LoginUser)

private val displayFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")
    .withZone(ZoneId.systemDefault())

fun String.toDisplayTime(): String = runCatching {
    displayFormatter.format(Instant.parse(this))
}.getOrDefault(this)
