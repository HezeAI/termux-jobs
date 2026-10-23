package com.termux.app.workspace;
import org.junit.Test;
import static org.junit.Assert.*;
public class ChatMediaBoundsTest {
 @Test public void landscapePreviewIsCompactAndPreservesAspect(){int[] b=ChatAttachmentUi.mediaBounds(1080,2400,1920,1080,3);assertTrue(b[0]<=1080*.94-48*3+1);assertTrue(b[1]<600);assertEquals(1920.0/1080,(double)b[0]/b[1],.01);}
 @Test public void portraitPreviewFitsHeightAndPreservesAspect(){int[] b=ChatAttachmentUi.mediaBounds(1080,2400,1080,2400,3);assertTrue(b[1]<=2400*.82-144*3+1);assertTrue(b[0]<=1080*.94-48*3+1);assertEquals(1080.0/2400,(double)b[0]/b[1],.01);}
 @Test public void unavailableDimensionsAndSmallScreensStayPositive(){for(int[] x:new int[][]{{1080,2400,0,0},{120,100,1000,2000}}){int[] b=ChatAttachmentUi.mediaBounds(x[0],x[1],x[2],x[3],3);assertTrue(b[0]>0);assertTrue(b[1]>0);assertTrue(b[0]<x[0]);assertTrue(b[1]<x[1]);}}
}
