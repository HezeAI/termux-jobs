from pathlib import Path
import importlib.util,tempfile,unittest,os,json
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('repair',ROOT/'x11/src/main/assets/x11-launchers.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
class LauncherPermissionsTests(unittest.TestCase):
 def test_explicit_repair_preserves_contents_modes_and_original_backup(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as tmp:
   base=Path(tmp);apps=base/'apps';apps.mkdir();manifest=base/'backup.json'
   for mode in [0,0o444,0o644,0o755]:
    p=apps/f'{mode:o}.desktop';p.write_text('Name=Test\n');p.chmod(mode)
   result=m.repair([apps],manifest);self.assertEqual(2,result['eligible']);self.assertFalse(manifest.exists())
   result=m.repair([apps],manifest,True);self.assertEqual(2,result['repaired'])
   for mode in [0,0o444,0o644,0o755]:
    p=apps/f'{mode:o}.desktop';self.assertEqual(mode|0o600,p.stat().st_mode&0o777);self.assertEqual('Name=Test\n',p.read_text())
   saved=json.loads(manifest.read_text());self.assertEqual({0,0o444},{v['mode'] for v in saved.values()})
   self.assertEqual(0,m.repair([apps],manifest,True)['repaired']);self.assertEqual(saved,json.loads(manifest.read_text()))
 def test_symlink_targets_and_unrelated_files_are_not_modified(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as tmp:
   base=Path(tmp);apps=base/'apps';apps.mkdir();target=base/'target';target.write_text('x');target.chmod(0o444);(apps/'link.desktop').symlink_to(target);other=apps/'other.txt';other.write_text('y');other.chmod(0o444)
   self.assertEqual(0,m.repair([apps],base/'backup',True)['repaired']);self.assertEqual(0o444,target.stat().st_mode&0o777);self.assertEqual(0o444,other.stat().st_mode&0o777)

 def test_unowned_or_modified_launchers_are_not_changed(self):
  import hashlib
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as tmp:
   base=Path(tmp);apps=base/'apps';apps.mkdir();known=apps/'known.desktop';unknown=apps/'unknown.desktop';changed=apps/'changed.desktop'
   for p in [known,unknown,changed]:p.write_text('original');p.chmod(0o444)
   checks={str(known):hashlib.md5(b'original').hexdigest(),str(changed):hashlib.md5(b'expected other contents').hexdigest()}
   result=m.repair([apps],base/'backup',True,checks);self.assertEqual(1,result['repaired']);self.assertEqual(2,result['unverified']);self.assertEqual(0o644,known.stat().st_mode&0o777);self.assertEqual(0o444,unknown.stat().st_mode&0o777);self.assertEqual(0o444,changed.stat().st_mode&0o777)
