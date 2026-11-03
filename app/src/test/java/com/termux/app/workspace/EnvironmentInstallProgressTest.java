package com.termux.app.workspace;
import org.junit.Test;import static org.junit.Assert.*;
public class EnvironmentInstallProgressTest {
 @Test public void pinnedDownloadStagesAreLocalizedAndShowMeasuredVolume(){
  EnvironmentInstallProgress p=new EnvironmentInstallProgress();p.update("STAGE:checking-image-cache\n",1000);
  assertEquals("Checking cached image",p.displayLine(false));assertEquals("Проверка сохранённого образа",p.displayLine(true));assertEquals(-1,p.percent);
  p.update("IMAGE_PROGRESS: 1% (3145728/211549268 bytes)\n",2000);
  assertEquals("Downloading image: 3.1 / 211.5 MB",p.displayLine(false));assertEquals(1,p.percent);
  p.update("STAGE:verifying-image-download\n",3000);assertEquals("Verifying image integrity",p.displayLine(false));assertEquals(-1,p.percent);assertEquals(-1,p.eta);
 }
 @Test public void etaRequiresMeasuredProgressAndResetsBetweenPhases(){
  EnvironmentInstallProgress p=new EnvironmentInstallProgress();p.update("Preparing",1000);assertEquals(-1,p.percent);assertEquals(-1,p.eta);
  p.update("IMAGE_PROGRESS: 20% (20/100 bytes)",2000);assertEquals(20,p.percent);assertEquals(-1,p.eta);
  p.update("IMAGE_PROGRESS: 40% (40/100 bytes)",4000);assertEquals(40,p.percent);assertEquals(6,p.eta);
  p.update("IMAGE_PROGRESS: 5% (5/100 bytes)",5000);assertEquals(5,p.percent);assertEquals(-1,p.eta);
  p.update("Configuring",6000);assertEquals(-1,p.percent);assertEquals(-1,p.eta);
 }
 @Test public void ansiAndInvalidPercentDoNotInventCompletion(){
  EnvironmentInstallProgress p=new EnvironmentInstallProgress();p.update("\u001b[31mDownloading 25%\u001b[0m",1000);assertEquals(-1,p.percent);p.update("Downloading 200%",2000);assertEquals(-1,p.percent);
 }
 @Test public void failedVerificationShowsMissingShellInsteadOfDone(){assertEquals("MISSING:shell",EnvironmentInstallProgress.failureLine("READY:environment\nMISSING:shell\nCHECK:done\n"));}
 @Test public void networkFailureIsNotHiddenByVerificationMarkers(){assertEquals("ERROR:ROOTFS_DOWNLOAD_FAILED",EnvironmentInstallProgress.failureLine("ERROR:ROOTFS_DOWNLOAD_FAILED\nMISSING:environment\nCHECK:done\n"));assertEquals("",EnvironmentInstallProgress.failureLine("Cannot download: connection reset by peer\nCHECK:done\n"));}
 @Test public void nativeInstallFailureWinsOverTrailingHelp(){assertEquals("",EnvironmentInstallProgress.failureLine("STAGE:install-environment\n[!] Failed to install: no space left on device\n[!] See 'proot-distro install --help' on how to install distribution image.\n"));}

}
