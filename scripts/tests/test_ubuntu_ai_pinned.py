"""Isolated control-flow tests; no network, package manager or working containers."""
import unittest,tempfile,subprocess,os,hashlib,json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];ASSETS=ROOT/'app/src/main/assets'
class UbuntuAI(unittest.TestCase):
 def setUp(self):
  base=ROOT/'.verification/ubuntu-ai-pinned/fixtures';base.mkdir(exist_ok=True)
  self.tmp=tempfile.TemporaryDirectory(dir=base);self.p=Path(self.tmp.name);self.prefix=self.p/'usr';(self.prefix/'bin').mkdir(parents=True)
  for name in ('environment-runtime.sh','agent-host-setup.sh','agent-preparation-status.sh'):(self.p/name).write_bytes((ASSETS/name).read_bytes())
  (self.p/'environment-image.py').write_text("import os;from pathlib import Path\np=Path(os.environ['PREFIX']).parent;p.joinpath('calls').open('a').write('DOWNLOAD\\n');p.joinpath('image.oci.tar').write_text('fixture');print(p/'image.oci.tar')\n")
  (self.p/'ubuntu-ai-installer.py').write_text("import sys,os;from pathlib import Path\np=Path(os.environ['PREFIX']);assert sys.argv[1]=='install' and sys.argv[3:]==['--name','ubuntu-ai'];p.parent.joinpath('calls').open('a').write('PINNED_INSTALL\\n');root=p/'var/lib/proot-distro/containers/ubuntu-ai/rootfs/bin';root.mkdir(parents=True);root.joinpath('sh').write_text('fixture');root.joinpath('sh').chmod(0o700)\n")
  self.tool=self.prefix/'bin/proot-distro';self.tool.write_text('''#!/bin/sh
if [ "$1" = install ];then echo --name;exit 0;fi
printf 'LOGIN\\n' >> "$PREFIX/../calls"
test -f "$PREFIX/var/lib/proot-distro/containers/ubuntu-ai/rootfs/bin/sh" || { echo 'incomplete fixture root' >&2;exit 31; }
''');self.tool.chmod(0o700)
  self.env={'PATH':str(self.prefix/'bin')+':/usr/bin:/bin','HOME':str(self.p),'PREFIX':str(self.prefix),'JOBS_AGENT_SETUP_ALLOWED':'1','JOBS_AGENT_UBUNTU':'ubuntu-ai','JOBS_AGENT_PREPARATION_STATUS':str(self.p/'status')}
 def tearDown(self):self.tmp.cleanup()
 def run_host(self):return subprocess.run(['/bin/bash','--noprofile','--norc','-c','source "$HOME/agent-host-setup.sh"; jobs_agent_host_prepare && printf "COMPONENTS_MAY_START\\n"'],env=self.env,capture_output=True,text=True,timeout=15)
 def test_clean_then_second_agent_and_repeat_install_once(self):
  old=self.tool.read_bytes()
  for a in ('codex','claude','gemini','opencode'):
   self.env['SELECTED_AGENT']=a;r=self.run_host();self.assertEqual(0,r.returncode,r.stderr);self.assertIn('COMPONENTS_MAY_START',r.stdout)
  calls=(self.p/'calls').read_text();self.assertEqual(1,calls.count('DOWNLOAD'));self.assertEqual(1,calls.count('PINNED_INSTALL'));self.assertEqual(old,self.tool.read_bytes())
 def test_existing_files_and_cli_are_preserved_without_download(self):
  root=self.prefix/'var/lib/proot-distro/containers/ubuntu-ai/rootfs';(root/'bin').mkdir(parents=True);(root/'bin/sh').write_text('fixture')
  files={}
  for n in ('root/.claude/credentials-fixture','root/project.txt','root/.codex/history-fixture','usr/local/bin/codex'):
   p=root/n;p.parent.mkdir(parents=True,exist_ok=True);p.write_text('synthetic preserved data');files[n]=p.read_bytes()
  r=self.run_host();self.assertEqual(0,r.returncode,r.stderr);self.assertNotIn('DOWNLOAD',(self.p/'calls').read_text())
  for n,b in files.items():self.assertEqual(b,(root/n).read_bytes())
 def test_incomplete_root_refuses_components_and_keeps_partial_files(self):
  root=self.prefix/'var/lib/proot-distro/containers/ubuntu-ai';root.mkdir(parents=True);(root/'partial').write_text('keep')
  for i in range(2):
   r=self.run_host();self.assertNotEqual(0,r.returncode);self.assertNotIn('COMPONENTS_MAY_START',r.stdout);self.assertIn('failed-ubuntu',r.stdout);self.assertEqual('keep',(root/'partial').read_text());self.assertNotIn('DOWNLOAD',(self.p/'calls').read_text())
 def test_unconfirmed_install_does_not_start_download(self):
  self.env['JOBS_AGENT_SETUP_ALLOWED']='0';r=self.run_host();self.assertNotEqual(0,r.returncode);self.assertFalse((self.p/'calls').exists())
 def test_real_bundle_is_pinned_and_separate_from_system(self):
  import zipfile
  bundle=ASSETS/'ubuntu-ai-proot-distro-5.9.0.zip';self.assertEqual('f3b14ee9e1743b79eee8942366434128bab73014b903504bf0e321f8efa46b85',hashlib.sha256(bundle.read_bytes()).hexdigest())
  with zipfile.ZipFile(bundle) as z:self.assertIn(b'Version: 5.9.0',z.read('proot_distro-5.9.0.dist-info/METADATA'));self.assertIn('LICENSE',z.namelist())
  s=(ASSETS/'environment-runtime.sh').read_text();self.assertIn('ubuntu-ai-installer.py',s);self.assertNotIn('install ubuntu --name ubuntu-ai',s)
if __name__=='__main__':unittest.main()
