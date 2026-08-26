@file:OptIn(ExperimentalMaterial3Api::class)

package net.igng.mcstatus.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VerticalAlignBottom
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Calendar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.igng.mcstatus.data.AppSettings
import net.igng.mcstatus.data.ChatBootstrap
import net.igng.mcstatus.data.ChatMessage
import net.igng.mcstatus.data.ChatRepository

private const val SOURCE_SERVER = "server"
private const val SOURCE_QQ = "qq"
private const val DEFAULT_LIMIT = 100
private const val MAX_LIMIT = 200

private val LOCAL_INPUT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
private val CHAT_LOG_DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
private val CHAT_LOG_QQ_ZONE: ZoneId = ZoneId.of("Asia/Shanghai")
private val DISPLAY_DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/M/d HH:mm")
private val DISPLAY_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE")

data class ChatLogsUiState(
    val bootstrapping: Boolean = true,
    val loading: Boolean = false,
    val loadingOlder: Boolean = false,
    val bootstrap: ChatBootstrap? = null,
    val messages: List<ChatMessage> = emptyList(),
    val hasMore: Boolean = false,
    val focusedId: String? = null,
    val error: String? = null,
    val refreshError: String? = null,
    val lastUpdated: Instant? = null,
)

private data class ChatQuery(
    val source: String,
    val serverId: String?,
    val groupId: String?,
    val limit: Int,
    val start: String?,
    val end: String?,
    val senderId: String?,
    val atAll: Boolean,
    val focusId: String? = null,
)

