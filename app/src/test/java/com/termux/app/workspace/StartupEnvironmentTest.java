package com.termux.app.workspace;
import android.app.*;import android.view.*;import android.widget.*;import com.termux.R;import com.termux.BuildConfig;import com.termux.app.TermuxActivity;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import org.robolectric.shadows.ShadowAlertDialog;import java.lang.reflect.*;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class StartupEnvironmentTest {
    private WorkspaceController controller;private TermuxActivity activity;
    @Before public void setup()throws Exception{activity=Robolectric.buildActivity(TermuxActivity.class).get();activity.setTheme(R.style.Theme_Termux_Black);activity.setContentView(R.layout.activity_termux);WorkspaceLanguage.select(activity,"en");Field f=TermuxActivity.class.getDeclaredField("mTerminalView");f.setAccessible(true);f.set(activity,activity.findViewById(R.id.terminal_view));controller=new WorkspaceController(activity);}
    @After public void close(){controller.close();}
    private View described(View root,String value){if(value.contentEquals(root.getContentDescription()==null?"":root.getContentDescription()))return root;if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++){View found=described(((ViewGroup)root).getChildAt(i),value);if(found!=null)return found;}return null;}
    private EditText firstEditor(View root){if(root instanceof EditText)return (EditText)root;if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++){EditText found=firstEditor(((ViewGroup)root).getChildAt(i));if(found!=null)return found;}return null;}
    @Test public void newSessionAndPresetKeepOtherEnvironmentsEnabled()throws Exception{
        Method edit=WorkspaceController.class.getDeclaredMethod("editSession",WorkspaceStore.Session.class,boolean.class);edit.setAccessible(true);
        for(boolean preset:new boolean[]{false,true}){edit.invoke(controller,null,preset);AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();View environment=described(dialog.findViewById(android.R.id.content),"Startup environment");if(environment==null)environment=described(dialog.findViewById(android.R.id.content),"Среда запуска");assertNotNull(environment);environment.performClick();AlertDialog chooser=ShadowAlertDialog.getLatestAlertDialog();ListAdapter adapter=chooser.getListView().getAdapter();assertEquals(3,adapter.getCount());for(int i=0;i<3;i++)assertTrue(adapter.isEnabled(i));chooser.dismiss();dialog.dismiss();}
    }



    @Test public void selectedLinuxAliasIsPersistedAndEntryCommandIsVisible()throws Exception{
        Field setup=WorkspaceController.class.getDeclaredField("environmentSetup");setup.setAccessible(true);((EnvironmentSetup)setup.get(controller)).close();
        setup.set(controller,new EnvironmentSetup(activity,()->java.util.Arrays.asList(new WorkspaceEnvironments.Choice("termux","","Termux","/termux/home",true),new WorkspaceEnvironments.Choice("proot:alpine","alpine","Alpine","/root",true))));
        Method edit=WorkspaceController.class.getDeclaredMethod("editSession",WorkspaceStore.Session.class,boolean.class);edit.setAccessible(true);edit.invoke(controller,null,false);
        AlertDialog form=ShadowAlertDialog.getLatestAlertDialog();View root=form.getWindow().getDecorView();described(root,"Environment").performClick();AlertDialog choices=ShadowAlertDialog.getLatestAlertDialog();assertEquals(2,choices.getListView().getCount());choices.getListView().performItemClick(null,1,1);
        assertEquals("Environment: Alpine",((TextView)described(root,"Environment")).getText().toString());String login=((TextView)described(root,"Environment login command")).getText().toString();assertTrue(login.contains("proot-distro login 'alpine'"));assertTrue(login.contains("exact instance"));
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();firstEditor(root).setText("Alpine test");form.getButton(-1).performClick();org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();Field field=WorkspaceController.class.getDeclaredField("store");field.setAccessible(true);WorkspaceStore store=(WorkspaceStore)field.get(controller);assertEquals(1,store.sessions.size());assertEquals("alpine",store.sessions.get(0).prootAlias);assertEquals("termux",store.sessions.get(0).environment);assertEquals("/root",store.sessions.get(0).directory);store.awaitWrites();
    }
    @Test public void ubuntuEntryExplainsSeparateAiDefault()throws Exception{
        WorkspaceStore.Session profile=new WorkspaceStore.Session();profile.environment="ubuntu";profile.name="Ubuntu";profile.directory="/root";
        Method edit=WorkspaceController.class.getDeclaredMethod("editSession",WorkspaceStore.Session.class,boolean.class);edit.setAccessible(true);edit.invoke(controller,profile,false);AlertDialog form=ShadowAlertDialog.getLatestAlertDialog();
        String login=((TextView)described(form.getWindow().getDecorView(),"Environment login command")).getText().toString();assertTrue(login.contains("proot-distro login 'ubuntu'"));assertTrue(login.contains("Ordinary Ubuntu"));assertTrue(login.contains("Ubuntu AI"));form.dismiss();
    }
    @Test public void absentEnvironmentOffersInstallationWithoutStartingItOnSelection()throws Exception{
        EnvironmentSetup setup=new EnvironmentSetup(activity,()->java.util.Arrays.asList(new WorkspaceEnvironments.Choice("proot:alpine","alpine","Alpine","/root",false)));
        try{setup.choose(c->fail("must wait for explicit installation"));AlertDialog choices=ShadowAlertDialog.getLatestAlertDialog();choices.getListView().performItemClick(null,0,0);AlertDialog consent=ShadowAlertDialog.getLatestAlertDialog();assertTrue(consent.getButton(-1).getText().toString().contains("Install"));assertTrue(((TextView)consent.findViewById(android.R.id.message)).getText().toString().contains("Existing systems are never replaced"));consent.getButton(-2).performClick();}finally{setup.close();}
    }

    @Test public void archInstallConsentExplainsArmVariantAndChecksumBeforeAnyInstallation(){
        for(String language:new String[]{"en","ru"}){WorkspaceLanguage.select(activity,language);EnvironmentSetup setup=new EnvironmentSetup(activity,()->java.util.Arrays.asList(new WorkspaceEnvironments.Choice("proot:archlinux","archlinux","Arch Linux","/root",false)));
            try{setup.choose(c->fail("must wait for installation consent"));ShadowAlertDialog.getLatestAlertDialog().getListView().performItemClick(null,0,0);AlertDialog consent=ShadowAlertDialog.getLatestAlertDialog();String message=((TextView)consent.findViewById(android.R.id.message)).getText().toString();assertTrue(message.contains("Arch Linux ARM"));assertTrue(message.contains("SHA-256"));assertTrue(message.contains("x86-64"));consent.getButton(-2).performClick();}finally{setup.close();}}
    }
}
