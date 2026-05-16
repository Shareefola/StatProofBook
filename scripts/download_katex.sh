#!/usr/bin/env bash
# =============================================================================
# download_katex.sh
#
# Downloads KaTeX 0.16.x and places it in the Android assets directory.
# Run this script ONCE before building, or add it to your CI pipeline.
#
# Usage:
#   chmod +x scripts/download_katex.sh
#   ./scripts/download_katex.sh
#
# Requirements: curl, unzip (standard on Linux/macOS)
# =============================================================================

set -euo pipefail

KATEX_VERSION="0.16.11"
KATEX_URL="https://github.com/KaTeX/KaTeX/releases/download/v${KATEX_VERSION}/katex.tar.gz"
ASSETS_DIR="app/src/main/assets/katex"
TMP_DIR=$(mktemp -d)

echo "▶ Downloading KaTeX ${KATEX_VERSION}..."
curl -L --retry 3 --retry-delay 2 -o "${TMP_DIR}/katex.tar.gz" "${KATEX_URL}"

echo "▶ Extracting..."
tar -xzf "${TMP_DIR}/katex.tar.gz" -C "${TMP_DIR}"

echo "▶ Installing to ${ASSETS_DIR}..."
mkdir -p "${ASSETS_DIR}/fonts"

# Copy core files
cp "${TMP_DIR}/katex/katex.min.js"  "${ASSETS_DIR}/katex.min.js"
cp "${TMP_DIR}/katex/katex.min.css" "${ASSETS_DIR}/katex.min.css"

# Copy fonts (required for KaTeX to render correctly)
cp "${TMP_DIR}/katex/fonts/"*.woff2 "${ASSETS_DIR}/fonts/" 2>/dev/null || true
cp "${TMP_DIR}/katex/fonts/"*.woff  "${ASSETS_DIR}/fonts/" 2>/dev/null || true
cp "${TMP_DIR}/katex/fonts/"*.ttf   "${ASSETS_DIR}/fonts/" 2>/dev/null || true

# Cleanup
rm -rf "${TMP_DIR}"

echo "▶ Generating render template..."
cat > "${ASSETS_DIR}/render.html" << 'HTML'
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<link rel="stylesheet" href="katex.min.css">
<script defer src="katex.min.js"></script>
</head>
<body>
<div id="math"></div>
<script>
window.renderMath = function(latex, displayMode) {
    katex.render(latex, document.getElementById("math"), {
        displayMode: displayMode || true,
        throwOnError: false
    });
};
</script>
</body>
</html>
HTML

echo "✅ KaTeX ${KATEX_VERSION} installed to ${ASSETS_DIR}"
echo ""
echo "Files installed:"
find "${ASSETS_DIR}" -type f | sort | sed 's/^/  /'