class ChatLogsViewModel(
    private val repository: ChatRepository,
) : ViewModel() {
    private val _state = mutableStateOf(ChatLogsUiState())
    val state: State<ChatLogsUiState> = _state

    var source by mutableStateOf(SOURCE_SERVER)
        private set
    var serverId by mutableStateOf("")
        private set
    var groupId by mutableStateOf("")
        private set

    var senderDraft by mutableStateOf("")
    var limitDraft by mutableStateOf(DEFAULT_LIMIT.toString())
    var startDraft by mutableStateOf("")
    var endDraft by mutableStateOf("")
    var atAllDraft by mutableStateOf(false)
    var focusDraft by mutableStateOf("")

    private var senderId = ""
    private var limit = DEFAULT_LIMIT
    private var startLocal = ""
    private var endLocal = ""
    private var atAllOnly = false
    private var requestGeneration = 0L
    private var activeRequestJob: Job? = null
    private var olderRequestJob: Job? = null
    private var autoRefreshJob: Job? = null
    private var historyExpanded = false

    var nearBottom by mutableStateOf(true)
        private set

    /** Message id that should remain visible after older rows are prepended. */
    var scrollAnchorMessageId by mutableStateOf<String?>(null)
        private set

    val currentTargetLabel: String
        get() {
            val bootstrap = _state.value.bootstrap
                ?: return if (source == SOURCE_QQ) "选择QQ群" else "选择服务器"
            return if (source == SOURCE_QQ) {
                bootstrap.qqGroups.firstOrNull { it.id == groupId }?.name
                    ?: groupId.takeIf { it.isNotBlank() }
                    ?: "选择QQ群"
            } else {
                bootstrap.servers.firstOrNull { it.id.toString() == serverId }?.name
                    ?: "选择服务器"
            }
        }

    val sourceLabel: String
        get() = if (source == SOURCE_QQ) "QQ群消息" else "服务器消息"

    val activeFilterCount: Int
        get() = listOf(
            senderId.isNotBlank() || senderDraft.isNotBlank(),
            startLocal.isNotBlank() || startDraft.isNotBlank(),
            endLocal.isNotBlank() || endDraft.isNotBlank(),
            atAllOnly || atAllDraft,
            limit != DEFAULT_LIMIT || limitDraft.toIntOrNull() != DEFAULT_LIMIT,
            !_state.value.focusedId.isNullOrBlank() || focusDraft.isNotBlank(),
        ).count { it }

    val hasAdvancedFilters: Boolean
        get() = activeFilterCount > 0

    init {
        bootstrap()
    }

    fun retryBootstrap() = bootstrap()

    private fun bootstrap() {
        invalidateRequests()
        viewModelScope.launch {
            _state.value = _state.value.copy(bootstrapping = true, error = null)
            runCatching { repository.bootstrap() }
                .onSuccess { data ->
                    serverId = data.servers.firstOrNull()?.id?.toString().orEmpty()
                    groupId = data.qqGroups.firstOrNull()?.id.orEmpty()
                    _state.value = _state.value.copy(
                        bootstrapping = false,
                        bootstrap = data,
                        error = null,
                    )
                    refresh()
                    restartAutoRefresh()
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        bootstrapping = false,
                        error = error.message ?: "初始化聊天记录失败",
                    )
                }
        }
    }

    fun updateNearBottom(value: Boolean) {
        nearBottom = value
    }

    fun consumeScrollAnchor() {
        scrollAnchorMessageId = null
    }

    fun selectSource(value: String) {
        if (value !in listOf(SOURCE_SERVER, SOURCE_QQ) || source == value) return
        source = value
        clearIncompatibleFilters()
        ensureSelectedTarget()
        clearMessages()
        refresh()
        restartAutoRefresh()
    }

    fun selectServerId(value: String) {
        if (serverId == value) return
        serverId = value
        clearMessages(clearFilters = false)
        refresh()
        restartAutoRefresh()
    }

    fun selectGroupId(value: String) {
        if (groupId == value) return
        groupId = value
        clearMessages(clearFilters = false)
        refresh()
        restartAutoRefresh()
    }

    fun applyFilters(): Boolean {
        val parsedLimit = limitDraft.trim().toIntOrNull()
        if (parsedLimit == null || parsedLimit !in 1..MAX_LIMIT) {
            setError("消息数量必须是 1-$MAX_LIMIT 之间的整数")
            return false
        }

        val nextStart = startDraft.trim()
        val nextEnd = endDraft.trim()
        val startApi = parseLocalToIso(nextStart, source)
        val endApi = parseLocalToIso(nextEnd, source)
        if (nextStart.isNotBlank() && startApi == null) {
            setError("开始时间格式无效，请使用日期选择器")
            return false
        }
        if (nextEnd.isNotBlank() && endApi == null) {
            setError("结束时间格式无效，请使用日期选择器")
            return false
        }
        if (startApi != null && endApi != null && Instant.parse(startApi) > Instant.parse(endApi)) {
            setError("开始时间不能晚于结束时间")
            return false
        }

        senderId = senderDraft.trim()
        limit = parsedLimit
        startLocal = nextStart
        endLocal = nextEnd
        atAllOnly = source == SOURCE_QQ && atAllDraft
        _state.value = _state.value.copy(focusedId = null, error = null, refreshError = null)
        focusDraft = ""
        clearMessages(clearFilters = false)
        refresh()
        restartAutoRefresh()
        return true
    }

    fun resetToLatest() {
        senderDraft = ""
        senderId = ""
        limitDraft = DEFAULT_LIMIT.toString()
        limit = DEFAULT_LIMIT
        startDraft = ""
        startLocal = ""
        endDraft = ""
        endLocal = ""
        atAllDraft = false
        atAllOnly = false
        focusDraft = ""
        _state.value = _state.value.copy(focusedId = null, error = null, refreshError = null)
        clearMessages(clearFilters = false)
        refresh()
        restartAutoRefresh()
    }

    fun focusMessage(): Boolean {
        val target = focusDraft.trim()
        if (target.isBlank()) {
            setError("请输入消息 ID")
            return false
        }
        val inferredSource = when {
            Regex("^qq-(?:v-)?\\d+$").matches(target) -> SOURCE_QQ
            Regex("^\\d+$").matches(target) -> SOURCE_SERVER
            else -> ""
        }
        if (inferredSource.isBlank()) {
            setError("消息 ID 格式无效")
            return false
        }

        if (source != inferredSource) {
            source = inferredSource
            clearIncompatibleFilters()
            ensureSelectedTarget()
        }
        if (source == SOURCE_SERVER && serverId.isBlank()) {
            setError("当前没有可用服务器")
            return false
        }
        if (source == SOURCE_QQ && groupId.isBlank()) {
            setError("当前没有可用审核群")
            return false
        }

        invalidateRequests()
        clearMessages(clearFilters = false)
        _state.value = _state.value.copy(focusedId = target, error = null, refreshError = null)
        refresh(focus = target)
        restartAutoRefresh()
        return true
    }

    fun refresh(silent: Boolean = false, focus: String? = _state.value.focusedId) {
        if (silent && !canAutoRefresh()) return
        val query = buildQuery(focus) ?: return
        val generation = ++requestGeneration
        activeRequestJob?.cancel()
        if (!silent) {
            olderRequestJob?.cancel()
            historyExpanded = false
            _state.value = _state.value.copy(loading = true, error = null, refreshError = null)
        } else {
            _state.value = _state.value.copy(refreshError = null)
        }

        activeRequestJob = viewModelScope.launch {
            try {
                val response = repository.chatlogs(
                    source = query.source,
                    serverId = query.serverId,
                    groupId = query.groupId,
                    limit = query.limit,
                    start = query.start,
                    end = query.end,
                    senderId = query.senderId,
                    atAll = query.atAll,
                    messageId = query.focusId,
                )
                if (generation != requestGeneration) return@launch

                val focused = response.messages.firstOrNull { it.id == (response.focusedId ?: query.focusId) }
                if (focused?.source == SOURCE_QQ && !focused.group_id.isNullOrBlank()) {
                    groupId = focused.group_id
                    source = SOURCE_QQ
                } else if (focused?.server_id != null) {
                    serverId = focused.server_id.toString()
                    source = SOURCE_SERVER
                }
                _state.value = _state.value.copy(
                    loading = false,
                    messages = response.messages,
                    hasMore = response.hasMore,
                    focusedId = response.focusedId ?: query.focusId,
                    error = null,
                    refreshError = null,
                    lastUpdated = Instant.now(),
                )
                nearBottom = true
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (generation != requestGeneration) return@launch
                _state.value = if (silent) {
                    _state.value.copy(loading = false, refreshError = error.message ?: "自动刷新失败")
                } else {
                    _state.value.copy(loading = false, error = error.message ?: "加载聊天记录失败")
                }
            }
        }
    }

    fun loadOlder() {
        if (_state.value.loadingOlder || _state.value.loading) return
        if (historyExpanded && !_state.value.hasMore) return
        val firstId = _state.value.messages.firstOrNull()?.id ?: return
        val query = buildQuery(focus = null) ?: return
        scrollAnchorMessageId = firstId
        val generation = ++requestGeneration
        activeRequestJob?.cancel()
        olderRequestJob?.cancel()
        _state.value = _state.value.copy(loadingOlder = true, error = null)

        olderRequestJob = viewModelScope.launch {
            try {
                val response = repository.chatlogs(
                    source = query.source,
                    serverId = query.serverId,
                    groupId = query.groupId,
                    limit = query.limit,
                    start = query.start,
                    end = query.end,
                    senderId = query.senderId,
                    atAll = query.atAll,
                    beforeId = firstId,
                )
                if (generation != requestGeneration) return@launch
                if (response.messages.isEmpty()) {
                    historyExpanded = true
                    scrollAnchorMessageId = null
                    _state.value = _state.value.copy(loadingOlder = false, hasMore = false)
                    return@launch
                }
                val merged = (response.messages + _state.value.messages).distinctBy { it.id }
                historyExpanded = true
                _state.value = _state.value.copy(
                    loadingOlder = false,
                    messages = merged,
                    hasMore = response.hasMore,
                    lastUpdated = Instant.now(),
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (generation != requestGeneration) return@launch
                scrollAnchorMessageId = null
                _state.value = _state.value.copy(
                    loadingOlder = false,
                    error = error.message ?: "加载历史消息失败",
                )
            }
        }
    }

    private fun buildQuery(focus: String?): ChatQuery? {
        val target = if (source == SOURCE_QQ) groupId else serverId
        if (target.isBlank() && focus.isNullOrBlank()) return null
        val startApi = parseLocalToIso(startLocal, source)
        val endApi = parseLocalToIso(endLocal, source)
        if (startLocal.isNotBlank() && startApi == null) {
            setError("开始时间格式无效，请使用日期选择器")
            return null
        }
        if (endLocal.isNotBlank() && endApi == null) {
            setError("结束时间格式无效，请使用日期选择器")
            return null
        }
        if (startApi != null && endApi != null && Instant.parse(startApi) > Instant.parse(endApi)) {
            setError("开始时间不能晚于结束时间")
            return null
        }
        return ChatQuery(
            source = source,
            serverId = serverId.takeIf { source == SOURCE_SERVER },
            groupId = groupId.takeIf { source == SOURCE_QQ },
            limit = limit,
            start = startApi,
            end = endApi,
            senderId = senderId.takeIf { it.isNotBlank() },
            atAll = source == SOURCE_QQ && atAllOnly,
            focusId = focus?.trim()?.takeIf { it.isNotBlank() },
        )
    }

    private fun clearMessages(clearFilters: Boolean = true) {
        invalidateRequests()
        historyExpanded = false
        scrollAnchorMessageId = null
        if (clearFilters) clearIncompatibleFilters()
        _state.value = _state.value.copy(
            messages = emptyList(),
            hasMore = false,
            focusedId = null,
            error = null,
            refreshError = null,
            lastUpdated = null,
        )
        nearBottom = true
    }

    private fun clearIncompatibleFilters() {
        senderDraft = ""
        senderId = ""
        startDraft = ""
        startLocal = ""
        endDraft = ""
        endLocal = ""
        atAllDraft = false
        atAllOnly = false
        focusDraft = ""
    }

    private fun ensureSelectedTarget() {
        val bootstrap = _state.value.bootstrap ?: return
        if (source == SOURCE_QQ && groupId.isBlank()) {
            groupId = bootstrap.qqGroups.firstOrNull()?.id.orEmpty()
        } else if (source == SOURCE_SERVER && serverId.isBlank()) {
            serverId = bootstrap.servers.firstOrNull()?.id?.toString().orEmpty()
        }
    }

    private fun setError(message: String) {
        _state.value = _state.value.copy(error = message, refreshError = null)
    }

    private fun canAutoRefresh(): Boolean =
        startLocal.isBlank() &&
            endLocal.isBlank() &&
            senderId.isBlank() &&
            !atAllOnly &&
            _state.value.focusedId.isNullOrBlank() &&
            !historyExpanded &&
            nearBottom &&
            !_state.value.loadingOlder &&
            !_state.value.loading

    private fun restartAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(10_000)
                if (canAutoRefresh()) refresh(silent = true)
            }
        }
    }

    private fun invalidateRequests() {
        requestGeneration += 1
        activeRequestJob?.cancel()
        olderRequestJob?.cancel()
    }

    private fun parseLocalToIso(value: String, querySource: String): String? {
        val text = value.trim()
        if (text.isEmpty()) return null
        return runCatching {
            if (text.endsWith("Z", ignoreCase = true) || text.contains('+') || text.count { it == '-' } >= 3) {
                Instant.parse(text).toString()
            } else {
                val local = LocalDateTime.parse(text, LOCAL_INPUT)
                if (querySource == SOURCE_QQ) {
                    val shanghaiWall = local
                        .atZone(ZoneId.systemDefault())
                        .withZoneSameInstant(CHAT_LOG_QQ_ZONE)
                        .toLocalDateTime()
                    shanghaiWall.format(CHAT_LOG_DATE_TIME) + "Z"
                } else {
                    local.atZone(ZoneId.systemDefault()).toInstant().toString()
                }
            }
        }.getOrNull()
    }

    override fun onCleared() {
        autoRefreshJob?.cancel()
        invalidateRequests()
        super.onCleared()
    }
}

