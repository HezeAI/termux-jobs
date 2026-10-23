#!/usr/bin/env python3
"""Inspect backdrop selection/decoder; replace only absent built-in defaults."""
import ctypes as C
import ctypes.util
import os
from pathlib import Path
import subprocess

DEFAULT='/usr/share/xfce4/backdrops/xubuntu-wallpaper.png'
FALLBACK=os.path.join(os.environ.get('JOBS_X11_CONTROL','/tmp/termux-jobs-x11'),'x11-default-wallpaper.png')
CHANNEL='xfce4-desktop'

def query(*args):
    try:
        r=subprocess.run(['xfconf-query','-c',CHANNEL,*args],capture_output=True,text=True,timeout=3)
        return r.stdout.strip() if r.returncode==0 else None
    except (OSError,subprocess.TimeoutExpired):return None

def replacement(value):
    # Never replace a chosen custom image, even when it is currently unavailable.
    return value in (None,'',DEFAULT) and not os.path.isfile(DEFAULT)

def decode(path):
    try:
        if not path or not os.path.isfile(path) or not os.access(path,os.R_OK):return 'unreadable'
        if os.path.getsize(path)>32*1024*1024:return 'skipped-size-limit'
        lib=C.CDLL(ctypes.util.find_library('gdk_pixbuf-2.0') or 'libgdk_pixbuf-2.0.so.0')
        info=lib.gdk_pixbuf_get_file_info;info.argtypes=[C.c_char_p,C.POINTER(C.c_int),C.POINTER(C.c_int)];info.restype=C.c_void_p
        w=C.c_int();h=C.c_int();name=os.fsencode(path)
        if not info(name,C.byref(w),C.byref(h)):return 'unsupported-or-invalid'
        if w.value<1 or h.value<1 or w.value*h.value>40_000_000:return 'skipped-pixel-limit'
        load=lib.gdk_pixbuf_new_from_file_at_scale
        load.argtypes=[C.c_char_p,C.c_int,C.c_int,C.c_int,C.POINTER(C.c_void_p)];load.restype=C.c_void_p
        error=C.c_void_p();pix=load(name,64,64,1,C.byref(error))
        if error:
            glib=C.CDLL(ctypes.util.find_library('glib-2.0') or 'libglib-2.0.so.0')
            glib.g_error_free.argtypes=[C.c_void_p];glib.g_error_free(error)
        if not pix:return 'decode-failed'
        obj=C.CDLL(ctypes.util.find_library('gobject-2.0') or 'libgobject-2.0.so.0')
        obj.g_object_unref.argtypes=[C.c_void_p];obj.g_object_unref(pix)
        return 'ok-%dx%d'%(w.value,h.value)
    except (OSError,ValueError):return 'decoder-unavailable'

def main(repair=True,brief=False):
    print('JOBS_X11_BACKDROP %s-snapshot default-present=%s'%('startup' if repair else 'current',str(os.path.isfile(DEFAULT)).lower()),flush=True)
    if not brief:
        mode=query('-p','/backdrop/single-workspace-mode')
        number=query('-p','/backdrop/single-workspace-number')
        print('JOBS_X11_BACKDROP single-workspace=%s selected=%s'%(mode if mode in ('true','false') else 'unset',number if number and number.isdigit() else 'unset'),flush=True)
    props=query('-l')
    if props is None:
        print('JOBS_X11_BACKDROP settings-unavailable',flush=True);return
    # Match the native core's initial connector and never rename it on attach.
    expected=['/backdrop/screen0/monitorscreen/workspace%d/last-image'%n for n in range(4)]
    legacy=['/backdrop/screen0/monitorTermuxJobsX11/workspace%d/last-image'%n for n in range(4)]
    listed=set(props.splitlines())
    properties=sorted(set(p for p in listed if p.startswith('/backdrop/') and p.endswith('/last-image'))|set(expected))[:32]
    if brief:properties=expected
    for i,prop in enumerate(properties):
        value=query('-p',prop)
        readable_property=value is not None or prop not in listed
        if repair and prop in expected and readable_property and value in (None,'',DEFAULT,FALLBACK):
            old=legacy[expected.index(prop)]
            chosen=query('-p',old) if old in listed else None
            if chosen and chosen not in (DEFAULT,FALLBACK) and os.path.isfile(chosen) and os.access(chosen,os.R_OK):
                args=['-p',prop,'-s',chosen]
                if prop not in listed:args+=['-n','-t','string']
                if query(*args) is not None:
                    value=chosen
                    print('JOBS_X11_BACKDROP image=%d selection-restored=true'%i,flush=True)
                    # Transfer style/cycling only when the destination has no value.
                    for key,typ,allowed in [('image-style','int',tuple(str(x) for x in range(7))),('cycle-enable','bool',('true','false'))]:
                        dest=prop.rsplit('/',1)[0]+'/'+key
                        source=old.rsplit('/',1)[0]+'/'+key
                        previous=query('-p',source)
                        if dest not in listed and previous in allowed:query('-p',dest,'-n','-t',typ,'-s',previous)
        if repair and prop in expected and readable_property and replacement(value) and os.path.isfile(FALLBACK):
            args=['-p',prop,'-s',FALLBACK]
            if prop not in listed:args+=['-n','-t','string']
            ok=query(*args) is not None
            print('JOBS_X11_BACKDROP image=%d fallback=%s'%(i,'applied' if ok else 'failed'),flush=True)
            if ok:value=FALLBACK
        if brief:continue
        # Property path names are metadata, but do not output arbitrary property/image strings.
        workspace=prop.rsplit('/',2)[-2]
        if not (workspace.startswith('workspace') and workspace[9:].isdigit()):workspace='other'
        style=query('-p',prop.rsplit('/',1)[0]+'/image-style')
        cycle=query('-p',prop.rsplit('/',1)[0]+'/cycle-enable')
        print('JOBS_X11_BACKDROP image=%d own-monitor=%s workspace=%s style=%s cycle=%s decode=%s'%(i,str(prop in expected).lower(),workspace,style if style in tuple(str(x) for x in range(7)) else 'unset',cycle if cycle in ('true','false') else 'unset',decode(value)),flush=True)

if __name__=='__main__':
    import sys
    main(repair='--inspect' not in sys.argv[1:],brief='--quick-start' in sys.argv[1:])
