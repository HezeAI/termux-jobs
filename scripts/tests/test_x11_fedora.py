"""Fedora routing and RPM/DNF operations, isolated from installed environments."""
import importlib.util,io,contextlib,os,subprocess,tempfile,unittest,hashlib
from pathlib import Path
from unittest.mock import patch,Mock
ROOT=Path(__file__).resolve().parents[2]
s=importlib.util.spec_from_file_location('fedora_native',ROOT/'x11/src/main/assets/x11-native-programs.py');m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
class FedoraTests(unittest.TestCase):
 def test_installed_and_catalog_use_rpm_and_cached_dnf_real_names(self):
  with patch.object(m,'output',return_value='Thunar\nmousepad\n--bad\n') as out:
   self.assertEqual({'Thunar','mousepad'},m.installed('fedora'));self.assertEqual('rpm',out.call_args.args[0][0])
  with patch.object(m,'output',return_value='Thunar\tFile manager\nlibfoo-devel\tDevelopment\n--bad\tUnsafe\n') as out:
   self.assertEqual({'Thunar':'File manager','libfoo-devel':'Development'},m.available('fedora'));self.assertIn('--cacheonly',out.call_args.args[0]);self.assertIn('%{name}\t%{summary}\n',out.call_args.args[0])
 def test_install_and_refresh_use_dnf_only_and_verify_complete_group(self):
  have=set();calls=[];codec=Mock();codec.ready.return_value=True;real_load=m.load
  def run(args):
   calls.append(args)
   if 'install' in args:have.update(m.GROUPS['fedora']['media'])
  with patch.dict(os.environ,{'JOBS_X11_NATIVE_TARGET':'fedora'}),patch.object(m.sys,'argv',['helper','install','media']),patch.object(m,'run',side_effect=run),patch.object(m,'installed',side_effect=lambda _:have),patch.object(m,'load',side_effect=lambda name:codec if name=='x11-openh264' else real_load(name)),contextlib.redirect_stdout(io.StringIO()) as log:
   m.main();self.assertIn('PROGRAM:media:installed',log.getvalue());self.assertTrue(all(c[0]=='dnf' for c in calls));self.assertIn('--setopt=retries=3',calls[1]);self.assertIn('gstreamer1-plugin-libav',calls[1]);self.assertNotIn('--allowerasing',str(calls));self.assertNotIn('nogpgcheck',str(calls));self.assertTrue(all('--disable-repo=fedora-cisco-openh264' in c for c in calls));self.assertIn('gstreamer1',calls[1]);self.assertIn('libstdc++',calls[1]);self.assertIn('libgcc',calls[1]);self.assertEqual('x11-gst-openh264.so',codec.install.call_args.args[3].name);codec.ready.assert_called_with(Path.home(),True)
 def test_incomplete_install_fails_and_remove_keeps_dependency_checks(self):
  with patch.dict(os.environ,{'JOBS_X11_NATIVE_TARGET':'fedora'}),patch.object(m.sys,'argv',['helper','install','editor']),patch.object(m,'run'),patch.object(m,'installed',return_value=set()),contextlib.redirect_stdout(io.StringIO()):
   with self.assertRaisesRegex(ValueError,'not fully installed'):m.main()
  with patch.object(m,'run') as run,patch.object(m,'installed',return_value=set()):m.remove('fedora','mousepad');run.assert_called_once_with(['rpm','-e','--','mousepad'])
  for pkg in ['Thunar','dnf5','rpm','glibc','xfce4-session','dbus-daemon','--force','name;id']:
   with patch.object(m,'run') as run:
    with self.assertRaises(ValueError):m.remove('fedora',pkg)
    run.assert_not_called()
 def test_repair_verifies_rpm_digest_and_preserves_edited_files(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   root=Path(d);apps=root/'apps';apps.mkdir();good=apps/'good.desktop';edited=apps/'edited.desktop'
   good.write_text('original');edited.write_text('edited');good.chmod(0o444);edited.chmod(0o444)
   helper=m.load('x11-launchers');helper.DIRS=(apps,);digest=hashlib.sha256(b'original').hexdigest();dump='\n'.join(str(p)+' 8 1 '+digest+' 0100444 root root 0 0 0 X' for p in [good,edited])
   with patch.object(m,'load',return_value=helper),patch.object(m,'output',return_value=dump),patch.object(m.Path,'home',return_value=root),contextlib.redirect_stdout(io.StringIO()):m.repair('fedora')
   self.assertEqual(0o644,good.stat().st_mode&0o777);self.assertEqual(0o444,edited.stat().st_mode&0o777)
 def test_base_install_uses_dnf_in_guest_not_apt_or_apk(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   base=Path(d);bin=base/'bin';bin.mkdir();calls=base/'calls'
   tools=['dnf','rpm','update-ca-trust','python3','thunar','xfce4-terminal','xwininfo','xprop','xfwm4','xfdesktop','xfce4-panel','xfsettingsd','xfconf-query','xfce4-session','dbus-run-session','dbus-monitor','fcitx5','xauth']
   for name in tools:
    p=bin/name;p.write_text('#!/bin/sh\nprintf "%s\\n" "'+name+':$*" >> "$CAPTURE"\n'+('case " $* " in *" xorg-x11-utils "*) echo "No match for argument: xorg-x11-utils" >&2; exit 1;;esac\n' if name=='dnf' else ''));p.chmod(0o700)
   env=dict(os.environ,PATH=str(bin)+':/usr/bin:/bin',CAPTURE=str(calls))
   r=subprocess.run(['/bin/sh',str(ROOT/'x11/src/main/assets/x11-native-desktop.sh'),'install','fedora'],env=env,capture_output=True,text=True,timeout=5)
   self.assertEqual(0,r.returncode,r.stderr);text=calls.read_text();self.assertIn('dnf:',text);self.assertIn('rpm:-q',text);self.assertIn('fcitx5-gtk3',text);self.assertIn('update-ca-trust:',text);self.assertNotIn('apt-get',text);self.assertNotIn('apk:',text);self.assertIn('xprop xwininfo',text);self.assertNotIn('xorg-x11-utils',text);self.assertNotIn('makecache --refresh',text);self.assertIn('--setopt=max_parallel_downloads=6',text)
   commands=[line for line in text.splitlines() if line.startswith('dnf:')]
   self.assertEqual(2,len(commands))
   for command in commands:
    self.assertIn('--disable-repo=fedora-cisco-openh264',command)
    for unsafe in ['--nogpgcheck','gpgcheck=0','--skip-broken','--allowerasing']:self.assertNotIn(unsafe,command)
 def test_failed_fedora_transaction_does_not_claim_installed(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   base=Path(d);calls=base/'calls';tools=base/'bin';tools.mkdir()
   for name,body in [('dnf','case " $* " in *" install "*) exit 23;;esac'),('rpm',''),('update-ca-trust','')]:
    p=tools/name;p.write_text('#!/bin/sh\nprintf "%s\\n" "'+name+':$*" >> "$CAPTURE"\n'+body+'\n');p.chmod(0o700)
   r=subprocess.run(['/bin/sh',str(ROOT/'x11/src/main/assets/x11-native-desktop.sh'),'install','fedora'],env=dict(os.environ,PATH=str(tools)+':/usr/bin:/bin',CAPTURE=str(calls)),capture_output=True,text=True,timeout=5)
   self.assertEqual(23,r.returncode);self.assertNotIn('rpm:',calls.read_text());self.assertNotIn('update-ca-trust:',calls.read_text())

 def test_installed_parole_without_private_codec_is_not_reported_complete(self):
  codec=Mock();codec.ready.return_value=False;real_load=m.load
  with patch.dict(os.environ,{'JOBS_X11_NATIVE_TARGET':'fedora'}),patch.object(m.sys,'argv',['helper','check']),patch.object(m,'installed',return_value=set(m.GROUPS['fedora']['media'])),patch.object(m,'load',side_effect=lambda name:codec if name=='x11-openh264' else real_load(name)),patch.object(m,'run') as run,contextlib.redirect_stdout(io.StringIO()) as log:
   m.main();self.assertIn('PROGRAM:media:missing',log.getvalue());codec.ready.assert_called_with(Path.home(),True);codec.install.assert_not_called();run.assert_not_called()
