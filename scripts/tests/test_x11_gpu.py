"""Pinned optional GPU packages; fixtures never install packages or access a real GPU."""
import hashlib,importlib.util,io,json,os
from pathlib import Path
import subprocess,tarfile,tempfile,unittest
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('gpu',ROOT/'x11/src/main/assets/x11-gpu.py');gpu=importlib.util.module_from_spec(spec);spec.loader.exec_module(gpu)
class GpuTests(unittest.TestCase):
 def setUp(self):
  parent=ROOT/'.verification/gpu-fixtures';parent.mkdir(parents=True,exist_ok=True);self.tmp=tempfile.TemporaryDirectory(dir=parent);self.base=Path(self.tmp.name);self.original=gpu.SPECS.copy();gpu.CANCEL_FILE=None
 def tearDown(self):gpu.SPECS=self.original;gpu.CANCEL_FILE=None;self.tmp.cleanup()
 def archive(self,entries):
  b=io.BytesIO()
  with tarfile.open(fileobj=b,mode='w:gz') as t:
   for name,data,kind in entries:
    x=tarfile.TarInfo(name)
    if kind=='link':x.type=tarfile.SYMTYPE;x.linkname=data;t.addfile(x)
    elif kind=='fifo':x.type=tarfile.FIFOTYPE;t.addfile(x)
    else:x.size=len(data);t.addfile(x,io.BytesIO(data))
  return b.getvalue()
 def configure(self,data):gpu.SPECS['ubuntu']={'directory':'ubuntu-fixture','version':'test','packages':[('https://example.invalid/archive',hashlib.sha256(data).hexdigest(),len(data),'tar')]}
 def test_atomic_install_verified_reuse_and_corruption_detection(self):
  data=self.archive([('usr/lib/driver.so',b'fake-driver','file'),('usr/lib/driver.so.0','driver.so','link')]);self.configure(data)
  gpu.install(self.base,'ubuntu',lambda *args:data);self.assertTrue(gpu.ready(self.base,'ubuntu'));self.assertFalse(list(self.base.glob('.stage-*')))
  gpu.install(self.base,'ubuntu',lambda *args:self.fail('cached package must not download'))
  (self.base/'ubuntu-fixture/usr/lib/driver.so').write_bytes(b'corrupt');self.assertFalse(gpu.ready(self.base,'ubuntu'))
  with self.assertRaisesRegex(ValueError,'CACHE_INCOMPLETE'):gpu.install(self.base,'ubuntu',lambda *args:data)
 def test_consented_repair_replaces_corrupt_cache_and_keeps_neighbour(self):
  data=self.archive([('usr/lib/driver.so',b'fake-driver','file')]);self.configure(data)
  gpu.install(self.base,'ubuntu',lambda *args:data);(self.base/'ubuntu-fixture/usr/lib/driver.so').write_bytes(b'corrupt')
  other=self.base/'other-target';other.mkdir();(other/'keep').write_bytes(b'keep')
  gpu.install(self.base,'ubuntu',lambda *args:data,repair=True)
  self.assertTrue(gpu.ready(self.base,'ubuntu'));self.assertEqual(b'keep',(other/'keep').read_bytes());self.assertFalse(list(self.base.glob('.retired-*')))
 def test_failed_repair_preserves_previous_cache(self):
  data=self.archive([('driver',b'old','file')]);self.configure(data);gpu.install(self.base,'ubuntu',lambda *args:data)
  marker=self.base/'ubuntu-fixture/ready.json';marker.write_text('{}');before=(self.base/'ubuntu-fixture/driver').read_bytes()
  with self.assertRaisesRegex(ValueError,'CHECKSUM'):gpu.install(self.base,'ubuntu',lambda *args:b'bad',repair=True)
  self.assertEqual(before,(self.base/'ubuntu-fixture/driver').read_bytes());self.assertEqual('{}',marker.read_text());self.assertFalse(list(self.base.glob('.stage-*')))
 def test_repair_never_follows_a_cache_link(self):
  data=self.archive([('driver',b'old','file')]);self.configure(data);outside=self.base/'outside';outside.mkdir();(outside/'keep').write_bytes(b'keep');(self.base/'ubuntu-fixture').symlink_to(outside,target_is_directory=True)
  with self.assertRaisesRegex(ValueError,'CACHE_IS_A_LINK'):gpu.install(self.base,'ubuntu',lambda *args:data,repair=True)
  self.assertEqual(b'keep',(outside/'keep').read_bytes())
 def test_bad_checksum_never_publishes(self):
  data=self.archive([('driver',b'x','file')]);self.configure(data)
  with self.assertRaisesRegex(ValueError,'CHECKSUM'):gpu.install(self.base,'ubuntu',lambda *args:b'wrong')
  self.assertFalse((self.base/'ubuntu-fixture').exists());self.assertFalse(list(self.base.glob('.stage-*')))
 def test_archive_escape_and_special_files_refused(self):
  for name,data,kind in [('../outside',b'x','file'),('/absolute-outside',b'x','file'),('outside','../../outside','link'),('fifo',b'','fifo')]:
   archive=self.archive([(name,data,kind)]);self.configure(archive)
   # data_filter strips a leading slash; the resulting file must stay in stage.
   if name.startswith('/'):
    gpu.install(self.base,'ubuntu',lambda *args:archive);self.assertTrue((self.base/'ubuntu-fixture/absolute-outside').is_file());import shutil;shutil.rmtree(self.base/'ubuntu-fixture');continue
   with self.assertRaises((ValueError,tarfile.FilterError)):gpu.install(self.base,'ubuntu',lambda *args:archive)
   self.assertFalse((self.base/'ubuntu-fixture').exists());self.assertFalse(list(self.base.glob('.stage-*')))
 def test_symlink_cache_and_lock_rejected(self):
  data=self.archive([('driver',b'x','file')]);self.configure(data);outside=self.base/'outside';outside.mkdir();cache=self.base/'cache';cache.symlink_to(outside,target_is_directory=True)
  with self.assertRaisesRegex(ValueError,'CACHE_IS_A_LINK'):gpu.install(cache,'ubuntu',lambda *args:data)
  (self.base/'install.lock').symlink_to(outside/'victim')
  with self.assertRaises(OSError):gpu.install(self.base,'ubuntu',lambda *args:data)
 def test_cancellation_does_not_publish_or_remove_other_files(self):
  data=self.archive([('driver',b'x','file')]);self.configure(data);marker=self.base/'cancel';keep=self.base/'keep';keep.write_text('keep');gpu.CANCEL_FILE=marker
  def fetch(*args):marker.touch();return data
  with self.assertRaisesRegex(ValueError,'GPU_CANCELLED'):gpu.install(self.base,'ubuntu',fetch)
  self.assertEqual('keep',keep.read_text());self.assertFalse((self.base/'ubuntu-fixture').exists());self.assertFalse(list(self.base.glob('.stage-*')))
 def test_ready_rejects_external_directory_link(self):
  data=self.archive([('driver',b'x','file')]);self.configure(data);gpu.install(self.base,'ubuntu',lambda *args:data);(self.base/'ubuntu-fixture/escape').symlink_to(self.base.parent,target_is_directory=True);self.assertFalse(gpu.ready(self.base,'ubuntu'))
 def test_deb_parser_extracts_only_data_archive(self):
  archive=self.archive([('driver',b'x','file')]);header=b'data.tar.gz/    '.ljust(16)+b'0'.ljust(12)+b'0'.ljust(6)+b'0'.ljust(6)+b'100644'.ljust(8)+str(len(archive)).encode().ljust(10)+b'`\n'
  deb=b'!<arch>\n'+header+archive
  self.assertEqual(archive,gpu.data_archive(deb,'deb').read())
  with self.assertRaises(ValueError):gpu.data_archive(b'bad','deb')
 def test_renderer_shell_changes_only_its_own_environment(self):
  base=self.base/'gpu';root=base/'ubuntu-26.04-mesa26.2.5';lib=root/'usr/lib/aarch64-linux-gnu';lib.mkdir(parents=True);(root/'ready.json').write_text('{}');(lib/'libvulkan_freedreno.so').touch()
  command='source "$1"; (jobs_gpu_environment ubuntu "$2"; [[ $GALLIUM_DRIVER == zink && ${LIBGL_ALWAYS_SOFTWARE:-} == "" && $LD_LIBRARY_PATH == "$2/ubuntu-26.04-mesa26.2.5/usr/lib/aarch64-linux-gnu" ]]); [[ $LIBGL_ALWAYS_SOFTWARE == 1 ]]'
  r=subprocess.run(['/bin/bash','-c',command,'fixture',str(ROOT/'x11/src/main/assets/x11-gpu-env.sh'),str(base)],env=dict(os.environ,LIBGL_ALWAYS_SOFTWARE='1',LD_LIBRARY_PATH=''),capture_output=True,text=True);self.assertEqual(0,r.returncode,r.stderr)

 def test_new_targets_pin_distinct_libcs_and_matching_runtime_paths(self):
  script=ROOT/'x11/src/main/assets/x11-gpu-env.sh'
  for target,libdir in [('debian','usr/lib/aarch64-linux-gnu'),('alpine','usr/lib'),('fedora','usr/lib64'),('opensuse','usr/lib/aarch64-linux-gnu'),('void','usr/lib')]:
   spec=gpu.SPECS[target];component=self.base/spec['directory'];lib=component/libdir;lib.mkdir(parents=True)
   (component/'ready.json').write_text('{}');(lib/'libvulkan_freedreno.so').touch()
   command='source "$1"; jobs_gpu_environment "$2" "$3"; test "$LD_LIBRARY_PATH" = "$4" && test "$LIBGL_DRIVERS_PATH" = "$4/dri" && test "$VK_DRIVER_FILES" = "$5/turnip-icd.json"; result=$?; if [ "$2" = alpine ];then test -z "${__EGL_VENDOR_LIBRARY_FILENAMES:-}" || exit 1;fi; exit "$result"'
   r=subprocess.run(['/bin/bash','-c',command,'fixture',str(script),target,str(self.base),str(lib),str(component)],env=dict(os.environ,LD_LIBRARY_PATH='',__EGL_VENDOR_LIBRARY_FILENAMES=''),capture_output=True,text=True)
   self.assertEqual(0,r.returncode,r.stderr)
   self.assertIn('musl-alpine3.24' if target=='alpine' else 'glibc-',spec['packages'][0][0]);self.assertEqual(64,len(spec['packages'][0][1]))
 def test_missing_new_target_components_fail_without_changing_renderer(self):
  for target in ['debian','alpine','fedora','opensuse','void']:
   r=subprocess.run(['/bin/bash','-c','source "$1"; jobs_gpu_environment "$2" "$3"', 'fixture',str(ROOT/'x11/src/main/assets/x11-gpu-env.sh'),target,str(self.base)],capture_output=True,text=True)
   self.assertNotEqual(0,r.returncode);self.assertIn('GPU_COMPONENTS_MISSING',r.stdout)
 def test_guest_requirements_are_scoped_and_reject_missing_libraries(self):
  from unittest.mock import patch,Mock
  for target in ['debian','alpine','fedora','opensuse','void']:
   with patch.object(gpu.platform,'machine',return_value='aarch64'),patch.object(gpu.Path,'read_text',return_value='Adreno830v2'),patch('builtins.open',unittest.mock.mock_open()),patch.object(gpu.subprocess,'run',return_value=Mock(returncode=0)) as run,patch.dict(os.environ,{'PREFIX':'/fixture/usr'}):
    gpu.requirements(target)
    argv=run.call_args.args[0];self.assertEqual(['/fixture/usr/bin/proot-distro',*(['run','--work-dir','/root'] if target=='void' else ['login']),target,'--'],argv[:-3]);self.assertIn('libvulkan.so.1',argv[-1]);self.assertNotIn('apt-get',argv[-1]);self.assertNotIn('apk add',argv[-1])
    run.return_value.returncode=72
    with self.assertRaisesRegex(ValueError,'REQUIRED'):gpu.requirements(target)
 def test_guest_gpu_routing_binds_cache_and_keeps_selected_target(self):
  prefix=self.base/'files/usr';bin=prefix/'bin';bin.mkdir(parents=True)
  control=self.base/'files/workspace-x11';control.mkdir()
  log=self.base/'calls'
  for name,body in {'python3':'printf "gpu-check:%s\\n" "$*" >> "$CALLS"', 'proot-distro':'printf "%s\\n" "$@" >> "$CALLS"'}.items():
   p=bin/name;p.write_text('#!/bin/sh\n'+body+'\n');p.chmod(0o700)
  for target,display in [('debian',100),('alpine',101),('fedora',102),('opensuse',103),('void',104)]:
   log.write_text('')
   env={**os.environ,'PREFIX':str(prefix),'PATH':str(bin)+':/usr/bin:/bin','CALLS':str(log),'JOBS_X11_RENDERER':'turnip','JOBS_X11_FOCUS_TOKEN':'a11a11a1-1111-4111-8111-111111111111'}
   r=subprocess.run(['/bin/bash',str(ROOT/'x11/src/main/assets/x11-desktop.sh'),'run',target,str(control)],env=env,capture_output=True,text=True,timeout=5)
   self.assertEqual(0,r.returncode,r.stderr)
   calls=log.read_text();self.assertIn('check '+target+' ',calls);self.assertIn(str(prefix.parent/'workspace-x11-gpu')+':/tmp/termux-jobs-x11-gpu',calls)
   self.assertIn('JOBS_X11_TARGET='+target,calls);self.assertIn('JOBS_X11_DISPLAY='+str(display),calls)
   self.assertIn('jobs_gpu_environment "$JOBS_X11_TARGET"',calls);self.assertNotIn('jobs_gpu_environment ubuntu',calls)

