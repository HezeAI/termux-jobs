#!/usr/bin/env python3
"""Explicit real Gemini controller/CLI probe. Invoke only under the kernel network guard.
Synthetic per-run broker and loopback provider; no Android/Keystore claim.
"""
from pathlib import Path
import json,os,sys,tempfile,threading,socket,http.server,subprocess,uuid,pty,select,time,shutil,hashlib,struct,fcntl,termios
REPO=Path(__file__).resolve().parents[2]
assert os.environ.get('TERMUX_FORK_GEMINI_LOCAL_PROBE')=='1','Explicit guarded probe only'
root=Path(os.environ['TERMUX_FORK_TEST_UNIX_ROOT']);assert root.is_dir() and root.stat().st_mode&0o077==0
home=root/'home';home.mkdir();project=root/'project';project.mkdir();(home/'.gemini').mkdir()
settings={'security':{'folderTrust':{'enabled':False}},'general':{'enableAutoUpdate':True,'enableAutoUpdateNotification':True},'telemetry':{'enabled':False},'privacy':{'usageStatisticsEnabled':False},'context':{'fileName':'.SYNTHETIC-GEMINI-CONTEXT-ONLY.md'}}
(home/'.gemini/settings.json').write_text(json.dumps(settings))
installed=Path('/root/.local/share/termux-jobs/gemini-cli/0.64.0')
runtime=home/'.local/share/termux-jobs/gemini-cli';runtime.mkdir(parents=True);(runtime/'0.64.0').symlink_to(installed,target_is_directory=True)
node_dir=runtime/'node-v22.22.1-linux-arm64/bin';node_dir.mkdir(parents=True);(node_dir/'node').symlink_to(Path(shutil.which('node')).resolve())
initial={str(p.relative_to(installed)):hashlib.sha256(p.read_bytes()).hexdigest() for p in (installed/'bundle').glob('*.js')}
requests=[];key='SYNTHETIC_INVALID_API_KEY_ONLY'
class Handler(http.server.BaseHTTPRequestHandler):
 def log_message(self,*args):pass
 def answer(self):
  body=self.rfile.read(int(self.headers.get('Content-Length',0)))
  requests.append({'method':self.command,'path':self.path.split('?')[0],'bytes':len(body),'responseStatus':401,'syntheticKeyReceived':self.headers.get('x-goog-api-key')==key})
  payload=json.dumps({'error':{'code':401,'message':'SYNTHETIC invalid API key','status':'UNAUTHENTICATED'}}).encode();self.send_response(401);self.send_header('Content-Type','application/json');self.send_header('Content-Length',str(len(payload)));self.end_headers();self.wfile.write(payload)
 do_POST=answer
 do_GET=answer
server=http.server.ThreadingHTTPServer(('127.0.0.1',0),Handler);threading.Thread(target=server.serve_forever,daemon=True).start()
results=[]
def rpc(path,query):
 with socket.socket(socket.AF_UNIX) as peer:
  peer.settimeout(2);peer.connect(str(path));peer.sendall(json.dumps(query).encode()+b'\n');parts=[]
  while True:
   b=peer.recv(65536)
   if not b:break
   parts.append(b)
  return json.loads(b''.join(parts))
