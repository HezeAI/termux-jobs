#!/usr/bin/env python3
"""Bounded read-only X11 metadata recorder. Never reads titles, keys or pixels."""
import collections
import ctypes as C
import ctypes.util
import select
import time
import os
import re
import sys
import subprocess
import pty
from datetime import datetime, timezone
from pathlib import Path

class Attr(C.Structure):
    _fields_ = [(n,t) for n,t in [
        ('x',C.c_int),('y',C.c_int),('width',C.c_int),('height',C.c_int),
        ('border_width',C.c_int),('depth',C.c_int),('visual',C.c_void_p),
        ('root',C.c_ulong),('class_',C.c_int),('bit_gravity',C.c_int),
        ('win_gravity',C.c_int),('backing_store',C.c_int),('backing_planes',C.c_ulong),
        ('backing_pixel',C.c_ulong),('save_under',C.c_int),('colormap',C.c_ulong),
        ('map_installed',C.c_int),('map_state',C.c_int),('all_event_masks',C.c_long),
        ('your_event_mask',C.c_long),('do_not_propagate_mask',C.c_long),
        ('override_redirect',C.c_int),('screen',C.c_void_p)]]

class Event(C.Union):
    _fields_ = [('type',C.c_int),('words',C.c_ulong*24)]

class Recorder:
    """Keep only counters/window IDs. A stable ID remap is not window recreation."""
    def __init__(self):
        self.counts=collections.Counter();self.transitions=collections.deque(maxlen=8)
        self.buttons=None;self.pointer_child=None;self.pointer_position=None;self.focus=None;self.popups={};self.seen=set();self.destroyed=set()
    def event(self,kind,window,category=""):
        name={16:'create',17:'destroy',18:'unmap',19:'map',22:'configure',28:'property'}.get(kind)
        if name:self.counts[name]+=1
        if kind in (16,17,18,19):self.transitions.append('%s:%x%s'%(name,window,'['+category+']' if category else ''))
        if kind==17:
            if window in self.seen:self.destroyed.add(window)
            self.popups.pop(window,None);self.seen.discard(window)
    def sample(self,focus,popups):
        if focus!=self.focus:
            if self.focus is not None:self.counts['focus-change']+=1
            self.transitions.append('focus:%x'%focus);self.focus=focus
        for window,mapped in popups.items():
            if window not in self.seen:
                self.counts['popup-new']+=1;self.transitions.append('new:%x'%window)
            elif self.popups.get(window)!=mapped:
                self.counts['popup-map' if mapped else 'popup-hide']+=1
                self.transitions.append(('show:' if mapped else 'hide:')+'%x'%window)
        self.counts['popup-destroy']+=len(self.destroyed);self.destroyed.clear()
        self.seen.update(popups);self.popups=dict(popups)
        # XIDs are not kept forever during long-lived desktop sessions.
        if len(self.seen)>4096:self.seen=set(popups)
    def pointer(self,buttons,child,position=None):
        if position is not None and self.pointer_position is not None and position!=self.pointer_position:self.counts['pointer-position-change']+=1
        self.pointer_position=position
        if self.buttons!=buttons:
            if self.buttons is not None:self.counts['mouse-button-change']+=1
            self.transitions.append('buttons:%d'%buttons);self.buttons=buttons
        self.pointer_child=child
    def damage(self,window,category):
        self.counts['content-update']+=1
        self.transitions.append('paint:%x[%s]'%(window,category))
    def line(self,seconds):
        counts=','.join('%s:%d'%x for x in sorted(self.counts.items())) or 'idle'
        return 'JOBS_X11_OBSERVE t=%ds %s popup-visible=%d recent=%s'%(seconds,counts,sum(self.popups.values()),','.join(self.transitions))+' buttons=%s pointer-child=%x'%(self.buttons if self.buttons is not None else 'unknown',self.pointer_child or 0)

class DamageEvent(C.Structure):
    _fields_=[('type',C.c_int),('serial',C.c_ulong),('send_event',C.c_int),('display',C.c_void_p),('drawable',C.c_ulong),('damage',C.c_ulong),('level',C.c_int),('more',C.c_int),('timestamp',C.c_ulong)]

