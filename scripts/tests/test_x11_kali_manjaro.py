"""New desktops retain their own rootfs, display, program manager and bus."""
import contextlib,io,os,subprocess,tempfile,unittest
from pathlib import Path
from unittest.mock import patch
from scripts.tests.test_x11_arch import module
ROOT=Path(__file__).resolve().parents[2];A=ROOT/'x11/src/main/assets';m=module('x11-native-programs');gpu=module('x11-gpu');bus=module('x11-session-bus');live=module('x11-scale-live')
class NewDesktops(unittest.TestCase):
 def test_manjaros_own_pacman_program_transactions_and_protected_keyrings(self):
  with patch.object(m,'output',return_value='mousepad\n') as call:self.assertEqual({'mousepad'},m.installed('manjaro'));call.assert_called_once_with(['pacman','-Qq'])
  self.assertEqual(['xarchiver','unzip','zip','p7zip'],m.packages('manjaro','archives'))
  self.assertEqual(['libreoffice-fresh','sed'],m.packages('manjaro','office'))
  self.assertEqual(['libreoffice-fresh','sed'],m.packages('manjaro','pkg:libreoffice-fresh'))
  for resume in ['0','1']:
   with patch.dict(os.environ,{'JOBS_X11_NATIVE_TARGET':'manjaro','JOBS_X11_PROGRAM_RESUME':resume}),patch.object(m.sys,'argv',['helper','install','office']),patch.object(m,'run') as run,patch.object(m,'installed',return_value={'libreoffice-fresh','sed'}),patch.object(m,'load') as load,contextlib.redirect_stdout(io.StringIO()):
    load.return_value.state.return_value='off';m.main();run.assert_called_once_with(['pacman','-Su' if resume=='1' else '-Syu','--needed','--noconfirm','--','libreoffice-fresh','sed'])
  for name in ['manjaro-arm-keyring','manjaro-system','archlinuxarm-keyring','python','xfdesktop']:
   with self.assertRaises(ValueError),patch.object(m,'run') as run:m.remove('manjaro',name)
   run.assert_not_called()
 def test_kali_and_manjaros_mesa_keep_separate_compatible_packages(self):
  self.assertEqual('kali-mesa26.2.5',gpu.SPECS['kali']['directory']);self.assertEqual(gpu.SPECS['debian']['packages'],gpu.SPECS['kali']['packages']);self.assertEqual('manjaro-mesa26.2.5-glibc235',gpu.SPECS['manjaro']['directory']);self.assertNotEqual(gpu.SPECS['archlinux']['packages'],gpu.SPECS['manjaro']['packages'])
 def test_new_buses_and_live_scale_identity_cannot_use_other_displays(self):
  for target,display in [('kali',106),('manjaro',107)]:
   self.assertIn(target,bus.TARGETS);self.assertEqual(display,live.TARGETS[target])
 def test_new_cpu_sessions_and_optional_style_keep_all_generic_controls(self):
  for target in ['kali','manjaro']:
   with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
    p=Path(d);prefix=p/'files/usr';bin=prefix/'bin';bin.mkdir(parents=True);control=prefix.parent/('workspace-x11-'+target);control.mkdir();(prefix/('var/lib/proot-distro/containers/'+target+'/rootfs')).mkdir(parents=True);capture=p/'calls';(bin/'python3').symlink_to('/usr/bin/python3')
    f=bin/'proot-distro';f.write_text('#!/bin/sh\nprintf "%s\\n" "$*" >> "$CAPTURE"\n');f.chmod(0o700)
    f=bin/'pulseaudio';f.write_text('#!/bin/sh\nexit 0\n');f.chmod(0o700);mods=prefix/'lib/pulseaudio/modules';mods.mkdir(parents=True)
    for n in ['module-sles-sink.so','module-native-protocol-unix.so']:(mods/n).touch()
    env=dict(os.environ,PREFIX=str(prefix),PATH=str(bin)+':/usr/bin:/bin',CAPTURE=str(capture),JOBS_X11_FOCUS_TOKEN='a11a11a1-1111-4111-8111-111111111111',JOBS_X11_STYLE='1',JOBS_X11_SCALE='75')
    for op in ['scale','check','install','run']:
     r=subprocess.run(['bash',str(A/'x11-desktop.sh'),op,target,str(control)],env=env,capture_output=True,text=True,timeout=5);self.assertEqual(0,r.returncode,(op,r.stdout,r.stderr))
    text=capture.read_text();self.assertIn('JOBS_X11_SCALE=75',text);self.assertIn('JOBS_X11_TARGET='+target,text);self.assertIn('JOBS_X11_DISPLAY='+str(live.TARGETS[target]),text);self.assertNotIn(' login ubuntu ',text)
