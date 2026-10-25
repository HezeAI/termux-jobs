"""Owned process lifecycle and target routing, without any installed rootfs/packages."""
import os,signal,subprocess,tempfile,time,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
GUARD=ROOT/'x11/src/main/assets/x11-guardian.sh'
SCRIPT=ROOT/'x11/src/main/assets/x11-desktop.sh'
class DesktopTests(unittest.TestCase):
 def start(self,*args):return subprocess.Popen(['setsid','bash',str(GUARD),*args],stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
 def test_completion_and_failure_are_reported_before_own_group_cleanup(self):
  for code in [0,7]:
   p=self.start('bash','-c',f'exit {code}')
   # communicate closes stdin too early; owner keeps it open until completion.
   output=p.stdout.read();p.wait(timeout=5);p.stdin.close();p.stdout.close()
   self.assertIn(f'DESKTOP_CHILD_EXIT={code}'.encode(),output)
 def test_owner_eof_cancels_owned_group_and_leaves_unrelated_process(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   pidfile=Path(d)/'child'
   sentinel=subprocess.Popen(['sleep','30'])
   p=self.start('bash','-c',f'echo $$ > "{pidfile}"; exec sleep 30')
   try:
    child_text=''
    for _ in range(100):
     try:child_text=pidfile.read_text().strip()
     except FileNotFoundError:pass
     if child_text.isdecimal():break
     time.sleep(.02)
    self.assertTrue(child_text.isdecimal(),"owned child did not publish its PID");child=int(child_text)
    self.assertIsNone(p.poll());p.stdin.close();p.wait(timeout=5)
    self.assertIsNone(sentinel.poll())
    # A zombie is already dead; allow the container init to reap it later.
    proc=Path(f'/proc/{child}/stat')
    self.assertTrue(not proc.exists() or proc.read_text().split(') ',1)[1].startswith('Z '))
    self.assertNotIn(b'DESKTOP_CHILD_EXIT=0',p.stdout.read())
   finally:
    if p.poll() is None:p.stdin.close();p.wait(timeout=5)
    sentinel.terminate();sentinel.wait();p.stdout.close()
 def test_stop_kills_owned_desktop_tree_for_each_target_and_not_another_session(self):
  # Real guardian + real routing script; only distro/DBus desktop binaries are fixtures.
  # Includes a TERM-resistant grandchild to exercise the group's KILL fallback.
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   base=Path(d)/'files';prefix=base/'usr';bin=prefix/'bin';bin.mkdir(parents=True)
   control=base/'workspace-x11';control.mkdir()
   stub=base/'desktop.sh';stub.write_text('echo $$ > "$PIDFILE"\nsleep 30 &\necho $! >> "$PIDFILE"\ntrap "" TERM\nwait\n');stub.chmod(0o755)
   fake=bin/'proot-distro';fake.write_text('#!/bin/bash\n[[ "$*" == *cleanup-shortcuts* ]] && exit 0\nexec bash "$FAKE_DESKTOP"\n');fake.chmod(0o755)
   dbus=bin/'dbus-run-session';dbus.write_text('#!/bin/sh\nshift\nexec "$@"\n');dbus.chmod(0o755)
   (control/'x11-session.sh').write_text(stub.read_text())
   (bin/'python3').symlink_to('/usr/bin/python3')
   env={**os.environ,'PREFIX':str(prefix),'PATH':str(bin)+':'+os.environ['PATH'],'FAKE_DESKTOP':str(stub),'JOBS_X11_FOCUS_TOKEN':'a11a11a1-1111-4111-8111-111111111111'}
   sentinel=subprocess.Popen(['sleep','30'])
   try:
    for target in ['ubuntu','jobs-x11-ubuntu','debian','alpine','fedora','opensuse','termux','void','archlinux']:
     (prefix/f'var/lib/proot-distro/containers/{target}/rootfs').mkdir(parents=True)
     pidfile=base/('pids-'+target);env['PIDFILE']=str(pidfile)
     proc=subprocess.Popen(['setsid','bash',str(GUARD),'bash',str(SCRIPT),'run',target,str(control)],env=env,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
     try:
      pids=[]
      for _ in range(150):
       try:pids=[int(x) for x in pidfile.read_text().split()]
       except FileNotFoundError:pass
       if len(pids)==2:break
       time.sleep(.02)
      if len(pids)!=2:
       proc.stdin.close();proc.wait(timeout=5);self.fail((target,proc.stdout.read().decode(errors="replace")))
      for pid in pids:self.assertEqual(proc.pid,os.getpgid(pid),target)
      proc.stdin.close();proc.wait(timeout=5)
      for pid in pids:
       stat=Path(f'/proc/{pid}/stat')
       self.assertTrue(not stat.exists() or stat.read_text().split(') ',1)[1].startswith('Z '),(target,pid))
      self.assertIsNone(sentinel.poll(),target)
     finally:
      if proc.poll() is None:proc.stdin.close();proc.wait(timeout=5)
      proc.stdout.close()
   finally:sentinel.terminate();sentinel.wait()
 def test_ordinary_ubuntu_launcher_repair_uses_only_its_existing_rootfs(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   base=Path(d)/'files';prefix=base/'usr';bin=prefix/'bin';bin.mkdir(parents=True);control=base/'workspace-x11-ubuntu';control.mkdir()
   root=prefix/'var/lib/proot-distro/containers/ubuntu/rootfs';root.mkdir(parents=True);sentinel=root/'keep';sentinel.write_text('project')
   capture=base/'calls';tool=bin/'proot-distro';tool.write_text('#!/bin/sh\nprintf "%s\\n" "$@" >> "$CAPTURE"\n');tool.chmod(0o700)
   env=dict(os.environ,PREFIX=str(prefix),PATH=str(bin)+':/usr/bin:/bin',CAPTURE=str(capture))
   r=subprocess.run(['/bin/bash',str(SCRIPT),'repair-launchers','ubuntu',str(control)],env=env,capture_output=True,text=True,timeout=5)
   self.assertEqual(0,r.returncode,r.stdout+r.stderr);self.assertIn('READY:desktop',r.stdout)
   calls=capture.read_text();self.assertIn('ubuntu\n',calls);self.assertIn('x11-launchers.py --repair',calls);self.assertNotIn('jobs-x11-ubuntu',calls);self.assertNotIn('install\n',calls);self.assertEqual('project',sentinel.read_text())
 def test_guardian_refuses_nonprivate_group(self):
  p=subprocess.run(['bash',str(GUARD),'true'],capture_output=True,timeout=5)
  self.assertEqual(2,p.returncode);self.assertIn(b'PRIVATE_PROCESS_GROUP_REQUIRED',p.stdout)
 def test_target_cannot_be_shell_command_or_other_distribution(self):
  for target in ['ubuntu-ai','ubuntu; echo injected','../ubuntu','']:
   p=subprocess.run(['bash',str(SCRIPT),'install',target,'/unused'],capture_output=True,timeout=5)
   self.assertEqual(2,p.returncode)
 def test_dedicated_ubuntu_checks_only_its_own_alias_and_does_not_install(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   base=Path(d)/'files';prefix=base/'usr';bin=prefix/'bin';bin.mkdir(parents=True)
   control=base/'workspace-x11';control.mkdir()
   for alias in ['ubuntu','jobs-x11-ubuntu']:(prefix/f'var/lib/proot-distro/containers/{alias}/rootfs').mkdir(parents=True)
   pulse=bin/'pulseaudio';pulse.write_text('#!/bin/sh\nexit 1\n');pulse.chmod(0o755)
   modules=prefix/'lib/pulseaudio/modules';modules.mkdir(parents=True);(modules/'module-sles-sink.so').touch();(modules/'module-native-protocol-unix.so').touch()
   fake=bin/'proot-distro';fake.write_text('#!/bin/bash\nprintf "%s\\n" "$@" >> "$CAPTURE"\nexit 0\n');fake.chmod(0o755)
   capture=base/'calls';env={**os.environ,'PREFIX':str(prefix),'PATH':str(bin)+':'+os.environ['PATH'],'CAPTURE':str(capture)}
   p=subprocess.run(['bash',str(SCRIPT),'check','jobs-x11-ubuntu',str(control)],env=env,capture_output=True,timeout=5)
   self.assertEqual(0,p.returncode,p.stderr);self.assertIn(b'READY:desktop',p.stdout)
   args=capture.read_text().splitlines();self.assertIn('jobs-x11-ubuntu',args);self.assertNotIn('ubuntu',args);self.assertNotIn('install',args)
   self.assertIn(f'{prefix}/tmp:/tmp',args);self.assertIn(f'{control}:/tmp/termux-jobs-x11',args)
 def test_existing_debian_check_install_run_and_repair_never_create_or_route_to_ubuntu(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   base=Path(d)/'files';prefix=base/'usr';bin=prefix/'bin';bin.mkdir(parents=True);control=base/'workspace-x11';control.mkdir()
   root=prefix/'var/lib/proot-distro/containers/debian/rootfs';root.mkdir(parents=True)
   sentinel=root/'keep.txt';sentinel.write_text('user project')
   pulse=bin/'pulseaudio';pulse.write_text('#!/bin/sh\nexit 1\n');pulse.chmod(0o755)
   modules=prefix/'lib/pulseaudio/modules';modules.mkdir(parents=True);(modules/'module-sles-sink.so').touch();(modules/'module-native-protocol-unix.so').touch()
   fake=bin/'proot-distro';fake.write_text('#!/bin/bash\nprintf "%s\\n" "$@" >> "$CAPTURE"\n');fake.chmod(0o755)
   capture=base/'calls';env={**os.environ,'PREFIX':str(prefix),'PATH':str(bin)+':'+os.environ['PATH'],'CAPTURE':str(capture),'JOBS_X11_FOCUS_TOKEN':'a11a11a1-1111-4111-8111-111111111111'}
   for op in ['check','install','run','repair-launchers','firefox-compat-on']:
    capture.write_text('');p=subprocess.run(['bash',str(SCRIPT),op,'debian',str(control)],env=env,capture_output=True,timeout=5)
    self.assertEqual(0,p.returncode,p.stderr);args=capture.read_text().splitlines();self.assertIn('debian',args);self.assertNotIn('ubuntu',args);self.assertNotIn('jobs-x11-ubuntu',args);self.assertNotIn('install',args)
    self.assertEqual('user project',sentinel.read_text())
    if op=='run':self.assertNotIn('cleanup-shortcuts',capture.read_text())
   sentinel.unlink();root.rmdir();capture.unlink()
   p=subprocess.run(['bash',str(SCRIPT),'install','debian',str(control)],env=env,capture_output=True,timeout=5)
   self.assertEqual(1,p.returncode);self.assertIn(b'INSTALL_ENVIRONMENT_FIRST',p.stdout);self.assertFalse(capture.exists());self.assertFalse(root.exists())
 def test_android_equivalent_files_paths_are_accepted_but_other_directory_is_not(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   base=Path(d)/'files';prefix=base/'usr';prefix.mkdir(parents=True)
   control=base/'workspace-x11';control.mkdir();alias=Path(d)/'android-files';alias.symlink_to(base,target_is_directory=True)
   env={**os.environ,'PREFIX':str(prefix)}
   p=subprocess.run(['bash',str(SCRIPT),'check','jobs-x11-ubuntu',str(alias/'workspace-x11')],env=env,capture_output=True,timeout=5)
   self.assertEqual(0,p.returncode,p.stderr);self.assertIn(b'MISSING:environment',p.stdout)
   other=Path(d)/'other';other.mkdir()
   p=subprocess.run(['bash',str(SCRIPT),'check','jobs-x11-ubuntu',str(other)],env=env,capture_output=True,timeout=5)
   self.assertEqual(2,p.returncode);self.assertIn(b'CONTROL_PATH',p.stdout)
 def test_identity_check_does_not_depend_on_canonical_path_text(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   base=Path(d)/'files';prefix=base/'usr';prefix.mkdir(parents=True)
   control=base/'workspace-x11';control.mkdir();alias=Path(d)/'android-alias';alias.symlink_to(base,target_is_directory=True)
   env={**os.environ,'PREFIX':str(prefix)}
   # Simulate differing directory names returned by Android bind-mount aliases.
   # An implementation that relies on pwd must fail here; inode identity must work.
   env['BASH_FUNC_pwd%%']='() { return 91; }'
   p=subprocess.run(['bash',str(SCRIPT),'check','jobs-x11-ubuntu',str(alias/'workspace-x11')],env=env,capture_output=True,timeout=5)
   self.assertEqual(0,p.returncode,p.stderr);self.assertIn(b'MISSING:environment',p.stdout)
 def test_retired_gtk_module_is_not_requested_for_any_guest(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   base=Path(d)/'files';prefix=base/'usr';bin=prefix/'bin';bin.mkdir(parents=True);control=base/'workspace-x11';control.mkdir()
   pulse=bin/'pulseaudio';pulse.write_text('#!/bin/sh\nexit 1\n');pulse.chmod(0o755)
   modules=prefix/'lib/pulseaudio/modules';modules.mkdir(parents=True);(modules/'module-sles-sink.so').touch();(modules/'module-native-protocol-unix.so').touch()
   fake=bin/'proot-distro';fake.write_text('#!/bin/bash\nprintf "%s\\n" "$@" > "$CAPTURE"\n');fake.chmod(0o755)
   capture=base/'calls';env={**os.environ,'PREFIX':str(prefix),'PATH':str(bin)+':'+os.environ['PATH'],'CAPTURE':str(capture),'JOBS_X11_FOCUS_TOKEN':'a11a11a1-1111-4111-8111-111111111111'}
   (prefix/'var/lib/proot-distro/containers/archlinux/rootfs').mkdir(parents=True);(bin/'python3').symlink_to('/usr/bin/python3')
   displays={'ubuntu':98,'jobs-x11-ubuntu':99,'debian':100,'alpine':101,'fedora':102,'opensuse':103,'void':104,'archlinux':105}
   for target in displays:
    env['JOBS_X11_STYLE']='0' if target=='jobs-x11-ubuntu' else '1'
    env['JOBS_X11_SCALE']='100' if target=='jobs-x11-ubuntu' else '75'
    result=subprocess.run(['bash',str(SCRIPT),'run',target,str(control)],env=env,capture_output=True,timeout=5)
    self.assertEqual(0,result.returncode,result.stderr);args=capture.read_text();self.assertNotIn('JOBS_X11_GTK_OBSERVE',args);self.assertIn('dbus-run-session',args);self.assertIn('export DISPLAY=:$JOBS_X11_DISPLAY',args);self.assertIn('export JOBS_X11_DISPLAY='+str(displays[target]),args)
    self.assertIn('JOBS_X11_STYLE='+env['JOBS_X11_STYLE']+' ',args);self.assertIn('JOBS_X11_SCALE='+env['JOBS_X11_SCALE']+';',args)
    guest_command=next(line for line in args.splitlines() if 'export JOBS_X11_DISPLAY=' in line);prefix_command=guest_command.split('; mkdir ',1)[0];probe=subprocess.run(['bash','-c',prefix_command+'; printf \"%s\\n\" \"$DISPLAY\"'],env={},capture_output=True,text=True,timeout=5);self.assertEqual(':'+str(displays[target]),probe.stdout.strip());self.assertEqual(0,probe.returncode,probe.stderr)
   self.assertEqual(['archlinux'],sorted(p.name for p in (prefix/'var/lib/proot-distro/containers').iterdir()))
 def test_lxqt_is_rejected_outside_test_and_cannot_inject_a_command(self):
  for target,desktop in [('ubuntu','lxqt'),('debian','lxqt'),('jobs-x11-ubuntu','lxqt;true')]:
   p=subprocess.run(['bash',str(SCRIPT),'install',target,'/unused',desktop],capture_output=True,timeout=5)
   self.assertEqual(2,p.returncode)
 def test_retired_lxqt_session_is_refused_before_starting_helpers(self):
  p=subprocess.run(['sh',str(ROOT/'x11/src/main/assets/x11-session.sh')],env={**os.environ,'JOBS_X11_DESKTOP':'lxqt','JOBS_X11_FOCUS_TOKEN':'a11a11a1-1111-4111-8111-111111111111'},capture_output=True,timeout=5)
  self.assertEqual(2,p.returncode);self.assertIn(b'ERROR:DESKTOP_CHOICE',p.stdout)
 def test_firefox_compatibility_is_selected_guest_scoped(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   base=Path(d)/'files';prefix=base/'usr';bin=prefix/'bin';bin.mkdir(parents=True);control=base/'workspace-x11';control.mkdir()
   (prefix/'var/lib/proot-distro/containers/jobs-x11-ubuntu/rootfs').mkdir(parents=True)
   pulse=bin/'pulseaudio';pulse.write_text('#!/bin/sh\nexit 1\n');pulse.chmod(0o755)
   modules=prefix/'lib/pulseaudio/modules';modules.mkdir(parents=True);(modules/'module-sles-sink.so').touch();(modules/'module-native-protocol-unix.so').touch()
   fake=bin/'proot-distro';fake.write_text('#!/bin/bash\nprintf "%s\\n" "$@" >> "$CAPTURE"\n');fake.chmod(0o755)
   capture=base/'calls';env={**os.environ,'PREFIX':str(prefix),'PATH':str(bin)+':'+os.environ['PATH'],'CAPTURE':str(capture)}
   (prefix/'var/lib/proot-distro/containers/ubuntu/rootfs').mkdir(parents=True)
   p=subprocess.run(['bash',str(SCRIPT),'firefox-compat-on','ubuntu',str(control)],env=env,capture_output=True,timeout=5);self.assertEqual(0,p.returncode,p.stderr);self.assertIn('ubuntu',capture.read_text().splitlines());self.assertNotIn('ubuntu-ai',capture.read_text().splitlines());capture.unlink()
   p=subprocess.run(['bash',str(SCRIPT),'firefox-compat-on','jobs-x11-ubuntu',str(control)],env=env,capture_output=True,timeout=5);self.assertEqual(0,p.returncode,p.stderr);args=capture.read_text();self.assertIn('x11-maintenance.py',args);self.assertNotIn('login\nubuntu',args)
 def test_trial_overlay_is_disabled_for_both_saved_preferences(self):
  text=(ROOT/'x11/src/main/assets/x11-session.sh').read_text()
  self.assertNotIn('export LD_LIBRARY_PATH=',text);self.assertNotIn('x11-gtk-observe.so',text)
  code=(ROOT/'x11/src/main/java/com/termux/x11/DesktopProcess.java').read_text();self.assertIn('put("JOBS_X11_GARCON_FILTER","0")',code)
if __name__=='__main__':unittest.main()
