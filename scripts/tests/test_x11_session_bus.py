import importlib.util,os,tempfile,unittest,xml.etree.ElementTree as ET
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
s=importlib.util.spec_from_file_location('bus',ROOT/'x11/src/main/assets/x11-session-bus.py');bus=importlib.util.module_from_spec(s);s.loader.exec_module(bus)
class SessionBus(unittest.TestCase):
 def test_each_proot_target_keeps_upstream_auth_and_only_allows_actual_app_uid(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as tmp:
   base=Path(tmp);source=base/'distribution.conf';original=b'<busconfig><auth>EXTERNAL</auth></busconfig>';source.write_bytes(original)
   for target in bus.TARGETS:
    control=base/target;control.mkdir();path=bus.prepare(control,target,10123,'11111111-1111-4111-8111-111111111111',source);data=path.read_bytes();root=ET.fromstring(data)
    self.assertEqual(str(source),root.findtext('include'));self.assertEqual([{'user':'10123'}],[n.attrib for n in root.findall('policy/allow')]);self.assertNotIn(b'ANONYMOUS',data);self.assertNotIn(b'tcp:',data);self.assertEqual(0o600,path.stat().st_mode&0o777)
    self.assertEqual(data,bus.prepare(control,target,10123,'22222222-2222-4222-8222-222222222222',source).read_bytes());self.assertEqual(['desktop-bus.conf'],[p.name for p in control.iterdir()])
   self.assertEqual(original,source.read_bytes())
 def test_invalid_identity_missing_config_and_links_do_not_write(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as tmp:
   base=Path(tmp);source=base/'source';source.write_text('<busconfig/>');control=base/'control';control.mkdir();token='11111111-1111-4111-8111-111111111111'
   for target,uid,tok in [('termux',10123,token),('ubuntu-ai',10123,token),('archlinux',0,token),('archlinux',True,token),('archlinux',2**31,token),('archlinux',10123,'bad')]:
    with self.assertRaises(ValueError):bus.prepare(control,target,uid,tok,source)
   victim=base/'victim';victim.write_text('keep');(control/'desktop-bus.conf').symlink_to(victim)
   with self.assertRaises(ValueError):bus.prepare(control,'archlinux',10123,token,source)
   self.assertEqual('keep',victim.read_text());(control/'desktop-bus.conf').unlink()
   link=base/'link';link.symlink_to(control,target_is_directory=True)
   with self.assertRaises(ValueError):bus.prepare(link,'archlinux',10123,token,source)
   with self.assertRaises(ValueError):bus.prepare(control,'archlinux',10123,token,base/'absent')
 def test_native_termux_start_is_unchanged_guest_start_has_private_config(self):
  source=(ROOT/'x11/src/main/assets/x11-desktop.sh').read_text()
  self.assertIn('jobs_desktop_run dbus-run-session -- sh "$control/x11-session.sh"',source)
  self.assertIn('jobs_app_uid=$(id -u)',source);self.assertIn('JOBS_X11_APP_UID=$jobs_app_uid',source);self.assertIn('dbus-run-session --config-file="$jobs_bus_config"',source)
  self.assertIn('x11-session-bus.py',(ROOT/'x11/src/main/java/com/termux/x11/DesktopProcess.java').read_text())
if __name__=='__main__':unittest.main()
