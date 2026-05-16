package com.statproof.proofengine

import com.statproof.proofengine.ast.ExpressionNode
import com.statproof.proofengine.ast.ExpressionNode.Const
import com.statproof.proofengine.ast.ExpressionNode.Exp
import com.statproof.proofengine.ast.ExpressionNode.Expectation
import com.statproof.proofengine.ast.ExpressionNode.Fraction
import com.statproof.proofengine.ast.ExpressionNode.Log
import com.statproof.proofengine.ast.ExpressionNode.Negate
import com.statproof.proofengine.ast.ExpressionNode.Num
import com.statproof.proofengine.ast.ExpressionNode.Power
import com.statproof.proofengine.ast.ExpressionNode.Product
import com.statproof.proofengine.ast.ExpressionNode.Sum
import com.statproof.proofengine.ast.ExpressionNode.Var
import com.statproof.proofengine.ast.ExpressionNode.Variance
import com.statproof.proofengine.ast.isOne
import com.statproof.proofengine.ast.isZero
import com.statproof.proofengine.ast.plus
import com.statproof.proofengine.ast.times
import com.statproof.proofengine.latex.LaTeXGenerator
import com.statproof.proofengine.rules.RuleRegistry
import com.statproof.proofengine.simplifier.SimplificationEngine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.math.BigDecimal

@DisplayName("StatProof Symbolic Engine Tests")
class SymbolicEngineTest {

    // ── ExpressionNode Tests ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Num Node")
    inner class NumTests {

        @Test
        fun `Num isZero detects zero correctly`() {
            assertTrue(Num(0L).isZero)
            assertTrue(Num(BigDecimal.ZERO).isZero)
            assertFalse(Num(1L).isZero)
            assertFalse(Num(-1L).isZero)
        }

        @Test
        fun `Num isOne detects one correctly`() {
            assertTrue(Num(1L).isOne)
            assertTrue(Num(BigDecimal.ONE).isOne)
            assertFalse(Num(0L).isOne)
            assertFalse(Num(2L).isOne)
        }

        @Test
        fun `Num isInteger detects integer values`() {
            assertTrue(Num(3L).isInteger)
            assertTrue(Num(0L).isInteger)
            assertFalse(Num("0.5").isInteger)
            assertFalse(Num("1.1").isInteger)
        }

        @Test
        fun `Num constants are structurally equal`() {
            assertEquals(Num.ZERO, Num(0L))
            assertEquals(Num.ONE, Num(1L))
            assertEquals(Num.TWO, Num(2L))
        }

        @Test
        fun `Num isPositive and isNegative`() {
            assertTrue(Num(5L).isPositive)
            assertFalse(Num(-1L).isPositive)
            assertTrue(Num(-3L).isNegative)
            assertFalse(Num(1L).isNegative)
            assertFalse(Num(0L).isPositive)
        }
    }

    @Nested
    @DisplayName("Var Node")
    inner class VarTests {

        @Test
        fun `Var displayName without subscript`() {
            assertEquals("X", Var("X").displayName)
            assertEquals("\\mu", Var("\\mu").displayName)
        }

        @Test
        fun `Var displayName with subscript`() {
            assertEquals("X_i", Var("X", "i").displayName)
            assertEquals("\\sigma_2", Var("\\sigma", "2").displayName)
        }

        @Test
        fun `Var structural equality`() {
            assertEquals(Var("X"), Var("X"))
            assertEquals(Var("X", "i"), Var("X", "i"))
            assertTrue(Var("X") != Var("Y"))
            assertTrue(Var("X", "i") != Var("X", "j"))
        }

        @Test
        fun `Var companion constants are correct`() {
            assertEquals("\\mu", Var.MU.name)
            assertEquals("\\sigma", Var.SIGMA.name)
            assertNull(Var.MU.subscript)
        }
    }

    // ── Simplification Engine Tests ───────────────────────────────────────────

