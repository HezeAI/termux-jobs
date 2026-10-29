"""Execute per-app compatibility in disposable roots, without nested PRoot/GUI."""
import importlib.util,json,os,shutil,subprocess,tempfile,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];ASSET=ROOT/'x11/src/main/assets/x11-app-launchers.py'
s=importlib.util.spec_from_file_location('apps',ASSET);m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
class AppLaunchers(unittest.TestCase):
 def fixture(self,d):
  root=Path(d)/'system with spaces';root.mkdir();control=Path(d)/'control';control.mkdir();return root,control
 def binary(self,root,path,body=None):
  file=root/path.lstrip('/');file.parent.mkdir(parents=True,exist_ok=True)
  file.write_text(body or '#!/usr/bin/python3\nimport json,os,sys\nprint(json.dumps({"args":sys.argv[1:],"gsk":os.getenv("GSK_RENDERER"),"driver":os.getenv("GALLIUM_DRIVER")}))\n');file.chmod(0o700);return file
 def test_new_install_live_install_repeat_and_remove(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   root,control=self.fixture(d);folder=m.prepare('kali',control,root);self.assertEqual([],list(folder.iterdir()))
   app=self.binary(root,'/usr/bin/evince');m.prepare('kali',control,root);wrapper=folder/'evince';before=wrapper.stat().st_mtime_ns
   m.prepare('kali',control,root);self.assertEqual(before,wrapper.stat().st_mtime_ns)
   marker=Path(d)/'must-not-exist';args=['file with spaces.pdf','; touch '+str(marker)]
   parent=dict(os.environ,GSK_RENDERER='vulkan',GALLIUM_DRIVER='zink')
   p=subprocess.run([str(wrapper),*args],env=parent,capture_output=True,text=True,check=True);result=json.loads(p.stdout)
   self.assertEqual(args,result['args']);self.assertEqual('cairo',result['gsk']);self.assertEqual('zink',result['driver']);self.assertEqual('vulkan',parent['GSK_RENDERER']);self.assertFalse(marker.exists())
   app.unlink();m.prepare('kali',control,root);self.assertFalse(wrapper.exists());self.assertTrue(folder.is_dir())
 def test_other_environments_have_no_side_effects(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   root,control=self.fixture(d)
   for target in ('termux','ubuntu','ubuntu-ai','jobs-x11-ubuntu','debian','alpine','fedora','archlinux','opensuse','void'):
    self.assertIsNone(m.prepare(target,control,root));self.assertEqual([],list(control.iterdir()))
 def test_soffice_canonical_path_avoids_missing_sed_in_real_startup_fragment(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   root,control=self.fixture(d);tools=Path(d)/'bin';tools.mkdir()
   for name in ('dirname','basename','ls'):(tools/name).symlink_to(shutil.which(name))
   # LibreOffice's installed shell path resolution, with its real sed dependency.
   fragment='''#!/bin/sh
sd_cwd=$(pwd)
sd_res="$0"
while [ -h "$sd_res" ] ; do
 sd_dirname=$(dirname "$sd_res")
 cd "$sd_dirname" || exit $?
 sd_basename=$(basename "$sd_res")
 sd_res=$(ls -l "$sd_basename" | sed "s/.*$sd_basename -> //g")
done
sd_dirname=$(dirname "$sd_res")
cd "$sd_dirname" || exit $?
sd_prog=$(pwd)
cd "$sd_cwd" || exit $?
printf '%s\\n' "$sd_prog"
'''
   script=self.binary(root,'/usr/lib/libreoffice/program/soffice',fragment);link=root/'usr/bin/libreoffice';link.parent.mkdir(parents=True);link.symlink_to(script)
   env=dict(os.environ,PATH=str(tools));broken=subprocess.run([str(link)],env=env,capture_output=True,text=True);self.assertIn('sed',broken.stderr);self.assertNotEqual(str(script.parent),broken.stdout.strip())
   folder=m.prepare('manjaro',control,root)
   for name in ('libreoffice','soffice'):
    fixed=subprocess.run([str(folder/name),'--writer'],env=env,capture_output=True,text=True);self.assertEqual(0,fixed.returncode,fixed.stderr);self.assertEqual(str(script.parent),fixed.stdout.strip());self.assertEqual('',fixed.stderr)
   self.assertFalse((tools/'sed').exists())
 def test_edited_files_links_and_unknown_path_entries_are_preserved(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   root,control=self.fixture(d);self.binary(root,'/usr/bin/evince');folder=m.prepare('kali',control,root);file=folder/'evince';file.write_text('user edit')
   with self.assertRaisesRegex(ValueError,'LOCAL_CHANGES'):m.prepare('kali',control,root)
   self.assertEqual('user edit',file.read_text());file.unlink();victim=Path(d)/'keep';victim.write_text('external')
   file.symlink_to(victim)
   with self.assertRaises(OSError):m.prepare('kali',control,root)
   self.assertEqual('external',victim.read_text());file.unlink();os.link(victim,file)
   with self.assertRaises(ValueError):m.prepare('kali',control,root)
   file.unlink();(folder/'python3').write_text('keep')
   with self.assertRaisesRegex(ValueError,'UNMANAGED'):m.prepare('kali',control,root)
   self.assertEqual('keep',(folder/'python3').read_text());self.assertEqual('external',victim.read_text())
 def test_directory_and_lock_symlinks_never_redirect_writes(self):
  for name in ('app-launchers','app-launchers.lock'):
   with self.subTest(name=name),tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
    root,control=self.fixture(d);outside=Path(d)/'outside'
    if name=='app-launchers':outside.mkdir()
    else:outside.write_text('keep')
    (control/name).symlink_to(outside)
    with self.assertRaises((ValueError,OSError)):m.prepare('kali',control,root)
    if outside.is_dir():self.assertEqual([],list(outside.iterdir()))
    else:self.assertEqual('keep',outside.read_text())
 def test_actual_entrypoint_rejects_mismatched_environment(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   control=Path(d);script=control/ASSET.name;shutil.copyfile(ASSET,script)
   env=dict(os.environ,JOBS_X11_TARGET='manjaro');env.pop('JOBS_X11_NATIVE_TARGET',None)
   p=subprocess.run(['/usr/bin/python3','-B',str(script),'kali'],env=env,capture_output=True,text=True)
   self.assertEqual(1,p.returncode);self.assertIn('APP_LAUNCH_SCOPE',p.stderr);self.assertEqual([script],list(control.iterdir()))
 def test_session_and_both_program_installers_stage_and_update_same_controls(self):
  java=(ROOT/'x11/src/main/java/com/termux/x11/DesktopProcess.java').read_text();self.assertEqual(2,java.count('asset(context,base,"x11-app-launchers.py")'))
  session=(ROOT/'x11/src/main/assets/x11-session.sh').read_text();self.assertIn('export PATH="$jobs_apps:$PATH"',session);self.assertNotIn('export GSK_RENDERER',session)
  shell=(ROOT/'x11/src/main/assets/x11-programs.sh').read_text();self.assertIn('x11-app-launchers.py "$target"',shell)
  native=(ROOT/'x11/src/main/assets/x11-native-programs.py').read_text();self.assertIn("load('x11-app-launchers').prepare(target,Path(__file__).parent)",native)
if __name__=='__main__':unittest.main()
