"""Read-only upstream metadata fixtures; no installed rootfs/guest processes."""
import contextlib,importlib.util,io,json,os,shlex,tempfile,unittest,urllib.request,uuid
from pathlib import Path
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[2];ASSETS=ROOT/'app/src/main/assets'
def module(name):
 s=importlib.util.spec_from_file_location(name,ASSETS/(name+'.py'));m=importlib.util.module_from_spec(s);s.loader.exec_module(m);return m
M=module('environment-official-images');U=module('environment-updates')
D='sha256:'+'a'*64
class OfficialImages(unittest.TestCase):
 def hub(self,tag,digest=D,arch='arm64',system='linux'):
  return {'name':tag,'images':[{'architecture':arch,'os':system,'digest':digest,'status':'active'}]}
 def fetch(self,data):
  def get(url,limit,headers=None):
   self.assertEqual('https',urllib.parse.urlsplit(url).scheme)
   value=data[url] if url in data else self.hub(url.rsplit('/',1)[-1])
   if isinstance(value,Exception):raise value
   return value if isinstance(value,bytes) else json.dumps(value).encode()
  return get
 def test_ubuntu_uses_numeric_released_directories_and_verifies_arm64(self):
  fetch=self.fetch({'https://releases.ubuntu.com/':b'<a href="26.04/">LTS</a><a href="26.04.1/">point</a><a href="26.10/">new</a><a href="27.04-beta/">beta</a><a href="https://evil/99.99/">bad</a>'})
  row=M.check('ubuntu',fetch);self.assertEqual('26.10',row['version']);self.assertEqual('26.10',row['series']);self.assertEqual('aarch64',row['architecture'])
 def test_debian_point_release_keeps_same_series_and_rejects_wrong_suite(self):
  url='https://ftp.debian.org/debian/dists/stable/Release'
  row=M.check('debian',self.fetch({url:b'Origin: Debian\nSuite: stable\nVersion: 13.7\nArchitectures: amd64 arm64\n'}));self.assertEqual('13',row['series']);self.assertEqual('13.7',row['version'])
  with self.assertRaisesRegex(M.Refused,'METADATA_INVALID'):M.check('debian',self.fetch({url:b'Origin: Debian\nSuite: testing\nVersion: 14\nArchitectures: arm64\n'}))
 def test_alpine_selects_minirootfs_not_raspberry_pi_or_wrong_arch(self):
  raw=b'---\n-\n  arch: aarch64\n  flavor: alpine-rpi\n  version: 9.9.9\n-\n  arch: aarch64\n  flavor: alpine-minirootfs\n  version: 3.24.3\n'
  row=M.check('alpine',self.fetch({'https://dl-cdn.alpinelinux.org/alpine/latest-stable/releases/aarch64/latest-releases.yaml':raw}));self.assertEqual('3.24.3',row['version']);self.assertEqual('3.24',row['series'])
 def test_fedora_excludes_beta_rawhide_x86_and_other_variants(self):
  base={'arch':'aarch64','variant':'Container','subvariant':'Container_Base'}
  rows=[dict(base,version='44'),dict(base,version='45 Beta'),dict(base,version='rawhide'),dict(base,version='99',arch='x86_64'),dict(base,version='90',subvariant='Container_Toolbox')]
  self.assertEqual('44',M.check('fedora',self.fetch({'https://fedoraproject.org/releases.json':rows}))['version'])
 def test_opensuse_rejects_numeric_rc_and_eol_releases(self):
  rows=[{'version':'16.1','state':'RC'},{'version':'16.0','state':'Stable'},{'version':'42.3','state':'EOL'}]
  row=M.check('opensuse',self.fetch({'https://get.opensuse.org/api/v0/distributions.json':{'Leap':rows}}));self.assertEqual('16.0',row['series'])
 def test_hub_requires_one_linux_arm64_image_and_safe_digest(self):
  url='https://hub.docker.com/v2/namespaces/library/repositories/ubuntu/tags/26.10'
  for row in [self.hub('26.10',arch='amd64'),self.hub('26.10',system='windows'),self.hub('26.10',digest='evil'),{'name':'26.10','images':self.hub('26.10')['images']*2}]:
   with self.assertRaises(M.Refused):M.hub('library/ubuntu','26.10',self.fetch({url:row}))
 def test_rolling_kali_uses_arm_digest_not_multiarch_digest_or_installed_claim(self):
  raw=json.loads((ASSETS/'environment-images.json').read_text());pinned=next(v for v in raw['images'] if v['alias']=='kali')['source'].split('@')[1]
  url='https://hub.docker.com/v2/namespaces/kalilinux/repositories/kali-rolling/tags/latest'
  data=self.hub('latest',pinned);data['digest']=D
  row=M.check('kali',self.fetch({url:data}));self.assertFalse(row['differsFromPinned']);self.assertEqual('rolling',row['kind']);self.assertNotIn('installed',row)
 def test_void_anonymous_token_stays_only_in_fixed_registry_header(self):
  calls=[];secret='anonymous-fixture-registry-token'
  def fetch(url,limit,headers=None):
   calls.append((url,headers))
   if '/token?' in url:return json.dumps({'token':secret}).encode()
   self.assertEqual({'Authorization':'Bearer '+secret,'Accept':'application/vnd.oci.image.index.v1+json, application/vnd.docker.distribution.manifest.list.v2+json'},headers)
   return json.dumps({'manifests':[{'platform':{'os':'linux','architecture':'arm64','variant':'v8'},'digest':D},{'platform':{'os':'linux','architecture':'amd64'},'digest':'sha256:'+'b'*64}]}).encode()
  row=M.check('void',fetch);self.assertNotIn(secret,json.dumps(row));self.assertEqual(D,row['digest']);self.assertTrue(all(urllib.parse.urlsplit(u).hostname=='ghcr.io' for u,h in calls))
 def test_manjaro_uses_only_stable_aarch64_rootfs_asset(self):
  url='https://api.github.com/repos/manjaro-arm/rootfs/releases/latest';tag='20261029';asset={'name':'Manjaro-ARM-aarch64-latest.tar.gz','state':'uploaded','size':10000,'browser_download_url':'https://github.com/manjaro-arm/rootfs/releases/download/'+tag+'/Manjaro-ARM-aarch64-latest.tar.gz'}
  data={'tag_name':tag,'draft':False,'prerelease':False,'assets':[asset]};row=M.check('manjaro',self.fetch({url:data}));self.assertTrue(row['newerThanPinned']);data['prerelease']=True
  with self.assertRaises(M.Refused):M.check('manjaro',self.fetch({url:data}))
 def test_arch_uses_generic_aarch64_snapshot_without_inventing_upgrade(self):
  raw=b'<a href="ArchLinuxARM-aarch64-chromebook-latest.tar.gz">x</a> 29-Oct-2026 01:00 50\n<a href="ArchLinuxARM-aarch64-latest.tar.gz">image</a> 05-Aug-2026 12:41 829367415'
  row=M.check('archlinux',self.fetch({'https://de3.mirror.archlinuxarm.org/os/':raw}));self.assertEqual('2026-08-05 12:41',row['published']);self.assertNotIn('newerThanPinned',row);self.assertNotIn('differsFromPinned',row)
 def test_source_boundaries_and_redirects_reject_http_credentials_and_other_hosts(self):
  for url in ('http://hub.docker.com/x','https://hub.docker.com.evil/x','https://user:secret@hub.docker.com/x','https://hub.docker.com:444/x','file:///etc/passwd','https://hub.docker.com/x\n'):
   with self.assertRaises((M.Refused,ValueError)):M.safe_url(url)
  handler=M.Redirect();request=urllib.request.Request('https://ghcr.io/v2/test',headers={'Authorization':'Bearer anonymous-token'})
  with self.assertRaises(M.Refused):handler.redirect_request(request,None,302,'Found',{},'https://hub.docker.com/x')
 def test_malformed_json_and_unknown_target_fail_closed(self):
  with self.assertRaises(M.Refused):M.check('ubuntu-ai',self.fetch({}))
  with self.assertRaisesRegex(M.Refused,'METADATA_INVALID'):M.fedora(lambda *a:b'not json')
 def runtime(self,path,runner):
  files=Path(path)/'files';(files/'usr').mkdir(parents=True);return U.Runtime(files,files/'usr',str(uuid.uuid4()),runner)
 def test_partial_network_failure_does_not_hide_other_sources_and_no_guest_exec(self):
  seen=[]
  def runner(args,**kw):
   self.assertEqual(['-I','-S'],args[1:3]);self.assertTrue(args[3].endswith('environment-official-images.py'));self.assertEqual('--target',args[4]);seen.append(args[5]);self.assertIsNone(kw['input_data'])
   return 0,json.dumps({'error':'OFFICIAL_NETWORK'} if args[5]=='fedora' else {'target':args[5],'kind':'rolling','version':'rolling'}).encode()
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d,contextlib.redirect_stdout(io.StringIO()):
   r=self.runtime(d,runner)
   try:result=r.official_images()
   finally:r.close()
  self.assertEqual(list(U.TARGETS),seen);self.assertEqual('OFFICIAL_NETWORK',result['targets']['fedora']['error']);self.assertEqual('opensuse',result['targets']['opensuse']['target'])
 def test_cancel_propagates_without_scanning_remaining_sources(self):
  seen=[]
  def runner(args,**kw):seen.append(args[-1]);raise U.C.Cancelled('CANCELLED')
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d,contextlib.redirect_stdout(io.StringIO()):
   r=self.runtime(d,runner)
   try:
    with self.assertRaises(U.C.Cancelled):r.official_images()
   finally:r.close()
  self.assertEqual(['ubuntu'],seen)
 def test_ndb_database_and_alias_are_fingerprinted_inside_guest_only(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   r=self.runtime(d,None);c=Path(d)/'container';database=c/'rootfs/usr/lib/sysimage/rpm';database.mkdir(parents=True);(c/'rootfs/var/lib').mkdir(parents=True);(c/'rootfs/var/lib/rpm').symlink_to('/usr/lib/sysimage/rpm');(database/'Packages.db').write_bytes(b'ndb1');(database/'Index.db').write_bytes(b'index')
   fd=U.T.m.open_dir(c)
   try:
    original=r.database(fd,'opensuse');(database/'Packages.db').write_bytes(b'ndb2');self.assertNotEqual(original,r.database(fd,'opensuse'));self.assertEqual(r.database(fd,'fedora'),r.database(fd,'opensuse'))
   finally:os.close(fd);r.close()
 def test_dnf_format_uses_real_tab_accepted_by_both_dnf_generations(self):
  words=shlex.split(U.P.QUERY['dnf']);fmt=words[words.index('--queryformat')+1];self.assertIn('\t',fmt);self.assertNotIn('\\t',fmt)
  output=fmt.replace('%{name}','libfoo').replace('%{evr}','2:1.2-3.fc44').replace('\\n','\n').encode();self.assertEqual([{'name':'libfoo','version':'2:1.2-3.fc44','installed':''}],U.P.parse('fedora',output))
  with self.assertRaises(U.P.Refused):U.P.parse('fedora',b'libfoo\\t2:1.2-3.fc44\n')

if __name__=='__main__':unittest.main()
