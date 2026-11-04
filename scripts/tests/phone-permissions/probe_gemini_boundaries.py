"""Native main CLI + production bootstrap/controller; synthetic provider, no phone UI."""
from pathlib import Path
import sys,os,subprocess,uuid,socket,json,pty,time,select,shutil,fcntl,termios
from gemini_fixture import REPO,prepare
root=prepare();short=Path(__import__('tempfile').mkdtemp(prefix='pg-',dir='/root/termux-fork/.verification/t'));short.chmod(0o700)
assets=root/'controls';assets.mkdir()
for p in (REPO/'app/src/main/assets').iterdir():
 if p.name.startswith(('gemini-','agent-')) and p.suffix in ('.sh','.js','.cjs','.mjs'):shutil.copy2(p,assets/p.name)
run=str(uuid.uuid4());sock=short/(run+'.sock')
command='set -e; export JOBS_AGENT_UBUNTU=ubuntu-ai; source "$1/agent-dependencies.sh"; jobs_agent_prepare gemini api; source "$1/gemini-bootstrap.sh"; exec "$JOBS_GEMINI_NODE" "$1/gemini-control.js" run "$2" gemini-2.5-flash default new "$3" api'
# The ordinary app command has the same preparation, bootstrap, controller and
# policy; paths below refer exclusively to this disposable controls/socket copy.
import shlex
command=command.replace('"$1', '"'+str(assets)).replace('"$2"',shlex.quote(str(sock))).replace('"$3"',shlex.quote(run))
env={'PATH':os.environ['PATH'],'HOME':str(root/'home'),'TERMUX_FORK_TEST_UNIX_ROOT':str(short)}
master,slave=pty.openpty();fcntl.ioctl(slave,termios.TIOCSWINSZ,__import__('struct').pack('HHHH',28,100,0,0))
child=subprocess.Popen([str(root/'local-network-only'),str(root/'native.network.jsonl'),'--',sys.executable,str(REPO/'scripts/tests/phone-permissions/gemini_fixture.py'),'launch',str(root),'/bin/bash','--noprofile','--norc','-c',command],cwd=root/'project',env=env,stdin=slave,stdout=slave,stderr=slave,close_fds=True);os.close(slave)
output=bytearray();results=[]
def drain():
 if select.select([master],[],[],0)[0]:
  try:output.extend(os.read(master,65536));output[:max(0,len(output)-100000)]=b''
  except OSError:pass

def rpc(q):
 with socket.socket(socket.AF_UNIX)as peer:
  peer.settimeout(3);peer.connect(str(sock));peer.sendall(json.dumps(q).encode()+b'\n');buf=b''
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
 snap=until(lambda:(lambda s:s if s.get('connected') and s.get('nativeUiReady') and not s.get('needsTerminal')else None)(rpc({'op':'chat-snapshot'})))
 for number,outcome in [(11,'cancel'),(12,'proceed_once'),(12,'cancel')]:
  target=root/'outside/shell.txt';before=target.read_bytes();mtime=target.stat().st_mtime_ns
  sent=rpc({'op':'chat-send','id':'msg_boundary_'+uuid.uuid4().hex,'session':snap['session'],'kind':'message','text':'AUDIT SHELL OUTSIDE '+str(number)});assert sent.get('accepted')
  perm=until(lambda:rpc({'op':'permission'}).get('permission'));assert perm['type']=='exec'and str(target)in perm['body']
  assert rpc({'op':'answer','id':perm['id'],'outcome':outcome}).get('ok');until(lambda:not rpc({'op':'chat-snapshot'}).get('busy'))
  after=target.read_bytes();assert (after==before and target.stat().st_mtime_ns==mtime)if outcome=='cancel'else after==b'AUDIT_SHELL_12'
  results.append({'tool':'shell','scope':'outside','number':number,'answer':outcome,'fileCorrect':True,'mtimeCorrectOnDeny':outcome=='cancel'});snap=rpc({'op':'chat-snapshot'})
 sent=rpc({'op':'chat-send','id':'msg_boundary_'+uuid.uuid4().hex,'session':snap['session'],'kind':'message','text':'AUDIT WRITE OUTSIDE 13'});assert sent.get('accepted')
 permissionSeen=False
 def direct_result():
  global permissionSeen
  p=rpc({'op':'permission'}).get('permission')
  if p:
   permissionSeen=True;assert rpc({'op':'answer','id':p['id'],'outcome':'cancel'}).get('ok')
  r=rpc({'op':'chat-snapshot'});rows=r.get('rows',[]);pos=next((i for i,v in enumerate(rows)if v.get('kind')=='user'and 'AUDIT WRITE OUTSIDE 13'in v.get('body','')),-1);return r if pos>=0 and not r.get('busy')and any(v.get('kind')=='text'and 'AUDIT FINISHED'in v.get('body','')for v in rows[pos+1:])else None
 final=until(direct_result);assert(root/'outside/write.txt').read_bytes()==b'AUDIT_BEFORE'
 results.append({'tool':'write','scope':'outside','permissionCardSeen':permissionSeen,'outsideFileUnchanged':True,'result':'UI denial'if permissionSeen else'no permission observed; inspect native result, not UI-denial proof'})
 assert(root/'project/shell.txt').read_text()=='AUDIT_BEFORE';assert(root/'project/write.txt').read_text()=='AUDIT_BEFORE'
 print(json.dumps(results))
finally:
 try:stopped=rpc({'op':'stop'}).get('ok')is True
 except(OSError,ValueError):stopped=False
 try:child.wait(timeout=15)
 except subprocess.TimeoutExpired:child.terminate();child.wait(timeout=8)
 os.close(master)
 (root/'native-proof.json').write_text(json.dumps({'scope':'native controller/bootstrap/CLI with synthetic provider; not phone UI','results':results,'stopAcknowledged':stopped,'processExited':child.poll()is not None,'socketRoot':str(short)},indent=2)+'\n')
 if len(results)!=4:(root/'failure-terminal.txt').write_bytes(output)
 (REPO/'.verification/phone-permissions/latest-gemini-boundaries-root.txt').write_text(str(root)+'\n')