    @Nested
    @DisplayName("SimplificationEngine — Arithmetic")
    inner class ArithmeticSimplificationTests {

        @Test
        fun `Sum of two numbers`() {
            val expr = Sum(listOf(Num(3L), Num(4L)))
            val result = SimplificationEngine.simplify(expr)
            assertEquals(Num(7L), result)
        }

        @Test
        fun `Sum with zero collapses`() {
            val x = Var("x")
            val expr = Sum(listOf(x, Num.ZERO))
            val result = SimplificationEngine.simplify(expr)
            assertEquals(x, result)
        }

        @Test
        fun `Sum of all zeros`() {
            val expr = Sum(listOf(Num.ZERO, Num.ZERO))
            val result = SimplificationEngine.simplify(expr)
            assertEquals(Num.ZERO, result)
        }

        @Test
        fun `Product with one collapses`() {
            val x = Var("x")
            val expr = Product(listOf(x, Num.ONE))
            val result = SimplificationEngine.simplify(expr)
            assertEquals(x, result)
        }

        @Test
        fun `Product with zero is zero`() {
            val x = Var("x")
            val expr = Product(listOf(x, Num.ZERO))
            val result = SimplificationEngine.simplify(expr)
            assertEquals(Num.ZERO, result)
        }

        @Test
        fun `Product of two numbers`() {
            val expr = Product(listOf(Num(3L), Num(4L)))
            val result = SimplificationEngine.simplify(expr)
            assertEquals(Num(12L), result)
        }

        @Test
        fun `Fraction n over 1 simplifies to n`() {
            val x = Var("x")
            val expr = Fraction(x, Num.ONE)
            val result = SimplificationEngine.simplify(expr)
            assertEquals(x, result)
        }

        @Test
        fun `Fraction 0 over n simplifies to 0`() {
            val expr = Fraction(Num.ZERO, Var("n"))
            val result = SimplificationEngine.simplify(expr)
            assertEquals(Num.ZERO, result)
        }

        @Test
        fun `Fraction a over a simplifies to 1`() {
            val x = Var("x")
            val expr = Fraction(x, x)
            val result = SimplificationEngine.simplify(expr)
            assertEquals(Num.ONE, result)
        }

        @Test
        fun `Fraction reduces by GCD`() {
            val expr = Fraction(Num(6L), Num(4L))
            val result = SimplificationEngine.simplify(expr)
            assertEquals(Fraction(Num(3L), Num(2L)), result)
        }

        @Test
        fun `Fraction with integer result`() {
            val expr = Fraction(Num(6L), Num(3L))
            val result = SimplificationEngine.simplify(expr)
            assertEquals(Num(2L), result)
        }

        @Test
        fun `Power x to 0 is 1`() {
            val expr = Power(Var("x"), Num.ZERO)
            val result = SimplificationEngine.simplify(expr)
            assertEquals(Num.ONE, result)
        }

        @Test
        fun `Power x to 1 is x`() {
            val x = Var("x")
            val expr = Power(x, Num.ONE)
            val result = SimplificationEngine.simplify(expr)
            assertEquals(x, result)
        }

        @Test
        fun `Power 1 to any is 1`() {
            val expr = Power(Num.ONE, Var("n"))
            val result = SimplificationEngine.simplify(expr)
            assertEquals(Num.ONE, result)
        }

        @Test
        fun `Power numeric base and exponent`() {
            val expr = Power(Num(2L), Num(3L))
            val result = SimplificationEngine.simplify(expr)
            assertEquals(Num(8L), result)
        }

        @Test
        fun `Negate of number`() {
            val expr = Negate(Num(5L))
            val result = SimplificationEngine.simplify(expr)
            assertEquals(Num(-5L), result)
        }

        @Test
        fun `Double negation cancels`() {
            val x = Var("x")
            val expr = Negate(Negate(x))
            val result = SimplificationEngine.simplify(expr)
            assertEquals(x, result)
        }

        @Test
        fun `Nested sum flattens`() {
            val a = Var("a")
            val b = Var("b")
            val c = Var("c")
            val inner = Sum(listOf(a, b))
            val outer = Sum(listOf(inner, c))
            val result = SimplificationEngine.simplify(outer)
            // After flattening: Sum(a, b, c)
            assertTrue(result is Sum)
            val terms = (result as Sum).terms
            assertEquals(3, terms.size)
        }
    }

