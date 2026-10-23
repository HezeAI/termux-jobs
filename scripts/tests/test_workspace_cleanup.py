import importlib.util
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import time
import unittest
from unittest.mock import patch

sys.dont_write_bytecode=True
ROOT=Path(__file__).resolve().parents[2]
def module(name,path):
    spec=importlib.util.spec_from_file_location(name,path);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m
b=module('backup_cleanup_fixture',ROOT/'app/src/main/assets/workspace-backup.py')
c=module('cleanup_fixture',ROOT/'app/src/main/assets/workspace-cleanup.py')

class CleanupTests(unittest.TestCase):
    def setUp(self):
        parent=ROOT/'.verification/storage-cleanup-v1.7.2/fixtures';parent.mkdir(parents=True,exist_ok=True)
        self.temp=tempfile.TemporaryDirectory(dir=parent);self.root=Path(self.temp.name)
        for name in ['usr','home','files','downloads']:(self.root/name).mkdir()
        self.request=dict(prefix=str(self.root/'usr'),home=str(self.root/'home'),files=str(self.root/'files'),archiveRoot=str(self.root/'downloads'),packageId='com.termux')
        self.progress=patch.object(b,'progress');self.progress.start()
    def tearDown(self):self.progress.stop();self.temp.cleanup()
    def write(self,relative,value='data'):
        path=self.root/relative;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(value);return path
    def scan(self):return c.inventory(self.request,b)
    def run_groups(self,plan,groups):return c.execute(dict(self.request,plan=plan['plan'],selected=[g['id'] for g in groups],acknowledged=True),b)
    def backup(self,name='Termux-Jobs-20261019-230000-aabbccdd'):
        folder=self.root/'downloads'/name;folder.mkdir()
        (folder/'manifest.json').write_text(json.dumps(dict(format='termux-jobs-backup',version=2,source=dict(packageId='com.termux'),components=[])))
        (folder/'data.tar.gz').write_bytes(b'archive');return folder
    def prepare(self,scope,entries):return c.prepare_home(dict(self.request,scope=scope,entries=entries),b)
    def test_overview_shows_home_prefix_and_environment_without_following_links(self):
        self.write('home/projects/code.txt','source');outside=self.write('personal/secret','S'*100)
        (self.root/'home/storage').symlink_to(outside.parent,target_is_directory=True)
        self.write('usr/bin/python3','runtime')
        self.write('usr/var/lib/proot-distro/containers/debian/rootfs/root/a.txt','guest')
        self.write('usr/var/lib/proot-distro/containers/debian/rootfs/etc/os-release','ID=debian')
        overview=self.scan() # legacy callers do not trigger the expensive overview
        self.assertNotIn('overview',overview)
        overview=c.inventory(dict(self.request,overview=True),b)['overview']
        home=next(e for e in overview if e['kind']=='home' and e['scope']=='termux')
        link=next(e for e in home['entries'] if e['name']=='storage')
        self.assertTrue(link['link']);self.assertEqual(1,link['count']);self.assertEqual(len(os.fsencode(str(outside.parent))),link['bytes'])
        prefix=next(e for e in overview if e['kind']=='system' and e['scope']=='termux')
        self.assertEqual(len('runtime'),prefix['bytes'])
        self.assertTrue(any(e['kind']=='home' and e['scope']=='debian' for e in overview))
        self.assertTrue(outside.exists())
    def test_home_selection_removes_only_chosen_data_and_link_not_target(self):
        chosen=self.write('home/projects/code.txt');kept=self.write('home/keep.txt')
        external=self.write('personal/secret.txt');(self.root/'home/storage').symlink_to(external.parent,target_is_directory=True)
        entries=next(e for e in c.overview(self.request,b) if e['kind']=='home')['entries']
        plan=self.prepare('termux',[e for e in entries if e['name'] in ('projects','storage')])
        result=self.run_groups(plan,plan['groups']);self.assertEqual([],result['errors'])
        self.assertFalse(chosen.exists());self.assertFalse((self.root/'home/projects').exists());self.assertFalse((self.root/'home/storage').is_symlink());self.assertTrue(external.exists());self.assertTrue(kept.exists());self.assertTrue((self.root/'home').is_dir())
    def test_home_prepare_refuses_changed_files_and_path_traversal(self):
        f=self.write('home/a.txt');entries=next(e for e in c.overview(self.request,b) if e['kind']=='home')['entries'];f.write_text('changed')
        with self.assertRaisesRegex(b.BackupError,'CLEANUP_CHANGED_RESCAN'):self.prepare('termux',entries)
        for name in ['../usr','/sdcard','.','a/b','']:
            with self.assertRaisesRegex(b.BackupError,'CLEANUP_PLAN_INVALID'):self.prepare('termux',[dict(name=name,identity=[0,0,0,0])])
        with self.assertRaisesRegex(b.BackupError,'CLEANUP_PLAN_INVALID'):self.prepare('prefix',entries)
        self.assertTrue(f.exists())
    def test_home_execute_refuses_changed_child_before_deleting_other_files(self):
        a=self.write('home/p/a');z=self.write('home/p/z')
        entries=next(e for e in c.overview(self.request,b) if e['kind']=='home')['entries'];plan=self.prepare('termux',entries);z.write_text('changed!')
        result=self.run_groups(plan,plan['groups']);self.assertEqual('CLEANUP_CHANGED_RESCAN',result['errors'][0]['code']);self.assertTrue(a.exists());self.assertTrue(z.exists())
    def test_guest_home_uses_its_native_container_lock(self):
        f=self.write('usr/var/lib/proot-distro/containers/debian/rootfs/home/u/a')
        overview=c.overview(self.request,b);entries=next(e for e in overview if e['scope']=='debian/home')['entries'];plan=self.prepare('debian/home',entries)
        with patch.object(b,'locked_remove_available',return_value=True):result=self.run_groups(plan,plan['groups'])
        self.assertEqual([],result['errors']);self.assertFalse(f.exists());self.assertTrue((self.root/'usr/var/lib/proot-distro/containers/debian/rootfs/home').is_dir())
    def test_home_special_files_block_prepare_without_deleting_regular_files(self):
        f=self.write('home/p/a');os.mkfifo(self.root/'home/p/pipe')
        entries=next(e for e in c.overview(self.request,b) if e['kind']=='home')['entries']
        with self.assertRaisesRegex(b.BackupError,'UNSAFE_LINK'):self.prepare('termux',entries)
        self.assertTrue(f.exists())

    def test_large_home_snapshot_exceeds_old_limits_and_deletes_sparse_40gib(self):
        folder=self.root/'home/large';folder.mkdir()
        # Real directory cardinality and >8 MiB manifest, without using 40 GiB
        # of phone storage: truncate creates a sparse fixture inside the repo.
        with patch.object(b,'progress',lambda *a,**k:None):
            for i in range(60005):
                (folder/(str(i).zfill(6)+'x'*90)).touch()
            sparse=folder/'large.bin'
            with sparse.open('wb') as out:out.truncate(40*1024**3)
            kept=self.write('home/keep.txt');external=self.write('personal/keep.txt')
            (folder/'external').symlink_to(external.parent,target_is_directory=True)
            entry=dict(name=folder.name,identity=c.identity(folder.lstat()))
            plan=self.prepare('termux',[entry]);group=plan['groups'][0]
            self.assertEqual(60007,group['count']);self.assertGreaterEqual(group['bytes'],40*1024**3)
            private=self.root/'files/workspace-cleanup-plans'
            raw=json.loads((private/(plan['plan']+'.json')).read_text())
            meta=raw['groups'][0]['spool']['files']
            self.assertGreater((private/meta['name']).stat().st_size,8*1024**2)
            self.assertLess((private/(plan['plan']+'.json')).stat().st_size,4096)
            result=self.run_groups(plan,plan['groups'])
            self.assertEqual([],result['errors']);self.assertFalse(folder.exists())
            self.assertTrue(kept.exists());self.assertTrue(external.exists())
            self.assertFalse(list(private.iterdir()))

    def test_disk_snapshot_replacement_is_rejected_before_any_unlink(self):
        target=self.write('home/p/a');entries=next(e for e in c.overview(self.request,b) if e['kind']=='home')['entries']
        plan=self.prepare('termux',entries);private=self.root/'files/workspace-cleanup-plans'
        raw=json.loads((private/(plan['plan']+'.json')).read_text());path=private/raw['groups'][0]['spool']['files']['name']
        old=path.with_suffix('.old');path.rename(old);path.write_bytes(old.read_bytes())
        result=self.run_groups(plan,plan['groups'])
        self.assertEqual('CLEANUP_CHANGED_RESCAN',result['errors'][0]['code']);self.assertTrue(target.exists())

    def test_failed_disk_scan_cleans_private_lists_and_retains_user_files(self):
        target=self.write('home/p/a');os.mkfifo(self.root/'home/p/pipe')
        entry=dict(name='p',identity=c.identity((self.root/'home/p').lstat()))
        with self.assertRaisesRegex(b.BackupError,'UNSAFE_LINK'):self.prepare('termux',[entry])
        self.assertFalse(list((self.root/'files/workspace-cleanup-plans').iterdir()));self.assertTrue(target.exists())

    def test_disk_directory_reverse_stream_crosses_blocks(self):
        folder=self.root/'home/p';folder.mkdir()
        for i in range(1200):(folder/(str(i)+'d'*65)).mkdir()
        with patch.object(b,'progress',lambda *a,**k:None):
            plan=self.prepare('termux',[dict(name='p',identity=c.identity(folder.lstat()))])
            result=self.run_groups(plan,plan['groups'])
        self.assertEqual([],result['errors']);self.assertFalse(folder.exists())

    def test_app_histories_are_individually_selected_without_touching_native_chats(self):
        chosen=self.write('files/workspace-sessions/session-a/transcript.txt','saved shell')
        kept=self.write('files/workspace-sessions/session-b/transcript.txt','keep')
        native=self.write('home/.codex/sessions/chat.jsonl','native chat')
        history=next(e for e in c.overview(self.request,b) if e['kind']=='history')
        plan=self.prepare('app-history',[e for e in history['entries'] if e['name']=='session-a'])
        self.assertEqual(['history:session-a'],plan['groups'][0]['busyScopes'])
        result=self.run_groups(plan,plan['groups']);self.assertEqual([],result['errors']);self.assertFalse(chosen.exists());self.assertTrue(kept.exists());self.assertTrue(native.exists())

    def environment_plan(self):
        self.write('usr/var/lib/proot-distro/containers/alpine/rootfs/etc/os-release','ID=alpine')
        result=c.inventory(dict(self.request,overview=True),b)
        return result,next(g for g in result['groups'] if g['kind']=='environment')
    def test_whole_environment_uses_only_exact_native_remove_and_preserves_siblings(self):
        import shutil
        plan,group=self.environment_plan();sibling=self.write('usr/var/lib/proot-distro/containers/debian/rootfs/root/keep');archive=self.backup();home=self.write('home/keep')
        def native(args):
            self.assertEqual([str(self.root/'usr/bin/proot-distro'),'remove','alpine'],args)
            shutil.rmtree(self.root/'usr/var/lib/proot-distro/containers/alpine')
        with patch.object(b,'locked_remove_available',return_value=True),patch.object(b,'run_tool',side_effect=native) as run:result=self.run_groups(plan,[group])
        self.assertEqual(['alpine'],result['removedEnvironments']);self.assertEqual([],result['errors']);self.assertEqual(1,run.call_count);self.assertTrue(sibling.exists());self.assertTrue(archive.exists());self.assertTrue(home.exists())
    def test_native_remove_failure_never_falls_back_or_stops_a_session(self):
        plan,group=self.environment_plan();root=Path(group['path']);keep=root/'etc/os-release'
        with patch.object(b,'locked_remove_available',return_value=True),patch.object(b,'run_tool',side_effect=b.BackupError('PROOT_DISTRO_FAILED')) as run:result=self.run_groups(plan,[group])
        self.assertEqual('CLEANUP_ENVIRONMENT_REMOVE_FAILED',result['errors'][0]['code']);self.assertEqual([],result['removedEnvironments']);self.assertTrue(keep.exists());self.assertEqual(1,run.call_count)
    def test_whole_environment_replacement_is_rejected_before_native_command(self):
        plan,group=self.environment_plan();old=Path(group['path']).parent;old.rename(old.with_name('saved-alpine'));(old/'rootfs').mkdir(parents=True)
        with patch.object(b,'locked_remove_available',return_value=True),patch.object(b,'run_tool') as run:result=self.run_groups(plan,[group])
        self.assertEqual('CLEANUP_CHANGED_RESCAN',result['errors'][0]['code']);run.assert_not_called();self.assertTrue((old.with_name('saved-alpine')/'rootfs/etc/os-release').exists())
    def test_whole_environment_requires_supported_native_locks(self):
        plan,group=self.environment_plan()
        with patch.object(b,'locked_remove_available',return_value=False),patch.object(b,'run_tool') as run:result=self.run_groups(plan,[group])
        self.assertEqual('RESTORED_LOCKS_REQUIRED',result['errors'][0]['code']);run.assert_not_called();self.assertTrue(Path(group['path']).exists())
    def test_whole_environment_and_nested_cache_cannot_be_selected_together(self):
        self.write('usr/var/lib/proot-distro/containers/alpine/rootfs/var/cache/apk/tree.apk')
        plan,group=self.environment_plan();cache=next(g for g in plan['groups'] if g['kind']=='cache')
        with patch.object(b,'run_tool') as run:
            with self.assertRaisesRegex(b.BackupError,'CLEANUP_OVERLAPPING_SELECTION'):self.run_groups(plan,[group,cache])
        run.assert_not_called();self.assertTrue(Path(group['path']).exists())

    def test_scan_only_offers_disposable_files_not_indexes_projects_or_accounts(self):
        deb=self.write('usr/var/cache/apt/archives/tree.deb')
        self.write('usr/var/cache/apt/archives/lock','')
        self.write('usr/var/cache/apt/archives/partial/current.deb')
        self.write('usr/var/lib/proot-distro/containers/alpine/rootfs/var/cache/apk/tree.apk')
        index=self.write('usr/var/lib/proot-distro/containers/alpine/rootfs/var/cache/apk/APKINDEX.tar.gz')
        auth=self.write('home/.codex/auth.json','secret');project=self.write('home/projects/source.txt')
        self.write('files/workspace-diagnostics/session.txt')
        plan=self.scan();self.assertEqual({'cache','reports'},{g['kind'] for g in plan['groups']})
        self.assertEqual(2,len([g for g in plan['groups'] if g['kind']=='cache']))
        group=next(g for g in plan['groups'] if g['scope']=='termux');self.assertEqual(1,group['count'])
        self.assertEqual(1,self.run_groups(plan,[group])['files']);self.assertFalse(deb.exists());self.assertTrue(index.exists());self.assertEqual('secret',auth.read_text());self.assertTrue(project.exists())
    def test_consent_and_nonempty_selection_are_enforced_by_backend(self):
        target=self.write('files/workspace-diagnostics/test.txt');plan=self.scan()
        for ack in [False,None,1,'true']:
            with self.assertRaisesRegex(b.BackupError,'CLEANUP_CONSENT_REQUIRED'):c.execute(dict(self.request,plan=plan['plan'],selected=[plan['groups'][0]['id']],acknowledged=ack),b)
        with self.assertRaisesRegex(b.BackupError,'CLEANUP_SELECTION_REQUIRED'):self.run_groups(plan,[])
        self.assertTrue(target.exists())
    def test_unselected_files_and_backups_are_retained(self):
        selected=self.write('files/workspace-diagnostics/test.txt');archive=self.backup();plan=self.scan();group=next(g for g in plan['groups'] if g['kind']=='reports')
        self.run_groups(plan,[group]);self.assertFalse(selected.exists());self.assertTrue(archive.is_dir());self.assertTrue((archive/'data.tar.gz').is_file())
    def test_backup_folder_removed_only_after_identification_and_explicit_selection(self):
        archive=self.backup();other=self.write('downloads/personal/notes.txt');unknown=self.root/'downloads/Termux-Jobs-20261019-230001-11223344';unknown.mkdir();(unknown/'INCOMPLETE').write_text('unknown')
        plan=self.scan();self.assertEqual(1,len(plan['groups']));self.assertEqual('backup',plan['groups'][0]['kind'])
        result=self.run_groups(plan,plan['groups']);self.assertEqual([],result['errors']);self.assertFalse(archive.exists());self.assertTrue(other.exists());self.assertTrue(unknown.exists())
    def test_partial_backup_needs_private_inode_receipt(self):
        archive=self.root/'downloads/Termux-Jobs-20261019-230000-aabbccdd';archive.mkdir();(archive/'INCOMPLETE').write_text('partial');self.assertEqual([],self.scan()['groups'])
        receipts=self.root/'files/workspace-backup-ownership';receipts.mkdir(exist_ok=True);st=archive.stat();(receipts/(archive.name+'.json')).write_text(json.dumps(dict(path=str(archive),identity=[st.st_dev,st.st_ino])))
        plan=self.scan();self.assertEqual('partial',plan['groups'][0]['kind']);self.run_groups(plan,plan['groups']);self.assertFalse(archive.exists())
    def test_changed_file_causes_group_to_be_skipped_before_first_unlink(self):
        a=self.write('files/workspace-diagnostics/a.txt');z=self.write('files/workspace-diagnostics/z.txt');plan=self.scan();z.write_text('changed')
        result=self.run_groups(plan,plan['groups']);self.assertEqual(0,result['files']);self.assertEqual('CLEANUP_CHANGED_RESCAN',result['errors'][0]['code']);self.assertTrue(a.exists());self.assertTrue(z.exists())
    def test_links_and_replaced_root_do_not_delete_external_data(self):
        external=self.write('personal/secret.txt');self.write('files/workspace-diagnostics/a.txt');(self.root/'files/workspace-diagnostics/link.txt').symlink_to(external)
        plan=self.scan();folder=self.root/'files/workspace-diagnostics';folder.rename(folder.with_name('old-reports'));folder.symlink_to(external.parent,target_is_directory=True)
        result=self.run_groups(plan,plan['groups']);self.assertEqual(0,result['files']);self.assertTrue(external.is_file());self.assertTrue((self.root/'files/old-reports/a.txt').is_file())
    def test_busy_native_environment_lock_refuses_cleanup_without_stopping_holder(self):
        target=self.write('usr/var/lib/proot-distro/containers/alpine/rootfs/var/cache/apk/tree.apk');plan=self.scan();lock=self.root/'usr/var/lib/proot-distro/locks/alpine.lock';lock.parent.mkdir(parents=True)
        code="import fcntl,sys;f=open(sys.argv[1],'a');fcntl.flock(f,fcntl.LOCK_SH);print('ready',flush=True);sys.stdin.read()"
        child=subprocess.Popen([sys.executable,'-c',code,str(lock)],stdin=subprocess.PIPE,stdout=subprocess.PIPE,text=True)
        try:
            self.assertEqual('ready',child.stdout.readline().strip())
            with patch.object(b,'locked_remove_available',return_value=True):result=self.run_groups(plan,plan['groups'])
            self.assertEqual('CLEANUP_ENVIRONMENT_BUSY',result['errors'][0]['code']);self.assertTrue(target.exists());self.assertIsNone(child.poll())
        finally:child.stdin.close();child.wait(timeout=5);child.stdout.close()
    def test_busy_apt_lock_refuses_termux_cleanup(self):
        target=self.write('usr/var/cache/apt/archives/tree.deb');lock=self.write('usr/var/cache/apt/archives/lock','');plan=self.scan()
        code="import fcntl,sys;f=open(sys.argv[1],'r+');fcntl.lockf(f,fcntl.LOCK_EX);print('ready',flush=True);sys.stdin.read()"
        child=subprocess.Popen([sys.executable,'-c',code,str(lock)],stdin=subprocess.PIPE,stdout=subprocess.PIPE,text=True)
        try:
            self.assertEqual('ready',child.stdout.readline().strip());result=self.run_groups(plan,plan['groups']);self.assertEqual('CLEANUP_ENVIRONMENT_BUSY',result['errors'][0]['code']);self.assertTrue(target.exists())
        finally:child.stdin.close();child.wait(timeout=5);child.stdout.close()
    def test_cancellation_does_not_continue_with_other_groups(self):
        self.write('files/workspace-diagnostics/a.txt');archive=self.backup();plan=self.scan();groups=sorted(plan['groups'],key=lambda g:g['kind']!='reports')
        def cancel(stage,*args,**kw):
            if stage=='CLEANUP_DELETE':raise b.BackupError('CANCELLED')
        with patch.object(b,'progress',side_effect=cancel):
            with self.assertRaisesRegex(b.BackupError,'CANCELLED'):self.run_groups(plan,groups)
        self.assertTrue(archive.exists());self.assertTrue((archive/'data.tar.gz').exists())
    def test_stale_plan_and_arbitrary_selection_cannot_delete(self):
        target=self.write('files/workspace-diagnostics/a.txt');plan=self.scan()
        with self.assertRaisesRegex(b.BackupError,'CLEANUP_PLAN_INVALID'):c.execute(dict(self.request,plan=plan['plan'],selected=['../home'],acknowledged=True),b)
        file=self.root/'files/workspace-cleanup-plans'/(plan['plan']+'.json');meta=json.loads(file.read_text());meta['created']=time.time()-601;file.write_text(json.dumps(meta))
        with self.assertRaisesRegex(b.BackupError,'CLEANUP_PLAN_EXPIRED'):self.run_groups(plan,plan['groups'])
        self.assertTrue(target.exists())
    def test_zypper_and_dnf_cache_archives_not_metadata_are_selected(self):
        for alias,rel in [('fedora','var/cache/libdnf5/updates/packages/tree.rpm'),('opensuse','var/cache/zypp/packages/repo/aarch64/tree.rpm')]:self.write('usr/var/lib/proot-distro/containers/'+alias+'/rootfs/'+rel)
        self.write('usr/var/lib/proot-distro/containers/fedora/rootfs/var/cache/libdnf5/updates/repodata/primary.rpm')
        plan=self.scan();self.assertEqual(2,len(plan['groups']));self.assertEqual([1,1],sorted(g['count'] for g in plan['groups']))
    def test_json_worker_dispatch_loads_cleanup_module_with_isolated_python(self):
        self.write('files/workspace-diagnostics/a.txt');request=dict(self.request,operation='cleanup-inventory');path=self.root/'request.json';path.write_text(json.dumps(request))
        result=subprocess.run([sys.executable,'-I','-S',str(ROOT/'app/src/main/assets/workspace-backup.py'),'--request',str(path)],capture_output=True,text=True,timeout=10)
        self.assertEqual(0,result.returncode,result.stdout);last=json.loads(result.stdout.splitlines()[-1]);self.assertEqual('DONE',last['stage']);self.assertEqual('reports',last['result']['groups'][0]['kind'])

if __name__=='__main__':unittest.main()
