#!/usr/bin/env python3
"""Compare every APK ZIP entry; report signature differences, never mask payloads."""
import argparse, hashlib, json, zipfile
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('published',type=Path);p.add_argument('rebuilt',type=Path);p.add_argument('output',type=Path);a=p.parse_args()
def sha(b):return hashlib.sha256(b).hexdigest()
def inventory(path):
 with zipfile.ZipFile(path) as z:
  assert z.testzip() is None
  assert len(z.namelist())==len(set(z.namelist())), 'Duplicate ZIP entry'
  return {i.filename:dict(sha256=sha(z.read(i)),size=i.file_size,compressedSize=i.compress_size,compression=i.compress_type,date=list(i.date_time),extra=i.extra.hex(),offset=i.header_offset) for i in z.infolist()}
x,y=inventory(a.published),inventory(a.rebuilt)
r=dict(published=dict(sha256=sha(a.published.read_bytes()),size=a.published.stat().st_size),rebuilt=dict(sha256=sha(a.rebuilt.read_bytes()),size=a.rebuilt.stat().st_size),onlyPublished=sorted(x.keys()-y.keys()),onlyRebuilt=sorted(y.keys()-x.keys()),changed=[n for n in sorted(x.keys()&y.keys()) if x[n]['sha256']!=y[n]['sha256']],sameCount=sum(x[n]['sha256']==y[n]['sha256'] for n in x.keys()&y.keys()),publishedEntries=x,rebuiltEntries=y)
a.output.write_text(json.dumps(r,indent=2)+'\n');print(json.dumps({k:v for k,v in r.items() if not k.endswith('Entries')},indent=2))
