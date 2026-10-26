#!/usr/bin/env python3
"""Termux-only experimental Wrapper cache and explicit user driver import.

Never installs a package, executes an archive member, or writes another renderer.
Imported drivers are native code, not a sandbox: UI consent precedes import/use.
"""
import contextlib
import shutil
import fcntl
import hashlib
import importlib.util
import io
import json
import os
from pathlib import Path, PurePosixPath
import re
import stat
import struct
import sys
import tarfile
import tempfile
import zipfile

VERSION = 'leegao-0.0.5r5-termux-1'
URL = 'https://github.com/HezeAI/termux-jobs-components/releases/download/graphics-20261025/vulkan-wrapper-android-leegaos-fork-aarch64.zip'
SHA = 'c50f3a9f884ca2c54ca76d4eb55709a4012cff77ff279216a0f04920c05bcc55'
SIZE = 3280408
LOADER = ('https://github.com/HezeAI/termux-jobs-components/releases/download/graphics-20261025/vulkan-loader-generic_1.4.366_aarch64.deb',
          '5b0a17efdd27e064c06dcf5befba4b466eac6025110ecc44be768019b00abdb7', 150484)
RUNTIME = {'libvulkan_wrapper.so', 'libadrenotools.so', 'libfile_redirect_hook.so',
           'libgsl_alloc_hook.so', 'libhook_impl.so', 'libmain_hook.so'}
MAX_ZIP = 64 * 1024 * 1024
MAX_EXPANDED = 192 * 1024 * 1024
CANCEL = None
DEFAULT_DRIVER_ID = 'ce35642c5d93ecdfa08b730f80fd738ad71b473cdd7aa3dc58c37d2e6b1e984f'

def cancelled():
    if CANCEL is not None and CANCEL.exists():
        raise ValueError('WRAPPER_CANCELLED')

def regular(path, limit):
    st = path.lstat()
    if not stat.S_ISREG(st.st_mode) or st.st_size > limit:
        raise ValueError('WRAPPER_FILE')
    return path

def directory(path):
    # Callers supply a private application base, not a shared-storage destination.
    path.mkdir(mode=0o700, parents=True, exist_ok=True)
    if path.is_symlink() or not path.is_dir():
        raise ValueError('WRAPPER_DIRECTORY')
    return path

