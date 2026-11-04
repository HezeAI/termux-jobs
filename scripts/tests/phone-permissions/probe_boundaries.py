"""New provider/route coverage. Earlier complete permission matrix is not rerun."""
from pathlib import Path
import os,sys,json,subprocess,socket,uuid,tempfile,shutil,re,time,pty,fcntl,termios,hashlib
from boundary_fixture import prepare,REPO
agent=sys.argv[1];root=prepare(agent);short=Path(tempfile.mkdtemp(prefix='bound-',dir='/root/termux-fork/.verification/t'));short.chmod(0o700);assets=root/'controls';assets.mkdir();run=str(uuid.uuid4());endpoint=short/(run+'.sock');sock=Path(str(endpoint)+'.chat')if agent=='codex'else endpoint
for p in (REPO/'app/src/main/assets').iterdir():
 if p.name.startswith((agent+'-','agent-','ai-agents-'))and p.suffix in ['.js','.cjs','.mjs','.sh','.py','.md']:shutil.copy2(p,assets/p.name)
quote=lambda x:"'"+str(x).replace("'","'\\''")+"'"
if agent=='codex':
 source=Path((REPO/'.verification/phone-boundaries/codex-launch-reference.txt').read_text().strip());command=source.read_text();oldroot=source.parent
 command=command.replace(str(oldroot/'project'),str(root/'project'));oldid=re.search(r'([a-f0-9-]{36})\.sock',command)[1];command=command.replace(str(oldroot),str(short)).replace(oldid,run)
 for src,dest in [('codex-launch.cjs','codex-launch-v1.cjs'),('codex-control.js','bridge-v0.2.21.js'),('codex-control.js','codex-wire-v0.2.22.js'),('codex-chat.cjs','codex-chat-v1.cjs'),('agent-receipts.cjs','agent-receipts.cjs'),('agent-completion-times.cjs','agent-completion-times.cjs'),('agent-action-preview.cjs','agent-action-preview.cjs')]:shutil.copy2(REPO/'app/src/main/assets'/src,short/dest)
 ins=short/re.search(r'instructions-\d+',command)[0];ins.mkdir()
 for name in ['agent-instructions.py','agent-instructions-ubuntu-ai.md']:shutil.copy2(REPO/'app/src/main/assets'/name,ins/name)
else:
 command='set -e; export JOBS_AGENT_UBUNTU=ubuntu-ai; source '+quote(assets/'agent-dependencies.sh')+'; jobs_agent_prepare '+agent+' api; source '+quote(assets/('ai-agents-bootstrap.sh'if agent=='opencode'else agent+'-bootstrap.sh'))+'; '
 if agent=='opencode':command+='exec node '+quote(assets/'ai-agents-control.js')+' run-chat '+quote(assets/'connections'/str(uuid.uuid4()))+' '+quote(endpoint)+' ses'+uuid.uuid4().hex+" '' build new '' ask unrestricted"
 else:command+='exec "$JOBS_'+agent.upper()+'_NODE" '+quote(assets/(agent+'-control-v2.js'if agent=='claude'else agent+'-control.js'))+' run '+quote(endpoint)+(' sonnet manual new 'if agent=='claude'else' gemini-2.5-flash default new ')+quote(run)+(' api'if agent=='gemini'else'')
env={'PATH':os.environ['PATH'],'HOME':str(root/'home'),'TERMUX_FORK_TEST_UNIX_ROOT':str(short)}
if agent=='codex':env['TERMUX_FORK_TEST_CODEX_SOCKET_HASH']=hashlib.sha256(str(endpoint).encode()).hexdigest()
(root/'probe-source.py').write_text(Path(__file__).read_text())
(root/'native-command.sh').write_text(command+'\n')
master,slave=pty.openpty();fcntl.ioctl(slave,termios.TIOCSWINSZ,__import__('struct').pack('HHHH',28,100,0,0))
child=subprocess.Popen([str(root/'local-network-only'),str(root/'network.jsonl'),'--',sys.executable,str(REPO/'scripts/tests/phone-permissions/boundary_fixture.py'),'launch',agent,str(root),'/bin/bash','--noprofile','--norc','-c',command],cwd=root/'project',env=env,stdin=slave,stdout=slave,stderr=slave,close_fds=True);os.close(slave);results=[]
# Drain the PTY even while no rendering assertion is needed.
import threading
terminal=bytearray()
def drain():
 while True:
  try:data=os.read(master,65536)
  except OSError:return
  if not data:return
  terminal.extend(data);terminal[:max(0,len(terminal)-100000)]=b''
