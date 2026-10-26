"""Wrapper archives/transactions and Termux-only routing, with no installed changes."""
import hashlib
import fcntl
import importlib.util
import io
import json
import os
from pathlib import Path
import stat
import struct
import subprocess
import tempfile
import tarfile
import unittest
import zipfile
from unittest.mock import patch

ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('wrapper',ROOT/'x11/src/main/assets/x11-wrapper.py')
w=importlib.util.module_from_spec(spec);spec.loader.exec_module(w)

class WrapperTests(unittest.TestCase):
 def setUp(self):
  parent=ROOT/'.verification/termux-wrapper/fixtures';parent.mkdir(parents=True,exist_ok=True)
  self.tmp=tempfile.TemporaryDirectory(dir=parent);self.root=Path(self.tmp.name);self.cache=self.root/'cache';w.CANCEL=None
 def tearDown(self):w.CANCEL=None;self.tmp.cleanup()
 def elf(self,extra=b''):
  data=bytearray(64);data[:7]=b'\x7fELF\x02\x01\x01';struct.pack_into('<HH',data,16,3,183);return bytes(data)+extra
 def zip(self,extra=(),metadata=None,payload=None):
  output=io.BytesIO()
  with zipfile.ZipFile(output,'w') as z:
   z.writestr('meta.json',json.dumps(metadata or dict(name='Turnip fixture',libraryName='libvulkan_freedreno.so',minApi=28)))
   z.writestr('libvulkan_freedreno.so',payload or self.elf())
   for name,data in extra:z.writestr(name,data)
  return output.getvalue()
 def source(self,data):
  path=self.root/'driver.zip';path.write_bytes(data);return path
 def imported(self):return w.import_driver(self.cache,self.source(self.zip()),35)
 def test_remove_selected_and_unselected_driver_preserves_other_targets_and_source_zip(self):
  first=self.imported();original=self.root/'driver.zip';raw=original.read_bytes()
  second=w.import_driver(self.cache,self.source(self.zip(payload=self.elf(b'second'))),35)
  other=self.root/'another-target';w.import_driver(other,original,35)
  self.assertFalse(w.remove_driver(self.cache,first['id']))
  self.assertEqual(second,w.driver(self.cache)[0]);self.assertEqual(second,w.driver(other)[0])
  self.assertEqual(1,len(w.catalog(self.cache)))
  self.assertTrue(w.remove_driver(self.cache,second['id']))
  self.assertFalse((self.cache/'current.json').exists());self.assertEqual([],w.catalog(self.cache))
  self.assertEqual(second,w.driver(other)[0]);self.assertTrue(original.is_file())
  self.assertFalse(list(self.cache.glob('.delete-driver-*')))
 def test_remove_protects_builtin_and_rejects_busy_cancelled_paths_and_main_library_links(self):
  value=self.imported();root=self.cache/'drivers'/value['id']
  with self.assertRaisesRegex(ValueError,'PROTECTED'):w.remove_driver(self.cache,w.DEFAULT_DRIVER_ID)
  for identity in ('../escape','../'+'a'*64,'x'*64):
   with self.assertRaisesRegex(ValueError,'METADATA'):w.remove_driver(self.cache,identity)
  with (self.cache/'operation.lock').open('r') as fd:
   fcntl.flock(fd,fcntl.LOCK_SH)
   with self.assertRaisesRegex(ValueError,'BUSY'):w.remove_driver(self.cache,value['id'])
  w.CANCEL=self.root/'cancel';w.CANCEL.touch()
  with self.assertRaisesRegex(ValueError,'CANCELLED'):w.remove_driver(self.cache,value['id'])
  w.CANCEL=None
  victim=self.root/'keep';victim.write_text('keep');library=root/value['library'];data=library.read_bytes();library.unlink();library.symlink_to(victim)
  with self.assertRaisesRegex(ValueError,'FILE'):w.remove_driver(self.cache,value['id'])
  library.unlink();library.write_bytes(data)
  self.assertEqual('keep',victim.read_text());self.assertEqual(value,w.driver(self.cache)[0])
 def test_remove_used_driver_with_auxiliary_files_caches_and_links_never_follows_external_targets(self):
  first=self.imported();root=self.cache/'drivers'/first['id']
  (root/'meta.json').write_text('{"runtime":"generated"}')
  (root/'cache'/'nested').mkdir(parents=True);(root/'cache'/'nested'/'shader.bin').write_bytes(b'cache')
  external=self.root/'external';external.mkdir();victim=external/'keep';victim.write_text('keep')
  (root/'cache'/'outside-directory').symlink_to(external,target_is_directory=True)
  (root/'outside-file').symlink_to(victim)
  os.link(victim,root/'hardlink')
  archive=self.source(self.zip(payload=self.elf(b'second')));second=w.import_driver(self.cache,archive,35)
  original=archive.read_bytes();other=self.root/'another-target';w.import_driver(other,archive,35)
  self.assertFalse(w.remove_driver(self.cache,first['id']))
  self.assertFalse(root.exists());self.assertEqual(second,w.driver(self.cache)[0]);self.assertEqual(second,w.driver(other)[0])
  self.assertEqual('keep',victim.read_text());self.assertEqual(original,archive.read_bytes())
  self.assertEqual([victim],list(external.iterdir()));self.assertFalse(list(self.cache.glob('.delete-driver-*')))
 def test_remove_requires_no_follow_recursive_removal_support_before_mutating_selection(self):
  value=self.imported()
  with patch.object(w.shutil.rmtree,'avoids_symlink_attacks',False),self.assertRaisesRegex(ValueError,'REMOVE_UNSUPPORTED'):
   w.remove_driver(self.cache,value['id'])
  self.assertEqual(value,w.driver(self.cache)[0]);self.assertFalse(list(self.cache.glob('.delete-driver-*')))
 def test_remove_rolls_back_directory_if_selected_metadata_cannot_be_cleared(self):
  value=self.imported();original=Path.unlink
  def unlink(path,*args,**kw):
   if path==self.cache/'current.json':raise OSError('fixture unlink failure')
   return original(path,*args,**kw)
  with patch.object(Path,'unlink',unlink),self.assertRaises(OSError):w.remove_driver(self.cache,value['id'])
  self.assertEqual(value,w.driver(self.cache)[0]);self.assertFalse(list(self.cache.glob('.delete-driver-*')))
 def test_successful_import_is_private_verified_and_retains_previous_driver(self):
  first=self.imported();keep=self.root/'ubuntu';keep.mkdir();(keep/'keep').write_text('keep')
  value,path=w.driver(self.cache);self.assertEqual(first,value);self.assertEqual(0o600,(path/'libvulkan_freedreno.so').stat().st_mode&0o777)
  second=w.import_driver(self.cache,self.source(self.zip(payload=self.elf(b'second'))),35)
  self.assertNotEqual(first['id'],second['id']);self.assertEqual({first['id'],second['id']},{p.name for p in (self.cache/'drivers').iterdir()});self.assertEqual('keep',(keep/'keep').read_text())
  self.assertFalse(list(self.cache.glob('.import-*')))
 def test_invalid_archives_and_minimum_api_preserve_previous_selection(self):
  first=self.imported()
  cases=[b'not zip',self.zip(metadata=dict(libraryName='../escape.so')),self.zip(metadata=dict(libraryName='libvulkan_freedreno.so',minApi=99)),self.zip(payload=self.elf(b'libc.so.6\0')),self.zip(payload=b'not ARM64')]
  for data in cases:
   with self.assertRaises((ValueError,zipfile.BadZipFile)):w.import_driver(self.cache,self.source(data),35)
   self.assertEqual(first,w.driver(self.cache)[0])
 def test_adrenotools_qualcomm_library_names_are_imported_retained_and_selected(self):
  first=self.imported()
  names=['vulkan.adr891.so','vkbgsl.so','vkbCB.so','vkb891.qti.hardware.hexlp-V2-ndk.so']
  raw=io.BytesIO()
  with zipfile.ZipFile(raw,'w') as z:
   z.writestr('meta.json',json.dumps(dict(name='891.11 fixture',libraryName=names[0],minApi=36)))
   for n in names:z.writestr(n,self.elf(n.encode()))
  value=w.import_driver(self.cache,self.source(raw.getvalue()),36)
  self.assertEqual(names[0],value['library']);self.assertEqual(set(names),set(value['files']))
  self.assertEqual(value,w.driver(self.cache)[0]);self.assertEqual(2,len(w.catalog(self.cache)))
  w.select_driver(self.cache,first['id'],36);w.select_driver(self.cache,value['id'],36)
  self.assertEqual(value,w.driver(self.cache)[0])
  (self.cache/'drivers'/value['id']/names[1]).write_bytes(b'tampered')
  with self.assertRaisesRegex(ValueError,'CHANGED'):w.driver(self.cache)
 def test_non_lib_main_and_dependencies_keep_path_elf_and_api_checks(self):
  first=self.imported()
  for main,extra,api in [('vulkan.adr891.so',[('vulkan.adr891.so',self.elf())],35),('../vulkan.adr891.so',[],36),('vulkan.adr891.so',[('vulkan.adr891.so',self.elf()),('vkbgsl.so',b'not ELF')],36),('vulkan.adr891.so',[('vulkan.adr891.so',self.elf()),('../vkbgsl.so',self.elf())],36)]:
   raw=io.BytesIO()
   with zipfile.ZipFile(raw,'w') as z:
    z.writestr('meta.json',json.dumps(dict(libraryName=main,minApi=36)))
    for n,data in extra:z.writestr(n,data)
   with self.assertRaises(ValueError):w.import_driver(self.cache,self.source(raw.getvalue()),api)
   self.assertEqual(first,w.driver(self.cache)[0]);self.assertFalse((self.root/'vkbgsl.so').exists())
 def test_no_archive_traversal_links_duplicate_or_scripts_are_executed(self):
  first=self.imported()
  link=zipfile.ZipInfo('link');link.create_system=3;link.external_attr=(stat.S_IFLNK|0o777)<<16
  for extra in [[('../escaped',b'x')],[(link,b'../../escaped')],[('meta.json',b'{}')]]:
   with self.assertRaises(ValueError):w.import_driver(self.cache,self.source(self.zip(extra)),35)
   self.assertEqual(first,w.driver(self.cache)[0]);self.assertFalse((self.root/'escaped').exists())
  value=w.import_driver(self.cache,self.source(self.zip([('setup.sh',b'never executed')])),35)
  self.assertFalse((self.cache/'drivers'/value['id']/'setup.sh').exists())
 def test_cancelled_import_and_symlinked_root_are_refused(self):
  first=self.imported();marker=self.root/'cancel';marker.touch();w.CANCEL=marker
  with self.assertRaisesRegex(ValueError,'CANCELLED'):w.import_driver(self.cache,self.source(self.zip(payload=self.elf(b'second'))),35)
  w.CANCEL=None;self.assertEqual(first,w.driver(self.cache)[0])
  link=self.root/'link';link.symlink_to(self.cache,target_is_directory=True)
  with self.assertRaisesRegex(ValueError,'DIRECTORY'):w.import_driver(link,self.source(self.zip()),35)
 def test_driver_file_and_metadata_symlinks_and_tampering_are_rejected(self):
  self.imported();value,root=w.driver(self.cache);lib=root/value['library'];lib.write_bytes(self.elf(b'changed'))
  with self.assertRaisesRegex(ValueError,'CHANGED'):w.driver(self.cache)
  lib.unlink();lib.symlink_to(self.source(self.elf()))
  with self.assertRaisesRegex(ValueError,'FILE'):w.driver(self.cache)
  pointer=self.cache/'current.json';pointer.rename(self.root/'metadata');pointer.symlink_to(self.root/'metadata')
  with self.assertRaisesRegex(ValueError,'FILE'):w.driver(self.cache)
 def test_runtime_requires_driver_and_never_downloads_when_missing(self):
  with self.assertRaises(FileNotFoundError):w.install_runtime(self.cache,lambda *args:self.fail('unexpected network'))
  self.assertFalse((self.cache/'runtime').exists())
 def test_runtime_rejects_hash_mismatch_and_removes_stage(self):
  self.imported()
  with self.assertRaisesRegex(ValueError,'CHECKSUM'):w.install_runtime(self.cache,lambda *args:b'bad')
  self.assertFalse((self.cache/'runtime').exists());self.assertFalse(list(self.cache.glob('.runtime-*')))
 def test_cli_rejects_all_guests_before_any_write(self):
  for target in ['ubuntu','debian','alpine','fedora','opensuse','void','archlinux','ubuntu-ai']:
   result=subprocess.run(['python3','-B',str(ROOT/'x11/src/main/assets/x11-wrapper.py'),'import',target,str(self.cache),'-','35','-'],capture_output=True,text=True)
   self.assertEqual(1,result.returncode);self.assertIn('ERROR:WRAPPER_TERMUX_ONLY',result.stdout);self.assertFalse(self.cache.exists())
 def test_desktop_script_refuses_wrapper_guest_before_proot_or_package_work(self):
  for target in ['ubuntu','debian','alpine','fedora','opensuse','void','archlinux']:
   r=subprocess.run(['/bin/bash',str(ROOT/'x11/src/main/assets/x11-desktop.sh'),'install',target,'/not-used','xfce'],env={**os.environ,'PREFIX':str(self.root/'usr'),'JOBS_X11_RENDERER':'wrapper'},capture_output=True,text=True)
   self.assertEqual(2,r.returncode);self.assertEqual('ERROR:WRAPPER_TERMUX_ONLY\n',r.stdout)
 def test_termux_wrapper_dependencies_are_requested_only_for_that_renderer(self):
  shell=(ROOT/'x11/src/main/assets/x11-native-desktop.sh').read_text().split('# Request-local workaround',1)[0]
  for target in ['termux','alpine','fedora','opensuse','void','archlinux']:
   for mode in ['cpu','turnip','virgl','wrapper']:
    result=subprocess.run(['/bin/sh','-c',shell+'\nprintf "PACKAGES:%s\\n" "$pkgs"','fixture','check',target,'/unused'],env={**os.environ,'PREFIX':'/fixture/usr','JOBS_X11_RENDERER':mode},capture_output=True,text=True)
    self.assertEqual(0,result.returncode,result.stderr)
    packages=result.stdout.split('PACKAGES:',1)[1].split()
    if target=='termux' and mode=='wrapper':
     for package in ['mesa','libandroid-shmem','libdrm','libx11','libxcb','libxshmfence','libwayland','zlib','zstd']:self.assertIn(package,packages)
    if target=='termux' and mode!='wrapper':self.assertNotIn('libwayland',packages)
 def runtime_archives(self):
  output=io.BytesIO()
  with zipfile.ZipFile(output,'w') as z:
   for name in w.RUNTIME:z.writestr('lib/'+name,self.elf(name.encode()))
  wrapper=output.getvalue();output=io.BytesIO()
  with tarfile.open(fileobj=output,mode='w:gz') as archive:
   member=tarfile.TarInfo('./data/data/com.termux/files/usr/lib/libvulkan.so.1.4.365');data=self.elf(b'loader');member.size=len(data);archive.addfile(member,io.BytesIO(data))
  data=output.getvalue();header=b'data.tar.gz/'.ljust(16)+b'0'.ljust(12)+b'0'.ljust(6)+b'0'.ljust(6)+b'100644'.ljust(8)+str(len(data)).encode().ljust(10)+b'`\n'
  loader=b'!<arch>\n'+header+data
  return wrapper,loader
 def test_runtime_atomic_install_reuse_corruption_and_cancel(self):
  self.imported();archive,loader=self.runtime_archives()
  with patch.object(w,'SHA',hashlib.sha256(archive).hexdigest()),patch.object(w,'SIZE',len(archive)),patch.object(w,'LOADER',('https://example.invalid/loader',hashlib.sha256(loader).hexdigest(),len(loader))):
   w.install_runtime(self.cache,lambda url,*args:archive if url==w.URL else loader)
   self.assertTrue(w.runtime_ready(self.cache));w.install_runtime(self.cache,lambda *args:self.fail('cache must be reused'))
   (self.cache/'runtime/wrapper-icd.json').write_text('changed');self.assertFalse(w.runtime_ready(self.cache))
   with self.assertRaisesRegex(ValueError,'CHANGED'):w.install_runtime(self.cache,lambda *args:self.fail('corrupt cache must not be overwritten'))
   import shutil;shutil.rmtree(self.cache/'runtime');marker=self.root/'cancel';w.CANCEL=marker
   def fetch(url,*args):marker.touch();return archive
   with self.assertRaisesRegex(ValueError,'CANCELLED'):w.install_runtime(self.cache,fetch)
   w.CANCEL=None;self.assertFalse((self.cache/'runtime').exists());self.assertFalse(list(self.cache.glob('.runtime-*')))
 def test_termux_launch_passes_custom_driver_only_to_owned_desktop_and_errors_do_not_fallback(self):
  prefix=self.root/'files/usr';binary=prefix/'bin';binary.mkdir(parents=True);control=prefix.parent/'workspace-x11';control.mkdir()
  cache=prefix.parent/'workspace-x11-wrapper';cache.mkdir();(cache/'operation.lock').touch()
  script=control/'x11-desktop.sh';script.write_bytes((ROOT/'x11/src/main/assets/x11-desktop.sh').read_bytes())
  (control/'x11-audio.sh').write_text('jobs_audio_start() { :; }\n')
  stub=binary/'python3';stub.write_text('#!/bin/sh\nif [ "${FAIL_WRAPPER:-0}" = 1 ];then echo ERROR:WRAPPER_DRIVER_CHANGED;exit 1;fi\nprintf "export ADRENOTOOLS_DRIVER_PATH=/private/selected/\\nexport ADRENOTOOLS_DRIVER_NAME=libvulkan_freedreno.so\\nexport VK_DRIVER_FILES=/private/wrapper.json\\n"\n');stub.chmod(0o700)
  dbus=binary/'dbus-run-session';dbus.write_text('#!/bin/sh\nprintf "OWNED:%s:%s:%s:%s\\n" "$JOBS_X11_TARGET" "$GALLIUM_DRIVER" "$ADRENOTOOLS_DRIVER_NAME" "$VK_DRIVER_FILES"\n');dbus.chmod(0o700)
  env={**os.environ,'PREFIX':str(prefix),'PATH':str(binary)+':/usr/bin:/bin','JOBS_X11_RENDERER':'wrapper'}
  result=subprocess.run(['/bin/bash',str(script),'run','termux',str(control),'xfce'],env=env,capture_output=True,text=True)
  self.assertEqual(0,result.returncode,result.stderr);self.assertIn('JOBS_GPU_RENDERER=wrapper-zink',result.stdout);self.assertIn('OWNED:termux:zink:libvulkan_freedreno.so:/private/wrapper.json',result.stdout)
  result=subprocess.run(['/bin/bash',str(script),'run','termux',str(control),'xfce'],env={**env,'FAIL_WRAPPER':'1'},capture_output=True,text=True)
  self.assertEqual(1,result.returncode);self.assertIn('ERROR:WRAPPER_DRIVER_CHANGED',result.stdout);self.assertNotIn('OWNED:',result.stdout)
 def test_active_desktop_lock_prevents_import_and_preserves_selected_driver(self):
  first=self.imported()
  with (self.cache/'operation.lock').open('rb') as active:
   fcntl.flock(active,fcntl.LOCK_SH)
   with self.assertRaisesRegex(ValueError,'WRAPPER_BUSY'):w.import_driver(self.cache,self.source(self.zip(payload=self.elf(b'new'))),35)
   self.assertEqual(first,w.driver(self.cache)[0])
 def test_saved_drivers_can_be_selected_without_original_zip_or_downloads(self):
  first=self.imported();second=w.import_driver(self.cache,self.source(self.zip(payload=self.elf(b'other'))),35)
  (self.root/'driver.zip').unlink()
  with patch.object(w,'gpu_module',side_effect=AssertionError('no downloads')):
   self.assertEqual({first['id'],second['id']},{row['id'] for row in w.catalog(self.cache)})
   self.assertEqual(first,w.select_driver(self.cache,first['id'],35));self.assertEqual(first,w.driver(self.cache)[0])
   self.assertEqual(second,w.select_driver(self.cache,second['id'],35));self.assertEqual(second,w.driver(self.cache)[0])
  self.assertEqual(2,len(list((self.cache/'drivers').iterdir())))
 def test_v87_current_driver_is_adopted_before_importing_another_driver(self):
  first=self.imported();descriptor=self.cache/'drivers'/first['id']/'library.json';descriptor.unlink()
  before=(self.cache/'current.json').read_bytes()
  self.assertEqual([first['id']],[row['id'] for row in w.catalog(self.cache)]);self.assertTrue(descriptor.is_file());self.assertEqual(before,(self.cache/'current.json').read_bytes())
  descriptor.unlink();w.import_driver(self.cache,self.source(self.zip(payload=self.elf(b'next'))),35)
  self.assertEqual(first,w.select_driver(self.cache,first['id'],35))
 def test_repeat_import_reuses_driver_and_invalid_selection_keeps_current(self):
  archive=self.source(self.zip());first=w.import_driver(self.cache,archive,35);w.import_driver(self.cache,archive,35);self.assertEqual(1,len(list((self.cache/'drivers').iterdir())))
  for identity in ['../escape','f'*64]:
   with self.assertRaises((ValueError,OSError)):w.select_driver(self.cache,identity,35)
   self.assertEqual(first,w.driver(self.cache)[0])
  second=w.import_driver(self.cache,self.source(self.zip(payload=self.elf(b'next'))),35)
  (self.cache/'drivers'/first['id']/first['library']).write_bytes(self.elf(b'corrupt'))
  with self.assertRaisesRegex(ValueError,'CHANGED'):w.select_driver(self.cache,first['id'],35)
  self.assertEqual(second,w.driver(self.cache)[0])
 def test_selection_cannot_run_while_desktop_is_active_or_cancelled(self):
  first=self.imported();second=w.import_driver(self.cache,self.source(self.zip(payload=self.elf(b'next'))),35)
  with (self.cache/'operation.lock').open('rb') as active:
   fcntl.flock(active,fcntl.LOCK_SH)
   with self.assertRaisesRegex(ValueError,'BUSY'):w.select_driver(self.cache,first['id'],35)
  marker=self.root/'cancel';marker.touch();w.CANCEL=marker
  with self.assertRaisesRegex(ValueError,'CANCELLED'):w.select_driver(self.cache,first['id'],35)
  w.CANCEL=None;self.assertEqual(second,w.driver(self.cache)[0])
 def test_catalog_skips_links_and_selection_rechecks_api_and_metadata_identity(self):
  first=self.imported();second=w.import_driver(self.cache,self.source(self.zip(payload=self.elf(b'next'))),35)
  descriptor=self.cache/'drivers'/first['id']/'library.json';value=json.loads(descriptor.read_text());value['minApi']=99;descriptor.write_text(json.dumps(value))
  with self.assertRaisesRegex(ValueError,'ANDROID_VERSION'):w.select_driver(self.cache,first['id'],35)
  self.assertEqual(second,w.driver(self.cache)[0])
  descriptor.unlink();descriptor.symlink_to(self.root/'external');(self.root/'external').write_text(json.dumps(value))
  self.assertEqual([second['id']],[row['id'] for row in w.catalog(self.cache)])
  with self.assertRaisesRegex(ValueError,'FILE'):w.select_driver(self.cache,first['id'],100)
  descriptor.unlink();value['id']=second['id'];descriptor.write_text(json.dumps(value))
  with self.assertRaisesRegex(ValueError,'METADATA'):w.select_driver(self.cache,first['id'],100)

if __name__=='__main__':unittest.main()
