#!/usr/bin/env python3
"""Install/remove only Jobs GPU Test's managed files in the selected home."""
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import shutil
import sys

spec=importlib.util.spec_from_file_location('jobs_gpu_shortcuts',Path(__file__).with_name('x11-gpu-tools.py'))
w=importlib.util.module_from_spec(spec);spec.loader.exec_module(w)
PACKAGE='termux-jobs-gpu-test'
REL=Path('.local/share/termux-jobs/gpu-test')

def paths(home):
    home=Path(home)
    if not home.is_absolute() or home.is_symlink():raise ValueError('GPU test home')
    support=w.directory(home,REL)
    return home,support,support/'managed.json'

def known_files(home,manifest):
    raw=w.read(manifest)
    if raw is None:return {},raw
    data=json.loads(raw)
    if not isinstance(data,dict) or len(data)>8:raise ValueError('GPU test manifest')
    for name,digest in data.items():
        path=Path(name)
        if path.is_absolute() or '..' in path.parts or not isinstance(digest,str) or len(digest)!=64:raise ValueError('GPU test manifest path')
        if name!=str(REL/'app.py') and (path.name!='termux-jobs-gpu-test.desktop' or path.suffix!='.desktop'):raise ValueError('GPU test managed file')
        parent=home/path.parent
        while parent!=home:
            if parent.is_symlink():raise ValueError('GPU test managed directory link')
            parent=parent.parent
    return data,raw

def ready(home):
    home=Path(home);manifest=home/REL/'managed.json'
    if not manifest.is_file():return False
    try:
        # Validate parent links without creating anything during a check.
        for parent in [manifest.parent,*manifest.parents]:
            if parent==home:break
            if parent.is_symlink():return False
        known,_=known_files(home,manifest)
        if len(known)!=3 or str(REL/'app.py') not in known:return False
        if known[str(REL/'app.py')]!=hashlib.sha256(Path(__file__).with_name('x11-gpu-test.py').read_bytes()).hexdigest():return False
        return all(w.read(home/name) is not None and hashlib.sha256(w.read(home/name)).hexdigest()==digest for name,digest in known.items())
    except (OSError,ValueError,TypeError):return False

def prepare(home,target):
    if target not in w.TARGETS:raise ValueError('GPU test target')
    home,support,manifest=paths(home);known,original=known_files(home,manifest)
    python=shutil.which('python3')
    if not python:raise ValueError('GPU test Python missing')
    desktop=w.desktop_directory(home);applications=w.directory(home,Path('.local/share/applications'))
    app=support/'app.py';payload=Path(__file__).with_name('x11-gpu-test.py').read_bytes()
    entry=("[Desktop Entry]\nVersion=1.0\nType=Application\nName=GPU TEST\nComment=Jobs GPU Test: 3D, textures, frame timing and driver info\nIcon=applications-graphics\nTerminal=false\nStartupNotify=false\nCategories=System;Utility;\nExec="+' '.join(w.quote(a) for a in [python,'-B',str(app)])+'\n').encode()
    files={app:(payload,0o600),desktop/'termux-jobs-gpu-test.desktop':(entry,0o755),applications/'termux-jobs-gpu-test.desktop':(entry,0o755)}
    originals={}
    for path,(data,mode) in files.items():
        old=w.read(path);originals[path]=old
        if old is not None and old!=data and known.get(str(path.relative_to(home)))!=hashlib.sha256(old).hexdigest():raise ValueError('Edited GPU test file preserved')
    for path,(data,mode) in files.items():
        if originals[path]!=data:w.write(path,data,mode,originals[path])
    record={str(path.relative_to(home)):hashlib.sha256(data).hexdigest() for path,(data,_) in files.items()}
    w.write(manifest,(json.dumps(record,sort_keys=True)+'\n').encode(),0o600,original)

def remove(home):
    home,support,manifest=paths(home);known,_=known_files(home,manifest)
    for name,digest in known.items():
        value=w.read(home/name)
        if value is not None and hashlib.sha256(value).hexdigest()!=digest:raise ValueError('Edited GPU test file preserved')
    for name in known:(home/name).unlink(missing_ok=True)
    manifest.unlink(missing_ok=True)
    try:support.rmdir()
    except OSError:pass

if __name__=='__main__':
    try:
        if len(sys.argv)!=3 or sys.argv[2] not in w.TARGETS:raise ValueError('GPU test arguments')
        op=sys.argv[1]
        if op=='prepare':prepare(Path.home(),sys.argv[2])
        elif op=='remove':remove(Path.home())
        elif op=='check':sys.exit(0 if ready(Path.home()) else 1)
        else:raise ValueError('GPU test operation')
    except (OSError,ValueError,TypeError) as error:
        print('ERROR:GPU_TEST_SETUP: '+str(error),file=sys.stderr);sys.exit(1)
