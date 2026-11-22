package net.igng.mcstatus.feature.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.LockReset
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import net.igng.mcstatus.core.auth.CustomTabsLauncher
import net.igng.mcstatus.core.data.BoundMcAccountDto
import net.igng.mcstatus.core.data.CustomMedalSlotDto
import net.igng.mcstatus.core.data.McCharacterDetailsResponse
import net.igng.mcstatus.core.data.MedalProfileDto
import net.igng.mcstatus.core.data.PermissionInsightsResponse
import net.igng.mcstatus.core.data.PersonalPerfectResponse
import net.igng.mcstatus.core.data.PlayerFakePlayerDto
import net.igng.mcstatus.core.data.PlayerLandDto
import net.igng.mcstatus.core.data.PlayerLoginRecordDto
import net.igng.mcstatus.core.data.PlayerPermissionsResponse
import net.igng.mcstatus.core.data.PlayerRepository
import net.igng.mcstatus.core.data.PlayerTeleportDto
import net.igng.mcstatus.core.data.VanillaStatsResponse
import net.igng.mcstatus.core.data.VaultHistoryResponse
import net.igng.mcstatus.core.data.VaultItemGroupDto
import net.igng.mcstatus.core.data.VaultItemsResponse
import net.igng.mcstatus.core.data.VaultOverviewResponse
import net.igng.mcstatus.core.data.VaultTraceResponse
import net.igng.mcstatus.core.designsystem.IgngAmbientCanvas
import net.igng.mcstatus.core.designsystem.IgngBadge
import net.igng.mcstatus.core.designsystem.IgngBadgeTone
import net.igng.mcstatus.core.designsystem.IgngBarMetric
import net.igng.mcstatus.core.designsystem.IgngButton
import net.igng.mcstatus.core.designsystem.IgngButtonStyle
import net.igng.mcstatus.core.designsystem.IgngConfirmDialog
import net.igng.mcstatus.core.designsystem.IgngCopyPill
import net.igng.mcstatus.core.designsystem.IgngDetailSheet
import net.igng.mcstatus.core.designsystem.IgngHorizontalBarList
import net.igng.mcstatus.core.designsystem.IgngInteractiveTile
import net.igng.mcstatus.core.designsystem.IgngLoadingBlock
import net.igng.mcstatus.core.designsystem.IgngMcFormattedText
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
import net.igng.mcstatus.core.model.CustomMedalValidator
import net.igng.mcstatus.core.model.IgngFormatters
import net.igng.mcstatus.core.model.MedalValidationResult
import net.igng.mcstatus.core.model.SiteEndpoints
import net.igng.mcstatus.core.model.asBooleanFlag

// ==================== 1. Characters ====================

