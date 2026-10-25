#!/usr/bin/env python3
"""Native APK/APT/DNF/Zypper/XBPS/Pacman desktop operations; fixed arguments only."""
import plistlib
import gzip,base64,configparser,contextlib,hashlib,importlib.util,json,os,re,subprocess,sys,tempfile
from urllib.parse import urlsplit
import xml.etree.ElementTree as ET
from pathlib import Path
NAME=re.compile(r'[A-Za-z0-9][A-Za-z0-9+_.-]{0,127}')
GROUPS={
 'archlinux':{
  'files':['thunar'],'terminal':['xfce4-terminal'],'editor':['mousepad'],
  'archives':['xarchiver','unzip','zip','7zip'],'images':['ristretto','tumbler'],
  'fonts':['ttf-dejavu','ttf-liberation','adwaita-icon-theme'],'browser':['firefox'],
  'office':['libreoffice-fresh'],'pdf':['evince'],
  'media':['parole','gst-libav','gst-plugins-good','gst-plugins-bad','gst-plugins-base']},
 'void':{
  'files':['Thunar'],'terminal':['xfce4-terminal'],'editor':['mousepad'],
  'archives':['xarchiver','unzip','zip','7zip'],'images':['ristretto','tumbler'],
  'fonts':['dejavu-fonts-ttf','liberation-fonts-ttf','adwaita-icon-theme'],'browser':['firefox'],
  'office':['libreoffice-writer','libreoffice-calc','libreoffice-impress'],'pdf':['evince'],
  'media':['parole','gst-libav','gst-plugins-good1','gst-plugins-bad1','gst-plugins-base1']},
 'opensuse':{
  'files':['thunar'],'terminal':['xfce4-terminal'],'editor':['mousepad'],
  'archives':['engrampa','unzip','zip','7zip'],'images':['ristretto','tumbler'],
  'fonts':['dejavu-fonts','liberation-fonts','adwaita-icon-theme'],'browser':['MozillaFirefox'],
  'office':['libreoffice-writer','libreoffice-calc','libreoffice-impress'],'pdf':['evince','evince-plugin-pdfdocument'],
  'media':['parole','gstreamer-plugins-libav','gstreamer-plugins-good','gstreamer-plugins-bad','gstreamer-plugins-base']},
 'alpine':{
  'files':['thunar'],'terminal':['xfce4-terminal'],'editor':['mousepad'],
  'archives':['xarchiver','unzip','zip','7zip'],'images':['ristretto','tumbler'],
  'fonts':['font-dejavu','font-liberation','adwaita-icon-theme'],'browser':['firefox'],
  'office':['libreoffice-writer','libreoffice-calc','libreoffice-impress'], 'pdf':['evince'],
  'media':['parole','gst-libav','gst-plugins-good','gst-plugins-bad','gst-plugins-base']},
 'fedora':{
  'files':['Thunar'],'terminal':['xfce4-terminal'],'editor':['mousepad'],
  'archives':['xarchiver','unzip','zip','7zip'],'images':['ristretto','tumbler'],
  'fonts':['dejavu-sans-fonts','liberation-sans-fonts','adwaita-icon-theme'],'browser':['firefox'],
  'office':['libreoffice-writer','libreoffice-calc','libreoffice-impress'], 'pdf':['evince'],
  'media':['parole','gstreamer1','libstdc++','libgcc','gstreamer1-plugin-libav','gstreamer1-plugins-good','gstreamer1-plugins-bad-free','gstreamer1-plugins-base']},
 'termux':{
  'files':['thunar'],'terminal':['xfce4-terminal'],'editor':['mousepad'],
  'archives':['xarchiver','unzip','zip','7zip'],'images':['ristretto','tumbler','poppler'],
  'fonts':['ttf-dejavu','adwaita-icon-theme'],'browser':['firefox'],
  'office':['libreoffice'],'pdf':['evince','libandroid-complex-math'],
  'media':['parole','gst-libav','gst-plugins-good','gst-plugins-bad','gst-plugins-base']}}
