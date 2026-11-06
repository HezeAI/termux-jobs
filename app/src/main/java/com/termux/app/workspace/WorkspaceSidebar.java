package com.termux.app.workspace;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.termux.R;

/** Navigation only. Callbacks retain the identity of the row, never a global selection. */
final class WorkspaceSidebar {
    final Dialog dialog;
    final LinearLayout body;
    final ScrollView scroll;
    private final Context context;
    private final FrameLayout root;
    private WorkspaceExpansion expanded;
    private float downX, downY;
    private boolean closing;
    private View selectedRow;
    private WorkspacePopover activePopup;
    private final java.util.List<Runnable> sessionRefresh=new java.util.ArrayList<>();
    void refreshSessions(){for(Runnable refresh:sessionRefresh)refresh.run();}

    WorkspaceSidebar(Context context) {
        this.context=context;
        dialog=new Dialog(context);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        root=new FrameLayout(context);root.setBackgroundColor(0x88000000);
        scroll=new ScrollView(context){@Override public boolean dispatchTouchEvent(MotionEvent event){
            if(event.getActionMasked()==MotionEvent.ACTION_DOWN){downX=event.getX();downY=event.getY();}
            if(event.getActionMasked()==MotionEvent.ACTION_MOVE&&event.getX()-downX<-dp(64)&&Math.abs(event.getY()-downY)<dp(48)){MotionEvent cancel=MotionEvent.obtain(event);cancel.setAction(MotionEvent.ACTION_CANCEL);super.dispatchTouchEvent(cancel);cancel.recycle();dismiss();return true;}
            return super.dispatchTouchEvent(event);
        }};scroll.setTag("workspace-sidebar");scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);scroll.setHorizontalScrollBarEnabled(false);
        GradientDrawable drawer=new GradientDrawable();drawer.setColor(WorkspaceDialogs.BG);drawer.setStroke(dp(1),WorkspaceDialogs.LINE);float radius=dp(22);drawer.setCornerRadii(new float[]{0,0,radius,radius,radius,radius,0,0});scroll.setBackground(drawer);scroll.setClipToOutline(true);
        body=new LinearLayout(context);body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(10),dp(16),dp(10),dp(20));scroll.addView(body);
        root.addView(scroll,new FrameLayout.LayoutParams(width(),-1,Gravity.START));
        root.setOnClickListener(v->dismiss());scroll.setOnClickListener(v->{});
        root.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{
            int width=width();if(scroll.getLayoutParams().width!=width){scroll.getLayoutParams().width=width;scroll.requestLayout();}
        });
        dialog.setContentView(root);
        dialog.setOnDismissListener(d->{if(activePopup!=null)activePopup.dismissImmediately();});
        dialog.setOnKeyListener((d,key,event)->{if(key==android.view.KeyEvent.KEYCODE_BACK){if(event.getAction()==android.view.KeyEvent.ACTION_UP)dismiss();return true;}return false;});
    }
    private int width(){int screen=context.getResources().getDisplayMetrics().widthPixels;return Math.min(screen-dp(24),Math.max(dp(220),Math.round(screen*.61f)));}
    private int dp(int n){return WorkspaceDialogs.dp(context,n);}
    void show(){
        dialog.show();Window w=dialog.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.setDimAmount(0);w.setLayout(-1,-1);w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);}
        if(selectedRow!=null){
            scroll.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener(){
                @Override public void onGlobalLayout(){
                    if(!scroll.getViewTreeObserver().isAlive())return;
                    scroll.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    if(dialog.isShowing())scroll.scrollTo(0,Math.max(0,selectedRow.getTop()-dp(8)));
                }
            });
        }
        scroll.setTranslationX(-width());scroll.animate().translationX(0).setDuration(duration(280)).start();
    }
    private long duration(int ms){return android.animation.ValueAnimator.areAnimatorsEnabled()?ms:0;}
    void dismiss(){if(closing)return;closing=true;scroll.animate().translationX(-width()).setDuration(duration(220)).withEndAction(dialog::dismiss).start();}
    void section(String title){TextView v=new TextView(context);v.setText(title);v.setTextSize(9);v.setTextColor(0xffa5a5a2);v.setPadding(dp(10),dp(20),0,dp(9));body.addView(v);}
    TextView action(LinearLayout parent,String title,int icon,Runnable callback){
        TextView v=new TextView(context);v.setText(title);v.setTextSize(12);v.setTextColor(0xfff0f0ee);v.setGravity(Gravity.CENTER_VERTICAL);v.setMinHeight(dp(44));v.setPadding(dp(9),dp(10),dp(9),dp(10));
        if(icon!=0){android.graphics.drawable.Drawable d=context.getDrawable(icon).mutate();d.setTint(0xffa5a5a2);d.setBounds(0,0,dp(20),dp(20));v.setCompoundDrawables(d,null,null,null);v.setCompoundDrawablePadding(dp(11));}
        v.setBackground(WorkspaceRipple.background(context,null,10));
        v.setOnClickListener(x->callback.run());parent.addView(v,new LinearLayout.LayoutParams(-1,-2));return v;
    }
    void action(String title,int icon,Runnable callback){action(body,title,icon,()->{dismiss();callback.run();});}
    LinearLayout session(String id,String title,String subtitle,boolean base,boolean saved,boolean selected,java.util.function.BooleanSupplier isSaved,Runnable save,Runnable unsave,Runnable close){
        return session(id,title,subtitle,base,saved,selected,false,isSaved,save,unsave,close);
    }
    LinearLayout session(String id,String title,String subtitle,boolean base,boolean saved,boolean selected,boolean agent,java.util.function.BooleanSupplier isSaved,Runnable save,Runnable unsave,Runnable close){
        return session(id,title,subtitle,base,saved,selected,agent,()->true,isSaved,save,unsave,close);
    }
    LinearLayout session(String id,String title,String subtitle,boolean base,boolean saved,boolean selected,boolean agent,java.util.function.BooleanSupplier running,java.util.function.BooleanSupplier isSaved,Runnable save,Runnable unsave,Runnable close){
        return session(id,title,()->subtitle,base,saved,selected,agent,running,isSaved,save,unsave,close);
    }
    LinearLayout session(String id,String title,java.util.function.Supplier<String> subtitle,boolean base,boolean saved,boolean selected,boolean agent,java.util.function.BooleanSupplier running,java.util.function.BooleanSupplier isSaved,Runnable save,Runnable unsave,Runnable close){
        LinearLayout frame=new LinearLayout(context);frame.setOrientation(LinearLayout.VERTICAL);frame.setTag("sidebar-session-"+id);body.addView(frame);
        LinearLayout heading=new LinearLayout(context);heading.setGravity(Gravity.CENTER_VERTICAL);frame.addView(heading);
        TextView label=action(heading,title+(saved?" · "+tr("Сохранён","Saved"):"")+"\n"+subtitle.get(),agent?R.drawable.workspace_ai_agent:R.drawable.workspace_terminal,()->{});
        label.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));styleSessionLabel(label);
        if(selected){GradientDrawable bg=new GradientDrawable();bg.setColor(0xff303132);bg.setCornerRadius(dp(10));heading.setBackground(bg);}
        LinearLayout actions=new LinearLayout(context);actions.setOrientation(LinearLayout.VERTICAL);actions.setPadding(dp(8),0,0,dp(8));actions.setVisibility(View.GONE);frame.addView(actions);
        WorkspaceExpansion disclosure=new WorkspaceExpansion(actions);
        if(selected){if(expanded!=null)expanded.setOpen(false);disclosure.setOpen(true);expanded=disclosure;selectedRow=frame;}
        label.setOnClickListener(v->{boolean open=!disclosure.isOpen();if(expanded!=null&&expanded!=disclosure)expanded.setOpen(false);disclosure.setOpen(open);expanded=open?disclosure:null;});
        sessionRefresh.add(()->{boolean stored=isSaved.getAsBoolean();label.setText(title+(stored?" · "+tr("Сохранён","Saved"):"")+"\n"+subtitle.get());styleSessionLabel(label);if(!base&&!stored&&!running.getAsBoolean()){if(selectedRow==frame)selectedRow=null;body.removeView(frame);}});
        if(!base){TextView more=new TextView(context);more.setText("⋯");more.setTextSize(24);more.setTextColor(0xfff0f0ee);more.setGravity(Gravity.CENTER);more.setBackground(WorkspaceRipple.background(context,null,10));more.setContentDescription(tr("Меню: ","Menu: ")+title);heading.addView(more,new LinearLayout.LayoutParams(dp(48),dp(48)));
            final WorkspacePopover[] popup={null};more.setOnClickListener(v->{if(popup[0]!=null){popup[0].dismiss();return;}if(activePopup!=null)activePopup.dismissImmediately();WorkspacePopover m=new WorkspacePopover(context).animated();popup[0]=m;activePopup=m;boolean stored=isSaved.getAsBoolean();Runnable updateLabel=()->{label.setText(title+(isSaved.getAsBoolean()?" · "+tr("Сохранён","Saved"):"")+"\n"+subtitle.get());styleSessionLabel(label);};m.item(stored?tr("Удалить из сохранённых","Remove from saved"):(agent?tr("Сохранить чат","Save chat"):tr("Сохранить сессию","Save session")),R.drawable.workspace_library,()->{if(stored)unsave.run();else save.run();updateLabel.run();});if(running.getAsBoolean())m.item(agent?tr("Закрыть чат","Close chat"):tr("Закрыть сессию","Close session"),R.drawable.workspace_close,()->{if(running.getAsBoolean()){dismiss();close.run();}});m.dismissed(()->{popup[0]=null;if(activePopup==m)activePopup=null;updateLabel.run();});m.showBelow(more);});}

        return actions;
    }
    private void styleSessionLabel(TextView label){String value=label.getText().toString();int split=value.indexOf('\n');if(split<0)return;android.text.SpannableString styled=new android.text.SpannableString(value);styled.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),0,split,0);styled.setSpan(new android.text.style.AbsoluteSizeSpan(13,true),0,split,0);styled.setSpan(new android.text.style.AbsoluteSizeSpan(10,true),split+1,value.length(),0);styled.setSpan(new android.text.style.ForegroundColorSpan(WorkspaceDialogs.MUTED),split+1,value.length(),0);label.setText(styled);}
    void footer(String version){TextView v=new TextView(context);v.setText("Termux Job’s\n"+version);v.setTextColor(0xffa5a5a2);v.setTextSize(11);v.setGravity(Gravity.CENTER);v.setPadding(0,dp(24),0,dp(8));body.addView(v);}
    private String tr(String ru,String en){return WorkspaceLanguage.code(context).equals("ru")?ru:en;}
}
