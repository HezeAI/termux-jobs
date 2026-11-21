"""Isolated shell fixtures; never run APT, pkg, Cloud auth or network commands."""
from pathlib import Path
import hashlib,shlex,subprocess,tempfile,os,json
root=Path(__file__).resolve().parents[2];assets=root/'app/src/main/assets';base=root/'.verification/agent-setup-v1.5.30';base.mkdir(exist_ok=True,parents=True)
fixture=Path(tempfile.mkdtemp(prefix='shell-',dir=base));env={k:os.environ[k] for k in ['PATH','LD_LIBRARY_PATH','TMPDIR','LANG'] if k in os.environ};env['HOME']=str(fixture/'home');Path(env['HOME']).mkdir();env['CLOUDSDK_CONFIG']=str(fixture/'cloud');env.pop('GOOGLE_API_KEY',None)
def run(name,text,ok=True):
 p=fixture/(name+'.sh');p.write_text('source "'+str(assets/'agent-dependencies.sh')+'"\n'+text);r=subprocess.run(['/bin/bash','--noprofile','--norc',str(p)],env=env,cwd=fixture,text=True,capture_output=True,timeout=20)
 (fixture/(name+'.result.json')).write_text(json.dumps({'code':r.returncode,'stdout':r.stdout,'stderr':r.stderr}))
 assert (r.returncode==0)==ok,(name,r.returncode,r.stdout,r.stderr)
 return r
r=run('check-readonly','jobs_agent_check gemini vertex\n');assert 'CHECK:done' in r.stdout and 'MISSING:gemini-cli' in r.stdout and 'AUTH:needed' in r.stdout
# Private OpenCode paths must match launch, including the legacy npm layout.
# These fixture executables do not install packages or make network requests.
agents=Path(env['HOME'])/'.local/share/termux-jobs/agents'
legacy=agents/'node_modules/.bin/opencode';legacy.parent.mkdir(parents=True)
legacy.write_text('#!/bin/bash\nprintf "opencode v2.0.28\\n"\n');legacy.chmod(0o700)
r=run('opencode-legacy','jobs_agent_check opencode api\n');assert 'READY:opencode-cli' in r.stdout and 'MISSING:opencode-cli' not in r.stdout
# A private runtime is needed by a script-based CLI even if absent in ambient PATH.
private_node=agents/'node-v22.22.1-linux-arm64/bin/node';private_node.parent.mkdir(parents=True)
private_node.write_text('#!/bin/bash\nif [[ $1 == -e ]];then exit 0;fi;printf "opencode v2.0.28\\n"\n');private_node.chmod(0o700)
legacy.write_text('#!/usr/bin/env node\n');legacy.chmod(0o700)
r=run('opencode-private-node','jobs_agent_check opencode api\n');assert 'READY:opencode-cli' in r.stdout and 'READY:node' in r.stdout
# A broken private direct binary must not be hidden by the old valid npm CLI.
direct=agents/'bin/opencode';direct.parent.mkdir(parents=True);direct.write_text('#!/bin/bash\nprintf "opencode v0.0.1\\n"\n');direct.chmod(0o700)
r=run('opencode-old-direct','jobs_agent_check opencode api\n');assert 'MISSING:opencode-cli' in r.stdout
direct.write_text('#!/bin/bash\nprintf "opencode v2.0.28\\n"\n')
r=run('opencode-private-direct','jobs_agent_check opencode api\n');assert 'READY:opencode-cli' in r.stdout
# No exports leak to the outer launcher or other agents.
r=run('opencode-path-local','before="$PATH";jobs_agent_check opencode api;[[ "$PATH" == "$before" ]]\n')
direct.unlink();legacy.unlink();private_node.unlink()
r=run('opencode-really-missing','jobs_agent_check opencode api\n');assert 'MISSING:opencode-cli' in r.stdout
r=run('key-no-cloud',"export GOOGLE_API_KEY=SYNTHETIC_PRIVATE_KEY\njobs_agent_check gemini vertex\n");assert 'AUTH:needed' not in r.stdout and 'gcloud' not in r.stdout and 'SYNTHETIC' not in r.stdout
r=run('gemini-key-not-vertex',"export GEMINI_API_KEY=SYNTHETIC_GEMINI_KEY\njobs_agent_check gemini vertex\n");assert 'AUTH:needed' in r.stdout and 'SYNTHETIC' not in r.stdout
run('blocked',"apt-get(){ printf bad > apt-called; }; export JOBS_AGENT_SETUP_ALLOWED=0\njobs_agent_prepare gemini vertex\n",False);assert not (fixture/'apt-called').exists()
run('base-install',r'''
command(){ if [[ $1 == -v && $2 == curl && ! -e installed-marker ]]; then return 1;fi;builtin command "$@"; }
apt-get(){ printf '%s\n' "$*" >> apt-log; [[ $1 != install ]] || touch installed-marker;return 0; }
JOBS_AGENT_SETUP_ALLOWED=1 jobs_agent_base_prepare claude api
''');calls=(fixture/'apt-log').read_text();assert calls.startswith('update\n') and 'install -y --no-install-recommends' in calls and calls.rstrip().endswith('curl') and 'upgrade' not in calls
run('apt-failed',r'''
command(){ if [[ $1 == -v && $2 == curl ]]; then return 1;fi;builtin command "$@"; }
apt-get(){ printf '%s\n' "$*" >> failed-apt;return 71; }
JOBS_AGENT_SETUP_ALLOWED=1 jobs_agent_base_prepare claude api
''',False);assert (fixture/'failed-apt').read_text()=='update\n'

