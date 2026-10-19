package com.termux.app.workspace;

import android.app.Application;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import com.termux.R;
import com.termux.app.TermuxActivity;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.lang.reflect.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class GeminiPermissionUiTest {
    private static final String ID="12345678-1234-4234-8234-123456789abc";
    private JSONObject prompt()throws Exception{return new JSONObject().put("id",ID).put("type","exec").put("title","Shell").put("body","printf synthetic").put("options",new JSONArray().put("proceed_once").put("proceed_always").put("cancel"));}
    private void field(Object o,String key,Object value)throws Exception{Field f=o.getClass().getDeclaredField(key);f.setAccessible(true);f.set(o,value);}
    private static class Fixture {
        TermuxActivity activity;FrameLayout host;View terminal;WorkspaceStore.Session[] current={new WorkspaceStore.Session()};GeminiPermissionUi ui;
    }
    private Fixture fixture(GeminiPermissionUi.Transport transport)throws Exception{
        return fixture(transport,false);
    }
    private Fixture fixture(GeminiPermissionUi.Transport transport,boolean openCode)throws Exception{
        Fixture f=new Fixture();f.activity=Robolectric.buildActivity(TermuxActivity.class).get();f.activity.setTheme(R.style.Theme_Termux_Black);f.current[0].geminiAgent=!openCode;f.current[0].aiAgent=openCode?"opencode":"";
        f.host=new FrameLayout(f.activity);f.host.setId(R.id.workspace_terminal_surface);f.terminal=new View(f.activity);f.host.addView(f.terminal,new FrameLayout.LayoutParams(-1,-1));f.activity.setContentView(f.host);
        f.ui=new GeminiPermissionUi(f.activity,()->f.current[0],()->true,transport,openCode);field(f.ui,"run",openCode?f.current[0].id+":"+f.current[0].aiThread:f.current[0].geminiSocketId);field(f.ui,"id",ID);field(f.ui,"permission",prompt());field(f.ui,"body","printf synthetic");field(f.ui,"title","Shell");Method render=GeminiPermissionUi.class.getDeclaredMethod("render");render.setAccessible(true);render.invoke(f.ui);return f;
    }
    private String all(View v){String s=v instanceof TextView?((TextView)v).getText().toString():"";if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)s+="\n"+all(g.getChildAt(i));}return s;}
    private TextView find(View v,String text){if(v instanceof TextView&&((TextView)v).getText().toString().equals(text))return (TextView)v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){TextView t=find(g.getChildAt(i),text);if(t!=null)return t;}}return null;}
    private void idleUntil(java.util.function.BooleanSupplier done)throws Exception{long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);while(!done.getAsBoolean()&&System.nanoTime()<end){Thread.sleep(10);Shadows.shadowOf(Looper.getMainLooper()).idle();}assertTrue(done.getAsBoolean());}
    @Test public void nativeAcknowledgementHidesOverlayForNativeTranscriptReceiptWithoutResizingTerminal()throws Exception{
        for(String outcome:new String[]{"proceed_once","proceed_always","cancel"}){
            Fixture f=fixture((run,q)->new JSONObject().put("ok",true).put("resolved",new JSONObject().put("id",q.getString("id")).put("outcome",q.getString("outcome"))));
            try{
                String before=all(f.host);assertTrue(before.contains("Gemini"));assertTrue(before.contains("rules")||before.contains("правилам"));assertEquals(2,f.host.getChildCount());assertEquals(-1,f.terminal.getLayoutParams().height);
                String label=outcome.equals("cancel")?"No":outcome.equals("proceed_always")?"This session":"Allow once";
                if(WorkspaceLanguage.code(f.activity).equals("ru"))label=outcome.equals("cancel")?"Нет":outcome.equals("proceed_always")?"На эту сессию":"Разрешить разово";
                TextView button=find(f.host,label);assertNotNull(button);button.performClick();idleUntil(()->!all(f.host).contains("printf synthetic"));
                assertEquals(View.GONE,f.host.getChildAt(1).getVisibility());
                assertEquals(-1,f.terminal.getLayoutParams().height);
            }finally{f.ui.close();}
        }
    }
    @Test public void openCodeUsesItsOwnTitleAndAcknowledgementHidesOnlyOverlay()throws Exception{
        Fixture f=fixture((run,q)->new JSONObject().put("ok",true).put("resolved",new JSONObject().put("id",ID).put("outcome","proceed_once")),true);
        try{assertTrue(all(f.host).contains("OpenCode"));assertFalse(all(f.host).contains("Gemini"));find(f.host,WorkspaceLanguage.code(f.activity).equals("ru")?"Разрешить разово":"Allow once").performClick();idleUntil(()->f.host.getChildAt(1).getVisibility()==View.GONE);assertEquals(-1,f.terminal.getLayoutParams().height);}finally{f.ui.close();}
    }
    @Test public void repeatedTapAndLateAcknowledgementCannotAffectAnotherSession()throws Exception{
        AtomicInteger calls=new AtomicInteger();CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
        Fixture f=fixture((run,q)->{calls.incrementAndGet();entered.countDown();release.await(2,TimeUnit.SECONDS);return new JSONObject().put("ok",true).put("resolved",new JSONObject().put("id",ID).put("outcome","proceed_once"));});
        try{String label=WorkspaceLanguage.code(f.activity).equals("ru")?"Разрешить разово":"Allow once";TextView b=find(f.host,label);b.performClick();b.performClick();assertTrue(entered.await(2,TimeUnit.SECONDS));assertEquals(1,calls.get());f.current[0]=new WorkspaceStore.Session();f.ui.syncVisibility();release.countDown();Field worker=GeminiPermissionUi.class.getDeclaredField("worker");worker.setAccessible(true);((ExecutorService)worker.get(f.ui)).submit(()->{}).get(2,TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals(View.GONE,f.host.getChildAt(1).getVisibility());}finally{release.countDown();f.ui.close();}
    }
    @Test public void malformedOrPermanentActionsAreRejected(){
        try{assertTrue(GeminiPermissionUi.valid(prompt()));assertFalse(GeminiPermissionUi.valid(prompt().put("id","another")));assertFalse(GeminiPermissionUi.valid(prompt().put("options",new JSONArray().put("proceed_once").put("proceed_always_and_save").put("cancel"))));}catch(Exception e){throw new AssertionError(e);}
    }
    @Test public void wrongAcknowledgementNeverClaimsPermissionGranted()throws Exception{
        Fixture f=fixture((run,q)->new JSONObject().put("ok",true).put("resolved",new JSONObject().put("id","wrong").put("outcome","proceed_once")));
        try{find(f.host,WorkspaceLanguage.code(f.activity).equals("ru")?"Разрешить разово":"Allow once").performClick();idleUntil(()->all(f.host).contains("unconfirmed")||all(f.host).contains("не подтверждён"));assertFalse(all(f.host).contains("Allowed once"));assertFalse(all(f.host).contains("Разрешено один раз"));}finally{f.ui.close();}
    }
}
