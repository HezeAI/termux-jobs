#!/usr/bin/env python3
"""Bounded X11 startup probe; property metadata only, no titles or input logging."""
import os,re,subprocess,sys,time,socket
from pathlib import Path
ID=re.compile(r'\b0x[0-9a-fA-F]+\b')
ENV=dict(os.environ,LC_ALL='C')
TRACE=None

class StartupTrace:
 # Only this launch's allowlisted processes. No argv, paths, credentials or input.
 ROLES={'xfce4-session','xfwm4','xfsettingsd','xfce4-panel','Thunar','thunar','xfdesktop','fcitx5','dbus-daemon'}
 def __init__(self,token,proc=Path('/proc'),clock=time.monotonic,emit=print):
  self.token=token.encode();self.proc=proc;self.clock=clock;self.emit=emit;self.last=-10;self.next_query=-10;self.seen={};self.queries={};self.bus_done=False;self.last_clients=-10;self.last_session=None
  self.hz=os.sysconf('SC_CLK_TCK')
 def out(self,line):self.emit(line,flush=True)
 def record(self,args,elapsed,failed):
  kind='session' if args[0]=='dbus-send' else 'map' if args[0]=='xwininfo' and '-stats' in args else 'tree' if args[0]=='xwininfo' else 'root' if '-root' in args else 'window'
  q=self.queries.setdefault(kind,[0,0,0,0]);ms=max(0,int(elapsed*1000));q[0]+=1;q[1]+=ms;q[2]=max(q[2],ms);q[3]+=int(failed)
 def session(self,value,deadline):
  match=re.search(r'^\s*uint32\s+([0-9]+)\s*$',value or '',re.M)
  state='unavailable' if match is None else 'startup' if match[1]=='0' else 'idle' if match[1]=='1' else 'other'
  now=self.clock()
  if now-self.last_clients<5 and state==self.last_session:return
  self.last_clients=now;self.last_session=state;clients='unknown'
  if match is not None:
   reply=query(['dbus-send','--session','--print-reply','--reply-timeout=1000','--dest=org.xfce.SessionManager','/org/xfce/SessionManager','org.xfce.Session.Manager.ListClients'],deadline)
   if reply is not None and 'array [' in reply:clients=str(min(9999,reply.count('object path')))
  self.out('JOBS_X11_SESSION state='+state+' clients='+clients)
 def summarize(self):
  for kind,q in self.queries.items():self.out('JOBS_X11_QUERY %s count=%d total_ms=%d max_ms=%d failed=%d'%(kind,*q))
 def bus(self):
  self.bus_done=True
  try:
   actual=int(re.search(r'^Uid:\s+(\d+)',(self.proc/'self/status').read_text(),re.M).group(1));guest=os.getuid()
   address=os.environ.get('DBUS_SESSION_BUS_ADDRESS','');match=re.fullmatch(r'unix:(path|abstract)=([^,;]+)(?:,guid=[a-fA-F0-9]+)?',address)
   results=[]
   for uid in [guest,actual]:
    result='unavailable'
    if match:
     sock=socket.socket(socket.AF_UNIX);sock.settimeout(.3)
     try:
      sock.connect(('\0' if match[1]=='abstract' else '')+match[2]);sock.sendall(b'\0AUTH EXTERNAL '+str(uid).encode().hex().encode()+b'\r\n');reply=sock.recv(256);result='ok' if reply.startswith(b'OK ') else 'rejected' if reply.startswith(b'REJECTED') else 'other'
     except OSError:pass
     finally:sock.close()
    results.append(result)
   self.out('JOBS_X11_BUS guest_uid=%d kernel_uid=%d guest_auth=%s kernel_auth=%s'%(guest,actual,*results))
  except (OSError,ValueError,AttributeError):self.out('JOBS_X11_STARTUP=bus-probe-unavailable')
 def sample(self):
  now=self.clock()
  if now-self.last<1:return
  self.last=now
  if not self.bus_done:self.bus()
  # PRoot may replace /proc/uptime with container-relative values. proc stat
  # starttime uses kernel boottime, so compare only to the same kernel clock.
  try:uptime=time.clock_gettime(time.CLOCK_BOOTTIME)
  except (OSError,AttributeError,ValueError):uptime=None
  try:entries=list(self.proc.iterdir())
  except OSError:return
  for entry in entries:
   if not entry.name.isdigit():continue
   try:
    role=(entry/'comm').read_text().strip()
    if role not in self.ROLES:continue
    # Refuse another environment/launch even when process names coincide.
    with (entry/'environ').open('rb') as f:env=f.read(65537)
    if len(env)>65536 or b'JOBS_X11_FOCUS_TOKEN='+self.token not in env.split(b'\0'):continue
    stat=(entry/'stat').read_text().rsplit(')',1)[1].split();pid=int(entry.name);state=stat[0]
    if state not in 'RSDTZtIXWP':state='X'
    ticks=int(stat[11])+int(stat[12]);age='unknown' if uptime is None or uptime<int(stat[19])/self.hz else str(int((uptime-int(stat[19])/self.hz)*1000))
    try:wchan=(entry/'wchan').read_text().strip()
    except OSError:wchan=''
    wait='futex' if 'futex' in wchan else 'poll' if 'poll' in wchan or 'epoll' in wchan else 'sleep' if 'sleep' in wchan else 'io' if any(x in wchan for x in ['io_','wait_on','read']) else 'other'
    previous=self.seen.get(pid)
    if previous is None and len(self.seen)>=32:continue
    if previous is None or now-previous[0]>=5:
     event='first' if previous is None else 'sample';cpu_ms=max(0,int(ticks*1000/self.hz));self.seen[pid]=(now,ticks)
     # Native and guest bus daemons are distinguished without exposing executable paths.
     flavor='unknown'
     try:
      exe=str((entry/'exe').readlink());flavor='proot' if '/libexec/proot/loader' in exe else 'termux' if exe.startswith('/data/data/com.termux/files/usr/') else 'guest'
     except OSError:pass
     sm=int(any(x.startswith(b'SESSION_MANAGER=') and len(x)>16 for x in env.split(b'\0')))
     self.out('JOBS_X11_PROC %s pid=%d event=%s state=%s age_ms=%s cpu_ms=%d wait=%s sm=%d binary=%s'%(role,pid,event,state,age,cpu_ms,wait,sm,flavor))
   except (OSError,ValueError,IndexError):continue
  if now>=self.next_query:self.next_query=now+5;self.summarize()
 def finish(self):self.sample();self.summarize()

