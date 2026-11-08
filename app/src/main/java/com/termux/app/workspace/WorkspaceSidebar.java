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
    private final Context windowContext;
    private final java.util.List<TextView> widthRows=new java.util.ArrayList<>();
    private final java.util.List<String> widthCaptions=new java.util.ArrayList<>();
    private final FrameLayout root;
    private WorkspaceExpansion expanded;
    private LinearLayout sectionBody;
    private Section currentSection,activeSection;
    private final java.util.List<Section> sections=new java.util.ArrayList<>();
    private WorkspaceExpansion activeDisclosure;
    private final class Section {
        final String key,title;final TextView header;final LinearLayout items;final WorkspaceExpansion expansion;
        Section(String key,String title,int icon){this.key=key;this.title=title;
            LinearLayout card=new LinearLayout(context);card.setOrientation(LinearLayout.VERTICAL);card.setTag("sidebar-card-"+key);card.setPadding(dp(6),dp(7),dp(6),dp(9));
            GradientDrawable bg=new GradientDrawable();bg.setColor(WorkspaceDialogs.BG);bg.setStroke(dp(1),0xff303b34);bg.setCornerRadius(dp(15));card.setBackground(bg);
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.bottomMargin=dp(10);body.addView(card,cp);
            header=new TextView(context);header.setTextSize(17);header.setTypeface(null,android.graphics.Typeface.BOLD);header.setTextColor(0xffc8d5cc);header.setPadding(dp(8),dp(7),dp(8),dp(8));header.setMinHeight(dp(48));header.setGravity(Gravity.CENTER_VERTICAL);header.setTag("sidebar-section-"+key);
            if(icon!=0){android.graphics.drawable.Drawable d=context.getDrawable(icon).mutate();d.setTint(0xff8bb29a);d.setBounds(0,0,dp(18),dp(18));header.setCompoundDrawables(d,null,null,null);header.setCompoundDrawablePadding(dp(9));}
            card.addView(header,new LinearLayout.LayoutParams(-1,-2));fitWidth(header,title+" ▾");
            items=new LinearLayout(context);items.setOrientation(LinearLayout.VERTICAL);items.setTag("sidebar-section-items-"+key);card.addView(items,new LinearLayout.LayoutParams(-1,-2));expansion=new WorkspaceExpansion(items);
            open(saved());header.setOnClickListener(v->{boolean next=!expansion.isOpen();open(next);windowContext.getSharedPreferences("sidebar-sections",Context.MODE_PRIVATE).edit().putBoolean(key,next).apply();});
        }
        boolean saved(){return windowContext.getSharedPreferences("sidebar-sections",Context.MODE_PRIVATE).getBoolean(key,true);}
        void open(boolean value){expansion.setOpen(value);header.setText(title+(value?" ▾":" ▸"));header.setContentDescription(title+" · "+tr(value?"Развёрнут":"Свёрнут",value?"Expanded":"Collapsed"));}
    }
    private LinearLayout destination(){return sectionBody==null?body:sectionBody;}
    void section(String key,String title,int icon){currentSection=new Section(key,title,icon);sections.add(currentSection);sectionBody=currentSection.items;}

    private float downX, downY;
    private boolean closing;
    private boolean suspended;
    private View selectedRow;
    private WorkspacePopover activePopup;
    private final java.util.List<Runnable> sessionRefresh=new java.util.ArrayList<>();
    void track(Runnable refresh){sessionRefresh.add(refresh);refresh.run();}
    void refreshSessions(){for(Runnable refresh:sessionRefresh)refresh.run();}

    WorkspaceSidebar(Context context) {
        this.windowContext=context;this.context=SidebarStyle.content(context);context=this.context;
        dialog=new Dialog(windowContext);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        root=new FrameLayout(context);root.setBackgroundColor(0x88000000);
        scroll=new ScrollView(context){@Override public boolean dispatchTouchEvent(MotionEvent event){
            if(event.getActionMasked()==MotionEvent.ACTION_DOWN){downX=event.getX();downY=event.getY();}
            if(event.getActionMasked()==MotionEvent.ACTION_MOVE&&event.getX()-downX<-dp(64)&&Math.abs(event.getY()-downY)<dp(48)){MotionEvent cancel=MotionEvent.obtain(event);cancel.setAction(MotionEvent.ACTION_CANCEL);super.dispatchTouchEvent(cancel);cancel.recycle();dismiss();return true;}
            return super.dispatchTouchEvent(event);
        }};scroll.setTag("workspace-sidebar");scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);scroll.setHorizontalScrollBarEnabled(false);
        GradientDrawable drawer=new GradientDrawable();drawer.setColor(WorkspaceDialogs.BG);drawer.setStroke(dp(1),WorkspaceDialogs.LINE);float radius=dp(22);drawer.setCornerRadii(new float[]{0,0,radius,radius,radius,radius,0,0});scroll.setBackground(drawer);scroll.setClipToOutline(true);
        body=new LinearLayout(context);body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(10),dp(10),dp(10),dp(20));scroll.addView(body);
        root.addView(scroll,new FrameLayout.LayoutParams(width(),-1,Gravity.START));
        root.setOnClickListener(v->dismiss());scroll.setOnClickListener(v->{});
        root.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{
            int width=width();if(scroll.getLayoutParams().width!=width){scroll.getLayoutParams().width=width;scroll.requestLayout();}
        });
        dialog.setContentView(root);
        dialog.setOnDismissListener(d->{if(activePopup!=null)activePopup.dismissImmediately();});
        dialog.setOnKeyListener((d,key,event)->{if(key==android.view.KeyEvent.KEYCODE_BACK){if(event.getAction()==android.view.KeyEvent.ACTION_UP)dismiss();return true;}return false;});
    }
    private int width(){
        int screen=root.getWidth()>0?root.getWidth():windowContext.getResources().getDisplayMetrics().widthPixels;
        int cap=Math.max(1,Math.min(screen-WorkspaceDialogs.dp(windowContext,24),Math.round(screen*.80f)));
        int natural=dp(200);
        for(int i=0;i<widthRows.size();i++){
            TextView row=widthRows.get(i);String title=widthCaptions.get(i);int text=0;
            for(String part:title.split("\n"))text=Math.max(text,(int)Math.ceil(row.getPaint().measureText(part)));
            int edges=body.getPaddingLeft()+body.getPaddingRight()+row.getCompoundPaddingLeft()+row.getCompoundPaddingRight();
            View current=row;
            while(current!=body){android.view.ViewGroup.LayoutParams lp=current.getLayoutParams();if(lp instanceof android.view.ViewGroup.MarginLayoutParams){android.view.ViewGroup.MarginLayoutParams m=(android.view.ViewGroup.MarginLayoutParams)lp;edges+=m.leftMargin+m.rightMargin;}
                if(!(current.getParent() instanceof View)||current.getParent()==body)break;
                current=(View)current.getParent();edges+=current.getPaddingLeft()+current.getPaddingRight();
            }
            natural=Math.max(natural,Math.max(row.getMinWidth(),text+edges));
        }
        return Math.max(1,Math.min(cap,natural+dp(8)));
    }
    private void fitWidth(TextView view,String caption){widthRows.add(view);widthCaptions.add(caption);}
    LinearLayout group(LinearLayout parent,String title,String key){
        LinearLayout group=new LinearLayout(context);group.setOrientation(LinearLayout.VERTICAL);group.setTag("sidebar-group-"+key);parent.addView(group,new LinearLayout.LayoutParams(-1,-2));
        TextView label=new TextView(context);label.setText(title);label.setAllCaps(true);label.setTextSize(15);label.setLetterSpacing(.07f);label.setTextColor(0xff949c98);label.setPadding(dp(10),dp(14),dp(6),dp(5));label.setTag("sidebar-group-title");group.addView(label,new LinearLayout.LayoutParams(-1,-2));fitWidth(label,title);
        LinearLayout children=new LinearLayout(context);children.setOrientation(LinearLayout.VERTICAL);children.setPadding(dp(10),0,0,0);children.setTag("sidebar-group-items-"+key);group.addView(children,new LinearLayout.LayoutParams(-1,-2));return children;
    }
    private int dp(int n){return WorkspaceDialogs.dp(context,n);}
    void show(){
        for(Section section:sections)section.open(section==activeSection||section.saved());
        if(activeDisclosure!=null){if(expanded!=null&&expanded!=activeDisclosure)expanded.setOpen(false);activeDisclosure.setOpen(true);expanded=activeDisclosure;}
        dialog.show();Window w=dialog.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.setStatusBarColor(android.graphics.Color.BLACK);w.getDecorView().setSystemUiVisibility(w.getDecorView().getSystemUiVisibility()&~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);w.setDimAmount(0);w.setLayout(-1,-1);w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);}
        if(selectedRow!=null){
            scroll.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener(){
                @Override public void onGlobalLayout(){
                    if(!scroll.getViewTreeObserver().isAlive())return;
                    scroll.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    if(dialog.isShowing())scroll.scrollTo(0,Math.max(0,selectedTop()-dp(8)));
                }
            });
        }
        scroll.setTranslationX(-width());scroll.animate().translationX(0).setDuration(duration(280)).start();
    }
    private long duration(int ms){return android.animation.ValueAnimator.areAnimatorsEnabled()?ms:0;}
    void dismiss(){if(closing)return;closing=true;scroll.animate().translationX(-width()).setDuration(duration(220)).withEndAction(dialog::dismiss).start();}
    void dispose(){closing=true;scroll.animate().cancel();if(activePopup!=null)activePopup.dismissImmediately();dialog.dismiss();}
    void suspend(){if(closing||suspended)return;suspended=true;scroll.animate().cancel();scroll.setTranslationX(0);if(activePopup!=null)activePopup.dismissImmediately();dialog.hide();}
    void resume(){if(closing||!suspended)return;suspended=false;dialog.show();refreshSessions();}
    void reopen(){if(closing||!suspended)return;suspended=false;show();refreshSessions();}
    boolean suspended(){return suspended;}
    private int selectedTop(){int top=0;View v=selectedRow;while(v!=null&&v!=body){top+=v.getTop();v=v.getParent() instanceof View?(View)v.getParent():null;}return top;}
    void section(String title){section(title,title,0);}
    TextView action(LinearLayout parent,String title,int icon,Runnable callback){
        TextView v=new TextView(context);v.setText(title);v.setTextSize(20.5f);v.setTextColor(0xfff0f0ee);v.setGravity(Gravity.CENTER_VERTICAL);v.setMinHeight(dp(48));v.setPadding(dp(12),dp(10),dp(12),dp(10));
        if(icon!=0){android.graphics.drawable.Drawable d=context.getDrawable(icon).mutate();d.setTint(0xffa5a5a2);d.setBounds(0,0,dp(24),dp(24));v.setCompoundDrawables(d,null,null,null);v.setCompoundDrawablePadding(dp(16));}
        v.setBackground(WorkspaceRipple.background(context,null,10));
        v.setOnClickListener(x->callback.run());parent.addView(v,new LinearLayout.LayoutParams(-1,-2));fitWidth(v,title);return v;
    }
    void overlayAction(String title,int icon,Runnable callback){action(destination(),title,icon,callback);}
    void action(String title,int icon,Runnable callback){action(destination(),title,icon,()->{dismiss();callback.run();});}
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
        return session(id,()->title,subtitle,base,saved,selected,agent,running,isSaved,save,unsave,close);
    }
    LinearLayout session(String id,java.util.function.Supplier<String> title,java.util.function.Supplier<String> subtitle,boolean base,boolean saved,boolean selected,boolean agent,java.util.function.BooleanSupplier running,java.util.function.BooleanSupplier isSaved,Runnable save,Runnable unsave,Runnable close){
        LinearLayout frame=new LinearLayout(context);frame.setOrientation(LinearLayout.VERTICAL);frame.setTag("sidebar-session-"+id);destination().addView(frame);
        LinearLayout heading=new LinearLayout(context);heading.setGravity(Gravity.CENTER_VERTICAL);frame.addView(heading);
        SessionLabel label=new SessionLabel(title.get()+(saved?" · "+tr("Сохранён","Saved"):"")+"\n"+subtitle.get(),agent?R.drawable.workspace_ai_agent:R.drawable.workspace_terminal);heading.addView(label,new LinearLayout.LayoutParams(0,-2,1));
        label.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));
        if(selected){GradientDrawable bg=new GradientDrawable();bg.setColor(context.getColor(R.color.workspace_chat_background));bg.setStroke(dp(1),0xff4e6956);bg.setCornerRadius(dp(11));heading.setBackground(bg);}
        SidebarTreeActions actions=new SidebarTreeActions(context);actions.setVisibility(View.GONE);LinearLayout.LayoutParams branch=new LinearLayout.LayoutParams(-1,-2);branch.leftMargin=dp(24);branch.rightMargin=dp(4);branch.topMargin=dp(6);branch.bottomMargin=dp(12);frame.addView(actions,branch);
        WorkspaceExpansion disclosure=new WorkspaceExpansion(actions);
        if(selected){if(expanded!=null)expanded.setOpen(false);disclosure.setOpen(true);expanded=disclosure;activeDisclosure=disclosure;activeSection=currentSection;if(activeSection!=null)activeSection.open(true);selectedRow=frame;}
        label.setOnClickListener(v->{boolean open=!disclosure.isOpen();if(expanded!=null&&expanded!=disclosure)expanded.setOpen(false);disclosure.setOpen(open);expanded=open?disclosure:null;});
        sessionRefresh.add(()->{boolean stored=isSaved.getAsBoolean();label.setText(title.get()+(stored?" · "+tr("Сохранён","Saved"):"")+"\n"+subtitle.get());if(!base&&!stored&&!running.getAsBoolean()){if(selectedRow==frame)selectedRow=null;if(frame.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)frame.getParent()).removeView(frame);}});
        if(!base){TextView more=new TextView(context);more.setText("⋯");more.setTextSize(24);more.setTextColor(0xfff0f0ee);more.setGravity(Gravity.CENTER);more.setBackground(WorkspaceRipple.background(context,null,10));more.setContentDescription(tr("Меню: ","Menu: ")+title.get());heading.addView(more,new LinearLayout.LayoutParams(dp(48),dp(48)));sessionRefresh.add(()->more.setContentDescription(tr("Меню: ","Menu: ")+title.get()));
            final WorkspacePopover[] popup={null};more.setOnClickListener(v->{if(popup[0]!=null){popup[0].dismiss();return;}if(activePopup!=null)activePopup.dismissImmediately();WorkspacePopover m=new WorkspacePopover(SidebarStyle.scaled(context,1.3f)).animated();popup[0]=m;activePopup=m;boolean stored=isSaved.getAsBoolean();Runnable updateLabel=()->{label.setText(title.get()+(isSaved.getAsBoolean()?" · "+tr("Сохранён","Saved"):"")+"\n"+subtitle.get());};m.item(stored?tr("Удалить из сохранённых","Remove from saved"):(agent?tr("Сохранить чат","Save chat"):tr("Сохранить сессию","Save session")),R.drawable.workspace_library,()->{if(stored)unsave.run();else save.run();updateLabel.run();});if(running.getAsBoolean())m.item(agent?tr("Закрыть чат","Close chat"):tr("Закрыть сессию","Close session"),R.drawable.workspace_close,()->{if(running.getAsBoolean()){dismiss();close.run();}});m.dismissed(()->{popup[0]=null;if(activePopup==m)activePopup=null;updateLabel.run();});m.showBelow(more);});}

        return actions;
    }
    private final class SessionLabel extends LinearLayout {
        private final TextView name,details;
        SessionLabel(String value,int icon){super(context);setGravity(Gravity.CENTER_VERTICAL);setPadding(dp(12),dp(10),dp(12),dp(10));setMinimumHeight(dp(64));setBackground(WorkspaceRipple.background(context,null,10));android.widget.ImageView image=new android.widget.ImageView(context);android.graphics.drawable.Drawable drawable=context.getDrawable(icon).mutate();drawable.setTint(WorkspaceDialogs.MUTED);image.setImageDrawable(drawable);addView(image,new LinearLayout.LayoutParams(dp(24),dp(24)));LinearLayout text=new LinearLayout(context);text.setOrientation(VERTICAL);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,-2,1);tp.leftMargin=dp(16);addView(text,tp);name=new TextView(context);name.setTag("sidebar-session-title");name.setTextSize(20.5f);name.setTypeface(null,android.graphics.Typeface.BOLD);name.setTextColor(WorkspaceDialogs.TEXT);name.setSingleLine(true);name.setEllipsize(android.text.TextUtils.TruncateAt.END);details=new TextView(context);details.setTag("sidebar-session-subtitle");details.setTextSize(16);details.setTextColor(WorkspaceDialogs.MUTED);details.setSingleLine(true);details.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);text.addView(name,new LinearLayout.LayoutParams(-1,-2));text.addView(details,new LinearLayout.LayoutParams(-1,-2));setText(value);}
        void setText(String value){int split=value.indexOf('\n');name.setText(split<0?value:value.substring(0,split));details.setText(split<0?"":value.substring(split+1).replace('\n',' '));setContentDescription(value);setTooltipText(value);}
    }
    void footer(String version){TextView v=new TextView(context);v.setText("Termux Job’s\n"+version);v.setTextColor(0xffa5a5a2);v.setTextSize(14);v.setGravity(Gravity.CENTER);v.setPadding(0,dp(24),0,dp(8));body.addView(v);fitWidth(v,"Termux Job’s\n"+version);}
    private String tr(String ru,String en){return WorkspaceLanguage.code(context).equals("ru")?ru:en;}
}
