package com.termux.app.workspace;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import javax.crypto.*;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.io.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=android.app.Application.class)
public class GeminiApiKeyVaultTest {
 @Test public void randomizedCiphertextIsBoundToGeminiAndProfileAndExcludedFromCopies()throws Exception{
  KeyGenerator generator=KeyGenerator.getInstance("AES");generator.init(256);SecretKey key=generator.generateKey();WorkspaceStore.Session p=new WorkspaceStore.Session();p.name="Gemini";char[] secret="synthetic-unit-key-123".toCharArray();
  p.geminiEncryptedKey=GeminiApiKeyVault.encrypt(key,p,secret);assertTrue(GeminiApiKeyVault.validRecord(p.geminiEncryptedKey));assertNotEquals(p.geminiEncryptedKey,GeminiApiKeyVault.encrypt(key,p,secret));assertArrayEquals(secret,GeminiApiKeyVault.decrypt(key,p));assertFalse(WorkspaceStore.sessionJson(p).toString().contains("synthetic-unit"));assertEquals("",p.copy().geminiEncryptedKey);
  WorkspaceStore.Session other=new WorkspaceStore.Session();other.geminiEncryptedKey=p.geminiEncryptedKey;try{GeminiApiKeyVault.decrypt(key,other);fail();}catch(java.security.GeneralSecurityException expected){}
  try{SshPasswordVault.decrypt(key,SshPasswordVault.binding(p),p.geminiEncryptedKey);fail();}catch(java.security.GeneralSecurityException expected){}
  WorkspaceStore store=new WorkspaceStore(RuntimeEnvironment.application,message->{});try{store.sessions.clear();store.sessions.add(p);String json=store.encode().toString();store.decode(json);assertEquals(p.geminiEncryptedKey,store.sessions.get(0).geminiEncryptedKey);assertFalse(json.contains("synthetic-unit"));}finally{store.close();}
 }
 @Test public void oneTimeHandoffIsPrivateAndSecretNeverAppearsInLaunchCommand()throws Exception{
  WorkspaceStore.Session p=new WorkspaceStore.Session();p.geminiAgent=true;p.environment="ubuntu";p.codexEnvironment="ubuntu-ai";p.geminiEncryptedKey="opaque-ciphertext";File target=GeminiApiKeyVault.runtime(RuntimeEnvironment.application,p);char[] secret="synthetic-test-api-key".toCharArray();
  try{GeminiApiKeyVault.write(target,secret);assertArrayEquals(new String(secret).getBytes(java.nio.charset.StandardCharsets.UTF_8),Files.readAllBytes(target.toPath()));assertFalse(GeminiCli.command(new File("/private"),p).contains("synthetic-test"));assertTrue(GeminiCli.command(new File("/private"),p).contains("JOBS_GEMINI_KEY_FILE"));assertEquals(java.util.EnumSet.of(PosixFilePermission.OWNER_READ,PosixFilePermission.OWNER_WRITE),Files.getPosixFilePermissions(target.toPath()));GeminiApiKeyVault.discard(RuntimeEnvironment.application,p);assertFalse(target.exists());}
  finally{target.delete();}
  for(String invalid:new String[]{"","line\nbreak","key space","\u0000","юникод"})assertFalse(GeminiApiKeyVault.valid(invalid.toCharArray()));assertFalse(GeminiApiKeyVault.validRecord("plaintext-not-ciphertext"));
 }
 @Test public void failedEnvironmentSetupDeletesOnlyTheOwnedHandoff()throws Exception{
  WorkspaceStore.Session p=new WorkspaceStore.Session();p.geminiAgent=true;p.environment="ubuntu";p.codexEnvironment="ubuntu-ai";p.geminiEncryptedKey="opaque";
  File directory=Files.createTempDirectory(RuntimeEnvironment.application.getFilesDir().toPath(),"key-rc-").toFile();
  File target=GeminiApiKeyVault.runtimePath(directory,p);GeminiApiKeyVault.write(target,"synthetic-unit-key".toCharArray());
  File other=new File(target.getParentFile(),"unrelated.key");Files.write(other.toPath(),new byte[]{1});
  try{File rc=SessionShell.prepare(directory,p);String script=new String(Files.readAllBytes(rc.toPath()),java.nio.charset.StandardCharsets.UTF_8);String outer=script.substring(script.indexOf("( trap "));
   outer=outer.substring(0,outer.indexOf("export TERMUX_JOBS_AGENT_ENVIRONMENT"))+"false\n)\n";
   File fixture=new File(directory,"failure.sh");Files.write(fixture.toPath(),outer.getBytes(java.nio.charset.StandardCharsets.UTF_8));Process process=new ProcessBuilder("bash",fixture.getAbsolutePath()).start();assertEquals(1,process.waitFor());assertFalse(target.exists());assertTrue(other.exists());
  }finally{target.delete();other.delete();}
 }

}