def query(args,deadline=None):
 if deadline is not None and time.monotonic()>=deadline:return None
 began=time.monotonic();failed=True
 try:
  p=subprocess.run(args,env=ENV,stdout=subprocess.PIPE,stderr=subprocess.DEVNULL,text=True,timeout=2 if deadline is None else max(.05,min(2,deadline-time.monotonic())))
  failed=p.returncode!=0
  return p.stdout if not failed else None
 except (OSError,subprocess.TimeoutExpired):return None
 finally:
  if TRACE is not None:TRACE.record(args,time.monotonic()-began,failed)

def session_idle(deadline):
 # XFCE's own startup completion, not guesses from desktop/panel window types.
 # Current private dbus-run-session only; never query or activate another bus.
 value=query(['dbus-send','--session','--print-reply','--reply-timeout=1500',
              '--dest=org.xfce.SessionManager','/org/xfce/SessionManager',
              'org.xfce.Session.Manager.GetState'],deadline)
 if TRACE is not None:TRACE.session(value,deadline)
 return value is not None and re.search(r'^\s*uint32\s+1\s*$',value,re.M) is not None

def snapshot():
 deadline=time.monotonic()+3
 root=query(['xprop','-root','_NET_SUPPORTING_WM_CHECK','_NET_CLIENT_LIST'],deadline)
 if root is None:return False,False,False,'query-failed'
 wm_line=next((x for x in root.splitlines() if x.startswith('_NET_SUPPORTING_WM_CHECK(')),'')
 ids=ID.findall(wm_line);wm=False
 if ids and int(ids[0],16):
  check=query(['xprop','-id',ids[0],'_NET_SUPPORTING_WM_CHECK'],deadline)
  wm=check is not None and any(int(x,16)==int(ids[0],16) for x in ID.findall(check))
 if wm and session_idle(deadline):return True,False,False,'session-idle'
 clients=next((x for x in root.splitlines() if x.startswith('_NET_CLIENT_LIST(')),'')
 # Window manager's client list identifies actual clients even when reparented.
 # Window titles, localization and ordering are not readiness conditions.
 panel=desktop=False
 checked=set()
 def inspect(windows):
  nonlocal panel,desktop
  for wid in windows:
   if time.monotonic()>=deadline:return
   if not int(wid,16) or wid in checked:continue
   checked.add(wid)
   props=query(['xprop','-id',wid,'_NET_WM_WINDOW_TYPE'],deadline)
   if props is None:continue
   dock=bool(re.search(r'\b_NET_WM_WINDOW_TYPE_DOCK\b',props))
   background=bool(re.search(r'\b_NET_WM_WINDOW_TYPE_DESKTOP\b',props))
   if not dock and not background:continue
   stats=query(['xwininfo','-id',wid,'-stats'],deadline)
   if stats is None or not re.search(r'^\s*Map State:\s*IsViewable\s*$',stats,re.M):continue
   panel=panel or dock;desktop=desktop or background
   if panel and desktop:return
 inspect(list(dict.fromkeys(ID.findall(clients)))[:128])
 if not (panel and desktop) and time.monotonic()<deadline:
  # Override-redirect desktop/panel windows may be absent from WM client lists.
  # WM_CLASS hints select candidates; actual EWMH type and map state still decide.
  tree=query(['xwininfo','-root','-tree'],deadline)
  hints=[]
  for line in (tree or '').splitlines():
   klass=re.search(r'\("([^"\n]*)" "([^"\n]*)"\)',line)
   if klass and any(x.lower() in ('xfdesktop','xfce4-panel') for x in klass.groups()):
    ids=ID.findall(line)
    if ids:hints.append(ids[0])
  inspect(hints[:32])
 return wm,panel,desktop,'ok'

