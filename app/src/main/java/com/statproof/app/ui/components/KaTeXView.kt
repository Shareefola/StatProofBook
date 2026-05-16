package com.statproof.app.ui.components

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.MaterialTheme
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

/**
 * Composable that renders a LaTeX expression using KaTeX loaded from local assets.
 *
 * The KaTeX bundle lives at assets/katex/. No network access is performed.
 *
 * @param latex the LaTeX string to render (do NOT include $…$ or \[…\] delimiters)
 * @param modifier the Compose modifier
 * @param displayMode if true, uses display (block) math mode; if false uses inline
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun KaTeXView(
    latex: String,
    modifier: Modifier = Modifier,
    displayMode: Boolean = true,
) {
    val isDark = isSystemInDarkTheme()
    val backgroundColor = MaterialTheme.colorScheme.surface.toArgb()
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()

    val escapedLatex = remember(latex) { escapeForJs(latex) }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = false
                    allowFileAccessFromFileURLs = true
                    allowUniversalAccessFromFileURLs = true
                    cacheMode = WebSettings.LOAD_NO_CACHE
                    useWideViewPort = false
                    loadWithOverviewMode = false
                    setSupportZoom(false)
                    builtInZoomControls = false
                    displayZoomControls = false
                }

                // Enable dark mode for WebView if available
                if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                    WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, isDark)
                }

                isHorizontalScrollBarEnabled = false
                isVerticalScrollBarEnabled = false
                setBackgroundColor(backgroundColor)
            }
        },
        update = { webView ->
            val html = buildKatexHtml(
                latex = escapedLatex,
                displayMode = displayMode,
                isDark = isDark,
                bgColor = colorToHex(backgroundColor),
                textColor = colorToHex(textColor),
            )
            webView.loadDataWithBaseURL(
                "file:///android_asset/",
                html,
                "text/html",
                "UTF-8",
                null,
            )
        },
        modifier = modifier,
    )
}

private fun buildKatexHtml(
    latex: String,
    displayMode: Boolean,
    isDark: Boolean,
    bgColor: String,
    textColor: String,
): String = """
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1, user-scalable=no">
<link rel="stylesheet" href="katex/katex.min.css">
<script defer src="katex/katex.min.js"></script>
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; }
  html, body {
    background: $bgColor;
    color: $textColor;
    font-size: 16px;
    overflow: hidden;
    width: 100%;
  }
  .katex-container {
    padding: 4px 8px;
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 40px;
    width: 100%;
    overflow-x: auto;
  }
  .katex { color: $textColor; }
  .katex-error { color: #e53935; font-size: 12px; font-family: monospace; }
</style>
</head>
<body>
<div class="katex-container" id="math"></div>
<script>
  window.onload = function() {
    try {
      katex.render(
        "${latex}",
        document.getElementById("math"),
        {
          displayMode: $displayMode,
          throwOnError: false,
          strict: false,
          trust: false,
          maxSize: 10,
          maxExpand: 1000
        }
      );
    } catch(e) {
      document.getElementById("math").innerHTML =
        '<span class="katex-error">Render error: ' + e.message + '</span>';
    }
  };
</script>
</body>
</html>
""".trimIndent()

/** Escape a LaTeX string so it can be safely embedded in a JavaScript string literal. */
private fun escapeForJs(latex: String): String = latex
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
    .replace("\n", "\\n")
    .replace("\r", "")
    .replace("`", "\\`")

private fun colorToHex(argb: Int): String {
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    return "#%02x%02x%02x".format(r, g, b)
}
