#!/usr/bin/env python3
"""Reject incomplete modern-lint runs instead of reporting a partial zero-error result."""
import argparse
import collections
import json
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

INFRA_FAILURES = (
    'Failed to load custom lint', 'Could not load custom lint',
    'incompatible version of Kotlin', 'ClassNotFoundException', 'NoClassDefFoundError',
    'Unexpected failure during lint analysis', 'Lint found an issue registry',
)
MODULES = ('app', 'terminal-emulator', 'terminal-view', 'termux-shared', 'x11')


def assess(xml, log, gradle_exit_code=0):
    root = ET.fromstring(xml)
    if root.tag != 'issues' or root.get('by') != 'lint 7.4.2':
        raise ValueError('Unexpected lint engine/report format; review the pinned verification toolchain')
    issues = list(root.findall('issue'))
    diagnostics = [pattern for pattern in INFRA_FAILURES if pattern in log]
    scopes = {module: f':{module}:lintAnalyzeRelease' in log for module in MODULES}
    errors = [i for i in issues if i.get('severity') in ('Error', 'Fatal') or i.get('id') == 'LintError']
    return {
        'engine': root.get('by'),
        'gradleExitCode': gradle_exit_code,
        'severity': dict(collections.Counter(i.get('severity') for i in issues)),
        'ids': dict(collections.Counter(i.get('id') for i in issues)),
        'moduleAnalysis': scopes,
        'infrastructureDiagnostics': diagnostics,
        'compileSdkBeyondAgpTestedWarning': 'was tested up to compileSdk' in log,
        'passed': gradle_exit_code == 0 and not diagnostics and not errors and all(scopes.values()),
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--xml', type=Path, required=True)
    parser.add_argument('--log', type=Path, required=True)
    parser.add_argument('--out', type=Path, required=True)
    parser.add_argument('--gradle-exit-code', type=int, default=0)
    args = parser.parse_args()
    args.out.unlink(missing_ok=True)
    try:
        result = assess(args.xml.read_text(), args.log.read_text(), args.gradle_exit_code)
    except (OSError, ValueError, ET.ParseError) as error:
        print(f'Lint verification unavailable: {error}', file=sys.stderr)
        return 1
    args.out.write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps(result, sort_keys=True))
    return 0 if result['passed'] else 1


if __name__ == '__main__':
    raise SystemExit(main())
