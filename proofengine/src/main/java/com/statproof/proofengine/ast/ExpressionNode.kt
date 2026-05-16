package com.statproof.proofengine.ast

import java.math.BigDecimal
import java.math.MathContext

/**
 * Algebraic expression tree.
 *
 * Every mathematical expression in StatProof is represented as an immutable
 * tree of [ExpressionNode] instances. All nodes are sealed subclasses,
 * ensuring exhaustive pattern matching in the simplification and
 * LaTeX generation engines.
 *
 * Design principles:
 *  - Immutable: transformations produce new trees, never mutate
 *  - Sealed: all variants known at compile time
 *  - Recursive: composite nodes hold child nodes
 *  - Comparable: structural equality via data class
 */
sealed class ExpressionNode {

    // ── Leaf Nodes ────────────────────────────────────────────────────────────

    /**
     * A rational numeric constant.
     *
     * @param value the exact decimal value
     */
    data class Num(val value: BigDecimal) : ExpressionNode() {
        constructor(long: Long) : this(BigDecimal.valueOf(long))
        constructor(int: Int) : this(BigDecimal.valueOf(int.toLong()))
        constructor(double: Double) : this(BigDecimal(double, MathContext.DECIMAL128))
        constructor(string: String) : this(BigDecimal(string))

        val isZero: Boolean get() = value.compareTo(BigDecimal.ZERO) == 0
        val isOne: Boolean get() = value.compareTo(BigDecimal.ONE) == 0
        val isNegativeOne: Boolean get() = value.compareTo(BigDecimal.ONE.negate()) == 0
        val isPositive: Boolean get() = value > BigDecimal.ZERO
        val isNegative: Boolean get() = value < BigDecimal.ZERO
        val isInteger: Boolean get() = value.stripTrailingZeros().scale() <= 0

        companion object {
            val ZERO = Num(BigDecimal.ZERO)
            val ONE = Num(BigDecimal.ONE)
            val TWO = Num(2L)
            val NEGATIVE_ONE = Num(-1L)
            val ONE_HALF = Num("0.5")
        }
    }

    /**
     * A symbolic variable (e.g., x, μ, σ², X_i).
     *
     * @param name the variable name — may include Greek letters or subscripts
     * @param subscript optional subscript identifier
     */
    data class Var(
        val name: String,
        val subscript: String? = null,
    ) : ExpressionNode() {
        val displayName: String get() = if (subscript != null) "${name}_${subscript}" else name

        companion object {
            // Common statistical variables
            val X = Var("X")
            val Y = Var("Y")
            val N = Var("n")
            val K = Var("k")
            val P = Var("p")
            val MU = Var("\\mu")
            val SIGMA = Var("\\sigma")
            val SIGMA_SQ = Var("\\sigma", "2")
            val LAMBDA = Var("\\lambda")
            val THETA = Var("\\theta")
            val ALPHA = Var("\\alpha")
            val BETA = Var("\\beta")
            val PI = Var("\\pi")
        }
    }

    /**
     * Mathematical constant (e, π, etc.).
     */
    data class Const(val name: String) : ExpressionNode() {
        companion object {
            val E = Const("e")
            val PI = Const("\\pi")
            val INFINITY = Const("\\infty")
            val NEG_INFINITY = Const("-\\infty")
        }
    }

    // ── Arithmetic Nodes ──────────────────────────────────────────────────────

    /**
     * Sum of two or more terms: a + b + c + …
     *
     * Stored as a flat list (not binary tree) to simplify commutativity.
     * Invariant: [terms] always has at least 2 elements.
     */
    data class Sum(val terms: List<ExpressionNode>) : ExpressionNode() {
        constructor(vararg nodes: ExpressionNode) : this(nodes.toList())

        companion object {
            fun of(vararg nodes: ExpressionNode): Sum = Sum(nodes.toList())
        }
    }

    /**
     * Product of two or more factors: a × b × c × …
     *
     * Invariant: [factors] always has at least 2 elements.
     */
    data class Product(val factors: List<ExpressionNode>) : ExpressionNode() {
        constructor(vararg nodes: ExpressionNode) : this(nodes.toList())

        companion object {
            fun of(vararg nodes: ExpressionNode): Product = Product(nodes.toList())
        }
    }

    /**
     * Ratio / fraction: numerator / denominator.
     */
    data class Fraction(
        val numerator: ExpressionNode,
        val denominator: ExpressionNode,
    ) : ExpressionNode()

