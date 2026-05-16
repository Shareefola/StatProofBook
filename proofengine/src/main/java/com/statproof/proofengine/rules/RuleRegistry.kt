package com.statproof.proofengine.rules

import com.statproof.proofengine.ast.ExpressionNode
import com.statproof.proofengine.ast.ExpressionNode.*

/**
 * A named mathematical transformation rule.
 *
 * Rules map an input [ExpressionNode] to an output [ExpressionNode],
 * returning null if the rule does not apply to the given expression.
 *
 * Rules are purely functional — they never mutate their input.
 */
data class TransformationRule(
    val id: String,
    val name: String,
    val description: String,
    val latexDescription: String = "",
    val applies: (ExpressionNode) -> Boolean,
    val apply: (ExpressionNode) -> ExpressionNode?,
)

/**
 * Global registry of all mathematical transformation rules.
 *
 * Rules are indexed by [TransformationRule.id] for deterministic lookup.
 * The registry is immutable after construction.
 */
object RuleRegistry {

    private val _rules = mutableMapOf<String, TransformationRule>()
    val rules: Map<String, TransformationRule> = _rules

    init {
        registerAll()
    }

    fun get(id: String): TransformationRule? = _rules[id]

    fun getByName(name: String): TransformationRule? = _rules.values.find { it.name == name }

    fun applyRule(id: String, expr: ExpressionNode): ExpressionNode? =
        _rules[id]?.takeIf { it.applies(expr) }?.apply?.invoke(expr)

    private fun register(rule: TransformationRule) {
        _rules[rule.id] = rule
    }

