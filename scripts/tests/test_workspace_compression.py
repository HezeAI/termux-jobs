"""Compression/progress fixtures only; never run APT or proot on the real environment."""
import gzip,importlib.util,io,itertools,json,os,subprocess,sys,tarfile,tempfile,unittest
from pathlib import Path
from unittest.mock import patch
sys.dont_write_bytecode=True
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('backup',ROOT/'app/src/main/assets/workspace-backup.py')
b=importlib.util.module_from_spec(spec);spec.loader.exec_module(b)
class CompressionTests(unittest.TestCase):
 def setUp(self):
  base=ROOT/'.verification/backup-progress-v1.1.3/fixtures';base.mkdir(parents=True,exist_ok=True)
  self.tmp=tempfile.TemporaryDirectory(dir=base);self.root=Path(self.tmp.name);self.prefix=self.root/'usr';(self.prefix/'bin').mkdir(parents=True)
 def tearDown(self):self.tmp.cleanup()
 def executable(self,name,code):
  p=self.prefix/'bin'/name;p.write_text('#!'+sys.executable+'\n'+code);p.chmod(0o700);return p
 def fake_pigz(self,fail=False):
  return self.executable('pigz',"import sys,gzip\n"+("sys.exit(7)\n" if fail else "sys.stdout.buffer.write(gzip.compress(sys.stdin.buffer.read(),compresslevel=int(sys.argv[1][1:])))\n"))
 def options(self,engine='gzip',level=3):return b.compression_options(dict(prefix=str(self.prefix),compressionEngine=engine,compressionLevel=level))
 def test_levels_roundtrip_and_smaller_compact_archive(self):
  src=self.root/'home';src.mkdir();payload=b'abcdefghijk'*200000;(src/'file').write_bytes(payload);sizes={}
  for level in (1,3,9):
   p=self.root/('level'+str(level)+'.tar.gz');b.create_tar(p,src,'home',compression=self.options(level=level))
   with tarfile.open(p) as t:self.assertEqual(t.extractfile('home/file').read(),payload)
   sizes[level]=p.stat().st_size
  self.assertLess(sizes[9],sizes[1])
 def test_invalid_level_or_engine_rejected_before_destination_exists(self):
  for key,value in [('compressionLevel',True),('compressionLevel',2),('compressionLevel','3'),('compressionEngine','shell')]:
   req=dict(prefix=str(self.prefix),destination=str(self.root/'nonexistent'));req[key]=value
   with self.assertRaisesRegex(b.BackupError,'INVALID_COMPRESSION'):b.create(req)
 def test_missing_pigz_and_default_gzip(self):
  self.assertEqual(self.options()['level'],3)
  self.assertFalse(b.compression_status(dict(prefix=str(self.prefix)))['pigz'])
  with self.assertRaisesRegex(b.BackupError,'PIGZ_REQUIRED'):self.options('pigz')
 def test_pigz_roundtrip_and_failure_not_success(self):
  self.fake_pigz();p=self.root/'pigz.gz'
  with b.compressed_output(p,self.options('pigz')) as f:f.write(b'fixture-data'*1000)
  self.assertEqual(gzip.decompress(p.read_bytes()),b'fixture-data'*1000)
  self.fake_pigz(True)
  with self.assertRaises((b.BackupError,BrokenPipeError)):
   with b.compressed_output(self.root/'bad.gz',self.options('pigz')) as f:f.write(b'test')
 def test_explicit_two_and_four_threads_are_passed_and_roundtrip(self):
  self.fake_pigz();real=b.subprocess.Popen;calls=[]
  def start(args,**kw):calls.append(args);return real(args,**kw)
  for threads in (2,4):
   options=b.compression_options(dict(prefix=str(self.prefix),compressionEngine='pigz',compressionThreads=threads))
   path=self.root/('threads'+str(threads)+'.gz')
   with patch.object(b.subprocess,'Popen',side_effect=start):
    with b.compressed_output(path,options) as stream:stream.write(b'fixture roundtrip')
   self.assertEqual(calls[-1][2:4],['-p',str(threads)])
   self.assertEqual(gzip.decompress(path.read_bytes()),b'fixture roundtrip')
 def test_invalid_thread_modes_fail_before_creating_output(self):
  self.fake_pigz()
  for engine,threads in [('gzip',2),('pigz',1),('pigz',3),('pigz',8),('pigz',True),('pigz','4')]:
   with self.assertRaisesRegex(b.BackupError,'INVALID_COMPRESSION'):
    b.compression_options(dict(prefix=str(self.prefix),compressionEngine=engine,compressionThreads=threads))
  self.assertEqual(self.options()['threads'],1)
  self.assertEqual(self.options('pigz')['threads'],4)
 def test_pigz_failure_cleanup_only_its_child(self):
  self.executable('pigz','import time\ntime.sleep(30)\n');children=[];real=b.subprocess.Popen
  def start(*a,**kw):p=real(*a,**kw);children.append(p);return p
  with patch.object(b.subprocess,'Popen',side_effect=start):
   with self.assertRaisesRegex(b.BackupError,'CANCELLED'):
    with b.compressed_output(self.root/'cancel.gz',self.options('pigz')) as f:raise b.BackupError('CANCELLED')
  self.assertEqual(len(children),1);self.assertIsNotNone(children[0].poll())
 def test_native_uncompressed_stdout_is_compressed_with_selected_engine(self):
  raw=io.BytesIO()
  with tarfile.open(fileobj=raw,mode='w') as t:
   member=tarfile.TarInfo('ubuntu/rootfs/etc/test');member.size=7;t.addfile(member,io.BytesIO(b'fixture'))
  data=raw.getvalue();self.executable('proot-distro',"# proot_distro\nimport sys\nassert sys.argv[1:]==['backup','--compress','none','ubuntu']\nsys.stdout.buffer.write("+repr(data)+")\n")
  for engine in ('gzip','pigz'):
   self.fake_pigz();p=self.root/(engine+'.tar.gz');self.assertEqual(b.backup_ubuntu(self.prefix,'ubuntu',p,self.options(engine)),engine)
   self.assertEqual(gzip.decompress(p.read_bytes()),data)
 def test_native_failure_not_reported_as_complete(self):
  self.executable('proot-distro',"# proot_distro\nimport sys\nsys.stdout.buffer.write(b'bad')\nsys.exit(4)\n")
  with self.assertRaisesRegex(b.BackupError,'PROOT_DISTRO_FAILED'):b.backup_ubuntu(self.prefix,'ubuntu',self.root/'bad.gz',self.options())
 def test_legacy_keeps_supported_backup_command(self):
  with patch.object(b,'run_tool') as run:
   p=self.root/'legacy.gz';self.assertEqual(b.backup_ubuntu(self.prefix,'ubuntu',p,self.options()),'native')
   self.assertEqual(run.call_args.args[0],[str(self.prefix/'bin/proot-distro'),'backup','--output',str(p),'ubuntu'])
 def test_explicit_install_uses_only_fixed_package_and_checks_result(self):
  self.executable('apt-get',"import sys,pathlib\nassert sys.argv[1:]==['install','-y','pigz']\np=pathlib.Path(__file__).parent/'pigz'\np.write_text('#!"+sys.executable+"\\n')\np.chmod(0o700)\n")
  self.assertTrue(b.install_pigz(dict(prefix=str(self.prefix)))['pigz'])
  self.assertTrue(b.install_pigz(dict(prefix=str(self.prefix)))['pigz'])
 def test_install_failure_leaves_gzip_usable(self):
  self.executable('apt-get','import sys\nsys.exit(2)\n')
  with self.assertRaisesRegex(b.BackupError,'PIGZ_INSTALL_FAILED'):b.install_pigz(dict(prefix=str(self.prefix)))
  self.assertEqual(self.options()['engine'],'gzip')
 def test_cancelled_install_waits_for_its_apt_child_without_killing_it(self):
  self.executable('apt-get',"import time,pathlib\ntime.sleep(.1)\np=pathlib.Path(__file__).parent/'pigz'\np.write_text('installed')\np.chmod(0o700)\n")
  with patch.object(b.time,'sleep',side_effect=b.BackupError('CANCELLED')):
   with self.assertRaisesRegex(b.BackupError,'CANCELLED'):b.install_pigz(dict(prefix=str(self.prefix)))
  self.assertTrue(b.pigz_available(self.prefix))
 def test_native_cancel_cleans_up_only_owned_proot_child(self):
  self.executable('proot-distro',"# proot_distro\nimport time\ntime.sleep(30)\n");children=[];real=b.subprocess.Popen
  def start(*a,**kw):p=real(*a,**kw);children.append(p);return p
  with patch.object(b.subprocess,'Popen',side_effect=start),patch.object(b,'compressed_output',side_effect=b.BackupError('CANCELLED')):
   with self.assertRaisesRegex(b.BackupError,'CANCELLED'):b.backup_ubuntu(self.prefix,'ubuntu',self.root/'cancel.gz',self.options())
  self.assertEqual(len(children),1);self.assertIsNotNone(children[0].poll())
 def test_stream_progress_names_totals_and_no_contents(self):
  source=self.root/'home';source.mkdir();payload=b'SECRET_CONTENT_SENTINEL'*100000;(source/'large').write_bytes(payload);events=[]
  with patch.object(b,'event',side_effect=lambda stage,**kw:events.append(dict(stage=stage,**kw))),patch.object(b.time,'monotonic',side_effect=itertools.count()):
   b.create_tar(self.root/'out.gz',source,'home',total=len(payload))
  events=[e for e in events if e['stage']=='ARCHIVE'];self.assertTrue(any(e['processed']==len(payload) for e in events))
  self.assertTrue(any(e['current']=='home/large' and 0<e['processed']<len(payload) for e in events))
  self.assertTrue(all(e['total']==len(payload) for e in events));self.assertNotIn('SECRET_CONTENT_SENTINEL',json.dumps(events))
 def test_scan_all_supported_compressions_after_progress_changes(self):
  source=self.root/'home';source.mkdir();(source/'a').write_text('hello')
  for mode in ('gz','bz2','xz'):
   p=self.root/('out.tar.'+mode)
   with tarfile.open(p,'w:'+mode) as t:t.add(source,arcname='home')
   self.assertEqual(b.scan_tar(p)['unpacked'],5)
if __name__=='__main__':unittest.main()
