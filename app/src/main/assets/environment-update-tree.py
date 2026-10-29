"""Descriptor-relative container copies; no guest code or ordinary symlink targets."""
import ctypes,errno,hashlib,importlib.util,json,os,re,stat,uuid
from pathlib import Path
s=importlib.util.spec_from_file_location('jobs_migration_tree',Path(__file__).with_name('agent-environment.py'));m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
DIR=m.DIR;FILE=m.FILE
class Refused(Exception):pass

def ident(st):return [st.st_dev,st.st_ino]
def write_json(parent,name,value):
 if not re.fullmatch(r'[a-zA-Z0-9_.-]+',name):raise Refused('CONTROL_PATH')
 data=json.dumps(value,sort_keys=True,separators=(',',':')).encode();tmp=name+'.'+uuid.uuid4().hex+'.new'
 fd=os.open(tmp,os.O_WRONLY|os.O_CREAT|os.O_EXCL|os.O_NOFOLLOW,0o600,dir_fd=parent)
 try:
  with os.fdopen(fd,'wb') as out:out.write(data);out.flush();os.fsync(out.fileno())
  try:
   before=os.stat(name,dir_fd=parent,follow_symlinks=False)
   if not stat.S_ISREG(before.st_mode) or before.st_nlink!=1:raise Refused('CONTROL_FILE')
  except FileNotFoundError:pass
  os.replace(tmp,name,src_dir_fd=parent,dst_dir_fd=parent);os.fsync(parent)
 finally:
  try:os.unlink(tmp,dir_fd=parent)
  except FileNotFoundError:pass

def read_json(parent,name,limit=4*1024*1024):
 fd=os.open(name,FILE|os.O_NONBLOCK,dir_fd=parent)
 with os.fdopen(fd,'rb') as f:
  st=os.fstat(f.fileno())
  if not stat.S_ISREG(st.st_mode) or st.st_nlink!=1 or st.st_size>limit:raise Refused('CONTROL_FILE')
  return json.load(f)

def mkdir(parent,name,private=True):
 try:os.mkdir(name,0o700,dir_fd=parent)
 except FileExistsError:pass
 fd=os.open(name,DIR,dir_fd=parent);st=os.fstat(fd)
 if st.st_uid!=os.getuid() or private and stat.S_IMODE(st.st_mode)&0o077:os.close(fd);raise Refused('PRIVATE_DIRECTORY')
 return fd

def scan(fd,tick=lambda *a:None):
 rows={};total=0;links={}
 for name,st in m.walk(fd):
  tick('scan',len(rows),0)
  if len(rows)>=1000000:raise Refused('TOO_MANY_FILES')
  if stat.S_ISREG(st.st_mode):
   total+=st.st_size;key=(st.st_dev,st.st_ino);n,expected=links.get(key,(0,st.st_nlink));links[key]=(n+1,expected)
  elif not (stat.S_ISDIR(st.st_mode) or stat.S_ISLNK(st.st_mode) or stat.S_ISSOCK(st.st_mode) or stat.S_ISFIFO(st.st_mode)):raise Refused('SPECIAL_FILE')
  rows[name]=m.identity(st)
 if any(n!=expected for n,expected in links.values()):raise Refused('EXTERNAL_HARDLINKS')
 token=hashlib.sha256(json.dumps(rows,sort_keys=True).encode()).hexdigest()
 return rows,total,token

def exchange(parent,left,right):
 if not all(re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_.-]{0,80}',x) for x in (left,right)):raise Refused('ALIAS')
 libc=ctypes.CDLL(None,use_errno=True);fn=getattr(libc,'renameat2',None)
 if fn is None:
  import platform
  number={'aarch64':276,'armv8l':382,'armv7l':382,'x86_64':316,'i686':353}.get(platform.machine())
  if number is None:raise Refused('ATOMIC_EXCHANGE_UNAVAILABLE')
  raw=libc.syscall;raw.restype=ctypes.c_long
  fn=lambda a,b,c,d,flags:raw(ctypes.c_long(number),ctypes.c_int(a),ctypes.c_char_p(b),ctypes.c_int(c),ctypes.c_char_p(d),ctypes.c_uint(flags))
 fn.argtypes=(ctypes.c_int,ctypes.c_char_p,ctypes.c_int,ctypes.c_char_p,ctypes.c_uint);fn.restype=ctypes.c_int
 if fn(parent,os.fsencode(left),parent,os.fsencode(right),2):raise OSError(ctypes.get_errno(),'atomic exchange refused')
 os.fsync(parent)