CORE=re.compile(r'(?:pacman(?:-.*)?|archlinuxarm-keyring|xbps(?:-.*)?|base-(?:files|filesystem|system)|Thunar(?:-.*)?|rpm(?:-.*)?|dnf(?:5)?(?:-.*)?|zypper(?:-.*)?|libzypp(?:-.*)?|glibc(?:-.*)?|systemd(?:-.*)?|bash|coreutils(?:-.*)?|xfce4(?:-.*)?|xfdesktop(?:4)?(?:-.*)?|xfwm4(?:-.*)?|thunar(?:-.*)?|xfconf|libxfconf.*|fcitx5(?:-.*)?|dbus(?:-.*)?|libpulse(?:0)?|pulseaudio|gst-plugins-good|gstreamer1\.0-pulseaudio|musl(?:-.*)?|apk-tools(?:-.*)?|ca-certificates(?:-.*)?|font-.*|fonts-.*|ttf-.*|adwaita-icon-theme|hicolor-icon-theme|python(?:3)?(?:-.*)?|xauth|xorg-xauth|xprop|xorg-xprop|xwininfo|xorg-xwininfo|x11-.*)')
GPU_PACKAGES={'archlinux':['glmark2','mesa-utils'],'void':['glmark2','glxinfo'],
 'opensuse':['glmark2','Mesa-demo-x'],'alpine':['mesa-utils'],
 'fedora':['glmark2','glx-utils'],'termux':['glmark2','mesa-demos']}
for _target,_packages in GPU_PACKAGES.items():GROUPS[_target]['gpu-tools']=_packages
for _target,_packages in GPU_PACKAGES.items():GROUPS[_target]['gpu-test']=[*list(_packages),'python' if _target in ('termux','archlinux') else 'python3']
# The local GLX diagnostic does not execute glmark2 or mesa-demos.
GROUPS['termux']['gpu-test']=['python','libx11','mesa']
def clean(value,limit):return ' '.join(str(value or '').split())[:limit]
def manager_environment(args):
 env=dict(os.environ,LC_ALL='C')
 if args and args[0] in ('xbps-query','xbps-install','xbps-remove','xbps-fetch'):
  env.pop('FORCE_IPV6',None);env['FORCE_IPV4']='1'
 return env
def output(args):return subprocess.check_output(args,text=True,env=manager_environment(args))
def run(args):subprocess.run(args,check=True,env=manager_environment(args),pass_fds=(3,))
def load(name):
 p=Path(__file__).with_name(name+'.py');s=importlib.util.spec_from_file_location(name,p);m=importlib.util.module_from_spec(s);s.loader.exec_module(m);return m

@contextlib.contextmanager
def zypper_repositories(directory=Path('/etc/zypp/repos.d'),keep=False):
 # A command-local view: do not change the user's repo files or bypass GPG.
 # The codec server may return 403; OSS provides the libopenh264 ABI stub.
 with tempfile.TemporaryDirectory(prefix='termux-jobs-zypper-') as temporary:
  filtered=False;codec_excluded=False;files=sorted(directory.glob('*.repo'))
  if len(files)>128:raise ValueError('too many repository definitions')
  for path in files:
   if path.is_symlink() or not path.is_file() or path.stat().st_size>1024*1024:raise ValueError('unsafe repository definition')
   text=path.read_text();ini=configparser.ConfigParser(interpolation=None,strict=True)
   try:ini.read_string(text)
   except configparser.Error:raise ValueError('invalid repository definition') from None
   changed=False
   for section in ini.sections():
    if keep:ini.set(section,'keeppackages','1');changed=True;filtered=True
    for value in ini.get(section,'baseurl',fallback='').split():
     url=urlsplit(value)
     if url.hostname=='codecs.opensuse.org' and url.path.startswith('/openh264/'):
      ini.set(section,'enabled','0');changed=True;filtered=True;codec_excluded=True;break
   destination=Path(temporary)/path.name
   with destination.open('w') as f:
    if changed:ini.write(f)
    else:f.write(text)
  if codec_excluded:print('JOBS_ZYPPER_REPO:openh264-excluded',flush=True)
  yield ['--reposd-dir',temporary] if filtered else []

def zypper_install(pkgs,resume=False):
 with zypper_repositories(keep=True) as options:
  if not resume:run(['zypper',*options,'--non-interactive','refresh'])
  print('JOBS_PROGRAM_INDEX_READY',flush=True)
  if pkgs:
   print('JOBS_APT_PHASE:install',flush=True)
   run(['zypper',*options,'--non-interactive','--no-refresh','install','--no-recommends',*pkgs])

