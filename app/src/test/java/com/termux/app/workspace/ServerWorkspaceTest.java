package com.termux.app.workspace;
import android.content.*;
import android.widget.*;
import android.view.*;
import com.termux.R;
import com.termux.app.TermuxActivity;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class ServerWorkspaceTest {
    private TermuxActivity activity(){TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);return a;}
    private ServerProfiles.Profile profile(){ServerProfiles.Profile p=new ServerProfiles.Profile();p.name="Test";p.host="example.invalid";p.user="tester";return p;}
    @Test public void separateStoreRoundTripPreservesProjectsWithoutChangingLibrary()throws Exception{
        Context c=RuntimeEnvironment.application;File storeFile=new File(c.getFilesDir(),"workspace-servers-v1.json");storeFile.delete();File library=new File(c.getFilesDir(),"workspace-v1.json");try(FileOutputStream out=new FileOutputStream(library)){out.write("library unchanged".getBytes("UTF-8"));}
        ServerProfiles store=new ServerProfiles(c);ServerProfiles.Profile p=profile();p.projects.add(new ServerProfiles.Project("project","/srv/project"));store.profiles.add(p);store.save();
        ServerProfiles loaded=new ServerProfiles(c);assertTrue(loaded.writable);assertEquals(1,loaded.profiles.size());assertEquals("/srv/project",loaded.profiles.get(0).projects.get(0).path);assertEquals("library unchanged",new String(java.nio.file.Files.readAllBytes(library.toPath()),"UTF-8"));storeFile.delete();library.delete();
    }
    @Test public void malformedStoreFailsClosedAndIsNotOverwritten()throws Exception{
        Context c=RuntimeEnvironment.application;File f=new File(c.getFilesDir(),"workspace-servers-v1.json");try(FileOutputStream out=new FileOutputStream(f)){out.write("{broken".getBytes("UTF-8"));}ServerProfiles s=new ServerProfiles(c);assertFalse(s.writable);try{s.save();fail();}catch(IOException expected){}assertEquals("{broken",new String(java.nio.file.Files.readAllBytes(f.toPath()),"UTF-8"));f.delete();
    }
    @Test public void sshArgumentsPinHostAndNeverForwardAgent()throws Exception{
        ServerConnection c=new ServerConnection(activity(),profile());try{List<String> args=c.ssh();assertTrue(args.contains("StrictHostKeyChecking=yes"));assertTrue(args.contains("ForwardAgent=no"));assertTrue(args.contains("BatchMode=yes"));assertTrue(args.contains("GlobalKnownHostsFile=/dev/null"));assertTrue(args.stream().anyMatch(x->x.startsWith("UserKnownHostsFile=")));assertFalse(args.contains("StrictHostKeyChecking=no"));}finally{c.close();}
    }
    @Test public void keyPathIsSeparateArgumentAndPasswordNotInCommandLine(){
        ServerProfiles.Profile p=profile();p.auth="key";p.identity="/tmp/key with ' quotes";ServerConnection c=new ServerConnection(activity(),p);try{List<String> a=c.ssh();assertEquals(p.identity,a.get(a.indexOf("-i")+1));assertTrue(a.contains("IdentitiesOnly=yes"));}finally{c.close();}
    }
    @Test public void encryptedKeyUsesOnlyPublicKeyAndKeepsSecretOutOfArguments(){
        ServerProfiles.Profile p=profile();p.auth="key";p.identity="/private/key";p.encrypted="ciphertext-fixture";
        ServerConnection c=new ServerConnection(activity(),p);try{List<String> a=c.ssh();assertTrue(a.contains("BatchMode=no"));assertTrue(a.contains("PreferredAuthentications=publickey"));assertTrue(a.contains("PasswordAuthentication=no"));assertFalse(a.toString().contains(p.encrypted));String fileCommand=ProjectFiles.command(p.session(),"pwd");assertTrue(fileCommand.contains("BatchMode=no"));assertTrue(fileCommand.contains("PasswordAuthentication=no"));assertFalse(fileCommand.contains(p.encrypted));}finally{c.close();}
    }
    @Test public void projectTerminalUsesSelectedRemoteDirectory(){ServerConnection c=new ServerConnection(activity(),profile());try{String cmd=c.terminalCommand("/srv/project with spaces");assertTrue(cmd.contains("/srv/project with spaces"));assertTrue(cmd.contains("StrictHostKeyChecking=yes"));assertTrue(cmd.contains("UserKnownHostsFile="));}finally{c.close();}}
    @Test public void invalidServerProfileRejectedBeforeProcessCreation(){ServerProfiles.Profile p=profile();p.host="-oProxyCommand=evil";assertFalse(ServerProfiles.valid(p));p.host="host";p.port=0;assertFalse(ServerProfiles.valid(p));p.port=22;p.user="root;evil";assertFalse(ServerProfiles.valid(p));}
    @Test public void passwordCiphertextIsBoundToServerIdentity(){ServerProfiles.Profile p=profile();p.encrypted="encrypted";assertEquals(p.id,p.session().id);assertEquals(p.host,p.session().sshHost);assertEquals(p.user,p.session().sshUser);assertEquals("encrypted",p.session().sshEncryptedPassword);}
    @Test public void existingSshSessionOptionRemainsAvailable()throws Exception{
        WorkspaceStore.Session s=new WorkspaceStore.Session();s.environment="ssh";s.sshHost="example.invalid";s.sshUser="tester";assertTrue(SshSessionShell.command(s).startsWith("ssh -t"));
        String controller=new String(java.nio.file.Files.readAllBytes(new File("src/main/java/com/termux/app/workspace/WorkspaceController.java").toPath()),"UTF-8");assertTrue(controller.contains("\"local\", \"ssh\", \"codex\""));
    }
    private boolean contains(View v,String value){if(v instanceof TextView&&((TextView)v).getText().toString().contains(value))return true;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)if(contains(((ViewGroup)v).getChildAt(i),value))return true;return false;}
    @Test public void serverScreenOpensWithoutSshOrAutomaticInstallation(){TermuxActivity a=activity();WorkspaceServers s=new WorkspaceServers(a,(name,command)->fail("Should not launch terminals"));LinearLayout root=new LinearLayout(a);s.attach(root);assertTrue(contains(root,"Добавить сервер")||contains(root,"Add server"));assertEquals(0,s.store.profiles.size());s.close();}
    @Test public void infoExplainsPermissionsRemovalAndBackground(){TermuxActivity a=activity();WorkspaceLanguage.select(a,"ru");WorkspaceServers s=new WorkspaceServers(a,(n,c)->{});s.info();String text=((TextView)ShadowAlertDialog.getLatestAlertDialog().findViewById(android.R.id.message)).getText().toString();assertTrue(text.startsWith("Устанавливается"));assertFalse(text.contains("не ИИ"));assertFalse(text.contains("метрик"));assertFalse(text.contains("График"));assertTrue(text.contains("root"));assertTrue(text.contains("удаляется"));assertTrue(text.contains("мониторинг приостановлен"));s.close();}
    @Test public void monitoringPausesOnBackgroundAndClose()throws Exception{WorkspaceServers s=new WorkspaceServers(activity(),(n,c)->{});Field f=WorkspaceServers.class.getDeclaredField("foreground");f.setAccessible(true);s.resume();assertTrue(f.getBoolean(s));s.pause();assertFalse(f.getBoolean(s));s.close();}
    private void set(Object object,String name,Object value)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);f.set(object,value);}
    private Object get(Object object,String name)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    private org.json.JSONObject snapshot(long time)throws Exception{return new org.json.JSONObject("{\"protocol\":1,\"metrics\":{\"time\":"+time+",\"cpu\":10,\"ramUsed\":100,\"ramTotal\":200,\"diskUsed\":100,\"diskTotal\":300},\"history\":[],\"tasks\":[]}");}
    @SuppressWarnings("unchecked") private WorkspaceServers serverScreen(LinearLayout root)throws Exception{
        TermuxActivity a=(TermuxActivity)root.getContext();WorkspaceLanguage.select(a,"ru");WorkspaceServers s=new WorkspaceServers(a,(n,c)->{});ServerProfiles.Profile p=profile();set(s,"selected",p);set(s,"tab","files");set(s,"foreground",true);((Map<String,org.json.JSONObject>)get(s,"snapshots")).put(p.id,snapshot(1));set(s,"listing",new org.json.JSONObject("{\"path\":\"/srv\",\"parent\":\"/\",\"entries\":[]}"));s.attach(root);return s;
    }
    private View description(View v,String label){if(label.equals(v.getContentDescription()))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View found=description(((ViewGroup)v).getChildAt(i),label);if(found!=null)return found;}return null;}
    private View textView(View v,String label){if(v instanceof TextView&&label.equals(((TextView)v).getText().toString()))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View found=textView(((ViewGroup)v).getChildAt(i),label);if(found!=null)return found;}return null;}
    @Test public void fileControlsAreCenteredVectorsWithEqualTouchTargetsAndGaps()throws Exception{
        LinearLayout root=new LinearLayout(activity());WorkspaceServers s=serverScreen(root);
        try{View up=description(root,"На уровень выше"),home=description(root,"Домашний каталог сервера"),path=description(root,"Путь…"),refresh=description(root,"Обновить");assertNotNull(up);assertNotNull(home);assertNotNull(path);assertNotNull(refresh);
            for(View v:new View[]{up,home,path,refresh}){assertTrue(v instanceof WorkspaceIconButton);assertEquals(up.getLayoutParams().width,v.getLayoutParams().width);assertEquals(up.getLayoutParams().height,v.getLayoutParams().height);}
            assertTrue(((LinearLayout.LayoutParams)path.getLayoutParams()).getMarginStart()>0);
        }finally{s.close();}
    }
    @Test public void serverSectionsHaveGapsAndRoundedSelectedBackground()throws Exception{
        LinearLayout root=new LinearLayout(activity());WorkspaceServers s=serverScreen(root);try{View projects=textView(root,"Проекты"),tasks=textView(root,"Задачи"),files=textView(root,"Файлы"),services=textView(root,"Службы");assertNotNull(projects);assertNotNull(tasks);assertNotNull(files);assertNotNull(services);assertTrue(((LinearLayout.LayoutParams)projects.getLayoutParams()).getMarginEnd()>0);assertTrue(((LinearLayout.LayoutParams)tasks.getLayoutParams()).getMarginEnd()>0);assertTrue(files.isSelected());assertFalse(files.getBackground() instanceof android.graphics.drawable.ColorDrawable);}finally{s.close();}
    }
    @Test public void addServerHasOneTextButtonWithoutPlus()throws Exception{
        TermuxActivity a=activity();WorkspaceLanguage.select(a,"ru");WorkspaceServers s=new WorkspaceServers(a,(n,c)->{});LinearLayout root=new LinearLayout(a);s.attach(root);try{assertNotNull(textView(root,"Добавить сервер"));assertNull(textView(root,"+"));assertNull(textView(root,"+ Добавить сервер"));assertTrue(contains(root,"Профиль хранится на телефоне"));assertTrue(contains(root,"остаются там после удаления профиля"));}finally{s.close();}
    }
    @Test public void liveCardUpdateKeepsScrollAndFileControlsInstances()throws Exception{
        LinearLayout root=new LinearLayout(activity());WorkspaceServers s=serverScreen(root);try{View scroll=root.getChildAt(0),path=description(root,"Путь…"),graph=(View)get(s,"metricCards");Method update=WorkspaceServers.class.getDeclaredMethod("updateLiveViews",org.json.JSONObject.class);update.setAccessible(true);update.invoke(s,snapshot(42));assertSame(scroll,root.getChildAt(0));assertSame(path,description(root,"Путь…"));assertSame(graph,get(s,"metricCards"));assertFalse(contains(root,"Подключение / выполнение…"));assertTrue(((TextView)get(s,"sampleText")).getText().toString().endsWith("42"));}finally{s.close();}
    }
    @Test public void quietPollFailureDoesNotRebuildOrUseManualBusyState()throws Exception{
        LinearLayout root=new LinearLayout(activity());WorkspaceServers s=serverScreen(root);try{View scroll=root.getChildAt(0);Method run=WorkspaceServers.class.getDeclaredMethod("run",WorkspaceServers.Work.class,WorkspaceServers.Done.class,boolean.class);run.setAccessible(true);
            run.invoke(s,(WorkspaceServers.Work)()->{throw new IOException("SSH_FAILED");},(WorkspaceServers.Done)value->{},false);
            assertSame(scroll,root.getChildAt(0));assertFalse((Boolean)get(s,"busy"));
            ((java.util.concurrent.ExecutorService)get(s,"worker")).submit(()->{}).get(5,java.util.concurrent.TimeUnit.SECONDS);org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            assertSame(scroll,root.getChildAt(0));assertFalse((Boolean)get(s,"busy"));assertFalse((Boolean)get(s,"pollBusy"));assertTrue(((TextView)get(s,"monitorStatus")).getText().toString().contains("SSH_FAILED"));assertFalse(contains(root,"Подключение / выполнение…"));
        }finally{s.close();}
    }

    @Test public void fileSizesNeverRoundSmallFilesToZeroMegabytes(){assertEquals("2.5 KB",ServerFileInfo.size(2514,false));assertEquals("0 B",ServerFileInfo.size(0,false));assertEquals("12 B",ServerFileInfo.size(12,false));assertEquals("1.0 GB",ServerFileInfo.size(1024L*1024*1024,false));assertTrue(ServerFileInfo.size(2514,true).contains("КБ"));}
    @Test public void propertiesHaveReadableDatePathAndNoMediaHintForText()throws Exception{
        org.json.JSONObject data=new org.json.JSONObject().put("path","/root/.bash_history").put("size",2514).put("modified",0);
        String text=ServerFileInfo.properties(data,true,TimeZone.getTimeZone("UTC"));assertTrue(text.contains("Имя: .bash_history"));assertTrue(text.contains("Путь: /root/.bash_history"));assertTrue(text.contains("01.01.1970 00:00:00 UTC"));assertFalse(text.contains("ffprobe"));assertFalse(text.contains("FFmpeg"));assertFalse(text.contains("\\/root"));assertFalse(text.contains("{\""));
    }
    @Test public void mediaHintAndStreamDetailsAreConditional()throws Exception{
        org.json.JSONObject data=new org.json.JSONObject("{\"path\":\"/srv/movie.mp4\",\"size\":1024,\"modified\":0,\"components\":{\"ffprobe\":false}}");assertTrue(ServerFileInfo.properties(data,false,TimeZone.getTimeZone("UTC")).contains("Install FFmpeg"));data.put("media",new org.json.JSONObject("{\"format\":{\"duration\":\"125.9\"},\"streams\":[{\"codec_name\":\"h264\",\"width\":1920,\"height\":1080}]}"));String text=ServerFileInfo.properties(data,false,TimeZone.getTimeZone("UTC"));assertTrue(text.contains("00:02:05"));assertTrue(text.contains("1920×1080"));assertTrue(text.contains("h264"));assertFalse(text.contains("Install FFmpeg"));assertTrue(ServerFileInfo.media("MOVIE.MKV"));assertFalse(ServerFileInfo.media(".bash_history"));assertFalse(ServerFileInfo.media("source.ts"));
    }
    @Test public void dependencyInstallCommandsAreFixedAndNoninteractive(){String root=ServerFileInfo.installCommand(true),user=ServerFileInfo.installCommand(false);assertFalse(root.contains("sudo"));assertTrue(user.startsWith("sudo -n "));assertTrue(root.contains("apt-get update &&"));assertTrue(root.endsWith("apt-get install --no-install-recommends -y ffmpeg"));assertFalse(root.contains("upgrade"));}
    @Test public void folderRowsUseSquareVectorIconsAndASeparateChevron()throws Exception{
        WorkspaceServers s=new WorkspaceServers(activity(),(n,c)->{});try{Method row=WorkspaceServers.class.getDeclaredMethod("fileRow",org.json.JSONObject.class);row.setAccessible(true);LinearLayout folder=(LinearLayout)row.invoke(s,new org.json.JSONObject().put("name","folder").put("path","/srv/folder").put("directory",true));assertTrue(folder.getChildAt(0) instanceof ImageView);ImageView icon=(ImageView)folder.getChildAt(0);assertEquals(icon.getLayoutParams().width,icon.getLayoutParams().height);assertEquals(ImageView.ScaleType.FIT_CENTER,icon.getScaleType());assertNotNull(icon.getDrawable());assertTrue(folder.getChildAt(2) instanceof ImageView);assertFalse(contains(folder,"▱"));}finally{s.close();}
    }
    private static class FakeProcess extends Process {boolean running=true;public OutputStream getOutputStream(){return new ByteArrayOutputStream();}public InputStream getInputStream(){return new ByteArrayInputStream(new byte[0]);}public InputStream getErrorStream(){return new ByteArrayInputStream(new byte[0]);}public int waitFor(){running=false;return 0;}public int exitValue(){if(running)throw new IllegalThreadStateException();return 0;}public void destroy(){running=false;}}
    @Test public void videoPanelOnlyAppearsForLiveTunnelAndTracksFileName()throws Exception{
        LinearLayout root=new LinearLayout(activity());WorkspaceServers s=serverScreen(root);ServerConnection c=null;try{LinearLayout panel=(LinearLayout)get(s,"streamPanel");assertEquals(View.GONE,panel.getVisibility());assertEquals(0,panel.getChildCount());ServerProfiles.Profile p=(ServerProfiles.Profile)get(s,"selected");c=new ServerConnection((TermuxActivity)root.getContext(),p);((Map<String,ServerConnection>)get(s,"connections")).put(p.id,c);FakeProcess stream=new FakeProcess();set(c,"tunnel",stream);((Map<String,String>)get(s,"streamNames")).put(p.id,"movie.mp4");Method update=WorkspaceServers.class.getDeclaredMethod("updateStreamPanel");update.setAccessible(true);update.invoke(s);assertEquals(View.VISIBLE,panel.getVisibility());assertTrue(contains(panel,"movie.mp4"));((Map<String,String>)get(s,"streamNames")).put(p.id,"second.mp4");update.invoke(s);assertTrue(contains(panel,"second.mp4"));assertFalse(contains(panel,"movie.mp4"));stream.destroy();update.invoke(s);assertEquals(View.GONE,panel.getVisibility());assertEquals(0,panel.getChildCount());}finally{s.close();}
    }
    @Test public void stoppingVideoDoesNotStopIndependentRpcProcess()throws Exception{ServerConnection c=new ServerConnection(activity(),profile());FakeProcess tunnel=new FakeProcess(),rpc=new FakeProcess();try{set(c,"tunnel",tunnel);set(c,"process",rpc);assertTrue(c.tunnelActive());c.stopTunnel();assertFalse(tunnel.running);assertTrue(rpc.running);assertFalse(c.tunnelActive());}finally{c.close();}}

    @Test public void componentInstallRequiresConfirmationAndCancelStartsNothing()throws Exception{
        LinearLayout root=new LinearLayout(activity());WorkspaceServers s=serverScreen(root);try{Method method=WorkspaceServers.class.getDeclaredMethod("installFfmpeg",ServerProfiles.Profile.class,org.json.JSONObject.class);method.setAccessible(true);method.invoke(s,get(s,"selected"),new org.json.JSONObject().put("agent",true).put("apt",true).put("uid",0).put("home","/root"));android.app.AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();String message=((TextView)dialog.findViewById(android.R.id.message)).getText().toString();assertTrue(message.contains("apt-get install --no-install-recommends -y ffmpeg"));assertFalse((Boolean)get(s,"busy"));assertTrue(((Map<?,?>)get(s,"connections")).isEmpty());dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).performClick();assertTrue(((Map<?,?>)get(s,"connections")).isEmpty());}finally{s.close();}
    }
    @Test public void agentSettingsAreFirstAndGroupStatusRemovalAndComponentsWithoutRemoteWork()throws Exception{
        LinearLayout root=new LinearLayout(activity());WorkspaceServers s=serverScreen(root);try{Method method=WorkspaceServers.class.getDeclaredMethod("menu");method.setAccessible(true);method.invoke(s);android.app.AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();android.widget.ListAdapter items=dialog.getListView().getAdapter();assertEquals("Настройки агента",items.getItem(0));assertEquals("Отключиться",items.getItem(3));for(int i=0;i<items.getCount();i++){assertFalse(items.getItem(i).toString().contains("Установить"));assertNotEquals("Компоненты агента",items.getItem(i));}dialog.getListView().performItemClick(null,0,0);
            android.widget.ListAdapter child=ShadowAlertDialog.getLatestAlertDialog().getListView().getAdapter();assertEquals(3,child.getCount());assertEquals("Проверить состояние агента",child.getItem(0));assertEquals("Удалить агента…",child.getItem(1));assertEquals("Компоненты агента",child.getItem(2));assertTrue(((Map<?,?>)get(s,"connections")).isEmpty());
        }finally{s.close();}
    }
    @Test public void inlineFileReadChangesOnlyFileListAndRetainsPageAndCards()throws Exception{
        LinearLayout root=new LinearLayout(activity());WorkspaceServers s=serverScreen(root);try{View scroll=root.getChildAt(0),graph=(View)get(s,"metricCards"),path=description(root,"Путь…");Method run=WorkspaceServers.class.getDeclaredMethod("runInline",WorkspaceServers.Work.class,WorkspaceServers.Done.class,java.util.function.Consumer.class);run.setAccessible(true);org.json.JSONObject result=new org.json.JSONObject("{\"path\":\"/srv/new\",\"entries\":[{\"name\":\"movie.mp4\",\"path\":\"/srv/new/movie.mp4\",\"size\":2514}]}");
            run.invoke(s,(WorkspaceServers.Work)()->result,(WorkspaceServers.Done)value->{set(s,"listing",value);set(s,"directory",value.getString("path"));Method update=WorkspaceServers.class.getDeclaredMethod("updateFileViews");update.setAccessible(true);update.invoke(s);},(java.util.function.Consumer<String>)code->fail(code));
            assertSame(scroll,root.getChildAt(0));assertFalse(contains(root,"Подключение / выполнение…"));((java.util.concurrent.ExecutorService)get(s,"worker")).submit(()->{}).get(5,java.util.concurrent.TimeUnit.SECONDS);org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertSame(scroll,root.getChildAt(0));assertSame(graph,get(s,"metricCards"));assertSame(path,description(root,"Путь…"));assertTrue(contains(root,"movie.mp4"));assertFalse((Boolean)get(s,"busy"));
        }finally{s.close();}
    }
    @Test public void inlineReadFailureKeepsExistingFileRows()throws Exception{
        LinearLayout root=new LinearLayout(activity());WorkspaceServers s=serverScreen(root);try{View scroll=root.getChildAt(0),rows=(View)get(s,"fileRows");Method run=WorkspaceServers.class.getDeclaredMethod("runInline",WorkspaceServers.Work.class,WorkspaceServers.Done.class,java.util.function.Consumer.class);run.setAccessible(true);java.util.concurrent.atomic.AtomicBoolean failed=new java.util.concurrent.atomic.AtomicBoolean();run.invoke(s,(WorkspaceServers.Work)()->{throw new IOException("SSH_FAILED");},(WorkspaceServers.Done)value->fail(),(java.util.function.Consumer<String>)code->failed.set(code.equals("SSH_FAILED")));((java.util.concurrent.ExecutorService)get(s,"worker")).submit(()->{}).get(5,java.util.concurrent.TimeUnit.SECONDS);org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertTrue(failed.get());assertSame(scroll,root.getChildAt(0));assertSame(rows,get(s,"fileRows"));assertFalse((Boolean)get(s,"busy"));}finally{s.close();}
    }

    @Test public void playerAcceptsOnlyGeneratedLoopbackCapabilities(){
        String token=String.join("",java.util.Collections.nCopies(64,"a"));assertTrue(WorkspaceServerPlayer.validUrl("http://127.0.0.1:3333/"+token));
        for(String url:new String[]{"https://example.com/"+token,"http://localhost:3333/"+token,"http://127.0.0.1:3333/"+token+"?x=1","http://user@127.0.0.1:3333/"+token,"file:///tmp/media","http://127.0.0.1:3333/no-token"})assertFalse(url,WorkspaceServerPlayer.validUrl(url));
    }
    @Test public void serviceReadUsesInlineUpdateWithoutReplacingPage()throws Exception{
        LinearLayout root=new LinearLayout(activity());WorkspaceServers s=serverScreen(root);try{set(s,"tab","services");set(s,"listing",new org.json.JSONObject().put("text","old log"));s.rerender();View original=root.getChildAt(0);TextView log=(TextView)get(s,"serviceText");assertNotNull(log);
            Method run=WorkspaceServers.class.getDeclaredMethod("runInline",WorkspaceServers.Work.class,WorkspaceServers.Done.class,java.util.function.Consumer.class);run.setAccessible(true);
            run.invoke(s,(WorkspaceServers.Work)()->new org.json.JSONObject().put("text","new log"),(WorkspaceServers.Done)value->log.setText(value.getString("text")),(java.util.function.Consumer<String>)code->fail(code));
            ((java.util.concurrent.ExecutorService)get(s,"worker")).submit(()->{}).get(5,java.util.concurrent.TimeUnit.SECONDS);org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertSame(original,root.getChildAt(0));assertSame(log,get(s,"serviceText"));assertEquals("new log",log.getText().toString());
        }finally{s.close();}
    }

    @Test public void unchangedLogIsNotReassignedAndAppendedOutputRetainsView(){
        TermuxActivity a=activity();TextView log=new TextView(a);log.setTextIsSelectable(true);log.setText("line one\n");ScrollView viewport=new ScrollView(a);viewport.addView(log);CharSequence before=log.getText();WorkspaceServers.updateLog(log,viewport,"line one\n");assertSame(before,log.getText());WorkspaceServers.updateLog(log,viewport,"line one\nline two");assertSame(log,viewport.getChildAt(0));assertEquals("line one\nline two",log.getText().toString());
    }

    @Test public void uploadPickerCapturesDestinationWithoutLaunchingAndroidPickerOrTerminal()throws Exception{
        TermuxActivity a=activity();WorkspaceServerUploads upload=new WorkspaceServerUploads(a,()->{});ServerProfiles.Profile p=profile();try{
            upload.pick(p,"/srv/project");assertFalse(upload.active());assertNull(org.robolectric.Shadows.shadowOf(a).getNextStartedActivityForResult());
            android.app.AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();assertNotNull(dialog);assertTrue(dialog.getListView().getAdapter().getCount()>=2);
            p.host="changed.invalid";assertEquals("example.invalid",((ServerProfiles.Profile)get(upload,"profile")).host);assertEquals("/srv/project",get(upload,"directory"));dialog.dismiss();
        }finally{upload.close();}
    }
    @Test public void fileMenuOffersViewerBeforeOtherActionsWithoutConnecting()throws Exception{
        LinearLayout root=new LinearLayout(activity());WorkspaceServers s=serverScreen(root);try{Method menu=WorkspaceServers.class.getDeclaredMethod("fileMenu",org.json.JSONObject.class);menu.setAccessible(true);menu.invoke(s,new org.json.JSONObject().put("name","README.md").put("path","/srv/README.md"));android.app.AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();assertEquals("Посмотреть",dialog.getListView().getAdapter().getItem(0));assertTrue(((Map<?,?>)get(s,"connections")).isEmpty());}finally{s.close();}
    }

    @Test public void staleInlineResultReleasesOnlyItsOwnedResource()throws Exception{
        WorkspaceServers s=new WorkspaceServers(activity(),(n,c)->fail());try{java.util.concurrent.atomic.AtomicBoolean released=new java.util.concurrent.atomic.AtomicBoolean();java.util.concurrent.CountDownLatch started=new java.util.concurrent.CountDownLatch(1),finish=new java.util.concurrent.CountDownLatch(1);Method run=WorkspaceServers.class.getDeclaredMethod("runInline",WorkspaceServers.Work.class,WorkspaceServers.Done.class,java.util.function.Consumer.class,Runnable.class);run.setAccessible(true);
            run.invoke(s,(WorkspaceServers.Work)()->{started.countDown();finish.await(5,java.util.concurrent.TimeUnit.SECONDS);return new org.json.JSONObject();},(WorkspaceServers.Done)value->fail("stale result"),(java.util.function.Consumer<String>)code->fail(code),(Runnable)()->released.set(true));assertTrue(started.await(5,java.util.concurrent.TimeUnit.SECONDS));set(s,"generation",1);finish.countDown();((java.util.concurrent.ExecutorService)get(s,"worker")).submit(()->{}).get(5,java.util.concurrent.TimeUnit.SECONDS);org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertTrue(released.get());assertFalse((Boolean)get(s,"busy"));assertTrue(((Map<?,?>)get(s,"connections")).isEmpty());
        }finally{s.close();}
    }

    private static class QueuedWorker extends java.util.concurrent.AbstractExecutorService {
        boolean stopped;final java.util.List<Runnable> queued=new java.util.ArrayList<>();public void shutdown(){stopped=true;}public java.util.List<Runnable> shutdownNow(){stopped=true;return queued;}public boolean isShutdown(){return stopped;}public boolean isTerminated(){return stopped;}public boolean awaitTermination(long t,java.util.concurrent.TimeUnit u){return stopped;}public void execute(Runnable r){queued.add(r);}
    }
    @Test public void sectionClicksKeepMonitoringNavigationAndScrollInstances()throws Exception{
        LinearLayout root=new LinearLayout(activity());WorkspaceServers s=serverScreen(root);try{
            ((java.util.concurrent.ExecutorService)get(s,"worker")).shutdownNow();QueuedWorker queue=new QueuedWorker();set(s,"worker",queue);View scroll=root.getChildAt(0),graph=(View)get(s,"metricCards"),path=description(root,"Путь…");View projects=textView(root,"Проекты"),tasks=textView(root,"Задачи"),files=textView(root,"Файлы"),services=textView(root,"Службы");Object section=get(s,"sectionBody");
            for(View tab:new View[]{projects,tasks,files,services}){set(s,"busy",false);tab.performClick();assertSame(scroll,root.getChildAt(0));assertSame(graph,get(s,"metricCards"));assertSame(section,get(s,"sectionBody"));assertSame(projects,textView(root,"Проекты"));assertSame(files,textView(root,"Файлы"));assertTrue(tab.isSelected());for(View other:new View[]{projects,tasks,files,services})if(other!=tab)assertFalse(other.isSelected());}
            assertEquals(2,queue.queued.size());assertNotNull(path);
        }finally{s.close();}
    }
    @Test public void serverTerminalUsesCenteredVectorAndPreservesLaunchPath()throws Exception{
        LinearLayout root=new LinearLayout(activity());WorkspaceServers s=serverScreen(root);try{View terminal=description(root,"Терминал");assertTrue(terminal instanceof WorkspaceIconButton);assertEquals("",((TextView)terminal).getText().toString());assertEquals(terminal.getLayoutParams().height,terminal.getLayoutParams().width);assertTrue(((LinearLayout.LayoutParams)terminal.getLayoutParams()).getMarginStart()>0);assertNull(textView(root,"Терминал"));assertTrue(terminal.hasOnClickListeners());assertTrue(((Map<?,?>)get(s,"connections")).isEmpty());}finally{s.close();}
    }

    private void checkStatus(WorkspaceServers s,WorkspaceServers.Work probe,android.app.AlertDialog dialog,TextView status,TextView install,TextView remove,TextView retry)throws Exception{
        for(TextView v:new TextView[]{install,remove,retry})v.setVisibility(View.GONE);
        Method check=WorkspaceServers.class.getDeclaredMethod("checkAgentSettings",WorkspaceServers.Work.class,ServerProfiles.Profile.class,android.app.AlertDialog.class,TextView.class,TextView.class,TextView.class,TextView.class);check.setAccessible(true);check.invoke(s,probe,get(s,"selected"),dialog,status,install,remove,retry);
        ((java.util.concurrent.ExecutorService)get(s,"worker")).submit(()->{}).get(5,java.util.concurrent.TimeUnit.SECONDS);org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    }
    @Test public void verifiedAgentPresenceShowsOnlyTheCorrectActionAndNoAutomaticInstall()throws Exception{
        for(boolean present:new boolean[]{true,false}){TermuxActivity a=activity();LinearLayout root=new LinearLayout(a);WorkspaceServers s=serverScreen(root);android.app.AlertDialog dialog=WorkspaceDialogs.builder(a).setPositiveButton("OK",null).create();dialog.show();try{TextView status=new TextView(a),install=new TextView(a),remove=new TextView(a),retry=new TextView(a);View page=root.getChildAt(0);checkStatus(s,()->new org.json.JSONObject().put("agent",present).put("supported",true),dialog,status,install,remove,retry);assertEquals(present?View.GONE:View.VISIBLE,install.getVisibility());assertEquals(present?View.VISIBLE:View.GONE,remove.getVisibility());assertEquals(View.GONE,retry.getVisibility());assertEquals(present?"Агент установлен":"Агент не установлен",status.getText().toString());assertSame(page,root.getChildAt(0));assertTrue(((Map<?,?>)get(s,"connections")).isEmpty());Method known=WorkspaceServers.class.getDeclaredMethod("knownAgent",ServerProfiles.Profile.class);known.setAccessible(true);assertEquals(present,known.invoke(s,get(s,"selected")));}finally{dialog.dismiss();s.close();}}
    }
    @Test public void networkFailureDoesNotTurnInstalledAgentIntoMissingOrOfferInstallation()throws Exception{
        TermuxActivity a=activity();WorkspaceServers s=serverScreen(new LinearLayout(a));android.app.AlertDialog dialog=WorkspaceDialogs.builder(a).create();dialog.show();try{TextView status=new TextView(a),install=new TextView(a),remove=new TextView(a),retry=new TextView(a);checkStatus(s,()->{throw new IOException("SSH_FAILED");},dialog,status,install,remove,retry);assertTrue(status.getText().toString().contains("не означает"));assertEquals(View.GONE,install.getVisibility());assertEquals(View.GONE,remove.getVisibility());assertEquals(View.VISIBLE,retry.getVisibility());assertEquals(1,((Map<?,?>)get(s,"snapshots")).size());assertTrue(((Map<?,?>)get(s,"connections")).isEmpty());}finally{dialog.dismiss();s.close();}
    }
    @Test public void unsupportedPythonDoesNotOfferAnInstallationThatCannotWork()throws Exception{
        TermuxActivity a=activity();WorkspaceServers s=serverScreen(new LinearLayout(a));android.app.AlertDialog dialog=WorkspaceDialogs.builder(a).create();dialog.show();try{TextView status=new TextView(a),install=new TextView(a),remove=new TextView(a),retry=new TextView(a);checkStatus(s,()->new org.json.JSONObject().put("agent",false).put("supported",false),dialog,status,install,remove,retry);assertTrue(status.getText().toString().contains("Python 3.8+"));assertEquals(View.GONE,install.getVisibility());assertEquals(View.GONE,remove.getVisibility());}finally{dialog.dismiss();s.close();}
    }
    @Test public void agentInstallGuardRejectsDuplicateBeforeCreatingConnection()throws Exception{
        WorkspaceServers s=serverScreen(new LinearLayout(activity()));try{Method install=WorkspaceServers.class.getDeclaredMethod("install");install.setAccessible(true);install.invoke(s);String message=((TextView)ShadowAlertDialog.getLatestAlertDialog().findViewById(android.R.id.message)).getText().toString();assertTrue(message.contains("уже установлен"));assertTrue(((Map<?,?>)get(s,"connections")).isEmpty());assertFalse((Boolean)get(s,"busy"));}finally{s.close();}
    }
    @Test public void infoAndOverflowHaveSeparateSquareTouchTargetsAndGap()throws Exception{
        LinearLayout root=new LinearLayout(activity());WorkspaceServers s=serverScreen(root);try{View info=description(root,"Об агенте сервера"),menu=description(root,"Настройки сервера");assertNotNull(info);assertNotNull(menu);assertEquals(menu.getLayoutParams().width,menu.getLayoutParams().height);assertEquals(WorkspaceDialogs.dp(root.getContext(),8),((LinearLayout.LayoutParams)menu.getLayoutParams()).getMarginStart());}finally{s.close();}
    }

    private android.widget.Spinner spinner(View v){if(v instanceof android.widget.Spinner)return (android.widget.Spinner)v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){android.widget.Spinner found=spinner(((ViewGroup)v).getChildAt(i));if(found!=null)return found;}return null;}
    @Test public void newTaskOffersImmediateOrScheduledWithoutStartingRemoteWork()throws Exception{
        WorkspaceServers s=serverScreen(new LinearLayout(activity()));try{Method task=WorkspaceServers.class.getDeclaredMethod("startTask");task.setAccessible(true);task.invoke(s);android.app.AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();android.widget.Spinner mode=spinner(dialog.getWindow().getDecorView());assertNotNull(mode);assertEquals(2,mode.getCount());assertEquals("Сейчас",mode.getItemAtPosition(0));assertEquals("По расписанию",mode.getItemAtPosition(1));assertEquals(0,mode.getSelectedItemPosition());assertTrue(((Map<?,?>)get(s,"connections")).isEmpty());}finally{s.close();}
    }


    private View exactText(View view,String text){if(view instanceof TextView&&((TextView)view).getText().toString().equals(text))return view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View found=exactText(((ViewGroup)view).getChildAt(i),text);if(found!=null)return found;}return null;}
    @Test public void favoritesAndPinnedCommandsPersistPerServerWithoutExecution()throws Exception{
        Context context=RuntimeEnvironment.application;File file=new File(context.getFilesDir(),"workspace-servers-v1.json");file.delete();ServerProfiles store=new ServerProfiles(context);ServerProfiles.Profile one=profile(),two=profile();one.favorites.add("/srv/проект");one.favoriteFiles.add("/srv/README.md");one.actions.add(new ServerProfiles.Action("Run","/srv/проект","printf '%s' 'literal; $(not-executed)'"));store.profiles.add(one);store.profiles.add(two);store.save();ServerProfiles loaded=new ServerProfiles(context);assertTrue(loaded.writable);assertEquals(one.favorites,loaded.profiles.get(0).favorites);assertEquals(one.favoriteFiles,loaded.profiles.get(0).favoriteFiles);assertEquals(one.actions.get(0).command,loaded.profiles.get(0).actions.get(0).command);assertTrue(loaded.profiles.get(1).favorites.isEmpty());assertTrue(loaded.profiles.get(1).actions.isEmpty());file.delete();
    }
    @Test public void oldProfileWithoutNewFieldsRemainsReadable()throws Exception{
        Context context=RuntimeEnvironment.application;File file=new File(context.getFilesDir(),"workspace-servers-v1.json");org.json.JSONObject old=profile().json();old.remove("favorites");old.remove("actions");try(FileOutputStream out=new FileOutputStream(file)){out.write(new org.json.JSONObject().put("version",1).put("servers",new org.json.JSONArray().put(old)).toString().getBytes("UTF-8"));}ServerProfiles loaded=new ServerProfiles(context);assertTrue(loaded.writable);assertEquals(1,loaded.profiles.size());assertTrue(loaded.profiles.get(0).favorites.isEmpty());file.delete();
    }
    @Test public void favoritesToolbarAndDialogDoNotIssueRemoteCommands()throws Exception{
        LinearLayout root=new LinearLayout(activity());WorkspaceServers s=serverScreen(root);try{assertNotNull(description(root,"Избранные папки и файлы"));assertNotNull(description(root,"Поиск по именам"));description(root,"Избранные папки и файлы").performClick();assertTrue(contains(ShadowAlertDialog.getLatestAlertDialog().getWindow().getDecorView(),"Добавить текущую папку"));assertTrue(((Map<?,?>)get(s,"connections")).isEmpty());}finally{s.close();}
    }
    @Test public void searchDialogExplainsScopeAndDoesNotStartUntilSearch()throws Exception{
        LinearLayout root=new LinearLayout(activity());WorkspaceServers s=serverScreen(root);try{description(root,"Поиск по именам").performClick();android.app.AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();assertTrue(contains(d.getWindow().getDecorView(),"подпапках"));assertTrue(contains(d.getWindow().getDecorView(),"Содержимое файлов не читается"));assertTrue(((Map<?,?>)get(s,"connections")).isEmpty());d.getButton(-2).performClick();}finally{s.close();}
    }
    @Test public void pinnedCommandClickRequiresReviewAndDoesNotStartProcess()throws Exception{
        WorkspaceServers s=serverScreen(new LinearLayout(activity()));try{ServerProfiles.Profile p=(ServerProfiles.Profile)get(s,"selected");p.actions.add(new ServerProfiles.Action("Deploy","/srv/project","printf deploy"));Method method=WorkspaceServers.class.getDeclaredMethod("pinnedActions");method.setAccessible(true);method.invoke(s);android.app.AlertDialog list=ShadowAlertDialog.getLatestAlertDialog();exactText(list.getWindow().getDecorView(),"Deploy\n/srv/project").performClick();android.app.AlertDialog confirmation=ShadowAlertDialog.getLatestAlertDialog();String text=((TextView)confirmation.findViewById(android.R.id.message)).getText().toString();assertTrue(text.contains("printf deploy"));assertTrue(text.contains("tester@example.invalid"));assertTrue(((Map<?,?>)get(s,"connections")).isEmpty());}finally{s.close();}
    }
    @Test public void diagnosticsDoesNotClaimAuthenticationWhenOnlyPortOrKeyKnown()throws Exception{
        WorkspaceServers s=new WorkspaceServers(activity(),(n,c)->{});try{WorkspaceLanguage.select(activity(),"ru");assertTrue(s.diagnosticReport(new org.json.JSONObject().put("network",false)).contains("Порт недоступен"));assertTrue(s.diagnosticReport(new org.json.JSONObject().put("network",true).put("trusted",false)).contains("не проверялась"));String ambiguous=s.diagnosticReport(new org.json.JSONObject().put("network",true).put("trusted",true).put("error","SSH_FAILED"));assertTrue(ambiguous.contains("не позволяет точно отличить"));assertFalse(ambiguous.contains("авторизация: успешно"));}finally{s.close();}
    }
    @Test public void transferProgressHandlesUnknownSizesAndEstimatesWithoutDividingByZero(){
        assertFalse(ServerTransferProgress.describe(0,-1,0,true).contains("осталось"));assertFalse(ServerTransferProgress.describe(100,-1,1000,true).contains("осталось"));assertTrue(ServerTransferProgress.describe(1000,2000,1000,true).contains("осталось ≈ 1 с"));assertTrue(ServerTransferProgress.describe(1000,2000,1000,false).contains("remaining ≈ 1 s"));assertFalse(ServerTransferProgress.describe(2000,2000,1000,true).contains("осталось"));assertFalse(WorkspaceServers.finishedTask("running"));assertFalse(WorkspaceServers.finishedTask("queued"));assertFalse(WorkspaceServers.finishedTask("unmanaged"));assertTrue(WorkspaceServers.finishedTask("done"));assertTrue(WorkspaceServers.finishedTask("failed"));assertTrue(WorkspaceServers.finishedTask("cancelled"));
    }

    @Test public void fileFavoritesAreAddedThroughFileMenuAndRemovalDoesNotRunRemoteCommands()throws Exception{
        WorkspaceServers s=serverScreen(new LinearLayout(activity()));try{ServerProfiles.Profile p=(ServerProfiles.Profile)get(s,"selected");s.store.profiles.clear();s.store.profiles.add(p);org.json.JSONObject entry=new org.json.JSONObject().put("path","/srv/README.md").put("name","README.md").put("size",10);Method method=WorkspaceServers.class.getDeclaredMethod("fileMenu",org.json.JSONObject.class);method.setAccessible(true);method.invoke(s,entry);android.app.AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();assertEquals("В избранное",dialog.getListView().getItemAtPosition(7));dialog.getListView().performItemClick(null,7,7);assertTrue(p.favoriteFiles.contains("/srv/README.md"));method.invoke(s,entry);dialog=ShadowAlertDialog.getLatestAlertDialog();assertEquals("Убрать из избранного",dialog.getListView().getItemAtPosition(7));dialog.getListView().performItemClick(null,7,7);assertTrue(p.favoriteFiles.isEmpty());}finally{s.close();new File(RuntimeEnvironment.application.getFilesDir(),"workspace-servers-v1.json").delete();}
    }

    @Test public void staleMonitorReplyCannotOverwriteManualStateOrDifferentProfile()throws Exception{
        WorkspaceServers s=serverScreen(new LinearLayout(activity()));try{ServerProfiles.Profile p=(ServerProfiles.Profile)get(s,"selected");Map<String,org.json.JSONObject> snapshots=(Map<String,org.json.JSONObject>)get(s,"snapshots");org.json.JSONObject original=snapshots.get(p.id),updated=snapshot(99);int generation=(int)get(s,"generation");s.store.profiles.add(p);((Set<String>)get(s,"connected")).add(p.id);set(s,"manualEpoch",4);s.applyMonitor(p,generation,3,updated,null);assertSame(original,snapshots.get(p.id));s.applyMonitor(p,generation-1,4,updated,null);assertSame(original,snapshots.get(p.id));s.applyMonitor(profile(),generation,4,updated,null);assertSame(original,snapshots.get(p.id));s.applyMonitor(p,generation,4,updated,null);assertSame(updated,snapshots.get(p.id));}finally{s.close();}
    }

    @Test public void diagnosticHeaderButtonIsWideAndBeforeInfoWithoutRemoteWork()throws Exception{LinearLayout root=new LinearLayout(activity());WorkspaceServers server=serverScreen(root);try{View diagnostics=description(root,"Диагностика сервера"),info=description(root,"Об агенте сервера");assertNotNull(diagnostics);assertSame(info.getParent(),diagnostics.getParent());ViewGroup row=(ViewGroup)info.getParent();assertTrue(row.indexOfChild(diagnostics)<row.indexOfChild(info));assertEquals(WorkspaceDialogs.dp(root.getContext(),106),diagnostics.getLayoutParams().width);diagnostics.performClick();assertTrue(((Map<?,?>)get(server,"connections")).isEmpty());}finally{server.close();}}

    @Test public void agentStatusDoesNotConfuseCachedConnectionWithLiveServerState()throws Exception{WorkspaceServers server=new WorkspaceServers(activity(),(name,command)->fail());try{WorkspaceLanguage.select(activity(),"ru");ServerProfiles.Profile profile=profile();assertTrue(server.agentLabel(profile).contains("не проверено"));((java.util.Set<String>)get(server,"connected")).add(profile.id);assertTrue(server.agentLabel(profile).contains("не проверено"));((Map<String,Boolean>)get(server,"agentPresence")).put(profile.id,true);assertTrue(server.agentLabel(profile).contains("по последней проверке"));assertFalse(server.agentLabel(profile).contains("Включён"));}finally{server.close();}}
    @SuppressWarnings("unchecked") @Test public void pollingPreferenceAndListRepliesAreIndependentOfAgentState()throws Exception{
        TermuxActivity a=activity();WorkspaceServers server=new WorkspaceServers(a,(n,c)->fail());try{
            ServerProfiles.Profile p=profile();server.store.profiles.add(p);assertFalse(p.backgroundPolling);
            ((Set<String>)get(server,"connected")).add(p.id);assertTrue(server.pollingEnabled(p));
            p.backgroundPolling=true;assertTrue(server.pollingEnabled(p));assertEquals(30000,server.pollingInterval(p));
            server.store.save();assertTrue(new ServerProfiles(a).profiles.get(0).backgroundPolling);
            set(server,"selected",p);set(server,"visible",true);assertEquals(10000,server.pollingInterval(p));
            p.backgroundPolling=false;assertTrue(server.pollingEnabled(p));set(server,"visible",false);assertTrue(server.pollingEnabled(p));
            p.backgroundPolling=true;set(server,"selected",null);org.json.JSONObject result=snapshot(42);
            server.applyMonitor(p,(int)get(server,"generation"),(int)get(server,"manualEpoch"),result,null);
            assertSame(result,((Map<String,org.json.JSONObject>)get(server,"snapshots")).get(p.id));
            ((Set<String>)get(server,"connected")).remove(p.id);server.applyMonitor(p,(int)get(server,"generation"),(int)get(server,"manualEpoch"),snapshot(43),null);
            assertSame(result,((Map<String,org.json.JSONObject>)get(server,"snapshots")).get(p.id));assertFalse(server.pollingEnabled(p));
        }finally{server.close();}
    }

    @SuppressWarnings("unchecked") @Test public void disconnectClosesOnlyOwnedProfileTransportsAndRejectsLatePoll()throws Exception{
        TermuxActivity a=activity();WorkspaceLanguage.select(a,"ru");WorkspaceServers server=new WorkspaceServers(a,(n,c)->fail());ServerProfiles.Profile p=profile(),other=profile();server.store.profiles.add(p);server.store.profiles.add(other);
        ServerConnection mine=new ServerConnection(a,p),parallel=new ServerConnection(a,p),different=new ServerConnection(a,other),differentActivity=new ServerConnection(activity(),p);FakeProcess own=new FakeProcess(),sftp=new FakeProcess(),safe=new FakeProcess(),safeActivity=new FakeProcess(),unmanaged=new FakeProcess();
        try{
            set(mine,"process",own);set(parallel,"process",sftp);set(different,"process",safe);set(differentActivity,"process",safeActivity);((Map<String,ServerConnection>)get(server,"connections")).put(p.id,mine);((Set<String>)get(server,"connected")).add(p.id);((Set<String>)get(server,"connected")).add(other.id);set(server,"selected",p);set(server,"visible",true);
            LinearLayout root=new LinearLayout(a);server.attach(root);assertEquals("Отключиться",((android.widget.TextView)root.findViewWithTag("server-connect-"+p.id)).getText().toString());int generation=(int)get(server,"generation"),epoch=(int)get(server,"manualEpoch");root.findViewWithTag("server-connect-"+p.id).performClick();assertEquals("Подключиться",((android.widget.TextView)root.findViewWithTag("server-connect-"+p.id)).getText().toString());
            assertFalse(own.running);assertFalse(sftp.running);assertTrue(safe.running);assertTrue(safeActivity.running);assertTrue(unmanaged.running);assertFalse(server.pollingEnabled(p));assertTrue(server.pollingEnabled(other));assertTrue(ServerConnection.disconnected(a,p.id));assertEquals("Отключён",server.pollingLabel(p));
            server.applyMonitor(p,generation,epoch,snapshot(42),null);assertFalse(((Map<String,org.json.JSONObject>)get(server,"snapshots")).containsKey(p.id));assertFalse(((Set<String>)get(server,"connected")).contains(p.id));
            ServerConnection blocked=new ServerConnection(a,p);try{assertTrue((boolean)get(blocked,"closed"));}finally{blocked.close();}
            ServerConnection.allow(a,p.id);ServerConnection fresh=new ServerConnection(a,p);try{assertFalse((boolean)get(fresh,"closed"));assertTrue((boolean)get(mine,"closed"));}finally{fresh.close();}
        }finally{mine.close();parallel.close();different.close();differentActivity.close();server.close();}
    }

}
