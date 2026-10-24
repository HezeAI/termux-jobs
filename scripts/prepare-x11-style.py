#!/usr/bin/env python3
"""Build original Termux Job's UI art/cursors using host Cairo; no phone changes."""
import ctypes as C,ctypes.util,hashlib,io,json,struct,zipfile,sys,math
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
lib=C.CDLL(ctypes.util.find_library('cairo'))
def bind(name,args,restype=None):
 f=getattr(lib,'cairo_'+name);f.argtypes=args;f.restype=restype;return f
ptr=C.c_void_p;dbl=C.c_double;integer=C.c_int
surface=bind('image_surface_create',[integer,integer,integer],ptr);context=bind('create',[ptr],ptr)
for name,args in {'set_source_rgb':[ptr,dbl,dbl,dbl],'set_source_rgba':[ptr,dbl,dbl,dbl,dbl],'paint':[ptr],'move_to':[ptr,dbl,dbl],'line_to':[ptr,dbl,dbl],'close_path':[ptr],'fill_preserve':[ptr],'fill':[ptr],'stroke':[ptr],'set_line_width':[ptr,dbl],'arc':[ptr,dbl,dbl,dbl,dbl,dbl],'rectangle':[ptr,dbl,dbl,dbl,dbl],'destroy':[ptr],'surface_destroy':[ptr],'surface_flush':[ptr],'select_font_face':[ptr,C.c_char_p,integer,integer],'set_font_size':[ptr,dbl],'show_text':[ptr,C.c_char_p]}.items():globals()[name]=bind(name,args)
class Ext(C.Structure):_fields_=[(n,dbl) for n in ['xb','yb','w','h','xa','ya']]
extents=bind('text_extents',[ptr,C.c_char_p,C.POINTER(Ext)])
getdata=bind('image_surface_get_data',[ptr],ptr)
writepng=bind('surface_write_to_png_stream',[ptr,ptr,ptr],integer)
pattern_create=bind('pattern_create_linear',[dbl,dbl,dbl,dbl],ptr)
pattern_stop=bind('pattern_add_color_stop_rgb',[ptr,dbl,dbl,dbl,dbl])
pattern_destroy=bind('pattern_destroy',[ptr])
set_source=bind('set_source',[ptr,ptr])
def gradient(c,top,bottom,height,offset=0):
 p=pattern_create(0,offset,0,offset+height)
 pattern_stop(p,0,*top);pattern_stop(p,1,*bottom);set_source(c,p);pattern_destroy(p)
def png(s):
 out=io.BytesIO();callback=C.CFUNCTYPE(integer,ptr,ptr,C.c_uint)(lambda _,data,n:(out.write(C.string_at(data,n)),0)[1]);assert writepng(s,callback,None)==0;return out.getvalue()
def wallpaper():
 data=(ROOT/'design/x11-kali/wallpaper.png').read_bytes()
 if not data.startswith(b'\x89PNG\r\n\x1a\n') or hashlib.sha256(data).hexdigest()!='d5014613d7d16b1e047052ab48c5868d879a666b6e8038e08bd4f5392aeeec18':raise ValueError('Kali-inspired wallpaper integrity')
 return data

