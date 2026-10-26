import copy
import gzip
import hashlib
import importlib.util
import io
import json
import os
from pathlib import Path
import shutil
import tarfile
import tempfile
import unittest
import sys
sys.dont_write_bytecode=True
from unittest.mock import patch

ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('backup', ROOT/'app/src/main/assets/workspace-backup.py')
b=importlib.util.module_from_spec(spec);spec.loader.exec_module(b)


def elf():
    head=bytearray(64);head[:4]=b'\x7fELF';head[4]=2;head[5]=1;head[18:20]=(183).to_bytes(2,'little');return bytes(head)


class BackupTests(unittest.TestCase):
    def setUp(self):
        parent=ROOT/'.verification/backup-v1.1.0/fixtures';parent.mkdir(parents=True,exist_ok=True)
        self.temp=tempfile.TemporaryDirectory(dir=parent);self.root=Path(self.temp.name)
        self.home=self.root/'home';self.prefix=self.root/'usr';self.home.mkdir();self.prefix.mkdir();self.work=self.root/'work';self.work.mkdir()
        self.req=dict(home=str(self.home),prefix=str(self.prefix),files=str(self.root/'files'),packageId='com.termux',appVersion='1.1.0')
    def tearDown(self): self.temp.cleanup()
    def tar(self,files,name='sample.tar.gz'):
        p=self.root/name
        with tarfile.open(p,'w:gz') as t:
            for key,value in files:
                info=tarfile.TarInfo(key)
                if isinstance(value,bytes):info.size=len(value);info.mode=0o640;t.addfile(info,io.BytesIO(value))
                else:info.type=value[0];info.linkname=value[1];t.addfile(info)
        return p
    def inspect(self,p):return b.inspect(dict(self.req,path=str(p)))
    def library(self):return dict(version=8,commands=[],notes=[],sessions=[],recentCommands=[])
    def test_alias_export_is_standalone_preserves_bindings_and_excludes_unrelated_data(self):
        value=self.library();value['aliases']=[dict(id='11111111-1111-1111-1111-111111111111',name='ll',command="ls -lah; printf 'not executed'",environment='debian',category='Files',favorite=True)]
        value['privateSetting']='not exported'
        output=self.root/'alias-output';output.mkdir()
        result=b.export_aliases(dict(destination=str(output),workspace=value))
        path=Path(result['path']);self.assertEqual(path.suffix,'.json');self.assertEqual(result['count'],1)
        saved=json.loads(path.read_text());self.assertNotIn('privateSetting',saved);self.assertEqual(saved['aliases'],value['aliases'])
        report=b.inspect(dict(self.req,path=str(path)));self.assertEqual(report['workspace'],saved);self.assertEqual(report['candidates'],[])
        second=b.export_aliases(dict(destination=str(output),workspace=value));self.assertNotEqual(second['path'],result['path']);self.assertTrue(path.is_file())
        value['commands']=[dict(command='private command')]
        self.assertRaises(b.BackupError,b.export_aliases,dict(destination=str(output),workspace=value))
    def test_alias_library_inspection_accepts_old_backups_and_bounds_new_array(self):
        self.assertEqual(b.libraries(self.library())['version'],8)
        value=self.library();value['aliases']={};self.assertRaises(b.BackupError,b.libraries,value)
        value['aliases']=[{}]*20001;self.assertRaises(b.BackupError,b.libraries,value)
    def test_manual_alias_import_reads_literals_and_skips_dynamic_statements_without_execution(self):
        marker=self.root/'must-not-exist'
        source=self.root/'.bashrc'
        source.write_text("# header\nalias ll='ls -lah'\nalias echo_path=\"echo \\$HOME\"\nalias complex='printf \\\"quotes\\\"; echo $(later)'\nalias multiline='first\nsecond'\nalias unsafe=$(touch "+str(marker)+")\nalias dynamic=\"$HOME\"\nalias skipped='ls'; touch "+str(marker)+"\necho 'not executed'\nalias ll='ls -la' # final definition\n")
        with patch.object(b.subprocess,'Popen',side_effect=AssertionError('must not execute a shell')):
            report=b.inspect_alias_shell(dict(path=str(source),environment='debian'))
        definitions={a['name']:a['command'] for a in report['workspace']['aliases']}
        self.assertEqual(definitions['ll'],'ls -la');self.assertEqual(definitions['echo_path'],'echo $HOME');self.assertEqual(definitions['multiline'],'first\nsecond');self.assertEqual(report['skipped'],3)
        self.assertTrue(all(a['environment']=='debian' and a['category']=='' for a in report['workspace']['aliases']));self.assertFalse(marker.exists())
        self.assertEqual(report['fingerprints'][str(source)],hashlib.sha256(source.read_bytes()).hexdigest())
    def test_shell_alias_export_roundtrips_quotes_multiline_and_one_environment_only(self):
        value=self.library();value['aliases']=[dict(id='11111111-1111-1111-1111-111111111111',name='quotes',command="printf '%s' \"$HOME\"; echo 'apostrophe'\nnext",environment='ubuntu',category='Files',favorite=True),dict(id='22222222-2222-2222-2222-222222222222',name='other',command='echo safe',environment='ubuntu',category='',favorite=False)]
        output=self.root/'shell-output';output.mkdir();result=b.export_aliases(dict(destination=str(output),workspace=value,aliasFormat='shell'));path=Path(result['path']);self.assertEqual(path.suffix,'.sh')
        report=b.inspect_alias_shell(dict(path=str(path),environment='alpine'));self.assertEqual({a['name']:a['command'] for a in report['workspace']['aliases']},{a['name']:a['command'] for a in value['aliases']});self.assertEqual(report['skipped'],0)
        value['aliases'][1]['environment']='debian';self.assertRaises(b.BackupError,b.export_aliases,dict(destination=str(output),workspace=value,aliasFormat='shell'))
    def test_shell_export_is_readable_by_real_bash_without_executing_alias_bodies(self):
        import subprocess
        marker=self.root/'never-run';value=self.library();body="printf '%s' \"$HOME\"; touch "+str(marker)+"\nnext"
        value['aliases']=[dict(id='11111111-1111-1111-1111-111111111111',name='verify',command=body,environment='termux',category='',favorite=False)]
        output=self.root/'bash-output';output.mkdir();result=b.export_aliases(dict(destination=str(output),workspace=value,aliasFormat='shell'))
        printed=subprocess.run(['bash','--noprofile','--norc','-c','. "$1"; alias -p','alias-check',result['path']],stdin=subprocess.DEVNULL,stdout=subprocess.PIPE,stderr=subprocess.PIPE,check=True,text=True).stdout
        definitions,skipped=b.literal_alias_commands(printed);self.assertEqual(definitions,{'verify':body});self.assertEqual(skipped,0);self.assertFalse(marker.exists())
        definitions,skipped=b.literal_alias_commands("alias semi='echo yes'; # allowed terminator\nalias chain=echo; touch nope\n")
        self.assertEqual(definitions,{'semi':'echo yes'});self.assertEqual(skipped,1)
    def test_manual_alias_import_bounds_input_and_rejects_ai_and_unfinished_quotes(self):
        path=self.root/'.zshrc';path.write_text("alias valid='echo ok'\nalias broken='unfinished\n")
        result=b.inspect_alias_shell(dict(path=str(path),environment='termux'));self.assertEqual([a['name'] for a in result['workspace']['aliases']],['valid']);self.assertEqual(result['skipped'],1)
        self.assertRaises(b.BackupError,b.inspect_alias_shell,dict(path=str(path),environment='ubuntu-ai'))
        path.write_bytes(b'\0');self.assertRaises(b.BackupError,b.inspect_alias_shell,dict(path=str(path),environment='termux'))
        path.write_bytes(b'x'*(1024*1024+1));self.assertRaises(b.BackupError,b.inspect_alias_shell,dict(path=str(path),environment='termux'))
    def test_home_aliases_and_scripts_no_execution(self):
        p=self.tar([('home/.bashrc',b"alias hello='echo hi'\nalias dynamic=$(touch NEVER)\nsource /tmp/evil\n"),('home/scripts/build.sh',b'#!/bin/bash\ntouch NEVER\n')]);r=self.inspect(p)
        self.assertEqual(r['components'][0]['kind'],'home');self.assertEqual(len(r['candidates']),2);self.assertEqual(r['candidates'][0]['command'],'echo hi');self.assertFalse((self.root/'NEVER').exists())
    def test_new_and_legacy_proot_arch(self):
        (self.prefix/'bin').mkdir();(self.prefix/'bin/proot-distro').write_text('# proot_distro')
        for root in ['ubuntu/rootfs','installed-rootfs/ubuntu']:
            p=self.tar([(root+'/usr/bin/env',elf()),(root+'/etc/os-release',b'ID=ubuntu\n')],root.replace('/','-')+'.tar.gz');r=self.inspect(p)
            self.assertEqual(r['components'][0]['status'],'compatible');self.assertEqual(r['components'][0]['arch'],'aarch64')
    def test_cross_arch_blocked(self):
        data=bytearray(elf());data[18:20]=(62).to_bytes(2,'little');p=self.tar([('ubuntu/rootfs/bin/bash',bytes(data))]);self.assertEqual(self.inspect(p)['components'][0]['status'],'blocked')
    def test_unconfirmed_arch_blocked(self):
        p=self.tar([('ubuntu/rootfs/etc/os-release',b'ID=ubuntu')]);self.assertEqual(self.inspect(p)['components'][0]['status'],'blocked')
    def test_traversal_and_absolute_rejected(self):
        for key in ['../escape','/absolute','home/../../escape','home\\escape']:
            p=self.tar([(key,b'bad')]);self.assertRaises(b.BackupError,self.inspect,p)
    def test_duplicate_rejected(self):
        p=self.tar([('home/a',b'1'),('home/a',b'2')]);self.assertRaises(b.BackupError,self.inspect,p)
    def test_symlink_write_before_or_after_rejected(self):
        for values in [[('home/link',(tarfile.SYMTYPE,'/tmp')),('home/link/file',b'bad')],[('home/link/file',b'bad'),('home/link',(tarfile.SYMTYPE,'/tmp'))]]:
            p=self.tar(values);self.assertRaises(b.BackupError,self.inspect,p)
    def test_external_symlink_not_dereferenced(self):
        p=self.tar([('home/storage',(tarfile.SYMTYPE,'/sdcard')),('home/.bashrc',b'# safe')]);r=self.inspect(p);self.assertEqual(r['components'][0]['kind'],'home')
        dest=self.home/'new';b.extract_new(p,'home',dest);self.assertEqual(os.readlink(dest/'storage'),'/sdcard');self.assertTrue((dest/'storage').is_symlink())
    def test_unsafe_hardlink_rejected(self):
        p=self.tar([('home/hard',(tarfile.LNKTYPE,'../outside'))]);self.assertRaises(b.BackupError,self.inspect,p)
    def test_gzip_trailer_corruption_rejected(self):
        p=self.tar([('home/.bashrc',b'foo')]);data=bytearray(p.read_bytes());data[-8]^=1;p.write_bytes(data);self.assertRaises((OSError,EOFError,tarfile.TarError),self.inspect,p)
    def test_extract_preserves_files_modes_and_links(self):
        p=self.tar([('home/file',b'payload'),('home/hard',(tarfile.LNKTYPE,'home/file')),('home/sym',(tarfile.SYMTYPE,'file'))]);b.scan_tar(p);dest=self.home/'new';b.extract_new(p,'home',dest)
        self.assertEqual((dest/'file').read_bytes(),b'payload');self.assertEqual((dest/'hard').read_bytes(),b'payload');self.assertEqual((dest/'file').stat().st_mode&0o777,0o640);self.assertEqual(os.readlink(dest/'sym'),'file')
    def test_never_overwrites_existing_target(self):
        p=self.tar([('home/file',b'new')]);dest=self.home/'new';dest.mkdir();(dest/'file').write_text('old');self.assertRaises(FileExistsError,b.extract_new,p,'home',dest);self.assertEqual((dest/'file').read_text(),'old')
    def test_manifest_checksum_and_component_traversal(self):
        p=self.tar([('home/.bashrc',b'foo')]);m=self.root/'manifest.json';value=dict(format='termux-jobs-backup',version=1,components=[dict(file=p.name,kind='home',sha256='0'*64,bytes=p.stat().st_size)])
        m.write_text(json.dumps(value));self.assertRaises(b.BackupError,self.inspect,m)
        value['components'][0]['file']='../outside';m.write_text(json.dumps(value));self.assertRaises(b.BackupError,self.inspect,m)
    def test_incomplete_manifest_rejected(self):
        m=self.root/'manifest.json';m.write_text(json.dumps(dict(format='termux-jobs-backup',version=1,components=[])));(self.root/'INCOMPLETE').touch();self.assertRaises(b.BackupError,self.inspect,m)
    def test_source_change_blocks_restore(self):
        p=self.tar([('home/.bashrc',b'foo')]);r=self.inspect(p);self.tar([('home/.bashrc',b'changed')]);self.assertRaises(b.BackupError,b.restore,dict(self.req,path=str(p),fingerprints=r['fingerprints'],selected=[0],destination=str(self.home),work=str(self.work)))
        self.assertEqual(list(self.home.iterdir()),[])
    def test_restore_stages_home_does_not_merge(self):
        (self.home/'.bashrc').write_text('original');p=self.tar([('home/.bashrc',b'backup')]);r=self.inspect(p);out=b.restore(dict(self.req,path=str(p),fingerprints=r['fingerprints'],selected=[0],destination=str(self.home),work=str(self.work)))
        self.assertEqual((self.home/'.bashrc').read_text(),'original');self.assertEqual(Path(out['paths']['0'],'.bashrc').read_text(),'backup');self.assertFalse(Path(out['path'],'INCOMPLETE').exists())
    def test_prefix_destination_forbidden(self):
        p=self.tar([('home/.bashrc',b'foo')]);r=self.inspect(p);self.assertRaises(b.BackupError,b.restore,dict(self.req,path=str(p),fingerprints=r['fingerprints'],selected=[0],destination=str(self.prefix),work=str(self.work)))
    def test_create_verify_and_standalone_helpers(self):
        (self.home/'.bashrc').write_text("alias x='echo safe'\n");files=self.root/'files';files.mkdir();(files/'workspace-sessions').mkdir();output=self.root/'output';output.mkdir()
        req=dict(self.req,destination=str(output),components=['library','home','prefix'],environments=[],workspace=self.library(),settings={})
        result=b.create(req);manifest=Path(result['path'],'manifest.json');r=self.inspect(manifest)
        self.assertTrue(any(c['kind']=='home' for c in r['components']));self.assertEqual(r['workspace']['version'],8);self.assertFalse((manifest.parent/'INCOMPLETE').exists());self.assertTrue((manifest.parent/'restore.sh').is_file());self.assertTrue((manifest.parent/'SHA256SUMS').is_file());self.assertEqual((self.home/'.bashrc').read_text(),"alias x='echo safe'\n")
    def test_output_inside_home_blocked(self):
        self.assertRaises(b.BackupError,b.create,dict(self.req,destination=str(self.home),components=['home'],environments=[],workspace=self.library()))
    def test_package_list_status_and_argument_validation(self):
        self.assertEqual(b.parse_packages('bash\t5.2\nfoo\nbad;cmd 1\n'),{'bash':'5.2','foo':''});self.assertFalse(b.valid_package('bash; touch x','5.2'));self.assertFalse(b.valid_package('bash','$(evil)'))
        text='Package: bash\nStatus: install ok installed\nVersion: 5.2\n\nPackage: removed\nStatus: deinstall ok config-files\nVersion: 1\n'
        self.assertEqual(b.status_packages(text),{'bash':'5.2'})
    def test_ubuntu_repack_fresh_name_only_no_manifest_execution(self):
        p=self.tar([('ubuntu/rootfs/usr/bin/env',elf()),('ubuntu/rootfs/etc/os-release',b'ID=ubuntu'),('ubuntu/manifest.json',b'{"danger":"not executed"}')]);scan=b.scan_tar(p);c=scan['components'][0];c['status']='compatible';calls=[]
        with patch.object(b,'run_tool',lambda args:calls.append(args)):
            alias=b.restore_ubuntu(p,c,self.prefix,self.work)
        self.assertRegex(alias,r'^jobs-[a-f0-9]{12}$');self.assertEqual(calls[0][1],'restore');self.assertNotIn('ubuntu.tar',calls[0][-1]);self.assertFalse((self.prefix/'var/lib/proot-distro/containers/ubuntu').exists())
    def test_nested_ubuntu_does_not_contaminate_termux_package_versions(self):
        native=b'Package: bash\nStatus: install ok installed\nVersion: 5.2-termux\n'
        guest=b'Package: bash\nStatus: install ok installed\nVersion: 5.3-ubuntu\n'
        p=self.tar([('usr/var/lib/dpkg/status',native),('usr/var/lib/proot-distro/installed-rootfs/ubuntu/var/lib/dpkg/status',guest),('usr/var/lib/proot-distro/installed-rootfs/ubuntu/usr/bin/env',elf())])
        r=self.inspect(p);self.assertEqual(r['packages']['bash'],'5.2-termux');self.assertTrue(any(c['kind']=='ubuntu' for c in r['components']))
    def test_apt_plan_is_simulation_and_versions_are_not_shell(self):
        calls=[]
        def fake(args,**kw):
            calls.append(args);return type('Result',(),dict(returncode=0,stdout=b'Plan only'))()
        with patch.object(b.subprocess,'run',fake):
            result=b.apt_plan(dict(self.req,packages={'bash':'5.2'}))
        self.assertTrue(result['ok']);self.assertIn('-s',calls[0]);self.assertIn('--no-remove',calls[0]);self.assertEqual(calls[0][-1],'bash=5.2');self.assertNotIn('-s',result['command']);self.assertRaises(b.BackupError,b.apt_plan,dict(self.req,packages={'bash;evil':'5.2'}))
    def test_python_security_limit(self):
        p=self.tar([('home/file',b'foo')]);with_limit=patch.object(b,'MAX_FILES',0)
        with with_limit:self.assertRaises(b.BackupError,b.scan_tar,p)

    def test_environment_metadata_preserves_concrete_alias_and_rejects_invalid_values(self):
        p=self.tar([('debian/rootfs/usr/bin/env',elf()),('debian/rootfs/etc/os-release',b'ID=debian')])
        manifest=self.root/'manifest.json'
        value=dict(format='termux-jobs-backup',version=1,components=[dict(file=p.name,kind='ubuntu',sha256=b.digest(p),bytes=p.stat().st_size,environmentAlias='debian')])
        manifest.write_text(json.dumps(value))
        report=self.inspect(manifest)
        self.assertEqual('debian',report['components'][0]['environmentAlias'])
        for invalid in ('../ubuntu','-bad',False,'a;b'):
            value['components'][0]['environmentAlias']=invalid
            manifest.write_text(json.dumps(value))
            self.assertRaises(b.BackupError,self.inspect,manifest)

    def test_restore_records_source_alias_without_replacing_it(self):
        p=self.tar([('debian/rootfs/usr/bin/env',elf()),('debian/rootfs/etc/os-release',b'ID=debian')])
        manifest=self.root/'manifest.json'
        manifest.write_text(json.dumps(dict(format='termux-jobs-backup',version=1,components=[dict(file=p.name,kind='ubuntu',sha256=b.digest(p),bytes=p.stat().st_size,environmentAlias='debian')])))
        with patch.object(b,'proot_modern',return_value=True),patch.object(b,'restore_ubuntu',return_value='jobs-abcdef123456') as create:
            report=self.inspect(manifest)
            result=b.restore(dict(self.req,path=str(manifest),fingerprints=report['fingerprints'],selected=[0],destination=str(self.home),work=str(self.work)))
        self.assertEqual({'debian':'jobs-abcdef123456'},result['environmentBindings'])
        self.assertEqual(['jobs-abcdef123456'],result['environments'])
        self.assertEqual('debian',create.call_args.args[1]['environmentAlias'])

    def test_restore_void_synthesizes_safe_manifest_and_preserves_rootfs(self):
        passwd=b'root:x:0:0:root:/root:/bin/bash\n'
        foreign=json.dumps(dict(image_ref='https://untrusted.invalid/image',image_config=dict(config=dict(Entrypoint=['evil'],Cmd=['evil'],Env=['LD_PRELOAD=evil'],WorkingDir='/evil')))).encode()
        p=self.tar([('void/rootfs/usr/bin/dash',elf()),('void/rootfs/bin',(tarfile.SYMTYPE,'usr/bin')),('void/rootfs/usr/bin/sh',(tarfile.SYMTYPE,'dash')),('void/rootfs/etc/passwd',passwd),('void/rootfs/etc/os-release',b'ID=void'),('void/manifest.json',foreign)])
        before=p.read_bytes();seen=[]
        def native(args):
            with tarfile.open(args[-1]) as tar:
                names=tar.getnames();alias=names[0].split('/')[0];meta=tar.getmember(alias+'/manifest.json');self.assertTrue(meta.isfile());self.assertEqual(meta.mode,0o600)
                config=json.load(tar.extractfile(meta));seen.append(config)
                self.assertEqual(config,dict(image_config=dict(config=dict(Entrypoint=None,Cmd=['/bin/sh'],WorkingDir='/',Env=['PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin']))))
                self.assertEqual(tar.extractfile(alias+'/rootfs/etc/passwd').read(),passwd)
                self.assertEqual(tar.getmember(alias+'/rootfs/bin').linkname,'usr/bin')
                self.assertNotIn('void/manifest.json',names)
        with patch.object(b,'run_tool',side_effect=native):
            alias=b.restore_ubuntu(p,dict(status='compatible',root='void/rootfs'),self.prefix,self.work)
        self.assertEqual(p.read_bytes(),before);self.assertEqual(len(seen),1);self.assertFalse((self.work/(alias+'.tar')).exists())

    def test_restore_ignores_foreign_manifest_symlink_and_rejects_rootfs_write_through_link(self):
        p=self.tar([('debian/rootfs/usr/bin/sh',elf()),('debian/rootfs/etc/os-release',b'ID=debian'),('debian/manifest.json',(tarfile.SYMTYPE,'/private/authorization'))])
        def native(args):
            with tarfile.open(args[-1]) as tar:
                members=[m for m in tar if m.name.endswith('/manifest.json')];self.assertEqual(len(members),1);self.assertTrue(members[0].isfile())
        with patch.object(b,'run_tool',side_effect=native) as tool:
            b.restore_ubuntu(p,dict(status='compatible',root='debian/rootfs'),self.prefix,self.work)
        self.assertEqual(tool.call_count,1)
        p=self.tar([('void/rootfs/etc',(tarfile.SYMTYPE,'/outside')),('void/rootfs/etc/passwd',b'bad')],name='unsafe.tar.gz')
        with patch.object(b,'run_tool') as tool:
            self.assertRaises(b.BackupError,b.restore_ubuntu,p,dict(status='compatible',root='void/rootfs'),self.prefix,self.work)
        tool.assert_not_called()

    def test_proot_rebase_regenerates_pax_paths_and_hardlinks_under_fresh_alias(self):
        p=self.root/'pax-long.tar';root='debian/rootfs';longname=root+'/var/lib/apt/lists/'+('repository-'*14)+'Packages';target=root+'/usr/share/'+('documentation-'*10)+'file'
        with tarfile.open(p,'w',format=tarfile.PAX_FORMAT) as tar:
            for name,payload in [(root+'/etc/os-release',b'ID=debian'),(longname,b'index'),(target,b'document')]:
                m=tarfile.TarInfo(name);m.size=len(payload);m.pax_headers={'path':name,'mtime':'123.5'};tar.addfile(m,io.BytesIO(payload))
            m=tarfile.TarInfo(root+'/usr/share/alias');m.type=tarfile.LNKTYPE;m.linkname=target;m.pax_headers={'linkpath':target};tar.addfile(m)
        original=p.read_bytes();seen=[]
        def native(args):
            with tarfile.open(args[-1]) as tar:
                rows=list(tar);aliases={m.name.split('/')[0] for m in rows};self.assertEqual(len(aliases),1);alias=next(iter(aliases));self.assertTrue(alias.startswith('jobs-'))
                for m in rows:
                    self.assertFalse(m.name.startswith('debian/'))
                    if m.islnk():self.assertTrue(m.linkname.startswith(alias+'/rootfs/'))
                self.assertEqual(tar.extractfile(alias+'/rootfs/'+longname.split('/rootfs/',1)[1]).read(),b'index')
                self.assertEqual(tar.extractfile(alias+'/rootfs/usr/share/alias').read(),b'document');seen.append(alias)
        with patch.object(b,'run_tool',side_effect=native):
            alias=b.restore_ubuntu(p,dict(status='compatible',root=root),self.prefix,self.work)
        self.assertEqual(seen,[alias]);self.assertEqual(p.read_bytes(),original)

    def test_alpine_busybox_establishes_architecture_without_following_host_symlinks(self):
        p=self.tar([('alpine/rootfs/bin/busybox',elf()),('alpine/rootfs/bin/sh',(tarfile.SYMTYPE,'/bin/busybox')),('alpine/rootfs/usr/bin/env',(tarfile.SYMTYPE,'/bin/busybox')),('alpine/rootfs/etc/os-release',b'ID=alpine')])
        with patch.object(b,'proot_modern',return_value=True),patch.object(b,'current_arch',return_value='aarch64'):
            self.assertEqual(self.inspect(p)['components'][0]['status'],'compatible')
        with patch.object(b,'proot_modern',return_value=True),patch.object(b,'current_arch',return_value='x86_64'):
            self.assertEqual(self.inspect(p)['components'][0]['status'],'blocked')
        p=self.tar([('alpine/rootfs/bin/busybox',b'not an ELF'),('alpine/rootfs/bin/sh',(tarfile.SYMTYPE,'/bin/busybox')),('alpine/rootfs/etc/os-release',b'ID=alpine')],name='unknown.tar.gz')
        with patch.object(b,'proot_modern',return_value=True):self.assertEqual(self.inspect(p)['components'][0]['status'],'blocked')

    def test_systemd_hex_escaped_filenames_are_literal_and_do_not_allow_traversal(self):
        name=r'usr/lib/systemd/system/system-systemd\x2dcryptsetup.slice'
        self.assertEqual(b.clean(name),name)
        p=self.tar([('home/'+name,b'unit'),(r'home/literal\x2f..\x2foutside',b'literal')])
        b.scan_tar(p);dest=self.home/'restored';b.extract_new(p,'home',dest)
        self.assertEqual((dest/name).read_bytes(),b'unit')
        self.assertEqual((dest/r'literal\x2f..\x2foutside').read_bytes(),b'literal')
        self.assertFalse((self.root/'outside').exists())
        for unsafe in [r'home\escape',r'home/..\evil',r'home/\x2gfile',r'home/\x2d/../../escape']:
            self.assertRaises(b.BackupError,b.clean,unsafe)

    def test_readonly_arch_root_gets_runtime_directory_without_changing_root_mode(self):
        p=self.root/'readonly-root.tar'
        with tarfile.open(p,'w') as tar:
            root=tarfile.TarInfo('archlinux/rootfs');root.type=tarfile.DIRTYPE;root.mode=0o555;tar.addfile(root)
            file=tarfile.TarInfo('archlinux/rootfs/etc/os-release');data=b'ID=arch';file.size=len(data);tar.addfile(file,io.BytesIO(data))
        def native(args):
            with tarfile.open(args[-1]) as tar:
                members=list(tar);alias=members[0].name.split('/')[0]
                self.assertEqual(tar.getmember(alias+'/rootfs').mode,0o555)
                runtime=[m for m in members if m.name==alias+'/rootfs/.l2s'];self.assertEqual(len(runtime),1);self.assertTrue(runtime[0].isdir());self.assertEqual(runtime[0].mode,0o700)
        with patch.object(b,'run_tool',side_effect=native):b.restore_ubuntu(p,dict(status='compatible',root='archlinux/rootfs'),self.prefix,self.work)

    def test_runtime_directory_cannot_be_a_foreign_file_or_symlink(self):
        for value in [b'not a directory',(tarfile.SYMTYPE,'/outside')]:
            p=self.tar([('arch/rootfs/etc/os-release',b'ID=arch'),('arch/rootfs/.l2s',value)])
            with patch.object(b,'run_tool') as tool:self.assertRaises(b.BackupError,b.restore_ubuntu,p,dict(status='compatible',root='arch/rootfs'),self.prefix,self.work)
            tool.assert_not_called()
        p=self.root/'runtime-dir.tar'
        with tarfile.open(p,'w') as tar:
            m=tarfile.TarInfo('arch/rootfs/.l2s');m.type=tarfile.DIRTYPE;m.mode=0o700;tar.addfile(m)
        def native(args):
            with tarfile.open(args[-1]) as tar:self.assertEqual(sum(m.name.endswith('/rootfs/.l2s') for m in tar),1)
        with patch.object(b,'run_tool',side_effect=native):b.restore_ubuntu(p,dict(status='compatible',root='arch/rootfs'),self.prefix,self.work)

    def restored_fixture(self, alias='jobs-abcdef123456', reported=True):
        self.req['files']=str(self.root/'private');Path(self.req['files']).mkdir(exist_ok=True)
        target=self.prefix/'var/lib/proot-distro/containers'/alias;(target/'rootfs').mkdir(parents=True)
        (target/'manifest.json').write_text('{}');(target/'rootfs/keep.txt').write_text('owned')
        if reported:
            report=self.home/'restored-20261019-120000-abcdef12';report.mkdir(exist_ok=True)
            (report/'RESTORE-RESULT.json').write_text(json.dumps(dict(environments=[alias],environmentBindings={'debian':alias})))
        return target

    def test_restored_removal_requires_provenance_and_fresh_identity(self):
        target=self.restored_fixture();row=b.restored_inventory(self.req)['environments'][0];self.assertTrue(row['confirmed']);self.assertEqual(row['source'],'debian')
        for alias,identity in [('ubuntu',row['identity']),('jobs-000000000000',row['identity']),('jobs-abcdef123456','changed')]:
            with patch.object(b,'run_tool') as tool:self.assertRaises(b.BackupError,b.remove_restored,dict(self.req,alias=alias,identity=identity))
            tool.assert_not_called();self.assertEqual((target/'rootfs/keep.txt').read_text(),'owned')
        unknown=self.restored_fixture('jobs-112233445566',False)
        row=next(r for r in b.restored_inventory(self.req)['environments'] if r['alias']==unknown.name);self.assertFalse(row['confirmed'])
        with patch.object(b,'run_tool') as tool:self.assertRaises(b.BackupError,b.remove_restored,dict(self.req,alias=unknown.name,identity=row['identity']))
        tool.assert_not_called()

    def test_native_lock_refusal_preserves_files_and_does_not_write_deleted_receipt(self):
        target=self.restored_fixture();row=b.restored_inventory(self.req)['environments'][0]
        with patch.object(b,'locked_remove_available',return_value=True),patch.object(b,'run_tool',side_effect=b.BackupError('PROOT_DISTRO_FAILED')) as tool:
            self.assertRaises(b.BackupError,b.remove_restored,dict(self.req,alias=target.name,identity=row['identity']))
        self.assertEqual(tool.call_args.args[0],[str(self.prefix/'bin/proot-distro'),'remove',target.name]);self.assertEqual((target/'rootfs/keep.txt').read_text(),'owned');self.assertIn(target.name,b.restored_sources(self.req))

    def test_successful_removal_retains_archives_reports_and_tombstones_old_receipts(self):
        target=self.restored_fixture();row=b.restored_inventory(self.req)['environments'][0];archive=self.root/'backup.tar';archive.write_bytes(b'archive')
        def native(args):self.assertEqual(args[1:],['remove',target.name]);shutil.rmtree(target)
        with patch.object(b,'locked_remove_available',return_value=True),patch.object(b,'run_tool',side_effect=native):
            self.assertEqual(b.remove_restored(dict(self.req,alias=target.name,identity=row['identity']))['removed'],target.name)
        self.assertEqual(archive.read_bytes(),b'archive');self.assertTrue(next(self.home.glob('restored-*/RESTORE-RESULT.json')).is_file());self.assertNotIn(target.name,b.restored_sources(self.req))
        target.mkdir();self.assertFalse(b.restored_inventory(self.req)['environments'][0]['confirmed'])

    def test_removal_refuses_missing_lock_support_and_symlink_container(self):
        target=self.restored_fixture();row=b.restored_inventory(self.req)['environments'][0]
        with patch.object(b,'run_tool') as tool:self.assertRaises(b.BackupError,b.remove_restored,dict(self.req,alias=target.name,identity=row['identity']))
        tool.assert_not_called();shutil.rmtree(target);os.symlink(self.home,target)
        self.assertRaises(b.BackupError,b.restored_target,self.prefix,target.name);self.assertEqual(b.restored_inventory(self.req)['environments'],[])

    def test_new_failed_restore_records_ownership_of_partial_copy_before_native_call(self):
        self.req['files']=str(self.root/'private');Path(self.req['files']).mkdir()
        archive=self.tar([('void/rootfs/bin/sh',elf()),('void/rootfs/etc/os-release',b'ID=void')])
        def failing_native(args):
            with tarfile.open(args[-1]) as tar:alias=next(iter(tar)).name.split('/')[0]
            (self.prefix/'var/lib/proot-distro/containers'/alias/'rootfs').mkdir(parents=True)
            raise b.BackupError('PROOT_DISTRO_FAILED')
        with patch.object(b,'run_tool',side_effect=failing_native):self.assertRaises(b.BackupError,b.restore_ubuntu,archive,dict(status='compatible',root='void/rootfs',alias='void'),self.prefix,self.work,self.req)
        row=b.restored_inventory(self.req)['environments'][0];self.assertTrue(row['confirmed']);self.assertTrue(row['incomplete']);self.assertEqual(row['source'],'void')


if __name__=='__main__':unittest.main()
