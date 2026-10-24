package com.termux.app.workspace;

import android.app.*;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

/** Window/state contracts only. A surrogate host does not prove Android SurfaceControl/IME. */
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class DesktopFullscreenTest {
    public static class LocalizedActivity extends Activity {
        @Override protected void attachBaseContext(android.content.Context base){super.attachBaseContext(WorkspaceLanguage.wrap(base));}
        @Override public void onConfigurationChanged(Configuration current){super.onConfigurationChanged(current);WorkspaceLanguage.configurationChanged(this,current);}
    }
    private Activity activity;private FrameLayout home,host;private View marker;private DesktopFullscreen fullscreen;
    private int[] actions;private boolean keyboardActive;
    @Before public void setup(){
        activity=Robolectric.buildActivity(Activity.class).setup().get();
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        home=new FrameLayout(activity);marker=new View(activity);home.addView(marker);
        host=new FrameLayout(activity);host.setTag("existing-x11-host");home.addView(host,new FrameLayout.LayoutParams(-1,300));
        activity.setContentView(home);orientation(Configuration.ORIENTATION_PORTRAIT);
        keyboardActive=false;actions=new int[2];fullscreen=new DesktopFullscreen(activity,()->{actions[0]++;keyboardActive=!keyboardActive;},()->keyboardActive,()->actions[1]++);
    }
    @After public void cleanup(){fullscreen.close();activity.finish();}
    private void orientation(int value){Configuration config=new Configuration(activity.getResources().getConfiguration());config.orientation=value;activity.getResources().updateConfiguration(config,activity.getResources().getDisplayMetrics());}
    private void rotate(int value){orientation(value);View decor=activity.getWindow().getDecorView();decor.layout(0,0,value==Configuration.ORIENTATION_LANDSCAPE?1600:800,value==Configuration.ORIENTATION_LANDSCAPE?800:1600);shadowOf(Looper.getMainLooper()).idle();}
    private Dialog dialog()throws Exception{java.lang.reflect.Field f=DesktopFullscreen.class.getDeclaredField("dialog");f.setAccessible(true);return (Dialog)f.get(fullscreen);}
    private Dialog controls()throws Exception{java.lang.reflect.Field f=DesktopFullscreen.class.getDeclaredField("controls");f.setAccessible(true);return (Dialog)f.get(fullscreen);}
    @Test public void rotationMovesSameHostAndPortraitRestoresExactPositionAndLayout()throws Exception{
        ViewGroup.LayoutParams params=host.getLayoutParams();fullscreen.sync(host);
        assertFalse(fullscreen.isFullscreen());assertSame(home,host.getParent());
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR,activity.getRequestedOrientation());
        rotate(Configuration.ORIENTATION_LANDSCAPE);
        assertTrue(fullscreen.isFullscreen());assertNotSame(home,host.getParent());assertEquals(1,home.getChildCount());assertSame(marker,home.getChildAt(0));
        assertSame(host,((ViewGroup)host.getParent()).getChildAt(0));
        assertEquals(-1,dialog().getWindow().getAttributes().width);assertEquals(-1,dialog().getWindow().getAttributes().height);
        rotate(Configuration.ORIENTATION_PORTRAIT);
        assertFalse(fullscreen.isFullscreen());assertSame(home,host.getParent());assertSame(host,home.getChildAt(1));assertSame(params,host.getLayoutParams());
        fullscreen.sync(null);assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,activity.getRequestedOrientation());
    }
    @Test public void systemBackReturnsToEmbeddedAndDoesNotImmediatelyReopen()throws Exception{
        fullscreen.sync(host);rotate(Configuration.ORIENTATION_LANDSCAPE);Dialog buttons=controls();dialog().onBackPressed();shadowOf(Looper.getMainLooper()).idle();
        assertFalse(fullscreen.isFullscreen());assertFalse(buttons.isShowing());assertSame(home,host.getParent());
        fullscreen.sync(host);assertFalse(fullscreen.isFullscreen());assertFalse(fullscreen.back());
        rotate(Configuration.ORIENTATION_PORTRAIT);rotate(Configuration.ORIENTATION_LANDSCAPE);assertTrue(fullscreen.isFullscreen());
        assertTrue(fullscreen.back());assertSame(home,host.getParent());assertFalse(fullscreen.isFullscreen());
    }
    @Test public void topRightInputIsASeparateNonFocusableWindowWithoutGearAndPassesOutsideTouches()throws Exception{
        fullscreen.sync(host);rotate(Configuration.ORIENTATION_LANDSCAPE);LinearLayout row=(LinearLayout)((ViewGroup)controls().findViewById(android.R.id.content)).getChildAt(0);
        assertEquals(2,row.getChildCount());assertTrue(controls().isShowing());
        WindowManager.LayoutParams attributes=controls().getWindow().getAttributes();
        assertEquals(WindowManager.LayoutParams.TYPE_APPLICATION,attributes.type);
        assertTrue((attributes.flags&WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)!=0);
        assertTrue((attributes.flags&WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)!=0);
        assertEquals(0,attributes.flags&WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        assertEquals(Gravity.TOP|Gravity.RIGHT,attributes.gravity);assertEquals(.65f,attributes.alpha,.001f);
        assertEquals(WorkspaceDialogs.dp(activity,48),attributes.y);
        assertEquals(WorkspaceLanguage.code(activity).equals("ru")?"Интерактивный ввод":"Interactive input",row.getChildAt(0).getContentDescription());
        row.getChildAt(0).performClick();
        assertEquals(1,actions[0]);assertTrue(fullscreen.isFullscreen());
        assertTrue((dialog().getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_FULLSCREEN)!=0);
        assertEquals(WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES,dialog().getWindow().getAttributes().layoutInDisplayCutoutMode);
    }
    @Test public void extraKeysButtonUsesItsOwnActionAndSelectedState()throws Exception{
        fullscreen.close();boolean[] keys={false};
        fullscreen=new DesktopFullscreen(activity,()->actions[0]++,()->false,()->{},()->keys[0]=!keys[0],()->keys[0]);
        fullscreen.sync(host);rotate(Configuration.ORIENTATION_LANDSCAPE);
        LinearLayout row=(LinearLayout)((ViewGroup)controls().findViewById(android.R.id.content)).getChildAt(0);
        View button=row.getChildAt(1);button.performClick();assertTrue(keys[0]);assertTrue(button.isSelected());assertEquals(0,actions[0]);
        button.performClick();assertFalse(keys[0]);assertFalse(button.isSelected());assertSame(host,((ViewGroup)host.getParent()).getChildAt(0));
    }
    @Test public void inputFillTracksManualAutomaticAndBackStateWithoutReplacingWindows()throws Exception{
        fullscreen.sync(host);rotate(Configuration.ORIENTATION_LANDSCAPE);Dialog desktop=dialog(),overlay=controls();
        LinearLayout row=(LinearLayout)((ViewGroup)overlay.findViewById(android.R.id.content)).getChildAt(0);
        View button=row.getChildAt(0);assertFalse(button.isSelected());Object inactive=row.getBackground();
        button.performClick();assertTrue(button.isSelected());assertNotSame(inactive,row.getBackground());
        Object active=row.getBackground();for(int i=0;i<10;i++)desktop.getWindow().getDecorView().getViewTreeObserver().dispatchOnPreDraw();
        assertSame(active,row.getBackground());assertSame(overlay,controls());assertSame(desktop,dialog());
        keyboardActive=false;desktop.getWindow().getDecorView().getViewTreeObserver().dispatchOnPreDraw();assertFalse(button.isSelected());
        keyboardActive=true;desktop.getWindow().getDecorView().getViewTreeObserver().dispatchOnPreDraw();assertTrue(button.isSelected());
        button.performClick();assertFalse(button.isSelected());assertFalse(keyboardActive);assertTrue(fullscreen.isFullscreen());
    }
    @Test public void switchingAwayRestoresPolicyAndCannotReopenFromQueuedLayouts(){
        fullscreen.sync(host);rotate(Configuration.ORIENTATION_LANDSCAPE);assertTrue(fullscreen.isFullscreen());
        fullscreen.sync(null);assertFalse(fullscreen.isFullscreen());assertSame(home,host.getParent());
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,activity.getRequestedOrientation());
        rotate(Configuration.ORIENTATION_LANDSCAPE);assertFalse(fullscreen.isFullscreen());
    }
    @Test public void replacingEnvironmentHostAndClosingDoesNotResurrectRemovedView(){
        fullscreen.sync(host);rotate(Configuration.ORIENTATION_LANDSCAPE);((ViewGroup)host.getParent()).removeView(host);
        FrameLayout other=new FrameLayout(activity);home.addView(other);fullscreen.sync(other);
        assertNull(host.getParent());assertTrue(fullscreen.isFullscreen());assertNotSame(home,other.getParent());
        fullscreen.close();assertSame(home,other.getParent());assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,activity.getRequestedOrientation());
        fullscreen.sync(host);assertNull(host.getParent());assertFalse(fullscreen.isFullscreen());
    }
    @Test public void repeatedStateRefreshesReuseWindowAndControls()throws Exception{
        fullscreen.sync(host);rotate(Configuration.ORIENTATION_LANDSCAPE);Dialog first=dialog();Dialog buttons=controls();
        for(int i=0;i<20;i++)fullscreen.sync(host);
        assertSame(first,dialog());assertSame(buttons,controls());assertEquals(1,home.getChildCount());
    }
    @Test public void queuedBackFromOldWindowCannotCloseReplacementHost()throws Exception{
        fullscreen.sync(host);rotate(Configuration.ORIENTATION_LANDSCAPE);Dialog old=dialog();old.cancel();
        FrameLayout other=new FrameLayout(activity);home.addView(other);fullscreen.sync(other);Dialog replacement=dialog();
        assertNotSame(old,replacement);shadowOf(Looper.getMainLooper()).idle();
        assertTrue(fullscreen.isFullscreen());assertSame(replacement,dialog());assertNotSame(home,other.getParent());
    }
    @Test public void systemRotationReachesLocalizedActivityWithoutManuallyUpdatingItsResources(){
        fullscreen.close();activity.finish();
        RuntimeEnvironment.setQualifiers("en-w400dp-h800dp-port");
        activity=Robolectric.buildActivity(LocalizedActivity.class).setup().get();
        home=new FrameLayout(activity);host=new FrameLayout(activity);home.addView(host,new FrameLayout.LayoutParams(-1,300));activity.setContentView(home);
        fullscreen=new DesktopFullscreen(activity,()->{},()->false,()->{});fullscreen.sync(host);
        assertFalse(fullscreen.isFullscreen());
        RuntimeEnvironment.setQualifiers("en-w800dp-h400dp-land");
        activity.onConfigurationChanged(new Configuration(android.content.res.Resources.getSystem().getConfiguration()));
        View decor=activity.getWindow().getDecorView();decor.layout(0,0,1600,800);shadowOf(Looper.getMainLooper()).idle();
        assertTrue(fullscreen.isFullscreen());assertNotSame(home,host.getParent());
        RuntimeEnvironment.setQualifiers("en-w400dp-h800dp-port");activity.onConfigurationChanged(new Configuration(android.content.res.Resources.getSystem().getConfiguration()));decor.layout(0,0,800,1600);shadowOf(Looper.getMainLooper()).idle();
        assertFalse(fullscreen.isFullscreen());assertSame(home,host.getParent());
    }
}
