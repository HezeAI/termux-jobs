#!/usr/bin/env python3
"""Opt-in per-environment XFCE appearance; reversible selected properties before XFCE starts."""
import copy,hashlib,io,json,os,stat,sys,tempfile,zipfile
from pathlib import Path,PurePosixPath
import xml.etree.ElementTree as ET
PIN='6bc19721d09cb5d16b5ee5df6f2758da9653a88e0211dd81ef5fcb408386aec0'
LIMIT=8*1024*1024

def read(path):
 try:fd=os.open(path,os.O_RDONLY|os.O_NOFOLLOW|os.O_NONBLOCK)
 except FileNotFoundError:return None
 with os.fdopen(fd,'rb') as f:
  st=os.fstat(f.fileno())
  if not stat.S_ISREG(st.st_mode) or st.st_size>LIMIT:raise ValueError('style file unsupported')
  value=f.read(LIMIT+1)
  if len(value)>LIMIT:raise ValueError('style file too large')
  return value

def safe(home,path,create=False):
 relative=path.relative_to(home)
 if '..' in relative.parts:raise ValueError('style path outside home')
 parent=home
 for name in relative.parts:
  parent=parent/name
  if parent.is_symlink():raise ValueError('style symlink preserved')
  if create:parent.mkdir(mode=0o700,exist_ok=True)
 return path

def write(path,data,before):
 fd,tmp=tempfile.mkstemp(prefix='.jobs-style-',dir=path.parent)
 try:
  with os.fdopen(fd,'wb') as f:os.fchmod(f.fileno(),0o600);f.write(data);f.flush();os.fsync(f.fileno())
  if read(path)!=before:raise ValueError('style file changed')
  os.replace(tmp,path)
 finally:
  if os.path.exists(tmp):os.unlink(tmp)

def tree(data,channel):
 if data is None:return ET.Element('channel',name=channel,version='1.0')
 if b'<!DOCTYPE' in data.upper() or b'<!ENTITY' in data.upper():raise ValueError('style XML declarations')
 root=ET.fromstring(data)
 if root.tag!='channel' or root.get('name')!=channel:raise ValueError('style channel mismatch')
 return root

def node(root,key,create=False):
 parts=key.strip('/').split('/');parent=root
 for name in parts[:-1]:
  child=next((x for x in parent.findall('property') if x.get('name')==name),None)
  if child is None:
   if not create:return None,None
   child=ET.SubElement(parent,'property',name=name,type='empty')
  parent=child
 child=next((x for x in parent.findall('property') if x.get('name')==parts[-1]),None)
 return parent,child

def canonical(n):
 n=copy.deepcopy(n)
 for item in n.iter():
  item.tail=None
  if item.text is not None and not item.text.strip():item.text=None
  attrs=sorted(item.attrib.items());item.attrib.clear();item.attrib.update(attrs)
 return ET.tostring(n,encoding='unicode')

def snapshot(root,key):
 _,n=node(root,key);return canonical(n) if n is not None else None

def set_node(root,key,value):
 parent,n=node(root,key,True)
 if n is not None:parent.remove(n)
 if value is not None:parent.append(ET.fromstring(value))

def prop(key,typ,value):return canonical(ET.Element('property',name=key.rsplit('/',1)[-1],type=typ,value=str(value).lower() if isinstance(value,bool) else str(value)))

def inherited(base,override):
 # XFConf's empty local properties inherit system values (notably openSUSE).
 # Used only to discover panel layout/plugins; snapshots/writes keep local XML.
 result=copy.deepcopy(base)
 if override.get('type')!='empty':
  result.attrib.clear();result.attrib.update(override.attrib)
  for item in list(result):
   if item.tag!='property':result.remove(item)
  for item in override:
   if item.tag!='property':result.append(copy.deepcopy(item))
 for item in override.findall('property'):
  previous=next((n for n in result.findall('property') if n.get('name')==item.get('name')),None)
  updated=inherited(previous,item) if previous is not None else copy.deepcopy(item)
  if previous is not None:result.remove(previous)
  result.append(updated)
 return result

