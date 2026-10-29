#!/usr/bin/env python3
"""Read-only upstream ARM64 image discovery. No archives, installs or guest commands."""
import datetime, hashlib, json, re, ssl, sys, time
import urllib.error, urllib.parse, urllib.request
from html.parser import HTMLParser
from pathlib import Path

TARGETS=('ubuntu','debian','kali','alpine','fedora','opensuse','void','archlinux','manjaro')
HOSTS={'releases.ubuntu.com','ftp.debian.org','dl-cdn.alpinelinux.org',
 'fedoraproject.org','get.opensuse.org','hub.docker.com','ghcr.io',
 'api.github.com','de3.mirror.archlinuxarm.org','registry-1.docker.io','auth.docker.io'}
DIGEST=re.compile(r'sha256:[0-9a-f]{64}')
NUMBER=re.compile(r'[0-9]{1,4}(?:\.[0-9]{1,4}){0,3}')
DOCS={
 'ubuntu':'https://releases.ubuntu.com/', 'debian':'https://www.debian.org/releases/stable/',
 'kali':'https://www.kali.org/docs/containers/official-kalilinux-docker-images/',
 'alpine':'https://alpinelinux.org/downloads/', 'fedora':'https://fedoraproject.org/',
 'opensuse':'https://get.opensuse.org/leap/', 'void':'https://github.com/void-linux/void-containers',
 'archlinux':'https://archlinuxarm.org/about/downloads', 'manjaro':'https://github.com/manjaro-arm/rootfs/releases',
}
class Refused(Exception): pass

def safe_url(url):
 p=urllib.parse.urlsplit(url)
 if p.scheme!='https' or p.hostname not in HOSTS or p.port not in (None,443) or p.username or p.password or p.fragment or any(ord(c)<=32 for c in url):
  raise Refused('OFFICIAL_SOURCE_INVALID')
 return p.hostname

class Redirect(urllib.request.HTTPRedirectHandler):
 def redirect_request(self,req,fp,code,msg,headers,url):
  # Never forward even an anonymous registry bearer to a different host.
  if safe_url(url)!=safe_url(req.full_url):raise Refused('OFFICIAL_SOURCE_INVALID')
  return super().redirect_request(req,fp,code,msg,headers,url)

def get(url,limit=1024*1024,headers=None):
 safe_url(url)
 request=urllib.request.Request(url,headers=dict({'User-Agent':'Termux-Jobs-official-images','Accept':'application/json'},**(headers or {})))
 try:
  with urllib.request.build_opener(Redirect()).open(request,timeout=15) as r:
   safe_url(r.geturl());data=r.read(limit+1)
   if len(data)>limit:raise Refused('OFFICIAL_METADATA_TOO_LARGE')
   return data
 except urllib.error.HTTPError as e:
  raise Refused('OFFICIAL_RATE_LIMIT' if e.code in (403,429) else 'OFFICIAL_SOURCE_UNAVAILABLE') from None
 except (ssl.SSLError,urllib.error.URLError,TimeoutError,OSError):raise Refused('OFFICIAL_NETWORK') from None

def load(url,fetch=get,limit=1024*1024,headers=None):
 try:return json.loads(fetch(url,limit,headers))
 except (ValueError,UnicodeError):raise Refused('OFFICIAL_METADATA_INVALID') from None

def numeric(value):
 if not isinstance(value,str) or not NUMBER.fullmatch(value):raise Refused('OFFICIAL_METADATA_INVALID')
 return tuple(int(x) for x in value.split('.'))

def newest(values):
 if not values:raise Refused('OFFICIAL_IMAGE_UNAVAILABLE')
 return max(values,key=numeric)

