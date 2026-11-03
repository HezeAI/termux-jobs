#!/usr/bin/env python3
"""Fetch pinned X11 build inputs into repo-local caches; never install packages."""
from pathlib import Path, PurePosixPath
import argparse, hashlib, io, json, os, re, subprocess, tarfile, urllib.request, zipfile
DRIVER_ROOT=Path(__file__).resolve().parents[1]
ROOT=DRIVER_ROOT
BUILD_INPUTS=json.loads((DRIVER_ROOT/'scripts/x11-build-inputs.json').read_text())
COMPONENTS=ROOT/'.tools/x11-components'
def verified_file(name,record):
    cache=ROOT/'.tools/termux-x11-downloads';cache.mkdir(parents=True,exist_ok=True)
    file=cache/name
    if not file.exists():
        print('Fetching verified X11 input: '+name,flush=True)
        temp=file.with_suffix('.partial')
        try:
            with urllib.request.urlopen(record['url'],timeout=60) as src,temp.open('wb') as dst:
                while chunk:=src.read(1024*1024):dst.write(chunk)
            if temp.stat().st_size!=record['size'] or hashlib.sha256(temp.read_bytes()).hexdigest()!=record['sha256']:raise ValueError('Pinned '+name+' checksum mismatch; upstream nightly may have changed')
            temp.replace(file)
        finally:temp.unlink(missing_ok=True)
    if file.stat().st_size!=record['size'] or hashlib.sha256(file.read_bytes()).hexdigest()!=record['sha256']:raise ValueError('Pinned '+name+' integrity failure')
    return file

def alpine_compat(assets):
    # Headers are a pinned build input only; never install an Alpine package.
    record=dict(url='https://dl-cdn.alpinelinux.org/alpine/v3.24/main/aarch64/musl-dev-1.2.6-r2.apk',size=2539434,sha256='ca72b84f4eb3c36bbbec04d919eb10159d414c7a43413cc2253841d301fa9486')
    archive=verified_file('musl-dev-1.2.6-r2.apk',record)
    headers=ROOT/'.tools/x11-alpine'
    with tarfile.open(archive,'r:gz') as tar:
        for member in tar:
            path=PurePosixPath(member.name)
            if member.isfile() and str(path).startswith('usr/include/'):
                if path.is_absolute() or '..' in path.parts:raise ValueError('Unsafe musl header path')
                out=headers/str(path);out.parent.mkdir(parents=True,exist_ok=True);out.write_bytes(tar.extractfile(member).read())
    subprocess.run([os.environ.get('FORK_X11_CLANG','clang'),'--target=aarch64-linux-musl','-fuse-ld=lld','-fPIC','-shared','-nostdlib','-O2','-Wall','-Wextra','-Werror','-I',str(headers/'usr/include'),'-Wl,-z,now,-z,relro,-z,noexecstack','-Wl,-soname,x11-alpine-compat.so',str(ROOT/'x11/src/main/cpp/x11-alpine-compat.c'),'-o',str(assets/'x11-alpine-compat.so')],check=True)
    data=(assets/'x11-alpine-compat.so').read_bytes()
    if data[:4]!=b'\x7fELF' or int.from_bytes(data[18:20],'little')!=183:raise ValueError('Alpine compatibility is not ARM64')

