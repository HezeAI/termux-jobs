import importlib.util,json,os,tempfile,unittest
from pathlib import Path
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('live',ROOT/'x11/src/main/assets/x11-scale-live.py');live=importlib.util.module_from_spec(spec);spec.loader.exec_module(live);s=live.s;m=live.m
class Conf:
 def __init__(self,values=None):self.values=dict(values or {});self.fail=None;self.calls=[]
 def get(self,key,template):return self.values.get(key)
 def put(self,key,value):
  self.calls.append(key)
  if key==self.fail:self.fail=None;raise OSError('fixture failure')
  if value is None:self.values.pop(key,None)
  else:self.values[key]=value
class Live(unittest.TestCase):
 def setUp(self):
  self.temp=tempfile.TemporaryDirectory(dir=ROOT/'.verification');self.home=Path(self.temp.name)/'home';self.home.mkdir();self.prefix=Path(self.temp.name)/'prefix';self.prefix.mkdir();self.conf=Conf();self.env=patch.dict(os.environ,{},clear=False);self.env.start();os.environ.pop('XDG_CONFIG_HOME',None)
 def tearDown(self):self.env.stop();self.temp.cleanup()
 def apply(self,n):live.apply(self.home,self.prefix,n,self.conf)
 def test_repeated_live_changes_do_not_compound_and_100_restores(self):
  dpi='xsettings|/Xft/DPI';self.conf.values[dpi]=m.prop('/Xft/DPI','int',96)
  for n in (75,75,150,50,100):
   self.apply(n);self.assertEqual(m.prop('/Xft/DPI','int',n*96//100),self.conf.values[dpi])
  self.assertNotIn('thunar|/last-window-width',self.conf.values);self.assertFalse(json.loads((self.home/'.local/state/termux-jobs/scale/previous.json').read_text())['active'])
 def test_startup_overlay_is_used_as_baseline_and_manual_edits_preserved(self):
  s.apply(self.home,self.prefix,75);_,roots=s.documents(self.home,self.home/'.config/xfce4/xfconf/xfce-perchannel-xml');self.conf.values={key:m.snapshot(roots[key.split('|')[0]],key.split('|')[1]) for key in s.desired(roots,self.prefix,100)}
  self.apply(150);self.assertIn('value="144"',self.conf.values['xsettings|/Xft/DPI'])
  self.conf.values['thunar|/last-window-width']=m.prop('/last-window-width','int',999);self.apply(100);self.assertIn('999',self.conf.values['thunar|/last-window-width'])
 def test_failure_rolls_back_and_keeps_previous_record(self):
  self.apply(75);before=dict(self.conf.values);file=self.home/'.local/state/termux-jobs/scale/previous.json';record=file.read_bytes();self.conf.fail='thunar|/last-window-height'
  with self.assertRaises(OSError):self.apply(150)
  self.assertEqual(before,self.conf.values);self.assertEqual(record,file.read_bytes())
 def test_startup_recovers_both_sides_of_interrupted_transaction(self):
  self.apply(75);file=self.home/'.local/state/termux-jobs/scale/previous.json';state=json.loads(file.read_text());key='xsettings|/Xft/DPI';state['pending']=dict(state['applied']);state['applied'][key]=m.prop('/Xft/DPI','int',144);file.write_text(json.dumps(state));path=self.home/'.config/xfce4/xfconf/xfce-perchannel-xml';path.mkdir(parents=True)
  root=m.tree(None,'xsettings');m.set_node(root,'/Xft/DPI',state['pending'][key]);(path/'xsettings.xml').write_bytes(s.ET.tostring(root));s.restore(self.home);_,node=m.node(s.documents(self.home,path)[1]['xsettings'],'/Xft/DPI');self.assertIsNone(node)
 def test_invalid_scope_stale_token_and_links_rejected(self):
  control=Path(self.temp.name)/'control';control.mkdir();token='11111111-1111-4111-8111-111111111111'
  with patch.dict(os.environ,{'JOBS_X11_TARGET':'termux','JOBS_X11_FOCUS_TOKEN':token,'HOME':str(self.home),'DISPLAY':':97','XAUTHORITY':str(control/'authority'),'DBUS_SESSION_BUS_ADDRESS':'unix:path=/tmp/dbus-fixture'}):
   live.session(control,True);os.environ['JOBS_X11_FOCUS_TOKEN']='22222222-2222-4222-8222-222222222222'
   with self.assertRaises(ValueError):live.session(control)
  for n in (49,151):
   with self.assertRaises(ValueError):self.apply(n)
 def test_native_put_restores_empty_leaf_without_setting_scalar_or_clearing_subtree(self):
  from unittest.mock import Mock
  conf=live.Conf.__new__(live.Conf);conf.lib=Mock();conf.channel=Mock(return_value=23)
  conf.put('xsettings|/Gtk/CursorThemeSize','<property name="CursorThemeSize" type="empty" />')
  conf.lib.xfconf_channel_reset_property.assert_called_once_with(23,b'/Gtk/CursorThemeSize',False)
  conf.lib.xfconf_channel_set_int.assert_not_called();conf.lib.xfconf_channel_set_string.assert_not_called()
  conf.lib.reset_mock()
  with self.assertRaises(ValueError):conf.put('xsettings|/Gtk/CursorThemeSize','<property name="CursorThemeSize" type="empty"><property name="keep" type="int" value="1" /></property>')
  conf.lib.xfconf_channel_reset_property.assert_not_called()
 def test_native_xfconf_string_lifetime_and_scale_roundtrip(self):
  import subprocess,signal,textwrap
  prefix=Path('/data/data/com.termux/files/usr');python=prefix/'bin/python3';bus=prefix/'bin/dbus-run-session'
  if not all(x.exists() for x in (python,bus,prefix/'lib/libxfconf-0.so')):self.skipTest('native Termux xfconf unavailable')
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   base=Path(d)
   for n in ('home','tmp','run','cache','state','data'):(base/n).mkdir()
   daemon=base/'private-daemon';daemon.write_text('#!/bin/sh\nexec /data/data/com.termux/files/usr/bin/dbus-daemon --address=unix:tmpdir="$SCALE_FIXTURE_TMP" "$@"\n');daemon.chmod(0o700)
   script=base/'check.py';script.write_text(textwrap.dedent("""
    import importlib.util,os
    from pathlib import Path
    spec=importlib.util.spec_from_file_location('live',Path(os.environ['SCALE_SOURCE']));m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
    conf=m.Conf()
    try:
     key='xsettings|/Xft/DPI';template=m.m.prop('/Xft/DPI','int',96)
     cursor='xsettings|/Gtk/CursorThemeSize';cursor_template=m.m.prop('/Gtk/CursorThemeSize','int',24)
     conf.put(key,None);conf.put(cursor,None)
     default_dpi=conf.get(key,template);default_cursor=conf.get(cursor,cursor_template)
     conf.put(key,template)
     # Real xfconf API plus a startup journal whose originals are XFCE's
     # untyped empty leaves, as found in developer Termux/Fedora/Ubuntu configs.
     directory=Path.home()/'.config/xfce4/xfconf/xfce-perchannel-xml'
     directory.mkdir(parents=True,exist_ok=True)
     for prop in ('/Xft/DPI','/Gtk/CursorThemeSize'):
      file=directory/'xsettings.xml';raw=m.m.read(file);root=m.m.tree(raw,'xsettings')
      m.m.set_node(root,prop,'<property name="'+prop.rsplit('/',1)[-1]+'" type="empty" />')
      file.write_bytes(m.ET.tostring(root))
     m.s.apply(Path.home(),Path(os.environ['PREFIX']),75)
     _,roots=m.s.documents(Path.home(),directory)
     record=Path.home()/'.local/state/termux-jobs/scale/previous.json'
     import json
     for identity,value in json.loads(record.read_text())['applied'].items():conf.put(identity,value)
     m.apply(Path.home(),Path(os.environ['PREFIX']),100,conf)
     assert conf.get(key,template)==default_dpi
     assert conf.get(cursor,cursor_template)==default_cursor
     conf.put(key,template)
     for percent,expected in [(75,72),(150,144),(75,72),(100,96)]:
      m.apply(Path.home(),Path(os.environ['PREFIX']),percent,conf)
      assert conf.get(key,template)==m.m.prop('/Xft/DPI','int',expected)
     print('native xfconf strings and repeated scale: pass')
    finally:conf.close()
   """))
   env=dict(os.environ,HOME=str(base/'home'),TMPDIR=str(base/'tmp'),XDG_RUNTIME_DIR=str(base/'run'),XDG_CONFIG_HOME=str(base/'home/.config'),XDG_CACHE_HOME=str(base/'cache'),XDG_STATE_HOME=str(base/'state'),XDG_DATA_HOME=str(base/'data'),PREFIX=str(prefix),PATH=str(prefix/'bin')+':/usr/bin:/bin',SCALE_SOURCE=str(ROOT/'x11/src/main/assets/x11-scale-live.py'),SCALE_FIXTURE_TMP=str(base/'tmp'))
   env.pop('DBUS_SESSION_BUS_ADDRESS',None);env.pop('DBUS_SYSTEM_BUS_ADDRESS',None)
   p=subprocess.Popen([str(bus),'--dbus-daemon='+str(daemon),'--',str(python),'-B',str(script)],env=env,stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True,start_new_session=True)
   try:out,err=p.communicate(timeout=15)
   except subprocess.TimeoutExpired:
    if p.poll() is None:os.killpg(p.pid,signal.SIGTERM)
    p.communicate(timeout=3);self.fail('private bus fixture timed out')
   self.assertEqual(0,p.returncode,err[-1000:]);self.assertIn('native xfconf strings and repeated scale: pass',out)
 def test_scale_route_never_checks_or_installs_packages_or_starts_a_bus(self):
  import subprocess
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   root=Path(d)/'files';prefix=root/'usr';bin=prefix/'bin';bin.mkdir(parents=True);control=root/'workspace-x11';control.mkdir();log=root/'calls';token='11111111-1111-4111-8111-111111111111'
   (control/'x11-audio.sh').write_bytes((ROOT/'x11/src/main/assets/x11-audio.sh').read_bytes());tool=bin/'python3';tool.write_text('#!/bin/sh\nprintf "%s\\n" "$@" > "$CAPTURE"\nprintf "READY:screen-scale\\n"\n');tool.chmod(0o700)
   env=dict(os.environ,PREFIX=str(prefix),PATH=str(bin)+':/usr/bin:/bin',CAPTURE=str(log),JOBS_X11_FOCUS_TOKEN=token,JOBS_X11_SCALE='75')
   p=subprocess.run(['bash',str(ROOT/'x11/src/main/assets/x11-desktop.sh'),'scale','termux',str(control)],env=env,capture_output=True,text=True,timeout=3)
   self.assertEqual(0,p.returncode,p.stderr);self.assertIn('x11-scale-live.py\napply\n75',log.read_text());self.assertIn('READY:screen-scale',p.stdout)
   proot=bin/'proot-distro';proot.write_text('#!/bin/sh\nprintf "%s\\n" "$@" > "$CAPTURE"\nprintf "READY:screen-scale\\n"\n');proot.chmod(0o700)
   for target in ('ubuntu','debian','alpine','fedora','opensuse','void','archlinux'):
    (prefix/('var/lib/proot-distro/containers/'+target+'/rootfs')).mkdir(parents=True);own=root/('workspace-x11-'+target);own.mkdir()
    result=subprocess.run(['bash',str(ROOT/'x11/src/main/assets/x11-desktop.sh'),'scale',target,str(own)],env=env,capture_output=True,text=True,timeout=3)
    self.assertEqual(0,result.returncode,(target,result.stderr));args=log.read_text();self.assertIn('\n'+target+'\n',args);self.assertIn('JOBS_X11_TARGET='+target,args);self.assertIn('x11-scale-live.py apply 75',args)
    for forbidden in ('dbus-run-session','apt-get','dnf ','pacman ','jobs-ai-ubuntu'):self.assertNotIn(forbidden,args)

 def test_window_resize_has_no_move_or_focus_and_uses_relative_ratio(self):
  from unittest.mock import Mock
  w=live.Windows.__new__(live.Windows);w.x=Mock();w.x.d=None;w.windows=[(7,800,600)];w.bounds=(1300,1000);w.resize(100,75);w.x.lib.XResizeWindow.assert_called_once_with(None,7,600,450);w.x.lib.XSetInputFocus.assert_not_called()
if __name__=='__main__':unittest.main()
