#!/usr/bin/env python3
"""Explicit, offline, descriptor-relative clone/removal of stopped proot-distro 5 systems.
No archive of credentials is produced; stdout contains counters/error codes only.
"""
import functools, contextlib, ctypes, errno, fcntl, hashlib, json, os, platform, re, shutil, stat, sys, time, uuid
DIR=os.O_RDONLY|os.O_DIRECTORY|os.O_NOFOLLOW|os.O_CLOEXEC
PATH_DIR=os.O_PATH|os.O_DIRECTORY|os.O_NOFOLLOW|os.O_CLOEXEC
FILE=os.O_RDONLY|os.O_NOFOLLOW|os.O_CLOEXEC
NAME=re.compile(r'[A-Za-z0-9][A-Za-z0-9_.-]{0,63}')
# Android host bind endpoints, never Ubuntu payload. Keep directory placeholders only.
ANDROID_MOUNTS=frozenset('rootfs/'+p for p in (
 'apex','odm','product','system','system_ext','vendor','sdcard','storage',
 'dev','proc','sys','mnt/sdcard',
 'data/app','data/dalvik-cache','data/misc/apexdata/com.android.art/dalvik-cache',
 'data/data/com.termux/files/home','data/data/com.termux/files/usr','data/data/com.termux/files/apps','data/data/com.termux/cache'))

APP_MOUNTS=frozenset('rootfs/tmp/'+n for n in (
 'termux-jobs-x11-gpu','termux-jobs-setup','termux-jobs-gemini','termux-jobs-claude','termux-jobs-ai','termux-jobs-codex'))
SESSION_MOUNT=re.compile(r'rootfs/tmp/termux-fork-session-[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}')
def mount_placeholder(path):return path in ANDROID_MOUNTS or path in APP_MOUNTS or SESSION_MOUNT.fullmatch(path) is not None

class Refused(Exception):pass
class Cancelled(Refused):pass
def access_stage(code):
 def decorate(fn):
  @functools.wraps(fn)
  def run(*args,**kwargs):
   try:return fn(*args,**kwargs)
   except PermissionError as e:
    if not hasattr(e,'migration_code'):e.migration_code=code
    raise
  return run
 return decorate

def error_event(e):
 # Error path is root-relative metadata for the local dialog, never file content.
 result=dict(event='error',code=getattr(e,'migration_code','FILES_ACCESS_DENIED'),errno=e.errno)
 if hasattr(e,'scan_path') and getattr(e,'scan_operation','') in ('list','stat','open-directory'):
  result['scanPath']=e.scan_path[:512]+('…' if len(e.scan_path)>512 else '');result['scanOperation']=e.scan_operation
 return result

def emit(**v):print(json.dumps(v,separators=(',',':')),flush=True)
def identity(s):return (s.st_dev,s.st_ino,s.st_mode,s.st_size,s.st_mtime_ns,s.st_ctime_ns)
def open_dir(path):
 fd=os.open('/',PATH_DIR)
 try:
  parts=[n for n in os.path.abspath(path).split('/')[1:] if n]
  for i,name in enumerate(parts):
   nxt=os.open(name,DIR if i==len(parts)-1 else PATH_DIR,dir_fd=fd);os.close(fd);fd=nxt
  if not parts:
   readable=os.open('.',DIR,dir_fd=fd);os.close(fd);fd=readable
  return fd
 except BaseException:os.close(fd);raise

def walk(fd,prefix=''):
 if prefix.count('/')>256:raise Refused('TREE_TOO_DEEP')
 try:names=sorted(os.listdir(fd))
 except OSError as e:
  e.scan_path=prefix;e.scan_operation='list';raise
 for n in names:
  p=prefix+n
  try:s=os.stat(n,dir_fd=fd,follow_symlinks=False)
  except OSError as e:
   e.scan_path=p;e.scan_operation='stat';raise
  yield p,s
  if stat.S_ISDIR(s.st_mode) and not mount_placeholder(p):
   try:child=os.open(n,DIR,dir_fd=fd)
   except OSError as e:
    e.scan_path=p;e.scan_operation='open-directory';raise
   try:
    if identity(os.fstat(child))!=identity(s):raise Refused('SOURCE_CHANGED')
    yield from walk(child,p+'/')
   finally:os.close(child)

