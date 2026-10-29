"""Bounded official Components release discovery; never installs a remote image."""
import json,re,urllib.request,urllib.error
from pathlib import Path
TARGETS={'ubuntu','debian','alpine','fedora','opensuse','void','archlinux','kali','manjaro'}
REPO='https://github.com/HezeAI/termux-jobs-components/releases/download/'
API='https://api.github.com/repos/HezeAI/termux-jobs-components/releases?per_page=30'
class Refused(Exception):pass
class Https(urllib.request.HTTPRedirectHandler):
 def redirect_request(self,req,fp,code,msg,headers,url):
  if not url.startswith('https://'):raise Refused('INSECURE_REDIRECT')
  return super().redirect_request(req,fp,code,msg,headers,url)

def get(url,limit):
 request=urllib.request.Request(url,headers={'User-Agent':'Termux-Jobs-environment-updates','Accept':'application/vnd.github+json'})
 try:
  with urllib.request.build_opener(Https()).open(request,timeout=20) as response:
   if not response.geturl().startswith('https://'):raise Refused('INSECURE_REDIRECT')
   data=response.read(limit+1)
   if len(data)>limit:raise Refused('IMAGE_METADATA_TOO_LARGE')
   return data
 except (urllib.error.URLError,TimeoutError,OSError):raise Refused('IMAGE_CHECK_NETWORK') from None

def validate(record,release=None):
 if not isinstance(record,dict) or record.get('schemaVersion')!=1 or not isinstance(record.get('images'),list):raise Refused('IMAGE_CATALOG')
 tag=record.get('release')
 if not isinstance(tag,str) or not re.fullmatch(r'rootfs-arm64-[0-9]{8}(?:-[a-z0-9-]{1,40})?',tag) or release is not None and tag!=release:raise Refused('IMAGE_CATALOG')
 result={}
 if len(record['images'])>30:raise Refused('IMAGE_CATALOG')
 for row in record['images']:
  if not isinstance(row,dict):raise Refused('IMAGE_CATALOG')
  target=row.get('alias');name=row.get('filename');size=row.get('size');sha=row.get('sha256')
  if target not in TARGETS or target in result or row.get('architecture')!='aarch64' or row.get('format') not in ('oci','rootfs-tar'):raise Refused('IMAGE_CATALOG')
  if not isinstance(name,str) or not re.fullmatch(r'[a-z0-9][a-z0-9._-]{1,140}',name) or not isinstance(sha,str) or not re.fullmatch(r'[0-9a-f]{64}',sha) or type(size) is not int or not 0<size<=8*1024**3:raise Refused('IMAGE_CATALOG')
  if row.get('url')!=REPO+tag+'/'+name:raise Refused('IMAGE_SOURCE')
  result[target]={k:row[k] for k in ('alias','filename','size','sha256','format','architecture','url')}
  result[target].update(release=tag,sourceTag=str(row.get('sourceTag',''))[:80])
 if set(result)!=TARGETS:raise Refused('IMAGE_CATALOG_INCOMPLETE')
 return result

def pinned():return validate(json.loads(Path(__file__).with_name('environment-images.json').read_text()))

def latest(fetch=get):
 releases=json.loads(fetch(API,2*1024*1024))
 if not isinstance(releases,list) or len(releases)>30:raise Refused('IMAGE_CATALOG')
 rows=[r for r in releases if isinstance(r,dict) and not r.get('draft') and not r.get('prerelease') and re.fullmatch(r'rootfs-arm64-[0-9]{8}(?:-[a-z0-9-]{1,40})?',r.get('tag_name',''))]
 if not rows:raise Refused('IMAGE_RELEASE_UNAVAILABLE')
 release=max(rows,key=lambda r:r['tag_name']);tag=release['tag_name'];assets=release.get('assets',[])
 matches=[a for a in assets if a.get('name')=='rootfs-manifest.json' and a.get('state')=='uploaded']
 if len(matches)!=1 or matches[0].get('browser_download_url')!=REPO+tag+'/rootfs-manifest.json':raise Refused('IMAGE_CATALOG')
 return validate(json.loads(fetch(matches[0]['browser_download_url'],1024*1024)),tag)
