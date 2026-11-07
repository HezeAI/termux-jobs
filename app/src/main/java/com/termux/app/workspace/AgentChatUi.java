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
    void applyPresentationScale(){WorkspaceStore.Session p=current.get();view.scale(SessionTextSize.agentPercent(activity,p==null?100:p.chatScale));if(composerArea!=null)AgentChatScale.apply(composerArea,selected()&&terminal==null&&view.getVisibility()==View.VISIBLE&&p!=null?SessionTextSize.agentPercent(activity,p.chatScale):100);}
    private final LinkedHashMap<String,JSONObject> rows=new LinkedHashMap<>();
    private final AgentChatCache cache=new AgentChatCache();
    private boolean connected,transportFailed,stopAwaiting;
    private JSONObject modelConnection;
    private String owner="",older="",requestStamp="",currentModel="",nativeSession="",actionNotice="";
    void modelChanged(WorkspaceStore.Session p){if(key(p).equals(owner))currentModel="";}
    String modelLabel(WorkspaceStore.Session p){return key(p).equals(owner)&&!currentModel.isEmpty()?currentModel:AiAgentsUi.sessionModelLabel(p,null);}
    private volatile boolean closed;private boolean historyRefreshRequired;private boolean olderQueued,historyExhausted,loading,nativeChat,olderLoading,wasVisible,resolved,presented,busy,directShell,requiresTerminal;
    private volatile int generation;
    private AgentTerminalScreen terminal;
    private AlertDialog details,formDialog,vertexDialog,stopConfirmation;
    private WorkspaceTransferBorder terminalAttentionBorder;
    private JSONObject requestTiming;
    private String stopToken="",confirmedStopToken="",confirmedStopOwner="";
    private String vertexPrompted="";
    private TextView thinkingBody;private ScrollView thinkingScroll;private String thinkingKey="";
    private AgentActionDetailsView actionDetails;
    private boolean actionLiveLoading,actionManual;
    private String actionLiveStamp="";
    java.util.function.BiConsumer<Integer,EditText> terminalLibrary=(section,input)->{};
    private final java.util.Map<String,String> nativeTerminalDrafts=new java.util.HashMap<>();
    private final java.util.Map<String,Boolean> nativeTerminalInteractive=new java.util.HashMap<>(),nativeTerminalKeys=new java.util.HashMap<>();
    private ViewGroup terminalParent;
    private ViewGroup.LayoutParams terminalParams;
    private Runnable visibilityChanged=()->{};
    void onVisibilityChanged(Runnable listener){visibilityChanged=listener;}

    private final LinkedHashMap<String,JSONObject> questionDrafts=new LinkedHashMap<>();
    private final Set<String> activeQuestions=new HashSet<>(),submittingQuestions=new HashSet<>();
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
    void composerInset(int pixels){view.composerInset(pixels);}
    void headerInset(int pixels){view.headerInset(pixels);}
    void composer(EditText input){composer(input,input);}
    void composer(EditText input,View area){composer=input;composerArea=area;}
    private com.termux.shared.terminal.io.extrakeys.ExtraKeysView chatKeys;
    private com.termux.shared.terminal.io.extrakeys.ExtraKeysView.IExtraKeysView previousKeys;
    private void syncExtraKeys(){
        com.termux.shared.terminal.io.extrakeys.ExtraKeysView keys=activity.getExtraKeysView();
        if(chatKeys!=null&&(!showing()||chatKeys!=keys)){chatKeys.setExtraKeysViewClient(previousKeys);chatKeys=null;previousKeys=null;}
        if(showing()&&keys!=null&&chatKeys==null){chatKeys=keys;previousKeys=keys.getExtraKeysViewClient();keys.setExtraKeysViewClient(new ChatExtraKeys(activity.getTerminalView(),keys,()->showing()?(composer):null));}
    }
    View attachmentButton(){return view.folder;}
    private final Map<String,String> sendIDs=new HashMap<>();
    AgentChatUi(TermuxActivity a,Supplier<WorkspaceStore.Session> c,BooleanSupplier v){this(a,c,v,(p,q)->p.environment.equals("codex")?CodexChatControl.request(a.getFilesDir(),p,q):p.claudeAgent?ClaudeLiveControl.request(a.getFilesDir(),p.claudeSocketId,q):p.geminiAgent?GeminiLiveControl.request(a.getFilesDir(),p.geminiSocketId,q):AiAgents.liveRequest(a,p,q));}
    AgentChatUi(TermuxActivity a,Supplier<WorkspaceStore.Session> c,BooleanSupplier v,Transport t){
        activity=a;current=c;visible=v;transport=t;surface=a.findViewById(R.id.workspace_terminal_surface);
        view=new AgentChatView(a,this::details,this::older,()->{},this::terminal,this::interrupt);
        attachments=new ChatAttachmentUi(a,view);view.thumbnail=attachments::thumbnail;
        view.assistantName("OpenCode");
        view.setVisibility(View.GONE);surface.addView(view,new FrameLayout.LayoutParams(-1,-1));handler.post(poll);
    }
    static JSONObject op(String name){JSONObject q=new JSONObject();try{q.put("op",name);}catch(JSONException impossible){throw new IllegalStateException(impossible);}return q;}
    private boolean eligible(){WorkspaceStore.Session p=current.get();return !closed&&p!=null&&(p.aiAgent.equals("opencode")||p.geminiAgent||p.claudeAgent||CodexChatControl.enabled(activity.getFilesDir(),p));}
    private boolean selected(){return eligible()&&visible.getAsBoolean();}
    private String key(WorkspaceStore.Session p){return p.id+":"+(p.environment.equals("codex")?"codex:"+p.codexSocketId:p.claudeAgent?"claude:"+p.claudeSocketId+":"+p.claudeThread:p.geminiAgent?p.geminiSocketId+":"+p.geminiThread:p.aiSocketId+":"+p.aiThread)+":"+p.handle;}
    private WorkspaceStore.Session snapshot(){WorkspaceStore.Session p=current.get();WorkspaceStore.Session s=new WorkspaceStore.Session();s.environment=p.environment;s.codexEnvironment=p.codexEnvironment;s.codexSocketId=p.codexSocketId;s.id=p.id;s.name=p.name;s.directory=p.directory;s.aiThread=p.aiThread;s.aiSocketId=p.aiSocketId;s.aiAgent=p.aiAgent;s.aiConnection=p.aiConnection;s.aiModel=p.aiModel;s.aiVariant=p.aiVariant;s.geminiAgent=p.geminiAgent;s.geminiSocketId=p.geminiSocketId;s.geminiThread=p.geminiThread;s.geminiModel=p.geminiModel;s.claudeAgent=p.claudeAgent;s.claudeSocketId=p.claudeSocketId;s.claudeThread=p.claudeThread;s.claudeModel=p.claudeModel;s.claudeMode=p.claudeMode;return s;}
    boolean terminalBack(){if(terminal==null)return false;terminal.dismiss();return true;}
    boolean terminalVisible(){return terminal!=null&&terminal.isShowing();}
    boolean handlesComposer(){return selected()&&terminal==null&&(nativeChat||!resolved);}
    String name(){WorkspaceStore.Session p=current.get();return p!=null&&p.environment.equals("codex")?"Codex":p!=null&&p.claudeAgent?"Claude":p!=null&&p.geminiAgent?"Gemini":"OpenCode";}
    boolean structured(){return selected()&&nativeChat;}
    boolean terminalAvailable(){return selected();}
    boolean authenticationRequired(){return selected()&&codexAuthRequired;}
    boolean requiresTerminal(){return selected()&&(requiresTerminal||codexAuthRequired);}
    private AgentIndicatorState.State indicator=AgentIndicatorState.State.IDLE;
    AgentIndicatorState.State indicatorState(){return transportFailed?AgentIndicatorState.State.ERROR:indicator;}
    void animationFrame(long time){view.execution.frame(showing()?time:0);}
    boolean working(){return showing()&&busy;}
    boolean canInterrupt(){return !closed&&selected()&&nativeChat&&busy;}
    boolean canShowStop(){return canInterrupt()&&!stopToken.isEmpty()&&owner.equals(key(current.get()));}
    void stopCurrent(){if(canInterrupt())interrupt();}
    private void mergeHistoryRows(JSONObject r,boolean previous,WorkspaceStore.Session p){
                java.util.Set<String> known=null;if((p.geminiAgent||p.claudeAgent||p.environment.equals("codex")||p.aiAgent.equals("opencode"))&&r.has("knownMessages")&&!r.optBoolean("partialHistory")){known=new java.util.HashSet<>();JSONArray ids=r.optJSONArray("knownMessages");if(ids!=null)for(int i=0;i<ids.length();i++)known.add(ids.optString(i));}
                JSONArray replaced=r.optJSONArray("replacedMessages");if(replaced!=null){Set<String> removed=new HashSet<>();for(int i=0;i<replaced.length();i++)removed.add(replaced.optString(i));rows.entrySet().removeIf(e->removed.contains(e.getValue().optString("message")));}
                JSONArray list=r.optJSONArray("rows");LinkedHashMap<String,JSONObject> updated=new LinkedHashMap<>();if(previous&&list!=null)for(int i=0;i<list.length();i++){JSONObject row=list.optJSONObject(i);if(row!=null&&!rows.containsKey(row.optString("key")))updated.put(row.optString("key"),row);}if(previous)updated.putAll(rows);else{Set<String> partialMessages=new HashSet<>();JSONArray partial=r.optJSONArray("partialMessages");if(partial!=null)for(int i=0;i<partial.length();i++)partialMessages.add(partial.optString(i));Set<String> refreshed=new HashSet<>();if(list!=null)for(int i=0;i<list.length();i++)refreshed.add(list.optJSONObject(i).optString("message"));for(Map.Entry<String,JSONObject> e:rows.entrySet())if((known==null||known.contains(e.getValue().optString("message")))&&(!refreshed.contains(e.getValue().optString("message"))||partialMessages.contains(e.getValue().optString("message")))&&!e.getValue().optBoolean("queued"))updated.put(e.getKey(),e.getValue());}
                if(!previous&&list!=null)for(int i=0;i<list.length();i++){JSONObject row=list.optJSONObject(i);if(row!=null)updated.put(row.optString("key"),row);}
                rows.clear();rows.putAll(updated);if(previous||older.isEmpty()&&!historyExhausted){Object cursor=r.opt("older");older=cursor instanceof String?(String)cursor:"";if(previous)historyExhausted=older.isEmpty();}historyRefreshRequired=false;
    }
    private final java.util.Set<AgentProjectPanel.Chat> projectReaders=new java.util.HashSet<>();
    private String projectFilesStamp="";
    private void notifyProjectFilesIfChanged(){if(projectReaders.isEmpty())return;StringBuilder stamp=new StringBuilder(older);for(JSONObject row:rows.values()){stamp.append(row.optString("key")).append(row.optString("kind")).append(row.optBoolean("more")).append(row.optLong("bodyLength")).append(row.opt("messageTime"));Object refs=row.opt("fileReferences"),attachment=row.opt("attachment");if(refs!=null)stamp.append(refs.toString().hashCode());else stamp.append(row.optString("body").hashCode());if(attachment!=null)stamp.append(attachment.toString().hashCode());}String value=stamp.toString();if(!value.equals(projectFilesStamp)){projectFilesStamp=value;projectFilesChanged.run();}}
    private Runnable projectFilesChanged=()->{};
    AgentProjectPanel.Chat projectSource(){final String captured=owner,session=nativeSession;final WorkspaceStore.Session fileProfile=ChatAttachmentUi.source(current.get());
        AgentProjectPanel.Chat source=new AgentProjectPanel.Chat(){Runnable listener;android.app.AlertDialog preview;volatile boolean disposed;boolean pagePending;String pageError="";Runnable retryRead=()->{};java.util.concurrent.Future<?> page;AgentHistoryRead read;
            class Extra{int offset;long readLength;String tail="";boolean done;final java.util.LinkedHashMap<String,JSONObject> files=new java.util.LinkedHashMap<>();}
            final java.util.Map<String,Extra> extras=new java.util.HashMap<>();
            public boolean valid(){return !disposed&&!closed&&showing()&&current.get()!=null&&captured.equals(key(current.get()))&&captured.equals(owner)&&session.equals(nativeSession);}
            public List<JSONObject> rows(){List<JSONObject> result=new ArrayList<>();if(!valid())return result;for(JSONObject row:AgentChatUi.this.rows.values()){Extra extra=extras.get(row.optString("key"));if(extra==null){result.add(row);continue;}try{JSONObject copy=new JSONObject(row.toString());JSONArray refs=new JSONArray();JSONArray original=copy.optJSONArray("fileReferences");if(original!=null)for(int i=0;i<original.length();i++)refs.put(original.get(i));for(JSONObject file:extra.files.values())refs.put(file);copy.put("fileReferences",refs);result.add(copy);}catch(Exception ignored){}}return result;}
            private JSONObject longRow(){if(!valid())return null;for(JSONObject row:AgentChatUi.this.rows.values()){if(!java.util.Arrays.asList("user","text").contains(row.optString("kind")))continue;Extra extra=extras.get(row.optString("key"));JSONArray refs=row.optJSONArray("fileReferences");if(extra!=null&&extra.done&&row.optLong("bodyLength")>extra.readLength)extra.done=false;if((row.optBoolean("more")||refs!=null&&refs.length()>=100)&&(extra==null||!extra.done))return row;}return null;}
            public boolean hasLongReferences(){return longRow()!=null;}
            public void retryRead(){if(!pagePending)retryRead.run();}
            public void moreReferences(){if(pagePending)return;retryRead=this::moreReferences;JSONObject row=longRow();if(row==null)return;Extra extra=extras.computeIfAbsent(row.optString("key"),k->new Extra());pagePending=true;pageError="";projectFilesChanged.run();final WorkspaceStore.Session original=snapshot();final int g=generation;AgentHistoryRead operation=new AgentHistoryRead();read=operation;
                page=reads.submit(()->{JSONObject response=null;try(AutoCloseable scope=operation.enter()){JSONObject q=op("chat-detail").put("session",session).put("message",row.optString("message")).put("part",row.optString("part")).put("offset",extra.offset);response=transport.request(original,q);}catch(Exception ignored){}final JSONObject result=response;handler.post(()->{if(!valid()||operation.cancelled()||g!=generation)return;pagePending=false;if(result!=null&&result.optBoolean("ok")){try{if(!(result.opt("body") instanceof String)||!result.has("next"))throw new java.io.IOException("INVALID_HISTORY_PAGE");String chunk=result.getString("body"),combined=extra.tail+chunk;JSONObject projected=new JSONObject(row.toString()).put("body",combined);projected.remove("attachment");projected.remove("fileReferences");for(ChatFileReferences.Entry e:ChatFileReferences.collect(java.util.Collections.singletonList(projected)))extra.files.put(e.file.optString("path"),e.file);int next=result.isNull("next")?-1:result.optInt("next",-1);if(!result.isNull("next")&&(!(result.opt("next") instanceof Number)||next<=extra.offset))throw new java.io.IOException("INVALID_HISTORY_CURSOR");extra.tail=combined.substring(Math.max(0,combined.length()-8192));extra.readLength=row.optLong("bodyLength");extra.done=next<0||next<=extra.offset;extra.offset=extra.done?extra.offset+chunk.length():next;}catch(Exception ignored){pageError=tr("Не удалось прочитать ссылки. Повторите.","Could not read links. Retry.");}}else pageError=tr("Не удалось прочитать сообщение. Повторите.","Could not read message. Retry.");projectFilesChanged.run();});});
            }
            public boolean hasOlder(){return valid()&&(!older.isEmpty()||historyRefreshRequired);}
            public boolean loadingOlder(){return pagePending;}public String historyError(){return pageError;}
            public void refresh(){readHistory(true);}
            public void older(){readHistory(false);}
            private void readHistory(boolean refresh){if(!valid()||!refresh&&older.isEmpty()&&!historyRefreshRequired&&pageError.isEmpty()||pagePending)return;retryRead=()->readHistory(refresh);pagePending=true;pageError="";projectFilesChanged.run();final WorkspaceStore.Session original=snapshot();final String cursor=refresh?"":older;final int g=generation;final AgentHistoryRead operation=new AgentHistoryRead();read=operation;
                page=reads.submit(()->{JSONObject response=null;try(AutoCloseable scope=operation.enter()){JSONObject request=op("chat-snapshot");if(!cursor.isEmpty())request.put("cursor",cursor);response=transport.request(original,request);}catch(Exception ignored){}final JSONObject result=response;handler.post(()->{if(!valid()||operation.cancelled()||g!=generation)return;pagePending=false;if(result!=null&&result.optBoolean("historyReset")){older=result.optString("older");historyExhausted=older.isEmpty();pageError=tr("История обновилась. Повторите загрузку.","History changed. Retry loading.");projectFilesChanged.run();return;}if(result!=null&&result.optBoolean("nativeChat")&&result.optJSONArray("rows")!=null&&result.has("older")&&session.equals(result.optString("session",result.optString("thread",session)))){mergeHistoryRows(result,!cursor.isEmpty(),original);view.olderRows(new ArrayList<>(AgentChatUi.this.rows.values()),!older.isEmpty());}else pageError=tr("Не удалось загрузить историю. Повторите.","Could not load history. Retry.");projectFilesChanged.run();});});
            }
            public void open(JSONObject file){if(valid()){attachments.openProjectFile(fileProfile,file);preview=attachments.projectViewer();}}
            public boolean canJump(String key){return valid()&&view.canReveal(key);}
            public boolean jump(String key){return valid()&&view.reveal(key);}
            public void observe(Runnable r){listener=r;projectFilesChanged=r;projectFilesStamp="";}
            public void close(){projectReaders.remove(this);extras.clear();disposed=true;pagePending=false;if(read!=null)read.close();if(page!=null)page.cancel(true);if(projectFilesChanged==listener)projectFilesChanged=()->{};if(preview!=null&&preview==attachments.projectViewer())preview.dismiss();}
        };projectReaders.add(source);return source;
    }
    boolean showing(){return selected()&&nativeChat&&terminal==null;}
    private java.util.function.BooleanSupplier externalDisplay=()->false;
    void externalDisplay(java.util.function.BooleanSupplier value){externalDisplay=value;}
    void sync(){
        for(AgentProjectPanel.Chat source:new java.util.ArrayList<>(projectReaders))if(!source.valid())source.close();
        if(stopConfirmation!=null&&(!canInterrupt()||!confirmedStopOwner.equals(key(current.get()))||!confirmedStopToken.equals(stopToken)))stopConfirmation.dismiss();
        String next=eligible()?key(current.get()):"";
        boolean nowVisible=selected();if(wasVisible&&!nowVisible){generation++;loading=false;olderLoading=false;olderQueued=false;view.olderState(false,null);if(terminal!=null)terminal.dismissImmediately();if(details!=null)details.dismiss();if(formDialog!=null)formDialog.dismiss();if(vertexDialog!=null)vertexDialog.dismiss();}wasVisible=nowVisible;
        if(!next.equals(owner)){
            if(!owner.isEmpty()){extraByOwner.put(owner,extraVisible);while(extraByOwner.size()>32)extraByOwner.remove(extraByOwner.keySet().iterator().next());}
            restoreChatKeys();extraVisible=extraByOwner.getOrDefault(next,false);
            if(!owner.isEmpty()){savePresentation();}
            generation++;indicator=AgentIndicatorState.State.IDLE;attachmentsSupported=false;codexAuthRequired=false;codexLoginTerminal=false;owner=next;modelConnection=null;modelConnectionView();stopToken="";requestTiming=null;actionNotice="";currentModel="";nativeSession="";connected=false;transportFailed=false;
            view.assistantName(name());view.actionCards(true);nativeChat=false;resolved=false;historyRefreshRequired=false;loading=false;olderLoading=false;olderQueued=false;busy=false;directShell=false;requiresTerminal=false;rows.clear();older="";historyExhausted=false;requestStamp="";view.clear();
            attachments.sync(owner,current.get()==null?null:snapshot(),false);
            AgentChatCache.State saved=cache.take(next);
            if(saved==null)view.connection(next.isEmpty()?"":tr("Подключение…","Connecting…"));else restorePresentation(saved);
            if(terminal!=null)terminal.dismissImmediately();if(details!=null)details.dismiss();if(formDialog!=null)formDialog.dismiss();if(vertexDialog!=null)vertexDialog.dismiss();
        }
        if(terminalAttentionBorder!=null)terminalAttentionBorder.active(requiresTerminal());
        applyPresentationScale();attachments.sync(owner,current.get()==null?null:snapshot(),attachmentsSupported);
        view.execution.timing(owner,requestTiming,busy);view.execution.present(indicatorState(),showing());
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
        AgentChatCache.State state=new AgentChatCache.State();state.indicator=indicator;state.requestTiming=requestTiming;state.modelConnection=modelConnection;state.rows.putAll(rows);state.timelineExpansion.putAll(view.timelineExpansion);state.historyExhausted=historyExhausted;state.older=older;state.model=currentModel;state.session=nativeSession;state.notice=actionNotice;state.nativeChat=nativeChat;state.resolved=resolved;state.busy=busy;state.directShell=directShell;state.needsTerminal=requiresTerminal||codexAuthRequired;state.connected=connected;state.failed=transportFailed;state.position=view.position();cache.put(owner,state);
    }
    private void restorePresentation(AgentChatCache.State state){
        indicator=state.indicator;requestTiming=state.requestTiming;modelConnection=state.modelConnection;modelConnectionView();rows.putAll(state.rows);historyRefreshRequired=state.trimmed;historyExhausted=state.historyExhausted&&!state.trimmed;older=state.trimmed?"":state.older;currentModel=state.model;nativeSession=state.session;actionNotice=state.notice;nativeChat=state.nativeChat;resolved=state.resolved;busy=state.busy;directShell=state.directShell;requiresTerminal=state.needsTerminal;connected=state.connected;transportFailed=state.failed;
        view.timelineExpansion.putAll(state.timelineExpansion);view.rows(new ArrayList<>(rows.values()),!older.isEmpty());if(transportFailed)connectionFailure();else{JSONObject status=op("chat-snapshot");try{status.put("connected",connected).put("needsTerminal",requiresTerminal);}catch(JSONException impossible){throw new IllegalStateException(impossible);}connectionState(status);}view.restorePosition(state.position);
        // Pending permissions/forms are intentionally rebuilt only from a fresh native snapshot.
    }
    private void connectionFailure(){if(vertexSetup())return;view.connection(tr("Нет связи с сессией. Повторяем подключение автоматически.\nЕсли ожидание затянулось, откройте терминал этого чата и проверьте запуск агента. Нажмите здесь, чтобы проверить снова.","No connection to the session. Retrying automatically.\nIf this persists, open this chat’s terminal and check that the agent started. Tap here to check again."),this::retryConnection);}
    private android.os.FileObserver preparationWatcher;
    private final Runnable poll=new Runnable(){public void run(){if(closed)return;sync();if(selected()&&!vertexSetup()&&!loading&&(terminal==null||codexLoginTerminal))load(false);handler.postDelayed(this,nativeChat&&!busy?2000:500);}};
    private boolean vertexSetup(){
        WorkspaceStore.Session p=current.get();
        if(connected)return false;
        if(!selected()||!VertexConnection.pending(activity,p)||activity.getCurrentSession()==null||!activity.getCurrentSession().isRunning()){if(vertexDialog!=null&&vertexDialog.isShowing())vertexDialog.dismiss();return false;}
        if(terminal!=null)terminal.dismissImmediately();
        if(VertexConnection.file(activity,p,"response").isFile()){view.connection(tr("Параметры Vertex сохранены. Продолжаем подключение…","Vertex settings saved. Continuing connection…"));return true;}
        String run=key(p);
        Runnable open=()->{if(vertexDialog!=null&&vertexDialog.isShowing())return;vertexDialog=VertexConnection.prompt(activity,p,()->!closed&&selected()&&key(current.get()).equals(run),()->{view.connection(tr("Параметры Vertex сохранены. Продолжаем подключение…","Vertex settings saved. Continuing connection…"));});};
        view.connection(tr("Учётные данные Google Cloud найдены. Укажите ID облачного проекта и регион для Vertex AI. Нажмите здесь, чтобы продолжить настройку.","Google Cloud credentials were found. Enter a cloud project ID and location for Vertex AI. Tap here to continue setup."),open);
        if(!vertexPrompted.equals(run)){vertexPrompted=run;open.run();}
        return true;
    }
    boolean focusRequest(){return focus(view.requests);}
    private boolean focus(View v){if(v instanceof ViewGroup){ViewGroup group=(ViewGroup)v;for(int i=0;i<group.getChildCount();i++)if(focus(group.getChildAt(i)))return true;}else if(v.isClickable()&&v instanceof TextView&&!((TextView)v).getText().toString().contains("\n")){v.setFocusableInTouchMode(true);return v.requestFocus();}return false;}
    private void interrupt(){
        if(!canInterrupt()||stopConfirmation!=null&&stopConfirmation.isShowing())return;
        if(stopToken.isEmpty()){actionNotice=tr("Этот запуск не передал идентификатор текущего запроса. Безопасная остановка через подтверждение недоступна; проверьте штатный терминал агента.","This launch did not report the current request ID. Confirmed stop is unavailable; check the agent's native terminal.");view.connection(actionNotice);return;}
        final int g=generation;final String selectedOwner=key(current.get()),token=stopToken,session=nativeSession;
        confirmedStopOwner=selectedOwner;confirmedStopToken=token;
        AlertDialog dialog=WorkspaceDialogs.builder(activity).setTitle(tr("Остановить ","Stop ")+name()+"?")
            .setMessage(directShell?tr("Остановка команды будет запрошена у выбранного агента. Частичный вывод может стать недоступен.","Request command interruption from the selected agent. Partial output may become unavailable."):tr("Пока это окно открыто, агент продолжает работу.","The agent keeps working while this window is open."))
            .setNegativeButton(tr("Да","Yes"),(d,w)->{
                if(!canInterrupt()||g!=generation||!selectedOwner.equals(key(current.get()))||!session.equals(nativeSession)||!token.equals(stopToken)){
                    actionNotice=tr("Запрос уже завершился или изменился. Остановка не отправлена.","The request has finished or changed. No stop was sent.");view.connection(actionNotice);return;
                }
                JSONObject q=op("chat-interrupt");try{q.put("session",session).put("stopToken",token);}catch(JSONException ignored){}
                operation(q,null);
            }).setPositiveButton(tr("Нет","No"),null).create();
        stopConfirmation=dialog;dialog.setOnDismissListener(d->{if(stopConfirmation==dialog)stopConfirmation=null;});dialog.setCanceledOnTouchOutside(true);dialog.show();
    }
    private String tr(String ru,String en){return WorkspaceLanguage.code(activity).equals("ru")?ru:en;}
    private void load(boolean previous){
        if(!selected()||loading)return;loading=true;if(previous){olderLoading=true;view.olderState(true,null);}final int g=generation;final WorkspaceStore.Session p=snapshot();final JSONObject q=op("chat-snapshot");
        try{if(previous)q.put("cursor",older);}catch(JSONException ignored){}
        reads.execute(()->{JSONObject result=null;try{result=transport.request(p,q);}catch(Exception ignored){try{JSONObject status=transport.request(p,op("status"));if(status.optBoolean("ok")&&!status.optBoolean("nativeChat"))result=status;}catch(Exception unavailable){}}final JSONObject r=result;
            handler.post(()->{if(closed||g!=generation)return;loading=false;olderLoading=false;try{if(previous&&r!=null&&r.optBoolean("historyReset")){Object cursor=r.opt("older");older=cursor instanceof String?(String)cursor:"";historyExhausted=older.isEmpty();view.rows(new ArrayList<>(rows.values()),!older.isEmpty());view.olderState(false,tr("История обновилась. Загрузить предыдущие","History refreshed. Load earlier"));return;}if(previous&&(r==null||!r.optBoolean("nativeChat"))){view.olderState(false,tr("Не удалось загрузить. Повторить","Could not load. Retry"));return;}if(r==null||!r.optBoolean("nativeChat")){if(r!=null){resolved=true;nativeChat=false;connected=false;sync();}if(r==null){transportFailed=true;connected=false;connectionFailure();}else view.connection("");return;}
                if(!previous)indicator=AgentIndicatorState.from(r,false);boolean authChanged=codexAuthRequired!=r.optBoolean("authRequired");codexAuthRequired=r.optBoolean("authRequired");transportFailed=false;attachmentsSupported=r.optBoolean("attachmentsSupported");connected=r.optBoolean("connected");String actualSession=r.optString("session");if((p.geminiAgent||p.claudeAgent||p.environment.equals("codex")||p.aiAgent.equals("opencode"))&&!actualSession.equals(nativeSession)){view.historyBaseline();if(!nativeSession.isEmpty()){generation++;olderQueued=false;view.olderState(false,null);actionNotice="";rows.clear();older="";historyExhausted=false;requestStamp="";view.clear();if(details!=null)details.dismiss();if(formDialog!=null)formDialog.dismiss();if(vertexDialog!=null)vertexDialog.dismiss();}nativeSession=actualSession;modelConnection=null;}
                if(!previous){modelConnection=r.optJSONObject("modelConnection");modelConnectionView();}
                String model=AiAgentsUi.sessionModelLabel(p,r.optJSONObject("model"));boolean modelChanged=!model.equals(currentModel);currentModel=model;boolean changed=!stopToken.equals(r.optString("stopToken"))||authChanged||modelChanged||busy!=r.optBoolean("busy")||requiresTerminal!=r.optBoolean("needsTerminal");requiresTerminal=r.optBoolean("needsTerminal");nativeChat=true;resolved=true;busy=r.optBoolean("busy");if(stopAwaiting&&!busy){stopAwaiting=false;actionNotice=tr("Агент больше не выполняет запрос.","The agent is no longer running a request.");}stopToken=r.optString("stopToken");requestTiming=r.optJSONObject("requestTiming");directShell=r.optBoolean("directShell");sync();layoutChatKeys();syncExtraKeys();if(changed)visibilityChanged.run();
                mergeHistoryRows(r,previous,p);connectionState(r);if(previous){view.olderRows(new ArrayList<>(rows.values()),!older.isEmpty());view.olderState(false,null);}else view.liveRows(new ArrayList<>(rows.values()),!older.isEmpty(),busy);requests(r.optJSONObject("permission"),r.optJSONArray("forms"),r.optBoolean("needsTerminal"),r.optString("questionSupport"));if(p.environment.equals("codex")&&codexLoginTerminal&&!codexAuthRequired&&connected&&terminal!=null)terminal.dismissImmediately();
                notifyProjectFilesIfChanged();
                refreshActionDetails();
                JSONObject thought=rows.get(thinkingKey);if(details!=null&&details.isShowing()&&thinkingBody!=null&&thought!=null&&!thought.optBoolean("more")&&thinkingBody.getSelectionStart()==thinkingBody.getSelectionEnd()&&!thinkingBody.getText().toString().equals(thought.optString("body"))){boolean bottom=thinkingBody.getHeight()-thinkingScroll.getHeight()-thinkingScroll.getScrollY()<view.dp(48);thinkingBody.setText(thought.optString("body"));if(bottom){ScrollView held=thinkingScroll;held.post(()->held.fullScroll(View.FOCUS_DOWN));}}
                }finally{if(!closed&&g==generation&&olderQueued){olderQueued=false;if(selected()&&!older.isEmpty())load(true);else view.olderState(false,null);}}
            });});
    }
    private void modelConnectionView(){
        String label=AgentModelConnection.label(modelConnection,WorkspaceLanguage.code(activity).equals("ru"));view.status.setText(label);view.status.setVisibility(label.isEmpty()?View.GONE:View.VISIBLE);view.updateHeaderInset();
        final JSONObject reported=modelConnection;view.status.setOnClickListener(reported==null?null:v->{
            if(details!=null)details.dismiss();ScrollView scroll=new ScrollView(activity);TextView text=view.label(AgentModelConnection.details(reported,WorkspaceLanguage.code(activity).equals("ru")),13);text.setTextIsSelectable(true);scroll.addView(text);
            details=WorkspaceDialogs.builder(activity).setTitle(label).setView(scroll).setPositiveButton(tr("Закрыть","Close"),null).show();
        });
    }
    private void retryConnection(){if(!loading)load(false);}
    private void connectionState(JSONObject r){
        if(current.get().environment.equals("codex")&&r.optBoolean("authRequired")){
            view.connection(r.optBoolean("loginPending")?tr("Вход ожидает подтверждения в браузере. Нажмите, чтобы открыть окно входа.","Sign-in awaits browser confirmation. Tap to open sign-in."):r.optBoolean("loginError")?tr("Вход не завершён. Нажмите, чтобы повторить вход в Codex.","Sign-in did not complete. Tap to retry Codex sign-in."):tr("Войти в Codex — нажмите для подключения аккаунта.","Sign in to Codex — tap to connect your account."),this::nativeTerminal);return;
        }
        if(current.get().geminiAgent&&current.get().geminiAuth.equals("api")&&r.optBoolean("authError")){view.connection(tr("Не удалось войти в Gemini. Проверьте API-ключ в настройках сессии/пресета и перезапустите её. Нажмите для просмотра терминала.","Could not sign in to Gemini. Check the API key in session/preset settings and restart the session. Tap to inspect the terminal."),this::terminal);return;}
        if(r.optBoolean("needsTerminal")){if((current.get().claudeAgent||current.get().environment.equals("codex"))&&!r.optString("connectionNotice").isEmpty()){String notice=r.optString("connectionNotice");view.connection(notice.length()>600?notice.substring(0,600):notice);return;}view.connection(tr("Требуется действие в терминале. Откройте терминал этого чата в шапке.","An action is needed in the terminal. Open this chat’s terminal in the header."));return;}
        if(!actionNotice.isEmpty()){view.connection(actionNotice);return;}
        if(!r.optBoolean("connected")){view.connection(tr("Подключаем чат к сессии. Если ожидание затянулось, проверьте запуск в терминале этого чата.","Connecting chat to the session. If this persists, check startup in this chat’s terminal."),this::retryConnection);return;}
        view.connection(rows.isEmpty()?tr("Подключено к сессии ","Connected to the ")+name()+tr(". Напишите сообщение, чтобы начать."," session. Send a message to begin."):"");
    }
    void showQueue(){
        LinearLayout list=AgentStatusUi.body(activity);int count=0;
        for(JSONObject row:rows.values())if(row.optBoolean("queued")){AgentStatusUi.info(list,row.optString("text",row.optString("body")));count++;}
        if(count==0)AgentStatusUi.info(list,tr("В загруженной истории нет сообщений в очереди","No queued messages in loaded history"));
        WorkspaceDialogs.builder(activity).setTitle(tr("Очередь сообщений","Message queue")).setView(AgentStatusUi.scroll(list)).setNegativeButton(tr("Закрыть","Close"),null).show();
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
        sendOperation(q,()->{sendIDs.remove(k);if(refs.length()>0)attachments.accepted(attachmentOwner,refs);view.followAfterSend();accepted.run();});
    }
    private void sendOperation(JSONObject q,Runnable success){
        if(!selected())return;final WorkspaceStore.Session p=snapshot();final int g=generation;
        actionNotice=tr("Отправляется… До подтверждения текст и вложения остаются во вводе.","Sending… Text and attachments stay in the composer until acknowledged.");view.connection(actionNotice);
        actions.execute(()->{if(closed||g!=generation)return;JSONObject reply=null;try{reply=transport.request(p,q);}catch(Exception ignored){}
            final AgentMessageDelivery result=new AgentMessageDelivery(reply);
            handler.post(()->{if(closed||g!=generation)return;
                actionNotice=result.accepted&&q.optString("kind").equals("message")?"":result.notice(WorkspaceLanguage.code(activity).equals("ru"));
                if(result.accepted){success.run();requestStamp="";if(!loading)load(false);}
                view.connection(actionNotice);
            });
        });
    }
    private void operation(JSONObject q,Runnable success){operation(q,success,null);}
    private void operation(JSONObject q,Runnable success,Runnable failure){operation(q,success,failure,null);}
    private void operation(JSONObject q,Runnable success,Runnable failure,Runnable settled){
        if(!selected())return;final WorkspaceStore.Session p=snapshot();final int g=generation;
        (q.optString("op").equals("chat-form")?reads:actions).execute(()->{if(closed||g!=generation){if(settled!=null)handler.post(settled);return;}boolean ok=false;String error="";try{JSONObject r=transport.request(p,q);ok=r.optBoolean("ok");error=r.optString("error");}catch(Exception ignored){}final boolean done=ok;final String failureCode=error;
            handler.post(()->{if(settled!=null)settled.run();if(closed||g!=generation)return;if(done){stopAwaiting=q.optString("op").equals("chat-interrupt");actionNotice=q.optString("op").equals("chat-interrupt")?tr("Запрос остановки принят. Ожидается фактический статус агента.","Stop request accepted. Waiting for the agent’s actual status."):"";if(success!=null)success.run();requestStamp="";if(!loading)load(false);}else {actionNotice=tr("Агент не подтвердил действие. Проверьте терминал и фактическое состояние перед повтором. Автоматического повтора нет.","The agent did not confirm the action. Check the terminal and actual state before retrying. There is no automatic retry.");if(q.optString("op").equals("chat-interrupt")){actionNotice=failureCode.equals("STALE_REQUEST")?tr("Запрос уже завершился или сменился. Старое подтверждение отклонено; новый запрос не остановлен.","The request finished or changed. The stale confirmation was rejected; the new request was not stopped."):tr("Остановка не подтверждена. Проверьте фактический статус или штатный терминал агента.","Stop was not confirmed. Check the agent's actual status or native terminal.");if(!loading)load(false);}view.connection(actionNotice);if(failure!=null)failure.run();}});});
    }
    private void details(JSONObject row){
        if(row.optString("kind").equals("completionTime")){
            long time=row.optJSONObject("requestSummary")==null?0:row.optJSONObject("requestSummary").optLong("completedAt");if(time<=0)return;
            WorkspaceDialogs.builder(activity).setTitle(tr("Ответ завершён","Response completed"))
                .setMessage(AgentMessageTime.caption(activity,row.optJSONObject("requestSummary"))+"\n\n"+AgentMessageTime.full(activity,time)+"\n\n"+tr("Длительность запроса включает ожидания и не является измерением загрузки CPU.","Request duration includes waits; it is not a CPU usage measurement.")).setPositiveButton(tr("Закрыть","Close"),null).show();return;
        }
        if(row.optString("kind").equals("deliveryReceipt")){
            if(!selected())return;if(details!=null)details.dismiss();ScrollView scroll=new ScrollView(activity);TextView text=view.label(new AgentMessageDelivery(row.optJSONObject("deliveryReceipt")).notice(WorkspaceLanguage.code(activity).equals("ru")),13);text.setTextIsSelectable(true);scroll.addView(text);details=WorkspaceDialogs.builder(activity).setTitle(tr("Доставка сообщения","Message delivery")).setView(scroll).setPositiveButton(tr("Закрыть","Close"),null).show();return;
        }

        boolean compactContext=(row.optString("toolType").equals("contextCompaction")||row.optString("part").equals("context"))&&!row.optBoolean("more");
        if(row.optString("kind").equals("tool")&&!compactContext){actionDetails(row);return;}
        if(!selected())return;if(row.optString("kind").equals("attachment")&&row.optJSONObject("attachment")!=null){attachments.open(row.optJSONObject("attachment"));return;}if(details!=null)details.dismiss();thinkingBody=null;thinkingScroll=null;thinkingKey="";LinearLayout box=new LinearLayout(activity);box.setOrientation(LinearLayout.VERTICAL);TextView body=view.label(row.optString("body"),14);body.setTextIsSelectable(true);if(compactContext){
            body.setPadding(view.dp(20),view.dp(14),view.dp(20),view.dp(18));
            details=WorkspaceDialogs.builder(activity).setTitle(CodexActionLabels.title(row,WorkspaceLanguage.code(activity).equals("ru"))).setView(body).create();details.setCanceledOnTouchOutside(true);details.show();
            int width=Math.min(Math.round(activity.getResources().getDisplayMetrics().widthPixels*.94f),Math.max(view.dp(240),Math.round(body.getPaint().measureText(row.optString("body")))+view.dp(64)));
            details.getWindow().setLayout(width,android.view.ViewGroup.LayoutParams.WRAP_CONTENT);return;
        }
        final AgentMarkdownView markdownBody=row.optString("kind").equals("text")?new AgentMarkdownView(activity,attachments.markdown()):null;if(markdownBody!=null)markdownBody.update(name(),row.optString("body"),null);
        ScrollView scroll=new ScrollView(activity);scroll.addView(markdownBody==null?body:markdownBody);box.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        TextView next=view.pill(tr("Загрузить полный текст","Load full text"),()->{});box.addView(next);next.setVisibility(row.optBoolean("more")?View.VISIBLE:View.GONE);
        details=WorkspaceDialogs.builder(activity).setTitle(row.optString("kind").equals("reasoning")?tr("Рассуждение","Thinking"):CodexActionLabels.title(row,WorkspaceLanguage.code(activity).equals("ru"))).setView(box).setPositiveButton(tr("Назад","Back"),null).create();details.show();details.getWindow().setLayout(-1,Math.round(activity.getResources().getDisplayMetrics().heightPixels*.72f));
        if(row.optString("kind").equals("reasoning")){thinkingBody=body;thinkingScroll=scroll;thinkingKey=row.optString("key");}
        final AlertDialog dialog=details;final int[] offset={0};final int g=generation;final WorkspaceStore.Session p=snapshot();
        Runnable[] page=new Runnable[1];page[0]=()->{next.setEnabled(false);JSONObject q=op("chat-detail");try{if(p.geminiAgent||p.claudeAgent||p.environment.equals("codex")||p.aiAgent.equals("opencode"))q.put("session",nativeSession);q.put("message",row.optString("message")).put("part",row.optString("part")).put("offset",offset[0]);}catch(JSONException ignored){}
            reads.execute(()->{JSONObject r=null;try{r=transport.request(p,q);}catch(Exception ignored){}final JSONObject result=r;handler.post(()->{if(closed||g!=generation||!dialog.isShowing())return;next.setEnabled(true);if(result==null||!result.optBoolean("ok")){next.setVisibility(View.VISIBLE);next.setText(tr("Повторить загрузку","Retry loading"));return;}if(offset[0]==0)body.setText((row.optString("kind").equals("receipt")?tr("Разрешение не означает успешного выполнения.\n\n","Permission does not mean successful execution.\n\n"):"")+result.optString("body"));else body.append(result.optString("body"));if(markdownBody!=null)markdownBody.update(name(),body.getText().toString(),null);offset[0]=result.isNull("next")?-1:result.optInt("next",-1);next.setVisibility(offset[0]<0?View.GONE:View.VISIBLE);next.setText(tr("Далее","More"));});});};next.setOnClickListener(v->page[0].run());page[0].run();
    }
    private JSONObject detailRequest(WorkspaceStore.Session p,JSONObject row,int offset,String session){
        JSONObject q=op("chat-detail");try{if(p.geminiAgent||p.claudeAgent||p.environment.equals("codex")||p.aiAgent.equals("opencode"))q.put("session",session);q.put("message",row.optString("message")).put("part",row.optString("part")).put("offset",offset);}catch(JSONException impossible){throw new IllegalStateException(impossible);}return q;
    }
    private void actionDetails(JSONObject row){
        if(!selected())return;if(details!=null)details.dismiss();thinkingBody=null;thinkingScroll=null;thinkingKey="";
        AgentActionDetailsView panel=new AgentActionDetailsView(view,row,attachments::thumbnail,attachments::open);actionDetails=panel;actionManual=false;actionLiveLoading=false;actionLiveStamp="";
        AlertDialog.Builder builder=WorkspaceDialogs.builder(activity).setTitle(CodexActionLabels.title(row,WorkspaceLanguage.code(activity).equals("ru"))).setView(panel);
        if(!panel.compactPreview())builder.setPositiveButton(tr("Назад","Back"),null);
        AlertDialog dialog=builder.create();details=dialog;
        dialog.setOnDismissListener(d->{if(actionDetails==panel){actionDetails=null;actionLiveLoading=false;actionLiveStamp="";}});dialog.setCanceledOnTouchOutside(true);dialog.show();dialog.getWindow().setLayout(Math.round(activity.getResources().getDisplayMetrics().widthPixels*.94f),panel.compactPreview()?android.view.ViewGroup.LayoutParams.WRAP_CONTENT:Math.round(activity.getResources().getDisplayMetrics().heightPixels*.72f));
        final WorkspaceStore.Session p=snapshot();final String session=nativeSession;final int g=generation;final int[] offset={0};final StringBuilder buffer=new StringBuilder();
        Runnable[] page=new Runnable[1];page[0]=()->{actionManual=true;panel.more.setEnabled(false);JSONObject request=detailRequest(p,row,offset[0],session);
            reads.execute(()->{JSONObject r=null;try{r=transport.request(p,request);}catch(Exception ignored){}JSONObject result=r;handler.post(()->{
                if(closed||g!=generation||actionDetails!=panel||!dialog.isShowing())return;panel.more.setEnabled(true);
                if(result==null||!result.optBoolean("ok",true)||!(result.opt("body") instanceof String)){panel.more.setVisibility(View.VISIBLE);panel.more.setText(tr("Повторить загрузку","Retry loading"));return;}
                String chunk=result.optString("body");int next=result.isNull("next")?-1:result.optInt("next",-1);if(chunk.length()>30000||buffer.length()+chunk.length()>2000000||next>=0&&(next<=offset[0]||next>2000000)){panel.more.setText(tr("Достигнут лимит просмотра","Preview limit reached"));panel.more.setEnabled(false);return;}
                if(offset[0]==0)buffer.setLength(0);buffer.append(chunk);panel.nativeText(buffer.toString(),next<0);JSONObject latest=rows.get(panel.key());if(next<0&&latest!=null&&CodexActionTimeline.running(latest))actionManual=false;offset[0]=Math.max(0,next);panel.more.setVisibility(next<0?View.GONE:View.VISIBLE);panel.more.setText(tr("Далее","More"));
            });});};panel.more.setOnClickListener(v->page[0].run());refreshActionDetails();
    }
    private void refreshActionDetails(){
        AgentActionDetailsView panel=actionDetails;AlertDialog dialog=details;if(panel==null||dialog==null||!dialog.isShowing())return;JSONObject row=rows.get(panel.key());if(row==null)return;
        panel.update(row);dialog.setTitle(CodexActionLabels.title(row,WorkspaceLanguage.code(activity).equals("ru")));
        JSONObject preview=AgentActionPresentation.preview(row);if(actionManual||actionLiveLoading||!row.optBoolean("more")||preview!=null||!panel.followsOutput())return;
        long length=row.optLong("bodyLength",row.optString("body").length());if(length<0||length>2000000)return;String stamp=row.optString("body")+":"+length+":"+row.optString("status");if(stamp.equals(actionLiveStamp))return;
        actionLiveLoading=true;int offset=(int)Math.max(0,length-24000);final int g=generation;final WorkspaceStore.Session p=snapshot();final String session=nativeSession;JSONObject request=detailRequest(p,row,offset,session);
        reads.execute(()->{JSONObject r=null;try{r=transport.request(p,request);}catch(Exception ignored){}JSONObject result=r;handler.post(()->{
            if(closed||g!=generation||actionDetails!=panel||!dialog.isShowing())return;actionLiveLoading=false;
            if(actionManual||!panel.followsOutput()||result==null||!result.optBoolean("ok",true)||!(result.opt("body") instanceof String))return;
            actionLiveStamp=stamp;panel.nativeText((offset>0?tr("… последние строки вывода …\n","… latest output …\n"):"")+result.optString("body"),false);
        });});
    }
    private String questionKey(JSONObject f){return owner+":"+nativeSession+":"+f.optString("id")+":"+f.optString("nonce");}
    private void requests(JSONObject permission,JSONArray forms,boolean needsTerminal,String questionSupport){
        activeQuestions.clear();if(forms!=null)for(int i=0;i<forms.length();i++)if(forms.optJSONObject(i)!=null)activeQuestions.add(questionKey(forms.optJSONObject(i)));
        String stamp=questionSupport+String.valueOf(permission)+String.valueOf(forms)+needsTerminal;if(stamp.equals(requestStamp))return;requestStamp=stamp;
        final boolean bottom=view.timeline.getHeight()-view.scroll.getHeight()-view.scroll.getScrollY()<view.dp(48);view.clearRequests();
        if(permission!=null){TextView title=view.label(permission.optString("title")+"\n"+permission.optString("body"),13);title.setMaxLines(5);title.setOnClickListener(v->{if(details!=null)details.dismiss();details=WorkspaceDialogs.builder(activity).setMessage(permission.optString("body")).setPositiveButton(tr("Назад","Back"),null).show();});view.requests.addView(title);LinearLayout buttons=new LinearLayout(activity);
            for(String outcome:new String[]{"proceed_once","cancel"})buttons.addView(view.pill(outcome.equals("cancel")?tr("Нет","No"):tr("Да, разово","Yes, once"),()->{JSONObject q=op("answer");try{q.put("id",permission.optString("id")).put("outcome",outcome);if(current.get().claudeAgent||current.get().environment.equals("codex"))q.put("session",nativeSession).put("nonce",permission.optString("nonce"));}catch(JSONException ignored){}for(int i=0;i<buttons.getChildCount();i++)buttons.getChildAt(i).setEnabled(false);operation(q,()->view.requests.removeAllViews(),()->{if(buttons.getParent()==view.requests)for(int i=0;i<buttons.getChildCount();i++)buttons.getChildAt(i).setEnabled(true);});}));view.requests.addView(buttons);
        }
        if(needsTerminal){view.requests.addView(view.label(tr("Завершите действие в терминале или нажмите «Вернуться в чат ","Finish the action in the terminal or tap Return to ")+name()+tr("»."," chat."),13));view.requests.addView(view.pill(tr("Открыть терминал ›","Open terminal ›"),this::nativeTerminal));view.requests.addView(view.pill(returnChatLabel(),this::returnAgentChat));}
        if(forms!=null)for(int i=0;i<forms.length();i++){JSONObject f=forms.optJSONObject(i);if(f!=null){if(current.get().environment.equals("codex"))view.requests.addView(codexQuestion(f));else view.requests.addView(view.pill(f.optString("title")+" ▾",()->form(f)));}}
        if(current.get().environment.equals("codex")&&questionSupport.equals("not-confirmed"))view.requests.addView(view.label(tr("Вопросы обычного режима не подтверждены этим запуском. Если инструмент недоступен, нужен совместимый CLI и новый запуск агента с продолжением диалога. Plan автоматически не включается.","Ordinary-mode questions are not confirmed by this launch. If unavailable, use a compatible CLI and restart the agent with conversation resume. Plan is never enabled automatically."),12));
        view.scaleContent();
        if(bottom)view.bottomAfterLayout();
    }
    private View codexQuestion(JSONObject f){
        final String key=questionKey(f),session=nativeSession,questionOwner=owner;final int g=generation;
        JSONObject draft=questionDrafts.get(key);if(draft==null){draft=new JSONObject();questionDrafts.put(key,draft);while(questionDrafts.size()>32)questionDrafts.remove(questionDrafts.keySet().iterator().next());}final JSONObject values=draft;
        LinearLayout card=new LinearLayout(activity);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(view.dp(12),view.dp(8),view.dp(12),view.dp(8));card.setBackground(WorkspaceDialogs.background(activity,false));card.setTag("codex-question-"+f.optString("id"));
        TextView status=view.label("",12);JSONArray fields=f.optJSONArray("fields");boolean supported=fields!=null&&fields.length()>0&&fields.length()<=3;
        if(fields!=null)for(int i=0;i<fields.length();i++){
            JSONObject field=fields.optJSONObject(i);if(field==null||!field.optString("type").equals("string")){supported=false;continue;}
            String fieldKey=field.optString("key");card.addView(view.label(field.optString("title"),14));
            RadioGroup group=new RadioGroup(activity);JSONArray options=field.optJSONArray("options");java.util.Map<Integer,String> optionValues=new LinkedHashMap<>();
            if(options!=null)for(int n=0;n<options.length();n++){JSONObject option=options.optJSONObject(n);if(option==null)continue;RadioButton button=new RadioButton(activity);button.setTextColor(WorkspaceDialogs.TEXT);button.setText(option.optString("label"));button.setId(View.generateViewId());button.setTag("question-option-"+fieldKey+"-"+n);button.setSaveEnabled(false);optionValues.put(button.getId(),option.optString("value"));group.addView(button);if(values.optString(fieldKey).equals(option.optString("value")))group.check(button.getId());}
            card.addView(group);EditText custom=new EditText(activity);custom.setTextColor(WorkspaceDialogs.TEXT);custom.setHintTextColor(WorkspaceDialogs.MUTED);custom.setSingleLine(false);custom.setMinLines(1);custom.setMaxLines(4);custom.setSaveEnabled(false);custom.setTag("question-custom-"+fieldKey);custom.setHint(tr("Свой ответ","Your answer"));custom.setImeOptions(android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI);
            boolean free=field.optBoolean("custom")||options==null||options.length()==0;
            if(free){card.addView(custom);if(!optionValues.containsValue(values.optString(fieldKey)))custom.setText(values.optString(fieldKey));}
            group.setOnCheckedChangeListener((selected,checked)->{String value=optionValues.get(checked);if(value!=null){try{values.put(fieldKey,value);}catch(JSONException ignored){}if(custom.length()>0)custom.setText("");}});
            custom.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){if(s.length()>0)group.clearCheck();if(s.length()>0||group.getCheckedRadioButtonId()==-1)try{values.put(fieldKey,s.toString());}catch(JSONException ignored){}}public void afterTextChanged(android.text.Editable e){}});
        }
        card.addView(status);TextView reply=view.pill(tr("Отправить ответ","Send answer"),()->{});reply.setTag("question-send");card.addView(reply);
        final boolean valid=supported;if(!valid){status.setText(tr("Тип вопроса не поддерживается этой версией. Используйте штатный интерфейс Codex; тип выбора не подменён.","This question type is unsupported. Use Codex's native interface; the selection contract was not changed."));reply.setEnabled(false);return card;}
        if(submittingQuestions.contains(key)||f.optBoolean("answering")){questionEnabled(card,false);status.setText(tr("Ответ отправляется; ожидаем подтверждение CLI.","Sending the answer; waiting for CLI acknowledgement."));}
        reply.setOnClickListener(v->{
            if(closed||g!=generation||!questionOwner.equals(owner)||!session.equals(nativeSession)||!activeQuestions.contains(key)){status.setText(tr("Этот вопрос уже закрыт или относится к другой сессии.","This question is closed or belongs to another session."));reply.setEnabled(false);return;}
            if(submittingQuestions.contains(key))return;
            for(int i=0;i<fields.length();i++){String k=fields.optJSONObject(i).optString("key");if(values.optString(k).trim().isEmpty()){status.setText(tr("Выберите вариант или введите ответ на каждый вопрос.","Choose an option or answer every question."));return;}}
            JSONObject q=op("chat-form");try{q.put("session",session).put("id",f.optString("id")).put("nonce",f.optString("nonce")).put("answer",new JSONObject(values.toString()));}catch(JSONException invalid){return;}
            submittingQuestions.add(key);questionEnabled(card,false);status.setText(tr("Ответ отправляется…","Sending answer…"));
            // The CLI's question reply must not queue behind the request waiting for this reply.
            operation(q,()->{submittingQuestions.remove(key);questionDrafts.remove(key);status.setText(tr("CLI закрыл вопрос после отправки ответа.","The CLI resolved the question after the answer was sent."));requestStamp="";},()->{submittingQuestions.remove(key);questionEnabled(card,true);status.setText(tr("Приём не подтверждён. Ответ сохранён. Повтор с тем же вопросом не создаёт новую отправку при неопределённой доставке.","Acceptance unconfirmed. Your answer was kept. Retrying this question does not resend an ambiguously delivered reply."));},()->{submittingQuestions.remove(key);if(!closed)requestStamp="";});
        });return card;
    }
    private void questionEnabled(View node,boolean enabled){node.setEnabled(enabled);if(node instanceof ViewGroup)for(int i=0;i<((ViewGroup)node).getChildCount();i++)questionEnabled(((ViewGroup)node).getChildAt(i),enabled);}
    private void form(JSONObject f){
        final int g=generation;LinearLayout box=new LinearLayout(activity);box.setOrientation(LinearLayout.VERTICAL);JSONArray fields=f.optJSONArray("fields");Map<String,Supplier<Object>> inputs=new LinkedHashMap<>();boolean simple=fields!=null;
        if(fields!=null)for(int i=0;i<fields.length();i++){
            JSONObject field=fields.optJSONObject(i);if(field==null)continue;String type=field.optString("type");
            if(field.has("when")||field.optBoolean("hidden")||(type.equals("multiselect")&&field.optBoolean("custom"))||!Arrays.asList("string","boolean","number","integer","multiselect").contains(type)){simple=false;continue;}
            String key=field.optString("key");box.addView(view.label(field.optString("title",key),14));if(field.has("description"))box.addView(view.label(field.optString("description"),12));
            if(type.equals("boolean")){CheckBox check=new CheckBox(activity);check.setTextColor(0xfff0f0ee);check.setText(tr("Да","Yes"));check.setChecked(field.optBoolean("default"));box.addView(check);inputs.put(key,check::isChecked);continue;}
            JSONArray options=field.optJSONArray("options");
            if(type.equals("multiselect")){
                List<CheckBox> checks=new ArrayList<>();List<String> optionValues=new ArrayList<>();if(options!=null)for(int j=0;j<options.length();j++){JSONObject option=options.optJSONObject(j);if(option==null)continue;CheckBox check=new CheckBox(activity);check.setTextColor(0xfff0f0ee);check.setText(option.optString("label"));check.setSaveEnabled(false);box.addView(check);checks.add(check);optionValues.add(option.optString("value"));}
                inputs.put(key,()->{JSONArray selected=new JSONArray();for(int j=0;j<checks.size();j++)if(checks.get(j).isChecked())selected.put(optionValues.get(j));return selected;});continue;
            }
            if(type.equals("string")&&options!=null){
                List<String> labels=new ArrayList<>(),optionValues=new ArrayList<>();for(int j=0;j<options.length();j++){JSONObject option=options.optJSONObject(j);if(option!=null){labels.add(option.optString("label"));optionValues.add(option.optString("value"));}}
                if(labels.isEmpty()){simple=false;continue;}Spinner pick=new Spinner(activity);pick.setAdapter(new ArrayAdapter<>(activity,android.R.layout.simple_spinner_dropdown_item,labels));int initial=optionValues.indexOf(field.optString("default"));if(initial>=0)pick.setSelection(initial);box.addView(pick);
                EditText custom=new EditText(activity);custom.setTextColor(0xfff0f0ee);custom.setSaveEnabled(false);custom.setHint(tr("Свой ответ (необязательно)","Custom answer (optional)"));if(field.optBoolean("custom"))box.addView(custom);
                inputs.put(key,()->custom.length()>0?custom.getText().toString():optionValues.get(pick.getSelectedItemPosition()));continue;
            }
            EditText input=new EditText(activity);input.setTextColor(0xfff0f0ee);input.setSaveEnabled(false);input.setText(field.optString("default"));if(!type.equals("string"))input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_SIGNED|(type.equals("number")?android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL:0));box.addView(input);
            inputs.put(key,()->type.equals("string")?input.getText().toString():type.equals("integer")?Long.valueOf(input.getText().toString()):Double.valueOf(input.getText().toString()));
        }
        if(!simple){formDialog=WorkspaceDialogs.builder(activity).setTitle(f.optString("title")).setMessage(tr("Этот запрос требует штатного интерфейса ","This request needs the native interface: ")+name()).setPositiveButton(tr("Открыть терминал","Open terminal"),(d,w)->{if(g==generation)nativeTerminal();}).setNegativeButton(tr("Назад","Back"),null).show();return;}
        ScrollView scroll=new ScrollView(activity);scroll.addView(box);AlertDialog dialog=WorkspaceDialogs.builder(activity).setTitle(f.optString("title")).setView(scroll).setPositiveButton(tr("Ответить","Reply"),null).setNegativeButton(tr("Назад","Back"),null).create();formDialog=dialog;dialog.show();dialog.getButton(-1).setOnClickListener(v->{if(g!=generation){dialog.dismiss();return;}JSONObject q=op("chat-form"),answer=new JSONObject();try{for(Map.Entry<String,Supplier<Object>> e:inputs.entrySet())answer.put(e.getKey(),e.getValue().get());q.put("id",f.optString("id")).put("nonce",f.optString("nonce")).put("answer",answer);if(current.get().claudeAgent||current.get().environment.equals("codex")||current.get().aiAgent.equals("opencode"))q.put("session",nativeSession);}catch(Exception invalid){Toast.makeText(activity,tr("Проверьте заполненные поля.","Check the entered values."),Toast.LENGTH_SHORT).show();return;}operation(q,dialog::dismiss);});
    }
    private com.termux.shared.terminal.io.extrakeys.ExtraKeysView createTerminalKeys(){
        com.termux.shared.terminal.io.extrakeys.ExtraKeysView keys=(com.termux.shared.terminal.io.extrakeys.ExtraKeysView)LayoutInflater.from(activity).inflate(R.layout.view_terminal_toolbar_extra_keys,null,false);
        WorkspaceExtraKeysStyle.apply(keys);
        keys.setExtraKeysViewClient(new com.termux.app.terminal.io.TermuxTerminalExtraKeys(activity.getTerminalView(),activity.getTermuxTerminalViewClient(),activity.getTermuxTerminalSessionClient()));
        com.termux.shared.terminal.io.extrakeys.ExtraKeysInfo info=activity.getProperties()==null?null:activity.getProperties().getExtraKeysInfo();
        if(info==null||info.getMatrix().length==0)try{info=new com.termux.shared.terminal.io.extrakeys.ExtraKeysInfo("[['ESC','/','-','HOME','UP','END','PGUP'],['TAB','CTRL','ALT','LEFT','DOWN','RIGHT','PGDN']]","default",com.termux.shared.terminal.io.extrakeys.ExtraKeysConstants.CONTROL_CHARS_ALIASES);}catch(JSONException impossible){throw new IllegalStateException(impossible);}
        keys.setButtonTextAllCaps(activity.getProperties()==null||activity.getProperties().shouldExtraKeysTextBeAllCaps());keys.reload(info);return keys;
    }
    private String returnChatLabel(){return tr("Вернуться в чат ","Return to ")+name()+tr(""," chat");}
    private void returnAgentChat(){
        if(!selected())return;final WorkspaceStore.Session profile=snapshot();final int g=generation;final AgentTerminalScreen opened=terminal;

        String command=profile.environment.equals("codex")||profile.claudeAgent?"terminal-exit":"terminal-close";
        actions.execute(()->{if(closed||g!=generation)return;JSONObject reply=null;try{reply=transport.request(profile,op(command));}catch(Exception ignored){}final JSONObject result=reply;
            handler.post(()->{if(closed||g!=generation)return;if(result!=null&&result.optBoolean("ok")){actionNotice="";if(opened!=null&&terminal==opened)opened.dismiss();requestStamp="";if(!loading)load(false);}else {String notice=result!=null&&result.optString("error").equals("NATIVE_BUSY")?tr("Агент выполняет запрос. Дождитесь завершения и нажмите ещё раз.","The agent is running a request. Wait for completion and tap again."):tr("Не удалось вернуть управление чату. Завершите действие в терминале и повторите.","Could not return control to chat. Finish the terminal action and retry.");if(opened!=null&&terminal==opened)activity.showToast(notice,true);else view.connection(notice);}});
        });
    }
    void terminal(){terminal(false);}
    private int composerCardHeight(){View root=activity.findViewById(com.termux.R.id.activity_termux_root_relative_layout);View card=root==null?null:root.findViewWithTag("composer-card");return card==null?0:card.getHeight();}
    private void nativeTerminal(){terminal(true);}
    private void terminal(boolean prepareNative){
        if(!selected()||terminal!=null)return;sync();final WorkspaceStore.Session terminalOwner=snapshot();codexLoginTerminal=prepareNative&&terminalOwner.environment.equals("codex")&&codexAuthRequired;final int terminalGeneration=generation;
            restoreChatKeys();
            final com.termux.terminal.TerminalSession session=activity.getCurrentSession();
            final boolean keyboardBefore=activity.getTerminalView().isSoftKeyboardInputEnabled();final int insetBefore=activity.getTerminalView().getComposerBottomInset();final int headerBefore=activity.getTerminalView().getHeaderTopInset();final boolean directInput=nativeTerminalInteractive.getOrDefault(terminalOwner.id,true),showKeys=nativeTerminalKeys.getOrDefault(terminalOwner.id,false);activity.getTerminalView().setSoftKeyboardInputEnabled(directInput);
            final View tv=activity.getTerminalView();terminalParent=(ViewGroup)tv.getParent();terminalParams=tv.getLayoutParams();terminalParent.removeView(tv);
            LinearLayout box=new LinearLayout(activity);box.setOrientation(LinearLayout.VERTICAL);terminalAttentionBorder=new WorkspaceTransferBorder(activity.getResources().getDisplayMetrics().density);box.setBackgroundColor(WorkspaceDialogs.BG);terminalAttentionBorder.active(requiresTerminal());final TextView terminalNotice=view.label(tr("Подключение терминала…","Connecting terminal…"),12);terminalNotice.setVisibility(View.GONE);box.addView(terminalNotice,new LinearLayout.LayoutParams(-1,-2));FrameLayout terminalViewport=new FrameLayout(activity);box.addView(terminalViewport,new LinearLayout.LayoutParams(-1,0,1));terminalViewport.addView(tv,new FrameLayout.LayoutParams(-1,-1));LinearLayout floating=new LinearLayout(activity);floating.setOrientation(LinearLayout.VERTICAL);floating.setClickable(true);terminalViewport.addView(floating,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));floating.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->activity.getTerminalView().setComposerBottomInset(floating.getHeight()));
            final com.termux.shared.terminal.io.extrakeys.ExtraKeysView originalKeys=activity.getExtraKeysView();final boolean createdKeys=originalKeys==null||originalKeys.getChildCount()==0;
            final com.termux.shared.terminal.io.extrakeys.ExtraKeysView keys=createdKeys?createTerminalKeys():originalKeys;if(createdKeys)activity.setExtraKeysView(keys);final ViewGroup keysParent=(ViewGroup)keys.getParent();
            final ViewGroup.LayoutParams keysParams=keys==null?null:keys.getLayoutParams();final int keysIndex=keysParent==null?0:keysParent.indexOfChild(keys);final int keysVisibility=keys==null?View.GONE:keys.getVisibility();
            if(keys!=null){if(keysParent!=null)keysParent.removeView(keys);keys.setVisibility(showKeys?View.VISIBLE:View.GONE);int rows=keys.getRowCount();floating.addView(keys,new LinearLayout.LayoutParams(-1,view.dp(Math.max(1,rows)*40)));}
            LinearLayout terminalComposer=new LinearLayout(activity);EditText terminalDraft=new EditText(activity);terminalDraft.setTextColor(WorkspaceDialogs.TEXT);terminalDraft.setTextSize(12);terminalDraft.setMinLines(1);terminalDraft.setMaxLines(6);terminalDraft.setHint(tr("Введите команду…","Enter command…"));terminalDraft.setText(nativeTerminalDrafts.getOrDefault(terminalOwner.id,""));
            TextView library=new TextView(activity),submit=new WorkspaceIconButton(activity);submit.setOnClickListener(v->{if(activity.getCurrentSession()!=session||session==null||!session.isRunning())return;String command=terminalDraft.getText().toString();if(command.isEmpty()){session.write("\r");return;}if(session.getEmulator()!=null){session.getEmulator().paste(command);session.write("\r");}else session.write(command+"\r");terminalDraft.setText("");});
            AgentComposerControls terminalControls=new AgentComposerControls(activity,terminalComposer,terminalDraft,library,submit,new WorkspaceIconButton(activity));
            terminalControls.actions(null,null,()->{},section->terminalLibrary.accept(section,terminalDraft),enabled->{nativeTerminalInteractive.put(terminalOwner.id,enabled);activity.getTerminalView().setSoftKeyboardInputEnabled(enabled);if(enabled)tv.requestFocus();else terminalDraft.requestFocus();},enabled->{nativeTerminalKeys.put(terminalOwner.id,enabled);keys.setVisibility(enabled?View.VISIBLE:View.GONE);});terminalControls.present(false);terminalControls.minimumCardHeight(composerCardHeight());terminalControls.terminalModes(directInput,showKeys);floating.addView(terminalComposer,new LinearLayout.LayoutParams(-1,-2));
            terminal=new AgentTerminalScreen(activity,terminalOwner.name,terminalOwner.directory,this::terminalBack);terminal.body(box);terminal.headerInset(pixels->activity.getTerminalView().setHeaderTopInset(pixels));
            terminal.setOnDismissListener(()->{nativeTerminalDrafts.put(terminalOwner.id,terminalDraft.getText().toString());if(preparationWatcher!=null){preparationWatcher.stopWatching();preparationWatcher=null;}if(terminalAttentionBorder!=null)terminalAttentionBorder.active(false);terminalAttentionBorder=null;ViewGroup parent=(ViewGroup)tv.getParent();if(parent!=null)parent.removeView(tv);terminalParent.addView(tv,0,terminalParams);activity.getTerminalView().setComposerBottomInset(insetBefore);activity.getTerminalView().setHeaderTopInset(headerBefore);if(keys!=null){ViewGroup kp=(ViewGroup)keys.getParent();if(kp!=null)kp.removeView(keys);if(keysParent!=null)keysParent.addView(keys,Math.min(keysIndex,keysParent.getChildCount()),keysParams);keys.setVisibility(keysVisibility);if(createdKeys&&activity.getExtraKeysView()==keys)activity.setExtraKeysView(originalKeys);}activity.getTerminalView().setSoftKeyboardInputEnabled(keyboardBefore);terminal=null;codexLoginTerminal=false;activity.getTerminalView().setWorkspaceTerminalVisible(false);if(prepareNative)actions.execute(()->{try{transport.request(terminalOwner,op("terminal-close"));}catch(Exception ignored){}});sync();});
            terminal.show();tv.setVisibility(View.VISIBLE);activity.getTerminalView().setWorkspaceTerminalVisible(true);tv.requestFocus();view.setVisibility(View.GONE);applyPresentationScale();syncExtraKeys();visibilityChanged.run();
            final AgentTerminalScreen opened=terminal;
            if(preparationWatcher!=null)preparationWatcher.stopWatching();
            if(AgentPreparation.supports(terminalOwner)){
                preparationWatcher=new android.os.FileObserver(SessionShell.directory(activity.getFilesDir(),terminalOwner.id).getAbsolutePath(),android.os.FileObserver.MOVED_TO|android.os.FileObserver.CLOSE_WRITE){
                    public void onEvent(int event,String path){if(!"agent-preparation-status".equals(path)||closed)return;try{actions.execute(()->{String message=AgentPreparation.status(activity.getFilesDir(),terminalOwner,WorkspaceLanguage.code(activity).equals("ru"));handler.post(()->{if(!closed&&terminalGeneration==generation&&terminal==opened&&opened.isShowing()&&!message.isEmpty()){terminalNotice.setText(message);terminalNotice.setVisibility(View.VISIBLE);}});});}catch(java.util.concurrent.RejectedExecutionException ignored){}}
                };preparationWatcher.startWatching();
            }

            if(prepareNative)actions.execute(()->{if(closed||terminalGeneration!=generation)return;boolean ok=false;String notice="";try{JSONObject response=transport.request(terminalOwner,op("terminal-open"));ok=response.optBoolean("ok")&&response.optBoolean("terminalReady",true);notice=response.optString("terminalNotice");}catch(Exception ignored){}final boolean ready=ok;final String explanation=notice.length()>600?notice.substring(0,600):notice;final String preparation=AgentPreparation.status(activity.getFilesDir(),terminalOwner,WorkspaceLanguage.code(activity).equals("ru"));handler.post(()->{if(closed||terminalGeneration!=generation||terminal!=opened||!opened.isShowing())return;String nativeHint="";terminalNotice.setText(ready?nativeHint:!preparation.isEmpty()?preparation:!explanation.isEmpty()?explanation:tr("Канал управления пока недоступен. Ниже — терминал текущей сессии: проверьте запуск агента или вход.","The control channel is not available yet. The current session terminal is shown below: check agent startup or sign-in."));terminalNotice.setVisibility(ready&&nativeHint.isEmpty()?View.GONE:View.VISIBLE);});});
    }
    void close(){for(AgentProjectPanel.Chat source:new java.util.ArrayList<>(projectReaders))source.close();closed=true;if(preparationWatcher!=null){preparationWatcher.stopWatching();preparationWatcher=null;}if(stopConfirmation!=null)stopConfirmation.dismiss();applyPresentationScale();attachments.close();cache.clear();restoreChatKeys();syncExtraKeys();generation++;handler.removeCallbacks(poll);if(terminal!=null)terminal.dismissImmediately();if(details!=null)details.dismiss();if(formDialog!=null)formDialog.dismiss();if(vertexDialog!=null)vertexDialog.dismiss();reads.shutdownNow();actions.shutdown();surface.removeView(view);}
}
