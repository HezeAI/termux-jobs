#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/build-env.sh"
cd "$FORK_ROOT"
./scripts/create-debug-key.sh
export TERMUX_APK_VERSION_TAG="${TERMUX_APK_VERSION_TAG:-fork-debug}"
export TERMUX_SPLIT_APKS_FOR_DEBUG_BUILDS=0
./gradlew --no-daemon :app:assembleDebug
