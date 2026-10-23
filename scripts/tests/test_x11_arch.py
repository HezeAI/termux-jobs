"""Arch X11 transactions and isolation; fixtures never modify installed rootfs."""
import contextlib,gzip,hashlib,importlib.util,io,json,os,subprocess,tarfile,tempfile,unittest
from pathlib import Path
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[2];ASSETS=ROOT/'x11/src/main/assets'
def module(file):
 s=importlib.util.spec_from_file_location(file,ASSETS/(file+'.py'));m=importlib.util.module_from_spec(s);s.loader.exec_module(m);return m
m=module('x11-native-programs');gpu=module('x11-gpu')
class ArchTests(unittest.TestCase):
 def test_pacman_installed_and_available_queries_do_not_use_another_manager(self):
  with patch.object(m,'output',return_value='mousepad\n../bad\n--option\nlibfoo-bar\n') as call:
   self.assertEqual({'mousepad','libfoo-bar'},m.installed('archlinux'));call.assert_called_once_with(['pacman','-Qq'])
  sample='Repository      : extra\nName            : mousepad\nVersion         : 0.6.4-1\nDescription     : Simple text editor\n\nRepository      : extra\nName            : ../bad\nDescription     : bad\n'
  with patch.object(m,'output',return_value=sample):self.assertEqual({'mousepad':'Simple text editor'},m.available('archlinux'))
 def test_install_resume_and_catalog_use_a_single_full_upgrade_transaction(self):
  for operation,identifier,resume in [('install','office','0'),('install','office','1'),('catalog-install','catalog-tools','0')]:
   pkgs=['libreoffice-fresh'] if operation=='install' else []
   with patch.dict(os.environ,{'JOBS_X11_NATIVE_TARGET':'archlinux','JOBS_X11_PROGRAM_RESUME':resume}),patch.object(m.sys,'argv',['helper',operation,identifier]),patch.object(m,'run') as run,patch.object(m,'installed',return_value=set(pkgs)),patch.object(m,'load') as load,patch.object(m,'catalog') as catalog,contextlib.redirect_stdout(io.StringIO()):
    load.return_value.state.return_value='off';m.main();run.assert_called_once_with(['pacman','-Su' if resume=='1' else '-Syu','--needed','--noconfirm','--',*pkgs])
    self.assertEqual(operation=='catalog-install',catalog.called)
 def test_dependency_refusal_does_not_force_or_remove_other_packages(self):
  with patch.object(m,'run',side_effect=subprocess.CalledProcessError(1,['pacman'])) as run:
   with self.assertRaises(subprocess.CalledProcessError):m.remove('archlinux','mousepad')
   run.assert_called_once_with(['pacman','-R','--noconfirm','--','mousepad'])
  with patch.object(m,'run') as run,patch.object(m,'installed',return_value=set()):
   m.remove('archlinux','mousepad');run.assert_called_once_with(['pacman','-R','--noconfirm','--','mousepad'])
  for name in ['pacman','archlinuxarm-keyring','xfdesktop','xfce4-panel','python','thunar']:
   with patch.object(m,'run') as run,self.assertRaises(ValueError):m.remove('archlinux',name)
   run.assert_not_called()
 def test_package_owned_launchers_use_files_without_decompressing_all_mtrees(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   root=Path(d);record=root/'mousepad-1-1';record.mkdir();(record/'desc').write_text('%NAME%\nmousepad\n\n');(record/'files').write_text('%FILES%\nusr/share/applications/mousepad.desktop\nusr/share/applications/../bad.desktop\netc/passwd\n');(record/'mtree').write_bytes(b'not-gzip')
   self.assertEqual({'/usr/share/applications/mousepad.desktop':'mousepad'},m.arch_manifests(root)[0])
   with self.assertRaises(OSError):m.arch_manifests(root,verify=True)
   (record/'mtree').unlink();(record/'files').unlink();(record/'files').symlink_to(record/'desc');self.assertEqual({},m.arch_manifests(root)[0])
 def test_clean_baseline_requests_complete_set_and_missing_panel_is_rejected(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   root=Path(d);bin=root/'bin';bin.mkdir();calls=root/'calls'
   for name in ['pacman','update-ca-trust','python3','thunar','xfce4-terminal','xwininfo','xprop','xfwm4','xfdesktop','xfce4-panel','xfsettingsd','xfconf-query','xfce4-session','dbus-run-session','dbus-monitor','fcitx5','xauth']:
    f=bin/name;f.write_text('#!/bin/sh\nprintf "%s\\n" "'+name+':$*" >> "$CAPTURE"\n');f.chmod(0o700)
   env=dict(os.environ,PATH=str(bin)+':/usr/bin:/bin',CAPTURE=str(calls))
   result=subprocess.run(['/bin/sh',str(ASSETS/'x11-native-desktop.sh'),'install','archlinux'],env=env,text=True,capture_output=True,timeout=5);self.assertEqual(0,result.returncode,result.stderr)
   text=calls.read_text();self.assertIn('pacman:-Syu --needed --noconfirm -- ',text)
   for package in ['xfdesktop','xfconf','dbus','fcitx5-gtk','ca-certificates','libpulse','llvm22-libs']:self.assertIn(package,text)
   # The mock package DB says installed but the required executable is absent.
   (bin/'xfce4-panel').unlink();result=subprocess.run(['/bin/sh',str(ASSETS/'x11-native-desktop.sh'),'check','archlinux'],env=env,capture_output=True,timeout=5);self.assertNotEqual(0,result.returncode)
 def test_all_arch_operations_keep_arch_rootfs_and_own_display(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   base=Path(d);prefix=base/'files/usr';bin=prefix/'bin';bin.mkdir(parents=True);control=prefix.parent/'workspace-x11-archlinux';control.mkdir();(prefix/'var/lib/proot-distro/containers/archlinux/rootfs').mkdir(parents=True);capture=base/'calls'; (bin/'python3').symlink_to('/usr/bin/python3')
   f=bin/'proot-distro';f.write_text('#!/usr/bin/python3\nimport os,sys,json\na=sys.argv[1:]\nassert a[0]=="login"\nassert a[a.index("--")-1]=="archlinux"\nwith open(os.environ["CAPTURE"],"a") as f:f.write(json.dumps(a)+"\\n")\n');f.chmod(0o700)
   f=bin/'pulseaudio';f.write_text('#!/bin/sh\nexit 0\n');f.chmod(0o700);modules=prefix/'lib/pulseaudio/modules';modules.mkdir(parents=True)
   for name in ['module-sles-sink.so','module-native-protocol-unix.so']:(modules/name).touch()
   env=dict(os.environ,PREFIX=str(prefix),PATH=str(bin)+':/usr/bin:/bin',CAPTURE=str(capture),JOBS_X11_FOCUS_TOKEN='a11a11a1-1111-4111-8111-111111111111')
   for op in ['check','install','run','repair-launchers','firefox-compat-on','firefox-compat-off']:
    r=subprocess.run(['bash',str(ASSETS/'x11-desktop.sh'),op,'archlinux',str(control)],env=env,capture_output=True,text=True,timeout=5);self.assertEqual(0,r.returncode,(op,r.stdout,r.stderr))
   for op,identifier in [('check',''),('install','editor'),('resume','editor'),('catalog',''),('catalog-install','catalog-tools'),('installed',''),('remove','pkg:mousepad'),('firefox-dns-on',''),('firefox-dns-off','')]:
    r=subprocess.run(['bash',str(ASSETS/'x11-programs.sh'),op,'archlinux',str(control),identifier],env=env,capture_output=True,text=True,timeout=5);self.assertEqual(0,r.returncode,(op,r.stdout,r.stderr))
   records=[json.loads(x) for x in capture.read_text().splitlines()];self.assertEqual(16,len(records));self.assertTrue(any('JOBS_X11_DISPLAY=105' in x[-1] for x in records));self.assertTrue(any('x11-native-desktop.sh install archlinux' in x[-1] for x in records))
 def tar(self,entries):
  data=io.BytesIO()
  with tarfile.open(fileobj=data,mode='w') as t:
   for name,content in entries:
    e=tarfile.TarInfo(name);e.size=len(content);t.addfile(e,io.BytesIO(content))
  return data.getvalue()
 def test_arch_bundle_extracts_only_reviewed_payloads_and_keeps_archive_boundary(self):
  mesa='mesa-1:26.3.0-1-aarch64.pkg.tar.xz';turnip='vulkan-freedreno-1:26.3.0-1-aarch64.pkg.tar.xz'
  bundle=self.tar([(mesa,self.tar([('usr/lib/mesa.so',b'mesa')])),(turnip,self.tar([('usr/lib/turnip.so',b'turnip')])),('unexpected',b'ignore')])
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   p=Path(d);gpu.unpack(bundle,'arch-bundle',p);self.assertTrue((p/'usr/lib/mesa.so').is_file());self.assertFalse((p/'unexpected').exists())
   for bad in [self.tar([(mesa,self.tar([('../escape',b'bad')])) ,(turnip,self.tar([]))]),self.tar([(mesa,self.tar([]))])]:
    with self.assertRaises((ValueError,tarfile.FilterError)):gpu.unpack(bad,'arch-bundle',p)
 def test_virgl_arch_checks_only_arch_guest(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   p=Path(d);prefix=p/'usr';bin=prefix/'bin';bin.mkdir(parents=True);angle=prefix/'opt/angle-android/vulkan';angle.mkdir(parents=True)
   for name in ['libEGL_angle.so','libGLESv2_angle.so']:(angle/name).touch()
   capture=p/'calls'
   for name,text in {'dpkg-query':'echo "install ok installed"','virgl_test_server_android':'echo "--socket-path --angle-gl --angle-vulkan --multi-clients"','apt-get':'exit 81','proot-distro':'printf "%s\\n" "$*" >> "$CAPTURE"'}.items():
    f=bin/name;f.write_text('#!/bin/sh\n'+text+'\n');f.chmod(0o700)
   f=p/'setup';f.write_text((ASSETS/'x11-virgl-setup.sh').read_text().replace('[[ $PREFIX == /data/data/com.termux/files/usr ]]','[[ $PREFIX == "$TEST_PREFIX" ]]'))
   r=subprocess.run(['bash',str(f),'install','archlinux',str(p)],env=dict(os.environ,PREFIX=str(prefix),TEST_PREFIX=str(prefix),CAPTURE=str(capture),PATH=str(bin)+':/usr/bin:/bin'),text=True,capture_output=True,timeout=4)
   self.assertEqual(0,r.returncode,r.stderr);self.assertIn('login archlinux --',capture.read_text());self.assertNotIn('ubuntu',capture.read_text());self.assertTrue(all(line.endswith('check archlinux none') for line in capture.read_text().splitlines()))
