package com.termux.app.workspace;

import android.app.*;import android.os.*;import android.view.*;import android.widget.*;
import com.termux.app.TermuxActivity;import com.termux.R;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import org.robolectric.shadows.ShadowAlertDialog;
import java.lang.reflect.*;import java.util.concurrent.ExecutorService;
import java.io.*;import java.nio.file.Files;import java.nio.charset.StandardCharsets;import java.util.function.BooleanSupplier;import org.json.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class WorkspaceDesktopWrapperTest {
 private TermuxActivity activity(){TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);a.getSharedPreferences("workspace-x11",0).edit().clear().commit();return a;}
 private Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
 @Test public void freshDefaultLibraryNeedsNeitherPythonNorDownload(){
  TermuxActivity a=activity();WorkspaceDesktopWrapper wrapper=new WorkspaceDesktopWrapper(a);
  try{wrapper.showDrivers(()->false,()->{});AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();assertEquals(1,d.getListView().getAdapter().getCount());assertFalse(wrapper.busy());assertEquals("a6xx-a7xx-a8xx",d.getListView().getAdapter().getItem(0));assertNull(WorkspaceDesktopWrapper.metadata(a));d.dismiss();}finally{wrapper.close();}
 }
 @Test public void everyWrapperHasImportedTrashButBuiltinHasNoneAndDeletionAsksFirst()throws Exception{
  TermuxActivity a=activity();Method show=WorkspaceDesktopWrapper.class.getDeclaredMethod("driverChoices",JSONArray.class,BooleanSupplier.class,Runnable.class);show.setAccessible(true);
  String id=new String(new char[64]).replace('\0','c');JSONObject value=new JSONObject().put("id",id).put("name","Imported driver").put("minApi",99);
  for(String target:new String[]{"termux","ubuntu","debian","fedora","opensuse","void","archlinux"}){
   WorkspaceDesktopWrapper wrapper=new WorkspaceDesktopWrapper(a,target);
   try{
    show.invoke(wrapper,new JSONArray().put(value),(BooleanSupplier)()->false,(Runnable)()->{});
    AlertDialog list=ShadowAlertDialog.getLatestAlertDialog();ListAdapter adapter=list.getListView().getAdapter();
    View builtin=adapter.getView(0,null,list.getListView());assertNull(builtin.findViewWithTag("wrapper-remove-"+WorkspaceDesktopWrapper.DEFAULT_DRIVER_ID));
    View imported=adapter.getView(1,null,list.getListView());View trash=imported.findViewWithTag("wrapper-remove-"+id);assertNotNull(target,trash);assertTrue(trash.isEnabled());trash.performClick();
    AlertDialog confirm=ShadowAlertDialog.getLatestAlertDialog();assertNotSame(list,confirm);assertFalse(list.isShowing());assertTrue(confirm.isShowing());assertNotNull(confirm.getButton(AlertDialog.BUTTON_POSITIVE));assertNotNull(confirm.getButton(AlertDialog.BUTTON_NEGATIVE));assertFalse(wrapper.busy());assertNull(WorkspaceDesktopWrapper.metadata(a,target));confirm.dismiss();
   }finally{wrapper.close();}
  }
 }
 @Test public void builtinChoiceIsDeferredAndScopedAndDoesNotReplaceImportedDriver()throws Exception{
  TermuxActivity a=activity();
  assertTrue(WorkspaceDesktopWrapper.builtinSelected(a,"termux"));assertEquals("a6xx-a7xx-a8xx",WorkspaceDesktopWrapper.driverLabel(a));assertNull(WorkspaceDesktopWrapper.metadata(a));assertFalse(WorkspaceDesktopWrapper.installed(a));
  JSONObject imported=saved(a);assertFalse(WorkspaceDesktopWrapper.builtinSelected(a,"termux"));assertEquals("Turnip Gen8 V38",WorkspaceDesktopWrapper.driverLabel(a));
  WorkspaceDesktopWrapper.chooseBuiltin(a,"termux");assertEquals("a6xx-a7xx-a8xx",WorkspaceDesktopWrapper.driverLabel(a));assertEquals(imported.getString("id"),WorkspaceDesktopWrapper.metadata(a).getString("id"));assertFalse(WorkspaceDesktopWrapper.installed(a));
  for(String target:new String[]{"ubuntu","debian","fedora","opensuse","void","archlinux"}){assertTrue(WorkspaceDesktopWrapper.builtinSelected(a,target));WorkspaceDesktopWrapper.chooseImported(a,target);assertTrue(WorkspaceDesktopWrapper.builtinSelected(a,"termux"));}
  WorkspaceDesktopWrapper.chooseImported(a,"termux");assertEquals("Turnip Gen8 V38",WorkspaceDesktopWrapper.driverLabel(a));
 }
 @Test public void wrapperIsShownAndSelectedOnlyForTermux(){
  TermuxActivity a=activity();
  for(String target:new String[]{"termux","ubuntu","debian","alpine","fedora","opensuse","void","archlinux"}){
   String[] selection={""};WorkspaceDesktopGpu.select(a,target,()->false,v->selection[0]=v,()->{});AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();
   assertEquals((target.equals("termux")||target.equals("ubuntu")||target.equals("debian")||target.equals("fedora")||target.equals("archlinux")||target.equals("opensuse")||target.equals("void"))?4:3,d.getListView().getAdapter().getCount());
   if(target.equals("termux")){d.getListView().performItemClick(d.getListView().getAdapter().getView(3,null,d.getListView()),3,3);assertEquals("wrapper",selection[0]);}
   else d.dismiss();
  }
  a.getSharedPreferences("workspace-x11",0).edit().putString("renderer:termux","wrapper").putString("renderer:ubuntu","wrapper").commit();
  assertEquals("wrapper",WorkspaceDesktopGpu.mode(a,"termux"));assertEquals("cpu",WorkspaceDesktopGpu.mode(a,"ubuntu"));assertFalse(WorkspaceDesktopGpu.installed(a,"termux","wrapper"));
 }
 @Test public void selectionReturnsToMenuAndFolderIsDisabledWhileDesktopRuns()throws Exception{
  TermuxActivity a=activity();WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="termux";
  WorkspaceDesktop ui=new WorkspaceDesktop(a,()->p,()->true,new TextView(a),()->{},()->{});
  try{
   field(WorkspaceDesktop.class,"remote").set(ui,new Binder());Bundle state=new Bundle();state.putString("phase","installed");field(WorkspaceDesktop.class,"state").set(ui,state);
   Method select=WorkspaceDesktop.class.getDeclaredMethod("selectRenderer",WorkspaceStore.Session.class,String.class);select.setAccessible(true);select.invoke(ui,p,"wrapper");
   AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();View folder=d.findViewById(android.R.id.content).findViewWithTag("desktop-wrapper-folder"),driver=d.getWindow().getDecorView().findViewWithTag("agent-status-desktop-wrapper-driver");assertNotNull(folder);assertNotNull(driver);assertTrue(folder.isEnabled());assertTrue(driver.isEnabled());assertTrue(driver.hasOnClickListeners());assertTrue(d.isShowing());d.dismiss();
   state.putBoolean("started",true);ui.menu();d=ShadowAlertDialog.getLatestAlertDialog();folder=d.getWindow().getDecorView().findViewWithTag("desktop-wrapper-folder");driver=d.getWindow().getDecorView().findViewWithTag("agent-status-desktop-wrapper-driver");assertNotNull(folder);assertFalse(folder.isEnabled());assertFalse(driver.isEnabled());d.dismiss();
   state.putBoolean("started",false);
   for(String mode:new String[]{"cpu","turnip","virgl"}){a.getSharedPreferences("workspace-x11",0).edit().putString("renderer:termux",mode).commit();ui.menu();d=ShadowAlertDialog.getLatestAlertDialog();assertNull(d.getWindow().getDecorView().findViewWithTag("desktop-wrapper-folder"));assertNull(d.getWindow().getDecorView().findViewWithTag("agent-status-desktop-wrapper-driver"));d.dismiss();}
   p.environment="ubuntu";ui.menu();d=ShadowAlertDialog.getLatestAlertDialog();assertNull(d.getWindow().getDecorView().findViewWithTag("desktop-wrapper-folder"));assertNull(d.getWindow().getDecorView().findViewWithTag("agent-status-desktop-wrapper-driver"));d.dismiss();
  }finally{ui.close();}
 }
 @Test public void removedSelectionFallsBackEvenIfUiCallbackWasInterrupted()throws Exception{
  TermuxActivity a=activity();saved(a);WorkspaceDesktopWrapper.chooseImported(a,"termux");assertFalse(WorkspaceDesktopWrapper.builtinSelected(a,"termux"));
  Files.delete(new File(WorkspaceDesktopWrapper.base(a),"current.json").toPath());
  assertTrue(WorkspaceDesktopWrapper.builtinSelected(a,"termux"));assertEquals("a6xx-a7xx-a8xx",WorkspaceDesktopWrapper.driverLabel(a));assertFalse(WorkspaceDesktopWrapper.installed(a));
 }
 @Test public void serviceRejectsWrapperForAllGuestsAndLiveSwitchForTermux()throws Exception{
  com.termux.x11.X11Service service=Robolectric.buildService(com.termux.x11.X11Service.class).get();Class<?> type=com.termux.x11.X11Service.class;
  Method command=type.getDeclaredMethod("command",Bundle.class);command.setAccessible(true);
  try{
   for(String target:new String[]{"ubuntu","debian","alpine","fedora","opensuse","void","archlinux"}){
    Bundle q=new Bundle();q.putString("op","check");q.putString("target",target);q.putString("renderer","wrapper");try{command.invoke(service,q);fail(target);}catch(InvocationTargetException e){assertTrue(e.getCause() instanceof IllegalArgumentException);}assertEquals("cpu",field(type,"renderer").get(service));
   }
   field(type,"target").set(service,"termux");field(type,"started").setBoolean(service,true);
   Bundle q=new Bundle();q.putString("op","launch");q.putString("target","termux");q.putString("renderer","wrapper");try{command.invoke(service,q);fail();}catch(InvocationTargetException e){assertTrue(e.getCause() instanceof IllegalStateException);}assertEquals("cpu",field(type,"renderer").get(service));
  }finally{((ExecutorService)field(type,"worker").get(service)).shutdown();}
 }
 @Test public void diagnosticIncludesOnlyFixedDriverIdentityAndErrorCategories(){
  com.termux.x11.X11StartupLog log=new com.termux.x11.X11StartupLog();log.feed("JOBS_X11_WRAPPER_DRIVER=642cb514534dc6b7\nJOBS_GPU_RENDERER=wrapper-zink\nERROR:WRAPPER_DRIVER_CHANGED\nJOBS_X11_WRAPPER_DRIVER=private-driver-name\n");
  assertTrue(log.snapshot().contains("642cb514534dc6b7"));assertTrue(log.snapshot().contains("wrapper-zink"));assertTrue(log.snapshot().contains("WRAPPER_DRIVER_CHANGED"));assertFalse(log.snapshot().contains("private-driver-name"));
 }
 private JSONObject saved(TermuxActivity a)throws Exception{
  String id=new String(new char[64]).replace('\0','a');File base=WorkspaceDesktopWrapper.base(a),root=new File(base,"drivers/"+id);assertTrue(root.mkdirs());Files.write(new File(root,"libvulkan_freedreno.so").toPath(),new byte[]{1});
  JSONObject value=new JSONObject().put("id",id).put("name","Turnip Gen8 V38").put("library","libvulkan_freedreno.so");Files.write(new File(base,"current.json").toPath(),value.toString().getBytes(StandardCharsets.UTF_8));return value;
 }
 @Test public void adrenotoolsLabelAndQualcommMainLibraryAreRestoredWithoutLibPrefix()throws Exception{
  TermuxActivity a=activity();JSONObject value=saved(a);File base=WorkspaceDesktopWrapper.base(a),root=new File(base,"drivers/"+value.getString("id"));
  Files.move(new File(root,"libvulkan_freedreno.so").toPath(),new File(root,"vulkan.adr891.so").toPath());value.put("library","vulkan.adr891.so").put("name","891.11");Files.write(new File(base,"current.json").toPath(),value.toString().getBytes(StandardCharsets.UTF_8));
  assertEquals("vulkan.adr891.so",WorkspaceDesktopWrapper.metadata(a).getString("library"));assertEquals("891.11",WorkspaceDesktopWrapper.driverLabel(a));
  a.getSharedPreferences("workspace-x11",0).edit().putString("renderer:termux","wrapper").putString("renderer:ubuntu","bionic-glibc").commit();
  assertEquals("Wrapper · Bionic (AdrenoTools)",WorkspaceDesktopGpu.label(a,"termux"));assertFalse(WorkspaceDesktopGpu.label(a,"ubuntu").contains("AdrenoTools"));
 }
 @Test public void savedNameCardIsRestoredAfterOtherModesAndSelectingCurrentDoesNotReimport()throws Exception{
  TermuxActivity a=activity();JSONObject value=saved(a);a.getSharedPreferences("workspace-x11",0).edit().putString("renderer:termux","wrapper").commit();WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="termux";
  WorkspaceDesktop ui=new WorkspaceDesktop(a,()->p,()->true,new TextView(a),()->{},()->{});
  try{field(WorkspaceDesktop.class,"remote").set(ui,new Binder());Bundle state=new Bundle();state.putString("phase","installed");field(WorkspaceDesktop.class,"state").set(ui,state);ui.menu();AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();TextView card=d.getWindow().getDecorView().findViewWithTag("agent-status-desktop-wrapper-driver");assertEquals("Turnip Gen8 V38",card.getText().toString());d.dismiss();
   a.getSharedPreferences("workspace-x11",0).edit().putString("renderer:termux","cpu").commit();ui.menu();d=ShadowAlertDialog.getLatestAlertDialog();assertNull(d.getWindow().getDecorView().findViewWithTag("agent-status-desktop-wrapper-driver"));d.dismiss();
   a.getSharedPreferences("workspace-x11",0).edit().putString("renderer:termux","wrapper").commit();ui.menu();d=ShadowAlertDialog.getLatestAlertDialog();card=d.getWindow().getDecorView().findViewWithTag("agent-status-desktop-wrapper-driver");assertEquals("Turnip Gen8 V38",card.getText().toString());d.dismiss();
  }finally{ui.close();}
  WorkspaceDesktopWrapper wrapper=new WorkspaceDesktopWrapper(a);try{
   Method show=WorkspaceDesktopWrapper.class.getDeclaredMethod("driverChoices",JSONArray.class,BooleanSupplier.class,Runnable.class);show.setAccessible(true);JSONArray entries=new JSONArray().put(value).put(new JSONObject().put("id",new String(new char[64]).replace('\0','b')).put("name","Future driver").put("minApi",99));boolean[] updated={false};
   show.invoke(wrapper,entries,(BooleanSupplier)()->false,(Runnable)()->updated[0]=true);AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();assertEquals(3,d.getListView().getAdapter().getCount());assertTrue(d.getListView().getAdapter().isEnabled(0));assertTrue(d.getListView().getAdapter().isEnabled(1));assertTrue(d.getListView().getAdapter().isEnabled(2));View future=d.getListView().getAdapter().getView(2,null,d.getListView());assertNotNull(future.findViewWithTag("wrapper-remove-"+new String(new char[64]).replace('\0','b')));
   d.getListView().performItemClick(d.getListView().getAdapter().getView(1,null,d.getListView()),1,1);assertTrue(updated[0]);assertFalse(wrapper.busy());assertEquals(value.getString("id"),WorkspaceDesktopWrapper.metadata(a).getString("id"));
  }finally{wrapper.close();}
 }
}
