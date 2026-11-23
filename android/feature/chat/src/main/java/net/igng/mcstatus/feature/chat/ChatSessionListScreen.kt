package net.igng.mcstatus.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.igng.mcstatus.core.data.ChatRepository
import net.igng.mcstatus.core.data.ChatSourcesResponse
import net.igng.mcstatus.core.designsystem.IgngAmbientCanvas
import net.igng.mcstatus.core.designsystem.IgngHapticKind
import net.igng.mcstatus.core.designsystem.IgngPullToRefreshBox
import net.igng.mcstatus.core.designsystem.IgngSearchField
import net.igng.mcstatus.core.designsystem.IgngSkeletonRows
import net.igng.mcstatus.core.designsystem.IgngStateBlock
import net.igng.mcstatus.core.designsystem.IgngTheme
import net.igng.mcstatus.core.designsystem.IgngTopBar
import net.igng.mcstatus.core.designsystem.rememberIgngHaptics
import net.igng.mcstatus.core.model.SavedAccount
import net.igng.mcstatus.core.model.SessionAuthStatus
import net.igng.mcstatus.core.model.SiteEndpoints

/**
 * Primary Chat Tab screen aligned with `chat.igng.net`'s unified sidebar (QQ / WeChat / MC / Private),
 * defaulting to the "全部" tab with WeChat-style session rows rendered in IGNG's design language.
 */