def cursor(kind):
 s=surface(0,32,32);c=context(s)
 def path(points,close=False):
  move_to(c,*points[0])
  for p in points[1:]:line_to(c,*p)
  if close:close_path(c)
 def outlined(points,close=False):
  path(points,close);set_source_rgb(c,.015,.02,.025);set_line_width(c,5);stroke(c);path(points,close);set_source_rgb(c,.96,.98,1);set_line_width(c,3);stroke(c);path(points,close);set_source_rgb(c,.015,.02,.025);set_line_width(c,1.5);stroke(c)
 hot=(16,16)
 if kind in ('arrow','help','progress','right'):
  points=[(5,3),(5,25),(11,20),(16,29),(20,27),(15,18),(25,18)]
  if kind=='right':points=[(31-x,y) for x,y in points]
  path(points,True);set_source_rgb(c,.015,.02,.025);fill_preserve(c);set_source_rgb(c,.96,.98,1);set_line_width(c,1.6);stroke(c);hot=(26,3) if kind=='right' else (5,3)
 elif kind=='hand':
  path([(11,28),(7,20),(4,17),(6,14),(11,18),(11,5),(14,3),(17,5),(17,13),(20,11),(23,13),(26,13),(29,17),(27,27),(24,30),(15,30)],True);set_source_rgb(c,.015,.02,.025);fill_preserve(c);set_source_rgb(c,.96,.98,1);set_line_width(c,1.5);stroke(c);hot=(14,4)
 elif kind=='text':
  for points in [[(16,4),(16,28)],[(11,4),(21,4)],[(11,28),(21,28)]]:outlined(points)
 elif kind in ('h','v','nw','ne','move','cross'):
  vectors={'h':[(1,0)],'v':[(0,1)],'nw':[(.7,.7)],'ne':[(.7,-.7)],'move':[(1,0),(0,1)],'cross':[(1,0),(0,1)]}[kind]
  for x,y in vectors:
   a=(16-11*x,16-11*y);b=(16+11*x,16+11*y);outlined([a,b])
   if kind!='cross':
    for sign,point in [(-1,a),(1,b)]:
     px,py=point;outlined([(px-sign*5*x-4*y,py-sign*5*y+4*x),point,(px-sign*5*x+4*y,py-sign*5*y-4*x)])
 elif kind in ('wait','deny'):
  set_source_rgb(c,.015,.02,.025);arc(c,16,16,11,0,6.2832);fill_preserve(c);set_source_rgb(c,.96,.98,1);set_line_width(c,1.6);stroke(c)
  outlined([(8,24),(24,8)] if kind=='deny' else [(16,8),(16,16),(22,19)])
 if kind in ('help','progress'):
  set_source_rgb(c,.015,.02,.025);arc(c,24,25,6,0,6.2832);fill_preserve(c);set_source_rgb(c,.96,.98,1);set_line_width(c,1.3);stroke(c);move_to(c,24,21);line_to(c,24,25);line_to(c,27,26);stroke(c)
 surface_flush(s);pixels=C.string_at(getdata(s),32*32*4);destroy(c);surface_destroy(s)
 header=struct.pack('<4sIII',b'Xcur',16,0x10000,1)+struct.pack('<III',0xfffd0002,32,28)
 chunk=struct.pack('<9I',36,0xfffd0002,32,1,32,32,*hot,0)
 return header+chunk+pixels

def rounded(c,x,y,w,h,r):
 new_path=bind('new_path',[ptr]);new_path(c)
 for cx,cy,start in [(x+w-r,y+r,-1.57079633),(x+w-r,y+h-r,0),(x+r,y+h-r,1.57079633),(x+r,y+r,3.14159265)]:arc(c,cx,cy,r,start,start+1.57079633)
 close_path(c)