def at(root,path):
 fd=os.dup(root)
 try:
  parts=path.split('/')
  for n in parts[:-1]:nxt=os.open(n,DIR,dir_fd=fd);os.close(fd);fd=nxt
  return fd,parts[-1]
 except BaseException:os.close(fd);raise

def copy_xattrs(source,destination):
 # Termux Python is built without os.*xattr; use fd-only Bionic calls there.
 if all(callable(getattr(os,n,None)) for n in ('listxattr','getxattr','setxattr')):
  for key in os.listxattr(source):
   if key!='security.selinux':os.setxattr(destination,key,os.getxattr(source,key))
  return
 libc=ctypes.CDLL(None,use_errno=True)
 listing=libc.flistxattr;listing.argtypes=(ctypes.c_int,ctypes.c_void_p,ctypes.c_size_t);listing.restype=ctypes.c_ssize_t
 getting=libc.fgetxattr;getting.argtypes=(ctypes.c_int,ctypes.c_char_p,ctypes.c_void_p,ctypes.c_size_t);getting.restype=ctypes.c_ssize_t
 setting=libc.fsetxattr;setting.argtypes=(ctypes.c_int,ctypes.c_char_p,ctypes.c_void_p,ctypes.c_size_t,ctypes.c_int);setting.restype=ctypes.c_int
 def read(fn,*args):
  for attempt in range(3):
   size=fn(*args,None,0)
   if size<0:raise OSError(ctypes.get_errno(),'xattr query refused')
   if size>1024*1024:raise Refused('XATTR_TOO_LARGE')
   buf=ctypes.create_string_buffer(max(1,size));actual=fn(*args,buf,size)
   if actual>=0:return buf.raw[:actual]
   if ctypes.get_errno()!=errno.ERANGE:raise OSError(ctypes.get_errno(),'xattr read refused')
  raise Refused('SOURCE_CHANGED')
 for key in read(listing,source).split(b'\0'):
  if not key or key==b'security.selinux':continue
  value=read(getting,source,key)
  if setting(destination,key,value,len(value),0):raise OSError(ctypes.get_errno(),'xattr write refused')

def hard_link(old,new,src_dir_fd=None,dst_dir_fd=None,follow_symlinks=False):
 if callable(getattr(os,'link',None)):
  return os.link(old,new,src_dir_fd=src_dir_fd,dst_dir_fd=dst_dir_fd,follow_symlinks=follow_symlinks)
 if follow_symlinks:raise Refused('UNSAFE_LINK')
 libc=ctypes.CDLL(None,use_errno=True);fn=libc.linkat
 fn.argtypes=(ctypes.c_int,ctypes.c_char_p,ctypes.c_int,ctypes.c_char_p,ctypes.c_int);fn.restype=ctypes.c_int
 if fn(-100 if src_dir_fd is None else src_dir_fd,os.fsencode(old),-100 if dst_dir_fd is None else dst_dir_fd,os.fsencode(new),0):raise OSError(ctypes.get_errno(),'hardlink creation refused')

def rename_new(parent,old,new):
 # Atomic no-replace publication, including an existing empty target directory.
 libc=ctypes.CDLL(None,use_errno=True)
 fn=getattr(libc,'renameat2',None)
 args=(parent,os.fsencode(old),parent,os.fsencode(new),1)
 if fn is not None:result=fn(*args)
 else:
  nr={'aarch64':276,'arm64':276,'x86_64':316}.get(platform.machine())
  if nr is None:raise Refused('ATOMIC_RENAME_UNAVAILABLE')
  result=libc.syscall(nr,*args)
 if result:raise OSError(ctypes.get_errno(),'atomic publication refused')

