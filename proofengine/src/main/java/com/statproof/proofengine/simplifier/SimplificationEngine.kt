package com.statproof.proofengine.simplifier

import com.statproof.proofengine.ast.ExpressionNode
import com.statproof.proofengine.ast.ExpressionNode.*
import com.statproof.proofengine.ast.isOne
import com.statproof.proofengine.ast.isZero
import java.math.BigDecimal
import java.math.MathContext

/**
 * Algebraic simplification engine.
 *
 * Applies a fixed set of rewrite rules repeatedly until no more rules apply
 * (fixed-point iteration). Each rule maps an [ExpressionNode] to a
 * potentially simpler [ExpressionNode].
 *
 * Rules are applied bottom-up (children first), ensuring full simplification.
 *
 * This is NOT a full computer algebra system — it targets the algebraic
 * manipulations commonly required in statistical derivations:
 *  - Arithmetic on numeric constants
 *  - Identity and zero rules
 *  - Fraction cancellation
 *  - Logarithm and exponent laws
 *  - Statistical identities (E[c] = c, Var[c] = 0, etc.)
 */
object SimplificationEngine {

    private const val MAX_ITERATIONS = 100

    /**
     * Simplify the expression to a normal form.
     *
     * @param expr the expression to simplify
     * @return the simplified expression (may be structurally equal to input)
     */
    fun simplify(expr: ExpressionNode): ExpressionNode {
        var current = expr
        repeat(MAX_ITERATIONS) {
            val next = simplifyOnce(current)
            if (next == current) return current
            current = next
        }
        return current
    }

    /**
     * Apply one round of simplification rules (bottom-up).
     */
    private fun simplifyOnce(expr: ExpressionNode): ExpressionNode {
        // First recursively simplify children
        val simplified = simplifyChildren(expr)
        // Then apply rules to the root
        return applyRules(simplified)
    }

    /** Recursively simplify all child nodes. */
    private fun simplifyChildren(expr: ExpressionNode): ExpressionNode = when (expr) {
        is Num -> expr
        is Var -> expr
        is Const -> expr
        is Sum -> Sum(expr.terms.map { simplifyOnce(it) })
        is Product -> Product(expr.factors.map { simplifyOnce(it) })
        is Fraction -> Fraction(simplifyOnce(expr.numerator), simplifyOnce(expr.denominator))
        is Power -> Power(simplifyOnce(expr.base), simplifyOnce(expr.exponent))
        is Negate -> Negate(simplifyOnce(expr.expr))
        is Log -> Log(simplifyOnce(expr.arg), expr.base?.let { simplifyOnce(it) })
        is Exp -> Exp(simplifyOnce(expr.arg))
        is Sqrt -> Sqrt(simplifyOnce(expr.arg))
        is Abs -> Abs(simplifyOnce(expr.arg))
        is Factorial -> Factorial(simplifyOnce(expr.arg))
        is Integral -> Integral(
            simplifyOnce(expr.integrand), expr.variable,
            expr.lowerBound?.let { simplifyOnce(it) },
            expr.upperBound?.let { simplifyOnce(it) }
        )
        is Derivative -> Derivative(simplifyOnce(expr.expr), expr.variable, expr.order)
        is Summation -> Summation(
            simplifyOnce(expr.expr), expr.variable,
            simplifyOnce(expr.from), simplifyOnce(expr.to)
        )
        is ProductNotation -> ProductNotation(
            simplifyOnce(expr.expr), expr.variable,
            simplifyOnce(expr.from), simplifyOnce(expr.to)
        )
        is Limit -> Limit(simplifyOnce(expr.expr), expr.variable, simplifyOnce(expr.to))
        is Expectation -> Expectation(
            simplifyOnce(expr.expr),
            expr.condition?.let { simplifyOnce(it) },
            expr.subscript
        )
        is Variance -> Variance(
            simplifyOnce(expr.expr),
            expr.condition?.let { simplifyOnce(it) }
        )
        is Covariance -> Covariance(simplifyOnce(expr.a), simplifyOnce(expr.b))
        is Probability -> Probability(simplifyOnce(expr.event), expr.condition?.let { simplifyOnce(it) })
        is Matrix -> Matrix(expr.rows.map { row -> row.map { simplifyOnce(it) } })
        is Transpose -> Transpose(simplifyOnce(expr.matrix))
        is Inverse -> Inverse(simplifyOnce(expr.matrix))
        is Determinant -> Determinant(simplifyOnce(expr.matrix))
        is Trace -> Trace(simplifyOnce(expr.matrix))
        is Norm -> Norm(simplifyOnce(expr.vector), simplifyOnce(expr.order))
        is Subscript -> Subscript(simplifyOnce(expr.expr), simplifyOnce(expr.index))
        is Labeled -> Labeled(simplifyOnce(expr.expr), expr.label)
        is FunctionCall -> FunctionCall(expr.name, expr.args.map { simplifyOnce(it) })
        is Piecewise -> Piecewise(expr.cases.map { (e, c) -> simplifyOnce(e) to c?.let { simplifyOnce(it) } })
        is Indicator -> Indicator(simplifyOnce(expr.condition))
        is Binomial -> Binomial(simplifyOnce(expr.n), simplifyOnce(expr.k))
        is Entropy -> expr
        is KLDivergence -> KLDivergence(simplifyOnce(expr.p), simplifyOnce(expr.q))
        is MutualInformation -> MutualInformation(simplifyOnce(expr.x), simplifyOnce(expr.y))
        is MGF -> MGF(expr.randomVar, simplifyOnce(expr.t))
        is CharacteristicFn -> CharacteristicFn(expr.randomVar, simplifyOnce(expr.t))
        is CDF -> CDF(expr.randomVar, simplifyOnce(expr.x))
        is PDF -> PDF(expr.randomVar, simplifyOnce(expr.x), expr.isContinuous)
    }

