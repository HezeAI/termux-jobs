#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/build-env.sh"
fork_configure_java_tests
cd "$FORK_ROOT"
gradle="$FORK_ROOT/.tools/gradle-7.2/bin/gradle"
if [[ ! -x "$gradle" ]]; then gradle="$FORK_ROOT/gradlew"; fi
options=(--no-daemon)
if [[ -n "${FORK_AAPT2:-}" ]]; then
    options+=("-Pandroid.aapt2FromMavenOverride=$FORK_AAPT2")
fi
"$gradle" "${options[@]}" :terminal-emulator:testDebugUnitTest :terminal-view:testDebugUnitTest :termux-shared:testDebugUnitTest :app:testDebugUnitTest