def remove_tree(parent,name):
 if not re.fullmatch(r'\.jobs-(?:ai|delete)-[0-9a-f]{32}',name):raise Refused('UNSAFE_CLEANUP_NAME')
 if not shutil.rmtree.avoids_symlink_attacks:raise Refused('SAFE_DELETE_UNAVAILABLE')
 def opened(parent,name):
  pin=os.open(name,PATH_DIR,dir_fd=parent)
  try:
   original=os.fstat(pin);mode=stat.S_IMODE(original.st_mode)|0o700
   if mode!=stat.S_IMODE(original.st_mode):
    # O_PATH pins the selected directory even when its permissions deny read.
    # Proc fd refers to that inode, never to a guest-controlled symlink name.
    try:os.fchmod(pin,mode)
    except OSError as e:
     if e.errno!=errno.EBADF:raise
     os.chmod('/proc/self/fd/'+str(pin),mode)
   read=os.open('.',DIR,dir_fd=pin)
   if (os.fstat(read).st_dev,os.fstat(read).st_ino)!=(original.st_dev,original.st_ino):
    os.close(read);raise Refused('SOURCE_CHANGED')
   return read
  finally:os.close(pin)
 fd=opened(parent,name)
 def writable(d):
  for n in os.listdir(d):
   try:s=os.stat(n,dir_fd=d,follow_symlinks=False)
   except OSError as e:
    if e.errno not in (errno.EPERM,errno.EACCES):raise
    # Unlink an unreadable entry itself, never its target. A directory refuses.
    os.unlink(n,dir_fd=d);continue
   if stat.S_ISDIR(s.st_mode):
    child=opened(d,n)
    try:writable(child)
    finally:os.close(child)
 try:
  original=os.fstat(fd);writable(fd)
  current=os.stat(name,dir_fd=parent,follow_symlinks=False)
  if (current.st_dev,current.st_ino)!=(original.st_dev,original.st_ino):raise Refused('SOURCE_CHANGED')
  shutil.rmtree(name,dir_fd=parent)
 finally:os.close(fd)
 os.fsync(parent)

