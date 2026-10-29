#!/usr/bin/env python3
"""Explicit native environment updates. Only validated working copies are published."""
import contextlib,fcntl,hashlib,importlib.util,json,os,re,signal,stat,sys,time,uuid
from pathlib import Path
HERE=Path(__file__).parent
def module(name):
 spec=importlib.util.spec_from_file_location(name.replace('-','_'),HERE/(name+'.py'));value=importlib.util.module_from_spec(spec);spec.loader.exec_module(value);return value
T=module('environment-update-tree');P=module('environment-update-packages');I=module('environment-update-images');O=module('environment-official-images');C=module('environment-update-process');B=module('workspace-backup')
TARGETS=tuple(P.KINDS);UUID=re.compile('[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}')
DESKTOP=('xfdesktop','xfce4-panel','xfsettingsd','xfconf-query','python3','thunar','xfce4-terminal','xwininfo','xprop','xfwm4','xfce4-session','dbus-run-session','dbus-monitor','fcitx5','xauth')
OSIDS={'ubuntu':('ubuntu',),'debian':('debian',),'kali':('kali',),'alpine':('alpine',),'fedora':('fedora',),'opensuse':('opensuse-leap','opensuse-tumbleweed'),'void':('void',),'archlinux':('archarm','arch'),'manjaro':('manjaro-arm','manjaro')}
class Refused(Exception):pass
STOP=False
def emit(**row):print(json.dumps(row,separators=(',',':')),flush=True)
def uuid_ok(value):
 if not isinstance(value,str) or not UUID.fullmatch(value):raise Refused('TRANSACTION_ID')
 return value
def signal_cancel(*_):
 global STOP
 STOP=True

