"""Deterministic local tool provider. No model, account, audio or prompt logs."""
import http.server,json,re,threading
from pathlib import Path
PATTERN=re.compile(r'\bAUDIT (SHELL|WRITE) (INSIDE|OUTSIDE) ([0-9]{1,3})\b')
class Provider:
 def __init__(self,root):
  self.root=Path(root);self.events=[];owner=self
  class Handler(http.server.BaseHTTPRequestHandler):
   def log_message(self,*args):pass
   def do_GET(self):self.answer({'name':'models/gemini-2.5-flash','inputTokenLimit':1000000,'outputTokenLimit':8192,'supportedGenerationMethods':['generateContent','streamGenerateContent']})
   def answer(self,data,stream=False,status=200):
    payload=json.dumps(data).encode();self.send_response(status);self.send_header('Content-Type','text/event-stream' if stream else 'application/json');payload=b'data: '+payload+b'\n\n' if stream else payload;self.send_header('Content-Length',str(len(payload)));self.end_headers();self.wfile.write(payload)
   def do_POST(self):
    try:
     n=int(self.headers.get('Content-Length',0))
     if not 0<n<=2*1024*1024:return self.answer({'error':{'code':413,'message':'Fixture request too large'}},status=413)
     q=json.loads(self.rfile.read(n))
     if 'countTokens' in self.path:return self.answer({'totalTokens':256})
     parts=[]
     for item in reversed(q.get('contents',[])):
      if item.get('role')=='user':parts=item.get('parts',[]);break
     command='\n'.join(p.get('text','') for p in parts if isinstance(p,dict)).strip()
     matches=list(PATTERN.finditer(command));match=matches[-1] if matches else None;followup=any('functionResponse'in p for p in parts)
     part={'text':'AUDIT READY. Use only the documented AUDIT commands.'}
     event={'bytes':n,'kind':'ready','keyIsSynthetic':self.headers.get('x-goog-api-key')=='SYNTHETIC_PHONE_AUDIT_ONLY'}
     if followup:part={'text':'AUDIT FINISHED. Verify the marker file independently.'};event['kind']='tool-result'
     elif match:
      kind,scope,number=match.groups();target=owner.root/('project' if scope=='INSIDE' else 'outside')/(kind.lower()+'.txt');value='AUDIT_'+kind+'_'+number
      name='run_shell_command'if kind=='SHELL'else'write_file';args={'command':"printf '%s' '"+value+"' > '"+str(target)+"'",'dir_path':str(owner.root/'project')}if kind=='SHELL'else{'file_path':str(target),'content':value}
      declarations=[d.get('name')for t in q.get('tools',[])for d in t.get('functionDeclarations',[])]
      if name not in declarations:raise ValueError('Required fixture tool unavailable')
      part={'functionCall':{'name':name,'args':args}};event.update(kind='tool-request',tool=name,scope=scope,number=int(number))
     owner.events.append(event);owner.events[:max(0,len(owner.events)-512)]=[]
     self.answer({'candidates':[{'content':{'role':'model','parts':[part]},'finishReason':'STOP','index':0}],'usageMetadata':{'promptTokenCount':1000,'candidatesTokenCount':10,'totalTokenCount':1010},'modelVersion':'gemini-2.5-flash','responseId':'audit_'+str(len(owner.events))},'streamGenerateContent'in self.path)
    except (ValueError,TypeError,KeyError,json.JSONDecodeError):self.answer({'error':{'code':400,'message':'Invalid synthetic audit request'}},status=400)
  self.server=http.server.ThreadingHTTPServer(('127.0.0.1',0),Handler);self.thread=threading.Thread(target=self.server.serve_forever,daemon=True)
 @property
 def url(self):return 'http://127.0.0.1:'+str(self.server.server_port)
 def start(self):self.thread.start()
 def stop(self):self.server.shutdown();self.server.server_close();self.thread.join(timeout=2)
