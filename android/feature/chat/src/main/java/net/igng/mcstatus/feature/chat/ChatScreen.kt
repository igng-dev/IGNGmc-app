package net.igng.mcstatus.feature.chat

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import net.igng.mcstatus.core.auth.CustomTabsLauncher
import net.igng.mcstatus.core.data.ChatForwardResponse
import net.igng.mcstatus.core.data.ChatMessageDto
import net.igng.mcstatus.core.data.ChatRepository
import net.igng.mcstatus.core.data.ChatSourcesResponse
import net.igng.mcstatus.core.designsystem.IgngAmbientCanvas
import net.igng.mcstatus.core.designsystem.IgngBadge
import net.igng.mcstatus.core.designsystem.IgngBadgeTone
import net.igng.mcstatus.core.designsystem.IgngButton
import net.igng.mcstatus.core.designsystem.IgngButtonStyle
import net.igng.mcstatus.core.designsystem.IgngCopyPill
import net.igng.mcstatus.core.designsystem.IgngDetailSheet
import net.igng.mcstatus.core.designsystem.IgngHapticKind
import net.igng.mcstatus.core.designsystem.IgngImageLightbox
import net.igng.mcstatus.core.designsystem.IgngLoadingBlock
import net.igng.mcstatus.core.designsystem.IgngPullToRefreshBox
import net.igng.mcstatus.core.designsystem.IgngSearchField
import net.igng.mcstatus.core.designsystem.IgngSectionHeader
import net.igng.mcstatus.core.designsystem.IgngSegmentedTabs
import net.igng.mcstatus.core.designsystem.IgngSkeletonRows
import net.igng.mcstatus.core.designsystem.IgngStateBlock
import net.igng.mcstatus.core.designsystem.IgngTheme
import net.igng.mcstatus.core.designsystem.IgngTile
import net.igng.mcstatus.core.designsystem.IgngTopBar
import net.igng.mcstatus.core.designsystem.igngStaggeredReveal
import net.igng.mcstatus.core.designsystem.rememberIgngHaptics
import net.igng.mcstatus.core.model.IgngFormatters
import net.igng.mcstatus.core.model.IgngSocialChannels
import net.igng.mcstatus.core.model.SiteEndpoints

private enum class ChatMode(val label: String) {
    MC("MC 聊天"),
    QQ("QQ 群聊"),
    SOCIAL("交流群"),
}

