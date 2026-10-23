"""Explicit storage overview and snapshot-based, opt-in deletion."""
import fcntl
import json
import os
from pathlib import Path
import re
import stat
import time
import uuid
from itertools import chain
from contextlib import contextmanager, ExitStack

MAX_ENTRIES = 60000
MAX_PLAN_BYTES = 8 * 1024 * 1024
PLAN_TTL = 600
CACHE_LAYOUT = [
    ('var/cache/apt/archives', 'deb'),
    ('var/cache/apk', 'apk'),
    ('var/cache/libdnf5', 'rpm'),
    ('var/cache/dnf', 'rpm'),
    ('var/cache/zypp/packages', 'rpm-all'),
    ('var/cache/xbps', 'xbps'),
    ('var/cache/pacman/pkg', 'pacman'),
]


def fail(b, code):
    raise b.BackupError(code)


def identity(st):
    return [st.st_dev, st.st_ino, st.st_size, st.st_mtime_ns]


def safe_dir(path):
    path = Path(path).absolute()
    # Android's top-level /data/data and /sdcard aliases are resolved by callers.
    if path.resolve(strict=True) != path or not stat.S_ISDIR(path.lstat().st_mode):
        raise OSError('unsafe directory')
    return path


def open_dir(root, parts=()):
    fd = os.open(root, os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW)
    try:
        for part in parts:
            if not isinstance(part, str) or part in ('', '.', '..') or '/' in part or '\x00' in part:
                raise OSError('unsafe component')
            new = os.open(part, os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW, dir_fd=fd)
            os.close(fd)
            fd = new
        return fd
    except BaseException:
        os.close(fd)
        raise


def package_file(name, flavor, parts):
    if flavor == 'deb': return name.endswith('.deb') and len(parts) == 1
    if flavor == 'apk': return name.endswith('.apk') and len(parts) == 1
    if flavor == 'rpm': return name.endswith('.rpm') and 'packages' in parts[:-1]
    if flavor == 'rpm-all': return name.endswith('.rpm')
    if flavor == 'xbps': return name.endswith('.xbps') and len(parts) == 1
    if flavor == 'pacman': return bool(re.fullmatch(r'.+\.pkg\.tar\.(?:gz|xz|zst|bz2|lz4|lrz|lzo|Z)(?:\.sig)?', name)) and len(parts) == 1
    return False


def snapshot(root, accept, b, strict=False, top=None, links=False, stores=None):
    files, dirs = stores if stores is not None else ([], [])
    total = 0
    count = 0
    def walk(fd, parts):
        nonlocal total, count
        dirs.append(dict(parts=parts, identity=identity(os.fstat(fd))))
        with os.scandir(fd) as entries:
            for entry in entries:
                if not parts and top is not None and entry.name not in top: continue
                count += 1
                if stores is None and count > MAX_ENTRIES: fail(b, 'CLEANUP_TOO_MANY_FILES')
                st = entry.stat(follow_symlinks=False)
                child = parts + [entry.name]
                if stat.S_ISDIR(st.st_mode):
                    sub = os.open(entry.name, os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW, dir_fd=fd)
                    try: walk(sub, child)
                    finally: os.close(sub)
                elif stat.S_ISREG(st.st_mode):
                    if accept(entry.name, child):
                        files.append(dict(parts=child, identity=identity(st), link=False))
                        total += st.st_size
                        b.progress('CLEANUP_SCAN', len(files), current=entry.name, unit='files')
                elif links and stat.S_ISLNK(st.st_mode):
                    files.append(dict(parts=child, identity=identity(st), link=True))
                    total += st.st_size
                elif strict:
                    fail(b, 'UNSAFE_LINK')
    fd = open_dir(root)
    try: walk(fd, [])
    finally: os.close(fd)
    return dict(root=str(root), files=files, dirs=dirs, bytes=total)


class EntryWriter:
    def __init__(self, parent, name):
        self.name, self.count = name, 0
        self.stream = os.fdopen(os.open(name, os.O_WRONLY | os.O_CREAT | os.O_EXCL | os.O_NOFOLLOW, 0o600, dir_fd=parent), 'wb')
    def __enter__(self): return self
    def __exit__(self, *args): self.stream.close()
    def append(self, entry):
        self.stream.write(json.dumps(entry, separators=(',', ':')).encode()+b'\n')
        self.count += 1
    def __len__(self): return self.count
    def metadata(self):
        self.stream.flush()
        os.fsync(self.stream.fileno())
        return dict(name=self.name, count=self.count, identity=identity(os.fstat(self.stream.fileno())))


