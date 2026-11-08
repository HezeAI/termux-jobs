"""Version 2 measured outputs, unknown baselines, recovery and migration. No real agents/auth."""
import contextlib,hashlib,importlib.util,io,json,os,subprocess,tempfile,unittest,uuid
from pathlib import Path
from unittest.mock import patch
REPO=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('rtk',REPO/'app/src/main/assets/rtk-mode.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
class Statistics(unittest.TestCase):
 def setUp(self):
  self.tmp=tempfile.TemporaryDirectory(dir=REPO/'.verification/rtk-statistics');self.before=m.BASE;m.BASE=Path(self.tmp.name);self.profile=str(uuid.uuid4());self.run=str(uuid.uuid4());self.key='a'*64
 def tearDown(self):m.BASE=self.before;self.tmp.cleanup()
 def event(self,key='tool',command='head -n 45 input',output=''):
  return dict(hook_event_name='PostToolUse',tool_use_id=key,tool_name='Bash',tool_input={'command':command},tool_response=output)
 def capture(self,raw,shown,key='tool'):
  m.metric_update(self.profile,m.event_key(self.event(key),self.run),mode='basic',captured=True,inputBytes=len(raw),filteredBytes=len(shown),filteredHash=hashlib.sha256(shown).hexdigest())
 def stats(self,mode='basic'):return m.statistics(self.profile)[mode]
 def test_native_measurement_matches_actual_result_and_keeps_recall_separate(self):
  event=self.event(command='rtk ls',output='compact\n');key=m.event_key(event,self.run)
  m.metric_update(self.profile,key,mode='advanced',nativeArgsHash='args',startedAt=1)
  root=m.directory(m.chat(self.profile)/'native-bytes-v1');meta=root/'a.meta.json'
  m.write(meta,dict(run=self.run,argsHash='args',createdAt=2))
  m.write(root/'a.result.json',dict(inputBytes=4410,outputBytes=8,outputHash=hashlib.sha256(b'compact\n').hexdigest()))
  with patch.object(m.subprocess,'run',side_effect=AssertionError('never repeat')):m.observe_result(event,self.profile,self.run)
  s=self.stats('advanced');self.assertEqual((s['inputBytes'],s['outputBytes']),(4410,8));self.assertTrue((root/'completed/a.meta.json').is_file());self.assertTrue((root/'completed/a.result.json').is_file());self.assertFalse(meta.exists())
  m.observe_result(self.event('recall','rtk recall aabbccddeeff','hidden\n'),self.profile,self.run)
  s=self.stats('advanced');self.assertEqual((s['inputBytes'],s['outputBytes'],s['recallBytes']),(4410,8,7))
 def test_native_claude_normalization_uses_matching_whitespace_scope(self):
  event=self.event(command='rtk ls');event['tool_response']={'stdout':'s','stderr':''};key=m.event_key(event,self.run)
  m.metric_update(self.profile,key,mode='advanced',nativeArgsHash='args',startedAt=1)
  root=m.directory(m.chat(self.profile)/'native-bytes-v1');m.write(root/'trim.meta.json',dict(run=self.run,argsHash='args',createdAt=2))
  digest=hashlib.sha256(b's').hexdigest();m.write(root/'trim.result.json',dict(inputBytes=6,outputHash=hashlib.sha256(b's\n').hexdigest(),normalizations=[{'hash':digest,'inputBytes':5},{'hash':digest,'inputBytes':4}]))
  m.observe_result(event,self.profile,self.run);self.assertEqual(self.stats('advanced')['inputBytes'],4)

 def test_measured_head_with_error_word_is_still_unchanged(self):
  event=self.event(command='head -n 5 file',output='error\n');key=m.event_key(event,self.run)
  m.metric_update(self.profile,key,mode='advanced',nativeArgsHash='args',startedAt=1,verbatimWindow=True)
  root=m.directory(m.chat(self.profile)/'native-bytes-v1');m.write(root/'head.meta.json',dict(run=self.run,argsHash='args',createdAt=2));m.write(root/'head.result.json',dict(inputBytes=6,outputHash=hashlib.sha256(b'error\n').hexdigest()))
  m.observe_result(event,self.profile,self.run);stats=self.stats('advanced');self.assertEqual((stats['inputBytes'],stats['outputBytes'],stats['noReductionCommands']),(6,6,1))

 def test_new_native_window_without_capture_remains_unknown(self):
  event=self.event(command='head -n 5 binary.log',output='replacement text');key=m.event_key(event,self.run);m.metric_update(self.profile,key,mode='advanced',verbatimWindow=True)
  m.observe_result(event,self.profile,self.run);self.assertEqual(self.stats('advanced')['unknownBaselineCommands'],1)

 def test_native_ambiguous_or_other_chat_run_never_invents_base(self):
  event=self.event(command='rtk ls',output='same');key=m.event_key(event,self.run)
  m.metric_update(self.profile,key,mode='advanced',nativeArgsHash='args',startedAt=1)
  root=m.directory(m.chat(self.profile)/'native-bytes-v1')
  for name,base in [('a',100),('b',200)]:
   m.write(root/(name+'.meta.json'),dict(run=self.run,argsHash='args',createdAt=2))
   m.write(root/(name+'.result.json'),dict(inputBytes=base,outputHash=hashlib.sha256(b'same').hexdigest()))
  m.observe_result(event,self.profile,self.run);self.assertEqual(self.stats('advanced')['unknownBaselineCommands'],1)

 def test_head45_counts_only_actual_window_and_no_repeat(self):
  raw=('line\n'*45).encode();self.capture(raw,raw)
  with patch.object(m.subprocess,'run',side_effect=AssertionError('never execute for accounting')):
   m.observe_result(self.event(output=raw.decode()),self.profile,self.run)
  s=self.stats();self.assertEqual((s['inputBytes'],s['outputBytes'],s['noReductionCommands']),(225,225,1));self.assertEqual(s['comparedCommands'],1)
 def test_compression_and_recall_are_separate_idempotent_receipts(self):
  self.capture(b'x'*1000,b'x'*200);m.observe_result(self.event(output='x'*200),self.profile,self.run)
  path=str(m.chat(self.profile)/'originals'/('b'*32+'.txt'));q=self.event('recall','cat '+path,'x'*500)
  m.observe_result(q,self.profile,self.run);m.observe_result(q,self.profile,self.run)
  s=self.stats();self.assertEqual((s['inputBytes'],s['outputBytes'],s['recallBytes'],s['commands'],s['recallCommands']),(1000,200,500,1,1))
  self.assertEqual(m.statistics(str(uuid.uuid4()))['basic']['commands'],0)
 def test_saved_output_alone_is_not_recall_or_delivery(self):
  self.capture(b'x'*1000,b'x'*200);s=self.stats();self.assertEqual(s['recallBytes'],0);self.assertEqual(s['comparedCommands'],0);self.assertEqual(s['unconfirmedCommands'],1)
 def test_native_unknown_is_not_zero_and_verbatim_window_is_zero_reduction(self):
  key=m.event_key(self.event(),self.run);m.metric_update(self.profile,key,mode='advanced',verbatimWindow=False);m.observe_result(self.event(output='abc'),self.profile,self.run)
  s=self.stats('advanced');self.assertEqual(s['unknownBaselineCommands'],1);self.assertEqual(s['comparedCommands'],0);self.assertEqual(s['knownOutputBytes'],3)
  q=self.event('window',output='line\n'*45);m.metric_update(self.profile,m.event_key(q,self.run),mode='advanced',verbatimWindow=True)
  with patch.dict(m.PIN,measurementProtocol=0):m.observe_result(q,self.profile,self.run)
  s=self.stats('advanced');self.assertEqual((s['inputBytes'],s['outputBytes'],s['noReductionCommands']),(225,225,1))
  self.assertTrue(m.native_window('head -n 45 input','rtk read input --head-lines 45'))
  for command in ['rtk read input --max-lines 45','rtk read input --head-lines 45 -l aggressive','rtk read input']:
   self.assertFalse(m.native_window('head -n 45 input',command))
 def test_changed_result_and_unrecognized_response_stay_unknown(self):
  self.capture(b'x'*1000,b'x'*200);m.observe_result(self.event(output='truncated'),self.profile,self.run)
  s=self.stats();self.assertEqual(s['unknownBaselineCommands'],1);self.assertEqual(s['comparedCommands'],0)
  q=self.event('other');q['tool_response']={'unexpected':True};m.metric_update(self.profile,m.event_key(q,self.run),mode='advanced');m.observe_result(q,self.profile,self.run);self.assertEqual(self.stats('advanced')['unknownOutputCommands'],1)
 def test_stderr_preserved_in_both_sides(self):
  self.capture(b'abcdef',b'abc');q=self.event(output={'stdout':'abc','stderr':'oops\n'});m.observe_result(q,self.profile,self.run);s=self.stats();self.assertEqual((s['inputBytes'],s['outputBytes']),(11,8))
 def test_utf8_and_matched_whitespace_presentation_are_exact(self):
  raw='я🙂\n'.encode('utf-8');trimmed=raw.rstrip(b'\n');key=m.event_key(self.event(),self.run)
  m.metric_update(self.profile,key,mode='basic',captured=True,inputBytes=len(raw),rawInputBytes=len(raw),filteredHash=hashlib.sha256(raw).hexdigest(),normalizations={hashlib.sha256(trimmed).hexdigest():len(trimmed)})
  m.observe_result(self.event(output=trimmed.decode()),self.profile,self.run);s=self.stats();self.assertEqual((s['inputBytes'],s['outputBytes'],s['noReductionCommands']),(6,6,1))
 def test_legacy_is_frozen_separate_and_files_untouched(self):
  p=m.directory(m.chat(self.profile)/'measurements')/'old.json';m.write(p,{'input':1000,'output':400});before=p.read_bytes();first=m.statistics(self.profile);self.assertEqual(first['legacy']['basic']['input'],1000);self.assertEqual(first['basic']['commands'],0)
  m.write(p,{'input':2000,'output':500});second=m.statistics(self.profile);self.assertEqual(first['legacy'],second['legacy']);self.assertEqual(first['startedAt'],second['startedAt']);self.assertEqual(m.read(p)['input'],2000)
 def test_statistics_never_follow_foreign_directory_symlinks(self):
  external=Path(self.tmp.name)/'foreign';external.mkdir();(m.chat(self.profile)/'measurements').symlink_to(external,target_is_directory=True)
  with self.assertRaises(ValueError):m.statistics(self.profile)
  other=str(uuid.uuid4());d=m.statistics_root(other);(d/'events').symlink_to(external,target_is_directory=True)
  with self.assertRaises(ValueError):m.statistics(other)
 def test_native_recall_without_rewrite_and_off_mode_counts(self):
  q=self.event('native-recall','rtk recall --full abcd1234','full\n');m.hook(q,self.profile,self.run,'codex');s=self.stats('advanced');self.assertEqual(s['recallBytes'],5);self.assertEqual(s['commands'],0)
if __name__=='__main__':unittest.main()
