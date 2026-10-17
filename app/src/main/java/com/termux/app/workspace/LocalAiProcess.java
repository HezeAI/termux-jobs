package com.termux.app.workspace;
import android.content.Context;
import com.termux.shared.shell.TermuxShellUtils;
import com.termux.shared.termux.TermuxConstants;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
/** App-owned runner, stdin/stdout only. No network endpoint or shell prompt scraping. */
final class LocalAiProcess implements AutoCloseable {
 static void validate(String location){if(!Arrays.asList("android","termux","ubuntu").contains(location))throw new IllegalArgumentException("Invalid engine location");}
 static String label(String location,boolean ru){return location.equals("android")?(ru?"Встроенный Android":"Embedded Android"):location.equals("termux")?"Termux":(ru?"Ubuntu для локального ИИ":"Ubuntu for local AI");}
 static String command(String location,File binary,File model){validate(location);if(location.equals("android"))throw new IllegalArgumentException("Embedded engine does not use a runner");if(location.equals("termux"))return "exec "+SessionShell.quote(binary.getPath());return "exec proot-distro login --isolated --bind "+SessionShell.quote(binary.getParent()+":/jobs-native")+" --bind "+SessionShell.quote(model.getParent()+":/jobs-models")+" "+SessionShell.quote(LocalAiEnvironment.ALIAS)+" -- "+SessionShell.quote("/jobs-native/"+binary.getName());}
 private final Thread reader;private final Process process;private final BufferedWriter writer;private final BlockingQueue<JSONObject> replies=new LinkedBlockingQueue<>(128);private final Object writeLock=new Object();private volatile int active;private int sequence;private volatile boolean closed;
 LocalAiProcess(Context c,String location,boolean fast,File model)throws Exception{this(c,location,fast,model,false);}
 LocalAiProcess(Context c,String location,boolean fast,File model,boolean gpu)throws Exception{
  validate(location);if(location.equals("ubuntu"))LocalAiEnvironment.require(c,LocalAiEnvironment.UBUNTU);
  File binary=new File(c.getApplicationInfo().nativeLibraryDir,gpu?"libjobs_runner_opencl.so":"libjobs_runner_"+location+(fast?"_fast":"")+".so");if(!binary.isFile()||!binary.canExecute())throw new IOException("RUNNER_NOT_AVAILABLE");
  ProcessBuilder pb=new ProcessBuilder(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH+"/bash","--noprofile","--norc","-c",command(location,binary,model));pb.directory(new File(TermuxConstants.TERMUX_HOME_DIR_PATH));pb.environment().clear();for(String item:TermuxShellUtils.buildEnvironment(c,false,TermuxConstants.TERMUX_HOME_DIR_PATH)){int at=item.indexOf('=');if(at>0)pb.environment().put(item.substring(0,at),item.substring(at+1));}if(gpu){File cache=new File(c.getCacheDir(),"local-ai/opencl");if(!cache.isDirectory()&&!cache.mkdirs())throw new IOException("GPU_CACHE_UNAVAILABLE");pb.environment().put("LD_LIBRARY_PATH","/vendor/lib64:/system/lib64");pb.environment().put("XDG_CACHE_HOME",cache.getPath());pb.environment().put("CL_CACHE_DIR",cache.getPath());}pb.redirectError(new File("/dev/null"));process=pb.start();writer=new BufferedWriter(new OutputStreamWriter(process.getOutputStream(),StandardCharsets.UTF_8));
  reader=new Thread(()->{try(BufferedReader input=new BufferedReader(new InputStreamReader(process.getInputStream(),StandardCharsets.UTF_8))){String line;while((line=input.readLine())!=null){if(line.length()>2*1024*1024)break;replies.put(new JSONObject(line));}}catch(Exception ignored){}finally{replies.offer(new JSONObject());}},"jobs-llama-reader");reader.setDaemon(true);reader.start();
 }
 void load(File model,String location,int threads,int context)throws Exception{try{request(new JSONObject().put("op","load").put("path",location.equals("ubuntu")?"/jobs-models/"+model.getName():model.getPath()).put("threads",threads).put("context",context),null);}catch(Exception e){close();throw e;}}

 private void send(JSONObject value)throws Exception{synchronized(writeLock){if(closed)throw new IOException("RUNNER_CLOSED");writer.write(value.toString());writer.newLine();writer.flush();}}
 JSONObject request(JSONObject request,LocalAiEngine.Output output)throws Exception{
  int id=++sequence;active=id;try{send(new JSONObject(request.toString()).put("id",id));long deadline=System.nanoTime()+TimeUnit.MINUTES.toNanos(20);while(System.nanoTime()<deadline){JSONObject r=replies.poll(250,TimeUnit.MILLISECONDS);if(r==null){if(!process.isAlive())throw new IOException("RUNNER_EXITED");continue;}if(r.length()==0)throw new IOException("RUNNER_OUTPUT_CLOSED");if(r.optInt("id")!=id)continue;String type=r.optString("type");if(type.equals("done"))return r;if(type.equals("error")){if(r.optString("value").equals("CANCELLED"))throw new CancellationException();throw new IOException("RUNNER_OPERATION_FAILED");}if(output!=null){if(type.equals("text"))output.text(r.getString("value"));else if(type.equals("stage"))output.stage(r.getString("value"));else if(type.equals("performance"))output.performance(r.get("value").toString());}}
   cancel();throw new IOException("RUNNER_TIMEOUT");
  }finally{active=0;}
 }
 void cancel(){int id=active;if(id!=0)try{send(new JSONObject().put("op","cancel").put("id",id));}catch(Exception ignored){}}
 public void close(){if(closed)return;try{send(new JSONObject().put("op","quit"));}catch(Exception ignored){}closed=true;try{writer.close();}catch(Exception ignored){}try{if(!process.waitFor(15,TimeUnit.SECONDS)){process.destroy();if(!process.waitFor(2,TimeUnit.SECONDS))process.destroyForcibly();}}catch(InterruptedException e){process.destroy();Thread.currentThread().interrupt();}try{process.getInputStream().close();}catch(Exception ignored){}reader.interrupt();}
 static JSONObject prompt(JSONArray messages,String system)throws Exception{JSONArray roles=new JSONArray(),texts=new JSONArray();for(int i=0;i<messages.length();i++){roles.put(messages.getJSONObject(i).getString("role"));texts.put(messages.getJSONObject(i).getString("text"));}return new JSONObject().put("roles",roles).put("texts",texts).put("system",system);}
}