    /**
     * Exponentiation: base ^ exponent.
     */
    data class Power(
        val base: ExpressionNode,
        val exponent: ExpressionNode,
    ) : ExpressionNode()

    /**
     * Negation: -expr.
     * Represented as Product(Num(-1), expr) in normal form,
     * but kept separate for LaTeX clarity.
     */
    data class Negate(val expr: ExpressionNode) : ExpressionNode()

    // ── Transcendental Functions ──────────────────────────────────────────────

    /**
     * Natural logarithm (base e) or logarithm with explicit base.
     *
     * @param arg the argument of the logarithm
     * @param base null means natural log (ln)
     */
    data class Log(
        val arg: ExpressionNode,
        val base: ExpressionNode? = null,
    ) : ExpressionNode() {
        val isNatural: Boolean get() = base == null
    }

    /**
     * Exponential function: e^arg.
     */
    data class Exp(val arg: ExpressionNode) : ExpressionNode()

    /**
     * Square root: √arg.
     */
    data class Sqrt(val arg: ExpressionNode) : ExpressionNode()

    /**
     * Absolute value: |arg|.
     */
    data class Abs(val arg: ExpressionNode) : ExpressionNode()

    /**
     * Factorial: n!
     */
    data class Factorial(val arg: ExpressionNode) : ExpressionNode()

    // ── Calculus Nodes ────────────────────────────────────────────────────────

    /**
     * Definite or indefinite integral.
     *
     * @param integrand the expression being integrated
     * @param variable the integration variable
     * @param lowerBound null for indefinite integral
     * @param upperBound null for indefinite integral
     */
    data class Integral(
        val integrand: ExpressionNode,
        val variable: Var,
        val lowerBound: ExpressionNode? = null,
        val upperBound: ExpressionNode? = null,
    ) : ExpressionNode() {
        val isDefinite: Boolean get() = lowerBound != null && upperBound != null
    }

    /**
     * Derivative.
     *
     * @param expr the expression being differentiated
     * @param variable the differentiation variable
     * @param order order of differentiation (1 = first derivative)
     */
    data class Derivative(
        val expr: ExpressionNode,
        val variable: Var,
        val order: Int = 1,
    ) : ExpressionNode() {
        init {
            require(order >= 1) { "Derivative order must be >= 1, got $order" }
        }
    }

    /**
     * Discrete summation: Σ_{var=from}^{to} expr
     */
    data class Summation(
        val expr: ExpressionNode,
        val variable: Var,
        val from: ExpressionNode,
        val to: ExpressionNode,
    ) : ExpressionNode()

    /**
     * Product notation: Π_{var=from}^{to} expr
     */
    data class ProductNotation(
        val expr: ExpressionNode,
        val variable: Var,
        val from: ExpressionNode,
        val to: ExpressionNode,
    ) : ExpressionNode()

    /**
     * Limit: lim_{var → to} expr
     */
    data class Limit(
        val expr: ExpressionNode,
        val variable: Var,
        val to: ExpressionNode,
        val direction: LimitDirection = LimitDirection.BOTH,
    ) : ExpressionNode()

    enum class LimitDirection { FROM_LEFT, FROM_RIGHT, BOTH }

    // ── Statistical / Probabilistic Nodes ────────────────────────────────────

    /**
     * Expected value: E[expr] or E[expr | condition].
     */
    data class Expectation(
        val expr: ExpressionNode,
        val condition: ExpressionNode? = null,
        val subscript: String? = null,
    ) : ExpressionNode()

    /**
     * Variance: Var(expr) or Var(expr | condition).
     */
    data class Variance(
        val expr: ExpressionNode,
        val condition: ExpressionNode? = null,
    ) : ExpressionNode()

    /**
     * Covariance: Cov(a, b).
     */
    data class Covariance(
        val a: ExpressionNode,
        val b: ExpressionNode,
    ) : ExpressionNode()

    /**
     * Probability: P(event) or P(event | condition).
     */
    data class Probability(
        val event: ExpressionNode,
        val condition: ExpressionNode? = null,
    ) : ExpressionNode()

    /**
     * Moment generating function: M_X(t).
     */
    data class MGF(
        val randomVar: Var,
        val t: ExpressionNode,
    ) : ExpressionNode()

