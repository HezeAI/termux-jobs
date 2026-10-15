#!/usr/bin/env python3
"""App-owned, per-user cron/systemd schedules. No sudo, OS package installs, or foreign units."""
import os,sys,json,pathlib,subprocess,shutil,re,fcntl,uuid,shlex,datetime,time
ROOT=pathlib.Path.home()/'.local/share/termux-jobs-schedules-v1'
ID=re.compile(r'^[a-f0-9]{32}$')
PREFIX='termux-jobs-schedule-'
RUNNER=r'''import os,sys,json,pathlib,fcntl,subprocess,time,signal,threading
root=pathlib.Path(__file__).resolve().parent
job=root/sys.argv[1]
if len(sys.argv[1])!=32 or any(c not in '0123456789abcdef' for c in sys.argv[1]):sys.exit(2)
def save(value):
 p=job/'state.json';t=job/('.state-'+str(os.getpid()));t.write_text(json.dumps(value));os.chmod(t,0o600);os.replace(t,p)
try:
 admission=open(root/'lock','a+');fcntl.flock(admission,fcntl.LOCK_SH)
 if not job.is_dir() or job.is_symlink():sys.exit(0)
 lock=open(job/'run.lock','a+')
 try:fcntl.flock(lock,fcntl.LOCK_EX|fcntl.LOCK_NB)
 except BlockingIOError:sys.exit(0)
 config=json.loads((job/'config.json').read_text())
 if not config.get('enabled'):sys.exit(0)
 fcntl.flock(admission,fcntl.LOCK_UN);admission.close()
except (OSError,ValueError):sys.exit(1)
os.umask(0o077)
stop=threading.Event()
signal.signal(signal.SIGTERM,lambda *a:stop.set());signal.signal(signal.SIGINT,lambda *a:stop.set())
state={'state':'running','started':time.time()};save(state)
try:
 fd=os.open(job/'output.log',os.O_WRONLY|os.O_CREAT|os.O_NOFOLLOW,0o600)
 if os.fstat(fd).st_nlink!=1:os.close(fd);raise RuntimeError('LOG_LINK')
 os.ftruncate(fd,0)
 with os.fdopen(fd,'wb') as log:
  child=subprocess.Popen([config['bash'],str(job/'command.sh')],cwd=config['directory'],stdin=subprocess.DEVNULL,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,start_new_session=True)
  def drain():
   size=0
   while True:
    data=child.stdout.read(65536)
    if not data:break
    if size<2097152:log.write(data[:2097152-size]);log.flush()
    size+=len(data)
  reader=threading.Thread(target=drain,daemon=True);reader.start();deadline=time.monotonic()+3600
  while True:
   try:code=child.wait(timeout=.2);break
   except subprocess.TimeoutExpired:
    if not stop.is_set() and time.monotonic()<deadline:continue
    # Leader remains unreaped during TERM/KILL: never signal a reused stored PID.
    try:os.killpg(child.pid,signal.SIGTERM)
    except ProcessLookupError:pass
    time.sleep(1)
    try:os.killpg(child.pid,signal.SIGKILL)
    except ProcessLookupError:pass
    code=child.wait();break
  reader.join(timeout=2)
  state.update(state='done' if code==0 else 'failed',exitCode=code,finished=time.time());save(state)
except Exception:
 state.update(state='failed',finished=time.time());save(state)
sys.exit(0 if state.get('state')=='done' else 1)
'''
def fail(code):raise ValueError(code)
def run(args,data=None,allowed=(0,),timeout=10):
 r=subprocess.run(args,input=data,stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=timeout,env=dict(os.environ,LC_ALL='C'))
 if r.returncode not in allowed:fail('SCHEDULER_COMMAND_FAILED')
 if len(r.stdout)>1048576:fail('RESPONSE_LIMIT')
 return r
