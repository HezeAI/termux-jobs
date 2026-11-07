package com.termux.app.workspace;

import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.function.Consumer;

/** One window owns the dim layer and both pages for the complete creation flow. */
final class AgentCreationSteps {
    final FrameLayout host, form;
    final LinearLayout chooser;
    boolean moving, choosing;
    private final AlertDialog dialog;
    private final Consumer<String> select;

    AgentCreationSteps(AlertDialog dialog, AlertDialog previous, Consumer<String> select) {
        this.dialog=dialog;
        this.select=select;
        Window window=dialog.getWindow();
        android.content.Context context=dialog.getContext();
        host=new FrameLayout(context){
            @Override public boolean dispatchTouchEvent(MotionEvent event){return moving||super.dispatchTouchEvent(event);}
        };
        host.setTag("agent-creation-pages");
        form=new FrameLayout(context);
        form.setTag("agent-creation-form-page");
        ViewGroup content=window.findViewById(android.R.id.content);
        while(content.getChildCount()>0){View child=content.getChildAt(0);content.removeView(child);form.addView(child,new FrameLayout.LayoutParams(-1,-2));}
        GradientDrawable background=new GradientDrawable();background.setColor(WorkspaceDialogs.BG);background.setCornerRadius(dp(24));background.setStroke(dp(1),WorkspaceDialogs.LINE);
        form.setBackground(background);form.setClickable(true);
        chooser=new LinearLayout(context);chooser.setOrientation(LinearLayout.VERTICAL);chooser.setPadding(dp(12),dp(10),dp(12),dp(12));
        chooser.setBackground(background.getConstantState().newDrawable().mutate());
        TextView title=new TextView(context);title.setText(AgentStatusUi.tr(context,"Выберите агента","Choose an agent"));title.setTextSize(16);title.setTextColor(WorkspaceDialogs.TEXT);title.setGravity(Gravity.CENTER);title.setPadding(0,dp(8),0,dp(8));chooser.addView(title);
        LinearLayout choices=new LinearLayout(context);choices.setOrientation(LinearLayout.VERTICAL);choices.setTag("agent-chooser-list");chooser.addView(choices);
        for(String name:new String[]{"Codex","Claude","Gemini","OpenCode"}){TextView item=WorkspaceDialogs.action(context,name,0);item.setGravity(Gravity.CENTER);choices.addView(item);item.setOnClickListener(v->{if(moving||!choosing)return;select.accept(name.toLowerCase(java.util.Locale.ROOT));forward();});}
        host.addView(chooser,new FrameLayout.LayoutParams(Math.min(dp(300),context.getResources().getDisplayMetrics().widthPixels-dp(24)),-2,Gravity.CENTER));
        FrameLayout.LayoutParams fp=new FrameLayout.LayoutParams(-1,-2,Gravity.CENTER);fp.setMargins(dp(12),dp(12),dp(12),dp(12));host.addView(form,fp);
        form.findViewWithTag("agent-create-back").setOnClickListener(v->back());
        window.setWindowAnimations(0);window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));window.setContentView(host);window.setLayout(-1,-1);
        // Transfer ownership of the single dim layer before the next rendered frame.
        if(previous!=null){previous.getWindow().setWindowAnimations(0);previous.dismiss();}
        choosing=true;form.setTranslationX(distance());setMoving(true);
        host.post(()->{setMoving(false);if(dialog.isShowing())forward();});
        host.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){public void onViewAttachedToWindow(View v){}public void onViewDetachedFromWindow(View v){form.animate().cancel();moving=false;}});
    }
    private int dp(int value){return WorkspaceDialogs.dp(dialog.getContext(),value);}
    private int distance(){return dialog.getContext().getResources().getDisplayMetrics().widthPixels;}
    void forward(){if(moving||!choosing)return;choosing=false;form.setVisibility(View.VISIBLE);form.bringToFront();form.setTranslationX(distance());move(0,()->chooser.setVisibility(View.INVISIBLE));}
    void back(){if(moving||choosing)return;chooser.setVisibility(View.VISIBLE);form.bringToFront();move(distance(),()->{form.setVisibility(View.INVISIBLE);choosing=true;});}
    private void setMoving(boolean value){moving=value;form.findViewWithTag("agent-create-back").setEnabled(!value);form.findViewWithTag("agent-create-submit").setEnabled(!value);}
    private void move(float target,Runnable end){setMoving(true);form.animate().cancel();if(ValueAnimator.areAnimatorsEnabled()){form.animate().translationX(target).setDuration(260).setInterpolator(new android.view.animation.DecelerateInterpolator()).withEndAction(()->{end.run();setMoving(false);}).start();}else{form.setTranslationX(target);end.run();setMoving(false);}}
}
