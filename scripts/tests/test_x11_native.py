import importlib.util,os,tempfile,unittest,json,io,contextlib,hashlib,base64
from pathlib import Path
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[2]
s=importlib.util.spec_from_file_location('native',ROOT/'x11/src/main/assets/x11-native-programs.py');m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
class NativeTests(unittest.TestCase):
 def test_all_recommended_groups_map_to_reviewed_native_packages(self):
  self.assertEqual({'alpine','termux','fedora','opensuse','void','archlinux'},set(m.GROUPS))
  self.assertIn('fcitx5', (ROOT/'x11/src/main/assets/x11-native-desktop.sh').read_text())
  self.assertEqual(['libreoffice'],m.packages('termux','office'))
  self.assertIn('libreoffice-writer',m.packages('alpine','office'))
  self.assertEqual(m.packages('alpine','media'),m.packages('alpine','pkg:parole'))
  for target in m.GROUPS:
   for value in m.GROUPS[target].values():self.assertTrue(all(m.NAME.fullmatch(v) for v in value))
   for bad in ['pkg:--bad','pkg:foo;true','pkg:../file','unknown']:
    with self.assertRaises(ValueError):m.packages(target,bad)
 def test_apk_database_ownership_checksums_and_traversal(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   p=Path(d)/'installed';p.write_text('P:mousepad\nF:usr/share/applications\nR:mousepad.desktop\nZ:Q1digest\n')
   have,owners,sums=m.apk_database(p);self.assertIn('mousepad',have);self.assertEqual('mousepad',owners['/usr/share/applications/mousepad.desktop']);self.assertEqual('Q1digest',sums['/usr/share/applications/mousepad.desktop'])
   p.write_text('P:mousepad\nF:../../outside\nR:file.desktop\n')
   with self.assertRaises(ValueError):m.apk_database(p)
 def test_alpine_case_underscore_and_single_letter_names_are_preserved(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   path=Path(d)/'installed';path.write_text('P:libSvtAv1Enc\nF:usr/lib\nR:libSvtAv1Enc.so.4\n\nP:ssl_client\nF:usr/bin\nR:ssl_client\n\nP:R\nF:usr/share/applications\nR:R.desktop\nZ:Q1hash\n')
   packages,owners,sums=m.apk_database(path);self.assertEqual({'libSvtAv1Enc','ssl_client','R'},set(packages));self.assertEqual('R',owners['/usr/share/applications/R.desktop']);self.assertEqual('Q1hash',sums['/usr/share/applications/R.desktop'])
   for name in ['libSvtAv1Enc','ssl_client','R']:self.assertEqual([name],m.packages('alpine','pkg:'+name))
   for name in ['--help','../file','name;true','name arg','/path']:self.assertFalse(m.NAME.fullmatch(name))
 def test_catalog_uses_real_names_and_does_not_confuse_versioned_names(self):
  def out(args):
   return 'tool-2\nmousepad\n' if '-q' in args else 'tool-2-1.0-r0 - Tool\nmousepad-0.6.0-r0 - Text editor\n'
  with patch.object(m,'output',side_effect=out):self.assertEqual({'tool-2':'Tool','mousepad':'Text editor'},m.available('alpine'))
  text='Package: mousepad\nVersion: 1\nDescription: Text editor\n\nPackage: --bad\nDescription: Nope\n'
  with patch.object(m,'output',return_value=text):self.assertEqual({'mousepad':'Text editor'},m.available('termux'))
 def test_apk_removal_rejects_dependency_removal_and_protects_core(self):
  with patch.object(m,'output',return_value='(1/2) Purging mousepad (1)\n(2/2) Purging libsomething (1)\n'),patch.object(m,'run') as run:
   with self.assertRaises(ValueError):m.remove('alpine','mousepad')
   run.assert_not_called()
  with patch.object(m,'output',return_value='(1/1) Purging mousepad (1)\n'),patch.object(m,'run') as run,patch.object(m,'installed',return_value=set()):
   m.remove('alpine','mousepad');run.assert_called_once_with(['apk','del','--no-progress','mousepad'])
  for target in m.GROUPS:
   with patch.object(m,'run') as run:
    for p in ['xfce4-session','python3','libpulse','fcitx5','musl','apk-tools','gst-plugins-good']:
     with self.assertRaises(ValueError):m.remove(target,p)
    run.assert_not_called()
 def test_alpine_install_routes_only_through_apk_then_verifies_group(self):
  have=set();calls=[]
  def run(args):
   calls.append(args)
   if 'add' in args:have.update(m.GROUPS['alpine']['media'])
  with patch.dict(os.environ,{'JOBS_X11_NATIVE_TARGET':'alpine'}),patch.object(m.sys,'argv',['helper','install','media']),patch.object(m,'run',side_effect=run),patch.object(m,'installed',side_effect=lambda t:have),contextlib.redirect_stdout(io.StringIO()) as log:
   m.main();self.assertIn('PROGRAM:media:installed',log.getvalue());self.assertIn('READY:programs',log.getvalue());self.assertEqual('apk',calls[0][0]);self.assertEqual(['mkdir','-p','--','/var/cache/apk'],calls[1]);self.assertEqual(['apk','--cache-dir','/var/cache/apk','add'],calls[2][:4]);self.assertNotIn('apt-get',str(calls))
 def test_edited_unknown_or_unlisted_args_cannot_trigger_a_package_install(self):
  with patch.dict(os.environ,{'JOBS_X11_NATIVE_TARGET':'alpine'}),patch.object(m,'run') as run:
   for args in [['helper','install','pkg:foo;bad'],['helper','command','foo']]:
    with patch.object(m.sys,'argv',args),self.assertRaises(ValueError):m.main()
   run.assert_not_called()
 def test_native_firefox_wrapper_uses_native_prefix_and_home_and_preserves_system_file(self):
  helper=m.load('x11-maintenance')
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   root=Path(d);prefix=root/'usr';home=root/'home';home.mkdir();binary=prefix/'lib/firefox/firefox';binary.parent.mkdir(parents=True);binary.write_bytes(b'\x7fELFfixture');entry=prefix/'share/applications/firefox.desktop';entry.parent.mkdir(parents=True);entry.write_text('[Desktop Entry]\nExec='+str(prefix/'bin/firefox')+' %u\n')
   before=entry.read_bytes();helper.compatibility(home,root,True,prefix);self.assertIn(str(prefix/'bin/firefox'),(home/helper.WRAPPER_PATH).read_text());self.assertIn(str(home/helper.WRAPPER_PATH),(home/helper.LAUNCHER_PATH).read_text());self.assertEqual(before,entry.read_bytes());helper.compatibility(home,root,False,prefix);self.assertFalse((home/helper.WRAPPER_PATH).exists())
 def test_termux_run_stays_native_and_routes_audio_and_runtime_without_proot(self):
  import subprocess
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   base=Path(d)/'files';prefix=base/'usr';bin=prefix/'bin';bin.mkdir(parents=True);control=base/'workspace-x11';control.mkdir()
   pulse=bin/'pulseaudio';pulse.write_text('#!/bin/sh\nexit 1\n');pulse.chmod(0o755)
   modules=prefix/'lib/pulseaudio/modules';modules.mkdir(parents=True)
   for n in ['module-sles-sink.so','module-native-protocol-unix.so']:(modules/n).touch()
   fake=bin/'dbus-run-session';fake.write_text('#!/bin/sh\nprintf "%s\\n" "$PULSE_SERVER" "$JOBS_X11_CONTROL" "$XDG_RUNTIME_DIR" > "$CAPTURE"\n');fake.chmod(0o755)
   env={**os.environ,'PREFIX':str(prefix),'PATH':str(bin)+':'+os.environ['PATH'],'CAPTURE':str(base/'capture'),'JOBS_X11_FOCUS_TOKEN':'a11a11a1-1111-4111-8111-111111111111'}
   p=subprocess.run(['bash',str(ROOT/'x11/src/main/assets/x11-desktop.sh'),'run','termux',str(control)],env=env,capture_output=True,timeout=5)
   self.assertEqual(0,p.returncode,p.stderr);values=(base/'capture').read_text();self.assertIn(str(control),values);self.assertNotIn('/tmp/termux-jobs-x11',values)
   self.assertEqual([],list(control.glob('pulse.*')))
 def test_alpine_base_install_uses_apk_and_native_termux_install_uses_apt(self):
  import subprocess
  for target in ['alpine','termux']:
   with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
    base=Path(d);bin=base/'bin';bin.mkdir();calls=base/'calls'
    scripts={'apk':'printf "%s\\n" "apk $*" >> "$CAPTURE"\n','apt-get':'printf "%s\\n" "apt $*" >> "$CAPTURE"\n','dpkg-query':'printf "hold ok installed"\n','update-ca-certificates':'exit 0\n'}
    for n in ['python3','thunar','xfce4-terminal','xwininfo','xprop','xfwm4','xfdesktop','xfce4-panel','xfsettingsd','xfconf-query','xfce4-session','dbus-run-session','dbus-monitor','fcitx5','xauth','exo-open']:scripts[n]='exit 0\n'
    for n,body in scripts.items():p=bin/n;p.write_text('#!/bin/sh\n'+body);p.chmod(0o755)
    env={**os.environ,'PATH':str(bin)+':'+os.environ['PATH'],'PREFIX':str(base),'CAPTURE':str(calls)}
    # fd3 is an output pipe only, not an actual APK/APT transaction.
    proc=subprocess.run(['sh','-c','sh "$1" install "$2" "$3" 3>&1','fixture',str(ROOT/'x11/src/main/assets/x11-native-desktop.sh'),target,str(base)],env=env,capture_output=True,timeout=5)
    self.assertEqual(0,proc.returncode,proc.stderr);text=calls.read_text();self.assertIn('apk add' if target=='alpine' else 'apt -o',text);self.assertNotIn('apt ' if target=='alpine' else 'apk ',text)

 def test_retired_observers_are_absent_but_keyboard_and_readiness_remain(self):
  text=(ROOT/'x11/src/main/assets/x11-session.sh').read_text()
  self.assertNotIn('x11-observe.py',text);self.assertNotIn('x11-gtk-observe.so',text)
  self.assertIn('x11-focus.py',text)
  self.assertIn("'dbus-monitor','--session'",(ROOT/'x11/src/main/assets/x11-focus.py').read_text())
  self.assertIn('x11-ready.py',text);self.assertIn('JOBS_X11_DESKTOP_READY',(ROOT/'x11/src/main/assets/x11-ready.py').read_text())

 def test_termux_evince_requires_math_library_but_alpine_keeps_its_own_dependencies(self):
  self.assertEqual(['evince','libandroid-complex-math'],m.packages('termux','pdf'))
  self.assertEqual(['evince','libandroid-complex-math'],m.packages('termux','pkg:evince'))
  self.assertEqual(['evince'],m.packages('alpine','pdf'))
 def test_native_catalog_preparation_refreshes_indexes_without_installing_appstream(self):
  for target in ['alpine','termux']:
   with patch.dict(os.environ,{'JOBS_X11_NATIVE_TARGET':target,'PREFIX':'/data/data/com.termux/files/usr'}),patch.object(m.Path,'home',return_value=Path('/data/data/com.termux/files/home')),patch.object(m.sys,'argv',['helper','catalog-install','catalog-tools']),patch.object(m,'run') as run,patch.object(m,'catalog') as catalog,contextlib.redirect_stdout(io.StringIO()):
    m.main();catalog.assert_called_once();self.assertEqual(1,run.call_count);args=run.call_args[0][0];self.assertIn('update',args);self.assertNotIn('install',args);self.assertNotIn('add',args);self.assertNotIn('appstream',args)

 def test_dpkg_selection_is_not_actual_installation_state(self):
  text='evince\tdeinstall ok installed\nheld\thold ok installed\npurged-choice\tpurge ok installed\nnormal\tinstall ok installed\nbroken\tinstall reinstreq installed\nunpacked\tinstall ok unpacked\nremoved\tdeinstall ok config-files\n'
  with patch.object(m,'output',return_value=text):self.assertEqual({'evince','held','purged-choice','normal'},m.installed('termux'))
 def test_termux_evince_removes_only_confirmed_help_and_main_package(self):
  for have,expected in [({'evince','evince-help'},['evince-help','evince']),({'evince'},['evince']),({'mousepad','evince-help'},['mousepad'])]:
   name='mousepad' if 'mousepad' in have else 'evince'
   with patch.object(m,'installed',side_effect=([have,set()] if name=='evince' else [set()])),patch.object(m,'run') as run:
    m.remove('termux',name);run.assert_called_once_with(['dpkg','--remove','--',*expected])
 def test_termux_dependency_refusal_is_not_retried_or_forced(self):
  import subprocess
  with patch.object(m,'installed',return_value={'evince','evince-help'}),patch.object(m,'run',side_effect=subprocess.CalledProcessError(1,['dpkg'])) as run:
   with self.assertRaises(subprocess.CalledProcessError):m.remove('termux','evince')
   self.assertEqual(1,run.call_count);self.assertNotIn('--force',str(run.call_args))
 def test_termux_removal_must_really_remove_the_program(self):
  with patch.object(m,'installed',return_value={'evince'}),patch.object(m,'run'):
   with self.assertRaisesRegex(ValueError,'still installed'):m.remove('termux','evince')
