package com.statproof.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.statproof.proofengine.models.Difficulty
import com.statproof.proofengine.models.SearchResult
import com.statproof.proofengine.models.TheoremDefinition
import com.statproof.proofengine.models.Topic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onProofClick: (String) -> Unit,
    onComposeClick: () -> Unit,
    onLibraryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("StatProof", style = MaterialTheme.typography.headlineMedium) },
                actions = {
                    IconButton(onClick = onComposeClick) {
                        Icon(Icons.Default.Edit, contentDescription = "Compose proof")
                    }
                    IconButton(onClick = onLibraryClick) {
                        Icon(Icons.Default.Book, contentDescription = "Open library")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            // ── Search Bar ────────────────────────────────────────────────────
            item {
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChanged = viewModel::onSearchQueryChanged,
                    onClear = viewModel::onClearSearch,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            // ── Topic Filter Chips ────────────────────────────────────────────
            item {
                TopicFilterRow(
                    selectedTopics = uiState.selectedTopics,
                    onTopicToggled = viewModel::onTopicFilterChanged,
                )
            }

            // ── Search Results ────────────────────────────────────────────────
            if (uiState.searchQuery.isNotBlank() || uiState.selectedTopics.isNotEmpty()) {
                if (uiState.isSearching) {
                    item {
                        Box(
                            Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                } else {
                    item {
                        SectionHeader(
                            title = "Results (${uiState.searchResults.size})",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(uiState.searchResults, key = { it.theoremId }) { result ->
                        SearchResultCard(
                            result = result,
                            onClick = { onProofClick(result.theoremId) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                    if (uiState.searchResults.isEmpty()) {
                        item {
                            EmptySearchState(modifier = Modifier.padding(32.dp))
                        }
                    }
                }
                return@LazyColumn
            }

            // ── Recent Proofs ─────────────────────────────────────────────────
            if (uiState.recentIds.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Recently Viewed",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                val recents = uiState.recentIds.mapNotNull { id ->
                    uiState.allTheorems.find { it.id == id }
                }
                items(recents.take(5), key = { "recent_${it.id}" }) { theorem ->
                    TheoremCard(
                        theorem = theorem,
                        isFavorite = theorem.id in uiState.favoriteIds,
                        onClick = { onProofClick(theorem.id) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }

            // ── Favorites ─────────────────────────────────────────────────────
            if (uiState.favoriteIds.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Favorites",
                        action = "See all" to onLibraryClick,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                val favorites = uiState.favoriteIds.mapNotNull { id ->
                    uiState.allTheorems.find { it.id == id }
                }
                items(favorites.take(3), key = { "fav_${it.id}" }) { theorem ->
                    TheoremCard(
                        theorem = theorem,
                        isFavorite = true,
                        onClick = { onProofClick(theorem.id) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }

            // ── Browse by Topic ───────────────────────────────────────────────
            item {
                SectionHeader(
                    title = "Browse Topics",
                    action = "Full library" to onLibraryClick,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            item { TopicBrowserRow(onTopicClick = { topic ->
                viewModel.onTopicFilterChanged(topic, true)
            }) }

            // ── All Theorems ──────────────────────────────────────────────────
            item {
                SectionHeader(
                    title = "All Theorems",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            items(uiState.allTheorems, key = { it.id }) { theorem ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically(),
                ) {
                    TheoremCard(
                        theorem = theorem,
                        isFavorite = theorem.id in uiState.favoriteIds,
                        onClick = { onProofClick(theorem.id) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Search theorems" },
        placeholder = { Text("Search theorems, topics, keywords…") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.large,
    )
}

@Composable
private fun TopicFilterRow(
    selectedTopics: Set<Topic>,
    onTopicToggled: (Topic, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(Topic.entries) { topic ->
            FilterChip(
                selected = topic in selectedTopics,
                onClick = { onTopicToggled(topic, topic !in selectedTopics) },
                label = { Text(topic.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    action: Pair<String, () -> Unit>? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (action != null) {
            TextButton(onClick = action.second) {
                Text(action.first)
            }
        }
    }
}

@Composable
private fun TheoremCard(
    theorem: TheoremDefinition,
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = theorem.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (isFavorite) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = "Favorited",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DifficultyBadge(theorem.difficulty.displayName)
                TopicBadge(theorem.topic.displayName)
            }
            if (theorem.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    theorem.tags.take(3).forEach { tag ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    result: SearchResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(result.title, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                result.snippet,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DifficultyBadge(result.difficulty.displayName)
                TopicBadge(result.topic.displayName)
            }
        }
    }
}

@Composable
private fun DifficultyBadge(text: String) {
    SuggestionChip(
        onClick = {},
        label = { Text(text, style = MaterialTheme.typography.labelSmall) },
        icon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(12.dp)) },
    )
}

@Composable
private fun TopicBadge(text: String) {
    SuggestionChip(
        onClick = {},
        label = { Text(text, style = MaterialTheme.typography.labelSmall) },
    )
}

@Composable
private fun TopicBrowserRow(
    onTopicClick: (Topic) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(Topic.entries) { topic ->
            TopicCard(topic = topic, onClick = { onTopicClick(topic) })
        }
    }
}

@Composable
private fun TopicCard(topic: Topic, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(140.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = topic.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EmptySearchState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "No theorems found",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