    /** Apply all pattern rules to a single node (not recursive). */
    @Suppress("CyclomaticComplexMethod")
    private fun applyRules(expr: ExpressionNode): ExpressionNode = when (expr) {

        // ── Numeric Arithmetic ──────────────────────────────────────────────
        is Sum -> simplifySum(expr)
        is Product -> simplifyProduct(expr)
        is Fraction -> simplifyFraction(expr)
        is Power -> simplifyPower(expr)
        is Negate -> simplifyNegate(expr)

        // ── Logarithm Rules ─────────────────────────────────────────────────
        is Log -> simplifyLog(expr)
        is Exp -> simplifyExp(expr)

        // ── Statistical Identities ──────────────────────────────────────────
        is Expectation -> simplifyExpectation(expr)
        is Variance -> simplifyVariance(expr)
        is Covariance -> simplifyCovariance(expr)

        // ── Square Root ─────────────────────────────────────────────────────
        is Sqrt -> simplifySqrt(expr)

        // All other nodes: no simplification at root level
        else -> expr
    }

    // ── Sum Simplification ────────────────────────────────────────────────────

    private fun simplifySum(sum: Sum): ExpressionNode {
        val flat = flattenSum(sum.terms)
        val (nums, others) = flat.partition { it is Num }

        // Compute numeric sum
        val numSum = nums.fold(BigDecimal.ZERO) { acc, n -> acc + (n as Num).value }

        val result = buildList {
            if (numSum.compareTo(BigDecimal.ZERO) != 0) add(Num(numSum))
            addAll(others)
        }

        return when {
            result.isEmpty() -> Num.ZERO
            result.size == 1 -> result[0]
            else -> Sum(result)
        }
    }

    /** Flatten nested Sums: (a + (b + c)) → [a, b, c] */
    private fun flattenSum(terms: List<ExpressionNode>): List<ExpressionNode> = buildList {
        for (term in terms) {
            when (term) {
                is Sum -> addAll(flattenSum(term.terms))
                is Negate -> {
                    when (val inner = term.expr) {
                        is Num -> add(Num(inner.value.negate()))
                        else -> add(term)
                    }
                }
                else -> add(term)
            }
        }
    }

