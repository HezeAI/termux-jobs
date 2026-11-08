"""Components API fallback keeps pinned identity, resume and unsafe-input rejection."""
import unittest,json,hashlib,io,urllib.error,contextlib
from unittest.mock import patch
from test_environment_images import Images,Response,m
class ApiOpener:
 def __init__(self,data,record,url,base,corrupt=False):self.data=data;self.record=record;self.url=url;self.base=base;self.requests=[];self.corrupt=corrupt
 def open(self,request,timeout):
  self.requests.append(request)
  if len(self.requests)==1:raise urllib.error.HTTPError(self.url,504,'gateway',{},None)
  if '/releases/tags/' in request.full_url:return Response(json.dumps(self.record).encode(),url=request.full_url)
  if '/releases/assets/123' in request.full_url:
   self.base.assertEqual('application/octet-stream',request.get_header('Accept'))
   data=b'x'*len(self.data)if self.corrupt else self.data
   if request.get_header('Range')=='bytes=7-':return Response(data[7:],206,{'Content-Range':'bytes 7-%d/%d'%(len(data)-1,len(data))},url=request.full_url)
   return Response(data,url=request.full_url)
  raise AssertionError('Unexpected URL; no token or foreign host permitted')
class Fallback(Images):
 url='https://github.com/HezeAI/termux-jobs-components/releases/download/fixed/image.tar'
 def record(self):return {'tag_name':'fixed','draft':False,'assets':[{'id':123,'name':'image.tar','size':len(self.data),'digest':'sha256:'+self.sha,'browser_download_url':self.url}]}
 def actual(self,opener):
  with contextlib.redirect_stderr(io.StringIO()),patch.object(m.time,'sleep'):return m.download(self.path,self.url,self.sha,len(self.data),opener)
 def test_gateway_falls_back_and_verifies_original_hash(self):
  o=ApiOpener(self.data,self.record(),self.url,self);self.actual(o);self.assertEqual(self.data,self.path.read_bytes());self.assertEqual(3,len(o.requests))
 def test_fallback_resumes_exact_existing_partial(self):
  self.path.with_name('image.tar.part').write_bytes(self.data[:7]);o=ApiOpener(self.data,self.record(),self.url,self);self.actual(o);self.assertEqual(self.data,self.path.read_bytes());self.assertEqual('bytes=7-',o.requests[-1].get_header('Range'))
 def test_corrupt_api_body_never_publishes(self):
  with self.assertRaisesRegex(ValueError,'CHECKSUM'):self.actual(ApiOpener(self.data,self.record(),self.url,self,True))
  self.assertFalse(self.path.exists())
 def test_wrong_metadata_never_selects_untrusted_asset(self):
  for field,value in [('id',True),('name','other.tar'),('size',1),('digest','sha256:'+'0'*64),('browser_download_url','https://foreign.invalid/image.tar')]:
   record=self.record();record['assets'][0][field]=value;o=ApiOpener(self.data,record,self.url,self)
   self.assertIsNone(m.github_asset(self.url,self.sha,len(self.data),type('Meta',(),{'open':lambda _s,_r,timeout:Response(json.dumps(record).encode(),url=_r.full_url)})()))
 def test_malformed_metadata_is_not_a_download_target(self):
  for record in ([],None,{'tag_name':'fixed','assets':{}},{'tag_name':'fixed','assets':[None,1]}):
   opener=type('Meta',(),{'open':lambda _s,_r,timeout:Response(json.dumps(record).encode(),url=_r.full_url)})()
   self.assertIsNone(m.github_asset(self.url,self.sha,len(self.data),opener))
 def test_foreign_repository_never_queries_api(self):
  self.assertIsNone(m.github_asset(self.url.replace('HezeAI','other'),self.sha,len(self.data),None))
if __name__=='__main__':unittest.main()
