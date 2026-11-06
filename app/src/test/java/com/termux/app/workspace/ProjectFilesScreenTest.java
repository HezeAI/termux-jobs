package com.termux.app.workspace;
import android.app.Application;import android.view.*;import android.widget.*;import com.termux.R;import com.termux.app.TermuxActivity;import org.json.JSONObject;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import java.util.*;import java.lang.reflect.Field;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class ProjectFilesScreenTest {
 @Test public void allAgentsUseFullScreenAndRetainFileManagerWhileBackOnlyClosesReaders()throws Exception{
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);a.setContentView(R.layout.activity_termux);WorkspaceLanguage.select(a,"en");ViewGroup host=a.findViewById(R.id.activity_termux_root_relative_layout);int originalChildren=host.getChildCount();
  for(String agent:new String[]{"codex","claude","gemini","opencode"}){
   WorkspaceStore.Session p=new WorkspaceStore.Session();p.name=agent;p.environment=agent.equals("codex")?"codex":"ubuntu";p.claudeAgent=agent.equals("claude");p.geminiAgent=agent.equals("gemini");p.aiAgent=agent.equals("opencode")?"opencode":"";
   int[] closed={0};AgentProjectPanel.Chat chat=new AgentProjectPanel.Chat(){public boolean valid(){return closed[0]==0;}public List<JSONObject> rows(){return Collections.emptyList();}public boolean hasOlder(){return false;}public void older(){fail("No archive on opening");}public void open(JSONObject file){fail("No opening on mount");}public boolean jump(String key){return false;}public void observe(Runnable r){}public void close(){closed[0]++;}};
   AgentProjectPanel.Backend backend=new AgentProjectPanel.Backend(){public byte[] read(String script,int limit){fail("No Git on opening Files");return null;}public void close(){}};
   WorkspaceFileBrowser browser=new WorkspaceFileBrowser(a,p,"/fixture",null);Field busy=WorkspaceFileBrowser.class.getDeclaredField("busy");busy.setAccessible(true);busy.setBoolean(browser,true);browser.projectPanel=new AgentProjectPanel(a,p,"/fixture",chat,backend);browser.show();
   View page=host.findViewWithTag("project-files-screen");assertNotNull(page);assertEquals(originalChildren+1,host.getChildCount());assertNotNull(page.findViewWithTag("workspace-file-refresh"));assertNotNull(page.findViewWithTag("workspace-file-sort"));assertNotNull(page.findViewWithTag("project-tab-0"));
   for(int height:new int[]{824,430,824}){page.measure(View.MeasureSpec.makeMeasureSpec(440,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));page.layout(0,0,440,height);assertEquals(height,page.getHeight());assertTrue("content height at "+height+" = "+browser.projectPanel.content.getHeight(),browser.projectPanel.content.getHeight()>0);assertEquals(browser.projectPanel.root.getHeight()-browser.projectPanel.root.getPaddingBottom(),browser.projectPanel.content.getBottom());assertEquals(WorkspaceDialogs.dp(a,16),browser.projectPanel.root.getPaddingLeft());}
   Field transfer=WorkspaceFileBrowser.class.getDeclaredField("transferring");transfer.setAccessible(true);transfer.setBoolean(browser,true);assertFalse(browser.leavePageForNavigation());assertNotNull(page.getParent());transfer.setBoolean(browser,false);assertTrue(browser.leavePageForNavigation());assertNull(host.findViewWithTag("project-files-screen"));assertEquals(originalChildren,host.getChildCount());assertEquals(1,closed[0]);assertFalse(browser.pageBack());
  }a.finish();
 }
 @Test public void backFromDiffPreservesProjectPageQueryListingAndReturnOrigin()throws Exception{
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);a.setContentView(R.layout.activity_termux);int[] closed={0};
  AgentProjectPanel.Chat chat=new AgentProjectPanel.Chat(){public boolean valid(){return closed[0]==0;}public List<JSONObject> rows(){return Collections.emptyList();}public boolean hasOlder(){return false;}public void older(){fail();}public void open(JSONObject file){fail();}public boolean jump(String key){return false;}public void observe(Runnable r){}public void close(){closed[0]++;}};
  AgentProjectPanel.Backend backend=new AgentProjectPanel.Backend(){public byte[] read(String s,int n){return new byte[0];}public void close(){}};
  WorkspaceStore.Session p=new WorkspaceStore.Session();WorkspaceFileBrowser browser=new WorkspaceFileBrowser(a,p,"/fixture",null);Field busy=WorkspaceFileBrowser.class.getDeclaredField("busy");busy.setAccessible(true);busy.setBoolean(browser,true);AgentProjectPanel panel=new AgentProjectPanel(a,p,"/fixture",chat,backend);browser.projectPanel=panel;
  WorkspaceSidebar origin=new WorkspaceSidebar(a);origin.show();origin.suspend();browser.setOnClosed(origin::resume);browser.show();ProjectChanges.Listing list=new ProjectChanges.Listing();list.state="OK";list.root="/fixture";list.head="HEAD";ProjectChanges.Entry entry=new ProjectChanges.Entry("src/a.java"," M");list.entries.add(entry);panel.listing=list;panel.select(2);panel.gitSearch.setText("src/");panel.selectedDiff=entry;panel.diffLoading=true;int serial=panel.gitSerial;
  assertTrue(browser.pageBack());assertNull(panel.selectedDiff);assertFalse(panel.diffLoading);assertTrue(panel.gitSerial>serial);assertSame(list,panel.listing);assertEquals("src/",panel.gitQuery);assertEquals(2,panel.selected);assertNotNull(a.findViewById(R.id.activity_termux_root_relative_layout).findViewWithTag("project-files-screen"));assertEquals(0,closed[0]);assertTrue(origin.suspended());
  assertTrue(browser.pageBack());assertEquals(1,closed[0]);assertFalse(origin.suspended());assertTrue(origin.dialog.isShowing());assertFalse(browser.pageBack());origin.dispose();a.finish();
 }

 @Test public void projectPageCoversFloatingChatHeaderAndRestoresItOnClose(){
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);a.setContentView(R.layout.activity_termux);
  View header=a.findViewById(R.id.workspace_header);header.setElevation(1);
  AgentTerminalScreen page=new AgentTerminalScreen(a,"Synthetic project","/fixture","Back","project-files-screen",()->{});page.body(new View(a));page.show();
  assertTrue(page.getElevation()>header.getElevation());assertEquals(ViewGroup.LayoutParams.MATCH_PARENT,page.getLayoutParams().width);
  page.dismiss();assertNull(page.getParent());assertEquals(1,header.getElevation(),0);a.finish();
 }

}
