"""Bounded, non-executing skill package manager. No credentials or agent processes."""
import ctypes
import difflib
import fcntl
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import shutil
import stat
import tempfile
import urllib.parse
import urllib.request
import uuid
import zipfile

MAX_BYTES = 32 * 1024 * 1024
MAX_FILES = 512
MAX_FILE = 2 * 1024 * 1024
NAME = re.compile(r'[a-z0-9][a-z0-9-]{0,63}\Z')

class SkillError(Exception):
    pass

def fail(code):
    raise SkillError(code)

def safe_path(p):
    p = Path(p)
    if not p.is_absolute() or '..' in p.parts:
        fail('UNSAFE_PATH')
    for item in [*reversed(p.parents), p]:
        if item.is_symlink():
            fail('UNSAFE_LINK')
    return p

def read(p, limit=MAX_FILE):
    safe_path(p)
    fd = os.open(p, os.O_RDONLY | os.O_NOFOLLOW)
    with os.fdopen(fd, 'rb') as stream:
        st = os.fstat(stream.fileno())
        if not stat.S_ISREG(st.st_mode) or st.st_nlink != 1 or st.st_size > limit:
            fail('UNSAFE_FILE')
        b = stream.read(limit + 1)
        if len(b) > limit:
            fail('SIZE_LIMIT')
        return b

def atomic(p, value):
    safe_path(p)
    p.parent.mkdir(parents=True, exist_ok=True)
    fd, tmp = tempfile.mkstemp(prefix='.write-', dir=p.parent)
    try:
        with os.fdopen(fd, 'wb') as out:
            out.write(json.dumps(value, ensure_ascii=False).encode()); out.flush(); os.fsync(out.fileno())
        os.replace(tmp, p)
        sync(p.parent)
    finally:
        if os.path.exists(tmp): os.unlink(tmp)

def sync(p):
    fd = os.open(p, os.O_RDONLY | os.O_DIRECTORY)
    try: os.fsync(fd)
    finally: os.close(fd)

def inventory(root):
    root = safe_path(root)
    if not root.is_dir(): fail('NOT_A_DIRECTORY')
    result = {}; size = 0; directories = 0
    for base, dirs, files in os.walk(root, followlinks=False):
        if len(Path(base).relative_to(root).parts) > 16: fail('DEPTH_LIMIT')
        dirs[:] = [name for name in dirs if name != '.git']
        directories += len(dirs)
        if directories > MAX_FILES or len(files) > MAX_FILES: fail('SIZE_LIMIT')
        for name in dirs:
            p = Path(base) / name
            if p.is_symlink(): fail('UNSAFE_LINK')
        for name in files:
            p = Path(base) / name
            if name in ('auth.json', 'credentials.json', '.env') or name.startswith('.env.'):
                fail('SENSITIVE_FILE')
            data = read(p)
            rel = p.relative_to(root).as_posix()
            size += len(data)
            if len(result) >= MAX_FILES or size > MAX_BYTES: fail('SIZE_LIMIT')
            result[rel] = hashlib.sha256(data).hexdigest()
    return result

def fingerprint(files):
    return hashlib.sha256(json.dumps(files, sort_keys=True).encode()).hexdigest()

def copy_tree(src, dst):
    expected = inventory(src)
    dst.mkdir(mode=0o700)
    for name, checksum in expected.items():
        p = dst / name; p.parent.mkdir(parents=True, exist_ok=True)
        b = read(src / name)
        if hashlib.sha256(b).hexdigest() != checksum: fail('SOURCE_CHANGED')
        with open(p, 'xb') as out:
            out.write(b); out.flush(); os.fsync(out.fileno())
        p.chmod((src/name).stat().st_mode & 0o777)  # Preserve executable flags without executing.
    if inventory(dst) != expected: fail('SOURCE_CHANGED')
    return expected

