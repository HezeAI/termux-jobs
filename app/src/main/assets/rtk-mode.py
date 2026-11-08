"""Pinned optional RTK component and per-chat hooks. Never executes a rewritten command.
The native agent owns approval/execution; this helper only rewrites a conservative
shell pipeline and filters its stdin. No auth, instruction or foreign-hook edits.
"""
import contextlib,fcntl,hashlib,json,os,platform,re,shlex,sqlite3,stat,subprocess,sys,tarfile,tempfile,time,uuid,urllib.request
from pathlib import Path
PIN=globals().get('PIN') or json.loads(Path(__file__).with_name('rtk-component.json').read_text())
BASE=Path.home()/'.local/share/termux-jobs'
MAX_OUTPUT=16*1024*1024

def directory(path):
    path=Path(path)
    if any(p.is_symlink() for p in [path,*path.parents]):raise ValueError('UNSAFE_PATH')
    if not path.exists():
        directory(path.parent);path.mkdir(mode=0o700,exist_ok=True)
    if not path.is_dir():raise ValueError('UNSAFE_PATH')
    return path

def read(path,default=None):
    try:
        if path.is_symlink() or path.stat().st_size>1024*1024:raise ValueError('UNSAFE_PATH')
        return json.loads(path.read_text())
    except FileNotFoundError:return default

def write(path,value):
    directory(path.parent)
    fd,name=tempfile.mkstemp(prefix='.rtk-',dir=path.parent)
    try:
        with os.fdopen(fd,'w') as f:json.dump(value,f);f.flush();os.fsync(f.fileno())
        os.replace(name,path)
    finally:
        if os.path.exists(name):os.unlink(name)

def chat(profile):
    if not re.fullmatch('[0-9a-fA-F-]{36}',profile):raise ValueError('INVALID_CHAT')
    return directory(BASE/'rtk/chats'/profile)

def component():return BASE/'components/rtk'/PIN['version']
def digest(path):
    h=hashlib.sha256()
    with path.open('rb')as f:
        for b in iter(lambda:f.read(65536),b''):h.update(b)
    return h.hexdigest()

def verify(path,spec=None):
    spec=PIN if spec is None else spec
    if path.is_symlink():return False
    binary=path/'rtk'
    if binary.is_symlink() or not binary.is_file() or digest(binary)!=spec['binarySha256']:return False
    with binary.open('rb')as f:head=f.read(64)
    if head[:6]!=b'\x7fELF\x02\x01' or int.from_bytes(head[18:20],'little')!=183:return False
    if spec.get('noticesSha256') and ((path/'THIRD_PARTY_NOTICES.txt').is_symlink() or not (path/'THIRD_PARTY_NOTICES.txt').is_file() or digest(path/'THIRD_PARTY_NOTICES.txt')!=spec['noticesSha256']):return False
    try:
        r=subprocess.run([str(binary),'--version'],capture_output=True,timeout=10,env={'PATH':'/usr/bin:/bin','HOME':str(path),'RTK_TELEMETRY_DISABLED':'1'})
        return r.returncode==0 and r.stdout.decode().strip()=='rtk '+spec['version'] and not (path/'LICENSE').is_symlink() and (path/'LICENSE').is_file() and digest(path/'LICENSE')==spec['licenseSha256']
    except (OSError,subprocess.TimeoutExpired):return False

def identity(pid):
    try:return Path('/proc',str(pid),'stat').read_text().rsplit(')',1)[1].split()[19]
    except OSError:return ''

def ready(profile,run):
    r=read(chat(profile)/'ready.json',{})
    return r.get('run')==run and r.get('start') and identity(r.get('pid'))==r['start']

def mark_ready(profile,run,pid,statistics_ready=False):
    start=identity(pid)
    if start:write(chat(profile)/'ready.json',{'run':run,'pid':pid,'start':start,'componentVersion':PIN['version'],'statisticsVersion':2 if statistics_ready else 0,'nativeDispatch':str(component()/'chat-bin') in os.environ.get('PATH','').split(os.pathsep)})