@Composable
fun ChatScreen(
    endpoints: SiteEndpoints,
    chatRepository: ChatRepository,
    bottomPadding: PaddingValues,
) {
    val colors = IgngTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = rememberIgngHaptics()

    var mode by remember { mutableStateOf(ChatMode.MC) }
    var sources by remember { mutableStateOf<ChatSourcesResponse?>(null) }
    var selectedServerId by remember { mutableStateOf<Int?>(null) }
    var selectedGroupId by remember { mutableStateOf<String?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var atAllOnly by remember { mutableStateOf(false) }

    var messages by remember { mutableStateOf<List<ChatMessageDto>>(emptyList()) }
    var hasMore by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var previewImageUrl by remember { mutableStateOf<String?>(null) }
    var forwardDetail by remember { mutableStateOf<ChatForwardResponse?>(null) }
    var loadingForward by remember { mutableStateOf(false) }

    suspend fun loadMessages(reset: Boolean, isRefresh: Boolean = false) {
        if (mode == ChatMode.SOCIAL) return
        if (reset) {
            if (isRefresh) refreshing = true else loading = true
            error = null
        } else {
            loadingMore = true
        }
        runCatching {
            val src = sources ?: chatRepository.fetchSources().also { loaded ->
                sources = loaded
                if (selectedServerId == null) selectedServerId = loaded.servers.firstOrNull()?.id
                if (selectedGroupId == null) selectedGroupId = loaded.qqGroups.firstOrNull()?.id
            }
            val beforeId = if (reset) null else messages.lastOrNull()?.id
            val resp = chatRepository.fetchMessages(
                source = if (mode == ChatMode.QQ) "qq" else "mc",
                serverId = if (mode == ChatMode.MC) selectedServerId ?: src.servers.firstOrNull()?.id else null,
                groupId = if (mode == ChatMode.QQ) selectedGroupId ?: src.qqGroups.firstOrNull()?.id else null,
                query = query,
                atAll = mode == ChatMode.QQ && atAllOnly,
                beforeId = beforeId,
            )
            messages = if (reset) resp.messages else (messages + resp.messages).distinctBy { it.id }
            hasMore = resp.hasMore
        }.onFailure {
            error = it.message ?: "聊天记录暂时没有读到"
        }
        loading = false
        loadingMore = false
        refreshing = false
    }

    LaunchedEffect(mode, selectedServerId, selectedGroupId, atAllOnly) {
        if (mode != ChatMode.SOCIAL) loadMessages(reset = true)
    }

    IgngAmbientCanvas {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            IgngTopBar(
                title = "聊天与交流",
                subtitle = "来自 chat.igng.net 实时存档",
                actions = {
                    if (mode != ChatMode.SOCIAL) {
                        IgngButton(
                            text = "筛选",
                            onClick = { showFilters = !showFilters },
                            style = if (showFilters || query.isNotBlank() || atAllOnly) IgngButtonStyle.Primary else IgngButtonStyle.Secondary,
                            icon = Icons.Rounded.FilterList,
                            compact = true,
                        )
                    }
                },
            )

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                IgngSegmentedTabs(
                    items = ChatMode.entries,
                    selected = mode,
                    labelOf = { it.label },
                    onSelect = { mode = it },
                )

                // Channel selector strip
                if (mode == ChatMode.MC && sources != null && sources!!.servers.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        sources!!.servers.forEach { srv ->
                            val picked = srv.id == selectedServerId
                            ChannelChip(srv.name, picked) {
                                haptic(IgngHapticKind.Tick)
                                selectedServerId = srv.id
                            }
                        }
                    }
                } else if (mode == ChatMode.QQ && sources != null && sources!!.qqGroups.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        sources!!.qqGroups.forEach { grp ->
                            val picked = grp.id == selectedGroupId
                            ChannelChip(grp.name, picked) {
                                haptic(IgngHapticKind.Tick)
                                selectedGroupId = grp.id
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = showFilters && mode != ChatMode.SOCIAL) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        IgngSearchField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = "按消息关键词搜索，回车确认",
                            onSubmit = { scope.launch { loadMessages(reset = true) } },
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (mode == ChatMode.QQ) {
                                ChannelChip("只看 @全体", atAllOnly) { atAllOnly = !atAllOnly }
                            }
                            IgngButton(
                                text = "应用筛选",
                                onClick = { scope.launch { loadMessages(reset = true) } },
                                style = IgngButtonStyle.Primary,
                                compact = true,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            if (mode == ChatMode.SOCIAL) {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = bottomPadding.calculateBottomPadding() + 20.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { IgngSectionHeader(title = "官方 QQ 交流群", subtitle = "点击群号一键复制") }
                    itemsIndexed(IgngSocialChannels.qqGroups, key = { _, g -> g.key }) { idx, group ->
                        IgngTile(modifier = Modifier.fillMaxWidth().igngStaggeredReveal(idx)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(group.label, style = IgngTheme.type.cardTitle, color = colors.textPrimary)
                                IgngBadge(group.badge, tone = IgngBadgeTone.Accent)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(group.summary, style = IgngTheme.type.body, color = colors.textSecondary)
                            group.copyValue?.let { num ->
                                Spacer(Modifier.height(10.dp))
                                IgngCopyPill(value = num, label = "群号 $num", modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                    item { IgngSectionHeader(title = "更多 IGNG 社区平台") }
                    items(IgngSocialChannels.externalChannels, key = { it.key }) { ch ->
                        IgngTile(modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(ch.label, style = IgngTheme.type.cardTitle, color = colors.textPrimary)
                                    Spacer(Modifier.height(3.dp))
                                    Text(ch.summary, style = IgngTheme.type.caption, color = colors.textSecondary)
                                }
                                Spacer(Modifier.width(10.dp))
                                ch.url?.let { url ->
                                    IgngButton(
                                        text = "打开",
                                        onClick = { CustomTabsLauncher.openExternalUrl(context, url, colors.isDark) },
                                        style = IgngButtonStyle.Secondary,
                                        icon = Icons.Rounded.OpenInBrowser,
                                        compact = true,
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                IgngPullToRefreshBox(
                    isRefreshing = refreshing,
                    onRefresh = { scope.launch { loadMessages(reset = true, isRefresh = true) } },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    when {
                        loading && messages.isEmpty() -> Box(Modifier.padding(16.dp)) { IgngSkeletonRows(5) }
                        error != null && messages.isEmpty() -> Box(Modifier.padding(16.dp)) {
                            IgngStateBlock(
                                title = "聊天记录没有读到",
                                description = error,
                                isError = true,
                                actionLabel = "重试",
                                onAction = { scope.launch { loadMessages(reset = true) } },
                            )
                        }
                        messages.isEmpty() -> Box(Modifier.padding(16.dp)) {
                            IgngStateBlock(title = "还没有匹配的聊天记录", description = "切换频道或放宽筛选条件再试。")
                        }
                        else -> LazyColumn(
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 4.dp,
                                bottom = bottomPadding.calculateBottomPadding() + 20.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            itemsIndexed(messages, key = { _, m -> m.id }) { idx, msg ->
                                ChatMessageCard(
                                    message = msg,
                                    endpoints = endpoints,
                                    onPreviewImage = { previewImageUrl = it },
                                    onOpenForward = {
                                        scope.launch {
                                            loadingForward = true
                                            forwardDetail = runCatching { chatRepository.fetchForwardDetail(msg.id) }.getOrNull()
                                            loadingForward = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().igngStaggeredReveal(idx),
                                )
                            }
                            if (hasMore) {
                                item(key = "loadMore") {
                                    Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                        IgngButton(
                                            text = "加载更早的消息",
                                            onClick = { scope.launch { loadMessages(reset = false) } },
                                            style = IgngButtonStyle.Secondary,
                                            loading = loadingMore,
                                            compact = true,
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

    previewImageUrl?.let { url ->
        IgngImageLightbox(imageUrl = url, title = "聊天图片", onDismiss = { previewImageUrl = null })
    }

    if (loadingForward || forwardDetail != null) {
        IgngDetailSheet(
            onDismiss = { forwardDetail = null; loadingForward = false },
            title = forwardDetail?.title ?: "合并转发消息",
            subtitle = forwardDetail?.let { "共 ${it.resolvedNodes.size} 条转发内容" },
        ) {
            if (loadingForward) {
                IgngLoadingBlock(compact = true)
            } else {
                forwardDetail?.resolvedNodes?.forEach { node ->
                    IgngTile(soft = true, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), contentPadding = PaddingValues(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(node.author, style = IgngTheme.type.caption.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                            Text(IgngFormatters.formatIsoDateTime(node.sent_at ?: node.time, short = true), style = IgngTheme.type.badge, color = colors.textMuted)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(node.content, style = IgngTheme.type.body, color = colors.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = IgngTheme.colors
    Box(
        modifier = Modifier
            .clip(IgngTheme.radii.pill)
            .background(if (selected) colors.accentSoft else colors.tile)
            .border(1.dp, if (selected) colors.accent else colors.line, IgngTheme.radii.pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = IgngTheme.type.caption.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium),
            color = if (selected) colors.accent else colors.textSecondary,
        )
    }
}

@Composable
private fun ChatMessageCard(
    message: ChatMessageDto,
    endpoints: SiteEndpoints,
    onPreviewImage: (String) -> Unit,
    onOpenForward: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = IgngTheme.colors
    IgngTile(modifier = modifier, contentPadding = PaddingValues(13.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = message.player_name.ifBlank { "匿名" },
                    style = IgngTheme.type.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary,
                )
                if (message.is_at_all) {
                    IgngBadge("@全体", tone = IgngBadgeTone.Warning)
                }
            }
            Text(
                text = IgngFormatters.formatIsoDateTime(message.sent_at, short = true),
                style = IgngTheme.type.badge,
                color = colors.textMuted,
            )
        }
        if (message.content.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(message.content, style = IgngTheme.type.body, color = colors.textSecondary)
        }
        val attachments = message.allAttachments
        if (attachments.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                attachments.forEach { att ->
                    val resolved = endpoints.resolveChatMediaUrl(
                        att.previewUrl ?: att.url ?: "/api/chatlogs/media/${message.id}/${att.index}",
                    )
                    if (resolved != null) {
                        AsyncImage(
                            model = resolved,
                            contentDescription = "图片附件",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(92.dp)
                                .clip(IgngTheme.radii.sm)
                                .border(1.dp, colors.line, IgngTheme.radii.sm)
                                .clickable { onPreviewImage(resolved) },
                        )
                    }
                }
            }
        }
        if (message.has_forward || message.content.contains("[合并转发")) {
            Spacer(Modifier.height(8.dp))
            IgngButton(
                text = "展开合并转发",
                onClick = onOpenForward,
                style = IgngButtonStyle.Secondary,
                icon = Icons.Rounded.Forum,
                compact = true,
            )
        }
    }
}