def caps():
 cron=bool(shutil.which('crontab'));systemd=False;linger=False
 if shutil.which('systemctl'):
  try:systemd=run(['systemctl','--user','show-environment'],allowed=(0,1)).returncode==0
  except Exception:pass
 if systemd and shutil.which('loginctl'):
  try:linger=run(['loginctl','show-user',str(os.getuid()),'-p','Linger','--value'],allowed=(0,1)).stdout.strip()==b'yes'
  except Exception:pass
 cron_timezone=''
 if cron:
  try:
   for line in cron_read().decode('utf-8','replace').splitlines():
    match=re.fullmatch(r'\s*CRON_TZ\s*=\s*(.*?)\s*',line)
    if match:cron_timezone=match[1].strip('\"\'')[:100]
  except Exception:pass
 return {'cronTimezone':cron_timezone,'cron':cron,'systemd':systemd,'linger':linger,'timezone':str(datetime.datetime.now().astimezone().tzinfo),'time':datetime.datetime.now().astimezone().isoformat(timespec='seconds')}
def directory(path):
 # Never follow pre-existing symlink parents to install our own executable/configs.
 for p in reversed([path]+list(path.parents)[:-1]):
  if p.is_symlink():fail('SYMLINK_DIRECTORY')
  if not p.exists():p.mkdir(mode=0o700)
  if not p.is_dir():fail('NOT_DIRECTORY')
 if path.stat().st_uid!=os.getuid():fail('DIRECTORY_OWNER')
 if path.stat().st_mode&0o022:fail('DIRECTORY_PERMISSIONS')
