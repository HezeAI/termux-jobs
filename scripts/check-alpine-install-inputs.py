#!/usr/bin/env python3
"""Read-only network/integrity check of pinned Alpine install archives; no installation."""
from pathlib import Path
import argparse, hashlib, json, tarfile, tempfile, urllib.request, time
ROOT=Path(__file__).resolve().parents[1]
def sha(path):
 h=hashlib.sha256()
 with path.open('rb')as f:
  while True:
   b=f.read(1024*1024)
   if not b:break
   h.update(b)
 return h.hexdigest()
def fetch(record,destination):
 for attempt in range(3):
  try:
   h=hashlib.sha256();size=0
   with urllib.request.urlopen(record['url'],timeout=45)as response,destination.open('wb')as output:
    if not response.geturl().startswith('https://'):raise ValueError('Insecure redirect')
    while True:
     b=response.read(1024*1024)
     if not b:break
     size+=len(b)
     if size>record['size']:raise ValueError('Oversized archive')
     h.update(b);output.write(b)
   if size!=record['size']or h.hexdigest()!=record['sha256']:raise ValueError('Archive identity mismatch')
   return {'url':record['url'],'bytes':size,'sha256':h.hexdigest(),'checked':True}
  except Exception:
   if attempt==2:raise
   time.sleep(1)
def main():
 parser=argparse.ArgumentParser();parser.add_argument('--output',type=Path,required=True);args=parser.parse_args()
 image=next(x for x in json.loads((ROOT/'app/src/main/assets/environment-images.json').read_text())['images']if x['alias']=='alpine')
 base=json.loads((ROOT/'x11/src/main/assets/x11-base-packages.json').read_text())['targets']['alpine']
 args.output.parent.mkdir(parents=True,exist_ok=True)
 with tempfile.TemporaryDirectory(prefix='alpine-inputs-',dir=args.output.parent)as tmp:
  folder=Path(tmp);results=[fetch(image,folder/image['filename']),fetch(base,folder/base['filename'])]
  with tarfile.open(folder/base['filename'])as tar:
   raw=tar.extractfile('base.json').read()
   if hashlib.sha256(raw).hexdigest()!=base['manifestSha256']:raise ValueError('Manifest identity mismatch')
   files=json.loads(raw)['files'];seen=set()
   for member in tar:
    if member.name=='base.json':continue
    name=member.name.removeprefix('packages/')
    if name not in files or name in seen or not member.isfile():raise ValueError('Unexpected archive member')
    h=hashlib.sha256();size=0
    with tar.extractfile(member)as source:
     while True:
      b=source.read(1024*1024)
      if not b:break
      size+=len(b);h.update(b)
    if size!=files[name]['size']or h.hexdigest()!=files[name]['sha256']:raise ValueError('Package identity mismatch')
    seen.add(name)
   if seen!=set(files):raise ValueError('Missing package')
  result={'passed':True,'scope':'Actual canonical public downloads and all packaged dependencies; no install/GUI test','downloads':results,'packageFilesVerified':len(files),'runtimeMusl':[n for n in files if n.startswith('musl-')],'ciHeaderPackageIncluded':'musl-dev-1.2.6-r2.apk'in files}
  args.output.write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result))
if __name__=='__main__':main()
