import contextlib
import importlib.util
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

ROOT=Path(__file__).resolve().parents[2]
ASSETS=ROOT/'x11/src/main/assets'
def load(name):
 spec=importlib.util.spec_from_file_location(name,ASSETS/(name+'.py'));m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m
setup=load('x11-gpu-test-setup');app=load('x11-gpu-test');native=load('x11-native-programs')
class GpuTest(unittest.TestCase):
 def setUp(self):
  parent=ROOT/'.verification/gpu-test/fixtures';parent.mkdir(parents=True,exist_ok=True)
  self.tmp=tempfile.TemporaryDirectory(dir=parent);self.home=Path(self.tmp.name)
  self.env=patch.dict(os.environ,{'XDG_CONFIG_HOME':str(self.home/'.config')});self.env.start()
 def tearDown(self):self.env.stop();self.tmp.cleanup()
 def test_optional_setup_offline_idempotent_and_removal_keeps_reports(self):
  self.assertFalse(setup.ready(self.home));self.assertFalse(list(self.home.iterdir()))
  setup.prepare(self.home,'ubuntu');self.assertTrue(setup.ready(self.home))
  before={str(p.relative_to(self.home)):p.read_bytes() for p in self.home.rglob('*') if p.is_file()}
  setup.prepare(self.home,'ubuntu');self.assertEqual(before,{str(p.relative_to(self.home)):p.read_bytes() for p in self.home.rglob('*') if p.is_file()})
  desktop=self.home/'Desktop/termux-jobs-gpu-test.desktop';self.assertIn('Name=GPU TEST',desktop.read_text());self.assertIn('Terminal=false',desktop.read_text())
  report=self.home/'Jobs-GPU-Test.json';report.write_text('report');setup.remove(self.home);self.assertFalse(setup.ready(self.home));self.assertFalse(desktop.exists());self.assertEqual('report',report.read_text())
 def test_edited_files_preserved_before_any_removal(self):
  setup.prepare(self.home,'debian');desktop=self.home/'Desktop/termux-jobs-gpu-test.desktop';desktop.write_text('my file')
  with self.assertRaisesRegex(ValueError,'Edited'):setup.remove(self.home)
  self.assertTrue((self.home/setup.REL/'app.py').exists());self.assertEqual('my file',desktop.read_text())
 def test_unmanaged_existing_shortcut_preserved(self):
  desktop=self.home/'Desktop';desktop.mkdir();f=desktop/'termux-jobs-gpu-test.desktop';f.write_text('personal')
  with self.assertRaisesRegex(ValueError,'Edited'):setup.prepare(self.home,'alpine')
  self.assertEqual('personal',f.read_text())
 def test_symlink_never_overwrites_outside_home(self):
  outside=self.home/'outside';outside.mkdir();(self.home/'.local').symlink_to(outside,target_is_directory=True)
  with self.assertRaisesRegex(ValueError,'linked'):setup.prepare(self.home,'termux')
  self.assertFalse(list(outside.iterdir()))
 def test_changed_payload_is_not_reported_installed(self):
  setup.prepare(self.home,'void');(self.home/setup.REL/'app.py').write_text('broken');self.assertFalse(setup.ready(self.home))
 def test_installer_groups_use_each_environments_package_manager_dependencies(self):
  for target in ['termux','alpine','fedora','opensuse','void','archlinux']:
   group=native.packages(target,'gpu-test');self.assertIn('python' if target in ('termux','archlinux') else 'python3',group)
   if target=='termux':self.assertEqual(['python','libx11','mesa'],group)
   else:self.assertTrue(set(native.GPU_PACKAGES[target])<=set(group))
 def test_configured_termux_runtime_installs_local_payload_without_network(self):
  pkgs=[*native.packages('termux','gpu-test'),'libc++']
  with patch.object(native,'installed',return_value=set(pkgs)),patch.object(native,'run',side_effect=AssertionError('network transaction')),patch.object(native,'load',return_value=setup),patch.object(native.Path,'home',return_value=self.home):
   self.assertTrue(native.local_gpu_test('termux',pkgs))
  self.assertTrue(setup.ready(self.home))
 def test_termux_install_entrypoint_uses_local_setup_without_package_commands(self):
  pkgs={'python','libx11','mesa','libc++'}
  class Helper:
   def prepare(_,home,target):setup.prepare(self.home,target)
  with patch.dict(os.environ,{'JOBS_X11_NATIVE_TARGET':'termux','PREFIX':'/data/data/com.termux/files/usr'}),patch.object(native.sys,'argv',['helper','install','gpu-test']),patch.object(native.Path,'home',return_value=Path('/data/data/com.termux/files/home')),patch.object(native,'installed',return_value=pkgs),patch.object(native,'load',return_value=Helper()),patch.object(native,'run',side_effect=AssertionError('network transaction')),patch.object(native,'main_check') as check:
   native.main();check.assert_called_once_with('termux')
  self.assertTrue(setup.ready(self.home))
 def test_missing_runtime_dependency_requires_normal_transaction(self):
  with patch.object(native,'installed',return_value={'python','libx11','libc++'}),patch.object(native,'load',side_effect=AssertionError('premature setup')):
   self.assertFalse(native.local_gpu_test('termux',['python','libx11','mesa','libc++']))
  self.assertFalse(list(self.home.iterdir()))
 def test_report_is_bounded_metadata_not_environment(self):
  obj=app.App.__new__(app.App);obj.args=type('Args',(),{})();obj.info={'renderer':'test','opengl':'4.6','vendor':'test'};obj.width=640;obj.height=480;obj.scene=0;obj.interval=0;obj.swap_control=True;obj.frames=5;obj.start=app.time.perf_counter();obj.samples=[1.,2.,3.]
  with patch.dict(os.environ,{'JOBS_SECRET_TOKEN':'do-not-export'}):report=obj.report()
  data=json.dumps(report);self.assertNotIn('do-not-export',data);self.assertEqual(2.,report['recentFrameTimesMs']['median'])
  dest=self.home/'report.json';app.write_report(dest,report);self.assertEqual(report,json.loads(dest.read_text()));self.assertEqual([dest],list(self.home.iterdir()))
 def test_manual_reports_keep_both_modes_and_repeated_same_mode(self):
  before={'requestedSwapInterval':0,'frames':20}
  after={'requestedSwapInterval':1,'frames':30}
  a=app.write_new_report(self.home,before);b=app.write_new_report(self.home,after);c=app.write_new_report(self.home,after)
  self.assertEqual(3,len({a,b,c}));self.assertIn('vsync-0-',a.name);self.assertIn('vsync-1-',b.name)
  self.assertEqual(before,json.loads(a.read_text()));self.assertEqual(after,json.loads(b.read_text()))
 def test_explicit_report_path_retains_overwrite_for_automation(self):
  path=self.home/'fixed.json';app.write_report(path,{'frames':1});app.write_report(path,{'frames':2})
  self.assertEqual({'frames':2},json.loads(path.read_text()));self.assertEqual([path],list(self.home.iterdir()))
 def test_report_refuses_existing_link(self):
  original=self.home/'personal';original.write_text('keep');link=self.home/'report.json';link.symlink_to(original)
  with self.assertRaises(ValueError):app.write_report(link,{'test':True})
  self.assertEqual('keep',original.read_text())