class Runtime:
 def __init__(self,base,cancel=lambda:False,progress=emit):self.base=os.path.abspath(base);self.cancel=cancel;self.progress=progress;self.last=0
 def tick(self,phase,done=0,total=0,files=0):
  if self.cancel():raise Cancelled('CANCELLED')
  now=time.monotonic()
  if now-self.last>.15:self.progress(event='progress',phase=phase,done=done,total=total,files=files);self.last=now
 @contextlib.contextmanager
 def locked(self,names):
  if not all(NAME.fullmatch(n) for n in names):raise Refused('INVALID_ALIAS')
  try:base=open_dir(self.base)
  except PermissionError as e:raise Refused('STORAGE_ACCESS_DENIED') from e
  try:
   try:
    try:os.mkdir('locks',0o700,dir_fd=base)
    except FileExistsError:pass
    locks=os.open('locks',DIR,dir_fd=base)
   except PermissionError as e:
    e.migration_code='LOCKS_ACCESS_DENIED';raise
   handles=[]
   try:
    for n in sorted(set(names)):
     try:fd=os.open(n+'.lock',os.O_RDWR|os.O_CREAT|os.O_NOFOLLOW|os.O_CLOEXEC,0o600,dir_fd=locks)
     except PermissionError as e:
      e.migration_code='LOCKS_ACCESS_DENIED';raise
     handles.append(fd)
     s=os.fstat(fd)
     if not stat.S_ISREG(s.st_mode) or s.st_nlink!=1 or s.st_uid!=os.getuid():raise Refused('UNSAFE_LOCK')
     try:fcntl.flock(fd,fcntl.LOCK_EX|fcntl.LOCK_NB)
     except BlockingIOError:raise Refused('ENVIRONMENT_BUSY')
    yield base
   finally:
    for fd in handles:os.close(fd)
    os.close(locks)
  finally:os.close(base)
 @access_stage('PROCESS_CHECK_DENIED')
 def idle(self,alias):
  source=(self.base+'/containers/'+alias+'/rootfs').encode()
  for p in os.listdir('/proc'):
   if not p.isdecimal():continue
   proc='/proc/'+p
   try:
    if os.stat(proc).st_uid!=os.getuid():continue
    with open(proc+'/cmdline','rb') as f:args=f.read(128*1024).split(b'\0')
    if args and os.path.basename(args[0]) in (b'proot',b'proot-distro') and any(source in arg for arg in args):raise Refused('ENVIRONMENT_BUSY')
   except (FileNotFoundError,ProcessLookupError):continue
   except PermissionError as e:raise Refused('PROCESS_CHECK_DENIED') from e
 @access_stage('SCAN_ACCESS_DENIED')
 def snapshot(self,fd):
  result={};size=0
  for p,s in walk(fd):
   self.tick('scan',size,0,len(result))
   if stat.S_ISREG(s.st_mode):size+=s.st_size
   elif not (stat.S_ISDIR(s.st_mode) or stat.S_ISLNK(s.st_mode) or stat.S_ISSOCK(s.st_mode)):raise Refused('UNSUPPORTED_SPECIAL_FILE')
   result[p]=identity(s)
  digest=hashlib.sha256()
  for p,s in result.items():digest.update(os.fsencode(p)+b'\0'+repr(s).encode()+b'\n')
  return result,size,digest.hexdigest()
 def prepare_empty_workspace(self,root):
  # Fixed legacy empty placeholder only. rmdir atomically refuses any payload.
  try:s=os.stat('workspace',dir_fd=root,follow_symlinks=False)
  except FileNotFoundError:return False
  if not stat.S_ISDIR(s.st_mode) or stat.S_IMODE(s.st_mode)!=0:return False
  try:os.rmdir('workspace',dir_fd=root)
  except OSError as e:
   if e.errno in (errno.ENOTEMPTY,errno.EEXIST,errno.ENOENT):return False
   raise
  os.fsync(root);return True
 @access_stage('ENVIRONMENT_ACCESS_DENIED')
 def plan(self,alias='ubuntu',prepare=False):
  with self.locked([alias]) as base:
   self.idle(alias)
   parent=os.open('containers',DIR,dir_fd=base)
   try:
    src=os.open(alias,DIR,dir_fd=parent)
    try:
     self.tick('prepare')
     root=os.open('rootfs',DIR,dir_fd=src)
     try:cleaned=self.prepare_empty_workspace(root) if prepare and alias=='ubuntu' else False
     finally:os.close(root)
     entries,size,token=self.snapshot(src)
     return dict(event='plan',alias=alias,bytes=size,files=len(entries),token=token,emptyWorkspaceRemoved=cleaned,free=os.fstatvfs(parent).f_bavail*os.fstatvfs(parent).f_frsize)
    finally:os.close(src)
   finally:os.close(parent)
 @access_stage('COPY_ACCESS_DENIED')
 def clone(self,token):
  with self.locked(['ubuntu','ubuntu-ai']) as base:
   self.idle('ubuntu');self.idle('ubuntu-ai')
   parent=os.open('containers',DIR,dir_fd=base);src=None;stage=None
   try:
    try:os.stat('ubuntu-ai',dir_fd=parent,follow_symlinks=False)
    except FileNotFoundError:pass
    else:raise Refused('DESTINATION_EXISTS')
    src=os.open('ubuntu',DIR,dir_fd=parent)
    entries,size,actual=self.snapshot(src)
    if actual!=token:raise Refused('SOURCE_CHANGED')
    space=os.fstatvfs(parent)
    if space.f_bavail*space.f_frsize<size+256*1024*1024:raise Refused('NOT_ENOUGH_SPACE')
    # OCI metadata and shell/package database must exist before any publication.
    manifest=os.open('manifest.json',FILE,dir_fd=src)
    try:
     if os.fstat(manifest).st_size>1024*1024:raise Refused('INVALID_MANIFEST')
     with os.fdopen(os.dup(manifest)) as f:data=json.load(f)
     if not isinstance(data,dict):raise Refused('INVALID_MANIFEST')
    finally:os.close(manifest)
    root=os.open('rootfs',DIR,dir_fd=src)
    try:
     for required in ['etc/os-release','var/lib/dpkg/status']:
      d,n=at(root,required)
      try:
       st=os.stat(n,dir_fd=d,follow_symlinks=False)
       if not stat.S_ISREG(st.st_mode) and not (required=='etc/os-release' and stat.S_ISLNK(st.st_mode)):raise Refused('INVALID_ROOTFS')
      finally:os.close(d)
    finally:os.close(root)
    stage='.jobs-ai-'+uuid.uuid4().hex;os.mkdir(stage,0o700,dir_fd=parent);dst=os.open(stage,DIR,dir_fd=parent)
    try:
     self.copy(src,dst,entries,size)
     if self.snapshot(src)[2]!=token:raise Refused('SOURCE_CHANGED')
     if os.stat('ubuntu',dir_fd=parent,follow_symlinks=False).st_ino!=os.fstat(src).st_ino:raise Refused('SOURCE_CHANGED')
     self.idle('ubuntu');self.idle('ubuntu-ai')
     self.tick('publish',size,size,len(entries))
     os.fsync(dst)
     rename_new(parent,stage,'ubuntu-ai');stage=None;os.fsync(parent)
    finally:os.close(dst)
    return dict(event='done',alias='ubuntu-ai',bytes=size,files=len(entries))
   finally:
    if stage is not None:remove_tree(parent,stage)
    if src is not None:os.close(src)
    os.close(parent)
 def copy(self,src,dst,entries,total):
  done=0;hard={};dirs=[]
  old=self.base+'/containers/ubuntu';new=self.base+'/containers/ubuntu-ai'
  for p,expected in entries.items():
   a,n=at(src,p);b,m=at(dst,p)
   try:
    s=os.stat(n,dir_fd=a,follow_symlinks=False)
    if identity(s)!=expected:raise Refused('SOURCE_CHANGED')
    mode=s.st_mode
    if stat.S_ISDIR(mode):os.mkdir(m,0o700,dir_fd=b);dirs.append((p,s))
    elif stat.S_ISLNK(mode):
     target=os.readlink(n,dir_fd=a)
     if target==old or target.startswith(old+'/'):target=new+target[len(old):]
     os.symlink(target,m,dir_fd=b)
    elif stat.S_ISSOCK(mode):continue # runtime sockets are recreated, never credentials.
    elif stat.S_ISREG(mode):
     key=(s.st_dev,s.st_ino)
     if s.st_nlink>1 and key in hard:hard_link(hard[key],m,src_dir_fd=dst,dst_dir_fd=b,follow_symlinks=False);done+=s.st_size
     else:
      f=os.open(n,FILE,dir_fd=a);g=os.open(m,os.O_RDWR|os.O_CREAT|os.O_EXCL|os.O_NOFOLLOW,0o600,dir_fd=b)
      try:
       if identity(os.fstat(f))!=expected:raise Refused('SOURCE_CHANGED')
       digest=hashlib.sha256()
       while True:
        block=os.read(f,1024*1024)
        if not block:break
        digest.update(block);view=memoryview(block)
        while view:view=view[os.write(g,view):]
        done+=len(block);self.tick('copy',done,total,len(hard))
       if identity(os.fstat(f))!=expected:raise Refused('SOURCE_CHANGED')
       os.lseek(g,0,os.SEEK_SET);check=hashlib.sha256()
       while True:
        block=os.read(g,1024*1024)
        if not block:break
        check.update(block);self.tick('verify',done,total,len(hard))
       if digest.digest()!=check.digest():raise Refused('COPY_VERIFY_FAILED')
       copy_xattrs(f,g)
       os.fchmod(g,stat.S_IMODE(mode));os.utime(g,ns=(s.st_atime_ns,s.st_mtime_ns));os.fsync(g)
       hard[key]=p
      finally:os.close(f);os.close(g)
    else:raise Refused('UNSUPPORTED_SPECIAL_FILE')
    self.tick('copy',done,total,len(hard))
   finally:os.close(a);os.close(b)
  for p,s in reversed(dirs):
   b,n=at(dst,p)
   try:
    f=os.open(n,DIR,dir_fd=b)
    original=None
    try:
     if not mount_placeholder(p):
      a,m=at(src,p)
      try:original=os.open(m,DIR,dir_fd=a)
      finally:os.close(a)
      copy_xattrs(original,f)
     os.fchmod(f,stat.S_IMODE(s.st_mode));os.utime(f,ns=(s.st_atime_ns,s.st_mtime_ns));os.fsync(f)
    finally:
     os.close(f)
     if original is not None:os.close(original)
   finally:os.close(b)
 @access_stage('ENVIRONMENT_ACCESS_DENIED')
 def remove(self,alias,token):
  if not NAME.fullmatch(alias):raise Refused('INVALID_ALIAS')
  with self.locked([alias]) as base:
   self.idle(alias)
   parent=os.open('containers',DIR,dir_fd=base)
   try:
    src=os.open(alias,DIR,dir_fd=parent)
    try:
     original=os.fstat(src)
     if self.snapshot(src)[2]!=token:raise Refused('SOURCE_CHANGED')
     self.idle(alias)
     self.tick('delete')
     name='.jobs-delete-'+uuid.uuid4().hex;rename_new(parent,alias,name);os.fsync(parent)
     moved=os.stat(name,dir_fd=parent,follow_symlinks=False)
     if (moved.st_dev,moved.st_ino)!=(original.st_dev,original.st_ino):
      rename_new(parent,name,alias)
      raise Refused('SOURCE_CHANGED')
     # Deletion is non-cancellable after quarantine. Never follow guest symlinks.
     try:remove_tree(parent,name)
     except OSError as e:raise Refused('DELETE_INCOMPLETE') from e
     return dict(event='done',alias=alias)
    finally:os.close(src)
   finally:os.close(parent)