# Model a clean Ubuntu AI PATH, with local stand-ins for both Git and APT.
git_bin=fixture/'git-bin';git_bin.mkdir()
pinned_git=fixture/'pinned git';pinned_git.write_text('#!/bin/bash\nprintf "git version 2.53.0\\n"\n');pinned_git.chmod(0o700)
git_digest=hashlib.sha256(pinned_git.read_bytes()).hexdigest()
git_fixture=f'export PATH={shlex.quote(str(git_bin))}:"$PATH" JOBS_AGENT_UBUNTU=ubuntu-ai\n'+r'''
command() {
    if [[ $1 == -v && ${2:-} == git && ! -x $PWD/git-bin/git ]]; then return 1; fi
    builtin command "$@"
}
apt-get() {
    printf '%s\n' "$*" >> git-apt-log
    if [[ $1 == install ]]; then cp -- "$PWD/pinned git" "$PWD/git-bin/git"; fi
}
'''
for agent,auth in [('codex','api'),('claude','api'),('gemini','antigravity')]:
 r=run('git-required-'+agent,git_fixture+f'jobs_agent_check {agent} {auth}\n')
 assert 'MISSING:git' in r.stdout.splitlines()
for environment,agent,auth in [('ubuntu','codex','api'),('ubuntu','claude','api'),('ubuntu-ai','gemini','api'),('ubuntu-ai','opencode','api')]:
 r=run('git-optional-'+environment+'-'+agent,git_fixture+f'JOBS_AGENT_UBUNTU={environment} jobs_agent_check {agent} {auth}\n')
 assert not {'MISSING:git','READY:git'}.intersection(r.stdout.splitlines())
git_fallback=f'export JOBS_PROJECT_GIT_EXECUTABLE={shlex.quote(str(pinned_git))} JOBS_PROJECT_GIT_SHA256={git_digest}\n'
r=run('git-pinned',git_fixture+git_fallback+'jobs_agent_check claude api\njobs_agent_base_prepare claude api\n')
assert 'READY:git' in r.stdout.splitlines() and not (fixture/'git-apt-log').exists()
bad_git_hash='JOBS_PROJECT_GIT_SHA256='+'0'*64+' '
r=run('git-altered-hash',git_fixture+git_fallback+bad_git_hash+'jobs_agent_check claude api\n')
assert 'MISSING:git' in r.stdout.splitlines()
git_link=fixture/'pinned-git-link';git_link.symlink_to(pinned_git)
r=run('git-linked-fallback',git_fixture+git_fallback+f'JOBS_PROJECT_GIT_EXECUTABLE={shlex.quote(str(git_link))} jobs_agent_check claude api\n')
assert 'MISSING:git' in r.stdout.splitlines()
run('git-apt-refused',git_fixture+'JOBS_AGENT_SETUP_ALLOWED=0 jobs_agent_prepare claude api\n',False)
assert not (fixture/'git-apt-log').exists()
run('git-apt-inventory-race',git_fixture+r'''
jobs_agent_check() { printf '%s\n' READY:git CHECK:done; }
JOBS_AGENT_SETUP_ALLOWED=0 jobs_agent_prepare claude api
''',False)
assert not (fixture/'git-apt-log').exists()
r=run('git-apt-approved',git_fixture+r'''
JOBS_AGENT_SETUP_ALLOWED=1 jobs_agent_prepare claude api || exit $?
jobs_agent_check claude api || exit $?
jobs_agent_base_prepare claude api
''')
assert 'READY:git' in r.stdout.splitlines()
assert (fixture/'git-apt-log').read_text().splitlines()==[
 'update','install -y --no-install-recommends -o Dpkg::Options::=--force-confold git',
]
r=run('git-path-first',git_fixture+git_fallback+bad_git_hash+'jobs_agent_check claude api\n')
assert 'READY:git' in r.stdout.splitlines()

