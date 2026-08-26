@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.igng.mcstatus.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.LockReset
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.serialization.json.JsonElement
import net.igng.mcstatus.data.AppSettings
import net.igng.mcstatus.data.McAccount
import net.igng.mcstatus.data.McAccountDetailsResponse
import net.igng.mcstatus.data.McDataKind
import net.igng.mcstatus.data.McFakePlayerRecord
import net.igng.mcstatus.data.McLandRecord
import net.igng.mcstatus.data.McLoginRecord
import net.igng.mcstatus.data.McPermissionAccount
import net.igng.mcstatus.data.McPermissionGroup
import net.igng.mcstatus.data.McPermissionIdentity
import net.igng.mcstatus.data.McPermissionNode
import net.igng.mcstatus.data.McPermissionsResponse
import net.igng.mcstatus.data.McServer
import net.igng.mcstatus.data.McTeleportRecord
import net.igng.mcstatus.data.asBoolean
import net.igng.mcstatus.data.toDisplayTime

enum class McManagementSection(val label: String) {
    ACCOUNTS("游戏账号"),
    DATA("我的数据"),
    PERMISSIONS("权限"),
}

@Composable
fun McManagementSettingsEntry(
    settings: AppSettings,
    onClick: () -> Unit,
) {
    val enabled = !settings.sessionToken.isNullOrBlank()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        ListItem(
            modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
            leadingContent = {
                Icon(
                    imageVector = Icons.Rounded.ManageAccounts,
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            headlineContent = { Text("个人管理", fontWeight = FontWeight.Bold) },
            supportingContent = {
                Text(if (enabled) "管理 Minecraft 账号、领地和游戏数据" else "登录 IGNG 账号后可用")
            },
            trailingContent = {
                Icon(Icons.Rounded.ChevronRight, contentDescription = if (enabled) "打开个人管理" else null)
            },
        )
    }
}

@Composable
fun McManagementSignedOutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("个人管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Rounded.ManageAccounts, contentDescription = null, modifier = Modifier.size(42.dp))
                    Text("请先登录 IGNG 账号", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("登录后才能查看与你绑定的 Minecraft 数据。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun McManagementScreen(
    settings: AppSettings,
    adminRole: String?,
    viewModel: McManagementViewModel,
    onBack: () -> Unit,
    onOpenBindSite: () -> Unit,
    onOpenAdminSite: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var section by rememberSaveable { mutableStateOf(McManagementSection.ACCOUNTS) }
    var resetAccount by rememberSaveable { mutableStateOf<String?>(null) }
    var unbindAccount by rememberSaveable { mutableStateOf<String?>(null) }
    var descriptionLand by remember { mutableStateOf<McLandRecord?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("个人管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshCurrent(section) }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "刷新")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            McManagementSummary(
                settings = settings,
                state = state,
                adminRole = adminRole,
            )
            PrimaryScrollableTabRow(
                selectedTabIndex = section.ordinal,
                edgePadding = 12.dp,
                divider = {},
            ) {
                McManagementSection.entries.forEach { item ->
                    Tab(
                        selected = section == item,
                        onClick = {
                            section = item
                            when (item) {
                                McManagementSection.ACCOUNTS -> Unit
                                McManagementSection.DATA -> viewModel.refreshData()
                                McManagementSection.PERMISSIONS -> viewModel.refreshPermissions()
                            }
                        },
                        text = { Text(item.label) },
                    )
                }
            }

            state.message?.let { message ->
                McMessageBanner(message = message.text, success = message.success, onDismiss = viewModel::clearMessage)
            }

            if (state.sessionExpired) {
                SessionExpiredBanner(onBack = onBack)
            }

            when (section) {
                McManagementSection.ACCOUNTS -> McAccountsContent(
                    state = state,
                    adminRole = adminRole,
                    onBind = onOpenBindSite,
                    onOpenAdmin = onOpenAdminSite,
                    onDetails = viewModel::openAccountDetails,
                    onTogglePublic = viewModel::toggleAccountPublic,
                    onReset = { resetAccount = it },
                    onUnbind = { unbindAccount = it },
                    modifier = Modifier.weight(1f),
                )

                McManagementSection.DATA -> McDataContent(
                    state = state,
                    onKind = viewModel::selectDataKind,
                    onServerFilter = viewModel::setServerFilter,
                    onAccountFilter = viewModel::setAccountFilter,
                    onRefresh = { viewModel.refreshData() },
                    onToggleLandPublic = viewModel::toggleLandPublic,
                    onEditDescription = { descriptionLand = it },
                    modifier = Modifier.weight(1f),
                )

                McManagementSection.PERMISSIONS -> McPermissionsContent(
                    state = state,
                    onRefresh = viewModel::refreshPermissions,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    state.selectedAccount?.let { username ->
        McAccountDetailsDialog(
            username = username,
            details = state.accountDetails,
            loading = state.detailsLoading,
            onClose = viewModel::closeAccountDetails,
            onPageChange = { viewModel.loadAccountDetails(username, it) },
        )
    }

    resetAccount?.let { username ->
        McResetPasswordDialog(
            username = username,
            onDismiss = { resetAccount = null },
            onConfirm = { password ->
                resetAccount = null
                viewModel.resetAccountPassword(username, password)
            },
        )
    }

    unbindAccount?.let { username ->
        AlertDialog(
            onDismissRequest = { unbindAccount = null },
            icon = { Icon(Icons.Rounded.LinkOff, contentDescription = null) },
            title = { Text("确认解绑？") },
            text = { Text("解绑 $username 后，个人管理将不再显示这个账号产生的数据。游戏账号本身不会被删除。") },
            confirmButton = {
                Button(onClick = {
                    unbindAccount = null
                    viewModel.unbindAccount(username)
                }) { Text("确认解绑") }
            },
            dismissButton = { TextButton(onClick = { unbindAccount = null }) { Text("取消") } },
        )
    }

    descriptionLand?.let { land ->
        McLandDescriptionDialog(
            land = land,
            onDismiss = { descriptionLand = null },
            onConfirm = { description ->
                descriptionLand = null
                viewModel.updateLandDescription(land, description)
            },
        )
    }
}

@Composable
private fun McManagementSummary(
    settings: AppSettings,
    state: McManagementUiState,
    adminRole: String?,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.ManageAccounts, contentDescription = null, modifier = Modifier.size(24.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(settings.accountName ?: "IGNG 账户", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text("Minecraft 个人空间", style = MaterialTheme.typography.bodySmall)
                }
                adminRole?.let { McBadge(adminRoleLabel(it), success = true) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                McSummaryMetric("绑定账号", state.accounts.size.toString(), Modifier.weight(1f))
                McSummaryMetric("当前数据", currentDataCount(state).toString(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun McSummaryMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun currentDataCount(state: McManagementUiState): Int = when (state.dataKind) {
    McDataKind.LANDS -> state.lands.size
    McDataKind.TELEPORTS -> state.teleports.size
    McDataKind.LOGINS -> state.logins.size
    McDataKind.FAKE_PLAYERS -> state.fakePlayers.size
}

@Composable
private fun McAccountsContent(
    state: McManagementUiState,
    adminRole: String?,
    onBind: () -> Unit,
    onOpenAdmin: () -> Unit,
    onDetails: (String) -> Unit,
    onTogglePublic: (McAccount, Boolean) -> Unit,
    onReset: (String) -> Unit,
    onUnbind: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onBind, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Shield, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("在站点绑定账号")
                }
                if (adminRole != null) {
                    OutlinedButton(onClick = onOpenAdmin) {
                        Icon(Icons.Rounded.Security, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("管理中心")
                    }
                }
            }
        }
        item {
            Text("游戏账号", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text("公开开关只影响站点公开资料；账号详情和数据仍仅对当前账户可见。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (state.accountsLoading && state.accounts.isEmpty()) {
            item { McLoadingCard("正在读取绑定账号…") }
        } else if (state.accounts.isEmpty()) {
            item { McEmptyCard("还没有绑定游戏账号", "完成绑定后，这里会显示账号状态和可用操作。") }
        } else {
            items(state.accounts, key = { it.id.takeIf { id -> id != 0 } ?: it.mc_username }) { account ->
                McAccountCard(
                    account = account,
                    onDetails = { onDetails(account.mc_username) },
                    onTogglePublic = { onTogglePublic(account, it) },
                    onReset = { onReset(account.mc_username) },
                    onUnbind = { onUnbind(account.mc_username) },
                )
            }
        }
    }
}

@Composable
private fun McAccountCard(
    account: McAccount,
    onDetails: () -> Unit,
    onTogglePublic: (Boolean) -> Unit,
    onReset: () -> Unit,
    onUnbind: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(Icons.Rounded.Shield, contentDescription = null, modifier = Modifier.padding(10.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(account.mc_username, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        if (account.is_banned.asBoolean()) McBadge("封禁", success = false)
                        if (account.is_muted.asBoolean()) McBadge("禁言", success = false)
                    }
                    Text("绑定于 ${displayMcDate(account.created_at)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (account.is_public.asBoolean()) Icons.Rounded.Public else Icons.Rounded.VisibilityOff, contentDescription = null, modifier = Modifier.size(18.dp))
                Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                    Text("公开展示", fontWeight = FontWeight.SemiBold)
                    Text(if (account.is_public.asBoolean()) "其他人可以在站点公开资料中看到" else "仅自己可见", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = account.is_public.asBoolean(), onCheckedChange = onTogglePublic)
            }
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onDetails) {
                    Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("详情")
                }
                OutlinedButton(onClick = onReset) {
                    Icon(Icons.Rounded.LockReset, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("重置密码")
                }
                TextButton(onClick = onUnbind) {
                    Icon(Icons.Rounded.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("解绑")
                }
            }
        }
    }
}

@Composable
private fun McDataContent(
    state: McManagementUiState,
    onKind: (McDataKind) -> Unit,
    onServerFilter: (String) -> Unit,
    onAccountFilter: (String) -> Unit,
    onRefresh: () -> Unit,
    onToggleLandPublic: (McLandRecord, Boolean) -> Unit,
    onEditDescription: (McLandRecord) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            PrimaryScrollableTabRow(selectedTabIndex = state.dataKind.ordinal, edgePadding = 0.dp, divider = {}) {
                McDataKind.entries.forEach { kind ->
                    Tab(selected = state.dataKind == kind, onClick = { onKind(kind) }, text = { Text(kind.label) })
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                McFilterMenu(
                    label = state.servers.firstOrNull { it.server_id.toString() == state.serverFilter }?.server_name ?: "所有服务器",
                    options = listOf("all" to "所有服务器") + state.servers.map { it.server_id.toString() to it.server_name },
                    onSelect = onServerFilter,
                    modifier = Modifier.weight(1f),
                )
                if (state.dataKind != McDataKind.LANDS) {
                    McFilterMenu(
                        label = state.accounts.firstOrNull { it.mc_username == state.accountFilter }?.mc_username ?: "所有绑定账号",
                        options = listOf("all" to "所有绑定账号") + state.accounts.map { it.mc_username to it.mc_username },
                        onSelect = onAccountFilter,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(state.dataKind.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(dataDescription(state.dataKind), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onRefresh) { Icon(Icons.Rounded.Refresh, contentDescription = "刷新数据") }
            }
        }
        if (state.dataLoading) {
            item { McLoadingCard("正在读取${state.dataKind.label}…") }
        } else {
            when (state.dataKind) {
                McDataKind.LANDS -> if (state.lands.isEmpty()) {
                    item { McEmptyCard("暂无领地", "你作为主人或成员参与的领地会显示在这里。") }
                } else {
                    items(state.lands, key = { it.id }) { land ->
                        McLandCard(land, onTogglePublic = { onToggleLandPublic(land, it) }, onEditDescription = { onEditDescription(land) })
                    }
                }

                McDataKind.TELEPORTS -> if (state.teleports.isEmpty()) {
                    item { McEmptyCard("暂无传送点", "当前绑定账号没有可显示的传送点。") }
                } else {
                    items(state.teleports, key = { it.id }) { McTeleportCard(it) }
                }

                McDataKind.LOGINS -> if (state.logins.isEmpty()) {
                    item { McEmptyCard("暂无登录记录", "当前绑定账号没有可显示的登录记录。") }
                } else {
                    items(state.logins, key = { if (it.id != 0) it.id else "${it.player_name}-${it.recorded_at}" }) { McLoginCard(it) }
                }

                McDataKind.FAKE_PLAYERS -> if (state.fakePlayers.isEmpty()) {
                    item { McEmptyCard("暂无假人", "当前绑定账号创建的假人会显示在这里。") }
                } else {
                    items(state.fakePlayers, key = { if (it.id != 0) it.id else "${it.fake_name}-${it.last_update}" }) { McFakePlayerCard(it) }
                }
            }
        }
    }
}

@Composable
private fun McFilterMenu(
    label: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, optionLabel) ->
                DropdownMenuItem(
                    text = { Text(optionLabel) },
                    onClick = {
                        expanded = false
                        onSelect(value)
                    },
                )
            }
        }
    }
}

@Composable
private fun McLandCard(
    land: McLandRecord,
    onTogglePublic: (Boolean) -> Unit,
    onEditDescription: () -> Unit,
) {
    val owner = land.viewer_role == "owner"
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.Map, contentDescription = null, modifier = Modifier.size(22.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("${land.server_name ?: "未命名服务器"} · ${land.world ?: "未知世界"}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(land.land_name ?: "未命名领地", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                }
                McBadge(landRoleLabel(land.viewer_role), success = owner)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (land.is_public) Icons.Rounded.Public else Icons.Rounded.VisibilityOff, contentDescription = null, modifier = Modifier.size(18.dp))
                Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                    Text("公开领地", fontWeight = FontWeight.SemiBold)
                    Text(if (land.can_edit) "其他人可以在信息大厅看到" else "只有 UUID 精确匹配的领地主人可以修改", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = land.is_public, onCheckedChange = onTogglePublic, enabled = land.can_edit)
            }
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (land.description.isBlank()) "暂无领地简介" else land.description, modifier = Modifier.weight(1f), color = if (land.description.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                if (land.can_edit) IconButton(onClick = onEditDescription) { Icon(Icons.Rounded.Edit, contentDescription = "编辑简介") }
            }
            HorizontalDivider()
            McLandFacts(land)
            McLandMembers(land)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                McPermissionSummaryBlock("所有玩家权限", land.guest_flags, Modifier.weight(1f))
                McPermissionSummaryBlock("环境权限", land.environment_flags, Modifier.weight(1f))
            }
            Text("更新于 ${displayMcDate(land.last_updated)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun McLandFacts(land: McLandRecord) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("领地资料", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        McFactRow("中心位置", "${formatMcNumber(land.x)}, ${formatMcNumber(land.y)}, ${formatMcNumber(land.z)}")
        land.size?.let { McFactRow("尺寸", "${formatMcNumber(it.width)} × ${formatMcNumber(it.height)} × ${formatMcNumber(it.length)}") }
        if (land.corners.isNotEmpty()) {
            McFactRow("四个角点", land.corners.mapIndexed { index, corner -> "0${index + 1}: ${formatMcNumber(corner.x)}, ${formatMcNumber(corner.y)}, ${formatMcNumber(corner.z)}" }.joinToString("\n"))
        }
    }
}

@Composable
private fun McLandMembers(land: McLandRecord) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("成员（${land.members.size}）", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        if (land.members.isEmpty()) {
            Text("暂无成员数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            land.members.forEach { member ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(member.name ?: "未知玩家", fontWeight = FontWeight.SemiBold)
                    if (land.viewer_role == "owner") {
                        Text(permissionMapSummary(member.permissions), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        val isSelf = land.viewer_member?.uuid == member.uuid || land.viewer_member?.name == member.name
                        Text(if (isSelf) "我的成员权限：${permissionMapSummary(land.viewer_member?.permissions.orEmpty())}" else "成员权限不可见", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun McPermissionSummaryBlock(title: String, permissions: Map<String, JsonElement>, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(permissionMapSummary(permissions), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun McTeleportCard(record: McTeleportRecord) {
    McRecordCard(icon = Icons.Rounded.Place, title = record.point_name ?: "未命名传送点", subtitle = record.server_name ?: "未命名服务器") {
        McFactRow("所有者", record.owner_name ?: "未知")
        McFactRow("位置", "${record.world ?: "未知世界"} · ${formatMcNumber(record.x)}, ${formatMcNumber(record.y)}, ${formatMcNumber(record.z)}")
        McFactRow("更新时间", displayMcDate(record.last_updated))
    }
}

@Composable
private fun McLoginCard(record: McLoginRecord) {
    val success = record.is_success.asBoolean()
    McRecordCard(icon = Icons.Rounded.History, title = record.player_name ?: "未知玩家", subtitle = record.server_name ?: "服务器 ${record.server_id}") {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            McBadge(if (success) "登录成功" else "登录失败", success = success)
            Text(displayMcDate(record.recorded_at), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        McFactRow("位置", "${record.world ?: "未知世界"} · ${formatMcNumber(record.x)}, ${formatMcNumber(record.y)}, ${formatMcNumber(record.z)}")
        McFactRow("IP 地址", record.ip_address ?: "未记录")
    }
}

@Composable
private fun McFakePlayerCard(record: McFakePlayerRecord) {
    McRecordCard(icon = Icons.Rounded.People, title = record.fake_name ?: "未知假人", subtitle = record.server_name ?: "服务器 ${record.server_id}") {
        McBadge("在线数据", success = true)
        McFactRow("创建者", record.creator_name ?: "未知")
        McFactRow("状态", "❤ ${formatMcNumber(record.health)} · 🍖 ${formatMcNumber(record.hunger)}")
        McFactRow("位置", "${record.world ?: "未知世界"} · ${formatMcNumber(record.x)}, ${formatMcNumber(record.y)}, ${formatMcNumber(record.z)}")
        McFactRow("更新时间", displayMcDate(record.last_update))
    }
}

@Composable
private fun McRecordCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(21.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                }
            }
            content()
        }
    }
}

@Composable
private fun McFactRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Text(label, modifier = Modifier.width(72.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun McPermissionsContent(
    state: McManagementUiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("LuckPerms 权限", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("只展示绑定 Minecraft 账号的实际权限，不包含 IGNG 平台权限。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onRefresh) { Icon(Icons.Rounded.Refresh, contentDescription = "刷新权限") }
            }
        }
        if (state.permissionsLoading && state.permissions == null) {
            item { McLoadingCard("正在读取权限…") }
        } else if (state.permissions == null) {
            item { McEmptyCard("权限信息暂时不可用", "请稍后刷新重试。") }
        } else {
            val permissions = state.permissions
            item { McPermissionTotals(permissions) }
            if (permissions.accounts.isEmpty()) {
                item { McEmptyCard("当前没有绑定 Minecraft 账号", "绑定账号后才能读取 LuckPerms 身份。") }
            } else {
                items(permissions.accounts, key = { it.id }) { account -> McPermissionAccountCard(account, state.servers) }
            }
        }
    }
}

@Composable
private fun McPermissionTotals(data: McPermissionsResponse) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        McSummaryMetric("账号", data.summary.accountCount.toString(), Modifier.weight(1f))
        McSummaryMetric("权限组", data.summary.groupCount.toString(), Modifier.weight(1f))
        McSummaryMetric("权限节点", data.summary.permissionNodeCount.toString(), Modifier.weight(1f))
    }
}

@Composable
private fun McPermissionAccountCard(account: McPermissionAccount, servers: List<McServer>) {
    val serverNames = remember(servers) { servers.associate { it.server_id to it.server_name } }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Icon(Icons.Rounded.Security, contentDescription = null, modifier = Modifier.size(21.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(account.username, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text("${account.identities.size} 个服务器身份 · ${account.uuid ?: "UUID 未记录"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            account.identities.forEach { identity ->
                McPermissionIdentityCard(identity, serverNames)
            }
        }
    }
}

@Composable
private fun McPermissionIdentityCard(identity: McPermissionIdentity, serverNames: Map<Int, String>) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val serverLabel = identity.serverId?.let { serverNames[it] ?: "服务器 $it" } ?: "未识别服务器"
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(identity.username ?: "未知玩家", fontWeight = FontWeight.Bold)
                    Text("$serverLabel · ${identity.uuid ?: "UUID 未记录"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(if (expanded) Icons.Rounded.Close else Icons.Rounded.ChevronRight, contentDescription = if (expanded) "收起" else "展开", modifier = Modifier.size(18.dp))
            }
            if (!identity.found) {
                Text("未找到 LuckPerms 玩家数据", fontWeight = FontWeight.SemiBold)
                Text("该身份目前没有可展示的权限组或独立权限。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (expanded) {
                Text("权限组：${identity.summary.directGroupCount} 个直接隶属 · ${identity.summary.inheritedGroupCount} 个继承组", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                identity.groups.forEach { group -> McPermissionGroupCard(group, serverLabel) }
                if (identity.independentPermissions.isNotEmpty()) {
                    Text("独立权限", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    identity.independentPermissions.forEach { node -> McPermissionNodeRow(node) }
                } else {
                    Text("没有独立权限节点", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Text("${identity.groups.size} 个权限组 · ${identity.independentPermissions.size} 个独立节点", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun McPermissionGroupCard(group: McPermissionGroup, serverLabel: String) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(group.displayName ?: group.name, fontWeight = FontWeight.Bold)
                        McBadge(if (group.assignment == "direct") "直接隶属" else "继承获得", success = group.assignment == "direct")
                    }
                    Text("${group.name}${group.inheritedFrom?.let { " · 继承自 $it" } ?: ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(if (expanded) "收起" else "${group.permissions.size} 节点", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            if (expanded) {
                Text("${group.weight?.let { "权重 $it" } ?: serverLabel}${group.expiresAt?.let { " · 有效期至 ${displayMcDate(it)}" } ?: ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (group.permissions.isEmpty()) Text("这个权限组暂未配置权限节点", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                group.permissions.forEach { node -> McPermissionNodeRow(node) }
            }
        }
    }
}

@Composable
private fun McPermissionNodeRow(node: McPermissionNode) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Icon(if (node.effect == "allow") Icons.Rounded.CheckCircle else Icons.Rounded.VisibilityOff, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (node.effect == "allow") Color(0xFF16A34A) else MaterialTheme.colorScheme.error)
        Column(modifier = Modifier.weight(1f)) {
            Text(node.nodeKey, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            Text("${if (node.effect == "allow") "允许" else "拒绝"} · ${node.contextLabel ?: "全局"}${node.expiresAt?.let { " · 有效期至 ${displayMcDate(it)}" } ?: ""}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun McAccountDetailsDialog(
    username: String,
    details: McAccountDetailsResponse?,
    loading: Boolean,
    onClose: () -> Unit,
    onPageChange: (Int) -> Unit,
) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("账号详情：$username", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "关闭") } },
                    )
                },
            ) { padding ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (loading && details == null) {
                        item { McLoadingCard("正在读取账号详情…") }
                    } else if (details != null) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text("账号登录信息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    McFactRow("最后登录", displayEpoch(details.authme?.lastlogin))
                                    McFactRow("IP 地址", details.authme?.ip ?: "未记录")
                                }
                            }
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                McPenaltyCard("封禁状态", details.activePenalty?.ban, isBan = true, Modifier.weight(1f))
                                McPenaltyCard("禁言状态", details.activePenalty?.mute, isBan = false, Modifier.weight(1f))
                            }
                        }
                        item { Text("处罚历史（${details.totalHistory} 条）", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                        if (details.history.isEmpty()) {
                            item { McEmptyCard("无历史记录", "该账号没有封禁或禁言历史。") }
                        } else {
                            itemsIndexed(details.history, key = { index, item -> "${item.type}-${item.time}-$index" }) { _, item -> McPunishmentCard(item) }
                        }
                        if (details.totalPages > 1) {
                            item {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("第 ${details.page} / ${details.totalPages} 页", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row {
                                        TextButton(onClick = { onPageChange(details.page - 1) }, enabled = details.page > 1) { Text("上一页") }
                                        TextButton(onClick = { onPageChange(details.page + 1) }, enabled = details.page < details.totalPages) { Text("下一页") }
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

@Composable
private fun McPenaltyCard(
    title: String,
    penalty: net.igng.mcstatus.data.McPunishmentRecord?,
    isBan: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = if (penalty == null) MaterialTheme.colorScheme.surfaceContainer else if (isBan) MaterialTheme.colorScheme.errorContainer else Color(0xFFFFF0C2))) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if (penalty == null) "正常" else if (isBan) "已封禁" else "已禁言", fontWeight = FontWeight.Black)
            penalty?.reason?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis) }
            penalty?.let { Text("开始：${displayEpoch(it.time)}\n结束：${if (it.until == -1L) "永久" else displayEpoch(it.until)}", style = MaterialTheme.typography.labelSmall) }
        }
    }
}

@Composable
private fun McPunishmentCard(item: net.igng.mcstatus.data.McPunishmentRecord) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                McBadge(if (item.type == "BAN") "封禁" else "禁言", success = false)
                Text(if (item.active.asBoolean() && (item.until == -1L || (item.until ?: 0L) > System.currentTimeMillis())) "生效中" else "已失效", color = if (item.active.asBoolean()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            }
            McFactRow("原因", item.reason ?: "未说明")
            McFactRow("执行人", item.banned_by_name ?: "未知")
            McFactRow("开始时间", displayEpoch(item.time))
            McFactRow("结束时间", if (item.until == -1L) "永久" else displayEpoch(item.until))
        }
    }
}

@Composable
private fun McResetPasswordDialog(
    username: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var password by rememberSaveable(username) { mutableStateOf("") }
    var confirmation by rememberSaveable(username) { mutableStateOf("") }
    val valid = password.isNotBlank() && password == confirmation
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.LockReset, contentDescription = null) },
        title = { Text("重置 $username 的密码") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("站点会立即更新游戏账号密码。密码不会保存在 Android 应用中。")
                OutlinedTextField(password, { password = it }, label = { Text("新密码") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true)
                OutlinedTextField(confirmation, { confirmation = it }, label = { Text("再次输入") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true, isError = confirmation.isNotEmpty() && password != confirmation)
                if (confirmation.isNotEmpty() && password != confirmation) Text("两次输入的密码不一致", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { Button(onClick = { onConfirm(password) }, enabled = valid) { Text("确认重置") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun McLandDescriptionDialog(
    land: McLandRecord,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var description by rememberSaveable(land.id) { mutableStateOf(land.description) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
        title = { Text("编辑领地简介") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(land.land_name ?: "未命名领地")
                OutlinedTextField(description, { if (it.length <= 200) description = it }, label = { Text("简介") }, minLines = 4, supportingText = { Text("${description.length}/200") })
            }
        },
        confirmButton = { Button(onClick = { onConfirm(description) }) { Text("保存简介") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun McMessageBanner(message: String, success: Boolean, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (success) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, modifier = Modifier.weight(1f), color = if (success) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer)
            IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = "关闭提示") }
        }
    }
}

@Composable
private fun SessionExpiredBanner(onBack: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp), color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("请返回设置重新登录", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
            TextButton(onClick = onBack) { Text("返回设置") }
        }
    }
}

@Composable
private fun McBadge(text: String, success: Boolean) {
    Surface(shape = RoundedCornerShape(999.dp), color = if (success) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer) {
        Text(text, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun McLoadingCard(label: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Row(modifier = Modifier.fillMaxWidth().padding(22.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun McEmptyCard(title: String, subtitle: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(modifier = Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Rounded.ManageAccounts, contentDescription = null, modifier = Modifier.size(30.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(title, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun dataDescription(kind: McDataKind): String = when (kind) {
    McDataKind.LANDS -> "展示你作为主人或成员参与的领地资料。"
    McDataKind.TELEPORTS -> "展示绑定账号拥有的传送点。"
    McDataKind.LOGINS -> "展示绑定账号最近的登录记录，最多返回 100 条。"
    McDataKind.FAKE_PLAYERS -> "展示绑定账号创建的在线假人。"
}

private fun landRoleLabel(role: String?): String = when (role) {
    "owner" -> "领地主人"
    "owner-readonly" -> "主人资料待同步"
    else -> "参与成员"
}

private fun adminRoleLabel(role: String): String = when (role) {
    "SUPERADMIN" -> "超级管理员"
    "TECHADMIN" -> "技术管理员"
    "ADMIN" -> "管理员"
    else -> role
}

private fun permissionMapSummary(permissions: Map<String, JsonElement>): String {
    if (permissions.isEmpty()) return "暂无权限数据"
    val enabled = permissions.values.count { it.asBoolean() }
    val names = permissions.entries.filter { it.value.asBoolean() }.take(4).joinToString("、") { it.key }
    return if (names.isBlank()) "$enabled/${permissions.size} 项已开启" else "$enabled/${permissions.size} 项已开启：$names"
}

private fun displayMcDate(value: String?): String = value?.let { runCatching { it.toDisplayTime() }.getOrDefault(it) } ?: "—"

private val mcEpochFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.systemDefault())

private fun displayEpoch(value: Long?): String = value?.let { runCatching { mcEpochFormatter.format(Instant.ofEpochMilli(it)) }.getOrDefault(it.toString()) } ?: "—"

private fun formatMcNumber(value: Double?): String {
    if (value == null) return "—"
    return if (value % 1.0 == 0.0) "%.0f".format(value) else "%.1f".format(value)
}
