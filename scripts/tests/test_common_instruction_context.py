"""Updated common templates remain opt-in for existing rules in both agents/locales."""
from pathlib import Path
import importlib.util,tempfile,unittest
ROOT=Path(__file__).resolve().parents[2]
ASSETS=ROOT/'app/src/main/assets'
LEGACY=ROOT/'scripts/tests/fixtures/legacy-instructions'
spec=importlib.util.spec_from_file_location('common_instructions',ASSETS/'agent-instructions.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
class CommonContext(unittest.TestCase):
 def test_codex_templates_are_adapted_for_the_selected_termux_jobs_environment(self):
  for language in ('','.en'):
   codex=((ASSETS if language else LEGACY)/f'agent-instructions-ubuntu-ai{language}.md').read_text()
   self.assertIn('Termux Job’s',codex);self.assertIn('Ubuntu AI',codex)
   self.assertIn('AGENTS.md',codex);self.assertIn('LEARNING.md',codex)
   self.assertNotIn('DevKit',codex);self.assertNotIn('devkit-build-apk',codex)
   self.assertIn('reasoning_effort="medium"',codex);self.assertIn('fork_turns="none"',codex)
 def test_preservation_preview_explicit_save_and_conflict_in_four_variants(self):
  for agent,cls,filename in (('ubuntu-ai',m.Editor,'AGENTS.md'),('claude',m.ClaudeEditor,'CLAUDE.md')):
   for lang in ('','.en'):
    with self.subTest(agent=agent,language=lang),tempfile.TemporaryDirectory(dir=ROOT/'.verification') as temp:
     home=Path(temp);template=(ASSETS if lang else LEGACY)/f'agent-instructions-{agent}{lang}.md';editor=cls(template,home,{})
     path=editor.location({});path.parent.mkdir();path.write_text('SYNTHETIC USER RULES\n')
     self.assertEqual(filename,path.name);self.assertFalse(editor.initialize()['created'])
     preview=editor.view({});self.assertEqual('SYNTHETIC USER RULES\n',preview['text'])
     self.assertEqual(template.read_text(),preview['template']);self.assertEqual('SYNTHETIC USER RULES\n',path.read_text())
     editor.save(dict(path=str(path),revision=preview['revision'],text=preview['template']+'\nEDITED BY USER\n'))
     self.assertEqual(template.read_text()+'\nEDITED BY USER\n',path.read_text())
     stale=editor.view({});path.write_text('OTHER WRITER\n')
     with self.assertRaisesRegex(m.Refused,'EDIT_CONFLICT'):
      editor.save(dict(path=str(path),revision=stale['revision'],text=preview['template']))
     self.assertEqual('OTHER WRITER\n',path.read_text())
 def test_empty_saved_rules_are_not_replaced(self):
  for agent,cls in (('ubuntu-ai',m.Editor),('claude',m.ClaudeEditor)):
   for lang in ('','.en'):
    with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as temp:
     editor=cls((ASSETS if lang else LEGACY)/f'agent-instructions-{agent}{lang}.md',Path(temp),{})
     path=editor.location({});path.parent.mkdir();path.touch()
     self.assertFalse(editor.initialize()['created']);self.assertEqual('',editor.view({})['text']);self.assertEqual(b'',path.read_bytes())
if __name__=='__main__':unittest.main()
