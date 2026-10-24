package com.termux.app.workspace;

import android.app.*;
import android.graphics.Canvas;
import android.view.View;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class SessionActivityUiTest {
    @Test public void onlyDeclaredAiAgentSessionsGetTheRobot(){
        WorkspaceStore.Session p=new WorkspaceStore.Session();assertEquals(com.termux.R.drawable.workspace_terminal,WorkspaceController.sessionIcon(p));
        for(String env:new String[]{"termux","ubuntu","ssh"}){p.environment=env;assertEquals(com.termux.R.drawable.workspace_terminal,WorkspaceController.sessionIcon(p));}
        p.environment="codex";assertEquals(com.termux.R.drawable.workspace_ai_agent,WorkspaceController.sessionIcon(p));p.environment="ubuntu";
        p.aiAgent="opencode";assertEquals(com.termux.R.drawable.workspace_ai_agent,WorkspaceController.sessionIcon(p));p.aiAgent="";p.claudeAgent=true;assertEquals(com.termux.R.drawable.workspace_ai_agent,WorkspaceController.sessionIcon(p));p.claudeAgent=false;p.geminiAgent=true;assertEquals(com.termux.R.drawable.workspace_ai_agent,WorkspaceController.sessionIcon(p));
    }
    @Test public void foregroundProcParsingDoesNotReadCommandArguments(){
        assertEquals(456,SessionActivitySignal.foreground("123 (name with ) brackets) S 1 2 3 4 456 0 0"));
        assertEquals(-1,SessionActivitySignal.foreground("invalid"));assertEquals(-1,SessionActivitySignal.foreground("1 (sh) S"));
        assertFalse(SessionActivitySignal.command("bash\n"));assertFalse(SessionActivitySignal.command("ssh"));assertFalse(SessionActivitySignal.command("node"));assertFalse(SessionActivitySignal.busy(-1));assertTrue(SessionActivitySignal.command("sleep\n"));assertTrue(SessionActivitySignal.command("clang"));
    }
    @Test public void lineNeverChangesLayoutHeightWhenWorkChanges(){
        android.content.Context c=RuntimeEnvironment.application;WorkspaceActivityLine line=new WorkspaceActivityLine(c,null);line.setLayoutParams(new android.widget.FrameLayout.LayoutParams(300,1));
        line.working(true);assertTrue(line.working());assertEquals(View.VISIBLE,line.getVisibility());assertEquals(1,line.getLayoutParams().height);
        line.working(false);assertFalse(line.working());assertEquals(View.INVISIBLE,line.getVisibility());assertEquals(1,line.getLayoutParams().height);
    }
    @Test public void hidingAndRestoringTransferPreservesTheWindowAndProgress(){
        Activity a=Robolectric.buildActivity(Activity.class).setup().get();AlertDialog dialog=new AlertDialog.Builder(a).setMessage("progress").setNegativeButton("Cancel",null).create();dialog.show();
        ServerTransferPresentation state=new ServerTransferPresentation();int[] changes={0};state.listener(()->changes[0]++);state.begin("owner",dialog,"initial");state.background();assertTrue(state.active());assertTrue(state.belongs("owner"));assertFalse(state.belongs("other"));state.update("50%");state.foreground();assertTrue(dialog.isShowing());assertEquals("50%",state.text());assertTrue(changes[0]>=4);assertFalse(state.finish());assertFalse(state.active());assertFalse(state.belongs("owner"));dialog.dismiss();
    }
    @Test public void backgroundCompletionStopsOnlyPresentation(){
        Activity a=Robolectric.buildActivity(Activity.class).setup().get();AlertDialog d=new AlertDialog.Builder(a).create();d.show();ServerTransferPresentation s=new ServerTransferPresentation();s.begin("one",d,"working");s.background();assertTrue(s.finish());assertFalse(s.active());s.foreground();assertFalse(s.active());d.dismiss();
    }
    @Test public void paintingTransferBorderKeepsGeometry(){
        WorkspaceTransferBorder border=new WorkspaceTransferBorder(1);border.setBounds(0,0,106,48);border.active(true);border.draw(new Canvas());assertTrue(border.active());border.active(false);assertFalse(border.active());assertEquals(106,border.getBounds().width());
    }
    @Test public void repeatedBorderDrawingCannotAccumulateFrameCallbacks()throws Exception{
        WorkspaceTransferBorder border=new WorkspaceTransferBorder(1);
        final int[] schedules={0},cancels={0};final Runnable[] pending={null};
        // Drawable keeps a weak callback, while a real View owner stays strongly
        // reachable. Keep the fixture's owner alive across GC in the full suite.
        android.graphics.drawable.Drawable.Callback owner=new android.graphics.drawable.Drawable.Callback(){
            public void invalidateDrawable(android.graphics.drawable.Drawable d){}
            public void scheduleDrawable(android.graphics.drawable.Drawable d,Runnable r,long when){schedules[0]++;pending[0]=r;}
            public void unscheduleDrawable(android.graphics.drawable.Drawable d,Runnable r){cancels[0]++;pending[0]=null;}
        };
        border.setCallback(owner);
        border.setBounds(0,0,106,48);
        // Robolectric's legacy PathMeasure returns zero; isolate frame scheduling from graphics.
        java.lang.reflect.Field measure=WorkspaceTransferBorder.class.getDeclaredField("measure");measure.setAccessible(true);
        measure.set(border,new android.graphics.PathMeasure(){@Override public float getLength(){return 200;}});
        border.active(true);
        for(int i=0;i<20;i++)border.draw(new Canvas());assertEquals(1,schedules[0]);
        pending[0].run();border.draw(new Canvas());assertEquals(2,schedules[0]);
        border.active(false);assertTrue(cancels[0]>0);assertNull(pending[0]);
        border.draw(new Canvas());assertEquals(2,schedules[0]);
        assertSame(owner,border.getCallback());
    }
}