class DiskEntries:
    def __init__(self, fd, count, b): self.fd, self.count, self.b = fd, count, b
    def __len__(self): return self.count
    def decode(self, raw):
        try:
            row=json.loads(raw)
            if not isinstance(row,dict) or not isinstance(row.get('parts'),list) or not isinstance(row.get('identity'),list): raise ValueError()
            return row
        except (ValueError, UnicodeError): fail(self.b,'CLEANUP_PLAN_INVALID')
    def __iter__(self):
        # The descriptor stays open across both passes; never reopen by path.
        offset, pending, count = 0, b'', 0
        while True:
            data=os.pread(self.fd,65536,offset)
            if not data: break
            offset+=len(data);pending+=data
            lines=pending.split(b'\n');pending=lines.pop()
            for line in lines:
                count+=1
                yield self.decode(line)
            if len(pending)>1024*1024: fail(self.b,'CLEANUP_PLAN_INVALID')
        if pending or count!=self.count: fail(self.b,'CLEANUP_PLAN_INVALID')
    def __reversed__(self):
        offset=os.fstat(self.fd).st_size;pending=b'';count=0
        while offset:
            size=min(65536,offset);offset-=size
            pending=os.pread(self.fd,size,offset)+pending
            lines=pending.split(b'\n');pending=lines.pop(0)
            for line in reversed(lines):
                if not line: continue  # Last line ends with a newline.
                count+=1
                yield self.decode(line)
            if len(pending)>1024*1024: fail(self.b,'CLEANUP_PLAN_INVALID')
        if pending:
            count+=1
            yield self.decode(pending)
        if count!=self.count: fail(self.b,'CLEANUP_PLAN_INVALID')


@contextmanager
def open_entries(request, b, spool, kind):
    meta=spool.get(kind,{})
    name=meta.get('name','')
    if not isinstance(name,str) or not re.fullmatch(r'[a-f0-9]{32}\.'+kind,name) or not isinstance(meta.get('count'),int) or meta['count']<0:
        fail(b,'CLEANUP_PLAN_INVALID')
    parent=private_fd(request,b)
    try: fd=os.open(name,os.O_RDONLY|os.O_NOFOLLOW,dir_fd=parent)
    finally: os.close(parent)
    try:
        st=os.fstat(fd)
        if not stat.S_ISREG(st.st_mode) or identity(st)!=meta.get('identity'): fail(b,'CLEANUP_CHANGED_RESCAN')
        yield DiskEntries(fd,meta['count'],b)
    finally: os.close(fd)


def scope_for_path(path, request):
    prefix=Path(request['prefix']).resolve(strict=True)
    base=prefix/'var/lib/proot-distro/containers'
    try:
        parts=path.relative_to(base).parts
        if len(parts)>=2 and parts[1]=='rootfs': return parts[0]
    except ValueError: pass
    home=Path(request['home']).resolve(strict=True)
    if path==prefix or prefix in path.parents or path==home or home in path.parents: return 'termux'
    return ''


def cache_scopes(request):
    prefix = Path(request['prefix']).resolve(strict=True)
    scopes = []
    host = prefix/'var/cache/apt/archives'
    if host.is_dir():
        try: scopes.append(('termux', safe_dir(host), 'deb'))
        except OSError: pass
    base = prefix/'var/lib/proot-distro/containers'
    if base.is_dir():
        try: safe_dir(base)
        except OSError: return scopes
        for container in sorted(base.iterdir()):
            if not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_.-]{0,63}', container.name): continue
            try: root = safe_dir(container/'rootfs')
            except OSError: continue
            for relative, flavor in CACHE_LAYOUT:
                p = root/relative
                if not p.is_dir(): continue
                try: scopes.append((container.name, safe_dir(p), flavor))
                except OSError: pass
    return scopes


def private_fd(request, b):
    return b.directory_fd(Path(request['files']), ['workspace-cleanup-plans'])


