package com.termux.app.workspace;

import android.content.Context;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.shell.TermuxShellUtils;
import java.io.*;
import java.util.concurrent.TimeUnit;

/** Independent native Claude CLI; no access to another agent's control plane. */
final class ClaudeCode {
    static final String GUEST="/tmp/termux-jobs-claude";
    private ClaudeCode() {}
    static File directory(File files) {return new File(files,".claude-control");}
    static boolean validThread(String id) {return id!=null && id.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");}
    static boolean valid(WorkspaceStore.Session s) {
        return s.claudeAgent && s.environment.equals("ubuntu") && AgentEnvironments.ubuntu(s.codexEnvironment) && s.aiAgent.isEmpty()
            && java.util.Arrays.asList("","sonnet","opus","haiku").contains(s.claudeModel)
            && java.util.Arrays.asList("manual","acceptEdits","plan").contains(s.claudeMode)
            && java.util.Arrays.asList("default","low","medium","high","xhigh","max").contains(s.claudeEffort)
            && java.util.Arrays.asList("new","choose","id").contains(s.claudeChat)
            && validThread(s.claudeThread) && validThread(s.claudeSocketId);
    }
    static void prepare(Context context) throws IOException {
        File dir=directory(context.getFilesDir());
        if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Cannot create Claude controls");
        protect(dir);
        for(String asset:new String[]{"claude-bootstrap.sh","claude-control.js","claude-control-v2.js","claude-chat.cjs","claude-native-hook.cjs","agent-receipts.cjs","agent-attachments.cjs"}) {
            File temporary=File.createTempFile("claude-",".tmp",dir);
            try {
                try(InputStream in=context.getAssets().open(asset);OutputStream out=new FileOutputStream(temporary)) {
                    byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);
                }
                protect(temporary);
                if(!temporary.renameTo(new File(dir,asset)))throw new IOException("Cannot replace Claude controls");
            } finally {temporary.delete();}
        }
    }
    private static void protect(File f) throws IOException {
        if(!f.setReadable(false,false)||!f.setWritable(false,false)||!f.setExecutable(false,false)
            ||!f.setReadable(true,true)||!f.setWritable(true,true)||(f.isDirectory()&&!f.setExecutable(true,true)))throw new IOException("Cannot protect Claude controls");
    }
    static String command(File files,WorkspaceStore.Session s) {
        if(!valid(s))throw new IllegalArgumentException("Invalid Claude profile");
        return "( set -e; export JOBS_CLAUDE_LANGUAGE="+(WorkspaceText.tr("Агент: ").startsWith("Агент")?"ru":"en")
            +"; export JOBS_CLAUDE_EFFORT="+SessionShell.quote(s.claudeEffort)
            +"; source "+SessionShell.quote(GUEST+"/claude-bootstrap.sh")
            +"; exec \"$JOBS_CLAUDE_NODE\" "+SessionShell.quote(GUEST+"/claude-control-v2.js")+" run "
            +SessionShell.quote(GUEST+"/"+s.claudeSocketId+".sock")+" "+SessionShell.quote(s.claudeModel)+" "+SessionShell.quote(s.claudeMode)+" "+SessionShell.quote(s.claudeChat)+" "+SessionShell.quote(s.claudeThread)+" )";
    }
    private static WorkspaceStore.Session profile(String environment){WorkspaceStore.Session p=new WorkspaceStore.Session();p.codexEnvironment=environment;return p;}
    static void stop(Context context,String runId)throws Exception {stop(context,runId,"ubuntu");}
    /** Caller captures the unique run ID before scheduling; a later run cannot be stopped. */
    static void stop(Context context,String runId,String environment) throws Exception {
        if(!validThread(runId))throw new IllegalArgumentException("Invalid Claude run");
        String guest="node=$(cat \"$HOME/.local/share/termux-jobs/claude-code/controller-node\") && exec \"$node\" "
            +SessionShell.quote(GUEST+"/claude-control-v2.js")+" stop "+SessionShell.quote(GUEST+"/"+runId+".sock");
        String script="exec proot-distro login --bind "+SessionShell.quote(directory(context.getFilesDir()).getAbsolutePath()+":"+GUEST)
            +" "+SessionShell.quote(AgentEnvironments.alias(profile(environment)))+" -- /bin/bash --noprofile --norc -c "+SessionShell.quote(guest);
        ProcessBuilder pb=new ProcessBuilder(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH+"/bash","--noprofile","--norc","-c",script);
        pb.directory(new File(TermuxConstants.TERMUX_HOME_DIR_PATH));pb.environment().clear();
        for(String entry:TermuxShellUtils.buildEnvironment(context,false,TermuxConstants.TERMUX_HOME_DIR_PATH)) {
            int i=entry.indexOf('=');if(i>0)pb.environment().put(entry.substring(0,i),entry.substring(i+1));
        }
        pb.redirectOutput(new File("/dev/null"));pb.redirectError(new File("/dev/null"));
        Process p=pb.start();try {if(!p.waitFor(7,TimeUnit.SECONDS))throw new IOException("Claude stop timeout");}
        finally {p.destroy();}
    }
}
