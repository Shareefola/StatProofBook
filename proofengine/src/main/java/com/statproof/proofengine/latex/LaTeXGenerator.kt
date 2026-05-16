package com.statproof.proofengine.latex

import com.statproof.proofengine.ast.ExpressionNode
import com.statproof.proofengine.ast.ExpressionNode.*
import java.math.BigDecimal

/**
 * Converts [ExpressionNode] trees into LaTeX strings suitable for rendering
 * with KaTeX in display mode.
 *
 * The generated LaTeX is human-readable and follows standard mathematical
 * typesetting conventions.
 */
object LaTeXGenerator {

    /**
     * Convert an expression to a LaTeX string.
     *
     * @param expr the expression to render
     * @param displayMode whether to use display-mode sizing for the top level
     * @return LaTeX string ready for KaTeX rendering
     */
    fun generate(expr: ExpressionNode, displayMode: Boolean = true): String {
        val inner = generateInner(expr, precedence = 0)
        return if (displayMode) inner else inner
    }

    /**
     * Wrap LaTeX in a display math environment.
     */
    fun wrapDisplay(latex: String): String = "\\displaystyle $latex"

    /**
     * Generate LaTeX for a full proof step (left = right format).
     */
    fun generateEquality(lhs: ExpressionNode, rhs: ExpressionNode): String {
        return "${generate(lhs)} = ${generate(rhs)}"
    }

    /**
     * Generate aligned multi-line LaTeX for a chain of equalities.
     * Suitable for \begin{aligned}...\end{aligned} environments.
     */
    fun generateAligned(steps: List<Pair<String, ExpressionNode>>): String {
        val sb = StringBuilder("\\begin{aligned}\n")
        steps.forEachIndexed { i, (description, expr) ->
            if (i == 0) {
                sb.append("  &= ${generate(expr)}")
            } else {
                sb.append("  &= ${generate(expr)}")
            }
            if (i < steps.size - 1) sb.append(" \\\\")
            if (description.isNotEmpty()) sb.append(" \\quad \\text{($description)}")
            sb.append("\n")
        }
        sb.append("\\end{aligned}")
        return sb.toString()
    }

