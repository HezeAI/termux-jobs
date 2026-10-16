#!/usr/bin/env python3
"""Inventory DEX definitions/references, and reject missing required file APIs.
Type presence is not method linkage, reflection, API-level or device verification.
"""
import argparse, hashlib, json, struct, zipfile
from pathlib import Path

def dex_types(data):
    def u32(offset): return struct.unpack_from('<I', data, offset)[0]
    strings=[]
    for i in range(u32(56)):
        p=u32(u32(60)+4*i)
        while data[p]&128: p+=1
        p+=1
        strings.append(data[p:data.index(b'\0',p)].decode('utf-8',errors='replace'))
    types=[strings[u32(u32(68)+4*i)] for i in range(u32(64))]
    defined={types[u32(u32(100)+32*i)] for i in range(u32(96))}
    referenced={t.lstrip('[') for t in types if t.lstrip('[').startswith('L')}
    return defined,referenced

def inspect(apk,android_jar):
    defined=set();referenced=set()
    with zipfile.ZipFile(apk) as z:
        for name in z.namelist():
            if name.startswith('classes') and name.endswith('.dex'):
                d,r=dex_types(z.read(name));defined.update(d);referenced.update(r)
    with zipfile.ZipFile(android_jar) as z:
        platform={'L'+name[:-6]+';' for name in z.namelist() if name.endswith('.class')}
    required={'Lcom/google/common/io/MoreFiles;','Lcom/google/common/io/RecursiveDeleteOption;',
              'Lcom/google/common/base/Strings;','Lcom/google/common/collect/ImmutableBiMap;',
              'Lorg/apache/commons/io/FileUtils;'}
    missing=sorted(required-defined)
    unresolved=sorted(referenced-defined-platform)
    optional=json.loads(Path(__file__).with_name('apk-runtime-optional-types.json').read_text())
    unreviewed=sorted(set(unresolved)-set(optional)-required)
    return {'apk':str(apk),'sha256':hashlib.sha256(Path(apk).read_bytes()).hexdigest(),
            'definedClasses':len(defined),'referencedClasses':len(referenced),
            'requiredMissing':missing,'unresolvedAgainstCompileSdk':unresolved,
            'unreviewedMissingTypes':unreviewed,
            'reviewedOptionalTypes':{t:optional[t] for t in unresolved if t in optional},
            'limitations':'Inventory includes annotations and optional APIs; presence alone does not prove method, reflection or Android API compatibility.'}

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--apk',required=True);p.add_argument('--android-jar',required=True);p.add_argument('--output',required=True);args=p.parse_args()
    report=inspect(args.apk,args.android_jar);Path(args.output).write_text(json.dumps(report,indent=2)+'\n')
    print('Defined classes:',report['definedClasses'],'unresolved types:',len(report['unresolvedAgainstCompileSdk']))
    if report['requiredMissing']:raise SystemExit('Missing required runtime definitions: '+', '.join(report['requiredMissing']))
    if report['unreviewedMissingTypes']:raise SystemExit('Unreviewed missing types: '+', '.join(report['unreviewedMissingTypes']))
    print('Required runtime definitions verified; all remaining missing types have explicit review notes')