class DamageWatch:
    """Own bounded Damage regions, never requests pixels or alters client drawing."""
    def __init__(self,client):
        self.client=client;self.objects={};self.available=False;self.event_base=-1
        try:
            self.lib=C.CDLL(ctypes.util.find_library('Xdamage') or 'libXdamage.so.1')
            P=C.c_void_p;U=C.c_ulong;I=C.c_int
            def fn(name,result,*args):
                f=getattr(self.lib,name);f.restype=result;f.argtypes=list(args);return f
            query=fn('XDamageQueryExtension',I,P,C.POINTER(I),C.POINTER(I));event=I();error=I()
            if not query(client.display,C.byref(event),C.byref(error)):return
            self.create=fn('XDamageCreate',U,P,U,I);self.destroy=fn('XDamageDestroy',None,P,U)
            self.subtract=fn('XDamageSubtract',None,P,U,U,U)
            self.event_base=event.value;self.available=True
        except (OSError,AttributeError):pass
    def watch(self,popups):
        if not self.available:return
        for window in list(self.objects):
            if window not in popups:self.remove(window)
        for window,mapped in popups.items():
            if mapped and window not in self.objects and len(self.objects)<128:
                if window not in self.client.window_types:self.client.classify(window)
                damage=self.create(self.client.display,window,3) # XDamageReportNonEmpty
                if damage:self.objects[window]=damage
        self.client.flush(self.client.display)
    def event(self,event,record):
        if not self.available or event.type!=self.event_base:return False
        e=C.cast(C.byref(event),C.POINTER(DamageEvent)).contents
        if self.objects.get(e.drawable)==e.damage:
            record.damage(e.drawable,self.client.window_types.get(e.drawable,'unknown'))
            self.subtract(self.client.display,e.damage,0,0)
        return True
    def remove(self,window):
        damage=self.objects.pop(window,None)
        if damage is not None:self.destroy(self.client.display,damage)
    def close(self):
        for window in list(self.objects):self.remove(window)

