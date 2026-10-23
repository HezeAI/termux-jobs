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
    final LinearLayout timeline,requests,keysHost;
    final TextView status,older,connection;
    final EditText shellInput;
    private WorkspaceIconButton shellSend;
    private String assistant="AI";
    private boolean codexActions,groupAllTools;
    void actionCards(boolean allTools){codexActions=true;groupAllTools=allTools;}
    void codexActions(boolean enabled){codexActions=enabled;}
    void assistantName(String name){assistant=name;}
    void shellEnabled(boolean enabled){shellInput.setEnabled(enabled);shellInput.setFocusable(enabled);shellInput.setFocusableInTouchMode(enabled);shellInput.setClickable(enabled);shellSend.setEnabled(enabled);shellSend.setAlpha(enabled?1f:.4f);if(!enabled)shellInput.clearFocus();}
    private final Map<String,View> views=new LinkedHashMap<>();
    private final Map<String,String> values=new HashMap<>();
    private final Map<String,JSONObject> data=new HashMap<>();
    private final Consumer<JSONObject> open;
    private boolean touchingTimeline;
    private boolean followNextRows;
    private boolean followLayout;
    static final class Position {final String anchor;final int offset,y;final boolean bottom;Position(String anchor,int offset,int y,boolean bottom){this.anchor=anchor;this.offset=offset;this.y=y;this.bottom=bottom;}}
    private Position restoring;
    private ViewTreeObserver restoreObserver;private ViewTreeObserver.OnPreDrawListener restoreListener;
    private void cancelRestore(){restoring=null;if(restoreObserver!=null&&restoreObserver.isAlive()&&restoreListener!=null)restoreObserver.removeOnPreDrawListener(restoreListener);restoreObserver=null;restoreListener=null;}
    Position position(){
        if(restoring!=null)return restoring;
        String anchor=null;int offset=0;
        for(int i=1;i<timeline.getChildCount();i++){View v=timeline.getChildAt(i);if(v.getBottom()>scroll.getScrollY()){for(Map.Entry<String,View> e:views.entrySet())if(e.getValue()==v){anchor=e.getKey();offset=v.getTop()-scroll.getScrollY();break;}if(anchor!=null)break;}}
        return new Position(anchor,offset,scroll.getScrollY(),timeline.getHeight()-scroll.getHeight()-scroll.getScrollY()<dp(48));
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
        scroll.scrollTo(0,p.bottom?Math.max(0,timeline.getHeight()-scroll.getHeight()):anchor==null?p.y:anchor.getTop()-p.offset);
    }
    // ScrollView.fullScroll also moves keyboard focus to a transcript child.
    // Streaming must only move the viewport, leaving both composers and IME composition alone.
    void bottomAfterLayout(){queuePosition(new Position(null,0,0,true));}
    void followAfterSend(){followNextRows=true;followLayout=true;bottomAfterLayout();}
    AgentChatView(Context c,Consumer<JSONObject> open,Runnable loadOlder,Runnable sendShell,Runnable terminal,Runnable interrupt){
        super(c);this.open=open;setOrientation(VERTICAL);setBackgroundColor(Color.BLACK);
        status=label("",12);status.setVisibility(GONE);addView(status);
        scroll=new ScrollView(c){
            @Override public boolean dispatchTouchEvent(MotionEvent e){
                // Selectable TextViews may consume DOWN before a parent's OnTouchListener.
                // Observe dispatch without intercepting selection or the ScrollView gesture.
                touchingTimeline=e.getActionMasked()!=MotionEvent.ACTION_UP&&e.getActionMasked()!=MotionEvent.ACTION_CANCEL;
                cancelRestore();followNextRows=false;followLayout=false;
                return super.dispatchTouchEvent(e);
            }
        };scroll.setFillViewport(true);
        scroll.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(restoring==null&&followLayout)bottomAfterLayout();});scroll.setSmoothScrollingEnabled(false);
        timeline=new LinearLayout(c);timeline.setOrientation(VERTICAL);timeline.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(followLayout){followLayout=false;if(restoring==null)bottomAfterLayout();}});timeline.setPadding(dp(12),dp(4),dp(12),dp(16));scroll.addView(timeline);
        older=pill(tr("Предыдущие сообщения","Earlier messages"),loadOlder);timeline.addView(older);
        addView(scroll,new LayoutParams(-1,0,1));requests=new LinearLayout(c);requests.setOrientation(VERTICAL);requests.setPadding(dp(12),0,dp(12),0);timeline.addView(requests,new LayoutParams(-1,-2));connection=label("",13);connection.setVisibility(GONE);requests.addView(connection);
        LinearLayout shell=new LinearLayout(c);shell.setGravity(Gravity.CENTER_VERTICAL);shell.setPadding(dp(8),dp(4),dp(8),dp(4));shell.addView(label("Shell",12));
        shellInput=new EditText(c);shellInput.setTextColor(0xffd5e4e0);shellInput.setHintTextColor(0xff829b96);shellInput.setTextSize(13);shellInput.setMinLines(1);shellInput.setMaxLines(3);shellInput.setHint(tr("Команда без модели…","Command without model…"));shellInput.setTypeface(Typeface.MONOSPACE);shellInput.setSaveEnabled(false);shellInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);shellInput.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_NONE|android.view.inputmethod.EditorInfo.IME_FLAG_NO_ENTER_ACTION);shell.addView(shellInput,new LayoutParams(0,-2,1));
        WorkspaceIconButton send=new WorkspaceIconButton(c);shellSend=send;send.setIcon(c.getDrawable(com.termux.R.drawable.workspace_play),dp(24));send.setTextColor(0xff79dfcd);send.setContentDescription(tr("Выполнить shell-команду","Run shell command"));send.setOnClickListener(v->sendShell.run());shell.addView(send,new LayoutParams(dp(44),dp(44)));addView(shell,new LayoutParams(-1,-2));shellEnabled(false);
        keysHost=new LinearLayout(c);keysHost.setOrientation(VERTICAL);keysHost.setVisibility(GONE);addView(keysHost,new LayoutParams(-1,-2));

    }
    void connection(String message){connection(message,null);}
    void connection(String message,Runnable retry){connection.setOnClickListener(retry==null?null:v->retry.run());connection.setClickable(retry!=null);if(connection.getText().toString().equals(message)&&connection.getVisibility()==(message.isEmpty()?GONE:VISIBLE))return;boolean bottom=timeline.getHeight()-scroll.getHeight()-scroll.getScrollY()<dp(48);connection.setText(message);connection.setVisibility(message.isEmpty()?GONE:VISIBLE);if(bottom){followLayout=true;bottomAfterLayout();}}
    void clearRequests(){requests.removeAllViews();requests.addView(connection);}
    String tr(String ru,String en){return WorkspaceLanguage.code(getContext()).equals("ru")?ru:en;}
    int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    TextView label(String s,int size){TextView t=new TextView(getContext());t.setText(s);t.setTextSize(size);t.setTextColor(0xffd5e4e0);t.setPadding(dp(10),dp(8),dp(10),dp(8));return t;}
    TextView pill(String s,Runnable action){TextView t=label(s,12);GradientDrawable bg=new GradientDrawable();bg.setColor(0xff0b1b19);bg.setCornerRadius(dp(22));bg.setStroke(dp(1),0xff28433d);t.setBackground(bg);t.setOnClickListener(v->action.run());LayoutParams lp=new LayoutParams(-2,-2);lp.setMargins(dp(3),dp(5),dp(3),dp(5));t.setLayoutParams(lp);t.setMinHeight(dp(36));return t;}
    void olderState(boolean loading,String error){older.setEnabled(!loading);older.setText(loading?tr("Загрузка сообщений…","Loading messages…"):error!=null?error:tr("Предыдущие сообщения","Earlier messages"));}
    void olderRows(List<JSONObject> rows,boolean hasOlder){boolean fits=timeline.getHeight()<=scroll.getHeight();Position held=position();rows(rows,hasOlder);restorePosition(fits?new Position(null,0,0,false):new Position(held.anchor,held.offset,held.y,false));}
    void clear(){olderState(false,null);cancelRestore();touchingTimeline=false;followNextRows=false;followLayout=false;views.clear();values.clear();data.clear();timeline.removeViews(1,timeline.getChildCount()-1);clearRequests();timeline.addView(requests);scroll.scrollTo(0,0);}
    void rows(List<JSONObject> rows,boolean hasOlder){
        if(codexActions)rows=CodexActionTimeline.group(rows,groupAllTools);
        Position held=position();final boolean bottom=!touchingTimeline&&(followNextRows||held.bottom);
        boolean changed=false;Set<String> present=new HashSet<>();int index=1;
        for(JSONObject r:rows){String key=r.optString("key");present.add(key);data.put(key,r);String value=r.toString()+(codexActions?WorkspaceLanguage.code(getContext()):"");
            if(r.optString("kind").equals("actionGroup")){
                View existing=views.get(key);CodexActionsView card=existing instanceof CodexActionsView?(CodexActionsView)existing:new CodexActionsView(this,open);
                views.put(key,card);
                if(!value.equals(values.get(key))){changed=true;values.put(key,value);card.update(r);}
                if(card.getParent()==null){changed=true;timeline.addView(card,index);}else if(timeline.indexOfChild(card)!=index){changed=true;timeline.removeView(card);timeline.addView(card,index);}index++;continue;
            }
            TextView t=(TextView)views.get(key);
            if(t==null){t=label("",14);views.put(key,t);}
            if(!value.equals(values.get(key))){changed=true;values.put(key,value);String kind=r.optString("kind");boolean error=kind.equals("error");boolean chip=kind.equals("reasoning")||kind.equals("tool")||kind.equals("receipt");
                String caption=kind.equals("reasoning")?tr("Рассуждение ▾","Thinking ▾"):kind.equals("receipt")?receiptTitle(r):(codexActions?CodexActionLabels.title(r,WorkspaceLanguage.code(getContext()).equals("ru")):r.optString("title"))+" ▾";
                String body=r.optString("body");String shown=AgentChatErrors.nativeDiagnosticHint(body)?tr("Подробности доступны по нажатию на сообщение об ошибке.","Tap the error message for details."):body;
                t.setText(error?tr("Ошибка · ","Error · ")+assistant+"\n"+AgentChatErrors.explain(body,WorkspaceLanguage.code(getContext()).equals("ru"))+tr("\nПодробности ▾","\nDetails ▾"):chip?caption:(kind.equals("user")?tr("Вы: ","You: "):kind.equals("text")?assistant+": ":"")+shown+(r.optBoolean("more")?tr("\nНажмите для полного текста…","\nTap for full text…"):""));
                t.setTextIsSelectable(!chip&&!error);final String k=key;t.setOnClickListener(error||chip||r.optBoolean("more")?v->{JSONObject row=data.get(k);if(row!=null)open.accept(row);}:null);t.setTextSize(chip?12:14);t.setTypeface(chip?Typeface.DEFAULT:Typeface.DEFAULT);
                GradientDrawable bg=new GradientDrawable();bg.setColor(error?0xff241416:chip?0xff0b1b19:Color.BLACK);bg.setCornerRadius(error?dp(12):chip?dp(22):0);bg.setStroke(dp(1),error?0xff704343:chip?0xff28433d:kind.equals("user")?0xff43776b:Color.BLACK);t.setBackground(bg);
                LayoutParams lp=new LayoutParams(chip?-2:-1,-2);lp.setMargins(0,dp(6),0,dp(chip?3:10));lp.gravity=Gravity.START;t.setLayoutParams(lp);
            }
            if(t.getParent()==null){changed=true;timeline.addView(t,index);}else if(timeline.indexOfChild(t)!=index){changed=true;timeline.removeView(t);timeline.addView(t,index);}index++;
        }
        for(String key:new ArrayList<>(views.keySet()))if(!present.contains(key)){changed=true;timeline.removeView(views.remove(key));values.remove(key);data.remove(key);}
        older.setVisibility(hasOlder?VISIBLE:GONE);
        if(changed){if(bottom)followLayout=true;followNextRows=false;queuePosition(bottom?new Position(null,0,0,true):new Position(held.anchor,held.offset,held.y,false));}
    }
    private String receiptTitle(JSONObject r){String title=r.optString("title");return title.contains("Denied")?tr("Отклонено ▾","Denied ▾"):title.contains("project")?tr("Разрешено для проекта ▾","Allowed for project ▾"):tr("Разрешено разово ▾","Allowed once ▾");}
}
