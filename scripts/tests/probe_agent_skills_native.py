"""Read-only native discovery, isolated HOME/config; no auth or model request."""
import json,subprocess,tempfile,os,selectors,time,hashlib
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];base=Path(tempfile.mkdtemp(prefix='skills-native-',dir=ROOT/'.verification'))
home=base/'home';cfg=home/'.codex';project=base/'project'
for p in [cfg,project,project/'.git']:p.mkdir(parents=True,exist_ok=True)
def skill(folder,name,description):
 folder.mkdir(parents=True,exist_ok=True);(folder/'SKILL.md').write_text(f'---\nname: {name}\ndescription: {description}\n---\nHarmless native discovery marker.\n')
for folder,name in [(home/'.agents/skills/user-probe','user-probe'),(cfg/'skills/legacy-probe','legacy-probe'),(project/'.agents/skills/project-probe','project-probe')]:skill(folder,name,'Native '+name+' fixture')
exe='/usr/local/lib/node_modules/@openai/codex/node_modules/@openai/codex-linux-arm64/vendor/aarch64-unknown-linux-musl/bin/codex'
env={'PATH':os.environ['PATH'],'HOME':str(home),'CODEX_HOME':str(cfg),'TMPDIR':str(ROOT/'.tools/tmp')}
p=subprocess.Popen([exe,'--no-daemon','app-server'],cwd=project,env=env,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.DEVNULL,bufsize=0);sel=selectors.DefaultSelector();sel.register(p.stdout,selectors.EVENT_READ);buf=b'';serial=0
def rpc(method,params):
 global serial,buf
 serial+=1;i=serial;p.stdin.write((json.dumps({'id':i,'method':method,'params':params})+'\n').encode());p.stdin.flush();end=time.time()+20
 while time.time()<end:
  if b'\n' not in buf:
   if not sel.select(.1):continue
   chunk=os.read(p.stdout.fileno(),65536)
   if not chunk:raise RuntimeError('CLI exited')
   buf+=chunk
  while b'\n' in buf:
   line,buf=buf.split(b'\n',1);q=json.loads(line)
   if q.get('id')==i:return q
 raise TimeoutError(method)
try:
 rpc('initialize',{'clientInfo':{'name':'skills_fixture','version':'1'},'capabilities':{'experimentalApi':True}});p.stdin.write(b'{"method":"initialized"}\n');p.stdin.flush()
 skill(home/'.agents/skills/collision-probe','collision-probe','Personal collision fixture');skill(project/'.agents/skills/collision-probe','collision-probe','Project collision fixture')
 first=rpc('skills/list',{'cwds':[str(project)],'forceReload':True});skill(project/'.agents/skills/late-probe','late-probe','Native late fixture');late=rpc('skills/list',{'cwds':[str(project)],'forceReload':True})
 entries=first['result']['data'][0]['skills'];later=late['result']['data'][0]['skills']
 assert {'user-probe','legacy-probe','project-probe'} <= {x['name'] for x in entries}
 assert sum(x['name']=='collision-probe' for x in entries)==2
 assert any(x['name']=='late-probe' for x in later)
 result={'version':subprocess.check_output([exe,'--version'],env=env,text=True).strip(),'first':first,'late':late,'fixture':str(base),'modelRequests':0};(base/'result.json').write_text(json.dumps(result,indent=2));print(json.dumps({'version':result['version'],'fixture':str(base),'rootsConfirmed':True,'sameNameListedSeparately':True,'forceReloadConfirmed':True,'modelRequests':0}))
finally:
 p.stdin.close()
 try:p.wait(timeout=5)
 except subprocess.TimeoutExpired:p.terminate();p.wait(timeout=5)
 sel.close()
