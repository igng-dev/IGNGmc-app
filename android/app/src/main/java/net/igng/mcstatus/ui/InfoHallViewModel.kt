package net.igng.mcstatus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.igng.mcstatus.data.InfoHallFakePlayer
import net.igng.mcstatus.data.InfoHallLand
import net.igng.mcstatus.data.InfoHallRepository

enum class InfoHallTab(val route: String, val label: String) {
    LANDS("lands", "领地"),
    FAKE_PLAYERS("fake-players", "假人"),
    ;

    companion object {
        fun fromRoute(route: String?): InfoHallTab =
            entries.firstOrNull { it.route == route } ?: LANDS
    }
}

data class InfoHallUiState(
    val tab: InfoHallTab = InfoHallTab.LANDS,
    val isLoading: Boolean = true,
    val lands: List<InfoHallLand> = emptyList(),
    val fakePlayers: List<InfoHallFakePlayer> = emptyList(),
    val query: String = "",
    val errorMessage: String? = null,
) {
    private val normalizedQuery: String
        get() = query.trim().lowercase(Locale.ROOT)

    val visibleLands: List<InfoHallLand>
        get() = lands.filter { land ->
            normalizedQuery.isBlank() || listOf(
                land.name,
                land.ownerName,
                land.serverName,
                land.world.orEmpty(),
            ).any { it.lowercase(Locale.ROOT).contains(normalizedQuery) }
        }

    val visibleFakePlayers: List<InfoHallFakePlayer>
        get() = fakePlayers.filter { player ->
            normalizedQuery.isBlank() || listOf(
                player.name,
                player.ownerName,
                player.serverName,
            ).any { it.lowercase(Locale.ROOT).contains(normalizedQuery) }
        }

    val visibleCount: Int
        get() = if (tab == InfoHallTab.LANDS) visibleLands.size else visibleFakePlayers.size

    val hasVisibleData: Boolean
        get() = if (tab == InfoHallTab.LANDS) visibleLands.isNotEmpty() else visibleFakePlayers.isNotEmpty()

    val hasLoadedData: Boolean
        get() = if (tab == InfoHallTab.LANDS) lands.isNotEmpty() else fakePlayers.isNotEmpty()
}

class InfoHallViewModel(
    private val repository: InfoHallRepository,
    initialTab: InfoHallTab = InfoHallTab.LANDS,
) : ViewModel() {
    private val _uiState = MutableStateFlow(InfoHallUiState(tab = initialTab))
    val uiState: StateFlow<InfoHallUiState> = _uiState.asStateFlow()

    private var loadGeneration = 0L

    init {
        refresh()
    }

    fun selectTab(tab: InfoHallTab) {
        if (_uiState.value.tab == tab) return
        _uiState.value = _uiState.value.copy(
            tab = tab,
            query = "",
            isLoading = true,
            errorMessage = null,
        )
        refresh()
    }

    fun setQuery(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
    }

    fun refresh() {
        val tab = _uiState.value.tab
        val generation = ++loadGeneration
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                if (tab == InfoHallTab.LANDS) {
                    InfoHallLoad(lands = repository.fetchLands())
                } else {
                    InfoHallLoad(fakePlayers = repository.fetchFakePlayers())
                }
            }.onSuccess { load ->
                if (generation != loadGeneration || _uiState.value.tab != tab) return@onSuccess
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    lands = load.lands ?: _uiState.value.lands,
                    fakePlayers = load.fakePlayers ?: _uiState.value.fakePlayers,
                    errorMessage = null,
                )
            }.onFailure { error ->
                if (generation != loadGeneration || _uiState.value.tab != tab) return@onFailure
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "信息大厅加载失败",
                )
            }
        }
    }
}

private data class InfoHallLoad(
    val lands: List<InfoHallLand>? = null,
    val fakePlayers: List<InfoHallFakePlayer>? = null,
)

class InfoHallViewModelFactory(
    private val repository: InfoHallRepository,
    private val initialTab: InfoHallTab,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return InfoHallViewModel(repository, initialTab) as T
    }
}
