#!/usr/bin/env python3
"""Build a private Garcon4.20.0 overlay, never install into the host or guest."""
from pathlib import Path
import hashlib,json,subprocess,tarfile,urllib.request,shutil,re,zipfile,difflib
ROOT=Path(__file__).resolve().parents[1];base=ROOT/'.tools/garcon-build';sdk=base/'sdk';src=base/'garcon-4.20.0';out=ROOT/'.tools/x11-runtime/assets'
def main():
 pins=json.loads((ROOT/'third-party/garcon/provenance.json').read_text());base.mkdir(parents=True,exist_ok=True);sdk.mkdir(exist_ok=True);out.mkdir(parents=True,exist_ok=True)
 for key,record in pins.items():
  url=record.get('url','https://ports.ubuntu.com/ubuntu-ports/'+record.get('Filename',''));dest=base/url.rsplit('/',1)[-1];expected=record.get('sha256',record.get('SHA256'))
  if not dest.exists():
   data=urllib.request.urlopen(url,timeout=60).read()
   if hashlib.sha256(data).hexdigest()!=expected:raise ValueError('Garcon input mismatch: '+key)
   dest.write_bytes(data)
  if hashlib.sha256(dest.read_bytes()).hexdigest()!=expected:raise ValueError('Garcon cache mismatch: '+key)
  if key=='source':
   with tarfile.open(dest) as archive:archive.extractall(base,filter='data')
  else:subprocess.run(['dpkg-deb','-x',str(dest),str(sdk)],check=True)
 conf=(src/'garcon/garcon-config.h.in').read_text()
 for k,v in [('MAJOR','4'),('MINOR','20'),('MICRO','0')]:conf=conf.replace('@GARCON_VERSION_'+k+'@',v)
 (src/'garcon/garcon-config.h').write_text(conf)
 menu=src/'garcon/garcon-menu.c';s=menu.read_text();needle='  if (event_type == G_FILE_MONITOR_EVENT_CHANGES_DONE_HINT\n'
 assert s.count(needle)==2
 s=s.replace('#include "garcon-private.h"','#include "garcon-private.h"\n#include "garcon-attribute-filter.h"')
 s=s.replace(needle,'  if (jobs_garcon_attribute_unchanged (G_OBJECT (menu), file, event_type))\n    {\n      g_mutex_unlock (&menu->priv->load_lock);\n      return;\n    }\n\n'+needle,1)
 original=menu.read_text();menu.write_text(s)
 patch="".join(difflib.unified_diff(original.splitlines(True),s.splitlines(True),fromfile="a/garcon/garcon-menu.c",tofile="b/garcon/garcon-menu.c"))
 with zipfile.ZipFile(out/'x11-garcon-source.zip','w',compression=zipfile.ZIP_DEFLATED) as archive:
  archive.write(base/'garcon-4.20.0.tar.bz2','garcon-4.20.0.tar.bz2')
  archive.writestr('garcon-menu.patch',patch)
  for file in ['scripts/prepare-garcon.py','x11/src/main/cpp/garcon-attribute-filter.c','x11/src/main/cpp/garcon-attribute-filter.h','third-party/garcon/provenance.json','third-party/garcon/README.md']:
   archive.write(ROOT/file,file)
 sources=re.search(r'libgarcon_sources =(.+?)\n\nlibgarconincludedir', (src/'garcon/Makefile.am').read_text(),re.S)[1]
 files=[str(src/'garcon'/name) for name in re.findall(r'garcon-[\w-]+\.c',sources)]
 inc=sdk/'usr/include';lib=sdk/'usr/lib/aarch64-linux-gnu'
 args=['cc','-ffile-prefix-map='+str(ROOT)+'=/src','-shared','-fPIC','-O2','-std=gnu11','-Wall','-Wextra','-Werror=implicit-function-declaration','-DGARCON_COMPILATION','-DHAVE_UNISTD_H','-DHAVE_STDLIB_H','-DHAVE_STRING_H','-DHAVE_MEMORY_H','-DHAVE_ERRNO_H','-DHAVE_FCNTL_H','-DHAVE_REALPATH','-DGLIB_VERSION_MIN_REQUIRED=GLIB_VERSION_2_68','-DGLIB_VERSION_MAX_ALLOWED=GLIB_VERSION_2_68','-DSYSCONFIGDIR="/etc/xdg"','-DGARCON_VERSION_API="1"']
 args += ['-I'+str(p) for p in [src,src/'garcon',ROOT/'x11/src/main/cpp',inc/'glib-2.0',lib/'glib-2.0/include',inc/'xfce4']]
 args += files+[str(ROOT/'x11/src/main/cpp/garcon-attribute-filter.c'),str(lib/'libxfce4util.so.7'),'/usr/lib/aarch64-linux-gnu/libgio-2.0.so.0','/usr/lib/aarch64-linux-gnu/libgobject-2.0.so.0','/usr/lib/aarch64-linux-gnu/libglib-2.0.so.0','-Wl,-soname,libgarcon-1.so.0','-Wl,-z,relro,-z,now,-z,nodelete','-Wl,--no-undefined','-o',str(out/'libgarcon-1.so.0')]
 subprocess.run(args,check=True)
 image=(out/'libgarcon-1.so.0').read_bytes()
 if image[:4]!=b'\x7fELF' or int.from_bytes(image[18:20],'little')!=183:raise ValueError('Garcon overlay requires ARM64 glibc compiler')
 print('Garcon overlay SHA256',hashlib.sha256((out/'libgarcon-1.so.0').read_bytes()).hexdigest())
if __name__=='__main__':main()
