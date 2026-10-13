package com.termux.app.workspace;

import android.app.AlertDialog;
import android.app.Application;
import android.os.Looper;
import static org.robolectric.Shadows.shadowOf;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import com.termux.R;
import com.termux.app.TermuxActivity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import java.lang.reflect.Method;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class WorkspacePackagesTest {
    private RadioButton radio(View root,String label){if(root instanceof RadioButton&&((RadioButton)root).getText().toString().equals(label))return (RadioButton)root;if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++){RadioButton found=radio(group.getChildAt(i),label);if(found!=null)return found;}}return null;}
    @Test public void compactButtonsStaySingleLineAcrossWidthsAndFontScales(){
        android.content.Context context=org.robolectric.RuntimeEnvironment.application;
        for(float scale:new float[]{1f,1.3f}){
            context.getResources().getDisplayMetrics().scaledDensity=context.getResources().getDisplayMetrics().density*scale;
            for(int width:new int[]{80,110,140})for(String label:new String[]{"Установленные","Доступные","Обновления","Перечитать список","Installed","Refresh catalogue"}){
                android.widget.TextView view=WorkspacePackages.compactAction(context,label,()->{});
                int w=WorkspaceDialogs.dp(context,width),h=WorkspaceDialogs.dp(context,56);view.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));view.layout(0,0,w,h);
                assertEquals(h,view.getMeasuredHeight());assertEquals(1,view.getLineCount());assertEquals(1,view.getMaxLines());
            }
        }
    }
    @Test public void versionConfirmationShowsExactCommandAndDowngradeWarning()throws Exception{
        TermuxActivity activity=Robolectric.buildActivity(TermuxActivity.class).get();activity.setTheme(R.style.Theme_Termux_Black);
        WorkspaceStore.Session profile=new WorkspaceStore.Session();profile.environment="termux";int[] calls={0};String[] command={null};
        WorkspacePackages browser=new WorkspacePackages(activity,profile,(p,current)->{calls[0]++;command[0]=p.startup;});
        Method confirm=WorkspacePackages.class.getDeclaredMethod("confirm",String.class,PackageCatalog.Entry.class);confirm.setAccessible(true);confirm.invoke(browser,"version",new PackageCatalog.Entry("bash","5.1-1",true));
        AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();assertTrue(hasText(dialog.getWindow().getDecorView(),"apt-get install -- 'bash=5.1-1'"));assertTrue(hasText(dialog.getWindow().getDecorView(),"Выбрана более старая версия."));assertEquals(0,calls[0]);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();shadowOf(Looper.getMainLooper()).idle();assertEquals(1,calls[0]);assertEquals("apt-get install -- 'bash=5.1-1'",command[0]);browser.close();
    }
    private boolean hasText(View view,String value){if(view instanceof android.widget.TextView&&((android.widget.TextView)view).getText().toString().contains(value))return true;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)if(hasText(group.getChildAt(i),value))return true;}return false;}
    @Test public void fullDescriptionHasBoundedInternalScrollAndOriginalText()throws Exception{
        TermuxActivity activity=Robolectric.buildActivity(TermuxActivity.class).get();activity.setTheme(R.style.Theme_Termux_Black);WorkspaceStore.Session profile=new WorkspaceStore.Session();WorkspacePackages browser=new WorkspacePackages(activity,profile,(p,c)->fail("description must not run a command"));
        Method show=WorkspacePackages.class.getDeclaredMethod("fullDescription",PackageCatalog.Entry.class,String.class);show.setAccessible(true);show.invoke(browser,new PackageCatalog.Entry("bash","5.2"),"Original description\nSecond paragraph");AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();assertTrue(hasText(dialog.getWindow().getDecorView(),"Original description\nSecond paragraph"));assertTrue(hasText(dialog.getWindow().getDecorView(),"Язык описания зависит от репозитория."));dialog.dismiss();browser.close();
    }
    @Test public void cancelNeverRunsAndBothConfirmedDestinationsPreserveEnvironment()throws Exception{
        TermuxActivity activity=Robolectric.buildActivity(TermuxActivity.class).get();activity.setTheme(R.style.Theme_Termux_Black);
        WorkspaceStore.Session source=new WorkspaceStore.Session();source.environment="codex";source.codexEnvironment="ubuntu";source.name="Selected Codex";
        Method confirm=WorkspacePackages.class.getDeclaredMethod("confirm",String.class,PackageCatalog.Entry.class);confirm.setAccessible(true);
        int[] calls={0};boolean[] selected={false};WorkspaceStore.Session[] operation={null};
        WorkspacePackages first=new WorkspacePackages(activity,source,(p,current)->{calls[0]++;operation[0]=p;selected[0]=current;});
        confirm.invoke(first,"install",new PackageCatalog.Entry("htop","3.4"));AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();assertTrue(radio(d.getWindow().getDecorView(),"В выбранной сессии").isChecked());d.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();shadowOf(Looper.getMainLooper()).idle();assertEquals(0,calls[0]);first.close();
        for(boolean same:new boolean[]{true,false}){
            WorkspacePackages browser=new WorkspacePackages(activity,source,(p,current)->{calls[0]++;operation[0]=p;selected[0]=current;});
            confirm.invoke(browser,"install",new PackageCatalog.Entry("htop","3.4"));d=ShadowAlertDialog.getLatestAlertDialog();assertEquals(same?0:1,calls[0]);
            if(!same)radio(d.getWindow().getDecorView(),"В новой сессии того же окружения").performClick();
            d.getButton(AlertDialog.BUTTON_POSITIVE).performClick();shadowOf(Looper.getMainLooper()).idle();assertEquals(same,selected[0]);assertEquals("ubuntu",operation[0].environment);assertEquals("apt-get install -- 'htop'",operation[0].startup);assertEquals("codex",source.environment);browser.close();
        }
        assertEquals(2,calls[0]);
    }
}
