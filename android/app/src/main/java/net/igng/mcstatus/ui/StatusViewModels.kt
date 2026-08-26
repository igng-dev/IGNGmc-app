package net.igng.mcstatus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.igng.mcstatus.data.CurrentStatusResponse
import net.igng.mcstatus.data.FakePlayer
import net.igng.mcstatus.data.LatencyRecord
import net.igng.mcstatus.data.NodeSummary
import net.igng.mcstatus.data.OverviewMetrics
import net.igng.mcstatus.data.OverviewServerSnapshot
import net.igng.mcstatus.data.RangePreset
import net.igng.mcstatus.data.ServerCardState
import net.igng.mcstatus.data.ServerDetailResponse
import net.igng.mcstatus.data.ServerSummary
import net.igng.mcstatus.data.StatusRepository
import net.igng.mcstatus.data.TrafficAdminRange
import net.igng.mcstatus.data.TrafficAdminResponse
import net.igng.mcstatus.data.TrafficResponse
import net.igng.mcstatus.data.TimelineServerPayload
import net.igng.mcstatus.data.TimelineResponse

data class OverviewUiState(
    val isLoading: Boolean = true,
    val selectedRange: RangePreset = RangePreset.DAY_1,
    val servers: List<ServerSummary> = emptyList(),
    val nodes: List<NodeSummary> = emptyList(),
    val timeline: TimelineResponse = TimelineResponse(),
    val current: CurrentStatusResponse = CurrentStatusResponse(),
    val snapshots: List<OverviewServerSnapshot> = emptyList(),
    val errorMessage: String? = null,
) {
    val cards: List<ServerCardState>
        get() = snapshots.map { snapshot ->
            val currentServer = current.data[snapshot.server.server_id.toString()]
            val latestPerf = currentServer?.performance ?: snapshot.latestPerf
            val currentReference = currentServer?.network?.reference_node_id?.let { nodeId ->
                currentServer.network.rows.firstOrNull { it.node_id == nodeId }
            }
            ServerCardState(
                server = snapshot.server,
                latestPerf = latestPerf,
                bestLatency = if (currentServer != null) currentReference else snapshot.bestLatency,
                latencies = snapshot.latencies,
                perf = snapshot.perf,
                statusGrid = snapshot.statusGrid,
                current = currentServer,
                isOnline = currentServer?.status?.overall in setOf("healthy", "degraded")
                    || (currentServer == null && latestPerf != null),
            )
        }

    val nodeNames: Map<Int, String>
        get() = buildMap {
            nodes.forEach { put(it.node_id, it.node_name) }
            timeline.nodeNames.forEach { (id, name) -> id.toIntOrNull()?.let { put(it, name) } }
        }

    val metrics: OverviewMetrics
        get() {
            val latest = cards.mapNotNull { it.latestPerf }
            val tps = latest.mapNotNull { it.avg_tps }
            return OverviewMetrics(
                serverCount = servers.size,
                onlineCount = cards.count { it.isOnline },
                totalPlayers = latest.sumOf { it.online_players ?: 0 },
                avgTps = if (tps.isEmpty()) 0.0 else tps.average()
            )
        }
}

data class DetailUiState(
    val isLoading: Boolean = true,
    val selectedRange: RangePreset = RangePreset.DAY_1,
    val detail: ServerDetailResponse? = null,
    val traffic: TrafficResponse? = null,
    val trafficErrorMessage: String? = null,
    val errorMessage: String? = null,
)