class ChatLogsViewModelFactory(
    private val repository: ChatRepository,
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ChatLogsViewModel(repository) as T
    }
}

@Composable
fun ChatLogsScreen(
    repository: ChatRepository,
    settings: AppSettings,
) {
    val vm: ChatLogsViewModel = viewModel(
        key = "chatlogs",
        factory = remember(repository) { ChatLogsViewModelFactory(repository) },
    )
    val state by vm.state
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    var showFilterSheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollScope = rememberCoroutineScope()
    var initialScrollDone by remember { mutableStateOf(false) }

    LaunchedEffect(state.messages.isEmpty(), vm.source, vm.serverId, vm.groupId, state.loading) {
        if (state.messages.isEmpty() || state.loading) initialScrollDone = false
    }

    LaunchedEffect(
        state.messages,
        state.focusedId,
        state.loading,
        state.loadingOlder,
        state.hasMore,
        state.error,
        vm.scrollAnchorMessageId,
    ) {
        if (state.messages.isEmpty()) return@LaunchedEffect
        val showOlderHeader = state.hasMore || state.loadingOlder
        val headerCount = (if (state.error != null) 1 else 0) + (if (showOlderHeader) 1 else 0)
        val anchorId = vm.scrollAnchorMessageId
        if (!state.loadingOlder && anchorId != null) {
            val anchorIndex = state.messages.indexOfFirst { it.id == anchorId }
            if (anchorIndex >= 0) listState.scrollToItem(anchorIndex + headerCount)
            vm.consumeScrollAnchor()
            initialScrollDone = true
            return@LaunchedEffect
        }
        if (state.loadingOlder || state.loading) return@LaunchedEffect

        val focusIndex = state.focusedId?.let { id -> state.messages.indexOfFirst { it.id == id } } ?: -1
        if (focusIndex >= 0) {
            listState.animateScrollToItem(focusIndex + headerCount)
            initialScrollDone = true
            return@LaunchedEffect
        }
        if (!initialScrollDone) {
            listState.scrollToItem(state.messages.lastIndex + headerCount)
            initialScrollDone = true
        } else if (vm.nearBottom && state.focusedId.isNullOrBlank()) {
            listState.animateScrollToItem(state.messages.lastIndex + headerCount)
        }
    }

    LaunchedEffect(
        listState,
        state.hasMore,
        state.loadingOlder,
        state.loading,
        state.messages.size,
        initialScrollDone,
    ) {
        if (!initialScrollDone) return@LaunchedEffect
        snapshotFlow {
            val layout = listState.layoutInfo
            val lastVisible = layout.visibleItemsInfo.lastOrNull()?.index ?: -1
            Triple(listState.firstVisibleItemIndex, lastVisible, layout.totalItemsCount)
        }.distinctUntilChanged().collect { (firstVisible, lastVisible, totalItems) ->
            vm.updateNearBottom(totalItems == 0 || lastVisible >= totalItems - 2)
            if (
                firstVisible <= 1 &&
                state.hasMore &&
                !state.loadingOlder &&
                !state.loading &&
                state.messages.isNotEmpty()
            ) {
                vm.loadOlder()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("聊天消息")
                        Text(
                            text = "${vm.sourceLabel} · ${vm.currentTargetLabel}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            buzzChat(haptic, settings)
                            vm.refresh()
                        },
                        enabled = !state.loading && !state.loadingOlder,
                    ) {
                        if (state.loading) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Rounded.Refresh, contentDescription = "刷新聊天消息")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (state.bootstrap != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!vm.nearBottom && state.messages.isNotEmpty()) {
                        SmallFloatingActionButton(
                            onClick = {
                                buzzChat(haptic, settings)
                                scrollScope.launch {
                                    val lastItem = (listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
                                    listState.animateScrollToItem(lastItem)
                                    vm.updateNearBottom(true)
                                }
                            },
                        ) {
                            Icon(Icons.Rounded.VerticalAlignBottom, contentDescription = "回到最新")
                        }
                    }
                    ExtendedFloatingActionButton(
                        onClick = {
                            buzzChat(haptic, settings)
                            showFilterSheet = true
                        },
                        icon = {
                            BadgedBox(badge = { if (vm.hasAdvancedFilters) Badge() }) {
                                Icon(Icons.Rounded.FilterList, contentDescription = null)
                            }
                        },
                        text = { Text("频道与筛选") },
                    )
                }
            }
        },
    ) { padding ->
        when {
            state.bootstrapping -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            state.bootstrap == null -> {
                ErrorState(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    message = state.error ?: "聊天频道初始化失败",
                    onRetry = {
                        buzzChat(haptic, settings)
                        vm.retryBootstrap()
                    },
                )
            }

            else -> {
                ChatContent(
                    state = state,
                    vm = vm,
                    settings = settings,
                    haptic = haptic,
                    context = context,
                    listState = listState,
                    padding = padding,
                    onOpenFilters = { showFilterSheet = true },
                )
            }
        }
    }

    if (showFilterSheet && state.bootstrap != null) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = filterSheetState,
        ) {
            ChatFilterSheet(
                vm = vm,
                bootstrap = state.bootstrap!!,
                settings = settings,
                haptic = haptic,
                error = state.error,
                onApply = { if (vm.applyFilters()) showFilterSheet = false },
                onFocus = { if (vm.focusMessage()) showFilterSheet = false },
                onReset = {
                    vm.resetToLatest()
                    showFilterSheet = false
                },
                onClose = { showFilterSheet = false },
            )
        }
    }
}

