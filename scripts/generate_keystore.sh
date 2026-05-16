#!/usr/bin/env bash
# =============================================================================
# generate_keystore.sh
#
# Generates a release signing keystore for StatProof and prints the
# base64-encoded value ready to paste into GitHub Secrets.
#
# Usage:
#   chmod +x scripts/generate_keystore.sh
#   ./scripts/generate_keystore.sh
#
# ⚠ IMPORTANT: Store the keystore file and password securely.
#   Anyone with the keystore + password can sign APKs as StatProof.
# =============================================================================

set -euo pipefail

KEYSTORE_FILE="statproof-release.jks"
KEY_ALIAS="statproof-key"
VALIDITY_DAYS=10000

echo "▶ Generating StatProof release keystore..."
echo ""
echo "You will be prompted for:"
echo "  - A keystore password (remember this)"
echo "  - A key password (can be same as keystore password)"
echo "  - Your name / organisation details"
echo ""

keytool -genkey -v \
  -keystore "${KEYSTORE_FILE}" \
  -alias "${KEY_ALIAS}" \
  -keyalg RSA \
  -keysize 4096 \
  -validity "${VALIDITY_DAYS}" \
  -storetype JKS

echo ""
echo "▶ Encoding keystore to base64 for GitHub Secrets..."
echo ""
echo "============================================================"
echo "ANDROID_KEYSTORE_BASE64 (paste this into GitHub Secrets):"
echo "============================================================"
base64 -i "${KEYSTORE_FILE}"
echo ""
echo "============================================================"
echo ""
echo "▶ Add these secrets to your GitHub repository:"
echo "  Settings → Secrets and variables → Actions → New repository secret"
echo ""
echo "  ANDROID_KEYSTORE_BASE64  = (the base64 value above)"
echo "  ANDROID_KEYSTORE_PASSWORD = (your keystore password)"
echo "  ANDROID_KEY_ALIAS         = ${KEY_ALIAS}"
echo "  ANDROID_KEY_PASSWORD      = (your key password)"
echo ""
echo "⚠  The keystore file '${KEYSTORE_FILE}' has been created locally."
echo "   Store it securely and do NOT commit it to version control."
echo "   Add '*.jks' and '*.keystore' to your .gitignore."
