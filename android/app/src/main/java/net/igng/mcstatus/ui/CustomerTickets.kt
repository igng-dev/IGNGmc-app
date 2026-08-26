@file:OptIn(ExperimentalMaterial3Api::class)

package net.igng.mcstatus.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.igng.mcstatus.data.ApiException
import net.igng.mcstatus.data.AppSettings
import net.igng.mcstatus.data.CreateTicketRequest
import net.igng.mcstatus.data.CustomerTicketDetail
import net.igng.mcstatus.data.CustomerTicketRepository
import net.igng.mcstatus.data.CustomerTicketSummary
import net.igng.mcstatus.data.TicketAuthorLabel
import net.igng.mcstatus.data.TicketMessage
import net.igng.mcstatus.data.TicketOptions
import net.igng.mcstatus.data.TicketPlayerOption
import net.igng.mcstatus.data.UpdateTicketRequest
import net.igng.mcstatus.data.toDisplayTime

data class CustomerTicketListState(
    val loading: Boolean = true,
    val data: net.igng.mcstatus.data.TicketListResponse? = null,
    val error: String? = null,
)

data class CustomerTicketOptionsState(
    val loading: Boolean = false,
    val data: TicketOptions? = null,
    val error: String? = null,
)

data class CustomerTicketDetailState(
    val loading: Boolean = true,
    val ticketId: Int? = null,
    val ticket: CustomerTicketDetail? = null,
    val error: String? = null,
)

data class TicketPlayerSearchState(
    val loading: Boolean = false,
    val players: List<TicketPlayerOption> = emptyList(),
    val error: String? = null,
)

