#!/usr/bin/env python3
"""Opt-in Firefox-only HTTPS DNS. Journal originals; never alter system DNS."""
import base64,configparser,hashlib,json,re
from pathlib import Path
import importlib.util
spec=importlib.util.spec_from_file_location('maintenance',Path(__file__).with_name('x11-maintenance.py'));m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
STATE='.local/state/termux-jobs/firefox-dns.json'
PREF=re.compile(rb'^user_pref\("network\.trr\.(?:mode|uri)",.*\);[ \t]*$',re.M)
BLOCK=b'\n// Termux Jobs HTTPS DNS BEGIN\nuser_pref("network.trr.mode", 2);\nuser_pref("network.trr.uri", "https://mozilla.cloudflare-dns.com/dns-query");\n// Termux Jobs HTTPS DNS END\n'
def pref_lines(raw):return PREF.findall(raw or b'')
def restore_prefs(path,record):
 raw=m.read(path,2*1024*1024)
 if raw is None:return None
 current=pref_lines(raw);original=[base64.b64decode(v,validate=True) for v in record['prefs']]
 managed=pref_lines(BLOCK)
 # Firefox serializes user.js to prefs.js. Undo only our two keys, retaining all
 # unrelated preferences. Refuse custom edits instead of overwriting them.
 for line in current:
  if line not in managed and line not in original:raise ValueError('Firefox HTTPS DNS preferences changed; preserved')
 cleaned=PREF.sub(b'',raw)
 return cleaned+(b'\n'+b'\n'.join(original)+b'\n' if original else b'')
ROOTS=('.mozilla/firefox','.config/mozilla/firefox')
def entries(home):
 result=[]
 for directory in ROOTS:
  root=home/directory;raw=m.read(root/'profiles.ini')
  if raw is None:continue
  c=configparser.ConfigParser(interpolation=None)
  try:c.read_string(raw.decode())
  except configparser.Error as exc:raise ValueError('Invalid Firefox profiles list') from exc
  for section in c.sections():
   if not section.startswith('Profile'):continue
   value=c.get(section,'Path',fallback='');relative=c.get(section,'IsRelative',fallback='1')
   p=Path(value)
   if relative!='1' or p.is_absolute() or '..' in p.parts or not value:raise ValueError('External Firefox profile is not changed')
   target=root/p/'user.js';m.safe(target)
   if not target.parent.is_dir():raise ValueError('Firefox profile directory is missing')
   result.append(target)
 if not result:raise ValueError('Start Firefox once, close it, then enable HTTPS DNS')
 if len(result)>32:raise ValueError('Invalid Firefox profiles list')
 return list(dict.fromkeys(result))
def journal(home):
 raw=m.read(home/STATE,2*1024*1024)
 if raw is None:return None
 data=json.loads(raw)
 if not isinstance(data,dict) or not 1<=len(data)<=32:raise ValueError('Invalid HTTPS DNS state')
 for rel,record in data.items():
  p=Path(rel)
  if p.is_absolute() or '..' in p.parts or not any(rel.startswith(root+'/') for root in ROOTS) or p.name!='user.js':raise ValueError('Invalid HTTPS DNS path')
  if not isinstance(record,dict) or set(record)!={'before','after','prefs'}:raise ValueError('Invalid HTTPS DNS record')
  if not isinstance(record['after'],str) or not re.fullmatch('[a-f0-9]{64}',record['after']):raise ValueError('Invalid HTTPS DNS hash')
  if record['before'] is not None and (not isinstance(record['before'],str) or len(record['before'])>349528):raise ValueError('Invalid HTTPS DNS backup')
  if not isinstance(record['prefs'],list) or len(record['prefs'])>2:raise ValueError('Invalid Firefox preferences backup')
  for value in record['prefs']:
   if not isinstance(value,str) or len(value)>16384 or not PREF.fullmatch(base64.b64decode(value,validate=True)):raise ValueError('Invalid Firefox preference')
  original=b'' if record['before'] is None else base64.b64decode(record['before'],validate=True)
  if m.digest(original+BLOCK)!=record['after']:raise ValueError('Invalid HTTPS DNS backup hash')
 return data
def state(home):
 try:
  data=journal(home)
  if data is None:return 'off'
  for rel,record in data.items():
   raw=m.read(home/rel,262144)
   original=None if record['before'] is None else base64.b64decode(record['before'],validate=True)
   if (raw is None or m.digest(raw)!=record['after']) and raw!=original:return 'unknown'
  return 'on'
 except (OSError,ValueError,TypeError):return 'unknown'
def configure(home,enable):
 data=journal(home)
 if data is not None:
  # Validate all files before any write; preserve edits, symlinks and foreign paths.
  for rel,record in data.items():
   raw=m.read(home/rel,262144)
   original=None if record['before'] is None else base64.b64decode(record['before'],validate=True)
   if (raw is None or m.digest(raw)!=record['after']) and raw!=original:raise ValueError('Firefox user.js has local changes; preserved')
  if enable:
   for rel,record in data.items():
    raw=m.read(home/rel,262144)
    if raw is None or m.digest(raw)!=record['after']:
     original=None if record['before'] is None else base64.b64decode(record['before'],validate=True)
     m.write(home/rel,(original or b'')+BLOCK)
   return
  prefs={rel:restore_prefs((home/rel).with_name('prefs.js'),record) for rel,record in data.items()}
  originals={rel:None if record['before'] is None else base64.b64decode(record['before'],validate=True) for rel,record in data.items()}
  for rel,raw in originals.items():
   if raw is None:(home/rel).unlink(missing_ok=True)
   else:m.write(home/rel,raw)
  for rel,raw in prefs.items():
   if raw is not None:m.write((home/rel).with_name('prefs.js'),raw)
  (home/STATE).unlink();return
 if not enable:return
 files={};data={}
 for path in entries(home):
  raw=m.read(path,262144-len(BLOCK))
  if raw and b'Termux Jobs HTTPS DNS BEGIN' in raw:raise ValueError('Unmanaged HTTPS DNS block; preserved')
  changed=(raw or b'')+BLOCK;rel=str(path.relative_to(home));files[rel]=changed
  data[rel]={'before':None if raw is None else base64.b64encode(raw).decode(),'after':m.digest(changed),'prefs':[base64.b64encode(line).decode() for line in pref_lines(m.read(path.with_name('prefs.js'),2*1024*1024))]}
 encoded=json.dumps(data,sort_keys=True).encode()
 if len(encoded)>2*1024*1024:raise ValueError('Firefox DNS backup exceeds limit')
 m.write(home/STATE,encoded)
 for rel,raw in files.items():m.write(home/rel,raw)


def main():
 import os,sys
 if os.environ.get('JOBS_X11_MAINTENANCE')!='1' or Path.home()!=Path('/root'):raise SystemExit(2)
 if sys.argv[1:] not in (['state'],['firefox-dns-on'],['firefox-dns-off']):raise SystemExit(2)
 if sys.argv[1]!='state':configure(Path.home(),sys.argv[1].endswith('-on'))
 print('FIREFOX_DNS:'+state(Path.home()),flush=True)

if __name__=='__main__':main()
