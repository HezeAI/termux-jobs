"""Production Codex adapter + pinned CLI, artificial provider and kernel network fence.
Copy this and the existing media-context probe helpers into an isolated task folder.
No real authorization, images, tools or working chats are used.
"""
from pathlib import Path
base=Path(__file__).with_name('probe-codex-chain.py').read_text();separator="try:\n    start();thread=";assert base.count(separator)==1
exec(compile(base.split(separator)[0],str(Path(__file__).with_name('probe-codex-chain.py')),'exec'),globals())
def identity(child):
    try:return Path('/proc',str(child.pid),'stat').read_text().rsplit(')',1)[1].split()[19]
    except FileNotFoundError:return None
owned={}
def remember(child):owned[child]=identity(child);return child
def owned_stop(child):
    if child is None or child.poll() is not None:return
    assert owned.get(child) is not None and identity(child)==owned[child], 'No signal without task ownership/start identity'
    child.terminate()
    try:child.wait(timeout=10)
    except subprocess.TimeoutExpired:
        assert identity(child)==owned[child];child.kill();child.wait(timeout=5)
original_start=start
def start():
    try:original_start()
    finally:
        if process is not None:remember(process)
def stop():
    if process is None or process.poll() is not None:return
    process.stdin.close()
    try:process.wait(timeout=8)
    except subprocess.TimeoutExpired:owned_stop(process)
original_node=node_child
def node_child(args,label):return remember(original_node(args,label))
def status():return request({'op':'status'})[0]
def wait_for(test,seconds=40):
    end=time.monotonic()+seconds
    while time.monotonic()<end:
        value=status()
        if test(value):return value
        time.sleep(.05)
    raise AssertionError('Native condition timed out: '+json.dumps(value))
report={'scope':'Pinned Codex 0.166.0 + production adapter/Unix socket with synthetic loopback provider under kernel fence; not Android or external-route acceptance'}
try:
    start();thread=rpc('thread/start',{'cwd':str(PROJECT),'approvalPolicy':'never','sandbox':'danger-full-access'})['thread']['id'];turn('seed',[text('Artificial seed; preserve history')]);stop()
    native=remember(subprocess.Popen([exe,'--no-daemon','app-server','--listen','unix://'+str(endpoint)],cwd=PROJECT,env=env,stdin=subprocess.DEVNULL,stdout=subprocess.DEVNULL,stderr=open(RUN/'native-uds.stderr','w')));children.append(native)
    end=time.monotonic()+30
    while not endpoint.exists():assert time.monotonic()<end and native.poll() is None;time.sleep(.05)
    chat,_=start_chat(thread);first=status();assert first['context']['compactSupported'],first
    report['initialContext']=first['context'];chat_turn(thread,'initial-usage','Artificial context counter')
    before=wait_for(lambda s:s.get('context',{}).get('usage') is not None);assert before['context']['usage']['used']==1010,before['context']
    report['beforeCompact']=before['context'];case={'name':'manual-compact','requests':[],'tool':None,'issued':False,'drop':False};admission=request({'op':'chat-compact','session':thread})[0];assert admission.get('accepted'),admission
    after=wait_for(lambda s:s['context'].get('compaction',{}).get('phase') in ('completed','failed','unknown'));assert after['context']['compaction']['phase']=='completed',after
    snapshot=request({'op':'chat-snapshot'})[0];assert any(r.get('toolType')=='contextCompaction' for r in snapshot['rows']);report['compactAdmission']=admission;report['afterCompact']=after['context']
    chat_turn(thread,'after-compact-usage','Artificial continuation after manual compaction');current=status();assert current['context']['usage']['used']==1010;assert not current['context']['stale'];report['freshAfterCompact']=current['context']
    selected=request({'op':'model','model':'gpt-6-sol','effort':'medium'})[0];assert selected.get('ok'),selected;assert status()['context']['stale'];chat_turn(thread,'new-model-usage','Artificial continuation with another supported model');current=status();assert current['context']['usage']['model']=='gpt-6-sol';assert current['context']['usage']['used']==1010;assert not current['context']['stale'];report['freshAfterModel']=current['context']
    owned_stop(chat);count=len(records);chat,_=start_chat(thread);reopened=status();assert reopened['context']['usage']['used']==1010;assert len(records)==count,'Reopen must not compact or invoke the provider';report['reopened']=reopened['context']
    # A real local connection loss during the operation is not a successful finish.
    entered=threading.Event();release=threading.Event();original_post=server.RequestHandlerClass.do_POST
    def slow_post(self):
        if case['name']=='disconnect-compact':entered.set();release.wait(15)
        try:original_post(self)
        except (BrokenPipeError,ConnectionResetError):pass
    server.RequestHandlerClass.do_POST=slow_post;case={'name':'disconnect-compact','requests':[],'tool':None,'issued':False,'drop':False}
    admission=request({'op':'chat-compact','session':thread})[0];assert admission.get('accepted');assert entered.wait(10);live=status();timed=request({'op':'chat-snapshot'})[0];assert timed.get('requestTiming') and timed['requestTiming']['request']==live['context']['compaction']['turn'];report['liveRequestTiming']=timed['requestTiming'];owned_stop(native);release.set()
    broken=wait_for(lambda s:not s.get('connected'),10);assert broken['context']['compaction']['phase']=='unknown';assert not broken['context']['canCompact'];assert broken['context']['stale'];assert request({'op':'chat-snapshot'})[0].get('requestTiming') is None;report['disconnected']=broken['context'];report['completed']=True
finally:
    if 'release' in globals():release.set()
    for child in reversed(children):owned_stop(child)
    stop();server.shutdown();server.server_close();persist();report['allOwnedChildrenExited']=all(c.poll() is not None for c in children)
    (RUN/'context-ui-result.json').write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps(report,indent=2))
