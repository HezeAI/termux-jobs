#!/usr/bin/env python3
"""Private executable shim: native arguments unchanged, fixture XDG only."""
from pathlib import Path
import os,sys,json,hashlib
root=Path(sys.argv[1]).resolve();meta=json.loads((root/'identity.json').read_text());binary=Path(meta['binary']);assert hashlib.sha256(binary.read_bytes()).hexdigest()==meta['binarySha256']
env={k:os.environ[k]for k in ['PATH','OPENCODE_SERVER_PASSWORD','TERM','COLORTERM','LANG','AUDIT_API_KEY']if k in os.environ}
env.update(HOME=str(root/'home'),TMPDIR=str(root/'tmp'),XDG_CONFIG_HOME=str(root/'xdg/config'),XDG_DATA_HOME=str(root/'xdg/data'),XDG_CACHE_HOME=str(root/'xdg/cache'),XDG_STATE_HOME=str(root/'xdg/state'),OPENCODE_DISABLE_AUTOUPDATE='true',OPENCODE_DISABLE_MODELS_FETCH='true',GIT_CONFIG_GLOBAL='/dev/null',GIT_CONFIG_NOSYSTEM='1',GIT_CEILING_DIRECTORIES=str(root))
os.execve(binary,[str(binary),*sys.argv[2:]],env)