class X11:
    def __init__(self):
        self.lib=C.CDLL(ctypes.util.find_library('X11') or 'libX11.so.6')
        def fn(name,restype,*args):
            f=getattr(self.lib,name);f.restype=restype;f.argtypes=list(args);return f
        P=C.c_void_p;U=C.c_ulong;I=C.c_int
        self.open=fn('XOpenDisplay',P,C.c_char_p);self.close=fn('XCloseDisplay',I,P)
        self.root_fn=fn('XDefaultRootWindow',U,P);self.fd_fn=fn('XConnectionNumber',I,P)
        self.select=fn('XSelectInput',I,P,U,C.c_long);self.pending=fn('XPending',I,P)
        self.next=fn('XNextEvent',I,P,C.POINTER(Event));self.flush=fn('XFlush',I,P)
        self.sync=fn('XSync',I,P,I);self.free=fn('XFree',I,P)
        self.tree=fn('XQueryTree',I,P,U,C.POINTER(U),C.POINTER(U),C.POINTER(C.POINTER(U)),C.POINTER(C.c_uint))
        self.attrs=fn('XGetWindowAttributes',I,P,U,C.POINTER(Attr))
        self.focus_fn=fn('XGetInputFocus',I,P,C.POINTER(U),C.POINTER(I))
        self.pointer_fn=fn('XQueryPointer',I,P,U,C.POINTER(U),C.POINTER(U),C.POINTER(I),C.POINTER(I),C.POINTER(I),C.POINTER(I),C.POINTER(C.c_uint))
        self.atom_fn=fn('XInternAtom',U,P,C.c_char_p,I)
        self.property_fn=fn('XGetWindowProperty',I,P,U,U,C.c_long,C.c_long,I,U,C.POINTER(U),C.POINTER(I),C.POINTER(U),C.POINTER(U),C.POINTER(C.POINTER(C.c_ubyte)))
        # A client can disappear between QueryTree and GetWindowAttributes.
        self.errors=0
        self.error_type=C.CFUNCTYPE(I,P,P)
        def error(display,event):self.errors+=1;return 0
        self.error_callback=self.error_type(error)
        fn('XSetErrorHandler',P,self.error_type)(self.error_callback)
        self.display=self.open(None)
        if not self.display:raise RuntimeError('display-unavailable')
        self.root=self.root_fn(self.display)
        self.type_atom=self.atom_fn(self.display,b'_NET_WM_WINDOW_TYPE',1)
        self.class_atom=self.atom_fn(self.display,b'WM_CLASS',1)
        self.leader_atom=self.atom_fn(self.display,b'WM_CLIENT_LEADER',1)
        self.types={self.atom_fn(self.display,('_NET_WM_WINDOW_TYPE_'+n).encode(),1):n.lower() for n in ['POPUP_MENU','DROPDOWN_MENU','TOOLTIP','DND','NORMAL','DESKTOP']}
        self.window_types={}
        self.damage=DamageWatch(self)
        # Notifications only, never redirect/grab/change focus or request input data.
        self.select(self.display,self.root,(1<<19)|(1<<22))
        self.flush(self.display)
    def property(self,window,atom,length):
        if not atom:return None
        typ=C.c_ulong();fmt=C.c_int();count=C.c_ulong();remaining=C.c_ulong();data=C.POINTER(C.c_ubyte)()
        code=self.property_fn(self.display,window,atom,0,length,0,0,C.byref(typ),C.byref(fmt),C.byref(count),C.byref(remaining),C.byref(data))
        try:
            if code or not data:return None
            if fmt.value==32:return tuple(C.cast(data,C.POINTER(C.c_ulong))[i] for i in range(min(count.value,length)))
            if fmt.value==8:return C.string_at(data,min(count.value,length*4))
        finally:
            if data:self.free(data)
    def classify(self,window):
        types=self.property(window,self.type_atom,8) or ()
        kind=next((self.types[t] for t in types if t in self.types),'unknown')
        raw=self.property(window,self.class_atom,64)
        # Only known program categories; never output raw WM_CLASS or a title.
        names=set(raw.lower().split(b'\0')) if isinstance(raw,bytes) else set()
        if not names:
            leader=self.property(window,self.leader_atom,1)
            if isinstance(leader,tuple) and leader and leader[0]!=window:
                raw=self.property(leader[0],self.class_atom,64)
                if isinstance(raw,bytes):names=set(raw.lower().split(b'\0'))
        owner='desktop' if b'xfdesktop' in names else 'panel' if b'xfce4-panel' in names else 'terminal' if b'xfce4-terminal' in names else 'other'
        value=kind+'/'+owner
        self.window_types[window]=value
        if len(self.window_types)>512:self.window_types.pop(next(iter(self.window_types)))
        return value
    def pointer_state(self):
        root=C.c_ulong();child=C.c_ulong();coords=[C.c_int() for i in range(4)];mask=C.c_uint()
        self.pointer_fn(self.display,self.root,C.byref(root),C.byref(child),*[C.byref(x) for x in coords],C.byref(mask))
        # Coordinates are used only to increment a movement counter, never logged.
        return (mask.value>>8)&31,child.value,(coords[0].value,coords[1].value)
    def snapshot(self):
        focus=C.c_ulong();revert=C.c_int();self.focus_fn(self.display,C.byref(focus),C.byref(revert))
        root=C.c_ulong();parent=C.c_ulong();children=C.POINTER(C.c_ulong)();count=C.c_uint()
        popups={}
        if self.tree(self.display,self.root,C.byref(root),C.byref(parent),C.byref(children),C.byref(count)):
            try:
                for j in range(min(count.value,2048)):
                    window=children[j];a=Attr()
                    if self.attrs(self.display,window,C.byref(a)) and a.override_redirect and a.class_==1:
                        popups[window]=(a.map_state==2)
            finally:
                if children:self.free(children)
        return focus.value,popups
    def run(self,duration=180,keep_running=lambda:True,stamp=lambda text:text,backdrop=None):
        record=Recorder();start=time.monotonic();last_emit=-10;previous='';events=0
        print(stamp('JOBS_X11_OBSERVE start duration=%s metadata-only=true damage=%s'%('manual' if duration is None else str(duration)+'s','enabled' if self.damage.available else 'unavailable')),flush=True)
        try:
            while (duration is None or time.monotonic()-start<duration) and keep_running():
                # Drain a bounded batch to avoid starving sampling on an event storm.
                for i in range(min(self.pending(self.display),2048)):
                    e=Event();self.next(self.display,C.byref(e));events+=1
                    if self.damage.event(e,record):continue
                    # XCreate/Destroy/Map/Unmap/Configure share event,parent + window slots.
                    window=int(e.words[5]) if e.type in (16,17,18,19,22) else 0
                    category=(self.window_types.get(window) or self.classify(window)) if e.type==19 else self.window_types.get(window,'')
                    record.event(e.type,window,category)
                    if e.type==17:
                        self.window_types.pop(window,None);self.damage.remove(window)
                focus,popups=self.snapshot();self.damage.watch(popups);record.sample(focus,popups);record.pointer(*self.pointer_state())
                if backdrop is not None:backdrop.tick()
                elapsed=int(time.monotonic()-start);line=record.line(elapsed)
                signature=line.split(' ',2)[2]
                if elapsed-last_emit>=1 and (signature!=previous or elapsed-last_emit>=30):
                    print(stamp(line+' x-errors=%d'%self.errors),flush=True);last_emit=elapsed;previous=signature
                select.select([self.fd_fn(self.display)],[],[],.25 if popups else 1.0)
            print(stamp('JOBS_X11_OBSERVE finished '+record.line(int(time.monotonic()-start))),flush=True)
        finally:
            self.damage.close();self.close(self.display)

