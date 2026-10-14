#!/usr/bin/env python3
"""Optional isolated OpenCode v2.0.28 smoke check. No inference or real credentials.
Requires .tools/ai-agents/runtime/opencode and Node.js.
"""
import pathlib,subprocess,os,json,time
root=pathlib.Path(__file__).resolve().parents[1];d=root/'.tools/ai-agents/runtime'; env=dict(os.environ);env.update(PATH=str(d)+':'+env['PATH'])
# All agent databases/configuration/cache are under the project, no inference is submitted.
bridge=root/'app/src/main/assets/ai-agents-control.js';connection=d/'bridge-connection'
p=subprocess.run(['node',str(bridge),'request',str(connection)],input=b'{"op":"models"}',stdout=subprocess.PIPE,stderr=subprocess.PIPE,env=env,timeout=60)
value=json.loads(p.stdout);assert value['ok'],p.stderr.decode();assert value['models'];assert all(set(m)<= {'id','provider','name','variants','enabled'} for m in value['models']);assert b'apiKey' not in p.stdout
(connection/'probe-result.json').write_text(json.dumps({'models':len(value['models']),'exit':p.returncode,'secret_fields_removed':True}))
print('PASS isolated OpenCode model request; sanitized output; no inference')
import pty
project=d/'project';project.mkdir(exist_ok=True);sock=d/'test.sock';sid='sesProbe'+__import__('uuid').uuid4().hex
master,slave=pty.openpty()
p=subprocess.Popen(['node',str(bridge),'run',str(connection),str(sock),sid,'','build','new',''],stdin=slave,stdout=slave,stderr=slave,env=env,cwd=project)
os.close(slave)
try:
 for _ in range(400):
  if sock.exists() or p.poll() is not None:break
  time.sleep(.1)
 if not sock.exists():
  import select
  out=os.read(master,8192) if select.select([master],[],[],1)[0] else b''
  raise AssertionError('Run failed: '+out.decode(errors='replace'))
 r=subprocess.run(['node',str(bridge),'proxy',str(sock)],input=b'{"op":"status"}',stdout=subprocess.PIPE,stderr=subprocess.PIPE,env=env,timeout=40)
 result=json.loads(r.stdout);assert result['ok'] and result['id'].startswith('ses_'),result
 native_id=result['id']
 r=subprocess.run(['node',str(bridge),'proxy',str(sock)],input=b'{"op":"stop"}',stdout=subprocess.PIPE,stderr=subprocess.PIPE,env=env,timeout=40);assert json.loads(r.stdout)['stopped'];p.wait(timeout=10);assert not sock.exists()
 print('PASS real OpenCode create/status/owned stop; no inference')
 r=subprocess.run(['node',str(bridge),'request',str(connection)],input=json.dumps({'op':'chat','id':sid}).encode(),stdout=subprocess.PIPE,env=env,timeout=40);assert json.loads(r.stdout)['id']==native_id
 print('PASS closed-profile alias resolves to native ID for Android history save')
 # The same ID opens the stored session after the first server exits.
 os.close(master);master,slave=pty.openpty()
 p=subprocess.Popen(['node',str(bridge),'run',str(connection),str(sock),sid,'','build','resume',''],stdin=slave,stdout=slave,stderr=slave,env=env,cwd=project)
 os.close(slave)
 for _ in range(400):
  if sock.exists() or p.poll() is not None:break
  time.sleep(.1)
 assert sock.exists(),'Resume did not become ready'
 r=subprocess.run(['node',str(bridge),'proxy',str(sock)],input=b'{"op":"status"}',stdout=subprocess.PIPE,env=env,timeout=40);assert json.loads(r.stdout)['id']==native_id
 r=subprocess.run(['node',str(bridge),'proxy',str(sock)],input=b'{"op":"stop"}',stdout=subprocess.PIPE,env=env,timeout=40);assert json.loads(r.stdout)['stopped'];p.wait(timeout=10)
 print('PASS real OpenCode resume same ID after server restart')
finally:
 if p.poll() is None:p.terminate();p.wait(timeout=10)
 os.close(master)