try:
 for attempt in range(4):
  mode="normal" if attempt<2 else "symlink"
  run=str(uuid.uuid4());sock=root/(run+'.sock');address='termux-jobs-gemini-key-'+run;state={'handshake':False,'delivered':False}
  broker=socket.socket(socket.AF_UNIX);broker.bind('\0'+address);broker.listen(1);broker.settimeout(40)
  def deliver(broker=broker,run=run,state=state):
   try:
    peer,_=broker.accept()
    with peer:
     peer.settimeout(5);buf=b''
     while len(buf)<38:buf+=peer.recv(38-len(buf))
     state['handshake']=buf==struct.pack('>H',36)+run.encode()
     if state['handshake']:peer.sendall(struct.pack('>I',len(key))+key.encode());state['delivered']=True
   except (OSError,TimeoutError):pass
   finally:broker.close()
  thread=threading.Thread(target=deliver,daemon=True);thread.start()
  env={'PATH':os.environ['PATH'],'HOME':str(home),'GIT_CEILING_DIRECTORIES':str(root),'TERM':'xterm-256color','COLORTERM':'truecolor','JOBS_GEMINI_KEY_SOCKET':address,'GOOGLE_GEMINI_BASE_URL':'http://127.0.0.1:'+str(server.server_port),'JOBS_GEMINI_AUTH_DIR':str(root/'auth'),'JOBS_GEMINI_ATTACHMENT_ROOT':str(root),'GEMINI_CLI_HOME':str(home),'GEMINI_CLI_NO_RELAUNCH':'1','NO_BROWSER':'1'}
  command='set -e; source "$1"; if [ "$5" = normal ]; then export JOBS_GEMINI_BINARY="$6"; fi; exec "$JOBS_GEMINI_NODE" "$2" run "$3" gemini-2.5-flash default new "$4" api'
  master,slave=pty.openpty();fcntl.ioctl(slave,termios.TIOCSWINSZ,struct.pack('HHHH',28,100,0,0));process=subprocess.Popen(['bash','--noprofile','--norc','-c',command,'probe',str(REPO/'app/src/main/assets/gemini-bootstrap.sh'),str(REPO/'app/src/main/assets/gemini-control.js'),str(sock),run,mode,str(installed/'bundle/gemini.js')],cwd=project,env=env,stdin=slave,stdout=slave,stderr=slave,close_fds=True,start_new_session=True);os.close(slave)
  output=bytearray();sent=False;snapshot={};before=len(requests);policyObserved=False;started=time.monotonic()
  try:
   until=started+65
   while time.monotonic()<until and process.poll() is None:
    if select.select([master],[],[],0.1)[0]:
     try:output.extend(os.read(master,65536));output[:max(0,len(output)-160000)]=b''
     except OSError:break
    policy=root/'auth'/(run+'.json')
    if policy.exists():
     general=json.loads(policy.read_text()).get('general');assert general=={'enableAutoUpdate':False,'enableAutoUpdateNotification':True};policyObserved=True
    if sock.exists():
     try:
      snapshot=rpc(sock,{'op':'chat-snapshot'})
      if snapshot.get('connected') and snapshot.get('nativeUiReady') and not sent and not snapshot.get('needsTerminal'):
       reply=rpc(sock,{'op':'chat-send','id':'msg_auth_probe_'+str(attempt),'kind':'message','text':'SYNTHETIC hello; do not use tools','session':snapshot['session']});sent=reply.get('accepted') is True
     except (OSError,ValueError):pass
    if len(requests)>before and (snapshot.get('agentError') or snapshot.get('authError') or any('SYNTHETIC invalid API key' in r.get('body','') for r in snapshot.get('rows',[]))):break
   result={'pathMode':mode,'attempt':attempt%2+1,'brokerHandshake':state['handshake'],'keyDeliveredToController':state['delivered'],'policyObserved':policyObserved,'chatReady':snapshot.get('connected',False),'messageAdmitted':sent,'authError':snapshot.get('authError',False),'sessionError':snapshot.get('agentError',False),'responseErrorShown':any(r.get('kind')=='error' and ('SYNTHETIC invalid API key' in r.get('body','') or '401' in r.get('body','')) for r in snapshot.get('rows',[])),'providerRefusalVisible':any('SYNTHETIC invalid API key' in r.get('body','') for r in snapshot.get('rows',[])),'providerRequests':requests[before:],'cliAliveAtResult':process.poll() is None,'runtimeAliasPassedUnchangedToController':mode=='symlink','elapsedSeconds':round(time.monotonic()-started,2),'legacyHandoffKeyFiles':list(str(p.relative_to(home)) for p in home.rglob('*.key'))}
   # Artificial-only bounded terminal text, retained separately for failed initialization diagnosis.
   if not requests[before:]:
    import re
    (root/('terminal-attempt-'+str(attempt)+'.txt')).write_text(re.sub(r'\x1b\[[0-9;?]*[a-zA-Z]','',output.decode(errors='replace'))[-12000:].replace(key,'[SYNTHETIC KEY OMITTED]'))
   results.append(result)
  finally:
   if process.poll() is None:
    try:result['stopAcknowledgedAfterChildExit']=rpc(sock,{'op':'stop'}).get('ok') is True
    except (OSError,ValueError):result['stopAcknowledgedAfterChildExit']=False;process.terminate()
    try:process.wait(timeout=12)
    except subprocess.TimeoutExpired:process.kill();process.wait(timeout=5)
   result['controllerExited']=process.poll() is not None;os.close(master);broker.close();thread.join(timeout=1)
 for n,h in initial.items():assert hashlib.sha256((installed/n).read_bytes()).hexdigest()==h,n
 (root/'result.json').write_text(json.dumps({'scope':'real CLI/controller; synthetic broker, loopback 401 provider; no Android/Keystore','results':results,'nativeFilesUnchanged':True},indent=2)+'\n')
 print(json.dumps({'root':str(root),'results':results,'nativeFilesUnchanged':True},indent=2))
 assert all(r['brokerHandshake']and r['keyDeliveredToController']and r['policyObserved']and r['controllerExited']and r.get('stopAcknowledgedAfterChildExit')and r['responseErrorShown']and r['providerRefusalVisible']and any(q['syntheticKeyReceived']for q in r['providerRequests']) for r in results),'Probe did not reach synthetic-key HTTP request; inspect isolated evidence'
finally:server.shutdown();server.server_close()
