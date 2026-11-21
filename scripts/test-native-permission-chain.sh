#!/usr/bin/env bash
# Explicit isolated native integration suite. Not part of ordinary JVM/CI tests.
set -euo pipefail
source "$(dirname "$0")/build-env.sh"
fork_configure_java_tests
# The generated Codex command checks the host control socket against 100 bytes.
# The fixture adds /fXXXXXX/.codex-control/<uuid>.sock (65 bytes).
if (( $(printf '%s' "$FORK_TEST_TMPDIR" | wc -c) > 35 )); then
    echo 'The native permission suite needs a fixture root of at most 35 bytes. Set FORK_TEST_TMPDIR to a short, private directory.' >&2
    exit 1
fi
cd "$FORK_ROOT"
if ! command -v git >/dev/null && [[ -x "$FORK_ROOT/.tools/git-local/usr/bin/git" ]]; then
    export PATH="$FORK_ROOT/.tools/git-local/usr/bin:$PATH"
fi
test -f /root/.local/share/termux-jobs/gemini-cli/0.64.0/bundle/chunk-NR7L3FXV.js
test -x "$FORK_ROOT/.tools/ai-agents/runtime/opencode"
python3 - <<'PY'
import json,hashlib
from pathlib import Path
for entry in json.loads(Path("scripts/tests/media-context/native-inputs.json").read_text()):
    p=Path(entry["path"])
    assert hashlib.sha256(p.read_bytes()).hexdigest()==entry["sha256"], str(p)
print("Pinned native CLI hashes verified; no components installed")
PY
mkdir -p "$FORK_ROOT/.verification/native-permissions"
gcc -O2 -Wall -Wextra "$FORK_ROOT/scripts/tests/fixtures/local-network-only.c" -o "$FORK_ROOT/.verification/native-permissions/local-network-only"
export TERMUX_FORK_AUDIT_NET_GUARD="$FORK_ROOT/.verification/native-permissions/local-network-only"
python3 "$FORK_ROOT/scripts/tests/test_local_network_only.py" "$TERMUX_FORK_AUDIT_NET_GUARD" "$FORK_ROOT/.verification/native-permissions/enforcement"
python3 "$FORK_ROOT/scripts/prepare-java-verification.py"
cp "$FORK_ROOT/scripts/tests/native/NativePermissionChainTest.java" "$FORK_ROOT/.verification/source/app/src/test/java/com/termux/app/workspace/NativePermissionChainTest.java"
cp "$FORK_ROOT/scripts/tests/native/NativeProfilePermissionTest.java" "$FORK_ROOT/.verification/source/app/src/test/java/com/termux/app/workspace/NativeProfilePermissionTest.java"
cd "$FORK_ROOT/.verification/source"
"$FORK_ROOT/.tools/gradle-7.2/bin/gradle" --offline --no-daemon --max-workers=2 \
 -Dorg.gradle.jvmargs=-Xmx1536m \
 -Pandroid.aapt2FromMavenOverride="${FORK_AAPT2:-/data/data/com.termux/files/usr/bin/aapt2}" \
 -I "$FORK_ROOT/scripts/tests/native/force-native-execution.gradle" \
 -x :app:downloadBootstraps :app:testDebugUnitTest --tests '*NativePermissionChainTest' --tests '*NativeProfilePermissionTest'
