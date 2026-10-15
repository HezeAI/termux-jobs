"""Read-only filename search tests; all fixtures remain in .verification."""
import importlib.util, json, os, sys, tempfile, unittest
from pathlib import Path
from unittest.mock import patch
sys.dont_write_bytecode=True
REPO=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('search', REPO/'app/src/main/assets/workspace-server-search.py')
s=importlib.util.module_from_spec(spec);spec.loader.exec_module(s)
class SearchTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory(dir=REPO/'.verification',prefix='search-test-');self.root=Path(self.temp.name)
    def tearDown(self):self.temp.cleanup()
    def search(self,q):return s.search({'path':str(self.root),'query':q})
    def test_recursive_casefold_and_no_contents_search(self):
        child=self.root/'ПРОЕКТ';child.mkdir();(child/'ReadMe.md').write_text('secret text does not match filename');(self.root/'other').write_text('ReadMe')
        r=self.search('readme');self.assertEqual([str(child/'ReadMe.md')],[e['path'] for e in r['entries']]);self.assertFalse(r['partial']);self.assertEqual(3,r['scanned']);self.assertEqual('ReadMe', (self.root/'other').read_text());self.assertTrue(self.search('проект')['entries'][0]['directory'])
    def test_links_not_followed_or_returned(self):
        (self.root/'real').mkdir();(self.root/'real'/'find-me').write_text('ok');(self.root/'find-link').symlink_to(self.root/'real',target_is_directory=True);(self.root/'loop').symlink_to(self.root,target_is_directory=True)
        r=self.search('find');self.assertEqual(1,len(r['entries']));self.assertEqual(2,r['skipped'])
    def test_result_limit_is_explicit(self):
        for i in range(505):(self.root/f'match-{i}').touch()
        r=self.search('match');self.assertEqual(500,len(r['entries']));self.assertTrue(r['partial'])
    def test_query_is_literal_not_shell_or_glob(self):
        name='$(touch NOT_CREATED)';(self.root/name).touch();self.assertEqual(1,len(self.search(name)['entries']));self.assertFalse((self.root/'NOT_CREATED').exists());self.assertEqual([],self.search('*')['entries'])
    def test_invalid_queries_and_root(self):
        for query in ('',' ',None,'x'*201,'bad\0'):
            with self.assertRaises(ValueError):self.search(query)
        with self.assertRaises(FileNotFoundError):s.search({'path':str(self.root/'missing'),'query':'a'})
    def test_deadline_reports_partial(self):
        (self.root/'match').touch()
        with patch.object(s.time,'monotonic',side_effect=[0,9]):r=self.search('match')
        self.assertTrue(r['partial']);self.assertEqual([],r['entries'])
if __name__=='__main__':unittest.main()
