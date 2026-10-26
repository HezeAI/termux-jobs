#!/usr/bin/env python3
"""Optional pinned Mesa components in private app storage; never replaces system Mesa."""
import fcntl
import hashlib
import io
import json
import os
from pathlib import Path
import platform
import re
import shutil
import stat
import subprocess
import sys
import tarfile
import tempfile
import time
import urllib.request
import urllib.error
import uuid

CANCEL_FILE = None

def check_cancel():
 if CANCEL_FILE is not None and CANCEL_FILE.exists():raise ValueError("GPU_CANCELLED")

SPECS = {'ubuntu': {'directory': 'ubuntu-26.04-mesa26.2.5', 'version': '26.2.5-jobs1', 'packages': [('https://github.com/HezeAI/termux-jobs-components/releases/download/mesa-26.2.5/mesa-26.2.5-jobs1-glibc-multiarch-aarch64.tar.gz', 'a848b94d5950b8573c42d41f89592d3d20f7dd0e6a969e44475da2589751a632', 10348020, 'tar')]},
 'debian': {'directory': 'debian-13-mesa26.2.5', 'version': '26.2.5-jobs1', 'packages': [('https://github.com/HezeAI/termux-jobs-components/releases/download/mesa-26.2.5/mesa-26.2.5-jobs1-glibc-multiarch-aarch64.tar.gz', 'a848b94d5950b8573c42d41f89592d3d20f7dd0e6a969e44475da2589751a632', 10348020, 'tar')]},
 'alpine': {'directory': 'alpine-3.24-mesa26.2.5', 'version': '26.2.5-jobs1', 'packages': [('https://github.com/HezeAI/termux-jobs-components/releases/download/mesa-26.2.5/mesa-26.2.5-jobs1-musl-alpine3.24-aarch64.tar.gz', '85cbbb81c7c30cb8eb3675c97babf3fc6f47d0f9021fe4664a40e3a47709f192', 8221445, 'tar')]},
 'fedora': {'directory': 'fedora-44-mesa26.2.5', 'version': '26.2.5-jobs1', 'packages': [('https://github.com/HezeAI/termux-jobs-components/releases/download/mesa-26.2.5/mesa-26.2.5-jobs1-glibc-lib64-aarch64.tar.gz', '196b9ba40741958adff13e988c76e4d1082fca36dc165c1dcf34e7c9ced6d141', 10347945, 'tar')]},
 'opensuse': {'directory': 'opensuse-16-mesa26.2.5', 'version': '26.2.5-jobs1', 'packages': [('https://github.com/HezeAI/termux-jobs-components/releases/download/mesa-26.2.5/mesa-26.2.5-jobs1-glibc-multiarch-aarch64.tar.gz', 'a848b94d5950b8573c42d41f89592d3d20f7dd0e6a969e44475da2589751a632', 10348020, 'tar'), ('https://github.com/HezeAI/termux-jobs-components/releases/download/graphics-20261025/libsensors5_3.6.2-2_arm64.deb', '7469f1ccce8afdba33d3b11847d0ecdf2221805a0694dee21ee4d622e8f72145', 36444, 'deb')]},
 'void': {'directory': 'void-mesa26.2.5', 'version': '26.2.5-jobs1', 'packages': [('https://github.com/HezeAI/termux-jobs-components/releases/download/mesa-26.2.5/mesa-26.2.5-jobs1-glibc-lib-aarch64.tar.gz', 'be26f1d122d2e626d8411c968ff7074d3d7c614f35f59fe8c97164f690c70dcb', 10347926, 'tar')]},
 'archlinux': {'directory': 'archlinux-mesa26.2.5', 'version': '26.2.5-jobs1', 'packages': [('https://github.com/HezeAI/termux-jobs-components/releases/download/mesa-26.2.5/mesa-26.2.5-jobs1-glibc-lib-aarch64.tar.gz', 'be26f1d122d2e626d8411c968ff7074d3d7c614f35f59fe8c97164f690c70dcb', 10347926, 'tar')]},
 'termux': {'directory': 'termux-mesa26.2.5', 'version': '26.2.5', 'packages': [('https://github.com/HezeAI/termux-jobs-components/releases/download/graphics-20261025/mesa-vulkan-icd-freedreno_26.2.5_aarch64.deb', '79d4328e41b3b40edea91088c7992f5522469c682e44223efb69ef0861903c37', 1599892, 'deb'), ('https://github.com/HezeAI/termux-jobs-components/releases/download/graphics-20261025/vulkan-loader-generic_1.4.366_aarch64.deb', '5b0a17efdd27e064c06dcf5befba4b466eac6025110ecc44be768019b00abdb7', 150484, 'deb')]}}

