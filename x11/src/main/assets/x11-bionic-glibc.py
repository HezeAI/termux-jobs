#!/usr/bin/env python3
"""Private, Selected glibc experimental Bionic ICD cache. Never installs packages."""
import contextlib
import importlib.util
import json
import os
from pathlib import Path
import re
import shlex
import shutil
import struct
import sys
import tempfile
import zipfile

_spec = importlib.util.spec_from_file_location('jobs_driver_library', Path(__file__).with_name('x11-wrapper.py'))
w = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(w)
VERSION = 'hybris-wsi-ubuntu-2'
PIN = 'bfa1acd82b0f69283125174f68e248add7c1d851bb2a90b3fc96218c819f4872'
FILES = {'libjobs-bionic.so', 'libhybris-common.so.1', 'linker/q.so',
         'libVkLayer_window_system_integration.so', 'bionic-icd.json',
         'mesa/libGLX_mesa.so.0', 'mesa/libEGL_mesa.so.0', 'mesa/libgallium-26.1.6.so',
         'share/vulkan/implicit_layer.d/jobs-bionic.json'}


def valid_runtime(base):
    runtime = base / 'runtime'
    if runtime.is_symlink() or not runtime.is_dir():
        raise ValueError('BIONIC_RUNTIME_MISSING')
    value = w.read_json(runtime / 'ready.json')
    if value.get('version') != VERSION or value.get('bundle') != PIN or set(value.get('files', {})) != FILES:
        raise ValueError('BIONIC_RUNTIME_CHANGED')
    for name, digest in value['files'].items():
        path = runtime / name
        # No runtime component may redirect outside our private cache.
        parent = path.parent
        while parent != runtime:
            if parent.is_symlink():
                raise ValueError('BIONIC_RUNTIME_LINK')
            parent = parent.parent
        if w.digest(path) != digest:
            raise ValueError('BIONIC_RUNTIME_CHANGED')
    return runtime


def install(base):
    with w.lock(base):
        w.driver(base)
        try:
            valid_runtime(base)
            return
        except (OSError, ValueError, TypeError, KeyError, AttributeError):
            pass
        archive = Path(__file__).with_name('x11-bionic-glibc.zip')
        if w.digest(archive) != PIN:
            raise ValueError('BIONIC_BUNDLE_CHECKSUM')
        stage = Path(tempfile.mkdtemp(prefix='.runtime-', dir=base))
        try:
            hashes = {}
            with zipfile.ZipFile(archive) as packed:
                members = w.entries(packed)
                if {m.filename for m in members} != FILES:
                    raise ValueError('BIONIC_BUNDLE_FILES')
                for member in members:
                    w.cancelled()
                    data = packed.read(member)
                    if re.search(r'\.so(?:\.[0-9]+)*$', member.filename):
                        if len(data) < 64 or data[:7] != b'\x7fELF\x02\x01\x01' or struct.unpack_from('<HH', data, 16) != (3, 183):
                            raise ValueError('BIONIC_BUNDLE_ARM64')
                    out = stage / member.filename
                    out.parent.mkdir(mode=0o700, parents=True, exist_ok=True)
                    out.write_bytes(data)
                    out.chmod(0o700 if '.so' in out.name else 0o600)
                    hashes[member.filename] = w.digest(out)
            w.write_json(stage / 'ready.json', dict(version=VERSION, bundle=PIN, files=hashes))
            w.cancelled()
            runtime = base / 'runtime'
            if runtime.is_symlink():
                raise ValueError('BIONIC_RUNTIME_LINK')
            # Only our cache, under the exclusive lock; a running desktop holds a shared lock.
            old = base / '.runtime-old'
            if old.is_symlink():
                raise ValueError('BIONIC_RUNTIME_LINK')
            if old.exists():
                shutil.rmtree(old)
            if runtime.exists():
                runtime.rename(old)
            try:
                stage.rename(runtime)
            except BaseException:
                if old.exists():
                    old.rename(runtime)
                raise
            if old.exists():
                shutil.rmtree(old)
        finally:
            if stage.exists():
                shutil.rmtree(stage)


@contextlib.contextmanager
def shared(base):
    w.directory(base)
    fd = os.open(base / 'operation.lock', os.O_CREAT | os.O_RDONLY | os.O_NOFOLLOW, 0o600)
    try:
        import fcntl
        fcntl.flock(fd, fcntl.LOCK_SH | fcntl.LOCK_NB)
        yield
    finally:
        os.close(fd)


def environment(base):
    with shared(base):
        valid_runtime(base)
        value, _ = w.driver(base)
        # A fixed guest bind, never a shared-storage path or shell text from metadata.
        driver = '/tmp/termux-jobs-bionic-glibc/drivers/' + value['id'] + '/' + value['library']
        print('export JOBS_BIONIC_DRIVER=' + shlex.quote(driver))


def main(argv):
    if len(argv) != 7:
        raise ValueError('BIONIC_ARGUMENTS')
    _, operation, target, cache, source, api, cancel = argv
    if target not in ('ubuntu', 'debian', 'fedora', 'archlinux', 'opensuse', 'void'):
        raise ValueError('BIONIC_TARGET_UNSUPPORTED')
    base = Path(cache)
    if not base.is_absolute():
        raise ValueError('BIONIC_CACHE_PATH')
    if operation != 'environment':
        expected = 'workspace-x11-bionic-glibc' + ('' if target == 'ubuntu' else '-'+target)
        if base.name != expected:
            raise ValueError('BIONIC_CACHE_TARGET')
    w.CANCEL = None if cancel == '-' else Path(cancel)
    w.cancelled()
    if operation == 'import':
        w.import_driver(base, Path(source), int(api))
        print('READY:wrapper-driver')
    elif operation == 'catalog':
        print('WRAPPER_DRIVERS:' + json.dumps(w.catalog(base), ensure_ascii=True))
        print('READY:wrapper-catalog')
    elif operation == 'select':
        w.select_driver(base, source, int(api))
        print('READY:wrapper-selected')
    elif operation == 'install':
        install(base)
        print('READY:wrapper')
    elif operation == 'check':
        with shared(base):
            w.driver(base)
            valid_runtime(base)
        print('READY:wrapper')
    elif operation == 'environment':
        environment(base)
    else:
        raise ValueError('BIONIC_OPERATION')


if __name__ == '__main__':
    try:
        main(sys.argv)
    except (OSError, ValueError, KeyError, TypeError, AttributeError, zipfile.BadZipFile) as error:
        print('ERROR:' + str(error), flush=True)
        raise SystemExit(1)
