#!/usr/bin/env python3
"""Pinned optional X11 archives. Private caches only; no package/rootfs writes."""
import fcntl
import hashlib
import os
from pathlib import Path
import signal
import stat
import sys
import tempfile
import time
import urllib.error
import urllib.request

BASE = 'https://github.com/HezeAI/termux-jobs-components/releases/download/x11-extras-20261025/'
EXPECTED_PREFIX = '/data/data/com.termux/files/usr'
SPECS = {
    'default-driver': ('Turnip-a6xx-a7xx-a8xx.zip', 'ce35642c5d93ecdfa08b730f80fd738ad71b473cdd7aa3dc58c37d2e6b1e984f', 3507242),
    'kali-style': ('x11-style.zip', '6bc19721d09cb5d16b5ee5df6f2758da9653a88e0211dd81ef5fcb408386aec0', 1830990),
    'virgl-alpine': ('x11-virgl-alpine.zip', 'd226941f42b135c4e5cd58218b50758beb132e5a46d37f4aec8972a481c33739', 4616451),
}


def verified(path, pin, size):
    try:
        fd = os.open(path, os.O_RDONLY | os.O_NOFOLLOW | os.O_NONBLOCK)
    except FileNotFoundError:
        return None
    with os.fdopen(fd, 'rb') as source:
        info = os.fstat(source.fileno())
        if not stat.S_ISREG(info.st_mode) or info.st_nlink != 1:
            raise ValueError('EXTRAS_UNSAFE_FILE')
        if info.st_size != size:
            return None
        data = source.read(size + 1)
    return data if len(data) == size and hashlib.sha256(data).hexdigest() == pin else None


def directory(path):
    if path.is_symlink():
        raise ValueError('EXTRAS_DIRECTORY_LINK')
    path.mkdir(mode=0o700, exist_ok=True)
    if not path.is_dir():
        raise ValueError('EXTRAS_DIRECTORY')


def publish(path, data):
    if path.is_symlink() or path.exists() and (not path.is_file() or path.stat().st_nlink != 1):
        raise ValueError('EXTRAS_UNSAFE_FILE')
    fd, temporary = tempfile.mkstemp(prefix='.extra-', dir=path.parent)
    try:
        with os.fdopen(fd, 'wb') as out:
            os.fchmod(out.fileno(), 0o600)
            out.write(data)
            out.flush()
            os.fsync(out.fileno())
        os.replace(temporary, path)
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)


class HttpsOnly(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        if not newurl.startswith('https://'):
            raise ValueError('EXTRAS_INSECURE_REDIRECT')
        return super().redirect_request(req, fp, code, msg, headers, newurl)


def download(name, pin, size):
    data = bytearray()
    start = time.monotonic()
    try:
        response = urllib.request.build_opener(HttpsOnly()).open(BASE + name, timeout=20)
        with response:
            if not response.geturl().startswith('https://'):
                raise ValueError('EXTRAS_INSECURE_RESPONSE')
            while chunk := response.read(65536):
                data.extend(chunk)
                if len(data) > size or time.monotonic() - start > 300:
                    raise ValueError('EXTRAS_DOWNLOAD_LIMIT')
                print('EXTRA_PROGRESS:%d:%d' % (len(data), size), flush=True)
    except urllib.error.HTTPError as error:
        raise ValueError('EXTRAS_HTTP_' + str(error.code)) from None
    except (urllib.error.URLError, TimeoutError):
        raise ValueError('EXTRAS_NETWORK') from None
    if len(data) != size or hashlib.sha256(data).hexdigest() != pin:
        raise ValueError('EXTRAS_CHECKSUM')
    return bytes(data)


def ensure(key, destination, cache, install=True, fetch=download, cancel_check=lambda: None):
    name, pin, size = SPECS[key]
    destination, cache = Path(destination), Path(cache)
    cancel_check()
    if destination.parent.is_symlink() or not destination.parent.is_dir():
        raise ValueError('EXTRAS_DESTINATION')
    if verified(destination, pin, size) is not None:
        return destination
    directory(cache)
    fd = os.open(cache / 'download.lock', os.O_RDWR | os.O_CREAT | os.O_NOFOLLOW, 0o600)
    try:
        info = os.fstat(fd)
        if not stat.S_ISREG(info.st_mode) or info.st_nlink != 1:
            raise ValueError('EXTRAS_UNSAFE_LOCK')
        while True:
            cancel_check()
            try:
                fcntl.flock(fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
                break
            except BlockingIOError:
                time.sleep(0.1)
        saved = cache / (pin + '.zip')
        data = verified(saved, pin, size)
        if data is None:
            if not install:
                raise ValueError('EXTRAS_REQUIRED')
            print('EXTRA_STAGE:' + key, flush=True)
            data = fetch(name, pin, size)
            # Even injected/local fetchers must pass the production integrity gate.
            if len(data) != size or hashlib.sha256(data).hexdigest() != pin:
                raise ValueError('EXTRAS_CHECKSUM')
            cancel_check()
            publish(saved, data)
        cancel_check()
        publish(destination, data)
        return destination
    finally:
        os.close(fd)


def main():
    if len(sys.argv) != 3 or sys.argv[1] != 'kali-style':
        raise ValueError('EXTRAS_ARGUMENTS')
    prefix = Path(os.environ['PREFIX'])
    if str(prefix) != EXPECTED_PREFIX:
        raise ValueError('EXTRAS_PREFIX')
    control = Path(sys.argv[2])
    allowed = {'workspace-x11'} | {'workspace-x11-' + t for t in ('ubuntu', 'debian', 'alpine', 'fedora', 'opensuse', 'void', 'archlinux')}
    # Context.filesDir and PREFIX can expose Android's same private directory
    # through /data/user/0 and /data/data respectively.
    try:
        same_parent = control.is_absolute() and control.parent.samefile(prefix.parent)
    except OSError:
        same_parent = False
    if not same_parent or control.name not in allowed:
        raise ValueError('EXTRAS_CONTROL')
    ensure('kali-style', control / 'x11-style.zip', prefix.parent / 'workspace-x11-extras')
    print('READY:extras', flush=True)


if __name__ == '__main__':
    signal.signal(signal.SIGTERM, lambda *_: sys.exit(143))
    try:
        main()
    except Exception as error:
        code = str(error) if isinstance(error, ValueError) and str(error).startswith('EXTRAS_') else 'EXTRAS_FAILED'
        print('ERROR:' + code, flush=True)
        sys.exit(1)