def hub(repo,tag,fetch):
 # Repo is a constant and tag has been parsed as a number or fixed channel.
 if tag not in ('latest','stable') and not NUMBER.fullmatch(tag):raise Refused('OFFICIAL_METADATA_INVALID')
 data=load('https://hub.docker.com/v2/namespaces/'+repo.replace('/', '/repositories/',1)+'/tags/'+tag,fetch)
 if not isinstance(data,dict) or data.get('name')!=tag:raise Refused('OFFICIAL_METADATA_INVALID')
 images=data.get('images',[])
 if not isinstance(images,list) or len(images)>100:raise Refused('OFFICIAL_METADATA_INVALID')
 arm=[v for v in images if isinstance(v,dict) and v.get('architecture')=='arm64' and v.get('os')=='linux' and v.get('variant') in (None,'','v8') and v.get('status','active')=='active']
 if len(arm)!=1 or not DIGEST.fullmatch(arm[0].get('digest','')):raise Refused('OFFICIAL_ARM64_UNAVAILABLE')
 return {'digest':arm[0]['digest'],'image':'docker.io/'+repo+':'+tag}

class Links(HTMLParser):
 def __init__(self):super().__init__();self.links=[]
 def handle_starttag(self,tag,attrs):
  if tag=='a':self.links.extend(v for k,v in attrs if k=='href' and isinstance(v,str))

def ubuntu(fetch):
 raw=fetch('https://releases.ubuntu.com/',1024*1024,None).decode('utf-8');links=Links();links.feed(raw)
 versions=[v[:-1] for v in links.links if re.fullmatch(r'[0-9]{2}\.[0-9]{2}(?:\.[0-9]{1,2})?/',v)]
 version=newest(versions);series='.'.join(version.split('.')[:2])
 return dict(hub('library/ubuntu',series,fetch),version=version,series=series,kind='release')

def debian(fetch):
 raw=fetch('https://ftp.debian.org/debian/dists/stable/Release',1024*1024,None).decode('utf-8')
 fields=dict(re.findall(r'^([A-Za-z-]+): ([^\n]*)$',raw,re.M))
 if fields.get('Origin')!='Debian' or fields.get('Suite')!='stable' or 'arm64' not in fields.get('Architectures','').split():raise Refused('OFFICIAL_METADATA_INVALID')
 version=fields.get('Version','');numeric(version)
 return dict(hub('library/debian','stable',fetch),version=version,series=version.split('.')[0],kind='release')

def alpine(fetch):
 raw=fetch('https://dl-cdn.alpinelinux.org/alpine/latest-stable/releases/aarch64/latest-releases.yaml',256*1024,None).decode('utf-8')
 # Read only simple scalar fields from the documented list; no YAML execution,
 # tags, aliases, multiline data or dependency on a third-party YAML package.
 rows=[dict(re.findall(r'^  (arch|version|flavor|file|sha256): ([A-Za-z0-9._-]+)$',block,re.M)) for block in re.split(r'^-\s*$',raw,flags=re.M)]
 rows=[v for v in rows if v.get('arch')=='aarch64' and v.get('flavor')=='alpine-minirootfs']
 if len(rows)!=1:raise Refused('OFFICIAL_METADATA_INVALID')
 version=rows[0].get('version','');numeric(version);series='.'.join(version.split('.')[:2])
 return dict(hub('library/alpine',series,fetch),version=version,series=series,kind='release')

def fedora(fetch):
 rows=load('https://fedoraproject.org/releases.json',fetch,2*1024*1024)
 if not isinstance(rows,list) or len(rows)>10000:raise Refused('OFFICIAL_METADATA_INVALID')
 versions=[v['version'] for v in rows if isinstance(v,dict) and re.fullmatch(r'[0-9]{2,3}',str(v.get('version',''))) and v.get('arch')=='aarch64' and v.get('variant')=='Container' and v.get('subvariant')=='Container_Base']
 version=newest(versions)
 return dict(hub('library/fedora',version,fetch),version=version,series=version,kind='release')

def opensuse(fetch):
 data=load('https://get.opensuse.org/api/v0/distributions.json',fetch)
 if not isinstance(data,dict) or not isinstance(data.get('Leap'),list):raise Refused('OFFICIAL_METADATA_INVALID')
 versions=[v['version'] for v in data['Leap'] if isinstance(v,dict) and v.get('state')=='Stable' and isinstance(v.get('version'),str) and NUMBER.fullmatch(v['version'])]
 version=newest(versions)
 return dict(hub('opensuse/leap',version,fetch),version=version,series=version,kind='release')

