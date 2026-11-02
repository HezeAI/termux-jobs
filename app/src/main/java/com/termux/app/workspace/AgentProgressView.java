package com.termux.app.workspace;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.Gravity;
import android.widget.TextView;

/** Transient footer. Receives the existing activity clock; owns no polling/timer. */
final class AgentProgressView extends androidx.appcompat.widget.AppCompatTextView {
    private final Paint dots=new Paint(Paint.ANTI_ALIAS_FLAG);
    private AgentIndicatorState.State state=AgentIndicatorState.State.IDLE;
    private long time;
    AgentProgressView(Context c){super(c);setTextSize(12);setTextColor(WorkspaceDialogs.MUTED);setGravity(Gravity.CENTER_VERTICAL);setPadding(dp(52),dp(4),dp(4),dp(4));setMaxLines(2);setTextIsSelectable(false);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);setTag("agent-execution-footer");setVisibility(INVISIBLE);}
    private int dp(int n){return WorkspaceDialogs.dp(getContext(),n);}
    private String tr(String ru,String en){return WorkspaceLanguage.code(getContext()).equals("ru")?ru:en;}
    void present(AgentIndicatorState.State next,boolean visible){state=next;setVisibility(visible&&(next==AgentIndicatorState.State.WORKING||next==AgentIndicatorState.State.WAITING||next==AgentIndicatorState.State.RECONNECTING)?VISIBLE:INVISIBLE);label();invalidate();}
    private void label(){String value=state==AgentIndicatorState.State.WAITING?tr("Ожидание ответа или действия","Waiting for your answer or action"):state==AgentIndicatorState.State.RECONNECTING?tr("Переподключение…","Reconnecting…"):tr("Запрос выполняется","Request running");setContentDescription(value);String shown=state==AgentIndicatorState.State.WORKING&&time!=0?"":value;if(!getText().toString().equals(shown))setText(shown);}
    void frame(long nanos){long next=getVisibility()==VISIBLE&&state==AgentIndicatorState.State.WORKING?nanos:0;if(time==next)return;time=next;label();invalidate();}
    @Override protected void onMeasure(int width,int height){int fixed=Math.max(dp(36),(int)Math.ceil(getPaint().getFontSpacing()*2)+getPaddingTop()+getPaddingBottom());super.onMeasure(width,MeasureSpec.makeMeasureSpec(fixed,MeasureSpec.EXACTLY));}
    @Override protected void onDraw(Canvas c){super.onDraw(c);if(state!=AgentIndicatorState.State.WORKING)return;dots.setColor(WorkspaceDialogs.ACCENT);for(int i=0;i<3;i++){double phase=time==0?0:(time%1500000000L)/1500000000d*2*Math.PI-i*2*Math.PI/3;float pulse=time==0?0:(float)Math.pow((1+Math.cos(phase))/2,2);c.drawCircle(dp(12+i*12),getHeight()/2f,dp(2)*(1+.8f*pulse),dots);}}
}
