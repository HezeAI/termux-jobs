package com.termux.app.workspace;
import android.app.*;import android.view.*;import android.widget.*;
import org.json.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;
import java.io.*;import java.util.*;import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class LocalAiLoraTest {
 LocalAiFiles files;JSONObject base;
 @Before public void setup()throws Exception{files=new LocalAiFiles(RuntimeEnvironment.application);base=new JSONObject().put("engine","llama").put("sha256",String.join("",Collections.nCopies(64,"a")));}
 // Header-only fixture tests managed import, not native tensor compatibility or training.
 byte[] fixture()throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();le(out,0x46554747L,4);le(out,3,4);le(out,1,8);le(out,2,8);for(String[] pair:new String[][]{{"general.type","adapter"},{"adapter.type","lora"}}){str(out,pair[0]);le(out,8,4);str(out,pair[1]);}return out.toByteArray();}
 void le(ByteArrayOutputStream out,long value,int count){for(int i=0;i<count;i++)out.write((int)(value >>> (8*i))&255);}
 void str(ByteArrayOutputStream out,String s)throws Exception{byte[] bytes=s.getBytes("UTF-8");le(out,bytes.length,8);out.write(bytes);}
 @Test public void importCopiesAndHashesAdapterWithoutTurningItIntoCatalogModel()throws Exception{
  int before=files.catalog().size();byte[] bytes=fixture();JSONObject m=LocalAiLora.importFile(files,"test.gguf",bytes.length,()->new ByteArrayInputStream(bytes),base,new AtomicBoolean(),(p,d,t)->{});assertEquals(before,files.catalog().size());File f=LocalAiLora.checked(files,m.getString("id"),base);assertArrayEquals(bytes,java.nio.file.Files.readAllBytes(f.toPath()));assertEquals("user-declared",m.getString("binding"));assertEquals(1,LocalAiLora.list(files,base).size());
  JSONObject other=new JSONObject(base.toString()).put("sha256",String.join("",Collections.nCopies(64,"b")));try{LocalAiLora.checked(files,m.getString("id"),other);fail();}catch(IOException expected){}try(RandomAccessFile edit=new RandomAccessFile(f,"rw")){edit.seek(f.length()-1);edit.writeByte(42);}try{LocalAiLora.checked(files,m.getString("id"),base);fail();}catch(IOException expected){}
 }
 @Test public void invalidCancelledAndOversizedImportsDoNotLeaveAdapterFiles()throws Exception{
  for(int mode=0;mode<4;mode++){AtomicBoolean cancel=new AtomicBoolean(mode==1);byte[] b=mode==0?new byte[]{1,2,3}:fixture();int selected=mode;try{LocalAiLora.importFile(files,mode==2?"bad.txt":"x.gguf",mode==3?LocalAiLora.MAX+1:b.length,()->new ByteArrayInputStream(b),base,cancel,(p,d,t)->{});fail("mode"+selected);}catch(Exception expected){}assertEquals(0,LocalAiLora.list(files,base).size());assertEquals(0,files.models.listFiles((d,n)->n.startsWith("lora-")).length);}
  for(String id:new String[]{"../escape","/sdcard/key","lora-../../secret"})try{LocalAiLora.id(id);fail();}catch(IllegalArgumentException expected){}
 }
 @Test public void sessionPresetCopyAndOptionalJsonRoundTripKeepDefaultOffAndRejectPaths()throws Exception{
  WorkspaceStore store=new WorkspaceStore(RuntimeEnvironment.application,s->{});try{store.sessions.clear();WorkspaceStore.Session p=new WorkspaceStore.Session();assertEquals("",p.localLora);p.localLora="lora-12345678-1234-1234-1234-123456789abc";store.sessions.add(p);store.decode(store.encode().toString());assertEquals(p.localLora,store.sessions.get(0).copy().localLora);LocalAiUi.Selection sel=new LocalAiUi.Selection(p);assertEquals(p.localLora,sel.profile().localLora);JSONObject json=store.encode();json.getJSONArray("sessions").getJSONObject(0).put("localLora","../escape");try{store.decode(json.toString());fail();}catch(Exception expected){}}finally{store.close();}
 }
 @Test public void loraMenuIsDisabledForLiteRtAndCpuHintIsNotDuplicated()throws Exception{
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();LocalAiUi ui=new LocalAiUi(a);try{WorkspaceStore.Session p=new WorkspaceStore.Session();p.localModel="qwen35-2b";LocalAiUi.Selection sel=new LocalAiUi.Selection(p);LinearLayout form=ui.settings(sel,()->{});assertTrue(form.findViewWithTag("local-lora").isEnabled());for(int i=0;i<form.getChildCount();i++)if(form.getChildAt(i) instanceof TextView)assertFalse(((TextView)form.getChildAt(i)).getText().toString().startsWith("Режим работы: CPU. GPU"));JSONObject lite=null;for(JSONObject m:files.catalog())if(m.optString("engine").equals("litert")){lite=m;break;}assertNotNull(lite);sel.model=lite.getString("id");sel.lora="";form=ui.settings(sel,()->{});assertFalse(form.findViewWithTag("local-lora").isEnabled());}finally{ui.close();}
 }
}