def add_icons(entries):
 """Original blue base set; keep theme lookup independent of installed extra packs."""
 def icon(kind):
  s=surface(0,96,96);c=context(s)
  def box(x,y,w,h,top,bottom,r=6):
   rounded(c,x,y,w,h,r);gradient(c,top,bottom,h,y);fill_preserve(c);set_source_rgba(c,.35,.68,1,.65);set_line_width(c,1.2);stroke(c)
  def glyph(points,width=4):
   set_source_rgb(c,.88,.96,1);set_line_width(c,width);move_to(c,*points[0])
   for point in points[1:]:line_to(c,*point)
   stroke(c)
  if kind.startswith('folder'):
   box(9,17,38,27,(.20,.64,1),(.06,.33,.77),5);box(9,28,78,56,(.25,.69,1),(.025,.33,.89),7)
   set_source_rgba(c,.60,.88,1,.6);set_line_width(c,1.5);move_to(c,16,32);line_to(c,80,32);stroke(c)
   if kind=='folder-home':
    set_source_rgb(c,.02,.20,.49);move_to(c,32,56);line_to(c,48,41);line_to(c,64,56);line_to(c,59,56);line_to(c,59,72);line_to(c,51,72);line_to(c,51,61);line_to(c,45,61);line_to(c,45,72);line_to(c,37,72);line_to(c,37,56);close_path(c);fill(c)
   elif kind=='folder-documents':
    glyph([(37,42),(53,42),(60,49),(60,72),(37,72),(37,42)],3)
    for y in [54,60,66]:glyph([(42,y),(54,y)],2)
   elif kind=='folder-download':glyph([(48,42),(48,67)],5);glyph([(36,56),(48,69),(61,56)],5)
   elif kind=='folder-pictures':
    glyph([(31,44),(66,44),(66,71),(31,71),(31,44)],3);glyph([(33,68),(45,54),(55,65),(60,59),(65,66)],3)
   elif kind=='folder-desktop':glyph([(31,42),(66,42),(66,65),(31,65),(31,42)],3);glyph([(48,66),(48,73)],3)
  elif kind in ('terminal','menu'):
   box(10,12,76,72,(.15,.20,.26),(.035,.055,.08),10)
   glyph([(26,31),(43,47),(26,63)],5);glyph([(51,64),(72,64)],5)
  elif kind=='browser':
   arc(c,48,48,37,0,6.2832);gradient(c,(.07,.60,1),(.025,.23,.80),74,11);fill(c)
   set_source_rgba(c,.79,.94,1,.85);set_line_width(c,7);arc(c,46,48,25,-.7,4.8);stroke(c)
   set_source_rgba(c,.15,.61,1,.95);set_line_width(c,12);arc(c,45,53,24,.25,3.4);stroke(c)
   set_source_rgba(c,.86,.97,1,.70);set_line_width(c,2);arc(c,49,44,21,3.7,6.0);stroke(c)
  elif kind=='trash':
   box(23,24,51,58,(.77,.84,.91),(.35,.44,.55),7);box(20,18,57,8,(.90,.94,.98),(.47,.59,.70),4)
   set_source_rgb(c,.03,.37,.85);set_line_width(c,5);move_to(c,35,48);line_to(c,47,36);line_to(c,60,48);stroke(c);move_to(c,61,52);line_to(c,59,66);line_to(c,44,66);stroke(c);move_to(c,40,64);line_to(c,33,57);stroke(c)
  elif kind=='computer':
   box(10,12,76,57,(.35,.43,.53),(.11,.17,.24));box(16,18,64,44,(.09,.41,.77),(.015,.10,.25),3);box(40,69,16,10,(.37,.47,.57),(.14,.22,.32),2);box(24,80,48,6,(.47,.57,.67),(.24,.33,.43),2)
  elif kind=='drive':
   box(12,31,72,46,(.61,.71,.82),(.20,.29,.39));box(15,62,66,13,(.24,.33,.44),(.10,.17,.25),3);set_source_rgb(c,.10,.64,1);arc(c,73,68,2.4,0,6.2832);fill(c)
  elif kind=='document':
   box(23,10,51,76,(.87,.93,.99),(.61,.72,.84),3);set_source_rgb(c,.12,.29,.48);set_line_width(c,2.4)
   for y in range(32,75,10):move_to(c,32,y);line_to(c,64,y);stroke(c)
  result=png(s);destroy(c);surface_destroy(s);return result
 groups={
  'folder':('places',['folder','folder-publicshare','folder-templates','folder-music','folder-videos']),
  'folder-home':('places',['user-home']),
  'folder-desktop':('places',['user-desktop']),
  'folder-documents':('places',['folder-documents']),
  'folder-download':('places',['folder-download','folder-downloads']),
  'folder-pictures':('places',['folder-pictures']),
  'terminal':('apps',['utilities-terminal','xfce4-terminal','org.xfce.terminal']),
  'menu':('apps',['termux-jobs-menu']),
  'folder-manager':('apps',['org.xfce.thunar','Thunar']),
  'browser':('apps',['web-browser','applications-internet','firefox','firefox-esr']),
  'trash':('places',['user-trash','user-trash-full']),
  'computer':('devices',['computer']),
  'drive':('devices',['drive-harddisk','drive-harddisk-system']),
  'document':('mimetypes',['text-x-generic','application-x-executable'])}
 directories=['96x96/'+name for name in ['places','apps','devices','mimetypes']]
 index='[Icon Theme]\nName=Termux Jobs Kali-inspired\nInherits=Adwaita,hicolor\nDirectories='+','.join(directories)+'\n'
 for directory in directories:index+='\n['+directory+']\nSize=96\nType=Scalable\nMinSize=16\nMaxSize=256\nContext='+{'apps':'Applications','mimetypes':'MimeTypes','places':'Places','devices':'Devices'}[directory.split('/')[-1]]+'\n'
 entries['icons/index.theme']=index.encode()
 for kind,(directory,names) in groups.items():
  data=icon(kind)
  for name in names:entries['icons/96x96/'+directory+'/'+name+'.png']=data

