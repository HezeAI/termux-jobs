"""Verify native-only research evidence. Does not launch CLI or access accounts."""
import hashlib,json,sys
from pathlib import Path
roots=[Path(arg).resolve() for arg in sys.argv[1:4]]
assert len(roots)==3,'Arguments: compact-run-root chain-run-root websocket-run-root'
summary={'scope':'Synthetic native providers; no external-route success or real-model semantic guarantee','runs':{}}
for kind,root,pointer in zip(('compact','chain','websocket'),roots,('latest-codex-run.txt','latest-chain-run.txt','latest-websocket-run.txt')):
    run=Path((root/pointer).read_text().strip())
    raw=(run/'result.json').read_bytes();result=json.loads(raw)
    assert all(c['status']=='completed' for c in result['cases'])
    manifest=json.loads((root/'data/manifest.json').read_text())
    for entry in manifest:
        for base in (root/'data',run/'project'):
            assert hashlib.sha256((base/entry['file']).read_bytes()).hexdigest()==entry['sha256']
    summary['runs'][kind]={'root':str(root),'run':str(run),'resultSha256':hashlib.sha256(raw).hexdigest(),'originalsUnchanged':True}
    if kind=='compact':
        history=json.loads((run/'compaction-history.json').read_text())
        assert history['allBeforeTurnsStillAvailable'] and history['allBeforeTurnContentsUnchanged']
        assert history['beforeTurnsSha256']==history['afterOriginalTurnsSha256']
        for name,prefix in history['originalJournalPrefixes'].items():
            assert prefix['beforeSha256']==prefix['afterPrefixSha256']
            assert hashlib.sha256((run/'home'/name).read_bytes()[:prefix['bytes']]).hexdigest()==prefix['beforeSha256']
        assert all(all(r['requiredMarkers'].values()) for r in result['requests'])
        for case,count in [('cold-reopen-20-png',20),('after-native-compact',0),('reopen-after-native-compact',0),('continue-after-four-reviews',4),('reopen-after-four-reviews',4)]:
            assert next(r for r in result['requests'] if r['case']==case)['imageCount']==count
        original=next(r for r in result['requests'] if r['case']=='png-batch-1')['images'][0]['sha256']
        again=next(r for r in result['requests'] if r['case']=='review-old-image-after-compact' and r['imageCount'])['images'][0]['sha256']
        assert original==again
        summary['runs'][kind]['cases']=result['cases'];summary['runs'][kind]['history']=history
    elif kind=='chain':
        chain=json.loads((run/'chain-result.json').read_text())
        assert chain['allOwnedChildrenExited'] and chain['identicalForkHistorySha256']
        assert len(chain['pairs'])==8
        for pair in chain['pairs']:
            assert pair['completed']
            for request in pair['requests']:
                assert request['bodyBytes']==request['receivedHttpBodyBytes']
                if request['bodyComplete']:
                    assert request['model']=='gpt-6.1-sol' and request['reasoning']['effort']=='low' and request['imageCount']==20
            if 'during-upload' in pair['case']:
                assert not pair['requests'][0]['bodyComplete'] and pair['requests'][0]['receivedHttpBodyBytes']==1048576
                assert pair['requests'][-1]['bodyComplete']
            elif 'provider-drop' in pair['case']:
                assert len(pair['requests'])==2 and all(r['bodyComplete'] for r in pair['requests'])
        summary['runs'][kind]['chain']=chain
    else:
        requests=[r for r in result['requests'] if r.get('generate') is not False]
        assert all(r.get('transport')=='websocket' for r in result['requests'])
        assert any(r['case']=='continue-20-png' and r['imageCount']==0 and r['logicalImageCount']==20 for r in requests)
        assert any(r['case']=='cold-reopen-20-png' and r['imageCount']==20 for r in requests)
        retry=[r for r in requests if r['case']=='disconnect-recover-after-images']
        assert [r['imageCount'] for r in retry]==[0,21]
        summary['runs'][kind]['requests']=[{k:r.get(k) for k in ('case','bodyBytes','imageCount','logicalImageCount','previousResponseId','generate')} for r in result['requests']]
out=roots[0]/'context-probes-summary.json';out.write_text(json.dumps(summary,indent=2)+'\n')
print(json.dumps({'validated':list(summary['runs']),'summary':str(out),'sha256':hashlib.sha256(out.read_bytes()).hexdigest()}))
