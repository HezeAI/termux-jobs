"""Actual shell routing, poisoned prior renderer state, no installed rootfs access."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'x11/src/main/assets'
TARGETS = ('termux', 'ubuntu', 'debian', 'alpine', 'fedora', 'opensuse', 'void', 'archlinux')
VARS = ('GALLIUM_DRIVER', 'MESA_LOADER_DRIVER_OVERRIDE', 'LIBGL_ALWAYS_SOFTWARE',
        'LIBGL_KOPPER_DISABLE', 'VK_DRIVER_FILES', 'VK_ICD_FILENAMES',
        '__EGL_VENDOR_LIBRARY_FILENAMES', 'LD_LIBRARY_PATH', 'LIBGL_DRIVERS_PATH',
        'VTEST_SOCKET_NAME', 'VTEST_RENDERER_SOCKET_NAME')


class RendererEnvironmentTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory(dir=ROOT / '.verification')
        self.base = Path(self.tmp.name)
        # Only the physical Android bind root is replaced in this private fixture.
        self.client_root = self.base / 'clients'
        self.helper = self.base / 'virgl.sh'
        self.helper.write_text((ASSETS / 'x11-virgl-env.sh').read_text().replace(
            '/tmp/termux-jobs-virgl', str(self.client_root)))
        self.env = dict(os.environ, **{k: 'old-renderer-value' for k in VARS})
        # Do not poison the dynamic loader of the shell used by the test itself.
        self.env.pop('LD_LIBRARY_PATH')

    def tearDown(self):
        self.tmp.cleanup()

    def run_helper(self, target, client='none'):
        script = '. "$1"; jobs_virgl_environment "$2" "$3" "$4"; status=$?; '
        script += 'env -0 > "$5"; exit "$status"'
        output = self.base / 'env'
        r = subprocess.run(['/bin/sh', '-c', script, 'fixture', str(self.helper),
                            target, str(self.base / 'own.sock'), client, str(output)],
                           env=self.env, capture_output=True, text=True, timeout=4)
        state = dict(x.split('=', 1) for x in output.read_text().split('\0') if '=' in x)
        return r, state

    def test_all_eight_virgl_targets_select_remote_pipe_not_zink_or_llvmpipe(self):
        client = 'client-0123456789abcdef'
        (self.client_root / client / 'lib').mkdir(parents=True)
        for target in TARGETS:
            with self.subTest(target=target):
                r, state = self.run_helper(target, client if target == 'alpine' else 'none')
                self.assertEqual(0, r.returncode, r.stderr + r.stdout)
                self.assertEqual('virpipe', state['GALLIUM_DRIVER'])
                self.assertEqual('true', state['LIBGL_ALWAYS_SOFTWARE'])
                self.assertEqual('true', state['LIBGL_KOPPER_DISABLE'])
                self.assertEqual(str(self.base / 'own.sock'), state['VTEST_SOCKET_NAME'])
                for k in ('MESA_LOADER_DRIVER_OVERRIDE', 'VK_DRIVER_FILES', 'VK_ICD_FILENAMES',
                          '__EGL_VENDOR_LIBRARY_FILENAMES', 'VTEST_RENDERER_SOCKET_NAME'):
                    self.assertNotIn(k, state)
                if target == 'alpine':
                    self.assertEqual(str(self.client_root / client / 'lib'), state['LD_LIBRARY_PATH'])
                else:
                    self.assertNotIn('LD_LIBRARY_PATH', state)
                    self.assertNotIn('LIBGL_DRIVERS_PATH', state)
                self.assertIn('JOBS_GPU_GLX=drisw-virpipe', r.stdout)

    def test_invalid_target_or_missing_alpine_client_does_not_mutate_prior_state(self):
        for target, client in [('ubuntu-ai', 'none'), ('../debian', 'none'),
                               ('alpine', 'client-0123456789abcdef'),
                               ('alpine', 'client-../../escape'),
                               ('alpine', 'client-Z123456789abcdef')]:
            with self.subTest(target=target, client=client):
                r, state = self.run_helper(target, client)
                self.assertNotEqual(0, r.returncode)
                self.assertEqual('old-renderer-value', state['GALLIUM_DRIVER'])
                self.assertEqual('old-renderer-value', state['VK_DRIVER_FILES'])
                self.assertNotIn('JOBS_GPU_GLX=', r.stdout)

    def test_turnip_all_eight_targets_clear_virgl_and_select_their_pinned_icd(self):
        components = {'termux': ('termux-mesa26.2.5', 'data/data/com.termux/files/usr/lib'),
                      'ubuntu': ('ubuntu-26.04-mesa26.2.5', 'usr/lib/aarch64-linux-gnu'),
                      'debian': ('debian-13-mesa26.2.5', 'usr/lib/aarch64-linux-gnu'),
                      'alpine': ('alpine-3.24-mesa26.2.5', 'usr/lib'),
                      'fedora': ('fedora-44-mesa26.2.5', 'usr/lib64'),
                      'opensuse': ('opensuse-16-mesa26.2.5', 'usr/lib/aarch64-linux-gnu'),
                      'void': ('void-mesa26.2.5', 'usr/lib'),
                      'archlinux': ('archlinux-mesa26.2.5', 'usr/lib')}
        for target, (name, libdir) in components.items():
            with self.subTest(target=target):
                component = self.base / name
                lib = component / libdir
                lib.mkdir(parents=True)
                (component / 'ready.json').write_text('{}')
                (lib / 'libvulkan_freedreno.so').touch()
                output = self.base / 'env'
                command = '. "$1"; jobs_gpu_environment "$2" "$3" || exit; env -0 > "$4"'
                r = subprocess.run(['/bin/sh', '-c', command, 'fixture',
                                    str(ASSETS / 'x11-gpu-env.sh'), target, str(self.base), str(output)],
                                   env=self.env, capture_output=True, text=True, timeout=4)
                self.assertEqual(0, r.returncode, r.stdout + r.stderr)
                state = dict(x.split('=', 1) for x in output.read_text().split('\0') if '=' in x)
                for k in ('LIBGL_ALWAYS_SOFTWARE', 'LIBGL_KOPPER_DISABLE',
                          'VTEST_SOCKET_NAME', 'VTEST_RENDERER_SOCKET_NAME'):
                    self.assertNotIn(k, state)
                self.assertEqual('zink', state['GALLIUM_DRIVER'])
                self.assertEqual('zink', state['MESA_LOADER_DRIVER_OVERRIDE'])
                self.assertEqual(str(component / 'turnip-icd.json'), state['VK_DRIVER_FILES'])
                self.assertEqual(state['VK_DRIVER_FILES'], state['VK_ICD_FILENAMES'])


if __name__ == '__main__':
    unittest.main()