def panel_defaults(prefix):
 # A distribution can provide the XFConf channel directly instead of default.xml.
 for relative in ('etc/xdg/xfce4/xfconf/xfce-perchannel-xml/xfce4-panel.xml','etc/xdg/xfce4/panel/default.xml'):
  data=read(prefix/relative)
  if data is not None:return tree(data,'xfce4-panel')
 return tree(None,'xfce4-panel')

def desired(roots,wallpaper):
 out={}
 def add(channel,key,typ,value):out[channel+'|'+key]=prop(key,typ,value)
 for key,typ,value in [('Net/ThemeName','string','TermuxJobsStyle'),('Net/IconThemeName','string','TermuxJobsIcons'),('Gtk/FontName','string','DejaVu Sans 12'),('Gtk/CursorThemeName','string','TermuxJobsCursor'),('Gtk/CursorThemeSize','int',30)]:add('xsettings','/'+key,typ,value)
 for key,value in [('theme','TermuxJobsStyle'),('title_font','DejaVu Sans Bold 12'),('title_alignment','left'),('button_layout','O|HMC')]:add('xfwm4','/general/'+key,'string',value)
 for key,typ,value in [('icon-size','uint',74),('font-size','double',11),('use-custom-font-size','bool',True)]:add('xfce4-desktop','/desktop-icons/'+key,typ,value)
 background=roots['xfce4-desktop'];backdrops=set('/backdrop/screen0/monitorscreen/workspace%d'%n for n in range(4))
 for screen in background.findall("./property[@name='backdrop']/property"):
  for monitor in screen.findall('property'):
   for workspace in monitor.findall('property'):
    if workspace.get('name','').startswith('workspace'):backdrops.add('/backdrop/'+screen.get('name')+'/'+monitor.get('name')+'/'+workspace.get('name'))
 for base in sorted(backdrops)[:32]:
  add('xfce4-desktop',base+'/last-image','string',str(wallpaper));add('xfce4-desktop',base+'/image-style','int',5);add('xfce4-desktop',base+'/cycle-enable','bool',False)
 panel=roots['xfce4-panel'].find("property[@name='panels']")
 if panel is not None:
  add('xfce4-panel','/panels/dark-mode','bool',True)
  for item in panel.findall('property'):
   name=item.get('name','')
   if not name.startswith('panel-') or not name[6:].isdigit():continue
   pos=item.find("property[@name='position']");bottom=pos is not None and 'p=10;' in pos.get('value','')
   for key,typ,value in [('size','uint',81 if bottom else 44),('icon-size','uint',52 if bottom else 30),('background-style','uint',0)]:add('xfce4-panel','/panels/'+name+'/'+key,typ,value)
   if bottom:
    add('xfce4-panel','/panels/'+name+'/autohide-behavior','uint',0)
    add('xfce4-panel','/panels/'+name+'/length','uint',100)
    add('xfce4-panel','/panels/'+name+'/nrows','uint',1)
 plugins=roots['xfce4-panel'].find("property[@name='plugins']")
 if plugins is not None:
  menu_ids=set()
  for item in plugins.findall('property'):
   name=item.get('name','')
   if name.startswith('plugin-') and name[7:].isdigit() and item.get('value')=='applicationsmenu':
    menu_ids.add(name[7:])
    add('xfce4-panel','/plugins/'+name+'/button-title','string','Menu')
    add('xfce4-panel','/plugins/'+name+'/show-button-title','bool',True)
    add('xfce4-panel','/plugins/'+name+'/button-icon','string','termux-jobs-menu')
  # Move an existing Menu to the existing bottom panel, keeping every plugin ID.
  # Do not create plugins or replace the user's complete panel configuration.
  if panel is not None:
   panels=[item for item in panel.findall('property') if item.get('name','').startswith('panel-') and item.get('name','')[6:].isdigit()]
   bottom=next((item for item in panels if item.find("property[@name='position']") is not None and 'p=10;' in item.find("property[@name='position']").get('value','') and item.find("property[@name='plugin-ids']") is not None),None)
   if bottom is not None:
    destination=bottom.find("property[@name='plugin-ids']");moved=[]
    for item in panels:
     ids=item.find("property[@name='plugin-ids']")
     if item is bottom or ids is None:continue
     chosen=[value for value in ids.findall('value') if value.get('value') in menu_ids]
     if chosen:
      updated=copy.deepcopy(ids)
      for value in list(updated):
       if value.tag=='value' and value.get('value') in menu_ids:updated.remove(value)
      out['xfce4-panel|/panels/'+item.get('name')+'/plugin-ids']=canonical(updated)
      moved.extend(copy.deepcopy(chosen))
    if moved:
     updated=copy.deepcopy(destination);existing={value.get('value') for value in updated.findall('value')}
     for value in reversed(moved):
      if value.get('value') not in existing:updated.insert(0,value);existing.add(value.get('value'))
     out['xfce4-panel|/panels/'+bottom.get('name')+'/plugin-ids']=canonical(updated)
    # Keep these identities on later launches, when Menu is already at bottom.
    for item in panels:
     ids=item.find("property[@name='plugin-ids']")
     if ids is not None:out.setdefault('xfce4-panel|/panels/'+item.get('name')+'/plugin-ids',canonical(ids))
 add('thunar','/last-icon-view-zoom-level','string','THUNAR_ZOOM_LEVEL_150_PERCENT')
 return out

