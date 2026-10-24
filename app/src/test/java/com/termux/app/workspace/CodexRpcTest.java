package com.termux.app.workspace;
import android.app.Application;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28, application=Application.class)
public class CodexRpcTest {
    @Test public void queuedStopKeepsItsOriginalSocketAfterTheProfileRelaunches() throws Exception {
        File files=java.nio.file.Files.createTempDirectory("codex-owned-stop").toFile();
        WorkspaceStore.Session profile=new WorkspaceStore.Session();
        File old=CodexSessionShell.socket(files,profile);assertTrue(old.getParentFile().mkdirs());
        File stopped=new File(old.getAbsolutePath()+".stopped");assertTrue(stopped.createNewFile());
        Runnable pending=CodexRpc.ownedStop(files,profile);
        profile.codexSocketId=java.util.UUID.randomUUID().toString();File next=CodexSessionShell.socket(files,profile);
        try{pending.run();assertTrue(new File(old.getAbsolutePath()+".stop").isFile());assertFalse(new File(next.getAbsolutePath()+".stop").exists());}
        finally{new File(old.getAbsolutePath()+".stop").delete();stopped.delete();old.getParentFile().delete();files.delete();}
    }
    private static CodexRpc rpc(InputStream in, OutputStream out) throws Exception {
        Constructor<CodexRpc> c = CodexRpc.class.getDeclaredConstructor(boolean.class);c.setAccessible(true);CodexRpc r=c.newInstance(true);
        Field f=CodexRpc.class.getDeclaredField("in");f.setAccessible(true);f.set(r,in);f=CodexRpc.class.getDeclaredField("out");f.setAccessible(true);f.set(r,out);return r;
    }
    @Test public void websocketHandlesFragmentedJsonAndPingWithoutConfusingRpcResponses() throws Exception {
        byte[] bytes=new byte[]{1,5,'{','"','x','"',':',(byte)137,1,'!',(byte)128,2,'1','}'};
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        try(CodexRpc r=rpc(new ByteArrayInputStream(bytes),out)) {Method receive=CodexRpc.class.getDeclaredMethod("receive");receive.setAccessible(true);JSONObject data=(JSONObject)receive.invoke(r);assertEquals(1,data.getInt("x"));}
        assertEquals(138,out.toByteArray()[0]&255);assertEquals(129,out.toByteArray()[1]&255); // masked client pong
    }
    @Test public void oversizedServerFramesFailBeforeAllocatingPayload() throws Exception {
        byte[] bytes=new byte[]{(byte)129,127,0,0,0,0,0,64,0,0};
        try(CodexRpc r=rpc(new ByteArrayInputStream(bytes),new ByteArrayOutputStream())) {
            Method receive=CodexRpc.class.getDeclaredMethod("receive");receive.setAccessible(true);
            try {receive.invoke(r);fail("oversized frame accepted");} catch(InvocationTargetException expected){assertTrue(expected.getCause() instanceof IOException);}
        }
    }
    @Test public void localStatusRejectsStaleAndOversizedDataAndPreservesAccountLimits() throws Exception {
        File file=File.createTempFile("codex-status", ".json");
        JSONObject value=new JSONObject().put("schema",1).put("updated",System.currentTimeMillis()).put("thread","fixture-thread").put("model","fixture-model")
            .put("limits",new JSONObject().put("primary",new JSONObject().put("usedPercent",12)));
        try(FileOutputStream output=new FileOutputStream(file)) {output.write(value.toString().getBytes(StandardCharsets.UTF_8));}
        CodexStatus status=CodexStatus.fromFile(file);assertEquals("fixture-thread",status.thread);assertEquals("5ч: 88%",CodexStatus.limit(status.limits,"primary","5ч"));
        value.put("updated",1);try(FileOutputStream output=new FileOutputStream(file)) {output.write(value.toString().getBytes(StandardCharsets.UTF_8));}
        try {CodexStatus.fromFile(file);fail("stale status accepted");}catch(IOException expected) {}
        try(FileOutputStream output=new FileOutputStream(file)) {output.write(new byte[65537]);}
        try {CodexStatus.fromFile(file);fail("oversized status accepted");}catch(IOException expected) {}
    }
    @Test public void absentAndExhaustedLimitsAreDistinguished() throws Exception {
        assertEquals("5ч: нет данных",CodexStatus.limit(new JSONObject(),"primary","5ч"));
        JSONObject limits=new JSONObject().put("primary",new JSONObject().put("usedPercent",100));
        assertEquals("5ч: 0%",CodexStatus.limit(limits,"primary","5ч"));
    }
}
