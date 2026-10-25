#!/usr/bin/env python3
"""Build the pinned private Android Wayland core into repository-only outputs."""
from pathlib import Path
import os,re,shutil,subprocess,hashlib,json,zipfile,urllib.request
R=Path(__file__).resolve().parents[1];B=R/'.tools/wayland-build';U=R/'third-party/droiddeck-wayland/upstream';S=B/'source';OUT=R/'.tools/wayland-runtime/jniLibs/arm64-v8a';OUT.mkdir(parents=True,exist_ok=True);(B/'build').mkdir(parents=True,exist_ok=True)
for entry in json.loads((U.parent/'upstream.json').read_text())['files']:
 p=U/entry['path']
 if p.is_symlink() or hashlib.sha256(p.read_bytes()).hexdigest()!=entry['sha256']:raise ValueError('Wayland upstream integrity: '+entry['path'])
for entry in json.loads((U.parent/'build-headers.json').read_text())['files']:
 p=U.parent/'build-headers'/entry['path']
 if p.is_symlink() or hashlib.sha256(p.read_bytes()).hexdigest()!=entry['sha256']:raise ValueError('Wayland header integrity')
# Build from verified pinned input, never patch the pristine source snapshot.
shutil.copytree(U,S,dirs_exist_ok=True)
cpp=S/'app/src/main/cpp';core=cpp/'waylandcomp'
compositor=core/'src/compositor.c';text=compositor.read_text()
a=text.index('static void open_session_log(void) {');b=text.index('\n}',a)+2
text=text[:a]+'static void open_session_log(void) { /* Jobs: diagnostics stay app-private and opt-in. */ }'+text[b:]
# Do not send application/window/text payloads to logcat or shared-storage logs.
a=text.index('void droiddeck_log(');b=text.index('\n}',a)+2
text=text[:a]+'void droiddeck_log(const char *tag, const char *fmt, ...) { (void)tag; (void)fmt; }'+text[b:]
# A listening socket must not disguise an unusable Vulkan renderer.
text=text.replace('int droiddeck_wayland_run(void) {', '\nstatic atomic_int jobs_startup_state;\nint jobs_wayland_state(void) { return atomic_load(&jobs_startup_state); }\nint droiddeck_wayland_run(void) {\n    atomic_store(&jobs_startup_state, 1);')
text=text.replace('if (vkp_ready() != 0) WLOGE("renderer unavailable; clients will connect but nothing is drawn");', 'if (vkp_ready() != 0) { atomic_store(&jobs_startup_state, -1); return 1; }')
text=text.replace('WLOGE("wl_display_create failed");', 'atomic_store(&jobs_startup_state, -2); WLOGE("wl_display_create failed");')
text=text.replace('WLOGE("add_socket(wayland-0) failed', 'atomic_store(&jobs_startup_state, -3); wl_display_destroy(display); WLOGE("add_socket(wayland-0) failed')
text=text.replace('    start_test_input(wl_display_get_event_loop(display));', '    /* Jobs: no production test-input FIFO. */')
text=text.replace('    wl_display_run(display);', '    atomic_store(&jobs_startup_state, 2);\n    wl_display_run(display);\n    atomic_store(&jobs_startup_state, 3);')
compositor.write_text(text)
jni=core/'src/waylandcomp_jni.c'
jni.write_text(jni.read_text()+'\nextern int jobs_wayland_state(void);\nJNIEXPORT jint JNICALL Java_com_droiddeck_launcher_wayland_WaylandCompositor_nativeStartupState(JNIEnv *env, jclass cls) { (void)env; (void)cls; return jobs_wayland_state(); }\n')
jni.write_text(jni.read_text()+'''
#include <sys/file.h>
#include <sys/stat.h>
#include <fcntl.h>
static int jobs_driver_lock = -1;
JNIEXPORT jboolean JNICALL Java_com_droiddeck_launcher_wayland_WaylandCompositor_nativeLockDriver(JNIEnv *env, jclass cls, jstring path) {
    (void)cls; if(jobs_driver_lock >= 0 || !path) return JNI_FALSE;
    const char *p=(*env)->GetStringUTFChars(env,path,NULL); if(!p) return JNI_FALSE;
    int fd=open(p,O_RDONLY|O_CLOEXEC|O_NOFOLLOW);(*env)->ReleaseStringUTFChars(env,path,p);
    if(fd<0) return JNI_FALSE;
    struct stat st; if(fstat(fd,&st)!=0 || !S_ISREG(st.st_mode) || st.st_uid!=getuid() || st.st_nlink!=1 || flock(fd,LOCK_SH|LOCK_NB)!=0){close(fd);return JNI_FALSE;}
    jobs_driver_lock=fd;return JNI_TRUE;
}
''')
# Advertise the exact same KGSL device identity as the guest's private DRM view.
dmabuf=core/'src/wl_dmabuf.c';data=dmabuf.read_text();data=data.replace('{"/dev/dri/renderD128", "/dev/dri/renderD129", "/dev/dri/card0"}', '{"/dev/kgsl-3d0"}');dmabuf.write_text(data)
loader=core/'src/vk_loader.c';text=loader.read_text().replace('    if (!g_handle) {\n        g_handle = dlopen', '    if (driver_path && library_name && !g_handle) return -1;\n    if (!g_handle) {\n        g_handle = dlopen');loader.write_text(text)
# JNI stays source-compatible; app adapter will live under this original private class.
wrapper=B/'wrapper.zip'
if not wrapper.exists():wrapper.write_bytes(urllib.request.urlopen('https://github.com/sabamdarif/termux-desktop/releases/download/bionic-vulkan-wrapper-0.0.5r5/vulkan-wrapper-android-leegaos-fork-aarch64.zip',timeout=40).read())
assert hashlib.sha256(wrapper.read_bytes()).hexdigest()=='c50f3a9f884ca2c54ca76d4eb55709a4012cff77ff279216a0f04920c05bcc55'
wanted={'libadrenotools.so','libfile_redirect_hook.so','libgsl_alloc_hook.so','libhook_impl.so','libmain_hook.so'}
with zipfile.ZipFile(wrapper) as z:
 for info in z.infolist():
  if Path(info.filename).name in wanted:(OUT/Path(info.filename).name).write_bytes(z.read(info))
