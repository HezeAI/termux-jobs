"""Pinned package archives and native offline plans; no live network or package installs."""
import contextlib,hashlib,importlib.util,io,json,os,subprocess,tarfile,tempfile,unittest
from pathlib import Path
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[2];ASSETS=ROOT/'x11/src/main/assets'
s=importlib.util.spec_from_file_location('base_packages',ASSETS/'x11-base.py');m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
class Archives(unittest.TestCase):
 def setUp(self):
  self.temp=tempfile.TemporaryDirectory(dir=ROOT/'.verification');self.root=Path(self.temp.name);self.dest=self.root/'base';self.data=b'original package fixture'
  self.record={'version':m.VERSION,'target':'debian','files':{'fixture_1_arm64.deb':{'size':len(self.data),'sha256':hashlib.sha256(self.data).hexdigest(),'name':'fixture','version':'1','arch':'arm64'}}}
 def tearDown(self):self.temp.cleanup()
 def archive(self,extra=None,payload=None,omit=False):
  raw=(json.dumps(self.record)+'\n').encode();self.pin=hashlib.sha256(raw).hexdigest();p=self.root/'archive.tar'
  with tarfile.open(p,'w') as t:
   def add(n,data,kind=tarfile.REGTYPE,link=''):
    e=tarfile.TarInfo(n);e.size=len(data);e.type=kind;e.linkname=link;t.addfile(e,io.BytesIO(data))
   add('base.json',raw)
   if not omit:add('packages/fixture_1_arm64.deb',self.data if payload is None else payload)
   if extra:add(*extra)
  return p
 def test_verified_bundle_can_be_reused_and_detects_modified_content(self):
  p=self.archive();m.unpack(p,self.dest,'debian',self.pin);self.assertTrue(m.ready(self.dest,'debian',self.pin));self.assertFalse(m.ready(self.dest,'kali',self.pin))
  (self.dest/'packages/fixture_1_arm64.deb').write_bytes(b'changed');self.assertFalse(m.ready(self.dest,'debian',self.pin))
 def test_missing_duplicate_traversal_links_and_checksum_preserve_previous_cache(self):
  m.unpack(self.archive(),self.dest,'debian',self.pin);before=(self.dest/'base.json').read_bytes()
  cases=[{'omit':True},{'payload':b'x'*len(self.data)},{'extra':('packages/fixture_1_arm64.deb',self.data)}, {'extra':('../outside',b'bad')},{'extra':('packages/link',b'',tarfile.SYMTYPE,'/outside')},{'extra':('base.json',b'{}')}]
  for kw in cases:
   with self.subTest(case=kw):
    p=self.archive(**kw)
    with self.assertRaises(ValueError):m.unpack(p,self.dest,'debian',self.pin)
    self.assertEqual(before,(self.dest/'base.json').read_bytes());self.assertTrue(m.ready(self.dest,'debian',self.pin));self.assertFalse((self.root/'outside').exists())
 def test_retry_reuses_fixed_stage_and_preserves_external_link_target(self):
  p=self.archive();stage=self.dest.with_name('.'+self.dest.name+'.stage');stage.mkdir();(stage/'partial').write_text('incomplete');keep=self.root/'keep';keep.write_text('keep');(stage/'external').symlink_to(keep)
  m.unpack(p,self.dest,'debian',self.pin);self.assertTrue(m.ready(self.dest,'debian',self.pin));self.assertFalse(stage.exists());self.assertEqual('keep',keep.read_text())
 def test_fedora_caret_versions_and_original_rpm_filenames_are_accepted(self):
  self.record['files']={'gdk-pixbuf2-2.44.6^really2.44.4-3.fc44.aarch64.rpm':{'size':len(self.data),'sha256':hashlib.sha256(self.data).hexdigest(),'name':'gdk-pixbuf2','version':'2.44.6^really2.44.4-3.fc44','arch':'aarch64'}}
  raw=json.dumps(self.record).encode();self.pin=hashlib.sha256(raw).hexdigest();p=self.root/'rpm.tar'
  with tarfile.open(p,'w') as t:
   for name,data in [('base.json',raw),('packages/'+next(iter(self.record['files'])),self.data)]:
    e=tarfile.TarInfo(name);e.size=len(data);t.addfile(e,io.BytesIO(data))
  m.unpack(p,self.dest,'debian',self.pin);self.assertTrue(m.ready(self.dest,'debian',self.pin))
 def test_manifest_pin_and_destination_symlink_are_rejected(self):
  p=self.archive()
  with self.assertRaises(ValueError):m.unpack(p,self.dest,'debian','0'*64)
  outside=self.root/'keep';outside.mkdir();(outside/'keep').write_text('keep');self.dest.symlink_to(outside)
  with self.assertRaises(ValueError):m.unpack(p,self.dest,'debian',self.pin)
  self.assertEqual('keep',(outside/'keep').read_text())
 def test_cached_file_link_hardlink_extra_and_manifest_mutation_are_not_ready(self):
  p=self.archive();m.unpack(p,self.dest,'debian',self.pin);f=self.dest/'packages/fixture_1_arm64.deb';outside=self.root/'keep';outside.write_bytes(self.data)
  f.unlink();f.symlink_to(outside);self.assertFalse(m.ready(self.dest,'debian',self.pin));f.unlink();os.link(outside,f);self.assertFalse(m.ready(self.dest,'debian',self.pin));f.unlink();f.write_bytes(self.data)
  extra=self.dest/'packages/extra';extra.write_text('extra');self.assertFalse(m.ready(self.dest,'debian',self.pin));extra.unlink();(self.dest/'versions.tsv').write_text('changed');self.assertFalse(m.ready(self.dest,'debian',self.pin))
 def test_actual_entry_accepts_android_files_alias_reuses_cache_and_rejects_impostor(self):
  archive=self.archive();files=self.root/'files';files.mkdir();prefix=files/'usr';prefix.mkdir();control=files/'workspace-x11-debian';control.mkdir();alias=self.root/'android-alias';alias.symlink_to(files)
  runtime=self.root/'runtime';runtime.mkdir();(runtime/'environment-image.py').write_text('def download(*args):\n raise RuntimeError("network must not run for verified cache")\n')
  catalog={'version':m.VERSION,'targets':{'debian':{'filename':'fixture.tar','sha256':hashlib.sha256(archive.read_bytes()).hexdigest(),'size':archive.stat().st_size,'manifestSha256':self.pin}}};(runtime/'x11-base-packages.json').write_text(json.dumps(catalog))
  m.unpack(archive,control/'base-packages-20261028-debian','debian',self.pin)
  with patch.object(m,'__file__',str(runtime/'x11-base.py')),patch.dict(os.environ,{'PREFIX':str(alias/'usr')}),patch.object(m.sys,'argv',['helper','debian',str(control)]),contextlib.redirect_stdout(io.StringIO()) as out:m.main()
  self.assertIn('READY:base-packages',out.getvalue())
  impostor=self.root/'other/workspace-x11-debian';impostor.mkdir(parents=True)
  with patch.dict(os.environ,{'PREFIX':str(prefix)}),patch.object(m.sys,'argv',['helper','debian',str(impostor)]),self.assertRaises(ValueError):m.main()
