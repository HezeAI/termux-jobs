#!/usr/bin/env python3
"""Optional GPU tools: selected-home launchers, never install packages at startup."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import stat
import subprocess
import sys
import tempfile

TARGETS={'termux','ubuntu','jobs-x11-ubuntu','debian','alpine','fedora','opensuse','void','archlinux'}
MAX=512*1024
VIRGL_EGL_INFO={'ubuntu','alpine','fedora','void','debian','archlinux','opensuse'}

def egl_info():
    """Query this process's selected Mesa, without GLX/X_GetImage or a window."""
    import ctypes as c
    egl=c.CDLL('libEGL.so.1')
    pointer=c.c_void_p;integer=c.c_int;ints=c.POINTER(integer)
    def function(name,result,args):
        fn=getattr(egl,name);fn.restype=result;fn.argtypes=args;return fn
    display=function('eglGetDisplay',pointer,[pointer])(None)
    initialize=function('eglInitialize',c.c_uint,[pointer,ints,ints])
    bind=function('eglBindAPI',c.c_uint,[c.c_uint])
    choose=function('eglChooseConfig',c.c_uint,[pointer,ints,c.POINTER(pointer),integer,ints])
    pbuffer=function('eglCreatePbufferSurface',pointer,[pointer,pointer,ints])
    context=function('eglCreateContext',pointer,[pointer,pointer,pointer,ints])
    current=function('eglMakeCurrent',c.c_uint,[pointer,pointer,pointer,pointer])
    proc=function('eglGetProcAddress',pointer,[c.c_char_p])
    error=function('eglGetError',integer,[])
    destroy_context=function('eglDestroyContext',c.c_uint,[pointer,pointer])
    destroy_surface=function('eglDestroySurface',c.c_uint,[pointer,pointer])
    terminate=function('eglTerminate',c.c_uint,[pointer])
    major=integer();minor=integer();surface=ctx=None;initialized=False
    def require(ok,step):
        if not ok:raise ValueError('EGL '+step+' failed (0x%04x)'%error())
    try:
        require(display,'display');require(initialize(display,c.byref(major),c.byref(minor)),'initialize');initialized=True
        require(bind(0x30A2),'OpenGL API')
        attrs=(integer*11)(0x3033,1,0x3040,8,0x3024,8,0x3023,8,0x3022,8,0x3038)
        config=pointer();count=integer()
        require(choose(display,attrs,c.byref(config),1,c.byref(count)) and count.value>0,'config')
        surface=pbuffer(display,config,(integer*5)(0x3057,1,0x3056,1,0x3038));require(surface,'pbuffer')
        ctx=context(display,config,None,(integer*1)(0x3038));require(ctx,'context')
        require(current(display,surface,surface,ctx),'make current')
        address=proc(b'glGetString');require(address,'glGetString')
        get_string=c.CFUNCTYPE(c.c_char_p,c.c_uint)(address)
        values=[]
        for label,key in [('GL_VENDOR',0x1F00),('GL_RENDERER',0x1F01),('GL_VERSION',0x1F02)]:
            raw=get_string(key)
            if not raw:raise ValueError('OpenGL information unavailable')
            text=raw[:512].decode('utf-8','replace')
            values.append(label+': '+''.join(ch if ch.isprintable() else '?' for ch in text))
        print('Query: EGL pbuffer (not a GLX test)\nEGL: %d.%d'%(major.value,minor.value),flush=True)
        for value in values:print(value,flush=True)
        return 0
    finally:
        if initialized:
            current(display,None,None,None)
            if ctx:destroy_context(display,ctx)
            if surface:destroy_surface(display,surface)
            terminate(display)