def apk_database(path=Path('/lib/apk/db/installed')):
 if path.is_symlink() or not path.is_file() or path.stat().st_size>128*1024*1024:raise ValueError('invalid APK package database')
 packages={};owners={};checksums={};name='';folder='';file=''
 for line in path.read_text(errors='replace').splitlines():
  key,sep,value=line.partition(':')
  if not sep:continue
  if key=='P':name=value;packages[name]=True;folder='';file=''
  elif key=='F':folder=value;file=''
  elif key=='R':
   file='/'+folder+'/'+value
   p=Path(file)
   if '..' in p.parts or not NAME.fullmatch(name):raise ValueError('invalid APK file ownership')
   if p.parent in (Path('/usr/share/applications'),Path('/usr/local/share/applications')) and p.suffix=='.desktop':owners[str(p)]=name
  elif key=='Z' and str(Path(file)) in owners:checksums[str(Path(file))]=value
 return packages,owners,checksums

def xbps_name(version):
 name,sep,revision=version.rpartition('-')
 return name if sep and revision and NAME.fullmatch(name) and re.fullmatch(r'[0-9][A-Za-z0-9+_.:~]*_[0-9]+',revision) else ''

def xbps_manifests(directory=Path('/var/db/xbps')):
 owners={};sums={};files=sorted(directory.glob('.*-files.plist'))
 if directory.is_symlink() or len(files)>50000:raise ValueError('invalid XBPS metadata directory')
 for manifest in files:
  if manifest.is_symlink() or not manifest.is_file() or manifest.stat().st_size>16*1024*1024:continue
  name=manifest.name[1:-12]
  if not NAME.fullmatch(name):continue
  data=plistlib.loads(manifest.read_bytes())
  for item in data.get('files',[]):
   if not isinstance(item,dict):continue
   p=Path('/'+str(item.get('file','')).lstrip('/'));digest=str(item.get('sha256',''))
   if '..' in p.parts or p.parent not in (Path('/usr/share/applications'),Path('/usr/local/share/applications')) or p.suffix!='.desktop':continue
   owners[str(p)]=name
   if re.fullmatch('[0-9a-f]{64}',digest):sums[str(p)]=digest
 return owners,sums

def arch_manifests(directory=Path('/var/lib/pacman/local'),verify=False):
 # Native package-owned launcher records. No recursive filesystem scan.
 owners={};sums={};records=sorted(directory.glob('*/desc'))
 if directory.is_symlink() or len(records)>50000:raise ValueError('invalid pacman metadata directory')
 for desc in records:
  if desc.parent.is_symlink() or desc.is_symlink() or not desc.is_file() or desc.stat().st_size>1024*1024:continue
  fields=desc.read_text(errors='replace').splitlines()
  try:name=fields[fields.index('%NAME%')+1]
  except (ValueError,IndexError):continue
  if not NAME.fullmatch(name):continue
  files=desc.with_name('files')
  if files.is_symlink() or not files.is_file() or files.stat().st_size>16*1024*1024:continue
  for line in files.read_text(errors='replace').splitlines():
   p=Path('/'+line.lstrip('/'))
   if '..' not in p.parts and p.parent==Path('/usr/share/applications') and p.suffix=='.desktop':owners[str(p)]=name
  if not verify:continue
  mtree=desc.with_name('mtree')
  if mtree.is_symlink() or not mtree.is_file() or mtree.stat().st_size>16*1024*1024:continue
  # Limit decompression before parsing package checksums.
  with gzip.open(mtree,'rb') as f:data=f.read(16*1024*1024+1)
  if len(data)>16*1024*1024:raise ValueError('pacman manifest too large')
  for line in data.decode(errors='replace').splitlines():
   pieces=line.split()
   if not pieces:continue
   relative=pieces[0][2:] if pieces[0].startswith('./') else pieces[0]
   path=Path('/'+relative)
   if str(path) not in owners or owners[str(path)]!=name:continue
   digest=next((v.split('=',1)[1] for v in pieces[1:] if v.startswith('sha256digest=')),'')
   if not re.fullmatch('[0-9a-f]{64}',digest) or path.is_symlink() or not path.is_file() or path.stat().st_size>262144:continue
   content=path.read_bytes()
   if hashlib.sha256(content).hexdigest()==digest:sums[str(path)]=hashlib.md5(content).hexdigest()
 return owners,sums

