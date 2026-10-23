"""Every desktop baseline: execute fixtures, never install or alter environments."""
import os,re,subprocess,tempfile,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];ASSETS=ROOT/'x11/src/main/assets'
COMMANDS=['xfdesktop','xfce4-panel','xfsettingsd','xfconf-query','python3','thunar','xfce4-terminal','xwininfo','xprop','xfwm4','xfce4-session','dbus-run-session','dbus-monitor','fcitx5','xauth','exo-open']
class CleanMatrix(unittest.TestCase):
 def fixture(self,base):
  bin=base/'bin';bin.mkdir();capture=base/'capture'
  scripts={n:'exit 0' for n in COMMANDS}
  scripts.update({'apk':'exit 0','rpm':'exit 0','pacman':'exit 0','xbps-query':'printf installed','xbps-install':'exit 0','apt-get':'exit 0','dnf':'exit 0','zypper':'exit 0','dpkg-query':'printf "hold ok installed"','update-ca-certificates':'exit 0','update-ca-trust':'exit 0'})
  for n,body in scripts.items():
   f=bin/n;f.write_text('#!/bin/sh\nprintf "%s\\n" "'+n+':$*" >> "$CAPTURE"\n'+body+'\n');f.chmod(0o700)
  return bin,dict(os.environ,PATH=str(bin),PREFIX=str(base),CAPTURE=str(capture)),capture
 def test_every_native_install_requests_complete_set_and_rejects_missing_desktop_binary(self):
  expected={'termux':'apt-get:','alpine':'apk:','fedora':'dnf:','opensuse':'zypper:','void':'xbps-install:','archlinux':'pacman:'}
  for target,manager in expected.items():
   with self.subTest(target=target),tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
    base=Path(d);bin,env,capture=self.fixture(base)
    r=subprocess.run(['/bin/sh',str(ASSETS/'x11-native-desktop.sh'),'install',target],env=env,text=True,capture_output=True,timeout=5);self.assertEqual(0,r.returncode,r.stderr)
    text=capture.read_text();self.assertIn(manager,text)
    # All required package names are passed even if package DB reports installed.
    source=re.split(r'(?:in |\n )'+target+r'\)\n',(ASSETS/'x11-native-desktop.sh').read_text(),maxsplit=1)[1].split(';;',1)[0];packages=re.search("pkgs='([^']+)'",source)[1]
    self.assertIn(packages,text)
    for binary in ['xfdesktop','xfce4-panel','xfconf-query']+(['exo-open'] if target=='termux' else []):
     f=bin/binary;content=f.read_text();f.unlink()
     r=subprocess.run(['/bin/sh',str(ASSETS/'x11-native-desktop.sh'),'check',target],env=env,text=True,capture_output=True,timeout=5);self.assertNotEqual(0,r.returncode,binary)
     f.write_text(content);f.chmod(0o700)
 def test_apt_guests_reject_absent_desktop_runtime_audio_and_ca(self):
  source=(ASSETS/'x11-desktop.sh').read_text();components=source.split("components='",1)[1].split("\npackages=",1)[0][:-1]
  for target in ['ubuntu','jobs-x11-ubuntu','debian']:
   with self.subTest(target=target),tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
    base=Path(d);bin,env,_=self.fixture(base);cert=base/'ca.crt';cert.write_text('fixture');components_=components.replace('/etc/ssl/certs/ca-certificates.crt',str(cert))
    r=subprocess.run(['/bin/sh','-c',components_],env=env,text=True,capture_output=True,timeout=5);self.assertEqual(0,r.returncode,r.stderr)
    for binary in ['xfdesktop','xfce4-panel','xfconf-query','dbus-monitor']:
     f=bin/binary;text=f.read_text();f.unlink();r=subprocess.run(['/bin/sh','-c',components_],env=env,capture_output=True,timeout=5);self.assertNotEqual(0,r.returncode);f.write_text(text);f.chmod(0o700)
    cert.unlink();self.assertNotEqual(0,subprocess.run(['/bin/sh','-c',components_],env=env,capture_output=True,timeout=5).returncode)
    cert.write_text('fixture');f=bin/'dpkg-query';f.write_text('#!/bin/sh\ncase "$*" in *libpulse0*)exit 1;;esac\nprintf "install ok installed"\n');f.chmod(0o700)
    self.assertNotEqual(0,subprocess.run(['/bin/sh','-c',components_],env=env,capture_output=True,timeout=5).returncode)
 def test_every_guest_install_run_and_program_operation_keeps_selected_rootfs(self):
  import json
  for target in ['ubuntu','jobs-x11-ubuntu','debian','alpine','fedora','opensuse','void','archlinux']:
   with self.subTest(target=target),tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
    base=Path(d);prefix=base/'files/usr';bin=prefix/'bin';bin.mkdir(parents=True);control=prefix.parent/('workspace-x11-'+target);control.mkdir();(prefix/('var/lib/proot-distro/containers/'+target+'/rootfs')).mkdir(parents=True);capture=base/'capture'; (bin/'python3').symlink_to('/usr/bin/python3')
    fake=bin/'proot-distro';fake.write_text('#!/usr/bin/python3\nimport os,sys,json\nwith open(os.environ["CAPTURE"],"a") as f:f.write(json.dumps(sys.argv[1:])+"\\n")\n');fake.chmod(0o700)
    fake=bin/'pulseaudio';fake.write_text('#!/bin/sh\nexit 0\n');fake.chmod(0o700);modules=prefix/'lib/pulseaudio/modules';modules.mkdir(parents=True)
    for name in ['module-sles-sink.so','module-native-protocol-unix.so']:(modules/name).touch()
    env=dict(os.environ,PREFIX=str(prefix),PATH=str(bin)+':/usr/bin:/bin',CAPTURE=str(capture),JOBS_X11_FOCUS_TOKEN='a11a11a1-1111-4111-8111-111111111111')
    for op in ['check','install','run']:
     r=subprocess.run(['/bin/bash',str(ASSETS/'x11-desktop.sh'),op,target,str(control)],env=env,text=True,capture_output=True,timeout=5);self.assertEqual(0,r.returncode,(op,r.stdout,r.stderr))
    for op,identifier in [('check',''),('install','editor'),('installed',''),('remove','pkg:mousepad')]:
     r=subprocess.run(['/bin/bash',str(ASSETS/'x11-programs.sh'),op,target,str(control),identifier],env=env,text=True,capture_output=True,timeout=5);self.assertEqual(0,r.returncode,(op,r.stdout,r.stderr))
    calls=[json.loads(x) for x in capture.read_text().splitlines()];self.assertGreaterEqual(len(calls),8)
    for call in calls:
     self.assertEqual(target,call[call.index('--')-1]);self.assertEqual('run' if target=='void' else 'login',call[0]);self.assertIn(str(control)+':/tmp/termux-jobs-x11',call)