@Composable
fun ChatSessionListScreen(
    endpoints: SiteEndpoints,
    chatRepository: ChatRepository,
    activeAccount: SavedAccount?,
    sessionAuthStatus: SessionAuthStatus = if (activeAccount != null) SessionAuthStatus.ACTIVE else SessionAuthStatus.SIGNED_OUT,
    sessionEpoch: Int = 0,
    bottomPadding: PaddingValues,
    onSignIn: () -> Unit,
    onOpenSession: (ChatSessionItem) -> Unit,
    onOpenCommunityChannels: () -> Unit,
) {
    val colors = IgngTheme.colors
    val semantic = chatSemanticColors(colors.isDark)
    val scope = rememberCoroutineScope()
    val haptic = rememberIgngHaptics()
    val sessionExpired = activeAccount != null && sessionAuthStatus.requiresReauth
    val isAuthenticated = activeAccount != null && sessionAuthStatus == SessionAuthStatus.ACTIVE

    var sources by remember { mutableStateOf<ChatSourcesResponse?>(null) }
    var selectedTab by remember { mutableStateOf(ChatSessionTab.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun loadSources(isRefresh: Boolean = false, silent: Boolean = false) {
        if (isRefresh) refreshing = true else if (!silent && sources == null) loading = true
        if (!silent) error = null
        runCatching { chatRepository.fetchSources() }
            .onSuccess { sources = it }
            .onFailure { if (!silent) error = it.message ?: "会话列表暂时没有读到" }
        loading = false
        refreshing = false
    }

    // Initial load & reload when signed-in identity or re-auth epoch changes (unlocks private chats / WeChat groups)
    LaunchedEffect(activeAccount?.userId, sessionEpoch, sessionAuthStatus) {
        loadSources(isRefresh = false, silent = false)
    }

    // Background foreground refresh every 60s matching chat.igng.net
    LaunchedEffect(activeAccount?.userId, sessionEpoch, sessionAuthStatus) {
        while (true) {
            delay(60_000L)
            loadSources(silent = true)
        }
    }

    val allSessions = remember(sources) { sources?.let(::buildUnifiedSessions).orEmpty() }
    val hasPrivateChats = remember(allSessions) { allSessions.any { it.isPrivate } }
    val visibleTabs = remember(hasPrivateChats) {
        if (hasPrivateChats) {
            listOf(ChatSessionTab.ALL, ChatSessionTab.QQ, ChatSessionTab.WECHAT, ChatSessionTab.MC, ChatSessionTab.PRIVATE)
        } else {
            listOf(ChatSessionTab.ALL, ChatSessionTab.QQ, ChatSessionTab.WECHAT, ChatSessionTab.MC)
        }
    }
    LaunchedEffect(visibleTabs) {
        if (selectedTab !in visibleTabs) selectedTab = ChatSessionTab.ALL
    }

    val filteredSessions = remember(allSessions, selectedTab, searchQuery) {
        filterSessions(allSessions, selectedTab, searchQuery)
    }

    IgngAmbientCanvas {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            IgngTopBar(
                title = "聊天",
                subtitle = when {
                    allSessions.isNotEmpty() -> "${allSessions.size} 个同步会话 · QQ / 微信 / MC"
                    activeAccount == null -> "公开群聊与 MC 服务器实时档案"
                    else -> "社区跨平台实时聊天档案"
                },
                actions = {
                    Row(
                        modifier = Modifier
                            .clip(IgngTheme.radii.pill)
                            .background(colors.tile)
                            .border(1.dp, colors.lineStrong, IgngTheme.radii.pill)
                            .clickable {
                                haptic(IgngHapticKind.Tap)
                                onOpenCommunityChannels()
                            }
                            .padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Groups,
                            contentDescription = "官方交流群与社区",
                            tint = colors.accent,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "交流群",
                            style = IgngTheme.type.badge.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.textPrimary,
                            maxLines = 1,
                        )
                    }
                    IconButton(
                        onClick = {
                            haptic(IgngHapticKind.Tick)
                            scope.launch { loadSources(isRefresh = true) }
                        },
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "刷新会话列表",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
            )

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                IgngSearchField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "搜索会话、群名、群号或服务器…",
                )

                // Horizontal filter tabs defaulting to "全部"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    visibleTabs.forEach { tab ->
                        val count = remember(allSessions, tab) {
                            filterSessions(allSessions, tab, "").size
                        }
                        val picked = selectedTab == tab
                        SessionTabPill(
                            label = tab.label,
                            count = if (allSessions.isNotEmpty() && count > 0) count else null,
                            selected = picked,
                            onClick = {
                                haptic(IgngHapticKind.Tick)
                                selectedTab = tab
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            IgngPullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = { scope.launch { loadSources(isRefresh = true) } },
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    loading && allSessions.isEmpty() -> Box(Modifier.padding(16.dp)) { IgngSkeletonRows(6) }
                    error != null && allSessions.isEmpty() -> Box(Modifier.padding(16.dp)) {
                        IgngStateBlock(
                            title = "会话列表读取失败",
                            description = error,
                            isError = true,
                            actionLabel = "重试",
                            onAction = { scope.launch { loadSources(isRefresh = false) } },
                        )
                    }
                    filteredSessions.isEmpty() -> Box(Modifier.padding(16.dp)) {
                        when {
                            selectedTab == ChatSessionTab.WECHAT && searchQuery.isBlank() -> {
                                if (!isAuthenticated) {
                                    IgngStateBlock(
                                        title = if (sessionExpired) "重新授权后可查看微信会话" else "登录后可查看微信会话",
                                        description = if (sessionExpired) {
                                            "当前登录会话已失效，仅展示公开频道。重新授权后即可恢复微信群聊与私聊同步。"
                                        } else {
                                            "微信群聊与私聊仅向已登录且绑定微信的群成员开放，登录 IGNG 账号后即可自动同步。"
                                        },
                                        actionLabel = if (sessionExpired) "重新授权登录" else "登录 IGNG 账号",
                                        onAction = onSignIn,
                                    )
                                } else {
                                    IgngStateBlock(
                                        title = "暂无可见的微信会话",
                                        description = "在网页端账户中心绑定微信账号，且所在微信群由云萤同步后，即可在此查看记录。",
                                    )
                                }
                            }
                            searchQuery.isNotBlank() -> IgngStateBlock(
                                title = "没有匹配的会话",
                                description = "换个关键词或切换顶部分类标签再试试。",
                                actionLabel = "清空搜索",
                                onAction = { searchQuery = "" },
                            )
                            else -> IgngStateBlock(
                                title = "该分类下暂无会话",
                                description = "切换到「全部」查看其它平台频道。",
                                actionLabel = "查看全部",
                                onAction = { selectedTab = ChatSessionTab.ALL },
                            )
                        }
                    }
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 2.dp,
                            bottom = bottomPadding.calculateBottomPadding() + 20.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (activeAccount == null || sessionExpired) {
                            item(key = "guestHint") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(IgngTheme.radii.md)
                                        .background(if (sessionExpired) colors.warningBg else colors.accentSoft)
                                        .border(
                                            0.8.dp,
                                            (if (sessionExpired) colors.warning else colors.accent).copy(alpha = 0.35f),
                                            IgngTheme.radii.md,
                                        )
                                        .clickable(onClick = onSignIn)
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = if (sessionExpired) {
                                            "登录会话已失效，当前仅显示公开聊天 · 点击重新授权"
                                        } else {
                                            "登录 IGNG 账号可查看微信群、私聊与完整昵称"
                                        },
                                        style = IgngTheme.type.caption.copy(fontWeight = FontWeight.Medium),
                                        color = if (sessionExpired) colors.warning else colors.accent,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (sessionExpired) "重新授权" else "去登录",
                                        style = IgngTheme.type.badge,
                                        color = if (sessionExpired) colors.warning else colors.accent,
                                    )
                                }
                            }
                        }

                        // Unified WeChat-style session container card
                        item(key = "sessionCardList") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(IgngTheme.radii.lg)
                                    .background(colors.tile)
                                    .border(1.dp, colors.line, IgngTheme.radii.lg),
                            ) {
                                filteredSessions.forEachIndexed { idx, session ->
                                    if (idx > 0) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(start = 72.dp, end = 14.dp),
                                            thickness = 0.5.dp,
                                            color = colors.line,
                                        )
                                    }
                                    WeChatStyleSessionRow(
                                        session = session,
                                        endpoints = endpoints,
                                        semantic = semantic,
                                        onClick = {
                                            haptic(IgngHapticKind.Tick)
                                            onOpenSession(session)
                                        },
                                    )
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
private fun SessionTabPill(
    label: String,
    count: Int?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = IgngTheme.colors
    Row(
        modifier = Modifier
            .clip(IgngTheme.radii.pill)
            .background(if (selected) colors.accent else colors.tile)
            .border(1.dp, if (selected) colors.accent else colors.line, IgngTheme.radii.pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = label,
            style = IgngTheme.type.caption.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium),
            color = if (selected) colors.onAccent else colors.textSecondary,
        )
        if (count != null) {
            Text(
                text = count.toString(),
                style = IgngTheme.type.badge.copy(fontSize = 10.sp),
                color = if (selected) colors.onAccent.copy(alpha = 0.85f) else colors.textMuted,
            )
        }
    }
}

@Composable
private fun WeChatStyleSessionRow(
    session: ChatSessionItem,
    endpoints: SiteEndpoints,
    semantic: ChatSemanticColors,
    onClick: () -> Unit,
) {
    val colors = IgngTheme.colors
    val timeText = remember(session.lastMessageAt) { formatSessionTime(session.lastMessageAt) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 46dp rounded-square avatar + 16dp platform micro-badge
        SessionPlatformAvatar(
            kind = session.kind,
            avatarUrl = endpoints.resolveApiMediaUrl(session.avatarUrl),
            contentDescription = session.name,
            semantic = semantic,
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = session.name,
                    style = IgngTheme.type.cardTitle.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (session.isAdminGroup) {
                    InlineMiniTag("管理", colors.dangerBg, colors.danger)
                }
                if (session.isTechGroup) {
                    InlineMiniTag("技术", colors.accentSoft, colors.accent)
                }
                if (session.isPrivate) {
                    InlineMiniTag("私聊", semantic.privateSoft, semantic.privateAccent)
                }
                Spacer(Modifier.weight(1f))
                if (timeText.isNotBlank()) {
                    Text(
                        text = timeText,
                        style = IgngTheme.type.badge.copy(fontFeatureSettings = "tnum"),
                        color = colors.textMuted,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (session.previewHasAtAll) {
                    Text(
                        text = "[有人@全体]",
                        style = IgngTheme.type.caption.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.warning,
                        maxLines = 1,
                    )
                }
                Text(
                    text = session.formattedPreview,
                    style = IgngTheme.type.caption.copy(fontSize = 13.sp),
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
fun SessionPlatformAvatar(
    kind: ChatSessionKind,
    avatarUrl: String?,
    contentDescription: String,
    semantic: ChatSemanticColors,
    sizeDp: Int = 46,
) {
    val colors = IgngTheme.colors
    val (bg, fg, icon, badgeBg, badgeLabel) = when (kind) {
        ChatSessionKind.QQ_GROUP -> AvatarStyle(semantic.qqSoft, semantic.qq, Icons.Rounded.Groups, semantic.qq, "Q")
        ChatSessionKind.QQ_PRIVATE -> AvatarStyle(semantic.privateSoft, semantic.privateAccent, Icons.Rounded.Person, semantic.privateAccent, "私")
        ChatSessionKind.WECHAT_GROUP -> AvatarStyle(semantic.wechatSoft, semantic.wechat, Icons.Rounded.ChatBubble, semantic.wechat, "微")
        ChatSessionKind.WECHAT_PRIVATE -> AvatarStyle(semantic.wechatSoft, semantic.wechat, Icons.Rounded.Person, semantic.wechat, "微")
        ChatSessionKind.MC_SERVER -> AvatarStyle(semantic.serverSoft, semantic.server, Icons.Rounded.Dns, semantic.server, "MC")
    }
    val shape = RoundedCornerShape(12.dp)

    Box(modifier = Modifier.size(sizeDp.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(bg)
                .border(0.5.dp, colors.line, shape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = fg,
                modifier = Modifier.size((sizeDp * 0.48f).dp),
            )
            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // Bottom-right platform micro-badge
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(17.dp)
                .clip(CircleShape)
                .background(colors.tile)
                .padding(1.5.dp)
                .clip(CircleShape)
                .background(badgeBg),
            contentAlignment = Alignment.Center,
        ) {
            if (kind == ChatSessionKind.QQ_PRIVATE) {
                Icon(Icons.Rounded.Lock, null, tint = Color.White, modifier = Modifier.size(9.dp))
            } else {
                Text(
                    text = badgeLabel,
                    style = IgngTheme.type.badge.copy(
                        fontSize = if (badgeLabel.length > 1) 7.sp else 8.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 9.sp,
                    ),
                    color = Color.White,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun InlineMiniTag(label: String, bg: Color, fg: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(bg)
            .padding(horizontal = 5.dp, vertical = 1.5.dp),
    ) {
        Text(
            text = label,
            style = IgngTheme.type.badge.copy(fontSize = 10.sp),
            color = fg,
            maxLines = 1,
        )
    }
}

private data class AvatarStyle(
    val bg: Color,
    val fg: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val badgeBg: Color,
    val badgeLabel: String,
)
