package com.termux.app.workspace;
import android.app.*;
import android.graphics.PixelFormat;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class AgentCreationStepsTest {
 @Test public void repeatedSelectionUsesOneWindowAndOpaqueTopFormAndRetainsDraft(){
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();
  AlertDialog d=new AlertDialog.Builder(a).setView(new EditText(a)).setPositiveButton("Create",null).create();
  AgentLaunchForm.creationTitle(d,"New chat",true);d.show();AgentLaunchForm.creationWindow(d);
  AtomicInteger selected=new AtomicInteger();AgentCreationSteps steps=new AgentCreationSteps(d,null,agent->selected.incrementAndGet());
  Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
  assertSame(steps.form,steps.host.getChildAt(1));assertFalse(steps.choosing);assertFalse(steps.moving);
  EditText draft=find(steps.form);draft.setText("unchanged draft");float dim=d.getWindow().getAttributes().dimAmount;
  ViewGroup choices=steps.chooser.findViewWithTag("agent-chooser-list");
  for(int cycle=0;cycle<8;cycle++){
   steps.back();steps.back();
   Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
   assertTrue(steps.choosing);assertEquals(View.INVISIBLE,steps.form.getVisibility());
   choices.getChildAt(cycle%4).performClick();choices.getChildAt((cycle+1)%4).performClick();
   assertSame(steps.form,steps.host.getChildAt(steps.host.getChildCount()-1));
   assertEquals(View.VISIBLE,steps.form.getVisibility());assertNotNull(steps.form.getBackground());
   Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
   assertEquals(cycle+1,selected.get());assertEquals(2,steps.host.getChildCount());assertFalse(steps.choosing);
   assertEquals("unchanged draft",draft.getText().toString());assertEquals(dim,d.getWindow().getAttributes().dimAmount,0);
  }
  d.cancel();assertFalse(d.isShowing());a.finish();
 }
 private EditText find(View v){if(v instanceof EditText)return(EditText)v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){EditText e=find(((ViewGroup)v).getChildAt(i));if(e!=null)return e;}return null;}
}
