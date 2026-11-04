"""Native main CLI + production bootstrap/controller; synthetic provider, no phone UI."""
from pathlib import Path
import sys,os,subprocess,uuid,socket,json,pty,time,select,shutil,fcntl,termios
from opencode_fixture import REPO,prepare
root=prepare();short=Path(__import__('tempfile').mkdtemp(prefix='po-',dir='/root/termux-fork/.verification/t'));short.chmod(0o700)
assets=root/'controls';assets.mkdir()
for p in (REPO/'app/src/main/assets').iterdir():
 if p.name.startswith(('opencode-','agent-','ai-agents-')) and p.suffix in ('.sh','.js','.cjs','.mjs'):shutil.copy2(p,assets/p.name)
run=str(uuid.uuid4());sock=short/(run+'.sock');connection=assets/'connections'/str(uuid.uuid4())
connection.mkdir(parents=True);sentinel=connection/'artificial-original.txt';sentinel.write_text('KEEP ORIGINAL CONNECTION');originalTime=sentinel.stat().st_mtime_ns
import shlex
command='set -e; export JOBS_AGENT_UBUNTU=ubuntu-ai; source '+shlex.quote(str(assets/'agent-dependencies.sh'))+'; jobs_agent_prepare opencode api; source '+shlex.quote(str(assets/'ai-agents-bootstrap.sh'))+'; exec node '+shlex.quote(str(assets/'ai-agents-control.js'))+' run-chat '+("'"+str(connection)+"'")+' '+shlex.quote(str(sock))+' ses'+uuid.uuid4().hex+" '' build new '' ask unrestricted"
env={'PATH':os.environ['PATH'],'HOME':str(root/'home'),'TERMUX_FORK_TEST_UNIX_ROOT':str(short)}
master,slave=pty.openpty();fcntl.ioctl(slave,termios.TIOCSWINSZ,__import__('struct').pack('HHHH',28,100,0,0))
child=subprocess.Popen([str(root/'local-network-only'),str(root/'native.network.jsonl'),'--',sys.executable,str(REPO/'scripts/tests/phone-permissions/opencode_fixture.py'),'launch',str(root),'/bin/bash','--noprofile','--norc','-c',command],cwd=root/'project',env=env,stdin=slave,stdout=slave,stderr=slave,close_fds=True);os.close(slave)
output=bytearray();results=[]
def drain():
 if select.select([master],[],[],0)[0]:
  try:output.extend(os.read(master,65536));output[:max(0,len(output)-100000)]=b''
  except OSError:pass

def rpc(q):
 with socket.socket(socket.AF_UNIX)as peer:
  peer.settimeout(3);peer.connect(str(sock));peer.sendall(json.dumps(q).encode()+b'\n');peer.shutdown(socket.SHUT_WR);buf=b''
  while True:
   b=peer.recv(65536)
   if not b:break
   buf+=b
  return json.loads(buf)
def until(fn,seconds=50):
 end=time.monotonic()+seconds
 while time.monotonic()<end:
  drain()
  assert child.poll() is None,'Owned fixture process exited'
  try:
   value=fn()
   if value:return value
  except (OSError,ValueError):pass
  time.sleep(.1)
 raise TimeoutError('Synthetic native fixture wait')
try:
 snap=until(lambda:rpc({'op':'chat-snapshot'})if sock.exists()else None)
 snap=until(lambda:(lambda s:s if s.get('connected') and not s.get('needsTerminal')else None)(rpc({'op':'chat-snapshot'})))
 catalog=rpc({'op':'models'});assert any(m.get('provider')=='audit'and m.get('id')=='audit'and m.get('enabled')for m in catalog['models']),'Synthetic model missing'
 selected=rpc({'op':'model','provider':'audit','model':'audit','reopen':True});assert selected.get('applied'),selected
 snap=rpc({'op':'chat-snapshot'});assert snap['model']['providerID']=='audit'and snap['model']['id']=='audit'and snap['model']['variant']in ('','default'),snap['model']
 policy=rpc({'op':'settings-read'});assert policy['access']=='ask'and policy['mode']=='build',policy
 for number,(kind,outcome)in enumerate([('SHELL','cancel'),('SHELL','proceed_once'),('WRITE','cancel'),('WRITE','proceed_once')],1):
  target=root/'project'/(kind.lower()+'.txt');before=target.read_bytes();query='AUDIT '+kind+' INSIDE '+str(number)
  sent=rpc({'op':'chat-send','id':'msg_audit_'+uuid.uuid4().hex,'session':snap['session'],'kind':'message','text':query});assert sent.get('accepted'),sent
  perm=until(lambda:rpc({'op':'permission'}).get('permission'));assert perm['type']==('exec'if kind=='SHELL'else'edit'),perm['type']
  assert (str(target)if kind=='SHELL'else target.name)in perm['body'];reply=rpc({'op':'answer','id':perm['id'],'outcome':outcome});assert reply.get('ok'),reply
  until(lambda:not rpc({'op':'chat-snapshot'}).get('busy'))
  after=target.read_bytes();assert (after==before)if outcome=='cancel'else after==('AUDIT_'+kind+'_'+str(number)).encode(),(kind,outcome,after)
  if outcome=='cancel':
   count=number//2+1
   neutral=until(lambda:(lambda r:r if not r.get('agentError')and len([v for v in r.get('rows',[])if v.get('part')=='error'and v.get('actionOutcome')=='user_declined'])>=count else None)(rpc({'op':'chat-snapshot'})))
   assert not any(v.get('kind')=='error'for v in neutral['rows']);assert any(v.get('status')=='declined'for v in neutral['rows'])
  results.append({'tool':kind,'answer':outcome,'fileCorrect':True,'permissionType':perm['type'],'declinedProjectionChecked':outcome=='cancel'});snap=rpc({'op':'chat-snapshot'})
 assert sentinel.read_text()=='KEEP ORIGINAL CONNECTION'and sentinel.stat().st_mtime_ns==originalTime;assert sorted(p.name for p in connection.iterdir())==['artificial-original.txt']
 assert(root/'outside/shell.txt').read_text()=='AUDIT_BEFORE';assert(root/'outside/write.txt').read_text()=='AUDIT_BEFORE'
 print(json.dumps(results))