def openh264(assets):
    # Real decoder for openSUSE's noopenh264 ABI stub. Extract only reviewed
    # members; never install a Debian package into openSUSE or replace its RPMs.
    record=dict(url='https://deb.debian.org/debian/pool/main/o/openh264/libopenh264-8_2.6.0+dfsg-2_arm64.deb',size=335852,sha256='dd40ffeeb006af3a610e015881340c704b8bb38627434a150b7d9e39e657dfbe')
    deb=verified_file('libopenh264-8_2.6.0+dfsg-2_arm64.deb',record)
    part=next(p for p in subprocess.check_output(['ar','t',str(deb)],text=True).splitlines() if p.startswith('data.tar.'))
    with tarfile.open(fileobj=io.BytesIO(subprocess.check_output(['ar','p',str(deb),part]))) as archive:
        entries={m.name.removeprefix('./'):m for m in archive if m.isfile()}
        library=archive.extractfile(entries['usr/lib/aarch64-linux-gnu/libopenh264.so.2.6.0']).read()
        copyright=archive.extractfile(entries['usr/share/doc/libopenh264-8/copyright']).read()
    if library[:4]!=b'\x7fELF' or int.from_bytes(library[18:20],'little')!=183:raise ValueError('OpenH264 is not ARM64')
    (assets/'x11-openh264.so').write_bytes(library)
    licenses=assets/'licenses';licenses.mkdir(exist_ok=True)
    (licenses/'OpenH264-copyright.txt').write_bytes(copyright)

def gst_openh264(assets):
    # Private Fedora Parole bridge. Never install a Debian package into a guest.
    record=dict(url='https://deb.debian.org/debian/pool/main/g/gst-plugins-bad1.0/gstreamer1.0-plugins-bad_1.26.2-3+deb13u3_arm64.deb',size=2841904,sha256='c5f1fba98072aee17e064348c9586951a506f4e0c2bff1fea3603a86e16e5590')
    deb=verified_file('gstreamer1.0-plugins-bad_1.26.2-3+deb13u3_arm64.deb',record)
    part=next(p for p in subprocess.check_output(['ar','t',str(deb)],text=True).splitlines() if p.startswith('data.tar.'))
    with tarfile.open(fileobj=io.BytesIO(subprocess.check_output(['ar','p',str(deb),part]))) as archive:
        entries={m.name.removeprefix('./'):m for m in archive if m.isfile()}
        plugin=archive.extractfile(entries['usr/lib/aarch64-linux-gnu/gstreamer-1.0/libgstopenh264.so']).read()
        copyright=archive.extractfile(entries['usr/share/doc/gstreamer1.0-plugins-bad/copyright']).read()
    if plugin[:4]!=b'\x7fELF' or int.from_bytes(plugin[18:20],'little')!=183 or hashlib.sha256(plugin).hexdigest()!='1f4c3a85e0b23d66f4f657064767900e857855e65659627ad078e69a229f1db4':raise ValueError('GStreamer OpenH264 ARM64 integrity')
    (assets/'x11-gst-openh264.so').write_bytes(plugin)
    licenses=assets/'licenses';licenses.mkdir(exist_ok=True);(licenses/'GStreamer-OpenH264-copyright.txt').write_bytes(copyright)

def glibc_headers():
    headers=ROOT/'.tools/x11-glibc'
    for name,record in BUILD_INPUTS['glibcHeaders'].items():
        deb=verified_file(name,record)
        part=next(p for p in subprocess.check_output(['ar','t',str(deb)],text=True).splitlines() if p.startswith('data.tar.'))
        with tarfile.open(fileobj=io.BytesIO(subprocess.check_output(['ar','p',str(deb),part]))) as archive:
            for member in archive:
                path=PurePosixPath(member.name.removeprefix('./'))
                if member.isfile() and str(path).startswith('usr/aarch64-linux-gnu/include/'):
                    if path.is_absolute() or '..' in path.parts:raise ValueError('Unsafe glibc header path')
                    out=headers/str(path);out.parent.mkdir(parents=True,exist_ok=True)
                    out.write_bytes(archive.extractfile(member).read())
    return headers/'usr/aarch64-linux-gnu/include'

