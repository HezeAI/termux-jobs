"""Isolated, kernel-fenced Claude/Gemini context probes; synthetic provider data only."""
import os,sys,json,time,uuid,pty,fcntl,termios,struct,socket,subprocess,select,shutil,http.server,threading,signal,hashlib
from pathlib import Path
REPO=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(REPO/'scripts/tests/phone-permissions'))
import claude_fixture,gemini_fixture
OUT=Path(os.environ['JOBS_CONTEXT_OUTPUT']).resolve();assert OUT.is_relative_to(REPO/'.verification')
# Fail closed unless our tested kernel supervisor is the actual parent.
assert Path('/proc',str(os.getppid()),'exe').resolve()==OUT/'local-network-only','Run through the local-network-only supervisor'
status=Path('/proc/self/status').read_text();assert 'NoNewPrivs:\t1' in status and 'Seccomp:\t2' in status
OUT.mkdir(parents=True,exist_ok=True)
if sys.argv[1:]==['--preflight']:print('PASS owned kernel guard preflight');sys.exit(0)
class Provider:
 def __init__(self,root,agent):
  self.root=root;self.agent=agent;self.phase='tools';self.step=0;self.events=[];owner=self
  class Handler(http.server.BaseHTTPRequestHandler):
   def log_message(self,*a):pass
   def do_GET(self):self.answer({'name':'models/gemini-3-pro-preview','inputTokenLimit':1048576})
   def answer(self,data,status=200):
    raw=json.dumps(data).encode();self.send_response(status);self.send_header('Content-Type','application/json');self.send_header('Content-Length',str(len(raw)));self.end_headers();self.wfile.write(raw)
   def do_POST(self):
    size=int(self.headers.get('Content-Length','0'));assert 0<size<4*1024*1024;q=json.loads(self.rfile.read(size))
    if 'count_tokens' in self.path:return self.answer({'input_tokens':50})
    if 'countTokens' in self.path:return self.answer({'totalTokens':50})
    step=owner.step;owner.step+=1;tool=owner.phase=='tools' and step<2;used=100*(step+1) if owner.agent=='claude' else 111*(step+1)
    if owner.agent=='gemini' and owner.phase=='auto' and 'streamGenerateContent' in self.path:used=40000
    owner.events.append({'phase':owner.phase,'step':step,'bytes':size,'tool':tool,'used':used,'path':self.path,'requestedModel':q.get('model'),'schemaFields':list(q.get('generationConfig',{}).get('responseJsonSchema',q.get('generationConfig',{}).get('responseSchema',{})).get('properties',{}))})
    if owner.agent=='claude':
     name='Read';data={'file_path':str(root/'project'/('a.txt' if step==0 else 'b.txt'))};model=q.get('model');id='ctx_'+str(len(owner.events));content={'type':'tool_use','id':id,'name':name,'input':data}if tool else{'type':'text','text':'Synthetic context answer / compact summary '+owner.phase}
     self.send_response(200);self.send_header('Content-Type','text/event-stream');self.end_headers()
     def emit(t,v):self.wfile.write(('event: '+t+'\ndata: '+json.dumps(v)+'\n\n').encode());self.wfile.flush()
     emit('message_start',{'type':'message_start','message':{'id':id,'type':'message','role':'assistant','model':model,'content':[],'stop_reason':None,'stop_sequence':None,'usage':{'input_tokens':used,'cache_creation_input_tokens':20,'cache_read_input_tokens':50,'output_tokens':1}}})
     emit('content_block_start',{'type':'content_block_start','index':0,'content_block':{**content,'input':{}}if tool else{'type':'text','text':''}})
     emit('content_block_delta',{'type':'content_block_delta','index':0,'delta':{'type':'input_json_delta','partial_json':json.dumps(data)}if tool else{'type':'text_delta','text':content['text']}})
     emit('content_block_stop',{'type':'content_block_stop','index':0});emit('message_delta',{'type':'message_delta','delta':{'stop_reason':'tool_use'if tool else'end_turn','stop_sequence':None},'usage':{'output_tokens':7}});emit('message_stop',{'type':'message_stop'});return
    model=self.path.split('/models/')[-1].split(':')[0];part={'functionCall':{'name':'read_file','args':{'file_path':str(root/'project'/('a.txt'if step==0 else'b.txt'))}}}if tool else{'text':'Synthetic context answer / compact summary '+owner.phase}
    if owner.phase=='compact':part={'text':'<state_snapshot><overall_goal>Synthetic context fixture only.</overall_goal></state_snapshot>'}
    schema=q.get('generationConfig',{}).get('responseJsonSchema')or q.get('generationConfig',{}).get('responseSchema')
    if schema and 'model_choice' in schema.get('properties',{}):part={'text':json.dumps({'reasoning':'Artificial simple request','model_choice':schema['properties']['model_choice'].get('enum',['flash'])[0]})}
    if schema and 'complexity_score' in schema.get('properties',{}):part={'text':json.dumps({'complexity_reasoning':'Artificial simple request','complexity_score':1})}
    body={'candidates':[{'content':{'role':'model','parts':[part]},'finishReason':'STOP','index':0}],'usageMetadata':{'promptTokenCount':used,'cachedContentTokenCount':70,'candidatesTokenCount':7,'totalTokenCount':used+7},'modelVersion':model,'responseId':'ctx_'+str(len(owner.events))}
    if owner.phase=='missing':body.pop('usageMetadata',None)
    raw=json.dumps(body).encode();stream='streamGenerateContent'in self.path;raw=b'data: '+raw+b'\n\n'if stream else raw;self.send_response(200);self.send_header('Content-Type','text/event-stream'if stream else'application/json');self.send_header('Content-Length',str(len(raw)));self.end_headers();self.wfile.write(raw)
  self.server=http.server.ThreadingHTTPServer(('127.0.0.1',0),Handler);self.thread=threading.Thread(target=self.server.serve_forever,daemon=True);self.thread.start()
 @property
 def url(self):return 'http://127.0.0.1:'+str(self.server.server_port)
 def phase_start(self,name):self.phase=name;self.step=0;print('PHASE '+self.agent+' '+name,flush=True)
 def stop(self):self.server.shutdown();self.server.server_close();self.thread.join()
