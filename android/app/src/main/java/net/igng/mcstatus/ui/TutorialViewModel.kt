package net.igng.mcstatus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.net.Uri
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.igng.mcstatus.data.WikiCatalogNode
import net.igng.mcstatus.data.WikiPageSummary
import net.igng.mcstatus.data.WikiRepository

data class TutorialUiState(
    val catalog: net.igng.mcstatus.data.WikiCatalogResponse? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val query: String = "",
    val errorMessage: String? = null,
    val fromCache: Boolean = false,
    val lastSyncAt: String? = null,
    val expandedNodeIds: Set<Int> = emptySet(),
) {
    val rootPage: WikiPageSummary?
        get() = catalog?.knowledgeBase?.rootPage

    val nodes: List<WikiCatalogNode>
        get() = catalog?.nodes.orEmpty()

    val allPages: List<WikiPageSummary>
        get() = buildList {
            rootPage?.let(::add)
            nodes.forEach { addPages(it, this) }
        }

    val visiblePages: List<WikiPageSummary>
        get() {
            val normalized = query.trim().lowercase(Locale.ROOT)
            if (normalized.isBlank()) return allPages
            return allPages.filter { page ->
                page.title.lowercase(Locale.ROOT).contains(normalized) ||
                    page.path.lowercase(Locale.ROOT).contains(normalized)
            }
        }

    private fun addPages(node: WikiCatalogNode, target: MutableList<WikiPageSummary>) {
        node.page?.let(target::add)
        node.children.forEach { addPages(it, target) }
    }
}

class TutorialViewModel(
    private val repository: WikiRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TutorialUiState())
    val uiState: StateFlow<TutorialUiState> = _uiState.asStateFlow()

    private var loadGeneration = 0L

    fun loadIfNeeded() {
        if (_uiState.value.catalog == null && !_uiState.value.isRefreshing) refresh()
    }

    fun setQuery(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
    }

    fun toggleNode(nodeId: Int) {
        val expanded = _uiState.value.expandedNodeIds.toMutableSet()
        if (!expanded.add(nodeId)) expanded.remove(nodeId)
        _uiState.value = _uiState.value.copy(expandedNodeIds = expanded)
    }

    fun refresh() {
        val generation = ++loadGeneration
        viewModelScope.launch {
            var cached = _uiState.value.catalog
            if (cached == null) {
                cached = repository.readCachedCatalog()
                if (cached != null && generation == loadGeneration) {
                    _uiState.value = _uiState.value.copy(
                        catalog = cached,
                        isLoading = false,
                        fromCache = true,
                        lastSyncAt = cached.generatedAt,
                        errorMessage = null,
                        expandedNodeIds = defaultExpandedNodes(cached.nodes),
                    )
                }
            }

            _uiState.value = _uiState.value.copy(
                isLoading = _uiState.value.catalog == null,
                isRefreshing = true,
                errorMessage = null,
            )

            runCatching { repository.fetchCatalog() }
                .onSuccess { catalog ->
                    if (generation != loadGeneration) return@onSuccess
                    _uiState.value = _uiState.value.copy(
                        catalog = catalog,
                        isLoading = false,
                        isRefreshing = false,
                        fromCache = false,
                        lastSyncAt = catalog.generatedAt,
                        errorMessage = null,
                        expandedNodeIds = _uiState.value.expandedNodeIds.ifEmpty {
                            defaultExpandedNodes(catalog.nodes)
                        },
                    )
                }
                .onFailure { error ->
                    if (generation != loadGeneration) return@onFailure
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        fromCache = cached != null,
                        errorMessage = error.message ?: "教程目录加载失败",
                    )
                }
        }
    }

    fun neighborsFor(pageId: Int): TutorialPageNeighbors {
        val pages = _uiState.value.allPages
        val index = pages.indexOfFirst { it.id == pageId }
        if (index < 0) return TutorialPageNeighbors()
        return TutorialPageNeighbors(
            previous = pages.getOrNull(index - 1),
            next = pages.getOrNull(index + 1),
        )
    }

    fun pageIdForUrl(url: String): Int? {
        val path = runCatching { Uri.parse(url).path }.getOrNull()
            ?.trim('/')
            ?.let(Uri::decode)
            ?: return null
        return _uiState.value.allPages.firstOrNull { it.path.trim('/') == path }?.id
    }

    private fun defaultExpandedNodes(nodes: List<WikiCatalogNode>): Set<Int> =
        nodes.filter { it.children.isNotEmpty() || it.page != null }.map { it.id }.toSet()
}

data class TutorialPageNeighbors(
    val previous: WikiPageSummary? = null,
    val next: WikiPageSummary? = null,
)

class TutorialViewModelFactory(
    private val repository: WikiRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return TutorialViewModel(repository) as T
    }
}
