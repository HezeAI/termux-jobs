package com.termux.app;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.view.View;
import android.view.ViewGroup;
import com.termux.R;
import com.termux.app.settings.properties.TermuxAppSharedProperties;
import java.lang.reflect.Field;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

/** No property file, PTY, service or desktop is opened by this layout fixture. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=Application.class)
public class WorkspaceWidthTest {
 static class Properties extends TermuxAppSharedProperties {
  Properties(Context context){super(context);}
  @Override public void loadTermuxPropertiesFromDisk(){} // Never read actual property contents.
  @Override public int getTerminalMarginHorizontal(){return 22;}
  @Override public int getTerminalMarginVertical(){return 3;}
 }
 @Test public void portraitRotationAndReturnUseSafeWidthAndKeepInternalGutters()throws Exception{
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);a.setContentView(R.layout.activity_termux);
  Field properties=TermuxActivity.class.getDeclaredField("mProperties");properties.setAccessible(true);properties.set(a,new Properties(a));
  View root=a.findViewById(R.id.activity_termux_root_view),content=a.findViewById(R.id.activity_termux_root_relative_layout),surface=a.findViewById(R.id.workspace_terminal_surface);
  root.setPadding(9,24,13,20); // Already consumed safe system/cutout insets.
  int gutter=((ViewGroup.MarginLayoutParams)a.findViewById(R.id.terminal_view).getLayoutParams()).getMarginStart();
  for(int orientation:new int[]{Configuration.ORIENTATION_PORTRAIT,Configuration.ORIENTATION_LANDSCAPE,Configuration.ORIENTATION_PORTRAIT}){
   Configuration next=new Configuration(a.getResources().getConfiguration());next.orientation=orientation;a.onConfigurationChanged(next);
   int width=orientation==Configuration.ORIENTATION_PORTRAIT?400:800,height=orientation==Configuration.ORIENTATION_PORTRAIT?800:400;
   root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));root.layout(0,0,width,height);
   int margin=orientation==Configuration.ORIENTATION_PORTRAIT?0:Math.round(22*a.getResources().getDisplayMetrics().density);
   ViewGroup.MarginLayoutParams p=(ViewGroup.MarginLayoutParams)content.getLayoutParams();assertEquals(margin,p.leftMargin);assertEquals(margin,p.rightMargin);
   assertEquals(width-9-13-2*margin,content.getWidth());assertEquals(content.getWidth(),surface.getWidth());
   assertEquals(9,root.getPaddingLeft());assertEquals(13,root.getPaddingRight());assertTrue(root.getFitsSystemWindows());
   assertEquals(gutter,((ViewGroup.MarginLayoutParams)a.findViewById(R.id.terminal_view).getLayoutParams()).getMarginStart());
  }
  a.finish();
 }
}
