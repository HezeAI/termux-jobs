"""Official-image publication/rollback fixtures. No installed environment is entered."""
import contextlib,hashlib,importlib.util,io,json,os,subprocess,sys,tempfile,time,unittest,uuid
from pathlib import Path
from unittest.mock import patch
import test_environment_updates as fixture
m=fixture.m;ROOT=fixture.ROOT;ASSETS=fixture.ASSETS
def module(name):
 s=importlib.util.spec_from_file_location(name,ASSETS/(name+'.py'));v=importlib.util.module_from_spec(s);s.loader.exec_module(v);return v
F=module('environment-official-fetch');POLICY=module('environment-desktop-policy')
class OfficialInstall(unittest.TestCase):
 def fixture(self,path,initial=False,desktop=False):
  files,prefix,c=fixture.Updates().fixture(path)
  if desktop:
   for name in m.DESKTOP:
    f=c/'rootfs/usr/bin'/name;f.parent.mkdir(parents=True,exist_ok=True);f.write_text('#!/bin/sh\n');f.chmod(0o755)
  if initial:c.rename(Path(path)/'old-template')
  return files,prefix,c
 def plan(self,r,c):
  row=dict(target='debian',architecture='aarch64',kind='release',version='13.7',series='13',image='docker.io/library/debian:stable',digest='sha256:'+'a'*64)
  pid=str(uuid.uuid4());data=dict(id=pid,owner='termux-jobs-official-image-plan',target='debian',created=time.time(),containerId=m.T.ident(c.stat()) if c.exists() else None,image=row);p=r.root/'official-plans'/(pid+'.json');p.write_text(json.dumps(data));return pid,data
 def runner(self,r,c,fail=False,cancel=False,missing=False):
  calls=[]
  def run(args,**kw):
   calls.append((args,kw.get('input_data')))
   if len(args)>3 and args[3].endswith('environment-official-fetch.py'):
    record=json.loads(Path(args[4]).read_text());folder=r.root/'official-plans'/('download-'+m.O.cache_key('debian',record['image']));folder.mkdir();path=folder/'rootfs.oci.tar';path.write_bytes(b'approved fixture OCI data');return 0,json.dumps(dict(path=str(path),sha256=hashlib.sha256(path.read_bytes()).hexdigest(),size=path.stat().st_size,manifestDigest=record['image']['digest'])).encode()
   if len(args)>1 and args[1]=='install':
    name=args[args.index('--name')+1];container=r.prefix/'var/lib/proot-distro/containers'/name;self.assertTrue(container.is_dir());root=container/'rootfs';root.mkdir()
    for path,content in {'etc/os-release':'ID=debian\nVERSION_ID=13\n','bin/sh':'#!/bin/sh\n','usr/bin/apt-get':'#!/bin/sh\n','usr/bin/env':'#!/bin/sh\n','var/lib/dpkg/status':'Package: fresh\nVersion: 2\n','root/default.txt':'base'}.items():
     p=root/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(content);p.chmod(0o755)
    (container/'manifest.json').write_text('{}')
    if fail:return 1,b'failed to download'
    if cancel:r.cancel_path.touch()
    return 0,b''
   script=kw.get('input_data',b'')
   if b'jobs-official-desktop.sh install' in script and not missing:
    name=args[args.index('--')-1];root=r.prefix/'var/lib/proot-distro/containers'/name/'rootfs/usr/bin'
    for name in m.DESKTOP:p=root/name;p.write_text('#!/bin/sh\n');p.chmod(0o755)
   return 0,b''
  return run,calls
 def runtime(self,files,prefix):return m.Runtime(files,prefix,str(uuid.uuid4()))
 def test_update_preserves_homes_full_snapshot_and_origin_then_can_rollback(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d,patch.object(m.B,'locked_remove_available',return_value=True),contextlib.redirect_stdout(io.StringIO()):
   files,prefix,c=self.fixture(d);(c/'rootfs/opt').mkdir();(c/'rootfs/opt/user-app').write_text('installed app');r=self.runtime(files,prefix);r.runner,calls=self.runner(r,c);pid,_=self.plan(r,c);old=c.stat().st_ino
   try:
    result=r.official_install('debian',pid);self.assertNotEqual(old,c.stat().st_ino);self.assertEqual('original',(c/'rootfs/root/project.txt').read_text());self.assertFalse((c/'rootfs/opt/user-app').exists());saved=r.root/'snapshots'/result['snapshot']/'container';self.assertEqual('installed app',(saved/'rootfs/opt/user-app').read_text());self.assertTrue(POLICY.official(files,prefix,'debian'));self.assertFalse(list((r.root/'official-plans').glob('download-*')))
    r.transaction('debian','rollback',snapshot_id=result['snapshot'],preserve_home=False);self.assertFalse(POLICY.official(files,prefix,'debian'));self.assertTrue((c/'rootfs/opt/user-app').exists())
   finally:r.close()
 def test_initial_install_has_no_fake_snapshot_and_uses_public_archive_cli(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d,patch.object(m.B,'locked_remove_available',return_value=True),contextlib.redirect_stdout(io.StringIO()):
   files,prefix,c=self.fixture(d,initial=True);r=self.runtime(files,prefix);r.runner,calls=self.runner(r,c);pid,_=self.plan(r,c)
   try:
    result=r.official_install('debian',pid);self.assertTrue(result['installed']);self.assertNotIn('snapshot',result);self.assertTrue(POLICY.official(files,prefix,'debian'));install=next(args for args,data in calls if len(args)>1 and args[1]=='install');self.assertTrue(install[2].endswith('rootfs.oci.tar'));self.assertNotIn('@sha256',install[2]);self.assertEqual('aarch64',install[-1]);self.assertFalse(list((r.root/'snapshots').iterdir()))
   finally:r.close()
 def test_download_or_installer_failure_never_changes_original(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d,patch.object(m.B,'locked_remove_available',return_value=True),contextlib.redirect_stdout(io.StringIO()):
   files,prefix,c=self.fixture(d);r=self.runtime(files,prefix);r.runner,calls=self.runner(r,c,fail=True);pid,_=self.plan(r,c);old=c.stat().st_ino
   try:
    with self.assertRaisesRegex(m.Refused,'REPOSITORY_NETWORK'):r.official_install('debian',pid)
    self.assertEqual(old,c.stat().st_ino);self.assertFalse(any(v.name.startswith('jobs-update-') for v in c.parent.iterdir()));self.assertFalse(POLICY.official(files,prefix,'debian'))
   finally:r.close()
 def test_missing_required_desktop_binary_refuses_publication(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d,patch.object(m.B,'locked_remove_available',return_value=True),contextlib.redirect_stdout(io.StringIO()):
   files,prefix,c=self.fixture(d,desktop=True);r=self.runtime(files,prefix);r.runner,calls=self.runner(r,c,missing=True);pid,_=self.plan(r,c);old=c.stat().st_ino
   try:
    with self.assertRaisesRegex(m.Refused,'DESKTOP_COMPONENT_MISSING'):r.official_install('debian',pid)
    self.assertEqual(old,c.stat().st_ino)
   finally:r.close()
 def test_desktop_is_reinstalled_from_required_dynamic_target_packages(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d,patch.object(m.B,'locked_remove_available',return_value=True),contextlib.redirect_stdout(io.StringIO()):
   files,prefix,c=self.fixture(d,desktop=True);r=self.runtime(files,prefix);r.runner,calls=self.runner(r,c);pid,_=self.plan(r,c)
   try:
    r.official_install('debian',pid);self.assertTrue((c/'rootfs/usr/bin/xfdesktop').exists());self.assertTrue(any(b'jobs-official-desktop.sh install debian' in (data or b'') for args,data in calls));self.assertFalse(any('x11-base.py' in str(args) for args,data in calls))
   finally:r.close()
 def test_cancellation_keeps_source_and_cleans_only_recorded_partial(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d,patch.object(m.B,'locked_remove_available',return_value=True),contextlib.redirect_stdout(io.StringIO()):
   files,prefix,c=self.fixture(d);r=self.runtime(files,prefix);r.runner,calls=self.runner(r,c,cancel=True);pid,_=self.plan(r,c);old=c.stat().st_ino
   try:
    with self.assertRaises(m.C.Cancelled):r.official_install('debian',pid)
    self.assertEqual(old,c.stat().st_ino);self.assertFalse(any(v.name.startswith('jobs-update-') for v in c.parent.iterdir()))
   finally:r.close()
 def test_expired_or_replaced_environment_plan_is_refused_before_download(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d,patch.object(m.B,'locked_remove_available',return_value=True):
   files,prefix,c=self.fixture(d);r=self.runtime(files,prefix);r.runner,calls=self.runner(r,c);pid,record=self.plan(r,c);record['created']=time.time()-1900;(r.root/'official-plans'/(pid+'.json')).write_text(json.dumps(record))
   try:
    with self.assertRaisesRegex(m.Refused,'OFFICIAL_PLAN_EXPIRED'):r.official_install('debian',pid)
    self.assertEqual([],calls)
   finally:r.close()
 def test_recovery_after_initial_publish_finishes_origin_without_reinstall(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d,patch.object(m.B,'locked_remove_available',return_value=True),contextlib.redirect_stdout(io.StringIO()):
   files,prefix,c=self.fixture(d,initial=True);r=self.runtime(files,prefix);r.runner,calls=self.runner(r,c);pid,_=self.plan(r,c)
   try:
    with patch.object(r,'finish_initial',side_effect=OSError('crash')),self.assertRaises(OSError):r.official_install('debian',pid)
    job=r.id;identity=c.stat().st_ino
   finally:r.close()
   r=self.runtime(files,prefix)
   try:r.recover(job);self.assertEqual(identity,c.stat().st_ino);self.assertTrue(POLICY.official(files,prefix,'debian'))
   finally:r.close()
 def test_policy_actual_entry_accepts_alias_and_rejects_impostor_files_dir(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   files,prefix,c=self.fixture(d);origin=files/'workspace-environment-updates/origins';origin.mkdir(parents=True);(origin/'debian.json').write_text(json.dumps({'containerId':m.T.ident(c.stat()),'image':{'channel':'official'}}));alias=Path(d)/'files-alias';alias.symlink_to(files);other=Path(d)/'other';other.mkdir()
   script=str(ASSETS/'environment-desktop-policy.py')
   good=subprocess.run([sys.executable,'-I','-S',script,str(alias),str(prefix),'debian'],capture_output=True,text=True);self.assertEqual((0,'official\n'),(good.returncode,good.stdout))
   bad=subprocess.run([sys.executable,'-I','-S',script,str(other),str(prefix),'debian'],capture_output=True);self.assertEqual(2,bad.returncode)
 def test_blob_redirect_drops_bearer_and_refuses_http_or_unknown_domain(self):
  req=F.urllib.request.Request('https://registry-1.docker.io/v2/x',headers={'Authorization':'Bearer fixture-token'});handler=F.Blobs()
  redirected=handler.redirect_request(req,None,307,'redirect',{},'https://production.cloudflare.docker.com/x');self.assertIsNone(redirected.get_header('Authorization'))
  for url in ('http://production.cloudflare.docker.com/x','https://evil.invalid/x'):
   with self.assertRaises(F.O.Refused):handler.redirect_request(req,None,307,'redirect',{},url)
 def test_managed_native_links_survive_alias_rename_without_touching_external_links(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   files,prefix,c=self.fixture(d);p=c/'rootfs/usr/bin/.proot.l2s.fixture';p.write_text('binary payload');link=p.with_name('tool');link.symlink_to(p);external=Path(d)/'outside';external.write_text('external');(c/'rootfs/root/external').symlink_to(external)
   fd=m.T.m.open_dir(c)
   try:m.T.materialize_managed_links(fd,[str(c)])
   finally:os.close(fd)
   relocated=c.with_name('relocated');c.rename(relocated);tool=relocated/'rootfs/usr/bin/tool';self.assertFalse(tool.is_symlink());self.assertEqual('binary payload',tool.read_text());self.assertEqual(tool.stat().st_ino,(relocated/'rootfs/usr/bin/.proot.l2s.fixture').stat().st_ino);self.assertTrue((relocated/'rootfs/root/external').is_symlink());self.assertEqual('external',external.read_text())
 def test_managed_link_into_another_container_is_refused(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   files,prefix,c=self.fixture(d);outside=Path(d)/'.proot.l2s.foreign';outside.write_text('foreign');(c/'rootfs/usr/bin/tool').symlink_to(outside);fd=m.T.m.open_dir(c)
   try:
    with self.assertRaises(m.T.Refused):m.T.materialize_managed_links(fd,[str(c)])
   finally:os.close(fd)
   self.assertEqual('foreign',outside.read_text())

if __name__=='__main__':unittest.main()
