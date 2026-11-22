package net.igng.mcstatus.feature.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import net.igng.mcstatus.core.auth.CustomTabsLauncher
import net.igng.mcstatus.core.data.AdminCurrentNodesResponse
import net.igng.mcstatus.core.data.AdminPlayerResultDto
import net.igng.mcstatus.core.data.AdminPunishmentsResponse
import net.igng.mcstatus.core.data.AdminRepository
import net.igng.mcstatus.core.data.HistorianDetailResponse
import net.igng.mcstatus.core.data.HistorianListResponse
import net.igng.mcstatus.core.data.TicketDetailDto
import net.igng.mcstatus.core.data.TicketSummaryDto
import net.igng.mcstatus.core.data.TrafficAdminResponse
import net.igng.mcstatus.core.designsystem.IgngAmbientCanvas
import net.igng.mcstatus.core.designsystem.IgngBadge
import net.igng.mcstatus.core.designsystem.IgngBadgeTone
import net.igng.mcstatus.core.designsystem.IgngBarMetric
import net.igng.mcstatus.core.designsystem.IgngButton
import net.igng.mcstatus.core.designsystem.IgngButtonStyle
import net.igng.mcstatus.core.designsystem.IgngDetailSheet
import net.igng.mcstatus.core.designsystem.IgngHorizontalBarList
import net.igng.mcstatus.core.designsystem.IgngInteractiveTile
import net.igng.mcstatus.core.designsystem.IgngLoadingBlock
import net.igng.mcstatus.core.designsystem.IgngSearchField
import net.igng.mcstatus.core.designsystem.IgngSectionHeader
import net.igng.mcstatus.core.designsystem.IgngSegmentedTabs
import net.igng.mcstatus.core.designsystem.IgngSkeletonRows
import net.igng.mcstatus.core.designsystem.IgngStateBlock
import net.igng.mcstatus.core.designsystem.IgngTextField
import net.igng.mcstatus.core.designsystem.IgngTheme
import net.igng.mcstatus.core.designsystem.IgngTile
import net.igng.mcstatus.core.designsystem.IgngTopBar
import net.igng.mcstatus.core.designsystem.igngStaggeredReveal
import net.igng.mcstatus.core.model.IgngFormatters
import net.igng.mcstatus.core.model.McAdminRole
import net.igng.mcstatus.core.model.SiteEndpoints

private enum class AdminSection(val label: String, val elevatedOnly: Boolean = false) {
    TICKETS("工单"),
    AUDIT("玩家审计"),
    HISTORIAN("观察周报"),
    OPS("运维流量", elevatedOnly = true),
}

