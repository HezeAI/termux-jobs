"""Artificial native Codex context/reopen probe. Run ONLY under local-network-only."""
import os,sys,json,time,threading,queue,subprocess,hashlib,base64,uuid,http.server,shutil,socket
from pathlib import Path
assert os.environ.get('TERMUX_MEDIA_GUARDED_RUN')=='1', 'Use the guarded media-context/run.sh runner'
ROOT=Path(__file__).resolve().parents[2];OUT=Path(__file__).resolve().parent
RUN=OUT/('codex-'+uuid.uuid4().hex[:8]);RUN.mkdir();HOME=RUN/'home';HOME.mkdir();PROJECT=RUN/'project';PROJECT.mkdir();(PROJECT/'.git').mkdir();(PROJECT/'AGENTS.md').write_text('Synthetic fixture project. Only its artificial files may be read.\n')
prompt_variant=os.environ.get('SYNTHETIC_PROMPT_VARIANT','');prompt_inputs={}
if prompt_variant:
 assert prompt_variant in ('none','project','ru','en')
 assets=Path(os.environ['TERMUX_MEDIA_ASSETS']).resolve()
 if prompt_variant in ('ru','en'):
  prompt_inputs['common']=(assets/('agent-instructions-ubuntu-ai'+('.en' if prompt_variant=='en' else '')+'.md')).read_text()
  (HOME/'AGENTS.md').write_text(prompt_inputs['common'])
 if prompt_variant!='none':
  prompt_inputs['project']=(assets/'agent-instructions-project.md').read_text()
  (PROJECT/'AGENTS.md').write_text(prompt_inputs['project'])
 else:(PROJECT/'AGENTS.md').unlink()
for p in (OUT/'data').iterdir():
 if p.is_file():shutil.copy2(p,PROJECT/p.name)
exe='/usr/local/lib/node_modules/@openai/codex/node_modules/@openai/codex-linux-arm64/vendor/aarch64-unknown-linux-musl/bin/codex'
assert hashlib.sha256(Path(exe).read_bytes()).hexdigest()=='f2e6c980c937e734644bed1b4a9a018d3e63a0ccbe71aafc85c86a8c6e4b462a'
records=[];tool_namespace={}
case={'name':'startup','requests':[],'tool':None,'issued':False,'drop':False};serial=0;process=None;inbox=queue.Queue();events=[]
def summarize(q,raw):
 images=[];strings=0
 def walk(v):
  nonlocal strings
  if isinstance(v,dict):
   if v.get('type')=='input_image':
    url=v.get('image_url','');images.append({'dataUrlBytes':len(url.encode()),'sha256':hashlib.sha256(url.encode()).hexdigest(),'detail':v.get('detail')})
   for x in v.values():walk(x)
  elif isinstance(v,list):
   for x in v:walk(x)
  elif isinstance(v,str):strings+=len(v.encode())
 walk(q.get('input',[]))
 prompt_evidence={}
 if prompt_variant:
  def strings_in(v):
   if isinstance(v,str):yield v
   elif isinstance(v,list):
    for x in v:yield from strings_in(x)
   elif isinstance(v,dict):
    for x in v.values():yield from strings_in(x)
  values=list(strings_in(q.get('input',[])))
  prompt_evidence={name:{'sha256':hashlib.sha256(value.encode()).hexdigest(),'bytes':len(value.encode()),'fullyPresent':any(value.strip() in value_in_request for value_in_request in values)} for name,value in prompt_inputs.items()}
 def toolnames(tools,namespace=None):
  for t in tools:
   if not isinstance(t,dict):continue
   if t.get('type')=='namespace':yield from toolnames(t.get('tools',[]),t.get('name'))
   elif t.get('name'):
    tool_namespace[t['name']]=namespace;yield t['name']
 return {'requiredMarkers':{marker:marker in json.dumps(q.get('input',[])) for marker in ('REQ_KEEP_ORIGINALS','REQ_NO_PUBLISH','REQ_VERIFY_RESULT')},'oldImagePathPresent':str(PROJECT/'png-600-0.png') in json.dumps(q.get('input',[])),'promptEvidence':prompt_evidence,'bodyBytes':len(raw),'inputJsonBytes':len(json.dumps(q.get('input',[]),ensure_ascii=False,separators=(',',':')).encode()),'inputStringBytes':strings,'images':images,'imageCount':len(images),'uniqueImageCount':len(set(i['sha256'] for i in images)),'inputTypes':sorted(set(i.get('type','message') for i in q.get('input',[]) if isinstance(i,dict))),'keys':list(q),'toolsType':type(q.get('tools')).__name__,'previousResponseId':q.get('previous_response_id'),'toolNames':list(toolnames((q.get('tools') or [])+[t for i in q.get('input',[]) if isinstance(i,dict) and i.get('type')=='additional_tools' for t in i.get('tools',[])]))}
