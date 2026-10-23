package com.termux.app.workspace;
import android.app.Application;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.RobolectricTestRunner;import org.robolectric.annotation.Config;import java.nio.charset.StandardCharsets;import java.util.*;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class EnvironmentPackagesTest{
 byte[] data(String text){return text.getBytes(StandardCharsets.UTF_8);}
 void row(EnvironmentPackages.Kind kind,int tab,String raw,String name,String version)throws Exception{List<PackageCatalog.Entry> rows=EnvironmentPackages.parse(kind,data(raw),tab);assertEquals(1,rows.size());assertEquals(name,rows.get(0).name);assertEquals(version,rows.get(0).version);}
 @Test public void nativeCatalogueFormatsAreParsedWithoutGuessingEnvironment()throws Exception{
  row(EnvironmentPackages.Kind.APK,0,"libssl3-3.3.6-r0 aarch64 {openssl} (Apache-2.0) [installed]\n","libssl3","3.3.6-r0");
  row(EnvironmentPackages.Kind.DNF,1,"python3-libs\t3.14.0-1.fc44\n","python3-libs","3.14.0-1.fc44");
  row(EnvironmentPackages.Kind.ZYPPER,0,"<stream><search-result><solvable-list><solvable name='libFoo_1' edition='1.0-3.1'/></solvable-list></search-result></stream>","libFoo_1","1.0-3.1");
  row(EnvironmentPackages.Kind.XBPS,0,"ii bash-5.2.37_1 GNU shell\nhr removed-1.0_1 old\n","bash","5.2.37_1");
  row(EnvironmentPackages.Kind.XBPS,1,"[-] bash-5.2.37_1 GNU shell\n","bash","5.2.37_1");
  row(EnvironmentPackages.Kind.XBPS,2,"bash-5.3_1 update aarch64 repo 20KB 10KB\nlibfoo-1_1 install aarch64 repo 1KB 1KB\n","bash","5.3_1");
  row(EnvironmentPackages.Kind.PACMAN,0,"bash 5.3-1\n","bash","5.3-1");row(EnvironmentPackages.Kind.PACMAN,1,"core bash 5.3-1 [installed]\n","bash","5.3-1");row(EnvironmentPackages.Kind.PACMAN,2,"bash 5.2-1 -> 5.3-1\n","bash","5.3-1");
 }
 @Test public void namesCannotInjectArgumentsAndMutationsNeedConfirmation(){
  for(EnvironmentPackages.Kind kind:EnvironmentPackages.Kind.values())for(String value:new String[]{"-y","a;id","a/b","a\nx","a'bad","$(id)"})try{EnvironmentPackages.operation(kind,"install",value);fail();}catch(IllegalArgumentException expected){}
  assertEquals("pacman -Syu",EnvironmentPackages.operation(EnvironmentPackages.Kind.PACMAN,"refresh",null));
  assertEquals("pacman -Syu -- 'bash'",EnvironmentPackages.operation(EnvironmentPackages.Kind.PACMAN,"install","bash"));
  assertEquals("apk --interactive del -- 'bash'",EnvironmentPackages.operation(EnvironmentPackages.Kind.APK,"remove","bash"));
  for(EnvironmentPackages.Kind kind:EnvironmentPackages.Kind.values())for(String action:new String[]{"install","upgrade","remove"}){String cmd=EnvironmentPackages.operation(kind,action,"bash");assertFalse(cmd.contains("--noconfirm"));assertFalse(cmd.contains("--allow-unauthenticated"));assertFalse(cmd.contains("--force"));}
 }
 @Test public void xmlRejectsDoctypeAndPreservesDescription()throws Exception{
  try{EnvironmentPackages.parse(EnvironmentPackages.Kind.ZYPPER,data("<!DOCTYPE stream [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><stream>&x;</stream>"),0);fail();}catch(java.io.IOException expected){}
  assertEquals("A & B",EnvironmentPackages.parseDescription(EnvironmentPackages.Kind.ZYPPER,data("<stream><description>A &amp; B</description></stream>")));
 }
 @Test public void operationsKeepConcreteAliasAndRestoredIsolation(){
  WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="termux";p.prootAlias="jobs-abcdef123456";p.prootIsolated=true;p.handle="held";p.geminiAgent=false;
  WorkspaceStore.Session op=PackageCatalog.operationProfile(p,"apk add htop");assertEquals(p.prootAlias,op.prootAlias);assertTrue(op.prootIsolated);assertEquals("",op.handle);assertNotEquals(p.id,op.id);assertTrue(PackageCatalog.matchesTarget(p,"held","held",ProjectFiles.environment(op)));
  p.prootAlias="other";assertFalse(PackageCatalog.matchesTarget(p,"held","held",ProjectFiles.environment(op)));
 }
 @Test public void queriesNeverRefreshOrInstall(){for(EnvironmentPackages.Kind kind:EnvironmentPackages.Kind.values())for(int tab=0;tab<3;tab++){String cmd=EnvironmentPackages.query(kind,tab);assertFalse(cmd.contains("makecache"));assertFalse(cmd.contains("-Syu"));assertFalse(cmd.contains("apt-get update"));assertFalse(cmd.contains("apk update"));assertFalse(cmd.contains("zypper refresh"));assertFalse(cmd.contains("xbps-install -S"));}}

 @Test public void selectedOperationCannotFallThroughIntoHostAfterGuestExit()throws Exception{
  WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="termux";p.prootAlias="alpine";java.nio.file.Path marker=java.nio.file.Files.createTempDirectory("pkg-boundary").resolve("changed");WorkspaceStore.Session op=PackageCatalog.operationProfile(p,"printf changed > "+SessionShell.quote(marker.toString()));
  ProcessBuilder builder=new ProcessBuilder("/bin/sh","-c",op.startup);builder.environment().remove("TERMUX_JOBS_ENVIRONMENT_ALIAS");assertNotEquals(0,builder.start().waitFor());assertFalse(java.nio.file.Files.exists(marker));
  builder.environment().put("TERMUX_JOBS_ENVIRONMENT_ALIAS","debian");assertNotEquals(0,builder.start().waitFor());assertFalse(java.nio.file.Files.exists(marker));builder.environment().put("TERMUX_JOBS_ENVIRONMENT_ALIAS","alpine");assertEquals(0,builder.start().waitFor());assertTrue(java.nio.file.Files.exists(marker));
 }

 @Test public void guestProbeIgnoresHostAptAndKeepsNativeManagerForEveryGuest()throws Exception{
  java.nio.file.Path fixture=java.nio.file.Files.createTempDirectory("pkg-path");
  java.nio.file.Path host=java.nio.file.Files.createDirectory(fixture.resolve("host"));
  for(String name:new String[]{"apt-get","dpkg-query"})executable(host.resolve(name),"printf WRONG_HOST");
  String[] commands={"apk","dnf","zypper","xbps-query","pacman"};
  String[] expected={"APK","DNF","ZYPPER","XBPS","PACMAN"};
  for(int i=0;i<commands.length;i++){
   java.nio.file.Path guest=java.nio.file.Files.createDirectory(fixture.resolve(expected[i]));
   executable(guest.resolve(commands[i]),"printf NATIVE");
   String script=EnvironmentPackages.nativeCommand(EnvironmentPackages.probe()).replace(EnvironmentPackages.GUEST_PATH,guest.toString());
   ProcessBuilder builder=new ProcessBuilder("/bin/sh","-c",script);builder.environment().put("PATH",host.toString());
   Process probe=builder.start();String result=new String(probe.getInputStream().readAllBytes(),StandardCharsets.UTF_8);assertEquals(0,probe.waitFor());assertEquals(expected[i],result);
   script=EnvironmentPackages.nativeCommand(commands[i]).replace(EnvironmentPackages.GUEST_PATH,guest.toString());
   builder=new ProcessBuilder("/bin/sh","-c","original=$PATH; "+script+"; [ \"$PATH\" = \"$original\" ]");builder.environment().put("PATH",host.toString());
   Process operation=builder.start();assertEquals("NATIVE",new String(operation.getInputStream().readAllBytes(),StandardCharsets.UTF_8));assertEquals(0,operation.waitFor());
  }
  String empty=EnvironmentPackages.nativeCommand(EnvironmentPackages.probe()).replace(EnvironmentPackages.GUEST_PATH,fixture.resolve("missing").toString());
  ProcessBuilder b=new ProcessBuilder("/bin/sh","-c",empty);b.environment().put("PATH",host.toString());Process missing=b.start();assertEquals(2,missing.waitFor());
 }
 private void executable(java.nio.file.Path file,String body)throws Exception{java.nio.file.Files.write(file,("#!/bin/sh\n"+body+"\n").getBytes(StandardCharsets.UTF_8));assertTrue(file.toFile().setExecutable(true));}

 @Test public void alpineOfflineCatalogueDoesNotFilterOutRemoteRepositoryEntries()throws Exception{
  assertEquals("LC_ALL=C apk --no-network list",EnvironmentPackages.query(EnvironmentPackages.Kind.APK,1).trim());
  assertTrue(EnvironmentPackages.query(EnvironmentPackages.Kind.APK,0).contains("--installed"));
  assertTrue(EnvironmentPackages.query(EnvironmentPackages.Kind.APK,2).contains("--upgradable"));
  row(EnvironmentPackages.Kind.APK,1,"7zip-26.01-r0 aarch64 {7zip} (LGPL-2.0-only)\n","7zip","26.01-r0");
 }

 @Test public void dnfQueryUsesLiteralTabBecauseDnf5DoesNotExpandBackslashT()throws Exception{
  for(int tab=0;tab<3;tab++){
   String command=EnvironmentPackages.query(EnvironmentPackages.Kind.DNF,tab);
   assertTrue(command.contains("%{name}\t%{evr}"));assertFalse(command.contains("%{name}\\t%{evr}"));
  }
  java.nio.file.Path fixture=java.nio.file.Files.createTempDirectory("dnf-format");
  executable(fixture.resolve("dnf"),"while [ \"$#\" -gt 0 ]; do if [ \"$1\" = --queryformat ]; then shift; printf '%s' \"$1\"; exit; fi; shift; done; exit 2");
  ProcessBuilder builder=new ProcessBuilder("/bin/sh","-c",EnvironmentPackages.query(EnvironmentPackages.Kind.DNF,1));builder.environment().put("PATH",fixture.toString());Process process=builder.start();
  String actual=new String(process.getInputStream().readAllBytes(),StandardCharsets.UTF_8);assertEquals(0,process.waitFor());
  // DNF5 expands tags and newline, but leaves backslash-t literally intact.
  actual=actual.replace("%{name}","0xFFFF").replace("%{evr}","0.10-18.fc44").replace("\\n","\n");
  row(EnvironmentPackages.Kind.DNF,1,actual,"0xFFFF","0.10-18.fc44");
 }

 @Test public void voidVersionsMayStartWithLettersAndRetainRevision()throws Exception{
  for(String[] pair:new String[][]{{"UEFITool","A75_1"},{"gtk-lshw","B.02.20_1"},{"lf","r42_2"},{"libui-devel","alpha4.1_2"},{"mksh","R59c_2"},{"perl-Test-Trap","v0.3.4_3"},{"psutils","p17_5"}}){
   row(EnvironmentPackages.Kind.XBPS,1,"[-] "+pair[0]+"-"+pair[1]+" Description\n",pair[0],pair[1]);
   row(EnvironmentPackages.Kind.XBPS,0,"ii "+pair[0]+"-"+pair[1]+" Description\n",pair[0],pair[1]);
   row(EnvironmentPackages.Kind.XBPS,2,pair[0]+"-"+pair[1]+" update aarch64 repo 1KB 1KB\n",pair[0],pair[1]);
  }
  try{EnvironmentPackages.parse(EnvironmentPackages.Kind.XBPS,data("[-] mksh-R59c Description\n"),1);fail();}catch(java.io.IOException expected){}
 }

 @Test public void xbpsRequestsUseIpv4AndDoNotModifyTheCallingShell()throws Exception{
  java.nio.file.Path fixture=java.nio.file.Files.createTempDirectory("xbps-ipv4");
  for(String binary:new String[]{"xbps-query","xbps-install","xbps-remove"})executable(fixture.resolve(binary),"printf 'child:%s:%s\\n' \"$FORCE_IPV4\" \"${FORCE_IPV6-unset}\"");
  List<String> commands=new ArrayList<>();for(int tab=0;tab<3;tab++)commands.add(EnvironmentPackages.query(EnvironmentPackages.Kind.XBPS,tab));for(String action:new String[]{"install","upgrade","remove","refresh"})commands.add(EnvironmentPackages.operation(EnvironmentPackages.Kind.XBPS,action,"mousepad"));
  for(String command:commands){ProcessBuilder b=new ProcessBuilder("/bin/sh","-c",command+"; printf 'parent:%s:%s\\n' \"$FORCE_IPV4\" \"$FORCE_IPV6\"");b.environment().put("PATH",fixture.toString());b.environment().put("FORCE_IPV4","0");b.environment().put("FORCE_IPV6","1");Process p=b.start();String out=new String(p.getInputStream().readAllBytes(),StandardCharsets.UTF_8);assertEquals(command,0,p.waitFor());assertEquals(command,"child:1:unset\nparent:0:1\n",out);}
  for(EnvironmentPackages.Kind kind:EnvironmentPackages.Kind.values())if(kind!=EnvironmentPackages.Kind.XBPS)for(int tab=0;tab<3;tab++)assertFalse(EnvironmentPackages.query(kind,tab).contains("FORCE_IPV4"));
 }
}