# Read pinned distribution packages; do not copy unverified installed libraries.
import io,tarfile
for record in json.loads((U.parent/'runtime-packages.json').read_text()):
 package=B/Path(record['Filename']).name
 if not package.exists():
  with urllib.request.urlopen('https://packages.termux.dev/apt/termux-main/'+record['Filename'],timeout=40) as response:data=response.read(int(record['Size'])+1)
  if len(data)!=int(record['Size']) or hashlib.sha256(data).hexdigest()!=record['SHA256']:raise ValueError('Runtime package checksum')
  package.write_bytes(data)
 data=package.read_bytes()
 if len(data)!=int(record['Size']) or hashlib.sha256(data).hexdigest()!=record['SHA256']:raise ValueError('Runtime package integrity')
 part=next(x for x in subprocess.check_output(['ar','t',str(package)],text=True).splitlines() if x.startswith('data.tar.'))
 licenses=R/'.tools/wayland-runtime/assets/licenses';licenses.mkdir(parents=True,exist_ok=True)
 with tarfile.open(fileobj=io.BytesIO(subprocess.check_output(['ar','p',str(package),part]))) as tar:
  for member in tar:
   if member.isfile() and Path(member.name).name in ('libffi.so','libc++_shared.so'):(OUT/Path(member.name).name).write_bytes(tar.extractfile(member).read())
   if member.isfile() and member.name.endswith('/share/doc/'+record['Package']+'/copyright'):(licenses/('Wayland-'+record['Package'].replace('+','p')+'-copyright.txt')).write_bytes(tar.extractfile(member).read())

shutil.copyfile(core/'prebuilt/lib/libwayland-server.so',OUT/'libwayland-server.so')
# The compositor's only optional AdrenoTools helper is our existing library's
# turbo call; preserve upstream API when present, no change to its driver loading.
exports=subprocess.check_output(['readelf','-Ws',str(OUT/'libadrenotools.so')],text=True)
if 'adrenotools_set_turbo' not in exports:
 (B/'build/adreno-turbo-stub.c').write_text('void adrenotools_set_turbo(int enabled) {(void)enabled;}\n')
# All current Wayland protocol sources from pinned upstream.
cmake=(core/'CMakeLists.txt').read_text();part=cmake[cmake.index('add_library(droiddeckwayland SHARED'):cmake.index('target_compile_options')]
sources=re.findall(r'(?:src|generated)/[a-zA-Z0-9_-]+\.c',part)+['src/framegen_engine_stub.c']
sourcepaths=[core/p for p in sources]
if 'adrenotools_set_turbo' not in exports:sourcepaths.append(B/'build/adreno-turbo-stub.c')
include=R/'third-party/droiddeck-wayland/build-headers'
prefix=Path('/data/data/com.termux/files/usr')
flags=['clang','--target=aarch64-linux-android30','--sysroot='+str(prefix),'-fPIC','-O2','-std=gnu11','-mno-outline-atomics','-fvisibility=hidden','-Wall','-Wextra','-Wno-unused-parameter','-Wno-unused-function','-Wno-missing-field-initializers','-I'+str(prefix/'include/aarch64-linux-android'),'-I'+str(core/'generated'),'-I'+str(core/'prebuilt/include'),'-I'+str(core/'src'),'-I'+str(cpp/'shaders'),'-I'+str(include)]
objects=[]
for source in sourcepaths:
 obj=B/'build'/(source.stem+'.o');subprocess.run(flags+['-c',str(source),'-o',str(obj)],check=True);objects.append(obj)
