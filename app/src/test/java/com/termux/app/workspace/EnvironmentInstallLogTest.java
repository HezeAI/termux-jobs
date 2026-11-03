package com.termux.app.workspace;
import org.junit.Test;import static org.junit.Assert.*;
public class EnvironmentInstallLogTest {
 @Test public void multilineAndUnfinishedSecretsNeverEnterReport()throws Exception{
  String raw="-----BEGIN PRIVATE KEY-----\nSYNTHETIC_SECRET\n-----END PRIVATE KEY-----\nSTAGE:install-environment\nIMAGE_PROGRESS: 50% (50/100 bytes)\nERROR:SETUP_FAILED\nunfinished-SYNTHETIC_SECRET";
  String report=EnvironmentInstallLog.report("alpine",1,raw);assertFalse(report.contains("SYNTHETIC"));assertTrue(report.contains("ERROR:SETUP_FAILED"));assertTrue(report.contains("50/100"));
  for(int i=0;i<5000;i++)raw+="\nUNKNOWN_LINE";
  assertTrue(EnvironmentInstallLog.report("alpine",1,raw).length()<1024);
 }
 @Test public void unknownMarkersAndArbitraryPercentAreRejected(){
  EnvironmentSetup.Result r=new EnvironmentSetup.Result("ERROR:SYNTHETIC_SECRET\nSTAGE:SYNTHETIC_SECRET\nDownloading SYNTHETIC_SECRET 50%\nREADY:environment\nREADY:shell\nCHECK:done",0);
  assertTrue(r.ready());assertFalse(r.report.contains("SYNTHETIC"));
  assertFalse(new EnvironmentSetup.Result("ERROR:SETUP_FAILED\nREADY:environment\nREADY:shell\nCHECK:done",0).ready());
 }
 @Test public void resultAndExportAcceptOnlyKnownBoundedRecords(){
  String raw="ERROR:SETUP_FAILED\nAPI_KEY=SYNTHETIC_SECRET\nSTAGE:install-environment";
  EnvironmentSetup.Result r=new EnvironmentSetup.Result(raw,17);assertEquals(17,r.code);assertFalse(r.ready());assertFalse(r.report.contains("SYNTHETIC"));
  assertFalse(EnvironmentInstallLog.report("../SYNTHETIC_SECRET",17,raw).contains("SYNTHETIC"));
  assertFalse(EnvironmentSetupStatus.description(raw,false).contains("SYNTHETIC"));
 }
}