class Handler(http.server.BaseHTTPRequestHandler):
 def log_message(self,*a):pass
 def do_POST(self):
  global case
  size=int(self.headers.get('content-length','0'));assert 0<size<160*1024*1024
  raw=self.rfile.read(size);q=json.loads(raw);(RUN/'tool-declarations.json').write_text(json.dumps([i for i in q.get('input',[]) if isinstance(i,dict) and i.get('type')=='additional_tools'],indent=2));record=summarize(q,raw);record.update(path=self.path,case=case['name'],at=time.monotonic());case['requests'].append(record);records.append(record)
  rid='response_'+uuid.uuid4().hex;itemid='item_'+uuid.uuid4().hex
  self.send_response(200);self.send_header('Content-Type','text/event-stream');self.end_headers()
  def emit(kind,**v):self.wfile.write(('event: '+kind+'\ndata: '+json.dumps(dict(type=kind,**v))+'\n\n').encode());self.wfile.flush()
  emit('response.created',response={'id':rid,'status':'in_progress'})
  if case.get('drop') and len(case['requests'])==1:
   self.connection.shutdown(socket.SHUT_RDWR);self.connection.close();return
  plan=case.get('tool') or [];plan=plan if isinstance(plan,list) else [plan];step=int(case['issued']);tool=plan[step] if step<len(plan) else None
  if tool:
   if tool.get('requiresPath'):
    assert tool['requiresPath'] in json.dumps(q.get('input',[])), 'Latest-file discovery output missing from native context'
   name=tool['name'];case['issued']=step+1
   item={'id':itemid,'type':'function_call','status':'completed','call_id':'call_'+uuid.uuid4().hex,'name':name,'arguments':json.dumps(tool['args']),**({'namespace':tool_namespace[name]} if tool_namespace.get(name) else {})}
  else:item={'id':itemid,'type':'message','status':'completed','role':'assistant','content':[{'type':'output_text','text':'Synthetic local response only','annotations':[]}]}
  emit('response.output_item.added',output_index=0,item={**item,'status':'in_progress'})
  emit('response.output_item.done',output_index=0,item=item)
  emit('response.completed',response={'id':rid,'status':'completed','output':[item],'usage':{'input_tokens':1000,'output_tokens':10,'total_tokens':1010}})
server=http.server.ThreadingHTTPServer(('127.0.0.1',0),Handler);threading.Thread(target=server.serve_forever,daemon=True).start()
(HOME/'config.toml').write_text(('features.unified_image_budget=true\n' if os.environ.get('SYNTHETIC_IMAGE_BUDGET')=='1' else '')+'model="gpt-6.1-sol"\nmodel_provider="fixture"\n[model_providers.fixture]\nname="Local artificial provider"\nbase_url="http://127.0.0.1:'+str(server.server_port)+'/v1"\nwire_api="responses"\nrequires_openai_auth=false\n')
def start():
 global process,inbox
 inbox=queue.Queue();env={'HOME':str(HOME),'CODEX_HOME':str(HOME),'PATH':os.environ['PATH'],'TMPDIR':str(RUN)}
 process=subprocess.Popen([exe,'--no-daemon','app-server'],cwd=PROJECT,env=env,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=open(RUN/('native-'+uuid.uuid4().hex[:8]+'.stderr'),'w'),text=True)
 def reader(p,q):
  for line in p.stdout:
   try:q.put(json.loads(line))
   except ValueError:q.put({'parseError':len(line)})
 threading.Thread(target=reader,args=(process,inbox),daemon=True).start()
 rpc('initialize',{'clientInfo':{'name':'synthetic_context_probe','version':'1'},'capabilities':{'experimentalApi':True}});process.stdin.write('{"method":"initialized"}\n');process.stdin.flush()
def rpc(method,params):
 global serial
 serial+=1;ident=serial;process.stdin.write(json.dumps({'id':ident,'method':method,'params':params})+'\n');process.stdin.flush();until=time.monotonic()+30
 while time.monotonic()<until:
  e=inbox.get(timeout=max(.1,until-time.monotonic()))
  if e.get('id')==ident:
   if 'error'in e:raise RuntimeError(json.dumps(e['error']))
   return e.get('result')
  events.append({'method':e.get('method'),'error':e.get('params',{}).get('error') if e.get('method')=='error' else None})
 raise TimeoutError(method)
