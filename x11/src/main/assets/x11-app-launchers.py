#!/usr/bin/env python3
"""Owned per-desktop application launchers; never alter packages or user profiles."""
import fcntl,os,shlex,stat,sys,tempfile
from pathlib import Path
SPECS={
 'kali':{'evince':'/usr/bin/evince','jobs-nmap':'/usr/bin/nmap','jobs-apktool':'/usr/bin/apktool'},
 'manjaro':{name:'/usr/lib/libreoffice/program/soffice' for name in ('libreoffice','soffice')},
}
def contents(target,name,system=Path('/')):
 binary=SPECS[target][name]
 if target=='kali' and name in ('jobs-nmap','jobs-apktool'):
  command=shlex.quote(str(Path(system)/binary.lstrip('/')))
  flag=' --unprivileged' if name=='jobs-nmap' else ''
  return ('#!/bin/sh\n# Termux Jobs app compatibility v1: '+target+'/'+name+'\n'+('export NMAP_UNPRIVILEGED=1\n' if name=='jobs-nmap' else '')+'if [ "$#" -gt 0 ]; then exec '+command+flag+' "$@"; fi\n'+command+flag+(' --help' if name=='jobs-nmap' else '')+'\nexec /bin/bash --noprofile --norc\n').encode()
 command=('GSK_RENDERER=cairo ' if target=='kali' and name=='evince' else '')+'exec '+shlex.quote(str(Path(system)/binary.lstrip('/')))+' "$@"'
 return ('#!/bin/sh\n# Termux Jobs app compatibility v1: '+target+'/'+name+'\n'+command+'\n').encode()
def private_dir(path):
 st=path.lstat()
 if not stat.S_ISDIR(st.st_mode) or st.st_uid!=os.getuid():raise ValueError('APP_LAUNCH_DIRECTORY')
def current(path):
 try:fd=os.open(path,os.O_RDONLY|os.O_NOFOLLOW|os.O_NONBLOCK)
 except FileNotFoundError:return None
 with os.fdopen(fd,'rb') as f:
  st=os.fstat(f.fileno())
  if not stat.S_ISREG(st.st_mode) or st.st_nlink!=1 or st.st_uid!=os.getuid() or st.st_size>4096:raise ValueError('APP_LAUNCH_FILE')
  return f.read(4097)
def prepare(target,control,system=Path('/')):
 if target not in SPECS:return None
 control=Path(control);private_dir(control)
 fd=os.open(control/'app-launchers.lock',os.O_CREAT|os.O_RDWR|os.O_NOFOLLOW,0o600)
 try:
  st=os.fstat(fd)
  if not stat.S_ISREG(st.st_mode) or st.st_nlink!=1 or st.st_uid!=os.getuid():raise ValueError('APP_LAUNCH_LOCK')
  try:fcntl.flock(fd,fcntl.LOCK_EX|fcntl.LOCK_NB)
  except BlockingIOError:raise ValueError('APP_LAUNCH_BUSY') from None
  folder=control/'app-launchers'
  try:folder.mkdir(mode=0o700)
  except FileExistsError:pass
  private_dir(folder)
  if any(p.name not in SPECS[target] for p in folder.iterdir()):raise ValueError('APP_LAUNCH_UNMANAGED')
  before={}
  for name in SPECS[target]:
   before[name]=current(folder/name)
   if before[name] not in (None,contents(target,name,system)):raise ValueError('APP_LAUNCH_LOCAL_CHANGES')
  for name,binary in SPECS[target].items():
   file=folder/name;source=Path(system)/binary.lstrip('/')
   installed=source.is_file() and os.access(source,os.X_OK)
   if not installed:
    if before[name] is not None:
     if current(file)!=before[name]:raise ValueError('APP_LAUNCH_CHANGED')
     file.unlink()
    continue
   data=contents(target,name,system)
   if before[name]==data and file.stat().st_mode&stat.S_IXUSR:continue
   tempfd,temp=tempfile.mkstemp(prefix='.app-launch-',dir=control)
   try:
    with os.fdopen(tempfd,'wb') as out:os.fchmod(out.fileno(),0o700);out.write(data);out.flush();os.fsync(out.fileno())
    if current(file)!=before[name]:raise ValueError('APP_LAUNCH_CHANGED')
    os.replace(temp,file)
   finally:
    if os.path.exists(temp):os.unlink(temp)
   print('JOBS_X11_APP_COMPAT '+target+'/'+name, file=sys.stderr,flush=True)
  # Keep the directory in this desktop PATH before an app is installed, so the
  # explicit program installer can add/remove launchers without restarting XFCE.
  return folder
 finally:os.close(fd)
def shortcuts(home,control):
 # Production uses the controlled guest mount; tests supply private HOME/control.
 folder=Path(home)
 for name in ('.local','share','applications'):
  folder=folder/name
  try:folder.mkdir(mode=0o700)
  except FileExistsError:pass
  private_dir(folder)
 for key,title in [('jobs-nmap','Nmap'),('jobs-apktool','Apktool')]:
  file=folder/('termux-jobs-'+key+'.desktop')
  data=('[Desktop Entry]\nType=Application\nName='+title+'\nComment=Termux Jobs Kali terminal tool\nExec=xfce4-terminal --title='+title+' --command='+key+'\nIcon=utilities-terminal\nTerminal=false\nCategories=Development;Utility;\nX-Termux-Jobs-Kali-Tool=1\n').encode()
  old=current(file)
  if old not in (None,data):raise ValueError('APP_LAUNCH_LOCAL_CHANGES')
  installed=(Path(control)/'app-launchers'/key).is_file()
  if not installed:
   if old is not None:file.unlink()
   continue
  if old==data:continue
  fd,temp=tempfile.mkstemp(prefix='.jobs-tool-',dir=folder)
  try:
   with os.fdopen(fd,'wb') as out:out.write(data);out.flush();os.fsync(out.fileno())
   if current(file)!=old:raise ValueError('APP_LAUNCH_CHANGED')
   os.replace(temp,file)
  finally:
   if os.path.exists(temp):os.unlink(temp)
def main():
 if len(sys.argv)!=2 or sys.argv[1] not in SPECS:raise ValueError('APP_LAUNCH_TARGET')
 target=sys.argv[1]
 if target not in (os.environ.get('JOBS_X11_TARGET'),os.environ.get('JOBS_X11_NATIVE_TARGET')):raise ValueError('APP_LAUNCH_SCOPE')
 folder=prepare(target,Path(__file__).parent)
 if target=='kali':
  try:shortcuts(Path.home(),Path(__file__).parent)
  except (OSError,ValueError) as error:print('JOBS_X11_TOOL_SHORTCUTS:unavailable '+str(error),file=sys.stderr,flush=True)
 if folder is not None:print(folder,flush=True)
if __name__=='__main__':
 try:main()
 except (OSError,ValueError) as error:print('ERROR:'+str(error),file=sys.stderr,flush=True);raise SystemExit(1)
