"""Real fd-relative filesystem/lock operations, exclusively under the repository."""
import importlib.util,fcntl,os,tempfile,unittest,json
from pathlib import Path
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[2]
s=importlib.util.spec_from_file_location('agent_environment',ROOT/'app/src/main/assets/agent-environment.py');m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
class MigrationTests(unittest.TestCase):
 def setUp(self):
  self.tmp=tempfile.TemporaryDirectory(dir=ROOT/'.verification');self.base=Path(self.tmp.name);self.source=self.base/'containers/ubuntu';self.root=self.source/'rootfs'
  for n in ['etc','var/lib/dpkg','root/projects','usr/bin']:(self.root/n).mkdir(parents=True)
  (self.source/'manifest.json').write_text('{"image_config":{"config":{}}}')
  (self.root/'etc/os-release').write_text('ID=ubuntu\n');(self.root/'var/lib/dpkg/status').write_text('Package: fixture\nStatus: install ok installed\n')
  self.secret=self.root/'root/projects/key';self.secret.write_bytes(b'synthetic-fixture-not-a-real-key');self.secret.chmod(0o600)
  self.r=m.Runtime(str(self.base),progress=lambda **kw:None)
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
 def test_android_mount_directories_are_empty_placeholders_but_nearby_user_data_copies(self):
  for name in ['apex','data/app','data/dalvik-cache','sdcard','system','storage','dev','proc','sys','mnt/sdcard']:
   folder=self.root/name;folder.mkdir(parents=True,exist_ok=True);(folder/'host-only').write_text('external fixture')
  (self.root/'root/projects/apex').mkdir();(self.root/'root/projects/apex/keep').write_text('project')
  token=self.token();self.r.clone(token);dest=self.base/'containers/ubuntu-ai/rootfs'
  for name in ['apex','data/app','data/dalvik-cache','sdcard','system','storage','dev','proc','sys','mnt/sdcard']:self.assertEqual([],os.listdir(dest/name))
  self.assertEqual('project',(dest/'root/projects/apex/keep').read_text());self.assertEqual('external fixture',(self.root/'apex/host-only').read_text())
 def test_denied_android_mount_open_is_never_attempted_but_other_denials_fail(self):
  (self.root/'apex').mkdir();original=m.os.open
  def opening(name,*args,**kwargs):
   if name=='apex' and os.fstat(kwargs['dir_fd']).st_ino==self.root.stat().st_ino:raise PermissionError(13,'fixture')
   return original(name,*args,**kwargs)
  with patch.object(m.os,'open',side_effect=opening):self.r.clone(self.token())
  self.assertEqual([],os.listdir(self.base/'containers/ubuntu-ai/rootfs/apex'))
 def test_app_mounts_skip_only_exact_directories_and_preserve_other_tmp_data(self):
  names=['termux-jobs-x11-gpu','termux-fork-session-001136f4-58c5-44b1-94da-4690fd10ec7f','termux-jobs-setup','termux-jobs-gemini','termux-jobs-claude','termux-jobs-ai','termux-jobs-codex']
  tmp=self.root/'tmp';tmp.mkdir()
  for name in names:(tmp/name).mkdir();(tmp/name/'external-fixture').write_text('do not clone bind payload')
  for name in ['termux-fork-session-not-a-uuid','termux-jobs-codex-project','project']:(tmp/name).mkdir();(tmp/name/'keep').write_text('keep')
  token=self.token();self.r.clone(token);dest=self.base/'containers/ubuntu-ai/rootfs/tmp'
  for name in names:self.assertEqual([],os.listdir(dest/name));self.assertTrue((tmp/name/'external-fixture').exists())
  for name in ['termux-fork-session-not-a-uuid','termux-jobs-codex-project','project']:self.assertEqual('keep',(dest/name/'keep').read_text())
 def test_denied_session_mount_not_opened_but_unrelated_denied_tmp_still_fails(self):
  tmp=self.root/'tmp';tmp.mkdir();name='termux-fork-session-001136f4-58c5-44b1-94da-4690fd10ec7f';(tmp/name).mkdir();original=m.os.open
  def opening(n,*args,**kwargs):
   if n==name and os.fstat(kwargs['dir_fd']).st_ino==tmp.stat().st_ino:raise PermissionError(13,'fixture')
   return original(n,*args,**kwargs)
  with patch.object(m.os,'open',side_effect=opening):self.r.clone(self.token())
  (tmp/'private-project').mkdir()
  def blocked(n,*args,**kwargs):
   if n=='private-project':raise PermissionError(13,'fixture')
   return original(n,*args,**kwargs)
  with patch.object(m.os,'open',side_effect=blocked),self.assertRaises(PermissionError):self.token()
 def test_migration_preparation_removes_only_empty_mode_zero_workspace(self):
  workspace=self.root/'workspace';workspace.mkdir();workspace.chmod(0)
  plan=self.r.plan(prepare=True);self.assertTrue(plan['emptyWorkspaceRemoved']);self.assertFalse(workspace.exists());self.r.clone(plan['token']);self.assertTrue(self.secret.is_file())
 def test_workspace_preparation_preserves_nonempty_normal_and_symlink_objects(self):
  root=os.open(self.root,m.DIR);workspace=self.root/'workspace'
  try:
   workspace.mkdir();self.assertFalse(self.r.prepare_empty_workspace(root));(workspace/'keep').write_text('keep');workspace.chmod(0);self.assertFalse(self.r.prepare_empty_workspace(root));workspace.chmod(0o700);self.assertEqual('keep',(workspace/'keep').read_text());(workspace/'keep').unlink();workspace.rmdir()
   target=self.root/'target';target.mkdir();workspace.symlink_to(target);self.assertFalse(self.r.prepare_empty_workspace(root));self.assertTrue(workspace.is_symlink());self.assertTrue(target.is_dir())
  finally:os.close(root)
 def test_workspace_preparation_needs_migration_flag_lock_and_no_cancellation(self):
  workspace=self.root/'workspace';workspace.mkdir();self.r.plan();self.assertTrue(workspace.exists());workspace.chmod(0)
  fd=os.open(self.base/'locks/ubuntu.lock',os.O_RDWR);fcntl.flock(fd,fcntl.LOCK_SH|fcntl.LOCK_NB)
  try:
   with self.assertRaisesRegex(m.Refused,'BUSY'):self.r.plan(prepare=True)
   self.assertTrue(workspace.exists())
  finally:os.close(fd)
  self.r.cancel=lambda:True
  with self.assertRaises(m.Cancelled):self.r.plan(prepare=True)
  self.assertTrue(workspace.exists())
 def test_native_xattr_and_hardlink_fallbacks_preserve_metadata(self):
  source=self.root/'root/projects/extra';source.write_bytes(b'fixture')
  a=os.open(source,os.O_RDONLY);b=os.open(self.root/'root/projects/copy',os.O_CREAT|os.O_RDWR,0o600)
  try:
   if hasattr(os,'setxattr'):os.setxattr(a,'user.jobs-test',b'fixture-value')
   with patch.object(m.os,'listxattr',None,create=True):m.copy_xattrs(a,b)
   if hasattr(os,'getxattr'):self.assertEqual(b'fixture-value',os.getxattr(b,'user.jobs-test'))
  finally:os.close(a);os.close(b)
  with patch.object(m.os,'link',None,create=True):m.hard_link(source,source.with_name('linked'))
  self.assertEqual(source.stat().st_ino,source.with_name('linked').stat().st_ino)
 def test_native_hardlink_fallback_never_replaces_an_existing_file(self):
  target=self.root/'root/projects/existing';target.write_bytes(b'keep')
  with patch.object(m.os,'link',None,create=True),self.assertRaises(OSError):m.hard_link(self.secret,target)
  self.assertEqual(b'keep',target.read_bytes())
 def test_whole_copy_preserves_private_files_modes_hardlinks_and_l2s_but_not_external_data(self):
  m.hard_link(self.secret,self.secret.with_name('hard'));(self.root/'usr/bin/tool').write_text('tool');(self.root/'usr/bin/tool').chmod(0o755)
  (self.root/'root/projects/l2s').symlink_to(self.secret)
  outside=self.base/'external';outside.mkdir();(outside/'untouched').write_text('outside')
  (self.root/'sdcard').symlink_to(outside)
  before=self.token();result=self.r.clone(before);dest=self.base/'containers/ubuntu-ai/rootfs'
  self.assertEqual('done',result['event']);self.assertEqual(before,self.token())
  self.assertEqual(self.secret.read_bytes(),(dest/'root/projects/key').read_bytes());self.assertEqual(0o600,(dest/'root/projects/key').stat().st_mode&0o777);self.assertNotEqual(self.secret.stat().st_ino,(dest/'root/projects/key').stat().st_ino)
  self.assertEqual((dest/'root/projects/key').stat().st_ino,(dest/'root/projects/hard').stat().st_ino)
  self.assertEqual(str(dest/'root/projects/key'),os.readlink(dest/'root/projects/l2s'))
  self.assertTrue((dest/'sdcard').is_symlink());self.assertEqual('outside',(outside/'untouched').read_text())
  self.assertEqual(0o755,(dest/'usr/bin/tool').stat().st_mode&0o777)
 def test_live_shared_native_lock_blocks_plan_clone_and_remove(self):
  token=self.token();fd=os.open(self.base/'locks/ubuntu.lock',os.O_RDWR);fcntl.flock(fd,fcntl.LOCK_SH|fcntl.LOCK_NB)
  try:
   for action in [self.r.plan,lambda:self.r.clone(token),lambda:self.r.remove('ubuntu',token)]:
    with self.assertRaisesRegex(m.Refused,'BUSY'):action()
  finally:os.close(fd)
  self.assertTrue(self.secret.is_file())
 def test_cancel_removes_only_unpublished_stage(self):
  token=self.token();self.r.cancel=lambda:any((self.base/'containers').glob('.jobs-ai-*'))
  with self.assertRaises(m.Cancelled):self.r.clone(token)
  self.assertEqual(['ubuntu'],os.listdir(self.base/'containers'));self.assertTrue(self.secret.exists())
 def test_changed_source_between_confirmation_and_copy_is_refused(self):
  token=self.token();self.secret.write_text('changed')
  with self.assertRaisesRegex(m.Refused,'SOURCE_CHANGED'):self.r.clone(token)
  self.assertFalse((self.base/'containers/ubuntu-ai').exists())
 def test_changed_source_during_copy_never_publishes(self):
  token=self.token();original=self.r.copy
  def copy(*args):original(*args);self.secret.write_text('changed')
  with patch.object(self.r,'copy',side_effect=copy),self.assertRaisesRegex(m.Refused,'SOURCE_CHANGED'):self.r.clone(token)
  self.assertEqual(['ubuntu'],os.listdir(self.base/'containers'))
 def test_existing_destination_and_atomic_race_cannot_be_overwritten(self):
  token=self.token();original=m.rename_new
  def race(parent,old,new):os.mkdir(new,dir_fd=parent);original(parent,old,new)
  with patch.object(m,'rename_new',side_effect=race),self.assertRaises(OSError):self.r.clone(token)
  self.assertEqual([],os.listdir(self.base/'containers/ubuntu-ai'))
  with self.assertRaisesRegex(m.Refused,'DESTINATION_EXISTS'):self.r.clone(token)
 def test_source_link_is_not_followed_and_existing_target_link_is_not_overwritten(self):
  token=self.token();(self.base/'containers/ubuntu-ai').symlink_to(self.root)
  with self.assertRaisesRegex(m.Refused,'DESTINATION_EXISTS'):self.r.clone(token)
  (self.base/'containers/ubuntu-ai').unlink();self.source.rename(self.base/'original');self.source.symlink_to(self.base/'original',target_is_directory=True)
  with self.assertRaises(OSError):self.r.clone(token)
  self.assertTrue((self.base/'original/rootfs/root/projects/key').exists())
 def test_not_enough_space_keeps_source_and_has_no_stage(self):
  token=self.token()
  from types import SimpleNamespace
  with patch.object(m.os,'fstatvfs',return_value=SimpleNamespace(f_bavail=0,f_frsize=1)),self.assertRaisesRegex(m.Refused,'NOT_ENOUGH_SPACE'):self.r.clone(token)
  self.assertEqual(['ubuntu'],os.listdir(self.base/'containers'))
 def test_os_release_guest_symlink_is_allowed_without_host_resolution(self):
  (self.root/'etc/os-release').unlink();(self.root/'etc/os-release').symlink_to('../usr/lib/os-release');self.r.clone(self.token())
  self.assertEqual('../usr/lib/os-release',os.readlink(self.base/'containers/ubuntu-ai/rootfs/etc/os-release'))
 def test_missing_database_and_special_file_fail_without_publishing(self):
  (self.root/'var/lib/dpkg/status').unlink();token=self.token()
  with self.assertRaises(OSError):self.r.clone(token)
  self.assertEqual(['ubuntu'],os.listdir(self.base/'containers'))
  os.mkfifo(self.root/'root/pipe')
  with self.assertRaisesRegex(m.Refused,'SPECIAL'):self.token()
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

 def test_failed_final_verification_cleans_read_only_destination_directories(self):
  token=self.token();original=self.r.copy
  def copy(*args):
   original(*args)
   parent,name=m.at(args[1],'rootfs/root/projects')
   try:os.chmod(name,0o555,dir_fd=parent)
   finally:os.close(parent)
   self.secret.chmod(0o644)
  with patch.object(self.r,'copy',side_effect=copy),self.assertRaisesRegex(m.Refused,'SOURCE_CHANGED'):self.r.clone(token)
  self.assertEqual(['ubuntu'],os.listdir(self.base/'containers'))
 def test_failed_deletion_reports_quarantined_remainder_without_touching_other_system(self):
  token=self.token();other=self.base/'containers/debian';other.mkdir();(other/'keep').write_text('keep')
  with patch.object(m,'remove_tree',side_effect=PermissionError()),self.assertRaisesRegex(m.Refused,'DELETE_INCOMPLETE'):self.r.remove('ubuntu',token)
  self.assertFalse(self.source.exists());self.assertEqual('keep',(other/'keep').read_text());hidden=list((self.base/'containers').glob('.jobs-delete-*'));self.assertEqual(1,len(hidden));self.assertTrue((hidden[0]/'rootfs/root/projects/key').exists())
 def test_cleanup_can_open_sealed_directories_without_touching_link_targets(self):
  name='.jobs-delete-'+'a'*32;trash=self.base/name;sealed=trash/'sealed';sealed.mkdir(parents=True);(sealed/'payload').write_text('delete')
  outside=self.base/'outside';outside.mkdir();kept=outside/'keep';kept.write_text('keep');outside.chmod(0o500);(trash/'link').symlink_to(outside)
  m.hard_link(kept,trash/'hard');kept.chmod(0o600);sealed.chmod(0);trash.chmod(0)
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
  self.r.clone(self.token());fd=os.open(self.base/'locks/ubuntu-ai.lock',os.O_RDWR);fcntl.flock(fd,fcntl.LOCK_SH|fcntl.LOCK_NB)
  try:self.r.remove('ubuntu',self.token())
  finally:os.close(fd)
  self.assertFalse(self.source.exists());self.assertTrue((self.base/'containers/ubuntu-ai/rootfs/root/projects/key').exists())