class Runtime:
 def __init__(self,files,prefix,job_id,runner=None):
  self.files=Path(files);self.prefix=Path(prefix);self.id=uuid_ok(job_id);self.runner=runner or C.run;self.last=0;self.phase='';self.target='';self.started=time.monotonic();self.job=None;self.control=None;self.runtime=None;self.containers=None;self.state={}
  if self.prefix.name!='usr' or not self.prefix.parent.samefile(self.files):raise Refused('PRIVATE_STORAGE_ALIAS')
  self.files=self.files.resolve(strict=True);self.prefix=self.prefix.resolve(strict=True)
  if not self.prefix.parent.samefile(self.files):raise Refused('PRIVATE_STORAGE_ALIAS')
  self.root=self.files/'workspace-environment-updates';self.base=self.prefix/'var/lib/proot-distro'
  root=T.m.open_dir(str(self.files))
  try:self.control=T.mkdir(root,'workspace-environment-updates')
  finally:os.close(root)
  for name in ('jobs','plans','official-plans','snapshots','trash','origins'):
   fd=T.mkdir(self.control,name);os.close(fd)
  jobs=os.open('jobs',T.DIR,dir_fd=self.control)
  try:self.job=T.mkdir(jobs,self.id)
  finally:os.close(jobs)
  self.cancel_path=self.root/'jobs'/self.id/'cancel'
 def close(self):
  for fd in (self.job,self.containers,self.runtime,self.control):
   if fd is not None:os.close(fd)
 def cancelled(self):return STOP or self.cancel_path.exists()
 def tick(self,phase,done=0,total=0):
  if self.cancelled():raise C.Cancelled('CANCELLED')
  now=time.monotonic()
  if phase!=self.phase or now-self.last>.3:
   self.phase=phase;self.last=now;event=dict(event='progress',target=self.target,phase=phase,processed=max(0,done),total=max(0,total),elapsed=int(now-self.started))
   emit(**event)
   if self.state:self.state.update(phase=phase,processed=max(0,done),total=max(0,total),elapsed=event['elapsed']);self.save()
 def save(self):T.write_json(self.job,'state.json',self.state)
 def native(self):
  if not B.locked_remove_available(self.prefix):raise Refused('NATIVE_LOCKS_REQUIRED')
  if self.runtime is None:self.runtime=T.m.open_dir(str(self.base))
  if self.containers is None:self.containers=os.open('containers',T.DIR,dir_fd=self.runtime)
  if os.fstat(self.control).st_dev!=os.fstat(self.containers).st_dev:raise Refused('ATOMIC_FILESYSTEM_REQUIRED')
 @contextlib.contextmanager
 def lock(self,alias,exclusive):
  self.native()
  if alias not in TARGETS and not re.fullmatch('jobs-update-'+UUID.pattern,alias):raise Refused('ALIAS')
  try:os.mkdir('locks',0o700,dir_fd=self.runtime)
  except FileExistsError:pass
  directory=os.open('locks',T.DIR,dir_fd=self.runtime)
  try:fd=os.open(alias+'.lock',os.O_RDWR|os.O_CREAT|os.O_NOFOLLOW|os.O_CLOEXEC,0o600,dir_fd=directory)
  finally:os.close(directory)
  try:
   st=os.fstat(fd)
   if not stat.S_ISREG(st.st_mode) or st.st_nlink!=1 or st.st_uid!=os.getuid():raise Refused('NATIVE_LOCK_FILE')
   try:fcntl.flock(fd,(fcntl.LOCK_EX if exclusive else fcntl.LOCK_SH)|fcntl.LOCK_NB)
   except BlockingIOError:raise Refused('ENVIRONMENT_BUSY') from None
   yield
  finally:os.close(fd)
 def source(self,alias):
  fd=os.open(alias,T.DIR,dir_fd=self.containers)
  try:
   root=os.open('rootfs',T.DIR,dir_fd=fd);os.close(root)
   try:
    if not isinstance(T.read_json(fd,'manifest.json',1024*1024),dict):raise Refused('IMAGE_METADATA_INVALID')
   except FileNotFoundError:pass
   return fd
  except BaseException:os.close(fd);raise
 def os_release(self,fd):
  raw=T.guest_read(fd,'/etc/os-release',128*1024).decode(errors='replace');fields={}
  for line in raw.splitlines():
   if '=' in line:
    key,value=line.split('=',1)
    if key in ('ID','VERSION_ID','PRETTY_NAME'):fields[key]=value.strip().strip('"')[:120]
  return fields
 def validate(self,fd,target,desktop=False):
  if self.os_release(fd).get('ID') not in OSIDS[target]:raise Refused('ENVIRONMENT_ID_MISMATCH')
  if not T.guest_executable(fd,'/bin/sh'):raise Refused('SYSTEM_SHELL_MISSING')
  binary={'apt':'apt-get','apk':'apk','dnf':'dnf','zypper':'zypper','xbps':'xbps-install','pacman':'pacman'}[P.KINDS[target]]
  if not any(T.guest_executable(fd,p+'/'+binary) for p in ('/usr/bin','/bin','/usr/sbin','/sbin')):raise Refused('PACKAGE_MANAGER_MISSING')
  if desktop:
   for name in DESKTOP:
    if not any(T.guest_executable(fd,p+'/'+name) for p in ('/usr/bin','/bin')):raise Refused('DESKTOP_COMPONENT_MISSING')
 def desktop(self,fd):return T.guest_executable(fd,'/usr/bin/xfce4-session') or T.guest_executable(fd,'/usr/bin/xfdesktop')
 def database(self,fd,target):
  # Installed state changes invalidate a preview. Never expose database bytes
  # or credentials in an event/receipt; candidates are resolved again in the copy.
  digest=hashlib.sha256();found=False
  for path in P.DATABASE[P.KINDS[target]]:
   try:
    if path.endswith(('/local','/updates')):
     root=os.open('rootfs',T.DIR,dir_fd=fd)
     try:
      parent,name=T.m.at(root,path);local=os.open(name,T.DIR,dir_fd=parent);os.close(parent)
      try:
       for row,st in T.m.walk(local):
        digest.update(row.encode());digest.update(repr(T.m.identity(st)).encode())
       found=True
      finally:os.close(local)
     finally:os.close(root)
    else:digest.update(T.guest_read(fd,path,64*1024*1024));found=True
   except FileNotFoundError:continue
  if not found:raise Refused('PACKAGE_DATABASE_MISSING')
  return digest.hexdigest()
 def child(self,args,script=None,phase='packages',accepted=(0,),timeout=3600):
  self.tick(phase);env=dict(os.environ,PREFIX=str(self.prefix),TERMUX__PREFIX=str(self.prefix),PROOT_DISTRO_DISABLE_PROMPT='1')
  for key in ('PYTHONPATH','PYTHONSTARTUP','BASH_ENV','ENV','LD_PRELOAD'):env.pop(key,None)
  status,data=self.runner(args,input_data=script.encode() if script is not None else None,cancel=self.cancelled,tick=lambda:self.tick(phase),env=env,timeout=timeout)
  if status not in accepted:raise Refused(P.diagnostic(data))
  return data
 def guest(self,alias,script,phase,binds=()):
  fd=self.source(alias)
  try:
   try:manifest=T.read_json(fd,'manifest.json',1024*1024)
   except FileNotFoundError:manifest=None
   verb='run' if manifest is not None else 'login'
   if manifest is not None:
    config=manifest.get('image_config',{}).get('config',{}) or {}
    if config.get('Entrypoint') not in (None,[]):raise Refused('IMAGE_ENTRYPOINT_UNSUPPORTED')
  finally:os.close(fd)
  args=[str(self.prefix/'bin/proot-distro'),verb,'--isolated','--work-dir','/']
  for source,target in binds:args.extend(['--bind',str(source)+':'+target])
  args.extend([alias,'--','/usr/bin/env','-i','PATH='+P.PATH,'HOME=/root','LC_ALL=C','/bin/sh','-s'])
  return self.child(args,script,phase)
 def read_plan(self,plan_id):
  plans=os.open('plans',T.DIR,dir_fd=self.control)
  try:return T.read_json(plans,uuid_ok(plan_id)+'.json',16*1024*1024)
  finally:os.close(plans)
 def check(self,target):
  if target not in TARGETS:raise Refused('TARGET')
  self.target=target
  with self.lock(target,False):
   fd=self.source(target)
   try:
    self.validate(fd,target);before=self.database(fd,target);identity=T.ident(os.fstat(fd))
    data=self.guest(target,P.check_script(target),'checking')
    marker=b'\nJOBS_UPDATES_BEGIN\n'
    if marker not in data:raise Refused('PACKAGE_METADATA_INVALID')
    rows=P.parse(target,data.split(marker,1)[1])
    if before!=self.database(fd,target):raise Refused('PACKAGE_STATE_CHANGED')
    plan=dict(id=str(uuid.uuid4()),target=target,created=time.time(),containerId=identity,database=before,candidates=rows,packageToken=P.token(rows),desktop=self.desktop(fd))
    plans=os.open('plans',T.DIR,dir_fd=self.control)
    try:T.write_json(plans,plan['id']+'.json',plan)
    finally:os.close(plans)
    return plan
   finally:os.close(fd)
 def snapshot_dir(self,snapshot_id):
  parent=os.open('snapshots',T.DIR,dir_fd=self.control)
  try:return os.open(uuid_ok(snapshot_id),T.DIR,dir_fd=parent)
  finally:os.close(parent)
 def original_paths(self,fd,alias):
  primary=str(self.base/'containers'/alias);paths=[primary]
  for old,new in (('/data/data/','/data/user/0/'),('/data/user/0/','/data/data/')):
   if primary.startswith(old):
    candidate=primary.replace(old,new,1)
    try:
     if T.ident(os.stat(candidate,follow_symlinks=False))==T.ident(os.fstat(fd)):paths.append(candidate)
    except OSError:pass
  return paths
 def temp_identity(self,name):
  st=os.stat(name,dir_fd=self.containers,follow_symlinks=False)
  if not stat.S_ISDIR(st.st_mode):raise Refused('CONTAINER_CHANGED')
  return T.ident(st)
 def finish_swap(self):
  # Called while both native alias locks are held, including on recovery.
  name=self.state['temporary'];target=self.state['target'];snapshot=self.state['snapshot']
  if self.temp_identity(target)!=self.state['newId'] or self.temp_identity(name)!=self.state['oldId']:raise Refused('RECOVERY_REQUIRES_REVIEW')
  destination=self.snapshot_dir(snapshot)
  try:
   T.publish(self.containers,name,destination,'container')
   T.write_json(destination,'meta.json',dict(id=snapshot,target=target,created=time.time(),reason=self.state['operation'],containerId=self.state['oldId'],original=self.state['originalPaths'],origin=self.state.get('oldOrigin'),transaction=self.state['id']))
  finally:os.close(destination)
  origin=os.open('origins',T.DIR,dir_fd=self.control)
  try:T.write_json(origin,target+'.json',dict(containerId=self.state['newId'],image=self.state.get('newOrigin')))
  finally:os.close(origin)
  self.state.update(status='complete',phase='complete');self.save()
 def commit(self,target,name):
  self.tick('validating')
  new=self.source(name)
  try:
   T.materialize_managed_links(new,self.original_paths(new,name),self.tick)
   self.validate(new,target,self.state.get('desktop',False))
  finally:os.close(new)
  snapshot=str(uuid.uuid4());parent=os.open('snapshots',T.DIR,dir_fd=self.control)
  try:directory=T.mkdir(parent,snapshot);os.close(directory)
  finally:os.close(parent)
  self.state.update(phase='committing',snapshot=snapshot,oldId=self.temp_identity(target),newId=self.temp_identity(name));self.save()
  # This one kernel operation either exchanges both complete trees or does nothing.
  T.exchange(self.containers,target,name);self.state['phase']='swapped';self.save();self.finish_swap()
  return {'target':target,'snapshot':snapshot,'operation':self.state['operation']}
 def finish_initial(self):
  target=self.state['target']
  if self.temp_identity(target)!=self.state['newId']:raise Refused('RECOVERY_REQUIRES_REVIEW')
  origin=os.open('origins',T.DIR,dir_fd=self.control)
  try:T.write_json(origin,target+'.json',dict(containerId=self.state['newId'],image=self.state['newOrigin']))
  finally:os.close(origin)
  self.state.update(status='complete',phase='complete');self.save()
 def official_install(self,target,plan_id,preserve_home=True):
  if target not in TARGETS:raise Refused('TARGET')
  self.target=target
  with self.lock(target,True):
   directory=os.open('official-plans',T.DIR,dir_fd=self.control)
   try:plan=T.read_json(directory,uuid_ok(plan_id)+'.json',128*1024)
   finally:os.close(directory)
   if plan.get('id')!=plan_id or plan.get('target')!=target or plan.get('owner')!='termux-jobs-official-image-plan' or not 0<=time.time()-plan.get('created',0)<=1800:raise Refused('OFFICIAL_PLAN_EXPIRED')
   spec=O.install_source(target,plan['image']);source=None;name='jobs-update-'+self.id
   if spec['kind']=='signed-archive' and not os.access(self.prefix/'bin/gpgv',os.X_OK):raise Refused('OFFICIAL_GPG_REQUIRED')
   try:
    try:source=self.source(target)
    except FileNotFoundError:pass
    identity=T.ident(os.fstat(source)) if source is not None else None
    if identity!=plan.get('containerId'):raise Refused('OFFICIAL_PLAN_CHANGED')
    self.state=dict(format=1,owner='termux-jobs-environment-updates',id=self.id,target=target,operation='official-install',temporary=name,phase='prepare',status='running',created=time.time(),pid=os.getpid(),sourceId=identity,desktop=self.desktop(source) if source is not None else False,originalPaths=self.original_paths(source,target) if source is not None else [],oldOrigin=self.origin(target,identity) if identity else None)
    self.save();before=T.scan(source,self.tick)[2] if source is not None else None
    free=os.fstatvfs(self.containers)
    if free.f_bavail*free.f_frsize<max(plan['image'].get('size',0)*3,512*1024*1024):raise Refused('NOT_ENOUGH_SPACE')
    archive=None
    if spec['kind'] in ('oci','archive','signed-archive'):
     raw=self.child([sys.executable,'-I','-S',str(HERE/'environment-official-fetch.py'),str(self.root/'official-plans'/(plan_id+'.json')),str(self.prefix)],phase='downloading-image')
     archive=json.loads(raw)
     if 'error' in archive:raise Refused(archive['error'])
     expected=self.root/'official-plans'/('download-'+O.cache_key(target,plan['image']))/('rootfs.oci.tar' if spec['kind']=='oci' else 'rootfs.tar.gz')
     if archive.get('path')!=str(expected) or not re.fullmatch('[0-9a-f]{64}',archive.get('sha256','')):raise Refused('OFFICIAL_PLAN_INVALID')
     if spec['kind']=='oci':
      if archive.get('manifestDigest')!=plan['image']['digest'] or type(archive.get('size')) is not int or not 0<archive['size']<=9*1024**3:raise Refused('OFFICIAL_PLAN_INVALID')
     elif archive.get('size')!=spec['size'] or spec['kind']=='archive' and archive['sha256']!=spec['sha256']:raise Refused('OFFICIAL_PLAN_INVALID')
     helper=module('environment-official-fetch')
     if not helper.verified_file(expected,archive['size'],archive['sha256']):raise Refused('IMAGE_CHECKSUM')
     cache=T.m.open_dir(expected.parent)
     try:self.state['downloadCacheId']=T.ident(os.fstat(cache));self.save()
     finally:os.close(cache)
     image=str(expected)
    # Reserve and journal this exact empty directory before native extraction.
    # Native CLI owns its lock while installing. Its cleanup may remove it;
    # unknown/replaced identities are never deleted by our cleanup/recovery.
    with self.lock(name,True):
     os.mkdir(name,0o700,dir_fd=self.containers);self.state['temporaryId']=self.temp_identity(name);self.save()
    self.child([str(self.prefix/'bin/proot-distro'),'install',image,'--name',name,'--architecture','aarch64'],phase='installing-image')
    if self.temp_identity(name)!=self.state['temporaryId']:raise Refused('CONTAINER_CHANGED')
    candidate=self.source(name)
    try:self.validate(candidate,target)
    finally:os.close(candidate)
    if target in ('archlinux','manjaro'):
     self.guest(name,P.ENV+'\nset -eu\npacman-key --init\npacman-key --populate '+('archlinuxarm manjaro-arm manjaro' if target=='manjaro' else 'archlinuxarm')+'\n','preparing-keyrings')
    if self.state['desktop']:
     self.guest(name,'set -eu\n'+P.ENV+'\nexec /bin/sh /tmp/jobs-official-desktop.sh install '+target+' 3>&1\n','installing-desktop',[(HERE/'x11-native-desktop.sh','/tmp/jobs-official-desktop.sh')])
    # Validate the pristine replacement before carrying over user-controlled homes.
    probe='set -eu\n'+P.ENV+'\n/bin/sh -c :\n'+{'apt':'apt-get','apk':'apk','dnf':'dnf','zypper':'zypper','xbps':'xbps-install','pacman':'pacman'}[P.KINDS[target]]+' --version >/dev/null\n'
    if self.state['desktop']:probe+="python3 -I -S -c 'import json,ctypes,encodings'\n"
    self.guest(name,probe,'validating')
    if preserve_home and source is not None:self.overlay_home(source,name)
    self.state['newOrigin']=dict(plan['image'],channel='official',release='official-'+plan['image']['version'],sha256=archive['sha256'] if archive else plan['image']['digest'][7:]);self.save()
    with self.lock(name,True):
     candidate=self.source(name)
     try:
      T.materialize_managed_links(candidate,self.original_paths(candidate,name),self.tick)
      self.validate(candidate,target,self.state['desktop'])
     finally:os.close(candidate)
     if source is not None:
      if self.temp_identity(target)!=identity or T.scan(source,self.tick)[2]!=before:raise Refused('SOURCE_CHANGED')
      result=self.commit(target,name);self.release_official_cache(target,plan['image']);return result
     try:os.stat(target,dir_fd=self.containers,follow_symlinks=False)
     except FileNotFoundError:pass
     else:raise Refused('SOURCE_CHANGED')
     self.state.update(phase='committing',newId=self.temp_identity(name));self.save()
     T.publish(self.containers,name,self.containers,target);self.state['phase']='swapped';self.save();self.finish_initial()
     self.release_official_cache(target,plan['image'])
     return {'target':target,'operation':'official-install','installed':True}
   except BaseException as error:
    if self.state:
     try:self.cleanup_temp()
     except (OSError,Refused,T.Refused):pass
     if self.state.get('status')!='complete':
      self.state.update(status='interrupted' if self.state.get('phase') in ('committing','swapped') else 'cancelled' if isinstance(error,C.Cancelled) else 'error',error=str(error) if isinstance(error,(Refused,C.Cancelled,O.Refused)) else 'UPDATE_FAILED');self.save()
    raise
   finally:
    if source is not None:os.close(source)
 def release_official_cache(self,target,row):
  try:
   parent=os.open('official-plans',T.DIR,dir_fd=self.control)
   try:
    name='download-'+O.cache_key(target,row);directory=os.open(name,T.DIR,dir_fd=parent)
    try:
     if T.ident(os.fstat(directory))!=self.state.get('downloadCacheId'):return
    finally:os.close(directory)
    trash=os.open('trash',T.DIR,dir_fd=self.control)
    try:
     retired='.jobs-delete-'+uuid.uuid4().hex;T.publish(parent,name,trash,retired);T.m.remove_tree(trash,retired)
    finally:os.close(trash)
   finally:os.close(parent)
  except (OSError,T.Refused,T.m.Refused):pass
 def copy_to_temp(self,source,original,name):
  rows,total,token=T.scan(source,self.tick);self.state['bytes']=total;self.save()
  free=os.fstatvfs(self.containers)
  if free.f_bavail*free.f_frsize<total+256*1024*1024:raise Refused('NOT_ENOUGH_SPACE')
  os.mkdir(name,0o700,dir_fd=self.containers);self.state['temporaryId']=self.temp_identity(name);self.save();dst=os.open(name,T.DIR,dir_fd=self.containers)
  try:T.clone(source,dst,rows,total,original,self.tick)
  finally:os.close(dst)
  if T.scan(source,self.tick)[2]!=token:raise Refused('SOURCE_CHANGED')
 def cleanup_temp(self,locked=False):
  name=self.state.get('temporary')
  if not name:return
  try:
   with contextlib.nullcontext() if locked else self.lock(name,True):
    if self.state.get('phase') in ('committing','swapped') or self.state.get('status')=='complete':return
    st=os.stat(name,dir_fd=self.containers,follow_symlinks=False)
    if not stat.S_ISDIR(st.st_mode) or T.ident(st)!=self.state.get('temporaryId'):raise Refused('CONTAINER_CHANGED')
    trash=os.open('trash',T.DIR,dir_fd=self.control)
    try:
     new='.jobs-delete-'+uuid.uuid4().hex;T.publish(self.containers,name,trash,new);T.m.remove_tree(trash,new)
    finally:os.close(trash)
  except FileNotFoundError:pass
 def transaction(self,target,operation,plan_id=None,snapshot_id=None,preserve_home=True):
  if target not in TARGETS or operation not in ('upgrade','rollback','baseline'):raise Refused('TARGET')
  self.target=target
  with self.lock(target,True):
   source=self.source(target);name='jobs-update-'+self.id
   try:
    self.validate(source,target);self.state=dict(format=1,owner='termux-jobs-environment-updates',id=self.id,target=target,operation=operation,temporary=name,phase='prepare',status='running',created=time.time(),pid=os.getpid(),sourceId=T.ident(os.fstat(source)),desktop=self.desktop(source),originalPaths=self.original_paths(source,target),oldOrigin=self.origin(target,T.ident(os.fstat(source))),imageRequest=getattr(self,'image_request',{}));self.state['newOrigin']=self.state['oldOrigin'];self.save()
    source_token=T.scan(source,self.tick)[2]
    if operation=='upgrade':
     plan=self.read_plan(plan_id)
     if plan['target']!=target or time.time()-plan['created']>1800 or plan['containerId']!=self.state['sourceId'] or plan['database']!=self.database(source,target):raise Refused('PLAN_EXPIRED')
     if not plan['candidates']:raise Refused('NO_UPDATES')
     with self.lock(name,True):self.copy_to_temp(source,self.state['originalPaths'],name)
     data=self.guest(name,P.script(target,'refresh')+"printf '\\nJOBS_UPDATES_BEGIN\\n'\n"+P.QUERY[P.KINDS[target]]+'\n','rechecking')
     if b'\nJOBS_UPDATES_BEGIN\n' not in data:raise Refused('PACKAGE_METADATA_INVALID')
     actual=P.parse(target,data.split(b'\nJOBS_UPDATES_BEGIN\n',1)[1])
     if P.token(actual)!=plan['packageToken']:raise Refused('PLAN_CHANGED')
     self.guest(name,P.script(target,'upgrade'),'upgrading')
    elif operation=='rollback':
     snapshot=self.snapshot_dir(snapshot_id)
     try:
      meta=T.read_json(snapshot,'meta.json')
      if meta.get('target')!=target:raise Refused('SNAPSHOT_TARGET')
      self.state['newOrigin']=meta.get('origin')
      saved=os.open('container',T.DIR,dir_fd=snapshot)
      try:
       if T.ident(os.fstat(saved))!=meta['containerId']:raise Refused('SNAPSHOT_CHANGED')
       self.state['desktop']=self.desktop(saved);self.save()
       with self.lock(name,True):self.copy_to_temp(saved,meta['original'],name)
      finally:os.close(saved)
     finally:os.close(snapshot)
     if preserve_home:self.overlay_home(source,name)
    else:self.baseline(source,target,name,preserve_home)
    binary={'apt':'apt-get','apk':'apk','dnf':'dnf','zypper':'zypper','xbps':'xbps-install','pacman':'pacman'}[P.KINDS[target]]
    probe=P.ENV+'\nset -eu\n/bin/sh -c :\n'+binary+' --version >/dev/null\n'
    if self.state['desktop']:probe+="python3 -I -S -c 'import json,ctypes,encodings'\n"
    self.guest(name,probe,'validating')
    with self.lock(name,True):
     if self.temp_identity(target)!=self.state['sourceId']:raise Refused('SOURCE_CHANGED')
     if T.scan(source,self.tick)[2]!=source_token:raise Refused('SOURCE_CHANGED')
     return self.commit(target,name)
   except BaseException as error:
    # Never discard either side of an interrupted exchange.
    try:self.cleanup_temp()
    except (OSError,Refused,T.Refused):pass
    if self.state.get('status')!='complete':
     self.state.update(status='interrupted' if self.state.get('phase') in ('committing','swapped') else 'cancelled' if isinstance(error,C.Cancelled) else 'error',error=str(error) if isinstance(error,(Refused,C.Cancelled,P.Refused,T.Refused)) else 'UPDATE_FAILED');self.save()
    raise
   finally:os.close(source)
 def overlay_home(self,source,name):
  self.tick('preserving-home');rows,total,_=T.scan(source,self.tick);chosen={p:s for p,s in rows.items() if p in ('rootfs/root','rootfs/home') or p.startswith(('rootfs/root/','rootfs/home/'))}
  dst=self.source(name)
  try:
   root=os.open('rootfs',T.DIR,dir_fd=dst)
   try:
    for part in ('root','home'):
     if 'rootfs/'+part not in chosen:continue
     try:
      st=os.stat(part,dir_fd=root,follow_symlinks=False)
      if stat.S_ISDIR(st.st_mode):
       tmp='.jobs-delete-'+uuid.uuid4().hex;T.publish(root,part,root,tmp);T.m.remove_tree(root,tmp)
      else:os.unlink(part,dir_fd=root)
     except FileNotFoundError:pass
   finally:os.close(root)
   T.clone(source,dst,chosen,total,self.original_paths(source,self.target),self.tick)
  finally:os.close(dst)
 def baseline(self,source,target,name,preserve_home):
  item=I.pinned()[target]
  requested=self.state.get('imageRequest',{})
  if requested.get('channel')=='latest':
   remote=I.latest()[target]
   if remote['sha256']!=requested.get('sha256'):raise Refused('IMAGE_PLAN_CHANGED')
   if self.state['desktop'] and remote['sha256']!=item['sha256']:raise Refused('MATCHED_DESKTOP_BASE_REQUIRED')
   item=remote
  cache=self.files/'.environment-control/images'/item['release']/target
  if item['sha256']==I.pinned()[target]['sha256']:
   self.child([str(self.prefix/'bin/python3'),'-I','-S',str(HERE/'environment-image.py'),target],phase='downloading-image')
   image=cache/item['filename']
  else:
   # A future image is fetched only after explicit review of its verified
   # immutable release descriptor; no mutable upstream image tag is installed.
   self.child([str(self.prefix/'bin/python3'),'-I','-S',str(HERE/'environment-update-fetch.py'),target,item['sha256'],str(self.prefix)],phase='downloading-image')
   image=cache/item['filename']
  fd=os.open(image,T.FILE);digest=hashlib.sha256()
  try:
   st=os.fstat(fd)
   if not stat.S_ISREG(st.st_mode) or st.st_nlink!=1 or st.st_size!=item['size']:raise Refused('IMAGE_CHECKSUM')
   while data:=os.read(fd,1024*1024):digest.update(data);self.tick('verifying-image')
   if digest.hexdigest()!=item['sha256']:raise Refused('IMAGE_CHECKSUM')
  finally:os.close(fd)
  with self.lock(name,True):
   try:os.stat(name,dir_fd=self.containers,follow_symlinks=False)
   except FileNotFoundError:pass
   else:raise Refused('TEMPORARY_ALIAS_EXISTS')
  self.child([str(self.prefix/'bin/proot-distro'),'install',str(image),'--name',name],phase='installing-image')
  self.state['temporaryId']=self.temp_identity(name);self.state['newOrigin']=item;self.save()
  if target in ('archlinux','manjaro'):
   setup=P.ENV+'\n'
   if target=='manjaro':setup+="awk '/^\\[/{skip=($0==\"[community]\")} !skip{print}' /etc/pacman.conf > /etc/pacman.conf.jobs-update\nmv /etc/pacman.conf.jobs-update /etc/pacman.conf\n"
   setup+='pacman-key --init\npacman-key --populate '+('archlinuxarm manjaro-arm manjaro' if target=='manjaro' else 'archlinuxarm')+'\n'
   self.guest(name,setup,'preparing-keyrings')
  if self.state['desktop']:
   control=self.files/('workspace-x11-'+target)
   fd=T.m.open_dir(str(self.files))
   try:created=T.mkdir(fd,control.name,private=False);os.close(created)
   finally:os.close(fd)
   self.child([str(self.prefix/'bin/python3'),'-I','-S',str(HERE/'x11-base.py'),target,str(control)],phase='downloading-desktop')
   catalog=json.loads((HERE/'x11-base-packages.json').read_text());base=control/('base-packages-20261028-'+target)
   helper=module('x11-base')
   if not helper.ready(base,target,catalog['targets'][target]['manifestSha256']):raise Refused('BASE_PACKAGE_INCOMPLETE')
   script='set -eu\n'+P.ENV+'\nexec /bin/sh /tmp/jobs-update-install.sh /tmp/jobs-update-base/packages '+target+'\n'
   self.guest(name,script,'installing-desktop',[(base,'/tmp/jobs-update-base'),(HERE/'x11-base-install.sh','/tmp/jobs-update-install.sh')])
  if preserve_home:self.overlay_home(source,name)


 @contextlib.contextmanager
 def operation_lock(self):
  fd=os.open('operation.lock',os.O_RDWR|os.O_CREAT|os.O_NOFOLLOW,0o600,dir_fd=self.control)
  try:
   st=os.fstat(fd)
   if not stat.S_ISREG(st.st_mode) or st.st_nlink!=1:raise Refused('CONTROL_FILE')
   try:fcntl.flock(fd,fcntl.LOCK_EX|fcntl.LOCK_NB)
   except BlockingIOError:raise Refused('ENVIRONMENT_UPDATE_BUSY') from None
   yield
  finally:os.close(fd)
 def origin(self,target,identity):
  try:
   directory=os.open('origins',T.DIR,dir_fd=self.control)
   try:row=T.read_json(directory,target+'.json')
   finally:os.close(directory)
   if row.get('containerId')==identity:return row.get('image')
  except FileNotFoundError:pass
  return None
 def official_images(self,target=None):
  # One bounded, owned child per upstream. Partial failures remain per-target;
  # cancellation propagates and never becomes a successful partial scan.
  result={'checkedAt':int(time.time()),'targets':{}}
  if target is not None and target not in TARGETS:raise Refused('TARGET')
  for target in (TARGETS if target is None else (target,)):
   self.target=target;self.tick('checking-official-images')
   try:
    raw=self.child([sys.executable,'-I','-S',str(HERE/'environment-official-images.py'),'--target',target],phase='checking-official-images',timeout=65)
    row=json.loads(raw)
    if not isinstance(row,dict) or 'error' not in row and row.get('target')!=target:raise Refused('OFFICIAL_METADATA_INVALID')
    if 'error' not in row:
     try:
      spec=O.install_source(target,row);self.native();identity=None
      try:
       fd=self.source(target)
       try:identity=T.ident(os.fstat(fd))
       finally:os.close(fd)
      except FileNotFoundError:pass
      pid=str(uuid.uuid4());plans=os.open('official-plans',T.DIR,dir_fd=self.control)
      try:T.write_json(plans,pid+'.json',dict(owner='termux-jobs-official-image-plan',id=pid,target=target,created=time.time(),containerId=identity,image=row))
      finally:os.close(plans)
      row['plan']=pid
     except (O.Refused,Refused,FileNotFoundError) as error:row['installError']=str(error)
    result['targets'][target]=row
   except (C.Refused,Refused,ValueError,OSError):result['targets'][target]={'error':'OFFICIAL_CHECK_FAILED'}
  return result
 def inventory(self):
  result={'targets':[],'snapshots':[],'jobs':[],'native':B.locked_remove_available(self.prefix)}
  try:self.native()
  except (Refused,FileNotFoundError):pass
  if self.containers is not None:
   for target in TARGETS:
    row={'target':target,'installed':False}
    try:
     source=self.source(target)
     try:
      identity=T.ident(os.fstat(source));row.update(installed=True,containerId=identity,desktop=self.desktop(source),system=self.os_release(source),origin=self.origin(target,identity))
     finally:os.close(source)
    except FileNotFoundError:pass
    except (OSError,Refused,T.Refused):row['error']='ENVIRONMENT_UNAVAILABLE'
    result['targets'].append(row)
  else:result['targets']=[{'target':target,'installed':False} for target in TARGETS]
  for folder,key in [('snapshots','snapshots'),('jobs','jobs')]:
   parent=os.open(folder,T.DIR,dir_fd=self.control)
   try:
    for name in sorted(os.listdir(parent)):
     if not UUID.fullmatch(name):continue
     child=os.open(name,T.DIR,dir_fd=parent)
     try:
      try:row=T.read_json(child,'meta.json' if folder=='snapshots' else 'state.json')
      except FileNotFoundError:continue
      if row.get('target') in TARGETS and row.get('id')==name:result[key].append(row)
     finally:os.close(child)
   finally:os.close(parent)
  result['jobs'].sort(key=lambda r:r.get('created',0),reverse=True);result['snapshots'].sort(key=lambda r:r.get('created',0),reverse=True)
  plans=os.open('plans',T.DIR,dir_fd=self.control)
  try:
   newest={}
   for name in os.listdir(plans):
    if not re.fullmatch(UUID.pattern+r'\.json',name):continue
    row=T.read_json(plans,name,16*1024*1024);target=row.get('target')
    if target not in TARGETS:continue
    if row.get('created',0)>newest.get(target,{}).get('created',0):newest[target]=row
   for row in result['targets']:
    plan=newest.get(row['target'])
    if plan and plan.get('containerId')==row.get('containerId'):row['plan']={k:plan[k] for k in ('id','created','candidates')}
  finally:os.close(plans)
  return result
 def recover(self,job_id):
  old=os.open('jobs',T.DIR,dir_fd=self.control)
  try:job=os.open(uuid_ok(job_id),T.DIR,dir_fd=old)
  finally:os.close(old)
  original_job=self.job
  try:
   state=T.read_json(job,'state.json');target=state.get('target')
   if state.get('owner')!='termux-jobs-environment-updates' or state.get('id')!=job_id or state.get('status')=='complete' or target not in TARGETS or state.get('temporary')!='jobs-update-'+job_id:raise Refused('RECOVERY_REQUIRES_REVIEW')
   with self.lock(target,True),self.lock(state['temporary'],True):
    self.job=job;self.state=state;self.target=target
    try:active=self.temp_identity(target)
    except FileNotFoundError:active=None
    if state.get('operation')=='official-install' and state.get('sourceId') is None and 'newId' in state and active==state['newId']:
     self.finish_initial();return {'recovered':True,'target':target}
    if state.get('operation')=='official-install' and state.get('sourceId') is None and active is None:
     self.state['phase']='aborted-before-commit';self.cleanup_temp(locked=True);self.state.update(status='cancelled',phase='cancelled');self.save();return {'recovered':True,'target':target}
    if state.get('phase') in ('committing','swapped') or state.get('status')=='interrupted' and 'newId' in state:
     snapshot=self.snapshot_dir(state['snapshot'])
     try:
      try:
       old_st=os.stat('container',dir_fd=snapshot,follow_symlinks=False)
       present=T.ident(old_st)==state['oldId']
      except FileNotFoundError:present=False
      if active==state['newId'] and present:
       T.write_json(snapshot,'meta.json',dict(id=state['snapshot'],target=target,created=state['created'],reason=state['operation'],containerId=state['oldId'],original=state['originalPaths'],origin=state.get('oldOrigin'),transaction=job_id))
       origin=os.open('origins',T.DIR,dir_fd=self.control)
       try:T.write_json(origin,target+'.json',dict(containerId=state['newId'],image=state.get('newOrigin')))
       finally:os.close(origin)
       self.state.update(status='complete',phase='complete');self.save();return {'recovered':True,'target':target}
     finally:os.close(snapshot)
     if active==state['newId'] and self.temp_identity(state['temporary'])==state['oldId']:self.finish_swap();return {'recovered':True,'target':target}
     if active!=state['oldId']:raise Refused('RECOVERY_REQUIRES_REVIEW')
    elif active!=state.get('sourceId'):raise Refused('RECOVERY_REQUIRES_REVIEW')
    self.state['phase']='aborted-before-commit'
    # Both alias locks already held. Removing the temporary alias is confined
    # to its recorded identity and cannot target another user container.
    try:
     if self.temp_identity(state['temporary'])!=state.get('temporaryId'):raise Refused('RECOVERY_REQUIRES_REVIEW')
     trash=os.open('trash',T.DIR,dir_fd=self.control)
     try:
      new='.jobs-delete-'+uuid.uuid4().hex;T.publish(self.containers,state['temporary'],trash,new);T.m.remove_tree(trash,new)
     finally:os.close(trash)
    except FileNotFoundError:pass
    self.state.update(status='cancelled',phase='cancelled',error='INTERRUPTED_BEFORE_COMMIT');self.save()
    return {'recovered':True,'target':target}
  finally:self.job=original_job;os.close(job)
 def delete_snapshot(self,snapshot_id):
  snapshot=self.snapshot_dir(snapshot_id)
  try:
   meta=T.read_json(snapshot,'meta.json');target=meta.get('target')
   if target not in TARGETS or meta.get('id')!=snapshot_id:raise Refused('SNAPSHOT_TARGET')
   with self.lock(target,True):
    container=os.open('container',T.DIR,dir_fd=snapshot)
    try:
     if T.ident(os.fstat(container))!=meta.get('containerId'):raise Refused('SNAPSHOT_CHANGED')
    finally:os.close(container)
    parent=os.open('snapshots',T.DIR,dir_fd=self.control);trash=os.open('trash',T.DIR,dir_fd=self.control)
    try:
     name='.jobs-delete-'+uuid.uuid4().hex;T.publish(parent,snapshot_id,trash,name);T.m.remove_tree(trash,name)
    finally:os.close(parent);os.close(trash)
   return {'deleted':snapshot_id}
  finally:os.close(snapshot)


