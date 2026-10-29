"""Real offline APT transaction of script-free fixture packages into an empty DB.
Exercises the new official-base desktop installer without PRoot or host installs.
Executable placeholders establish installer/status checks, not GUI functionality.
"""
import hashlib,os,re,subprocess,tempfile,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];SCRIPT=ROOT/'x11/src/main/assets/x11-native-desktop.sh'
@unittest.skipUnless(all(Path('/usr/bin/'+n).exists() for n in ('apt-get','dpkg','dpkg-deb','dpkg-scanpackages')),'native fixture tools required')
class NativeOfficialDesktop(unittest.TestCase):
 def test_empty_database_installs_complete_declared_set_and_repeated_run_is_valid(self):
  before=hashlib.sha256(Path('/var/lib/dpkg/status').read_bytes()).hexdigest()
  names=re.search(r"case \$target in ubuntu\|debian\|kali\)\n pkgs='([^']+)'",SCRIPT.read_text())[1].split()
  self.assertTrue({'xfdesktop4','xfce4-session','fonts-dejavu-core','dbus-x11','fcitx5','xauth'}.issubset(names))
  programs={'xfdesktop4':['xfdesktop'],'xfce4-panel':['xfce4-panel'],'xfce4-session':['xfce4-session'],'xfce4-settings':['xfsettingsd'],'xfconf':['xfconf-query'],'thunar':['thunar'],'xfwm4':['xfwm4'],'xfce4-terminal':['xfce4-terminal'],'x11-utils':['xprop','xwininfo'],'python3':['python3'],'xauth':['xauth'],'dbus-x11':['dbus-run-session','dbus-monitor'],'fcitx5':['fcitx5']}
  with tempfile.TemporaryDirectory(prefix='official-apt-',dir=ROOT/'.verification') as d:
   base=Path(d);fs=base/'guest';repo=base/'repo';repo.mkdir();tools=base/'tools';tools.mkdir()
   for folder in ('var/lib/dpkg','var/lib/apt/lists/partial','var/cache/apt/archives/partial','var/log/apt','etc/apt/apt.conf.d','etc/apt/sources.list.d','usr/bin'):(fs/folder).mkdir(parents=True,exist_ok=True)
   (fs/'var/lib/dpkg/status').write_text('');(fs/'etc/apt/sources.list').write_text('deb [trusted=yes] file:'+str(repo)+' ./\n')
   for name in names:
    package=base/('package-'+name);(package/'DEBIAN').mkdir(parents=True);(package/'DEBIAN').chmod(0o755);(package/'DEBIAN/control').write_text('Package: '+name+'\nVersion: 1.0\nArchitecture: all\nMaintainer: Fixture <fixture@example.invalid>\nDescription: private script-free fixture\n')
    for command in programs.get(name,[]):
     p=package/'usr/bin'/command;p.parent.mkdir(parents=True,exist_ok=True);p.write_text('#!/bin/sh\nexit 0\n');p.chmod(0o755)
    subprocess.run(['/usr/bin/dpkg-deb','--build',str(package),str(repo/(name+'_1.0_all.deb'))],check=True,capture_output=True,timeout=10)
   index=subprocess.run(['/usr/bin/dpkg-scanpackages','.'],cwd=repo,check=True,capture_output=True,timeout=10);(repo/'Packages').write_bytes(index.stdout)
   config=base/'apt.conf';config.write_text('\n'.join([
    'Dir::Etc "'+str(fs/'etc/apt')+'";','Dir::Etc::parts "apt.conf.d";','Dir::Etc::main "-";',
    'Dir::State "'+str(fs/'var/lib/apt')+'";','Dir::State::status "'+str(fs/'var/lib/dpkg/status')+'";',
    'Dir::Cache "'+str(fs/'var/cache/apt')+'";','Dir::Log "'+str(fs/'var/log/apt')+'";',
    'Dir::Bin::dpkg "/usr/bin/dpkg";','APT::Sandbox::User "root";','Dpkg::Use-Pty "0";',
    'DPkg::Options { "--root='+str(fs)+'"; "--log='+str(fs/'var/log/dpkg.log')+'"; };',
    '#clear DPkg::Pre-Invoke;','#clear DPkg::Post-Invoke;','#clear DPkg::Pre-Install-Pkgs;','#clear APT::Update::Post-Invoke;','#clear APT::Update::Post-Invoke-Success;','']))
   for name,body in {'apt-get':'exec /usr/bin/apt-get "$@"','dpkg-query':'exec /usr/bin/dpkg-query --admindir="$JOBS_FIXTURE_DB" "$@"','update-ca-certificates':'exit 0'}.items():
    p=tools/name;p.write_text('#!/bin/sh\n'+body+'\n');p.chmod(0o755)
   env=dict(os.environ,PATH=str(tools)+':'+str(fs/'usr/bin')+':/usr/bin:/bin',APT_CONFIG=str(config),DPKG_ADMINDIR=str(fs/'var/lib/dpkg'),JOBS_FIXTURE_DB=str(fs/'var/lib/dpkg'))
   for repeat in range(2):
    result=subprocess.run(['/bin/sh',str(SCRIPT),'install','debian'],env=env,capture_output=True,timeout=60)
    self.assertEqual(0,result.returncode,(result.stdout+result.stderr).decode()[-3000:])
   result=subprocess.run(['/usr/bin/dpkg-query','--admindir='+str(fs/'var/lib/dpkg'),'-W','-f=${Package}\n'],capture_output=True,text=True,check=True);self.assertEqual(set(names),set(result.stdout.splitlines()))
  self.assertEqual(before,hashlib.sha256(Path('/var/lib/dpkg/status').read_bytes()).hexdigest())
if __name__=='__main__':unittest.main()
