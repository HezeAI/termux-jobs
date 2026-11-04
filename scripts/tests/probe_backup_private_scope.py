#!/usr/bin/env python3
"""Manual audit: synthetic app-private connection state versus native backup scope.
Keeps evidence in the requested fresh .verification directory; no real auth/CLI.
"""
import hashlib
import importlib.util
import json
from pathlib import Path
import sys
import tarfile

sys.dont_write_bytecode = True
ROOT = Path(__file__).resolve().parents[2]
if len(sys.argv) != 2:
    raise SystemExit('usage: probe_backup_private_scope.py FRESH_VERIFICATION_DIRECTORY')
base = Path(sys.argv[1]).resolve()
assert ROOT / '.verification' in base.parents
base.mkdir(mode=0o700, parents=True, exist_ok=False)
spec = importlib.util.spec_from_file_location('backup_scope', ROOT / 'app/src/main/assets/workspace-backup.py')
b = importlib.util.module_from_spec(spec)
spec.loader.exec_module(b)
files, output, work = base / 'files', base / 'output', base / 'work'
home, prefix = files / 'home', files / 'usr'
for p in (home, prefix, output, work):
    p.mkdir(mode=0o700, parents=True, exist_ok=True)
connection = '11111111-1111-4111-8111-111111111111'
markers = {
    home / '.codex/auth.json': 'SYNTHETIC_CODEX_SECRET',
    home / '.claude/.credentials.json': 'SYNTHETIC_CLAUDE_SECRET',
    home / '.gemini/oauth_creds.json': 'SYNTHETIC_GEMINI_SECRET',
    home / '.ssh/id_ed25519': 'SYNTHETIC_SSH_SECRET',
    home / 'project/.env': 'SYNTHETIC_PROJECT_SECRET',
    files / 'workspace-sessions/fake/history.txt': 'SYNTHETIC_TERMINAL_HISTORY',
    files / '.ai-control/connections' / connection / 'config/opencode/opencode.json':
        json.dumps({'providers': {'audit': {'settings': {'apiKey': 'SYNTHETIC_CONNECTION_SECRET'}}}}),
    files / '.ai-control/connections' / connection / 'data/probe-only.txt': 'SYNTHETIC_PRIVATE_STATE',
}
for p, value in markers.items():
    p.parent.mkdir(mode=0o700, parents=True, exist_ok=True)
    p.write_text(value)
    p.chmod(0o600)
profile = dict(id=connection, name='Synthetic audit only', environment='ubuntu',
               codexEnvironment='ubuntu-ai', aiAgent='opencode', aiConnection=connection)
workspace = dict(version=8, sessions=[profile], commands=[], notes=[], recentCommands=[])
req = dict(home=str(home), prefix=str(prefix), files=str(files), packageId='com.termux',
           appVersion='2.1.3-test5-audit', work=str(work), destination=str(output),
           environments=[], workspace=workspace, settings={})
results = {}
for mode in ('home-prefix-library', 'all-termux-selections-library'):
    options = dict(req, components=['library', 'home', 'prefix'])
    if mode.startswith('all-termux'):
        scope = b.selection_scopes(req)[0]
        options.update(components=['library'], selections=[dict(scope='termux',
            items=[item['id'] for item in b.selection_layout(scope)])])
    created = b.create(options)
    manifest = Path(created['path']) / 'manifest.json'
    report = b.inspect(dict(req, path=str(manifest)))
    assert report['workspace']['sessions'][0]['aiConnection'] == connection
    members = {}
    for archive in manifest.parent.glob('*.tar.gz'):
        with tarfile.open(archive) as tar:
            for member in tar.getmembers():
                if member.isfile():
                    members[member.name] = tar.extractfile(member).read()
    def archived(value):
        return any(value.encode() in data for data in members.values())
    for p, value in markers.items():
        assert archived(value) == ('.ai-control' not in p.parts), str(p)
    indices = [i for i, c in enumerate(report['components']) if c['kind'] in ('home', 'data', 'app-data')]
    restore_home = base / ('restore-' + mode)
    restore_home.mkdir(mode=0o700)
    restored = b.restore(dict(req, home=str(restore_home), path=str(manifest),
                              fingerprints=report['fingerprints'], selected=indices,
                              destination=str(restore_home)))
    restored_files = [p for p in Path(restored['path']).rglob('*') if p.is_file()]
    for p, value in markers.items():
        assert p.read_text() == value  # original source is never merged/replaced
        found = any(value.encode() in f.read_bytes() for f in restored_files)
        assert found == ('.ai-control' not in p.parts), str(p)
    results[mode] = dict(profileConnectionReferenceRetained=True,
        homeAndTerminalMarkersArchivedAndRestored=True, sourcePreserved=True,
        privateConnectionMarkersArchived=False, privateConnectionMarkersRestored=False,
        archiveMembers=sorted(members), archive=str(manifest), restore=str(restored['path']))
result = dict(artificialDataOnly=True, realEnvironmentsOrSessionsUsed=False,
              noSecretValuesInReport=True, noAndroidKeystoreOrSAFTest=True,
              helperSha256=hashlib.sha256((ROOT / 'app/src/main/assets/workspace-backup.py').read_bytes()).hexdigest(),
              results=results)
(base / 'result.json').write_text(json.dumps(result, indent=2) + '\n')
print('Confirmed: synthetic private OpenCode connection state omitted in both backup modes; profile reference retained.')
