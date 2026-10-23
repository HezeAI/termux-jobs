"""Fixture-only maintenance, no installed rootfs/process changes."""
import importlib.util,tempfile,unittest,os
from pathlib import Path
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('maintenance',ROOT/'x11/src/main/assets/x11-maintenance.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
class MaintenanceTests(unittest.TestCase):
 def test_fedora_lib64_and_org_mozilla_launcher_roundtrip(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   root,home=self.fixture(d);old=root/'usr/lib/firefox/firefox';binary=root/'usr/lib64/firefox/firefox';binary.parent.mkdir(parents=True);old.rename(binary)
   source=root/'usr/share/applications/firefox.desktop';source=source.rename(source.with_name('org.mozilla.firefox.desktop'));original=source.read_bytes()
   m.compatibility(home,root,True);m.compatibility(home,root,True)
   launcher=home/m.FEDORA_LAUNCHER_PATH;self.assertTrue(launcher.exists());self.assertFalse((home/m.LAUNCHER_PATH).exists())
   self.assertIn('Exec=/root/.local/bin/termux-jobs-firefox --new-window %u',launcher.read_text())
   self.assertEqual({m.WRAPPER_PATH,m.FEDORA_LAUNCHER_PATH},set(__import__('json').loads((home/m.STATE_PATH).read_text())))
   m.compatibility(home,root,False);self.assertFalse(launcher.exists());self.assertEqual(original,source.read_bytes())
 def test_fedora_user_launcher_is_preserved_and_unknown_journal_refused(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   root,home=self.fixture(d);source=root/'usr/share/applications/firefox.desktop';source.rename(source.with_name('org.mozilla.firefox.desktop'))
   custom=home/m.FEDORA_LAUNCHER_PATH;custom.parent.mkdir(parents=True);custom.write_text('user launcher')
   with self.assertRaises(ValueError):m.compatibility(home,root,True)
   self.assertEqual('user launcher',custom.read_text());self.assertFalse((home/m.WRAPPER_PATH).exists())
 def fixture(self,tmp):
  root=Path(tmp)/'guest';home=root/'root';home.mkdir(parents=True);b=root/'usr/lib/firefox/firefox';b.parent.mkdir(parents=True);b.write_bytes(b'\x7fELFfixture')
  f=root/'usr/share/applications/firefox.desktop';f.parent.mkdir(parents=True);f.write_text('[Desktop Entry]\nName=Firefox\nName[ru]=Firefox\nExec=firefox %u\n[Desktop Action new-window]\nExec=firefox --new-window %u\n')
  return root,home
 def test_compat_enable_disable_retains_system_launcher_and_only_scopes_firefox(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   root,home=self.fixture(d);source=root/'usr/share/applications/firefox.desktop';before=source.read_bytes();m.compatibility(home,root,True);m.compatibility(home,root,True)
   wrapper=home/m.WRAPPER_PATH;launcher=home/m.LAUNCHER_PATH;self.assertEqual(m.WRAPPER,wrapper.read_bytes());self.assertEqual(0o700,wrapper.stat().st_mode&0o777);self.assertEqual(0o600,launcher.stat().st_mode&0o777)
   self.assertIn('Exec=/root/.local/bin/termux-jobs-firefox --new-window %u',launcher.read_text());self.assertNotIn('MOZ_SHM_NO_SEALS',launcher.read_text());self.assertEqual(before,source.read_bytes())
   m.compatibility(home,root,False);self.assertFalse(wrapper.exists());self.assertFalse(launcher.exists());self.assertFalse((home/m.STATE_PATH).exists());self.assertEqual(before,source.read_bytes());m.compatibility(home,root,False)
 def test_alpine_direct_lib_launcher_and_all_actions_roundtrip(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   root,home=self.fixture(d);source=root/'usr/share/applications/firefox.desktop'
   original='[Desktop Entry]\nExec='+str(root/'usr/lib/firefox/firefox')+' %u\n[Desktop Action private]\nExec='+str(root/'usr/lib/firefox/firefox')+' --private-window %u\n';source.write_text(original)
   m.compatibility(home,root,True);self.assertIn('--private-window %u',(home/m.LAUNCHER_PATH).read_text());m.compatibility(home,root,False);self.assertEqual(original,source.read_text())
 def test_custom_launcher_and_user_edits_are_preserved(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   root,home=self.fixture(d);f=home/m.LAUNCHER_PATH;f.parent.mkdir(parents=True);f.write_text('my launcher')
   with self.assertRaises(ValueError):m.compatibility(home,root,True)
   self.assertEqual('my launcher',f.read_text());self.assertFalse((home/m.STATE_PATH).exists());f.unlink();m.compatibility(home,root,True);f.write_text('user edit')
   with self.assertRaises(ValueError):m.compatibility(home,root,False)
   self.assertEqual('user edit',f.read_text());self.assertTrue((home/m.WRAPPER_PATH).exists())
 def test_symlink_parent_refused_without_changing_outside_file(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   root,home=self.fixture(d);outside=Path(d)/'outside';outside.mkdir();(home/'.local').symlink_to(outside)
   with self.assertRaises(ValueError):m.compatibility(home,root,True)
   self.assertEqual([],list(outside.iterdir()))
 def test_failed_partial_enable_can_be_disabled(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   root,home=self.fixture(d);m.compatibility(home,root,True);(home/m.LAUNCHER_PATH).unlink();m.compatibility(home,root,False);self.assertFalse((home/m.WRAPPER_PATH).exists())
 def test_malformed_source_exec_refuses_before_any_changes(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   root,home=self.fixture(d);(root/'usr/share/applications/firefox.desktop').write_text('[Desktop Entry]\nExec=some-other-program\n')
   with self.assertRaises(ValueError):m.compatibility(home,root,True)
   self.assertFalse((home/'.local').exists())
 def test_generated_shortcuts_are_only_deleted_when_app_absent_without_backups(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   root,home=self.fixture(d);desk=home/'Desktop';desk.mkdir()
   for n,data in m.OLD.items():(desk/n).write_bytes(data)
   f=root/'usr/bin/pcmanfm-qt';f.parent.mkdir(parents=True);f.write_bytes(b'exists');self.assertEqual(0,m.clean_shortcuts(home,root));f.unlink()
   custom=desk/'custom.desktop';custom.write_text('Exec=pcmanfm-qt /my/files');self.assertEqual(3,m.clean_shortcuts(home,root));self.assertEqual(0,m.clean_shortcuts(home,root));self.assertTrue(custom.exists())
   self.assertFalse((home/'.local/state/termux-jobs/retired-lxqt-launchers').exists())
 def test_changed_shortcuts_and_links_are_not_deleted(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   root,home=self.fixture(d);desk=home/'Desktop';desk.mkdir();p=desk/'computer.desktop';p.write_text('user changed');self.assertEqual(0,m.clean_shortcuts(home,root));self.assertTrue(p.exists());p.unlink();other=Path(d)/'other';other.write_bytes(m.OLD['computer.desktop']);p.symlink_to(other)
   with self.assertRaises(ValueError):m.clean_shortcuts(home,root)
   self.assertTrue(other.exists());self.assertTrue(p.is_symlink())
 def test_quick_wallpaper_preserves_repair_but_skips_repeated_image_decoding(self):
  s=importlib.util.spec_from_file_location('wallpaper',ROOT/'x11/src/main/assets/x11-wallpaper.py');w=importlib.util.module_from_spec(s);s.loader.exec_module(w)
  props=['/backdrop/screen0/monitorscreen/workspace%d/last-image'%n for n in range(4)]
  def query(*a):return '\n'.join(props) if a==('-l',) else '/custom/background.jpg' if a[-1].endswith('/last-image') else '1'
  with patch.object(w,'query',side_effect=query) as call,patch.object(w,'decode') as decode:
   w.main(brief=True);self.assertEqual(5,call.call_count);decode.assert_not_called()
  with patch.object(w,'query',side_effect=query) as call,patch.object(w,'decode') as decode:
   w.main();self.assertGreater(call.call_count,5);self.assertEqual(4,decode.call_count)

 def test_quick_and_full_start_preserve_the_same_fallback_repair(self):
  spec=importlib.util.spec_from_file_location('wallpaper',ROOT/'x11/src/main/assets/x11-wallpaper.py');w=importlib.util.module_from_spec(spec);spec.loader.exec_module(w)
  results=[]
  for brief in [False,True]:
   writes=[]
   def query(*args):
    if args==('-l',):return ''
    if '-s' in args:writes.append(args);return ''
    return None
   with patch.object(w,'query',side_effect=query),patch.object(w.os.path,'isfile',side_effect=lambda p:p==w.FALLBACK),patch.object(w,'decode',return_value='ok'):
    w.main(brief=brief)
   results.append(writes)
  self.assertEqual(results[0],results[1]);self.assertEqual(4,len(results[1]))