def installed(target):
 if target=='archlinux':return {n for n in output(['pacman','-Qq']).splitlines() if NAME.fullmatch(n)}
 if target=='alpine':return set(apk_database()[0])
 if target=='void':return {name for line in output(['xbps-query','-l']).splitlines() if line.startswith('ii ') and len(line.split())>=2 and (name:=xbps_name(line.split()[1]))}
 if target in ('fedora','opensuse'):return {n for n in output(['rpm','-qa','--qf','%{NAME}\n']).splitlines() if NAME.fullmatch(n)}
 return {l.split('\t')[0].split(':')[0] for l in output(['dpkg-query','-W','-f=${binary:Package}\t${Status}\n']).splitlines() if '\t' in l and l.split('\t',1)[1].split()[1:]==['ok','installed']}

def packages(target,id):
 if id.startswith('pkg:'):
  name=id[4:]
  if not NAME.fullmatch(name):raise ValueError('invalid program package')
  if target=='termux' and name=='gnupg':return ['gnupg','libgpg-error','libgcrypt']
  if target=='termux' and name=='fcitx5':return ['fcitx5','libc++']
  if name=='parole':id='media'
  elif name=='ristretto':id='images'
  elif name=='evince' and target in ('termux','opensuse'):id='pdf'
  else:return [name]
 if id not in GROUPS[target]:raise ValueError('unknown program')
 return GROUPS[target][id]

def local_gpu_test(target,pkgs):
 # Existing configured runtime dependencies suffice to copy our own payload.
 # Missing dependencies still go through the authenticated native transaction.
 if not set(pkgs)<=installed(target):return False
 load('x11-gpu-test-setup').prepare(Path.home(),target)
 print('JOBS_APT_PHASE:install',flush=True)
 return True

def available(target):
 result={}
 if target=='archlinux':
  for block in re.split(r'\n\s*\n',output(['pacman','-Si'])):
   fields={}
   for line in block.splitlines():
    k,sep,v=line.partition(' : ')
    if sep:fields[k.strip()]=v.strip()
   name=fields.get('Name','')
   if NAME.fullmatch(name):result[name]=clean(fields.get('Description',''),500)
 elif target=='void':
  for line in output(['xbps-query','-Rs','']).splitlines():
   match=re.fullmatch(r'\[[^\]]\]\s+(\S+)\s+(.*)',line)
   if match and (name:=xbps_name(match.group(1))):result[name]=clean(match.group(2),500)
 elif target=='opensuse':
  document=ET.fromstring(output(['zypper','--xmlout','--non-interactive','--no-refresh','search','--details','--type','package']))
  for item in document.iter('solvable'):
   name=item.get('name','')
   if item.get('kind')=='package' and NAME.fullmatch(name):result[name]=clean(item.get('summary',''),500)
 elif target=='termux':
  for block in output(['apt-cache','dumpavail']).split('\n\n'):
   fields={}
   for line in block.splitlines():
    if not line.startswith(' ') and ': ' in line:
     k,v=line.split(': ',1);fields[k]=v
   n=fields.get('Package','')
   if NAME.fullmatch(n):result[n]=clean(fields.get('Description',''),500)
 elif target=='fedora':
  text=output(['dnf','--quiet','--cacheonly','repoquery','--available','--latest-limit=1','--queryformat','%{name}\t%{summary}\n'])
  for line in text.splitlines():
   n,sep,summary=line.partition('\t')
   if sep and NAME.fullmatch(n):result[n]=clean(summary,500)
 else:
  names={n for n in output(['apk','search','--no-progress','-q']).splitlines() if NAME.fullmatch(n)}
  for line in output(['apk','search','--no-progress','-v']).splitlines():
   label,_,summary=line.partition(' - ')
   # Match the longest real package name, including names containing versions.
   positions=[m.start() for m in re.finditer('-',label)]
   for i in reversed(positions):
    n=label[:i]
    if n in names:result[n]=clean(summary,500);break
  for n in names:result.setdefault(n,'')
 if len(result)>100000:raise ValueError('package catalog exceeds limit')
 return result