class CustomerTicketsViewModel(
    private val repository: CustomerTicketRepository,
    private val token: String,
) : ViewModel() {
    private val _listState = mutableStateOf(CustomerTicketListState())
    val listState: State<CustomerTicketListState> = _listState

    private val _optionsState = mutableStateOf(CustomerTicketOptionsState())
    val optionsState: State<CustomerTicketOptionsState> = _optionsState

    private val _detailState = mutableStateOf(CustomerTicketDetailState())
    val detailState: State<CustomerTicketDetailState> = _detailState

    private val _playerSearchState = mutableStateOf(TicketPlayerSearchState())
    val playerSearchState: State<TicketPlayerSearchState> = _playerSearchState

    private val _actionPending = mutableStateOf(false)
    val actionPending: State<Boolean> = _actionPending

    var relation by mutableStateOf("all")
        private set
    var includeClosed by mutableStateOf(false)
        private set
    var query by mutableStateOf("")
        private set
    var page by mutableStateOf(1)
        private set

    private var listGeneration = 0
    private var detailGeneration = 0
    private var playerSearchGeneration = 0

    init {
        refresh()
    }

    fun changeRelation(value: String) {
        if (relation == value) return
        relation = value
        refresh(resetPage = true)
    }

    fun changeIncludeClosed(value: Boolean) {
        if (includeClosed == value) return
        includeClosed = value
        refresh(resetPage = true)
    }

    fun submitQuery(value: String) {
        val next = value.trim()
        if (query == next) return
        query = next
        refresh(resetPage = true)
    }

    fun goToPage(value: Int) {
        val totalPages = _listState.value.data?.totalPages ?: 1
        val next = value.coerceIn(1, totalPages)
        if (page == next) return
        page = next
        refresh()
    }

    fun refresh(resetPage: Boolean = false) {
        if (resetPage) page = 1
        val generation = ++listGeneration
        viewModelScope.launch {
            _listState.value = _listState.value.copy(loading = true, error = null)
            runCatching {
                repository.list(token, relation, includeClosed, query, page)
            }.onSuccess { data ->
                if (generation == listGeneration) {
                    _listState.value = CustomerTicketListState(loading = false, data = data)
                }
            }.onFailure { error ->
                if (generation == listGeneration) {
                    _listState.value = _listState.value.copy(
                        loading = false,
                        error = ticketErrorMessage(error),
                    )
                }
            }
        }
    }

    fun loadOptions(force: Boolean = false) {
        if (!force && (_optionsState.value.loading || _optionsState.value.data != null)) return
        viewModelScope.launch {
            _optionsState.value = CustomerTicketOptionsState(loading = true)
            runCatching { repository.options(token) }
                .onSuccess { _optionsState.value = CustomerTicketOptionsState(data = it) }
                .onFailure { _optionsState.value = CustomerTicketOptionsState(error = ticketErrorMessage(it)) }
        }
    }

    fun searchPlayers(query: String) {
        val input = query.trim()
        val generation = ++playerSearchGeneration
        if (input.length < 2) {
            _playerSearchState.value = TicketPlayerSearchState()
            return
        }
        viewModelScope.launch {
            delay(260)
            if (generation != playerSearchGeneration) return@launch
            _playerSearchState.value = TicketPlayerSearchState(loading = true)
            runCatching { repository.searchPlayers(token, input) }
                .onSuccess {
                    if (generation == playerSearchGeneration) {
                        _playerSearchState.value = TicketPlayerSearchState(players = it)
                    }
                }
                .onFailure {
                    if (generation == playerSearchGeneration) {
                        _playerSearchState.value = TicketPlayerSearchState(error = ticketErrorMessage(it))
                    }
                }
        }
    }

    fun clearPlayerSearch() {
        playerSearchGeneration++
        _playerSearchState.value = TicketPlayerSearchState()
    }

    fun loadDetail(id: Int) {
        val generation = ++detailGeneration
        _detailState.value = CustomerTicketDetailState(loading = true, ticketId = id)
        viewModelScope.launch {
            runCatching { repository.detail(token, id) }
                .onSuccess { ticket ->
                    if (generation == detailGeneration) {
                        _detailState.value = CustomerTicketDetailState(ticketId = id, ticket = ticket)
                    }
                }
                .onFailure { error ->
                    if (generation == detailGeneration) {
                        _detailState.value = CustomerTicketDetailState(
                            loading = false,
                            ticketId = id,
                            error = ticketErrorMessage(error),
                        )
                    }
                }
        }
    }

    fun create(request: CreateTicketRequest, onResult: (Result<Int>) -> Unit) {
        perform({ repository.create(token, request) }) { result ->
            result.onSuccess { refresh(resetPage = true) }
            onResult(result)
        }
    }

    fun update(id: Int, request: UpdateTicketRequest, onResult: (Result<Unit>) -> Unit) {
        perform({ repository.update(token, id, request) }) { result ->
            result.onSuccess { loadDetail(id) }
            onResult(result.map { Unit })
        }
    }

    fun reply(id: Int, content: String, onResult: (Result<Unit>) -> Unit) {
        perform({ repository.reply(token, id, content) }) { result ->
            result.onSuccess { loadDetail(id) }
            onResult(result.map { Unit })
        }
    }

    fun close(id: Int, onResult: (Result<Unit>) -> Unit) {
        perform({ repository.close(token, id) }) { result ->
            result.onSuccess {
                loadDetail(id)
                refresh()
            }
            onResult(result.map { Unit })
        }
    }

    private fun <T> perform(action: suspend () -> T, onResult: (Result<T>) -> Unit) {
        if (_actionPending.value) return
        viewModelScope.launch {
            _actionPending.value = true
            val result = runCatching { action() }
            _actionPending.value = false
            onResult(result)
        }
    }
}

class CustomerTicketsViewModelFactory(
    private val repository: CustomerTicketRepository,
    private val token: String,
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return CustomerTicketsViewModel(repository, token) as T
    }
}

