#!/usr/bin/env python3
"""Download the explicit built-in driver choice, then use existing scoped installers."""
import importlib.util
from pathlib import Path
import sys


def module(name):
    spec = importlib.util.spec_from_file_location('jobs_default_' + name, Path(__file__).with_name('x11-' + name + '.py'))
    value = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value


def prepare(base, target, api, cancel, fetch=None):
    if target not in ('termux', 'ubuntu', 'debian', 'fedora', 'opensuse', 'void', 'archlinux'):
        raise ValueError('WRAPPER_TARGET_UNSUPPORTED')
    wrapper, gpu, extras = module('wrapper'), module('gpu'), module('extras')
    wrapper.CANCEL = cancel
    gpu.CANCEL_FILE = cancel
    wrapper.cancelled()
    try:
        selected = wrapper.driver(base)[0]['id'] == extras.SPECS['default-driver'][1]
    except FileNotFoundError:
        selected = False
    if not selected:
        archive = cancel.parent / 'driver.zip'
        def download(name, pin, size):
            return gpu.download(extras.BASE + name, pin, size,
                                lambda done, total: print('GPU_PROGRESS:%d:%d' % (done, total), flush=True))
        extras.ensure('default-driver', archive, base.parent / 'workspace-x11-extras', fetch=fetch or download,
                      cancel_check=wrapper.cancelled)
        wrapper.cancelled()
        wrapper.import_driver(base, archive, api)
    wrapper.cancelled()
    if target == 'termux':
        wrapper.install_runtime(base)
    else:
        bridge = module('bionic-glibc')
        bridge.w.CANCEL = cancel
        bridge.install(base)
    print('READY:wrapper', flush=True)


if __name__ == '__main__':
    try:
        if len(sys.argv) != 7 or sys.argv[1] != 'install-default':
            raise ValueError('WRAPPER_ARGUMENTS')
        _, _, target, cache, _, api, cancel = sys.argv
        base, marker = Path(cache), Path(cancel)
        expected = 'workspace-x11-wrapper' if target == 'termux' else 'workspace-x11-bionic-glibc' + ('' if target == 'ubuntu' else '-' + target)
        if not base.is_absolute() or base.name != expected or marker.parent.parent != base or not marker.parent.name.startswith('.job-') or marker.name != 'cancel':
            raise ValueError('WRAPPER_CACHE_PATH')
        prepare(base, target, int(api), marker)
    except Exception as error:
        code = str(error) if isinstance(error, ValueError) and str(error).startswith(('WRAPPER_', 'EXTRAS_', 'GPU_', 'BIONIC_')) else 'WRAPPER_DEFAULT_DRIVER_FAILED'
        print('ERROR:' + code, flush=True)
        sys.exit(1)