def write_plan(request, b, value):
    raw = json.dumps(value).encode()
    if len(raw) > MAX_PLAN_BYTES: fail(b, 'CLEANUP_TOO_MANY_FILES')
    token = uuid.uuid4().hex
    fd = private_fd(request, b)
    try:
        file = os.open(token+'.json', os.O_WRONLY | os.O_CREAT | os.O_EXCL | os.O_NOFOLLOW, 0o600, dir_fd=fd)
        with os.fdopen(file, 'wb') as stream: stream.write(raw)
        # Only our expired scan plans; never backup jobs, archives or user files.
        for name in os.listdir(fd):
            if not re.fullmatch(r'[a-f0-9]{32}\.(?:json|files|dirs)', name): continue
            st = os.stat(name, dir_fd=fd, follow_symlinks=False)
            if stat.S_ISREG(st.st_mode) and time.time()-st.st_mtime > 86400:
                os.unlink(name, dir_fd=fd)
    finally: os.close(fd)
    return token


def backup_owned(folder, request, b):
    try:
        meta = b.read_json(folder/'manifest.json')
        return isinstance(meta, dict) and meta.get('format') == 'termux-jobs-backup' and meta.get('version') in (1, 2) and meta.get('source', {}).get('packageId') == request['packageId']
    except (OSError, ValueError, b.BackupError, AttributeError): return False


def partial_owned(folder, request, b):
    # Only new attempts with a private receipt; INCOMPLETE/name alone is insufficient.
    if not (folder/'INCOMPLETE').is_file(): return False
    try:
        fd = b.directory_fd(Path(request['files']), ['workspace-backup-ownership'])
        try:
            f = os.open(folder.name+'.json', os.O_RDONLY | os.O_NOFOLLOW, dir_fd=fd)
            with os.fdopen(f, 'rb') as stream:
                raw = stream.read(8193)
            if len(raw) > 8192: return False
            record = json.loads(raw)
            st = folder.lstat()
            return record.get('path') == str(folder) and record.get('identity') == [st.st_dev, st.st_ino]
        finally: os.close(fd)
    except (OSError, ValueError, AttributeError): return False


def inventory(request, b):
    groups = []
    def add(kind, name, scope, snap, remove_root=False):
        if not snap['files'] and not remove_root: return
        snap.update(id=uuid.uuid4().hex, kind=kind, name=name, scope=scope, removeRoot=remove_root)
        groups.append(snap)
    for alias, root, flavor in cache_scopes(request):
        try:
            snap = snapshot(root, lambda name, parts: package_file(name, flavor, parts), b)
            add('cache', root.name, alias, snap)
        except OSError: continue
    reports = Path(request['files']).resolve(strict=True)/'workspace-diagnostics'
    if reports.is_dir():
        try:
            add('reports', 'workspace-diagnostics', '', snapshot(safe_dir(reports), lambda name, parts: len(parts)==1 and name.endswith('.txt'), b))
        except OSError: pass
    archive_error = False
    try:
        parent = Path(request['archiveRoot']).resolve(strict=True)
        safe_dir(parent)
        for folder in sorted(parent.iterdir()):
            if not re.fullmatch(r'Termux-Jobs-[0-9]{8}-[0-9]{6}-[a-f0-9]{8}', folder.name): continue
            try: safe_dir(folder)
            except OSError: continue
            complete = backup_owned(folder, request, b)
            partial = not complete and partial_owned(folder, request, b)
            if not (complete or partial): continue
            try:
                snap = snapshot(folder, lambda name, parts: True, b, strict=True)
                add('backup' if complete else 'partial', folder.name, scope_for_path(folder,request), snap, True)
            except (OSError, b.BackupError): continue
    except (OSError, KeyError): archive_error = True
    overview_data=overview(request,b) if request.get('overview') else []
    if request.get('overview'):
        for alias,root in environment_roots(request,b).items():
            value=sum(x['bytes'] for x in overview_data if x['scope']==alias or x['scope']==alias+'/home')
            groups.append(dict(id=uuid.uuid4().hex,kind='environment',name=alias,scope=alias,root=str(root),rootIdentity=identity(root.lstat())[:2],containerIdentity=identity(root.parent.lstat())[:2],bytes=value,files=[],dirs=[],removeRoot=False))
    plan = dict(created=time.time(), prefix=str(Path(request['prefix']).resolve(strict=True)), filesRoot=str(Path(request['files']).resolve(strict=True)), groups=groups)
    token = write_plan(request, b, plan)
    result = dict(plan=token, archiveUnavailable=archive_error, groups=[{k: g[k] for k in ('id','kind','name','scope','bytes')} | dict(count=len(g['files']),path=g['root'],selectable=g['kind']!='environment' or b.locked_remove_available(Path(request['prefix']))) for g in groups])
    if request.get('overview'): result['overview'] = overview_data
    return result


