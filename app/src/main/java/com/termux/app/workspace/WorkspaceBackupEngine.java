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
    interface Callback {void progress(JSONObject event);void done(JSONObject result);void error(String code);}
    private static final AtomicBoolean BUSY=new AtomicBoolean();
    interface ProcessStarter {Process start(ProcessBuilder builder)throws Exception;}
    private final ProcessStarter starter;
    WorkspaceBackupEngine(){this(null);}
    WorkspaceBackupEngine(ProcessStarter starter){this.starter=starter;}
    private volatile Process process;
    private volatile boolean cancelled;
    private final AtomicBoolean terminationRequested=new AtomicBoolean();
    static String home(){return TermuxConstants.TERMUX_HOME_DIR_PATH;}
    static String prefix(){return TermuxConstants.TERMUX_PREFIX_DIR_PATH;}
    static JSONObject base(Activity a,String operation)throws Exception{return new JSONObject().put("operation",operation).put("prefix",prefix()).put("home",home()).put("files",a.getFilesDir().getAbsolutePath()).put("packageId",a.getPackageName()).put("appVersion",com.termux.BuildConfig.VERSION_NAME);}
    void cancel(){cancelled=true;Process owned=process;if(owned!=null)terminateOnce(owned);}
    private void terminateOnce(Process owned){if(terminationRequested.compareAndSet(false,true))owned.destroy();}
    boolean start(Activity a,JSONObject request,Callback callback){
        if(!BUSY.compareAndSet(false,true))return false;
        new Thread(()->{File work=null;JSONObject completed=null;String terminalError=null;try{
            File python=new File(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH,"python3");if(starter==null&&!python.canExecute())throw new IOException("PYTHON_REQUIRED");
            File jobs=new File(a.getFilesDir(),"workspace-backup-jobs");if(!jobs.isDirectory()&&!jobs.mkdirs())throw new IOException("PRIVATE_STORAGE");
            work=new File(jobs,UUID.randomUUID().toString());if(!work.mkdir())throw new IOException("PRIVATE_STORAGE");
            File script=new File(work,"backup.py");try(InputStream in=a.getAssets().open("workspace-backup.py");OutputStream out=new FileOutputStream(script)){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
            if(request.optString("operation").startsWith("cleanup-"))try(InputStream in=a.getAssets().open("workspace-cleanup.py");OutputStream out=new FileOutputStream(new File(work,"workspace-cleanup.py"))){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
            request.put("work",work.getAbsolutePath());File input=new File(work,"request.json");try(OutputStream out=new FileOutputStream(input)){out.write(request.toString().getBytes(StandardCharsets.UTF_8));}
            ProcessBuilder pb=new ProcessBuilder(python.getAbsolutePath(),"-I","-S",script.getAbsolutePath(),"--request",input.getAbsolutePath());
            pb.directory(new File(home()));pb.environment().clear();for(String item:TermuxShellUtils.buildEnvironment(a,false,home())){int i=item.indexOf('=');if(i>0)pb.environment().put(item.substring(0,i),item.substring(i+1));}
            pb.redirectError(new File("/dev/null"));if(cancelled)throw new IOException("CANCELLED");process=starter==null?pb.start():starter.start(pb);if(cancelled)terminateOnce(process);
            JSONObject[] result={null};String[] failure={null};
            try(BufferedReader reader=new BufferedReader(new InputStreamReader(process.getInputStream(),StandardCharsets.UTF_8))){String line;while((line=reader.readLine())!=null){if(line.length()>32*1024*1024)throw new IOException("RESULT_TOO_LARGE");JSONObject event=new JSONObject(line);String stage=event.getString("stage");if(stage.equals("DONE"))result[0]=event.getJSONObject("result");else if(stage.equals("ERROR"))failure[0]=event.optString("code","BACKUP_FAILED");else a.runOnUiThread(()->{if(!cancelled&&!a.isDestroyed())callback.progress(event);});}}
            int status=process.waitFor();if(cancelled)throw new IOException("CANCELLED");if(status!=0||result[0]==null)throw new IOException(failure[0]==null?"WORKER_FAILED":failure[0]);
            completed=result[0];
        }catch(Exception e){String code=cancelled?"CANCELLED":e.getMessage();if(code==null||!code.matches("[A-Z_]{1,80}"))code="BACKUP_FAILED";terminalError=code;}
        finally{Process owned=process;if(owned!=null&&running(owned)){
            terminateOnce(owned);boolean interrupted=false;
            // Python must finish its owned archive/package-manager children before another storage job.
            // An elapsed grace period is not evidence of process exit. Never release BUSY on a timer.
            for(;;)try{owned.waitFor();break;}catch(InterruptedException e){interrupted=true;}
            if(interrupted)Thread.currentThread().interrupt();
        }process=null;try{if(work!=null)removeOwned(work);}finally{BUSY.set(false);}}
        final JSONObject result=completed;final String error=terminalError;
        // A late tap on Cancel must not hide an already committed restore/backup result.
        a.runOnUiThread(()->{if(!a.isDestroyed()){if(error==null)callback.done(result);else callback.error(error);}});
        },"jobs-backup-worker").start();return true;
    }
    static boolean symlink(File file){try{return android.system.OsConstants.S_ISLNK(android.system.Os.lstat(file.getAbsolutePath()).st_mode);}catch(android.system.ErrnoException e){return false;}}
    private static boolean running(Process p){try{p.exitValue();return false;}catch(IllegalThreadStateException e){return true;}}
    private static void removeOwned(File file){if(symlink(file)){file.delete();return;}File[] children=file.listFiles();if(children!=null)for(File child:children)removeOwned(child);file.delete();}
}
