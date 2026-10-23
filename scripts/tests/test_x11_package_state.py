"""Selection changes after failed removal must not hide configured X11 packages."""
import importlib.util, subprocess, tempfile, unittest
from pathlib import Path
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[2]
ASSETS=ROOT/'x11/src/main/assets'
class PackageStateTests(unittest.TestCase):
 def test_guest_base_check_distinguishes_selection_error_and_actual_state(self):
  # Execute exactly the shell function embedded in the production guest command.
  components=(ASSETS/'x11-desktop.sh').read_text().split("components='",1)[1].split("'\npackages=",1)[0]
  function=components.split('; configured libpulse0',1)[0]
  for status,ready in [('install ok installed',True),('deinstall ok installed',True),('hold ok installed',True),('purge ok installed',True),('install reinstreq installed',False),('deinstall ok config-files',False),('install ok half-configured',False),('',False)]:
   with self.subTest(status=status),tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
    stub=Path(d)/'dpkg-query';stub.write_text('#!/bin/sh\nprintf "%s" "$STATUS_FIXTURE"\n');stub.chmod(0o755)
    proc=subprocess.run(['/bin/sh','-c',function+'; configured fixture'],env={'STATUS_FIXTURE':status,'PATH':d},capture_output=True,text=True)
    self.assertEqual(ready,proc.returncode==0,proc.stderr)
 def test_firefox_keeps_binary_check_and_rejects_real_package_errors(self):
  spec=importlib.util.spec_from_file_location('firefox_state',ASSETS/'x11-firefox.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   root=Path(d);binary=root/'usr/lib/firefox/firefox';binary.parent.mkdir(parents=True);binary.write_bytes(b'\x7fELFfixture');binary.chmod(0o755)
   for status,ready in [('deinstall ok installed',True),('hold ok installed',True),('install reinstreq installed',False),('install ok unpacked',False)]:
    with self.subTest(status=status),patch.object(m,'output',return_value=status):self.assertEqual(ready,m.ready(root))
