"""Isolated launch fixtures. No installed Termux/rootfs or existing D-Bus touched."""
from pathlib import Path
import os,subprocess,tempfile,unittest
ROOT=Path(__file__).resolve().parents[2]
SESSION=ROOT/'x11/src/main/assets/x11-session.sh'
DESKTOP=ROOT/'x11/src/main/assets/x11-desktop.sh'
class KeyboardFixtures(unittest.TestCase):
 def test_session_starts_private_focus_gate_and_owned_helpers(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   base=Path(d);bin=base/'bin';bin.mkdir();log=base/'calls'
   scripts={
    'python3': 'if [ "$1" = -B ] && [ "$2" = "$JOBS_X11_CONTROL/x11-bookmarks.py" ];then exec /usr/bin/python3 "$@";fi; if [ "$1" = -B ] && [ "$2" = -u ] && [ "$3" = "$JOBS_X11_CONTROL/x11-focus.py" ];then printf "focus-gate-started\\n" >> "$CALLS";exit 0;fi; test "$1" = -u && test "$2" = "$JOBS_X11_CONTROL/x11-ready.py" || exit 7; printf "JOBS_X11_DESKTOP_${FIXTURE_STARTUP:-READY} %s\\n" "$JOBS_X11_FOCUS_TOKEN"',
    'fcitx5': 'printf "fcitx-started\\n" >> "$CALLS"',
    'xfwm4': 'printf "wm:%s\\n" "$*" >> "$CALLS"',
    'xprop': 'printf "_NET_SUPPORTING_WM_CHECK(WINDOW): window id # 0xa0001\\n"',
    'xwininfo': 'printf \'0x1 ("xfce4-panel" "Xfce4-panel")\\n0x2 ("xfdesktop" "Xfdesktop")\\n\'',
    'xfce4-session': 'xfwm4; printf "session:%s:%s:%s\\n" "$GTK_IM_MODULE" "$QT_IM_MODULE" "$XMODIFIERS" >> "$CALLS"; sleep .1'
   }
   for name,body in scripts.items():
    p=bin/name;p.write_text('#!/bin/sh\n'+body+'\n');p.chmod(0o700)
   # The session requires a Fedora overlay asset; this fixture tests shell wiring, not native loading.
   (base/'x11-focus.py').write_text('# fixture: gate semantics tested separately')
   (base/'x11-fedora-glycin.so').write_bytes(b'fixture')
   (base/'x11-bookmarks.py').write_bytes((ROOT/'x11/src/main/assets/x11-bookmarks.py').read_bytes())
   (base/'home').mkdir()
   token='a11a11a1-1111-4111-8111-111111111111'
   env={**os.environ,'PATH':str(bin)+':/usr/bin:/bin','CALLS':str(log),'JOBS_X11_FOCUS_TOKEN':token,'JOBS_X11_CONTROL':str(base),'XDG_RUNTIME_DIR':str(base),'HOME':str(base/'home'),'XDG_CONFIG_HOME':str(base/'home/.config')}
   p=subprocess.run(['/bin/sh',str(SESSION)],env=env,capture_output=True,text=True,timeout=5)
   self.assertEqual(0,p.returncode,p.stderr);self.assertIn('focus-gate-started',log.read_text())
   self.assertIn(f'JOBS_X11_DESKTOP_READY {token}',p.stdout);self.assertNotIn('JOBS_X11_DESKTOP_TIMEOUT',p.stdout);self.assertNotIn('CommitString',p.stdout);self.assertIn('wm:--compositor=on',log.read_text());self.assertNotIn('--compositor=off',log.read_text());self.assertIn('session:fcitx:fcitx:@im=fcitx',log.read_text())
   # Exercise the real session script with isolated commands for each environment/mode.
   for target,renderer,expected in [('ubuntu','turnip','off'),('ubuntu','cpu','on'),('termux','turnip','on'),('debian','turnip','off'),('alpine','turnip','off'),('fedora','turnip','off'),('fedora','cpu','on'),('debian','cpu','on'),('alpine','cpu','on'),('void','cpu','on'),('void','turnip','off'),('archlinux','cpu','on'),('archlinux','turnip','off'),('archlinux','virgl','off'),('opensuse','cpu','on'),('opensuse','turnip','off')]:
    log.write_text('')
    result=subprocess.run(['/bin/sh',str(SESSION)],env={**env,'JOBS_X11_TARGET':target,'JOBS_X11_RENDERER':renderer},capture_output=True,text=True,timeout=5)
    self.assertEqual(0,result.returncode,result.stderr);self.assertIn('wm:--compositor='+expected,log.read_text());self.assertIn('JOBS_X11_COMPOSITOR='+expected,result.stdout);self.assertEqual(1,log.read_text().count('wm:'));self.assertIn('JOBS_X11_STARTUP=managed-window-manager',result.stdout)

   self.assertIn('focus-gate-started',log.read_text());self.assertEqual(3,len((base/'home/.config/gtk-3.0/bookmarks').read_text().splitlines()))
   # A pre-existing link must never redirect wrapper writes to user files.
   wrapper=base/'termux-jobs-xfce-launch/xfwm4';wrapper.unlink();victim=base/'keep';victim.write_text('preserve');wrapper.symlink_to(victim);log.write_text('')
   bad=subprocess.run(['/bin/sh',str(SESSION)],env=env,capture_output=True,text=True,timeout=5)
   self.assertNotEqual(0,bad.returncode);self.assertIn('ERROR:WINDOW_MANAGER_LINK',bad.stdout);self.assertEqual('preserve',victim.read_text());self.assertNotIn('wm:',log.read_text());wrapper.unlink()

   # Wiring forwards the helper timeout; property/state behavior is tested separately.
   (bin/'xwininfo').write_text('#!/bin/sh\nprintf \'"xfce4-panel"\\n\'\n')
   (bin/'seq').write_text("#!/bin/sh\nprintf '1\\n'\n");(bin/'seq').chmod(0o700)
   (bin/'sleep').write_text('#!/bin/sh\nexec /bin/sleep .01\n');(bin/'sleep').chmod(0o700)
   env['FIXTURE_STARTUP']='TIMEOUT'
   p=subprocess.run(['/bin/sh',str(SESSION)],env=env,capture_output=True,text=True,timeout=5)
   self.assertIn(f'JOBS_X11_DESKTOP_TIMEOUT {token}',p.stdout);self.assertNotIn('JOBS_X11_DESKTOP_READY',p.stdout)

 def test_component_check_verifies_frontend_package_status_without_installing(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   base=Path(d)/'files';prefix=base/'usr';bin=prefix/'bin';bin.mkdir(parents=True);control=base/'workspace-x11';control.mkdir()
   (prefix/'var/lib/proot-distro/containers/jobs-x11-ubuntu/rootfs').mkdir(parents=True)
   scripts={'proot-distro': 'for last;do :;done\nexport JOBS_FIXTURE_PATH="$PATH"\nlast=$(printf \'%s\' "$last" | /usr/bin/sed \'s#export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/local/games:/usr/games;#export PATH="$JOBS_FIXTURE_PATH";#\')\nexec /bin/sh -c "$last"',
    'dpkg-query': '''[ "$3" = '-f=${Status}' ] || { printf 'bad format: %s' "$3" >&2;exit 9; }
printf '%s' "${FIXTURE_PACKAGE_STATUS:-install ok installed}"'''}
   for name in ['thunar','xfce4-terminal','xwininfo','xfwm4','xfdesktop','xfce4-panel','xfsettingsd','xfconf-query','xprop','xfce4-session','dbus-run-session','xauth','fcitx5','dbus-monitor']:scripts[name]='exit 0'
   # dpkg argv: -W, -f=${Status}, package; second argument is the format.
   scripts['dpkg-query']=scripts['dpkg-query'].replace('"$3"','"$2"')
   for name,body in scripts.items():
    p=bin/name;p.write_text('#!/bin/sh\n'+body+'\n');p.chmod(0o700)
   modules=prefix/'lib/pulseaudio/modules';modules.mkdir(parents=True)
   for n in ['module-sles-sink.so','module-native-protocol-unix.so']:(modules/n).touch()
   pulse=bin/'pulseaudio';pulse.write_text('#!/bin/sh\nexit 1\n');pulse.chmod(0o700)
   env={**os.environ,'PREFIX':str(prefix),'PATH':str(bin)+':/usr/bin:/bin'}
   p=subprocess.run(['/bin/bash',str(DESKTOP),'check','jobs-x11-ubuntu',str(control)],env=env,capture_output=True,text=True,timeout=5)
   self.assertEqual(0,p.returncode,p.stderr);self.assertIn('READY:desktop',p.stdout)
   env['FIXTURE_PACKAGE_STATUS']='deinstall ok config-files'
   p=subprocess.run(['/bin/bash',str(DESKTOP),'check','jobs-x11-ubuntu',str(control)],env=env,capture_output=True,text=True,timeout=5)
   self.assertIn('MISSING:xfce',p.stdout)
if __name__=='__main__':unittest.main()
