"""Fail-closed downloads and targeting for the repository-local CI preparer."""
import hashlib, importlib.util, io, tempfile, unittest
from pathlib import Path
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('prepare_x11',ROOT/'scripts/prepare-x11.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
class PrepareX11Tests(unittest.TestCase):
 def test_download_checks_size_and_hash_before_promoting(self):
  data=b'verified input';record=dict(url='https://example.invalid/pinned',size=len(data),sha256=hashlib.sha256(data).hexdigest())
  for body in (b'truncated',b'x'*len(data),data):
   with self.subTest(body=body),tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d,patch.object(m,'ROOT',Path(d)),patch.object(m.urllib.request,'urlopen',return_value=io.BytesIO(body)):
    if body==data:self.assertEqual(data,m.verified_file('input',record).read_bytes())
    else:
     with self.assertRaises(ValueError):m.verified_file('input',record)
     self.assertFalse((Path(d)/'.tools/termux-x11-downloads/input').exists())
    self.assertFalse(list(Path(d).rglob('*.partial')))
 def test_gateway_fallback_keeps_the_exact_pinned_asset(self):
  import json,urllib.error
  data=b'input';url='https://github.com/HezeAI/termux-jobs-components/releases/download/pin/file.tar'
  record=dict(url=url,size=len(data),sha256=hashlib.sha256(data).hexdigest())
  asset=dict(id=123,name='file.tar',browser_download_url=url,size=len(data),digest='sha256:'+record['sha256'])
  metadata=dict(tag_name='pin',draft=False,assets=[asset])
  for wrong in (False,True):
   if wrong:asset['digest']='sha256:'+'0'*64
   calls=[urllib.error.HTTPError(url,504,'gateway',{},None),io.BytesIO(json.dumps(metadata).encode()),io.BytesIO(data)]
   with patch.object(m.urllib.request,'urlopen',side_effect=calls) as fetch:
    if wrong:
     with self.assertRaises(ValueError):m.open_input(record)
     self.assertEqual(2,fetch.call_count)
    else:
     with m.open_input(record) as response:self.assertEqual(data,response.read())
     req=fetch.call_args.args[0];self.assertEqual('https://api.github.com/repos/HezeAI/termux-jobs-components/releases/assets/123',req.full_url);self.assertEqual('application/octet-stream',req.get_header('Accept'))
 def test_corrupt_cache_is_not_accepted_or_silently_replaced(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d,patch.object(m,'ROOT',Path(d)),patch.object(m.urllib.request,'urlopen') as fetch:
   p=Path(d)/'.tools/termux-x11-downloads/input';p.parent.mkdir(parents=True);p.write_bytes(b'bad')
   with self.assertRaises(ValueError):m.verified_file('input',dict(size=3,sha256='0'*64))
   fetch.assert_not_called();self.assertEqual(b'bad',p.read_bytes())
 def test_mirror_keeps_original_binary_identity(self):
  import json
  pins=json.loads((ROOT/'third-party/termux-x11/provenance.json').read_text())
  for key in ('size','sha256'):self.assertEqual(pins['apk'][key],m.BUILD_INPUTS['apkMirror'][key])
 def test_guard_is_still_mandatory(self):
  source=(ROOT/'x11/build.gradle').read_text()
  self.assertIn('preBuild.dependsOn verifyX11Inputs',source)
  self.assertIn('throw new GradleException',source)
if __name__=='__main__':unittest.main()
