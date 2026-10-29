package com.termux.app.workspace;

import android.content.Context;
import android.content.Intent;
import com.termux.BuildConfig;
import com.termux.shared.shell.TermuxShellUtils;
import com.termux.shared.termux.TermuxConstants;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Application-owned job, independent of the settings window or terminal stdin. */
final class EnvironmentUpdateJobs {
 private EnvironmentUpdateJobs(){}
 private static final Object LOCK=new Object();
 private static JSONObject state=new JSONObject(),inventory=new JSONObject(),images=new JSONObject(),officialImages=new JSONObject();
 private static boolean active,cancelRequested;private static File cancel;private static long serial,revision;
 private static JSONObject withoutInventory(JSONObject value){JSONObject r=copy(value);r.remove("inventory");return r;}
 private static JSONObject copy(JSONObject value){try{return new JSONObject(value.toString());}catch(JSONException e){throw new IllegalStateException(e);}}
 static JSONObject state(){synchronized(LOCK){return copy(state);}}
 static JSONObject inventory(){synchronized(LOCK){return copy(inventory);}}
 static JSONObject images(){synchronized(LOCK){return copy(images);}}
 static JSONObject officialImages(){synchronized(LOCK){return copy(officialImages);}}
 static boolean active(){synchronized(LOCK){return active;}}
 static long revision(){synchronized(LOCK){return revision;}}
 static long serial(){synchronized(LOCK){return serial;}}
 static boolean mutating(String target){synchronized(LOCK){String op=state.optString("operation");return active&&state.optString("target").equals(target)&&(op.equals("upgrade")||op.equals("rollback")||op.equals("baseline")||op.equals("official-install")||op.equals("recover"));}}
 static void cancel(){cancel(state().optString("id"));}
 static void cancel(String expected){synchronized(LOCK){if(active&&state.optString("id").equals(expected))try{cancelRequested=true;if(cancel!=null)cancel.createNewFile();state.put("stopping",true);}catch(Exception ignored){}}}
 static boolean stopServiceIfIdle(android.app.Service service){synchronized(LOCK){if(active)return false;service.stopForeground(true);service.stopSelf();return true;}}
 static JSONObject request(String op,String target){try{JSONObject r=new JSONObject().put("operation",op);if(target!=null)r.put("target",target);return r;}catch(JSONException e){throw new IllegalStateException(e);}}
 static boolean start(Context supplied,JSONObject input){
  final Context context=supplied.getApplicationContext();final JSONObject request=copy(input);final boolean foreground=!Arrays.asList("inventory","images").contains(request.optString("operation"));final String id=UUID.randomUUID().toString();
  synchronized(LOCK){if(active)return false;active=true;serial++;state=copy(request);try{state.put("phase","starting").put("status","running").put("id",id);}catch(JSONException impossible){throw new IllegalStateException(impossible);}cancel=null;cancelRequested=false;}
  if(foreground)try{Intent intent=new Intent(context,EnvironmentUpdateService.class);context.startForegroundService(intent);}catch(RuntimeException denied){synchronized(LOCK){active=false;try{state.put("status","error").put("error","FOREGROUND_SERVICE_REQUIRED");}catch(JSONException ignored){}}return false;}
  new Thread(()->run(context,request,id,foreground),"jobs-environment-update").start();return true;
 }
 private static void privateDirectory(File dir)throws IOException{
  Path path=dir.toPath();if(Files.isSymbolicLink(path))throw new IOException("CONTROL_PATH");
  if(!dir.isDirectory()&&!dir.mkdir())throw new IOException("PRIVATE_STORAGE");
  dir.setReadable(false,false);dir.setWritable(false,false);dir.setExecutable(false,false);if(!dir.setReadable(true,true)||!dir.setWritable(true,true)||!dir.setExecutable(true,true))throw new IOException("PRIVATE_STORAGE");
 }
 private static void asset(Context c,File dir,String name)throws Exception{
  File target=new File(dir,name);if(Files.isSymbolicLink(target.toPath()))throw new IOException("CONTROL_PATH");
  File tmp=new File(dir,"asset-"+UUID.randomUUID());
  try(InputStream in=c.getAssets().open(name);OutputStream out=Files.newOutputStream(tmp.toPath(),StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
  try{tmp.setReadable(false,false);tmp.setWritable(false,false);tmp.setReadable(true,true);tmp.setWritable(true,true);Files.move(tmp.toPath(),target.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}finally{tmp.delete();}
 }
 private static void run(Context c,JSONObject request,String id,boolean foreground){
  JSONObject completed=null;String failure=null;Process process=null;File work=null;
  try{
   if(!new File(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH,"python3").canExecute())throw new IOException("PYTHON_REQUIRED");
   File root=new File(c.getFilesDir(),"workspace-environment-updates");privateDirectory(root);
   File jobs=new File(root,"jobs");privateDirectory(jobs);work=new File(jobs,id);privateDirectory(work);
   File runtime=new File(root,"runtime-"+BuildConfig.VERSION_CODE);privateDirectory(runtime);
   // A single operation owns this versioned runtime. An APK update cannot
   // replace files being used by a worker from an older application version.
   for(String name:new String[]{"environment-updates.py","environment-update-tree.py","environment-update-process.py","environment-update-packages.py","environment-update-images.py","environment-official-images.py","environment-official-fetch.py","environment-archlinuxarm.gpg","environment-update-fetch.py","agent-environment.py","workspace-backup.py","environment-image.py","environment-images.json","x11-base.py","x11-base-packages.json","x11-base-install.sh","x11-native-desktop.sh"})asset(c,runtime,name);
   request.put("files",c.getFilesDir().getAbsolutePath()).put("prefix",TermuxConstants.TERMUX_PREFIX_DIR_PATH).put("job",id);
   File file=new File(work,"request.json");try(OutputStream out=Files.newOutputStream(file.toPath(),StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){out.write(request.toString().getBytes(StandardCharsets.UTF_8));}
   file.setReadable(false,false);file.setWritable(false,false);file.setReadable(true,true);file.setWritable(true,true);
   synchronized(LOCK){cancel=new File(work,"cancel");if(cancelRequested)cancel.createNewFile();}
   ProcessBuilder pb=new ProcessBuilder(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH+"/python3","-I","-S",new File(runtime,"environment-updates.py").getAbsolutePath(),"--request",file.getAbsolutePath());
   pb.directory(new File(TermuxConstants.TERMUX_HOME_DIR_PATH));pb.environment().clear();
   for(String item:TermuxShellUtils.buildEnvironment(c,false,TermuxConstants.TERMUX_HOME_DIR_PATH)){int split=item.indexOf('=');if(split>0)pb.environment().put(item.substring(0,split),item.substring(split+1));}
   pb.environment().put("PREFIX",TermuxConstants.TERMUX_PREFIX_DIR_PATH);pb.redirectError(new File("/dev/null"));process=pb.start();process.getOutputStream().close();
   try(BufferedReader reader=new BufferedReader(new InputStreamReader(process.getInputStream(),StandardCharsets.UTF_8))){String line;while((line=reader.readLine())!=null){
    if(line.length()>16*1024*1024)throw new IOException("RESULT_TOO_LARGE");
    JSONObject event=new JSONObject(line);String type=event.optString("event");
    synchronized(LOCK){if(type.equals("progress")){for(Iterator<String> keys=event.keys();keys.hasNext();){String key=keys.next();state.put(key,event.get(key));}}}
    if(type.equals("done"))completed=event.getJSONObject("result");else if(type.equals("error"))failure=event.optString("code","UPDATE_FAILED");
   }}
   int exit=process.waitFor();if(exit!=0||completed==null)throw new IOException(failure==null?"UPDATE_WORKER_FAILED":failure);
   synchronized(LOCK){
    if(completed.has("inventory"))inventory=copy(completed.getJSONObject("inventory"));
    String op=request.getString("operation");
    if(op.equals("inventory"))inventory=copy(completed);
    else if(op.equals("images"))images=copy(completed);
    else if(op.equals("official-images")){JSONObject targets=officialImages.optJSONObject("targets");if(targets==null)targets=new JSONObject();JSONObject found=completed.optJSONObject("targets");if(found!=null)for(Iterator<String> keys=found.keys();keys.hasNext();){String key=keys.next();targets.put(key,found.get(key));}officialImages.put("targets",targets).put("checkedAt",completed.optLong("checkedAt"));}
    else if(op.equals("check")){JSONArray rows=inventory.optJSONArray("targets");if(rows!=null)for(int n=0;n<rows.length();n++){JSONObject row=rows.getJSONObject(n);if(row.optString("target").equals(request.optString("target")))row.put("plan",withoutInventory(completed));}}
    if(Arrays.asList("official-install","upgrade","baseline","rollback","recover").contains(op)){JSONObject targets=officialImages.optJSONObject("targets");String target=completed.optString("target",request.optString("target"));JSONObject row=targets==null?null:targets.optJSONObject(target);if(row!=null){row.remove("plan");row.put("installError","OFFICIAL_PLAN_CHANGED");}}
    revision++;state.put("status","complete").put("phase","complete");
   }
  }catch(Exception e){
   String code=e.getMessage();if(code==null||!code.matches("[A-Z_]{1,80}"))code="UPDATE_WORKER_FAILED";
   synchronized(LOCK){try{state.put("status",code.equals("CANCELLED")?"cancelled":"error").put("error",code);}catch(JSONException ignored){}}
  }finally{
   // Request cooperative cancellation and wait for the Python supervisor. Never
   // signal a user's terminal or recycle a child process/group ID on a timer.
   if(process!=null){if(completed==null&&work!=null)try{new File(work,"cancel").createNewFile();try(InputStream in=process.getInputStream()){byte[] discard=new byte[8192];while(in.read(discard)!=-1){}}}catch(IOException ignored){}boolean interrupted=false;for(;;)try{process.waitFor();break;}catch(InterruptedException e){interrupted=true;}if(interrupted)Thread.currentThread().interrupt();}
   synchronized(LOCK){if(foreground)c.stopService(new Intent(c,EnvironmentUpdateService.class));active=false;cancel=null;}
  }
 }
}
