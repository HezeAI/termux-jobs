package com.termux.app.workspace;

import android.app.Activity;
import com.termux.shared.shell.TermuxShellUtils;
import com.termux.shared.termux.TermuxConstants;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** Own Python worker only; never writes input to, or stops, a user's terminal session. */
final class WorkspaceBackupEngine {
    interface Callback {void progress(String stage,long value);void done(JSONObject result);void error(String code);}
    private static final AtomicBoolean BUSY=new AtomicBoolean();
    private volatile Process process;
    private volatile boolean cancelled;
    static String home(){return TermuxConstants.TERMUX_HOME_DIR_PATH;}
    static String prefix(){return TermuxConstants.TERMUX_PREFIX_DIR_PATH;}
    static JSONObject base(Activity a,String operation)throws Exception{return new JSONObject().put("operation",operation).put("prefix",prefix()).put("home",home()).put("files",a.getFilesDir().getAbsolutePath()).put("packageId",a.getPackageName()).put("appVersion",com.termux.BuildConfig.VERSION_NAME);}
    void cancel(){cancelled=true;Process owned=process;if(owned!=null)owned.destroy();}
    boolean start(Activity a,JSONObject request,Callback callback){
        if(!BUSY.compareAndSet(false,true))return false;
        new Thread(()->{File work=null;try{
            File python=new File(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH,"python3");if(!python.canExecute())throw new IOException("PYTHON_REQUIRED");
            File jobs=new File(a.getFilesDir(),"workspace-backup-jobs");if(!jobs.isDirectory()&&!jobs.mkdirs())throw new IOException("PRIVATE_STORAGE");
            work=new File(jobs,UUID.randomUUID().toString());if(!work.mkdir())throw new IOException("PRIVATE_STORAGE");
            File script=new File(work,"backup.py");try(InputStream in=a.getAssets().open("workspace-backup.py");OutputStream out=new FileOutputStream(script)){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
            request.put("work",work.getAbsolutePath());File input=new File(work,"request.json");try(OutputStream out=new FileOutputStream(input)){out.write(request.toString().getBytes(StandardCharsets.UTF_8));}
            ProcessBuilder pb=new ProcessBuilder(python.getAbsolutePath(),"-I","-S",script.getAbsolutePath(),"--request",input.getAbsolutePath());
            pb.directory(new File(home()));pb.environment().clear();for(String item:TermuxShellUtils.buildEnvironment(a,false,home())){int i=item.indexOf('=');if(i>0)pb.environment().put(item.substring(0,i),item.substring(i+1));}
            pb.redirectError(new File("/dev/null"));if(cancelled)throw new IOException("CANCELLED");process=pb.start();if(cancelled)process.destroy();
            JSONObject[] result={null};String[] failure={null};
            try(BufferedReader reader=new BufferedReader(new InputStreamReader(process.getInputStream(),StandardCharsets.UTF_8))){String line;while((line=reader.readLine())!=null){if(line.length()>32*1024*1024)throw new IOException("RESULT_TOO_LARGE");JSONObject event=new JSONObject(line);String stage=event.getString("stage");if(stage.equals("DONE"))result[0]=event.getJSONObject("result");else if(stage.equals("ERROR"))failure[0]=event.optString("code","BACKUP_FAILED");else a.runOnUiThread(()->{if(!cancelled&&!a.isDestroyed())callback.progress(stage,event.optLong("processed"));});}}
            int status=process.waitFor();if(cancelled)throw new IOException("CANCELLED");if(status!=0||result[0]==null)throw new IOException(failure[0]==null?"WORKER_FAILED":failure[0]);
            a.runOnUiThread(()->{if(!cancelled&&!a.isDestroyed())callback.done(result[0]);});
        }catch(Exception e){String code=cancelled?"CANCELLED":e.getMessage();if(code==null||!code.matches("[A-Z_]{1,80}"))code="BACKUP_FAILED";final String error=code;a.runOnUiThread(()->{if(!a.isDestroyed())callback.error(error);});}
        finally{Process owned=process;if(owned!=null&&running(owned)){owned.destroy();try{for(int i=0;i<80&&running(owned);i++)Thread.sleep(100);}catch(InterruptedException e){Thread.currentThread().interrupt();}}process=null;if(work!=null&&(owned==null||!running(owned)))removeOwned(work);BUSY.set(false);}},"jobs-backup-worker").start();return true;
    }
    static boolean symlink(File file){try{return android.system.OsConstants.S_ISLNK(android.system.Os.lstat(file.getAbsolutePath()).st_mode);}catch(android.system.ErrnoException e){return false;}}
    private static boolean running(Process p){try{p.exitValue();return false;}catch(IllegalThreadStateException e){return true;}}
    private static void removeOwned(File file){if(symlink(file)){file.delete();return;}File[] children=file.listFiles();if(children!=null)for(File child:children)removeOwned(child);file.delete();}
}
