"""Isolated guest-module ABI/control tests; never loads into a running desktop."""
from pathlib import Path
import ctypes,json,os,subprocess,tempfile,unittest
ROOT=Path(__file__).resolve().parents[2]
class GtkObserverTests(unittest.TestCase):
 def test_loader_flag_defaults_off(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as tmp:
   marker=Path(tmp)/'called';module=Path(tmp)/'libjobs.so'
   fake=Path(tmp)/'fake.c';fake.write_text('#include <stdio.h>\nconst char *g_get_prgname(void){FILE *f=fopen("MARKER", "w");if(f)fclose(f);return "unrelated-program";}\n'.replace('MARKER',str(marker)))
   subprocess.run(['cc','-shared','-fPIC',str(fake),'-o',str(module)],check=True,capture_output=True)
   library=Path(tmp)/'probe.so';subprocess.run(['cc','-shared','-fPIC','-std=c11',str(ROOT/'x11/src/main/cpp/x11-gtk-observe.c'),'-ldl','-o',str(library)],check=True,capture_output=True)
   code='import ctypes,sys; ctypes.CDLL(sys.argv[1],mode=ctypes.RTLD_GLOBAL); ctypes.CDLL(sys.argv[2]).gtk_module_init(None,None)'
   for flag in ['', '0', '1']:
    marker.unlink(missing_ok=True);env={**os.environ,'JOBS_X11_GTK_OBSERVE':flag}
    subprocess.run(['python3','-c',code,str(module),str(library)],env=env,check=True,capture_output=True,timeout=5)
    self.assertEqual(flag=='1',marker.exists())

 def test_control_and_emissions_cannot_change_events_or_leak_payloads(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as tmp:
   binary=Path(tmp)/'probe';control=Path(tmp)/'control'
   subprocess.run(['cc','-O2','-std=c11','-Wall','-Wextra','-Werror','-DJOBS_GTK_CONTROL='+json.dumps(str(control)),str(ROOT/'scripts/tests/fixtures/x11-gtk-observe-test.c'),'-ldl','-o',str(binary)],check=True,capture_output=True)
   result=subprocess.run([str(binary)],check=True,capture_output=True,text=True,timeout=5)
   self.assertIn('capture=1 phase=sample',result.stdout);self.assertIn('draw=1 add=1 remove=1',result.stdout);self.assertIn('reload=1',result.stdout);self.assertIn('capture=1 phase=finished',result.stdout);self.assertIn('capture=3 phase=started',result.stdout)
   self.assertNotIn('wrong-token',result.stdout);self.assertNotIn('do-not-follow',result.stdout);self.assertEqual('',result.stderr)
 def test_guest_module_is_optional_when_loaded_without_gtk(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as tmp:
   library=Path(tmp)/'probe.so'
   subprocess.run(['cc','-shared','-fPIC','-O2','-std=c11','-Wall','-Wextra','-Werror',str(ROOT/'x11/src/main/cpp/x11-gtk-observe.c'),'-ldl','-o',str(library)],check=True,capture_output=True)
   subprocess.run(['python3','-c','import ctypes,sys; ctypes.CDLL(sys.argv[1]).gtk_module_init(None,None)',str(library)],env={**os.environ,'JOBS_X11_GTK_OBSERVE':'1'},check=True,capture_output=True,timeout=5)

 def test_real_gobject_emission_hooks_and_no_hooks_reload_signal(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as tmp:
   binary=Path(tmp)/'probe';module=Path(tmp)/'module.so';control=Path(tmp)/'control'
   subprocess.run(['cc','-shared','-fPIC','-O2','-std=c11','-DJOBS_GTK_CONTROL='+json.dumps(str(control)),str(ROOT/'x11/src/main/cpp/x11-gtk-observe.c'),'-ldl','-o',str(module)],check=True,capture_output=True)
   subprocess.run(['cc','-rdynamic','-O2','-std=c11','-Wall','-Wextra','-Werror',str(ROOT/'scripts/tests/fixtures/x11-gtk-gobject-test.c'),'-ldl','-o',str(binary)],check=True,capture_output=True)
   result=subprocess.run([str(binary),str(module),str(control)],check=True,capture_output=True,text=True,timeout=6)
   self.assertIn('hooks=511 core-hooks=1',result.stdout);self.assertIn('draw=2 add=1 remove=1 map=1',result.stdout);self.assertIn('reload=1',result.stdout);self.assertIn('file-hook=1 file-events=2 changed=1',result.stdout);self.assertIn('attribute=1',result.stdout);self.assertIn('garcon-tagged=2',result.stdout);self.assertIn('menu=1 desktop=1',result.stdout);self.assertNotIn('SECRET',result.stdout);self.assertNotIn('PRIVATE',result.stdout);self.assertEqual('',result.stderr)
 def test_gmodule_keeps_callback_code_resident_after_reference_close(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as tmp:
   module=Path(tmp)/'module.so';subprocess.run(['cc','-shared','-fPIC','-std=c11',str(ROOT/'x11/src/main/cpp/x11-gtk-observe.c'),'-ldl','-o',str(module)],check=True,capture_output=True)
   code="""import ctypes,sys,pathlib
lib=ctypes.CDLL('libgmodule-2.0.so.0',mode=ctypes.RTLD_GLOBAL)
lib.g_module_open.argtypes=[ctypes.c_char_p,ctypes.c_int];lib.g_module_open.restype=ctypes.c_void_p
lib.g_module_close.argtypes=[ctypes.c_void_p];lib.g_module_close.restype=ctypes.c_int
m=lib.g_module_open(sys.argv[1].encode(),0);assert m
assert lib.g_module_close(m)
assert sys.argv[1] in pathlib.Path('/proc/self/maps').read_text()
"""
   subprocess.run(['python3','-c',code,str(module)],check=True,capture_output=True,timeout=5)
