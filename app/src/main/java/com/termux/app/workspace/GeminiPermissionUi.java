package com.termux.app.workspace;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import com.termux.app.TermuxActivity;
import com.termux.R;
import org.json.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.*;

/** Run-scoped native approvals. Details stay in memory, never in workspace/status/history files. */
final class GeminiPermissionUi {
    private final TermuxActivity activity;
    private final Supplier<WorkspaceStore.Session> current;
    private final BooleanSupplier visible;
    interface Transport {JSONObject request(String run,JSONObject value)throws Exception;}
    private final Transport transport;
    private final boolean openCode;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private LinearLayout card;
    private String run="",id="",requested="",body="",title="";
    private JSONObject permission;
    private boolean loading,busy,closed;
    private android.app.AlertDialog preview;
    GeminiPermissionUi(TermuxActivity a,Supplier<WorkspaceStore.Session> current,BooleanSupplier visible){this(a,current,visible,(run,value)->GeminiLiveControl.request(a.getFilesDir(),run,value));}
    GeminiPermissionUi(TermuxActivity a,Supplier<WorkspaceStore.Session> current,BooleanSupplier visible,Transport transport){this(a,current,visible,transport,false);}
    GeminiPermissionUi(TermuxActivity a,Supplier<WorkspaceStore.Session> current,BooleanSupplier visible,Transport transport,boolean openCode){activity=a;this.current=current;this.visible=visible;this.transport=transport;this.openCode=openCode;}
    private String tr(String ru,String en){return WorkspaceLanguage.code(activity).equals("ru")?ru:en;}
    private int dp(int n){return Math.round(n*activity.getResources().getDisplayMetrics().density);}
    private String key(WorkspaceStore.Session p){return openCode?p.id+":"+p.aiThread:p.geminiSocketId;}
    private boolean owns(String value){WorkspaceStore.Session p=current.get();return !closed&&visible.getAsBoolean()&&p!=null&&value.equals(key(p));}
    void syncVisibility(){if(!owns(run))hide();}
    private void hide(){if(card!=null)card.setVisibility(View.GONE);if(preview!=null){preview.dismiss();preview=null;}if(!owns(run)){run="";id="";requested="";permission=null;body="";title="";busy=false;}}
    void update(WorkspaceStore.Session p,GeminiLiveStatus status){
        if(!owns(key(p))){hide();return;}
        if(!run.equals(key(p))){hide();run=key(p);}
        String wanted=status==null||!status.available?"":status.permissionId;
        if(wanted.isEmpty()){if(!busy&&permission!=null){permission=null;id="";body="";title="";if(card!=null)card.setVisibility(View.GONE);}return;}
        if(permission!=null&&!wanted.equals(id)&&!busy){permission=null;id="";if(card!=null)card.setVisibility(View.GONE);}
        if(wanted.equals(id)||wanted.equals(requested)||loading||busy)return;
        final String captured=run;requested=wanted;loading=true;
        worker.execute(()->{JSONObject response=null;try{response=transport.request(captured,new JSONObject().put("op","permission"));}catch(Exception ignored){}JSONObject result=response;
            activity.runOnUiThread(()->{loading=false;if(!owns(captured)||!requested.equals(wanted))return;requested="";
                JSONObject value=result==null?null:result.optJSONObject("permission");
                if(value==null||!wanted.equals(value.optString("id"))||!valid(value))return;
                run=captured;id=wanted;permission=value;body=value.optString("body");title=value.optString("title");render();
            });});
    }
    void refresh(WorkspaceStore.Session p){
        if(!owns(key(p))){hide();return;}if(loading||busy)return;
        final String captured=key(p);run=captured;loading=true;
        worker.execute(()->{JSONObject result=null;try{result=transport.request(captured,new JSONObject().put("op","permission"));}catch(Exception ignored){}final JSONObject response=result;
            activity.runOnUiThread(()->{loading=false;if(!owns(captured))return;JSONObject value=response==null?null:response.optJSONObject("permission");
                if(value==null){if(response!=null){permission=null;id="";if(card!=null)card.setVisibility(View.GONE);}return;}
                if(!valid(value)||value.optString("id").equals(id))return;id=value.optString("id");permission=value;body=value.optString("body");title=value.optString("title");render();
            });});
    }
    static boolean valid(JSONObject value){
        if(!GeminiCli.validThread(value.optString("id"))||!java.util.Arrays.asList("exec","edit","info").contains(value.optString("type")))return false;
        if(value.optString("body").isEmpty()||value.optString("body").length()>40000||value.optString("title").length()>80)return false;
        JSONArray options=value.optJSONArray("options");if(options==null||options.length()>3)return false;
        boolean yes=false,no=false;
        for(int i=0;i<options.length();i++){String s=options.optString(i);if(!java.util.Arrays.asList("proceed_once","proceed_always","cancel").contains(s))return false;yes|=s.equals("proceed_once");no|=s.equals("cancel");}
        return yes&&no;
    }
    private TextView text(String value,int size){TextView t=new TextView(activity);t.setText(value);t.setTextSize(size);t.setTextColor(0xffd9e6eb);return t;}
    private GradientDrawable background(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));d.setStroke(dp(1),0xff315047);return d;}
    private void ensureCard(){
        if(card!=null)return;FrameLayout host=activity.findViewById(R.id.workspace_terminal_surface);if(host==null)return;
        card=new LinearLayout(activity);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(12),dp(10),dp(12),dp(10));card.setBackground(background(0xff0b1319,14));card.setTag("gemini-permission-card");
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);lp.setMargins(dp(12),dp(8),dp(12),dp(8));host.addView(card,lp);
    }
    private void render(){
        if(!owns(run)||permission==null)return;ensureCard();if(card==null)return;card.removeAllViews();card.setVisibility(View.VISIBLE);
        card.addView(text(tr((openCode?"OpenCode":"Gemini")+" · Разрешить действие?",(openCode?"OpenCode":"Gemini")+" · Allow action?"),14));
        TextView command=text(title+"\n"+body,12);command.setTypeface(Typeface.MONOSPACE);command.setMaxLines(3);command.setEllipsize(android.text.TextUtils.TruncateAt.END);command.setPadding(0,dp(6),0,dp(8));command.setOnClickListener(v->preview());card.addView(command);
        TextView full=text(tr("Посмотреть полностью ›","View full request ›"),12);full.setPadding(0,0,0,dp(8));full.setOnClickListener(v->preview());card.addView(full);
        LinearLayout actions=new LinearLayout(activity);actions.setGravity(Gravity.CENTER);JSONArray options=permission.optJSONArray("options");
        for(String outcome:new String[]{"proceed_once","proceed_always","cancel"}){
            boolean allowed=false;for(int i=0;i<options.length();i++)allowed|=outcome.equals(options.optString(i));if(!allowed)continue;
            TextView b=text(outcome.equals("cancel")?tr("Нет","No"):outcome.equals("proceed_always")?tr("На эту сессию","This session"):tr("Разрешить разово","Allow once"),13);
            b.setGravity(Gravity.CENTER);b.setMinHeight(dp(44));b.setBackground(background(outcome.equals("cancel")?0xff20191c:0xff16342e,10));b.setOnClickListener(v->answer(outcome));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(dp(3),0,dp(3),0);actions.addView(b,lp);
        }
        card.addView(actions);
        TextView hint=text(openCode?tr("Разово — только текущий запрос. Нет — отклонить его. Правила для проекта можно выбрать в штатном запросе OpenCode.","Once — only this request. No — reject it. Project rules remain available in the native OpenCode prompt."):tr("Разово — только это действие. На эту сессию — без повторного запроса по правилам Gemini; разрешение может охватывать похожие команды или действия. Сбрасывается после завершения Gemini.","Once — only this action. This session — no repeat prompt under Gemini's rules; the permission may cover similar commands or actions. It resets when Gemini exits."),11);hint.setPadding(0,dp(7),0,0);card.addView(hint);
    }
    private void preview(){
        if(permission==null||!owns(run))return;TextView t=text(body,13);t.setTypeface(Typeface.MONOSPACE);t.setTextIsSelectable(true);t.setPadding(dp(16),dp(12),dp(16),dp(12));ScrollView scroll=new ScrollView(activity);scroll.addView(t);
        preview=WorkspaceDialogs.builder(activity).setTitle(title).setView(scroll).setPositiveButton(tr("Назад","Back"),null).create();preview.show();
    }
    private void answer(String outcome){
        if(busy||permission==null||!owns(run))return;
        final String captured=run,token=id;busy=true;if(preview!=null){preview.dismiss();preview=null;}
        for(int i=0;i<card.getChildCount();i++)card.getChildAt(i).setEnabled(false);
        // Disable descendants too, avoiding repeated taps while the callback is in flight.
        setEnabled(card,false);
        worker.execute(()->{JSONObject response=null;try{response=transport.request(captured,new JSONObject().put("op","answer").put("id",token).put("outcome",outcome));}catch(Exception ignored){}JSONObject result=response;
            activity.runOnUiThread(()->{if(!owns(captured)||!token.equals(id))return;busy=false;JSONObject ack=result==null?null:result.optJSONObject("resolved");
                boolean confirmed=ack!=null&&token.equals(ack.optString("id"))&&outcome.equals(ack.optString("outcome"));
                permission=null;id="";body="";title="";card.removeAllViews();setEnabled(card,true);
                if(confirmed){card.setVisibility(View.GONE);return;}
                TextView done=text(confirmed?(outcome.equals("cancel")?tr("Отклонено","Denied"):outcome.equals("proceed_always")?tr("Разрешено на эту сессию без повторного запроса","Allowed for this session without repeat prompts"):tr("Разрешено один раз","Allowed once")):tr("Ответ не подтверждён. Проверьте запрос в терминале.","Response unconfirmed. Check the terminal prompt."),12);card.addView(done);done.setOnClickListener(v->card.setVisibility(View.GONE));
            });});
    }
    private void setEnabled(View view,boolean value){view.setEnabled(value);if(view instanceof android.view.ViewGroup){android.view.ViewGroup g=(android.view.ViewGroup)view;for(int i=0;i<g.getChildCount();i++)setEnabled(g.getChildAt(i),value);}}
    void close(){closed=true;hide();worker.shutdownNow();}
}
