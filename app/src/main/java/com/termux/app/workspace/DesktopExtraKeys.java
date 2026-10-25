package com.termux.app.workspace;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.view.*;
import android.widget.*;
import com.termux.x11.X11ExtraKeys;

/** A small, non-focusable sibling window above the embedded X11 surface. */
final class DesktopExtraKeys {
    private static final int KEY_WIDTH=36,KEY_HEIGHT=30,HANDLE_WIDTH=20,PADDING=3;
    private final Activity activity;
    private final Runnable changed;
    private Dialog dialog;
    private View anchor;
    private Runnable release;
    private boolean placed;
    private int panelX,panelY,panelWidth,panelHeight;
    private final View.OnAttachStateChangeListener attachment=new View.OnAttachStateChangeListener(){
        public void onViewAttachedToWindow(View v){}
        public void onViewDetachedFromWindow(View v){close();}
    };
    private final ViewTreeObserver.OnPreDrawListener layout=()->{position();return true;};
    DesktopExtraKeys(Activity activity,Runnable changed){this.activity=activity;this.changed=changed;}
    boolean showing(){return dialog!=null;}
    void show(View target,X11ExtraKeys.Sink sink,Runnable releaseKeys){
        close();if(activity.isFinishing()||activity.isDestroyed())return;
        anchor=target;release=releaseKeys;placed=false;
        LinearLayout body=new LinearLayout(activity);body.setPadding(dp(PADDING),dp(PADDING),dp(PADDING),dp(PADDING));body.setBackground(WorkspaceDialogs.background(activity,false));
        addHandle(body,true);
        KeyGrid first=new KeyGrid();body.addView(first,new LinearLayout.LayoutParams(0,-1,1));
        key(first,"Esc",KeyEvent.KEYCODE_ESCAPE,false,sink);key(first,"Tab",KeyEvent.KEYCODE_TAB,false,sink);
        key(first,"Ctrl",KeyEvent.KEYCODE_CTRL_LEFT,true,sink);key(first,"Alt",KeyEvent.KEYCODE_ALT_LEFT,true,sink);key(first,"Shift",KeyEvent.KEYCODE_SHIFT_LEFT,true,sink);
        TextView hide=button(first,"×");hide.setContentDescription(tr("Скрыть дополнительные клавиши","Hide additional keys"));hide.setOnClickListener(v->close());
        KeyGrid second=first;
        key(second,"Home",KeyEvent.KEYCODE_MOVE_HOME,false,sink);key(second,"PgUp",KeyEvent.KEYCODE_PAGE_UP,false,sink);key(second,"↑",KeyEvent.KEYCODE_DPAD_UP,false,sink);key(second,"PgDn",KeyEvent.KEYCODE_PAGE_DOWN,false,sink);key(second,"End",KeyEvent.KEYCODE_MOVE_END,false,sink);
        KeyGrid third=first;
        key(third,"←",KeyEvent.KEYCODE_DPAD_LEFT,false,sink);key(third,"↓",KeyEvent.KEYCODE_DPAD_DOWN,false,sink);key(third,"→",KeyEvent.KEYCODE_DPAD_RIGHT,false,sink);key(third,"Del",KeyEvent.KEYCODE_FORWARD_DEL,false,sink);key(third,"Enter",KeyEvent.KEYCODE_ENTER,false,sink);
        addHandle(body,false);
        dialog=new Dialog(activity);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);dialog.setContentView(body);dialog.setCancelable(false);dialog.setCanceledOnTouchOutside(false);
        Window window=dialog.getWindow();window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_FULLSCREEN|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        window.getDecorView().setSystemUiVisibility(target.getWindowSystemUiVisibility());
        WindowManager.LayoutParams attributes=window.getAttributes();attributes.gravity=Gravity.TOP|Gravity.LEFT;window.setAttributes(attributes);
        dialog.show();anchor.addOnAttachStateChangeListener(attachment);anchor.getViewTreeObserver().addOnPreDrawListener(layout);position();changed.run();
    }
    private TextView button(KeyGrid row,String label){
        TextView button=new KeyButton(activity);button.setText(label);button.setTextSize(12);button.setSingleLine(true);button.setIncludeFontPadding(false);button.setTextColor(WorkspaceDialogs.TEXT);button.setGravity(Gravity.CENTER);button.setFocusable(false);button.setContentDescription(label);
        button.setBackground(WorkspaceDialogs.background(activity,false));row.addView(button);return button;
    }
    private void key(KeyGrid row,String label,int code,boolean modifier,X11ExtraKeys.Sink sink){
        TextView button=button(row,label);
        if(modifier){button.setOnClickListener(v->{boolean down=!button.isSelected();sink.key(code,down);button.setSelected(down);button.setBackground(WorkspaceDialogs.background(activity,down));});return;}
        ((KeyButton)button).physical(code,sink);
    }
    private void addHandle(LinearLayout body,boolean resize){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(HANDLE_WIDTH),dp(32));p.gravity=Gravity.CENTER_VERTICAL;body.addView(new Handle(resize),p);}
    /** Reflow existing key views without replacing them or losing held modifiers. */
    private final class KeyGrid extends ViewGroup {
        private int columns=6;
        KeyGrid(){super(activity);}
        @Override protected void onMeasure(int widthSpec,int heightSpec){
            int width=MeasureSpec.getSize(widthSpec),height=MeasureSpec.getSize(heightSpec),count=getChildCount();
            double best=Double.MAX_VALUE;
            for(int c=1;c<=count;c++){
                int rows=(count+c-1)/c;double w=(double)width/c,h=(double)height/rows;
                double cost=Math.abs(Math.log(Math.max(.01,w)/Math.max(.01,h)/1.8))+.15*(c*rows-count)/count;
                if(w<dp(KEY_WIDTH))cost+=10*(dp(KEY_WIDTH)-w)/dp(KEY_WIDTH);
                if(h<dp(KEY_HEIGHT))cost+=10*(dp(KEY_HEIGHT)-h)/dp(KEY_HEIGHT);
                if(cost<best){best=cost;columns=c;}
            }
            int rows=(count+columns-1)/columns,gap=dp(1);
            for(int i=0;i<count;i++)getChildAt(i).measure(MeasureSpec.makeMeasureSpec(Math.max(1,width/columns-2*gap),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(Math.max(1,height/rows-2*gap),MeasureSpec.EXACTLY));
            setMeasuredDimension(width,height);
        }
        @Override protected void onLayout(boolean changed,int left,int top,int right,int bottom){
            int rows=(getChildCount()+columns-1)/columns,gap=dp(1);
            for(int i=0;i<getChildCount();i++){
                int x=(i%columns)*getWidth()/columns+gap,y=(i/columns)*getHeight()/rows+gap;
                View key=getChildAt(i);key.layout(x,y,x+key.getMeasuredWidth(),y+key.getMeasuredHeight());
            }
        }
    }
    /** Only these narrow tabs consume move/resize gestures; key input stays independent. */
    private final class Handle extends View {
        private final boolean resize;
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private int pointer=-1,startX,startY,startWidth,startHeight;
        private float downX,downY;
        Handle(boolean resize){
            super(activity);this.resize=resize;setFocusable(false);setClickable(true);
            setContentDescription(resize?tr("Изменить размер панели клавиш","Resize key panel"):tr("Переместить панель клавиш","Move key panel"));
            setBackground(WorkspaceDialogs.background(activity,false));
        }
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);paint.setColor(WorkspaceDialogs.TEXT);paint.setStrokeWidth(dp(1));paint.setStrokeCap(Paint.Cap.ROUND);
            float x=getWidth()/2f,y=getHeight()/2f,r=dp(7),a=dp(3);
            if(resize){
                canvas.drawLine(x-r,y+r,x+r,y-r,paint);
                canvas.drawLine(x+r,y-r,x+r,y-r+2*a,paint);canvas.drawLine(x+r,y-r,x+r-2*a,y-r,paint);
                canvas.drawLine(x-r,y+r,x-r,y+r-2*a,paint);canvas.drawLine(x-r,y+r,x-r+2*a,y+r,paint);return;
            }
            canvas.drawLine(x-r,y,x+r,y,paint);canvas.drawLine(x,y-r,x,y+r,paint);
            for(int sign=-1;sign<=1;sign+=2){
                canvas.drawLine(x+sign*r,y,x+sign*(r-a),y-a,paint);canvas.drawLine(x+sign*r,y,x+sign*(r-a),y+a,paint);
                canvas.drawLine(x,y+sign*r,x-a,y+sign*(r-a),paint);canvas.drawLine(x,y+sign*r,x+a,y+sign*(r-a),paint);
            }
        }
        @Override public boolean performClick(){super.performClick();return true;}
        @Override public boolean onTouchEvent(MotionEvent event){
            switch(event.getActionMasked()){
                case MotionEvent.ACTION_DOWN:
                    pointer=event.getPointerId(0);downX=event.getRawX();downY=event.getRawY();
                    startX=panelX;startY=panelY;startWidth=panelWidth;startHeight=panelHeight;setPressed(true);return true;
                case MotionEvent.ACTION_MOVE:
                    if(pointer<0||event.getPointerId(0)!=pointer)return true;
                    int dx=Math.round(event.getRawX()-downX),dy=Math.round(event.getRawY()-downY);
                    if(resize){
                        Rect bounds=new Rect();if(anchor==null||!anchor.getGlobalVisibleRect(bounds))return true;
                        int right=startX+startWidth,bottom=startY+startHeight;
                        panelWidth=Math.min(right-bounds.left,Math.max(dp(184),startWidth-dx));
                        panelHeight=Math.min(bottom-bounds.top,Math.max(minHeight(panelWidth),startHeight-dy));
                        panelX=right-panelWidth;panelY=bottom-panelHeight;
                    }else{panelX=startX+dx;panelY=startY+dy;}
                    position();return true;
                case MotionEvent.ACTION_POINTER_UP:
                    if(event.getPointerId(event.getActionIndex())==pointer){pointer=-1;setPressed(false);}return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    pointer=-1;setPressed(false);if(event.getActionMasked()==MotionEvent.ACTION_UP)performClick();return true;
                default:return true;
            }
        }
    }
    private int minHeight(int width){int columns=Math.max(1,(width-dp(2*HANDLE_WIDTH+2*PADDING))/dp(KEY_WIDTH));return dp(2*PADDING)+((16+columns-1)/columns)*dp(KEY_HEIGHT);}
    private static final class KeyButton extends androidx.appcompat.widget.AppCompatTextView {
        private X11ExtraKeys.Sink sink;private int code;private boolean held,physicalClick;
        public KeyButton(android.content.Context context){super(context);}
        void physical(int code,X11ExtraKeys.Sink sink){this.code=code;this.sink=sink;setClickable(true);}
        @Override public boolean performClick(){
            super.performClick();
            if(sink!=null&&!physicalClick){sink.key(code,true);sink.key(code,false);}return true;
        }
        @Override public boolean onTouchEvent(MotionEvent event){
            if(sink==null)return super.onTouchEvent(event);
            switch(event.getActionMasked()){
                case MotionEvent.ACTION_DOWN:held=true;setPressed(true);sink.key(code,true);return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if(held){held=false;sink.key(code,false);}setPressed(false);
                    if(event.getActionMasked()==MotionEvent.ACTION_UP){physicalClick=true;performClick();physicalClick=false;}return true;
                default:return true;
            }
        }
    }
    private void position(){
        if(dialog==null||anchor==null)return;
        Rect bounds=new Rect();if(!anchor.getGlobalVisibleRect(bounds)||bounds.width()<1||bounds.height()<1){close();return;}
        if(!placed){panelWidth=dp(400);panelHeight=dp(104);panelX=bounds.left+(bounds.width()-Math.min(panelWidth,bounds.width()))/2;panelY=bounds.bottom-Math.min(panelHeight,bounds.height())-dp(4);placed=true;}
        int width=Math.min(panelWidth,bounds.width()),height=Math.min(Math.max(panelHeight,minHeight(width)),bounds.height());
        int x=Math.max(bounds.left,Math.min(panelX,bounds.right-width)),y=Math.max(bounds.top,Math.min(panelY,bounds.bottom-height));
        panelWidth=width;panelHeight=height;panelX=x;panelY=y;
        Window window=dialog.getWindow();WindowManager.LayoutParams p=window.getAttributes();
        if(p.width==width&&p.height==height&&p.x==x&&p.y==y)return;
        p.width=width;p.height=height;p.x=x;p.y=Math.max(bounds.top,y);window.setAttributes(p);
    }
    void close(){
        if(dialog==null)return;
        Dialog old=dialog;dialog=null;
        if(anchor!=null){anchor.removeOnAttachStateChangeListener(attachment);ViewTreeObserver observer=anchor.getViewTreeObserver();if(observer.isAlive())observer.removeOnPreDrawListener(layout);anchor=null;}
        if(release!=null){release.run();release=null;}old.dismiss();changed.run();
    }
    private int dp(int n){return WorkspaceDialogs.dp(activity,n);}
    private String tr(String ru,String en){return WorkspaceLanguage.code(activity).equals("ru")?ru:en;}
}