@Composable
fun CustomerTicketsScreen(
    repository: CustomerTicketRepository,
    settings: AppSettings,
) {
    val token = settings.sessionToken
    if (token.isNullOrBlank()) {
        LoginRequiredTicketPage()
        return
    }

    val vm: CustomerTicketsViewModel = viewModel(
        key = "customer-tickets-$token",
        factory = remember(repository, token) {
            CustomerTicketsViewModelFactory(repository, token)
        },
    )
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "list",
        modifier = Modifier.fillMaxSize(),
    ) {
        composable("list") {
            TicketListPage(
                vm = vm,
                settings = settings,
                onCreate = { navController.navigate("new") },
                onOpen = { navController.navigate("detail/$it") },
            )
        }
        composable("new") {
            TicketCreatePage(
                vm = vm,
                settings = settings,
                onBack = { navController.popBackStack() },
                onCreated = { id ->
                    navController.popBackStack()
                    navController.navigate("detail/$id")
                },
            )
        }
        composable(
            route = "detail/{ticketId}",
            arguments = listOf(navArgument("ticketId") { type = NavType.IntType }),
        ) { entry ->
            val id = entry.arguments?.getInt("ticketId") ?: return@composable
            TicketDetailPage(
                vm = vm,
                settings = settings,
                ticketId = id,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

@Composable
private fun LoginRequiredTicketPage() {
    Scaffold(
        topBar = { TopAppBar(title = { Text("工单") }) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "请先在设置中登录 IGNG 账号",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TicketListPage(
    vm: CustomerTicketsViewModel,
    settings: AppSettings,
    onCreate: () -> Unit,
    onOpen: (Int) -> Unit,
) {
    val state by vm.listState
    val haptic = LocalHapticFeedback.current
    var queryInput by rememberSaveable { mutableStateOf(vm.query) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("工单中心") },
                actions = {
                    IconButton(
                        onClick = { ticketBuzz(haptic, settings); vm.refresh() },
                        enabled = !state.loading,
                    ) {
                        if (state.loading) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Rounded.Refresh, contentDescription = "刷新")
                        }
                    }
                    IconButton(onClick = { ticketBuzz(haptic, settings); onCreate() }) {
                        Icon(Icons.Rounded.Add, contentDescription = "发起工单")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        "all" to "全部参与",
                        "created" to "我发起的",
                        "participated" to "我参与的",
                    ).forEach { (value, label) ->
                        FilterChip(
                            selected = vm.relation == value,
                            onClick = { ticketBuzz(haptic, settings); vm.changeRelation(value) },
                            label = { Text(label) },
                        )
                    }
                    FilterChip(
                        selected = vm.includeClosed,
                        onClick = { ticketBuzz(haptic, settings); vm.changeIncludeClosed(!vm.includeClosed) },
                        label = { Text("查看已结单") },
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = queryInput,
                    onValueChange = { queryInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("搜索工单") },
                    placeholder = { Text("标题或工单编号") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                ticketBuzz(haptic, settings)
                                vm.submitQuery(queryInput)
                            },
                        ) {
                            Icon(Icons.Rounded.Search, contentDescription = "搜索")
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions {
                        vm.submitQuery(queryInput)
                    },
                )
            }
            if (state.data?.bindingLookupUnavailable == true) {
                item { TicketNotice("暂时无法读取 MC 账号绑定信息，“我参与的”列表可能不完整。") }
            }
            if (state.loading && state.data != null) {
                item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            }
            state.error?.let { message ->
                item {
                    TicketErrorBlock(message = message, onRetry = { vm.refresh() })
                }
            }
            val tickets = state.data?.tickets.orEmpty()
            if (!state.loading && state.error == null && tickets.isEmpty()) {
                item {
                    TicketEmptyState(
                        title = if (vm.includeClosed) "还没有符合条件的工单" else "暂时没有未结单工单",
                        description = if (vm.relation == "participated") {
                            "绑定 MC 游戏账号后，允许涉事玩家查看的工单会出现在这里。"
                        } else {
                            "如果你遇到需要管理员处理的问题，可以直接发起一张工单。"
                        },
                        onCreate = if (vm.relation == "participated") null else onCreate,
                    )
                }
            }
            items(tickets, key = { it.id }) { ticket ->
                TicketSummaryCard(ticket = ticket, onClick = { onOpen(ticket.id) })
            }
            if (!state.loading && state.data != null && state.data!!.totalPages > 1) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(
                            onClick = { vm.goToPage(vm.page - 1) },
                            enabled = vm.page > 1,
                        ) { Text("上一页") }
                        Text("第 ${vm.page} / ${state.data!!.totalPages} 页")
                        OutlinedButton(
                            onClick = { vm.goToPage(vm.page + 1) },
                            enabled = vm.page < state.data!!.totalPages,
                        ) { Text("下一页") }
                    }
                }
            }
        }
    }
}

