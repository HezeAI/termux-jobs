#!/usr/bin/env python3
"""Isolated fixtures and fake scheduler commands: never touch real cron/systemd."""
import importlib.util,unittest,tempfile,pathlib,subprocess,sys,json,fcntl,os,shutil
from unittest.mock import patch
sys.dont_write_bytecode=True
ROOT=pathlib.Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('schedules',ROOT/'app/src/main/assets/workspace-server-schedules.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
class SchedulesTest(unittest.TestCase):
 def setUp(self):
  self.temp=tempfile.TemporaryDirectory(dir=ROOT/'.verification');self.home=pathlib.Path(self.temp.name);m.ROOT=self.home/'.local/share/termux-jobs-schedules-v1';self.cron=b'MAILTO=owner@example.invalid\n17 * * * * echo keep\n';self.calls=[]
  self.patches=[patch.object(m.pathlib.Path,'home',classmethod(lambda cls:self.home)),patch.object(m,'caps',return_value={'cron':True,'systemd':True,'linger':False,'timezone':'UTC'}),patch.object(m,'run',side_effect=self.fake_run)]
  for p in self.patches:p.start()
 def tearDown(self):
  for p in reversed(self.patches):p.stop()
  self.temp.cleanup()
 def fake_run(self,args,data=None,allowed=(0,),timeout=10):
  self.calls.append(args)
  if args[:2]==['crontab','-l']:return subprocess.CompletedProcess(args,0,self.cron,b'')
  if args==['crontab','-']:self.cron=data;return subprocess.CompletedProcess(args,0,b'',b'')
  return subprocess.CompletedProcess(args,0,b'',b'')
 def create(self,backend='cron',command='printf hello',plan=None):return m.rpc({'op':'create','backend':backend,'plan':plan or ('*/30 * * * *' if backend=='cron' else 'daily'),'directory':str(self.home),'title':'test','command':command})
 def test_cron_preserves_foreign_entries_and_never_contains_user_command(self):
  before=self.cron;c=self.create(command="printf '%s' 'secret; $HOME'\n# more")
  self.assertTrue(self.cron.startswith(before));self.assertNotIn(b'secret',self.cron);self.assertIn(c['id'].encode(),self.cron)
  m.rpc({'op':'delete','id':c['id']});self.assertEqual(before,self.cron)
 def test_runner_executes_and_caps_to_one_app_job(self):
  c=self.create();r=subprocess.run([sys.executable,'-I',str(m.ROOT/'runner.py'),c['id']],capture_output=True,timeout=5);self.assertEqual(0,r.returncode,r.stderr);job=m.ROOT/c['id'];self.assertEqual(b'hello',(job/'output.log').read_bytes());self.assertEqual('done',json.loads((job/'state.json').read_text())['state'])
 def test_disabled_runner_does_not_execute(self):
  c=self.create(command='printf should-not-run');m.rpc({'op':'disable','id':c['id']});subprocess.run([sys.executable,'-I',str(m.ROOT/'runner.py'),c['id']],timeout=5,check=True);self.assertFalse((m.ROOT/c['id']/'output.log').exists());m.rpc({'op':'enable','id':c['id']});self.assertTrue(m.config(c['id'])['enabled'])
 def test_running_lock_blocks_delete_and_duplicate_run(self):
  c=self.create();job=m.ROOT/c['id']
  with open(job/'run.lock','a+') as lock:
   fcntl.flock(lock,fcntl.LOCK_EX)
   with self.assertRaisesRegex(ValueError,'SCHEDULE_RUNNING'):m.rpc({'op':'delete','id':c['id']})
   subprocess.run([sys.executable,'-I',str(m.ROOT/'runner.py'),c['id']],timeout=5,check=True);self.assertFalse((job/'output.log').exists())
 def test_systemd_units_are_user_scoped_and_only_own_files_deleted(self):
  foreign=self.home/'.config/systemd/user/foreign.timer';foreign.parent.mkdir(parents=True);foreign.write_text('keep');c=self.create('systemd');name=m.PREFIX+c['id'];folder=foreign.parent;self.assertIn('OnCalendar=daily',(folder/(name+'.timer')).read_text());self.assertIn('Persistent=true',(folder/(name+'.timer')).read_text());self.assertIn('KillMode=control-group',(folder/(name+'.service')).read_text());self.assertTrue(all('--user' in a for a in self.calls if a[0]=='systemctl'));m.rpc({'op':'delete','id':c['id']});self.assertEqual('keep',foreign.read_text());self.assertFalse((folder/(name+'.timer')).exists())
 def test_edit_changes_only_timing_and_preserves_disabled_state(self):
  c=self.create();m.rpc({'op':'disable','id':c['id']});m.rpc({'op':'edit','id':c['id'],'plan':'0 3 * * *'});after=m.config(c['id']);self.assertEqual('0 3 * * *',after['plan']);self.assertFalse(after['enabled']);self.assertEqual(c['command'],after['command']);self.assertNotIn(c['id'].encode(),self.cron)
 def test_systemd_edit_updates_owned_timer(self):
  c=self.create('systemd');m.rpc({'op':'edit','id':c['id'],'plan':'*-*-* 03:00:00'});p=self.home/'.config/systemd/user'/ (m.PREFIX+c['id']+'.timer');self.assertIn('OnCalendar=*-*-* 03:00:00',p.read_text())
 def test_invalid_cron_calendar_and_path_are_rejected_before_mutation(self):
  for plan in ['* * * * *\n* * * * * evil','99 * * * *','*/0 * * * *','0 25 * * *','@reboot','* * * * *; id']:
   with self.assertRaises(ValueError):self.create(plan=plan)
  for plan in ['daily\nUnit=foreign.service','2026-99-99 10:00:00','*-*-* 99:00:00']:
   with self.assertRaises((ValueError,OverflowError)):self.create('systemd',plan=plan)
  self.assertEqual([],self.calls)
 def test_setup_failure_stays_disabled_and_visible(self):
  original=self.fake_run
  def fail(args,*a,**kw):
   if args==['crontab','-']:raise ValueError('SCHEDULER_COMMAND_FAILED')
   return original(args,*a,**kw)
  with patch.object(m,'run',side_effect=fail):
   with self.assertRaisesRegex(ValueError,'SCHEDULE_SETUP_FAILED'):self.create()
  records=m.rpc({'op':'list'})['schedules'];self.assertEqual(1,len(records));self.assertFalse(records[0]['enabled']);self.assertTrue(records[0]['setupError'])
 def test_rejects_symlink_root(self):
  m.ROOT.parent.mkdir(parents=True);m.ROOT.symlink_to(self.home)
  with self.assertRaisesRegex(ValueError,'SYMLINK_DIRECTORY'):self.create()
 def test_log_is_bounded_and_removal_does_not_signal_any_process(self):
  c=self.create();(m.ROOT/c['id']/'output.log').write_bytes(b'x'*40000);self.assertEqual(32768,len(m.rpc({'op':'log','id':c['id']})['log']));m.rpc({'op':'delete','id':c['id']});self.assertFalse(any('kill' in ' '.join(a) for a in self.calls))
 def test_read_only_list_does_not_install_runner(self):
  self.assertEqual({'schedules':[]},m.rpc({'op':'list'}));self.assertFalse(m.ROOT.exists());self.assertEqual([],self.calls)
 def test_runner_termination_targets_only_owned_child_group(self):
  import time
  c=self.create(command='echo ready; sleep 30');runner=subprocess.Popen([sys.executable,'-I',str(m.ROOT/'runner.py'),c['id']],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL,start_new_session=True);sibling=subprocess.Popen(['sleep','15'])
  try:
   log=m.ROOT/c['id']/'output.log';deadline=time.monotonic()+4
   while time.monotonic()<deadline:
    if log.exists() and b'ready' in log.read_bytes():break
    time.sleep(.05)
   self.assertTrue(log.exists());runner.terminate();self.assertEqual(1,runner.wait(timeout=5));self.assertIsNone(sibling.poll());self.assertEqual('failed',json.loads((m.ROOT/c['id']/'state.json').read_text())['state'])
  finally:
   if runner.poll() is None:runner.kill();runner.wait()
   sibling.terminate();sibling.wait()
 def test_config_id_corruption_cannot_delete_other_schedule(self):
  c=self.create();other=self.create();p=m.ROOT/c['id']/'config.json';record=json.loads(p.read_text());record['id']=other['id'];p.write_text(json.dumps(record))
  with self.assertRaisesRegex(ValueError,'SCHEDULE_CORRUPT'):m.rpc({'op':'delete','id':c['id']})
  self.assertTrue((m.ROOT/other['id']).exists())
 def test_stale_running_state_is_reported_as_interrupted_without_signals(self):
  c=self.create();m.atomic(m.ROOT/c['id']/'state.json',{'state':'running','started':0});record=m.rpc({'op':'list'})['schedules'][0];self.assertEqual('interrupted',record['last']['state'])
if __name__=='__main__':unittest.main()
