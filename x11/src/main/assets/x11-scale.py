#!/usr/bin/env python3
"""Reversible XFCE scale overlay; restore BEFORE the optional theme on each start."""
import importlib.util,json,os,re,sys
from pathlib import Path
import xml.etree.ElementTree as ET
spec=importlib.util.spec_from_file_location('jobs_style',Path(__file__).with_name('x11-style.py'))
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
CHANNELS=('xsettings','xfce4-desktop','xfce4-panel','thunar')
KEYS=re.compile(r'^(?:xsettings\|/(?:Xft/DPI|Gtk/CursorThemeSize)|xfce4-desktop\|/desktop-icons/icon-size|xfce4-panel\|/panels/panel-[0-9]{1,6}/(?:size|icon-size)|thunar\|/(?:last-(?:window-width|window-height|separator-position|(?:icon|details|compact)-view-zoom-level)|(?:shortcuts|tree)-icon-size))$')
# Thunar's zoom is a discrete enum, independent of Xft DPI (64px = 100%).
# Use the long-established 16..256px levels, supported by every target's GTK3 Thunar.
ICON_PIXELS=(16,24,32,48,64,96,128,160,192,256)
ZOOM_PERCENT=(25,38,50,75,100,150,200,250,300,400)
LEGACY=('SMALLEST','SMALLER','SMALL','NORMAL','LARGE','LARGER','LARGEST')

def nearest(pixels,percent):
 # Compare without floating point. Round ties up when growing, down when shrinking.
 return min(ICON_PIXELS,key=lambda n:(abs(n*100-pixels*percent),-n if percent>100 else n))

def enum_pixels(root,key,zoom,fallback):
 _,n=m.node(root,key);value=n.get('value','') if n is not None else ''
 prefix='THUNAR_ZOOM_LEVEL_' if zoom else 'THUNAR_ICON_SIZE_'
 if value.startswith(prefix):
  suffix=value[len(prefix):]
  if suffix in LEGACY:return ICON_PIXELS[LEGACY.index(suffix)]
  if zoom and suffix.endswith('_PERCENT') and suffix[:-8].isdigit():
   level=int(suffix[:-8])
   if level in ZOOM_PERCENT:return ICON_PIXELS[ZOOM_PERCENT.index(level)]
  elif not zoom and suffix.isdigit() and int(suffix) in ICON_PIXELS:return int(suffix)
 # Unknown newer/custom enum: preserve it instead of replacing it with a guess.
 return fallback if n is None else None
def locations(home):
 home=Path(home)
 if not home.is_absolute():raise ValueError('scale home')
 directory=Path(os.environ.get('XDG_CONFIG_HOME') or home/'.config')/'xfce4/xfconf/xfce-perchannel-xml'
 state=home/'.local/state/termux-jobs/scale/previous.json';m.safe(home,state.parent)
 return home,directory,state

def documents(home,directory):
 out={};roots={}
 for channel in CHANNELS:
  path=directory/(channel+'.xml');m.safe(home,path);data=m.read(path);out[channel]=(path,data);roots[channel]=m.tree(data,channel)
 return out,roots

def save_documents(home,documents,roots):
 for channel,(path,before) in documents.items():
  after=ET.tostring(roots[channel],encoding='utf-8',xml_declaration=True)
  # Do not create an empty channel just to restore unrelated properties.
  if before is None and not len(roots[channel]):continue
  if before!=after:m.safe(home,path.parent,True);m.write(path,after,before)

def validate_state(old,home,directory):
 if not isinstance(old,dict) or old.get('schema')!=1:raise ValueError('scale state')
 if old.get('config')!=str(directory.relative_to(home)):raise ValueError('scale config changed')
 original=old.get('original');applied=old.get('applied');pending=old.get('pending',{})
 if not isinstance(original,dict) or not isinstance(applied,dict) or len(applied)>256 or set(original)!=set(applied):raise ValueError('scale properties')
 if not isinstance(pending,dict) or not set(pending)<=set(applied):raise ValueError('scale pending')
 for identity,value in list(original.items())+list(applied.items())+list(pending.items()):
  if not KEYS.fullmatch(identity) or value is not None and (not isinstance(value,str) or len(value)>4096):raise ValueError('scale identity')
  if value is not None:
   if '<!DOCTYPE' in value.upper() or '<!ENTITY' in value.upper():raise ValueError('scale XML declaration')
   node=ET.fromstring(value)
   if node.tag!='property' or node.get('name')!=identity.rsplit('/',1)[-1] or list(node):raise ValueError('scale snapshot')
 return old

