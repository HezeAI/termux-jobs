#!/usr/bin/env python3
"""Pinned private musl VirGL client. No package-manager/rootfs writes."""
import hashlib,io,os,shutil,stat,sys,tempfile,zipfile,importlib.util,signal
from pathlib import Path,PurePosixPath
PIN='d226941f42b135c4e5cd58218b50758beb132e5a46d37f4aec8972a481c33739'
NAME='client-'+PIN[:16]
LIMIT=64*1024*1024

def regular(path,limit):
 fd=os.open(path,os.O_RDONLY|os.O_NOFOLLOW|os.O_NONBLOCK)
 with os.fdopen(fd,'rb') as src:
  st=os.fstat(src.fileno())
  if not stat.S_ISREG(st.st_mode) or st.st_size>limit:raise ValueError('VIRGL_CLIENT_FILE')
  data=src.read(limit+1)
  if len(data)>limit:raise ValueError('VIRGL_CLIENT_LIMIT')
  return data

def directory(path):
 if path.is_symlink():raise ValueError('VIRGL_CLIENT_LINK')
 path.mkdir(mode=0o700,exist_ok=True)
 if not path.is_dir():raise ValueError('VIRGL_CLIENT_DIRECTORY')

def payload(archive):
 data=regular(archive,16*1024*1024)
 if hashlib.sha256(data).hexdigest()!=PIN:raise ValueError('VIRGL_CLIENT_CHECKSUM')
 result={};total=0
 with zipfile.ZipFile(io.BytesIO(data)) as z:
  if len(z.infolist())>32:raise ValueError('VIRGL_CLIENT_COUNT')
  for info in z.infolist():
   p=PurePosixPath(info.filename)
   if p.is_absolute() or '..' in p.parts or info.filename in result or info.is_dir() or stat.S_ISLNK(info.external_attr>>16):raise ValueError('VIRGL_CLIENT_ENTRY')
   if p.parts[0]!='lib' or len(p.parts) not in (2,3) or len(p.parts)==3 and p.parts[1]!='dri':raise ValueError('VIRGL_CLIENT_ENTRY')
   total+=info.file_size
   if total>LIMIT:raise ValueError('VIRGL_CLIENT_LIMIT')
   value=z.read(info)
   if value[:4]!=b'\x7fELF' or int.from_bytes(value[18:20],'little')!=183 or int.from_bytes(value[16:18],'little')!=3:raise ValueError('VIRGL_CLIENT_ABI')
   result[info.filename]=value
 if not {'lib/libGL.so.1','lib/libEGL.so.1','lib/libgallium-26.1.6.so','lib/dri/swrast_dri.so'}<=result.keys():raise ValueError('VIRGL_CLIENT_MISSING')
 if b'virpipe\0' not in result['lib/libgallium-26.1.6.so']:raise ValueError('VIRGL_CLIENT_NO_DRIVER')
 return result

def ready(root,files):
 if root.is_symlink() or not root.is_dir():return False
 try:
  for name,data in files.items():
   path=root
   for part in PurePosixPath(name).parts:
    path=path/part
    if path.is_symlink():return False
   if regular(path,len(data))!=data:return False
  return True
 except OSError:return False

def prepare(base,archive,install):
 if not archive.exists():
  spec=importlib.util.spec_from_file_location('jobs_extras',Path(__file__).with_name('x11-extras.py'));extras=importlib.util.module_from_spec(spec);spec.loader.exec_module(extras)
  try:extras.ensure('virgl-alpine',archive,base.parent/'workspace-x11-extras',install)
  except ValueError as error:
   if str(error)=='EXTRAS_REQUIRED':raise ValueError('VIRGL_CLIENT_REQUIRED') from None
   raise
 files=payload(archive)
 if base.is_symlink():raise ValueError('VIRGL_CLIENT_LINK')
 dest=base/NAME
 if ready(dest,files):return dest
 if not install:raise ValueError('VIRGL_CLIENT_REQUIRED')
 directory(base)
 if dest.exists() or dest.is_symlink():raise ValueError('VIRGL_CLIENT_CACHE_INVALID')
 stage=Path(tempfile.mkdtemp(prefix='.client-',dir=base))
 try:
  for name,data in files.items():
   path=stage/name;path.parent.mkdir(mode=0o700,parents=True,exist_ok=True)
   with path.open('xb') as out:out.write(data);out.flush();os.fsync(out.fileno())
   path.chmod(0o600)
  if not ready(stage,files):raise ValueError('VIRGL_CLIENT_CHECKSUM')
  os.rename(stage,dest)
 finally:
  if stage.exists():shutil.rmtree(stage)
 return dest

if __name__=='__main__':
 signal.signal(signal.SIGTERM,lambda *_:sys.exit(143))
 try:
  op,archive=sys.argv[1:]
  if op not in ('check','install'):raise ValueError('VIRGL_CLIENT_ARGUMENT')
  prefix=Path(os.environ['PREFIX'])
  if str(prefix)!='/data/data/com.termux/files/usr':raise ValueError('VIRGL_CLIENT_PREFIX')
  prepare(prefix.parent/'workspace-x11-virgl-alpine',Path(archive),op=='install')
  print('JOBS_VIRGL_CLIENT='+NAME)
 except (OSError,ValueError,KeyError,zipfile.BadZipFile) as e:
  reason=str(e) if isinstance(e,ValueError) and str(e).startswith(('VIRGL_','EXTRAS_')) else 'VIRGL_CLIENT_FAILED'
  print('ERROR:'+reason,flush=True);raise SystemExit(1)
