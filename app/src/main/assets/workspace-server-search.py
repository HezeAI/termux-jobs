"""Bounded read-only filename search over pinned SSH; no symlink traversal."""
import json, os, stat, sys, time

def search(req):
    raw=req.get('path',''); query=req.get('query','')
    if not isinstance(raw,str) or not raw or len(raw)>4096 or '\0' in raw: raise ValueError('INVALID_PATH')
    if not isinstance(query,str) or not query.strip() or len(query)>200 or '\0' in query: raise ValueError('INVALID_QUERY')
    root=os.path.realpath(os.path.expanduser(raw)); info=os.stat(root)
    if not stat.S_ISDIR(info.st_mode): raise ValueError('NOT_DIRECTORY')
    device=info.st_dev; stack=[(root,0)]; entries=[]; scanned=skipped=0; partial=False; started=time.monotonic(); needle=query.casefold()
    while stack:
        folder,depth=stack.pop()
        if depth>64: partial=True; continue
        # Recheck directory identity before entering; never deliberately follow symlinks.
        try:
            meta=os.lstat(folder)
            if not stat.S_ISDIR(meta.st_mode) or meta.st_dev!=device: skipped+=1;continue
            with os.scandir(folder) as listing:
                for entry in listing:
                    scanned+=1
                    if scanned>20000 or len(entries)>=500 or time.monotonic()-started>=8:
                        partial=True;stack.clear();break
                    try:
                        if entry.is_symlink(): skipped+=1;continue
                        st=entry.stat(follow_symlinks=False); directory=stat.S_ISDIR(st.st_mode)
                        if st.st_dev!=device: skipped+=1;continue
                        if needle in entry.name.casefold():
                            entries.append({'name':entry.name,'path':entry.path,'directory':directory,'size':st.st_size})
                        if directory: stack.append((entry.path,depth+1))
                    except OSError:skipped+=1
        except OSError:skipped+=1
    return {'path':root,'entries':entries,'scanned':scanned,'skipped':skipped,'partial':partial}

if __name__=='__main__':
    try: print(json.dumps({'ok':True,'result':search(json.load(sys.stdin))}))
    except Exception as exc: print(json.dumps({'ok':False,'error':str(exc) if isinstance(exc,ValueError) else 'SEARCH_FAILED'}))
