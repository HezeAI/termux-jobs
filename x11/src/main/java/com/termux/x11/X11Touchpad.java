package com.termux.x11;

import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;

/** Finger gestures are relative mouse motion; a finger touching down is not a mouse press. */
final class X11Touchpad {
    interface Sink {void move(float dx,float dy);void button(int button,boolean down);void scroll(float dx,float dy);default void tap(){} }
    private final Sink sink;
    private final float slop;
    private final long holdMillis;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private boolean active,moved,multi,multiMoved,dragging;
    private float startX,startY,lastX,lastY,scrollX,scrollY;
    private long downTime;
    X11Touchpad(Sink sink,float slop,long holdMillis){this.sink=sink;this.slop=slop;this.holdMillis=holdMillis;}
    private final Runnable hold=this::beginDrag;
    private void beginDrag(){if(active&&!moved&&!multi){dragging=true;sink.button(1,true);}}
    boolean event(MotionEvent e){
        int action=e.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){
            cancel();active=true;downTime=e.getEventTime();startX=lastX=e.getX();startY=lastY=e.getY();
            handler.postDelayed(hold,holdMillis);return true;
        }
        if(!active)return false;
        if(action==MotionEvent.ACTION_POINTER_DOWN){
            handler.removeCallbacks(hold);if(dragging){sink.button(1,false);dragging=false;}
            multi=true;multiMoved=multiMoved||moved||e.getPointerCount()>2;
            scrollX=center(e,true);scrollY=center(e,false);return true;
        }
        if(action==MotionEvent.ACTION_MOVE){
            if(multi){
                if(e.getPointerCount()<2)return true;
                float x=center(e,true),y=center(e,false),dx=x-scrollX,dy=y-scrollY;
                if(multiMoved||Math.hypot(dx,dy)>slop){multiMoved=true;sink.scroll(-dx*2,-dy*2);scrollX=x;scrollY=y;}
            }else{
                float x=e.getX(),y=e.getY();
                if(!moved&&!dragging&&Math.hypot(x-startX,y-startY)<=slop)return true;
                moved=true;handler.removeCallbacks(hold);sink.move(x-lastX,y-lastY);lastX=x;lastY=y;
            }
            return true;
        }
        if(action==MotionEvent.ACTION_UP){
            handler.removeCallbacks(hold);
            if(dragging)sink.button(1,false);
            else if(multi&&!multiMoved&&e.getEventTime()-downTime<holdMillis)click(3);
            else if(!multi&&!moved&&e.getEventTime()-downTime<holdMillis)click(1);
            active=dragging=false;return true;
        }
        if(action==MotionEvent.ACTION_CANCEL){cancel();return true;}
        return true;
    }
    private static float center(MotionEvent e,boolean x){return ((x?e.getX(0):e.getY(0))+(x?e.getX(1):e.getY(1)))/2;}
    private void click(int button){sink.button(button,true);sink.button(button,false);if(button==1)sink.tap();}
    void cancel(){handler.removeCallbacks(hold);if(dragging)sink.button(1,false);active=moved=multi=multiMoved=dragging=false;}
}
