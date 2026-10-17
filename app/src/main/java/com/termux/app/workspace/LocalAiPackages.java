package com.termux.app.workspace;

import org.json.JSONObject;

/** Explicit user-confirmed installation of a small, fixed APT tool set. */
final class LocalAiPackages {
    static boolean known(String name){return name!=null&&name.matches("coreutils|findutils|file|unzip|zip|binutils|aapt|apksigner|python|python3|python3-pip|python3-venv|git|make|cmake|clang|build-essential|pkg-config|curl|wget");}
    static String script(JSONObject request,boolean plan)throws Exception{String name=request.getString("package");if(!known(name))throw new SecurityException("Unsupported tool package");return "command -v apt-get >/dev/null || { printf 'MISSING_TOOL: apt-get; installation is unavailable in this environment\\n'; exit 127; }; export DEBIAN_FRONTEND=noninteractive; exec apt-get "+(plan?"-s":"-y -o DPkg::Lock::Timeout=15 -o Acquire::Retries=0 -o Acquire::http::Timeout=30 -o Acquire::https::Timeout=30 -o Dpkg::Options::=--force-confold")+" --no-remove install -- "+SessionShell.quote(name);}
    static String hint(String result){if(result.contains("realpath: command not found")||result.contains("stat: command not found")||result.contains("readlink: command not found"))return "\nMISSING_TOOL: coreutils. If confirm-actions is enabled, request install package coreutils for a user-confirmed APT plan.";if(result.contains("find: command not found")||result.contains("find: unrecognized")||result.contains("find: unknown"))return "\nMISSING_TOOL: findutils. If confirm-actions is enabled, request install package findutils for a user-confirmed APT plan.";return "";}
}
