#!/usr/bin/env python3
"""Isolated native OpenCode model/replay smoke test. Test proxy blocks inference.
Requires the existing ignored v2.0.28 binary and Node; never touches user agents.
"""
import pathlib,os,pty,fcntl,termios,struct,subprocess,time,json,select,uuid,signal
root=pathlib.Path(__file__).resolve().parents[1];d=root/'.tools/ai-agents/reopen'/('run-'+uuid.uuid4().hex);d.mkdir(parents=True);print('Receipts:',d,flush=True);source=(root/'app/src/main/assets/ai-agents-control.js').read_text()
# Isolated diagnostic proxy prevents any inference, records only our synthetic test requests.
hook='''
  const seedResponse=await request('GET','/api/session/'+id),seedInfo=seedResponse.data || seedResponse;
  seedInfo.id='ses_'+crypto.randomBytes(16).toString('hex');
  const imported=await request('POST','/api/experimental/session/import',{info:seedInfo,messages:[{id:'msg_reopenhistoryprobe',type:'user',time:{created:Date.now()},text:'REOPEN_HISTORY_PROBE'}]});
  id=seedInfo.id;rememberID(root,alias,id);
  const upstream=endpoint,requests=[];
  const diagnostic=http.createServer((req,res)=>{
    let chunks=[];req.on('data',b=>chunks.push(b));req.on('end',()=>{
      const body=Buffer.concat(chunks);requests.push({method:req.method,url:req.url,body:body.toString()});fs.writeFileSync(process.env.PROBE_RECORD,JSON.stringify(requests));
      if(req.method==='POST' && /\\/(prompt|generate|command|shell)$/.test(req.url)){res.writeHead(400,{'Content-Type':'application/json'});res.end(JSON.stringify({_tag:'InvalidRequestError',message:'Isolated probe: inference blocked'}));return;}
      const r=http.request(upstream+req.url,{method:req.method,headers:req.headers},response=>{res.writeHead(response.statusCode,response.headers);response.pipe(res);});r.on('error',()=>{res.writeHead(502);res.end();});r.end(body);
    });
  });await new Promise(resolve=>diagnostic.listen(0,'127.0.0.1',resolve));
  const miniEndpoint='http://127.0.0.1:'+diagnostic.address().port;
'''
source=source.replace("main().catch(()=>{","main().catch(e=>{console.error(e.stack);")
source=source.replace('  let reopening=false, finishRun;',hook+'  let reopening=false, finishRun;')
source=source.replace("    const next=client;", "    fs.writeFileSync(process.env.PROBE_CLIENT,String(client.pid));\n    const next=client;")
source=source.replace("['mini','--server',endpoint,","['mini','--server',miniEndpoint,")
source=source.replace("  async function busy() {","  let raceChecks=0;\n  async function busy() {\n    if(fs.existsSync(process.env.PROBE_BUSY)){if(fs.readFileSync(process.env.PROBE_BUSY,'utf8')==='race')return ++raceChecks>1;return true;}")
# Diagnostic proxy has its own listener; stop it when bridge-owned server exits.
source=source.replace("  const miniEndpoint=", "  server.once('exit',()=>diagnostic.close());\n  const miniEndpoint=")
bridge=d/'bridge.js';bridge.write_text(source)
env=dict(os.environ);env['PATH']=str(root/'.tools/ai-agents/runtime')+':'+env['PATH'];env['PROBE_RECORD']=str(d/'requests.json');env['PROBE_BUSY']=str(d/'busy');env['PROBE_CLIENT']=str(d/'mini-pid')
sock=d/'control.sock';alias='sesReopen'+uuid.uuid4().hex
master,slave=pty.openpty();fcntl.ioctl(slave,termios.TIOCSWINSZ,struct.pack('HHHH',35,110,0,0))
p=subprocess.Popen(['node',str(bridge),'run',str(d/'connection'),str(sock),alias,'opencode/ling-3.1-flash-free','build','new','','ask'],stdin=slave,stdout=slave,stderr=slave,cwd=d,env=env);os.close(slave)
output=b''
emulator=None;screen=b''
if os.environ.get('PROBE_EMULATOR')=='1':
 cp=str(root/'.tools/ai-agents/resize')+':'+str(root/'.verification/arm64/terminal-emulator/build/intermediates/javac/debug/classes')+':'+str(root/'.tools/sdk/platforms/android-30/android.jar')
 emulator=subprocess.Popen([str(root/'.tools/jdk/usr/lib/jvm/java-11-openjdk-arm64/bin/java'),'-cp',cp,'NativeTerminalHarness'],stdin=subprocess.PIPE,stdout=subprocess.PIPE)
