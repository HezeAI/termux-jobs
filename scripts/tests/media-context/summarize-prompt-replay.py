"""Validate captured native requests; no CLI execution or account access."""
import hashlib
import json
import sys
from pathlib import Path

root = Path(sys.argv[1]).resolve()
rows = []
reference_images = None
for variant in ('none', 'project', 'ru', 'en'):
    run = Path((root / ('latest-' + variant + '-run.txt')).read_text().strip())
    raw = (run / 'result.json').read_bytes()
    result = json.loads(raw)
    assert result['promptVariant'] == variant
    cases = {case['case']: case for case in result['cases']}
    assert len(cases) == 8 and all(c['status'] == 'completed' for c in cases.values())
    assert len(result['requests']) == 10
    baseline = next(r for r in result['requests'] if r['case'] == 'two-images')
    images = [image['sha256'] for image in baseline['images']]
    assert len(images) == len(set(images)) == 2
    if reference_images is None:
        reference_images = images
    assert images == reference_images, 'A/B image bytes differ'
    for name in ('continue-no-view', 'reopen-no-view'):
        requests = [r for r in result['requests'] if r['case'] == name]
        assert len(requests) == 1
        assert [i['sha256'] for i in requests[0]['images']] == images
    for number in range(2):
        requests = [r for r in result['requests'] if r['case'] == 'view-image-repeat-' + str(number)]
        assert len(requests) == 2
        assert [i['sha256'] for i in requests[0]['images']] == images + [images[0]] * number
        assert [i['sha256'] for i in requests[1]['images']] == images + [images[0]] * (number + 1)
    for name in ('continue-after-view-no-view', 'reopen-after-view-no-view'):
        requests = [r for r in result['requests'] if r['case'] == name]
        assert len(requests) == 1
        assert [i['sha256'] for i in requests[0]['images']] == images + [images[0]] * 2
    for request in result['requests']:
        assert all(p['fullyPresent'] for p in request['promptEvidence'].values())
        expected = set() if variant == 'none' else {'project'} if variant == 'project' else {'common', 'project'}
        assert set(request['promptEvidence']) == expected
    # The provider scripts these calls; this is NOT a real model's choice of tools.
    rows.append({'variant': variant, 'run': str(run), 'resultSha256': hashlib.sha256(raw).hexdigest(),
                 'binarySha256': result['binarySha256'], 'promptEvidence': baseline['promptEvidence'],
                 'scriptedViewCallsConfirmedByImageResults': 2,
                 'cases': result['cases'],
                 'requestImageIdentitiesSha256': [hashlib.sha256(json.dumps([i['sha256'] for i in r['images']]).encode()).hexdigest() for r in result['requests']]})
manifest = json.loads((root / 'data/manifest.json').read_text())
for entry in manifest:
    assert hashlib.sha256((root / 'data' / entry['file']).read_bytes()).hexdigest() == entry['sha256']
    for row in rows:
        assert hashlib.sha256((Path(row['run']) / 'project' / entry['file']).read_bytes()).hexdigest() == entry['sha256']
summary = {'scope': 'Scripted local-provider serialization A/B; not real-model planning or an external-route fix',
           'originalsUnchanged': True, 'runs': rows}
(root / 'prompt-replay-summary.json').write_text(json.dumps(summary, indent=2) + '\n')
print(json.dumps({'variants': len(rows), 'turns': 32, 'requests': 40,
                  'scriptedViewCallsConfirmedByImageResults': 8, 'originalsUnchanged': True}))
