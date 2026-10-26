import ctypes as C,ctypes.util,hashlib,importlib.util,json,os,stat,subprocess,sys,tempfile,unittest,zipfile
from pathlib import Path
from unittest.mock import patch
import xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('style',ROOT/'x11/src/main/assets/x11-style.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
class StyleTests(unittest.TestCase):
 def setUp(self):
  self.tmp=tempfile.TemporaryDirectory(dir=ROOT/'.verification');self.root=Path(self.tmp.name);self.home=self.root/'home';self.home.mkdir();self.prefix=self.root/'usr';self.prefix.mkdir();self.control=self.root/'control';self.control.mkdir();(self.control/'x11-style.zip').write_bytes((ROOT/'.tools/x11-components/x11-style.zip').read_bytes());self.config=self.home/'.config';self.channels=self.config/'xfce4/xfconf/xfce-perchannel-xml';self.env=patch.dict(os.environ,{'XDG_CONFIG_HOME':str(self.config)});self.env.start()
 def tearDown(self):self.env.stop();self.tmp.cleanup()
 def channel(self,name):return m.tree(m.read(self.channels/(name+'.xml')),name)
 def put(self,name,key,typ,value):
  self.channels.mkdir(parents=True,exist_ok=True);root=self.channel(name);m.set_node(root,key,m.prop(key,typ,value));(self.channels/(name+'.xml')).write_bytes(ET.tostring(root))
 def legacy_touch(self):
  # Persist the previous schema exactly as an older app would; no retired pack
  # or executable is needed by the new APK or by this migration fixture.
  values=[('xfwm4','/general/workspace_count','int',4,3),('xsettings','/Net/DoubleClickTime','int',330,500),('xfce4-panel','/panels/panel-1/autohide-behavior','uint',0,2)]
  self.put('xfce4-panel','/panels/panel-1/position','string','p=6;x=0;y=0')
  for channel,key,typ,before,after in values:self.put(channel,key,typ,before)
  m.apply(self.home,self.prefix,self.control,True)
  record=self.home/'.local/state/termux-jobs/style/previous.json';state=json.loads(record.read_text())
  for channel,key,typ,before,after in values:
   identity=channel+'|'+key;state['original'].setdefault(identity,m.prop(key,typ,before));state['applied'][identity]=m.prop(key,typ,after);self.put(channel,key,typ,after)
  asset=self.home/'.local/share/termux-jobs/style/wallpaper.png';asset.write_bytes(b'old tracked wallpaper');state['files'][str(asset.relative_to(self.home))]=hashlib.sha256(asset.read_bytes()).hexdigest()
  record.write_text(json.dumps(state))
 def test_retired_touch_state_restores_panels_timing_and_workspace_count_when_switching_to_kali(self):
  self.legacy_touch();m.apply(self.home,self.prefix,self.control,True)
  for channel,key,value in [('xfwm4','/general/workspace_count',4),('xsettings','/Net/DoubleClickTime',330),('xfce4-panel','/panels/panel-1/autohide-behavior',0)]:self.assertIn('value="'+str(value)+'"',m.snapshot(self.channel(channel),key))
  with zipfile.ZipFile(self.control/'x11-style.zip') as z:self.assertEqual(z.read('wallpaper.png'),(self.home/'.local/share/termux-jobs/style/wallpaper.png').read_bytes())
  m.apply(self.home,self.prefix,self.control,True);m.apply(self.home,self.prefix,self.control,False)
  self.assertIn('value="330"',m.snapshot(self.channel('xsettings'),'/Net/DoubleClickTime'))
 def test_retired_touch_migration_preserves_later_manual_changes(self):
  self.legacy_touch();self.put('xfwm4','/general/workspace_count','int',7);self.put('xsettings','/Net/DoubleClickTime','int',700)
  m.apply(self.home,self.prefix,self.control,True);m.apply(self.home,self.prefix,self.control,False)
  self.assertIn('value="7"',m.snapshot(self.channel('xfwm4'),'/general/workspace_count'));self.assertIn('value="700"',m.snapshot(self.channel('xsettings'),'/Net/DoubleClickTime'))
 def test_disabled_without_prior_style_does_not_write_anything(self):
  self.assertEqual('off',m.apply(self.home,self.prefix,self.control,False));self.assertEqual([],list(self.home.iterdir()))
 def test_bundle_pin_and_static_cursor_format_are_valid(self):
  pack=(self.control/'x11-style.zip').read_bytes();self.assertEqual(m.PIN,hashlib.sha256(pack).hexdigest())
  lib=C.CDLL(ctypes.util.find_library('Xcursor'));lib.XcursorFilenameLoadImages.argtypes=[C.c_char_p,C.c_int];lib.XcursorFilenameLoadImages.restype=C.c_void_p;lib.XcursorImagesDestroy.argtypes=[C.c_void_p]
  with zipfile.ZipFile(self.control/'x11-style.zip') as z:
   self.assertTrue(z.read('wallpaper.png').startswith(b'\x89PNG'));self.assertGreater(len(z.namelist()),100)
   for name in ['left_ptr','text','pointer','ew-resize','ns-resize','nwse-resize','nesw-resize','wait']:
    data=z.read('cursor/cursors/'+name);self.assertEqual(b'Xcur',data[:4]);file=self.root/(name+'.cursor');file.write_bytes(data);images=lib.XcursorFilenameLoadImages(str(file).encode(),32);self.assertTrue(images,name);lib.XcursorImagesDestroy(images)
 def test_enable_then_disable_restores_original_properties_and_other_settings(self):
  self.put('xsettings','/Net/ThemeName','string','MyTheme');self.put('xfce4-desktop','/backdrop/screen0/monitorscreen/workspace0/last-image','string','/my-picture.png');self.put('xsettings','/Gtk/DoubleClickTime','int',450)
  self.assertEqual('on',m.apply(self.home,self.prefix,self.control,True));self.assertIn('TermuxJobsStyle',m.snapshot(self.channel('xsettings'),'/Net/ThemeName'));self.assertIn('74',m.snapshot(self.channel('xfce4-desktop'),'/desktop-icons/icon-size'))
  self.assertEqual('restored',m.apply(self.home,self.prefix,self.control,False));self.assertIn('MyTheme',m.snapshot(self.channel('xsettings'),'/Net/ThemeName'));self.assertIn('/my-picture.png',m.snapshot(self.channel('xfce4-desktop'),'/backdrop/screen0/monitorscreen/workspace0/last-image'));self.assertIn('450',m.snapshot(self.channel('xsettings'),'/Gtk/DoubleClickTime'));self.assertIsNone(m.snapshot(self.channel('xfce4-desktop'),'/desktop-icons/icon-size'));self.assertEqual('off',m.apply(self.home,self.prefix,self.control,False))
 def test_runtime_xml_formatting_does_not_prevent_restore_and_manual_edits_win(self):
  self.put('xsettings','/Net/ThemeName','string','Before');m.apply(self.home,self.prefix,self.control,True)
  path=self.channels/'xsettings.xml';root=self.channel('xsettings');ET.indent(root);path.write_bytes(ET.tostring(root));self.put('xsettings','/Gtk/FontName','string','User changed 13');m.apply(self.home,self.prefix,self.control,False);self.assertIn('Before',m.snapshot(self.channel('xsettings'),'/Net/ThemeName'));self.assertIn('User changed 13',m.snapshot(self.channel('xsettings'),'/Gtk/FontName'))
 def test_repeated_enabled_launch_keeps_first_original_and_assets_are_not_rewritten(self):
  self.put('xsettings','/Net/ThemeName','string','First');m.apply(self.home,self.prefix,self.control,True);asset=self.home/'.local/share/termux-jobs/style/wallpaper.png';inode=asset.stat().st_ino;m.apply(self.home,self.prefix,self.control,True);self.assertEqual(inode,asset.stat().st_ino);m.apply(self.home,self.prefix,self.control,False);self.assertIn('First',m.snapshot(self.channel('xsettings'),'/Net/ThemeName'))
 def test_clean_panel_is_seeded_from_own_prefix_and_restores_original_size(self):
  seed=self.prefix/'etc/xdg/xfce4/panel/default.xml';seed.parent.mkdir(parents=True);seed.write_text('<channel name="xfce4-panel" version="1.0"><property name="panels" type="array"><value type="int" value="1"/><property name="panel-1" type="empty"><property name="position" type="string" value="p=6;x=0;y=0"/><property name="size" type="uint" value="26"/></property></property></channel>');m.apply(self.home,self.prefix,self.control,True);self.assertIn('44',m.snapshot(self.channel('xfce4-panel'),'/panels/panel-1/size'));m.apply(self.home,self.prefix,self.control,False);self.assertIn('26',m.snapshot(self.channel('xfce4-panel'),'/panels/panel-1/size'))
 def test_symlink_or_corrupt_config_is_preserved_without_appearance_changes(self):
  self.channels.mkdir(parents=True);victim=self.root/'keep';victim.write_text('keep');(self.channels/'xsettings.xml').symlink_to(victim)
  with self.assertRaises(ValueError):m.apply(self.home,self.prefix,self.control,True)
  self.assertEqual('keep',victim.read_text());self.assertFalse((self.home/'.local/state/termux-jobs/style/previous.json').exists());(self.channels/'xsettings.xml').unlink();(self.channels/'xsettings.xml').write_text('<invalid>')
  with self.assertRaises(ET.ParseError):m.apply(self.home,self.prefix,self.control,True)
 def test_foreign_theme_assets_are_not_overwritten(self):
  path=self.home/'.themes/TermuxJobsStyle/gtk-3.0/gtk.css';path.parent.mkdir(parents=True);path.write_text('user theme')
  with self.assertRaises(ValueError):m.apply(self.home,self.prefix,self.control,True)
  self.assertEqual('user theme',path.read_text());self.assertFalse((self.home/'.local/state/termux-jobs/style/previous.json').exists())
 def test_failed_asset_write_can_be_retried_and_originals_are_restorable(self):
  self.put('xsettings','/Net/ThemeName','string','Before');original=m.write;failed=[False]
  def write(path,data,before):
   if path.suffix=='.xpm' and not failed[0]:failed[0]=True;raise OSError('injected')
   return original(path,data,before)
  with patch.object(m,'write',side_effect=write),self.assertRaises(OSError):m.apply(self.home,self.prefix,self.control,True)
  m.apply(self.home,self.prefix,self.control,True);m.apply(self.home,self.prefix,self.control,False);self.assertIn('Before',m.snapshot(self.channel('xsettings'),'/Net/ThemeName'))
 def test_style_scope_and_selected_prefix_never_uses_termux_prefix_for_guests(self):
  self.assertEqual(self.prefix,m.environment_prefix('termux',str(self.prefix)))
  for target in ['ubuntu','debian','alpine','fedora','opensuse','void','archlinux']:self.assertEqual(Path('/'),m.environment_prefix(target,str(self.prefix)))
  for target in ['ubuntu-ai','jobs-ai-ubuntu','jobs-x11-ubuntu','unknown','../../ubuntu',None]:
   with self.assertRaises(ValueError):m.environment_prefix(target,str(self.prefix))
  session=(ROOT/'x11/src/main/assets/x11-session.sh').read_text();self.assertLess(session.index('x11-style.py'),session.index('x11-wallpaper.py'));self.assertIn('XCURSOR_THEME=TermuxJobsCursor',session)
 def test_every_guest_cli_changes_only_its_home_and_can_restore_without_host_prefix(self):
  self.put('xsettings','/Net/ThemeName','string','TermuxPersonal');host_before=(self.channels/'xsettings.xml').read_bytes()
  seed=self.prefix/'etc/xdg/xfce4/panel/default.xml';seed.parent.mkdir(parents=True);seed.write_text('host-only-invalid-xml');observed={}
  for target in ['ubuntu','debian','alpine','fedora','opensuse','void','archlinux']:
   with self.subTest(target=target):
    guest=self.root/(target+'-home');guest.mkdir();guest_config=guest/'.config';channels=guest_config/'xfce4/xfconf/xfce-perchannel-xml';channels.mkdir(parents=True)
    personal=('<channel name="xsettings" version="1.0"><property name="Net" type="empty"><property name="ThemeName" type="string" value="'+target+'Personal"/></property></channel>').encode();(channels/'xsettings.xml').write_bytes(personal)
    env=dict(os.environ,HOME=str(guest),XDG_CONFIG_HOME=str(guest_config),PREFIX=str(self.prefix),JOBS_X11_TARGET=target,JOBS_X11_CONTROL=str(self.control))
    for flag,expected in [('1','on'),('1','on'),('0','restored')]:
     result=subprocess.run([sys.executable,'-B',str(ROOT/'x11/src/main/assets/x11-style.py'),flag],env=env,capture_output=True,text=True,timeout=10)
     self.assertEqual(0,result.returncode,result.stdout+result.stderr);self.assertIn('JOBS_X11_STYLE='+expected,result.stdout)
     root=m.tree((channels/'xsettings.xml').read_bytes(),'xsettings');self.assertIn(target+'Personal' if flag=='0' else 'TermuxJobsStyle',m.snapshot(root,'/Net/ThemeName'))
     self.assertEqual(host_before,(self.channels/'xsettings.xml').read_bytes());self.assertEqual('host-only-invalid-xml',seed.read_text())
     for other,data in observed.items():self.assertEqual(data,other.read_bytes())
    self.assertTrue((guest/'.themes/TermuxJobsStyle/gtk-3.0/gtk.css').is_file());self.assertFalse((self.home/'.themes').exists());observed[channels/'xsettings.xml']=(channels/'xsettings.xml').read_bytes()
 def test_enabled_style_rejects_missing_helper_and_unsupported_guests_before_launch(self):
  for target,error in [(t,'STYLE_MISSING') for t in ['termux','ubuntu','debian','alpine','fedora','opensuse','void','archlinux']]+[('jobs-x11-ubuntu','STYLE_TARGET'),('jobs-ai-ubuntu','STYLE_TARGET')]:
   env=dict(os.environ,JOBS_X11_FOCUS_TOKEN='12345678-1234-1234-1234-123456789abc',JOBS_X11_STYLE='1',JOBS_X11_TARGET=target,JOBS_X11_CONTROL=str(self.root/'missing'))
   result=subprocess.run(['/bin/sh',str(ROOT/'x11/src/main/assets/x11-session.sh')],env=env,capture_output=True,text=True,timeout=3)
   self.assertNotEqual(0,result.returncode);self.assertIn('ERROR:'+error,result.stdout);self.assertNotIn('JOBS_X11_STARTUP',result.stdout)

 def test_bottom_panel_is_large_visible_full_width_and_menu_title_restores(self):
  for key,typ,value in [('position','string','p=10;x=0;y=0'),('size','uint',48),('icon-size','uint',24),('length','uint',1),('autohide-behavior','uint',1)]:self.put('xfce4-panel','/panels/panel-2/'+key,typ,value)
  self.put('xfce4-panel','/plugins/plugin-41','string','applicationsmenu');self.put('xfce4-panel','/plugins/plugin-41/button-title','string','Applications');self.put('xfce4-panel','/plugins/plugin-42','string','clock')
  m.apply(self.home,self.prefix,self.control,True);panel=self.channel('xfce4-panel')
  for key,value in [('size','81'),('icon-size','52'),('length','100'),('autohide-behavior','0')]:self.assertEqual(value,m.node(panel,'/panels/panel-2/'+key)[1].get('value'))
  self.assertEqual('Menu',m.node(panel,'/plugins/plugin-41/button-title')[1].get('value'));self.assertEqual('clock',m.node(panel,'/plugins/plugin-42')[1].get('value'))
  m.apply(self.home,self.prefix,self.control,False);panel=self.channel('xfce4-panel');self.assertEqual('48',m.node(panel,'/panels/panel-2/size')[1].get('value'));self.assertEqual('Applications',m.node(panel,'/plugins/plugin-41/button-title')[1].get('value'));self.assertIsNone(m.node(panel,'/panels/panel-2/background-style')[1])
 def test_old_style_assets_upgrade_and_original_theme_survives_new_assets(self):
  import io
  newpack=(self.control/'x11-style.zip').read_bytes();old=io.BytesIO()
  with zipfile.ZipFile(io.BytesIO(newpack)) as source,zipfile.ZipFile(old,'w',zipfile.ZIP_DEFLATED) as dest:
   for name in source.namelist():
    if not name.startswith('icons/'):dest.writestr(name,b'old dark style' if name=='theme/gtk-3.0/gtk.css' else source.read(name))
  (self.control/'x11-style.zip').write_bytes(old.getvalue());self.put('xsettings','/Net/ThemeName','string','PersonalTheme');self.put('xsettings','/Net/IconThemeName','string','PersonalIcons')
  previous={'xsettings|/Net/ThemeName':m.prop('/Net/ThemeName','string','TermuxJobsStyle'),'xsettings|/Net/IconThemeName':m.prop('/Net/IconThemeName','string','Adwaita')}
  with patch.object(m,'PIN',hashlib.sha256(old.getvalue()).hexdigest()),patch.object(m,'desired',return_value=previous):m.apply(self.home,self.prefix,self.control,True)
  (self.control/'x11-style.zip').write_bytes(newpack);m.apply(self.home,self.prefix,self.control,True)
  self.assertIn('TermuxJobsIcons',m.snapshot(self.channel('xsettings'),'/Net/IconThemeName'));self.assertIn(b'#141c26',(self.home/'.themes/TermuxJobsStyle/gtk-3.0/gtk.css').read_bytes())
  m.apply(self.home,self.prefix,self.control,False);self.assertIn('PersonalTheme',m.snapshot(self.channel('xsettings'),'/Net/ThemeName'));self.assertIn('PersonalIcons',m.snapshot(self.channel('xsettings'),'/Net/IconThemeName'))
 def test_all_xpm_frames_and_png_icons_load_in_real_pixbuf_without_a_desktop(self):
  pix=C.CDLL(ctypes.util.find_library('gdk_pixbuf-2.0'));obj=C.CDLL(ctypes.util.find_library('gobject-2.0'))
  pix.gdk_pixbuf_new_from_file.argtypes=[C.c_char_p,C.c_void_p];pix.gdk_pixbuf_new_from_file.restype=C.c_void_p;pix.gdk_pixbuf_get_width.argtypes=[C.c_void_p];pix.gdk_pixbuf_get_width.restype=C.c_int;obj.g_object_unref.argtypes=[C.c_void_p]
  with zipfile.ZipFile(self.control/'x11-style.zip') as z:
   names=[n for n in z.namelist() if n.endswith(('.xpm','.png'))];self.assertGreater(len(names),70)
   for i,name in enumerate(names):
    file=self.root/('asset-'+str(i)+Path(name).suffix);file.write_bytes(z.read(name));image=pix.gdk_pixbuf_new_from_file(str(file).encode(),None);self.assertTrue(image,name)
    if name.startswith('icons/'):self.assertEqual(96,pix.gdk_pixbuf_get_width(image))
    obj.g_object_unref(image)
 def test_corrupt_bundle_cannot_replace_existing_theme(self):
  self.put('xsettings','/Net/ThemeName','string','Personal');(self.control/'x11-style.zip').write_bytes(b'bad zip')
  with self.assertRaisesRegex(ValueError,'integrity'):m.apply(self.home,self.prefix,self.control,True)
  self.assertIn('Personal',m.snapshot(self.channel('xsettings'),'/Net/ThemeName'));self.assertFalse((self.home/'.local/state/termux-jobs/style/previous.json').exists())

 def test_menu_moves_to_bottom_without_losing_plugins_and_restores_order(self):
  self.put('xfce4-panel','/panels/panel-1/position','string','p=6;x=0;y=0');self.put('xfce4-panel','/panels/panel-2/position','string','p=10;x=0;y=0');self.put('xfce4-panel','/plugins/plugin-41','string','applicationsmenu')
  root=self.channel('xfce4-panel')
  for key,values in [('/panels/panel-1/plugin-ids',['41','42']),('/panels/panel-2/plugin-ids',['51','52'])]:
   n=ET.Element('property',name='plugin-ids',type='array')
   for value in values:ET.SubElement(n,'value',type='int',value=value)
   m.set_node(root,key,m.canonical(n))
  (self.channels/'xfce4-panel.xml').write_bytes(ET.tostring(root));m.apply(self.home,self.prefix,self.control,True)
  def ids(name):return [x.get('value') for x in m.node(self.channel('xfce4-panel'),'/panels/'+name+'/plugin-ids')[1].findall('value')]
  self.assertEqual(['42'],ids('panel-1'));self.assertEqual(['41','51','52'],ids('panel-2'));m.apply(self.home,self.prefix,self.control,True);self.assertEqual(['41','51','52'],ids('panel-2'));m.apply(self.home,self.prefix,self.control,False);self.assertEqual(['41','42'],ids('panel-1'));self.assertEqual(['51','52'],ids('panel-2'))

 def test_menu_icon_changes_only_its_button_and_restores_previous_icon(self):
  self.put('xfce4-panel','/plugins/plugin-7','string','applicationsmenu');self.put('xfce4-panel','/plugins/plugin-7/button-icon','string','org.xfce.panel.applicationsmenu');self.put('xfce4-panel','/plugins/plugin-7/custom-menu','bool',True);self.put('xfce4-panel','/plugins/plugin-7/custom-menu-file','string','/custom/menu.xml');self.put('xfce4-panel','/plugins/plugin-7/show-menu-icons','bool',False)
  m.apply(self.home,self.prefix,self.control,True);p=self.channel('xfce4-panel');self.assertEqual('termux-jobs-menu',m.node(p,'/plugins/plugin-7/button-icon')[1].get('value'));self.assertEqual('/custom/menu.xml',m.node(p,'/plugins/plugin-7/custom-menu-file')[1].get('value'));self.assertEqual('true',m.node(p,'/plugins/plugin-7/custom-menu')[1].get('value'));self.assertEqual('false',m.node(p,'/plugins/plugin-7/show-menu-icons')[1].get('value'))
  self.assertTrue((self.home/'.icons/TermuxJobsIcons/96x96/apps/termux-jobs-menu.png').is_file());m.apply(self.home,self.prefix,self.control,False);self.assertIn('org.xfce.panel.applicationsmenu',m.snapshot(self.channel('xfce4-panel'),'/plugins/plugin-7/button-icon'))
 def test_border_tiles_are_continuous_and_controls_have_no_square_canvas(self):
  import re
  def image(z,name):
   strings=re.findall(r'"([^"\n]*)"',z.read('theme/xfwm4/'+name+'.xpm').decode());w,h,colors,cpp=map(int,strings[0].split());self.assertEqual(1,cpp);palette={s[0]:s[4:] for s in strings[1:1+colors]};return w,h,palette,strings[1+colors:]
  with zipfile.ZipFile(self.control/'x11-style.zip') as z:
   for state in ['active','inactive']:
    for side in ['left','right']:
     w,h,palette,rows=image(z,side+'-'+state);self.assertEqual(1,h);self.assertEqual(6,w);self.assertEqual(rows[0],(rows*100)[99])
    w,h,palette,rows=image(z,'bottom-'+state);self.assertEqual(1,w);self.assertEqual(6,h)
   for state in ['active','inactive','prelight','pressed']:
    w,h,palette,rows=image(z,'menu-'+state);self.assertTrue(all(palette[p]=='None' for row in rows for p in row))
    for name in ['close','hide','maximize']:
     w,h,palette,rows=image(z,name+'-'+state);self.assertTrue(all(palette[p]=='None' for row in rows for p in row));self.assertIn('theme/xfwm4/'+name+'-'+state+'.png',z.namelist())

 def test_png_controls_composite_without_square_background_and_keep_antialiased_edges(self):
  pix=C.CDLL(ctypes.util.find_library('gdk_pixbuf-2.0'));obj=C.CDLL(ctypes.util.find_library('gobject-2.0'))
  pix.gdk_pixbuf_new_from_file.argtypes=[C.c_char_p,C.c_void_p];pix.gdk_pixbuf_new_from_file.restype=C.c_void_p;pix.gdk_pixbuf_composite.argtypes=[C.c_void_p,C.c_void_p,C.c_int,C.c_int,C.c_int,C.c_int,C.c_double,C.c_double,C.c_double,C.c_double,C.c_int,C.c_int];pix.gdk_pixbuf_get_pixels.argtypes=[C.c_void_p];pix.gdk_pixbuf_get_pixels.restype=C.c_void_p;pix.gdk_pixbuf_get_rowstride.argtypes=[C.c_void_p];pix.gdk_pixbuf_get_rowstride.restype=C.c_int;pix.gdk_pixbuf_get_n_channels.argtypes=[C.c_void_p];pix.gdk_pixbuf_get_n_channels.restype=C.c_int;obj.g_object_unref.argtypes=[C.c_void_p]
  with zipfile.ZipFile(self.control/'x11-style.zip') as z:
   for name in ['close','hide','maximize','maximize-toggled']:
    for state in ['active','inactive','prelight','pressed']:
     prefix='theme/xfwm4/'+name+'-'+state;base=self.root/'control.xpm';overlay=self.root/'control.png';base.write_bytes(z.read(prefix+'.xpm'));overlay.write_bytes(z.read(prefix+'.png'));dest=pix.gdk_pixbuf_new_from_file(str(base).encode(),None);src=pix.gdk_pixbuf_new_from_file(str(overlay).encode(),None);self.assertTrue(dest);self.assertTrue(src)
     pix.gdk_pixbuf_composite(src,dest,0,0,34,34,0,0,1,1,2,255);stride=pix.gdk_pixbuf_get_rowstride(dest);channels=pix.gdk_pixbuf_get_n_channels(dest);self.assertEqual(4,channels);data=C.string_at(pix.gdk_pixbuf_get_pixels(dest),stride*34);alpha=lambda x,y:data[y*stride+x*channels+3]
     for x,y in [(0,0),(2,2),(31,2),(2,31),(31,31)]:self.assertEqual(0,alpha(x,y),(name,state,x,y))
     self.assertEqual(255,alpha(17,17));self.assertTrue(any(0<alpha(x,y)<255 for y in range(34) for x in range(34)),(name,state));obj.g_object_unref(src);obj.g_object_unref(dest)
 def test_kali_style_is_dark_blue_and_keeps_native_window_controls_reversible(self):
  self.put('xfwm4','/general/button_layout','string','O|SHMC');self.put('xfwm4','/general/title_alignment','string','center');self.put('xsettings','/Gtk/FontName','string','Personal Font 11');m.apply(self.home,self.prefix,self.control,True)
  self.assertIn('O|HMC',m.snapshot(self.channel('xfwm4'),'/general/button_layout'));self.assertIn('left',m.snapshot(self.channel('xfwm4'),'/general/title_alignment'))
  with zipfile.ZipFile(self.control/'x11-style.zip') as z:
   self.assertIn(b'gtk-contained-dark.css',z.read('theme/gtk-3.0/gtk.css'));self.assertIn(b'#141c26',z.read('theme/gtk-3.0/gtk.css'));self.assertNotIn(b'#ece9d8',z.read('theme/gtk-3.0/gtk.css'));self.assertIn(b'Kali-inspired',z.read('icons/index.theme'))
   self.assertNotEqual(z.read('icons/96x96/places/folder-documents.png'),z.read('icons/96x96/places/folder-download.png'))
  m.apply(self.home,self.prefix,self.control,False);self.assertIn('O|SHMC',m.snapshot(self.channel('xfwm4'),'/general/button_layout'));self.assertIn('center',m.snapshot(self.channel('xfwm4'),'/general/title_alignment'));self.assertIn('Personal Font 11',m.snapshot(self.channel('xsettings'),'/Gtk/FontName'))

 def test_nonstandard_panel_and_whisker_menu_keep_their_plugins_and_restore(self):
  self.channels.mkdir(parents=True)
  xml=b'<channel name="xfce4-panel" version="1.0"><property name="panels" type="array"><value type="int" value="37"/><property name="panel-37" type="empty"><property name="position" type="string" value="p=10;x=0;y=0"/><property name="size" type="uint" value="32"/><property name="plugin-ids" type="array"><value type="int" value="83"/><value type="int" value="91"/></property></property></property><property name="plugins" type="empty"><property name="plugin-83" type="string" value="whiskermenu"><property name="title" type="string" value="Distribution menu"/></property><property name="plugin-91" type="string" value="clock"/></property></channel>'
  (self.channels/'xfce4-panel.xml').write_bytes(xml);original=self.channel('xfce4-panel');menu=m.snapshot(original,'/plugins/plugin-83');ids=m.snapshot(original,'/panels/panel-37/plugin-ids')
  for enabled in [True,True,False]:
   m.apply(self.home,self.prefix,self.control,enabled);panel=self.channel('xfce4-panel')
   self.assertEqual(menu,m.snapshot(panel,'/plugins/plugin-83'));self.assertEqual(ids,m.snapshot(panel,'/panels/panel-37/plugin-ids'));self.assertIn('clock',m.snapshot(panel,'/plugins/plugin-91'))
   self.assertEqual('81' if enabled else '32',m.node(panel,'/panels/panel-37/size')[1].get('value'))

 def test_system_channel_defaults_seed_a_clean_desktop_before_classic_fallback(self):
  seed=self.prefix/'etc/xdg/xfce4/xfconf/xfce-perchannel-xml/xfce4-panel.xml';seed.parent.mkdir(parents=True)
  seed.write_text('<channel name="xfce4-panel" version="1.0"><property name="panels" type="array"><value type="int" value="1"/><property name="panel-1" type="empty"><property name="position" type="string" value="p=10;x=0;y=0"/><property name="size" type="uint" value="24"/></property></property><property name="plugins" type="empty"><property name="plugin-8" type="string" value="whiskermenu"/></property></channel>')
  fallback=self.prefix/'etc/xdg/xfce4/panel/default.xml';fallback.parent.mkdir(parents=True);fallback.write_text('not XML; must not override the system channel')
  m.apply(self.home,self.prefix,self.control,True);root=self.channel('xfce4-panel');self.assertEqual('81',m.node(root,'/panels/panel-1/size')[1].get('value'));self.assertIn('whiskermenu',m.snapshot(root,'/plugins/plugin-8'))
  m.apply(self.home,self.prefix,self.control,False);self.assertEqual('24',m.node(self.channel('xfce4-panel'),'/panels/panel-1/size')[1].get('value'))
 def test_inherited_empty_local_properties_use_system_layout_without_copying_over_user_menu(self):
  seed=self.prefix/'etc/xdg/xfce4/xfconf/xfce-perchannel-xml/xfce4-panel.xml';seed.parent.mkdir(parents=True)
  seed.write_text('<channel name="xfce4-panel" version="1.0"><property name="panels" type="array"><value type="int" value="1"/><property name="panel-1" type="empty"><property name="position" type="string" value="p=10;x=0;y=0"/><property name="size" type="uint" value="24"/></property></property><property name="plugins" type="empty"><property name="plugin-1" type="string" value="whiskermenu"/></property></channel>')
  self.channels.mkdir(parents=True);(self.channels/'xfce4-panel.xml').write_text('<channel name="xfce4-panel" version="1.0"><property name="panels" type="empty"><property name="panel-1" type="empty"><property name="position" type="empty"/><property name="size" type="empty"/></property></property><property name="plugins" type="empty"><property name="plugin-1" type="empty"><property name="favorites" type="array"><value type="string" value="personal.desktop"/></property></property></property></channel>')
  before=self.channel('xfce4-panel');menu=m.snapshot(before,'/plugins');position=m.snapshot(before,'/panels/panel-1/position')
  for enabled in [True,True,False]:
   m.apply(self.home,self.prefix,self.control,enabled);root=self.channel('xfce4-panel');self.assertEqual(menu,m.snapshot(root,'/plugins'));self.assertEqual(position,m.snapshot(root,'/panels/panel-1/position'))
   size=m.node(root,'/panels/panel-1/size')[1];self.assertEqual('uint' if enabled else 'empty',size.get('type'))
   if enabled:self.assertEqual('81',size.get('value'))
 def test_explicit_user_layout_wins_over_inherited_bottom_panel(self):
  defaults=m.tree(b'<channel name="xfce4-panel"><property name="panels" type="array"><value type="int" value="1"/><property name="panel-1" type="empty"><property name="position" type="string" value="p=10;x=0;y=0"/><property name="plugin-ids" type="array"><value type="int" value="1"/></property></property></property></channel>','xfce4-panel')
  local=m.tree(b'<channel name="xfce4-panel"><property name="panels" type="empty"><property name="panel-1" type="empty"><property name="position" type="string" value="p=6;x=0;y=0"/><property name="plugin-ids" type="array"><value type="int" value="99"/></property></property></property></channel>','xfce4-panel')
  merged=m.inherited(defaults,local);self.assertEqual('p=6;x=0;y=0',m.node(merged,'/panels/panel-1/position')[1].get('value'));self.assertEqual(['99'],[n.get('value') for n in m.node(merged,'/panels/panel-1/plugin-ids')[1].findall('value')]);self.assertEqual('p=10;x=0;y=0',m.node(defaults,'/panels/panel-1/position')[1].get('value'))

 def test_eight_percent_metrics_upgrade_without_replacing_original_preferences(self):
  self.put('xsettings','/Gtk/FontName','string','Personal Font 10')
  self.put('xfce4-desktop','/desktop-icons/icon-size','uint',48)
  self.put('xfce4-panel','/panels/panel-1/position','string','p=10;x=0;y=0')
  self.put('xfce4-panel','/panels/panel-1/size','uint',32)
  previous=m.desired
  def old_metrics(roots,wallpaper):
   result=previous(roots,wallpaper)
   for ch,key,typ,value in [('xsettings','/Gtk/FontName','string','DejaVu Sans 13'),('xfce4-desktop','/desktop-icons/icon-size','uint',80),('xfce4-panel','/panels/panel-1/size','uint',88)]:result[ch+'|'+key]=m.prop(key,typ,value)
   return result
  with patch.object(m,'desired',side_effect=old_metrics):m.apply(self.home,self.prefix,self.control,True)
  m.apply(self.home,self.prefix,self.control,True)
  for ch,key,value in [('xsettings','/Gtk/FontName','DejaVu Sans 12'),('xfce4-desktop','/desktop-icons/icon-size','74'),('xfce4-panel','/panels/panel-1/size','81')]:self.assertEqual(value,m.node(self.channel(ch),key)[1].get('value'))
  m.apply(self.home,self.prefix,self.control,False)
  for ch,key,value in [('xsettings','/Gtk/FontName','Personal Font 10'),('xfce4-desktop','/desktop-icons/icon-size','48'),('xfce4-panel','/panels/panel-1/size','32')]:self.assertEqual(value,m.node(self.channel(ch),key)[1].get('value'))
 def test_menu_clearance_is_scoped_to_whisker_and_font_matches_gtk3(self):
  with zipfile.ZipFile(self.control/'x11-style.zip') as z:
   css=z.read('theme/gtk-3.0/gtk.css')
   self.assertIn(b'#whiskermenu-window { padding-bottom: 12px; }',css)
   self.assertIn(b'font_name="DejaVu Sans 12"',z.read('theme/gtk-2.0/gtkrc'))