class HttpsOnly(urllib.request.HTTPRedirectHandler):
 def redirect_request(self, req, fp, code, msg, headers, newurl):
  if not newurl.startswith('https://'): raise ValueError('INSECURE_REDIRECT')
  return super().redirect_request(req,fp,code,msg,headers,newurl)

def data_archive(data,kind):
 if kind=='tar':return io.BytesIO(data)
 if not data.startswith(b'!<arch>\n'):raise ValueError('INVALID_DEB')
 offset=8
 while offset+60<=len(data):
  header=data[offset:offset+60]
  if header[58:60]!=b'`\n':raise ValueError('INVALID_DEB_HEADER')
  size=int(header[48:58]);name=header[:16].decode('ascii').strip().rstrip('/')
  if size<0 or offset+60+size>len(data):raise ValueError('INVALID_DEB_SIZE')
  if name.startswith('data.tar.'):return io.BytesIO(data[offset+60:offset+60+size])
  offset+=60+size+(size%2)
 raise ValueError('MISSING_DEB_DATA')

def unpack(data,kind,destination):
 if kind=='arch-bundle':
  # Fixed reviewed payloads only. Never execute .INSTALL or install these globally.
  expected={'mesa-1:26.3.0-1-aarch64.pkg.tar.xz','vulkan-freedreno-1:26.3.0-1-aarch64.pkg.tar.xz'}
  found=set()
  with tarfile.open(fileobj=io.BytesIO(data)) as bundle:
   for entry in bundle:
    check_cancel()
    if entry.name not in expected:continue
    if entry.name in found or not entry.isfile() or entry.size>64*1024*1024:raise ValueError('ARCH_BUNDLE_INVALID')
    found.add(entry.name);unpack(bundle.extractfile(entry).read(),'tar',destination)
  if found!=expected:raise ValueError('ARCH_BUNDLE_MISSING_PACKAGE')
  return
 with tarfile.open(fileobj=data_archive(data,kind)) as archive:
  entries=[];total=0
  for entry in archive:
   check_cancel();total+=entry.size;entries.append(entry)
   if len(entries)>5000 or total>192*1024*1024:raise ValueError('ARCHIVE_TOO_LARGE')
   if not (entry.isfile() or entry.isdir() or entry.issym() or entry.islnk()):raise ValueError('ARCHIVE_SPECIAL_FILE')
  archive.extractall(destination,members=entries,filter='data')

def hashes(root):
 result={}
 for path in root.rglob('*'):
  if path==root/'ready.json':continue
  relative=str(path.relative_to(root))
  if path.is_symlink():
   if not path.resolve().is_relative_to(root.resolve()):raise ValueError('EXTERNAL_LINK')
   result[relative]='link:'+os.readlink(path)
  elif path.is_dir():continue
  elif path.is_file():result[relative]=hashlib.sha256(path.read_bytes()).hexdigest()
  else:raise ValueError('SPECIAL_FILE')
 return result

def ready(base,target):
 try:
  spec=SPECS[target];root=base/spec['directory']
  if root.is_symlink() or not root.is_dir():return False
  marker=root/'ready.json'
  if marker.is_symlink() or marker.stat().st_size>262144:return False
  state=json.loads(marker.read_text())
  return state.get('version')==spec['version'] and state.get('files')==hashes(root)
 except (OSError,ValueError,KeyError):return False

def download(url,sha,size,update):
 check_cancel()
 opener=urllib.request.build_opener(HttpsOnly());start=time.monotonic();data=bytearray()
 try:response=opener.open(url,timeout=20)
 except urllib.error.HTTPError as error:raise ValueError('GPU_DOWNLOAD_HTTP_'+str(error.code)) from None
 except (urllib.error.URLError,TimeoutError):raise ValueError('GPU_DOWNLOAD_NETWORK') from None
 with response:
  if not response.geturl().startswith('https://'):raise ValueError('INSECURE_RESPONSE')
  while True:
   check_cancel()
   chunk=response.read(65536)
   if not chunk:break
   data.extend(chunk)
   if len(data)>size or time.monotonic()-start>300:raise ValueError('DOWNLOAD_LIMIT')
   update(len(data),size)
 if len(data)!=size or hashlib.sha256(data).hexdigest()!=sha:raise ValueError('CHECKSUM_MISMATCH')
 return bytes(data)

