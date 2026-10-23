"""Fedora/Arch decoder ABI/scope; no changes to installed systems."""
import os,subprocess,tempfile,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
class FedoraGlycinTests(unittest.TestCase):
 def test_constructor_selectors_work_for_global_and_local_glycin(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   base=Path(d);shim=base/'compat.so';fake=base/'fake.c'
   subprocess.run(['cc','-fPIC','-shared','-nostdlib','-O2','-Wall','-Wextra','-Werror',str(ROOT/'x11/src/main/cpp/x11-fedora-glycin.c'),'-o',str(shim)],check=True,capture_output=True)
   fake.write_text('static int value;void *gly_loader_new(void *f){(void)f;value=0;return &value;}void gly_loader_set_sandbox_selector(void *p,int v){*(int*)p=v;}void *gly_creator_new(const char *m,void **e){(void)m;(void)e;value=0;return &value;}void gly_creator_set_sandbox_selector(void *p,int v){*(int*)p=v;}')
   subprocess.run(['cc','-fPIC','-shared',str(fake),'-o',str(base/'libglycin-2.so.0')],check=True,capture_output=True)
   probe=base/'probe.py';probe.write_text('''import ctypes as c,os
lib=c.CDLL('libglycin-2.so.0',mode=int(os.environ['SCOPE']))
shim=c.CDLL(None)
new=shim.gly_loader_new;new.argtypes=[c.c_void_p];new.restype=c.POINTER(c.c_int)
assert new(None).contents.value==3
create=shim.gly_creator_new;create.argtypes=[c.c_char_p,c.c_void_p];create.restype=c.POINTER(c.c_int)
assert create(b'image/png',None).contents.value==3
''')
   for scope in [os.RTLD_LOCAL,os.RTLD_GLOBAL]:
    with self.subTest(scope=scope):
     r=subprocess.run(['python3','-B',str(probe)],env=dict(os.environ,LD_PRELOAD=str(shim),LD_LIBRARY_PATH=str(base),SCOPE=str(scope)),capture_output=True,text=True,timeout=10)
     self.assertEqual(0,r.returncode,r.stderr)
   symbols=subprocess.check_output(['nm','-D','--defined-only',str(shim)],text=True)
   self.assertNotIn('faccessat',symbols);self.assertNotIn('stat',symbols)
 def test_fedora_arch_only_and_preload_before_private_bus(self):
  shell=(ROOT/'x11/src/main/assets/x11-desktop.sh').read_text()
  start=shell.index('if [ "$JOBS_X11_TARGET" = fedora ]')
  self.assertIn('x11-fedora-glycin.so',shell[start:])
  self.assertLess(shell.index('export LD_PRELOAD=',start),shell.index('exec dbus-run-session',start))
  self.assertIn('ERROR:FEDORA_GLYCIN_MISSING',shell[start:])
  session=(ROOT/'x11/src/main/assets/x11-session.sh').read_text()
  self.assertIn('"${JOBS_X11_TARGET:-}" = fedora',session)
  java=(ROOT/'x11/src/main/java/com/termux/x11/DesktopProcess.java').read_text()
  self.assertIn('if(target.equals("fedora")||target.equals("archlinux"))asset(context,base,"x11-fedora-glycin.so")',java)
  self.assertIn('[ "$JOBS_X11_TARGET" = archlinux ]',shell[start:])
  self.assertIn('[ "${JOBS_X11_TARGET:-}" = archlinux ]',session)
 def test_executed_preload_scope_does_not_escape_to_other_environments(self):
  source=(ROOT/'x11/src/main/assets/x11-session.sh').read_text();block=source.split('# Same Glycin 2 ABI',1)[1].split('\nfi\n',1)[0];block='# Same Glycin 2 ABI'+block+'\nfi\n'
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   base=Path(d);(base/'x11-fedora-glycin.so').touch()
   for target in ['termux','ubuntu','jobs-x11-ubuntu','debian','alpine','fedora','opensuse','void','archlinux','ubuntu-ai']:
    with self.subTest(target=target):
     script='jobs_control="$CONTROL"\nLD_PRELOAD=\n'+block+'printf "PRELOAD:%s\\n" "${LD_PRELOAD-}"\n'
     p=subprocess.run(['/bin/sh','-c',script],env=dict(os.environ,CONTROL=str(base),JOBS_X11_TARGET=target,LD_PRELOAD=''),text=True,capture_output=True,timeout=3)
     self.assertEqual(0,p.returncode,p.stderr);expected=str(base/'x11-fedora-glycin.so') if target in ('fedora','archlinux') else '';self.assertIn('PRELOAD:'+expected+'\n',p.stdout)
   (base/'x11-fedora-glycin.so').unlink()
   p=subprocess.run(['/bin/sh','-c','jobs_control="$CONTROL"\n'+block],env=dict(os.environ,CONTROL=str(base),JOBS_X11_TARGET='archlinux',LD_PRELOAD=''),capture_output=True,timeout=3);self.assertEqual(1,p.returncode)
