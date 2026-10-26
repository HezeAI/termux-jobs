from pathlib import Path
import hashlib,importlib.util,io,tempfile,unittest,zipfile
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('virgl_client',ROOT/'x11/src/main/assets/x11-virgl-client.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
class ClientTests(unittest.TestCase):
 def setUp(self):
  self.tmp=tempfile.TemporaryDirectory(dir=ROOT/'.verification');self.base=Path(self.tmp.name);self.root=self.base/'private';self.archive=self.base/'client.zip'
 def tearDown(self):self.tmp.cleanup()
 def pack(self,extra=None):
  elf=bytearray(40);elf[:4]=b'\x7fELF';elf[16:18]=(3).to_bytes(2,'little');elf[18:20]=(183).to_bytes(2,'little')
  entries={name:bytes(elf)+b'virpipe\0' for name in ['lib/libGL.so.1','lib/libEGL.so.1','lib/libgallium-26.1.6.so','lib/dri/swrast_dri.so']};entries.update(extra or {})
  with zipfile.ZipFile(self.archive,'w') as z:
   for n,d in entries.items():z.writestr(n,d)
  pin=hashlib.sha256(self.archive.read_bytes()).hexdigest();return patch.multiple(m,PIN=pin,NAME='client-'+pin[:16])
 def test_private_install_ready_and_repeat_reuse_without_rootfs_changes(self):
  with self.pack():
   with self.assertRaisesRegex(ValueError,'REQUIRED'):m.prepare(self.root,self.archive,False)
   dest=m.prepare(self.root,self.archive,True);file=dest/'lib/libGL.so.1';inode=file.stat().st_ino
   self.assertEqual(dest,m.prepare(self.root,self.archive,False));self.assertEqual(dest,m.prepare(self.root,self.archive,True));self.assertEqual(inode,file.stat().st_ino);self.assertEqual(0o600,file.stat().st_mode&0o777)
 def test_modified_file_is_rejected_without_overwriting_user_content(self):
  with self.pack():
   dest=m.prepare(self.root,self.archive,True);file=dest/'lib/libGL.so.1';file.write_bytes(b'edited')
   with self.assertRaisesRegex(ValueError,'CACHE_INVALID'):m.prepare(self.root,self.archive,True)
   self.assertEqual(b'edited',file.read_bytes())
 def test_links_are_not_followed_for_cache_archive_or_libraries(self):
  with self.pack():
   other=self.base/'keep';other.mkdir();self.root.symlink_to(other)
   with self.assertRaisesRegex(ValueError,'LINK'):m.prepare(self.root,self.archive,True)
   self.assertEqual([],list(other.iterdir()));self.root.unlink()
   dest=m.prepare(self.root,self.archive,True);file=dest/'lib/libGL.so.1';file.unlink();victim=other/'file';victim.write_bytes(b'keep');file.symlink_to(victim)
   with self.assertRaisesRegex(ValueError,'CACHE_INVALID'):m.prepare(self.root,self.archive,True)
   self.assertEqual(b'keep',victim.read_bytes())
 def test_traversal_non_library_wrong_abi_and_no_driver_are_rejected(self):
  for name,data in [('../escape',b'bad'),('lib/../../escape',b'bad'),('bin/run',b'bad'),('lib/extra.so',b'bad'),('lib/libgallium-26.1.6.so',bytes(40))]:
   with self.subTest(name=name),self.pack({name:data}):
    with self.assertRaises(ValueError):m.prepare(self.root,self.archive,True)
    self.assertFalse(self.root.exists())
 def test_checksum_failure_and_interrupted_publication_leave_no_partial_client(self):
  with self.pack():
   raw=self.archive.read_bytes();self.archive.write_bytes(b'corrupt')
   with self.assertRaisesRegex(ValueError,'CHECKSUM'):m.prepare(self.root,self.archive,True)
   self.archive.write_bytes(raw)
   with patch.object(m.os,'rename',side_effect=OSError('interrupted')),self.assertRaises(OSError):m.prepare(self.root,self.archive,True)
   self.assertEqual([],list(self.root.iterdir()));self.assertTrue(m.prepare(self.root,self.archive,True).is_dir())
 def test_real_pack_is_pinned_musl_arm64_and_contains_virpipe(self):
  archive=ROOT/'.tools/x11-components/x11-virgl-alpine.zip';files=m.payload(archive)
  self.assertIn(b'virpipe\0',files['lib/libgallium-26.1.6.so']);self.assertNotIn(b'libLLVM',files['lib/libgallium-26.1.6.so'])
