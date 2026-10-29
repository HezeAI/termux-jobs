package com.termux.app.workspace;
import android.view.View;import android.webkit.WebView;import org.robolectric.annotation.*;import org.robolectric.shadow.api.Shadow;import org.robolectric.shadows.ShadowWebView;import org.robolectric.util.ReflectionHelpers.ClassParameter;
/** The 4.4 WebView provider's ViewDelegate returns false from setFrame. Use
 * Android View's real bounds calculation for layout tests; no Chromium mock
 * drawing is claimed. The fake provider's long-click delegate is similarly
 * routed to View's listener; native touch gesture recognition is not emulated. Visual callbacks remain explicitly controlled by tests. */
@Implements(WebView.class)
public class DocumentationWebViewShadow extends ShadowWebView {
 @RealObject private WebView view;
 @Implementation protected boolean performLongClick(){return Shadow.directlyOn(view,View.class,"performLongClick");}
 final java.util.List<String> urls=new java.util.ArrayList<>();
 android.os.Message href;
 @Implementation protected void requestFocusNodeHref(android.os.Message message){href=message;}
 boolean loadedBeforeViewport,resumedBeforeViewport;int stopped,invalidations;WebView.VisualStateCallback frame;long frameId;
 @Implementation protected boolean setFrame(int l,int t,int r,int b){return Shadow.directlyOn(view,View.class,"setFrame",ClassParameter.from(int.class,l),ClassParameter.from(int.class,t),ClassParameter.from(int.class,r),ClassParameter.from(int.class,b));}
 @Override @Implementation protected void loadUrl(String url){urls.add(url);loadedBeforeViewport|=!view.isAttachedToWindow()||view.getWidth()<=0||view.getHeight()<=0;super.loadUrl(url);}
 @Override @Implementation protected void onResume(){resumedBeforeViewport|=!view.isAttachedToWindow()||view.getWidth()<=0||view.getHeight()<=0;super.onResume();}
 @Implementation protected void stopLoading(){stopped++;}
 @Implementation protected void postVisualStateCallback(long id,WebView.VisualStateCallback callback){frame=callback;frameId=id;}
 @Implementation protected void postInvalidateOnAnimation(){invalidations++;}
}
