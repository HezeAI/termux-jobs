package com.termux.x11;
import org.junit.Test;
import static org.junit.Assert.*;
public class X11InstallProgressTest {
 @Test public void optionalArchiveReportsRealBytesAndResetsBeforePackageInstallation(){
  X11InstallProgress p=new X11InstallProgress();p.feed("EXTRA_STAGE:kali-style\nEXTRA_PRO",0);p.feed("GRESS:25:100\n",1000);assertEquals(25,p.percent());assertEquals(25,p.downloadedBytes());assertEquals(100,p.totalBytes());assertEquals("Kali theme",p.currentItem());
  p.feed("EXTRA_PROGRESS:75:100\n",4000);assertEquals(75,p.percent());assertTrue(p.etaSeconds(4000)>0);
  for(String invalid:new String[]{"EXTRA_PROGRESS:101:100","EXTRA_PROGRESS:1:0","EXTRA_PROGRESS:-1:100","EXTRA_PROGRESS:999999999999999999999:1"})p.feed(invalid+"\n",4001);
  assertEquals(75,p.percent());p.feed("EXTRA_PROGRESS:100:100\nJOBS_APT_PHASE:install\ndlstatus:p:0:fetching\n",5000);assertEquals(10,p.percent());assertEquals(-1,p.totalBytes());
 }
 @Test public void zypperPackageSurvivesFragmentsAndSpinnerWithoutInventingBytes(){
  X11InstallProgress p=new X11InstallProgress();p.feed("JOBS_APT_PHASE:install\nPreloa",0);p.feed("ding: libpulse0-1.aarch64.rpm [",1);
  assertEquals("libpulse0-1.aarch64.rpm",p.currentItem());assertEquals(-1,p.percent());
  p.feed("done]\n.\r|\r/\r",2);assertEquals("libpulse0-1.aarch64.rpm",p.currentItem());assertEquals(-1,p.totalBytes());
  p.feed("JOBS_APT_PHASE:update\n",3);assertEquals("",p.currentItem());
 }
 @Test public void zypperOrdinalsAreOnlyCompletedAfterDoneAndInstallDoesNotRegress(){
  X11InstallProgress p=new X11InstallProgress();p.feed("JOBS_APT_PHASE:install\nRetrieving: foo-1.aarch64 (repo-oss (16.0)) (1/10), 1 MiB\n",1000);assertEquals(10,p.percent());assertEquals("foo-1.aarch64 (1/10)",p.currentItem());
  p.feed("Retrieving: foo-1.aarch64 (1/10), 1 MiB [done]\n",2000);assertEquals(14,p.percent());
  p.feed("( 1/10) Installing: foo-1.aarch64 [done]\n",3000);assertEquals("installing",p.stage());assertEquals(59,p.percent());
  p.feed("Retrieving: late-1.aarch64 (10/10) [done]\n",4000);assertEquals(59,p.percent());assertEquals("foo-1.aarch64 (1/10)",p.currentItem());
  p.feed("(10/10) Installing: bar-1.aarch64 [done]\n",5000);assertEquals(99,p.percent());assertEquals(-1,p.totalBytes());
 }
 @Test public void invalidZypperCountsOrUnframedOutputCannotChangeProgress(){
  X11InstallProgress p=new X11InstallProgress();p.feed("Retrieving: foo (1/2) [done]\n",0);assertEquals(-1,p.percent());assertEquals("",p.currentItem());p.feed("JOBS_APT_PHASE:install\n",0);
  for(String n:new String[]{"1/0","0/10","11/10","999999999999999999999999999999999999999999999999999999/0"})p.feed("Retrieving: invalid ("+n+") [done]\n",1);
  assertEquals(-1,p.percent());assertEquals("",p.currentItem());
 }
 @Test public void fragmentedAptStagesProvideMeasuredEtaAndNeverFinishBeforeVerification(){
  X11InstallProgress p=new X11InstallProgress();assertEquals(-1,p.percent());assertEquals(-1,p.etaSeconds(0));
  p.feed("JOBS_APT_PHASE:up",0);p.feed("date\ndlstatus:1:10:lists\n",0);p.feed("dlstatus:2:50:lists\n",4000);
  assertEquals("updating",p.stage());assertEquals(5,p.percent());assertEquals(5,p.etaSeconds(4000));
  p.feed("JOBS_APT_PHASE:install\ndlstatus:1:10:fetching\n",5000);assertEquals(-1,p.etaSeconds(5000));
  p.feed("dlstatus:2:50:fetching\n",9000);assertEquals("downloading",p.stage());assertEquals(32,p.percent());assertEquals(5,p.etaSeconds(9000));
  p.feed("pmstatus:test:10:unpacking\n",10000);assertEquals("installing",p.stage());assertEquals(-1,p.etaSeconds(10000));
  p.feed("dlstatus:2:40:late\npmstatus:test:100:configured\n",12000);assertEquals("installing",p.stage());assertEquals(99,p.percent());assertEquals(-1,p.etaSeconds(12000));
 }
 @Test public void malformedOversizeOutOfRangeAndStaleReportsDoNotInventProgress(){
  X11InstallProgress p=new X11InstallProgress();p.feed("dlstatus:1:50:no active apt\n",0);assertEquals(-1,p.percent());
  p.feed("JOBS_APT_PHASE:install\n",0);
  for(String n:new String[]{"NaN","Infinity","-1","101","bad"})p.feed("pmstatus:p:"+n+":x\n",0);
  assertEquals(-1,p.percent());p.feed(new String(new char[9000]).replace('\0','x')+"\npmstatus:p:10:x\n",0);p.feed("pmstatus:p:50:x\n",4000);assertEquals(77,p.percent());assertTrue(p.etaSeconds(4000)>0);assertEquals(-1,p.etaSeconds(20000));
 }
 @Test public void measuredAptBytesSurviveFragmentsButNotStageChangesOrFileCounts(){
  X11InstallProgress p=new X11InstallProgress();p.feed("JOBS_APT_PHASE:install\n",0);
  p.feed("[ 45.00] Bytes: 180 M / ",0);p.feed("400 M # Files: 2 / 10\ndlstatus:3:45:Retrieving file 3 of 10\n",1);
  assertEquals(180000000L,p.downloadedBytes());assertEquals(400000000L,p.totalBytes());
  p.feed("[ 50.00] Bytes: 1.5 G / 2.0 G # Files: 5 / 10\n",2);assertEquals(1500000000L,p.downloadedBytes());
  p.feed("pmstatus:p:5:unpack\n",3);assertEquals(-1,p.downloadedBytes());assertEquals(-1,p.totalBytes());
  p.feed("[ 60.00] Bytes: 1.8 G / 2.0 G # Files: 6 / 10\n",4);assertEquals(-1,p.totalBytes());
  p.feed("JOBS_APT_PHASE:update\n[1.000] Bytes: 125  / 500  # Files: 1 / 4\n",5);assertEquals(125,p.downloadedBytes());assertEquals(500,p.totalBytes());
  p.feed("JOBS_APT_PHASE:install\ndlstatus:300:50:files\n",6);assertEquals(-1,p.downloadedBytes());
 }
 @Test public void invalidOrUnknownByteTotalsAreNeverDisplayed(){
  X11InstallProgress p=new X11InstallProgress();p.feed("JOBS_APT_PHASE:install\n",0);
  for(String value:new String[]{"[50] Bytes: 1 M / 0 M # Files: 1 / 2","[101] Bytes: 1 M / 2 M # Files: 1 / 2","[50] Bytes: 3 M / 2 M # Files: 1 / 2","[50] Bytes: NaN M / 2 M # Files: 1 / 2","[50] Bytes: 1 X / 2 X # Files: 1 / 2"}){p.feed(value+"\n",0);assertEquals(-1,p.totalBytes());}
 }
 @Test public void anotherAptTransactionResetsCompletedBootstrapPercentage(){
  X11InstallProgress p=new X11InstallProgress();p.feed("JOBS_APT_PHASE:install\npmstatus:ca-certificates:100:configured\n",0);assertEquals(99,p.percent());
  p.feed("JOBS_APT_PHASE:update\ndlstatus:1:0:lists\n",1);assertEquals(0,p.percent());assertEquals(-1,p.totalBytes());
  p.feed("JOBS_APT_PHASE:install\ndlstatus:1:50:Firefox\n",2);assertEquals(32,p.percent());
 }
 @Test public void apkStageRatiosDoNotPretendToBeByteTotals(){
  X11InstallProgress p=new X11InstallProgress();p.feed("JOBS_APT_PHASE:install\n100/",0);p.feed("200 download\n",1);assertEquals(32,p.percent());assertEquals(-1,p.totalBytes());
  p.feed("50/100 install\n",2);assertEquals("installing",p.stage());assertEquals(77,p.percent());
  p.feed("300/200 download\n",3);assertEquals(77,p.percent());assertEquals(-1,p.totalBytes());
 }
 @org.junit.Test public void dnfProgressUsesMeasuredTransactionCountsAndDoesNotInventBytes(){
  X11InstallProgress p=new X11InstallProgress();p.feed("JOBS_APT_PHASE:install\n[ 2/ 10] Installing mousepad\n",1000);assertEquals("installing",p.stage());assertTrue(p.percent()>55);assertEquals(-1,p.totalBytes());
  p.feed("[9/0] Installing invalid\n[11/10] Installing invalid\n",2000);assertTrue(p.percent()<99);
 }
}
