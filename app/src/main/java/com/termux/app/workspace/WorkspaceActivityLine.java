package com.termux.app.workspace;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.Choreographer;
import android.graphics.drawable.Drawable;

/** Paint-only activity cue. Idle and active have the same height: never resizes the PTY. */
public final class WorkspaceActivityLine extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix matrix=new Matrix();
    private LinearGradient gradient;
    private boolean working,framePending,legacyAnimations=true;
    private final FrameCadence cadence=new FrameCadence();
    private long animationTimeNanos;
    private View stop;
    private ViewGroup routeHost;
    private final Route route=new Route();
    private final int[] lineLocation=new int[2],stopLocation=new int[2],hostLocation=new int[2];
    public void stopTarget(View target){if(stop==target)return;resetHeart();stop=target;clearRoute();invalidate();}
    private void resetHeart(){if(stop instanceof AgentStopButton)((AgentStopButton)stop).frame(0,0);}
    private void clearRoute(){resetHeart();if(routeHost!=null){routeHost.getOverlay().remove(route);routeHost=null;}}
    boolean routing(){return routeHost!=null;}
    static Path routePath(float width,float lineY,float cx,float cy,float radius){
        Path p=new Path();p.moveTo(0,lineY);p.lineTo(Math.max(0,cx-radius-8),lineY);p.quadTo(cx,lineY,cx,cy+radius);RectF circle=new RectF(cx-radius,cy-radius,cx+radius,cy+radius);p.arcTo(circle,90,-180,false);p.arcTo(circle,-90,-180,false);p.quadTo(cx,lineY,Math.min(width,cx+radius+8),lineY);p.lineTo(width,lineY);return p;
    }
    private boolean drawRoute(){
        if(stop==null||!stop.isEnabled()||!stop.isShown()||stop.getWindowToken()!=getWindowToken()||!(getParent() instanceof ViewGroup)||getWidth()==0){clearRoute();return false;}
        ViewGroup host=(ViewGroup)getParent();getLocationInWindow(lineLocation);stop.getLocationInWindow(stopLocation);host.getLocationInWindow(hostLocation);
        float radius=stop instanceof AgentStopButton?((AgentStopButton)stop).circleRadius()+1.5f*getResources().getDisplayMetrics().density:Math.min(stop.getWidth(),stop.getHeight())*.45f;
        int left=lineLocation[0]-hostLocation[0],top=Math.min(lineLocation[1],Math.round(stopLocation[1]+stop.getHeight()/2f-radius))-hostLocation[1];
        int bottom=Math.max(lineLocation[1]+getHeight(),Math.round(stopLocation[1]+stop.getHeight()/2f+radius))-hostLocation[1];
        float cx=stopLocation[0]-hostLocation[0]+stop.getWidth()/2f-left,cy=stopLocation[1]-hostLocation[1]+stop.getHeight()/2f-top,lineY=lineLocation[1]-hostLocation[1]+getHeight()/2f-top;
        route.geometry(left,top,getWidth(),Math.max(1,bottom-top),lineY,cx,cy,radius);route.frameTimeNanos=animationsAllowed()?(animationTimeNanos==0?System.nanoTime():animationTimeNanos):0;
        if(stop instanceof AgentStopButton)((AgentStopButton)stop).frame(route.frameTimeNanos,route.strength());
        if(routeHost!=host){clearRoute();routeHost=host;host.getOverlay().add(route);}return true;
    }
    /** Deterministic asymmetric inner contacts, measured on the same continuous path. */
    static final class HeartRoute {
        final Path path=new Path();final float insideStart,insideEnd;final float[] hits=new float[6];
        HeartRoute(float width,float lineY,float cx,float cy,float radius){
            PathMeasure m=new PathMeasure();path.moveTo(0,lineY);path.lineTo(Math.max(0,cx-radius-8),lineY);path.quadTo(cx,lineY,cx,cy+radius);m=new PathMeasure(new Path(path),false);insideStart=m.getLength();
            float inner=radius*.84f;double[] angles={-42,154,269,18,201,78};float lastX=cx,lastY=cy+radius;
            for(int i=0;i<angles.length;i++){
                double a=Math.toRadians(angles[i]);float x=cx+inner*(float)Math.cos(a),y=cy+inner*(float)Math.sin(a);
                if(i==0)path.cubicTo(cx,cy+radius*.8f,cx+inner*.65f,cy+inner*.25f,x,y);
                else path.quadTo((lastX+x)*.5f,(lastY+y)*.5f+radius*(i%2==0?.035f:.075f),x,y);
                m=new PathMeasure(new Path(path),false);hits[i]=m.getLength();lastX=x;lastY=y;
            }
            // The last inner contact exits with the incoming motion, then joins
            // the clockwise outer circle tangentially at its original start.
            path.cubicTo(lastX+radius*.07f,lastY+radius*.12f,cx-radius*.12f,cy+radius,cx,cy+radius);m=new PathMeasure(new Path(path),false);insideEnd=m.getLength();
            RectF circle=new RectF(cx-radius,cy-radius,cx+radius,cy+radius);path.arcTo(circle,90,-180,false);path.arcTo(circle,-90,-180,false);path.quadTo(cx,lineY,Math.min(width,cx+radius+8),lineY);path.lineTo(width,lineY);
        }
        float strength(float distance,float radius){if(distance<=insideStart||distance>=insideEnd)return 0;float x=(distance-insideStart)/(insideEnd-insideStart),envelope=(float)Math.sin(Math.PI*x);float strike=0;for(float hit:hits){float d=(distance-hit)/Math.max(.1f,radius*.45f);strike=Math.max(strike,(float)Math.exp(-d*d));}float fade=Math.min(1,Math.min(distance-insideStart,insideEnd-distance)/Math.max(.1f,radius*.25f));return fade*Math.max(.28f*envelope,.95f*strike);}
    }
    private static final class Route extends Drawable {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private final PathMeasure measure=new PathMeasure();private final Path part=new Path();private float lastLineY=Float.NaN,lastCx,lastCy,lastRadius,insideStart,insideEnd;private long frameTimeNanos;private HeartRoute geometry;private final Region heartRegion=new Region();private final float[] position=new float[2];
        Route(){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);paint.setStrokeCap(Paint.Cap.ROUND);}
        void geometry(int x,int y,int width,int height,float lineY,float cx,float cy,float radius){Rect bounds=getBounds();if(bounds.left==x&&bounds.top==y&&bounds.width()==width&&bounds.height()==height&&lastLineY==lineY&&lastCx==cx&&lastCy==cy&&lastRadius==radius)return;lastLineY=lineY;lastCx=cx;lastCy=cy;lastRadius=radius;setBounds(x,y,x+width,y+height);geometry=new HeartRoute(width,lineY,cx,cy,radius);insideStart=geometry.insideStart;insideEnd=geometry.insideEnd;measure.setPath(geometry.path,false);
            float h=radius*(18f*.68f/19.5f);
            Path heart=AgentStopButton.heartPath();Matrix transform=new Matrix();transform.setScale(h,h);transform.postTranslate(cx,cy);heart.transform(transform);heartRegion.setPath(heart,new Region(0,0,width,height));paint.setStrokeWidth(Math.max(1.5f,radius/15));}
        private float eased(float x){return x*x*(3-2*x);}
        private float head(){float phase=(frameTimeNanos%3800000000L)/3800000000f,length=measure.getLength();if(phase<.32f)return insideStart*eased(phase/.32f);if(phase<.68f)return insideStart+(insideEnd-insideStart)*eased((phase-.32f)/.36f);return insideEnd+(length-insideEnd)*eased((phase-.68f)/.32f);}
        float strength(){return geometry==null?0:geometry.strength(head(),lastRadius);}
        @Override public void draw(Canvas c){float length=measure.getLength();if(length<=0)return;float head=head(),tail=Math.min(length*.3f,350);c.save();c.translate(getBounds().left,getBounds().top);
            for(int i=0;i<32;i++){float begin=head-tail+tail*i/32,end=head-tail+tail*(i+1)/32;while(begin<0){begin+=length;end+=length;}part.reset();measure.getSegment(begin,Math.min(end,length),part,true);if(end>length)measure.getSegment(0,end-length,part,true);measure.getPosTan(Math.min(length,(begin+Math.min(end,length))/2),position,null);paint.setColor(heartRegion.contains(Math.round(position[0]),Math.round(position[1]))?0xff07546c:0xff79dfcd);paint.setAlpha((i+1)*255/32);paint.setStrokeCap(i==0||i==31?Paint.Cap.ROUND:Paint.Cap.BUTT);c.drawPath(part,paint);}c.restore();}
        @Override public void setAlpha(int value){}@Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);}@Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
    /** Display-frame driven, with an average 60 Hz ceiling on faster panels. */
    static final class FrameCadence {
        static final long PERIOD=16666667L;
        private long next;
        boolean due(long time){
            if(next==0||time-next>PERIOD*4){next=time+PERIOD;return true;}
            if(time+PERIOD/20<next)return false;
            next+=PERIOD;return true;
        }
        void reset(){next=0;}
    }
    private boolean animationsAllowed(){return android.os.Build.VERSION.SDK_INT<26?legacyAnimations:android.animation.ValueAnimator.areAnimatorsEnabled();}
    private boolean canAnimate(){return animationsAllowed()&&working&&isAttachedToWindow()&&isShown()&&getWindowVisibility()==VISIBLE;}
    private final Choreographer.FrameCallback frame=time->{
        framePending=false;if(!canAnimate()){resetHeart();animationTimeNanos=0;invalidate();return;}
        if(cadence.due(time)){animationTimeNanos=time;route.frameTimeNanos=time;invalidate();if(routeHost!=null)route.invalidateSelf();}
        scheduleFrame();
    };
    private void scheduleFrame(){if(!framePending&&canAnimate()){framePending=true;Choreographer.getInstance().postFrameCallback(frame);}}
    private void cancelFrame(){if(framePending)Choreographer.getInstance().removeFrameCallback(frame);framePending=false;cadence.reset();animationTimeNanos=0;resetHeart();}
    public WorkspaceActivityLine(Context c,AttributeSet a){super(c,a);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    public void working(boolean value){if(working==value)return;if(android.os.Build.VERSION.SDK_INT<26)legacyAnimations=android.provider.Settings.Global.getFloat(getContext().getContentResolver(),android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,1f)>0;working=value;if(!value){cancelFrame();clearRoute();}setVisibility(value?VISIBLE:INVISIBLE);invalidate();}
    boolean working(){return working;}
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);gradient=new LinearGradient(0,0,Math.max(1,w*.45f),0,new int[]{0x0079dfcd,0xff79dfcd,0x0079dfcd},null,Shader.TileMode.CLAMP);}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);if(!working||gradient==null)return;
        if(drawRoute()){scheduleFrame();return;}
        float width=getWidth(),phase=animationsAllowed()?((animationTimeNanos==0?System.nanoTime():animationTimeNanos)%1400000000L)/1400000000f:.5f;
        matrix.setTranslate(-width*.45f+width*1.45f*phase,0);gradient.setLocalMatrix(matrix);paint.setShader(gradient);canvas.drawRect(0,0,width,getHeight(),paint);
        scheduleFrame();
    }
    @Override protected void onVisibilityChanged(View v,int visibility){super.onVisibilityChanged(v,visibility);if(visibility!=VISIBLE){cancelFrame();clearRoute();}else if(working)invalidate();}
    @Override protected void onDetachedFromWindow(){cancelFrame();clearRoute();super.onDetachedFromWindow();}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(visibility!=VISIBLE){cancelFrame();clearRoute();}else if(working)invalidate();}
}