def install(base,target,fetch=download,repair=False):
 spec=SPECS[target];base.mkdir(mode=0o700,parents=True,exist_ok=True)
 if base.is_symlink():raise ValueError('CACHE_IS_A_LINK')
 lock=os.open(base/'install.lock',os.O_RDWR|os.O_CREAT|os.O_NOFOLLOW,0o600)
 try:
  if not stat.S_ISREG(os.fstat(lock).st_mode) or os.fstat(lock).st_nlink!=1:raise ValueError('UNSAFE_LOCK')
  fcntl.flock(lock,fcntl.LOCK_EX|fcntl.LOCK_NB)
  check_cancel()
  if ready(base,target):return
  destination=base/spec['directory']
  previous=None
  if destination.exists() or destination.is_symlink():
   if destination.is_symlink() or not destination.is_dir():raise ValueError('CACHE_IS_A_LINK_OR_FILE')
   if not repair:raise ValueError('CACHE_INCOMPLETE')
   info=destination.stat();previous=(info.st_dev,info.st_ino)

  stage=Path(tempfile.mkdtemp(prefix='.stage-',dir=base));total=sum(p[2] for p in spec['packages']);done=0
  try:
   for url,sha,size,kind in spec['packages']:
    print('GPU_STAGE:download',flush=True)
    data=fetch(url,sha,size,lambda n,t:print('GPU_PROGRESS:%d:%d'%(done+n,total),flush=True))
    # Recheck even with an injected fetcher, before any extraction.
    if len(data)!=size or hashlib.sha256(data).hexdigest()!=sha:raise ValueError('CHECKSUM_MISMATCH')
    print('GPU_STAGE:extract',flush=True);unpack(data,kind,stage);done+=size
   (stage/'turnip-icd.json').write_text(json.dumps({'file_format_version':'1.0.0','ICD':{'library_path':'libvulkan_freedreno.so','api_version':'1.4.0'}}))
   check_cancel()
   files=hashes(stage);assert files
   (stage/'ready.json').write_text(json.dumps({'version':spec['version'],'files':files}))
   check_cancel()
   retired=None
   if previous is not None:
    info=destination.lstat()
    if not stat.S_ISDIR(info.st_mode) or (info.st_dev,info.st_ino)!=previous:raise ValueError('CACHE_CHANGED')
    retired=base/('.retired-'+uuid.uuid4().hex)
    os.rename(destination,retired)
   try:os.rename(stage,destination)
   except BaseException:
    if retired is not None:os.rename(retired,destination)
    raise
   if retired is not None:
    # fd-relative rmtree refuses symlink replacement; do not use an unsafe fallback.
    if shutil.rmtree.avoids_symlink_attacks:
     try:shutil.rmtree(retired)
     except OSError:print('GPU_WARNING:OLD_CACHE_RETAINED',flush=True)
    else:print('GPU_WARNING:OLD_CACHE_RETAINED',flush=True)
  finally:
   if stage.exists():shutil.rmtree(stage)
 finally:os.close(lock)

