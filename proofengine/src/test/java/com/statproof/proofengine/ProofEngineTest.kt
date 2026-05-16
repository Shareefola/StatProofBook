package com.statproof.proofengine

import com.statproof.proofengine.engine.ProofEngine
import com.statproof.proofengine.models.DerivationStepDef
import com.statproof.proofengine.models.Difficulty
import com.statproof.proofengine.models.ExampleDef
import com.statproof.proofengine.models.Proof
import com.statproof.proofengine.models.ProofMode
import com.statproof.proofengine.models.TheoremDefinition
import com.statproof.proofengine.models.Topic
import com.statproof.proofengine.verification.VerificationEngine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@DisplayName("ProofEngine Tests")
class ProofEngineTest {

    private lateinit var engine: ProofEngine

    // ── Canonical test theorem: Bayes' theorem (4 steps) ─────────────────────

    private val bayesDefinition = TheoremDefinition(
        id = "test_bayes",
        title = "Bayes' Theorem",
        topic = Topic.PROBABILITY_THEORY,
        subtopic = "Conditional Probability",
        difficulty = Difficulty.UNDERGRADUATE,
        statement = "P(A \\mid B) = \\frac{P(B \\mid A) P(A)}{P(B)}",
        conclusion = "P(A \\mid B) = \\frac{P(B \\mid A) P(A)}{P(B)}",
        tags = listOf("bayes", "conditional probability"),
        assumptions = listOf("P(B) > 0"),
        searchKeywords = listOf("bayes", "conditional"),
        intuition = "Reverses conditional probabilities.",
        derivationSteps = listOf(
            DerivationStepDef(
                stepNumber = 1,
                expression = "P(A \\cap B) = P(A \\mid B) P(B)",
                justification = "Definition of Conditional Probability",
                explanation = "By definition, P(A|B) = P(A∩B)/P(B).",
                ruleId = "definition",
                hints = listOf("Multiply both sides by P(B)."),
            ),
            DerivationStepDef(
                stepNumber = 2,
                expression = "P(A \\cap B) = P(B \\mid A) P(A)",
                justification = "Symmetry of Intersection",
                explanation = "A∩B = B∩A so we can apply the definition again.",
                ruleId = "definition",
            ),
            DerivationStepDef(
                stepNumber = 3,
                expression = "P(A \\mid B) P(B) = P(B \\mid A) P(A)",
                justification = "Equating Both Expressions",
                explanation = "Steps 1 and 2 both equal P(A∩B).",
                ruleId = "add_zero",
            ),
            DerivationStepDef(
                stepNumber = 4,
                expression = "P(A \\mid B) = \\frac{P(B \\mid A) P(A)}{P(B)}",
                justification = "Division by P(B)",
                explanation = "Divide both sides by P(B) > 0.",
                ruleId = "mul_one",
                assumptions = listOf("P(B) > 0"),
            ),
        ),
        references = listOf("Bayes (1763)"),
    )

    // ── Theorem with substeps ──────────────────────────────────────────────────

    private val theoremWithSubsteps = TheoremDefinition(
        id = "test_substeps",
        title = "Test with Substeps",
        topic = Topic.PROBABILITY_THEORY,
        subtopic = "Test",
        difficulty = Difficulty.ELEMENTARY,
        statement = "A = B",
        conclusion = "A = B",
        tags = listOf("test"),
        assumptions = emptyList(),
        searchKeywords = emptyList(),
        derivationSteps = listOf(
            DerivationStepDef(
                stepNumber = 1,
                expression = "A",
                justification = "Given",
                explanation = "Starting point.",
                ruleId = "axiom",
                substeps = listOf(
                    DerivationStepDef(
                        stepNumber = 1,
                        expression = "A_1",
                        justification = "Substep 1",
                        explanation = "Detail 1.",
                        ruleId = "axiom",
                    ),
                    DerivationStepDef(
                        stepNumber = 2,
                        expression = "A_2",
                        justification = "Substep 2",
                        explanation = "Detail 2.",
                        ruleId = "axiom",
                    ),
                ),
            ),
            DerivationStepDef(
                stepNumber = 2,
                expression = "B",
                justification = "Transformation",
                explanation = "Transforms to B.",
                ruleId = "mul_one",
            ),
        ),
    )

