package com.termux.app.workspace;

import android.content.Context;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.shell.TermuxShellUtils;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/** OpenCode-only adapter; existing Codex profiles continue on their original path. */
public final class AiAgents {
    public static final String DEFAULT_CONNECTION="00000000-0000-4000-8000-000000000002";
    private static final String GUEST="/tmp/termux-jobs-ai";
    private AiAgents() {}
    static boolean validThread(String id) {return id!=null && id.matches("ses[A-Za-z0-9_-]{1,124}");}
    static boolean validModel(String model) {return model!=null && model.length()<=256 && (model.isEmpty() || model.matches("[A-Za-z0-9_-]+/[A-Za-z0-9_.:/-]+"));}
    static void prepareLaunch(WorkspaceStore.Session s) {
        s.aiSocketId=java.util.UUID.randomUUID().toString();
        if(!s.aiSavedThread.isEmpty()){s.aiThread=s.aiSavedThread;s.aiChat="resume";}
    }
    static File directory(File files) {return new File(files,".ai-control");}
    static File socket(File files,WorkspaceStore.Session s) {String run=s.aiSocketId.isEmpty()?s.id:s.aiSocketId;java.util.UUID.fromString(run);return new File(directory(files),run+".sock");}
    /** Capture before scheduling so a later launch cannot retarget an old action. */
    static WorkspaceStore.Session snapshot(WorkspaceStore.Session p){WorkspaceStore.Session s=p.copy();s.id=p.id;s.handle=p.handle;s.aiSocketId=p.aiSocketId;s.aiThread=p.aiThread;s.aiSavedThread=p.aiSavedThread;return s;}
    private static void protect(File f) throws IOException {
        if(!f.setReadable(false,false)||!f.setWritable(false,false)||!f.setExecutable(false,false)||!f.setReadable(true,true)||!f.setWritable(true,true)||(f.isDirectory()&&!f.setExecutable(true,true)))throw new IOException("Cannot protect AI controls");
    }
    public static void prepare(Context context) throws IOException {
        File dir=directory(context.getFilesDir());if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Cannot create AI controls");protect(dir);
        for(String asset:new String[]{"ai-agents-control.js","ai-agents-bootstrap.sh","agent-receipts.cjs","agent-attachments.cjs","opencode-permissions.cjs","opencode-chat.cjs","opencode-session-settings.cjs"}) {
        File bridge=new File(dir,asset.equals("ai-agents-bootstrap.sh")?"bootstrap-v1.sh":asset.equals("ai-agents-control.js")?"bridge-v1.js":asset);
        if(asset.endsWith(".bundle")&&bridge.isFile()&&bridge.length()>0)continue;
        File temporary=File.createTempFile("bridge-",".tmp",dir);
        try(InputStream in=context.getAssets().open(asset);OutputStream out=new FileOutputStream(temporary)) {byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
        protect(temporary);if(!temporary.renameTo(bridge)){temporary.delete();throw new IOException("Cannot replace AI bridge");}
        }
    }
    static String command(File files,WorkspaceStore.Session s) {
        if(!s.aiAgent.equals("opencode")||!s.environment.equals("ubuntu")||!AgentEnvironments.ubuntu(s.codexEnvironment)||!validThread(s.aiThread)||!validModel(s.aiModel))throw new IllegalArgumentException("Invalid AI profile");
        return "( set -e; export JOBS_AI_LANGUAGE="+(WorkspaceText.tr("Агент: ").startsWith("Агент")?"ru":"en")+"; source "+SessionShell.quote(GUEST+"/bootstrap-v1.sh")+"; exec node "+SessionShell.quote(GUEST+"/bridge-v1.js")+" run-chat "+SessionShell.quote(GUEST+"/connections/"+s.aiConnection)+" "+SessionShell.quote(GUEST+"/"+socket(files,s).getName())+" "+SessionShell.quote(s.aiThread)+" "+SessionShell.quote(s.aiModel)+" "+SessionShell.quote(s.aiMode)+" "+SessionShell.quote(s.aiChat)+" "+SessionShell.quote(s.aiVariant)+" "+SessionShell.quote(s.aiAccess)+" "+SessionShell.quote(s.aiProjectOnly?"project":"unrestricted")+" )";
    }
    static JSONObject liveRequest(Context c,WorkspaceStore.Session s,JSONObject request)throws Exception {
        java.util.UUID.fromString(s.id);if(!s.aiAgent.equals("opencode"))throw new IOException("Not OpenCode");
        try(android.net.LocalSocket client=new android.net.LocalSocket()){
            GeminiLiveControl.connect(client,socket(c.getFilesDir(),s).getAbsolutePath());client.setSoTimeout(35000);
            byte[] data=request.toString().getBytes(StandardCharsets.UTF_8);if(data.length>65536)throw new IOException("Request too large");
            client.getOutputStream().write(data);client.getOutputStream().flush();client.shutdownOutput();
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;
            while((n=client.getInputStream().read(b))!=-1){if(out.size()+n>524288)throw new IOException("Response too large");out.write(b,0,n);}
            JSONObject value=new JSONObject(out.toString("UTF-8"));if(!value.optBoolean("ok"))throw new IOException("OpenCode request failed");return value;
        }
    }
    static JSONObject call(Context c,String connection,WorkspaceStore.Session live,JSONObject request) throws Exception {
        java.util.UUID.fromString(connection);prepare(c);
        String args=live==null||!live.aiAgent.equals("opencode")?"request "+SessionShell.quote(GUEST+"/connections/"+connection):"proxy "+SessionShell.quote(GUEST+"/"+socket(c.getFilesDir(),live).getName());
        String script="exec proot-distro login --bind "+SessionShell.quote(directory(c.getFilesDir()).getAbsolutePath()+":"+GUEST)+" "+SessionShell.quote(live==null?AgentEnvironments.defaultUbuntu():AgentEnvironments.alias(live))+" -- /bin/bash --noprofile --norc -c "+SessionShell.quote("export PATH=\"$HOME/.local/share/termux-jobs/agents/bin:$HOME/.local/share/termux-jobs/agents/node-v22.22.1-linux-arm64/bin:$HOME/.local/share/termux-jobs/agents/node_modules/.bin:$PATH\"; exec node "+SessionShell.quote(GUEST+"/bridge-v1.js")+" "+args);
        ProcessBuilder pb=new ProcessBuilder(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH+"/bash","--noprofile","--norc","-c",script);
        pb.directory(new File(TermuxConstants.TERMUX_HOME_DIR_PATH));pb.environment().clear();
        for(String v:TermuxShellUtils.buildEnvironment(c,false,TermuxConstants.TERMUX_HOME_DIR_PATH)){int i=v.indexOf('=');if(i>0)pb.environment().put(v.substring(0,i),v.substring(i+1));}
        pb.redirectError(new File("/dev/null"));Process p=pb.start();
        java.util.concurrent.ScheduledExecutorService timer=java.util.concurrent.Executors.newSingleThreadScheduledExecutor();timer.schedule(p::destroy,65,TimeUnit.SECONDS);
        try {
            try(OutputStream stdin=p.getOutputStream()){stdin.write(request.toString().getBytes(StandardCharsets.UTF_8));}
            ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=p.getInputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>2*1024*1024)throw new IOException("AI response too large");out.write(b,0,n);}}
            JSONObject result=new JSONObject(out.toString("UTF-8"));if(!result.optBoolean("ok"))throw new IOException("OpenCode unavailable");return result;
        } finally {p.destroy();timer.shutdownNow();}
    }
}
