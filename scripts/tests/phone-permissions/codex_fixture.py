"""Private Codex configuration for boundary fixtures; no existing login used."""
from pathlib import Path
import os,sys,json,hashlib,tempfile,subprocess,shutil
REPO=Path(__file__).resolve().parents[3];WORKSPACE=Path('/root/termux-fork')
BINARY=Path('/usr/local/lib/node_modules/@openai/codex/node_modules/@openai/codex-linux-arm64/vendor/aarch64-unknown-linux-musl/bin/codex');HASH='f2e6c980c937e734644bed1b4a9a018d3e63a0ccbe71aafc85c86a8c6e4b462a'
def prepare():
 assert hashlib.sha256(BINARY.read_bytes()).hexdigest()==HASH
 root=Path(tempfile.mkdtemp(prefix='codex-',dir=REPO/'.verification/phone-permissions'));root.chmod(0o700)
 for name in ['home/.codex','project','outside','tmp','runs','bin']:(root/name).mkdir(parents=True,mode=0o700,exist_ok=True)
 (root/'bin/codex').symlink_to(BINARY);(root/'bin/node').symlink_to(Path(shutil.which('node')).resolve())
 subprocess.run(['git','init','--quiet',str(root/'project')],env={'PATH':os.environ['PATH'],'HOME':str(root/'home'),'GIT_CONFIG_GLOBAL':'/dev/null','GIT_CONFIG_NOSYSTEM':'1'},check=True)
 (root/'project/AGENTS.md').write_text('Artificial boundary project only. No real credentials.\n')
 subprocess.run(['gcc','-O2','-Wall','-Wextra',str(REPO/'scripts/tests/fixtures/local-network-only.c'),'-o',str(root/'local-network-only')],check=True)
 tools=root/'tools';tools.mkdir()
 for name in ['codex-chat.cjs','codex-control.js','agent-receipts.cjs','agent-action-preview.cjs','agent-attachments.cjs','agent-completion-times.cjs']:
  shutil.copy2(REPO/'app/src/main/assets'/name,tools/('codex-wire-v0.2.22.js'if name=='codex-control.js'else name))
 shutil.copy2(Path(__file__).with_name('codex_owned_runner.cjs'),tools/'codex_owned_runner.cjs')
 (root/'identity.json').write_text(json.dumps({'codex':'0.166.0','binary':str(BINARY),'binarySha256':HASH,'scripts':{},'project':str(root/'project')},indent=2)+'\n');return root

def launch(root,command,provider_factory):
 root=root.resolve();meta=json.loads((root/'identity.json').read_text());assert root.is_relative_to(WORKSPACE)and Path.cwd().resolve()==root/'project'
 for name,digest in meta['scripts'].items():assert hashlib.sha256(Path(__file__).with_name(name).read_bytes()).hexdigest()==digest,'Fixture source changed'
 assert command[:4]==['/bin/bash','--noprofile','--norc','-c']and len(command)==5
 provider=provider_factory(root);provider.start();child=None
 try:
  (root/'home/.codex/config.toml').write_text('model="gpt-6.1-sol"\nmodel_provider="audit"\n[model_providers.audit]\nname="Local boundary audit"\nbase_url="'+provider.url+'/v1"\nwire_api="responses"\nrequires_openai_auth=false\n')
  env={'PATH':str(root/'bin')+':'+os.environ['PATH'],'HOME':str(root/'home'),'CODEX_HOME':str(root/'home/.codex'),'TMPDIR':str(root/'tmp'),'TERM':'xterm-256color','COLORTERM':'truecolor','GIT_CONFIG_GLOBAL':'/dev/null','GIT_CONFIG_NOSYSTEM':'1','GIT_CEILING_DIRECTORIES':str(root)}
  child=subprocess.Popen(['node',str(root/'tools/codex_owned_runner.cjs'),*command],env=env,cwd=root/'project');return child.wait()
 finally:
  if child is not None and child.poll()is None:
   child.terminate()
   try:child.wait(timeout=8)
   except subprocess.TimeoutExpired:child.kill();child.wait(timeout=8)
  provider.stop()
