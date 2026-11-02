package com.termux.app.workspace;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Agent glyph/orbit, painted by the existing activity-line frame clock. */
final class AgentStopButton extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path part=new Path();private final float[] point=new float[2];
    private OrbitRoute orbit;private float orbitRadius;private long time;
    private static final String[] OC_PIXELS={"11100111","10100100","10100100","10100100","11100111"};
    private String agent="Codex";
    private AgentIndicatorState.State state=AgentIndicatorState.State.IDLE;
    AgentStopButton(Context c){super(c);setClickable(true);setFocusable(true);setEnabled(false);}
    void indicator(String name,AgentIndicatorState.State next){if(agent.equals(name)&&state==next)return;agent=name;state=next;invalidate();}
    AgentIndicatorState.State state(){return state;}
    boolean animated(){return state==AgentIndicatorState.State.WORKING||state==AgentIndicatorState.State.WAITING||state==AgentIndicatorState.State.RECONNECTING;}
    float circleRadius(){float d=getResources().getDisplayMetrics().density;return Math.max(1,Math.min(Math.min(getWidth(),getHeight())/2f-3*d,18*d));}
    void frame(long nanos,float ignored){if(time!=nanos){time=nanos;invalidate();}}
    @Override public CharSequence getAccessibilityClassName(){return android.widget.Button.class.getName();}
    @Override protected void drawableStateChanged(){super.drawableStateChanged();invalidate();}
    /** Cached, single closed contour. Asymmetric contacts; no center waypoint. */
    static final class OrbitRoute {
        static final long PERIOD=7200000000L;
        final Path path=new Path();final PathMeasure measure;final float[] hits=new float[6];final float insideStart,insideEnd;
        OrbitRoute(float r){
            RectF circle=new RectF(-r,-r,r,r);path.moveTo(r,0);path.arcTo(circle,0,205,false);
            PathMeasure m=new PathMeasure(path,false);insideStart=m.getLength();
            double[] angles={255,34,188,300,92,342};float x=r*(float)Math.cos(Math.toRadians(205)),y=r*(float)Math.sin(Math.toRadians(205));
            for(int i=0;i<angles.length;i++){
                float nx=r*.98f*(float)Math.cos(Math.toRadians(angles[i])),ny=r*.98f*(float)Math.sin(Math.toRadians(angles[i]));
                if(i==0)path.cubicTo(x+r*.06f,y-r*.12f,nx-r*.12f,ny-r*.03f,nx,ny);
                else path.quadTo((x+nx)*.5f+r*(i%2==0?.025f:-.04f),(y+ny)*.5f,nx,ny);
                hits[i]=new PathMeasure(path,false).getLength();x=nx;y=ny;
            }
            float ex=r*.70710677f,ey=-r*.70710677f;
            path.cubicTo(x+r*.025f,y-r*.05f,ex-r*.10f,ey-r*.10f,ex,ey);
            insideEnd=new PathMeasure(path,false).getLength();path.arcTo(circle,315,45,false);path.close();measure=new PathMeasure(path,true);
        }
        float distance(long nanos){return (nanos%PERIOD)/(float)PERIOD*measure.getLength();}
        float strike(float distance,float r){float strength=0;for(float hit:hits){float delta=(distance-hit)/Math.max(.1f,r*.22f);strength=Math.max(strength,(float)Math.exp(-delta*delta));}return strength;}
    }
    private void glyph(Canvas c,float r,int color){
        paint.setColor(color);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Math.max(1,r*.12f));paint.setStrokeCap(Paint.Cap.ROUND);
        if(agent.equals("Codex")){c.drawLine(-r*.52f,-r*.28f,-r*.16f,0,paint);c.drawLine(-r*.16f,0,-r*.52f,r*.28f,paint);c.drawLine(r*.02f,r*.30f,r*.52f,r*.30f,paint);}
        else if(agent.equals("Claude")){for(int i=0;i<12;i++){double a=i*Math.PI/6;float outer=r*(i%3==0?.63f:.54f);c.drawLine((float)Math.cos(a)*r*.16f,(float)Math.sin(a)*r*.16f,(float)Math.cos(a)*outer,(float)Math.sin(a)*outer,paint);}}
        else if(agent.equals("Gemini")){part.reset();part.moveTo(0,-r*.67f);part.quadTo(r*.12f,-r*.12f,r*.62f,0);part.quadTo(r*.12f,r*.12f,0,r*.67f);part.quadTo(-r*.12f,r*.12f,-r*.62f,0);part.quadTo(-r*.12f,-r*.12f,0,-r*.67f);part.close();paint.setStyle(Paint.Style.FILL);c.drawPath(part,paint);}
        else {paint.setStyle(Paint.Style.FILL);float cell=r*.145f;for(int y=0;y<5;y++)for(int x=0;x<8;x++)if(OC_PIXELS[y].charAt(x)=='1')c.drawRect((x-4)*cell,(y-2.5f)*cell,(x-3.12f)*cell,(y-1.62f)*cell,paint);}
    }
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float d=getResources().getDisplayMetrics().density,r=circleRadius();boolean idle=state==AgentIndicatorState.State.IDLE,waiting=state==AgentIndicatorState.State.WAITING,reconnect=state==AgentIndicatorState.State.RECONNECTING,error=state==AgentIndicatorState.State.ERROR;
        int accent=error?0xffff7481:reconnect?0xffffc168:idle?0xff74868c:0xff79dfcd;
        c.save();c.translate(getWidth()/2f,getHeight()/2f);
        paint.setShader(null);paint.setStyle(Paint.Style.FILL);paint.setColor(idle?0xff111b21:0xff102821);c.drawCircle(0,0,r,paint);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(d);paint.setColor(accent);paint.setAlpha(idle?130:210);c.drawCircle(0,0,r,paint);paint.setAlpha(255);
        float orbitR=Math.max(1,r-2f*d);if(orbit==null||orbitRadius!=orbitR){orbit=new OrbitRoute(orbitR);orbitRadius=orbitR;}
        float head=orbit.distance(time),strike=state==AgentIndicatorState.State.WORKING&&time!=0?orbit.strike(head,orbitR):0;
        float breath=waiting&&time!=0?(float)(.5+.5*Math.sin(time/1e9*2*Math.PI/2.8)):0;
        int glyphColor=idle?0xff8a9ba1:accent;paint.setAlpha(255);c.save();float scale=1+strike*.13f+breath*.07f;c.scale(scale,scale);glyph(c,r,glyphColor);if(strike>.1f){paint.setColor(Color.WHITE);paint.setAlpha((int)(220*strike));glyph(c,r,Color.WHITE);}c.restore();paint.setAlpha(255);
        if(reconnect){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.6f*d);paint.setColor(accent);RectF bounds=new RectF(-orbitR,-orbitR,orbitR,orbitR);float turn=time==0?0:(time%2600000000L)/2600000000f*360;for(int i=0;i<6;i++)c.drawArc(bounds,turn+i*60,27,false,paint);}
        else if(state==AgentIndicatorState.State.WORKING){
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.7f*d);paint.setStrokeCap(Paint.Cap.ROUND);paint.setColor(Color.WHITE);
            float length=orbit.measure.getLength(),tail=orbitR*.7f;
            if(time==0)head=orbit.insideStart*.45f;
            for(int i=0;i<10;i++){float begin=head-tail+tail*i/10,end=head-tail+tail*(i+1)/10;while(begin<0){begin+=length;end+=length;}part.reset();orbit.measure.getSegment(begin,Math.min(end,length),part,true);if(end>length)orbit.measure.getSegment(0,end-length,part,true);paint.setAlpha((i+1)*255/10);c.drawPath(part,paint);}
            orbit.measure.getPosTan(head,point,null);paint.setStyle(Paint.Style.FILL);paint.setAlpha(255);c.drawCircle(point[0],point[1],1.7f*d,paint);
        }
        if(waiting){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth((1+breath)*d);paint.setAlpha((int)(130+breath*125));paint.setColor(accent);c.drawCircle(0,0,r-1.2f*d,paint);paint.setAlpha(255);}
        c.restore();
    }
}
