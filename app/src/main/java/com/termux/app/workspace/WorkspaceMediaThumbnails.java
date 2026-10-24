package com.termux.app.workspace;

import android.graphics.*;
import androidx.exifinterface.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.os.*;
import android.util.LruCache;
import android.view.View;
import android.widget.*;
import com.termux.R;
import com.termux.app.TermuxActivity;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** A single independent, cancellable reader; only viewport thumbnails are decoded. */
final class WorkspaceMediaThumbnails implements AutoCloseable {
    interface Loader {Bitmap load(ProjectFiles.Entry entry)throws Exception;void cancel();}
    static boolean media(ProjectFiles.Entry e){String mime=ProjectFiles.mime(e.name);return !e.directory&&(mime.startsWith("image/")||mime.startsWith("video/"));}
    static String identity(String environment,ProjectFiles.Entry e){return environment+"\0"+e.path+"\0"+e.size+"\0"+e.modified;}
    static boolean inViewport(int top,int bottom,int scroll,int height){return height>0&&bottom>scroll&&top<scroll+height;}
    private static final class Slot {
        final View row;final ImageView image;final ProjectFiles.Entry entry;final String key;final int generation;
        volatile boolean visible,attempted;boolean shown;
        Slot(View row,ImageView image,ProjectFiles.Entry entry,String key,int generation){this.row=row;this.image=image;this.entry=entry;this.key=key;this.generation=generation;}
    }
    private final Handler main=new Handler(Looper.getMainLooper());private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final AtomicBoolean working=new AtomicBoolean();private final List<Slot> slots=new ArrayList<>();
    private final LruCache<String,Bitmap> memory=new LruCache<String,Bitmap>(6*1024*1024){@Override protected int sizeOf(String key,Bitmap value){return value.getAllocationByteCount();}};
    private final Loader loader;private final String environment;private volatile int generation;private volatile boolean closed;private volatile List<Slot> wanted=Collections.emptyList();private ScrollView viewport;
    WorkspaceMediaThumbnails(TermuxActivity a,WorkspaceStore.Session p){this(ProjectFiles.environment(p),new Source(a,p));}
    WorkspaceMediaThumbnails(String environment,Loader loader){this.environment=environment;this.loader=loader;}
    boolean enabled(){return !environment.equals("ssh");}
    void reset(){generation++;wanted=Collections.emptyList();for(Slot s:slots){s.visible=false;s.image.setImageDrawable(null);}slots.clear();loader.cancel();main.removeCallbacks(refresh);}
    void bind(View row,ImageView image,ProjectFiles.Entry entry){slots.add(new Slot(row,image,entry,identity(environment,entry),generation));}
    void schedule(ScrollView viewport){this.viewport=viewport;main.removeCallbacks(refresh);main.postDelayed(refresh,50);}
    private final Runnable refresh=()->{
        if(closed||viewport==null)return;int top=viewport.getScrollY(),height=viewport.getHeight();List<Slot> next=new ArrayList<>();boolean pending=false;
        for(Slot s:slots){s.visible=next.size()<24&&inViewport(s.row.getTop(),s.row.getBottom(),top,height);
            if(!s.visible){if(s.shown){placeholder(s);s.shown=false;}continue;}
            next.add(s);Bitmap cached=memory.get(s.key);if(cached!=null){s.image.setImageBitmap(cached);s.shown=true;}else if(!s.attempted)pending=true;
        }
        wanted=next;if(pending&&working.compareAndSet(false,true))worker.execute(this::drain);
    };
    private void placeholder(Slot s){s.image.setImageResource(ProjectFiles.mime(s.entry.name).startsWith("video/")?R.drawable.workspace_media_video:R.drawable.workspace_media_photo);s.image.setScaleType(ImageView.ScaleType.CENTER_CROP);}
    private void drain(){try{
        for(Slot s:wanted){if(closed||s.generation!=generation||!s.visible||s.attempted)continue;s.attempted=true;
            Bitmap bitmap=memory.get(s.key);try{if(bitmap==null)bitmap=loader.load(s.entry);}catch(Exception ignored){}
            if(bitmap==null)continue;if(closed||s.generation!=generation)continue;memory.put(s.key,bitmap);final Bitmap ready=bitmap;
            main.post(()->{if(!closed&&s.generation==generation&&s.visible&&s.image.getParent()!=null){s.image.setImageBitmap(ready);s.shown=true;}});
        }
    }finally{working.set(false);main.post(refresh);}}
    @Override public void close(){if(closed)return;closed=true;reset();worker.shutdownNow();memory.evictAll();}

