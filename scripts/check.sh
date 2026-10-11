#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/build-env.sh"
cd "$FORK_ROOT"
./gradlew --no-daemon :terminal-emulator:testDebugUnitTest :terminal-view:testDebugUnitTest :termux-shared:testDebugUnitTest :app:testDebugUnitTest