class OverviewViewModel(
    private val repository: StatusRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OverviewUiState())
    val uiState: StateFlow<OverviewUiState> = _uiState.asStateFlow()
    private var refreshGeneration = 0L

    fun selectRange(preset: RangePreset) {
        if (_uiState.value.selectedRange == preset) return
        refresh(preset)
    }

    fun refresh(range: RangePreset = _uiState.value.selectedRange) {
        val generation = ++refreshGeneration
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                selectedRange = range,
                errorMessage = null
            )

            runCatching {
                val servers = repository.fetchServers()
                val nodes = runCatching { repository.fetchNodes() }.getOrDefault(emptyList())
                val timeline = repository.fetchTimeline(
                    serverIds = servers.map { it.server_id },
                    preset = range,
                )
                OverviewLoad(
                    servers = servers,
                    nodes = nodes.ifEmpty { timeline.nodes },
                    timeline = timeline,
                    current = runCatching {
                        repository.fetchCurrentStatus(servers.map { it.server_id })
                    }.getOrDefault(CurrentStatusResponse()),
                    snapshots = servers.map { server ->
                        val payload = timeline.data[server.server_id.toString()] ?: TimelineServerPayload()
                        val perf = payload.perf.sortedBy { timestampMillis(it.recorded_at) }
                        val latencies = payload.latencies.sortedBy { timestampMillis(it.timestamp_utc) }
                        OverviewServerSnapshot(
                            server = server,
                            latestPerf = perf.lastOrNull(),
                            bestLatency = chooseBestLatency(latencies),
                            latencies = latencies,
                            perf = perf,
                            statusGrid = payload.statusGrid,
                        )
                    },
                )
            }.onSuccess { load ->
                if (generation != refreshGeneration) return@onSuccess
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    servers = load.servers,
                    nodes = load.nodes,
                    timeline = load.timeline,
                    current = load.current,
                    snapshots = load.snapshots,
                )
            }.onFailure { error ->
                if (generation == refreshGeneration) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "加载失败"
                    )
                }
            }
        }
    }

    init {
        refresh()
        viewModelScope.launch {
            while (isActive) {
                delay(60_000)
                refresh()
            }
        }
    }
}

private data class OverviewLoad(
    val servers: List<ServerSummary>,
    val nodes: List<NodeSummary>,
    val timeline: TimelineResponse,
    val current: CurrentStatusResponse,
    val snapshots: List<OverviewServerSnapshot>,
)

private fun chooseBestLatency(records: List<LatencyRecord>): LatencyRecord? = records
    .groupBy { it.node_id }
    .values
    .mapNotNull { nodeRecords -> nodeRecords.maxByOrNull { timestampMillis(it.timestamp_utc) } }
    .filter { (it.avg_latency_ms ?: 0.0) > 0.0 }
    .minWithOrNull(
        compareBy<LatencyRecord> { it.avg_latency_ms ?: Double.MAX_VALUE }
            .thenByDescending { timestampMillis(it.timestamp_utc) }
    )

private fun timestampMillis(value: String): Long = runCatching {
    Instant.parse(value).toEpochMilli()
}.getOrDefault(Long.MIN_VALUE)

class DetailViewModel(
    private val repository: StatusRepository,
    private val serverId: Int,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            while (isActive) {
                delay(60_000)
                refresh()
            }
        }
    }

    fun selectRange(preset: RangePreset) {
        if (_uiState.value.selectedRange == preset) return
        refresh(preset)
    }

    fun refresh(range: RangePreset = _uiState.value.selectedRange) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                selectedRange = range,
                errorMessage = null
            )

            runCatching {
                val detail = repository.fetchServerDetail(serverId, range)
                val traffic = runCatching { repository.fetchTraffic(serverId, range) }
                DetailLoad(detail, traffic.getOrNull(), traffic.exceptionOrNull()?.message)
            }
                .onSuccess { load ->
                    _uiState.value = DetailUiState(
                        isLoading = false,
                        selectedRange = range,
                        detail = load.detail,
                        traffic = load.traffic,
                        trafficErrorMessage = load.trafficError,
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "加载失败"
                    )
                }
        }
    }
}

private data class DetailLoad(
    val detail: ServerDetailResponse,
    val traffic: TrafficResponse?,
    val trafficError: String?,
)

data class FakePlayersUiState(
    val isLoading: Boolean = true,
    val players: List<FakePlayer> = emptyList(),
    val errorMessage: String? = null,
)

class FakePlayersViewModel(
    private val repository: StatusRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FakePlayersUiState())
    val uiState: StateFlow<FakePlayersUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { repository.fetchFakePlayers() }
                .onSuccess { players ->
                    _uiState.value = FakePlayersUiState(isLoading = false, players = players)
                }
                .onFailure { error ->
                    _uiState.value = FakePlayersUiState(
                        isLoading = false,
                        errorMessage = error.message ?: "假人列表加载失败",
                    )
                }
        }
    }
}

