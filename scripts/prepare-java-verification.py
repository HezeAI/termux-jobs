#!/usr/bin/env python3
"""Create a Java/resource verification copy; leave production JNI configuration intact."""
from pathlib import Path
import shutil
import re
import sys

root = Path(__file__).resolve().parent.parent
stage = root / ('.verification/arm64' if '--arm64' in sys.argv else '.verification/source')
stage.mkdir(parents=True, exist_ok=True)
(stage / '.tools/test-home').mkdir(parents=True, exist_ok=True)
for name in ['app', 'terminal-emulator', 'terminal-view', 'termux-shared', 'gradle']:
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

for name in ['app', 'terminal-emulator', 'terminal-view', 'termux-shared']:
    p = stage / name / 'build.gradle'
    text = remove_block(p.read_text(), 'externalNativeBuild')
    text = re.sub(r'^\s*ndkVersion\s*=.*$', '', text, flags=re.MULTILINE)
    text = text.replace('android {', 'android {\n    buildToolsVersion "30.0.3"', 1)
    p.write_text(text)
p = stage / 'gradle.properties'
# Match the production compile SDK; keep target/min SDK unchanged.
print(stage)
