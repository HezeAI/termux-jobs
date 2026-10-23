#!/usr/bin/python3
"""AppStream OS catalog matched to the selected environment's native APT cache."""
import json,re,sys
import configparser,subprocess
from pathlib import Path

def clean(value,limit):
    return ' '.join(re.sub(r'<[^>]*>',' ',str(value or '')).split())[:limit]

def collect(pool,cache,kind,configured_state):
    box=pool.get_components_by_kind(kind)
    components=box.as_array() if hasattr(box,'as_array') else box
    result={}
    for component in components:
        names=component.get_pkgnames() or []
        for name in names:
            if not re.fullmatch(r'[a-z0-9][a-z0-9+.-]{1,127}',name) or name not in cache:continue
            package=cache[name];candidate=package.candidate
            if candidate is None and not package.is_installed:continue
            installed=package._pkg.current_state==configured_state
            if name=='ristretto':installed=installed and 'tumbler' in cache and cache['tumbler']._pkg.current_state==configured_state
            description=clean(component.get_summary(),500)
            title=clean(component.get_name(),128) or name
            # Native APT entries that require Snap are not runnable by our proot installer.
            record=candidate.record if candidate else {}
            snap=bool(re.search(r'\bsnapd\b',record.get('Depends','')+' '+record.get('Pre-Depends','')))
            entry={'package':name,'name':title,'summary':description,'installed':installed,'available':candidate is not None and not snap,'snap':snap}
            if name not in result or len(description)>len(result[name]['summary']):result[name]=entry
    return sorted(result.values(),key=lambda e:(e['name'].casefold(),e['package']))

def installed_programs(language, directory=Path('/usr/share/applications'), output=subprocess.check_output):
    # Read package ownership and status in two calls. Never execute desktop-file Exec fields.
    files=sorted(directory.glob('*.desktop'))
    if len(files)>10000:raise RuntimeError('Too many installed desktop entries')
    if not files:return []
    statuses=output(['dpkg-query','-W','-f=${binary:Package}\t${Status}\n'],text=True)
    installed={line.split('\t',1)[0].split(':',1)[0] for line in statuses.splitlines() if '\t' in line and line.split('\t',1)[1].split()[1:]==['ok','installed']}
    ownership=output(['dpkg-query','-S',str(directory/'*.desktop')],text=True)
    owners={}
    for line in ownership.splitlines():
        if ': ' not in line:continue
        packages,path=line.split(': ',1)
        for name in packages.split(', '):
            name=name.split(':',1)[0]
            if name in installed and re.fullmatch(r'[a-z0-9][a-z0-9+.-]{1,127}',name):owners[path]=name;break
    result={}
    for file in files:
        name=owners.get(str(file))
        if not name or file.is_symlink() or file.stat().st_size>65536:continue
        ini=configparser.ConfigParser(interpolation=None,strict=False)
        try:
            ini.read_string(file.read_text(encoding='utf-8'))
            entry=ini['Desktop Entry']
            if entry.get('Type')!='Application' or entry.get('Hidden','false').lower()=='true' or entry.get('NoDisplay','false').lower()=='true':continue
            title=clean(entry.get('Name['+language+']',entry.get('Name',name)),128) or name
            summary=clean(entry.get('Comment['+language+']',entry.get('Comment','')),500)
        except (OSError,UnicodeError,configparser.Error,KeyError):continue
        result.setdefault(name,{'package':name,'name':title,'summary':summary,'installed':True,'available':False,'snap':False})
    return sorted(result.values(),key=lambda e:(e['name'].casefold(),e['package']))

def emit(entries):
    for entry in entries:print('CATALOG:'+json.dumps(entry,ensure_ascii=False,separators=(',',':')))
    print('READY:catalog',flush=True)

def main():
    if len(sys.argv)>2 and sys.argv[2]=='installed':
        emit(installed_programs('ru' if sys.argv[1]=='ru' else 'en'));return 0

    try:
        import gi
        gi.require_version('AppStream','1.0')
        from gi.repository import AppStream
        import apt,apt_pkg
    except (ImportError,ValueError):
        print('ERROR:CATALOG_TOOLS_REQUIRED',flush=True);return 3
    pool=AppStream.Pool();pool.set_locale('ru' if len(sys.argv)>1 and sys.argv[1]=='ru' else 'en')
    pool.set_flags(AppStream.PoolFlags.LOAD_OS_CATALOG)
    if not pool.load(None):raise RuntimeError('AppStream catalog could not be loaded')
    entries=collect(pool,apt.Cache(),AppStream.ComponentKind.DESKTOP_APP,apt_pkg.CURSTATE_INSTALLED)
    if not entries:
        print('ERROR:CATALOG_METADATA_EMPTY',flush=True);return 4
    if len(entries)>10000:raise RuntimeError('AppStream catalog exceeds safety limit')
    emit(entries);return 0

if __name__=='__main__':
    try:sys.exit(main())
    except Exception as error:
        print('ERROR:CATALOG:'+clean(error,240),flush=True);sys.exit(1)
