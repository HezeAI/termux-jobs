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
   WorkspaceDesktops desktop=(WorkspaceDesktops)field(WorkspaceController.class,"desktopUi").get(c);Runnable changed=(Runnable)field(WorkspaceDesktops.class,"keyboardChanged").get(desktop);
   WorkspaceStore.Session p=new WorkspaceStore.Session();p.id="synthetic";p.handle="h";p.environment="termux";
   WorkspaceDesktops visible=new WorkspaceDesktops(a,()->p,()->true,a.findViewById(R.id.workspace_input),()->{},()->{});
   visible.label();java.util.Map channels=(java.util.Map)field(WorkspaceDesktops.class,"channels").get(visible);WorkspaceDesktop channel=(WorkspaceDesktop)channels.get("termux");
   ((java.util.Set<String>)field(WorkspaceDesktop.class,"graphical").get(channel)).add(p.id);field(WorkspaceController.class,"desktopUi").set(c,visible);desktop.close();render.invoke(c);
   LinearLayout header=(LinearLayout)field(WorkspaceController.class,"header").get(c);LinearLayout top=(LinearLayout)header.getChildAt(0);View menu=top.getChildAt(0),title=top.getChildAt(1);
   assertEquals("Termux",((TextView)((LinearLayout)title).getChildAt(0)).getText().toString());
   TextView button=(TextView)field(WorkspaceController.class,"desktopInputButton").get(c);assertNotNull(button);field(WorkspaceController.class,"interactiveInput").setBoolean(c,true);
   changed.run();
   assertSame(top,header.getChildAt(0));assertSame(title,top.getChildAt(1));assertSame(menu,top.getChildAt(0));assertSame(button,field(WorkspaceController.class,"desktopInputButton").get(c));assertTrue(button.isSelected());changed.run();assertSame(title,((LinearLayout)header.getChildAt(0)).getChildAt(1));

  }finally{c.close();}
 }
}
