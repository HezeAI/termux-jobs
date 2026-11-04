"""Artificial Anthropic-compatible tool replies; local only, no forwarding."""
import http.server,json,re,threading,uuid
from pathlib import Path
PATTERN=re.compile(r'\bAUDIT (SHELL|WRITE) (INSIDE|OUTSIDE) ([0-9]{1,3})\b')
class Provider:
 def __init__(self,root):
  self.root=Path(root);self.events=[];owner=self
  class Handler(http.server.BaseHTTPRequestHandler):
   def log_message(self,*args):pass
   def error(self,status):
    data=b'{"error":{"type":"invalid_request_error","message":"Invalid synthetic audit request"}}';self.send_response(status);self.send_header('Content-Type','application/json');self.send_header('Content-Length',str(len(data)));self.end_headers();self.wfile.write(data)
   def do_POST(self):
    try:
     n=int(self.headers.get('Content-Length',0))
     if not 0<n<=2*1024*1024:return self.error(413)
     q=json.loads(self.rfile.read(n))
     if 'count_tokens'in self.path:
      data=b'{"input_tokens":1000}';self.send_response(200);self.send_header('Content-Type','application/json');self.send_header('Content-Length',str(len(data)));self.end_headers();return self.wfile.write(data)
     if not self.path.split('?')[0].endswith('/messages'):return self.error(404)
     user=[];uses={}
     for message in q.get('messages',[]):
      blocks=message.get('content',[]);blocks=[{'type':'text','text':blocks}]if isinstance(blocks,str)else blocks
      if message.get('role')=='user':user=blocks
      for part in blocks:
       if part.get('type')=='tool_use':uses[part.get('id')]=part.get('name')
     # Find current command across user messages; a tool result carries no new
     # prompt. A later explicit command takes priority over earlier results.
     match=None
     for message in q.get('messages',[]):
      if message.get('role')!='user':continue
      blocks=message.get('content',[]);blocks=[{'type':'text','text':blocks}]if isinstance(blocks,str)else blocks
      for part in blocks:
       matches=list(PATTERN.finditer(part.get('text','')))
       if matches:match=matches[-1]
     results=[p for p in user if p.get('type')=='tool_result'];newCommand=any(PATTERN.search(p.get('text','')) for p in user)
     previous=uses.get(results[-1].get('tool_use_id'))if results and not newCommand else None
     content={'type':'text','text':'AUDIT READY. Use only the documented AUDIT commands.'};event={'bytes':n,'kind':'ready','keyIsSynthetic':self.headers.get('x-api-key')=='SYNTHETIC_PHONE_AUDIT_ONLY'}
     if results and not newCommand and previous!='Read':content={'type':'text','text':'AUDIT FINISHED. Verify the marker file independently.'};event['kind']='tool-result'
     elif match:
      kind,scope,number=match.groups();target=owner.root/('project'if scope=='INSIDE'else'outside')/(kind.lower()+'.txt');value='AUDIT_'+kind+'_'+number
      name='Bash'if kind=='SHELL'else'Write'if previous=='Read'else'Read'
      args={'command':"printf '%s' '"+value+"' > '"+str(target)+"'"}if name=='Bash'else{'file_path':str(target),**({'content':value}if name=='Write'else{})}
      if name not in [t.get('name')for t in q.get('tools',[])]:return self.error(400)
      content={'type':'tool_use','id':'call_'+uuid.uuid4().hex,'name':name,'input':args};event.update(kind='tool-request',tool=name,scope=scope,number=int(number))
     owner.events.append(event);owner.events[:max(0,len(owner.events)-512)]=[]
     self.send_response(200);self.send_header('Content-Type','text/event-stream');self.end_headers()
     def emit(event,data):self.wfile.write(('event: '+event+'\ndata: '+json.dumps(data)+'\n\n').encode());self.wfile.flush()
     tool=content['type']=='tool_use'
     emit('message_start',{'type':'message_start','message':{'id':'msg_'+uuid.uuid4().hex,'type':'message','role':'assistant','model':q.get('model','claude-sonnet'),'content':[],'stop_reason':None,'stop_sequence':None,'usage':{'input_tokens':1000,'output_tokens':1}}})
     emit('content_block_start',{'type':'content_block_start','index':0,'content_block':{**content,'input':{}}if tool else{'type':'text','text':''}})
     emit('content_block_delta',{'type':'content_block_delta','index':0,'delta':{'type':'input_json_delta','partial_json':json.dumps(content['input'])}if tool else{'type':'text_delta','text':content['text']}})
     emit('content_block_stop',{'type':'content_block_stop','index':0});emit('message_delta',{'type':'message_delta','delta':{'stop_reason':'tool_use'if tool else'end_turn','stop_sequence':None},'usage':{'output_tokens':10}});emit('message_stop',{'type':'message_stop'})
    except (ValueError,TypeError,KeyError):self.error(400)
  self.server=http.server.ThreadingHTTPServer(('127.0.0.1',0),Handler);self.thread=threading.Thread(target=self.server.serve_forever,daemon=True)
 @property
 def url(self):return 'http://127.0.0.1:'+str(self.server.server_port)
 def start(self):self.thread.start()
 def stop(self):self.server.shutdown();self.server.server_close();self.thread.join(timeout=2)
