"""Deferred built-in import uses real archive validation, scoped caches and cancellation."""
from pathlib import Path
import importlib.util
import tempfile
import unittest
from unittest.mock import Mock, patch

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('default_driver_test', ROOT/'x11/src/main/assets/x11-default-driver.py')
default = importlib.util.module_from_spec(spec)
spec.loader.exec_module(default)


class DefaultDriverTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(dir=ROOT/'.verification')
        self.root = Path(self.temp.name)
        self.wrapper, self.gpu, self.extras = [default.module(n) for n in ('wrapper', 'gpu', 'extras')]
        self.bridge = Mock()
        self.modules = dict(wrapper=self.wrapper, gpu=self.gpu, extras=self.extras, **{'bionic-glibc': self.bridge})
        self.loader = patch.object(default, 'module', side_effect=self.modules.__getitem__)
        self.loader.start()
        self.archive = ROOT/'third-party/graphics-drivers/Turnip-a6xx-a7xx-a8xx.zip'

    def tearDown(self):
        self.loader.stop()
        self.temp.cleanup()

    def marker(self, name):
        base = self.root/name
        marker = base/'.job-fixture/cancel'
        marker.parent.mkdir(parents=True)
        return base, marker

    def test_scoped_real_import_shared_download_and_repeat_offline(self):
        fetch = Mock(return_value=self.archive.read_bytes())
        first, marker = self.marker('workspace-x11-wrapper')
        with patch.object(self.wrapper, 'install_runtime') as runtime:
            default.prepare(first, 'termux', 36, marker, fetch)
            default.prepare(first, 'termux', 36, marker, Mock(side_effect=AssertionError('network')))
            self.assertEqual(2, runtime.call_count)
        value = self.wrapper.driver(first)[0]
        self.assertEqual('a6xx-a7xx-a8xx', value['name'])
        self.assertEqual(self.extras.SPECS['default-driver'][1], value['id'])
        second, marker = self.marker('workspace-x11-bionic-glibc-debian')
        default.prepare(second, 'debian', 36, marker, Mock(side_effect=AssertionError('network')))
        self.bridge.install.assert_called_once_with(second)
        self.assertEqual(value['id'], self.wrapper.driver(second)[0]['id'])
        self.assertNotEqual((first/'current.json').stat().st_ino, (second/'current.json').stat().st_ino)
        fetch.assert_called_once()

    def test_cancelled_download_is_not_imported_or_activated(self):
        base, marker = self.marker('workspace-x11-wrapper')
        def interrupted(*_):
            marker.touch()
            return self.archive.read_bytes()
        with patch.object(self.wrapper, 'install_runtime') as runtime, self.assertRaisesRegex(ValueError, 'CANCELLED'):
            default.prepare(base, 'termux', 36, marker, interrupted)
        runtime.assert_not_called()
        self.assertFalse((base/'current.json').exists())
        self.assertFalse((marker.parent/'driver.zip').exists())
        self.assertFalse(list((self.root/'workspace-x11-extras').glob('*.zip')))

    def test_wrong_bytes_and_incompatible_api_do_not_activate_driver(self):
        for api, data in [(36, b'wrong'), (26, self.archive.read_bytes())]:
            with self.subTest(api=api):
                base, marker = self.marker('fixture-'+str(api))
                with patch.object(self.wrapper, 'install_runtime') as runtime, self.assertRaises(ValueError):
                    default.prepare(base, 'termux', api, marker, lambda *_: data)
                runtime.assert_not_called()
                self.assertFalse((base/'current.json').exists())

    def test_unsupported_target_never_loads_or_downloads(self):
        with self.assertRaisesRegex(ValueError, 'TARGET_UNSUPPORTED'):
            default.prepare(self.root/'unused', 'alpine', 36, self.root/'cancel')


if __name__ == '__main__':
    unittest.main()