def read_plan(request, b):
    token = request.get('plan', '')
    if not isinstance(token,str) or not re.fullmatch(r'[a-f0-9]{32}', token): fail(b, 'CLEANUP_PLAN_INVALID')
    fd = private_fd(request, b)
    try:
        f = os.open(token+'.json', os.O_RDONLY | os.O_NOFOLLOW, dir_fd=fd)
        with os.fdopen(f, 'rb') as stream:
            if not stat.S_ISREG(os.fstat(stream.fileno()).st_mode): fail(b,'CLEANUP_PLAN_INVALID')
            raw=stream.read(MAX_PLAN_BYTES+1)
        if len(raw)>MAX_PLAN_BYTES: fail(b,'CLEANUP_PLAN_INVALID')
        value=json.loads(raw)
    finally: os.close(fd)
    if not isinstance(value,dict) or not 0 <= time.time()-value.get('created',0) <= PLAN_TTL: fail(b,'CLEANUP_PLAN_EXPIRED')
    if value.get('prefix') != str(Path(request['prefix']).resolve(strict=True)) or value.get('filesRoot') != str(Path(request['files']).resolve(strict=True)): fail(b,'CLEANUP_PLAN_INVALID')
    return value


@contextmanager
def cache_lock(request, alias, b):
    prefix=Path(request['prefix']).resolve(strict=True)
    with ExitStack() as stack:
        if alias != 'termux':
            if not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_.-]{0,63}',alias): fail(b,'CLEANUP_PLAN_INVALID')
            if not b.locked_remove_available(prefix): fail(b,'RESTORED_LOCKS_REQUIRED')
            root=safe_dir(prefix/'var/lib/proot-distro')
            fd=b.directory_fd(root,['locks']);stack.callback(os.close,fd)
            lock=os.open(alias+'.lock',os.O_RDWR|os.O_CREAT|os.O_NOFOLLOW,0o600,dir_fd=fd);stack.callback(os.close,lock)
            if not stat.S_ISREG(os.fstat(lock).st_mode): fail(b,'UNSAFE_LINK')
            try: fcntl.flock(lock,fcntl.LOCK_EX|fcntl.LOCK_NB)
            except OSError: fail(b,'CLEANUP_ENVIRONMENT_BUSY')
        else:
            for relative in ['var/lib/dpkg/lock-frontend','var/lib/dpkg/lock','var/lib/apt/lists/lock','var/cache/apt/archives/lock']:
                path=prefix/relative
                if not path.parent.is_dir(): continue
                parent=open_dir(safe_dir(path.parent));stack.callback(os.close,parent)
                lock=os.open(path.name,os.O_RDWR|os.O_CREAT|os.O_NOFOLLOW,0o600,dir_fd=parent);stack.callback(os.close,lock)
                if not stat.S_ISREG(os.fstat(lock).st_mode): fail(b,'UNSAFE_LINK')
                try: fcntl.lockf(lock,fcntl.LOCK_EX|fcntl.LOCK_NB)
                except OSError: fail(b,'CLEANUP_ENVIRONMENT_BUSY')
        yield


def stat_entry(rootfd, entry):
    parts=entry['parts']
    if not parts: return os.fstat(rootfd)
    parent=os.dup(rootfd)
    try:
        for part in parts[:-1]:
            if not isinstance(part,str) or part in ('','.','..') or '/' in part or '\x00' in part: raise OSError('invalid component')
            new=os.open(part,os.O_RDONLY|os.O_DIRECTORY|os.O_NOFOLLOW,dir_fd=parent);os.close(parent);parent=new
        name=parts[-1]
        if not isinstance(name,str) or name in ('','.','..') or '/' in name or '\x00' in name: raise OSError('invalid component')
        return os.stat(name,dir_fd=parent,follow_symlinks=False)
    finally: os.close(parent)


def delete_group(group, request, b, totals):
    with ExitStack() as stack:
        if 'spool' in group:
            group = dict(group)
            for kind in ('files', 'dirs'):
                group[kind] = stack.enter_context(open_entries(request, b, group['spool'], kind))
        return delete_loaded_group(group, request, b, totals)


