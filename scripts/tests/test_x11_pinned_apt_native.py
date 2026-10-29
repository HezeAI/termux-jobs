"""Real APT + dpkg transactions in an empty disposable database/root only.

No PRoot or installed environment is required. No repository sources are used;
controlled test scripts can write only below the disposable DPKG_ROOT. The
production helper runs through wrappers which only redirect the
native tools' state/cache/log/root into this test's private directory.
"""
from pathlib import Path
import os,shlex,signal,subprocess,tempfile,time,unittest
ROOT=Path(__file__).resolve().parents[2]
HELPER=ROOT/'x11/src/main/assets/x11-base-install.sh'
TOOLS=['/usr/bin/apt-get','/usr/bin/dpkg','/usr/bin/dpkg-query','/usr/bin/dpkg-deb']
@unittest.skipUnless(all(Path(x).is_file() for x in TOOLS),'native Debian tools required')
class AptTransactions(unittest.TestCase):
 def setUp(self):
  self.temp=tempfile.TemporaryDirectory(dir=ROOT/'.verification');self.root=Path(self.temp.name);self.bin=self.root/'bin';self.bin.mkdir();self.guest=self.root/'guest'
  for n in ['var/lib/dpkg','var/lib/apt/lists/partial','var/cache/apt/archives/partial','var/log/apt']:(self.guest/n).mkdir(parents=True,exist_ok=True)
  (self.guest/'var/lib/dpkg/status').touch();self.packages=self.root/'cache/packages';self.packages.mkdir(parents=True)
  opts=['-o','Dir::State='+str(self.guest/'var/lib/apt'),'-o','Dir::State::status='+str(self.guest/'var/lib/dpkg/status'),'-o','Dir::Cache='+str(self.guest/'var/cache/apt'),'-o','Dir::Log='+str(self.guest/'var/log/apt'),'-o','APT::Sandbox::User=root','-o','Dpkg::Use-Pty=0','-o','DPkg::Options::=--root='+str(self.guest),'-o','DPkg::Options::=--log='+str(self.guest/'var/log/dpkg.log')]
  self.wrapper('apt-get',['/usr/bin/apt-get',*opts]);self.wrapper('dpkg-query',['/usr/bin/dpkg-query','--admindir='+str(self.guest/'var/lib/dpkg')]);self.wrapper('dpkg',['/usr/bin/dpkg','--root='+str(self.guest),'--log='+str(self.guest/'var/log/dpkg.log')])
  self.env=dict(os.environ,PATH=str(self.bin)+':/usr/sbin:/usr/bin:/sbin:/bin',LC_ALL='C',DEBIAN_FRONTEND='noninteractive',DPKG_ADMINDIR=str(self.guest/'var/lib/dpkg'))
 def tearDown(self):self.temp.cleanup()
 def wrapper(self,name,args):
  p=self.bin/name;p.write_text('#!/bin/sh\nexec '+shlex.join(args)+' "$@"\n');p.chmod(0o700)
 def package(self,name,version='1',depends=None):
  build=self.root/(name+'-build');(build/'DEBIAN').mkdir(parents=True);(build/'DEBIAN').chmod(0o755)
  text=f'Package: {name}\nVersion: {version}\nArchitecture: all\nMaintainer: Fixture <test@example.invalid>\nDescription: isolated transaction fixture\n'
  if depends:text+='Depends: '+depends+'\n'
  (build/'DEBIAN/control').write_text(text);data=build/'usr/share'/name;data.mkdir(parents=True);(data/'proof').write_text('installed\n')
  filename=f'{name}_{version}_all.deb';subprocess.run(['/usr/bin/dpkg-deb','--build',str(build),str(self.packages/filename)],check=True,capture_output=True,timeout=20)
  with (self.packages.parent/'versions.tsv').open('a') as f:f.write(f'{name}\t{version}\t{filename}\n')
  return self.packages/filename
 def run_helper(self):return subprocess.run(['/bin/sh',str(HELPER),str(self.packages),'debian'],env=self.env,capture_output=True,text=True,timeout=40)
 def test_fresh_cache_reproduces_old_failure_then_real_install_and_repeat_succeed(self):
  deb=self.package('jobs-local-test')
  before=subprocess.run([str(self.bin/'apt-get'),'-o','Dir::Etc::sourcelist=/dev/null','-o','Dir::Etc::sourceparts=/dev/null','--no-download','install','-y','--no-remove',str(deb)],env=self.env,capture_output=True,text=True,timeout=40)
  self.assertEqual(100,before.returncode,before.stdout+before.stderr);self.assertIn('Pathname to install is not absolute',before.stderr)
  fixed=self.run_helper();self.assertEqual(0,fixed.returncode,fixed.stdout+fixed.stderr)
  self.assertTrue((self.guest/'usr/share/jobs-local-test/proof').is_file());self.assertIn('install ok installed',(self.guest/'var/lib/dpkg/status').read_text())
  again=self.run_helper();self.assertEqual(0,again.returncode,again.stdout+again.stderr);self.assertIn('0 newly installed',again.stdout)
 def test_missing_dependency_fails_without_using_upstream_or_changing_database(self):
  self.package('jobs-missing-test',depends='jobs-does-not-exist (= 1)');before=(self.guest/'var/lib/dpkg/status').read_bytes();result=self.run_helper()
  self.assertEqual(100,result.returncode,result.stdout+result.stderr);self.assertIn('jobs-does-not-exist',result.stdout+result.stderr);self.assertEqual(before,(self.guest/'var/lib/dpkg/status').read_bytes());self.assertNotIn('http:',result.stdout+result.stderr);self.assertNotIn('https:',result.stdout+result.stderr)
 def test_interrupted_configuration_resumes_with_real_dpkg_and_no_manual_database_edits(self):
  deb=self.package('jobs-interrupt-test');script=self.root/'jobs-interrupt-test-build/DEBIAN/postinst'
  script.write_text('#!/bin/sh\nset -eu\n: "${DPKG_ROOT:?}"\n[ -f "$DPKG_ROOT/continue" ] && exit 0\ntouch "$DPKG_ROOT/postinst-start"\nwhile :;do sleep .1;done\n');script.chmod(0o755)
  subprocess.run(['/usr/bin/dpkg-deb','--build',str(script.parent.parent),str(deb)],check=True,capture_output=True,timeout=20)
  # The empty fixture has no shell/runtime of its own. Only this controlled
  # script uses chrootless mode and it requires the private DPKG_ROOT explicitly.
  wrapper=self.bin/'apt-get';wrapper.write_text(wrapper.read_text().replace('exec /usr/bin/apt-get','exec /usr/bin/apt-get -o DPkg::Options::=--force-script-chrootless'))
  self.wrapper('dpkg',['/usr/bin/dpkg','--root='+str(self.guest),'--log='+str(self.guest/'var/log/dpkg.log'),'--force-script-chrootless'])
  child=subprocess.Popen(['/bin/sh',str(HELPER),str(self.packages),'debian'],env=self.env,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,start_new_session=True)
  try:
   deadline=time.monotonic()+30
   while not (self.guest/'postinst-start').exists() and time.monotonic()<deadline:time.sleep(.05)
   self.assertTrue((self.guest/'postinst-start').exists())
   # The unreaped, live child reserves the identity of this test-owned group.
   os.killpg(child.pid,signal.SIGTERM);child.communicate(timeout=10)
  finally:
   if child.poll() is None:os.killpg(child.pid,signal.SIGKILL);child.communicate(timeout=10)
  (self.guest/'continue').touch();result=self.run_helper();self.assertEqual(0,result.returncode,result.stdout+result.stderr)
  self.assertIn('BASE_STAGE:recovering-apt',result.stdout);self.assertNotIn('WARNING:BASE_APT_CONFIGURATION_PENDING',result.stdout)
  self.assertIn('install ok installed',(self.guest/'var/lib/dpkg/status').read_text())
if __name__=='__main__':unittest.main()
