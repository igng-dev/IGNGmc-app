package net.igng.mcstatus.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.igng.mcstatus.core.data.PlayerRepository
import net.igng.mcstatus.core.data.TicketRepository
import net.igng.mcstatus.core.designsystem.IgngAmbientCanvas
import net.igng.mcstatus.core.designsystem.IgngAvatar
import net.igng.mcstatus.core.designsystem.IgngBadge
import net.igng.mcstatus.core.designsystem.IgngBadgeTone
import net.igng.mcstatus.core.designsystem.IgngButton
import net.igng.mcstatus.core.designsystem.IgngButtonStyle
import net.igng.mcstatus.core.designsystem.IgngInteractiveTile
import net.igng.mcstatus.core.designsystem.IgngSectionHeader
import net.igng.mcstatus.core.designsystem.IgngTheme
import net.igng.mcstatus.core.designsystem.IgngTile
import net.igng.mcstatus.core.designsystem.IgngTopBar
import net.igng.mcstatus.core.designsystem.igngStaggeredReveal
import net.igng.mcstatus.core.model.DailyExpStatus
import net.igng.mcstatus.core.model.SavedAccount
import net.igng.mcstatus.core.model.SessionAuthStatus

enum class PlayerDestination {
    CHARACTERS,
    TICKETS,
    CREATE_TICKET,
    VAULT,
    ACTIVITY,
    LANDS,
    TELEPORTS,
    FAKE_PLAYERS,
    STATS,
    MEDALS,
    PERFECT,
    PERMISSIONS,
    ADMIN_CONSOLE,
    SETTINGS,
}

