import contextlib
import fcntl
import importlib.util
import io
import json
import os
from pathlib import Path
import shutil
import struct
import subprocess
import tempfile
import unittest
import zipfile

ROOT=Path(__file__).resolve().parents[2]
ASSETS=ROOT/'x11/src/main/assets'

class BionicBridgeTest(unittest.TestCase):
    def setUp(self):
        parent=ROOT/'.verification/bionic-bridge/fixtures';parent.mkdir(parents=True,exist_ok=True)
        self.tmp=tempfile.TemporaryDirectory(dir=parent);self.root=Path(self.tmp.name);self.cache=self.root/'ubuntu-cache'
        for name in ['x11-bionic-glibc.py','x11-wrapper.py','x11-gpu.py']:
            shutil.copyfile(ASSETS/name,self.root/name)
        shutil.copyfile(ROOT/'third-party/bionic-glibc/ubuntu-arm64-runtime.zip',self.root/'x11-bionic-glibc.zip')
        spec=importlib.util.spec_from_file_location('bridge_fixture',self.root/'x11-bionic-glibc.py');self.b=importlib.util.module_from_spec(spec);spec.loader.exec_module(self.b)
        data=bytearray(64);data[:7]=b'\x7fELF\x02\x01\x01';struct.pack_into('<HH',data,16,3,183)
        self.zip=self.root/'driver.zip'
        with zipfile.ZipFile(self.zip,'w') as z:
            z.writestr('meta.json',json.dumps(dict(name='Test driver',libraryName='libvulkan_freedreno.so',minApi=28)))
            z.writestr('libvulkan_freedreno.so',data)
        self.b.w.import_driver(self.cache,self.zip,36)
        self.seed(self.cache)
    def seed(self, cache):
        shutil.copyfile(self.root/'x11-bionic-glibc.zip', cache/'runtime-bundle.zip')
    def tearDown(self):
        self.tmp.cleanup()
    def test_offline_install_check_and_no_layering(self):
        self.b.install(self.cache);first=self.b.valid_runtime(self.cache)
        self.assertEqual(self.cache/'runtime',first)
        self.b.install(self.cache)
        self.assertFalse(list(self.cache.glob('.runtime-*')))
        layer=json.loads((first/'share/vulkan/implicit_layer.d/jobs-bionic.json').read_text())['layer']
        self.assertEqual({'JOBS_BIONIC_ENABLED':'1'},layer['enable_environment'])
        self.assertTrue((first/'share/vulkan/implicit_layer.d'/layer['library_path']).resolve().is_file())
    def test_first_launch_downloads_once_then_reuses_verified_runtime_offline(self):
        (self.cache/'runtime-bundle.zip').unlink()
        selected=(self.cache/'current.json').read_bytes();calls=[]
        def fetch(url, pin, size, progress):
            calls.append((url,pin,size))
            data=(self.root/'x11-bionic-glibc.zip').read_bytes();progress(len(data),size)
            return data
        self.b.install(self.cache,fetch)
        self.b.install(self.cache,lambda *args:self.fail('ready runtime must work offline'))
        self.assertEqual([(self.b.URL,self.b.PIN,self.b.SIZE)],calls)
        self.assertEqual(selected,(self.cache/'current.json').read_bytes())
        self.assertEqual(0o600,(self.cache/'runtime-bundle.zip').stat().st_mode & 0o777)
        self.assertFalse(list(self.cache.glob('.bundle-*')))
    def test_failed_download_and_checksum_keep_previous_runtime_and_selection(self):
        self.b.install(self.cache);old=(self.cache/'runtime/libjobs-bionic.so').read_bytes()
        selected=(self.cache/'current.json').read_bytes()
        (self.cache/'runtime/ready.json').write_text('{}')
        (self.cache/'runtime-bundle.zip').unlink()
        def unavailable(*args):raise ValueError('GPU_DOWNLOAD_NETWORK')
        for fetch,error in [(unavailable,'NETWORK'),(lambda *args:b'corrupt','CHECKSUM')]:
            with self.assertRaisesRegex(ValueError,error):self.b.install(self.cache,fetch)
            self.assertEqual(old,(self.cache/'runtime/libjobs-bionic.so').read_bytes())
            self.assertEqual(selected,(self.cache/'current.json').read_bytes())
            self.assertFalse((self.cache/'runtime-bundle.zip').exists())
            self.assertFalse(list(self.cache.glob('.bundle-*')))
    def test_cancelled_download_does_not_activate_or_cache_partial_archive(self):
        (self.cache/'runtime-bundle.zip').unlink()
        marker=self.root/'cancel';self.b.w.CANCEL=marker
        def fetch(*args):
            marker.touch()
            return (self.root/'x11-bionic-glibc.zip').read_bytes()
        with self.assertRaisesRegex(ValueError,'CANCELLED'):self.b.install(self.cache,fetch)
        self.assertFalse((self.cache/'runtime').exists())
        self.assertFalse((self.cache/'runtime-bundle.zip').exists())
    def test_download_cache_symlink_is_rejected_without_touching_destination(self):
        archive=self.cache/'runtime-bundle.zip';archive.unlink()
        other=self.root/'keep';other.write_text('keep');archive.symlink_to(other)
        with self.assertRaisesRegex(ValueError,'BUNDLE_LINK'):self.b.install(self.cache)
        self.assertEqual('keep',other.read_text())
    def test_launcher_extracts_download_helper_without_bundled_zip(self):
        java=(ROOT/'app/src/main/java/com/termux/app/workspace/WorkspaceDesktopWrapper.java').read_text()
        self.assertIn('bridge()?new String[]{"x11-wrapper.py","x11-bionic-glibc.py","x11-gpu.py","x11-extras.py","x11-default-driver.py"}',java)
        self.assertIn("'x11-bionic-glibc.zip'",(ROOT/'x11/build.gradle').read_text())
        self.assertIn("(assets/'x11-bionic-glibc.zip').unlink(missing_ok=True)",(ROOT/'scripts/prepare-x11.py').read_text())
    def test_corrupt_runtime_is_repaired_but_driver_is_preserved(self):
        self.b.install(self.cache);selected=(self.cache/'current.json').read_bytes()
        (self.cache/'runtime/libjobs-bionic.so').write_bytes(b'broken')
        with self.assertRaisesRegex(ValueError,'CHANGED'):self.b.valid_runtime(self.cache)
        self.b.install(self.cache);self.b.valid_runtime(self.cache)
        self.assertEqual(selected,(self.cache/'current.json').read_bytes())
    def test_corrupt_bundle_is_refused_without_replacing_cache(self):
        self.b.install(self.cache);self.b.PIN='0'*64
        with self.assertRaisesRegex(ValueError,'CHECKSUM'):self.b.install(self.cache,lambda *args:(self.root/'x11-bionic-glibc.zip').read_bytes())
        self.assertTrue((self.cache/'runtime/libjobs-bionic.so').is_file())
        self.assertFalse(list(self.cache.glob('.runtime-*')))
    def test_invalid_versioned_mesa_elf_is_rejected_before_activation(self):
        archive=self.cache/'runtime-bundle.zip'
        with zipfile.ZipFile(archive) as z:files={n:z.read(n) for n in z.namelist()}
        files['mesa/libGLX_mesa.so.0']=b'not an ELF'*8
        with zipfile.ZipFile(archive,'w') as z:
            for name,data in files.items():z.writestr(name,data)
        self.b.PIN=self.b.w.digest(archive);self.b.SIZE=archive.stat().st_size
        with self.assertRaisesRegex(ValueError,'BUNDLE_ARM64'):self.b.install(self.cache)
        self.assertFalse((self.cache/'runtime').exists());self.assertFalse(list(self.cache.glob('.runtime-*')))
    def test_previous_runtime_upgrade_preserves_selected_driver(self):
        self.b.install(self.cache);selected=(self.cache/'current.json').read_bytes()
        marker=self.cache/'runtime/ready.json';value=json.loads(marker.read_text());value['version']='hybris-wsi-ubuntu-1';marker.write_text(json.dumps(value))
        self.b.install(self.cache);self.b.valid_runtime(self.cache)
        self.assertEqual(selected,(self.cache/'current.json').read_bytes());self.assertFalse((self.cache/'.runtime-old').exists())
    def test_debian_cache_can_prepare_while_ubuntu_runtime_is_locked(self):
        self.b.install(self.cache);before=(self.cache/'current.json').read_bytes()
        debian=self.root/'workspace-x11-bionic-glibc-debian';self.b.w.import_driver(debian,self.zip,36);self.seed(debian)
        with self.b.shared(self.cache):
            self.b.install(debian);self.b.valid_runtime(debian)
            with self.assertRaisesRegex(ValueError,'BUSY'):self.b.install(self.cache)
        self.assertEqual(before,(self.cache/'current.json').read_bytes())
        self.assertNotEqual((self.cache/'runtime').resolve(),(debian/'runtime').resolve())
    def test_fedora_is_independent_while_both_existing_targets_are_locked(self):
        self.b.install(self.cache)
        debian=self.root/'workspace-x11-bionic-glibc-debian'
        fedora=self.root/'workspace-x11-bionic-glibc-fedora'
        for dest in (debian,fedora):self.b.w.import_driver(dest,self.zip,36);self.seed(dest)
        self.b.install(debian)
        with self.b.shared(self.cache),self.b.shared(debian):
            run=subprocess.run(['python3','-B',str(self.root/'x11-bionic-glibc.py'),'install','fedora',str(fedora),'-','36','-'],capture_output=True,text=True)
            self.assertEqual(0,run.returncode,run.stdout+run.stderr)
            self.b.valid_runtime(fedora)
            with self.assertRaisesRegex(ValueError,'BUSY'):self.b.install(debian)
        self.assertNotEqual(fedora/'runtime',debian/'runtime')
    def test_fedora_bridge_dependency_check_rejects_missing_rpm_only_for_bridge(self):
        script=(ASSETS/'x11-native-desktop.sh').read_text()
        prefix=script.split('case $op in',1)[0]
        # Stub all other required binaries. A missing bridge-only package must
        # fail readiness instead of passing because XFCE itself is installed.
        body=prefix+"\nrpm(){ for p in \"$@\";do [ \"$p\" != libX11-xcb ] || return 1;done; };command(){ return 0; };fcitx5(){ return 0; };check\n"
        for mode in ('cpu','turnip','virgl','bionic-glibc'):
            run=subprocess.run(['sh','-c',body,'fixture','check','fedora'],env={**os.environ,'JOBS_X11_RENDERER':mode},capture_output=True,text=True)
            self.assertEqual(1 if mode=='bionic-glibc' else 0,run.returncode,run.stderr)
    def test_arch_cache_is_independent_while_other_runtime_is_locked(self):
        self.b.install(self.cache)
        arch=self.root/'workspace-x11-bionic-glibc-archlinux';self.b.w.import_driver(arch,self.zip,36);self.seed(arch)
        with self.b.shared(self.cache):
            run=subprocess.run(['python3','-B',str(self.root/'x11-bionic-glibc.py'),'install','archlinux',str(arch),'-','36','-'],capture_output=True,text=True)
            self.assertEqual(0,run.returncode,run.stdout+run.stderr);self.b.valid_runtime(arch)
    def test_arch_bridge_dependency_check_rejects_missing_package_only_for_bridge(self):
        prefix=(ASSETS/'x11-native-desktop.sh').read_text().split('case $op in',1)[0]
        body=prefix+"\npacman(){ for p in \"$@\";do [ \"$p\" != libxshmfence ] || return 1;done; };command(){ return 0; };fcitx5(){ return 0; };check\n"
        for mode in ('cpu','turnip','virgl','bionic-glibc'):
            run=subprocess.run(['sh','-c',body,'fixture','check','archlinux'],env={**os.environ,'JOBS_X11_RENDERER':mode},capture_output=True,text=True)
            self.assertEqual(1 if mode=='bionic-glibc' else 0,run.returncode,run.stderr)
    def test_new_targets_prepare_under_own_locks_without_touching_other_runtime(self):
        self.b.install(self.cache);before=(self.cache/'current.json').read_bytes()
        for target in ('opensuse','void'):
            cache=self.root/('workspace-x11-bionic-glibc-'+target);self.b.w.import_driver(cache,self.zip,36);self.seed(cache)
            with self.b.shared(self.cache):
                run=subprocess.run(['python3','-B',str(self.root/'x11-bionic-glibc.py'),'install',target,str(cache),'-','36','-'],capture_output=True,text=True)
                self.assertEqual(0,run.returncode,run.stdout+run.stderr);self.b.valid_runtime(cache)
            self.assertEqual(before,(self.cache/'current.json').read_bytes())
    def test_new_targets_require_own_bridge_packages_without_changing_other_renderers(self):
        prefix=(ASSETS/'x11-native-desktop.sh').read_text().split('case $op in',1)[0]
        stubs={
          'opensuse':'rpm(){ for p in "$@";do [ "$p" != libX11-xcb1 ] || return 1;done; };',
          'void':'xbps_command(){ for p in "$@";do [ "$p" != libX11 ] || return 1;done;echo installed; };'}
        for target,stub in stubs.items():
            body=prefix+'\n'+stub+'command(){ return 0; };fcitx5(){ return 0; };check\n'
            for mode in ('cpu','turnip','virgl','bionic-glibc'):
                run=subprocess.run(['sh','-c',body,'fixture','check',target],env={**os.environ,'JOBS_X11_RENDERER':mode},capture_output=True,text=True)
                self.assertEqual(1 if mode=='bionic-glibc' else 0,run.returncode,run.stderr)
    def test_all_target_cache_pairs_reject_cross_environment_mutations(self):
        targets=('ubuntu','debian','fedora','archlinux','opensuse','void')
        for target in targets:
            for other in targets:
                if target==other:continue
                cache=self.root/('workspace-x11-bionic-glibc'+('' if other=='ubuntu' else '-'+other))
                run=subprocess.run(['python3','-B',str(self.root/'x11-bionic-glibc.py'),'install',target,str(cache),'-','36','-'],capture_output=True,text=True)
                self.assertEqual(1,run.returncode);self.assertIn('BIONIC_CACHE_TARGET',run.stdout);self.assertFalse(cache.exists())
    def test_cli_cannot_use_other_environments_cache(self):
        for target,name in [('debian','workspace-x11-bionic-glibc'),('ubuntu','workspace-x11-bionic-glibc-debian'),('fedora','workspace-x11-bionic-glibc-debian'),('debian','workspace-x11-bionic-glibc-fedora'),('archlinux','workspace-x11-bionic-glibc-fedora')]:
            dest=self.root/name
            run=subprocess.run(['python3','-B',str(self.root/'x11-bionic-glibc.py'),'install',target,str(dest),'-','36','-'],capture_output=True,text=True)
            self.assertEqual(1,run.returncode);self.assertIn('BIONIC_CACHE_TARGET',run.stdout);self.assertFalse(dest.exists())
    def test_missing_driver_never_prepares_runtime(self):
        (self.cache/'current.json').unlink()
        with self.assertRaises(OSError):self.b.install(self.cache)
        self.assertFalse((self.cache/'runtime').exists())
    def test_running_desktop_lock_blocks_import_select_and_install(self):
        fd=os.open(self.cache/'operation.lock',os.O_RDONLY)
        try:
            fcntl.flock(fd,fcntl.LOCK_SH|fcntl.LOCK_NB)
            for call in [lambda:self.b.install(self.cache),lambda:self.b.w.import_driver(self.cache,self.zip,36)]:
                with self.assertRaisesRegex(ValueError,'BUSY'):call()
        finally:os.close(fd)
    def test_links_cannot_redirect_runtime_or_parent(self):
        self.b.install(self.cache)
        runtime=self.cache/'runtime';(runtime/'linker/q.so').unlink();(runtime/'linker').rmdir();(runtime/'linker').symlink_to(self.root,target_is_directory=True)
        with self.assertRaisesRegex(ValueError,'LINK'):self.b.valid_runtime(self.cache)
        # Repair removes the link itself; it must not traverse its destination.
        keep=self.root/'keep';keep.write_text('unchanged')
        self.b.install(self.cache);self.b.valid_runtime(self.cache);self.assertEqual('unchanged',keep.read_text())
    def test_cancelled_preparation_preserves_driver_and_runtime(self):
        self.b.install(self.cache);before=(self.cache/'runtime/libjobs-bionic.so').read_bytes()
        marker=self.root/'cancel';marker.touch();self.b.w.CANCEL=marker
        with self.assertRaisesRegex(ValueError,'CANCELLED'):self.b.install(self.cache)
        self.assertEqual(before,(self.cache/'runtime/libjobs-bionic.so').read_bytes())
    def test_driver_tampering_prevents_environment_export(self):
        self.b.install(self.cache);value,path=self.b.w.driver(self.cache)
        (path/value['library']).write_bytes(b'changed')
        with contextlib.redirect_stdout(io.StringIO()) as output:
            with self.assertRaisesRegex(ValueError,'CHANGED'):self.b.environment(self.cache)
        self.assertEqual('',output.getvalue())
    def test_export_uses_fixed_guest_bind_and_not_metadata_shell_text(self):
        self.b.install(self.cache)
        with contextlib.redirect_stdout(io.StringIO()) as output:self.b.environment(self.cache)
        value=self.b.w.driver(self.cache)[0]
        self.assertEqual('export JOBS_BIONIC_DRIVER=/tmp/termux-jobs-bionic-glibc/drivers/'+value['id']+'/libvulkan_freedreno.so\n',output.getvalue())
    def test_other_targets_rejected_before_any_write(self):
        for target in ['termux','ubuntu-ai','alpine']:
            dest=self.root/'must-not-exist'
            run=subprocess.run(['python3','-B',str(self.root/'x11-bionic-glibc.py'),'install',target,str(dest),'-','36','-'],capture_output=True,text=True)
            self.assertEqual(1,run.returncode);self.assertIn('BIONIC_TARGET_UNSUPPORTED',run.stdout);self.assertFalse(dest.exists())
            run=subprocess.run(['bash',str(ASSETS/'x11-desktop.sh'),'install',target,str(dest),'xfce'],env={**os.environ,'PREFIX':str(self.root/'usr'),'JOBS_X11_RENDERER':'bionic-glibc'},capture_output=True,text=True)
            self.assertEqual(2,run.returncode);self.assertTrue('BIONIC_TARGET_UNSUPPORTED' in run.stdout or 'UNSUPPORTED_DESKTOP' in run.stdout)
    def test_missing_bridge_package_fails_selected_ubuntu_readiness_only(self):
        script=(ASSETS/'x11-desktop.sh').read_text()
        block=script.split('if [[ ( $target == ubuntu || $target == debian ) && ${JOBS_X11_RENDERER:-cpu} == bionic-glibc ]];then',1)[1].split('\nfi',1)[0]
        body='set -eu;target=$1;JOBS_X11_RENDERER=$2;components=true;packages=base; if [[ ( $target == ubuntu || $target == debian ) && $JOBS_X11_RENDERER == bionic-glibc ]];then '+block+'\nfi; configured(){ [[ $1 != libxcb-dri3-0 ]]; };eval "$components"'
        for target in ['ubuntu','termux','debian','alpine','fedora','opensuse','void','archlinux']:
            for mode in ['cpu','turnip','virgl','bionic-glibc']:
                result=subprocess.run(['bash','-c',body,'fixture',target,mode],capture_output=True,text=True)
                self.assertEqual(1 if target in ('ubuntu','debian') and mode=='bionic-glibc' else 0,result.returncode,result.stderr)
    def test_guest_packages_required_only_by_selected_ubuntu_bridge(self):
        script=(ASSETS/'x11-desktop.sh').read_text()
        block=script.split("jobs_bionic_packages='",1)[1].split("'",1)[0]
        for name in ['libgl1-mesa-dri','libglx-mesa0','libvulkan1','libxcb-present0','libxcb-dri3-0','libx11-xcb1']:
            self.assertIn(name,block)
        self.assertIn('components+=" && configured $jobs_bionic_package"',script)
        env=(ASSETS/'x11-bionic-glibc-env.sh').read_text()
        self.assertNotIn('export LD_PRELOAD=',env)
        self.assertIn('ubuntu|debian|fedora|archlinux|opensuse|void)',env)

if __name__=='__main__':unittest.main()
