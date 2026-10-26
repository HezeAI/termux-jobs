#!/usr/bin/env python3
"""One-shot scale update on this owned desktop's existing private session bus."""
import importlib.util,json,os,re,subprocess,sys,ctypes as c
from pathlib import Path
import xml.etree.ElementTree as ET
spec=importlib.util.spec_from_file_location('scale',Path(__file__).with_name('x11-scale.py'));s=importlib.util.module_from_spec(spec);spec.loader.exec_module(s);m=s.m
TARGETS={'termux':97,'ubuntu':98,'debian':100,'alpine':101,'fedora':102,'opensuse':103,'void':104,'archlinux':105}

def session(control,register=False):
 target=os.environ.get('JOBS_X11_TARGET','');token=os.environ.get('JOBS_X11_FOCUS_TOKEN','')
 if target not in TARGETS or not re.fullmatch(r'[a-f0-9-]{36}',token):raise ValueError('session identity')
 file=control/'scale-session.json'
 if control.is_symlink() or file.is_symlink():raise ValueError('session link')
 before=m.read(file)
 if register:
  address=os.environ.get('DBUS_SESSION_BUS_ADDRESS','')
  value={'schema':1,'target':target,'token':token,'home':str(Path.home()),'bus':address,'display':os.environ.get('DISPLAY',''),'authority':os.environ.get('XAUTHORITY',''),'percent':int(os.environ.get('JOBS_X11_SCALE','100'))}
  if value['display']!=':'+str(TARGETS[target]) or not re.fullmatch(r'unix:(?:path|abstract)=[A-Za-z0-9_./-]{1,512}(?:,guid=[a-f0-9]{32})?',address):raise ValueError('session endpoint')
  if value['authority']!=str(control/'authority'):raise ValueError('session authority')
  m.write(file,(json.dumps(value)+'\n').encode(),before);return value
 if before is None or len(before)>4096:raise ValueError('session missing')
 value=json.loads(before)
 if not isinstance(value,dict) or value.get('schema')!=1 or value.get('target')!=target or value.get('token')!=token or value.get('home')!=str(Path.home()):raise ValueError('session changed')
 if value.get('display')!=':'+str(TARGETS[target]) or value.get('authority')!=str(control/'authority') or not re.fullmatch(r'unix:(?:path|abstract)=[A-Za-z0-9_./-]{1,512}(?:,guid=[a-f0-9]{32})?',value.get('bus','')):raise ValueError('session endpoint')
 os.environ.update(DBUS_SESSION_BUS_ADDRESS=value['bus'],DISPLAY=value['display'],XAUTHORITY=value['authority']);return value