finally:
 try:stopped=rpc({'op':'stop'}).get('ok')is True
 except(OSError,ValueError):stopped=False
 try:child.wait(timeout=15)
 except subprocess.TimeoutExpired:child.terminate();child.wait(timeout=8)
 os.close(master)
 (root/'native-proof.json').write_text(json.dumps({'scope':'native controller/bootstrap/CLI with synthetic provider; not phone UI','results':results,'stopAcknowledged':stopped,'processExited':child.poll()is not None,'socketRoot':str(short),'originalConnectionPreserved':sentinel.read_text()=='KEEP ORIGINAL CONNECTION'and sentinel.stat().st_mtime_ns==originalTime},indent=2)+'\n')
 if len(results)!=4:(root/'failure-terminal.txt').write_bytes(output)
 (REPO/'.verification/phone-permissions/latest-opencode-verified-root.txt').write_text(str(root)+'\n')
# Reopen the same owned fixture, command and session after a clean stop. No
# permission or model/tool request may be replayed merely because state exists.
assert len(results)==4 and stopped and child.poll()is not None
master,slave=pty.openpty();fcntl.ioctl(slave,termios.TIOCSWINSZ,__import__('struct').pack('HHHH',28,100,0,0))
child=subprocess.Popen([str(root/'local-network-only'),str(root/'restart.network.jsonl'),'--',sys.executable,str(REPO/'scripts/tests/phone-permissions/opencode_fixture.py'),'launch',str(root),'/bin/bash','--noprofile','--norc','-c',command],cwd=root/'project',env=env,stdin=slave,stdout=slave,stderr=slave,close_fds=True);os.close(slave)
reopened=False;restartStop=False
try:
 view=until(lambda:(lambda v:v if v.get('connected')and not v.get('needsTerminal')else None)(rpc({'op':'chat-snapshot'}))if sock.exists()else None)
 assert not view.get('permission');assert not view.get('busy');assert not view.get('agentError');assert len([r for r in view['rows']if r.get('part')=='error'and r.get('actionOutcome')=='user_declined'])==2;assert not any(r.get('kind')=='error'for r in view['rows']);assert view['session']==snap['session'];assert view['model']['providerID']=='audit'and view['model']['id']=='audit'
 assert (root/'project/shell.txt').read_text()=='AUDIT_SHELL_2';assert (root/'project/write.txt').read_text()=='AUDIT_WRITE_4';assert sentinel.read_text()=='KEEP ORIGINAL CONNECTION'and sentinel.stat().st_mtime_ns==originalTime
 assert rpc({'op':'settings-read'})['access']=='ask';reopened=True
finally:
 try:restartStop=rpc({'op':'stop'}).get('ok')is True
 except(OSError,ValueError):restartStop=False
 try:child.wait(timeout=15)
 except subprocess.TimeoutExpired:child.terminate();child.wait(timeout=8)
 os.close(master)
 (root/'restart-proof.json').write_text(json.dumps({'sameSessionAndModel':reopened,'noPendingPermissionOrAutomaticEffect':reopened,'originalConnectionPreserved':sentinel.read_text()=='KEEP ORIGINAL CONNECTION'and sentinel.stat().st_mtime_ns==originalTime,'stopAcknowledged':restartStop,'processExited':child.poll()is not None},indent=2)+'\n')
assert reopened and restartStop and child.poll()is not None
print('PASS same-fixture reopen; original connection retained; no replayed action; effective ask; owned stop/exit')