# A checksum failure must stop before tar/move, even when called under || return.
run('codex-bad-hash',r'''
command(){ if [[ $1 == -v && $2 == codex ]]; then return 1;fi;builtin command "$@"; }
curl(){ local arg;while (($#));do if [[ $1 == -o ]];then shift;printf corrupt > "$1";break;fi;shift;done; }
tar(){ printf bad > tar-called;return 0; }
source "'''+str(assets/'codex-bootstrap.sh')+'"\n',False);assert not (fixture/'tar-called').exists();assert not (Path(env['HOME'])/'.local/share/termux-jobs/codex-cli/0.163.0').exists()
# Existing ADC is never reset/re-authenticated. A Gemini API key cannot bypass this.
Path(env['CLOUDSDK_CONFIG']).mkdir();(Path(env['CLOUDSDK_CONFIG'])/'application_default_credentials.json').write_text('{}')
run('existing-adc',r'''
gcloud(){ printf bad > auth-called; };curl(){ printf bad > download-called; }
source "'''+str(assets/'vertex-bootstrap.sh')+'"\n');assert not (fixture/'auth-called').exists();assert not (fixture/'download-called').exists()
(Path(env['CLOUDSDK_CONFIG'])/'application_default_credentials.json').unlink()
run('cloud-denied',r'''
command(){ if [[ $1 == -v && $2 == gcloud ]];then return 1;fi;builtin command "$@"; }
export JOBS_AGENT_SETUP_ALLOWED=0
source "'''+str(assets/'vertex-bootstrap.sh')+'"\n',False);assert not (fixture/'download-called').exists()
# Host installer never mutates packages without consent, and never removes existing rootfs.
run('host-denied',r'''
command(){ if [[ $1 == -v && $2 == proot-distro ]];then return 1;fi;builtin command "$@"; }
pkg(){ printf bad > pkg-called; }
source "'''+str(assets/'agent-host-setup.sh')+'"\nJOBS_AGENT_SETUP_ALLOWED=0 jobs_agent_host_prepare\n',False);assert not (fixture/'pkg-called').exists()
run('host-approved',r'''
export PREFIX="$PWD/host-prefix"
mkdir -p "$PREFIX/var/lib/proot-distro/containers/ubuntu-ai/rootfs"
command(){ if [[ $1 == -v && $2 == proot-distro && ! -e proot-installed ]];then return 1;fi;builtin command "$@"; }
pkg(){ printf '%s\n' "$*" >> pkg-log;touch proot-installed; }
proot-distro(){ printf '%s\n' "$*" >> proot-log; [[ $1 == login ]]; }
source "'''+str(assets/'agent-host-setup.sh')+'"\nJOBS_AGENT_SETUP_ALLOWED=1 jobs_agent_host_prepare\n');assert (fixture/'pkg-log').read_text()=='install -y proot-distro\n';assert (fixture/'proot-log').read_text()=='login ubuntu-ai -- /bin/true\n'
# Codex Ubuntu AI needs Python for real instruction storage; legacy Ubuntu is unchanged.
r=run('codex-ai-missing-python',r'''
command(){ if [[ $1 == -v && $2 == python3 ]];then return 1;fi;builtin command "$@"; }
JOBS_AGENT_UBUNTU=ubuntu-ai jobs_agent_check codex api
''');assert 'MISSING:python3' in r.stdout
r=run('codex-legacy-no-new-python-requirement',r'''
command(){ if [[ $1 == -v && $2 == python3 ]];then return 1;fi;builtin command "$@"; }
JOBS_AGENT_UBUNTU=ubuntu jobs_agent_check codex api
''');assert 'MISSING:python3' not in r.stdout
run('codex-ai-python-plan',r'''
export PATH="$PWD/git-bin:$PATH"
command(){ if [[ $1 == -v && $2 == python3 ]];then return 1;fi;builtin command "$@"; }
apt-get(){ printf '%s\n' "$*" >> python-apt-log; }
JOBS_AGENT_UBUNTU=ubuntu-ai JOBS_AGENT_SETUP_ALLOWED=1 jobs_agent_base_prepare codex api
''');assert (fixture/'python-apt-log').read_text().splitlines()==['update','install -y --no-install-recommends -o Dpkg::Options::=--force-confold python3']
# Existing compatible/newer Codex is not replaced by the pinned fallback.
run('codex-existing-preserved',r'''
jobs_agent_has_node(){ return 0; }
codex(){ printf '%s\n' 'codex-cli 99.0.0'; }
curl(){ printf bad > existing-cli-download;return 99; }
source "'''+str(assets/'codex-bootstrap.sh')+'"\n');assert not (fixture/'existing-cli-download').exists()
print('PASS isolated agent setup: read-only inventory, component-only/key privacy, consent guards, missing-only APT, APT failure abort, corrupt archive abort before extraction, existing ADC retained, host preparation consent/no rootfs deletion')

# Component-only Vertex preparation never invokes an interactive sign-in.
cloud=fixture/'existing-gcloud';cloud.write_text('#!/bin/sh\nprintf forbidden > "'+str(fixture/'components-only-auth-called')+'"\n');cloud.chmod(0o700)
run('vertex-components-only',r'''
jobs_agent_cloud_credentials(){ return 1; }
jobs_agent_cloud_path(){ printf '%s\n' "'''+str(cloud)+'''"; }
export JOBS_AGENT_COMPONENTS_ONLY=1 JOBS_AGENT_SETUP_ALLOWED=1
source "'''+str(assets/'vertex-bootstrap.sh')+'"\n');assert not (fixture/'components-only-auth-called').exists()