def stop():
 if process is None:return
 process.stdin.close()
 try:process.wait(timeout=8)
 except subprocess.TimeoutExpired:
  process.terminate()
  try:process.wait(timeout=8)
  except subprocess.TimeoutExpired:process.kill();process.wait(timeout=8)
 assert process.poll() is not None

def history_bytes():return sum(p.stat().st_size for p in (HOME/'sessions').rglob('*') if p.is_file())
def turn(name,inputs,tool=None,drop=False):
 global case
 case={'name':name,'requests':[],'tool':tool,'issued':False,'drop':drop};beg=time.monotonic();r=rpc('turn/start',{'threadId':thread,'input':inputs});tid=r['turn']['id'];status='timeout';until=beg+90
 while time.monotonic()<until:
  try:e=inbox.get(timeout=max(.1,until-time.monotonic()))
  except queue.Empty:break
  if e.get('method')=='turn/completed' and e.get('params',{}).get('turn',{}).get('id')==tid:
   status=e['params']['turn']['status'];break
  if e.get('method')=='error':events.append({'method':'error','data':e.get('params',{}).get('error')})
 row={'case':name,'status':status,'elapsedSeconds':round(time.monotonic()-beg,3),'requestCount':len(case['requests']),'requestBytes':[v['bodyBytes'] for v in case['requests']],'imageCounts':[v['imageCount'] for v in case['requests']],'historyBytes':history_bytes(),'unsupportedTool':case.get('unsupportedTool'),'processExited':process.poll() is not None};results.append(row);persist();print(json.dumps(row),flush=True)
 if status!='completed':raise RuntimeError('Incomplete synthetic turn '+name)
def text(v):return {'type':'text','text':v,'text_elements':[]}
def image(file):return {'type':'localImage','path':str(PROJECT/file)}
results=[]
def persist():
 (RUN/'result.json').write_text(json.dumps({'scope':'Native Codex with artificial loopback provider. Serialization/replay only; not real provider success or Android UI.','binarySha256':hashlib.sha256(Path(exe).read_bytes()).hexdigest(),'promptVariant':prompt_variant,'thread':globals().get('thread'),'cases':results,'requests':records,'events':events},indent=2)+'\n')
