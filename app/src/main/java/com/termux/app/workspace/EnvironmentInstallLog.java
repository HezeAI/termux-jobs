package com.termux.app.workspace;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Date;

/** One bounded, private last-attempt report per environment. No terminal/session data. */
final class EnvironmentInstallLog {
    static final int LIMIT=262144;
    static final String TRUNCATED="[Earlier installer output omitted: log limit reached]\n";
    static final class Capture {
        private final StringBuilder text=new StringBuilder();private boolean truncated;
        void append(char[] chars,int count){text.append(chars,0,count);if(text.length()>LIMIT){text.delete(0,text.length()-LIMIT);truncated=true;}}
        String snapshot(){return (truncated?TRUNCATED:"")+text;}
        String recent(){return text.substring(Math.max(0,text.length()-12000));}
    }
    static String clean(String raw){
        String text=raw.replaceAll("\\x1B(?:\\[[0-?]*[ -/]*[@-~]|\\][^\\x07\\x1B]*(?:\\x07|\\x1B\\\\))","")
            .replace('\r','\n').replaceAll("[\\p{Cc}&&[^\\n\\t]]","");
        text=text.replaceAll("(?im)-----BEGIN [^\\n]*PRIVATE KEY-----[\\s\\S]*?(?:-----END [^\\n]*PRIVATE KEY-----|\\z)","[private key redacted]");
        text=text.replaceAll("(?i)\\b[a-z][a-z0-9+.-]*://[^\\s'\"<>]+","[url]");
        text=text.replaceAll("(?im)\\b(?:authorization|proxy-authorization|cookie|set-cookie|password|passwd|passphrase|access[_-]?token|refresh[_-]?token|token|api[_-]?key|client[_-]?secret|secret)\\b\\s*[:=]\\s*[^\\n]*","[credential redacted]");
        return text.replaceAll("\\b(?:gh[pousr]_[A-Za-z0-9_]+|github[_]pat[_][A-Za-z0-9_]+|sk-[A-Za-z0-9_-]{16,})","[token redacted]");
    }
    static String report(String alias,int code,String output){
        return "Termux Job's environment installation\nEnvironment: "+alias+"\nExit status: "+code+"\nRecorded: "+new Date()+"\n\n"+clean(output.length()>LIMIT+TRUNCATED.length()?TRUNCATED+output.substring(output.length()-LIMIT):output);
    }
    private static Path path(File files,String alias)throws IOException{
        if(!WorkspaceLocalFilePicker.validAlias(alias)||alias.equals("termux"))throw new IOException("INVALID_ALIAS");
        File file=new File(files.getCanonicalFile(),".environment-control/logs/"+alias+".txt");
        for(File part=file;!part.equals(files.getCanonicalFile());part=part.getParentFile())if(Files.isSymbolicLink(part.toPath()))throw new IOException("LOG_LINK");
        if(!file.getCanonicalPath().equals(file.getAbsolutePath()))throw new IOException("LOG_PATH");
        return file.toPath();
    }
    static boolean exists(File files,String alias){try{return Files.isRegularFile(path(files,alias),LinkOption.NOFOLLOW_LINKS);}catch(IOException e){return false;}}
    static void save(File files,String alias,String text)throws IOException{
        Path dest=path(files,alias);Files.createDirectories(dest.getParent());path(files,alias);
        Path temp=Files.createTempFile(dest.getParent(),"attempt-",".tmp",PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        try{Files.write(temp,text.getBytes(StandardCharsets.UTF_8));Files.move(temp,dest,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}finally{Files.deleteIfExists(temp);}
    }
    static String read(File files,String alias)throws IOException{
        Path file=path(files,alias);if(!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)||Files.size(file)>4L*LIMIT+4096)throw new IOException("LOG_UNAVAILABLE");
        try(InputStream in=Files.newInputStream(file,LinkOption.NOFOLLOW_LINKS);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] bytes=new byte[8192];int n;while((n=in.read(bytes))!=-1){if(out.size()+n>4L*LIMIT+4096)throw new IOException("LOG_LIMIT");out.write(bytes,0,n);}return new String(out.toByteArray(),StandardCharsets.UTF_8);
        }
    }
    static void clear(File files,String alias)throws IOException{Files.deleteIfExists(path(files,alias));}
}