data class StatusAdminAccessUiState(
    val isLoading: Boolean = false,
    val adminRole: String? = null,
)

class StatusAdminAccessViewModel(
    private val repository: StatusRepository,
    private val token: String?,
) : ViewModel() {
    private val _uiState = MutableStateFlow(StatusAdminAccessUiState())
    val uiState: StateFlow<StatusAdminAccessUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (token.isNullOrBlank()) {
            _uiState.value = StatusAdminAccessUiState()
            return
        }
        viewModelScope.launch {
            _uiState.value = StatusAdminAccessUiState(isLoading = true)
            runCatching { repository.fetchMcAuthMe(token) }
                .onSuccess { response ->
                    _uiState.value = StatusAdminAccessUiState(adminRole = response.adminRole)
                }
                .onFailure {
                    _uiState.value = StatusAdminAccessUiState()
                }
        }
    }
}

data class TrafficAdminUiState(
    val isLoading: Boolean = true,
    val selectedRange: TrafficAdminRange = TrafficAdminRange.HOUR_1,
    val selectedServerId: Int? = null,
    val source: String = "backend",
    val metric: String = "total",
    val mode: String = "interval",
    val searchInput: String = "",
    val activeSearch: String = "",
    val payload: TrafficAdminResponse? = null,
    val errorMessage: String? = null,
)

class TrafficAdminViewModel(
    private val repository: StatusRepository,
    private val token: String?,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TrafficAdminUiState())
    val uiState: StateFlow<TrafficAdminUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun selectRange(range: TrafficAdminRange) {
        _uiState.value = _uiState.value.copy(selectedRange = range)
        refresh()
    }

    fun selectServer(serverId: Int?) {
        _uiState.value = _uiState.value.copy(selectedServerId = serverId)
        refresh()
    }

    fun selectSource(source: String) {
        _uiState.value = _uiState.value.copy(source = source)
        refresh()
    }

    fun selectMetric(metric: String) {
        _uiState.value = _uiState.value.copy(metric = metric)
    }

    fun selectMode(mode: String) {
        _uiState.value = _uiState.value.copy(mode = mode)
    }

    fun setSearchInput(value: String) {
        _uiState.value = _uiState.value.copy(searchInput = value)
    }

    fun submitSearch() {
        _uiState.value = _uiState.value.copy(activeSearch = _uiState.value.searchInput.trim())
        refresh()
    }

    fun clearSearch() {
        _uiState.value = _uiState.value.copy(searchInput = "", activeSearch = "")
        refresh()
    }

    fun refresh() {
        val snapshot = _uiState.value
        if (token.isNullOrBlank()) {
            _uiState.value = snapshot.copy(isLoading = false, errorMessage = "请先登录 IGNG 账户")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                repository.fetchAdminTraffic(
                    token = token,
                    preset = snapshot.selectedRange,
                    serverId = snapshot.selectedServerId,
                    source = snapshot.source,
                    ip = snapshot.activeSearch.takeIf { it.isNotBlank() },
                )
            }.onSuccess { payload ->
                _uiState.value = _uiState.value.copy(isLoading = false, payload = payload)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    payload = null,
                    errorMessage = error.message ?: "流量明细加载失败",
                )
            }
        }
    }
}

class OverviewViewModelFactory(
    private val repository: StatusRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return OverviewViewModel(repository) as T
    }
}

class FakePlayersViewModelFactory(
    private val repository: StatusRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return FakePlayersViewModel(repository) as T
    }
}

class StatusAdminAccessViewModelFactory(
    private val repository: StatusRepository,
    private val token: String?,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return StatusAdminAccessViewModel(repository, token) as T
    }
}

class TrafficAdminViewModelFactory(
    private val repository: StatusRepository,
    private val token: String?,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return TrafficAdminViewModel(repository, token) as T
    }
}

class DetailViewModelFactory(
    private val repository: StatusRepository,
    private val serverId: Int,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return DetailViewModel(repository, serverId) as T
    }
}