def button_png(name,state):
 s=surface(0,34,34);c=context(s)
 inactive=state=='inactive'
 top,bottom=((.12,.16,.21),(.08,.11,.16)) if inactive else ((.13,.20,.28),(.09,.14,.21))
 if name=='close' and not inactive:top,bottom=(.05,.57,1),(.025,.36,.90)
 if state=='prelight':top,bottom=(.12,.64,1),(.04,.40,.85)
 if state=='pressed':top,bottom=(.025,.23,.47),(.03,.18,.35)
 arc(c,17,17,14.5,0,6.2832);gradient(c,top,bottom,29,2.5);fill_preserve(c);set_source_rgba(c,.34,.52,.72,.45);set_line_width(c,1);stroke(c)
 bind('set_line_cap',[ptr,integer])(c,1);set_line_width(c,1.8);set_source_rgb(c,*((.48,.56,.65) if inactive else (.93,.97,1)))
 if name=='close':
  move_to(c,12,12);line_to(c,22,22);move_to(c,22,12);line_to(c,12,22)
 elif name=='hide':move_to(c,11,21);line_to(c,23,21)
 elif name.startswith('maximize'):
  rectangle(c,11,11,12,12)
  if name.endswith('toggled'):move_to(c,14,9);line_to(c,25,9);line_to(c,25,20)
 elif name.startswith('shade'):move_to(c,11,20);line_to(c,17,13);line_to(c,23,20)
 else:move_to(c,17,10);line_to(c,17,24);move_to(c,11,15);line_to(c,23,15)
 stroke(c);result=png(s);destroy(c);surface_destroy(s);return result

