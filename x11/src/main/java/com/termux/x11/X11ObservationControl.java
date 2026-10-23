package com.termux.x11;

import android.util.AtomicFile;
import java.io.*;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

/** Private fixed-path data, never a PID or a shell command. */
final class X11ObservationControl {
    static void write(File base,String token,int serial,boolean enabled)throws IOException{
        if(!token.matches("[a-f0-9-]{36}")||serial<0)throw new IOException("Invalid observation identity");
        if(Files.isSymbolicLink(base.toPath()))throw new IOException("Observation directory is a link");
        File dest=new File(base,"x11-observation-control");
        for(String suffix:new String[]{"",".new",".bak"})if(Files.isSymbolicLink(new File(dest.getPath()+suffix).toPath()))throw new IOException("Observation control is a link");
        AtomicFile file=new AtomicFile(dest);FileOutputStream out=null;
        try{out=file.startWrite();out.write((token+" "+serial+" "+(enabled?"on":"off")+"\n").getBytes(StandardCharsets.US_ASCII));file.finishWrite(out);}
        catch(IOException error){if(out!=null)file.failWrite(out);throw error;}
    }
    interface Status { void accept(int serial,String value); }
    private final String token;
    private final Status status;
    private final StringBuilder line=new StringBuilder();private boolean oversized;
    X11ObservationControl(String token,Status status){this.token=token;this.status=status;}
    void feed(String chunk){for(int i=0;i<chunk.length();i++){char c=chunk.charAt(i);if(c=='\n'){if(!oversized)parse(line.toString());line.setLength(0);oversized=false;}else if(!oversized){if(line.length()<512)line.append(c);else{line.setLength(0);oversized=true;}}}}
    private void parse(String text){
        String[] p=text.trim().split("\\s+");
        if(p.length!=4||!p[0].equals("JOBS_X11_OBSERVATION")||!p[1].equals(token)||!p[2].matches("[0-9]{1,10}")||!(p[3].equals("active")||p[3].equals("stopped")||p[3].equals("error")))return;
        try{int serial=Integer.parseInt(p[2]);if(serial>=0)status.accept(serial,p[3]);}catch(NumberFormatException ignored){}
    }
}
