#!/usr/bin/env python3
"""Check app-owned locale resources without modifying source or Android state."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

root=Path(__file__).resolve().parents[1]
def strings(path):
    entries=ET.parse(path).getroot()
    result={item.attrib['name']:''.join(item.itertext()) for item in entries if item.tag=='string'}
    assert len(result)==len(entries),f'Duplicate keys or unsupported entries: {path}'
    return result

en=strings(root/'app/src/main/res/values/workspace_strings.xml')
ru=strings(root/'app/src/main/res/values-ru/workspace_strings.xml')
assert en.keys()==ru.keys(),'Workspace locale keys differ'
source=(root/'app/src/main/java/com/termux/app/workspace/WorkspaceText.java').read_text()
ids=re.findall(r'ids\.put\(.*?, R\.string\.(workspace_[a-z0-9_]+)\);',source)
assert len(ids)==len(set(ids))==len(en),'Workspace text map differs from resources'
assert set(ids)==set(en),'Workspace resource key is missing from text map'
for name,value in en.items():
    if name not in {'workspace_text_404','workspace_text_406'}:
        assert not re.search('[А-Яа-яЁё]',value),f'Untranslated English resource: {name}'
format_token=re.compile(r'%(?:\d+\$)?[sdif]')
for module in ['app','termux-shared']:
    default=strings(root/f'{module}/src/main/res/values/strings.xml')
    russian=strings(root/f'{module}/src/main/res/values-ru/strings.xml')
    assert default.keys()==russian.keys(),f'{module}: locale keys differ'
    for name,value in default.items():
        assert sorted(format_token.findall(value))==sorted(format_token.findall(russian[name])),f'{module}: format arguments differ for {name}'
print(f'PASS: {len(en)} workspace strings, all app/shared settings keys and format arguments')
