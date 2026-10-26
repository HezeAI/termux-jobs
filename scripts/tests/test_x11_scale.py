import importlib.util,os,tempfile,unittest
from pathlib import Path
from unittest.mock import patch
import xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[2];ASSETS=ROOT/'x11/src/main/assets'
spec=importlib.util.spec_from_file_location('jobs_scale',ASSETS/'x11-scale.py');scale=importlib.util.module_from_spec(spec);spec.loader.exec_module(scale);style=scale.m
class ScaleTests(unittest.TestCase):
 def setUp(self):
  self.temp=tempfile.TemporaryDirectory(dir=ROOT/'.verification');self.root=Path(self.temp.name);self.home=self.root/'home';self.home.mkdir();self.prefix=self.root/'prefix';self.prefix.mkdir();self.control=self.root/'control';self.control.mkdir();self.channels=self.home/'.config/xfce4/xfconf/xfce-perchannel-xml';self.channels.mkdir(parents=True)
  self.env=patch.dict(os.environ,{},clear=False);self.env.start();os.environ.pop('XDG_CONFIG_HOME',None)
 def tearDown(self):self.env.stop();self.temp.cleanup()
 def channel(self,name):return style.tree(style.read(self.channels/(name+'.xml')),name)
 def value(self,channel,key):
  _,n=style.node(self.channel(channel),key);return n.get('value') if n is not None else None
 def put(self,channel,key,typ,value):
  root=self.channel(channel);style.set_node(root,key,style.prop(key,typ,value));(self.channels/(channel+'.xml')).write_bytes(ET.tostring(root))
 def cycle(self,percent):scale.restore(self.home);return scale.apply(self.home,self.prefix,percent)
 def test_default_100_does_not_create_or_change_configuration(self):
  before={p:p.read_bytes() for p in self.home.rglob('*') if p.is_file()};self.cycle(100);self.assertEqual(before,{p:p.read_bytes() for p in self.home.rglob('*') if p.is_file()})
 def test_scale_repeated_start_does_not_accumulate_and_100_restores(self):
  self.put('xsettings','/Xft/DPI','int',96);self.put('xfce4-desktop','/desktop-icons/icon-size','uint',74);self.put('xfce4-panel','/panels/panel-1/size','uint',80);self.put('xfce4-panel','/panels/panel-1/icon-size','uint',52)
  for _ in range(3):
   self.cycle(75);self.assertEqual('72',self.value('xsettings','/Xft/DPI'));self.assertEqual('56',self.value('xfce4-desktop','/desktop-icons/icon-size'));self.assertEqual('60',self.value('xfce4-panel','/panels/panel-1/size'));self.assertEqual('39',self.value('xfce4-panel','/panels/panel-1/icon-size'))
  self.cycle(100);self.assertEqual('96',self.value('xsettings','/Xft/DPI'));self.assertEqual('74',self.value('xfce4-desktop','/desktop-icons/icon-size'));self.assertEqual('80',self.value('xfce4-panel','/panels/panel-1/size'));self.assertIsNone(self.value('xsettings','/Gtk/CursorThemeSize'))
 def test_restoration_preserves_manual_edits_and_other_home(self):
  self.cycle(75);self.put('xsettings','/Xft/DPI','int',123);scale.restore(self.home);self.assertEqual('123',self.value('xsettings','/Xft/DPI'))
  other=self.root/'other';other.mkdir();self.assertEqual('off',scale.restore(other));self.assertEqual([],list(other.iterdir()))
 def test_distribution_panel_defaults_and_local_empty_inheritance(self):
  defaults=self.prefix/'etc/xdg/xfce4/panel/default.xml';defaults.parent.mkdir(parents=True);defaults.write_text('<channel name="xfce4-panel"><property name="panels" type="array"><property name="panel-1" type="empty"><property name="size" type="uint" value="44"/><property name="icon-size" type="uint" value="0"/></property></property></channel>')
  self.put('xfce4-panel','/panels/panel-1','empty','');self.cycle(75);self.assertEqual('33',self.value('xfce4-panel','/panels/panel-1/size'));self.assertIsNone(self.value('xfce4-panel','/panels/panel-1/icon-size'));self.cycle(100);self.assertIsNone(self.value('xfce4-panel','/panels/panel-1/size'))
 def test_theme_switching_does_not_treat_scale_as_user_edits(self):
  (self.control/'x11-style.zip').write_bytes((ROOT/'.tools/x11-components/x11-style.zip').read_bytes());self.put('xfce4-desktop','/desktop-icons/icon-size','uint',48)
  scale.restore(self.home);style.apply(self.home,self.prefix,self.control,True);scale.apply(self.home,self.prefix,75);self.assertEqual('56',self.value('xfce4-desktop','/desktop-icons/icon-size'))
  scale.restore(self.home);style.apply(self.home,self.prefix,self.control,False);scale.apply(self.home,self.prefix,75);self.assertEqual('36',self.value('xfce4-desktop','/desktop-icons/icon-size'))
  self.cycle(100);self.assertEqual('48',self.value('xfce4-desktop','/desktop-icons/icon-size'))
 def test_folder_contents_sidebar_and_new_window_follow_single_scale(self):
  self.put('thunar','/last-window-width','int',1000);self.put('thunar','/last-window-height','int',800)
  self.put('thunar','/last-icon-view-zoom-level','string','THUNAR_ZOOM_LEVEL_100_PERCENT')
  self.put('thunar','/shortcuts-icon-size','string','THUNAR_ICON_SIZE_48')
  for _ in range(3):
   self.cycle(75);self.assertEqual('750',self.value('thunar','/last-window-width'));self.assertEqual('600',self.value('thunar','/last-window-height'))
   self.assertEqual('THUNAR_ZOOM_LEVEL_75_PERCENT',self.value('thunar','/last-icon-view-zoom-level'));self.assertEqual('THUNAR_ICON_SIZE_32',self.value('thunar','/shortcuts-icon-size'));self.assertEqual('128',self.value('thunar','/last-separator-position'))
  self.cycle(100);self.assertEqual('1000',self.value('thunar','/last-window-width'));self.assertEqual('THUNAR_ZOOM_LEVEL_100_PERCENT',self.value('thunar','/last-icon-view-zoom-level'));self.assertIsNone(self.value('thunar','/last-separator-position'))
 def test_fresh_thunar_legacy_enum_and_user_edits(self):
  self.cycle(150);self.assertEqual('960',self.value('thunar','/last-window-width'));self.assertEqual('THUNAR_ZOOM_LEVEL_150_PERCENT',self.value('thunar','/last-icon-view-zoom-level'));self.assertEqual('THUNAR_ICON_SIZE_24',self.value('thunar','/tree-icon-size'))
  self.put('thunar','/last-window-width','int',999);self.cycle(100);self.assertEqual('999',self.value('thunar','/last-window-width'));self.assertIsNone(self.value('thunar','/last-icon-view-zoom-level'))
  self.put('thunar','/last-icon-view-zoom-level','string','THUNAR_ZOOM_LEVEL_NORMAL');self.put('thunar','/shortcuts-icon-size','string','THUNAR_ICON_SIZE_NORMAL');self.cycle(50)
  self.assertEqual('THUNAR_ZOOM_LEVEL_38_PERCENT',self.value('thunar','/last-icon-view-zoom-level'));self.assertEqual('THUNAR_ICON_SIZE_24',self.value('thunar','/shortcuts-icon-size'))
 def test_unknown_zoom_and_other_channels_are_preserved(self):
  self.put('thunar','/last-icon-view-zoom-level','string','THUNAR_ZOOM_LEVEL_1600_PERCENT');self.put('thunar','/last-tips','string','keep');self.put('agent','/project','string','keep')
  self.cycle(75);self.assertEqual('THUNAR_ZOOM_LEVEL_1600_PERCENT',self.value('thunar','/last-icon-view-zoom-level'));self.assertEqual('keep',self.value('thunar','/last-tips'));self.assertEqual('keep',self.value('agent','/project'))
 def test_invalid_scale_and_link_or_malformed_config_are_refused(self):
  for n in (0,49,151,999):
   with self.assertRaises(ValueError):scale.apply(self.home,self.prefix,n)
  victim=self.root/'keep';victim.write_text('keep');(self.channels/'xsettings.xml').symlink_to(victim)
  with self.assertRaises(ValueError):self.cycle(75)
  self.assertEqual('keep',victim.read_text());(self.channels/'xsettings.xml').unlink();(self.channels/'xsettings.xml').write_text('<!DOCTYPE channel><channel name="xsettings"/>')
  with self.assertRaises(ValueError):self.cycle(75)
 def test_default_with_custom_config_link_keeps_preexisting_behavior(self):
  alternate=self.root/'user-config';alternate.mkdir();os.environ['XDG_CONFIG_HOME']=str(self.home/'linked-config');(self.home/'linked-config').symlink_to(alternate,target_is_directory=True)
  self.assertEqual('off',scale.restore(self.home));self.assertEqual('default',scale.apply(self.home,self.prefix,100));self.assertEqual([],list(alternate.iterdir()))
if __name__=='__main__':unittest.main()