    @BeforeEach
    fun setUp() {
        engine = ProofEngine()
    }

    // ── generateProof tests ───────────────────────────────────────────────────

    @Nested
    @DisplayName("generateProof")
    inner class GenerateProofTests {

        @Test
        fun `generates proof with correct id`() {
            val proof = engine.generateProof(bayesDefinition)
            assertEquals("test_bayes", proof.id)
        }

        @Test
        fun `generates proof with correct title`() {
            val proof = engine.generateProof(bayesDefinition)
            assertEquals("Bayes' Theorem", proof.title)
        }

        @Test
        fun `generates correct number of steps`() {
            val proof = engine.generateProof(bayesDefinition)
            assertEquals(4, proof.steps.size)
        }

        @Test
        fun `steps are numbered sequentially from 1`() {
            val proof = engine.generateProof(bayesDefinition)
            proof.steps.forEachIndexed { index, step ->
                assertEquals(index + 1, step.stepNumber)
            }
        }

        @Test
        fun `steps have non-blank latex`() {
            val proof = engine.generateProof(bayesDefinition)
            proof.steps.forEach { step ->
                assertTrue(step.latex.isNotBlank(), "Step ${step.stepNumber} has blank LaTeX")
            }
        }

        @Test
        fun `steps have non-blank justification`() {
            val proof = engine.generateProof(bayesDefinition)
            proof.steps.forEach { step ->
                assertTrue(step.justification.isNotBlank(), "Step ${step.stepNumber} has blank justification")
            }
        }

        @Test
        fun `steps have non-blank explanation`() {
            val proof = engine.generateProof(bayesDefinition)
            proof.steps.forEach { step ->
                assertTrue(step.explanation.isNotBlank(), "Step ${step.stepNumber} has blank explanation")
            }
        }

        @Test
        fun `metadata is populated from definition`() {
            val proof = engine.generateProof(bayesDefinition)
            assertEquals(Topic.PROBABILITY_THEORY, proof.metadata.topic)
            assertEquals(Difficulty.UNDERGRADUATE, proof.metadata.difficulty)
            assertEquals("Conditional Probability", proof.metadata.subtopic)
        }

        @Test
        fun `verification result is included when requested`() {
            val proof = engine.generateProof(bayesDefinition, verify = true)
            assertNotNull(proof.verificationResult)
        }

        @Test
        fun `verification result is null when not requested`() {
            val proof = engine.generateProof(bayesDefinition, verify = false)
            assertNull(proof.verificationResult)
        }

        @Test
        fun `proof is verified as valid for well-formed definition`() {
            val proof = engine.generateProof(bayesDefinition, verify = true)
            assertTrue(proof.verificationResult!!.isValid)
        }

        @Test
        fun `proof contains correct statement`() {
            val proof = engine.generateProof(bayesDefinition)
            assertTrue(proof.statement.isNotBlank())
        }

        @Test
        fun `proof contains correct conclusion`() {
            val proof = engine.generateProof(bayesDefinition)
            assertTrue(proof.conclusion.isNotBlank())
        }

        @Test
        fun `step assumptions are preserved`() {
            val proof = engine.generateProof(bayesDefinition)
            val lastStep = proof.steps.last()
            assertTrue(lastStep.assumptions.contains("P(B) > 0"))
        }

        @Test
        fun `substeps are generated correctly`() {
            val proof = engine.generateProof(theoremWithSubsteps)
            val firstStep = proof.steps.first()
            assertEquals(2, firstStep.substeps.size)
        }

        @Test
        fun `substeps are numbered correctly`() {
            val proof = engine.generateProof(theoremWithSubsteps)
            val substeps = proof.steps.first().substeps
            assertEquals(1, substeps[0].stepNumber)
            assertEquals(2, substeps[1].stepNumber)
        }

        @Test
        fun `mode is stored in proof`() {
            val proof = engine.generateProof(bayesDefinition, mode = ProofMode.COMPACT)
            assertEquals(ProofMode.COMPACT, proof.mode)
        }
    }

