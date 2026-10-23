"""Agent first-launch routing; all executables/storage are isolated repository fixtures."""
import subprocess
import unittest
from scripts.tests import test_environment_runtime as fixture
ROOT=fixture.ROOT

class AgentHostEnvironmentTests(unittest.TestCase):
    stub=fixture.EnvironmentRuntimeTests.stub
    tearDown=fixture.EnvironmentRuntimeTests.tearDown
    def setUp(self):
        fixture.EnvironmentRuntimeTests.setUp(self)
        self.env.update(JOBS_AGENT_SETUP_ALLOWED='1', FIXTURE_HELP='install IMAGE --name NAME')
        self.stub('proot-distro', '''printf '%s\\n' "$*" >> "$FIXTURE_LOG"
if [ "$1" = login ];then
 [ "${FIXTURE_LOGIN_FAIL:-0}" != 1 ] || exit 23
 printf 'CHECK:done\\n'
 exit 0
fi
if [ "$*" = 'install --help' ];then printf '%s\\n' "$FIXTURE_HELP";exit;fi
[ "$1" = install ] || exit 92
[ "${FIXTURE_INSTALL_FAIL:-0}" != 1 ] || exit 17
alias=$2
if [ "${3:-}" = --name ];then alias=$4;fi
if [ "$2" = --override-alias ];then alias=$3;fi
mkdir -p "$PREFIX/var/lib/proot-distro/containers/$alias/rootfs/bin"
cp /bin/sh "$PREFIX/var/lib/proot-distro/containers/$alias/rootfs/bin/sh"
''')
    def run_host(self, alias=None, check=False):
        env=dict(self.env)
        if alias is not None:env['JOBS_AGENT_UBUNTU']=alias
        script=ROOT/'app/src/main/assets/agent-host-setup.sh'
        cmd=['/bin/bash',str(script),'check','gemini','api',alias or 'ubuntu-ai'] if check else ['/bin/bash','--noprofile','--norc','-c','source "$1"; jobs_agent_host_prepare','fixture',str(script)]
        return subprocess.run(cmd,env=env,text=True,capture_output=True,timeout=10)
    def installed(self,alias,legacy=False):
        p=self.prefix/'var/lib/proot-distro'/('installed-rootfs' if legacy else 'containers')/alias
        p.mkdir(parents=True)
        return p
    def test_ai_clean_install_is_independent_of_ordinary(self):
        normal=self.installed('ubuntu');marker=normal/'keep';marker.write_text('keep')
        r=self.run_host('ubuntu-ai');self.assertEqual(0,r.returncode,r.stderr)
        self.assertEqual(['install --help','install ubuntu --name ubuntu-ai','login ubuntu-ai -- /bin/true'],self.log.read_text().splitlines())
        self.assertEqual('keep',marker.read_text())
    def test_no_environment_defaults_to_ai(self):
        r=self.run_host();self.assertEqual(0,r.returncode,r.stderr)
        self.assertIn('install ubuntu --name ubuntu-ai',self.log.read_text())
        self.assertFalse((self.prefix/'var/lib/proot-distro/containers/ubuntu').exists())
    def test_existing_ai_only_logs_in(self):
        self.installed('ubuntu-ai');r=self.run_host();self.assertEqual(0,r.returncode)
        self.assertEqual(['login ubuntu-ai -- /bin/true'],self.log.read_text().splitlines())
    def test_existing_busy_ai_never_installs_or_falls_back(self):
        self.installed('ubuntu-ai');self.installed('ubuntu');self.env['FIXTURE_LOGIN_FAIL']='1'
        r=self.run_host();self.assertNotEqual(0,r.returncode);self.assertNotIn('Installing',r.stdout)
        self.assertEqual(['login ubuntu-ai -- /bin/true'],self.log.read_text().splitlines())
    def test_incomplete_container_is_not_reinstalled(self):
        self.installed('ubuntu-ai');self.env['FIXTURE_LOGIN_FAIL']='1'
        self.assertNotEqual(0,self.run_host().returncode)
        self.assertNotIn('install ',self.log.read_text())
    def test_legacy_existing_ai_is_not_reinstalled(self):
        self.installed('ubuntu-ai',True);self.assertEqual(0,self.run_host().returncode)
        self.assertNotIn('install ',self.log.read_text())
    def test_explicit_ordinary_selection_preserved(self):
        r=self.run_host('ubuntu');self.assertEqual(0,r.returncode,r.stderr)
        self.assertIn('install ubuntu --name ubuntu\n',self.log.read_text());self.assertNotIn('ubuntu-ai',self.log.read_text())
    def test_missing_without_consent_never_installs(self):
        self.env['JOBS_AGENT_SETUP_ALLOWED']='0';self.assertNotEqual(0,self.run_host().returncode)
        self.assertFalse(self.log.exists())
    def test_failed_install_does_not_attempt_login(self):
        self.env['FIXTURE_INSTALL_FAIL']='1';self.assertNotEqual(0,self.run_host().returncode)
        self.assertNotIn('login ',self.log.read_text())
    def test_missing_inventory_is_readonly(self):
        r=self.run_host(check=True);self.assertEqual(0,r.returncode)
        self.assertIn('MISSING:ubuntu',r.stdout);self.assertFalse(self.log.exists())
    def test_existing_failed_inventory_is_error_not_missing(self):
        self.installed('ubuntu-ai');self.env['FIXTURE_LOGIN_FAIL']='1'
        r=self.run_host(check=True);self.assertNotEqual(0,r.returncode)
        self.assertNotIn('MISSING:ubuntu',r.stdout);self.assertNotIn('install ',self.log.read_text())
    def test_invalid_alias_rejected(self):
        self.assertNotEqual(0,self.run_host('../ubuntu').returncode);self.assertFalse(self.log.exists())
    def test_ai_legacy_installer_alias(self):
        self.env['FIXTURE_HELP']='legacy';r=self.run_host()
        self.assertEqual(0,r.returncode,r.stderr)
        self.assertIn('install --override-alias ubuntu-ai ubuntu',self.log.read_text())
