#!/usr/bin/env python3
"""Package the privately rebuilt Manjaro bridge without modifying other runtimes."""
from pathlib import Path
import hashlib,json,re,subprocess,zipfile
ROOT=Path(__file__).resolve().parents[1]
WORK=ROOT/'.tools/manjaro-bridge'
OUT=ROOT/'third-party/bionic-glibc'
MESA=WORK/'mesa-stage/opt/termux-jobs-bionic-mesa/lib'
meta=json.loads((OUT/'provenance.json').read_text())
with zipfile.ZipFile(OUT/'ubuntu-arm64-runtime.zip') as old:
    assert hashlib.sha256((OUT/'ubuntu-arm64-runtime.zip').read_bytes()).hexdigest()==meta['bundle']['sha256']
    files={n:old.read(n) for n in ('bionic-icd.json','share/vulkan/implicit_layer.d/jobs-bionic.json')}
inputs={n:WORK/'runtime'/n for n in ('libjobs-bionic.so','libhybris-common.so.1','linker/q.so','libVkLayer_window_system_integration.so')}
inputs.update({f'mesa/{n}':MESA/n for n in ('libGLX_mesa.so.0','libEGL_mesa.so.0','libgallium-26.2.5.so')})
package=WORK/'stripped';package.mkdir(exist_ok=True)
for name,path in inputs.items():
    p=package/Path(name).name;p.write_bytes(path.read_bytes())
    subprocess.run([str(WORK/'bin/strip'),'--strip-unneeded',str(p)],check=True)
    data=p.read_bytes()
    if data[:7]!=b'\x7fELF\x02\x01\x01' or int.from_bytes(data[18:20],'little')!=183:raise ValueError('Not ARM64 ELF: '+name)
    text=subprocess.check_output(['readelf','-V',str(p)],text=True)
    if 'GLIBC_PRIVATE' in text:raise ValueError('Private glibc ABI in '+name)
    for prefix,maximum in [('GLIBC_',(2,35)),('GLIBCXX_',(3,4,30)),('CXXABI_',(1,3,13))]:
        for value in re.findall(r'Name: '+prefix+r'([0-9.]+)',text):
            if tuple(map(int,value.split('.')))>maximum:raise ValueError('Incompatible symbol version '+prefix+value+' in '+name)
    if b'JOBS_TEST_' in data:raise ValueError('Development tracing in '+name)
    files[name]=data
archive=OUT/'manjaro-arm64-runtime.zip'
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
    for name,data in sorted(files.items()):
        info=zipfile.ZipInfo(name,(2026,10,29,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;info.external_attr=0o100600<<16;z.writestr(info,data)
sha=hashlib.sha256(archive.read_bytes()).hexdigest();size=archive.stat().st_size
helper=ROOT/'x11/src/main/assets/x11-bionic-glibc.py';s=helper.read_text();s=re.sub(r"^MANJARO_PIN = '[^']+'$",f"MANJARO_PIN = '{sha}'",s,flags=re.M);s=re.sub(r'^MANJARO_SIZE = [0-9]+$',f'MANJARO_SIZE = {size}',s,flags=re.M);helper.write_text(s)
meta.update(name='Bionic for glibc — Manjaro ARM build',scope='Manjaro ARM only; private runtime and driver cache',requirements={'arch':'aarch64','libc':'glibc >= 2.35','cxx':'GLIBCXX_3.4.30 / CXXABI_1.3.13','android':'API >=29; device verification pending'})
meta['toolchain']={'gcc':'12.1.0-2.1','binutils':'2.38-5.1','sysroot':'Manjaro ARM glibc 2.35-5.1','packages':'manjaro-build-packages.json'}
meta['bundle']={'file':archive.name,'bytes':size,'sha256':sha,'files':{n:hashlib.sha256(d).hexdigest() for n,d in files.items()}}
meta['mesa'].pop('compatibilitySource',None);meta['mesa'].pop('compatibilitySourceSHA256',None)
meta['evidence']='docs/fork/VALIDATION-test6-manjaro-bridge.md'
(OUT/'manjaro-provenance.json').write_text(json.dumps(meta,indent=2,ensure_ascii=False)+'\n')
print(json.dumps(meta['bundle'],indent=2))