def virgl_info():
    # A short-lived child bounds driver hangs/crashes and leaves parent library
    # paths untouched. Keep the selected guest Mesa and private vtest socket.
    env=dict(os.environ,EGL_PLATFORM='surfaceless',PYTHONDONTWRITEBYTECODE='1')
    try:
        result=subprocess.run([sys.executable,'-B',str(Path(__file__).resolve()),'virgl-info'],env=env,timeout=20)
    except subprocess.TimeoutExpired:
        print('GPU INFO timed out. Save X11 diagnostics and retry after restarting this desktop.',flush=True);return 1
    if result.returncode:
        print('GPU INFO could not query the selected VirGL driver. Save X11 diagnostics; rendering may still work.',flush=True);return 1
    return 0

def commands(target):
    if target not in TARGETS:raise ValueError('unsupported GPU tools target')
    return ['glxinfo','glxgears' if target=='alpine' else 'glmark2-es2' if target=='opensuse' else 'glmark2']

def ready(target):
    return all(shutil.which(c) for c in commands(target))

def directory(home,relative):
    if not home.is_absolute() or '..' in relative.parts:raise ValueError('GPU shortcut path')
    parent=home
    for name in relative.parts:
        parent=parent/name
        if parent.is_symlink():raise ValueError('linked GPU shortcut directory preserved')
        parent.mkdir(mode=0o700,exist_ok=True)
    return parent

def read(path):
    try:fd=os.open(path,os.O_RDONLY|os.O_NOFOLLOW|os.O_NONBLOCK)
    except FileNotFoundError:return None
    with os.fdopen(fd,'rb') as source:
        info=os.fstat(source.fileno())
        if not stat.S_ISREG(info.st_mode) or info.st_size>MAX:raise ValueError('GPU shortcut file preserved')
        value=source.read(MAX+1)
        if len(value)>MAX:raise ValueError('GPU shortcut file too large')
        return value

def write(path,content,mode,expected):
    fd,name=tempfile.mkstemp(prefix='.jobs-gpu-',dir=path.parent)
    try:
        with os.fdopen(fd,'wb') as output:
            os.fchmod(output.fileno(),mode);output.write(content);output.flush();os.fsync(output.fileno())
        if read(path)!=expected:raise ValueError('changed GPU shortcut preserved')
        os.replace(name,path)
    finally:
        if os.path.exists(name):os.unlink(name)

def quote(argument):
    if any(ord(c)<32 for c in argument):raise ValueError('GPU command path contains controls')
    # Desktop Entry Exec is not shell syntax. Escape reserved field codes as %%.
    return '"'+''.join('\\'+c if c in '\\"`$' else '%%' if c=='%' else c for c in argument)+'"'

def desktop_directory(home):
    config=Path(os.environ.get('XDG_CONFIG_HOME') or home/'.config')
    relative=config.relative_to(home)
    if '..' in relative.parts:raise ValueError('GPU config outside selected home')
    # Only read a local XDG setting, never source/eval user-dirs.dirs as shell.
    for parent in [config,*config.parents]:
        if parent==home:break
        if parent.is_symlink():raise ValueError('linked GPU config preserved')
    setting=read(config/'user-dirs.dirs')
    result=home/'Desktop'
    if setting:
        for line in setting.decode('utf-8').splitlines():
            if line.startswith('XDG_DESKTOP_DIR='):
                value=line.split('=',1)[1].strip()
                if len(value)>=2 and value[0]==value[-1]=='"':value=value[1:-1]
                if value=='$HOME' or value.startswith('$HOME/'):value=str(home)+value[5:]
                result=Path(value);break
    relative=result.relative_to(home)
    if '..' in relative.parts:raise ValueError('GPU desktop outside selected home')
    return directory(home,relative)