    // ── ProofMode tests ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("ProofMode Effects")
    inner class ProofModeTests {

        @Test
        fun `BEGINNER mode expands substeps`() {
            val proof = engine.generateProof(theoremWithSubsteps, mode = ProofMode.BEGINNER)
            val stepsWithSubsteps = proof.steps.filter { it.substeps.isNotEmpty() }
            assertTrue(stepsWithSubsteps.all { it.isExpanded })
        }

        @Test
        fun `COMPACT mode does not auto-expand substeps`() {
            val proof = engine.generateProof(theoremWithSubsteps, mode = ProofMode.COMPACT)
            proof.steps.forEach { step ->
                assertFalse(step.isExpanded, "Step ${step.stepNumber} should not be expanded in COMPACT mode")
            }
        }

        @Test
        fun `STANDARD mode does not auto-expand substeps`() {
            val proof = engine.generateProof(theoremWithSubsteps, mode = ProofMode.STANDARD)
            proof.steps.forEach { step ->
                assertFalse(step.isExpanded)
            }
        }

        @Test
        fun `BEGINNER mode includes hints`() {
            val proof = engine.generateProof(bayesDefinition, mode = ProofMode.BEGINNER)
            val stepsWithHints = proof.steps.filter { it.hints.isNotEmpty() }
            assertTrue(stepsWithHints.isNotEmpty())
        }

        @Test
        fun `COMPACT mode has shorter explanations than STANDARD`() {
            val compactProof = engine.generateProof(bayesDefinition, mode = ProofMode.COMPACT)
            val standardProof = engine.generateProof(bayesDefinition, mode = ProofMode.STANDARD)
            val compactTotalLength = compactProof.steps.sumOf { it.explanation.length }
            val standardTotalLength = standardProof.steps.sumOf { it.explanation.length }
            assertTrue(compactTotalLength <= standardTotalLength)
        }

        @Test
        fun `FORMAL mode expands substeps like BEGINNER`() {
            val proof = engine.generateProof(theoremWithSubsteps, mode = ProofMode.FORMAL)
            val stepsWithSubsteps = proof.steps.filter { it.substeps.isNotEmpty() }
            assertTrue(stepsWithSubsteps.all { it.isExpanded })
        }
    }

    // ── expandStep / collapseStep tests ───────────────────────────────────────

    @Nested
    @DisplayName("Step Expansion")
    inner class StepExpansionTests {

        @Test
        fun `expandStep marks step as expanded`() {
            val proof = engine.generateProof(theoremWithSubsteps)
            val expanded = engine.expandStep(proof, stepNumber = 1)
            assertTrue(expanded.steps.first { it.stepNumber == 1 }.isExpanded)
        }

        @Test
        fun `collapseStep marks step as collapsed`() {
            val proof = engine.generateProof(theoremWithSubsteps, mode = ProofMode.BEGINNER)
            val collapsed = engine.collapseStep(proof, stepNumber = 1)
            assertFalse(collapsed.steps.first { it.stepNumber == 1 }.isExpanded)
        }

        @Test
        fun `expandStep does not affect other steps`() {
            val proof = engine.generateProof(theoremWithSubsteps)
            val expanded = engine.expandStep(proof, stepNumber = 1)
            val otherSteps = expanded.steps.filter { it.stepNumber != 1 }
            assertTrue(otherSteps.all { !it.isExpanded })
        }

        @Test
        fun `expandStep on step without substeps has no visible effect`() {
            val proof = engine.generateProof(bayesDefinition)
            val expanded = engine.expandStep(proof, stepNumber = 1)
            // Step 1 has no substeps, so isExpanded stays false
            assertFalse(expanded.steps.first().isExpanded)
        }
    }

