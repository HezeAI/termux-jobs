#!/usr/bin/env python3
"""Arch Android-compositor driver library. No Linux package/runtime installation."""
import importlib.util
import os
from pathlib import Path
import re
import sys


def main():
    if len(sys.argv)!=7:raise ValueError('WRAPPER_ARGUMENTS')
    operation,target,path,source,api,cancel=sys.argv[1:]
    if target!='wayland-archlinux' or operation not in ('import','catalog','select','check'):
        raise ValueError('WRAPPER_WAYLAND_OPERATION')
    base=Path(path);expected=Path(os.environ['PREFIX']).parent/'workspace-wayland-driver-archlinux'
    if base.name!=expected.name or not base.parent.samefile(expected.parent) or base.is_symlink():raise ValueError('WRAPPER_CACHE_PATH')
    if cancel!='-':
        marker=Path(cancel)
        if not re.fullmatch(r'\.job-[A-Za-z0-9_-]+',marker.parent.name) or marker.parent.parent!=base or marker.name!='cancel' or marker.parent.is_symlink():raise ValueError('WRAPPER_CANCEL_PATH')
    spec=importlib.util.spec_from_file_location('jobs_driver',Path(__file__).with_name('x11-wrapper.py'))
    module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
    module.CANCEL=Path(cancel) if cancel!='-' else None
    if operation=='check':
        with module.lock(base):module.driver(base)
        print('READY:wrapper')
    else:
        # Reuse reviewed ZIP/ELF/hash/catalog code, with a separate validated base.
        sys.argv[2]='termux'
        module.main()

if __name__=='__main__':
    try:main()
    except Exception as error:
        reason=str(error)
        if not re.fullmatch(r'WRAPPER_[A-Z0-9_]+',reason):reason='WRAPPER_FAILED'
        print('ERROR:'+reason,flush=True);raise SystemExit(1)