def legacy_totals(profile):
    d=chat(profile);basic={'input':0,'output':0,'commands':0};advanced=dict(basic)
    if (d/'measurements').is_symlink() or (d/'advanced').is_symlink():raise ValueError('UNSAFE_PATH')
    for p in (d/'measurements').glob('*.json'):
        e=read(p,{})
        if all(type(e.get(k)) is int and 0<=e[k]<=MAX_OUTPUT*2 for k in ['input','output']):
            basic['input']+=e['input'];basic['output']+=e['output'];basic['commands']+=1
    db=d/'advanced/history.db'
    if db.is_file() and not db.is_symlink():
        try:
            with contextlib.closing(sqlite3.connect(db.as_uri()+'?mode=ro',uri=True)) as c:
                advanced=dict(zip(['input','output','commands'],c.execute('SELECT COALESCE(SUM(input_tokens),0),COALESCE(SUM(output_tokens),0),COUNT(*) FROM commands').fetchone()))
        except sqlite3.Error:pass
    return {'basic':basic,'advanced':advanced}

def statistics_root(profile):
    d=directory(chat(profile)/'statistics-v2')
    fd=os.open(d/'lock',os.O_CREAT|os.O_RDWR|os.O_NOFOLLOW,0o600)
    with os.fdopen(fd,'w') as lock:
        fcntl.flock(lock,fcntl.LOCK_EX)
        if not (d/'epoch.json').exists():write(d/'epoch.json',{'version':2,'startedAt':int(time.time()*1000),'legacy':legacy_totals(profile)})
    return d

def event_key(value,run):
    key=value.get('tool_use_id')
    return hashlib.sha256((run+'\0'+key).encode()).hexdigest() if isinstance(key,str) and key else uuid.uuid4().hex

def metric_file(profile,key):
    if not re.fullmatch('[a-f0-9]{32,64}',key):raise ValueError('INVALID_EVENT')
    return directory(statistics_root(profile)/'events')/(key+'.json')

def metric_update(profile,key,**values):
    p=metric_file(profile,key);old=read(p,{})
    # The post hook is observational and idempotent. Replayed events never add totals.
    old.update(values);write(p,old)

def recall_mode(value,profile):
    inp=value.get('tool_input',{});tool=value.get('tool_name');words=literal_words(inp.get('command','')) if tool=='Bash' else None
    if words and len(words)>2 and words[:2]==['rtk','recall'] and '--list' not in words:return 'advanced'
    paths=words[1:] if words and words[0] in ('cat','head','tail','sed') else [inp.get('file_path','')] if tool=='Read' else []
    root=str(chat(profile)/'originals')+'/'
    if any(isinstance(p,str) and p.startswith(root) and re.fullmatch(r'[a-f0-9]{32}\.txt',p[len(root):]) for p in paths):return 'basic'
    return None

def native_window(original,rewritten):
    # Pinned RTK read at level=none writes the selected head/tail window verbatim.
    # Never infer a base from a file's size, a token estimate or arbitrary filters.
    a=literal_words(original);b=literal_words(rewritten)
    if not a or not b or len(a)!=4 or a[0] not in ('head','tail') or a[1]!='-n' or not a[2].isdigit():return False
    return b==['rtk','read',a[3],'--'+a[0]+'-lines',a[2]]

def response_bytes(value):
    response=value.get('tool_response')
    if isinstance(response,str):return response.encode('utf-8'),None
    if isinstance(response,dict) and isinstance(response.get('stdout'),str) and isinstance(response.get('stderr',''),str):
        return response['stdout'].encode('utf-8'),response.get('stderr','').encode('utf-8')
    return None,None

