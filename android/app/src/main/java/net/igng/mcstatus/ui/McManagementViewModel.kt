package net.igng.mcstatus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.igng.mcstatus.data.ApiException
import net.igng.mcstatus.data.McAccount
import net.igng.mcstatus.data.McAccountDetailsResponse
import net.igng.mcstatus.data.McDataKind
import net.igng.mcstatus.data.McFakePlayerRecord
import net.igng.mcstatus.data.McLandRecord
import net.igng.mcstatus.data.McLoginRecord
import net.igng.mcstatus.data.McManagementRepository
import net.igng.mcstatus.data.McPermissionsResponse
import net.igng.mcstatus.data.McRecords
import net.igng.mcstatus.data.McServer
import net.igng.mcstatus.data.McTeleportRecord

data class McManagementMessage(
    val text: String,
    val success: Boolean,
)

data class McManagementUiState(
    val accounts: List<McAccount> = emptyList(),
    val accountsLoading: Boolean = true,
    val servers: List<McServer> = emptyList(),
    val dataKind: McDataKind = McDataKind.LANDS,
    val serverFilter: String = "all",
    val accountFilter: String = "all",
    val lands: List<McLandRecord> = emptyList(),
    val teleports: List<McTeleportRecord> = emptyList(),
    val logins: List<McLoginRecord> = emptyList(),
    val fakePlayers: List<McFakePlayerRecord> = emptyList(),
    val dataLoading: Boolean = false,
    val permissions: McPermissionsResponse? = null,
    val permissionsLoading: Boolean = false,
    val selectedAccount: String? = null,
    val accountDetails: McAccountDetailsResponse? = null,
    val detailsLoading: Boolean = false,
    val sessionExpired: Boolean = false,
    val message: McManagementMessage? = null,
)

