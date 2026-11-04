#!/usr/bin/env bash
# Manual, isolated pinned-CLI check. No APK/CLI install or real authorization.
set -euo pipefail
cd "$(dirname "$0")/.."
verification="$PWD/.verification/gemini-managed-updates"
mkdir -p "$verification"
exec 9>"$verification/runner.lock"
flock -n 9 || { echo 'This Gemini test runner is already active.' >&2; exit 1; }
gcc -O2 -Wall -Wextra scripts/tests/fixtures/local-network-only.c -o "$verification/local-network-only.next"
mv "$verification/local-network-only.next" "$verification/local-network-only"
python3 scripts/tests/test_local_network_only.py "$verification/local-network-only" "$verification/network-negatives"
python3 - "$verification" <<'PY'
from pathlib import Path
import os,subprocess,sys,tempfile,json
repo=Path.cwd();out=Path(sys.argv[1]);guard=out/'local-network-only'
run=Path(tempfile.mkdtemp(prefix='checks-',dir=out));home=run/'synthetic-home';home.mkdir(mode=0o700)
results=[]
for filename in ['../check-gemini-auth.js','test_gemini_managed_updates.cjs','test_gemini_handler_guard.cjs','test_gemini_vertex_auth.cjs','test_gemini_key_broker.cjs','test_gemini_key_broker_interrupted.cjs','test_gemini_chat_socket.cjs','test_gemini_chat_recovery.cjs','test_gemini_permissions.cjs','test_gemini_live_modes.cjs','test_gemini_chat_hooks.cjs']:
 fixture=Path(tempfile.mkdtemp(prefix='gm-',dir='/root/termux-fork/.verification/t'))
 env={'PATH':os.environ['PATH'],'HOME':str(home),'TERMUX_FORK_TEST_UNIX_ROOT':str(fixture)}
 # The existing fixture helper receives the exact fresh Unix root allowed by guard.
 wrapper="require('./scripts/tests/fixture-paths.cjs').fixture=()=>process.env.TERMUX_FORK_TEST_UNIX_ROOT;require('./scripts/tests/'+process.argv[1]);"
 label=Path(filename).name
 with (run/(label+'.log')).open('w') as log:
  p=subprocess.run([str(guard),str(run/(label+'.network.jsonl')),'--','node','-e',wrapper,filename],env=env,stdout=log,stderr=subprocess.STDOUT,timeout=90)
 results.append(dict(test=filename,exit=p.returncode));assert p.returncode==0,filename
fixture=Path(tempfile.mkdtemp(prefix='gla-',dir='/root/termux-fork/.verification/t'))
env={'PATH':os.environ['PATH'],'HOME':str(home),'TERMUX_FORK_GEMINI_LOCAL_PROBE':'1','TERMUX_FORK_TEST_UNIX_ROOT':str(fixture)}
with (run/'real-launch.log').open('w') as log:
 p=subprocess.run([str(guard),str(run/'real-launch.network.jsonl'),'--','python3','scripts/tests/probe_gemini_auth_launch.py'],env=env,stdout=log,stderr=subprocess.STDOUT,timeout=420)
results.append(dict(test='real-launch-normal-and-symlink',exit=p.returncode,evidence=str(fixture)))
(run/'summary.json').write_text(json.dumps(results,indent=2)+'\n');assert p.returncode==0
print('PASS:',run)
PY
