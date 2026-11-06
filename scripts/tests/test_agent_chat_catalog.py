import hashlib
import importlib.util
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('catalog', ROOT / 'app/src/main/assets/agent-chat-catalog.py')
catalog = importlib.util.module_from_spec(spec)
spec.loader.exec_module(catalog)

class NativeMetadata(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory(dir=ROOT / '.verification/redesign-1500')
        self.home = Path(self.tmp.name)
        self.env = patch.dict(os.environ, {'HOME':str(self.home), 'CLAUDE_CONFIG_DIR':str(self.home / '.claude'), 'GEMINI_CLI_HOME':str(self.home), 'SANDBOX':''})
        self.env.start()
    def tearDown(self):
        self.env.stop()
        self.tmp.cleanup()
    def test_claude_pages_keep_equal_text_distinct_ids_and_never_read_whole_archive(self):
        folder=self.home / '.claude/projects/project';folder.mkdir(parents=True)
        for i in range(65):
            identifier=f'12345678-1234-1234-1234-{i:012d}'
            file=folder / (identifier+'.jsonl')
            file.write_text(json.dumps({'sessionId':identifier,'cwd':'/project','type':'user','message':{'content':'same title'}})+'\n'+json.dumps({'padding':'X'*200000})+'\n')
        os.symlink(self.home / '.claude/credentials.json',folder/'99999999-1234-1234-1234-123456789012.jsonl')
        cursor='';found=[];requests=0
        original=os.read;read_bytes=[]
        def bounded(fd,n):
            self.assertLessEqual(n,catalog.READ_BYTES);read_bytes.append(n);return original(fd,n)
        with patch.object(catalog.os,'read',bounded):
            while True:
                result=catalog.catalog('claude','/project',cursor);requests+=1
                self.assertLessEqual(len(result['sessions']),30);found+=result['sessions'];cursor=result['cursor']
                if not cursor:break
        self.assertEqual(3,requests);self.assertEqual(65,len({x['id'] for x in found}));self.assertTrue(all(x['name']=='same title' for x in found));self.assertLessEqual(sum(read_bytes),65*131072)
    def test_gemini_native_jsonl_metadata_and_legacy_json_project_isolation(self):
        folder=self.home / '.gemini/tmp/slug/chats';folder.mkdir(parents=True)
        def record(project,n):return {'sessionId':f'12345678-1234-1234-1234-{n:012d}', 'projectHash':hashlib.sha256(project.encode()).hexdigest(),'summary':'Native title'}
        (folder/'session-first.jsonl').write_text(json.dumps({'$set':record('/project',1)})+'\n'+json.dumps({'type':'user','id':'1','content':'hi'})+'\n')
        (folder/'session-legacy.json').write_text(json.dumps(record('/project',2)))
        (folder/'session-foreign.jsonl').write_text(json.dumps({'$set':record('/foreign',3)})+'\n')
        result=catalog.catalog('gemini','/project');self.assertEqual(2,len(result['sessions']));self.assertTrue(all(x['directory']=='/project' and x['name']=='Native title' for x in result['sessions']))
        (folder/'session-broken.jsonl').write_text('broken')
        with self.assertRaisesRegex(ValueError,'HISTORY_METADATA'):catalog.catalog('gemini','/project')
    def test_missing_is_empty_but_permission_error_and_changed_cursor_are_errors(self):
        self.assertEqual([],catalog.catalog('claude','/project')['sessions'])
        with patch.object(catalog.os,'scandir',side_effect=PermissionError('denied')):
            with self.assertRaises(PermissionError):catalog.catalog('claude','/project')
        folder=self.home / '.claude/projects/project';folder.mkdir(parents=True)
        with self.assertRaisesRegex(ValueError,'CATALOG_CHANGED'):catalog.catalog('claude','/project','wrong:30')

if __name__=='__main__':unittest.main()