def native_measurement(profile,run,event,stdout,stderr,key):
    if stdout is None or not event.get('nativeArgsHash'):return None
    body=stdout+(stderr or b'');digest_body=hashlib.sha256(body).hexdigest();matches=[]
    root=directory(chat(profile)/'native-bytes-v1')
    for meta in root.glob('*.meta.json'):
        try:m=read(meta,{})
        except (ValueError,OSError):continue
        if m.get('run')!=run or m.get('argsHash')!=event['nativeArgsHash'] or m.get('createdAt',0)<event.get('startedAt',0):continue
        if m.get('consumedBy') not in (None,key):continue
        try:result=read(meta.with_name(meta.name.replace('.meta.json','.result.json')),{})
        except (ValueError,OSError):continue
        if result.get('outputHash')==digest_body:baseline=result.get('inputBytes')
        else:
            variants=[x.get('inputBytes') for x in result.get('normalizations',[]) if x.get('hash')==digest_body]
            # Claude's structured receipt trims all trailing whitespace. A string
            # receipt with indistinguishable normalizations has no trustworthy base.
            baseline=variants[-1] if variants and stderr is not None else variants[0] if variants and len(set(variants))==1 else None
        if type(baseline)is int and baseline>=0:matches.append((meta,m,baseline))
    # Concurrent indistinguishable results with different bases cannot be attributed safely.
    if not matches or len({m[2] for m in matches})!=1:return None
    path,metadata,baseline=matches[0];metadata['consumedBy']=key;write(path,metadata)
    # Retain completed evidence without rescanning every historical invocation.
    try:
        completed=directory(root/'completed')
        for source in [path,path.with_name(path.name.replace('.meta.json','.result.json'))]:
            destination=completed/source.name
            if not destination.exists() and not destination.is_symlink():os.rename(source,destination)
    except (OSError,ValueError):pass
    return baseline

def observe_result(value,profile,run):
    key=event_key(value,run);p=metric_file(profile,key);e=read(p,{})
    if e.get('delivered') is True:return
    recall=recall_mode(value,profile)
    if not e and not recall:return
    stdout,stderr=response_bytes(value);output=None if stdout is None else len(stdout)+(len(stderr) if stderr is not None else 0)
    if recall:
        metric_update(profile,key,mode=recall,recall=True,delivered=True,outputBytes=output);return
    baseline=e.get('inputBytes')
    # Only compare the captured stdin with the matching returned stdout. A changed
    # or truncated tool response cannot silently masquerade as measured RTK savings.
    if stdout is None:baseline=None
    elif e.get('mode')=='basic':
        digest=hashlib.sha256(stdout).hexdigest()
        if digest==e.get('filteredHash'):baseline=e.get('rawInputBytes',baseline)
        else:baseline=e.get('normalizations',{}).get(digest)
        if stderr is not None and baseline is not None:baseline+=len(stderr)
    else:
        baseline=native_measurement(profile,run,e,stdout,stderr,key)
        if baseline is None and e.get('verbatimWindow') and not PIN.get('measurementProtocol'):
            if not any(marker in stdout.lower() for marker in [b'truncated',b'output limit',b'rtk recall',b'failed',b'error']):baseline=output
    metric_update(profile,key,delivered=True,inputBytes=baseline,outputBytes=output)

def statistics(profile):
    d=statistics_root(profile);epoch=read(d/'epoch.json',{});modes={}
    for mode in ['basic','advanced']:
        modes[mode]={'commands':0,'comparedCommands':0,'noReductionCommands':0,'unknownBaselineCommands':0,'unconfirmedCommands':0,'inputBytes':0,'outputBytes':0,'knownOutputBytes':0,'unknownOutputCommands':0,'recallBytes':0,'recallCommands':0,'unknownRecallCommands':0}
    for path in directory(d/'events').glob('*.json'):
        e=read(path,{});mode=e.get('mode')
        if mode not in modes:continue
        t=modes[mode];output=e.get('outputBytes');before=e.get('inputBytes');known=lambda n:type(n) is int and n>=0
        if e.get('recall'):
            if e.get('delivered'):
                t['recallCommands']+=1
                if known(output):t['recallBytes']+=output
                else:t['unknownRecallCommands']+=1
            continue
        if not e.get('delivered') and not e.get('captured'):continue
        t['commands']+=1
        if not e.get('delivered'):t['unconfirmedCommands']+=1;continue
        if known(output):t['knownOutputBytes']+=output
        else:t['unknownOutputCommands']+=1
        if known(before) and known(output):
            t['comparedCommands']+=1;t['inputBytes']+=before;t['outputBytes']+=output
            if output>=before:t['noReductionCommands']+=1
        else:t['unknownBaselineCommands']+=1
    return dict(version=2,startedAt=epoch.get('startedAt'),legacy=epoch.get('legacy',{}),**modes)

