"""Generate genuine v2.1.2-format archives from the unchanged published asset.
Offline local APK input only; artificial files and authorization markers.
"""
import hashlib
import importlib.util
import json
from pathlib import Path
import sys
import tempfile
import unittest
import zipfile
sys.dont_write_bytecode=True
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('new_backup',ROOT/'app/src/main/assets/workspace-backup.py');new=importlib.util.module_from_spec(spec);spec.loader.exec_module(new)
class LegacyArchiveTests(unittest.TestCase):
 def setUp(self):
  parent=ROOT/'.verification/opencode-backup-fix/legacy';parent.mkdir(parents=True,exist_ok=True);self.temp=tempfile.TemporaryDirectory(dir=parent);self.base=Path(self.temp.name)
  apk=ROOT/'.verification/release-v2.1.2/Termux-Jobs-v2.1.2-arm64.apk';self.assertEqual(hashlib.sha256(apk.read_bytes()).hexdigest(),'2f8b97b2d9083a5b7dcc3966eca54ea6d843fec3e253ba6e279c123921eeddab')
  with zipfile.ZipFile(apk) as z:(self.base/'old-backup.py').write_bytes(z.read('assets/workspace-backup.py'))
  spec=importlib.util.spec_from_file_location('old_backup',self.base/'old-backup.py');self.old=importlib.util.module_from_spec(spec);spec.loader.exec_module(self.old)
  files=self.base/'files';self.home=files/'home';self.prefix=files/'usr';self.work=self.base/'work';output=self.base/'output'
  for p in [self.home/'project',self.prefix,self.work,output,files/'workspace-sessions/fake'] :p.mkdir(parents=True)
  (self.home/'project/file.txt').write_text('SYNTHETIC_LEGACY_FILE');(files/'workspace-sessions/fake/history').write_text('SYNTHETIC_LEGACY_HISTORY')
  workspace=dict(version=8,commands=[],notes=[],recentCommands=[],sessions=[dict(id='22222222-2222-4222-8222-222222222222',name='Synthetic legacy OpenCode',saved=True,environment='ubuntu',codexEnvironment='ubuntu-ai',aiAgent='opencode',aiConnection='11111111-1111-4111-8111-111111111111',aiThread='sesLegacy',aiChat='resume',aiMode='build',aiAccess='ask')])
  self.req=dict(home=str(self.home),prefix=str(self.prefix),files=str(files),work=str(self.work),destination=str(output),components=['library','home'],environments=[],workspace=workspace,settings={})
 def tearDown(self):self.temp.cleanup()
 def check(self,selected):
  req=dict(self.req)
  if selected:req.update(components=['library'],selections=[dict(scope='termux',items=[i['id'] for i in self.old.selection_layout(self.old.selection_scopes(req)[0])])])
  made=self.old.create(req);path=Path(made['path'])/'manifest.json';self.assertEqual(json.loads(path.read_text())['version'],2 if selected else 1)
  report=new.inspect(dict(req,path=str(path)));self.assertEqual(report['workspace']['sessions'][0]['aiConnection'],'11111111-1111-4111-8111-111111111111');self.assertEqual(report['workspace']['sessions'][0]['aiAccess'],'ask');target=self.base/'fresh-home';target.mkdir();indices=[i for i,c in enumerate(report['components']) if c['kind'] in ('home','data','app-data')]
  r=new.restore(dict(req,home=str(target),path=str(path),fingerprints=report['fingerprints'],destination=str(target),selected=indices));contents=[p.read_bytes() for p in Path(r['path']).rglob('*') if p.is_file()]
  for marker in [b'SYNTHETIC_LEGACY_FILE',b'SYNTHETIC_LEGACY_HISTORY']:self.assertTrue(any(marker in c for c in contents))
  self.assertEqual(r['opencodeConnections'],{});self.assertEqual((self.home/'project/file.txt').read_text(),'SYNTHETIC_LEGACY_FILE')
 def test_v1_archive_created_by_published_helper_restores(self):self.check(False)
 def test_v2_selected_archive_created_by_published_helper_restores(self):self.check(True)
 def test_published_reader_rejects_new_connection_format_instead_of_silently_losing_it(self):
  self.req['includeOpenCodeConnections']=True;connection='11111111-1111-4111-8111-111111111111';self.req['workspace']['sessions']=[dict(aiAgent='opencode',aiConnection=connection)]
  root=Path(self.req['files'])/'.ai-control/connections'/connection/'config';root.mkdir(parents=True);(root/'synthetic.json').write_text('SYNTHETIC_SECRET')
  made=new.create(self.req);path=Path(made['path'])/'manifest.json';self.assertEqual(json.loads(path.read_text())['version'],3)
  with self.assertRaises(self.old.BackupError):self.old.inspect(dict(self.req,path=str(path)))
if __name__=='__main__':unittest.main()
