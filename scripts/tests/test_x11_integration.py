"""Safety/ABI checks for the pinned native dependency and private process wiring."""
from pathlib import Path
import hashlib,json,re,socket,unittest,xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[2]
class X11Checks(unittest.TestCase):
 def test_native_registration_declarations_are_unchanged(self):
  contract=json.loads((ROOT/'third-party/termux-x11/jni-declarations.json').read_text())
  source=(ROOT/'x11/src/main/java/com/termux/x11/LorieView.java').read_text()
  actual=re.findall(r'    @(?:FastNative|CriticalNative)[^\n]*native[^\n]*;',source)
  self.assertEqual(contract,actual);self.assertEqual(28,len(contract))
 def test_required_activity_native_registration_is_present(self):
  source=(ROOT/'x11/src/main/java/com/termux/x11/MainActivity.java').read_text()
  self.assertIn('private static native void nativeSetLaunchWindowingModeFreeform(Object activityOptions);',source)
 def test_components_are_private_and_share_only_the_dedicated_process(self):
  xml=ET.parse(ROOT/'x11/src/main/AndroidManifest.xml').getroot();ns='{http://schemas.android.com/apk/res/android}'
  parts=list(xml.find('application'));self.assertEqual(['activity']+['service']*9,[p.tag for p in parts])
  for part in parts:self.assertEqual('false',part.get(ns+'exported'))
  self.assertEqual({':jobs_x11',':jobs_x11_ubuntu',':jobs_x11_test',':jobs_x11_debian',':jobs_x11_alpine',':jobs_x11_fedora',':jobs_x11_opensuse',':jobs_x11_void',':jobs_x11_archlinux'},{p.get(ns+'process') for p in parts});self.assertEqual(9,len({p.get(ns+'process') for p in parts if p.tag=='service'}))
  self.assertIsNone(xml.find('application').get(ns+'name'))
 def test_pinned_inputs_match_and_only_arm64_is_supplied(self):
  cache=ROOT/'.tools/x11-runtime'
  self.assertEqual(['arm64-v8a/libXlorie.so'],[str(p.relative_to(cache/'jniLibs')) for p in (cache/'jniLibs').rglob('*.so')])
  meta=json.loads((cache/'verified.json').read_text())
  for name,key in [('jniLibs/arm64-v8a/libXlorie.so','nativeSha256'),('assets/x11-keyboard.zip','keyboardSha256')]:self.assertEqual(meta[key],hashlib.sha256((cache/name).read_bytes()).hexdigest())
 def test_bound_nonlistening_loopback_socket_blocks_upstream_any_bind(self):
  # Same kernel bind semantics as the Android Os.socket/Os.bind guard; no listener.
  with socket.socket() as guard,socket.socket() as upstream:
   guard.bind(('127.0.0.1',0));port=guard.getsockname()[1]
   upstream.setsockopt(socket.SOL_SOCKET,socket.SO_REUSEADDR,1)
   if hasattr(socket,'SO_REUSEPORT'):upstream.setsockopt(socket.SOL_SOCKET,socket.SO_REUSEPORT,1)
   with self.assertRaises(OSError):upstream.bind(('0.0.0.0',port))
 def test_native_not_loaded_by_terminal_application(self):
  source=(ROOT/'app/src/main/java/com/termux/app/TermuxApplication.java').read_text()
  self.assertNotIn('x11',source.lower());self.assertNotIn('X11Native',source)
  source=(ROOT/'x11/src/main/java/com/termux/x11/X11Service.java').read_text()
  self.assertNotIn('kill -',source);self.assertNotIn('pkill',source)
  self.assertIn('environment().owns(processName(),getPackageName())',source)
  self.assertIn('android.os.Process.killProcess(android.os.Process.myPid())',source)
if __name__=='__main__':unittest.main()
