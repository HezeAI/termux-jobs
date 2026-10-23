import contextlib,hashlib,importlib.util,io,json,os,plistlib,subprocess,tempfile,unittest
from pathlib import Path
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[2]
s=importlib.util.spec_from_file_location('native',ROOT/'x11/src/main/assets/x11-native-programs.py');m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
class VoidTests(unittest.TestCase):
 def test_only_configured_packages_and_valid_repository_names(self):
  with patch.object(m,'output',return_value='ii mousepad-0.6.4_1 Editor\nuu parole-4.20.0_1 Player\nii --bad-1_1 Bad\nii libfoo-bar-1.2.3_4 Library\n'):
   self.assertEqual({'mousepad','libfoo-bar'},m.installed('void'))
  with patch.object(m,'output',return_value='[-] mousepad-0.6.4_1 Text editor\n[*] libfoo-bar-1.2.3_4 Library\n[-] ../bad-1_1 Bad\n'):
   self.assertEqual({'mousepad':'Text editor','libfoo-bar':'Library'},m.available('void'))
 def test_installs_only_selected_void_group_and_verifies_dependencies(self):
  have=set()
  def run(args):
   if args[:2]==['xbps-install','-y']:have.update(m.GROUPS['void']['media'])
  with patch.dict(os.environ,{'JOBS_X11_NATIVE_TARGET':'void'}),patch.object(m.sys,'argv',['helper','install','media']),patch.object(m,'run',side_effect=run) as call,patch.object(m,'installed',side_effect=lambda _:have),patch.object(m,'load') as loader,contextlib.redirect_stdout(io.StringIO()):
   loader.return_value.state.return_value='off';m.main();self.assertEqual(['xbps-install','-y','--',*m.GROUPS['void']['media']],call.call_args.args[0]);self.assertEqual(['xbps-install','-S'],call.call_args_list[0].args[0])
 def test_single_removal_keeps_dependencies_and_refuses_wider_plans(self):
  with patch.object(m,'output',return_value='mousepad-0.6.4_1 remove aarch64 /repo 100 20\n'),patch.object(m,'run') as call,patch.object(m,'installed',return_value=set()):
   m.remove('void','mousepad');call.assert_called_once_with(['xbps-remove','-y','--','mousepad'])
  with patch.object(m,'output',return_value='mousepad-0.6.4_1 remove aarch64 /repo 100 20\nlibfoo-1.0_1 remove aarch64 /repo 100 20\n'),patch.object(m,'run') as call:
   with self.assertRaises(ValueError):m.remove('void','mousepad')
   call.assert_not_called()
  for name in ['xbps','xbps-install','base-files','thunar','xfce4-panel']:
   with self.assertRaises(ValueError):m.remove('void',name)
 def test_real_xbps_manifest_shape_filters_paths_and_extracts_checksum(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as tmp:
   d=Path(tmp);digest=hashlib.sha256(b'launcher').hexdigest();(d/'.mousepad-files.plist').write_bytes(plistlib.dumps({'files':[{'file':'/usr/share/applications/mousepad.desktop','sha256':digest},{'file':'/etc/password','sha256':digest},{'file':'/usr/share/applications/../bad.desktop','sha256':digest}]}))
   owners,sums=m.xbps_manifests(d);self.assertEqual({'/usr/share/applications/mousepad.desktop':'mousepad'},owners);self.assertEqual(digest,sums['/usr/share/applications/mousepad.desktop'])
 def test_baseline_void_uses_xbps_and_configured_state(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as tmp:
   d=Path(tmp);bin=d/'bin';bin.mkdir();calls=d/'calls'
   for name in ['xbps-query','xbps-install','update-ca-certificates','python3','thunar','xfce4-terminal','xwininfo','xprop','xfwm4','xfdesktop','xfce4-panel','xfsettingsd','xfconf-query','xfce4-session','dbus-run-session','dbus-monitor','fcitx5','xauth']:
    p=bin/name;p.write_text('#!/bin/sh\nprintf "%s\\n" "'+name+':$*" >> "$CAPTURE"\n'+('printf installed\n' if name=='xbps-query' else ''));p.chmod(0o700)
   p=subprocess.run(['/bin/sh',str(ROOT/'x11/src/main/assets/x11-native-desktop.sh'),'install','void'],env=dict(os.environ,PATH=str(bin)+':/usr/bin:/bin',CAPTURE=str(calls)),text=True,capture_output=True,timeout=6)
   self.assertEqual(0,p.returncode,p.stderr);text=calls.read_text();self.assertIn('xbps-install:-S',text);self.assertIn('xbps-install:-y -- ',text);self.assertIn('libllvm18',text);self.assertIn('Thunar',text);self.assertIn('xbps-query:-p state -- ',text)
   for bad in ['apt-get','rpm:','dnf:','apk:','--force','--insecure','--ignore']:self.assertNotIn(bad,text)
 def test_pdf_group_in_opensuse_requires_the_working_plugin(self):
  self.assertEqual(['evince','evince-plugin-pdfdocument'],m.packages('opensuse','pdf'));self.assertEqual(m.packages('opensuse','pdf'),m.packages('opensuse','pkg:evince'));self.assertEqual(['evince'],m.packages('void','pdf'))
 def test_all_void_x11_entries_bypass_missing_passwd_bash(self):
  # Model proot-distro v5: login refuses the image's absent passwd shell
  # even when a /bin/sh command follows '--'. run executes that command.
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as temporary:
   base=Path(temporary);prefix=base/'files/usr';bin=prefix/'bin';bin.mkdir(parents=True);control=prefix.parent/'workspace-x11-void';control.mkdir();(prefix/'var/lib/proot-distro/containers/void/rootfs').mkdir(parents=True)
   capture=base/'calls';fake=bin/'proot-distro'
   fake.write_text('#!/usr/bin/python3\nimport json,os,sys\na=sys.argv[1:]\nif a[0]=="login":sys.exit(71)\nassert a[:3]==["run","--work-dir","/root"]\nassert a[a.index("--")-1]=="void"\nassert a[a.index("--")+1]=="/bin/sh"\nwith open(os.environ["CAPTURE"],"a") as f:f.write(json.dumps(a)+"\\n")\n');fake.chmod(0o700)
   pulse=bin/'pulseaudio';pulse.write_text('#!/bin/sh\nexit 0\n');pulse.chmod(0o700)
   modules=prefix/'lib/pulseaudio/modules';modules.mkdir(parents=True)
   for name in ['module-sles-sink.so','module-native-protocol-unix.so']:(modules/name).touch()
   env=dict(os.environ,PREFIX=str(prefix),PATH=str(bin)+':/usr/bin:/bin',CAPTURE=str(capture),JOBS_X11_FOCUS_TOKEN='a11a11a1-1111-4111-8111-111111111111')
   assets=ROOT/'x11/src/main/assets'
   for operation in ['check','install','run','repair-launchers','firefox-compat-on','firefox-compat-off']:
    result=subprocess.run(['bash',str(assets/'x11-desktop.sh'),operation,'void',str(control)],env=env,capture_output=True,text=True,timeout=8)
    self.assertEqual(0,result.returncode,(operation,result.stdout,result.stderr))
   for operation,program in [('check',''),('install','editor'),('resume','editor'),('catalog',''),('catalog-install','catalog-tools'),('installed',''),('remove','pkg:mousepad'),('firefox-dns-on',''),('firefox-dns-off','')]:
    result=subprocess.run(['bash',str(assets/'x11-programs.sh'),operation,'void',str(control),program],env=env,capture_output=True,text=True,timeout=8)
    self.assertEqual(0,result.returncode,(operation,result.stdout,result.stderr))
   calls=[json.loads(line) for line in capture.read_text().splitlines()];self.assertEqual(16,len(calls))
   self.assertTrue(all('--bind' in call for call in calls));self.assertTrue(any('JOBS_X11_DISPLAY=104' in call[-1] for call in calls))

 def test_xbps_program_subprocesses_get_ipv4_without_changing_other_commands_or_parent(self):
  with patch.dict(os.environ,{'FORCE_IPV4':'0','FORCE_IPV6':'1'}):
   for binary in ['xbps-install','xbps-query','xbps-remove','xbps-fetch']:
    child=m.manager_environment([binary,'-S']);self.assertEqual('1',child['FORCE_IPV4']);self.assertNotIn('FORCE_IPV6',child)
   for binary in ['apt-get','apk','dnf','zypper','python3']:
    child=m.manager_environment([binary]);self.assertEqual('0',child['FORCE_IPV4']);self.assertEqual('1',child['FORCE_IPV6'])
   with patch.object(m.subprocess,'run') as call:m.run(['xbps-install','-S']);self.assertEqual('1',call.call_args.kwargs['env']['FORCE_IPV4']);self.assertNotIn('FORCE_IPV6',call.call_args.kwargs['env'])
   with patch.object(m.subprocess,'check_output',return_value='') as call:m.output(['xbps-query','-Rs','']);self.assertEqual('1',call.call_args.kwargs['env']['FORCE_IPV4']);self.assertNotIn('FORCE_IPV6',call.call_args.kwargs['env'])
   self.assertEqual('0',os.environ['FORCE_IPV4']);self.assertEqual('1',os.environ['FORCE_IPV6'])

 def test_shell_xbps_ipv4_scope_leaves_next_non_xbps_command_unchanged(self):
  text=(ROOT/'x11/src/main/assets/x11-native-desktop.sh').read_text();definition=text.split('xbps_command() {',1)[1].split('\ncheck() {',1)[0]
  script='xbps_command() {'+definition+'\nxbps_fixture() { printf "%s:%s\\n" "$FORCE_IPV4" "${FORCE_IPV6-unset}"; }\nxbps_command xbps_fixture\nxbps_fixture\n'
  result=subprocess.run(['/bin/sh','-c',script],env=dict(os.environ,FORCE_IPV4='0',FORCE_IPV6='1'),capture_output=True,text=True,timeout=3)
  self.assertEqual(0,result.returncode,result.stderr);self.assertEqual('1:unset\n0:1\n',result.stdout)
