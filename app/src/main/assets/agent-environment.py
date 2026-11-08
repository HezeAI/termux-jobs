#!/usr/bin/env python3
"""Explicit, offline, descriptor-relative removal of stopped proot-distro 5 systems.
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
    if not hasattr(e,'operation_code'):e.operation_code=code
    raise
  return run
 return decorate

def error_event(e):
 # Error path is root-relative metadata for the local dialog, never file content.
 result=dict(event='error',code=getattr(e,'operation_code','FILES_ACCESS_DENIED'),errno=e.errno)
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

SEALED_SYSTEMD_DIRECTORY='rootfs/run/systemd/dissect-root'
def sealed_systemd(path,info):
 return path==SEALED_SYSTEMD_DIRECTORY and stat.S_ISDIR(info.st_mode) and stat.S_IMODE(info.st_mode)==0

def walk(fd,prefix='',sealed=None):
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
  if sealed is not None and sealed_systemd(p,s):
   sealed.append(p)
   continue
  if stat.S_ISDIR(s.st_mode) and not mount_placeholder(p):
   try:child=os.open(n,DIR,dir_fd=fd)
   except OSError as e:
    e.scan_path=p;e.scan_operation='open-directory';raise
   try:
    if identity(os.fstat(child))!=identity(s):raise Refused('SOURCE_CHANGED')
    yield from walk(child,p+'/',sealed)
   finally:os.close(child)

def at(root,path):
 fd=os.dup(root)
 try:
  parts=path.split('/')
  for n in parts[:-1]:nxt=os.open(n,DIR,dir_fd=fd);os.close(fd);fd=nxt
  return fd,parts[-1]
 except BaseException:os.close(fd);raise



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
    e.operation_code='LOCKS_ACCESS_DENIED';raise
   handles=[]
   try:
    for n in sorted(set(names)):
     try:fd=os.open(n+'.lock',os.O_RDWR|os.O_CREAT|os.O_NOFOLLOW|os.O_CLOEXEC,0o600,dir_fd=locks)
     except PermissionError as e:
      e.operation_code='LOCKS_ACCESS_DENIED';raise
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
 def snapshot(self,fd,sealed=None):
  result={};size=0
  for p,s in walk(fd,sealed=sealed):
   self.tick('scan',size,0,len(result))
   if stat.S_ISREG(s.st_mode):size+=s.st_size
   elif not (stat.S_ISDIR(s.st_mode) or stat.S_ISLNK(s.st_mode) or stat.S_ISSOCK(s.st_mode)):raise Refused('UNSUPPORTED_SPECIAL_FILE')
   result[p]=identity(s)
  digest=hashlib.sha256()
  for p,s in result.items():digest.update(os.fsencode(p)+b'\0'+repr(s).encode()+b'\n')
  return result,size,digest.hexdigest()
 @access_stage('ENVIRONMENT_ACCESS_DENIED')
 def plan(self,alias='ubuntu'):
  with self.locked([alias]) as base:
   self.idle(alias)
   parent=os.open('containers',DIR,dir_fd=base)
   try:
    src=os.open(alias,DIR,dir_fd=parent)
    try:
     sealed=[];entries,size,token=self.snapshot(src,sealed)
     return dict(event='plan',alias=alias,container='containers/'+alias,unscanned=sealed,bytes=size,files=len(entries),token=token,free=os.fstatvfs(parent).f_bavail*os.fstatvfs(parent).f_frsize)
    finally:os.close(src)
   finally:os.close(parent)
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
     sealed=[]
     if self.snapshot(src,sealed)[2]!=token:raise Refused('SOURCE_CHANGED')
     self.idle(alias)
     self.tick('delete')
     name='.jobs-delete-'+uuid.uuid4().hex;rename_new(parent,alias,name);os.fsync(parent)
     moved=os.stat(name,dir_fd=parent,follow_symlinks=False)
     if (moved.st_dev,moved.st_ino)!=(original.st_dev,original.st_ino):
      rename_new(parent,name,alias)
      raise Refused('SOURCE_CHANGED')
     # The only unscanned directory is a sealed systemd mount placeholder.
     # rmdir verifies emptiness in the kernel without reading or chmod. If it
     # contains anything, restore the original container before any tree removal.
     try:
      for path in sealed:
       directory,leaf=at(src,path)
       try:
        if not sealed_systemd(path,os.stat(leaf,dir_fd=directory,follow_symlinks=False)):raise Refused('SOURCE_CHANGED')
        os.rmdir(leaf,dir_fd=directory)
       finally:os.close(directory)
     except (OSError,Refused) as error:
      rename_new(parent,name,alias);os.fsync(parent)
      if isinstance(error,OSError) and error.errno in (errno.ENOTEMPTY,errno.EEXIST):raise Refused('SEALED_DIRECTORY_NOT_EMPTY') from error
      raise
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
 if op=='plan' and token=='':result=runtime.plan(alias)
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