def main():
 if len(sys.argv)!=5:raise Refused('INVALID_REQUEST')
 op,alias,token,cancel=sys.argv[1:]
 prefix=os.environ.get('PREFIX','')
 if prefix!='/data/data/com.termux/files/usr':raise Refused('INVALID_PREFIX')
 # Require the native v5 locking/storage contract; never mutate unsupported layouts.
 from proot_distro.constants import RUNTIME_DIR
 from proot_distro.locking import LOCKS_DIR
 base=prefix+'/var/lib/proot-distro'
 if RUNTIME_DIR!=base or LOCKS_DIR!=base+'/locks':raise Refused('NATIVE_LOCKS_REQUIRED')
 if not NAME.fullmatch(alias):raise Refused('INVALID_ALIAS')
 runtime=Runtime(base,lambda:os.path.lexists(cancel))
 if op=='plan' and token in ('','prepare-migration'):
  if token and alias!='ubuntu':raise Refused('INVALID_REQUEST')
  result=runtime.plan(alias,prepare=token=='prepare-migration')
 elif op=='clone' and alias=='ubuntu' and re.fullmatch('[0-9a-f]{64}',token):result=runtime.clone(token)
 elif op=='remove' and re.fullmatch('[0-9a-f]{64}',token):result=runtime.remove(alias,token)
 else:raise Refused('INVALID_REQUEST')
 emit(**result)
if __name__=='__main__':
 try:main()
 except Cancelled:emit(event='error',code='CANCELLED');sys.exit(2)
 except (ImportError,ModuleNotFoundError):emit(event='error',code='NATIVE_LOCKS_REQUIRED');sys.exit(1)
 except PermissionError as e:emit(**error_event(e));sys.exit(1)
 except FileNotFoundError as e:emit(event='error',code='ENVIRONMENT_PATH_MISSING',errno=e.errno);sys.exit(1)
 except OSError as e:emit(event='error',code='FILESYSTEM_ERROR',errno=e.errno);sys.exit(1)
 except BaseException as e:
  emit(event='error',code=str(e) if isinstance(e,Refused) else 'OPERATION_FAILED');sys.exit(1)
