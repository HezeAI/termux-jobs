package com.termux.app.workspace;

import android.app.AlertDialog;
import android.content.*;
import android.view.*;
import android.widget.*;
import com.termux.R;
import com.termux.app.TermuxActivity;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class WorkspaceErrorLibraryTest {
    private WorkspaceErrorLibrary load()throws Exception{return WorkspaceErrorLibrary.load(RuntimeEnvironment.application);}
    private TermuxActivity activity(boolean ru){TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);WorkspaceLanguage.select(a,ru?"ru":"en");return a;}
    private View text(View view,String text){if(view instanceof TextView&&text.equals(((TextView)view).getText().toString()))return view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View found=text(((ViewGroup)view).getChildAt(i),text);if(found!=null)return found;}return null;}
    private EditText search(View view){if(view instanceof EditText)return(EditText)view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){EditText e=search(((ViewGroup)view).getChildAt(i));if(e!=null)return e;}return null;}
    @Test public void catalogContainsBilingualSourcedEntriesForEveryEnvironment()throws Exception{
        WorkspaceErrorLibrary lib=load();assertEquals(129,lib.entries.size());Set<String> ids=new HashSet<>();for(WorkspaceErrorLibrary.Entry e:lib.entries){assertTrue(ids.add(e.id));assertFalse(e.text("solution",true).isEmpty());assertFalse(e.text("solution",false).isEmpty());assertTrue(e.data.getJSONArray("sources").length()>0);assertFalse(e.command.contains("sudo "));assertFalse(e.command.contains("rm "));}
        for(String env:WorkspaceErrorLibrary.ENV)assertFalse(env,lib.search("",env,"all","all").isEmpty());
    }
    @Test public void xdaCandidatesAreSearchableAndNeverAutoRun()throws Exception{
        WorkspaceErrorLibrary lib=load();
        for(String key:new String[]{"NOSPLIT","netlink","adb pair"}) {
            java.util.List<WorkspaceErrorLibrary.Entry> found=lib.search(key,"termux","all","all");
            assertFalse(key,found.isEmpty());
            for(WorkspaceErrorLibrary.Entry e:found) if(e.id.equals("termux-apt-nosplit")||e.id.equals("termux-netlink-denied")||e.id.equals("termux-adb-paired-offline"))assertEquals("",e.command);
        }
    }
    @Test public void searchUsesBothLanguagesAllTokensAndCombinedFilters()throws Exception{
        WorkspaceErrorLibrary lib=load();assertFalse(lib.search("JAVA_HOME","ubuntu","build","all").isEmpty());assertFalse(lib.search("общей памяти","termux","files","all").isEmpty());assertTrue(lib.search("JAVA_HOME","ubuntu","git","all").isEmpty());assertTrue(lib.search("no-such-error-12345","all","all","all").isEmpty());
        for(WorkspaceErrorLibrary.Entry e:lib.search("","alpine","packages","all"))assertTrue(e.id.startsWith("alpine-")||e.id.startsWith("jobs-"));
    }
    @Test public void prootAdviceIsExcludedFromNativeAndSshFilters()throws Exception{
        WorkspaceErrorLibrary lib=load();assertFalse(lib.search("systemd","debian","environment","proot").isEmpty());assertTrue(lib.search("systemd","debian","environment","native").isEmpty());assertTrue(lib.search("systemd","debian","environment","ssh").isEmpty());
    }
    @Test public void communityRecipesRemainSourcedAndRespectExecutionContext()throws Exception{
        WorkspaceErrorLibrary lib=load();
        assertTrue(lib.search("Installing pip is forbidden","ubuntu","all","all").isEmpty());
        assertFalse(lib.search("Installing pip is forbidden","termux","all","all").isEmpty());
        assertFalse(lib.search("Cannot open display","ubuntu","all","proot").isEmpty());
        assertTrue(lib.search("Cannot open display","ubuntu","all","native").isEmpty());
        assertFalse(lib.search("SSH_AUTH_SOCK","debian","all","ssh").isEmpty());
        assertTrue(lib.search("SSH_AUTH_SOCK","debian","all","proot").isEmpty());
        int community=0;for(WorkspaceErrorLibrary.Entry entry:lib.entries){boolean official=false,discussion=false;JSONArray sources=entry.data.getJSONArray("sources");for(int i=0;i<sources.length();i++){String kind=sources.getJSONObject(i).getString("kind");official|="official".equals(kind)||"project".equals(kind);discussion|="community".equals(kind);}assertTrue(entry.id,official);if(discussion)community++;}
        assertEquals(29,community);
    }
    @Test public void projectCasesAreDistinctFromOfficialAdviceAndUnresolvedIssueStaysOpen()throws Exception{
        WorkspaceErrorLibrary lib=load();int count=0;for(WorkspaceErrorLibrary.Entry e:lib.entries)if(e.data.has("projectStatus")){count++;assertFalse(e.text("projectEvidence",true).isEmpty());assertFalse(e.text("projectEvidence",false).isEmpty());}
        assertEquals(40,count);WorkspaceErrorLibrary.Entry flicker=lib.search("OC-001","termux","environment","all").get(0);assertEquals("open",flicker.data.getString("projectStatus"));assertTrue(flicker.text("solution",false).contains("No confirmed universal fix"));
        assertEquals(1,lib.entries.stream().filter(e->e.id.equals("termux-storage")).count());assertTrue(lib.search("grant","termux","files","all").stream().anyMatch(e->e.id.equals("termux-storage")));
    }
    @Test public void projectSourceWithoutEvidenceOrValidStatusFailsClosed()throws Exception{
        JSONObject card=new JSONObject(load().entries.stream().filter(e->e.id.startsWith("jobs-")).findFirst().get().data.toString());card.remove("projectEvidence");JSONObject root=new JSONObject().put("schema",1).put("version","test").put("entries",new JSONArray().put(card));try{parse(root);fail();}catch(JSONException expected){}
        card.put("projectEvidence",new JSONObject().put("ru","test").put("en","test")).put("projectStatus","certain");try{parse(root);fail();}catch(JSONException expected){}
    }
    @Test public void reviewedForumCasesHavePrimarySourcesAndDoNotDuplicateExtendedCards()throws Exception{
        WorkspaceErrorLibrary lib=load();
        for(String id:new String[]{"termux-boot-start","termux-services-start","termux-incoming-ssh","ssh-reexec-legacy","shell-source-not-found","termux-rish-package","termux-rish-writable-dex","termux-rish-environment","termux-x11-black-screen","termux-x11-swapped-colors","archive-zip-filename-encoding","samba-supervisor-mode"}){
            WorkspaceErrorLibrary.Entry e=lib.entries.stream().filter(x->x.id.equals(id)).findFirst().get();assertEquals("",e.command);boolean primary=false;for(int i=0;i<e.data.getJSONArray("sources").length();i++)primary|=e.data.getJSONArray("sources").getJSONObject(i).getString("kind").equals("official");assertTrue(id,primary);
        }
        assertFalse(lib.search("writable dex","termux","files","all").isEmpty());
        assertFalse(lib.search("hdrcharset","ubuntu","files","all").isEmpty());
        WorkspaceErrorLibrary.Entry backup=lib.entries.stream().filter(x->x.id.equals("jobs-backup-source-unreadable")).findFirst().get();assertTrue(backup.text("caution",false).contains("ignore-failed-read"));assertEquals(1,lib.entries.stream().filter(x->x.id.equals("jobs-backup-source-unreadable")).count());
    }
    @Test public void libraryTitleHasLocalizedAiCreditOnItsRight(){
        for(boolean ru:new boolean[]{true,false}){
            TermuxActivity a=activity(ru);WorkspaceErrorLibraryUi.show(a,c->fail());AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();View root=dialog.getWindow().getDecorView();TextView title=(TextView)text(root,ru?"Ошибки и решения":"Errors and solutions"),credit=(TextView)text(root,ru?"Собрано с помощью AI!":"Compiled with AI!");assertNotNull(title);assertNotNull(credit);assertSame(title.getParent(),credit.getParent());ViewGroup row=(ViewGroup)title.getParent();assertTrue(row.indexOfChild(title)<row.indexOfChild(credit));assertTrue(((LinearLayout.LayoutParams)title.getLayoutParams()).weight>0);assertTrue(credit.getMaxWidth()>0);dialog.dismiss();
        }
    }
    @Test public void sourcesRejectCommandSchemesCredentialsAndMalformedUrls(){assertFalse(WorkspaceErrorLibrary.safeUrl("javascript:alert(1)"));assertFalse(WorkspaceErrorLibrary.safeUrl("file:///root/secret"));assertFalse(WorkspaceErrorLibrary.safeUrl("https://user:pass@example.com/path"));assertTrue(WorkspaceErrorLibrary.safeUrl("https://github.com/termux/proot-distro#limitations"));}
    private WorkspaceErrorLibrary parse(JSONObject root)throws Exception{return WorkspaceErrorLibrary.parse(new ByteArrayInputStream(root.toString().getBytes(StandardCharsets.UTF_8)));}
    @Test public void invalidSchemaDuplicateIdsAndMissingTranslationsFailClosed()throws Exception{
        WorkspaceErrorLibrary lib=load();JSONObject root=new JSONObject().put("schema",2).put("version","test").put("entries",new JSONArray());try{parse(root);fail();}catch(JSONException expected){}
        root.put("schema",1).put("entries",new JSONArray().put(lib.entries.get(0).data).put(lib.entries.get(0).data));try{parse(root);fail();}catch(JSONException expected){}
        JSONObject broken=new JSONObject(lib.entries.get(0).data.toString());broken.getJSONObject("title").remove("en");root.put("entries",new JSONArray().put(broken));try{parse(root);fail();}catch(JSONException expected){}
    }
    @Test public void oversizedCatalogIsRejected()throws Exception{try{WorkspaceErrorLibrary.parse(new ByteArrayInputStream(new byte[512*1024+1]));fail();}catch(IOException expected){}}
    @Test public void unresolvedProjectCaseUsesDiagnosticHeadingAndVisibleStatus()throws Exception{
        TermuxActivity a=activity(false);WorkspaceErrorLibraryUi.show(a,cmd->fail());AlertDialog list=ShadowAlertDialog.getLatestAlertDialog();View root=list.getWindow().getDecorView();search(root).setText("OC-001");View title=text(root,"OC-001: OpenCode flicker and screen replay after a second");assertNotNull(title);((View)title.getParent()).performClick();AlertDialog detail=ShadowAlertDialog.getLatestAlertDialog();View body=detail.getWindow().getDecorView();assertNotNull(text(body,"Diagnostics and temporary measures"));assertNull(text(body,"Possible solution"));assertNotNull(text(body,"Termux Job’s development experience"));detail.dismiss();list.dismiss();
    }
    @Test public void libraryUiSearchAndCopyNeverInvokeSessionAction()throws Exception{
        TermuxActivity a=activity(true);AtomicInteger calls=new AtomicInteger();WorkspaceErrorLibraryUi.show(a,cmd->calls.incrementAndGet());AlertDialog list=ShadowAlertDialog.getLatestAlertDialog();View root=list.getWindow().getDecorView();search(root).setText("JAVA_HOME");View entry=text(root,"JAVA_HOME is not set / invalid directory");assertNotNull(entry);((View)entry.getParent()).performClick();AlertDialog details=ShadowAlertDialog.getLatestAlertDialog();View detail=details.getWindow().getDecorView();assertNotNull(text(detail,"Скопировать команду"));text(detail,"Скопировать команду").performClick();assertEquals(0,calls.get());assertTrue(((ClipboardManager)a.getSystemService(Context.CLIPBOARD_SERVICE)).getPrimaryClip().getItemAt(0).getText().toString().contains("java -version"));details.dismiss();list.dismiss();
    }
    @Test public void pasteRequiresConfirmationAndOnlyReturnsText()throws Exception{
        TermuxActivity a=activity(false);List<String> values=new ArrayList<>();WorkspaceErrorLibraryUi.show(a,values::add);AlertDialog list=ShadowAlertDialog.getLatestAlertDialog();View root=list.getWindow().getDecorView();search(root).setText("JAVA_HOME");View entry=text(root,"JAVA_HOME is not set / invalid directory");((View)entry.getParent()).performClick();AlertDialog detail=ShadowAlertDialog.getLatestAlertDialog();text(detail.getWindow().getDecorView(),"Insert into session…").performClick();AlertDialog confirmation=ShadowAlertDialog.getLatestAlertDialog();assertTrue(values.isEmpty());confirmation.getButton(-1).performClick();org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals(1,values.size());assertFalse(values.get(0).endsWith("\n"));assertFalse(list.isShowing());assertFalse(detail.isShowing());
    }
}
