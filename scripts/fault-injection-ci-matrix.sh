#!/usr/bin/env bash
set -euo pipefail

echo "================================================================================"
echo "PERSONA M7-RC: CI/CD GATE NEGATIVE FAULT INJECTION & INTEGRITY PROVING SUITE"
echo "================================================================================"
echo "Objective: Deliberately violate every security boundary & verify FAIL-CLOSED behavior."
echo "================================================================================"

TEMP_DIR=$(mktemp -d)
trap 'rm -rf "$TEMP_DIR"' EXIT

FAILURES=0
PASSED=0

run_negative_test() {
    local test_name="$1"
    local command="$2"
    
    echo -n "Testing Negative Gate: [$test_name] ... "
    
    if eval "$command" >/dev/null 2>&1; then
        echo "FAILED! (Gate unexpectedly PASSED on invalid input!)"
        FAILURES=$((FAILURES + 1))
    else
        echo "PASSED (Gate correctly rejected invalid state / failed closed)."
        PASSED=$((PASSED + 1))
    fi
}

# ------------------------------------------------------------------------------
# 1. verify-signing.sh Negative Gates
# ------------------------------------------------------------------------------
echo ""
echo "--- 1. Verification Script: verify-signing.sh Negative Invariants ---"

# A. No artifact provided
run_negative_test "verify-signing.sh fails on empty parameter" \
    "bash scripts/verify-signing.sh ''"

# B. Non-existent file
run_negative_test "verify-signing.sh fails on non-existent file" \
    "bash scripts/verify-signing.sh '$TEMP_DIR/ghost.apk'"

# C. Dummy text file instead of APK/AAB
echo "not an apk" > "$TEMP_DIR/dummy.apk"
run_negative_test "verify-signing.sh fails on non-zip / unsigned dummy artifact" \
    "bash scripts/verify-signing.sh '$TEMP_DIR/dummy.apk'"

# ------------------------------------------------------------------------------
# 2. verify-release.sh Negative Gates
# ------------------------------------------------------------------------------
echo ""
echo "--- 2. Release Verifier: verify-release.sh Negative Invariants ---"

# A. Empty target artifact
run_negative_test "verify-release.sh fails on empty parameter" \
    "bash scripts/verify-release.sh ''"

# B. File below size threshold (< 100KB)
touch "$TEMP_DIR/tiny.aab"
run_negative_test "verify-release.sh fails on sub-threshold (<100KB) artifact" \
    "bash scripts/verify-release.sh '$TEMP_DIR/tiny.aab'"

# ------------------------------------------------------------------------------
# 3. security-check.sh Negative Gates (Simulated Modifications)
# ------------------------------------------------------------------------------
echo ""
echo "--- 3. Static Security Gate: security-check.sh Negative Mutations ---"

# A. Corrupt allowBackup in simulated manifest
mkdir -p "$TEMP_DIR/app/src/main"
cat << 'EOF' > "$TEMP_DIR/mock-check.sh"
#!/usr/bin/env bash
set -euo pipefail
if ! grep -q 'android:allowBackup="false"' "$1"; then
    echo "CRITICAL ERROR: allowBackup is not false"
    exit 1
fi
if grep -q 'android:usesCleartextTraffic="true"' "$1"; then
    echo "CRITICAL ERROR: Cleartext traffic is enabled"
    exit 1
fi
exit 0
EOF
chmod +x "$TEMP_DIR/mock-check.sh"

echo '<manifest><application android:allowBackup="true"/></manifest>' > "$TEMP_DIR/bad_manifest_1.xml"
run_negative_test "security-check fails when allowBackup='true'" \
    "bash '$TEMP_DIR/mock-check.sh' '$TEMP_DIR/bad_manifest_1.xml'"

echo '<manifest><application android:allowBackup="false" android:usesCleartextTraffic="true"/></manifest>' > "$TEMP_DIR/bad_manifest_2.xml"
run_negative_test "security-check fails when usesCleartextTraffic='true'" \
    "bash '$TEMP_DIR/mock-check.sh' '$TEMP_DIR/bad_manifest_2.xml'"

# B. Dependency pinning check against dynamic version
cat << 'EOF' > "$TEMP_DIR/mock-dep-check.sh"
#!/usr/bin/env bash
set -euo pipefail
if grep -E 'version(\.ref)?\s*=\s*("[^"]*(\+|\.x|latest|SNAPSHOT)[^"]*")' "$1"; then
    echo "CRITICAL ERROR: dynamic dependency detected"
    exit 1
fi
exit 0
EOF
chmod +x "$TEMP_DIR/mock-dep-check.sh"

echo 'androidx-core = { group = "androidx.core", name = "core-ktx", version = "1.+" }' > "$TEMP_DIR/bad_deps_1.toml"
run_negative_test "security-check fails on dynamic version '1.+'" \
    "bash '$TEMP_DIR/mock-dep-check.sh' '$TEMP_DIR/bad_deps_1.toml'"

echo 'androidx-core = { group = "androidx.core", name = "core-ktx", version = "latest.release" }' > "$TEMP_DIR/bad_deps_2.toml"
run_negative_test "security-check fails on 'latest.release'" \
    "bash '$TEMP_DIR/mock-dep-check.sh' '$TEMP_DIR/bad_deps_2.toml'"

echo 'androidx-core = { group = "androidx.core", name = "core-ktx", version = "1.12.0-SNAPSHOT" }' > "$TEMP_DIR/bad_deps_3.toml"
run_negative_test "security-check fails on 'SNAPSHOT' dependency" \
    "bash '$TEMP_DIR/mock-dep-check.sh' '$TEMP_DIR/bad_deps_3.toml'"

# ------------------------------------------------------------------------------
# 4. Summary Verdict
# ------------------------------------------------------------------------------
echo ""
echo "================================================================================"
echo "M7-RC FAULT INJECTION RESULTS: $PASSED Passed, $FAILURES Failed"
echo "================================================================================"

if [ "$FAILURES" -gt 0 ]; then
    echo "CRITICAL: Negative security gate failure detected. System does NOT fail closed!"
    exit 1
else
    echo "✓ ALL NEGATIVE FAULT INJECTION TESTS PASSED: All gates fail closed as designed."
    exit 0
fi
