#!/usr/bin/env python3
"""Termux Job's backup beta. JSON protocol; never executes archive content."""
import argparse
import bz2
import copy
import gzip
import hashlib
import io
import json
import lzma
import os
from pathlib import Path, PurePosixPath
import platform
import re
import shlex
import shutil
import signal
import stat
import struct
import subprocess
import sys
import tarfile
import time
import uuid

MAX_JSON = 8 * 1024 * 1024
MAX_FILES = 500000
MAX_BYTES = 512 * 1024 ** 3
_last = 0


class BackupError(Exception):
    pass


def event(stage, **values):
    print(json.dumps(dict(stage=stage, **values), ensure_ascii=True), flush=True)


def progress(stage, count=0):
    global _last
    if time.monotonic() - _last > .7:
        _last = time.monotonic()
        event(stage, processed=count)


def regular(path):
    p = Path(path)
    if not stat.S_ISREG(p.lstat().st_mode):
        raise BackupError('REGULAR_FILE_REQUIRED')
    return p


def read_json(path):
    p = regular(path)
    if p.stat().st_size > MAX_JSON:
        raise BackupError('JSON_TOO_LARGE')
    return json.loads(p.read_text())


def digest(path):
    h = hashlib.sha256()
    with regular(path).open('rb') as stream:
        while True:
            b = stream.read(1024 * 1024)
            if not b:
                break
            h.update(b)
            progress('CHECKSUM', stream.tell())
    return h.hexdigest()


def clean(name):
    if '\x00' in name or '\\' in name or name.startswith('/') or len(name) > 4096:
        raise BackupError('UNSAFE_ARCHIVE_PATH')
    while name.startswith('./'):
        name = name[2:]
    parts = name.rstrip('/').split('/')
    if '..' in parts:
        raise BackupError('UNSAFE_ARCHIVE_PATH')
    parts = [p for p in parts if p and p != '.']
    return '/'.join(parts)


class SafeInfo(tarfile.TarInfo):
    def _proc_pax(self, tarfile):
        if self.size > 1024 * 1024:
            raise BackupError('OVERSIZED_TAR_HEADER')
        return super()._proc_pax(tarfile)

    def _proc_gnulong(self, tarfile):
        if self.size > 8192:
            raise BackupError('OVERSIZED_TAR_HEADER')
        return super()._proc_gnulong(tarfile)


def archive(path):
    return tarfile.open(regular(path), mode='r|*', tarinfo=SafeInfo)


def content(stream, member, limit):
    if member.size > limit:
        raise BackupError('METADATA_TOO_LARGE')
    f = stream.extractfile(member)
    return f.read(limit + 1) if f else b''


def arch_of(data):
    if data[:4] != b'\x7fELF' or len(data) < 20 or data[5] not in (1, 2):
        return ''
    code = int.from_bytes(data[18:20], 'little' if data[5] == 1 else 'big')
    return {183: 'aarch64', 40: 'arm', 62: 'x86_64', 3: 'i686'}.get(code, 'unknown')


def current_arch():
    return {'arm64': 'aarch64', 'armv8l': 'arm', 'armv7l': 'arm', 'amd64': 'x86_64'}.get(platform.machine().lower(), platform.machine().lower())


def libraries(value):
    if not isinstance(value, dict) or value.get('version') not in range(1, 9):
        raise BackupError('UNSUPPORTED_LIBRARY')
    if not isinstance(value.get('commands'), list) or not isinstance(value.get('sessions'), list):
        raise BackupError('UNSUPPORTED_LIBRARY')
    if sum(len(value.get(k, [])) for k in ('commands', 'sessions', 'notes')) > 20000:
        raise BackupError('LIBRARY_TOO_LARGE')
    return value


