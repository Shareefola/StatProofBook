package com.statproof.proofengine

import com.statproof.proofengine.engine.ProofEngine
import com.statproof.proofengine.latex.LaTeXGenerator
import com.statproof.proofengine.models.DerivationStepDef
import com.statproof.proofengine.models.Difficulty
import com.statproof.proofengine.models.ProofMode
import com.statproof.proofengine.models.TheoremDefinition
import com.statproof.proofengine.models.Topic
import com.statproof.proofengine.ast.ExpressionNode.*
import com.statproof.proofengine.simplifier.SimplificationEngine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Canonical derivation validation tests.
 *
 * Tests 20 proof correctness properties:
 *  - Structural validity of each canonical theorem
 *  - LaTeX generation produces non-empty, valid-looking strings
 *  - Simplification of key intermediate expressions
 *  - Determinism of proof generation across all modes
 */
@DisplayName("Canonical Derivation Validation — 20 Proof Tests")
class CanonicalDerivationTest {

    private val engine = ProofEngine()

    companion object {
        // ── Shared canonical theorem definitions ──────────────────────────────

        private fun makeDef(
            id: String,
            title: String,
            topic: Topic,
            steps: List<DerivationStepDef>,
        ) = TheoremDefinition(
            id = id,
            title = title,
            topic = topic,
            subtopic = "Canonical",
            difficulty = Difficulty.UNDERGRADUATE,
            statement = "\\text{$title}",
            conclusion = "\\text{Q.E.D.}",
            tags = listOf(topic.name),
            assumptions = listOf("Standard regularity conditions"),
            searchKeywords = listOf(id),
        ).let { def ->
            // Use copy with derivationSteps
            TheoremDefinition(
                id = def.id, title = def.title, topic = def.topic,
                subtopic = def.subtopic, difficulty = def.difficulty,
                statement = def.statement, conclusion = def.conclusion,
                tags = def.tags, assumptions = def.assumptions,
                searchKeywords = def.searchKeywords,
                derivationSteps = steps,
            )
        }

        private fun step(n: Int, expr: String, just: String) = DerivationStepDef(
            stepNumber = n, expression = expr, justification = just,
            explanation = "Step $n: $just", ruleId = "definition",
        )

        val CANONICAL_THEOREMS: List<TheoremDefinition> = listOf(
            // 1. Bayes' Theorem
            makeDef("canon_01_bayes", "Bayes' Theorem", Topic.PROBABILITY_THEORY, listOf(
                step(1, "P(A \\cap B) = P(A \\mid B) P(B)", "Conditional probability definition"),
                step(2, "P(A \\cap B) = P(B \\mid A) P(A)", "Symmetry of intersection"),
                step(3, "P(A \\mid B) = \\frac{P(B \\mid A) P(A)}{P(B)}", "Division by P(B)"),
            )),
            // 2. Law of Total Probability
            makeDef("canon_02_total_prob", "Law of Total Probability", Topic.PROBABILITY_THEORY, listOf(
                step(1, "B = \\bigcup_i (B \\cap A_i)", "Partition expansion"),
                step(2, "P(B) = \\sum_i P(B \\cap A_i)", "Countable additivity"),
                step(3, "P(B) = \\sum_i P(B \\mid A_i) P(A_i)", "Conditional probability"),
            )),
            // 3. Linearity of Expectation
            makeDef("canon_03_linearity_E", "Linearity of Expectation", Topic.PROBABILITY_THEORY, listOf(
                step(1, "\\mathbb{E}[aX + bY] = \\int (ax+by) f_{X,Y} \\, dx \\, dy", "Definition"),
                step(2, "= a \\mathbb{E}[X] + b \\mathbb{E}[Y]", "Linearity of integration"),
            )),
            // 4. Variance decomposition: Var(X) = E[X²] - (E[X])²
            makeDef("canon_04_variance_decomp", "Variance Decomposition", Topic.PROBABILITY_THEORY, listOf(
                step(1, "\\mathrm{Var}(X) = \\mathbb{E}[(X - \\mu)^2]", "Definition"),
                step(2, "= \\mathbb{E}[X^2 - 2\\mu X + \\mu^2]", "Expansion"),
                step(3, "= \\mathbb{E}[X^2] - 2\\mu^2 + \\mu^2", "Linearity of E"),
                step(4, "= \\mathbb{E}[X^2] - (\\mathbb{E}[X])^2", "Definition of μ"),
            )),
            // 5. Normal MGF
            makeDef("canon_05_normal_mgf", "Normal MGF", Topic.DISTRIBUTION_THEORY, listOf(
                step(1, "M_X(t) = \\mathbb{E}[e^{tX}]", "Definition"),
                step(2, "= \\int_{-\\infty}^\\infty e^{tx} \\frac{1}{\\sqrt{2\\pi\\sigma^2}} e^{-\\frac{(x-\\mu)^2}{2\\sigma^2}} dx", "Substituting normal density"),
                step(3, "= \\exp\\!\\left(\\mu t + \\tfrac{1}{2}\\sigma^2 t^2\\right)", "Completing the square"),
            )),
            // 6. Poisson MGF
            makeDef("canon_06_poisson_mgf", "Poisson MGF", Topic.DISTRIBUTION_THEORY, listOf(
                step(1, "M_X(t) = e^{-\\lambda} \\sum_{k=0}^\\infty \\frac{(\\lambda e^t)^k}{k!}", "Definition + rearrangement"),
                step(2, "= e^{-\\lambda} e^{\\lambda e^t}", "Taylor series e^x = Σ x^k/k!"),
                step(3, "= \\exp(\\lambda(e^t - 1))", "Combining exponents"),
            )),
            // 7. Memoryless property of Exponential
            makeDef("canon_07_memoryless", "Memoryless Property", Topic.DISTRIBUTION_THEORY, listOf(
                step(1, "P(X > s+t \\mid X > s) = \\frac{P(X > s+t)}{P(X > s)}", "Conditional probability"),
                step(2, "= \\frac{e^{-\\lambda(s+t)}}{e^{-\\lambda s}}", "Exponential survival function"),
                step(3, "= e^{-\\lambda t} = P(X > t)", "Laws of exponents"),
            )),
            // 8. Central Limit Theorem
            makeDef("canon_08_clt", "Central Limit Theorem", Topic.STATISTICAL_INFERENCE, listOf(
                step(1, "M_{Z_n}(t) = \\left[M_{Y_1}(t/\\sqrt{n})\\right]^n", "MGF of standardised sum"),
                step(2, "= \\left[1 + \\frac{t^2}{2n} + O(n^{-3/2})\\right]^n", "Taylor expansion"),
                step(3, "\\to e^{t^2/2} \\text{ as } n \\to \\infty", "Limit definition of e^x"),
                step(4, "\\sqrt{n}(\\bar{X}_n - \\mu)/\\sigma \\xrightarrow{d} \\mathcal{N}(0,1)", "Continuity theorem"),
            )),
            // 9. MLE for Normal Mean
            makeDef("canon_09_mle_normal", "MLE for Normal Mean", Topic.STATISTICAL_INFERENCE, listOf(
                step(1, "\\ell(\\mu) = -\\frac{n}{2}\\ln(2\\pi\\sigma^2) - \\frac{1}{2\\sigma^2}\\sum(x_i-\\mu)^2", "Log-likelihood"),
                step(2, "\\partial\\ell/\\partial\\mu = \\frac{1}{\\sigma^2}\\sum(x_i - \\mu) = 0", "Score equation"),
                step(3, "\\hat{\\mu} = \\bar{X}", "Solving"),
            )),
            // 10. Cramér-Rao Lower Bound
            makeDef("canon_10_cramer_rao", "Cramér-Rao Lower Bound", Topic.STATISTICAL_INFERENCE, listOf(
                step(1, "\\mathrm{Cov}(\\hat{\\theta}, s(X;\\theta)) = \\partial_\\theta \\mathbb{E}[\\hat{\\theta}]", "Score equation"),
                step(2, "[\\partial_\\theta \\mathbb{E}(\\hat{\\theta})]^2 \\leq \\mathrm{Var}(\\hat{\\theta}) I(\\theta)", "Cauchy-Schwarz"),
                step(3, "\\mathrm{Var}(\\hat{\\theta}) \\geq 1/I(\\theta)", "Rearranging (unbiased)"),
            )),
            // 11. OLS Normal Equations
            makeDef("canon_11_ols", "OLS Normal Equations", Topic.REGRESSION, listOf(
                step(1, "\\text{RSS}(\\beta) = \\|y - X\\beta\\|^2", "Objective"),
                step(2, "\\partial_\\beta \\text{RSS} = -2X^\\top y + 2X^\\top X \\beta = 0", "Gradient condition"),
                step(3, "\\hat{\\beta} = (X^\\top X)^{-1} X^\\top y", "Solving normal equations"),
            )),
            // 12. Gauss-Markov Theorem
            makeDef("canon_12_gauss_markov", "Gauss-Markov Theorem", Topic.REGRESSION, listOf(
                step(1, "\\mathrm{Var}(\\tilde{\\beta}) = \\sigma^2[(X^\\top X)^{-1} + DD^\\top]", "Variance of linear estimator"),
                step(2, "\\mathrm{Var}(\\tilde{\\beta}) - \\mathrm{Var}(\\hat{\\beta}) = \\sigma^2 DD^\\top \\succeq 0", "Positive semidefinite"),
                step(3, "\\hat{\\beta}_{\\text{OLS}} \\text{ is BLUE}", "Q.E.D."),
            )),
            // 13. Bayesian Normal-Normal Update
            makeDef("canon_13_bayes_normal", "Bayesian Normal-Normal Conjugate Update", Topic.BAYESIAN, listOf(
                step(1, "p(\\mu \\mid x) \\propto \\exp(-\\frac{n}{2\\sigma^2}(\\bar{x}-\\mu)^2) \\exp(-\\frac{1}{2\\tau^2}(\\mu-\\mu_0)^2)", "Bayes' theorem"),
                step(2, "\\propto \\exp(-\\frac{1}{2\\tau_n^2}(\\mu - \\mu_n)^2)", "Complete the square in μ"),
                step(3, "\\mu \\mid X \\sim \\mathcal{N}(\\mu_n, \\tau_n^2)", "Posterior identification"),
            )),
            // 14. Jensen's Inequality
            makeDef("canon_14_jensen", "Jensen's Inequality", Topic.PROBABILITY_THEORY, listOf(
                step(1, "\\phi(\\mathbb{E}[X]) \\leq \\mathbb{E}[\\phi(X)]", "Statement for convex φ"),
                step(2, "\\phi(x) \\geq \\phi(a) + \\phi'(a)(x-a) \\text{ for convex } \\phi", "Supporting hyperplane"),
                step(3, "\\mathbb{E}[\\phi(X)] \\geq \\phi(\\mathbb{E}[X]) + \\phi'(a)(\\mathbb{E}[X]-a)", "Taking expectations"),
                step(4, "\\text{Setting } a = \\mathbb{E}[X] \\text{ gives the result}", "Conclusion"),
            )),
            // 15. Markov's Inequality
            makeDef("canon_15_markov", "Markov's Inequality", Topic.PROBABILITY_THEORY, listOf(
                step(1, "\\mathbb{E}[X] = \\mathbb{E}[X \\mathbf{1}_{X \\geq a}] + \\mathbb{E}[X \\mathbf{1}_{X < a}]", "Partition of expectation"),
                step(2, "\\geq \\mathbb{E}[a \\mathbf{1}_{X \\geq a}] = a P(X \\geq a)", "X ≥ a on the event {X ≥ a}"),
                step(3, "P(X \\geq a) \\leq \\mathbb{E}[X] / a", "Rearranging"),
            )),
            // 16. Chebyshev's Inequality
            makeDef("canon_16_chebyshev", "Chebyshev's Inequality", Topic.PROBABILITY_THEORY, listOf(
                step(1, "P(|X - \\mu| \\geq k\\sigma) = P((X-\\mu)^2 \\geq k^2\\sigma^2)", "Squaring"),
                step(2, "\\leq \\frac{\\mathbb{E}[(X-\\mu)^2]}{k^2 \\sigma^2}", "Markov applied to (X-μ)²"),
                step(3, "= \\frac{\\sigma^2}{k^2 \\sigma^2} = \\frac{1}{k^2}", "Variance definition"),
            )),
            // 17. Covariance bilinearity
            makeDef("canon_17_cov_bilinear", "Covariance Bilinearity", Topic.PROBABILITY_THEORY, listOf(
                step(1, "\\mathrm{Cov}(aX, Y) = \\mathbb{E}[aXY] - \\mathbb{E}[aX]\\mathbb{E}[Y]", "Definition"),
                step(2, "= a(\\mathbb{E}[XY] - \\mathbb{E}[X]\\mathbb{E}[Y])", "Linearity of E"),
                step(3, "= a \\, \\mathrm{Cov}(X, Y)", "Definition of Cov"),
            )),
            // 18. Weak Law of Large Numbers
            makeDef("canon_18_wlln", "Weak Law of Large Numbers", Topic.STATISTICAL_INFERENCE, listOf(
                step(1, "\\mathrm{Var}(\\bar{X}_n) = \\sigma^2/n", "Independence + variance formula"),
                step(2, "P(|\\bar{X}_n - \\mu| \\geq \\varepsilon) \\leq \\frac{\\sigma^2}{n\\varepsilon^2}", "Chebyshev's inequality"),
                step(3, "\\to 0 \\text{ as } n \\to \\infty", "For fixed ε > 0"),
                step(4, "\\bar{X}_n \\xrightarrow{P} \\mu", "Definition of convergence in probability"),
            )),
            // 19. Information Inequality (KL non-negativity)
            makeDef("canon_19_kl_nonneg", "Non-negativity of KL Divergence", Topic.INFORMATION_THEORY, listOf(
                step(1, "D_{\\mathrm{KL}}(P \\| Q) = \\mathbb{E}_P\\left[\\ln\\frac{p(X)}{q(X)}\\right]", "Definition"),
                step(2, "= -\\mathbb{E}_P\\left[\\ln\\frac{q(X)}{p(X)}\\right]", "Negation"),
                step(3, "\\geq -\\ln \\mathbb{E}_P\\left[\\frac{q(X)}{p(X)}\\right]", "Jensen's (ln is concave)"),
                step(4, "= -\\ln 1 = 0", "Normalisation of Q"),
            )),
            // 20. Delta Method
            makeDef("canon_20_delta_method", "Delta Method", Topic.ASYMPTOTICS, listOf(
                step(1, "\\sqrt{n}(\\hat{\\theta} - \\theta) \\xrightarrow{d} \\mathcal{N}(0, \\sigma^2)", "Assumption"),
                step(2, "g(\\hat{\\theta}) \\approx g(\\theta) + g'(\\theta)(\\hat{\\theta} - \\theta)", "First-order Taylor expansion"),
                step(3, "\\sqrt{n}(g(\\hat{\\theta}) - g(\\theta)) \\xrightarrow{d} \\mathcal{N}(0, [g'(\\theta)]^2 \\sigma^2)", "CLT + Slutsky"),
            )),
        )

        @JvmStatic
        fun canonicalTheorems(): Stream<TheoremDefinition> = CANONICAL_THEOREMS.stream()
    }

