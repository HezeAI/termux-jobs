package com.termux.app.workspace;

import com.termux.shared.termux.TermuxConstants;
import java.util.ArrayDeque;
import java.util.Deque;
import org.json.JSONObject;

/** Project association is a directory plus an environment, not a copied transcript. */
final class CodexProjects {
    static String directory(String environment,String path) {
        String home=AgentEnvironments.ubuntu(environment)?"/root":TermuxConstants.TERMUX_HOME_DIR_PATH;
        if(path==null||path.isEmpty()||path.equals("~"))path=home;
        else if(path.startsWith("~/"))path=home+path.substring(1);
        if(!path.startsWith("/")||path.indexOf('\0')>=0||path.contains("\n")||path.contains("\r"))return "";
        Deque<String> parts=new ArrayDeque<>();for(String part:path.split("/")){if(part.isEmpty()||part.equals("."))continue;if(part.equals("..")){if(!parts.isEmpty())parts.removeLast();}else parts.addLast(part);}
        return "/"+android.text.TextUtils.join("/",parts);
    }
    static boolean matches(String environment,String expected,String actual) {String a=directory(environment,expected);return !a.isEmpty()&&a.equals(directory(environment,actual));}
    static boolean bind(WorkspaceStore.Session profile,CodexStatus status) {
        if(profile.codexSocketId.equals(profile.codexBindingBlockedRun)||Boolean.FALSE.equals(status.materialized)||!profile.environment.equals("codex")||!profile.codexChat.equals("new")||!status.error.isEmpty()||!CodexSessionShell.validThread(status.thread)||status.cwd.isEmpty()||!matches(profile.codexEnvironment,profile.directory,status.cwd))return false;
        profile.codexSourceDirectory=status.cwd;profile.codexChat="id";profile.codexThread=status.thread;profile.codexThreadName=status.title.isEmpty()?profile.name:status.title;return true;
    }
}
