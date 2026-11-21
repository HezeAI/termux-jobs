#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/build-env.sh"
fork_configure_java_tests
test_tasks=(:app:testDebugUnitTest :terminal-emulator:testDebugUnitTest
    :terminal-view:testDebugUnitTest :termux-shared:testDebugUnitTest)
if (( $# )); then
    test_tasks=(:app:testDebugUnitTest)
    while (( $# )); do
        if [[ "$1" != --tests || $# -lt 2 || -z "$2" || "$2" == -* ]]; then
            echo "Usage: bash scripts/verify-java.sh [--tests PATTERN]..." >&2
            exit 2
        fi
        test_tasks+=(--tests "$2")
        shift 2
    done
fi
cd "$FORK_ROOT"
# Reuse prepared inputs. Gradle still checks the pinned X11 hashes.
if [[ ! -d .tools/x11-runtime ]]; then
    python3 scripts/prepare-x11.py
fi
python3 scripts/prepare-java-verification.py
cd "$FORK_ROOT/.verification/source"
FORK_GRADLE="$FORK_ROOT/.tools/gradle-7.2/bin/gradle"
if [[ ! -x "$FORK_GRADLE" ]]; then FORK_GRADLE="$PWD/gradlew"; fi
"$FORK_GRADLE" --no-daemon --no-watch-fs --max-workers=2 \
    -Pandroid.aapt2FromMavenOverride="${FORK_AAPT2:-$ANDROID_HOME/build-tools/30.0.3/aapt2}" \
    -x :app:downloadBootstraps "${test_tasks[@]}"
