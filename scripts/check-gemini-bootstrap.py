#!/usr/bin/env python3
"""Offline bootstrap checks using only repository-owned files and private probe config."""
from pathlib import Path
import tempfile,subprocess,os,shutil,hashlib
repo=Path(__file__).resolve().parents[1]
base=Path(tempfile.mkdtemp(prefix='bootstrap-',dir=repo/'.verification/gemini-v1.0.15'))
root=base/'runtime';bin_dir=base/'bin';bin_dir.mkdir()
archive=repo/'.tools/gemini-cli/gemini.tgz'
assert hashlib.sha256(archive.read_bytes()).hexdigest()=='891a794e55c8319b6b78f6f8f13d6b8b2782a4709dfd2fcabf3a1f63ae8365e6'
# Download is mocked, but official bytes, tar extraction, checksums and native --version are real.
curl=bin_dir/'curl';curl.write_text('#!/bin/bash\ntarget=\nfor ((i=1;i<=$#;i++)); do if [ "${!i}" = -o ]; then j=$((i+1)); target=${!j}; fi; done\ncase "${!#}" in *node-v*) cp -- "$GEMINI_TEST_NODE" "$target";; *) cp -- "$GEMINI_TEST_ARCHIVE" "$target";; esac\n');curl.chmod(0o700)
source=(repo/'app/src/main/assets/gemini-bootstrap.sh').read_text().replace('$HOME/.local/share/termux-jobs/gemini-cli',str(root))
assert '$HOME/.local/share' not in source, 'Refuse non-isolated test'
script=base/'bootstrap.sh';script.write_text(source)
env={k:v for k,v in os.environ.items() if not k.startswith(('ANTHROPIC_','CLAUDE_','GEMINI_','GOOGLE_'))}
env['GEMINI_TEST_NODE']=str(repo/'.tools/ai-agents/free-diagnostic/node-v22.22.1-linux-arm64.tar.xz')
env.update(PATH=str(bin_dir)+':'+os.environ['PATH'],GEMINI_TEST_ARCHIVE=str(archive),GEMINI_CLI_HOME=str(base/'config'),GEMINI_CLI_NO_RELAUNCH='1')
def run():return subprocess.run(['bash','-c','source "$1"', 'check',str(script)],env=env,capture_output=True,text=True,timeout=90)
result=run();assert result.returncode==0,result.stderr
binary=root/'0.64.0/bundle/gemini.js';assert binary.exists();stamp=binary.stat().st_mtime_ns
result=run();assert result.returncode==0,result.stderr;assert binary.stat().st_mtime_ns==stamp
# A corrupt pre-existing runtime is detected and preserved, not silently overwritten.
binary.write_bytes(b'preserve damaged runtime');result=run();assert result.returncode!=0;assert binary.read_bytes()==b'preserve damaged runtime'
# Verify that corrupt download cannot publish a runnable version.
other=base/'other';script.write_text(source.replace(str(root),str(other)));corrupt=base/'corrupt.tgz';corrupt.write_bytes(b'bad archive');env['GEMINI_TEST_ARCHIVE']=str(corrupt)
result=run();assert result.returncode!=0;assert not (other/'0.64.0').exists()
print('PASS: private Node installation, official archive integrity, native private installation, cache reuse, corrupt runtime preservation, corrupt download rejection')