def catalog(target,only_installed=False,prefix=None):
 have=installed(target);result={}
 if only_installed:
  directory=(prefix/'share/applications') if prefix else Path('/usr/share/applications')
  if target=='archlinux':owners=arch_manifests()[0]
  elif target=='alpine':owners=apk_database()[1]
  elif target=='void':owners=xbps_manifests()[0]
  elif target in ('fedora','opensuse'):
   owners={}
   for line in output(['rpm','-qa','--qf','[%{FILENAMES}\t%{=NAME}\n]']).splitlines():
    path,sep,name=line.partition('\t')
    if sep and NAME.fullmatch(name) and Path(path).parent==directory and path.endswith('.desktop'):owners[path]=name
  else:
   owners={}
   proc=subprocess.run(['dpkg-query','-S',str(directory/'*.desktop')],capture_output=True,text=True)
   if proc.returncode not in (0,1):raise ValueError('cannot read desktop ownership')
   for line in proc.stdout.splitlines():
    if ': ' in line:
     pkg,path=line.split(': ',1);owners[path]=pkg.split(', ')[0].split(':')[0]
  files=sorted(directory.glob('*.desktop'))
  if len(files)>10000:raise ValueError('too many installed desktop entries')
  language='ru' if os.environ.get('JOBS_X11_LANGUAGE')=='ru' else 'en'
  for f in files:
   name=owners.get(str(f),'')
   if not NAME.fullmatch(name) or name not in have or f.is_symlink() or f.stat().st_size>65536:continue
   ini=configparser.ConfigParser(interpolation=None,strict=False)
   try:
    ini.read_string(f.read_text());e=ini['Desktop Entry']
    if e.get('Type')!='Application' or e.get('Hidden','false')=='true' or e.get('NoDisplay','false')=='true':continue
    title=clean(e.get('Name['+language+']',e.get('Name',name)),128);summary=clean(e.get('Comment['+language+']',e.get('Comment','')),500)
   except (OSError,UnicodeError,configparser.Error,KeyError):continue
   result[name]=(title or name,summary,False)
 else:result={n:(n,s,True) for n,s in available(target).items()}
 for n,(title,summary,available_) in sorted(result.items()):
  print('CATALOG:'+json.dumps(dict(package=n,name=title,summary=summary,installed=n in have,available=available_,snap=False),ensure_ascii=False,separators=(',',':')))
 print('READY:catalog',flush=True)

def remove(target,name):
 if not NAME.fullmatch(name) or CORE.fullmatch(name):raise ValueError('protected desktop component')
 if target=='archlinux':
  if name in {'breeze-icons','xkeyboard-config','libglvnd','vulkan-icd-loader','libdrm','llvm22-libs','spirv-tools','libdisplay-info','xcb-util-keysyms'}:raise ValueError('protected desktop graphics component')
  run(['pacman','-R','--noconfirm','--',name])
  if name in installed(target):raise ValueError('program still installed')
  return
 if target=='void':
  plan=output(['xbps-remove','--dry-run','--',name]);changes=[]
  for line in plan.splitlines():
   fields=line.split()
   if len(fields)==6 and fields[1]=='remove':changes.append(xbps_name(fields[0]))
  if changes!=[name]:raise ValueError('XBPS removal would affect other packages; preserved')
  run(['xbps-remove','-y','--',name])
  if name in installed(target):raise ValueError('program still installed')
  return
 if target in ('fedora','opensuse'):
  # RPM removes exactly this named package; dependency checks remain enabled.
  # Unlike `dnf remove`, it does not silently remove reverse dependencies/orphans.
  companions=['evince-plugin-pdfdocument'] if target=='opensuse' and name=='evince' and 'evince-plugin-pdfdocument' in installed(target) else []
  run(['rpm','-e','--',*companions,name])
  if set([*companions,name]) & installed(target):raise ValueError('program still installed')
  return
 if target=='termux':
  # Only this reviewed documentation companion is part of the confirmed removal.
  # dpkg still refuses any other reverse dependency; no autoremove or force.
  companions=['evince-help'] if name=='evince' and 'evince-help' in installed(target) else []
  run(['dpkg','--remove','--',*companions,name])
  if name in installed(target):raise ValueError('program still installed')
  return
 # APK can remove orphan dependencies implicitly. Refuse any wider plan.
 text=output(['apk','del','--simulate','--no-progress',name])
 changes=re.findall(r'^\([0-9]+/[0-9]+\) (?:Purging|Removing) ([a-z0-9+.-]+) ',text,re.M)
 if changes!=[name]:raise ValueError('APK removal would affect other packages; preserved')
 run(['apk','del','--no-progress',name])
 if name in installed(target):raise ValueError('program still installed')