threading.Thread(target=drain,daemon=True).start()
def rpc(q):
 with socket.socket(socket.AF_UNIX)as p:
  p.settimeout(4);p.connect(str(sock));p.sendall(json.dumps(q).encode()+b'\n')
  if agent=='opencode':p.shutdown(socket.SHUT_WR)
  b=b''
  while True:
   d=p.recv(65536)
   if not d:break
   b+=d
  return json.loads(b)
def wait(fn,seconds=50):
 end=time.monotonic()+seconds
 while time.monotonic()<end:
  assert child.poll()is None,'Owned child exited'
  if 'remember_owned'in globals():remember_owned(child.pid)
  try:
   v=fn()
   if v:return v
  except(OSError,ValueError):pass
  time.sleep(.1)
 raise TimeoutError('Bounded fixture wait')
# Track only descendants of the actual Popen root using PID/start identities.
owned={}
def remember_owned(pid):
 # Android may omit /proc/PID/task/PID/children. Read only public process metadata,
 # then follow the exact Popen ancestry, as the production cleanup does.
 tree={}
 for entry in Path('/proc').glob('[0-9]*/stat'):
  try:
   fields=entry.read_text().rsplit(') ',1)[1].split();tree[int(entry.parent.name)]=(int(fields[1]),fields[19])
  except (OSError,ValueError,IndexError):pass
 active={pid};changed=True
 while changed:
  changed=False
  for ident,(parent,start)in tree.items():
   if ident in active:owned[ident]=start
   elif parent in active:active.add(ident);owned[ident]=start;changed=True
def still_owned():
 running=[]
 for pid,start in owned.items():
  try:
   fields=Path('/proc/'+str(pid)+'/stat').read_text().split(') ',1)[1].split()
   if fields[19]==start and fields[0]not in ['Z','X']:running.append(pid)
  except (OSError,ValueError,IndexError):pass
 return running
stop=False
try:
 snap=wait(lambda:(lambda v:v if v.get('connected')and not v.get('needsTerminal')else None)(rpc({'op':'chat-snapshot'}))if sock.exists()else None)
 if agent=='opencode':assert rpc({'op':'model','provider':'audit','model':'audit','reopen':True}).get('applied');snap=rpc({'op':'chat-snapshot'})
 # Fresh read files per operation make marker transfer independently observable.
 operations=[('READ',31,'cancel'),('READ',32,'proceed_once'),('SHELL_READ',41,'cancel'),('SHELL_READ',42,'proceed_once'),('WRITE',33,'cancel'),('WRITE',34,'proceed_once'),('SHELL_WRITE',51,'cancel'),('SHELL_WRITE',52,'proceed_once')]
 if len(sys.argv)>2:operations=[v for v in operations if v[1]==int(sys.argv[2])]
 for op,n,outcome in operations:
  sent=rpc({'op':'chat-send','id':'msg_'+uuid.uuid4().hex,'session':snap['session'],'kind':'message','text':'AUDIT '+op+' OUTSIDE '+str(n)});assert sent.get('accepted'),sent;answered=False;gate=False;prompted=[]
  def ready():
   global answered,gate
   v=rpc({'op':'chat-snapshot'});p=v.get('permission')
   if p and not answered:
    if agent=='opencode':allowed=rpc({'op':'permission'}).get('permission');p=allowed or p
    q={'op':'answer','id':p['id'],'outcome':outcome}
    if agent in ['claude','codex']:q.update(session=snap['session'],nonce=p['nonce'])
    assert rpc(q).get('ok');answered=True;prompted.append(p.get('title',p.get('type')))
   if v.get('needsTerminal'):gate=True;return v
   try:events=json.loads((root/'boundary-observations.json').read_text())['events']
   except(FileNotFoundError,ValueError):events=[]
   own=[e for e in events if e.get('number')==n]
   done=own and own[-1].get('kind')in ['tool-result','unavailable','unverified']
   return v if not v.get('busy')and(done or answered)else None
  final=wait(ready)
  gateLabels=[]
  if gate:
   assert rpc({'op':'terminal-open'}).get('ok')
   def native_gate_visible():
    import re
    visible=re.sub(r'\x1b\[[0-?]*[ -/]*[@-~]','',terminal.decode(errors='replace'))
    labels=[v for v in ['external_directory','External directory','external directory','outside','Permission required']if v in visible]
    return labels or None
   gateLabels=wait(native_gate_visible,seconds=8)
   import re
   visible=re.sub(r'\x1b\[[0-?]*[ -/]*[@-~]','',terminal.decode(errors='replace'))
   (root/'native-gate-screen.txt').write_text(visible[-7000:])
   (root/'native-gate-screen.ansi').write_bytes(terminal[-18000:])
   if len(sys.argv)>3:
    decision=sys.argv[3];assert decision in ['reject-gate','allow-gate']
    assert 'esc reject'in visible and 'external directory'in visible and root.name in visible
    if decision=='reject-gate':
     os.write(master,b'\x1b')
     wait(lambda:'Tell OpenCode what to do differently'in re.sub(r'\x1b\[[0-?]*[ -/]*[@-~]','',terminal.decode(errors='replace')),seconds=8)
     os.write(master,b'\r') # submit the native blank rejection explanation
     final=wait(lambda:(lambda v:v if not v.get('busy')and not v.get('needsTerminal')else None)(rpc({'op':'chat-snapshot'})))
    else:
     # Verify native highlight: never choose Always allow by assumed key order.
     assert b'\x1b[48;2;250;178;131mAllow once'in terminal
     assert b'\x1b[48;2;30;30;30mAlways allow'in terminal
     os.write(master,b'\r')
     wait(lambda:not rpc({'op':'chat-snapshot'}).get('needsTerminal'))
     assert rpc({'op':'terminal-close'}).get('ok')
     final=wait(ready)
  obs=json.loads((root/'boundary-observations.json').read_text());seen='read-'+str(n)+'.txt'in obs['seenReadMarkers'];created=(root/'outside'/('new-'+str(n)+'.txt')).exists()
  if answered and outcome=='cancel':assert not seen and not created,'Effect or contents after UI denial'
  results.append({'agent':agent,'operation':op,'number':n,'prompted':prompted,'answer':outcome if answered else None,'nativeTerminalGate':gate,'nativeGateLabels':gateLabels,'gateDecision':sys.argv[3]if len(sys.argv)>3 else None,'pendingFormCount':len(final.get('forms',[])),'markerReachedProvider':seen,'fileCreated':created,'lastEvent':obs['events'][-1]if obs['events']else None});snap=final
  if gate:break # unsupported native gate is not silently granted by this probe
 print(json.dumps(results),flush=True)