def prepare(home,target):
    commands(target)
    if not ready(target):return 'missing-tools'
    home=Path(home)
    support=directory(home,Path('.local/share/termux-jobs/gpu-tools'))
    manifest=support/'shortcuts.json';original=read(manifest)
    known=json.loads(original) if original else {}
    if not isinstance(known,dict):raise ValueError('GPU shortcut metadata')
    desktop=desktop_directory(home);helper=support/'gpu-tools.py'
    python=shutil.which('python3');terminal=shutil.which('xfce4-terminal')
    if not python or not terminal:raise ValueError('GPU tools terminal missing')
    payload=Path(__file__).read_bytes()
    files={helper:(payload,0o600)}
    for name,action,icon in [('GPU','test','applications-games'),('GPU INFO','info','dialog-information')]:
        args=[terminal,'--disable-server','--hold','--title='+name,'--execute',python,str(helper),'run',target,action]
        entry='[Desktop Entry]\nVersion=1.0\nType=Application\nName='+name+'\nComment=Termux Job\'s GPU tools\nIcon='+icon+'\nTerminal=false\nStartupNotify=false\nExec='+' '.join(quote(a) for a in args)+'\n'
        files[desktop/('termux-jobs-gpu'+('-info' if action=='info' else '')+'.desktop')]=(entry.encode(),0o755)
    # Preflight all collisions before changing any managed file. User edits win.
    originals={}
    for path,(data,mode) in files.items():
        old=read(path)
        originals[path]=old
        if old is not None and old!=data and known.get(str(path.relative_to(home)))!=hashlib.sha256(old).hexdigest():raise ValueError('edited GPU shortcut preserved')
    for path,(data,mode) in files.items():
        if originals[path]!=data:write(path,data,mode,originals[path])
        known[str(path.relative_to(home))]=hashlib.sha256(data).hexdigest()
    value=(json.dumps(known,sort_keys=True)+'\n').encode()
    if original!=value:write(manifest,value,0o600,original)
    return 'ready'

def run(target,action):
    info,test=commands(target)
    if action not in ('info','test'):raise ValueError('GPU action')
    command=info if action=='info' else test
    print("Termux Job's · "+('GPU INFO' if action=='info' else 'GPU'),flush=True)
    print('Environment: '+target+'\nSelected renderer: '+os.environ.get('JOBS_X11_RENDERER','unknown'),flush=True)
    if not os.environ.get('DISPLAY'):
        print('Open this shortcut inside the running X11 desktop.',flush=True);return 1
    if action=='info' and target in VIRGL_EGL_INFO and os.environ.get('JOBS_X11_RENDERER')=='virgl':
        return virgl_info()
    if not shutil.which(command):
        print('Missing '+command+'. Install GPU & GPU INFO in Programs > Recommended for this environment.',flush=True);return 1
    if target=='alpine' and action=='test':print('glxgears: rendering check only; FPS is not a glmark2 score. Close the gears window to stop.',flush=True)
    if target=='opensuse' and action=='test':print('glmark2-es2: OpenGL ES / EGL test. Compare scores only with the same test and settings.',flush=True)
    args=[command]+(['-B'] if action=='info' else ['-info'] if target=='alpine' else [])
    # Preserve DISPLAY, XAUTHORITY and this desktop's selected renderer libraries.
    return subprocess.call(args)

def main():
    if sys.argv[1:]==['virgl-info']:
        try:raise SystemExit(egl_info())
        except (OSError,ValueError,AttributeError) as error:
            # Fixed codes from our own query, never arbitrary loader paths/text.
            if isinstance(error,ValueError):print(str(error),flush=True)
            else:print('EGL query unavailable in the selected environment.',flush=True)
            raise SystemExit(1)
    elif len(sys.argv)==3 and sys.argv[1]=='prepare':
        print('JOBS_X11_GPU_SHORTCUTS='+prepare(Path.home(),sys.argv[2]),flush=True)
    elif len(sys.argv)==4 and sys.argv[1]=='run':
        raise SystemExit(run(sys.argv[2],sys.argv[3]))
    else:raise ValueError('invalid GPU tools request')

if __name__=='__main__':
    try:main()
    except (OSError,ValueError,UnicodeError):
        print('ERROR:GPU_TOOLS_SHORTCUTS: files unavailable or user edits preserved',flush=True);raise SystemExit(1)
