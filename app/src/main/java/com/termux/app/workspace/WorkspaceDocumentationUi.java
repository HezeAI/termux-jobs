package com.termux.app.workspace;

import android.annotation.SuppressLint;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.net.http.SslError;
import android.os.*;
import android.text.InputFilter;
import android.view.*;
import android.view.inputmethod.*;
import android.webkit.*;
import android.widget.*;
import com.termux.R;
import java.io.ByteArrayInputStream;
import java.util.*;

/** Read-only website window. One global ArchWiki state per app process, not per terminal. */
final class WorkspaceDocumentationUi {
    private static final Map<Activity,Reader> READERS=new IdentityHashMap<>();
    private static final Map<String,Page> PAGES=new HashMap<>();
    private static String language;private static boolean languagePicked;
    private static final class Page {Bundle history;String url;int x,y;}
    static void wiki(Activity activity){show(activity,WorkspaceDocumentation.WIKI);}
    static void link(Activity activity,String url){
        if(WorkspaceDocumentation.https(url)==null)return;
        WorkspaceDocumentation.Source source=new WorkspaceDocumentation.Source("chat-links",Uri.parse(url).getHost(),url,url);
        show(activity,source,url);
    }
    static void environment(Activity activity,String target){show(activity,WorkspaceDocumentation.environment(target));}
    private static void show(Activity activity,WorkspaceDocumentation.Source source){show(activity,source,null);}
    private static void show(Activity activity,WorkspaceDocumentation.Source source,String explicitUrl){
        if(activity.isFinishing()||activity.isDestroyed())return;
        if(!languagePicked)language=WorkspaceLanguage.code(activity);
        Reader reader=READERS.get(activity);if(reader==null){reader=new Reader(activity);READERS.put(activity,reader);}reader.show(source,explicitUrl);
    }
    static void dispose(Activity activity){Reader r=READERS.remove(activity);if(r!=null)r.dispose();if(activity.isFinishing()){PAGES.clear();language=null;languagePicked=false;}}
    static void linkMenu(Activity activity,String url){
        if(WorkspaceDocumentation.https(url)==null)return;
        boolean russian=WorkspaceLanguage.code(activity).equals("ru");
        LinearLayout body=new LinearLayout(activity);body.setOrientation(LinearLayout.VERTICAL);int p=WorkspaceDialogs.dp(activity,12);body.setPadding(p,p,p,p);
        TextView address=new TextView(activity);address.setText(url);address.setTextColor(WorkspaceDialogs.TEXT);address.setTextSize(13);address.setTextIsSelectable(true);address.setPadding(p,p,p,p);body.addView(address);
        AlertDialog[] menu={null};
        TextView copy=WorkspaceDialogs.action(activity,russian?"Скопировать ссылку":"Copy link",0);copy.setTag("docs-copy-link");copy.setOnClickListener(v->{((android.content.ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("URL",url));menu[0].dismiss();Toast.makeText(activity,russian?"Ссылка скопирована":"Link copied",Toast.LENGTH_SHORT).show();});body.addView(copy);
        TextView browser=WorkspaceDialogs.action(activity,russian?"Открыть в браузере":"Open in browser",0);browser.setTag("docs-open-link-browser");browser.setOnClickListener(v->{menu[0].dismiss();try{activity.startActivity(Intent.createChooser(new Intent(Intent.ACTION_VIEW,Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE),russian?"Открыть в браузере":"Open in browser"));}catch(ActivityNotFoundException unavailable){Toast.makeText(activity,russian?"Браузер не найден":"No browser available",Toast.LENGTH_LONG).show();}});body.addView(browser);
        ScrollView scroll=new ScrollView(activity);scroll.addView(body);menu[0]=WorkspaceDialogs.builder(activity).setTitle(russian?"Ссылка":"Link").setView(scroll).create();menu[0].setCanceledOnTouchOutside(true);menu[0].show();
    }
    static final class Reader implements Application.ActivityLifecycleCallbacks {
        final Activity activity;WorkspaceDocumentation.Source source;String pageKey="",address="",error="",requestedUrl;WebView web;Dialog dialog;LinearLayout root;FrameLayout webHost;TextView status,ru,en,back,site;ProgressBar progress;EditText search;boolean disposed,restoreScroll,pendingInitial,webResumed;long frameEpoch;Runnable pendingActivation;int scrollX,scrollY,lastWidth,lastHeight;
        final View.OnLayoutChangeListener resize=(v,l,t,r,b,ol,ot,or,ob)->size();
        final View.OnLayoutChangeListener viewport=(v,l,t,r,b,ol,ot,or,ob)->activateWeb();
        Reader(Activity activity){this.activity=activity;activity.getApplication().registerActivityLifecycleCallbacks(this);if(language==null)language=WorkspaceLanguage.code(activity);}
        String tr(String r,String e){return WorkspaceLanguage.code(activity).equals("ru")?r:e;}
        int dp(int n){return WorkspaceDialogs.dp(activity,n);}
        void show(WorkspaceDocumentation.Source requested){show(requested,null);}
        void show(WorkspaceDocumentation.Source requested,String explicitUrl){
            if(disposed)return;
            if(dialog!=null){Dialog previous=dialog;closeWindow(previous);previous.dismiss();}
            String key=requested.id+"|"+language;
            if(!key.equals(pageKey)){remember();destroyWeb();source=requested;pageKey=key;address=source.home(language);error="";}
            source=requested;if(explicitUrl!=null){address=explicitUrl;requestedUrl=explicitUrl;error="";}
            root=new LinearLayout(activity);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(6),dp(6),dp(6),dp(6));root.setFocusableInTouchMode(true);
            LinearLayout toolbar=row();back=icon(toolbar,R.drawable.workspace_next,tr("Назад по страницам","Page back"),"docs-back",this::back);back.setRotation(180);
            TextView title=label(source.name,17);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);toolbar.addView(title,new LinearLayout.LayoutParams(0,-2,1));
            if(source==WorkspaceDocumentation.WIKI){TextView forum=label(tr("Форум","Forum"),12);forum.setTag("docs-forum");forum.setGravity(Gravity.CENTER);forum.setBackground(WorkspaceDialogs.background(activity,false));forum.setOnClickListener(v->visit(WorkspaceDocumentation.FORUM));LinearLayout.LayoutParams f=new LinearLayout.LayoutParams(dp(72),dp(40));f.setMargins(dp(4),0,dp(4),0);toolbar.addView(forum,f);}
            icon(toolbar,R.drawable.workspace_docs_reload,tr("Обновить страницу","Reload page"),"docs-reload",()->{if(web!=null){error="";web.reload();}});
            icon(toolbar,R.drawable.workspace_docs_external,tr("Открыть в браузере","Open in browser"),"docs-browser",()->external(address));
            icon(toolbar,R.drawable.workspace_attachment_remove,tr("Закрыть документацию","Close documentation"),"docs-close",()->dialog.dismiss());root.addView(toolbar);
            LinearLayout langs=row();ru=languageButton(langs,"RU","ru");en=languageButton(langs,"EN","en");site=label(Uri.parse(address).getHost(),11);site.setSingleLine(true);site.setEllipsize(android.text.TextUtils.TruncateAt.END);site.setTextColor(WorkspaceDialogs.MUTED);langs.addView(site,new LinearLayout.LayoutParams(0,-2,1));root.addView(langs);
            search=new EditText(activity);search.setTag("docs-search");search.setSingleLine(true);search.setTextColor(WorkspaceDialogs.TEXT);search.setHintTextColor(WorkspaceDialogs.MUTED);search.setTextSize(14);search.setFilters(new InputFilter[]{new InputFilter.LengthFilter(200)});search.setHint(source==WorkspaceDocumentation.WIKI?tr("Поиск по ArchWiki","Search ArchWiki"):tr("Найти на странице","Find on page"));search.setImeOptions(EditorInfo.IME_ACTION_SEARCH|EditorInfo.IME_FLAG_NO_EXTRACT_UI);search.setOnEditorActionListener((v,action,event)->{if(action==EditorInfo.IME_ACTION_SEARCH||event!=null&&event.getKeyCode()==KeyEvent.KEYCODE_ENTER&&event.getAction()==KeyEvent.ACTION_UP){find();return true;}return false;});root.addView(search,new LinearLayout.LayoutParams(-1,dp(44)));
            status=label("",11);status.setTag("docs-status");status.setTextColor(WorkspaceDialogs.MUTED);status.setMaxLines(3);root.addView(status);message();
            progress=new ProgressBar(activity,null,android.R.attr.progressBarStyleHorizontal);progress.setTag("docs-progress");root.addView(progress,new LinearLayout.LayoutParams(-1,dp(3)));
            webHost=new FrameLayout(activity);root.addView(webHost,new LinearLayout.LayoutParams(-1,0,1));
            if(web==null)createWeb();
            if(web!=null){if(web.getParent()!=null)((ViewGroup)web.getParent()).removeView(web);webHost.addView(web,new FrameLayout.LayoutParams(-1,-1));progress.setProgress(web.getProgress());progress.setVisibility(web.getProgress()>=100?View.GONE:View.VISIBLE);}else progress.setVisibility(View.GONE);
            // AlertDialog's wrap-content custom panel can collapse a zero-height,
            // weighted WebView host. Give this reader its own match-parent content.
            final Dialog opened=new Dialog(activity,R.style.WorkspaceDialog);opened.requestWindowFeature(Window.FEATURE_NO_TITLE);opened.setContentView(root,new ViewGroup.LayoutParams(-1,-1));dialog=opened;dialog.setCanceledOnTouchOutside(true);dialog.setOnKeyListener((d,keyCode,event)->{if(keyCode==KeyEvent.KEYCODE_BACK&&event.getAction()==KeyEvent.ACTION_UP&&web!=null&&web.canGoBack()){back();return true;}return false;});
            dialog.setOnDismissListener(d->closeWindow(opened));
            dialog.show();dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE|WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);activity.getWindow().getDecorView().addOnLayoutChangeListener(resize);lastWidth=lastHeight=0;size();root.requestFocus();activateWeb();scheduleActivation();
        }
        @SuppressLint("SetJavaScriptEnabled") private void createWeb(){
            try{
                web=new WebView(activity);pendingInitial=true;webResumed=false;web.addOnLayoutChangeListener(viewport);web.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){@Override public void onViewAttachedToWindow(View v){if(v==web)scheduleActivation();}@Override public void onViewDetachedFromWindow(View v){if(v==web){cancelActivation();frameEpoch++;if(webResumed){web.onPause();webResumed=false;}}}});web.setTag("documentation-webview");WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setAllowFileAccessFromFileURLs(false);s.setAllowUniversalAccessFromFileURLs(false);s.setJavaScriptCanOpenWindowsAutomatically(false);s.setSupportMultipleWindows(false);s.setGeolocationEnabled(false);s.setMediaPlaybackRequiresUserGesture(true);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);s.setLoadWithOverviewMode(true);s.setUseWideViewPort(true);s.setBuiltInZoomControls(true);s.setDisplayZoomControls(false);s.setCacheMode(WebSettings.LOAD_DEFAULT);if(Build.VERSION.SDK_INT>=26)s.setSafeBrowsingEnabled(true);
                web.setOnLongClickListener(v->{
                    WebView current=web;Dialog opened=dialog;
                    if(v!=current||opened==null||!opened.isShowing())return false;
                    WebView.HitTestResult hit=current.getHitTestResult();if(hit==null)return false;
                    if(hit.getType()==WebView.HitTestResult.SRC_ANCHOR_TYPE){String url=hit.getExtra();if(WorkspaceDocumentation.https(url)==null)return false;linkMenu(activity,url);return true;}
                    if(hit.getType()==WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE){
                        String page=address;Handler receiver=new Handler(Looper.getMainLooper(),message->{if(web==current&&dialog==opened&&opened.isShowing()&&page.equals(address)){String url=message.getData().getString("url");if(WorkspaceDocumentation.https(url)!=null)linkMenu(activity,url);}return true;});
                        current.requestFocusNodeHref(receiver.obtainMessage());return true;
                    }
                    return false;
                });
                CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
                web.setWebChromeClient(new WebChromeClient(){@Override public void onProgressChanged(WebView v,int n){if(v==web&&progress!=null){progress.setProgress(n);progress.setVisibility(n==100?View.GONE:View.VISIBLE);}}@Override public void onPermissionRequest(PermissionRequest request){request.deny();}@Override public boolean onShowFileChooser(WebView v,ValueCallback<Uri[]> callback,FileChooserParams p){callback.onReceiveValue(null);return true;}});
                web.setWebViewClient(new WebViewClient(){
                    @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest req){return v!=web||navigate(req.getUrl().toString(),req.isForMainFrame()&&req.hasGesture());}
                    @Override public boolean shouldOverrideUrlLoading(WebView v,String url){return v!=web||navigate(url,false);}
                    @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest req){return WorkspaceDocumentation.https(req.getUrl().toString())==null?new WebResourceResponse("text/plain","UTF-8",new ByteArrayInputStream(new byte[0])):null;}
                    @Override public void onPageStarted(WebView v,String url,android.graphics.Bitmap image){if(v!=web||disposed)return;if(WorkspaceDocumentation.https(url)==null){v.stopLoading();return;}frameEpoch++;address=url;error="";if(status!=null)message();if(progress!=null)progress.setVisibility(View.VISIBLE);}
                    @Override public void onPageCommitVisible(WebView v,String url){if(v==web&&WorkspaceDocumentation.https(url)!=null)requestFrame(v);}
                    @Override public void onPageFinished(WebView v,String url){if(v!=web||disposed||WorkspaceDocumentation.https(url)==null)return;address=url;if(back!=null){back.setEnabled(v.canGoBack());back.setAlpha(v.canGoBack()?1f:.4f);}if(restoreScroll){restoreScroll=false;v.post(()->{if(web==v&&!disposed)v.scrollTo(scrollX,scrollY);});}message();requestFrame(v);}
                    @Override public void onReceivedError(WebView v,WebResourceRequest req,WebResourceError e){if(v==web&&req.isForMainFrame())failed();}
                    @Override public void onReceivedHttpError(WebView v,WebResourceRequest req,WebResourceResponse response){if(v==web&&req.isForMainFrame()&&response.getStatusCode()>=400)failed();}
                    @Override public void onReceivedSslError(WebView v,SslErrorHandler handler,SslError e){handler.cancel();if(v==web)failed();}
                    @Override public void onReceivedHttpAuthRequest(WebView v,HttpAuthHandler h,String host,String realm){h.cancel();if(v==web)failed();}
                    @Override public boolean onRenderProcessGone(WebView v,RenderProcessGoneDetail detail){if(v!=web)return true;remember();destroyWeb();error=tr("Просмотрщик остановлен. Закройте и снова откройте документацию или используйте браузер.","The viewer stopped. Close and reopen documentation, or use a browser.");message();return true;}
                });
                // Loading/restoring starts only after the visible WebView has a viewport.

            }catch(RuntimeException|LinkageError unavailable){destroyWeb();error=tr("Android WebView недоступен. Откройте документацию в браузере кнопкой сверху.","Android WebView is unavailable. Use the browser button above.");message();}
        }
        private void cancelActivation(){if(web!=null&&pendingActivation!=null)web.removeCallbacks(pendingActivation);pendingActivation=null;}
        private void scheduleActivation(){
            cancelActivation();final WebView v=web;final Dialog opened=dialog;if(v==null||opened==null)return;
            pendingActivation=()->{pendingActivation=null;if(web==v&&dialog==opened)activateWeb();};v.post(pendingActivation);
        }
        private void activateWeb(){
            WebView v=web;
            if(disposed||v==null||dialog==null||!dialog.isShowing()||!v.isAttachedToWindow()||v.getWidth()<=0||v.getHeight()<=0)return;
            boolean resumed=!webResumed;if(resumed){webResumed=true;v.onResume();}
            if(requestedUrl!=null){String next=requestedUrl;requestedUrl=null;pendingInitial=false;restoreScroll=false;v.loadUrl(next);}
            else if(pendingInitial){
                pendingInitial=false;
                Page page=PAGES.get(pageKey);boolean restored=false;
                if(page!=null&&WorkspaceDocumentation.https(page.url)!=null){
                    address=page.url;scrollX=page.x;scrollY=page.y;restoreScroll=true;
                    if(page.history!=null)try{WebBackForwardList list=v.restoreState(new Bundle(page.history));restored=list!=null&&list.getSize()>0;}catch(RuntimeException unsupportedHistory){/* Restore URL/scroll if history is unsupported. */}
                }
                if(!restored)v.loadUrl(address);
            }
            if(resumed)requestFrame(v);
        }
        private void requestFrame(WebView v){
            if(v!=web||disposed||dialog==null||!dialog.isShowing()||!v.isAttachedToWindow()||v.getWidth()<=0||v.getHeight()<=0)return;
            final long epoch=++frameEpoch;
            v.postVisualStateCallback(epoch,new WebView.VisualStateCallback(){@Override public void onComplete(long requestId){if(v==web&&!disposed&&dialog!=null&&dialog.isShowing()&&webResumed&&epoch==frameEpoch&&v.isAttachedToWindow())v.postInvalidateOnAnimation();}});
        }
        private void closeWindow(Dialog closing){
            if(dialog!=closing)return;
            remember();cancelActivation();dialog=null;frameEpoch++;
            if(web!=null){if(webResumed)web.onPause();webResumed=false;if(web.getParent()!=null)((ViewGroup)web.getParent()).removeView(web);}
            // Keep the retained page's network load intact; stopLoading here would
            // leave an early-closed article permanently partial on reopening.
            activity.getWindow().getDecorView().removeOnLayoutChangeListener(resize);root=null;webHost=null;status=null;progress=null;search=null;back=null;ru=null;en=null;site=null;
        }
        private boolean navigate(String url,boolean gesture){return WorkspaceDocumentation.https(url)==null;}
        private void failed(){error=tr("Не удалось загрузить страницу. Проверьте интернет, обновите её или откройте в браузере.","Could not load the page. Check the connection, reload, or open it in a browser.");message();if(progress!=null)progress.setVisibility(View.GONE);}
        private void message(){if(status==null)return;if(site!=null)site.setText(Uri.parse(address).getHost());String note=source.fallback(language)?tr("Русского раздела у источника нет: открыт английский оригинал.","This source has no Russian section; showing the English original."):tr("Официальный сайт · страницы обновляются без обновления приложения","Official website · pages update independently of the app");if(!source.owns(address))note=tr("Веб-страница · ","Web page · ")+Uri.parse(address).getHost();status.setText(error.isEmpty()?note:error);}
        private void find(){if(web==null||search==null)return;String q=search.getText().toString();if(source==WorkspaceDocumentation.WIKI){address=WorkspaceDocumentation.search(q,language);error="";web.loadUrl(address);}else web.findAllAsync(q);InputMethodManager ime=(InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);if(ime!=null)ime.hideSoftInputFromWindow(search.getWindowToken(),0);}
        private void visit(String url){if(WorkspaceDocumentation.https(url)==null)return;address=url;error="";requestedUrl=url;message();activateWeb();scheduleActivation();}
        private void back(){if(web!=null&&web.canGoBack())web.goBack();}
        private void external(String url){if(WorkspaceDocumentation.https(url)!=null&&root!=null)AgentMarkdown.openLink(root,url);}
        private void remember(){if(web==null||pageKey.isEmpty()||WorkspaceDocumentation.https(address)==null)return;Page p=new Page();p.history=new Bundle();p.url=address;try{if(requestedUrl==null)web.saveState(p.history);else p.history=null;p.x=web.getScrollX();p.y=web.getScrollY();}catch(RuntimeException unavailable){p.history=null;}PAGES.put(pageKey,p);}
        private void destroyWeb(){if(web==null)return;cancelActivation();WebView old=web;web=null;requestedUrl=null;pendingInitial=false;webResumed=false;frameEpoch++;old.removeOnLayoutChangeListener(viewport);if(old.getParent()!=null)((ViewGroup)old.getParent()).removeView(old);try{old.stopLoading();old.setWebChromeClient(null);old.setWebViewClient(null);}catch(RuntimeException unavailable){}finally{try{old.destroy();}catch(RuntimeException unavailable){}}}
        private void size(){if(dialog==null||dialog.getWindow()==null)return;View decor=activity.getWindow().getDecorView();int w=decor.getWidth(),h=decor.getHeight();if(w<=0||h<=0){w=activity.getResources().getDisplayMetrics().widthPixels;h=activity.getResources().getDisplayMetrics().heightPixels;}if(w==lastWidth&&h==lastHeight)return;lastWidth=w;lastHeight=h;dialog.getWindow().setLayout(Math.max(1,w-dp(16)),Math.max(1,h-dp(32)));}
        private LinearLayout row(){LinearLayout row=new LinearLayout(activity);row.setGravity(Gravity.CENTER_VERTICAL);return row;}
        private TextView label(String text,int size){TextView t=new TextView(activity);t.setText(text);t.setTextSize(size);t.setTextColor(WorkspaceDialogs.TEXT);t.setPadding(dp(6),dp(4),dp(6),dp(4));return t;}
        private TextView languageButton(LinearLayout parent,String name,String code){TextView b=label(name,12);b.setTag("docs-language-"+code);b.setGravity(Gravity.CENTER);b.setBackground(WorkspaceDialogs.background(activity,code.equals(language)));b.setOnClickListener(v->{if(!code.equals(language)){remember();destroyWeb();language=code;languagePicked=true;pageKey="";show(source);}});parent.addView(b,new LinearLayout.LayoutParams(dp(44),dp(36)));return b;}
        private TextView icon(LinearLayout parent,int drawable,String desc,String tag,Runnable action){WorkspaceIconButton b=new WorkspaceIconButton(activity);b.setTextColor(WorkspaceDialogs.TEXT);b.setIcon(activity.getDrawable(drawable).mutate(),dp(22));b.setTag(tag);b.setContentDescription(desc);b.setBackground(WorkspaceDialogs.background(activity,false));b.setOnClickListener(v->action.run());LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(40),dp(40));lp.setMargins(dp(2),0,dp(2),0);parent.addView(b,lp);return b;}
        void dispose(){if(disposed)return;disposed=true;remember();if(dialog!=null){Dialog previous=dialog;closeWindow(previous);previous.dismiss();}destroyWeb();activity.getWindow().getDecorView().removeOnLayoutChangeListener(resize);activity.getApplication().unregisterActivityLifecycleCallbacks(this);}
        @Override public void onActivityDestroyed(Activity a){if(a==activity)WorkspaceDocumentationUi.dispose(a);}
        @Override public void onActivityPaused(Activity a){if(a==activity&&web!=null&&webResumed){web.onPause();webResumed=false;frameEpoch++;}}
        @Override public void onActivityResumed(Activity a){if(a==activity)activateWeb();}
        @Override public void onActivityCreated(Activity a,Bundle b){}@Override public void onActivityStarted(Activity a){}@Override public void onActivityStopped(Activity a){}@Override public void onActivitySaveInstanceState(Activity a,Bundle b){}
    }
}