@Composable
private fun TicketSummaryCard(ticket: CustomerTicketSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("#${ticket.id}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(
                        ticket.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TicketPill(
                    ticket.statusLabel.ifBlank { if (ticket.status == "CLOSED") "已结单" else "进行中" },
                    tone = if (ticket.status == "CLOSED") TicketPillTone.Closed else TicketPillTone.Open,
                )
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TicketPill(ticket.typeLabel.ifBlank { ticket.type })
                TicketPill(ticket.relationLabel.ifBlank { "参与者" })
                ticket.conversationStateLabel?.let { TicketPill(it, TicketPillTone.Attention) }
            }
            Text(
                ticket.latestMessage?.content?.replace(Regex("\\s+"), " ")?.trim().orEmpty()
                    .ifBlank { "暂无文字内容" },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TicketMeta("更新 ${ticket.updatedAt.toDisplayTime()}")
                TicketMeta(if (ticket.serverScope == "ALL") "综合" else ticket.serverName ?: "指定服务器")
                TicketMeta(
                    ticket.targets.joinToString("、") { it.mcUsername }
                        .ifBlank { "未指定涉事玩家" },
                )
            }
        }
    }
}

@Composable
private fun TicketCreatePage(
    vm: CustomerTicketsViewModel,
    settings: AppSettings,
    onBack: () -> Unit,
    onCreated: (Int) -> Unit,
) {
    val optionsState by vm.optionsState
    val searchState by vm.playerSearchState
    val pending by vm.actionPending
    val haptic = LocalHapticFeedback.current
    var title by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("") }
    var serverScope by rememberSaveable { mutableStateOf("ALL") }
    var serverId by rememberSaveable { mutableStateOf<Int?>(null) }
    var adminVisibility by rememberSaveable { mutableStateOf("ADMIN") }
    var targetVisibility by rememberSaveable { mutableStateOf(false) }
    var content by rememberSaveable { mutableStateOf("") }
    var playerQuery by rememberSaveable { mutableStateOf("") }
    var selectedTargets by remember { mutableStateOf(emptyList<TicketPlayerOption>()) }
    var error by remember { mutableStateOf<String?>(null) }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var serverMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.loadOptions() }
    LaunchedEffect(optionsState.data) {
        val options = optionsState.data ?: return@LaunchedEffect
        if (type.isBlank()) type = options.types.firstOrNull()?.value.orEmpty()
        if (options.adminVisibility.none { it.value == adminVisibility }) {
            adminVisibility = options.adminVisibility.firstOrNull()?.value ?: "ADMIN"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("发起工单") },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !pending) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        if (optionsState.loading && optionsState.data == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        if (optionsState.error != null && optionsState.data == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                TicketErrorBlock(optionsState.error!!, onRetry = { vm.loadOptions(force = true) })
            }
            return@Scaffold
        }
        val options = optionsState.data ?: TicketOptions()
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                TicketSectionTitle("基本信息", "告诉管理员这是关于什么的请求")
                TicketTextField(
                    value = title,
                    onValueChange = { if (it.length <= 120) title = it },
                    label = "工单标题",
                    supporting = "${title.length}/120",
                    enabled = !pending,
                )
            }
            item {
                TicketChoiceButton(
                    label = "工单类型",
                    value = options.types.firstOrNull { it.value == type }?.label ?: "请选择类型",
                    enabled = !pending,
                    expanded = typeMenuExpanded,
                    onExpandedChange = { typeMenuExpanded = it },
                ) {
                    options.types.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = { type = option.value; typeMenuExpanded = false },
                        )
                    }
                }
            }
            item {
                TicketSectionTitle("涉及服务器", "可以选择综合问题或具体服务器")
                TicketChoiceButton(
                    label = "服务器范围",
                    value = if (serverScope == "ALL") "综合" else options.servers.firstOrNull { it.id == serverId }?.name ?: "请选择服务器",
                    enabled = !pending,
                    expanded = serverMenuExpanded,
                    onExpandedChange = { serverMenuExpanded = it },
                ) {
                    DropdownMenuItem(
                        text = { Text("综合") },
                        onClick = { serverScope = "ALL"; serverId = null; serverMenuExpanded = false },
                    )
                    options.servers.forEach { server ->
                        DropdownMenuItem(
                            text = { Text(server.name) },
                            onClick = { serverScope = "SPECIFIC"; serverId = server.id; serverMenuExpanded = false },
                        )
                    }
                }
            }
            item {
                TicketSectionTitle("管理员可见范围", "提交后不能由用户侧修改")
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    options.adminVisibility.forEach { option ->
                        FilterChip(
                            selected = adminVisibility == option.value,
                            onClick = { adminVisibility = option.value },
                            label = { Text(option.label) },
                            enabled = !pending,
                        )
                    }
                }
                options.adminVisibility.firstOrNull { it.value == adminVisibility }?.description?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                TicketSectionTitle("涉事玩家", "可不选择；只能从 AuthMe 已有玩家中选择")
                TicketPlayerPicker(
                    query = playerQuery,
                    onQueryChange = {
                        playerQuery = it
                        vm.searchPlayers(it)
                    },
                    searchState = searchState,
                    selectedTargets = selectedTargets,
                    onAdd = { player ->
                        if (selectedTargets.none { selected -> selected.username.equals(player.username, ignoreCase = true) }) {
                            selectedTargets = selectedTargets + player
                        }
                        playerQuery = ""
                        vm.clearPlayerSearch()
                    },
                    onRemove = { username ->
                        selectedTargets = selectedTargets.filterNot { it.username == username }
                        if (selectedTargets.isEmpty()) targetVisibility = false
                    },
                    enabled = !pending,
                )
                FilterChip(
                    selected = targetVisibility && selectedTargets.isNotEmpty(),
                    onClick = { targetVisibility = !targetVisibility },
                    label = { Text(if (targetVisibility) "允许涉事玩家查看" else "不允许涉事玩家查看") },
                    leadingIcon = { Icon(if (targetVisibility) Icons.Rounded.Person else Icons.Rounded.Lock, contentDescription = null) },
                    enabled = selectedTargets.isNotEmpty() && !pending,
                )
            }
            item {
                TicketSectionTitle("工单内容", "只支持文字，不支持图片、附件或富文本")
                TicketTextField(
                    value = content,
                    onValueChange = { if (it.length <= 10000) content = it },
                    label = "详细说明",
                    supporting = "${content.length}/10000",
                    minLines = 8,
                    enabled = !pending,
                )
            }
            error?.let { message -> item { TicketNotice(message, isError = true) } }
            item {
                Button(
                    onClick = {
                        error = null
                        if (title.isBlank() || content.isBlank() || type.isBlank()) {
                            error = "请填写标题、类型和工单内容。"
                            return@Button
                        }
                        if (serverScope == "SPECIFIC" && serverId == null) {
                            error = "请选择具体服务器。"
                            return@Button
                        }
                        ticketBuzz(haptic, settings)
                        vm.create(
                            CreateTicketRequest(
                                title = title.trim(),
                                type = type,
                                serverScope = serverScope,
                                serverId = if (serverScope == "SPECIFIC") serverId else null,
                                adminVisibility = adminVisibility,
                                targetVisibility = if (targetVisibility && selectedTargets.isNotEmpty()) "PUBLIC" else "PRIVATE",
                                targetNames = selectedTargets.map { it.username },
                                content = content.trim(),
                            ),
                        ) { result ->
                            result.onSuccess(onCreated).onFailure { error = ticketErrorMessage(it) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !pending,
                ) {
                    if (pending) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Rounded.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (pending) "提交中…" else "提交工单")
                }
            }
        }
    }
}