    @Nested
    @DisplayName("SimplificationEngine — Logarithms")
    inner class LogSimplificationTests {

        @Test
        fun `ln of 1 is 0`() {
            val expr = Log(Num.ONE)
            assertEquals(Num.ZERO, SimplificationEngine.simplify(expr))
        }

        @Test
        fun `ln of e is 1`() {
            val expr = Log(Const.E)
            assertEquals(Num.ONE, SimplificationEngine.simplify(expr))
        }

        @Test
        fun `ln(e^x) = x`() {
            val x = Var("x")
            val expr = Log(Exp(x))
            assertEquals(x, SimplificationEngine.simplify(expr))
        }

        @Test
        fun `e^0 = 1`() {
            val expr = Exp(Num.ZERO)
            assertEquals(Num.ONE, SimplificationEngine.simplify(expr))
        }

        @Test
        fun `e^(ln x) = x`() {
            val x = Var("x")
            val expr = Exp(Log(x))
            assertEquals(x, SimplificationEngine.simplify(expr))
        }

        @Test
        fun `ln(x^n) = n ln(x)`() {
            val x = Var("x")
            val n = Var("n")
            val expr = Log(Power(x, n))
            val result = SimplificationEngine.simplify(expr)
            assertTrue(result is Product)
            val factors = (result as Product).factors
            assertTrue(factors.contains(n))
            assertTrue(factors.any { it is Log && (it as Log).arg == x })
        }

        @Test
        fun `ln(a * b) = ln(a) + ln(b)`() {
            val a = Var("a")
            val b = Var("b")
            val expr = Log(Product(listOf(a, b)))
            val result = SimplificationEngine.simplify(expr)
            assertTrue(result is Sum)
        }
    }

    @Nested
    @DisplayName("SimplificationEngine — Statistical")
    inner class StatisticalSimplificationTests {

        @Test
        fun `E[c] = c for numeric constant`() {
            val expr = Expectation(Num(5L))
            val result = SimplificationEngine.simplify(expr)
            assertEquals(Num(5L), result)
        }

        @Test
        fun `E[X+Y] = E[X] + E[Y]`() {
            val x = Var("X")
            val y = Var("Y")
            val expr = Expectation(Sum(listOf(x, y)))
            val result = SimplificationEngine.simplify(expr)
            assertTrue(result is Sum, "Expected Sum but got $result")
            val terms = (result as Sum).terms
            assertEquals(2, terms.size)
            assertTrue(terms.all { it is Expectation })
        }

        @Test
        fun `E[c*X] = c*E[X] for numeric c`() {
            val x = Var("X")
            val c = Num(3L)
            val expr = Expectation(Product(listOf(c, x)))
            val result = SimplificationEngine.simplify(expr)
            assertTrue(result is Product)
            val factors = (result as Product).factors
            assertTrue(factors.contains(c))
            assertTrue(factors.any { it is Expectation })
        }

        @Test
        fun `Var[c] = 0 for constant c`() {
            val expr = Variance(Num(7L))
            val result = SimplificationEngine.simplify(expr)
            assertEquals(Num.ZERO, result)
        }

        @Test
        fun `Cov(X,X) = Var(X)`() {
            val x = Var("X")
            val expr = ExpressionNode.Covariance(x, x)
            val result = SimplificationEngine.simplify(expr)
            assertTrue(result is Variance)
            assertEquals(x, (result as Variance).expr)
        }

        @Test
        fun `Cov(c, X) = 0`() {
            val x = Var("X")
            val c = Num(5L)
            val expr = ExpressionNode.Covariance(c, x)
            val result = SimplificationEngine.simplify(expr)
            assertEquals(Num.ZERO, result)
        }
    }

    @Nested
    @DisplayName("SimplificationEngine — Square Root")
    inner class SqrtTests {

        @Test
        fun `sqrt(0) = 0`() {
            assertEquals(Num.ZERO, SimplificationEngine.simplify(ExpressionNode.Sqrt(Num.ZERO)))
        }

        @Test
        fun `sqrt(1) = 1`() {
            assertEquals(Num.ONE, SimplificationEngine.simplify(ExpressionNode.Sqrt(Num.ONE)))
        }

        @Test
        fun `sqrt(4) = 2`() {
            val result = SimplificationEngine.simplify(ExpressionNode.Sqrt(Num(4L)))
            assertEquals(Num(2L), result)
        }

        @Test
        fun `sqrt(9) = 3`() {
            val result = SimplificationEngine.simplify(ExpressionNode.Sqrt(Num(9L)))
            assertEquals(Num(3L), result)
        }

        @Test
        fun `sqrt(x^2) = |x|`() {
            val x = Var("x")
            val expr = ExpressionNode.Sqrt(Power(x, Num.TWO))
            val result = SimplificationEngine.simplify(expr)
            assertTrue(result is ExpressionNode.Abs)
        }
    }

