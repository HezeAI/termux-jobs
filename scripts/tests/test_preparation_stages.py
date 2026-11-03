#!/usr/bin/env python3
"""Actual host/runtime scripts; artificial pkg/proot/Python, no install or network."""
from pathlib import Path
import os,subprocess,tempfile,json
root=Path(__file__).resolve().parents[2];assets=root/'app/src/main/assets';base=root/'.verification/preparation-stages-test2';base.mkdir(parents=True,exist_ok=True)
fixture=Path(tempfile.mkdtemp(prefix='host-',dir=base));results=[]
for name in ['missing-proot','missing-both','missing-ubuntu','ready','download-error','package-error']:
 case=fixture/name;bin=case/'bin';bin.mkdir(parents=True);prefix=case/'prefix';prefix.mkdir();(case/'image.tar.xz').write_bytes(b'ARTIFICIAL_ARCHIVE')
 def executable(name,text):
  p=bin/name;p.write_text('#!/bin/bash\nset -eu\n'+text);p.chmod(0o700)
 executable('pkg','printf "FAKE_PKG_START\\n";printf "%s\\n" "$*" >> "$CASE/pkg-calls";if [[ $SCENARIO == package-error ]];then exit 71;fi;touch "$CASE/proot-ready"\n')
 executable('proot-distro','printf "%s\\n" "$*" >> "$CASE/proot-calls";if [[ $1 == install ]];then printf "%s\\n" --name;fi\n')
 executable('python3',r'''
if [[ $1 == -B && $2 == */environment-image.py ]];then
 printf 'STAGE:download-pinned-ubuntu-ai\nSTAGE:connecting-image-download\nIMAGE_PROGRESS: 25%% (100/400 bytes)\n' >&2
 if [[ $SCENARIO == download-error ]];then printf 'ERROR:IMAGE_DOWNLOAD_NETWORK\n' >&2;exit 33;fi
 printf 'STAGE:verifying-image-download\nSTAGE:verified-rootfs-sha256\n' >&2
 printf '%s\n' "$CASE/image.tar.xz"
elif [[ $1 == -I && $2 == */ubuntu-ai-installer.py ]];then
 [[ $3 == install && $4 == "$CASE/image.tar.xz" && $5 == --name && $6 == ubuntu-ai ]]
 printf 'FAKE_EXTRACT_START\n';touch "$CASE/extracted"
 mkdir -p "$PREFIX/var/lib/proot-distro/containers/ubuntu-ai/rootfs/bin"
 printf '#!/bin/sh\n' > "$PREFIX/var/lib/proot-distro/containers/ubuntu-ai/rootfs/bin/bash"
 chmod +x "$PREFIX/var/lib/proot-distro/containers/ubuntu-ai/rootfs/bin/bash"
else exit 97;fi
''')
 if name in ['missing-proot','ready']:
  fs=prefix/'var/lib/proot-distro/containers/ubuntu-ai/rootfs';fs.mkdir(parents=True);(fs/'KEEP').write_text('EXISTING_FIXTURE')
 if name not in ['missing-proot','missing-both','package-error']:(case/'proot-ready').touch()
 env={'PATH':str(bin)+':/usr/bin:/bin','HOME':str(case),'PREFIX':str(prefix),'CASE':str(case),'SCENARIO':name,'JOBS_AGENT_SETUP_ALLOWED':'1','JOBS_AGENT_UBUNTU':'ubuntu-ai','JOBS_AGENT_PREPARATION_STATUS':str(case/'status')}
 script='''command(){ if [[ $1 == -v && $2 == proot-distro && ! -e $CASE/proot-ready ]];then return 1;fi;builtin command "$@"; };export -f command
source "$1/agent-host-setup.sh"
jobs_agent_host_prepare
'''
 r=subprocess.run(['/bin/bash','--noprofile','--norc','-c',script,'fixture',str(assets)],env=env,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=10)
 out=r.stdout
 if name=='missing-proot':
  assert r.returncode==0 and r.stdout.index('PREPARATION:preparing-termux-packages')<r.stdout.index('FAKE_PKG_START');assert (case/'pkg-calls').read_text()=='install -y proot-distro\n';assert not (case/'extracted').exists()
 elif name=='ready':assert r.returncode==0 and not (case/'pkg-calls').exists() and not (case/'extracted').exists() and 'installing-ubuntu' not in out
 elif name=='missing-both':
  assert r.returncode==0 and out.index('PREPARATION:preparing-termux-packages')<out.index('FAKE_PKG_START')<out.index('STAGE:connecting-image-download')<out.index('STAGE:extracting-environment') and (case/'extracted').exists()
 elif name=='missing-ubuntu':assert r.returncode==0 and 'STAGE:extracting-environment' in out and r.stdout.index('STAGE:extracting-environment')<r.stdout.index('FAKE_EXTRACT_START') and (case/'extracted').exists() and 'STAGE:verify' in out and out.index('STAGE:connecting-image-download')<out.index('STAGE:extracting-environment')
 elif name=='package-error':assert r.returncode==1 and 'PREPARATION:failed-ubuntu' in out and not (case/'extracted').exists()
 else:assert r.returncode==33 and 'ERROR:IMAGE_DOWNLOAD_NETWORK' in out and 'PREPARATION:failed-ubuntu' in out and not (case/'extracted').exists() and 'STAGE:extracting-environment' not in out
 if name in ['missing-proot','ready']:assert (prefix/'var/lib/proot-distro/containers/ubuntu-ai/rootfs/KEEP').read_text()=='EXISTING_FIXTURE'
 result={'scenario':name,'exit':r.returncode,'passed':True}
 if name in ['download-error','package-error']:
  env['SCENARIO']='missing-both' if name=='package-error' else 'missing-ubuntu'
  retry=subprocess.run(['/bin/bash','--noprofile','--norc','-c',script,'fixture',str(assets)],env=env,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=10)
  assert retry.returncode==0 and (case/'extracted').exists() and 'STAGE:verify' in retry.stdout
  result['retryPassed']=True
 results.append(result)
(base/'host-stage-results.json').write_text(json.dumps({'realInstallation':False,'networkUsed':False,'results':results},indent=2)+'\n');print(json.dumps(results,indent=2))
