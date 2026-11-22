package net.igng.mcstatus.feature.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.igng.mcstatus.core.data.ChatForwardResponse
import net.igng.mcstatus.core.data.ChatMessageDto
import net.igng.mcstatus.core.data.ChatPersonaDto
import net.igng.mcstatus.core.data.ChatRepository
import net.igng.mcstatus.core.data.ChatSenderProfileDto
import net.igng.mcstatus.core.designsystem.IgngAmbientCanvas
import net.igng.mcstatus.core.designsystem.IgngButton
import net.igng.mcstatus.core.designsystem.IgngButtonStyle
import net.igng.mcstatus.core.designsystem.IgngDetailSheet
import net.igng.mcstatus.core.designsystem.IgngHapticKind
import net.igng.mcstatus.core.designsystem.IgngImageLightbox
import net.igng.mcstatus.core.designsystem.IgngSkeletonRows
import net.igng.mcstatus.core.designsystem.IgngStateBlock
import net.igng.mcstatus.core.designsystem.IgngTheme
import net.igng.mcstatus.core.designsystem.copyTextToClipboard
import net.igng.mcstatus.core.designsystem.rememberIgngHaptics
import net.igng.mcstatus.core.model.SavedAccount
import net.igng.mcstatus.core.model.SiteEndpoints

private data class LightboxPayload(
    val urls: List<String>,
    val initialIndex: Int,
    val titles: List<String?>,
)

private data class VideoPayload(
    val path: String,
    val title: String,
)

/**
 * Full-screen immersive conversation view (hides bottom nav bar) with WeChat/QQ-style mobile bubbles,
 * chronological bottom anchoring, top-edge older pagination, foreground polling, and platform-specific cards.
 */
