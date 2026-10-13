#!/usr/bin/env python3
"""Copy a frozen APK, raise only versionCode and sign it locally. Never installs."""
import argparse, copy, hashlib, os, struct, subprocess, zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CERT = 'dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644'
SHA = {
    '0.2.6': '584b6f741053736d8faa7281277e610921275a65994ab45abb7d1b835da904d3',
    '0.2.7': '82ad013fef8b31bfef285858d9b1e024f1ce7d843986eaaec1e0b03374e3920d',
    '0.2.9': 'eac92dd578a8086df0f12d68d76a16c96315f63efbfb7e7b1107e392f75e56a3',
}

def version_code(data, new_code=None):
    result = bytearray(data)
    if len(data) < 8 or struct.unpack_from('<H', data)[0] != 3:
        raise ValueError('Expected binary AndroidManifest.xml')
    offset, resources, found = struct.unpack_from('<H', data, 2)[0], [], []
    while offset < len(data):
        kind, header_size, size = struct.unpack_from('<HHI', data, offset)
        if size < 8 or offset + size > len(data): raise ValueError('Invalid manifest chunk')
        if kind == 0x180:
            resources = list(struct.unpack_from('<' + 'I' * ((size-header_size)//4), data, offset+header_size))
        elif kind == 0x102:
            attr_start, attr_size, count = struct.unpack_from('<HHH', data, offset+header_size+8)
            for index in range(count):
                attr = offset + header_size + attr_start + index * attr_size
                name = struct.unpack_from('<I', data, attr+4)[0]
                if name < len(resources) and resources[name] == 0x0101021b:
                    if data[attr+15] not in (0x10, 0x11): raise ValueError('versionCode must be integer')
                    found.append(struct.unpack_from('<I', data, attr+16)[0])
                    if new_code is not None:
                        struct.pack_into('<I', result, attr+8, 0xffffffff)
                        result[attr+15] = 0x10
                        struct.pack_into('<I', result, attr+16, new_code)
        offset += size
    if len(found) != 1: raise ValueError('Exactly one versionCode required')
    return found[0], bytes(result)

def create_unsigned(source, output, code):
    with zipfile.ZipFile(source) as original, zipfile.ZipFile(output, 'w') as target:
        target.comment = original.comment
        for entry in original.infolist():
            if entry.filename.startswith('META-INF/') and (entry.filename.endswith(('.SF','.RSA','.DSA','.EC')) or entry.filename == 'META-INF/MANIFEST.MF'): continue
            content = original.read(entry)
            if entry.filename == 'AndroidManifest.xml': _, content = version_code(content, code)
            info = copy.copy(entry)
            if info.compress_type == zipfile.ZIP_STORED:
                alignment = 16384 if info.filename.startswith('lib/') and info.filename.endswith('.so') else 4
                header_end = target.fp.tell() + 30 + len(info.filename.encode('utf-8')) + len(info.extra)
                if header_end % alignment:
                    padding = (-header_end - 4) % alignment
                    info.extra += struct.pack('<HH', 0xffff, padding) + bytes(padding)
            target.writestr(info, content)

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('version', choices=SHA)
    parser.add_argument('version_code', type=int, help='Must exceed the versionCode installed on the phone')
    parser.add_argument('--prepare-apk', action='store_true', help='Actually create and sign a local APK; never installs')
    args = parser.parse_args()
    if not (1 <= args.version_code <= 2100000000): parser.error('versionCode out of range')
    source = ROOT/'build/checkpoints'/('v'+args.version)/('Termux-Jobs-v'+args.version+'-arm64.apk')
    if hashlib.sha256(source.read_bytes()).hexdigest() != SHA[args.version]: raise RuntimeError('Frozen APK checksum mismatch')
    with zipfile.ZipFile(source) as apk: old, _ = version_code(apk.read('AndroidManifest.xml'))
    if args.version_code <= max(old,1025): parser.error('Use a code above 1025 AND above the version currently installed')
    print('Frozen version:', args.version, 'original code:', old, 'requested code:', args.version_code)
    print('All application code/resources stay from the frozen APK; only versionCode and signature change.')
    print('Library-format compatibility is NOT changed. Review docs/fork/CHECKPOINTS.md before a real rollback.')
    if not args.prepare_apk:
        print('Dry run only. Add --prepare-apk to produce a local APK. Nothing installed.'); return
    target = ROOT/'build/recovery'; target.mkdir(parents=True,exist_ok=True)
    name = 'Termux-Jobs-v'+args.version+'-recovery-'+str(args.version_code)+'-arm64.apk'
    unsigned, signed = target/(name+'.unsigned'), target/name
    if signed.exists() or unsigned.exists(): raise RuntimeError('Output exists; original files will not be overwritten')
    create_unsigned(source,unsigned,args.version_code)
    java = ROOT/'.tools/jdk/usr/lib/jvm/java-11-openjdk-arm64/bin/java'
    signer = ROOT/'.tools/sdk/build-tools/30.0.3/lib/apksigner.jar'
    env = os.environ.copy()
    # Same existing development signing configuration as app/build.gradle.
    env['TERMUX_CHECKPOINT_SIGNING_PASSWORD'] = env.get('TERMUX_FORK_KEYSTORE_PASSWORD','termux-fork-debug')
    cmd = [str(java),'-jar',str(signer),'sign','--ks',str(ROOT/'.signing/fork-debug.jks'),'--ks-key-alias','termux-fork-debug',
        '--ks-pass','env:TERMUX_CHECKPOINT_SIGNING_PASSWORD','--key-pass','env:TERMUX_CHECKPOINT_SIGNING_PASSWORD',
        '--v1-signing-enabled','false','--v2-signing-enabled','true','--out',str(signed),str(unsigned)]
    subprocess.run(cmd,env=env,check=True)
    verification = subprocess.run([str(java),'-jar',str(signer),'verify','--print-certs',str(signed)],capture_output=True,text=True,check=True)
    if CERT not in verification.stdout: raise RuntimeError('Signing certificate mismatch')
    with zipfile.ZipFile(source) as a,zipfile.ZipFile(signed) as b:
        if b.testzip() is not None: raise RuntimeError('Corrupt APK')
        new, _ = version_code(b.read('AndroidManifest.xml'))
        if new != args.version_code: raise RuntimeError('Wrong versionCode')
        for name0 in a.namelist():
            if name0 != 'AndroidManifest.xml' and not name0.startswith('META-INF/') and a.read(name0) != b.read(name0):
                raise RuntimeError('Frozen payload changed')
    digest = hashlib.sha256(signed.read_bytes()).hexdigest()
    signed.with_suffix('.apk.sha256').write_text(digest+'  '+signed.name+'\n')
    print('Prepared and verified:', signed)
    print('Nothing installed. Data compatibility must be handled before installing a rollback APK.')

if __name__ == '__main__': main()