def wait(token,probe=snapshot,clock=time.monotonic,sleep=time.sleep,trace=None):
 start=clock();warned=False;seen=set()
 while clock()-start<180:
  wm,panel,desktop,reason=probe()
  if trace is not None:trace.sample()
  # Reuse this startup-only probe: never log titles, paths, input or bus identifiers.
  for stage,visible in [('wm-observed',wm),('panel-visible',panel),('desktop-visible',desktop),('session-idle',reason=='session-idle')]:
   if visible and stage not in seen:
    seen.add(stage);print('JOBS_X11_STARTUP='+stage,flush=True)
    if stage=='wm-observed':print('JOBS_X11_WINDOW_MANAGER=ready',flush=True)
  if wm and (reason=='session-idle' or panel and desktop):
   print('\nJOBS_X11_DESKTOP_SOURCE '+token+' '+reason,flush=True)
   if trace is not None:trace.finish()
   print('JOBS_X11_DESKTOP_READY '+token,flush=True);return True
  elapsed=clock()-start
  if elapsed>=20 and not warned:
   print('JOBS_X11_DESKTOP_WAIT '+token+' wm='+str(int(wm))+' panel='+str(int(panel))+' desktop='+str(int(desktop))+' result='+reason,flush=True)
   print('JOBS_X11_DESKTOP_TIMEOUT '+token,flush=True);warned=True
  sleep(2 if warned else .5)
 if not warned:print('JOBS_X11_DESKTOP_TIMEOUT '+token,flush=True)
 if trace is not None:trace.finish()
 return False

if __name__=='__main__':
 token=os.environ.get('JOBS_X11_FOCUS_TOKEN','')
 if not re.fullmatch(r'[a-f0-9-]{36}',token):raise SystemExit(2)
 TRACE=StartupTrace(token)
 wait(token,trace=TRACE)
