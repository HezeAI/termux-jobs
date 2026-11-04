"""User-supplied template bytes and real atomic storage in disposable projects only."""
from pathlib import Path
import hashlib,importlib.util,tempfile,unittest,types
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('project_instructions',ROOT/'app/src/main/assets/agent-instructions.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
ASSET=ROOT/'app/src/main/assets/agent-instructions-project.md'
class ProjectTemplate(unittest.TestCase):
 def setUp(self):
  self.tmp=tempfile.TemporaryDirectory(dir=ROOT/'.verification/project-instructions-template');self.root=Path(self.tmp.name);self.home=self.root/'home';self.home.mkdir();self.value=ASSET.read_text();self.probe=patch.object(m.subprocess,'run',return_value=types.SimpleNamespace(returncode=0,stdout='fixture-version'));self.probe.start()
 def tearDown(self):self.probe.stop();self.tmp.cleanup()
 def target(self,agent):
  project=self.root/agent;project.mkdir();other=self.root/(agent+'-other');other.mkdir();(other/'AGENTS.md').write_text('KEEP OTHER');
  cls=m.Editor if agent=='codex' else m.ClaudeEditor
  builtin='agent-instructions-ubuntu-ai.md'if agent=='codex'else'agent-instructions-claude.md'
  editor=cls(ROOT/'app/src/main/assets'/builtin,self.home,{})
  q=dict(scope='project',project=str(project),name='AGENTS.md'if agent=='codex'else'CLAUDE.md')
  return editor,q,project,other
 def save(self,e,q,view,value):return e.save(dict(q,path=view['path'],revision=view['revision'],text=value))
 def test_full_supplied_template_bytes_and_placeholders(self):
  self.assertEqual('3554f8178cfbc032b50bc07f3e937236a66a10dea420da84f3b1a6ed7c6a39e4',hashlib.sha256(ASSET.read_bytes()).hexdigest())
  self.assertNotIn('НАЧАЛО ШАБЛОНА',self.value);self.assertNotIn('КОНЕЦ ШАБЛОНА',self.value)
  for field in ('[название]','[что делает проект]','[путь]','[документ проверок]','[команда проверки]'):self.assertIn(field,self.value)
 def test_read_cancel_then_create_and_replace_exact_edited_text(self):
  for agent in ('codex','claude'):
   e,q,project,other=self.target(agent);view=e.view(q);self.assertFalse(Path(view['path']).exists());self.assertFalse((project/'LEARNING.md').exists())
   edited=self.value.replace('[название]','Synthetic '+agent)+'\nCUSTOM USER TEXT\r\n';one=self.save(e,q,view,edited);self.assertEqual(edited,Path(view['path']).read_bytes().decode());self.assertEqual('KEEP OTHER',(other/'AGENTS.md').read_text())
   prior=Path(view['path']).read_bytes();e.view(q);self.assertEqual(prior,Path(view['path']).read_bytes())
   two=self.save(e,q,one,edited+'REPLACEMENT\n');self.assertEqual(edited+'REPLACEMENT\n',two['text']);self.assertEqual(edited,e.previous(dict(q,id=e.history(q)['history'][0]['id']))['text']);self.assertFalse((project/'LEARNING.md').exists())
 def test_other_process_change_refuses_stale_replace_for_both_agents(self):
  for agent in ('codex','claude'):
   e,q,project,other=self.target(agent);view=e.view(q);Path(view['path']).write_text('OTHER PROCESS REVISION')
   with self.assertRaisesRegex(m.Refused,'EDIT_CONFLICT'):self.save(e,q,view,self.value)
   self.assertEqual('OTHER PROCESS REVISION',Path(view['path']).read_text());self.assertEqual('KEEP OTHER',(other/'AGENTS.md').read_text())
 def test_atomic_replace_error_preserves_existing_file_and_retry(self):
  for agent in ('codex','claude'):
   e,q,project,other=self.target(agent);target=project/q['name'];target.write_text('ORIGINAL');view=e.view(q);replace=m.os.replace
   def fail_target(source,destination,*args,**kw):
    if Path(destination)==target:raise PermissionError('SYNTHETIC WRITE FAILURE')
    return replace(source,destination,*args,**kw)
   with patch.object(m.os,'replace',side_effect=fail_target):
    with self.assertRaises(PermissionError):self.save(e,q,view,self.value)
   self.assertEqual('ORIGINAL',target.read_text());self.assertEqual(self.value,self.save(e,q,e.view(q),self.value)['text']);self.assertEqual('KEEP OTHER',(other/'AGENTS.md').read_text())
if __name__=='__main__':unittest.main()
