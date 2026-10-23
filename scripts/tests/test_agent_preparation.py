"""Isolated shell fixtures; never run APT, pkg, Cloud auth or network commands."""
from pathlib import Path
import subprocess,tempfile,os,json
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
jobs_agent_base_prepare claude api
''');calls=(fixture/'apt-log').read_text();assert calls.startswith('update\n') and 'install -y --no-install-recommends' in calls and calls.rstrip().endswith('curl') and 'upgrade' not in calls
run('apt-failed',r'''
command(){ if [[ $1 == -v && $2 == curl ]]; then return 1;fi;builtin command "$@"; }
apt-get(){ printf '%s\n' "$*" >> failed-apt;return 71; }
jobs_agent_base_prepare claude api
''',False);assert (fixture/'failed-apt').read_text()=='update\n'
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
print('PASS isolated agent setup: read-only inventory, component-only/key privacy, consent guards, missing-only APT, APT failure abort, corrupt archive abort before extraction, existing ADC retained, host preparation consent/no rootfs deletion')
