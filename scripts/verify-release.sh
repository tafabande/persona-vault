#!/usr/bin/env bash
set -euo pipefail

echo "========================================================"
echo "Persona Compiled Release Artifact Verification Gate"
echo "========================================================"

TARGET_ARTIFACT="${1:-}"

if [ -z "$TARGET_ARTIFACT" ]; then
    # Try finding the compiled release AAB or APK if none supplied
    TARGET_ARTIFACT=$(find app/build/outputs -type f \( -name "*release*.aab" -o -name "*release*.apk" \) | head -n 1 || true)
fi

if [ -z "$TARGET_ARTIFACT" ] || [ ! -f "$TARGET_ARTIFACT" ]; then
    echo "CRITICAL ERROR: No compiled release artifact found to verify!"
    exit 1
fi

echo "Inspecting compiled release artifact: $TARGET_ARTIFACT"

# 1. Verify artifact is non-empty
FILE_SIZE=$(wc -c < "$TARGET_ARTIFACT" | tr -d ' ')
if [ "$FILE_SIZE" -lt 100000 ]; then
    echo "CRITICAL ERROR: Artifact size ($FILE_SIZE bytes) is suspiciously small. Incomplete build!"
    exit 1
fi
echo "✓ Artifact size validated ($FILE_SIZE bytes)."

# 2. Inspect manifest/archive inside the AAB or APK
if command -v aapt2 >/dev/null 2>&1; then
    echo "Inspecting manifest with aapt2..."
    DUMP_OUTPUT=$(aapt2 dump badging "$TARGET_ARTIFACT" 2>/dev/null || true)
    
    # Check for debuggable flag
    if echo "$DUMP_OUTPUT" | grep -i "debuggable"; then
        echo "CRITICAL ERROR: Compiled artifact is marked DEBUGGABLE!"
        exit 1
    fi
    echo "✓ Non-debuggable attribute confirmed."
elif command -v unzip >/dev/null 2>&1; then
    echo "Inspecting archive contents with unzip..."
    ARCHIVE_LIST=$(unzip -l "$TARGET_ARTIFACT")
    
    # Verify presence of compiled DEX
    if ! echo "$ARCHIVE_LIST" | grep -E "(classes\.dex|base/dex/)"; then
        echo "CRITICAL ERROR: No DEX bytecode found in compiled artifact!"
        exit 1
    fi
    echo "✓ Bytecode DEX present."
fi

# 3. Verify BuildConfig release hardening flags
echo "Verifying build configuration flags..."
if ! awk '
    BEGIN { in_release = 0; depth = 0; insecure = 0 }
    /release[[:space:]]*\{/ {
        in_release = 1
        depth = 1
        next
    }
    in_release {
        open_braces = gsub(/\{/, "{")
        close_braces = gsub(/\}/, "}")
        depth += open_braces - close_braces
        if ($0 ~ /buildConfigField\("Boolean", "IS_DEBUG_CRYPTO_ALLOWED", "true"\)/) {
            insecure = 1
        }
        if (depth <= 0) {
            exit insecure ? 1 : 0
        }
    }
    END { exit insecure ? 1 : 0 }
' app/build.gradle.kts; then
    echo "CRITICAL ERROR: IS_DEBUG_CRYPTO_ALLOWED is enabled in release build type!"
    exit 1
fi

# 4. Verify Positive Manifest Security Settings
echo "Verifying positive manifest security settings..."
if ! grep -q 'android:allowBackup="false"' app/src/main/AndroidManifest.xml; then
    echo "CRITICAL ERROR: android:allowBackup is NOT set to false in AndroidManifest.xml!"
    exit 1
fi

if grep -q 'android:usesCleartextTraffic="true"' app/src/main/AndroidManifest.xml; then
    echo "CRITICAL ERROR: android:usesCleartextTraffic is true in AndroidManifest.xml!"
    exit 1
fi

echo "========================================================"
echo "✓ Compiled Release Artifact Pre-Deployment Gate PASSED."
echo "========================================================"
exit 0
