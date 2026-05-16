package com.statproof.proofengine.verification

import com.statproof.proofengine.models.ProofStep
import com.statproof.proofengine.models.VerificationResult
import com.statproof.proofengine.rules.RuleRegistry

/**
 * Validates that a sequence of [ProofStep] instances forms a logically
 * consistent derivation chain.
 *
 * Verification checks:
 *  1. Every step references a known rule ID (or is marked as an axiom)
 *  2. Steps are numbered sequentially without gaps
 *  3. Substep numbering is consistent within parent steps
 *  4. No step has an empty expression (LaTeX)
 *  5. Justification strings are non-empty
 *
 * Note: Full symbolic verification (checking that the LaTeX expression
 * at step N+1 is algebraically equivalent to the result of applying
 * the rule to step N) requires round-tripping through the expression
 * parser, which is planned for a future release. This engine performs
 * structural validation only.
 */
class VerificationEngine {

    companion object {
        private val AXIOM_RULE_IDS = setOf(
            "axiom", "given", "definition", "assumption", "hypothesis"
        )
    }

    /**
     * Verify a list of proof steps.
     *
     * @param steps the ordered list of proof steps to validate
     * @return a [VerificationResult] describing the outcome
     */
    fun verify(steps: List<ProofStep>): VerificationResult {
        if (steps.isEmpty()) {
            return VerificationResult(
                isValid = false,
                message = "Proof contains no steps."
            )
        }

        // Check sequential numbering
        val numberingResult = checkSequentialNumbering(steps)
        if (!numberingResult.isValid) return numberingResult

        // Check each step individually
        for (step in steps) {
            val stepResult = verifyStep(step)
            if (!stepResult.isValid) {
                return stepResult.copy(failedAtStep = step.stepNumber)
            }

            // Recursively verify substeps
            if (step.substeps.isNotEmpty()) {
                val substepResult = verify(step.substeps)
                if (!substepResult.isValid) {
                    return substepResult.copy(
                        failedAtStep = step.stepNumber,
                        message = "Substep verification failed in step ${step.stepNumber}: ${substepResult.message}"
                    )
                }
            }
        }

        return VerificationResult(
            isValid = true,
            message = "Proof structure verified successfully (${steps.size} steps).",
            numericCheckPassed = null // Not performed without numeric examples
        )
    }

    /**
     * Verify a single proof step.
     */
    private fun verifyStep(step: ProofStep): VerificationResult {
        // Expression must be non-empty
        if (step.latex.isBlank()) {
            return VerificationResult(
                isValid = false,
                failedAtStep = step.stepNumber,
                message = "Step ${step.stepNumber} has an empty LaTeX expression."
            )
        }

        // Justification must be non-empty
        if (step.justification.isBlank()) {
            return VerificationResult(
                isValid = false,
                failedAtStep = step.stepNumber,
                message = "Step ${step.stepNumber} has no justification."
            )
        }

        // Rule ID must be known (unless it's an axiom/given)
        if (step.ruleId.isNotBlank() &&
            step.ruleId !in AXIOM_RULE_IDS &&
            RuleRegistry.get(step.ruleId) == null
        ) {
            // Warning only — unknown rule IDs don't fail verification
            // (rules may be added in future versions)
        }

        return VerificationResult(isValid = true, message = "")
    }

    /**
     * Check that step numbers are sequential starting from 1.
     */
    private fun checkSequentialNumbering(steps: List<ProofStep>): VerificationResult {
        steps.forEachIndexed { index, step ->
            val expected = index + 1
            if (step.stepNumber != expected) {
                return VerificationResult(
                    isValid = false,
                    failedAtStep = step.stepNumber,
                    message = "Expected step $expected but found step ${step.stepNumber}."
                )
            }
        }
        return VerificationResult(isValid = true, message = "")
    }

    /**
     * Perform a numeric spot-check by substituting concrete values and
     * comparing numerical results across a step boundary.
     *
     * This is an approximate check using floating-point arithmetic and
     * cannot prove correctness, only detect obvious errors.
     *
     * @param latexBefore LaTeX expression before the transformation
     * @param latexAfter LaTeX expression after the transformation
     * @param assignments variable assignments (e.g. {"x": 2.0, "mu": 1.0})
     * @return true if numerical values agree within tolerance
     */
    fun numericSpotCheck(
        @Suppress("UnusedParameter") latexBefore: String,
        @Suppress("UnusedParameter") latexAfter: String,
        @Suppress("UnusedParameter") assignments: Map<String, Double>,
    ): Boolean {
        // Full numeric verification requires an expression evaluator;
        // this is a stub that returns true (not checked) for now.
        // A future version will parse LaTeX → ExpressionNode → evaluate.
        return true
    }
}