class DownloadFailures(unittest.TestCase):
 def test_http_and_network_errors_are_bounded_codes(self):
  from unittest.mock import patch,Mock
  import urllib.error
  opener=Mock()
  for error,code in [(urllib.error.HTTPError('https://example.invalid',404,'missing',{},None),'GPU_DOWNLOAD_HTTP_404'),(urllib.error.URLError('private connection detail'),'GPU_DOWNLOAD_NETWORK')]:
   opener.open.side_effect=error
   with patch.object(gpu.urllib.request,'build_opener',return_value=opener):
    with self.assertRaisesRegex(ValueError,'^'+code+'$'):gpu.download('https://example.invalid','0'*64,1,lambda *_:None)
 def test_termux_packages_use_immutable_release_assets_and_wrapper_shares_loader(self):
  spec=importlib.util.spec_from_file_location('wrapper_pins',ROOT/'x11/src/main/assets/x11-wrapper.py');wrapper=importlib.util.module_from_spec(spec);spec.loader.exec_module(wrapper)
  for target, spec in gpu.SPECS.items():
   for index, package in enumerate(spec['packages']):
    tag='graphics-20261025' if target=='termux' or index>0 else 'mesa-26.2.5'
    self.assertTrue(package[0].startswith('https://github.com/HezeAI/termux-jobs-components/releases/download/'+tag+'/'))
  self.assertEqual(tuple(gpu.SPECS['termux']['packages'][1][:3]),wrapper.LOADER)

if __name__=='__main__':unittest.main()
