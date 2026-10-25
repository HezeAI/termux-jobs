"""Isolated failure-path checks, not evidence of KDE GUI/device operation."""
import ctypes
import importlib.util
import json
import os
from pathlib import Path
import shlex
import struct
import subprocess
import tempfile
import unittest
import zipfile

ROOT=Path(__file__).resolve().parents[2]
ASSETS=ROOT/'x11/src/main/assets'
VERIFY=ROOT/'.verification/wayland-direct/tests'
VERIFY.mkdir(parents=True,exist_ok=True)

class WaylandDirectTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory(dir=VERIFY)
        self.base=Path(self.temp.name)
    def tearDown(self):self.temp.cleanup()
    def shell_fixture(self,missing='',os_id='archarm'):
        bin=self.base/'bin';bin.mkdir();(self.base/'os-release').write_text('ID='+os_id+'\n')
        commands='startplasma-wayland kwin_wayland kwin_wayland_wrapper plasmashell dolphin konsole wayland-info Xwayland xauth dbus-run-session dbus-send dbus-monitor python3 fcitx5'.split()
        for name in commands:
            if name==missing:continue
            p=bin/name
            p.write_text("#!/bin/sh\nprintf '%s\\n' --wayland-display\n" if name=='kwin_wayland' else '#!/bin/sh\nexit 0\n');p.chmod(0o700)
        (bin/'grep').symlink_to('/usr/bin/grep')
        log=self.base/'pacman.log'
        p=bin/'pacman';p.write_text('#!/bin/sh\nprintf "%s\\n" "$*" >> '+shlex.quote(str(log))+'\n[ "${FAIL_PACKAGE_CHECK:-0}" != 1 ]\n');p.chmod(0o700)
        (bin/'update-ca-trust').symlink_to('/bin/true')
        marker=self.base/'fixture-data';marker.write_text('fixture\n')
        script=(ASSETS/'wayland-components.sh').read_text().replace('. /etc/os-release','. '+shlex.quote(str(self.base/'os-release')))
        # Substitute only fixed read-only file locations; keep production control flow.
        script=script.replace('/etc/ssl/certs/ca-certificates.crt',str(marker)).replace('/usr/share/X11/xkb/rules/evdev',str(marker))
        path=self.base/'components.sh';path.write_text(script)
        return path,dict(os.environ,PATH=str(bin)),log
    def test_clean_kde_closure_passes_without_xfce_or_host_fallback(self):
        path,env,_=self.shell_fixture()
        p=subprocess.run(['/bin/sh',str(path),'check','archlinux'],env=env,capture_output=True,text=True)
        self.assertEqual(p.returncode,0,p.stdout+p.stderr)
    def test_missing_required_binary_is_never_ready(self):
        for name in ('plasmashell','kwin_wayland_wrapper','Xwayland','fcitx5'):
            with self.subTest(name=name):
                saved=self.base;self.base=saved/name;self.base.mkdir()
                try:
                    path,env,_=self.shell_fixture(missing=name)
                    p=subprocess.run(['/bin/sh',str(path),'check','archlinux'],env=env,capture_output=True,text=True)
                    self.assertNotEqual(p.returncode,0)
                    self.assertIn('MISSING:wayland-binary:'+name,p.stdout)
                finally:self.base=saved
    def test_missing_package_and_wrong_os_are_rejected(self):
        path,env,_=self.shell_fixture(os_id='debian')
        p=subprocess.run(['/bin/sh',str(path),'check','archlinux'],env=env,capture_output=True,text=True)
        self.assertNotEqual(p.returncode,0);self.assertIn('ERROR:WAYLAND_GUEST_OS',p.stdout)
        (self.base/'os-release').write_text('ID=arch\n')
        p=subprocess.run(['/bin/sh',str(path),'check','archlinux'],env=dict(env,FAIL_PACKAGE_CHECK='1'))
        self.assertNotEqual(p.returncode,0)
    def test_install_requests_full_upgrade_and_explicit_fonts_runtime(self):
        path,env,log=self.shell_fixture()
        p=subprocess.run(['/bin/sh',str(path),'install','archlinux'],env=env,capture_output=True,text=True)
        self.assertEqual(p.returncode,0,p.stdout+p.stderr)
        commands=log.read_text().splitlines();transaction=next(x for x in commands if x.startswith('-Syu '))
        for package in ('kwin','qt6-wayland','xorg-xwayland','xkeyboard-config','noto-fonts','breeze-icons','fcitx5-qt','llvm22-libs','vulkan-icd-loader'):
            self.assertIn(package,transaction.split())
        self.assertNotIn('--overwrite',transaction);self.assertNotIn('--nodeps',transaction)
    def test_profile_directories_reject_links_and_support_traversal_only_parent(self):
        spec=importlib.util.spec_from_file_location('jobs_wayland_ready',ASSETS/'wayland-ready.py');module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
        directory=self.base/'private';module.directory(directory)
        (self.base/'link').symlink_to(directory,target_is_directory=True)
        with self.assertRaises(OSError):module.directory(self.base/'link'/'child')
        parent=self.base/'traverse';parent.mkdir();parent.chmod(0o300)
        try:self.assertEqual(module.directory(parent/'child'),parent/'child')
        finally:parent.chmod(0o700)
    def test_wayland_driver_library_cannot_write_another_renderer_cache(self):
        job=self.base/'job';job.mkdir();prefix=self.base/'usr';prefix.mkdir();cache=self.base/'workspace-x11-wrapper'
        p=subprocess.run(['python3','-B',str(ASSETS/'wayland-driver.py'),'catalog','wayland-archlinux',str(cache),'-','36','-'],env=dict(os.environ,PREFIX=str(prefix)),capture_output=True,text=True)
        self.assertNotEqual(p.returncode,0);self.assertIn('WRAPPER_CACHE_PATH',p.stdout);self.assertFalse(cache.exists())
    def test_driver_import_is_separate_hash_checked_and_cancel_safe(self):
        import fcntl
        prefix=self.base/'usr';prefix.mkdir();cache=self.base/'workspace-wayland-driver-archlinux'
        data=bytearray(64);data[:7]=b'\x7fELF\x02\x01\x01';struct.pack_into('<HH',data,16,3,183)
        archive=self.base/'driver.zip'
        with zipfile.ZipFile(archive,'w') as z:
            z.writestr('meta.json',json.dumps(dict(name='fixture',libraryName='driver.so',minApi=30)))
            z.writestr('driver.so',bytes(data))
        def run(op,source='-',cancel='-'):
            return subprocess.run(['python3','-B',str(ASSETS/'wayland-driver.py'),op,'wayland-archlinux',str(cache),str(source),'36',str(cancel)],env=dict(os.environ,PREFIX=str(prefix)),capture_output=True,text=True)
        answer=run('import',archive);self.assertEqual(answer.returncode,0,answer.stdout+answer.stderr)
        original=(cache/'current.json').read_bytes();self.assertFalse((cache/'runtime').exists())
        self.assertNotEqual(run('install').returncode,0)
        with (cache/'operation.lock').open('rb') as lock:
            fcntl.flock(lock,fcntl.LOCK_SH|fcntl.LOCK_NB)
            answer=run('import',archive);self.assertIn('WRAPPER_BUSY',answer.stdout)
        job=cache/'.job-cancel';job.mkdir();marker=job/'cancel';marker.touch()
        self.assertIn('WRAPPER_CANCELLED',run('import',archive,marker).stdout)
        self.assertEqual((cache/'current.json').read_bytes(),original)
        selected=json.loads(original);(cache/'drivers'/selected['id']/'driver.so').write_bytes(b'changed')
        self.assertIn('WRAPPER_DRIVER_CHANGED',run('check').stdout)

    def test_kde_settings_can_be_edited_but_launch_shim_cannot_be_replaced(self):
        spec=importlib.util.spec_from_file_location('jobs_wayland_config',ASSETS/'wayland-ready.py');module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
        config=self.base/'kscreenlockerrc';module.managed(config,'[Daemon]\nAutolock=false\n',defaults=True)
        config.write_text('[Daemon]\nAutolock=false\n[KDE-written-setting]\nfoo=true\n')
        module.managed(config,'[Daemon]\nAutolock=false\n',defaults=True)
        self.assertIn('KDE-written',config.read_text())
        shim=self.base/'launch';module.managed(shim,'#!/bin/sh\nexit 0\n',True);shim.write_text('changed')
        with self.assertRaises(ValueError):module.managed(shim,'#!/bin/sh\nexit 0\n',True)

    def test_kwin_gpu_timer_workaround_is_local_and_preserves_launch_arguments(self):
        from unittest.mock import patch
        spec=importlib.util.spec_from_file_location('jobs_wayland_timing',ASSETS/'wayland-ready.py');module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
        runtime=self.base/'runtime';runtime.mkdir()
        profile=dict(XDG_CONFIG_HOME=str(self.base/'config'),XDG_CACHE_HOME=str(self.base/'cache'),XDG_STATE_HOME=str(self.base/'state'),XDG_RUNTIME_DIR=str(runtime),KWIN_NO_TIMER_QUERY='0')
        with patch.dict(os.environ,profile):
            module.prepare()
            self.assertEqual(os.environ['KWIN_NO_TIMER_QUERY'],'0')
        size=self.base/'output-size';size.write_text('800 600\n')
        executable=self.base/'kwin-fixture'
        executable.write_text('#!/bin/sh\nprintf "timer=%s\\n" "$KWIN_NO_TIMER_QUERY"\nprintf "%s\\n" "$@"\n');executable.chmod(0o700)
        shim=(runtime/'launch/kwin_wayland_wrapper').read_text()
        fixture=self.base/'launch.sh';fixture.write_text(shim.replace('/run/termux-jobs-wayland/output-size',shlex.quote(str(size))).replace('/usr/bin/kwin_wayland_wrapper',shlex.quote(str(executable))))
        result=subprocess.run(['/bin/sh',str(fixture),'--xwayland'],env=dict(os.environ,KWIN_NO_TIMER_QUERY='0'),capture_output=True,text=True)
        self.assertEqual(result.returncode,0,result.stderr)
        self.assertEqual(result.stdout.splitlines(),['JOBS_X11_STARTUP=kwin-wrapper','timer=1','--wayland-display','wayland-0','--width','800','--height','600','--no-lockscreen','--xwayland'])
        size.write_text('0 600\n')
        failed=subprocess.run(['/bin/sh',str(fixture)],capture_output=True,text=True)
        self.assertNotEqual(failed.returncode,0)
        self.assertNotIn('timer=',failed.stdout)

    def test_arch_profile_lock_is_released_after_owned_guardian_stops(self):
        import shutil
        lock=self.base/'desktop-session.lock'
        command=['flock','--no-fork','--nonblocking','--conflict-exit-code','75',str(lock)]
        child=subprocess.Popen(['setsid','bash',str(ASSETS/'x11-guardian.sh'),*command,'sh','-c','echo owned-ready; exec sleep 20'],stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True)
        try:
            import select
            self.assertTrue(select.select([child.stdout],[],[],5)[0]);self.assertEqual(child.stdout.readline().strip(),'owned-ready')
            self.assertEqual(subprocess.run([*command,'true']).returncode,75)
            # Another environment's lock is independent.
            self.assertEqual(subprocess.run(['flock','--no-fork','--nonblocking',str(self.base/'debian-session.lock'),'true']).returncode,0)
            child.stdin.close();child.wait(timeout=5)
            self.assertEqual(subprocess.run([*command,'true']).returncode,0)
        finally:
            if child.poll() is None:
                import signal
                os.killpg(child.pid,signal.SIGKILL);child.wait(timeout=3)
            for pipe in (child.stdin,child.stdout,child.stderr):
                if pipe and not pipe.closed:pipe.close()

    def device_fixture(self):
        short=tempfile.TemporaryDirectory(prefix='',dir=ROOT/'.verification');self.addCleanup(short.cleanup);base=Path(short.name)
        prefix=base/'usr';prefix.mkdir();control=base/'workspace-x11-archlinux';control.mkdir()
        runtime=control/'wl-11111111-1111-1111-1111-111111111111';runtime.mkdir()
        # Mock only the read-only device identity, never rely on development GPU/rootfs.
        driver=base/'device-setup.py'
        driver.write_text("import os,runpy,types\nreal=os.stat\nos.stat=lambda p,*a,**kw: types.SimpleNamespace(st_rdev=os.makedev(226,128)) if str(p)=='/dev/kgsl-3d0' else real(p,*a,**kw)\nrunpy.run_path("+repr(str(ASSETS/'wayland-root.py'))+",run_name='__main__')\n")
        return control,runtime,['python3','-B',str(driver),'prepare-device',str(control),str(runtime)],dict(os.environ,PREFIX=str(prefix))

    def test_fresh_wayland_runtime_prepares_private_x11_ice_and_drm(self):
        control,runtime,command,env=self.device_fixture()
        result=subprocess.run(command,env=env,capture_output=True,text=True)
        self.assertEqual(result.returncode,0,result.stdout+result.stderr)
        for name in ('.X11-unix','.ICE-unix'):
            path=control/'wt-1111111111111111'/name;self.assertTrue(path.is_dir());self.assertEqual(path.stat().st_mode&0o7777,0o1777)
        self.assertTrue((runtime/'drm/dri/renderD0').is_file())
        self.assertIn('wayland-socket-directories',result.stdout)
        self.assertEqual(subprocess.run(command,env=env,capture_output=True).returncode,0)

    def test_colliding_short_tmp_cannot_adopt_another_launch_directory(self):
        control,runtime,command,env=self.device_fixture()
        self.assertEqual(subprocess.run(command,env=env,capture_output=True).returncode,0)
        previous=control/'wt-1111111111111111'/'.wayland-owner';owner=previous.read_bytes()
        second=control/'wl-11111111-1111-1111-2222-222222222222';second.mkdir()
        answer=subprocess.run([*command[:-1],str(second)],env=env,capture_output=True)
        self.assertNotEqual(answer.returncode,0);self.assertEqual(previous.read_bytes(),owner)

    def test_production_tmp_paths_leave_room_for_x11_ice_and_dbus_sockets(self):
        root=Path('/data/data/com.termux/files/workspace-x11-archlinux')
        tmp=root/'wt-1111111111111111'
        for suffix in ('.X11-unix/X105','.ICE-unix/2147483647','dbus-1234567890'):
            self.assertLessEqual(len(os.fsencode(tmp/suffix)),107)
        self.assertGreater(len(os.fsencode(root/'wl-11111111-1111-1111-1111-111111111111'/'tmp/.X11-unix/X0')),107)

    def test_socket_directory_links_are_rejected_without_changing_target_permissions(self):
        control,runtime,command,env=self.device_fixture();outside=self.base/'outside';outside.mkdir(mode=0o700)
        tmp=control/'wt-1111111111111111';tmp.mkdir();(tmp/'.X11-unix').symlink_to(outside,target_is_directory=True)
        result=subprocess.run(command,env=env,capture_output=True,text=True)
        self.assertNotEqual(result.returncode,0);self.assertEqual(outside.stat().st_mode&0o7777,0o700)
        self.assertEqual(list(outside.iterdir()),[])

    def test_private_tmp_is_bound_only_for_run_and_qt_errors_use_the_existing_pipe(self):
        # Ordering guard complements the executable directory/symlink tests.
        launcher=(ASSETS/'wayland-desktop.sh').read_text();run=launcher.index('runtime=${JOBS_WAYLAND_RUNTIME:-}')
        self.assertNotIn('$guest_tmp:/tmp',launcher[:run]);self.assertIn('binds=(--bind "$guest_tmp:/tmp" --bind "$control:/tmp/termux-jobs-x11"',launcher[run:])
        self.assertIn('export TMPDIR=/tmp;',launcher)
        session=(ASSETS/'wayland-session.sh').read_text();self.assertIn('QT_FORCE_STDERR_LOGGING=1',session)
        self.assertLess(session.index('QT_FORCE_STDERR_LOGGING=1'),session.index('exec startplasma-wayland'))

    def test_native_keyboard_map_is_present_and_compiles(self):
        data=(ASSETS/'wayland-keymap.xkb').read_bytes();self.assertTrue(data.endswith(b'\0'));self.assertLess(len(data),512*1024)
        try:l=ctypes.CDLL('libxkbcommon.so.0')
        except OSError:self.skipTest('Host libxkbcommon unavailable')
        C=ctypes;l.xkb_context_new.argtypes=[C.c_int];l.xkb_context_new.restype=C.c_void_p;c=l.xkb_context_new(0)
        l.xkb_keymap_new_from_string.argtypes=[C.c_void_p,C.c_char_p,C.c_int,C.c_int];l.xkb_keymap_new_from_string.restype=C.c_void_p;k=l.xkb_keymap_new_from_string(c,data,1,0)
        self.assertTrue(k)
        l.xkb_keymap_key_get_syms_by_level.argtypes=[C.c_void_p,C.c_uint32,C.c_uint32,C.c_uint32,C.POINTER(C.POINTER(C.c_uint32))]
        for key,cp in [(608,0x410),(671,0x44f),(672,0x401),(673,0x451)]:
            values=C.POINTER(C.c_uint32)();self.assertEqual(l.xkb_keymap_key_get_syms_by_level(k,key,0,0,C.byref(values)),1);self.assertEqual(values[0],0x1000000|cp)
        l.xkb_keymap_unref.argtypes=[C.c_void_p];l.xkb_context_unref.argtypes=[C.c_void_p];l.xkb_keymap_unref(k);l.xkb_context_unref(c)

if __name__=='__main__':unittest.main()
