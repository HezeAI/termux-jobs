package com.termux.app.workspace;
import android.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadow.api.Shadow;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=android.app.Application.class,shadows={GeminiKeyBrokerTest.Server.class,GeminiKeyBrokerTest.Peer.class})
public class GeminiKeyBrokerTest {
 @Implements(LocalServerSocket.class) public static class Server {
  static final Map<String,Server> bound=new ConcurrentHashMap<>();final BlockingQueue<LocalSocket> peers=new LinkedBlockingQueue<>();volatile boolean closed;String address;
  @Implementation protected void __constructor__(String name)throws IOException{address=name;if(bound.putIfAbsent(name,this)!=null)throw new IOException("already bound");}
  @Implementation protected LocalSocket accept()throws IOException{try{while(!closed){LocalSocket p=peers.poll(50,TimeUnit.MILLISECONDS);if(p!=null)return p;}throw new IOException("closed");}catch(InterruptedException e){throw new IOException(e);}}
  @Implementation protected void close(){closed=true;bound.remove(address,this);}
 }
 @Implements(LocalSocket.class) public static class Peer {
  ByteArrayInputStream input;final ByteArrayOutputStream output=new ByteArrayOutputStream();Credentials credentials;final CountDownLatch closed=new CountDownLatch(1);
  @Implementation protected void __constructor__(){}
  @Implementation protected Credentials getPeerCredentials(){return credentials;}
  @Implementation protected InputStream getInputStream(){return input;}
  @Implementation protected OutputStream getOutputStream(){return output;}
  @Implementation protected void setSoTimeout(int timeout){}
  @Implementation protected void close(){closed.countDown();}
 }
 private Peer submit(String brokerRun,String requestedRun,int pid,int uid)throws Exception{
  LocalSocket socket=new LocalSocket();Peer p=Shadow.extract(socket);p.credentials=new Credentials(pid,uid,uid);ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(b);out.writeUTF(requestedRun);p.input=new ByteArrayInputStream(b.toByteArray());Server.bound.get(GeminiKeyBroker.address(brokerRun)).peers.add(socket);return p;
 }
 private void finished(Peer peer)throws Exception{assertTrue(peer.closed.await(5,TimeUnit.SECONDS));}
 @Test public void foreignUidRunAndProcessDoNotConsumeActiveRunAndKeyIsOneShot()throws Exception{
  String run=UUID.randomUUID().toString();Map<Integer,GeminiKeyBroker.Identity> ids=new ConcurrentHashMap<>();ids.put(100,new GeminiKeyBroker.Identity(100,1,"10"));ids.put(101,new GeminiKeyBroker.Identity(101,100,"11"));ids.put(200,new GeminiKeyBroker.Identity(200,1,"12"));
  AtomicInteger reads=new AtomicInteger();char[] supplied="SYNTHETIC_KEY".toCharArray();GeminiKeyBroker broker=new GeminiKeyBroker(run,()->{reads.incrementAndGet();return supplied;},ids::get);broker.attach(()->100);
  try{Peer uid=submit(run,run,101,android.os.Process.myUid()+1);finished(uid);assertEquals(0,uid.output.size());Peer foreign=submit(run,run,200,android.os.Process.myUid());finished(foreign);assertEquals(0,foreign.output.size());Peer wrong=submit(run,UUID.randomUUID().toString(),101,android.os.Process.myUid());finished(wrong);assertEquals(0,wrong.output.size());
   assertEquals(0,reads.get());Peer good=submit(run,run,101,android.os.Process.myUid());finished(good);assertEquals(1,reads.get());for(char c:supplied)assertEquals('\0',c);DataInputStream result=new DataInputStream(new ByteArrayInputStream(good.output.toByteArray()));assertEquals(13,result.readInt());byte[] value=new byte[13];result.readFully(value);assertEquals("SYNTHETIC_KEY",new String(value,java.nio.charset.StandardCharsets.US_ASCII));assertEquals(-1,result.read());
  }finally{broker.close();}
 }
 @Test public void reusedOwnerAndExplicitCancelClearOnlyTheirOwnBrokerAndRetryWorks()throws Exception{
  Map<Integer,GeminiKeyBroker.Identity> ids=new ConcurrentHashMap<>();ids.put(100,new GeminiKeyBroker.Identity(100,1,"10"));ids.put(101,new GeminiKeyBroker.Identity(101,100,"11"));
  String run=UUID.randomUUID().toString(),other=UUID.randomUUID().toString();GeminiKeyBroker a=new GeminiKeyBroker(run,()->"SYNTHETIC_A".toCharArray(),ids::get),b=new GeminiKeyBroker(other,()->"SYNTHETIC_B".toCharArray(),ids::get);a.attach(()->100);b.attach(()->100);
  java.lang.reflect.Field f=GeminiKeyBroker.class.getDeclaredField("keys");f.setAccessible(true);assertNotNull(f.get(a));a.close();assertNull(f.get(a));assertTrue(Server.bound.containsKey(GeminiKeyBroker.address(other)));
  try{ids.put(100,new GeminiKeyBroker.Identity(100,1,"99"));long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(4);while(Server.bound.containsKey(GeminiKeyBroker.address(other))&&System.nanoTime()<end)Thread.sleep(10);assertFalse(Server.bound.containsKey(GeminiKeyBroker.address(other)));}finally{b.close();}
  GeminiKeyBroker retry=new GeminiKeyBroker(run,()->"SYNTHETIC_RETRY".toCharArray(),ids::get);try{retry.attach(()->100);Peer good=submit(run,run,101,android.os.Process.myUid());finished(good);assertTrue(good.output.size()>4);}finally{retry.close();}
 }
 @Test public void deferredTerminalPidAndDeadOwnerFailClosedWithoutDeletingFiles()throws Exception{
  String run=UUID.randomUUID().toString();AtomicInteger pid=new AtomicInteger();GeminiKeyBroker broker=new GeminiKeyBroker(run,()->"SYNTHETIC".toCharArray(),p->p==100?new GeminiKeyBroker.Identity(100,1,"10"):p==101?new GeminiKeyBroker.Identity(101,100,"11"):null);broker.attach(pid::get);
  try{pid.set(100);Peer good=submit(run,run,101,android.os.Process.myUid());finished(good);assertTrue(good.output.size()>4);}finally{broker.close();}
 }
 @Test public void deadOwnerWatcherClearsKeyAndKeepsAnotherActiveRun()throws Exception{
  Map<Integer,GeminiKeyBroker.Identity> ids=new ConcurrentHashMap<>();ids.put(100,new GeminiKeyBroker.Identity(100,1,"10"));ids.put(200,new GeminiKeyBroker.Identity(200,1,"20"));String one=UUID.randomUUID().toString(),two=UUID.randomUUID().toString();
  GeminiKeyBroker a=new GeminiKeyBroker(one,()->"SYNTHETIC_A".toCharArray(),ids::get),b=new GeminiKeyBroker(two,()->"SYNTHETIC_B".toCharArray(),ids::get);a.attach(()->100);b.attach(()->200);
  java.lang.reflect.Field f=GeminiKeyBroker.class.getDeclaredField("keys");f.setAccessible(true);assertNotNull(f.get(a));
  try{ids.remove(100);long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(4);while(Server.bound.containsKey(GeminiKeyBroker.address(one))&&System.nanoTime()<end)Thread.sleep(10);assertFalse(Server.bound.containsKey(GeminiKeyBroker.address(one)));assertTrue(Server.bound.containsKey(GeminiKeyBroker.address(two)));assertNull(f.get(a));assertNull(f.get(a));}finally{a.close();b.close();}
 }
 @Test public void unstartedFailureCanCloseAndRetryWithoutTouchingAnotherRun()throws Exception{
  String run=UUID.randomUUID().toString();GeminiKeyBroker broker=new GeminiKeyBroker(run,()->"SYNTHETIC".toCharArray(),p->null);broker.close();assertFalse(Server.bound.containsKey(GeminiKeyBroker.address(run)));GeminiKeyBroker retry=new GeminiKeyBroker(run,()->"SYNTHETIC_RETRY".toCharArray(),p->null);retry.close();assertFalse(Server.bound.containsKey(GeminiKeyBroker.address(run)));
 }

