package com.termux.app.workspace;

import android.app.Application;
import android.app.AlertDialog;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import com.termux.R;
import com.termux.app.TermuxActivity;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

/** Real browser rendering with isolated file metadata; no environment/CLI launch. */
@org.robolectric.annotation.TextLayoutMode(org.robolectric.annotation.TextLayoutMode.Mode.REALISTIC)
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=Application.class,instrumentedPackages={"com.termux.app.workspace.ProjectFiles"},shadows=WorkspaceFileMetadataUiTest.Directory.class)
public class WorkspaceFileMetadataUiTest {
    static File fixture;static int calls;
    @Implements(value=ProjectFiles.class,isInAndroidSdk=false)
    public static class Directory {
        @Implementation protected ProjectFiles.Listing list(String path)throws Exception{
            calls++;ProjectFiles.Listing result=new ProjectFiles.Listing();result.path=fixture.getPath();
            for(File file:Objects.requireNonNull(fixture.listFiles()))result.entries.add(new ProjectFiles.Entry(result.path,file.getName(),file.isDirectory(),file.length(),file.lastModified()));
            result.entries.add(new ProjectFiles.Entry(result.path,"unknown.txt",false,0));return result;
        }
    }
    TermuxActivity activity;WorkspaceFileBrowser browser;TimeZone original;File file;String name="long-file-name-for-narrow-display-and-selected-overwrite.txt";
    static Object field(Object object,String name)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    @Before public void setUp()throws Exception{
        original=TimeZone.getDefault();TimeZone.setDefault(TimeZone.getTimeZone("Europe/Moscow"));
        activity=Robolectric.buildActivity(TermuxActivity.class).get();activity.setTheme(R.style.Theme_Termux_Black);WorkspaceLanguage.select(activity,"ru");WorkspaceLanguage.apply(activity);calls=0;
        fixture=Files.createTempDirectory(activity.getCacheDir().toPath(),"metadata-").toFile();file=new File(fixture,name);Files.write(file.toPath(),new byte[]{1});assertTrue(file.setLastModified(1793796128000L));
    }
    @After public void clean(){if(browser!=null)browser.close();activity.finish();TimeZone.setDefault(original);}
    void open(boolean picker)throws Exception{
        WorkspaceStore.Session session=new WorkspaceStore.Session();session.environment="termux";browser=new WorkspaceFileBrowser(activity,session,fixture.getPath(),null);
        if(picker)browser.setChatFileChoices((s,entries)->{},4);browser.show();idle();
    }
    void idle()throws Exception{for(int i=0;i<250;i++){Shadows.shadowOf(Looper.getMainLooper()).idle();if(!(boolean)field(browser,"busy"))return;Thread.sleep(10);}fail("Listing did not finish");}
    TextView row(String name)throws Exception{return ((AlertDialog)field(browser,"dialog")).getWindow().getDecorView().findViewWithTag("workspace-file-entry:"+new File(fixture,name).getPath());}
    void refresh()throws Exception{((View)field(browser,"refreshButton")).performClick();idle();}
    void metadataCycle(boolean picker)throws Exception{
        open(picker);assertTrue(row(name).getText().toString().contains("04.11.2026 15:42:08"));assertTrue(row("unknown.txt").getText().toString().contains("Дата неизвестна"));
        assertTrue(row(name).performLongClick());FileSort.save(activity,FileSort.Order.NAME_DESC);
        Files.write(file.toPath(),new byte[8192]);assertTrue(file.setLastModified(1793882529000L));Files.write(new File(fixture,"new.txt").toPath(),new byte[]{2});
        refresh();assertEquals(2,calls);assertNotNull(row("new.txt"));assertTrue(row(name).getText().toString().contains("05.11.2026 15:42:09"));assertTrue(row(name).getText().toString().contains(name));
        Map<?,?> selected=(Map<?,?>)field(browser,picker?"selectedFiles":"transferSelection");assertEquals(1,selected.size());ProjectFiles.Entry entry=(ProjectFiles.Entry)selected.values().iterator().next();assertEquals(8192,entry.size);assertEquals(file.lastModified(),entry.modified);assertEquals(FileSort.Order.NAME_DESC,FileSort.read(activity));
        TimeZone.setDefault(TimeZone.getTimeZone("GMT-08:00"));refresh();assertTrue(row(name).getText().toString().contains("05.11.2026 04:42:09"));
        TextView label=row(name);label.measure(View.MeasureSpec.makeMeasureSpec(180,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));assertNull(label.getEllipsize());assertEquals(Integer.MAX_VALUE,label.getMaxLines());assertTrue("Name, size and time remain separate lines",label.getLayout().getLineCount()>=3);assertTrue("All laid-out text fits the row height",label.getMeasuredHeight()>=label.getLayout().getHeight()+label.getCompoundPaddingTop()+label.getCompoundPaddingBottom());
    }
    @Test public void managerRefreshesOverwrittenMetadataAndKeepsSelection()throws Exception{metadataCycle(false);}
    @Test public void attachmentPickerRefreshesOverwrittenMetadataAndKeepsSelection()throws Exception{metadataCycle(true);}
    @Test public void unknownDateIsLocalizedInBothWindows()throws Exception{
        WorkspaceLanguage.select(activity,"en");WorkspaceLanguage.apply(activity);
        for(boolean picker:new boolean[]{false,true}){open(picker);assertTrue(row("unknown.txt").getText().toString().contains("Date unknown"));browser.close();}
        assertEquals("01.01.1970 03:00:00",WorkspaceFileBrowser.modifiedTime(0,"unknown"));assertEquals("unknown",WorkspaceFileBrowser.modifiedTime(-1,"unknown"));
    }
}