def status(profile,run):
    d=chat(profile);state=read(d/'mode.json',{});installed=verify(component());connected=bool(ready(profile,run));mode=state.get('mode','basic' if state.get('enabled') else 'off');error=read(d/'hook-error.json',{})
    failure=error.get('run')==run and bool(error.get('error'))
    readiness=read(d/'ready.json',{})
    if mode=='advanced' and connected and not readiness.get('nativeDispatch'):
        failure=True;error={'error':'RESTART_REQUIRED'}
    previous=PIN.get('previous',{});legacy=bool(previous and verify(component().parent/previous['version'],previous))
    active_legacy=legacy and connected and readiness.get('componentVersion',previous.get('version'))==previous.get('version')
    return dict(installed=installed,legacyInstalled=legacy,updateAvailable=legacy and not installed,activeLegacy=active_legacy,ready=connected and not failure,enabled=(installed or active_legacy) and connected and not failure and mode!='off',desired=mode!='off',mode=mode,error=error.get('error','') if failure else '',statisticsReady=bool(readiness.get('statisticsVersion')==2),statistics=statistics(profile))

def emit(**value):print(json.dumps(value),flush=True)

class SecureRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self,req,fp,code,msg,headers,newurl):
        if not newurl.startswith('https://'):raise ValueError('INSECURE_DOWNLOAD')
        return super().redirect_request(req,fp,code,msg,headers,newurl)

def installation(request):
    # Single environment lock; never replace a valid installation used by other chats.
    parent=directory(component().parent)
    fd=os.open(parent/'install.lock',os.O_CREAT|os.O_RDWR|os.O_NOFOLLOW,0o600)
    with os.fdopen(fd,'w')as lock:
        try:fcntl.flock(lock,fcntl.LOCK_EX|fcntl.LOCK_NB)
        except BlockingIOError:raise ValueError('INSTALL_BUSY')
        if verify(component()):return
        with tempfile.TemporaryDirectory(prefix='.install-',dir=parent)as tmp:
            staging=Path(tmp);archive=staging/'archive.tar.gz';downloaded=0;h=hashlib.sha256()
            emit(stage='download',bytes=0,total=PIN['size'])
            with urllib.request.build_opener(SecureRedirect()).open(PIN['url'],timeout=15) as response,archive.open('xb') as f:
                if not response.geturl().startswith('https://'):raise ValueError('INSECURE_DOWNLOAD')
                while True:
                    cancelled(request)
                    b=response.read(32768)
                    if not b:break
                    downloaded+=len(b)
                    if downloaded>PIN['size']:raise ValueError('SIZE_MISMATCH')
                    h.update(b);f.write(b);emit(stage='download',bytes=downloaded,total=PIN['size'])
            cancelled(request)
            if downloaded!=PIN['size']or h.hexdigest()!=PIN['sha256']:raise ValueError('CHECKSUM_MISMATCH')
            emit(stage='install')
            unpack=directory(staging/'unpacked')
            with tarfile.open(archive,'r:gz')as tar:
                members=tar.getmembers()
                expected={'rtk','LICENSE'}|({'THIRD_PARTY_NOTICES.txt'} if PIN.get('noticesSha256') else set())
                if {m.name for m in members}!=expected or len(members)!=len(expected):raise ValueError('UNSAFE_ARCHIVE')
                for m in members:
                    if not m.isfile()or m.size>80*1024*1024:raise ValueError('UNSAFE_ARCHIVE')
                    with tar.extractfile(m)as source,(unpack/m.name).open('xb')as target:
                        while True:
                            cancelled(request);b=source.read(65536)
                            if not b:break
                            target.write(b)
                    (unpack/m.name).chmod(0o700 if m.name=='rtk'else 0o600)
            if not verify(unpack):raise ValueError('BINARY_INCOMPATIBLE')
            (unpack/'rtk-mode.py').write_text(request['_source']);(unpack/'rtk-component.json').write_text(json.dumps(PIN))
            cancelled(request)
            if component().exists():raise ValueError('INSTALLATION_DAMAGED')
            os.rename(unpack,component())

