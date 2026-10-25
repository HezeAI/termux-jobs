package com.termux.app.workspace;

import android.app.*;
import android.widget.*;
import com.termux.x11.X11ScreenSettings;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class WorkspaceDesktopScreenTest {
 private Activity a;private AlertDialog d;private int saved,cancelled;
 @Before public void setup(){a=Robolectric.buildActivity(Activity.class).setup().get();a.setTheme(com.termux.R.style.Theme_Termux_Black);a.getSharedPreferences("workspace-x11",0).edit().clear().commit();WorkspaceDesktopScreen.show(a,"ubuntu",()->saved++,()->cancelled++);d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();}
 @After public void cleanup(){d.dismiss();a.finish();}
 private <T extends android.view.View>T view(String tag){return d.getWindow().getDecorView().findViewWithTag("x11-screen-"+tag);}
 private void text(String tag,String value){this.<EditText>view(tag).setText(value);}
 @Test public void applyValidModeAndMarginsThenReopen(){
  assertFalse(view("width").isEnabled());this.<CheckBox>view("auto").setChecked(false);assertTrue(view("width").isEnabled());text("width","1300");text("height","600");text("horizontal","24");text("vertical","12");text("scale","75");d.getButton(-1).performClick();assertEquals(1,saved);assertFalse(d.isShowing());
  X11ScreenSettings s=X11ScreenSettings.load(a,"ubuntu");assertEquals(1300,s.width);assertEquals(24,s.horizontalDp);assertEquals(75,s.scalePercent);assertTrue(X11ScreenSettings.load(a,"debian").automatic());
  WorkspaceDesktopScreen.show(a,"ubuntu",()->{},()->{});d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertFalse(this.<CheckBox>view("auto").isChecked());assertEquals("1300",this.<EditText>view("width").getText().toString());
 }
 @Test public void invalidEntryStaysOpenAndCancelDoesNotSave(){
  this.<CheckBox>view("auto").setChecked(false);text("width","999999999999999999");d.getButton(-1).performClick();assertTrue(d.isShowing());assertEquals(0,saved);assertEquals(android.view.View.VISIBLE,view("error").getVisibility());assertTrue(X11ScreenSettings.load(a,"ubuntu").automatic());d.getButton(-2).performClick();Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals(1,cancelled);
 }
 @Test public void automaticDoesNotParseDisabledManualFields(){text("width","");text("height","");text("horizontal","16");d.getButton(-1).performClick();assertEquals(1,saved);assertTrue(X11ScreenSettings.load(a,"ubuntu").automatic());assertEquals(0,X11ScreenSettings.load(a,"ubuntu").horizontalDp);}
 @Test public void invalidScaleCannotSaveOtherwiseValidSettings(){this.<CheckBox>view("auto").setChecked(false);text("scale","49");d.getButton(-1).performClick();assertEquals(0,saved);assertTrue(d.isShowing());assertEquals(100,X11ScreenSettings.load(a,"ubuntu").scalePercent);}

 @Test public void liveApplyWaitsForSuccessAndFailurePreservesPreferences(){
  d.dismiss();java.util.concurrent.atomic.AtomicReference<java.util.function.Consumer<String>> complete=new java.util.concurrent.atomic.AtomicReference<>();
  WorkspaceDesktopScreen.show(a,"ubuntu",()->saved++,()->cancelled++,(settings,done)->complete.set(done));d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();this.<CheckBox>view("auto").setChecked(false);text("scale","75");d.getButton(-1).performClick();assertEquals(0,saved);assertFalse(d.getButton(-1).isEnabled());assertFalse(view("auto").isEnabled());assertFalse(view("scale").isEnabled());assertEquals(100,X11ScreenSettings.load(a,"ubuntu").scalePercent);
  complete.get().accept("fixture failure");assertTrue(d.isShowing());assertTrue(d.getButton(-1).isEnabled());assertTrue(view("auto").isEnabled());assertTrue(view("scale").isEnabled());assertEquals(100,X11ScreenSettings.load(a,"ubuntu").scalePercent);
  d.getButton(-1).performClick();complete.get().accept(null);assertEquals(1,saved);assertFalse(d.isShowing());assertEquals(75,X11ScreenSettings.load(a,"ubuntu").scalePercent);
 }

 @Test public void autoSetupResetsAllValuesDisablesFieldsAndUsesLiveApply(){
  d.dismiss();new X11ScreenSettings(1300,600,24,12,75).save(a,"ubuntu");java.util.concurrent.atomic.AtomicReference<X11ScreenSettings> requested=new java.util.concurrent.atomic.AtomicReference<>();
  WorkspaceDesktopScreen.show(a,"ubuntu",()->saved++,()->cancelled++,(settings,done)->{requested.set(settings);done.accept(null);});d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
  assertFalse(this.<CheckBox>view("auto").isChecked());this.<CheckBox>view("auto").setChecked(true);
  for(String tag:new String[]{"width","height","scale","horizontal","vertical"}){assertFalse(tag,view(tag).isEnabled());assertEquals(.4f,((android.view.View)view(tag).getParent()).getAlpha(),.01f);}
  assertEquals("100",this.<EditText>view("scale").getText().toString());assertEquals("0",this.<EditText>view("horizontal").getText().toString());assertEquals("0",this.<EditText>view("vertical").getText().toString());
  assertEquals(75,X11ScreenSettings.load(a,"ubuntu").scalePercent);d.getButton(-1).performClick();X11ScreenSettings next=requested.get();assertTrue(next.automatic());assertEquals(100,next.scalePercent);assertEquals(0,next.horizontalDp);assertEquals(0,next.verticalDp);assertEquals(1,saved);
 }
 @Test public void oldAutomaticResolutionWithCustomScaleIsManualAndCancelPreservesIt(){
  d.dismiss();new X11ScreenSettings(0,0,16,8,75).save(a,"ubuntu");WorkspaceDesktopScreen.show(a,"ubuntu",()->saved++,()->cancelled++);d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
  assertFalse(this.<CheckBox>view("auto").isChecked());assertTrue(view("scale").isEnabled());this.<CheckBox>view("auto").setChecked(true);this.<CheckBox>view("auto").setChecked(false);
  for(String tag:new String[]{"width","height","scale","horizontal","vertical"})assertTrue(tag,view(tag).isEnabled());assertEquals("75",this.<EditText>view("scale").getText().toString());
  d.getButton(-2).performClick();assertEquals(75,X11ScreenSettings.load(a,"ubuntu").scalePercent);assertEquals(16,X11ScreenSettings.load(a,"ubuntu").horizontalDp);assertEquals(0,saved);
 }

 @Test public void disablingAutoPrefillsScreenshotPresetWithoutSavingUntilApply(){
  this.<CheckBox>view("auto").setChecked(false);
  String[] tags={"width","height","scale","horizontal","vertical"};String[] expected={"1600","720","75","5","5"};
  for(int i=0;i<tags.length;i++)assertEquals(tags[i],expected[i],this.<EditText>view(tags[i]).getText().toString());
  assertTrue(X11ScreenSettings.load(a,"ubuntu").automatic());assertEquals(0,saved);
  d.getButton(-1).performClick();X11ScreenSettings next=X11ScreenSettings.load(a,"ubuntu");
  assertEquals(1600,next.width);assertEquals(720,next.height);assertEquals(75,next.scalePercent);assertEquals(5,next.horizontalDp);assertEquals(5,next.verticalDp);assertEquals(1,saved);
 }

 @Test public void reopeningManualModePreservesItsOwnValuesInsteadOfPreset(){
  d.dismiss();new X11ScreenSettings(1920,1080,8,12,90).save(a,"ubuntu");
  WorkspaceDesktopScreen.show(a,"ubuntu",()->saved++,()->cancelled++);d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
  String[] tags={"width","height","scale","horizontal","vertical"};String[] expected={"1920","1080","90","8","12"};
  for(int i=0;i<tags.length;i++)assertEquals(tags[i],expected[i],this.<EditText>view(tags[i]).getText().toString());
 }

}
