#!/usr/bin/env bash
# Source this from a build script. Keep build tools and all caches in the fork.
set -euo pipefail
FORK_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export TERMUX_FORK_SOURCE_ROOT="$FORK_ROOT"
export GRADLE_USER_HOME="$FORK_ROOT/.tools/gradle-home"
export ANDROID_HOME="${FORK_ANDROID_HOME:-$FORK_ROOT/.tools/sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
mkdir -p "$FORK_ROOT/.tools/tmp"
export TMPDIR="$(realpath "$FORK_ROOT/.tools/tmp")"
mkdir -p "$GRADLE_USER_HOME" "$TMPDIR" "$FORK_ROOT/.tools/test-home"
if [[ ! -d "$ANDROID_HOME" ]]; then
    echo "Prepare a local SDK in $FORK_ROOT/.tools/sdk before building." >&2
    exit 1
fi
case "$(realpath "$ANDROID_HOME")" in
    "$FORK_ROOT"/*) ;;
    *) echo "SDK must be a local copy inside the fork; shared SDK writes are forbidden." >&2; exit 1 ;;
esac
if [[ -f "$FORK_ROOT/local.properties" ]] && grep -q '^sdk.dir=' "$FORK_ROOT/local.properties"; then
    echo "Remove sdk.dir from local.properties; use the isolated ANDROID_HOME." >&2
    exit 1
fi
if [[ -n "${FORK_JAVA_HOME:-}" ]]; then
    export JAVA_HOME="$FORK_JAVA_HOME"
elif [[ -x "$FORK_ROOT/.tools/jdk/usr/lib/jvm/java-11-openjdk-arm64/bin/java" ]]; then
    export JAVA_HOME="$FORK_ROOT/.tools/jdk/usr/lib/jvm/java-11-openjdk-arm64"
fi
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Djava.io.tmpdir=$TMPDIR"
