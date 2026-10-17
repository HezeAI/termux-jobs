#!/usr/bin/env python3
"""Drop host-only linker search paths from our Android ELF64 artifact."""
import struct,sys
from pathlib import Path
p=Path(sys.argv[1]);b=bytearray(p.read_bytes());assert b[:5]==b'\x7fELF\x02' and b[5]==1
phoff=struct.unpack_from('<Q',b,32)[0];phsize,phnum=struct.unpack_from('<HH',b,54)
for i in range(phnum):
 off=phoff+i*phsize
 if struct.unpack_from('<I',b,off)[0]==2:
  start=struct.unpack_from('<Q',b,off+8)[0];size=struct.unpack_from('<Q',b,off+32)[0]
  for d in range(start,start+size,16):
   tag,value=struct.unpack_from('<QQ',b,d)
   if tag in (15,29):struct.pack_into('<Q',b,d+8,0)
p.write_bytes(b)