def atomic(path,value):
 tmp=path.parent/('.tmp-'+uuid.uuid4().hex)
 try:
  fd=os.open(tmp,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
  with os.fdopen(fd,'w',encoding='utf-8') as f:f.write(json.dumps(value));f.flush();os.fsync(f.fileno())
  os.replace(tmp,path)
 finally:
  if tmp.exists():tmp.unlink()
def text_file(path,value):
 fd=os.open(path,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
 with os.fdopen(fd,'w',encoding='utf-8') as f:f.write(value)
def config(id):
 if not ID.fullmatch(id or ''):fail('INVALID_ID')
 job=ROOT/id
 if job.is_symlink() or not job.is_dir():fail('SCHEDULE_NOT_FOUND')
 p=job/'config.json'
 if p.is_symlink() or p.stat().st_uid!=os.getuid() or p.stat().st_nlink!=1:fail('SCHEDULE_OWNER')
 c=json.loads(p.read_text())
 if c.get('id')!=id or c.get('backend') not in ('cron','systemd'):fail('SCHEDULE_CORRUPT')
 if c['backend']=='cron':cron_plan(c.get('plan',''))
 else:calendar(c.get('plan',''))
 return c
def cron_plan(value):
 fields=value.split();bounds=[(0,59),(0,23),(1,31),(1,12),(0,7)]
 if len(fields)!=5:fail('INVALID_CRON')
 for field,(low,high) in zip(fields,bounds):
  for part in field.split(','):
   if not re.fullmatch(r'(\*|\d+(?:-\d+)?)(?:/\d+)?',part):fail('INVALID_CRON')
   base,*step=part.split('/')
   if step and not 1<=int(step[0])<=high-low+1:fail('INVALID_CRON')
   if base!='*':
    values=[int(x) for x in base.split('-')]
    if any(x<low or x>high for x in values) or values!=sorted(values):fail('INVALID_CRON')
 return ' '.join(fields)
def calendar(value):
 if value in ('hourly','daily','weekly'):return value
 if not re.fullmatch(r'(?:\*-\*-\*|\d{4}-\d{2}-\d{2}) (?:[01]\d|2[0-3]):[0-5]\d(?::[0-5]\d)?',value):fail('INVALID_CALENDAR')
 if not value.startswith('*'):datetime.date.fromisoformat(value.split()[0])
 return value
def cron_read():
 r=run(['crontab','-l'],allowed=(0,1))
 if r.returncode:
  if b'no crontab for' not in r.stderr.lower():fail('CRONTAB_UNAVAILABLE')
  return b''
 return r.stdout
def cron_change(id,line):
 marker=('# '+PREFIX+id).encode();before=cron_read()
 for _ in range(3):
  rows=before.splitlines(keepends=True);after=b''.join(row for row in rows if not row.rstrip(b'\r\n').endswith(marker))
  if line:
   if after and not after.endswith(b'\n'):after+=b'\n'
   after+=line.encode()+b'\n'
  current=cron_read()
  if current!=before:before=current;continue
  run(['crontab','-'],after);return
 fail('CRONTAB_CHANGED')
def unit_dir():return pathlib.Path.home()/'.config/systemd/user'
def unitarg(value):return '"'+value.replace('\\','\\\\').replace('"','\\"').replace('%','%%').replace('$','$$')+'"'
def cron_line(id,plan):
 command=shlex.quote(sys.executable)+' -I '+shlex.quote(str(ROOT/'runner.py'))+' '+id
 if any(c in command for c in '\r\n%'):fail('UNSUPPORTED_HOME_PATH')
 return plan+' '+command+' # '+PREFIX+id
def units(id,plan):
 unit=PREFIX+id;folder=unit_dir();directory(folder)
 service='[Unit]\nDescription=Termux Jobs scheduled task\n[Service]\nType=oneshot\nExecStart='+unitarg(sys.executable)+' -I '+unitarg(str(ROOT/'runner.py'))+' '+id+'\nTimeoutStartSec=3700\nKillMode=control-group\n'
 timer='[Unit]\nDescription=Termux Jobs schedule\n[Timer]\nOnCalendar='+plan+'\nPersistent=true\nUnit='+unit+'.service\n[Install]\nWantedBy=timers.target\n'
 text_file(folder/(unit+'.service'),service);text_file(folder/(unit+'.timer'),timer);run(['systemctl','--user','daemon-reload'])
def enabled(c,value):
 if c['backend']=='cron':cron_change(c['id'],cron_line(c['id'],c['plan']) if value else None)
 else:run(['systemctl','--user','enable' if value else 'disable','--now',PREFIX+c['id']+'.timer'])
def summary(c):
 state={};p=ROOT/c['id']/'state.json'
 if p.exists() and not p.is_symlink():
  try:state=json.loads(p.read_text())
  except Exception:pass
 if state.get('state')=='running':
  try:
   with open(ROOT/c['id']/'run.lock','a+') as current:
    try:fcntl.flock(current,fcntl.LOCK_EX|fcntl.LOCK_NB);state=dict(state,state='interrupted')
    except BlockingIOError:pass
  except OSError:state=dict(state,state='unknown')
 return dict(c,last=state)
def rpc(req):
 op=req.get('op')
 if op=='capabilities':return caps()
 if op=='list' and not ROOT.exists():return {'schedules':[]}
 directory(ROOT);os.umask(0o077)
 with open(ROOT/'lock','a+') as lock:
  fcntl.flock(lock,fcntl.LOCK_EX)
  if op=='list':return {'schedules':[summary(config(p.name)) for p in sorted(ROOT.iterdir()) if ID.fullmatch(p.name) and p.is_dir()][:30]}
  if op=='create':
   available=caps();backend=req.get('backend');plan=req.get('plan','');title=req.get('title','');command=req.get('command','');cwd=req.get('directory','~')
   if backend not in ('cron','systemd') or not available.get(backend):fail('SCHEDULER_UNAVAILABLE')
   plan=cron_plan(plan) if backend=='cron' else calendar(plan)
   if not isinstance(title,str) or len(title)>160 or not isinstance(command,str) or not command.strip() or len(command)>8192 or '\0' in command:fail('INVALID_COMMAND')
   cwd=pathlib.Path(cwd).expanduser().resolve()
   if not cwd.is_dir():fail('NOT_DIRECTORY')
   if len([p for p in ROOT.iterdir() if ID.fullmatch(p.name)])>=30:fail('SCHEDULE_LIMIT')
   runner=ROOT/'runner.py'
   if runner.exists():
    if runner.is_symlink() or runner.read_text()!=RUNNER:fail('RUNNER_CONFLICT')
   else:text_file(runner,RUNNER)
   id=uuid.uuid4().hex;job=ROOT/id;job.mkdir(mode=0o700);c={'id':id,'title':title or command[:60],'command':command,'directory':str(cwd),'backend':backend,'plan':plan,'enabled':False,'created':time.time(),'timezone':available['timezone'],'bash':shutil.which('bash') or '/bin/bash'}
   atomic(job/'config.json',c);text_file(job/'command.sh',command+'\n')
   try:
    if backend=='systemd':units(id,plan)
    enabled(c,True);c['enabled']=True;atomic(job/'config.json',c)
   except Exception:
    # Leave the disabled record for inspection/removal; runner checks enabled before launch.
    c['enabled']=False;c['setupError']=True;atomic(job/'config.json',c)
    try:enabled(c,False)
    except Exception:pass
    fail('SCHEDULE_SETUP_FAILED')
   return summary(c)
  c=config(req.get('id'));job=ROOT/c['id']
  if op=='log':
   p=job/'output.log';text=''
   if p.exists() and not p.is_symlink():
    with open(p,'rb') as f:f.seek(max(0,p.stat().st_size-32768));text=f.read(32768).decode('utf-8','replace')
   return {'schedule':summary(c),'log':text}
  if op in ('enable','disable'):
   value=op=='enable';c['enabled']=False;atomic(job/'config.json',c)
   enabled(c,value);c['enabled']=value;atomic(job/'config.json',c);return summary(c)
  if op=='edit':
   plan=cron_plan(req.get('plan','')) if c['backend']=='cron' else calendar(req.get('plan',''))
   was_enabled=c['enabled'];c['enabled']=False;atomic(job/'config.json',c);enabled(c,False)
   if c['backend']=='systemd':
    timer=unit_dir()/(PREFIX+c['id']+'.timer')
    if timer.is_symlink() or not timer.is_file():fail('UNIT_CHANGED')
    content=timer.read_text();old='OnCalendar='+c['plan']+'\n'
    if content.count(old)!=1:fail('UNIT_CHANGED')
    temporary=timer.parent/('.tmp-'+uuid.uuid4().hex);text_file(temporary,content.replace(old,'OnCalendar='+plan+'\n'));os.replace(temporary,timer)
    run(['systemctl','--user','daemon-reload'])
   c['plan']=plan;atomic(job/'config.json',c)
   if was_enabled:enabled(c,True);c['enabled']=True;atomic(job/'config.json',c)
   return summary(c)
  if op=='delete':
   with open(job/'run.lock','a+') as running:
    try:fcntl.flock(running,fcntl.LOCK_EX|fcntl.LOCK_NB)
    except BlockingIOError:fail('SCHEDULE_RUNNING')
    c['enabled']=False;atomic(job/'config.json',c);enabled(c,False)
    if c['backend']=='systemd':
     for suffix in ('.timer','.service'):
      p=unit_dir()/(PREFIX+c['id']+suffix)
      if p.is_symlink():fail('SYMLINK_UNIT')
      if p.exists():p.unlink()
     run(['systemctl','--user','daemon-reload'])
    shutil.rmtree(job);return {'deleted':True}
  fail('UNKNOWN_OPERATION')
if __name__=='__main__':
 try:
  data=sys.stdin.buffer.read(65537)
  if len(data)>65536:fail('REQUEST_LIMIT')
  result=rpc(json.loads(data));print(json.dumps({'ok':True,'result':result}))
 except Exception as e:
  code=str(e) if isinstance(e,ValueError) and re.fullmatch('[A-Z_]{1,80}',str(e)) else 'SCHEDULE_ERROR'
  print(json.dumps({'ok':False,'error':code}));sys.exit(1)
