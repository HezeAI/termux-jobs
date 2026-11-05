package com.termux.app.workspace;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Copies the materialized original; no bitmap encoding, model request or remote download. */
final class ImageGalleryExport {
    static final String ALBUM="Pictures/TermuxJobs/";
    static final class Result {
        final Uri uri;final String name;
        Result(Uri uri,String name){this.uri=uri;this.name=name;}
    }
    static final class Failure extends IOException {
        final String code;
        Failure(String code,Throwable cause){super(code,cause);this.code=code;}
    }
    static final class Description {
        final String name,mime;
        Description(String name,String mime){this.name=name;this.mime=mime;}
    }
    interface Store {
        Uri insert(ContentValues values)throws Exception;
        OutputStream open(Uri uri)throws Exception;
        Description describe(Uri uri)throws Exception;
        int publish(Uri uri)throws Exception;
        boolean remove(Uri uri)throws Exception;
    }
    static final class AndroidStore implements Store {
        final ContentResolver resolver;final Uri collection;
        AndroidStore(ContentResolver resolver,boolean modern){this.resolver=resolver;collection=modern?MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY):MediaStore.Images.Media.EXTERNAL_CONTENT_URI;}
        public Uri insert(ContentValues values){return resolver.insert(collection,values);}
        public OutputStream open(Uri uri)throws IOException{return resolver.openOutputStream(uri,"w");}
        public Description describe(Uri uri){
            try(Cursor c=resolver.query(uri,new String[]{MediaStore.Images.Media.DISPLAY_NAME,MediaStore.Images.Media.MIME_TYPE},null,null,null)){
                if(c==null||!c.moveToFirst())return null;
                return new Description(c.getString(0),c.getString(1));
            }
        }
        public int publish(Uri uri){ContentValues values=new ContentValues();values.put(MediaStore.Images.Media.IS_PENDING,0);return resolver.update(uri,values,null,null);}
        public boolean remove(Uri uri){
            if(resolver.delete(uri,null,null)>0)return true;
            try(Cursor c=resolver.query(uri,new String[]{MediaStore.Images.Media._ID},null,null,null)){return c!=null&&!c.moveToFirst();}
        }
    }
    static boolean candidate(String name,String kind){return ProjectFiles.mime(name).startsWith("image/")||kind.equals("image");}
    static Result save(Context context,File original,String name,BooleanSupplier cancelled)throws IOException{
        boolean modern=Build.VERSION.SDK_INT>=29;
        File legacy=modern?null:new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"TermuxJobs");
        return save(new AndroidStore(context.getContentResolver(),modern),modern,legacy,original,name,cancelled);
    }
    static Result save(Store store,boolean modern,File legacy,File original,String displayName,BooleanSupplier cancelled)throws IOException{
        Plan plan=plan(original,displayName);check(cancelled);
        Uri uri=null;File legacyFile=null;boolean committed=false;Exception failure=null;
        try{
            ContentValues values=new ContentValues();values.put(MediaStore.Images.Media.MIME_TYPE,plan.mime);
            if(modern){values.put(MediaStore.Images.Media.DISPLAY_NAME,plan.name);values.put(MediaStore.Images.Media.RELATIVE_PATH,ALBUM);values.put(MediaStore.Images.Media.IS_PENDING,1);}
            else{
                try(InputStream in=new FileInputStream(original)){legacyFile=ProjectFileExport.save(in,legacy,plan.name,Long.MAX_VALUE,cancelled);}
                check(cancelled);values.put(MediaStore.Images.Media.DISPLAY_NAME,legacyFile.getName());values.put(MediaStore.Images.Media.DATA,legacyFile.getAbsolutePath());
            }
            uri=store.insert(values);if(uri==null)throw new IOException("MEDIA_INSERT_FAILED");
            Description actual=store.describe(uri);
            if(actual==null||actual.name==null||actual.mime==null||!actual.mime.equalsIgnoreCase(plan.mime)||!actual.name.toLowerCase(Locale.ROOT).endsWith(plan.suffix.toLowerCase(Locale.ROOT)))throw new Failure("UNSUPPORTED_IMAGE",null);
            if(modern){
                long expected=original.length();
                try(InputStream in=new FileInputStream(original);OutputStream output=store.open(uri)){
                    if(output==null)throw new IOException("MEDIA_STREAM_UNAVAILABLE");
                    byte[] buffer=new byte[32768];long written=0;int count;
                    while(true){check(cancelled);count=in.read(buffer);if(count<0)break;check(cancelled);output.write(buffer,0,count);written+=count;}
                    if(written!=expected)throw new IOException("ORIGINAL_CHANGED");
                }
                check(cancelled);if(store.publish(uri)!=1)throw new IOException("MEDIA_PUBLISH_FAILED");
            }
            committed=true;return new Result(uri,actual.name);
        }catch(Exception error){failure=error;}
        finally{
            if(!committed){boolean cleaned=true;
                if(uri!=null)try{cleaned=store.remove(uri);}catch(Exception error){cleaned=false;}
                if(legacyFile!=null&&legacyFile.exists()&&!legacyFile.delete())cleaned=false;
                if(!cleaned)throw new Failure("CLEANUP_FAILED",failure);
            }
        }
        if(failure instanceof Failure)throw (Failure)failure;
        if(failure instanceof InterruptedIOException)throw new Failure("CANCELLED",failure);
        if(failure instanceof IllegalArgumentException||failure instanceof UnsupportedOperationException)throw new Failure("UNSUPPORTED_IMAGE",failure);
        throw new Failure("SAVE_FAILED",failure);
    }
    private static void check(BooleanSupplier cancelled)throws InterruptedIOException{if(cancelled.getAsBoolean()||Thread.currentThread().isInterrupted())throw new InterruptedIOException("IMAGE_SAVE_CANCELLED");}
    static final class Plan {final String name,mime,suffix;Plan(String name,String mime,String suffix){this.name=name;this.mime=mime;this.suffix=suffix;}}
    static Plan plan(File file,String supplied)throws IOException{
        if(!file.isFile()||file.length()==0)throw new IOException("IMAGE_UNAVAILABLE");
        byte[] header=new byte[256];int length;try(InputStream in=new FileInputStream(file)){length=in.read(header);}
        String actual=sniff(header,length),declared=ProjectFiles.mime(supplied);
        if(actual.isEmpty())throw new Failure("UNSUPPORTED_IMAGE",null);
        boolean heif=Arrays.asList("image/heic","image/heif").contains(actual)&&Arrays.asList("image/heic","image/heif").contains(declared);
        if(declared.startsWith("image/")&&!declared.equals(actual)&&!heif)throw new Failure("UNSUPPORTED_IMAGE",null);
        String safe=supplied.replaceAll("[\\\\/\\p{Cntrl}]","_");int dot=safe.lastIndexOf('.');String suffix;
        if(declared.startsWith("image/"))suffix=safe.substring(dot);
        else{if(dot>0)throw new Failure("UNSUPPORTED_IMAGE",null);declared=actual;suffix=actual.equals("image/jpeg")?".jpg":actual.equals("image/heif")?".heif":actual.equals("image/heic")?".heic":"."+actual.substring(6);}
        String stem=dot>0?safe.substring(0,dot):safe;if(stem.isEmpty()||stem.equals(".")||stem.equals("..")||stem.equals(suffix))stem="image";
        while((stem+suffix).getBytes(StandardCharsets.UTF_8).length>200)stem=stem.substring(0,stem.offsetByCodePoints(stem.length(),-1));
        return new Plan(stem+suffix,declared,suffix);
    }
    static String sniff(byte[] b,int length){
        if(length>=8&&(b[0]&255)==137&&b[1]=='P'&&b[2]=='N'&&b[3]=='G'&&b[4]==13&&b[5]==10&&b[6]==26&&b[7]==10)return "image/png";
        if(length>=3&&(b[0]&255)==255&&(b[1]&255)==216&&(b[2]&255)==255)return "image/jpeg";
        if(length>=6&&(new String(b,0,6,StandardCharsets.US_ASCII).equals("GIF87a")||new String(b,0,6,StandardCharsets.US_ASCII).equals("GIF89a")))return "image/gif";
        if(length>=12&&new String(b,0,4,StandardCharsets.US_ASCII).equals("RIFF")&&new String(b,8,4,StandardCharsets.US_ASCII).equals("WEBP"))return "image/webp";
        if(length>=2&&b[0]=='B'&&b[1]=='M')return "image/bmp";
        if(length>=12&&new String(b,4,4,StandardCharsets.US_ASCII).equals("ftyp")){
            Set<String> brands=new HashSet<>();brands.add(new String(b,8,4,StandardCharsets.US_ASCII));for(int i=16;i+4<=length;i+=4)brands.add(new String(b,i,4,StandardCharsets.US_ASCII));
            if(brands.contains("avif")||brands.contains("avis"))return "image/avif";
            for(String brand:new String[]{"heic","heix","hevc","hevx","heim","heis","hevm","hevs"})if(brands.contains(brand))return "image/heic";
            if(brands.contains("mif1")||brands.contains("msf1"))return "image/heif";
        }
        return "";
    }
}