    /**
     * Characteristic function: φ_X(t).
     */
    data class CharacteristicFn(
        val randomVar: Var,
        val t: ExpressionNode,
    ) : ExpressionNode()

    /**
     * Cumulative distribution function: F_X(x).
     */
    data class CDF(
        val randomVar: Var,
        val x: ExpressionNode,
    ) : ExpressionNode()

    /**
     * Probability density/mass function: f_X(x) or p_X(x).
     */
    data class PDF(
        val randomVar: Var,
        val x: ExpressionNode,
        val isContinuous: Boolean = true,
    ) : ExpressionNode()

    // ── Linear Algebra Nodes ──────────────────────────────────────────────────

    /**
     * Matrix expression.
     *
     * @param rows rows of the matrix, each row is a list of expressions
     */
    data class Matrix(val rows: List<List<ExpressionNode>>) : ExpressionNode() {
        val rowCount: Int get() = rows.size
        val colCount: Int get() = rows.firstOrNull()?.size ?: 0

        fun get(row: Int, col: Int): ExpressionNode = rows[row][col]

        val isSquare: Boolean get() = rowCount == colCount
        val isVector: Boolean get() = colCount == 1 || rowCount == 1
    }

    /**
     * Matrix transpose: Xᵀ.
     */
    data class Transpose(val matrix: ExpressionNode) : ExpressionNode()

    /**
     * Matrix inverse: X⁻¹.
     */
    data class Inverse(val matrix: ExpressionNode) : ExpressionNode()

    /**
     * Matrix determinant: det(X).
     */
    data class Determinant(val matrix: ExpressionNode) : ExpressionNode()

    /**
     * Matrix trace: tr(X).
     */
    data class Trace(val matrix: ExpressionNode) : ExpressionNode()

    /**
     * Vector norm: ||x||_p.
     */
    data class Norm(
        val vector: ExpressionNode,
        val order: ExpressionNode = Num.TWO,
    ) : ExpressionNode()

    // ── Information Theory Nodes ──────────────────────────────────────────────

    /**
     * Shannon entropy: H(X) = -Σ p(x) log p(x).
     */
    data class Entropy(val randomVar: Var) : ExpressionNode()

    /**
     * KL divergence: D_KL(P || Q).
     */
    data class KLDivergence(
        val p: ExpressionNode,
        val q: ExpressionNode,
    ) : ExpressionNode()

    /**
     * Mutual information: I(X; Y).
     */
    data class MutualInformation(
        val x: ExpressionNode,
        val y: ExpressionNode,
    ) : ExpressionNode()

    // ── Logical / Conditional Nodes ───────────────────────────────────────────

    /**
     * Indicator function: 𝟙[condition].
     */
    data class Indicator(val condition: ExpressionNode) : ExpressionNode()

    /**
     * Piecewise-defined function.
     *
     * @param cases list of (expression, condition) pairs, last condition may be null (else case)
     */
    data class Piecewise(val cases: List<Pair<ExpressionNode, ExpressionNode?>>) : ExpressionNode()

    /**
     * Named function call: f(args).
     *
     * Used for functions without a dedicated node (e.g. Gamma, Beta, erf).
     */
    data class FunctionCall(
        val name: String,
        val args: List<ExpressionNode>,
    ) : ExpressionNode() {
        constructor(name: String, vararg args: ExpressionNode) : this(name, args.toList())
    }

    /**
     * Binomial coefficient: C(n, k).
     */
    data class Binomial(
        val n: ExpressionNode,
        val k: ExpressionNode,
    ) : ExpressionNode()

    /**
     * Subscripted expression: expr_subscript (used for indexed sequences).
     */
    data class Subscript(
        val expr: ExpressionNode,
        val index: ExpressionNode,
    ) : ExpressionNode()

    /**
     * Annotated expression with a human-readable label.
     * Used to mark intermediate steps with semantic tags.
     */
    data class Labeled(
        val expr: ExpressionNode,
        val label: String,
    ) : ExpressionNode()
}

// ── Extension Functions ───────────────────────────────────────────────────────

/** Returns true if this expression is structurally zero. */
fun ExpressionNode.isZero(): Boolean = this is ExpressionNode.Num && this.isZero

/** Returns true if this expression is structurally one. */
fun ExpressionNode.isOne(): Boolean = this is ExpressionNode.Num && this.isOne

