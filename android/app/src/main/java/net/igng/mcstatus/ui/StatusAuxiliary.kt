@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.igng.mcstatus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import net.igng.mcstatus.data.AppSettings
import net.igng.mcstatus.data.FakePlayer
import net.igng.mcstatus.data.IpTrafficTotal
import net.igng.mcstatus.data.ServerSummary
import net.igng.mcstatus.data.SearchedIpTraffic
import net.igng.mcstatus.data.TrafficAdminRange
import net.igng.mcstatus.data.TrafficAdminResponse
import net.igng.mcstatus.data.TrafficPoint
import net.igng.mcstatus.data.TrafficResponse

@Composable
fun FakePlayersScreen(
    uiState: FakePlayersUiState,
    settings: AppSettings,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val normalizedQuery = query.trim().lowercase()
    val filteredPlayers = uiState.players.filter { player ->
        normalizedQuery.isBlank() || listOf(player.fake_name, player.server_name, player.world.orEmpty())
            .any { it.lowercase().contains(normalizedQuery) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("假人列表") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onRetry) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "刷新")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("搜索假人、服务器或世界") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Rounded.Clear, contentDescription = "清除")
                            }
                        }
                    },
                )
            }
            if (uiState.isLoading && uiState.players.isEmpty()) {
                item { AuxiliaryLoading() }
            } else if (uiState.errorMessage != null && uiState.players.isEmpty()) {
                item { AuxiliaryError(uiState.errorMessage, onRetry) }
            } else if (filteredPlayers.isEmpty()) {
                item {
                    AuxiliaryEmpty(if (query.isBlank()) "当前没有假人数据" else "没有匹配的假人")
                }
            } else {
                items(filteredPlayers, key = { "${it.server_name}:${it.fake_name}:${it.world}" }) { player ->
                    FakePlayerCard(player)
                }
            }
        }
    }
}

@Composable
private fun FakePlayerCard(player: FakePlayer) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text("假", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(player.fake_name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    player.server_name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                player.world?.takeIf { it.isNotBlank() } ?: "未知世界",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun TrafficAdminScreen(
    uiState: TrafficAdminUiState,
    servers: List<ServerSummary>,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSelectRange: (TrafficAdminRange) -> Unit,
    onSelectServer: (Int?) -> Unit,
    onSelectSource: (String) -> Unit,
    onSelectMetric: (String) -> Unit,
    onSelectMode: (String) -> Unit,
    onSearchInput: (String) -> Unit,
    onSubmitSearch: () -> Unit,
    onClearSearch: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("流量明细") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onRetry) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "刷新")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                TrafficFilterSection(
                        uiState = uiState,
                    servers = servers,
                    onSelectRange = onSelectRange,
                    onSelectServer = onSelectServer,
                    onSelectSource = onSelectSource,
                    onSelectMetric = onSelectMetric,
                    onSelectMode = onSelectMode,
                )
            }
            item {
                OutlinedTextField(
                    value = uiState.searchInput,
                    onValueChange = onSearchInput,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("查询 IP（可选）") },
                    placeholder = { Text("例如 192.168.1.10") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        Row {
                            if (uiState.searchInput.isNotEmpty()) {
                                IconButton(onClick = onClearSearch) {
                                    Icon(Icons.Rounded.Clear, contentDescription = "清除")
                                }
                            }
                            IconButton(onClick = onSubmitSearch) {
                                Icon(Icons.Rounded.Search, contentDescription = "查询")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSubmitSearch() }),
                )
            }
            if (uiState.isLoading && uiState.payload == null) {
                item { AuxiliaryLoading() }
            }
            uiState.errorMessage?.let { message ->
                item { AuxiliaryError(message, onRetry) }
            }
            uiState.payload?.let { payload ->
                item { TrafficTotalsCard(payload.totals, subtitle = "${payload.sourceLabel ?: payload.source ?: "后端统计"} · ${payload.admin?.role ?: "管理员"}") }
                payload.searched?.let { searched ->
                    item { SearchedIpCard(searched) }
                }
                item {
                    TrafficIpRanking(
                        topIps = payload.topIps,
                        metric = uiState.metric,
                        series = payload.series,
                        mode = uiState.mode,
                    )
                }
            }
        }
    }
}

