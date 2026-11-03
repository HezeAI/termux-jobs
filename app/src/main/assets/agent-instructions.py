#!/usr/bin/env python3
"""Scoped instruction-file editor. No authentication reads or process control."""
import contextlib
import fcntl
import hashlib
import json
import os
from pathlib import Path
import stat
import re
import shutil
import subprocess
import sys
import tempfile
import time
import uuid

LIMIT = 256 * 1024
NAMES = ('AGENTS.md', 'AGENTS.override.md')
PROJECT_NAMES = NAMES + ('LEARNING.md',)


class Refused(Exception):
    pass


def digest(data):
    return hashlib.sha256(data).hexdigest()


def read_file(path, limit=LIMIT):
    try:
        fd = os.open(path, os.O_RDONLY | os.O_NOFOLLOW | os.O_NONBLOCK)
    except FileNotFoundError:
        return None, None
    except OSError:
        raise Refused('UNSAFE_OR_UNREADABLE_FILE') from None
    with os.fdopen(fd, 'rb') as stream:
        info = os.fstat(stream.fileno())
        if not stat.S_ISREG(info.st_mode) or info.st_nlink != 1 or info.st_size > limit:
            raise Refused('UNSAFE_OR_LARGE_FILE')
        data = stream.read(limit + 1)
        if len(data) > limit:
            raise Refused('UNSAFE_OR_LARGE_FILE')
        after = os.fstat(stream.fileno())
        if (info.st_ino, info.st_size, info.st_mtime_ns) != (after.st_ino, after.st_size, after.st_mtime_ns):
            raise Refused('EDIT_CONFLICT')
    return data, info


def revision(data, info):
    if data is None:
        return 'absent'
    return ':'.join(map(str, (info.st_dev, info.st_ino, info.st_mtime_ns, info.st_mode, digest(data))))


def directory(path, create=False):
    path = Path(path)
    if path.is_symlink():
        raise Refused('UNSAFE_DIRECTORY')
    if create:
        path.mkdir(mode=0o700, exist_ok=True)
    if not path.is_dir():
        raise Refused('DIRECTORY_UNAVAILABLE')
    if create and (path.stat().st_mode & 0o077):
        raise Refused('PRIVATE_DIRECTORY_PERMISSIONS')
    return path


def atomic(path, data, mode=0o600):
    fd, name = tempfile.mkstemp(prefix='.instructions-', dir=path.parent)
    try:
        with os.fdopen(fd, 'wb') as stream:
            os.fchmod(stream.fileno(), mode)
            stream.write(data)
            stream.flush()
            os.fsync(stream.fileno())
        os.replace(name, path)
        descriptor = os.open(path.parent, os.O_RDONLY | os.O_DIRECTORY)
        try:
            os.fsync(descriptor)
        finally:
            os.close(descriptor)
    finally:
        if os.path.lexists(name):
            os.unlink(name)


