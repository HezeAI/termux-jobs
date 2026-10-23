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
