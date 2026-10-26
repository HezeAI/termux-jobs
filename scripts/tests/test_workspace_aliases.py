import os, pathlib, subprocess, tempfile, unittest
ROOT=pathlib.Path(__file__).resolve().parents[2]
HELPER=ROOT/'app/src/main/assets/workspace-aliases.sh'
ID='00000000-0000-4000-8000-000000000123'
OTHER='00000000-0000-4000-8000-000000000124'
class AliasInstall(unittest.TestCase):
 def setUp(self):
  self.tmp=tempfile.TemporaryDirectory(dir=ROOT/'.verification/aliases'); self.home=pathlib.Path(self.tmp.name);self.env={**os.environ,'HOME':str(self.home)}
 def tearDown(self):self.tmp.cleanup()
 def run_helper(self,action='save',ident=ID,name='ll',command="printf '%s\\n' 'a b'",replace='',ok=True):
  p=subprocess.run(['/bin/sh',str(HELPER),action,ident,name,command,replace],env=self.env,text=True,capture_output=True)
  if ok:self.assertEqual(0,p.returncode,p.stdout+p.stderr);self.assertEqual('ALIAS_OK\n',p.stdout)
  else:self.assertNotEqual(0,p.returncode)
  return p
 def test_save_does_not_execute_and_new_shell_loads_quoted_multiline_body(self):
  command="printf '%s\\n' 'quoted a b'\nprintf 'second'"
  self.run_helper(command=command)
  rc=self.home/'.bashrc';before=rc.read_bytes();self.run_helper(command=command);self.assertEqual(before,rc.read_bytes());self.assertEqual(1,before.count(b'# Termux Jobs aliases v1'))
  p=subprocess.run(['/bin/bash','--noprofile','--rcfile',str(rc),'-ic','ll'],env=self.env,text=True,capture_output=True)
  self.assertEqual(0,p.returncode,p.stderr);self.assertEqual('quoted a b\nsecond',p.stdout)
  marker=self.home/'not-executed';self.run_helper(command='touch '+str(marker));self.assertFalse(marker.exists())
 def test_startup_does_not_evaluate_substitutions_or_cleanup_alias(self):
  marker=self.home/'not-created'
  self.run_helper(name='unset',command='touch '+str(marker))
  self.run_helper(ident=OTHER,name='danger',command="printf '%s' \"$(touch "+str(marker)+")\"")
  p=subprocess.run(['/bin/bash','--noprofile','--rcfile',str(self.home/'.bashrc'),'-ic',':'],env=self.env,text=True,capture_output=True)
  self.assertEqual(0,p.returncode,p.stderr);self.assertFalse(marker.exists())
 def test_empty_collection_and_removed_managed_directory_are_safe_at_startup(self):
  self.run_helper();self.run_helper(action='delete')
  directory=self.home/'.termux-jobs-aliases';self.assertTrue((directory/'_loader.sh').is_file())
  for removed in (False,True):
   if removed:
    import shutil
    shutil.rmtree(directory)
   for shell in ('/bin/bash','/bin/sh'):
    p=subprocess.run([shell,'-c','. "$HOME/.profile"'],env=self.env,text=True,capture_output=True)
    self.assertEqual(0,p.returncode,p.stderr);self.assertEqual('',p.stdout+p.stderr)
 def test_rename_delete_and_other_environment_isolation(self):
  self.run_helper();self.run_helper(name='newname',command='pwd');self.assertNotIn('ll=',(self.home/'.termux-jobs-aliases'/f'{ID}.sh').read_text())
  self.run_helper(ident=OTHER,name='keep',command='pwd');self.run_helper(action='delete');self.assertFalse((self.home/'.termux-jobs-aliases'/f'{ID}.sh').exists());self.assertTrue((self.home/'.termux-jobs-aliases'/f'{OTHER}.sh').exists())
  self.run_helper(action='delete')
 def test_symlinks_and_unmanaged_targets_and_existing_profile_are_preserved(self):
  outside=self.home/'original';outside.write_text('personal data\n');(self.home/'.bashrc').symlink_to(outside)
  self.assertIn('ALIAS_RC_SYMLINK',self.run_helper(ok=False).stdout);self.assertEqual('personal data\n',outside.read_text());self.assertFalse((self.home/'.zshrc').exists())
  (self.home/'.bashrc').unlink();(self.home/'.profile').write_text('export USER_SETTING=preserved\n');self.run_helper();self.assertIn('USER_SETTING=preserved',(self.home/'.profile').read_text());self.assertFalse((self.home/'.bash_profile').exists())
  dest=self.home/'.termux-jobs-aliases'/f'{ID}.sh';dest.write_text('unmanaged\n');self.run_helper(action='delete',ok=False);self.assertEqual('unmanaged\n',dest.read_text())
 def test_alias_file_symlink_and_directory_symlink_do_not_escape(self):
  outside=self.home/'original';outside.write_text('keep');directory=self.home/'.termux-jobs-aliases';directory.mkdir();target=directory/f'{ID}.sh';target.symlink_to(outside);self.run_helper(ok=False);self.run_helper(action='delete',ok=False);self.assertEqual('keep',outside.read_text());target.unlink();directory.rmdir();directory.symlink_to(self.home);self.run_helper(ok=False)
 def test_busy_lock_and_invalid_names_do_not_commit(self):
  self.run_helper();directory=self.home/'.termux-jobs-aliases';(directory/'.lock').mkdir();self.assertIn('ALIAS_BUSY',self.run_helper(ok=False).stdout);(directory/'.lock').rmdir();before=(directory/f'{ID}.sh').read_bytes();self.run_helper(name='bad;touch',ok=False);self.assertEqual(before,(directory/f'{ID}.sh').read_bytes());self.assertFalse((directory/'.lock').exists())
 def test_posix_shell_and_zsh_hook_and_bash_profile(self):
  (self.home/'.bash_profile').write_text('export KEEP=1\n');self.run_helper();self.assertIn('Termux Jobs aliases',(self.home/'.bash_profile').read_text());self.assertIn('Termux Jobs aliases',(self.home/'.zshrc').read_text())
  p=subprocess.run(['/bin/sh','-c','. "$HOME/.profile"\neval ll'],env=self.env,text=True,capture_output=True);self.assertEqual(0,p.returncode,p.stderr);self.assertEqual('a b\n',p.stdout)
if __name__=='__main__':unittest.main()
