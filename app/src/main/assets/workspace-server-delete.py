"""Confirmed server deletion. Linux dir-fd traversal, no shell filenames or symlink following."""
import json
import os
import stat
import sys
import time

LIMIT = 20000
DEPTH = 64
SECONDS = 25


def identity(st):
    return st.st_dev, st.st_ino, st.st_mode, st.st_size, st.st_mtime_ns


def open_directory(path):
    if not isinstance(path, str) or not path.startswith('/') or '\0' in path or os.path.normpath(path) != path or path.startswith('//'):
        raise ValueError('INVALID_PATH')
    fd = os.open('/', os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW)
    try:
        for part in path.split('/')[1:]:
            if not part:
                continue
            child = os.open(part, os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW, dir_fd=fd)
            os.close(fd)
            fd = child
        return fd
    except BaseException:
        os.close(fd)
        raise


def delete_selection(request):
    deleted = []
    count = [0]
    removed = [0]
    deadline = time.monotonic() + SECONDS
    parent = None

    def check():
        if time.monotonic() > deadline:
            raise ValueError('TIMEOUT')

    def scan(fd, name, depth, device):
        check()
        count[0] += 1
        if count[0] > LIMIT or depth > DEPTH:
            raise ValueError('TREE_LIMIT')
        st = os.stat(name, dir_fd=fd, follow_symlinks=False)
        if st.st_dev != device:
            raise ValueError('DELETE_MOUNT')
        children = []
        if stat.S_ISDIR(st.st_mode):
            child = os.open(name, os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW, dir_fd=fd)
            try:
                if identity(os.fstat(child)) != identity(st):
                    raise ValueError('REMOTE_CHANGED')
                with os.scandir(child) as listing:
                    for item in listing:
                        children.append(scan(child, item.name, depth + 1, device))
                if identity(os.fstat(child)) != identity(st):
                    raise ValueError('REMOTE_CHANGED')
            finally:
                os.close(child)
        elif not (stat.S_ISREG(st.st_mode) or stat.S_ISLNK(st.st_mode)):
            raise ValueError('UNSUPPORTED_TYPE')
        return name, identity(st), children

    def remove(fd, node):
        check()
        name, expected, children = node
        st = os.stat(name, dir_fd=fd, follow_symlinks=False)
        if identity(st) != expected:
            raise ValueError('REMOTE_CHANGED')
        if stat.S_ISDIR(st.st_mode):
            child = os.open(name, os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW, dir_fd=fd)
            try:
                if identity(os.fstat(child)) != expected:
                    raise ValueError('REMOTE_CHANGED')
                for item in children:
                    remove(child, item)
                current = os.stat(name, dir_fd=fd, follow_symlinks=False)
                if (current.st_dev, current.st_ino) != expected[:2]:
                    raise ValueError('REMOTE_CHANGED')
                os.rmdir(name, dir_fd=fd)
            finally:
                os.close(child)
        else:
            os.unlink(name, dir_fd=fd)
        removed[0] += 1

    try:
        directory = request.get('directory')
        entries = request.get('entries')
        if not isinstance(entries, list) or not 1 <= len(entries) <= 500:
            raise ValueError('INVALID_PATH')
        parent = open_directory(directory)
        device = os.fstat(parent).st_dev
        plans = []
        seen = set()
        for entry in entries:
            path = entry.get('path') if isinstance(entry, dict) else None
            if not isinstance(path, str) or not path.startswith('/') or '\0' in path or os.path.normpath(path) != path:
                raise ValueError('INVALID_PATH')
            name = os.path.basename(path)
            if not name or name in ('.', '..') or os.path.dirname(path) != directory or path in seen:
                raise ValueError('INVALID_PATH')
            seen.add(path)
            node = scan(parent, name, 0, device)
            # Listing values are advisory for links (the old agent follows links when listing).
            mode = node[1][2]
            if not stat.S_ISLNK(mode):
                if bool(entry.get('directory', False)) != stat.S_ISDIR(mode):
                    raise ValueError('REMOTE_CHANGED')
                if stat.S_ISREG(mode) and entry.get('size') != node[1][3]:
                    raise ValueError('REMOTE_CHANGED')
            plans.append((path, node))
        # Entire selection validated before any mutation. No retries or rollback.
        for path, node in plans:
            remove(parent, node)
            deleted.append(path)
        return {'deleted': deleted, 'removedNodes': removed[0], 'error': ''}
    except Exception as exc:
        allowed = {'INVALID_PATH', 'TREE_LIMIT', 'TIMEOUT', 'REMOTE_CHANGED', 'DELETE_MOUNT', 'UNSUPPORTED_TYPE'}
        code = str(exc) if isinstance(exc, ValueError) and str(exc) in allowed else 'PERMISSION_DENIED' if isinstance(exc, PermissionError) else 'DELETE_FAILED'
        return {'deleted': deleted, 'removedNodes': removed[0], 'error': code}
    finally:
        if parent is not None:
            os.close(parent)


if __name__ == '__main__':
    try:
        raw = sys.stdin.buffer.read(512001)
        if len(raw) > 512000:
            raise ValueError('RESPONSE_LIMIT')
        print(json.dumps({'ok': True, 'result': delete_selection(json.loads(raw))}))
    except Exception:
        print(json.dumps({'ok': False, 'error': 'INVALID_PATH'}))
