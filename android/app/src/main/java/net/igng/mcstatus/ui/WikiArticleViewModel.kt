package net.igng.mcstatus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.igng.mcstatus.data.WikiPage
import net.igng.mcstatus.data.WikiRepository

data class WikiArticleUiState(
    val pageId: Int,
    val page: WikiPage? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val fromCache: Boolean = false,
    val errorMessage: String? = null,
)

class WikiArticleViewModel(
    private val repository: WikiRepository,
    pageId: Int,
) : ViewModel() {
    private val _uiState = MutableStateFlow(WikiArticleUiState(pageId = pageId))
    val uiState: StateFlow<WikiArticleUiState> = _uiState.asStateFlow()

    private var loadGeneration = 0L

    init {
        refresh()
    }

    fun refresh() {
        val generation = ++loadGeneration
        viewModelScope.launch {
            var cached = _uiState.value.page
            if (cached == null) {
                cached = repository.readCachedPage(_uiState.value.pageId)
                if (cached != null && generation == loadGeneration) {
                    _uiState.value = _uiState.value.copy(
                        page = cached,
                        isLoading = false,
                        fromCache = true,
                        errorMessage = null,
                    )
                }
            }

            _uiState.value = _uiState.value.copy(
                isLoading = _uiState.value.page == null,
                isRefreshing = true,
                errorMessage = null,
            )

            runCatching { repository.fetchPage(_uiState.value.pageId) }
                .onSuccess { page ->
                    if (generation != loadGeneration) return@onSuccess
                    _uiState.value = _uiState.value.copy(
                        page = page,
                        isLoading = false,
                        isRefreshing = false,
                        fromCache = false,
                        errorMessage = null,
                    )
                }
                .onFailure { error ->
                    if (generation != loadGeneration) return@onFailure
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        fromCache = cached != null,
                        errorMessage = error.message ?: "教程文章加载失败",
                    )
                }
        }
    }
}

class WikiArticleViewModelFactory(
    private val repository: WikiRepository,
    private val pageId: Int,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return WikiArticleViewModel(repository, pageId) as T
    }
}
