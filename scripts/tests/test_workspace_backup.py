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


if __name__=='__main__':unittest.main()
