package com.termux.app.workspace;

import android.app.Application;
import android.view.MotionEvent;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class AttachmentSafetyTest {
    @Rule public TemporaryFolder tmp=new TemporaryFolder();
    @Test public void attachmentExportUsesExclusiveCreationAndPreservesLinks()throws Exception{
        File source=tmp.newFile("original"),dir=tmp.newFolder("download"),target=tmp.newFile("keep");
        Files.write(source.toPath(),new byte[]{1,2});Files.write(target.toPath(),new byte[]{42});
        File link=new File(dir,"result.apk");Files.createSymbolicLink(link.toPath(),target.toPath());
        File saved=ChatAttachmentUi.saveAttachment(source,dir,"result.apk",()->false);
        assertEquals("result-2.apk",saved.getName());assertArrayEquals(new byte[]{1,2},Files.readAllBytes(saved.toPath()));
        assertTrue(Files.isSymbolicLink(link.toPath()));assertArrayEquals(new byte[]{42},Files.readAllBytes(target.toPath()));
    }
    @Test public void stoppedExportLeavesNoFileAndPathLikeNameIsOnlyABasename()throws Exception{
        File source=tmp.newFile("original"),dir=tmp.newFolder("download");Files.write(source.toPath(),new byte[]{1,2});
        try{ChatAttachmentUi.saveAttachment(source,dir,"nothing",()->true);fail();}catch(InterruptedIOException expected){}
        assertEquals(0,dir.list().length);
        File saved=ChatAttachmentUi.saveAttachment(source,dir,"../folder\\name.txt",()->false);
        assertEquals(dir,saved.getParentFile());assertEquals(".._folder_name.txt",saved.getName());
    }
    @Test public void jpegOrientationIsReadWithoutChangingOriginal()throws Exception{
        // Minimal JPEG EXIF container: tests the real metadata parser without
        // depending on the host JDK's optional native JPEG codec. PNG decoding
        // and thumbnail dimensions are covered separately by the existing tests.
        File source=tmp.newFile("orientation.jpg");
        java.nio.ByteBuffer tiff=java.nio.ByteBuffer.allocate(26).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        tiff.putShort((short)0x4949).putShort((short)42).putInt(8).putShort((short)1)
            .putShort((short)0x112).putShort((short)3).putInt(1).putShort((short)6).putShort((short)0).putInt(0);
        try(DataOutputStream out=new DataOutputStream(new FileOutputStream(source))){
            out.writeShort(0xffd8);out.writeShort(0xffe1);out.writeShort(34);
            out.write(new byte[]{'E','x','i','f',0,0});out.write(tiff.array());out.writeShort(0xffd9);
        }
        byte[] before=Files.readAllBytes(source.toPath());float[] point={1,0};WorkspaceMediaThumbnails.imageOrientation(source).mapPoints(point);
        assertEquals(0,point[0],.0001);assertEquals(1,point[1],.0001);
        assertArrayEquals(before,Files.readAllBytes(source.toPath()));
    }
    private void event(ChatAttachmentUi.ZoomImage image,int action,float x,float y){
        MotionEvent e=MotionEvent.obtain(100,150,action,x,y,0);try{assertTrue(image.onTouchEvent(e));}finally{e.recycle();}
    }
    @Test public void imageTapClicksOnceWhileDragAndCancelDoNotClick(){
        ChatAttachmentUi.ZoomImage image=new ChatAttachmentUi.ZoomImage(RuntimeEnvironment.application);AtomicInteger clicks=new AtomicInteger();image.setOnClickListener(v->clicks.incrementAndGet());
        event(image,MotionEvent.ACTION_DOWN,10,10);event(image,MotionEvent.ACTION_UP,10,10);assertEquals(1,clicks.get());
        event(image,MotionEvent.ACTION_DOWN,10,10);event(image,MotionEvent.ACTION_MOVE,200,200);event(image,MotionEvent.ACTION_UP,200,200);assertEquals(1,clicks.get());
        event(image,MotionEvent.ACTION_DOWN,10,10);event(image,MotionEvent.ACTION_CANCEL,10,10);event(image,MotionEvent.ACTION_UP,10,10);assertEquals(1,clicks.get());
        assertTrue(image.performClick());assertEquals(2,clicks.get());
    }
}
