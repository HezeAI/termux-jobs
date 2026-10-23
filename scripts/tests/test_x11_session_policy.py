import os,subprocess,tempfile,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
class PolicyTests(unittest.TestCase):
 def test_override_is_session_only_and_preserves_user_config(self):
  for target in ['fedora','debian']:
   with self.subTest(target=target),tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
    root=Path(d);runtime=root/'run';runtime.mkdir();control=root/'control';control.mkdir();tools=root/'bin';tools.mkdir();home=root/'home';user=home/'.config/autostart/xfce-polkit.desktop';user.parent.mkdir(parents=True);user.write_text('user config')
    subprocess.run(['cc','-shared','-fPIC','-nostdlib',str(ROOT/'x11/src/main/cpp/x11-fedora-glycin.c'),'-o',str(control/'x11-fedora-glycin.so')],check=True,capture_output=True)
    for name in ['fcitx5','dbus-monitor','xfwm4','python3','xfce4-session']:
     p=tools/name;p.write_text('#!/bin/sh\n'+('printf "%s" "$XDG_CONFIG_DIRS" > "$CAPTURE"\n' if name=='xfce4-session' else '')+'exit 0\n');p.chmod(0o700)
    env=dict(os.environ,PATH=str(tools)+':/usr/bin:/bin',HOME=str(home),XDG_CONFIG_HOME=str(home/'.config'),XDG_RUNTIME_DIR=str(runtime),XDG_CONFIG_DIRS='/etc/xdg',JOBS_X11_FOCUS_TOKEN='12345678-1234-1234-1234-123456789abc',JOBS_X11_TARGET=target,JOBS_X11_CONTROL=str(control),CAPTURE=str(root/'capture'));env.pop('LD_PRELOAD',None)
    r=subprocess.run(['/bin/sh',str(ROOT/'x11/src/main/assets/x11-session.sh')],env=env,capture_output=True,text=True,timeout=10)
    self.assertEqual(0,r.returncode,r.stderr);self.assertEqual('user config',user.read_text());xdg=runtime/'termux-jobs-xdg'
    if target=='fedora':self.assertIn('Hidden=true',(xdg/'autostart/xfce-polkit.desktop').read_text());self.assertEqual(str(xdg)+':/etc/xdg',(root/'capture').read_text())
    else:self.assertFalse(xdg.exists());self.assertEqual('/etc/xdg',(root/'capture').read_text())


 def test_retired_dashboard_is_never_launched_or_autostarted(self):
  script=(ROOT/'x11/src/main/assets/x11-session.sh').read_text()
  self.assertNotIn('x11-touch-workspace',script);self.assertNotIn('JOBS_X11_STYLE:-0}" = 2',script)