class McManagementViewModel(
    private val repository: McManagementRepository,
    private val token: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow(McManagementUiState())
    val uiState: StateFlow<McManagementUiState> = _uiState.asStateFlow()

    private var dataRequestId = 0

    init {
        refreshAccounts()
        refreshServers()
    }

    fun refreshAccounts() {
        viewModelScope.launch {
            _uiState.update { it.copy(accountsLoading = true) }
            try {
                val accounts = repository.fetchAccounts(token)
                _uiState.update { it.copy(accounts = accounts, accountsLoading = false, sessionExpired = false) }
            } catch (error: Throwable) {
                _uiState.update { it.copy(accountsLoading = false) }
                showFailure(error, "获取游戏账号失败，请稍后重试。")
            }
        }
    }

    private fun refreshServers() {
        viewModelScope.launch {
            try {
                val servers = repository.fetchServers(token)
                _uiState.update { it.copy(servers = servers, sessionExpired = false) }
            } catch (error: Throwable) {
                showFailure(error, "获取服务器列表失败，请稍后重试。")
            }
        }
    }

    fun selectDataKind(kind: McDataKind) {
        _uiState.update { it.copy(dataKind = kind) }
        refreshData(kind)
    }

    fun setServerFilter(value: String) {
        _uiState.update { it.copy(serverFilter = value) }
        refreshData(serverId = value)
    }

    fun setAccountFilter(value: String) {
        _uiState.update { it.copy(accountFilter = value) }
        refreshData(username = value)
    }

    fun refreshData(kind: McDataKind = _uiState.value.dataKind, username: String = _uiState.value.accountFilter, serverId: String = _uiState.value.serverFilter) {
        val requestId = ++dataRequestId
        viewModelScope.launch {
            _uiState.update { it.copy(dataLoading = true) }
            try {
                val records = repository.fetchRecords(token, kind, username, serverId)
                if (requestId != dataRequestId) return@launch
                _uiState.update {
                    when (records) {
                        is McRecords.Lands -> it.copy(lands = records.records)
                        is McRecords.Teleports -> it.copy(teleports = records.records)
                        is McRecords.Logins -> it.copy(logins = records.records)
                        is McRecords.FakePlayers -> it.copy(fakePlayers = records.records)
                    }.copy(sessionExpired = false)
                }
            } catch (error: Throwable) {
                if (requestId == dataRequestId) showFailure(error, "读取 Minecraft 数据失败，请稍后重试。")
            } finally {
                if (requestId == dataRequestId) _uiState.update { it.copy(dataLoading = false) }
            }
        }
    }

    fun refreshPermissions() {
        viewModelScope.launch {
            _uiState.update { it.copy(permissionsLoading = true) }
            try {
                val permissions = repository.fetchPermissions(token)
                _uiState.update { it.copy(permissions = permissions, permissionsLoading = false, sessionExpired = false) }
            } catch (error: Throwable) {
                _uiState.update { it.copy(permissionsLoading = false) }
                showFailure(error, "权限信息暂时不可用，请稍后重试。")
            }
        }
    }

    fun refreshCurrent(section: McManagementSection) {
        when (section) {
            McManagementSection.ACCOUNTS -> refreshAccounts()
            McManagementSection.DATA -> refreshData()
            McManagementSection.PERMISSIONS -> refreshPermissions()
        }
    }

    fun openAccountDetails(username: String) {
        _uiState.update {
            it.copy(selectedAccount = username, accountDetails = null, detailsLoading = true)
        }
        loadAccountDetails(username, 1)
    }

    fun loadAccountDetails(username: String = _uiState.value.selectedAccount.orEmpty(), page: Int = 1) {
        if (username.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(detailsLoading = true) }
            try {
                val details = repository.fetchAccountDetails(token, username, page)
                _uiState.update { it.copy(accountDetails = details, detailsLoading = false, sessionExpired = false) }
            } catch (error: Throwable) {
                _uiState.update { it.copy(detailsLoading = false) }
                showFailure(error, "获取账号详情失败，请稍后重试。")
            }
        }
    }

    fun closeAccountDetails() {
        _uiState.update { it.copy(selectedAccount = null, accountDetails = null, detailsLoading = false) }
    }

    fun toggleAccountPublic(account: McAccount, enabled: Boolean) {
        performAccountAction(
            username = account.mc_username,
            action = "toggle-public",
            isPublic = enabled,
            successMessage = if (enabled) "游戏账号已公开展示" else "游戏账号已停止公开展示",
        )
    }

    fun resetAccountPassword(username: String, password: String) {
        performAccountAction(
            username = username,
            action = "reset-password",
            newPassword = password,
            successMessage = "游戏账号密码已重置",
        )
    }

    fun unbindAccount(username: String) {
        performAccountAction(
            username = username,
            action = "unbind",
            successMessage = "游戏账号已解绑",
            afterSuccess = {
                if (_uiState.value.selectedAccount == username) closeAccountDetails()
            },
        )
    }

    private fun performAccountAction(
        username: String,
        action: String,
        newPassword: String? = null,
        isPublic: Boolean? = null,
        successMessage: String,
        afterSuccess: () -> Unit = {},
    ) {
        viewModelScope.launch {
            try {
                repository.manageAccount(token, username, action, newPassword, isPublic)
                _uiState.update { it.copy(message = McManagementMessage(successMessage, true), sessionExpired = false) }
                afterSuccess()
                refreshAccounts()
            } catch (error: Throwable) {
                showFailure(error, "操作失败，请稍后重试。")
            }
        }
    }

    fun toggleLandPublic(land: McLandRecord, enabled: Boolean) {
        viewModelScope.launch {
            try {
                repository.manageLand(token, land.id, "toggle-public", isPublic = enabled)
                _uiState.update {
                    it.copy(
                        lands = it.lands.map { current ->
                            if (current.id == land.id) current.copy(is_public = enabled) else current
                        },
                        message = McManagementMessage(if (enabled) "领地已公开" else "领地已停止公开", true),
                        sessionExpired = false,
                    )
                }
            } catch (error: Throwable) {
                showFailure(error, "更新领地公开状态失败，请稍后重试。")
            }
        }
    }

    fun updateLandDescription(land: McLandRecord, description: String) {
        viewModelScope.launch {
            try {
                val result = repository.manageLand(token, land.id, "update-description", description = description)
                val saved = result.description ?: description.trim()
                _uiState.update {
                    it.copy(
                        lands = it.lands.map { current ->
                            if (current.id == land.id) current.copy(description = saved) else current
                        },
                        message = McManagementMessage("领地简介已更新", true),
                        sessionExpired = false,
                    )
                }
            } catch (error: Throwable) {
                showFailure(error, "更新领地简介失败，请稍后重试。")
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun showFailure(error: Throwable, fallback: String) {
        val expired = error is ApiException && error.statusCode == 401
        _uiState.update {
            it.copy(
                sessionExpired = it.sessionExpired || expired,
                message = McManagementMessage(
                    if (expired) "登录状态已失效，请返回设置重新登录。" else error.message ?: fallback,
                    false,
                ),
            )
        }
    }
}

class McManagementViewModelFactory(
    private val repository: McManagementRepository,
    private val token: String,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return McManagementViewModel(repository, token) as T
    }
}