def scan_tar(path):
    """Full streaming read, bounded metadata, no extraction or foreign command execution."""
    names = {}
    links = set()
    payload = 0
    candidates = []
    found = []
    elf = {}
    workspace = None
    packages = {}
    with archive(path) as tar:
        for m in tar:
            name = clean(m.name)
            if not name:
                continue
            if len(names) >= MAX_FILES or m.size < 0 or m.size > MAX_BYTES:
                raise BackupError('ARCHIVE_LIMIT')
            kind = 'dir' if m.isdir() else 'file' if m.isfile() else 'symlink' if m.issym() else 'hardlink' if m.islnk() else 'other'
            if kind == 'other':
                # Socket/FIFO/device metadata is not portable; do not materialise it.
                continue
            if name in names and not (names[name] == kind == 'dir'):
                raise BackupError('DUPLICATE_ARCHIVE_PATH')
            names[name] = kind
            if m.issym():
                if len(m.linkname) > 4096 or '\x00' in m.linkname:
                    raise BackupError('UNSAFE_LINK')
                links.add(name)
            if m.islnk():
                clean(m.linkname)
            if m.isfile():
                payload += m.size
                if payload > MAX_BYTES:
                    raise BackupError('ARCHIVE_LIMIT')
                base = name.rsplit('/', 1)[-1]
                if name.endswith('var/lib/dpkg/status') and m.size <= 32*1024*1024:
                    packages[name]=status_packages(content(tar,m,32*1024*1024).decode('utf-8','replace'))
                elif base == 'workspace-v1.json':
                    workspace = libraries(json.loads(content(tar, m, MAX_JSON)))
                elif base in ('.bashrc', '.zshrc', '.bash_aliases') and m.size <= 256 * 1024:
                    text = content(tar, m, 256 * 1024).decode('utf-8', 'replace')
                    for line in text.splitlines():
                        if not line.lstrip().startswith('alias '):
                            continue
                        try:
                            tokens = shlex.split(line, comments=True)
                        except ValueError:
                            continue
                        if len(tokens) == 2 and tokens[0] == 'alias' and '=' in tokens[1]:
                            label, command = tokens[1].split('=', 1)
                            if re.fullmatch(r'[A-Za-z0-9_.-]{1,80}', label) and command and len(candidates) < 500:
                                candidates.append(dict(kind='alias', name=label, command=command, source=name))
                elif base in ('bash', 'env', 'python3', 'sh', 'ls') or name.endswith('.sh'):
                    f = tar.extractfile(m)
                    head = f.read(min(m.size, 128)) if f else b''
                    arch = arch_of(head)
                    if arch:
                        elf[name] = arch
                    if name.endswith('.sh') and len(candidates) < 500:
                        candidates.append(dict(kind='script', name=base, source=name))
            progress('SCAN', payload)
    # No archive member may write through a symlink, regardless of member ordering.
    for name in names:
        parts = name.split('/')
        if any('/'.join(parts[:i]) in links for i in range(1, len(parts))):
            raise BackupError('WRITE_THROUGH_SYMLINK')
    # Validate compressor trailer as tar readers can stop at the TAR end marker.
    with regular(path).open('rb') as f:
        magic = f.read(6)
    opener = gzip.open if magic.startswith(b'\x1f\x8b') else bz2.open if magic.startswith(b'BZh') else lzma.open if magic.startswith(b'\xfd7zXZ') else None
    if opener:
        total = 0
        with opener(path, 'rb') as f:
            while True:
                block = f.read(1024 * 1024)
                if not block:
                    break
                total += len(block)
                if total > MAX_BYTES + 1024 ** 3:
                    raise BackupError('ARCHIVE_LIMIT')
                progress('VERIFY_COMPRESSION', total)
    all_names = list(names)
    roots = []
    # Exact layout recognition, never infer compatibility from the file name.
    for n in all_names:
        parts = n.split('/')
        # Ordinary full PREFIX backups can contain proot roots below usr/var/lib/.
        for j, part in enumerate(parts):
            if part == 'installed-rootfs' and j+2 < len(parts) and j>0 and parts[max(0,j-3):j]==['var','lib','proot-distro']:
                roots.append(('ubuntu','/'.join(parts[:j+2]),parts[j+1],'proot-legacy'))
            if part == 'containers' and j+3 < len(parts) and parts[j+2]=='rootfs' and parts[max(0,j-3):j]==['var','lib','proot-distro']:
                roots.append(('ubuntu','/'.join(parts[:j+3]),parts[j+1],'proot-modern'))
        if len(parts) >= 3 and parts[0] == 'installed-rootfs':
            roots.append(('ubuntu', '/'.join(parts[:2]), parts[1], 'proot-legacy'))
        elif len(parts) >= 3 and parts[1] == 'rootfs':
            roots.append(('ubuntu', '/'.join(parts[:2]), parts[0], 'proot-modern'))
        elif len(parts) >= 6 and parts[:2] in (['data', 'data'], ['data', 'user']) and 'files' in parts:
            idx = parts.index('files')
            if len(parts) > idx + 2 and parts[idx + 1] in ('home', 'usr'):
                roots.append(('home' if parts[idx + 1] == 'home' else 'prefix', '/'.join(parts[:idx + 2]), '', 'termux-path'))
    for root, kind in [('home', 'home'), ('usr', 'prefix')]:
        if root in names or any(n.startswith(root + '/') for n in all_names):
            roots.append((kind, root, '', 'termux-relative'))
    if not roots and ('etc/os-release' in names and any(n.startswith(('usr/', 'bin/')) for n in all_names)):
        roots.append(('ubuntu', '', 'ubuntu', 'rootfs-bare'))
    if not roots and any(n in names for n in ('.bashrc', '.zshrc', '.bash_history', '.profile', '.ssh/config')):
        roots.append(('home', '', '', 'home-bare'))
    if not roots and workspace:
        roots.append(('library', '', '', 'workspace-v8'))
    if not roots and any(n.startswith('workspace-sessions/') for n in all_names):
        roots.append(('app-data', '', '', 'jobs-app-data'))
    for kind, root, alias, fmt in sorted(set(roots)):
        selected = [n for n in all_names if not root or n == root or n.startswith(root + '/')]
        architecture = sorted({a for n, a in elf.items() if n in selected})
        # Only binaries in bin directories establish the rootfs architecture.
        primary = sorted({a for n, a in elf.items() if n in selected and any(n == (root + '/' if root else '') + q for q in ('bin/bash', 'bin/sh', 'usr/bin/bash', 'usr/bin/env', 'usr/bin/sh'))})
        arch = primary[0] if len(primary) == 1 else architecture[0] if len(architecture) == 1 else ''
        found.append(dict(kind=kind, root=root, alias=alias, format=fmt, arch=arch, files=len(selected)))
    termux_packages={}
    for c in found:
        if c['kind']=='prefix':termux_packages.update(packages.get((c['root']+'/' if c['root'] else '')+'var/lib/dpkg/status',{}))
    return dict(components=found, unpacked=payload, entries=len(names), candidates=candidates, workspace=workspace,
                externalLinks=bool(links), packages=termux_packages)


