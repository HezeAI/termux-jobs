#!/usr/bin/env python3
"""Owned X11 keyboard gate. Only Fcitx focus/capability and window metadata.
No key, surrounding-text, title, clipboard or user-document subscriptions.
"""
import ctypes as c
import os,re,selectors,subprocess,time
I=c.c_int;U=c.c_ulong;P=c.c_void_p
INTERFACE='org.fcitx.Fcitx.InputContext1'
MEMBERS=('FocusIn','FocusOut','DestroyIC','SetCapability')
DISABLED=(1<<40)|(1<<15)  # Fcitx Disable / NoOnScreenKeyboard
IDENTITY=re.compile(r'^:[0-9]{1,12}\.[0-9]{1,12}$')
PATH=re.compile(r'^/[A-Za-z0-9_/]{1,240}$')
HEADER=re.compile(r'^method call .* sender=(:[0-9.]+) -> .* path=(/[^; ]+); interface=org\.fcitx\.Fcitx\.InputContext1; member=(FocusIn|FocusOut|DestroyIC|SetCapability)$')

class BusLines:
 def __init__(self,callback):self.callback=callback;self.pending=None
 def line(self,line):
  if len(line)>1024:self.pending=None;return
  found=HEADER.fullmatch(line)
  if found:
   sender,path,member=found.groups();self.pending=None
   if not IDENTITY.fullmatch(sender) or not PATH.fullmatch(path):return
   if member=='SetCapability':self.pending=(sender,path)
   else:self.callback(sender,path,member,None)
  elif self.pending:
   match=re.fullmatch(r'\s+uint64 ([0-9]{1,20})',line);pending=self.pending;self.pending=None
   if match and int(match[1])<1<<64:self.callback(*pending,'SetCapability',int(match[1]))

class Gate:
 def __init__(self,window,pid,emit,clock=time.monotonic):
  self.window=window;self.pid=pid;self.emit=emit;self.clock=clock
  self.contexts={};self.current=None;self.bound=0;self.visible=None;self.pending=0
 def output(self,context):
  if self.visible==context:return
  if self.visible:self.emit(*self.visible,'FocusOut')
  self.visible=context
  if context:self.emit(*context,'FocusIn')
 def refresh(self):
  active,owner,editable=self.window()
  if self.bound and (active!=self.bound or not editable):
   self.current=None;self.bound=0;self.pending=0
  if not editable:self.output(None);return
  if self.pending and self.clock()<self.pending:return
  key=self.current;cap=self.contexts.get(key)
  belongs=bool(key and owner>0 and owner==self.pid(key[0]))
  if belongs:self.bound=active
  good=belongs and cap is not None and not cap&DISABLED
  self.output(key if good else None)
 def event(self,sender,path,member,capability):
  key=(sender,path)
  if member=='SetCapability':
   if key not in self.contexts and len(self.contexts)>=256:return
   self.contexts[key]=capability
   if self.current==key:
    if capability&DISABLED:self.output(None)
    else:self.refresh()
  elif member=='FocusIn':
   self.current=key;self.bound=0;self.pending=self.clock()+.20
   # Settle the WM activation and toolkit's capability change together.
  else:
   if member=='DestroyIC':self.contexts.pop(key,None)
   if self.current==key:self.current=None;self.bound=0;self.pending=0;self.output(None)
 def changed(self):
  # Keep a just-arrived FocusIn across WM activation ordering, but invalidate a
  # previously bound context when its window is no longer active/viewable.
  self.refresh()
 def due(self):
  if self.pending and self.clock()>=self.pending:
   self.pending=0;self.refresh()
   if not self.bound:self.current=None

class PropertyEvent(c.Structure):
 _fields_=[('type',I),('serial',U),('send_event',I),('display',P),('window',U),('atom',U),('time',U),('state',I)]
class Event(c.Union):
 _fields_=[('type',I),('property',PropertyEvent),('pad',c.c_long*24)]