def cancelled(q):
    if q.get('attempt'):
        a=read(chat(q['profile'])/'attempt.json',{})
        if a.get('id')!=q['attempt']or a.get('cancelled'):raise ValueError('CANCELLED')

def literal_words(command):
    # A single literal argv only. Never reinterpret shell syntax or insert a shell
    # around the original command: native prefix policy must still see its argv.
    if not isinstance(command,str) or len(command)>16384:return None
    if any(c in command for c in '\n\r\x00$`|&;<>\\'):return None
    try:words=shlex.split(command)
    except ValueError:return None
    if not words or any(c in command for c in '*?[]{}~'):return None
    return words

def filter_for(command):
    if re.fullmatch(r'git status(?: --(?:short|porcelain|branch))*',command):return 'git-status'
    if re.fullmatch(r'git diff(?: --(?:stat|cached|staged|name-only))*',command):return 'git-diff'
    if re.fullmatch(r'git log(?: --oneline)?(?: -[n] [1-9][0-9]{0,2})?',command):return 'git-log'
    words=literal_words(command)
    if not words:return None
    exe,*args=words
    if any('/originals/' in a for a in args):return None
    if any(a in ('--pdb','--trace','--watch','-w') for a in args):return None
    if exe in ('rg','grep'):
        # Structured/binary, replacement, passthrough and stdin forms stay native.
        if not args or any(a in ('-','-z','-0','-U','--json','--null','--null-data','--multiline','--passthru','--pre','--pre-glob','--replace','-r') or a.startswith(('--pre=','--pre-glob=','--replace=','--encoding=')) for a in args):return None
        if '--files' in args:return 'find' if exe=='rg' else None
        return 'grep'
    if exe in ('cat','head','tail'):
        if not args or any(a in ('-','-f','-F','--follow') or a.startswith('--follow=') for a in args):return None
        # Byte windows/binary and unusual flags remain untouched.
        if any(a.startswith('-') and not re.fullmatch(r'(?:--|-[nqv]|-[0-9]+|--lines(?:=[0-9]+)?)',a) for a in args):return None
        return 'read'
    if exe=='find':
        if not args or any(a in ('-exec','-execdir','-ok','-okdir','-delete','-fprint','-fprint0','-fprintf','-print0','-printf') for a in args):return None
        return 'find'
    if exe in ('gradle','./gradlew'):
        # Preserve command/tasks exactly. No daemon control, continuous/watch or stdin.
        if not args or any(a in ('--continuous','-t','--stop','--status','--foreground','--console=rich') for a in args):return None
        return 'build-preview'
    if exe in ('pytest','pytest-3') or (exe in ('python','python3') and args[:2]==['-m','pytest']):return 'pytest'
    if exe=='cargo' and args[:1]==['test']:return 'cargo-test'
    if exe=='go' and args[:1]==['test'] and '-json' in args:return 'go-test'
    if exe=='go' and args[:1]==['build']:return 'go-build'
    if exe=='ctest':return 'ctest'
    if exe in ('vitest','./node_modules/.bin/vitest') and args[:1]==['run']:return 'vitest'
    return None

def native_env(profile):
    d=directory(chat(profile)/'advanced');config=directory(d/'config');data=directory(d/'data')
    env=dict(os.environ,RTK_DB_PATH=str(d/'history.db'),RTK_RECALL_DB=str(d/'recall.db'),XDG_CONFIG_HOME=str(config),XDG_DATA_HOME=str(data),RTK_TELEMETRY_DISABLED='1',RTK_SUPPRESS_HOOK_WARNING='1')
    # The selected chat keeps native recovery enabled. Never touch shared config.
    env.pop('RTK_RECALL',None);env.pop('RTK_TEE',None)
    return env

