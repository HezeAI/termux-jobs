#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/build-env.sh"
cd "$FORK_ROOT"
python3 scripts/prepare-x11.py
python3 scripts/prepare-java-verification.py
cd "$FORK_ROOT/.verification/source"
FORK_GRADLE="$FORK_ROOT/.tools/gradle-7.2/bin/gradle"
if [[ ! -x "$FORK_GRADLE" ]]; then FORK_GRADLE="$PWD/gradlew"; fi
"$FORK_GRADLE" --no-daemon \
    -Pandroid.aapt2FromMavenOverride="${FORK_AAPT2:-$ANDROID_HOME/build-tools/30.0.3/aapt2}" \
    -x :app:downloadBootstraps \
    :app:testDebugUnitTest :terminal-emulator:testDebugUnitTest \
    :terminal-view:testDebugUnitTest :termux-shared:testDebugUnitTest