class X11:
 def __init__(self):
  self.lib=None
  for name in ('libX11.so.6','libX11.so'):
   try:self.lib=c.CDLL(name);break
   except OSError:pass
  if self.lib is None:raise OSError('X11 library')
  x=self.lib
  signatures={'XOpenDisplay':(P,[c.c_char_p]),'XDefaultRootWindow':(U,[P]),'XConnectionNumber':(I,[P]),'XInternAtom':(U,[P,c.c_char_p,I]),
   'XGetWindowProperty':(I,[P,U,U,c.c_long,c.c_long,I,U,c.POINTER(U),c.POINTER(I),c.POINTER(U),c.POINTER(U),c.POINTER(P)]),
   'XSelectInput':(I,[P,U,c.c_long]),'XFlush':(I,[P]),'XPending':(I,[P]),'XNextEvent':(I,[P,c.POINTER(Event)]),'XFree':(I,[P]),'XCloseDisplay':(I,[P])}
  for name,(result,args) in signatures.items():getattr(x,name).restype=result;getattr(x,name).argtypes=args
  # A client may disappear between reading its ID and properties; ignore BadWindow.
  self.error=c.CFUNCTYPE(I,P,P)(lambda d,e:0);x.XSetErrorHandler.argtypes=[type(self.error)];x.XSetErrorHandler(self.error)
  self.d=x.XOpenDisplay(None)
  if not self.d:raise OSError('X11 display')
  self.root=x.XDefaultRootWindow(self.d);self.active=0
  names=('_NET_ACTIVE_WINDOW','_NET_WM_PID','_NET_WM_WINDOW_TYPE','_NET_WM_STATE','_NET_WM_STATE_HIDDEN','WM_STATE','_NET_WM_WINDOW_TYPE_NORMAL','_NET_WM_WINDOW_TYPE_DIALOG','_NET_WM_WINDOW_TYPE_UTILITY')
  self.atoms={name:x.XInternAtom(self.d,name.encode(),0) for name in names}
  x.XSelectInput(self.d,self.root,1<<22);x.XFlush(self.d)
 def prop(self,window,name):
  typ=U();fmt=I();count=U();after=U();data=P()
  result=self.lib.XGetWindowProperty(self.d,window,self.atoms[name],0,32,0,0,c.byref(typ),c.byref(fmt),c.byref(count),c.byref(after),c.byref(data))
  try:return list(c.cast(data,c.POINTER(U))[:min(count.value,32)]) if result==0 and fmt.value==32 and data.value else []
  finally:
   if data.value:self.lib.XFree(data)
 def window(self):
  ids=self.prop(self.root,'_NET_ACTIVE_WINDOW');active=ids[0] if ids else 0
  if active!=self.active:
   if self.active:self.lib.XSelectInput(self.d,self.active,0)
   self.active=active
   if active:self.lib.XSelectInput(self.d,active,(1<<22)|(1<<17))
   self.lib.XFlush(self.d)
  if not active:return 0,0,False
  types=self.prop(active,'_NET_WM_WINDOW_TYPE');state=self.prop(active,'WM_STATE');hidden=self.prop(active,'_NET_WM_STATE');pid=self.prop(active,'_NET_WM_PID')
  allowed={self.atoms['_NET_WM_WINDOW_TYPE_'+v] for v in ('NORMAL','DIALOG','UTILITY')}
  return active,pid[0] if pid else 0,bool(state and state[0]==1 and self.atoms['_NET_WM_STATE_HIDDEN'] not in hidden and (not types or all(t in allowed for t in types)))
 def drain(self):
  changed=False;event=Event()
  while self.lib.XPending(self.d):
   self.lib.XNextEvent(self.d,c.byref(event))
   if event.type in (17,18,19):changed=True
   elif event.type==28:
    p=event.property
    if p.window==self.root and p.atom==self.atoms['_NET_ACTIVE_WINDOW']:changed=True
    elif p.window==self.active and p.atom in {self.atoms[n] for n in ('_NET_WM_STATE','WM_STATE','_NET_WM_WINDOW_TYPE','_NET_WM_PID')}:changed=True
  return changed
 def close(self):self.lib.XCloseDisplay(self.d)

class Owners:
 def __init__(self):self.cache={}
 def __call__(self,sender):
  if sender in self.cache:return self.cache[sender]
  if len(self.cache)>=256:return 0
  try:
   result=subprocess.run(['dbus-send','--session','--print-reply','--reply-timeout=700','--dest=org.freedesktop.DBus','/org/freedesktop/DBus','org.freedesktop.DBus.GetConnectionUnixProcessID','string:'+sender],stdout=subprocess.PIPE,stderr=subprocess.DEVNULL,timeout=1,text=True)
   match=re.search(r'^\s*uint32 ([0-9]{1,10})\s*$',result.stdout,re.M) if result.returncode==0 else None
   pid=int(match[1]) if match else 0
  except (OSError,subprocess.TimeoutExpired):pid=0
  # Failed lookups can recover on the next focus event.
  if pid:self.cache[sender]=pid
  return pid

def run():
 token=os.environ.get('JOBS_X11_FOCUS_TOKEN','')
 if not re.fullmatch(r'[a-f0-9-]{1,80}',token):raise ValueError('focus token')
 x=X11();monitor=None;gate=None;selector=selectors.DefaultSelector()
 try:
  matches=["type='method_call',interface='%s',member='%s'"%(INTERFACE,m) for m in MEMBERS]
  monitor=subprocess.Popen(['dbus-monitor','--session',*matches],stdout=subprocess.PIPE,stderr=subprocess.DEVNULL)
  def emit(sender,path,member):print('JOBS_X11_FOCUS_V1',token,member,sender,path,flush=True)
  gate=Gate(x.window,Owners(),emit);parser=BusLines(gate.event)
  selector.register(monitor.stdout,selectors.EVENT_READ,'bus');selector.register(x.lib.XConnectionNumber(x.d),selectors.EVENT_READ,'x11')
  buffer=b'';overflow=False
  while True:
   # Xlib synchronous queries may already have drained the fd into its queue.
   if x.drain():gate.changed()
   gate.due();timeout=max(0,gate.pending-time.monotonic()) if gate.pending else None
   for key,_ in selector.select(timeout):
    if key.data=='x11':
     if x.drain():gate.changed()
    else:
     data=os.read(monitor.stdout.fileno(),4096)
     if not data:gate.output(None);return
     for char in data:
      if char==10:
       if not overflow:parser.line(buffer.decode('ascii',errors='replace'))
       buffer=b'';overflow=False
      elif not overflow:
       if len(buffer)>=1024:buffer=b'';overflow=True
       else:buffer+=bytes((char,))
 finally:
  if gate is not None:gate.output(None)
  selector.close()
  if monitor is not None:
   monitor.terminate()
   try:monitor.wait(timeout=1)
   except subprocess.TimeoutExpired:monitor.kill();monitor.wait()
   monitor.stdout.close()
  x.close()

if __name__=='__main__':
 try:run()
 except (OSError,ValueError):print('JOBS_X11_IME=auto-focus-unavailable',flush=True)
