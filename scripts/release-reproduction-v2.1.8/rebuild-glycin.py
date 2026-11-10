#!/usr/bin/env python3
"""Rebuild the historical GCC shim from the v2.1.8 source, never from an APK."""
from pathlib import Path
import hashlib, json, shutil, subprocess
out=Path.cwd()/'.verification/release-reproduction-v2.1.8'
source=out/'source/x11/src/main/cpp/x11-fedora-glycin.c'
command=['cc','-fPIC','-shared','-nostdlib','-O2','-Wall','-Wextra','-Werror','-Wl,-z,now,-z,relro,-z,noexecstack','-Wl,-soname,x11-fedora-glycin.so',str(source),'-o',str(out/'glycin-gcc.so')]
subprocess.run(command,check=True)
result=out/'glycin-gcc.so'
sha=hashlib.sha256(result.read_bytes()).hexdigest()
if sha!='1caad510d1f79e0d7823621191c294b64f3e105a228ccc23e7a1ab5631ddac61':
 raise SystemExit('GCC result differs; retain it for investigation, do not silently substitute another binary')
(out/'glycin-historical-recipe.json').write_text(json.dumps(dict(argv=command,sha256=sha),indent=2)+'\n')
shutil.copyfile(result,out/'source/.tools/x11-runtime/assets/x11-fedora-glycin.so')
print('Historical GCC recipe rebuilt and staged; published APK was not a build input')
