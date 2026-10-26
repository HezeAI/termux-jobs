#!/usr/bin/env python3
"""Termux Job's backup. JSON protocol; never executes archive content."""
import argparse
import bz2
import copy
from contextlib import contextmanager
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
_last_stage = ""
_current = ""


class BackupError(Exception):
    pass


def event(stage, **values):
    print(json.dumps(dict(stage=stage, **values), ensure_ascii=True), flush=True)


def progress(stage, count=0, total=0, current=None, unit='bytes', force=False):
    global _last, _last_stage, _current
    changed=stage!=_last_stage
    if changed:_current=''
    if current is not None:
        # Only a bounded filename/path label, never file contents or tool output.
        _current=''.join(c if c.isprintable() else ' ' for c in str(current))[-512:]
    now=time.monotonic()
    if force or changed or now-_last>.5:
        _last=now;_last_stage=stage
        event(stage,processed=max(0,count),total=max(0,total),current=_current,unit=unit)


class ProgressReader:
    def __init__(self,stream,callback):self.stream=stream;self.callback=callback
    def read(self,size=-1):
        data=self.stream.read(size);self.callback(len(data));return data


def track_archive(tar,total=0):
    """Observe the existing TAR writer's input stream; do not change archive layout."""
    addfile=tar.addfile
    processed=0
    def tracked(info,fileobj=None):
        nonlocal processed
        progress('ARCHIVE',processed,total,current=info.name)
        def read(count):
            nonlocal processed
            processed+=count;progress('ARCHIVE',processed,total,current=info.name)
        addfile(info,ProgressReader(fileobj,read) if fileobj is not None else None)
    tar.addfile=tracked


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
            progress('CHECKSUM',stream.tell(),path.stat().st_size,current=path.name)
    return h.hexdigest()


def clean(name):
    # systemd stores literal \\xHH in unit names. Accept that spelling, never decode it.
    other_backslashes = re.sub(r'\\x[0-9A-Fa-f]{2}', '', name)
    if '\x00' in name or '\\' in other_backslashes or name.startswith('/') or len(name) > 4096:
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
    if any(not isinstance(value.get(k, []), list) for k in ('commands', 'sessions', 'notes', 'aliases')):
        raise BackupError('UNSUPPORTED_LIBRARY')
    if sum(len(value.get(k, [])) for k in ('commands', 'sessions', 'notes', 'aliases')) > 20000:
        raise BackupError('LIBRARY_TOO_LARGE')
    return value


def export_aliases(request):
    value = libraries(request['workspace'])
    if not value.get('aliases') or any(value.get(k) for k in ('commands', 'sessions', 'notes', 'recentCommands')):
        raise BackupError('INVALID_ALIAS_EXPORT')
    # Export only the explicit alias fields, never unrelated settings or credentials.
    fields = ('id', 'name', 'command', 'environment', 'category', 'favorite')
    data = dict(version=8, commands=[], sessions=[], notes=[], recentCommands=[],
                aliases=[{k: a[k] for k in fields} for a in value['aliases']])
    shell = request.get('aliasFormat', 'json') == 'shell'
    if shell:
        if len({a['environment'] for a in data['aliases']}) != 1:
            raise BackupError('ALIAS_SINGLE_ENVIRONMENT_REQUIRED')
        if any(not re.fullmatch(r'[A-Za-z_][A-Za-z0-9_-]{0,63}', a['name']) or not isinstance(a['command'], str) or not a['command'].strip() or '\0' in a['command'] or '\r' in a['command'] for a in data['aliases']):
            raise BackupError('INVALID_ALIAS_EXPORT')
        encoded = ('# Termux Jobs aliases — review before sourcing in Bash/Zsh.\n' + ''.join('alias ' + a['name'] + '=' + shlex.quote(a['command']) + '\n' for a in data['aliases'])).encode()
    else:
        encoded = json.dumps(data, ensure_ascii=False).encode()
    if len(encoded) > MAX_JSON:
        raise BackupError('LIBRARY_TOO_LARGE')
    parent = Path(request['destination']).resolve(strict=True)
    if not parent.is_dir():
        raise BackupError('INVALID_PATH')
    path = parent / ('Termux-Jobs-Alias-' + time.strftime('%Y%m%d-%H%M%S') + '-' + uuid.uuid4().hex[:8] + ('.sh' if shell else '.json'))
    protected_write(path, encoded)
    return dict(path=str(path), sha256=digest(path), count=len(data['aliases']))


def alias_literal_words(line):
    words, word = [], []
    quote = None
    active = False
    index = 0
    while index < len(line):
        char = line[index]
        index += 1
        if quote == "'":
            if char == "'":
                quote = None
            else:
                word.append(char)
        elif char == '\\':
            if index == len(line):
                return None
            next_char = line[index]
            index += 1
            if quote == '"' and next_char not in '$`\\"\n':
                word.append('\\')
            if next_char != '\n':
                word.append(next_char)
            active = True
        elif quote == '"':
            if char == '"':
                quote = None
            elif char in '$`':
                return None
            else:
                word.append(char)
        elif char in "'\"":
            quote = char
            active = True
        elif char.isspace():
            if active:
                words.append(''.join(word))
                word, active = [], False
        elif char == ';' and not line[index:].strip():
            break
        elif char in '$`;&|<>(){}':
            return None
        else:
            word.append(char)
            active = True
    if quote:
        return None
    if active:
        words.append(''.join(word))
    return words


def literal_alias_commands(text):
    """Read literal alias statements only. No source, eval, substitutions or shell process."""
    quote = None
    escaped = False
    invalid = False
    statement = []
    found = {}
    skipped = 0
    reserved = {'if', 'then', 'else', 'elif', 'fi', 'do', 'done', 'for', 'while', 'until', 'case', 'esac', 'in', 'function', 'select', 'time', 'coproc'}

    def finish(chars, unsafe):
        nonlocal skipped
        line = ''.join(chars).strip()
        if not re.match(r'(?:alias|\\alias)(?:\s|$)', line):
            return
        if unsafe:
            skipped += 1
            return
        words = alias_literal_words(line)
        if not words or len(words) < 2 or words[0] != 'alias':
            skipped += 1
            return
        definitions = []
        for word in words[1:]:
            name, separator, body = word.partition('=')
            if not separator or not re.fullmatch(r'[A-Za-z_][A-Za-z0-9_-]{0,63}', name) or name in reserved or not body.strip() or len(body) > 16384:
                skipped += 1
                return
            definitions.append((name, body))
        for name, body in definitions:
            found[name] = body

    comment = False
    for char in text + '\n':
        if comment:
            if char != '\n':
                continue
            comment = False
        if escaped:
            statement.append(char)
            escaped = False
            continue
        if char == '\\' and quote != "'":
            statement.append(char)
            escaped = True
            continue
        if quote:
            statement.append(char)
            if char == quote:
                quote = None
            elif quote == '"' and char in '$`':
                invalid = True
            continue
        if char in "'\"":
            quote = char
            statement.append(char)
        elif char == '#' and (not statement or statement[-1].isspace()):
            comment = True
        elif char == '\n':
            finish(statement, invalid)
            statement = []
            invalid = False
        else:
            if char in '$`&|<>(){}':
                invalid = True
            statement.append(char)
    if statement:
        finish(statement, True)  # Unclosed quote/continuation is never guessed.
    return found, skipped


