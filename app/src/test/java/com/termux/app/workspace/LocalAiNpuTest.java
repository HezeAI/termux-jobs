package com.termux.app.workspace;
import org.junit.*;import org.json.*;import java.io.*;import static org.junit.Assert.*;
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk=28,application=android.app.Application.class)
public class LocalAiNpuTest {
    @Rule public org.junit.rules.TemporaryFolder tmp=new org.junit.rules.TemporaryFolder();
    @Test public void npuPairingUsesPinnedHashRatherThanFilename()throws Exception{JSONObject m=new JSONObject().put("engine","litert").put("name","SM8750 NPU").put("sha256","wrong");assertFalse(LocalAiNpu.verifiedModel(m));m.put("sha256",LocalAiNpu.SHA);assertTrue(LocalAiNpu.model(m));m.put("engine","llama");assertFalse(LocalAiNpu.verifiedModel(m));}
    @Test public void requiresCompleteDispatchAndQairtSet()throws Exception{File dir=tmp.newFolder();for(String name:LocalAiNpu.LIBRARIES){assertFalse(LocalAiNpu.available(dir));assertTrue(new File(dir,name).createNewFile());}assertTrue(LocalAiNpu.available(dir));assertTrue(new File(dir,"libQnnHtpV79Skel.so").delete());assertFalse(LocalAiNpu.available(dir));}
}
