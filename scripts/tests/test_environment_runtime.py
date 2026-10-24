"""Hermetic fixtures: never invoke the installed proot-distro or package manager."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / 'app/src/main/assets/environment-runtime.sh'


class EnvironmentRuntimeTests(unittest.TestCase):
    def setUp(self):
        parent = ROOT / '.verification/environment-runtime-fixtures'
        parent.mkdir(parents=True, exist_ok=True)
        self.temp = tempfile.TemporaryDirectory(dir=parent)
        self.root = Path(self.temp.name)
        self.prefix = self.root / 'usr'
        (self.prefix / 'bin').mkdir(parents=True)
        self.log = self.root / 'calls'
        self.downloads=self.root/'downloads'
        self.env = dict(os.environ, PREFIX=str(self.prefix), PATH=str(self.prefix / 'bin') + ':/usr/bin:/bin', FIXTURE_LOG=str(self.log), JOBS_ENVIRONMENT_DOWNLOAD_DIR=str(self.downloads), JOBS_ENVIRONMENT_DOWNLOADER=str(self.root/"no-downloader.py"))
        self.stub('proot-distro', '''printf '%s\\n' "$*" >> "$FIXTURE_LOG"
if [ "$*" = 'install --help' ]; then printf '%s\\n' "${FIXTURE_HELP:-legacy}"; exit 0; fi
if [ "${FIXTURE_FAIL:-0}" = 1 ]; then exit 17; fi
if [ "$1" = install ]; then
  alias=$2
  if [ "${3:-}" = --name ]; then alias=$4; fi
  mkdir -p "$PREFIX/var/lib/proot-distro/containers/$alias/rootfs/bin"
  cp /bin/sh "$PREFIX/var/lib/proot-distro/containers/$alias/rootfs/bin/sh"
fi
''')
        self.stub('uname', 'printf "%s\\n" "${FIXTURE_MACHINE:-aarch64}"')
        self.stub('curl', '''printf 'CURL\\n' >> "$FIXTURE_LOG"
if [ "${FIXTURE_DOWNLOAD_FAIL:-0}" = 1 ]; then exit 22; fi
while [ "$#" -gt 0 ]; do if [ "$1" = --output ]; then shift; printf fixture > "$1"; fi; shift; done
''')
        self.stub('sha256sum', '''read -r checksum path
[ "$checksum" = dabc2382ddcb725969cf7b9e2f3b102ec862ea6e0294198a30c71e9a4b837f81 ] || exit 99
[ -f "$path" ] || exit 98
printf 'SHA256\\n' >> "$FIXTURE_LOG"
[ "${FIXTURE_BAD_CHECKSUM:-0}" = 0 ]
''')
        self.stub('pkg', 'printf "PKG:%s\\n" "$*" >> "$FIXTURE_LOG"; exit 19\n')

    def tearDown(self):
        self.temp.cleanup()

    def stub(self, name, content):
        p = self.prefix / 'bin' / name
        p.write_text('#!/bin/sh\nset -eu\n' + content)
        p.chmod(0o700)

    def run_helper(self, op, alias):
        return subprocess.run(['/bin/bash', str(SCRIPT), op, alias], env=self.env, text=True, capture_output=True, timeout=10)

    def test_check_never_installs_missing_environment(self):
        result = self.run_helper('check', 'debian')
        self.assertIn('MISSING:environment', result.stdout)
        self.assertFalse(self.log.exists())

    def test_existing_environment_is_not_replaced_even_if_incomplete(self):
        (self.prefix / 'var/lib/proot-distro/installed-rootfs/alpine').mkdir(parents=True)
        result = self.run_helper('install', 'alpine')
        self.assertIn('MISSING:shell', result.stdout)
        self.assertFalse(self.log.exists())

    def test_modern_install_names_selected_alias_and_verifies_shell(self):
        self.env['FIXTURE_HELP'] = 'install IMAGE --name NAME'
        result = self.run_helper('install', 'debian')
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn('READY:shell', result.stdout)
        self.assertEqual(['install --help', 'install debian --name debian'], self.log.read_text().splitlines())

    def test_x11_test_ubuntu_installs_under_separate_alias(self):
        self.env['FIXTURE_HELP'] = 'install IMAGE --name NAME'
        result = self.run_helper('install', 'jobs-x11-ubuntu')
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn('READY:shell', result.stdout)
        self.assertEqual(['install --help', 'install ubuntu --name jobs-x11-ubuntu'], self.log.read_text().splitlines())
        self.assertFalse((self.prefix/'var/lib/proot-distro/containers/ubuntu').exists())

    def test_legacy_install_requires_plugin(self):
        result = self.run_helper('install', 'alpine')
        self.assertNotEqual(0, result.returncode)
        self.assertIn('DISTRIBUTION_UNAVAILABLE', result.stdout)
        plugin = self.prefix / 'etc/proot-distro/alpine.sh'
        plugin.parent.mkdir(parents=True)
        plugin.touch()
        result = self.run_helper('install', 'alpine')
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn('install alpine', self.log.read_text().splitlines())

    def test_invalid_alias_never_calls_tools(self):
        for alias in ('../ubuntu', '-name', 'foo;touch X', 'a\nb'):
            self.assertNotEqual(0, self.run_helper('install', alias).returncode)
        self.assertFalse(self.log.exists())

    def test_install_failure_does_not_claim_readiness(self):
        self.env.update(FIXTURE_HELP='--name', FIXTURE_FAIL='1')
        result = self.run_helper('install', 'debian')
        self.assertEqual(17, result.returncode)
        self.assertNotIn('READY:shell', result.stdout)

    def test_void_uses_official_glibc_full_image(self):
        self.env['FIXTURE_HELP'] = '--name'
        result = self.run_helper('install', 'void')
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn('READY:shell',result.stdout)
        self.assertEqual(['install --help', 'install ghcr.io/void-linux/void-glibc-full:latest --name void'], self.log.read_text().splitlines())

    def test_arch_arm_archive_is_verified_before_install_and_deleted_afterwards(self):
        self.env['FIXTURE_HELP'] = '--name'
        result=self.run_helper('install','archlinux')
        self.assertEqual(0,result.returncode,result.stderr)
        calls=self.log.read_text().splitlines()
        self.assertEqual(['install --help','CURL','SHA256'],calls[:3])
        self.assertIn('archlinux-arm64.tar.xz --name archlinux',calls[3])
        self.assertIn('READY:shell',result.stdout)
        self.assertEqual([],list(self.downloads.iterdir()))

    def test_arch_bad_checksum_never_installs_and_cleans_download(self):
        self.env.update(FIXTURE_HELP='--name',FIXTURE_BAD_CHECKSUM='1')
        result=self.run_helper('install','archlinux')
        self.assertNotEqual(0,result.returncode)
        self.assertIn('ROOTFS_CHECKSUM_MISMATCH',result.stdout)
        self.assertEqual(['install --help','CURL','SHA256'],self.log.read_text().splitlines())
        self.assertEqual([],list(self.downloads.iterdir()))

    def test_arch_download_failure_never_verifies_or_installs(self):
        self.env.update(FIXTURE_HELP='--name',FIXTURE_DOWNLOAD_FAIL='1')
        result=self.run_helper('install','archlinux')
        self.assertNotEqual(0,result.returncode)
        self.assertIn('ROOTFS_DOWNLOAD_FAILED',result.stdout)
        self.assertEqual(['install --help','CURL'],self.log.read_text().splitlines())
        self.assertEqual([],list(self.downloads.iterdir()))

    def test_arch_installer_failure_cleans_archive_without_false_ready(self):
        self.env.update(FIXTURE_HELP='--name',FIXTURE_FAIL='1')
        result=self.run_helper('install','archlinux')
        self.assertEqual(17,result.returncode)
        self.assertNotIn('READY:shell',result.stdout)
        self.assertEqual([],list(self.downloads.iterdir()))

    def test_arch_other_architecture_is_rejected_before_download(self):
        self.env.update(FIXTURE_HELP='--name',FIXTURE_MACHINE='x86_64')
        result=self.run_helper('install','archlinux')
        self.assertNotEqual(0,result.returncode)
        self.assertIn('ARCH_ARM64_REQUIRED',result.stdout)
        self.assertEqual(['install --help'],self.log.read_text().splitlines())

    def test_symlink_alias_is_never_accepted(self):
        container = self.prefix / 'var/lib/proot-distro/containers'
        container.mkdir(parents=True)
        (container / 'debian').symlink_to(self.root, target_is_directory=True)
        result = self.run_helper('check', 'debian')
        self.assertIn('MISSING:environment', result.stdout)
        self.assertFalse(self.log.exists())

    def test_clean_agent_ubuntu_installs_separately_without_creating_normal_ubuntu(self):
        self.env['FIXTURE_HELP']='--name'
        result=self.run_helper('install','ubuntu-ai')
        self.assertEqual(0,result.returncode,result.stderr)
        self.assertIn('READY:shell',result.stdout)
        self.assertEqual(['install --help','install ubuntu --name ubuntu-ai'],self.log.read_text().splitlines())
        self.assertFalse((self.prefix/'var/lib/proot-distro/containers/ubuntu').exists())

    def test_existing_agent_ubuntu_is_never_replaced(self):
        root=self.prefix/'var/lib/proot-distro/containers/ubuntu-ai/rootfs';root.mkdir(parents=True);(root/'keep').write_text('keep')
        result=self.run_helper('install','ubuntu-ai')
        self.assertEqual(0,result.returncode,result.stderr);self.assertFalse(self.log.exists());self.assertEqual('keep',(root/'keep').read_text())

    def test_migration_tools_ready_does_not_reinstall_or_touch_ubuntu(self):
        self.stub('python3','printf "CHECK_PYTHON\\n" >> "$FIXTURE_LOG"; exit 0\n')
        result=self.run_helper('install','agent-migration-tools')
        self.assertEqual(0,result.returncode,result.stderr);self.assertIn('CHECK:done',result.stdout)
        self.assertNotIn('PKG:',self.log.read_text());self.assertNotIn('install ',self.log.read_text())
        self.assertFalse((self.prefix/'var/lib/proot-distro').exists())

    def test_migration_tools_install_failure_is_not_reported_ready(self):
        self.stub('python3','exit 1\n')
        result=self.run_helper('install','agent-migration-tools')
        self.assertEqual(19,result.returncode,result.stderr);self.assertNotIn('READY:shell',result.stdout)
        self.assertIn('PKG:install -y proot-distro',self.log.read_text())
        self.assertFalse((self.prefix/'var/lib/proot-distro').exists())

    def test_resume_never_overwrites_an_existing_partial_root(self):
        self.env['FIXTURE_HELP']='install IMAGE --name NAME'
        fs=self.prefix/'var/lib/proot-distro/containers/debian/rootfs';fs.mkdir(parents=True);keep=fs/'keep';keep.write_text('retained')
        state=self.downloads/'downloads/debian/state.json';state.parent.mkdir(parents=True);state.write_text('{}')
        result=self.run_helper('install','debian')
        self.assertNotEqual(0,result.returncode);self.assertIn('ERROR:INCOMPLETE_ROOTFS_REQUIRES_REMOVAL',result.stdout);self.assertEqual('retained',keep.read_text());self.assertFalse(self.log.exists())

    def test_all_oci_environments_use_only_public_cli_even_with_retired_helper_present(self):
        self.env['FIXTURE_HELP']='--name';helper=self.root/'retired-download.py';helper.write_text('raise RuntimeError("RETIRED_HELPER_MUST_NOT_RUN")\n');self.env['JOBS_ENVIRONMENT_DOWNLOADER']=str(helper)
        for alias,image in [('ubuntu','ubuntu'),('ubuntu-ai','ubuntu'),('debian','debian'),('alpine','alpine'),('fedora','fedora'),('opensuse','opensuse/leap'),('void','ghcr.io/void-linux/void-glibc-full:latest')]:
            result=self.run_helper('install',alias);self.assertEqual(0,result.returncode,alias+result.stderr);self.assertIn('READY:shell',result.stdout);self.assertIn('install '+image+' --name '+alias,self.log.read_text())
        self.assertNotIn('PKG:',self.log.read_text());self.assertNotIn('RESUME_UNSUPPORTED',result.stdout)

    def test_retry_with_partial_root_is_refused_without_removal_or_native_install(self):
        fs=self.prefix/'var/lib/proot-distro/containers/debian/rootfs';fs.mkdir(parents=True);(fs/'keep').write_text('keep');self.env['JOBS_ENVIRONMENT_RETRY']='1'
        result=self.run_helper('install','debian');self.assertNotEqual(0,result.returncode);self.assertIn('INCOMPLETE_ROOTFS_REQUIRES_REMOVAL',result.stdout);self.assertEqual('keep',(fs/'keep').read_text());self.assertFalse(self.log.exists())

    def test_broken_guest_shell_link_is_missing(self):
        fs=self.prefix/'var/lib/proot-distro/containers/debian/rootfs';(fs/'bin').mkdir(parents=True)
        (fs/'bin/sh').symlink_to('missing-shell')
        result=self.run_helper('check','debian')
        self.assertIn('MISSING:shell',result.stdout);self.assertNotIn('READY:shell',result.stdout)

    def test_absolute_guest_shell_link_is_not_resolved_against_host(self):
        fs=self.prefix/'var/lib/proot-distro/containers/debian/rootfs';(fs/'bin').mkdir(parents=True)
        (fs/'bin/sh').symlink_to('/bin/bash')
        result=self.run_helper('check','debian')
        self.assertIn('MISSING:shell',result.stdout);self.assertNotIn('READY:shell',result.stdout)

    def test_absolute_guest_shell_link_to_existing_guest_binary_is_ready(self):
        fs=self.prefix/'var/lib/proot-distro/containers/debian/rootfs';(fs/'bin').mkdir(parents=True);(fs/'usr/bin').mkdir(parents=True)
        (fs/'usr/bin/dash').write_text('fixture');(fs/'usr/bin/dash').chmod(0o700);(fs/'bin/sh').symlink_to('/usr/bin/dash')
        result=self.run_helper('check','debian')
        self.assertIn('READY:shell',result.stdout);self.assertNotIn('MISSING:shell',result.stdout)

    def test_merged_usr_absolute_directory_links_use_guest_root(self):
        fs=self.prefix/'var/lib/proot-distro/containers/debian/rootfs';(fs/'usr/bin').mkdir(parents=True)
        (fs/'usr/bin/sh').write_text('fixture');(fs/'usr/bin/sh').chmod(0o700);(fs/'bin').symlink_to('/usr/bin')
        result=self.run_helper('check','debian')
        self.assertIn('READY:shell',result.stdout);self.assertNotIn('MISSING:shell',result.stdout)

    def test_guest_shell_symlink_loop_is_missing(self):
        fs=self.prefix/'var/lib/proot-distro/containers/debian/rootfs';(fs/'bin').mkdir(parents=True)
        (fs/'bin/sh').symlink_to('sh')
        result=self.run_helper('check','debian')
        self.assertIn('MISSING:shell',result.stdout);self.assertNotIn('READY:shell',result.stdout)

    def test_nonexecutable_guest_shell_is_missing(self):
        fs=self.prefix/'var/lib/proot-distro/containers/debian/rootfs';(fs/'bin').mkdir(parents=True)
        (fs/'bin/sh').write_text('fixture');(fs/'bin/sh').chmod(0o600)
        result=self.run_helper('check','debian')
        self.assertIn('MISSING:shell',result.stdout);self.assertNotIn('READY:shell',result.stdout)

    def test_parent_segments_in_guest_link_cannot_escape_guest_root(self):
        fs=self.prefix/'var/lib/proot-distro/containers/debian/rootfs';(fs/'bin').mkdir(parents=True)
        # A host /bin/bash must not satisfy this missing guest target either.
        (fs/'bin/sh').symlink_to('../../../../../../../../bin/bash')
        result=self.run_helper('check','debian')
        self.assertIn('MISSING:shell',result.stdout);self.assertNotIn('READY:shell',result.stdout)
