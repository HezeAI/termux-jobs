package com.termux.x11;
import org.junit.Test;
import static org.junit.Assert.*;
public class X11MetadataLogTest {
 @Test public void fragmentedWindowManagerReadyIsRetainedWithoutLoggingOtherOutput(){X11MetadataLog log=new X11MetadataLog();log.feed("JOBS_X11_WINDOW_MAN");assertFalse(log.windowManagerReady());log.feed("AGER=ready\n");assertTrue(log.windowManagerReady());for(int i=0;i<100;i++)log.feed("unrelated output\n");assertTrue(log.windowManagerReady());assertEquals("",log.snapshot());}

 @Test public void preservesFragmentedBackdropResultsThroughLaterOutput(){X11MetadataLog log=new X11MetadataLog();log.feed("JOBS_X11_BACK");log.feed("DROP image=0 decode=ok-64x64\n");for(int i=0;i<100;i++)log.feed("JOBS_X11_OBSERVE normal-output\n");assertEquals("JOBS_X11_BACKDROP image=0 decode=ok-64x64\n",log.snapshot());}
 @Test public void ignoresOtherLinesAndBoundsIncompleteAndRepeatedInput(){X11MetadataLog log=new X11MetadataLog();log.feed(new String(new char[10000]).replace('\0','x'));log.feed("\nprivate-path-or-text\n");assertEquals("",log.snapshot());for(int i=0;i<1000;i++)log.feed("JOBS_X11_BACKDROP image=1 decode=ok\n");assertTrue(log.snapshot().length()<=4096);assertFalse(log.snapshot().contains("private-path"));}
}