@Composable
private fun TicketDetailPage(
    vm: CustomerTicketsViewModel,
    settings: AppSettings,
    ticketId: Int,
    onBack: () -> Unit,
) {
    val detailState by vm.detailState
    val pending by vm.actionPending
    val haptic = LocalHapticFeedback.current
    var reply by rememberSaveable(ticketId) { mutableStateOf("") }
    var editing by rememberSaveable(ticketId) { mutableStateOf(false) }
    var closeDialog by rememberSaveable(ticketId) { mutableStateOf(false) }
    var actionError by remember(ticketId) { mutableStateOf<String?>(null) }

    LaunchedEffect(ticketId) { vm.loadDetail(ticketId) }

    BackHandler(enabled = editing && !pending) { editing = false }

    val ticket = detailState.ticket
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("工单 #$ticketId") },
                navigationIcon = {
                    IconButton(onClick = { if (editing) editing = false else onBack() }, enabled = !pending) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { ticketBuzz(haptic, settings); vm.loadDetail(ticketId) }, enabled = !pending) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "刷新")
                    }
                },
            )
        },
    ) { padding ->
        if (detailState.loading && ticket == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        if (ticket == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                TicketErrorBlock(detailState.error ?: "工单读取失败", onRetry = { vm.loadDetail(ticketId) })
            }
            return@Scaffold
        }

        val isClosed = ticket.status == "CLOSED"
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(ticket.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    TicketPill(ticket.typeLabel.ifBlank { ticket.type })
                    TicketPill(ticket.relationLabel.ifBlank { "参与者" })
                    TicketPill(
                        ticket.statusLabel.ifBlank { if (isClosed) "已结单" else "进行中" },
                        if (isClosed) TicketPillTone.Closed else TicketPillTone.Open,
                    )
                    ticket.conversationStateLabel?.let { TicketPill(it, TicketPillTone.Attention) }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (ticket.canEdit) {
                        OutlinedButton(onClick = { editing = !editing }, enabled = !pending) {
                            Icon(Icons.Rounded.Edit, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (editing) "收起编辑" else "编辑工单")
                        }
                    }
                    if (ticket.canClose) {
                        Button(onClick = { closeDialog = true }, enabled = !pending) {
                            Icon(Icons.Rounded.Lock, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("结单")
                        }
                    }
                }
            }
            item {
                TicketInfoCard(ticket)
            }
            if (ticket.bindingLookupUnavailable) {
                item { TicketNotice("暂时无法读取 MC 账号绑定信息，涉事玩家访问状态可能需要稍后刷新。") }
            }
            if (editing && ticket.canEdit) {
                item {
                    TicketEditPanel(
                        vm = vm,
                        settings = settings,
                        ticket = ticket,
                        enabled = !pending,
                        onCancel = { editing = false },
                        onSaved = {
                            editing = false
                            actionError = null
                        },
                        onError = { actionError = ticketErrorMessage(it) },
                    )
                }
            }
            item {
                Text("工单交流（${ticket.messages.size} 条消息）", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
            if (ticket.messages.isEmpty()) {
                item { Text("暂无消息", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(ticket.messages, key = { it.id }) { message -> TicketMessageCard(message) }
            }
            actionError?.let { message -> item { TicketNotice(message, isError = true) } }
            if (isClosed) {
                item { TicketNotice("工单已结单，历史消息仍可查看，但不能继续回复。") }
            } else if (ticket.canReply) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TicketTextField(
                            value = reply,
                            onValueChange = { if (it.length <= 10000) reply = it },
                            label = "继续回复",
                            supporting = "${reply.length}/10000",
                            minLines = 5,
                            enabled = !pending,
                        )
                        Button(
                            onClick = {
                                if (reply.isBlank()) return@Button
                                ticketBuzz(haptic, settings)
                                actionError = null
                                vm.reply(ticket.id, reply.trim()) { result ->
                                    result.onSuccess { reply = "" }.onFailure { actionError = ticketErrorMessage(it) }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = reply.isNotBlank() && !pending,
                        ) {
                            if (pending) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (pending) "发送中…" else "发送回复")
                        }
                    }
                }
            } else {
                item { TicketNotice("当前账号只有查看权限。") }
            }
        }
    }

    if (closeDialog) {
        AlertDialog(
            onDismissRequest = { if (!pending) closeDialog = false },
            title = { Text("结单") },
            text = { Text("结单后将无法继续回复或修改，只能查看历史记录。确定要结单吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        actionError = null
                        vm.close(ticketId) { result ->
                            result.onSuccess { closeDialog = false }.onFailure { actionError = ticketErrorMessage(it) }
                        }
                    },
                    enabled = !pending,
                ) { Text(if (pending) "处理中…" else "确认结单") }
            },
            dismissButton = { TextButton(onClick = { closeDialog = false }, enabled = !pending) { Text("取消") } },
        )
    }
}