/** Returns true if this expression is a numeric constant. */
fun ExpressionNode.isNumeric(): Boolean = this is ExpressionNode.Num

/** Returns true if this expression contains the given variable. */
fun ExpressionNode.containsVar(v: ExpressionNode.Var): Boolean = when (this) {
    is ExpressionNode.Var -> this == v
    is ExpressionNode.Num -> false
    is ExpressionNode.Const -> false
    is ExpressionNode.Sum -> terms.any { it.containsVar(v) }
    is ExpressionNode.Product -> factors.any { it.containsVar(v) }
    is ExpressionNode.Fraction -> numerator.containsVar(v) || denominator.containsVar(v)
    is ExpressionNode.Power -> base.containsVar(v) || exponent.containsVar(v)
    is ExpressionNode.Negate -> expr.containsVar(v)
    is ExpressionNode.Log -> arg.containsVar(v)
    is ExpressionNode.Exp -> arg.containsVar(v)
    is ExpressionNode.Sqrt -> arg.containsVar(v)
    is ExpressionNode.Abs -> arg.containsVar(v)
    is ExpressionNode.Factorial -> arg.containsVar(v)
    is ExpressionNode.Integral -> integrand.containsVar(v) && variable != v
    is ExpressionNode.Derivative -> expr.containsVar(v)
    is ExpressionNode.Summation -> expr.containsVar(v) && variable != v
    is ExpressionNode.ProductNotation -> expr.containsVar(v) && variable != v
    is ExpressionNode.Limit -> expr.containsVar(v)
    is ExpressionNode.Expectation -> expr.containsVar(v)
    is ExpressionNode.Variance -> expr.containsVar(v)
    is ExpressionNode.Covariance -> a.containsVar(v) || b.containsVar(v)
    is ExpressionNode.Probability -> event.containsVar(v)
    is ExpressionNode.Matrix -> rows.any { row -> row.any { it.containsVar(v) } }
    is ExpressionNode.Transpose -> matrix.containsVar(v)
    is ExpressionNode.Inverse -> matrix.containsVar(v)
    is ExpressionNode.Determinant -> matrix.containsVar(v)
    is ExpressionNode.Trace -> matrix.containsVar(v)
    is ExpressionNode.Subscript -> expr.containsVar(v)
    is ExpressionNode.Labeled -> expr.containsVar(v)
    is ExpressionNode.FunctionCall -> args.any { it.containsVar(v) }
    is ExpressionNode.Piecewise -> cases.any { (e, c) -> e.containsVar(v) || (c?.containsVar(v) == true) }
    is ExpressionNode.Indicator -> condition.containsVar(v)
    is ExpressionNode.Binomial -> n.containsVar(v) || k.containsVar(v)
    is ExpressionNode.Norm -> vector.containsVar(v)
    is ExpressionNode.MGF -> t.containsVar(v)
    is ExpressionNode.CharacteristicFn -> t.containsVar(v)
    is ExpressionNode.CDF -> x.containsVar(v)
    is ExpressionNode.PDF -> x.containsVar(v)
    is ExpressionNode.Entropy -> false
    is ExpressionNode.KLDivergence -> p.containsVar(v) || q.containsVar(v)
    is ExpressionNode.MutualInformation -> x.containsVar(v) || y.containsVar(v)
    is ExpressionNode.Determinant -> matrix.containsVar(v)
}

/** Convenience operator overloads for building expression trees readably. */
operator fun ExpressionNode.plus(other: ExpressionNode): ExpressionNode =
    ExpressionNode.Sum(listOf(this, other))

operator fun ExpressionNode.minus(other: ExpressionNode): ExpressionNode =
    ExpressionNode.Sum(listOf(this, ExpressionNode.Negate(other)))

operator fun ExpressionNode.times(other: ExpressionNode): ExpressionNode =
    ExpressionNode.Product(listOf(this, other))

operator fun ExpressionNode.div(other: ExpressionNode): ExpressionNode =
    ExpressionNode.Fraction(this, other)

operator fun ExpressionNode.unaryMinus(): ExpressionNode =
    ExpressionNode.Negate(this)

fun ExpressionNode.pow(exp: ExpressionNode): ExpressionNode =
    ExpressionNode.Power(this, exp)

fun ExpressionNode.pow(exp: Int): ExpressionNode =
    ExpressionNode.Power(this, ExpressionNode.Num(exp))
