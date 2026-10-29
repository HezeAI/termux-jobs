"""Kali-only tool routing and owned shortcuts; package managers are simulated here."""
import os,subprocess,tempfile,unittest
from pathlib import Path
from scripts.tests import test_x11_programs as programs
ROOT=programs.ROOT
from scripts.tests.test_x11_app_launchers import m
class KaliTools(unittest.TestCase):
 def test_all_groups_install_resume_and_check_required_binaries(self):
  groups={'kali-nmap':('nmap',),'kali-wireshark':('wireshark',),'kali-burp':('burpsuite','default-jre'),'kali-apktool':('apktool','default-jre-headless')}
  harness=programs.ProgramTests()
  for key,packages in groups.items():
   with self.subTest(key=key),tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
    control,env=harness.fixture(d);bin=Path(env['PREFIX'])/'bin';(Path(env['PREFIX'])/'var/lib/proot-distro/containers/kali/rootfs').mkdir(parents=True)
    apt=bin/'apt-get';apt.write_text(apt.read_text().replace('case $p in thunar|','case $p in nmap|wireshark|burpsuite|default-jre|apktool|default-jre-headless|thunar|'))
    for name in ('nmap','wireshark','burpsuite','apktool','java'):
     p=bin/name;p.write_text('#!/bin/sh\nexit 0\n');p.chmod(0o700)
    for op in ('install','resume'):
     result=harness.run_program(env,control,op,key,'kali');self.assertEqual(0,result.returncode,result.stdout+result.stderr);self.assertIn('PROGRAM:'+key+':installed',result.stdout)
     self.assertEqual(16,sum(x.startswith('PROGRAM:') for x in result.stdout.splitlines()));self.assertTrue(set(packages)<=set(Path(env['INSTALLED']).read_text().splitlines()));self.assertIn('\nkali\n','\n'+Path(env['ROUTE']).read_text());self.assertNotIn('ubuntu-ai',Path(env['ROUTE']).read_text());Path(env['APT_CALLS']).unlink()
    for name in ('bash','sed','awk'):
     if not (bin/name).exists():(bin/name).symlink_to('/usr/bin/'+name)
    (bin/packages[0]).unlink();env['PATH']=str(bin)
    result=harness.run_program(env,control,'check','','kali');self.assertEqual(0,result.returncode,result.stderr);self.assertIn('PROGRAM:'+key+':missing',result.stdout);self.assertFalse(Path(env['APT_CALLS']).exists())
 def test_non_kali_rejects_tool_before_any_guest_call(self):
  harness=programs.ProgramTests()
  for target in ('ubuntu','debian','manjaro','termux'):
   with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
    control,env=harness.fixture(d);r=harness.run_program(env,control,'install','kali-nmap',target);self.assertEqual(2,r.returncode);self.assertIn('ERROR:PROGRAM_TARGET',r.stdout);self.assertFalse(Path(env['ROUTE']).exists());self.assertFalse(Path(env['APT_CALLS']).exists())
 def test_owned_shortcuts_follow_presence_preserve_user_files_and_cli_arguments(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   base=Path(d);root=base/'root';root.mkdir();control=base/'control';control.mkdir();home=base/'home';home.mkdir();bin=root/'usr/bin';bin.mkdir(parents=True)
   script=bin/'nmap';script.write_text('#!/usr/bin/python3\nimport json,os,sys\nprint(json.dumps([sys.argv[1:],os.getenv("NMAP_UNPRIVILEGED")]))\n');script.chmod(0o700)
   folder=m.prepare('kali',control,root);m.shortcuts(home,control);shortcut=home/'.local/share/applications/termux-jobs-jobs-nmap.desktop';self.assertTrue(shortcut.is_file());self.assertIn('--command=jobs-nmap',shortcut.read_text());self.assertFalse(shortcut.with_name('termux-jobs-jobs-apktool.desktop').exists())
   import json
   r=subprocess.run([str(folder/'jobs-nmap'),'-sT','example.test; touch no'],capture_output=True,text=True,check=True);self.assertEqual([['--unprivileged','-sT','example.test; touch no'],'1'],json.loads(r.stdout))
   script.unlink();m.prepare('kali',control,root);m.shortcuts(home,control);self.assertFalse(shortcut.exists());shortcut.write_text('my shortcut')
   with self.assertRaisesRegex(ValueError,'LOCAL_CHANGES'):m.shortcuts(home,control)
   self.assertEqual('my shortcut',shortcut.read_text())
if __name__=='__main__':unittest.main()