def proot_modern(prefix):
    executable = prefix / 'bin/proot-distro'
    if not executable.is_file():
        return False
    try:
        return b'proot_distro' in executable.read_bytes()[:4096]
    except OSError:
        return False


def environments(prefix):
    root = prefix / 'var/lib/proot-distro'
    result = []
    for folder, modern in [(root / 'containers', True), (root / 'installed-rootfs', False)]:
        if not folder.is_dir():
            continue
        for path in sorted(folder.iterdir()):
            fs = path / 'rootfs' if modern else path
            if path.is_symlink() or not re.fullmatch(r'[a-zA-Z0-9][a-zA-Z0-9_.-]{0,63}', path.name) or not fs.is_dir():
                continue
            if not any(e['name'] == path.name for e in result):
                result.append(dict(name=path.name, root=str(fs)))
    return result


def tree_bytes(path, excluded=()):
    total = 0
    for root, dirs, files in os.walk(path, followlinks=False):
        dirs[:] = [d for d in dirs if not Path(root, d).is_symlink() and not any(Path(root, d) == p for p in excluded)]
        for f in files:
            p = Path(root, f)
            if not p.is_symlink() and p.is_file():
                total += p.stat().st_size
                progress('ESTIMATE',total)
    return total


def environment_info(request):
    prefix, home = Path(request['prefix']), Path(request['home'])
    envs = environments(prefix)
    return dict(arch=current_arch(), environments=envs, modern=proot_modern(prefix),
                homeBytes=tree_bytes(home), prefixBytes=tree_bytes(prefix, (prefix / 'var/lib/proot-distro',)),
                ubuntuBytes={e['name']: tree_bytes(Path(e['root'])) for e in envs})


def protected_write(path, data):
    with open(path, 'xb') as f:
        try:
            os.chmod(path, 0o600)
        except PermissionError:
            pass  # Shared Android storage does not enforce POSIX permissions.
        f.write(data)
        f.flush()
        os.fsync(f.fileno())