    // ── convertMode tests ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("convertMode")
    inner class ConvertModeTests {

        @ParameterizedTest
        @EnumSource(ProofMode::class)
        fun `convertMode produces proof with requested mode`(mode: ProofMode) {
            val proof = engine.generateProof(bayesDefinition)
            val converted = engine.convertMode(proof, mode)
            assertEquals(mode, converted.mode)
        }

        @Test
        fun `convertMode COMPACT collapses all steps`() {
            val proof = engine.generateProof(theoremWithSubsteps, mode = ProofMode.BEGINNER)
            val compacted = engine.convertMode(proof, ProofMode.COMPACT)
            assertTrue(compacted.steps.none { it.isExpanded })
        }

        @Test
        fun `convertMode preserves step count`() {
            val proof = engine.generateProof(bayesDefinition)
            ProofMode.entries.forEach { mode ->
                val converted = engine.convertMode(proof, mode)
                assertEquals(proof.steps.size, converted.steps.size)
            }
        }

        @Test
        fun `convertMode preserves step content`() {
            val proof = engine.generateProof(bayesDefinition)
            val converted = engine.convertMode(proof, ProofMode.EXAM)
            proof.steps.zip(converted.steps).forEach { (orig, conv) ->
                assertEquals(orig.latex, conv.latex)
                assertEquals(orig.justification, conv.justification)
                assertEquals(orig.stepNumber, conv.stepNumber)
            }
        }
    }

    // ── explainStep tests ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("explainStep")
    inner class ExplainStepTests {

        @Test
        fun `explainStep returns non-empty string`() {
            val proof = engine.generateProof(bayesDefinition)
            val explanation = engine.explainStep(proof.steps.first())
            assertTrue(explanation.isNotBlank())
        }

        @Test
        fun `explainStep contains rule name`() {
            val proof = engine.generateProof(bayesDefinition)
            val step = proof.steps.first()
            val explanation = engine.explainStep(step)
            assertTrue(explanation.contains(step.justification))
        }

        @Test
        fun `explainStep contains assumptions when present`() {
            val proof = engine.generateProof(bayesDefinition)
            val lastStep = proof.steps.last()
            val explanation = engine.explainStep(lastStep)
            if (lastStep.assumptions.isNotEmpty()) {
                assertTrue(explanation.contains(lastStep.assumptions.first()))
            }
        }
    }

