"""Destructive fixtures stay under .verification; no SSH or real server operations."""
import importlib.util
import os
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch
sys.dont_write_bytecode = True
REPO = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('server_delete', REPO/'app/src/main/assets/workspace-server-delete.py')
s = importlib.util.module_from_spec(spec)
spec.loader.exec_module(s)


class DeleteTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(dir=REPO/'.verification', prefix='server-delete-test-')
        self.root = Path(self.temp.name)
        self.directory = self.root/'files'
        self.directory.mkdir()

    def tearDown(self):
        self.temp.cleanup()

    def entry(self, path):
        return {'path': str(path), 'directory': path.is_dir(), 'size': path.stat().st_size}

    def delete(self, entries):
        return s.delete_selection({'directory': str(self.directory), 'entries': entries})

    def test_batch_deletes_folders_and_link_only_without_following_target(self):
        outside = self.root/'outside';outside.mkdir();(outside/'keep').write_text('keep')
        folder = self.directory/'folder';folder.mkdir();(folder/'file').write_text('x')
        (folder/'outside').symlink_to(outside, target_is_directory=True)
        leaf = self.directory/'$(touch BAD)';leaf.write_text('x')
        r = self.delete([self.entry(folder), self.entry(leaf)])
        self.assertEqual('', r['error']);self.assertEqual(2, len(r['deleted']))
        self.assertEqual(4, r['removedNodes']);self.assertFalse(folder.exists());self.assertFalse(leaf.exists())
        self.assertEqual('keep', (outside/'keep').read_text());self.assertFalse((self.directory/'BAD').exists())

    def test_all_paths_validated_before_any_mutation(self):
        a = self.directory/'a';a.touch()
        for invalid in [str(self.directory), str(self.root/'outside'), str(self.directory/'..'/'outside'), '/', str(a)+'\0']:
            r = self.delete([self.entry(a), {'path': invalid}])
            self.assertNotEqual('', r['error']);self.assertEqual([], r['deleted']);self.assertTrue(a.exists())
        self.assertNotEqual('', self.delete([self.entry(a), self.entry(a)])['error'])

    def test_type_or_size_changes_are_rejected(self):
        a = self.directory/'a';a.write_text('new value')
        e = self.entry(a);e['size'] = 0
        self.assertEqual('REMOTE_CHANGED', self.delete([e])['error']);self.assertTrue(a.exists())
        e = self.entry(a);e['directory'] = True
        self.assertEqual('REMOTE_CHANGED', self.delete([e])['error'])

    def test_ancestor_symlink_is_refused(self):
        a = self.directory/'a';a.touch();alias = self.root/'alias';alias.symlink_to(self.directory, target_is_directory=True)
        r = s.delete_selection({'directory': str(alias), 'entries': [{'path': str(alias/'a'), 'size': 0}]})
        self.assertNotEqual('', r['error']);self.assertTrue(a.exists())

    def test_limits_abort_before_deleting(self):
        folder = self.directory/'folder';folder.mkdir();(folder/'a').touch();(folder/'b').touch()
        with patch.object(s, 'LIMIT', 2):r = self.delete([self.entry(folder)])
        self.assertEqual('TREE_LIMIT', r['error']);self.assertEqual(0, r['removedNodes']);self.assertTrue((folder/'a').exists())
        with patch.object(s, 'SECONDS', -1):r = self.delete([self.entry(folder)])
        self.assertEqual('TIMEOUT', r['error']);self.assertTrue(folder.exists())

    def test_partial_failure_reports_completed_entries_without_retry(self):
        a = self.directory/'a';a.touch();b = self.directory/'b';b.touch();real_unlink = os.unlink;calls = []
        def unlink(name, **kw):
            calls.append(name)
            if name == 'b':raise PermissionError()
            return real_unlink(name, **kw)
        with patch.object(s.os, 'unlink', side_effect=unlink):r = self.delete([self.entry(a), self.entry(b)])
        self.assertEqual('PERMISSION_DENIED', r['error']);self.assertEqual([str(a)], r['deleted']);self.assertEqual(['a', 'b'], calls)
        self.assertTrue(b.exists());self.assertFalse(a.exists())

    def test_directory_replaced_with_link_after_scan_cannot_escape(self):
        folder = self.directory/'folder';folder.mkdir();(folder/'a').touch()
        outside = self.root/'outside';outside.mkdir();(outside/'keep').write_text('keep')
        entries = [self.entry(folder)];real_stat = os.stat;hits = [0]
        def changed(path, *args, **kw):
            if path == 'folder' and 'dir_fd' in kw:
                hits[0] += 1
                if hits[0] == 2:
                    folder.rename(self.directory/'original');folder.symlink_to(outside, target_is_directory=True)
            return real_stat(path, *args, **kw)
        with patch.object(s.os, 'stat', side_effect=changed):r = self.delete(entries)
        self.assertEqual('REMOTE_CHANGED', r['error']);self.assertEqual(0, r['removedNodes']);self.assertEqual('keep', (outside/'keep').read_text())

    def test_stdin_transport_matches_remote_python_invocation(self):
        import subprocess, json
        a = self.directory/'тест.txt';a.write_text('x')
        request = {'directory': str(self.directory), 'entries': [self.entry(a)]}
        source = (REPO/'app/src/main/assets/workspace-server-delete.py').read_text()
        result = subprocess.run([sys.executable, '-I', '-c', source], input=json.dumps(request).encode(), stdout=subprocess.PIPE, check=True, timeout=5)
        response = json.loads(result.stdout)
        self.assertTrue(response['ok']);self.assertEqual('', response['result']['error']);self.assertFalse(a.exists())


if __name__ == '__main__':unittest.main()
