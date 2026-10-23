"""Owner-access repair touches only Arch root after installation consent."""
from pathlib import Path
import os,stat,subprocess,tempfile,unittest
ROOT=Path(__file__).resolve().parents[2];ASSET=ROOT/'x11/src/main/assets/x11-desktop.sh'
CODE=ASSET.read_text().split("<<'ROOT_PERMISSIONS'\n",1)[1].split('\nROOT_PERMISSIONS',1)[0]
def mode(path):
 fd=os.open(path,os.O_RDONLY|os.O_DIRECTORY)
 try:return stat.S_IMODE(os.fstat(fd).st_mode)
 finally:os.close(fd)
def chmod(path,value):
 fd=os.open(path,os.O_RDONLY|os.O_DIRECTORY)
 try:os.fchmod(fd,value)
 finally:os.close(fd)
class ArchRootTests(unittest.TestCase):
 def run_helper(self,base,operation):return subprocess.run(['/usr/bin/python3','-B','-c',CODE,str(base),operation],text=True,capture_output=True,timeout=4)
 def test_check_is_non_mutating_and_repair_preserves_children_and_other_bits(self):
  for layout in ['containers/archlinux/rootfs','installed-rootfs/archlinux']:
   with self.subTest(layout=layout),tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
    base=Path(d);root=base/layout;root.mkdir(parents=True);document=root/'private.txt';document.write_text('private fixture');document.chmod(0o600);other=base/'containers/ubuntu/rootfs';other.mkdir(parents=True);chmod(other,0o550);chmod(root,0o555)
    r=self.run_helper(base,'check');self.assertEqual(1,r.returncode,r.stderr);self.assertEqual(0o555,mode(root));self.assertNotIn('restored',r.stdout)
    r=self.run_helper(base,'repair');self.assertEqual(0,r.returncode,r.stderr);self.assertIn('owner-access-restored',r.stdout);self.assertEqual(0o755,mode(root));self.assertEqual(0o600,stat.S_IMODE(document.stat().st_mode));self.assertEqual('private fixture',document.read_text());self.assertEqual(0o550,mode(other))
    r=self.run_helper(base,'repair');self.assertEqual(0,r.returncode);self.assertNotIn('restored',r.stdout);self.assertEqual(0,self.run_helper(base,'check').returncode)
 def test_root_or_container_symlink_refused_without_touching_target(self):
  for link in ['containers/archlinux','containers/archlinux/rootfs']:
   with self.subTest(link=link),tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
    base=Path(d);outside=base/'outside';outside.mkdir();chmod(outside,0o555);p=base/link;p.parent.mkdir(parents=True);p.symlink_to(outside,target_is_directory=True)
    r=self.run_helper(base,'repair');self.assertEqual(1,r.returncode);self.assertIn('ERROR:ARCH_ROOTFS_OWNER_ACCESS',r.stdout);self.assertEqual(0o555,mode(outside))
 def test_missing_wrong_owner_and_unknown_operation_do_not_repair(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   base=Path(d);self.assertEqual(1,self.run_helper(base,'repair').returncode);root=base/'containers/archlinux/rootfs';root.mkdir(parents=True);chmod(root,0o555)
   probe=CODE.replace('base,operation=sys.argv[1:]','base,operation=sys.argv[1:]\nos.getuid=lambda: 999999')
   r=subprocess.run(['/usr/bin/python3','-B','-c',probe,str(base),'repair'],capture_output=True,timeout=4);self.assertEqual(1,r.returncode);self.assertEqual(0o555,mode(root));self.assertEqual(2,self.run_helper(base,'unknown').returncode)
 def test_shell_gates_repair_to_selected_arch_and_to_consented_install(self):
  text=ASSET.read_text();self.assertIn('[[ $target == archlinux ]] || return 0',text)
  check=text.split('if [[ $op == check ]]; then',1)[1].split('if [[ $op == install ]]; then',1)[0];self.assertIn('arch_root_permissions check',check);self.assertNotIn('arch_root_permissions repair',check)
  run=text.split('gpu_mode=${JOBS_X11_RENDERER:-cpu}',1)[1];self.assertNotIn('arch_root_permissions repair',run)
 def test_missing_native_python_fails_check_but_other_targets_do_not_need_it(self):
  text=ASSET.read_text();function=text.split('arch_root_permissions() {',1)[1].split('\ngpu_bind=()',1)[0]
  script='arch_root_permissions() {'+function+'\narch_root_permissions check\n'
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   for target in ['archlinux','termux','ubuntu','debian','alpine','fedora','opensuse','void']:
    with self.subTest(target=target):
     r=subprocess.run(['/bin/bash','-c','target="$TARGET"\n'+script],env=dict(os.environ,PREFIX=d,TARGET=target),text=True,capture_output=True,timeout=4)
     self.assertEqual(1 if target=='archlinux' else 0,r.returncode,r.stderr);self.assertEqual(target=='archlinux','MISSING:python3' in r.stdout)
