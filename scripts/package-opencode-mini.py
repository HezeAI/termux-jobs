#!/usr/bin/env python3
"""Package an independently built v2.0.28/jobs2 mini into repository-only assets."""
import argparse,gzip,hashlib,json,tarfile
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--binary',type=Path,required=True);p.add_argument('--license',type=Path,required=True);a=p.parse_args()
root=Path(__file__).resolve().parents[1];assets=root/'.verification/opencode-mini-jobs2-archive';assets.mkdir(parents=True,exist_ok=True)
for file in (a.binary,a.license):
 if not file.resolve().is_relative_to(root):raise SystemExit('Inputs must be staged inside repository')
asset=assets/'opencode-mini-v2.0.28-jobs2.bundle';manifest=assets/'opencode-mini-v2.0.28-jobs2.json'
previous=json.loads((manifest if manifest.exists() else root/'patches/opencode-mini-jobs2/opencode-mini-v2.0.28-jobs2.json').read_text())
with asset.open('wb') as f:
 with gzip.GzipFile(fileobj=f,mode='wb',compresslevel=1,mtime=0) as gz:
  with tarfile.open(fileobj=gz,mode='w|') as tar:
   for source,name in ((a.binary,'opencode-mini'),(a.license,'LICENSE')):
    entry=tar.gettarinfo(str(source),arcname=name);entry.uid=entry.gid=entry.mtime=0;entry.uname=entry.gname='';entry.mode=0o700 if name=='opencode-mini' else 0o600
    with source.open('rb') as data:tar.addfile(entry,data)
sha=lambda file:hashlib.sha256(file.read_bytes()).hexdigest()
value={'version':'2.0.28','revision':'jobs2','upstreamCommit':'026b106bc5d41b679ffc413264229c340871e4d3','binarySha256':sha(a.binary),'bundleSha256':sha(asset),'binaryBytes':a.binary.stat().st_size,'bundleBytes':asset.stat().st_size}
manifest.write_text(json.dumps(value,indent=2)+'\n')
installer=assets/'opencode-mini-v2.0.28-jobs2.sh'
text=(installer if installer.exists() else root/'patches/opencode-mini-jobs2/opencode-mini-v2.0.28-jobs2.sh').read_text()
for key in ('binarySha256','bundleSha256'):
 if previous.get(key):text=text.replace(previous[key],value[key])
installer.write_text(text)
# Keep installer constants and the generated manifest consistent.
print(json.dumps(value,indent=2))
