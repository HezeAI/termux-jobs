package com.termux.app.workspace;

import android.app.Application;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import com.termux.R;
import com.termux.app.TermuxActivity;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

/** Actual app buttons -> production bridge/native callback -> scheduler/tool -> artificial file. */
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class NativePermissionChainTest {
 static TextView find(View v,String label){if(v instanceof TextView&&label.equals(((TextView)v).getText().toString()))return (TextView)v;if(v instanceof ViewGroup)for(int n=0;n<((ViewGroup)v).getChildCount();n++){TextView found=find(((ViewGroup)v).getChildAt(n),label);if(found!=null)return found;}return null;}
 static JSONObject request(int port,String route,JSONObject value)throws Exception{
  HttpURLConnection c=(HttpURLConnection)new URL("http://127.0.0.1:"+port+route).openConnection();c.setConnectTimeout(10000);c.setReadTimeout(10000);c.setRequestMethod("POST");c.setDoOutput(true);
  try{try(OutputStream out=c.getOutputStream()){out.write(value.toString().getBytes(StandardCharsets.UTF_8));}try(InputStream in=c.getInputStream()){ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] b=new byte[4096];for(int n;(n=in.read(b))!=-1;)bytes.write(b,0,n);return new JSONObject(bytes.toString("UTF-8"));}}finally{c.disconnect();}
 }
 void check(String agent,String kind,String where,boolean allow)throws Exception{check(agent,kind,where,allow,false);}
 void check(String agent,String kind,String where,boolean allow,boolean rejectScope)throws Exception{
  String repo=System.getenv("TERMUX_FORK_SOURCE_ROOT");assertNotNull("Run with scripts/build-env.sh",repo);File root=Files.createTempDirectory(new File(repo,".verification/t").toPath(),"approval-chain-").toFile();String run=java.util.UUID.randomUUID().toString();
  String guard=System.getenv("TERMUX_FORK_AUDIT_NET_GUARD");assertNotNull("Native CLI checks require the kernel network guard",guard);ProcessBuilder builder=new ProcessBuilder(guard,new File(root,"network-events.jsonl").getAbsolutePath(),"--","node",new File(repo,"scripts/tests/fixtures/native-permission-chain.cjs").getAbsolutePath(),agent,kind,where,root.getAbsolutePath(),repo,run);String path=System.getenv("PATH");builder.environment().clear();builder.environment().put("PATH",path);builder.environment().put("HOME",new File(root,"home").getAbsolutePath());builder.directory(root);builder.redirectError(new File(root,"fixture.stderr"));Process child=builder.start();int port=-1;GeminiPermissionUi ui=null;boolean succeeded=false;
  BufferedReader stdout=new BufferedReader(new InputStreamReader(child.getInputStream(),StandardCharsets.UTF_8));try{
   java.util.concurrent.ExecutorService reader=java.util.concurrent.Executors.newSingleThreadExecutor();String line;
   try{line=reader.submit(()->{String next;while((next=stdout.readLine())!=null)if(next.startsWith("CHAIN_READY "))return next.substring(12);return null;}).get(30,TimeUnit.SECONDS);
    reader.submit(()->{try{while(stdout.readLine()!=null){}}catch(IOException ignored){}});
   }finally{reader.shutdown();}
   assertNotNull("Native fixture failed; evidence "+root,line);JSONObject ready=new JSONObject(line);port=ready.getInt("port");final int capturedPort=port;
   TermuxActivity activity=Robolectric.buildActivity(TermuxActivity.class).get();activity.setTheme(R.style.Theme_Termux_Black);WorkspaceLanguage.select(activity,"en");
   FrameLayout host=new FrameLayout(activity);host.setId(R.id.workspace_terminal_surface);activity.setContentView(host);WorkspaceStore.Session p=new WorkspaceStore.Session();p.codexEnvironment="ubuntu-ai";p.environment="ubuntu";p.geminiAgent=agent.equals("gemini");p.geminiSocketId=run;p.aiAgent=agent.equals("opencode")?"opencode":"";if(agent.equals("opencode"))p.aiThread=ready.getString("nativeSession");String captured=agent.equals("gemini")?run:p.id+":"+p.aiThread;
   ui=new GeminiPermissionUi(activity,()->p,()->true,(source,q)->{assertEquals(captured,source);return request(capturedPort,"/command",q);},agent.equals("opencode"));if(agent.equals("opencode")&&where.equals("outside"))request(port,"/scope-consent",new JSONObject().put("decision",rejectScope?"reject":"once"));request(port,"/start",new JSONObject());boolean clicked=false;JSONObject done=null;long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(45);
   while(System.nanoTime()<end){ui.refresh(p);Shadows.shadowOf(Looper.getMainLooper()).idle();TextView button=find(host,allow?"Allow once":"No");if(button!=null&&button.isEnabled()&&!clicked){button.performClick();clicked=true;}JSONObject state=request(port,"/result",new JSONObject());if(!state.isNull("done")){done=state.getJSONObject("done");break;}Thread.sleep(20);}
   assertNotNull("Native tool timeout; evidence "+root,done);assertFalse("Native error "+done+"; evidence "+root,done.has("error"));
   boolean nativePathDenied=agent.equals("gemini")&&where.equals("outside")&&!kind.equals("shell");boolean automaticRead=agent.equals("gemini")&&kind.equals("read")&&where.equals("inside");
   if(rejectScope){assertFalse(clicked);assertFalse(done.getBoolean("contentChanged"));assertEquals("reject",done.getJSONArray("scopeDecisions").getJSONObject(0).getString("decision"));}
   else if(nativePathDenied){assertFalse(clicked);assertFalse(done.getBoolean("contentChanged"));assertTrue(done.toString().contains("Path not in workspace"));}
   else if(automaticRead){assertFalse(clicked);assertTrue(done.toString().contains("SYNTHETIC_BEFORE"));}
   else{assertTrue("Our permission button was never used; evidence "+root,clicked);if(!kind.equals("read"))assertEquals("Tool effect mismatch; evidence "+root,allow,done.getBoolean("contentChanged"));if(agent.equals("gemini"))assertTrue(done.toString().contains(allow?"success":"cancelled"));else if(kind.equals("read"))assertEquals(allow,done.getBoolean("modelRead"));}
   done.put("appButton",clicked?(allow?"Allow once":"No"):"not offered by native policy").put("agent",agent).put("kind",kind).put("where",where).put("requestedAllow",allow);Files.write(new File(root,"ui-result.json").toPath(),done.toString(2).getBytes(StandardCharsets.UTF_8));
   activity.finish();succeeded=true;
  }catch(Exception|AssertionError error){Files.write(new File(root,"java-error.txt").toPath(),error.toString().getBytes(StandardCharsets.UTF_8));throw error;}finally{if(ui!=null)ui.close();if(port>0)try{request(port,"/shutdown",new JSONObject());}catch(Exception ignored){}if(!child.waitFor(10,TimeUnit.SECONDS)){child.destroy();assertTrue("Owned fixture did not stop",child.waitFor(10,TimeUnit.SECONDS));}stdout.close();if(succeeded)assertEquals("Native fixture exited abnormally; evidence "+root,0,child.exitValue());}
 }
 @Test public void geminiActualUiToNativeToolsInsideAndOutside()throws Exception{for(String kind:new String[]{"write","shell","read"})for(String where:new String[]{"inside","outside"})for(boolean allow:new boolean[]{false,true})check("gemini",kind,where,allow);}
 @Test public void openCodeActualUiToNativeToolsInsideAndOutsideAfterExplicitScopeConsent()throws Exception{for(String kind:new String[]{"write","shell","read"})for(String where:new String[]{"inside","outside"})for(boolean allow:new boolean[]{false,true})check("opencode",kind,where,allow);}
 @Test public void openCodeNativeScopeDenialStopsBeforeFileActionWithoutOurCard()throws Exception{check("opencode","write","outside",false,true);}
}