class Windows:
 """Only this DISPLAY's normal Thunar clients; no titles, focus or synthetic keys."""
 def __init__(self):
  spec=importlib.util.spec_from_file_location('focus',Path(__file__).with_name('x11-focus.py'));f=importlib.util.module_from_spec(spec);spec.loader.exec_module(f)
  self.x=f.X11();x=self.x.lib;P=c.c_void_p;U=c.c_ulong;I=c.c_int
  x.XGetGeometry.argtypes=[P,U,c.POINTER(U),c.POINTER(I),c.POINTER(I),c.POINTER(c.c_uint),c.POINTER(c.c_uint),c.POINTER(c.c_uint),c.POINTER(c.c_uint)];x.XGetGeometry.restype=I
  x.XResizeWindow.argtypes=[P,U,c.c_uint,c.c_uint];x.XDisplayWidth.argtypes=[P,I];x.XDisplayHeight.argtypes=[P,I];x.XDefaultScreen.argtypes=[P]
  for name in ('_NET_CLIENT_LIST','WM_CLASS','_NET_WM_STATE_MAXIMIZED_HORZ','_NET_WM_STATE_MAXIMIZED_VERT','_NET_WM_STATE_FULLSCREEN'):self.x.atoms[name]=x.XInternAtom(self.x.d,name.encode(),0)
  screen=x.XDefaultScreen(self.x.d);self.bounds=(x.XDisplayWidth(self.x.d,screen),x.XDisplayHeight(self.x.d,screen));self.windows=[]
  for wid in self.x.prop(self.x.root,'_NET_CLIENT_LIST')[:128]:
   typ=U();fmt=I();n=U();after=U();data=P()
   result=x.XGetWindowProperty(self.x.d,wid,self.x.atoms['WM_CLASS'],0,128,False,0,c.byref(typ),c.byref(fmt),c.byref(n),c.byref(after),c.byref(data))
   try:classes=c.string_at(data,n.value).split(b'\0') if result==0 and fmt.value==8 and data.value and n.value<=512 else []
   finally:
    if data.value:x.XFree(data)
   if b'Thunar' not in classes and b'thunar' not in classes:continue
   states=self.x.prop(wid,'_NET_WM_STATE');wm=self.x.prop(wid,'WM_STATE')
   excluded={self.x.atoms[key] for key in ('_NET_WM_STATE_HIDDEN','_NET_WM_STATE_MAXIMIZED_HORZ','_NET_WM_STATE_MAXIMIZED_VERT','_NET_WM_STATE_FULLSCREEN')}
   if not wm or wm[0]!=1 or excluded.intersection(states):continue
   root=U();left=I();top=I();width=c.c_uint();height=c.c_uint();border=c.c_uint();depth=c.c_uint()
   if x.XGetGeometry(self.x.d,wid,c.byref(root),c.byref(left),c.byref(top),c.byref(width),c.byref(height),c.byref(border),c.byref(depth)):self.windows.append((wid,width.value,height.value))
 def resize(self,previous,percent):
  if not 50<=previous<=150 or previous==percent:return
  for wid,width,height in self.windows:
   w=max(160,min(self.bounds[0],(width*percent+previous//2)//previous));h=max(120,min(self.bounds[1],(height*percent+previous//2)//previous));self.x.lib.XResizeWindow(self.x.d,wid,w,h)
  self.x.lib.XFlush(self.x.d)
 def close(self):self.x.close()

class Conf:
 def __init__(self):
  self.lib=None
  for name in ('libxfconf-0.so.3','libxfconf-0.so'):
   try:self.lib=c.CDLL(name);break
   except OSError:pass
  if self.lib is None:raise OSError('xfconf library')
  self.glib=None
  for name in ('libglib-2.0.so.0','libglib-2.0.so'):
   try:self.glib=c.CDLL(name);break
   except OSError:pass
  if self.glib is None:raise OSError('glib library')
  self.glib.g_free.argtypes=[c.c_void_p];self.glib.g_main_context_iteration.argtypes=[c.c_void_p,c.c_int]
  self.channels={};x=self.lib
  signatures={'xfconf_init':(c.c_int,[c.c_void_p]),'xfconf_channel_get':(c.c_void_p,[c.c_char_p]),'xfconf_shutdown':(None,[]),'xfconf_channel_has_property':(c.c_int,[c.c_void_p,c.c_char_p]),'xfconf_channel_reset_property':(None,[c.c_void_p,c.c_char_p,c.c_int])}
  for typ,ct in [('int',c.c_int32),('uint',c.c_uint32),('string',c.c_char_p)]:
   signatures['xfconf_channel_get_'+typ]=(c.c_void_p if typ=='string' else ct,[c.c_void_p,c.c_char_p,ct]);signatures['xfconf_channel_set_'+typ]=(c.c_int,[c.c_void_p,c.c_char_p,ct])
  for name,(ret,args) in signatures.items():getattr(x,name).restype=ret;getattr(x,name).argtypes=args
  if not x.xfconf_init(None):raise OSError('xfconf unavailable')
 def channel(self,name):
  if name not in self.channels:self.channels[name]=self.lib.xfconf_channel_get(name.encode())
  if not self.channels[name]:raise ValueError('xfconf channel')
  return self.channels[name]
 def get(self,identity,template):
  if not s.KEYS.fullmatch(identity):raise ValueError('scale property')
  channel,key=identity.split('|');channel=self.channel(channel);path=key.encode()
  for _ in range(32):
   if not self.glib.g_main_context_iteration(None,False):break
  if not self.lib.xfconf_channel_has_property(channel,path):return None
  typ=ET.fromstring(template).get('type')
  if typ not in ('int','uint','string'):raise ValueError('scale type')
  value=getattr(self.lib,'xfconf_channel_get_'+typ)(channel,path,None if typ=='string' else 0)
  if typ=='string':
   if not value:raise ValueError('scale string')
   pointer=value
   try:value=c.string_at(pointer).decode('utf-8')
   finally:self.glib.g_free(pointer)
   if len(value)>4096:raise ValueError('scale string size')
  return m.prop(key,typ,value)
 def put(self,identity,value):
  if not s.KEYS.fullmatch(identity):raise ValueError('scale property')
  channel,key=identity.split('|');channel=self.channel(channel);path=key.encode()
  if value is None:self.lib.xfconf_channel_reset_property(channel,path,False);return
  node=ET.fromstring(value);typ=node.get('type');value=node.get('value')
  # XFCE XML represents an unset leaf as type="empty". Restoring that
  # original means removing the scalar override, not assigning a new type.
  if typ=='empty' and value is None and not list(node):
   self.lib.xfconf_channel_reset_property(channel,path,False);return
  if typ not in ('int','uint','string'):raise ValueError('scale type')
  arg=value.encode() if typ=='string' else int(value)
  if not getattr(self.lib,'xfconf_channel_set_'+typ)(channel,path,arg):raise ValueError('scale write')
 def close(self):self.lib.xfconf_shutdown()

def apply(home,prefix,percent,conf):
 if type(percent) is not int or not 50<=percent<=150:raise ValueError('scale range')
 home,directory,record=s.locations(home);before=m.read(record);old=s.validate_state(json.loads(before),home,directory) if before else None
 _,roots=s.documents(home,directory);templates=s.desired(roots,prefix,100)
 if old and old.get('active'):
  for key,val in old['applied'].items():
   if key not in templates:templates[key]=val or old['original'][key] or old.get('pending',{}).get(key)
 current={};baseline={}
 for identity,template in templates.items():
  if template is None:continue
  channel,key=identity.split('|');value=conf.get(identity,template);current[identity]=value
  base=value
  if old and old.get('active') and identity in old['applied'] and value in [old['applied'][identity],old.get('pending',{}).get(identity,object())]:base=old['original'][identity]
  baseline[identity]=base;m.set_node(roots[channel],key,base)
 changes=s.desired(roots,prefix,percent) if percent!=100 else {}
 desired={key:baseline[key] for key in current if old and old.get('active') and key in old['applied']};desired.update(changes)
 if not desired:return
 for key in desired:
  if key not in current:raise ValueError('scale layout changed')
 journal={'schema':1,'active':True,'config':str(directory.relative_to(home)),'original':{key:baseline[key] for key in desired},'applied':desired,'pending':{key:current[key] for key in desired}}
 m.safe(home,record.parent,True);data=(json.dumps(journal,sort_keys=True)+'\n').encode();m.write(record,data,before)
 changed=[]
 try:
  for identity,value in desired.items():
   if current[identity]==value:continue
   template=templates.get(identity) or changes.get(identity)
   if conf.get(identity,template)!=current[identity]:raise ValueError('scale concurrently changed')
   conf.put(identity,value);changed.append(identity)
  state={'schema':1,'active':percent!=100,'config':str(directory.relative_to(home)),'original':{key:baseline[key] for key in changes},'applied':changes}
  m.write(record,(json.dumps(state,sort_keys=True)+'\n').encode(),data)
 except Exception:
  # Preserve the journal if rollback itself fails; next startup can recover it.
  for identity in reversed(changed):
   if conf.get(identity,templates[identity])==desired[identity]:conf.put(identity,current[identity])
  if m.read(record)==data:
   if before is None:record.unlink()
   else:m.write(record,before,data)
  raise

if __name__=='__main__':
 conf=None;windows=None
 try:
  control=Path(__file__).parent
  if sys.argv[1:]==['register']:session(control,True)
  elif len(sys.argv)==3 and sys.argv[1]=='apply':
   current=session(control);percent=int(sys.argv[2])
   if not 50<=percent<=150:raise ValueError('scale range')
   try:windows=Windows()
   except (OSError,ValueError):pass
   conf=Conf();apply(Path.home(),m.environment_prefix(os.environ.get('JOBS_X11_TARGET'),os.environ.get('PREFIX')),percent,conf)
   if windows is not None:windows.resize(current.get('percent',100),percent)
   file=control/'scale-session.json';before=m.read(file)
   latest=json.loads(before) if before else {}
   if latest.get('token')!=current['token'] or latest.get('target')!=current['target']:raise ValueError('session changed')
   current['percent']=percent;m.write(file,(json.dumps(current)+'\n').encode(),before)
  else:raise ValueError('scale operation')
  print('READY:screen-scale',flush=True)
 except Exception:print('ERROR:X11_SCALE_LIVE',flush=True);raise SystemExit(1)
 finally:
  if conf is not None:conf.close()
  if windows is not None:windows.close()
