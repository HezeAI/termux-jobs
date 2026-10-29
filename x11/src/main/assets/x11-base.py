#!/usr/bin/env python3
"""Pinned desktop package archives in private controls; native managers own installation."""
import fcntl
import hashlib,importlib.util,json,os,re,shutil,stat,sys,tarfile
from pathlib import Path
VERSION='xfce-base-20261028'
TARGETS={'termux','ubuntu','debian','alpine','fedora','opensuse','void','archlinux','kali','manjaro','host-audio','host-virgl'}
NAME=re.compile(r'[A-Za-z0-9][A-Za-z0-9._+:%~^=-]{0,220}')
def sha(path):
 with path.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
def files(record):
 entries=record.get('files')
 if not isinstance(entries,dict) or not 1<=len(entries)<=5000:raise ValueError('BASE_PACKAGE_MANIFEST')
 for name,value in entries.items():
  if not NAME.fullmatch(name) or not isinstance(value,dict) or type(value.get('size')) is not int or not 0<value['size']<=1024**3 or not re.fullmatch('[0-9a-f]{64}',value.get('sha256','')):raise ValueError('BASE_PACKAGE_ENTRY')
 return entries
def versions(record):
 rows=[]
 for filename,value in record['files'].items():
  if value.get('kind')=='repository-metadata':continue
  name=value.get('name','');version=value.get('version','')
  if not re.fullmatch('[A-Za-z0-9][A-Za-z0-9+_.-]{0,255}',name) or not re.fullmatch('[A-Za-z0-9][A-Za-z0-9+_.:~^=-]{0,255}',version):raise ValueError('BASE_PACKAGE_VERSION_ENTRY')
  rows.append(name+'\t'+version+'\t'+filename+'\n')
 return ''.join(sorted(set(rows))).encode()
def ready(root,target,manifest_sha=None):
 try:
  if root.is_symlink() or not root.is_dir():return False
  p=root/'base.json'
  if p.is_symlink() or p.stat().st_size>2*1024**2 or p.stat().st_nlink!=1 or p.stat().st_uid!=os.getuid():return False
  if manifest_sha is not None and sha(p)!=manifest_sha:return False
  record=json.loads(p.read_text())
  if record.get('version')!=VERSION or record.get('target')!=target:return False
  v=root/'versions.tsv'
  if v.is_symlink() or v.stat().st_nlink!=1 or v.stat().st_uid!=os.getuid() or v.read_bytes()!=versions(record):return False
  repo=root/'empty-repositories'
  if repo.is_symlink() or not repo.is_dir() or any(repo.iterdir()):return False
  entries=files(record);folder=root/'packages'
  if folder.is_symlink() or not folder.is_dir():return False
  if {p.name for p in folder.iterdir()}!=set(entries):return False
  for name,item in entries.items():
   p=folder/name;info=p.lstat()
   if not stat.S_ISREG(info.st_mode) or info.st_nlink!=1 or info.st_size!=item['size'] or sha(p)!=item['sha256']:return False
  return True
 except (ValueError,OSError,KeyError):return False
def unpack(archive,destination,target,manifest_sha=None):
 with tarfile.open(archive,'r:*') as t:
  info=t.getmember('base.json')
  if not info.isfile() or info.size>2*1024**2:raise ValueError('BASE_PACKAGE_MANIFEST')
  raw=t.extractfile(info).read()
  if manifest_sha is not None and hashlib.sha256(raw).hexdigest()!=manifest_sha:raise ValueError('BASE_MANIFEST_CHECKSUM')
  record=json.loads(raw);entries=files(record)
  if record.get('version')!=VERSION or record.get('target')!=target:raise ValueError('BASE_PACKAGE_TARGET')
  stage=destination.with_name('.'+destination.name+'.stage')
  if stage.exists() or stage.is_symlink():
   if stage.is_symlink() or not stage.is_dir() or stage.stat().st_uid!=os.getuid():raise ValueError('BASE_PACKAGE_STAGE')
   # Fixed private staging path; retry replaces an interrupted extraction instead
   # of accumulating a new full package cache on every cancellation.
   shutil.rmtree(stage)
  stage.mkdir(mode=0o700);found=set();total=0;manifests=0
  try:
   (stage/'packages').mkdir(mode=0o700)
   (stage/'empty-repositories').mkdir(mode=0o700)
   for member in t:
    if member.name=='base.json':
     manifests+=1
     if manifests>1:raise ValueError('BASE_DUPLICATE_MANIFEST')
     continue
    name=member.name.removeprefix('packages/')
    if member.name!='packages/'+name or name not in entries or name in found or not member.isfile():raise ValueError('BASE_PACKAGE_ARCHIVE')
    item=entries[name];total+=member.size
    if total>4*1024**3 or member.size!=item['size']:raise ValueError('BASE_PACKAGE_SIZE')
    path=stage/'packages'/name;h=hashlib.sha256()
    with t.extractfile(member) as source,path.open('xb') as out:
     os.chmod(path,0o600)
     while data:=source.read(1024*1024):h.update(data);out.write(data)
    if h.hexdigest()!=item['sha256']:raise ValueError('BASE_PACKAGE_CHECKSUM')
    found.add(name)
   if found!=set(entries):raise ValueError('BASE_PACKAGE_MISSING')
   (stage/'versions.tsv').write_bytes(versions(record));os.chmod(stage/'versions.tsv',0o600)
   (stage/'base.json').write_bytes(raw);os.chmod(stage/'base.json',0o600)
   if not ready(stage,target,manifest_sha):raise ValueError('BASE_PACKAGE_INCOMPLETE')
   if destination.exists() or destination.is_symlink():
    if destination.is_symlink() or not destination.is_dir():raise ValueError('BASE_PACKAGE_CACHE_LINK')
    # This is only a fixed application-owned package cache, never a guest root.
    shutil.rmtree(destination)
   os.rename(stage,destination)
  finally:
   if stage.exists():shutil.rmtree(stage)
