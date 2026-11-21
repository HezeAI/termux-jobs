package com.termux.app.workspace;
import android.content.*;
import android.net.Uri;
import android.provider.MediaStore;
import android.os.Environment;
import java.io.*;
/** Publish only a newly created pending Download entry; never replace an existing file. */
final class AgentActionExport {
 static void save(Context context,File source)throws IOException{
  if(android.os.Build.VERSION.SDK_INT<29){
   File directory=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
   if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("Download unavailable");
   File destination=File.createTempFile("termux-jobs-action-",".txt",directory);
   try(InputStream in=new FileInputStream(source);OutputStream out=new FileOutputStream(destination)){byte[] block=new byte[65536];int n;while((n=in.read(block))!=-1)out.write(block,0,n);}catch(IOException failure){destination.delete();throw failure;}return;
  }
  ContentResolver resolver=context.getContentResolver();ContentValues values=new ContentValues();values.put(MediaStore.Downloads.DISPLAY_NAME,source.getName());values.put(MediaStore.Downloads.MIME_TYPE,mime(source));values.put(MediaStore.Downloads.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS);values.put(MediaStore.Downloads.IS_PENDING,1);Uri uri=resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);if(uri==null)throw new IOException("Не удалось создать файл в Download");boolean complete=false;
  try{try(InputStream in=new FileInputStream(source);OutputStream out=resolver.openOutputStream(uri,"w")){if(out==null)throw new IOException("Нет доступа к Download");byte[] block=new byte[65536];int n;while((n=in.read(block))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();out.write(block,0,n);}}values.clear();values.put(MediaStore.Downloads.IS_PENDING,0);if(resolver.update(uri,values,null,null)!=1)throw new IOException("Сохранение не подтверждено");complete=true;}finally{if(!complete)resolver.delete(uri,null,null);}
 }
 static String mime(File f){String n=f.getName();int dot=n.lastIndexOf('.');String type=dot<0?null:android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(n.substring(dot+1).toLowerCase(java.util.Locale.ROOT));return type==null?"application/octet-stream":type;}
}
