package com.termux.app.workspace;
import android.app.*;
import android.graphics.Rect;
import android.view.*;
import android.widget.*;
import com.termux.x11.X11ExtraKeys;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** Overlay/routing contracts; device X11/IME remains a separate smoke check. */
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class DesktopExtraKeysTest {
    private Activity activity;private View anchor;private DesktopExtraKeys panel;private List<String> sent;private X11ExtraKeys keys;
    private final Rect bounds=new Rect(10,100,610,900);
    @Before public void setup(){activity=Robolectric.buildActivity(Activity.class).setup().get();anchor=new View(activity){@Override public boolean getGlobalVisibleRect(Rect r,android.graphics.Point offset){r.set(bounds);return true;}};activity.setContentView(anchor);sent=new ArrayList<>();keys=new X11ExtraKeys((c,d)->sent.add(c+":"+d));panel=new DesktopExtraKeys(activity,()->{});panel.show(anchor,keys::key,keys::release);}
    @After public void cleanup(){panel.close();activity.finish();}
    private Dialog dialog(){return org.robolectric.util.ReflectionHelpers.getField(panel,"dialog");}
    private TextView button(String label){return find(dialog().findViewById(android.R.id.content),label);}
    private TextView find(View v,String text){if(v instanceof TextView&&text.contentEquals(((TextView)v).getText()))return (TextView)v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){TextView found=find(((ViewGroup)v).getChildAt(i),text);if(found!=null)return found;}return null;}
    private void touch(TextView v,int action){MotionEvent e=MotionEvent.obtain(0,0,action,2,2,0);v.dispatchTouchEvent(e);e.recycle();}
    @Test public void overlayDoesNotTakeImeFocusOrBlockOutsideDesktop(){WindowManager.LayoutParams p=dialog().getWindow().getAttributes();assertTrue((p.flags&WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)!=0);assertTrue((p.flags&WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)!=0);assertEquals(0,p.flags&WindowManager.LayoutParams.FLAG_DIM_BEHIND);assertTrue(p.width<=600);assertEquals(0,sent.size());}
    @Test public void compactDefaultKeepsAllKeysVisibleWithinTheGrid(){
        WindowManager.LayoutParams p=dialog().getWindow().getAttributes();assertEquals(WorkspaceDialogs.dp(activity,400),p.width);assertEquals(WorkspaceDialogs.dp(activity,104),p.height);layoutPanel();
        ViewGroup grid=(ViewGroup)button("Esc").getParent();assertEquals(16,grid.getChildCount());
        for(int i=0;i<grid.getChildCount();i++){View key=grid.getChildAt(i);assertTrue(key.getWidth()>0);assertTrue(key.getHeight()>0);assertTrue(key.getRight()<=grid.getWidth());assertTrue(key.getBottom()<=grid.getHeight());}
        assertEquals(12f,button("Enter").getTextSize()/activity.getResources().getDisplayMetrics().scaledDensity,.01f);
    }
    @Test public void escAccessibilityClickSendsOnePhysicalPair(){button("Esc").performClick();assertEquals(Arrays.asList("111:true","111:false"),sent);}
    @Test public void touchSendsOnePairAndCancelReleasesArrow(){touch(button("Esc"),MotionEvent.ACTION_DOWN);touch(button("Esc"),MotionEvent.ACTION_UP);touch(button("↑"),MotionEvent.ACTION_DOWN);touch(button("↑"),MotionEvent.ACTION_CANCEL);assertEquals(Arrays.asList("111:true","111:false","19:true","19:false"),sent);}
    @Test public void closeReleasesLatchedModifierAndHeldArrowExactlyOnce(){button("Ctrl").performClick();assertTrue(button("Ctrl").isSelected());touch(button("←"),MotionEvent.ACTION_DOWN);panel.close();panel.close();assertEquals(Arrays.asList("113:true","21:true","21:false","113:false"),sent);assertFalse(panel.showing());}
    @Test public void replacingPanelReleasesOldTargetBeforeNewInput(){button("Alt").performClick();List<String> next=new ArrayList<>();X11ExtraKeys b=new X11ExtraKeys((c,d)->next.add(c+":"+d));panel.show(anchor,b::key,b::release);button("Enter").performClick();assertEquals(Arrays.asList("57:true","57:false"),sent);assertEquals(Arrays.asList("66:true","66:false"),next);}
    private View handle(boolean resize){ViewGroup body=(ViewGroup)((ViewGroup)dialog().findViewById(android.R.id.content)).getChildAt(0);return body.getChildAt(resize?0:2);}
    private void at(View view,int action,float x,float y){MotionEvent e=MotionEvent.obtain(0,0,action,x,y,0);view.dispatchTouchEvent(e);e.recycle();}
    private void layoutPanel(){WindowManager.LayoutParams p=dialog().getWindow().getAttributes();ViewGroup content=dialog().findViewById(android.R.id.content);content.measure(View.MeasureSpec.makeMeasureSpec(p.width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(p.height,View.MeasureSpec.EXACTLY));content.layout(0,0,p.width,p.height);}
    @Test public void rightHandleMovesOnlyPanelAndClampsToDesktop(){
        WindowManager.LayoutParams p=dialog().getWindow().getAttributes();int width=p.width,height=p.height;
        at(handle(false),MotionEvent.ACTION_DOWN,0,0);at(handle(false),MotionEvent.ACTION_MOVE,-10000,-10000);at(handle(false),MotionEvent.ACTION_UP,-10000,-10000);
        p=dialog().getWindow().getAttributes();assertEquals(bounds.left,p.x);assertEquals(bounds.top,p.y);assertEquals(width,p.width);assertEquals(height,p.height);assertTrue(sent.isEmpty());
        org.robolectric.util.ReflectionHelpers.callInstanceMethod(panel,"position");assertEquals(bounds.top,dialog().getWindow().getAttributes().y);
        button("Enter").performClick();assertEquals(Arrays.asList("66:true","66:false"),sent);
    }
    @Test public void leftHandleReflowsKeysAndPreservesModifierState(){
        layoutPanel();int oldLeft=button("Shift").getLeft(),oldTop=button("Shift").getTop();
        button("Ctrl").performClick();WindowManager.LayoutParams p=dialog().getWindow().getAttributes();int right=p.x+p.width,initialWidth=p.width,initialHeight=p.height;
        // First lift the panel to make room for a taller, narrower shape.
        at(handle(false),MotionEvent.ACTION_DOWN,0,0);at(handle(false),MotionEvent.ACTION_MOVE,0,-500);at(handle(false),MotionEvent.ACTION_UP,0,-500);
        at(handle(true),MotionEvent.ACTION_DOWN,0,0);at(handle(true),MotionEvent.ACTION_MOVE,200,-200);at(handle(true),MotionEvent.ACTION_UP,200,-200);
        p=dialog().getWindow().getAttributes();assertEquals(right,p.x+p.width);assertTrue(p.width<initialWidth);assertTrue(p.height>initialHeight);layoutPanel();
        assertTrue(oldLeft!=button("Shift").getLeft()||oldTop!=button("Shift").getTop());assertTrue(button("Ctrl").isSelected());assertEquals(Collections.singletonList("113:true"),sent);
        ViewGroup grid=(ViewGroup)button("Ctrl").getParent();for(int i=0;i<grid.getChildCount();i++){View key=grid.getChildAt(i);assertTrue(key.getWidth()>0);assertTrue(key.getHeight()>0);assertTrue(key.getRight()<=grid.getWidth());assertTrue(key.getBottom()<=grid.getHeight());}
    }
    @Test public void viewportShrinkKeepsPanelInsideAndCancelStopsDragging(){
        at(handle(false),MotionEvent.ACTION_DOWN,0,0);at(handle(false),MotionEvent.ACTION_MOVE,10000,-10000);at(handle(false),MotionEvent.ACTION_CANCEL,0,0);
        int x=dialog().getWindow().getAttributes().x;at(handle(false),MotionEvent.ACTION_MOVE,-10000,10000);assertEquals(x,dialog().getWindow().getAttributes().x);
        bounds.set(10,100,310,330);org.robolectric.util.ReflectionHelpers.callInstanceMethod(panel,"position");WindowManager.LayoutParams p=dialog().getWindow().getAttributes();assertTrue(p.x>=bounds.left);assertTrue(p.y>=bounds.top);assertTrue(p.x+p.width<=bounds.right);assertTrue(p.y+p.height<=bounds.bottom);assertTrue(sent.isEmpty());
    }
}