class Editor:
    def __init__(self, template, home=None, environ=None):
        self.env = dict(os.environ if environ is None else environ)
        self.home = Path(home if home is not None else Path.home()).resolve()
        raw = self.env.get('CODEX_HOME')
        self.codex_home = Path(raw).expanduser().resolve() if raw else (self.home / '.codex').resolve()
        self.template, _ = read_file(Path(template))
        if self.template is None:
            raise Refused('TEMPLATE_UNAVAILABLE')
        self.template.decode('utf-8')
        self.template_id = digest(self.template)
        text = self.template.decode('utf-8')
        self.storage_addition = ''
        for title in ('## Хранение файлов и временных данных', '## File and temporary data storage'):
            start = text.find(title + '\n')
            if start >= 0:
                end = text.find('\n## ', start + 1)
                self.storage_addition = text[start:] if end < 0 else text[start:end + 1]
                break

    def location(self, request):
        scope = request.get('scope', 'general')
        if scope == 'general':
            override, _ = read_file(self.codex_home / NAMES[1])
            name = NAMES[1] if override and override.strip() else NAMES[0]
            return self.codex_home / name
        if scope != 'project' or request.get('name') not in PROJECT_NAMES:
            raise Refused('INVALID_SCOPE')
        raw = request.get('project', '')
        if not isinstance(raw, str) or not raw.startswith('/') or '\0' in raw:
            raise Refused('INVALID_PROJECT')
        project = Path(raw).resolve(strict=True)
        directory(project)
        if request['name'] == 'LEARNING.md':
            explicit = request.get('memoryPath', '')
            if explicit:
                if not isinstance(explicit, str) or not explicit.startswith('/') or '\0' in explicit or Path(explicit).name != 'LEARNING.md':
                    raise Refused('INVALID_MEMORY_PATH')
                parent = Path(explicit).parent.resolve(strict=True)
                directory(parent)
                return parent / 'LEARNING.md'
            # Only inspect literal Markdown paths. Never execute project instructions.
            # An explicit reference prevents silently creating a second memory file.
            candidates = self.memory_references(project)
            if len(candidates) > 1:
                raise Refused('MEMORY_PATH_REQUIRED')
            if candidates:
                candidate = Path(candidates[0])
                directory(candidate.parent)
                return candidate.parent.resolve(strict=True) / candidate.name
        return project / request['name']

    def memory_references(self, project):
        for folder in (project, *project.parents):
            references = set()
            for name in (NAMES[1], NAMES[0]):
                data, _ = read_file(folder / name)
                if not data:
                    continue
                text = data.decode('utf-8')
                # Recognize literal absolute paths and backticked relative paths.
                # This does not evaluate commands or interpret arbitrary prose.
                paths = re.findall(r'`([^`\n\r\0]*LEARNING\.md)`', text)
                paths += re.findall(r"(?<![\w])(/[^\s`<>\"')]*?/LEARNING\.md)", text)
                for raw in paths:
                    if '/' not in raw or '://' in raw or '\0' in raw:
                        continue
                    p = Path(raw)
                    if p.name == 'LEARNING.md':
                        references.add(str((p if p.is_absolute() else folder / p).absolute()))
            if references:
                return sorted(references)
            if (folder / '.git').exists():
                break
        return []

    def view(self, request):
        path = self.location(request)
        data, info = read_file(path)
        project_folder = Path(request['project']).resolve(strict=True) if request.get('scope') == 'project' else path.parent
        override, _ = read_file(project_folder / NAMES[1])
        masked = str(path.parent / NAMES[1]) if path.name == NAMES[0] and override and override.strip() else None
        project_source = None
        if request.get('scope') == 'project':
            base, _ = read_file(project_folder / NAMES[0])
            if override and override.strip():
                project_source = str(project_folder / NAMES[1])
            elif base and base.strip():
                project_source = str(project_folder / NAMES[0])
        return {'path': str(path), 'exists': data is not None, 'maskedBy': masked,
                'projectSource': project_source,
                'projectBaseExists': base is not None if request.get('scope') == 'project' else None,
                'active': path.name in NAMES and bool(data and data.strip()) and not masked,
                'text': '' if data is None else data.decode('utf-8'),
                'revision': revision(data, info), 'template': self.template.decode('utf-8'),
                'templateId': self.template_id, 'templateAvailable': data != self.template,
                'storageAddition': self.storage_addition,
                'storageAdditionAvailable': bool(self.storage_addition) and not re.search(r'^## (?:Хранение файлов и временных данных|File and temporary data storage)[ \t]*\r?$', data.decode('utf-8') if data else '', re.M) and self.storage_addition not in (data.decode('utf-8') if data else ''),
                'codexHome': str(self.codex_home), 'homeExplicit': bool(self.env.get('CODEX_HOME'))}

    def storage(self):
        if not self.codex_home.exists():
            self.codex_home.mkdir(mode=0o700, parents=True)
        directory(self.codex_home)
        store = directory(self.codex_home / '.termux-jobs-instructions', create=True)
        directory(store / 'history', create=True)
        return store

    @contextlib.contextmanager
    def locked(self):
        store = self.storage()
        try:
            fd = os.open(store / 'edit.lock', os.O_WRONLY | os.O_CREAT | os.O_NOFOLLOW, 0o600)
        except OSError:
            raise Refused('UNSAFE_LOCK') from None
        try:
            if not stat.S_ISREG(os.fstat(fd).st_mode) or os.fstat(fd).st_nlink != 1:
                raise Refused('UNSAFE_LOCK')
            fcntl.flock(fd, fcntl.LOCK_EX)
            yield store
        finally:
            os.close(fd)

    def initialize(self):
        # Existing even-empty user files and overrides must never be replaced.
        with self.locked() as store:
            if any(os.path.lexists(self.codex_home / n) for n in NAMES):
                return {'created': False}
            path = self.codex_home / NAMES[0]
            # Publish without replacement, even if an external editor raced us.
            fd, name = tempfile.mkstemp(prefix='.instructions-', dir=self.codex_home)
            try:
                with os.fdopen(fd, 'wb') as stream:
                    os.fchmod(stream.fileno(), 0o600)
                    stream.write(self.template)
                    stream.flush()
                    os.fsync(stream.fileno())
                if any(os.path.lexists(self.codex_home / n) for n in NAMES):
                    return {'created': False}
                try:
                    os.link(name, path, follow_symlinks=False)
                except FileExistsError:
                    return {'created': False}
            finally:
                os.unlink(name)
            return {'created': True, 'path': str(path)}

    def save(self, request):
        text = request.get('text')
        if not isinstance(text, str) or len(text.encode('utf-8')) > LIMIT or '\0' in text:
            raise Refused('TEXT_TOO_LARGE_OR_INVALID')
        with self.locked() as store:
            path = self.location(request)
            if request.get('path') != str(path):
                raise Refused('SOURCE_CHANGED')
            old, info = read_file(path)
            if revision(old, info) != request.get('revision'):
                raise Refused('EDIT_CONFLICT')
            data = text.encode('utf-8')
            if old == data:
                return self.view(request)
            if old is not None:
                record = {'path': str(path), 'time': time.time(), 'text': old.decode('utf-8'),
                          'sha256': digest(old)}
                atomic(store / 'history' / (uuid.uuid4().hex + '.json'),
                       json.dumps(record, ensure_ascii=False).encode('utf-8'))
            # Recheck external edits after writing the history, immediately before publish.
            current, current_info = read_file(path)
            if revision(current, current_info) != request['revision']:
                raise Refused('EDIT_CONFLICT')
            atomic(path, data, stat.S_IMODE(info.st_mode) if info else 0o600)
            return self.view(request)

    def history(self, request):
        path = str(self.location(request))
        folder = self.codex_home / '.termux-jobs-instructions' / 'history'
        if not folder.exists():
            return {'history': []}
        directory(folder.parent)
        directory(folder)
        result = []
        for p in folder.glob('*.json'):
            if not p.stem.isalnum() or len(p.stem) != 32:
                continue
            data, _ = read_file(p, LIMIT * 8)
            record = json.loads(data)
            if record.get('path') == path:
                result.append({'id': p.stem, 'time': record['time'], 'sha256': record['sha256']})
        return {'history': sorted(result, key=lambda x: x['time'], reverse=True)[:100]}

    def previous(self, request):
        key = request.get('id', '')
        if len(key) != 32 or any(c not in '0123456789abcdef' for c in key):
            raise Refused('INVALID_HISTORY')
        folder = self.codex_home / '.termux-jobs-instructions' / 'history'
        directory(folder.parent)
        directory(folder)
        data, _ = read_file(folder / (key + '.json'), LIMIT * 8)
        if data is None:
            raise Refused('HISTORY_UNAVAILABLE')
        record = json.loads(data)
        if record.get('path') != str(self.location(request)) or digest(record['text'].encode('utf-8')) != record['sha256']:
            raise Refused('INVALID_HISTORY')
        return {'text': record['text']}

    def cli(self):
        executable = shutil.which('codex')
        result = {'path': executable, 'version': None, 'behaviorVerified': False,
                  'verifiedVersions': ['0.163.0', '0.164.0']}
        if not executable:
            return result
        try:
            probe = subprocess.run([executable, '--version'], stdin=subprocess.DEVNULL,
                                   stdout=subprocess.PIPE, stderr=subprocess.DEVNULL,
                                   text=True, timeout=5)
            match = re.fullmatch(r'codex-cli (\d+\.\d+\.\d+)\s*', probe.stdout)
            if probe.returncode == 0 and match:
                result['version'] = match[1]
                result['behaviorVerified'] = match[1] in result['verifiedVersions']
        except (OSError, subprocess.TimeoutExpired):
            pass
        return result

    def diagnostics(self, request):
        result = self.view(dict(request, scope='general'))
        project = request.get('project', '')
        cwd = Path(project).resolve(strict=True) if project else self.home
        directory(cwd)
        chain = [cwd]
        for parent in cwd.parents:
            if (chain[-1] / '.git').exists():
                break
            chain.append(parent)
        if not any((p / '.git').exists() for p in chain):
            chain = [cwd]
        chain.reverse()
        sources, configurations = [], []
        directories = [('global', self.codex_home)] + [('project', p) for p in chain]
        for scope, folder in directories:
            selected = False
            for name in (NAMES[1], NAMES[0]):
                p = folder / name
                data, _ = read_file(p)
                active = bool(data and data.strip()) and not selected
                if data is not None:
                    sources.append({'scope': scope, 'path': str(p), 'active': active,
                                    'bytes': len(data), 'sha256': digest(data)})
                selected |= active
        for path in [Path('/etc/codex/config.toml'), self.codex_home / 'config.toml'] + [p / '.codex/config.toml' for p in chain]:
            if not path.exists():
                continue
            if path.parent.is_symlink():
                configurations.append({'path': str(path), 'settings': [], 'parsed': False, 'reason': 'symlink-directory'})
                continue
            data, _ = read_file(path)
            try:
                try:
                    import tomllib
                except ImportError:
                    import tomli as tomllib
                conf = tomllib.loads(data.decode('utf-8'))
                flags = []
                def inspect(value, prefix=''):
                    if not isinstance(value, dict):
                        return
                    for key, v in value.items():
                        if key in ('developer_instructions', 'instructions', 'model_instructions_file',
                                   'project_doc_max_bytes', 'project_doc_fallback_filenames', 'profile'):
                            # Presence only. Never report arbitrary config strings or tokens.
                            flags.append(prefix + key)
                        elif isinstance(v, dict):
                            inspect(v, prefix + key + '.')
                inspect(conf)
                configurations.append({'path': str(path), 'settings': flags, 'parsed': True})
            except (ValueError, ImportError):
                configurations.append({'path': str(path), 'settings': [], 'parsed': False})
        # Auth files, provider credentials, history logs and instruction replacements are never opened.
        return {'codexHome': result['codexHome'], 'homeExplicit': result['homeExplicit'],
                'generalPath': result['path'], 'historyPath': str(self.codex_home / '.termux-jobs-instructions/history'),
                'cwd': str(cwd), 'sources': sources,
                'configurations': configurations, 'environment': 'ubuntu-ai',
                'mechanism': 'AGENTS files; not developerInstructions',
                'cli': self.cli(),
                'application': 'restart-and-resume to reread all rules; running request unchanged; live reread is version-dependent'}

    def call(self, request):
        operation = request.get('operation', 'read')
        if operation == 'read':
            return self.view(request)
        if operation == 'save':
            return self.save(request)
        if operation == 'history':
            return self.history(request)
        if operation == 'previous':
            return self.previous(request)
        if operation == 'diagnostics':
            return self.diagnostics(request)
        if operation == 'init':
            return self.initialize()
        raise Refused('INVALID_OPERATION')



