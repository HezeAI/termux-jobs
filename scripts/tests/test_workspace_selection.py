"""Selective-backup tests: isolated repo fixtures, never real HOME/proot or package installation."""
from contextlib import closing
import importlib.util
import json
import os
from pathlib import Path
import sqlite3
import sys
import tarfile
import tempfile
import unittest
from unittest.mock import patch
sys.dont_write_bytecode=True
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('backup',ROOT/'app/src/main/assets/workspace-backup.py')
b=importlib.util.module_from_spec(spec);spec.loader.exec_module(b)


def elf():
    data=bytearray(64);data[:4]=b'\x7fELF';data[4]=2;data[5]=1;data[18:20]=(183).to_bytes(2,'little');return data


class SelectionTests(unittest.TestCase):
    def setUp(self):
        directory=ROOT/'.verification/selective-v1.1.2/fixtures';directory.mkdir(parents=True,exist_ok=True)
        self.temp=tempfile.TemporaryDirectory(dir=directory);self.base=Path(self.temp.name)
        self.files=self.base/'files';self.home=self.files/'home';self.prefix=self.files/'usr';self.home.mkdir(parents=True);self.prefix.mkdir()
        self.work=self.base/'work';self.work.mkdir();self.out=self.base/'out';self.out.mkdir()
        self.guest=self.prefix/'var/lib/proot-distro/containers/ubuntu/rootfs';(self.guest/'root/.codex/sessions').mkdir(parents=True)
        self.put('root/.codex/auth.json',b'{"test":"fixture-only"}');self.put('root/.codex/config.toml',b'# fixture');self.put('root/.codex/sessions/chat.jsonl',b'{}\n')
        self.put('usr/bin/env',elf());self.put('usr/bin/node',elf());self.put('etc/os-release',b'ID=ubuntu\n')
        self.put('root/projects/demo/main.py',b'print(1)');self.put('root/projects/demo/build/out.apk',b'BUILD')
        self.put('root/termux-fork/.tools/tool',b'TOOL');self.put('root/termux-fork/app/source',b'SOURCE')
        self.put('root/.ssh/id_ed25519',b'fixture-not-a-key');self.put('home/other-user/file',b'other user')
        (self.prefix/'bin').mkdir();(self.prefix/'bin/proot-distro').write_text('# proot_distro')
        self.req=dict(home=str(self.home),prefix=str(self.prefix),files=str(self.files),work=str(self.work),packageId='com.termux',appVersion='1.1.1')
    def tearDown(self):self.temp.cleanup()
    def put(self,path,data):
        p=self.guest/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(data);return p
    def create(self,items,folders=()):
        result=b.create(dict(self.req,destination=str(self.out),components=[],environments=[],selections=[dict(scope='ubuntu:ubuntu',items=items,folders=list(folders))]))
        manifest=Path(result['path'])/'manifest.json';return manifest,b.inspect(dict(self.req,path=str(manifest)))
    def test_inventory_disjoint_and_build_tools_not_in_project(self):
        report=b.selection_inventory(self.req);scope=next(s for s in report['scopes'] if s['id']=='ubuntu:ubuntu');items={i['id']:i for i in scope['items']}
        self.assertEqual(items['project:root/termux-fork']['bytes'],len(b'SOURCE'));self.assertEqual(items['builds']['bytes'],len(b'BUILD')+len(b'TOOL'))
        self.assertEqual(sum(i['bytes'] for i in items.values()),sum(p.stat().st_size for p,r in b.selection_walk(scope) if p.is_file() and not p.is_symlink()))
        self.assertTrue(scope['runtime']['node'])
    def test_codex_only_schema2_no_system_projects_or_ssh(self):
        manifest,r=self.create(['codex']);self.assertEqual(json.loads(manifest.read_text())['version'],2);c=r['components'][0];self.assertEqual(c['kind'],'data')
        with tarfile.open(c['file']) as t:
            names=t.getnames();self.assertIn('payload/root/.codex/auth.json',names);self.assertFalse(any('/usr/' in n or '/projects/' in n or '/.ssh/' in n for n in names))
        self.assertTrue(c['requirements']['needsNode']);self.assertTrue(c['requirements']['stagingOnly']);self.assertTrue(c['selection']['historyOnlyWithoutProjects'])
    def test_system_and_codex_is_rootfs_not_other_user_files(self):
        _,r=self.create(['system','codex']);c=r['components'][0];self.assertEqual(c['kind'],'ubuntu');self.assertEqual(c['status'],'compatible')
        with tarfile.open(c['file']) as t:
            names=t.getnames();self.assertIn('ubuntu/rootfs/usr/bin/node',names);self.assertIn('ubuntu/rootfs/root/.codex/auth.json',names)
            self.assertNotIn('ubuntu/rootfs/root/.ssh/id_ed25519',names);self.assertNotIn('ubuntu/rootfs/home/other-user/file',names)
    def test_all_groups_preserve_regular_files_once(self):
        scope=next(s for s in b.selection_inventory(self.req)['scopes'] if s['id']=='ubuntu:ubuntu');ids=[i['id'] for i in scope['items']]
        _,r=self.create(ids)
        with tarfile.open(r['components'][0]['file']) as t:
            names=[m.name.removeprefix('ubuntu/rootfs/') for m in t if m.isfile()];expected=[rel for p,rel in b.selection_walk(scope) if p.is_file() and not p.is_symlink()]
        self.assertCountEqual(names,expected)
    def test_consistent_sqlite_with_uncheckpointed_wal_and_no_runtime_locks(self):
        p=self.guest/'root/.codex/state.sqlite';writer=sqlite3.connect(p)
        try:
            writer.execute('PRAGMA journal_mode=WAL');writer.execute('CREATE TABLE sample(value)');writer.execute('INSERT INTO sample VALUES (17)');writer.commit()
            self.put('root/.codex/thread-writer-locks/lock',b'busy');self.put('root/.codex/tmp/socket-info',b'process')
            _,r=self.create(['codex']);dest=self.base/'read';b.extract_new(r['components'][0]['file'],'payload',dest)
            with closing(sqlite3.connect(dest/'root/.codex/state.sqlite')) as db:self.assertEqual(db.execute('SELECT value FROM sample').fetchone(),(17,))
            self.assertFalse((dest/'root/.codex/state.sqlite-wal').exists());self.assertFalse((dest/'root/.codex/thread-writer-locks').exists());self.assertTrue((p.parent/'state.sqlite-wal').exists())
        finally:writer.close()
    def test_existing_target_and_source_unchanged_restore_is_staging(self):
        manifest,r=self.create(['codex']);auth=self.guest/'root/.codex/auth.json';before=auth.read_bytes()
        result=b.restore(dict(self.req,path=str(manifest),fingerprints=r['fingerprints'],selected=[0],destination=str(self.home)))
        self.assertEqual(auth.read_bytes(),before);self.assertEqual(Path(result['paths']['0'],'root/.codex/auth.json').read_bytes(),before);self.assertEqual(result['environments'],[])
        self.assertTrue(Path(result['path'],'README-SELECTED.txt').exists())
    def test_second_device_missing_environment_or_node(self):
        manifest,_=self.create(['codex']);other=self.base/'other-prefix';other.mkdir()
        r=b.inspect(dict(self.req,prefix=str(other),path=str(manifest)));self.assertEqual(r['components'][0]['requirements']['candidates'],[])
        (self.guest/'usr/bin/node').unlink();r=b.inspect(dict(self.req,path=str(manifest)));self.assertFalse(r['components'][0]['requirements']['candidates'][0]['node'])
    def test_custom_user_directory_and_symlink_escape(self):
        _,r=self.create(['folder:root/projects/demo'],['root/projects/demo'])
        with tarfile.open(r['components'][0]['file']) as t:self.assertIn('payload/root/projects/demo/main.py',t.getnames());self.assertNotIn('payload/root/projects/demo/build/out.apk',t.getnames())
        os.symlink(self.out,self.guest/'root/link')
        for folders in [['../escape'],['etc'],['root/link'],['root/link/sub']]:
            with self.assertRaises(b.BackupError):b.selected_plan(dict(self.req,selections=[dict(scope='ubuntu:ubuntu',items=['codex'],folders=folders)]))
    def test_explicit_build_directory_can_be_selected(self):
        _,r=self.create(['folder:root/termux-fork/.tools'],['root/termux-fork/.tools'])
        with tarfile.open(r['components'][0]['file']) as t:self.assertIn('payload/root/termux-fork/.tools/tool',t.getnames())
    def test_unknown_scope_item_duplicate_and_unsafe_paths_rejected(self):
        for choice in [dict(scope='ubuntu:missing',items=['codex']),dict(scope='ubuntu:ubuntu',items=['unknown']),dict(scope='ubuntu:ubuntu',items=['codex','codex'])]:
            with self.assertRaises(b.BackupError):b.selected_plan(dict(self.req,selections=[choice]))
    def test_termux_codex_keeps_home_and_prefix_paths(self):
        (self.home/'.codex').mkdir();(self.home/'.codex/auth.json').write_text('fixture')
        p=self.prefix/'lib/node_modules/@openai/codex';p.mkdir(parents=True);(p/'package.json').write_text('{}')
        result=b.create(dict(self.req,destination=str(self.out),components=[],environments=[],selections=[dict(scope='termux',items=['codex'])]))
        r=b.inspect(dict(self.req,path=str(Path(result['path'])/'manifest.json')))
        with tarfile.open(r['components'][0]['file']) as t:self.assertIn('payload/home/.codex/auth.json',t.getnames());self.assertIn('payload/usr/lib/node_modules/@openai/codex/package.json',t.getnames())
        self.assertEqual(r['components'][0]['selection']['environment'],'termux')
    def test_ssh_git_dependencies_checked_without_installation(self):
        _,r=self.create(['ssh']);req=r['components'][0]['requirements'];self.assertTrue(req['needsSshGit']);self.assertFalse(req['needsNode']);self.assertFalse(req['candidates'][0]['ssh']);self.assertFalse(req['candidates'][0]['git'])
        self.put('usr/bin/git',elf());self.put('usr/bin/ssh',elf());_,r=self.create(['ssh']);self.assertTrue(r['components'][0]['requirements']['candidates'][0]['ssh']);self.assertTrue(r['components'][0]['requirements']['candidates'][0]['git'])
    def test_unreadable_unselected_directory_does_not_abort_inventory(self):
        original=b.os.walk;unreadable=str(self.guest/'root/termux-fork/.tools')
        def fake(top,**kwargs):
            for folder,dirs,files in original(top,**kwargs):
                if folder==unreadable:
                    kwargs['onerror'](PermissionError(13,'fixture blocked',folder));continue
                yield folder,dirs,files
        with patch.object(b.os,'walk',fake):
            scope=next(v for v in b.selection_inventory(self.req)['scopes'] if v['id']=='ubuntu:ubuntu')
            items={v['id']:v for v in scope['items']}
            self.assertGreater(items['builds']['unreadable'],0);self.assertEqual(items['codex']['unreadable'],0)
            self.assertEqual(scope['issues'][0]['path'],'root/termux-fork/.tools')
            _,report=self.create(['codex']);self.assertEqual(report['components'][0]['kind'],'data')
            with self.assertRaises(b.BackupError):self.create(['builds'])
    def test_selected_copy_never_ignores_missing_file_or_permission_error(self):
        original=b.os.walk
        def fake(top,**kwargs):
            for folder,dirs,files in original(top,**kwargs):
                if folder==str(self.guest/'root/.codex'):
                    kwargs['onerror'](PermissionError(13,'fixture blocked',folder));continue
                yield folder,dirs,files
        with patch.object(b.os,'walk',fake):
            with self.assertRaises(b.BackupError):self.create(['codex'])
        self.assertTrue((self.guest/'root/.codex/auth.json').exists())
    def l2s(self,relative,data):
        source=self.guest/relative
        if source.exists():source.unlink()
        store=self.guest/'.l2s';store.mkdir(exist_ok=True)
        backing=store/('.proot.l2s.'+source.name+'0001.0001');backing.write_bytes(data)
        marker=store/('.proot.l2s.'+source.name+'0001');marker.symlink_to(backing.name)
        source.parent.mkdir(parents=True,exist_ok=True);source.symlink_to(marker)
        return source,backing
    def test_l2s_codex_file_materialized_and_backing_store_not_duplicated(self):
        source,backing=self.l2s('root/.codex/config.toml',b'# backed')
        _,r=self.create(['codex'])
        with tarfile.open(r['components'][0]['file']) as tar:
            item=tar.getmember('payload/root/.codex/config.toml');self.assertTrue(item.isfile());self.assertEqual(tar.extractfile(item).read(),b'# backed')
            self.assertFalse(any('/.l2s/' in p for p in tar.getnames()))
        self.assertTrue(source.is_symlink());self.assertEqual(backing.read_bytes(),b'# backed')
    def test_l2s_system_binary_has_detectable_architecture(self):
        self.l2s('usr/bin/env',bytes(elf()));_,r=self.create(['system'])
        self.assertEqual(r['components'][0]['arch'],'aarch64');self.assertEqual(r['components'][0]['status'],'compatible')
    def test_l2s_external_or_other_private_target_never_dereferenced(self):
        p=self.guest/'root/.codex/evil';p.symlink_to(self.out/'.proot.l2s.evil0001')
        (self.out/'.proot.l2s.evil0001').write_text('DO NOT COPY')
        with self.assertRaises(b.BackupError):self.create(['codex'])
        p.unlink();store=self.guest/'.l2s';store.mkdir();marker=store/'.proot.l2s.evil0001';marker.symlink_to(self.guest/'root/.ssh/id_ed25519');p.symlink_to(marker)
        with self.assertRaises(b.BackupError):self.create(['codex'])
    def test_pinned_file_open_rejects_symlink_ancestor(self):
        source=self.guest/'root/escape';source.symlink_to(self.out,target_is_directory=True);(self.out/'private').write_text('outside')
        with self.assertRaises(b.BackupError):b.selection_open(self.guest,'root/escape/private')
    def test_misleading_partial_metadata_cannot_claim_bootable_ubuntu(self):
        manifest,_=self.create(['codex']);value=json.loads(manifest.read_text());value['components'][0]['selection']['system']=True;manifest.write_text(json.dumps(value))
        with self.assertRaises(b.BackupError):b.inspect(dict(self.req,path=str(manifest)))
    def test_nested_project_workspace_file_is_not_library_metadata(self):
        self.put('root/projects/demo/workspace-v1.json',b'{"not":"a library"}')
        _,r=self.create(['project:root/projects']);self.assertIsNone(r['workspace'])
    def test_selected_data_cross_arch_warns_without_executing_binaries(self):
        manifest,_=self.create(['codex']);value=json.loads(manifest.read_text());value['source']['arch']='x86_64';manifest.write_text(json.dumps(value))
        r=b.inspect(dict(self.req,path=str(manifest)));self.assertFalse(r['components'][0]['requirements']['architectureMatches'])
    def test_mounts_and_external_symlinks_not_followed(self):
        self.put('proc/runtime/file',b'NEVER');os.symlink(self.out,self.guest/'root/external')
        _,r=self.create(['system','other'])
        with tarfile.open(r['components'][0]['file']) as t:
            self.assertNotIn('ubuntu/rootfs/proc/runtime/file',t.getnames());self.assertTrue(t.getmember('ubuntu/rootfs/root/external').issym())

    def counts(self,report):
        return {s['id']:{i['id']:(i['bytes'],i['files'],i['unreadable']) for i in s['items']} for s in report['scopes']}
    def test_cached_folder_reclassification_matches_fresh_without_tree_walk(self):
        snapshot=b.selection_inventory(self.req)
        folders={'ubuntu:ubuntu':['root/projects/demo','root/projects/demo/build','root/.codex']}
        with patch.object(b,'selection_walk',side_effect=AssertionError('unexpected tree walk')):
            reused=b.selection_inventory(dict(self.req,folders=folders,inventoryCache=snapshot))
        fresh=b.selection_inventory(dict(self.req,folders=folders))
        self.assertTrue(reused['reusedEstimate']);self.assertEqual(self.counts(reused),self.counts(fresh))
    def test_cache_expires_and_changed_root_or_rules_force_rescan(self):
        import copy
        original=b.selection_inventory(self.req)
        for reason in ('expired','future','root','rules','malformed'):
            snapshot=copy.deepcopy(original)
            for scope in snapshot['scopes']:
                cache=scope['scan']
                if reason=='expired':cache['time']-=b.INVENTORY_CACHE_SECONDS+1
                elif reason=='future':cache['time']+=999999
                elif reason=='root':cache['root']=[-1,-1]
                elif reason=='rules':cache['specs']=[]
                else:cache['rows']=[['bad',-1,1]]
            walk=b.selection_walk
            with patch.object(b,'selection_walk',wraps=walk) as observed:
                report=b.selection_inventory(dict(self.req,inventoryCache=snapshot))
                self.assertFalse(report['reusedEstimate']);self.assertGreater(observed.call_count,0)
    def test_cache_limit_falls_back_and_fresh_counts_are_preserved(self):
        normal=b.selection_inventory(self.req)
        with patch.object(b,'INVENTORY_CACHE_ROWS',0):
            limited=b.selection_inventory(self.req)
        self.assertEqual(self.counts(normal),self.counts(limited))
        self.assertTrue(all('scan' not in scope for scope in limited['scopes']))
        with patch.object(b,'INVENTORY_CACHE_BYTES',0):
            limited=b.selection_inventory(self.req)
        self.assertEqual(self.counts(normal),self.counts(limited))
        self.assertTrue(all('scan' not in scope for scope in limited['scopes']))
    def test_cached_sizes_never_supply_actual_archive_data(self):
        snapshot=b.selection_inventory(self.req)
        target=self.put('root/projects/demo/main.py',b'NEW LONGER CONTENT AFTER INVENTORY')
        req=dict(self.req,destination=str(self.out),components=[],environments=[],inventoryCache=snapshot,
                 selections=[dict(scope='ubuntu:ubuntu',items=['project:root/projects'],folders=[])])
        result=b.create(req);report=b.inspect(dict(self.req,path=str(Path(result['path'])/'manifest.json')))
        with tarfile.open(report['components'][0]['file']) as archive:
            self.assertEqual(archive.extractfile('payload/root/projects/demo/main.py').read(),target.read_bytes())
    def test_removed_environment_after_inventory_is_not_silently_omitted(self):
        b.environment_info(self.req)
        self.guest.parent.rename(self.base/'moved-environment')
        req=dict(self.req,destination=str(self.out),components=[],environments=['ubuntu'])
        with self.assertRaisesRegex(b.BackupError,'SOURCE_CHANGED_RETRY'):b.create(req)
        self.assertEqual(list(self.out.iterdir()),[])
    def test_inventory_reuses_walker_metadata(self):
        size=b.selection_size;seen=[]
        def checked(root,path,metadata=None):
            self.assertIsNotNone(metadata);seen.append(path);return size(root,path,metadata)
        with patch.object(b,'selection_size',side_effect=checked):b.selection_inventory(self.req)
        self.assertTrue(seen)
    def test_compiled_classifier_preserves_longest_prefix_and_custom_ties(self):
        specs=[{'id':'first','paths':['root/a','root/config']},
               {'id':'second','paths':['root/a','root/a/deep']},
               {'id':'folder:root/a','paths':['root/a']},
               {'id':'other','paths':['root']},{'id':'system','paths':['']}]
        classifier=b.SelectionClassifier(specs)
        for relative in ['root/a','root/a/file','root/a/deep/file','root/abc','usr/bin/x','root/config']:
            matches=[(len(p),int(spec['id'].startswith('folder:')),-i,spec['id']) for i,spec in enumerate(specs) for p in spec['paths'] if not p or relative==p or relative.startswith(p+'/')]
            self.assertEqual(classifier.group(relative),max(matches)[3])
    def test_full_size_uses_one_metadata_read_and_ignores_link_targets(self):
        root=self.base/'scan';root.mkdir();(root/'file').write_bytes(b'123');(root/'nested').mkdir();(root/'nested/a').write_bytes(b'45')
        (root/'outside').symlink_to(self.guest,target_is_directory=True)
        (root/'excluded').mkdir();(root/'excluded/x').write_bytes(b'NEVER')
        original=b.os.scandir;reads={}
        class Entry:
            def __init__(self,entry):self.entry=entry;self.path=entry.path
            def stat(self,follow_symlinks=False):
                reads[self.path]=reads.get(self.path,0)+1
                return self.entry.stat(follow_symlinks=follow_symlinks)
        from contextlib import contextmanager
        @contextmanager
        def scan(path):
            with original(path) as entries:yield (Entry(entry) for entry in entries)
        with patch.object(b.os,'scandir',scan):self.assertEqual(b.tree_bytes(root,[root/'excluded']),5)
        self.assertTrue(reads);self.assertTrue(all(n==1 for n in reads.values()))

if __name__=='__main__':unittest.main()
