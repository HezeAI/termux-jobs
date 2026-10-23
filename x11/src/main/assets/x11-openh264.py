#!/usr/bin/env python3
"""Private Parole decoder; never replaces openSUSE's RPM-managed codec stub."""
import hashlib,importlib.util,json,os
from pathlib import Path
DIGEST='79d49f67882c0744973158d7cccc4f0bcf29bfc597b294d77d5f3bbdaba221bf'
LIB='.local/lib/termux-jobs/openh264/libopenh264.so.8'
WRAPPER='.local/bin/termux-jobs-parole'
LAUNCHER='.local/share/applications/org.xfce.Parole.desktop'
STATE='.local/state/termux-jobs/parole-codec.json'
def maintenance():
 s=importlib.util.spec_from_file_location('codec_maintenance',Path(__file__).with_name('x11-maintenance.py'));m=importlib.util.module_from_spec(s);s.loader.exec_module(m);return m

def ready(home):
 try:
  m=maintenance();raw=m.read(home/STATE)
  if raw is None:return False
  entries=json.loads(raw)
  if not isinstance(entries,dict) or set(entries)!={LIB,WRAPPER,LAUNCHER}:return False
  for name,digest in entries.items():
   data=m.read(home/name,2*1024*1024 if name==LIB else 65536)
   if data is None or hashlib.sha256(data).hexdigest()!=digest:return False
  return entries[LIB]==DIGEST and os.access(home/WRAPPER,os.X_OK)
 except (OSError,ValueError):return False

def install(home,system,source):
 m=maintenance();library=m.read(source,2*1024*1024)
 if library is None or hashlib.sha256(library).hexdigest()!=DIGEST:raise ValueError('OpenH264 asset integrity failure')
 original=m.read(system/'usr/share/applications/org.xfce.Parole.desktop')
 if not original:raise ValueError('Parole launcher missing')
 lines=original.decode().splitlines();count=0
 for i,line in enumerate(lines):
  if line.startswith('Exec='):
   import re
   match=re.fullmatch(r'Exec=(?:parole|/usr/bin/parole)(\s.*)?',line)
   if not match:raise ValueError('unsupported Parole launcher command')
   lines[i]='Exec=/root/'+WRAPPER+(match.group(1) or '');count+=1
 if not count:raise ValueError('Parole launcher has no command')
 wrapper=('''#!/bin/sh
# Termux Jobs private Parole OpenH264 v1
export LD_LIBRARY_PATH="$HOME/.local/lib/termux-jobs/openh264${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
exec /usr/bin/parole "$@"
''').encode()
 files={LIB:library,WRAPPER:wrapper,LAUNCHER:('\n'.join(lines)+'\n').encode()}
 raw=m.read(home/STATE);recorded=json.loads(raw) if raw is not None else None
 if recorded is not None and (not isinstance(recorded,dict) or set(recorded)!=set(files)):raise ValueError('invalid Parole codec state')
 # Validate every destination before any write, including interrupted setup.
 for name in files:
  data=m.read(home/name,2*1024*1024 if name==LIB else 65536)
  if data is not None and (recorded is None or hashlib.sha256(data).hexdigest()!=recorded[name]):raise ValueError('Parole codec files have local changes; preserved')
 m.write(home/STATE,json.dumps({name:hashlib.sha256(data).hexdigest() for name,data in files.items()},sort_keys=True).encode())
 for name,data in files.items():m.write(home/name,data,0o700 if name==WRAPPER else 0o600)
 if not ready(home):raise ValueError('Parole decoder setup incomplete')
 print('JOBS_PAROLE_CODEC:openh264-2.6.0',flush=True)
