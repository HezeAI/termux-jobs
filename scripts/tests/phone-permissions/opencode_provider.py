"""Synthetic OpenAI-compatible replies for native OpenCode; no cloud route."""
import http.server,json,re,threading,uuid
from pathlib import Path
PATTERN=re.compile(r'\bAUDIT (SHELL|WRITE) (INSIDE|OUTSIDE) ([0-9]{1,3})\b')
class Provider:
 def __init__(self,root):
  self.root=Path(root);self.events=[];owner=self
  class Handler(http.server.BaseHTTPRequestHandler):
   def log_message(self,*args):pass
   def do_POST(self):
    try:
     n=int(self.headers.get('Content-Length',0));assert 0<n<=2*1024*1024
     q=json.loads(self.rfile.read(n));messages=q.get('messages',[]);matches=[]
     for m in messages:
      if m.get('role')!='user':continue
      content=m.get('content','');text=content if isinstance(content,str)else'\n'.join(p.get('text','')for p in content if isinstance(p,dict));matches.extend(PATTERN.finditer(text))
     match=matches[-1]if matches else None;followup=messages and messages[-1].get('role')=='tool'
     delta={'role':'assistant','content':'AUDIT READY. Use only the documented AUDIT commands.'};finish='stop';event={'bytes':n,'kind':'ready','keyIsSynthetic':self.headers.get('authorization')=='Bearer SYNTHETIC_PHONE_AUDIT_ONLY'}
     if followup:delta={'role':'assistant','content':'AUDIT FINISHED. Verify the marker file independently.'};event['kind']='tool-result'
     elif match:
      kind,scope,number=match.groups();target=owner.root/('project'if scope=='INSIDE'else'outside')/(kind.lower()+'.txt');value='AUDIT_'+kind+'_'+number;name='shell'if kind=='SHELL'else'write'
      args={'command':"printf '%s' '"+value+"' > '"+str(target)+"'",'workdir':str(owner.root/'project')}if name=='shell'else{'path':str(target),'content':value}
      assert name in [t.get('function',{}).get('name')for t in q.get('tools',[])]
      delta={'role':'assistant','tool_calls':[{'index':0,'id':'call_'+uuid.uuid4().hex,'type':'function','function':{'name':name,'arguments':json.dumps(args)}}]};finish='tool_calls';event.update(kind='tool-request',tool=name,scope=scope,number=int(number))
     owner.events.append(event);owner.events[:max(0,len(owner.events)-512)]=[]
     self.send_response(200);self.send_header('Content-Type','text/event-stream');self.end_headers();ident='chat_audit_'+uuid.uuid4().hex
     for d,f in [(delta,None),({},finish)]:self.wfile.write(('data: '+json.dumps({'id':ident,'object':'chat.completion.chunk','created':1,'model':q.get('model','audit'),'choices':[{'index':0,'delta':d,'finish_reason':f}]})+'\n\n').encode())
     self.wfile.write(b'data: [DONE]\n\n');self.wfile.flush()
    except (AssertionError,ValueError,TypeError,KeyError):
     self.send_response(400);self.send_header('Content-Type','application/json');self.end_headers();self.wfile.write(b'{"error":{"message":"Invalid synthetic audit request"}}')
  self.server=http.server.ThreadingHTTPServer(('127.0.0.1',0),Handler);self.thread=threading.Thread(target=self.server.serve_forever,daemon=True)
 @property
 def url(self):return 'http://127.0.0.1:'+str(self.server.server_port)
 def start(self):self.thread.start()
 def stop(self):self.server.shutdown();self.server.server_close();self.thread.join(timeout=2)
