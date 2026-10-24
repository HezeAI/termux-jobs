from pathlib import Path
import hashlib,importlib.util,tempfile,unittest
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[2]
s=importlib.util.spec_from_file_location('codec',ROOT/'x11/src/main/assets/x11-openh264.py');m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
class CodecTests(unittest.TestCase):
 def fixture(self,base):
  home=base/'home';system=base/'system';source=base/'codec.so';source.write_bytes(b'known decoder');p=system/'usr/share/applications/org.xfce.Parole.desktop';p.parent.mkdir(parents=True);p.write_text('[Desktop Entry]\nName=Parole\nExec=parole %U\n');return home,system,source
 def test_private_setup_restart_and_idempotence_preserve_system_launcher(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d,patch.object(m,'DIGEST',hashlib.sha256(b'known decoder').hexdigest()):
   home,system,source=self.fixture(Path(d));self.assertFalse(m.ready(home));m.install(home,system,source);self.assertTrue(m.ready(home));m.install(home,system,source)
   self.assertIn('Exec=parole %U',(system/'usr/share/applications/org.xfce.Parole.desktop').read_text());self.assertIn('LD_LIBRARY_PATH',(home/m.WRAPPER).read_text());self.assertFalse((system/'usr/lib64/libopenh264.so.8').exists())
 def test_mismatched_asset_user_overrides_and_symlinks_are_preserved(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   home,system,source=self.fixture(Path(d))
   with self.assertRaisesRegex(ValueError,'integrity'):m.install(home,system,source)
   self.assertFalse(home.exists())
   with patch.object(m,'DIGEST',hashlib.sha256(source.read_bytes()).hexdigest()):
    user=home/m.LAUNCHER;user.parent.mkdir(parents=True);user.write_text('user launcher')
    with self.assertRaisesRegex(ValueError,'local changes'):m.install(home,system,source)
    self.assertEqual('user launcher',user.read_text());user.unlink();user.symlink_to(source)
    with self.assertRaisesRegex(ValueError,'link'):m.install(home,system,source)
 def test_interrupted_setup_is_recoverable_but_user_edits_are_not_overwritten(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d,patch.object(m,'DIGEST',hashlib.sha256(b'known decoder').hexdigest()):
   home,system,source=self.fixture(Path(d));m.install(home,system,source);(home/m.WRAPPER).unlink();self.assertFalse(m.ready(home));m.install(home,system,source);self.assertTrue(m.ready(home));(home/m.WRAPPER).write_text('edited')
   with self.assertRaisesRegex(ValueError,'local changes'):m.install(home,system,source)
   self.assertEqual('edited',(home/m.WRAPPER).read_text())

 def test_fedora_plugin_is_pinned_arm64_and_has_separate_registry(self):
  data=(ROOT/'.tools/x11-runtime/assets/x11-gst-openh264.so').read_bytes();self.assertEqual(m.PLUGIN_DIGEST,hashlib.sha256(data).hexdigest());self.assertEqual(b'\x7fELF',data[:4]);self.assertEqual(183,int.from_bytes(data[18:20],'little'))
  with patch.dict(m.os.environ,{'LD_LIBRARY_PATH':'/existing/lib','GST_PLUGIN_PATH_1_0':'/existing/plugins'}):
   env=m.fedora_environment(Path('/root'));self.assertTrue(env['LD_LIBRARY_PATH'].endswith(':/existing/lib'));self.assertTrue(env['GST_PLUGIN_PATH_1_0'].endswith(':/existing/plugins'));self.assertEqual('/root/'+m.REGISTRY,env['GST_REGISTRY']);self.assertEqual(env['GST_REGISTRY'],env['GST_REGISTRY_1_0'])
 def test_fedora_upgrade_adds_owned_plugin_without_changing_system_files(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d,patch.object(m,'DIGEST',hashlib.sha256(b'known decoder').hexdigest()),patch.object(m,'PLUGIN_DIGEST',hashlib.sha256(b'plugin').hexdigest()),patch.object(m,'verify_fedora') as verify:
   home,system,source=self.fixture(Path(d));plugin=Path(d)/'plugin.so';plugin.write_bytes(b'plugin');original=(system/'usr/share/applications/org.xfce.Parole.desktop').read_bytes()
   m.install(home,system,source);self.assertTrue(m.ready(home));self.assertFalse(m.ready(home,True));m.install(home,system,source,plugin);m.install(home,system,source,plugin)
   self.assertTrue(m.ready(home,True));verify.assert_called_with(home,system);wrapper=(home/m.WRAPPER).read_text();self.assertIn('GST_PLUGIN_PATH_1_0',wrapper);self.assertIn('parole-gst-registry.bin',wrapper);self.assertEqual(original,(system/'usr/share/applications/org.xfce.Parole.desktop').read_bytes());self.assertFalse((system/'usr/lib64/libopenh264.so.8').exists());self.assertFalse((system/'usr/lib64/gstreamer-1.0/libgstopenh264.so').exists())
   (home/m.PLUGIN).write_bytes(b'user edit');self.assertFalse(m.ready(home,True))
   with self.assertRaisesRegex(ValueError,'local changes'):m.install(home,system,source,plugin)
   self.assertEqual(b'user edit',(home/m.PLUGIN).read_bytes())
 def test_fedora_failed_runtime_probe_stays_unready_and_can_resume(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d,patch.object(m,'DIGEST',hashlib.sha256(b'known decoder').hexdigest()),patch.object(m,'PLUGIN_DIGEST',hashlib.sha256(b'plugin').hexdigest()):
   home,system,source=self.fixture(Path(d));plugin=Path(d)/'plugin.so';plugin.write_bytes(b'plugin')
   with patch.object(m,'verify_fedora',side_effect=ValueError('component missing')),self.assertRaisesRegex(ValueError,'component missing'):m.install(home,system,source,plugin)
   self.assertFalse(m.ready(home,True));self.assertEqual(b'pending',(home/m.PROOF).read_bytes())
   with patch.object(m,'verify_fedora'):m.install(home,system,source,plugin)
   self.assertTrue(m.ready(home,True));(home/m.PROOF).write_bytes(b'personal proof')
   with patch.object(m,'verify_fedora') as verify,self.assertRaisesRegex(ValueError,'local changes'):m.install(home,system,source,plugin)
   verify.assert_not_called();self.assertEqual(b'personal proof',(home/m.PROOF).read_bytes())
 def test_fedora_invalid_plugin_does_not_create_home_or_state(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d,patch.object(m,'DIGEST',hashlib.sha256(b'known decoder').hexdigest()):
   home,system,source=self.fixture(Path(d));plugin=Path(d)/'plugin.so';plugin.write_bytes(b'corrupt')
   with self.assertRaisesRegex(ValueError,'integrity'):m.install(home,system,source,plugin)
   self.assertFalse(home.exists())
