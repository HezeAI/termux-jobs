package com.termux.app.workspace;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import static org.junit.Assert.*;
public class ChatAttachmentsTest {
 @Rule public TemporaryFolder folder=new TemporaryFolder();
 @Test public void largeFileAdmissionUsesLongBoundsAndKeepsImageLimit()throws Exception{
  for(String kind:new String[]{"apk","binary","video","file"}){
   for(long size:new long[]{256L*1024*1024+1,2L*1024*1024*1024+1,5L*1024*1024*1024})ChatAttachments.requireSize(kind,size);
   for(long size:new long[]{0,-1,5L*1024*1024*1024+1})try{ChatAttachments.requireSize(kind,size);fail(kind+":"+size);}catch(IOException expected){assertEquals("FILE_LIMIT",expected.getMessage());}
  }
  ChatAttachments.requireSize("image",100L*1024*1024);
  try{ChatAttachments.requireSize("image",100L*1024*1024+1);fail();}catch(IOException expected){assertEquals("FILE_LIMIT",expected.getMessage());}
 }
 @Test public void supportedTypesAndExplicitRefusal()throws Exception{
  assertEquals("image",ChatAttachments.type("shot.PNG"));assertEquals("file",ChatAttachments.type("device.jsonl"));assertEquals("file",ChatAttachments.type("report.LOG"));assertEquals("video",ChatAttachments.type("recording.MP4"));
  assertEquals("apk",ChatAttachments.type("package.apk"));assertEquals("binary",ChatAttachments.type("archive.zip"));
 }
 @Test public void attachmentPathsRejectTraversalAndSymlinks()throws Exception{
  File root=folder.newFolder("attachments");String id="aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";File dir=new File(root,id);assertTrue(dir.mkdir());File f=new File(dir,"original.png");assertTrue(f.createNewFile());assertEquals(f,ChatAttachments.inside(root,id+"/original.png"));assertEquals(ChatAttachments.GUEST+id+"/original.png",ChatAttachments.guest(root,f));
  for(String path:new String[]{"../secret",id+"/../secret",id+"/sub/file.png","/sdcard/photo.png"})try{ChatAttachments.inside(root,path);fail(path);}catch(IOException expected){}
  File outside=folder.newFile("outside");File link=new File(dir,"link.png");Files.createSymbolicLink(link.toPath(),outside.toPath());try{ChatAttachments.inside(root,id+"/link.png");fail();}catch(IOException expected){}
 }
 @Test public void trustedAndroidRootAliasIsCanonicalizedBeforeCheckingChildren()throws Exception{
  File root=folder.newFolder("actual"),alias=new File(folder.getRoot(),"android-root-alias");Files.createSymbolicLink(alias.toPath(),root.toPath());String id="aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";File dir=new File(root,id);assertTrue(dir.mkdir());File f=new File(dir,"original.png");assertTrue(f.createNewFile());assertEquals(f,ChatAttachments.inside(alias,id+"/original.png"));assertEquals(ChatAttachments.GUEST+id+"/original.png",ChatAttachments.guest(alias,f));
 }
 @Test public void removingDraftDoesNotFollowSymlinkToAnotherDirectory()throws Exception{
  File draft=folder.newFolder("draft"),outside=folder.newFolder("outside-tree"),keep=new File(outside,"keep.txt");assertTrue(keep.createNewFile());Files.createSymbolicLink(new File(draft,"linked").toPath(),outside.toPath());ChatAttachments.delete(draft);assertTrue(keep.isFile());assertFalse(draft.exists());
 }
 @Test public void logPreviewIsBoundedAndDoesNotModifyFullFile()throws Exception{
  File log=folder.newFile("report.log");byte[] content="log entry\n".repeat(4000).getBytes(StandardCharsets.UTF_8);Files.write(log.toPath(),content);assertEquals(8192,ChatAttachments.textPreview(log,8192).length());assertArrayEquals(content,Files.readAllBytes(log.toPath()));
  Files.write(log.toPath(),new byte[]{65,0,66});try{ChatAttachments.textPreview(log,8192);fail();}catch(IOException expected){assertEquals("INVALID_TEXT",expected.getMessage());}
 }
 @Test public void arbitraryFormatsStayOpaqueAndBadgesRetainCompoundArchives()throws Exception{
  for(String name:new String[]{"model.3mf","part.stl","tools.exe","script.bat","archive.7z","archive.rar","archive.gzip","archive.tar.gz","custom.xyz123","README"})assertEquals(name,"binary",ChatAttachments.type(name));
  assertEquals("3MF",ChatAttachments.badge("model.3mf"));assertEquals("STL",ChatAttachments.badge("part.stl"));assertEquals("TAR.GZ",ChatAttachments.badge("project.tar.gz"));assertEquals("FILE",ChatAttachments.badge("README"));assertEquals("FILE",ChatAttachments.badge(".env"));
 }

}