def requirements(target):
 if platform.machine() not in ('aarch64','arm64'):raise ValueError('ARM64_REQUIRED')
 model=Path('/sys/class/kgsl/kgsl-3d0/gpu_model').read_text().strip()
 if not re.search(r'Adreno[^0-9]*(?:7[0-9]{2}|830|840)(?:v[0-9]+)?$',model,re.I):raise ValueError('GPU_NOT_IN_TEST_TARGETS')
 with open('/dev/kgsl-3d0','rb',buffering=0):pass
 if target=='ubuntu':
  command=[os.environ['PREFIX']+'/bin/proot-distro','login','ubuntu','--','/bin/sh','-c',
   'export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; export XDG_DATA_DIRS=/usr/local/share:/usr/share XDG_CONFIG_DIRS=/etc/xdg; unset LD_LIBRARY_PATH LD_PRELOAD DBUS_SYSTEM_BUS_ADDRESS; . /etc/os-release; [ "$ID" = ubuntu ] && [ "$VERSION_ID" = 26.04 ] || exit 72; dpkg-query -W -f=\'${Status}\\n\' libvulkan1 libegl1 2>/dev/null | grep -c "install ok installed" | grep -qx 2']
  if subprocess.run(command,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL,timeout=25).returncode:raise ValueError('UBUNTU_26_04_LIBVULKAN1_LIBEGL1_REQUIRED')

 if target in ('debian','alpine','fedora','opensuse','void','archlinux'):
  # Check only the selected rootfs. Never install or change its packages here.
  os_check = '[ "$ID" = debian ] && [ "${VERSION_ID%%.*}" = 13 ]' if target=='debian' else '[ "$ID" = alpine ] && [ "${VERSION_ID%.*}" = 3.24 ]'
  if target=='opensuse':os_check='[ "$ID" = opensuse-leap ] && [ "$VERSION_ID" = 16.0 ]'
  if target=='archlinux':os_check='[ "$ID" = archarm ] || [ "$ID" = arch ]'
  if target=='void':os_check='[ "$ID" = void ] && getconf GNU_LIBC_VERSION >/dev/null'
  if target=='fedora':os_check='[ "$ID" = fedora ] && [ "$VERSION_ID" = 44 ]'
  import shlex
  probe = 'import ctypes; ctypes.CDLL("libvulkan.so.1"); ctypes.CDLL("libEGL.so.1")'
  if target=='opensuse':probe+='; ctypes.CDLL("libLLVM.so.19.1")'
  if target=='archlinux':probe+='; ctypes.CDLL("libLLVM.so.22.1"); ctypes.CDLL("libdisplay-info.so.3"); ctypes.CDLL("libSPIRV-Tools.so"); ctypes.CDLL("libsensors.so.5")'
  if target=='void':probe+='; ctypes.CDLL("libLLVM.so.18.1")'
  entry=['run','--work-dir','/root'] if target=='void' else ['login']
  command=[os.environ['PREFIX']+'/bin/proot-distro',*entry,target,'--','/bin/sh','-c',
   'export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; export XDG_DATA_DIRS=/usr/local/share:/usr/share XDG_CONFIG_DIRS=/etc/xdg; unset LD_LIBRARY_PATH LD_PRELOAD DBUS_SYSTEM_BUS_ADDRESS; . /etc/os-release; { '+os_check+'; } || exit 72; python3 -c '+shlex.quote(probe)]
  if subprocess.run(command,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL,timeout=25).returncode:
   if target=='opensuse':raise ValueError('OPENSUSE_16_VULKAN_EGL_LLVM19_REQUIRED')
   if target=='archlinux':raise ValueError('ARCH_ARM_VULKAN_EGL_LLVM22_REQUIRED')
   if target=='void':raise ValueError('VOID_GLIBC_VULKAN_EGL_REQUIRED')
   if target=='fedora':raise ValueError('FEDORA_44_VULKAN_EGL_REQUIRED')
   raise ValueError('DEBIAN_13_VULKAN_EGL_REQUIRED' if target=='debian' else 'ALPINE_3_24_VULKAN_EGL_REQUIRED')

def main():
 global CANCEL_FILE
 if len(sys.argv) not in (4,5):raise ValueError('ARGUMENTS')
 operation,target,path=sys.argv[1:4]
 if target not in SPECS or operation not in ('install','repair','check'):raise ValueError('ARGUMENTS')
 base=Path(path);expected=Path(os.environ['PREFIX']).parent/'workspace-x11-gpu'
 if base.name!=expected.name or not base.parent.samefile(expected.parent):raise ValueError('CACHE_PATH')
 if len(sys.argv)==5:
  CANCEL_FILE=Path(sys.argv[4])
  if CANCEL_FILE.parent!=base.parent/'.gpu-control' or not re.fullmatch(r'cancel-[0-9a-f-]{36}',CANCEL_FILE.name):raise ValueError('CANCEL_PATH')
 check_cancel()
 requirements(target)
 check_cancel()
 if operation in ('install','repair'):install(base,target,repair=operation=='repair')
 if not ready(base,target):raise ValueError('GPU_COMPONENTS_MISSING')
 check_cancel()
 print('READY:gpu',flush=True)

if __name__=='__main__':
 try:main()
 except Exception as error:
  code=str(error) if isinstance(error,ValueError) else 'GPU_SETUP_FAILED'
  print('ERROR:'+code,flush=True);sys.exit(1)
