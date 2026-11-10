from pathlib import Path
import subprocess,tarfile,hashlib,json,shutil,os
root=Path.cwd();out=root/'.verification/release-reproduction-v2.1.8';source=out/'source';out.mkdir(parents=True,exist_ok=True);source.mkdir(exist_ok=False)
ref=os.environ.get('REPRO_SOURCE_REF','v2.1.8');sha=subprocess.check_output(['git','rev-parse',ref+'^{commit}'],text=True).strip()
archive=out/'source.tar'
with archive.open('wb') as f:subprocess.run(['git','archive',sha],stdout=f,check=True)
with tarfile.open(archive) as t:t.extractall(source,filter='data')
assert not (source/'.signing').exists()
# Cache only pinned downloaded inputs, never previous compiled project outputs.
cache=source/'.tools/termux-x11-downloads';cache.mkdir(parents=True)
for f in (root/'.tools/termux-x11-downloads').iterdir():
 if f.is_file() and not f.name.endswith('.partial'):shutil.copyfile(f,cache/f.name)
# Minimal private SDK view. Existing tools/platform files are read, not installed.
sdk=source/'.tools/sdk';sdk.mkdir()
for name in ['platforms','build-tools','licenses','platform-tools']:
 if (root/'.tools/sdk'/name).exists():(sdk/name).symlink_to(root/'.tools/sdk'/name,target_is_directory=True)
env=dict(os.environ,FORK_X11_CLANG='/data/data/com.termux/files/usr/bin/clang')
with (out/'x11-prepare.log').open('w') as f:subprocess.run(['python3','scripts/prepare-x11.py'],cwd=source,env=env,stdout=f,stderr=subprocess.STDOUT,check=True)
subprocess.run(['python3','scripts/prepare-java-verification.py','--arm64'],cwd=source,check=True)
stage=source/'.verification/arm64';native=out/'native';native.mkdir()
bootstrap=root/'.tools/downloads/bootstrap-aarch64.zip';assert hashlib.sha256(bootstrap.read_bytes()).hexdigest()=='c8d702b6f742935001c37cda81b8ac69504a95d5cf28f2899532dd8cd4b057eb'
shutil.copyfile(bootstrap,stage/'app/src/main/cpp/bootstrap-aarch64.zip')
sysroot=Path('/data/data/com.termux/files/home/Android/Sdk/ndk/28.1.13356709/toolchains/llvm/prebuilt/linux-x86_64/sysroot');assert sysroot.is_dir()
clang='/data/data/com.termux/files/usr/bin/clang';lld='/data/data/com.termux/files/usr/bin/ld.lld'
flags=['--target=aarch64-linux-android24','--sysroot='+str(sysroot),'-std=c11','-Wall','-Wextra','-Werror','-Os','-fPIC','-fno-stack-protector']
commands=[]
def run(args,cwd=source):
 commands.append({'argv':args,'cwd':str(cwd)});subprocess.run(args,cwd=cwd,check=True)
run([clang,*flags,'-c','terminal-emulator/src/main/jni/termux.c','-o',str(native/'termux.o')])
run([clang,*flags,'-c','app/src/main/cpp/termux-bootstrap.c','-o',str(native/'bootstrap.o')])
run([clang,'--target=aarch64-linux-android24','-c','termux-bootstrap-zip.S','-o',str(native/'bootstrap-zip.o')],stage/'app/src/main/cpp')
lib=sysroot/'usr/lib/aarch64-linux-android/24'
for filename,objs,module in [('libtermux.so',['termux.o'],'terminal-emulator'),('libtermux-bootstrap.so',['bootstrap.o','bootstrap-zip.o'],'app')]:
 run([lld,'-shared','--no-undefined','--gc-sections','-z','now','-z','noexecstack','-z','max-page-size=16384','-soname',filename,str(lib/'crtbegin_so.o'),*[str(native/n) for n in objs],'-L',str(lib),'-lc','-ldl',str(lib/'crtend_so.o'),'-o',str(native/filename)])
 dest=stage/module/'src/main/jniLibs/arm64-v8a';dest.mkdir(parents=True,exist_ok=True);shutil.copyfile(native/filename,dest/filename)
(out/'native-commands.json').write_text(json.dumps(commands,indent=2))
(out/'source-identity.json').write_text(json.dumps({'tag':'v2.1.8','commit':sha,'sourceArchiveSha256':hashlib.sha256(archive.read_bytes()).hexdigest(),'releaseKeyPresent':False,'sysroot':str(sysroot),'compiler':subprocess.check_output([clang,'--version'],text=True)},indent=2))
print('Clean source, fresh own native builds and pinned external inputs prepared:',stage)