def main():
 global STOP
 signal.signal(signal.SIGTERM,signal_cancel);signal.signal(signal.SIGINT,signal_cancel)
 if len(sys.argv)!=3 or sys.argv[1]!='--request':raise Refused('ARGUMENTS')
 path=Path(sys.argv[2])
 if path.name!='request.json':raise Refused('REQUEST_PATH')
 if len(path.parents)<4 or path.parent.parent.name!='jobs' or path.parent.parent.parent.name!='workspace-environment-updates':raise Refused('REQUEST_PATH')
 uuid_ok(path.parent.name)
 # Only the filesDir anchor may be Android's legitimate private-directory
 # alias. Resolve that anchor once; descend into every control component using
 # directory descriptors and O_NOFOLLOW.
 anchor=T.m.open_dir(path.parents[3].resolve(strict=True))
 try:
  control=os.open('workspace-environment-updates',T.DIR,dir_fd=anchor)
  try:
   jobs=os.open('jobs',T.DIR,dir_fd=control)
   try:parent=os.open(path.parent.name,T.DIR,dir_fd=jobs)
   finally:os.close(jobs)
   try:request=T.read_json(parent,'request.json',128*1024)
   finally:os.close(parent)
  finally:os.close(control)
 finally:os.close(anchor)
 runtime=Runtime(request['files'],request['prefix'],request['job'])
 try:
  if not path.parent.samefile(runtime.root/'jobs'/runtime.id):raise Refused('REQUEST_PATH')
  operation=request['operation'];target=request.get('target');runtime.image_request=request.get('image',{})
  if operation=='inventory':result=runtime.inventory()
  elif operation=='images':result=I.latest()
  elif operation=='official-images':result=runtime.official_images(target)
  else:
   with runtime.operation_lock():
    if operation=='check-all':
     checked={};runtime.native()
     for selected in TARGETS:
      try:
       fd=runtime.source(selected);os.close(fd)
      except FileNotFoundError:continue
      try:checked[selected]=runtime.check(selected)
      except (Refused,T.Refused,P.Refused,C.Refused,OSError) as error:
       checked[selected]={'error':str(error) if re.fullmatch('[A-Z_]{1,80}',str(error)) else 'UPDATE_FAILED'}
     result={'checked':checked}
    elif operation=='check':result=runtime.check(target)
    elif operation=='official-install':result=runtime.official_install(target,request.get('plan'),request.get('preserveHome',True))
    elif operation in ('upgrade','rollback','baseline'):result=runtime.transaction(target,operation,request.get('plan'),request.get('snapshot'),request.get('preserveHome',True))
    elif operation=='recover':result=runtime.recover(request['transaction'])
    elif operation=='delete-snapshot':result=runtime.delete_snapshot(request['snapshot'])
    else:raise Refused('OPERATION')
  if operation not in ('inventory','images','official-images'):
   try:result['inventory']=runtime.inventory()
   except (OSError,ValueError,Refused,T.Refused,T.m.Refused):result['inventoryWarning']='INVENTORY_REFRESH_REQUIRED'
   if operation=='check-all' and 'inventory' in result:
    for row in result['inventory']['targets']:
     if 'error' in result['checked'].get(row['target'],{}):row['checkError']=result['checked'][row['target']]['error']
  emit(event='done',result=result)
 finally:runtime.close()

if __name__=='__main__':
 try:main()
 except C.Cancelled:emit(event='error',code='CANCELLED');sys.exit(130)
 except (Refused,T.Refused,T.m.Refused,P.Refused,I.Refused,O.Refused,C.Refused,ValueError,KeyError) as error:
  code=str(error);emit(event='error',code=code if re.fullmatch('[A-Z_]{1,80}',code) else 'UPDATE_FAILED');sys.exit(1)
 except PermissionError:emit(event='error',code='FILES_ACCESS_DENIED');sys.exit(1)
 except OSError as error:emit(event='error',code='NOT_ENOUGH_SPACE' if error.errno==28 else 'FILESYSTEM_ERROR');sys.exit(1)
