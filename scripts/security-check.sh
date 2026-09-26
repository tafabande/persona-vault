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

# 6. Planning Phase Sync Check
echo "[6/6] Verifying planning docs and app model are in sync..."
for f in "AGENTS.md" "app/AGENTS.md" "docs/planning/README.md" "docs/planning/ROADMAP.md" "docs/planning/PHASES.md" "docs/planning/WORKFLOW.md" "docs/planning/TEMPLATE.md"; do
    if [ ! -f "$f" ]; then
        echo "CRITICAL ERROR: Required planning file missing: $f"
        exit 1
    fi
done
if [ ! -f "app/src/main/java/com/pims/vault/core/model/PlanningPhase.kt" ]; then
    echo "CRITICAL ERROR: PlanningPhase.kt missing — app model must mirror docs/planning/ROADMAP.md"
    exit 1
fi
# Count phases in ROADMAP.md (lines starting with "| 0" through "| 7") vs enum entries
ROADMAP_PHASES=$(grep -cE '^\| [0-9]+ \|' docs/planning/ROADMAP.md || true)
KT_PHASES=$(grep -cE '^\s*(PLANNING_SYSTEM|M[0-9]+_)' app/src/main/java/com/pims/vault/core/model/PlanningPhase.kt || true)
if [ "$ROADMAP_PHASES" != "$KT_PHASES" ]; then
    echo "CRITICAL ERROR: Phase count mismatch — ROADMAP.md has $ROADMAP_PHASES phases, PlanningPhase.kt has $KT_PHASES entries. Keep them in sync."
    exit 1
fi
echo "✓ Planning docs and app model in sync ($ROADMAP_PHASES phases)."

echo "========================================================"
echo "✓ All Persona Machine-Enforced Security Gates PASSED."
echo "========================================================"
exit 0
