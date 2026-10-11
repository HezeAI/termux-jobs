#!/usr/bin/env bash
set -euo pipefail
FORK_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
mkdir -p "$FORK_ROOT/.signing" "$FORK_ROOT/.tools/tmp"
FORK_KEYTOOL="${FORK_JAVA_HOME:-${JAVA_HOME:-}}/bin/keytool"
if [[ -x "$FORK_ROOT/.tools/jdk/usr/lib/jvm/java-11-openjdk-arm64/bin/keytool" ]]; then
    FORK_KEYTOOL="$FORK_ROOT/.tools/jdk/usr/lib/jvm/java-11-openjdk-arm64/bin/keytool"
elif [[ ! -x "$FORK_KEYTOOL" ]]; then
    FORK_KEYTOOL=keytool
fi
export TERMUX_FORK_KEYSTORE_PASSWORD="${TERMUX_FORK_KEYSTORE_PASSWORD:-termux-fork-debug}"
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Djava.io.tmpdir=$FORK_ROOT/.tools/tmp"
if [[ ! -f "$FORK_ROOT/.signing/fork-debug.jks" ]]; then
    "$FORK_KEYTOOL" -genkeypair -keystore "$FORK_ROOT/.signing/fork-debug.jks" \
        -storetype JKS -alias termux-fork-debug -keyalg RSA -keysize 3072 -validity 3650 \
        -dname 'CN=Termux Fork Development, OU=Local Testing' \
        -storepass:env TERMUX_FORK_KEYSTORE_PASSWORD -keypass:env TERMUX_FORK_KEYSTORE_PASSWORD
    chmod 600 "$FORK_ROOT/.signing/fork-debug.jks"
fi
