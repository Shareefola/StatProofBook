package com.statproof.proofengine.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Complete proof object returned by the proof engine.
 *
 * A [Proof] is the full derivation from statement to conclusion,
 * broken into [ProofStep] instances. Each step represents one
 * algebraic transformation with its justification.
 */
@Serializable
data class Proof(
    val id: String,
    val title: String,
    val statement: String,                          // LaTeX statement of the theorem
    val conclusion: String,                         // LaTeX final result
    val steps: List<ProofStep>,
    val metadata: ProofMetadata,
    val mode: ProofMode = ProofMode.STANDARD,
    val verificationResult: VerificationResult? = null,
)

/**
 * A single step in a proof derivation.
 *
 * Steps form a linear chain: each step transforms an expression using
 * a named rule, producing the expression for the next step.
 */
@Serializable
data class ProofStep(
    val stepNumber: Int,
    val latex: String,                              // LaTeX rendering of this step's expression
    val justification: String,                      // Human-readable rule name
    val explanation: String,                        // Longer explanation of why this rule applies
    val ruleId: String,                             // Machine-readable rule ID for the rule engine
    val substeps: List<ProofStep> = emptyList(),    // Optional detailed substeps
    val isExpanded: Boolean = false,                // UI state: whether substeps shown
    val expressionBefore: String = "",              // LaTeX of expression BEFORE this step
    val expressionAfter: String = "",               // LaTeX of expression AFTER this step
    val assumptions: List<String> = emptyList(),    // Assumptions required for this step
    val hints: List<String> = emptyList(),          // Pedagogical hints
)

/**
 * Metadata attached to each proof.
 */
@Serializable
data class ProofMetadata(
    val topic: Topic,
    val subtopic: String,
    val difficulty: Difficulty,
    val tags: List<String>,
    val assumptions: List<String>,
    val references: List<String> = emptyList(),
    val relatedProofIds: List<String> = emptyList(),
    val searchKeywords: List<String> = emptyList(),
    val author: String = "StatProof",
    val version: Int = 1,
)

/**
 * Result of symbolic/numeric verification of a proof.
 */
@Serializable
data class VerificationResult(
    val isValid: Boolean,
    val failedAtStep: Int? = null,                  // null if all steps pass
    val message: String = "",
    val numericCheckPassed: Boolean? = null,
)

/**
 * High-level topic classification for theorems.
 */
@Serializable
enum class Topic(val displayName: String, val latexSymbol: String = "") {
    @SerialName("probability_theory")
    PROBABILITY_THEORY("Probability Theory", "P"),

    @SerialName("distribution_theory")
    DISTRIBUTION_THEORY("Distribution Theory", "f"),

    @SerialName("statistical_inference")
    STATISTICAL_INFERENCE("Statistical Inference", "\\hat{\\theta}"),

    @SerialName("hypothesis_testing")
    HYPOTHESIS_TESTING("Hypothesis Testing", "H_0"),

    @SerialName("regression")
    REGRESSION("Regression Models", "\\hat{\\beta}"),

    @SerialName("bayesian")
    BAYESIAN("Bayesian Statistics", "\\pi"),

    @SerialName("sampling_theory")
    SAMPLING_THEORY("Sampling Theory", "\\bar{X}"),

    @SerialName("stochastic_processes")
    STOCHASTIC_PROCESSES("Stochastic Processes", "\\{X_t\\}"),

    @SerialName("information_theory")
    INFORMATION_THEORY("Information Theory", "H"),

    @SerialName("asymptotics")
    ASYMPTOTICS("Asymptotics", "n \\to \\infty"),
}

/**
 * Proof difficulty levels.
 */
@Serializable
enum class Difficulty(val displayName: String, val stars: Int) {
    @SerialName("elementary")
    ELEMENTARY("Elementary", 1),

    @SerialName("undergraduate")
    UNDERGRADUATE("Undergraduate", 2),

    @SerialName("advanced_undergraduate")
    ADVANCED_UNDERGRADUATE("Advanced Undergraduate", 3),

    @SerialName("graduate")
    GRADUATE("Graduate", 4),

    @SerialName("research")
    RESEARCH("Research Level", 5),
}

/**
 * Proof presentation modes.
 */
@Serializable
enum class ProofMode {
    @SerialName("standard")
    STANDARD,               // Normal academic presentation

    @SerialName("beginner")
    BEGINNER,               // Extra explanations, every step expanded

    @SerialName("compact")
    COMPACT,                // Minimal steps, high-level only

    @SerialName("exam")
    EXAM,                   // Clean, exam-ready format

    @SerialName("intuition")
    INTUITION,              // Intuition-first, less formal

    @SerialName("formal")
    FORMAL,                 // Fully formal with all assumptions stated
}

/**
 * A theorem entry in the static knowledge database.
 * Distinct from [Proof] — this is the raw definition loaded from JSON.
 */
@Serializable
data class TheoremDefinition(
    val id: String,
    val title: String,
    val statement: String,                          // LaTeX statement
    val topic: Topic,
    val subtopic: String,
    val difficulty: Difficulty,
    val tags: List<String>,
    val assumptions: List<String>,
    val derivationSteps: List<DerivationStepDef>,
    val conclusion: String,                         // LaTeX conclusion
    val references: List<String> = emptyList(),
    val relatedIds: List<String> = emptyList(),
    val searchKeywords: List<String> = emptyList(),
    val intuition: String = "",                     // Plain-language intuition
    val pitfalls: List<String> = emptyList(),
    val examples: List<ExampleDef> = emptyList(),
)

/**
 * A derivation step as stored in JSON.
 */
@Serializable
data class DerivationStepDef(
    val stepNumber: Int,
    val expression: String,                         // LaTeX of the expression at this step
    val justification: String,
    val explanation: String,
    val ruleId: String,
    val substeps: List<DerivationStepDef> = emptyList(),
    val assumptions: List<String> = emptyList(),
    val hints: List<String> = emptyList(),
)

/**
 * A worked example attached to a theorem.
 */
@Serializable
data class ExampleDef(
    val title: String,
    val setup: String,                              // LaTeX or plain text
    val solution: String,                           // LaTeX or plain text
    val distribution: String = "",
)

/**
 * Search result entry.
 */
data class SearchResult(
    val theoremId: String,
    val title: String,
    val topic: Topic,
    val difficulty: Difficulty,
    val snippet: String,                            // Short preview text
    val score: Float,
    val tags: List<String>,
)

/**
 * Proof expansion request — user asks for more detail on a step.
 */
data class ExpansionRequest(
    val proofId: String,
    val stepNumber: Int,
    val expansionType: ExpansionType,
)

enum class ExpansionType {
    MORE_DETAIL,
    WHY_THIS_STEP,
    FORMAL_JUSTIFICATION,
    NUMERIC_VERIFICATION,
    RELATED_THEOREMS,
}
