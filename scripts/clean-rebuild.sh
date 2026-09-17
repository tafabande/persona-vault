#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "========================================================"
echo "Persona Vault: Clean Rebuild & Cache Reset Pipeline"
echo "========================================================"

# 1. Stop background Gradle daemons
echo "[1/5] Stopping active Gradle daemons..."
"$PROJECT_ROOT/gradlew" --stop 2>/dev/null || true

# 2. Purge redirected build directory ($HOME/.gradle-builds/PimsVault)
EXTERNAL_BUILD_DIR="$HOME/.gradle-builds/PimsVault"
echo "[2/5] Purging external build cache ($EXTERNAL_BUILD_DIR)..."
rm -rf "$EXTERNAL_BUILD_DIR" 2>/dev/null || true

# 3. Purge workspace caches
echo "[3/5] Purging local workspace caches..."
rm -rf "$PROJECT_ROOT/.gradle" \
       "$PROJECT_ROOT/.kotlin" \
       "$PROJECT_ROOT/.firebase" \
       "$PROJECT_ROOT/build" \
       "$PROJECT_ROOT/app/build" \
       "$PROJECT_ROOT/app/.cxx" 2>/dev/null || true

# 4. Remove temporary logs and root screenshots
echo "[4/5] Removing temporary logs and screenshots..."
find "$PROJECT_ROOT" -maxdepth 1 -name "*.log" -delete 2>/dev/null || true
find "$PROJECT_ROOT" -maxdepth 1 -name "*.png" -delete 2>/dev/null || true

# 5. Run clean build
echo "[5/5] Executing Gradle clean lifecycle..."
if [ $# -gt 0 ]; then
    "$PROJECT_ROOT/gradlew" clean "$@" --no-build-cache
else
    "$PROJECT_ROOT/gradlew" clean --no-build-cache
fi

echo "========================================================"
echo "✓ Fresh build state ready. Caches & old builds purged."
echo "========================================================"
