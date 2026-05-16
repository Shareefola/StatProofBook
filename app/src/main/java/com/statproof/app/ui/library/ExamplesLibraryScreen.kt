package com.statproof.app.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.statproof.domain.usecase.GetTheoremsByTopicUseCase
import com.statproof.proofengine.models.TheoremDefinition
import com.statproof.proofengine.models.Topic
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val selectedTopic: Topic = Topic.PROBABILITY_THEORY,
    val theorems: List<TheoremDefinition> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val getTheoremsByTopicUseCase: GetTheoremsByTopicUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init { loadTopic(Topic.PROBABILITY_THEORY) }

    fun onTopicSelected(topic: Topic) { loadTopic(topic) }

    private fun loadTopic(topic: Topic) {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedTopic = topic, isLoading = true) }
            getTheoremsByTopicUseCase(topic).collect { theorems ->
                _uiState.update { it.copy(theorems = theorems, isLoading = false) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamplesLibraryScreen(
    onProofClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Theorem Library") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Topic tabs
            ScrollableTabRow(
                selectedTabIndex = Topic.entries.indexOf(uiState.selectedTopic),
            ) {
                Topic.entries.forEachIndexed { _, topic ->
                    Tab(
                        selected = uiState.selectedTopic == topic,
                        onClick = { viewModel.onTopicSelected(topic) },
                        text = { Text(topic.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.theorems, key = { it.id }) { theorem ->
                        LibraryTheoremCard(theorem = theorem, onClick = { onProofClick(theorem.id) })
                    }
                    if (uiState.theorems.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No theorems in this topic yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryTheoremCard(theorem: TheoremDefinition, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(theorem.title, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(theorem.subtopic, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SuggestionChip(onClick = {}, label = {
                    Text(theorem.difficulty.displayName, style = MaterialTheme.typography.labelSmall)
                })
                SuggestionChip(onClick = {}, label = {
                    Text("${theorem.derivationSteps.size} steps", style = MaterialTheme.typography.labelSmall)
                })
            }
            if (theorem.intuition.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(theorem.intuition, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
