package com.termux.app.workspace;

import android.content.Context;
import android.net.Uri;
import android.database.Cursor;
import android.provider.OpenableColumns;
import android.graphics.*;
import android.media.MediaMetadataRetriever;
import org.json.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.security.MessageDigest;
import java.util.*;

/** App-private copies of explicitly chosen files. No shell interpolation or remote fetches. */
final class ChatAttachments {
    static final int MAX_COUNT=4, VIDEO_FRAMES=12;
    static final long MAX_FILE=5L*1024*1024*1024, MAX_IMAGE=100L*1024*1024;
    static final String GUEST="/tmp/termux-jobs-codex/chat-attachments/";
    static File root(Context c){return new File(CodexSessionShell.socketDirectory(c.getFilesDir()),"chat-attachments");}
    static boolean supported(WorkspaceStore.Session p){return p!=null&&(p.environment.equals("codex")||p.geminiAgent||p.claudeAgent||p.aiAgent.equals("opencode"));}
    static File root(Context c,WorkspaceStore.Session p){return new File(p!=null&&!p.environment.equals("codex")&&p.geminiAgent?GeminiCli.directory(c.getFilesDir()):p!=null&&!p.environment.equals("codex")&&p.claudeAgent?ClaudeCode.directory(c.getFilesDir()):p!=null&&!p.environment.equals("codex")&&p.aiAgent.equals("opencode")?AiAgents.directory(c.getFilesDir()):CodexSessionShell.socketDirectory(c.getFilesDir()),"chat-attachments");}
    static String prefix(WorkspaceStore.Session p){return p!=null&&!p.environment.equals("codex")&&p.geminiAgent?GeminiCli.GUEST+"/chat-attachments/":p!=null&&!p.environment.equals("codex")&&p.claudeAgent?ClaudeCode.GUEST+"/chat-attachments/":p!=null&&!p.environment.equals("codex")&&p.aiAgent.equals("opencode")?"/tmp/termux-jobs-ai/chat-attachments/":GUEST;}
    static String type(String name)throws IOException{
        String n=name.toLowerCase(Locale.ROOT);
        if(n.matches(".*\\.(png|jpe?g|webp|gif|bmp|heic|heif|avif)$"))return "image";
        if(n.matches(".*\\.(mp4|mkv|webm|mov|m4v|3gp)$"))return "video";
        if(ProjectFiles.mime(n).equals("text/plain")||n.matches(".*\\.(csv|ini|conf|out|ndjson|jsonl|trace)$"))return "file";
        return n.endsWith(".apk")?"apk":"binary";
    }
    static String badge(String name){
        String n=name.toLowerCase(Locale.ROOT);
        for(String suffix:new String[]{"tar.gz","tar.bz2","tar.xz","tar.zst"})if(n.endsWith("."+suffix))return suffix.toUpperCase(Locale.ROOT);
        int dot=n.lastIndexOf('.');String ext=dot>0?n.substring(dot+1):"";
        return ext.matches("[a-z0-9]{1,8}")?ext.toUpperCase(Locale.ROOT):"FILE";
    }
    static long limit(String kind){return kind.equals("image")?MAX_IMAGE:MAX_FILE;}
    static void requireSize(String kind,long bytes)throws IOException{if(bytes<=0||bytes>limit(kind))throw new IOException("FILE_LIMIT");}
    static File inside(File root,String relative)throws IOException{
        if(relative==null||!relative.matches("[a-f0-9]{32}/[A-Za-z0-9_.-]{1,100}"))throw new IOException("INVALID_ATTACHMENT");
        File canonicalRoot=root.getCanonicalFile();File f=new File(canonicalRoot,relative);if(!f.getCanonicalPath().equals(f.getAbsolutePath())||!f.getCanonicalPath().startsWith(canonicalRoot.getPath()+File.separator))throw new IOException("INVALID_ATTACHMENT");return f;
    }
    static String guest(File root,File file)throws IOException{
        String relative=root.getCanonicalFile().toPath().relativize(file.getCanonicalFile().toPath()).toString();inside(root,relative);return GUEST+relative;
    }
    static JSONObject stage(Context c,WorkspaceStore.Session source,ProjectFiles.Entry entry,WorkspaceStore.Session destination)throws Exception{
        String kind=type(entry.name);long max=limit(kind);
        if(entry.directory)throw new IOException("FILE_LIMIT");requireSize(kind,entry.size);
        File root=root(c,destination).getCanonicalFile();if(!root.isDirectory()&&!root.mkdirs())throw new IOException("STORAGE_UNAVAILABLE");
        String id=UUID.randomUUID().toString().replace("-","");File dir=new File(root,id);if(!dir.mkdir())throw new IOException("STORAGE_UNAVAILABLE");
        String ext=entry.name.contains(".")?entry.name.substring(entry.name.lastIndexOf('.')).toLowerCase(Locale.ROOT):".txt";
        if(!ext.matches("\\.[a-z0-9]{1,15}"))ext=".txt";
        File file=inside(root,id+"/original"+ext);
        try{
            try(ProjectFiles files=new ProjectFiles((android.app.Activity)c,source)){files.export(entry,file,max);}
            if(file.length()<=0)throw new IOException("FILE_LIMIT");
            return finish(c,root,destination,entry.name,kind,id,file,false);
        }catch(Exception e){delete(dir);throw e;}
    }
    private static JSONObject finish(Context c,File root,WorkspaceStore.Session destination,String name,String kind,String id,File file,boolean gallery)throws Exception{
        requireSize(kind,file.length());
        File dir=file.getParentFile();
            CodexChatControl.protect(file);
            JSONObject a=new JSONObject().put("id",id).put("name",name).put("type",kind).put("path",guest(root,file).replace(GUEST,prefix(destination))).put("bytes",file.length());
            if(kind.equals("image")){checkImage(file);}
            if(kind.equals("file")){textPreview(file,8192);}
            if(kind.equals("video")){video(root,file,a);JSONArray frames=a.getJSONArray("frames");for(int i=0;i<frames.length();i++){JSONObject frame=frames.getJSONObject(i);frame.put("path",frame.getString("path").replace(GUEST,prefix(destination)));}}
            if(kind.equals("image")&&(gallery||!file.getName().matches("(?i).*\\.(png|jpe?g|webp|gif)$")||!prefix(destination).equals(GUEST)||file.length()>20L*1024*1024)){
                Bitmap decoded=bitmap(file,1280),model=Bitmap.createBitmap(decoded.getWidth(),decoded.getHeight(),Bitmap.Config.RGB_565);Canvas canvas=new Canvas(model);canvas.drawColor(Color.WHITE);canvas.drawBitmap(decoded,0,0,null);File prepared=new File(dir,"model.jpg");
                try(OutputStream out=new FileOutputStream(prepared)){if(!model.compress(Bitmap.CompressFormat.JPEG,85,out))throw new IOException("INVALID_IMAGE");}finally{decoded.recycle();model.recycle();}CodexChatControl.protect(prepared);a.put("modelPath",guest(root,prepared).replace(GUEST,prefix(destination)));
            }
            try(FileOutputStream out=new FileOutputStream(new File(dir,"attachment.json"))){out.write(a.toString().getBytes(StandardCharsets.UTF_8));}
            return a;
    }
    static JSONObject stageUri(Context c,Uri uri,WorkspaceStore.Session destination)throws Exception{
        if(uri==null||!"content".equals(uri.getScheme()))throw new IOException("INVALID_ATTACHMENT");
        String mime=c.getContentResolver().getType(uri),name="";long size=-1;
        try(Cursor cursor=c.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE},null,null,null)){
            if(cursor!=null&&cursor.moveToFirst()){int n=cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME),s=cursor.getColumnIndex(OpenableColumns.SIZE);if(n>=0)name=cursor.getString(n);if(s>=0&&!cursor.isNull(s))size=cursor.getLong(s);}
        }
        String kind=mime!=null&&mime.startsWith("image/")?"image":mime!=null&&mime.startsWith("video/")?"video":"";
        if(kind.isEmpty())throw new IOException("SUPPORTED_TYPES");
        if(name==null||name.isEmpty())name=kind.equals("image")?"photo.jpg":"video.mp4";
        name=name.replaceAll("[\\\\/\\p{Cntrl}]","_");if(name.length()>512)throw new IOException("INVALID_ATTACHMENT");
        if(size>=0)requireSize(kind,size);
        File root=root(c,destination).getCanonicalFile();if(!root.isDirectory()&&!root.mkdirs())throw new IOException("STORAGE_UNAVAILABLE");
        String id=UUID.randomUUID().toString().replace("-","");File dir=new File(root,id);if(!dir.mkdir())throw new IOException("STORAGE_UNAVAILABLE");
        String ext=name.contains(".")?name.substring(name.lastIndexOf('.')).toLowerCase(Locale.ROOT):kind.equals("image")?".jpg":".mp4";if(!ext.matches("\\.[a-z0-9]{1,15}"))ext=kind.equals("image")?".jpg":".mp4";
        File file=inside(root,id+"/original"+ext);
        try{
            try(InputStream in=c.getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(file)){
                if(in==null)throw new IOException("ATTACHMENT_MISSING");byte[] buffer=new byte[65536];long copied=0;int count;
                while((count=in.read(buffer))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();copied+=count;if(copied>limit(kind))throw new IOException("FILE_LIMIT");out.write(buffer,0,count);}
            }
            return finish(c,root,destination,name,kind,id,file,true);
        }catch(Exception error){delete(dir);throw error;}
    }
    static void checkImage(File f)throws IOException{
        BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeFile(f.getPath(),o);
        if(o.outWidth<=0||o.outHeight<=0||(long)o.outWidth*o.outHeight>64000000L)throw new IOException("INVALID_IMAGE");
    }
    static Bitmap bitmap(File f,int edge)throws IOException{
        checkImage(f);BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeFile(f.getPath(),o);int sample=1;while(Math.max(o.outWidth,o.outHeight)/sample>edge)sample*=2;
        o.inJustDecodeBounds=false;o.inSampleSize=sample;Bitmap b=BitmapFactory.decodeFile(f.getPath(),o);if(b==null)throw new IOException("INVALID_IMAGE");return b;
    }
    static String textPreview(File f,int max)throws IOException{
        byte[] b=new byte[(int)Math.min(f.length(),max)];try(InputStream in=new FileInputStream(f)){int n=0,r;while(n<b.length&&(r=in.read(b,n,b.length-n))>0)n+=r;}
        for(byte v:b)if(v==0)throw new IOException("INVALID_TEXT");
        // A sample may end inside a UTF-8 sequence; the complete file remains intact.
        return StandardCharsets.UTF_8.decode(ByteBuffer.wrap(b)).toString();
    }
    private static void video(File root,File file,JSONObject a)throws Exception{
        MediaMetadataRetriever r=new MediaMetadataRetriever();try{
            r.setDataSource(file.getPath());long duration=Long.parseLong(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
            int w=Integer.parseInt(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)),h=Integer.parseInt(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
            if(duration<=0||w<=0||h<=0||(long)w*h>64000000L)throw new IOException("INVALID_VIDEO");
            JSONArray frames=new JSONArray();int count=(int)Math.min(VIDEO_FRAMES,Math.max(1,(duration+999)/1000));
            float scale=Math.min(1f,1280f/Math.max(w,h));int tw=Math.max(1,Math.round(w*scale)),th=Math.max(1,Math.round(h*scale));
            for(int i=0;i<count;i++){
                if(Thread.currentThread().isInterrupted())throw new IOException("CANCELLED");
                long ms=count==1?0:Math.min(duration-1,i*(duration-1)/(count-1));
                Bitmap b=android.os.Build.VERSION.SDK_INT>=27?r.getScaledFrameAtTime(ms*1000,MediaMetadataRetriever.OPTION_CLOSEST,tw,th):r.getFrameAtTime(ms*1000,MediaMetadataRetriever.OPTION_CLOSEST);
                if(b==null)throw new IOException("INVALID_VIDEO");
                if(Math.max(b.getWidth(),b.getHeight())>1280){Bitmap small=Bitmap.createScaledBitmap(b,tw,th,true);if(small!=b)b.recycle();b=small;}
                File frame=new File(file.getParentFile(),"frame-"+i+".jpg");try(OutputStream out=new FileOutputStream(frame)){if(!b.compress(Bitmap.CompressFormat.JPEG,85,out))throw new IOException("INVALID_VIDEO");}finally{b.recycle();}
                CodexChatControl.protect(frame);frames.put(new JSONObject().put("path",guest(root,frame)).put("timeMs",ms));
            }
            a.put("durationMs",duration).put("frames",frames);
        }finally{r.release();}
    }
    static void discard(Context c,JSONObject a){try{File storage=root(c);String path=a.optString("path");for(String kind:new String[]{"gemini","claude","opencode"}){WorkspaceStore.Session p=new WorkspaceStore.Session();p.geminiAgent=kind.equals("gemini");p.claudeAgent=kind.equals("claude");p.aiAgent=kind.equals("opencode")?kind:"";if(path.startsWith(prefix(p)))storage=root(c,p);}File f=inside(storage,a.getString("id")+"/attachment.json");delete(f.getParentFile());}catch(Exception ignored){}}
    static void delete(File f){try{if(f.isDirectory()&&f.getCanonicalFile().equals(f.getAbsoluteFile())){File[] files=f.listFiles();if(files!=null)for(File child:files)delete(child);}}catch(IOException ignored){}f.delete();}
    static String hash(String s)throws Exception{byte[] b=MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));StringBuilder h=new StringBuilder();for(byte v:b)h.append(String.format(Locale.ROOT,"%02x",v&255));return h.toString();}
    static String fileSha256(File file)throws Exception{
        MessageDigest digest=MessageDigest.getInstance("SHA-256");byte[] buffer=new byte[65536];
        try(InputStream in=new FileInputStream(file)){int n;while((n=in.read(buffer))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();digest.update(buffer,0,n);}}
        return WorkspaceUpdates.hex(digest.digest());
    }
    static File materialize(Context c,WorkspaceStore.Session profile,JSONObject a)throws Exception{
        String path=a.getString("path");File root=root(c,profile);String guestPrefix=prefix(profile);boolean apk=a.optString("type").equals("apk");
        File managed=null;if(path.startsWith(guestPrefix)){managed=inside(root,path.substring(guestPrefix.length()));if(!managed.isFile())throw new IOException("ATTACHMENT_MISSING");requireSize(a.optString("type","image"),managed.length());if(!apk)return managed;}
        if(!path.startsWith("/")||path.contains("\n")||path.contains("\r")||path.contains("\0"))throw new IOException("INVALID_ATTACHMENT");
        if(apk&&!path.toLowerCase(Locale.ROOT).endsWith(".apk"))throw new IOException("INVALID_ATTACHMENT");
        boolean image=a.optString("type").equals("image")||!a.has("type");
        File cache=new File(c.getCacheDir(),apk?"chat-apk-artifacts":image?"chat-image-previews":"chat-file-previews");if(!cache.isDirectory()&&!cache.mkdirs())throw new IOException("STORAGE_UNAVAILABLE");
        File f=new File(cache,hash(ProjectFiles.environment(profile)+"\0"+a.optString("cacheKey")+"\0"+path)+(apk?".apk":image?".image":".file"));if(f.isFile())return f;
        synchronized(ChatAttachments.class){if(f.isFile())return f;File temp=File.createTempFile("image-",".tmp",cache);try{
            if(managed!=null){if(managed.length()>MAX_FILE)throw new IOException("FILE_LIMIT");try(InputStream in=new FileInputStream(managed);OutputStream out=new FileOutputStream(temp)){byte[] buffer=new byte[65536];int n;long count=0;while((n=in.read(buffer))!=-1){count+=n;if(count>MAX_FILE)throw new IOException("FILE_LIMIT");out.write(buffer,0,n);}}}
            else try(ProjectFiles files=new ProjectFiles((android.app.Activity)c,profile)){files.export(new ProjectFiles.Entry(new File(path).getParent(),new File(path).getName(),false,0),temp,image?MAX_IMAGE:MAX_FILE);}
            if(!image){if(temp.length()<=0)throw new IOException("FILE_LIMIT");}else checkImage(temp);if(!temp.renameTo(f))throw new IOException("STORAGE_UNAVAILABLE");
            File[] cached=cache.listFiles();if(cached!=null){Arrays.sort(cached,Comparator.comparingLong(File::lastModified));long total=0;for(File x:cached)total+=x.length();for(File x:cached)if(total>(image?64L:512L)*1024*1024&&!x.equals(f)){long n=x.length();if(x.delete())total-=n;}}
        }finally{temp.delete();}}return f;
    }
}
