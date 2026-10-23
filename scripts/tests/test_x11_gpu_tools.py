import contextlib,importlib.util,io,json,os,stat,subprocess,tempfile,unittest
from pathlib import Path
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[2]
def load(name):
 s=importlib.util.spec_from_file_location(name,ROOT/'x11/src/main/assets'/str(name+'.py'));m=importlib.util.module_from_spec(s);s.loader.exec_module(m);return m
m=load('x11-gpu-tools');native=load('x11-native-programs')
class GpuToolsTests(unittest.TestCase):
 def setUp(self):
  self.temp=tempfile.TemporaryDirectory(dir=ROOT/'.verification');self.home=Path(self.temp.name)/'home';self.home.mkdir();self.config=patch.dict(os.environ,{'XDG_CONFIG_HOME':str(self.home/'.config')});self.config.start();self.which=patch.object(m.shutil,'which',side_effect=lambda c:'/usr/bin/'+c);self.which.start()
 def tearDown(self):self.which.stop();self.config.stop();self.temp.cleanup()
 def test_per_environment_package_matrix_has_only_its_native_packages(self):
  self.assertEqual({'archlinux':['glmark2','mesa-utils'],'void':['glmark2','glxinfo'],'opensuse':['glmark2','Mesa-demo-x'],'alpine':['mesa-utils'],'fedora':['glmark2','glx-utils'],'termux':['glmark2','mesa-demos']},native.GPU_PACKAGES)
  for target in native.GROUPS:self.assertEqual(native.GPU_PACKAGES[target],native.packages(target,'gpu-tools'))
  self.assertEqual(['glxinfo','glxgears'],m.commands('alpine'));self.assertEqual(['glxinfo','glmark2-es2'],m.commands('opensuse'))
 def test_missing_commands_do_not_install_or_create_fake_ready_shortcuts(self):
  with patch.object(m.shutil,'which',return_value=None),patch.object(m.subprocess,'call') as command:
   self.assertFalse(m.ready('ubuntu'));self.assertEqual('missing-tools',m.prepare(self.home,'ubuntu'));self.assertEqual([],list(self.home.iterdir()));command.assert_not_called()
 def test_first_install_creates_two_english_executable_shortcuts_and_repeat_does_not_rewrite(self):
  self.assertEqual('ready',m.prepare(self.home,'termux'));desktop=self.home/'Desktop';files=sorted(desktop.glob('*.desktop'));self.assertEqual(2,len(files))
  for file in files:self.assertTrue(file.stat().st_mode&stat.S_IXUSR);self.assertIn('--disable-server',file.read_text());self.assertIn('--hold',file.read_text());self.assertIn('"termux"',file.read_text())
  self.assertIn('Name=GPU\n',(desktop/'termux-jobs-gpu.desktop').read_text());self.assertIn('Name=GPU INFO\n',(desktop/'termux-jobs-gpu-info.desktop').read_text())
  managed=[*files,*list((self.home/'.local/share/termux-jobs/gpu-tools').iterdir())];before={p:(p.stat().st_ino,p.read_bytes()) for p in managed};m.prepare(self.home,'termux');self.assertEqual(before,{p:(p.stat().st_ino,p.read_bytes()) for p in managed})
 def test_each_home_has_its_own_helper_shortcuts_and_target(self):
  for target in ['ubuntu','debian','alpine','fedora','opensuse','void','archlinux']:
   home=self.home/target;home.mkdir()
   with patch.dict(os.environ,{'XDG_CONFIG_HOME':str(home/'.config')}):m.prepare(home,target)
   text=(home/'Desktop/termux-jobs-gpu.desktop').read_text();self.assertIn('"'+target+'"',text);self.assertIn(str(home),text)
  self.assertFalse((self.home/'Desktop').exists())
 def test_localized_desktop_path_and_exec_escaping_without_shell_evaluation(self):
  config=self.home/'.config';config.mkdir();(config/'user-dirs.dirs').write_text('XDG_DESKTOP_DIR="$HOME/Рабочий стол %"\n');m.prepare(self.home,'debian');self.assertTrue((self.home/'Рабочий стол %/termux-jobs-gpu.desktop').is_file())
  self.assertEqual('"a\\\"b%%\\$\\`\\\\"',m.quote('a"b%$`\\'))
  (config/'user-dirs.dirs').write_text('XDG_DESKTOP_DIR="$(touch /tmp/not-executed)"\n')
  with self.assertRaises(ValueError):m.prepare(self.home,'debian')
 def test_user_edits_and_symlink_targets_are_preserved(self):
  m.prepare(self.home,'ubuntu');file=self.home/'Desktop/termux-jobs-gpu.desktop';file.write_text('my edited shortcut')
  with self.assertRaises(ValueError):m.prepare(self.home,'ubuntu')
  self.assertEqual('my edited shortcut',file.read_text());file.unlink();victim=self.home/'keep';victim.write_text('keep');file.symlink_to(victim)
  with self.assertRaises(OSError):m.prepare(self.home,'ubuntu')
  self.assertEqual('keep',victim.read_text())
 def test_app_owned_old_helper_updates_on_next_start_without_replacing_user_shortcuts(self):
  m.prepare(self.home,'alpine');support=self.home/'.local/share/termux-jobs/gpu-tools';helper=support/'gpu-tools.py';manifest=support/'shortcuts.json'
  old=b'# previous app helper\n';helper.write_bytes(old);known=json.loads(manifest.read_bytes());known[str(helper.relative_to(self.home))]=m.hashlib.sha256(old).hexdigest();manifest.write_text(json.dumps(known))
  shortcut=self.home/'Desktop/termux-jobs-gpu-info.desktop';before=(shortcut.stat().st_ino,shortcut.read_bytes())
  self.assertEqual('ready',m.prepare(self.home,'alpine'));self.assertEqual(Path(m.__file__).read_bytes(),helper.read_bytes());self.assertEqual(before,(shortcut.stat().st_ino,shortcut.read_bytes()))
 def test_linked_desktop_directory_and_outside_xdg_are_refused(self):
  outside=Path(self.temp.name)/'outside';outside.mkdir();(self.home/'Desktop').symlink_to(outside)
  with self.assertRaises(ValueError):m.prepare(self.home,'ubuntu')
  self.assertEqual([],list(outside.iterdir()));(self.home/'Desktop').unlink()
  config=self.home/'.config';config.mkdir();(config/'user-dirs.dirs').write_text('XDG_DESKTOP_DIR="'+str(outside)+'"\n')
  with self.assertRaises(ValueError):m.prepare(self.home,'ubuntu')
  self.assertEqual([],list(outside.iterdir()))
 def test_runner_selects_x11_commands_and_keeps_renderer_environment(self):
  with patch.dict(os.environ,{'DISPLAY':':105','LD_LIBRARY_PATH':'private-driver','VK_DRIVER_FILES':'selected-icd','JOBS_X11_RENDERER':'turnip'}):
   for target in sorted(m.TARGETS):
    for action in ['info','test']:
     with patch.object(m.subprocess,'call',return_value=0) as command,contextlib.redirect_stdout(io.StringIO()) as log:
      self.assertEqual(0,m.run(target,action));wanted=['glxinfo','-B'] if action=='info' else ['glxgears','-info'] if target=='alpine' else ['glmark2-es2'] if target=='opensuse' else ['glmark2'];command.assert_called_once_with(wanted);self.assertEqual('private-driver',os.environ['LD_LIBRARY_PATH']);self.assertIn(target,log.getvalue())
      if action=='test' and target=='alpine':self.assertIn('not a glmark2 score',log.getvalue())
 def test_missing_display_or_tool_has_actionable_message_without_process(self):
  for display,missing in [('',False),(':97',True)]:
   with patch.dict(os.environ,{'DISPLAY':display}),patch.object(m.subprocess,'call') as command,contextlib.redirect_stdout(io.StringIO()) as log:
    if missing:
     with patch.object(m.shutil,'which',return_value=None):self.assertEqual(1,m.run('termux','test'))
     self.assertIn('Programs > Recommended',log.getvalue())
    else:self.assertEqual(1,m.run('termux','test'));self.assertIn('running X11 desktop',log.getvalue())
    command.assert_not_called()
  for target in ['ubuntu-ai','debian;touch','../ubuntu']:
   with self.assertRaises(ValueError):m.commands(target)
 def test_virgl_info_uses_egl_only_for_affected_guests(self):
  with patch.dict(os.environ,{'DISPLAY':':105','JOBS_X11_RENDERER':'virgl'}):
   for target in sorted(m.TARGETS):
    with self.subTest(target=target),patch.object(m,'virgl_info',return_value=0) as egl,patch.object(m.subprocess,'call',return_value=0) as command,contextlib.redirect_stdout(io.StringIO()):
     self.assertEqual(0,m.run(target,'info'))
     if target in {'ubuntu','alpine','fedora','void','debian','archlinux','opensuse'}:egl.assert_called_once_with();command.assert_not_called()
     else:egl.assert_not_called();command.assert_called_once_with(['glxinfo','-B'])
   for target in ['ubuntu','alpine','fedora','void','debian','archlinux','opensuse']:
    with patch.object(m,'virgl_info') as egl,patch.object(m.subprocess,'call',return_value=0) as command,contextlib.redirect_stdout(io.StringIO()):
     self.assertEqual(0,m.run(target,'test'));egl.assert_not_called()
 def test_egl_worker_keeps_selected_driver_and_socket_without_mutating_parent(self):
  from types import SimpleNamespace
  with patch.dict(os.environ,{'DISPLAY':':105','LD_LIBRARY_PATH':'selected-mesa','LIBGL_DRIVERS_PATH':'selected-dri','VTEST_SOCKET_NAME':'own-socket','EGL_PLATFORM':'x11'}),patch.object(m.subprocess,'run',return_value=SimpleNamespace(returncode=0)) as child:
   before=dict(os.environ);self.assertEqual(0,m.virgl_info());self.assertEqual(before,dict(os.environ))
   args,kwargs=child.call_args
   self.assertEqual([m.sys.executable,'-B',str(Path(m.__file__).resolve()),'virgl-info'],args[0]);self.assertEqual(20,kwargs['timeout'])
   self.assertEqual(dict(before,EGL_PLATFORM='surfaceless',PYTHONDONTWRITEBYTECODE='1'),kwargs['env'])
 def test_cpu_and_turnip_on_affected_guests_keep_their_original_glx_paths(self):
  import ctypes
  for renderer in ['cpu','turnip']:
   for target in ['ubuntu','alpine','fedora','void','debian','archlinux','opensuse']:
    for action in ['info','test']:
     with self.subTest(renderer=renderer,target=target,action=action),patch.dict(os.environ,{'DISPLAY':':105','JOBS_X11_RENDERER':renderer,'EGL_PLATFORM':'x11','LD_LIBRARY_PATH':'selected-'+renderer,'VK_DRIVER_FILES':'own-icd'}),patch.object(m,'virgl_info') as egl,patch.object(ctypes,'CDLL') as load,patch.object(m.subprocess,'call',return_value=0) as command,contextlib.redirect_stdout(io.StringIO()):
      before=dict(os.environ);self.assertEqual(0,m.run(target,action));self.assertEqual(before,dict(os.environ));egl.assert_not_called();load.assert_not_called()
      command.assert_called_once_with(['glxinfo','-B'] if action=='info' else ['glxgears','-info'] if target=='alpine' else ['glmark2-es2'] if target=='opensuse' else ['glmark2'])
 def test_egl_worker_timeout_and_crash_are_explicit_failures_without_glx_fallback(self):
  from types import SimpleNamespace
  for result in [subprocess.TimeoutExpired('worker',20),SimpleNamespace(returncode=-11),SimpleNamespace(returncode=1)]:
   with patch.object(m.subprocess,'run',side_effect=result if isinstance(result,Exception) else None,return_value=result),patch.object(m.subprocess,'call') as glx,contextlib.redirect_stdout(io.StringIO()) as log:
    self.assertEqual(1,m.virgl_info());glx.assert_not_called();self.assertIn('GPU INFO',log.getvalue());self.assertIn('diagnostics',log.getvalue())
 def test_egl_query_failure_releases_only_its_own_context_and_surface(self):
  from unittest.mock import Mock
  import ctypes
  for failed in ['eglInitialize','eglChooseConfig','eglCreatePbufferSurface','eglCreateContext','eglMakeCurrent']:
   library=Mock()
   for name in ['eglGetDisplay','eglInitialize','eglBindAPI','eglChooseConfig','eglCreatePbufferSurface','eglCreateContext','eglMakeCurrent','eglGetProcAddress','eglDestroyContext','eglDestroySurface','eglTerminate']:
    getattr(library,name).return_value=123
   def choose(display,attrs,config,size,count):count._obj.value=1;config._obj.value=456;return 1
   library.eglChooseConfig.side_effect=choose
   if failed=='eglChooseConfig':library.eglChooseConfig.side_effect=lambda *args:0
   else:getattr(library,failed).return_value=0
   library.eglGetError.return_value=0x3009
   with self.subTest(failed=failed),patch.object(ctypes,'CDLL',return_value=library):
    with self.assertRaisesRegex(ValueError,'EGL .* failed'):m.egl_info()
    if failed=='eglInitialize':library.eglTerminate.assert_not_called()
    else:library.eglTerminate.assert_called_once_with(123)
    if failed=='eglMakeCurrent':library.eglDestroyContext.assert_called_once_with(123,123)
    else:library.eglDestroyContext.assert_not_called()
    if failed in {'eglMakeCurrent','eglCreateContext'}:library.eglDestroySurface.assert_called_once_with(123,123)
    else:library.eglDestroySurface.assert_not_called()
 def test_native_install_uses_own_package_manager_and_only_then_prepares_shortcuts(self):
  from unittest.mock import Mock
  original=native.load
  for target in ['archlinux','void','opensuse','alpine','fedora']:
   wanted=native.GPU_PACKAGES[target];have=set();calls=[];helper=Mock();helper.ready.return_value=True
   def install(args):
    calls.append(args)
    if args[0] in ['pacman','xbps-install','zypper','apk','dnf'] and all(p in args for p in wanted):have.update(wanted)
   def dynamic(name):return helper if name=='x11-gpu-tools' else original(name)
   with patch.dict(os.environ,{'JOBS_X11_NATIVE_TARGET':target}),patch.object(native.sys,'argv',['tool','install','gpu-tools']),patch.object(native,'run',side_effect=install),patch.object(native,'installed',side_effect=lambda t:have),patch.object(native,'load',side_effect=dynamic),patch.object(native,'zypper_repositories',return_value=contextlib.nullcontext([])),contextlib.redirect_stdout(io.StringIO()) as log:
    native.main();helper.prepare.assert_called_once_with(Path.home(),target);self.assertIn('PROGRAM:gpu-tools:installed',log.getvalue());self.assertTrue(any(all(p in args for p in wanted) for args in calls));self.assertFalse(any(args[0]=='apt-get' for args in calls))
 def test_missing_native_binary_is_not_reported_as_installed(self):
  from unittest.mock import Mock
  helper=Mock();helper.ready.return_value=False;original=native.load
  with patch.dict(os.environ,{'JOBS_X11_NATIVE_TARGET':'alpine'}),patch.object(native.sys,'argv',['tool','check']),patch.object(native,'installed',return_value={'mesa-utils'}),patch.object(native,'load',side_effect=lambda name:helper if name=='x11-gpu-tools' else original(name)),contextlib.redirect_stdout(io.StringIO()) as log:
   native.main();self.assertIn('PROGRAM:gpu-tools:missing',log.getvalue());helper.prepare.assert_not_called()
 def test_common_session_and_program_jobs_package_the_helper(self):
  java=(ROOT/'x11/src/main/java/com/termux/x11/DesktopProcess.java').read_text();self.assertEqual(2,java.count('asset(context,base,"x11-gpu-tools.py")'))
  session=(ROOT/'x11/src/main/assets/x11-session.sh').read_text();self.assertIn('prepare "${JOBS_X11_TARGET:-termux}"',session)
  self.assertNotIn('glmark2',(ROOT/'x11/src/main/assets/x11-native-desktop.sh').read_text())
