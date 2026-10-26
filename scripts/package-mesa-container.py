#!/usr/bin/env python3
"""Package repository-local Mesa 26.2.5 stages; never modify installed libraries."""
from pathlib import Path
import hashlib
import json
import os
import re
import shutil
import struct
import subprocess
import tarfile
import tempfile

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / '.tools/mesa-26.2.5'
OUT = ROOT / '.verification/mesa-26.2.5/components'
OUT.mkdir(parents=True, exist_ok=True)


def elf(path, musl):
    data = path.read_bytes()
    if data[:7] != b'\x7fELF\x02\x01\x01' or struct.unpack_from('<HH', data, 16) != (3, 183):
        raise ValueError('Not ARM64 ET_DYN: ' + str(path))
    dynamic = subprocess.check_output(['readelf', '-d', str(path)], text=True)
    if re.search(r'\((?:RPATH|RUNPATH)\).*\[(?!\$ORIGIN)', dynamic):
        raise ValueError('External runtime path: ' + str(path))
    if not musl:
        versions = subprocess.check_output(['readelf', '--version-info', str(path)], text=True)
        for pattern, limit in [(r'GLIBC_([0-9.]+)', (2, 38)), (r'GLIBCXX_([0-9.]+)', (3, 4, 32))]:
            values = [tuple(map(int, m.split('.'))) for m in re.findall(pattern, versions)]
            if values and max(values) > limit:
                raise ValueError('New host ABI requirement: ' + str(path))
    else:
        symbols = subprocess.check_output(['readelf', '--dyn-syms', '--wide', str(path)], text=True)
        # Alpine's GCC startup objects reference these two weak unwind helpers.
        # Its own libgcc_s exports their historical GLIBC_2.0 version labels;
        # these are not references to glibc's libc or ARM64 glibc entry points.
        allowed = {'__register_frame_info@GLIBC_2.0', '__deregister_frame_info@GLIBC_2.0'}
        for line in symbols.splitlines():
            if 'GLIBC_' in line and not (' WEAK ' in line and line.split()[-2] in allowed):
                raise ValueError('Foreign ABI in musl library: ' + str(path))
        if b'__system_property_get\0' in data or re.search(r'NEEDED.*\[(?:libc\.so\.6|ld-linux[^]]*)\]', dynamic):
            raise ValueError('Foreign libc in musl library: ' + str(path))


records = []
for name, kind, libdir, targets in [
    ('glibc-multiarch', 'glibc', 'lib/aarch64-linux-gnu', ['ubuntu', 'debian', 'opensuse']),
    ('glibc-lib64', 'glibc', 'lib64', ['fedora']),
    ('glibc-lib', 'glibc', 'lib', ['void', 'archlinux']),
    ('musl-alpine3.24', 'musl', 'lib', ['alpine']),
]:
    source = TOOLS / ('stage-' + kind) / 'usr'
    required = ['libvulkan_freedreno.so', 'libgallium-26.2.5.so']
    required += ['libGL.so.1', 'libEGL.so.1'] if kind == 'musl' else ['libGLX_mesa.so.0', 'libEGL_mesa.so.0']
    for item in required:
        if not (source / 'lib' / item).is_file():
            raise ValueError('Missing Mesa stage component: ' + item)
    with tempfile.TemporaryDirectory(prefix='package-', dir=TOOLS) as temporary:
        stage = Path(temporary)
        destination = stage / 'usr' / libdir
        destination.mkdir(parents=True)
        # Runtime libraries only; do not distribute headers, executable tools or pkg-config files.
        for path in (source / 'lib').rglob('*'):
            if not (path.is_file() or path.is_symlink()) or not re.search(r'\.so(?:\.[0-9]+)*$', path.name):
                continue
            relative = path.relative_to(source / 'lib')
            out = destination / relative
            out.parent.mkdir(parents=True, exist_ok=True)
            if path.is_symlink():
                resolved = path.resolve()
                if not resolved.is_relative_to((source / 'lib').resolve()):
                    raise ValueError('Stage library link escapes: ' + str(relative))
                value = os.readlink(path)
                if value.startswith('/'):
                    raise ValueError('Absolute stage link: ' + str(relative))
                out.symlink_to(value)
            else:
                shutil.copyfile(path, out)
                subprocess.run(['strip', '--strip-unneeded', str(out)], check=True)
                elf(out, kind == 'musl')
                out.chmod(0o644)
        share = source / 'share/glvnd/egl_vendor.d'
        if share.is_dir():
            for path in share.glob('*.json'):
                value = json.loads(path.read_text())
                if value.get('ICD', {}).get('library_path') != 'libEGL_mesa.so.0':
                    raise ValueError('Unexpected EGL vendor path')
                out = stage / 'usr/share/glvnd/egl_vendor.d' / path.name
                out.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(path, out)
        artifact = OUT / ('mesa-26.2.5-jobs1-' + name + '-aarch64.tar.gz')
        def metadata(info):
            info.uid = info.gid = 0
            info.uname = info.gname = ''
            info.mtime = 1792886400
            return info
        with tarfile.open(artifact, 'w:gz', format=tarfile.PAX_FORMAT) as archive:
            for path in sorted(stage.rglob('*')):
                archive.add(path, arcname=str(path.relative_to(stage)), recursive=False, filter=metadata)
        records.append(dict(asset=artifact.name, bytes=artifact.stat().st_size,
                            sha256=hashlib.sha256(artifact.read_bytes()).hexdigest(),
                            targets=targets, format='tar', mesaVersion='26.2.5'))
(OUT / 'mesa-packages.json').write_text(json.dumps(records, indent=2) + '\n')
print(json.dumps(records, indent=2))
