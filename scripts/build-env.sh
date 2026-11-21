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
elif [[ -x "$FORK_ROOT/.tools/jdk11/usr/lib/jvm/java-11-openjdk-arm64/bin/java" ]]; then
    export JAVA_HOME="$FORK_ROOT/.tools/jdk11/usr/lib/jvm/java-11-openjdk-arm64"
fi
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Djava.io.tmpdir=\"$TMPDIR\""
if [[ -z "${FORK_AAPT2:-}" && -x "$FORK_ROOT/.tools/native-aapt/aapt2" ]]; then
    export FORK_AAPT2="$FORK_ROOT/.tools/native-aapt/aapt2"
fi

fork_configure_java_tests() {
    if [[ -n "${JAVA_HOME:-}" ]]; then export PATH="$JAVA_HOME/bin:$PATH"; fi
    local javac_version
    javac_version="$(javac -version 2>&1)" || javac_version=""
    javac_version="${javac_version##*$'\n'}"
    if [[ "$javac_version" != "javac 11."* && "$javac_version" != "javac 11" ]]; then
        echo "Gradle/Robolectric tests require JDK 11. Set FORK_JAVA_HOME to a compatible JDK." >&2
        return 1
    fi
    # The locally extracted JDK can lack the distribution's system trust store.
    if [[ -n "${JAVA_HOME:-}" && ! -r "$JAVA_HOME/lib/security/cacerts" &&
        -r "$FORK_ROOT/.tools/cacerts" && "$JAVA_TOOL_OPTIONS" != *javax.net.ssl.trustStore=* ]]; then
        export JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -Djavax.net.ssl.trustStore=\"$FORK_ROOT/.tools/cacerts\""
    fi
    export FORK_TEST_TMPDIR="${FORK_TEST_TMPDIR:-$FORK_ROOT/.cx}"
    if [[ "$FORK_TEST_TMPDIR" != /* || -L "$FORK_TEST_TMPDIR" ]]; then
        echo "FORK_TEST_TMPDIR must be an absolute path to a real directory." >&2
        return 1
    fi
    mkdir -p -m 700 "$FORK_TEST_TMPDIR"
    export FORK_TEST_TMPDIR="$(realpath "$FORK_TEST_TMPDIR")"
    # Leave room for the temporary profile and its socket filename.
    local LC_ALL=C
    if (( ${#FORK_TEST_TMPDIR} > 52 )); then
        echo "The test path is too long for Unix sockets. Set FORK_TEST_TMPDIR to a short, private directory." >&2
        return 1
    fi
    export FORK_TEST_JAVA="$(command -v java)"
    if [[ "$FORK_ROOT" == *"'"* ]]; then
        # Gradle 7.2 probes a supplied executable through its parent JDK's bin/java.
        local test_bin="$FORK_ROOT/.tools/test-jvm/bin"
        mkdir -p "$test_bin"
        if ! cmp -s "$FORK_ROOT/scripts/gradle-test-java.sh" "$test_bin/java"; then
            install -m 700 "$FORK_ROOT/scripts/gradle-test-java.sh" "$test_bin/java"
        fi
        export FORK_TEST_EXECUTABLE="$test_bin/java"
    fi
}