def delete_loaded_group(group, request, b, totals):
    root=safe_dir(group['root'])
    if group['kind']=='cache':
        allowed={(alias,str(path)) for alias,path,_ in cache_scopes(request)}
        if (group['scope'],str(root)) not in allowed: fail(b,'CLEANUP_PLAN_INVALID')
    elif group['kind']=='reports':
        if root != Path(request['files']).resolve(strict=True)/'workspace-diagnostics': fail(b,'CLEANUP_PLAN_INVALID')
    elif group['kind'] in ('home-data','app-history'):
        roots=data_roots(request,b)
        if group.get('dataScope',group['scope']) not in roots or root != roots[group.get('dataScope',group['scope'])]: fail(b,'CLEANUP_PLAN_INVALID')
        top=group.get('top')
        if not isinstance(top,list) or not top or any(not valid_name(n) for n in top): fail(b,'CLEANUP_PLAN_INVALID')
        if any(not e['parts'] or e['parts'][0] not in top for e in group['files']): fail(b,'CLEANUP_PLAN_INVALID')
        if any(e['parts'] and e['parts'][0] not in top for e in group['dirs']): fail(b,'CLEANUP_PLAN_INVALID')
    elif group['kind'] in ('backup','partial'):
        if not (backup_owned(root,request,b) if group['kind']=='backup' else partial_owned(root,request,b)): fail(b,'CLEANUP_CHANGED_RESCAN')
    else: fail(b,'CLEANUP_PLAN_INVALID')
    fd=open_dir(root)
    try:
        # All selected files and directories must still match before the first unlink.
        for entry in chain(group['dirs'], group['files']):
            actual=identity(stat_entry(fd,entry));expected=entry['identity']
            if group['kind']=='cache' and entry in group['dirs']:
                actual,expected=actual[:2],expected[:2]
            if actual != expected: fail(b,'CLEANUP_CHANGED_RESCAN')
        for entry in group['files']:
            parts=entry['parts'];parent=os.dup(fd)
            try:
                for part in parts[:-1]:
                    new=os.open(part,os.O_RDONLY|os.O_DIRECTORY|os.O_NOFOLLOW,dir_fd=parent);os.close(parent);parent=new
                st=os.stat(parts[-1],dir_fd=parent,follow_symlinks=False)
                if not (stat.S_ISLNK(st.st_mode) if entry.get('link') and group['kind'] in ('home-data','app-history') else stat.S_ISREG(st.st_mode)) or identity(st)!=entry['identity']: fail(b,'CLEANUP_CHANGED_RESCAN')
                os.unlink(parts[-1],dir_fd=parent)
                totals['files']+=1;totals['bytes']+=st.st_size
                b.progress('CLEANUP_DELETE',totals['files'],totals['totalFiles'],current=parts[-1],unit='files')
            finally: os.close(parent)
        if group['removeRoot'] or group['kind'] in ('home-data','app-history'):
            for entry in reversed(group['dirs']):
                parts=entry['parts']
                st=stat_entry(fd,entry)
                if not stat.S_ISDIR(st.st_mode) or identity(st)[:2]!=entry['identity'][:2]: fail(b,'CLEANUP_CHANGED_RESCAN')
                if parts:
                    parent=os.dup(fd)
                    try:
                        for part in parts[:-1]:
                            new=os.open(part,os.O_RDONLY|os.O_DIRECTORY|os.O_NOFOLLOW,dir_fd=parent);os.close(parent);parent=new
                        os.rmdir(parts[-1],dir_fd=parent)
                    finally: os.close(parent)
            if not group['removeRoot']: return
            parent=open_dir(root.parent)
            try:
                if identity(os.stat(root.name,dir_fd=parent,follow_symlinks=False))[:2]!=group['dirs'][0]['identity'][:2]: fail(b,'CLEANUP_CHANGED_RESCAN')
                os.rmdir(root.name,dir_fd=parent)
            finally: os.close(parent)
    finally: os.close(fd)


