package com.statproof.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.statproof.domain.usecase.GetUserPreferencesUseCase
import com.statproof.domain.usecase.UpdateUserPreferencesUseCase
import com.statproof.proofengine.models.ProofMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val preferredMode: ProofMode = ProofMode.STANDARD,
    val darkModeOverride: Boolean? = null,
    val fontScale: Float = 1.0f,
    val dynamicColorEnabled: Boolean = true,
    val showHintsByDefault: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getPrefs: GetUserPreferencesUseCase,
    private val updatePrefs: UpdateUserPreferencesUseCase,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        getPrefs.getPreferredMode(),
        getPrefs.getDarkModeOverride(),
        getPrefs.getFontScale(),
        getPrefs.getDynamicColorEnabled(),
        getPrefs.getShowHintsByDefault(),
    ) { mode, dark, font, dynamic, hints ->
        SettingsUiState(mode, dark, font, dynamic, hints)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setMode(mode: ProofMode) = viewModelScope.launch { updatePrefs.setPreferredMode(mode) }
    fun setDarkMode(isDark: Boolean?) = viewModelScope.launch { updatePrefs.setDarkModeOverride(isDark) }
    fun setFontScale(scale: Float) = viewModelScope.launch { updatePrefs.setFontScale(scale) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { updatePrefs.setDynamicColorEnabled(enabled) }
    fun setShowHints(show: Boolean) = viewModelScope.launch { updatePrefs.setShowHintsByDefault(show) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAboutClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var modeMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = onAboutClick) { Icon(Icons.Default.Info, "About") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsGroup("Proof Presentation") {
                // Default proof mode
                ListItem(
                    headlineContent = { Text("Default Proof Mode") },
                    supportingContent = { Text(state.preferredMode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    trailingContent = {
                        Box {
                            TextButton(onClick = { modeMenuExpanded = true }) { Text("Change") }
                            DropdownMenu(expanded = modeMenuExpanded, onDismissRequest = { modeMenuExpanded = false }) {
                                ProofMode.entries.forEach { mode ->
                                    DropdownMenuItem(
                                        text = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                        onClick = { viewModel.setMode(mode); modeMenuExpanded = false },
                                    )
                                }
                            }
                        }
                    },
                )
                Divider()
                // Show hints
                ListItem(
                    headlineContent = { Text("Show Hints by Default") },
                    supportingContent = { Text("Show pedagogical hints in Beginner mode") },
                    trailingContent = {
                        Switch(checked = state.showHintsByDefault, onCheckedChange = viewModel::setShowHints)
                    },
                )
            }

            SettingsGroup("Appearance") {
                // Font scale
                ListItem(
                    headlineContent = { Text("Math Font Size") },
                    supportingContent = { Text("Scale: ${"%.1f".format(state.fontScale)}×") },
                )
                Slider(
                    value = state.fontScale,
                    onValueChange = viewModel::setFontScale,
                    valueRange = 0.8f..1.6f,
                    steps = 7,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Divider()
                // Dynamic color
                ListItem(
                    headlineContent = { Text("Dynamic Color (Material You)") },
                    supportingContent = { Text("Use wallpaper-based color scheme (Android 12+)") },
                    trailingContent = {
                        Switch(checked = state.dynamicColorEnabled, onCheckedChange = viewModel::setDynamicColor)
                    },
                )
                Divider()
                // Dark mode
                ListItem(
                    headlineContent = { Text("Dark Mode") },
                    supportingContent = {
                        Text(when (state.darkModeOverride) {
                            true -> "Always dark"
                            false -> "Always light"
                            null -> "Follow system"
                        })
                    },
                    trailingContent = {
                        Row {
                            listOf(null to "System", false to "Light", true to "Dark").forEach { (value, label) ->
                                FilterChip(
                                    selected = state.darkModeOverride == value,
                                    onClick = { viewModel.setDarkMode(value) },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.padding(end = 4.dp),
                                )
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
        )
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column { content() }
        }
    }
}
