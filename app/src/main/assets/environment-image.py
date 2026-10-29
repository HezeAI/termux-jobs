#!/usr/bin/env python3
"""Fetch only APK-pinned rootfs archives; extraction belongs to native proot-distro."""
import http.client
import fcntl,hashlib,json,os,re,stat,sys,time,urllib.error,urllib.request
from pathlib import Path
RELEASE='rootfs-arm64-20261028'
ALIASES={'jobs-x11-ubuntu':'ubuntu'}
class HttpsOnly(urllib.request.HTTPRedirectHandler):
 def redirect_request(self,req,fp,code,msg,headers,newurl):
  if not newurl.startswith('https://'):raise ValueError('IMAGE_INSECURE_REDIRECT')
  return super().redirect_request(req,fp,code,msg,headers,newurl)
def regular(path):
 info=path.lstat()
 if not stat.S_ISREG(info.st_mode) or info.st_nlink!=1 or info.st_uid!=os.getuid():raise ValueError('IMAGE_UNSAFE_FILE')
 return info
def digest(path):
 fd=os.open(path,os.O_RDONLY|os.O_NOFOLLOW)
 try:
  info=os.fstat(fd)
  if not stat.S_ISREG(info.st_mode) or info.st_nlink!=1:raise ValueError('IMAGE_UNSAFE_FILE')
  with os.fdopen(fd,'rb',closefd=False) as f:return hashlib.file_digest(f,'sha256').hexdigest()
 finally:os.close(fd)
def matches(path,sha,size):
 try:return regular(path).st_size==size and digest(path)==sha
 except FileNotFoundError:return False
def specs():
 p=Path(__file__).with_name('environment-images.json')
 if p.is_symlink() or p.stat().st_size>65536:raise ValueError('IMAGE_CATALOG')
 j=json.loads(p.read_text())
 if j.get('release')!=RELEASE:raise ValueError('IMAGE_CATALOG_VERSION')
 result={}
 for item in j['images']:
  name=item['alias'];filename=item['filename'];sha=item['sha256'];size=item['size']
  if name not in {'ubuntu','debian','alpine','archlinux','fedora','opensuse','void','kali','manjaro'} or name in result:raise ValueError('IMAGE_CATALOG_TARGET')
  if not re.fullmatch(r'[a-z0-9.-]{1,100}',filename) or not re.fullmatch('[a-f0-9]{64}',sha) or type(size) is not int or not 0<size<=2*1024**3:raise ValueError('IMAGE_CATALOG_PIN')
  result[name]=(filename,sha,size)
 return result
def directory(base,name):
 p=base/name
 try:p.mkdir(mode=0o700)
 except FileExistsError:pass
 info=p.lstat()
 if not stat.S_ISDIR(info.st_mode) or info.st_uid!=os.getuid():raise ValueError('IMAGE_CACHE_DIRECTORY')
 return p