 @Test public void unavailableVaultReturnsOnlySafeFailureAndNextRunCanRetry()throws Exception{
  String run=UUID.randomUUID().toString();GeminiKeyBroker broker=new GeminiKeyBroker(run,()->{throw new java.security.GeneralSecurityException("SYNTHETIC_PRIVATE_ERROR");},p->p==100?new GeminiKeyBroker.Identity(100,1,"10"):null);broker.attach(()->100);
  try{Peer p=submit(run,run,100,android.os.Process.myUid());finished(p);DataInputStream data=new DataInputStream(new ByteArrayInputStream(p.output.toByteArray()));assertEquals(-1,data.readInt());assertEquals(-1,data.read());}finally{broker.close();}
 }
 @Test public void cancelledDuringDecryptionNeverDeliversLateKeyAndClearsBytes()throws Exception{
  String run=UUID.randomUUID().toString();CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);char[] value="SYNTHETIC_LATE_KEY".toCharArray();GeminiKeyBroker broker=new GeminiKeyBroker(run,()->{entered.countDown();try{release.await(5,TimeUnit.SECONDS);}catch(InterruptedException e){throw new java.security.GeneralSecurityException(e);}return value;},p->p==100?new GeminiKeyBroker.Identity(100,1,"10"):null);broker.attach(()->100);
  try{Peer p=submit(run,run,100,android.os.Process.myUid());assertTrue(entered.await(3,TimeUnit.SECONDS));broker.close();release.countDown();long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);while(value[0]!='\0'&&System.nanoTime()<end)Thread.sleep(10);assertEquals(0,p.output.size());for(char c:value)assertEquals('\0',c);}finally{release.countDown();broker.close();}
 }

 @Test public void savedProfileReopensAfterOwnerDeathAndOnlyFreshRunReceivesKey()throws Exception{
  javax.crypto.KeyGenerator generator=javax.crypto.KeyGenerator.getInstance("AES");generator.init(256);javax.crypto.SecretKey key=generator.generateKey();
  android.app.Application app=RuntimeEnvironment.application;android.util.AtomicFile file=new android.util.AtomicFile(new File(app.getFilesDir(),"synthetic-recovery.json"));
  WorkspaceStore.Session original=WorkspaceStore.newProfile();original.geminiAgent=true;original.environment="ubuntu";original.codexEnvironment="ubuntu-ai";original.geminiAuth="api";original.saved=true;
  original.geminiEncryptedKey=GeminiApiKeyVault.encrypt(key,original,"SYNTHETIC_RECOVERY_ONLY".toCharArray());String id=original.id,oldRun=original.geminiSocketId;
  WorkspaceStore before=new WorkspaceStore(app,x->fail(x),file);before.sessions.clear();before.sessions.add(original);before.save();before.awaitWrites();before.close();
  Map<Integer,GeminiKeyBroker.Identity> ids=new ConcurrentHashMap<>();ids.put(100,new GeminiKeyBroker.Identity(100,1,"10"));AtomicInteger reads=new AtomicInteger();
  GeminiKeyBroker previous=new GeminiKeyBroker(oldRun,()->{reads.incrementAndGet();return GeminiApiKeyVault.decrypt(key,original);},ids::get);previous.attach(()->100);
  try{ids.remove(100);long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(4);while(Server.bound.containsKey(GeminiKeyBroker.address(oldRun))&&System.nanoTime()<end)Thread.sleep(10);assertFalse(Server.bound.containsKey(GeminiKeyBroker.address(oldRun)));assertEquals(0,reads.get());}finally{previous.close();}
  WorkspaceStore reopened=new WorkspaceStore(app,x->fail(x),file);
  try{WorkspaceStore.Session restored=reopened.sessions.stream().filter(p->p.id.equals(id)).findFirst().get();assertEquals(original.geminiEncryptedKey,restored.geminiEncryptedKey);restored.geminiSocketId=UUID.randomUUID().toString();String run=restored.geminiSocketId;
   String command=GeminiCli.command(app.getFilesDir(),restored);assertTrue(command.contains(GeminiKeyBroker.address(run)));assertFalse(command.contains(GeminiKeyBroker.address(oldRun)));assertFalse(command.contains("SYNTHETIC_RECOVERY_ONLY"));
   ids.put(200,new GeminiKeyBroker.Identity(200,1,"20"));ids.put(201,new GeminiKeyBroker.Identity(201,200,"21"));
   GeminiKeyBroker next=new GeminiKeyBroker(run,()->{reads.incrementAndGet();return GeminiApiKeyVault.decrypt(key,restored);},ids::get);next.attach(()->200);
   try{Peer stale=submit(run,oldRun,201,android.os.Process.myUid());finished(stale);assertEquals(0,stale.output.size());assertEquals(0,reads.get());Peer valid=submit(run,run,201,android.os.Process.myUid());finished(valid);DataInputStream data=new DataInputStream(new ByteArrayInputStream(valid.output.toByteArray()));byte[] delivered=new byte[data.readInt()];data.readFully(delivered);assertEquals("SYNTHETIC_RECOVERY_ONLY",new String(delivered,java.nio.charset.StandardCharsets.US_ASCII));assertEquals(1,reads.get());}finally{next.close();}
   assertFalse(new File(GeminiCli.directory(app.getFilesDir()),"keys").exists());assertFalse(new String(java.nio.file.Files.readAllBytes(file.getBaseFile().toPath()),java.nio.charset.StandardCharsets.UTF_8).contains("SYNTHETIC_RECOVERY_ONLY"));
  }finally{reopened.close();}
 }

}
