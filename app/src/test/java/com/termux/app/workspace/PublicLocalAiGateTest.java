package com.termux.app.workspace;
import android.app.*;import android.view.*;import android.widget.*;import com.termux.R;import com.termux.BuildConfig;import com.termux.app.TermuxActivity;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import org.robolectric.shadows.ShadowAlertDialog;import java.lang.reflect.*;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class PublicLocalAiGateTest {
    private WorkspaceController controller;private TermuxActivity activity;
    @Before public void setup()throws Exception{activity=Robolectric.buildActivity(TermuxActivity.class).get();activity.setTheme(R.style.Theme_Termux_Black);activity.setContentView(R.layout.activity_termux);WorkspaceLanguage.select(activity,"en");Field f=TermuxActivity.class.getDeclaredField("mTerminalView");f.setAccessible(true);f.set(activity,activity.findViewById(R.id.terminal_view));controller=new WorkspaceController(activity);}
    @After public void close(){controller.close();}
    private View described(View root,String value){if(value.contentEquals(root.getContentDescription()==null?"":root.getContentDescription()))return root;if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++){View found=described(((ViewGroup)root).getChildAt(i),value);if(found!=null)return found;}return null;}
    @Test public void newSessionAndPresetKeepOtherEnvironmentsEnabled()throws Exception{
        Method edit=WorkspaceController.class.getDeclaredMethod("editSession",WorkspaceStore.Session.class,boolean.class);edit.setAccessible(true);
        for(boolean preset:new boolean[]{false,true}){edit.invoke(controller,null,preset);AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();View environment=described(dialog.findViewById(android.R.id.content),"Startup environment");if(environment==null)environment=described(dialog.findViewById(android.R.id.content),"Среда запуска");assertNotNull(environment);environment.performClick();AlertDialog chooser=ShadowAlertDialog.getLatestAlertDialog();ListAdapter adapter=chooser.getListView().getAdapter();assertEquals(5,adapter.getCount());for(int i=0;i<4;i++)assertTrue(adapter.isEnabled(i));assertEquals(BuildConfig.LOCAL_AI_ENABLED,adapter.isEnabled(4));chooser.dismiss();dialog.dismiss();}
    }
    @Test public void publicBuildAlsoBlocksDirectPresetEditing()throws Exception{
        if(BuildConfig.LOCAL_AI_ENABLED){assertTrue(BuildConfig.LOCAL_AI_ENABLED);return;}
        WorkspaceStore.Session p=new WorkspaceStore.Session();p.localAi=true;p.saved=true;p.localChat="kept";Method edit=WorkspaceController.class.getDeclaredMethod("editSession",WorkspaceStore.Session.class,boolean.class);edit.setAccessible(true);edit.invoke(controller,p,false);AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();assertNotNull(dialog.findViewById(android.R.id.message));assertEquals("kept",p.localChat);assertTrue(p.saved);
    }
    @Test public void publicBuildPreservesExistingLocalPresetWithoutStarting()throws Exception{
        if(BuildConfig.LOCAL_AI_ENABLED){assertTrue(BuildConfig.LOCAL_AI_ENABLED);return;}
        WorkspaceStore.Session profile=new WorkspaceStore.Session();profile.localAi=true;profile.saved=true;profile.localChat="retained";profile.name="existing";Method launch=WorkspaceController.class.getDeclaredMethod("launch",WorkspaceStore.Session.class);launch.setAccessible(true);launch.invoke(controller,profile);assertTrue(ShadowAlertDialog.getLatestAlertDialog().isShowing());assertEquals("retained",profile.localChat);assertTrue(profile.saved);assertNull(activity.getCurrentSession());
    }
}