@Composable
private fun TrafficFilterSection(
    uiState: TrafficAdminUiState,
    servers: List<ServerSummary>,
    onSelectRange: (TrafficAdminRange) -> Unit,
    onSelectServer: (Int?) -> Unit,
    onSelectSource: (String) -> Unit,
    onSelectMetric: (String) -> Unit,
    onSelectMode: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("时间范围", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        HorizontalChipRow {
            TrafficAdminRange.entries.forEach { range ->
                FilterChip(
                    selected = uiState.selectedRange == range,
                    onClick = { onSelectRange(range) },
                    label = { Text(range.label) },
                )
            }
        }
        Text("服务器", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        HorizontalChipRow {
            FilterChip(
                selected = uiState.selectedServerId == null,
                onClick = { onSelectServer(null) },
                label = { Text("全部") },
            )
            servers.forEach { server ->
                FilterChip(
                    selected = uiState.selectedServerId == server.server_id,
                    onClick = { onSelectServer(server.server_id) },
                    label = { Text(server.server_name) },
                )
            }
        }
        Text("数据源", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        HorizontalChipRow {
            listOf("backend" to "后端统计", "velocity" to "VC 统计").forEach { (source, label) ->
                FilterChip(
                    selected = uiState.source == source,
                    onClick = { onSelectSource(source) },
                    label = { Text(label) },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("排序", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            listOf("total" to "总量", "tx" to "上行", "rx" to "下行").forEach { (metric, label) ->
                FilterChip(
                    selected = uiState.metric == metric,
                    onClick = { onSelectMetric(metric) },
                    label = { Text(label) },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("曲线", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            listOf("interval" to "区间", "cumulative" to "累计").forEach { (mode, label) ->
                FilterChip(
                    selected = uiState.mode == mode,
                    onClick = { onSelectMode(mode) },
                    label = { Text(label) },
                )
            }
        }
    }
}

@Composable
private fun HorizontalChipRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
fun PublicTrafficPanel(
    traffic: TrafficResponse?,
    errorMessage: String?,
) {
    var metric by rememberSaveable { mutableStateOf("total") }
    var mode by rememberSaveable { mutableStateOf("interval") }
    val points = traffic?.data?.values?.flatten()?.sortedBy { it.bucketEpoch }.orEmpty()
    val totals = traffic?.totals?.values?.fold(net.igng.mcstatus.data.TrafficTotals()) { sum, value ->
        net.igng.mcstatus.data.TrafficTotals(
            tx = sum.tx + value.tx,
            rx = sum.rx + value.rx,
            total = sum.total + value.total,
        )
    } ?: net.igng.mcstatus.data.TrafficTotals()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("公开流量", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        if (errorMessage != null && traffic == null) {
            AuxiliaryError(errorMessage, onRetry = null)
        } else {
            TrafficTotalsCard(totals, subtitle = "仅展示聚合数据，不包含 IP")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("total" to "总量", "tx" to "上行", "rx" to "下行").forEach { (value, label) ->
                    FilterChip(selected = metric == value, onClick = { metric = value }, label = { Text(label) })
                }
                FilterChip(
                    selected = mode == "cumulative",
                    onClick = { mode = if (mode == "cumulative") "interval" else "cumulative" },
                    label = { Text(if (mode == "cumulative") "累计" else "区间") },
                )
            }
            TrafficPointBars(points, metric, mode)
        }
    }
}

@Composable
private fun TrafficTotalsCard(
    totals: net.igng.mcstatus.data.TrafficTotals,
    subtitle: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("流量汇总", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TrafficTotalTile("总量", totals.total, Modifier.weight(1f))
                TrafficTotalTile("上行", totals.tx, Modifier.weight(1f))
                TrafficTotalTile("下行", totals.rx, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TrafficTotalTile(label: String, bytes: Long, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatTrafficBytes(bytes), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TrafficIpRanking(
    topIps: List<IpTrafficTotal>,
    metric: String,
    series: Map<String, List<TrafficPoint>>,
    mode: String,
) {
    val sorted = topIps.sortedByDescending { trafficMetric(it.tx, it.rx, it.total, metric) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("IP 排名", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (sorted.isEmpty()) {
                Text("当前范围内没有 IP 流量数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val maxValue = sorted.maxOf { trafficMetric(it.tx, it.rx, it.total, metric) }.coerceAtLeast(1L)
                sorted.forEachIndexed { index, item ->
                    val value = trafficMetric(item.tx, item.rx, item.total, metric)
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${index + 1}. ${item.ip}", maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Text(formatTrafficBytes(value), fontWeight = FontWeight.Bold)
                        }
                        TrafficBar(value.toFloat() / maxValue, index)
                        val points = series[item.ip].orEmpty()
                        if (points.isNotEmpty()) {
                            val pointValue = if (mode == "cumulative") {
                                points.sumOf { point -> trafficMetric(point.tx, point.rx, point.total, metric) }
                            } else {
                                trafficMetric(points.last().tx, points.last().rx, points.last().total, metric)
                            }
                            Text(
                                "${if (mode == "cumulative") "累计" else "区间"} ${points.size} 个采样点 · ${formatTrafficBytes(pointValue)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchedIpCard(searched: SearchedIpTraffic) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("查询结果", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(searched.ip, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(
                "总量 ${formatTrafficBytes(searched.total)} · 上行 ${formatTrafficBytes(searched.tx)} · 下行 ${formatTrafficBytes(searched.rx)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun TrafficPointBars(points: List<TrafficPoint>, metric: String, mode: String) {
    if (points.isEmpty()) {
        AuxiliaryEmpty("当前范围内暂无公开流量数据")
        return
    }
    val values = if (mode == "cumulative") {
        var total = 0L
        points.map { point ->
            total += trafficMetric(point.tx, point.rx, point.total, metric)
            total
        }
    } else {
        points.map { trafficMetric(it.tx, it.rx, it.total, metric) }
    }
    val maxValue = values.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("流量趋势", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            values.takeLast(24).forEachIndexed { index, value ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${index + 1}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(22.dp))
                    TrafficBar(value.toFloat() / maxValue, index, Modifier.weight(1f))
                    Text(formatTrafficBytes(value), style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(72.dp))
                }
            }
        }
    }
}

@Composable
private fun TrafficBar(fraction: Float, index: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(9.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(99.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                .fillMaxSize()
                .clip(RoundedCornerShape(99.dp))
                .background(if (index % 2 == 0) Color(0xFF2AA889) else Color(0xFF4E8DF7)),
        )
    }
}

private fun trafficMetric(tx: Long, rx: Long, total: Long, metric: String): Long = when (metric) {
    "tx" -> tx
    "rx" -> rx
    else -> total
}

private fun formatTrafficBytes(bytes: Long): String {
    val value = bytes.toDouble().coerceAtLeast(0.0)
    return when {
        value >= 1024 * 1024 * 1024 -> "%.2f GB".format(value / (1024 * 1024 * 1024))
        value >= 1024 * 1024 -> "%.2f MB".format(value / (1024 * 1024))
        value >= 1024 -> "%.1f KB".format(value / 1024)
        else -> "$bytes B"
    }
}

@Composable
private fun AuxiliaryLoading() {
    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun AuxiliaryError(message: String, onRetry: (() -> Unit)?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
            if (onRetry != null) {
                OutlinedButton(onClick = onRetry) { Text("重试") }
            }
        }
    }
}

@Composable
private fun AuxiliaryEmpty(message: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
