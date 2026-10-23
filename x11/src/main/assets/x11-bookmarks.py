#!/usr/bin/env python3
"""Append fixed local shortcuts in the selected desktop user's GTK bookmarks."""
import os
from pathlib import Path
import stat
import tempfile
from urllib.parse import unquote, urlsplit

MAX_BYTES = 1024 * 1024

def local_path(uri):
    parsed = urlsplit(uri)
    if parsed.scheme != 'file' or parsed.netloc not in ('', 'localhost') or parsed.query or parsed.fragment:
        return None
    return unquote(parsed.path).rstrip('/') or '/'

def read_bookmarks(destination):
    if destination.is_symlink():
        raise ValueError('linked bookmarks preserved')
    try:
        fd = os.open(destination, os.O_RDONLY | os.O_NOFOLLOW | os.O_NONBLOCK)
    except FileNotFoundError:
        return b'', 0o600
    with os.fdopen(fd, 'rb') as source:
        info = os.fstat(source.fileno())
        if not stat.S_ISREG(info.st_mode) or info.st_size > MAX_BYTES:
            raise ValueError('unsupported bookmarks preserved')
        data = source.read(MAX_BYTES + 1)
        if len(data) > MAX_BYTES:
            raise ValueError('large bookmarks preserved')
        return data, stat.S_IMODE(info.st_mode) & 0o777

def prepare(home, config=None):
    home = Path(home)
    config = Path(config) if config else home / '.config'
    if not home.is_absolute() or not config.is_absolute():
        raise ValueError('absolute home/config required')
    # Do not follow user-config links or write outside this selected home.
    relative = config.relative_to(home)
    if '..' in relative.parts:
        raise ValueError('config outside home')
    parent = home
    for name in (*relative.parts, 'gtk-3.0'):
        parent = parent / name
        if parent.is_symlink():
            raise ValueError('linked config preserved')
        parent.mkdir(mode=0o700, exist_ok=True)
    destination = parent / 'bookmarks'
    original, mode = read_bookmarks(destination)
    text = original.decode('utf-8')
    known = set()
    for line in text.splitlines():
        if line.strip():
            try:
                known.add(local_path(line.split(None, 1)[0]))
            except ValueError:
                pass
    additions = []
    for path, label in [(Path('/sdcard'), 'Phone storage'), (Path('/sdcard/Download'), 'Downloads'), (home, 'Home')]:
        uri = path.as_uri()
        if local_path(uri) not in known:
            additions.append(uri + ' ' + label + '\n')
            known.add(local_path(uri))
    if not additions:
        return 0
    content = original + (b'\n' if original and not original.endswith(b'\n') else b'') + ''.join(additions).encode('utf-8')
    if len(content) > MAX_BYTES:
        raise ValueError('large bookmarks preserved')
    fd, name = tempfile.mkstemp(prefix='.jobs-bookmarks-', dir=parent)
    try:
        with os.fdopen(fd, 'wb') as output:
            os.fchmod(output.fileno(), mode)
            output.write(content)
            output.flush()
            os.fsync(output.fileno())
        # Preserve a file edited while preparing the replacement.
        if read_bookmarks(destination) != (original, mode):
            raise ValueError('changed bookmarks preserved')
        os.replace(name, destination)
    finally:
        if os.path.exists(name):
            os.unlink(name)
    return len(additions)

if __name__ == '__main__':
    try:
        count = prepare(Path.home(), os.environ.get('XDG_CONFIG_HOME'))
        print('JOBS_X11_BOOKMARKS=ready added=' + str(count), flush=True)
    except (OSError, ValueError, UnicodeError):
        # No path or bookmark contents in diagnostics. Do not block the desktop.
        print('JOBS_X11_BOOKMARKS=preserved-unavailable', flush=True)
