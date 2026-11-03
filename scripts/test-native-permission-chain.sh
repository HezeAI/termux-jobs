#!/usr/bin/env bash
# Explicit isolated native integration suite. Not part of ordinary JVM/CI tests.
set -euo pipefail
source "$(dirname "$0")/build-env.sh"
test -f /root/.local/share/termux-jobs/gemini-cli/0.64.0/bundle/chunk-NR7L3FXV.js
test -x "$FORK_ROOT/.tools/ai-agents/runtime/opencode"
mkdir -p "$FORK_ROOT/.verification/native-permissions"
gcc -O2 -Wall -Wextra "$FORK_ROOT/scripts/tests/fixtures/local-network-only.c" -o "$FORK_ROOT/.verification/native-permissions/local-network-only"
export TERMUX_FORK_AUDIT_NET_GUARD="$FORK_ROOT/.verification/native-permissions/local-network-only"
python3 "$FORK_ROOT/scripts/tests/test_local_network_only.py" "$TERMUX_FORK_AUDIT_NET_GUARD" "$FORK_ROOT/.verification/native-permissions/enforcement"
python3 "$FORK_ROOT/scripts/prepare-java-verification.py"
cp "$FORK_ROOT/scripts/tests/native/NativePermissionChainTest.java" "$FORK_ROOT/.verification/source/app/src/test/java/com/termux/app/workspace/NativePermissionChainTest.java"
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Djava.io.tmpdir=/root/termux-fork/.verification/t"
cd "$FORK_ROOT/.verification/source"
"$FORK_ROOT/.tools/gradle-7.2/bin/gradle" --offline --no-daemon --max-workers=2 \
 -Dorg.gradle.jvmargs=-Xmx1536m \
 -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2 \
 -I "$FORK_ROOT/scripts/tests/native/force-native-execution.gradle" \
 -x :app:downloadBootstraps :app:testDebugUnitTest --tests '*NativePermissionChainTest'
