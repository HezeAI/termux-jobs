"""Fetch one approved upstream archive; hashes/signatures precede native extraction."""
import contextlib,hashlib,importlib.util,io,json,os,stat,subprocess,sys,tarfile,urllib.request,urllib.parse
from pathlib import Path
def module(name):
 s=importlib.util.spec_from_file_location(name,Path(__file__).with_name(name+'.py'));m=importlib.util.module_from_spec(s);s.loader.exec_module(m);return m
O=module('environment-official-images');T=module('environment-update-tree');F=module('environment-image')
FPR='68B3537F39A313B3E574D06777193F152BDBE6A6'

class Blobs(urllib.request.HTTPRedirectHandler):
 def redirect_request(self,req,fp,code,msg,headers,url):
  p=urllib.parse.urlsplit(url);host=p.hostname or ''
  if p.scheme!='https' or p.port not in (None,443) or p.username or p.password or not any(host==suffix or host.endswith('.'+suffix) for suffix in ('docker.io','docker.com','cloudflarestorage.com','githubusercontent.com','blob.core.windows.net','ghcr.io')):raise O.Refused('OFFICIAL_SOURCE_INVALID')
  new=super().redirect_request(req,fp,code,msg,headers,url)
  if p.hostname!=urllib.parse.urlsplit(req.full_url).hostname:
   new.remove_header('Authorization');new.remove_header('Cookie')
  return new

def blob(directory,name,url,size,sha,headers):
 path=directory/name
 if verified_file(path,size,sha):return path
 tmp=directory/(name+'.part');fd=os.open(tmp,os.O_WRONLY|os.O_CREAT|os.O_NOFOLLOW|os.O_NONBLOCK,0o600)
 with os.fdopen(fd,'wb') as out:
  st=os.fstat(out.fileno())
  if not stat.S_ISREG(st.st_mode) or st.st_nlink!=1:raise O.Refused('CONTROL_FILE')
  os.ftruncate(out.fileno(),0);digest=hashlib.sha256();total=0
  req=urllib.request.Request(url,headers=headers)
  with urllib.request.build_opener(Blobs()).open(req,timeout=30) as response:
   while data:=response.read(1024*1024):
    total+=len(data)
    if total>size:raise O.Refused('IMAGE_CHECKSUM')
    digest.update(data);out.write(data)
  if total!=size or digest.hexdigest()!=sha:raise O.Refused('IMAGE_CHECKSUM')
  out.flush();os.fsync(out.fileno())
 os.replace(tmp,path);return path

