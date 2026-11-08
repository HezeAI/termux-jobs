"""Read attachment references from one saved native journal, without launching a CLI.
Bounded pages and explicit errors; no authentication or transcript persistence.
"""
import json,os,re,sys,sqlite3,stat,urllib.parse
from contextlib import closing
from pathlib import Path

def rows(value, key):
    out=[]
    def walk(v,role='agent'):
        if isinstance(v,list):
            for item in v:walk(item,role)
        elif isinstance(v,dict):
            role='user' if v.get('role')=='user' or v.get('type')=='user' else role
            if v.get('name') in ('Read','view_image','read_file'):
                args=v.get('input',v.get('arguments',v.get('args',{})))
                if isinstance(args,str):
                    try:args=json.loads(args)
                    except ValueError:args={}
                path=args.get('file_path',args.get('path','')) if isinstance(args,dict) else ''
                if isinstance(path,str) and path.startswith('/'):
                    out.append(dict(key=key+':'+str(len(out)),message=key,kind='text',messageSender='agent',fileReferences=[dict(path=path,name=Path(path).name,localFileLink=True)]))
            if v.get('type')=='file' and isinstance(v.get('url'),str) and v['url'].startswith('file:///'):
                path=urllib.parse.unquote(urllib.parse.urlparse(v['url']).path)
                out.append(dict(key=key+':'+str(len(out)),message=key,kind='text',messageSender=role,fileReferences=[dict(path=path,name=v.get('filename',Path(path).name),localFileLink=True)]))
            for k,item in v.items():
                if k in ('text','body','content') and isinstance(item,str):
                    # Only reference-bearing text reaches Android, never base64 or a full transcript.
                    snippets=re.findall(r'\[TermuxJobs attachment:[a-f0-9]{32}\]\r?\nName: [^\r\n]+\r?\nFile: /[^\r\n]+\r?\nSize: [0-9]+ bytes\.|!?\[[^\]\r\n]{0,200}\]\((?:<[^<>\r\n]+>|[^\r\n)]+)\)',item)
                    if snippets:out.append(dict(key=key+':'+str(len(out)),message=key,kind='user' if role=='user' else 'text',body='\n'.join(snippets),messageSender=role))
                elif k not in ('data','source','encrypted_content'):walk(item,role)
    walk(value)
    return out

def main(q):
    agent=q['agent'];thread=q['thread'];offset=int(q.get('offset',0));home=Path.home()
    if not re.fullmatch(r'[A-Za-z0-9_-]{1,120}',thread) or offset<0:raise ValueError('INVALID_SAVED_CHAT')
    if agent=='claude':files=list((home/'.claude/projects').glob('*/'+thread+'.jsonl'))
    elif agent=='codex':files=list((home/'.codex/sessions').glob('*/*/*/*'+thread+'.jsonl'))
    elif agent=='gemini':
        files=[]
        for candidate in (home/'.gemini/tmp').glob('*/chats/session-*'+thread[:8]+'*.json*'):
            if candidate.is_symlink():continue
            # Native filenames may contain only the first eight UUID characters.
            # Verify the top-level session ID before reading message references.
            with candidate.open('rb') as f:header=f.read(65536).decode('utf-8','replace')
            match=re.search(r'"sessionId"\s*:\s*"([a-fA-F0-9-]{36})"',header)
            if match and match[1]==thread:files.append(candidate)
    elif agent=='opencode':
        database=home/'.local/share/opencode/opencode.db'
        if not database.is_file():raise FileNotFoundError('SAVED_HISTORY_NOT_FOUND')
        if database.is_symlink():raise ValueError('UNSAFE_HISTORY_PATH')
        with closing(sqlite3.connect(database.as_uri()+'?mode=ro',uri=True,timeout=2)) as db:
            parts=db.execute('SELECT id,data FROM part WHERE session_id=? ORDER BY time_created,id LIMIT 51 OFFSET ?', (thread,offset)).fetchall()
        result=[]
        for key,raw in parts[:50]:result.extend(rows(json.loads(raw),key))
        return dict(ok=True,rows=result,next=offset+50 if len(parts)>50 else None)
    else:raise ValueError('OFFLINE_NATIVE_HISTORY_UNSUPPORTED')
    if not files:raise FileNotFoundError('SAVED_HISTORY_NOT_FOUND')
    if len(files)!=1:raise ValueError('AMBIGUOUS_SAVED_HISTORY')
    path=files[0]
    if path.is_symlink():raise ValueError('UNSAFE_HISTORY_PATH')
    result=[];next_offset=None
    with path.open('rb') as f:
        if not stat.S_ISREG(os.fstat(f.fileno()).st_mode):raise ValueError('NOT_REGULAR_HISTORY')
        if path.suffix=='.json':
            if path.stat().st_size>16*1024*1024:raise ValueError('HISTORY_RECORD_TOO_LARGE')
            result=rows(json.load(f),'saved')
            result=result[offset:offset+50];next_offset=offset+50 if len(result)==50 else None
        else:
            f.seek(offset);count=0;size=0
            while count<300 and size<512*1024:
                line=f.readline(16*1024*1024+1)
                if not line:break
                if len(line)>16*1024*1024:raise ValueError('HISTORY_RECORD_TOO_LARGE')
                count+=1;size+=len(line)
                try:value=json.loads(line)
                except ValueError:continue
                result.extend(rows(value,str(f.tell())))
            if f.tell()<os.fstat(f.fileno()).st_size:next_offset=f.tell()
    return dict(ok=True,rows=result,next=next_offset)
if __name__=='__main__':
    try:print(json.dumps(main(json.loads(sys.argv[1]))))
    except (OSError,ValueError,KeyError,sqlite3.Error) as e:print(json.dumps(dict(ok=False,error=str(e))))