def inspect_alias_shell(request):
    path = regular(request['path'])
    if path.stat().st_size > 1024 * 1024:
        raise BackupError('ALIAS_FILE_TOO_LARGE')
    with open(path, 'rb') as stream:
        raw = stream.read(1024 * 1024 + 1)
    if len(raw) > 1024 * 1024 or b'\0' in raw:
        raise BackupError('INVALID_ALIAS_FILE')
    environment = request['environment']
    if environment not in ('termux', 'ubuntu', 'debian', 'alpine', 'archlinux', 'fedora', 'opensuse', 'void'):
        raise BackupError('ALIAS_INVALID_ENVIRONMENT')
    definitions, skipped = literal_alias_commands(raw.decode('utf-8-sig').replace('\r\n', '\n'))
    aliases = [dict(id=str(uuid.uuid4()), name=name, command=command, environment=environment, category='', favorite=False) for name, command in definitions.items()]
    workspace = libraries(dict(version=8, commands=[], sessions=[], notes=[], recentCommands=[], aliases=aliases))
    return dict(path=str(path), fingerprints={str(path): hashlib.sha256(raw).hexdigest()}, workspace=workspace, aliasShell=True, environment=environment, skipped=skipped)


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
                elif name in ('workspace-v1.json','home/.termux/workspace-v1.json','data/data/com.termux/files/workspace-v1.json','data/user/0/com.termux/files/workspace-v1.json'):
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
                elif base in ('bash', 'env', 'python3', 'sh', 'ls', 'busybox') or name.endswith('.sh'):
                    f = tar.extractfile(m)
                    head = f.read(min(m.size, 128)) if f else b''
                    arch = arch_of(head)
                    if arch:
                        elf[name] = arch
                    if name.endswith('.sh') and len(candidates) < 500:
                        candidates.append(dict(kind='script', name=base, source=name))
            progress('SCAN',payload,current=name)
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
        with regular(path).open('rb') as raw, opener(raw,'rb') as f:
            while True:
                block = f.read(1024 * 1024)
                if not block:
                    break
                total += len(block)
                if total > MAX_BYTES + 1024 ** 3:
                    raise BackupError('ARCHIVE_LIMIT')
                progress('VERIFY_COMPRESSION',raw.tell(),path.stat().st_size,current=path.name)
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
    if not roots and any(n=='payload' or n.startswith('payload/') for n in all_names):
        roots.append(('data','payload','','selected-data'))
    if not roots and workspace:
        roots.append(('library', '', '', 'workspace-v8'))
    if not roots and any(n.startswith('workspace-sessions/') for n in all_names):
        roots.append(('app-data', '', '', 'jobs-app-data'))
    for kind, root, alias, fmt in sorted(set(roots)):
        selected = [n for n in all_names if not root or n == root or n.startswith(root + '/')]
        architecture = sorted({a for n, a in elf.items() if n in selected})
        # Only binaries in bin directories establish the rootfs architecture.
        primary = sorted({a for n, a in elf.items() if n in selected and any(n == (root + '/' if root else '') + q for q in ('bin/bash', 'bin/sh', 'usr/bin/bash', 'usr/bin/env', 'usr/bin/sh', 'bin/busybox', 'usr/bin/busybox'))})
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
            if path.is_symlink() or not re.fullmatch(r'[a-zA-Z0-9][a-zA-Z0-9_.-]{0,63}', path.name) or not fs.is_dir() or fs.is_symlink():
                continue
            if not any(e['name'] == path.name for e in result):
                result.append(dict(name=path.name, root=str(fs)))
    return result


def tree_bytes(path, excluded=()):
    # Reuse DirEntry metadata; never descend into queued symbolic-link directories.
    total = 0
    pending = [Path(path)]
    excluded = {str(p) for p in excluded}
    while pending:
        folder = pending.pop()
        try:
            if folder.is_symlink(): continue
            entries = os.scandir(folder)
        except OSError:
            continue
        with entries as children:
            for entry in children:
                try:
                    metadata = entry.stat(follow_symlinks=False)
                    if stat.S_ISDIR(metadata.st_mode):
                        if entry.path not in excluded: pending.append(Path(entry.path))
                    elif stat.S_ISREG(metadata.st_mode):
                        total += metadata.st_size
                        progress('ESTIMATE', total, current=entry.path)
                except FileNotFoundError:
                    continue  # Files can disappear during a live size estimate.
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
            progress('PROOT_DISTRO',current=Path(args[-1]).name)
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


def pigz_available(prefix):
    return os.access(Path(prefix) / 'bin/pigz', os.X_OK)


def compression_options(request):
    level=request.get('compressionLevel',3)
    engine=request.get('compressionEngine','gzip')
    if type(level) is not int or level not in (1,3,9) or engine not in ('gzip','pigz'):
        raise BackupError('INVALID_COMPRESSION')
    threads=request.get('compressionThreads',4 if engine=='pigz' else 1)
    if type(threads) is not int or threads not in ((2,4) if engine=='pigz' else (1,)):
        raise BackupError('INVALID_COMPRESSION')
    if engine=='pigz' and not pigz_available(request['prefix']):
        raise BackupError('PIGZ_REQUIRED')
    return dict(level=level,engine=engine,threads=threads,prefix=request['prefix'])


def compression_status(request):
    return dict(pigz=pigz_available(request['prefix']))


