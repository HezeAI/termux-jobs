"""Repository configuration is tested only under the repository's temporary tree."""
from pathlib import Path
import importlib.util, tempfile, unittest, subprocess
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[2]
def module(name):
 spec=importlib.util.spec_from_file_location(name,ROOT/'x11/src/main/assets'/('x11-'+name+'.py'));m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m
m=module('firefox')
class FirefoxTests(unittest.TestCase):
 def test_signed_repo_is_pinned_idempotent_and_never_overwrites_local_edits(self):
  key=(ROOT/'x11/src/main/assets/mozilla-repo.asc').read_bytes()
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   root=Path(d);m.configure(root,key);m.configure(root,key)
   source=root/'etc/apt/sources.list.d/termux-jobs-mozilla.sources';self.assertIn(b'Signed-By:',source.read_bytes());self.assertNotIn(b'trusted=yes',source.read_bytes())
   self.assertIn(b'Pin-Priority: -1', (root/'etc/apt/preferences.d/termux-jobs-mozilla').read_bytes())
   source.write_text('local changes')
   with self.assertRaises(ValueError):m.configure(root,key)
   self.assertEqual('local changes',source.read_text())
 def test_wrong_key_and_symlink_parents_fail_before_configuration(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   root=Path(d)
   with self.assertRaises(ValueError):m.configure(root,b'unexpected key')
   self.assertEqual([],list(root.iterdir()))
   outside=root/'outside';outside.mkdir();(root/'usr').symlink_to(outside)
   with self.assertRaises(ValueError):m.configure(root,(ROOT/'x11/src/main/assets/mozilla-repo.asc').read_bytes())
   self.assertEqual([],list(outside.iterdir()))
 def test_ubuntu_snap_or_unrelated_candidates_are_refused(self):
  policy='firefox:\n  Candidate: 156.0.1~build1\n'
  self.assertEqual('156.0.1~build1',m.candidate(policy,'firefox | 156.0.1~build1 | https://packages.mozilla.org/apt mozilla/main arm64 Packages'))
  for listing in ['firefox | 156.0.1~build1 | http://archive.ubuntu.com/ubuntu noble/main arm64 Packages','firefox | 156.0.1~build1 | https://packages.mozilla.org/apt.evil mozilla/main arm64 Packages','firefox | 155 | https://packages.mozilla.org/apt mozilla/main arm64 Packages']:
   with self.assertRaises(ValueError):m.candidate(policy,listing)
 def test_snap_wrapper_is_not_reported_installed(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d,patch.object(m,'output',return_value='install ok installed'):
   root=Path(d);binary=root/'usr/lib/firefox/firefox';binary.parent.mkdir(parents=True);binary.write_bytes(b'#!/bin/sh\nsnap run firefox');binary.chmod(0o755);self.assertFalse(m.ready(root))
   binary.write_bytes(b'\x7fELFfixture');self.assertTrue(m.ready(root))

 def test_missing_ca_is_bootstrapped_before_https_without_tls_bypass(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d,patch.object(m,'output',side_effect=subprocess.CalledProcessError(1,['dpkg-query'])):
   root=Path(d);calls=[]
   def run(args,**kwargs):
    calls.append(args)
    if args==['update-ca-certificates']:
     bundle=root/'etc/ssl/certs/ca-certificates.crt';bundle.parent.mkdir(parents=True);bundle.write_text('fixture trust store')
   with patch.object(m.subprocess,'run',side_effect=run):m.ensure_certificates(root,['apt-get'],{'LC_ALL':'C'})
   self.assertEqual([['apt-get','update'],['apt-get','install','-y','--no-remove','--no-install-recommends','ca-certificates'],['update-ca-certificates']],calls)
 def test_existing_ca_does_not_install_and_empty_store_is_repaired(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d,patch.object(m,'output',return_value='install ok installed'):
   root=Path(d);bundle=root/'etc/ssl/certs/ca-certificates.crt';bundle.parent.mkdir(parents=True);bundle.write_text('fixture')
   with patch.object(m.subprocess,'run') as run:m.ensure_certificates(root,['apt-get'],{});run.assert_not_called()
   bundle.write_text('')
   with patch.object(m.subprocess,'run') as run:
    with self.assertRaisesRegex(ValueError,'certificate store'):m.ensure_certificates(root,['apt-get'],{})
    run.assert_called_once_with(['update-ca-certificates'],env={},check=True)
 def test_ca_install_failure_stops_without_false_success(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d,patch.object(m,'output',return_value='unknown'),patch.object(m.subprocess,'run',side_effect=subprocess.CalledProcessError(1,['apt-get'])) as run:
   with self.assertRaises(subprocess.CalledProcessError):m.ensure_certificates(Path(d),['apt-get'],{})
   self.assertEqual(1,run.call_count)

 def test_cached_distro_ca_candidate_avoids_retrying_broken_mozilla_https(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   root=Path(d);bundle=root/'etc/ssl/certs/ca-certificates.crt';calls=[]
   def output(*args):
    if args[0]=='dpkg-query':raise subprocess.CalledProcessError(1,args)
    return '  Candidate: 20250419\n'
   def run(args,**kwargs):
    calls.append(args)
    if args==['update-ca-certificates']:bundle.parent.mkdir(parents=True);bundle.write_text('fixture')
   with patch.object(m,'output',side_effect=output),patch.object(m.subprocess,'run',side_effect=run):m.ensure_certificates(root,['apt-get'],{})
   self.assertEqual('install',calls[0][1]);self.assertFalse(any('update' in c for c in calls))