def publish(src_parent,name,dst_parent,new_name):
 libc=ctypes.CDLL(None,use_errno=True);fn=getattr(libc,'renameat2',None)
 if fn is None:
  import platform
  number={'aarch64':276,'armv8l':382,'armv7l':382,'x86_64':316,'i686':353}.get(platform.machine())
  if number is None:raise Refused('ATOMIC_EXCHANGE_UNAVAILABLE')
  raw=libc.syscall;raw.restype=ctypes.c_long
  fn=lambda a,b,c,d,flags:raw(ctypes.c_long(number),ctypes.c_int(a),ctypes.c_char_p(b),ctypes.c_int(c),ctypes.c_char_p(d),ctypes.c_uint(flags))
 fn.argtypes=(ctypes.c_int,ctypes.c_char_p,ctypes.c_int,ctypes.c_char_p,ctypes.c_uint);fn.restype=ctypes.c_int
 if fn(src_parent,os.fsencode(name),dst_parent,os.fsencode(new_name),1):raise OSError(ctypes.get_errno(),'atomic publication refused')
 os.fsync(src_parent);os.fsync(dst_parent)

def l2s_entry(root,p,target,original,path_result=False):
 # Relocate only PRoot backing links. Never open an ordinary symlink target,
 # including a link into /sdcard or another container with the same basename.
 if not Path(target).name.startswith(('.proot.l2s.','.l2s.')):return None
 originals=[original] if isinstance(original,str) else original
 matched=next((p for p in originals if target.startswith(p+'/')),None)
 if matched is not None:relative=target[len(matched)+1:]
 elif not target.startswith('/'):
  import posixpath
  relative=posixpath.normpath(posixpath.join(str(Path(p).parent),target))
 else:raise Refused('L2S_TARGET')
 parts=relative.split('/')
 if any(x in ('','..','.') for x in parts) or not relative.startswith('rootfs/'):raise Refused('L2S_TARGET')
 if '/.l2s/' not in '/'+relative and str(Path(relative).parent)!=str(Path(p).parent):raise Refused('L2S_TARGET')
 parent,name=m.at(root,relative)
 try:
  fd=os.open(name,FILE,dir_fd=parent);st=os.fstat(fd)
  if not stat.S_ISREG(st.st_mode):os.close(fd);raise Refused('L2S_TARGET')
  return (fd,st,relative) if path_result else (fd,st)
 finally:os.close(parent)

def materialize_managed_links(root,original,tick=lambda *a:None):
 """Turn private PRoot backing links into real hardlinks before alias rename."""
 for count,(path,st) in enumerate(m.walk(root)):
  tick('validating',count,0)
  if not stat.S_ISLNK(st.st_mode):continue
  parent,name=m.at(root,path)
  try:
   target=os.readlink(name,dir_fd=parent);entry=l2s_entry(root,path,target,original,path_result=True)
   if entry is None:continue
   fd,backing,relative=entry;src,source_name=m.at(root,relative);tmp='.jobs-l2s-'+uuid.uuid4().hex
   try:
    m.hard_link(source_name,tmp,src_dir_fd=src,dst_dir_fd=parent,follow_symlinks=False)
    created=os.stat(tmp,dir_fd=parent,follow_symlinks=False)
    if not stat.S_ISREG(created.st_mode) or ident(created)!=ident(backing):raise Refused('L2S_TARGET')
    if os.readlink(name,dir_fd=parent)!=target:raise Refused('SOURCE_CHANGED')
    os.replace(tmp,name,src_dir_fd=parent,dst_dir_fd=parent)
   finally:
    os.close(src);os.close(fd)
    try:os.unlink(tmp,dir_fd=parent)
    except FileNotFoundError:pass
  finally:os.close(parent)