CONTROL=os.path.join(os.environ.get('JOBS_X11_CONTROL','/tmp/termux-jobs-x11'),'x11-observation-control')

def read_control(token,path=CONTROL):
    try:
        fd=os.open(path,os.O_RDONLY|os.O_NOFOLLOW)
        with os.fdopen(fd,'rb') as stream:data=stream.read(257)
        if len(data)>256:return None
        parts=data.decode('ascii').split()
        if len(parts)!=3 or parts[0]!=token or not parts[1].isdigit() or len(parts[1])>10 or parts[2] not in ('on','off'):return None
        serial=int(parts[1])
        return (serial,parts[2]=='on') if serial<=2147483647 else None
    except (OSError,UnicodeError):return None

def backdrop_key(line):
    match=re.search(r'/backdrop/[A-Za-z0-9_./-]+',line)
    if not match:return None
    prop=match.group(0);key=prop.rsplit('/',1)[-1]
    if key not in {'last-image','image-style','color-style','rgba1','rgba2','cycle-enable','cycle-period','cycle-timer','single-workspace-mode','single-workspace-number'}:return None
    workspace=re.search(r'/workspace([0-9]{1,4})/',prop)
    return 'key=%s workspace=%s own-monitor=%s'%(key,workspace[1] if workspace else 'global',str('/monitorscreen/' in prop).lower())

def stop_child(child):
    if child is None:return
    if child.poll() is None:
        child.terminate()
        try:child.wait(timeout=.2)
        except subprocess.TimeoutExpired:
            child.kill()
            try:child.wait(timeout=.5)
            except subprocess.TimeoutExpired:pass
    if child.stdout:child.stdout.close()

