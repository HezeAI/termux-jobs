package com.termux.app.workspace;

import android.content.Context;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.shell.TermuxShellUtils;
import java.io.*;
import java.util.concurrent.TimeUnit;

/** Independent Gemini CLI; no access to another agent's control plane. */
final class GeminiCli {
    static final String GUEST="/tmp/termux-jobs-gemini";
    private GeminiCli() {}
    static File directory(File files) {return new File(files,".gemini-control");}
    static boolean validThread(String id) {return id!=null && id.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");}
    static boolean valid(WorkspaceStore.Session s) {
        return s.geminiAgent && s.environment.equals("ubuntu") && s.aiAgent.isEmpty() && !s.claudeAgent
            && java.util.Arrays.asList("auto","google","api").contains(s.geminiAuth)
            && java.util.Arrays.asList("","auto","flash","pro").contains(s.geminiModel)
            && java.util.Arrays.asList("default","auto_edit","plan").contains(s.geminiMode)
            && java.util.Arrays.asList("new","choose","id").contains(s.geminiChat)
            && validThread(s.geminiThread) && validThread(s.geminiSocketId);
    }
    static void prepare(Context context) throws IOException {
        File dir=directory(context.getFilesDir());
        if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Cannot create Gemini controls");
        protect(dir);
        for(String asset:new String[]{"gemini-bootstrap.sh","gemini-control.js"}) {
            File temporary=File.createTempFile("gemini-",".tmp",dir);
            try {
                try(InputStream in=context.getAssets().open(asset);OutputStream out=new FileOutputStream(temporary)) {
                    byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);
                }
                protect(temporary);
                if(!temporary.renameTo(new File(dir,asset)))throw new IOException("Cannot replace Gemini controls");
            } finally {temporary.delete();}
        }
    }
    private static void protect(File f) throws IOException {
        if(!f.setReadable(false,false)||!f.setWritable(false,false)||!f.setExecutable(false,false)
            ||!f.setReadable(true,true)||!f.setWritable(true,true)||(f.isDirectory()&&!f.setExecutable(true,true)))throw new IOException("Cannot protect Gemini controls");
    }
    static String command(File files,WorkspaceStore.Session s) {
        if(!valid(s))throw new IllegalArgumentException("Invalid Gemini profile");
        return "( set -e; export JOBS_GEMINI_LANGUAGE="+(WorkspaceText.tr("Агент: ").startsWith("Агент")?"ru":"en")
            +"; export JOBS_GEMINI_DEBUG="+(s.extendedDebug?"1":"0")
            +"; source "+SessionShell.quote(GUEST+"/gemini-bootstrap.sh")
            +"; exec \"$JOBS_GEMINI_NODE\" "+SessionShell.quote(GUEST+"/gemini-control.js")+" run "
            +SessionShell.quote(GUEST+"/"+s.geminiSocketId+".sock")+" "+SessionShell.quote(s.geminiModel)+" "+SessionShell.quote(s.geminiMode)+" "+SessionShell.quote(s.geminiChat)+" "+SessionShell.quote(s.geminiThread)+" "+SessionShell.quote(s.geminiAuth)+" )";
    }
    /** Caller captures the unique run ID before scheduling; a later run cannot be stopped. */
    static void stop(Context context,String runId) throws Exception {
        if(!validThread(runId))throw new IllegalArgumentException("Invalid Gemini run");
        String guest="node=$(cat \"$HOME/.local/share/termux-jobs/gemini-cli/controller-node\") && exec \"$node\" "
            +SessionShell.quote(GUEST+"/gemini-control.js")+" stop "+SessionShell.quote(GUEST+"/"+runId+".sock");
        String script="exec proot-distro login --bind "+SessionShell.quote(directory(context.getFilesDir()).getAbsolutePath()+":"+GUEST)
            +" ubuntu -- /bin/bash --noprofile --norc -c "+SessionShell.quote(guest);
        ProcessBuilder pb=new ProcessBuilder(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH+"/bash","--noprofile","--norc","-c",script);
        pb.directory(new File(TermuxConstants.TERMUX_HOME_DIR_PATH));pb.environment().clear();
        for(String entry:TermuxShellUtils.buildEnvironment(context,false,TermuxConstants.TERMUX_HOME_DIR_PATH)) {
            int i=entry.indexOf('=');if(i>0)pb.environment().put(entry.substring(0,i),entry.substring(i+1));
        }
        pb.redirectOutput(new File("/dev/null"));pb.redirectError(new File("/dev/null"));
        Process p=pb.start();try {if(!p.waitFor(7,TimeUnit.SECONDS))throw new IOException("Gemini stop timeout");}
        finally {p.destroy();}
    }
}