def clone(src,dst,rows,total,original,tick=lambda *a:None):
 done=0;hard={};directories=[]
 for path,expected in rows.items():
  tick('copy',done,total)
  a,name=m.at(src,path);b,out=m.at(dst,path)
  try:
   st=os.stat(name,dir_fd=a,follow_symlinks=False)
   if m.identity(st)!=tuple(expected):raise Refused('SOURCE_CHANGED')
   if stat.S_ISDIR(st.st_mode):os.mkdir(out,0o700,dir_fd=b);directories.append((path,st));continue
   if stat.S_ISSOCK(st.st_mode):continue
   if stat.S_ISFIFO(st.st_mode):os.mkfifo(out,stat.S_IMODE(st.st_mode),dir_fd=b);continue
   source=None
   if stat.S_ISLNK(st.st_mode):
    target=os.readlink(name,dir_fd=a);managed=l2s_entry(src,path,target,original)
    if managed is None:os.symlink(target,out,dir_fd=b);continue
    source,st=managed
   elif stat.S_ISREG(st.st_mode):source=os.open(name,FILE,dir_fd=a)
   else:raise Refused('SPECIAL_FILE')
   try:
    if m.identity(os.fstat(source))!=m.identity(st):raise Refused('SOURCE_CHANGED')
    key=(st.st_dev,st.st_ino)
    if key in hard:m.hard_link(hard[key],out,src_dir_fd=dst,dst_dir_fd=b,follow_symlinks=False);continue
    with os.fdopen(os.dup(source),'rb') as data:
     current=m.identity(os.fstat(source));destination=os.open(out,os.O_RDWR|os.O_CREAT|os.O_EXCL|os.O_NOFOLLOW,0o600,dir_fd=b)
     try:
      digest=hashlib.sha256()
      while chunk:=data.read(1024*1024):
       digest.update(chunk);view=memoryview(chunk)
       while view:view=view[os.write(destination,view):]
       done+=len(chunk);tick('copy',done,total)
      if m.identity(os.fstat(source))!=current:raise Refused('SOURCE_CHANGED')
      os.lseek(destination,0,os.SEEK_SET);check=hashlib.sha256()
      while chunk:=os.read(destination,1024*1024):check.update(chunk);tick('verify',done,total)
      if digest.digest()!=check.digest():raise Refused('COPY_VERIFY_FAILED')
      m.copy_xattrs(source,destination);os.fchmod(destination,stat.S_IMODE(st.st_mode));os.utime(destination,ns=(st.st_atime_ns,st.st_mtime_ns));os.fsync(destination);hard[key]=path
     finally:os.close(destination)
   finally:
    if source is not None:os.close(source)
  finally:os.close(a);os.close(b)
 for path,st in reversed(directories):
  parent,name=m.at(dst,path)
  try:
   child=os.open(name,DIR,dir_fd=parent)
   try:
    source_parent,source_name=m.at(src,path)
    try:source_dir=os.open(source_name,DIR,dir_fd=source_parent)
    finally:os.close(source_parent)
    try:m.copy_xattrs(source_dir,child)
    finally:os.close(source_dir)
    os.fchmod(child,stat.S_IMODE(st.st_mode));os.utime(child,ns=(st.st_atime_ns,st.st_mtime_ns));os.fsync(child)
   finally:os.close(child)
  finally:os.close(parent)
 os.fsync(dst)

def guest_open(container,path):
 """Resolve guest absolute/relative links with chroot semantics, never host paths."""
 root=os.open('rootfs',DIR,dir_fd=container);todo=path.lstrip('/').split('/');resolved=[];links=0
 try:
  while todo:
   part=todo.pop(0)
   if part in ('','.'):continue
   if part=='..':
    if resolved:resolved.pop()
    continue
   current=os.dup(root)
   try:
    for name in resolved:
     nxt=os.open(name,DIR,dir_fd=current);os.close(current);current=nxt
    st=os.stat(part,dir_fd=current,follow_symlinks=False)
    if stat.S_ISLNK(st.st_mode):
     links+=1
     if links>40:raise Refused('GUEST_SYMLINK_LOOP')
     target=os.readlink(part,dir_fd=current)
     if target.startswith('/'):resolved=[]
     todo=target.split('/')+todo;continue
    if todo:
     if not stat.S_ISDIR(st.st_mode):raise Refused('GUEST_PATH')
     resolved.append(part)
    else:
     fd=os.open(part,FILE|os.O_NONBLOCK,dir_fd=current)
     if not stat.S_ISREG(os.fstat(fd).st_mode):os.close(fd);raise Refused('GUEST_FILE')
     return fd
   finally:os.close(current)
  raise Refused('GUEST_PATH')
 finally:os.close(root)

def guest_read(container,path,limit=8*1024*1024):
 fd=guest_open(container,path)
 with os.fdopen(fd,'rb') as f:
  if os.fstat(f.fileno()).st_size>limit:raise Refused('GUEST_FILE_TOO_LARGE')
  data=f.read(limit+1)
  if len(data)>limit:raise Refused('GUEST_FILE_TOO_LARGE')
  return data

def guest_executable(container,path):
 try:
  fd=guest_open(container,path)
  try:return bool(os.fstat(fd).st_mode&0o111)
  finally:os.close(fd)
 except (OSError,Refused):return False
