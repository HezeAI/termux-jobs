package com.termux.app.workspace;

import org.junit.Test;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

public class TextFileEditsTest {
    private byte[] run(String script,byte[] input)throws Exception{
        Process p=new ProcessBuilder("bash","--noprofile","--norc","-c",script).redirectError(ProcessBuilder.Redirect.INHERIT).start();try(OutputStream out=p.getOutputStream()){if(input!=null)out.write(input);}ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=p.getInputStream().read(b))!=-1)out.write(b,0,n);assertTrue(p.waitFor(15,TimeUnit.SECONDS));assertEquals(0,p.exitValue());return out.toByteArray();
    }
    private TextFileEdits.Snapshot read(Path p)throws Exception{return TextFileEdits.parse(p.toString(),run(TextFileEdits.readScript(p.toString()),null));}
    private void cleanup(Path dir)throws Exception{try(java.util.stream.Stream<Path> files=Files.list(dir)){for(Path p:(Iterable<Path>)files::iterator)Files.deleteIfExists(p);}Files.deleteIfExists(dir);}
    @Test public void markdownUnicodeMultilineAndPermissionsArePreserved()throws Exception{
        Path dir=Files.createTempDirectory("editor-");Path file=dir.resolve("проект ' $(echo unsafe).md");Files.write(file,"# Привет\r\ntext\r\n".getBytes(StandardCharsets.UTF_8));Files.setPosixFilePermissions(file,PosixFilePermissions.fromString("rw-------"));
        try{TextFileEdits.Snapshot snapshot=read(file);assertEquals("# Привет\r\ntext\r\n",snapshot.text);String next="# Изменено\n$(touch NEVER_EXECUTE)\n";assertEquals("SAVED",new String(run(TextFileEdits.saveScript(snapshot),TextFileEdits.encode(next)),StandardCharsets.UTF_8));assertEquals(next,new String(Files.readAllBytes(file),StandardCharsets.UTF_8));assertEquals("rw-------",PosixFilePermissions.toString(Files.getPosixFilePermissions(file)));assertEquals(1,Files.list(dir).count());}finally{cleanup(dir);}
    }
    @Test public void externalEditsDeletionAndReplacementAreNotOverwritten()throws Exception{
        Path dir=Files.createTempDirectory("editor-conflict-");Path file=dir.resolve("README.md");
        try{Files.write(file,"initial".getBytes(StandardCharsets.UTF_8));TextFileEdits.Snapshot old=read(file);Files.write(file,"external".getBytes(StandardCharsets.UTF_8));assertEquals("CONFLICT",new String(run(TextFileEdits.saveScript(old),TextFileEdits.encode("my text")),StandardCharsets.UTF_8));assertEquals("external",new String(Files.readAllBytes(file),StandardCharsets.UTF_8));
            old=read(file);Files.delete(file);assertEquals("CONFLICT",new String(run(TextFileEdits.saveScript(old),TextFileEdits.encode("new")),StandardCharsets.UTF_8));assertFalse(Files.exists(file));
            Files.write(file,"original".getBytes(StandardCharsets.UTF_8));old=read(file);Files.delete(file);Files.createSymbolicLink(file,Paths.get("target.md"));Files.write(dir.resolve("target.md"),"secret".getBytes(StandardCharsets.UTF_8));assertEquals("CONFLICT",new String(run(TextFileEdits.saveScript(old),TextFileEdits.encode("replace")),StandardCharsets.UTF_8));assertEquals("secret",new String(Files.readAllBytes(dir.resolve("target.md")),StandardCharsets.UTF_8));assertTrue(Files.isSymbolicLink(file));
        }finally{cleanup(dir);}
    }
    @Test public void emptyFileIsValidButFailedReadBinaryAndInvalidUtf8AreNot()throws Exception{
        Path dir=Files.createTempDirectory("editor-text-");Path file=dir.resolve("note.md");
        try{Files.write(file,new byte[0]);assertEquals("",read(file).text);
            for(byte[] data:new byte[][]{new byte[]{0,1},new byte[]{(byte)0xc3,0x28}}){Files.write(file,data);try{read(file);fail("binary/invalid text accepted");}catch(IOException expected){}}
            try{TextFileEdits.parse(file.toString(),new byte[0]);fail("failed read became empty editor");}catch(IOException expected){}
            try{TextFileEdits.encode("\0");fail();}catch(IOException expected){}
            try{TextFileEdits.encode(new String(new char[TextFileEdits.MAX_BYTES+1]).replace('\0','a'));fail();}catch(IOException expected){}
        }finally{cleanup(dir);}
    }
    @Test public void scriptsHaveValidSyntaxAndSymlinksHardlinksRejected()throws Exception{
        Path dir=Files.createTempDirectory("editor-links-");Path file=dir.resolve("real.md");Files.write(file,"text".getBytes(StandardCharsets.UTF_8));
        try{Path link=dir.resolve("sym.md");Files.createSymbolicLink(link,file);Process p=new ProcessBuilder("bash","-c",TextFileEdits.readScript(link.toString())).start();assertNotEquals(0,p.waitFor());Files.delete(link);Files.createLink(link,file);p=new ProcessBuilder("bash","-c",TextFileEdits.readScript(file.toString())).start();assertNotEquals(0,p.waitFor());Files.delete(link);TextFileEdits.Snapshot snapshot=read(file);for(String script:new String[]{TextFileEdits.readScript(file.toString()),TextFileEdits.saveScript(snapshot)}){p=new ProcessBuilder("bash","-n","-c",script).start();assertEquals(0,p.waitFor());}}finally{cleanup(dir);}
    }
}