def run_tool(args):
    # No shell, no inherited stdin, and output never copied into reports (may contain secrets).
    p = subprocess.Popen(args, stdin=subprocess.DEVNULL, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    try:
        while p.poll() is None:
            progress('PROOT_DISTRO')
            time.sleep(.5)
        if p.returncode:
            raise BackupError('PROOT_DISTRO_FAILED')
    finally:
        if p.poll() is None:
            p.terminate()
            try:
                p.wait(timeout=5)
            except subprocess.TimeoutExpired:
                p.kill()
                p.wait()


def create_tar(path, source, root, exclude=()):
    excluded = {str(p) for p in exclude}
    skipped = 0
    with tarfile.open(path, 'w:gz', dereference=False) as tar:
        def include(info):
            nonlocal skipped
            relative = info.name[len(root):].lstrip('/')
            if any(relative == p or relative.startswith(p + '/') for p in excluded):
                return None
            if not (info.isfile() or info.isdir() or info.issym() or info.islnk()):
                skipped += 1
                return None
            progress('ARCHIVE')
            return info
        tar.add(source, arcname=root or '.', filter=include)
    return skipped


def create(request):
    prefix, home = Path(request['prefix']), Path(request['home'])
    parent = Path(request['destination']).resolve(strict=True)
    # Prevent archiving our output recursively; shared storage is intentionally supported.
    if parent == home or home in parent.parents or parent == prefix or prefix in parent.parents:
        raise BackupError('OUTPUT_INSIDE_SOURCE')
    info = environment_info(request)
    wanted = request['components']
    estimate = (info['homeBytes'] if 'home' in wanted else 0) + (info['prefixBytes'] if 'prefix' in wanted else 0)
    selected_envs = [e for e in info['environments'] if e['name'] in request.get('environments', [])]
    estimate += sum(info['ubuntuBytes'][e['name']] for e in selected_envs)
    if shutil.disk_usage(parent).free < estimate + 64 * 1024 ** 2:
        raise BackupError('INSUFFICIENT_SPACE')
    out = parent / ('Termux-Jobs-' + time.strftime('%Y%m%d-%H%M%S') + '-' + uuid.uuid4().hex[:8])
    out.mkdir(mode=0o700)
    protected_write(out / 'INCOMPLETE', b'Not verified. Do not use as a complete backup.\n')
    meta = dict(format='termux-jobs-backup', version=1, created=time.time(), live=True,
                consistency='Best effort live copy; user must finish writers before backup. No process memory.',
                source=dict(arch=info['arch'], packageId=request.get('packageId'), prefix=str(prefix), home=str(home), appVersion=request.get('appVersion')), components=[])
    def add(name, kind):
        file = out / name
        details = scan_tar(file) if name.endswith('.tar.gz') else None
        meta['components'].append(dict(file=name, kind=kind, sha256=digest(file), bytes=file.stat().st_size,
                                       unpacked=details['unpacked'] if details else file.stat().st_size))
    if 'library' in wanted:
        protected_write(out / 'workspace-v1.json', json.dumps(request['workspace'], ensure_ascii=False).encode())
        add('workspace-v1.json', 'library')
        protected_write(out / 'settings.json', json.dumps(request.get('settings', {})).encode())
        add('settings.json', 'settings')
        # Per-tab shell history survives separately; launch helper files are retained but not executed on import.
        sessions = Path(request['files']) / 'workspace-sessions'
        if sessions.is_dir():
            create_tar(out / 'app-data.tar.gz', sessions, 'workspace-sessions')
            add('app-data.tar.gz', 'app-data')
    if 'home' in wanted:
        create_tar(out / 'home.tar.gz', home, 'home')
        add('home.tar.gz', 'home')
    if 'prefix' in wanted:
        create_tar(out / 'prefix.tar.gz', prefix, 'usr', ('var/lib/proot-distro', 'tmp'))
        add('prefix.tar.gz', 'prefix')
    dpkg = prefix / 'bin/dpkg-query'
    if dpkg.is_file():
        with open(out / 'packages.txt', 'xb') as f:
            p = subprocess.run([str(dpkg), '-W', '-f=${binary:Package}\t${Version}\n'], stdin=subprocess.DEVNULL, stdout=f, stderr=subprocess.DEVNULL, timeout=90)
        if p.returncode:
            raise BackupError('PACKAGE_LIST_FAILED')
        add('packages.txt', 'packages')
    for env in selected_envs:
        file = out / ('ubuntu-' + env['name'] + '.tar.gz')
        run_tool([str(prefix / 'bin/proot-distro'), 'backup', '--output', str(file), env['name']])
        add(file.name, 'ubuntu')
    # The helper is a known app asset; never copy a script from an imported archive for execution.
    shutil.copyfile(__file__, out / 'backup-tool.py')
    protected_write(out / 'restore.sh', b'''#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
cd -- "$(dirname -- "$0")"
command -v python3 >/dev/null || { echo 'Install Python first: pkg install python'; exit 1; }
exec python3 -I -S ./backup-tool.py --wizard ./manifest.json
''')
    protected_write(out / 'README-RESTORE.txt', b'''Termux Job's backup beta. Contains private credentials; keep offline and protected.
Install a trusted Termux (same architecture), run termux-setup-storage, then pkg install python proot-distro.
Open Backup and restore in Termux Job's, select manifest.json, inspect compatibility and choose components.
In ordinary Termux run: bash /sdcard/Download/<backup>/restore.sh
The helper revalidates archives and stages HOME/PREFIX in a NEW folder only. It never overwrites the running PREFIX.
Ubuntu can be restored under a fresh alias on supported modern proot-distro; no extra project binds are added.
HOME/PREFIX are not automatically activated. Review restored scripts/config before copying selected files.
Install packages individually from packages.txt; package versions may be unavailable. Do not blindly execute package names from a foreign list.
Android UID is not restored; extracted files belong to the current UID. Permissions/mtime/links are preserved where possible.
Android Keystore keys, permissions and live processes cannot be restored. Re-enter remembered SSH passwords.
Live copying is not an atomic snapshot. Finish jobs/writers before backing up. External symlink targets and Android shared storage are NOT archived.
''')
    checks = ''.join(c['sha256'] + '  ' + c['file'] + '\n' for c in meta['components'])
    for name in ('backup-tool.py', 'restore.sh', 'README-RESTORE.txt'):
        checks += digest(out / name) + '  ' + name + '\n'
    protected_write(out / 'SHA256SUMS', checks.encode())
    protected_write(out / 'manifest.json', json.dumps(meta, indent=2).encode())
    (out / 'INCOMPLETE').unlink()
    return dict(path=str(out), bytes=sum(p.stat().st_size for p in out.iterdir() if p.is_file()), components=len(meta['components']))


def inspect(request):
    path = regular(request['path'])
    fingerprints = {str(path): digest(path)}
    source = {}
    inputs = []
    settings = None
    workspace = None
    if path.name.endswith('.json'):
        value = read_json(path)
        if value.get('format') == 'termux-jobs-backup':
            if value.get('version') != 1 or (path.parent / 'INCOMPLETE').exists():
                raise BackupError('INCOMPLETE_OR_UNSUPPORTED_BACKUP')
            source = value.get('source', {})
            if len(value.get('components', [])) > 100:
                raise BackupError('TOO_MANY_COMPONENTS')
            for c in value['components']:
                name = c['file']
                if not re.fullmatch(r'[A-Za-z0-9_.-]{1,160}', name) or name in ('.', '..'):
                    raise BackupError('UNSAFE_COMPONENT_PATH')
                f = regular(path.parent / name)
                sha = digest(f)
                if sha != c['sha256'] or f.stat().st_size != c['bytes']:
                    raise BackupError('CHECKSUM_MISMATCH')
                fingerprints[str(f)] = sha
                inputs.append((f, c['kind']))
        else:
            workspace = libraries(value)
    else:
        inputs.append((path, 'auto'))
    components = []
    candidates = []
    package_versions = {}
    for f, declared in inputs:
        if declared == 'library':
            workspace = libraries(read_json(f))
            continue
        if declared == 'settings':
            settings = read_json(f)
            continue
        if declared == 'packages':
            if f.stat().st_size > 8*1024*1024: raise BackupError('PACKAGE_LIST_TOO_LARGE')
            package_versions.update(parse_packages(f.read_text(errors='replace')))
            components.append(dict(kind='packages', file=str(f), root='', format='package-list', status='portable', unpacked=f.stat().st_size))
            continue
        scan = scan_tar(f)
        if any(c['kind']=='prefix' for c in scan['components']): package_versions.update(scan['packages'])
        if scan['workspace']:
            workspace = scan['workspace']
        for c in scan['components']:
            # PREFIX is always staged; never applied to the running runtime.
            c.update(file=str(f), unpacked=scan['unpacked'], status='portable' if c['kind'] in ('home', 'library', 'app-data') else 'review')
            if c['kind'] == 'ubuntu':
                c['status'] = 'compatible' if c['arch'] == current_arch() and proot_modern(Path(request['prefix'])) else 'blocked'
                c['reason'] = '' if c['status'] == 'compatible' else 'ARCH_OR_PROOT_UNCONFIRMED'
            if c['kind'] == 'prefix':
                c['reason'] = 'STAGING_ONLY_NOT_LIVE_PREFIX'
            components.append(c)
        for c in scan['candidates']:
            # Never import system package scripts as user commands.
            matching = next((x for x in scan['components'] if x['kind'] in ('home', 'ubuntu') and (not x['root'] or c['source'].startswith(x['root'] + '/'))), None)
            if matching:
                c.update(file=str(f), root=matching['root'], component=len(components) - len(scan['components']) + scan['components'].index(matching))
                candidates.append(c)
    if not components and workspace is None:
        raise BackupError('UNRECOGNIZED_ARCHIVE')
    if len(components) > 100:
        raise BackupError('TOO_MANY_COMPONENTS')
    return dict(path=str(path), fingerprints=fingerprints, source=source, components=components,
                workspace=workspace, settings=settings, candidates=candidates[:500],
                free=shutil.disk_usage(request['home']).free,
                packages=package_versions, warning='Foreign archives are untrusted. Validation does not prove authenticity or database consistency. Nothing has been executed.')


def directory_fd(root, relative):
    fd = os.open(root, os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW)
    try:
        for part in relative:
            try:
                os.mkdir(part, mode=0o700, dir_fd=fd)
            except FileExistsError:
                pass
            new = os.open(part, os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW, dir_fd=fd)
            os.close(fd)
            fd = new
        return fd
    except BaseException:
        os.close(fd)
        raise


def extract_new(path, root, destination):
    """Manual no-follow extraction into an empty owned directory; no chown or execute."""
    destination.mkdir(mode=0o700, parents=False)
    directories = []
    hard = []
    written = 0
    with archive(path) as tar:
        for m in tar:
            name = clean(m.name)
            if root and not (name == root or name.startswith(root + '/')):
                continue
            relative = name[len(root):].lstrip('/') if root else name
            if not relative:
                continue
            parts = relative.split('/')
            if not (m.isfile() or m.isdir() or m.issym() or m.islnk()):
                continue
            fd = directory_fd(destination, parts[:-1])
            try:
                if m.isdir():
                    try:
                        os.mkdir(parts[-1], 0o700, dir_fd=fd)
                    except FileExistsError:
                        child = os.open(parts[-1], os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW, dir_fd=fd)
                        os.close(child)
                    directories.append((parts, m.mode & 0o777, m.mtime))
                elif m.issym():
                    os.symlink(m.linkname, parts[-1], dir_fd=fd)
                elif m.islnk():
                    target = clean(m.linkname)
                    if root:
                        if not target.startswith(root + '/'):
                            raise BackupError('CROSS_COMPONENT_HARDLINK')
                        target = target[len(root) + 1:]
                    hard.append((parts, target.split('/'), m.mode & 0o777, m.mtime))
                else:
                    src = tar.extractfile(m)
                    out = os.open(parts[-1], os.O_WRONLY | os.O_CREAT | os.O_EXCL | os.O_NOFOLLOW, m.mode & 0o777, dir_fd=fd)
                    with os.fdopen(out, 'wb') as f:
                        shutil.copyfileobj(src, f, 1024 * 1024)
                        f.flush()
                        os.fchmod(f.fileno(), m.mode & 0o777)
                    os.utime(parts[-1], (m.mtime, m.mtime), dir_fd=fd, follow_symlinks=False)
                    written += m.size
                    progress('RESTORE_FILES', written)
            finally:
                os.close(fd)
    for parts, target, mode, mtime in hard:
        srcfd = directory_fd(destination, target[:-1])
        dstfd = directory_fd(destination, parts[:-1])
        try:
            src = os.open(target[-1], os.O_RDONLY | os.O_NOFOLLOW, dir_fd=srcfd)
            if not stat.S_ISREG(os.fstat(src).st_mode):
                os.close(src)
                raise BackupError('INVALID_HARDLINK')
            out = os.open(parts[-1], os.O_WRONLY | os.O_CREAT | os.O_EXCL | os.O_NOFOLLOW, mode, dir_fd=dstfd)
            with os.fdopen(src, 'rb') as a, os.fdopen(out, 'wb') as b:
                shutil.copyfileobj(a, b, 1024 * 1024)
            os.utime(parts[-1], (mtime, mtime), dir_fd=dstfd, follow_symlinks=False)
        finally:
            os.close(srcfd)
            os.close(dstfd)
    for parts, mode, mtime in reversed(directories):
        fd = directory_fd(destination, parts)
        try:
            os.fchmod(fd, mode)
            os.utime(fd, (mtime, mtime))
        finally:
            os.close(fd)
    return written


def restore_ubuntu(path, component, prefix, work):
    if component['status'] != 'compatible':
        raise BackupError('ARCH_OR_PROOT_UNCONFIRMED')
    alias = 'jobs-' + uuid.uuid4().hex[:12]
    # Repack only rootfs under a fresh name, omit foreign host configuration scripts.
    target = prefix / 'var/lib/proot-distro/containers' / alias
    if target.exists() or target.is_symlink() or (prefix / 'var/lib/proot-distro/installed-rootfs' / alias).exists():
        raise BackupError('TARGET_EXISTS')
    rewritten = work / (alias + '.tar')
    root = component['root']
    with archive(path) as src, tarfile.open(rewritten, 'w') as dst:
        for m in src:
            name = clean(m.name)
            if root and not (name == root or name.startswith(root + '/')):
                continue
            rel = name[len(root):].lstrip('/') if root else name
            item = copy.copy(m)
            item.name = alias + '/rootfs' + ('/' + rel if rel else '')
            if item.islnk():
                link = clean(item.linkname)
                if root and not link.startswith(root + '/'):
                    raise BackupError('CROSS_COMPONENT_HARDLINK')
                relink = link[len(root) + 1:] if root else link
                item.linkname = alias + '/rootfs/' + relink
            if not (item.isfile() or item.isdir() or item.issym() or item.islnk()):
                continue
            dst.addfile(item, src.extractfile(m) if item.isfile() else None)
            progress('PREPARE_UBUNTU')
    scan_tar(rewritten)
    run_tool([str(prefix / 'bin/proot-distro'), 'restore', str(rewritten)])
    rewritten.unlink()
    return alias


def restore(request):
    report = inspect(request)
    if report['fingerprints'] != request['fingerprints']:
        raise BackupError('BACKUP_CHANGED_RECHECK')
    choices = request['selected']
    if not choices:
        raise BackupError('NO_COMPONENTS_SELECTED')
    if len(set(choices))!=len(choices) or any(not isinstance(i,int) or i<0 or i>=len(report['components']) for i in choices):
        raise BackupError('INVALID_SELECTION')
    if any(report['components'][i]['status']=='blocked' for i in choices):
        raise BackupError('ARCH_OR_PROOT_UNCONFIRMED')
    parent = Path(request['destination']).resolve(strict=True)
    home, prefix = Path(request['home']).resolve(), Path(request['prefix']).resolve()
    # Restored files always get a new child folder. Never merge into active HOME/PREFIX/rootfs.
    if parent == prefix or prefix in parent.parents or (parent != home and home not in parent.parents):
        raise BackupError('DESTINATION_MUST_BE_INSIDE_HOME')
    selected = [report['components'][i] for i in choices]
    required = sum(c['unpacked'] * (3 if c['kind'] == 'ubuntu' else 2) for c in selected)
    if shutil.disk_usage(parent).free < required + 64 * 1024 ** 2:
        raise BackupError('INSUFFICIENT_SPACE')
    destination = parent / ('restored-' + time.strftime('%Y%m%d-%H%M%S') + '-' + uuid.uuid4().hex[:8])
    destination.mkdir(mode=0o700)
    protected_write(destination / 'INCOMPLETE', b'Interrupted restore must be inspected before use.\n')
    workspace = report['workspace']
    # Copy only selected archives to a private, immutable-to-shared-storage job directory.
    work = Path(request['work'])
    copies = {}
    for c in selected:
        source = c['file']
        if source not in copies:
            target = work / (uuid.uuid4().hex + '.archive')
            with regular(source).open('rb') as a, target.open('xb') as b:
                shutil.copyfileobj(a, b, 1024 * 1024)
            if digest(target) != report['fingerprints'][source]:
                raise BackupError('BACKUP_CHANGED_RECHECK')
            copies[source] = target
    environments_created = []
    paths = {}
    for i, c in zip(choices, selected):
        path = copies[c['file']]
        if c['kind'] == 'ubuntu':
            alias = restore_ubuntu(path, c, prefix, work)
            environments_created.append(alias)
            paths[str(i)] = 'proot-distro login --isolated ' + alias
        elif c['kind'] == 'packages':
            target = destination / ('packages-' + str(i) + '.txt')
            shutil.copyfile(path, target)
            paths[str(i)] = str(target)
        else:
            target = destination / (c['kind'] + '-' + str(i))
            extract_new(path, c['root'], target)
            paths[str(i)] = str(target)
    (destination / 'INCOMPLETE').unlink()
    protected_write(destination / 'RESTORE-RESULT.json', json.dumps(dict(paths=paths, environments=environments_created)).encode())
    return dict(path=str(destination), paths=paths, environments=environments_created)


def valid_package(name, version):
    return bool(re.fullmatch(r'[a-z0-9][a-z0-9+.-]*(?::[a-z0-9-]+)?', name) and re.fullmatch(r'[A-Za-z0-9.+:~_-]{1,160}', version))


def parse_packages(text):
    result = {}
    for line in text.splitlines():
        parts=line.split()
        if len(parts)==2 and valid_package(parts[0],parts[1]): result[parts[0]]=parts[1]
        elif len(parts)==1 and re.fullmatch(r'[a-z0-9][a-z0-9+.-]*(?::[a-z0-9-]+)?',parts[0]): result[parts[0]]=''
        if len(result)>100000: raise BackupError('PACKAGE_LIST_TOO_LARGE')
    return result


def status_packages(text):
    result={}
    for block in text.split('\n\n'):
        fields={}
        for line in block.splitlines():
            if ': ' in line and not line.startswith(' '):
                k,v=line.split(': ',1);fields[k]=v
        if fields.get('Status')=='install ok installed' and valid_package(fields.get('Package',''),fields.get('Version','')):
            result[fields['Package']]=fields['Version']
    return result


def compare_packages(request):
    prefix=Path(request['prefix'])
    tool=prefix/'bin/dpkg-query'
    p=subprocess.run([str(tool),'-W','-f=${binary:Package}\t${Version}\n'],stdin=subprocess.DEVNULL,stdout=subprocess.PIPE,stderr=subprocess.DEVNULL,timeout=60)
    if p.returncode: raise BackupError('PACKAGE_LIST_FAILED')
    current=parse_packages(p.stdout.decode('utf-8','replace'))
    source=request['packages']
    if not isinstance(source,dict) or len(source)>100000: raise BackupError('PACKAGE_LIST_TOO_LARGE')
    return dict(packages=[dict(name=k,backup=v,current=current.get(k,''),selectable=valid_package(k,v)) for k,v in sorted(source.items()) if current.get(k)!=v])


def apt_plan(request):
    packages=request['packages']
    if not packages or len(packages)>200 or any(not valid_package(k,v) for k,v in packages.items()): raise BackupError('INVALID_PACKAGES')
    args=[str(Path(request['prefix'])/'bin/apt-get'),'-s','--no-remove','install','--']+[k+'='+v for k,v in sorted(packages.items())]
    p=subprocess.run(args,stdin=subprocess.DEVNULL,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=90)
    if len(p.stdout)>256*1024: raise BackupError('APT_PLAN_TOO_LARGE')
    text=p.stdout.decode('utf-8','replace')
    # No command is constructed from free-form APT output.
    return dict(ok=p.returncode==0,plan=text,command='apt-get --no-remove install -- '+' '.join(shlex.quote(k+'='+v) for k,v in sorted(packages.items())))


def wizard(manifest):
    prefix = os.environ.get('PREFIX', '/data/data/com.termux/files/usr')
    home = str(Path.home())
    req = dict(path=str(Path(manifest).resolve()), prefix=prefix, home=home)
    result = inspect(req)
    print('Verified components (blocked components cannot be restored):')
    for i, c in enumerate(result['components']):
        print(i, c['kind'], c['status'], c.get('arch', ''))
    print('All files go into a NEW folder; PREFIX is never replaced. Ubuntu uses a fresh alias.')
    selected = [int(v) for v in input('Component numbers, comma separated (blank cancels): ').split(',') if v.strip()]
    if not selected:
        return
    if any(i < 0 or i >= len(result['components']) or result['components'][i]['status'] == 'blocked' for i in selected):
        raise BackupError('INVALID_SELECTION')
    import tempfile
    with tempfile.TemporaryDirectory(prefix='jobs-restore-', dir=home) as work:
        print(json.dumps(restore(dict(req, fingerprints=result['fingerprints'], selected=selected, destination=home, work=work)), indent=2))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--request')
    parser.add_argument('--wizard')
    args = parser.parse_args()
    if args.wizard:
        wizard(args.wizard)
        return
    request = read_json(args.request)
    operation = request['operation']
    value = {'inventory': environment_info, 'create': create, 'inspect': inspect, 'restore': restore, 'packages': compare_packages, 'apt-plan': apt_plan}[operation](request)
    event('DONE', result=value)


if __name__ == '__main__':
    def cancelled(_signum, _frame):
        raise BackupError('CANCELLED')
    signal.signal(signal.SIGTERM, cancelled)
    try:
        main()
    except Exception as error:
        # Paths, shell output, credentials and arbitrary exception strings never enter UI logs.
        event('ERROR', code=str(error) if isinstance(error, BackupError) else type(error).__name__)
        sys.exit(1)