    @Suppress("LongMethod")
    private fun registerAll() {

        // ── Arithmetic Identities ─────────────────────────────────────────────

        register(TransformationRule(
            id = "add_zero",
            name = "Additive Identity",
            description = "a + 0 = a",
            latexDescription = "a + 0 = a",
            applies = { it is Sum && it.terms.any { t -> t.isZero() } },
            apply = { expr ->
                if (expr is Sum) {
                    val nonZero = expr.terms.filter { !it.isZero() }
                    when {
                        nonZero.isEmpty() -> Num.ZERO
                        nonZero.size == 1 -> nonZero[0]
                        else -> Sum(nonZero)
                    }
                } else null
            }
        ))

        register(TransformationRule(
            id = "mul_one",
            name = "Multiplicative Identity",
            description = "a × 1 = a",
            latexDescription = "a \\cdot 1 = a",
            applies = { it is Product && it.factors.any { f -> f.isOne() } },
            apply = { expr ->
                if (expr is Product) {
                    val nonOne = expr.factors.filter { !it.isOne() }
                    when {
                        nonOne.isEmpty() -> Num.ONE
                        nonOne.size == 1 -> nonOne[0]
                        else -> Product(nonOne)
                    }
                } else null
            }
        ))

        register(TransformationRule(
            id = "mul_zero",
            name = "Zero Product",
            description = "a × 0 = 0",
            latexDescription = "a \\cdot 0 = 0",
            applies = { it is Product && it.factors.any { f -> f.isZero() } },
            apply = { Num.ZERO }
        ))

        // ── Logarithm Rules ───────────────────────────────────────────────────

        register(TransformationRule(
            id = "log_product",
            name = "Logarithm of Product",
            description = "ln(a·b) = ln(a) + ln(b)",
            latexDescription = "\\ln(ab) = \\ln(a) + \\ln(b)",
            applies = { it is Log && it.isNatural && it.arg is Product },
            apply = { expr ->
                if (expr is Log && expr.arg is Product) {
                    Sum(expr.arg.factors.map { Log(it) })
                } else null
            }
        ))

        register(TransformationRule(
            id = "log_power",
            name = "Logarithm of Power",
            description = "ln(x^n) = n·ln(x)",
            latexDescription = "\\ln(x^n) = n \\ln(x)",
            applies = { it is Log && it.isNatural && it.arg is Power },
            apply = { expr ->
                if (expr is Log && expr.arg is Power) {
                    Product(listOf(expr.arg.exponent, Log(expr.arg.base)))
                } else null
            }
        ))

        register(TransformationRule(
            id = "log_fraction",
            name = "Logarithm of Fraction",
            description = "ln(a/b) = ln(a) - ln(b)",
            latexDescription = "\\ln\\left(\\frac{a}{b}\\right) = \\ln(a) - \\ln(b)",
            applies = { it is Log && it.isNatural && it.arg is Fraction },
            apply = { expr ->
                if (expr is Log && expr.arg is Fraction) {
                    Sum(listOf(Log(expr.arg.numerator), Negate(Log(expr.arg.denominator))))
                } else null
            }
        ))

        register(TransformationRule(
            id = "log_exp",
            name = "Logarithm of Exponential",
            description = "ln(e^x) = x",
            latexDescription = "\\ln(e^x) = x",
            applies = { it is Log && it.isNatural && it.arg is Exp },
            apply = { expr ->
                if (expr is Log && expr.arg is Exp) expr.arg.arg else null
            }
        ))

        register(TransformationRule(
            id = "exp_sum",
            name = "Exponential of Sum",
            description = "e^(a+b) = e^a · e^b",
            latexDescription = "e^{a+b} = e^a \\cdot e^b",
            applies = { it is Exp && it.arg is Sum },
            apply = { expr ->
                if (expr is Exp && expr.arg is Sum) {
                    Product(expr.arg.terms.map { Exp(it) })
                } else null
            }
        ))

        // ── Expectation Rules (Linearity of Expectation) ──────────────────────

        register(TransformationRule(
            id = "expectation_linearity_sum",
            name = "Linearity of Expectation (Sum)",
            description = "E[X + Y] = E[X] + E[Y]",
            latexDescription = "\\mathbb{E}[X + Y] = \\mathbb{E}[X] + \\mathbb{E}[Y]",
            applies = { it is Expectation && it.expr is Sum },
            apply = { expr ->
                if (expr is Expectation && expr.expr is Sum) {
                    Sum(expr.expr.terms.map { Expectation(it, expr.condition, expr.subscript) })
                } else null
            }
        ))

        register(TransformationRule(
            id = "expectation_linearity_constant",
            name = "Linearity of Expectation (Constant)",
            description = "E[c·X] = c·E[X]",
            latexDescription = "\\mathbb{E}[cX] = c\\,\\mathbb{E}[X]",
            applies = { it is Expectation && it.expr is Product && (it.expr as Product).factors.any { f -> f is Num } },
            apply = { expr ->
                if (expr is Expectation && expr.expr is Product) {
                    val (consts, others) = expr.expr.factors.partition { it is Num }
                    if (consts.isNotEmpty() && others.isNotEmpty()) {
                        val c = if (consts.size == 1) consts[0] else Product(consts)
                        val inner = if (others.size == 1) others[0] else Product(others)
                        Product(listOf(c, Expectation(inner, expr.condition, expr.subscript)))
                    } else null
                } else null
            }
        ))

        register(TransformationRule(
            id = "expectation_constant",
            name = "Expectation of Constant",
            description = "E[c] = c",
            latexDescription = "\\mathbb{E}[c] = c",
            applies = { it is Expectation && (it.expr is Num || it.expr is Const) },
            apply = { expr ->
                if (expr is Expectation) expr.expr else null
            }
        ))

        // ── Variance Rules ────────────────────────────────────────────────────

        register(TransformationRule(
            id = "variance_definition",
            name = "Variance Definition",
            description = "Var(X) = E[X²] - (E[X])²",
            latexDescription = "\\mathrm{Var}(X) = \\mathbb{E}[X^2] - (\\mathbb{E}[X])^2",
            applies = { it is Variance },
            apply = { expr ->
                if (expr is Variance) {
                    val x = expr.expr
                    Sum(listOf(
                        Expectation(Power(x, Num.TWO)),
                        Negate(Power(Expectation(x), Num.TWO))
                    ))
                } else null
            }
        ))

        register(TransformationRule(
            id = "variance_constant",
            name = "Variance of Constant",
            description = "Var(c) = 0",
            latexDescription = "\\mathrm{Var}(c) = 0",
            applies = { it is Variance && (it.expr is Num || it.expr is Const) },
            apply = { Num.ZERO }
        ))

        register(TransformationRule(
            id = "variance_linear",
            name = "Variance of Linear Transform",
            description = "Var(aX + b) = a²·Var(X)",
            latexDescription = "\\mathrm{Var}(aX + b) = a^2\\,\\mathrm{Var}(X)",
            applies = { expr ->
                expr is Variance && expr.expr is Sum &&
                    (expr.expr as Sum).terms.any { it is Num || it is Const }
            },
            apply = { expr ->
                if (expr is Variance && expr.expr is Sum) {
                    val terms = expr.expr.terms
                    val constants = terms.filter { it is Num || it is Const }
                    val variables = terms.filter { it !is Num && it !is Const }
                    if (constants.isNotEmpty() && variables.isNotEmpty()) {
                        // Var(aX + b) = Var(aX) since Var(b) = 0
                        val varPart = if (variables.size == 1) variables[0] else Sum(variables)
                        Variance(varPart)
                    } else null
                } else null
            }
        ))

        // ── Covariance Rules ──────────────────────────────────────────────────

        register(TransformationRule(
            id = "covariance_symmetry",
            name = "Symmetry of Covariance",
            description = "Cov(X, Y) = Cov(Y, X)",
            latexDescription = "\\mathrm{Cov}(X, Y) = \\mathrm{Cov}(Y, X)",
            applies = { it is Covariance },
            apply = { expr ->
                if (expr is Covariance) Covariance(expr.b, expr.a) else null
            }
        ))

        register(TransformationRule(
            id = "covariance_definition",
            name = "Covariance Definition",
            description = "Cov(X,Y) = E[XY] - E[X]E[Y]",
            latexDescription = "\\mathrm{Cov}(X, Y) = \\mathbb{E}[XY] - \\mathbb{E}[X]\\mathbb{E}[Y]",
            applies = { it is Covariance },
            apply = { expr ->
                if (expr is Covariance) {
                    Sum(listOf(
                        Expectation(Product(listOf(expr.a, expr.b))),
                        Negate(Product(listOf(Expectation(expr.a), Expectation(expr.b))))
                    ))
                } else null
            }
        ))

        // ── Probability Rules ─────────────────────────────────────────────────

        register(TransformationRule(
            id = "bayes_theorem",
            name = "Bayes' Theorem",
            description = "P(A|B) = P(B|A)P(A) / P(B)",
            latexDescription = "\\mathbb{P}(A \\mid B) = \\frac{\\mathbb{P}(B \\mid A)\\,\\mathbb{P}(A)}{\\mathbb{P}(B)}",
            applies = { it is Probability && it.condition != null },
            apply = { null } // Application context-dependent, used in step generation
        ))

        // ── Power Rules ───────────────────────────────────────────────────────

        register(TransformationRule(
            id = "power_product",
            name = "Power of Product",
            description = "(ab)^n = a^n · b^n",
            latexDescription = "(ab)^n = a^n b^n",
            applies = { it is Power && it.base is Product },
            apply = { expr ->
                if (expr is Power && expr.base is Product) {
                    Product(expr.base.factors.map { Power(it, expr.exponent) })
                } else null
            }
        ))

        register(TransformationRule(
            id = "power_sum",
            name = "Power of Sum (Binomial)",
            description = "(a+b)^n — binomial expansion",
            latexDescription = "(a+b)^n = \\sum_{k=0}^{n} \\binom{n}{k} a^k b^{n-k}",
            applies = { it is Power && it.base is Sum && it.exponent is Num },
            apply = { null } // Complex: handled in proof expansion engine
        ))

        register(TransformationRule(
            id = "complete_the_square",
            name = "Complete the Square",
            description = "ax² + bx + c = a(x + b/2a)² + c - b²/4a",
            latexDescription = "ax^2 + bx + c = a\\left(x + \\frac{b}{2a}\\right)^2 + c - \\frac{b^2}{4a}",
            applies = { false }, // Used explicitly in Gaussian proof steps
            apply = { null }
        ))

        // ── Matrix Rules ──────────────────────────────────────────────────────

        register(TransformationRule(
            id = "transpose_product",
            name = "Transpose of Product",
            description = "(AB)ᵀ = BᵀAᵀ",
            latexDescription = "(AB)^{\\top} = B^{\\top} A^{\\top}",
            applies = { it is Transpose && it.matrix is Product },
            apply = { expr ->
                if (expr is Transpose && expr.matrix is Product) {
                    Product(expr.matrix.factors.reversed().map { Transpose(it) })
                } else null
            }
        ))

        register(TransformationRule(
            id = "transpose_transpose",
            name = "Double Transpose",
            description = "(Aᵀ)ᵀ = A",
            latexDescription = "(A^{\\top})^{\\top} = A",
            applies = { it is Transpose && it.matrix is Transpose },
            apply = { expr ->
                if (expr is Transpose && expr.matrix is Transpose) expr.matrix.matrix else null
            }
        ))

        register(TransformationRule(
            id = "mgf_derivative",
            name = "MGF Moment Extraction",
            description = "E[Xⁿ] = M_X^(n)(0)",
            latexDescription = "\\mathbb{E}[X^n] = M_X^{(n)}(0)",
            applies = { false }, // Used explicitly in derivation steps
            apply = { null }
        ))
    }
}

private fun ExpressionNode.isZero(): Boolean = this is Num && isZero
private fun ExpressionNode.isOne(): Boolean = this is Num && isOne