@Composable
private fun ChatContent(
    state: ChatLogsUiState,
    vm: ChatLogsViewModel,
    settings: AppSettings,
    haptic: HapticFeedback,
    context: Context,
    listState: androidx.compose.foundation.lazy.LazyListState,
    padding: PaddingValues,
    onOpenFilters: () -> Unit,
) {
    val sourceLabel = vm.sourceLabel
    val targetLabel = vm.currentTargetLabel
    val hasChannel = if (vm.source == SOURCE_QQ) {
        state.bootstrap?.qqGroups?.isNotEmpty() == true
    } else {
        state.bootstrap?.servers?.isNotEmpty() == true
    }

    Box(Modifier.fillMaxSize().padding(padding)) {
        when {
            !hasChannel -> {
                ErrorState(
                    modifier = Modifier.fillMaxSize(),
                    message = "当前没有可用的${if (vm.source == SOURCE_QQ) "审核 QQ 群" else "服务器"}频道",
                    onRetry = onOpenFilters,
                    retryLabel = "打开频道选择",
                )
            }

            state.loading && state.messages.isEmpty() -> {
                LoadingState()
            }

            state.messages.isEmpty() -> {
                EmptyChatState(
                    error = state.error,
                    onRetry = { vm.refresh() },
                    onOpenFilters = onOpenFilters,
                    hasFilters = vm.hasAdvancedFilters,
                )
            }

            else -> {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 112.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item(key = "summary") {
                        ChatWindowSummary(
                            sourceLabel = sourceLabel,
                            targetLabel = targetLabel,
                            messageCount = state.messages.size,
                            hasMore = state.hasMore,
                            historyExpanded = state.focusedId.isNullOrBlank() && !vm.nearBottom,
                            lastUpdated = state.lastUpdated,
                            filterCount = vm.activeFilterCount,
                        )
                    }
                    state.refreshError?.let { message ->
                        item(key = "refresh-error") {
                            InlineMessage(
                                message = message,
                                isError = false,
                                onRetry = { vm.refresh(silent = true) },
                            )
                        }
                    }
                    state.error?.let { message ->
                        item(key = "error") {
                            InlineMessage(message = message, isError = true, onRetry = { vm.refresh() })
                        }
                    }
                    if (state.hasMore || state.loadingOlder) {
                        item(key = "older") {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                TextButton(
                                    onClick = {
                                        buzzChat(haptic, settings)
                                        vm.loadOlder()
                                    },
                                    enabled = !state.loadingOlder && !state.loading,
                                ) {
                                    if (state.loadingOlder) {
                                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                        Spacer(Modifier.width(8.dp))
                                        Text("正在加载更早消息…")
                                    } else {
                                        Text("查看更早消息")
                                    }
                                }
                            }
                        }
                    }
                    items(state.messages, key = { it.id }) { message ->
                        val index = state.messages.indexOf(message)
                        val previous = state.messages.getOrNull(index - 1)
                        val currentDate = message.sent_at.toDateKey(message.source)
                        val previousDate = previous?.sent_at?.toDateKey(previous.source)
                        if (currentDate != previousDate) {
                            ChatDateDivider(message.sent_at, message.source)
                        }
                        ChatMessageCard(
                            message = message,
                            focused = message.id == state.focusedId,
                            onFocus = {
                                buzzChat(haptic, settings)
                                vm.focusDraft = message.id
                                vm.focusMessage()
                            },
                            onCopyId = {
                                buzzChat(haptic, settings)
                                copyText(context, message.id)
                            },
                            onCopyContent = {
                                buzzChat(haptic, settings)
                                copyText(context, message.content)
                            },
                        )
                    }
                }
            }
        }
        if (state.loading && state.messages.isNotEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp),
                strokeWidth = 2.dp,
            )
        }
    }
}