    /** Cache only small thumbnails. Temporary proot originals are removed after each read. */
    private static final class Source implements Loader {
        private final TermuxActivity activity;private final WorkspaceStore.Session profile;private final File cache;private volatile ProjectFiles active;private int epoch;
        Source(TermuxActivity a,WorkspaceStore.Session p){activity=a;profile=p.copy();profile.environment=ProjectFiles.environment(p);profile.prootAlias="";cache=new File(a.getCacheDir(),"workspace-media-thumbnails-v1");}
        @Override public void cancel(){ProjectFiles f;synchronized(this){epoch++;f=active;}if(f!=null)f.close();}
        @Override public Bitmap load(ProjectFiles.Entry e)throws Exception{
            final int current;synchronized(this){current=epoch;}
            if(!cache.isDirectory()&&!cache.mkdirs())throw new IOException("CACHE_UNAVAILABLE");
            String key=ChatAttachments.hash(identity(ProjectFiles.environment(profile),e));File saved=new File(cache,key+".jpg");
            if(saved.isFile()){Bitmap b=BitmapFactory.decodeFile(saved.getPath());if(b!=null){saved.setLastModified(System.currentTimeMillis());return b;}saved.delete();}
            boolean video=ProjectFiles.mime(e.name).startsWith("video/");File original,temp=null;
            try{
                if(ProjectFiles.environment(profile).equals("termux"))original=new File(e.path);
                else {
                    long limit=video?64L*1024*1024:20L*1024*1024;if(e.size<=0||e.size>limit)throw new IOException("THUMBNAIL_LIMIT");
                    temp=File.createTempFile("source-",".tmp",cache);original=temp;
                    try(ProjectFiles files=new ProjectFiles(activity,profile)){synchronized(this){if(epoch!=current)throw new IOException("CANCELLED");active=files;}if(Thread.currentThread().isInterrupted())throw new IOException("CANCELLED");files.export(e,temp,limit);}finally{active=null;}
                }
                Bitmap thumbnail=decode(original,video);if(thumbnail==null)throw new IOException("NO_THUMBNAIL");
                File partial=File.createTempFile("thumb-",".tmp",cache);try{try(OutputStream out=new FileOutputStream(partial)){if(!thumbnail.compress(Bitmap.CompressFormat.JPEG,82,out))throw new IOException("NO_THUMBNAIL");}if(!partial.renameTo(saved))throw new IOException("CACHE_UNAVAILABLE");}finally{partial.delete();}
                prune(cache,System.currentTimeMillis());return thumbnail;
            }finally{if(temp!=null)temp.delete();}
        }
    }
    static Bitmap decode(File original,boolean video)throws Exception {
        Bitmap bitmap;
        if(video){MediaMetadataRetriever r=new MediaMetadataRetriever();try(FileInputStream input=new FileInputStream(original)){
            r.setDataSource(input.getFD());String duration=r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);long ms=duration==null?0:Math.max(0,Long.parseLong(duration));long timestamp=Math.min(1000,ms/3)*1000;
            if(Build.VERSION.SDK_INT>=27)bitmap=r.getScaledFrameAtTime(timestamp,MediaMetadataRetriever.OPTION_CLOSEST_SYNC,256,256);
            else {String w=r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH),h=r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);if(w==null||h==null||(long)Integer.parseInt(w)*Integer.parseInt(h)>16000000L)return null;bitmap=r.getFrameAtTime(timestamp,MediaMetadataRetriever.OPTION_CLOSEST_SYNC);}
        }finally{r.release();}}
        else {
            bitmap=ChatAttachments.bitmap(original,384);
            try{Matrix m=imageOrientation(original);
                if(!m.isIdentity()){Bitmap rotated=Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),m,true);if(rotated!=bitmap)bitmap.recycle();bitmap=rotated;}
            }catch(IOException ignored){}
        }
        return bitmap==null?null:ThumbnailUtils.extractThumbnail(bitmap,192,192,ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
    }
    static Matrix imageOrientation(File original)throws IOException {
        ExifInterface exif=new ExifInterface(original.getPath());
        int o=exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL);Matrix m=new Matrix();
        if(o==2)m.setScale(-1,1);else if(o==3)m.setRotate(180);else if(o==4)m.setScale(1,-1);else if(o==5){m.setRotate(90);m.postScale(1,-1);}else if(o==6)m.setRotate(90);else if(o==7){m.setRotate(90);m.postScale(-1,1);}else if(o==8)m.setRotate(270);
        return m;
    }
    static void prune(File directory,long now){
        File[] files=directory.listFiles();if(files==null)return;Arrays.sort(files,Comparator.comparingLong(File::lastModified));long total=0;
        for(File f:files)if(f.getName().endsWith(".jpg"))total+=f.length();
        for(File f:files){if(f.getName().endsWith(".jpg")&&total>16L*1024*1024){long size=f.length();if(f.delete())total-=size;}else if(f.getName().endsWith(".tmp")&&now-f.lastModified()>10*60*1000)f.delete();}
    }
}
