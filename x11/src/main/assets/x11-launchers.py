#!/usr/bin/env python3
"""Explicit selected-rootfs permission repair; never modify launcher contents."""
import hashlib,json,os,stat,sys,tempfile
from pathlib import Path
DIRS=(Path('/usr/share/applications'),Path('/usr/local/share/applications'))
def package_checksums(info=Path('/var/lib/dpkg/info')):
    sums={}
    for manifest in info.glob('*.md5sums'):
        if manifest.is_symlink() or manifest.stat().st_size>4*1024*1024:continue
        for line in manifest.read_text(errors='replace').splitlines():
            fields=line.split(None,1)
            if len(fields)!=2 or len(fields[0])!=32:continue
            file=Path('/'+fields[1].lstrip('./'))
            if file.parent in DIRS and file.suffix=='.desktop':sums[str(file)]=fields[0]
    return sums
def repair(directories,manifest,apply=False,approved=None):
    for parent in manifest.parents:
        if parent.is_symlink():raise ValueError('permission backup ancestor is a link')
    records={}
    if manifest.is_symlink():raise ValueError('permission backup is a link')
    if manifest.exists():
        if manifest.stat().st_size>262144:raise ValueError('permission backup too large')
        records=json.loads(manifest.read_text())
        if not isinstance(records,dict):raise ValueError('invalid permission backup')
    checked=eligible=changed=unverified=0
    for directory in directories:
        if directory.is_symlink():raise ValueError('application directory is a link')
        if not directory.is_dir():continue
        for file in sorted(directory.glob('*.desktop')):
            st=file.lstat()
            if not stat.S_ISREG(st.st_mode):continue
            checked+=1;old=stat.S_IMODE(st.st_mode);desired=old|stat.S_IRUSR|stat.S_IWUSR
            if old==desired:continue
            if approved is not None and (st.st_size>262144 or str(file) not in approved or hashlib.md5(file.read_bytes()).hexdigest()!=approved[str(file)]):
                unverified+=1;continue
            eligible+=1
            if not apply:continue
            if checked>4096:raise ValueError('launcher limit exceeded')
            if manifest.parent.is_symlink():raise ValueError('backup directory is a link')
            manifest.parent.mkdir(parents=True,exist_ok=True)
            previous=records.get(str(file))
            if not isinstance(previous,dict) or previous.get('inode')!=st.st_ino or previous.get('device')!=st.st_dev:
                records[str(file)]={'mode':old,'inode':st.st_ino,'device':st.st_dev}
            fd,tmp=tempfile.mkstemp(prefix='.launcher-permissions-',dir=manifest.parent)
            try:
                with os.fdopen(fd,'w') as out:json.dump(records,out);out.flush();os.fsync(out.fileno())
                os.replace(tmp,manifest)
            finally:
                if os.path.exists(tmp):os.unlink(tmp)
            fd=os.open(file,os.O_RDONLY|os.O_NONBLOCK|os.O_NOFOLLOW|os.O_CLOEXEC)
            try:
                current=os.fstat(fd)
                if not stat.S_ISREG(current.st_mode) or current.st_ino!=st.st_ino or current.st_dev!=st.st_dev:raise ValueError('launcher changed during repair')
                os.fchmod(fd,desired);changed+=1
            finally:os.close(fd)
    return {'checked':checked,'eligible':eligible,'repaired':changed,'unverified':unverified}
def main():
    if os.environ.get('JOBS_X11_LAUNCHERS_REPAIR')!='1' or sys.argv[1:] not in [['--check'],['--repair']]:raise SystemExit(2)
    report=repair(DIRS,Path.home()/'.local/state/termux-jobs/launcher-permissions.json',sys.argv[1]=='--repair',package_checksums())
    print('JOBS_X11_LAUNCHER_PERMISSIONS '+json.dumps(report,sort_keys=True))
if __name__=='__main__':main()
