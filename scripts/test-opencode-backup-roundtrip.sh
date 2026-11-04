#!/usr/bin/env bash
# Explicit isolated backup/profile/native check. Never installs components or APKs.
set -euo pipefail
source "$(dirname "$0")/build-env.sh"
cd "$FORK_ROOT"
verification="$FORK_ROOT/.verification/opencode-backup-roundtrip"
mkdir -p "$verification"
exec 9>"$verification/runner.lock"
flock -n 9 || { echo 'This backup test runner is already active.' >&2; exit 1; }
python3 - <<'PY'
import hashlib,json
from pathlib import Path
r=next(r for r in json.loads(Path('scripts/tests/media-context/native-inputs.json').read_text()) if r['version']=='2.0.28')
assert hashlib.sha256(Path(r['path']).read_bytes()).hexdigest()==r['sha256']
print('Pinned OpenCode 2.0.28 matches; no CLI installation')
PY
gcc -O2 -Wall -Wextra scripts/tests/fixtures/local-network-only.c -o "$verification/local-network-only.next"
mv "$verification/local-network-only.next" "$verification/local-network-only"
export TERMUX_FORK_AUDIT_NET_GUARD="$verification/local-network-only"
python3 scripts/tests/test_local_network_only.py "$TERMUX_FORK_AUDIT_NET_GUARD" "$verification/network-enforcement"
python3 scripts/tests/test_opencode_backup.py
python3 scripts/tests/test_opencode_backup_legacy.py
python3 scripts/tests/test_workspace_backup.py
python3 scripts/tests/test_workspace_selection.py
python3 scripts/prepare-java-verification.py
cp scripts/tests/native/OpenCodeBackupRoundTripTest.java .verification/source/app/src/test/java/com/termux/app/workspace/
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Djava.io.tmpdir=/root/termux-fork/.verification/t"
cd .verification/source
"$FORK_ROOT/.tools/gradle-7.2/bin/gradle" --offline --no-daemon --max-workers=2 \
 -Dorg.gradle.jvmargs=-Xmx1536m \
 -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2 \
 -I "$FORK_ROOT/scripts/tests/native/force-native-execution.gradle" \
 -x :app:downloadBootstraps :app:testDebugUnitTest \
 --tests '*OpenCodeBackupRestoreTest' --tests '*WorkspaceBackup*' --tests '*OpenCodeBackupRoundTripTest'
