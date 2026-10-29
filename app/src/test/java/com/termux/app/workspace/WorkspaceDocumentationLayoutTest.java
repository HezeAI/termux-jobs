package com.termux.app.workspace;
import android.app.*;import android.view.*;import android.webkit.*;import com.termux.R;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class,shadows=DocumentationWebViewShadow.class)
public class WorkspaceDocumentationLayoutTest {
 private Activity a;
 @Before public void setup(){a=Robolectric.buildActivity(Activity.class).setup().get();a.setTheme(R.style.Theme_Termux_Black);WorkspaceLanguage.select(a,"ru");}
 @After public void close(){a.finish();WorkspaceDocumentationUi.dispose(a);}
 private void dump(View v,String pad){System.out.println(pad+v.getClass().getSimpleName()+" id="+v.getId()+" view="+v.getWidth()+"x"+v.getHeight()+" measured="+v.getMeasuredWidth()+"x"+v.getMeasuredHeight()+" visibility="+v.getVisibility()+" lp="+(v.getLayoutParams()==null?"null":v.getLayoutParams().width+"x"+v.getLayoutParams().height));if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)dump(((ViewGroup)v).getChildAt(i),pad+" ");}
 private Dialog window(){return org.robolectric.shadows.ShadowDialog.getLatestDialog();}
 private WebView measure(){View decor=window().getWindow().getDecorView();decor.measure(View.MeasureSpec.makeMeasureSpec(440,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(850,View.MeasureSpec.EXACTLY));decor.layout(0,0,440,850);Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();return decor.findViewWithTag("documentation-webview");}
 @Test public void firstOpenAndLoadedPageHaveUsableViewportWithoutReopen(){
  for(String target:new String[]{"archwiki","termux","ubuntu","debian","alpine","fedora","opensuse","void","archlinux","kali","manjaro"}){
   if(target.equals("archwiki"))WorkspaceDocumentationUi.wiki(a);else WorkspaceDocumentationUi.environment(a,target);
   WebView v=measure();System.out.println("DOC_VIEWPORT initial "+target+" "+v.getWidth()+"x"+v.getHeight()+" parent="+((View)v.getParent()).getHeight());
   if(v.getHeight()<=200)dump(window().getWindow().getDecorView(),"");assertTrue(target+" initial web height",v.getHeight()>200);
   v.getWebChromeClient().onProgressChanged(v,100);v.getWebViewClient().onPageFinished(v,v.getUrl());v=measure();System.out.println("DOC_VIEWPORT loaded "+target+" "+v.getWidth()+"x"+v.getHeight()+" parent="+((View)v.getParent()).getHeight());
   assertTrue(target+" loaded web height",v.getHeight()>200);window().dismiss();
  }
 }
 @Test public void initialNavigationWaitsForAttachedMeasuredViewport(){
  WorkspaceDocumentationUi.wiki(a);WebView v=window().getWindow().getDecorView().findViewWithTag("documentation-webview");
  assertNull(Shadows.shadowOf(v).getLastLoadedUrl());v=measure();assertNotNull(Shadows.shadowOf(v).getLastLoadedUrl());DocumentationWebViewShadow tracker=org.robolectric.shadow.api.Shadow.extract(v);assertFalse(tracker.loadedBeforeViewport);assertFalse(tracker.resumedBeforeViewport);
 }
 @Test public void closingDoesNotCancelFetchAndLateVisualCallbacksCannotDrawClosedWindow(){
  WorkspaceDocumentationUi.environment(a,"kali");WebView v=measure();DocumentationWebViewShadow tracker=org.robolectric.shadow.api.Shadow.extract(v);String url="https://www.kali.org/docs/";v.getWebViewClient().onPageCommitVisible(v,url);assertNotNull(tracker.frame);WebView.VisualStateCallback frame=tracker.frame;long id=tracker.frameId;window().dismiss();int before=tracker.invalidations;frame.onComplete(id);assertEquals(before,tracker.invalidations);assertEquals(0,tracker.stopped);
  v.getWebViewClient().onPageFinished(v,url);WorkspaceDocumentationUi.environment(a,"kali");assertSame(v,measure());assertFalse(tracker.resumedBeforeViewport);
 }

}