CHANNELS=('xsettings','xfwm4','xfce4-desktop','xfce4-panel','thunar')
def apply(home,prefix,control,enabled):
 home=Path(home);prefix=Path(prefix);control=Path(control)
 if not home.is_absolute() or not prefix.is_absolute():raise ValueError('style absolute paths required')
 state_dir=home/'.local/state/termux-jobs/style';safe(home,state_dir)
 record=state_dir/'previous.json';before=read(record);old=json.loads(before) if before else {}
 if old and (old.get('schema')!=1 or not isinstance(old.get('original'),dict) or not isinstance(old.get('applied'),dict)):raise ValueError('style state unsupported')
 if not enabled and not old.get('active'):return 'off'
 config=Path(os.environ.get('XDG_CONFIG_HOME') or home/'.config');directory=config/'xfce4/xfconf/xfce-perchannel-xml';safe(home,directory)
 documents={};roots={}
 defaults=panel_defaults(prefix) if enabled else None
 for channel in CHANNELS:
  path=directory/(channel+'.xml');safe(home,path);data=read(path);documents[channel]=(path,data)
  roots[channel]=copy.deepcopy(defaults) if channel=='xfce4-panel' and data is None and defaults is not None else tree(data,channel)
 if enabled:
  pack=read(control/'x11-style.zip')
  if pack is None or hashlib.sha256(pack).hexdigest()!=PIN:raise ValueError('style archive integrity')
  assets={}
  with zipfile.ZipFile(io.BytesIO(pack)) as z:
   total=0
   if len(z.infolist())>192:raise ValueError('style archive count')
   for item in z.infolist():
    rel=PurePosixPath(item.filename)
    if rel.is_absolute() or '..' in rel.parts or item.is_dir() or stat.S_ISLNK(item.external_attr>>16):raise ValueError('style archive entry')
    total+=item.file_size
    if total>16*1024*1024:raise ValueError('style archive limit')
    if item.filename.startswith('theme/'):dest=home/'.themes/TermuxJobsStyle'/str(rel.relative_to('theme'))
    elif item.filename.startswith('cursor/'):dest=home/'.icons/TermuxJobsCursor'/str(rel.relative_to('cursor'))
    elif item.filename.startswith('icons/'):dest=home/'.icons/TermuxJobsIcons'/str(rel.relative_to('icons'))
    elif item.filename=='wallpaper.png':dest=home/'.local/share/termux-jobs/style/wallpaper.png'
    else:raise ValueError('style archive name')
    safe(home,dest);data=z.read(item);previous=read(dest);known=old.get('files',{}).get(str(dest.relative_to(home)))
    if previous is not None and previous!=data and hashlib.sha256(previous).hexdigest() not in (known,old.get('previous_files',{}).get(str(dest.relative_to(home)))):raise ValueError('edited style asset preserved')
    assets[dest]=(data,previous)
  effective=dict(roots);effective['xfce4-panel']=inherited(defaults,roots['xfce4-panel'])
  changes=desired(effective,home/'.local/share/termux-jobs/style/wallpaper.png')
  # Switching presets must restore properties that belong only to the old one.
  for identity,value in old.get('applied',{}).items():
   if identity not in changes:
    channel,key=identity.split('|',1)
    if channel in roots and snapshot(roots[channel],key)==value:set_node(roots[channel],key,old.get('original',{}).get(identity))
  original=dict(old.get('original',{})) if old.get('active') else {}
  for identity,value in changes.items():
   channel,key=identity.split('|',1)
   if identity not in original:original[identity]=snapshot(roots[channel],key)
  state={'schema':1,'active':True,'original':original,'applied':changes,'files':{str(p.relative_to(home)):hashlib.sha256(data).hexdigest() for p,(data,_) in assets.items()},'previous_files':{str(p.relative_to(home)):hashlib.sha256(previous).hexdigest() for p,(_,previous) in assets.items() if previous is not None}}
  safe(home,state_dir,True);data=(json.dumps(state,sort_keys=True)+'\n').encode();write(record,data,before)
  for dest,(data,previous) in assets.items():
   if data!=previous:safe(home,dest.parent,True);write(dest,data,previous)
  for identity,value in changes.items():
   channel,key=identity.split('|',1);set_node(roots[channel],key,value)
 else:
  for identity,value in old['applied'].items():
   channel,key=identity.split('|',1)
   if channel not in roots or key not in [x.split('|',1)[1] for x in old['original'] if x.startswith(channel+'|')]:raise ValueError('invalid style restore identity')
   # Later manual changes win over the saved preset.
   if snapshot(roots[channel],key)==value:set_node(roots[channel],key,old['original'].get(identity))
 for channel,(path,data) in documents.items():
  if data is None and not enabled and not any(x is not None for x in old['original'].values()):continue
  new=ET.tostring(roots[channel],encoding='utf-8',xml_declaration=True)
  if data!=new:safe(home,path.parent,True);write(path,new,data)
 if not enabled:
  old['active']=False;data=(json.dumps(old,sort_keys=True)+'\n').encode();write(record,data,before)
 return 'on' if enabled else 'restored'

def environment_prefix(target,prefix):
 if target in ('ubuntu','debian','alpine','fedora','opensuse','void','archlinux'):return Path('/')  # Each guest owns its /etc/xdg; never inherit Termux PREFIX.
 if target=='termux' and prefix:return Path(prefix)
 raise ValueError('style requires a supported X11 desktop')

if __name__=='__main__':
 try:
  if sys.argv[1:] not in [['0'],['1']]:raise ValueError('invalid style selection')
  prefix=environment_prefix(os.environ.get('JOBS_X11_TARGET'),os.environ.get('PREFIX'))
  print('JOBS_X11_STYLE='+apply(Path.home(),prefix,os.environ['JOBS_X11_CONTROL'],sys.argv[1]!='0'),flush=True)
 except (OSError,ValueError,KeyError,ET.ParseError,zipfile.BadZipFile):
  print('ERROR:TERMUX_STYLE: original settings and user edits preserved where possible',flush=True);raise SystemExit(1)
