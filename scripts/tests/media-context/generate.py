"""Deterministic artificial media only; never reads a user image/history."""
from pathlib import Path
from PIL import Image
import random,json,hashlib,sys,zipfile,struct
base=Path(sys.argv[1]);base.mkdir(parents=True,exist_ok=True);rows=[]
for fmt,ext,size,count in [('PNG','png',(600,600),20),('JPEG','jpg',(2048,1536),1),('WEBP','webp',(1280,720),1),('PNG','png',(32,32),24),('JPEG','jpg',(32,32),24),('WEBP','webp',(32,32),24)]:
 for i in range(count):
  p=base/f'{ext}-{size[0]}-{i}.{ext}';im=Image.frombytes('RGB',size,random.Random(f'{fmt}:{size}:{i}').randbytes(size[0]*size[1]*3));im.save(p,format=fmt,**({'quality':90} if fmt=='JPEG' else {'lossless':True} if fmt=='WEBP' else {}));rows.append({'file':p.name,'format':fmt,'width':size[0],'height':size[1]})
for ext,prefix in [('log','SYNTHETIC_LOG'),('py','print("SYNTHETIC_CODE") #'),('json','')]:
 p=base/f'large.{ext}';s=(prefix+'x'*100+'\n')*16000 if ext!='json' else json.dumps({'synthetic':True,'values':['x'*100]*16000});p.write_text(s);rows.append({'file':p.name})
p=base/'binary.zip'
with zipfile.ZipFile(p,'w',zipfile.ZIP_DEFLATED) as z:z.writestr(zipfile.ZipInfo('synthetic.txt',(1980,1,1,0,0,0)),b'SYNTHETIC_ZIP_DATA_ONLY\n'*100)
rows.append({'file':p.name,'kind':'valid artificial ZIP'})
p=base/'binary.stl';p.write_bytes(b'SYNTHETIC_STL'.ljust(80,b' ')+struct.pack('<I',1)+struct.pack('<12fH',0,0,1,0,0,0,1,0,0,0,1,0,0));rows.append({'file':p.name,'kind':'binary STL, one triangle'})
p=base/'binary.blend';p.write_bytes(b'BLENDER-v300\0'+random.Random('blend').randbytes(4096));rows.append({'file':p.name,'kind':'opaque BLEND-signature stub, not a valid Blender project/parser test'})
for row in rows:
 p=base/row['file'];row.update(bytes=p.stat().st_size,sha256=hashlib.sha256(p.read_bytes()).hexdigest())
(base/'manifest.json').write_text(json.dumps(rows,indent=2)+'\n');print(json.dumps({'files':len(rows),'bytes':sum(r['bytes'] for r in rows)}))