class ClaudeEditor(Editor):
    """Same atomic/history engine, independent Claude file semantics."""
    def __init__(self, template, home=None, environ=None):
        super().__init__(template, home, environ)
        self.claude_home = Path(self.env.get('CLAUDE_CONFIG_DIR', str(self.home / '.claude'))).expanduser().resolve()
        self.codex_home = self.claude_home  # storage engine root, not Codex configuration

    def location(self, request):
        if request.get('scope', 'general') == 'general':
            return self.claude_home / 'CLAUDE.md'
        if request.get('scope') != 'project' or request.get('name') not in ('CLAUDE.md', 'CLAUDE.local.md', '.claude/CLAUDE.md', 'LEARNING.md'):
            raise Refused('INVALID_SCOPE')
        raw = request.get('project', '')
        if not isinstance(raw, str) or not raw.startswith('/') or '\0' in raw:
            raise Refused('INVALID_PROJECT')
        project = Path(raw).resolve(strict=True)
        directory(project)
        if request['name'] == 'LEARNING.md':
            return super().location(request)
        if request['name'].startswith('.claude/'):
            folder = project / '.claude'
            if folder.exists():
                directory(folder)
            elif folder.is_symlink():
                raise Refused('UNSAFE_DIRECTORY')
        return project / request['name']

    def memory_references(self, project):
        # One shared memory file: also inspect Codex rules, never execute them.
        for folder in (project, *project.parents):
            refs = set(super().memory_references(folder))
            for name in ('CLAUDE.md', 'CLAUDE.local.md', '.claude/CLAUDE.md'):
                data, _ = read_file(folder / name)
                if data:
                    text = data.decode('utf-8')
                    paths = re.findall(r'`([^`\n\r\0]*LEARNING\.md)`', text)
                    paths += re.findall(r'(?<![\w])(/[^\s`<>\"\')]*?/LEARNING\.md)', text)
                    paths += re.findall(r'@([^\s`<>\"\']*LEARNING\.md)', text)
                    for raw in paths:
                        if '/' in raw and '://' not in raw:
                            candidate = Path(raw)
                            if candidate.name == 'LEARNING.md':
                                refs.add(str((candidate if candidate.is_absolute() else folder / candidate).resolve()))
            if refs:
                return sorted(refs)
            if (folder / '.git').exists():
                break
        return []

    def view(self, request):
        path = self.location(request)
        data, info = read_file(path)
        return {'path': str(path), 'exists': data is not None, 'maskedBy': None,
                'projectSource': None,
                'projectBaseExists': os.path.lexists(Path(request['project']) / 'CLAUDE.md') if request.get('scope') == 'project' else None,
                'active': None, 'contentPresent': bool(data and data.strip()),
                'text': '' if data is None else data.decode('utf-8'), 'revision': revision(data, info),
                'template': self.template.decode('utf-8'), 'templateId': self.template_id,
                'templateAvailable': data != self.template, 'storageAddition': self.storage_addition,
                'claudeHome': str(self.claude_home), 'agent': 'claude', 'homeExplicit': bool(self.env.get('CLAUDE_CONFIG_DIR'))}

    def initialize(self):
        # Claude adoption is always explicit; never create instructions at chat launch.
        return {'created': False}

    def save(self, request):
        path = self.location(request)
        if request.get('path') != str(path):
            raise Refused('SOURCE_CHANGED')
        if path.parent.name == '.claude' and request.get('scope') == 'project' and not path.parent.exists():
            if request.get('revision') != 'absent':
                raise Refused('EDIT_CONFLICT')
            path.parent.mkdir(mode=0o700)
        return super().save(request)

    def cli(self):
        pinned = self.home / '.local/share/termux-jobs/claude-code/2.1.296/claude'
        executable = str(pinned) if pinned.is_file() else shutil.which('claude')
        result = {'path': executable, 'version': None, 'behaviorVerified': False, 'verifiedVersions': ['2.1.296']}
        if executable:
            try:
                probe = subprocess.run([executable, '--version'], stdin=subprocess.DEVNULL,
                                       stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, text=True, timeout=5)
                match = re.fullmatch(r'(\d+\.\d+\.\d+) \(Claude Code\)\s*', probe.stdout)
                if probe.returncode == 0 and match:
                    result.update(version=match[1], behaviorVerified=match[1] == '2.1.296')
            except (OSError, subprocess.TimeoutExpired):
                pass
        return result

    def diagnostics(self, request):
        cwd = Path(request.get('project', str(self.home))).resolve(strict=True)
        directory(cwd)
        sources = []
        def record(scope, path, conditional=False):
            try:
                data, _ = read_file(path)
                if data is not None:
                    sources.append({'scope': scope, 'path': str(path), 'bytes': len(data),
                                    'sha256': digest(data), 'conditional': conditional})
            except Refused:
                sources.append({'scope': scope, 'path': str(path), 'readable': False})
        record('managed', Path('/etc/claude-code/CLAUDE.md'))
        record('global', self.claude_home / 'CLAUDE.md')
        for folder in [*reversed(cwd.parents), cwd]:
            for name in ('CLAUDE.md', '.claude/CLAUDE.md', 'CLAUDE.local.md'):
                record('project', folder / name)
            for name in ('AGENTS.md', '.claude/AGENTS.md'):
                record('project-fallback-version-and-settings-dependent', folder / name, True)
            rules = folder / '.claude/rules'
            if rules.is_dir() and not rules.is_symlink():
                # Directory presence, not arbitrary file imports or authorization.
                sources.append({'scope': 'rules', 'path': str(rules), 'conditional': True})
        if (self.claude_home / 'rules').is_dir():
            sources.append({'scope': 'global-rules', 'path': str(self.claude_home / 'rules'), 'conditional': True})
        configurations = []
        for path in [self.claude_home / 'settings.json', cwd / '.claude/settings.json', cwd / '.claude/settings.local.json']:
            if path.is_file():
                mode = None
                exclusions = False
                try:
                    raw, _ = read_file(path)
                    settings = json.loads(raw)
                    exclusions = 'claudeMdExcludes' in settings
                    plugins = settings.get('pluginConfigs', {})
                    for plugin in ('cc-plugin-agents-md@builtin', 'agents-md@builtin'):
                        candidate = plugins.get(plugin, {}).get('options', {}).get('instructionFiles')
                        if candidate in ('claude-md-or-agents-md', 'claude-md-and-agents-md', 'claude-md', 'managed-only'):
                            mode = candidate
                            break
                except (Refused, ValueError, TypeError, AttributeError):
                    pass
                configurations.append({'path': str(path), 'present': True, 'instructionFiles': mode,
                                       'claudeMdExcludesPresent': exclusions, 'arbitraryValuesReported': False})
        return {'agent': 'claude', 'claudeHome': str(self.claude_home), 'homeExplicit': bool(self.env.get('CLAUDE_CONFIG_DIR')),
                'generalPath': str(self.claude_home / 'CLAUDE.md'), 'cwd': str(cwd), 'sources': sources,
                'configurations': configurations, 'cli': self.cli(),
                'mechanism': 'Claude CLAUDE.md/local/rules cumulative; AGENTS fallback conditional; no AGENTS.override.md',
                'limits': ['file candidates for standard startup, not an introspection of a running process',
                           'imports and conditional rules are not expanded; settings, managed policy, CLI arguments and custom launchers can alter loading'],
                'application': 'restart-and-resume; running request unchanged; version-specific native test required'}


def main():
    try:
        if len(sys.argv) != 2:
            raise Refused('ARGUMENTS')
        request = json.loads(sys.stdin.buffer.read(LIMIT * 8 + 1))
        agent = request.get('agent', 'codex')
        if agent not in ('codex', 'claude'):
            raise Refused('INVALID_AGENT')
        if request.get('kind') == 'skills':
            import importlib.util
            spec = importlib.util.spec_from_file_location('agent_skills', Path(__file__).with_name('agent-skills.py'))
            module = importlib.util.module_from_spec(spec)
            spec.loader.exec_module(module)
            try:
                result = module.call(request)
            except module.SkillError as error:
                raise Refused(str(error)) from None
        else:
            result = (ClaudeEditor if agent == 'claude' else Editor)(sys.argv[1]).call(request)
        if isinstance(result, dict):
            result['environment'] = request.get('environment', 'ubuntu-ai')
        print(json.dumps({'ok': True, 'result': result}, ensure_ascii=False))
    except Exception as error:
        # Paths/content/configuration/credentials never enter diagnostic exception text.
        code = str(error) if isinstance(error, Refused) else 'INSTRUCTIONS_UNAVAILABLE'
        print(json.dumps({'ok': False, 'error': code}))
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main())
