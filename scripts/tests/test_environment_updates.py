"""Private filesystem fixtures; never execute installed environment package managers."""
import contextlib,importlib.util,io,json,os,subprocess,sys,tempfile,unittest,uuid
from pathlib import Path
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[2];ASSETS=ROOT/'app/src/main/assets'
spec=importlib.util.spec_from_file_location('env_updates',ASSETS/'environment-updates.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
class Updates(unittest.TestCase):
 def fixture(self,path):
  files=Path(path)/'files';prefix=files/'usr';(prefix/'var/lib/proot-distro/containers/debian/rootfs').mkdir(parents=True)
  container=prefix/'var/lib/proot-distro/containers/debian';(container/'manifest.json').write_text('{}')
  for name,data in {'etc/os-release':'ID=debian\nVERSION_ID=13\n','bin/sh':'#!/bin/sh\n','usr/bin/apt-get':'#!/bin/sh\n','usr/bin/env':'#!/bin/sh\n','var/lib/dpkg/status':'Package: demo\nVersion: 1\n','root/project.txt':'original','home/user.txt':'home-data'}.items():
   p=container/'rootfs'/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(data);p.chmod(0o700)
  return files,prefix,container
 def runtime(self,files,prefix,runner=None):return m.Runtime(files,prefix,str(uuid.uuid4()),runner)
 def fake(self,prefix,fail=False):
  def run(args,**kw):
   alias=args[args.index('--')-1];container=prefix/'var/lib/proot-distro/containers'/alias;script=kw.get('input_data',b'').decode()
   if 'full-upgrade' in script:
    if fail:return 42,b'password=PRIVATE-TEST-SECRET\nunmet dependencies'
    (container/'rootfs/var/lib/dpkg/status').write_text('Package: demo\nVersion: 2\n')
    return 0,b'private package output'
   return 0,b'refresh\nJOBS_UPDATES_BEGIN\ndemo/stable 2 arm64 [upgradable from: 1]\n'
  return run
 def test_upgrade_exchanges_full_copy_and_keeps_old_snapshot(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d,patch.object(m.B,'locked_remove_available',return_value=True),contextlib.redirect_stdout(io.StringIO()):
   files,prefix,container=self.fixture(d);original=container.stat().st_ino;one=self.runtime(files,prefix,self.fake(prefix))
   try:plan=one.check('debian')
   finally:one.close()
   two=self.runtime(files,prefix,self.fake(prefix))
   try:
    result=two.transaction('debian','upgrade',plan['id']);self.assertNotEqual(original,container.stat().st_ino);saved=two.root/'snapshots'/result['snapshot']/'container';self.assertEqual(original,saved.stat().st_ino);self.assertIn('Version: 1',(saved/'rootfs/var/lib/dpkg/status').read_text());self.assertIn('Version: 2',(container/'rootfs/var/lib/dpkg/status').read_text());self.assertEqual('original',(container/'rootfs/root/project.txt').read_text())
   finally:two.close()
 def test_failed_package_child_never_changes_source_and_does_not_leak_output(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d,patch.object(m.B,'locked_remove_available',return_value=True):
   files,prefix,container=self.fixture(d);one=self.runtime(files,prefix,self.fake(prefix));output=io.StringIO()
   try:
    with contextlib.redirect_stdout(output):plan=one.check('debian')
   finally:one.close()
   old=container.stat().st_ino;two=self.runtime(files,prefix,self.fake(prefix,True))
   try:
    with contextlib.redirect_stdout(output),self.assertRaisesRegex(m.Refused,'DEPENDENCY_CONFLICT'):two.transaction('debian','upgrade',plan['id'])
    self.assertEqual(old,container.stat().st_ino);self.assertIn('Version: 1',(container/'rootfs/var/lib/dpkg/status').read_text());self.assertNotIn('PRIVATE-TEST-SECRET',output.getvalue());self.assertFalse(any(p.name.startswith('jobs-update-') for p in container.parent.iterdir()))
   finally:two.close()
 def test_external_symlink_is_copied_without_reading_its_target(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d,contextlib.redirect_stdout(io.StringIO()):
   files,prefix,c=self.fixture(d);outside=Path(d)/'outside';outside.write_text('untouched');(c/'rootfs/root/external').symlink_to(outside);dst=Path(d)/'copy';dst.mkdir();a=m.T.m.open_dir(c);b=m.T.m.open_dir(dst)
   try:rows,total,_=m.T.scan(a);m.T.clone(a,b,rows,total,str(c))
   finally:os.close(a);os.close(b)
   self.assertTrue((dst/'rootfs/root/external').is_symlink());self.assertEqual(str(outside),os.readlink(dst/'rootfs/root/external'));self.assertEqual('untouched',outside.read_text())
 def test_guest_links_resolve_with_chroot_semantics(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   _,_,c=self.fixture(d);(c/'rootfs/etc/os-release').unlink();(c/'rootfs/usr/lib').mkdir(parents=True);(c/'rootfs/usr/lib/os-release').write_text('ID=debian\n');(c/'rootfs/etc/os-release').symlink_to('/usr/lib/os-release');fd=m.T.m.open_dir(c)
   try:self.assertEqual(b'ID=debian\n',m.T.guest_read(fd,'/etc/os-release'))
   finally:os.close(fd)
 def test_alias_files_identity_accepted_impostor_refused(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   files,prefix,_=self.fixture(d);alias=Path(d)/'files-alias';alias.symlink_to(files);r=self.runtime(alias,prefix);r.close();other=Path(d)/'other';other.mkdir()
   with self.assertRaisesRegex(m.Refused,'PRIVATE_STORAGE_ALIAS'):self.runtime(other,prefix)
 def test_external_hardlink_refused_before_copy(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   _,_,c=self.fixture(d);os.link(c/'rootfs/root/project.txt',Path(d)/'outside');fd=m.T.m.open_dir(c)
   try:
    with self.assertRaisesRegex(m.T.Refused,'EXTERNAL_HARDLINKS'):m.T.scan(fd)
   finally:os.close(fd)
 def test_all_package_scripts_and_pin_catalog_are_bounded_and_targeted(self):
  self.assertEqual(9,len(m.I.pinned()))
  for target in m.TARGETS:
   for operation in ('refresh','query','upgrade'):
    script=m.P.script(target,operation);p=subprocess.run(['bash','-n'],input=script,text=True,capture_output=True);self.assertEqual(0,p.returncode,p.stderr);self.assertNotIn('--allow-unauthenticated',script);self.assertNotIn('--nodeps',script)
  check=m.P.check_script('archlinux');self.assertIn('--dbpath "$tmp"',check);self.assertNotIn('pacman -Syu',check)

 def upgraded(self,d):
  files,prefix,c=self.fixture(d);r=self.runtime(files,prefix,self.fake(prefix))
  plan=r.check('debian');result=r.transaction('debian','upgrade',plan['id']);r.close()
  return files,prefix,c,result
 def test_rollback_keeps_current_homes_and_full_reverse_checkpoint(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d,patch.object(m.B,'locked_remove_available',return_value=True),contextlib.redirect_stdout(io.StringIO()):
   files,prefix,c,upgrade=self.upgraded(d);(c/'rootfs/root/project.txt').write_text('new project');(c/'rootfs/opt').mkdir();(c/'rootfs/opt/custom').write_text('outside home')
   r=self.runtime(files,prefix,self.fake(prefix))
   try:
    old=c.stat().st_ino;result=r.transaction('debian','rollback',snapshot_id=upgrade['snapshot'])
    self.assertIn('Version: 1',(c/'rootfs/var/lib/dpkg/status').read_text());self.assertEqual('new project',(c/'rootfs/root/project.txt').read_text())
    self.assertFalse((c/'rootfs/opt/custom').exists());safety=r.root/'snapshots'/result['snapshot']/'container'
    self.assertEqual(old,safety.stat().st_ino);self.assertEqual('outside home',(safety/'rootfs/opt/custom').read_text())
   finally:r.close()
 def test_rollback_without_home_merge_restores_snapshot_exactly(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d,patch.object(m.B,'locked_remove_available',return_value=True),contextlib.redirect_stdout(io.StringIO()):
   files,prefix,c,upgrade=self.upgraded(d);(c/'rootfs/root/project.txt').write_text('new project');r=self.runtime(files,prefix,self.fake(prefix))
   try:r.transaction('debian','rollback',snapshot_id=upgrade['snapshot'],preserve_home=False);self.assertEqual('original',(c/'rootfs/root/project.txt').read_text())
   finally:r.close()
 def test_cancel_during_copy_preserves_original_and_removes_owned_partial(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d,patch.object(m.B,'locked_remove_available',return_value=True),contextlib.redirect_stdout(io.StringIO()):
   files,prefix,c=self.fixture(d);r=self.runtime(files,prefix,self.fake(prefix));plan=r.check('debian');original=c.stat().st_ino;real=m.T.clone
   def cancelled(*args,**kwargs):r.cancel_path.touch();r.tick('copy')
   try:
    with patch.object(m.T,'clone',side_effect=cancelled),self.assertRaises(m.C.Cancelled):r.transaction('debian','upgrade',plan['id'])
    self.assertEqual(original,c.stat().st_ino);self.assertFalse(any(p.name.startswith('jobs-update-') for p in c.parent.iterdir()))
   finally:r.close()
 def test_locked_native_session_blocks_apply_without_signals(self):
  import fcntl
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d,patch.object(m.B,'locked_remove_available',return_value=True),contextlib.redirect_stdout(io.StringIO()):
   files,prefix,c=self.fixture(d);r=self.runtime(files,prefix,self.fake(prefix));plan=r.check('debian');lock=os.open(prefix/'var/lib/proot-distro/locks/debian.lock',os.O_RDWR);fcntl.flock(lock,fcntl.LOCK_SH)
   try:
    with self.assertRaisesRegex(m.Refused,'ENVIRONMENT_BUSY'):r.transaction('debian','upgrade',plan['id'])
   finally:os.close(lock);r.close()
 def test_recover_crash_before_exchange_discards_only_recorded_copy(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d,patch.object(m.B,'locked_remove_available',return_value=True),contextlib.redirect_stdout(io.StringIO()):
   files,prefix,c=self.fixture(d);r=self.runtime(files,prefix,self.fake(prefix));plan=r.check('debian');old=c.stat().st_ino
   try:
    with patch.object(m.T,'exchange',side_effect=OSError('simulated crash')),self.assertRaises(OSError):r.transaction('debian','upgrade',plan['id'])
    job=r.id;self.assertEqual(old,c.stat().st_ino)
   finally:r.close()
   r=self.runtime(files,prefix)
   try:r.recover(job);self.assertEqual(old,c.stat().st_ino);self.assertFalse(any(p.name.startswith('jobs-update-') for p in c.parent.iterdir()))
   finally:r.close()
 def test_recover_crash_after_exchange_finishes_snapshot_without_second_exchange(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d,patch.object(m.B,'locked_remove_available',return_value=True),contextlib.redirect_stdout(io.StringIO()):
   files,prefix,c=self.fixture(d);r=self.runtime(files,prefix,self.fake(prefix));plan=r.check('debian');old=c.stat().st_ino
   try:
    with patch.object(r,'finish_swap',side_effect=OSError('simulated crash')),self.assertRaises(OSError):r.transaction('debian','upgrade',plan['id'])
    job=r.id;new=c.stat().st_ino;self.assertNotEqual(old,new);snap=r.state['snapshot']
   finally:r.close()
   r=self.runtime(files,prefix)
   try:r.recover(job);self.assertEqual(new,c.stat().st_ino);self.assertEqual(old,(r.root/'snapshots'/snap/'container').stat().st_ino)
   finally:r.close()
 def test_recover_crash_after_snapshot_move_reconstructs_meta_and_origin(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d,patch.object(m.B,'locked_remove_available',return_value=True),contextlib.redirect_stdout(io.StringIO()):
   files,prefix,c=self.fixture(d);r=self.runtime(files,prefix,self.fake(prefix));plan=r.check('debian');real=m.T.write_json
   def fail(parent,name,value):
    if name=='meta.json':raise OSError('simulated crash')
    return real(parent,name,value)
   try:
    with patch.object(m.T,'write_json',side_effect=fail),self.assertRaises(OSError):r.transaction('debian','upgrade',plan['id'])
    job=r.id;snap=r.state['snapshot']
   finally:r.close()
   r=self.runtime(files,prefix)
   try:r.recover(job);self.assertTrue((r.root/'snapshots'/snap/'meta.json').is_file());self.assertTrue((r.root/'origins/debian.json').is_file())
   finally:r.close()
 def test_temp_alias_collision_never_deleted(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d,patch.object(m.B,'locked_remove_available',return_value=True),contextlib.redirect_stdout(io.StringIO()):
   files,prefix,c=self.fixture(d);r=self.runtime(files,prefix,self.fake(prefix));plan=r.check('debian');collision=c.parent/('jobs-update-'+r.id);collision.mkdir();(collision/'keep').write_text('not owned')
   try:
    with self.assertRaises(FileExistsError):r.transaction('debian','upgrade',plan['id'])
    self.assertEqual('not owned',(collision/'keep').read_text())
   finally:r.close()
 def test_changed_plan_refused_before_exchange(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d,patch.object(m.B,'locked_remove_available',return_value=True),contextlib.redirect_stdout(io.StringIO()):
   files,prefix,c=self.fixture(d);r=self.runtime(files,prefix,self.fake(prefix));plan=r.check('debian');(c/'rootfs/var/lib/dpkg/status').write_text('new installed packages')
   try:
    with self.assertRaisesRegex(m.Refused,'PLAN_EXPIRED'):r.transaction('debian','upgrade',plan['id'])
    self.assertEqual('new installed packages',(c/'rootfs/var/lib/dpkg/status').read_text())
   finally:r.close()
 def test_snapshot_delete_with_nested_caches_keeps_external_links_and_other_snapshots(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d,patch.object(m.B,'locked_remove_available',return_value=True),contextlib.redirect_stdout(io.StringIO()):
   files,prefix,c,result=self.upgraded(d);r=self.runtime(files,prefix);outside=Path(d)/'outside';outside.mkdir();(outside/'keep').write_text('safe');saved=r.root/'snapshots'/result['snapshot'];(saved/'container/rootfs/root/cache').mkdir();(saved/'container/rootfs/root/cache/link').symlink_to(outside)
   try:r.delete_snapshot(result['snapshot']);self.assertFalse(saved.exists());self.assertEqual('safe',(outside/'keep').read_text());self.assertTrue(c.is_dir())
   finally:r.close()
 def test_l2s_backing_is_relocated_from_snapshot_not_new_active_alias(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   _,_,c=self.fixture(d);backing=c/'rootfs/root/.proot.l2s.123';backing.write_text('old bytes');(c/'rootfs/root/l2s').symlink_to(backing)
   snapshot=Path(d)/'saved';c.rename(snapshot);c.mkdir();(c/'rootfs/root').mkdir(parents=True);(c/'rootfs/root/.proot.l2s.123').write_text('WRONG bytes')
   dst=Path(d)/'copy';dst.mkdir();a=m.T.m.open_dir(snapshot);b=m.T.m.open_dir(dst)
   try:rows,total,_=m.T.scan(a);m.T.clone(a,b,rows,total,str(c))
   finally:os.close(a);os.close(b)
   self.assertFalse((dst/'rootfs/root/l2s').is_symlink());self.assertEqual('old bytes',(dst/'rootfs/root/l2s').read_text());self.assertEqual((dst/'rootfs/root/l2s').stat().st_ino,(dst/'rootfs/root/.proot.l2s.123').stat().st_ino)
 def test_package_formats_empty_invalid_and_non_disclosure(self):
  data={'debian':b'Listing...\nfoo/stable 2.0 arm64 [upgradable from: 1.0]\n','alpine':b'foo-bar-2.0-r0 aarch64 [upgradable from: foo-bar-1.0-r0]\n','fedora':b'foo\t2.0-1.fc44\n','opensuse':b'<stream><update name="foo" edition="2.0" old-edition="1.0"/></stream>','void':b'foo-2.0_1 update aarch64 https://private.example/secret 50 20\n','archlinux':b'foo 1.0 -> 2.0\n'}
  for target,raw in data.items():
   rows=m.P.parse(target,raw);self.assertEqual(1,len(rows));self.assertNotIn('private.example',json.dumps(rows))
  for target in ('debian','alpine','fedora','void','archlinux'):self.assertEqual([],m.P.parse(target,b''))
  self.assertEqual([],m.P.parse('opensuse',b'<stream/>'))
  for target in ('fedora','alpine','void','archlinux'):
   with self.assertRaises(m.P.Refused):m.P.parse(target,b'not a valid package row')
 def test_remote_catalog_rejects_foreign_url_architecture_and_sha(self):
  catalog=json.loads((ASSETS/'environment-images.json').read_text())
  for field,value in [('url','https://evil.example/rootfs'),('architecture','x86_64'),('sha256','x'*64),('size',True)]:
   modified=json.loads(json.dumps(catalog));modified['images'][0][field]=value
   with self.assertRaises(m.I.Refused):m.I.validate(modified)
 def test_control_symlink_and_hardlink_rejected(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   base=Path(d);outside=base/'outside';outside.write_text('{}');private=base/'private';private.mkdir();fd=m.T.m.open_dir(private)
   try:
    (private/'bad.json').symlink_to(outside)
    with self.assertRaises(OSError):m.T.read_json(fd,'bad.json')
    os.link(outside,private/'hard.json')
    with self.assertRaises(m.T.Refused):m.T.write_json(fd,'hard.json',{'modified':True})
    self.assertEqual('{}',outside.read_text())
    (private/'ok.json.new').write_text('stale interrupted temp');m.T.write_json(fd,'ok.json',{'ok':True});self.assertTrue(m.T.read_json(fd,'ok.json')['ok'])
   finally:os.close(fd)
 def test_source_modified_during_packages_refused_and_original_change_kept(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d,patch.object(m.B,'locked_remove_available',return_value=True),contextlib.redirect_stdout(io.StringIO()):
   files,prefix,c=self.fixture(d);fake=self.fake(prefix)
   def runner(args,**kw):
    result=fake(args,**kw)
    if b'full-upgrade' in kw.get('input_data',b''):(c/'rootfs/root/project.txt').write_text('external change')
    return result
   r=self.runtime(files,prefix,runner);plan=r.check('debian');original=c.stat().st_ino
   try:
    with self.assertRaisesRegex(m.Refused,'SOURCE_CHANGED'):r.transaction('debian','upgrade',plan['id'])
    self.assertEqual(original,c.stat().st_ino);self.assertEqual('external change',(c/'rootfs/root/project.txt').read_text())
   finally:r.close()


 def test_plain_rootfs_without_oci_manifest_uses_public_login_and_can_upgrade(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d,patch.object(m.B,'locked_remove_available',return_value=True),contextlib.redirect_stdout(io.StringIO()):
   files,prefix,c=self.fixture(d);(c/'manifest.json').unlink();fake=self.fake(prefix);calls=[]
   def runner(args,**kw):calls.append(args);return fake(args,**kw)
   r=self.runtime(files,prefix,runner)
   try:
    self.assertTrue(next(row for row in r.inventory()['targets'] if row['target']=='debian')['installed'])
    plan=r.check('debian');r.transaction('debian','upgrade',plan['id']);self.assertTrue(calls);self.assertTrue(all(args[1]=='login' for args in calls));self.assertFalse((c/'manifest.json').exists());self.assertIn('Version: 2',(c/'rootfs/var/lib/dpkg/status').read_text())
   finally:r.close()
 def test_plain_arch_and_manjaro_metadata_are_discovered_and_checked_as_their_own_targets(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d,patch.object(m.B,'locked_remove_available',return_value=True),contextlib.redirect_stdout(io.StringIO()):
   files,prefix,seed=self.fixture(d);seed.rename(seed.with_name('archlinux'));arch=seed.with_name('archlinux');(arch/'manifest.json').unlink();(arch/'rootfs/etc/os-release').write_text('ID=archarm\n')
   (arch/'rootfs/usr/bin/pacman').write_text('#!/bin/sh\n');(arch/'rootfs/usr/bin/pacman').chmod(0o700);(arch/'rootfs/var/lib/pacman/local/foo').mkdir(parents=True);(arch/'rootfs/var/lib/pacman/local/foo/desc').write_text('installed')
   import shutil
   manjaro=arch.with_name('manjaro');shutil.copytree(arch,manjaro);(manjaro/'rootfs/etc/os-release').write_text('ID=manjaro-arm\n');calls=[]
   def runner(args,**kw):calls.append(args);return 0,b'\nJOBS_UPDATES_BEGIN\nfoo 1 -> 2\n'
   r=self.runtime(files,prefix,runner)
   try:
    present={row['target'] for row in r.inventory()['targets'] if row['installed']};self.assertEqual({'archlinux','manjaro'},present)
    for target in present:self.assertEqual(target,r.check(target)['target'])
    self.assertEqual({'archlinux','manjaro'},{args[args.index('--')-1] for args in calls});self.assertTrue(all(args[1]=='login' for args in calls))
   finally:r.close()


 def test_usr_symlink_cannot_redirect_native_prefix_to_foreign_tree(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   files=Path(d)/'files';files.mkdir();outside=Path(d)/'outside';outside.mkdir();(files/'usr').symlink_to(outside)
   with self.assertRaisesRegex(m.Refused,'PRIVATE_STORAGE_ALIAS'):self.runtime(files,files/'usr')

if __name__=='__main__':unittest.main()