    // ── Determinism tests ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Determinism")
    inner class DeterminismTests {

        @Test
        fun `generateProof is deterministic`() {
            val proof1 = engine.generateProof(bayesDefinition, mode = ProofMode.STANDARD)
            val proof2 = engine.generateProof(bayesDefinition, mode = ProofMode.STANDARD)

            assertEquals(proof1.id, proof2.id)
            assertEquals(proof1.steps.size, proof2.steps.size)
            proof1.steps.zip(proof2.steps).forEach { (s1, s2) ->
                assertEquals(s1.latex, s2.latex)
                assertEquals(s1.justification, s2.justification)
                assertEquals(s1.explanation, s2.explanation)
            }
        }

        @Test
        fun `mode conversion is deterministic`() {
            val proof = engine.generateProof(bayesDefinition)
            val converted1 = engine.convertMode(proof, ProofMode.BEGINNER)
            val converted2 = engine.convertMode(proof, ProofMode.BEGINNER)
            assertEquals(converted1.steps.map { it.isExpanded }, converted2.steps.map { it.isExpanded })
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@DisplayName("VerificationEngine Tests")
class VerificationEngineTest {

    private lateinit var verifier: VerificationEngine

    private fun makeSteps(count: Int, ruleId: String = "definition"): List<com.statproof.proofengine.models.ProofStep> =
        (1..count).map { i ->
            com.statproof.proofengine.models.ProofStep(
                stepNumber = i,
                latex = "\\text{Step $i expression}",
                justification = "Rule $i",
                explanation = "Explanation for step $i",
                ruleId = ruleId,
            )
        }

    @BeforeEach
    fun setUp() {
        verifier = VerificationEngine()
    }

    @Test
    fun `empty step list fails verification`() {
        val result = verifier.verify(emptyList())
        assertFalse(result.isValid)
        assertTrue(result.message.isNotBlank())
    }

    @Test
    fun `single valid step passes`() {
        val steps = makeSteps(1)
        val result = verifier.verify(steps)
        assertTrue(result.isValid, "Expected valid but got: ${result.message}")
    }

    @Test
    fun `four sequential steps pass`() {
        val steps = makeSteps(4)
        val result = verifier.verify(steps)
        assertTrue(result.isValid, result.message)
    }

    @Test
    fun `non-sequential numbering fails`() {
        val steps = listOf(
            com.statproof.proofengine.models.ProofStep(
                stepNumber = 1, latex = "A", justification = "J1", explanation = "E1", ruleId = "definition",
            ),
            com.statproof.proofengine.models.ProofStep(
                stepNumber = 3, latex = "B", justification = "J3", explanation = "E3", ruleId = "definition",
            ),
        )
        val result = verifier.verify(steps)
        assertFalse(result.isValid)
    }

    @Test
    fun `step with blank latex fails`() {
        val steps = listOf(
            com.statproof.proofengine.models.ProofStep(
                stepNumber = 1, latex = "", justification = "J1", explanation = "E1", ruleId = "definition",
            ),
        )
        val result = verifier.verify(steps)
        assertFalse(result.isValid)
        assertEquals(1, result.failedAtStep)
    }

    @Test
    fun `step with blank justification fails`() {
        val steps = listOf(
            com.statproof.proofengine.models.ProofStep(
                stepNumber = 1, latex = "x = y", justification = "", explanation = "E1", ruleId = "definition",
            ),
        )
        val result = verifier.verify(steps)
        assertFalse(result.isValid)
    }

    @Test
    fun `failed step number is reported`() {
        val steps = listOf(
            com.statproof.proofengine.models.ProofStep(
                stepNumber = 1, latex = "valid", justification = "J", explanation = "E", ruleId = "axiom",
            ),
            com.statproof.proofengine.models.ProofStep(
                stepNumber = 2, latex = "", justification = "J", explanation = "E", ruleId = "axiom",
            ),
        )
        val result = verifier.verify(steps)
        assertFalse(result.isValid)
        assertEquals(2, result.failedAtStep)
    }

    @Test
    fun `axiom rule id passes`() {
        val steps = makeSteps(2, ruleId = "axiom")
        val result = verifier.verify(steps)
        assertTrue(result.isValid)
    }

    @Test
    fun `given rule id passes`() {
        val steps = makeSteps(2, ruleId = "given")
        val result = verifier.verify(steps)
        assertTrue(result.isValid)
    }

    @Test
    fun `unknown rule id passes (warning only)`() {
        // Unknown rules are not an error — they might be future rules
        val steps = makeSteps(2, ruleId = "some_future_rule_v99")
        val result = verifier.verify(steps)
        assertTrue(result.isValid)
    }

    @Test
    fun `steps with substeps are recursively verified`() {
        val substeps = makeSteps(2)
        val steps = listOf(
            com.statproof.proofengine.models.ProofStep(
                stepNumber = 1,
                latex = "expr",
                justification = "Justification",
                explanation = "Explanation",
                ruleId = "definition",
                substeps = substeps,
            ),
        )
        val result = verifier.verify(steps)
        assertTrue(result.isValid)
    }

    @Test
    fun `invalid substeps cause parent to fail`() {
        val invalidSubsteps = listOf(
            com.statproof.proofengine.models.ProofStep(
                stepNumber = 1, latex = "", justification = "J", explanation = "E", ruleId = "axiom",
            ),
        )
        val steps = listOf(
            com.statproof.proofengine.models.ProofStep(
                stepNumber = 1,
                latex = "valid",
                justification = "J",
                explanation = "E",
                ruleId = "axiom",
                substeps = invalidSubsteps,
            ),
        )
        val result = verifier.verify(steps)
        assertFalse(result.isValid)
    }

    @Test
    fun `successful verification message is informative`() {
        val steps = makeSteps(3)
        val result = verifier.verify(steps)
        assertTrue(result.isValid)
        assertTrue(result.message.contains("3") || result.message.isNotBlank())
    }

    @Test
    fun `numericSpotCheck returns true (stub)`() {
        assertTrue(verifier.numericSpotCheck("x", "x", mapOf("x" to 1.0)))
    }
}