@Composable
private fun ChatWindowSummary(
    sourceLabel: String,
    targetLabel: String,
    messageCount: Int,
    hasMore: Boolean,
    historyExpanded: Boolean,
    lastUpdated: Instant?,
    filterCount: Int,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(sourceLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(targetLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Text("$messageCount 条", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = when {
                    historyExpanded || hasMore -> "历史模式 · 向上滑动加载更早消息"
                    filterCount > 0 -> "已启用 $filterCount 项筛选"
                    else -> "最新消息模式 · 自动刷新已开启"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            lastUpdated?.let {
                Text(
                    text = "最近更新 ${it.toDisplayTime()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ChatDateDivider(value: String, source: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
        Text(
            text = value.toDateLabel(source),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
    }
}

@Composable
private fun ChatMessageCard(
    message: ChatMessage,
    focused: Boolean,
    onFocus: () -> Unit,
    onCopyId: () -> Unit,
    onCopyContent: () -> Unit,
) {
    val container = when {
        focused -> MaterialTheme.colorScheme.primaryContainer
        message.moderation != null -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.48f)
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = if (message.source == SOURCE_QQ) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                ) {
                    Text(
                        text = if (message.source == SOURCE_QQ) "群" else "服",
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = message.player_name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = message.sent_at.toDisplayTimeDetailed(message.source),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (message.is_at_all) ChatTag("@全体成员", MaterialTheme.colorScheme.tertiaryContainer)
                message.moderation?.let { ChatTag(it.label, MaterialTheme.colorScheme.errorContainer) }
            }

            androidx.compose.foundation.text.selection.SelectionContainer {
                Text(
                    text = message.content,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.15f,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onFocus, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                    Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("定位 #${message.id}")
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onCopyContent) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = "复制消息内容", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onCopyId) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = "复制消息 ID", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun ChatTag(text: String, color: androidx.compose.ui.graphics.Color) {
    Surface(shape = RoundedCornerShape(50), color = color) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ChatFilterSheet(
    vm: ChatLogsViewModel,
    bootstrap: ChatBootstrap,
    settings: AppSettings,
    haptic: HapticFeedback,
    error: String?,
    onApply: () -> Unit,
    onFocus: () -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    var channelMenuExpanded by remember(vm.source) { mutableStateOf(false) }
    val channels = if (vm.source == SOURCE_QQ) bootstrap.qqGroups else bootstrap.servers

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f)
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("频道与筛选", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "先选择频道，再应用筛选或定位消息",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onClose) { Text("×", style = MaterialTheme.typography.headlineSmall) }
        }
        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("消息来源", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = vm.source == SOURCE_SERVER,
                    onClick = { buzzChat(haptic, settings); vm.selectSource(SOURCE_SERVER) },
                    label = { Text("服务器") },
                )
                FilterChip(
                    selected = vm.source == SOURCE_QQ,
                    onClick = { buzzChat(haptic, settings); vm.selectSource(SOURCE_QQ) },
                    label = { Text("审核 QQ 群") },
                )
            }

            Text(if (vm.source == SOURCE_QQ) "审核 QQ 群" else "服务器", style = MaterialTheme.typography.labelLarge)
            Box {
                OutlinedButton(
                    onClick = { channelMenuExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = channels.isNotEmpty(),
                ) {
                    Text(vm.currentTargetLabel, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "展开频道列表")
                }
                DropdownMenu(
                    expanded = channelMenuExpanded,
                    onDismissRequest = { channelMenuExpanded = false },
                ) {
                    channels.forEach { channel ->
                        DropdownMenuItem(
                            text = {
                                val label = if (vm.source == SOURCE_QQ) {
                                    val qqChannel = channel as net.igng.mcstatus.data.ChatQqGroupOption
                                    qqChannel.count?.let { "${qqChannel.name} · $it 条" } ?: qqChannel.name
                                } else {
                                    (channel as net.igng.mcstatus.data.ChatServerOption).name
                                }
                                Text(label)
                            },
                            onClick = {
                                buzzChat(haptic, settings)
                                if (vm.source == SOURCE_QQ) {
                                    vm.selectGroupId((channel as net.igng.mcstatus.data.ChatQqGroupOption).id)
                                } else {
                                    vm.selectServerId((channel as net.igng.mcstatus.data.ChatServerOption).id.toString())
                                }
                                channelMenuExpanded = false
                            },
                        )
                    }
                }
            }

            Text("筛选条件", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = vm.senderDraft,
                onValueChange = {
                    vm.senderDraft = if (vm.source == SOURCE_QQ) {
                        it.filter(Char::isDigit).take(32)
                    } else {
                        it
                    }
                },
                label = { Text(if (vm.source == SOURCE_QQ) "用户 QQ 号" else "玩家名") },
                placeholder = { Text(if (vm.source == SOURCE_QQ) "输入数字 QQ 号" else "输入玩家名") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (vm.source == SOURCE_QQ) KeyboardType.Number else KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
            )
            OutlinedTextField(
                value = vm.limitDraft,
                onValueChange = { vm.limitDraft = it.filter(Char::isDigit).take(3) },
                label = { Text("消息数量（1-$MAX_LIMIT）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboard?.hide(); onApply() }),
            )
            if (vm.source == SOURCE_QQ) {
                Row(
                    modifier = Modifier.fillMaxWidth().selectable(
                        selected = vm.atAllDraft,
                        onClick = { vm.atAllDraft = !vm.atAllDraft },
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = vm.atAllDraft,
                        onCheckedChange = { vm.atAllDraft = it },
                    )
                    Text("只看 @全体成员 消息")
                }
            }

            ChatDateTimeField(
                label = "开始时间",
                value = vm.startDraft,
                onValueChange = { vm.startDraft = it },
                context = context,
            )
            ChatDateTimeField(
                label = "结束时间",
                value = vm.endDraft,
                onValueChange = { vm.endDraft = it },
                context = context,
            )
            if (vm.startDraft.isNotBlank() || vm.endDraft.isNotBlank()) {
                TextButton(
                    onClick = {
                        vm.startDraft = ""
                        vm.endDraft = ""
                    },
                ) { Text("清除时间范围") }
            }

            Text("消息定位", style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = vm.focusDraft,
                    onValueChange = { vm.focusDraft = it },
                    label = { Text("消息 ID") },
                    placeholder = { Text("服务器数字 ID，或 qq-37000 / qq-v-12") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); onFocus() }),
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = { keyboard?.hide(); onFocus() }, enabled = vm.focusDraft.isNotBlank()) {
                    Icon(Icons.Rounded.Search, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("定位")
                }
            }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) { Text("回到最新") }
            Button(onClick = { keyboard?.hide(); onApply() }, modifier = Modifier.weight(1f)) { Text("应用筛选") }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ChatDateTimeField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    context: Context,
) {
    OutlinedTextField(
        value = value.toDisplayLocalInput(),
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        placeholder = { Text("未设置，点击右侧日历选择") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        trailingIcon = {
            IconButton(onClick = { showDateTimePicker(context, value, onValueChange) }) {
                Icon(Icons.Rounded.CalendarMonth, contentDescription = "选择$label")
            }
        },
    )
}

private fun showDateTimePicker(context: Context, current: String, onChanged: (String) -> Unit) {
    val calendar = Calendar.getInstance()
    runCatching {
        LocalDateTime.parse(current, LOCAL_INPUT).let {
            calendar.set(it.year, it.monthValue - 1, it.dayOfMonth, it.hour, it.minute, 0)
        }
    }
    DatePickerDialog(
        context,
        { _, year, month, day ->
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    onChanged("%04d-%02d-%02dT%02d:%02d".format(year, month + 1, day, hour, minute))
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true,
            ).show()
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH),
    ).show()
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator()
            Text("正在加载聊天消息…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyChatState(
    error: String?,
    onRetry: () -> Unit,
    onOpenFilters: () -> Unit,
    hasFilters: Boolean,
) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                error ?: "这个频道还没有聊天记录",
                color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                if (hasFilters) "可以回到最新消息，或调整筛选条件。" else "可以切换频道或稍后刷新。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpenFilters) { Text(if (hasFilters) "调整筛选" else "选择频道") }
                Button(onClick = onRetry) { Text("刷新") }
            }
        }
    }
}

@Composable
private fun ErrorState(
    modifier: Modifier = Modifier,
    message: String,
    onRetry: () -> Unit,
    retryLabel: String = "重试",
) {
    Box(modifier.padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
            Button(onClick = onRetry) { Text(retryLabel) }
        }
    }
}

@Composable
private fun InlineMessage(message: String, isError: Boolean, onRetry: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onRetry) { Text("重试") }
        }
    }
}