# Reuse the native toolchain's CRT, keep all outputs and caches in this workspace.
link=['clang','--target=aarch64-linux-android30','--sysroot='+str(prefix),'-shared','-nostdlib','-Wl,--no-undefined,-z,relro,-z,now,-z,noexecstack,-z,max-page-size=16384,-soname,libjobs-wayland.so',str(prefix/'lib/crtbegin_so.o'),*[str(p) for p in objects],'-L'+str(OUT),'-lwayland-server','-ladrenotools','/system/lib64/libc.so','/system/lib64/libdl.so','/system/lib64/libm.so','/system/lib64/liblog.so','/system/lib64/libandroid.so',str(prefix/'lib/crtend_so.o'),'-o',str(OUT/'libjobs-wayland.so')]
subprocess.run(link,check=True)
manifest={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in OUT.glob('*.so')}
(R/'.tools/wayland-runtime/native-sha256.json').write_text(json.dumps(manifest,indent=2)+'\n')
print('Built',OUT/'libjobs-wayland.so')

# Guest glibc compatibility is an asset, never an Android JNI library.
assets=R/'.tools/wayland-runtime/assets';assets.mkdir(parents=True,exist_ok=True)
guest=R/'third-party/droiddeck-wayland/guest'
for item in json.loads((guest/'upstream.json').read_text())['files']:
 p=guest/Path(item['path']).name
 if hashlib.sha256(p.read_bytes()).hexdigest()!=item['sha256']:raise ValueError('Guest source integrity')
drm=B/'build/guest-drm.c';drm.write_text((guest/'drm.c').read_text().replace('kgsl_dev = st.st_rdev;', 'kgsl_dev = kgsl_state > 0 ? st.st_rdev : 0;'))
subprocess.run(['/usr/bin/cc','-O2','-fPIC','-shared','-Wall','-Wextra','-Wl,--no-undefined,-z,now,-z,relro,-z,noexecstack',str(drm),str(guest/'udevmon.c'),str(R/'x11/src/main/cpp/wayland-guest.c'),'-ldl','-lpthread','-o',str(assets/'wayland-guest.so')],check=True)
blob=(assets/'wayland-guest.so').read_bytes()
if blob[:4]!=b'\x7fELF' or int.from_bytes(blob[18:20],'little')!=183:raise ValueError('Guest ABI must be glibc ARM64')
for name in ['LICENSE']:(assets/'licenses'/('DroidDeck-'+name+'.txt')).write_bytes((U/name).read_bytes())
# XKB layout data is already a pinned input of the X11 keyboard package.
with zipfile.ZipFile(R/'.tools/x11-runtime/assets/x11-keyboard.zip') as z:(assets/'licenses/Wayland-keyboard-COPYRIGHT.txt').write_bytes(z.read('COPYRIGHT'))

(assets/'licenses/Wayland-Vulkan-Headers-Apache-2.0.txt').write_bytes((U.parent/'Vulkan-Headers-LICENSE.txt').read_bytes())
for path,expected in json.loads((R/'x11/wayland-inputs.json').read_text()).items():
 if hashlib.sha256((R/'.tools/wayland-runtime'/path).read_bytes()).hexdigest()!=expected:raise ValueError('Wayland output changed: review pins before APK build: '+path)

for relative,name in [('app/src/main/cpp/adrenotools/LICENSE','Wayland-AdrenoTools-LICENSE.txt'),('app/src/main/cpp/adrenotools/lib/linkernsbypass/LICENSE','Wayland-linkernsbypass-LICENSE.txt')]:
 (assets/'licenses'/name).write_bytes((U/relative).read_bytes())

for entry in json.loads((U.parent/'license-sources.json').read_text()):
 data=(U.parent/entry['path']).read_bytes()
 if hashlib.sha256(data).hexdigest()!=entry['sha256']:raise ValueError('Wayland license integrity')
 (assets/'licenses'/entry['path']).write_bytes(data)