def metadata(root):
    raw = read(root / 'SKILL.md', 128 * 1024).decode('utf-8').replace('\r\n','\n')
    if '\x00' in raw or not raw.startswith('---\n'): fail('INVALID_METADATA')
    parts = re.split(r'\n---[ \t]*(?:\n|$)',raw,maxsplit=1)
    if len(parts) != 2: fail('INVALID_METADATA')
    header = parts[0][4:]; values = {}; lines = header.splitlines(); i = 0
    # Safe scalar subset; arbitrary YAML is not evaluated or falsely certified.
    while i < len(lines):
        line = lines[i]; i += 1
        if not line or line.startswith('#') or line[0].isspace(): continue
        m = re.fullmatch(r'([A-Za-z][\w-]*):\s*(.*)', line)
        if not m: fail('UNSUPPORTED_METADATA')
        key, value = m.groups()
        if key in values: fail('INVALID_METADATA')
        if value in ('>', '|', '>-', '|-'):
            block = []
            while i < len(lines) and (not lines[i] or lines[i][0].isspace()):
                block.append(lines[i].strip()); i += 1
            value = (' ' if value.startswith('>') else '\n').join(block)
        elif value.startswith('"'):
            try: value = json.loads(value)
            except ValueError: fail('UNSUPPORTED_METADATA')
        elif value.startswith("'") and value.endswith("'"):
            value = value[1:-1].replace("''", "'")
        elif key in ('name', 'description') and (value in ('true','false','null','~') or value.startswith(("'",'&','*','!','[','{')) or ': ' in value or any(x in value for x in (' &', ' *', '!','{','['))):
            fail('UNSUPPORTED_METADATA')
        values[key] = value
    name = values.get('name', root.name)
    if not isinstance(name, str) or not NAME.fullmatch(name): fail('INVALID_NAME')
    description = values.get('description', '')
    if not isinstance(description, str) or not description.strip() or len(description) > 4096: fail('INVALID_DESCRIPTION')
    declaration=header
    if (root/'agents/openai.yaml').exists():
        declaration += '\n\n--- agents/openai.yaml ---\n'+read(root/'agents/openai.yaml',128*1024).decode('utf-8')
    return {'name': name, 'description': description, 'text': raw, 'declarations': declaration,
            'metadataValidation': 'safe-scalar-subset', 'dependencyCheck': 'not-performed',
            'functionalCheck': 'not-performed'}

def unpack(source, dest, subdirectory=""):
    with zipfile.ZipFile(source) as archive:
        infos = archive.infolist()
        if len(infos) > 10000: fail('SIZE_LIMIT')
        prefix = (PurePosixPath(infos[0].filename).parts[0]+'/'+subdirectory.strip('/')+'/') if subdirectory and infos else ''
        selected = []
        names = set(); total = 0
        for item in infos:
            p = PurePosixPath(item.filename)
            mode = item.external_attr >> 16
            if (not item.filename or '\x00' in item.orig_filename or '\\' in item.filename or p.is_absolute() or '..' in p.parts or
                len(p.parts) > 17 or item.filename in names or item.flag_bits & 1 or
                stat.S_ISLNK(mode) or stat.S_IFMT(mode) not in (0, stat.S_IFREG, stat.S_IFDIR)):
                fail('UNSAFE_ARCHIVE')
            names.add(item.filename)
            if prefix and not item.filename.startswith(prefix): continue
            if '.git' in p.parts: continue
            if p.name in ('auth.json','credentials.json','.env') or p.name.startswith('.env.'): fail('SENSITIVE_FILE')
            selected.append(item)
            if len(selected)>MAX_FILES*2: fail('SIZE_LIMIT')
            total += item.file_size
            if item.file_size > MAX_FILE or total > MAX_BYTES or item.file_size > max(1048576, item.compress_size * 300): fail('SIZE_LIMIT')
        for item in selected:
            target = dest / item.filename
            if item.is_dir(): target.mkdir(parents=True, exist_ok=True); continue
            target.parent.mkdir(parents=True, exist_ok=True)
            with archive.open(item) as stream, open(target, 'xb') as out:
                count = 0
                while True:
                    b = stream.read(65536)
                    if not b: break
                    count += len(b)
                    if count > MAX_FILE: fail('SIZE_LIMIT')
                    out.write(b)
            target.chmod((item.external_attr >> 16) & 0o777 or 0o600)

class PublicRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        validate_url(newurl)
        return super().redirect_request(req, fp, code, msg, headers, newurl)

def validate_url(url):
    u = urllib.parse.urlsplit(url)
    if u.scheme != 'https' or u.hostname not in ('api.github.com', 'codeload.github.com', 'github.com') or u.username or u.password or u.port not in (None,443): fail('PUBLIC_GITHUB_ONLY')

