"""Summarize ONLY synthetic native rollout traces; never read auth or live chats.
Recorded request JSON is not encrypted wire bytes or provider receipt evidence.
"""
import argparse
import hashlib
import json
import re
from pathlib import Path

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('synthetic_run', type=Path)
args = parser.parse_args()
root = args.synthetic_run.resolve()
assert (root / 'dataset.json').is_file() and (root / 'result.json').is_file(), 'Expected a preserved artificial experiment'
manifest = json.loads((root / 'dataset.json').read_text())
assert manifest and all(re.fullmatch(r'png-600-[0-9]+\.png', e['file']) for e in manifest)
requests = []

def images(value):
    if isinstance(value, dict):
        if value.get('type') == 'input_image':
            url = value.get('image_url', '')
            if isinstance(url, dict):
                url = url.get('url', '')
            yield hashlib.sha256(str(url).encode()).hexdigest()
        for child in value.values():
            yield from images(child)
    elif isinstance(value, list):
        for child in value:
            yield from images(child)

for trace in sorted((root / 'traces').rglob('trace.jsonl')):
    for line in trace.open():
        event = json.loads(line)
        payload = event.get('payload', {})
        if payload.get('type') not in ('inference_started', 'compaction_request_started'):
            continue
        ref = Path(payload['request_payload']['path'])
        assert not ref.is_absolute() and '..' not in ref.parts
        source = (trace.parent / ref).resolve()
        assert source.is_relative_to(root)
        raw = source.read_bytes()
        request = json.loads(raw)
        image_hashes = list(images(request.get('input', [])))
        requests.append({
            'phase': trace.relative_to(root / 'traces').parts[0],
            'wallTimeUnixMs': event.get('wall_time_unix_ms'),
            'kind': payload['type'],
            'recordedJsonBytes': len(raw),
            'recordedJsonSha256': hashlib.sha256(raw).hexdigest(),
            'inlineImageCount': len(image_hashes),
            'uniqueInlineImageCount': len(set(image_hashes)),
            'hasPreviousResponseId': bool(request.get('previous_response_id')),
            'requestType': request.get('type'),
        })
requests.sort(key=lambda row: row['wallTimeUnixMs'] or 0)
result = {
    'scope': 'Recorded native JSON only; may be logical prompts or WS warmup; not TLS bytes, physical WS delta or provider receipts',
    'sourceResultSha256': hashlib.sha256((root / 'result.json').read_bytes()).hexdigest(),
    'originalsUnchangedNow': all(hashlib.sha256((root / 'project' / e['file']).read_bytes()).hexdigest() == e['sha256'] for e in manifest),
    'requests': requests,
}
assert result['originalsUnchangedNow']
(root / 'trace-request-summary.json').write_text(json.dumps(result, indent=2) + '\n')
print(json.dumps({'requests': len(requests), 'originalsUnchangedNow': True, 'summary': str(root / 'trace-request-summary.json')}))
