#!/usr/bin/env python3
"""Private, integrity-pinned upstream installer. Never replaces the system CLI."""
import hashlib
import os
import sys
from pathlib import Path

EXPECTED = 'f3b14ee9e1743b79eee8942366434128bab73014b903504bf0e321f8efa46b85'

def main():
    # This entry point is intentionally limited to creating the shared AI rootfs.
    if len(sys.argv) != 5 or sys.argv[1] != 'install' or sys.argv[3:] != ['--name', 'ubuntu-ai']:
        raise RuntimeError('PINNED_INSTALLER_ARGUMENTS_REFUSED')
    image = Path(sys.argv[2])
    if not image.is_absolute() or image.is_symlink() or not image.is_file() or image.stat().st_size != 41144320:
        raise RuntimeError('PINNED_IMAGE_REQUIRED')
    with image.open('rb') as f:
        if hashlib.file_digest(f,'sha256').hexdigest() != 'c08828893609894e5ab6d80f146961961148afde007ce904292418d8fcd011e0':
            raise RuntimeError('PINNED_IMAGE_CHECKSUM_FAILED')
    bundle = Path(__file__).with_name('ubuntu-ai-proot-distro-5.9.0.zip')
    if hashlib.sha256(bundle.read_bytes()).hexdigest() != EXPECTED:
        raise RuntimeError('PINNED_INSTALLER_CHECKSUM_FAILED')
    prefix = os.environ.get('PREFIX', '')
    if not prefix or not Path(prefix).is_absolute():
        raise RuntimeError('PINNED_INSTALLER_PREFIX_REQUIRED')
    os.environ['TERMUX__PREFIX'] = prefix
    sys.path.insert(0, str(bundle))
    from proot_distro.constants import PROGRAM_VERSION, RUNTIME_DIR
    if PROGRAM_VERSION != '5.9.0' or RUNTIME_DIR != prefix + '/var/lib/proot-distro':
        raise RuntimeError('PINNED_INSTALLER_IDENTITY_FAILED')
    from proot_distro.cli import main as upstream_main
    sys.argv[0] = 'ubuntu-ai-proot-distro-5.9.0'
    return upstream_main()

if __name__ == '__main__':
    sys.exit(main())
