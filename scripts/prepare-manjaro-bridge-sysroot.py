#!/usr/bin/env python3
"""Extract hash-pinned build inputs privately; never install packages or hooks."""
from pathlib import Path
import json,urllib.request,hashlib,subprocess,os,time,tarfile
root=Path(__file__).resolve().parents[1];work=root/'.tools/manjaro-bridge';cache=work/'downloads';sysroot=work/'sysroot';cache.mkdir(parents=True,exist_ok=True);sysroot.mkdir(exist_ok=True)
import fcntl
lock=(work/'sysroot.lock').open('a')
fcntl.flock(lock.fileno(),fcntl.LOCK_EX|fcntl.LOCK_NB)
packages=json.loads((root/'third-party/bionic-glibc/manjaro-build-packages.json').read_text())
for name,p in packages.items():
 dest=cache/p['FILENAME'];expected=p['SHA256SUM'];size=int(p['CSIZE'])
 if not dest.is_file() or dest.stat().st_size!=size or hashlib.file_digest(dest.open('rb'),'sha256').hexdigest()!=expected:
  for mirror in ['https://coresite.mm.fcix.net/manjaro/arm-stable','https://mirrors.up.pt/pub/manjaro/arm-stable']:
   url=mirror+'/'+p['repo']+'/aarch64/'+p['FILENAME'];temp=dest.with_suffix(dest.suffix+'.part')
   try:
    print('DOWNLOAD',name,p['VERSION'],size,flush=True)
    with urllib.request.urlopen(url,timeout=40) as response,temp.open('wb') as out:
     while data:=response.read(1024*1024):out.write(data)
    if temp.stat().st_size!=size or hashlib.file_digest(temp.open('rb'),'sha256').hexdigest()!=expected:raise ValueError('checksum '+name)
    temp.replace(dest);p['url']=url;break
   except Exception as e:print('RETRY',name,type(e).__name__,str(e),flush=True);temp.unlink(missing_ok=True)
  else:raise RuntimeError('download failed: '+name)
 # Hash-pinned official archives only. Never execute package hooks or write outside sysroot.
 subprocess.run(['/usr/bin/tar','-xf',str(dest),'--no-same-owner','--no-same-permissions','-C',str(sysroot)],check=True)
 print('READY',name,flush=True)
# Normalize absolute library-directory aliases inside our disposable sysroot.
for p in sysroot.rglob('*'):
 if p.is_symlink() and os.readlink(p).startswith('/'):
  p_target=sysroot/os.readlink(p).lstrip('/');p.unlink();p.symlink_to(os.path.relpath(p_target,p.parent))
(work/'resolved-build-packages.json').write_text(json.dumps(packages,indent=2)+'\n')
print('SYSROOT_READY',flush=True)

# Host-only binutils helpers and explicit target compiler wrappers.
import shutil,shlex
(w:=work/'host-libs').mkdir(exist_ok=True)
(work/'bin').mkdir(exist_ok=True)
for pattern in ('libbfd-*.so','libopcodes-*.so'):
 for src in (sysroot/'usr/lib').glob(pattern):shutil.copy2(src,w/src.name)
for name in ('gcc','g++','ar','strip'):
 flags=(' --sysroot='+shlex.quote(str(sysroot))+' -B'+shlex.quote(str(sysroot/'usr/bin/'))) if name in ('gcc','g++') else ''
 p=work/'bin'/name
 p.write_text('#!/bin/sh\nexport LD_LIBRARY_PATH='+shlex.quote(str(w))+'\nexec '+shlex.quote(str(sysroot/'usr/bin'/name))+flags+' "$@"\n');p.chmod(0o700)
