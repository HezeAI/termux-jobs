#!/usr/bin/env python3
"""Create public release metadata from the final verified APK, never from a guessed hash."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import zipfile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apk', type=Path, required=True)
    parser.add_argument('--aapt2', required=True)
    parser.add_argument('--notes', type=Path, required=True, help='JSON object with ru/en arrays')
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    badging = subprocess.check_output([args.aapt2, 'dump', 'badging', str(args.apk)], text=True)
    package = re.search(r"package: name='([^']+)' versionCode='([0-9]+)' versionName='([^']+)'", badging)
    sdk = re.search(r"(?:minSdkVersion|sdkVersion):'([0-9]+)'", badging)
    if not package or not sdk:
        raise ValueError('Cannot read APK metadata')
    package_id, code, version = package.groups()
    if package_id != 'com.termux' or not re.fullmatch(r'\d+\.\d+\.\d+', version):
        raise ValueError('Unexpected package/version')
    expected = f'Termux-Jobs-v{version}-arm64.apk'
    if args.apk.name != expected:
        raise ValueError(f'APK must be named {expected}')
    with zipfile.ZipFile(args.apk) as archive:
        if archive.testzip():
            raise ValueError('APK CRC failure')
        abis = {n.split('/')[1] for n in archive.namelist() if n.startswith('lib/')}
        if abis != {'arm64-v8a'}:
            raise ValueError('Only ARM64 releases are supported')
    notes = json.loads(args.notes.read_text())
    for language in ('ru', 'en'):
        lines = notes[language]
        compatibility = notes['compatibility'][language]
        if not isinstance(compatibility, list) or not 1 <= len(compatibility) <= 5:
            raise ValueError('Explicit localized compatibility notes required')
        lines += [('Совместимость: ' if language == 'ru' else 'Compatibility: ') + item for item in compatibility]
        if not isinstance(lines, list) or not 1 <= len(lines) <= 20 or any(not isinstance(s, str) or not s.strip() or len(s) > 1000 for s in lines):
            raise ValueError('Invalid localized release notes')
        notes[language] = lines
    value = dict(schema=1, packageId=package_id, versionCode=int(code), versionName=version,
                 minSdk=int(sdk.group(1)), apk=dict(abi='arm64-v8a',
                 url=f'https://github.com/HezeAI/termux-jobs/releases/download/v{version}/{expected}',
                 size=args.apk.stat().st_size, sha256=hashlib.sha256(args.apk.read_bytes()).hexdigest()), changes={language: notes[language] for language in ('ru', 'en')}, compatibility=notes['compatibility'])
    args.output.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n')
    print(f'Created {args.output}: v{version}, code {code}')


if __name__ == '__main__':
    main()
