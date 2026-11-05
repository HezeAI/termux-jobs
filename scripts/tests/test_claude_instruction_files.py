"""Claude instruction editor fixtures. No real accounts, rootfs or session control."""
from pathlib import Path
import unittest,tempfile,importlib.util,os,types
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('instruction_files',ROOT/'app/src/main/assets/agent-instructions.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
class ClaudeFiles(unittest.TestCase):
 def setUp(self):
  self.tmp=tempfile.TemporaryDirectory(dir=ROOT/'.verification');self.root=Path(self.tmp.name);self.home=self.root/'home';self.home.mkdir();self.project=self.root/'project';self.project.mkdir();(self.project/'.git').mkdir();self.config=self.root/'claude-config';self.template=ROOT/'app/src/main/assets/agent-instructions-claude.md';self.e=m.ClaudeEditor(self.template,self.home,{'CLAUDE_CONFIG_DIR':str(self.config)})
 def tearDown(self):self.tmp.cleanup()
 def q(self,name='CLAUDE.md',**kw):return dict(scope='project',project=str(self.project),name=name,**kw)
 def save(self,q,text):
  v=self.e.view(q);return self.e.save(dict(q,path=v['path'],revision=v['revision'],text=text))
 def test_four_supplied_templates_preserve_existing_empty_files_and_reject_stale_save(self):
  for agent in ['codex','claude']:
   for language in ['ru','en']:
    with self.subTest(agent=agent,language=language):
     home=self.root/(agent+'-'+language);home.mkdir();config=home/('.codex' if agent=='codex' else '.claude');config.mkdir()
     target=config/('AGENTS.md' if agent=='codex' else 'CLAUDE.md');target.write_bytes(b'')
     asset='agent-instructions-'+('ubuntu-ai' if agent=='codex' else 'claude')+('.en' if language=='en' else '')+'.md'
     cls=m.Editor if agent=='codex' else m.ClaudeEditor;e=cls(ROOT/'app/src/main/assets'/asset,home,{})
     self.assertFalse(e.initialize()['created']);self.assertEqual(b'',target.read_bytes())
     view=e.view({});self.assertEqual('',view['text']);self.assertEqual(str(target),view['path'])
     text=(ROOT/'app/src/main/assets'/asset).read_text();target.write_text('external change')
     with self.assertRaisesRegex(m.Refused,'EDIT_CONFLICT'):e.save(dict(path=view['path'],revision=view['revision'],text=text))
     self.assertEqual('external change',target.read_text());current=e.view({})
     e.save(dict(path=current['path'],revision=current['revision'],text=text));self.assertEqual(text.encode(),target.read_bytes())
 def test_project_base_presence_does_not_confuse_additive_local_file(self):
  (self.project/'CLAUDE.local.md').write_text('local')
  self.assertFalse(self.e.view(self.q('CLAUDE.local.md'))['projectBaseExists'])
  (self.project/'CLAUDE.md').write_text('base')
  self.assertTrue(self.e.view(self.q('CLAUDE.local.md'))['projectBaseExists'])
  self.assertTrue(self.e.view(self.q())['projectBaseExists'])
  self.assertEqual('local',(self.project/'CLAUDE.local.md').read_text())
 def test_global_custom_config_absence_and_explicit_creation(self):
  v=self.e.view({});self.assertEqual(str(self.config/'CLAUDE.md'),v['path']);self.assertFalse(self.config.exists());self.assertFalse(self.e.initialize()['created']);self.assertFalse(self.config.exists());self.save({},'custom');self.assertEqual('custom',self.e.view({})['text']);self.assertFalse((self.home/'.codex').exists())
 def test_project_claude_and_local_are_additive_no_override_target(self):
  self.save(self.q(),'base');self.save(self.q('CLAUDE.local.md'),'local');self.assertEqual('base',self.e.view(self.q())['text']);self.assertIsNone(self.e.view(self.q())['maskedBy']);self.assertTrue(self.e.view(self.q('CLAUDE.local.md'))['contentPresent']);self.assertIsNone(self.e.view(self.q())['active'])
  with self.assertRaisesRegex(m.Refused,'INVALID_SCOPE'):self.e.view(self.q('AGENTS.override.md'))
 def test_dot_claude_missing_read_does_not_create_directory(self):
  q=self.q('.claude/CLAUDE.md');v=self.e.view(q);self.assertFalse(v['exists']);self.assertFalse((self.project/'.claude').exists());self.save(q,'nested');self.assertEqual('nested',(self.project/'.claude/CLAUDE.md').read_text())
 def test_save_history_recovery_conflict_and_exact_import_text(self):
  q=self.q();self.save(q,'before');v=self.e.view(q);self.save(q,'\ufeffImported\\r\\n');h=self.e.history(q)['history'];self.assertEqual('before',self.e.previous(dict(q,id=h[0]['id']))['text'])
  with self.assertRaisesRegex(m.Refused,'EDIT_CONFLICT'):self.e.save(dict(q,path=v['path'],revision=v['revision'],text='stale'))
  self.save(q,'before');self.assertEqual(2,len(self.e.history(q)['history']))
 def test_memory_binding_from_codex_or_claude_uses_one_file(self):
  shared=self.root/'LEARNING.md';shared.write_text('shared');(self.project/'AGENTS.md').write_text('Memory `'+str(shared)+'`');q=self.q('LEARNING.md');self.assertEqual(str(shared),self.e.view(q)['path']);self.save(q,'updated');self.assertFalse((self.project/'LEARNING.md').exists());self.assertEqual('updated',shared.read_text())
  (self.project/'AGENTS.md').unlink();(self.project/'CLAUDE.md').write_text('Memory `'+str(shared)+'`');self.assertEqual(str(shared),self.e.view(q)['path'])
 def test_explicit_shared_path_and_unsafe_symlinks(self):
  q=self.q('LEARNING.md',memoryPath=str(self.root/'LEARNING.md'));self.save(q,'chosen');self.assertFalse((self.project/'LEARNING.md').exists());(self.project/'CLAUDE.local.md').symlink_to(self.root/'LEARNING.md')
  with self.assertRaises(m.Refused):self.e.view(self.q('CLAUDE.local.md'))
 def test_at_import_memory_path_uses_existing_common_file_without_copy(self):
  target=self.root/'LEARNING.md';target.write_text('common');(self.project/'CLAUDE.md').write_text('@../LEARNING.md')
  self.assertEqual(str(target),self.e.view(self.q('LEARNING.md'))['path']);self.assertFalse((self.project/'LEARNING.md').exists())
 def test_claude_templates_equal_latest_codex_semantics(self):
  for src,dest in [('agent-instructions-ubuntu-ai.md','agent-instructions-claude.md'),('agent-instructions-ubuntu-ai.en.md','agent-instructions-claude.en.md')]:
   a=(ROOT/'app/src/main/assets'/src).read_text();b=(ROOT/'app/src/main/assets'/dest).read_text();expected=a.replace('AGENTS.md','CLAUDE.md').replace('# Общие инструкции Ubuntu AI','# Общие инструкции Claude Code в Ubuntu').replace('# General Ubuntu AI instructions','# General Claude Code instructions in Ubuntu').replace('Не дублируй в нём общие инструкции Ubuntu AI.','Не дублируй в нём общие инструкции Claude Code.').replace('Do not duplicate general Ubuntu AI instructions in it.','Do not duplicate general Claude Code instructions in it.')
   self.assertEqual(expected,b);self.assertIn('LEARNING.md',b)
 def test_diagnostics_reports_only_sources_no_auth_or_setting_content(self):
  self.save({},'shared');self.save(self.q(),'project');self.save(self.q('CLAUDE.local.md'),'local');self.config.joinpath('settings.json').write_text('{"private":"MUST_NOT_APPEAR"}');self.config.joinpath('.credentials.json').write_text('MUST_NOT_READ')
  with patch.object(m.shutil,'which',return_value='/fixture/claude'),patch.object(m.subprocess,'run',return_value=types.SimpleNamespace(returncode=0,stdout='2.1.296 (Claude Code)\n')):
   d=self.e.diagnostics(self.q());self.assertTrue(d['cli']['behaviorVerified']);self.assertEqual(str(self.config),d['claudeHome']);self.assertTrue(any(s['path'].endswith('CLAUDE.local.md') for s in d['sources']));self.assertNotIn('MUST_NOT',str(d));self.assertNotIn('.credentials',str(d))
if __name__=='__main__':unittest.main()
