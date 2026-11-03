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
 @Test public void launchUsesOnlyRunOwnedSocketAndNeverPlaintextPath()throws Exception{
  WorkspaceStore.Session p=new WorkspaceStore.Session();p.geminiAgent=true;p.environment="ubuntu";p.codexEnvironment="ubuntu-ai";p.geminiAuth="api";p.geminiEncryptedKey="opaque-ciphertext";
  String command=GeminiCli.command(new File("/private"),p);assertTrue(command.contains("JOBS_GEMINI_KEY_SOCKET"));assertTrue(command.contains(GeminiKeyBroker.address(p.geminiSocketId)));assertFalse(command.contains("JOBS_GEMINI_KEY_FILE"));assertFalse(command.contains("/keys/"));assertFalse(command.contains("opaque-ciphertext"));
  File directory=Files.createTempDirectory(RuntimeEnvironment.application.getFilesDir().toPath(),"key-rc-").toFile();File rc=SessionShell.prepare(directory,p);String script=new String(Files.readAllBytes(rc.toPath()),java.nio.charset.StandardCharsets.UTF_8);assertFalse(script.contains(".key"));assertFalse(script.contains("rm -f"));assertFalse(new File(GeminiCli.directory(directory),"keys").exists());
  WorkspaceStore.Session other=p.copy();other.geminiSocketId=java.util.UUID.randomUUID().toString();assertNotEquals(GeminiKeyBroker.address(p.geminiSocketId),GeminiKeyBroker.address(other.geminiSocketId));
  for(String invalid:new String[]{"","line\nbreak","key space","\u0000","юникод"})assertFalse(GeminiApiKeyVault.valid(invalid.toCharArray()));assertFalse(GeminiApiKeyVault.validRecord("plaintext-not-ciphertext"));
 }
 @Test public void ownerIdentityRejectsForeignReusedAndUnknownProcesses(){
  java.util.Map<Integer,GeminiKeyBroker.Identity> processes=new java.util.HashMap<>();GeminiKeyBroker.Identity owner=new GeminiKeyBroker.Identity(100,1,"123");processes.put(100,owner);processes.put(101,new GeminiKeyBroker.Identity(101,100,"124"));processes.put(102,new GeminiKeyBroker.Identity(102,101,"125"));processes.put(200,new GeminiKeyBroker.Identity(200,1,"126"));
  assertTrue(GeminiKeyBroker.belongs(owner,102,processes::get));assertFalse(GeminiKeyBroker.belongs(owner,200,processes::get));assertFalse(GeminiKeyBroker.belongs(owner,999,processes::get));
  processes.put(100,new GeminiKeyBroker.Identity(100,1,"999"));assertFalse(GeminiKeyBroker.belongs(owner,102,processes::get));processes.remove(100);assertFalse(GeminiKeyBroker.belongs(owner,102,processes::get));assertFalse(GeminiKeyBroker.belongs(null,102,processes::get));
  processes.put(100,owner);processes.put(102,new GeminiKeyBroker.Identity(102,102,"125"));assertFalse(GeminiKeyBroker.belongs(owner,102,processes::get));
 }
 @Test public void procParserUsesKernelStartIdentityAndRejectsDeadProcesses(){
  String[] fields=new String[20];java.util.Arrays.fill(fields,"0");fields[0]="S";fields[1]="42";fields[19]="12345";
  String stat="99 (name with ) paren) "+String.join(" ",fields);GeminiKeyBroker.Identity parsed=GeminiKeyBroker.Identity.parse(99,stat);assertNotNull(parsed);assertEquals(42,parsed.parent);assertEquals("12345",parsed.start);assertNull(GeminiKeyBroker.Identity.parse(100,stat));assertNull(GeminiKeyBroker.Identity.parse(99,stat.replace(") S ",") Z ")));assertNull(GeminiKeyBroker.Identity.parse(99,"broken"));
 }
}
