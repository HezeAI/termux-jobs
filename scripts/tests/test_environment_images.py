"""Pinned rootfs downloads: no live network, no installed environment changes."""
import contextlib,hashlib,importlib.util,io,json,os,tempfile,unittest
from pathlib import Path
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[2]
s=importlib.util.spec_from_file_location('images',ROOT/'app/src/main/assets/environment-image.py');m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
class Response(io.BytesIO):
 def __init__(self,data,status=200,headers=None,url='https://example.test/image'):
  super().__init__(data);self.status=status;self.headers=headers or {};self.url=url
 def geturl(self):return self.url
class Opener:
 def __init__(self,*responses):self.responses=list(responses);self.requests=[]
 def open(self,request,timeout):self.requests.append(request);return self.responses.pop(0)
class Images(unittest.TestCase):
 def setUp(self):
  self.temp=tempfile.TemporaryDirectory(dir=ROOT/'.verification');self.base=Path(self.temp.name);self.path=self.base/'image.tar';self.data=b'reviewed rootfs fixture';self.sha=hashlib.sha256(self.data).hexdigest()
 def tearDown(self):self.temp.cleanup()
 def fetch(self,o):
  with contextlib.redirect_stderr(io.StringIO()):return m.download(self.path,'https://example.test/image',self.sha,len(self.data),o)
 def test_verified_cache_is_reused_offline(self):
  self.path.write_bytes(self.data);o=Opener();self.assertEqual(self.path,self.fetch(o));self.assertEqual([],o.requests)
 def test_available_bytes_are_reported_without_waiting_for_a_full_read_buffer(self):
  class Available(Response):
   def read(self,*args):raise AssertionError('blocking full-buffer read must not be used')
   def read1(self,n):return io.BytesIO.read(self,3)
  self.fetch(Opener(Available(self.data)));self.assertEqual(self.data,self.path.read_bytes())
 def test_interrupt_retains_received_bytes_for_the_next_launch(self):
  class Interrupted(Response):
   calls=0
   def read1(self,n):
    self.calls+=1
    if self.calls>1:raise KeyboardInterrupt()
    return super().read1(7)
  with self.assertRaises(KeyboardInterrupt):self.fetch(Opener(Interrupted(self.data)))
  self.assertFalse(self.path.exists());self.assertEqual(self.data[:7],self.path.with_name('image.tar.part').read_bytes())
  self.fetch(Opener(Response(self.data[7:],206,{'Content-Range':'bytes 7-%d/%d'%(len(self.data)-1,len(self.data))})));self.assertEqual(self.data,self.path.read_bytes())
 def test_resume_uses_range_and_verifies_whole_file_before_publish(self):
  self.path.with_name('image.tar.part').write_bytes(self.data[:7]);o=Opener(Response(self.data[7:],206,{'Content-Range':'bytes 7-%d/%d'%(len(self.data)-1,len(self.data))}));self.fetch(o)
  self.assertEqual('bytes=7-',o.requests[0].get_header('Range'));self.assertEqual(self.data,self.path.read_bytes());self.assertFalse(self.path.with_name('image.tar.part').exists())
 def test_server_ignoring_range_restarts_without_duplicate_bytes(self):
  self.path.with_name('image.tar.part').write_bytes(b'old');self.fetch(Opener(Response(self.data)));self.assertEqual(self.data,self.path.read_bytes())
 def test_short_body_is_retained_and_resumed_after_connection_loss(self):
  o=Opener(Response(self.data[:7]),Response(self.data[7:],206,{'Content-Range':'bytes 7-%d/%d'%(len(self.data)-1,len(self.data))}))
  with patch.object(m.time,'sleep'):self.fetch(o)
  self.assertEqual(self.data,self.path.read_bytes());self.assertEqual('bytes=7-',o.requests[1].get_header('Range'))
 def test_wrong_digest_size_and_range_never_publish(self):
  for o in [Opener(Response(b'x'*len(self.data))),Opener(Response(self.data+b'!')),Opener(Response(self.data,206,{'Content-Range':'bytes 1-2/3'}))]:
   with self.subTest(opener=o),self.assertRaises(ValueError):self.fetch(o)
   self.assertFalse(self.path.exists());self.path.with_name('image.tar.part').unlink(missing_ok=True)
 def test_cache_or_partial_link_and_hardlink_do_not_modify_external_file(self):
  external=self.base/'keep';external.write_bytes(self.data)
  for name in ['image.tar','image.tar.part']:
   p=self.base/name;p.symlink_to(external)
   with self.assertRaises(ValueError):self.fetch(Opener(Response(self.data)))
   self.assertEqual(self.data,external.read_bytes());p.unlink()
  os.link(external,self.path)
  with self.assertRaises(ValueError):self.fetch(Opener(Response(self.data)))
  self.assertEqual(self.data,external.read_bytes())
 def test_plain_http_redirect_and_response_are_rejected(self):
  with self.assertRaises(ValueError):m.HttpsOnly().redirect_request(None,None,302,'',{},'http://example.test/image')
  with self.assertRaises(ValueError):self.fetch(Opener(Response(self.data,url='http://example.test/image')))
 def test_main_accepts_filesdir_alias_but_not_a_different_same_named_control(self):
  files=self.base/'files';files.mkdir();prefix=files/'usr';prefix.mkdir();control=files/'.environment-control';control.mkdir();alias=self.base/'alias';alias.symlink_to(files,target_is_directory=True)
  rows={'ubuntu':('image.tar',self.sha,len(self.data))}
  with patch.dict(os.environ,{'PREFIX':str(alias/'usr'),'JOBS_ENVIRONMENT_DOWNLOAD_DIR':str(control)}),patch.object(m.sys,'argv',['helper','jobs-x11-ubuntu']),patch.object(m,'specs',return_value=rows),patch.object(m,'download',return_value=control/'image.tar') as fetch,contextlib.redirect_stdout(io.StringIO()),contextlib.redirect_stderr(io.StringIO()):
   m.main();self.assertIn('/ubuntu/image.tar',str(fetch.call_args.args[0]))
  other=self.base/'other/.environment-control';other.mkdir(parents=True)
  with patch.dict(os.environ,{'PREFIX':str(prefix),'JOBS_ENVIRONMENT_DOWNLOAD_DIR':str(other)}),patch.object(m.sys,'argv',['helper','jobs-x11-ubuntu']),patch.object(m,'specs',return_value=rows),self.assertRaises(ValueError):m.main()
 def test_catalog_has_nine_bases_and_excludes_agent_ubuntu(self):
  rows=m.specs();self.assertEqual({'ubuntu','debian','alpine','archlinux','fedora','opensuse','void','kali','manjaro'},set(rows));self.assertNotIn('ubuntu-ai',m.ALIASES);self.assertEqual(9,len({v[0] for v in rows.values()}))
