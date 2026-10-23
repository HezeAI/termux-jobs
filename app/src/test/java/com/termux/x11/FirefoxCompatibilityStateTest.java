package com.termux.x11;
import java.io.*;
import java.nio.file.Files;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(manifest=Config.NONE,sdk=28)
public class FirefoxCompatibilityStateTest {
 @Test public void fedoraJournalUsesExactOrgMozillaLauncherAndRejectsOtherPaths()throws Exception{
  File root=Files.createTempDirectory("fedora-state").toFile(),home=new File(root,"root");
  try{JSONObject data=new JSONObject();byte[] bytes="fixture".getBytes();
   for(String name:new String[]{FirefoxCompatibilityState.WRAPPER,FirefoxCompatibilityState.FEDORA_LAUNCHER}){File f=new File(home,name);f.getParentFile().mkdirs();Files.write(f.toPath(),bytes);data.put(name,X11Runtime.digest(bytes));}
   File journal=new File(home,".local/state/termux-jobs/firefox-compat.json");journal.getParentFile().mkdirs();Files.write(journal.toPath(),data.toString().getBytes());assertEquals(1,FirefoxCompatibilityState.read(root));
   data.put("../other",X11Runtime.digest(bytes));Files.write(journal.toPath(),data.toString().getBytes());assertEquals(-1,FirefoxCompatibilityState.read(root));
  }finally{remove(root);}
 }
 @Test public void journalRequiresBothActualFilesAndRejectsEditsAndLinks()throws Exception{
  File root=Files.createTempDirectory("firefox-state").toFile(),home=new File(root,"root"),wrapper=new File(home,FirefoxCompatibilityState.WRAPPER),launcher=new File(home,FirefoxCompatibilityState.LAUNCHER),journal=new File(home,".local/state/termux-jobs/firefox-compat.json");
  try{assertEquals(0,FirefoxCompatibilityState.read(root));wrapper.getParentFile().mkdirs();launcher.getParentFile().mkdirs();journal.getParentFile().mkdirs();byte[] bytes="managed fixture".getBytes();Files.write(wrapper.toPath(),bytes);Files.write(launcher.toPath(),bytes);
   JSONObject data=new JSONObject();data.put(FirefoxCompatibilityState.WRAPPER,X11Runtime.digest(bytes));data.put(FirefoxCompatibilityState.LAUNCHER,X11Runtime.digest(bytes));Files.write(journal.toPath(),data.toString().getBytes());assertEquals(1,FirefoxCompatibilityState.read(root));
   Files.write(launcher.toPath(),"user edit".getBytes());assertEquals(-1,FirefoxCompatibilityState.read(root));Files.delete(launcher.toPath());Files.createSymbolicLink(launcher.toPath(),wrapper.toPath());try{FirefoxCompatibilityState.read(root);fail();}catch(IOException expected){}
  }finally{remove(root);}
 }
 private void remove(File file)throws IOException{if(file.isDirectory()&&!Files.isSymbolicLink(file.toPath()))for(File child:file.listFiles())remove(child);Files.deleteIfExists(file.toPath());}
}
