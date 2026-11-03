#!/usr/bin/env bash
# Manual, isolated native serialization research. Not a phone or cloud success test.
set -euo pipefail
umask 077
unset SYNTHETIC_TOOLS_ONLY SYNTHETIC_LATEST_ONLY SYNTHETIC_IMAGE_BUDGET
cd "$(dirname "$0")/../../.."
case "${1:-}" in
 codex|codex-latest) task_media_agent=codex ;;
 claude|gemini|opencode) task_media_agent=$1 ;;
 *) echo 'Usage: run.sh codex|codex-latest|claude|gemini|opencode' >&2; exit 2 ;;
esac
mkdir -p .verification/media-context
export TERMUX_MEDIA_OUTPUT
TERMUX_MEDIA_OUTPUT=$(mktemp -d "$PWD/.verification/media-context/replay-XXXXXX")
python3 - "$task_media_agent" <<'PY'
from pathlib import Path
import json,hashlib,sys
rows=json.loads(Path('scripts/tests/media-context/native-inputs.json').read_text());index={'codex':0,'claude':1,'opencode':2,'gemini':3}[sys.argv[1]];row=rows[index];p=Path(row['path']);assert p.is_file(),'Missing pinned native prerequisite';assert hashlib.sha256(p.read_bytes()).hexdigest()==row['sha256'],'Native input changed; review before execution'
PY
# Compile and TEST the actual kernel fence; never fall back to config-only pinning.
gcc -O2 -Wall -Wextra scripts/tests/fixtures/local-network-only.c -o "$TERMUX_MEDIA_OUTPUT/local-network-only"
python3 scripts/tests/test_local_network_only.py "$TERMUX_MEDIA_OUTPUT/local-network-only" "$TERMUX_MEDIA_OUTPUT/enforcement" > "$TERMUX_MEDIA_OUTPUT/enforcement.log" 2>&1
python3 scripts/tests/media-context/generate.py "$TERMUX_MEDIA_OUTPUT/data" > "$TERMUX_MEDIA_OUTPUT/data.json"
export TERMUX_MEDIA_GUARDED_RUN=1
cp scripts/tests/media-context/probe-* "$TERMUX_MEDIA_OUTPUT/"
if [[ "$1" == codex-latest ]]; then export SYNTHETIC_LATEST_ONLY=1; fi
if [[ "$task_media_agent" == codex ]]; then
 "$TERMUX_MEDIA_OUTPUT/local-network-only" "$TERMUX_MEDIA_OUTPUT/network.jsonl" -- python3 "$TERMUX_MEDIA_OUTPUT/probe-codex.py" > "$TERMUX_MEDIA_OUTPUT/run.log" 2>&1
else
 "$TERMUX_MEDIA_OUTPUT/local-network-only" "$TERMUX_MEDIA_OUTPUT/network.jsonl" -- node "$TERMUX_MEDIA_OUTPUT/probe-$task_media_agent.cjs" > "$TERMUX_MEDIA_OUTPUT/run.log" 2>&1
fi
printf 'Evidence: %s\n' "$TERMUX_MEDIA_OUTPUT"
