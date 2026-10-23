"""Real GIO + compiled filter; temporary regular files only, no live desktop."""
from pathlib import Path
import ctypes,os,subprocess,tempfile,unittest
ROOT=Path(__file__).resolve().parents[2]
class GarconFilterTests(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  cls.tmp=tempfile.TemporaryDirectory(dir=ROOT/'.verification');cls.base=Path(cls.tmp.name);sdk=ROOT/'.tools/garcon-build/sdk'
  cls.libpath=cls.base/'filter.so'
  subprocess.run(['cc','-shared','-fPIC','-O2','-std=c11','-Wall','-Wextra','-Werror','-I'+str(sdk/'usr/include/glib-2.0'),'-I'+str(sdk/'usr/lib/aarch64-linux-gnu/glib-2.0/include'),str(ROOT/'x11/src/main/cpp/garcon-attribute-filter.c'),'/usr/lib/aarch64-linux-gnu/libgio-2.0.so.0','/usr/lib/aarch64-linux-gnu/libglib-2.0.so.0','-o',str(cls.libpath)],check=True,capture_output=True)
  cls.gio=ctypes.CDLL('libgio-2.0.so.0');cls.gio.g_file_new_for_path.argtypes=[ctypes.c_char_p];cls.gio.g_file_new_for_path.restype=ctypes.c_void_p
  cls.gio.g_object_unref.argtypes=[ctypes.c_void_p]
  cls.gio.g_object_get_type.restype=ctypes.c_ulong;cls.gio.g_object_new.argtypes=[ctypes.c_ulong,ctypes.c_void_p];cls.gio.g_object_new.restype=ctypes.c_void_p
  cls.owner=cls.gio.g_object_new(cls.gio.g_object_get_type(),None)
  cls.lib=ctypes.CDLL(str(cls.libpath));cls.lib.jobs_garcon_attribute_unchanged.argtypes=[ctypes.c_void_p,ctypes.c_void_p,ctypes.c_int];cls.lib.jobs_garcon_attribute_unchanged.restype=ctypes.c_int
 @classmethod
 def tearDownClass(cls):cls.gio.g_object_unref(cls.owner);cls.tmp.cleanup()
 def setUp(self):self.env=os.environ.get('JOBS_X11_GARCON_FILTER');os.environ['JOBS_X11_GARCON_FILTER']='1'
 def tearDown(self):
  if self.env is None:os.environ.pop('JOBS_X11_GARCON_FILTER',None)
  else:os.environ['JOBS_X11_GARCON_FILTER']=self.env
 def check(self,p,event=4):
  f=self.gio.g_file_new_for_path(os.fsencode(p))
  try:return bool(self.lib.jobs_garcon_attribute_unchanged(self.owner,f,event))
  finally:self.gio.g_object_unref(f)
 def test_duplicate_attributes_and_same_mode_chmod_are_suppressed(self):
  p=self.base/'duplicate.desktop';p.write_text('Name=Test\n');self.assertFalse(self.check(p));self.assertTrue(self.check(p));p.chmod(p.stat().st_mode);self.assertTrue(self.check(p))
 def test_content_permissions_and_identity_changes_are_allowed(self):
  p=self.base/'changes.desktop';p.write_text('alpha');self.assertFalse(self.check(p));st=p.stat();p.write_text('bravo');os.utime(p,ns=(st.st_atime_ns,st.st_mtime_ns));self.assertFalse(self.check(p));self.assertTrue(self.check(p));p.chmod(0o700);self.assertFalse(self.check(p));self.assertTrue(self.check(p));q=self.base/'replacement';q.write_text('bravo');os.replace(q,p);self.assertFalse(self.check(p))
 def test_other_events_invalidate_and_opt_in_is_required(self):
  p=self.base/'events.desktop';p.write_text('Name=Test\n');self.assertFalse(self.check(p));self.assertTrue(self.check(p));self.assertFalse(self.check(p,1));self.assertFalse(self.check(p));os.environ['JOBS_X11_GARCON_FILTER']='0';self.assertFalse(self.check(p))
 def test_missing_symlink_non_desktop_and_large_files_pass(self):
  p=self.base/'missing.desktop';self.assertFalse(self.check(p));p.write_text('Name=Test\n');link=self.base/'link.desktop';link.symlink_to(p);self.assertFalse(self.check(link));self.assertFalse(self.check(link));other=self.base/'other.txt';other.write_text('x');self.assertFalse(self.check(other));self.assertFalse(self.check(other));large=self.base/'large.desktop';large.write_bytes(b'x'*(256*1024+1));self.assertFalse(self.check(large));self.assertFalse(self.check(large))
 def test_real_garcon_menu_load_inotify_duplicates_and_content_update(self):
  sdk=ROOT/'.tools/garcon-build/sdk';src=ROOT/'.tools/garcon-build/garcon-4.20.0';library=ROOT/'.tools/x11-runtime/assets/libgarcon-1.so.0';binary=self.base/'integration'
  if not library.exists():self.skipTest('Unsafe trial overlay retired from APK inputs')
  args=['cc','-O2','-std=c11','-I'+str(src),'-I'+str(sdk/'usr/include/glib-2.0'),'-I'+str(sdk/'usr/lib/aarch64-linux-gnu/glib-2.0/include'),'-I'+str(ROOT/'x11/src/main/cpp'),str(ROOT/'scripts/tests/fixtures/garcon-integration-test.c'),str(library),'/usr/lib/aarch64-linux-gnu/libgio-2.0.so.0','/usr/lib/aarch64-linux-gnu/libglib-2.0.so.0','/usr/lib/aarch64-linux-gnu/libgobject-2.0.so.0','-Wl,-rpath-link,'+str(sdk/'usr/lib/aarch64-linux-gnu'),'-o',str(binary)]
  subprocess.run(args,check=True,capture_output=True)
  with tempfile.TemporaryDirectory(dir=self.base) as tmp:
   result=subprocess.run([str(binary),tmp],env={**os.environ,'LD_LIBRARY_PATH':str(library.parent)+':'+str(sdk/'usr/lib/aarch64-linux-gnu')},capture_output=True,text=True,check=True,timeout=8)
  self.assertIn('real-menu-read=true real-inotify-filtered=true content-change-applied=true',result.stdout);self.assertEqual('',result.stderr)
 def test_each_menu_consumer_sees_first_event_and_its_real_changes(self):
  p=self.base/'consumers.desktop';p.write_text('alpha');other=self.gio.g_object_new(self.gio.g_object_get_type(),None);f=self.gio.g_file_new_for_path(os.fsencode(p))
  try:
   self.assertFalse(self.check(p));self.assertFalse(self.lib.jobs_garcon_attribute_unchanged(other,f,4));self.assertTrue(self.check(p));self.assertTrue(self.lib.jobs_garcon_attribute_unchanged(other,f,4))
   p.write_text('bravo');self.assertFalse(self.check(p));self.assertFalse(self.lib.jobs_garcon_attribute_unchanged(other,f,4))
  finally:self.gio.g_object_unref(f);self.gio.g_object_unref(other)
 def test_disabled_filter_does_not_scan_even_while_recording(self):
  p=self.base/'observe.desktop';p.write_text('Name=Test\n');os.environ['JOBS_X11_GARCON_FILTER']='0'
  data=(ctypes.c_uint64*12)();self.lib.jobs_garcon_filter_details(data);before=list(data)
  self.lib.jobs_garcon_set_observing(1)
  try:
   self.assertFalse(self.check(p));self.assertFalse(self.check(p))
   self.lib.jobs_garcon_filter_details(data);self.assertEqual(before,list(data))
  finally:self.lib.jobs_garcon_set_observing(0)
 def test_cache_capacity_passes_first_event_after_bounded_reset(self):
  for i in range(515):
   p=self.base/f'capacity-{i}.desktop';p.write_text('Name=Test\n');self.assertFalse(self.check(p))
  self.assertTrue(self.check(p))
if __name__=='__main__':unittest.main()
