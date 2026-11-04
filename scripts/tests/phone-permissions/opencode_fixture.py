#!/usr/bin/env python3
"""Explicit OpenCode fixture; app launch argv and native permission path retained."""
from pathlib import Path
import os,sys,json,subprocess,hashlib,tempfile,shutil,re,fcntl,shlex
from opencode_provider import Provider
REPO=Path(__file__).resolve().parents[3];WORKSPACE=Path('/root/termux-fork')
BINARY=Path('/root/.local/share/termux-jobs/agents/node_modules/@opencode/cli/bin/opencode.exe');HASH='3549c97d73c8046fe06eecd35ce8c928fc8f45e1eae3e4bf017430c168ba04c9'
DEFAULT='00000000-0000-4000-8000-000000000002'
def prepare():
 assert hashlib.sha256(BINARY.read_bytes()).hexdigest()==HASH
 root=Path(tempfile.mkdtemp(prefix='opencode-',dir=REPO/'.verification/phone-permissions'));root.chmod(0o700)
 for name in ['home','project','outside','tmp','runs','bridge/connections','xdg/config/opencode','xdg/data','xdg/cache','xdg/state','home/.local/share/termux-jobs/agents/bin']:(root/name).mkdir(parents=True,mode=0o700,exist_ok=True)
 for scope in ['project','outside']:
  for kind in ['shell','write']:(root/scope/(kind+'.txt')).write_text('AUDIT_BEFORE')
 subprocess.run(['git','init','--quiet',str(root/'project')],env={'PATH':os.environ['PATH'],'HOME':str(root/'home'),'GIT_CONFIG_GLOBAL':'/dev/null','GIT_CONFIG_NOSYSTEM':'1'},check=True)
 (root/'project/AGENTS.md').write_text('Synthetic test project only. Do not access working profiles.\n')
 guard=root/'local-network-only';subprocess.run(['gcc','-O2','-Wall','-Wextra',str(REPO/'scripts/tests/fixtures/local-network-only.c'),'-o',str(guard)],check=True)
 shim=root/'home/.local/share/termux-jobs/agents/bin/opencode';shim.write_text('#!/bin/bash\nexec '+sys.executable+' '+str(Path(__file__).with_name('opencode_binary.py'))+' '+str(root)+' "$@"\n');shim.chmod(0o700)
 launcher=root/'launch-opencode';launcher.write_text('#!/bin/bash\nset -eu\nexport TERMUX_FORK_TEST_UNIX_ROOT=/tmp/termux-jobs-ai\nexec '+str(guard)+' '+str(root)+'/network-$(date +%s)-$$.jsonl -- '+sys.executable+' '+str(Path(__file__).resolve())+' launch '+str(root)+' "$@"\n');launcher.chmod(0o700)
 scripts={name:hashlib.sha256(Path(__file__).with_name(name).read_bytes()).hexdigest()for name in ['opencode_fixture.py','opencode_provider.py','opencode_binary.py']}
 (root/'identity.json').write_text(json.dumps({'opencode':'2.0.28','node':subprocess.check_output(['node','--version'],text=True).strip(),'binary':str(BINARY),'binarySha256':HASH,'scripts':scripts,'project':str(root/'project'),'launcher':str(launcher)},indent=2)+'\n');print(root);return root

