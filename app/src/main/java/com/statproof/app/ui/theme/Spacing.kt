package com.statproof.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * StatProof spacing design tokens.
 *
 * All spacing values in the UI should reference these tokens
 * rather than hardcoded dp values, ensuring visual consistency.
 */
data class Spacing(
    val none: Dp = 0.dp,
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 48.dp,
    val huge: Dp = 64.dp,
    // Content padding
    val screenHorizontal: Dp = 16.dp,
    val screenVertical: Dp = 16.dp,
    val cardPadding: Dp = 16.dp,
    val sectionGap: Dp = 24.dp,
    val itemGap: Dp = 12.dp,
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }
