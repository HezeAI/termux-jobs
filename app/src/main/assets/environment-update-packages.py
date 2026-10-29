"""Fixed native commands and bounded update candidates; never accepts shell input."""
import hashlib,json,re,xml.etree.ElementTree as ET
KINDS={'ubuntu':'apt','debian':'apt','kali':'apt','alpine':'apk','fedora':'dnf','opensuse':'zypper','void':'xbps','archlinux':'pacman','manjaro':'pacman'}
PATH='/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin'
ENV="export PATH="+PATH+" LC_ALL=C DEBIAN_FRONTEND=noninteractive; unset LD_LIBRARY_PATH LD_PRELOAD BASH_ENV ENV DISPLAY WAYLAND_DISPLAY DBUS_SESSION_BUS_ADDRESS; "
REFRESH={
 'apt':'apt-get -o Acquire::Retries=2 -o Acquire::http::Timeout=30 -o Acquire::https::Timeout=30 -o DPkg::Lock::Timeout=0 update',
 'apk':'apk update',
 'dnf':'dnf --quiet --disable-repo=fedora-cisco-openh264 --setopt=retries=2 --setopt=timeout=30 makecache --refresh',
 'zypper':'zypper --non-interactive refresh',
 'xbps':'(unset FORCE_IPV6; export FORCE_IPV4=1; xbps-install -S)',
 'pacman':'pacman -Sy --noconfirm',
}
QUERY={
 'apt':"apt -o Dir::Cache::pkgcache='' -o Dir::Cache::srcpkgcache='' list --upgradable",
 'apk':'apk --no-network list --upgradable',
 # DNF5 expands \\n, but not \\t. A literal tab works with DNF4 and DNF5.
 'dnf':"dnf --quiet --cacheonly --disable-repo=fedora-cisco-openh264 repoquery --upgrades --latest-limit=1 --queryformat '%{name}\t%{evr}\\n'",
 'zypper':'zypper --xmlout --no-refresh list-updates --type package',
 'xbps':'(unset FORCE_IPV6; export FORCE_IPV4=1; xbps-install -un)',
 'pacman':'pacman -Qu || [ "$?" = 1 ]',
}
UPGRADE={
 'apt':'apt-get -o Acquire::Retries=2 -o Acquire::http::Timeout=30 -o Acquire::https::Timeout=30 -o DPkg::Lock::Timeout=0 -o Dpkg::Options::=--force-confdef -o Dpkg::Options::=--force-confold -y full-upgrade',
 'apk':'apk upgrade',
 'dnf':'dnf --disable-repo=fedora-cisco-openh264 --setopt=metadata_expire=-1 --setopt=keepcache=True -y upgrade',
 'zypper':'zypper --non-interactive --no-refresh update --no-recommends',
 'xbps':'(unset FORCE_IPV6; export FORCE_IPV4=1 CONNECTION_TIMEOUT=30000; xbps-install -uy xbps; xbps-install -uy)',
 'pacman':'pacman -Su --noconfirm',
}
# State files are read only; absence does not mean an installed database is empty.
DATABASE={'apt':['var/lib/dpkg/status','var/lib/dpkg/updates'],'apk':['lib/apk/db/installed'],'dnf':['usr/lib/sysimage/rpm/rpmdb.sqlite','usr/lib/sysimage/rpm/rpmdb.sqlite-wal','var/lib/rpm/rpmdb.sqlite','var/lib/rpm/rpmdb.sqlite-wal','var/lib/rpm/Packages'],'zypper':['usr/lib/sysimage/rpm/rpmdb.sqlite','usr/lib/sysimage/rpm/rpmdb.sqlite-wal','var/lib/rpm/rpmdb.sqlite','var/lib/rpm/rpmdb.sqlite-wal','var/lib/rpm/Packages'],'xbps':['var/db/xbps/pkgdb-0.38.plist'],'pacman':['var/lib/pacman/local']}
# RPM supports SQLite, NDB and older Berkeley DB installations. Resolve guest
# aliases through guest_read; never follow an absolute link into the host root.
RPM_DATABASE=[directory+'/'+name for directory in ('usr/lib/sysimage/rpm','var/lib/rpm') for name in ('rpmdb.sqlite','rpmdb.sqlite-wal','Packages.db','Index.db','Packages')]
DATABASE['dnf']=RPM_DATABASE
DATABASE['zypper']=RPM_DATABASE
NAME=re.compile(r'[A-Za-z0-9][A-Za-z0-9+_.:@-]{0,255}')
VERSION=re.compile(r'[A-Za-z0-9][A-Za-z0-9.+:~^_=-]{0,255}')
class Refused(Exception):pass

def script(target,operation):
 kind=KINDS[target];cmd={'refresh':REFRESH,'query':QUERY,'upgrade':UPGRADE}[operation][kind]
 return 'set -eu\n'+ENV+'\n'+cmd+'\n'

def parse(target,data):
 if len(data)>16*1024*1024:raise Refused('PACKAGE_METADATA_TOO_LARGE')
 text=data.decode('utf-8',errors='replace');kind=KINDS[target];rows={}
 def add(name,version,old=''):
  if not NAME.fullmatch(name or '') or not VERSION.fullmatch(version or '') or old and not VERSION.fullmatch(old):raise Refused('PACKAGE_METADATA_INVALID')
  rows[name]={'name':name,'version':version,'installed':old}
  if len(rows)>10000:raise Refused('PACKAGE_METADATA_TOO_LARGE')
 if kind=='zypper':
  if '<!DOCTYPE' in text.upper() or '<!ENTITY' in text.upper():raise Refused('PACKAGE_METADATA_INVALID')
  try:
   root=ET.fromstring(text)
   for row in root.iter('update'):add(row.get('name'),row.get('edition'),row.get('old-edition',''))
  except ET.ParseError:raise Refused('PACKAGE_METADATA_INVALID') from None
 else:
  for line in text.splitlines():
   f=line.strip().split()
   if not f:continue
   if kind=='apt':
    if '/' not in f[0]:continue
    if len(f)<3:raise Refused('PACKAGE_METADATA_INVALID')
    old=re.search(r'\[upgradable from: ([^\]]+)\]',line);add(f[0].split('/',1)[0],f[1],old.group(1) if old else '')
   elif kind=='dnf':
    f=line.split('\t')
    if len(f)!=2:raise Refused('PACKAGE_METADATA_INVALID')
    add(f[0],f[1])
   elif kind=='pacman':
    if len(f)!=4 or f[2]!='->':raise Refused('PACKAGE_METADATA_INVALID')
    add(f[0],f[3],f[1])
   elif kind=='apk':
    match=re.fullmatch(r'(.+)-([0-9][A-Za-z0-9._+~:-]*)',f[0])
    if not match:raise Refused('PACKAGE_METADATA_INVALID')
    add(*match.groups())
   elif kind=='xbps':
    if len(f)!=6 or f[1] not in ('install','update','remove','configure','hold'):raise Refused('PACKAGE_METADATA_INVALID')
    if f[1] not in ('install','update'):continue
    match=re.fullmatch(r'(.+)-([A-Za-z0-9][A-Za-z0-9._+~:]*_[0-9]+)',f[0])
    if not match:raise Refused('PACKAGE_METADATA_INVALID')
    add(*match.groups())
 return [rows[k] for k in sorted(rows)]

def token(rows):return hashlib.sha256(json.dumps(rows,sort_keys=True,separators=(',',':')).encode()).hexdigest()

def diagnostic(data):
 text=data.lower()
 for code,words in [('PACKAGE_MANAGER_BUSY',[b'could not get lock',b'unable to acquire',b'is locked',b'failed to lock',b'database is locked']),('NOT_ENOUGH_SPACE',[b'no space left',b'not enough free space']),('REPOSITORY_KEY_REQUIRED',[b'no_pubkey',b'public key is not installed',b'signature verification failed',b'unknown public key']),('REPOSITORY_NETWORK',[b'temporary failure resolving',b'could not resolve',b'connection timed out',b'failed to download',b'network is unreachable']),('REPOSITORY_UNAVAILABLE',[b'404',b'403',b'no longer has a release file']),('DEPENDENCY_CONFLICT',[b'conflicting dependencies',b'unmet dependencies',b'held broken packages',b'conflicting requests'])]:
  if any(word in text for word in words):return code
 return 'PACKAGE_COMMAND_FAILED'

def check_script(target):
 kind=KINDS[target]
 if kind=='pacman':
  return f"""set -eu
{ENV}
tmp=$(mktemp -d /tmp/.termux-jobs-updates.XXXXXXXX)
trap 'rm -rf -- "$tmp"' EXIT
ln -s /var/lib/pacman/local "$tmp/local"
pacman -Sy --noconfirm --dbpath "$tmp" --logfile /dev/null
printf '\\nJOBS_UPDATES_BEGIN\\n'
pacman -Qu --dbpath "$tmp" || [ "$?" = 1 ]
"""
 return script(target,'refresh')+"printf '\\nJOBS_UPDATES_BEGIN\\n'\n"+QUERY[kind]+'\n'
