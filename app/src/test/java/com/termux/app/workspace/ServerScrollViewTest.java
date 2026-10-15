package com.termux.app.workspace;

import android.graphics.Rect;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class ServerScrollViewTest {
    private ServerScrollView scroll;private LinearLayout body;
    private void rows(int count){body.removeAllViews();for(int i=0;i<count;i++){TextView row=new TextView(RuntimeEnvironment.application);row.setText("Row "+i);row.setFocusable(true);body.addView(row,new LinearLayout.LayoutParams(-1,100));}}
    private void layout(){scroll.forceLayout();scroll.measure(View.MeasureSpec.makeMeasureSpec(400,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(300,View.MeasureSpec.EXACTLY));scroll.layout(0,0,400,300);}
    @Before public void setup(){scroll=new ServerScrollView(RuntimeEnvironment.application);body=new LinearLayout(RuntimeEnvironment.application);body.setOrientation(LinearLayout.VERTICAL);scroll.addView(body);rows(30);layout();scroll.scrollTo(0,650);assertEquals(650,scroll.getScrollY());}
    @Test public void replacementRestoresViewportAndSuppressesAutomaticFocusReveal(){
        scroll.preserve(()->{rows(20);assertFalse(scroll.requestChildRectangleOnScreen(body,new Rect(0,0,10,20),true));scroll.requestChildFocus(body,body.getChildAt(0));});layout();assertEquals(650,scroll.getScrollY());assertEquals(20,body.getChildCount());
        assertTrue(scroll.requestChildRectangleOnScreen(body,new Rect(0,1700,10,1750),true));assertTrue(scroll.getScrollY()>650);
    }
    @Test public void shorterContentClampsToItsRealBottom(){scroll.preserve(()->rows(5));layout();assertEquals(200,scroll.getScrollY());}
    @Test public void nestedUpdatesKeepTheFirstCapturedPosition(){scroll.preserve(()->{scroll.scrollTo(0,0);scroll.preserve(()->rows(25));});layout();assertEquals(650,scroll.getScrollY());}
    @Test public void userGestureTakesPriorityOverPendingRestore(){scroll.preserve(()->rows(25));MotionEvent down=MotionEvent.obtain(1,1,MotionEvent.ACTION_DOWN,10,10,0);try{scroll.onInterceptTouchEvent(down);}finally{down.recycle();}scroll.scrollTo(0,120);layout();assertEquals(120,scroll.getScrollY());}
    @Test public void explicitFullPageRestoreUsesRequestedPosition(){scroll.restoreTo(850);layout();assertEquals(850,scroll.getScrollY());}
}