    // ── Product Simplification ────────────────────────────────────────────────

    private fun simplifyProduct(product: Product): ExpressionNode {
        val flat = flattenProduct(product.factors)

        // If any factor is zero, whole product is zero
        if (flat.any { it.isZero() }) return Num.ZERO

        val (nums, others) = flat.partition { it is Num }
        val numProduct = nums.fold(BigDecimal.ONE) { acc, n -> acc * (n as Num).value }

        val result: List<ExpressionNode> = buildList {
            if (numProduct.compareTo(BigDecimal.ONE) != 0) {
                if (numProduct.compareTo(BigDecimal.ONE.negate()) == 0) {
                    // -1 * others → negate
                    if (others.size == 1) return Negate(others[0])
                    if (others.isNotEmpty()) return Negate(Product(others))
                }
                add(Num(numProduct))
            }
            addAll(others)
        }

        return when {
            result.isEmpty() -> Num.ONE
            result.size == 1 -> result[0]
            else -> Product(result)
        }
    }

    /** Flatten nested Products: (a * (b * c)) → [a, b, c] */
    private fun flattenProduct(factors: List<ExpressionNode>): List<ExpressionNode> = buildList {
        for (factor in factors) {
            when (factor) {
                is Product -> addAll(flattenProduct(factor.factors))
                else -> add(factor)
            }
        }
    }

    // ── Fraction Simplification ───────────────────────────────────────────────

    private fun simplifyFraction(frac: Fraction): ExpressionNode {
        val num = frac.numerator
        val den = frac.denominator

        // n / 1 = n
        if (den.isOne()) return num

        // 0 / n = 0 (for non-zero n)
        if (num.isZero() && !den.isZero()) return Num.ZERO

        // Numeric fraction: reduce by GCD
        if (num is Num && den is Num) {
            val gcd = num.value.toBigInteger().gcd(den.value.toBigInteger())
            if (gcd > java.math.BigInteger.ONE) {
                val newNum = num.value.toBigInteger().divide(gcd)
                val newDen = den.value.toBigInteger().divide(gcd)
                if (newDen == java.math.BigInteger.ONE) return Num(BigDecimal(newNum))
                return Fraction(Num(BigDecimal(newNum)), Num(BigDecimal(newDen)))
            }
        }

        // a/a = 1 (structural equality)
        if (num == den) return Num.ONE

        return frac
    }

    // ── Power Simplification ──────────────────────────────────────────────────

    private fun simplifyPower(power: Power): ExpressionNode {
        val base = power.base
        val exp = power.exponent

        // x^0 = 1
        if (exp.isZero()) return Num.ONE

        // x^1 = x
        if (exp.isOne()) return base

        // 1^n = 1
        if (base.isOne()) return Num.ONE

        // 0^n = 0 (for positive n)
        if (base.isZero() && exp is Num && exp.isPositive) return Num.ZERO

        // Numeric: a^n where n is positive integer
        if (base is Num && exp is Num && exp.isInteger && exp.isPositive) {
            try {
                val result = base.value.pow(exp.value.toInt(), MathContext.DECIMAL128)
                return Num(result)
            } catch (_: ArithmeticException) {
                // Exponent too large, leave symbolic
            }
        }

        // (x^a)^b = x^(a*b)
        if (base is Power) {
            return Power(base.base, Product(listOf(base.exponent, exp)))
        }

        // e^(ln x) = x
        if (base == Const.E && exp is Log && exp.isNatural) return exp.arg

        return power
    }

    // ── Negate Simplification ─────────────────────────────────────────────────

    private fun simplifyNegate(negate: Negate): ExpressionNode = when (val inner = negate.expr) {
        is Num -> Num(inner.value.negate())
        is Negate -> inner.expr // --x = x
        else -> negate
    }

    // ── Logarithm Simplification ──────────────────────────────────────────────

