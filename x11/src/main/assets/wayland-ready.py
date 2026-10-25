#!/usr/bin/env python3
"""Private KDE configuration and bounded readiness; no window/input payload logs."""
import os,re,stat,subprocess,sys,time
from pathlib import Path
def directory(path):
    """Walk with O_PATH: Android may permit traversal while refusing root listing."""
    path=Path(path)
    if not path.is_absolute() or '..' in path.parts:raise ValueError('WAYLAND_DIRECTORY')
    fd=os.open('/',os.O_PATH|os.O_DIRECTORY)
    try:
        for part in path.parts[1:]:
            try:child=os.open(part,os.O_PATH|os.O_DIRECTORY|os.O_NOFOLLOW,dir_fd=fd)
            except FileNotFoundError:
                os.mkdir(part,0o700,dir_fd=fd)
                child=os.open(part,os.O_PATH|os.O_DIRECTORY|os.O_NOFOLLOW,dir_fd=fd)
            os.close(fd);fd=child
        rw=os.open('.',os.O_RDONLY|os.O_DIRECTORY|os.O_NOFOLLOW,dir_fd=fd)
        try:
            if os.fstat(rw).st_uid!=os.getuid():raise ValueError('WAYLAND_DIRECTORY_OWNER')
            os.fchmod(rw,0o700)
        finally:os.close(rw)
    finally:os.close(fd)
    return path


def managed(path, content, executable=False, defaults=False):
    """Create only our private fixed files; preserve an edited file by refusing it."""
    data = content.encode()
    flags = os.O_WRONLY | os.O_CREAT | os.O_EXCL | os.O_NOFOLLOW
    try:
        fd = os.open(path, flags, 0o700 if executable else 0o600)
    except FileExistsError:
        fd = os.open(path, os.O_RDONLY | os.O_NOFOLLOW | os.O_NONBLOCK)
        try:
            st=os.fstat(fd)
            if not stat.S_ISREG(st.st_mode) or st.st_uid!=os.getuid() or st.st_size>65536 or (not defaults and os.read(fd, len(data) + 1) != data):
                raise ValueError('PLASMA_MANAGED_CONFIG_CHANGED')
        finally:
            os.close(fd)
        return
    with os.fdopen(fd, 'wb') as stream:
        stream.write(data)


def prepare():
    for key in ('XDG_CONFIG_HOME','XDG_CACHE_HOME','XDG_STATE_HOME'):directory(os.environ[key])
    config=Path(os.environ['XDG_CONFIG_HOME'])
    managed(config/'startkderc','[General]\nsystemdBoot=false\n',defaults=True)
    managed(config/'kscreenlockerrc','[Daemon]\nAutolock=false\nLockOnResume=false\n',defaults=True)
    managed(config/'ksplashrc','[KSplash]\nEngine=none\n',defaults=True)
    # Parent compositor owns wayland-0; KWin's wrapper allocates the inner socket.
    launch=directory(Path(os.environ['XDG_RUNTIME_DIR'])/'launch')
    managed(launch/'kwin_wayland_wrapper',"""#!/bin/sh
set -eu
echo JOBS_X11_STARTUP=kwin-wrapper
read width height < /run/termux-jobs-wayland/output-size
case $width:$height in *[!0-9:]*|:*|*:) exit 2;;esac
[ "$width" -ge 64 ] && [ "$width" -le 4096 ] && [ "$height" -ge 64 ] && [ "$height" -le 4096 ] || exit 2
# KWin's GL_TIMESTAMP query traps in this Linux Turnip/KGSL path. Its native
# fallback skips GPU timing queries while retaining OpenGL GPU compositing.
export KWIN_NO_TIMER_QUERY=1
exec /usr/bin/kwin_wayland_wrapper --wayland-display wayland-0 --width "$width" --height "$height" --no-lockscreen "$@"
""",True)

def ready():
    for name in ('org.kde.KWin','org.kde.plasmashell'):
        p=subprocess.run(['dbus-send','--session','--print-reply','--reply-timeout=500','--dest=org.freedesktop.DBus','/org/freedesktop/DBus','org.freedesktop.DBus.NameHasOwner','string:'+name],stdout=subprocess.PIPE,stderr=subprocess.DEVNULL,timeout=2,text=True)
        if p.returncode or not re.search(r'\bboolean\s+true\b',p.stdout):return False
    env=dict(os.environ,WAYLAND_DISPLAY='wayland-1')
    p=subprocess.run(['wayland-info'],env=env,stdout=subprocess.PIPE,stderr=subprocess.DEVNULL,timeout=2,text=True)
    return p.returncode==0 and all(x in p.stdout for x in ('wl_compositor','xdg_wm_base'))

def watch():
    token=os.environ.get('JOBS_X11_FOCUS_TOKEN','')
    if not re.fullmatch(r'[a-f0-9-]{36}',token):raise ValueError('WAYLAND_TOKEN')
    start=time.monotonic();warned=False
    while time.monotonic()-start<180:
        try:ok=ready()
        except (OSError,subprocess.TimeoutExpired):ok=False
        if ok:
            print('JOBS_X11_WINDOW_MANAGER=ready',flush=True)
            print('JOBS_X11_DESKTOP_READY '+token,flush=True);return
        if time.monotonic()-start>25 and not warned:
            print('JOBS_X11_DESKTOP_TIMEOUT '+token,flush=True);warned=True
        time.sleep(.5)
    print('ERROR:WAYLAND_READY_TIMEOUT',flush=True)

if __name__=='__main__':
    try:
        if sys.argv[1:]==['prepare']:prepare()
        elif sys.argv[1:]==['watch']:watch()
        else:raise ValueError('WAYLAND_OPERATION')
    except Exception:
        print('ERROR:WAYLAND_PRIVATE_SETUP',flush=True);raise SystemExit(1)
