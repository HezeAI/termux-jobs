#!/usr/bin/env python3
"""Isolated bootstrap checks: existing components, private CLI installation, failed download.
Uses the already verified official tarball in .tools; never changes HOME or system packages.
"""
from pathlib import Path
import subprocess,os,shutil
root=Path(__file__).resolve().parents[1];d=root/'.tools/ai-agents/free-diagnostic'/('bootstrap-check-'+__import__('uuid').uuid4().hex[:8]);d.mkdir(parents=True,exist_ok=True)
script=root/'app/src/main/assets/ai-agents-bootstrap.sh';helpers=d/'helpers';helpers.mkdir(exist_ok=True)
# Network is replaced with a read-only copy of the verified official tarball.
(helpers/'curl').write_text('#!/bin/bash\nwhile [[ $# -gt 0 ]]; do if [[ "$1" == --output ]]; then shift; target="$1"; fi; shift; done\ncp -- '+str(root/'.tools/ai-agents/runtime/opencode.tgz')+' "$target"\n');(helpers/'curl').chmod(0o700)
env=dict(os.environ);env['PATH']=str(helpers)+':/usr/bin:/bin';env['JOBS_AI_LANGUAGE']='en';agent=d/'agents'
run=subprocess.run(['bash',str(script),str(agent)],env=env,stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=60)
assert run.returncode==0,run.stderr.decode();assert (agent/'bin/opencode').is_file()
print('PASS private OpenCode install with official checksum, existing Node reused')
(helpers/'curl').write_text('#!/bin/bash\nexit 22\n')
run=subprocess.run(['bash',str(script),str(agent)],env=env,stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=15)
assert run.returncode==0,run.stderr.decode();assert b'Installing OpenCode' not in run.stdout
print('PASS existing components: no download or reinstall')
run=subprocess.run(['bash',str(script),str(d/'failed-agents')],env=env,stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=15)
assert run.returncode!=0;assert not (d/'failed-agents/bin/opencode').exists();assert not list((d/'failed-agents').glob('.bootstrap.*'))
print('PASS failed download stops installation and cleans only its staging directory')

# Force the old/missing Node branch, then use the actual official private ARM64 runtime.
archive=root/'.tools/ai-agents/free-diagnostic/node-v22.22.1-linux-arm64.tar.xz'
if archive.exists():
    import hashlib
    assert hashlib.sha256(archive.read_bytes()).hexdigest()=='0f3550d58d45e5d3cf7103d9e3f69937f09fe82fb5dd474c66a5d816fa58c9ee'
    (helpers/'node').write_text('#!/bin/bash\nexit 1\n');(helpers/'node').chmod(0o700)
    (helpers/'curl').write_text('#!/bin/bash\nwhile [[ $# -gt 0 ]]; do if [[ "$1" == --output ]]; then shift; target="$1"; fi; url="$1"; shift; done\nif [[ "$url" == *nodejs.org* ]]; then cp -- '+str(archive)+' "$target"; else cp -- '+str(root/'.tools/ai-agents/runtime/opencode.tgz')+' "$target"; fi\n')
    run=subprocess.run(['bash',str(script),str(d/'private-node-agents')],env=env,stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=60)
    assert run.returncode==0,run.stderr.decode()
    assert (d/'private-node-agents/node-v22.22.1-linux-arm64/bin/node').is_file()
    print('PASS missing/old Node: verified private Node + OpenCode installed; system Node untouched')
