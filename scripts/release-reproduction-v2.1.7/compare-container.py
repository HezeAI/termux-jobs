#!/usr/bin/env python3
"""Compare unsigned container bytes after removing only a validated APK signing block.
Does not write either APK. This does not verify the cryptographic signature;
run apksigner separately. No ZIP entry/content is removed or rewritten.
"""
import argparse,hashlib,json,struct
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('published',type=Path);p.add_argument('rebuilt',type=Path);p.add_argument('output',type=Path);a=p.parse_args()
b=a.published.read_bytes();e=b.rfind(b'PK\x05\x06')
assert e>=0 and e+22+struct.unpack_from('<H',b,e+20)[0]==len(b), 'Invalid EOCD'
cd=struct.unpack_from('<I',b,e+16)[0]
assert b[cd:cd+4]==b'PK\x01\x02' and b[cd-16:cd]==b'APK Sig Block 42', 'Missing signing block'
n=struct.unpack_from('<Q',b,cd-24)[0];start=cd-n-8
assert start>=0 and struct.unpack_from('<Q',b,start)[0]==n, 'Signing block size mismatch'
ids=[];pos=start+8
while pos<cd-24:
 size=struct.unpack_from('<Q',b,pos)[0];assert size>=4 and pos+8+size<=cd-24
 ids.append(hex(struct.unpack_from('<I',b,pos+8)[0]));pos+=8+size
assert pos==cd-24 and '0x7109871a' in ids, 'Expected v2 signature'
x=bytearray(b[:start]+b[cd:]);struct.pack_into('<I',x,e-(cd-start)+16,start)
r=dict(signingBlockBytes=cd-start,ids=ids,publishedWithoutSigningBlockSha256=hashlib.sha256(x).hexdigest(),equalsRebuilt=x==a.rebuilt.read_bytes())
a.output.write_text(json.dumps(r,indent=2)+'\n');print(json.dumps(r,indent=2))
raise SystemExit(0 if r['equalsRebuilt'] else 1)
