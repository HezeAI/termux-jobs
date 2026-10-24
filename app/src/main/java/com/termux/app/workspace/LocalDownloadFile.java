package com.termux.app.workspace;

import com.termux.app.TermuxActivity;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.shell.TermuxShellUtils;
import java.io.*;
import java.util.*;

/** Exclusive destination stream in the selected local environment; publishes only after SSH succeeds. */
final class LocalDownloadFile implements AutoCloseable {
    final String path,temporary;
    private final ProjectFiles files;private final WorkspaceStore.Session location;
    private Process child;private boolean owned,committed;final OutputStream stream;
    static String writeScript(String directory,String name){return "umask 077; cd -- "+ProjectFiles.target(directory)+" || exit 2; set -C; exec 3> "+SessionShell.quote(name)+" || exit 3; printf 'READY\\n'; cat >&3";}
    static String publishScript(String temporary,String target){return "test -f "+ProjectFiles.target(temporary)+" && ! test -e "+ProjectFiles.target(target)+" && ! test -L "+ProjectFiles.target(target)+" && mv -T -n -- "+ProjectFiles.target(temporary)+" "+ProjectFiles.target(target)+" && ! test -e "+ProjectFiles.target(temporary);}
    LocalDownloadFile(TermuxActivity activity,WorkspaceStore.Session source,String directory,String name)throws Exception{
        if(!ServerSftp.validName(name)||!directory.startsWith("/"))throw new IOException("INVALID_DESTINATION");
        location=source.copy();files=new ProjectFiles(activity,location);path=(directory.equals("/")?"/":directory+"/")+name;temporary=(directory.equals("/")?"/":directory+"/")+".termux-jobs-download-"+UUID.randomUUID()+".part";
        ProcessBuilder builder=new ProcessBuilder(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH+"/bash","--noprofile","--norc","-c","exec "+ProjectFiles.command(location,writeScript(directory,new File(temporary).getName())));
        builder.directory(new File(TermuxConstants.TERMUX_HOME_DIR_PATH));builder.environment().clear();
        for(String item:TermuxShellUtils.buildEnvironment(activity,false,TermuxConstants.TERMUX_HOME_DIR_PATH)){int eq=item.indexOf('=');if(eq>0)builder.environment().put(item.substring(0,eq),item.substring(eq+1));}
        builder.redirectError(new File("/dev/null"));
        try{child=builder.start();java.util.concurrent.ScheduledExecutorService watchdog=java.util.concurrent.Executors.newSingleThreadScheduledExecutor();java.util.concurrent.ScheduledFuture<?> timeout=watchdog.schedule(child::destroy,15,java.util.concurrent.TimeUnit.SECONDS);
            try{InputStream ready=child.getInputStream();byte[] marker={'R','E','A','D','Y','\n'};for(byte expected:marker)if(ready.read()!=expected)throw new IOException("DESTINATION_UNAVAILABLE");owned=true;stream=child.getOutputStream();}finally{timeout.cancel(false);watchdog.shutdown();}}
        catch(Exception error){if(child!=null)child.destroy();files.close();throw error;}
    }
    void finish()throws Exception{stream.close();if(child.waitFor()!=0)throw new IOException("DESTINATION_WRITE_FAILED");files.localOperation(publishScript(temporary,path));committed=true;}
    void cancel(){if(child!=null)child.destroy();}
    @Override public void close()throws Exception{
        cancel();try{try{stream.close();}catch(IOException ignored){}if(owned&&!committed)files.localOperation("rm -f -- "+ProjectFiles.target(temporary));}finally{files.close();}
    }
}