def identity(pid):
 try:return Path('/proc',str(pid),'stat').read_text().rsplit(')',1)[1].split()[19]
 except FileNotFoundError:return None
def stop_owned(child,ident):
 if child.poll()is not None:return
 assert identity(child.pid)==ident;child.terminate()
 try:child.wait(timeout=12)
 except subprocess.TimeoutExpired:
  assert identity(child.pid)==ident;child.kill();child.wait(timeout=8)
def run(agent):
 fixture=claude_fixture if agent=='claude'else gemini_fixture;root=fixture.prepare();assets=root/'controls';assets.mkdir()
 for p in(REPO/'app/src/main/assets').iterdir():
  if p.name.startswith(('claude-','gemini-','agent-'))and p.suffix in('.sh','.js','.cjs','.mjs'):shutil.copy2(p,assets/p.name)
 short=Path(os.environ['TERMUX_FORK_TEST_UNIX_ROOT']);session=str(uuid.uuid4());sock=short/(str(uuid.uuid4())+'.sock');provider=Provider(root,agent);output=bytearray();child=None;master=None;owner_id=None;result={'agent':agent,'root':str(root),'scope':'real pinned CLI with synthetic loopback provider; no Android/real authorization'}
 for n in['a.txt','b.txt']:(root/'project'/n).write_text('Artificial file '+n)
 def start(resume=False,selected=None):
  nonlocal child,master,owner_id
  model=selected or('sonnet'if agent=='claude'else'gemini-2.5-flash');script='claude-control-v2.js'if agent=='claude'else'gemini-control.js';bootstrap=agent+'-bootstrap.sh'
  command=f'set -e; export JOBS_AGENT_UBUNTU=ubuntu-ai; source "{assets}/agent-dependencies.sh"; jobs_agent_prepare {agent} api; source "{assets}/{bootstrap}"; exec "$JOBS_'+('CLAUDE_NODE'if agent=='claude'else'GEMINI_NODE')+f'" "{assets}/{script}" run "{sock}" {model} '+('manual'if agent=='claude'else'default')+(' id 'if resume else' new ')+session+(' api'if agent=='gemini'else'')
  env=fixture.environment(root,provider.url);env['TERMUX_FORK_TEST_UNIX_ROOT']=str(short)
  master,slave=pty.openpty();fcntl.ioctl(slave,termios.TIOCSWINSZ,struct.pack('HHHH',32,100,0,0));child=subprocess.Popen(['/bin/bash','--noprofile','--norc','-c',command],cwd=root/'project',env=env,stdin=slave,stdout=slave,stderr=slave,close_fds=True);owner_id=identity(child.pid);os.close(slave)
 def drain():
  if master is not None and select.select([master],[],[],0)[0]:
   try:output.extend(os.read(master,65536));output[:max(0,len(output)-150000)]=b''
   except OSError:pass
 def rpc(q):
  with socket.socket(socket.AF_UNIX)as p:
   p.settimeout(3);p.connect(str(sock));p.sendall(json.dumps(q).encode()+b'\n');data=b''
   while True:
    chunk=p.recv(65536)
    if not chunk:break
    data+=chunk
   return json.loads(data)
 def wait(fn,seconds=60):
  end=time.monotonic()+seconds
  while time.monotonic()<end:
   drain();assert child.poll()is None,'Native controller exited'
   try:
    value=fn()
    if value:return value
   except (OSError,ValueError):pass
   time.sleep(.07)
  raise TimeoutError('Native '+agent+' condition timed out')
 def snap():return rpc({'op':'chat-snapshot'})
 def ctx():return rpc({'op':'status'})if agent=='claude'else json.loads(sock.with_suffix('.status.json').read_text())
 def finish():
  def done():
   v=snap();return not v.get('busy') and not v.get('requestTiming') and any(('Synthetic context answer / compact summary '+provider.phase) in str(row.get('body','')) for row in v.get('rows',[]))
  wait(done);return ctx().get('context')
 def send(text):
  q={'op':'chat-send','id':'msg_'+uuid.uuid4().hex,'kind':'message','session':session,'text':text};r=rpc(q);result.setdefault('admissions',[]).append({'textKind':'compact'if text.startswith('/')else'message','ok':r.get('ok'),'accepted':r.get('accepted')});return r
 def close():
  nonlocal master
  try:rpc({'op':'stop'})
  except(OSError,ValueError):pass
  try:child.wait(timeout=15)
  except subprocess.TimeoutExpired:stop_owned(child,owner_id)
  if master is not None:os.close(master);master=None
 try:
  start();wait(lambda:snap().get('connected')and not snap().get('needsTerminal'));session=snap()['session'];result['initial']=ctx().get('context');assert not result['initial'].get('usage')
  send('CTXTOOLS Read the two artificial files');c=finish();result['afterTools']=c;assert c and c['usage'] and not c['stale'],c
  assert len([e for e in provider.events if e['phase']=='tools' and e['tool']])==2
  expected=370 if agent=='claude'else 333;assert c['usage']['used']==expected,(expected,c)
  if agent=='claude':assert c['usage']['capacity']>0,c
  result['providerEvents']=list(provider.events);provider.phase_start('model')
  if agent=='claude':rpc({'op':'set-model','model':'haiku'})
  else:
   rpc({'op':'model','model':'gemini-2.5-pro','id':uuid.uuid4().hex});wait(lambda:ctx().get('change',{}).get('state')=='applied')
  send('CTXMODEL Continue with selected model');result['afterModel']=finish();assert result['afterModel']['usage']['model']!=result['afterTools']['usage']['model'];assert result['afterModel']['usage']['capacity']>0
  if agent=='gemini':
   provider.phase_start('auto');rpc({'op':'model','model':'auto','id':uuid.uuid4().hex});wait(lambda:ctx().get('change',{}).get('state')=='applied');send('CTXAUTO Simple synthetic answer');result['afterAuto']=finish();assert result['afterAuto']['usage']['model'].startswith('gemini-'),result['afterAuto'];assert result['afterAuto']['usage']['model']!='auto'
  # Explicit native command in this disposable chat only; no UI compaction button is added.
  provider.phase_start('compact');send('/compact'if agent=='claude'else'/compress');time.sleep(1);drain();wait(lambda:not snap().get('busy') and not snap().get('requestTiming'));result['afterCompaction']=ctx().get('context')
  assert result['afterCompaction']['usage']['source']=='native-compression',result['afterCompaction'];assert not result['afterCompaction']['stale']
  if agent=='gemini':
   provider.phase_start('missing');send('CTXMISSING Synthetic reply without usage statistics');result['missingStatistics']=finish();assert result['missingStatistics']['stale'];assert result['missingStatistics']['usage']['used']==result['afterCompaction']['usage']['used']
  provider.phase_start('after-compact');send('CTXAFTER Synthetic follow-up');result['afterFollowup']=finish();assert not result['afterFollowup']['stale']
  old=result['afterFollowup']['usage'];close();start(True);wait(lambda:snap().get('connected')and not snap().get('needsTerminal'));result['reopened']=ctx().get('context');assert result['reopened']['usage']['used']==old['used'];assert result['reopened']['stale']
  original_session=session;close();session=str(uuid.uuid4());provider.phase_start('other-chat');start();wait(lambda:snap().get('connected')and not snap().get('needsTerminal'));session=snap()['session'];result['otherChatInitial']=ctx().get('context');assert not result['otherChatInitial']['usage'];send('CTXOTHER Separate synthetic chat');result['otherChat']=finish();close();session=original_session;start(True);wait(lambda:snap().get('connected')and not snap().get('needsTerminal'));result['switchedBack']=ctx().get('context');assert result['switchedBack']['usage']['used']==old['used'] and result['switchedBack']['stale']
  result['success']=True
 finally:
  if child:close()
  provider.stop();result['providerEvents']=provider.events;result['processExited']=child is not None and child.poll()is not None;(OUT/(agent+'-native.json')).write_text(json.dumps(result,indent=2)+'\n')
  if not result.get('success'):(OUT/(agent+'-terminal-fixture.txt')).write_bytes(output)
 print(json.dumps(result,indent=2))
for agent in sys.argv[1:]or['claude','gemini']:run(agent)
