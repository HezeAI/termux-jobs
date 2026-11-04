"""Pair production private chat transport with codex exec on native forks.
Only artificial data, under the existing kernel network guard. No journal edits.
"""
from pathlib import Path
# Reuse the exact provider, image counter, native identity and stdio RPC harness.
base = Path(__file__).with_name('probe-codex.py').read_text()
separator = "try:\n start();thread=rpc('thread/start'"
assert base.count(separator) == 1
exec(compile(base.split(separator)[0], str(Path(__file__).with_name('probe-codex.py')), 'exec'), globals())
repo = Path(os.environ['TERMUX_MEDIA_REPO'])
private = Path(os.environ['TERMUX_FORK_TEST_UNIX_ROOT'])
endpoint = private / 'chain.sock'
children = []
original_summarize = summarize
def summarize(q,raw):
    value=original_summarize(q,raw)
    value.update(model=q.get('model'),reasoning=q.get('reasoning'),receivedHttpBodyBytes=len(raw),bodyComplete=True)
    return value
class ChainHandler(Handler):
    def do_POST(self):
        if case.get('uploadClose') and not case.get('uploadDropped'):
            case['uploadDropped']=True
            declared=int(self.headers.get('content-length','0'))
            prefix=self.rfile.read(min(declared,1048576))
            record={'case':case['name'],'path':self.path,'receivedHttpBodyBytes':len(prefix),'bodyBytes':len(prefix),'declaredHttpBodyBytes':declared,'bodyComplete':False,'imageCount':None,'controlledClosePhase':'upload-prefix'}
            case['requests'].append(record);records.append(record)
            self.connection.shutdown(socket.SHUT_RDWR);self.connection.close();return
        super().do_POST()
server.RequestHandlerClass=ChainHandler
chain = {'scope': 'Actual production Node chat socket/adapter/app-server versus native exec, local HTTP only; Android UI and external path not tested', 'pairs': []}
env = {'HOME': str(HOME), 'CODEX_HOME': str(HOME), 'PATH': os.environ['PATH'], 'TMPDIR': str(RUN)}
# No real config or authorization is inherited. Apply the same model/effort to both paths.
config = (HOME/'config.toml').read_text().replace('model="gpt-6.1-sol"', 'model="gpt-6.1-sol"\nmodel_reasoning_effort="low"')
(HOME/'config.toml').write_text(config)
bin_dir = private/'bin';bin_dir.mkdir();(bin_dir/'codex').symlink_to(exe);env['PATH'] = str(bin_dir)+':'+env['PATH']
for name in ('codex-chat.cjs','agent-receipts.cjs','agent-action-preview.cjs','agent-completion-times.cjs','codex-launch.cjs'):
    shutil.copy2(repo/'app/src/main/assets'/name,private/name)
shutil.copy2(repo/'app/src/main/assets/codex-control.js',private/'codex-wire-v0.2.22.js')
def owned_stop(child):
    if child is None or child.poll() is not None:return
    child.terminate()
    try:child.wait(timeout=12)
    except subprocess.TimeoutExpired:
        child.kill();child.wait(timeout=8)
    assert child.poll() is not None

def node_child(args, label):
    child = subprocess.Popen(['node', *map(str,args)], cwd=PROJECT, env=env, stdin=subprocess.DEVNULL,
                             stdout=subprocess.DEVNULL, stderr=open(RUN/(label+'.stderr'),'ab'))
    children.append(child);return child

def request(q):
    wire = (json.dumps(q,separators=(',',':'))+'\n').encode()
    with socket.socket(socket.AF_UNIX,socket.SOCK_STREAM) as sock:
        sock.settimeout(20);sock.connect(str(endpoint)+'.chat');sock.sendall(wire);raw=b''
        while True:
            chunk=sock.recv(65536)
            if not chunk:break
            raw+=chunk
            assert len(raw)<524288,'Android chat response boundary exceeded'
    return json.loads(raw),len(wire),len(raw)

def start_chat(ident):
    child=node_child([private/'codex-chat.cjs','run',endpoint,'full','id',ident,PROJECT,'','never','en'],'chat')
    end=time.monotonic()+30
    while time.monotonic()<end:
        assert child.poll() is None,'Production chat adapter exited'
        try:
            value,_,_=request({'op':'chat-snapshot'})
            if value.get('connected'):return child,value
        except (OSError,ValueError):pass
        time.sleep(.05)
    raise TimeoutError('Production chat initialization')

def chat_turn(ident, name, prompt, drop=False, upload=False):
    global case
    case={'name':name,'requests':[],'tool':None,'issued':False,'drop':drop,'uploadClose':upload};beg=time.monotonic()
    reply,control_bytes,reply_bytes=request({'op':'chat-send','session':ident,'id':'msg_'+uuid.uuid4().hex,'kind':'message','text':prompt})
    assert reply.get('ok'),reply
    end=beg+90
    while time.monotonic()<end:
        snapshot,_,_=request({'op':'chat-snapshot'})
        if case['requests'] and not snapshot.get('busy'):
            assert snapshot.get('connected') and not snapshot.get('modelConnection'),snapshot.get('modelConnection')
            return {'path':'production-chat','case':name,'controlRequestBytes':control_bytes,'controlReplyBytes':reply_bytes,'elapsedSeconds':round(time.monotonic()-beg,3),'requests':list(case['requests']),'completed':True}
        time.sleep(.05)
    raise TimeoutError('Production chat completion')

