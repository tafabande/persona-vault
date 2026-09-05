#!/usr/bin/env bash
set -euo pipefail

echo "========================================================"
echo "Persona Machine-Enforced Security Static Analysis Gate"
echo "========================================================"

# 1. POSITIVE check for allowBackup="false"
echo "[1/5] Verifying android:allowBackup is explicitly false..."
if ! grep -q 'android:allowBackup="false"' app/src/main/AndroidManifest.xml; then
    echo "CRITICAL ERROR: android:allowBackup must be explicitly defined as 'false' in AndroidManifest.xml!"
    exit 1
fi
echo "✓ allowBackup=false explicitly configured."

# 2. POSITIVE check for networkSecurityConfig
echo "[2/5] Verifying networkSecurityConfig presence..."
if ! grep -q 'android:networkSecurityConfig="@xml/network_security_config"' app/src/main/AndroidManifest.xml; then
    echo "CRITICAL ERROR: android:networkSecurityConfig is missing from AndroidManifest.xml!"
    exit 1
fi
echo "✓ networkSecurityConfig explicitly configured."

# 3. Reject usesCleartextTraffic="true"
echo "[3/5] Checking for forbidden cleartext traffic..."
if grep -q 'android:usesCleartextTraffic="true"' app/src/main/AndroidManifest.xml; then
    echo "CRITICAL ERROR: Cleartext HTTP traffic is forbidden in Persona!"
    exit 1
fi
echo "✓ Cleartext traffic forbidden."

# 4. Comprehensive Secret Pattern Scanner
echo "[4/5] Scanning for hardcoded credentials, private keys, and master passwords..."
if grep -rnE '(PRIVATE_KEY_PLAINTEXT|BEGIN (RSA|EC|PRIVATE) KEY|HARDCODED_MASTER_PASSWORD|password\s*=\s*"[a-zA-Z0-9!@#$%^&*]{8,}")' app/src/main/java/; then
    echo "CRITICAL ERROR: Hardcoded credential or private key detected in source tree!"
    exit 1
fi
echo "✓ No hardcoded secrets detected in main sources."

# 5. Strict Dependency Pinning Check (reject +, 1.+, latest.release, latest.integration, SNAPSHOT)
echo "[5/5] Checking for unpinned or dynamic dependencies in version catalog..."
if grep -E 'version(\.ref)?\s*=\s*("[^"]*(\+|\.x|latest|SNAPSHOT)[^"]*")' gradle/libs.versions.toml; then
    echo "CRITICAL ERROR: Unpinned, dynamic, or SNAPSHOT dependency found in libs.versions.toml!"
    exit 1
fi
echo "✓ All dependencies strictly pinned."

echo "========================================================"
echo "✓ All Persona Machine-Enforced Security Gates PASSED."
echo "========================================================"
exit 0
