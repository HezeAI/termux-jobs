#!/usr/bin/env python3
"""Synthetic DRM view inside this KDE child only; never writes /dev or /sys."""
from pathlib import Path
import os,re,sys
from importlib.util import spec_from_file_location,module_from_spec
spec=spec_from_file_location('jobs_setup',Path(__file__).with_name('wayland-ready.py'));m=module_from_spec(spec);spec.loader.exec_module(m)
operation=sys.argv[1] if len(sys.argv)>1 else ''
expected=Path(os.environ['PREFIX']).parent/'workspace-x11-archlinux'
control=Path(sys.argv[2]) if len(sys.argv)>2 else Path('.')
if control.is_symlink() or not control.samefile(expected):raise SystemExit('ERROR:WAYLAND_CONTROL')
if operation in ('check-root','repair-root'):
    base=Path(os.environ['PREFIX'])/'var/lib/proot-distro'
    import stat
    for relative in ('containers/archlinux/rootfs','installed-rootfs/archlinux'):
        fds=[]
        try:
            parent=os.open(base,os.O_RDONLY|os.O_DIRECTORY|os.O_NOFOLLOW);fds.append(parent)
            for part in relative.split('/'):
                parent=os.open(part,os.O_RDONLY|os.O_DIRECTORY|os.O_NOFOLLOW,dir_fd=parent);fds.append(parent)
            info=os.fstat(parent)
            if info.st_uid!=os.getuid():raise ValueError('WAYLAND_ROOT_OWNER')
            mode=stat.S_IMODE(info.st_mode)
            if mode&0o700!=0o700:
                if operation=='check-root':raise SystemExit(1)
                os.fchmod(parent,mode|0o700)
            raise SystemExit(0)
        except FileNotFoundError:continue
        finally:
            for fd in reversed(fds):os.close(fd)
    raise SystemExit('ERROR:INSTALL_ARCH_FIRST')
if operation!='prepare-device' or len(sys.argv)!=4:raise SystemExit('ERROR:WAYLAND_OPERATION')
r=Path(sys.argv[3])
if not re.fullmatch(r'wl-[a-f0-9-]{36}',r.name) or not r.parent.samefile(expected) or r.is_symlink():raise SystemExit('ERROR:WAYLAND_RUNTIME')
# A fresh private /tmp for this launch. Never adopt rootfs/another desktop sockets.
# Keep socket paths below sockaddr_un's 108-byte limit on Android.
# The full wl-UUID/tmp path is already too long for .X11-unix/X0.
identity=r.name[3:]
if not re.fullmatch(r'[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}',identity):raise SystemExit('ERROR:WAYLAND_RUNTIME')
tmp=m.directory(control/('wt-'+identity.replace('-','')[:16]))
m.managed(tmp/'.wayland-owner',r.name+'\n')
if len(os.fsencode(tmp/'.ICE-unix'/'2147483647'))>107:raise SystemExit('ERROR:WAYLAND_SOCKET_PATH')
for name in ('.X11-unix','.ICE-unix'):
    path=m.directory(tmp/name)
    fd=os.open(path,os.O_RDONLY|os.O_DIRECTORY|os.O_NOFOLLOW)
    try:
        if os.fstat(fd).st_uid!=os.getuid():raise ValueError('WAYLAND_DIRECTORY_OWNER')
        os.fchmod(fd,0o1777)
    finally:os.close(fd)
print('JOBS_X11_STARTUP=wayland-socket-directories',flush=True)
st=os.stat('/dev/kgsl-3d0');numbers=f'{os.major(st.st_rdev)}:{os.minor(st.st_rdev)}'
base=m.directory(r/'drm');dri=m.directory(base/'dri');m.managed(dri/'renderD0','')
device=m.directory(base/'sys'/numbers/'device');drm=m.directory(device/'drm'/'renderD0');m.managed(drm/'dev',numbers+'\n');m.managed(device/'uevent','DRIVER=kgsl-3d0\nMODALIAS=platform:kgsl-3d0\n')
cls=m.directory(base/'class')
for p,target in [(device/'subsystem','/sys/bus/platform'),(device/'driver','/sys/bus/platform/drivers/msm_drm'),(cls/'renderD0','/sys/dev/char/'+numbers)]:
    if p.is_symlink():
        if os.readlink(p)!=target:raise SystemExit('ERROR:WAYLAND_DEVICE_LINK')
    elif p.exists():raise SystemExit('ERROR:WAYLAND_DEVICE_PATH')
    else:p.symlink_to(target)