def download(path,url,sha,size,opener=None):
 if path.is_symlink():raise ValueError('IMAGE_UNSAFE_FILE')
 print('STAGE:checking-image-cache',file=sys.stderr,flush=True)
 if matches(path,sha,size):return path
 if path.exists():regular(path);path.unlink()
 part=path.with_name(path.name+'.part')
 if part.exists() or part.is_symlink():regular(part)
 opener=opener or urllib.request.build_opener(HttpsOnly())
 for attempt in range(3):
  offset=regular(part).st_size if part.exists() else 0
  if offset and matches(part,sha,size):os.replace(part,path);return path
  if offset>=size:part.unlink();offset=0
  headers={'Range':'bytes='+str(offset)+'-'} if offset else {}
  try:
   print('STAGE:connecting-image-download',file=sys.stderr,flush=True)
   with opener.open(urllib.request.Request(url,headers=headers),timeout=30) as response:
    if not response.geturl().startswith('https://'):raise ValueError('IMAGE_INSECURE_RESPONSE')
    status=response.status
    if status==206:
     value=response.headers.get('Content-Range','')
     if not re.fullmatch('bytes '+str(offset)+r'-[0-9]+/'+str(size),value):raise ValueError('IMAGE_RANGE_MISMATCH')
    elif status==200:offset=0
    else:raise ValueError('IMAGE_HTTP_STATUS')
    fd=os.open(part,os.O_WRONLY|os.O_CREAT|os.O_NOFOLLOW|(os.O_APPEND if offset else os.O_TRUNC),0o600)
    try:
     info=os.fstat(fd)
     if not stat.S_ISREG(info.st_mode) or info.st_nlink!=1:raise ValueError('IMAGE_UNSAFE_FILE')
     done=offset;last=0
     with os.fdopen(fd,'wb',closefd=False) as f:
      # Return available bytes instead of waiting for a full 256 KiB on a
      # slow connection, so partial progress and cancellation stay visible.
      read=getattr(response,'read1',response.read)
      while chunk:=read(256*1024):
       done+=len(chunk)
       if done>size:raise ValueError('IMAGE_SIZE_LIMIT')
       f.write(chunk)
       now=time.monotonic()
       if now-last>.25 or done==size:
        last=now;print('IMAGE_PROGRESS: %d%% (%d/%d bytes)'%(done*100//size,done,size),file=sys.stderr,flush=True)
      f.flush();os.fsync(fd)
    finally:os.close(fd)
   if regular(part).st_size<size:raise OSError("incomplete response")
   print('STAGE:verifying-image-download',file=sys.stderr,flush=True)
   if not matches(part,sha,size):
    # A completed wrong archive must never be resumed or installed.
    if part.exists():part.unlink()
    raise ValueError('IMAGE_CHECKSUM_MISMATCH')
   os.replace(part,path);return path
  except (urllib.error.URLError,TimeoutError,OSError,http.client.HTTPException) as error:
   if attempt==2:raise ValueError('IMAGE_DOWNLOAD_NETWORK') from error
   print('STAGE:reconnecting-image-download',file=sys.stderr,flush=True);time.sleep(1)
 raise ValueError('IMAGE_DOWNLOAD_FAILED')
def main():
 if len(sys.argv)!=2:raise ValueError('IMAGE_ARGUMENTS')
 name=ALIASES.get(sys.argv[1],sys.argv[1]);items=specs()
 if name not in items:raise ValueError('IMAGE_TARGET')
 prefix=Path(os.environ['PREFIX']);parent=prefix.parent
 # Resolve only Android's legitimate filesDir alias. Cache components below it
 # must be owned real directories, never links to a same-named foreign tree.
 base=directory(parent,'.environment-control')
 requested=os.environ.get('JOBS_ENVIRONMENT_DOWNLOAD_DIR')
 if requested and not Path(requested).samefile(base):raise ValueError('IMAGE_CACHE_CONTROL')
 base=directory(directory(directory(base,'images'),RELEASE),name)
 lock=os.open(base/'download.lock',os.O_RDWR|os.O_CREAT|os.O_NOFOLLOW,0o600)
 try:
  info=os.fstat(lock)
  if not stat.S_ISREG(info.st_mode) or info.st_nlink!=1:raise ValueError('IMAGE_UNSAFE_LOCK')
  try:fcntl.flock(lock,fcntl.LOCK_EX|fcntl.LOCK_NB)
  except BlockingIOError:raise ValueError('IMAGE_DOWNLOAD_BUSY') from None
  filename,sha,size=items[name]
  url='https://github.com/HezeAI/termux-jobs-components/releases/download/'+RELEASE+'/'+filename
  print('STAGE:download-pinned-'+name,file=sys.stderr,flush=True)
  path=download(base/filename,url,sha,size)
  print('STAGE:verified-rootfs-sha256',file=sys.stderr,flush=True)
  print(path,flush=True)
 finally:os.close(lock)
if __name__=='__main__':
 try:main()
 except KeyboardInterrupt:
  print('ERROR:INSTALL_CANCELLED',file=sys.stderr,flush=True);sys.exit(130)
 except (ValueError,KeyError,OSError) as error:
  print('ERROR:'+str(error) if isinstance(error,ValueError) else 'ERROR:IMAGE_SETUP_FAILED',file=sys.stderr,flush=True);sys.exit(1)
