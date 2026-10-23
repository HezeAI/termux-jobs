#!/usr/bin/env python3
"""Scoped selected-desktop maintenance. Never run against the host or agent Ubuntu."""
import hashlib,json,os,re,stat,sys,tempfile
from pathlib import Path
WRAPPER=b'#!/bin/sh\n# Termux Jobs Firefox PRoot compatibility v1\nMOZ_SHM_NO_SEALS=1 exec /usr/bin/firefox "$@"\n'
WRAPPER_PATH='.local/bin/termux-jobs-firefox'
LAUNCHER_PATH='.local/share/applications/firefox.desktop'
FEDORA_LAUNCHER_PATH='.local/share/applications/org.mozilla.firefox.desktop'
STATE_PATH='.local/state/termux-jobs/firefox-compat.json'
OLD={
 'computer.desktop':b'[Desktop Entry]\nType=Application\nExec=pcmanfm-qt computer:///\nIcon=computer\nName=Computer\n',
 'network.desktop':b'[Desktop Entry]\nType=Application\nExec=pcmanfm-qt network:///\nIcon=folder-network\nName=Network\n',
 'user-home.desktop':b'[Desktop Entry]\nType=Application\nExec=pcmanfm-qt /root\nIcon=user-home\nName=root\n',
}
def safe(path):
    for p in (path,*path.parents):
        if p.is_symlink():raise ValueError('maintenance path is a link')

def read(path,limit=65536):
    safe(path)
    try:fd=os.open(path,os.O_RDONLY|os.O_NONBLOCK|os.O_NOFOLLOW|os.O_CLOEXEC)
    except FileNotFoundError:return None
    try:
        st=os.fstat(fd)
        if not stat.S_ISREG(st.st_mode) or st.st_size>limit:raise ValueError('invalid maintenance file')
        data=os.read(fd,limit+1)
        if len(data)>limit:raise ValueError('maintenance file too large')
        return data
    finally:os.close(fd)

def write(path,data,mode=0o600):
    safe(path);path.parent.mkdir(parents=True,exist_ok=True)
    fd,tmp=tempfile.mkstemp(prefix='.jobs-maintenance-',dir=path.parent)
    try:
        with os.fdopen(fd,'wb') as out:out.write(data);out.flush();os.fchmod(out.fileno(),mode);os.fsync(out.fileno())
        safe(path);os.replace(tmp,path)
    finally:
        if os.path.exists(tmp):os.unlink(tmp)

def digest(data):return hashlib.sha256(data).hexdigest()

def compatibility(home,system,enable,native_prefix=None):
    install=native_prefix if native_prefix is not None else system/'usr'
    statefile=home/STATE_PATH;raw=read(statefile)
    recorded=json.loads(raw) if raw is not None else None
    valid_sets=({WRAPPER_PATH,LAUNCHER_PATH},{WRAPPER_PATH,FEDORA_LAUNCHER_PATH})
    if recorded is not None and (not isinstance(recorded,dict) or set(recorded) not in valid_sets):raise ValueError('invalid Firefox compatibility state')
    launcher_path=next(n for n in recorded if n!=WRAPPER_PATH) if recorded is not None else FEDORA_LAUNCHER_PATH if (install/'share/applications/org.mozilla.firefox.desktop').is_file() and not (install/'share/applications/firefox.desktop').exists() else LAUNCHER_PATH
    # Validate the entire managed set before changing anything. Preserve user edits.
    for name in (WRAPPER_PATH,launcher_path):
        current=read(home/name)
        if current is not None and (recorded is None or digest(current)!=recorded[name]):raise ValueError('Firefox launcher has local changes; preserved')
    if not enable:
        if recorded is None:return
        for name in (launcher_path,WRAPPER_PATH):
            file=home/name
            if read(file) is not None:file.unlink()
        statefile.unlink();return
    if recorded is not None:
        if all(read(home/name) is not None for name in recorded):return
        raise ValueError('partial Firefox compatibility setup; disable before retrying')
    candidates=['lib64/firefox/firefox','lib/firefox/firefox'] if launcher_path==FEDORA_LAUNCHER_PATH else ['lib/firefox/firefox','lib64/firefox/firefox']
    binary=next((install/name for name in candidates if (install/name).is_file()),install/candidates[0])
    # Read the ELF header without loading or executing the browser.
    with binary.open('rb') as stream:
        if stream.read(4)!=b'\x7fELF':raise ValueError('native Firefox is not installed')
    source=read(install/'share/applications'/Path(launcher_path).name)
    if not source:raise ValueError('Firefox launcher not installed')
    lines=source.decode('utf-8').splitlines();count=0
    for i,line in enumerate(lines):
        if line.startswith('Exec='):
            command=str(install/'bin/firefox') if native_prefix is not None else '/usr/bin/firefox'
            match=re.fullmatch(r'Exec=(?:firefox|'+re.escape(command)+'|'+re.escape(str(binary))+r')(\s.*)?',line)
            if not match:raise ValueError('unsupported Firefox launcher command')
            launcher_command=str(home/WRAPPER_PATH) if native_prefix is not None else '/root/'+WRAPPER_PATH
            lines[i]='Exec='+launcher_command+(match.group(1) or '');count+=1
    if not count:raise ValueError('Firefox launcher has no command')
    data=('\n'.join(lines)+'\n').encode()
    wrapper=WRAPPER if native_prefix is None else ('#!/bin/sh\n# Termux Jobs Firefox PRoot compatibility v1\nMOZ_SHM_NO_SEALS=1 exec '+str(native_prefix/'bin/firefox')+' "$@"\n').encode()
    files={WRAPPER_PATH:wrapper,launcher_path:data}
    # Journal first: interrupted enable can always be undone without deleting edits.
    write(statefile,json.dumps({n:digest(d) for n,d in files.items()},sort_keys=True).encode())
    for name,contents in files.items():write(home/name,contents,0o700 if name==WRAPPER_PATH else 0o600)

def clean_shortcuts(home,system):
    if (system/'usr/bin/pcmanfm-qt').exists():return 0
    removed=0
    directories=[home/'Desktop',home/'Рабочий стол']
    settings=read(home/'.config/user-dirs.dirs')
    if settings:
        for line in settings.decode('utf-8').splitlines():
            m=re.fullmatch(r'XDG_DESKTOP_DIR="([^"`]+)"',line)
            if m:
                value=m.group(1).replace('${HOME}',str(home)).replace('$HOME',str(home));p=Path(value)
                if p.is_absolute() and '..' not in p.parts and p!=home and home in p.parents and p not in directories:directories.append(p)
    for directory in directories:
        for name,expected in OLD.items():
            file=directory/name;data=read(file)
            if data!=expected:continue
            before=file.lstat()
            after=file.lstat()
            if (before.st_dev,before.st_ino)!=(after.st_dev,after.st_ino) or read(file)!=data:raise ValueError('shortcut changed during cleanup')
            file.unlink();removed+=1
    return removed

def main():
    if os.environ.get('JOBS_X11_MAINTENANCE')!='1' or Path.home()!=Path('/root'):raise SystemExit(2)
    args=sys.argv[1:]
    if args==['cleanup-shortcuts']:print('JOBS_X11_RETIRED_SHORTCUTS removed='+str(clean_shortcuts(Path.home(),Path('/'))))
    elif args in (['firefox-compat-on'],['firefox-compat-off']):
        compatibility(Path.home(),Path('/'),args[0].endswith('-on'));print('JOBS_X11_FIREFOX_COMPAT='+('on' if args[0].endswith('-on') else 'off'))
    else:raise SystemExit(2)
if __name__=='__main__':main()
