"""Clean bootstrap ABI and selected-rootfs contracts; never installs packages."""
import contextlib,importlib.util,io,json,os,re,subprocess,tempfile,unittest,zipfile
from pathlib import Path
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[2];ASSETS=ROOT/'x11/src/main/assets'
s=importlib.util.spec_from_file_location('native_scoped',ASSETS/'x11-native-programs.py');m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
class ScopedTests(unittest.TestCase):
 def test_clean_bootstrap_old_cpp_is_explicitly_updated_by_native_desktop_install(self):
  archive=ROOT/'.tools/downloads/bootstrap-aarch64.zip'
  if not archive.exists():self.skipTest('local pinned bootstrap required')
  with zipfile.ZipFile(archive) as z:
   status=z.read('var/lib/dpkg/status').decode();cpp=next(b for b in status.split('\n\n') if b.startswith('Package: libc++\n'))
   self.assertIn('Version: 27c',cpp)
  script=(ASSETS/'x11-native-desktop.sh').read_text();native=script.split(' termux)\n',1)[1].split(';;',1)[0];self.assertIn(' libc++ ',native);self.assertNotIn('gnupg',native);self.assertIn('--no-install-recommends $pkgs',script)
 def test_host_only_command_is_not_selected_in_guest_desktop(self):
  text=(ASSETS/'x11-desktop.sh').read_text();setup=re.search("guest_environment='([^']*)'",text).group(1)
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   tool=Path(d)/'termux_only_fixture_tool';tool.write_text('#!/bin/sh\nexit 0\n');tool.chmod(0o700)
   result=subprocess.run(['/bin/sh','-c',setup+'command -v termux_only_fixture_tool'],env=dict(os.environ,PATH=d,LD_LIBRARY_PATH='/host-only',LD_PRELOAD=''),capture_output=True,text=True,timeout=3)
   self.assertNotEqual(0,result.returncode);self.assertEqual('',result.stdout)
   result=subprocess.run(['/bin/sh','-c',setup+'printf "%s|%s|%s" "$XDG_DATA_DIRS" "$XDG_CONFIG_DIRS" "${LD_LIBRARY_PATH-unset}"'],env=dict(os.environ,PATH=d,LD_LIBRARY_PATH='/host-only'),capture_output=True,text=True,timeout=3)
   self.assertEqual('/usr/local/share:/usr/share|/etc/xdg|unset',result.stdout)
 def test_all_guests_receive_same_private_environment_but_keep_their_rootfs(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   base=Path(d);prefix=base/'files/usr';bin=prefix/'bin';bin.mkdir(parents=True);control=prefix.parent/'workspace-x11';control.mkdir();log=base/'calls'; (bin/'python3').symlink_to('/usr/bin/python3')
   tool=bin/'proot-distro';tool.write_text('#!/usr/bin/python3\nimport json,os,sys\nwith open(os.environ["CAPTURE"],"a") as f:f.write(json.dumps(sys.argv[1:])+"\\n")\n');tool.chmod(0o700)
   for target in ['ubuntu','jobs-x11-ubuntu','debian','alpine','fedora','opensuse','void','archlinux']:
    (prefix/f'var/lib/proot-distro/containers/{target}/rootfs').mkdir(parents=True)
    env=dict(os.environ,PREFIX=str(prefix),PATH=str(bin)+':/usr/bin:/bin',CAPTURE=str(log))
    result=subprocess.run(['bash',str(ASSETS/'x11-desktop.sh'),'check',target,str(control)],env=env,capture_output=True,text=True,timeout=3);self.assertEqual(0,result.returncode,result.stderr)
    args=json.loads(log.read_text().splitlines()[-1]);self.assertEqual(target,args[args.index('--')-1]);self.assertTrue(args[-1].startswith('export PATH=/usr/local/sbin:'));self.assertIn('unset LD_LIBRARY_PATH LD_PRELOAD DBUS_SYSTEM_BUS_ADDRESS;',args[-1]);self.assertEqual('run' if target=='void' else 'login',args[0])
 def test_additional_runtime_fixes_are_owned_by_the_application_group(self):
  self.assertEqual(['ristretto','tumbler','poppler'],m.packages('termux','images'));self.assertEqual(m.packages('termux','images'),m.packages('termux','pkg:ristretto'))
  self.assertEqual(['gnupg','libgpg-error','libgcrypt'],m.packages('termux','pkg:gnupg'));self.assertEqual(['fcitx5','libc++'],m.packages('termux','pkg:fcitx5'))
  self.assertEqual(['gnupg'],m.packages('void','pkg:gnupg'))
  for target in m.GROUPS:
   self.assertNotIn('poppler',m.packages(target,'editor'));self.assertNotIn('libc++',m.packages(target,'media'))
  self.assertIn('evince-plugin-pdfdocument',m.packages('opensuse','pdf'));self.assertNotIn('cleanup-shortcuts',(ASSETS/'x11-desktop.sh').read_text())
 def test_native_program_install_updates_cpp_only_with_explicit_program_transaction(self):
  for identifier in ['images','office','pkg:gnupg']:
   have=set();calls=[]
   def run(args):
    calls.append(args)
    if 'install' in args:have.update(args[args.index('--no-remove')+1:])
   with patch.dict(os.environ,{'JOBS_X11_NATIVE_TARGET':'termux','PREFIX':'/data/data/com.termux/files/usr'}),patch.object(m.Path,'home',return_value=Path('/data/data/com.termux/files/home')),patch.object(m.sys,'argv',['helper','install',identifier]),patch.object(m,'installed',side_effect=lambda _:have),patch.object(m,'run',side_effect=run),patch.object(m,'load') as loader,contextlib.redirect_stdout(io.StringIO()):
    loader.return_value.state.return_value='off';m.main();transaction=next(a for a in calls if 'install' in a);self.assertIn('libc++',transaction);self.assertEqual(identifier=='images','poppler' in transaction);self.assertEqual(identifier=='pkg:gnupg','libgcrypt' in transaction)
 def test_no_firefox_install_means_no_firefox_dns_profile_changes(self):
  for target in m.GROUPS:
   with patch.dict(os.environ,{'JOBS_X11_NATIVE_TARGET':target,'PREFIX':'/data/data/com.termux/files/usr'}),patch.object(m.Path,'home',return_value=Path('/data/data/com.termux/files/home')),patch.object(m.sys,'argv',['helper','firefox-dns-on']),patch.object(m,'installed',return_value=set()),patch.object(m,'load') as loader:
    with self.assertRaisesRegex(ValueError,'Firefox is not installed'):m.main()
    loader.assert_not_called()
 def test_failed_automatic_input_does_not_block_manual_desktop_or_install_packages(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   base=Path(d);bin=base/'bin';bin.mkdir()
   tools=['dpkg-query','python3','thunar','xfce4-terminal','xwininfo','xprop','xfwm4','xfdesktop','xfce4-panel','xfsettingsd','xfconf-query','xfce4-session','dbus-run-session','dbus-monitor','fcitx5','xauth','exo-open']
   for name in tools:
    body='exit 0'
    if name=='dpkg-query':body='printf "install ok installed"'
    if name=='fcitx5':body='echo "CANNOT LINK EXECUTABLE fcitx5: cannot locate symbol __hash_memory" >&2;exit 1'
    p=bin/name;p.write_text('#!/bin/sh\n'+body+'\n');p.chmod(0o700)
   result=subprocess.run(['/bin/sh',str(ASSETS/'x11-native-desktop.sh'),'check','termux',str(base)],env=dict(os.environ,PREFIX=str(base),PATH=str(bin)+':/usr/bin:/bin'),capture_output=True,text=True,timeout=3)
   self.assertEqual(0,result.returncode,result.stderr);self.assertIn('WARNING:X11_INPUT_COMPONENT_UNAVAILABLE',result.stdout)

 def test_required_desktop_is_explicit_and_missing_background_binary_fails_check(self):
  script=(ASSETS/'x11-native-desktop.sh').read_text()
  for target in ['termux','alpine','fedora','opensuse','void','archlinux']:
   section=script.split(' '+target+')\n',1)[1].split(';;',1)[0]
   self.assertIn('xfdesktop',section);self.assertIn('xfce4-panel',section);self.assertIn('xfce4-session',section);self.assertIn('xfce4-settings',section)
  apt=(ASSETS/'x11-desktop.sh').read_text();self.assertIn("packages='xfce4 xfdesktop4 xfce4-panel xfce4-session xfce4-settings xfconf thunar",apt)
  check=script.split('check() {',1)[1].split('\ncase $op in',1)[0]
  for missing in ['xfdesktop','xfce4-panel','xfsettingsd','xfconf-query']:
   # Only the requested command is missing. No package manager or user service runs.
   fixture='target=termux\ndpkg-query() { echo "install ok installed"; }\ncommand() { [ "$2" != "'+missing+'" ]; }\nfcitx5() { return 0; }\ncheck() {'+check+'\ncheck\n'
   result=subprocess.run(['/bin/sh','-c',fixture],capture_output=True,text=True,timeout=3)
   self.assertNotEqual(0,result.returncode,missing)
