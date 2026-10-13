package com.termux.app.workspace;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(manifest=Config.NONE)
public class WorkspaceIconButtonTest {
    static final class Recording extends Drawable {
        Rect drawn;int tint;
        @Override public void draw(Canvas canvas){drawn=new Rect(getBounds());}
        @Override public void setTint(int color){tint=color;}
        @Override public void setAlpha(int alpha){}
        @Override public void setColorFilter(ColorFilter filter){}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
    @Test public void iconsRemainCenteredAcrossNarrowAndSquareButtons() {
        for(int width:new int[]{44,48,60}) {
            WorkspaceIconButton button=new WorkspaceIconButton(RuntimeEnvironment.application);Recording icon=new Recording();button.setTextColor(Color.CYAN);button.setPadding(13,8,13,8);button.setIcon(icon,22);button.layout(0,0,width,48);
            button.onDraw(new Canvas(Bitmap.createBitmap(width,48,Bitmap.Config.ARGB_8888)));
            assertEquals(new Rect((width-22)/2,13,(width-22)/2+22,35),icon.drawn);assertEquals(0,button.getPaddingLeft());assertEquals("",button.getText().toString());assertEquals(Color.CYAN,icon.tint);
        }
    }
    @Test public void changingButtonColorAlsoTintsItsIcon() {
        WorkspaceIconButton button=new WorkspaceIconButton(RuntimeEnvironment.application);Recording icon=new Recording();button.setIcon(icon,22);button.setTextColor(Color.GREEN);assertEquals(Color.GREEN,icon.tint);
    }
    @Test public void activeFileFilterHasSelectionBackgroundAndAccessibleLabel() {
        android.widget.TextView filter=new android.widget.TextView(RuntimeEnvironment.application);filter.setText("APK");
        WorkspaceFileBrowser.styleFilter(RuntimeEnvironment.application,filter,true);assertTrue(filter.isSelected());assertEquals(WorkspaceDialogs.ACCENT,filter.getCurrentTextColor());assertTrue(filter.getContentDescription().toString().contains("выбран"));assertNotNull(filter.getBackground());
        WorkspaceFileBrowser.styleFilter(RuntimeEnvironment.application,filter,false);assertFalse(filter.isSelected());assertEquals(WorkspaceDialogs.MUTED,filter.getCurrentTextColor());assertFalse(filter.getContentDescription().toString().contains("выбран"));
    }

}
