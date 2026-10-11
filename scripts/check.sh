#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
./gradlew --no-daemon :terminal-emulator:testDebugUnitTest :terminal-view:testDebugUnitTest :termux-shared:testDebugUnitTest :app:testDebugUnitTest