def install_pigz(request):
    # Only the explicit install-pigz UI request reaches here. Never execute imported commands.
    prefix=Path(request['prefix'])
    if pigz_available(prefix):return dict(pigz=True)
    executable=prefix/'bin/apt-get'
    if not os.access(executable,os.X_OK):raise BackupError('PIGZ_INSTALL_FAILED')
    # APT owns its locks. Do not disable them, stop other package managers or retry a broken dpkg.
    p=subprocess.Popen([str(executable),'install','-y','pigz'],stdin=subprocess.DEVNULL,
                       stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
    try:
        while p.poll() is None:
            progress('INSTALL_PIGZ',current='pigz');time.sleep(.5)
        if p.returncode or not pigz_available(prefix):raise BackupError('PIGZ_INSTALL_FAILED')
    finally:
        # The install dialog cannot be cancelled; wait rather than interrupt dpkg on a signal.
        if p.poll() is None:p.wait()
    return dict(pigz=True)


def finish_owned(p):
    if p.poll() is None:
        p.terminate()
        try:p.wait(timeout=5)
        except subprocess.TimeoutExpired:p.kill();p.wait()


@contextmanager
def compressed_output(path, options=None):
    options=options or dict(engine='gzip',level=3)
    if options['engine']=='gzip':
        with gzip.open(path,'xb',compresslevel=options['level']) as stream:yield stream
    else:
        # Bound parallelism to four workers; avoid using every core while terminals are active.
        with open(path,'xb') as output:
            p=subprocess.Popen([str(Path(options['prefix'])/'bin/pigz'),'-'+str(options['level']),
                                '-p',str(options.get('threads',4)),'-c'],stdin=subprocess.PIPE,
                               stdout=output,stderr=subprocess.DEVNULL)
            try:
                yield p.stdin
                p.stdin.close()
                if p.wait()!=0:raise BackupError('COMPRESSION_FAILED')
            finally:
                finish_owned(p)
                if not p.stdin.closed:p.stdin.close()


def backup_ubuntu(prefix,alias,path,options):
    if not proot_modern(prefix):
        # Legacy proot-distro has no supported uncompressed stdout interface.
        run_tool([str(prefix/'bin/proot-distro'),'backup','--output',str(path),alias])
        return 'native'
    p=subprocess.Popen([str(prefix/'bin/proot-distro'),'backup','--compress','none',alias],
                       stdin=subprocess.DEVNULL,stdout=subprocess.PIPE,stderr=subprocess.DEVNULL)
    try:
        count=0
        with compressed_output(path,options) as output:
            while True:
                data=p.stdout.read(1024*1024)
                if not data:break
                output.write(data);count+=len(data)
                progress('PROOT_DISTRO',count,current=alias)
        if p.wait()!=0:raise BackupError('PROOT_DISTRO_FAILED')
    finally:
        finish_owned(p);p.stdout.close()
    return options['engine']


def create_tar(path, source, root, exclude=(), total=0, compression=None):
    excluded = {str(p) for p in exclude}
    skipped = 0
    with compressed_output(path,compression) as output, tarfile.open(fileobj=output,mode='w|',dereference=False) as tar:
        track_archive(tar,total)
        def include(info):
            nonlocal skipped
            relative = info.name[len(root):].lstrip('/')
            if any(relative == p or relative.startswith(p + '/') for p in excluded):
                return None
            if not (info.isfile() or info.isdir() or info.issym() or info.islnk()):
                skipped += 1
                return None
            return info
        tar.add(source, arcname=root or '.', filter=include)
    return skipped


# Selective backups use a separate schema: v1 clients must refuse partial runtimes.
# Selection identifiers never contain arbitrary host paths or shell commands.
def selection_scopes(request):
    prefix, home = Path(request['prefix']), Path(request['home'])
    scopes = [dict(id='termux', root=str(home.parent), home='home', prefix='usr', type='termux')]
    for env in environments(prefix):
        scopes.append(dict(id='ubuntu:' + env['name'], root=env['root'], home='root', prefix='usr', type='ubuntu'))
    return scopes


def selection_layout(scope, custom=()):
    root, h = Path(scope['root']), scope['home']
    specs = []
    def item(key, paths, needs=()):
        paths = [p for p in paths if (root / p).exists() or (root / p).is_symlink()]
        if paths:
            specs.append(dict(id=key, paths=paths, requires=list(needs)))
    item('codex', [h+'/.codex', 'usr/local/lib/node_modules/@openai/codex', 'usr/local/bin/codex',
                  'usr/lib/node_modules/@openai/codex', 'usr/bin/codex', h+'/.npm-global/lib/node_modules/@openai/codex',
                  h+'/.npm-global/bin/codex'], ['environment', 'node'])
    item('agents', [h+'/'+p for p in ('.claude', '.claude.json', '.gemini', '.config/opencode', '.local/share/opencode',
                 '.opencode', '.local/share/claude', '.local/bin/claude')] + ['usr/local/lib/node_modules/'+p for p in ('@anthropic-ai/claude-code', '@google/gemini-cli')]
                 + ['usr/local/bin/'+p for p in ('claude', 'gemini', 'opencode')], ['environment', 'agent-runtime'])
    item('ssh', [h+'/'+p for p in ('.ssh', '.gitconfig', '.git-credentials', '.config/gh', '.config/git')], ['environment'])
    item('shell', [h+'/'+p for p in ('.bashrc', '.bash_aliases', '.profile', '.bash_profile', '.zshrc', '.bash_history',
                 '.zsh_history', 'bin', 'scripts', '.local/bin')], ['environment'])
    project_dirs = []
    if (root/h).is_dir() and not (root/h).is_symlink():
        for p in sorted((root/h).iterdir()):
            if p.is_dir() and not p.is_symlink() and not p.name.startswith('.') and p.name not in ('bin', 'scripts', 'storage'):
                project_dirs.append(p.relative_to(root).as_posix())
    # Known build/tool trees are separately selectable; no generic filename exclusion.
    builds = [h+'/'+p for p in ('.cache', '.npm', '.gradle', '.cargo/registry', '.cargo/git')]
    for parent in project_dirs:
        projects = [root/parent]
        if Path(parent).name in ('projects', 'codex-workspaces'):
            projects += [p for p in (root/parent).iterdir() if p.is_dir() and not p.is_symlink()]
        for project in projects:
            builds += [(project/p).relative_to(root).as_posix() for p in ('.tools', '.verification', 'build', 'target', '.gradle')]
    item('builds', builds, ['environment'])
    for value in custom:
        relative = clean(value)
        p = root / relative
        # Only user directories, not arbitrary host/private app trees or symlink targets.
        if not relative.startswith(h+'/') or not p.is_dir() or p.is_symlink() or root.resolve() not in p.resolve().parents:
            raise BackupError('INVALID_SELECTION_PATH')
        if any((root/Path(*PurePosixPath(relative).parts[:i])).is_symlink() for i in range(1,len(PurePosixPath(relative).parts)+1)):
            raise BackupError('INVALID_SELECTION_PATH')
        item('folder:'+relative, [relative], ['environment'])
    for path in project_dirs:
        item('project:'+path, [path], ['environment'])
    specs.append(dict(id='other', paths=[h]+(['home'] if scope['type']=='ubuntu' else []), requires=['environment']))
    specs.append(dict(id='system', paths=[''], requires=[]))
    return specs


def selection_issue(issues, root, path, error):
    if issues is None:
        raise BackupError('SOURCE_UNREADABLE')
    try: relative=Path(path).relative_to(root).as_posix()
    except ValueError: relative='.'
    if len(issues)<100:
        issues.append(dict(path=relative, errno=getattr(error,'errno',0) or 0))
    elif not any(i['path']=='.' for i in issues):
        issues.append(dict(path='.',errno=0))


def selection_backing(root, path, known_link=False):
    """Recognise proot's internal link2symlink files without following ordinary links.

    Only marker-named backing files in the rootfs .l2s store or alongside the
    original are accepted. Opening is descriptor-relative with O_NOFOLLOW;
    changing an ancestor into a link cannot redirect the read outside rootfs.
    No chmod or proot process is used. This is a portable regular-file copy,
    rather than an absolute link back into the source environment's container.
    """
    if not known_link and not path.is_symlink(): return None
    target=os.readlink(path)
    if not Path(target).name.startswith(('.proot.l2s.','.l2s.')): return None
    try:
        canonical_root=root.resolve(strict=True)
        backing=(Path(target) if os.path.isabs(target) else path.parent/target).resolve(strict=True)
        relative=backing.relative_to(canonical_root)
        if not backing.name.startswith(('.proot.l2s.','.l2s.')) or not (relative.parts[0]=='.l2s' or backing.parent==path.parent.resolve(strict=True)):
            raise BackupError('PROOT_LINK_UNSAFE')
        return relative
    except (ValueError,OSError) as error:
        raise BackupError('PROOT_LINK_UNSAFE') from error


def selection_open(root, relative):
    parts=PurePosixPath(clean(str(relative))).parts
    if not parts: raise BackupError('SOURCE_UNREADABLE')
    fd=os.open(root,os.O_RDONLY|os.O_DIRECTORY|os.O_NOFOLLOW)
    try:
        for part in parts[:-1]:
            nxt=os.open(part,os.O_RDONLY|os.O_DIRECTORY|os.O_NOFOLLOW,dir_fd=fd)
            os.close(fd);fd=nxt
        result=os.open(parts[-1],os.O_RDONLY|os.O_NOFOLLOW|os.O_NONBLOCK,dir_fd=fd)
        try:
            metadata=os.fstat(result)
            if not stat.S_ISREG(metadata.st_mode):raise BackupError('SOURCE_UNREADABLE')
            return result,metadata
        except BaseException:
            os.close(result);raise
    except OSError as error:
        raise BackupError('SOURCE_UNREADABLE') from error
    finally:os.close(fd)


def selection_size(root,path,metadata=None):
    if metadata is None: metadata=path.lstat()
    if stat.S_ISREG(metadata.st_mode):return metadata.st_size,True
    if stat.S_ISLNK(metadata.st_mode):
        backing=selection_backing(root,path,known_link=True)
        if backing is not None:
            fd,metadata=selection_open(root,backing)
            os.close(fd)
            return metadata.st_size,True
    return 0,False


def selection_walk(scope, specs=None, chosen=None, issues=None, include_metadata=False):
    root = Path(scope['root'])
    starts = [root/'home', root/'usr'] if scope['type']=='termux' else [root]
    excluded = {'.l2s','proc','sys','dev','run','tmp','mnt','media','sdcard','storage'} if scope['type']=='ubuntu' else {'usr/var/lib/proot-distro','usr/tmp','home/storage'}
    classifier = SelectionClassifier(specs) if specs is not None else None
    chosen_paths = [p for spec in (specs or []) if chosen is not None and spec['id'] in chosen for p in spec['paths']]
    def failed(error): selection_issue(issues,root,error.filename or root,error)
    for start in starts:
        if not start.is_dir() or start.is_symlink(): continue
        for folder, dirs, files in os.walk(start, followlinks=False, onerror=failed):
            # Transient Codex IPC, process locks and shell snapshots cannot be resumed on another device.
            parent = Path(folder).relative_to(root).as_posix()
            skip = {'tmp','app-server-control','app-server-daemon','thread-writer-locks','shell_snapshots','tui-thread-reference-capabilities'} if parent.endswith('/.codex') else set()
            dirs[:] = [d for d in dirs if (Path(folder,d).relative_to(root).as_posix() not in excluded and d not in skip)]
            if chosen is not None:
                def wanted(relative):
                    return classifier.group(relative) in chosen or any(p.startswith(relative+'/') for p in chosen_paths)
                dirs[:] = [d for d in dirs if wanted(Path(folder,d).relative_to(root).as_posix())]
            for name in dirs+files:
                path = Path(folder,name)
                relative = path.relative_to(root).as_posix()
                if relative in excluded or '/.codex/' in '/'+relative and (name.endswith(('.sqlite-wal','.sqlite-shm')) or name=='.sqlite-maintenance.lock'):
                    continue
                if scope['type']=='ubuntu' and name.startswith(('.proot.l2s.','.l2s.')):continue
                try:metadata = path.lstat();mode = metadata.st_mode
                except OSError as error:
                    selection_issue(issues,root,path,error);continue
                if stat.S_ISREG(mode) or stat.S_ISDIR(mode) or stat.S_ISLNK(mode):
                    yield (path, relative, metadata) if include_metadata else (path, relative)


class SelectionClassifier:
    """Compile prefix ownership once, then look up only path ancestors."""
    def __init__(self, specs):
        self.owners = {}
        for i, spec in enumerate(specs):
            for path in spec['paths']:
                rank = (len(path), int(spec['id'].startswith('folder:')), -i, spec['id'])
                previous = self.owners.get(path)
                if previous is None or rank > previous: self.owners[path] = rank

    def match(self, relative):
        path = relative
        while True:
            rank = self.owners.get(path)
            if rank is not None: return rank[3], path
            if not path: return 'system', ''
            path = path.rpartition('/')[0]

    def group(self, relative): return self.match(relative)[0]


def selection_group(relative, specs):
    return SelectionClassifier(specs).group(relative)


def selection_runtime(root):
    root=Path(root)
    return {name:any((root/p).is_file() for p in paths) for name,paths in dict(
        node=('usr/bin/node','usr/local/bin/node','usr/bin/nodejs'),
        git=('usr/bin/git','usr/local/bin/git'),ssh=('usr/bin/ssh','usr/local/bin/ssh')).items()}


# Estimates only: bounded directory aggregates, never reused for archive reads.
INVENTORY_CACHE_ROWS = 12000
INVENTORY_CACHE_SECONDS = 600
INVENTORY_CACHE_BYTES = 2 * 1024 * 1024


def inventory_cached(scope, specs, candidate, now, budget):
    if not isinstance(candidate, dict): return None
    cache = candidate.get('scan')
    try:
        root = Path(scope['root']).stat()
        if (not isinstance(cache, dict) or cache.get('root') != [root.st_dev, root.st_ino]
                or cache.get('specs') != [[s['id'], s['paths']] for s in specs]
                or not 0 <= now-cache['time'] < INVENTORY_CACHE_SECONDS): return None
        rows = cache['rows']
        if not isinstance(rows, list) or len(rows) > budget[0]: return None
        for row in rows:
            if (not isinstance(row, list) or len(row) != 3 or not isinstance(row[0], str)
                    or len(row[0]) > 16384 or not all(type(n) is int and 0 <= n < 2**63 for n in row[1:])): return None
        cost = sum(len(json.dumps(row[0], ensure_ascii=True))+128 for row in rows)
        if cost > budget[1]: return None
        issues = candidate.get('issues', [])
        if not isinstance(issues, list) or len(issues) > 101: return None
        if any(not isinstance(i, dict) or not isinstance(i.get('path'), str) for i in issues): return None
        budget[0] -= len(rows);budget[1] -= cost
        return cache, issues
    except (KeyError, TypeError, ValueError, OSError): return None


def selection_inventory(request):
    result=[];now=time.monotonic();budget=[INVENTORY_CACHE_ROWS,INVENTORY_CACHE_BYTES];reused=False
    old=request.get('inventoryCache',{})
    old_scopes=old.get('scopes',[]) if isinstance(old,dict) else []
    old_scopes={s.get('id'):s for s in old_scopes if isinstance(s,dict)} if isinstance(old_scopes,list) else {}
    for scope in selection_scopes(request):
        custom=request.get('folders',{}).get(scope['id'],[])
        if not isinstance(custom,list) or len(custom)>30: raise BackupError('INVALID_SELECTION')
        specs=selection_layout(scope,custom)
        base_specs=[s for s in specs if not s['id'].startswith('folder:')]
        classifier=SelectionClassifier(specs);base_classifier=SelectionClassifier(base_specs)
        counts={s['id']:dict(bytes=0,files=0,unreadable=0) for s in specs}
        cached=inventory_cached(scope,base_specs,old_scopes.get(scope['id']),now,budget)
        scan=None;root=Path(scope['root']);total=0
        if cached is not None:
            scan,issues=cached;reused=True
            for relative,size,files in scan['rows']:
                c=counts[classifier.group(relative)];c['bytes']+=size;c['files']+=files
        else:
            issues=[];aggregates={};cacheable=True
            for path,relative,metadata in selection_walk(scope,issues=issues,include_metadata=True):
                try:
                    size,is_file=selection_size(root,path,metadata)
                    c=counts[classifier.group(relative)];c['bytes']+=size;c['files']+=int(is_file);total+=size
                    if is_file and cacheable:
                        key=(relative.rpartition('/')[0],base_classifier.match(relative)[1])
                        if key not in aggregates:
                            cost=len(json.dumps(relative,ensure_ascii=True))+128
                            if budget[0]<=0 or budget[1]<cost: aggregates.clear();cacheable=False
                            else: budget[0]-=1;budget[1]-=cost;aggregates[key]=[relative,0,0]
                        if cacheable:
                            aggregates[key][1]+=size;aggregates[key][2]+=1
                except (OSError,BackupError) as error: selection_issue(issues,root,path,error)
                progress('ESTIMATE',total,current=relative)
            if cacheable:
                identity=root.stat()
                scan=dict(root=[identity.st_dev,identity.st_ino],specs=[[s['id'],s['paths']] for s in base_specs],time=time.monotonic(),rows=list(aggregates.values()))
        for issue in issues:
            group=classifier.group(issue['path'])
            for spec in specs:
                if issue['path']=='.' or group==spec['id'] or any(p.startswith(issue['path']+'/') for p in spec['paths']): counts[spec['id']]['unreadable']+=1
        scope['issues']=issues;scope['items']=[dict(s,**counts[s['id']]) for s in specs]
        scope['runtime']=selection_runtime(root)
        if scan is not None: scope['scan']=scan
        result.append(scope)
    # This small history tree is always fresh, independently of environment estimates.
    return dict(scopes=result,reusedEstimate=reused,libraryBytes=tree_bytes(Path(request['files'])/'workspace-sessions') if request.get('files') else 0)


def selected_plan(request):
    available={s['id']:s for s in selection_scopes(request)}
    result=[]
    selections=request.get('selections',[])
    if not isinstance(selections,list) or len(selections)>30: raise BackupError('INVALID_SELECTION')
    seen=set()
    for selection in selections:
        sid=selection.get('scope')
        if sid not in available or sid in seen: raise BackupError('INVALID_SELECTION')
        seen.add(sid); scope=available[sid]
        folders=selection.get('folders',[])
        if not isinstance(folders,list) or len(folders)>30 or len(set(folders))!=len(folders): raise BackupError('INVALID_SELECTION')
        specs=selection_layout(scope,folders)
        ids=selection.get('items',[])
        if not isinstance(ids,list) or len(ids)!=len(set(ids)) or any(i not in {s['id'] for s in specs} for i in ids):
            raise BackupError('INVALID_SELECTION')
        if ids: result.append((scope,specs,set(ids)))
    return result


def create_selected_tar(file, scope, specs, chosen, work, total=0, compression=None):
    import sqlite3
    from contextlib import closing
    root=Path(scope['root'])
    wrapper='ubuntu/rootfs' if scope['type']=='ubuntu' and 'system' in chosen else 'payload'
    entries=0
    classifier=SelectionClassifier(specs)
    contains_codex=False
    with compressed_output(file,compression) as output, tarfile.open(fileobj=output,mode='w|',dereference=False) as tar:
        track_archive(tar,total)
        base=tarfile.TarInfo(wrapper);base.type=tarfile.DIRTYPE;base.mode=0o700;tar.addfile(base)
        if wrapper!='payload':
            for name in ('proc','sys','dev','run','tmp','mnt','media','root','home'):
                directory=tarfile.TarInfo(wrapper+'/'+name);directory.type=tarfile.DIRTYPE;directory.mode=0o1777 if name=='tmp' else 0o700 if name=='root' else 0o755;tar.addfile(directory)
        for path, relative in selection_walk(scope,specs,chosen):
            if classifier.group(relative) not in chosen: continue
            contains_codex = contains_codex or relative.startswith(scope['home']+'/.codex/')
            arcname=wrapper+'/'+relative
            metadata=path.lstat()
            info=tarfile.TarInfo(arcname);info.mode=stat.S_IMODE(metadata.st_mode);info.mtime=metadata.st_mtime;info.uid=metadata.st_uid;info.gid=metadata.st_gid
            backing=selection_backing(root,path) if scope['type']=='ubuntu' and stat.S_ISLNK(metadata.st_mode) else None
            if stat.S_ISDIR(metadata.st_mode):
                info.type=tarfile.DIRTYPE;tar.addfile(info)
            elif stat.S_ISLNK(metadata.st_mode) and backing is None:
                info.type=tarfile.SYMTYPE;info.linkname=os.readlink(path);tar.addfile(info)
            else:
                fd,metadata=selection_open(root,backing if backing is not None else relative)
                try:
                    info.type=tarfile.REGTYPE;info.size=metadata.st_size;info.mode=stat.S_IMODE(metadata.st_mode);info.mtime=metadata.st_mtime
                    if '/.codex/' in '/'+relative and path.name.endswith('.sqlite'):
                        progress('DATABASE',current=relative,force=True)
                        snapshot=Path(work)/(uuid.uuid4().hex+'.sqlite');deadline=time.monotonic()+60
                        def progress_db(_status,_remaining,_total):
                            progress('DATABASE',_total-_remaining,_total,current=relative,unit='pages')
                            if time.monotonic()>deadline:raise BackupError('DATABASE_BUSY_RETRY')
                        try:
                            # Keep the usual database path so SQLite can see its live WAL.
                            # The pinned regular file must still match before/after backup.
                            current=path.stat()
                            if (current.st_dev,current.st_ino)!=(metadata.st_dev,metadata.st_ino):raise BackupError('SOURCE_CHANGED_RETRY')
                            with closing(sqlite3.connect(path.as_uri()+'?mode=ro',uri=True,timeout=5)) as source, closing(sqlite3.connect(snapshot)) as dest:
                                source.backup(dest,pages=256,progress=progress_db,sleep=0.05)
                                dest.commit();dest.execute('PRAGMA journal_mode=DELETE')
                                if dest.execute('PRAGMA quick_check').fetchone()[0]!='ok':raise BackupError('DATABASE_CHECK_FAILED')
                            current=path.stat()
                            if (current.st_dev,current.st_ino)!=(metadata.st_dev,metadata.st_ino):raise BackupError('SOURCE_CHANGED_RETRY')
                            info.size=snapshot.stat().st_size
                            with snapshot.open('rb') as stream:tar.addfile(info,stream)
                        finally:
                            if snapshot.exists():snapshot.unlink()
                    else:
                        with os.fdopen(fd,'rb',closefd=False) as stream:tar.addfile(info,stream)
                finally:os.close(fd)
            entries+=1
    # Selected data is not a bootable distro unless the base system was selected too.
    return dict(scope=scope['id'], environment=scope['type'], items=sorted(chosen),
                system='system' in chosen, **selection_runtime(root),
                containsCodex=contains_codex or 'codex' in chosen,
                historyOnlyWithoutProjects=(contains_codex or 'codex' in chosen) and not any(i.startswith(('project:','folder:')) for i in chosen))


def selection_requirements(selection, request):
    # Presence checks only; do not execute a foreign CLI or start a guest environment.
    available=[]
    for scope in selection_scopes(request):
        if scope['type']!=selection.get('environment'): continue
        root=Path(scope['root'])
        available.append(dict(scope=scope['id'],**selection_runtime(root)))
    return dict(candidates=available, needsNode=bool(selection.get('containsCodex')) or 'codex' in selection.get('items',[]),
                needsSshGit='ssh' in selection.get('items',[]),scriptsRequireReview='shell' in selection.get('items',[]),
                agentsRequireReview='agents' in selection.get('items',[]), stagingOnly=not(selection.get('environment')=='ubuntu' and selection.get('system')))


def create(request):
    compression=compression_options(request)
    prefix, home = Path(request['prefix']), Path(request['home'])
    parent = Path(request['destination']).resolve(strict=True)
    # Prevent archiving our output recursively; shared storage is intentionally supported.
    if parent == home or home in parent.parents or parent == prefix or prefix in parent.parents:
        raise BackupError('OUTPUT_INSIDE_SOURCE')
    plans = selected_plan(request)
    wanted = request['components']
    needs_full = 'home' in wanted or 'prefix' in wanted or bool(request.get('environments'))
    info = environment_info(request) if needs_full else dict(arch=current_arch(),environments=[],homeBytes=0,prefixBytes=0,ubuntuBytes={})
    library_bytes = tree_bytes(Path(request['files'])/'workspace-sessions') if 'library' in wanted else 0
    estimate = library_bytes + (info['homeBytes'] if 'home' in wanted else 0) + (info['prefixBytes'] if 'prefix' in wanted else 0)
    if any(name not in {e['name'] for e in info['environments']} for name in request.get('environments', [])):
        raise BackupError('SOURCE_CHANGED_RETRY')
    selected_envs = [e for e in info['environments'] if e['name'] in request.get('environments', [])]
    estimate += sum(info['ubuntuBytes'][e['name']] for e in selected_envs)
    plan_sizes=[]
    for scope,specs,chosen in plans:
        classifier=SelectionClassifier(specs)
        value=sum(selection_size(Path(scope['root']),p,m)[0] for p,r,m in selection_walk(scope,specs,chosen,include_metadata=True) if classifier.group(r) in chosen)
        plan_sizes.append(value);estimate+=value
    if shutil.disk_usage(parent).free < estimate + 64 * 1024 ** 2:
        raise BackupError('INSUFFICIENT_SPACE')
    out = parent / ('Termux-Jobs-' + time.strftime('%Y%m%d-%H%M%S') + '-' + uuid.uuid4().hex[:8])
    out.mkdir(mode=0o700)
    if request.get('files') and Path(request['files']).is_dir():
        # Private provenance for cleaning a future interrupted attempt, never inferred by name.
        receipt_fd=directory_fd(Path(request['files']),['workspace-backup-ownership'])
        try:
            record=os.open(out.name+'.json',os.O_WRONLY|os.O_CREAT|os.O_EXCL|os.O_NOFOLLOW,0o600,dir_fd=receipt_fd)
            st=out.stat()
            with os.fdopen(record,'wb') as stream:stream.write(json.dumps(dict(path=str(out),identity=[st.st_dev,st.st_ino])).encode())
        finally:os.close(receipt_fd)
    protected_write(out / 'INCOMPLETE', b'Not verified. Do not use as a complete backup.\n')
    meta = dict(format='termux-jobs-backup', version=2 if plans else 1, created=time.time(), live=True,
                consistency='Best effort live copy; user must finish writers before backup. No process memory.',
                compression=dict(engine=compression['engine'],level=compression['level'],threads=compression['threads']),
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
            create_tar(out / 'app-data.tar.gz', sessions, 'workspace-sessions',total=library_bytes,compression=compression)
            add('app-data.tar.gz', 'app-data')
    if 'home' in wanted:
        create_tar(out / 'home.tar.gz', home, 'home',total=info['homeBytes'],compression=compression)
        add('home.tar.gz', 'home')
    if 'prefix' in wanted:
        create_tar(out / 'prefix.tar.gz', prefix, 'usr', ('var/lib/proot-distro', 'tmp'),total=info['prefixBytes'],compression=compression)
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
        actual=backup_ubuntu(prefix,env['name'],file,compression)
        add(file.name, 'ubuntu')
        meta['components'][-1]['compressionEngine']=actual
        meta['components'][-1]['environmentAlias']=env['name']
    for index,(scope,specs,chosen) in enumerate(plans):
        file=out/('selected-'+str(index)+'.tar.gz')
        details=create_selected_tar(file,scope,specs,chosen,request['work'],total=plan_sizes[index],compression=compression)
        add(file.name,'ubuntu' if scope['type']=='ubuntu' and 'system' in chosen else 'data')
        meta['components'][-1]['selection']=details
        if scope['type']=='ubuntu': meta['components'][-1]['environmentAlias']=scope['id'].split(':',1)[1]
    # The helper is a known app asset; never copy a script from an imported archive for execution.
    shutil.copyfile(__file__, out / 'backup-tool.py')
    protected_write(out / 'restore.sh', b'''#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
cd -- "$(dirname -- "$0")"
command -v python3 >/dev/null || { echo 'Install Python first: pkg install python'; exit 1; }
exec python3 -I -S ./backup-tool.py --wizard ./manifest.json
''')
    protected_write(out / 'README-RESTORE.txt', b'''Termux Job's backup. Contains private credentials; keep offline and protected.
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
Selective manifest v2 needs app v1.1.1+ or this trusted helper. Keep the WHOLE directory.
Data-only payloads are staged, not activated. Prepare the environment/runtimes first; review README-SELECTED.txt after extraction.
In payload, Ubuntu paths are relative to guest /; Termux paths are relative to files/ (home/ and usr/).
Do not overwrite an active assistant directory. Chat history does not include unselected projects.
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
    selections = {}
    aliases = {}
    settings = None
    workspace = None
    if path.name.endswith('.json'):
        value = read_json(path)
        if value.get('format') == 'termux-jobs-backup':
            if value.get('version') not in (1,2) or (path.parent / 'INCOMPLETE').exists():
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
                if 'selection' in c:
                    details=c['selection']
                    if not isinstance(details,dict) or details.get('environment') not in ('ubuntu','termux') or not isinstance(details.get('items'),list) or len(details['items'])>100 or any(not isinstance(i,str) or len(i)>4096 for i in details['items']):
                        raise BackupError('INVALID_SELECTION')
                    if any(not isinstance(details.get(key),bool) for key in ('system','node')):
                        raise BackupError('INVALID_SELECTION')
                    expected='ubuntu' if details['environment']=='ubuntu' and details['system'] else 'data'
                    if c['kind']!=expected: raise BackupError('INVALID_SELECTION_ARCHIVE')
                    selections[str(f)]=details
                if 'environmentAlias' in c:
                    alias=c['environmentAlias']
                    if not isinstance(alias,str) or not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_.-]{0,63}',alias): raise BackupError('INVALID_ENVIRONMENT_ALIAS')
                    aliases[str(f)]=alias
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
        if str(f) in selections and (len(scan['components'])!=1 or scan['components'][0]['kind']!=declared):
            raise BackupError('INVALID_SELECTION_ARCHIVE')
        if declared=='data' and (len(scan['components'])!=1 or scan['components'][0]['kind']!='data'):
            raise BackupError('INVALID_SELECTION_ARCHIVE')
        if any(c['kind']=='prefix' for c in scan['components']): package_versions.update(scan['packages'])
        if scan['workspace']:
            workspace = scan['workspace']
        for c in scan['components']:
            # PREFIX is always staged; never applied to the running runtime.
            c.update(file=str(f), unpacked=scan['unpacked'], status='portable' if c['kind'] in ('home', 'library', 'app-data','data') else 'review')
            if c['kind'] == 'ubuntu':
                if str(f) in aliases: c['environmentAlias']=aliases[str(f)]
                c['status'] = 'compatible' if c['arch'] == current_arch() and proot_modern(Path(request['prefix'])) else 'blocked'
                c['reason'] = '' if c['status'] == 'compatible' else 'ARCH_OR_PROOT_UNCONFIRMED'
            if c['kind'] == 'prefix':
                c['reason'] = 'STAGING_ONLY_NOT_LIVE_PREFIX'
            if str(f) in selections:
                c['selection']=selections[str(f)]
                c['requirements']=selection_requirements(c['selection'],request)
                c['requirements']['architecture']=source.get('arch','')
                c['requirements']['architectureMatches']=source.get('arch')==current_arch()
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
                    progress('RESTORE_FILES',written,current=name)
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


def restore_ubuntu(path, component, prefix, work, receipt=None):
    if component['status'] != 'compatible':
        raise BackupError('ARCH_OR_PROOT_UNCONFIRMED')
    alias = 'jobs-' + uuid.uuid4().hex[:12]
    # Repack only rootfs under a fresh name, omit foreign host configuration scripts.
    target = prefix / 'var/lib/proot-distro/containers' / alias
    if target.exists() or target.is_symlink() or (prefix / 'var/lib/proot-distro/installed-rootfs' / alias).exists():
        raise BackupError('TARGET_EXISTS')
    rewritten = work / (alias + '.tar')
    root = component['root']
    runtime_directory_present = False
    with archive(path) as src, tarfile.open(rewritten, 'w') as dst:
        for m in src:
            name = clean(m.name)
            if root and not (name == root or name.startswith(root + '/')):
                continue
            rel = name[len(root):].lstrip('/') if root else name
            if rel == '.l2s':
                # Never accept a foreign runtime symlink/file at proot's write target.
                if not m.isdir():
                    raise BackupError('UNSAFE_LINK')
                runtime_directory_present = True
            item = copy.copy(m)
            # PAX path/linkpath override TarInfo names during writing. Drop foreign
            # extended headers and let tarfile regenerate them from rebased fields.
            # extractfile supplies logical bytes, so sparse inputs become regular files.
            item.pax_headers = {}
            item.sparse = None
            if item.isfile():
                item.type = tarfile.REGTYPE
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
            progress('PREPARE_UBUNTU',current=name)
        if not runtime_directory_present:
            # Native backup omits this transient store. Create it before native restore
            # reapplies restrictive rootfs modes (Arch rootfs may be 0555).
            runtime = tarfile.TarInfo(alias + '/rootfs/.l2s')
            runtime.type = tarfile.DIRTYPE
            runtime.mode = 0o700
            dst.addfile(runtime)
        # Fresh runtime metadata, never the archive's Entrypoint/Env/image origin.
        # Native run needs a manifest to bypass an unavailable passwd shell (Void).
        manifest = dict(image_config=dict(config=dict(Entrypoint=None, Cmd=['/bin/sh'],
                        WorkingDir='/', Env=['PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin'])))
        payload = json.dumps(manifest, separators=(',', ':')).encode('utf-8')
        member = tarfile.TarInfo(alias + '/manifest.json')
        member.mode = 0o600
        member.size = len(payload)
        dst.addfile(member, io.BytesIO(payload))
    scan_tar(rewritten)
    if receipt is not None:
        write_restore_receipt(receipt, alias, dict(alias=alias, source=component.get('environmentAlias', component.get('alias', '')), deleted=False))
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
    environment_bindings = {}
    paths = {}
    for i, c in zip(choices, selected):
        path = copies[c['file']]
        if c['kind'] == 'ubuntu':
            alias = restore_ubuntu(path, c, prefix, work, request if Path(request.get('files', '')).is_dir() and request.get('files') else None)
            environments_created.append(alias)
            source_alias=c.get('environmentAlias')
            if source_alias:
                if not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_.-]{0,63}',source_alias): raise BackupError('INVALID_ENVIRONMENT_ALIAS')
                environment_bindings[source_alias]=alias if source_alias not in environment_bindings else None
            paths[str(i)] = 'proot-distro run --isolated --work-dir / ' + shlex.quote(alias) + ' -- /bin/sh -i'
        elif c['kind'] == 'packages':
            target = destination / ('packages-' + str(i) + '.txt')
            shutil.copyfile(path, target)
            paths[str(i)] = str(target)
        else:
            target = destination / (c['kind'] + '-' + str(i))
            extract_new(path, c['root'], target)
            paths[str(i)] = str(target)
    if any('selection' in c or c['kind']=='data' for c in selected):
        protected_write(destination/'README-SELECTED.txt', b'Selected data is STAGED only. Existing files were NOT replaced.\nFor Ubuntu data, paths in payload are relative to the guest root (/); for Termux, relative to files/ (home/ and usr/).\nPrepare a compatible environment and runtime first. Review paths, scripts and authorization before manually copying files.\nDo not replace an active .codex directory. Stop the TARGET assistant first, retaining the original data.\nChat history does not include project files unless selected. Processes and Android Keystore cannot be restored.\n')
    (destination / 'INCOMPLETE').unlink()
    protected_write(destination / 'RESTORE-RESULT.json', json.dumps(dict(paths=paths, environments=environments_created, environmentBindings=environment_bindings)).encode())
    return dict(path=str(destination), paths=paths, environments=environments_created, environmentBindings=environment_bindings)



RESTORED_ALIAS = re.compile(r'jobs-[a-f0-9]{12}')


def restore_receipt_fd(request):
    return directory_fd(Path(request['files']), ['workspace-restored-environments'])


def write_restore_receipt(request, alias, value):
    if not RESTORED_ALIAS.fullmatch(alias): raise BackupError('INVALID_ENVIRONMENT_ALIAS')
    fd = restore_receipt_fd(request)
    temp = uuid.uuid4().hex + '.tmp'
    try:
        out = os.open(temp, os.O_WRONLY | os.O_CREAT | os.O_EXCL | os.O_NOFOLLOW, 0o600, dir_fd=fd)
        with os.fdopen(out, 'wb') as stream:
            stream.write(json.dumps(value).encode()); stream.flush(); os.fsync(stream.fileno())
        os.replace(temp, alias + '.json', src_dir_fd=fd, dst_dir_fd=fd)
    finally:
        try: os.unlink(temp, dir_fd=fd)
        except FileNotFoundError: pass
        os.close(fd)


def restored_sources(request):
    sources, deleted = {}, set()
    home = Path(request['home'])
    # Legacy successful imports have receipts in their HOME result folders.
    for folder in home.iterdir():
        if folder.is_symlink() or not folder.is_dir() or not re.fullmatch(r'restored-[0-9]{8}-[0-9]{6}-[a-f0-9]{8}', folder.name): continue
        try: value = read_json(folder / 'RESTORE-RESULT.json')
        except (OSError, ValueError, BackupError): continue
        if not isinstance(value, dict): continue
        envs, bindings = value.get('environments'), value.get('environmentBindings', {})
        if not isinstance(envs, list) or not isinstance(bindings, dict): continue
        for alias in envs:
            if isinstance(alias, str) and RESTORED_ALIAS.fullmatch(alias):
                sources[alias] = next((k for k,v in bindings.items() if v == alias and isinstance(k,str) and re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_.-]{0,63}',k)), '')
    fd = restore_receipt_fd(request)
    try:
        for name in os.listdir(fd):
            if not re.fullmatch(r'jobs-[a-f0-9]{12}\.json', name): continue
            try:
                f = os.open(name, os.O_RDONLY | os.O_NOFOLLOW, dir_fd=fd)
                with os.fdopen(f, 'rb') as stream:
                    if not stat.S_ISREG(os.fstat(stream.fileno()).st_mode): continue
                    raw = stream.read(4097)
                if len(raw) > 4096: continue
                record = json.loads(raw)
                alias = name[:-5]
                if record.get('alias') != alias: continue
                if record.get('deleted') is True: deleted.add(alias)
                else:
                    source=record.get('source','')
                    sources[alias]=source if isinstance(source,str) and re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_.-]{0,63}',source) else ''
            except (OSError, ValueError, AttributeError): continue
    finally: os.close(fd)
    return {k:v for k,v in sources.items() if k not in deleted}


def restored_target(prefix, alias):
    if not isinstance(alias, str) or not RESTORED_ALIAS.fullmatch(alias): raise BackupError('RESTORED_NOT_CONFIRMED')
    base=Path(prefix).resolve(strict=True)/'var/lib/proot-distro/containers'
    if base.resolve() != base.absolute(): raise BackupError('UNSAFE_LINK')
    target=base/alias
    st=target.lstat()
    if not stat.S_ISDIR(st.st_mode): raise BackupError('UNSAFE_LINK')
    return target, str(st.st_dev)+':'+str(st.st_ino)


def restored_inventory(request):
    confirmed=restored_sources(request);base=Path(request['prefix'])/'var/lib/proot-distro/containers';rows=[]
    if base.is_dir():
        for item in sorted(base.iterdir()):
            if not RESTORED_ALIAS.fullmatch(item.name): continue
            try: target, identity=restored_target(request['prefix'],item.name)
            except (OSError,BackupError): continue
            trusted=item.name in confirmed
            rows.append(dict(alias=item.name,source=confirmed.get(item.name,''),identity=identity,
                             confirmed=trusted,incomplete=not (target/'manifest.json').is_file(),
                             bytes=tree_bytes(target) if trusted else 0))
    return dict(environments=rows)


def locked_remove_available(prefix):
    # Fail closed on older managers without shared session/exclusive removal locks.
    prefix=Path(prefix).resolve(strict=True)
    if not proot_modern(prefix): return False
    for module in (prefix/'lib').glob('python*/site-packages/proot_distro'):
        try:
            if module.resolve() != module.absolute(): continue
            paths=[module/'commands/remove.py',module/'commands/login/__init__.py',module/'locking.py']
            if any(not p.is_file() or p.is_symlink() or p.stat().st_size>256*1024 for p in paths): continue
            remove,login,locking=[p.read_text() for p in paths]
            if 'ContainerLock(container_name, exclusive=True, command="remove")' in remove and 'exclusive=False, command="login", inheritable=True' in login and 'LOCK_NB' in locking: return True
        except (OSError,ValueError): continue
    return False


def remove_restored(request):
    alias=request.get('alias')
    if not isinstance(alias,str) or not RESTORED_ALIAS.fullmatch(alias): raise BackupError('RESTORED_NOT_CONFIRMED')
    if alias not in restored_sources(request): raise BackupError('RESTORED_NOT_CONFIRMED')
    prefix=Path(request['prefix']);target,identity=restored_target(prefix,alias)
    if identity != request.get('identity'): raise BackupError('RESTORED_CHANGED_RECHECK')
    if not locked_remove_available(prefix): raise BackupError('RESTORED_LOCKS_REQUIRED')
    # The native exclusive nonblocking lock refuses active sessions, including CLI sessions.
    # No force/kill flags, filesystem fallback or arbitrary command from the receipt.
    try: run_tool([str(prefix/'bin/proot-distro'),'remove',alias])
    except BackupError: raise BackupError('RESTORED_REMOVE_FAILED')
    if target.exists() or target.is_symlink(): raise BackupError('RESTORED_REMOVE_FAILED')
    write_restore_receipt(request,alias,dict(alias=alias,deleted=True))
    return dict(removed=alias)

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
    if operation in ('cleanup-inventory','cleanup-execute','cleanup-prepare-home'):
        import importlib.util
        spec=importlib.util.spec_from_file_location('workspace_cleanup',Path(__file__).with_name('workspace-cleanup.py'))
        module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
        event('DONE',result=module.run(request,sys.modules[__name__]));return
    value = {'alias-inspect-shell': inspect_alias_shell, 'alias-export': export_aliases, 'compression-status': compression_status, 'install-pigz': install_pigz, 'inventory': environment_info, 'selection-inventory': selection_inventory, 'create': create, 'inspect': inspect, 'restore': restore, 'packages': compare_packages, 'apt-plan': apt_plan, 'restored-inventory': restored_inventory, 'remove-restored': remove_restored}[operation](request)
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
