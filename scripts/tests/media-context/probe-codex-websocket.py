"""Native WS continuation versus physical image resend; synthetic provider only."""
from pathlib import Path
base=Path(__file__).with_name('probe-codex.py').read_text();separator="try:\n start();thread=rpc('thread/start'";assert base.count(separator)==1
exec(compile(base.split(separator)[0],str(Path(__file__).with_name('probe-codex.py')),'exec'),globals())
response_images={}
class WebSocketHandler(Handler):
 protocol_version="HTTP/1.1"
 def do_GET(self):
  if self.headers.get('Upgrade','').lower()!='websocket':self.send_error(404);return
  accept=base64.b64encode(hashlib.sha1((self.headers['Sec-WebSocket-Key']+'258EAFA5-E914-47DA-95CA-C5AB0DC85B11').encode()).digest()).decode()
  self.send_response(101);self.send_header('Upgrade','websocket');self.send_header('Connection','Upgrade');self.send_header('Sec-WebSocket-Accept',accept);self.end_headers()
  # Deliberately do not negotiate compression; bytes below are uncompressed WS payloads.
  def exact(n):
   data=self.rfile.read(n)
   if len(data)!=n:raise EOFError()
   return data
  def frame(opcode,payload):
   length=len(payload);header=bytes([0x80|opcode])+ (bytes([length]) if length<126 else b'\x7e'+length.to_bytes(2,'big') if length<65536 else b'\x7f'+length.to_bytes(8,'big'))
   self.wfile.write(header+payload);self.wfile.flush()
  def emit(kind,**value):frame(1,json.dumps(dict(type=kind,**value),separators=(',',':')).encode())
  try:
   while True:
    first,second=exact(2);opcode=first&15;assert first&0x80 and not first&0x70
    length=second&127
    if length==126:length=int.from_bytes(exact(2),'big')
    elif length==127:length=int.from_bytes(exact(8),'big')
    assert length<160*1024*1024
    mask=exact(4) if second&128 else b'';raw=exact(length)
    if mask:raw=bytes(v^mask[i%4] for i,v in enumerate(raw))
    if opcode==8:frame(8,b'');return
    if opcode==9:frame(10,raw);continue
    assert opcode==1
    q=json.loads(raw);record=summarize(q,raw);previous=q.get('previous_response_id');logical=response_images.get(previous,[])+record['images']
    record.update(case=case['name'],path=self.path,at=time.monotonic(),transport='websocket',generate=q.get('generate'),logicalImageCount=len(logical),receivedWsPayloadBytes=len(raw))
    records.append(record);case['requests'].append(record)
    rid='response_'+uuid.uuid4().hex;itemid='item_'+uuid.uuid4().hex
    response_images[rid]=logical
    emit('response.created',response={'id':rid,'status':'in_progress'})
    if case.get('drop') and sum(r.get('generate') is not False for r in case['requests'])==1:
     self.connection.shutdown(socket.SHUT_RDWR);self.connection.close();return
    if q.get('generate') is False:
     emit('response.completed',response={'id':rid,'status':'completed','output':[],'usage':{'input_tokens':1000,'output_tokens':0,'total_tokens':1000}});continue
    plan=case.get('tool') or [];plan=plan if isinstance(plan,list) else [plan];step=int(case['issued']);tool=plan[step] if step<len(plan) else None
    if tool:
     name=tool['name'];case['issued']=step+1
     item={'id':itemid,'type':'function_call','status':'completed','call_id':'call_'+uuid.uuid4().hex,'name':name,'arguments':json.dumps(tool['args']),**({'namespace':tool_namespace[name]} if tool_namespace.get(name) else {})}
    else:item={'id':itemid,'type':'message','status':'completed','role':'assistant','content':[{'type':'output_text','text':'Synthetic local response only','annotations':[]}]}
    emit('response.output_item.added',output_index=0,item={**item,'status':'in_progress'})
    emit('response.output_item.done',output_index=0,item=item)
    emit('response.completed',response={'id':rid,'status':'completed','output':[item],'usage':{'input_tokens':1000,'output_tokens':10,'total_tokens':1010}})
  except (EOFError,BrokenPipeError,ConnectionResetError):return
server.RequestHandlerClass=WebSocketHandler
with (HOME/'config.toml').open('a') as f:f.write('supports_websockets=true\n')
try:
 start();thread=rpc('thread/start',{'cwd':str(PROJECT),'approvalPolicy':'never','sandbox':'danger-full-access'})['thread']['id']
 turn('short-control',[text('SYNTHETIC short control')])
 for i in range(0,20,4):turn('png-batch-'+str(i//4+1),[text('SYNTHETIC PNG batch')]+[image(f'png-600-{n}.png') for n in range(i,i+4)])
 turn('continue-20-png',[text('SYNTHETIC continue without new images')])
 stop();start();rpc('thread/resume',{'threadId':thread,'cwd':str(PROJECT),'approvalPolicy':'never','sandbox':'danger-full-access','excludeTurns':True});turn('cold-reopen-20-png',[text('SYNTHETIC reopen control')])
 turn('view-old-image',[text('SYNTHETIC view old image')],{'name':'view_image','args':{'path':str(PROJECT/'png-600-0.png')}})
 turn('continue-after-view',[text('SYNTHETIC continue without new images')])
 turn('disconnect-recover-after-images',[text('SYNTHETIC recover from deliberate provider disconnect')],drop=True)
 assert any(r['case']=='continue-20-png' and r.get('generate') is not False and r['imageCount']==0 and r['logicalImageCount']==20 for r in records), 'No incremental image-free continuation'
 assert any(r['case']=='cold-reopen-20-png' and r['imageCount']==20 for r in records), 'Cold reopen did not replay baseline'
 assert any(r['case']=='disconnect-recover-after-images' and r['imageCount']==21 for r in records), 'Reconnect did not replay baseline'
 assert records and all(r.get('transport')=='websocket' for r in records),'Native CLI fell back to HTTP'
finally:
 stop();server.shutdown();server.server_close();persist();(OUT/'latest-websocket-run.txt').write_text(str(RUN)+'\n')
 print(json.dumps({'run':str(RUN),'requests':len(records),'websocketRequests':sum(r.get('transport')=='websocket' for r in records),'httpRequests':sum(r.get('transport')!='websocket' for r in records)}),flush=True)
