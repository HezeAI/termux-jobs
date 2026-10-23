from pathlib import Path
import json,os,subprocess,tempfile,time,unittest
ROOT=Path(__file__).resolve().parents[2];ASSETS=ROOT/'x11/src/main/assets'
class VirglTests(unittest.TestCase):
 def test_other_renderers_never_start_virgl_or_change_their_environment(self):
  for renderer in ['cpu','turnip','wrapper']:
   with self.subTest(renderer=renderer),tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
    script='source "$HELPER"\ntarget=debian;control="$CONTROL"\njobs_virgl_start;status=$?\n[[ $status == 2 && $GALLIUM_DRIVER == zink && $VK_DRIVER_FILES == selected-icd ]]'
    result=subprocess.run(['bash','-c',script],env=dict(os.environ,HELPER=str(ASSETS/'x11-virgl.sh'),CONTROL=d,PREFIX=d,JOBS_X11_RENDERER=renderer,GALLIUM_DRIVER='zink',VK_DRIVER_FILES='selected-icd'),capture_output=True,text=True,timeout=3)
    self.assertEqual(0,result.returncode,result.stderr);self.assertEqual([],list(Path(d).iterdir()))
 def start(self,base,label,server_fails=False,session_ends=False,server_ends=False,target="termux"):
  control=base/label;control.mkdir();prefix=base/'usr';(prefix/'bin').mkdir(parents=True,exist_ok=True)
  server=prefix/'bin/virgl_test_server_android'
  server.write_text('''#!/usr/bin/python3
import os,socket,sys,time,json,signal
from pathlib import Path
if os.environ.get('SERVER_FAIL')=='1':sys.exit(7)
p=sys.argv[sys.argv.index('--socket-path')+1];s=socket.socket(socket.AF_UNIX);s.bind(p);s.listen()
forked=None
if '--multi-clients' not in sys.argv:
 r,w=os.pipe();forked=os.fork()
 if forked==0:
  os.close(r)
  def stopped(sig,frame):
   Path(os.environ['CAPTURE']+'.child-stopped').write_text('stopped');sys.exit(0)
  signal.signal(signal.SIGTERM,stopped);os.write(w,b'1');os.close(w)
  while True:time.sleep(.05)
 os.close(w);os.read(r,1);os.close(r)
Path(os.environ['CAPTURE']).write_text(json.dumps(dict(args=sys.argv,socket=p,pid=os.getpid(),child=forked,group=os.getpgrp(),childGroup=os.getpgid(forked) if forked else None,gallium=os.environ.get('GALLIUM_DRIVER'),kopper=os.environ.get('LIBGL_KOPPER_DISABLE'),preload=os.environ.get('LD_PRELOAD'))))
if os.environ.get('SERVER_EXIT_LATER')=='1':time.sleep(.4);sys.exit(0)
while True:time.sleep(.1)
''');server.chmod(0o700)
  job=control/'job.sh';job.write_text('set -euo pipefail\nsource "$HELPER"\ncontrol="$CONTROL";target="$TEST_TARGET"\ntrap jobs_virgl_cleanup EXIT\njobs_virgl_start\necho SERVER_READY\njobs_desktop_run /usr/bin/python3 -c "import time;time.sleep('+('0.2' if session_ends else '30')+')"\n')
  env=dict(os.environ,PREFIX=str(prefix),HELPER=str(ASSETS/'x11-virgl.sh'),CONTROL=str(control),JOBS_X11_RENDERER='virgl',CAPTURE=str(control/'capture.json'),TEST_TARGET=target,SERVER_FAIL='1' if server_fails else '0',SERVER_EXIT_LATER='1' if server_ends else '0',GALLIUM_DRIVER='zink',LIBGL_KOPPER_DISABLE='true')
  p=subprocess.Popen(['setsid','bash',str(ASSETS/'x11-guardian.sh'),'bash',str(job)],env=env,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
  return p,control
 def stop(self,p):
  if p.stdin:p.stdin.close();p.stdin=None
  output=p.communicate(timeout=5)[0];p.stdout.close();return output
 def ready(self,control):
  for _ in range(100):
   if (control/'capture.json').is_file():return json.loads((control/'capture.json').read_text())
   time.sleep(.02)
  self.fail('Test server did not start')
 def test_distinct_sockets_and_stopping_one_owned_group_keeps_other_desktop(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   base=Path(d);p,a=self.start(base,'void',target='void');q,b=self.start(base,'opensuse',target='opensuse')
   try:
    first=self.ready(a);second=self.ready(b);self.assertNotEqual(first['socket'],second['socket']);self.assertIsNone(first['gallium']);self.assertIsNone(first['kopper']);self.assertIn('--angle-vulkan',first['args']);self.assertNotIn('--multi-clients',first['args']);self.assertLess(len(first['socket'].encode()),108)
    self.stop(p);self.assertTrue((a/'capture.json.child-stopped').exists());self.assertIsNone(q.poll());self.assertTrue(Path(second['socket']).exists());self.assertFalse(Path(first['socket']).exists());self.assertFalse((b/'capture.json.child-stopped').exists())
   finally:
    if p.poll() is None:self.stop(p)
    self.stop(q)
 def test_server_failure_does_not_launch_session_and_successful_session_cleans_socket(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   p,a=self.start(Path(d),'broken',server_fails=True)
   try:
    p.wait(timeout=7);output=p.stdout.read();p.stdout.close();p.stdin.close();p.stdin=None;self.assertIn('ERROR:VIRGL_SERVER_EXITED',output);self.assertNotIn('SERVER_READY',output)
   finally:
    if p.poll() is None:self.stop(p)
   q,b=self.start(Path(d),'completed',session_ends=True)
   try:
    record=self.ready(b);q.wait(timeout=5);output=q.stdout.read();q.stdout.close();q.stdin.close();q.stdin=None;self.assertIn('DESKTOP_CHILD_EXIT=0',output);self.assertFalse(Path(record['socket']).exists())
   finally:
    if q.poll() is None:self.stop(q)
 def test_setup_reuses_ready_components_and_never_installs_in_an_agent_environment(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   base=Path(d);prefix=base/'usr';tools=prefix/'bin';tools.mkdir(parents=True);angle=prefix/'opt/angle-android/gl';angle.mkdir(parents=True)
   for name in ['libEGL_angle.so','libGLESv2_angle.so']:(angle/name).touch()
   vk=prefix/'opt/angle-android/vulkan';vk.mkdir(parents=True)
   for name in ['libEGL_angle.so','libGLESv2_angle.so']:(vk/name).touch()
   for name,text in {'dpkg-query':'echo "install ok installed"','virgl_test_server_android':'echo "--socket-path --angle-gl --angle-vulkan --multi-clients"','apt-get':'echo UNEXPECTED_INSTALL;exit 9'}.items():
    p=tools/name;p.write_text('#!/bin/sh\n'+text+'\n');p.chmod(0o700)
   # Replace only the physical Android prefix assertion in this isolated fixture.
   source=(ASSETS/'x11-virgl-setup.sh').read_text();self.assertIn('[[ $PREFIX == /data/data/com.termux/files/usr ]]',source)
   fixture=base/'setup.sh';fixture.write_text(source.replace('[[ $PREFIX == /data/data/com.termux/files/usr ]]','[[ $PREFIX == "$TEST_PREFIX" ]]'))
   env=dict(os.environ,PREFIX=str(prefix),TEST_PREFIX=str(prefix),PATH=str(tools)+':/usr/bin:/bin')
   r=subprocess.run(['bash',str(fixture),'install','termux',str(base)],env=env,capture_output=True,text=True,timeout=5);self.assertEqual(0,r.returncode,r.stderr);self.assertIn('READY:virgl',r.stdout);self.assertNotIn('UNEXPECTED_INSTALL',r.stdout)
   library=vk/'libEGL_angle.so';library.rename(vk/'saved-EGL')
   try:
    r=subprocess.run(['bash',str(fixture),'check','termux',str(base)],env=env,capture_output=True,text=True,timeout=5)
    self.assertNotEqual(0,r.returncode);self.assertNotIn('READY:virgl',r.stdout)
   finally:(vk/'saved-EGL').rename(library)
   for target in ['ubuntu-ai','jobs-x11-ubuntu','termux;true']:
    r=subprocess.run(['bash',str(fixture),'install',target,str(base)],env=env,capture_output=True,text=True,timeout=5);self.assertEqual(2,r.returncode);self.assertNotIn('UNEXPECTED_INSTALL',r.stdout)

 def test_server_exit_after_connection_ends_only_its_owned_desktop_group(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   p,a=self.start(Path(d),'server-exit',server_ends=True)
   try:
    record=self.ready(a);p.wait(timeout=5);output=p.stdout.read();p.stdout.close();p.stdin.close();p.stdin=None;self.assertIn('SERVER_READY',output);self.assertIn('ERROR:VIRGL_SERVER_EXITED',output);self.assertIn('DESKTOP_CHILD_EXIT=1',output);self.assertFalse(Path(record['socket']).exists())
   finally:
    if p.poll() is None:self.stop(p)

 def test_each_guest_uses_only_its_own_package_manager_and_missing_components_block_readiness(self):
  plans={'ubuntu':('apt-get',['libgl1-mesa-dri','libglx-mesa0','libegl-mesa0']), 'debian':('apt-get',['libgl1-mesa-dri','libglx-mesa0','libegl-mesa0']), 'alpine':('apk',['mesa-dri-gallium','mesa-gl','mesa-egl']), 'fedora':('dnf',['mesa-dri-drivers','mesa-libGL','mesa-libEGL']), 'opensuse':('zypper',['Mesa-dri','Mesa-libGL1','Mesa-libEGL1']), 'void':('xbps-install',['mesa-dri','libglvnd']), 'archlinux':('pacman',['mesa','libglvnd'])}
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   base=Path(d);prefix=base/'usr';tools=prefix/'bin';tools.mkdir(parents=True);angle=prefix/'opt/angle-android/gl';angle.mkdir(parents=True)
   for name in ['libEGL_angle.so','libGLESv2_angle.so']:(angle/name).touch()
   vk=prefix/'opt/angle-android/vulkan';vk.mkdir(parents=True)
   for name in ['libEGL_angle.so','libGLESv2_angle.so']:(vk/name).touch()
   stub=tools/'stub';stub.write_text("""#!/usr/bin/python3
import os,sys,json
from pathlib import Path
name=Path(sys.argv[0]).name;args=sys.argv[1:];alias=os.environ.get('TEST_ALIAS','host')
with open(os.environ['TEST_LOG'],'a') as f:f.write(json.dumps([alias,name,args])+'\\n')
if name=='virgl_test_server_android':print('--socket-path --angle-gl --angle-vulkan --multi-clients');sys.exit(0)
if name=='proot-distro':
 split=args.index('--');assert args[split+1:split+3]==['/bin/sh','-s'],args
 alias=args[split-1];assert args[:1]==['run' if alias=='void' else 'login'],args
 os.environ['TEST_ALIAS']=alias;os.execv('/bin/sh',['/bin/sh']+args[split+2:])
if name=='python3':
 if 'x11-virgl-client.py' in ' '.join(args):print('JOBS_VIRGL_CLIENT=client-0123456789abcdef');sys.exit(0)
 sys.exit(1 if os.environ.get('TEST_DRIVER')=='0' else 0)
state=Path(os.environ['TEST_STATE'])
if name=='dpkg-query' and alias=='host':print('install ok installed');sys.exit(0)
install=name in ['apt-get','dnf','zypper'] and 'install' in args or name=='apk' and 'add' in args or name=='xbps-install' or name=='pacman' and '-Syu' in args
if install:
 if os.environ.get('TEST_FAIL')=='1':sys.exit(19)
 state.touch();sys.exit(0)
if name=='apt-get' and 'update' in args:sys.exit(0)
if not state.exists():sys.exit(1)
if name=='dpkg-query':print('install ok installed')
""");stub.chmod(0o700)
   for name in ['virgl_test_server_android','proot-distro','dpkg-query','apt-get','apk','dnf','zypper','rpm','xbps-install','xbps-query','pacman','python3']:(tools/name).symlink_to(stub)
   source=(ASSETS/'x11-virgl-setup.sh').read_text().replace('[[ $PREFIX == /data/data/com.termux/files/usr ]]','[[ $PREFIX == "$TEST_PREFIX" ]]').replace('export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin','export PATH="$TEST_TOOLS:/usr/bin:/bin"')
   client=base/'guest-client/client-0123456789abcdef/lib';client.mkdir(parents=True)
   source=source.replace('/tmp/termux-jobs-virgl',str(base/'guest-client'))
   fixture=base/'setup.sh';fixture.write_text(source)
   for target,(manager,packages) in plans.items():
    with self.subTest(target=target):
     log=base/'calls';state=base/'installed';env=dict(os.environ,PREFIX=str(prefix),TEST_PREFIX=str(prefix),PATH=str(tools)+':/usr/bin:/bin',TEST_TOOLS=str(tools),TEST_LOG=str(log),TEST_STATE=str(state))
     state.unlink(missing_ok=True);log.unlink(missing_ok=True)
     def run(op,fail=False):return subprocess.run(['bash',str(fixture),op,target,str(base)],env=dict(env,TEST_FAIL='1' if fail else '0'),capture_output=True,text=True,timeout=8)
     self.assertNotEqual(0,run('check').returncode);self.assertFalse(state.exists())
     r=run('install',True);self.assertNotEqual(0,r.returncode);self.assertNotIn('READY:virgl',r.stdout)
     log.unlink();r=run('install');self.assertEqual(0,r.returncode,r.stdout+r.stderr);self.assertIn('READY:virgl',r.stdout)
     calls=[json.loads(line) for line in log.read_text().splitlines()];installs=[args for alias,name,args in calls if alias==target and name==manager and (('install' in args) or ('add' in args) or manager=='xbps-install' or '-Syu' in args)]
     self.assertEqual(1,len(installs));self.assertTrue(set(packages).issubset(installs[0]),installs)
     self.assertFalse(any(alias not in [target,'host'] for alias,name,args in calls));self.assertFalse(any(alias=='host' and name=='apt-get' for alias,name,args in calls))
     bad=subprocess.run(['bash',str(fixture),'check',target,str(base)],env=dict(env,TEST_DRIVER='0'),capture_output=True,text=True,timeout=8);self.assertNotEqual(0,bad.returncode);self.assertNotIn('READY:virgl',bad.stdout)
     if target in {'debian','archlinux'}:
      library=vk/'libEGL_angle.so';library.rename(vk/'saved-EGL')
      try:
       missing=run('check');self.assertNotEqual(0,missing.returncode);self.assertNotIn('READY:virgl',missing.stdout)
      finally:(vk/'saved-EGL').rename(library)
     log.unlink();r=run('install');self.assertEqual(0,r.returncode);calls=[json.loads(line) for line in log.read_text().splitlines()];self.assertFalse(any(name==manager and ('install' in args or 'add' in args or manager=='xbps-install' or '-Syu' in args) for alias,name,args in calls))

 def test_new_guest_targets_can_start_and_cancel_their_own_servers(self):
  for target in ['termux','debian','alpine','fedora','opensuse','void','archlinux']:
   with self.subTest(target=target),tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
    child,control=self.start(Path(d),'desktop',target=target)
    try:
     record=self.ready(control);self.assertNotIn('--multi-clients',record['args']);self.assertNotIn('--no-fork',record['args']);self.assertIn('--angle-vulkan' if target in {'termux','void','fedora','debian','archlinux'} else '--angle-gl',record['args']);self.assertTrue(Path(record['socket']).exists())
    finally:
     if child.poll() is None:self.stop(child)
    self.assertFalse(Path(record['socket']).exists())
    self.assertEqual(record['group'],record['childGroup']);self.assertTrue((control/'capture.json.child-stopped').exists())
