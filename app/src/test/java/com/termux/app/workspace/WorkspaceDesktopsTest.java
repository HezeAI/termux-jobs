package com.termux.app.workspace;
import android.view.View;
import com.termux.app.TermuxActivity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(manifest=Config.NONE,sdk=28)
public class WorkspaceDesktopsTest {
 @Test public void tabsReuseOneChannelPerEnvironmentAndNeverBindUnsupportedAgents()throws Exception{
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();WorkspaceStore.Session[] selected={new WorkspaceStore.Session()};selected[0].environment="termux";selected[0].prootAlias="debian";
  WorkspaceDesktops desks=new WorkspaceDesktops(a,()->selected[0],()->true,new View(a),()->{},()->{});
  java.lang.reflect.Field field=WorkspaceDesktops.class.getDeclaredField("channels");field.setAccessible(true);java.util.Map channels=(java.util.Map)field.get(desks);
  try{
   desks.label();assertEquals(1,channels.size());Object first=channels.get("debian");selected[0]=new WorkspaceStore.Session();selected[0].environment="termux";selected[0].prootAlias="debian";desks.label();assertSame(first,channels.get("debian"));
   selected[0].prootAlias="alpine";desks.label();assertEquals(2,channels.size());assertNotSame(first,channels.get("alpine"));
   selected[0].prootAlias="fedora";desks.label();assertEquals(3,channels.size());assertNotSame(first,channels.get("fedora"));
   selected[0].environment="codex";desks.label();assertEquals(3,channels.size());assertFalse(desks.showing());desks.sync();
  }finally{desks.close();}
  assertTrue(channels.isEmpty());
 }
}