def main():
 entries={'wallpaper.png':wallpaper()}
 css='''@import url("resource:///org/gtk/libgtk/theme/Adwaita/gtk-contained-dark.css");
@define-color theme_bg_color #141c26;
@define-color theme_fg_color #dce5ef;
@define-color theme_base_color #0d141e;
@define-color theme_text_color #dce5ef;
@define-color theme_selected_bg_color #125cbd;
@define-color theme_selected_fg_color #ffffff;
window, .background { background-color: #141c26; color: #dce5ef; }
entry, textview, textview text, treeview.view, iconview.view { background-color: #0d141e; color: #dce5ef; }
button { min-height: 28px; padding: 5px 9px; border: 1px solid #344353; border-radius: 6px; color: #dce5ef; background-image: linear-gradient(to bottom, #273340, #1c2631); box-shadow: none; }
button:hover { border-color: #168bff; background-image: linear-gradient(to bottom, #314358, #24374b); }
button:active, button:checked { background-image: linear-gradient(to bottom, #165cba, #124b96); border-color: #168bff; color: #ffffff; }
entry { min-height: 30px; border-radius: 5px; border: 1px solid #344353; }
entry:focus { border-color: #168bff; }
menu, .menu, popover { background-color: #111a25; color: #dce5ef; border-color: #344353; }
menuitem:hover, menuitem:selected, selection, *:selected { background-color: #125cbd; color: #ffffff; }
scrollbar slider { min-width: 11px; min-height: 11px; background-color: #3c5068; border: 1px solid #48607a; border-radius: 5px; }
.sidebar { background-color: #141f2c; color: #c9d5e3; }
.xfce4-panel { background-image: linear-gradient(to bottom, #18222d, #101820); color: #dce5ef; border-top: 1px solid #324254; }
.xfce4-panel button { color: #dce5ef; background-image: none; background-color: transparent; border-color: transparent; box-shadow: none; border-radius: 6px; padding: 4px 7px; }
.xfce4-panel button:hover { background-color: #24374b; border-color: #168bff; }
.xfce4-panel .tasklist button, .xfce4-panel .tasklist .toggle { background-color: #1c2a3a; border: 1px solid transparent; }
.xfce4-panel .tasklist button:checked { background-color: #17365a; border-bottom: 3px solid #168bff; }
#applicationmenu-button, #whiskermenu-button { background-image: linear-gradient(to bottom, #263240, #19232e); border: 1px solid #3f5369; border-radius: 10px; font-weight: normal; font-style: normal; padding: 5px 11px; }
#applicationmenu-button:hover, #whiskermenu-button:hover { border-color: #168bff; background-color: #24374b; }
#whiskermenu-window { padding-bottom: 12px; }
.xfce4-panel label { color: #dce5ef; text-shadow: none; }
.xfdesktop-icon-view { color: #ffffff; text-shadow: 1px 2px #030b15; }
'''
 entries['theme/gtk-3.0/gtk.css']=css.encode();entries['theme/index.theme']=b'[Desktop Entry]\nName=Termux Jobs Kali-inspired\nType=X-GNOME-Metatheme\n'
 # GTK2 receives a self-contained native-engine fallback, without extra theme packages.
 entries['theme/gtk-2.0/gtkrc']=b'''style "jobs-kali" {
 bg[NORMAL]="#141c26"
 fg[NORMAL]="#dce5ef"
 base[NORMAL]="#0d141e"
 text[NORMAL]="#dce5ef"
 bg[SELECTED]="#125cbd"
 fg[SELECTED]="#ffffff"
 base[SELECTED]="#125cbd"
 text[SELECTED]="#ffffff"
 xthickness=2
 ythickness=2
 font_name="DejaVu Sans 12"
}
class "GtkWidget" style "jobs-kali"
'''
 entries['theme/xfwm4/themerc']=b'active_text_color=#ffffff\ninactive_text_color=#7c8b9c\nbutton_offset=5\nbutton_spacing=3\nfull_width_title=true\nshow_app_icon=true\ntitle_shadow_active=false\ntitle_shadow_inactive=false\ntitle_vertical_offset_active=0\ntitle_vertical_offset_inactive=0\n'
 def xpm(w,h,rows,palette):
  colors=''.join('"'+key+' c '+value+'",\n' for key,value in palette.items())
  return ('/* XPM */\nstatic char *image[] = {\n"%d %d %d 1",\n'%(w,h,len(palette))+colors+',\n'.join('"'+row+'"' for row in rows)+'};\n').encode()
 def blue(y,height,inactive=False):
  if y==0:return '#168bff' if not inactive else '#344353'
  return '#161f2a' if not inactive else '#111923'
 keys='ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
 for state in ['active','inactive']:
  for name in ['title-1','title-2','title-3','title-4','title-5']:
   palette={keys[y]:blue(y,44,state=='inactive') for y in range(44)}
   entries['theme/xfwm4/'+name+'-'+state+'.xpm']=xpm(8,44,[keys[y]*8 for y in range(44)],palette)
  for name in ['top-left','top-right']:
   palette={keys[y]:blue(y,44,state=='inactive') for y in range(44)};palette.update({' ':'None','!':'#168bff' if state=='active' else '#344353'})
   rows=[]
   for y in range(44):
    row=''
    for x in range(12):
     rounded=(6-y if y<2 else 3-y if y<4 else 0)
     row+=' ' if x<rounded else '!' if x==rounded else keys[y]
    rows.append(row if name=='top-left' else row[::-1])
   entries['theme/xfwm4/'+name+'-'+state+'.xpm']=xpm(12,44,rows,palette)
  palette={'b':'#161f2a','h':'#161f2a','o':'#168bff' if state=='active' else '#344353','i':'#161f2a'}
  # XFWM repeats these along the long axis. Constant rows/columns avoid dashed seams.
  entries['theme/xfwm4/left-'+state+'.xpm']=xpm(6,1,['ohbbbi'],palette)
  entries['theme/xfwm4/right-'+state+'.xpm']=xpm(6,1,['ibbbho'],palette)
  entries['theme/xfwm4/bottom-'+state+'.xpm']=xpm(1,6,['i','b','b','b','h','o'],palette)
  for name in ['bottom-left','bottom-right']:
   rows=['ohbbbi','ohbbbb','ohbbbb','ohbbbb','ohhhhh','oooooo']
   entries['theme/xfwm4/'+name+'-'+state+'.xpm']=xpm(6,6,rows if name=='bottom-left' else [row[::-1] for row in rows],palette)
 for name in ['close','hide','maximize','maximize-toggled','menu','shade','shade-toggled','stick','stick-toggled']:
  for state in ['active','inactive','prelight','pressed']:
   # XFWM composites the PNG onto this transparent XPM canvas. No old opaque box.
   entries['theme/xfwm4/'+name+'-'+state+'.xpm']=xpm(34,34,[' '*34 for _ in range(34)],{' ':'None'})
   if name!='menu':entries['theme/xfwm4/'+name+'-'+state+'.png']=button_png(name,state)
 add_icons(entries)
 aliases={'arrow':'left_ptr default arrow top_left_arrow dnd-none copy alias link 08e8e1c95fe2fc01f976f1e063a24ccd','right':'right_ptr','hand':'pointer hand hand1 hand2 grab grabbing 9d800788f1b08800ae810202380a0822 e29285e634086352946a0e7090d73106','text':'text xterm vertical-text','h':'ew-resize col-resize sb_h_double_arrow h_double_arrow left_side right_side','v':'ns-resize row-resize sb_v_double_arrow v_double_arrow top_side bottom_side','nw':'nwse-resize top_left_corner bottom_right_corner bd_double_arrow','ne':'nesw-resize top_right_corner bottom_left_corner fd_double_arrow','move':'move fleur all-scroll size_all','cross':'crosshair cross cross_reverse diamond_cross cell plus','wait':'wait watch','progress':'progress left_ptr_watch','help':'help question_arrow','deny':'not-allowed forbidden no-drop crossed_circle X_cursor'}
 entries['cursor/index.theme']=b'[Icon Theme]\nName=Termux Jobs Cursor\nInherits=Adwaita\n'
 for kind,names in aliases.items():
  data=cursor(kind)
  for name in names.split():entries['cursor/cursors/'+name]=data
 out=ROOT/'.tools/x11-runtime/assets';out.mkdir(parents=True,exist_ok=True)
 with zipfile.ZipFile(out/'x11-style.zip','w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
  for name,data in sorted(entries.items()):
   info=zipfile.ZipInfo(name,date_time=(2026,1,1,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;info.external_attr=0o100600<<16;z.writestr(info,data)
 file=out/'x11-style.zip';print(json.dumps({'sha256':hashlib.sha256(file.read_bytes()).hexdigest(),'bytes':file.stat().st_size,'files':len(entries)}))
if __name__=='__main__':main()