def execute(request,b):
    if request.get('acknowledged') is not True: fail(b,'CLEANUP_CONSENT_REQUIRED')
    selected=request.get('selected')
    if not isinstance(selected,list) or not selected or any(not isinstance(v,str) for v in selected) or len(selected)!=len(set(selected)): fail(b,'CLEANUP_SELECTION_REQUIRED')
    plan=read_plan(request,b);groups={g['id']:g for g in plan['groups']}
    if any(not isinstance(v,str) or v not in groups for v in selected): fail(b,'CLEANUP_PLAN_INVALID')
    totals=dict(files=0,bytes=0);errors=[];removed=[];removed_envs=[]
    env_aliases={groups[k]['scope'] for k in selected if groups[k]['kind']=='environment'}
    for k in selected:
        g=groups[k]
        if g['kind']!='environment' and g['scope'].split('/')[0] in env_aliases: fail(b,'CLEANUP_OVERLAPPING_SELECTION')
    totals['totalFiles']=sum(groups[k].get('spool', {}).get('files', {}).get('count', len(groups[k]['files'])) for k in selected)
    for key in selected:
        group=groups[key]
        try:
            if group['kind']=='environment':
                remove_environment(group,request,b)
                removed_envs.append(group['scope'])
            else:
                with cache_lock(request,group['scope'].split('/')[0],b) if group['scope'] else ExitStack():
                    delete_group(group,request,b,totals)
            removed.append(key)
        except (OSError,b.BackupError) as error:
            if isinstance(error,b.BackupError) and str(error)=='CANCELLED': raise
            errors.append(dict(id=key,code=str(error) if isinstance(error,b.BackupError) else 'CLEANUP_CHANGED_RESCAN'))
    fd=private_fd(request,b)
    try:
        os.unlink(request['plan']+'.json',dir_fd=fd)
        for group in groups.values():
            if 'spool' in group:
                for kind in ('files', 'dirs'):
                    name = group['spool'][kind]['name']
                    if re.fullmatch(r'[a-f0-9]{32}\.(?:files|dirs)', name):
                        try: os.unlink(name, dir_fd=fd)
                        except FileNotFoundError: pass
    finally: os.close(fd)
    return dict(**totals,removed=removed,removedEnvironments=removed_envs,errors=errors)


def environment_roots(request,b):
    prefix=Path(request['prefix']).resolve(strict=True)
    base=prefix/'var/lib/proot-distro/containers'
    result={}
    if not base.is_dir(): return result
    safe_dir(base)
    for env in b.environments(prefix):
        alias=env['name'];root=Path(env['root'])
        if not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_.-]{0,63}',alias): continue
        expected=base/alias/'rootfs'
        if root!=expected: continue
        try: result[alias]=safe_dir(root)
        except OSError: pass
    return result


def remove_environment(group,request,b):
    alias=group['scope'];roots=environment_roots(request,b)
    if alias not in roots or str(roots[alias])!=group['root']: fail(b,'CLEANUP_CHANGED_RESCAN')
    root=roots[alias]
    if identity(root.lstat())[:2]!=group.get('rootIdentity') or identity(root.parent.lstat())[:2]!=group.get('containerIdentity'): fail(b,'CLEANUP_CHANGED_RESCAN')
    prefix=Path(request['prefix']).resolve(strict=True)
    if not b.locked_remove_available(prefix): fail(b,'RESTORED_LOCKS_REQUIRED')
    # The native command owns its exclusive nonblocking lock. Do not take a
    # competing lock here, pass --force, stop sessions or use a filesystem fallback.
    try: b.run_tool([str(prefix/'bin/proot-distro'),'remove',alias])
    except b.BackupError: fail(b,'CLEANUP_ENVIRONMENT_REMOVE_FAILED')
    if root.parent.exists() or root.parent.is_symlink(): fail(b,'CLEANUP_ENVIRONMENT_REMOVE_FAILED')


def valid_name(name):
    return isinstance(name,str) and name not in ('','.','..') and '/' not in name and '\x00' not in name


def home_roots(request,b):
    roots={'termux':safe_dir(Path(request['home']).resolve(strict=True))}
    for env in b.environments(Path(request['prefix']).resolve(strict=True)):
        for guest_home in ('root','home'):
            try: roots[env['name']+('/home' if guest_home=='home' else '')]=safe_dir(Path(env['root'])/guest_home)
            except OSError: pass
    return roots


def data_roots(request,b):
    roots=home_roots(request,b)
    path=Path(request['files']).resolve(strict=True)/'workspace-sessions'
    if path.is_dir(): roots['app-history']=safe_dir(path)
    return roots


def measure(path,b):
    """No-follow size estimate. Unreadable subtrees are explicitly marked."""
    total=count=errors=0;pending=[path]
    while pending:
        item=pending.pop()
        try:
            st=item.lstat()
            if stat.S_ISDIR(st.st_mode):
                # Reopen without following a concurrently replaced directory.
                fd=open_dir(item)
                try:
                    with os.scandir(fd) as entries:
                        for e in entries: pending.append(item/e.name)
                finally: os.close(fd)
            elif stat.S_ISREG(st.st_mode) or stat.S_ISLNK(st.st_mode): total+=st.st_size;count+=1
            b.progress('CLEANUP_SCAN',count,current=item.name,unit='files')
        except OSError: errors+=1
    return dict(bytes=total,count=count,unreadable=errors)