def refresh_dispatch():
    target=directory(component()/'chat-bin')/'rtk'
    script='#!/bin/sh\nexec python3 -I '+shlex.quote(str(component()/'rtk-mode.py'))+' native "$@"\n'
    if target.is_symlink():raise ValueError('UNSAFE_PATH')
    if target.is_file() and target.read_text()==script:return
    fd,name=tempfile.mkstemp(prefix='.dispatch-',dir=target.parent)
    try:
        with os.fdopen(fd,'w') as f:f.write(script)
        os.chmod(name,0o700);os.replace(name,target)
    finally:
        if os.path.exists(name):os.unlink(name)

def refresh_helper(source):
    # Updating the app must update the installed bridge too; never replace the RTK
    # binary or reset per-chat state. Atomic replace also keeps in-flight reads safe.
    if not source:return
    refresh_dispatch()
    target=component()/'rtk-mode.py'
    if target.is_symlink():raise ValueError('UNSAFE_PATH')
    if target.is_file() and target.read_text()==source:return
    fd,name=tempfile.mkstemp(prefix='.bridge-',dir=directory(component()))
    try:
        with os.fdopen(fd,'w') as f:f.write(source);f.flush();os.fsync(f.fileno())
        os.replace(name,target)
    finally:
        if os.path.exists(name):os.unlink(name)

def hook(value,profile,run,agent):
    if value.get('hook_event_name')=='SessionStart':
        if agent=='codex' and value.get('verified')is not True:return {}
        # The hook's parent shell may be transient; the launcher exports the native owner PID.
        pid=int(os.environ.get('JOBS_RTK_OWNER_PID','0'))
        if pid>0:mark_ready(profile,run,pid,value.get('statisticsReady')is True)
        return {}
    if value.get('hook_event_name')=='PostToolUse':
        try:observe_result(value,profile,run)
        except Exception:pass
        return {}
    state=read(chat(profile)/'mode.json',{})
    if state.get('enabled') is not True:return {}
    if state.get('mode')=='advanced':
        try:
            binary=component()/'rtk'
            if binary.is_symlink() or digest(binary)!=PIN['binarySha256']:raise ValueError('BINARY_CHANGED')
            result=subprocess.run([str(binary),'hook',agent],input=json.dumps(value),text=True,capture_output=True,timeout=4,env=native_env(profile))
            if result.returncode:raise ValueError('NATIVE_HOOK_FAILED')
            rewritten=json.loads(result.stdout) if result.stdout.strip() else {}
            updated=rewritten.get('hookSpecificOutput',{}).get('updatedInput',{}).get('command')
            original=value.get('tool_input',{}).get('command','');words=literal_words(original)
            if updated or words and words[0]=='rtk' and (len(words)<2 or words[1]!='recall'):
                try:
                    args=literal_words(updated or original)
                    fingerprint=hashlib.sha256(json.dumps(args[1:],separators=(',',':')).encode()).hexdigest() if args and args[0]=='rtk' else ''
                    metric_update(profile,event_key(value,run),mode='advanced',verbatimWindow=native_window(original,updated or ''),nativeArgsHash=fingerprint,startedAt=time.time_ns())
                except Exception:pass
            return rewritten
        except Exception as e:
            write(chat(profile)/'hook-error.json',{'run':run,'error':'NATIVE_HOOK_FAILED'})
            return {}
    if value.get('tool_name')!='Bash':return {}
    original=value.get('tool_input',{});command=original.get('command','');kind=filter_for(command)
    if not kind:return {}
    helper=component()/'rtk-mode.py'
    token=event_key(value,run)
    try:metric_update(profile,token,mode='basic')
    except Exception:pass
    pipeline='set -o pipefail; '+command+' | python3 -I '+shlex.quote(str(helper))+' filter '+shlex.quote(profile)+' '+kind+' '+token
    output={'hookEventName':'PreToolUse','updatedInput':dict(original,command=pipeline)}
    # Codex requires allow for updatedInput, but still performs native exec approval.
    if agent=='codex':output['permissionDecision']='allow'
    return {'hookSpecificOutput':output}

