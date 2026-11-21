package com.termux.app.workspace;

import android.content.Context;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Exports only generated historical text, never a path from an agent event. */
final class ActionDiffExport {
    static File prepare(File cache,String text,String extension)throws IOException {
        if(!extension.equals("json")&&!extension.equals("diff")&&!extension.equals("txt"))throw new IOException("Unsupported historical output format");
        File directory=new File(cache,"action-diff-exports");
        if(Files.isSymbolicLink(directory.toPath())||(!directory.isDirectory()&&!directory.mkdirs()))throw new IOException("Export directory unavailable");
        File temporary=File.createTempFile("termux-jobs-action-","."+extension,directory);boolean complete=false;
        try{
            temporary.setReadable(false,false);temporary.setWritable(false,false);temporary.setReadable(true,true);temporary.setWritable(true,true);
            try(FileOutputStream output=new FileOutputStream(temporary);Writer writer=new BufferedWriter(new OutputStreamWriter(output,StandardCharsets.UTF_8))){writer.write(text);writer.flush();output.getFD().sync();}
            complete=true;return temporary;
        }finally{if(!complete)temporary.delete();}
    }
    static void publish(Context context,File generated)throws IOException {AgentActionExport.save(context,generated);}
    private ActionDiffExport(){}
}
