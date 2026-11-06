"""Read-only, bounded metadata projection of native Claude/Gemini histories.

No CLI startup, authentication files, model requests, migrations or history writes.
Only UUID Claude JSONL and Gemini session-*.json[l] inside native history roots.
"""
import hashlib
import json
import os
import re
import stat
import sys

UUID = re.compile(r"^[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}$")
READ_BYTES = 65536
PAGE = 30


def prefix_fields(text):
    """Return complete top-level fields, stopping before an oversized value."""
    decoder = json.JSONDecoder()
    fields = {}
    pos = len(text) - len(text.lstrip())
    if text[pos:pos + 1] != '{':
        return fields
    pos += 1
    while pos < len(text):
        while pos < len(text) and text[pos] in ' \r\n\t,':
            pos += 1
        try:
            key, pos = decoder.raw_decode(text, pos)
            while pos < len(text) and text[pos].isspace():
                pos += 1
            if text[pos:pos + 1] != ':':
                break
            pos += 1
            while pos < len(text) and text[pos].isspace():
                pos += 1
            value, pos = decoder.raw_decode(text, pos)
        except (ValueError, IndexError):
            break
        if not isinstance(key, str):
            break
        fields[key] = value
    return fields


def snippets(file):
    fd = os.open(file, os.O_RDONLY | os.O_NOFOLLOW | os.O_NONBLOCK)
    try:
        info = os.fstat(fd)
        if not stat.S_ISREG(info.st_mode):
            raise ValueError('NOT_REGULAR')
        head = os.read(fd, READ_BYTES).decode('utf-8', 'replace')
        yield prefix_fields(head)
        for line in head.splitlines():
            try:
                value = json.loads(line)
                if isinstance(value, dict):
                    yield value
            except ValueError:
                continue
        if info.st_size > READ_BYTES:
            os.lseek(fd, max(READ_BYTES, info.st_size - READ_BYTES), os.SEEK_SET)
            tail = os.read(fd, READ_BYTES).decode('utf-8', 'replace')
            for line in tail.splitlines()[1:]:
                try:
                    value = json.loads(line)
                    if isinstance(value, dict):
                        yield value
                except ValueError:
                    continue
    finally:
        os.close(fd)


def short(value):
    return value[:240] if isinstance(value, str) else ''


def metadata(file, agent, project):
    identifier = os.path.basename(file).removesuffix('.jsonl') if agent == 'claude' else ''
    title = cwd = project_hash = ''
    excluded = False
    for record in snippets(file):
        if agent == 'claude':
            if record.get('sessionId') not in (None, identifier):
                continue
            if record.get('isSidechain') or record.get('teamName'):
                excluded = True
            cwd = short(record.get('cwd')) or cwd
            title = short(record.get('customTitle')) or short(record.get('summary')) or title
            if not title and record.get('type') == 'user':
                message = record.get('message', {})
                content = message.get('content', '') if isinstance(message, dict) else ''
                if isinstance(content, list):
                    content = next((x.get('text', '') for x in content if isinstance(x, dict) and x.get('type') == 'text'), '')
                title = short(content)
        else:
            values = record.get('$set', record)
            if not isinstance(values, dict):
                continue
            identifier = short(values.get('sessionId')) or identifier
            project_hash = short(values.get('projectHash')) or project_hash
            title = short(values.get('summary')) or short(values.get('firstUserMessage')) or title
            if not title and values.get('type') == 'user':
                content = values.get('content', '')
                if isinstance(content, list):
                    content = next((x.get('text', '') for x in content if isinstance(x, dict) and isinstance(x.get('text'), str)), '')
                title = short(content)
            excluded = excluded or values.get('kind') == 'subagent'
    if excluded:
        return None
    if not UUID.fullmatch(identifier):
        raise ValueError('HISTORY_METADATA_UNAVAILABLE')
    if agent == 'gemini':
        if project_hash != hashlib.sha256(project.encode()).hexdigest():
            return None
        cwd = project
    return {'id': identifier, 'name': title or identifier, 'directory': cwd}


def catalog(agent, project, cursor=''):
    if agent not in ('claude', 'gemini') or not os.path.isabs(project):
        raise ValueError('INVALID_CONTEXT')
    if agent == 'claude':
        root = os.path.join(os.environ.get('CLAUDE_CONFIG_DIR') or os.path.expanduser('~/.claude'), 'projects')
    else:
        home = os.environ.get('GEMINI_CLI_HOME') or os.path.expanduser('~')
        root = os.path.join(home, '.cache' if os.environ.get('SANDBOX') == 'sandbox-exec' else '', '.gemini', 'tmp')
    files = []
    try:
        with os.scandir(root) as folders:
            for folder in folders:
                if not folder.is_dir(follow_symlinks=False):
                    continue
                directory = folder.path if agent == 'claude' else os.path.join(folder.path, 'chats')
                if os.path.islink(directory):
                    raise ValueError('UNSAFE_HISTORY_PATH')
                try:
                    with os.scandir(directory) as items:
                        for item in items:
                            match = UUID.fullmatch(item.name[:-6]) and item.name.endswith('.jsonl') if agent == 'claude' else re.fullmatch(r'session-[A-Za-z0-9_.:-]+\.jsonl?', item.name)
                            if match and item.is_file(follow_symlinks=False):
                                files.append(item.path)
                            if len(files) > 10000:
                                raise ValueError('CATALOG_TOO_LARGE')
                except FileNotFoundError:
                    continue
    except FileNotFoundError:
        return {'sessions': [], 'cursor': ''}
    files.sort()
    fingerprint = hashlib.sha256(('\n'.join(files) + '\n' + project + '\n' + agent).encode()).hexdigest()[:24]
    offset = 0
    if cursor:
        stamp, value = cursor.split(':')
        offset = int(value)
        if stamp != fingerprint or not 0 <= offset <= len(files):
            raise ValueError('CATALOG_CHANGED_RETRY')
    rows = []
    for file in files[offset:offset + PAGE]:
        row = metadata(file, agent, project)
        if row:
            rows.append(row)
    end = offset + PAGE
    return {'sessions': rows, 'cursor': fingerprint + ':' + str(end) if end < len(files) else ''}


if __name__ == '__main__':
    try:
        print(json.dumps(catalog(*sys.argv[1:]), ensure_ascii=False))
    except Exception as error:
        print(json.dumps({'error': type(error).__name__ + ':' + str(error)[:80]}))
        sys.exit(1)
