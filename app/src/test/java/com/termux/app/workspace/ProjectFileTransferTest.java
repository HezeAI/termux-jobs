package com.termux.app.workspace;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;
import java.io.*;import java.nio.file.*;import java.util.*;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(manifest=Config.NONE)
public class ProjectFileTransferTest {
 Path root,source,target;
 @Before public void setup()throws Exception{root=Files.createTempDirectory("file-transfer-");source=Files.createDirectory(root.resolve("source"));target=Files.createDirectory(root.resolve("target"));}
 @After public void cleanup()throws Exception{try(java.util.stream.Stream<Path> paths=Files.walk(root)){for(Path p:(Iterable<Path>)paths.sorted(Comparator.reverseOrder())::iterator)Files.deleteIfExists(p);}}
 ProjectFiles.Entry entry(Path p)throws Exception{return new ProjectFiles.Entry(p.getParent().toString(),p.getFileName().toString(),Files.isDirectory(p),Files.size(p));}
 int run(List<ProjectFiles.Entry> entries,Path destination,boolean move,boolean rename,boolean check)throws Exception{String script=ProjectFileTransfer.script(entries,destination.toString(),move,rename,check);Process p=new ProcessBuilder("bash","--noprofile","--norc","-c",script).redirectErrorStream(true).start();ByteArrayOutputStream log=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=p.getInputStream().read(b))!=-1)log.write(b,0,n);assertTrue(p.waitFor(15,java.util.concurrent.TimeUnit.SECONDS));return p.exitValue();}
 @Test public void quotedBatchCopiesFilesDirectoriesAndPreservesSymlinks()throws Exception{
  Path f=Files.write(source.resolve("quote ' $(touch bad).txt"),new byte[]{1,2});Path d=Files.createDirectory(source.resolve("папка"));Files.write(d.resolve("a"),new byte[]{3});Files.createSymbolicLink(d.resolve("link"),Paths.get("a"));
  List<ProjectFiles.Entry> entries=Arrays.asList(entry(f),entry(d));assertEquals(0,run(entries,target,false,false,true));assertFalse(Files.exists(target.resolve(f.getFileName())));assertEquals(0,run(entries,target,false,false,false));assertArrayEquals(new byte[]{1,2},Files.readAllBytes(target.resolve(f.getFileName())));assertTrue(Files.isSymbolicLink(target.resolve("папка/link")));assertTrue(Files.exists(f));assertFalse(Files.exists(root.resolve("bad")));
 }
 @Test public void conflictPreflightPreventsAnyPartialBatchAndRenameKeepsOriginal()throws Exception{
  Path a=Files.write(source.resolve("a"),new byte[]{1}),b=Files.write(source.resolve("b"),new byte[]{2});Files.write(target.resolve("b"),new byte[]{9});List<ProjectFiles.Entry> entries=Arrays.asList(entry(a),entry(b));assertEquals(23,run(entries,target,true,false,false));assertFalse(Files.exists(target.resolve("a")));assertTrue(Files.exists(a));assertEquals(0,run(entries,target,true,true,false));assertFalse(Files.exists(a));assertFalse(Files.exists(b));assertArrayEquals(new byte[]{9},Files.readAllBytes(target.resolve("b")));assertArrayEquals(new byte[]{2},Files.readAllBytes(target.resolve("b (2)")));
 }
 @Test public void recursiveOrOverlappingSelectionAndAliasDestinationAreRejected()throws Exception{
  Path d=Files.createDirectory(source.resolve("dir")),child=Files.createDirectory(d.resolve("child"));assertEquals(22,run(Arrays.asList(entry(d)),child,false,true,false));assertEquals(22,run(Arrays.asList(entry(d),entry(child)),target,true,true,false));Path alias=root.resolve("alias");Files.createSymbolicLink(alias,child);assertEquals(22,run(Arrays.asList(entry(d)),alias,true,true,false));assertTrue(Files.isDirectory(child));
 }
 @Test public void duplicateNamesAndCopyInPlaceCanReceiveExplicitNewNames()throws Exception{
  Path a=Files.write(source.resolve("same"),new byte[]{1}),b=Files.write(root.resolve("same"),new byte[]{2});assertEquals(23,run(Arrays.asList(entry(a),entry(b)),target,false,false,false));assertEquals(0,run(Arrays.asList(entry(a),entry(b)),target,false,true,false));assertArrayEquals(new byte[]{2},Files.readAllBytes(target.resolve("same (2)")));assertEquals(0,run(Arrays.asList(entry(a)),source,false,true,false));assertTrue(Files.exists(source.resolve("same (2)")));
 }
 @Test public void renamedApkRetainsExtension()throws Exception{Path a=Files.write(source.resolve("app.apk"),new byte[]{1});Files.write(target.resolve("app.apk"),new byte[]{2});assertEquals(0,run(Arrays.asList(entry(a)),target,false,true,false));assertTrue(Files.exists(target.resolve("app (2).apk")));}
 @Test public void missingSourceAndDanglingCollisionDoNotRemoveSource()throws Exception{
  Path a=Files.write(source.resolve("a"),new byte[]{1});ProjectFiles.Entry missing=new ProjectFiles.Entry(source.toString(),"missing",false,1);assertEquals(21,run(Arrays.asList(entry(a),missing),target,true,false,false));assertTrue(Files.exists(a));Files.createSymbolicLink(target.resolve("a"),Paths.get("missing"));assertEquals(23,run(Arrays.asList(entry(a)),target,true,false,false));assertTrue(Files.exists(a));
 }
 @Test public void racedDestinationDuringCopyNeverReplacesExistingDataAndCleansStage()throws Exception{
  Path a=Files.write(source.resolve("a"),new byte[]{1});String script="mv() { for destination;do :;done; printf keep > \"$destination\"; command mv \"$@\"; };\n"+ProjectFileTransfer.script(Arrays.asList(entry(a)),target.toString(),false,false,false);
  Process process=new ProcessBuilder("bash","--noprofile","--norc","-c",script).redirectErrorStream(true).start();try(InputStream in=process.getInputStream()){while(in.read()!=-1){}}assertTrue(process.waitFor(10,java.util.concurrent.TimeUnit.SECONDS));assertNotEquals(0,process.exitValue());assertArrayEquals("keep".getBytes(java.nio.charset.StandardCharsets.UTF_8),Files.readAllBytes(target.resolve("a")));assertTrue(Files.exists(a));
  try(java.util.stream.Stream<Path> paths=Files.list(target)){assertFalse(paths.anyMatch(p->p.getFileName().toString().startsWith(".termux-jobs-copy.")));}
 }
 @Test public void copyFailureRemovesOnlyItsTemporaryStage()throws Exception{
  Path a=Files.write(source.resolve("a"),new byte[]{1}),keep=Files.write(target.resolve("keep"),new byte[]{9});
  String script="cp() { for destination;do :;done; printf partial > \"$destination\"; return 1; };\n"+ProjectFileTransfer.script(Arrays.asList(entry(a)),target.toString(),false,false,false);
  Process process=new ProcessBuilder("bash","--noprofile","--norc","-c",script).redirectErrorStream(true).start();try(InputStream in=process.getInputStream()){while(in.read()!=-1){}}assertTrue(process.waitFor(10,java.util.concurrent.TimeUnit.SECONDS));assertEquals(24,process.exitValue());assertTrue(Files.exists(a));assertArrayEquals(new byte[]{9},Files.readAllBytes(keep));try(java.util.stream.Stream<Path> paths=Files.list(target)){assertEquals(1,paths.count());}
 }
 @Test public void serverDeletionGroupsKeepEachSourceParentAndRejectTraversal()throws Exception{
  org.json.JSONArray entries=new org.json.JSONArray().put(new org.json.JSONObject().put("path","/one/a")).put(new org.json.JSONObject().put("path","/two/b"));assertEquals(2,ServerConnection.deletionGroups(entries).size());for(String p:new String[]{"/a/../b","/a/./b","/a//b","relative","/"}){try{ServerConnection.deletionGroups(new org.json.JSONArray().put(new org.json.JSONObject().put("path",p)));fail(p);}catch(IOException expected){}}
 }
}
