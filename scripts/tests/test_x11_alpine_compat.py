"""Real preload ABI/local-scope and existence fallback tests, no installed rootfs."""
import os,subprocess,tempfile,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
class AlpineCompatTests(unittest.TestCase):
 def test_local_and_global_glycin_and_narrow_filesystem_fallback(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   d=Path(d);shim=d/'compat.so';fake=d/'fake.c';lib=d/'libglycin-2.so.0'
   subprocess.run(['cc','-fPIC','-shared','-O2','-Wall','-Wextra','-Werror',str(ROOT/'x11/src/main/cpp/x11-alpine-compat.c'),'-ldl','-o',str(shim)],check=True,capture_output=True)
   fake.write_text('#include <errno.h>\nstatic int selection;\nvoid *gly_loader_new(void *f){(void)f;selection=0;return &selection;}\nvoid gly_loader_set_sandbox_selector(void *p,int value){*(int*)p=value;}\nvoid *gly_creator_new(const char *m,void **e){(void)m;(void)e;selection=0;return &selection;}\nvoid gly_creator_set_sandbox_selector(void *p,int value){*(int*)p=value;}\nint faccessat(int fd,const char *path,int mode,int flags){(void)fd;(void)path;if(mode==0 && flags==0x300){errno=EINVAL;return -1;}errno=EACCES;return -1;}\n')
   subprocess.run(['cc','-fPIC','-shared',str(fake),'-o',str(lib)],check=True,capture_output=True)
   (d/'exists').touch();(d/'dangling').symlink_to('missing')
   test=d/'probe.py';test.write_text('''import ctypes as c,os
lib=c.CDLL('libglycin-2.so.0',mode=int(os.environ['SCOPE']))
global_lib=c.CDLL(None,use_errno=True)
new=global_lib.gly_loader_new;new.argtypes=[c.c_void_p];new.restype=c.POINTER(c.c_int)
assert new(None).contents.value==3
creator=global_lib.gly_creator_new;creator.argtypes=[c.c_char_p,c.c_void_p];creator.restype=c.POINTER(c.c_int)
assert creator(b'image/png',None).contents.value==3
if int(os.environ['SCOPE'])==0:
 print('RTLD_LOCAL selector OK');raise SystemExit(0)
access=global_lib.faccessat;access.argtypes=[c.c_int,c.c_char_p,c.c_int,c.c_int]
base=os.environ['FIXTURE'].encode()
assert access(-100,base+b'/exists',0,0x300)==0
assert access(-100,base+b'/dangling',0,0x300)==0
assert access(-100,base+b'/missing',0,0x300)==-1 and c.get_errno()==2
assert access(-100,base+b'/exists',4,0x300)==-1 and c.get_errno()==13
assert access(-100,base+b'/exists',0,0)==-1 and c.get_errno()==13
print('ABI and exact fallback OK')
''')
   for scope in [os.RTLD_LOCAL,os.RTLD_GLOBAL]:
    env={**os.environ,'LD_PRELOAD':str(shim)+':'+str(lib),'LD_LIBRARY_PATH':str(d),'SCOPE':str(scope),'FIXTURE':str(d)}
    # Separate library scope case below does not preload the fake library.
    if scope==os.RTLD_LOCAL:env['LD_PRELOAD']=str(shim)
    result=subprocess.run(['python3','-B',str(test)],env=env,capture_output=True,text=True,timeout=15)
    self.assertEqual(0,result.returncode,result.stderr)
 def test_only_alpine_session_enables_the_module(self):
  text=(ROOT/'x11/src/main/assets/x11-desktop.sh').read_text();self.assertIn('alpine_compat=0;if [[ $target == alpine ]];then alpine_compat=1;fi',text)
  guest=text.split('alpine_compat=0;',1)[1];self.assertLess(guest.index('export LD_PRELOAD='),guest.index('exec dbus-run-session'))
  session=(ROOT/'x11/src/main/assets/x11-session.sh').read_text();self.assertIn('"${JOBS_X11_ALPINE_COMPAT:-0}" = 1',session);self.assertIn('ERROR:ALPINE_COMPAT_MISSING',session)

 def test_private_bus_activation_inherits_environment_at_bus_start_not_later_shell(self):
  # Real isolated D-Bus, fixture service only. No installed bus or desktop touched.
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   base=Path(d);services=base/'share/dbus-1/services';services.mkdir(parents=True)
   capture=base/'env';service=base/'activated.sh';service.write_text('#!/bin/sh\nprintf "%s" "${JOBS_FIXTURE_COMPAT:-absent}" > "$CAPTURE"\n');service.chmod(0o700)
   (services/'org.jobs.CompatFixture.service').write_text('[D-BUS Service]\nName=org.jobs.CompatFixture\nExec='+str(service)+'\n')
   command='export JOBS_FIXTURE_COMPAT=enabled; dbus-send --session --type=method_call --print-reply --reply-timeout=3000 --dest=org.jobs.CompatFixture /org/jobs/CompatFixture org.jobs.CompatFixture.Ping >/dev/null 2>&1 || :'
   env={**os.environ,'XDG_DATA_HOME':str(base/'share'),'CAPTURE':str(capture),'TMPDIR':str(base)};env.pop('JOBS_FIXTURE_COMPAT',None)
   for preload,expected in [(False,'absent'),(True,'enabled')]:
    if preload:env['JOBS_FIXTURE_COMPAT']='enabled'
    p=subprocess.run(['dbus-run-session','--','sh','-c',command],env=env,capture_output=True,text=True,timeout=10)
    self.assertEqual(0,p.returncode,p.stderr);self.assertEqual(expected,capture.read_text())
