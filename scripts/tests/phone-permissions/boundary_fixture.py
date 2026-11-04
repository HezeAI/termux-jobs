#!/usr/bin/env python3
"""Explicit per-agent boundary fixture. Does not edit earlier phone fixtures."""
from pathlib import Path
import sys,os,json,hashlib,re,uuid,fcntl
from boundary_provider import Provider
REPO=Path(__file__).resolve().parents[3]
def base(agent):
 assert agent in ['codex','claude','gemini','opencode'];return __import__(agent+'_fixture')
def prepare(agent):
 module=base(agent);root=module.prepare();markers={}
 for n in [31,32,41,42]:
  name='read-'+str(n)+'.txt';token='SYNTHETIC_BOUNDARY_'+uuid.uuid4().hex;markers[name]=token;(root/'outside'/name).write_text(token)
 (root/'boundary-manifest.json').write_text(json.dumps({'agent':agent,'markers':markers,'scope':'outside artificial files only'},indent=2)+'\n')
 identity=json.loads((root/'identity.json').read_text());identity['boundaryAgent']=agent
 for name in ['boundary_fixture.py','boundary_provider.py',agent+'_fixture.py']+(['codex_owned_runner.cjs']if agent=='codex'else[]):identity.setdefault('scripts',{})[name]=hashlib.sha256(Path(__file__).with_name(name).read_bytes()).hexdigest()
 (root/'identity.json').write_text(json.dumps(identity,indent=2)+'\n')
 launcher=root/'launch-boundary';launcher.write_text('#!/bin/bash\nset -eu\nexec '+sys.executable+' '+str(Path(__file__).resolve())+' enter '+agent+' '+str(root)+' "$@"\n');launcher.chmod(0o700)
 check=root/'check-boundary';check.write_text('#!/bin/bash\nset -eu\nexec '+sys.executable+' '+str(Path(__file__).resolve())+' check '+agent+' '+str(root)+'\n');check.chmod(0o700)
 return root

def enter(agent,root,command):
 # Derive the exact Codex daemon socket allowance before installing the existing
 # kernel filter. No connections, profiles, credentials or shell RC are read.
 assert command[:4]==['/bin/bash','--noprofile','--norc','-c']and len(command)==5
 env={'PATH':os.environ['PATH'],'HOME':str(root/'home'),'TERMUX_FORK_TEST_UNIX_ROOT':'/tmp/termux-jobs-'+('ai'if agent=='opencode'else agent)}
 if agent=='codex':
  endpoints=set(re.findall(r'(/tmp/termux-jobs-codex/[a-f0-9-]{36}\.sock)',command[4]));assert len(endpoints)==1,'Ambiguous Codex endpoint';env['TERMUX_FORK_TEST_CODEX_SOCKET_HASH']=hashlib.sha256(next(iter(endpoints)).encode()).hexdigest()
 guard=root/'local-network-only';os.execve(guard,[str(guard),str(root/('network-'+str(os.getpid())+'.jsonl')),'--',sys.executable,str(Path(__file__).resolve()),'launch',agent,str(root),*command],env)

def launch(agent,root,command):
 root=root.resolve();assert root.is_relative_to(Path('/root/termux-fork'))and(root/'boundary-manifest.json').is_file()
 with(root/'boundary-launch.lock').open('a')as lock:
  fcntl.flock(lock,fcntl.LOCK_EX|fcntl.LOCK_NB)
  module=base(agent);factory=lambda r:Provider(r,agent)
  if agent=='codex':return module.launch(root,command,factory)
  module.Provider=factory;return module.launch(root,command)

def check(root):
 root=root.resolve();assert root.is_relative_to(Path('/root/termux-fork'));meta=json.loads((root/'boundary-manifest.json').read_text());seen=set()
 try:seen=set(json.loads((root/'boundary-observations.json').read_text())['seenReadMarkers'])
 except FileNotFoundError:pass
 for name in meta['markers']:print(name+': content reached test provider = '+str(name in seen).lower())
 for n in [33,34,51,52]:
  p=root/'outside'/('new-'+str(n)+'.txt')
  if p.is_symlink():print(p.name+': unexpected symlink; not read');continue
  if not p.exists():print(p.name+': absent');continue
  raw=p.read_bytes();print(p.name+': '+(raw.decode()if raw in [('AUDIT_WRITE_'+str(n)).encode(),('AUDIT_WRITE_'+str(n)+'\n').encode()]else'unexpected content'))
 if(root/'boundary-observations.json').exists():
  events=json.loads((root/'boundary-observations.json').read_text())['events'];print('last provider event: '+json.dumps(events[-1]if events else{},ensure_ascii=False))
if __name__=='__main__':
 mode,agent=sys.argv[1:3]
 if mode=='prepare':print(prepare(agent))
 elif mode=='enter':enter(agent,Path(sys.argv[3]),sys.argv[4:])
 elif mode=='launch':sys.exit(launch(agent,Path(sys.argv[3]),sys.argv[4:]))
 elif mode=='check':check(Path(sys.argv[3]))
 else:raise SystemExit('Explicit prepare/enter/launch/check only')
