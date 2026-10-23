package com.termux.app.workspace;

import android.app.Activity;
import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Looper;
import android.view.View;
import android.widget.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class WorkspaceMediaThumbnailsTest {
 @Rule public TemporaryFolder temp=new TemporaryFolder();
 private void layout(View v){v.measure(View.MeasureSpec.makeMeasureSpec(500,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(280,View.MeasureSpec.EXACTLY));v.layout(0,0,500,280);}
 private void drain()throws Exception{Thread.sleep(20);Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(100));}
 @Test public void onlyVisibleRowsDecodeAndReturningUsesCache()throws Exception{
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();ScrollView scroll=new ScrollView(a);LinearLayout rows=new LinearLayout(a);rows.setOrientation(LinearLayout.VERTICAL);scroll.addView(rows);FrameLayout host=new FrameLayout(a);host.addView(scroll,new FrameLayout.LayoutParams(500,280));a.setContentView(host);AtomicInteger calls=new AtomicInteger();
  WorkspaceMediaThumbnails thumbs=new WorkspaceMediaThumbnails("termux",new WorkspaceMediaThumbnails.Loader(){public Bitmap load(ProjectFiles.Entry e){calls.incrementAndGet();return Bitmap.createBitmap(192,192,Bitmap.Config.ARGB_8888);}public void cancel(){}});ImageView[] images=new ImageView[100];
  try{for(int i=0;i<100;i++){FrameLayout row=new FrameLayout(a);ImageView image=new ImageView(a);images[i]=image;row.addView(image);rows.addView(row,new LinearLayout.LayoutParams(-1,100));thumbs.bind(row,image,new ProjectFiles.Entry("/test","image-"+i+".jpg",false,100,i));}layout(scroll);thumbs.schedule(scroll);
   for(int i=0;i<50&&calls.get()<3;i++)drain();for(int i=0;i<3;i++)drain();assertEquals(3,calls.get());assertTrue(images[0].getDrawable() instanceof BitmapDrawable);assertNull(images[30].getDrawable());
   scroll.scrollTo(0,500);thumbs.schedule(scroll);for(int i=0;i<50&&calls.get()<6;i++)drain();for(int i=0;i<3;i++)drain();assertEquals(6,calls.get());assertFalse(images[0].getDrawable() instanceof BitmapDrawable);
   scroll.scrollTo(0,0);thumbs.schedule(scroll);for(int i=0;i<5;i++)drain();assertEquals(6,calls.get());assertTrue(images[0].getDrawable() instanceof BitmapDrawable);
  }finally{thumbs.close();}
 }
 @Test public void lateDecodeCannotPaintAfterDirectoryChangeOrClose()throws Exception{
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();ScrollView scroll=new ScrollView(a);LinearLayout rows=new LinearLayout(a);rows.setOrientation(LinearLayout.VERTICAL);scroll.addView(rows);FrameLayout host=new FrameLayout(a);host.addView(scroll,new FrameLayout.LayoutParams(500,280));a.setContentView(host);FrameLayout row=new FrameLayout(a);ImageView image=new ImageView(a);row.addView(image);rows.addView(row,new LinearLayout.LayoutParams(-1,100));layout(scroll);CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);AtomicInteger cancelled=new AtomicInteger();
  WorkspaceMediaThumbnails thumbs=new WorkspaceMediaThumbnails("termux",new WorkspaceMediaThumbnails.Loader(){public Bitmap load(ProjectFiles.Entry e)throws Exception{entered.countDown();release.await(3,TimeUnit.SECONDS);return Bitmap.createBitmap(192,192,Bitmap.Config.ARGB_8888);}public void cancel(){cancelled.incrementAndGet();}});
  try{thumbs.bind(row,image,new ProjectFiles.Entry("/first","shot.jpg",false,10,1));thumbs.schedule(scroll);Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(100));assertTrue(entered.await(2,TimeUnit.SECONDS));thumbs.reset();rows.removeAllViews();thumbs.close();release.countDown();for(int i=0;i<8;i++)drain();assertNull(image.getDrawable());assertTrue(cancelled.get()>=2);}finally{release.countDown();thumbs.close();}
 }
 @Test public void identitySeparatesEnvironmentsAndSameSizeReplacements(){
  ProjectFiles.Entry a=new ProjectFiles.Entry("/Pictures","photo.jpg",false,42,1000),b=new ProjectFiles.Entry("/Pictures","photo.jpg",false,42,2000);
  assertNotEquals(WorkspaceMediaThumbnails.identity("termux",a),WorkspaceMediaThumbnails.identity("proot:debian",a));assertNotEquals(WorkspaceMediaThumbnails.identity("termux",a),WorkspaceMediaThumbnails.identity("termux",b));
  assertTrue(WorkspaceMediaThumbnails.media(a));assertTrue(WorkspaceMediaThumbnails.media(new ProjectFiles.Entry("/","clip.MP4",false,20)));assertFalse(WorkspaceMediaThumbnails.media(new ProjectFiles.Entry("/","clip.mp4",true,0)));assertFalse(WorkspaceMediaThumbnails.media(new ProjectFiles.Entry("/","report.log",false,20)));
  assertFalse(WorkspaceMediaThumbnails.inViewport(0,100,100,200));assertTrue(WorkspaceMediaThumbnails.inViewport(100,200,100,200));assertFalse(WorkspaceMediaThumbnails.inViewport(300,400,100,200));
 }
 @Test public void cachePrunesOnlyOwnedThumbnailsAndStaleTemps()throws Exception{
  File folder=temp.newFolder("cache"),first=new File(folder,"first.jpg"),second=new File(folder,"second.jpg"),stale=new File(folder,"source-old.tmp"),current=new File(folder,"source-current.tmp"),unrelated=new File(folder,"keep.txt");
  try(RandomAccessFile f=new RandomAccessFile(first,"rw")){f.setLength(10L*1024*1024);}try(RandomAccessFile f=new RandomAccessFile(second,"rw")){f.setLength(10L*1024*1024);}assertTrue(stale.createNewFile());assertTrue(current.createNewFile());assertTrue(unrelated.createNewFile());long now=System.currentTimeMillis();assertTrue(first.setLastModified(now-2000));assertTrue(stale.setLastModified(now-11*60*1000));WorkspaceMediaThumbnails.prune(folder,now);assertFalse(first.exists());assertTrue(second.exists());assertFalse(stale.exists());assertTrue(current.exists());assertTrue(unrelated.exists());
 }
 @Test public void imageDecoderProducesSmallPreviewWithoutChangingOriginal()throws Exception{
  File source=temp.newFile("photo.png");Bitmap input=Bitmap.createBitmap(800,400,Bitmap.Config.ARGB_8888);try(OutputStream out=new FileOutputStream(source)){assertTrue(input.compress(Bitmap.CompressFormat.PNG,100,out));}byte[] before=java.nio.file.Files.readAllBytes(source.toPath());Bitmap preview=WorkspaceMediaThumbnails.decode(source,false);assertNotNull(preview);assertEquals(192,preview.getWidth());assertEquals(192,preview.getHeight());assertArrayEquals(before,java.nio.file.Files.readAllBytes(source.toPath()));
 }
}