    private fun simplifyLog(log: Log): ExpressionNode {
        val arg = log.arg

        // ln(1) = 0
        if (arg.isOne()) return Num.ZERO

        // ln(e) = 1
        if (arg == Const.E && log.isNatural) return Num.ONE

        // ln(e^x) = x
        if (arg is Exp && log.isNatural) return arg.arg

        // ln(a*b) = ln(a) + ln(b)  — kept symbolic for clarity in proofs

        // log_a(a) = 1
        if (log.base != null && arg == log.base) return Num.ONE

        // ln(x^n) = n * ln(x)
        if (arg is Power && log.isNatural) {
            return Product(listOf(arg.exponent, Log(arg.base)))
        }

        return log
    }

    // ── Exp Simplification ────────────────────────────────────────────────────

    private fun simplifyExp(exp: Exp): ExpressionNode {
        val arg = exp.arg

        // e^0 = 1
        if (arg.isZero()) return Num.ONE

        // e^1 = e
        if (arg.isOne()) return Const.E

        // e^(ln x) = x
        if (arg is Log && arg.isNatural) return arg.arg

        // e^(a + b) = e^a * e^b — kept symbolic

        return exp
    }

    // ── Statistical Simplifications ───────────────────────────────────────────

    private fun simplifyExpectation(e: Expectation): ExpressionNode {
        val inner = e.expr

        // E[c] = c where c is numeric constant
        if (inner is Num) return inner
        if (inner is Const) return inner

        // E[c * X] = c * E[X]
        if (inner is Product) {
            val (consts, others) = inner.factors.partition { it is Num || it is Const }
            if (consts.isNotEmpty() && others.isNotEmpty()) {
                val constPart = if (consts.size == 1) consts[0] else Product(consts)
                val expectationPart = Expectation(
                    if (others.size == 1) others[0] else Product(others),
                    e.condition, e.subscript
                )
                return Product(listOf(constPart, expectationPart))
            }
        }

        // E[X + Y] = E[X] + E[Y] (linearity)
        if (inner is Sum) {
            return Sum(inner.terms.map { Expectation(it, e.condition, e.subscript) })
        }

        return e
    }

    private fun simplifyVariance(v: Variance): ExpressionNode {
        val inner = v.expr

        // Var[c] = 0 where c is constant
        if (inner is Num) return Num.ZERO
        if (inner is Const) return Num.ZERO

        // Var[c * X] = c^2 * Var[X]
        if (inner is Product) {
            val (consts, others) = inner.factors.partition { it is Num }
            if (consts.isNotEmpty() && others.isNotEmpty()) {
                val constPart = consts.fold(Num.ONE as ExpressionNode) { acc, c ->
                    Product(listOf(acc, Power(c, Num.TWO)))
                }
                val varPart = Variance(if (others.size == 1) others[0] else Product(others), v.condition)
                return Product(listOf(simplify(constPart), varPart))
            }
        }

        return v
    }

    private fun simplifyCovariance(cov: Covariance): ExpressionNode {
        // Cov(X, X) = Var(X)
        if (cov.a == cov.b) return Variance(cov.a)

        // Cov(c, X) = 0 for constant c
        if (cov.a is Num || cov.a is Const) return Num.ZERO
        if (cov.b is Num || cov.b is Const) return Num.ZERO

        return cov
    }

    // ── Sqrt Simplification ───────────────────────────────────────────────────

    private fun simplifySqrt(sqrt: Sqrt): ExpressionNode {
        val arg = sqrt.arg

        // √0 = 0
        if (arg.isZero()) return Num.ZERO

        // √1 = 1
        if (arg.isOne()) return Num.ONE

        // √(x^2) = |x|
        if (arg is Power && arg.exponent == Num.TWO) return Abs(arg.base)

        // Numeric: √n for perfect squares
        if (arg is Num && arg.isInteger && arg.isPositive) {
            val n = arg.value.toLong()
            val sqrtN = Math.sqrt(n.toDouble()).toLong()
            if (sqrtN * sqrtN == n) return Num(sqrtN)
        }

        return sqrt
    }
}
