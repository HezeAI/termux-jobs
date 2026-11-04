"""Measure actual local history WS messages separately from provider body size.
Artificial data and fake provider under the existing kernel network guard only.
"""
from pathlib import Path
base=Path(__file__).with_name('probe-codex-chain.py').read_text();separator='try:\n    start();thread=';assert base.count(separator)==1
exec(compile(base.split(separator)[0],str(Path(__file__).with_name('probe-codex-chain.py')),'exec'),globals())
# All signal targets are Popen handles captured here with /proc start identities.
def identity(child):
    try:return Path('/proc',str(child.pid),'stat').read_text().rsplit(')',1)[1].split()[19]
    except FileNotFoundError:return None
owned_identities={}
def owned_stop(child):
    if child is None or child.poll() is not None:return
    assert identity(child)==owned_identities[child], 'No signal: owned identity mismatch'
    child.terminate()
    try:child.wait(timeout=12)
    except subprocess.TimeoutExpired:
        assert identity(child)==owned_identities[child], 'No signal: owned identity mismatch'
        child.kill();child.wait(timeout=8)
start_original=start
stop_original=stop
def start():
    try:start_original()
    finally:
        if process is not None:owned_identities[process]=identity(process)
def stop():
    if process is None or process.poll() is not None:return
    process.stdin.close()
    try:process.wait(timeout=8)
    except subprocess.TimeoutExpired:owned_stop(process)
node_original=node_child
def node_child(args,label):
    child=node_original(args,label);owned_identities[child]=identity(child);return child

metrics=RUN/'local-wire-metrics.jsonl'
wire=private/'codex-wire-v0.2.22.js';original_wire=wire.read_text()
needle="const message=JSON.parse(Buffer.concat(this.fragments,this.size).toString('utf8'));this.fragments=[];this.size=0;"
assert original_wire.count(needle)==1
instrumented=original_wire.replace(needle,"const message=JSON.parse(Buffer.concat(this.fragments,this.size).toString('utf8'));fs.appendFileSync("+json.dumps(str(metrics))+",JSON.stringify({direction:'in',bytes:this.size,method:this.pending.get(message.id)?.method||message.method||'notification'})+'\\n',{mode:0o600});this.fragments=[];this.size=0;")
# Only a private fixture copy is instrumented; production source is unchanged.
wire.write_text(instrumented)
result={'scope':'Guarded local provider versus actual production Node/native Unix WS history; no Android UI or authenticated external traffic','localCapBytes':32*1024*1024,'imageViewsRequested':24}
try:
    start();thread=rpc('thread/start',{'cwd':str(PROJECT),'approvalPolicy':'never','sandbox':'danger-full-access'})['thread']['id']
    plan=[{'name':'view_image','args':{'path':str(PROJECT/f'png-600-{n%20}.png')}} for n in range(24)]
    turn('one-turn-24-native-image-views',[text('SYNTHETIC transport boundary test; use only the requested artificial image tools')],tool=plan)
    result['providerBodyBytes']=records[-1]['bodyBytes'];result['providerInlineImageCount']=records[-1]['imageCount']
    assert result['providerInlineImageCount']==24 and result['providerBodyBytes']>32*1024*1024
    provider_count=len(records);stop()
    launcher=node_child([private/'codex-launch.cjs',endpoint,'danger-full-access','never','1','en'],'history-launch')
    end=time.monotonic()+30
    while not endpoint.exists():
        assert time.monotonic()<end and launcher.poll() is None
        time.sleep(.05)
    for repetition in range(2):
        chat,snapshot=start_chat(thread);assert snapshot.get('connected')
        cursor=snapshot.get('older');pages=1
        while cursor:
            snapshot,_,_=request({'op':'chat-snapshot','session':thread,'cursor':cursor});cursor=snapshot.get('older');pages+=1;assert pages<12
        owned_stop(chat);result['reopen'+str(repetition+1)]={'connected':True,'pages':pages}
    assert len(records)==provider_count,'History viewing unexpectedly triggered model inference'
    wire_rows=[json.loads(line) for line in metrics.read_text().splitlines()]
    result['wireMessages']=wire_rows;result['largestLocalMessageBytes']=max(v['bytes'] for v in wire_rows)
    result['largestHistoryPageBytes']=max(v['bytes'] for v in wire_rows if v['method']=='thread/turns/list')
    result['messagesAtOrAboveLocalCap']=sum(v['bytes']>=32*1024*1024 for v in wire_rows)
    assert not result['messagesAtOrAboveLocalCap'];result['completed']=True
finally:
    for child in reversed(children):owned_stop(child)
    stop();server.shutdown();server.server_close();persist()
    result['allOwnedChildrenExited']=all(c.poll() is not None for c in children)
    result['productionWireSha256']=hashlib.sha256(original_wire.encode()).hexdigest()
    (RUN/'local-history-result.json').write_text(json.dumps(result,indent=2)+'\n')
    (OUT/'latest-local-history-run.txt').write_text(str(RUN)+'\n')
    print(json.dumps({'run':str(RUN),**{k:v for k,v in result.items() if k!='wireMessages'}}),flush=True)
