package com.termux.app.workspace;

import android.app.AlertDialog;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.termux.R;
import com.termux.app.TermuxActivity;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/** Shared Android chat surface; each provider retains its own native bridge and permissions. */
final class AgentChatUi {
    interface Transport {JSONObject request(WorkspaceStore.Session p,JSONObject q)throws Exception;}
    private final TermuxActivity activity;
    private final Supplier<WorkspaceStore.Session> current;
    private final BooleanSupplier visible;
    private final Transport transport;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ExecutorService reads=Executors.newSingleThreadExecutor(),actions=Executors.newSingleThreadExecutor();
    private final FrameLayout surface;
    private final AgentChatView view;
    private final ChatAttachmentUi attachments;private boolean attachmentsSupported,codexAuthRequired,codexLoginTerminal;
    boolean hasAttachments(){return attachments.has();}
    void applyPresentationScale(){WorkspaceStore.Session p=current.get();view.scale(p==null?100:p.chatScale);if(composerArea!=null)AgentChatScale.apply(composerArea,selected()&&terminal==null&&view.getVisibility()==View.VISIBLE&&p!=null?p.chatScale:100);}
    private final LinkedHashMap<String,JSONObject> rows=new LinkedHashMap<>();
    private final AgentChatCache cache=new AgentChatCache();
    private boolean connected,transportFailed;
    private String owner="",older="",requestStamp="",currentModel="",nativeSession="",actionNotice="";
    void modelChanged(WorkspaceStore.Session p){if(key(p).equals(owner))currentModel="";}
    String modelLabel(WorkspaceStore.Session p){return key(p).equals(owner)&&!currentModel.isEmpty()?currentModel:AiAgentsUi.sessionModelLabel(p,null);}
    private volatile boolean closed;private boolean olderQueued,historyExhausted,loading,nativeChat,olderLoading,wasVisible,resolved,presented,busy,directShell,requiresTerminal;
    private volatile int generation;
    private AlertDialog terminal,details,formDialog,vertexDialog;
    private String vertexPrompted="";
    private TextView thinkingBody;private ScrollView thinkingScroll;private String thinkingKey="";
    private ViewGroup terminalParent;
    private ViewGroup.LayoutParams terminalParams;
    private Runnable visibilityChanged=()->{};
    void onVisibilityChanged(Runnable listener){visibilityChanged=listener;}
    private final Map<String,String> shellDrafts=new HashMap<>();
    private boolean extraVisible;
    private final LinkedHashMap<String,Boolean> extraByOwner=new LinkedHashMap<>();
    private View mountedKeys;private ViewGroup keysHome;private ViewGroup.LayoutParams keysHomeParams;private int keysHomeIndex,keysHomeVisibility;
    boolean extraKeysVisible(){return extraVisible;}
    void toggleExtraKeys(){extraVisible=!extraVisible;sync();}
    private void restoreChatKeys(){if(mountedKeys!=null){ViewGroup parent=(ViewGroup)mountedKeys.getParent();if(parent!=null)parent.removeView(mountedKeys);if(keysHome!=null)keysHome.addView(mountedKeys,Math.min(keysHomeIndex,keysHome.getChildCount()),keysHomeParams);mountedKeys.setVisibility(keysHomeVisibility);mountedKeys=null;keysHome=null;}view.keysHost.setVisibility(View.GONE);}
    private void layoutChatKeys(){
        View keys=activity.getExtraKeysView();if(!showing()||!extraVisible||keys!=mountedKeys)restoreChatKeys();
        if(showing()&&extraVisible&&keys!=null&&mountedKeys==null){mountedKeys=keys;keysHome=(ViewGroup)keys.getParent();keysHomeParams=keys.getLayoutParams();keysHomeIndex=keysHome==null?0:keysHome.indexOfChild(keys);keysHomeVisibility=keys.getVisibility();if(keysHome!=null)keysHome.removeView(keys);keys.setVisibility(View.VISIBLE);int count=activity.getProperties()==null||activity.getProperties().getExtraKeysInfo()==null?2:activity.getProperties().getExtraKeysInfo().getMatrix().length;view.keysHost.addView(keys,new LinearLayout.LayoutParams(-1,view.dp(Math.max(1,count)*40)));view.keysHost.setVisibility(View.VISIBLE);}
    }
    private EditText composer;
    private View composerArea;
    void composer(EditText input){composer(input,input);}
    void composer(EditText input,View area){composer=input;composerArea=area;}
    private com.termux.shared.terminal.io.extrakeys.ExtraKeysView chatKeys;
    private com.termux.shared.terminal.io.extrakeys.ExtraKeysView.IExtraKeysView previousKeys;
    private void syncExtraKeys(){
        com.termux.shared.terminal.io.extrakeys.ExtraKeysView keys=activity.getExtraKeysView();
        if(chatKeys!=null&&(!showing()||chatKeys!=keys)){chatKeys.setExtraKeysViewClient(previousKeys);chatKeys=null;previousKeys=null;}
        if(showing()&&keys!=null&&chatKeys==null){chatKeys=keys;previousKeys=keys.getExtraKeysViewClient();keys.setExtraKeysViewClient(new ChatExtraKeys(activity.getTerminalView(),keys,()->showing()?(view.shellInput.hasFocus()&&view.shellInput.isEnabled()?view.shellInput:composer):null));}
    }
    private Consumer<String> recordShell=s->{};void onShellCommand(Consumer<String> listener){recordShell=listener;}
    private final Map<String,String> sendIDs=new HashMap<>();
    AgentChatUi(TermuxActivity a,Supplier<WorkspaceStore.Session> c,BooleanSupplier v){this(a,c,v,(p,q)->p.environment.equals("codex")?CodexChatControl.request(a.getFilesDir(),p,q):p.claudeAgent?ClaudeLiveControl.request(a.getFilesDir(),p.claudeSocketId,q):p.geminiAgent?GeminiLiveControl.request(a.getFilesDir(),p.geminiSocketId,q):AiAgents.liveRequest(a,p,q));}
    AgentChatUi(TermuxActivity a,Supplier<WorkspaceStore.Session> c,BooleanSupplier v,Transport t){
        activity=a;current=c;visible=v;transport=t;surface=a.findViewById(R.id.workspace_terminal_surface);
        view=new AgentChatView(a,this::details,this::older,this::sendShell,this::terminal,this::interrupt);
        attachments=new ChatAttachmentUi(a,view);view.thumbnail=attachments::thumbnail;
        view.assistantName("OpenCode");view.shellEnabled(activity.getSharedPreferences("workspace-input",android.content.Context.MODE_PRIVATE).getBoolean("interactive-input",false));
        view.setVisibility(View.GONE);surface.addView(view,new FrameLayout.LayoutParams(-1,-1));handler.post(poll);
    }
    static JSONObject op(String name){JSONObject q=new JSONObject();try{q.put("op",name);}catch(JSONException impossible){throw new IllegalStateException(impossible);}return q;}
    private boolean eligible(){WorkspaceStore.Session p=current.get();return !closed&&p!=null&&(p.aiAgent.equals("opencode")||p.geminiAgent||p.claudeAgent||CodexChatControl.enabled(activity.getFilesDir(),p));}
    private boolean selected(){return eligible()&&visible.getAsBoolean();}
    private String key(WorkspaceStore.Session p){return p.id+":"+(p.environment.equals("codex")?"codex:"+p.codexSocketId:p.claudeAgent?"claude:"+p.claudeSocketId+":"+p.claudeThread:p.geminiAgent?p.geminiSocketId+":"+p.geminiThread:p.aiSocketId+":"+p.aiThread)+":"+p.handle;}
    private WorkspaceStore.Session snapshot(){WorkspaceStore.Session p=current.get();WorkspaceStore.Session s=new WorkspaceStore.Session();s.environment=p.environment;s.codexEnvironment=p.codexEnvironment;s.codexSocketId=p.codexSocketId;s.id=p.id;s.aiThread=p.aiThread;s.aiSocketId=p.aiSocketId;s.aiAgent=p.aiAgent;s.aiConnection=p.aiConnection;s.aiModel=p.aiModel;s.aiVariant=p.aiVariant;s.geminiAgent=p.geminiAgent;s.geminiSocketId=p.geminiSocketId;s.geminiThread=p.geminiThread;s.geminiModel=p.geminiModel;s.claudeAgent=p.claudeAgent;s.claudeSocketId=p.claudeSocketId;s.claudeThread=p.claudeThread;s.claudeModel=p.claudeModel;s.claudeMode=p.claudeMode;return s;}
    boolean handlesComposer(){return selected()&&terminal==null&&(nativeChat||!resolved);}
    String name(){WorkspaceStore.Session p=current.get();return p!=null&&p.environment.equals("codex")?"Codex":p!=null&&p.claudeAgent?"Claude":p!=null&&p.geminiAgent?"Gemini":"OpenCode";}
    boolean structured(){return selected()&&nativeChat;}
    boolean terminalAvailable(){return selected();}
    boolean requiresTerminal(){return showing()&&requiresTerminal;}
    boolean working(){return showing()&&busy;}
    boolean canInterrupt(){return !closed&&selected()&&nativeChat&&busy;}
    void stopCurrent(){if(canInterrupt())interrupt();}
    boolean showing(){return selected()&&nativeChat&&terminal==null;}
    private java.util.function.BooleanSupplier externalDisplay=()->false;
    void externalDisplay(java.util.function.BooleanSupplier value){externalDisplay=value;}
    void sync(){
        String next=eligible()?key(current.get()):"";
        boolean nowVisible=selected();if(wasVisible&&!nowVisible){generation++;loading=false;olderLoading=false;olderQueued=false;view.olderState(false,null);if(terminal!=null)terminal.dismiss();if(details!=null)details.dismiss();if(formDialog!=null)formDialog.dismiss();if(vertexDialog!=null)vertexDialog.dismiss();}wasVisible=nowVisible;
        if(!next.equals(owner)){
            if(!owner.isEmpty()){extraByOwner.put(owner,extraVisible);while(extraByOwner.size()>32)extraByOwner.remove(extraByOwner.keySet().iterator().next());}
            restoreChatKeys();extraVisible=extraByOwner.getOrDefault(next,false);
            if(!owner.isEmpty()){shellDrafts.put(owner,view.shellInput.getText().toString());savePresentation();}
            generation++;attachmentsSupported=false;codexAuthRequired=false;codexLoginTerminal=false;owner=next;actionNotice="";currentModel="";nativeSession="";connected=false;transportFailed=false;
            view.assistantName(name());view.actionCards(current.get()!=null&&!current.get().environment.equals("codex"));view.shellInput.setText(shellDrafts.getOrDefault(next,""));nativeChat=false;resolved=false;loading=false;olderLoading=false;olderQueued=false;busy=false;directShell=false;requiresTerminal=false;rows.clear();older="";historyExhausted=false;requestStamp="";view.clear();
            attachments.sync(owner,current.get()==null?null:snapshot(),false);
            AgentChatCache.State saved=cache.take(next);
            if(saved==null)view.connection(next.isEmpty()?"":tr("Подключение…","Connecting…"));else restorePresentation(saved);
            if(terminal!=null)terminal.dismiss();if(details!=null)details.dismiss();if(formDialog!=null)formDialog.dismiss();if(vertexDialog!=null)vertexDialog.dismiss();
        }
        applyPresentationScale();attachments.sync(owner,current.get()==null?null:snapshot(),attachmentsSupported);
        boolean cover=showing()||(selected()&&!resolved&&terminal==null);
        boolean changed=(view.getVisibility()==View.VISIBLE)!=cover||presented!=showing();presented=showing();
        view.setVisibility(cover?View.VISIBLE:View.GONE);applyPresentationScale();
        // show(TERMINAL) may restore the shared toolbar even without a chat visibility
        // transition. Enforce the structured-chat boundary on every synchronization.
        if((cover||terminal!=null)&&activity.getTerminalToolbarViewPager()!=null)activity.getTerminalToolbarViewPager().setVisibility(View.GONE);
        activity.getTerminalView().setVisibility(cover||externalDisplay.getAsBoolean()?View.INVISIBLE:View.VISIBLE);
        layoutChatKeys();syncExtraKeys();if(changed)visibilityChanged.run();
        activity.getTerminalView().setWorkspaceTerminalVisible(!closed&&visible.getAsBoolean()&&!cover&&!externalDisplay.getAsBoolean());
    }
    private void savePresentation(){
        if(!resolved&&!nativeChat)return;
        AgentChatCache.State state=new AgentChatCache.State();state.rows.putAll(rows);state.historyExhausted=historyExhausted;state.older=older;state.model=currentModel;state.session=nativeSession;state.notice=actionNotice;state.nativeChat=nativeChat;state.resolved=resolved;state.busy=busy;state.directShell=directShell;state.needsTerminal=requiresTerminal;state.connected=connected;state.failed=transportFailed;state.position=view.position();cache.put(owner,state);
    }
    private void restorePresentation(AgentChatCache.State state){
        rows.putAll(state.rows);historyExhausted=state.historyExhausted&&!state.trimmed;older=state.trimmed?"":state.older;currentModel=state.model;nativeSession=state.session;actionNotice=state.notice;nativeChat=state.nativeChat;resolved=state.resolved;busy=state.busy;directShell=state.directShell;requiresTerminal=state.needsTerminal;connected=state.connected;transportFailed=state.failed;
        view.rows(new ArrayList<>(rows.values()),!older.isEmpty());if(transportFailed)connectionFailure();else{JSONObject status=op("chat-snapshot");try{status.put("connected",connected).put("needsTerminal",requiresTerminal);}catch(JSONException impossible){throw new IllegalStateException(impossible);}connectionState(status);}view.restorePosition(state.position);
        // Pending permissions/forms are intentionally rebuilt only from a fresh native snapshot.
    }
    private void connectionFailure(){if(vertexSetup())return;view.connection(tr("Нет связи с сессией. Повторяем подключение автоматически.\nЕсли ожидание затянулось, откройте терминал кнопкой робота и проверьте запуск агента. Нажмите здесь, чтобы проверить снова.","No connection to the session. Retrying automatically.\nIf this persists, open the robot terminal and check that the agent started. Tap here to check again."),this::retryConnection);}
    private final Runnable poll=new Runnable(){public void run(){if(closed)return;sync();if(selected()&&!vertexSetup()&&!loading&&(terminal==null||codexLoginTerminal))load(false);handler.postDelayed(this,nativeChat&&!busy?2000:500);}};
    private boolean vertexSetup(){
        WorkspaceStore.Session p=current.get();
        if(connected)return false;
        if(!selected()||!VertexConnection.pending(activity,p)||activity.getCurrentSession()==null||!activity.getCurrentSession().isRunning()){if(vertexDialog!=null&&vertexDialog.isShowing())vertexDialog.dismiss();return false;}
        if(terminal!=null)terminal.dismiss();
        if(VertexConnection.file(activity,p,"response").isFile()){view.connection(tr("Параметры Vertex сохранены. Продолжаем подключение…","Vertex settings saved. Continuing connection…"));return true;}
        String run=key(p);
        Runnable open=()->{if(vertexDialog!=null&&vertexDialog.isShowing())return;vertexDialog=VertexConnection.prompt(activity,p,()->!closed&&selected()&&key(current.get()).equals(run),()->{view.connection(tr("Параметры Vertex сохранены. Продолжаем подключение…","Vertex settings saved. Continuing connection…"));});};
        view.connection(tr("Учётные данные Google Cloud найдены. Укажите ID облачного проекта и регион для Vertex AI. Нажмите здесь, чтобы продолжить настройку.","Google Cloud credentials were found. Enter a cloud project ID and location for Vertex AI. Tap here to continue setup."),open);
        if(!vertexPrompted.equals(run)){vertexPrompted=run;open.run();}
        return true;
    }
    private void sendShell(){if(!view.shellInput.isEnabled())return;String command=view.shellInput.getText().toString();if(command.trim().isEmpty())return;submit(command,"shell",()->{recordShell.accept(command);if(view.shellInput.getText().toString().equals(command))view.shellInput.setText("");});}
    void shellMode(boolean enabled){if(view.shellInput.isEnabled()!=enabled)view.shellEnabled(enabled);}
    void interactive(boolean enabled){
        shellMode(enabled);
        activity.getTerminalView().setSoftKeyboardInputEnabled(enabled);
        View target=terminal!=null?activity.getTerminalView():enabled?view.shellInput:null;
        android.view.inputmethod.InputMethodManager keyboard=(android.view.inputmethod.InputMethodManager)activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if(target!=null&&enabled){target.requestFocus();keyboard.restartInput(target);keyboard.showSoftInput(target,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);}
        else keyboard.hideSoftInputFromWindow(view.getWindowToken(),0);
    }
    boolean focusRequest(){return focus(view.requests);}
    private boolean focus(View v){if(v instanceof ViewGroup){ViewGroup group=(ViewGroup)v;for(int i=0;i<group.getChildCount();i++)if(focus(group.getChildAt(i)))return true;}else if(v.isClickable()&&v instanceof TextView&&!((TextView)v).getText().toString().contains("\n")){v.setFocusableInTouchMode(true);return v.requestFocus();}return false;}
    private void interrupt(){
        if(!selected())return;
        final int g=generation;final String owner=key(current.get());JSONObject q=op("chat-interrupt");try{q.put("session",nativeSession);}catch(JSONException ignored){}
        Runnable send=()->{if(selected()&&g==generation&&owner.equals(key(current.get())))operation(q,null);};
        if(current.get().environment.equals("codex")){send.run();return;}
        if(directShell)WorkspaceDialogs.builder(activity).setMessage(tr("Прервать команду? Штатный OpenCode удалит её процесс и сохранённый вывод; частичный вывод может стать недоступен.","Interrupt command? Native OpenCode removes its process and retained output; partial output may become unavailable.")).setPositiveButton(tr("Прервать","Interrupt"),(d,w)->send.run()).setNegativeButton(tr("Назад","Back"),null).show();else send.run();
    }
    private String tr(String ru,String en){return WorkspaceLanguage.code(activity).equals("ru")?ru:en;}
    private void load(boolean previous){
        if(!selected()||loading)return;loading=true;if(previous){olderLoading=true;view.olderState(true,null);}final int g=generation;final WorkspaceStore.Session p=snapshot();final JSONObject q=op("chat-snapshot");
        try{if(previous)q.put("cursor",older);}catch(JSONException ignored){}
        reads.execute(()->{JSONObject result=null;try{result=transport.request(p,q);}catch(Exception ignored){try{JSONObject status=transport.request(p,op("status"));if(status.optBoolean("ok")&&!status.optBoolean("nativeChat"))result=status;}catch(Exception unavailable){}}final JSONObject r=result;
            handler.post(()->{if(closed||g!=generation)return;loading=false;olderLoading=false;try{if(previous&&r!=null&&r.optBoolean("historyReset")){Object cursor=r.opt("older");older=cursor instanceof String?(String)cursor:"";historyExhausted=older.isEmpty();view.rows(new ArrayList<>(rows.values()),!older.isEmpty());view.olderState(false,tr("История обновилась. Загрузить предыдущие","History refreshed. Load earlier"));return;}if(previous&&(r==null||!r.optBoolean("nativeChat"))){view.olderState(false,tr("Не удалось загрузить. Повторить","Could not load. Retry"));return;}if(r==null||!r.optBoolean("nativeChat")){if(r!=null){resolved=true;nativeChat=false;connected=false;sync();}if(r==null){transportFailed=true;connected=false;connectionFailure();}else view.connection("");return;}
                codexAuthRequired=r.optBoolean("authRequired");transportFailed=false;attachmentsSupported=r.optBoolean("attachmentsSupported");connected=r.optBoolean("connected");String actualSession=r.optString("session");if((p.geminiAgent||p.claudeAgent||p.environment.equals("codex")||p.aiAgent.equals("opencode"))&&!actualSession.equals(nativeSession)){if(!nativeSession.isEmpty()){generation++;olderQueued=false;view.olderState(false,null);actionNotice="";rows.clear();older="";historyExhausted=false;requestStamp="";view.clear();if(details!=null)details.dismiss();if(formDialog!=null)formDialog.dismiss();if(vertexDialog!=null)vertexDialog.dismiss();}nativeSession=actualSession;}
                String model=AiAgentsUi.sessionModelLabel(p,r.optJSONObject("model"));boolean modelChanged=!model.equals(currentModel);currentModel=model;boolean changed=modelChanged||requiresTerminal!=r.optBoolean("needsTerminal");requiresTerminal=r.optBoolean("needsTerminal");nativeChat=true;resolved=true;busy=r.optBoolean("busy");directShell=r.optBoolean("directShell");sync();layoutChatKeys();syncExtraKeys();if(changed)visibilityChanged.run();
                java.util.Set<String> known=null;if((p.geminiAgent||p.claudeAgent||p.environment.equals("codex")||p.aiAgent.equals("opencode"))&&r.has("knownMessages")&&!r.optBoolean("partialHistory")){known=new java.util.HashSet<>();JSONArray ids=r.optJSONArray("knownMessages");if(ids!=null)for(int i=0;i<ids.length();i++)known.add(ids.optString(i));}
                JSONArray list=r.optJSONArray("rows");LinkedHashMap<String,JSONObject> updated=new LinkedHashMap<>();if(previous&&list!=null)for(int i=0;i<list.length();i++){JSONObject row=list.optJSONObject(i);if(row!=null&&!rows.containsKey(row.optString("key")))updated.put(row.optString("key"),row);}if(previous)updated.putAll(rows);else{Set<String> refreshed=new HashSet<>();if(list!=null)for(int i=0;i<list.length();i++)refreshed.add(list.optJSONObject(i).optString("message"));for(Map.Entry<String,JSONObject> e:rows.entrySet())if((known==null||known.contains(e.getValue().optString("message")))&&!refreshed.contains(e.getValue().optString("message"))&&!e.getValue().optBoolean("queued"))updated.put(e.getKey(),e.getValue());}
                if(!previous&&list!=null)for(int i=0;i<list.length();i++){JSONObject row=list.optJSONObject(i);if(row!=null)updated.put(row.optString("key"),row);}
                rows.clear();rows.putAll(updated);if(previous||older.isEmpty()&&!historyExhausted){Object cursor=r.opt("older");older=cursor instanceof String?(String)cursor:"";if(previous)historyExhausted=older.isEmpty();}connectionState(r);if(previous){view.olderRows(new ArrayList<>(rows.values()),!older.isEmpty());view.olderState(false,null);}else view.rows(new ArrayList<>(rows.values()),!older.isEmpty());requests(r.optJSONObject("permission"),r.optJSONArray("forms"),r.optBoolean("needsTerminal"));if(p.environment.equals("codex")&&codexLoginTerminal&&!codexAuthRequired&&connected&&terminal!=null)terminal.dismiss();
                JSONObject thought=rows.get(thinkingKey);if(details!=null&&details.isShowing()&&thinkingBody!=null&&thought!=null&&!thought.optBoolean("more")&&thinkingBody.getSelectionStart()==thinkingBody.getSelectionEnd()&&!thinkingBody.getText().toString().equals(thought.optString("body"))){boolean bottom=thinkingBody.getHeight()-thinkingScroll.getHeight()-thinkingScroll.getScrollY()<view.dp(48);thinkingBody.setText(thought.optString("body"));if(bottom){ScrollView held=thinkingScroll;held.post(()->held.fullScroll(View.FOCUS_DOWN));}}
                }finally{if(!closed&&g==generation&&olderQueued){olderQueued=false;if(selected()&&!older.isEmpty())load(true);else view.olderState(false,null);}}
            });});
    }
    private void retryConnection(){if(!loading)load(false);}
    private void connectionState(JSONObject r){
        if(current.get().environment.equals("codex")&&r.optBoolean("authRequired")){
            view.connection(r.optBoolean("loginPending")?tr("Вход ожидает подтверждения в браузере. Нажмите, чтобы открыть окно входа.","Sign-in awaits browser confirmation. Tap to open sign-in."):r.optBoolean("loginError")?tr("Вход не завершён. Нажмите, чтобы повторить вход в Codex.","Sign-in did not complete. Tap to retry Codex sign-in."):tr("Войти в Codex — нажмите для подключения аккаунта.","Sign in to Codex — tap to connect your account."),this::terminal);return;
        }
        if(current.get().geminiAgent&&current.get().geminiAuth.equals("api")&&r.optBoolean("authError")){view.connection(tr("Не удалось войти в Gemini. Проверьте API-ключ в настройках сессии/пресета и перезапустите её. Нажмите для просмотра терминала.","Could not sign in to Gemini. Check the API key in session/preset settings and restart the session. Tap to inspect the terminal."),this::terminal);return;}
        if(r.optBoolean("needsTerminal")){if((current.get().claudeAgent||current.get().environment.equals("codex"))&&!r.optString("connectionNotice").isEmpty()){String notice=r.optString("connectionNotice");view.connection(notice.length()>600?notice.substring(0,600):notice);return;}view.connection(tr("Требуется действие в терминале. Нажмите кнопку робота сверху.","An action is needed in the terminal. Tap the robot button above."));return;}
        if(!actionNotice.isEmpty()){view.connection(actionNotice);return;}
        if(!r.optBoolean("connected")){view.connection(tr("Подключаем чат к сессии. Если ожидание затянулось, проверьте запуск через кнопку робота.","Connecting chat to the session. If this persists, check startup using the robot button."),this::retryConnection);return;}
        view.connection(rows.isEmpty()?tr("Подключено к сессии ","Connected to the ")+name()+tr(". Напишите сообщение, чтобы начать."," session. Send a message to begin."):"");
    }
    private void older(){if(older.isEmpty()||olderLoading||olderQueued)return;view.olderState(true,null);if(loading)olderQueued=true;else load(true);}
    void send(String text,Runnable accepted){if(!showing()){if(selected()&&!resolved)view.connection(tr("Подключение ещё не установлено. Текст сохранён во вводе.","Not connected yet. Your text was kept in the input field."));return;}submit(text,"message",accepted);}
    private void submit(String text,String kind,Runnable accepted){
        if(!showing())return;
        if(kind.equals("message")&&attachments.busy()){Toast.makeText(activity,tr("Дождитесь подготовки вложения.","Wait for the attachment to finish preparing."),Toast.LENGTH_SHORT).show();return;}
        final JSONArray refs=kind.equals("message")?attachments.references():new JSONArray();final String attachmentOwner=owner;
        if(refs.length()>0&&!attachmentsSupported){Toast.makeText(activity,tr("Эта сессия ещё не поддерживает вложения. Перезапустите её с сохранением чата.","This session does not support attachments yet. Restart it with the existing chat."),Toast.LENGTH_LONG).show();return;}WorkspaceStore.Session p=snapshot();final String k=owner+":"+((p.geminiAgent||p.claudeAgent||p.environment.equals("codex")||p.aiAgent.equals("opencode"))?nativeSession+":":"")+kind+":"+text+":"+refs;String id=sendIDs.get(k);if(id==null){id="msg_"+UUID.randomUUID().toString().replace("-","");sendIDs.put(k,id);}
        JSONObject q=op("chat-send");try{q.put("text",text).put("kind",kind).put("id",id);if(refs.length()>0)q.put("attachments",refs);if(p.geminiAgent||p.claudeAgent||p.environment.equals("codex")||p.aiAgent.equals("opencode"))q.put("session",nativeSession);}catch(JSONException ignored){}
        if(refs.length()>0)attachments.submitted(refs);
        operation(q,()->{sendIDs.remove(k);if(refs.length()>0)attachments.accepted(attachmentOwner,refs);view.followAfterSend();accepted.run();});
    }
    private void operation(JSONObject q,Runnable success){operation(q,success,null);}
    private void operation(JSONObject q,Runnable success,Runnable failure){
        if(!selected())return;final WorkspaceStore.Session p=snapshot();final int g=generation;
        actions.execute(()->{if(closed||g!=generation)return;boolean ok=false;try{JSONObject r=transport.request(p,q);ok=r.optBoolean("ok");}catch(Exception ignored){}final boolean done=ok;
            handler.post(()->{if(closed||g!=generation)return;if(done){actionNotice="";if(success!=null)success.run();requestStamp="";if(!loading)load(false);}else {actionNotice=tr("Агент не подтвердил действие: он может запускаться, ожидать выбора в терминале или быть недоступен. Текст остался в поле ввода.\nПроверьте терминал кнопкой робота. Перед повтором убедитесь, что действие не выполнено; сообщение можно отправить ещё раз.","The agent did not confirm the action: it may be starting, waiting for a terminal choice or unavailable. Your text remains in the input field.\nCheck the robot terminal. Before retrying, check that the action did not run; you can send the message again.");view.connection(actionNotice);if(failure!=null)failure.run();}});});
    }
    private void details(JSONObject row){
        if(!selected())return;if(row.optString("kind").equals("attachment")&&row.optJSONObject("attachment")!=null){attachments.open(row.optJSONObject("attachment"));return;}if(details!=null)details.dismiss();thinkingBody=null;thinkingScroll=null;thinkingKey="";LinearLayout box=new LinearLayout(activity);box.setOrientation(LinearLayout.VERTICAL);TextView body=view.label(row.optString("body"),14);body.setTextIsSelectable(true);if((row.optString("toolType").equals("contextCompaction")||row.optString("part").equals("context"))&&!row.optBoolean("more")){
            body.setPadding(view.dp(20),view.dp(14),view.dp(20),view.dp(18));
            details=WorkspaceDialogs.builder(activity).setTitle(CodexActionLabels.title(row,WorkspaceLanguage.code(activity).equals("ru"))).setView(body).create();details.setCanceledOnTouchOutside(true);details.show();
            int width=Math.min(Math.round(activity.getResources().getDisplayMetrics().widthPixels*.94f),Math.max(view.dp(240),Math.round(body.getPaint().measureText(row.optString("body")))+view.dp(64)));
            details.getWindow().setLayout(width,android.view.ViewGroup.LayoutParams.WRAP_CONTENT);return;
        }
        final AgentMarkdownView markdownBody=row.optString("kind").equals("text")?new AgentMarkdownView(activity):null;if(markdownBody!=null)markdownBody.update(name(),row.optString("body"),null);
        ScrollView scroll=new ScrollView(activity);scroll.addView(markdownBody==null?body:markdownBody);box.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        TextView next=view.pill(tr("Загрузить полный текст","Load full text"),()->{});box.addView(next);next.setVisibility(row.optBoolean("more")?View.VISIBLE:View.GONE);
        details=WorkspaceDialogs.builder(activity).setTitle(row.optString("kind").equals("reasoning")?tr("Рассуждение","Thinking"):CodexActionLabels.title(row,WorkspaceLanguage.code(activity).equals("ru"))).setView(box).setPositiveButton(tr("Назад","Back"),null).create();details.show();details.getWindow().setLayout(-1,Math.round(activity.getResources().getDisplayMetrics().heightPixels*.72f));
        if(row.optString("kind").equals("reasoning")){thinkingBody=body;thinkingScroll=scroll;thinkingKey=row.optString("key");}
        final AlertDialog dialog=details;final int[] offset={0};final int g=generation;final WorkspaceStore.Session p=snapshot();
        Runnable[] page=new Runnable[1];page[0]=()->{next.setEnabled(false);JSONObject q=op("chat-detail");try{if(p.geminiAgent||p.claudeAgent||p.environment.equals("codex")||p.aiAgent.equals("opencode"))q.put("session",nativeSession);q.put("message",row.optString("message")).put("part",row.optString("part")).put("offset",offset[0]);}catch(JSONException ignored){}
            reads.execute(()->{JSONObject r=null;try{r=transport.request(p,q);}catch(Exception ignored){}final JSONObject result=r;handler.post(()->{if(closed||g!=generation||!dialog.isShowing())return;next.setEnabled(true);if(result==null){next.setVisibility(View.VISIBLE);next.setText(tr("Повторить загрузку","Retry loading"));return;}if(offset[0]==0)body.setText((row.optString("kind").equals("receipt")?tr("Разрешение не означает успешного выполнения.\n\n","Permission does not mean successful execution.\n\n"):"")+result.optString("body"));else body.append(result.optString("body"));if(markdownBody!=null)markdownBody.update(name(),body.getText().toString(),null);offset[0]=result.isNull("next")?-1:result.optInt("next",-1);next.setVisibility(offset[0]<0?View.GONE:View.VISIBLE);next.setText(tr("Далее","More"));});});};next.setOnClickListener(v->page[0].run());page[0].run();
    }
    private void requests(JSONObject permission,JSONArray forms,boolean needsTerminal){
        String stamp=String.valueOf(permission)+String.valueOf(forms)+needsTerminal;if(stamp.equals(requestStamp))return;requestStamp=stamp;
        final boolean bottom=view.timeline.getHeight()-view.scroll.getHeight()-view.scroll.getScrollY()<view.dp(48);view.clearRequests();
        if(permission!=null){TextView title=view.label(permission.optString("title")+"\n"+permission.optString("body"),13);title.setMaxLines(5);title.setOnClickListener(v->{if(details!=null)details.dismiss();details=WorkspaceDialogs.builder(activity).setMessage(permission.optString("body")).setPositiveButton(tr("Назад","Back"),null).show();});view.requests.addView(title);LinearLayout buttons=new LinearLayout(activity);
            for(String outcome:new String[]{"proceed_once","cancel"})buttons.addView(view.pill(outcome.equals("cancel")?tr("Нет","No"):tr("Да, разово","Yes, once"),()->{JSONObject q=op("answer");try{q.put("id",permission.optString("id")).put("outcome",outcome);if(current.get().claudeAgent||current.get().environment.equals("codex"))q.put("session",nativeSession).put("nonce",permission.optString("nonce"));}catch(JSONException ignored){}for(int i=0;i<buttons.getChildCount();i++)buttons.getChildAt(i).setEnabled(false);operation(q,()->view.requests.removeAllViews(),()->{if(buttons.getParent()==view.requests)for(int i=0;i<buttons.getChildCount();i++)buttons.getChildAt(i).setEnabled(true);});}));view.requests.addView(buttons);
        }
        if(needsTerminal){view.requests.addView(view.label(tr("Завершите действие в терминале или нажмите «Вернуться в чат ","Finish the action in the terminal or tap Return to ")+name()+tr("»."," chat."),13));view.requests.addView(view.pill(tr("Открыть терминал ›","Open terminal ›"),this::terminal));view.requests.addView(view.pill(returnChatLabel(),this::returnAgentChat));}
        if(forms!=null)for(int i=0;i<forms.length();i++){JSONObject f=forms.optJSONObject(i);if(f!=null)view.requests.addView(view.pill(f.optString("title")+" ▾",()->form(f)));}
        view.scaleContent();
        if(bottom)view.bottomAfterLayout();
    }
    private void form(JSONObject f){
        final int g=generation;LinearLayout box=new LinearLayout(activity);box.setOrientation(LinearLayout.VERTICAL);JSONArray fields=f.optJSONArray("fields");Map<String,Supplier<Object>> inputs=new LinkedHashMap<>();boolean simple=fields!=null;
        if(fields!=null)for(int i=0;i<fields.length();i++){
            JSONObject field=fields.optJSONObject(i);if(field==null)continue;String type=field.optString("type");
            if(field.has("when")||field.optBoolean("hidden")||(type.equals("multiselect")&&field.optBoolean("custom"))||!Arrays.asList("string","boolean","number","integer","multiselect").contains(type)){simple=false;continue;}
            String key=field.optString("key");box.addView(view.label(field.optString("title",key),14));if(field.has("description"))box.addView(view.label(field.optString("description"),12));
            if(type.equals("boolean")){CheckBox check=new CheckBox(activity);check.setTextColor(0xffd5e4e0);check.setText(tr("Да","Yes"));check.setChecked(field.optBoolean("default"));box.addView(check);inputs.put(key,check::isChecked);continue;}
            JSONArray options=field.optJSONArray("options");
            if(type.equals("multiselect")){
                List<CheckBox> checks=new ArrayList<>();List<String> optionValues=new ArrayList<>();if(options!=null)for(int j=0;j<options.length();j++){JSONObject option=options.optJSONObject(j);if(option==null)continue;CheckBox check=new CheckBox(activity);check.setTextColor(0xffd5e4e0);check.setText(option.optString("label"));check.setSaveEnabled(false);box.addView(check);checks.add(check);optionValues.add(option.optString("value"));}
                inputs.put(key,()->{JSONArray selected=new JSONArray();for(int j=0;j<checks.size();j++)if(checks.get(j).isChecked())selected.put(optionValues.get(j));return selected;});continue;
            }
            if(type.equals("string")&&options!=null){
                List<String> labels=new ArrayList<>(),optionValues=new ArrayList<>();for(int j=0;j<options.length();j++){JSONObject option=options.optJSONObject(j);if(option!=null){labels.add(option.optString("label"));optionValues.add(option.optString("value"));}}
                if(labels.isEmpty()){simple=false;continue;}Spinner pick=new Spinner(activity);pick.setAdapter(new ArrayAdapter<>(activity,android.R.layout.simple_spinner_dropdown_item,labels));int initial=optionValues.indexOf(field.optString("default"));if(initial>=0)pick.setSelection(initial);box.addView(pick);
                EditText custom=new EditText(activity);custom.setTextColor(0xffd5e4e0);custom.setSaveEnabled(false);custom.setHint(tr("Свой ответ (необязательно)","Custom answer (optional)"));if(field.optBoolean("custom"))box.addView(custom);
                inputs.put(key,()->custom.length()>0?custom.getText().toString():optionValues.get(pick.getSelectedItemPosition()));continue;
            }
            EditText input=new EditText(activity);input.setTextColor(0xffd5e4e0);input.setSaveEnabled(false);input.setText(field.optString("default"));if(!type.equals("string"))input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_SIGNED|(type.equals("number")?android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL:0));box.addView(input);
            inputs.put(key,()->type.equals("string")?input.getText().toString():type.equals("integer")?Long.valueOf(input.getText().toString()):Double.valueOf(input.getText().toString()));
        }
        if(!simple){formDialog=WorkspaceDialogs.builder(activity).setTitle(f.optString("title")).setMessage(tr("Этот запрос требует штатного интерфейса ","This request needs the native interface: ")+name()).setPositiveButton(tr("Открыть терминал","Open terminal"),(d,w)->{if(g==generation)terminal();}).setNegativeButton(tr("Назад","Back"),null).show();return;}
        ScrollView scroll=new ScrollView(activity);scroll.addView(box);AlertDialog dialog=WorkspaceDialogs.builder(activity).setTitle(f.optString("title")).setView(scroll).setPositiveButton(tr("Ответить","Reply"),null).setNegativeButton(tr("Назад","Back"),null).create();formDialog=dialog;dialog.show();dialog.getButton(-1).setOnClickListener(v->{if(g!=generation){dialog.dismiss();return;}JSONObject q=op("chat-form"),answer=new JSONObject();try{for(Map.Entry<String,Supplier<Object>> e:inputs.entrySet())answer.put(e.getKey(),e.getValue().get());q.put("id",f.optString("id")).put("nonce",f.optString("nonce")).put("answer",answer);if(current.get().claudeAgent||current.get().environment.equals("codex")||current.get().aiAgent.equals("opencode"))q.put("session",nativeSession);}catch(Exception invalid){Toast.makeText(activity,tr("Проверьте заполненные поля.","Check the entered values."),Toast.LENGTH_SHORT).show();return;}operation(q,dialog::dismiss);});
    }
    private com.termux.shared.terminal.io.extrakeys.ExtraKeysView createTerminalKeys(){
        com.termux.shared.terminal.io.extrakeys.ExtraKeysView keys=(com.termux.shared.terminal.io.extrakeys.ExtraKeysView)LayoutInflater.from(activity).inflate(R.layout.view_terminal_toolbar_extra_keys,null,false);
        keys.setExtraKeysViewClient(new com.termux.app.terminal.io.TermuxTerminalExtraKeys(activity.getTerminalView(),activity.getTermuxTerminalViewClient(),activity.getTermuxTerminalSessionClient()));
        com.termux.shared.terminal.io.extrakeys.ExtraKeysInfo info=activity.getProperties()==null?null:activity.getProperties().getExtraKeysInfo();
        if(info==null||info.getMatrix().length==0)try{info=new com.termux.shared.terminal.io.extrakeys.ExtraKeysInfo("[['ESC','/','-','HOME','UP','END','PGUP'],['TAB','CTRL','ALT','LEFT','DOWN','RIGHT','PGDN']]","default",com.termux.shared.terminal.io.extrakeys.ExtraKeysConstants.CONTROL_CHARS_ALIASES);}catch(JSONException impossible){throw new IllegalStateException(impossible);}
        keys.setButtonTextAllCaps(activity.getProperties()==null||activity.getProperties().shouldExtraKeysTextBeAllCaps());keys.reload(info);return keys;
    }
    private String returnChatLabel(){return tr("Вернуться в чат ","Return to ")+name()+tr(""," chat");}
    private void returnAgentChat(){
        if(!selected())return;final WorkspaceStore.Session profile=snapshot();final int g=generation;final AlertDialog opened=terminal;
        String command=profile.environment.equals("codex")||profile.claudeAgent?"terminal-exit":"terminal-close";
        actions.execute(()->{if(closed||g!=generation)return;JSONObject reply=null;try{reply=transport.request(profile,op(command));}catch(Exception ignored){}final JSONObject result=reply;
            handler.post(()->{if(closed||g!=generation)return;if(result!=null&&result.optBoolean("ok")){actionNotice="";if(opened!=null&&terminal==opened)opened.dismiss();requestStamp="";if(!loading)load(false);}else {String notice=result!=null&&result.optString("error").equals("NATIVE_BUSY")?tr("Агент выполняет запрос. Дождитесь завершения и нажмите ещё раз.","The agent is running a request. Wait for completion and tap again."):tr("Не удалось вернуть управление чату. Завершите действие в терминале и повторите.","Could not return control to chat. Finish the terminal action and retry.");if(opened!=null&&terminal==opened)activity.showToast(notice,true);else view.connection(notice);}});
        });
    }
    void terminal(){
        if(!selected()||terminal!=null)return;sync();final WorkspaceStore.Session terminalOwner=snapshot();codexLoginTerminal=terminalOwner.environment.equals("codex")&&codexAuthRequired;final int terminalGeneration=generation;
            restoreChatKeys();
            final com.termux.terminal.TerminalSession session=activity.getCurrentSession();
            final boolean keyboardBefore=activity.getTerminalView().isSoftKeyboardInputEnabled();activity.getTerminalView().setSoftKeyboardInputEnabled(true);
            final View tv=activity.getTerminalView();terminalParent=(ViewGroup)tv.getParent();terminalParams=tv.getLayoutParams();terminalParent.removeView(tv);
            LinearLayout box=new LinearLayout(activity);box.setOrientation(LinearLayout.VERTICAL);final TextView terminalNotice=view.label(tr("Подключение терминала…","Connecting terminal…"),12);box.addView(terminalNotice,new LinearLayout.LayoutParams(-1,-2));box.addView(tv,new LinearLayout.LayoutParams(-1,0,1));
            final com.termux.shared.terminal.io.extrakeys.ExtraKeysView originalKeys=activity.getExtraKeysView();final boolean createdKeys=originalKeys==null||originalKeys.getChildCount()==0;
            final com.termux.shared.terminal.io.extrakeys.ExtraKeysView keys=createdKeys?createTerminalKeys():originalKeys;if(createdKeys)activity.setExtraKeysView(keys);final ViewGroup keysParent=(ViewGroup)keys.getParent();
            final ViewGroup.LayoutParams keysParams=keys==null?null:keys.getLayoutParams();final int keysIndex=keysParent==null?0:keysParent.indexOfChild(keys);final int keysVisibility=keys==null?View.GONE:keys.getVisibility();
            if(keys!=null){if(keysParent!=null)keysParent.removeView(keys);keys.setVisibility(View.VISIBLE);int rows=keys.getRowCount();box.addView(keys,new LinearLayout.LayoutParams(-1,view.dp(Math.max(1,rows)*40)));}
            TextView returnChat=view.pill(returnChatLabel(),this::returnAgentChat);returnChat.setGravity(android.view.Gravity.CENTER);box.addView(returnChat,new LinearLayout.LayoutParams(-1,-2));
            terminal=WorkspaceDialogs.builder(activity).setTitle(name()+" · "+tr("Терминал","Terminal")).setView(box).setNeutralButton(tr("Свернуть","Minimize"),null).setPositiveButton("Enter",null).create();terminal.setCanceledOnTouchOutside(false);
            terminal.setOnDismissListener(d->{ViewGroup parent=(ViewGroup)tv.getParent();if(parent!=null)parent.removeView(tv);terminalParent.addView(tv,0,terminalParams);if(keys!=null){ViewGroup kp=(ViewGroup)keys.getParent();if(kp!=null)kp.removeView(keys);if(keysParent!=null)keysParent.addView(keys,Math.min(keysIndex,keysParent.getChildCount()),keysParams);keys.setVisibility(keysVisibility);if(createdKeys&&activity.getExtraKeysView()==keys)activity.setExtraKeysView(originalKeys);}activity.getTerminalView().setSoftKeyboardInputEnabled(keyboardBefore);terminal=null;codexLoginTerminal=false;activity.getTerminalView().setWorkspaceTerminalVisible(false);actions.execute(()->{try{transport.request(terminalOwner,op("terminal-close"));}catch(Exception ignored){}});sync();});
            terminal.show();terminal.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);terminal.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{if(activity.getCurrentSession()==session&&session!=null&&session.isRunning())session.write("\r");});terminal.getWindow().setLayout(Math.round(activity.getResources().getDisplayMetrics().widthPixels*.92f),Math.round(activity.getResources().getDisplayMetrics().heightPixels*.62f));terminal.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);tv.setVisibility(View.VISIBLE);activity.getTerminalView().setWorkspaceTerminalVisible(true);tv.requestFocus();view.setVisibility(View.GONE);applyPresentationScale();syncExtraKeys();visibilityChanged.run();
            final AlertDialog opened=terminal;
            actions.execute(()->{if(closed||terminalGeneration!=generation)return;boolean ok=false;String notice="";try{JSONObject response=transport.request(terminalOwner,op("terminal-open"));ok=response.optBoolean("ok")&&response.optBoolean("terminalReady",true);notice=response.optString("terminalNotice");}catch(Exception ignored){}final boolean ready=ok;final String explanation=notice.length()>600?notice.substring(0,600):notice;handler.post(()->{if(closed||terminalGeneration!=generation||terminal!=opened||!opened.isShowing())return;String nativeHint=terminalOwner.environment.equals("codex")?tr("«Свернуть» только скрывает окно. «Вернуться в чат Codex» возвращает управление чату. Выполняющийся запрос продолжится до завершения. Shell-поле выполняет вашу команду напрямую с правами Ubuntu, без обращения к модели.","Minimize only hides this window. Return to Codex chat returns control to the chat. A running request continues until completion. The Shell field executes your command directly with Ubuntu privileges, without model inference."):terminalOwner.claudeAgent?tr("После входа чат подключится автоматически. «Свернуть» только скрывает окно. Чтобы завершить штатный интерфейс, нажмите «Вернуться в чат Claude» после окончания запроса.","After sign-in, chat reconnects automatically. Minimize only hides this window. To exit the native interface, tap Return to Claude chat after the request finishes."):"";terminalNotice.setText(ready?nativeHint:!explanation.isEmpty()?explanation:tr("Канал управления пока недоступен. Ниже — терминал текущей сессии: проверьте запуск агента или вход.","The control channel is not available yet. The current session terminal is shown below: check agent startup or sign-in."));terminalNotice.setVisibility(ready&&nativeHint.isEmpty()?View.GONE:View.VISIBLE);});});
    }
    void close(){closed=true;applyPresentationScale();attachments.close();cache.clear();restoreChatKeys();syncExtraKeys();generation++;handler.removeCallbacks(poll);if(terminal!=null)terminal.dismiss();if(details!=null)details.dismiss();if(formDialog!=null)formDialog.dismiss();if(vertexDialog!=null)vertexDialog.dismiss();reads.shutdownNow();actions.shutdown();surface.removeView(view);}
}