def main():
 if len(sys.argv)!=3 or sys.argv[1] not in TARGETS:raise ValueError('BASE_PACKAGE_ARGUMENTS')
 target=sys.argv[1];control=Path(sys.argv[2]);prefix=Path(os.environ['PREFIX']);expected='workspace-x11'+('' if target=='termux' else '-'+target)
 if target.startswith('host-'):
  expected='workspace-x11-host'
  if control.name!=expected or not control.parent.samefile(prefix.parent):raise ValueError('BASE_PACKAGE_CONTROL')
  try:control.mkdir(mode=0o700)
  except FileExistsError:pass
 if target=='ubuntu' and control.name=='workspace-x11-jobs-x11-ubuntu':expected=control.name
 if control.name!=expected or not control.parent.samefile(prefix.parent) or control.is_symlink() or not control.is_dir() or control.stat().st_uid!=os.getuid():raise ValueError('BASE_PACKAGE_CONTROL')
 fd=os.open(control/'base-packages.lock',os.O_CREAT|os.O_RDWR|os.O_NOFOLLOW,0o600)
 info=os.fstat(fd)
 if not stat.S_ISREG(info.st_mode) or info.st_nlink!=1:os.close(fd);raise ValueError('BASE_PACKAGE_LOCK')
 try:
  try:fcntl.flock(fd,fcntl.LOCK_EX|fcntl.LOCK_NB)
  except BlockingIOError:raise ValueError('BASE_PACKAGE_BUSY') from None
  destination=control/('base-packages-20261028-'+target)
  p=Path(__file__).with_name('x11-base-packages.json')
  if p.is_symlink() or p.stat().st_size>128*1024:raise ValueError('BASE_PACKAGE_CATALOG')
  catalog=json.loads(p.read_text())
  if catalog.get('version')!=VERSION:raise ValueError('BASE_PACKAGE_VERSION')
  item=catalog['targets'][target];name=item['filename'];manifest_sha=item['manifestSha256']
  if not re.fullmatch('[0-9a-f]{64}',manifest_sha):raise ValueError('BASE_MANIFEST_PIN')
  if ready(destination,target,manifest_sha):print('READY:base-packages');return
  if not re.fullmatch('[a-z0-9.-]{1,100}',name) or not re.fullmatch('[0-9a-f]{64}',item['sha256']) or type(item['size']) is not int or not 0<item['size']<=2*1024**3:raise ValueError('BASE_PACKAGE_PIN')
  spec=importlib.util.spec_from_file_location('jobs_image_download',Path(__file__).with_name('environment-image.py'));module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
  archive=control/name
  print('BASE_STAGE:download:'+target,flush=True)
  module.download(archive,'https://github.com/HezeAI/termux-jobs-components/releases/download/'+VERSION+'/'+name,item['sha256'],item['size'])
  print('STAGE:unpack-pinned-desktop',flush=True);unpack(archive,destination,target,manifest_sha);print('READY:base-packages',flush=True)
 finally:os.close(fd)

if __name__=='__main__':
 try:main()
 except (OSError,ValueError,KeyError,tarfile.TarError) as error:
  print('ERROR:'+str(error) if isinstance(error,ValueError) else 'ERROR:BASE_PACKAGE_SETUP_FAILED',flush=True);sys.exit(1)
