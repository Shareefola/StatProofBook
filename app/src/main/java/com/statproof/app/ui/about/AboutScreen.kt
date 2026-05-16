package com.statproof.app.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About StatProof") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // App identity
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Default.AutoStories,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("StatProof", style = MaterialTheme.typography.headlineMedium)
                    Text("Version 1.0.0", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Offline statistical proof explorer for students and researchers.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Mission
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Mission", style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "StatProof provides rigorous, step-by-step mathematical derivations for " +
                                "statistical theorems. All proofs run entirely offline — no internet, " +
                                "no cloud, no accounts required.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            // Open-source licenses
            item {
                Text("Open-Source Components", style = MaterialTheme.typography.titleMedium)
            }

            item {
                Card {
                    Column {
                        LicenseItem(
                            name = "KaTeX",
                            version = "0.16.x",
                            license = "MIT License",
                            copyright = "© Khan Academy",
                            description = "Math rendering engine used for LaTeX display.",
                        )
                        Divider()
                        LicenseItem(
                            name = "Jetpack Compose",
                            version = "2024.12",
                            license = "Apache 2.0",
                            copyright = "© Google LLC",
                            description = "Modern declarative Android UI toolkit.",
                        )
                        Divider()
                        LicenseItem(
                            name = "Hilt",
                            version = "2.52",
                            license = "Apache 2.0",
                            copyright = "© Google LLC",
                            description = "Dependency injection for Android.",
                        )
                        Divider()
                        LicenseItem(
                            name = "Room",
                            version = "2.6.1",
                            license = "Apache 2.0",
                            copyright = "© Google LLC",
                            description = "SQLite persistence library with FTS support.",
                        )
                        Divider()
                        LicenseItem(
                            name = "kotlinx.serialization",
                            version = "1.7.3",
                            license = "Apache 2.0",
                            copyright = "© JetBrains",
                            description = "Kotlin-native JSON serialization.",
                        )
                        Divider()
                        LicenseItem(
                            name = "kotlinx.coroutines",
                            version = "1.9.0",
                            license = "Apache 2.0",
                            copyright = "© JetBrains",
                            description = "Asynchronous programming for Kotlin.",
                        )
                        Divider()
                        LicenseItem(
                            name = "DataStore",
                            version = "1.1.1",
                            license = "Apache 2.0",
                            copyright = "© Google LLC",
                            description = "Key-value preference storage.",
                        )
                        Divider()
                        LicenseItem(
                            name = "Mockk",
                            version = "1.13.13",
                            license = "Apache 2.0",
                            copyright = "© Mockk contributors",
                            description = "Kotlin-idiomatic mocking library for tests.",
                        )
                        Divider()
                        LicenseItem(
                            name = "Turbine",
                            version = "1.2.0",
                            license = "Apache 2.0",
                            copyright = "© Cash App",
                            description = "Flow testing library.",
                        )
                    }
                }
            }

            // Technical details
            item {
                Text("Technical Details", style = MaterialTheme.typography.titleMedium)
            }
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        DetailRow("Architecture", "Clean Architecture + MVVM")
                        DetailRow("Proof Engine", "Native Kotlin symbolic AST engine")
                        DetailRow("Math Rendering", "KaTeX (offline WebView)")
                        DetailRow("Database", "Room + SQLite FTS4")
                        DetailRow("Min Android", "API 26 (Android 8.0)")
                        DetailRow("Target Android", "API 35 (Android 15)")
                        DetailRow("Network", "None — fully offline")
                        DetailRow("Telemetry", "None — zero data collection")
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun LicenseItem(
    name: String,
    version: String,
    license: String,
    copyright: String,
    description: String,
) {
    ListItem(
        headlineContent = { Text("$name $version", style = MaterialTheme.typography.titleSmall) },
        supportingContent = {
            Column {
                Text(description, style = MaterialTheme.typography.bodySmall)
                Text("$license · $copyright",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    androidx.compose.foundation.layout.Row {
        Text("$label: ", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}
