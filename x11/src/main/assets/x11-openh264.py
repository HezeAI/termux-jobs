#!/usr/bin/env python3
"""Private Parole decoder; Fedora/openSUSE only; never replaces an RPM-managed codec stub."""
import hashlib,importlib.util,json,os,subprocess
from pathlib import Path
DIGEST='79d49f67882c0744973158d7cccc4f0bcf29bfc597b294d77d5f3bbdaba221bf'
LIB='.local/lib/termux-jobs/openh264/libopenh264.so.8'
WRAPPER='.local/bin/termux-jobs-parole'
LAUNCHER='.local/share/applications/org.xfce.Parole.desktop'
STATE='.local/state/termux-jobs/parole-codec.json'
PLUGIN='.local/lib/termux-jobs/openh264/gstreamer-1.0/libgstopenh264.so'
PLUGIN_DIGEST='1f4c3a85e0b23d66f4f657064767900e857855e65659627ad078e69a229f1db4'
PROOF='.local/state/termux-jobs/parole-fedora-verified.json'
REGISTRY='.local/state/termux-jobs/parole-gst-registry.bin'
def maintenance():
 s=importlib.util.spec_from_file_location('codec_maintenance',Path(__file__).with_name('x11-maintenance.py'));m=importlib.util.module_from_spec(s);s.loader.exec_module(m);return m

def proof(library=None,plugin=None):
 return (json.dumps({'schema':1,'library':library or DIGEST,'plugin':plugin or PLUGIN_DIGEST},sort_keys=True)+'\n').encode()

def fedora_environment(home):
 env=dict(os.environ)
 env['LD_LIBRARY_PATH']=str(home/Path(LIB).parent)+(':'+env['LD_LIBRARY_PATH'] if env.get('LD_LIBRARY_PATH') else '')
 previous=env.get('GST_PLUGIN_PATH_1_0',env.get('GST_PLUGIN_PATH',''))
 env['GST_PLUGIN_PATH_1_0']=str(home/Path(PLUGIN).parent)+(':'+previous if previous else '')
 env['GST_REGISTRY']=str(home/REGISTRY);env['GST_REGISTRY_1_0']=str(home/REGISTRY)
 return env

def verify_fedora(home,system):
 import ctypes as c
 library=c.CDLL(str(home/LIB));p=c.c_void_p
 library.WelsCreateDecoder.argtypes=[c.POINTER(p)];library.WelsCreateDecoder.restype=c.c_long
 decoder=p();rc=library.WelsCreateDecoder(c.byref(decoder))
 if rc or not decoder:raise ValueError('Parole H264 decoder initialization failed')
 library.WelsDestroyDecoder.argtypes=[p];library.WelsDestroyDecoder(decoder)
 for factory in ('openh264dec','avdec_aac','qtdemux'):
  result=subprocess.run([str(system/'usr/bin/gst-inspect-1.0'),factory],env=fedora_environment(home),stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL,timeout=20)
  if result.returncode:raise ValueError('Parole MP4 component unavailable: '+factory)

def ready(home,with_plugin=False):
 try:
  m=maintenance();raw=m.read(home/STATE)
  if raw is None:return False
  entries=json.loads(raw)
  expected={LIB,WRAPPER,LAUNCHER}|({PLUGIN} if with_plugin else set())
  if not isinstance(entries,dict) or set(entries)!=expected:return False
  if with_plugin and (entries[PLUGIN]!=PLUGIN_DIGEST or m.read(home/PROOF)!=proof()):return False
  for name,digest in entries.items():
   data=m.read(home/name,2*1024*1024 if name in (LIB,PLUGIN) else 65536)
   if data is None or hashlib.sha256(data).hexdigest()!=digest:return False
  return entries[LIB]==DIGEST and os.access(home/WRAPPER,os.X_OK)
 except (OSError,ValueError):return False

def install(home,system,source,plugin_source=None):
 m=maintenance();library=m.read(source,2*1024*1024)
 if library is None or hashlib.sha256(library).hexdigest()!=DIGEST:raise ValueError('OpenH264 asset integrity failure')
 plugin=m.read(plugin_source,2*1024*1024) if plugin_source is not None else None
 if plugin_source is not None and (plugin is None or hashlib.sha256(plugin).hexdigest()!=PLUGIN_DIGEST):raise ValueError('GStreamer OpenH264 asset integrity failure')
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
 if plugin is not None:
  wrapper=wrapper.replace(b'exec /usr/bin/parole',b'jobs_gst_path=${GST_PLUGIN_PATH_1_0-${GST_PLUGIN_PATH:-}}\nexport GST_PLUGIN_PATH_1_0="$HOME/.local/lib/termux-jobs/openh264/gstreamer-1.0${jobs_gst_path:+:$jobs_gst_path}"\nunset jobs_gst_path\nexport GST_REGISTRY="$HOME/.local/state/termux-jobs/parole-gst-registry.bin" GST_REGISTRY_1_0="$HOME/.local/state/termux-jobs/parole-gst-registry.bin"\nexec /usr/bin/parole')
 files={LIB:library,WRAPPER:wrapper,LAUNCHER:('\n'.join(lines)+'\n').encode()}
 if plugin is not None:files[PLUGIN]=plugin
 raw=m.read(home/STATE);recorded=json.loads(raw) if raw is not None else None
 if recorded is not None and (not isinstance(recorded,dict) or set(recorded) not in (set(files),{LIB,WRAPPER,LAUNCHER})):raise ValueError('invalid Parole codec state')
 # Validate every destination before any write, including interrupted setup.
 for name in files:
  data=m.read(home/name,2*1024*1024 if name in (LIB,PLUGIN) else 65536)
  if data is not None and (recorded is None or name not in recorded or hashlib.sha256(data).hexdigest()!=recorded[name]):raise ValueError('Parole codec files have local changes; preserved')
 if plugin is not None:
  previous=m.read(home/PROOF);owned=proof(recorded.get(LIB),recorded.get(PLUGIN)) if recorded else proof()
  if previous not in (None,b'pending',owned,proof()):raise ValueError('Parole codec proof has local changes; preserved')
  m.write(home/PROOF,b'pending')
 m.write(home/STATE,json.dumps({name:hashlib.sha256(data).hexdigest() for name,data in files.items()},sort_keys=True).encode())
 for name,data in files.items():m.write(home/name,data,0o700 if name==WRAPPER else 0o600)
 if plugin is not None:verify_fedora(home,system);m.write(home/PROOF,proof())
 if not ready(home,plugin is not None):raise ValueError('Parole decoder setup incomplete')
 print('JOBS_PAROLE_CODEC:openh264-2.6.0',flush=True)