@contextlib.contextmanager
def lock(base):
    directory(base)
    fd = os.open(base / 'operation.lock', os.O_CREAT | os.O_RDWR | os.O_NOFOLLOW, 0o600)
    try:
        try:
            fcntl.flock(fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            raise ValueError('WRAPPER_BUSY') from None
        yield
    finally:
        os.close(fd)

def elf(data):
    if len(data) < 64 or data[:7] != b'\x7fELF\x02\x01\x01' or struct.unpack_from('<HH', data, 16) != (3, 183):
        raise ValueError('WRAPPER_ARM64_BIONIC_REQUIRED')
    # Reject normal glibc/musl payloads. Full ABI/device compatibility is runtime-tested.
    if any(x in data for x in (b'libc.so.6\0', b'ld-linux-aarch64', b'libc.musl-', b'GLIBC_2.')):
        raise ValueError('WRAPPER_ARM64_BIONIC_REQUIRED')

def entries(archive):
    seen = set()
    total = 0
    files = []
    for member in archive.infolist():
        cancelled()
        path = PurePosixPath(member.filename)
        if (path.is_absolute() or '..' in path.parts or '\\' in member.filename or
                '\0' in member.filename or member.filename in seen):
            raise ValueError('WRAPPER_ARCHIVE_PATH')
        seen.add(member.filename)
        total += member.file_size
        mode = member.external_attr >> 16
        if (len(seen) > 128 or total > MAX_EXPANDED or member.flag_bits & 1 or
                stat.S_ISLNK(mode) or (stat.S_IFMT(mode) not in (0, stat.S_IFREG, stat.S_IFDIR))):
            raise ValueError('WRAPPER_ARCHIVE_LIMIT')
        if not member.is_dir():
            files.append(member)
    return files

def digest(path):
    h = hashlib.sha256()
    with regular(path, MAX_EXPANDED).open('rb') as stream:
        while chunk := stream.read(262144):
            cancelled()
            h.update(chunk)
    return h.hexdigest()

def write_json(path, value):
    if path.is_symlink():
        raise ValueError('WRAPPER_METADATA_LINK')
    stage = path.with_name('.' + path.name + '.new')
    if stage.exists() or stage.is_symlink():
        regular(stage, 32768)  # A previous crash may leave our uncommitted metadata.
        stage.unlink()
    fd = os.open(stage, os.O_WRONLY | os.O_CREAT | os.O_EXCL | os.O_NOFOLLOW, 0o600)
    try:
        with os.fdopen(fd, 'w') as stream:
            json.dump(value, stream, ensure_ascii=True)
            stream.flush()
            os.fsync(stream.fileno())
        stage.replace(path)
    finally:
        stage.unlink(missing_ok=True)

def read_json(path):
    return json.loads(regular(path, 32768).read_text())

def validate_driver(base, value, verify=True):
    identity = value.get('id', '')
    name = value.get('library', '')
    if not re.fullmatch('[a-f0-9]{64}', identity) or not re.fullmatch(r'[A-Za-z0-9_][A-Za-z0-9_.+-]{0,127}\.so', name):
        raise ValueError('WRAPPER_DRIVER_METADATA')
    root = base / 'drivers' / identity
    if (base / 'drivers').is_symlink() or root.is_symlink() or not root.is_dir():
        raise ValueError('WRAPPER_DRIVER_MISSING')
    hashes = value.get('files', {})
    if not hashes or len(hashes) > 16 or name not in hashes:
        raise ValueError('WRAPPER_DRIVER_METADATA')
    for filename, expected in hashes.items():
        if not re.fullmatch(r'[A-Za-z0-9_][A-Za-z0-9_.+-]{0,127}\.so', filename):
            raise ValueError('WRAPPER_DRIVER_METADATA')
        regular(root / filename, MAX_EXPANDED)
        if verify and digest(root / filename) != expected:
            raise ValueError('WRAPPER_DRIVER_CHANGED')
    return value, root

def driver(base):
    return validate_driver(base, read_json(base / 'current.json'))

def retain_legacy_driver(base):
    """Adopt v87's selected driver without requiring its original ZIP again."""
    try:
        value, root = driver(base)
    except (OSError, ValueError, KeyError, TypeError, AttributeError):
        return
    if not (root / 'library.json').exists() and not (root / 'library.json').is_symlink():
        write_json(root / 'library.json', value)

def catalog(base):
    with lock(base):
        retain_legacy_driver(base)
        parent = base / 'drivers'
        if parent.is_symlink():
            raise ValueError('WRAPPER_DIRECTORY')
        if not parent.exists():
            return []
        result = []
        for root in parent.iterdir():
            cancelled()
            if not re.fullmatch('[a-f0-9]{64}', root.name) or root.is_symlink() or not root.is_dir():
                continue
            try:
                value = read_json(root / 'library.json')
                if value.get('id') != root.name:
                    continue
                validate_driver(base, value, verify=False)
                name = value.get('name', 'Turnip Bionic')
                if not isinstance(name, str) or len(name) > 120:
                    continue
                minimum = value.get('minApi', 0)
                if not isinstance(minimum, int) or minimum < 0:
                    continue
                result.append(dict(id=value['id'], name=name, minApi=minimum))
            except (OSError, ValueError, KeyError, TypeError, AttributeError):
                continue
            if len(result) > 512:
                raise ValueError('WRAPPER_LIBRARY_LIMIT')
        return sorted(result, key=lambda v: (v['name'].casefold(), v['id']))

def select_driver(base, identity, api):
    if not re.fullmatch('[a-f0-9]{64}', identity):
        raise ValueError('WRAPPER_DRIVER_METADATA')
    with lock(base):
        cancelled()
        retain_legacy_driver(base)
        parent = base / 'drivers'
        root = parent / identity
        if parent.is_symlink() or root.is_symlink():
            raise ValueError('WRAPPER_DIRECTORY')
        value = read_json(root / 'library.json')
        if value.get('id') != identity:
            raise ValueError('WRAPPER_DRIVER_METADATA')
        minimum = value.get('minApi', 0)
        if not isinstance(minimum, int) or minimum < 0 or minimum > api:
            raise ValueError('WRAPPER_ANDROID_VERSION')
        validate_driver(base, value)  # Verify all library hashes before activation.
        cancelled()
        write_json(base / 'current.json', value)
        return value

def remove_driver(base, identity):
    """Delete one flat imported driver under the same lock held by desktop users."""
    if not re.fullmatch('[a-f0-9]{64}', identity):
        raise ValueError('WRAPPER_DRIVER_METADATA')
    if identity == DEFAULT_DRIVER_ID:
        raise ValueError('WRAPPER_DRIVER_PROTECTED')
    with lock(base):
        cancelled()
        parent = base / 'drivers'
        root = parent / identity
        if parent.is_symlink() or root.is_symlink():
            raise ValueError('WRAPPER_DIRECTORY')
        value = read_json(root / 'library.json')
        if value.get('id') != identity:
            raise ValueError('WRAPPER_DRIVER_METADATA')
        validate_driver(base, value, verify=False)
        # Used drivers may create auxiliary files and nested runtime caches.
        # Delete this explicitly chosen private directory without following links;
        # never widen the scope to the parent library or original phone ZIP.
        if not shutil.rmtree.avoids_symlink_attacks:
            raise ValueError('WRAPPER_REMOVE_UNSUPPORTED')
        current = base / 'current.json'
        if current.is_symlink():
            raise ValueError('WRAPPER_METADATA_LINK')
        selected = current.exists() and read_json(current).get('id') == identity
        cancelled()
        # Commit cannot be interrupted by the cancellation marker midway.
        # Rename first; unlink failure restores the complete driver directory.
        stage = Path(tempfile.mkdtemp(prefix='.delete-driver-', dir=base))
        stage.rmdir()
        root.rename(stage)
        try:
            if selected:
                current.unlink()
        except OSError:
            stage.rename(root)
            raise
        shutil.rmtree(stage)
        return selected

def import_driver(base, source, api):
    with lock(base):
        cancelled()
        retain_legacy_driver(base)
        source = regular(source, MAX_ZIP)
        identity = digest(source)
        parent = directory(base / 'drivers')
        with tempfile.TemporaryDirectory(prefix='.import-', dir=base) as temporary:
            stage = Path(temporary)
            with zipfile.ZipFile(source) as archive:
                members = entries(archive)
                meta = [m for m in members if m.filename in ('meta.json', 'driver.json')]
                if len(meta) != 1 or meta[0].file_size > 16384:
                    raise ValueError('WRAPPER_DRIVER_METADATA')
                metadata = json.loads(archive.read(meta[0]))
                minimum = metadata.get('minApi', 0)
                if not isinstance(minimum, int) or minimum < 0 or minimum > api:
                    raise ValueError('WRAPPER_ANDROID_VERSION')
                library = metadata.get('libraryName', '')
                if not re.fullmatch(r'[A-Za-z0-9_][A-Za-z0-9_.+-]{0,127}\.so', library):
                    raise ValueError('WRAPPER_DRIVER_METADATA')
                libraries = [m for m in members if re.fullmatch(r'[A-Za-z0-9_][A-Za-z0-9_.+-]{0,127}\.so', m.filename)]
                if not 1 <= len(libraries) <= 16 or library not in {m.filename for m in libraries}:
                    raise ValueError('WRAPPER_DRIVER_MISSING')
                hashes = {}
                for member in libraries:
                    cancelled()
                    data = archive.read(member)
                    elf(data)
                    (stage / member.filename).write_bytes(data)
                    (stage / member.filename).chmod(0o600)
                    hashes[member.filename] = hashlib.sha256(data).hexdigest()
                label = metadata.get('name', 'Turnip Bionic')
                if not isinstance(label, str):
                    raise ValueError('WRAPPER_DRIVER_METADATA')
                label = ''.join(c for c in label if c.isprintable() and c not in '\r\n')[:120]
            value = dict(id=identity, library=library, name=label, files=hashes, minApi=minimum)
            cancelled()
            dest = parent / identity
            if dest.is_symlink():
                raise ValueError('WRAPPER_DIRECTORY')
            if not dest.exists():
                # Stage contains libraries only, never a publisher-provided script/config.
                write_json(stage / 'library.json', value)
                stage.rename(dest)
            else:
                for name, expected in hashes.items():
                    if digest(dest / name) != expected:
                        raise ValueError('WRAPPER_DRIVER_CHANGED')
            cancelled()
            write_json(dest / 'library.json', value)
            write_json(base / 'current.json', value)
        # Retain imported drivers for offline switching. Equal ZIPs reuse their ID.
        return value

def runtime_ready(base):
    try:
        root = base / 'runtime'
        if root.is_symlink() or not root.is_dir():
            return False
        state = read_json(root / 'ready.json')
        required = RUNTIME | {'libvulkan.so', 'libvulkan.so.1', 'wrapper-icd.json'}
        return (state.get('version') == VERSION and set(state.get('files', {})) == required and
                all(digest(root / name) == value for name, value in state['files'].items()))
    except (OSError, ValueError, KeyError, TypeError):
        return False

def gpu_module():
    spec = importlib.util.spec_from_file_location('jobs_wrapper_download', Path(__file__).with_name('x11-gpu.py'))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    module.CANCEL_FILE = CANCEL
    return module

def install_runtime(base, fetch=None):
    with lock(base):
        cancelled()
        driver(base)  # Missing selection must not trigger an unnecessary download.
        if runtime_ready(base):
            return
        if (base / 'runtime').exists() or (base / 'runtime').is_symlink():
            raise ValueError('WRAPPER_RUNTIME_CHANGED')
        gpu = gpu_module()
        if fetch is None:
            fetch = gpu.download
        def progress(done, total):
            print(f'GPU_PROGRESS:{done}:{total}', flush=True)
        data = fetch(URL, SHA, SIZE, progress)
        if len(data) != SIZE or hashlib.sha256(data).hexdigest() != SHA:
            raise ValueError('WRAPPER_CHECKSUM')
        with tempfile.TemporaryDirectory(prefix='.runtime-', dir=base) as temporary:
            stage = Path(temporary)
            with zipfile.ZipFile(io.BytesIO(data)) as archive:
                members = {m.filename: m for m in entries(archive)}
                for name in RUNTIME:
                    payload = archive.read(members['lib/' + name])
                    elf(payload)
                    (stage / name).write_bytes(payload)
            cancelled()
            data = fetch(*LOADER, progress)
            if len(data) != LOADER[2] or hashlib.sha256(data).hexdigest() != LOADER[1]:
                raise ValueError('WRAPPER_CHECKSUM')
            with tarfile.open(fileobj=gpu.data_archive(data, 'deb')) as archive:
                matches = [m for m in archive if m.isfile() and re.fullmatch(r'(?:\./)?data/data/com\.termux/files/usr/lib/libvulkan\.so\.[0-9.]+', m.name)]
                if len(matches) != 1 or matches[0].size > 4 * 1024 * 1024:
                    raise ValueError('WRAPPER_LOADER')
                payload = archive.extractfile(matches[0]).read()
                elf(payload)
                # Regular copies keep all cache checks no-follow, including SONAME.
                (stage / 'libvulkan.so').write_bytes(payload)
                (stage / 'libvulkan.so.1').write_bytes(payload)
            icd = {'file_format_version': '1.0.0', 'ICD': {'library_path': str(base / 'runtime/libvulkan_wrapper.so'), 'api_version': '1.3.0'}}
            (stage / 'wrapper-icd.json').write_text(json.dumps(icd))
            hashes = {name: digest(stage / name) for name in RUNTIME | {'libvulkan.so', 'libvulkan.so.1', 'wrapper-icd.json'}}
            write_json(stage / 'ready.json', dict(version=VERSION, files=hashes))
            cancelled()
            stage.rename(base / 'runtime')

def main():
    global CANCEL
    if len(sys.argv) != 7:
        raise ValueError('WRAPPER_ARGUMENTS')
    operation, target, base, source, api, cancel = sys.argv[1:]
    if target != 'termux':
        raise ValueError('WRAPPER_TERMUX_ONLY')
    base = Path(base)
    CANCEL = Path(cancel) if cancel != '-' else None
    if operation == 'import':
        import_driver(base, Path(source), int(api))
        print('READY:wrapper-driver')
    elif operation == 'catalog':
        values = json.dumps(catalog(base), ensure_ascii=False)
        if len(values) > 196608:
            raise ValueError('WRAPPER_LIBRARY_LIMIT')
        print('WRAPPER_DRIVERS:' + values)
        print('READY:wrapper-catalog')
    elif operation == 'select':
        select_driver(base, source, int(api))
        print('READY:wrapper-selected')
    elif operation == 'remove':
        remove_driver(base, source)
        print('READY:wrapper-removed')
    elif operation == 'install':
        install_runtime(base)
        print('READY:wrapper')
    elif operation == 'check':
        driver(base)
        if not runtime_ready(base):
            raise ValueError('WRAPPER_RUNTIME_MISSING')
        print('READY:wrapper')
    elif operation == 'environment':
        value, root = driver(base)
        identity = value['id'][:16]
        if not runtime_ready(base):
            raise ValueError('WRAPPER_RUNTIME_MISSING')
        # Fixed-value exports only. Never source publisher metadata or an archive script.
        import shlex
        values = {'ADRENOTOOLS_DRIVER_PATH': str(root) + '/', 'ADRENOTOOLS_DRIVER_NAME': value['library'],
                  'ADRENOTOOLS_HOOKS_PATH': str(base / 'runtime'), 'VK_DRIVER_FILES': str(base / 'runtime/wrapper-icd.json'),
                  'VK_ICD_FILENAMES': str(base / 'runtime/wrapper-icd.json')}
        for key, value in values.items():
            print('export ' + key + '=' + shlex.quote(value))
        print('echo JOBS_X11_WRAPPER_DRIVER=' + identity)
    else:
        raise ValueError('WRAPPER_OPERATION')

if __name__ == '__main__':
    try:
        main()
    except Exception as error:
        category = str(error)
        if not re.fullmatch('WRAPPER_[A-Z0-9_]+', category):
            category = 'WRAPPER_FAILED'
        print('ERROR:' + category, flush=True)
        sys.exit(1)