class BackdropMonitor:
    def __init__(self,stamp):
        self.stamp=stamp;self.monitor=None;self.monitor_pipe=None;self.snapshot=None;self.buffer=b'';self.oversized=False
        self.snapshot_data=bytearray();self.dirty=True;self.next_snapshot=0;self.deadline=0
        master=slave=None
        try:
            # xfconf-query uses g_print without fflush: a PTY keeps notifications
            # line-buffered instead of delaying them in an ordinary stdout pipe.
            master,slave=pty.openpty()
            self.monitor=subprocess.Popen(['xfconf-query','-c','xfce4-desktop','--monitor'],stdin=subprocess.DEVNULL,stdout=slave,stderr=subprocess.DEVNULL)
            self.monitor_pipe=os.fdopen(master,'rb',buffering=0);master=None
            os.set_blocking(self.monitor_pipe.fileno(),False)
        except OSError:
            stop_child(self.monitor);self.monitor=None
            if self.monitor_pipe is not None:self.monitor_pipe.close();self.monitor_pipe=None
            print(stamp('JOBS_X11_BACKDROP_CHANGE monitor-unavailable'),flush=True)
        finally:
            if master is not None:os.close(master)
            if slave is not None:os.close(slave)
    def emit(self,text):print(self.stamp(text),flush=True)
    def tick(self):
        now=time.monotonic()
        if self.monitor is not None:
            try:data=os.read(self.monitor_pipe.fileno(),4096)
            except OSError:data=b''
            for value in data:
                if value==10:
                    if not self.oversized:
                        key=backdrop_key(self.buffer.decode('utf-8','replace'))
                        if key:self.emit('JOBS_X11_BACKDROP_CHANGE '+key);self.dirty=True
                    self.buffer=b'';self.oversized=False
                elif not self.oversized:
                    if len(self.buffer)<2048:self.buffer+=bytes((value,))
                    else:self.buffer=b'';self.oversized=True
            if self.monitor.poll() is not None:
                self.emit('JOBS_X11_BACKDROP_CHANGE monitor-ended');stop_child(self.monitor);self.monitor=None
                if self.monitor_pipe is not None:self.monitor_pipe.close();self.monitor_pipe=None
        if self.snapshot is not None:
            try:data=os.read(self.snapshot.stdout.fileno(),4096)
            except BlockingIOError:data=b''
            self.snapshot_data.extend(data)
            ended=self.snapshot.poll() is not None
            if len(self.snapshot_data)>32768 or now>self.deadline:
                self.emit('JOBS_X11_BACKDROP_CURRENT_BEGIN')
                self.emit('JOBS_X11_BACKDROP_CURRENT unavailable=snapshot-limit')
                self.emit('JOBS_X11_BACKDROP_CURRENT_END')
                stop_child(self.snapshot);self.snapshot=None
            elif ended and not data:
                self.emit('JOBS_X11_BACKDROP_CURRENT_BEGIN')
                lines=self.snapshot_data.decode('utf-8','replace').splitlines()
                allowed=r'JOBS_X11_BACKDROP (?:current-snapshot default-present=(?:true|false)|single-workspace=(?:true|false|unset) selected=(?:[0-9]+|unset)|image=[0-9]+ own-monitor=(?:true|false) workspace=(?:workspace[0-9]+|other) style=(?:[0-6]|unset) cycle=(?:true|false|unset) decode=[a-z0-9-]+|settings-unavailable)'
                found=False
                for line in lines:
                    if re.fullmatch(allowed,line):self.emit(line.replace('JOBS_X11_BACKDROP ','JOBS_X11_BACKDROP_CURRENT ',1));found=True
                if not found:self.emit('JOBS_X11_BACKDROP_CURRENT unavailable=no-result')
                self.emit('JOBS_X11_BACKDROP_CURRENT_END');stop_child(self.snapshot);self.snapshot=None
        if self.snapshot is None and self.dirty and now>=self.next_snapshot:
            self.dirty=False;self.next_snapshot=now+1;self.snapshot_data=bytearray();self.deadline=now+5
            try:
                self.snapshot=subprocess.Popen([sys.executable,str(Path(__file__).with_name('x11-wallpaper.py')),'--inspect'],stdout=subprocess.PIPE,stderr=subprocess.DEVNULL)
                os.set_blocking(self.snapshot.stdout.fileno(),False)
            except OSError:
                stop_child(self.snapshot);self.snapshot=None
                self.emit('JOBS_X11_BACKDROP_CURRENT unavailable=helper-unavailable')
    def close(self):
        stop_child(self.monitor);stop_child(self.snapshot);self.monitor=self.snapshot=None
        if self.monitor_pipe is not None:self.monitor_pipe.close();self.monitor_pipe=None

def controlled():
    token=os.environ.get('JOBS_X11_FOCUS_TOKEN','')
    if not re.fullmatch('[a-f0-9-]{36}',token):return
    handled=-1
    while True:
        command=read_control(token)
        if command is not None and command[0]>handled:
            serial,on=command;handled=serial
            if on:
                start=time.monotonic()
                def stamp(text):return text+' capture=%d elapsed-ms=%d utc=%s'%(serial,int((time.monotonic()-start)*1000),datetime.now(timezone.utc).isoformat(timespec='milliseconds'))
                monitor=None
                try:
                    client=X11()
                    os.write(1,('\nJOBS_X11_CAPTURE_BEGIN %s %d\n'%(token,serial)).encode())
                    os.write(1,('\nJOBS_X11_OBSERVATION %s %d active\n'%(token,serial)).encode())
                    monitor=BackdropMonitor(stamp)
                    client.run(None,lambda:read_control(token) in (None,command),stamp,monitor)
                except Exception as exc:
                    os.write(1,('\nJOBS_X11_OBSERVATION %s %d error\n'%(token,serial)).encode())
                finally:
                    if monitor is not None:monitor.close()
            else:os.write(1,('\nJOBS_X11_OBSERVATION %s %d stopped\n'%(token,serial)).encode())
        time.sleep(.5)

if __name__=='__main__':
    if '--controlled' in sys.argv[1:]:controlled()
    else:
        try:X11().run()
        except Exception as exc:print('JOBS_X11_OBSERVE unavailable='+type(exc).__name__,flush=True)
