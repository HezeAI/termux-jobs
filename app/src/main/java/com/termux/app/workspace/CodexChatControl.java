package com.termux.app.workspace;

import android.content.Context;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** New-run Codex chat transport; existing terminal sessions retain their immutable bridge. */
final class CodexChatControl {
    static File marker(File files,WorkspaceStore.Session p){return new File(CodexSessionShell.socketDirectory(files),p.codexSocketId+".android-chat");}
    static boolean enabled(File files,WorkspaceStore.Session p){return p!=null&&p.environment.equals("codex")&&AgentEnvironments.ubuntu(p.codexEnvironment)&&CodexSessionShell.validThread(p.codexSocketId)&&marker(files,p).isFile();}
    static void prepare(Context c,WorkspaceStore.Session p)throws IOException{
        if(!p.environment.equals("codex")||!AgentEnvironments.ubuntu(p.codexEnvironment))return;
        if(!CodexSessionShell.validThread(p.codexSocketId))throw new IOException("Invalid Codex run");
        File dir=CodexSessionShell.socketDirectory(c.getFilesDir());if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Cannot create Codex controls");
        for(String asset:new String[]{"codex-chat.cjs","codex-control.js","agent-receipts.cjs"}){
            String name=asset.equals("codex-control.js")?"codex-wire-v0.2.22.js":asset.equals("codex-chat.cjs")?"codex-chat-v1.cjs":asset;
            File temp=File.createTempFile("codex-chat-",".tmp",dir);try{try(InputStream in=c.getAssets().open(asset);OutputStream out=new FileOutputStream(temp)){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}protect(temp);if(!temp.renameTo(new File(dir,name)))throw new IOException("Cannot publish Codex chat helper");}finally{temp.delete();}
        }
        try(FileOutputStream out=new FileOutputStream(marker(c.getFilesDir(),p))){out.write(1);}protect(marker(c.getFilesDir(),p));
    }
    static void protect(File f)throws IOException{if(!f.setReadable(false,false)||!f.setWritable(false,false)||!f.setReadable(true,true)||!f.setWritable(true,true))throw new IOException("Cannot protect Codex chat file");}
    static String command(File files,WorkspaceStore.Session p,String endpoint){
        String home=AgentEnvironments.ubuntu(p.codexEnvironment)?"/root":com.termux.shared.termux.TermuxConstants.TERMUX_HOME_DIR_PATH;
        String cwd=p.directory.isEmpty()||p.directory.equals("~")?home:p.directory;if(cwd.startsWith("~/"))cwd=home+cwd.substring(1);
        return "node "+SessionShell.quote("/tmp/termux-jobs-codex/codex-chat-v1.cjs")+" run "+SessionShell.quote(endpoint)+" "+SessionShell.quote(p.codexAccess)+" "+SessionShell.quote(p.codexChat)+" "+SessionShell.quote(p.codexThread)+" "+SessionShell.quote(cwd)+" "+SessionShell.quote(p.codexSourceDirectory)+" "+SessionShell.quote(CodexSessionShell.approval(p))+" "+SessionShell.quote(WorkspaceText.tr("Агент: ").startsWith("Агент")?"ru":"en");
    }
    static JSONObject request(File files,WorkspaceStore.Session p,JSONObject q)throws Exception{
        if(!enabled(files,p))return new JSONObject().put("ok",true).put("nativeChat",false);
        File socket=new File(CodexSessionShell.socket(files,p).getAbsolutePath()+".chat");
        try(LocalSocket client=new LocalSocket()){
            client.connect(new LocalSocketAddress(socket.getAbsolutePath(),LocalSocketAddress.Namespace.FILESYSTEM));client.setSoTimeout(15000);
            byte[] bytes=(q.toString()+"\n").getBytes(StandardCharsets.UTF_8);if(bytes.length>65536)throw new IOException("REQUEST_TOO_LARGE");client.getOutputStream().write(bytes);client.getOutputStream().flush();
            JSONObject r=new JSONObject(ClaudeLiveControl.readReply(client.getInputStream(),524288));if(!r.optBoolean("ok"))throw new IOException("Codex chat did not confirm action");return r;
        }
    }
}
