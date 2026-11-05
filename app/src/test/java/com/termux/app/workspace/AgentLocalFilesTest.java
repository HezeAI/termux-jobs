package com.termux.app.workspace;

import android.app.*;
import android.os.Looper;
import android.text.*;
import android.text.style.URLSpan;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import com.termux.R;
import com.termux.app.TermuxActivity;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class,shadows=AgentChatLinksTest.TextMetrics.class)
public class AgentLocalFilesTest {
    private WorkspaceStore.Session profile(int agent){WorkspaceStore.Session p=new WorkspaceStore.Session();p.id="origin-"+agent;p.codexSocketId="fixture-"+agent;p.environment=agent==0?"codex":"ubuntu";p.codexEnvironment="ubuntu-ai";p.claudeAgent=agent==1;p.geminiAgent=agent==2;p.aiAgent=agent==3?"opencode":"";return p;}
    private WebView web(View v){if(v instanceof WebView)return (WebView)v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){WebView w=web(((ViewGroup)v).getChildAt(i));if(w!=null)return w;}return null;}
    private TextView linked(View v){if(v instanceof TextView&&((TextView)v).getText() instanceof Spanned){Spanned s=(Spanned)((TextView)v).getText();if(s.getSpans(0,s.length(),URLSpan.class).length>0)return (TextView)v;}if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){TextView t=linked(((ViewGroup)v).getChildAt(i));if(t!=null)return t;}return null;}
    private WebView await(AlertDialog d)throws Exception{for(int i=0;i<150;i++){Shadows.shadowOf(Looper.getMainLooper()).idle();WebView w=web(d.getWindow().getDecorView());if(w!=null){layout(d.getWindow().getDecorView());Shadows.shadowOf(Looper.getMainLooper()).idle();return w;}Thread.sleep(10);}throw new AssertionError("Viewer did not open");}
    private File fixture(Activity a,WorkspaceStore.Session p,String ext,byte[] bytes)throws Exception{File dir=new File(ChatAttachments.root(a,p),"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");dir.mkdirs();File f=new File(dir,"original"+ext);try(OutputStream out=new FileOutputStream(f)){out.write(bytes);}return f;}
    private void layout(View v){v.measure(View.MeasureSpec.makeMeasureSpec(720,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.EXACTLY));v.layout(0,0,720,900);}
    @Test public void boundLocalSpansAndCardsUseOriginalProfileForEveryAgentAfterSwitch()throws Exception{
        for(int agent=0;agent<4;agent++)for(boolean card:new boolean[]{false,true}){
            TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);
            WorkspaceStore.Session p=profile(agent);AgentChatView view=new AgentChatView(a,r->{throw new AssertionError("must use common file opener");},()->{},()->{},()->{},()->{});ChatAttachmentUi ui=new ChatAttachmentUi(a,view);
            try{
                fixture(a,p,".html",("<!doctype html><script>document.title='source-"+agent+"'</script>").getBytes(StandardCharsets.UTF_8));ui.sync("origin",p,true);
                JSONObject attachment=new JSONObject().put("name","Макет с пробелами.html").put("type","binary").put("path",ChatAttachments.prefix(p)+"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa/original.html");
                JSONObject row=new JSONObject().put("key","file").put("kind",card?"attachment":"text");if(card)row.put("attachment",attachment);else row.put("body","[Макет]("+attachment.getString("path")+")");
                view.rows(Collections.singletonList(row),false);layout(view);
                Runnable click;if(card){java.lang.reflect.Field field=AgentChatView.class.getDeclaredField("views");field.setAccessible(true);View v=(View)((Map<?,?>)field.get(view)).get("file");click=v::performClick;}
                else {TextView text=linked(view);assertNotNull(text);Spanned spans=(Spanned)text.getText();URLSpan span=spans.getSpans(0,spans.length(),URLSpan.class)[0];assertTrue(span instanceof AgentMarkdown.LocalLink);click=()->span.onClick(text);}
                // Changing the selected source must not change an already-rendered link/card.
                WorkspaceStore.Session other=profile((agent+1)%4);other.prootAlias="unrelated";ui.sync("other",other,true);p.codexEnvironment="termux";
                click.run();AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();WebView w=await(d);assertTrue(w.getSettings().getJavaScriptEnabled());assertFalse(w.getSettings().getAllowFileAccess());assertTrue(d.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled());
                // Switching while the original document is open does not dismiss it.
                ui.sync("third",profile(0),true);assertTrue("Changing source must keep original viewer open",d.isShowing());d.dismiss();Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue("Dismiss releases WebView",Shadows.shadowOf(w).wasDestroyCalled());
            }finally{ui.close();a.finish();}
        }
    }
    @Test public void gifCardAndLinkShareAnimatedWebViewWithoutJavascript()throws Exception{
        byte[] gif=Base64.getDecoder().decode("R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7");
        for(int agent=0;agent<4;agent++){
            TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);WorkspaceStore.Session p=profile(agent);AgentChatView view=new AgentChatView(a,r->{},()->{},()->{},()->{},()->{});ChatAttachmentUi ui=new ChatAttachmentUi(a,view);
            try{fixture(a,p,".gif",gif);ui.sync("origin",p,true);JSONObject card=new JSONObject().put("name","animation.gif").put("type","image").put("path",ChatAttachments.prefix(p)+"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa/original.gif");
                for(Runnable open:new Runnable[]{ui.captureAttachment(card),ui.capturePath(card.getString("path"))}){open.run();AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();WebView w=await(d);assertFalse(w.getSettings().getJavaScriptEnabled());assertTrue(Shadows.shadowOf(w).getLastLoadedUrl().startsWith("https://preview-"));d.dismiss();}
            }finally{ui.close();a.finish();}
        }
    }
    @Test public void selectableLocalLinkTapUsesBoundActionNotDefaultAndroidUrlSpan()throws Exception{
        Activity a=Robolectric.buildActivity(Activity.class).setup().get();
        for(String agent:new String[]{"Codex","Claude","Gemini","OpenCode"}){
            java.util.concurrent.atomic.AtomicInteger opened=new java.util.concurrent.atomic.AtomicInteger();AgentChatView chat=new AgentChatView(a,r->{throw new AssertionError("tap reached row");},()->{},()->{},()->{},()->{});chat.assistantName(agent);chat.localFileAction=path->{assertEquals("/root/Макет%20с%20пробелами.html",path);return opened::incrementAndGet;};a.setContentView(chat);chat.rows(Collections.singletonList(new JSONObject().put("key","local").put("kind","text").put("body","[File](/root/Макет%20с%20пробелами.html)").put("more",true)),false);layout(chat);
            TextView text=linked(chat);assertNotNull(text);assertTrue(text.isTextSelectable());Spanned spans=(Spanned)text.getText();URLSpan span=spans.getSpans(0,spans.length(),URLSpan.class)[0];int offset=spans.getSpanStart(span)+1,line=text.getLayout().getLineForOffset(offset);float x=text.getTotalPaddingLeft()+text.getLayout().getPrimaryHorizontal(offset),y=text.getTotalPaddingTop()+(text.getLayout().getLineTop(line)+text.getLayout().getLineBottom(line))/2f;long now=android.os.SystemClock.uptimeMillis();
            for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}){MotionEvent e=MotionEvent.obtain(now,now+(action==MotionEvent.ACTION_UP?40:0),action,x,y,0);try{text.dispatchTouchEvent(e);}finally{e.recycle();}}
            assertEquals(agent,1,opened.get());assertNull(Shadows.shadowOf(a).getNextStartedActivity());
        }a.finish();
    }
    @Test public void capturedSessionKeepsSourceEnvironmentAndIdentity(){for(int i=0;i<4;i++){WorkspaceStore.Session p=profile(i),captured=ChatAttachmentUi.source(p);p.codexEnvironment="termux";p.id="changed";p.codexSocketId="changed";assertEquals("origin-"+i,captured.id);assertEquals("fixture-"+i,captured.codexSocketId);assertEquals("proot:ubuntu-ai",ProjectFiles.environment(captured));assertTrue(ProjectFiles.command(captured,"printf source").contains("ubuntu-ai"));}}
    @Test public void pathsAreDecodedOnceWithoutChangingPlusAndRejectControls(){
        assertEquals("/root/Макет с пробелами+1.html",ChatAttachmentUi.localPath("/root/%D0%9C%D0%B0%D0%BA%D0%B5%D1%82%20%D1%81%20%D0%BF%D1%80%D0%BE%D0%B1%D0%B5%D0%BB%D0%B0%D0%BC%D0%B8+1.html"));
        assertEquals("/tmp/%20.html",ChatAttachmentUi.localPath("/tmp/%2520.html"));
        for(String path:new String[]{"//host/file","file:///tmp/a","/tmp/%00x","/tmp/%0ax"})try{ChatAttachmentUi.localPath(path);fail(path);}catch(IllegalArgumentException expected){}
    }
    @Test public void documentServesOnlyOneFileAndEnforcesSizeSignatureAndNetworkPolicy()throws Exception{
        File file=File.createTempFile("viewer-",".html");try{try(OutputStream out=new FileOutputStream(file)){out.write("<script>document.body.textContent='ok'</script>".getBytes(StandardCharsets.UTF_8));}
            ChatFileViewer.Document d=new ChatFileViewer.Document(file,true);WebResourceResponse own=d.response(d.url);assertEquals(200,own.getStatusCode());assertEquals(ChatFileViewer.POLICY,own.getResponseHeaders().get("Content-Security-Policy"));assertTrue(new String(read(own.getData()),StandardCharsets.UTF_8).contains("script"));
            for(String url:new String[]{"file:///root/.codex/auth.json","content://other","https://example.org/","http://127.0.0.1:19051/",d.url+"/../other",d.url+"?other"}){WebResourceResponse r=d.response(url);assertEquals(403,r.getStatusCode());assertEquals(0,read(r.getData()).length);}
            try{new ChatFileViewer.Document(file,false);fail();}catch(IOException expected){assertEquals("INVALID_GIF",expected.getMessage());}
            try(RandomAccessFile large=new RandomAccessFile(file,"rw")){large.setLength(32*1024*1024+1);}
            try{new ChatFileViewer.Document(file,true);fail();}catch(IOException expected){assertEquals("PREVIEW_LIMIT",expected.getMessage());}
        }finally{file.delete();}
    }
    @Test public void webViewDisablesPrivilegedSurfacesAndHandlesRendererFailure()throws Exception{
        Activity a=Robolectric.buildActivity(Activity.class).setup().get();File file=File.createTempFile("secure-viewer-",".html");try{java.nio.file.Files.write(file.toPath(),"<button onclick=\"this.textContent='ok'\">Play</button>".getBytes(StandardCharsets.UTF_8));ChatFileViewer.Document doc=new ChatFileViewer.Document(file,true);List<String> failures=new ArrayList<>();WebView w=ChatFileViewer.create(a,doc,failures::add);LinearLayout parent=new LinearLayout(a);parent.addView(w,new LinearLayout.LayoutParams(-1,300));a.setContentView(parent);assertNull("No load before viewport",Shadows.shadowOf(w).getLastLoadedUrl());layout(parent);assertEquals(doc.url,Shadows.shadowOf(w).getLastLoadedUrl());
            WebSettings settings=w.getSettings();assertTrue(settings.getJavaScriptEnabled());assertTrue(settings.getBlockNetworkLoads());assertFalse(settings.getAllowFileAccess());assertFalse(settings.getAllowContentAccess());assertFalse(settings.getAllowFileAccessFromFileURLs());assertFalse(settings.getAllowUniversalAccessFromFileURLs());assertFalse(settings.getDomStorageEnabled());assertFalse(settings.getDatabaseEnabled());assertFalse(settings.getJavaScriptCanOpenWindowsAutomatically());assertFalse(settings.supportMultipleWindows());assertEquals(WebSettings.MIXED_CONTENT_NEVER_ALLOW,settings.getMixedContentMode());
            for(String target:new String[]{"file:///tmp/other","https://example.org","intent://app","content://private"}){assertTrue(w.getWebViewClient().shouldOverrideUrlLoading(w,request(target)));assertEquals("LINK_BLOCKED",failures.get(failures.size()-1));}
            assertFalse(w.getWebViewClient().shouldOverrideUrlLoading(w,request(doc.url+"#section")));assertEquals(403,w.getWebViewClient().shouldInterceptRequest(w,request("https://other.invalid/")).getStatusCode());
            final boolean[] chooserRejected={false};assertTrue(w.getWebChromeClient().onShowFileChooser(w,value->{assertNull(value);chooserRejected[0]=true;},null));assertTrue(chooserRejected[0]);
            w.getWebViewClient().onReceivedHttpError(w,request(doc.url),doc.response("https://blocked.invalid"));assertEquals("WEBVIEW_ERROR",failures.get(failures.size()-1));
            assertTrue(w.getWebViewClient().onRenderProcessGone(w,null));assertNull(w.getParent());assertTrue(Shadows.shadowOf(w).wasDestroyCalled());assertEquals("WEBVIEW_ERROR",failures.get(failures.size()-1));ChatFileViewer.release(w);
        }finally{file.delete();a.finish();}
    }
    private WebResourceRequest request(String url){return new WebResourceRequest(){public android.net.Uri getUrl(){return android.net.Uri.parse(url);}public boolean isForMainFrame(){return true;}public boolean isRedirect(){return false;}public boolean hasGesture(){return true;}public String getMethod(){return "GET";}public Map<String,String> getRequestHeaders(){return Collections.emptyMap();}};}
    @Test public void invalidGifShowsReasonAndStillOffersOriginal()throws Exception{
        TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);WorkspaceStore.Session p=profile(0);AgentChatView view=new AgentChatView(a,r->{},()->{},()->{},()->{},()->{});ChatAttachmentUi ui=new ChatAttachmentUi(a,view);try{fixture(a,p,".gif",new byte[]{1,2,3});ui.sync("origin",p,true);ui.captureAttachment(new JSONObject().put("name","bad.gif").put("type","image").put("path",ChatAttachments.prefix(p)+"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa/original.gif")).run();AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();for(int i=0;i<150&&!d.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled();i++){Thread.sleep(10);Shadows.shadowOf(Looper.getMainLooper()).idle();}assertTrue(d.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled());assertTrue(contains(d.getWindow().getDecorView(),"Preview unavailable")||contains(d.getWindow().getDecorView(),"Предпросмотр недоступен"));assertNull(web(d.getWindow().getDecorView()));}finally{ui.close();a.finish();}
    }
    @Test public void oversizedHtmlShowsCurrentLimitsInBothLanguagesAndKeepsOriginal()throws Exception{
        for(String language:new String[]{"ru","en"}){
            TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);WorkspaceLanguage.select(a,language);WorkspaceLanguage.apply(a);WorkspaceStore.Session p=profile(0);AgentChatView view=new AgentChatView(a,r->{},()->{},()->{},()->{},()->{});ChatAttachmentUi ui=new ChatAttachmentUi(a,view);
            try{File f=fixture(a,p,".html",new byte[]{1});try(RandomAccessFile sparse=new RandomAccessFile(f,"rw")){sparse.setLength(32L*1024*1024+1);}ui.sync("origin",p,true);ui.captureAttachment(new JSONObject().put("name","large.html").put("type","binary").put("path",ChatAttachments.prefix(p)+"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa/original.html")).run();AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();for(int i=0;i<150&&!d.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled();i++){Thread.sleep(10);Shadows.shadowOf(Looper.getMainLooper()).idle();}assertTrue(d.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled());assertTrue(contains(d.getWindow().getDecorView(),language.equals("ru")?"HTML до 32 МиБ, GIF до 64 МиБ":"HTML up to 32 MiB, GIF up to 64 MiB"));assertNull(web(d.getWindow().getDecorView()));}finally{ui.close();a.finish();}
        }
    }
    private byte[] read(InputStream in)throws Exception{try(InputStream source=in;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[1024];int n;while((n=source.read(b))!=-1)out.write(b,0,n);return out.toByteArray();}}
    @Test public void missingFileHasVisibleErrorAndNoSilentFailure()throws Exception{
        TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);AgentChatView view=new AgentChatView(a,r->{},()->{},()->{},()->{},()->{});ChatAttachmentUi ui=new ChatAttachmentUi(a,view);try{WorkspaceStore.Session p=profile(0);ui.sync("origin",p,true);ui.capturePath(ChatAttachments.prefix(p)+"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb/missing.html").run();AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();for(int i=0;i<150&&!contains(d.getWindow().getDecorView(),"unavailable")&&!contains(d.getWindow().getDecorView(),"недоступно");i++){Thread.sleep(10);Shadows.shadowOf(Looper.getMainLooper()).idle();}assertTrue(contains(d.getWindow().getDecorView(),"unavailable")||contains(d.getWindow().getDecorView(),"недоступно"));assertFalse(d.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled());}finally{ui.close();a.finish();}
    }
    private boolean contains(View v,String word){if(v instanceof TextView&&((TextView)v).getText().toString().contains(word))return true;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)if(contains(((ViewGroup)v).getChildAt(i),word))return true;return false;}
}
