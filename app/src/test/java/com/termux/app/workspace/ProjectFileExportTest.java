package com.termux.app.workspace;
import java.io.*;import java.nio.file.*;import java.util.*;import java.util.concurrent.*;import org.junit.*;import org.junit.rules.TemporaryFolder;import static org.junit.Assert.*;
public class ProjectFileExportTest {
 @Rule public TemporaryFolder temp=new TemporaryFolder();
 File save(String name)throws Exception{return ProjectFileExport.save(new ByteArrayInputStream(new byte[]{1,2}),temp.getRoot(),name,16,()->false);}
 @Test public void existingFileDirectoryAndDanglingLinkAreNeverReplaced()throws Exception{
  File original=new File(temp.getRoot(),"app.apk");Files.write(original.toPath(),new byte[]{9});File folder=new File(temp.getRoot(),"app-2.apk");assertTrue(folder.mkdir());
  Path dangling=new File(temp.getRoot(),"app-3.apk").toPath();Files.createSymbolicLink(dangling,Paths.get("missing"));
  assertEquals("app-4.apk",save("app.apk").getName());assertArrayEquals(new byte[]{9},Files.readAllBytes(original.toPath()));assertTrue(folder.isDirectory());assertTrue(Files.isSymbolicLink(dangling));
 }
 @Test public void destinationSymlinkDoesNotWriteIntoItsTarget()throws Exception{
  File target=temp.newFile("target");Files.write(target.toPath(),new byte[]{9});Path link=new File(temp.getRoot(),"alias").toPath();Files.createSymbolicLink(link,target.toPath());
  assertEquals("alias-2",save("alias").getName());assertTrue(Files.isSymbolicLink(link));assertArrayEquals(new byte[]{9},Files.readAllBytes(target.toPath()));
 }
 @Test public void readErrorAndByteLimitRemoveOnlyNewPartialOutput()throws Exception{
  File preserved=temp.newFile("keep");Files.write(preserved.toPath(),new byte[]{9});
  InputStream broken=new InputStream(){int calls;public int read()throws IOException{if(calls++<2)return 1;throw new IOException("synthetic");}};
  try{ProjectFileExport.save(broken,temp.getRoot(),"partial",16,()->false);fail();}catch(IOException expected){}assertFalse(new File(temp.getRoot(),"partial").exists());
  try{ProjectFileExport.save(new ByteArrayInputStream(new byte[]{1,2,3}),temp.getRoot(),"large",2,()->false);fail();}catch(IOException expected){}assertFalse(new File(temp.getRoot(),"large").exists());assertArrayEquals(new byte[]{9},Files.readAllBytes(preserved.toPath()));
 }
 @Test public void cancelDuringReadRemovesPartialAndDoesNotOverwriteEarlierFile()throws Exception{
  File preserved=temp.newFile("file");Files.write(preserved.toPath(),new byte[]{9});final boolean[] stop={false};
  InputStream input=new ByteArrayInputStream(new byte[]{1,2}){public synchronized int read(byte[] b,int o,int n){int read=super.read(b,o,n);stop[0]=true;return read;}};
  try{ProjectFileExport.save(input,temp.getRoot(),"file",16,()->stop[0]);fail();}catch(InterruptedIOException expected){}
  assertFalse(new File(temp.getRoot(),"file-2").exists());assertArrayEquals(new byte[]{9},Files.readAllBytes(preserved.toPath()));
 }
 @Test public void invalidNamesAndPreCancelledOperationCreateNoFiles()throws Exception{
  for(String name:new String[]{"", ".", "..", "../bad", "/bad", "a\u0000b", "a\nb"})try{save(name);fail(name);}catch(IOException expected){}
  try{ProjectFileExport.save(new ByteArrayInputStream(new byte[]{1}),temp.getRoot(),"not-created",16,()->true);fail();}catch(InterruptedIOException expected){}
  assertEquals(0,temp.getRoot().list().length);
 }
 @Test public void concurrentExportsGetDistinctNamesAndCompleteBytes()throws Exception{
  ExecutorService executor=Executors.newFixedThreadPool(2);try{List<Future<File>> outputs=executor.invokeAll(Arrays.asList(()->save("same.txt"),()->save("same.txt")));
   File a=outputs.get(0).get(),b=outputs.get(1).get();assertNotEquals(a,b);assertArrayEquals(new byte[]{1,2},Files.readAllBytes(a.toPath()));assertArrayEquals(new byte[]{1,2},Files.readAllBytes(b.toPath()));
  }finally{executor.shutdownNow();assertTrue(executor.awaitTermination(3,TimeUnit.SECONDS));}
 }
 @Test public void interruptedWorkerCreatesNothingAndKeepsInterruptFlag()throws Exception{
  try{Thread.currentThread().interrupt();try{save("not-created");fail();}catch(InterruptedIOException expected){}assertTrue(Thread.currentThread().isInterrupted());assertEquals(0,temp.getRoot().list().length);}
  finally{Thread.interrupted();}
 }

}
