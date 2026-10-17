#!/usr/bin/env python3
"""Fetch pinned build inputs into this repository; never install system packages."""
import hashlib,json,os,tarfile,urllib.request,zipfile
from pathlib import Path
root=Path(__file__).resolve().parent.parent
out=root/'.tools/local-ai';out.mkdir(parents=True,exist_ok=True)
for item in json.loads((root/'scripts/local-ai-tools.json').read_text()):
 p=out/item['file']
 if not p.exists() or hashlib.sha256(p.read_bytes()).hexdigest()!=item['sha256']:
  part=Path(str(p)+'.download')
  with urllib.request.urlopen(item['url'],timeout=60) as src,part.open('wb') as dst:
   while b:=src.read(1024*1024):dst.write(b)
  if hashlib.sha256(part.read_bytes()).hexdigest()!=item['sha256']:part.unlink();raise SystemExit('Input checksum mismatch: '+item['file'])
  part.replace(p)
 if p.name.endswith('.tar.gz'):
  with tarfile.open(p) as t:t.extractall(out,filter='data')
 elif p.name.endswith('.whl'):
  with zipfile.ZipFile(p) as z:z.extractall(out/'cmake')
  for binary in (out/'cmake/cmake/data/bin').iterdir():binary.chmod(0o755)
 print('Verified',p.name)
