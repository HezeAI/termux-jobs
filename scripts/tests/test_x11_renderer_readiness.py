"""Execute the production check function without touching any installed environment."""
import os, subprocess, tempfile, unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
TARGETS=('termux','ubuntu','debian','alpine','fedora','opensuse','void','archlinux')
class RendererReadinessTests(unittest.TestCase):
 def test_full_check_is_selected_target_scoped_and_never_installs(self):
  source=(ROOT/'x11/src/main/assets/x11-desktop.sh').read_text()
  function=source[source.index('check_renderer() {'):source.index('if [[ $op == check ]]; then')]
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as temporary:
   root=Path(temporary);prefix=root/'files/usr';(prefix/'bin').mkdir(parents=True);control=root/'control';control.mkdir();capture=root/'args'
   stub='#!/bin/sh\nprintf "%s\\n" "$*" >> "$CAPTURE"\nexit "$FIXTURE_EXIT"\n'
   (prefix/'bin/python3').write_text(stub);(prefix/'bin/python3').chmod(0o700)
   (control/'x11-virgl-setup.sh').write_text(stub)
   for target in TARGETS:
    for mode in ('turnip','virgl'):
     for status in (0,1):
      with self.subTest(target=target,renderer=mode,status=status):
       capture.unlink(missing_ok=True)
       env=dict(os.environ,PREFIX=str(prefix),JOBS_X11_RENDERER=mode,CAPTURE=str(capture),FIXTURE_EXIT=str(status))
       r=subprocess.run(['bash','-c',function+'\ntarget=$1;control=$2;check_renderer','fixture',target,str(control)],env=env,text=True,capture_output=True,timeout=3)
       self.assertEqual(0,r.returncode,r.stderr)
       self.assertIn(('READY' if status==0 else 'MISSING')+':renderer:'+mode,r.stdout)
       args=capture.read_text();self.assertIn('check '+target,args);self.assertNotIn('install',args)
 def test_launch_still_performs_its_own_fail_closed_validation(self):
  source=(ROOT/'x11/src/main/assets/x11-desktop.sh').read_text()
  run=source[source.index('gpu_mode=${JOBS_X11_RENDERER:-cpu}'):]
  self.assertIn('"$control/x11-gpu.py" check "$target" "$gpu_root" || exit',run)
  self.assertIn('"$control/x11-virgl-setup.sh" check "$target" "$control" || exit',run)