def restore(home):
 home,directory,record=locations(home);before=m.read(record)
 if before is None:return 'off'
 old=validate_state(json.loads(before),home,directory)
 if not old.get('active'):return 'off'
 original=old['original'];applied=old['applied'];pending=old.get('pending',{})
 docs,roots=documents(home,directory)
 for identity,value in applied.items():
  channel,key=identity.split('|',1)
  if m.snapshot(roots[channel],key) in ([value]+([pending[identity]] if identity in pending else [])):m.set_node(roots[channel],key,original[identity])
 save_documents(home,docs,roots)
 old['active']=False;m.write(record,(json.dumps(old,sort_keys=True)+'\n').encode(),before)
 return 'restored'

def number(root,key,fallback):
 _,node=m.node(root,key)
 try:value=int(node.get('value')) if node is not None else fallback
 except (ValueError,TypeError):value=fallback
 return value

def desired(roots,prefix,percent):
 panels=m.inherited(m.panel_defaults(Path(prefix)),roots['xfce4-panel'])
 changes={}
 def scaled(channel,key,baseline,typ='int',limit=512):
  if baseline<=0:return
  changes[channel+'|'+key]=m.prop(key,typ,max(1,min(limit,(baseline*percent+50)//100)))
 dpi=number(roots['xsettings'],'/Xft/DPI',96)
 scaled('xsettings','/Xft/DPI',dpi if 48<=dpi<=768 else 96,limit=1152)
 scaled('xsettings','/Gtk/CursorThemeSize',number(roots['xsettings'],'/Gtk/CursorThemeSize',24))
 scaled('xfce4-desktop','/desktop-icons/icon-size',number(roots['xfce4-desktop'],'/desktop-icons/icon-size',48),'uint')
 thunar=roots['thunar']
 for key,baseline in [('window-width',640),('window-height',480),('separator-position',170)]:
  scaled('thunar','/last-'+key,number(thunar,'/last-'+key,baseline),limit=8192)
 for view,baseline in [('icon',64),('details',24),('compact',16)]:
  key='/last-'+view+'-view-zoom-level';pixels=enum_pixels(thunar,key,True,baseline)
  if pixels is not None:
   chosen=nearest(pixels,percent);level=ZOOM_PERCENT[ICON_PIXELS.index(chosen)]
   changes['thunar|'+key]=m.prop(key,'string','THUNAR_ZOOM_LEVEL_'+str(level)+'_PERCENT')
 for pane,baseline in [('shortcuts',24),('tree',16)]:
  key='/'+pane+'-icon-size';pixels=enum_pixels(thunar,key,False,baseline)
  if pixels is not None:changes['thunar|'+key]=m.prop(key,'string','THUNAR_ICON_SIZE_'+str(nearest(pixels,percent)))
 panel=panels.find("property[@name='panels']")
 if panel is not None:
  for item in panel.findall('property'):
   name=item.get('name','')
   if not re.fullmatch(r'panel-[0-9]{1,6}',name):continue
   for key,fallback in [('size',48),('icon-size',0)]:
    path='/panels/'+name+'/'+key;scaled('xfce4-panel',path,number(panels,path,fallback),'uint')
 return changes

def apply(home,prefix,percent):
 if not isinstance(percent,int) or percent<50 or percent>150:raise ValueError('scale range')
 if percent==100:return 'default'
 home,directory,record=locations(home);before=m.read(record)
 if before:
  previous=json.loads(before)
  if not isinstance(previous,dict) or previous.get('schema')!=1:raise ValueError('scale state')
  if previous.get('active'):raise ValueError('restore scale before theme')
 docs,roots=documents(home,directory)
 changes=desired(roots,prefix,percent)
 original={}
 for identity,value in changes.items():
  channel,key=identity.split('|',1);original[identity]=m.snapshot(roots[channel],key);m.set_node(roots[channel],key,value)
 state={'schema':1,'active':True,'config':str(directory.relative_to(home)),'original':original,'applied':changes}
 m.safe(home,record.parent,True);m.write(record,(json.dumps(state,sort_keys=True)+'\n').encode(),before)
 save_documents(home,docs,roots)
 return str(percent)

if __name__=='__main__':
 try:
  if sys.argv[1:]==['restore']:result=restore(Path.home())
  elif len(sys.argv)==3 and sys.argv[1]=='apply':result=apply(Path.home(),m.environment_prefix(os.environ.get('JOBS_X11_TARGET'),os.environ.get('PREFIX')),int(sys.argv[2]))
  else:raise ValueError('scale operation')
  print('JOBS_X11_UI_SCALE='+result,flush=True)
 except (OSError,ValueError,KeyError,TypeError,ET.ParseError):
  print('ERROR:X11_SCALE',flush=True);raise SystemExit(1)