def exec_turn(ident, name, prompt, drop=False, upload=False):
    global case
    case={'name':name,'requests':[],'tool':None,'issued':False,'drop':drop,'uploadClose':upload};beg=time.monotonic()
    args=[exe,'--no-daemon','--enable','default_mode_request_user_input','exec','--json','--skip-git-repo-check','-C',str(PROJECT),'-s','danger-full-access','-c','approval_policy="never"','-m','gpt-6.1-sol','resume',ident,prompt]
    child=subprocess.Popen(args,cwd=PROJECT,env=env,stdin=subprocess.DEVNULL,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
    children.append(child)
    try:out,err=child.communicate(timeout=90)
    except subprocess.TimeoutExpired:owned_stop(child);raise
    (RUN/(name+'.jsonl')).write_bytes(out);(RUN/(name+'.stderr')).write_bytes(err)
    events=[json.loads(line) for line in out.splitlines() if line.startswith(b'{')]
    assert child.returncode==0 and any(e.get('type')=='turn.completed' for e in events),(child.returncode,[e.get('type') for e in events])
    return {'path':'native-exec','case':name,'elapsedSeconds':round(time.monotonic()-beg,3),'requests':list(case['requests']),'exitCode':child.returncode,'completed':True}

try:
    start();thread=rpc('thread/start',{'cwd':str(PROJECT),'approvalPolicy':'never','sandbox':'danger-full-access'})['thread']['id']
    turn('short-control',[text('SYNTHETIC short control')])
    for i in range(0,20,4):turn('png-batch-'+str(i//4+1),[text('SYNTHETIC PNG batch')]+[image(f'png-600-{n}.png') for n in range(i,i+4)])
    forks={name:rpc('thread/fork',{'threadId':thread,'cwd':str(PROJECT),'approvalPolicy':'never','sandbox':'danger-full-access','excludeTurns':True})['thread']['id'] for name in ('chat','exec','chat-drop','exec-drop','chat-upload','exec-upload')}
    histories={name:rpc('thread/turns/list',{'threadId':ident,'limit':100,'itemsView':'full'})['data'] for name,ident in forks.items()}
    assert all(value==histories['chat'] for value in histories.values()),'Native fork histories differ'
    chain['identicalForkHistorySha256']=hashlib.sha256(json.dumps(histories['chat'],sort_keys=True).encode()).hexdigest()
    chain['forks']=forks
    stop()
    launcher=node_child([private/'codex-launch.cjs',endpoint,'danger-full-access','never','1','en'],'launch')
    end=time.monotonic()+30
    while not endpoint.exists():
        assert time.monotonic()<end and launcher.poll() is None,'Native Unix app-server unavailable'
        time.sleep(.05)
    chat,snapshot=start_chat(forks['chat'])
    prompt='SYNTHETIC continue without new images'
    chain['pairs'].append(chat_turn(forks['chat'],'chat-cold-reopen-20',prompt))
    owned_stop(chat)
    chat,snapshot=start_chat(forks['chat'])
    chain['pairs'].append(chat_turn(forks['chat'],'chat-second-reopen-20',prompt))
    owned_stop(chat)
    chat,snapshot=start_chat(forks['chat-drop'])
    chain['pairs'].append(chat_turn(forks['chat-drop'],'chat-provider-drop-after-full-body',prompt,drop=True))
    owned_stop(chat)
    chat,snapshot=start_chat(forks['chat-upload'])
    chain['pairs'].append(chat_turn(forks['chat-upload'],'chat-provider-close-during-upload',prompt,upload=True))
    owned_stop(chat);owned_stop(launcher)
    chain['pairs'].append(exec_turn(forks['exec'],'exec-cold-reopen-20',prompt))
    chain['pairs'].append(exec_turn(forks['exec'],'exec-second-reopen-20',prompt))
    chain['pairs'].append(exec_turn(forks['exec-drop'],'exec-provider-drop-after-full-body',prompt,drop=True))
    chain['pairs'].append(exec_turn(forks['exec-upload'],'exec-provider-close-during-upload',prompt,upload=True))
    # Both branches started with identical complete turn arrays, not edited journal copies.
    first=[r['requests'][0] for r in chain['pairs'] if r['case'] in ('chat-cold-reopen-20','exec-cold-reopen-20')]
    assert all(r['model']=='gpt-6.1-sol' and r['reasoning'].get('effort')=='low' for c in chain['pairs'] for r in c['requests'] if r['bodyComplete']), 'Model or effort differ'
    assert all(len(c['requests'])==2 for c in chain['pairs'] if 'provider-drop' in c['case']), 'Expected one controlled retry'
    assert all(c['requests'][0]['receivedHttpBodyBytes']==1048576 and not c['requests'][0]['bodyComplete'] and c['requests'][-1]['bodyComplete'] for c in chain['pairs'] if 'during-upload' in c['case']), 'Upload fault not observed at the provider prefix'
    assert len(first)==2 and first[0]['imageCount']==first[1]['imageCount']==20
    assert [i['sha256'] for i in first[0]['images']]==[i['sha256'] for i in first[1]['images']]
finally:
    for child in reversed(children):owned_stop(child)
    stop();server.shutdown();server.server_close();persist()
    chain['allOwnedChildrenExited']=all(child.poll() is not None for child in children)
    (RUN/'chain-result.json').write_text(json.dumps(chain,indent=2)+'\n')
    (OUT/'latest-chain-run.txt').write_text(str(RUN)+'\n')
    print(json.dumps({'run':str(RUN),'pairs':len(chain['pairs']),'allOwnedChildrenExited':chain['allOwnedChildrenExited']}),flush=True)
