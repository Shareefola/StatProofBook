package com.statproof.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.statproof.domain.usecase.GetAllTheoremsUseCase
import com.statproof.domain.usecase.GetHomeDataUseCase
import com.statproof.domain.usecase.SearchTheoremsUseCase
import com.statproof.proofengine.models.Difficulty
import com.statproof.proofengine.models.SearchResult
import com.statproof.proofengine.models.TheoremDefinition
import com.statproof.proofengine.models.Topic
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val allTheorems: List<TheoremDefinition> = emptyList(),
    val recentIds: List<String> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val selectedTopics: Set<Topic> = emptySet(),
    val selectedDifficulties: Set<Difficulty> = emptySet(),
    val error: String? = null,
)

sealed interface HomeUiEffect {
    data class NavigateToProof(val proofId: String) : HomeUiEffect
    data class ShowError(val message: String) : HomeUiEffect
}

@OptIn(FlowPreview::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getAllTheoremsUseCase: GetAllTheoremsUseCase,
    private val getHomeDataUseCase: GetHomeDataUseCase,
    private val searchTheoremsUseCase: SearchTheoremsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
        observeSearch()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                getAllTheoremsUseCase(),
                getHomeDataUseCase.getHomeState(),
            ) { theorems, homeData ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        allTheorems = theorems,
                        recentIds = homeData.recentIds,
                        favoriteIds = homeData.favoriteIds,
                    )
                }
            }.collect {}
        }
    }

    private fun observeSearch() {
        viewModelScope.launch {
            _uiState
                .debounce(SEARCH_DEBOUNCE_MS)
                .flatMapLatest { state ->
                    flow {
                        val query = state.searchQuery
                        val topics = state.selectedTopics
                        val diffs = state.selectedDifficulties
                        if (query.isBlank() && topics.isEmpty() && diffs.isEmpty()) {
                            emit(emptyList<SearchResult>())
                            return@flow
                        }
                        _uiState.update { it.copy(isSearching = true) }
                        val results = runCatching {
                            searchTheoremsUseCase(query, topics, diffs)
                        }.getOrDefault(emptyList())
                        emit(results)
                    }
                }.collect { results ->
                    _uiState.update { it.copy(searchResults = results, isSearching = false) }
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onTopicFilterChanged(topic: Topic, selected: Boolean) {
        _uiState.update { state ->
            val topics = if (selected) state.selectedTopics + topic
            else state.selectedTopics - topic
            state.copy(selectedTopics = topics)
        }
    }

    fun onDifficultyFilterChanged(difficulty: Difficulty, selected: Boolean) {
        _uiState.update { state ->
            val diffs = if (selected) state.selectedDifficulties + difficulty
            else state.selectedDifficulties - difficulty
            state.copy(selectedDifficulties = diffs)
        }
    }

    fun onClearSearch() {
        _uiState.update { it.copy(searchQuery = "", searchResults = emptyList(), selectedTopics = emptySet()) }
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 300L
    }
}
