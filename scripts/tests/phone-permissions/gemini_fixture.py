#!/usr/bin/env python3
"""Explicit synthetic phone permission fixture; never imports an app profile."""
from pathlib import Path
import json,os,sys,subprocess,tempfile,shutil,hashlib,signal
from gemini_provider import Provider
REPO=Path(__file__).resolve().parents[3]
WORKSPACE=Path('/root/termux-fork')
VERSION=Path('/root/.local/share/termux-jobs/gemini-cli/0.64.0')
BINARY_HASH='ab113d84c8f1692fd6c95081366718897876d8664328c7bf0ccf9437d688145f'
def prepare():
 root=Path(tempfile.mkdtemp(prefix='gemini-',dir=REPO/'.verification/phone-permissions'));root.chmod(0o700)
 for name in ('home','home/.gemini','project/.git','outside','tmp','runs'): (root/name).mkdir(parents=True,exist_ok=True,mode=0o700)
 for scope in ('project','outside'):
  for kind in ('shell','write'):(root/scope/(kind+'.txt')).write_text('AUDIT_BEFORE')
 subprocess.run(['git','init','--quiet',str(root/'project')],env={'PATH':os.environ['PATH'],'HOME':str(root/'home'),'GIT_CONFIG_GLOBAL':'/dev/null','GIT_CONFIG_NOSYSTEM':'1'},check=True)
 binary=VERSION/'bundle/gemini.js';assert hashlib.sha256(binary.read_bytes()).hexdigest()==BINARY_HASH
 runtime=root/'home/.local/share/termux-jobs/gemini-cli';runtime.mkdir(parents=True);(runtime/'0.64.0').symlink_to(VERSION,target_is_directory=True)
 node=Path(shutil.which('node')).resolve();assert subprocess.check_output([str(node),'--version'],text=True).strip()=='v22.22.1'
 (runtime/'node-v22.22.1-linux-arm64/bin').mkdir(parents=True);(runtime/'node-v22.22.1-linux-arm64/bin/node').symlink_to(node)
 settings={'security':{'folderTrust':{'enabled':False}},'telemetry':{'enabled':False},'privacy':{'usageStatisticsEnabled':False},'context':{'fileName':'.AUDIT-ONLY.md'},'general':{'enableAutoUpdate':False,'enableAutoUpdateNotification':True}}
 (root/'home/.gemini/settings.json').write_text(json.dumps(settings));(root/'project/.AUDIT-ONLY.md').write_text('Synthetic permission fixture only. Do not access other projects.\n')
 guard=root/'local-network-only';subprocess.run(['gcc','-O2','-Wall','-Wextra',str(REPO/'scripts/tests/fixtures/local-network-only.c'),'-o',str(guard)],check=True)
 launcher=root/'launch-gemini';launcher.write_text('#!/bin/bash\nset -eu\nexport TERMUX_FORK_TEST_UNIX_ROOT=/tmp/termux-jobs-gemini\nexec '+str(guard)+' '+str(root)+'/network-$(date +%s)-$$.jsonl -- '+sys.executable+' '+str(Path(__file__).resolve())+' launch '+str(root)+' "$@"\n');launcher.chmod(0o700)
 (root/'identity.json').write_text(json.dumps({'gemini':'0.64.0','binarySha256':BINARY_HASH,'node':'v22.22.1','launcher':str(launcher),'project':str(root/'project'),'scope':'synthetic Gemini API-compatible provider; not Google authorization','scripts':{name:hashlib.sha256((Path(__file__).parent/name).read_bytes()).hexdigest() for name in ['gemini_fixture.py','gemini_provider.py']}},indent=2)+'\n')
 print(root)
 return root

def environment(root,url):
 # Construct a fresh child environment. Never inspect/copy credentials/proxies.
 return {'PATH':os.environ['PATH'],'HOME':str(root/'home'),'GEMINI_CLI_HOME':str(root/'home'),'TMPDIR':str(root/'tmp'),'TERM':'xterm-256color','COLORTERM':'truecolor','LANG':'C.UTF-8','GIT_CONFIG_GLOBAL':'/dev/null','GIT_CONFIG_NOSYSTEM':'1','GIT_CEILING_DIRECTORIES':str(root),'GOOGLE_GEMINI_BASE_URL':url,'GEMINI_API_KEY':'SYNTHETIC_PHONE_AUDIT_ONLY','GEMINI_CLI_NO_RELAUNCH':'1','NO_BROWSER':'1','OTEL_SDK_DISABLED':'true','GEMINI_TELEMETRY_ENABLED':'false','JOBS_GEMINI_AUTH_DIR':str(root/'home/.local/share/termux-jobs/gemini-cli/auth')}

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
 else:raise SystemExit('Usage: gemini_fixture.py prepare | launch ROOT /bin/bash --noprofile --norc -c COMMAND')
