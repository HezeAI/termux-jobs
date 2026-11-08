package com.termux.app.workspace;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.util.function.Consumer;

/** Agent-neutral Android transcript. Updates rows by stable identity, never rebuilds the terminal. */
final class AgentChatView extends LinearLayout {
    final ScrollView scroll;
    private final LinearLayout footer;
    private int composerInset,headerInset;
    final LinearLayout timeline,requests,keysHost,attachmentDrafts;
    final WorkspaceIconButton folder;
    java.util.function.BiConsumer<JSONObject,ImageView> thumbnail=(a,v)->{};
    java.util.function.BiConsumer<JSONObject,TextView> apkDetails=(a,v)->{};
    final TextView status,older,connection;
    private String assistant="AI";
    private boolean codexActions,groupAllTools;
    void actionCards(boolean allTools){codexActions=true;groupAllTools=allTools;}
    void codexActions(boolean enabled){codexActions=enabled;}
    void assistantName(String name){assistant=name;}

    private final Map<String,View> views=new LinkedHashMap<>();
    private final Map<String,String> values=new HashMap<>();
    private final Map<String,JSONObject> data=new HashMap<>();
    private final Consumer<JSONObject> open;
    private io.noties.markwon.Markwon markdown;
    java.util.function.Function<String,Runnable> localFileAction=path->null;
    java.util.function.Function<JSONObject,Runnable> fileCardAction=attachment->null;
    io.noties.markwon.Markwon fileMarkdown(){return AgentMarkdown.create(getContext(),localFileAction);}
    final LinkedHashMap<String,Boolean> timelineExpansion=new LinkedHashMap<>();
    String choiceKey(JSONObject row){return row.optString("actionId").isEmpty()?row.optString("key"):"action:"+row.optString("actionId");}
    void rememberActions(Collection<JSONObject> rows,boolean expanded){for(JSONObject row:rows){String key=choiceKey(row);if(!key.isEmpty()){timelineExpansion.remove(key);timelineExpansion.put(key,expanded);}}while(timelineExpansion.size()>1024)timelineExpansion.remove(timelineExpansion.keySet().iterator().next());}
    Boolean actionChoice(JSONArray rows){Set<String> keys=new HashSet<>();for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row!=null)keys.add(choiceKey(row));}Boolean choice=null;for(Map.Entry<String,Boolean> entry:timelineExpansion.entrySet())if(keys.contains(entry.getKey()))choice=entry.getValue();return choice;}
    private boolean touchingTimeline;
    private boolean seenLiveSnapshot;
    private final Set<String> arrivalSeen=new HashSet<>(),arrivalActions=new HashSet<>();
    private final Map<View,android.animation.ValueAnimator> landings=new HashMap<>();
    private void land(View item){
        if(item==null||landings.containsKey(item)||!isAttachedToWindow()||!isShown()||getWindowVisibility()!=VISIBLE||!android.animation.ValueAnimator.areAnimatorsEnabled())return;
        android.animation.ValueAnimator animation=android.animation.ValueAnimator.ofFloat(0,1);landings.put(item,animation);
        item.setPivotY(0);item.setTranslationY(dp(10));item.setScaleX(.985f);item.setScaleY(.985f);item.setAlpha(0);animation.setDuration(520);animation.setInterpolator(new android.view.animation.LinearInterpolator());
        animation.addUpdateListener(frame->{float progress=(float)frame.getAnimatedValue();for(Map.Entry<View,android.animation.ValueAnimator> entry:landings.entrySet())if(entry.getValue()==animation)landingFrame(entry.getKey(),progress);});
        animation.addListener(new android.animation.AnimatorListenerAdapter(){@Override public void onAnimationEnd(android.animation.Animator a){for(View target:new ArrayList<>(landings.keySet()))if(landings.get(target)==animation){landings.remove(target);landingFrame(target,1);}}});animation.start();
    }
    private void landingFrame(View item,float progress){float eased=new android.view.animation.OvershootInterpolator(.45f).getInterpolation(progress);item.setPivotY(0);item.setAlpha(Math.min(1,progress*2));item.setTranslationY(dp(10)*(1-eased));item.setScaleX(.985f+.015f*eased);item.setScaleY(.985f+.015f*eased);}
    private void inheritLanding(View from,View to){
        android.animation.ValueAnimator animation=landings.get(from);if(to==null||animation==null||landings.containsKey(to))return;
        int height=views.containsValue(from)?0:from.getHeight();
        if(to instanceof AgentLinkTextView)((AgentLinkTextView)to).seedCardHeight(height);
        else if(to instanceof AgentAttachmentView)((AgentAttachmentView)to).size.seed(height);
        else if(to instanceof CodexActionsView)((CodexActionsView)to).seedCardHeight(height);
        landings.put(to,animation);landingFrame(to,(float)animation.getAnimatedValue());
    }
    private void finishArrivals(){if(landings==null)return;for(android.animation.ValueAnimator animation:new HashSet<>(landings.values()))animation.cancel();}

    @Override protected void onVisibilityChanged(View changed,int visibility){super.onVisibilityChanged(changed,visibility);if(!isShown()){finishArrivals();seenLiveSnapshot=false;}}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(visibility!=VISIBLE){finishArrivals();seenLiveSnapshot=false;}}

    void historyBaseline(){seenLiveSnapshot=false;finishArrivals();}
    @Override protected void onDetachedFromWindow(){finishArrivals();seenLiveSnapshot=false;super.onDetachedFromWindow();}
    void preserveTimelinePosition(){if(!touchingTimeline&&!((AgentManualScroll)scroll).manualScrolling()&&restoring==null)queuePosition(position());}
    private void revealLayout(){if(!touchingTimeline&&position().bottom){followLayout=true;bottomAfterLayout();}}
    void liveRows(List<JSONObject> rows,boolean older,boolean busy){boolean animate=seenLiveSnapshot;seenLiveSnapshot=true;rows(rows,older,animate);}
    final AgentProgressView execution;
    private int scalePercent=100;
    int scalePercent(){return scalePercent;}
    void scale(int percent){percent=AgentChatScale.clamp(percent);if(scalePercent==percent)return;Position held=position();float ratio=(float)percent/scalePercent;scalePercent=percent;scaleContent();queuePosition(new Position(held.anchor,Math.round(held.offset*ratio),held.y,held.bottom,held.step));}
    void scaleContent(){AgentChatScale.apply(timeline,scalePercent);AgentChatScale.apply(attachmentDrafts,scalePercent);updateHeaderInset();updateComposerInset();}

    private boolean followNextRows;
    private boolean followLayout;
    static final class Position {final String anchor,step;final int offset,y;final boolean bottom;Position(String anchor,int offset,int y,boolean bottom){this(anchor,offset,y,bottom,null);}Position(String anchor,int offset,int y,boolean bottom,String step){this.anchor=anchor;this.offset=offset;this.y=y;this.bottom=bottom;this.step=step;}}
    private Position restoring;
    private ViewTreeObserver restoreObserver;private ViewTreeObserver.OnPreDrawListener restoreListener;
    private void cancelRestore(){restoring=null;if(restoreObserver!=null&&restoreObserver.isAlive()&&restoreListener!=null)restoreObserver.removeOnPreDrawListener(restoreListener);restoreObserver=null;restoreListener=null;}
    Position position(){
        if(restoring!=null)return restoring;
        String anchor=null;int offset=0;
        for(int i=1;i<timeline.getChildCount();i++){View v=timeline.getChildAt(i);if(v.getBottom()>scroll.getScrollY()){for(Map.Entry<String,View> e:views.entrySet())if(e.getValue()==v){anchor=e.getKey();offset=v.getTop()-scroll.getScrollY();break;}if(anchor!=null)break;}}
        String step=null;View anchored=anchor==null?null:views.get(anchor);if(anchored instanceof CodexActionsView){CodexActionsView card=(CodexActionsView)anchored;String visible=card.readingStep(scroll.getScrollY()-card.getTop());if(visible!=null){step=visible;offset=card.getTop()+card.stepTop(visible)-scroll.getScrollY();}}
        return new Position(anchor,offset,scroll.getScrollY(),timeline.getHeight()-scroll.getHeight()-scroll.getScrollY()<dp(48),step);
    }
    void restorePosition(Position p){followNextRows=false;followLayout=false;queuePosition(p);}
    private void queuePosition(Position p){
        cancelRestore();restoring=p;if(p==null)return;
        restoreObserver=scroll.getViewTreeObserver();restoreListener=()->{restorePositionAfterLayout();return true;};restoreObserver.addOnPreDrawListener(restoreListener);
        // Fallback for hosts laid out off screen. Pre-draw settles the visible frame
        // before any new row can be painted at the old scroll position.
        scroll.post(this::restorePositionAfterLayout);
    }
    private void restorePositionAfterLayout(){
        if(restoring==null||timeline.getWidth()==0||scroll.getHeight()==0||timeline.isLayoutRequested()||scroll.isLayoutRequested())return;
        Position p=restoring;cancelRestore();View anchor=p.anchor==null?null:views.get(p.anchor);
        Integer actionTop=null;if(p.step!=null)for(View view:views.values())if(view instanceof CodexActionsView){int top=((CodexActionsView)view).stepTop(p.step);if(top>=0){actionTop=view.getTop()+top;break;}}
        scroll.scrollTo(0,p.bottom?Math.max(0,timeline.getHeight()-scroll.getHeight()):actionTop!=null?actionTop-p.offset:anchor==null?p.y:anchor.getTop()-p.offset);
    }
    // ScrollView.fullScroll also moves keyboard focus to a transcript child.
    // Streaming must only move the viewport, leaving both composers and IME composition alone.
    void bottomAfterLayout(){queuePosition(new Position(null,0,0,true));}
    void followAfterSend(){followNextRows=true;followLayout=true;bottomAfterLayout();}
    AgentChatView(Context c,Consumer<JSONObject> open,Runnable loadOlder,Runnable sendShell,Runnable terminal,Runnable interrupt){
        super(c);this.open=open;setOrientation(VERTICAL);setBackgroundColor(c.getColor(com.termux.R.color.workspace_chat_background));
        status=label("",12);status.setMaxLines(2);status.setEllipsize(android.text.TextUtils.TruncateAt.END);status.setTag("agent-model-connection");status.setVisibility(GONE);status.setBackground(WorkspaceDialogs.background(c,false));
        scroll=new AgentManualScroll(c){
            @Override public boolean dispatchTouchEvent(MotionEvent e){
                // Selectable TextViews may consume DOWN before a parent's OnTouchListener.
                // Observe dispatch without intercepting selection or the ScrollView gesture.
                touchingTimeline=e.getActionMasked()!=MotionEvent.ACTION_UP&&e.getActionMasked()!=MotionEvent.ACTION_CANCEL;
                cancelRestore();followNextRows=false;followLayout=false;
                return super.dispatchTouchEvent(e);
            }
        };scroll.setFillViewport(true);
        scroll.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(restoring==null&&followLayout)bottomAfterLayout();});scroll.setSmoothScrollingEnabled(false);
        timeline=new AgentTimelineLayout(c);timeline.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(followLayout){followLayout=false;if(restoring==null)bottomAfterLayout();}});timeline.setPadding(dp(12),dp(4),dp(12),dp(16));scroll.addView(timeline);
        older=pill(tr("Предыдущие сообщения","Earlier messages"),loadOlder);timeline.addView(older);
        FrameLayout body=new FrameLayout(c);body.addView(scroll,new FrameLayout.LayoutParams(-1,-1));body.addView(status,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP));status.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->updateHeaderInset());addView(body,new LayoutParams(-1,0,1));footer=new LinearLayout(c);footer.setOrientation(VERTICAL);body.addView(footer,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));footer.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->updateComposerInset());requests=new LinearLayout(c);requests.setOrientation(VERTICAL);requests.setPadding(dp(12),0,dp(12),0);timeline.addView(requests,new LayoutParams(-1,-2));connection=label("",13);connection.setVisibility(GONE);requests.addView(connection);execution=new AgentProgressView(c);timeline.addView(execution,new LayoutParams(-1,-2));
        attachmentDrafts=new LinearLayout(c);attachmentDrafts.setOrientation(VERTICAL);attachmentDrafts.setVisibility(GONE);footer.addView(attachmentDrafts,new LayoutParams(-1,-2));
        folder=new WorkspaceIconButton(c);folder.setIcon(c.getDrawable(com.termux.R.drawable.workspace_paperclip),dp(24));folder.setTextColor(WorkspaceDialogs.ACCENT);folder.setContentDescription(tr("Прикрепить файл","Attach file"));folder.setTooltipText(tr("Прикрепить файл","Attach file"));folder.setVisibility(GONE);

        keysHost=new LinearLayout(c);keysHost.setOrientation(VERTICAL);keysHost.setVisibility(GONE);footer.addView(keysHost,new LayoutParams(-1,-2));

    }
    void headerInset(int pixels){headerInset=Math.max(0,pixels);updateHeaderInset();}
    void updateHeaderInset(){
        FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)status.getLayoutParams();
        if(p.topMargin!=headerInset){p.topMargin=headerInset;status.setLayoutParams(p);}
        int padding=dp(4)+headerInset+(status.getVisibility()==VISIBLE?status.getHeight():0);
        if(timeline.getPaddingTop()==padding)return;
        Position held=position();boolean top=held.y==0&&!held.bottom;
        timeline.setPadding(timeline.getPaddingLeft(),padding,timeline.getPaddingRight(),timeline.getPaddingBottom());
        queuePosition(top?new Position(null,0,0,false):held);
    }
    void composerInset(int pixels){composerInset=Math.max(0,pixels);updateComposerInset();}
    private void updateComposerInset(){
        int padding=dp(16)+composerInset+footer.getHeight();
        FrameLayout.LayoutParams f=(FrameLayout.LayoutParams)footer.getLayoutParams();
        if(f.bottomMargin!=composerInset){f.bottomMargin=composerInset;footer.setLayoutParams(f);}
        if(timeline.getPaddingBottom()==padding)return;
        Position held=position();timeline.setPadding(timeline.getPaddingLeft(),timeline.getPaddingTop(),timeline.getPaddingRight(),padding);queuePosition(held);
    }
    void attachmentPicker(boolean enabled){folder.setVisibility(enabled?VISIBLE:GONE);}
    void connection(String message){connection(message,null);}
    void connection(String message,Runnable retry){connection.setOnClickListener(retry==null?null:v->retry.run());connection.setClickable(retry!=null);if(connection.getText().toString().equals(message)&&connection.getVisibility()==(message.isEmpty()?GONE:VISIBLE))return;boolean bottom=timeline.getHeight()-scroll.getHeight()-scroll.getScrollY()<dp(48);connection.setText(message);connection.setVisibility(message.isEmpty()?GONE:VISIBLE);if(bottom){followLayout=true;bottomAfterLayout();}}
    void clearRequests(){requests.removeAllViews();requests.addView(connection);}
    String tr(String ru,String en){return WorkspaceLanguage.code(getContext()).equals("ru")?ru:en;}
    int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    TextView label(String s,int size){TextView t=new AgentLinkTextView(getContext());t.setText(s);t.setTextSize(size);t.setTextColor(0xfff0f0ee);t.setPadding(dp(10),dp(8),dp(10),dp(8));return t;}
    TextView pill(String s,Runnable action){TextView t=label(s,12);GradientDrawable bg=new GradientDrawable();bg.setColor(0xff232426);bg.setCornerRadius(dp(22));bg.setStroke(dp(1),0xff393a3c);t.setBackground(bg);t.setOnClickListener(v->action.run());LayoutParams lp=new LayoutParams(-2,-2);lp.setMargins(dp(3),dp(5),dp(3),dp(5));t.setLayoutParams(lp);t.setMinHeight(dp(36));return t;}
    void olderState(boolean loading,String error){older.setEnabled(!loading);older.setText(loading?tr("Загрузка сообщений…","Loading messages…"):error!=null?error:tr("Предыдущие сообщения","Earlier messages"));}
    void olderRows(List<JSONObject> rows,boolean hasOlder){boolean fits=timeline.getHeight()<=scroll.getHeight();Position held=position();rows(rows,hasOlder);restorePosition(fits?new Position(null,0,0,false):new Position(held.anchor,held.offset,held.y,false,held.step));}
    boolean canReveal(String key){return views.containsKey(key);}
    boolean reveal(String key){if(!views.containsKey(key))return false;restorePosition(new Position(key,headerInset,0,false));return true;}
    void clear(){((AgentManualScroll)scroll).hideIndicator();finishArrivals();arrivalSeen.clear();arrivalActions.clear();seenLiveSnapshot=false;timelineExpansion.clear();olderState(false,null);cancelRestore();touchingTimeline=false;followNextRows=false;followLayout=false;views.clear();values.clear();data.clear();timeline.removeViews(1,timeline.getChildCount()-1);clearRequests();timeline.addView(requests);timeline.addView(execution);scroll.scrollTo(0,0);}
    void rows(List<JSONObject> rows,boolean hasOlder){rows(rows,hasOlder,false);}
    private void rows(List<JSONObject> rows,boolean hasOlder,boolean animate){
        rows=AgentMessageTime.captions(rows);
        if(codexActions)rows=CodexActionTimeline.group(rows,groupAllTools);
        Position held=position();final boolean bottom=!touchingTimeline&&!((AgentManualScroll)scroll).manualScrolling()&&(followNextRows||held.bottom);
        Set<String> incomingGroups=new HashSet<>();for(JSONObject row:rows)if(row.optString("kind").equals("actionGroup"))incomingGroups.add(row.optString("key"));
        Map<String,View> priorViews=new HashMap<>(views),priorActions=new HashMap<>();
        for(Map.Entry<String,JSONObject> entry:data.entrySet()){JSONObject row=entry.getValue();View previous=views.get(entry.getKey());if(previous==null)continue;if(row.optString("kind").equals("tool"))priorActions.put(choiceKey(row),previous);JSONArray steps=row.optJSONArray("steps");if(steps!=null)for(int n=0;n<steps.length();n++)if(steps.optJSONObject(n)!=null)priorActions.put(choiceKey(steps.optJSONObject(n)),previous);}
        boolean changed=false;Set<String> present=new HashSet<>();Set<String> arriving=new HashSet<>();int index=1;
        for(JSONObject r:rows){String key=r.optString("key");String arrivalType=r.optString("kind");if(!arrivalType.equals("completionTime")){String identity=arrivalType+":"+(arrivalType.equals("user")?(r.optString("message").isEmpty()?key:r.optString("message")):key);boolean fresh=arrivalSeen.add(identity);if(arrivalType.equals("actionGroup")){JSONArray steps=r.optJSONArray("steps");if(steps!=null)for(int i=0;i<steps.length();i++){JSONObject step=steps.optJSONObject(i);if(step!=null&&!arrivalActions.add(choiceKey(step)))fresh=false;}}if(arrivalType.equals("tool")&&!arrivalActions.add(choiceKey(r)))fresh=false;if(fresh&&animate)arriving.add(key);}present.add(key);data.put(key,r);String value=r.toString()+(codexActions?WorkspaceLanguage.code(getContext()):"");
            if(r.optString("kind").equals("completionTime")){
                TextView caption=views.get(key) instanceof TextView?(TextView)views.get(key):label("",11);
                String signature=value+java.util.TimeZone.getDefault().getID()+WorkspaceLanguage.code(getContext())+android.text.format.DateFormat.is24HourFormat(getContext());
                if(!signature.equals(values.get(key))){changed=true;AgentChatScale.prepare(caption);values.put(key,signature);caption.setText(AgentMessageTime.caption(getContext(),r.optJSONObject("requestSummary")));caption.setTextColor(WorkspaceDialogs.MUTED);caption.setGravity(Gravity.END);caption.setSingleLine(false);caption.setTag("completion-"+r.optString("message"));caption.setContentDescription(tr("Ответ завершён: ","Response completed: ")+AgentMessageTime.full(getContext(),r.optJSONObject("requestSummary").optLong("completedAt")));caption.setOnClickListener(v->open.accept(r));LayoutParams lp=new LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(6));caption.setLayoutParams(lp);}
                views.put(key,caption);if(caption.getParent()==null){changed=true;timeline.addView(caption,index);}else if(timeline.indexOfChild(caption)!=index){changed=true;((AgentTimelineLayout)timeline).move(caption,index);}index++;continue;
            }
            if(r.optString("kind").equals("actionGroup")){
                value+=java.util.TimeZone.getDefault().getID();
                View existing=views.get(key);
                if(existing==null)for(String prior:new ArrayList<>(views.keySet())){View candidate=views.get(prior);if(!incomingGroups.contains(prior)&&candidate instanceof CodexActionsView&&((CodexActionsView)candidate).sharesActions(r)){existing=candidate;views.remove(prior);values.remove(prior);data.remove(prior);break;}}
                CodexActionsView card=existing instanceof CodexActionsView?(CodexActionsView)existing:new CodexActionsView(this,open);
                views.put(key,card);
                if(!value.equals(values.get(key))){changed=true;values.put(key,value);AgentChatScale.prepare(card);card.smoothResize=animate||landings.containsKey(card);card.update(r);}
                if(card.getParent()==null){changed=true;timeline.addView(card,index);}else if(timeline.indexOfChild(card)!=index){changed=true;((AgentTimelineLayout)timeline).move(card,index);}index++;continue;
            }
            if(r.optString("kind").equals("attachment")){
                View existing=views.get(key);AgentAttachmentView card=existing instanceof AgentAttachmentView?(AgentAttachmentView)existing:new AgentAttachmentView(this);card.size.enabled=animate||landings.containsKey(card);
                if(!value.equals(values.get(key))){changed=true;values.put(key,value);AgentChatScale.prepare(card);card.removeAllViews();card.setOrientation(VERTICAL);JSONObject a=r.optJSONObject("attachment");
                    if(a!=null&&(a.optString("type").equals("apk")||a.optString("type").equals("binary")||a.optString("type").equals("file"))){
                        boolean isApk=a.optString("type").equals("apk");
                        card.setOrientation(HORIZONTAL);card.setGravity(Gravity.CENTER_VERTICAL);
                        LinearLayout badge=new LinearLayout(getContext());badge.setOrientation(VERTICAL);badge.setGravity(Gravity.CENTER);
                        ImageView icon=new ImageView(getContext());icon.setImageResource(isApk?com.termux.R.drawable.workspace_apk:com.termux.R.drawable.workspace_server_file);badge.addView(icon,new LayoutParams(dp(36),dp(36)));
                        TextView apk=label(isApk?"APK":ChatAttachments.badge(a.optString("name")),11);apk.setGravity(Gravity.CENTER);badge.addView(apk);card.addView(badge,new LayoutParams(dp(72),-2));
                        LinearLayout info=new LinearLayout(getContext());info.setOrientation(VERTICAL);TextView filename=label(a.optString("name","APK"),13);filename.setMaxLines(2);info.addView(filename);
                        TextView detail=label(android.text.format.Formatter.formatFileSize(getContext(),a.optLong("bytes"))+(isApk?" · SHA-256: "+tr("подготовка…","preparing…"):""),11);info.addView(detail);card.addView(info,new LayoutParams(0,-2,1));if(isApk)apkDetails.accept(a,detail);
                    }else if(a!=null){TextView title=label(a.optString("name",tr("Вложение","Attachment")),13);card.addView(title);
                        String type=a.optString("type");if(type.equals("image")||type.equals("video")){ImageView image=new ImageView(getContext());image.setScaleType(ImageView.ScaleType.FIT_CENTER);image.setBackgroundColor(0xff242527);image.setContentDescription(title.getText());card.addView(image,new LayoutParams(-1,dp(160)));thumbnail.accept(a,image);}
                        TextView hint=label(type.equals("video")?tr("Видео · выбранные кадры, без звука · открыть","Video · sampled frames, no audio · open"):tr("Нажмите, чтобы открыть","Tap to open"),11);card.addView(hint);
                    }
                    if(r.optJSONObject("deliveryReceipt")!=null){LinearLayout captionHost=card.getOrientation()==HORIZONTAL&&card.getChildAt(card.getChildCount()-1) instanceof LinearLayout?(LinearLayout)card.getChildAt(card.getChildCount()-1):card;captionHost.addView(deliveryCaption(r));}
                    GradientDrawable bg=new GradientDrawable();bg.setColor(0xff232426);bg.setCornerRadius(dp(14));bg.setStroke(dp(1),0xff393a3c);card.setBackground(bg);LayoutParams lp=new LayoutParams(-1,-2);lp.setMargins(0,dp(6),0,dp(8));card.setLayoutParams(lp);final String k=key;final Runnable fileAction=a==null?null:fileCardAction.apply(a);card.setOnClickListener(v->{if(fileAction!=null){fileAction.run();return;}JSONObject row=data.get(k);if(row!=null)open.accept(row);});
                }
                views.put(key,card);if(card.getParent()==null){changed=true;timeline.addView(card,index);}else if(timeline.indexOfChild(card)!=index){changed=true;((AgentTimelineLayout)timeline).move(card,index);}index++;continue;
            }
            if(r.optString("kind").equals("text")&&AgentMarkdown.rich(r.optString("body"))){
                if(markdown==null)markdown=fileMarkdown();View existing=views.get(key);AgentMarkdownView block=existing instanceof AgentMarkdownView?(AgentMarkdownView)existing:new AgentMarkdownView(getContext(),markdown);
                if(!value.equals(values.get(key))||existing!=block){changed=true;values.put(key,value);AgentChatScale.prepare(block);final String k=key;block.animation(animate,this::revealLayout);block.update(assistant,r.optString("body"),r.optBoolean("more")?()->{JSONObject row=data.get(k);if(row!=null)open.accept(row);}:null);}
                if(existing!=null&&existing!=block){timeline.removeView(existing);}views.put(key,block);
                if(block.getParent()==null){changed=true;timeline.addView(block,index);}else if(timeline.indexOfChild(block)!=index){changed=true;((AgentTimelineLayout)timeline).move(block,index);}index++;continue;
            }
            if(r.optString("kind").equals("user")){
                View old=views.get(key);AgentBubble bubble=old instanceof AgentBubble?(AgentBubble)old:new AgentBubble(getContext());
                if(!value.equals(values.get(key))||old!=bubble){changed=true;values.put(key,value);AgentChatScale.prepare(bubble);
                    TextView text=bubble.getChildCount()>0?(TextView)bubble.getChildAt(0):label("",14);if(text.getParent()==null)bubble.addView(text,new LayoutParams(-2,-2));
                    String shown=r.optString("body");if(r.optBoolean("queued"))shown=tr("В очереди · ","Queued · ")+shown;else if(r.optString("delivery").equals("cancelled"))shown=tr("Отменено · ","Cancelled · ")+shown;else if(java.util.Arrays.asList("refused","discarded").contains(r.optString("delivery")))shown=tr("Отклонено CLI · ","Rejected by CLI · ")+shown;else if(r.optJSONObject("deliveryReceipt")==null&&java.util.Arrays.asList("started","current","completed").contains(r.optString("delivery")))shown=tr("Принято CLI · ","Accepted by CLI · ")+shown;
                    shown+=r.optBoolean("more")?tr("\nНажмите для полного текста…","\nTap for full text…"):"";if(!shown.contentEquals(text.getText()))text.setText(shown);text.setTextIsSelectable(true);text.setOnClickListener(r.optBoolean("more")?v->{JSONObject latest=data.get(key);if(latest!=null)open.accept(latest);}:null);
                    if(bubble.getChildCount()>1)bubble.removeViewAt(1);if(r.optJSONObject("deliveryReceipt")!=null)bubble.addView(deliveryCaption(r),new LayoutParams(-2,-2));
                    GradientDrawable bg=new GradientDrawable();bg.setColor(WorkspaceDialogs.USER);bg.setCornerRadius(dp(18));bubble.setBackground(bg);LayoutParams lp=new LayoutParams(-2,-2);lp.gravity=Gravity.END;lp.setMargins(0,dp(6),0,dp(10));bubble.setLayoutParams(lp);
                }
                if(old!=null&&old!=bubble)timeline.removeView(old);views.put(key,bubble);
                if(bubble.getParent()==null){changed=true;timeline.addView(bubble,index);}else if(timeline.indexOfChild(bubble)!=index){changed=true;((AgentTimelineLayout)timeline).move(bubble,index);}index++;continue;
            }
            View previous=views.get(key);if(previous!=null&&!(previous instanceof TextView)){timeline.removeView(previous);views.remove(key);values.remove(key);}
            TextView t=(TextView)views.get(key);
            if(t==null){t=label("",14);views.put(key,t);}if(!r.optString("kind").equals("text"))((AgentLinkTextView)t).smoothSize(this,animate||landings.containsKey(t));
            if(!value.equals(values.get(key))){changed=true;values.put(key,value);AgentChatScale.prepare(t);String kind=r.optString("kind");boolean error=kind.equals("error");boolean chip=kind.equals("reasoning")||kind.equals("tool")||kind.equals("receipt");
                String caption=kind.equals("reasoning")?tr("Рассуждение ▾","Thinking ▾"):kind.equals("receipt")?receiptTitle(r):(codexActions?CodexActionLabels.title(r,WorkspaceLanguage.code(getContext()).equals("ru")):r.optString("title"))+" ▾";
                String body=r.optString("actionOutcome").equals("user_declined")?tr("Действие отклонено","Action declined"):r.optString("body");if(r.optBoolean("queued"))body=tr("В очереди · ","Queued · ")+body;else if(r.optString("delivery").equals("cancelled"))body=tr("Отменено · ","Cancelled · ")+body;else if(java.util.Arrays.asList("refused","discarded").contains(r.optString("delivery")))body=tr("Отклонено CLI · ","Rejected by CLI · ")+body;else if(java.util.Arrays.asList("started","current","completed").contains(r.optString("delivery")))body=tr("Принято CLI · ","Accepted by CLI · ")+body;String shown=AgentChatErrors.nativeDiagnosticHint(body)?tr("Подробности доступны по нажатию на сообщение об ошибке.","Tap the error message for details."):body;
                if(!kind.equals("text"))t.setText(error?tr("Ошибка · ","Error · ")+assistant+"\n"+AgentChatErrors.explain(body,WorkspaceLanguage.code(getContext()).equals("ru"))+tr("\nПодробности ▾","\nDetails ▾"):chip?caption:(kind.equals("user")?tr("Вы: ","You: "):kind.equals("text")?assistant+": ":"")+shown+(r.optBoolean("more")?tr("\nНажмите для полного текста…","\nTap for full text…"):""));
                t.setTextIsSelectable(!chip&&!error);
                if(kind.equals("text")){if(markdown==null)markdown=fileMarkdown();android.text.SpannableStringBuilder formatted=new android.text.SpannableStringBuilder(assistant+": ");formatted.append(markdown.toMarkdown(AgentMarkdown.checkboxes(shown)));if(r.optBoolean("more"))formatted.append(tr("\nНажмите для полного текста…","\nTap for full text…"));((AgentLinkTextView)t).animateNextText(animate,this::revealLayout);markdown.setParsedMarkdown(t,formatted);}
                final String k=key;t.setOnClickListener(error||chip||r.optBoolean("more")?v->{JSONObject row=data.get(k);if(row!=null)open.accept(row);}:null);t.setTextSize(chip?12:14);t.setTypeface(chip?Typeface.DEFAULT:Typeface.DEFAULT);
                GradientDrawable bg=new GradientDrawable();bg.setColor(error?0xff241416:chip?0xff232426:android.graphics.Color.BLACK);bg.setCornerRadius(error?dp(12):chip?dp(22):0);bg.setStroke(dp(1),error?0xff704343:chip?0xff393a3c:kind.equals("user")?0xff393a3c:android.graphics.Color.BLACK);t.setBackground(bg);
                LayoutParams lp=new LayoutParams(chip?-2:-1,-2);lp.setMargins(0,dp(6),0,dp(chip?3:10));lp.gravity=Gravity.START;t.setLayoutParams(lp);
            }
            if(t.getParent()==null){changed=true;timeline.addView(t,index);}else if(timeline.indexOfChild(t)!=index){changed=true;((AgentTimelineLayout)timeline).move(t,index);}index++;
        }
        for(String key:new ArrayList<>(views.keySet()))if(!present.contains(key)){changed=true;timeline.removeView(views.remove(key));values.remove(key);data.remove(key);}
        // A late image preview or regrouping can change the row type. Carry the same
        // animator across that representation change (also across a split group).
        for(JSONObject row:rows){View item=views.get(row.optString("key"));inheritLanding(priorViews.get(row.optString("key")),item);if(row.optString("kind").equals("tool"))inheritLanding(priorActions.get(choiceKey(row)),item);JSONArray steps=row.optJSONArray("steps");if(steps!=null)for(int n=0;n<steps.length();n++)if(steps.optJSONObject(n)!=null)inheritLanding(priorActions.get(choiceKey(steps.optJSONObject(n))),item);}
        Set<View> retained=new HashSet<>(views.values());for(View old:new ArrayList<>(landings.keySet()))if(!retained.contains(old)){android.animation.ValueAnimator animation=landings.remove(old);landingFrame(old,1);if(!landings.containsValue(animation))animation.cancel();}
        for(String key:arriving)land(views.get(key));
        older.setVisibility(hasOlder?VISIBLE:GONE);
        scaleContent();
        if(changed){if(bottom)followLayout=true;followNextRows=false;queuePosition(bottom?new Position(null,0,0,true):new Position(held.anchor,held.offset,held.y,false,held.step));}
    }
    private TextView deliveryCaption(JSONObject row){
        AgentMessageDelivery delivery=new AgentMessageDelivery(row.optJSONObject("deliveryReceipt"));TextView caption=label(delivery.caption(WorkspaceLanguage.code(getContext()).equals("ru")),11);caption.setTextColor(delivery.accepted?WorkspaceDialogs.ACCENT:delivery.state.equals("rejected")?0xffff8895:0xffffc168);caption.setTag("delivery-"+row.optString("key"));
        final String key=row.optString("key");caption.setOnClickListener(v->{JSONObject latest=data.get(key);if(latest==null)return;try{JSONObject details=new JSONObject().put("kind","deliveryReceipt").put("deliveryReceipt",latest.optJSONObject("deliveryReceipt"));open.accept(details);}catch(JSONException ignored){}});return caption;
    }
    private String receiptTitle(JSONObject r){if(r.optBoolean("questionAnswer"))return tr("Ответ: ","Answer: ")+r.optString("answerSummary")+" ▾";String title=r.optString("title");return title.contains("Denied")?tr("Отклонено ▾","Denied ▾"):title.contains("project")?tr("Разрешено для проекта ▾","Allowed for project ▾"):tr("Разрешено разово ▾","Allowed once ▾");}
}
