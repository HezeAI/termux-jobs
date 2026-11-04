"""Preflight must refuse working/default connections before spawning anything."""
import unittest,tempfile,json,hashlib,os,uuid,fcntl,types
from pathlib import Path
from unittest.mock import patch
import opencode_fixture as fixture
class Safety(unittest.TestCase):
 def setUp(self):
  self.tmp=tempfile.TemporaryDirectory(dir=fixture.REPO/'.verification/phone-permissions');self.root=Path(self.tmp.name);(self.root/'project').mkdir();(self.root/'runs').mkdir();(self.root/'xdg/config/opencode').mkdir(parents=True);self.before=Path.cwd();os.chdir(self.root/'project')
  scripts={n:hashlib.sha256(Path(fixture.__file__).with_name(n).read_bytes()).hexdigest()for n in ['opencode_fixture.py','opencode_provider.py','opencode_binary.py']};(self.root/'identity.json').write_text(json.dumps({'scripts':scripts}))
  self.child=patch.object(fixture.subprocess,'Popen',side_effect=AssertionError('Unexpected child'));self.start=patch.object(fixture.Provider,'start',side_effect=AssertionError('Unexpected provider'));self.child.start();self.start.start()
 def tearDown(self):self.start.stop();self.child.stop();os.chdir(self.before);self.tmp.cleanup()
 def command(self,path,access='ask'):
  return ['/bin/bash','--noprofile','--norc','-c',"node bridge run-chat '"+str(path)+"' socket sesFake '' build new '' "+access+' unrestricted']
 def connection(self):return self.root/'connections'/str(uuid.uuid4())
 def test_default_is_rejected(self):
  with self.assertRaisesRegex(AssertionError,'NEW named'):fixture.launch(self.root,self.command(Path('/tmp/termux-jobs-ai/connections')/fixture.DEFAULT))
  self.assertFalse((self.root/'claimed-connection.json').exists())
 def test_existing_is_never_read_or_written(self):
  target=self.connection();target.mkdir(parents=True);sentinel=target/'artificial.txt';sentinel.write_text('KEEP');before=sentinel.stat().st_mtime_ns
  self.start.stop();self.child.stop()
  fake=types.SimpleNamespace(start=lambda:None,stop=lambda:None,url='http://127.0.0.1:1',events=[])
  child=types.SimpleNamespace(wait=lambda:0,poll=lambda:0)
  with patch.object(fixture,'Provider',return_value=fake),patch.object(fixture.subprocess,'Popen',return_value=child)as spawn:
   self.assertEqual(0,fixture.launch(self.root,self.command(target)))
   words=fixture.shlex.split(spawn.call_args.args[0][4]);index=words.index('run-chat');self.assertEqual(str(self.root/'bridge/connections'/target.name),words[index+1]);self.assertEqual(['socket','sesFake','','build','new','','ask','unrestricted'],words[index+2:index+10])
  self.assertEqual('KEEP',sentinel.read_text());self.assertEqual(before,sentinel.stat().st_mtime_ns);self.assertEqual(['artificial.txt'],sorted(p.name for p in target.iterdir()))
  self.start.start();self.child.start()
 def test_foreign_owner_is_rejected(self):
  owner=self.root/'claimed-connection.json';owner.write_text(json.dumps({'id':'different','path':'synthetic'}));before=owner.read_bytes()
  with self.assertRaisesRegex(AssertionError,'different connection'):fixture.launch(self.root,self.command(self.connection()))
  self.assertEqual(before,owner.read_bytes())
 def test_full_access_is_rejected(self):
  with self.assertRaisesRegex(AssertionError,'Build / ask'):fixture.launch(self.root,self.command(self.connection(),'full'))
  self.assertFalse((self.root/'claimed-connection.json').exists())
 def test_second_launch_is_rejected(self):
  with (self.root/'launch.lock').open('a')as held:
   fcntl.flock(held,fcntl.LOCK_EX|fcntl.LOCK_NB)
   with self.assertRaises(BlockingIOError):fixture.launch(self.root,self.command(self.connection()))
  self.assertFalse((self.root/'claimed-connection.json').exists())
if __name__=='__main__':unittest.main()
