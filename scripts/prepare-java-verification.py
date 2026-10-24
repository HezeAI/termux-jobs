#!/usr/bin/env python3
"""Create a Java/resource verification copy; leave production JNI configuration intact."""
from pathlib import Path
import shutil
import re
import argparse

root = Path(__file__).resolve().parent.parent
parser = argparse.ArgumentParser(description=__doc__)
group = parser.add_mutually_exclusive_group()
group.add_argument('--arm64', action='store_true', help='stage the isolated ARM64 APK build')
group.add_argument('--modern-lint', action='store_true', help='stage AGP7.4.2 lint without changing the APK build toolchain')
args = parser.parse_args()
stage = root / ('.verification/arm64' if args.arm64 else
                '.verification/modern-lint' if args.modern_lint else '.verification/source')
stage.mkdir(parents=True, exist_ok=True)
(stage / '.tools/test-home').mkdir(parents=True, exist_ok=True)
x11_cache=stage/'.tools/x11-runtime'
if not x11_cache.exists():x11_cache.symlink_to(root/'.tools/x11-runtime',target_is_directory=True)
for name in ['app', 'terminal-emulator', 'terminal-view', 'termux-shared', 'x11', 'gradle']:
    # Stage source trees are disposable. Recreate them so deleted assets, native
    # payloads and resources cannot leak back into a later APK.
    staged_src = stage / name / 'src'
    if staged_src.exists():
        shutil.rmtree(staged_src)
    shutil.copytree(root / name, stage / name, dirs_exist_ok=True,
                    ignore=shutil.ignore_patterns('build', '.cxx', '.externalNativeBuild', 'bootstrap-*.zip', '__pycache__', '*.pyc'))
for name in ['settings.gradle', 'build.gradle', 'gradle.properties', 'gradlew', 'gradlew.bat']:
    shutil.copy2(root / name, stage / name)

def remove_block(text, name):
    while (match := re.search(r'\b' + name + r'\s*\{', text)):
        depth, end = 1, match.end()
        while depth:
            if text[end] == '{': depth += 1
            elif text[end] == '}': depth -= 1
            end += 1
        text = text[:match.start()] + text[end:]
    return text

for name in ['app', 'terminal-emulator', 'terminal-view', 'termux-shared', 'x11']:
    p = stage / name / 'build.gradle'
    text = remove_block(p.read_text(), 'externalNativeBuild')
    text = re.sub(r'^\s*ndkVersion\s*=.*$', '', text, flags=re.MULTILINE)
    text = text.replace('android {', 'android {\n    buildToolsVersion "30.0.3"', 1)
    p.write_text(text)
p = stage / 'gradle.properties'
# Match the production compile SDK; keep target/min SDK unchanged.
if args.modern_lint:
    p = stage / 'build.gradle'
    text = p.read_text()
    old = "classpath 'com.android.tools.build:gradle:4.2.2'"
    if text.count(old) != 1:
        raise RuntimeError('Review modern-lint staging after changing the APK AGP version')
    p.write_text(text.replace(old, "classpath 'com.android.tools.build:gradle:7.4.2'"))
    p = stage / 'app/build.gradle'
    p.write_text(p.read_text() + '\nandroid.lintOptions.checkDependencies = true\n')
print(stage)
