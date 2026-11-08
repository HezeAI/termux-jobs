"""Real fd-relative filesystem/lock operations, exclusively under the repository."""
import importlib.util,fcntl,os,tempfile,unittest,json
from pathlib import Path
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[2]
s=importlib.util.spec_from_file_location('agent_environment',ROOT/'app/src/main/assets/agent-environment.py');m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
class EnvironmentRemovalTests(unittest.TestCase):
 def setUp(self):
  self.tmp=tempfile.TemporaryDirectory(dir=ROOT/'.verification');self.base=Path(self.tmp.name);self.source=self.base/'containers/ubuntu';self.root=self.source/'rootfs'
  for n in ['etc','var/lib/dpkg','root/projects','usr/bin']:(self.root/n).mkdir(parents=True)
  (self.source/'manifest.json').write_text('{"image_config":{"config":{}}}')
  (self.root/'etc/os-release').write_text('ID=ubuntu\n');(self.root/'var/lib/dpkg/status').write_text('Package: fixture\nStatus: install ok installed\n')
  self.secret=self.root/'root/projects/key';self.secret.write_bytes(b'synthetic-fixture-not-a-real-key');self.secret.chmod(0o600)
  self.r=m.Runtime(str(self.base),progress=lambda **kw:None)
 def hardlink(self,source,target):
  if hasattr(os,'link'):os.link(source,target);return
  # Android Python omits os.link; exercise the same real kernel link via libc.
  import ctypes
  libc=ctypes.CDLL(None,use_errno=True)
  if libc.link(os.fsencode(source),os.fsencode(target))!=0:
   if ctypes.get_errno() in (1,13):self.skipTest('Android denies native hardlinks; covered separately on Linux/PRoot')
   raise OSError(ctypes.get_errno(),'fixture link failed')
 def tearDown(self):self.tmp.cleanup()
 def token(self):return self.r.plan()['token']
 def test_access_diagnostics_identify_scan_without_disclosing_private_path(self):
  error=PermissionError(13,'private exception','/synthetic/private-key')
  with patch.object(m,'walk',side_effect=error),self.assertRaises(PermissionError) as caught:self.token()
  event=m.error_event(caught.exception)
  self.assertEqual('SCAN_ACCESS_DENIED',event['code']);self.assertEqual(13,event['errno']);self.assertNotIn('private',json.dumps(event))
 def test_failed_walk_reports_only_constructed_relative_metadata_not_exception_text(self):
  original=m.os.open
  def opening(name,*args,**kwargs):
   if name=='projects':raise PermissionError(13,'secret exception text','/outside/private-key')
   return original(name,*args,**kwargs)
  with patch.object(m.os,'open',side_effect=opening),self.assertRaises(PermissionError) as caught:self.token()
  event=m.error_event(caught.exception);self.assertEqual('rootfs/root/projects',event['scanPath']);self.assertEqual('open-directory',event['scanOperation']);self.assertNotIn('secret exception',json.dumps(event));self.assertNotIn('/outside/',json.dumps(event))
 def test_access_diagnostics_identify_lock_open(self):
  original=m.os.open
  def opening(name,*args,**kwargs):
   if name=='ubuntu.lock':raise PermissionError(13,'private exception','secret')
   return original(name,*args,**kwargs)
  with patch.object(m.os,'open',side_effect=opening),self.assertRaises(PermissionError) as caught:self.token()
  self.assertEqual('LOCKS_ACCESS_DENIED',m.error_event(caught.exception)['code'])
 def test_access_diagnostics_identify_proc_listing(self):
  original=m.os.listdir
  def listing(path):
   if path=='/proc':raise PermissionError(13,'hidden')
   return original(path)
  with patch.object(m.os,'listdir',side_effect=listing),self.assertRaises(PermissionError) as caught:self.token()
  self.assertEqual('PROCESS_CHECK_DENIED',m.error_event(caught.exception)['code'])
 def test_remove_pins_environment_and_does_not_follow_external_symlinks(self):
  outside=self.base/'outside';outside.mkdir();(outside/'keep').write_text('keep');(self.root/'external').symlink_to(outside)
  token=self.token();self.r.remove('ubuntu',token);self.assertEqual('keep',(outside/'keep').read_text());self.assertFalse(self.source.exists())
 def test_remove_changed_or_unsafe_alias_never_deletes(self):
  token=self.token();self.secret.write_text('changed')
  with self.assertRaisesRegex(m.Refused,'SOURCE_CHANGED'):self.r.remove('ubuntu',token)
  for alias in ['../outside','/','--all','']:
   with self.assertRaises(m.Refused):self.r.remove(alias,token)
  self.assertTrue(self.source.exists())
 def test_replaced_container_is_restored_without_deleting_the_new_object(self):
  token=self.token();original=m.rename_new
  def swap(parent,old,new):
   if old=='ubuntu':
    os.rename('ubuntu','original',src_dir_fd=parent,dst_dir_fd=parent);os.mkdir('ubuntu',dir_fd=parent)
   original(parent,old,new)
  with patch.object(m,'rename_new',side_effect=swap),self.assertRaisesRegex(m.Refused,'SOURCE_CHANGED'):self.r.remove('ubuntu',token)
  self.assertTrue((self.base/'containers/original/rootfs/root/projects/key').exists());self.assertEqual([],os.listdir(self.source))

 def test_failed_deletion_reports_quarantined_remainder_without_touching_other_system(self):
  token=self.token();other=self.base/'containers/debian';other.mkdir();(other/'keep').write_text('keep')
  with patch.object(m,'remove_tree',side_effect=PermissionError()),self.assertRaisesRegex(m.Refused,'DELETE_INCOMPLETE'):self.r.remove('ubuntu',token)
  self.assertFalse(self.source.exists());self.assertEqual('keep',(other/'keep').read_text());hidden=list((self.base/'containers').glob('.jobs-delete-*'));self.assertEqual(1,len(hidden));self.assertTrue((hidden[0]/'rootfs/root/projects/key').exists())
 def test_cleanup_can_open_sealed_directories_without_touching_link_targets(self):
  name='.jobs-delete-'+'a'*32;trash=self.base/name;sealed=trash/'sealed';sealed.mkdir(parents=True);(sealed/'payload').write_text('delete')
  outside=self.base/'outside';outside.mkdir();kept=outside/'keep';kept.write_text('keep');outside.chmod(0o500);(trash/'link').symlink_to(outside)
  self.hardlink(kept,trash/'hard');kept.chmod(0o600);sealed.chmod(0);trash.chmod(0)
  parent=os.open(self.base,m.DIR)
  try:m.remove_tree(parent,name)
  finally:os.close(parent)
  self.assertFalse(trash.exists());self.assertEqual('keep',kept.read_text());self.assertEqual(0o500,outside.stat().st_mode&0o777);self.assertEqual(0o600,kept.stat().st_mode&0o777)
 def test_cleanup_unlinks_unstatable_file_without_following_a_target(self):
  name='.jobs-delete-'+'c'*32;trash=self.base/name;trash.mkdir();outside=self.base/'outside-file';outside.write_text('keep');(trash/'denied').symlink_to(outside);original=m.os.stat
  def denied(n,*args,**kwargs):
   if n=='denied':raise PermissionError(1,'fixture')
   return original(n,*args,**kwargs)
  fd=os.open(self.base,m.DIR)
  try:
   with patch.object(m.os,'stat',side_effect=denied):m.remove_tree(fd,name)
  finally:os.close(fd)
  self.assertFalse(trash.exists());self.assertEqual('keep',outside.read_text())
 def test_cleanup_refuses_normal_alias_and_linked_root(self):
  parent=os.open(self.base,m.DIR)
  try:
   with self.assertRaisesRegex(m.Refused,'UNSAFE_CLEANUP_NAME'):m.remove_tree(parent,'containers')
   name='.jobs-delete-'+'b'*32;(self.base/name).symlink_to(self.source)
   with self.assertRaises(OSError):m.remove_tree(parent,name)
   self.assertTrue(self.secret.is_file())
  finally:os.close(parent)
 def test_parent_traversal_does_not_require_directory_read_permission(self):
  original=os.open;seen=[]
  def execute_only(name,flags,*args,**kw):
   seen.append((name,flags))
   if name in ['/','root','.verification'] and not flags&os.O_PATH:raise PermissionError(13,'fixture traversal only')
   return original(name,flags,*args,**kw)
  with patch.object(m.os,'open',side_effect=execute_only):fd=m.open_dir(str(self.base))
  try:self.assertIn('containers',os.listdir(fd))
  finally:os.close(fd)
  self.assertTrue(seen[0][1]&os.O_PATH);self.assertFalse(seen[-1][1]&os.O_PATH)
 def test_ancestor_links_are_still_refused_with_path_descriptors(self):
  target=self.base/'outside';target.mkdir();link=self.base/'alias';link.symlink_to(target,target_is_directory=True)
  with self.assertRaises(OSError):m.open_dir(str(link/'child'))
 def test_removing_old_ubuntu_does_not_require_stopping_independent_ai(self):
  target=self.base/'containers/ubuntu-ai/rootfs/root/projects';target.mkdir(parents=True);(target/'key').write_text('independent fixture');(self.base/'locks').mkdir(exist_ok=True);fd=os.open(self.base/'locks/ubuntu-ai.lock',os.O_RDWR|os.O_CREAT,0o600);fcntl.flock(fd,fcntl.LOCK_SH|fcntl.LOCK_NB)
  try:self.r.remove('ubuntu',self.token())
  finally:os.close(fd)
  self.assertFalse(self.source.exists());self.assertTrue((self.base/'containers/ubuntu-ai/rootfs/root/projects/key').exists())

 def sealed(self,content=False):
  self.source.rename(self.source.with_name('archlinux'));self.source=self.source.with_name('archlinux');self.root=self.source/'rootfs'
  sealed=self.root/'run/systemd/dissect-root';sealed.mkdir(parents=True)
  if content:(sealed/'keep').write_text('must survive')
  sealed.chmod(0)
  neighbour=self.base/'containers/ubuntu/rootfs';neighbour.mkdir(parents=True);(neighbour/'keep').write_text('neighbour')
  return sealed,neighbour
 def test_arch_empty_sealed_placeholder_is_reported_and_removed_without_chmod(self):
  sealed,other=self.sealed();plan=self.r.plan('archlinux')
  self.assertEqual('archlinux',plan['alias']);self.assertEqual('containers/archlinux',plan['container'])
  self.assertEqual([m.SEALED_SYSTEMD_DIRECTORY],plan['unscanned']);self.assertEqual(0,sealed.stat().st_mode&0o777)
  with patch.object(m.os,'chmod',side_effect=AssertionError('must not chmod')),patch.object(m.os,'fchmod',side_effect=AssertionError('must not chmod')):
   self.r.remove('archlinux',plan['token'])
  self.assertFalse(self.source.exists());self.assertEqual('neighbour',(other/'keep').read_text())
 def test_arch_nonempty_sealed_placeholder_restores_alias_without_deleting_files(self):
  sealed,other=self.sealed(True);plan=self.r.plan('archlinux')
  with self.assertRaisesRegex(m.Refused,'SEALED_DIRECTORY_NOT_EMPTY'):self.r.remove('archlinux',plan['token'])
  self.assertTrue(self.source.is_dir());self.assertEqual(0,sealed.stat().st_mode&0o777);self.assertEqual('neighbour',(other/'keep').read_text())
  self.assertFalse(list(self.source.parent.glob('.jobs-delete-*')))
  # Restore this test fixture's permission solely for assertion/cleanup.
  sealed.chmod(0o700);self.assertEqual('must survive',(sealed/'keep').read_text())
 def test_cancel_arch_scan_retains_both_environments_and_modes(self):
  sealed,other=self.sealed();self.r.cancel=lambda:True
  with self.assertRaises(m.Cancelled):self.r.plan('archlinux')
  self.assertTrue(self.source.exists());self.assertTrue(other.exists());self.assertEqual(0,sealed.stat().st_mode&0o777)
  sealed.chmod(0o700)
 def test_other_denied_directory_is_not_excluded(self):
  sealed,other=self.sealed();original=m.os.open
  def deny(name,*args,**kwargs):
   if name=='projects':raise PermissionError(13,'fixture')
   return original(name,*args,**kwargs)
  with patch.object(m.os,'open',side_effect=deny),self.assertRaises(PermissionError):self.r.plan('archlinux')
  self.assertTrue(other.exists());sealed.chmod(0o700)


 def test_legacy_wayland_endpoint_is_empty_only_and_restores_prior_placeholder(self):
  systemd,other=self.sealed();wayland=self.root/'run/termux-jobs-wayland';wayland.mkdir();(wayland/'keep').write_text('must survive');wayland.chmod(0)
  plan=self.r.plan('archlinux');self.assertEqual({m.SEALED_SYSTEMD_DIRECTORY,m.LEGACY_WAYLAND_DIRECTORY},set(plan['unscanned']))
  with self.assertRaisesRegex(m.Refused,'SEALED_DIRECTORY_NOT_EMPTY'):self.r.remove('archlinux',plan['token'])
  self.assertTrue(self.source.is_dir());self.assertEqual(0,systemd.stat().st_mode&0o777);self.assertEqual(0,wayland.stat().st_mode&0o777)
  self.assertEqual('neighbour',(other/'keep').read_text());wayland.chmod(0o700);self.assertEqual('must survive',(wayland/'keep').read_text())
  (wayland/'keep').unlink();wayland.chmod(0);plan=self.r.plan('archlinux')
  with patch.object(m.os,'chmod',side_effect=AssertionError('must not chmod')),patch.object(m.os,'fchmod',side_effect=AssertionError('must not chmod')):self.r.remove('archlinux',plan['token'])
  self.assertFalse(self.source.exists());self.assertEqual('neighbour',(other/'keep').read_text())
 def test_legacy_wayland_symlink_never_follows_or_changes_bind_source(self):
  sealed,other=self.sealed();outside=self.base/'runtime';outside.mkdir();(outside/'keep').write_text('keep')
  (self.root/'run/termux-jobs-wayland').symlink_to(outside)
  plan=self.r.plan('archlinux');self.assertNotIn(m.LEGACY_WAYLAND_DIRECTORY,plan['unscanned'])
  self.r.remove('archlinux',plan['token']);self.assertEqual('keep',(outside/'keep').read_text())
 def test_readable_wayland_contents_are_scanned_and_not_excluded(self):
  sealed,other=self.sealed();wayland=self.root/'run/termux-jobs-wayland';wayland.mkdir();(wayland/'file').write_text('payload')
  plan=self.r.plan('archlinux');self.assertNotIn(m.LEGACY_WAYLAND_DIRECTORY,plan['unscanned']);self.assertGreaterEqual(plan['bytes'],7)
  (wayland/'file').write_text('changed')
  with self.assertRaisesRegex(m.Refused,'SOURCE_CHANGED'):self.r.remove('archlinux',plan['token'])
  self.assertTrue(self.source.exists());sealed.chmod(0o700)

class RemovedMigrationContract(unittest.TestCase):
 def test_no_rootfs_clone_or_preparation_api(self):
  self.assertFalse(hasattr(m.Runtime,'clone'))
  self.assertFalse(hasattr(m.Runtime,'copy'))
  self.assertFalse(hasattr(m.Runtime,'prepare_empty_workspace'))
