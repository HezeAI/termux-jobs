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
 def test_native_quoted_patterns_are_literal_but_expansion_is_unknown(self):
  self.assertEqual(m.native_plan("rtk rg 'a|b' --glob '*.py' ."),m.native_plan('rtk rg "a|b" --glob "*.py" .'))
  self.assertIsNotNone(m.native_plan("rtk rg 'a;b' file"))
  for command in ['rtk rg x *.py','rtk read "$HOME/f"','rtk ls | head -5','rtk ls &','rtk ls > out','rtk ls && rtk ls','rtk ls; eval other','rtk ls; X=1 command']:
   self.assertIsNone(m.native_plan(command),command)
 def sequence(self,output='prefix\nsmall\nend\n',other_run=False,ambiguous=False,missing=False):
  plan=m.native_plan('printf prefix; rtk read sample; printf end')
  event=self.event(command='ignored',output=output);key=m.event_key(event,self.run)
  m.metric_update(self.profile,key,mode='advanced',nativePlan=plan,nativeArgsHash='',startedAt=1)
  root=m.directory(m.chat(self.profile)/'native-bytes-v1')
  for name in ['one','two'] if ambiguous else ['one']:
   m.write(root/(name+'.meta.json'),dict(run='other' if other_run else self.run,argsHash=plan[0],createdAt=2))
   if not missing:m.write(root/(name+'.result.json'),dict(inputBytes=100,outputBytes=6,outputHash=hashlib.sha256(b'small\n').hexdigest()))
  with patch.object(m.subprocess,'run',side_effect=AssertionError('must not execute commands')):m.observe_result(event,self.profile,self.run)
  return event
 def test_sequence_matches_delivered_segment_once_including_unchanged_gaps(self):
  e=self.sequence();s=self.stats('advanced');self.assertEqual((s['inputBytes'],s['outputBytes'],s['commands']),(111,17,1))
  m.observe_result(e,self.profile,self.run);self.assertEqual(self.stats('advanced'),s)
 def test_sequence_rejects_other_run(self):
  self.sequence(other_run=True);self.assertEqual(self.stats('advanced')['unknownBaselineCommands'],1)
 def test_sequence_rejects_ambiguous_receipts(self):
  self.sequence(ambiguous=True);self.assertEqual(self.stats('advanced')['unknownBaselineCommands'],1)
 def test_sequence_rejects_truncated_and_missing_measurement(self):
  self.sequence(output='prefix\nsmal');self.assertEqual(self.stats('advanced')['unknownBaselineCommands'],1)
 def test_sequence_rejects_repeated_output_placement(self):
  self.sequence(output='small\nsmall\n');self.assertEqual(self.stats('advanced')['unknownBaselineCommands'],1)
 def test_recall_help_does_not_count_as_recovered_output(self):
  for command in ['rtk recall --help','rtk recall -h','rtk recall --list']:
   m.hook(self.event(command=command,output='help'),self.profile,self.run,'codex')
  self.assertEqual(self.stats('advanced')['recallCommands'],0)
 def test_sequence_final_cli_newline_normalization(self):
  plan=m.native_plan('rtk read a; rtk read b');e=self.event(output='a\nb');key=m.event_key(e,self.run)
  m.metric_update(self.profile,key,mode='advanced',nativePlan=plan,nativeArgsHash='',startedAt=1)
  root=m.directory(m.chat(self.profile)/'native-bytes-v1')
  for i,(raw,base) in enumerate([(b'a\n',10),(b'b\n',20)]):
   m.write(root/(str(i)+'.meta.json'),dict(run=self.run,argsHash=plan[i],createdAt=2))
   m.write(root/(str(i)+'.result.json'),dict(inputBytes=base,outputBytes=len(raw),outputHash=hashlib.sha256(raw).hexdigest(),normalizations=[dict(hash=hashlib.sha256(raw.rstrip(b'\n')).hexdigest(),inputBytes=base-1)]))
  m.observe_result(e,self.profile,self.run);s=self.stats('advanced');self.assertEqual((s['inputBytes'],s['outputBytes']),(29,3))
 def test_explicit_native_command_with_quoted_glob_gets_receipt_key(self):
  m.write(m.chat(self.profile)/'mode.json',{'enabled':True,'mode':'advanced'})
  command="rtk rg 'x|y' --glob '*.py' .";e=self.event(command=command);e['hook_event_name']='PreToolUse'
  class Result: returncode=0;stdout='{}'
  with patch.object(m,'digest',return_value=m.PIN['binarySha256']),patch.object(m.subprocess,'run',return_value=Result()):m.hook(e,self.profile,self.run,'codex')
  record=m.read(m.metric_file(self.profile,m.event_key(e,self.run)));self.assertEqual(record['nativeArgsHash'],m.native_plan(command)[0])
if __name__=='__main__':unittest.main()
