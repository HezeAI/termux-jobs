package com.termux.app.workspace;
import android.app.Application;
import android.view.View;
import android.widget.*;
import com.termux.app.TermuxActivity;
import com.termux.R;
import java.lang.reflect.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class WorkspaceDesktopHeaderTest {
 private static Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
 @Test public void keyboardCallbackKeepsHeaderTitleNavigationAndButtonViews()throws Exception{
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);a.setContentView(R.layout.activity_termux);field(TermuxActivity.class,"mTerminalView").set(a,a.findViewById(R.id.terminal_view));WorkspaceController c=new WorkspaceController(a);
  try{
   Field screen=field(WorkspaceController.class,"screen");screen.set(c,Enum.valueOf((Class)screen.getType(),"TERMINAL"));
   Method render=WorkspaceController.class.getDeclaredMethod("renderHeader");render.setAccessible(true);render.invoke(c);
   LinearLayout header=(LinearLayout)field(WorkspaceController.class,"header").get(c);View top=header.getChildAt(0),title=((LinearLayout)top).getChildAt(0),navigation=header.getChildAt(1);
   assertEquals("Termux Job’s v2.0",((TextView)((LinearLayout)title).getChildAt(0)).getText().toString());
   TextView button=(TextView)field(WorkspaceController.class,"desktopInputButton").get(c);field(WorkspaceController.class,"interactiveInput").setBoolean(c,true);
   WorkspaceDesktops desktop=(WorkspaceDesktops)field(WorkspaceController.class,"desktopUi").get(c);Runnable changed=(Runnable)field(WorkspaceDesktops.class,"keyboardChanged").get(desktop);changed.run();
   assertSame(top,header.getChildAt(0));assertSame(title,((LinearLayout)top).getChildAt(0));assertSame(navigation,header.getChildAt(1));assertSame(button,field(WorkspaceController.class,"desktopInputButton").get(c));assertTrue(button.isSelected());changed.run();assertSame(title,((LinearLayout)header.getChildAt(0)).getChildAt(0));
  }finally{c.close();}
 }
}
