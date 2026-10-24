package com.termux.app.workspace;

import android.app.Application;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import com.termux.R;
import com.termux.app.TermuxActivity;
import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Map;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=Application.class)
public class WorkspaceLanguageTest {
    @After public void reset() {
        RuntimeEnvironment.application.getSharedPreferences("workspace-language",Context.MODE_PRIVATE).edit().clear().commit();
        WorkspaceText.resources=null;WorkspaceText.englishResources=null;
    }
    @Test public void systemLanguageAndExplicitChoices() {
        assertEquals("ru",WorkspaceLanguage.resolve("system",new Locale("ru","RU")));
        assertEquals("en",WorkspaceLanguage.resolve("system",Locale.GERMAN));
        assertEquals("ru",WorkspaceLanguage.resolve("ru",Locale.ENGLISH));
        assertEquals("en",WorkspaceLanguage.resolve("en",new Locale("ru")));
    }
    @Test public void localizedActivityReceivesRotationSizeDensityAndThemeWithoutLosingChosenLanguage(){
        Context base=RuntimeEnvironment.application;
        for(String language:new String[]{"ru","en"}){
            RuntimeEnvironment.setQualifiers("en-w400dp-h800dp-port-notnight-mdpi");
            WorkspaceLanguage.select(base,language);TermuxActivity activity=Robolectric.buildActivity(TermuxActivity.class).get();
            assertEquals(android.content.res.Configuration.ORIENTATION_PORTRAIT,activity.getResources().getConfiguration().orientation);
            RuntimeEnvironment.setQualifiers("en-w800dp-h400dp-land-night-xhdpi");
            android.content.res.Configuration event=new android.content.res.Configuration(android.content.res.Resources.getSystem().getConfiguration());
            activity.onConfigurationChanged(event);
            android.content.res.Configuration live=activity.getResources().getConfiguration();
            assertEquals(android.content.res.Configuration.ORIENTATION_LANDSCAPE,live.orientation);
            assertEquals(800,live.screenWidthDp);assertEquals(400,live.screenHeightDp);
            assertEquals(320,live.densityDpi);assertEquals(android.content.res.Configuration.UI_MODE_NIGHT_YES,live.uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK);
            assertEquals(language,live.getLocales().get(0).getLanguage());
            assertEquals("en",event.getLocales().get(0).getLanguage());
            String next=language.equals("ru")?"en":"ru";WorkspaceLanguage.select(activity,next);
            RuntimeEnvironment.setQualifiers("en-w400dp-h800dp-port-notnight-mdpi");
            activity.onConfigurationChanged(new android.content.res.Configuration(android.content.res.Resources.getSystem().getConfiguration()));
            assertEquals(android.content.res.Configuration.ORIENTATION_PORTRAIT,activity.getResources().getConfiguration().orientation);
            assertEquals(next,activity.getResources().getConfiguration().getLocales().get(0).getLanguage());
            assertEquals(next.equals("ru")?"Настройки":"Settings",activity.getString(R.string.action_open_settings));
        }
    }
    @Test public void allResourcesRoundTripAndChoicePersists() throws Exception {
        Context c=RuntimeEnvironment.application;
        WorkspaceLanguage.select(c,"ru");WorkspaceLanguage.initialize(c);
        Field field=WorkspaceText.class.getDeclaredField("IDS");field.setAccessible(true);
        Map<String,Integer> ids=(Map<String,Integer>)field.get(null);
        assertTrue(ids.size()>=410);
        for(String source:ids.keySet()) assertEquals(source,WorkspaceText.tr(source));
        assertEquals("Русский",WorkspaceText.tr("Русский"));
        WorkspaceLanguage.select(c,"en");WorkspaceLanguage.initialize(c);
        assertEquals("en",WorkspaceLanguage.choice(c));
        assertEquals("Working directory",WorkspaceText.tr("Рабочий каталог"));
        assertEquals("Five-hour limit",WorkspaceText.tr("Пятичасовой лимит"));
        assertEquals("High",WorkspaceText.tr("Высокий"));
        for(String source:ids.keySet()) {
            String translated=WorkspaceText.tr(source);
            if(!source.equals("Русский")&&!source.equals("Язык / Language")) assertFalse(source,translated.matches("(?s).*[А-Яа-яЁё].*"));
        }
        assertEquals("custom /root/мой-проект",WorkspaceText.tr("custom /root/мой-проект"));
        assertEquals("Model changed to gpt-6 high",WorkspaceText.tr("Model changed to gpt-6 high"));
    }
    @Test public void localizedShellsKeepSyntaxAndPermissionFlags() throws Exception {
        Context c=RuntimeEnvironment.application;
        for(String language:new String[]{"ru","en"}) {
            WorkspaceLanguage.select(c,language);WorkspaceLanguage.initialize(c);
            java.io.File files=java.nio.file.Files.createTempDirectory("language-shell").toFile();
            WorkspaceStore.Session profile=new WorkspaceStore.Session();profile.id="local-language";profile.codexSocketId="local-language";profile.environment="codex";profile.codexEnvironment="termux";profile.directory="/tmp/a project";
            String codex=CodexSessionShell.command(files,profile);
            assertTrue(codex.contains("sandbox_mode=\"danger-full-access\""));assertTrue(codex.contains("approval_policy=\"never\""));
            java.io.File script=new java.io.File(files,"syntax.sh");java.nio.file.Files.write(script.toPath(),codex.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            Process syntax=new ProcessBuilder("/bin/bash","-n",script.getAbsolutePath()).redirectErrorStream(true).start();
            assertTrue(syntax.waitFor(5,java.util.concurrent.TimeUnit.SECONDS));assertEquals(0,syntax.exitValue());
            profile.sshHost="example.test";profile.sshUser="test";profile.startup="printf '%s\\n' 'user text'";
            String ssh=SshSessionShell.remoteScript(profile);
            java.nio.file.Files.write(script.toPath(),ssh.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            syntax=new ProcessBuilder("/bin/bash","-n",script.getAbsolutePath()).redirectErrorStream(true).start();
            assertTrue(syntax.waitFor(5,java.util.concurrent.TimeUnit.SECONDS));assertEquals(0,syntax.exitValue());
            assertTrue(ssh.contains("user text"));
        }
    }

    @Test public void switchingUiKeepsInputAndUserDataAndTileSize() throws Exception {
        TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();
        a.setTheme(R.style.Theme_Termux_Black);a.setContentView(R.layout.activity_termux);
        Field tv=TermuxActivity.class.getDeclaredField("mTerminalView");tv.setAccessible(true);tv.set(a,a.findViewById(R.id.terminal_view));
        WorkspaceLanguage.select(a,"ru");WorkspaceLanguage.apply(a);
        WorkspaceController controller=new WorkspaceController(a);
        Field input=WorkspaceController.class.getDeclaredField("terminalInput");input.setAccessible(true);EditText editor=(EditText)input.get(controller);
        editor.setText("cd /root/мой-проект\nprintf 'hello'\n");editor.setSelection(4);
        Field sf=WorkspaceController.class.getDeclaredField("store");sf.setAccessible(true);WorkspaceStore store=(WorkspaceStore)sf.get(controller);
        WorkspaceStore.Command command=new WorkspaceStore.Command();command.name="Моя команда";command.command="echo привет";command.category="Личное";store.commands.add(command);
        Field data=WorkspaceController.class.getDeclaredField("terminalDrafts");
        data.setAccessible(true);Object originalDrafts=data.get(controller);
        ViewGroup nav=a.findViewById(R.id.workspace_navigation);int height=nav.getChildAt(0).getLayoutParams().height;
        assertEquals(Math.round(56*a.getResources().getDisplayMetrics().density),height);
        for(int i=0;i<3;i++)assertEquals("",((TextView)nav.getChildAt(i)).getText().toString());
        WorkspaceLanguage.select(a,"en");WorkspaceLanguage.apply(a);controller.refreshLanguage();
        assertEquals("Enter a command…",editor.getHint().toString());assertEquals(4,editor.getSelectionStart());
        assertEquals("cd /root/мой-проект\nprintf 'hello'\n",editor.getText().toString());
        assertSame(originalDrafts,data.get(controller));assertEquals("Моя команда",command.name);assertEquals("echo привет",command.command);assertEquals("Личное",command.category);
        assertEquals("Home",nav.getChildAt(0).getContentDescription().toString());
        for(int i=0;i<3;i++){assertEquals(height,nav.getChildAt(i).getLayoutParams().height);assertEquals("",((TextView)nav.getChildAt(i)).getText().toString());assertFalse(nav.getChildAt(i).isLongClickable());}
        assertEquals("Settings",a.getString(R.string.action_open_settings));
        WorkspaceLanguage.select(a,"ru");WorkspaceLanguage.apply(a);controller.refreshLanguage();
        assertEquals("Введите команду…",editor.getHint().toString());assertEquals("Настройки",a.getString(R.string.action_open_settings));
        controller.close();
    }
}