private fun copyText(context: Context, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("IGNG 聊天消息", value))
}

private fun buzzChat(haptic: HapticFeedback, settings: AppSettings) {
    if (settings.vibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
}

private fun String.toDisplayLocalInput(): String {
    if (isBlank()) return ""
    return runCatching { LocalDateTime.parse(this, LOCAL_INPUT).format(DISPLAY_DATE_TIME) }.getOrDefault(this)
}

private fun String.toDateKey(source: String): String = runCatching {
    parseChatLogInstant(this, source).atZone(ZoneId.systemDefault()).toLocalDate().toString()
}.getOrDefault(this)

private fun String.toDateLabel(source: String): String = runCatching {
    DISPLAY_DATE.format(parseChatLogInstant(this, source).atZone(ZoneId.systemDefault()))
}.getOrDefault("时间未知")

private fun String.toDisplayTimeDetailed(source: String? = null): String = runCatching {
    DateTimeFormatter.ofPattern("yyyy/M/d HH:mm:ss")
        .withZone(ZoneId.systemDefault())
        .format(parseChatLogInstant(this, source))
}.getOrDefault(if (isBlank()) "时间未知" else this)

private fun Instant.toDisplayTime(): String =
    DISPLAY_DATE_TIME.withZone(ZoneId.systemDefault()).format(this)

private fun parseChatLogInstant(raw: String, source: String?): Instant {
    val text = raw.trim()
    require(text.isNotEmpty()) { "empty timestamp" }
    if (
        text.endsWith("Z", ignoreCase = true).not() &&
        text.length >= 6 &&
        (text[text.length - 6] == '+' || text[text.length - 6] == '-') &&
        text[text.length - 3] == ':'
    ) {
        return Instant.parse(text)
    }
    val body = text.removeSuffix("Z").removeSuffix("z").replace(' ', 'T').let { if (it.length > 19) it.substring(0, 19) else it }
    val local = LocalDateTime.parse(body, CHAT_LOG_DATE_TIME)
    val zone = if (source.equals(SOURCE_QQ, ignoreCase = true)) CHAT_LOG_QQ_ZONE else ZoneOffset.UTC
    return local.atZone(zone).toInstant()
}