@Composable
fun AdminPocketConsoleScreen(
    role: McAdminRole,
    endpoints: SiteEndpoints,
    adminRepository: AdminRepository,
    onBack: () -> Unit,
) {
    val colors = IgngTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val availableSections = remember(role) {
        AdminSection.entries.filter { !it.elevatedOnly || role.isElevated() }
    }
    var section by remember { mutableStateOf(AdminSection.TICKETS) }

    var adminTickets by remember { mutableStateOf<List<TicketSummaryDto>>(emptyList()) }
    var inspectedTicket by remember { mutableStateOf<TicketDetailDto?>(null) }
    var adminReplyDraft by remember { mutableStateOf("") }

    var playerQuery by remember { mutableStateOf("") }
    var playerResults by remember { mutableStateOf<List<AdminPlayerResultDto>>(emptyList()) }
    var punishments by remember { mutableStateOf<AdminPunishmentsResponse?>(null) }

    var historianList by remember { mutableStateOf<HistorianListResponse?>(null) }
    var historianDetail by remember { mutableStateOf<HistorianDetailResponse?>(null) }
    var loadingHistorianDetail by remember { mutableStateOf(false) }

    var nodesData by remember { mutableStateOf<AdminCurrentNodesResponse?>(null) }
    var trafficData by remember { mutableStateOf<TrafficAdminResponse?>(null) }
    var trafficRangeHours by remember { mutableStateOf(24L) }

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun loadSection() {
        loading = true
        error = null
        runCatching {
            when (section) {
                AdminSection.TICKETS -> {
                    adminTickets = adminRepository.fetchAdminTickets(includeClosed = false).resolvedTickets
                }
                AdminSection.AUDIT -> {
                    punishments = adminRepository.fetchRecentPunishments(search = playerQuery)
                }
                AdminSection.HISTORIAN -> {
                    historianList = adminRepository.fetchHistorianReports()
                }
                AdminSection.OPS -> {
                    val nDef = scope.async { runCatching { adminRepository.fetchAdminCurrentNodes() }.getOrNull() }
                    val tDef = scope.async { runCatching { adminRepository.fetchAdminTraffic(rangeHours = trafficRangeHours) }.getOrNull() }
                    nodesData = nDef.await()
                    trafficData = tDef.await()
                }
            }
        }.onFailure { error = it.message ?: "管理台数据暂时不可用" }
        loading = false
    }

    LaunchedEffect(section, trafficRangeHours) { loadSection() }

    IgngAmbientCanvas {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            IgngTopBar(
                title = "管理控制台",
                subtitle = "${role.label} · 移动端高频运维",
                onBack = onBack,
                actions = {
                    IgngButton(
                        text = "网页版",
                        onClick = { CustomTabsLauncher.openExternalUrl(context, "${endpoints.dash}/mc/admin", colors.isDark) },
                        style = IgngButtonStyle.Secondary,
                        icon = Icons.Rounded.OpenInBrowser,
                        compact = true,
                    )
                },
            )

            Box(Modifier.padding(horizontal = 16.dp)) {
                IgngSegmentedTabs(availableSections, section, { it.label }, { section = it })
            }
            Spacer(Modifier.height(10.dp))

            when {
                loading -> Box(Modifier.padding(16.dp)) { IgngSkeletonRows(4) }
                error != null -> Box(Modifier.padding(16.dp)) {
                    IgngStateBlock(title = "读取失败", description = error, isError = true, actionLabel = "重试", onAction = { scope.launch { loadSection() } })
                }
                section == AdminSection.TICKETS -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (adminTickets.isEmpty()) {
                        item { IgngStateBlock(title = "一切正常", description = "当前没有待处理的全站工单。") }
                    } else {
                        itemsIndexed(adminTickets, key = { _, t -> t.id }) { idx, t ->
                            IgngInteractiveTile(
                                onClick = {
                                    scope.launch {
                                        adminReplyDraft = ""
                                        inspectedTicket = runCatching { adminRepository.fetchAdminTicketDetail(t.id) }.getOrNull()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().igngStaggeredReveal(idx),
                            ) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("#${t.id} ${t.title}", style = IgngTheme.type.cardTitle, color = colors.textPrimary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(Modifier.width(8.dp))
                                    IgngBadge(t.typeLabel.ifBlank { t.type }, tone = IgngBadgeTone.Accent)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text("${t.creatorName} · ${t.serverName ?: "全服"} · ${IgngFormatters.formatRelativeTime(t.updatedAt)}", style = IgngTheme.type.caption, color = colors.textSecondary)
                            }
                        }
                    }
                }
                section == AdminSection.AUDIT -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        IgngSearchField(
                            value = playerQuery,
                            onValueChange = { playerQuery = it },
                            placeholder = "输入玩家名或账号搜索审计记录，回车查询",
                            onSubmit = {
                                scope.launch {
                                    if (playerQuery.trim().length >= 2) {
                                        playerResults = runCatching { adminRepository.searchAdminPlayers(playerQuery) }.getOrDefault(emptyList())
                                    }
                                    punishments = runCatching { adminRepository.fetchRecentPunishments(search = playerQuery) }.getOrNull()
                                }
                            },
                        )
                    }
                    if (playerResults.isNotEmpty()) {
                        item { IgngSectionHeader(title = "匹配玩家") }
                        items(playerResults, key = { "${it.igngId}:${it.mcUsername}:${it.username}" }) { p ->
                            IgngTile(modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(p.mcUsername ?: p.nickname ?: p.username ?: "玩家", style = IgngTheme.type.cardTitle, color = colors.textPrimary)
                                    if (p.isBanned) IgngBadge("封禁中", tone = IgngBadgeTone.Danger)
                                    else if (p.isMuted) IgngBadge("禁言中", tone = IgngBadgeTone.Warning)
                                    else IgngBadge("正常", tone = IgngBadgeTone.Success)
                                }
                                if (p.boundAccounts.isNotEmpty()) {
                                    Text("绑定角色：${p.boundAccounts.joinToString("、")}", style = IgngTheme.type.caption, color = colors.textSecondary)
                                }
                            }
                        }
                    }
                    item { IgngSectionHeader(title = "近期处罚记录", subtitle = punishments?.let { "共 ${it.total} 条" }) }
                    val recs = punishments?.records.orEmpty()
                    if (recs.isEmpty()) {
                        item { IgngStateBlock(title = "没有匹配的处罚记录") }
                    } else {
                        itemsIndexed(recs, key = { i, r -> "${r.id}:$i" }) { idx, r ->
                            IgngTile(modifier = Modifier.fillMaxWidth().igngStaggeredReveal(idx)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(r.player_name ?: "未知玩家", style = IgngTheme.type.cardTitle, color = colors.textPrimary)
                                    IgngBadge(r.type ?: "处罚", tone = IgngBadgeTone.Warning)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(r.reason ?: "无理由说明", style = IgngTheme.type.body, color = colors.textSecondary)
                                Text("执行人：${r.admin_name ?: r.banned_by_name ?: "系统"} · ${IgngFormatters.formatIsoDateTime(r.started_at, short = true)}", style = IgngTheme.type.badge, color = colors.textMuted)
                            }
                        }
                    }
                }
                section == AdminSection.HISTORIAN -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val serverNames = historianList?.servers.orEmpty().associate { it.server_id to it.server_name }
                    val reports = historianList?.items.orEmpty()
                    if (reports.isEmpty()) {
                        item { IgngStateBlock(title = "还没有服务器观察周报") }
                    } else {
                        itemsIndexed(reports, key = { _, r -> r.id }) { idx, report ->
                            IgngInteractiveTile(
                                onClick = {
                                    scope.launch {
                                        loadingHistorianDetail = true
                                        historianDetail = runCatching { adminRepository.fetchHistorianDetail(report.id) }.getOrNull()
                                        loadingHistorianDetail = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().igngStaggeredReveal(idx),
                            ) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        text = "${serverNames[report.server_id] ?: "服务器 ${report.server_id}"} · ${report.period_start}",
                                        style = IgngTheme.type.cardTitle,
                                        color = colors.textPrimary,
                                    )
                                    IgngBadge(report.status ?: report.period_kind, tone = if (report.status == "succeeded" || report.status == "completed") IgngBadgeTone.Success else IgngBadgeTone.Neutral)
                                }
                                report.summary?.takeIf { it.isNotBlank() }?.let { summary ->
                                    Spacer(Modifier.height(4.dp))
                                    Text(summary, style = IgngTheme.type.body, color = colors.textSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
                section == AdminSection.OPS -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        IgngSegmentedTabs(
                            items = listOf(1L, 0L, 24L, 168L),
                            selected = trafficRangeHours,
                            labelOf = {
                                when (it) {
                                    1L -> "1小时"
                                    0L -> "今天"
                                    24L -> "24小时"
                                    else -> "近7天"
                                }
                            },
                            onSelect = { trafficRangeHours = it },
                        )
                    }
                    trafficData?.let { tf ->
                        item {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IgngTile(Modifier.weight(1f), contentPadding = PaddingValues(12.dp)) {
                                    Text("总流量", style = IgngTheme.type.badge, color = colors.textSecondary)
                                    Text(IgngFormatters.formatBytes(tf.totals.total), style = IgngTheme.type.metricMedium, color = colors.accent)
                                }
                                IgngTile(Modifier.weight(1f), contentPadding = PaddingValues(12.dp)) {
                                    Text("出站 TX", style = IgngTheme.type.badge, color = colors.textSecondary)
                                    Text(IgngFormatters.formatBytes(tf.totals.tx), style = IgngTheme.type.metricMedium, color = colors.textPrimary)
                                }
                                IgngTile(Modifier.weight(1f), contentPadding = PaddingValues(12.dp)) {
                                    Text("入站 RX", style = IgngTheme.type.badge, color = colors.textSecondary)
                                    Text(IgngFormatters.formatBytes(tf.totals.rx), style = IgngTheme.type.metricMedium, color = colors.textPrimary)
                                }
                            }
                        }
                        if (tf.topIps.isNotEmpty()) {
                            item {
                                IgngTile(modifier = Modifier.fillMaxWidth()) {
                                    IgngSectionHeader(title = "Top 来源 IP 流量", subtitle = tf.sourceLabel ?: "后端流量")
                                    Spacer(Modifier.height(10.dp))
                                    val maxTotal = tf.topIps.maxOf { it.total }.coerceAtLeast(1L).toFloat()
                                    IgngHorizontalBarList(
                                        items = tf.topIps.take(10).map {
                                            IgngBarMetric(
                                                label = it.ip,
                                                formattedValue = IgngFormatters.formatBytes(it.total),
                                                ratio = it.total / maxTotal,
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }
                    nodesData?.let { nd ->
                        item { IgngSectionHeader(title = "探针与服务器实时健康", subtitle = "${nd.nodes.size} 个采集节点") }
                        items(nd.data.values.toList(), key = { it.server?.server_id ?: it.hashCode() }) { srv ->
                            IgngTile(modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(srv.server?.server_name ?: "服务器", style = IgngTheme.type.cardTitle, color = colors.textPrimary)
                                    val overall = srv.status?.overall ?: "unknown"
                                    IgngBadge(
                                        overall,
                                        tone = when (overall) {
                                            "healthy" -> IgngBadgeTone.Success
                                            "degraded" -> IgngBadgeTone.Warning
                                            else -> IgngBadgeTone.Danger
                                        },
                                    )
                                }
                                Text(
                                    "TPS ${IgngFormatters.formatDecimal(srv.performance?.avg_tps, 1)} · 玩家 ${IgngFormatters.formatNumber(srv.performance?.online_players)} · 网络 ${srv.status?.network ?: "—"}",
                                    style = IgngTheme.type.caption,
                                    color = colors.textSecondary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    inspectedTicket?.let { t ->
        IgngDetailSheet(
            onDismiss = { inspectedTicket = null },
            title = "#${t.id} ${t.title}",
            subtitle = "管理员快速处置 · ${t.creatorName}",
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                t.messages.takeLast(6).forEach { m ->
                    IgngTile(soft = true, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(10.dp)) {
                        Text("${m.authorName} · ${IgngFormatters.formatIsoDateTime(m.createdAt, short = true)}", style = IgngTheme.type.badge, color = colors.textSecondary)
                        Spacer(Modifier.height(3.dp))
                        Text(m.content, style = IgngTheme.type.body, color = colors.textPrimary)
                    }
                }
                IgngTextField(value = adminReplyDraft, onValueChange = { adminReplyDraft = it }, label = "管理员回复", singleLine = false, minLines = 3)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IgngButton(
                        text = "发送回复",
                        enabled = adminReplyDraft.isNotBlank(),
                        onClick = {
                            scope.launch {
                                runCatching { adminRepository.replyAdminTicket(t.id, adminReplyDraft.trim()) }
                                    .onSuccess {
                                        adminReplyDraft = ""
                                        inspectedTicket = runCatching { adminRepository.fetchAdminTicketDetail(t.id) }.getOrNull()
                                    }
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                    IgngButton(
                        text = if (t.status.equals("CLOSED", true)) "重开" else "结单",
                        style = if (t.status.equals("CLOSED", true)) IgngButtonStyle.Secondary else IgngButtonStyle.Danger,
                        onClick = {
                            scope.launch {
                                runCatching {
                                    if (t.status.equals("CLOSED", true)) adminRepository.reopenAdminTicket(t.id)
                                    else adminRepository.closeAdminTicket(t.id)
                                }.onSuccess {
                                    inspectedTicket = null
                                    loadSection()
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    if (loadingHistorianDetail || historianDetail != null) {
        IgngDetailSheet(
            onDismiss = { historianDetail = null; loadingHistorianDetail = false },
            title = historianDetail?.report?.let { "周报 ${it.period_start}" } ?: "服务器观察报告",
            subtitle = historianDetail?.run?.created_at?.let { "生成于 ${IgngFormatters.formatIsoDateTime(it, short = true)}" },
        ) {
            if (loadingHistorianDetail) {
                IgngLoadingBlock(compact = true)
            } else {
                val run = historianDetail?.run
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val bodyText = run?.report_markdown ?: run?.markdown ?: run?.summary ?: "暂无正文"
                    Text(bodyText, style = IgngTheme.type.body, color = colors.textPrimary)
                    if (!run?.observations.isNullOrEmpty()) {
                        IgngSectionHeader(title = "关键观察项")
                        run?.observations.orEmpty().forEach { obs ->
                            IgngTile(soft = true, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(10.dp)) {
                                Text(obs.title ?: obs.category ?: "观察", style = IgngTheme.type.bodyMedium.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                                Text(obs.detail ?: obs.summary ?: "", style = IgngTheme.type.caption, color = colors.textSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}
