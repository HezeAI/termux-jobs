#!/usr/bin/env python3
"""Prepare the pinned offline model asset; never installs an Android package."""
from pathlib import Path
import argparse,hashlib,shutil,urllib.request,zipfile,tempfile
ROOT=Path(__file__).resolve().parent.parent
SHA='961d5ff98a17f4aa6de69864d0aa71fa5bac682301d2b5d17a3f24c5c99a46d4'
URL='https://alphacephei.com/vosk/models/vosk-model-small-ru-0.22.zip'
parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--source',type=Path);args=parser.parse_args()
target=ROOT/'app/src/main/assets/dictation-model-ru.zip'
if target.exists():
    assert hashlib.sha256(target.read_bytes()).hexdigest()==SHA,'Existing model asset SHA mismatch'
else:
    work=ROOT/'.verification/offline-dictation';work.mkdir(parents=True,exist_ok=True)
    source=args.source
    if source is None:
        with tempfile.NamedTemporaryFile(prefix='model-',suffix='.zip',dir=work,delete=False)as writer:
            source=Path(writer.name)
            with urllib.request.urlopen(URL,timeout=60)as reader:shutil.copyfileobj(reader,writer)
    assert hashlib.sha256(source.read_bytes()).hexdigest()==SHA,'Downloaded model SHA mismatch'
    with zipfile.ZipFile(source)as archive:assert archive.testzip() is None
    shutil.copyfile(source,target)
print(str(target),SHA)