@Composable
fun CharactersScreen(
    endpoints: SiteEndpoints,
    playerRepository: PlayerRepository,
    onBack: () -> Unit,
) {
    val colors = IgngTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var accounts by remember { mutableStateOf<List<BoundMcAccountDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }

    var detailTarget by remember { mutableStateOf<BoundMcAccountDto?>(null) }
    var detailData by remember { mutableStateOf<McCharacterDetailsResponse?>(null) }
    var detailLoading by remember { mutableStateOf(false) }

    var resetTarget by remember { mutableStateOf<BoundMcAccountDto?>(null) }
    var currentCred by remember { mutableStateOf("") }
    var nextCred by remember { mutableStateOf("") }
    var resetting by remember { mutableStateOf(false) }
    var resetError by remember { mutableStateOf<String?>(null) }

    var unbindTarget by remember { mutableStateOf<BoundMcAccountDto?>(null) }

    suspend fun reload() {
        loading = true
        error = null
        runCatching { playerRepository.fetchBoundAccounts() }
            .onSuccess { accounts = it }
            .onFailure { error = it.message ?: "角色列表没有读到" }
        loading = false
    }

    LaunchedEffect(Unit) { reload() }

    IgngAmbientCanvas {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            IgngTopBar(
                title = "我的角色",
                subtitle = "管理绑定的 Minecraft 角色、密码与公开性",
                onBack = onBack,
                actions = {
                    IgngButton(
                        text = "绑定新角色",
                        onClick = { CustomTabsLauncher.openExternalUrl(context, "${endpoints.dash}/mc/characters", colors.isDark) },
                        style = IgngButtonStyle.Secondary,
                        icon = Icons.Rounded.OpenInBrowser,
                        compact = true,
                    )
                },
            )
            feedback?.let { msg ->
                Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    IgngBadge(msg, tone = IgngBadgeTone.Success)
                }
            }
            when {
                loading -> Box(Modifier.padding(16.dp)) { IgngSkeletonRows(3) }
                error != null -> Box(Modifier.padding(16.dp)) {
                    IgngStateBlock(title = "读取失败", description = error, isError = true, actionLabel = "重试", onAction = { scope.launch { reload() } })
                }
                accounts.isEmpty() -> Box(Modifier.padding(16.dp)) {
                    IgngStateBlock(
                        title = "还没有绑定 Minecraft 角色",
                        description = "前往网页端完成人机验证并验证游戏密码即可绑定。",
                        actionLabel = "前往绑定角色",
                        onAction = { CustomTabsLauncher.openExternalUrl(context, "${endpoints.dash}/mc/characters", colors.isDark) },
                    )
                }
                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(accounts, key = { _, a -> a.id }) { idx, acc ->
                        val isPub = acc.isPublic || acc.is_public.asBooleanFlag()
                        IgngTile(modifier = Modifier.fillMaxWidth().igngStaggeredReveal(idx)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(acc.resolvedName, style = IgngTheme.type.sectionTitle, color = colors.textPrimary)
                                    Text("绑定于 ${IgngFormatters.formatIsoDateTime(acc.createdAt ?: acc.created_at, short = true)}", style = IgngTheme.type.caption, color = colors.textSecondary)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(if (isPub) "公开" else "私密", style = IgngTheme.type.caption, color = colors.textSecondary)
                                    Switch(
                                        checked = isPub,
                                        onCheckedChange = { next ->
                                            scope.launch {
                                                runCatching { playerRepository.toggleAccountPublic(acc.resolvedName, next) }
                                                    .onSuccess {
                                                        feedback = it.message ?: "已更新公开状态"
                                                        reload()
                                                    }
                                                    .onFailure { feedback = it.message }
                                            }
                                        },
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IgngButton(
                                    text = "登录与处罚",
                                    onClick = {
                                        detailTarget = acc
                                        scope.launch {
                                            detailLoading = true
                                            detailData = runCatching { playerRepository.fetchAccountDetails(acc.resolvedName) }.getOrNull()
                                            detailLoading = false
                                        }
                                    },
                                    style = IgngButtonStyle.Secondary,
                                    compact = true,
                                )
                                IgngButton(
                                    text = "修改游戏密码",
                                    onClick = {
                                        resetTarget = acc
                                        currentCred = ""
                                        nextCred = ""
                                        resetError = null
                                    },
                                    style = IgngButtonStyle.Secondary,
                                    icon = Icons.Rounded.LockReset,
                                    compact = true,
                                )
                                IgngButton(
                                    text = "解绑",
                                    onClick = { unbindTarget = acc },
                                    style = IgngButtonStyle.Danger,
                                    icon = Icons.Rounded.LinkOff,
                                    compact = true,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    detailTarget?.let { acc ->
        IgngDetailSheet(
            onDismiss = { detailTarget = null; detailData = null },
            title = acc.resolvedName,
            subtitle = "登录信息与处罚记录",
        ) {
            if (detailLoading) {
                IgngLoadingBlock(compact = true)
            } else {
                val info = detailData
                Text("最近进服：${IgngFormatters.formatEpochMillis(info?.authme?.lastlogin)}", style = IgngTheme.type.body, color = colors.textPrimary)
                info?.authme?.ip?.let { Text("最近 IP：$it", style = IgngTheme.type.mono, color = colors.textSecondary) }
                Spacer(Modifier.height(10.dp))
                val ban = info?.activePenalty?.ban
                val mute = info?.activePenalty?.mute
                if (ban != null) IgngBadge("生效封禁：${ban.reason ?: "无说明"}", tone = IgngBadgeTone.Danger)
                if (mute != null) IgngBadge("生效禁言：${mute.reason ?: "无说明"}", tone = IgngBadgeTone.Warning)
                if (ban == null && mute == null) IgngBadge("当前无生效处罚", tone = IgngBadgeTone.Success)
                Spacer(Modifier.height(12.dp))
                val history = info?.history.orEmpty()
                if (history.isEmpty()) {
                    Text("没有历史处罚记录。", style = IgngTheme.type.caption, color = colors.textSecondary)
                } else {
                    history.take(10).forEach { p ->
                        IgngTile(soft = true, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), contentPadding = PaddingValues(10.dp)) {
                            Text("${p.type ?: "处罚"} · ${p.reason ?: "无理由"}", style = IgngTheme.type.bodyMedium, color = colors.textPrimary)
                            Text("操作人：${p.banned_by_name ?: "系统"} · ${IgngFormatters.formatEpochMillis(p.time)}", style = IgngTheme.type.caption, color = colors.textSecondary)
                        }
                    }
                }
            }
        }
    }

    resetTarget?.let { acc ->
        IgngDetailSheet(
            onDismiss = { if (!resetting) resetTarget = null },
            title = "修改游戏密码",
            subtitle = "角色 ${acc.resolvedName} · 通过 RSA-OAEP + AES-256-GCM 加密提交",
        ) {
            IgngTextField(
                value = currentCred,
                onValueChange = { currentCred = it },
                label = "当前游戏密码",
                placeholder = "用于二次安全核验",
                isPassword = true,
            )
            Spacer(Modifier.height(10.dp))
            IgngTextField(
                value = nextCred,
                onValueChange = { nextCred = it },
                label = "新游戏密码（6-128 位）",
                placeholder = "输入新游戏密码",
                isPassword = true,
                isError = resetError != null,
                supportingText = resetError,
            )
            Spacer(Modifier.height(14.dp))
            IgngButton(
                text = "确认修改游戏密码",
                loading = resetting,
                enabled = currentCred.isNotEmpty() && nextCred.length >= 6,
                onClick = {
                    scope.launch {
                        resetting = true
                        resetError = null
                        runCatching { playerRepository.resetGamePassword(acc.resolvedName, currentCred, nextCred) }
                            .onSuccess {
                                feedback = it.message ?: "游戏密码已更新"
                                resetTarget = null
                            }
                            .onFailure { resetError = it.message ?: "修改失败" }
                        resetting = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    unbindTarget?.let { acc ->
        IgngConfirmDialog(
            title = "解绑角色 ${acc.resolvedName}",
            message = "解绑后，本 IGNG 账户将不再显示该角色的领地、仓库、统计与完美之人关联权益；之后可以重新验证游戏密码再绑定。",
            confirmLabel = "解绑角色",
            danger = true,
            onDismiss = { unbindTarget = null },
            onConfirm = {
                val target = acc
                unbindTarget = null
                scope.launch {
                    runCatching { playerRepository.unbindAccount(target.resolvedName) }
                        .onSuccess {
                            feedback = it.message ?: "已解绑 ${target.resolvedName}"
                            reload()
                        }
                        .onFailure { feedback = it.message }
                }
            },
        )
    }
}

// ==================== 2. OneWayVault ====================

private enum class VaultTab(val label: String) { ITEMS("物品检索"), HISTORY("存取流水") }

@Composable
fun VaultScreen(
    playerRepository: PlayerRepository,
    onBack: () -> Unit,
) {
    val colors = IgngTheme.colors
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(VaultTab.ITEMS) }
    var overview by remember { mutableStateOf<VaultOverviewResponse?>(null) }
    var selectedVaultId by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var itemsData by remember { mutableStateOf<VaultItemsResponse?>(null) }
    var historyData by remember { mutableStateOf<VaultHistoryResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedItem by remember { mutableStateOf<VaultItemGroupDto?>(null) }
    var traceData by remember { mutableStateOf<VaultTraceResponse?>(null) }
    var traceLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        loading = true
        runCatching { playerRepository.fetchVaultOverview() }
            .onSuccess {
                overview = it
                selectedVaultId = it.vaults.firstOrNull()?.vault_id.orEmpty()
            }
            .onFailure { error = it.message ?: "跨服仓库没有读到" }
        loading = false
    }

    LaunchedEffect(tab, selectedVaultId) {
        if (overview?.available != true) return@LaunchedEffect
        if (tab == VaultTab.ITEMS && selectedVaultId.isNotBlank()) {
            runCatching { playerRepository.fetchVaultItems(selectedVaultId, query) }
                .onSuccess { itemsData = it }
        } else if (tab == VaultTab.HISTORY) {
            runCatching { playerRepository.fetchVaultHistory(selectedVaultId) }
                .onSuccess { historyData = it }
        }
    }

    IgngAmbientCanvas {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            IgngTopBar(title = "跨服物资仓库", subtitle = "OneWayVault 物品检索、附魔与流转追踪", onBack = onBack)
            when {
                loading -> Box(Modifier.padding(16.dp)) { IgngSkeletonRows(4) }
                error != null -> Box(Modifier.padding(16.dp)) { IgngStateBlock(title = "读取失败", description = error, isError = true) }
                overview?.available != true -> Box(Modifier.padding(16.dp)) { IgngStateBlock(title = "尚未开通跨服仓库", description = "当前绑定的角色还没有跨服仓库数据。") }
                else -> {
                    val ov = overview!!
                    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MiniKpi("仓库数", IgngFormatters.formatNumber(ov.totals.vault_count), Modifier.weight(1f))
                            MiniKpi("总物品", IgngFormatters.formatNumber(ov.totals.item_amount), Modifier.weight(1f))
                            MiniKpi("种类", IgngFormatters.formatNumber(ov.totals.material_count), Modifier.weight(1f))
                        }
                        if (ov.vaults.size > 1) {
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                ov.vaults.forEach { bucket ->
                                    val picked = bucket.vault_id == selectedVaultId
                                    IgngBadge(
                                        text = "${bucket.vault_id} (${bucket.item_amount})",
                                        tone = if (picked) IgngBadgeTone.Accent else IgngBadgeTone.Neutral,
                                        modifier = Modifier.clickable { selectedVaultId = bucket.vault_id },
                                    )
                                }
                            }
                        }
                        IgngSegmentedTabs(VaultTab.entries, tab, { it.label }, { tab = it })
                        if (tab == VaultTab.ITEMS) {
                            IgngSearchField(
                                value = query,
                                onValueChange = { query = it },
                                placeholder = "按中文名、材质 ID 或自定义名搜索…",
                                onSubmit = {
                                    scope.launch {
                                        if (selectedVaultId.isNotBlank()) {
                                            runCatching { playerRepository.fetchVaultItems(selectedVaultId, query) }
                                                .onSuccess { itemsData = it }
                                        }
                                    }
                                },
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (tab == VaultTab.ITEMS) {
                            val groups = itemsData?.groups.orEmpty()
                            if (groups.isEmpty()) {
                                item { IgngStateBlock(title = "仓库里还没有匹配的物品") }
                            } else {
                                itemsIndexed(groups, key = { i, g -> "${g.material}:${g.custom_name}:$i" }) { idx, item ->
                                    IgngInteractiveTile(
                                        onClick = { selectedItem = item },
                                        modifier = Modifier.fillMaxWidth().igngStaggeredReveal(idx),
                                    ) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) {
                                                IgngMcFormattedText(item.title, style = IgngTheme.type.cardTitle)
                                                Text(item.material, style = IgngTheme.type.mono, color = colors.textMuted)
                                            }
                                            IgngBadge("×${IgngFormatters.formatNumber(item.total_amount)}", tone = IgngBadgeTone.Accent)
                                        }
                                        if (item.enchantments.isNotEmpty()) {
                                            Spacer(Modifier.height(6.dp))
                                            Text(
                                                item.enchantments.entries.joinToString(" · ") { "${it.key} ${it.value}" },
                                                style = IgngTheme.type.badge,
                                                color = colors.accent,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            val ops = historyData?.records.orEmpty()
                            if (ops.isEmpty()) {
                                item { IgngStateBlock(title = "还没有存取流水记录") }
                            } else {
                                itemsIndexed(ops, key = { i, o -> "${o.operation_id}:$i" }) { idx, op ->
                                    IgngInteractiveTile(
                                        onClick = {
                                            scope.launch {
                                                traceLoading = true
                                                traceData = runCatching { playerRepository.fetchVaultTrace(op.operation_id) }.getOrNull()
                                                traceLoading = false
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().igngStaggeredReveal(idx),
                                    ) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(op.chinese_name ?: op.material ?: op.kind, style = IgngTheme.type.cardTitle, color = colors.textPrimary)
                                            IgngBadge(op.state, tone = if (op.state.equals("COMMITTED", true) || op.state.equals("COMPLETED", true)) IgngBadgeTone.Success else IgngBadgeTone.Neutral)
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text("${op.kind} · ${op.source_server ?: "跨服"} · ${IgngFormatters.formatIsoDateTime(op.created_at, short = true)}", style = IgngTheme.type.caption, color = colors.textSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedItem?.let { item ->
        IgngDetailSheet(
            onDismiss = { selectedItem = null },
            title = item.title,
            subtitle = "${item.material} · 总数量 ${IgngFormatters.formatNumber(item.total_amount)}",
        ) {
            if (item.enchantments.isNotEmpty()) {
                Text("附魔属性", style = IgngTheme.type.caption.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                Spacer(Modifier.height(6.dp))
                item.enchantments.forEach { (k, v) ->
                    Text("· $k Lv.$v", style = IgngTheme.type.body, color = colors.accent)
                }
                Spacer(Modifier.height(10.dp))
            }
            if (item.lore.isNotEmpty()) {
                Text("物品说明（Lore）", style = IgngTheme.type.caption.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                Spacer(Modifier.height(6.dp))
                item.lore.forEach { line -> IgngMcFormattedText(line, style = IgngTheme.type.body) }
                Spacer(Modifier.height(10.dp))
            }
            if (item.entries.isNotEmpty()) {
                Text("来源批次", style = IgngTheme.type.caption.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                Spacer(Modifier.height(6.dp))
                item.entries.take(8).forEach { e ->
                    Text(
                        "×${e.amount} · ${e.source_server ?: "未知服"} ${e.source_season ?: ""} · ${IgngFormatters.formatIsoDateTime(e.created_at, short = true)}",
                        style = IgngTheme.type.caption,
                        color = colors.textSecondary,
                    )
                }
            }
        }
    }

    if (traceLoading || traceData != null) {
        IgngDetailSheet(
            onDismiss = { traceData = null; traceLoading = false },
            title = "流转链路追踪",
            subtitle = traceData?.operation_id,
        ) {
            if (traceLoading) {
                IgngLoadingBlock(compact = true)
            } else {
                traceData?.events.orEmpty().forEach { ev ->
                    IgngTile(soft = true, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), contentPadding = PaddingValues(10.dp)) {
                        Text("${ev.event_type ?: "事件"} · ${ev.state ?: ""}", style = IgngTheme.type.bodyMedium, color = colors.textPrimary)
                        ev.detail?.takeIf { it.isNotBlank() }?.let { detail ->
                            Text(detail, style = IgngTheme.type.caption, color = colors.textSecondary)
                        }
                        Text(IgngFormatters.formatIsoDateTime(ev.created_at), style = IgngTheme.type.badge, color = colors.textMuted)
                    }
                }
            }
        }
    }
}

// ==================== 3. My Lands / Teleports / Fake Players / Logins ====================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyLandsScreen(playerRepository: PlayerRepository, onBack: () -> Unit) {
    val colors = IgngTheme.colors
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var lands by remember { mutableStateOf<List<PlayerLandDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var editingLand by remember { mutableStateOf<PlayerLandDto?>(null) }
    var descDraft by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    suspend fun reload() {
        loading = true
        error = null
        runCatching { playerRepository.fetchLands(keyword = query) }
            .onSuccess { lands = it.records }
            .onFailure { error = it.message ?: "领地列表没有读到" }
        loading = false
    }

    LaunchedEffect(Unit) { reload() }

    IgngAmbientCanvas {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            IgngTopBar(title = "我的领地", subtitle = "查看权限旗标、公开展示与描述编辑", onBack = onBack)
            IgngSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "搜索领地名称或描述，回车查询",
                onSubmit = { scope.launch { reload() } },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(10.dp))
            when {
                loading -> Box(Modifier.padding(16.dp)) { IgngSkeletonRows(4) }
                error != null -> Box(Modifier.padding(16.dp)) { IgngStateBlock(title = "读取失败", description = error, isError = true, actionLabel = "重试", onAction = { scope.launch { reload() } }) }
                lands.isEmpty() -> Box(Modifier.padding(16.dp)) { IgngStateBlock(title = "还没有领地记录") }
                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(lands, key = { _, l -> l.id }) { idx, land ->
                        IgngInteractiveTile(
                            onClick = { editingLand = land; descDraft = land.description },
                            modifier = Modifier.fillMaxWidth().igngStaggeredReveal(idx),
                        ) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(land.land_name ?: "未命名领地", style = IgngTheme.type.cardTitle, color = colors.textPrimary)
                                IgngBadge(if (land.is_public) "大厅公开" else "仅自己可见", tone = if (land.is_public) IgngBadgeTone.Success else IgngBadgeTone.Neutral)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("${land.server_name ?: "服务器 ${land.server_id}"} · ${IgngFormatters.worldLabel(land.world)} (${IgngFormatters.formatCoordinate(land.x, land.y, land.z)})", style = IgngTheme.type.caption, color = colors.textSecondary)
                            if (land.description.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(land.description, style = IgngTheme.type.body, color = colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }

    editingLand?.let { land ->
        IgngDetailSheet(
            onDismiss = { if (!saving) editingLand = null },
            title = land.land_name ?: "领地详情",
            subtitle = "${land.server_name ?: ""} · ${IgngFormatters.worldLabel(land.world)} · 领主 ${land.owner_name ?: "—"}",
        ) {
            val coord = IgngFormatters.formatCoordinate(land.x, land.y, land.z)
            IgngCopyPill(value = coord, label = "中心坐标：$coord", modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            if (land.can_edit) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("在社区大厅公开展示", style = IgngTheme.type.bodyMedium, color = colors.textPrimary)
                    Switch(
                        checked = land.is_public,
                        onCheckedChange = { next ->
                            scope.launch {
                                saving = true
                                runCatching { playerRepository.updateLand(land.id, isPublic = next) }
                                    .onSuccess {
                                        editingLand = land.copy(is_public = next)
                                        reload()
                                    }
                                saving = false
                            }
                        },
                    )
                }
                Spacer(Modifier.height(8.dp))
                IgngTextField(value = descDraft, onValueChange = { descDraft = it }, label = "领地介绍", singleLine = false, minLines = 3)
                Spacer(Modifier.height(10.dp))
                IgngButton(
                    text = "保存领地介绍",
                    loading = saving,
                    icon = Icons.Rounded.Edit,
                    onClick = {
                        scope.launch {
                            saving = true
                            runCatching { playerRepository.updateLand(land.id, description = descDraft.trim()) }
                                .onSuccess {
                                    editingLand = null
                                    reload()
                                }
                            saving = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (land.members.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("领地成员（${land.members.size}）", style = IgngTheme.type.caption.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    land.members.forEach { m -> IgngBadge(m.name ?: m.uuid?.take(8) ?: "成员") }
                }
            }
        }
    }
}

@Composable
fun MyTeleportsScreen(playerRepository: PlayerRepository, onBack: () -> Unit) {
    val colors = IgngTheme.colors
    var list by remember { mutableStateOf<List<PlayerTeleportDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        runCatching { playerRepository.fetchTeleports() }
            .onSuccess { list = it }
            .onFailure { error = it.message ?: "传送点列表没有读到" }
        loading = false
    }

    IgngAmbientCanvas {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            IgngTopBar(title = "我的传送点", subtitle = "点击坐标即可复制", onBack = onBack)
            when {
                loading -> Box(Modifier.padding(16.dp)) { IgngSkeletonRows(4) }
                error != null -> Box(Modifier.padding(16.dp)) { IgngStateBlock(title = "读取失败", description = error, isError = true) }
                list.isEmpty() -> Box(Modifier.padding(16.dp)) { IgngStateBlock(title = "还没有传送点") }
                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(list, key = { _, tp -> tp.id }) { idx, tp ->
                        IgngTile(modifier = Modifier.fillMaxWidth().igngStaggeredReveal(idx)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(tp.point_name ?: "未命名传送点", style = IgngTheme.type.cardTitle, color = colors.textPrimary)
                                IgngBadge(tp.server_name ?: "服务器 ${tp.server_id}")
                            }
                            Spacer(Modifier.height(6.dp))
                            Text("${tp.owner_name ?: "—"} · ${IgngFormatters.worldLabel(tp.world)}", style = IgngTheme.type.caption, color = colors.textSecondary)
                            Spacer(Modifier.height(8.dp))
                            val coord = IgngFormatters.formatCoordinate(tp.x, tp.y, tp.z)
                            IgngCopyPill(value = coord, label = "坐标：$coord", modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MyFakePlayersScreen(playerRepository: PlayerRepository, onBack: () -> Unit) {
    val colors = IgngTheme.colors
    var list by remember { mutableStateOf<List<PlayerFakePlayerDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        runCatching { playerRepository.fetchFakePlayers() }
            .onSuccess { list = it }
            .onFailure { error = it.message ?: "假人列表没有读到" }
        loading = false
    }

    IgngAmbientCanvas {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            IgngTopBar(title = "我的假人", subtitle = "查看在线假人状态与位置", onBack = onBack)
            when {
                loading -> Box(Modifier.padding(16.dp)) { IgngSkeletonRows(3) }
                error != null -> Box(Modifier.padding(16.dp)) { IgngStateBlock(title = "读取失败", description = error, isError = true) }
                list.isEmpty() -> Box(Modifier.padding(16.dp)) { IgngStateBlock(title = "当前没有在线假人") }
                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(list, key = { _, fp -> fp.id }) { idx, fp ->
                        IgngTile(modifier = Modifier.fillMaxWidth().igngStaggeredReveal(idx)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(fp.fake_name ?: "假人", style = IgngTheme.type.cardTitle, color = colors.textPrimary)
                                IgngBadge(fp.server_name ?: "服务器 ${fp.server_id}", tone = IgngBadgeTone.Success, leadingDot = true, pulse = true)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "血量 ${IgngFormatters.formatDecimal(fp.health, 1)} · 饥饿 ${IgngFormatters.formatDecimal(fp.hunger, 1)} · ${IgngFormatters.worldLabel(fp.world)} (${IgngFormatters.formatCoordinate(fp.x, fp.y, fp.z)})",
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

@Composable
fun LoginActivityScreen(playerRepository: PlayerRepository, onBack: () -> Unit) {
    val colors = IgngTheme.colors
    var list by remember { mutableStateOf<List<PlayerLoginRecordDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        runCatching { playerRepository.fetchLogins() }
            .onSuccess { list = it }
            .onFailure { error = it.message ?: "登录记录没有读到" }
        loading = false
    }

    IgngAmbientCanvas {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            IgngTopBar(title = "登录与活动", subtitle = "Minecraft 游戏内登录安全回溯", onBack = onBack)
            when {
                loading -> Box(Modifier.padding(16.dp)) { IgngSkeletonRows(4) }
                error != null -> Box(Modifier.padding(16.dp)) { IgngStateBlock(title = "读取失败", description = error, isError = true) }
                list.isEmpty() -> Box(Modifier.padding(16.dp)) { IgngStateBlock(title = "还没有登录记录") }
                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(list, key = { _, r -> r.id }) { idx, rec ->
                        val ok = rec.is_success.asBooleanFlag()
                        IgngTile(modifier = Modifier.fillMaxWidth().igngStaggeredReveal(idx)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${rec.player_name ?: "角色"} · ${rec.server_name ?: "服务器 ${rec.server_id}"}", style = IgngTheme.type.cardTitle, color = colors.textPrimary)
                                IgngBadge(if (ok) "成功" else "失败", tone = if (ok) IgngBadgeTone.Success else IgngBadgeTone.Danger)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${IgngFormatters.formatIsoDateTime(rec.recorded_at)} · IP ${rec.ip_address ?: "—"} · ${IgngFormatters.worldLabel(rec.world)}",
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

// ==================== 4. Vanilla Stats / Medals / Perfect / Permissions ====================

@Composable
fun VanillaStatsScreen(playerRepository: PlayerRepository, onBack: () -> Unit) {
    val colors = IgngTheme.colors
    var stats by remember { mutableStateOf<VanillaStatsResponse?>(null) }
    var selectedServerId by remember { mutableStateOf<Int?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedServerId) {
        loading = true
        error = null
        runCatching { playerRepository.fetchVanillaStats(serverId = selectedServerId) }
            .onSuccess { stats = it }
            .onFailure { error = it.message ?: "统计数据没有读到" }
        loading = false
    }

    IgngAmbientCanvas {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            IgngTopBar(title = "游戏统计", subtitle = "Minecraft 原版统计快照与图表", onBack = onBack)
            when {
                loading && stats == null -> Box(Modifier.padding(16.dp)) { IgngSkeletonRows(4) }
                error != null && stats == null -> Box(Modifier.padding(16.dp)) { IgngStateBlock(title = "读取失败", description = error, isError = true) }
                stats != null -> {
                    val data = stats!!
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        if (data.servers.isNotEmpty()) {
                            item {
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IgngBadge(
                                        text = "全部服务器",
                                        tone = if (selectedServerId == null) IgngBadgeTone.Accent else IgngBadgeTone.Neutral,
                                        modifier = Modifier.clickable { selectedServerId = null },
                                    )
                                    data.servers.forEach { srv ->
                                        IgngBadge(
                                            text = srv.serverName,
                                            tone = if (selectedServerId == srv.serverId) IgngBadgeTone.Accent else IgngBadgeTone.Neutral,
                                            modifier = Modifier.clickable { selectedServerId = srv.serverId },
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            val s = data.summary
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    MiniKpi("总游玩时长", IgngFormatters.formatPlaytimeSeconds(s.playTimeSeconds), Modifier.weight(1f))
                                    MiniKpi("总移动里程", IgngFormatters.formatDistanceCm(s.distanceCm), Modifier.weight(1f))
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    MiniKpi("挖掘方块", IgngFormatters.formatNumber(s.blocksMined), Modifier.weight(1f))
                                    MiniKpi("合成物品", IgngFormatters.formatNumber(s.itemsCrafted), Modifier.weight(1f))
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    MiniKpi("击败生物", IgngFormatters.formatNumber(s.mobKills), Modifier.weight(1f))
                                    MiniKpi("阵亡次数", IgngFormatters.formatNumber(s.deaths), Modifier.weight(1f))
                                }
                            }
                        }

                        fun renderRanking(title: String, entries: List<net.igng.mcstatus.core.data.VanillaStatEntryDto>) {
                            if (entries.isEmpty()) return
                            item {
                                IgngTile(modifier = Modifier.fillMaxWidth()) {
                                    IgngSectionHeader(title = title)
                                    Spacer(Modifier.height(10.dp))
                                    val max = entries.maxOf { it.value }.coerceAtLeast(1L).toFloat()
                                    IgngHorizontalBarList(
                                        items = entries.take(8).map {
                                            IgngBarMetric(
                                                label = it.label.ifBlank { it.key },
                                                formattedValue = it.formatted ?: IgngFormatters.formatNumber(it.value),
                                                ratio = it.value / max,
                                            )
                                        },
                                    )
                                }
                            }
                        }

                        renderRanking("移动方式分布", data.movement)
                        renderRanking("挖掘最多方块", data.topMined)
                        renderRanking("合成最多物品", data.topCrafted)
                        renderRanking("击败最多生物", data.topKilled)
                    }
                }
            }
        }
    }
}

@Composable
fun MyMedalsScreen(playerRepository: PlayerRepository, onBack: () -> Unit) {
    val colors = IgngTheme.colors
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<MedalProfileDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var editingSlot by remember { mutableStateOf<CustomMedalSlotDto?>(null) }
    var customInput by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    suspend fun reload() {
        loading = true
        error = null
        runCatching { playerRepository.fetchMedals() }
            .onSuccess { profile = it }
            .onFailure { error = it.message ?: "勋章数据没有读到" }
        loading = false
    }

    LaunchedEffect(Unit) { reload() }

    IgngAmbientCanvas {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            IgngTopBar(title = "我的勋章", subtitle = "荣誉陈列与自定义称号名称", onBack = onBack)
            when {
                loading -> Box(Modifier.padding(16.dp)) { IgngSkeletonRows(4) }
                error != null -> Box(Modifier.padding(16.dp)) { IgngStateBlock(title = "读取失败", description = error, isError = true, actionLabel = "重试", onAction = { scope.launch { reload() } }) }
                profile != null -> {
                    val data = profile!!
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (data.customMedals.isNotEmpty()) {
                            item { IgngSectionHeader(title = "自定义勋章称号", subtitle = "修改后将与游戏内同步") }
                            items(data.customMedals, key = { it.ownerName }) { slot ->
                                IgngTile(modifier = Modifier.fillMaxWidth()) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(slot.customName?.ifBlank { "未设置自定义称号" } ?: "未设置自定义称号", style = IgngTheme.type.cardTitle, color = colors.textPrimary)
                                            Text("角色：${slot.ownerName}", style = IgngTheme.type.caption, color = colors.textSecondary)
                                        }
                                        if (slot.canEdit) {
                                            IgngButton(
                                                text = "修改名称",
                                                onClick = {
                                                    editingSlot = slot
                                                    customInput = slot.customName.orEmpty()
                                                    validationError = null
                                                },
                                                style = IgngButtonStyle.Secondary,
                                                icon = Icons.Rounded.Edit,
                                                compact = true,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item { IgngSectionHeader(title = "已获得勋章", subtitle = "共 ${data.allActiveMedals.size} 枚") }
                        if (data.allActiveMedals.isEmpty()) {
                            item { IgngStateBlock(title = "还没有获得勋章", description = "参与服务器活动与周目建设可解锁专属勋章。") }
                        } else {
                            itemsIndexed(data.allActiveMedals, key = { i, m -> "${m.id}:${m.medalId}:$i" }) { idx, medal ->
                                IgngTile(modifier = Modifier.fillMaxWidth().igngStaggeredReveal(idx)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(medal.displayName ?: medal.name, style = IgngTheme.type.cardTitle, color = colors.textPrimary)
                                        medal.account?.let { IgngBadge(it, tone = IgngBadgeTone.Accent) }
                                    }
                                    medal.description?.takeIf { it.isNotBlank() }?.let {
                                        Spacer(Modifier.height(4.dp))
                                        Text(it, style = IgngTheme.type.body, color = colors.textSecondary)
                                    }
                                    medal.publicReason?.takeIf { it.isNotBlank() }?.let {
                                        Spacer(Modifier.height(4.dp))
                                        Text("颁发原因：$it", style = IgngTheme.type.caption, color = colors.textMuted)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingSlot?.let { slot ->
        IgngDetailSheet(
            onDismiss = { if (!saving) editingSlot = null },
            title = "修改自定义勋章名称",
            subtitle = "角色 ${slot.ownerName} · 1-16 个字符，不含颜色代码",
        ) {
            IgngTextField(
                value = customInput,
                onValueChange = {
                    customInput = it
                    validationError = (CustomMedalValidator.validate(it) as? MedalValidationResult.Invalid)?.message
                },
                label = "新自定义名称",
                isError = validationError != null,
                supportingText = validationError,
            )
            Spacer(Modifier.height(14.dp))
            IgngButton(
                text = "保存名称",
                loading = saving,
                enabled = CustomMedalValidator.validate(customInput) is MedalValidationResult.Valid,
                onClick = {
                    val valid = CustomMedalValidator.validate(customInput) as? MedalValidationResult.Valid ?: return@IgngButton
                    scope.launch {
                        saving = true
                        runCatching { playerRepository.renameCustomMedal(slot.ownerName, valid.normalized) }
                            .onSuccess {
                                editingSlot = null
                                reload()
                            }
                            .onFailure { validationError = it.message ?: "保存失败" }
                        saving = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun PersonalPerfectScreen(playerRepository: PlayerRepository, onBack: () -> Unit) {
    val colors = IgngTheme.colors
    var data by remember { mutableStateOf<PersonalPerfectResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        runCatching { playerRepository.fetchPersonalPerfect() }
            .onSuccess { data = it }
            .onFailure { error = it.message ?: "完美之人信息暂时不可用" }
        loading = false
    }

    IgngAmbientCanvas {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            IgngTopBar(title = "完美之人", subtitle = "个人资格状态与权益额度", onBack = onBack)
            when {
                loading -> Box(Modifier.padding(16.dp)) { IgngSkeletonRows(3) }
                error != null -> Box(Modifier.padding(16.dp)) { IgngStateBlock(title = "读取失败", description = error, isError = true) }
                data != null -> {
                    val info = data!!
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        itemsIndexed(info.accounts, key = { _, a -> a.username }) { idx, acc ->
                            IgngTile(modifier = Modifier.fillMaxWidth().igngStaggeredReveal(idx)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(acc.username, style = IgngTheme.type.sectionTitle, color = colors.textPrimary)
                                    IgngBadge(
                                        text = if (acc.isPerfect) (acc.memberNoLabel ?: "完美之人") else "未认证",
                                        tone = if (acc.isPerfect) IgngBadgeTone.Warning else IgngBadgeTone.Neutral,
                                    )
                                }
                                acc.qualificationLabel?.let {
                                    Spacer(Modifier.height(4.dp))
                                    Text(it, style = IgngTheme.type.caption, color = colors.textSecondary)
                                }
                                if (acc.landUsage.isNotEmpty()) {
                                    Spacer(Modifier.height(10.dp))
                                    acc.landUsage.forEach { usage ->
                                        val limit = usage.limit ?: 0
                                        val ratio = if (limit > 0) (usage.count.toFloat() / limit).coerceIn(0f, 1f) else 0f
                                        Column(Modifier.padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("${usage.serverName} 领地额度", style = IgngTheme.type.caption, color = colors.textSecondary)
                                                Text(if (limit > 0) "${usage.count} / $limit" else "${usage.count}", style = IgngTheme.type.mono, color = colors.textPrimary)
                                            }
                                            if (limit > 0) {
                                                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)).background(colors.tileSoft)) {
                                                    Box(Modifier.fillMaxWidth(ratio).height(6.dp).clip(RoundedCornerShape(999.dp)).background(colors.accent))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class PermTab(val label: String) { GROUPS("权限组与节点"), INSIGHTS("动态与到期") }

@Composable
fun PermissionsScreen(playerRepository: PlayerRepository, onBack: () -> Unit) {
    val colors = IgngTheme.colors
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(PermTab.GROUPS) }
    var perms by remember { mutableStateOf<PlayerPermissionsResponse?>(null) }
    var insights by remember { mutableStateOf<PermissionInsightsResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        val pDef = scope.async { runCatching { playerRepository.fetchPermissions() } }
        val iDef = scope.async { runCatching { playerRepository.fetchPermissionInsights() } }
        pDef.await().onSuccess { perms = it }.onFailure { error = it.message ?: "游戏权限没有读到" }
        iDef.await().onSuccess { insights = it }
        loading = false
    }

    IgngAmbientCanvas {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            IgngTopBar(title = "游戏权限", subtitle = "LuckPerms 跨服权限组与独立节点", onBack = onBack)
            Box(Modifier.padding(horizontal = 16.dp)) {
                IgngSegmentedTabs(PermTab.entries, tab, { it.label }, { tab = it })
            }
            Spacer(Modifier.height(10.dp))
            when {
                loading -> Box(Modifier.padding(16.dp)) { IgngSkeletonRows(4) }
                error != null -> Box(Modifier.padding(16.dp)) { IgngStateBlock(title = "读取失败", description = error, isError = true) }
                tab == PermTab.GROUPS -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val accounts = perms?.accounts.orEmpty()
                    if (accounts.isEmpty()) {
                        item { IgngStateBlock(title = "还没有权限记录") }
                    } else {
                        items(accounts, key = { it.id }) { acc ->
                            IgngTile(modifier = Modifier.fillMaxWidth()) {
                                Text(acc.username, style = IgngTheme.type.sectionTitle, color = colors.textPrimary)
                                Spacer(Modifier.height(8.dp))
                                acc.identities.forEach { id ->
                                    id.groups.forEach { grp ->
                                        var expanded by remember { mutableStateOf(false) }
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(IgngTheme.radii.sm)
                                                .background(colors.tileSoft)
                                                .clickable { expanded = !expanded }
                                                .padding(10.dp),
                                        ) {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(grp.displayName ?: grp.name, style = IgngTheme.type.bodyMedium.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                                                IgngBadge("${grp.permissions.size} 节点")
                                            }
                                            AnimatedVisibility(visible = expanded) {
                                                Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    grp.permissions.take(30).forEach { node ->
                                                        Text(
                                                            "${if (node.value) "+" else "-"} ${node.nodeKey}",
                                                            style = IgngTheme.type.mono,
                                                            color = if (node.value) colors.success else colors.danger,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(Modifier.height(6.dp))
                                    }
                                    if (id.independentPermissions.isNotEmpty()) {
                                        Spacer(Modifier.height(4.dp))
                                        Text("独立节点", style = IgngTheme.type.caption.copy(fontWeight = FontWeight.Bold), color = colors.textSecondary)
                                        id.independentPermissions.forEach { node ->
                                            Text("${if (node.value) "+" else "-"} ${node.nodeKey}", style = IgngTheme.type.mono, color = colors.textPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val expList = insights?.expiring.orEmpty()
                    val changes = insights?.recentChanges.orEmpty()
                    if (expList.isEmpty() && changes.isEmpty()) {
                        item { IgngStateBlock(title = "当前没有即将到期的临时权限或变更动态") }
                    } else {
                        if (expList.isNotEmpty()) {
                            item { IgngSectionHeader(title = "即将到期节点") }
                            items(expList, key = { "${it.id}:${it.nodeKey}" }) { node ->
                                IgngTile(modifier = Modifier.fillMaxWidth()) {
                                    Text(node.nodeKey, style = IgngTheme.type.mono, color = colors.textPrimary)
                                    Text("到期：${IgngFormatters.formatIsoDateTime(node.expiresAt)}", style = IgngTheme.type.caption, color = colors.warning)
                                }
                            }
                        }
                        if (changes.isNotEmpty()) {
                            item { IgngSectionHeader(title = "近期权限动态") }
                            itemsIndexed(changes) { _, ch ->
                                IgngTile(modifier = Modifier.fillMaxWidth()) {
                                    Text("${ch.action} · ${ch.permission}", style = IgngTheme.type.mono, color = colors.textPrimary)
                                    Text("${ch.target} · ${IgngFormatters.formatIsoDateTime(ch.timestamp, short = true)}", style = IgngTheme.type.caption, color = colors.textSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniKpi(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = IgngTheme.colors
    IgngTile(modifier = modifier, contentPadding = PaddingValues(12.dp)) {
        Text(label, style = IgngTheme.type.badge, color = colors.textSecondary)
        Spacer(Modifier.height(3.dp))
        Text(value, style = IgngTheme.type.metricMedium, color = colors.textPrimary)
    }
}
