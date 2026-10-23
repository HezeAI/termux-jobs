package com.termux.x11;
import org.junit.Test;
import static org.junit.Assert.*;

public class X11InstallErrorsTest {
 @Test public void pacmanRetrievingFailuresKeepCauseAndRedactPackageAndMirror(){
  for(String reason:new String[]{"Operation too slow. Less than 1 bytes/sec transferred the last 10 seconds","The requested URL returned error: 404","Could not resolve host","Failed to connect: No route to host","Permission denied"}){
   X11InstallErrors log=new X11InstallErrors();log.feed("error: failed retrieving file 'libcups-2:2.4.20-1-aarch64.pkg.tar.xz' from mirror.example : "+reason+"\n");
   String out=log.snapshot();assertTrue(out,out.contains(reason.startsWith("Operation too slow")?"Operation too slow":reason));assertFalse(out.contains("libcups"));assertFalse(out.contains("mirror.example"));
  }
 }
 @Test public void realXbpsMessagePreservesReasonAfterRepositoryAndPackageAreRemoved(){
  X11InstallErrors log=new X11InstallErrors();
  log.feed("ERROR: [trans] failed to download `libICE-1.1.2_1' signature from `https://user:password@repo.example/private?token=my-secret': Not Found\n");
  String out=log.snapshot();assertTrue(out,out.contains("signature from [value]: Not Found"));
  for(String secret:new String[]{"libICE","user:","password","repo.example","private","my-secret","https://"})assertFalse(secret,out.contains(secret));
 }
 @Test public void fragmentsAnsiCrAndFinalLineWithoutNewlineKeepOriginalFailure(){
  X11InstallErrors log=new X11InstallErrors();log.feed("\u001b[31mERROR: [trans] failed to down");log.feed("load `package' signature from `https://mirror.example/repo': Service Unavailable\u001b[0m\r");
  log.feed("curl: (28) Failed to fetch https://host.example/file: Operation timed out");log.finish();
  String out=log.snapshot();assertTrue(out.contains("Service Unavailable"));assertTrue(out.contains("Operation timed out"));assertFalse(out.contains("\u001b"));assertFalse(out.contains("mirror.example"));
 }
 @Test public void pathsCredentialsAddressesHeadersAndOpaqueValuesAreRedacted(){
  X11InstallErrors log=new X11InstallErrors();
  log.feed("Failed to download /sdcard/Download/private.txt to C:\\Users\\Alice\\file: Permission denied\n");
  log.feed("curl error 7 Failed to connect 192.168.0.9 at alice@example.test: Connection refused\n");
  log.feed("Failed to fetch token=short-secret\nFailed to fetch Authorization: Bearer abc123\n");
  log.feed("Failed to download api_key: a secret with spaces\nFailed to download credential bcd123\n");
  log.feed("Failed to fetch ghp_fakeSecretValue123456789012345678901234\nFailed to fetch /home/alice/a path/file.txt?access_token=hidden\n");
  String out=log.snapshot();for(String secret:new String[]{"private.txt","Alice","alice","192.168","example.test","short-secret","abc123","a secret","bcd123","ghp_","hidden","access_token"})assertFalse(secret,out.contains(secret));
  assertTrue(out.contains("Permission denied"));assertTrue(out.contains("Connection refused"));
 }
 @Test public void outputIsBoundedDeduplicatedAndNotReplacedByNormalProgress(){
  X11InstallErrors log=new X11InstallErrors();for(int n=0;n<30;n++)log.feed("Failed to download: diagnostic reason "+n+"\n");
  for(int n=0;n<1000;n++)log.feed("Installing normal-package progress private text\n");
  log.feed("Failed to download: diagnostic reason 29\n");String out=log.snapshot();assertEquals(12,out.split("\n").length);assertTrue(out.contains("reason 29"));assertFalse(out.contains("private"));assertFalse(out.contains("reason 0\n"));assertTrue(out.length()<12500);
  assertEquals("",new X11InstallErrors().snapshot());
 }
 @Test public void oversizedErrorsAreExplicitlyOmittedAndOrdinaryChatIsIgnored(){
  X11InstallErrors log=new X11InstallErrors();log.feed("Failed to download: "+new String(new char[9000]).replace('\0','x')+"password=secret\n");
  log.feed("my message and secret\nBEGIN PRIVATE KEY\n");String out=log.snapshot();assertTrue(out.contains("exceeded diagnostic limit"));assertFalse(out.contains("secret"));assertFalse(out.contains("PRIVATE KEY"));
 }
 @Test public void longSanitizedLinesKeepTheCauseAtTheirEnd(){
  X11InstallErrors log=new X11InstallErrors();StringBuilder line=new StringBuilder("Failed to download ");for(int n=0;n<300;n++)line.append("word ");line.append(": Bad Request\n");log.feed(line.toString());String out=log.snapshot();assertTrue(out.contains("middle omitted"));assertTrue(out.endsWith("Bad Request"));assertTrue(out.length()<1024);
 }
}