    // Operator precedence constants
    private const val PREC_SUM = 1
    private const val PREC_PRODUCT = 2
    private const val PREC_UNARY = 3
    private const val PREC_POWER = 4
    private const val PREC_ATOM = 5

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private fun generateInner(expr: ExpressionNode, precedence: Int): String = when (expr) {

        // ── Leaf Nodes ────────────────────────────────────────────────────────
        is Num -> formatNum(expr)
        is Var -> formatVar(expr)
        is Const -> expr.name

        // ── Arithmetic ────────────────────────────────────────────────────────
        is Sum -> {
            val latex = expr.terms.joinToString(" + ") { term ->
                when (term) {
                    is Negate -> "- ${generateInner(term.expr, PREC_SUM)}"
                    else -> generateInner(term, PREC_SUM)
                }
            }
            if (precedence > PREC_SUM) "\\left($latex\\right)" else latex
        }

        is Product -> {
            val latex = buildProductLatex(expr)
            if (precedence > PREC_PRODUCT) "\\left($latex\\right)" else latex
        }

        is Fraction -> {
            "\\frac{${generateInner(expr.numerator, 0)}}{${generateInner(expr.denominator, 0)}}"
        }

        is Power -> {
            val base = generateInner(expr.base, PREC_POWER)
            val exp = generateInner(expr.exponent, 0)
            val baseStr = if (needsParenForPower(expr.base)) "\\left($base\\right)" else base
            "${baseStr}^{$exp}"
        }

        is Negate -> {
            val inner = generateInner(expr.expr, PREC_UNARY)
            "-$inner"
        }

        // ── Transcendental Functions ──────────────────────────────────────────
        is Log -> {
            val arg = generateInner(expr.arg, 0)
            if (expr.isNatural) {
                "\\ln\\!\\left($arg\\right)"
            } else {
                val base = generateInner(expr.base!!, 0)
                "\\log_{$base}\\!\\left($arg\\right)"
            }
        }

        is Exp -> {
            val arg = generateInner(expr.arg, 0)
            "e^{$arg}"
        }

        is Sqrt -> "\\sqrt{${generateInner(expr.arg, 0)}}"

        is Abs -> "\\left|${generateInner(expr.arg, 0)}\\right|"

        is Factorial -> "${generateInner(expr.arg, PREC_ATOM)}!"

        // ── Calculus ──────────────────────────────────────────────────────────
        is Integral -> {
            val integrand = generateInner(expr.integrand, 0)
            val variable = formatVar(expr.variable)
            if (expr.isDefinite) {
                val lo = generateInner(expr.lowerBound!!, 0)
                val hi = generateInner(expr.upperBound!!, 0)
                "\\int_{$lo}^{$hi} $integrand \\, d$variable"
            } else {
                "\\int $integrand \\, d$variable"
            }
        }

        is Derivative -> {
            val inner = generateInner(expr.expr, 0)
            val v = formatVar(expr.variable)
            if (expr.order == 1) {
                "\\frac{d}{d$v}\\left($inner\\right)"
            } else {
                "\\frac{d^{${expr.order}}}{d${v}^{${expr.order}}}\\left($inner\\right)"
            }
        }

        is Summation -> {
            val e = generateInner(expr.expr, 0)
            val v = formatVar(expr.variable)
            val from = generateInner(expr.from, 0)
            val to = generateInner(expr.to, 0)
            "\\sum_{$v=${from}}^{${to}} $e"
        }

        is ProductNotation -> {
            val e = generateInner(expr.expr, 0)
            val v = formatVar(expr.variable)
            val from = generateInner(expr.from, 0)
            val to = generateInner(expr.to, 0)
            "\\prod_{$v=${from}}^{${to}} $e"
        }

        is Limit -> {
            val e = generateInner(expr.expr, 0)
            val v = formatVar(expr.variable)
            val to = generateInner(expr.to, 0)
            val arrow = when (expr.direction) {
                LimitDirection.FROM_LEFT -> "${to}^-"
                LimitDirection.FROM_RIGHT -> "${to}^+"
                LimitDirection.BOTH -> to
            }
            "\\lim_{$v \\to $arrow} $e"
        }

        // ── Statistical / Probabilistic ───────────────────────────────────────
        is Expectation -> {
            val inner = generateInner(expr.expr, 0)
            val sub = expr.subscript?.let { "_{$it}" } ?: ""
            val cond = expr.condition?.let { " \\mid ${generateInner(it, 0)}" } ?: ""
            "\\mathbb{E}${sub}\\!\\left[$inner$cond\\right]"
        }

        is Variance -> {
            val inner = generateInner(expr.expr, 0)
            val cond = expr.condition?.let { " \\mid ${generateInner(it, 0)}" } ?: ""
            "\\mathrm{Var}\\!\\left($inner$cond\\right)"
        }

        is Covariance -> {
            val a = generateInner(expr.a, 0)
            val b = generateInner(expr.b, 0)
            "\\mathrm{Cov}\\!\\left($a,\\, $b\\right)"
        }

        is Probability -> {
            val event = generateInner(expr.event, 0)
            val cond = expr.condition?.let { " \\mid ${generateInner(it, 0)}" } ?: ""
            "\\mathbb{P}\\!\\left($event$cond\\right)"
        }

        is MGF -> {
            val t = generateInner(expr.t, 0)
            "M_{${expr.randomVar.displayName}}\\!\\left($t\\right)"
        }

        is CharacteristicFn -> {
            val t = generateInner(expr.t, 0)
            "\\varphi_{${expr.randomVar.displayName}}\\!\\left($t\\right)"
        }

        is CDF -> {
            val x = generateInner(expr.x, 0)
            "F_{${expr.randomVar.displayName}}\\!\\left($x\\right)"
        }

        is PDF -> {
            val x = generateInner(expr.x, 0)
            val letter = if (expr.isContinuous) "f" else "p"
            "${letter}_{${expr.randomVar.displayName}}\\!\\left($x\\right)"
        }

        // ── Linear Algebra ────────────────────────────────────────────────────
        is Matrix -> generateMatrix(expr)

        is Transpose -> {
            val m = generateInner(expr.matrix, PREC_ATOM)
            "${m}^{\\top}"
        }

        is Inverse -> {
            val m = generateInner(expr.matrix, PREC_ATOM)
            "${m}^{-1}"
        }

        is Determinant -> {
            val m = generateInner(expr.matrix, 0)
            "\\det\\!\\left($m\\right)"
        }

        is Trace -> {
            val m = generateInner(expr.matrix, 0)
            "\\mathrm{tr}\\!\\left($m\\right)"
        }

        is Norm -> {
            val v = generateInner(expr.vector, 0)
            val ord = generateInner(expr.order, 0)
            "\\left\\|$v\\right\\|_{$ord}"
        }

        // ── Information Theory ────────────────────────────────────────────────
        is Entropy -> "H\\!\\left(${expr.randomVar.displayName}\\right)"

        is KLDivergence -> {
            val p = generateInner(expr.p, 0)
            val q = generateInner(expr.q, 0)
            "D_{\\mathrm{KL}}\\!\\left($p \\,\\|\\, $q\\right)"
        }

        is MutualInformation -> {
            val x = generateInner(expr.x, 0)
            val y = generateInner(expr.y, 0)
            "I\\!\\left($x\\,;\\,$y\\right)"
        }

        // ── Misc ──────────────────────────────────────────────────────────────
        is Indicator -> {
            val cond = generateInner(expr.condition, 0)
            "\\mathbf{1}\\!\\left[$cond\\right]"
        }

        is Piecewise -> {
            val cases = expr.cases.joinToString(" \\\\\n") { (e, cond) ->
                val eStr = generateInner(e, 0)
                val condStr = cond?.let { "& \\text{if } ${generateInner(it, 0)}" } ?: "& \\text{otherwise}"
                "$eStr $condStr"
            }
            "\\begin{cases}\n$cases\n\\end{cases}"
        }

        is FunctionCall -> {
            val args = expr.args.joinToString(",\\, ") { generateInner(it, 0) }
            "\\mathrm{${expr.name}}\\!\\left($args\\right)"
        }

        is Binomial -> {
            val n = generateInner(expr.n, 0)
            val k = generateInner(expr.k, 0)
            "\\binom{$n}{$k}"
        }

        is Subscript -> {
            val e = generateInner(expr.expr, PREC_ATOM)
            val idx = generateInner(expr.index, 0)
            "${e}_{$idx}"
        }

        is Labeled -> generateInner(expr.expr, precedence)
    }