def launch(root,command):
 root=root.resolve();assert root.is_relative_to(WORKSPACE);identity=json.loads((root/'identity.json').read_text())
 for name,digest in identity['scripts'].items():assert hashlib.sha256(Path(__file__).with_name(name).read_bytes()).hexdigest()==digest,'Fixture source changed; prepare a new fixture'
 assert Path.cwd().resolve()==root/'project','Use the fixture project only'
 assert command[:4]==['/bin/bash','--noprofile','--norc','-c']and len(command)==5
 words=shlex.split(command[4]);index=words.index('run-chat');args=words[index+1:index+10];assert len(args)==9,'Missing ordinary OpenCode arguments'
 connection=Path(args[0]);ident=connection.name;assert connection.is_absolute()and connection.parent.name=='connections'and re.fullmatch(r'[a-f0-9-]{36}',ident),'Invalid connection path'
 assert args[4]=='build'and args[7]=='ask'and args[8]=='unrestricted','Use Build / ask and leave project-only mode off for this fixture'
 assert ident!=DEFAULT,'Create a NEW named test connection; never use the main connection'
 with (root/'launch.lock').open('a')as lock:
  fcntl.flock(lock,fcntl.LOCK_EX|fcntl.LOCK_NB)
  owner=root/'claimed-connection.json'
  if owner.exists():assert json.loads(owner.read_text())=={'id':ident,'path':str(connection)},'A different connection already owns this fixture'
  else:
   owner.write_text(json.dumps({'id':ident,'path':str(connection)}))
  # The selected app connection is an identity only. Never inspect, read, or
  # write its existing files. Redirect only this bridge argument to private
  # state; retain every session/model/permission argument unchanged.
  private=root/'bridge/connections'/ident;private.parent.mkdir(parents=True,exist_ok=True,mode=0o700)
  prefix="run-chat '"+str(connection)+"'";assert command[4].count(prefix)==1,'Ambiguous connection argument'
  isolated=command[:4]+[command[4].replace(prefix,"run-chat '"+str(private)+"'",1)]
  routed=shlex.split(isolated[4]);start=routed.index('run-chat');assert routed[start+1]==str(private)and routed[start+2:start+10]==args[1:],'Unexpected argument change'
  provider=Provider(root);provider.start();child=None
  try:
   config={'providers':{'audit':{'canonical':'openai','name':'Synthetic audit','env':['AUDIT_API_KEY'],'package':'@opencode/ai/providers/openai-compatible','settings':{'baseURL':provider.url+'/v1','apiKey':'SYNTHETIC_PHONE_AUDIT_ONLY','transport':'http'},'models':{'audit':{'name':'Synthetic audit','limit':{'context':16384,'output':2048}}}}}}
   (root/'xdg/config/opencode/opencode.json').write_text(json.dumps(config))
   env={'PATH':os.environ['PATH'],'HOME':str(root/'home'),'TMPDIR':str(root/'tmp'),'TERM':'xterm-256color','COLORTERM':'truecolor','LANG':'C.UTF-8','GIT_CONFIG_GLOBAL':'/dev/null','GIT_CONFIG_NOSYSTEM':'1','GIT_CEILING_DIRECTORIES':str(root),'AUDIT_API_KEY':'SYNTHETIC_PHONE_AUDIT_ONLY'}
   child=subprocess.Popen(isolated,env=env,cwd=root/'project');return child.wait()
  finally:
   if child is not None and child.poll()is None:
    child.terminate()
    try:child.wait(timeout=8)
    except subprocess.TimeoutExpired:child.kill();child.wait(timeout=8)
   provider.stop();(root/'runs'/('provider-'+str(os.getpid())+'.json')).write_text(json.dumps({'requests':provider.events,'childExited':child is not None and child.poll()is not None,'privateBridgeState':True},indent=2)+'\n')
def markers(root):
 root=root.resolve();assert root.is_relative_to(WORKSPACE)and(root/'identity.json').is_file()
 for scope in ['project','outside']:
  for kind in ['shell','write']:
   value=(root/scope/(kind+'.txt')).read_bytes();print(scope+'/'+kind+'.txt: '+(value.decode()if len(value)<80 and all(32<=b<127 for b in value)else'[unexpected marker]'))
if __name__=='__main__':
 if sys.argv[1:]==['prepare']:prepare()
 elif len(sys.argv)==3 and sys.argv[1]=='markers':markers(Path(sys.argv[2]))
 elif len(sys.argv)>3 and sys.argv[1]=='launch':sys.exit(launch(Path(sys.argv[2]),sys.argv[3:]))
 else:raise SystemExit('Explicit prepare / markers / launch only')