def main():
    pins=json.loads((ROOT/'third-party/termux-x11/provenance.json').read_text())
    apk_record=dict(pins['apk'])
    mirror=BUILD_INPUTS['apkMirror']
    if any(mirror[k]!=apk_record[k] for k in ('sha256','size')):
        raise ValueError('X11 mirror differs from source provenance')
    apk_record['url']=mirror['url']
    apk=verified_file('termux-x11-universal-debug.apk',apk_record)
    out=ROOT/'.tools/x11-runtime';lib=out/'jniLibs/arm64-v8a';assets=out/'assets';lib.mkdir(parents=True,exist_ok=True);assets.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(apk) as z:
        if pins['commit'].encode() not in z.read('classes.dex'):raise ValueError('Java/native provenance mismatch')
        native=z.read('lib/arm64-v8a/libXlorie.so')
        if native[:4]!=b'\x7fELF' or int.from_bytes(native[18:20],'little')!=183:raise ValueError('Not ARM64 ELF')
        (lib/'libXlorie.so').write_bytes(native)
    # The original Termux xkb DEB URL is gone (404). Recover the exact reviewed
    # keyboard ZIP, including COPYRIGHT, from the immutable published Jobs APK.
    # Both the APK and extracted ZIP are verified, never regenerated from new xkb.
    keyboard_source=verified_file('jobs-v2.1.2-keyboard-source.apk',BUILD_INPUTS['keyboardApk'])
    with zipfile.ZipFile(keyboard_source) as z:
        keyboard=z.read('assets/x11-keyboard.zip')
    if hashlib.sha256(keyboard).hexdigest()!=BUILD_INPUTS['keyboardSha256']:
        raise ValueError('Published keyboard integrity failure')
    (assets/'x11-keyboard.zip').write_bytes(keyboard)
    with zipfile.ZipFile(io.BytesIO(keyboard)) as z:
        entries=z.namelist()
        if not all(n in entries for n in ('rules/base','keycodes/evdev','symbols/us','COPYRIGHT')):
            raise ValueError('Incomplete keyboard data or copyright')
    # Failed trial is kept in source but must not ship executable guest code.
    (assets/'libgarcon-1.so.0').unlink(missing_ok=True)

    # Consume the existing immutable component instead of host-dependent Cairo output.
    # Art regeneration remains an explicit separate prepare-x11-style.py operation.
    style_pin=re.search(r"^PIN='([a-f0-9]{64})'$",(ROOT/'x11/src/main/assets/x11-style.py').read_text(),re.M)
    style=BUILD_INPUTS['style']
    if not style_pin or style_pin[1]!=style['sha256']:
        raise ValueError('Style component differs from reviewed runtime pin')
    COMPONENTS.mkdir(parents=True,exist_ok=True)
    (COMPONENTS/'x11-style.zip').write_bytes(verified_file('x11-style.zip',style).read_bytes())
    # Retired resources must also disappear from an existing build cache.
    for retired in ('x11-touch-style.zip','x11-touch-workspace'):
        (assets/retired).unlink(missing_ok=True)
    # Reviewed minimal musl Mesa for VirGL only; never loads as an Android JNI library.
    client=(ROOT/'third-party/virgl-client/alpine-mesa-26.1.6-virgl.zip').read_bytes()
    client_pin=re.search(r"^PIN='([a-f0-9]{64})'$",(ROOT/'x11/src/main/assets/x11-virgl-client.py').read_text(),re.M)
    if not client_pin or hashlib.sha256(client).hexdigest()!=client_pin[1]:raise ValueError('VirGL client integrity')
    (COMPONENTS/'x11-virgl-alpine.zip').write_bytes(client)
    (assets/'x11-virgl-alpine.zip').unlink(missing_ok=True)
    (assets/'x11-virgl-alpine-LICENSE.txt').write_bytes((ROOT/'third-party/virgl-client/LICENSE.txt').read_bytes())
    alpine_compat(assets)
    openh264(assets)
    gst_openh264(assets)
    # A separate glibc module: never load Alpine's musl shim in Fedora.
    headers=glibc_headers()
    compiler=os.environ.get('FORK_X11_CLANG','clang')
    builtin=Path(subprocess.check_output([compiler,'-print-resource-dir'],text=True).strip())/'include'
    subprocess.run([os.environ.get('FORK_X11_CLANG','clang'),'--target=aarch64-linux-gnu','-fuse-ld=lld','-nostdinc','-isystem',str(builtin),'-isystem',str(headers),'-fPIC','-shared','-nostdlib','-O2','-Wall','-Wextra','-Werror',
        '-Wl,-z,now,-z,relro,-z,noexecstack','-Wl,-soname,x11-fedora-glycin.so',
        str(ROOT/'x11/src/main/cpp/x11-fedora-glycin.c'),'-o',str(assets/'x11-fedora-glycin.so')],check=True)
    data=(assets/'x11-fedora-glycin.so').read_bytes()
    if data[:4]!=b'\x7fELF' or int.from_bytes(data[18:20],'little')!=183:raise ValueError('Fedora compatibility is not ARM64')

    # Verify the release component, but do not embed the optional download in APKs.
    bridge=ROOT/'third-party/bionic-glibc/ubuntu-arm64-runtime.zip'
    bridge_data=bridge.read_bytes()
    bridge_pin=re.search(r"^PIN = '([a-f0-9]{64})'$",(ROOT/'x11/src/main/assets/x11-bionic-glibc.py').read_text(),re.M)
    bridge_size=re.search(r'^SIZE = ([0-9]+)$',(ROOT/'x11/src/main/assets/x11-bionic-glibc.py').read_text(),re.M)
    if not bridge_pin or not bridge_size or len(bridge_data)!=int(bridge_size[1]) or hashlib.sha256(bridge_data).hexdigest()!=bridge_pin[1]:raise ValueError('Bionic glibc bridge pin mismatch')
    (assets/'x11-bionic-glibc.zip').unlink(missing_ok=True)
    manjaro_bridge=ROOT/'third-party/bionic-glibc/manjaro-arm64-runtime.zip'
    manjaro_data=manjaro_bridge.read_bytes()
    helper=(ROOT/'x11/src/main/assets/x11-bionic-glibc.py').read_text()
    manjaro_pin=re.search(r"^MANJARO_PIN = '([a-f0-9]{64})'$",helper,re.M)
    manjaro_size=re.search(r'^MANJARO_SIZE = ([0-9]+)$',helper,re.M)
    if not manjaro_pin or not manjaro_size or len(manjaro_data)!=int(manjaro_size[1]) or hashlib.sha256(manjaro_data).hexdigest()!=manjaro_pin[1]:raise ValueError('Manjaro Bionic glibc bridge pin mismatch')
    (assets/'x11-bionic-glibc-manjaro.zip').unlink(missing_ok=True)
    # Finished diagnostics/failed trial: never ship or compile the guest modules.
    for name in ['x11-gtk-observe.so','x11-garcon-source.zip']:(assets/name).unlink(missing_ok=True)
    report=dict(commit=pins['commit'],nativeSha256=hashlib.sha256(native).hexdigest(),keyboardSha256=hashlib.sha256((assets/'x11-keyboard.zip').read_bytes()).hexdigest(),keyboardFiles=len(entries))
    report['downloadedInputs']={p.name:dict(size=p.stat().st_size,sha256=hashlib.sha256(p.read_bytes()).hexdigest()) for p in sorted((ROOT/'.tools/termux-x11-downloads').iterdir()) if p.is_file() and not p.name.endswith('.partial')}
    report['compiledInputs']={name:dict(sourceSha256=hashlib.sha256((ROOT/'x11/src/main/cpp'/name).read_bytes()).hexdigest(),outputSha256=hashlib.sha256((assets/name.replace('.c','.so')).read_bytes()).hexdigest()) for name in ('x11-alpine-compat.c','x11-fedora-glycin.c')}
    report['compiler']=subprocess.check_output([compiler,'--version'],text=True).splitlines()[0]
    report['compileFlags']='--target=aarch64-linux-{musl,gnu} -fuse-ld=lld -fPIC -shared -nostdlib -O2 -Wall -Wextra -Werror -Wl,-z,now,-z,relro,-z,noexecstack; soname=output basename; pinned target headers'
    (out/'verified.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root',type=Path,default=ROOT,help='Target source checkout (CI may verify an older source commit)')
    args=parser.parse_args()
    ROOT=args.root.resolve()
    COMPONENTS=ROOT/'.tools/x11-components'
    main()
