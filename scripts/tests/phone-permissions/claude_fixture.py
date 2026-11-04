#!/usr/bin/env python3
"""Explicit synthetic phone permission fixture; never imports an app profile."""
from pathlib import Path
import json,os,sys,subprocess,tempfile,shutil,hashlib,signal
from claude_provider import Provider
REPO=Path(__file__).resolve().parents[3]
WORKSPACE=Path('/root/termux-fork')
VERSION=Path('/root/.local/share/termux-jobs/claude-code/2.1.296')
BINARY_HASH='f1f6e96e0d8342b9dbf41d7e88255397a6a52ce3d8736ad6a4c6b59c9b62fefa'
def prepare():
 root=Path(tempfile.mkdtemp(prefix='claude-',dir=REPO/'.verification/phone-permissions'));root.chmod(0o700)
 for name in ('home','home/.claude','project/.git','outside','tmp','runs'): (root/name).mkdir(parents=True,exist_ok=True,mode=0o700)
 for scope in ('project','outside'):
  for kind in ('shell','write'):(root/scope/(kind+'.txt')).write_text('AUDIT_BEFORE')
 subprocess.run(['git','init','--quiet',str(root/'project')],env={'PATH':os.environ['PATH'],'HOME':str(root/'home'),'GIT_CONFIG_GLOBAL':'/dev/null','GIT_CONFIG_NOSYSTEM':'1'},check=True)
 binary=VERSION/'claude';assert hashlib.sha256(binary.read_bytes()).hexdigest()==BINARY_HASH
 runtime=root/'home/.local/share/termux-jobs/claude-code';runtime.mkdir(parents=True);(runtime/'2.1.296').symlink_to(VERSION,target_is_directory=True)
 node=Path(shutil.which('node')).resolve();assert subprocess.check_output([str(node),'--version'],text=True).strip()=='v22.22.1'
 (runtime/'node-v22.22.1-linux-arm64/bin').mkdir(parents=True);(runtime/'node-v22.22.1-linux-arm64/bin/node').symlink_to(node)
 (root/'home/.claude/settings.json').write_text('{}');(root/'project/CLAUDE.md').write_text('Synthetic permission fixture only. Do not access other projects.\n')
 guard=root/'local-network-only';subprocess.run(['gcc','-O2','-Wall','-Wextra',str(REPO/'scripts/tests/fixtures/local-network-only.c'),'-o',str(guard)],check=True)
 launcher=root/'launch-claude';launcher.write_text('#!/bin/bash\nset -eu\nexport TERMUX_FORK_TEST_UNIX_ROOT=/tmp/termux-jobs-claude\nexec '+str(guard)+' '+str(root)+'/network-$(date +%s)-$$.jsonl -- '+sys.executable+' '+str(Path(__file__).resolve())+' launch '+str(root)+' "$@"\n');launcher.chmod(0o700)
 (root/'identity.json').write_text(json.dumps({'claude':'2.1.296','binarySha256':BINARY_HASH,'node':'v22.22.1','launcher':str(launcher),'project':str(root/'project'),'scope':'synthetic Anthropic-compatible provider; not Anthropic authorization','scripts':{name:hashlib.sha256((Path(__file__).parent/name).read_bytes()).hexdigest() for name in ['claude_fixture.py','claude_provider.py']}},indent=2)+'\n')
 print(root)
 return root

def environment(root,url):
 # Fresh environment only; no real credentials, proxies or user configuration.
 return {'PATH':os.environ['PATH'],'HOME':str(root/'home'),'CLAUDE_CONFIG_DIR':str(root/'home/.claude'),'TMPDIR':str(root/'tmp'),'TERM':'xterm-256color','COLORTERM':'truecolor','LANG':'C.UTF-8','GIT_CONFIG_GLOBAL':'/dev/null','GIT_CONFIG_NOSYSTEM':'1','GIT_CEILING_DIRECTORIES':str(root),'ANTHROPIC_BASE_URL':url,'ANTHROPIC_API_KEY':'SYNTHETIC_PHONE_AUDIT_ONLY','DISABLE_AUTOUPDATER':'1','CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC':'1','OTEL_SDK_DISABLED':'true'}

def launch(root,command):
 root=root.resolve();assert root.is_relative_to(WORKSPACE) and (root/'identity.json').is_file()
 for name,digest in json.loads((root/'identity.json').read_text()).get('scripts',{}).items():assert hashlib.sha256((Path(__file__).parent/name).read_bytes()).hexdigest()==digest,'Fixture source changed; prepare a new fixture'
 assert Path.cwd().resolve()==root/'project','Use only the fixture project in this test profile'
 assert command[:3]==['/bin/bash','--noprofile','--norc'] and len(command)==5 and command[3]=='-c','Expected the ordinary application launch command'
 provider=Provider(root);provider.start();child=None
 try:
  child=subprocess.Popen(command,env=environment(root,provider.url),cwd=root/'project');return child.wait()
 finally:
  if child is not None and child.poll() is None:
   child.terminate()
   try:child.wait(timeout=8)
   except subprocess.TimeoutExpired:child.kill();child.wait(timeout=8)
  provider.stop();run=root/'runs'/('provider-'+str(os.getpid())+'.json');run.write_text(json.dumps({'requests':provider.events,'childExited':child is not None and child.poll() is not None},indent=2)+'\n')
def markers(root):
 root=root.resolve();assert root.is_relative_to(WORKSPACE) and (root/'identity.json').is_file()
 for scope in ('project','outside'):
  for kind in ('shell','write'):
   value=(root/scope/(kind+'.txt')).read_bytes();print(scope+'/'+kind+'.txt: '+(value.decode() if len(value)<80 and all(32<=b<127 for b in value) else '[unexpected marker]'))

if __name__=='__main__':
 if sys.argv[1:] == ['prepare']:prepare()
 elif len(sys.argv)==3 and sys.argv[1]=='markers':markers(Path(sys.argv[2]))
 elif len(sys.argv)>3 and sys.argv[1]=='launch':sys.exit(launch(Path(sys.argv[2]),sys.argv[3:]))
 else:raise SystemExit('Usage: claude_fixture.py prepare | launch ROOT /bin/bash --noprofile --norc -c COMMAND')