@Composable
private fun TicketInfoCard(ticket: CustomerTicketDetail) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TicketInfoRow("发起人", ticket.creatorName)
            TicketInfoRow("涉及服务器", if (ticket.serverScope == "ALL") "综合" else ticket.serverName ?: "指定服务器")
            TicketInfoRow("管理员范围", if (ticket.adminVisibility == "SUPERADMIN") "仅全局管理员" else "全部 MC 管理员")
            TicketInfoRow(
                "涉事玩家",
                ticket.targets.joinToString("、") { it.mcUsername }.ifBlank { "未指定" },
            )
            TicketInfoRow("涉事玩家可见", if (ticket.targetVisibility == "PUBLIC") "允许绑定账号参与" else "仅发起者和管理员可见")
            TicketInfoRow("提交时间", ticket.createdAt.toDisplayTime())
        }
    }
}

@Composable
private fun TicketMessageCard(message: TicketMessage) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(message.authorName, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(message.createdAt.toDisplayTime(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (message.authorLabels.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    message.authorLabels.forEach { label -> TicketPill(label.label, labelTone(label)) }
                    if (message.kind == "INITIAL") TicketPill("原始工单")
                }
            }
            SelectionContainer { Text(message.content) }
        }
    }
}

@Composable
private fun TicketEditPanel(
    vm: CustomerTicketsViewModel,
    settings: AppSettings,
    ticket: CustomerTicketDetail,
    enabled: Boolean,
    onCancel: () -> Unit,
    onSaved: () -> Unit,
    onError: (Throwable) -> Unit,
) {
    val optionsState by vm.optionsState
    val searchState by vm.playerSearchState
    val pending by vm.actionPending
    val haptic = LocalHapticFeedback.current
    var title by remember(ticket.id) { mutableStateOf(ticket.title) }
    var type by remember(ticket.id) { mutableStateOf(ticket.type) }
    var targetVisibility by remember(ticket.id) { mutableStateOf(ticket.targetVisibility == "PUBLIC") }
    var playerQuery by remember(ticket.id) { mutableStateOf("") }
    var selectedTargets by remember(ticket.id) {
        mutableStateOf(ticket.targets.map { TicketPlayerOption(it.mcUsername, it.authmeUsername) })
    }
    LaunchedEffect(Unit) { vm.loadOptions() }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TicketSectionTitle("修改工单信息", "仅在结单前可修改标题、类型、涉事玩家和可见性")
            TicketTextField(
                value = title,
                onValueChange = { if (it.length <= 120) title = it },
                label = "工单标题",
                supporting = "${title.length}/120",
                enabled = enabled,
            )
            val types = optionsState.data?.types.orEmpty()
            var typeMenuExpanded by remember(ticket.id) { mutableStateOf(false) }
            TicketChoiceButton(
                label = "工单类型",
                value = types.firstOrNull { it.value == type }?.label ?: type,
                enabled = enabled && types.isNotEmpty(),
                expanded = typeMenuExpanded,
                onExpandedChange = { typeMenuExpanded = it },
            ) {
                types.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = { type = option.value; typeMenuExpanded = false },
                    )
                }
            }
            TicketInfoRow("管理员范围", if (ticket.adminVisibility == "SUPERADMIN") "仅全局管理员（不可修改）" else "全部 MC 管理员（不可修改）")
            TicketInfoRow("涉及服务器", if (ticket.serverScope == "ALL") "综合（不可修改）" else "${ticket.serverName ?: "指定服务器"}（不可修改）")
            TicketPlayerPicker(
                query = playerQuery,
                onQueryChange = {
                    playerQuery = it
                    vm.searchPlayers(it)
                },
                searchState = searchState,
                selectedTargets = selectedTargets,
                onAdd = {
                    if (selectedTargets.none { selected -> selected.username.equals(it.username, ignoreCase = true) }) {
                        selectedTargets = selectedTargets + it
                    }
                    playerQuery = ""
                    vm.clearPlayerSearch()
                },
                onRemove = { username ->
                    selectedTargets = selectedTargets.filterNot { it.username == username }
                    if (selectedTargets.isEmpty()) targetVisibility = false
                },
                enabled = enabled,
            )
            FilterChip(
                selected = targetVisibility && selectedTargets.isNotEmpty(),
                onClick = { targetVisibility = !targetVisibility },
                label = { Text(if (targetVisibility) "允许涉事玩家查看" else "不允许涉事玩家查看") },
                leadingIcon = { Icon(if (targetVisibility) Icons.Rounded.Person else Icons.Rounded.Lock, contentDescription = null) },
                enabled = selectedTargets.isNotEmpty() && enabled,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onCancel, enabled = !pending, modifier = Modifier.weight(1f)) { Text("取消") }
                Button(
                    onClick = {
                        if (title.isBlank() || type.isBlank()) {
                            onError(IllegalArgumentException("请填写标题并选择工单类型。"))
                            return@Button
                        }
                        ticketBuzz(haptic, settings)
                        vm.update(
                            ticket.id,
                            UpdateTicketRequest(
                                title = title.trim(),
                                type = type,
                                targetVisibility = if (targetVisibility && selectedTargets.isNotEmpty()) "PUBLIC" else "PRIVATE",
                                targetNames = selectedTargets.map { it.username },
                            ),
                        ) { result -> result.onSuccess { onSaved() }.onFailure(onError) }
                    },
                    enabled = enabled && !pending,
                    modifier = Modifier.weight(1f),
                ) {
                    if (pending) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Rounded.Check, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (pending) "保存中…" else "保存修改")
                }
            }
        }
    }
}

