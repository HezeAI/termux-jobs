"""Validate saved synthetic size-sweep evidence; no network or real log/auth reads."""
import argparse
import hashlib
import json
from pathlib import Path

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('run', type=Path)
args = parser.parse_args()
root = args.run.resolve()
source = root / 'result.json'
result = json.loads(source.read_text())
summary = json.loads((root / 'size-sweep-summary.json').read_text())
assert result['scope'].startswith('Repeated cold size sweep')
assert summary['sourceResultSha256'] == hashlib.sha256(source.read_bytes()).hexdigest(), 'Stale safe snapshot'
assert result['completed'] and len(result['trialRows']) == 15
assert result['allOwnedNativeChildrenExited'] and result['originalsUnchanged']
assert json.loads((root / 'owned-process-observation.json').read_text())['allObservedDescendantsExited']
assert {k: len(v) for k, v in result['initialHistories'].items()} == {'10': 3, '20': 5, '25': 6, '30': 7, '35': 8}
for item in json.loads((root / 'dataset.json').read_text()):
    path = (root / 'project' / item['file']).resolve()
    assert path.is_relative_to(root / 'project')
    assert hashlib.sha256(path.read_bytes()).hexdigest() == item['sha256']
rows = []
for group in summary['groups']:
    target = group['targetDecimalMB']
    trials = [t for t in summary['trials'] if t['targetDecimalMB'] == target]
    assert len(trials) == 3 and {t['repetition'] for t in trials} == {1, 2, 3}
    assert all(t['ownedNativeExited'] for t in trials)
    for trial in trials:
        if trial['completed']:
            assert trial['answerMatches'] and not trial.get('nativeCompactionSeen') and not trial.get('error')
        else:
            assert trial['error'] == 'SYNTHETIC_240_SECOND_BENCHMARK' and trial['seconds'] >= 240
        assert trial['observedImageCounts'] == [trial['expectedImageCount']]
        assert abs(trial['fullRequestJsonBytes'] / 1_000_000 - target) < 1
    rows.append({'targetDecimalMB': target, 'completedWithin240Seconds': sum(t['completed'] for t in trials), 'seconds': [t['seconds'] for t in trials], 'retryNotifications': [t['retryEvents'] for t in trials], 'httpFallbacksObserved': sum(bool(t['safeLogClassifications'].get('HTTP_FALLBACK')) for t in trials)})
print(json.dumps({'scope': 'Validated recorded JSON sizes and safe synthetic metadata, not TLS bytes/provider receipts or phone acceptance', 'groups': rows}))
