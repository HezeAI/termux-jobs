package com.termux.app.workspace;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.Gravity;
import android.widget.TextView;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import org.json.JSONObject;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.function.LongSupplier;

/** Existing animated footer plus a visible-only one-second clock; no transport calls. */
final class AgentProgressView extends androidx.appcompat.widget.AppCompatTextView {
    private final Paint dots=new Paint(Paint.ANTI_ALIAS_FLAG);
    private AgentIndicatorState.State state=AgentIndicatorState.State.IDLE;
    private long time;
    private final LongSupplier clock;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private static final class Running {String request;long initial,anchor;}
    private final LinkedHashMap<String,Running> timers=new LinkedHashMap<>();
    private String owner="";
    private boolean tickPending;
    private final Runnable tick=()->{tickPending=false;if(timerVisible()){label();scheduleTick();}};
    void timing(String owner,JSONObject timing,boolean running){
        this.owner=owner;String request=timing==null?"":timing.optString("request");Long elapsed=milliseconds(timing);
        if(!running||request.isEmpty()||request.length()>240||elapsed==null)timers.remove(owner);
        else {Running current=timers.get(owner);if(current==null||!current.request.equals(request)){Running value=new Running();value.request=request;value.initial=elapsed;value.anchor=clock.getAsLong();timers.put(owner,value);while(timers.size()>32)timers.remove(timers.keySet().iterator().next());}}
        label();scheduleTick();
    }
    private static Long milliseconds(JSONObject timing){Object raw=timing==null?null:timing.opt("elapsedMs");if(!(raw instanceof Number))return null;double value=((Number)raw).doubleValue();long whole=((Number)raw).longValue();return Double.isFinite(value)&&value==whole&&whole>=0&&whole<=9007199254740991L?whole:null;}
    static String elapsed(long millis){long seconds=Math.max(0,millis)/1000,minutes=seconds/60,hours=minutes/60;return hours>0?String.format(Locale.ROOT,"%d:%02d:%02d",hours,minutes%60,seconds%60):String.format(Locale.ROOT,"%02d:%02d",minutes,seconds%60);}
    String timerText(){Running current=timers.get(owner);return current==null?"":elapsed(current.initial+Math.max(0,clock.getAsLong()-current.anchor));}
    private boolean timerVisible(){return getVisibility()==VISIBLE&&isShown()&&getWindowVisibility()==VISIBLE&&timers.containsKey(owner);}
    private void scheduleTick(){if(!timerVisible()){cancelTick();return;}if(tickPending)return;tickPending=true;handler.postDelayed(tick,1000);}
    private void cancelTick(){handler.removeCallbacks(tick);tickPending=false;}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();label();scheduleTick();}
    @Override protected void onDetachedFromWindow(){cancelTick();super.onDetachedFromWindow();}
    @Override protected void onVisibilityChanged(android.view.View changed,int visibility){super.onVisibilityChanged(changed,visibility);if(handler==null)return;if(timerVisible()){label();scheduleTick();}else cancelTick();}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(handler==null)return;if(visibility==VISIBLE){label();scheduleTick();}else cancelTick();}

    AgentProgressView(Context c){this(c,SystemClock::elapsedRealtime);}
    AgentProgressView(Context c,LongSupplier clock){super(c);this.clock=clock;setTextSize(12);setTextColor(WorkspaceDialogs.MUTED);setGravity(Gravity.CENTER_VERTICAL);setPadding(dp(52),dp(4),dp(4),dp(4));setMaxLines(2);setTextIsSelectable(false);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);setTag("agent-execution-footer");setVisibility(INVISIBLE);}
    private int dp(int n){return WorkspaceDialogs.dp(getContext(),n);}
    private String tr(String ru,String en){return WorkspaceLanguage.code(getContext()).equals("ru")?ru:en;}
    void present(AgentIndicatorState.State next,boolean visible){state=next;setVisibility(visible&&(next==AgentIndicatorState.State.WORKING||next==AgentIndicatorState.State.WAITING||next==AgentIndicatorState.State.RECONNECTING)?VISIBLE:INVISIBLE);label();scheduleTick();invalidate();}
    private void label(){String value=state==AgentIndicatorState.State.WAITING?tr("Ожидание ответа или действия","Waiting for your answer or action"):state==AgentIndicatorState.State.RECONNECTING?tr("Переподключение…","Reconnecting…"):tr("Запрос выполняется","Request running");String elapsed=timerText();setContentDescription(value+(elapsed.isEmpty()?"":" · "+elapsed));String shown=state==AgentIndicatorState.State.WORKING&&time!=0?"":value;if(!elapsed.isEmpty())shown=state==AgentIndicatorState.State.WORKING?elapsed:elapsed+" · "+value;if(!getText().toString().equals(shown))setText(shown);}
    void frame(long nanos){long next=getVisibility()==VISIBLE&&state==AgentIndicatorState.State.WORKING?nanos:0;if(time==next)return;boolean changed=(time==0)!=(next==0);time=next;if(changed)label();invalidate();}
    @Override protected void onMeasure(int width,int height){int fixed=Math.max(dp(36),(int)Math.ceil(getPaint().getFontSpacing()*2)+getPaddingTop()+getPaddingBottom());super.onMeasure(width,MeasureSpec.makeMeasureSpec(fixed,MeasureSpec.EXACTLY));}
    @Override protected void onDraw(Canvas c){super.onDraw(c);if(state!=AgentIndicatorState.State.WORKING)return;dots.setColor(WorkspaceDialogs.ACCENT);for(int i=0;i<3;i++){double phase=time==0?0:(time%1500000000L)/1500000000d*2*Math.PI-i*2*Math.PI/3;float pulse=time==0?0:(float)Math.pow((1+Math.cos(phase))/2,2);c.drawCircle(dp(12+i*12),getHeight()/2f,dp(2)*(1+.8f*pulse),dots);}}
}