@Composable
private fun TicketPlayerPicker(
    query: String,
    onQueryChange: (String) -> Unit,
    searchState: TicketPlayerSearchState,
    selectedTargets: List<TicketPlayerOption>,
    onAdd: (TicketPlayerOption) -> Unit,
    onRemove: (String) -> Unit,
    enabled: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = enabled,
            label = { Text("搜索游戏账号名") },
            placeholder = { Text("至少输入 2 个字符") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = { if (searchState.loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) },
        )
        if (searchState.players.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)) {
                Column {
                    searchState.players.forEach { player ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(player.username)
                                    Text("AuthMe 已注册", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                            onClick = { onAdd(player) },
                            enabled = enabled,
                        )
                    }
                }
            }
        }
        searchState.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        if (selectedTargets.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                selectedTargets.forEach { target ->
                    FilterChip(
                        selected = true,
                        onClick = { onRemove(target.username) },
                        label = { Text(target.username) },
                        trailingIcon = { Icon(Icons.Rounded.Close, contentDescription = "移除 ${target.username}", modifier = Modifier.size(16.dp)) },
                        enabled = enabled,
                    )
                }
            }
        } else {
            Text("没有涉事玩家时可以直接提交。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TicketChoiceButton(
    label: String,
    value: String,
    enabled: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    menu: @Composable () -> Unit,
) {
    Box {
        OutlinedButton(onClick = { onExpandedChange(!expanded) }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) { menu() }
    }
}

@Composable
private fun TicketTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    supporting: String,
    enabled: Boolean,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        supportingText = { Text(supporting) },
        minLines = minLines,
        enabled = enabled,
    )
}

