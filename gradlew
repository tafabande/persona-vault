#!/usr/bin/env bash
set -euo pipefail

APP_HOME="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROPS_FILE="$APP_HOME/gradle/wrapper/gradle-wrapper.properties"

if [ ! -f "$PROPS_FILE" ]; then
    echo "Missing Gradle wrapper properties: $PROPS_FILE" >&2
    exit 1
fi

DIST_URL="$(sed -n 's/^distributionUrl=//p' "$PROPS_FILE" | head -n 1)"
if [ -z "$DIST_URL" ]; then
    echo "Unable to read distributionUrl from $PROPS_FILE" >&2
    exit 1
fi

GRADLE_VERSION="$(printf '%s\n' "$DIST_URL" | sed -n 's#.*/gradle-\([^/]*\)-bin\.zip#\1#p')"
if [ -z "$GRADLE_VERSION" ]; then
    echo "Unable to parse Gradle version from $DIST_URL" >&2
    exit 1
fi

GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
INSTALL_DIR="$GRADLE_USER_HOME/wrapper/dists/gradle-$GRADLE_VERSION-bin"
GRADLE_BIN="$INSTALL_DIR/gradle-$GRADLE_VERSION/bin/gradle"

if [ ! -x "$GRADLE_BIN" ]; then
    mkdir -p "$INSTALL_DIR"
    ZIP_FILE="$INSTALL_DIR/gradle-$GRADLE_VERSION-bin.zip"
    TMP_DIR="$INSTALL_DIR/tmp-$$"
    mkdir -p "$TMP_DIR"

    if command -v curl >/dev/null 2>&1; then
        curl -fsSL "$DIST_URL" -o "$ZIP_FILE"
    elif command -v wget >/dev/null 2>&1; then
        wget -qO "$ZIP_FILE" "$DIST_URL"
    else
        echo "Neither curl nor wget is available to download Gradle." >&2
        exit 1
    fi

    unzip -q "$ZIP_FILE" -d "$TMP_DIR"

    if [ ! -d "$TMP_DIR/gradle-$GRADLE_VERSION" ]; then
        echo "Downloaded Gradle archive did not contain gradle-$GRADLE_VERSION." >&2
        exit 1
    fi

    rm -rf "$INSTALL_DIR/gradle-$GRADLE_VERSION"
    mv "$TMP_DIR/gradle-$GRADLE_VERSION" "$INSTALL_DIR/"
    rm -rf "$TMP_DIR"
fi

exec "$GRADLE_BIN" "$@"
