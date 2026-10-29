"""Real APT/dpkg updates of tiny script-free packages in private roots.
Production workflow and package scripts run through wrappers redirecting ALL state,
configuration/cache/log/database/root paths. No installed environment is entered.
"""
import contextlib,hashlib,io,os,shlex,subprocess,tempfile,unittest
from pathlib import Path
from unittest.mock import patch
import test_environment_updates as fixture
m=fixture.m;ROOT=fixture.ROOT
@unittest.skipUnless(all(Path('/usr/bin/'+name).exists() for name in ('apt-get','apt','dpkg','dpkg-deb','dpkg-scanpackages')),'native Debian fixture tools required')
class NativeAPT(unittest.TestCase):
 def package(self,workspace,repo,version):
  base=workspace/('build-'+version);(base/'DEBIAN').mkdir(parents=True);(base/'DEBIAN').chmod(0o755);(base/'usr/share/demo').mkdir(parents=True)
  (base/'DEBIAN/control').write_text(f'Package: demo\nVersion: {version}\nArchitecture: all\nMaintainer: Fixture <fixture@example.invalid>\nDescription: private update fixture, no scripts\n')
  (base/'usr/share/demo/proof').write_text(version)
  deb=repo/('demo_'+version+'_all.deb')
  result=subprocess.run(['/usr/bin/dpkg-deb','--build',str(base),str(deb)],capture_output=True,timeout=10);self.assertEqual(0,result.returncode,result.stderr.decode())
  return deb
 def test_real_cold_cache_upgrade_repeat_check_and_rollback_keep_host_database(self):
  before=hashlib.sha256(Path('/var/lib/dpkg/status').read_bytes()).hexdigest();helper=fixture.Updates()
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d,patch.object(m.B,'locked_remove_available',return_value=True),contextlib.redirect_stdout(io.StringIO()):
   workspace=Path(d);files,prefix,c=helper.fixture(d);repo=workspace/'repo';repo.mkdir();first=self.package(workspace,repo,'1.0');self.package(workspace,repo,'2.0')
   index=subprocess.run(['/usr/bin/dpkg-scanpackages','--multiversion','.'],cwd=repo,check=True,capture_output=True,timeout=10);(repo/'Packages').write_bytes(index.stdout)
   guest=c/'rootfs';(guest/'var/lib/dpkg/status').write_text('')
   subprocess.run(['/usr/bin/dpkg','--root='+str(guest),'--log='+str(workspace/'initial-dpkg.log'),'--install',str(first)],check=True,capture_output=True,timeout=20)
   wrappers=workspace/'tools';wrappers.mkdir();configs=workspace/'configs';configs.mkdir()
   def runner(args,**kw):
    alias=args[args.index('--')-1];fs=prefix/'var/lib/proot-distro/containers'/alias/'rootfs'
    for name in ('var/lib/apt/lists/partial','var/cache/apt/archives/partial','var/log/apt','etc/apt/apt.conf.d','etc/apt/sources.list.d'):(fs/name).mkdir(parents=True,exist_ok=True)
    (fs/'etc/apt/sources.list').write_text('deb [trusted=yes] file:'+str(repo)+' ./\n')
    config=configs/(alias+'.conf')
    config.write_text('\n'.join([
     'Dir::Etc "'+str(fs/'etc/apt')+'";','Dir::Etc::parts "apt.conf.d";','Dir::Etc::main "-";',
     'Dir::State "'+str(fs/'var/lib/apt')+'";','Dir::State::status "'+str(fs/'var/lib/dpkg/status')+'";',
     'Dir::Cache "'+str(fs/'var/cache/apt')+'";','Dir::Log "'+str(fs/'var/log/apt')+'";',
     'Dir::Bin::dpkg "/usr/bin/dpkg";','APT::Sandbox::User "root";','Dpkg::Use-Pty "0";',
     'DPkg::Options { "--root='+str(fs)+'"; "--log='+str(fs/'var/log/dpkg.log')+'"; };',
     '#clear DPkg::Pre-Invoke;','#clear DPkg::Post-Invoke;','#clear DPkg::Pre-Install-Pkgs;',
     '#clear APT::Update::Post-Invoke;','#clear APT::Update::Post-Invoke-Success;','']))
    for name in ('apt-get','apt'):
     wrapper=wrappers/name;wrapper.write_text('#!/bin/sh\nexec /usr/bin/'+name+' "$@"\n');wrapper.chmod(0o700)
    script=kw['input_data'].decode().replace('export PATH='+m.P.PATH,'export PATH='+str(wrappers)+':'+m.P.PATH)
    env=dict(os.environ,APT_CONFIG=str(config),LC_ALL='C',DEBIAN_FRONTEND='noninteractive',DPKG_ADMINDIR=str(fs/'var/lib/dpkg'))
    result=subprocess.run(['/bin/sh','-s'],input=script.encode(),env=env,capture_output=True,timeout=30)
    if result.returncode:raise AssertionError((result.stdout+result.stderr).decode())
    return result.returncode,result.stdout+result.stderr
   r=helper.runtime(files,prefix,runner)
   try:
    plan=r.check('debian');self.assertEqual([{'name':'demo','version':'2.0','installed':'1.0'}],plan['candidates']);self.assertEqual('1.0',(guest/'usr/share/demo/proof').read_text())
    result=r.transaction('debian','upgrade',plan['id']);self.assertEqual('2.0',(guest/'usr/share/demo/proof').read_text())
    self.assertEqual([],r.check('debian')['candidates'])
    r.transaction('debian','rollback',snapshot_id=result['snapshot'],preserve_home=False);self.assertEqual('1.0',(guest/'usr/share/demo/proof').read_text())
   finally:r.close()
  self.assertEqual(before,hashlib.sha256(Path('/var/lib/dpkg/status').read_bytes()).hexdigest())
if __name__=='__main__':unittest.main()