def overview(request,b):
    result=[]
    for scope,root in data_roots(request,b).items():
        entries=[]
        for child in sorted(root.iterdir()):
            try:
                st=child.lstat();value=measure(child,b)
                entries.append(dict(name=child.name,identity=identity(st),link=stat.S_ISLNK(st.st_mode),**value))
            except OSError: entries.append(dict(name=child.name,bytes=0,count=0,unreadable=1))
        result.append(dict(kind='history' if scope=='app-history' else 'home',scope=scope,path=str(root),entries=entries,bytes=sum(e['bytes'] for e in entries)))
    prefix=Path(request['prefix']).resolve(strict=True)
    systems=[('termux',prefix)]+[(e['name'],Path(e['root'])) for e in b.environments(prefix)]
    for scope,root in systems:
        entries=[]
        for child in sorted(root.iterdir()):
            if scope!='termux' and child.name in ('root','home'): continue
            if scope=='termux' and child.name=='var' and child.is_dir() and not child.is_symlink():
                # The distribution store is listed separately, not twice in PREFIX.
                for sub in child.iterdir():
                    if sub.name=='lib' and sub.is_dir() and not sub.is_symlink():
                        for leaf in sub.iterdir():
                            if leaf.name!='proot-distro': entries.append(dict(name='var/lib/'+leaf.name,**measure(leaf,b)))
                    else: entries.append(dict(name='var/'+sub.name,**measure(sub,b)))
            else: entries.append(dict(name=child.name,**measure(child,b)))
        result.append(dict(kind='system',scope=scope,path=str(root),entries=entries,bytes=sum(e['bytes'] for e in entries)))
    return result


def prepare_home(request,b):
    scope=request.get('scope');roots=data_roots(request,b)
    if not isinstance(scope,str) or scope not in roots: fail(b,'CLEANUP_PLAN_INVALID')
    selected=request.get('entries')
    if not isinstance(selected,list) or not selected: fail(b,'CLEANUP_SELECTION_REQUIRED')
    names=[];root=roots[scope]
    for e in selected:
        if not isinstance(e,dict) or not valid_name(e.get('name')) or e['name'] in names: fail(b,'CLEANUP_PLAN_INVALID')
        if identity((root/e['name']).lstat())!=e.get('identity'): fail(b,'CLEANUP_CHANGED_RESCAN')
        names.append(e['name'])
    # Stream the full snapshot to private files: HOME trees may contain millions
    # of entries. The small JSON plan retains only identities and counts.
    token = uuid.uuid4().hex
    fd = private_fd(request, b)
    saved = False
    try:
        with ExitStack() as stack:
            stores = [stack.enter_context(EntryWriter(fd, token+'.'+kind)) for kind in ('files', 'dirs')]
            snap=snapshot(root,lambda n,p:True,b,strict=True,top=names,links=True,stores=stores)
            spool = {kind: store.metadata() for kind,store in zip(('files','dirs'),stores)}
            count = len(stores[0])
            snap.update(files=[], dirs=[], spool=spool, id=uuid.uuid4().hex,kind='app-history' if scope=='app-history' else 'home-data',name=', '.join(names),scope='' if scope=='app-history' else scope,dataScope=scope,removeRoot=False,top=names)
            plan=dict(created=time.time(),prefix=str(Path(request['prefix']).resolve(strict=True)),filesRoot=str(Path(request['files']).resolve(strict=True)),groups=[snap])
            plan_token=write_plan(request,b,plan)
            saved = True
        return dict(plan=plan_token,groups=[dict(busyScopes=['history:'+n for n in names] if scope=='app-history' else [],**{k:snap[k] for k in ('id','kind','name','scope','bytes')}|dict(count=count,path=snap['root']))])
    finally:
        if not saved:
            for kind in ('files','dirs'):
                try: os.unlink(token+'.'+kind, dir_fd=fd)
                except FileNotFoundError: pass
        os.close(fd)


def run(request,b):
    if request['operation']=='cleanup-prepare-home': return prepare_home(request,b)
    return inventory(request,b) if request['operation']=='cleanup-inventory' else execute(request,b)
