"""Isolated RTK state, cancellation, installer and filtering checks. No real auth."""
import contextlib,hashlib,importlib.util,io,json,os,sys,tempfile,unittest,uuid
from pathlib import Path
from unittest.mock import patch
REPO=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('rtk_mode',REPO/'app/src/main/assets/rtk-mode.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
class StateTests(unittest.TestCase):
 def setUp(self):
  self.tmp=tempfile.TemporaryDirectory(dir=REPO/'.verification/rtk-mode');self.old=m.BASE;m.BASE=Path(self.tmp.name)/'component-state';self.profile=str(uuid.uuid4());self.run=str(uuid.uuid4());self.attempt=str(uuid.uuid4())
 def tearDown(self):m.BASE=self.old;self.tmp.cleanup()
 def query(self,op,**extra):
  q=dict(profile=self.profile,run=self.run,attempt=self.attempt,op=op,**extra)
  with contextlib.redirect_stdout(io.StringIO())as out:m.main(q)
  return json.loads(out.getvalue().splitlines()[-1])
 def test_deferred_advanced_requires_consent_and_no_ready_process(self):
  self.query('begin')
  with patch.object(m,'verify',return_value=True):
   self.assertEqual(self.query('enable',deferred=True,mode='advanced')['error'],'CONFIRMATION_REQUIRED')
   r=self.query('enable',deferred=True,mode='advanced',confirmed=True)['result']
   self.assertTrue(r['desired']);self.assertFalse(r['enabled']);self.assertEqual(r['mode'],'advanced')
   self.query('disable');self.assertFalse(m.status(self.profile,self.run)['desired'])
 def test_cancel_mode_change_restores_previous_selection(self):
  m.write(m.chat(self.profile)/'mode.json',{'enabled':True,'mode':'advanced'});self.query('begin')
  with patch.object(m,'verify',return_value=True):self.query('enable',deferred=True,mode='basic')
  self.query('cancel');self.assertEqual(m.status(self.profile,self.run)['mode'],'advanced')
 def test_native_recovery_and_tracking_are_chat_scoped(self):
  other=str(uuid.uuid4());a=m.native_env(self.profile);b=m.native_env(other)
  for key in ['RTK_DB_PATH','RTK_RECALL_DB','XDG_CONFIG_HOME']:self.assertNotEqual(a[key],b[key])
  self.assertNotIn('RTK_RECALL',a);self.assertNotIn('RTK_TEE',a)
 def test_live_native_requires_new_dispatch_environment(self):
  self.query('begin');m.mark_ready(self.profile,self.run,os.getpid())
  with patch.object(m,'verify',return_value=True):self.assertEqual(self.query('enable',mode='advanced',confirmed=True)['error'],'RESTART_REQUIRED')
 def test_cancel_prevents_late_enable(self):
  self.query('begin');m.mark_ready(self.profile,self.run,os.getpid());self.query('cancel')
  with patch.object(m,'verify',return_value=True):r=self.query('enable')
  self.assertEqual(r['error'],'CANCELLED');self.assertFalse(m.read(m.chat(self.profile)/'mode.json')['enabled'])
 def test_new_attempt_is_not_cancelled_by_previous_window(self):
  self.query('begin');old=self.attempt;self.attempt=str(uuid.uuid4());self.query('begin');new=self.attempt;self.attempt=old;self.query('cancel');self.attempt=new
  m.cancelled(dict(profile=self.profile,attempt=new))
 def test_ready_is_run_and_process_identity_bound(self):
  m.mark_ready(self.profile,self.run,os.getpid());self.assertTrue(m.ready(self.profile,self.run));self.assertFalse(m.ready(self.profile,str(uuid.uuid4())))
  p=m.chat(self.profile)/'ready.json';r=m.read(p);r['start']='invalid';m.write(p,r);self.assertFalse(m.ready(self.profile,self.run))
 def test_chats_are_independent(self):
  self.query('begin');m.mark_ready(self.profile,self.run,os.getpid())
  with patch.object(m,'verify',return_value=True):self.assertTrue(self.query('enable')['result']['enabled'])
  other=str(uuid.uuid4());self.assertFalse(m.status(other,self.run)['desired']);self.query('disable');self.assertFalse(m.status(self.profile,self.run)['desired'])
 def test_only_supported_literal_commands(self):
  for command in ['git push','git status; touch x','git -c alias.status=x status','git status > x','git status $(id)','echo git status','git diff | cat','git log && id']:
   self.assertIsNone(m.filter_for(command),command)
  self.assertEqual(m.filter_for('git diff --staged'),'git-diff')
 def test_hook_preserves_original_and_other_fields(self):
  m.write(m.chat(self.profile)/'mode.json',{'enabled':True});q={'tool_name':'Bash','tool_input':{'command':'git diff','timeout':123}}
  r=m.hook(q,self.profile,self.run,'claude')['hookSpecificOutput'];self.assertNotIn('permissionDecision',r);self.assertEqual(r['updatedInput']['timeout'],123);self.assertIn('git diff |',r['updatedInput']['command']);self.assertNotIn('2>&1',r['updatedInput']['command'])
 def test_disabled_hook_does_not_rewrite(self):self.assertEqual(m.hook({'tool_name':'Bash','tool_input':{'command':'git diff'}},self.profile,self.run,'claude'),{})
 def test_symlink_state_rejected(self):
  root=m.chat(self.profile);(root/'mode.json').symlink_to(Path(self.tmp.name)/'outside');self.assertRaises(ValueError,m.read,root/'mode.json')
 def test_stats_are_measured_bytes_per_chat(self):
  d=m.directory(m.chat(self.profile)/'measurements');m.write(d/'a.json',dict(input=1000,output=500));m.write(d/'b.json',dict(input=20,output=20));s=m.status(self.profile,self.run)['statistics'];self.assertEqual(s['legacy']['basic'],dict(input=1020,output=520,commands=2));self.assertEqual(s['basic']['commands'],0);self.assertEqual(m.status(str(uuid.uuid4()),self.run)['statistics']['basic']['inputBytes'],0)
 def test_corrupt_binary_never_executes(self):
  d=m.directory(m.component());(d/'rtk').write_bytes(b'bad')
  with patch.object(m.subprocess,'run',side_effect=AssertionError('must not execute')):self.assertFalse(m.verify(d))
class InstallerTests(StateTests):
 @classmethod
 def setUpClass(cls):
  cls.archive=Path(os.environ.get('RTK_TEST_ARCHIVE',str(REPO/'.verification/rtk-remote/download/rtk-oss-1.1.0-jobs.1-aarch64-unknown-linux-gnu.tar.gz'))).read_bytes()
 def response(self,data):
  r=io.BytesIO(data);r.geturl=lambda:m.PIN['url'];return r
 def install(self,data):
  self.query('begin')
  with patch.object(m.urllib.request,'build_opener')as opener:
   opener.return_value.open.return_value=self.response(data)
   return self.query('install',_source=(REPO/'app/src/main/assets/rtk-mode.py').read_text())
 def test_verified_install_and_reuse(self):
  r=self.install(self.archive);self.assertTrue(r['ok'],r);self.assertTrue(r['result']['installed']);self.assertTrue((m.component()/'LICENSE').is_file())
  with patch.object(m.urllib.request,'build_opener',side_effect=AssertionError('must reuse')):
   self.assertTrue(self.query('install',_source='')['ok'])
 def test_corrupt_archive_rejected(self):
  r=self.install(self.archive[:-1]+bytes([self.archive[-1]^1]));self.assertEqual(r['error'],'CHECKSUM_MISMATCH');self.assertFalse(m.component().exists())
 def test_short_download_rejected(self):self.assertEqual(self.install(b'short')['error'],'CHECKSUM_MISMATCH')
 def test_cancel_during_download(self):
  self.query('begin');response=self.response(self.archive);original=response.read
  def read(n):
   b=original(n);m.write(m.chat(self.profile)/'attempt.json',{'id':self.attempt,'cancelled':True});return b
  response.read=read
  with patch.object(m.urllib.request,'build_opener')as opener:
   opener.return_value.open.return_value=response;r=self.query('install',_source='')
  self.assertEqual(r['error'],'CANCELLED');self.assertFalse(m.component().exists())
 def test_network_error(self):
  self.query('begin')
  with patch.object(m.urllib.request,'build_opener',side_effect=OSError('synthetic unavailable')):r=self.query('install',_source='')
  self.assertEqual(r['error'],'NETWORK_ERROR')
 def test_insecure_redirect_is_rejected_before_request(self):
  self.assertRaises(ValueError,m.SecureRedirect().redirect_request,None,None,302,'',{},'http://example.invalid/archive')
if __name__=='__main__':unittest.main()
