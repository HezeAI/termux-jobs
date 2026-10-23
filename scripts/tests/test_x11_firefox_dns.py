"""Isolated profile fixtures; never touch installed Firefox data."""
import importlib.util,tempfile,unittest,json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
s=importlib.util.spec_from_file_location('dns',ROOT/'x11/src/main/assets/x11-firefox-dns.py');m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
class DnsTests(unittest.TestCase):
 def fixture(self,d):
  h=Path(d);r=h/'.mozilla/firefox';(r/'default').mkdir(parents=True);(r/'profiles.ini').write_text('[Profile0]\nPath=default\nIsRelative=1\n');return h,r/'default/user.js'
 def test_roundtrip_existing_and_absent(self):
  for before in [None,b'user_pref("example", 1);\n']:
   with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
    h,p=self.fixture(d)
    if before is not None:p.write_bytes(before)
    self.assertEqual('off',m.state(h));m.configure(h,True);self.assertEqual((before or b'')+m.BLOCK,p.read_bytes());self.assertEqual('on',m.state(h));m.configure(h,True);m.configure(h,False);m.configure(h,False)
    self.assertEqual(before,p.read_bytes() if p.exists() else None);self.assertEqual('off',m.state(h))
 def test_local_edits_block_undo_and_are_preserved(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   h,p=self.fixture(d);m.configure(h,True);p.write_bytes(p.read_bytes()+b'edit');self.assertEqual('unknown',m.state(h))
   with self.assertRaises(ValueError):m.configure(h,False)
   self.assertTrue(p.read_bytes().endswith(b'edit'));self.assertTrue((h/m.STATE).exists())
 def test_interrupted_apply_and_undo_recover(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   h,p=self.fixture(d);p.write_bytes(b'original');m.configure(h,True);p.write_bytes(b'original');m.configure(h,True);self.assertEqual(b'original'+m.BLOCK,p.read_bytes());p.write_bytes(b'original');m.configure(h,False);self.assertEqual(b'original',p.read_bytes())
 def test_symlink_and_external_profile_refused(self):
  for path in ['../../outside','/tmp/outside']:
   with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
    h,p=self.fixture(d);(h/'.mozilla/firefox/profiles.ini').write_text('[Profile0]\nPath='+path+'\n')
    with self.assertRaises(ValueError):m.configure(h,True)
    self.assertFalse((h/m.STATE).exists())
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   h,p=self.fixture(d);other=h/'other';other.write_bytes(b'keep');p.symlink_to(other)
   with self.assertRaises(ValueError):m.configure(h,True)
   self.assertEqual(b'keep',other.read_bytes())
 def test_missing_malformed_and_oversized_profiles(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   h,p=self.fixture(d);ini=h/'.mozilla/firefox/profiles.ini';ini.write_text('bad')
   with self.assertRaises(ValueError):m.configure(h,True)
   ini.unlink()
   with self.assertRaises(ValueError):m.configure(h,True)
 def test_corrupt_journal_cannot_write_foreign_profile(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   h,p=self.fixture(d);m.configure(h,True);j=h/m.STATE;data=json.loads(j.read_text());next(iter(data.values()))['after']='0'*64;j.write_text(json.dumps(data));self.assertEqual('unknown',m.state(h))
   with self.assertRaises(ValueError):m.configure(h,False)
   self.assertEqual(m.BLOCK,p.read_bytes())

 def test_undo_restores_only_dns_keys_in_prefs_after_browser_use(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   h,p=self.fixture(d);prefs=p.with_name('prefs.js');prefs.write_bytes(b'user_pref("network.trr.mode", 0);\nuser_pref("other", 1);\n');m.configure(h,True)
   prefs.write_bytes(b'user_pref("other", 2);\n'+b'\n'.join(m.pref_lines(m.BLOCK))+b'\n');m.configure(h,False)
   self.assertIn(b'user_pref("other", 2);',prefs.read_bytes());self.assertIn(b'user_pref("network.trr.mode", 0);',prefs.read_bytes());self.assertNotIn(b'mozilla.cloudflare',prefs.read_bytes())
 def test_changed_dns_preferences_prevent_all_restore_writes(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   h,p=self.fixture(d);m.configure(h,True);p.with_name('prefs.js').write_bytes(b'user_pref("network.trr.mode", 3);\n')
   with self.assertRaises(ValueError):m.configure(h,False)
   self.assertEqual(m.BLOCK,p.read_bytes());self.assertTrue((h/m.STATE).exists())

 def test_current_xdg_and_legacy_profiles_both_roundtrip(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   h,legacy=self.fixture(d);root=h/'.config/mozilla/firefox';(root/'new').mkdir(parents=True);(root/'profiles.ini').write_text('[Profile0]\nPath=new\nIsRelative=1\n');current=root/'new/user.js'
   m.configure(h,True);self.assertEqual(m.BLOCK,current.read_bytes());self.assertEqual(m.BLOCK,legacy.read_bytes());m.configure(h,False);self.assertFalse(current.exists());self.assertFalse(legacy.exists())
 def test_current_xdg_only_profile_roundtrip(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   h,p=self.fixture(d);root=h/'.config/mozilla';root.mkdir(parents=True);(h/'.mozilla/firefox').rename(root/'firefox');p=root/'firefox/default/user.js'
   m.configure(h,True);self.assertEqual('on',m.state(h));self.assertEqual(m.BLOCK,p.read_bytes());m.configure(h,False);self.assertFalse(p.exists())
