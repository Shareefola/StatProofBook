package com.statproof.app.ui.viewer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.statproof.app.ui.components.KaTeXView
import com.statproof.proofengine.models.Proof
import com.statproof.proofengine.models.ProofMode
import com.statproof.proofengine.models.ProofStep
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProofViewerScreen(
    proofId: String,
    onBack: () -> Unit,
    viewModel: ProofViewerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }

    // Copy LaTeX to clipboard when triggered
    LaunchedEffect(uiState.copiedLatex) {
        uiState.copiedLatex?.let { latex ->
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("LaTeX", latex))
            scope.launch { snackbarHostState.showSnackbar("LaTeX copied to clipboard") }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        uiState.proof?.title ?: "Proof",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Go back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::onToggleFavorite) {
                        Icon(
                            if (uiState.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (uiState.isFavorite) "Remove favorite" else "Add favorite",
                            tint = if (uiState.isFavorite) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Expand all steps") },
                            onClick = { viewModel.onExpandAll(); menuExpanded = false },
                        )
                        DropdownMenuItem(
                            text = { Text("Collapse all steps") },
                            onClick = { viewModel.onCollapseAll(); menuExpanded = false },
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        when {
            uiState.isLoading -> LoadingState(paddingValues)
            uiState.error != null -> ErrorState(uiState.error!!, paddingValues)
            uiState.proof != null -> ProofContent(
                proof = uiState.proof!!,
                expandedSteps = uiState.expandedSteps,
                currentMode = uiState.currentMode,
                onModeChanged = viewModel::onModeChanged,
                onToggleStep = viewModel::onToggleStepExpanded,
                onCopyLatex = viewModel::onCopyStepLatex,
                modifier = Modifier.padding(paddingValues),
            )
        }
    }
}

@Composable
private fun ProofContent(
    proof: Proof,
    expandedSteps: Set<Int>,
    currentMode: ProofMode,
    onModeChanged: (ProofMode) -> Unit,
    onToggleStep: (Int) -> Unit,
    onCopyLatex: (ProofStep) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        // ── Mode Selector ─────────────────────────────────────────────────────
        item {
            ProofModeSelector(
                currentMode = currentMode,
                onModeChanged = onModeChanged,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        // ── Theorem Statement ─────────────────────────────────────────────────
        item {
            TheoremStatementCard(
                title = proof.title,
                statement = proof.statement,
                assumptions = proof.metadata.assumptions,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        // ── Verification Badge ────────────────────────────────────────────────
        proof.verificationResult?.let { result ->
            item {
                VerificationBadge(
                    isValid = result.isValid,
                    message = result.message,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }

        // ── Derivation header ─────────────────────────────────────────────────
        item {
            Text(
                "Derivation",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        // ── Proof Steps ───────────────────────────────────────────────────────
        itemsIndexed(proof.steps) { _, step ->
            ProofStepCard(
                step = step,
                isExpanded = step.stepNumber in expandedSteps || step.isExpanded,
                onToggleExpand = { onToggleStep(step.stepNumber) },
                onCopyLatex = { onCopyLatex(step) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp),
            )
        }

        // ── Conclusion ────────────────────────────────────────────────────────
        item {
            ConclusionCard(
                latex = proof.conclusion,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        // ── Metadata ──────────────────────────────────────────────────────────
        item {
            ProofMetadataCard(
                proof = proof,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun ProofModeSelector(
    currentMode: ProofMode,
    onModeChanged: (ProofMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val modes = listOf(
            ProofMode.STANDARD to "Standard",
            ProofMode.BEGINNER to "Beginner",
            ProofMode.COMPACT to "Compact",
            ProofMode.EXAM to "Exam",
            ProofMode.FORMAL to "Formal",
        )
        items(modes.size) { i ->
            val (mode, label) = modes[i]
            FilterChip(
                selected = currentMode == mode,
                onClick = { onModeChanged(mode) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun TheoremStatementCard(
    title: String,
    statement: String,
    assumptions: List<String>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Theorem",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(modifier = Modifier.height(12.dp))
            KaTeXView(
                latex = statement,
                modifier = Modifier.fillMaxWidth(),
            )
            if (assumptions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Assumptions:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                assumptions.forEach { assumption ->
                    Text(
                        "• $assumption",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProofStepCard(
    step: ProofStep,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onCopyLatex: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column {
            // Step header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StepNumberBadge(step.stepNumber)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    step.justification,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(onClick = onCopyLatex, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy LaTeX",
                        modifier = Modifier.size(16.dp),
                    )
                }
                if (step.substeps.isNotEmpty()) {
                    IconButton(onClick = onToggleExpand, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            // Expression
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp),
            ) {
                KaTeXView(
                    latex = step.latex,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Explanation
            Text(
                step.explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            // Assumptions
            if (step.assumptions.isNotEmpty()) {
                step.assumptions.forEach { assumption ->
                    Text(
                        "Requires: $assumption",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                    )
                }
            }

            // Substeps
            AnimatedVisibility(
                visible = isExpanded && step.substeps.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(
                    modifier = Modifier
                        .padding(start = 24.dp, end = 8.dp, top = 4.dp, bottom = 8.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .padding(8.dp),
                ) {
                    Text(
                        "Detailed Steps:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    step.substeps.forEach { substep ->
                        SubstepRow(substep = substep)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun SubstepRow(substep: ProofStep) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                "${substep.stepNumber}.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(20.dp),
            )
            Column {
                KaTeXView(latex = substep.latex, modifier = Modifier.fillMaxWidth())
                Text(
                    substep.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StepNumberBadge(number: Int) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.size(24.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                number.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondary,
            )
        }
    }
}

@Composable
private fun VerificationBadge(isValid: Boolean, message: String, modifier: Modifier = Modifier) {
    val color = if (isValid) MaterialTheme.colorScheme.secondaryContainer
    else MaterialTheme.colorScheme.errorContainer
    val textColor = if (isValid) MaterialTheme.colorScheme.onSecondaryContainer
    else MaterialTheme.colorScheme.onErrorContainer
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = color) {
        Text(
            if (isValid) "✓ Proof structure verified" else "⚠ $message",
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            modifier = Modifier.padding(8.dp),
        )
    }
}

@Composable
private fun ConclusionCard(latex: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Conclusion",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
            )
            Spacer(modifier = Modifier.height(8.dp))
            KaTeXView(latex = latex, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ProofMetadataCard(proof: Proof, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Details", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            MetaRow("Topic", proof.metadata.topic.displayName)
            MetaRow("Subtopic", proof.metadata.subtopic)
            MetaRow("Difficulty", proof.metadata.difficulty.displayName)
            MetaRow("Steps", proof.steps.size.toString())
            if (proof.metadata.tags.isNotEmpty()) {
                MetaRow("Tags", proof.metadata.tags.joinToString(", "))
            }
            if (proof.metadata.references.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("References:", style = MaterialTheme.typography.labelMedium)
                proof.metadata.references.forEach { ref ->
                    Text("• $ref", style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Serif,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text("$label: ", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun LoadingState(paddingValues: PaddingValues) {
    Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(message: String, paddingValues: PaddingValues) {
    Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.error)
    }
}