class NativePlans(unittest.TestCase):
 def fixture(self,root,kind):
  packages=root/'cache/packages';packages.mkdir(parents=True);(packages.parent/'empty-repositories').mkdir();(packages/('fixture.'+kind)).write_text('fixture');(packages.parent/'versions.tsv').write_text('fixture\t1\tfixture.'+kind+'\n');bin=root/'bin';bin.mkdir();capture=root/'calls'
  scripts={'pacman-key':'exit 0','apt-get':'exit 0','dpkg-query':'printf "fixture\\tarm64\\t${CURRENT:-0}\\n"','dpkg':'if [ "$1" = --print-architecture ];then echo arm64;else [ "${CURRENT:-0}" = 2 ];fi','pacman':'case "$1" in -Qp)echo "fixture 1";;-Q)echo "fixture ${CURRENT:-0}";;esac','vercmp':'if [ "${CURRENT:-0}" = 2 ];then echo 1;else echo 0;fi','rpm':'case "$1" in -qa)printf "fixture\\t0:${CURRENT:-0}\\n";;-qp)printf "fixture 0:1";;-q)printf "0:${CURRENT:-0}";;--eval)if [ "${CURRENT:-0}" = 2 ];then echo 1;else echo 0;fi;;esac','dnf':'exit 0','zypper':'exit 0','apk':'case "$1" in list)echo "fixture-${CURRENT:-0} aarch64 [installed]";;version)if [ "${CURRENT:-0}" = 2 ];then echo ">";else echo "=";fi;;esac','xbps-install':'exit 0','xbps-query':'echo fixture-${CURRENT:-0}','xbps-uhelper':'if [ \"${CURRENT:-0}\" = 2 ];then exit 1;else exit 255;fi','head':'/usr/bin/head "$@"'}
  for name,body in scripts.items():
   p=bin/name;p.write_text('#!/bin/sh\nprintf "%s\\n" "'+name+':$*" >> "$CAPTURE"\n'+body+'\n');p.chmod(0o700)
  return packages,dict(os.environ,PATH=str(bin),CAPTURE=str(capture),DPKG_ADMINDIR=str(root/'dpkg')),capture
 def test_all_managers_receive_only_local_package_plan(self):
  mapping={'termux':('deb','apt-get:','Dir::Etc::sourcelist=/dev/null'),'ubuntu':('deb','apt-get:','Dir::Etc::sourcelist=/dev/null'),'debian':('deb','apt-get:','Dir::Etc::sourcelist=/dev/null'),'kali':('deb','apt-get:','Dir::Etc::sourcelist=/dev/null'),'alpine':('apk','apk:add','--no-network'),'archlinux':('pkg.tar.zst','pacman:-U','--needed'),'manjaro':('pkg.tar.zst','pacman:-U','--needed'),'fedora':('rpm','dnf:','--disablerepo=*'),'opensuse':('rpm','zypper:','--reposd-dir'),'void':('xbps','xbps-install:','--ignore-conf-repos')}
  for target,(kind,manager,option) in mapping.items():
   with self.subTest(target=target),tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
    packages,env,capture=self.fixture(Path(d),kind)
    if target=='void':(packages/'aarch64-repodata').write_text('original signed metadata')
    r=subprocess.run(['/bin/sh',str(ASSETS/'x11-base-install.sh'),str(packages),target],env=env,capture_output=True,text=True,timeout=5);self.assertEqual(0,r.returncode,r.stderr)
    text=capture.read_text();self.assertIn(manager,text);self.assertIn(option,text);
    if kind=='deb':
     self.assertIn('Dir::Etc::sourceparts=/dev/null',text);self.assertIn('Dir::Cache::pkgcache=',text);self.assertNotIn('--no-download',text)
    self.assertNotIn('--allow-downgrades',text);self.assertNotIn('--ignore-verification',text);self.assertNotIn(' update',text);self.assertNotIn(' -Syu',text)
 def test_newer_packages_are_retained_without_rejecting_the_install(self):
  for target,kind in [('debian','deb'),('manjaro','pkg.tar.zst'),('fedora','rpm'),('opensuse','rpm'),('alpine','apk')]:
   with self.subTest(target=target),tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
    packages,env,capture=self.fixture(Path(d),kind);env['CURRENT']='2';r=subprocess.run(['/bin/sh',str(ASSETS/'x11-base-install.sh'),str(packages),target],env=env,text=True,capture_output=True,timeout=5);self.assertEqual(0,r.returncode,r.stderr);self.assertIn('BASE_PACKAGE_RETAINED:fixture',r.stdout)
    text=capture.read_text()
    if target=='debian':self.assertIn('fixture=2',text);self.assertNotIn(str(packages/'fixture.deb'),text)
    else:self.assertNotIn(str(packages/('fixture.'+kind)),text)
 def test_newer_installed_package_does_not_block_a_missing_package(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   packages,env,capture=self.fixture(Path(d),'deb');(packages/'missing.deb').write_text('missing');(packages.parent/'versions.tsv').write_text('fixture\t1\tfixture.deb\nmissing\t1\tmissing.deb\n')
   query=Path(d)/'bin/dpkg-query';query.write_text('#!/bin/sh\nprintf "fixture\\tarm64\\t2\\n"\n');query.chmod(0o700);env['CURRENT']='2'
   r=subprocess.run(['/bin/sh',str(ASSETS/'x11-base-install.sh'),str(packages),'debian'],env=env,text=True,capture_output=True,timeout=5);self.assertEqual(0,r.returncode,r.stderr);text=capture.read_text();self.assertIn('fixture=2',text);self.assertIn(str(packages/'missing.deb'),text);self.assertNotIn(str(packages/'fixture.deb'),text)
 def test_package_symlink_is_rejected_before_manager_runs(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   packages,env,capture=self.fixture(Path(d),'deb');f=packages/'fixture.deb';f.unlink();f.symlink_to('/outside');r=subprocess.run(['/bin/sh',str(ASSETS/'x11-base-install.sh'),str(packages),'debian'],env=env,capture_output=True,timeout=5);self.assertNotEqual(0,r.returncode);self.assertFalse(capture.exists())
 def test_installed_database_is_read_once_for_a_fresh_package_set(self):
  for target,kind,query in [('debian','deb','dpkg-query:'),('manjaro','pkg.tar.zst','pacman:-Q'),('fedora','rpm','rpm:-qa')]:
   with self.subTest(target=target),tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
    packages,env,capture=self.fixture(Path(d),kind)
    with (packages.parent/'versions.tsv').open('a') as rows:
     for i in range(30):
      name='missing'+str(i);filename=name+'.'+kind;(packages/filename).write_text('fixture');rows.write(name+'\t1\t'+filename+'\n')
    result=subprocess.run(['/bin/sh',str(ASSETS/'x11-base-install.sh'),str(packages),target],env=env,capture_output=True,text=True,timeout=5);self.assertEqual(0,result.returncode,result.stderr)
    self.assertEqual(1,sum(line.startswith(query) for line in capture.read_text().splitlines()))
 def test_bulk_lookup_uses_exact_name_and_native_architecture(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   packages,env,capture=self.fixture(Path(d),'deb');env['CURRENT']='2';query=Path(d)/'bin/dpkg-query'
   query.write_text('#!/bin/sh\nprintf "prefix-fixture\\tarm64\\t9\\nfixture\\tarmhf\\t9\\nfixture\\tarm64\\t2\\n"\n');query.chmod(0o700)
   result=subprocess.run(['/bin/sh',str(ASSETS/'x11-base-install.sh'),str(packages),'debian'],env=env,capture_output=True,text=True,timeout=5);self.assertEqual(0,result.returncode,result.stderr)
   self.assertIn('fixture=2',capture.read_text());self.assertNotIn('fixture=9',capture.read_text())
 def test_recovery_selects_only_manifest_packages_and_never_deletes_journals(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   packages,env,capture=self.fixture(Path(d),'deb');updates=Path(env['DPKG_ADMINDIR'])/'updates';updates.mkdir(parents=True);journal=updates/'0000';journal.write_text('owned fixture journal')
   query=Path(d)/'bin/dpkg-query';query.write_text('#!/bin/sh\ncase "$*" in *db:Status-Status*)printf "fixture\\tarm64\\tunpacked\\nunrelated\\tarm64\\thalf-configured\\n";;*)printf "fixture\\tarm64\\t1\\n";;esac\n');query.chmod(0o700)
   result=subprocess.run(['/bin/sh',str(ASSETS/'x11-base-install.sh'),str(packages),'debian'],env=env,capture_output=True,text=True,timeout=5);self.assertEqual(0,result.returncode,result.stderr);calls=capture.read_text()
   selected=[line for line in calls.splitlines() if line.startswith('dpkg:') and '--configure' in line];self.assertEqual(1,len(selected));self.assertTrue(selected[0].endswith('-- fixture'),selected);self.assertNotIn('unrelated',selected[0]);self.assertNotIn('--configure -a',calls);self.assertEqual('owned fixture journal',journal.read_text())

class ProductionDispatch(unittest.TestCase):
 def test_fixed_install_branch_never_refreshes_upstream_or_enters_another_guest(self):
  for target in ['termux','ubuntu','debian','kali','alpine','fedora','opensuse','void','archlinux','manjaro']:
   with self.subTest(target=target),tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
    root=Path(d);prefix=root/'files/usr';bin=prefix/'bin';bin.mkdir(parents=True);control=prefix.parent/('workspace-x11'+('' if target=='termux' else '-'+target));control.mkdir();capture=root/'calls';(prefix/'var/lib/proot-distro/containers'/target/'rootfs').mkdir(parents=True)
    for name in ['module-sles-sink.so','module-native-protocol-unix.so']:
     folder=prefix/'lib/pulseaudio/modules';folder.mkdir(parents=True,exist_ok=True);(folder/name).touch()
    for name,body in {'python3':'printf "python:%s\\n" "$*" >> "$CAPTURE"','proot-distro':'printf "guest:%s\\n" "$*" >> "$CAPTURE"','setsid':'exit 0','pulseaudio':'exit 0','pkg':'echo unexpected-upstream >&2;exit 99','apt-get':'echo unexpected-upstream >&2;exit 99'}.items():
     p=bin/name;p.write_text('#!/bin/sh\n'+body+'\n');p.chmod(0o700)
    for name in ['x11-native-desktop.sh','x11-base-install.sh']:(control/name).write_text('#!/bin/sh\nprintf "local:%s\\n" "$*" >> "$CAPTURE"\n')
    env=dict(os.environ,PATH=str(bin)+':/usr/bin:/bin',PREFIX=str(prefix),CAPTURE=str(capture),JOBS_X11_PINNED_BASES='1',JOBS_X11_RENDERER='cpu')
    r=subprocess.run(['/bin/bash',str(ASSETS/'x11-desktop.sh'),'install',target,str(control)],env=env,capture_output=True,text=True,timeout=5);self.assertEqual(0,r.returncode,r.stdout+r.stderr);self.assertIn('READY:desktop',r.stdout);calls=capture.read_text();self.assertIn('x11-base.py '+target+' '+str(control),calls);self.assertIn('base-packages-20261028-'+target+'/packages',calls);self.assertNotIn('apt-get',calls);self.assertNotIn(' update',calls)
    if target!='termux':
     for line in calls.splitlines():
      if line.startswith('guest:'):self.assertIn(' '+target+' -- ',line)
 def test_fixed_install_missing_environment_is_not_replaced(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   root=Path(d);prefix=root/'files/usr';bin=prefix/'bin';bin.mkdir(parents=True);control=prefix.parent/'workspace-x11-kali';control.mkdir();(bin/'python3').symlink_to('/usr/bin/python3');env=dict(os.environ,PATH=str(bin)+':/usr/bin:/bin',PREFIX=str(prefix),JOBS_X11_PINNED_BASES='1')
   r=subprocess.run(['/bin/bash',str(ASSETS/'x11-desktop.sh'),'install','kali',str(control)],env=env,capture_output=True,text=True,timeout=5);self.assertNotEqual(0,r.returncode);self.assertIn('INSTALL_ENVIRONMENT_FIRST',r.stdout)

if __name__=='__main__':unittest.main()