try:
 start();thread=rpc('thread/start',{'cwd':str(PROJECT),'approvalPolicy':'never','sandbox':'danger-full-access'})['thread']['id']
 turn('short-control',[text('SYNTHETIC REQUIREMENTS: REQ_KEEP_ORIGINALS REQ_NO_PUBLISH REQ_VERIFY_RESULT. Original image: '+str(PROJECT/'png-600-0.png') if os.environ.get('SYNTHETIC_COMPACT_ONLY')=='1' else 'SYNTHETIC short control')])
 if prompt_variant:
  turn('two-images',[text('SYNTHETIC PNG batch'),image('png-600-0.png'),image('png-600-1.png')])
  turn('continue-no-view',[text('SYNTHETIC continue without new images')])
  stop();start();rpc('thread/resume',{'threadId':thread,'cwd':str(PROJECT),'approvalPolicy':'never','sandbox':'danger-full-access','excludeTurns':True})
  turn('reopen-no-view',[text('SYNTHETIC reopen control')])
  for i in range(2):turn('view-image-repeat-'+str(i),[text('SYNTHETIC use image tool once')],{'name':'view_image','args':{'path':str(PROJECT/'png-600-0.png')}})
  turn('continue-after-view-no-view',[text('SYNTHETIC continue without new images')])
  stop();start();rpc('thread/resume',{'threadId':thread,'cwd':str(PROJECT),'approvalPolicy':'never','sandbox':'danger-full-access','excludeTurns':True})
  turn('reopen-after-view-no-view',[text('SYNTHETIC reopen control')])
  assert all(v['fullyPresent'] for r in records for v in r['promptEvidence'].values()), 'A requested prompt was truncated or not discovered'
  raise SystemExit(0)
 if os.environ.get('SYNTHETIC_TOOLS_ONLY')=='1':
  turn('view-image-forced',[text('SYNTHETIC view image')],{'name':'view_image','args':{'path':str(PROJECT/'png-600-0.png')}});raise SystemExit(0)
 if os.environ.get('SYNTHETIC_LATEST_ONLY')=='1':
  model=rpc('model/list',{'limit':100,'includeHidden':False});(RUN/'model-capabilities.json').write_text(json.dumps([{'model':m.get('model'),'inputModalities':m.get('inputModalities')} for m in model.get('data',[]) if m.get('model')=='gpt-6.1-sol'],indent=2)+'\n')
  recent=PROJECT/'recent';recent.mkdir();old=recent/'old.png';new=recent/'latest.webp';shutil.copy2(PROJECT/'png-600-0.png',old);shutil.copy2(PROJECT/'webp-1280-0.webp',new);os.utime(old,(1000,1000));os.utime(new,(2000,2000))
  find="python3 -c \"from pathlib import Path; p=Path('"+str(recent)+"'); print(max((x for x in p.iterdir() if x.suffix.lower() in ['.png','.jpg','.webp']),key=lambda x:x.stat().st_mtime))\""
  turn('find-latest-image-then-view',[text('Find the latest image in '+str(recent)+' and open it.')],[{'name':'exec_command','args':{'cmd':find,'max_output_tokens':1000}},{'name':'view_image','args':{'path':str(new)},'requiresPath':str(new)}])
  ffmpeg='/data/data/com.termux/files/usr/bin/ffmpeg';video=recent/'latest.mp4';subprocess.run([ffmpeg,'-loglevel','error','-f','lavfi','-i','testsrc2=duration=1:size=160x120:rate=2','-c:v','mpeg4','-y',str(video)],check=True,env={'PATH':os.environ['PATH'],'TMPDIR':str(RUN)},timeout=20);os.utime(video,(3000,3000));frame=recent/'video-frame.png'
  findvideo="python3 -c \"from pathlib import Path; p=Path('"+str(recent)+"'); print(max((x for x in p.iterdir() if x.suffix.lower() in ['.mp4','.webm']),key=lambda x:x.stat().st_mtime))\""
  turn('find-latest-video-extract-then-view',[text('Find the latest video in '+str(recent)+' and inspect its frame.')],[{'name':'exec_command','args':{'cmd':findvideo,'max_output_tokens':1000}},{'name':'exec_command','args':{'cmd':ffmpeg+' -loglevel error -i '+str(video)+' -frames:v 1 -y '+str(frame),'max_output_tokens':1000},'requiresPath':str(video)},{'name':'view_image','args':{'path':str(frame)}}])
  assert results[-2]['imageCounts'][-1]>=1 and results[-2]['requestCount']>=3
  assert results[-1]['imageCounts'][-1]>results[-2]['imageCounts'][-1] and results[-1]['requestCount']>=4
  raise SystemExit(0)
 for i in range(0,20,4):turn('png-batch-'+str(i//4+1),[text('SYNTHETIC PNG batch')]+[image(f'png-600-{n}.png') for n in range(i,i+4)])
 turn('continue-20-png',[text('SYNTHETIC continue without new images')])
 stop();start();rpc('thread/resume',{'threadId':thread,'cwd':str(PROJECT),'approvalPolicy':'never','sandbox':'danger-full-access','excludeTurns':True});turn('cold-reopen-20-png',[text('SYNTHETIC reopen control')])
 if os.environ.get('SYNTHETIC_COMPACT_ONLY')=='1':
  before=rpc('thread/turns/list',{'threadId':thread,'limit':100,'itemsView':'full'})
  before_ids=[t['id'] for t in before['data']]
  original_files={str(p):(p.stat().st_size,hashlib.sha256(p.read_bytes()).hexdigest()) for p in (HOME/'sessions').rglob('*') if p.is_file()}
  case={'name':'native-compact','requests':[],'tool':None,'issued':False,'drop':False}
  rpc('thread/compact/start',{'threadId':thread});end=time.monotonic()+90;notifications=[];compacted=False
  while time.monotonic()<end:
   e=inbox.get(timeout=max(.1,end-time.monotonic()));method=e.get('method');p=e.get('params',{});notifications.append({'method':method,'itemType':p.get('item',{}).get('type')})
   if method=='item/completed' and p.get('item',{}).get('type')=='contextCompaction':compacted=True
   if method in ('thread/compacted','contextCompacted') or method=='turn/completed' and compacted:
    assert p.get('turn',{}).get('status','completed')=='completed';break
  else:raise TimeoutError('Native compaction')
  turn('after-native-compact',[text('SYNTHETIC continue without new images')])
  stop();start();rpc('thread/resume',{'threadId':thread,'cwd':str(PROJECT),'approvalPolicy':'never','sandbox':'danger-full-access','excludeTurns':True})
  turn('reopen-after-native-compact',[text('SYNTHETIC reopen control')])
  after=rpc('thread/turns/list',{'threadId':thread,'limit':100,'itemsView':'full'})
  after_ids=[t['id'] for t in after['data']]
  turn('review-old-image-after-compact',[text('SYNTHETIC view old original image')],{'name':'view_image','args':{'path':str(PROJECT/'png-600-0.png')},'requiresPath':str(PROJECT/'png-600-0.png')})
  for number in range(3):turn('repeat-old-image-after-compact-'+str(number),[text('SYNTHETIC review old original again')],{'name':'view_image','args':{'path':str(PROJECT/'png-600-0.png')}})
  turn('continue-after-four-reviews',[text('SYNTHETIC continue without new images')])
  stop();start();rpc('thread/resume',{'threadId':thread,'cwd':str(PROJECT),'approvalPolicy':'never','sandbox':'danger-full-access','excludeTurns':True})
  turn('reopen-after-four-reviews',[text('SYNTHETIC reopen control')])
  after_by_id={t['id']:t for t in after['data']}
  assert all(after_by_id[t['id']]==t for t in before['data']), 'Old API turn contents changed'
  assert all(hashlib.sha256(Path(file).read_bytes()[:length]).hexdigest()==digest for file,(length,digest) in original_files.items()), 'Native journal prefix changed'
  assert all(all(r['requiredMarkers'].values()) for r in records), 'Task requirements disappeared from a request'
  assert [r for r in records if r['case']=='reopen-after-four-reviews'][0]['imageCount']==4
  stop()
  (RUN/'compaction-history.json').write_text(json.dumps({'beforeTurnIds':before_ids,'afterTurnIds':after_ids,'allBeforeTurnsStillAvailable':set(before_ids)<=set(after_ids),'allBeforeTurnContentsUnchanged':True,'beforeTurnsSha256':hashlib.sha256(json.dumps(before['data'],sort_keys=True).encode()).hexdigest(),'afterOriginalTurnsSha256':hashlib.sha256(json.dumps([after_by_id[t['id']] for t in before['data']],sort_keys=True).encode()).hexdigest(),'originalJournalPrefixes':{str(Path(file).relative_to(HOME)):{'bytes':length,'beforeSha256':digest,'afterPrefixSha256':hashlib.sha256(Path(file).read_bytes()[:length]).hexdigest(),'afterFileBytes':Path(file).stat().st_size} for file,(length,digest) in original_files.items()},'nativeJournalPrefixesUnchanged':True,'requirementsPresentInEveryRequest':True,'beforeImageItems':sum(sum(v.get('type')=='localImage' for v in i.get('content',[])) for t in before['data'] for i in t.get('items',[]) if i.get('type')=='userMessage'),'notifications':notifications},indent=2)+'\n')
  raise SystemExit(0)
 turn('jpeg-large',[text('SYNTHETIC large JPEG'),image('jpg-2048-0.jpg')]);turn('webp-large',[text('SYNTHETIC large WebP'),image('webp-1280-0.webp')])
 for i in range(0,24,4):turn('small-png-'+str(i//4+1),[text('SYNTHETIC small PNG')]+[image(f'png-32-{n}.png') for n in range(i,i+4)])
 for i in range(3):turn('view-image-repeat-'+str(i),[text('SYNTHETIC use image tool once')],{'name':'view_image','args':{'path':str(PROJECT/'png-600-0.png')}})
 turn('mixed-large-text',[text('SYNTHETIC supplied code sample\n'+(PROJECT/'large.py').read_text()[:60000])])
 turn('command-large-log',[text('SYNTHETIC read artificial log')],{'name':'exec_command','args':{'cmd':'cat '+str(PROJECT/'large.log'),'max_output_tokens':8000}})
 turn('disconnect-recover',[text('SYNTHETIC recover from deliberate local stream disconnect')],drop=True)
 stop();start();rpc('thread/resume',{'threadId':thread,'cwd':str(PROJECT),'approvalPolicy':'never','sandbox':'danger-full-access','excludeTurns':True});turn('reopen-mixed',[text('SYNTHETIC reopen mixed chat')])
finally:
 stop();server.shutdown();server.server_close();persist();(OUT/'latest-codex-run.txt').write_text(str(RUN)+'\n')
