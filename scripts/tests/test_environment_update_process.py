"""Actual owned process groups only; no Termux sessions or PRoot launch."""
import importlib.util,os,signal,subprocess,sys,tempfile,time,unittest
from pathlib import Path
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[2]
s=importlib.util.spec_from_file_location('updates_process',ROOT/'app/src/main/assets/environment-update-process.py');m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
class OwnedProcesses(unittest.TestCase):
 def fixture(self,leader_exits=False):
  temp=tempfile.TemporaryDirectory(dir=ROOT/'.verification');path=Path(temp.name)/'beats'
  code=f"""import os,time
child=os.fork()
if child==0:
 end=time.monotonic()+5
 while time.monotonic()<end:
  with open({str(path)!r},'a') as f:f.write('beat\\n')
  time.sleep(.02)
 os._exit(0)
{'os._exit(0)' if leader_exits else 'time.sleep(6)'}
"""
  return temp,path,[sys.executable,'-c',code]
 def test_cancel_kills_only_owned_descendants_and_keeps_guard_alive(self):
  temp,path,args=self.fixture();guard=subprocess.Popen([sys.executable,'-c','import time;time.sleep(10)'])
  try:
   with self.assertRaises(m.Cancelled):m.run(args,cancel=lambda:path.exists() and path.stat().st_size>=20)
   size=path.stat().st_size;time.sleep(.15);self.assertEqual(size,path.stat().st_size);self.assertIsNone(guard.poll())
  finally:guard.terminate();guard.wait(timeout=3);temp.cleanup()
 def test_exited_leader_with_inherited_pipe_is_drained_then_owned_descendant_stopped(self):
  temp,path,args=self.fixture(True);start=time.monotonic()
  try:
   status,data=m.run(args);self.assertEqual(0,status);self.assertLess(time.monotonic()-start,3);size=path.stat().st_size;time.sleep(.15);self.assertEqual(size,path.stat().st_size)
  finally:temp.cleanup()
 def test_timeout_keeps_unrelated_process_and_no_wait_precedes_final_signal(self):
  temp,path,args=self.fixture();events=[];original_kill=os.killpg;original_wait=subprocess.Popen.wait
  def kill(pid,sig):events.append(('signal',sig));return original_kill(pid,sig)
  def wait(p,*a,**kw):events.append(('wait',p.pid));return original_wait(p,*a,**kw)
  try:
   with patch.object(m.os,'killpg',side_effect=kill),patch.object(subprocess.Popen,'wait',wait),self.assertRaisesRegex(m.Refused,'PACKAGE_COMMAND_TIMEOUT'):m.run(args,timeout=.3)
   first=next(i for i,e in enumerate(events) if e[0]=='wait');self.assertIn(('signal',signal.SIGKILL),events[:first])
  finally:temp.cleanup()
 def test_output_limit_is_bounded_and_cancel_before_start_spawns_nothing(self):
  with self.assertRaisesRegex(m.Refused,'COMMAND_OUTPUT_TOO_LARGE'):m.run([sys.executable,'-c',"print('x'*100000)"],limit=1000)
  with patch.object(subprocess,'Popen',side_effect=AssertionError('must not spawn')),self.assertRaises(m.Cancelled):m.run(['not-run'],cancel=lambda:True)
if __name__=='__main__':unittest.main()
