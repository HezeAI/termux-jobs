package com.termux.app.workspace;
import android.app.*;import android.os.*;import android.webkit.*;import android.view.*;import android.widget.*;
import com.termux.app.TermuxActivity;import com.termux.R;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;
import java.util.*;import static org.junit.Assert.*;import org.robolectric.annotation.Implements;import org.robolectric.annotation.Implementation;import org.robolectric.shadow.api.Shadow;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class,shadows={WorkspaceDocumentationTest.TlsTracker.class,DocumentationWebViewShadow.class})
public class WorkspaceDocumentationTest {
 @Implements(SslErrorHandler.class) public static class TlsTracker {boolean cancelled,proceeded;@Implementation protected void cancel(){cancelled=true;}@Implementation protected void proceed(){proceeded=true;}}
 Activity a;
 @Before public void setup(){a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);WorkspaceLanguage.select(a,"ru");}
 @After public void cleanup(){a.finish();WorkspaceDocumentationUi.dispose(a);}
 Dialog dialog(){return org.robolectric.shadows.ShadowDialog.getLatestDialog();}
 View tagged(String tag){return dialog().getWindow().getDecorView().findViewWithTag(tag);}
 WebView web(){View decor=dialog().getWindow().getDecorView();decor.measure(View.MeasureSpec.makeMeasureSpec(440,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(850,View.MeasureSpec.EXACTLY));decor.layout(0,0,440,850);Shadows.shadowOf(Looper.getMainLooper()).idle();return (WebView)tagged("documentation-webview");}
 @Test public void onlyReviewedHttpsDomainsAreInternal(){
  assertTrue(WorkspaceDocumentation.WIKI.owns("https://wiki.archlinux.org/title/Xfce"));
  for(String url:Arrays.asList("http://wiki.archlinux.org/","https://wiki.archlinux.org.evil.test/","https://evilwiki.archlinux.org/","https://user:pass@wiki.archlinux.org/","https://wiki.archlinux.org:8080/","file:///sdcard/a","content://com.termux/a","javascript:alert(1)","data:text/html,a","intent://test","https://wiki.archlinux.org/\n"))assertFalse(url,WorkspaceDocumentation.WIKI.owns(url));
  assertFalse(WorkspaceDocumentation.environment("termux").owns("https://github.com/other/project"));
  assertFalse(WorkspaceDocumentation.environment("termux").owns("https://gist.github.com/termux/example"));
  assertTrue(WorkspaceDocumentation.environment("termux").owns("https://github.com/termux/termux-packages/wiki"));
 }
 @Test public void everyEnvironmentHasItsOwnOfficialSourceAndHonestLanguageFallback(){
  assertEquals(10,WorkspaceDocumentation.targets().size());
  for(String target:WorkspaceDocumentation.targets()){
   WorkspaceDocumentation.Source s=WorkspaceDocumentation.environment(target);assertEquals(target,s.id);assertTrue(target,s.owns(s.home("ru")));assertTrue(target,s.owns(s.home("en")));assertEquals(s.ru==null,s.fallback("ru"));assertFalse(s.fallback("en"));
  }
  assertSame(WorkspaceDocumentation.environment("ubuntu"),WorkspaceDocumentation.environment("ubuntu-ai"));
  try{WorkspaceDocumentation.environment("../kali");fail();}catch(IllegalArgumentException expected){}
 }
 @Test public void wikiSearchIsEncodedAndNeverChangesOrigin(){String q="XFCE & scale; $(touch /tmp/test)";String url=WorkspaceDocumentation.search(q,"ru");assertTrue(WorkspaceDocumentation.WIKI.owns(url));assertEquals(q,android.net.Uri.parse(url).getQueryParameter("search"));assertEquals("ru",android.net.Uri.parse(url).getQueryParameter("uselang"));}
 @Test public void websiteHasNoLocalFileOrContentAccessOrPopupPermissions(){
  WorkspaceDocumentationUi.wiki(a);WebSettings s=web().getSettings();assertFalse(s.getAllowFileAccess());assertFalse(s.getAllowContentAccess());assertFalse(s.getAllowFileAccessFromFileURLs());assertFalse(s.getAllowUniversalAccessFromFileURLs());assertFalse(s.getJavaScriptCanOpenWindowsAutomatically());assertFalse(s.supportMultipleWindows());assertEquals(WebSettings.MIXED_CONTENT_NEVER_ALLOW,s.getMixedContentMode());assertTrue(s.getMediaPlaybackRequiresUserGesture());assertNotNull(tagged("docs-browser"));
  WebView v=web();assertTrue(v.getWebViewClient().shouldOverrideUrlLoading(v,"file:///data/data/com.termux/files/home/.ssh/id_ed25519"));assertFalse(v.getWebViewClient().shouldOverrideUrlLoading(v,"https://external.example/"));assertNull(Shadows.shadowOf(a).getNextStartedActivity());
 }
 @Test public void closingAndOpeningWikiKeepsTheSamePageScrollAndWebView(){
  WorkspaceDocumentationUi.wiki(a);WebView v=web();String article="https://wiki.archlinux.org/title/Xfce";v.loadUrl(article);v.getWebViewClient().onPageStarted(v,article,null);v.scrollTo(0,240);
  tagged("docs-close").performClick();assertFalse(dialog().isShowing());WorkspaceDocumentationUi.wiki(a);assertSame(v,web());assertEquals(240,web().getScrollY());assertEquals(article,Shadows.shadowOf(web()).getLastLoadedUrl());
 }
 @Test public void switchingEnvironmentDocumentationDoesNotOverwriteSharedWikiPage(){
  WorkspaceDocumentationUi.wiki(a);WebView v=web();String url="https://wiki.archlinux.org/title/Fonts";v.loadUrl(url);v.getWebViewClient().onPageStarted(v,url,null);v.scrollTo(0,150);tagged("docs-close").performClick();
  WorkspaceDocumentationUi.environment(a,"kali");
  assertNotNull("Kali viewer",web());assertNotSame(v,web());assertEquals("https://www.kali.org/docs/",Shadows.shadowOf(web()).getLastLoadedUrl());
  tagged("docs-close").performClick();
  WorkspaceDocumentationUi.wiki(a);
  assertNotNull("Restored wiki viewer",web());assertNotNull("Restored wiki client",web().getWebViewClient());assertEquals(url,Shadows.shadowOf(web()).getLastLoadedUrl());
  web().getWebViewClient().onPageFinished(web(),url);
  Shadows.shadowOf(Looper.getMainLooper()).idle();
  assertEquals(150,web().getScrollY());
 }
 @Test public void activityRecreationRetainsGlobalSnapshotButFinishingClearsIt(){
  WorkspaceDocumentationUi.wiki(a);WebView first=web();String url="https://wiki.archlinux.org/title/Termux";first.loadUrl(url);first.getWebViewClient().onPageStarted(first,url,null);first.scrollTo(0,180);
  WorkspaceDocumentationUi.dispose(a);WorkspaceDocumentationUi.wiki(a);assertNotSame(first,web());assertEquals(url,Shadows.shadowOf(web()).getLastLoadedUrl());web().getWebViewClient().onPageFinished(web(),url);Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals(180,web().getScrollY());
  a.finish();WorkspaceDocumentationUi.dispose(a);a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);WorkspaceDocumentationUi.wiki(a);assertNotEquals(url,Shadows.shadowOf(web()).getLastLoadedUrl());
 }
 @Test public void oldWebViewCallbacksCannotReplaceNewEnvironmentPage(){
  WorkspaceDocumentationUi.wiki(a);WebView old=web();WebViewClient client=old.getWebViewClient();WorkspaceDocumentationUi.environment(a,"kali");WebView current=web();String wiki="https://wiki.archlinux.org/title/Xfce";client.onPageStarted(old,wiki,null);client.onPageFinished(old,wiki);tagged("docs-close").performClick();WorkspaceDocumentationUi.environment(a,"kali");assertSame(current,web());assertEquals("https://www.kali.org/docs/",Shadows.shadowOf(web()).getLastLoadedUrl());
 }
 @Test public void russianUiExplainsMissingRussianDocumentationAndEnglishUiIsLocalized(){
  WorkspaceDocumentationUi.environment(a,"kali");assertTrue(((TextView)tagged("docs-status")).getText().toString().contains("английский"));assertEquals("Найти на странице",((EditText)tagged("docs-search")).getHint().toString());
  tagged("docs-close").performClick();WorkspaceLanguage.select(a,"en");WorkspaceDocumentationUi.environment(a,"kali");assertTrue(((TextView)tagged("docs-status")).getText().toString().contains("Official website"));assertEquals("Close documentation",tagged("docs-close").getContentDescription());
 }
 @Test public void switchingLanguageUsesOfficialTranslatedHomeAndKeepsWikiHistorySeparate(){
  WorkspaceDocumentationUi.wiki(a);assertTrue(Shadows.shadowOf(web()).getLastLoadedUrl().contains("%D0"));tagged("docs-language-en").performClick();assertEquals("https://wiki.archlinux.org/",Shadows.shadowOf(web()).getLastLoadedUrl());tagged("docs-language-ru").performClick();assertTrue(Shadows.shadowOf(web()).getLastLoadedUrl().contains("%D0"));
 }
 @Test public void sslFailureCancelsInsteadOfBypassingCertificateVerification(){
  WorkspaceDocumentationUi.wiki(a);SslErrorHandler handler=Shadow.newInstanceOf(SslErrorHandler.class);web().getWebViewClient().onReceivedSslError(web(),handler,null);TlsTracker tracker=Shadow.extract(handler);assertTrue(tracker.cancelled);assertFalse(tracker.proceeded);assertTrue(((TextView)tagged("docs-status")).getText().toString().contains("Не удалось"));
 }
 @Test public void programsFooterOpensDocumentationWithoutPackageWorkOrClosingPrograms(){
  List<String> calls=new ArrayList<>();WorkspaceDesktopPrograms p=new WorkspaceDesktopPrograms((TermuxActivity)a,(op,id)->calls.add(op),()->{});WorkspaceStore.Session session=new WorkspaceStore.Session();session.environment="proot:kali";
  try{p.open(session,null);AlertDialog menu=(AlertDialog)dialog();assertEquals("Документация",menu.getButton(AlertDialog.BUTTON_NEUTRAL).getText());assertNotNull(menu.getButton(AlertDialog.BUTTON_NEGATIVE));menu.getButton(AlertDialog.BUTTON_NEUTRAL).performClick();assertTrue(menu.isShowing());assertTrue(calls.isEmpty());assertEquals("https://www.kali.org/docs/",Shadows.shadowOf(web()).getLastLoadedUrl());tagged("docs-close").performClick();assertTrue(menu.isShowing());}finally{p.close();}
 }
 @Test public void headerBookBelongsToBrandAndHasVisibleArchWikiLabel()throws Exception{
  a.setContentView(R.layout.activity_termux);java.lang.reflect.Field terminal=TermuxActivity.class.getDeclaredField("mTerminalView");terminal.setAccessible(true);terminal.set(a,a.findViewById(R.id.terminal_view));WorkspaceController controller=new WorkspaceController((TermuxActivity)a);
  try{java.lang.reflect.Method render=WorkspaceController.class.getDeclaredMethod("renderHeader");render.setAccessible(true);render.invoke(controller);java.lang.reflect.Field field=WorkspaceController.class.getDeclaredField("header");field.setAccessible(true);View root=(View)field.get(controller);LinearLayout book=root.findViewWithTag("workspace-archwiki");assertNotNull(book);assertEquals("ArchWiki",((TextView)book.getChildAt(1)).getText().toString());ViewGroup brand=(ViewGroup)book.getParent();assertEquals("Termux Job’s v2.0",((TextView)brand.getChildAt(0)).getText().toString());}finally{controller.close();}
 }
 @Test public void officialForumOpensInsideWikiAndKeepsItsPageAcrossClose()throws Exception{
  assertTrue(WorkspaceDocumentation.WIKI.owns(WorkspaceDocumentation.FORUM));
  assertTrue(WorkspaceDocumentation.WIKI.owns("https://bbs.archlinux.org/viewtopic.php?id=123"));
  for(String url:Arrays.asList("http://bbs.archlinux.org/","https://bbs.archlinux.org.evil.test/","https://evil.bbs.archlinux.org/","https://user:password@bbs.archlinux.org/","https://archlinux.org/"))assertFalse(url,WorkspaceDocumentation.WIKI.owns(url));
  WorkspaceDocumentationUi.wiki(a);WebView current=web();View forum=tagged("docs-forum"),reload=tagged("docs-reload");assertSame(forum.getParent(),reload.getParent());ViewGroup toolbar=(ViewGroup)forum.getParent();assertEquals(toolbar.indexOfChild(forum)+1,toolbar.indexOfChild(reload));assertNotNull(forum.getBackground());forum.performClick();assertEquals(WorkspaceDocumentation.FORUM,Shadows.shadowOf(current).getLastLoadedUrl());tagged("docs-close").performClick();WorkspaceDocumentationUi.wiki(a);assertSame(current,web());assertEquals(WorkspaceDocumentation.FORUM,Shadows.shadowOf(current).getLastLoadedUrl());
 }
 @Test public void forumShortcutBeforeFirstViewportLoadsAfterLayoutAndStaysInWikiWindow(){
  WorkspaceDocumentationUi.wiki(a);tagged("docs-forum").performClick();WebView current=web();assertEquals(WorkspaceDocumentation.FORUM,Shadows.shadowOf(current).getLastLoadedUrl());assertTrue(dialog().isShowing());
 }

 @Test public void allDocumentationReadersKeepHttpsNavigationInternalAndRetainExternalPage(){
  for(String target:WorkspaceDocumentation.targets()){
   WorkspaceDocumentationUi.environment(a,target);WebView v=web();String url="https://example.org/manual/"+target;
   assertFalse(v.getWebViewClient().shouldOverrideUrlLoading(v,url));v.loadUrl(url);v.getWebViewClient().onPageStarted(v,url,null);assertTrue(((TextView)tagged("docs-status")).getText().toString().contains("example.org"));assertNull(Shadows.shadowOf(a).getNextStartedActivity());tagged("docs-close").performClick();
   WorkspaceDocumentationUi.environment(a,target);assertSame(v,web());assertEquals(url,Shadows.shadowOf(v).getLastLoadedUrl());tagged("docs-close").performClick();
  }
 }
 @Test public void longPressLinkShowsCopyAndBrowserActionsWithoutNavigating(){
  WorkspaceDocumentationUi.wiki(a);WebView v=web();String page=Shadows.shadowOf(v).getLastLoadedUrl(),url="https://example.org/page?a=1&b=2";
  ((org.robolectric.shadows.ShadowWebView)Shadow.extract(v)).setHitTestResult(org.robolectric.shadows.ShadowWebView.createHitTestResult(WebView.HitTestResult.SRC_ANCHOR_TYPE,url));assertTrue(v.performLongClick());Dialog menu=dialog();assertNotNull(menu.getWindow().getDecorView().findViewWithTag("docs-open-link-browser"));assertEquals(page,Shadows.shadowOf(v).getLastLoadedUrl());assertNull(Shadows.shadowOf(a).getNextStartedActivity());
  menu.getWindow().getDecorView().findViewWithTag("docs-copy-link").performClick();assertEquals(url,((android.content.ClipboardManager)a.getSystemService(android.content.Context.CLIPBOARD_SERVICE)).getPrimaryClip().getItemAt(0).getText().toString());assertNull(Shadows.shadowOf(a).getNextStartedActivity());
 }
 @Test public void explicitBrowserActionUsesChooserAndRejectsLocalOrScriptUrls(){
  for(String url:Arrays.asList("file:///sdcard/private","content://com.termux/private","javascript:alert(1)","intent://example")){Dialog before=dialog();WorkspaceDocumentationUi.linkMenu(a,url);assertSame(before,dialog());}
  WorkspaceDocumentationUi.linkMenu(a,"https://example.org/");dialog().getWindow().getDecorView().findViewWithTag("docs-open-link-browser").performClick();android.content.Intent chooser=Shadows.shadowOf(a).getNextStartedActivity();assertEquals(android.content.Intent.ACTION_CHOOSER,chooser.getAction());android.content.Intent view=chooser.getParcelableExtra(android.content.Intent.EXTRA_INTENT);assertEquals(android.content.Intent.ACTION_VIEW,view.getAction());assertEquals("https://example.org/",view.getDataString());
 }
 @Test public void imageLinkUsesAnchorHrefAndLateCallbackCannotOpenOnAnotherWindow(){
  WorkspaceDocumentationUi.wiki(a);WebView v=web();Dialog wiki=dialog();DocumentationWebViewShadow shadow=Shadow.extract(v);shadow.setHitTestResult(org.robolectric.shadows.ShadowWebView.createHitTestResult(WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE,"https://example.org/thumbnail.png"));assertTrue(v.performLongClick());android.os.Message href=shadow.href;assertNotNull(href);android.os.Bundle b=new android.os.Bundle();b.putString("url","https://example.org/article");href.setData(b);href.sendToTarget();Shadows.shadowOf(Looper.getMainLooper()).idle();Dialog menu=dialog();menu.getWindow().getDecorView().findViewWithTag("docs-copy-link").performClick();assertEquals("https://example.org/article",((android.content.ClipboardManager)a.getSystemService(android.content.Context.CLIPBOARD_SERVICE)).getPrimaryClip().getItemAt(0).getText().toString());
  v.performLongClick();android.os.Message stale=shadow.href;wiki.getWindow().getDecorView().findViewWithTag("docs-close").performClick();WorkspaceDocumentationUi.environment(a,"debian");Dialog current=dialog();stale.setData(b);stale.sendToTarget();Shadows.shadowOf(Looper.getMainLooper()).idle();assertSame(current,dialog());
 }

 @Test public void chatLinkShowOpensExactUrlInsideReaderAndPreservesSeparateWikiState(){
  WorkspaceDocumentationUi.wiki(a);WebView wiki=web();String article="https://wiki.archlinux.org/title/Xfce";wiki.loadUrl(article);wiki.getWebViewClient().onPageStarted(wiki,article,null);tagged("docs-close").performClick();
  View source=new View(a);String url="https://example.org/chat-page";AgentMarkdown.openLink(source,url);AlertDialog prompt=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertEquals("Показать",prompt.getButton(AlertDialog.BUTTON_NEGATIVE).getText().toString());assertTrue(prompt.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled());prompt.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();WebView current=web();assertEquals(url,Shadows.shadowOf(current).getLastLoadedUrl());assertNull(Shadows.shadowOf(a).getNextStartedActivity());tagged("docs-close").performClick();
  AgentMarkdown.openLink(source,"https://example.org/another-page");prompt=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();prompt.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertEquals("https://example.org/another-page",Shadows.shadowOf(web()).getLastLoadedUrl());tagged("docs-close").performClick();WorkspaceDocumentationUi.wiki(a);assertEquals(article,Shadows.shadowOf(web()).getLastLoadedUrl());tagged("docs-close").performClick();
  WorkspaceDocumentationUi.link(a,"https://example.org/new-explicit-link");DocumentationWebViewShadow fresh=Shadow.extract(web());assertEquals(Collections.singletonList("https://example.org/new-explicit-link"),fresh.urls);
 }

}
