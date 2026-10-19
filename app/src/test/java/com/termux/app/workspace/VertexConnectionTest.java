package com.termux.app.workspace;

import android.app.*;
import android.widget.EditText;
import java.nio.file.Files;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class VertexConnectionTest {
    private Activity a;
    private WorkspaceStore.Session vertex(){WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="ubuntu";p.geminiAgent=true;p.geminiAuth="vertex";return p;}
    @Before public void setup(){a=Robolectric.buildActivity(Activity.class).setup().get();a.getSharedPreferences(VertexConnection.PREFS,0).edit().clear().commit();GeminiCli.directory(a.getFilesDir()).mkdirs();}
    @After public void finish(){a.finish();Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();}
    @Test public void defaultsAreConnectionMetadataNotASavedSession()throws Exception{
        assertTrue(VertexConnection.remember(a,"my-cloud-project","us-central1"));
        WorkspaceStore.Session p=vertex();String id=p.id,thread=p.geminiThread;VertexConnection.applyDefaults(a,p);
        assertEquals("my-cloud-project",p.geminiCloudProject);assertEquals("us-central1",p.geminiCloudLocation);assertFalse(p.saved);assertEquals(id,p.id);assertEquals(thread,p.geminiThread);
        p.geminiCloudProject="other-project";VertexConnection.applyDefaults(a,p);assertEquals("other-project",p.geminiCloudProject);
        WorkspaceStore.Session api=vertex();api.geminiAuth="api";VertexConnection.applyDefaults(a,api);assertEquals("",api.geminiCloudProject);
        WorkspaceStore.Session codex=new WorkspaceStore.Session();codex.environment="codex";VertexConnection.applyDefaults(a,codex);assertEquals("",codex.geminiCloudProject);
        assertFalse(VertexConnection.remember(a,"/sdcard/Download","global"));assertFalse(VertexConnection.remember(a,"my-project","global\ncommand"));
    }
    @Test public void responseIsScopedAtomicAndDoesNotContainCredentials()throws Exception{
        WorkspaceStore.Session p=vertex(),other=vertex();Files.write(VertexConnection.file(a,p,"request").toPath(),"project\n".getBytes());
        VertexConnection.respond(a,p,"my-cloud-project","europe-west4");assertEquals("my-cloud-project\neurope-west4\n",new String(Files.readAllBytes(VertexConnection.file(a,p,"response").toPath())));
        assertFalse(VertexConnection.file(a,other,"response").exists());assertFalse(p.saved);
        try{VertexConnection.respond(a,other,"my-cloud-project","global");fail();}catch(java.io.IOException expected){}
        p.geminiAuth="api";assertFalse(VertexConnection.pending(a,p));
    }
    @Test public void dialogRejectsAuthorizationCodeAndStaleRun()throws Exception{
        WorkspaceStore.Session p=vertex();Files.write(VertexConnection.file(a,p,"request").toPath(),"project\n".getBytes());boolean[] active={true},done={false};
        AlertDialog dialog=VertexConnection.prompt(a,p,()->active[0],()->done[0]=true);Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        EditText field=dialog.getWindow().getDecorView().findViewWithTag("vertex-setup-project");field.setText("4/authorization-code");dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();assertNotNull(field.getError());assertTrue(dialog.isShowing());assertFalse(done[0]);
        field.setText("my-cloud-project");active[0]=false;dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();assertFalse(done[0]);assertFalse(VertexConnection.file(a,p,"response").exists());
    }
    @Test public void validDialogSavesConnectionAndContinuesOnlyOnce()throws Exception{
        WorkspaceStore.Session p=vertex();Files.write(VertexConnection.file(a,p,"request").toPath(),"project\n".getBytes());int[] done={0};
        AlertDialog d=VertexConnection.prompt(a,p,()->true,()->done[0]++);Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();((EditText)d.getWindow().getDecorView().findViewWithTag("vertex-setup-project")).setText("my-cloud-project");d.getButton(AlertDialog.BUTTON_POSITIVE).performClick();assertEquals(1,done[0]);assertFalse(d.isShowing());WorkspaceStore.Session fresh=vertex();VertexConnection.applyDefaults(a,fresh);assertEquals("my-cloud-project",fresh.geminiCloudProject);
        String generated=GeminiCli.command(a.getFilesDir(),p);assertFalse(generated.contains("export JOBS_GEMINI_VERTEX_PROJECT"));assertTrue(AgentPreparation.wrap(p,generated).contains("vertex-project.sh"));
    }
}
