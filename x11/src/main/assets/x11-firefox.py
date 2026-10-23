#!/usr/bin/env python3
"""Explicitly consented Firefox install from Mozilla's signed ARM64 APT repo.
No Snap, downloaded shell code, unauthenticated packages or sandbox overrides.
"""
import hashlib, os, re, stat, subprocess, sys, tempfile
from pathlib import Path

KEY_SHA256='3ecc63922b7795eb23fdc449ff9396f9114cb3cf186d6f5b53ad4cc3ebfbb11f'
# Fingerprint verified against the public key served by packages.mozilla.org:
# 35BA A0B3 3E9E B396 F59C A838 C0BA 5CE6 DC63 15A3
SOURCE=b'Types: deb\nURIs: https://packages.mozilla.org/apt\nSuites: mozilla\nComponents: main\nArchitectures: arm64\nSigned-By: /usr/share/keyrings/termux-jobs-mozilla.asc\n'
PREFERENCES=b'Package: firefox firefox-l10n-*\nPin: origin packages.mozilla.org\nPin-Priority: 1001\n\nPackage: *\nPin: origin packages.mozilla.org\nPin-Priority: -1\n'

def publish(root, relative, data):
    path=root/relative
    for parent in reversed(path.parents):
        if parent.is_symlink():raise ValueError('repository parent is a link')
    path.parent.mkdir(parents=True,exist_ok=True)
    if path.is_symlink():raise ValueError('repository file is a link')
    if path.exists():
        if not path.is_file() or path.read_bytes()!=data:raise ValueError('repository file has local changes: '+relative)
        return
    fd,tmp=tempfile.mkstemp(prefix='.jobs-mozilla-',dir=path.parent)
    try:
        with os.fdopen(fd,'wb') as out:out.write(data);out.flush();os.fchmod(out.fileno(),0o644);os.fsync(out.fileno())
        # Do not replace a file created while the temporary file was written.
        os.link(tmp,path)
    finally:os.unlink(tmp)

def configure(root, key):
    if hashlib.sha256(key).hexdigest()!=KEY_SHA256:raise ValueError('Mozilla signing key mismatch')
    publish(root,'usr/share/keyrings/termux-jobs-mozilla.asc',key)
    publish(root,'etc/apt/sources.list.d/termux-jobs-mozilla.sources',SOURCE)
    publish(root,'etc/apt/preferences.d/termux-jobs-mozilla',PREFERENCES)

def output(*args):return subprocess.check_output(args,text=True).strip()

def ready(root=Path('/')):
    try:
        if output('dpkg-query','-W','-f=${Status}','firefox').split()[1:]!=['ok','installed']:return False
        # Reject Ubuntu's Snap transition wrapper without launching the browser.
        binary=root/'usr/lib/firefox/firefox'
        if not binary.is_file() or not os.access(binary,os.X_OK):return False
        with binary.open('rb') as stream:return stream.read(4)==b'\x7fELF'
    except (OSError,subprocess.SubprocessError):return False

def candidate(policy,madison):
    hit=re.search(r'^\s*Candidate:\s*([0-9][0-9A-Za-z.+:~\-]*)\s*$',policy,re.M)
    if not hit:raise ValueError('Firefox candidate unavailable')
    version=hit.group(1)
    for line in madison.splitlines():
        parts=[p.strip() for p in line.split('|')]
        if len(parts)==3 and parts[0]=='firefox' and parts[1]==version and parts[2].startswith('https://packages.mozilla.org/apt ') and 'mozilla/main arm64 Packages' in parts[2]:return version
    raise ValueError('Firefox candidate is not from the Mozilla ARM64 repository')

def ensure_certificates(root, apt, env):
    bundle=root/'etc/ssl/certs/ca-certificates.crt'
    try:configured=output('dpkg-query','-W','-f=${Status}','ca-certificates').split()[1:]==['ok','installed']
    except (OSError,subprocess.SubprocessError):configured=False
    if configured and bundle.is_file() and bundle.stat().st_size>0:return
    # Bootstrap from the environment's signed distro repositories before adding
    # Mozilla HTTPS. An old failed Mozilla source may warn, never bypass TLS.
    if not configured:
        # Existing desktop installs normally already have signed distro indices.
        # Avoid retrying an old Mozilla HTTPS failure before CA can be installed.
        try:policy=output('apt-cache','policy','ca-certificates')
        except (OSError,subprocess.SubprocessError):policy=''
        if not re.search(r'^\s*Candidate:\s*[0-9][0-9A-Za-z.+:~\-]*\s*$',policy,re.M):
            print('JOBS_APT_PHASE:update',flush=True)
            subprocess.run(apt+['update'],env=env,check=True,pass_fds=(3,))
        print('JOBS_APT_PHASE:install',flush=True)
        subprocess.run(apt+['install','-y','--no-remove','--no-install-recommends','ca-certificates'],env=env,check=True,pass_fds=(3,))
    subprocess.run(['update-ca-certificates'],env=env,check=True)
    if not bundle.is_file() or bundle.stat().st_size==0:raise ValueError('CA certificate store is unavailable')

def main():
    if sys.argv[1:]==['--check']:return 0 if ready() else 1
    if sys.argv[1:]!=['--install']:raise ValueError('unknown Firefox operation')
    if output('dpkg','--print-architecture')!='arm64':raise ValueError('Firefox installer requires ARM64')
    env=dict(os.environ,DEBIAN_FRONTEND='noninteractive',LC_ALL='C')
    apt=['apt-get','-o','APT::Keep-Downloaded-Packages=true','-o','Acquire::Retries=3','-o','Acquire::http::Timeout=30','-o','Acquire::https::Timeout=30','-o','Debug::acquire::progress=true','-o','APT::Status-Fd=3','-o','Dpkg::Use-Pty=0']
    ensure_certificates(Path('/'),apt,env)
    configure(Path('/'),Path('/tmp/termux-jobs-x11/mozilla-repo.asc').read_bytes())
    if os.environ.get('JOBS_X11_PROGRAM_RESUME')!='1':
        print('JOBS_APT_PHASE:update',flush=True);subprocess.run(apt+['update'],env=env,check=True,pass_fds=(3,))
    policy=subprocess.check_output(['apt-cache','policy','firefox'],env=env,text=True)
    madison=subprocess.check_output(['apt-cache','madison','firefox'],env=env,text=True)
    version=candidate(policy,madison)
    print('JOBS_PROGRAM_INDEX_READY',flush=True)
    print('JOBS_APT_PHASE:install',flush=True)
    subprocess.run(apt+['install','-y','--no-remove','--no-install-recommends','--allow-downgrades','firefox='+version],env=env,check=True,pass_fds=(3,))
    if not ready():raise ValueError('Firefox ELF binary is not configured')
    return 0

if __name__=='__main__':
    try:sys.exit(main())
    except (OSError,ValueError,subprocess.SubprocessError) as error:print('ERROR:FIREFOX_INSTALL: '+str(error),file=sys.stderr);sys.exit(1)