def oci(directory,target,row):
 repo=O.REPOS[target];digest=row['digest']
 if repo.startswith('docker.io/'):
  repo=repo[len('docker.io/'):];host='registry-1.docker.io';token_url='https://auth.docker.io/token?service=registry.docker.io&scope=repository:'+repo+':pull'
 else:
  repo=repo[len('ghcr.io/'):];host='ghcr.io';token_url='https://ghcr.io/token?service=ghcr.io&scope=repository:'+repo+':pull'
 token=O.load(token_url,limit=64*1024).get('token')
 if not isinstance(token,str) or not 1<len(token)<16000 or '\n' in token or '\r' in token:raise O.Refused('OFFICIAL_METADATA_INVALID')
 headers={'Authorization':'Bearer '+token,'User-Agent':'Termux-Jobs-official-images','Accept':'application/vnd.oci.image.manifest.v1+json, application/vnd.docker.distribution.manifest.v2+json'}
 url='https://'+host+'/v2/'+repo
 raw=O.get(url+'/manifests/'+digest,1024*1024,headers)
 if hashlib.sha256(raw).hexdigest()!=digest[7:]:raise O.Refused('IMAGE_CHECKSUM')
 manifest=json.loads(raw)
 if not isinstance(manifest,dict) or manifest.get('schemaVersion')!=2 or not isinstance(manifest.get('layers'),list) or len(manifest['layers'])>128 or not isinstance(manifest.get('config'),dict):raise O.Refused('OFFICIAL_METADATA_INVALID')
 descriptors=[manifest['config']]+manifest['layers'];total=0;paths={}
 if type(manifest['config'].get('size')) is not int or not 0<manifest['config']['size']<=4*1024**2:raise O.Refused('OFFICIAL_METADATA_INVALID')
 for item in descriptors:
  if not isinstance(item,dict) or not O.DIGEST.fullmatch(item.get('digest','')) or type(item.get('size')) is not int or not 0<item['size']<=4*1024**3:raise O.Refused('OFFICIAL_METADATA_INVALID')
  total+=item['size']
  if total>8*1024**3:raise O.Refused('OFFICIAL_METADATA_TOO_LARGE')
  sha=item['digest'][7:];paths[sha]=blob(directory,sha,url+'/blobs/'+item['digest'],item['size'],sha,headers)
 config=json.loads(paths[manifest['config']['digest'][7:]].read_bytes())
 if config.get('architecture')!='arm64' or config.get('os')!='linux' or (config.get('config') or {}).get('Entrypoint') not in (None,[]):raise O.Refused('OFFICIAL_ARM64_UNAVAILABLE')
 index={'schemaVersion':2,'manifests':[{'mediaType':manifest.get('mediaType','application/vnd.oci.image.manifest.v1+json'),'digest':digest,'size':len(raw),'platform':{'architecture':'arm64','os':'linux'}}]}
 path=directory/'rootfs.oci.tar';tmp=directory/'rootfs.oci.part';fd=os.open(tmp,os.O_WRONLY|os.O_CREAT|os.O_NOFOLLOW|os.O_NONBLOCK,0o600)
 with os.fdopen(fd,'wb') as out:
  st=os.fstat(out.fileno())
  if not stat.S_ISREG(st.st_mode) or st.st_nlink!=1:raise O.Refused('CONTROL_FILE')
  os.ftruncate(out.fileno(),0)
  with tarfile.open(fileobj=out,mode='w') as tar:
   for name,data in [('oci-layout',b'{"imageLayoutVersion":"1.0.0"}'),('index.json',json.dumps(index).encode()),('blobs/sha256/'+digest[7:],raw)]:
    info=tarfile.TarInfo(name);info.size=len(data);info.mode=0o600;tar.addfile(info,io.BytesIO(data))
   for sha,file in paths.items():
    opened=os.open(file,T.FILE)
    with os.fdopen(opened,'rb') as f:
     info=tarfile.TarInfo('blobs/sha256/'+sha);info.size=os.fstat(f.fileno()).st_size;info.mode=0o600;tar.addfile(info,f)
  out.flush();os.fsync(out.fileno())
 os.replace(tmp,path);sha=hashlib.sha256()
 with path.open('rb') as f:
  while data:=f.read(1024*1024):sha.update(data)
 return {'path':str(path),'sha256':sha.hexdigest(),'size':path.stat().st_size,'manifestDigest':digest}

def verified_file(path,size,sha):
 try:
  fd=os.open(path,T.FILE|os.O_NONBLOCK);digest=hashlib.sha256()
  with os.fdopen(fd,'rb') as f:
   st=os.fstat(f.fileno())
   if not stat.S_ISREG(st.st_mode) or st.st_nlink!=1 or st.st_size!=size:return False
   while data:=f.read(1024*1024):digest.update(data)
  return digest.hexdigest()==sha
 except FileNotFoundError:return False

def signature_verify(directory,archive,signature,prefix):
 tool=Path(prefix)/'bin/gpgv'
 if not tool.is_file():raise O.Refused('OFFICIAL_GPG_REQUIRED')
 home=directory/'gnupg';home.mkdir(mode=0o700,exist_ok=True)
 if home.is_symlink() or stat.S_IMODE(home.stat().st_mode)&0o077:raise O.Refused('CONTROL_FILE')
 key=Path(__file__).with_name('environment-archlinuxarm.gpg')
 result=subprocess.run([str(tool),'--homedir',str(home),'--status-fd','1','--keyring',str(key),str(signature),str(archive)],stdin=subprocess.DEVNULL,capture_output=True,timeout=60)
 valid=[line.split() for line in result.stdout.decode('ascii',errors='replace').splitlines() if line.startswith('[GNUPG:] VALIDSIG ')]
 if result.returncode or not any(row[2]==FPR or row[-1]==FPR for row in valid):raise O.Refused('OFFICIAL_SIGNATURE_INVALID')

