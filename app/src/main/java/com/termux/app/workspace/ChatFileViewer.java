package com.termux.app.workspace;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.webkit.*;
import java.io.*;
import java.util.*;

/** One cached document, one ephemeral origin. No app bridge, file access or network. */
final class ChatFileViewer {
    static boolean html(String name){String n=name.toLowerCase(Locale.ROOT);return n.endsWith(".html")||n.endsWith(".htm");}
    static boolean animated(String name){return name.toLowerCase(Locale.ROOT).endsWith(".gif");}
    static boolean handles(String name){return html(name)||animated(name);}
    static final String POLICY="default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; img-src 'self' data: blob:; media-src data: blob:; font-src data:; connect-src 'none'; frame-src 'none'; object-src 'none'; base-uri 'none'; form-action 'none'";
    static final class Document {
        final File file;final boolean html;final String url="https://preview-"+UUID.randomUUID()+".invalid/document";
        Document(File file,boolean html)throws IOException{
            this.file=file;this.html=html;
            if(file.length()==0||file.length()>(html?32L:64L)*1024*1024)throw new IOException("PREVIEW_LIMIT");
            if(!html){try(InputStream in=new FileInputStream(file)){byte[] header=new byte[10];if(in.read(header)!=10)throw new IOException("INVALID_GIF");String magic=new String(header,0,6,java.nio.charset.StandardCharsets.US_ASCII);int w=(header[6]&255)|((header[7]&255)<<8),h=(header[8]&255)|((header[9]&255)<<8);if(!(magic.equals("GIF87a")||magic.equals("GIF89a"))||w==0||h==0||(long)w*h>16000000)throw new IOException("INVALID_GIF");}}
        }
        boolean owns(String requested){return url.equals(requested)||requested!=null&&requested.startsWith(url+"#");}
        WebResourceResponse response(String requested){
            try{
                if(owns(requested))return new WebResourceResponse(html?"text/html":"image/gif",html?"UTF-8":null,200,"OK",headers(),new FileInputStream(file));
            }catch(IOException ignored){}
            return new WebResourceResponse("text/plain","UTF-8",403,"Blocked",headers(),new ByteArrayInputStream(new byte[0]));
        }
        private Map<String,String> headers(){Map<String,String> h=new HashMap<>();h.put("Content-Security-Policy",POLICY);h.put("X-Content-Type-Options","nosniff");h.put("Cache-Control","no-store");return h;}
    }
    static WebView create(Context context,Document document,java.util.function.Consumer<String> failure){
        WebView web=new WebView(context);web.setBackgroundColor(Color.rgb(11,19,26));
        WebSettings s=web.getSettings();s.setJavaScriptEnabled(document.html);s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setAllowFileAccessFromFileURLs(false);s.setAllowUniversalAccessFromFileURLs(false);
        s.setBlockNetworkLoads(true);s.setDomStorageEnabled(false);s.setDatabaseEnabled(false);s.setGeolocationEnabled(false);s.setJavaScriptCanOpenWindowsAutomatically(false);s.setSupportMultipleWindows(false);s.setMediaPlaybackRequiresUserGesture(true);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setBuiltInZoomControls(true);s.setDisplayZoomControls(false);s.setUseWideViewPort(true);s.setLoadWithOverviewMode(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
        web.setWebViewClient(new WebViewClient(){
            @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){return document.response(r.getUrl().toString());}
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){if(document.owns(r.getUrl().toString()))return false;failure.accept("LINK_BLOCKED");return true;}
            @Override public void onPageCommitVisible(WebView v,String url){if(!Boolean.TRUE.equals(v.getTag()))v.postVisualStateCallback(0,new WebView.VisualStateCallback(){@Override public void onComplete(long id){if(!Boolean.TRUE.equals(v.getTag()))v.invalidate();}});}
            @Override public void onReceivedHttpError(WebView v,WebResourceRequest r,WebResourceResponse response){if(r.isForMainFrame()&&response.getStatusCode()>=400)failure.accept("WEBVIEW_ERROR");}
            @Override public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError e){if(r.isForMainFrame())failure.accept("WEBVIEW_ERROR");}
            @Override public boolean onRenderProcessGone(WebView v,RenderProcessGoneDetail detail){if(v.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)v.getParent()).removeView(v);destroy(v);failure.accept("WEBVIEW_ERROR");return true;}
        });
        web.setWebChromeClient(new WebChromeClient(){@Override public void onPermissionRequest(PermissionRequest r){r.deny();}@Override public boolean onShowFileChooser(WebView v,ValueCallback<Uri[]> callback,FileChooserParams params){callback.onReceiveValue(null);return true;}});
        web.setDownloadListener((u,a,d,m,n)->failure.accept("LINK_BLOCKED"));
        // The documentation viewer uses the same rule: start with a real viewport.
        web.addOnLayoutChangeListener(new android.view.View.OnLayoutChangeListener(){
            private boolean started;
            @Override public void onLayoutChange(android.view.View v,int l,int t,int r,int b,int ol,int ot,int or,int ob){
                if(started||Boolean.TRUE.equals(web.getTag())||r<=l||b<=t)return;
                started=true;web.removeOnLayoutChangeListener(this);web.onResume();web.loadUrl(document.url);
            }
        });return web;
    }
    static void release(WebView web){if(Boolean.TRUE.equals(web.getTag()))return;web.stopLoading();web.onPause();web.loadUrl("about:blank");destroy(web);}
    private static void destroy(WebView web){if(Boolean.TRUE.equals(web.getTag()))return;web.setTag(Boolean.TRUE);web.removeAllViews();web.destroy();}
}
