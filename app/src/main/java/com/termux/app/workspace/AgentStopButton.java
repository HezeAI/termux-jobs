package com.termux.app.workspace;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** App-designed agent motion, driven only by the existing activity-line frame clock. */
final class AgentStopButton extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcBounds=new RectF();
    private final Path[] spark=new Path[]{new Path(),new Path(),new Path(),new Path()};
    private final Matrix geometry=new Matrix();
    private float sparkRadius=-1;
    private long time;
    private boolean compact;
    void compact(boolean value){compact=value;invalidate();}
    boolean compact(){return compact;}
    private static final String[] OC_PIXELS={"11100111","10100100","10100100","10100100","11100111"};
    private String agent="Codex";
    private AgentIndicatorState.State state=AgentIndicatorState.State.IDLE;
    AgentStopButton(Context c){super(c);setClickable(true);setFocusable(true);setEnabled(false);}
    void indicator(String name,AgentIndicatorState.State next){if(agent.equals(name)&&state==next)return;agent=name;state=next;invalidate();}
    AgentIndicatorState.State state(){return state;}
    boolean animated(){return !compact&&(state==AgentIndicatorState.State.WORKING||state==AgentIndicatorState.State.WAITING||state==AgentIndicatorState.State.RECONNECTING);}
    float circleRadius(){float d=getResources().getDisplayMetrics().density;return Math.max(1,Math.min(Math.min(getWidth(),getHeight())/2f-3*d,18*d));}
    void frame(long nanos,float ignored){long next=animated()?nanos:0;if(time!=next){time=next;invalidate();}}
    @Override public CharSequence getAccessibilityClassName(){return android.widget.Button.class.getName();}
    @Override protected void drawableStateChanged(){super.drawableStateChanged();invalidate();}

    private float phase(long period){return (time%period)/(float)period;}
    private float breath(long period){return .5f-.5f*(float)Math.cos(phase(period)*2*Math.PI);}
    /** Broad overlapping highlights: every element remains visible, including between cycles. */
    float elementBrightness(int index,int count){
        if(state!=AgentIndicatorState.State.WORKING||time==0)return 1;
        long period=agent.equals("Claude")?3600000000L:agent.equals("Gemini")?3200000000L:4000000000L;
        float wave=.5f+.5f*(float)Math.cos(2*Math.PI*(phase(period)-index/(float)count));
        return .36f+.64f*wave*wave;
    }
    float codexBrightness(){return state==AgentIndicatorState.State.WORKING&&time!=0?.48f+.52f*breath(2800000000L):1;}
    float glyphScale(){
        if(time==0)return 1;
        if(state==AgentIndicatorState.State.WAITING)return 1+.045f*breath(3200000000L);
        if(state!=AgentIndicatorState.State.WORKING)return 1;
        if(agent.equals("Claude"))return 1+.035f*breath(2600000000L);
        if(agent.equals("Gemini"))return 1+.055f*breath(3000000000L);
        return 1;
    }
    float arcAngle(){return time==0?0:phase(state==AgentIndicatorState.State.RECONNECTING?4000000000L:4200000000L)*360;}
    private void ink(int color,float brightness){paint.setColor(color);paint.setAlpha(Math.round(255*brightness));}
    private void sparkGeometry(float r){
        if(sparkRadius==r)return;
        sparkRadius=r;
        // Four fixed lobes form one upright spark. Only their brightness changes.
        spark[0].reset();spark[0].moveTo(0,0);spark[0].lineTo(-r*.215f,-r*.2275f);
        spark[0].quadTo(-r*.06f,-r*.395f,0,-r*.67f);
        spark[0].quadTo(r*.06f,-r*.395f,r*.215f,-r*.2275f);spark[0].close();
        for(int i=1;i<4;i++){geometry.setRotate(i*90);spark[0].transform(geometry,spark[i]);}
    }
    private void glyph(Canvas c,float r,int color){
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Math.max(1,r*.12f));paint.setStrokeCap(Paint.Cap.ROUND);
        if(agent.equals("Codex")){
            float glow=codexBrightness();
            ink(color,glow);c.drawLine(-r*.52f,-r*.28f,-r*.16f,0,paint);c.drawLine(-r*.16f,0,-r*.52f,r*.28f,paint);c.drawLine(r*.02f,r*.30f,r*.52f,r*.30f,paint);
        }else if(agent.equals("Claude")){
            for(int i=0;i<12;i++){ink(color,elementBrightness(i,12));double angle=i*Math.PI/6;float outer=r*(i%3==0?.63f:.54f);c.drawLine((float)Math.cos(angle)*r*.16f,(float)Math.sin(angle)*r*.16f,(float)Math.cos(angle)*outer,(float)Math.sin(angle)*outer,paint);}
        }else if(agent.equals("Gemini")){
            sparkGeometry(r);paint.setStyle(Paint.Style.FILL);
            for(int i=0;i<4;i++){ink(color,elementBrightness(i,4));c.drawPath(spark[i],paint);}
        }else{
            paint.setStyle(Paint.Style.FILL);float cell=r*.145f;int index=0;
            for(int y=0;y<5;y++)for(int x=0;x<8;x++)if(OC_PIXELS[y].charAt(x)=='1'){
                ink(color,elementBrightness(index++,21));c.drawRect((x-4)*cell,(y-2.5f)*cell,(x-3.12f)*cell,(y-1.62f)*cell,paint);
            }
        }
        paint.setAlpha(255);
    }
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        if(compact){float d=getResources().getDisplayMetrics().density,cx=getWidth()/2f,cy=getHeight()/2f;
            paint.setShader(null);paint.setStyle(Paint.Style.FILL);paint.setColor(WorkspaceDialogs.SURFACE);paint.setAlpha(isEnabled()?255:100);
            c.drawRoundRect(cx-16.5f*d,cy-12.5f*d,cx+16.5f*d,cy+12.5f*d,8*d,8*d,paint);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(d);paint.setColor(WorkspaceDialogs.LINE);c.drawRoundRect(cx-16.5f*d,cy-12.5f*d,cx+16.5f*d,cy+12.5f*d,8*d,8*d,paint);
            paint.setStyle(Paint.Style.FILL);paint.setColor(WorkspaceDialogs.TEXT);c.drawRoundRect(cx-4.5f*d,cy-4.5f*d,cx+4.5f*d,cy+4.5f*d,2*d,2*d,paint);paint.setAlpha(255);return;}
        float d=getResources().getDisplayMetrics().density,r=circleRadius();
        boolean idle=state==AgentIndicatorState.State.IDLE,waiting=state==AgentIndicatorState.State.WAITING,reconnect=state==AgentIndicatorState.State.RECONNECTING,error=state==AgentIndicatorState.State.ERROR;
        int accent=error?0xffff7481:reconnect?0xffffc168:idle?WorkspaceDialogs.MUTED:state==AgentIndicatorState.State.WORKING?WorkspaceDialogs.ACCENT:0xffecece5;
        c.save();c.translate(getWidth()/2f,getHeight()/2f);
        paint.setShader(null);paint.setStyle(Paint.Style.FILL);paint.setColor(idle?WorkspaceDialogs.SURFACE:state==AgentIndicatorState.State.WORKING?WorkspaceDialogs.TINT:WorkspaceDialogs.SURFACE);c.drawCircle(0,0,r,paint);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(d);ink(accent,idle?.51f:.82f);c.drawCircle(0,0,r,paint);
        c.save();float scale=glyphScale();c.scale(scale,scale);glyph(c,r,idle?WorkspaceDialogs.MUTED:accent);c.restore();
        float orbitR=Math.max(1,r-2*d);arcBounds.set(-orbitR,-orbitR,orbitR,orbitR);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeWidth((state==AgentIndicatorState.State.WORKING?2f:1.5f)*d);ink(accent,state==AgentIndicatorState.State.WORKING?1f:.9f);
        if(reconnect){for(int i=0;i<6;i++)c.drawArc(arcBounds,arcAngle()+i*60,27,false,paint);}
        else if(state==AgentIndicatorState.State.WORKING&&agent.equals("Codex"))c.drawArc(arcBounds,arcAngle()-90,48,false,paint);
        else if(waiting){float pulse=time==0?0:breath(3200000000L);paint.setStrokeWidth((1+.45f*pulse)*d);ink(accent,.5f+.35f*pulse);c.drawCircle(0,0,r-1.2f*d,paint);}
        paint.setAlpha(255);c.restore();
    }
}
