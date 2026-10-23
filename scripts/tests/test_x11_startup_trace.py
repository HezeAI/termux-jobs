"""Isolated startup tracing: no live processes, bus or installed desktop touched."""
import importlib.util,os,tempfile,unittest
from pathlib import Path
from unittest.mock import patch,MagicMock
ROOT=Path(__file__).resolve().parents[2]
s=importlib.util.spec_from_file_location('ready_trace',ROOT/'x11/src/main/assets/x11-ready.py');m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
class TraceTests(unittest.TestCase):
 def fixture(self,root,pid,token,role='xfdesktop'):
  p=root/str(pid);p.mkdir();(p/'comm').write_text(role);(p/'environ').write_bytes(b'JOBS_X11_FOCUS_TOKEN='+token.encode()+b'\0SESSION_MANAGER=private\0SECRET=must-not-export\0');fields=['S']+['0']*22;fields[11]='10';fields[12]='5';fields[19]='100';(p/'stat').write_text(str(pid)+' ('+role+') '+' '.join(fields));(p/'wchan').write_text('do_sys_poll');(p/'exe').symlink_to('/data/data/com.termux/files/usr/libexec/proot/loader')
 def test_only_owned_roles_are_seen_and_raw_environment_never_exported(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   root=Path(d);(root/'uptime').write_text('20 0');self.fixture(root,11,'ours');self.fixture(root,12,'other');self.fixture(root,13,'ours','private-name');out=[];now=[0];t=m.StartupTrace('ours',root,lambda:now[0],lambda x,**k:out.append(x));t.bus_done=True;t.sample();first='\n'.join(out);self.assertIn('xfdesktop pid=11 event=first',first);self.assertIn('wait=poll sm=1 binary=proot',first);self.assertNotIn('pid=12',first);self.assertNotIn('private',first);self.assertNotIn('must-not-export',first)
   t.sample();self.assertEqual(1,len(out));now[0]=6;t.sample();self.assertIn('event=sample','\n'.join(out))
 def test_queries_report_only_bounded_categories_and_numbers(self):
  out=[];t=m.StartupTrace('token',emit=lambda x,**k:out.append(x));t.record(['dbus-send','private-arg'],.125,True);t.record(['dbus-send'],.05,False);t.summarize();self.assertEqual(['JOBS_X11_QUERY session count=2 total_ms=175 max_ms=125 failed=1'],out)
 def test_auth_probe_never_exports_address_or_auth_reply(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   root=Path(d);(root/'self').mkdir();(root/'self/status').write_text('Uid:\t10620\t10620\n');out=[];t=m.StartupTrace('token',root,emit=lambda x,**k:out.append(x));sock=MagicMock();sock.recv.return_value=b'OK private-guid\r\n'
   with patch.dict(os.environ,{'DBUS_SESSION_BUS_ADDRESS':'unix:path=/private/bus'}),patch.object(m.os,'getuid',return_value=0),patch.object(m.socket,'socket',return_value=sock):t.bus()
   self.assertEqual(['JOBS_X11_BUS guest_uid=0 kernel_uid=10620 guest_auth=ok kernel_auth=ok'],out);self.assertNotIn('private',''.join(out))
 def test_registration_counts_do_not_export_client_objects(self):
  out=[];t=m.StartupTrace('token',emit=lambda x,**k:out.append(x))
  with patch.object(m,'query',return_value='array [ object path "/private/client1" object path "/private/client2" ]') as q:t.session('uint32 0',m.time.monotonic()+2)
  self.assertEqual(['JOBS_X11_SESSION state=startup clients=2'],out);self.assertNotIn('private',''.join(out))
 def test_ready_finishes_trace_once_without_changing_readiness(self):
  trace=MagicMock()
  with patch('builtins.print'):self.assertTrue(m.wait('token',probe=lambda:(True,True,True,'ok'),trace=trace))
  trace.sample.assert_called_once();trace.finish.assert_called_once()
