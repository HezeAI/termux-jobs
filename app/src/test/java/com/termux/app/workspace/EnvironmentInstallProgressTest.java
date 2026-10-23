package com.termux.app.workspace;
import org.junit.Test;import static org.junit.Assert.*;
public class EnvironmentInstallProgressTest {
 @Test public void etaRequiresMeasuredProgressAndResetsBetweenPhases(){
  EnvironmentInstallProgress p=new EnvironmentInstallProgress();p.update("Preparing",1000);assertEquals(-1,p.percent);assertEquals(-1,p.eta);
  p.update("Downloading 20%",2000);assertEquals(20,p.percent);assertEquals(-1,p.eta);
  p.update("Downloading 40%",4000);assertEquals(40,p.percent);assertEquals(6,p.eta);
  p.update("Extracting 5%",5000);assertEquals(5,p.percent);assertEquals(-1,p.eta);
  p.update("Configuring",6000);assertEquals(-1,p.percent);assertEquals(-1,p.eta);
 }
 @Test public void ansiAndInvalidPercentDoNotInventCompletion(){
  EnvironmentInstallProgress p=new EnvironmentInstallProgress();p.update("\u001b[31mDownloading 25%\u001b[0m",1000);assertEquals(25,p.percent);p.update("Downloading 200%",2000);assertEquals(-1,p.percent);
 }
 @Test public void failedVerificationShowsMissingShellInsteadOfDone(){assertEquals("MISSING:shell",EnvironmentInstallProgress.failureLine("READY:environment\nMISSING:shell\nCHECK:done\n"));}
 @Test public void networkFailureIsNotHiddenByVerificationMarkers(){assertEquals("ERROR:ROOTFS_DOWNLOAD_FAILED",EnvironmentInstallProgress.failureLine("ERROR:ROOTFS_DOWNLOAD_FAILED\nMISSING:environment\nCHECK:done\n"));assertEquals("Cannot download: connection reset by peer",EnvironmentInstallProgress.failureLine("Cannot download: connection reset by peer\nCHECK:done\n"));}
 @Test public void nativeInstallFailureWinsOverTrailingHelp(){assertEquals("[!] Failed to install: no space left on device",EnvironmentInstallProgress.failureLine("STAGE:install-environment\n[!] Failed to install: no space left on device\n[!] See 'proot-distro install --help' on how to install distribution image.\n"));}

}
