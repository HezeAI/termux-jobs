"""Isolated tests only: no SSH, installed apps, user processes or real server changes."""
import contextlib
import importlib.util
import io
import json
import os
from pathlib import Path
import signal
import subprocess
import sys
import tempfile
import time
import unittest
from unittest.mock import patch
import urllib.request
import urllib.error

REPO=Path(__file__).resolve().parents[2]
SOURCE=REPO/'app/src/main/assets/workspace-server-agent.py'
spec=importlib.util.spec_from_file_location('server_agent',SOURCE)
a=importlib.util.module_from_spec(spec);spec.loader.exec_module(a)

class AgentTest(unittest.TestCase):
    def setUp(self):
        (REPO/'.verification').mkdir(exist_ok=True)
        self.temp=tempfile.TemporaryDirectory(dir=REPO/'.verification',prefix='agent-test-')
        self.base=Path(self.temp.name);self.root=self.base/'agent';self.root.mkdir(mode=0o700)
        (self.root/'agent.py').write_bytes(SOURCE.read_bytes());self.old=a.ROOT;a.ROOT=self.root
        a.directory();self.no_daemon=patch.object(a,'ensure_daemon');self.no_daemon.start()
    def tearDown(self):
        self.no_daemon.stop();a.ROOT=self.old;self.temp.cleanup()
    def rpc(self,**kw):return a.rpc(kw)
    def test_invalid_ids_never_escape(self):
        for value in ('../agent','x'*32,'-','0'*33,None):
            with self.assertRaises(ValueError):self.rpc(op='cancel',id=value)
    def test_queue_limit_and_persistence(self):
        for _ in range(10):self.rpc(op='start',command='true',directory=str(self.base))
        with self.assertRaisesRegex(ValueError,'QUEUE_FULL'):self.rpc(op='start',command='true',directory=str(self.base))
        self.assertEqual(10,len(self.rpc(op='tasks')['tasks']))
    def test_start_validation(self):
        for cmd in ('','x\0y','x'*8193):
            with self.assertRaises(ValueError):self.rpc(op='start',command=cmd,directory=str(self.base))
        with self.assertRaises(FileNotFoundError):self.rpc(op='start',command='true',directory=str(self.base/'missing'))
    def test_command_is_data_at_submission(self):
        marker=self.base/'must-not-exist';command=f'touch "{marker}"'
        task=self.rpc(op='start',command=command,directory=str(self.base))
        self.assertEqual(command,task['command']);self.assertFalse(marker.exists())
    def test_uninstall_blocks_active_and_unmanaged(self):
        task=self.rpc(op='start',command='true',directory=str(self.base))
        for state in ('queued','starting','running','unmanaged'):
            task['state']=state;a.atomic(a.task_path(task['id']),task)
            with self.assertRaisesRegex(ValueError,'ACTIVE_TASKS'):self.rpc(op='uninstall')
        self.assertTrue((self.root/'agent.py').exists())
    def test_cancel_is_request_not_signal(self):
        task=self.rpc(op='start',command='true',directory=str(self.base))
        with patch.object(a.os,'killpg',side_effect=AssertionError('must not signal from RPC')):
            self.rpc(op='cancel',id=task['id'])
        self.assertTrue((self.root/'tasks'/(task['id']+'.cancel')).exists())
    def test_forget_finished_records_removes_only_record_and_log_and_blocks_active(self):
        result_file=self.base/'command-result';result_file.write_text('keep')
        with patch.object(a.os,'killpg',side_effect=AssertionError('must not signal')):
            for state in ('queued','starting','running','done','failed','cancelled'):
                task=self.rpc(op='start',command='true',directory=str(self.base));task['state']=state;p=a.task_path(task['id']);a.atomic(p,task);p.with_suffix('.log').write_text('output')
                if state in ('queued','starting','running'):
                    with self.assertRaisesRegex(ValueError,'ACTIVE_TASKS'):self.rpc(op='forget',id=task['id'])
                    self.assertTrue(p.exists());task['state']='cancelled';a.atomic(p,task)
                self.rpc(op='forget',id=task['id']);self.assertFalse(p.exists());self.assertFalse(p.with_suffix('.log').exists())
        self.assertEqual('keep',result_file.read_text())
    def test_forget_does_not_stop_unmanaged_process(self):
        task=self.rpc(op='start',command='true',directory=str(self.base));task['state']='unmanaged';a.atomic(a.task_path(task['id']),task)
        with patch.object(a.os,'killpg',side_effect=AssertionError('must not signal')):self.rpc(op='forget',id=task['id'])
        self.assertFalse(a.task_path(task['id']).exists())
    def test_rename_no_overwrite_and_shell_names(self):
        one=self.base/'one';two=self.base/'two';one.write_text('1');two.write_text('2')
        with self.assertRaises(FileExistsError):self.rpc(op='rename',path=str(one),name='two')
        name="$(touch nope); 'movie.mp4"
        self.rpc(op='rename',path=str(one),name=name)
        self.assertEqual('1',(self.base/name).read_text());self.assertEqual('2',two.read_text());self.assertFalse((self.base/'nope').exists())
    def test_delete_symlink_only(self):
        target=self.base/'movie';target.write_text('keep');link=self.base/'link';link.symlink_to(target)
        self.rpc(op='delete',path=str(link));self.assertTrue(target.exists());self.assertFalse(link.exists())
    def test_agent_files_protected(self):
        for op in ('delete','rename'):
            with self.assertRaisesRegex(ValueError,'AGENT_FILES_PROTECTED'):self.rpc(op=op,path=str(self.root/'agent.py'),name='lost')
    def test_service_names_cannot_inject(self):
        for name in ('--help','foo;touch /tmp/nope','foo\nbar.service','../foo.service'):
            with patch.object(a.subprocess,'run',side_effect=AssertionError('must not execute')):
                with self.assertRaises(ValueError):self.rpc(op='service-restart',name=name)
    def test_service_restart_does_not_elevate(self):
        with patch.object(a.subprocess,'run',return_value=subprocess.CompletedProcess([],0,b'',b'')) as mock:
            self.rpc(op='service-restart',name='example.service')
            self.assertEqual(['systemctl','--no-ask-password','restart','example.service'],mock.call_args.args[0])
    def test_stream_tokens_expire_and_bound(self):
        (self.root/'daemon.json').write_text('{"port":1234}');video=self.base/'movie.mp4';video.write_bytes(b'1234567890')
        for _ in range(8):
            r=self.rpc(op='stream',path=str(video));self.assertRegex(r['token'],r'^[0-9a-f]{64}$')
        with self.assertRaisesRegex(ValueError,'STREAM_LIMIT'):self.rpc(op='stream',path=str(video))
    def test_log_tail_bound(self):
        task=self.rpc(op='start',command='true',directory=str(self.base));log=a.task_path(task['id']).with_suffix('.log');log.write_bytes(b'x'*100000)
        self.assertEqual(32768,len(self.rpc(op='log',id=task['id'])['log']))
    def test_unknown_operation_not_start_daemon(self):
        with self.assertRaises(ValueError):self.rpc(op='evil')
        a.ensure_daemon.assert_not_called()
    def test_list_handles_hidden_and_quote_names(self):
        (self.base/"it's a file").write_text('a');(self.base/'.hidden').write_text('b')
        names=[e['name'] for e in self.rpc(op='list',path=str(self.base))['entries']]
        self.assertIn("it's a file",names);self.assertIn('.hidden',names)
    def test_media_range_and_modified_file_rejected(self):
        import threading
        video=self.base/'movie.mp4';video.write_bytes(b'0123456789');(self.root/'daemon.json').write_text('{"port":1234}')
        token=self.rpc(op='stream',path=str(video))['token']
        server=a.MediaServer();thread=threading.Thread(target=server.serve_forever);thread.start()
        try:
            url=f'http://127.0.0.1:{server.server_port}/{token}'
            with urllib.request.urlopen(urllib.request.Request(url,headers={'Range':'bytes=2-5'})) as r:
                self.assertEqual(206,r.status);self.assertEqual(b'2345',r.read());self.assertEqual('bytes 2-5/10',r.headers['Content-Range'])
            with urllib.request.urlopen(urllib.request.Request(url,headers={'Range':'bytes=-3'})) as r:self.assertEqual(b'789',r.read())
            with self.assertRaises(urllib.error.HTTPError) as missing:urllib.request.urlopen(url+'x')
            missing.exception.close()
            video.write_bytes(b'changed')
            with self.assertRaises(urllib.error.HTTPError) as error:urllib.request.urlopen(url)
            self.assertEqual(409,error.exception.code);error.exception.close()
        finally:server.shutdown();server.server_close();thread.join()

    def test_embedded_installer_checks_payload_and_refuses_overwrite(self):
        import re,hashlib
        java=(REPO/'app/src/main/java/com/termux/app/workspace/ServerConnection.java').read_text()
        part=java[java.index('String setup='):java.index('        byte[] data;',java.index('String setup='))]
        setup=''.join(json.loads(m) for m in re.findall(r'"(?:[^"\\]|\\.)*"',part))
        home=self.base/'home';home.mkdir()
        wrapper="import pathlib; pathlib.Path.home=classmethod(lambda cls:pathlib.Path("+repr(str(home))+")); "+setup
        payload=SOURCE.read_bytes()
        args=[sys.executable,'-I','-c',wrapper,str(len(payload)),hashlib.sha256(payload).hexdigest()]
        short=subprocess.run(args,input=payload[:50],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        self.assertNotEqual(0,short.returncode)
        target=home/'.local/share/termux-jobs-server/agent.py';self.assertFalse(target.exists())
        good=subprocess.run(args,input=payload,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        self.assertEqual(0,good.returncode,good.stderr.decode());self.assertEqual(payload,target.read_bytes())
        duplicate=subprocess.run(args,input=payload,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        self.assertNotEqual(0,duplicate.returncode);self.assertEqual(payload,target.read_bytes())

class DaemonIntegration(unittest.TestCase):
    def test_real_owned_jobs_disconnect_cancel_history_and_uninstall(self):
        with tempfile.TemporaryDirectory(dir=REPO/'.verification',prefix='agent-integration-') as temp:
            root=Path(temp)/'agent';root.mkdir(mode=0o700);script=root/'agent.py';script.write_bytes(SOURCE.read_bytes())
            proc=subprocess.Popen([sys.executable,str(script),'--daemon'],stdout=subprocess.DEVNULL,stderr=subprocess.PIPE)
            def rpc(**request):
                r=subprocess.run([sys.executable,str(script),'--rpc'],input=json.dumps(request).encode(),stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=20)
                data=json.loads(r.stdout)
                if not data['ok']:raise AssertionError(data)
                return data['result']
            def wait_task(ident,states,timeout=12):
                end=time.monotonic()+timeout
                while time.monotonic()<end:
                    r=rpc(op='log',id=ident)['task']
                    if r['state'] in states:return r
                    time.sleep(.15)
                raise AssertionError(r)
            try:
                end=time.monotonic()+5
                while not (root/'daemon.json').exists() and time.monotonic()<end:time.sleep(.05)
                first=rpc(op='start',command='printf hello',directory=temp,title='one')
                wait_task(first['id'],['done']);self.assertEqual('hello',rpc(op='log',id=first['id'])['log'])
                # A task survives the short-lived RPC client; no SSH session is its parent.
                second=rpc(op='start',command='sleep 1; printf survived',directory=temp)
                wait_task(second['id'],['done']);self.assertEqual('survived',rpc(op='log',id=second['id'])['log'])
                sleeper=rpc(op='start',command="trap '' TERM; sleep 60 & child=$!; printf '%s' \"$child\" > child.pid; wait \"$child\"",directory=temp)
                sibling=rpc(op='start',command='sleep 6; printf unaffected',directory=temp)
                wait_task(sleeper['id'],['running']);rpc(op='cancel',id=sleeper['id']);wait_task(sleeper['id'],['cancelled'])
                wait_task(sibling['id'],['done']);self.assertEqual('unaffected',rpc(op='log',id=sibling['id'])['log'])
                child=int((Path(temp)/'child.pid').read_text())
                child_stat=Path('/proc')/str(child)/'stat'
                if child_stat.exists():self.assertIn(child_stat.read_text().split(') ',1)[1].split()[0],('Z','X'))
                self.assertTrue(rpc(op='status')['history'])
                self.assertTrue(rpc(op='uninstall')['removed']);proc.wait(timeout=5);self.assertFalse(root.exists())
            finally:
                if proc.poll() is None:proc.terminate();proc.wait(timeout=5)
                err=proc.stderr.read().decode();proc.stderr.close()
                self.assertNotIn('Traceback',err)
if __name__=='__main__':unittest.main()
