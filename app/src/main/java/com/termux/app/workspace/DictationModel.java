package com.termux.app.workspace;

import android.content.Context;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.Comparator;
import java.util.function.BooleanSupplier;
import java.util.zip.*;

/** Verified app asset to an owned, atomic cache directory. Never installs a system service. */
final class DictationModel {
    static final String ASSET="dictation-model-ru.zip", ROOT="vosk-model-small-ru-0.22";
    static final String SHA256="961d5ff98a17f4aa6de69864d0aa71fa5bac682301d2b5d17a3f24c5c99a46d4";
    private DictationModel(){}
    static synchronized File prepare(Context context,BooleanSupplier cancelled)throws IOException{
        Path cache=new File(context.getCacheDir(),"dictation-models").toPath();
        Path ready=cache.resolve("ru-0.22-"+SHA256.substring(0,12));
        if(valid(ready,SHA256))return ready.resolve(ROOT).toFile();
        try(InputStream source=context.getAssets().open(ASSET)){return extract(source,cache,SHA256,cancelled);}
    }
    static File extract(InputStream source,Path cache,String expected,BooleanSupplier cancelled)throws IOException{
        Files.createDirectories(cache);
        if(Files.isSymbolicLink(cache))throw new IOException("Model cache is a symbolic link");
        Path destination=cache.resolve("ru-0.22-"+expected.substring(0,12));
        if(valid(destination,expected))return destination.resolve(ROOT).toFile();
        if(Files.exists(destination,LinkOption.NOFOLLOW_LINKS))throw new IOException("Model cache is incomplete");
        Path temporary=Files.createTempDirectory(cache,"preparing-");boolean moved=false;
        try{
            MessageDigest digest;
            try{digest=MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException e){throw new IOException(e);}
            try(DigestInputStream checked=new DigestInputStream(source,digest);ZipInputStream zip=new ZipInputStream(checked)){
                ZipEntry entry;long expanded=0;int count=0;byte[] block=new byte[64*1024];
                while((entry=zip.getNextEntry())!=null){
                    if(cancelled.getAsBoolean())throw new InterruptedIOException("Dictation cancelled");
                    if(++count>64)throw new IOException("Too many model files");
                    String name=entry.getName();Path target=temporary.resolve(name).normalize();
                    if(!name.startsWith(ROOT+"/")||name.contains("\\")||!target.startsWith(temporary))throw new IOException("Unsafe model path");
                    if(entry.isDirectory()){Files.createDirectories(target);continue;}
                    Files.createDirectories(target.getParent());
                    try(OutputStream output=Files.newOutputStream(target,StandardOpenOption.CREATE_NEW)){
                        int n;while((n=zip.read(block))!=-1){
                            if(cancelled.getAsBoolean())throw new InterruptedIOException("Dictation cancelled");
                            expanded+=n;if(expanded>100L*1024*1024)throw new IOException("Model size limit");output.write(block,0,n);
                        }
                    }
                }
                // ZipInputStream stops at the central directory. Hash the remaining archive too.
                while(checked.read(block)!=-1)if(cancelled.getAsBoolean())throw new InterruptedIOException("Dictation cancelled");
            }
            StringBuilder hex=new StringBuilder();for(byte b:digest.digest())hex.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
            if(!expected.equals(hex.toString()))throw new IOException("Model SHA-256 mismatch");
            if(!modelFiles(temporary.resolve(ROOT)))throw new IOException("Incomplete model archive");
            Files.write(temporary.resolve("ready.sha256"),expected.getBytes(StandardCharsets.US_ASCII),StandardOpenOption.CREATE_NEW);
            if(cancelled.getAsBoolean())throw new InterruptedIOException("Dictation cancelled");
            Files.move(temporary,destination,StandardCopyOption.ATOMIC_MOVE);moved=true;
            return destination.resolve(ROOT).toFile();
        }finally{
            if(!moved)try(java.util.stream.Stream<Path> paths=Files.walk(temporary)){
                for(Path p:(Iterable<Path>)paths.sorted(Comparator.reverseOrder())::iterator)Files.deleteIfExists(p);
            }
        }
    }
    private static boolean valid(Path directory,String expected)throws IOException{
        Path marker=directory.resolve("ready.sha256");
        return !Files.isSymbolicLink(directory)&&Files.isRegularFile(marker,LinkOption.NOFOLLOW_LINKS)
            &&new String(Files.readAllBytes(marker),StandardCharsets.US_ASCII).equals(expected)&&modelFiles(directory.resolve(ROOT));
    }
    private static boolean modelFiles(Path root){
        for(String file:new String[]{"am/final.mdl","conf/model.conf","graph/Gr.fst","graph/HCLr.fst"})
            if(!Files.isRegularFile(root.resolve(file),LinkOption.NOFOLLOW_LINKS))return false;
        return true;
    }
}