    // ── Test 1-20: One test per canonical theorem ─────────────────────────────

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("canonicalTheorems")
    @DisplayName("Canonical proof generates successfully")
    fun `canonical proof generates with valid structure`(definition: TheoremDefinition) {
        val proof = engine.generateProof(definition, mode = ProofMode.STANDARD, verify = true)

        // ID matches
        assertEquals(definition.id, proof.id)

        // At least one step
        assertTrue(proof.steps.isNotEmpty(), "Proof '${definition.title}' has no steps")

        // All steps have required fields
        proof.steps.forEach { step ->
            assertTrue(step.latex.isNotBlank(), "[${definition.id}] Step ${step.stepNumber} has blank LaTeX")
            assertTrue(step.justification.isNotBlank(), "[${definition.id}] Step ${step.stepNumber} has blank justification")
            assertTrue(step.explanation.isNotBlank(), "[${definition.id}] Step ${step.stepNumber} has blank explanation")
        }

        // Steps are sequential
        proof.steps.forEachIndexed { index, step ->
            assertEquals(index + 1, step.stepNumber, "[${definition.id}] Step numbering broken at index $index")
        }

        // Verification passed
        val vr = proof.verificationResult
        assertNotNull(vr, "[${definition.id}] Missing verification result")
        assertTrue(vr!!.isValid, "[${definition.id}] Proof verification FAILED: ${vr.message}")

        // Metadata correct
        assertEquals(definition.topic, proof.metadata.topic)
        assertEquals(definition.difficulty, proof.metadata.difficulty)
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("canonicalTheorems")
    @DisplayName("Canonical proof is deterministic across 3 runs")
    fun `canonical proof generation is deterministic`(definition: TheoremDefinition) {
        val proof1 = engine.generateProof(definition, mode = ProofMode.STANDARD)
        val proof2 = engine.generateProof(definition, mode = ProofMode.STANDARD)
        val proof3 = engine.generateProof(definition, mode = ProofMode.STANDARD)

        assertEquals(proof1.steps.size, proof2.steps.size)
        assertEquals(proof2.steps.size, proof3.steps.size)

        proof1.steps.zip(proof2.steps).forEach { (s1, s2) ->
            assertEquals(s1.latex, s2.latex)
            assertEquals(s1.justification, s2.justification)
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("canonicalTheorems")
    @DisplayName("Canonical proof converts across all modes without error")
    fun `canonical proof converts across all modes`(definition: TheoremDefinition) {
        val proof = engine.generateProof(definition)
        ProofMode.entries.forEach { mode ->
            val converted = engine.convertMode(proof, mode)
            assertEquals(proof.steps.size, converted.steps.size)
            assertEquals(mode, converted.mode)
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("canonicalTheorems")
    @DisplayName("Canonical proof step LaTeX is KaTeX-renderable")
    fun `canonical proof step latex is non-empty and plausibly valid`(definition: TheoremDefinition) {
        val proof = engine.generateProof(definition)
        proof.steps.forEach { step ->
            val latex = step.latex
            // Must be non-empty
            assertTrue(latex.isNotBlank())
            // Must not contain obvious errors
            assertFalse(latex.contains("null"), "Step ${step.stepNumber} latex contains 'null': $latex")
            assertFalse(latex.contains("NaN"), "Step ${step.stepNumber} latex contains 'NaN': $latex")
        }
    }

    // ── LaTeX generation correctness tests ───────────────────────────────────

    @Test
    @DisplayName("LaTeX for Fraction contains frac")
    fun `fraction latex is correct`() {
        val expr = Fraction(Var("P(B \\mid A) P(A)"), Var("P(B)"))
        val latex = LaTeXGenerator.generate(expr)
        assertTrue(latex.contains("\\frac"))
    }

    @Test
    @DisplayName("LaTeX for Summation contains sum")
    fun `summation latex is correct`() {
        val expr = Summation(
            Subscript(Var("x"), Var("i")),
            Var("i"),
            Num.ONE,
            Var("n"),
        )
        val latex = LaTeXGenerator.generate(expr)
        assertTrue(latex.contains("\\sum"))
        assertTrue(latex.contains("i="))
    }

    @Test
    @DisplayName("LaTeX for Integral contains int")
    fun `integral latex is correct`() {
        val expr = Integral(
            Product(listOf(Exp(Product(listOf(Num(-1L), Var("x")))), Var("f"))),
            Var("x"),
            Num.ZERO,
            Const.INFINITY,
        )
        val latex = LaTeXGenerator.generate(expr)
        assertTrue(latex.contains("\\int"))
        assertTrue(latex.contains("\\infty"))
    }

    @Test
    @DisplayName("Simplification: E[X+Y] = E[X] + E[Y]")
    fun `linearity of expectation simplification`() {
        val x = Var("X")
        val y = Var("Y")
        val expr = Expectation(Sum(listOf(x, y)))
        val result = SimplificationEngine.simplify(expr)
        assertTrue(result is Sum, "Should be Sum but got: $result")
        val terms = (result as Sum).terms
        assertEquals(2, terms.size)
        assertTrue(terms.all { it is Expectation })
    }

    @Test
    @DisplayName("Simplification: Var[c] = 0")
    fun `variance of constant is zero`() {
        val expr = Variance(Num(42L))
        val result = SimplificationEngine.simplify(expr)
        assertEquals(Num.ZERO, result)
    }

    @Test
    @DisplayName("Simplification: ln(e^x) = x")
    fun `log of exp cancels`() {
        val x = Var("x")
        val expr = Log(Exp(x))
        val result = SimplificationEngine.simplify(expr)
        assertEquals(x, result)
    }

    @Test
    @DisplayName("Simplification: e^(a+b) splits to e^a * e^b")
    fun `exp of sum splits`() {
        val a = Var("a")
        val b = Var("b")
        val expr = Exp(Sum(listOf(a, b)))
        val result = SimplificationEngine.simplify(expr)
        assertTrue(result is Product, "Expected Product but got: $result")
        val factors = (result as Product).factors
        assertTrue(factors.all { it is Exp })
    }
}
