package com.termux.app.workspace;

import java.io.*;
import java.nio.file.*;
import java.util.function.BooleanSupplier;

/** Exclusive creation in the chosen Download directory, without following a destination link. */
final class ProjectFileExport {
    private ProjectFileExport(){}
    static File save(InputStream input,File directory,String name,long limit,BooleanSupplier cancelled)throws IOException{
        if(name==null||name.isEmpty()||name.equals(".")||name.equals("..")||name.indexOf('/')>=0
            ||name.indexOf('\0')>=0||name.indexOf('\n')>=0||name.indexOf('\r')>=0||limit<0)
            throw new IOException("INVALID_EXPORT_NAME");
        check(cancelled);
        if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("DOWNLOAD_UNAVAILABLE");
        int dot=name.lastIndexOf('.');String stem=dot>0?name.substring(0,dot):name,suffix=dot>0?name.substring(dot):"";
        for(int number=1;number<=10000;number++){
            check(cancelled);
            File destination=new File(directory,number==1?name:stem+"-"+number+suffix);
            OutputStream stream;
            try{stream=Files.newOutputStream(destination.toPath(),StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE,LinkOption.NOFOLLOW_LINKS);}
            catch(FileAlreadyExistsException collision){continue;}
            boolean complete=false;
            try{
                try(OutputStream output=stream){
                    byte[] buffer=new byte[32768];long total=0;int count;
                    while(true){check(cancelled);count=input.read(buffer);if(count<0)break;check(cancelled);
                        total+=count;if(total>limit)throw new IOException("EXPORT_TOO_LARGE");output.write(buffer,0,count);}
                    check(cancelled);
                }
                complete=true;return destination;
            }finally{if(!complete)destination.delete();}
        }
        throw new IOException("TOO_MANY_EXPORT_COLLISIONS");
    }
    private static void check(BooleanSupplier cancelled)throws InterruptedIOException{
        if(cancelled.getAsBoolean()||Thread.currentThread().isInterrupted())throw new InterruptedIOException("EXPORT_CANCELLED");
    }
}
