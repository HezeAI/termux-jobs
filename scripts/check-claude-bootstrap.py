#!/usr/bin/env python3
"""Offline bootstrap checks using only repository-owned files and private probe config."""
from pathlib import Path
import tempfile,subprocess,os,shutil,hashlib
repo=Path(__file__).resolve().parents[1]
base=Path(tempfile.mkdtemp(prefix='bootstrap-',dir=repo/'.verification/claude-v1.0.14'))
root=base/'runtime';bin_dir=base/'bin';bin_dir.mkdir()
archive=repo/'.tools/claude-code/claude.tgz'
assert hashlib.sha256(archive.read_bytes()).hexdigest()=='56abbe1cfc85e5b37afeef5ae8b4856ddc32bbe443b82689a3b5f99992045cd1'
# Download is mocked, but official bytes, tar extraction, checksums and native --version are real.
curl=bin_dir/'curl';curl.write_text('#!/bin/bash\ntarget=\nfor ((i=1;i<=$#;i++)); do if [ "${!i}" = -o ]; then j=$((i+1)); target=${!j}; fi; done\ncase "${!#}" in *node-v*) cp -- "$CLAUDE_TEST_NODE" "$target";; *) cp -- "$CLAUDE_TEST_ARCHIVE" "$target";; esac\n');curl.chmod(0o700)
source=(repo/'app/src/main/assets/claude-bootstrap.sh').read_text().replace('$HOME/.local/share/termux-jobs/claude-code',str(root))
script=base/'bootstrap.sh';script.write_text(source)
env={k:v for k,v in os.environ.items() if not k.startswith(('ANTHROPIC_','CLAUDE_'))}
env.update(PATH=str(bin_dir)+':'+os.environ['PATH'],CLAUDE_TEST_ARCHIVE=str(archive),CLAUDE_CONFIG_DIR=str(base/'config'),DISABLE_AUTOUPDATER='1',CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC='1')
def run():return subprocess.run(['bash','-c','source "$1"', 'check',str(script)],env=env,capture_output=True,text=True,timeout=90)
result=run();assert result.returncode==0,result.stderr
binary=root/'2.1.296/claude';assert binary.exists();stamp=binary.stat().st_mtime_ns
result=run();assert result.returncode==0,result.stderr;assert binary.stat().st_mtime_ns==stamp
# A corrupt pre-existing runtime is detected and preserved, not silently overwritten.
binary.write_bytes(b'preserve damaged runtime');result=run();assert result.returncode!=0;assert binary.read_bytes()==b'preserve damaged runtime'
# Verify that corrupt download cannot publish a runnable version.
other=base/'other';script.write_text(source.replace(str(root),str(other)));corrupt=base/'corrupt.tgz';corrupt.write_bytes(b'bad archive');env['CLAUDE_TEST_ARCHIVE']=str(corrupt)
result=run();assert result.returncode!=0;assert not (other/'2.1.296').exists()
# No suitable system Node: use a verified private runtime without replacing system Node.
private=base/'private-node';script.write_text(source.replace(str(root),str(private)).replace('node=$(type -P node || true)', "node=''"))
env['CLAUDE_TEST_ARCHIVE']=str(archive);env['CLAUDE_TEST_NODE']=str(repo/'.tools/ai-agents/free-diagnostic/node-v22.22.1-linux-arm64.tar.xz')
result=run();assert result.returncode==0,result.stderr
assert (private/'controller-node').read_text().strip()==str(private/'node-v22.22.1-linux-arm64/bin/node')
print('PASS: private Node fallback, official archive integrity, native private installation, cache reuse, corrupt runtime preservation, corrupt download rejection')
