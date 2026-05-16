package com.statproof.proofengine.engine

import com.statproof.proofengine.latex.LaTeXGenerator
import com.statproof.proofengine.models.DerivationStepDef
import com.statproof.proofengine.models.Proof
import com.statproof.proofengine.models.ProofMetadata
import com.statproof.proofengine.models.ProofMode
import com.statproof.proofengine.models.ProofStep
import com.statproof.proofengine.models.TheoremDefinition
import com.statproof.proofengine.models.VerificationResult
import com.statproof.proofengine.verification.VerificationEngine

/**
 * Core proof generation and expansion engine.
 *
 * [ProofEngine] accepts [TheoremDefinition] objects (loaded from JSON)
 * and produces fully expanded [Proof] objects with LaTeX-rendered steps,
 * justifications, and optional substeps.
 *
 * All operations are deterministic and produce the same output
 * given the same input — no randomness, no external state.
 */
class ProofEngine {

    private val verifier = VerificationEngine()

    /**
     * Generate a complete [Proof] from a [TheoremDefinition].
     *
     * @param definition the theorem to prove
     * @param mode the presentation mode (affects verbosity and step depth)
     * @param verify whether to run symbolic verification on the proof chain
     * @return a fully populated [Proof] object ready for UI display
     */
    fun generateProof(
        definition: TheoremDefinition,
        mode: ProofMode = ProofMode.STANDARD,
        verify: Boolean = true,
    ): Proof {
        val steps = buildSteps(definition.derivationSteps, mode)
        val verificationResult = if (verify) verifier.verify(steps) else null

        return Proof(
            id = definition.id,
            title = definition.title,
            statement = definition.statement,
            conclusion = definition.conclusion,
            steps = steps,
            metadata = ProofMetadata(
                topic = definition.topic,
                subtopic = definition.subtopic,
                difficulty = definition.difficulty,
                tags = definition.tags,
                assumptions = definition.assumptions,
                references = definition.references,
                relatedProofIds = definition.relatedIds,
                searchKeywords = definition.searchKeywords,
            ),
            mode = mode,
            verificationResult = verificationResult,
        )
    }

    /**
     * Expand a single step into its substeps for the "more detail" feature.
     *
     * @param proof the parent proof
     * @param stepNumber the 1-based step number to expand
     * @param expansionDepth how many additional levels of detail to add
     * @return a new [Proof] with the specified step expanded
     */
    fun expandStep(
        proof: Proof,
        stepNumber: Int,
        expansionDepth: Int = 1,
    ): Proof {
        val updatedSteps = proof.steps.map { step ->
            if (step.stepNumber == stepNumber && step.substeps.isNotEmpty()) {
                step.copy(isExpanded = true)
            } else {
                step
            }
        }
        return proof.copy(steps = updatedSteps)
    }

    /**
     * Collapse a previously expanded step.
     */
    fun collapseStep(proof: Proof, stepNumber: Int): Proof {
        val updatedSteps = proof.steps.map { step ->
            if (step.stepNumber == stepNumber) step.copy(isExpanded = false)
            else step
        }
        return proof.copy(steps = updatedSteps)
    }

    /**
     * Convert a proof to a different [ProofMode].
     *
     * Beginner mode: all substeps expanded, extra hints shown
     * Compact mode:  only top-level steps shown, no substeps
     * Exam mode:     steps shown without extensive explanations
     * Formal mode:   all assumptions stated, maximum rigor
     */
    fun convertMode(proof: Proof, newMode: ProofMode): Proof {
        val adjustedSteps = when (newMode) {
            ProofMode.BEGINNER -> proof.steps.map { it.copy(isExpanded = it.substeps.isNotEmpty()) }
            ProofMode.COMPACT -> proof.steps.map { it.copy(isExpanded = false) }
            ProofMode.EXAM -> proof.steps.map { it.copy(isExpanded = false) }
            ProofMode.FORMAL -> proof.steps.map { it.copy(isExpanded = it.substeps.isNotEmpty()) }
            else -> proof.steps
        }
        return proof.copy(steps = adjustedSteps, mode = newMode)
    }

    /**
     * Generate a brief "why this step" explanation for the given step.
     *
     * Returns additional pedagogical context beyond the standard justification.
     */
    fun explainStep(step: ProofStep): String {
        return buildString {
            appendLine("**Rule Applied:** ${step.justification}")
            appendLine()
            appendLine(step.explanation)
            if (step.assumptions.isNotEmpty()) {
                appendLine()
                appendLine("**Assumptions Required:**")
                step.assumptions.forEach { assumption ->
                    appendLine("• $assumption")
                }
            }
            if (step.hints.isNotEmpty()) {
                appendLine()
                appendLine("**Hints:**")
                step.hints.forEach { hint ->
                    appendLine("• $hint")
                }
            }
        }.trim()
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private fun buildSteps(
        defs: List<DerivationStepDef>,
        mode: ProofMode,
    ): List<ProofStep> {
        return defs.mapIndexed { idx, def ->
            val substeps = if (def.substeps.isNotEmpty()) {
                buildSteps(def.substeps, mode)
            } else {
                emptyList()
            }

            val autoExpand = when (mode) {
                ProofMode.BEGINNER -> substeps.isNotEmpty()
                ProofMode.FORMAL -> substeps.isNotEmpty()
                else -> false
            }

            ProofStep(
                stepNumber = def.stepNumber,
                latex = adjustLatexForMode(def.expression, mode),
                justification = def.justification,
                explanation = buildExplanation(def, mode),
                ruleId = def.ruleId,
                substeps = substeps,
                isExpanded = autoExpand,
                expressionBefore = if (idx > 0) defs[idx - 1].expression else "",
                expressionAfter = def.expression,
                assumptions = def.assumptions,
                hints = if (mode == ProofMode.BEGINNER) def.hints else emptyList(),
            )
        }
    }

    private fun adjustLatexForMode(latex: String, mode: ProofMode): String {
        // In exam mode, wrap in display style
        return when (mode) {
            ProofMode.EXAM -> latex
            else -> latex
        }
    }

    private fun buildExplanation(def: DerivationStepDef, mode: ProofMode): String {
        return when (mode) {
            ProofMode.BEGINNER -> "${def.explanation}\n\nThis follows from the rule: ${def.justification}."
            ProofMode.COMPACT -> def.justification
            ProofMode.FORMAL -> "${def.explanation} [${def.ruleId}]"
            else -> def.explanation
        }
    }
}