def filter_stdin(profile,kind,token=None):
    token=token or uuid.uuid4().hex
    # Stream through unchanged after the bound; never truncate an original.
    raw=sys.stdin.buffer.read(MAX_OUTPUT+1)
    if len(raw)>MAX_OUTPUT:
        count=len(raw);h=hashlib.sha256(raw)
        sys.stdout.buffer.write(raw)
        while True:
            b=sys.stdin.buffer.read(65536)
            if not b:
                try:metric_update(profile,token,mode='basic',captured=True,inputBytes=count,filteredBytes=count,filteredHash=h.hexdigest())
                except Exception:pass
                return
            count+=len(b);h.update(b);sys.stdout.buffer.write(b)
    shown=raw
    try:
        d=chat(profile)
        if read(d/'mode.json',{}).get('enabled')is not True:raise ValueError('DISABLED')
        raw.decode('utf-8')
        diagnostics=re.sub(rb'(?i)\b0 (?:failed|errors|failures|warnings)\b',b'',raw)
        if re.search(rb'(?im)\b(?:fatal|error|errors|warning|warnings|failed|failure|exception|traceback|panic|assertionerror)\b',diagnostics):raise ValueError('KEEP_DIAGNOSTICS')
        if b'\0'in raw:raise ValueError('BINARY')
        env={'PATH':'/usr/bin:/bin','HOME':str(d),'XDG_CONFIG_HOME':str(d/'config'),'XDG_DATA_HOME':str(d/'data'),'RTK_TELEMETRY_DISABLED':'1','RTK_DB_PATH':str(d/'tracking.db')}
        binary=component()/'rtk'
        if binary.is_symlink() or digest(binary)!=PIN['binarySha256']:raise ValueError('BINARY_CHANGED')
        if kind in ('read','build-preview'):
            # The original command has already run under native permission checks.
            # RTK only reads our captured stdout, never re-executes a Gradle task.
            with tempfile.TemporaryDirectory(prefix='.preview-',dir=d) as tmp:
                captured=Path(tmp)/'stdout.txt';captured.write_bytes(raw)
                result=subprocess.run([str(binary),'read','--max-lines','160',str(captured)],capture_output=True,timeout=10,env=env,cwd=d)
            if kind=='build-preview' and result.returncode==0:
                result.stdout+=b'\n[Final build output]\n'+b'\n'.join(raw.splitlines()[-40:])+b'\n'
        else:
            result=subprocess.run([str(binary),'pipe','--filter',kind],input=raw,capture_output=True,timeout=10,env=env,cwd=d)
        if result.returncode!=0:raise ValueError('FILTER_FAILED')
        output=directory(d/'originals')/(uuid.uuid4().hex+'.txt')
        hint=('\n[RTK full output: '+str(output)+']\n').encode()
        candidate=result.stdout+hint
        if len(candidate)<len(raw):
            with output.open('xb')as f:f.write(raw)
            output.chmod(0o600);shown=candidate

    except Exception:shown=raw
    try:
        # Native Claude strips trailing whitespace before returning Bash output.
        # Match a measured hash, and apply the SAME presentation to the base.
        normalizations={hashlib.sha256(shown.rstrip(b'\n')).hexdigest():len(raw.rstrip(b'\n'))}
        try:normalizations[hashlib.sha256(shown.decode('utf-8').rstrip().encode('utf-8')).hexdigest()]=len(raw.decode('utf-8').rstrip().encode('utf-8'))
        except UnicodeError:pass
        metric_update(profile,token,mode='basic',captured=True,inputBytes=len(raw),rawInputBytes=len(raw),filteredBytes=len(shown),filteredHash=hashlib.sha256(shown).hexdigest(),normalizations=normalizations)
    except Exception:pass
    sys.stdout.buffer.write(shown)

@contextlib.contextmanager
def state_lock(profile):
    fd=os.open(chat(profile)/'state.lock',os.O_CREAT|os.O_RDWR|os.O_NOFOLLOW,0o600)
    with os.fdopen(fd,'w') as lock:
        fcntl.flock(lock,fcntl.LOCK_EX);yield