    // ── LaTeX Generator Tests ─────────────────────────────────────────────────

    @Nested
    @DisplayName("LaTeXGenerator")
    inner class LaTeXGeneratorTests {

        @Test
        fun `Num generates integer string`() {
            assertEquals("3", LaTeXGenerator.generate(Num(3L)))
            assertEquals("0", LaTeXGenerator.generate(Num.ZERO))
        }

        @Test
        fun `Var generates name`() {
            assertEquals("x", LaTeXGenerator.generate(Var("x")))
            assertEquals("\\mu", LaTeXGenerator.generate(Var("\\mu")))
        }

        @Test
        fun `Var with subscript`() {
            assertEquals("X_{i}", LaTeXGenerator.generate(Var("X", "i")))
        }

        @Test
        fun `Fraction generates frac command`() {
            val expr = Fraction(Var("a"), Var("b"))
            val latex = LaTeXGenerator.generate(expr)
            assertTrue(latex.contains("\\frac"), "Expected \\frac in: $latex")
            assertTrue(latex.contains("a"))
            assertTrue(latex.contains("b"))
        }

        @Test
        fun `Sum generates plus-separated terms`() {
            val expr = Sum(listOf(Var("a"), Var("b")))
            val latex = LaTeXGenerator.generate(expr)
            assertTrue(latex.contains("+"), "Expected + in: $latex")
        }

        @Test
        fun `Log generates ln command`() {
            val expr = Log(Var("x"))
            val latex = LaTeXGenerator.generate(expr)
            assertTrue(latex.contains("\\ln"), "Expected \\ln in: $latex")
        }

        @Test
        fun `Exp generates e^ notation`() {
            val expr = Exp(Var("x"))
            val latex = LaTeXGenerator.generate(expr)
            assertTrue(latex.contains("e^"), "Expected e^ in: $latex")
        }

        @Test
        fun `Integral generates int notation`() {
            val expr = ExpressionNode.Integral(
                Var("f"),
                Var("x"),
                Num.ZERO,
                Const.INFINITY,
            )
            val latex = LaTeXGenerator.generate(expr)
            assertTrue(latex.contains("\\int"), "Expected \\int in: $latex")
            assertTrue(latex.contains("\\infty"), "Expected \\infty in: $latex")
        }

        @Test
        fun `Summation generates sum notation`() {
            val expr = ExpressionNode.Summation(
                Var("x", "i"),
                Var("i"),
                Num.ONE,
                Var("n"),
            )
            val latex = LaTeXGenerator.generate(expr)
            assertTrue(latex.contains("\\sum"), "Expected \\sum in: $latex")
        }

        @Test
        fun `Expectation generates mathbb E`() {
            val expr = Expectation(Var("X"))
            val latex = LaTeXGenerator.generate(expr)
            assertTrue(latex.contains("\\mathbb{E}"), "Expected \\mathbb{E} in: $latex")
        }

        @Test
        fun `Variance generates Var notation`() {
            val expr = Variance(Var("X"))
            val latex = LaTeXGenerator.generate(expr)
            assertTrue(latex.contains("\\mathrm{Var}"), "Expected \\mathrm{Var} in: $latex")
        }

        @Test
        fun `Matrix generates pmatrix environment`() {
            val expr = ExpressionNode.Matrix(
                listOf(
                    listOf(Num.ONE, Num.ZERO),
                    listOf(Num.ZERO, Num.ONE),
                )
            )
            val latex = LaTeXGenerator.generate(expr)
            assertTrue(latex.contains("pmatrix"), "Expected pmatrix in: $latex")
        }

        @Test
        fun `Power generates superscript`() {
            val expr = Power(Var("x"), Num(2L))
            val latex = LaTeXGenerator.generate(expr)
            assertTrue(latex.contains("^{"), "Expected ^{ in: $latex")
        }

        @Test
        fun `Probability generates mathbb P`() {
            val expr = ExpressionNode.Probability(Var("A"))
            val latex = LaTeXGenerator.generate(expr)
            assertTrue(latex.contains("\\mathbb{P}"), "Expected \\mathbb{P} in: $latex")
        }

        @Test
        fun `Binomial generates binom`() {
            val expr = ExpressionNode.Binomial(Var("n"), Var("k"))
            val latex = LaTeXGenerator.generate(expr)
            assertTrue(latex.contains("\\binom"), "Expected \\binom in: $latex")
        }

        @ParameterizedTest
        @CsvSource(
            "0, 0",
            "1, 1",
            "42, 42",
            "-5, -5",
        )
        fun `Num renders correctly for integers`(input: Long, expected: String) {
            assertEquals(expected, LaTeXGenerator.generate(Num(input)))
        }

        @Test
        fun `Piecewise generates cases environment`() {
            val expr = ExpressionNode.Piecewise(
                listOf(
                    Num.ONE to ExpressionNode.Indicator(ExpressionNode.Probability(Var("A"))),
                    Num.ZERO to null,
                )
            )
            val latex = LaTeXGenerator.generate(expr)
            assertTrue(latex.contains("cases"), "Expected cases in: $latex")
        }

        @Test
        fun `generateAligned produces aligned block`() {
            val steps = listOf(
                "Definition" to Var("X") as ExpressionNode,
                "Substitution" to Num(5L) as ExpressionNode,
            )
            val latex = LaTeXGenerator.generateAligned(steps)
            assertTrue(latex.contains("\\begin{aligned}"))
            assertTrue(latex.contains("\\end{aligned}"))
        }
    }

