#!/usr/bin/env python3
"""Package reviewed, locally built glibc bridge binaries. No device installation."""
from pathlib import Path
import hashlib
import json
import shutil
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]
BUILD = ROOT / '.tools/bionic-bridge'
OUT = ROOT / 'third-party/bionic-glibc'
STAGE = BUILD / 'package'
STAGE.mkdir(exist_ok=True)
MESA = BUILD / 'mesa-stage/opt/termux-jobs-bionic-mesa/lib'
MESA_SHA = '5296b88a0f1e012e2cb9ada150a2bbadf728ca81e5a4fb2ab43c83a4d2158606'
if hashlib.sha256((BUILD / 'downloads/mesa-26.1.6.tar.xz').read_bytes()).hexdigest() != MESA_SHA:
    raise ValueError('Mesa source archive checksum mismatch')
inputs = {
    'libjobs-bionic.so': BUILD / 'runtime/libjobs-bionic.so',
    'libhybris-common.so.1': BUILD / 'runtime/libhybris-common.so.1',
    'linker/q.so': BUILD / 'runtime/linker/q.so',
    'libVkLayer_window_system_integration.so': BUILD / 'wsi-build/libVkLayer_window_system_integration.so',
    'mesa/libGLX_mesa.so.0': MESA / 'libGLX_mesa.so.0',
    'mesa/libEGL_mesa.so.0': MESA / 'libEGL_mesa.so.0',
    'mesa/libgallium-26.1.6.so': MESA / 'libgallium-26.1.6.so',
}
files = {}
for name, path in inputs.items():
    binary = STAGE / Path(name).name
    shutil.copyfile(path, binary)
    subprocess.run(['strip', '--strip-unneeded', str(binary)], check=True)
    files[name] = binary.read_bytes()
    if files[name][:7] != b'\x7fELF\x02\x01\x01' or int.from_bytes(files[name][18:20], 'little') != 183:
        raise ValueError('Not an ARM64 ELF: ' + name)
    if b'JOBS_TEST_' in files[name]:
        raise ValueError('Remove development-only WSI tracing before packaging: ' + name)
files['bionic-icd.json'] = (json.dumps(dict(file_format_version='1.0.0', ICD=dict(library_path='./libjobs-bionic.so', api_version='1.4.0')),indent=2)+'\n').encode()
manifest=json.loads((BUILD/'wsi-build/VkLayer_window_system_integration.json').read_text())
manifest['layer']['library_path']='../../../libVkLayer_window_system_integration.so'
manifest['layer']['device_extensions']=[e for e in manifest['layer']['device_extensions'] if 'name' in e]
manifest['layer']['enable_environment']={'JOBS_BIONIC_ENABLED':'1'}
files['share/vulkan/implicit_layer.d/jobs-bionic.json']=(json.dumps(manifest,indent=2)+'\n').encode()
packed=OUT/'ubuntu-arm64-runtime.zip'
with zipfile.ZipFile(packed,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
    for name,data in sorted(files.items()):
        info=zipfile.ZipInfo(name,(2026,10,24,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;info.external_attr=0o100600<<16;z.writestr(info,data)
sha=hashlib.sha256(packed.read_bytes()).hexdigest()
helper=ROOT/'x11/src/main/assets/x11-bionic-glibc.py'
import re
s=helper.read_text();s=re.sub(r"^PIN = '[^']+'$","PIN = '"+sha+"'",s,flags=re.M);helper.write_text(s)
provenance={
 'name':'Bionic for glibc (Experimental)', 'scope':'six ordinary glibc desktops (Ubuntu, Debian, Fedora, Arch, openSUSE, Void), separate runtime/driver caches; no vendor drivers bundled',
 'libhybris':{'url':'https://github.com/taowen/libhybris','commit':'07dfa88569d60f00e34a9f127ccae2fcc0730618','patch':'libhybris.patch'},
 'wsi':{'url':'https://github.com/xMeM/vulkan-wsi-layer','commit':'d5624d42d8b2debbd910ad25662a05c751eb38b7','patch':'wsi.patch'},
 'icd':{'source':'x11/src/bridge/bionic-icd.c','sha256':hashlib.sha256((ROOT/'x11/src/bridge/bionic-icd.c').read_bytes()).hexdigest(),'libraryFlags':['NODELETE'],'nativeBufferImport':'Qualcomm proprietary driver only; exact BGRA UNORM AHardwareBuffer format, queried import memory type/size/usage'},
 'androidHeaders':{'url':'https://github.com/Halium/android-headers','commit':'2c6ac3dcc4f8db593dd69906b0ec22822abfed91'},
 'mesa':{'url':'https://archive.mesa3d.org/mesa-26.1.6.tar.xz','sha256':MESA_SHA,'sourceChanges':[], 'compatibilitySource':'x11/src/bridge/mesa-libm-compat.c', 'compatibilitySourceSHA256':hashlib.sha256((ROOT/'x11/src/bridge/mesa-libm-compat.c').read_bytes()).hexdigest(), 'galliumDrivers':['zink'], 'vulkanDrivers':[], 'llvm':False, 'scope':'private library path for selected glibc Bionic-for-glibc desktop only; no installed library replacement'},
 'references':['https://github.com/wmww/tawc/blob/main/notes/gpu-strategy.md','https://github.com/kde-yyds/android-vulkan-bridge'],
 'bundle':{'file':packed.name,'bytes':packed.stat().st_size,'sha256':sha,'files':{n:hashlib.sha256(d).hexdigest() for n,d in files.items()}},
 'requirements':{'arch':'aarch64','libc':'glibc >= 2.38','cxx':'GLIBCXX_3.4.32','android':'API >= 29; Android 16/API 36 tested'},
 'evidence':'docs/fork/BIONIC_GLIBC.md'
}
(OUT/'provenance.json').write_text(json.dumps(provenance,indent=2)+'\n')
notice=['Bionic for glibc (Experimental)','Source pins, patches and build notes: third-party/bionic-glibc and x11/src/bridge in the Termux Jobs source repository.','libhybris upstream + TAWC/ARLinux fork; Vulkan WSI Layer by Arm and xMeM.','Our ICD, cache/environment integration and compatibility patches are provided with this fork. No Vortek/Venus or Universal driver is included.']
notice.append('Mesa 26.1.6: unmodified upstream source, private Zink/GLX/EGL build for this renderer only. Mesa and its contributors; license collection follows.')
for path in sorted(OUT.glob('LICENSE.*'))+list(OUT.glob('*LICENSE.txt')):
    notice.extend(['\n'+path.name,path.read_text()])
(ROOT/'x11/src/main/assets/x11-bionic-glibc-NOTICE.txt').write_text('\n'.join(notice))
print(json.dumps(provenance['bundle']))
