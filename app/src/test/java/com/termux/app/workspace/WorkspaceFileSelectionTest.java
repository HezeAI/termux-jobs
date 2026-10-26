package com.termux.app.workspace;
import android.app.*;import android.view.*;import android.widget.*;import com.termux.R;import com.termux.app.TermuxActivity;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import org.robolectric.shadows.ShadowAlertDialog;import java.lang.reflect.*;import java.util.*;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class WorkspaceFileSelectionTest {
 WorkspaceFileBrowser browser;TermuxActivity activity;LinearLayout rows;ProjectFiles.Entry one=new ProjectFiles.Entry("/source","one.txt",false,1);
 void set(String n,Object v)throws Exception{Field f=WorkspaceFileBrowser.class.getDeclaredField(n);f.setAccessible(true);f.set(browser,v);}Object get(String n)throws Exception{Field f=WorkspaceFileBrowser.class.getDeclaredField(n);f.setAccessible(true);return f.get(browser);}void render()throws Exception{Method m=WorkspaceFileBrowser.class.getDeclaredMethod("render");m.setAccessible(true);m.invoke(browser);}
 void listing(String path,ProjectFiles.Entry... entries)throws Exception{ProjectFiles.Listing listing=new ProjectFiles.Listing();listing.path=path;listing.entries.addAll(Arrays.asList(entries));set("listing",listing);set("path",path);render();}
 @Before public void setup()throws Exception{activity=Robolectric.buildActivity(TermuxActivity.class).get();activity.setTheme(R.style.Theme_Termux_Black);WorkspaceLanguage.select(activity,"en");FileSort.save(activity,FileSort.Order.NAME_ASC);WorkspaceStore.Session p=new WorkspaceStore.Session();p.name="fixture";p.environment="ubuntu";browser=new WorkspaceFileBrowser(activity,p,"/source",null);set("busy",true);browser.show();Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();set("busy",false);rows=(LinearLayout)get("rows");listing("/source",one);}
 @After public void close(){browser.close();}
 @Test public void longPressAndCheckboxSelectionKeepFileMenuAvailable()throws Exception{
  assertFalse(((View)get("transferButton")).isEnabled());assertTrue(rows.getChildAt(0).performLongClick());assertTrue(((View)get("transferButton")).isEnabled());assertEquals(View.VISIBLE,((View)get("clearTransferButton")).getVisibility());
  ViewGroup line=(ViewGroup)rows.getChildAt(0);assertTrue(line.getChildAt(0) instanceof CheckBox);line.getChildAt(1).performClick();assertTrue(ShadowAlertDialog.getLatestAlertDialog().isShowing());assertEquals(1,((Map<?,?>)get("transferSelection")).size());((CheckBox)line.getChildAt(0)).performClick();assertTrue(((Map<?,?>)get("transferSelection")).isEmpty());assertFalse(((View)get("transferButton")).isEnabled());
 }
 @Test public void selectionSurvivesFolderAndFilterChangesAndMenuUsesCurrentDestination()throws Exception{
  rows.getChildAt(0).performLongClick();listing("/destination",new ProjectFiles.Entry("/destination","other",true,0));set("filter","APK");render();assertEquals(1,((Map<?,?>)get("transferSelection")).size());Method m=WorkspaceFileBrowser.class.getDeclaredMethod("transferMenu");m.setAccessible(true);m.invoke(browser);AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();assertEquals("Copy here",dialog.getButton(-1).getText().toString());assertEquals("Move here",dialog.getButton(-3).getText().toString());assertEquals("Back",dialog.getButton(-2).getText().toString());assertTrue(((TextView)dialog.findViewById(android.R.id.message)).getText().toString().contains("/destination"));dialog.getButton(-2).performClick();assertEquals(1,((Map<?,?>)get("transferSelection")).size());
 }
 @Test public void clearButtonRemovesSelectionAndDisablesDestinationAction()throws Exception{rows.getChildAt(0).performLongClick();assertEquals("× 1",((TextView)get("clearTransferButton")).getText().toString());((View)get("clearTransferButton")).performClick();assertTrue(((Map<?,?>)get("transferSelection")).isEmpty());assertEquals(View.GONE,((View)get("clearTransferButton")).getVisibility());assertFalse(((View)get("transferButton")).isEnabled());}

 @Test public void selectAllAppearsOnlyWithSelectionAndHonorsFilterAndSearch()throws Exception{
  AlertDialog dialog=(AlertDialog)get("dialog");Button all=dialog.getButton(-3);assertEquals(View.GONE,all.getVisibility());rows.getChildAt(0).performLongClick();assertEquals(View.VISIBLE,all.getVisibility());
  listing("/other",new ProjectFiles.Entry("/other","match.apk",false,1),new ProjectFiles.Entry("/other","match.txt",false,1),new ProjectFiles.Entry("/other","other.apk",false,1),new ProjectFiles.Entry("/other","match-dir",true,0));set("filter","APK");((EditText)get("search")).setText("match");all.performClick();Map<?,?> selected=(Map<?,?>)get("transferSelection");assertEquals(3,selected.size());assertTrue(selected.containsKey(one.path));assertTrue(selected.containsKey("/other/match.apk"));assertTrue(selected.containsKey("/other/match-dir"));assertTrue(dialog.isShowing());((View)get("clearTransferButton")).performClick();assertEquals(View.GONE,all.getVisibility());
 }
 @Test public void selectAllStopsAtLimitAndKeepsOtherFolderSelection()throws Exception{rows.getChildAt(0).performLongClick();ProjectFiles.Entry[] entries=new ProjectFiles.Entry[60];for(int i=0;i<entries.length;i++)entries[i]=new ProjectFiles.Entry("/other","file"+i,false,1);listing("/other",entries);((AlertDialog)get("dialog")).getButton(-3).performClick();Map<?,?> selected=(Map<?,?>)get("transferSelection");assertEquals(50,selected.size());assertTrue(selected.containsKey(one.path));assertTrue(((TextView)get("message")).getText().toString().contains("50"));}

 @Test public void sortButtonReordersLocallyAndKeepsSelection()throws Exception{
  listing("/source",one,new ProjectFiles.Entry("/source","large.zip",false,90,20));
  // Keep a transfer selection while changing display order.
  rows.getChildAt(1).performLongClick();View sort=((AlertDialog)get("dialog")).findViewById(android.R.id.content).findViewWithTag("workspace-file-sort");assertNotNull(sort);
  ViewGroup navigation=(ViewGroup)sort.getParent();assertEquals(sort,navigation.getChildAt(navigation.getChildCount()-1));assertTrue(Arrays.asList("Home directory","Домашний каталог").contains(navigation.getChildAt(navigation.getChildCount()-2).getContentDescription().toString()));
  sort.performClick();AlertDialog menu=ShadowAlertDialog.getLatestAlertDialog();Shadows.shadowOf(menu).clickOnItem(FileSort.Order.SIZE_DESC.ordinal());
  assertEquals(FileSort.Order.SIZE_DESC,FileSort.read(activity));assertEquals("large.zip",((TextView)((ViewGroup)rows.getChildAt(0)).getChildAt(1)).getText().toString().split("\n")[0].substring(3));
  assertEquals(1,((Map<?,?>)get("transferSelection")).size());assertTrue(((Map<?,?>)get("transferSelection")).containsKey(one.path));assertTrue(((AlertDialog)get("dialog")).isShowing());
 }

}
