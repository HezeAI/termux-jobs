package com.termux.app.workspace;
import android.app.Application;
import android.content.*;
import androidx.core.content.FileProvider;
import java.io.*;
import java.nio.file.Files;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class ChatApkTest {
 @Before public void resetProviderCacheForIsolatedRobolectricApps()throws Exception{java.lang.reflect.Field cache=FileProvider.class.getDeclaredField("sCache");cache.setAccessible(true);((java.util.Map<?,?>)cache.get(null)).clear();}
 @Test public void installerUsesReadOnlyPrivateContentUriAndDoesNotLaunchIt()throws Exception{
  Context c=RuntimeEnvironment.application;File folder=new File(c.getCacheDir(),"chat-apk-artifacts");folder.mkdirs();File apk=new File(folder,"fixture.apk");Files.write(apk.toPath(),"abc".getBytes(java.nio.charset.StandardCharsets.UTF_8));
  try{Intent intent=ChatAttachmentUi.installIntent(c,apk);assertEquals(Intent.ACTION_VIEW,intent.getAction());assertEquals("application/vnd.android.package-archive",intent.getType());assertEquals("content",intent.getData().getScheme());assertEquals(c.getPackageName()+".workspace.files",intent.getData().getAuthority());assertTrue((intent.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION)!=0);assertEquals(0,intent.getFlags()&Intent.FLAG_GRANT_WRITE_URI_PERMISSION);assertNotNull(intent.getClipData());assertNull(Shadows.shadowOf((Application)c).getNextStartedActivity());
   assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",ChatAttachments.fileSha256(apk));assertEquals(3,apk.length());
   File privateFile=new File(c.getFilesDir(),"not-for-install.apk");Files.write(privateFile.toPath(),new byte[]{1});try{ChatAttachmentUi.installIntent(c,privateFile);fail("Do not expose arbitrary private files");}catch(IllegalArgumentException expected){}finally{privateFile.delete();}
  }finally{apk.delete();}
 }
 @Test public void eachAgentUsesItsOwnPrivateCopiesAndManagedApksCanBeInstalled()throws Exception{
  Context c=RuntimeEnvironment.application;java.util.Set<String> roots=new java.util.HashSet<>();String id="eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee";
  for(String agent:new String[]{"codex","gemini","claude","opencode"}){
   WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment=agent.equals("codex")?"codex":"ubuntu";p.geminiAgent=agent.equals("gemini");p.claudeAgent=agent.equals("claude");p.aiAgent=agent.equals("opencode")?agent:"";
   assertTrue(ChatAttachments.supported(p));File root=ChatAttachments.root(c,p);assertTrue(roots.add(root.getCanonicalPath()));File dir=new File(root,id);dir.mkdirs();File original=new File(dir,"original.apk");Files.write(original.toPath(),"abc".getBytes(java.nio.charset.StandardCharsets.UTF_8));Files.write(new File(dir,"attachment.json").toPath(),new byte[]{1});
   org.json.JSONObject attachment=new org.json.JSONObject().put("type","apk").put("id",id).put("path",ChatAttachments.prefix(p)+id+"/original.apk");File cached=ChatAttachments.materialize(c,p,attachment);assertTrue(cached.getPath().contains("chat-apk-artifacts"));assertNotEquals(original,cached);assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",ChatAttachments.fileSha256(cached));assertEquals("content",ChatAttachmentUi.installIntent(c,cached).getData().getScheme());
   ChatAttachments.discard(c,attachment);assertFalse(dir.exists());assertTrue(cached.isFile());cached.delete();
  }
 }

}
