package com.statproof.app.ui.viewer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.statproof.app.navigation.Screen
import com.statproof.domain.usecase.GenerateProofUseCase
import com.statproof.domain.usecase.RecordProofViewUseCase
import com.statproof.domain.usecase.ToggleFavoriteUseCase
import com.statproof.domain.usecase.CheckFavoriteUseCase
import com.statproof.proofengine.engine.ProofEngine
import com.statproof.proofengine.models.Proof
import com.statproof.proofengine.models.ProofMode
import com.statproof.proofengine.models.ProofStep
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProofViewerUiState(
    val isLoading: Boolean = true,
    val proof: Proof? = null,
    val isFavorite: Boolean = false,
    val currentMode: ProofMode = ProofMode.STANDARD,
    val expandedSteps: Set<Int> = emptySet(),
    val error: String? = null,
    val copiedLatex: String? = null,
)

@HiltViewModel
class ProofViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val generateProofUseCase: GenerateProofUseCase,
    private val recordProofViewUseCase: RecordProofViewUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val checkFavoriteUseCase: CheckFavoriteUseCase,
) : ViewModel() {

    private val proofId: String = requireNotNull(savedStateHandle[Screen.ProofViewer.ARG_PROOF_ID])
    private val proofEngine = ProofEngine()

    private val _uiState = MutableStateFlow(ProofViewerUiState())
    val uiState: StateFlow<ProofViewerUiState> = _uiState.asStateFlow()

    init {
        loadProof()
    }

    private fun loadProof(mode: ProofMode = ProofMode.STANDARD) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val proof = generateProofUseCase(proofId, mode)
            val isFavorite = checkFavoriteUseCase(proofId)
            if (proof != null) {
                _uiState.update { it.copy(isLoading = false, proof = proof, isFavorite = isFavorite, currentMode = mode) }
                recordProofViewUseCase(proofId)
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Proof not found: $proofId") }
            }
        }
    }

    fun onModeChanged(mode: ProofMode) {
        val current = _uiState.value.proof ?: return
        val converted = proofEngine.convertMode(current, mode)
        _uiState.update { it.copy(proof = converted, currentMode = mode) }
    }

    fun onToggleStepExpanded(stepNumber: Int) {
        _uiState.update { state ->
            val expanded = state.expandedSteps.toMutableSet()
            if (stepNumber in expanded) expanded.remove(stepNumber) else expanded.add(stepNumber)
            state.copy(expandedSteps = expanded)
        }
    }

    fun onToggleFavorite() {
        viewModelScope.launch {
            val newState = toggleFavoriteUseCase(proofId)
            _uiState.update { it.copy(isFavorite = newState) }
        }
    }

    fun onCopyStepLatex(step: ProofStep) {
        _uiState.update { it.copy(copiedLatex = step.latex) }
    }

    fun onExpandAll() {
        val proof = _uiState.value.proof ?: return
        val allStepNumbers = proof.steps.map { it.stepNumber }.toSet()
        _uiState.update { it.copy(expandedSteps = allStepNumbers) }
    }

    fun onCollapseAll() {
        _uiState.update { it.copy(expandedSteps = emptySet()) }
    }
}