@Composable
private fun TicketSectionTitle(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TicketInfoRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(92.dp))
        Text(value, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TicketMeta(value: String) {
    Text(value, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun TicketNotice(message: String, isError: Boolean = false) {
    Surface(
        color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            message,
            modifier = Modifier.padding(12.dp),
            color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun TicketErrorBlock(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(24.dp),
    ) {
        Text(message, color = MaterialTheme.colorScheme.error)
        OutlinedButton(onClick = onRetry) { Text("重新加载") }
    }
}

@Composable
private fun TicketEmptyState(title: String, description: String, onCreate: (() -> Unit)?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
    ) {
        Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
        onCreate?.let { Button(onClick = it) { Icon(Icons.Rounded.Add, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("发起工单") } }
    }
}

private enum class TicketPillTone { Default, Open, Closed, Attention }

@Composable
private fun TicketPill(text: String, tone: TicketPillTone = TicketPillTone.Default) {
    val (background, foreground) = when (tone) {
        TicketPillTone.Open -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        TicketPillTone.Closed -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        TicketPillTone.Attention -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        TicketPillTone.Default -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(color = background, contentColor = foreground, shape = RoundedCornerShape(50)) {
        Text(text, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), style = MaterialTheme.typography.labelMedium)
    }
}

private fun labelTone(label: TicketAuthorLabel): TicketPillTone = when (label.tone.lowercase(Locale.US)) {
    "accent" -> TicketPillTone.Attention
    "mine" -> TicketPillTone.Open
    "target" -> TicketPillTone.Attention
    else -> TicketPillTone.Default
}

private fun ticketErrorMessage(error: Throwable): String = when (error) {
    is ApiException -> when (error.statusCode) {
        401 -> "会话已失效，请前往设置重新登录。"
        403 -> error.message ?: "当前账号没有权限。"
        404 -> error.message ?: "工单不存在，或当前账号没有查看权限。"
        409 -> error.message ?: "工单状态已变化，请刷新后重试。"
        else -> error.message ?: "工单服务暂时不可用。"
    }
    else -> error.message ?: "工单服务暂时不可用。"
}

private fun ticketBuzz(haptic: HapticFeedback, settings: AppSettings) {
    if (settings.vibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
}