def repair(target,prefix=None):
 m=load('x11-launchers')
 if target=='termux':
  m.DIRS=(prefix/'share/applications',prefix/'local/share/applications')
  sums=m.package_checksums(prefix/'var/lib/dpkg/info')
 elif target=='archlinux':sums=arch_manifests(verify=True)[1]
 elif target=='void':
  sums={}
  for path,digest in xbps_manifests()[1].items():
   p=Path(path)
   if not p.is_symlink() and p.is_file() and p.stat().st_size<=262144:
    data=p.read_bytes()
    if hashlib.sha256(data).hexdigest()==digest:sums[path]=hashlib.md5(data).hexdigest()
 elif target in ('fedora','opensuse'):
  sums={}
  for line in output(['rpm','-qa','--dump']).splitlines():
   fields=line.split()
   if len(fields)<4:continue
   path,_,_,digest=fields[:4];p=Path(path)
   if p.parent not in m.DIRS or p.suffix!='.desktop' or p.is_symlink() or not p.is_file() or p.stat().st_size>262144:continue
   if not re.fullmatch(r'(?:[0-9a-f]{32}|[0-9a-f]{64})',digest):continue
   data=p.read_bytes();actual=(hashlib.md5(data) if len(digest)==32 else hashlib.sha256(data)).hexdigest()
   if actual==digest:sums[path]=hashlib.md5(data).hexdigest()
 else:
  sums={};checksums=apk_database()[2]
  for path,s in checksums.items():
   p=Path(path)
   if p.parent not in m.DIRS or p.suffix!='.desktop' or p.is_symlink() or not p.is_file() or p.stat().st_size>262144:continue
   data=p.read_bytes()
   if s.startswith('Q1') and base64.b64encode(hashlib.sha1(data).digest()).decode()==s[2:]:sums[path]=hashlib.md5(data).hexdigest()
 print('JOBS_X11_LAUNCHER_PERMISSIONS '+json.dumps(m.repair(m.DIRS,Path.home()/'.local/state/termux-jobs/launcher-permissions.json',True,sums)))