def main(q):
    try:
        if platform.machine()not in ['aarch64','arm64']or not Path('/etc/os-release').is_file()or 'ID=ubuntu'not in Path('/etc/os-release').read_text():raise ValueError('UNSUPPORTED_ENVIRONMENT')
        profile=q['profile'];run=q['run'];d=chat(profile);op=q['op']
        if op=='begin':
            with state_lock(profile):write(d/'attempt.json',{'id':q['attempt'],'cancelled':False,'previous':read(d/'mode.json',{'enabled':False,'mode':'off'})})
        elif op=='cancel':
            with state_lock(profile):
                a=read(d/'attempt.json',{})
                if a.get('id')==q['attempt']:write(d/'attempt.json',dict(a,cancelled=True));write(d/'mode.json',a.get('previous',{'enabled':False,'mode':'off'}))
            emit(ok=True);return
        elif op=='install':cancelled(q);installation(q)
        elif op=='enable':
            if not verify(component()):raise ValueError('RTK_NOT_INSTALLED')
            refresh_helper(q.get('_source',''))
            with state_lock(profile):
                cancelled(q)
                if not q.get('deferred') and (not ready(profile,run) or read(d/'ready.json',{}).get('componentVersion')!=PIN['version']):raise ValueError('RESTART_REQUIRED')
                mode=q.get('mode','basic')
                if mode not in ('basic','advanced'):raise ValueError('INVALID_MODE')
                if mode=='advanced' and q.get('confirmed') is not True:raise ValueError('CONFIRMATION_REQUIRED')
                if mode=='advanced' and not q.get('deferred') and not read(d/'ready.json',{}).get('nativeDispatch'):raise ValueError('RESTART_REQUIRED')
                write(d/'mode.json',{'enabled':True,'mode':mode})
                write(d/'hook-error.json',{})
        elif op=='disable':
            with state_lock(profile):write(d/'mode.json',{'enabled':False,'mode':'off'})
        elif op!='status':raise ValueError('INVALID_OPERATION')
        elif verify(component()):refresh_helper(q.get('_source',''))
        emit(result=status(profile,run),ok=True)
    except Exception as e:
        code=str(e)
        if not re.fullmatch('[A-Z_]{1,60}',code):code='NETWORK_ERROR'if isinstance(e,(OSError,urllib.error.URLError))else'RTK_FAILED'
        emit(ok=False,error=code)

if __name__=='__main__':
    if len(sys.argv)>=2 and sys.argv[1]=='native':
        profile=os.environ.get('JOBS_RTK_PROFILE','');binary=component()/'rtk'
        if binary.is_symlink() or digest(binary)!=PIN['binarySha256']:raise SystemExit('RTK binary verification failed')
        env=native_env(profile)
        # One private sidecar per invocation; exec preserves native exit/signal behavior.
        env.pop('JOBS_RTK_BYTES_FILE',None);env.pop('JOBS_RTK_BYTES_PID',None)
        try:
            metrics=directory(chat(profile)/'native-bytes-v1');name=uuid.uuid4().hex
            write(metrics/(name+'.meta.json'),{'run':os.environ.get('JOBS_RTK_RUN',''),'createdAt':time.time_ns(),'argsHash':hashlib.sha256(json.dumps(sys.argv[2:],separators=(',',':')).encode()).hexdigest()})
            env['JOBS_RTK_BYTES_FILE']=str(metrics/(name+'.result.json'));env['JOBS_RTK_BYTES_PID']=str(os.getpid())
        except (OSError,ValueError):pass
        os.execve(binary,[str(binary),*sys.argv[2:]],env)
    elif len(sys.argv)in (4,5) and sys.argv[1]=='filter':filter_stdin(sys.argv[2],sys.argv[3],sys.argv[4] if len(sys.argv)==5 else None)
    elif len(sys.argv)==5 and sys.argv[1]=='hook':
        try:print(json.dumps(hook(json.load(sys.stdin),*sys.argv[2:])))
        except Exception:print('{}')