    private fun formatNum(num: Num): String {
        val bd = num.value.stripTrailingZeros()
        return if (bd.scale() <= 0) {
            bd.toBigIntegerExact().toString()
        } else {
            bd.toPlainString()
        }
    }

    private fun formatVar(v: Var): String = if (v.subscript != null) {
        "${v.name}_{${v.subscript}}"
    } else {
        v.name
    }

    private fun buildProductLatex(product: Product): String {
        val parts = mutableListOf<String>()
        var denominator: ExpressionNode? = null

        // Check if product contains a fraction component
        val (fracs, others) = product.factors.partition { it is Fraction }

        if (fracs.size == 1 && others.isNotEmpty()) {
            val frac = fracs[0] as Fraction
            val numParts = buildList {
                add(generateInner(frac.numerator, PREC_PRODUCT))
                others.forEach { add(generateInner(it, PREC_PRODUCT)) }
            }
            return "\\frac{${numParts.joinToString(" ")}}{${generateInner(frac.denominator, 0)}}"
        }

        for (factor in product.factors) {
            val factorStr = when {
                factor is Power && factor.exponent is Num && (factor.exponent as Num).isNegativeOne -> {
                    denominator = factor.base
                    null
                }
                factor is Num && factor.value.compareTo(BigDecimal.ONE.negate()) == 0 -> {
                    parts.add(0, "-")
                    null
                }
                else -> generateInner(factor, PREC_PRODUCT)
            }
            factorStr?.let { parts.add(it) }
        }

        val numeratorStr = parts.joinToString(" \\cdot ")
        return if (denominator != null) {
            "\\frac{$numeratorStr}{${generateInner(denominator, 0)}}"
        } else {
            numeratorStr
        }
    }

    private fun generateMatrix(m: Matrix): String {
        val sb = StringBuilder("\\begin{pmatrix}\n")
        m.rows.forEachIndexed { i, row ->
            sb.append("  ")
            sb.append(row.joinToString(" & ") { generateInner(it, 0) })
            if (i < m.rows.size - 1) sb.append(" \\\\")
            sb.append("\n")
        }
        sb.append("\\end{pmatrix}")
        return sb.toString()
    }

    private fun needsParenForPower(base: ExpressionNode): Boolean = when (base) {
        is Num -> base.isNegative || !base.isInteger
        is Sum -> true
        is Product -> true
        is Fraction -> true
        is Negate -> true
        else -> false
    }
}
