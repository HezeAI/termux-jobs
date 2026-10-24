package com.termux.x11;

import android.app.Application;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class DesktopCatalogCacheTest {
 @Rule public TemporaryFolder temp=new TemporaryFolder(new File(System.getProperty("java.io.tmpdir")));
 private DesktopCatalog data(String pkg,boolean installed){DesktopCatalog.Parser p=new DesktopCatalog.Parser();p.feed("CATALOG:{\"package\":\""+pkg+"\",\"name\":\"Программа\",\"summary\":\"Описание\",\"installed\":"+installed+",\"available\":true}\nREADY:catalog\nDESKTOP_CHILD_EXIT=0\n");return p.result();}
 @Test public void snapshotsSurviveNewCacheInstancesAndAreSeparatedByEnvironmentAndList()throws Exception{
  File dir=temp.newFolder();DesktopCatalogCache cache=new DesktopCatalogCache(dir);cache.save("ubuntu",false,data("mousepad",false));cache.save("jobs-x11-ubuntu",true,data("ristretto",true));
  cache=new DesktopCatalogCache(dir);assertEquals("mousepad",cache.load("ubuntu",false).entries.get(0).pkg);assertEquals("ristretto",cache.load("jobs-x11-ubuntu",true).entries.get(0).pkg);assertNull(cache.load("ubuntu",true));assertNull(cache.load("jobs-x11-ubuntu",false));assertNull(cache.load("../ubuntu",false));
 }
 @Test public void refreshingReplacesOneFileWithoutAccumulatingVersionsAndInvalidationCannotReuseOldStatus()throws Exception{
  File dir=temp.newFolder();DesktopCatalogCache cache=new DesktopCatalogCache(dir);for(int i=0;i<5;i++)cache.save("ubuntu",false,data("app-"+i,false));assertEquals(1,dir.list().length);assertEquals("app-4",cache.load("ubuntu",false).entries.get(0).pkg);
  cache.invalidate("ubuntu",false);assertEquals(1,dir.list().length);assertNull(new DesktopCatalogCache(dir).load("ubuntu",false));cache.save("ubuntu",false,data("mousepad",false));assertEquals(1,dir.list().length);assertNotNull(cache.load("ubuntu",false));
 }
 @Test public void corruptIncompleteCacheAndSymlinksAreIgnoredAndNeverWrittenThrough()throws Exception{
  File dir=temp.newFolder(),file=new File(dir,"ubuntu-catalog.jsonl");Files.write(file.toPath(),"JOBS_X11_CATALOG_V1\nCATALOG:broken\n".getBytes(StandardCharsets.UTF_8));DesktopCatalogCache cache=new DesktopCatalogCache(dir);assertNull(cache.load("ubuntu",false));
  File outside=temp.newFile();Files.write(outside.toPath(),"untouched".getBytes(StandardCharsets.UTF_8));assertTrue(file.delete());Files.createSymbolicLink(file.toPath(),outside.toPath());assertNull(cache.load("ubuntu",false));cache.save("ubuntu",false,data("mousepad",false));assertEquals("untouched",new String(Files.readAllBytes(outside.toPath()),StandardCharsets.UTF_8));
 }
 @Test public void newProgramRequirementsRejectLegacyRecommendedStatusWithoutNewCacheFiles()throws Exception{
  File dir=temp.newFolder(),file=new File(dir,"termux-recommended.jsonl");org.json.JSONObject old=new org.json.JSONObject();android.os.Bundle items=new android.os.Bundle();for(String id:DesktopPrograms.IDS){old.put(id,true);items.putBoolean(id,true);}Files.write(file.toPath(),old.toString().getBytes(StandardCharsets.UTF_8));DesktopCatalogCache cache=new DesktopCatalogCache(dir);assertNull(cache.loadItems("termux"));cache.saveItems("termux",items);assertTrue(cache.loadItems("termux").getBoolean("images"));assertEquals(1,dir.list().length);
 }
 @Test public void cacheFromBeforeGpuToolsForcesFreshStatusCheck()throws Exception{
  File dir=temp.newFolder(),file=new File(dir,"ubuntu-recommended.jsonl");org.json.JSONObject old=new org.json.JSONObject();old.put("_schema",2);for(String id:DesktopPrograms.IDS)if(!id.equals("gpu-tools"))old.put(id,true);Files.write(file.toPath(),old.toString().getBytes(StandardCharsets.UTF_8));assertNull(new DesktopCatalogCache(dir).loadItems("ubuntu"));
 }

 @Test public void fedoraCodecRequirementsRejectOldFlagsAndReplaceTheSameCacheFile()throws Exception{
  File dir=temp.newFolder(),file=new File(dir,"fedora-recommended.jsonl");org.json.JSONObject old=new org.json.JSONObject();old.put("_schema",2);android.os.Bundle items=new android.os.Bundle();
  for(String id:DesktopPrograms.IDS){old.put(id,true);items.putBoolean(id,!id.equals("media"));}
  Files.write(file.toPath(),old.toString().getBytes(StandardCharsets.UTF_8));DesktopCatalogCache cache=new DesktopCatalogCache(dir);assertNull(cache.loadItems("fedora"));
  cache.saveItems("fedora",items);assertFalse(cache.loadItems("fedora").getBoolean("media"));assertTrue(cache.loadItems("fedora").getBoolean("editor"));assertEquals(1,dir.list().length);
  org.json.JSONObject rewritten=new org.json.JSONObject(new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8));assertEquals(3,rewritten.getInt("_schema"));
  cache.saveItems("debian",items);File other=new File(dir,"debian-recommended.jsonl");assertEquals(2,new org.json.JSONObject(new String(Files.readAllBytes(other.toPath()),StandardCharsets.UTF_8)).getInt("_schema"));assertNotNull(cache.loadItems("debian"));
 }

}
