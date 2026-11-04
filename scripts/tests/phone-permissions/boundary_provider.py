"""Bounded scripted provider. Only artificial outside files; no cloud or prompt logs."""
import http.server,json,re,threading,uuid,os
from pathlib import Path
PATTERN=re.compile(r'\bAUDIT (READ|WRITE|SHELL_READ|SHELL_WRITE) OUTSIDE ([0-9]{1,2})\b')
class Provider:
 def __init__(self,root,agent):
  self.root=Path(root);self.agent=agent;self.events=[];self.issued=set();self.markers=json.loads((self.root/'boundary-manifest.json').read_text())['markers'];self.seen=set();owner=self
  class Handler(http.server.BaseHTTPRequestHandler):
   def log_message(self,*args):pass
   def do_GET(self):self.reply({'name':'models/gemini-2.5-flash','inputTokenLimit':1000000,'outputTokenLimit':8192,'supportedGenerationMethods':['generateContent','streamGenerateContent']})
   def reply(self,data,status=200):
    b=json.dumps(data).encode();self.send_response(status);self.send_header('Content-Type','application/json');self.send_header('Content-Length',str(len(b)));self.end_headers();self.wfile.write(b)
   def emit(self,event,data):self.wfile.write((('event: '+event+'\n'if event else'')+'data: '+json.dumps(data)+'\n\n').encode());self.wfile.flush()
   def do_POST(self):
    try:
     size=int(self.headers.get('Content-Length',0));assert 0<size<=2*1024*1024;raw=self.rfile.read(size);q=json.loads(raw)
     if 'countTokens'in self.path:return self.reply({'totalTokens':1000})
     if 'count_tokens'in self.path:return self.reply({'input_tokens':1000})
     for name,marker in owner.markers.items():
      if marker.encode()in raw:owner.seen.add(name)
     history=q.get('contents',[])if agent=='gemini'else q.get('input',[])if agent=='codex'else q.get('messages',[]);matches=[]
     for m in history:
      if not isinstance(m,dict)or m.get('role')!='user':continue
      content=m.get('parts',m.get('content',[]));content=[{'text':content}]if isinstance(content,str)else content
      for part in content:
       if isinstance(part,dict):matches.extend(PATTERN.finditer(part.get('text','')))
     # Auxiliary title requests do not execute tools or consume a test operation.
     if agent=='opencode'and not q.get('tools'):matches=[]
     match=matches[-1]if matches else None;action=None;result='BOUNDARY READY';event={'bytes':size,'seenReadMarkers':sorted(owner.seen),'kind':'ready'}
     # Distinct numbered test operations prevent old allowed read history from
     # being mistaken for a newly denied read. No raw payload is retained.
     key=(len(matches),match.group(0))if match else None
     if match and key not in owner.issued:
      op,n=match.groups();number=int(n);assert 1<=number<=99;owner.issued.add(key)
      target=owner.root/'outside'/('read-'+n+'.txt'if op in ['READ','SHELL_READ']else'new-'+n+'.txt');value='AUDIT_WRITE_'+n
      if op=='READ'and agent=='codex':
       names=[]
       def tools(v):
        if isinstance(v,list):
         for x in v:tools(x)
        elif isinstance(v,dict):
         if v.get('type')in ['function','custom']and isinstance(v.get('name'),str):names.append(v['name'])
         for k in ['tools','function']:tools(v.get(k,[]))
       tools(q.get('tools',[]))
       for entry in q.get('input',[]):
        if isinstance(entry,dict)and entry.get('type')=='additional_tools':tools(entry.get('tools',[]))
       result='DIRECT_READ_TOOL_UNAVAILABLE'if names and not set(names)&{'read','read_file','read_text_file'}else'DIRECT_READ_NEEDS_REVIEW';event.update(kind='unavailable'if result.endswith('UNAVAILABLE')else'unverified',operation=op,number=number,declaredNativeTools=sorted(set(names))[:100])
      else:
       if op=='READ':name={'claude':'Read','gemini':'read_file','opencode':'read'}[agent];args={('file_path'if agent!='opencode'else'path'):str(target)}
       elif op=='WRITE':
        name={'claude':'Write','gemini':'write_file','opencode':'write','codex':'apply_patch'}[agent];args={('file_path'if agent in ['claude','gemini']else'path'):str(target),'content':value}
       else:
        command=("cat -- '"+str(target)+"'")if op=='SHELL_READ'else("printf '%s' '"+value+"' > '"+str(target)+"'")
        name={'claude':'Bash','gemini':'run_shell_command','opencode':'shell','codex':'exec_command'}[agent];args={('command'if agent!='codex'else'cmd'):command}
        if agent=='opencode':args['workdir']=str(owner.root/'project')
        if agent=='gemini':args['dir_path']=str(owner.root/'project')
        if agent=='codex':args.update(yield_time_ms=1000,max_output_tokens=1000)
       action=(name,args);event.update(kind='tool-request',operation=op,number=number,tool=name)
     elif match:result='BOUNDARY FINISHED';event.update(kind='tool-result',operation=match[1],number=int(match[2]))
     owner.events.append(event);owner.events[:max(0,len(owner.events)-128)]=[];owner.persist()
     self.send_response(200);self.send_header('Content-Type','text/event-stream');self.end_headers();ident=uuid.uuid4().hex
     if agent=='gemini':
      part={'functionCall':{'name':action[0],'args':action[1]}}if action else{'text':result};self.emit('',{'candidates':[{'content':{'role':'model','parts':[part]},'finishReason':'STOP','index':0}],'usageMetadata':{'promptTokenCount':1000,'candidatesTokenCount':10,'totalTokenCount':1010}})
     elif agent=='claude':
      block={'type':'tool_use','id':'call_'+ident,'name':action[0],'input':{}}if action else{'type':'text','text':''}
      self.emit('message_start',{'type':'message_start','message':{'id':'msg_'+ident,'type':'message','role':'assistant','model':q.get('model'),'content':[],'stop_reason':None,'usage':{'input_tokens':1000,'output_tokens':1}}});self.emit('content_block_start',{'type':'content_block_start','index':0,'content_block':block});self.emit('content_block_delta',{'type':'content_block_delta','index':0,'delta':{'type':'input_json_delta','partial_json':json.dumps(action[1])}if action else{'type':'text_delta','text':result}});self.emit('content_block_stop',{'type':'content_block_stop','index':0});self.emit('message_delta',{'type':'message_delta','delta':{'stop_reason':'tool_use'if action else'end_turn','stop_sequence':None},'usage':{'output_tokens':10}});self.emit('message_stop',{'type':'message_stop'})
     elif agent=='opencode':
      delta={'role':'assistant','tool_calls':[{'index':0,'id':'call_'+ident,'type':'function','function':{'name':action[0],'arguments':json.dumps(action[1])}}]}if action else{'role':'assistant','content':result}
      for d,finish in [(delta,None),({},'tool_calls'if action else'stop')]:self.emit('',{'id':'chat_'+ident,'object':'chat.completion.chunk','created':1,'model':q.get('model'),'choices':[{'index':0,'delta':d,'finish_reason':finish}]})
      self.wfile.write(b'data: [DONE]\n\n')
     else:
      item={'id':'item_'+ident,'type':'message','status':'completed','role':'assistant','content':[{'type':'output_text','text':result,'annotations':[]}]}
      if action:
       if action[0]=='apply_patch':item={'id':'item_'+ident,'type':'custom_tool_call','status':'completed','call_id':'call_'+ident,'name':'apply_patch','input':'*** Begin Patch\n*** Add File: '+action[1]['path']+'\n+'+action[1]['content']+'\n*** End Patch'}
       else:item={'id':'item_'+ident,'type':'function_call','status':'completed','call_id':'call_'+ident,'name':action[0],'arguments':json.dumps(action[1])}
      self.emit('response.created',{'type':'response.created','response':{'id':'response_'+ident,'status':'in_progress'}});self.emit('response.output_item.added',{'type':'response.output_item.added','output_index':0,'item':item});self.emit('response.output_item.done',{'type':'response.output_item.done','output_index':0,'item':item});self.emit('response.completed',{'type':'response.completed','response':{'id':'response_'+ident,'status':'completed','output':[item],'usage':{'input_tokens':1000,'output_tokens':10,'total_tokens':1010}}})
    except (AssertionError,ValueError,TypeError,KeyError):self.reply({'error':{'message':'Invalid bounded synthetic request'}},400)
  self.server=http.server.ThreadingHTTPServer(('127.0.0.1',0),Handler);self.thread=threading.Thread(target=self.server.serve_forever,daemon=True)
 @property
 def url(self):return 'http://127.0.0.1:'+str(self.server.server_port)
 def persist(self):
  target=self.root/'boundary-observations.json';tmp=target.with_suffix('.'+uuid.uuid4().hex+'.tmp');tmp.write_text(json.dumps({'source':'local fixture HTTP request inspection','seenReadMarkers':sorted(self.seen),'events':self.events},indent=2)+'\n');os.replace(tmp,target)
 def start(self):self.thread.start()
 def stop(self):self.server.shutdown();self.server.server_close();self.thread.join(timeout=2);self.persist()