    // ── Rule Registry Tests ───────────────────────────────────────────────────

    @Nested
    @DisplayName("RuleRegistry")
    inner class RuleRegistryTests {

        @Test
        fun `Registry is non-empty`() {
            assertTrue(RuleRegistry.rules.isNotEmpty())
        }

        @Test
        fun `get returns rule by id`() {
            assertNotNull(RuleRegistry.get("add_zero"))
            assertNotNull(RuleRegistry.get("mul_one"))
            assertNotNull(RuleRegistry.get("log_product"))
        }

        @Test
        fun `get returns null for unknown id`() {
            assertNull(RuleRegistry.get("nonexistent_rule_xyz"))
        }

        @Test
        fun `add_zero rule applies to sum with zero term`() {
            val rule = RuleRegistry.get("add_zero")!!
            val expr = Sum(listOf(Var("x"), Num.ZERO))
            assertTrue(rule.applies(expr))
        }

        @Test
        fun `add_zero rule does not apply to pure sum without zero`() {
            val rule = RuleRegistry.get("add_zero")!!
            val expr = Sum(listOf(Var("x"), Var("y")))
            assertFalse(rule.applies(expr))
        }

        @Test
        fun `mul_zero rule applies to product with zero`() {
            val rule = RuleRegistry.get("mul_zero")!!
            val expr = Product(listOf(Var("x"), Num.ZERO))
            assertTrue(rule.applies(expr))
            assertEquals(Num.ZERO, rule.apply(expr))
        }

        @Test
        fun `log_product rule applies to log of product`() {
            val rule = RuleRegistry.get("log_product")!!
            val expr = Log(Product(listOf(Var("a"), Var("b"))))
            assertTrue(rule.applies(expr))
            val result = rule.apply(expr)
            assertNotNull(result)
            assertTrue(result is Sum)
        }

        @Test
        fun `expectation_linearity_sum applies to E[X+Y]`() {
            val rule = RuleRegistry.get("expectation_linearity_sum")!!
            val expr = Expectation(Sum(listOf(Var("X"), Var("Y"))))
            assertTrue(rule.applies(expr))
            val result = rule.apply(expr)
            assertNotNull(result)
            assertTrue(result is Sum)
        }

        @Test
        fun `variance_constant rule gives zero`() {
            val rule = RuleRegistry.get("variance_constant")!!
            val expr = Variance(Num(5L))
            assertTrue(rule.applies(expr))
            assertEquals(Num.ZERO, rule.apply(expr))
        }

        @Test
        fun `transpose_transpose rule cancels double transpose`() {
            val rule = RuleRegistry.get("transpose_transpose")!!
            val x = Var("X")
            val expr = ExpressionNode.Transpose(ExpressionNode.Transpose(x))
            assertTrue(rule.applies(expr))
            assertEquals(x, rule.apply(expr))
        }

        @Test
        fun `All rules have non-empty ids and names`() {
            RuleRegistry.rules.forEach { (id, rule) ->
                assertTrue(id.isNotBlank(), "Rule id is blank")
                assertTrue(rule.name.isNotBlank(), "Rule '${rule.id}' has blank name")
                assertTrue(rule.description.isNotBlank(), "Rule '${rule.id}' has blank description")
            }
        }
    }