def fetch(url):
    validate_url(url)
    req = urllib.request.Request(url, headers={'User-Agent': 'Termux-Jobs-skills', 'Accept':'application/vnd.github+json'})
    # No credential store, git credential helper, netrc or user-supplied headers.
    with urllib.request.build_opener(PublicRedirect()).open(req, timeout=25) as response:
        b = response.read(MAX_BYTES + 1)
        if len(b) > MAX_BYTES: fail('SIZE_LIMIT')
        return b

class Manager:
    def __init__(self, q):
        self.q = q; self.agent = q.get('agent', 'codex')
        if self.agent not in ('codex','claude') or q.get('environment','ubuntu-ai') not in ('ubuntu-ai','ubuntu','termux') or self.agent == 'claude' and q.get('environment') == 'termux': fail('INVALID_ENVIRONMENT')
        self.home = safe_path(Path.home())
        self.project = safe_path(Path(q.get('project', str(self.home))))
        if len(self.project.parts)>64 or len(str(self.project))>4096: fail('PROJECT_PATH_LIMIT')
        self.config = safe_path(Path(os.environ.get('CODEX_HOME', str(self.home/'.codex'))) if self.agent == 'codex' else Path(os.environ.get('CLAUDE_CONFIG_DIR',str(self.home/'.claude'))))
        self.scope = q.get('scope','user')
        if self.scope not in ('user','project'): fail('INVALID_SCOPE')
        self.root = ((self.home/'.agents' if self.agent=='codex' else self.config) if self.scope=='user' else self.project/('.agents' if self.agent=='codex' else '.claude'))/'skills'
        safe_path(self.root)
        self.state = self.root.parent/'.termux-jobs-skills'
        self.staging = safe_path(self.home/'.local/share/termux-jobs/skill-previews')
    def roots(self, include_project=True):
        result = [(self.home/'.agents/skills' if self.agent=='codex' else self.config/'skills','user')]
        if self.agent=='codex': result += [(self.config/'skills','legacy'),(Path('/etc/codex/skills'),'admin')]
        if include_project:
            for p in [self.project,*self.project.parents]:
                result.append((p/('.agents' if self.agent=='codex' else '.claude')/'skills','project' if p==self.project else 'ancestor'))
                if (p/'.git').exists(): break
        # Plugin directories are listed separately only when actual package SKILL.md is present.
        result.append((self.config/'plugins/cache','plugin'))
        return list(dict.fromkeys(result))
    def record(self, target):
        p = target.parent.parent/'.termux-jobs-skills'/target.name/'record.json'
        if not p.exists(): return None
        r = json.loads(read(p))
        if r.get('path') != str(target) or r.get('manager') != 'termux-jobs-v1': fail('INVALID_RECORD')
        return r
    def list(self):
        rows=[]; seen=set(); errors=[]
        roots = self.roots(include_project=self.scope == "project")
        for root, scope in roots:
            if not root.exists(): continue
            try:
                safe_path(root)
                for base, dirs, files in os.walk(root,followlinks=False):
                    depth=len(Path(base).relative_to(root).parts)
                    dirs[:]=[d for d in dirs if not (Path(base)/d).is_symlink() and (depth<6 if scope=='plugin' else depth<2)]
                    if 'SKILL.md' not in files: continue
                    folder=Path(base)
                    if str(folder) in seen: continue
                    seen.add(str(folder))
                    if len(rows)>=256: fail('LIST_LIMIT')
                    item={'origin':('plugin' if scope=='plugin' else 'cli-system' if '.system' in folder.relative_to(root).parts else 'external'),'path':str(folder),'scope':scope,'agent':self.agent,'managed':False,'discovery':'unconfirmed','functionalCheck':'not-performed'}
                    try:
                        item.update(metadata(folder)); record=self.record(folder)
                        if record:
                            item.update(managed=folder.parent==self.root,source=record['source'],commit=record.get('commit',''),rollback=bool(record.get('previous')),revision=fingerprint(record['files']),expected=fingerprint(record['files']),requestedRef=record.get('requestedRef',''),subdirectory=record.get('subdirectory',''))
                        else: item.update(source='unknown')
                    except Exception as e: item.update(error=str(e) if isinstance(e,SkillError) else 'UNREADABLE',name=folder.name)
                    item.pop('text',None); item.pop('declarations',None)
                    if len(json.dumps(rows+[item],ensure_ascii=False).encode())>1024*1024: fail('LIST_LIMIT')
                    rows.append(item)
            except Exception as e: errors.append({'path':str(root),'code':str(e) if isinstance(e,SkillError) else 'UNREADABLE'})
        for row in rows:
            row['nameConflicts']=[x['path'] for x in rows if x.get('name')==row.get('name') and x['path']!=row['path']]
        rows=[x for x in rows if (x['scope'] in ('project','ancestor')) == (self.scope=='project')]
        pending=[]
        if self.staging.is_dir() and not self.staging.is_symlink():
            pending=[{'token':p.name,'path':str(p)} for p in self.staging.iterdir() if re.fullmatch('[a-f0-9]{32}',p.name) and p.is_dir() and not p.is_symlink()]
        recovery=[]
        if self.state.is_dir() and not self.state.is_symlink():
            recovery=[{'name':p.name,'path':str(self.root/p.name)} for p in self.state.iterdir() if NAME.fullmatch(p.name) and p.is_dir() and not p.is_symlink() and (p/'transaction.json').is_file()]
        return {'recovery':recovery,'pending':pending,'skills':rows,'roots':[{'path':str(p),'scope':s} for p,s in roots], 'target':str(self.root),'errors':errors}
    def prepare(self):
        safe_path(self.staging); self.staging.mkdir(parents=True,exist_ok=True)
        if len(list(self.staging.iterdir()))>=8: fail('PREVIEW_LIMIT')
        token=uuid.uuid4().hex; work=self.staging/token; work.mkdir(mode=0o700); content=work/'content'; content.mkdir()
        source=self.q.get('source',''); kind=self.q.get('sourceKind','folder'); commit=''; ref=''; sub=''
        try:
            if kind=='create':
                name=self.q.get('name',''); description=self.q.get('description',''); body=self.q.get('instructions','')
                if not isinstance(name,str) or not NAME.fullmatch(name): fail('INVALID_NAME')
                if not isinstance(description,str) or not description.strip() or len(description)>1024 or '\x00' in description: fail('INVALID_DESCRIPTION')
                if not isinstance(body,str) or not body.strip() or '\x00' in body: fail('INSTRUCTIONS_REQUIRED')
                text='---\nname: '+name+'\ndescription: '+json.dumps(description,ensure_ascii=False)+'\n---\n\n'+body
                if not text.endswith('\n'): text+='\n'
                if len(text.encode('utf-8'))>128*1024: fail('SIZE_LIMIT')
                folder=content/name; folder.mkdir()
                with open(folder/'SKILL.md','x',encoding='utf-8') as out:
                    out.write(text); out.flush(); os.fsync(out.fileno())
                source='created-in-app'
            elif kind=='github':
                u=urllib.parse.urlsplit(source); parts=u.path.strip('/').split('/')
                if u.scheme!='https' or u.netloc!='github.com' or len(parts)<2 or not all(re.fullmatch(r'[\w.-]+',x) for x in parts[:2]): fail('PUBLIC_GITHUB_ONLY')
                repo='/'.join(parts[:2]); ref=self.q.get('ref','').strip(); sub=self.q.get('subdirectory','').strip('/')
                if len(parts)>2:
                    if parts[2]!='tree' or len(parts)<4: fail('INVALID_GITHUB_URL')
                    if not ref: ref=parts[3]
                    if not sub: sub='/'.join(parts[4:])
                if not ref:
                    info=json.loads(fetch('https://api.github.com/repos/'+repo)); ref=info['default_branch']
                data=json.loads(fetch('https://api.github.com/repos/'+repo+'/commits/'+urllib.parse.quote(ref,safe='')))
                commit=data.get('sha','')
                if not re.fullmatch('[0-9a-f]{40}',commit): fail('INVALID_COMMIT')
                archive=work/'download.zip'; archive.write_bytes(fetch('https://codeload.github.com/'+repo+'/zip/'+commit)); unpack(archive,content,sub)
                children=list(content.iterdir())
                if len(children)!=1 or not children[0].is_dir(): fail('INVALID_ARCHIVE')
                content=children[0]
                if sub:
                    if '..' in PurePosixPath(sub).parts or PurePosixPath(sub).is_absolute(): fail('UNSAFE_PATH')
                    content=safe_path(content/sub)
                source='https://github.com/'+repo
            elif kind=='zip':
                p=safe_path(Path(source))
                if p.stat().st_size>MAX_BYTES: fail('SIZE_LIMIT')
                unpack(p,content)
            elif kind=='folder': copy_tree(safe_path(Path(source)),content/'import'); content=content/'import'
            else: fail('INVALID_SOURCE')
            inventory(content); candidates=[]
            for p in sorted(content.rglob('SKILL.md')):
                folder=p.parent
                # Nested packages cannot accidentally bundle another selected skill.
                if any(a!=folder and (a/'SKILL.md').is_file() for a in folder.parents if a==content or content in a.parents): continue
                rel=folder.relative_to(work).as_posix()
                item={'candidate':rel,'path':str(self.root/folder.name)}
                try:
                    item.update(metadata(folder))
                    if self.agent=='codex' and not re.search(r'(?m)^name:',item['text'].split('\n---',1)[0]): fail('NAME_REQUIRED')
                    item['path']=str(self.root/item['name']); item['files']=inventory(folder)
                    item['scripts']=[x for x in item['files'] if x.startswith('scripts/') or x.endswith(('.sh','.py','.js','.cjs','.ps1','.bat')) or (folder/x).stat().st_mode & 0o111]
                except Exception as e: item['error']=str(e) if isinstance(e,SkillError) else 'INVALID_METADATA'
                candidates.append(item)
                if len(candidates)>64: fail('LIST_LIMIT')
            if not candidates: fail('NO_SKILLS')
            if sum(len(x.get('text','').encode())+len(x.get('declarations','').encode()) for x in candidates)>1024*1024: fail('SIZE_LIMIT')
            manifest={'agent':self.agent,'scope':self.scope,'root':str(self.root),'project':str(self.project),'source':source,'sourceKind':kind,'requestedRef':ref,'subdirectory':sub,'commit':commit,'candidates':candidates}
            atomic(work/'preview.json',manifest)
            return dict(manifest,token=token)
        except Exception:
            shutil.rmtree(work); raise
    def preview(self):
        token=self.q.get('token','')
        if not re.fullmatch('[a-f0-9]{32}',token): fail('INVALID_PREVIEW')
        work=safe_path(self.staging/token); data=json.loads(read(work/'preview.json'))
        if (data['agent'],data['scope'],data['root'],data['project'])!=(self.agent,self.scope,str(self.root),str(self.project)): fail('STALE_PREVIEW')
        candidates=[x for x in data['candidates'] if x['candidate']==self.q.get('candidate') and not x.get('error')]
        if len(candidates)!=1: fail('INVALID_PREVIEW')
        item=candidates[0]; folder=safe_path(work/item['candidate'])
        if inventory(folder)!=item['files']: fail('SOURCE_CHANGED')
        return work,data,item,folder
    def transaction(self, box):
        journal=box/'transaction.json'
        if not journal.exists(): return
        tx=json.loads(read(journal)); target=safe_path(Path(tx['record']['path']));
        if target!=self.root/box.name or tx['record'].get('manager')!='termux-jobs-v1': fail('INVALID_RECORD')
        prepared=safe_path(box/'prepared'); previous=safe_path(box/'previous')
        current=inventory(target) if target.exists() else None
        if current==tx['record']['files']:
            if prepared.exists():
                if inventory(prepared)!=tx['oldFiles']: fail('RECOVERY_CONFLICT')
                if previous.exists(): shutil.rmtree(previous)
                prepared.rename(previous)
            atomic(box/'record.json',tx['record']); journal.unlink(); sync(box)
        elif current==tx['oldFiles']:
            if prepared.exists(): shutil.rmtree(prepared)
            journal.unlink()
        else: fail('RECOVERY_CONFLICT')
    def mutate(self):
        self.state.mkdir(parents=True,exist_ok=True); safe_path(self.state)
        with open(safe_path(self.state/'lock'),'a+b') as lock:
            fcntl.flock(lock,fcntl.LOCK_EX)
            return self.mutate_locked()
    def mutate_locked(self):
        op=self.q['operation']; name=self.q.get('name','')
        if op in ('install','update'):
            _,data,item,source=self.preview(); name=item['name']
        if not NAME.fullmatch(name): fail('INVALID_NAME')
        target=safe_path(self.root/name); box=safe_path(self.state/name); box.mkdir(exist_ok=True)
        self.transaction(box)
        record=self.record(target) if target.exists() else None
        oldfiles=inventory(target) if target.exists() else None
        if op=='install' and target.exists(): fail('NAME_CONFLICT')
        if op!='install':
            if self.q.get('path',str(target))!=str(target): fail('STALE_PREVIEW')
            if not record: fail('EXTERNAL_SKILL_READ_ONLY')
            if oldfiles!=record['files'] or self.q.get('revision')!=fingerprint(oldfiles): fail('LOCAL_CHANGES')
        if op=='delete':
            # Rename out of scan first; no external symlink targets are traversed.
            tomb=box/'deleted'
            if tomb.exists(): fail('RECOVERY_REQUIRED')
            target.rename(tomb); shutil.rmtree(tomb)
            if (box/'previous').exists(): shutil.rmtree(box/'previous')
            (box/'record.json').unlink(); return {'deleted':str(target)}
        prepared=box/'prepared'
        if prepared.exists(): fail('RECOVERY_REQUIRED')
        if op=='rollback':
            if not record.get('previous') or inventory(box/'previous')!=record['previous']['files']: fail('NO_ROLLBACK')
            new=dict(record['previous']); new['previous']={k:v for k,v in record.items() if k!='previous'}
            copy_tree(box/'previous',prepared)
        else:
            new={'manager':'termux-jobs-v1','path':str(target),'source':data['source'],'commit':data['commit'],'sourceKind':data.get('sourceKind',''),'requestedRef':data.get('requestedRef',''),'subdirectory':data.get('subdirectory',''),'files':copy_tree(source,prepared)}
            if record: new['previous']={k:v for k,v in record.items() if k!='previous'}
        atomic(box/'transaction.json',{'record':new,'oldFiles':oldfiles})
        self.root.mkdir(parents=True,exist_ok=True); safe_path(self.root)
        if target.exists():
            # Linux atomic exchange avoids a missing/half-installed target after interruption.
            libc=ctypes.CDLL(None,use_errno=True)
            fn=getattr(libc,'renameat2',None)
            if fn is None: fail('ATOMIC_EXCHANGE_UNAVAILABLE')
            if fn(-100,os.fsencode(prepared),-100,os.fsencode(target),2)!=0: fail('ATOMIC_EXCHANGE_UNAVAILABLE')
        else:
            libc=ctypes.CDLL(None,use_errno=True); fn=getattr(libc,'renameat2',None)
            if fn is None or fn(-100,os.fsencode(prepared),-100,os.fsencode(target),1)!=0:
                fail('ATOMIC_INSTALL_UNAVAILABLE')
        sync(self.root); self.transaction(box)
        return {'path':str(target),'revision':fingerprint(new['files']),'discovery':'unconfirmed','functionalCheck':'not-performed'}
    def call(self):
        op=self.q.get('operation')
        if op=='list': return self.list()
        if op=='recover':
            name=self.q.get('name','')
            if not NAME.fullmatch(name): fail('INVALID_NAME')
            self.state.mkdir(parents=True,exist_ok=True); safe_path(self.state)
            with open(safe_path(self.state/'lock'),'a+b') as lock:
                fcntl.flock(lock,fcntl.LOCK_EX); self.transaction(safe_path(self.state/name))
            return {'recovered':True}
        if op=='inspect':
            target=safe_path(Path(self.q.get('path','')))
            if not any(target==root or root in target.parents for root,_ in self.roots()): fail('INVALID_SOURCE')
            result=metadata(target)
            try: result['files']=inventory(target)
            except SkillError as e: result['error']=str(e)
            return result
        if op=='prepare': return self.prepare()
        if op=='discard':
            token=self.q.get('token','')
            if not re.fullmatch('[a-f0-9]{32}',token): fail('INVALID_PREVIEW')
            p=safe_path(self.staging/token)
            if p.exists(): shutil.rmtree(p)
            return {'discarded':True}
        if op=='diff':
            _,_,item,source=self.preview(); target=safe_path(self.root/item['name']); old=inventory(target) if target.exists() else {}
            new=item['files']; changes=[n for n in sorted(set(old)|set(new)) if old.get(n)!=new.get(n)]
            before=read(target/'SKILL.md').decode() if 'SKILL.md' in old else ''
            return {'files':changes,'instructionsDiff':''.join(difflib.unified_diff(before.splitlines(True),item['text'].splitlines(True),fromfile='installed/SKILL.md',tofile='candidate/SKILL.md')),'path':str(target),'revision':fingerprint(old)}
        if op in ('install','update','delete','rollback'): return self.mutate()
        fail('INVALID_OPERATION')

def call(request):
    try: return Manager(request).call()
    except SkillError: raise
    except (zipfile.BadZipFile,UnicodeError): fail('INVALID_PACKAGE')
    except Exception: fail('SKILLS_UNAVAILABLE')