def main():
 target=os.environ.get('JOBS_X11_NATIVE_TARGET','');args=sys.argv[1:]
 if target not in GROUPS or not args:raise ValueError('invalid native desktop request')
 prefix=Path(os.environ['PREFIX']) if target=='termux' else None
 if prefix and (str(prefix)!='/data/data/com.termux/files/usr' or Path.home()!=prefix.parent/'home'):raise ValueError('invalid Termux paths')
 op=args[0];id=args[1] if len(args)>1 else ''
 if op=='repair-launchers':repair(target,prefix);return
 if op=='firefox-dns-on' and GROUPS[target]['browser'][0] not in installed(target):raise ValueError('Firefox is not installed in this environment')
 if op in ('firefox-dns-on','firefox-dns-off'):load('x11-firefox-dns').configure(Path.home(),op.endswith('-on'))
 if op in ('firefox-dns-on','firefox-dns-off'):pass
 elif op in ('firefox-compat-on','firefox-compat-off'):
  load('x11-maintenance').compatibility(Path.home(),Path('/'),op.endswith('-on'),prefix)
 elif op in ('install','catalog-install'):
  if op=='catalog-install':id=''
  pkgs=list(packages(target,id)) if id else []
  # Bootstrap may contain an older C++ runtime with no versioned dependency edge.
  # Request the current runtime with this native program transaction, not at every launch.
  if target=='termux' and pkgs and 'libc++' not in pkgs:pkgs.append('libc++')
  if id=='gpu-test' and local_gpu_test(target,pkgs):
   return main_check(target)
  resume=op=='install' and os.environ.get('JOBS_X11_PROGRAM_RESUME')=='1'
  print('JOBS_APT_PHASE:update',flush=True)
  if target=='archlinux':
   # Resume reuses pacman's package cache and sync DB; -u still prevents partial upgrades.
   print('JOBS_APT_PHASE:install',flush=True)
   run(['pacman','-Su' if resume else '-Syu','--needed','--noconfirm','--',*pkgs])
  elif target=='void':
   if not resume:run(['xbps-install','-S'])
  elif target=='alpine':
   if not resume:run(['apk','update','--no-progress','--progress-fd','3'])
  elif target=='fedora':
   if not resume:run(['dnf','-y','--disable-repo=fedora-cisco-openh264','--setopt=retries=3','--setopt=timeout=30','makecache','--refresh'])
  elif target=='opensuse':zypper_install(pkgs,resume)
  elif not resume:run(['apt-get','-o','Acquire::Retries=3','-o','Acquire::http::Timeout=30','-o','Acquire::https::Timeout=30','-o','Debug::acquire::progress=true','-o','APT::Status-Fd=3','-o','Dpkg::Use-Pty=0','update'])
  if target!='opensuse':print('JOBS_PROGRAM_INDEX_READY',flush=True)
  if pkgs:
   if target not in ('opensuse','archlinux'):print('JOBS_APT_PHASE:install',flush=True)
   if target=='archlinux':pass # Same transaction refreshed, upgraded and installed.
   elif target=='void':run(['xbps-install','-y','--',*pkgs])
   elif target=='alpine':
    run(['mkdir','-p','--','/var/cache/apk'])
    run(['apk','--cache-dir','/var/cache/apk','add','--no-progress','--progress-fd','3',*pkgs])
   elif target=='fedora':run(['dnf','-y','--disable-repo=fedora-cisco-openh264','--setopt=retries=3','--setopt=timeout=30','--setopt=keepcache=True','--setopt=install_weak_deps=False',*(['--setopt=metadata_expire=-1','--setopt=check_config_file_age=False'] if resume else []),'install',*pkgs])
   elif target=='opensuse':pass # Already installed in the same filtered repository view.
   else:run(['apt-get','-o','APT::Keep-Downloaded-Packages=true','-o','Acquire::Retries=3','-o','Acquire::http::Timeout=30','-o','Acquire::https::Timeout=30','-o','Debug::acquire::progress=true','-o','APT::Status-Fd=3','-o','Dpkg::Use-Pty=0','install','-y','--no-remove',*pkgs])
   if not set(pkgs)<=installed(target):raise ValueError('program not fully installed')
   if id=='gpu-test':load('x11-gpu-test-setup').prepare(Path.home(),target)
   if id=='gpu-tools':
    helper=load('x11-gpu-tools')
    if not helper.ready(target):raise ValueError('GPU tools binary missing')
    helper.prepare(Path.home(),target)
   if target in ('opensuse','fedora') and id in ('media','pkg:parole'):
    codec=load('x11-openh264');args=[Path.home(),Path('/'),Path(__file__).with_name('x11-openh264.so')]
    if target=='fedora':args.append(Path(__file__).with_name('x11-gst-openh264.so'))
    codec.install(*args)
 elif op=='remove':
  if not id.startswith('pkg:'):raise ValueError('invalid removal')
  if id=='pkg:termux-jobs-gpu-test':load('x11-gpu-test-setup').remove(Path.home())
  else:remove(target,id[4:])
 elif op not in ('check','catalog','installed'):raise ValueError('invalid operation')
 if op in ('catalog','catalog-install','installed'):catalog(target,op=='installed',prefix);return
 main_check(target)

def main_check(target):
 print('FIREFOX_DNS:'+load('x11-firefox-dns').state(Path.home()),flush=True)
 have=installed(target)
 for name,pkgs in GROUPS[target].items():
  complete=set(pkgs)<=have
  if name=='gpu-test' and complete:complete=load('x11-gpu-test-setup').ready(Path.home())
  if name=='gpu-tools' and complete:complete=load('x11-gpu-tools').ready(target)
  if target in ('opensuse','fedora') and name=='media' and complete:
   codec=load('x11-openh264');complete=codec.ready(Path.home(),True) if target=='fedora' else codec.ready(Path.home())
  print('PROGRAM:'+name+':'+('installed' if complete else 'missing'))
 print('READY:programs',flush=True)
if __name__=='__main__':
 try:main()
 except (OSError,ValueError,subprocess.SubprocessError) as e:print('ERROR:NATIVE_DESKTOP: '+clean(e,240),file=sys.stderr);sys.exit(1)