@Composable
fun PlayerCenterScreen(
    activeAccount: SavedAccount?,
    sessionAuthStatus: SessionAuthStatus = if (activeAccount != null) SessionAuthStatus.ACTIVE else SessionAuthStatus.SIGNED_OUT,
    sessionEpoch: Int = 0,
    dailyExp: DailyExpStatus?,
    playerRepository: PlayerRepository,
    ticketRepository: TicketRepository,
    bottomPadding: PaddingValues,
    onSignIn: () -> Unit,
    onOpenPassport: (Int) -> Unit,
    onNavigate: (PlayerDestination) -> Unit,
) {
    val colors = IgngTheme.colors
    var openTicketCount by remember(activeAccount?.userId) { mutableIntStateOf(0) }
    var boundCount by remember(activeAccount?.userId) { mutableStateOf<Int?>(null) }
    val sessionExpired = activeAccount != null && sessionAuthStatus.requiresReauth

    LaunchedEffect(activeAccount?.userId, sessionEpoch, sessionAuthStatus) {
        if (activeAccount == null || sessionAuthStatus != SessionAuthStatus.ACTIVE) {
            openTicketCount = 0
            boundCount = null
            return@LaunchedEffect
        }
        runCatching { ticketRepository.fetchList(relation = "all", includeClosed = false, page = 1) }
            .onSuccess { openTicketCount = it.total.coerceAtLeast(it.resolvedTickets.size) }
        runCatching { playerRepository.fetchBoundAccounts() }
            .onSuccess { boundCount = it.size }
    }

    IgngAmbientCanvas {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            IgngTopBar(
                title = "我的中心",
                subtitle = when {
                    sessionExpired -> "登录会话已失效 · 仅展示本地缓存与公开入口"
                    activeAccount != null -> "IGNG 玩家控制中心"
                    else -> "登录以管理角色、工单与游戏资产"
                },
                actions = {
                    IgngButton(
                        text = "设置",
                        onClick = { onNavigate(PlayerDestination.SETTINGS) },
                        style = IgngButtonStyle.Secondary,
                        icon = Icons.Rounded.Settings,
                        compact = true,
                    )
                },
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 6.dp,
                    bottom = bottomPadding.calculateBottomPadding() + 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (activeAccount == null) {
                    item(key = "signedOutHero") {
                        IgngTile(modifier = Modifier.fillMaxWidth()) {
                            IgngBadge("OAuth 2.1 · PKCE 安全登录", tone = IgngBadgeTone.Accent)
                            Spacer(Modifier.height(10.dp))
                            Text("登录你的 IGNG 账户", style = IgngTheme.type.pageTitle, color = colors.textPrimary)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "通过官方 SSO 页面完成授权，客户端不接触明文密码。若手机浏览器已登录任意 IGNG 站点，可一键完成授权并自动同步每日登录经验。",
                                style = IgngTheme.type.body,
                                color = colors.textSecondary,
                            )
                            Spacer(Modifier.height(14.dp))
                            IgngButton(
                                text = "通过 IGNG SSO 登录",
                                onClick = onSignIn,
                                style = IgngButtonStyle.Primary,
                                icon = Icons.AutoMirrored.Rounded.Login,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    return@LazyColumn
                }

                if (sessionExpired) {
                    item(key = "sessionExpiredBanner") {
                        val isLegacy = sessionAuthStatus == SessionAuthStatus.LEGACY_UPGRADE_REQUIRED
                        IgngTile(
                            borderColor = colors.warning.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            IgngBadge(
                                text = if (isLegacy) "需升级 OAuth 2.1 授权" else "登录会话已失效",
                                tone = IgngBadgeTone.Warning,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = if (isLegacy) {
                                    "当前账户仍保留旧版登录凭证，新版本需通过 IGNG SSO 重新授权以恢复角色管理、工单与跨服仓库等个人数据。"
                                } else {
                                    "当前仅展示本地缓存的账户标识与公开主页入口；角色管理、工单、仓库等个人数据需重新授权后同步。"
                                },
                                style = IgngTheme.type.caption,
                                color = colors.textSecondary,
                            )
                            Spacer(Modifier.height(12.dp))
                            IgngButton(
                                text = "立即重新授权",
                                onClick = onSignIn,
                                style = IgngButtonStyle.Primary,
                                icon = Icons.AutoMirrored.Rounded.Login,
                                compact = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                // 1. Player Passport Header Card
                item(key = "passport") {
                    IgngTile(modifier = Modifier.fillMaxWidth().igngStaggeredReveal(0)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            IgngAvatar(
                                name = activeAccount.displayName,
                                avatarUrl = activeAccount.avatarUrl,
                                size = 54.dp,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(activeAccount.displayName, style = IgngTheme.type.pageTitle, color = colors.textPrimary)
                                    if (sessionExpired) {
                                        IgngBadge("已失效", tone = IgngBadgeTone.Warning)
                                    } else {
                                        activeAccount.adminRole?.let { role ->
                                            IgngBadge(role.label, tone = IgngBadgeTone.Accent)
                                        }
                                    }
                                }
                                Text(
                                    text = "账户编号 #${activeAccount.userId} · @${activeAccount.username}",
                                    style = IgngTheme.type.caption,
                                    color = colors.textSecondary,
                                )
                                if (!sessionExpired && dailyExp != null && dailyExp.loggedIn) {
                                    Spacer(Modifier.height(6.dp))
                                    val expText = buildString {
                                        if (dailyExp.awarded) append("今日登录经验已入账 · ")
                                        dailyExp.level?.let { append("Lv.$it ") }
                                        dailyExp.exp?.let { append("(${it} EXP)") }
                                    }.ifBlank { "今日登录经验已结算" }
                                    IgngBadge(expText, tone = IgngBadgeTone.Success)
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IgngButton(
                                text = "我的公开主页",
                                onClick = { onOpenPassport(activeAccount.userId) },
                                style = IgngButtonStyle.Secondary,
                                icon = Icons.Rounded.AccountCircle,
                                compact = true,
                                modifier = Modifier.weight(1f),
                            )
                            IgngButton(
                                text = "创建工单",
                                onClick = { onNavigate(PlayerDestination.CREATE_TICKET) },
                                style = IgngButtonStyle.Primary,
                                icon = Icons.Rounded.ConfirmationNumber,
                                compact = true,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                // 2. Priority Todo Tile (Console Design Rule: Todos First)
                item(key = "todo") {
                    IgngInteractiveTile(
                        onClick = { onNavigate(PlayerDestination.TICKETS) },
                        accentBorder = if (openTicketCount > 0) colors.accent else null,
                        modifier = Modifier.fillMaxWidth().igngStaggeredReveal(1),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("我的工单", style = IgngTheme.type.cardTitle, color = colors.textPrimary)
                                Text(
                                    text = when {
                                        sessionExpired -> "重新授权后查看你的进行中工单与进度"
                                        openTicketCount > 0 -> "你有 $openTicketCount 张进行中的工单等待跟进"
                                        else -> "当前没有进行中的工单，点击查看历史记录"
                                    },
                                    style = IgngTheme.type.caption,
                                    color = colors.textSecondary,
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (openTicketCount > 0) {
                                    IgngBadge("$openTicketCount 进行中", tone = IgngBadgeTone.Accent)
                                }
                                Icon(Icons.Rounded.ChevronRight, null, tint = colors.textSecondary)
                            }
                        }
                    }
                }

                // 3. Core Service Group
                item(key = "coreGroup") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        IgngSectionHeader(title = "角色与服务")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CenterGridTile(
                                title = "我的角色",
                                subtitle = when {
                                    sessionExpired -> "重新授权后查看已绑定角色"
                                    boundCount != null -> "已绑定 $boundCount 个角色"
                                    else -> "改密、隐私与处罚"
                                },
                                icon = Icons.Rounded.Person,
                                tint = colors.accent,
                                onClick = { onNavigate(PlayerDestination.CHARACTERS) },
                                modifier = Modifier.weight(1f).igngStaggeredReveal(2),
                            )
                            CenterGridTile(
                                title = "跨服仓库",
                                subtitle = "物品附魔与流转追踪",
                                icon = Icons.Rounded.Inventory2,
                                tint = Color(0xFF8B5CF6),
                                onClick = { onNavigate(PlayerDestination.VAULT) },
                                modifier = Modifier.weight(1f).igngStaggeredReveal(3),
                            )
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CenterGridTile(
                                title = "登录与活动",
                                subtitle = "进服记录与安全回溯",
                                icon = Icons.Rounded.History,
                                tint = Color(0xFF06B6D4),
                                onClick = { onNavigate(PlayerDestination.ACTIVITY) },
                                modifier = Modifier.weight(1f).igngStaggeredReveal(4),
                            )
                            CenterGridTile(
                                title = "我的工单",
                                subtitle = "反馈、图片附件与进度",
                                icon = Icons.Rounded.ConfirmationNumber,
                                tint = Color(0xFF10B981),
                                onClick = { onNavigate(PlayerDestination.TICKETS) },
                                modifier = Modifier.weight(1f).igngStaggeredReveal(5),
                            )
                        }
                    }
                }

                // 4. World & Building Group
                item(key = "worldGroup") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        IgngSectionHeader(title = "世界与建造")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CenterGridTile(
                                title = "我的领地",
                                subtitle = "公开设置与描述编辑",
                                icon = Icons.Rounded.Map,
                                tint = Color(0xFF10B981),
                                onClick = { onNavigate(PlayerDestination.LANDS) },
                                modifier = Modifier.weight(1f),
                            )
                            CenterGridTile(
                                title = "传送点",
                                subtitle = "跨服坐标一键复制",
                                icon = Icons.Rounded.Explore,
                                tint = Color(0xFFF59E0B),
                                onClick = { onNavigate(PlayerDestination.TELEPORTS) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        CenterRowTile(
                            title = "我的假人",
                            subtitle = "实时血量、饥饿度与挂机位置",
                            icon = Icons.Rounded.SmartToy,
                            tint = colors.accent,
                            onClick = { onNavigate(PlayerDestination.FAKE_PLAYERS) },
                        )
                    }
                }

                // 5. Identity & Honors Group
                item(key = "identityGroup") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        IgngSectionHeader(title = "身份与荣誉")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CenterGridTile(
                                title = "游戏统计",
                                subtitle = "游玩、挖掘与生物图鉴",
                                icon = Icons.Rounded.BarChart,
                                tint = colors.accent,
                                onClick = { onNavigate(PlayerDestination.STATS) },
                                modifier = Modifier.weight(1f),
                            )
                            CenterGridTile(
                                title = "我的勋章",
                                subtitle = "荣誉陈列与自定义称号",
                                icon = Icons.Rounded.MilitaryTech,
                                tint = Color(0xFFF59E0B),
                                onClick = { onNavigate(PlayerDestination.MEDALS) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CenterGridTile(
                                title = "完美之人",
                                subtitle = "资格状态与权益额度",
                                icon = Icons.Rounded.WorkspacePremium,
                                tint = Color(0xFFEC4899),
                                onClick = { onNavigate(PlayerDestination.PERFECT) },
                                modifier = Modifier.weight(1f),
                            )
                            CenterGridTile(
                                title = "游戏权限",
                                subtitle = "LuckPerms 组与洞察",
                                icon = Icons.Rounded.Key,
                                tint = Color(0xFF8B5CF6),
                                onClick = { onNavigate(PlayerDestination.PERMISSIONS) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                // 6. Role-Gated Admin Pocket Console (Invisible to regular players or expired sessions)
                if (!sessionExpired && activeAccount.adminRole != null) {
                    item(key = "adminEntry") {
                        IgngInteractiveTile(
                            onClick = { onNavigate(PlayerDestination.ADMIN_CONSOLE) },
                            accentBorder = colors.warning,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(IgngTheme.radii.sm)
                                        .background(colors.warningBg),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Rounded.AdminPanelSettings, null, tint = colors.warning)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("管理员口袋控制台", style = IgngTheme.type.cardTitle, color = colors.textPrimary)
                                        IgngBadge(activeAccount.adminRole!!.label, tone = IgngBadgeTone.Warning)
                                    }
                                    Text("全站工单处理、玩家审计、观察周报与流量监控", style = IgngTheme.type.caption, color = colors.textSecondary)
                                }
                                Icon(Icons.Rounded.ChevronRight, null, tint = colors.textSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CenterGridTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = IgngTheme.colors
    IgngInteractiveTile(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(IgngTheme.radii.sm)
                .background(tint.copy(alpha = if (colors.isDark) 0.18f else 0.11f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(title, style = IgngTheme.type.cardTitle, color = colors.textPrimary)
        Spacer(Modifier.height(2.dp))
        Text(subtitle, style = IgngTheme.type.caption, color = colors.textSecondary)
    }
}

@Composable
private fun CenterRowTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
) {
    val colors = IgngTheme.colors
    IgngInteractiveTile(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(IgngTheme.radii.sm)
                    .background(tint.copy(alpha = if (colors.isDark) 0.18f else 0.11f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(19.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = IgngTheme.type.cardTitle, color = colors.textPrimary)
                Text(subtitle, style = IgngTheme.type.caption, color = colors.textSecondary)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = colors.textSecondary)
        }
    }
}
