#!/usr/bin/env python3
"""Fetch pinned X11 build inputs into repo-local caches; never install packages."""
from pathlib import Path, PurePosixPath
import hashlib, io, json, re, subprocess, tarfile, urllib.request, zipfile
ROOT=Path(__file__).resolve().parents[1]
COMPONENTS=ROOT/'.tools/x11-components'
def verified_file(name,record):
    cache=ROOT/'.tools/termux-x11-downloads';cache.mkdir(parents=True,exist_ok=True)
    file=cache/name
    if not file.exists():
        temp=file.with_suffix('.partial')
        try:
            with urllib.request.urlopen(record['url'],timeout=60) as src,temp.open('wb') as dst:
                while chunk:=src.read(1024*1024):dst.write(chunk)
            if hashlib.sha256(temp.read_bytes()).hexdigest()!=record['sha256']:raise ValueError('Pinned '+name+' checksum mismatch; upstream nightly may have changed')
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
    subprocess.run(['clang','--target=aarch64-linux-musl','-fPIC','-shared','-nostdlib','-O2','-Wall','-Wextra','-Werror','-I',str(headers/'usr/include'),'-Wl,-z,now,-z,relro,-z,noexecstack','-Wl,-soname,x11-alpine-compat.so',str(ROOT/'x11/src/main/cpp/x11-alpine-compat.c'),'-o',str(assets/'x11-alpine-compat.so')],check=True)
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

def main():
    pins=json.loads((ROOT/'third-party/termux-x11/provenance.json').read_text())
    apk=verified_file('termux-x11-universal-debug.apk',pins['apk']);deb=verified_file('xkeyboard-config.deb',pins['xkb'])
    out=ROOT/'.tools/x11-runtime';lib=out/'jniLibs/arm64-v8a';assets=out/'assets';lib.mkdir(parents=True,exist_ok=True);assets.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(apk) as z:
        if pins['commit'].encode() not in z.read('classes.dex'):raise ValueError('Java/native provenance mismatch')
        native=z.read('lib/arm64-v8a/libXlorie.so')
        if native[:4]!=b'\x7fELF' or int.from_bytes(native[18:20],'little')!=183:raise ValueError('Not ARM64 ELF')
        (lib/'libXlorie.so').write_bytes(native)
    parts=subprocess.check_output(['ar','t',str(deb)],text=True).splitlines()
    part=next(p for p in parts if p.startswith('data.tar.'))
    tar=tarfile.open(fileobj=io.BytesIO(subprocess.check_output(['ar','p',str(deb),part])))
    base='data/data/com.termux/files/usr/share/xkeyboard-config-2/'
    entries={}
    for m in tar:
        name=m.name.removeprefix('./')
        if name.startswith(base) and m.isfile():
            rel=name[len(base):]
            if PurePosixPath(rel).is_absolute() or '..' in PurePosixPath(rel).parts:raise ValueError('Unsafe keyboard path')
            entries[rel]=tar.extractfile(m).read()
        if name.endswith('/share/doc/xkeyboard-config/copyright') and m.isfile():entries['COPYRIGHT']=tar.extractfile(m).read()
    if not all(n in entries for n in ['rules/base','keycodes/evdev','symbols/us']):raise ValueError('Incomplete keyboard data')
    with zipfile.ZipFile(assets/'x11-keyboard.zip','w',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as z:
        for name,data in sorted(entries.items()):
            info=zipfile.ZipInfo(name,date_time=(2026,1,1,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;info.external_attr=0o100644<<16;z.writestr(info,data)
    # Failed trial is kept in source but must not ship executable guest code.
    (assets/'libgarcon-1.so.0').unlink(missing_ok=True)

    subprocess.run(["python3","-B",str(ROOT/"scripts/prepare-x11-style.py")],check=True,env=dict(__import__("os").environ,XDG_CACHE_HOME=str(ROOT/".tools/font-cache")))
    style_pin=re.search(r"^PIN='([a-f0-9]{64})'$",(ROOT/'x11/src/main/assets/x11-style.py').read_text(),re.M)
    if not style_pin or hashlib.sha256((assets/'x11-style.zip').read_bytes()).hexdigest()!=style_pin[1]:
        raise ValueError('Generated style differs from its reviewed runtime pin')
    COMPONENTS.mkdir(parents=True,exist_ok=True)
    (assets/'x11-style.zip').replace(COMPONENTS/'x11-style.zip')
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
    subprocess.run(['cc','-fPIC','-shared','-nostdlib','-O2','-Wall','-Wextra','-Werror',
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
    # Finished diagnostics/failed trial: never ship or compile the guest modules.
    for name in ['x11-gtk-observe.so','x11-garcon-source.zip']:(assets/name).unlink(missing_ok=True)
    report=dict(commit=pins['commit'],nativeSha256=hashlib.sha256(native).hexdigest(),keyboardSha256=hashlib.sha256((assets/'x11-keyboard.zip').read_bytes()).hexdigest(),keyboardFiles=len(entries))
    (out/'verified.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
if __name__=='__main__':main()