@Composable
fun ChatConversationScreen(
    source: String,
    targetId: String,
    platform: String,
    isPrivate: Boolean,
    title: String,
    sessionAvatarUrl: String?,
    isAdminGroup: Boolean,
    isTechGroup: Boolean,
    initialPersonaLabel: String?,
    initialPersonaDescription: String?,
    initialFocusMessageId: String?,
    endpoints: SiteEndpoints,
    chatRepository: ChatRepository,
    activeAccount: SavedAccount?,
    boundMcUsernames: List<String>,
    onBack: () -> Unit,
    onOpenPlayerPassport: (Int) -> Unit,
    onOpenMcCharacter: (String) -> Unit,
) {
    val colors = IgngTheme.colors
    val semantic = chatSemanticColors(colors.isDark)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = rememberIgngHaptics()
    val listState = rememberLazyListState()
    val boundMcSet = remember(boundMcUsernames) { boundMcUsernames.toSet() }

    val sessionKind = remember(source, platform, isPrivate) {
        when {
            source == "server" -> ChatSessionKind.MC_SERVER
            platform == "wechat" && isPrivate -> ChatSessionKind.WECHAT_PRIVATE
            platform == "wechat" -> ChatSessionKind.WECHAT_GROUP
            isPrivate -> ChatSessionKind.QQ_PRIVATE
            else -> ChatSessionKind.QQ_GROUP
        }
    }

    var filter by remember {
        mutableStateOf(ChatFilterState(focusedId = initialFocusMessageId.orEmpty()))
    }
    var searchHighlight by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<ChatMessageDto>>(emptyList()) }
    var profiles by remember { mutableStateOf<Map<String, ChatSenderProfileDto>>(emptyMap()) }
    var persona by remember {
        mutableStateOf(
            initialPersonaLabel?.takeIf { it.isNotBlank() }?.let {
                ChatPersonaDto(label = it, description = initialPersonaDescription.orEmpty())
            },
        )
    }
    var hasMore by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var loadingOlder by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var newBelowCount by remember { mutableIntStateOf(0) }

    // Interactive overlays
    var showSearchSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showPersonaSheet by remember { mutableStateOf(false) }
    var lightbox by remember { mutableStateOf<LightboxPayload?>(null) }
    var videoPlayer by remember { mutableStateOf<VideoPayload?>(null) }
    var contextActionTarget by remember { mutableStateOf<Pair<ChatMessageDto, ResolvedChatSender>?>(null) }

    var forwardMessageId by remember { mutableStateOf<String?>(null) }
    var forwardDetail by remember { mutableStateOf<ChatForwardResponse?>(null) }
    var forwardLoading by remember { mutableStateOf(false) }
    var forwardError by remember { mutableStateOf<String?>(null) }

    val audioController = remember { ChatAudioPlayerController() }
    DisposableEffect(Unit) {
        onDispose { audioController.stop() }
    }
    LaunchedEffect(audioController.isPlaying) {
        while (audioController.isPlaying) {
            audioController.pollProgress()
            delay(250L)
        }
    }

    val timeline = remember(messages, profiles, activeAccount?.userId, boundMcSet) {
        buildChatTimeline(messages, profiles, activeAccount, boundMcSet)
    }

    val isNearBottom by remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            if (total == 0) true
            else {
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisible >= total - 2
            }
        }
    }
    LaunchedEffect(isNearBottom) {
        if (isNearBottom) newBelowCount = 0
    }

    suspend fun scrollToMessageOrBottom(targetMsgId: String?, smooth: Boolean = false) {
        val total = listState.layoutInfo.totalItemsCount
        if (total == 0) return
        if (!targetMsgId.isNullOrBlank()) {
            val idx = timeline.indexOfFirst { item ->
                when (item) {
                    is ChatTimelineItem.MessageGroup -> item.messages.any { it.id == targetMsgId }
                    is ChatTimelineItem.SystemNotice -> item.message.id == targetMsgId
                    else -> false
                }
            }
            if (idx >= 0) {
                // Account for the top history loader item at index 0
                val listIndex = idx + 1
                if (smooth) listState.animateScrollToItem(listIndex) else listState.scrollToItem(listIndex)
                return
            }
        }
        val bottomIndex = (total - 1).coerceAtLeast(0)
        if (smooth) listState.animateScrollToItem(bottomIndex) else listState.scrollToItem(bottomIndex)
    }

    suspend fun loadMessages(
        older: Boolean = false,
        silent: Boolean = false,
        activeFilter: ChatFilterState = filter,
    ) {
        if (older) {
            if (loadingOlder || !hasMore) return
            loadingOlder = true
        } else if (!silent) {
            loading = true
            error = null
        }

        val dayRange = activeFilter.dayKey.takeIf { it.isNotBlank() }?.let(::dayKeyToUtcRange)
        val beforeId = if (older) messages.firstOrNull()?.id else null
        val aroundId = if (!older && activeFilter.focusedId.isNotBlank()) activeFilter.focusedId else null

        runCatching {
            chatRepository.fetchMessages(
                source = source,
                serverId = if (source == "server") targetId.toIntOrNull() else null,
                groupId = if (source == "qq") targetId else null,
                senderId = activeFilter.senderId,
                atAll = activeFilter.atAll,
                kind = activeFilter.kind,
                beforeId = beforeId,
                aroundId = aroundId,
                start = dayRange?.first,
                end = dayRange?.second,
                limit = 80,
            )
        }.onSuccess { resp ->
            profiles = profiles + resp.senderProfiles
            if (resp.persona != null) persona = resp.persona
            val prevCount = messages.size
            val prevLastId = messages.lastOrNull()?.id
            val wasAtBottom = isNearBottom

            when {
                older -> {
                    val anchorMessageId = messages.firstOrNull()?.id
                    messages = mergeChatMessages(messages, resp.messages, pruneWindow = false)
                    hasMore = resp.hasMore
                    if (anchorMessageId != null && resp.messages.isNotEmpty()) {
                        // Preserve scroll position near previous top message
                        delay(16)
                        val newTimeline = buildChatTimeline(messages, profiles, activeAccount, boundMcSet)
                        val anchorIdx = newTimeline.indexOfFirst { item ->
                            item is ChatTimelineItem.MessageGroup && item.messages.any { it.id == anchorMessageId }
                        }
                        if (anchorIdx >= 0) {
                            listState.scrollToItem(anchorIdx + 1)
                        }
                    }
                }
                silent -> {
                    val merged = mergeChatMessages(messages, resp.messages, pruneWindow = true)
                    val addedTail = if (prevLastId != null) {
                        val idx = merged.indexOfLast { it.id == prevLastId }
                        if (idx >= 0) (merged.size - 1 - idx).coerceAtLeast(0) else 0
                    } else {
                        (merged.size - prevCount).coerceAtLeast(0)
                    }
                    messages = merged
                    if (addedTail > 0) {
                        if (wasAtBottom) {
                            delay(24)
                            scrollToMessageOrBottom(null, smooth = true)
                        } else {
                            newBelowCount += addedTail
                        }
                    }
                }
                else -> {
                    messages = mergeChatMessages(emptyList(), resp.messages, pruneWindow = false)
                    hasMore = resp.hasMore
                    newBelowCount = 0
                    delay(32)
                    scrollToMessageOrBottom(activeFilter.focusedId.takeIf { it.isNotBlank() }, smooth = false)
                }
            }
        }.onFailure { err ->
            if (!silent) error = err.message ?: "消息读取失败"
        }

        loading = false
        loadingOlder = false
    }

    // Load whenever filter changes
    LaunchedEffect(source, targetId, filter) {
        loadMessages(older = false, silent = false, activeFilter = filter)
    }

    // Silent foreground polling every 6s when viewing live latest stream
    LaunchedEffect(source, targetId, filter) {
        if (filter.isActive) return@LaunchedEffect
        while (true) {
            delay(6_000L)
            if (!loading && !loadingOlder) {
                loadMessages(older = false, silent = true, activeFilter = filter)
            }
        }
    }

    // Auto-load older messages when user scrolls to top
    val shouldLoadOlder by remember {
        derivedStateOf {
            hasMore && !loading && !loadingOlder && messages.isNotEmpty() &&
                listState.firstVisibleItemIndex == 0 &&
                listState.firstVisibleItemScrollOffset < 20
        }
    }
    LaunchedEffect(shouldLoadOlder) {
        if (shouldLoadOlder) {
            loadMessages(older = true, silent = false, activeFilter = filter)
        }
    }

    fun openForward(msg: ChatMessageDto) {
        forwardMessageId = msg.id
        forwardDetail = null
        forwardError = null
        forwardLoading = true
        scope.launch {
            runCatching { chatRepository.fetchForwardDetail(msg.id) }
                .onSuccess { forwardDetail = it }
                .onFailure { forwardError = it.message ?: "转发消息读取失败" }
            forwardLoading = false
        }
    }

    val displaySubtitle = remember(sessionKind, targetId, activeAccount, isAdminGroup, isTechGroup) {
        buildList {
            add(sessionKind.compactLabel)
            if (sessionKind == ChatSessionKind.QQ_GROUP && activeAccount != null && targetId.all(Char::isDigit)) {
                add(targetId)
            }
            if (isAdminGroup) add("管理群")
            if (isTechGroup) add("技术群")
        }.joinToString(" · ")
    }

    IgngAmbientCanvas {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            ) {
                // Immersive conversation header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.tile.copy(alpha = 0.94f))
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回会话列表", tint = colors.textPrimary)
                    }
                    SessionPlatformAvatar(
                        kind = sessionKind,
                        avatarUrl = endpoints.resolveApiMediaUrl(sessionAvatarUrl),
                        contentDescription = title,
                        semantic = semantic,
                        sizeDp = 36,
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title.ifBlank { sessionKind.label },
                            style = IgngTheme.type.cardTitle.copy(fontSize = 15.5.sp),
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = displaySubtitle,
                                style = IgngTheme.type.badge,
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val activePersona = persona
                            if (source == "qq" && activePersona != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(semantic.personaBg)
                                        .clickable { showPersonaSheet = true }
                                        .padding(horizontal = 5.dp, vertical = 1.dp),
                                ) {
                                    Text(
                                        text = "人设: ${activePersona.label}",
                                        style = IgngTheme.type.badge.copy(fontSize = 10.sp),
                                        color = semantic.personaFg,
                                    )
                                }
                            }
                        }
                    }

                    IconButton(onClick = { showSearchSheet = true }) {
                        Icon(Icons.Rounded.Search, contentDescription = "搜索消息", tint = colors.textSecondary)
                    }
                    Box {
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(
                                imageVector = Icons.Rounded.FilterList,
                                contentDescription = "筛选消息",
                                tint = if (filter.isActive) colors.accent else colors.textSecondary,
                            )
                        }
                        if (filter.isActive) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 10.dp, end = 10.dp)
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(colors.accent),
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            haptic(IgngHapticKind.Tick)
                            scope.launch { loadMessages(older = false, silent = false, activeFilter = filter) }
                        },
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "刷新", tint = colors.textSecondary)
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = colors.line)

                // Active filter strip (shown only when any filter/focus is active)
                AnimatedVisibility(visible = filter.isActive) {
                    ActiveFilterStrip(
                        filter = filter,
                        source = source,
                        platform = platform,
                        onStepDay = { delta ->
                            val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                            val cur = runCatching { LocalDate.parse(filter.dayKey, fmt) }.getOrNull() ?: return@ActiveFilterStrip
                            val next = cur.plusDays(delta.toLong())
                            if (!next.isAfter(LocalDate.now())) {
                                filter = filter.copy(dayKey = next.format(fmt), focusedId = "")
                            }
                        },
                        onClearDay = { filter = filter.copy(dayKey = "") },
                        onClearSender = { filter = filter.copy(senderId = "") },
                        onClearAtAll = { filter = filter.copy(atAll = false) },
                        onClearKind = { filter = filter.copy(kind = "") },
                        onClearFocus = {
                            searchHighlight = ""
                            filter = filter.copy(focusedId = "")
                        },
                        onResetAll = {
                            searchHighlight = ""
                            filter = ChatFilterState()
                        },
                    )
                }

                // Message stream body
                when {
                    loading && messages.isEmpty() -> Box(Modifier.fillMaxSize().padding(16.dp)) { IgngSkeletonRows(6) }
                    error != null && messages.isEmpty() -> Box(Modifier.fillMaxSize().padding(16.dp)) {
                        IgngStateBlock(
                            title = "消息记录读取失败",
                            description = error,
                            isError = true,
                            actionLabel = "重试",
                            onAction = { scope.launch { loadMessages(older = false, silent = false) } },
                        )
                    }
                    messages.isEmpty() -> Box(Modifier.fillMaxSize().padding(16.dp)) {
                        IgngStateBlock(
                            title = "这里还没有消息",
                            description = if (filter.isActive) "放宽筛选条件或回到最新消息流查看。" else "该会话暂时没有存档消息。",
                            actionLabel = if (filter.isActive) "清除筛选" else null,
                            onAction = if (filter.isActive) {
                                {
                                    searchHighlight = ""
                                    filter = ChatFilterState()
                                }
                            } else {
                                null
                            },
                        )
                    }
                    else -> LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 18.dp),
                    ) {
                        item(key = "historyTopHeader") {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                when {
                                    loadingOlder -> Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = colors.accent)
                                        Text("正在加载更早的消息…", style = IgngTheme.type.badge, color = colors.textMuted)
                                    }
                                    hasMore -> Row(
                                        modifier = Modifier
                                            .clip(IgngTheme.radii.pill)
                                            .background(colors.tile)
                                            .border(0.5.dp, colors.line, IgngTheme.radii.pill)
                                            .clickable { scope.launch { loadMessages(older = true) } }
                                            .padding(horizontal = 12.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    ) {
                                        Icon(Icons.Rounded.History, null, tint = colors.accent, modifier = Modifier.size(14.dp))
                                        Text("加载更早的消息", style = IgngTheme.type.badge, color = colors.accent)
                                    }
                                    else -> Text(
                                        text = "已经到最早的记录了",
                                        style = IgngTheme.type.badge,
                                        color = colors.textMuted,
                                    )
                                }
                            }
                        }

                        items(timeline, key = { it.key }) { item ->
                            when (item) {
                                is ChatTimelineItem.DayHeader -> ChatDayHeaderChip(label = item.label)
                                is ChatTimelineItem.TimeDivider -> ChatTimeDividerChip(label = item.label)
                                is ChatTimelineItem.SystemNotice -> ChatSystemNoticePill(text = item.text)
                                is ChatTimelineItem.MessageGroup -> ChatMessageGroupBlock(
                                    group = item,
                                    isPrivateChat = isPrivate,
                                    focusedMessageId = filter.focusedId.takeIf { it.isNotBlank() },
                                    highlightKeyword = searchHighlight,
                                    endpoints = endpoints,
                                    chatRepository = chatRepository,
                                    audioController = audioController,
                                    onSenderClick = { sender ->
                                        when {
                                            sender.boundIgngId != null -> onOpenPlayerPassport(sender.boundIgngId)
                                            !sender.mcUsername.isNullOrBlank() -> onOpenMcCharacter(sender.mcUsername)
                                        }
                                    },
                                    onLongPressMessage = { msg, sender ->
                                        haptic(IgngHapticKind.Tick)
                                        contextActionTarget = msg to sender
                                    },
                                    onJumpToMessage = { targetIdToFocus ->
                                        searchHighlight = ""
                                        filter = ChatFilterState(focusedId = targetIdToFocus)
                                    },
                                    onPreviewGallery = { urls, idx, titles ->
                                        lightbox = LightboxPayload(urls, idx, titles)
                                    },
                                    onOpenForward = ::openForward,
                                    onPlayVideo = { path, videoTitle ->
                                        videoPlayer = VideoPayload(path, videoTitle)
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // Floating "N new messages" pill when scrolled up during live updates
            AnimatedVisibility(
                visible = newBelowCount > 0,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 18.dp),
            ) {
                Row(
                    modifier = Modifier
                        .clip(IgngTheme.radii.pill)
                        .background(colors.accent)
                        .clickable {
                            newBelowCount = 0
                            scope.launch { scrollToMessageOrBottom(null, smooth = true) }
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(Icons.Rounded.ArrowDownward, null, tint = colors.onAccent, modifier = Modifier.size(15.dp))
                    Text(
                        text = "$newBelowCount 条新消息",
                        style = IgngTheme.type.caption.copy(fontWeight = FontWeight.Bold),
                        color = colors.onAccent,
                    )
                }
            }
        }
    }

    // ==================== Overlays & Sheets ====================

    lightbox?.let { payload ->
        IgngImageLightbox(
            imageUrls = payload.urls,
            initialIndex = payload.initialIndex,
            titles = payload.titles,
            onDismiss = { lightbox = null },
        )
    }

    videoPlayer?.let { vp ->
        ChatVideoPlayerDialog(
            mediaPath = vp.path,
            title = vp.title,
            chatRepository = chatRepository,
            onDismiss = { videoPlayer = null },
        )
    }

    if (showSearchSheet) {
        ChatSearchSheet(
            source = source,
            platform = platform,
            targetId = targetId,
            channelTitle = title.ifBlank { sessionKind.label },
            chatRepository = chatRepository,
            activeAccount = activeAccount,
            boundMcUsernames = boundMcSet,
            onSelectMessage = { msgId, kw ->
                showSearchSheet = false
                searchHighlight = kw
                filter = ChatFilterState(focusedId = msgId)
            },
            onDismiss = { showSearchSheet = false },
        )
    }

    if (showFilterSheet) {
        ChatFilterSheet(
            source = source,
            platform = platform,
            isPrivate = isPrivate,
            initial = filter,
            onApply = { next ->
                searchHighlight = ""
                filter = next
            },
            onReset = {
                searchHighlight = ""
                filter = ChatFilterState()
            },
            onDismiss = { showFilterSheet = false },
        )
    }

    val sheetPersona = persona
    if (showPersonaSheet && sheetPersona != null) {
        IgngDetailSheet(
            onDismiss = { showPersonaSheet = false },
            title = "云萤当前人设：${sheetPersona.label}",
            subtitle = "由当前会话配置决定",
        ) {
            Text(
                text = sheetPersona.description.ifBlank { "当前会话正在使用「${sheetPersona.label}」对话风格。" },
                style = IgngTheme.type.body,
                color = colors.textSecondary,
            )
        }
    }

    val activeForwardId = forwardMessageId
    if (activeForwardId != null) {
        ForwardViewerSheet(
            messageId = activeForwardId,
            loading = forwardLoading,
            detail = forwardDetail,
            error = forwardError,
            endpoints = endpoints,
            chatRepository = chatRepository,
            onRetry = {
                val targetMsg = messages.firstOrNull { it.id == activeForwardId }
                if (targetMsg != null) openForward(targetMsg)
            },
            onPreviewGallery = { urls, idx, titles ->
                lightbox = LightboxPayload(urls, idx, titles)
            },
            onDismiss = {
                forwardMessageId = null
                forwardDetail = null
                forwardError = null
            },
        )
    }

    contextActionTarget?.let { (msg, sender) ->
        IgngDetailSheet(
            onDismiss = { contextActionTarget = null },
            title = sender.displayName,
            subtitle = "#${msg.id} · ${formatFullDateTime(msg.createdAt)}",
        ) {
            if (sender.badges.isNotEmpty() || sender.relationship != null) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    sender.badges.forEach { badge ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.tileSoft)
                                .border(0.5.dp, colors.line, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(badge.label, style = IgngTheme.type.badge, color = colors.textSecondary)
                        }
                    }
                    sender.relationship?.let { rel ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(semantic.relationshipBg)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text("云萤关系：${rel.label}", style = IgngTheme.type.badge, color = semantic.relationshipFg)
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (msg.content.isNotBlank()) {
                    IgngButton(
                        text = "复制消息正文",
                        onClick = {
                            copyTextToClipboard(context, "消息正文", msg.content)
                            contextActionTarget = null
                        },
                        style = IgngButtonStyle.Secondary,
                        icon = Icons.Rounded.ContentCopy,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (!isPrivate && !sender.filterSenderId.isNullOrBlank()) {
                    IgngButton(
                        text = "只看 TA 的消息",
                        onClick = {
                            searchHighlight = ""
                            filter = filter.copy(senderId = sender.filterSenderId, focusedId = "")
                            contextActionTarget = null
                        },
                        style = IgngButtonStyle.Secondary,
                        icon = Icons.Rounded.PersonSearch,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (sender.boundIgngId != null) {
                    IgngButton(
                        text = "查看 TA 的 IGNG 名片",
                        onClick = {
                            val id = sender.boundIgngId
                            contextActionTarget = null
                            onOpenPlayerPassport(id)
                        },
                        style = IgngButtonStyle.Secondary,
                        icon = Icons.Rounded.Person,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else if (!sender.mcUsername.isNullOrBlank()) {
                    IgngButton(
                        text = "查看 MC 角色档案",
                        onClick = {
                            val name = sender.mcUsername
                            contextActionTarget = null
                            onOpenMcCharacter(name)
                        },
                        style = IgngButtonStyle.Secondary,
                        icon = Icons.Rounded.Person,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                IgngButton(
                    text = "复制网页定位链接",
                    onClick = {
                        copyTextToClipboard(context, "消息链接", "${endpoints.chat}/chatlogs/${msg.id}")
                        contextActionTarget = null
                    },
                    style = IgngButtonStyle.Ghost,
                    icon = Icons.Rounded.Link,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ActiveFilterStrip(
    filter: ChatFilterState,
    source: String,
    platform: String,
    onStepDay: (Int) -> Unit,
    onClearDay: () -> Unit,
    onClearSender: () -> Unit,
    onClearAtAll: () -> Unit,
    onClearKind: () -> Unit,
    onClearFocus: () -> Unit,
    onResetAll: () -> Unit,
) {
    val colors = IgngTheme.colors
    val todayKey = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.tileSoft)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (filter.focusedId.isNotBlank()) {
            RemovableFilterChip("定位 #${filter.focusedId}", onClearFocus)
        }
        if (filter.dayKey.isNotBlank()) {
            Row(
                modifier = Modifier
                    .clip(IgngTheme.radii.pill)
                    .background(colors.accentSoft)
                    .border(0.8.dp, colors.accent.copy(alpha = 0.4f), IgngTheme.radii.pill)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronLeft,
                    contentDescription = "前一天",
                    tint = colors.accent,
                    modifier = Modifier.size(16.dp).clickable { onStepDay(-1) },
                )
                Text(
                    text = formatShortDayLabel(filter.dayKey),
                    style = IgngTheme.type.badge,
                    color = colors.accent,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                val canNext = filter.dayKey < todayKey
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = "后一天",
                    tint = if (canNext) colors.accent else colors.textMuted,
                    modifier = Modifier.size(16.dp).then(if (canNext) Modifier.clickable { onStepDay(1) } else Modifier),
                )
                Spacer(Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "清除日期",
                    tint = colors.accent,
                    modifier = Modifier.size(14.dp).clickable(onClick = onClearDay),
                )
            }
        }
        if (filter.senderId.isNotBlank()) {
            val senderLabel = when {
                source == "server" -> "玩家 ${filter.senderId}"
                platform == "wechat" -> "微信用户"
                else -> "QQ ${filter.senderId}"
            }
            RemovableFilterChip(senderLabel, onClearSender)
        }
        if (filter.atAll) {
            RemovableFilterChip(if (platform == "wechat") "@所有人" else "@全体成员", onClearAtAll)
        }
        if (filter.kind.isNotBlank()) {
            val kindLabel = when (filter.kind) {
                "media" -> "含图片"
                "forward" -> "含转发"
                "bot" -> "云萤回复"
                else -> filter.kind
            }
            RemovableFilterChip(kindLabel, onClearKind)
        }
        Text(
            text = "清除全部",
            style = IgngTheme.type.badge,
            color = colors.textSecondary,
            modifier = Modifier
                .clip(IgngTheme.radii.pill)
                .clickable(onClick = onResetAll)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
    HorizontalDivider(thickness = 0.5.dp, color = colors.line)
}

@Composable
private fun RemovableFilterChip(label: String, onClear: () -> Unit) {
    val colors = IgngTheme.colors
    Row(
        modifier = Modifier
            .clip(IgngTheme.radii.pill)
            .background(colors.accentSoft)
            .border(0.8.dp, colors.accent.copy(alpha = 0.4f), IgngTheme.radii.pill)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = IgngTheme.type.badge, color = colors.accent)
        Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "移除筛选",
            tint = colors.accent,
            modifier = Modifier.size(13.dp).clickable(onClick = onClear),
        )
    }
}
