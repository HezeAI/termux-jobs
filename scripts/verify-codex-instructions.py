#!/usr/bin/env python3
"""Actual Codex app-server instruction loading against an isolated, credential-free HTTP fixture."""
import argparse,json,os,selectors,subprocess,threading,time,uuid,importlib.util,hashlib
from pathlib import Path
from http.server import BaseHTTPRequestHandler,ThreadingHTTPServer


def verify(binary,out):
 binary=str(Path(binary).resolve())
 out.mkdir(parents=True,exist_ok=False);home=out/'codex-home';home.mkdir();project=out/'project';project.mkdir();(project/'.git').mkdir();captures=[]
 class Handler(BaseHTTPRequestHandler):
  def log_message(self,*args):pass
  def do_GET(self):
   data=json.dumps({'data':[]}).encode();self.send_response(200);self.send_header('Content-Type','application/json');self.send_header('Content-Length',str(len(data)));self.end_headers();self.wfile.write(data)
  def do_POST(self):
   body=json.loads(self.rfile.read(int(self.headers['Content-Length'])));captures.append(body)
   # Only fake provider traffic. Never log auth headers or load a real profile.
   response_id='resp_'+uuid.uuid4().hex;item={'id':'msg_'+uuid.uuid4().hex,'type':'message','role':'assistant','status':'completed','content':[{'type':'output_text','text':'fixture ok','annotations':[]}]}
   events=[{'type':'response.created','response':{'id':response_id,'status':'in_progress','output':[]}}, {'type':'response.output_item.done','output_index':0,'item':item}, {'type':'response.completed','response':{'id':response_id,'status':'completed','output':[item],'usage':{'input_tokens':10,'output_tokens':2,'total_tokens':12}}}]
   self.send_response(200);self.send_header('Content-Type','text/event-stream');self.end_headers()
   for e in events:self.wfile.write(('event: '+e['type']+'\ndata: '+json.dumps(e)+'\n\n').encode())
   self.wfile.flush()
 server=ThreadingHTTPServer(('127.0.0.1',0),Handler);thread=threading.Thread(target=server.serve_forever,daemon=True);thread.start()
 (home/'config.toml').write_text('model = "gpt-5-codex"\nmodel_provider = "instruction_fixture"\nweb_search = "disabled"\n[features]\napps = false\n[model_providers.instruction_fixture]\nname = "Local instruction fixture"\nbase_url = "http://127.0.0.1:'+str(server.server_port)+'/v1"\nwire_api = "responses"\nrequires_openai_auth = false\nsupports_websockets = false\n')
 env=dict(os.environ,CODEX_HOME=str(home));env.pop('CODEX_GEOHIDE_INSTANCE',None)
 for key in list(env):
  if any(x in key.upper() for x in ('API_KEY','TOKEN','AUTHORIZATION')) or key.lower().endswith('_proxy'):env.pop(key,None)
 env['NO_PROXY']='127.0.0.1,localhost';env['CODEX_DISABLE_RESUMABLE_HISTORY']='0'
 class Rpc:
  def __init__(self,index):
   self.log=(out/('process-'+str(index)+'.log')).open('wb');self.p=subprocess.Popen([binary,'--no-daemon','app-server'],stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=self.log,env=env,cwd=project,bufsize=0);self.serial=0;self.buffer=b'';self.selector=selectors.DefaultSelector();self.selector.register(self.p.stdout,selectors.EVENT_READ)
   self.call('initialize',{'clientInfo':{'name':'termux_jobs_instruction_fixture','version':'1'},'capabilities':{'experimentalApi':True}});self.send({'method':'initialized'})
  def send(self,value):self.p.stdin.write(json.dumps(value).encode()+b'\n');self.p.stdin.flush()
  def receive(self,deadline):
   while b'\n' not in self.buffer:
    if time.monotonic()>deadline:raise RuntimeError('Fixture RPC timeout')
    if self.selector.select(.1):
     data=os.read(self.p.stdout.fileno(),65536)
     if not data:raise RuntimeError('Fixture app-server ended; see local process log')
     self.buffer+=data
   line,self.buffer=self.buffer.split(b'\n',1);return json.loads(line)
  def call(self,method,params):
   self.serial+=1;ident=self.serial;self.send({'id':ident,'method':method,'params':params});end=time.monotonic()+30
   while True:
    v=self.receive(end)
    if v.get('id')==ident:
     if 'error' in v:raise RuntimeError(method+': '+json.dumps(v['error']))
     return v['result']
  def turn(self,ident,label):
   start=len(captures);self.call('turn/start',{'threadId':ident,'input':[{'type':'text','text':'Return fixture ok. Do not run any tools. '+label}]});end=time.monotonic()+35
   while True:
    v=self.receive(end)
    if v.get('method')=='turn/completed':
     if not len(captures)>start:raise RuntimeError('No real provider request captured')
     return captures[-1]
  def close(self):
   self.selector.close()
   if self.p.poll() is None:
    self.p.stdin.close()
    try:self.p.wait(timeout=3)
    except subprocess.TimeoutExpired:
     self.p.terminate()
     try:self.p.wait(timeout=3)
     except subprocess.TimeoutExpired:self.p.kill();self.p.wait(timeout=3)
   self.log.close()
 def markers(body):
  data=json.dumps(body,ensure_ascii=False)
  return {n:{'present':n in data,'lastIndex':data.rfind(n)} for n in ('GLOBAL_RULE_A','PROJECT_RULE_A','GLOBAL_RULE_B','PROJECT_RULE_B')}
 proc=None
 try:
  module=Path(__file__).resolve().parents[1]/'app/src/main/assets/agent-instructions.py'
  spec=importlib.util.spec_from_file_location('instruction_editor_proof',module);editor_module=importlib.util.module_from_spec(spec);spec.loader.exec_module(editor_module)
  template=out/'template.md';template.write_text('For this fixture GLOBAL_RULE_A.\n');editor=editor_module.Editor(template,environ=env);editor.initialize()
  def save(scope,text):
   request={'scope':scope,'project':str(project),'name':'AGENTS.md'};view=editor.view(request);assert view['path']==str((home if scope=='general' else project)/'AGENTS.md');editor.save(dict(request,path=view['path'],revision=view['revision'],text=text))
  save('project','For this fixture PROJECT_RULE_A.\n')
  params={'cwd':str(project),'approvalPolicy':'never','sandbox':'read-only','persistExtendedHistory':True}
  proc=Rpc(1);ident=proc.call('thread/start',params)['thread']['id'];first=markers(proc.turn(ident,'FIRST'))
  assert first['GLOBAL_RULE_A']['present'] and first['PROJECT_RULE_A']['present'],first
  save('general','For this fixture GLOBAL_RULE_B.\n');save('project','For this fixture PROJECT_RULE_B.\n')
  same=markers(proc.turn(ident,'SAME PROCESS'));proc.close();proc=None
  proc=Rpc(2);proc.call('thread/resume',dict(params,threadId=ident,excludeTurns=True));resumed=markers(proc.turn(ident,'RESUMED NEW PROCESS'))
  fresh_id=proc.call('thread/start',params)['thread']['id'];fresh=markers(proc.turn(fresh_id,'NEW THREAD NEW PROCESS'))
  report={'binary':binary,'binarySha256':hashlib.sha256(Path(binary).read_bytes()).hexdigest(),'helperSha256':hashlib.sha256(module.read_bytes()).hexdigest(),'fixtureSha256':hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),'version':subprocess.check_output([binary,'--version'],env=env,text=True).strip(),'first':first,'sameProcessAfterEdit':same,'resumedNewProcess':resumed,'freshNewProcess':fresh,'providerRequests':len(captures),'realCodexProcesses':True,'authFilesReadByTest':False,'localProviderOnly':True}
  (out/'result.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2),flush=True)
  assert fresh['GLOBAL_RULE_B']['present'] and fresh['PROJECT_RULE_B']['present'],fresh
  assert resumed['GLOBAL_RULE_B']['present'] and resumed['PROJECT_RULE_B']['present'],resumed
  assert same['GLOBAL_RULE_B']['present'] and not same['PROJECT_RULE_B']['present'],same
 finally:
  if proc:proc.close()
  server.shutdown();server.server_close();thread.join(timeout=3)

if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--codex',required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();verify(a.codex,a.output.resolve())