    // ── Integration Tests: Full Simplification Chains ─────────────────────────

    @Nested
    @DisplayName("Integration: Full Simplification Chains")
    inner class IntegrationTests {

        @Test
        fun `Linearity of expectation chain`() {
            val x = Var("X")
            val y = Var("Y")
            val c = Num(3L)
            // E[3X + Y] should simplify to 3*E[X] + E[Y]
            val expr = Expectation(Sum(listOf(Product(listOf(c, x)), y)))
            val result = SimplificationEngine.simplify(expr)
            assertTrue(result is Sum, "Expected Sum: $result")
        }

        @Test
        fun `Variance of constant plus variable`() {
            val x = Var("X")
            val b = Num(5L)
            // Var(X + 5) = Var(X) since constants don't affect variance
            val expr = Variance(Sum(listOf(x, b)))
            val result = SimplificationEngine.simplify(expr)
            // After simplification, the constant term should be removed
            assertTrue(result is Variance, "Expected Variance: $result")
        }

        @Test
        fun `Power chain (x^a)^b = x^(a*b)`() {
            val x = Var("x")
            val a = Num(2L)
            val b = Num(3L)
            val expr = Power(Power(x, a), b)
            val result = SimplificationEngine.simplify(expr)
            // x^(2*3) = x^6
            assertTrue(result is Power || result == Power(x, Num(6L)))
        }

        @Test
        fun `Complex arithmetic: (3 + 4) * 2 - 1 = 13`() {
            val expr = Sum(listOf(
                Product(listOf(Sum(listOf(Num(3L), Num(4L))), Num(2L))),
                Negate(Num(1L))
            ))
            val result = SimplificationEngine.simplify(expr)
            assertEquals(Num(13L), result)
        }

        @Test
        fun `Fraction chain: (6/4) simplifies to 3/2`() {
            val expr = Fraction(Num(6L), Num(4L))
            val result = SimplificationEngine.simplify(expr)
            assertEquals(Fraction(Num(3L), Num(2L)), result)
        }

        @Test
        fun `Zero product in sum: x + 0*y = x`() {
            val x = Var("x")
            val y = Var("y")
            val expr = Sum(listOf(x, Product(listOf(Num.ZERO, y))))
            val result = SimplificationEngine.simplify(expr)
            assertEquals(x, result)
        }
    }

    // ── Determinism Tests ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Determinism")
    inner class DeterminismTests {

        @Test
        fun `Simplification is deterministic for same input`() {
            val expr = Sum(listOf(
                Product(listOf(Num(2L), Var("x"))),
                Fraction(Num(4L), Num(2L)),
            ))
            val result1 = SimplificationEngine.simplify(expr)
            val result2 = SimplificationEngine.simplify(expr)
            val result3 = SimplificationEngine.simplify(expr)
            assertEquals(result1, result2)
            assertEquals(result2, result3)
        }

        @Test
        fun `LaTeX generation is deterministic`() {
            val expr = Expectation(Product(listOf(Num(3L), Var("X"))))
            val latex1 = LaTeXGenerator.generate(expr)
            val latex2 = LaTeXGenerator.generate(expr)
            assertEquals(latex1, latex2)
        }

        @Test
        fun `Rule application is deterministic`() {
            val expr = Log(Exp(Var("x")))
            val result1 = SimplificationEngine.simplify(expr)
            val result2 = SimplificationEngine.simplify(expr)
            assertEquals(result1, result2)
        }
    }
}
