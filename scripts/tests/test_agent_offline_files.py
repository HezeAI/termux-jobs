import importlib.util,json,tempfile,unittest,sqlite3
from contextlib import closing
from pathlib import Path
from unittest.mock import patch
spec=importlib.util.spec_from_file_location('offline',Path(__file__).resolve().parents[2]/'app/src/main/assets/agent-offline-files.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
class OfflineFiles(unittest.TestCase):
 def setUp(self):
  self.tmp=tempfile.TemporaryDirectory();self.home=Path(self.tmp.name);self.mock=patch.object(m.Path,'home',return_value=self.home);self.mock.start();self.thread='12345678-1234-1234-1234-123456789abc'
 def tearDown(self):self.mock.stop();self.tmp.cleanup()
 def write(self,agent,value):
  relative={'claude':'.claude/projects/project/'+self.thread+'.jsonl','codex':'.codex/sessions/2026/11/08/rollout-'+self.thread+'.jsonl','gemini':'.gemini/tmp/project/chats/session-'+self.thread+'.json'}[agent];p=self.home/relative;p.parent.mkdir(parents=True);p.write_text(json.dumps(value)+'\n');return p
 def test_codex_and_claude_image_read(self):
  for agent,entry in [('claude',{'type':'tool_use','name':'Read','input':{'file_path':'/project/photo.png'}}),('codex',{'type':'function_call','name':'view_image','arguments':'{"path":"/project/photo.png"}'})]:
   p=self.write(agent,entry);before=p.read_bytes();r=m.main(dict(agent=agent,thread=self.thread));self.assertEqual(r['rows'][0]['fileReferences'][0]['path'],'/project/photo.png');self.assertEqual(p.read_bytes(),before)
 def test_user_attachment_and_gemini(self):
  self.write('gemini',{'sessionId':self.thread,'role':'user','text':'[TermuxJobs attachment:'+'a'*32+']\nName: a.png\nFile: /tmp/chat-attachments/'+'a'*32+'/original.png\nSize: 42 bytes.'});r=m.main(dict(agent='gemini',thread=self.thread));self.assertEqual(r['rows'][0]['kind'],'user')
 def test_gemini_short_filename_verifies_full_session_id(self):
  p=self.write('gemini',{'sessionId':self.thread,'role':'user','content':'[photo](/tmp/photo.png)'});p.rename(p.with_name('session-2026-'+self.thread[:8]+'.json'))
  self.assertTrue(m.main(dict(agent='gemini',thread=self.thread))['rows'])
  with self.assertRaises(FileNotFoundError):m.main(dict(agent='gemini',thread=self.thread[:-1]+'d'))
 def test_missing_and_invalid(self):
  with self.assertRaises(FileNotFoundError):m.main(dict(agent='claude',thread=self.thread))
  with self.assertRaises(ValueError):m.main(dict(agent='claude',thread='../auth'))
 def test_pagination(self):
  p=self.write('claude',{});p.write_text(('{}\n')*301);r=m.main(dict(agent='claude',thread=self.thread));self.assertEqual(r['next'],900);self.assertIsNone(m.main(dict(agent='claude',thread=self.thread,offset=900))['next'])
 def test_no_inline_image_payload(self):self.assertEqual(m.rows({'type':'image','source':{'data':'secretbase64'}},'x'),[])
 def test_opencode(self):
  p=self.home/'.local/share/opencode/opencode.db';p.parent.mkdir(parents=True)
  with closing(sqlite3.connect(p)) as db:
   db.execute('CREATE TABLE part(id TEXT,data TEXT,session_id TEXT,time_created INT)');db.execute('INSERT INTO part VALUES(?,?,?,?)',('p',json.dumps({'type':'file','url':'file:///tmp/p.png'}),self.thread,1));db.commit()
  self.assertEqual(m.main(dict(agent='opencode',thread=self.thread))['rows'][0]['fileReferences'][0]['path'],'/tmp/p.png')
if __name__=='__main__':unittest.main()