def void(fetch):
 token=load('https://ghcr.io/token?service=ghcr.io&scope=repository:void-linux/void-glibc-full:pull',fetch,64*1024).get('token')
 if not isinstance(token,str) or not 1<len(token)<16000 or re.search(r'[\r\n\x00]',token):raise Refused('OFFICIAL_METADATA_INVALID')
 headers={'Authorization':'Bearer '+token,'Accept':'application/vnd.oci.image.index.v1+json, application/vnd.docker.distribution.manifest.list.v2+json'}
 data=load('https://ghcr.io/v2/void-linux/void-glibc-full/manifests/latest',fetch,1024*1024,headers)
 if not isinstance(data,dict) or not isinstance(data.get('manifests'),list):raise Refused('OFFICIAL_METADATA_INVALID')
 rows=[v for v in data['manifests'] if isinstance(v,dict) and isinstance(v.get('platform'),dict) and v['platform'].get('os')=='linux' and v['platform'].get('architecture')=='arm64' and v['platform'].get('variant') in (None,'','v8')]
 if len(rows)!=1 or not DIGEST.fullmatch(rows[0].get('digest','')):raise Refused('OFFICIAL_ARM64_UNAVAILABLE')
 return dict(digest=rows[0]['digest'],image='ghcr.io/void-linux/void-glibc-full:latest',kind='rolling',version='rolling')

def manjaro(fetch):
 data=load('https://api.github.com/repos/manjaro-arm/rootfs/releases/latest',fetch)
 if not isinstance(data,dict) or data.get('draft') or data.get('prerelease') or not re.fullmatch('[0-9]{8}',str(data.get('tag_name',''))):raise Refused('OFFICIAL_METADATA_INVALID')
 version=data['tag_name'];datetime.datetime.strptime(version,'%Y%m%d')
 assets=data.get('assets',[])
 if not isinstance(assets,list):raise Refused('OFFICIAL_METADATA_INVALID')
 rows=[v for v in assets if isinstance(v,dict) and v.get('name')=='Manjaro-ARM-aarch64-latest.tar.gz' and v.get('state')=='uploaded']
 url='https://github.com/manjaro-arm/rootfs/releases/download/'+version+'/Manjaro-ARM-aarch64-latest.tar.gz'
 if len(rows)!=1 or rows[0].get('browser_download_url')!=url:raise Refused('OFFICIAL_ARM64_UNAVAILABLE')
 digest=rows[0].get('digest')
 if digest is not None and (not isinstance(digest,str) or not DIGEST.fullmatch(digest)):raise Refused('OFFICIAL_METADATA_INVALID')
 size=rows[0].get('size')
 if type(size) is not int or not 0<size<=8*1024**3:raise Refused('OFFICIAL_METADATA_INVALID')
 return dict(kind='rolling',version=version,image=url,size=size,**({'digest':digest} if digest else {}))

def archlinux(fetch):
 # Official ARM mirror, not x86_64 archlinux.org disk images. No TLS bypass if
 # the central os.archlinuxarm.org endpoint has an invalid certificate.
 url='https://de3.mirror.archlinuxarm.org/os/'
 raw=fetch(url,1024*1024,None).decode('utf-8')
 match=re.search(r'<a href="ArchLinuxARM-aarch64-latest\.tar\.gz">[^<]+</a>\s+([0-9]{2}-[A-Za-z]{3}-[0-9]{4} [0-9]{2}:[0-9]{2})\s+([0-9]+)',raw)
 if not match or not 0<int(match[2])<=8*1024**3:raise Refused('OFFICIAL_ARM64_UNAVAILABLE')
 published=datetime.datetime.strptime(match[1],'%d-%b-%Y %H:%M').strftime('%Y-%m-%d %H:%M')
 signature=fetch(url+'ArchLinuxARM-aarch64-latest.tar.gz.sig',64*1024,None)
 if not signature:raise Refused('OFFICIAL_METADATA_INVALID')
 return dict(kind='rolling',version='rolling',published=published,image=url+'ArchLinuxARM-aarch64-latest.tar.gz',size=int(match[2]),signatureSha256=hashlib.sha256(signature).hexdigest())

