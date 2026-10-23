import contextlib,importlib.util,io,os,subprocess,tempfile,unittest
from pathlib import Path
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[2]
s=importlib.util.spec_from_file_location('native',ROOT/'x11/src/main/assets/x11-native-programs.py');m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
class OpenSuseTests(unittest.TestCase):
 def test_xml_catalog_filters_non_packages_and_unsafe_names(self):
  xml='<stream><solvable-list><solvable kind="package" name="MozillaFirefox"/><solvable kind="package" name="mousepad"/><solvable kind="package" name="../bad"/><solvable kind="pattern" name="desktop"/></solvable-list></stream>'
  with patch.object(m,'output',return_value=xml) as output:
   self.assertEqual({'MozillaFirefox':'','mousepad':''},m.available('opensuse'));self.assertIn('--no-refresh',output.call_args.args[0]);self.assertEqual('zypper',output.call_args.args[0][0])
 def test_rpm_installed_and_removal_without_dependency_bypass(self):
  with patch.object(m,'output',return_value='MozillaFirefox\n--bad\n'):self.assertEqual({'MozillaFirefox'},m.installed('opensuse'))
  with patch.object(m,'run') as run,patch.object(m,'installed',return_value=set()):m.remove('opensuse','mousepad');run.assert_called_once_with(['rpm','-e','--','mousepad'])
  for n in ['zypper','rpm','glibc','../bad']:
   with self.assertRaises(ValueError):m.remove('opensuse',n)
 def test_pdf_removal_includes_only_installed_reviewed_companion(self):
  for target,have,expected in [('opensuse',{'evince','evince-plugin-pdfdocument'},['evince-plugin-pdfdocument','evince']),('opensuse',{'evince'},['evince']),('fedora',{'evince','evince-plugin-pdfdocument'},['evince'])]:
   with patch.object(m,'installed',side_effect=[have,set()] if target=='opensuse' else [set()]),patch.object(m,'run') as run:
    m.remove(target,'evince');run.assert_called_once_with(['rpm','-e','--',*expected])
 def test_pdf_dependency_refusal_propagates_without_force(self):
  with patch.object(m,'installed',return_value={'evince','evince-plugin-pdfdocument'}),patch.object(m,'run',side_effect=subprocess.CalledProcessError(1,['rpm'])) as run:
   with self.assertRaises(subprocess.CalledProcessError):m.remove('opensuse','evince')
   run.assert_called_once_with(['rpm','-e','--','evince-plugin-pdfdocument','evince'])
 def test_install_uses_zypper_and_checks_all_packages(self):
  for complete in [True,False]:
   have=set()
   def run(args):
    if complete and 'install' in args:have.update(m.GROUPS['opensuse']['browser'])
   with patch.dict(os.environ,{'JOBS_X11_NATIVE_TARGET':'opensuse'}),patch.object(m.sys,'argv',['helper','install','browser']),patch.object(m,'run',side_effect=run) as call,patch.object(m,'installed',side_effect=lambda _:have),contextlib.redirect_stdout(io.StringIO()):
    if complete:m.main()
    else:
     with self.assertRaisesRegex(ValueError,'not fully installed'):m.main()
    self.assertEqual(['zypper','--non-interactive','--no-refresh','install','--no-recommends','MozillaFirefox'],call.call_args.args[0])
 def test_codec_repo_is_filtered_only_in_temporary_view_without_disabling_gpg(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as temporary:
   directory=Path(temporary);repo=directory/'official.repo';original='[oss]\nbaseurl=https://download.opensuse.org/distribution/leap/16.0/repo/oss/\nenabled=1\ngpgcheck=1\n[codec]\nbaseurl=https://codecs.opensuse.org/openh264/openSUSE_Leap_16/\nenabled=1\ngpgcheck=1\n';repo.write_text(original)
   with m.zypper_repositories(directory) as args:
    self.assertEqual('--reposd-dir',args[0]);view=Path(args[1]);ini=m.configparser.ConfigParser(interpolation=None);ini.read(view/'official.repo');self.assertEqual('0',ini['codec']['enabled']);self.assertEqual('1',ini['codec']['gpgcheck']);self.assertEqual('1',ini['oss']['enabled']);self.assertEqual(original,repo.read_text())
   self.assertFalse(view.exists());self.assertEqual(original,repo.read_text())
   repo.write_text('[custom]\nbaseurl=https://codecs.opensuse.org.evil.example/openh264/test\nenabled=1\ngpgcheck=1\n')
   with m.zypper_repositories(directory) as args:self.assertEqual([],args)
 def test_codec_view_cleanup_on_install_failure_and_rejects_symlink(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as temporary:
   directory=Path(temporary);repo=directory/'official.repo';repo.write_text('[codec]\nbaseurl=https://codecs.opensuse.org/openh264/openSUSE_Leap_16/\nenabled=1\ngpgcheck=1\n')
   with self.assertRaises(RuntimeError):
    with m.zypper_repositories(directory) as args:
     view=Path(args[1]);raise RuntimeError('stop')
   self.assertFalse(view.exists());self.assertIn('enabled=1',repo.read_text())
   (directory/'linked.repo').symlink_to(repo)
   with self.assertRaisesRegex(ValueError,'unsafe'):
    with m.zypper_repositories(directory):pass
 def test_invalid_repo_does_not_echo_private_configuration(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as temporary:
   directory=Path(temporary);(directory/'private.repo').write_text('secret-credential-value-without-section\n')
   with self.assertRaisesRegex(ValueError,'^invalid repository definition$'):
    with m.zypper_repositories(directory):pass
 def test_base_install_uses_own_zypper_no_apt_dnf_or_apk(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   base=Path(d);tools=base/'bin';tools.mkdir();capture=base/'calls'
   for name in ['zypper','rpm','update-ca-certificates','python3','thunar','xfce4-terminal','xwininfo','xprop','xfwm4','xfdesktop','xfce4-panel','xfsettingsd','xfconf-query','xfce4-session','dbus-run-session','dbus-monitor','fcitx5','xauth']:
    p=tools/name;p.write_text('#!/bin/sh\nprintf "%s\\n" "'+name+':$*" >> "$CAPTURE"\n');p.chmod(0o700)
   r=subprocess.run(['/bin/sh',str(ROOT/'x11/src/main/assets/x11-native-desktop.sh'),'install','opensuse'],env=dict(os.environ,PATH=str(tools)+':/usr/bin:/bin',CAPTURE=str(capture)),capture_output=True,text=True,timeout=5)
   self.assertEqual(0,r.returncode,r.stderr);text=capture.read_text();self.assertIn('zypper:--non-interactive install --no-recommends',text);self.assertIn('rpm:-q',text);self.assertIn('libxfce4ui-tools',text);self.assertIn('xfce4-pulseaudio-plugin',text)
   for word in ['apt-get','dnf:','apk:','--no-gpg-checks','--allow-unsigned-rpm']:self.assertNotIn(word,text)