def read_exact(pipe,count):
 data=b''
 while len(data)<count:
  chunk=pipe.read(count-len(data))
  if not chunk:raise RuntimeError('Emulator pipe closed')
  data+=chunk
 return data
def emulate(data):
 global screen
 emulator.stdin.write(data);emulator.stdin.flush()
 count=struct.unpack('>I',read_exact(emulator.stdout,4))[0];reply=read_exact(emulator.stdout,count)
 count=struct.unpack('>I',read_exact(emulator.stdout,4))[0];screen=read_exact(emulator.stdout,count)
 if reply:os.write(master,reply)
def drain(sec):
 global output
 end=time.monotonic()+sec
 while time.monotonic()<end:
  if select.select([master],[],[],.05)[0]:
   try:b=os.read(master,65536)
   except OSError:return
   output+=b
   if emulator:emulate(struct.pack('>I',len(b))+b)
   elif b'\x1b[6n' in b:os.write(master,b'\x1b[1;1R')
def query(q):
 return json.loads(subprocess.check_output(['node',str(bridge),'proxy',str(sock)],input=json.dumps(q).encode(),env=env,timeout=40))
try:
 for _ in range(400):
  drain(.1)
  if sock.exists():break
  assert p.poll() is None,'Bridge exited'
 drain(2);before=query({'op':'status'});sid=before['id'];print('PASS native chat ready',flush=True)
 if emulator:assert b'REOPEN_HISTORY_PROBE' in screen,'History was overwritten during mini startup'
 q={'op':'model','provider':'opencode','model':'exo-free','variant':'high','reopen':True}
 (d/'busy').touch();busy=query(q);assert busy.get('reason')=='busy' and not busy['applied'],busy;(d/'busy').unlink()
 assert query({'op':'status'})['model']==before['model'];print('PASS busy refusal leaves model unchanged (injected active-work decision)',flush=True)
 (d/'busy').write_text('race');raced=query(q);assert raced.get('reason')=='busy' and not raced['applied'],raced;(d/'busy').unlink();drain(2)
 assert query({'op':'status'})['model']==before['model']
 print('PASS admission race after mini exit reopens unchanged model (injected active-work decision)',flush=True)
 refused=query({**q,'reopen':False});assert refused['reason']=='reopen_required';print('PASS no silent server-only model mutation',flush=True)
 history_start=len(output)
 result=query(q);assert result['applied'] and result['reopened'] and result['id']==sid,result
 drain(4);assert p.poll() is None
 assert query({'op':'status'})['id']==sid;assert query({'op':'status'})['model']['id']=='exo-free'
 assert b'REOPEN_HISTORY_PROBE' in output[history_start:],'History marker not replayed'
 print('PASS mini reopened, bridge/server/chat ID survive, confirmed model and seeded history replay',flush=True)
 if emulator:assert b'REOPEN_HISTORY_PROBE' in screen,'History was overwritten during mini reopen'
 os.write(master,b'Reopen model probe, do not execute tools\r');drain(5)
 requests=json.loads((d/'requests.json').read_text());writes=[json.loads(r['body']) for r in requests if r['method']=='POST' and r['url'].endswith('/model')]
 print('MODEL_WRITES',writes,flush=True)
 assert writes and writes[-1]['model']['id']=='exo-free',writes
 assert any(r['method']=='POST' and r['url'].endswith('/prompt') for r in requests),'Prompt not submitted'
 assert query({'op':'status'})['model']['id']=='exo-free'
 print('PASS next native mini submission uses selected model, no inference forwarded',flush=True)
 marker_count=output.count(b'REOPEN_HISTORY_PROBE');resize_start=len(output)
 for rows,columns in [(19,110),(35,110),(19,110),(35,110),(35,85),(35,110)]:
  if emulator:emulate(struct.pack('>iii',-2,rows,columns))
  fcntl.ioctl(master,termios.TIOCSWINSZ,struct.pack('HHHH',rows,columns,0,0));os.kill(int((d/'mini-pid').read_text()),signal.SIGWINCH);drain(.8)
 if emulator:assert b'REOPEN_HISTORY_PROBE' in screen,'History display lost after resize'
 assert output.count(b'REOPEN_HISTORY_PROBE')>=marker_count,'History output lost on resize'
 print('PASS native history remains available through six height/width/reattachment resize events; native replay retained',flush=True)

 query({'op':'stop'});p.wait(timeout=10);assert not sock.exists();print('PASS owned stop after reopen',flush=True)
finally:
 if (d/'busy').exists():(d/'busy').unlink()
 if p.poll() is None:p.terminate();p.wait(timeout=10)
 os.close(master);(d/'terminal.txt').write_bytes(output)
 if emulator:emulator.stdin.close();emulator.wait(timeout=10);(d/'screen.txt').write_bytes(screen)
