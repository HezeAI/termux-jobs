import importlib.util,os,stat,tempfile,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('bookmarks',ROOT/'x11/src/main/assets/x11-bookmarks.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
class BookmarkTests(unittest.TestCase):
 def setUp(self):
  self.temp=tempfile.TemporaryDirectory(dir=ROOT/'.verification');self.home=Path(self.temp.name)/'home';self.home.mkdir();self.file=self.home/'.config/gtk-3.0/bookmarks'
 def tearDown(self):self.temp.cleanup()
 def test_first_start_and_repeated_start_add_only_fixed_english_shortcuts(self):
  self.assertEqual(3,m.prepare(self.home));content=self.file.read_bytes();self.assertEqual(['file:///sdcard Phone storage','file:///sdcard/Download Downloads',self.home.as_uri()+' Home'],content.decode().splitlines());inode=self.file.stat().st_ino;self.assertEqual(0,m.prepare(self.home));self.assertEqual(content,self.file.read_bytes());self.assertEqual(inode,self.file.stat().st_ino)
 def test_user_labels_and_other_bookmarks_preserved_and_duplicates_normalized(self):
  self.file.parent.mkdir(parents=True);original=b'file:///sdcard/ My phone\nfile:///sdcard/Download Custom downloads\nfile:///somewhere Custom';self.file.write_bytes(original);self.file.chmod(0o640)
  self.assertEqual(1,m.prepare(self.home));self.assertTrue(self.file.read_bytes().startswith(original+b'\n'));self.assertEqual(0o640,stat.S_IMODE(self.file.stat().st_mode));self.assertNotIn('Phone storage',self.file.read_text())
 def test_new_environment_home_is_local_and_spaces_encoded(self):
  guest=self.home/'guest home';guest.mkdir();self.assertEqual(3,m.prepare(guest));text=(guest/'.config/gtk-3.0/bookmarks').read_text();self.assertIn('guest%20home Home',text);self.assertNotIn(self.home.as_uri()+' Home',text)
 def test_external_or_relative_config_is_preserved(self):
  outside=Path(self.temp.name)/'outside'
  for config in [outside,Path('relative'),self.home/'../outside']:
   with self.assertRaises(ValueError):m.prepare(self.home,config)
  self.assertFalse(outside.exists())
 def test_linked_file_or_directory_preserves_target(self):
  victim=Path(self.temp.name)/'keep';victim.write_text('keep');self.file.parent.mkdir(parents=True);self.file.symlink_to(victim)
  with self.assertRaises(ValueError):m.prepare(self.home)
  self.assertEqual('keep',victim.read_text());self.file.unlink();self.file.parent.rmdir();self.file.parent.symlink_to(victim.parent)
  with self.assertRaises(ValueError):m.prepare(self.home)
  self.assertEqual('keep',victim.read_text())
 def test_nonregular_large_and_nonutf8_files_preserved(self):
  self.file.parent.mkdir(parents=True)
  for content in [b'\xff',b'x'*(m.MAX_BYTES+1)]:
   self.file.write_bytes(content)
   with self.assertRaises((ValueError,UnicodeError)):m.prepare(self.home)
   self.assertEqual(content,self.file.read_bytes())
  self.file.unlink();os.mkfifo(self.file)
  with self.assertRaises(ValueError):m.prepare(self.home)
 def test_custom_config_inside_home_is_honored(self):
  config=self.home/'custom';self.assertEqual(3,m.prepare(self.home,config));self.assertTrue((config/'gtk-3.0/bookmarks').is_file());self.assertFalse(self.file.exists())
 def test_common_session_wiring_runs_before_xfce_for_every_target(self):
  text=(ROOT/'x11/src/main/assets/x11-session.sh').read_text();self.assertLess(text.index('x11-bookmarks.py'),text.index('fcitx5 &'));self.assertIn('python3 -B "$jobs_control/x11-bookmarks.py"',text);self.assertIn('asset(context,base,"x11-bookmarks.py")',(ROOT/'x11/src/main/java/com/termux/x11/DesktopProcess.java').read_text())
