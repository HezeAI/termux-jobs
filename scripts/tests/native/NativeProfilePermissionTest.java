package com.termux.app.workspace;

import android.app.Application;
import android.os.Looper;
import android.util.AtomicFile;
import android.view.*;
import android.widget.*;
import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.view.TerminalView;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

/** Persisted new profile -> actual native adapter -> app button -> native tool bytes. */
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class NativeProfilePermissionTest {
 void check(String agent,String kind,String where,boolean allow)throws Exception{
  String repo=System.getenv("TERMUX_FORK_SOURCE_ROOT"),guard=System.getenv("TERMUX_FORK_AUDIT_NET_GUARD");assertNotNull(repo);assertNotNull(guard);
  File root=new File(System.getProperty("java.io.tmpdir"),"p"+java.util.UUID.randomUUID().toString().replace("-","").substring(0,12));Files.createDirectory(root.toPath(),java.nio.file.attribute.PosixFilePermissions.asFileAttribute(java.nio.file.attribute.PosixFilePermissions.fromString("rwx------")));String run=java.util.UUID.randomUUID().toString();
  TermuxActivity activity=Robolectric.buildActivity(TermuxActivity.class).get();activity.setTheme(R.style.Theme_Termux_Black);WorkspaceLanguage.select(activity,"en");
  FrameLayout host=new FrameLayout(activity);host.setId(R.id.workspace_terminal_surface);TerminalView terminal=new TerminalView(activity,null);host.addView(terminal);java.lang.reflect.Field terminalField=TermuxActivity.class.getDeclaredField("mTerminalView");terminalField.setAccessible(true);terminalField.set(activity,terminal);activity.setContentView(host);
  AtomicFile storage=new AtomicFile(new File(root,"workspace.json"));WorkspaceStore store=new WorkspaceStore(activity,message->{throw new AssertionError(message);},storage);store.sessions.clear();
  WorkspaceStore.Session legacy=new WorkspaceStore.Session();legacy.environment="codex";legacy.codexEnvironment="termux";legacy.codexConfirmActions=false;String legacyBefore=WorkspaceStore.sessionJson(legacy).toString();store.sessions.add(legacy);
  WorkspaceStore.Session fresh=WorkspaceStore.newProfile();fresh.saved=true;fresh.directory=new File(root,"project").getAbsolutePath();fresh.codexEnvironment="ubuntu-ai";fresh.environment=agent.equals("codex")?"codex":"ubuntu";fresh.codexSocketId=run;fresh.claudeAgent=agent.equals("claude");fresh.geminiAgent=agent.startsWith("gemini");fresh.geminiAuth=agent.equals("gemini-vertex")?"vertex":"api";if(agent.equals("gemini-vertex")){fresh.geminiCloudProject="synthetic-project";fresh.geminiCloudLocation="global";}fresh.aiAgent=agent.equals("opencode")?"opencode":"";store.sessions.add(fresh);String id=fresh.id;store.save();store.awaitWrites();store.close();
  WorkspaceStore reopened=new WorkspaceStore(activity,message->{throw new AssertionError(message);},storage);WorkspaceStore.Session profile=reopened.sessions.stream().filter(p->p.id.equals(id)).findFirst().get();assertEquals(legacyBefore,WorkspaceStore.sessionJson(reopened.sessions.stream().filter(p->p.id.equals(legacy.id)).findFirst().get()).toString());assertEquals("untrusted",CodexSessionShell.approval(profile));assertEquals("manual",profile.claudeMode);assertEquals("default",profile.geminiMode);assertEquals("ask",profile.aiAccess);
  File files=new File(System.getProperty("java.io.tmpdir"),"f"+java.util.UUID.randomUUID().toString().replace("-","").substring(0,6));files.mkdirs();File controls=CodexSessionShell.socketDirectory(files);controls.mkdirs();if(agent.equals("codex"))Files.write(CodexChatControl.marker(files,profile).toPath(),new byte[]{1});
  String generated=agent.equals("codex")?CodexSessionShell.command(files,profile):agent.equals("claude")?ClaudeCode.command(files,profile):agent.startsWith("gemini")?GeminiCli.command(files,profile):AiAgents.command(files,profile);
  JSONObject spec=WorkspaceStore.sessionJson(profile).put("codexApproval",CodexSessionShell.approval(profile)).put("generatedLaunch",generated);File specFile=new File(root,"launch-profile.json");Files.write(specFile.toPath(),spec.toString(2).getBytes(StandardCharsets.UTF_8));reopened.close();
  ProcessBuilder builder=new ProcessBuilder(guard,new File(root,"network.jsonl").getAbsolutePath(),"--","node",new File(repo,"scripts/tests/fixtures/native-permission-chain.cjs").getAbsolutePath(),agent.startsWith("gemini")?"gemini":agent,kind,where,root.getAbsolutePath(),repo,run,specFile.getAbsolutePath());String path=System.getenv("PATH");builder.environment().clear();builder.environment().put("PATH",path);builder.environment().put("HOME",new File(root,"home").getAbsolutePath());builder.environment().put("TERMUX_FORK_TEST_UNIX_ROOT",root.getAbsolutePath());if(agent.equals("codex")){byte[] digest=java.security.MessageDigest.getInstance("SHA-256").digest(new File(root,run+".sock").getAbsolutePath().getBytes(StandardCharsets.UTF_8));StringBuilder hex=new StringBuilder();for(byte b:digest)hex.append(String.format("%02x",b&255));builder.environment().put("TERMUX_FORK_TEST_CODEX_SOCKET_HASH",hex.toString());}builder.redirectError(new File(root,"fixture.stderr"));Process child=builder.start();BufferedReader stdout=new BufferedReader(new InputStreamReader(child.getInputStream(),StandardCharsets.UTF_8));int port=-1;AgentChatUi chat=null;GeminiPermissionUi permissionUi=null;boolean success=false;
  try{
   ExecutorService reader=Executors.newSingleThreadExecutor();String readyLine;try{readyLine=reader.submit(()->{String line;while((line=stdout.readLine())!=null)if(line.startsWith("CHAIN_READY "))return line.substring(12);return null;}).get(35,TimeUnit.SECONDS);reader.submit(()->{try{while(stdout.readLine()!=null){}}catch(IOException ignored){}});}finally{reader.shutdown();}
   assertNotNull("Native startup failed; evidence "+root,readyLine);JSONObject ready=new JSONObject(readyLine);port=ready.getInt("port");final int targetPort=port;
   if(agent.equals("codex")){profile.codexThread=ready.getString("nativeSession");CodexSessionShell.socketDirectory(activity.getFilesDir()).mkdirs();Files.write(CodexChatControl.marker(activity.getFilesDir(),profile).toPath(),new byte[]{1});}if(agent.equals("claude"))profile.claudeThread=ready.getString("nativeSession");if(agent.equals("opencode"))profile.aiThread=ready.getString("nativeSession");profile.geminiSocketId=run;
   if(agent.equals("codex")||agent.equals("claude"))chat=new AgentChatUi(activity,()->profile,()->true,(p,q)->{assertEquals(id,p.id);return NativePermissionChainTest.request(targetPort,"/command",q);});
   else permissionUi=new GeminiPermissionUi(activity,()->profile,()->true,(source,q)->NativePermissionChainTest.request(targetPort,"/command",q),agent.equals("opencode"));
   if(agent.equals("opencode")&&where.equals("outside"))NativePermissionChainTest.request(port,"/scope-consent",new JSONObject().put("decision","once"));NativePermissionChainTest.request(port,"/start",new JSONObject());JSONObject done=null;boolean clicked=false;int preReadClicks=0;long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(60);
   while(System.nanoTime()<until){if(chat!=null)chat.sync();else permissionUi.refresh(profile);Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(120));JSONObject currentPermission=agent.equals("claude")&&kind.equals("write")?NativePermissionChainTest.request(port,"/command",new JSONObject().put("op","chat-snapshot")).optJSONObject("permission"):null;boolean preRead=currentPermission!=null&&currentPermission.optString("title").contains("Read");TextView button=NativePermissionChainTest.find(host,preRead?"Yes, once":allow?(chat!=null?"Yes, once":"Allow once"):"No");if(button!=null&&button.isEnabled()&&(preRead||!clicked)){button.performClick();if(preRead)preReadClicks++;else clicked=true;}JSONObject state=NativePermissionChainTest.request(port,"/result",new JSONObject());if(!state.isNull("done")){done=state.getJSONObject("done");break;}Thread.sleep(20);}
   assertNotNull("Native tool timeout; evidence "+root,done);assertFalse("Native error; evidence "+root+": "+done,done.has("error"));
   boolean nativeDenied=agent.startsWith("gemini")&&where.equals("outside")&&kind.equals("write");
   if(nativeDenied){assertFalse(clicked);assertFalse(done.getBoolean("contentChanged"));}
   else{assertTrue("No actual approval button; evidence "+root,clicked);assertEquals("Native effect disagrees with app button; evidence "+root,allow,done.getBoolean("contentChanged"));}
   done.put("preReadClicks",preReadClicks).put("agent",agent).put("kind",kind).put("where",where).put("requestedAllow",allow).put("appButton",clicked?(allow?"Allow once":"No"):"native policy denied path").put("persistedProfile",id).put("legacyProfileUnchanged",true);Files.write(new File(root,"ui-result.json").toPath(),done.toString(2).getBytes(StandardCharsets.UTF_8));success=true;
  }finally{if(chat!=null)chat.close();if(permissionUi!=null)permissionUi.close();if(port>0)try{NativePermissionChainTest.request(port,"/shutdown",new JSONObject());}catch(Exception ignored){}if(!child.waitFor(12,TimeUnit.SECONDS)){child.destroy();assertTrue("Owned fixture did not terminate",child.waitFor(12,TimeUnit.SECONDS));}stdout.close();activity.finish();if(success)assertEquals("Native fixture exit; evidence "+root,0,child.exitValue());}
 }
 @Test public void codexProfileToNativeShellAndPatch()throws Exception{for(String kind:new String[]{"shell","write"})for(String where:new String[]{"inside","outside"})for(boolean allow:new boolean[]{false,true})check("codex",kind,where,allow);}
 @Test public void claudeProfileToNativeShellAndWrite()throws Exception{for(String kind:new String[]{"shell","write"})for(String where:new String[]{"inside","outside"})for(boolean allow:new boolean[]{false,true})check("claude",kind,where,allow);}
 @Test public void geminiProfilesToNativeShellAndWrite()throws Exception{for(String agent:new String[]{"gemini-api","gemini-vertex"})for(String kind:new String[]{"shell","write"})for(String where:new String[]{"inside","outside"})for(boolean allow:new boolean[]{false,true})check(agent,kind,where,allow);}
 @Test public void openCodeProfileToNativeShellAndWrite()throws Exception{for(String kind:new String[]{"shell","write"})for(String where:new String[]{"inside","outside"})for(boolean allow:new boolean[]{false,true})check("opencode",kind,where,allow);}
}
