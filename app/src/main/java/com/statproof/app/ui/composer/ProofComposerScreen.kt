package com.statproof.app.ui.composer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.statproof.proofengine.models.Topic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProofComposerScreen(
    onBack: () -> Unit,
    onProofGenerated: (String) -> Unit,
) {
    var selectedTopic by remember { mutableStateOf(Topic.PROBABILITY_THEORY) }
    var customStatement by remember { mutableStateOf("") }
    var topicExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compose Proof") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { /* Open library to pick a theorem */ onBack() },
                icon = { Icon(Icons.Default.PlayArrow, null) },
                text = { Text("Browse Library") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Select a topic and explore theorems from the library.", style = MaterialTheme.typography.bodyMedium)

            ExposedDropdownMenuBox(
                expanded = topicExpanded,
                onExpandedChange = { topicExpanded = it },
            ) {
                OutlinedTextField(
                    value = selectedTopic.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Topic") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(topicExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                )
                ExposedDropdownMenu(expanded = topicExpanded, onDismissRequest = { topicExpanded = false }) {
                    Topic.entries.forEach { topic ->
                        DropdownMenuItem(
                            text = { Text(topic.displayName) },
                            onClick = { selectedTopic = topic; topicExpanded = false },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = customStatement,
                onValueChange = { customStatement = it },
                label = { Text("Notes / custom statement (optional)") },
                placeholder = { Text("E.g. 'Prove the CLT for i.i.d. variables'") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("How to use:", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("1. Select a topic above", style = MaterialTheme.typography.bodySmall)
                    Text("2. Tap 'Browse Library' to find theorems", style = MaterialTheme.typography.bodySmall)
                    Text("3. Open any theorem to view its full proof", style = MaterialTheme.typography.bodySmall)
                    Text("4. Change the proof mode for different detail levels", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
