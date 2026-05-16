package com.statproof.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Color Palette ─────────────────────────────────────────────────────────────

// Primary — Deep indigo (mathematical, academic)
val PrimaryLight = Color(0xFF3F51B5)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFE8EAF6)
val OnPrimaryContainerLight = Color(0xFF1A237E)

// Secondary — Teal accent
val SecondaryLight = Color(0xFF00897B)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFE0F2F1)
val OnSecondaryContainerLight = Color(0xFF004D40)

// Tertiary — Amber (highlight, formula emphasis)
val TertiaryLight = Color(0xFFF57F17)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFFFF8E1)
val OnTertiaryContainerLight = Color(0xFF7F4E00)

// Error
val ErrorLight = Color(0xFFB00020)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)

// Background & Surface (light)
val BackgroundLight = Color(0xFFFAFAFF)
val OnBackgroundLight = Color(0xFF1A1C2E)
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF1A1C2E)
val SurfaceVariantLight = Color(0xFFE8EAF6)
val OnSurfaceVariantLight = Color(0xFF3C4073)

// Dark scheme colors
val PrimaryDark = Color(0xFF7986CB)
val OnPrimaryDark = Color(0xFF1A237E)
val PrimaryContainerDark = Color(0xFF283593)
val OnPrimaryContainerDark = Color(0xFFE8EAF6)

val SecondaryDark = Color(0xFF4DB6AC)
val OnSecondaryDark = Color(0xFF004D40)
val SecondaryContainerDark = Color(0xFF00695C)
val OnSecondaryContainerDark = Color(0xFFE0F2F1)

val TertiaryDark = Color(0xFFFFCC80)
val OnTertiaryDark = Color(0xFF7F4E00)
val TertiaryContainerDark = Color(0xFFF57F17)
val OnTertiaryContainerDark = Color(0xFFFFF8E1)

val BackgroundDark = Color(0xFF0E1020)
val OnBackgroundDark = Color(0xFFE8EAF6)
val SurfaceDark = Color(0xFF161828)
val OnSurfaceDark = Color(0xFFE8EAF6)
val SurfaceVariantDark = Color(0xFF2A2D4A)
val OnSurfaceVariantDark = Color(0xFFB0B8E8)

// ── Color Schemes ─────────────────────────────────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = Color(0xFF7E84C0),
    outlineVariant = Color(0xFFBCC0E8),
    scrim = Color(0xFF000000),
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = Color(0xFF7E84C0),
    outlineVariant = Color(0xFF3C4073),
    scrim = Color(0xFF000000),
)

/**
 * StatProof application theme.
 *
 * Supports:
 * - System dark mode
 * - Dynamic color (Android 12+)
 * - Edge-to-edge rendering
 */
@Composable
fun StatProofTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = StatProofTypography,
        content = content,
    )
}
