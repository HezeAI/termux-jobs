from pathlib import Path
import contextlib,importlib.util,io,os,tempfile,unittest
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[2]
s=importlib.util.spec_from_file_location('resume_native',ROOT/'x11/src/main/assets/x11-native-programs.py');m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
class ResumeTests(unittest.TestCase):
 def test_all_native_resumes_skip_index_refresh_keep_dependency_checks_and_verify_result(self):
  for target in ['alpine','fedora','opensuse','termux','void','archlinux']:
   with self.subTest(target=target):
    have=set();calls=[]
    def run(args):
     calls.append(args)
     if 'install' in args or 'add' in args or args[:2]==['xbps-install','-y'] or args[:2]==['pacman','-Su']:have.update(m.packages(target,'editor')+(['libc++'] if target=='termux' else []))
    env={'JOBS_X11_NATIVE_TARGET':target,'JOBS_X11_PROGRAM_RESUME':'1','PREFIX':'/data/data/com.termux/files/usr'}
    with patch.dict(os.environ,env),patch.object(m.Path,'home',return_value=Path('/data/data/com.termux/files/home') if target=='termux' else Path('/root')),patch.object(m.sys,'argv',['helper','install','editor']),patch.object(m,'installed',side_effect=lambda _:have),patch.object(m,'run',side_effect=run),patch.object(m,'load') as load,contextlib.redirect_stdout(io.StringIO()) as log:
     load.return_value.state.return_value='off';m.main()
    text=str(calls)
    for denied in ['makecache','update','refresh','--cacheonly','--force','--no-gpg-checks','--allow-unauthenticated']:self.assertNotIn("'"+denied+"'",text)
    self.assertIn('JOBS_PROGRAM_INDEX_READY',log.getvalue());self.assertIn('PROGRAM:editor:installed',log.getvalue());self.assertIn('mousepad',text)
    if target=='fedora':self.assertIn('--setopt=keepcache=True',text);self.assertIn('--setopt=metadata_expire=-1',text)
    if target=='alpine':self.assertIn('/var/cache/apk',text)
 def test_failed_install_cannot_report_ready_and_fresh_install_refreshes(self):
  with patch.dict(os.environ,{'JOBS_X11_NATIVE_TARGET':'void','JOBS_X11_PROGRAM_RESUME':'0'}),patch.object(m.sys,'argv',['helper','install','editor']),patch.object(m,'run') as run,patch.object(m,'installed',return_value=set()),contextlib.redirect_stdout(io.StringIO()) as log:
   with self.assertRaisesRegex(ValueError,'not fully installed'):m.main()
   self.assertEqual(['xbps-install','-S'],run.call_args_list[0].args[0]);self.assertNotIn('READY:programs',log.getvalue())
 def test_zypper_resume_retains_packages_in_temporary_repo_view_only(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   directory=Path(d);repo=directory/'oss.repo';original='[oss]\nbaseurl=https://download.opensuse.org/distribution/leap/16.0/repo/oss/\ngpgcheck=1\n';repo.write_text(original)
   with m.zypper_repositories(directory,keep=True) as args:
    ini=m.configparser.ConfigParser(interpolation=None);ini.read(Path(args[1])/'oss.repo');self.assertEqual('1',ini['oss']['keeppackages']);self.assertEqual('1',ini['oss']['gpgcheck']);self.assertEqual(original,repo.read_text())
   self.assertFalse(Path(args[1]).exists());self.assertEqual(original,repo.read_text())