finally:
 if agent=='codex':
  # Signal only the real owned Node runner; it uses the existing PID/start-identity
  # checked terminateTree on the shell it spawned, not any other Codex instance.
  try:
   remember_owned(child.pid)
   child.terminate()
  except(ProcessLookupError,FileNotFoundError):pass
 else:
  try:stop=rpc({'op':'stop'}).get('ok')is True
  except(OSError,ValueError):pass
 try:child.wait(timeout=15)
 except subprocess.TimeoutExpired:child.terminate();child.wait(timeout=8)
 os.close(master)
 until_exit=time.monotonic()+8
 while still_owned()and time.monotonic()<until_exit:time.sleep(.1)
 descendantsExited=not still_owned()
 finalObserved=json.loads((root/'boundary-observations.json').read_text())if(root/'boundary-observations.json').exists()else{}
 for r in results:
  if r['answer']=='cancel' or r.get('gateDecision')=='reject-gate':
   assert 'read-'+str(r['number'])+'.txt'not in finalObserved.get('seenReadMarkers',[]),'Late contents after denial'
   assert not(root/'outside'/('new-'+str(r['number'])+'.txt')).exists(),'Late write after denial'
  if r['fileCreated']:assert(root/'outside'/('new-'+str(r['number'])+'.txt')).read_bytes()in [('AUDIT_WRITE_'+str(r['number'])).encode(),('AUDIT_WRITE_'+str(r['number'])+'\n').encode()]
 manifest=json.loads((root/'boundary-manifest.json').read_text())
 assert all((root/'outside'/name).read_text()==value for name,value in manifest['markers'].items()),'Read fixtures changed'
 (root/'result.json').write_text(json.dumps({'source':'assistant native fixture; not phone','agent':agent,'results':results,'ownedLaunchExited':child.poll()is not None,'stopAcknowledged':stop,'trackedProcessIdentities':len(owned),'allTrackedDescendantsExited':descendantsExited,'readFixturesUnchanged':True,'scope':'new boundary provider/main CLI; earlier unchanged matrix separate'},indent=2)+'\n')
 if not results:(root/'failure-terminal.txt').write_bytes(terminal)
 out=REPO/'.verification/phone-boundaries';out.mkdir(exist_ok=True);(out/(agent+('-'+sys.argv[2]if len(sys.argv)>2 else'')+'-root.txt')).write_text(str(root)+'\n')

 assert descendantsExited,'Owned descendants did not finish; do not claim safe cleanup'
