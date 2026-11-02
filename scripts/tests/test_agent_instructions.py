"""Private fixture-only instruction storage tests; no auth or installed environment access."""
from pathlib import Path
import importlib.util,json,os,tempfile,unittest,types,concurrent.futures
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('instructions',ROOT/'app/src/main/assets/agent-instructions.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
class Instructions(unittest.TestCase):
 def setUp(self):
  self.temp=tempfile.TemporaryDirectory(dir=ROOT/'.verification');self.root=Path(self.temp.name);self.home=self.root/'home';self.home.mkdir();self.project=self.root/'project';self.project.mkdir();(self.project/'.git').mkdir();self.template=self.root/'template.md';self.template.write_bytes(b'TEMPLATE\n');self.editor=m.Editor(self.template,self.home,{});self.version_probe=patch.object(m.subprocess,'run',return_value=types.SimpleNamespace(returncode=0,stdout='codex-cli 0.164.0\n'));self.version_probe.start();self.binary_probe=patch.object(m.shutil,'which',return_value='/fixture/codex');self.binary_probe.start()
 def tearDown(self):self.binary_probe.stop();self.version_probe.stop();self.temp.cleanup()
 def read(self,**kw):return self.editor.call(dict(operation='read',**kw))
 def save(self,view,text,**kw):return self.editor.call(dict(operation='save',path=view['path'],revision=view['revision'],text=text,**kw))
 def test_absent_initial_install_publishes_template_once(self):
  self.assertTrue(self.editor.initialize()['created']);self.assertFalse(self.editor.initialize()['created']);self.assertEqual('TEMPLATE\n',self.read()['text'])
 def test_existing_custom_text_is_preserved_on_upgrade_with_new_template_offer(self):
  self.editor.initialize();v=self.read();self.save(v,'user rules\n');self.template.write_text('NEW TEMPLATE\n');e=m.Editor(self.template,self.home,{})
  self.assertFalse(e.initialize()['created']);v=e.view({});self.assertEqual('user rules\n',v['text']);self.assertTrue(v['templateAvailable']);self.assertEqual('NEW TEMPLATE\n',v['template'])
 def test_empty_user_file_is_not_overwritten(self):
  p=self.home/'.codex';p.mkdir();(p/'AGENTS.md').touch();self.assertFalse(self.editor.initialize()['created']);self.assertEqual('',self.read()['text'])
 def test_global_override_is_actual_edit_target(self):
  p=self.home/'.codex';p.mkdir();(p/'AGENTS.md').write_text('base');(p/'AGENTS.override.md').write_text('override')
  v=self.read();self.assertTrue(v['path'].endswith('AGENTS.override.md'));self.save(v,'changed');self.assertEqual('base',(p/'AGENTS.md').read_text());self.assertEqual('changed',(p/'AGENTS.override.md').read_text())
 def test_empty_override_falls_back_but_is_not_replaced(self):
  p=self.home/'.codex';p.mkdir();(p/'AGENTS.override.md').touch();self.assertFalse(self.editor.initialize()['created']);self.assertTrue(self.read()['path'].endswith('AGENTS.md'))
 def test_actual_custom_codex_home_is_used_not_default(self):
  custom=self.root/'alternate';e=m.Editor(self.template,self.home,{'CODEX_HOME':str(custom)});e.initialize();self.assertTrue(e.view({})['path'].startswith(str(custom)));self.assertFalse((self.home/'.codex').exists())
 def test_exact_utf8_crlf_bom_and_empty_text(self):
  self.editor.initialize();text='\ufeffРусский\r\nline  \r\n';v=self.save(self.read(),text);self.assertEqual(text,v['text']);self.assertEqual(text.encode('utf-8'),Path(v['path']).read_bytes());self.assertEqual('',self.save(v,'')['text'])
 def test_cancellation_by_not_saving_does_not_touch_source_or_create_history(self):
  self.editor.initialize();before=self.read();self.assertEqual(before,self.read());self.assertEqual([],self.editor.history({})['history'])
 def test_history_preview_restore_and_undo_preserve_previous_revision(self):
  self.editor.initialize();a=self.read();b=self.save(a,'version two');history=self.editor.history({})['history'];self.assertEqual(1,len(history));old=self.editor.previous(dict(id=history[0]['id']))['text'];self.assertEqual(a['text'],old);self.save(b,old);self.assertEqual(2,len(self.editor.history({})['history']))
 def test_parallel_editor_conflict_preserves_other_revision(self):
  self.editor.initialize();first=self.read();second=self.read();self.save(second,'other editor')
  with self.assertRaisesRegex(m.Refused,'EDIT_CONFLICT'):self.save(first,'my draft')
  self.assertEqual('other editor',self.read()['text'])
 def test_external_edit_before_publication_refuses_overwrite(self):
  self.editor.initialize();v=self.read();original=m.atomic
  def racing(path,data,mode=0o600):
   original(path,data,mode)
   if path.parent.name=='history':Path(v['path']).write_text('external writer')
  with patch.object(m,'atomic',side_effect=racing):
   with self.assertRaisesRegex(m.Refused,'EDIT_CONFLICT'):self.save(v,'my draft')
  self.assertEqual('external writer',self.read()['text'])
 def test_source_override_appearing_after_load_is_conflict(self):
  self.editor.initialize();v=self.read();(self.home/'.codex/AGENTS.override.md').write_text('override')
  with self.assertRaisesRegex(m.Refused,'SOURCE_CHANGED'):self.save(v,'new')
 def test_save_failure_retains_original_and_private_history(self):
  self.editor.initialize();v=self.read();original=m.atomic
  def fail(path,data,mode=0o600):
   if path.name=='AGENTS.md':raise OSError('fixture disk failure')
   return original(path,data,mode)
  with patch.object(m,'atomic',side_effect=fail):
   with self.assertRaises(OSError):self.save(v,'new')
  self.assertEqual(v['text'],self.read()['text']);self.assertEqual(1,len(self.editor.history({})['history']))
 def test_project_base_override_independent_and_project_change_does_not_change_general(self):
  self.editor.initialize();general=self.read();q=dict(scope='project',name='AGENTS.md',project=str(self.project));v=self.read(**q);self.save(v,'project only',**q);q['name']='AGENTS.override.md';self.save(self.read(**q),'project override',**q);self.assertEqual(general,self.read());self.assertEqual('project only',(self.project/'AGENTS.md').read_text());base=dict(q,name='AGENTS.md');self.assertEqual(str(self.project/'AGENTS.override.md'),self.read(**base)['maskedBy']);self.assertFalse(self.read(**base)['active'])
 def test_project_source_is_independent_of_edit_target_and_empty_files(self):
  for base,override,expected in ((None,None,None),('  \n','\t',None),('base',None,'AGENTS.md'),('base','  \n','AGENTS.md'),(None,'override','AGENTS.override.md'),('base','override','AGENTS.override.md')):
   for name,value in (('AGENTS.md',base),('AGENTS.override.md',override)):
    p=self.project/name
    if p.exists():p.unlink()
    if value is not None:p.write_text(value)
   for target in m.NAMES:
    v=self.read(scope='project',name=target,project=str(self.project))
    self.assertEqual(str(self.project/expected) if expected else None,v['projectSource'])
    self.assertEqual(str(self.project/target),v['path'])
 def test_project_source_refreshes_after_save_and_restore_preserves_prior_text(self):
  self.editor.initialize();q=dict(scope='project',name='AGENTS.override.md',project=str(self.project))
  (self.project/'AGENTS.md').write_text('base rules');v=self.read(**q);self.assertEqual(str(self.project/'AGENTS.md'),v['projectSource'])
  self.save(v,'override rules',**q);v=self.read(**q);self.assertEqual(str(self.project/'AGENTS.override.md'),v['projectSource'])
  self.save(v,'  \n',**q);v=self.read(**q);self.assertEqual(str(self.project/'AGENTS.md'),v['projectSource'])
  history=self.editor.history(q)['history'];self.assertTrue(any(self.editor.previous(dict(q,id=row['id']))['text']=='override rules' for row in history))
 def test_reject_symlink_hardlink_and_unsafe_file_names(self):
  self.editor.initialize();p=self.home/'.codex/AGENTS.md';p.unlink();sentinel=self.root/'do-not-read';sentinel.write_text('private fixture');p.symlink_to(sentinel)
  with self.assertRaises(m.Refused):self.read()
  p.unlink();os.link(sentinel,p)
  with self.assertRaises(m.Refused):self.read()
  with self.assertRaisesRegex(m.Refused,'INVALID_SCOPE'):self.read(scope='project',name='auth.json',project=str(self.project))
  self.assertEqual('private fixture',sentinel.read_text())
 def test_unsafe_history_symlink_and_traversal_cannot_read_or_write_outside(self):
  self.editor.initialize();store=self.home/'.codex/.termux-jobs-instructions';hist=store/'history';hist.rmdir();hist.symlink_to(self.root)
  with self.assertRaises(m.Refused):self.save(self.read(),'new')
  with self.assertRaises(m.Refused):self.editor.previous({'id':'../auth'})
 def test_diagnostics_lists_chain_override_and_config_presence_without_auth_reads(self):
  self.editor.initialize();(self.project/'AGENTS.md').write_text('base');sub=self.project/'sub';sub.mkdir();(sub/'AGENTS.md').write_text('masked');(sub/'AGENTS.override.md').write_text('active');(self.home/'.codex/config.toml').write_text('developer_instructions="do not reveal this"\nmodel_instructions_file="auth.json"\nproject_doc_fallback_filenames=["auth.json"]\n')
  original=m.read_file;reads=[]
  def observe(path,*a,**kw):reads.append(Path(path).name);self.assertNotEqual('auth.json',Path(path).name);return original(path,*a,**kw)
  with patch.object(m,'read_file',side_effect=observe):d=self.editor.diagnostics({'project':str(sub)})
  active=[s['path'] for s in d['sources'] if s['active']];self.assertIn(str(sub/'AGENTS.override.md'),active);self.assertNotIn(str(sub/'AGENTS.md'),active);self.assertIn(str(self.project/'AGENTS.md'),active);self.assertNotIn('do not reveal this',json.dumps(d));self.assertTrue(d['configurations'][0]['parsed']);self.assertIn('developer_instructions',d['configurations'][0]['settings'])
 def test_diagnostics_does_not_create_missing_user_file(self):
  self.editor.diagnostics({'project':str(self.project)});self.assertFalse((self.home/'.codex').exists())
 def test_history_is_bound_to_real_selected_path(self):
  self.editor.initialize();self.save(self.read(),'new');key=self.editor.history({})['history'][0]['id']
  with self.assertRaisesRegex(m.Refused,'INVALID_HISTORY'):self.editor.previous(dict(scope='project',name='AGENTS.md',project=str(self.project),id=key))
 def test_oversize_text_refused_without_changing_existing_text(self):
  self.editor.initialize();v=self.read()
  with self.assertRaisesRegex(m.Refused,'TEXT_TOO_LARGE'):self.save(v,'a'*(m.LIMIT+1))
  self.assertEqual(v['text'],self.read()['text'])
 def test_actual_reported_cli_version_and_known_behavior_matrix(self):
  self.assertEqual('0.164.0',self.editor.cli()['version']);self.assertTrue(self.editor.cli()['behaviorVerified'])
  with patch.object(m.subprocess,'run',return_value=types.SimpleNamespace(returncode=0,stdout='codex-cli 0.163.0\n')):self.assertTrue(self.editor.cli()['behaviorVerified'])
 def test_unknown_cli_or_absent_cli_does_not_block_saving_or_reset_text(self):
  self.editor.initialize()
  with patch.object(m.subprocess,'run',return_value=types.SimpleNamespace(returncode=0,stdout='codex-cli 99.0.0\n')):
   d=self.editor.cli();self.assertEqual('99.0.0',d['version']);self.assertFalse(d['behaviorVerified']);self.save(self.read(),'custom for future CLI')
  with patch.object(m.shutil,'which',return_value=None):
   self.assertIsNone(self.editor.cli()['version']);self.assertEqual('custom for future CLI',self.read()['text']);self.save(self.read(),'custom without CLI')
 def test_concurrent_savers_serialize_and_one_stale_revision_is_rejected(self):
  self.editor.initialize();view=self.read()
  def write(value):
   try:self.save(view,value);return 'saved'
   except m.Refused as e:return str(e)
  with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:result=list(pool.map(write,('one','two')))
  self.assertEqual(['EDIT_CONFLICT','saved'],sorted(result));self.assertIn(self.read()['text'],('one','two'))
 def test_initialization_does_not_overwrite_external_creator(self):
  original=os.link
  def racing(source,target,**kw):Path(target).write_text('external creator');return original(source,target,**kw)
  with patch.object(m.os,'link',side_effect=racing):self.assertFalse(self.editor.initialize()['created'])
  self.assertEqual('external creator',self.read()['text'])
 def test_new_builtin_contains_storage_rules_and_existing_text_is_not_replaced(self):
  self.template.write_bytes((ROOT/'app/src/main/assets/agent-instructions-ubuntu-ai.md').read_bytes());e=m.Editor(self.template,self.home,{})
  e.initialize();v=e.view({});self.assertIn('## Хранение файлов и временных данных',v['text']);self.assertFalse(v['storageAdditionAvailable'])
  e.save(dict(path=v['path'],revision=v['revision'],text='\ufeffМой текст  \r\n'))
  self.assertFalse(e.initialize()['created']);v=e.view({});self.assertEqual('\ufeffМой текст  \r\n',v['text']);self.assertTrue(v['storageAdditionAvailable'])
  result=e.save(dict(path=v['path'],revision=v['revision'],text=v['text']+'\n'+v['storageAddition']))
  self.assertTrue(result['text'].startswith('\ufeffМой текст  \r\n'));self.assertFalse(result['storageAdditionAvailable']);self.assertEqual(2,len(e.history({})['history']))
 def test_english_storage_addition_is_present_without_a_cleanup_implementation(self):
  self.template.write_bytes((ROOT/'app/src/main/assets/agent-instructions-ubuntu-ai.en.md').read_bytes());e=m.Editor(self.template,self.home,{});v=e.view({});self.assertIn('## File and temporary data storage',v['storageAddition']);self.assertIn('explicitly authorized',v['storageAddition']);self.assertFalse((self.home/'.codex').exists())
 def test_storage_addition_offer_deduplicates_across_both_template_languages(self):
  for selected in ('agent-instructions-ubuntu-ai.md','agent-instructions-ubuntu-ai.en.md'):
   self.template.write_bytes((ROOT/'app/src/main/assets'/selected).read_bytes());e=m.Editor(self.template,self.home,{})
   for existing in ('agent-instructions-ubuntu-ai.md','agent-instructions-ubuntu-ai.en.md'):
    existing_text=(ROOT/'app/src/main/assets'/existing).read_text();v=e.view({});e.save(dict(path=v['path'],revision=v['revision'],text='Custom rules\n'+existing_text))
    self.assertFalse(e.view({})['storageAdditionAvailable'],(selected,existing));self.assertTrue(e.view({})['text'].startswith('Custom rules\n'))
 def test_bilingual_template_and_append_save_history_with_exact_previous_revision(self):
  self.editor.initialize();v=self.read();self.save(v,'User text  \r\n');prior=self.read()['text']
  ru=(ROOT/'app/src/main/assets/agent-instructions-ubuntu-ai.md').read_text();en=(ROOT/'app/src/main/assets/agent-instructions-ubuntu-ai.en.md').read_text()
  for value in (ru,en):
   v=self.read();before=v['text'];self.save(v,value);history=self.editor.history({})['history'];self.assertTrue(any(self.editor.previous(dict(id=row['id']))['text']==before for row in history));self.assertEqual(value,self.read()['text'])
  self.assertTrue(any(self.editor.previous(dict(id=row['id']))['text']==prior for row in self.editor.history({})['history']))
 def test_switching_template_language_does_not_translate_user_text_or_history(self):
  self.editor.initialize();self.save(self.read(),'Earlier custom rules\r\n');custom='\ufeffМои правила / My rules  \r\n';self.save(self.read(),custom);history=self.editor.history({})['history'];previous=[self.editor.previous(dict(id=row['id']))['text'] for row in history]
  path=Path(self.read()['path']);before=path.read_bytes();info=path.stat()
  for asset in ('agent-instructions-ubuntu-ai.en.md','agent-instructions-ubuntu-ai.md'):
   self.template.write_bytes((ROOT/'app/src/main/assets'/asset).read_bytes());e=m.Editor(self.template,self.home,{})
   self.assertFalse(e.initialize()['created']);self.assertEqual(custom,e.view({})['text']);self.assertEqual(before,path.read_bytes());self.assertEqual(info.st_ino,path.stat().st_ino);self.assertEqual(history,e.history({})['history']);self.assertEqual(previous,[e.previous(dict(id=row['id']))['text'] for row in history])
 def test_import_project_create_replace_restore_cancel_and_override_are_scoped(self):
  self.editor.initialize();general=self.read();override=self.project/'AGENTS.override.md';override.write_text('Override rules');q=dict(scope='project',name='AGENTS.md',project=str(self.project));v=self.read(**q)
  self.assertFalse(v['exists']);self.assertFalse((self.project/'AGENTS.md').exists()) # cancel before confirming
  imported='\ufeffImported user text  \r\n';a=self.save(v,imported,**q);self.assertEqual(imported,a['text']);self.assertEqual(str(override),a['projectSource']);self.assertEqual('Override rules',override.read_text());self.assertEqual(general,self.read())
  b=self.save(a,'replacement',**q);history=self.editor.history(q)['history'];prior=self.editor.previous(dict(q,id=history[0]['id']))['text'];self.assertEqual(imported,prior);self.save(b,prior,**q);self.assertEqual(imported,self.read(**q)['text']);self.assertEqual('Override rules',override.read_text());self.assertEqual(general,self.read())
 def test_import_global_uses_actual_override_and_conflict_preserves_every_other_file(self):
  custom=self.root/'custom-codex';custom.mkdir();(custom/'AGENTS.md').write_text('Shared base');override=custom/'AGENTS.override.md';override.write_text('Prior override');(self.project/'AGENTS.md').write_text('Project rules');e=m.Editor(self.template,self.home,dict(CODEX_HOME=str(custom)));v=e.view({});self.assertEqual(str(override),v['path']);value='\ufeffNew global rules  \r\n';result=e.save(dict(path=v['path'],revision=v['revision'],text=value));self.assertEqual(value,override.read_bytes().decode('utf-8'));self.assertEqual('Shared base',(custom/'AGENTS.md').read_text());self.assertEqual('Project rules',(self.project/'AGENTS.md').read_text());self.assertEqual('Prior override',e.previous(dict(id=e.history({})['history'][0]['id']))['text'])
  override.write_text('External edit')
  with self.assertRaisesRegex(m.Refused,'EDIT_CONFLICT'):e.save(dict(path=result['path'],revision=result['revision'],text='stale import'))
  self.assertEqual('External edit',override.read_text())
if __name__=='__main__':unittest.main()
