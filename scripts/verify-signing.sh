#!/usr/bin/env bash
set -euo pipefail

echo "========================================================"
echo "Persona Production APK / AAB Signature Verification"
echo "========================================================"

TARGET_FILE="${1:-}"
EXPECTED_CERT_FINGERPRINT="${2:-}"

# 1. Reject missing or non-existent target artifact
if [ -z "$TARGET_FILE" ]; then
    echo "CRITICAL ERROR: No target artifact supplied to verify-signing.sh!"
    exit 1
fi

if [ ! -f "$TARGET_FILE" ]; then
    echo "CRITICAL ERROR: Target artifact '$TARGET_FILE' does not exist!"
    exit 1
fi

# 2. Require apksigner tool to be installed and available
if ! command -v apksigner >/dev/null 2>&1; then
    echo "CRITICAL ERROR: apksigner tool is not available in PATH. Cannot verify signature!"
    exit 1
fi

echo "Verifying signature on artifact: $TARGET_FILE"

# 3. Verify signature validity (reject unsigned or corrupted artifacts)
if ! apksigner verify --verbose "$TARGET_FILE"; then
    echo "CRITICAL ERROR: apksigner verification failed! Artifact signature is invalid or unsigned."
    exit 1
fi
echo "✓ Signature scheme verification passed."

# 4. If an expected certificate fingerprint is provided, verify it strictly
if [ -n "$EXPECTED_CERT_FINGERPRINT" ]; then
    echo "Verifying certificate SHA-256 fingerprint against expected: $EXPECTED_CERT_FINGERPRINT"
    ACTUAL_FINGERPRINTS=$(apksigner verify --print-certs "$TARGET_FILE" | grep -i "SHA-256 digest" | tr -d ' ' | cut -d':' -f2-)
    
    MATCH_FOUND=false
    for FP in $ACTUAL_FINGERPRINTS; do
        # Normalize uppercase and strip colons
        NORM_ACTUAL=$(echo "$FP" | tr -d ':' | tr '[:lower:]' '[:upper:]')
        NORM_EXPECTED=$(echo "$EXPECTED_CERT_FINGERPRINT" | tr -d ':' | tr '[:lower:]' '[:upper:]')
        if [ "$NORM_ACTUAL" == "$NORM_EXPECTED" ]; then
            MATCH_FOUND=true
            break
        fi
    done

    if [ "$MATCH_FOUND" = false ]; then
        echo "CRITICAL ERROR: Artifact certificate fingerprint mismatch!"
        echo "Expected: $EXPECTED_CERT_FINGERPRINT"
        echo "Found:    $ACTUAL_FINGERPRINTS"
        exit 1
    fi
    echo "✓ Certificate fingerprint matched production authority."
fi

echo "========================================================"
echo "✓ Signature and Identity Verification PASSED."
echo "========================================================"
exit 0
