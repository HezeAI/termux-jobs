"""Program catalog/guest routing with fake APT, never the installed environment."""
import os,subprocess,tempfile,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
SCRIPT=ROOT/'x11/src/main/assets/x11-programs.sh'
class ProgramTests(unittest.TestCase):
 def fixture(self,d):
  base=Path(d)/'files';prefix=base/'usr';bin=prefix/'bin';bin.mkdir(parents=True);control=base/'workspace-x11';control.mkdir()
  (prefix/'var/lib/proot-distro/containers/jobs-x11-ubuntu/rootfs').mkdir(parents=True)
  scripts={
   'proot-distro':'#!/bin/bash\nprintf "%s\\n" "$@" >> "$ROUTE"\nwhile [[ $1 != -- ]];do shift;done\nshift\nexport JOBS_FIXTURE_PATH="$PATH"\n/usr/bin/sed \'s#export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin;#export PATH="$JOBS_FIXTURE_PATH";#\' | exec "$@"\n',
   'dpkg-query':'#!/bin/bash\npkg=${!#}\nwhile read -r known;do [[ $known == "$pkg" ]] && { printf "install ok installed";exit 0; };done < "$INSTALLED"\nexit 1\n',
   'apt-get':'#!/bin/bash\nprintf "%s\\n" "$@" >> "$APT_CALLS"\n[[ ${FAIL_APT:-0} == 0 ]] || exit 42\nfor p in "$@";do case $p in thunar|xfce4-terminal|mousepad|xarchiver|unzip|zip|7zip|ristretto|tumbler|fonts-dejavu-core|fonts-liberation|adwaita-icon-theme|appstream|python3-gi|gir1.2-appstream-1.0|python3-apt|netsurf-gtk|libreoffice-writer|libreoffice-calc|libreoffice-impress|libreoffice-gtk3|evince|parole|gstreamer1.0-libav|gstreamer1.0-plugins-good|gstreamer1.0-plugins-bad|gstreamer1.0-pulseaudio|gstreamer1.0-x) echo "$p" >> "$INSTALLED";;esac;done\necho "dlstatus:1:50:fetching" >&3\necho "pmstatus:p:100:configured" >&3\n'}
  scripts['firefox']='#!/bin/sh\nexit 0\n'
  scripts['python3']='#!/bin/bash\ncase "$1" in /tmp/termux-jobs-x11/x11-firefox.py) [[ $2 == --check ]] && exit 1;exit 77;; *) exec /usr/bin/python3 "$@";;esac\n'
  scripts['dpkg']='#!/bin/bash\nprintf "%s\\n" "$@" >> "$REMOVE_CALLS"\n[[ ${FAIL_REMOVE:-0} == 0 ]] || exit 12\npkg=${!#}\nawk -v p="$pkg" \'$0 != p\' "$INSTALLED" > "$INSTALLED.next"\nmv "$INSTALLED.next" "$INSTALLED"\n'
  for name,text in scripts.items():f=bin/name;f.write_text(text);f.chmod(0o755)
  installed=base/'installed';installed.write_text('thunar\nxfce4-terminal\n')
  return control,{'PATH':str(bin)+':'+os.environ['PATH'],'PREFIX':str(prefix),'ROUTE':str(base/'route'),'INSTALLED':str(installed),'APT_CALLS':str(base/'apt'),'REMOVE_CALLS':str(base/'remove')}
 def run_program(self,env,control,op='check',id='',target='jobs-x11-ubuntu'):
  return subprocess.run(['bash',str(SCRIPT),op,target,str(control),id],env={**os.environ,**env},capture_output=True,text=True,timeout=8)
 def test_compatibility_program_job_does_not_run_apt_and_supports_ordinary_ubuntu(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   control,env=self.fixture(d);python=Path(env['PREFIX'])/'bin/python3';python.write_text('#!/bin/bash\ncase "$1" in /tmp/termux-jobs-x11/x11-maintenance.py) printf "%s %s\\n" "$JOBS_X11_MAINTENANCE" "$2" > "$COMPAT_CALL";exit 0;; /tmp/termux-jobs-x11/x11-firefox.py) exit 1;; *) exec /usr/bin/python3 "$@";;esac\n');env['COMPAT_CALL']=str(Path(d)/'compat-called')
   p=self.run_program(env,control,'firefox-compat-on','browser');self.assertEqual(0,p.returncode,p.stderr);self.assertEqual('1 firefox-compat-on\n',Path(env['COMPAT_CALL']).read_text());self.assertIn('READY:programs',p.stdout);self.assertFalse(Path(env['APT_CALLS']).exists())
   Path(env['COMPAT_CALL']).unlink();(Path(env['PREFIX'])/'var/lib/proot-distro/containers/ubuntu/rootfs').mkdir(parents=True)
   p=self.run_program(env,control,'firefox-compat-on','browser','ubuntu');self.assertEqual(0,p.returncode,p.stderr);self.assertEqual('1 firefox-compat-on\n',Path(env['COMPAT_CALL']).read_text());self.assertFalse(Path(env['APT_CALLS']).exists())
 def test_debian_program_install_and_compatibility_keep_the_selected_environment(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   control,env=self.fixture(d);(Path(env['PREFIX'])/'var/lib/proot-distro/containers/debian/rootfs').mkdir(parents=True)
   p=self.run_program(env,control,'install','images','debian');self.assertEqual(0,p.returncode,p.stderr);self.assertIn('PROGRAM:images:installed',p.stdout)
   args=Path(env['ROUTE']).read_text().splitlines();self.assertIn('debian',args);self.assertNotIn('ubuntu',args);self.assertNotIn('jobs-x11-ubuntu',args)
   Path(env['APT_CALLS']).unlink();python=Path(env['PREFIX'])/'bin/python3';python.write_text('#!/bin/bash\ncase "$1" in /tmp/termux-jobs-x11/x11-maintenance.py) printf "%s %s\\n" "$JOBS_X11_MAINTENANCE" "$2" > "$COMPAT_CALL";exit 0;; *) exit 1;;esac\n');env['COMPAT_CALL']=str(Path(d)/'compat')
   p=self.run_program(env,control,'firefox-compat-on','browser','debian');self.assertEqual(0,p.returncode,p.stderr);self.assertEqual('1 firefox-compat-on\n',Path(env['COMPAT_CALL']).read_text());self.assertFalse(Path(env['APT_CALLS']).exists())
 def test_apt_firefox_dns_routes_only_to_selected_root_without_installing(self):
  for target in ['ubuntu','debian']:
   with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
    control,env=self.fixture(d);(Path(env['PREFIX'])/('var/lib/proot-distro/containers/'+target+'/rootfs')).mkdir(parents=True,exist_ok=True)
    python=Path(env['PREFIX'])/'bin/python3';python.write_text('#!/bin/bash\ncase "$1" in /tmp/termux-jobs-x11/x11-firefox-dns.py) printf "%s %s\\n" "$JOBS_X11_MAINTENANCE" "$2" >> "$DNS_CALL";echo FIREFOX_DNS:on;exit 0;; /tmp/termux-jobs-x11/x11-firefox.py) exit 1;; *) exec /usr/bin/python3 "$@";;esac\n');env['DNS_CALL']=str(Path(d)/'dns-calls')
    for op in ['firefox-dns-on','firefox-dns-off']:
     result=self.run_program(env,control,op,'browser',target);self.assertEqual(0,result.returncode,result.stderr);self.assertIn('FIREFOX_DNS:on',result.stdout)
    calls=Path(env['DNS_CALL']).read_text();self.assertIn('1 firefox-dns-on',calls);self.assertIn('1 firefox-dns-off',calls);self.assertIn('1 state',calls);self.assertFalse(Path(env['APT_CALLS']).exists());self.assertNotIn('ubuntu-ai',Path(env['ROUTE']).read_text())
 def test_check_routes_only_to_selected_environment_and_never_installs(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   control,env=self.fixture(d);p=self.run_program(env,control);self.assertEqual(0,p.returncode,p.stderr);self.assertIn('PROGRAM:files:installed',p.stdout);self.assertIn('PROGRAM:editor:missing',p.stdout);self.assertIn('READY:programs',p.stdout)
   self.assertFalse(Path(env['APT_CALLS']).exists());self.assertIn('jobs-x11-ubuntu',Path(env['ROUTE']).read_text().splitlines());self.assertNotIn('ubuntu',Path(env['ROUTE']).read_text().splitlines())
 def test_gpu_test_payload_install_skips_apt_only_when_complete_runtime_is_configured(self):
  for missing in (False,True):
   with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
    control,env=self.fixture(d);base=Path(d);bin=Path(env['PREFIX'])/'bin'
    Path(env['INSTALLED']).write_text('python3\nlibx11-6\nlibgl1\n'+('' if missing else 'libglx-mesa0\n'))
    python=bin/'python3';python.write_text('#!/bin/bash\n[[ $1 == -B ]] && shift\ncase "$1" in /tmp/termux-jobs-x11/x11-gpu-test-setup.py) [[ $2 != prepare ]] || printf "%s\\n" "$3" > "$SETUP_CALL";exit 0;; /tmp/termux-jobs-x11/x11-firefox.py) exit 1;; *) exec /usr/bin/python3 "$@";;esac\n')
    env.update(SETUP_CALL=str(base/'setup'),FAIL_APT='1')
    result=self.run_program(env,control,'install','gpu-test')
    if missing:
     self.assertNotEqual(0,result.returncode);self.assertTrue(Path(env['APT_CALLS']).exists());self.assertFalse((base/'setup').exists())
    else:
     self.assertEqual(0,result.returncode,result.stderr);self.assertFalse(Path(env['APT_CALLS']).exists());self.assertEqual('jobs-x11-ubuntu\n',(base/'setup').read_text());self.assertIn('PROGRAM:gpu-test:installed',result.stdout)
 def test_gpu_group_installs_apt_names_in_selected_guest_then_creates_shortcuts(self):
  for target in ['ubuntu','debian','jobs-x11-ubuntu']:
   with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
    control,env=self.fixture(d);bin=Path(env['PREFIX'])/'bin';(Path(env['PREFIX'])/('var/lib/proot-distro/containers/'+target+'/rootfs')).mkdir(parents=True,exist_ok=True)
    apt=bin/'apt-get';apt.write_text(apt.read_text().replace('case $p in thunar|','case $p in glmark2-x11|mesa-utils|thunar|'))
    for name in ['glxinfo','glmark2']:
     file=bin/name;file.write_text('#!/bin/sh\nexit 0\n');file.chmod(0o755)
    python=bin/'python3';python.write_text('#!/bin/bash\n[[ $1 == -B ]] && shift\ncase "$1" in /tmp/termux-jobs-x11/x11-gpu-tools.py) printf "%s\\n" "$2 $3" > "$GPU_CALL";exit 0;; /tmp/termux-jobs-x11/x11-firefox.py) exit 1;; *) exec /usr/bin/python3 "$@";;esac\n');env['GPU_CALL']=str(Path(d)/'gpu-called')
    result=self.run_program(env,control,'install','gpu-tools',target);self.assertEqual(0,result.returncode,result.stderr);self.assertIn('PROGRAM:gpu-tools:installed',result.stdout);self.assertEqual('prepare '+target+'\n',Path(env['GPU_CALL']).read_text());calls=Path(env['APT_CALLS']).read_text();self.assertIn('glmark2-x11',calls);self.assertIn('mesa-utils',calls);self.assertIn(target,Path(env['ROUTE']).read_text().splitlines());self.assertNotIn('ubuntu-ai',Path(env['ROUTE']).read_text())
    (bin/'glmark2').unlink();(bin/'bash').symlink_to('/bin/bash');env['PATH']=str(bin);result=self.run_program(env,control,'check','',target);self.assertIn('PROGRAM:gpu-tools:missing',result.stdout)
 def test_install_only_allowlisted_group_then_verify_all_packages(self):
  for id,expected in [('editor',['mousepad']),('archives',['xarchiver','unzip','zip','7zip']),('office',['libreoffice-writer','libreoffice-calc','libreoffice-impress','libreoffice-gtk3']),('images',['ristretto','tumbler']),('pdf',['evince']),('media',['parole','gstreamer1.0-libav','gstreamer1.0-plugins-good','gstreamer1.0-plugins-bad','gstreamer1.0-pulseaudio','gstreamer1.0-x'])]:
   with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
    control,env=self.fixture(d);p=self.run_program(env,control,'install',id);self.assertEqual(0,p.returncode,p.stderr);self.assertIn('PROGRAM:'+id+':installed',p.stdout);calls=Path(env['APT_CALLS']).read_text().splitlines();self.assertIn('--no-remove',calls);self.assertIn('APT::Status-Fd=3',calls)
    self.assertEqual(['thunar','xfce4-terminal']+expected,Path(env['INSTALLED']).read_text().splitlines())
 def test_ristretto_without_tumbler_needs_the_missing_component(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   control,env=self.fixture(d);Path(env['INSTALLED']).write_text('ristretto\n');p=self.run_program(env,control);self.assertEqual(0,p.returncode,p.stderr);self.assertIn('PROGRAM:images:missing',p.stdout)
   p=self.run_program(env,control,'install','images');self.assertEqual(0,p.returncode,p.stderr);self.assertIn('PROGRAM:images:installed',p.stdout)
 def test_player_without_decoders_is_not_a_complete_recommended_install(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   control,env=self.fixture(d);Path(env['INSTALLED']).write_text('parole\n')
   p=self.run_program(env,control);self.assertEqual(0,p.returncode,p.stderr);self.assertIn('PROGRAM:media:missing',p.stdout)
   p=self.run_program(env,control,'install','media');self.assertEqual(0,p.returncode,p.stderr);self.assertIn('PROGRAM:media:installed',p.stdout)
 def test_legacy_archiver_provider_is_recognized_without_installing(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   control,env=self.fixture(d);Path(env['INSTALLED']).write_text('xarchiver\nunzip\nzip\np7zip-full\n')
   p=self.run_program(env,control);self.assertEqual(0,p.returncode,p.stderr);self.assertIn('PROGRAM:archives:installed',p.stdout);self.assertFalse(Path(env['APT_CALLS']).exists())
 def test_catalog_missing_tools_is_read_only_and_requires_explicit_setup(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   control,env=self.fixture(d);p=self.run_program(env,control,'catalog');self.assertEqual(3,p.returncode,p.stderr);self.assertIn('ERROR:CATALOG_TOOLS_REQUIRED',p.stdout);self.assertFalse(Path(env['APT_CALLS']).exists())
 def test_failed_apt_never_emits_success_or_installed_results(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   control,env=self.fixture(d);env['FAIL_APT']='1';p=self.run_program(env,control,'install','editor');self.assertNotEqual(0,p.returncode);self.assertNotIn('READY:programs',p.stdout);self.assertNotIn('PROGRAM:',p.stdout)
 def test_injection_wrong_target_and_wrong_control_do_not_call_guest(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   control,env=self.fixture(d)
   for op,id,target in [('install','editor; true','jobs-x11-ubuntu'),('install','../pdf','jobs-x11-ubuntu'),('install','pkg:--bad','jobs-x11-ubuntu'),('install','pkg:mousepad;true','jobs-x11-ubuntu'),('install','','jobs-x11-ubuntu'),('check','','ubuntu-ai')]:
    p=self.run_program(env,control,op,id,target);self.assertEqual(2,p.returncode)
   p=self.run_program(env,Path(d),'check');self.assertEqual(2,p.returncode);self.assertFalse(Path(env['ROUTE']).exists())

 def test_remove_only_explicit_program_preserves_helpers_and_refuses_protected_components(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   control,env=self.fixture(d);Path(env['INSTALLED']).write_text('thunar\nxfce4-terminal\nristretto\ntumbler\n')
   p=self.run_program(env,control,'remove','pkg:ristretto');self.assertEqual(0,p.returncode,p.stderr);self.assertEqual(['--remove','--','ristretto'],Path(env['REMOVE_CALLS']).read_text().splitlines());self.assertIn('tumbler',Path(env['INSTALLED']).read_text());self.assertNotIn('ristretto',Path(env['INSTALLED']).read_text());self.assertIn('READY:programs',p.stdout);self.assertFalse(Path(env['APT_CALLS']).exists())
   Path(env['REMOVE_CALLS']).unlink()
   for pkg in ['thunar','xfce4-panel','fcitx5','python3','fonts-dejavu-core','libpulse0','gstreamer1.0-pulseaudio']:
    p=self.run_program(env,control,'remove','pkg:'+pkg);self.assertEqual(2,p.returncode);self.assertIn('ERROR:PROTECTED_DESKTOP_COMPONENT',p.stdout)
   self.assertFalse(Path(env['REMOVE_CALLS']).exists())
 def test_dependency_refusal_never_reports_success_and_does_not_force_or_autoremove(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   control,env=self.fixture(d);env['FAIL_REMOVE']='1';p=self.run_program(env,control,'remove','pkg:mousepad');self.assertNotEqual(0,p.returncode);self.assertNotIn('READY:programs',p.stdout);self.assertNotIn('force',Path(env['REMOVE_CALLS']).read_text());self.assertFalse(Path(env['APT_CALLS']).exists())

 def test_prepare_catalog_installs_missing_tools_once_then_only_refreshes(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   control,env=self.fixture(d)
   (Path(env['PREFIX'])/'bin/python3').write_text('#!/bin/bash\ncase "$1" in /tmp/termux-jobs-x11/x11-catalog.py) echo READY:catalog;; *) exit 1;;esac\n')
   # Virtual guest interpreter: production intentionally pins /usr/bin/python3.
   proot=Path(env['PREFIX'])/'bin/proot-distro'
   proot.write_text(proot.read_text().replace('exec "$@"',"sed 's#exec /usr/bin/python3 #exec python3 #' | \"$@\""))
   for first in [True,False]:
    calls=Path(env['APT_CALLS'])
    if calls.exists():calls.unlink()
    result=self.run_program(env,control,'catalog-install','catalog-tools')
    self.assertEqual(0,result.returncode,result.stderr);self.assertIn('READY:catalog',result.stdout)
    args=calls.read_text().splitlines();self.assertEqual(1 if first else 0,args.count('install'));self.assertEqual(2 if first else 1,args.count('update'))
    for pkg in ['appstream','python3-gi','gir1.2-appstream-1.0','python3-apt']:self.assertIn(pkg,Path(env['INSTALLED']).read_text().splitlines())
 def test_failed_catalog_preparation_does_not_emit_a_ready_catalog(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   control,env=self.fixture(d);env['FAIL_APT']='1';result=self.run_program(env,control,'catalog-install','catalog-tools');self.assertNotEqual(0,result.returncode);self.assertNotIn('READY:catalog',result.stdout);self.assertNotIn('install',Path(env['APT_CALLS']).read_text().splitlines())

 def test_guest_readiness_accepts_hold_or_failed_remove_but_not_partial_or_broken(self):
  for status,ready in [('deinstall ok installed',True),('hold ok installed',True),('install reinstreq installed',False),('install ok unpacked',False),('deinstall ok config-files',False)]:
   with self.subTest(status=status),tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
    control,env=self.fixture(d)
    (Path(env['PREFIX'])/'bin/dpkg-query').write_text('#!/bin/sh\nprintf "%s" "'+status+'"\n')
    proc=self.run_program(env,control)
    self.assertEqual(0,proc.returncode,proc.stderr)
    self.assertIn('PROGRAM:pdf:'+('installed' if ready else 'missing'),proc.stdout)

 def test_explicit_apt_resume_skips_refresh_but_keeps_install_and_configured_checks(self):
  for target in ['ubuntu','debian','jobs-x11-ubuntu']:
   with self.subTest(target=target),tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
    control,env=self.fixture(d);(Path(env['PREFIX'])/('var/lib/proot-distro/containers/'+target+'/rootfs')).mkdir(parents=True,exist_ok=True)
    result=self.run_program(env,control,'resume','editor',target);self.assertEqual(0,result.returncode,result.stderr);calls=Path(env['APT_CALLS']).read_text().splitlines();self.assertNotIn('update',calls);self.assertIn('install',calls);self.assertIn('--no-remove',calls);self.assertIn('APT::Keep-Downloaded-Packages=true',calls);self.assertIn('JOBS_PROGRAM_INDEX_READY',result.stdout);self.assertIn('PROGRAM:editor:installed',result.stdout)