def download(directory,target,row,prefix):
 source=O.install_source(target,row)
 if source['kind']=='oci':return oci(directory,target,row)
 path=directory/'rootfs.tar.gz';size=source['size']
 if source['kind']=='archive':
  with contextlib.redirect_stdout(sys.stderr):F.download(path,source['url'],source['sha256'],size)
  if not verified_file(path,size,source['sha256']):raise O.Refused('IMAGE_CHECKSUM')
  return {'path':str(path),'sha256':source['sha256'],'size':size}
 # Freeze the approved detached signature, not just a mutable latest URL.
 sig=O.get(source['url']+'.sig',64*1024)
 if hashlib.sha256(sig).hexdigest()!=source['signatureSha256']:raise O.Refused('OFFICIAL_PLAN_CHANGED')
 signature=directory/'rootfs.sig'
 fd=os.open(signature,os.O_RDWR|os.O_CREAT|os.O_NOFOLLOW|os.O_NONBLOCK,0o600)
 with os.fdopen(fd,'r+b') as f:
  st=os.fstat(f.fileno())
  if not stat.S_ISREG(st.st_mode) or st.st_nlink!=1:raise O.Refused('CONTROL_FILE')
  f.truncate(0);f.write(sig)
 # Reuse a complete cached file only after re-verifying its signature below.
 if not path.exists():
  tmp=directory/'rootfs.part';fd=os.open(tmp,os.O_WRONLY|os.O_CREAT|os.O_NOFOLLOW|os.O_NONBLOCK,0o600)
  try:
   st=os.fstat(fd)
   if not stat.S_ISREG(st.st_mode) or st.st_nlink!=1:os.close(fd);raise O.Refused('CONTROL_FILE')
   os.ftruncate(fd,0)
   req=urllib.request.Request(source['url'],headers={'User-Agent':'Termux-Jobs-official-images'})
   with os.fdopen(fd,'wb') as out,urllib.request.build_opener(O.Redirect()).open(req,timeout=30) as response:
    O.safe_url(response.geturl());total=0
    while data:=response.read(1024*1024):
     total+=len(data)
     if total>size:raise O.Refused('IMAGE_CHECKSUM')
     out.write(data)
    if total!=size:raise O.Refused('IMAGE_CHECKSUM')
    out.flush();os.fsync(out.fileno())
   os.replace(tmp,path)
  finally:
   try:tmp.unlink()
   except FileNotFoundError:pass
 opened=os.open(path,T.FILE|os.O_NONBLOCK)
 try:
  st=os.fstat(opened)
  if not stat.S_ISREG(st.st_mode) or st.st_nlink!=1 or st.st_size!=size:raise O.Refused('IMAGE_CHECKSUM')
 finally:os.close(opened)
 signature_verify(directory,path,signature,prefix)
 fd=os.open(path,T.FILE);digest=hashlib.sha256()
 with os.fdopen(fd,'rb') as f:
  st=os.fstat(f.fileno())
  if not stat.S_ISREG(st.st_mode) or st.st_nlink!=1 or st.st_size!=size:raise O.Refused('IMAGE_CHECKSUM')
  while data:=f.read(1024*1024):digest.update(data)
 return {'path':str(path),'sha256':digest.hexdigest(),'size':size}

def main():
 if len(sys.argv)!=3:raise O.Refused('ARGUMENTS')
 plan=Path(sys.argv[1]);prefix=Path(sys.argv[2]).resolve(strict=True)
 # The parent opens the plan by descriptor too; the worker has no free-form URL.
 parent=T.m.open_dir(plan.parent)
 try:record=T.read_json(parent,plan.name,128*1024)
 finally:os.close(parent)
 directory=plan.parent/('download-'+O.cache_key(record['target'],record['image']));directory.mkdir(mode=0o700,exist_ok=True)
 check=T.m.open_dir(directory)
 try:
  if stat.S_IMODE(os.fstat(check).st_mode)&0o077:raise O.Refused('CONTROL_FILE')
 finally:os.close(check)
 return download(directory,record['target'],record['image'],prefix)

if __name__=='__main__':
 try:print(json.dumps(main(),separators=(',',':')))
 except Exception as e:print(json.dumps({'error':str(e) if isinstance(e,O.Refused) else 'OFFICIAL_DOWNLOAD_FAILED'}))