REPOS={'ubuntu':'docker.io/library/ubuntu','debian':'docker.io/library/debian',
 'kali':'docker.io/kalilinux/kali-rolling','alpine':'docker.io/library/alpine',
 'fedora':'docker.io/library/fedora','opensuse':'docker.io/opensuse/leap',
 'void':'ghcr.io/void-linux/void-glibc-full'}

def install_source(target,row):
 """Validate a server-held preview again before it can reach a native CLI."""
 if target not in TARGETS or row.get('target')!=target or row.get('architecture')!='aarch64' or row.get('error'):raise Refused('OFFICIAL_PLAN_INVALID')
 if target in REPOS:
  if not DIGEST.fullmatch(row.get('digest','')) or not row.get('image','').startswith(REPOS[target]+':'):raise Refused('OFFICIAL_PLAN_INVALID')
  return {'kind':'oci','reference':REPOS[target]+'@'+row['digest']}
 if target=='manjaro':
  version=row.get('version','');digest=row.get('digest','')
  if not re.fullmatch('[0-9]{8}',version) or not DIGEST.fullmatch(digest):raise Refused('OFFICIAL_CHECKSUM_REQUIRED')
  url='https://github.com/manjaro-arm/rootfs/releases/download/'+version+'/Manjaro-ARM-aarch64-latest.tar.gz'
  if row.get('image')!=url:raise Refused('OFFICIAL_PLAN_INVALID')
  size=row.get('size')
  if type(size) is not int or not 0<size<=8*1024**3:raise Refused('OFFICIAL_PLAN_INVALID')
  return {'kind':'archive','url':url,'sha256':digest[7:],'size':size}
 url='https://de3.mirror.archlinuxarm.org/os/ArchLinuxARM-aarch64-latest.tar.gz'
 if row.get('image')!=url or type(row.get('size')) is not int or not 0<row['size']<=8*1024**3 or not re.fullmatch('[0-9a-f]{64}',row.get('signatureSha256','')):raise Refused('OFFICIAL_PLAN_INVALID')
 return {'kind':'signed-archive','url':url,'size':row['size'],'signatureSha256':row['signatureSha256']}

def cache_key(target,row):return hashlib.sha256(json.dumps(install_source(target,row),sort_keys=True,separators=(',',':')).encode()).hexdigest()

def check(target,fetch=get):
 if target not in TARGETS:raise Refused('TARGET')
 if target=='kali':row=dict(hub('kalilinux/kali-rolling','latest',fetch),kind='rolling',version='rolling')
 else:row=globals()[target](fetch)
 row.update(target=target,source=DOCS[target],architecture='aarch64',checkedAt=int(time.time()))
 # A changed rolling digest is compared ONLY with the APK's clean base, not
 # with an installed mutable environment or a multi-platform index digest.
 pinned=json.loads(Path(__file__).with_name('environment-images.json').read_text())
 base=next(v for v in pinned['images'] if v['alias']==target)
 digest=base.get('source','').rsplit('@',1)[-1]
 if DIGEST.fullmatch(digest) and 'digest' in row:row['differsFromPinned']=digest!=row['digest']
 if target=='manjaro':
  match=re.search(r'/releases/download/([0-9]{8})/',base.get('source',''))
  if match:row['newerThanPinned']=row['version']>match[1]
 return row

if __name__=='__main__':
 try:
  if len(sys.argv)!=3 or sys.argv[1]!='--target':raise Refused('ARGUMENTS')
  print(json.dumps(check(sys.argv[2]),separators=(',',':')))
 except (Refused,ValueError,KeyError,TypeError,UnicodeError,StopIteration) as e:
  code=str(e) if isinstance(e,Refused) else 'OFFICIAL_METADATA_INVALID'
  print(json.dumps({'error':code},separators=(',',':')))
