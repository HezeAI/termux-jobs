#!/usr/bin/env python3
"""Private session configuration: keep EXTERNAL and admit this app's kernel UID."""
from pathlib import Path
import os,re,stat,sys,tempfile
TARGETS={'ubuntu','debian','alpine','fedora','opensuse','void','archlinux'}

def prepare(control,target,uid,token,source=Path('/usr/share/dbus-1/session.conf')):
 control=Path(control)
 if target not in TARGETS or type(uid) is not int or not 10000<=uid<2**31 or not re.fullmatch(r'[a-f0-9-]{36}',token):raise ValueError('bus identity')
 if not control.is_absolute() or control.is_symlink() or not control.is_dir():raise ValueError('bus control')
 if not source.is_file():raise ValueError('bus distribution config')
 dest=control/'desktop-bus.conf'
 if dest.is_symlink() or dest.exists() and not dest.is_file():raise ValueError('bus config link')
 before=dest.read_bytes() if dest.exists() else None
 if before is not None and len(before)>8192:raise ValueError('bus config size')
 # No wildcard user, anonymous authentication, TCP listener or modified system XML.
 import xml.etree.ElementTree as ET
 root=ET.Element('busconfig');ET.SubElement(root,'include').text=str(source)
 policy=ET.SubElement(root,'policy',context='default');ET.SubElement(policy,'allow',user=str(uid))
 data=ET.tostring(root,encoding='utf-8',xml_declaration=True)+b'\n'
 fd,tmp=tempfile.mkstemp(prefix='.desktop-bus-',dir=control)
 try:
  with os.fdopen(fd,'wb') as f:os.fchmod(f.fileno(),0o600);f.write(data);f.flush();os.fsync(f.fileno())
  if dest.is_symlink() or (dest.read_bytes() if dest.exists() else None)!=before:raise ValueError('bus config changed')
  os.replace(tmp,dest)
 finally:
  if os.path.exists(tmp):os.unlink(tmp)
 return dest

if __name__=='__main__':
 try:
  if len(sys.argv)!=1:raise ValueError('bus operation')
  uid=os.environ.get('JOBS_X11_APP_UID','')
  if not re.fullmatch(r'[0-9]{1,10}',uid):raise ValueError('bus UID')
  print(prepare(Path(__file__).parent,os.environ.get('JOBS_X11_TARGET',''),int(uid),os.environ.get('JOBS_X11_FOCUS_TOKEN','')),flush=True)
 except (OSError,ValueError):print('ERROR:X11_SESSION_BUS',file=sys.stderr,flush=True);raise SystemExit(1)
