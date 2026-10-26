from pathlib import Path
import hashlib
import fcntl
import importlib.util
import os
import tempfile
import unittest
from unittest.mock import patch, Mock
import urllib.error
import contextlib
import io
import sys

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('extras_test', ROOT/'x11/src/main/assets/x11-extras.py')
extras = importlib.util.module_from_spec(spec)
spec.loader.exec_module(extras)


class ExtrasTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory(dir=ROOT/'.verification')
        self.base = Path(self.tmp.name)
        self.cache = self.base/'cache'
        self.destination = self.base/'x11-style.zip'
        self.data = b'verified fixture archive'
        self.pin = hashlib.sha256(self.data).hexdigest()
        self.record = patch.dict(extras.SPECS, {'kali-style': ('x11-style.zip', self.pin, len(self.data))})
        self.record.start()

    def tearDown(self):
        self.record.stop()
        self.tmp.cleanup()

    def test_cold_install_shared_cache_offline_reuse_and_legacy_archive(self):
        fetch = Mock(return_value=self.data)
        extras.ensure('kali-style', self.destination, self.cache, fetch=fetch)
        fetch.assert_called_once()
        inode = self.destination.stat().st_ino
        extras.ensure('kali-style', self.destination, self.cache, fetch=Mock(side_effect=AssertionError('network')))
        self.assertEqual(inode, self.destination.stat().st_ino)
        second = self.base/'another-control'; second.mkdir()
        extras.ensure('kali-style', second/'x11-style.zip', self.cache, install=False, fetch=Mock(side_effect=AssertionError('network')))
        self.assertEqual(self.data, (second/'x11-style.zip').read_bytes())
        legacy = self.base/'legacy'; legacy.mkdir(); (legacy/'x11-style.zip').write_bytes(self.data)
        extras.ensure('kali-style', legacy/'x11-style.zip', self.base/'unused-cache', install=False)
        self.assertFalse((self.base/'unused-cache').exists())

    def test_missing_check_does_not_download(self):
        fetch = Mock(side_effect=AssertionError('network'))
        with self.assertRaisesRegex(ValueError, 'EXTRAS_REQUIRED'):
            extras.ensure('kali-style', self.destination, self.cache, install=False, fetch=fetch)
        self.assertFalse(self.destination.exists())

    def test_bad_download_and_interruption_preserve_old_archive(self):
        self.destination.write_bytes(b'old')
        for fetch, error in [(Mock(return_value=b'wrong'), ValueError), (Mock(side_effect=KeyboardInterrupt), KeyboardInterrupt)]:
            with self.assertRaises(error):
                extras.ensure('kali-style', self.destination, self.cache, fetch=fetch)
            self.assertEqual(b'old', self.destination.read_bytes())
            self.assertFalse((self.cache/(self.pin+'.zip')).exists())
            self.assertFalse(list(self.cache.glob('.extra-*')))

    def test_verified_bytes_not_activated_on_failed_publication_and_retry_works(self):
        with patch.object(extras.os, 'replace', side_effect=OSError('fixture failure')), self.assertRaises(OSError):
            extras.ensure('kali-style', self.destination, self.cache, fetch=lambda *_:self.data)
        self.assertFalse(self.destination.exists()); self.assertFalse(list(self.cache.glob('.extra-*')))
        extras.ensure('kali-style', self.destination, self.cache, fetch=lambda *_:self.data)
        self.assertEqual(self.data,self.destination.read_bytes())

    def test_links_special_files_and_shared_hardlinks_are_rejected(self):
        victim=self.base/'keep'; victim.write_bytes(self.data)
        self.destination.symlink_to(victim)
        with self.assertRaises(OSError): extras.ensure('kali-style',self.destination,self.cache)
        self.destination.unlink(); os.link(victim,self.destination)
        with self.assertRaisesRegex(ValueError,'UNSAFE_FILE'): extras.ensure('kali-style',self.destination,self.cache)
        self.destination.unlink(); os.mkfifo(self.destination)
        with self.assertRaisesRegex(ValueError,'UNSAFE_FILE'): extras.ensure('kali-style',self.destination,self.cache)
        self.destination.unlink(); self.cache.symlink_to(self.base)
        with self.assertRaisesRegex(ValueError,'DIRECTORY_LINK'): extras.ensure('kali-style',self.destination,self.cache)
        self.assertEqual(self.data,victim.read_bytes())

    def test_lock_link_and_cache_archive_link_do_not_touch_their_targets(self):
        self.cache.mkdir(); victim=self.base/'keep';victim.write_bytes(self.data)
        lock=self.cache/'download.lock';lock.symlink_to(victim)
        with self.assertRaises(OSError): extras.ensure('kali-style',self.destination,self.cache)
        lock.unlink();(self.cache/(self.pin+'.zip')).symlink_to(victim)
        with self.assertRaises(OSError): extras.ensure('kali-style',self.destination,self.cache)
        self.assertEqual(self.data,victim.read_bytes())

    def test_transport_rejects_http_redirect_and_bounds_network_errors(self):
        with self.assertRaisesRegex(ValueError,'INSECURE_REDIRECT'):
            extras.HttpsOnly().redirect_request(None,None,302,'',{},'http://example.invalid')
        for error,code in [(urllib.error.URLError('private detail'),'EXTRAS_NETWORK'),(urllib.error.HTTPError('https://example.invalid',404,'',{},None),'EXTRAS_HTTP_404')]:
            opener=Mock();opener.open.side_effect=error
            with patch.object(extras.urllib.request,'build_opener',return_value=opener),self.assertRaisesRegex(ValueError,'^'+code+'$'):
                extras.download('fixed.zip',self.pin,len(self.data))

    def test_waiting_for_another_environment_download_can_be_cancelled(self):
        self.cache.mkdir()
        with (self.cache/'download.lock').open('w') as lock:
            fcntl.flock(lock, fcntl.LOCK_EX)
            check=Mock(side_effect=[None,None,ValueError('WRAPPER_CANCELLED')])
            fetch=Mock(side_effect=AssertionError('network'))
            with self.assertRaisesRegex(ValueError,'WRAPPER_CANCELLED'):
                extras.ensure('kali-style',self.destination,self.cache,fetch=fetch,cancel_check=check)
            fetch.assert_not_called()
        self.assertFalse(self.destination.exists())

    def test_real_entry_point_accepts_android_files_aliases_and_preserves_offline_archive(self):
        files=self.base/'files';(files/'usr').mkdir(parents=True)
        alias=self.base/'files-alias';alias.symlink_to(files,target_is_directory=True)
        prefix=files/'usr'
        targets=['workspace-x11']+['workspace-x11-'+t for t in ('ubuntu','debian','alpine','fedora','opensuse','void','archlinux')]
        for name in targets:
            with self.subTest(control=name):
                control=alias/name;control.mkdir();archive=control/'x11-style.zip';archive.write_bytes(self.data)
                output=io.StringIO()
                with patch.object(extras,'EXPECTED_PREFIX',str(prefix)),patch.dict(os.environ,{'PREFIX':str(prefix)}),patch.object(sys,'argv',['x11-extras.py','kali-style',str(control)]),contextlib.redirect_stdout(output):
                    extras.main()
                self.assertIn('READY:extras',output.getvalue())
                self.assertEqual(self.data,archive.read_bytes())
        self.assertFalse((files/'workspace-x11-extras').exists())

    def test_real_entry_point_rejects_same_basename_in_different_files_directory(self):
        files=self.base/'files';(files/'usr').mkdir(parents=True)
        other=self.base/'other';other.mkdir();control=other/'workspace-x11';control.mkdir()
        prefix=files/'usr'
        with patch.object(extras,'EXPECTED_PREFIX',str(prefix)),patch.dict(os.environ,{'PREFIX':str(prefix)}),patch.object(sys,'argv',['x11-extras.py','kali-style',str(control)]),patch.object(extras,'ensure') as ensure,self.assertRaisesRegex(ValueError,'^EXTRAS_CONTROL$'):
            extras.main()
        ensure.assert_not_called();self.assertEqual([],list(control.iterdir()))

    def test_real_archives_match_pins_and_current_installed_payload_bytes(self):
        with patch.dict(extras.SPECS, {'kali-style': ('x11-style.zip', '6bc19721d09cb5d16b5ee5df6f2758da9653a88e0211dd81ef5fcb408386aec0', 1830990)}):
            for key,(name,pin,size) in extras.SPECS.items():
                source=ROOT/'third-party/graphics-drivers'/name if key=='default-driver' else ROOT/'.tools/x11-components'/name
                self.assertIsNotNone(extras.verified(source,pin,size))
                self.assertTrue((extras.BASE+name).startswith('https://github.com/HezeAI/termux-jobs-components/releases/download/x11-extras-20261025/'))
