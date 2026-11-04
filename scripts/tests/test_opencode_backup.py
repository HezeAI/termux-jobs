"""Artificial connection archives only. Never invokes a CLI or inspects real auth."""
import importlib.util
import io
import json
from pathlib import Path
import sqlite3
import sys
import tarfile
import tempfile
import unittest
sys.dont_write_bytecode = True
ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('backup', ROOT/'app/src/main/assets/workspace-backup.py')
b = importlib.util.module_from_spec(spec)
spec.loader.exec_module(b)
ID = '11111111-1111-4111-8111-111111111111'
class OpenCodeBackupTests(unittest.TestCase):
    def setUp(self):
        base=ROOT/'.verification/opencode-backup-fix/unit';base.mkdir(parents=True,exist_ok=True)
        self.temp=tempfile.TemporaryDirectory(dir=base);self.root=Path(self.temp.name)
        self.files=self.root/'files';self.home=self.files/'home';self.prefix=self.files/'usr';self.work=self.root/'work';self.output=self.root/'output'
        for p in (self.home,self.prefix,self.work,self.output):p.mkdir(parents=True)
        self.connection=self.files/'.ai-control/connections'/ID
        for name in ('config/opencode','data/opencode','state/opencode','session-links','receipts','cache'):(self.connection/name).mkdir(parents=True,exist_ok=True)
        (self.connection/'config/opencode/opencode.json').write_text('{"key":"SYNTHETIC_CONFIG_SECRET"}')
        (self.connection/'data/opencode/auth.json').write_text('SYNTHETIC_AUTH_ONLY')
        (self.connection/'state/opencode/settings.json').write_text('SYNTHETIC_STATE')
        (self.connection/'session-links/sesAlias.json').write_text('{"id":"sesNative"}')
        (self.connection/'receipts/sesNative.json').write_text('SYNTHETIC_COMMAND_RESULT')
        (self.connection/'cache/omit.txt').write_text('SYNTHETIC_CACHE_OMIT')
        (self.connection/'runtime.txt').write_text('SYNTHETIC_RUNTIME_OMIT')
        self.workspace=dict(version=8,commands=[],notes=[],recentCommands=[],sessions=[dict(id=ID,aiAgent='opencode',aiConnection=ID)])
        self.req=dict(home=str(self.home),prefix=str(self.prefix),files=str(self.files),work=str(self.work),destination=str(self.output),components=['library'],environments=[],workspace=self.workspace,includeOpenCodeConnections=True)
    def tearDown(self):self.temp.cleanup()
    def export(self):
        result=b.create(self.req);path=Path(result['path'])/'manifest.json';report=b.inspect(dict(self.req,path=str(path)));return path,report
    def restore(self,path,report):
        home=self.root/('restore-'+str(len(list(self.root.glob('restore-*')))));home.mkdir()
        return b.restore(dict(self.req,home=str(home),path=str(path),destination=str(home),fingerprints=report['fingerprints'],selected=[i for i,c in enumerate(report['components']) if c['kind']=='opencode']))
    def test_component_contains_secrets_history_links_and_state_but_no_cache_or_runtime(self):
        path,report=self.export();self.assertEqual(json.loads(path.read_text())['version'],3)
        restored=self.restore(path,report);target=Path(restored['opencodeConnections'][ID])
        for name in ('config','data','state','session-links','receipts'):
            for p in (self.connection/name).rglob('*'):
                if p.is_file():self.assertEqual(p.read_bytes(),(target/p.relative_to(self.connection)).read_bytes())
        self.assertFalse((target/'cache').exists());self.assertFalse((target/'runtime.txt').exists())
        self.assertEqual(report['workspace']['sessions'][0]['aiConnection'],ID)
        for archive in path.parent.glob('*.tar.gz'):self.assertEqual(archive.stat().st_mode&0o777,0o600)
    def test_live_sqlite_wal_is_snapshotted_without_wal_sidecars(self):
        database=self.connection/'data/opencode/opencode.db'
        source=sqlite3.connect(database);source.execute('PRAGMA journal_mode=WAL');source.execute('CREATE TABLE secret(value TEXT)');source.execute("INSERT INTO secret VALUES ('SYNTHETIC_WAL_ONLY')");source.commit()
        try:
            self.assertTrue(Path(str(database)+'-wal').exists());path,report=self.export();restored=self.restore(path,report)
            target=Path(restored['opencodeConnections'][ID])/'data/opencode/opencode.db'
            with sqlite3.connect(target) as copied:self.assertEqual(copied.execute('SELECT value FROM secret').fetchone()[0],'SYNTHETIC_WAL_ONLY');self.assertEqual(copied.execute('PRAGMA quick_check').fetchone()[0],'ok')
            self.assertFalse(Path(str(target)+'-wal').exists());self.assertEqual(source.execute('SELECT count(*) FROM secret').fetchone()[0],1)
        finally:source.close()
    def test_shared_profiles_export_one_connection_and_restores_are_separate(self):
        self.workspace['sessions'].append(dict(self.workspace['sessions'][0],id='22222222-2222-4222-8222-222222222222'))
        path,report=self.export();first=self.restore(path,report);second=self.restore(path,report)
        self.assertEqual(len(first['opencodeConnections']),1);self.assertNotEqual(first['path'],second['path'])
    def test_connection_data_requires_explicit_boolean_opt_in(self):
        for flag in (None, False, 'true', 1):
            self.req.pop('includeOpenCodeConnections',None)
            if flag is not None:self.req['includeOpenCodeConnections']=flag
            path,report=self.export()
            self.assertEqual(json.loads(path.read_text())['version'],1)
            self.assertFalse(any(c['kind']=='opencode' for c in report['components']))
            self.assertEqual(report['workspace']['sessions'][0]['aiConnection'],ID)
            self.assertFalse(json.loads(path.read_text())['opencodeConnectionsIncluded'])
    def test_inventory_measures_private_data_separately_from_library(self):
        report=b.environment_info(self.req)
        self.assertEqual(report['libraryBytes'],0)
        self.assertGreater(report['opencodeBytes'],0)
    def test_unselected_unreadable_connection_does_not_block_other_backup(self):
        (self.connection/'data/external').symlink_to(self.home)
        self.req['includeOpenCodeConnections']=False
        inventory=b.environment_info(self.req)
        self.assertTrue(inventory['opencodeUnreadable'])
        path,report=self.export()
        self.assertFalse(any(c['kind']=='opencode' for c in report['components']))
    def test_no_library_means_no_private_connection_export(self):
        self.req['components']=['home'];path,report=self.export();self.assertFalse(any(c['kind']=='opencode' for c in report['components']));self.assertEqual(json.loads(path.read_text())['version'],1)
    def test_absent_unconfigured_connection_preserves_old_archive_format(self):
        self.workspace['sessions'][0]['aiConnection']='22222222-2222-4222-8222-222222222222';path,report=self.export();self.assertEqual(json.loads(path.read_text())['version'],1)
    def test_symlink_source_refused_and_original_preserved(self):
        marker=self.root/'outside';marker.write_text('SYNTHETIC_OUTSIDE');(self.connection/'data/leak').symlink_to(marker)
        with self.assertRaises(b.BackupError):self.export()
        self.assertEqual(marker.read_text(),'SYNTHETIC_OUTSIDE');self.assertEqual(list(self.output.iterdir()),[])
    def test_malformed_id_refused_without_path_access(self):
        self.workspace['sessions'][0]['aiConnection']='../../outside'
        with self.assertRaises(b.BackupError):self.export()
    def test_foreign_component_members_and_links_are_rejected(self):
        for bad in ('opencode-connections/'+ID+'/cache/forbidden','opencode-connections/../../outside','opencode-connections/not-a-uuid/config/key'):
            path=self.root/'malformed.tar.gz'
            with tarfile.open(path,'w:gz') as tar:
                index=json.dumps(dict(version=1,connections=[ID])).encode();m=tarfile.TarInfo('opencode-connections/index.json');m.size=len(index);tar.addfile(m,io.BytesIO(index));m=tarfile.TarInfo(bad);m.size=1;tar.addfile(m,io.BytesIO(b'x'))
            with self.assertRaises(b.BackupError):b.inspect(dict(self.req,path=str(path)))
    def test_tampered_archive_blocks_restore_and_retry_succeeds(self):
        path,report=self.export();archive=path.parent/'opencode-connections.tar.gz';data=archive.read_bytes();archive.write_bytes(data+b'bad')
        with self.assertRaises(b.BackupError):self.restore(path,report)
        archive.write_bytes(data);restored=self.restore(path,report);self.assertIn(ID,restored['opencodeConnections'])
if __name__=='__main__':unittest.main()
