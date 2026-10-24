"""Real installer supervision against isolated fixtures only, no system setup."""
from pathlib import Path
import os
import signal
import subprocess
import sys
import tempfile
import time
import unittest
import uuid

ROOT=Path(__file__).resolve().parents[2]
SUPERVISOR=ROOT/'app/src/main/assets/environment-install.py'

class EnvironmentInstallTests(unittest.TestCase):
    def setUp(self):
        parent=ROOT/'.verification/environment-install-fixtures';parent.mkdir(parents=True,exist_ok=True)
        self.tmp=tempfile.TemporaryDirectory(dir=parent);self.root=Path(self.tmp.name)
        (self.root/'bin').mkdir();(self.root/'bin/bash').symlink_to('/data/data/com.termux/files/usr/bin/bash' if sys.executable.startswith('/data/') else '/bin/bash')
        self.script=self.root/'runtime.sh';self.cancel=self.root/('cancel-'+str(uuid.uuid4()))
        self.env=dict(os.environ,PREFIX=str(self.root),PYTHONDONTWRITEBYTECODE='1')
        self.processes=[]
    def tearDown(self):
        for p in self.processes:
            if p.poll() is None:p.terminate()
            p.wait(timeout=5)
            if p.stdout:p.stdout.close()
        self.tmp.cleanup()
    def start(self,body):
        self.script.write_text('#!/bin/bash\nunset LD_LIBRARY_PATH LD_PRELOAD\n'+body)
        p=subprocess.Popen([sys.executable,'-B',str(SUPERVISOR),str(self.script),'ubuntu-ai',str(self.cancel)],env=self.env,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
        self.processes.append(p);return p
    def wait_file(self,name):
        p=self.root/name;deadline=time.monotonic()+5
        while not p.exists() and time.monotonic()<deadline:time.sleep(.02)
        self.assertTrue(p.exists(),name)
    def test_success_forwards_real_tty_progress_and_status(self):
        p=self.start('test -t 1 && test -t 2 || exit 71\nprintf "Downloading 50%%\\rDownloading 100%%\\nREADY:environment\\nREADY:shell\\nCHECK:done\\n"\n')
        out=p.communicate(timeout=5)[0];self.assertEqual(0,p.returncode,out)
        self.assertIn(b'50%',out);self.assertIn(b'CHECK:done',out)
    def test_failure_code_preserved(self):
        p=self.start('exit 19\n');p.communicate(timeout=5);self.assertEqual(19,p.returncode)
    def test_cancel_before_launch_does_not_start_installer(self):
        self.cancel.touch();p=self.start('touch "$PREFIX/started"\n');out=p.communicate(timeout=5)[0]
        self.assertEqual(130,p.returncode,out);self.assertFalse((self.root/'started').exists())
    def test_cancel_owns_only_its_installer_group(self):
        unrelated=subprocess.Popen(['/bin/sleep','20'],env={k:v for k,v in os.environ.items() if k not in {'LD_LIBRARY_PATH','LD_PRELOAD'}});self.processes.append(unrelated)
        p=self.start('(sleep 2;touch "$PREFIX/escaped") &\ntouch "$PREFIX/started"\nwait\n')
        self.wait_file('started');self.cancel.touch();out=p.communicate(timeout=6)[0]
        self.assertEqual(130,p.returncode,out);self.assertIn(b'INSTALL_CANCELLED',out)
        self.assertIsNone(unrelated.poll());time.sleep(2.1);self.assertFalse((self.root/'escaped').exists())
    def test_supervisor_sigterm_stops_installer(self):
        p=self.start('touch "$PREFIX/started"; sleep 20\n');self.wait_file('started');p.send_signal(signal.SIGTERM)
        p.communicate(timeout=6);self.assertEqual(130,p.returncode)
    def test_term_ignoring_installer_is_bounded(self):
        p=self.start('trap "" TERM; touch "$PREFIX/started"; while :; do sleep .1;done\n')
        self.wait_file('started');self.cancel.touch();p.communicate(timeout=7);self.assertEqual(130,p.returncode)
    def test_cancel_allows_installer_to_clean_partial_root_before_retry(self):
        helper=self.root/'child.py'
        helper.write_text("import pathlib,sys,time\np=pathlib.Path(sys.argv[1]);p.mkdir()\n(p.parent/'started').touch()\ntry:\n while True: time.sleep(.1)\nexcept KeyboardInterrupt:\n time.sleep(.2)\n p.rmdir()\n (p.parent/'cleaned').touch()\n")
        p=self.start('exec "'+sys.executable+'" "'+str(helper)+'" "$PREFIX/partial"\n')
        self.wait_file('started');self.cancel.touch();out=p.communicate(timeout=10)[0]
        self.assertEqual(130,p.returncode,out);self.assertTrue((self.root/'cleaned').is_file());self.assertFalse((self.root/'partial').exists())
        self.cancel.unlink();p=self.start('test ! -e "$PREFIX/partial" || exit 18\nprintf "READY:environment\\nREADY:shell\\nCHECK:done\\n"\n');out=p.communicate(timeout=5)[0];self.assertEqual(0,p.returncode,out)
    def test_control_path_outside_directory_refused(self):
        self.script.write_text('exit 0\n')
        r=subprocess.run([sys.executable,'-B',str(SUPERVISOR),str(self.script),'ubuntu-ai',str(self.root.parent/self.cancel.name)],env=self.env,capture_output=True,timeout=5)
        self.assertNotEqual(0,r.returncode)

    def test_completed_leader_cannot_leave_installer_descendant_running(self):
        p=self.start('(trap "" INT TERM; sleep 1.5; touch "$PREFIX/escaped") &\nexit 0\n')
        out=p.communicate(timeout=6)[0];self.assertEqual(0,p.returncode,out)
        time.sleep(1.7);self.assertFalse((self.root/'escaped').exists(),'descendant kept writing after success')

    def test_failed_leader_cannot_leave_installer_descendant_running(self):
        p=self.start('(trap "" INT TERM; sleep 1.5; touch "$PREFIX/escaped") &\nexit 17\n')
        out=p.communicate(timeout=6)[0];self.assertEqual(17,p.returncode,out)
        time.sleep(1.7);self.assertFalse((self.root/'escaped').exists(),'descendant kept writing after failure')

    def test_supervisor_output_error_cleans_group_before_reaping(self):
        self.script.write_text('#!/bin/bash\nunset LD_LIBRARY_PATH LD_PRELOAD\n'
            '(trap "" INT TERM; sleep 1.5; touch "$PREFIX/escaped") &\ntouch "$PREFIX/started"; sleep 20\n')
        wrapper=self.root/'fault.py'
        wrapper.write_text("import importlib.util,os,pathlib,sys\ns=importlib.util.spec_from_file_location('installer',sys.argv[1]);m=importlib.util.module_from_spec(s);s.loader.exec_module(m)\noriginal=m.select.select\ndef fail(*args):\n if (pathlib.Path(os.environ['PREFIX'])/'started').exists():raise OSError('synthetic output failure')\n return original(*args)\nm.select.select=fail\ntry:m.supervise(*sys.argv[2:])\nexcept OSError:sys.exit(29)\n")
        p=subprocess.Popen([sys.executable,'-B',str(wrapper),str(SUPERVISOR),str(self.script),'ubuntu-ai',str(self.cancel)],env=self.env,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
        self.processes.append(p);out=p.communicate(timeout=7)[0];self.assertEqual(29,p.returncode,out)
        time.sleep(1.7);self.assertFalse((self.root/'escaped').exists(),'supervisor error abandoned descendant')
